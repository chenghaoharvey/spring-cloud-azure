/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.integration.eventhub;

import java.util.Optional;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.eventhubs.EventHubClient;
import com.microsoft.azure.eventhubs.PartitionSender;
import com.microsoft.azure.eventprocessorhost.EventProcessorHost;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.spring.cloud.context.core.api.ResourceManagerProvider;
import com.microsoft.azure.spring.cloud.context.core.impl.StorageAccountManager;
import com.microsoft.azure.spring.cloud.context.core.storage.StorageConnectionStringProvider;
import com.microsoft.azure.spring.integration.eventhub.api.EventHubClientFactory;
import com.microsoft.azure.spring.integration.eventhub.factory.DefaultEventHubClientFactory;
import com.microsoft.azure.spring.integration.eventhub.factory.EventHubConnectionStringProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({EventHubClient.class, StorageConnectionStringProvider.class,
    EventProcessorHost.class, DefaultEventHubClientFactory.class })
public class DefaultEventHubClientFactoryTest {

    @Mock
    ResourceManagerProvider resourceManagerProvider;

    @Mock
    StorageAccountManager storageAccountManager;

    @Mock
    EventHubClient eventHubClient;

    @Mock
    PartitionSender partitionSender;

    @Mock
    EventProcessorHost eventProcessorHost;

    @Mock
    StorageAccount storageAccount;

    @Mock
    EventHubConnectionStringProvider connectionStringProvider;

    @Mock
    StorageConnectionStringProvider storageConnectionStringProvider;

    private EventHubClientFactory clientFactory;
    private String eventHubName = "eventHub";
    private String consumerGroup = "group";
    private String connectionString = "conStr";
    private String partitionId = "1";

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(EventHubClient.class);
        when(EventHubClient.createSync(eq(connectionString), any())).thenReturn(eventHubClient);
        when(eventHubClient.createPartitionSenderSync(eq(partitionId))).thenReturn(partitionSender);
        when(connectionStringProvider.getConnectionString(eq(eventHubName))).thenReturn(connectionString);

        PowerMockito.mockStatic(StorageConnectionStringProvider.class);
        when(StorageConnectionStringProvider
                .getConnectionString(isA(StorageAccount.class), isA(AzureEnvironment.class)))
        .thenReturn(connectionString);
        when(StorageConnectionStringProvider
                .getConnectionString(isA(StorageAccount.class), isA(AzureEnvironment.class)))
        .thenReturn(connectionString);
        when(resourceManagerProvider.getStorageAccountManager()).thenReturn(storageAccountManager);
        when(storageAccountManager.getOrCreate(any())).thenReturn(storageAccount);
        PowerMockito.whenNew(EventProcessorHost.class).withAnyArguments().thenReturn(eventProcessorHost);

        this.clientFactory = spy(new DefaultEventHubClientFactory(connectionStringProvider, connectionString));
    }

    @Test
    public void testGetEventHubClient() {
        EventHubClient client = clientFactory.getOrCreateClient(eventHubName);
        assertNotNull(client);
        EventHubClient another = clientFactory.getOrCreateClient(eventHubName);
        assertEquals(client, another);
    }

    @Test
    public void testGetPartitionSender() {
        PartitionSender sender = clientFactory.getOrCreatePartitionSender(this.eventHubName, partitionId);
        assertNotNull(sender);
        PartitionSender another = clientFactory.getOrCreatePartitionSender(eventHubName, partitionId);
        assertEquals(sender, another);
    }

    @Test
    public void testGetEventProcessorHost() throws Exception {
        clientFactory.getOrCreateEventProcessorHost(eventHubName, consumerGroup);
        Optional<EventProcessorHost> optionalEph = clientFactory.getEventProcessorHost(eventHubName, consumerGroup);

        assertTrue(optionalEph.isPresent());
    }

    @Test
    public void testGetNullEventProcessorHost() {
        Optional<EventProcessorHost> optionalEph = clientFactory.getEventProcessorHost(eventHubName, consumerGroup);
        assertFalse(optionalEph.isPresent());
    }

    @Test
    public void testRemoveEventProcessorHost() {
        EventProcessorHost host = clientFactory.getOrCreateEventProcessorHost(eventHubName, consumerGroup);
        EventProcessorHost another = clientFactory.removeEventProcessorHost(eventHubName, consumerGroup);

        assertSame(host, another);
    }

    @Test
    public void testRemoveAbsentEventProcessorHost() {
        EventProcessorHost eventProcessorHost = clientFactory.removeEventProcessorHost(eventHubName, consumerGroup);
        assertNull(eventProcessorHost);
    }

    @Test
    public void testGetOrCreateEventProcessorHost() throws Exception {
        EventProcessorHost host = clientFactory.getOrCreateEventProcessorHost(eventHubName, consumerGroup);
        assertNotNull(host);
        clientFactory.getOrCreateEventProcessorHost(eventHubName, consumerGroup);
        
        verifyPrivate(clientFactory).invoke("createEventProcessorHost", eventHubName, consumerGroup);
    }
    
    @Test
    public void testRecreateEventProcessorHost() throws Exception {
        EventProcessorHost host = clientFactory.getOrCreateEventProcessorHost(eventHubName, consumerGroup);
        assertNotNull(host);
        clientFactory.removeEventProcessorHost(eventHubName, consumerGroup);
        clientFactory.getOrCreateEventProcessorHost(eventHubName, consumerGroup);
        
        verifyPrivate(clientFactory, times(2)).invoke("createEventProcessorHost", eventHubName, consumerGroup);
    }
}
