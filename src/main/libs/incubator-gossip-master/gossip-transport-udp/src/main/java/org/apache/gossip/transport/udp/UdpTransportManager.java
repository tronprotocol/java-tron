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
package org.apache.gossip.transport.udp;

import org.apache.gossip.manager.GossipCore;
import org.apache.gossip.manager.GossipManager;
import org.apache.gossip.model.Base;
import org.apache.gossip.transport.AbstractTransportManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class is constructed by reflection in GossipManager.
 * It manages transport (byte read/write) operations over UDP.
 */
public class UdpTransportManager extends AbstractTransportManager implements Runnable {
  
  public static final Logger LOGGER = Logger.getLogger(UdpTransportManager.class);
  
  /** The socket used for the passive thread of the gossip service. */
  private final DatagramSocket server;
  
  private final int soTimeout;
  
  private final Thread me;
  
  private final AtomicBoolean keepRunning = new AtomicBoolean(true);
  
  /** required for reflection to work! */
  public UdpTransportManager(GossipManager gossipManager, GossipCore gossipCore) {
    super(gossipManager, gossipCore);
    soTimeout = gossipManager.getSettings().getGossipInterval() * 2;
    try {
      SocketAddress socketAddress = new InetSocketAddress(gossipManager.getMyself().getUri().getHost(),
              gossipManager.getMyself().getUri().getPort());
      server = new DatagramSocket(socketAddress);
    } catch (SocketException ex) {
      LOGGER.warn(ex);
      throw new RuntimeException(ex);
    }
    me = new Thread(this);
  }

  @Override
  public void run() {
    while (keepRunning.get()) {
      try {
        byte[] buf = read();
        try {
          Base message = gossipManager.getProtocolManager().read(buf);
          gossipCore.receive(message);
          //TODO this is suspect
          gossipManager.getMemberStateRefresher().run();
        } catch (RuntimeException ex) {//TODO trap json exception
          LOGGER.error("Unable to process message", ex);
        }
      } catch (IOException e) {
        // InterruptedException are completely normal here because of the blocking lifecycle.
        if (!(e.getCause() instanceof InterruptedException)) {
          LOGGER.error(e);
        }
        keepRunning.set(false);
      }
    }
  }
  
  @Override
  public void shutdown() {
    keepRunning.set(false);
    server.close();
    super.shutdown();
    me.interrupt();
  }

  /**
   * blocking read a message.
   * @return buffer of message contents.
   * @throws IOException
   */
  public byte[] read() throws IOException {
    byte[] buf = new byte[server.getReceiveBufferSize()];
    DatagramPacket p = new DatagramPacket(buf, buf.length);
    server.receive(p);
    debug(p.getData());
    return p.getData();
  }

  @Override
  public void send(URI endpoint, byte[] buf) throws IOException {
    // todo: investigate UDP socket reuse. It would save a little setup/teardown time wrt to the local socket.
    try (DatagramSocket socket = new DatagramSocket()){
      socket.setSoTimeout(soTimeout);
      InetAddress dest = InetAddress.getByName(endpoint.getHost());
      DatagramPacket payload = new DatagramPacket(buf, buf.length, dest, endpoint.getPort());
      socket.send(payload);
    }
  }
  
  private void debug(byte[] jsonBytes) {
    if (LOGGER.isDebugEnabled()){
      String receivedMessage = new String(jsonBytes);
      LOGGER.debug("Received message ( bytes): " + receivedMessage);
    }
  }

  @Override
  public void startEndpoint() {
    me.start();
  }
  
}
