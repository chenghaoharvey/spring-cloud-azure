/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.integration.servicebus.queue;

import com.microsoft.azure.spring.integration.core.api.SendOperation;
import com.microsoft.azure.spring.integration.core.api.SubscribeOperation;
import com.microsoft.azure.spring.integration.servicebus.ServiceBusClientConfig;
import org.springframework.messaging.Message;

/**
 * Azure service bus queue operation to support send
 * {@link org.springframework.messaging.Message}, subscribe, dead letter queue and abandon
 *
 *
 * @author Warren Zhu
 */
public interface ServiceBusQueueOperation extends SendOperation, SubscribeOperation {
    void setClientConfig(ServiceBusClientConfig clientConfig);

    /**
     * Send a {@link Message} to the given destination deadletterqueue.
     *
     * @return null
     */
    <T> void deadLetter(String destination, Message<T> message, String deadLetterReason,
                        String deadLetterErrorDescription);

    /**
     * Abandon Message with lock token and updated message property. This will make the message available again for
     * processing. Abandoning a message will increase the delivery count on the message
     *
     * @return null
     */
    <T> void abandon(String destination, Message<T> message);
}
