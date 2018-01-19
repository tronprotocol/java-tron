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
package org.apache.gossip.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.gossip.LocalMember;
import org.apache.log4j.Logger;

public class RingStatePersister implements Runnable {

  private static final Logger LOGGER = Logger.getLogger(RingStatePersister.class);
  private final File path;
  // NOTE: this is a different instance than what gets used for message marshalling.
  private final ObjectMapper objectMapper;
  private final GossipManager manager;
  
  public RingStatePersister(File path, GossipManager manager){
    this.path = path;
    this.objectMapper = GossipManager.metdataObjectMapper;
    this.manager = manager;
  }
  
  @Override
  public void run() {
    writeToDisk();
  }
  
  void writeToDisk() {
    NavigableSet<LocalMember> i = manager.getMembers().keySet();
    try (FileOutputStream fos = new FileOutputStream(path)){
      objectMapper.writeValue(fos, i);
    } catch (IOException e) {
      LOGGER.debug(e);
    }
  }

  @SuppressWarnings("unchecked")
  List<LocalMember> readFromDisk() {
    if (!path.exists()) {
      return new ArrayList<>();
    }
    try (FileInputStream fos = new FileInputStream(path)){
      return objectMapper.readValue(fos, ArrayList.class);
    } catch (IOException e) {
      LOGGER.debug(e);
    }
    return new ArrayList<>();
  }
}
