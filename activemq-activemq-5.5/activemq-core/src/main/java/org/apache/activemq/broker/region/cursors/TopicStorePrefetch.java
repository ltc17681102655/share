/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.broker.region.cursors;

import org.apache.activemq.broker.region.Subscription;
import org.apache.activemq.broker.region.Topic;
import org.apache.activemq.command.Message;
import org.apache.activemq.command.MessageId;
import org.apache.activemq.filter.MessageEvaluationContext;
import org.apache.activemq.filter.NonCachedMessageEvaluationContext;
import org.apache.activemq.store.TopicMessageStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * persist pendingCount messages pendingCount message (messages awaiting disptach
 * to a consumer) cursor
 * 
 * 
 */
class TopicStorePrefetch extends AbstractStoreCursor {
    private static final Logger LOG = LoggerFactory.getLogger(TopicStorePrefetch.class);
    private final TopicMessageStore store;
    private final String clientId;
    private final String subscriberName;
    private final Subscription subscription;
    private int currentLowestPriority;
    
    /**
     * @param topic
     * @param clientId
     * @param subscriberName
     */
    public TopicStorePrefetch(Subscription subscription,Topic topic, String clientId, String subscriberName) {
        super(topic);
        this.subscription=subscription;
        this.store = (TopicMessageStore)topic.getMessageStore();
        this.clientId = clientId;
        this.subscriberName = subscriberName;
        this.maxProducersToAudit=32;
        this.maxAuditDepth=10000;
        resetCurrentLowestPriority();
    }

    private void resetCurrentLowestPriority() {
        currentLowestPriority = 9;
    }

    public synchronized int getCurrentLowestPriority() {
        return currentLowestPriority;
    }

    public boolean recoverMessageReference(MessageId messageReference) throws Exception {
        // shouldn't get called
        throw new RuntimeException("Not supported");
    }
    
        
    @Override
    public synchronized boolean recoverMessage(Message message, boolean cached) throws Exception {
        if (LOG.isTraceEnabled()) {
            LOG.trace("recover: " + message.getMessageId() + ", priority: " + message.getPriority());
        }
        boolean recovered = false;
        MessageEvaluationContext messageEvaluationContext = new NonCachedMessageEvaluationContext();
        messageEvaluationContext.setMessageReference(message);
        if (this.subscription.matches(message, messageEvaluationContext)) {
            recovered = super.recoverMessage(message, cached);
            if (recovered) {
                currentLowestPriority = Math.min(currentLowestPriority, message.getPriority());                
            }
        }
        return recovered;      
    }
    
    @Override
    protected synchronized int getStoreSize() {
        try {
            return store.getMessageCount(clientId, subscriberName);
        } catch (Exception e) {
            LOG.error(this + " Failed to get the outstanding message count from the store", e);
            throw new RuntimeException(e);
        }
    }
    
    @Override
    protected synchronized boolean isStoreEmpty() {
        try {
            boolean empty = this.store.isEmpty();
            if (empty) {
                resetCurrentLowestPriority();
            }
            return empty;
            
        } catch (Exception e) {
            LOG.error("Failed to get message count", e);
            throw new RuntimeException(e);
        }
    }

            
    @Override
    protected void resetBatch() {
        this.store.resetBatching(clientId, subscriberName);
    }

    @Override
    public synchronized void gc() {
        super.gc();
        resetCurrentLowestPriority();
    }
    
    @Override
    protected void doFillBatch() throws Exception {
        this.store.recoverNextMessages(clientId, subscriberName,
                maxBatchSize, this);
    }

    @Override
    public String toString() {
        return "TopicStorePrefetch(" + clientId + "," + subscriberName + ")" + super.toString();
    }
}
