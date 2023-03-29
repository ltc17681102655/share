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
package org.apache.activemq.filter;

import javax.annotation.PostConstruct;

import org.apache.activemq.command.*;

/**
 * A base class for entry objects used to construct a destination based policy
 * map.
 * 
 * 
 * @org.apache.xbean.XBean
 */
public abstract class DestinationMapEntry implements Comparable {

    private ActiveMQDestination destination;

    public int compareTo(Object that) {
        if (that instanceof DestinationMapEntry) {
            DestinationMapEntry thatEntry = (DestinationMapEntry)that;
            return ActiveMQDestination.compare(destination, thatEntry.destination);
        } else if (that == null) {
            return 1;
        } else {
            return getClass().getName().compareTo(that.getClass().getName());
        }
    }

    /**
     * A helper method to set the destination from a configuration file
     */
    public void setQueue(String name) {
        setDestination(new ActiveMQQueue(name));
    }

    /**
     * A helper method to set the destination from a configuration file
     */
    public void setTopic(String name) {
        setDestination(new ActiveMQTopic(name));
    }

    public void setTempTopic(boolean flag){
        setDestination(new ActiveMQTempTopic(">"));
    }
    
    public void setTempQueue(boolean flag){
        setDestination(new ActiveMQTempQueue(">"));
    }

    public ActiveMQDestination getDestination() {
        return destination;
    }

    public void setDestination(ActiveMQDestination destination) {
        this.destination = destination;
    }

    /**
     *
     * @throws Exception
     * @org.apache.xbean.InitMethod
     */
    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        if (destination == null) {
            throw new IllegalArgumentException("You must specify the 'destination' property");
        }
    }

    public Object getValue() {
        return this;
    }
}
