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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This object represents the settings used when starting the gossip service.
 * 
 */
public class StartupSettings {
  private static final Logger log = Logger.getLogger(StartupSettings.class);

  /** The id to use fo the service */
  private String id;

  private URI uri;
  
  private String cluster;

  /** The gossip settings used at startup. */
  private final GossipSettings gossipSettings;

  /** The list with gossip members to start with. */
  private final List<Member> gossipMembers;

  /** Default setting values */
  private static final boolean DEFAULT_BULK_TRANSFER = false;
  public static final int DEFAULT_BULK_TRANSFER_SIZE = 100;

  /**
   * Constructor.
   * 
   * @param id
   *          The id to be used for this service
   * @param uri
   *          A URI object containing IP/hostname and port
   * @param logLevel
   *          unused
   */
  public StartupSettings(String id, URI uri, int logLevel, String cluster) {
    this(id, uri, new GossipSettings(), cluster);
  }

  public URI getUri() {
    return uri;
  }

  public void setUri(URI uri) {
    this.uri = uri;
  }

  /**
   * Constructor.
   * 
   * @param id
   *          The id to be used for this service
   * @param uri
   *          A URI object containing IP/hostname and port
   */
  public StartupSettings(String id, URI uri, GossipSettings gossipSettings, String cluster) {
    this.id = id;
    this.uri = uri;
    this.gossipSettings = gossipSettings;
    this.setCluster(cluster);
    gossipMembers = new ArrayList<>();
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  public String getCluster() {
    return cluster;
  }

  /**
   * Set the id to be used for this service.
   * 
   * @param id
   *          The id for this service.
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Get the id for this service.
   * 
   * @return the service's id.
   */
  public String getId() {
    return id;
  }

  /**
   * Get the GossipSettings.
   * 
   * @return The GossipSettings object.
   */
  public GossipSettings getGossipSettings() {
    return gossipSettings;
  }

  /**
   * Add a gossip member to the list of members to start with.
   * 
   * @param member
   *          The member to add.
   */
  public void addGossipMember(Member member) {
    gossipMembers.add(member);
  }

  /**
   * Get the list with gossip members.
   * 
   * @return The gossip members.
   */
  public List<Member> getGossipMembers() {
    return gossipMembers;
  }

  /**
   * Parse the settings for the gossip service from a JSON file.
   * 
   * @param jsonFile
   *          The file object which refers to the JSON config file.
   * @return The StartupSettings object with the settings from the config file.
   * @throws FileNotFoundException
   *           Thrown when the file cannot be found.
   * @throws IOException
   *           Thrown when reading the file gives problems.
   * @throws URISyntaxException 
   */
  public static StartupSettings fromJSONFile(File jsonFile) throws  
          FileNotFoundException, IOException, URISyntaxException {
    ObjectMapper om = new ObjectMapper();
    JsonNode root = om.readTree(jsonFile);
    JsonNode jsonObject = root.get(0);
    String uri = jsonObject.get("uri").textValue();
    String id = jsonObject.get("id").textValue();
    Map<String,String> properties = new HashMap<String,String>();
    JsonNode n = jsonObject.get("properties");
    Iterator<Entry<String, JsonNode>> l = n.fields();
    while (l.hasNext()){
      Entry<String, JsonNode> i = l.next();
      properties.put(i.getKey(), i.getValue().asText());
    }
    //TODO constants as defaults?
    // TODO setting keys as constants?
    int gossipInterval = jsonObject.get("gossip_interval").intValue();
    int cleanupInterval = jsonObject.get("cleanup_interval").intValue();
    int windowSize = jsonObject.get("window_size").intValue();
    int minSamples = jsonObject.get("minimum_samples").intValue();
    double convictThreshold = jsonObject.get("convict_threshold").asDouble();
    String cluster = jsonObject.get("cluster").textValue();
    String distribution = jsonObject.get("distribution").textValue();
    boolean bulkTransfer = jsonObject.has("bulk_transfer") ?
            jsonObject.get("bulk_transfer").booleanValue() :
            DEFAULT_BULK_TRANSFER;
    int bulkTransferSize = jsonObject.has("bulk_transfer_size") ?
            jsonObject.get("bulk_transfer_size").intValue() :
            DEFAULT_BULK_TRANSFER_SIZE;
    if (cluster == null){
      throw new IllegalArgumentException("cluster was null. It is required");
    }
    String transportClass = jsonObject.has("transport_manager_class") ? 
        jsonObject.get("transport_manager_class").textValue() : 
        null;
    String protocolClass = jsonObject.has("protocol_manager_class") ? 
        jsonObject.get("protocol_manager_class").textValue() : 
        null;
    URI uri2 = new URI(uri);
    GossipSettings gossipSettings = new GossipSettings(gossipInterval, cleanupInterval, windowSize,
            minSamples, convictThreshold, distribution, bulkTransfer);
    gossipSettings.setBulkTransferSize(bulkTransferSize);
    if (transportClass != null) {
      gossipSettings.setTransportManagerClass(transportClass);
    }
    if (protocolClass != null) {
      gossipSettings.setProtocolManagerClass(protocolClass);
    }
    StartupSettings settings = new StartupSettings(id, uri2, gossipSettings, cluster);
    String configMembersDetails = "Config-members [";
    JsonNode membersJSON = jsonObject.get("members");
    Iterator<JsonNode> it = membersJSON.iterator();
    while (it.hasNext()){
      JsonNode child = it.next();
      URI uri3 = new URI(child.get("uri").textValue());
      RemoteMember member = new RemoteMember(child.get("cluster").asText(),
              uri3, "", 0, new HashMap<String,String>());
      settings.addGossipMember(member);
      configMembersDetails += member.computeAddress();
      configMembersDetails += ", ";
    }
    log.info(configMembersDetails + "]");
    return settings;
  }
}
