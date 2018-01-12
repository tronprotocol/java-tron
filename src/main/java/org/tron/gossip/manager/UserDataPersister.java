/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tron.gossip.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import org.tron.gossip.model.PerNodeDataMessage;
import org.tron.gossip.model.SharedDataMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class UserDataPersister implements Runnable {
    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("UserDataPersister");

    private final GossipCore gossipCore;

    private final File perNodePath;
    private final File sharedPath;
    private final ObjectMapper objectMapper;

    UserDataPersister(GossipCore gossipCore, File perNodePath, File sharedPath) {
        this.gossipCore = gossipCore;
        this.objectMapper = GossipManager.metdataObjectMapper;
        this.perNodePath = perNodePath;
        this.sharedPath = sharedPath;
    }

    @SuppressWarnings("unchecked")
    ConcurrentHashMap<String, ConcurrentHashMap<String, PerNodeDataMessage>> readPerNodeFromDisk() {
        if (!perNodePath.exists()) {
            return new ConcurrentHashMap<String, ConcurrentHashMap<String, PerNodeDataMessage>>();
        }
        try (FileInputStream fos = new FileInputStream(perNodePath)) {
            return objectMapper.readValue(fos, ConcurrentHashMap.class);
        } catch (IOException e) {
            LOGGER.debug(e.toString());
        }
        return new ConcurrentHashMap<String, ConcurrentHashMap<String, PerNodeDataMessage>>();
    }

    void writePerNodeToDisk() {
        try (FileOutputStream fos = new FileOutputStream(perNodePath)) {
            objectMapper.writeValue(fos, gossipCore.getPerNodeData());
        } catch (IOException e) {
            LOGGER.warn(e.toString());
        }
    }

    void writeSharedToDisk() {
        try (FileOutputStream fos = new FileOutputStream(sharedPath)) {
            objectMapper.writeValue(fos, gossipCore.getSharedData());
        } catch (IOException e) {
            LOGGER.warn(e.toString());
        }
    }

    @SuppressWarnings("unchecked")
    ConcurrentHashMap<String, SharedDataMessage> readSharedDataFromDisk() {
        if (!sharedPath.exists()) {
            return new ConcurrentHashMap<>();
        }
        try (FileInputStream fos = new FileInputStream(sharedPath)) {
            return objectMapper.readValue(fos, ConcurrentHashMap.class);
        } catch (IOException e) {
            LOGGER.debug(e.toString());
        }
        return new ConcurrentHashMap<String, SharedDataMessage>();
    }

    /**
     * Writes all pernode and shared data to disk
     */
    @Override
    public void run() {
        writePerNodeToDisk();
        writeSharedToDisk();
    }
}
