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
package org.apache.gossip;

import org.apache.gossip.lock.LockManagerSettings;

import java.util.HashMap;
import java.util.Map;

/**
 * In this object the settings used by the GossipService are held.
 * 
 */
public class GossipSettings {

  /** Time between gossip'ing in ms. Default is 1 second. */
  private int gossipInterval = 10;

  /** Time between cleanups in ms. Default is 10 seconds. */
  private int cleanupInterval = 5000;

  /** the minimum samples needed before reporting a result */
  private int minimumSamples = 5;
  
  /** the number of samples to keep per host */
  private int windowSize = 5000;
  
  /** the threshold for the detector */
  private double convictThreshold = 10;
  
  private String distribution = "normal";
  
  private String activeGossipClass = "org.apache.gossip.manager.SimpleActiveGossiper";

  private String transportManagerClass = "org.apache.gossip.transport.udp.UdpTransportManager";
  private String protocolManagerClass = "org.apache.gossip.protocol.json.JacksonProtocolManager";
  
  private Map<String,String> activeGossipProperties = new HashMap<>();
  
  private String pathToRingState = "./";
  
  private boolean persistRingState = true;
  
  private String pathToDataState = "./";
  
  private boolean persistDataState = true;
  
  private String pathToKeyStore = "./keys";
  
  private boolean signMessages = false;

  // Settings related to lock manager
  private LockManagerSettings lockManagerSettings = LockManagerSettings
          .getLockManagerDefaultSettings();
  
  private boolean bulkTransfer = false;

  private int bulkTransferSize = StartupSettings.DEFAULT_BULK_TRANSFER_SIZE;
  
  /**
   * Construct GossipSettings with default settings.
   */
  public GossipSettings() {
  }

  /**
   * Construct GossipSettings with given settings.
   * 
   * @param gossipInterval
   *          The gossip interval in ms.
   * @param cleanupInterval
   *          The cleanup interval in ms.
   */
  public GossipSettings(int gossipInterval, int cleanupInterval, int windowSize, int minimumSamples,
          double convictThreshold, String distribution, boolean bulkTransfer) {
    this.gossipInterval = gossipInterval;
    this.cleanupInterval = cleanupInterval;
    this.windowSize = windowSize;
    this.minimumSamples = minimumSamples;
    this.convictThreshold = convictThreshold;
    this.distribution = distribution;
    this.bulkTransfer = bulkTransfer;
  }

  /**
   * Set the gossip interval. This is the time between a gossip message is send.
   * 
   * @param gossipInterval
   *          The gossip interval in ms.
   */
  public void setGossipTimeout(int gossipInterval) {
    this.gossipInterval = gossipInterval;
  }

  /**
   * Set the cleanup interval. This is the time between the last heartbeat received from a member
   * and when it will be marked as dead.
   * 
   * @param cleanupInterval
   *          The cleanup interval in ms.
   */
  public void setCleanupInterval(int cleanupInterval) {
    this.cleanupInterval = cleanupInterval;
  }

  /**
   * Get the gossip interval.
   * 
   * @return The gossip interval in ms.
   */
  public int getGossipInterval() {
    return gossipInterval;
  }

  /**
   * Get the clean interval.
   * 
   * @return The cleanup interval.
   */
  public int getCleanupInterval() {
    return cleanupInterval;
  }

  public int getMinimumSamples() {
    return minimumSamples;
  }

  public void setMinimumSamples(int minimumSamples) {
    this.minimumSamples = minimumSamples;
  }

  public int getWindowSize() {
    return windowSize;
  }

  public void setWindowSize(int windowSize) {
    this.windowSize = windowSize;
  }

  public double getConvictThreshold() {
    return convictThreshold;
  }

  public void setConvictThreshold(double convictThreshold) {
    this.convictThreshold = convictThreshold;
  }

  public void setGossipInterval(int gossipInterval) {
    this.gossipInterval = gossipInterval;
  }

  public String getDistribution() {
    return distribution;
  }

  public void setDistribution(String distribution) {
    this.distribution = distribution;
  }

  public String getActiveGossipClass() {
    return activeGossipClass;
  }

  public void setActiveGossipClass(String activeGossipClass) {
    this.activeGossipClass = activeGossipClass;
  }

  public Map<String, String> getActiveGossipProperties() {
    return activeGossipProperties;
  }

  public void setActiveGossipProperties(Map<String, String> activeGossipProperties) {
    this.activeGossipProperties = activeGossipProperties;
  }

  public String getPathToRingState() {
    return pathToRingState;
  }

  public void setPathToRingState(String pathToRingState) {
    this.pathToRingState = pathToRingState;
  }

  public boolean isPersistRingState() {
    return persistRingState;
  }

  public void setPersistRingState(boolean persistRingState) {
    this.persistRingState = persistRingState;
  }

  public String getPathToDataState() {
    return pathToDataState;
  }

  public void setPathToDataState(String pathToDataState) {
    this.pathToDataState = pathToDataState;
  }

  public boolean isPersistDataState() {
    return persistDataState;
  }

  public void setPersistDataState(boolean persistDataState) {
    this.persistDataState = persistDataState;
  }

  public String getPathToKeyStore() {
    return pathToKeyStore;
  }

  public void setPathToKeyStore(String pathToKeyStore) {
    this.pathToKeyStore = pathToKeyStore;
  }

  public boolean isSignMessages() {
    return signMessages;
  }

  public void setSignMessages(boolean signMessages) {
    this.signMessages = signMessages;
  }

  public String getTransportManagerClass() {
    return transportManagerClass;
  }

  public void setTransportManagerClass(String transportManagerClass) {
    this.transportManagerClass = transportManagerClass;
  }

  public String getProtocolManagerClass() {
    return protocolManagerClass;
  }

  public void setProtocolManagerClass(String protocolManagerClass) {
    this.protocolManagerClass = protocolManagerClass;
  }

  public LockManagerSettings getLockManagerSettings() {
    return lockManagerSettings;
  }

  /**
   * Set the lock settings use by the lock manager
   * @param lockManagerSettings lock settings. This object cannot be null.
   */
  public void setLockManagerSettings(LockManagerSettings lockManagerSettings) {
    this.lockManagerSettings = lockManagerSettings;
  }

  public boolean isBulkTransfer() {
    return bulkTransfer;
  }

  public void setBulkTransfer(boolean bulkTransfer) {
    this.bulkTransfer = bulkTransfer;
  }

  public int getBulkTransferSize() {
    return bulkTransferSize;
  }

  public void setBulkTransferSize(int bulkTransferSize) {
    this.bulkTransferSize = bulkTransferSize;
  }
}
