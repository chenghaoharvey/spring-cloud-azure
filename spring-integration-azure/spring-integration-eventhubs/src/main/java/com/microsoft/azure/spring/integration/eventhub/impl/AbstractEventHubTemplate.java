/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.integration.eventhub.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

import com.google.common.base.Strings;
import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.eventhubs.EventHubClient;
import com.microsoft.azure.eventhubs.EventPosition;
import com.microsoft.azure.eventprocessorhost.EventProcessorHost;
import com.microsoft.azure.eventprocessorhost.EventProcessorOptions;
import com.microsoft.azure.spring.integration.core.api.CheckpointConfig;
import com.microsoft.azure.spring.integration.core.api.CheckpointMode;
import com.microsoft.azure.spring.integration.core.api.PartitionSupplier;
import com.microsoft.azure.spring.integration.core.api.StartPosition;
import com.microsoft.azure.spring.integration.eventhub.api.EventHubClientFactory;
import com.microsoft.azure.spring.integration.eventhub.converter.EventHubMessageConverter;

/**
 * Abstract base implementation of event hub template.
 *
 * <p>
 * The main event hub component for sending to and consuming from event hub
 *
 * @author Warren Zhu
 */
public class AbstractEventHubTemplate {
    private static final Logger log = LoggerFactory.getLogger(AbstractEventHubTemplate.class);
    private final EventHubClientFactory clientFactory;

    private EventHubMessageConverter messageConverter = new EventHubMessageConverter();

    private StartPosition startPosition = StartPosition.LATEST;

    private CheckpointConfig checkpointConfig = CheckpointConfig.builder().checkpointMode(CheckpointMode.BATCH).build();

    AbstractEventHubTemplate(EventHubClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    private static EventProcessorOptions buildEventProcessorOptions(StartPosition startPosition) {
        EventProcessorOptions options = EventProcessorOptions.getDefaultOptions();

        if (startPosition == StartPosition.EARLIEST) {
            options.setInitialPositionProvider((s) -> EventPosition.fromStartOfStream());
        } else /* StartPosition.LATEST */ {
            options.setInitialPositionProvider((s) -> EventPosition.fromEndOfStream());
        }

        return options;
    }

    public <T> CompletableFuture<Void> sendAsync(String eventHubName, @NonNull Message<T> message,
            PartitionSupplier partitionSupplier) {
        return sendAsync(eventHubName, Collections.singleton(message), partitionSupplier);
    }

    public <T> CompletableFuture<Void> sendAsync(String eventHubName, Collection<Message<T>> messages,
            PartitionSupplier partitionSupplier) {
        Assert.hasText(eventHubName, "eventHubName can't be null or empty");
        List<EventData> eventData = messages.stream().map(m -> messageConverter.fromMessage(m, EventData.class))
                                            .collect(Collectors.toList());
        return doSend(eventHubName, partitionSupplier, eventData);
    }

    private CompletableFuture<Void> doSend(String eventHubName, PartitionSupplier partitionSupplier,
            List<EventData> eventData) {
        try {
            EventHubClient client = this.clientFactory.getOrCreateClient(eventHubName);

            if (partitionSupplier == null) {
                return client.send(eventData);
            } else if (!Strings.isNullOrEmpty(partitionSupplier.getPartitionId())) {
                return this.clientFactory.getOrCreatePartitionSender(eventHubName, partitionSupplier.getPartitionId())
                                         .send(eventData);
            } else if (!Strings.isNullOrEmpty(partitionSupplier.getPartitionKey())) {
                return client.send(eventData, partitionSupplier.getPartitionKey());
            } else {
                return client.send(eventData);
            }
        } catch (EventHubRuntimeException e) {
            log.error(String.format("Failed to send to '%s' ", eventHubName), e);
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    protected void register(String name, String consumerGroup, EventHubProcessor eventProcessor) {
        EventProcessorHost host = this.clientFactory.getOrCreateEventProcessorHost(name, consumerGroup);
        host.registerEventProcessorFactory(context -> eventProcessor, buildEventProcessorOptions(startPosition));
    }

    protected void unregister(String name, String consumerGroup) {
        this.clientFactory
        .getEventProcessorHost(name, consumerGroup)
        .ifPresent(eventProcessorHost -> unregisterEventProcessor(eventProcessorHost, name, consumerGroup));
    }

    private void unregisterEventProcessor(EventProcessorHost eventProcessorHost, String name, String consumerGroup) {
        this.clientFactory.removeEventProcessorHost(name, consumerGroup);

        eventProcessorHost
        .unregisterEventProcessor()
        .whenComplete((s, t) -> {
            if (t != null) {
                log.warn(String.format("Failed to unregister consumer '%s' with group '%s'", name,
                        consumerGroup), t);
            }
        });
    }

    protected Map<String, Object> buildPropertiesMap() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("startPosition", this.startPosition);
        properties.put("checkpointConfig", this.getCheckpointConfig());

        return properties;
    }

    public EventHubMessageConverter getMessageConverter() {
        return messageConverter;
    }

    public void setMessageConverter(EventHubMessageConverter messageConverter) {
        this.messageConverter = messageConverter;
    }

    public StartPosition getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(StartPosition startPosition) {
        log.info("EventHubTemplate startPosition becomes: {}", startPosition);
        this.startPosition = startPosition;
    }

    public CheckpointConfig getCheckpointConfig() {
        return checkpointConfig;
    }

    public void setCheckpointConfig(CheckpointConfig checkpointConfig) {
        log.info("EventHubTemplate checkpoint config becomes: {}", checkpointConfig);
        this.checkpointConfig = checkpointConfig;
    }
}
