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
package org.apache.gossip.protocol.json;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.gossip.GossipSettings;
import org.apache.gossip.crdt.CrdtModule;
import org.apache.gossip.manager.PassiveGossipConstants;
import org.apache.gossip.model.Base;
import org.apache.gossip.model.SignedPayload;
import org.apache.gossip.protocol.ProtocolManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

// this class is constructed by reflection in GossipManager.
public class JacksonProtocolManager implements ProtocolManager {
  
  private final ObjectMapper objectMapper;
  private final PrivateKey privKey;
  private final Meter signed;
  private final Meter unsigned;
  
  /** required for reflection to work! */
  public JacksonProtocolManager(GossipSettings settings, String id, MetricRegistry registry) {
    // set up object mapper.
    objectMapper = buildObjectMapper(settings);
    
    // set up message signing.
    if (settings.isSignMessages()){
      File privateKey = new File(settings.getPathToKeyStore(), id);
      File publicKey = new File(settings.getPathToKeyStore(), id + ".pub");
      if (!privateKey.exists()){
        throw new IllegalArgumentException("private key not found " + privateKey);
      }
      if (!publicKey.exists()){
        throw new IllegalArgumentException("public key not found " + publicKey);
      }
      try (FileInputStream keyfis = new FileInputStream(privateKey)) {
        byte[] encKey = new byte[keyfis.available()];
        keyfis.read(encKey);
        keyfis.close();
        PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(encKey);
        KeyFactory keyFactory = KeyFactory.getInstance("DSA");
        privKey = keyFactory.generatePrivate(privKeySpec);
      } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
        throw new RuntimeException("failed hard", e);
      }
    } else {
      privKey = null;
    }
    
    signed = registry.meter(PassiveGossipConstants.SIGNED_MESSAGE);
    unsigned = registry.meter(PassiveGossipConstants.UNSIGNED_MESSAGE);
  }

  @Override
  public byte[] write(Base message) throws IOException {
    byte[] json_bytes;
    if (privKey == null){
      json_bytes = objectMapper.writeValueAsBytes(message);
    } else {
      SignedPayload p = new SignedPayload();
      p.setData(objectMapper.writeValueAsString(message).getBytes());
      p.setSignature(sign(p.getData(), privKey));
      json_bytes = objectMapper.writeValueAsBytes(p);
    }
    return json_bytes;
  }

  @Override
  public Base read(byte[] buf) throws IOException {
    Base activeGossipMessage = objectMapper.readValue(buf, Base.class);
    if (activeGossipMessage instanceof SignedPayload){
      SignedPayload s = (SignedPayload) activeGossipMessage;
      signed.mark();
      return objectMapper.readValue(s.getData(), Base.class);
    } else {
      unsigned.mark();
      return activeGossipMessage;
    }
  }

  public static ObjectMapper buildObjectMapper(GossipSettings settings) {
    ObjectMapper om = new ObjectMapper();
    om.enableDefaultTyping();
    // todo: should be specified in the configuration.
    om.registerModule(new CrdtModule());
    om.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, false);
    return om;
  }
  
  private static byte[] sign(byte [] bytes, PrivateKey pk){
    Signature dsa;
    try {
      dsa = Signature.getInstance("SHA1withDSA", "SUN");
      dsa.initSign(pk);
      dsa.update(bytes);
      return dsa.sign();
    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException | SignatureException e) {
      throw new RuntimeException(e);
    } 
  }
}
