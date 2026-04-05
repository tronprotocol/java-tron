package org.tron.p2p.example;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.P2pEventHandler;
import org.tron.p2p.P2pService;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.discover.Node;
import org.tron.p2p.exception.P2pException;
import org.tron.p2p.stats.P2pStats;
import org.tron.p2p.utils.ByteArray;

public class ImportUsing {

  private P2pService p2pService = new P2pService();
  private Map<InetSocketAddress, Channel> channels = new ConcurrentHashMap<>();

  public void startP2pService() {
    // config p2p parameters
    P2pConfig config = new P2pConfig();
    initConfig(config);

    // register p2p event handler
    MyP2pEventHandler myP2pEventHandler = new MyP2pEventHandler();
    try {
      p2pService.register(myP2pEventHandler);
    } catch (P2pException e) {
      // todo process exception
    }

    // start p2p service
    p2pService.start(config);

    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // send message
    TestMessage testMessage = new TestMessage(ByteArray.fromString("hello"));
    for (Channel channel : channels.values()) {
      channel.send(ByteArray.fromObject(testMessage));
    }

    // close channel
    for (Channel channel : channels.values()) {
      channel.close();
    }
  }

  public void closeP2pService() {
    p2pService.close();
  }

  public void connect(InetSocketAddress address) {
    p2pService.connect(address);
  }

  public P2pStats getP2pStats() {
    return p2pService.getP2pStats();
  }

  public List<Node> getAllNodes() {
    return p2pService.getAllNodes();
  }

  public List<Node> getTableNodes() {
    return p2pService.getTableNodes();
  }

  public List<Node> getConnectableNodes() {
    return p2pService.getConnectableNodes();
  }

  private void initConfig(P2pConfig config) {
    // set p2p version
    config.setNetworkId(11111);

    // set tcp and udp listen port
    config.setPort(18888);

    // turn node discovery on or off
    config.setDiscoverEnable(true);

    // set discover seed nodes
    List<InetSocketAddress> seedNodeList = new ArrayList<>();
    seedNodeList.add(new InetSocketAddress("13.124.62.58", 18888));
    seedNodeList.add(new InetSocketAddress("2600:1f13:908:1b00:e1fd:5a84:251c:a32a", 18888));
    seedNodeList.add(new InetSocketAddress("127.0.0.4", 18888));
    config.setSeedNodes(seedNodeList);

    // set active nodes
    List<InetSocketAddress> activeNodeList = new ArrayList<>();
    activeNodeList.add(new InetSocketAddress("127.0.0.2", 18888));
    activeNodeList.add(new InetSocketAddress("127.0.0.3", 18888));
    config.setActiveNodes(activeNodeList);

    // set trust nodes
    List<InetAddress> trustNodeList = new ArrayList<>();
    trustNodeList.add((new InetSocketAddress("127.0.0.2", 18888)).getAddress());
    config.setTrustNodes(trustNodeList);

    // set the minimum number of connections
    config.setMinConnections(8);

    // set the minimum number of actively established connections
    config.setMinActiveConnections(2);

    // set the maximum number of connections
    config.setMaxConnections(30);

    // set the maximum number of connections with the same IP
    config.setMaxConnectionsWithSameIp(2);
  }

  private class MyP2pEventHandler extends P2pEventHandler {

    public MyP2pEventHandler() {
      this.messageTypes = new HashSet<>();
      this.messageTypes.add(MessageTypes.TEST.getType());
    }

    @Override
    public void onConnect(Channel channel) {
      channels.put(channel.getInetSocketAddress(), channel);
    }

    @Override
    public void onDisconnect(Channel channel) {
      channels.remove(channel.getInetSocketAddress());
    }

    @Override
    public void onMessage(Channel channel, byte[] data) {
      byte type = data[0];
      byte[] messageData = ArrayUtils.subarray(data, 1, data.length);
      switch (MessageTypes.fromByte(type)) {
        case TEST:
          TestMessage message = new TestMessage(messageData);
          // process TestMessage
          break;
        default:
          // todo
      }
    }

  }

  private enum MessageTypes {

    FIRST((byte) 0x00),

    TEST((byte) 0x01),

    LAST((byte) 0x8f);

    private final byte type;

    MessageTypes(byte type) {
      this.type = type;
    }

    public byte getType() {
      return type;
    }

    private static final Map<Byte, MessageTypes> map = new HashMap<>();

    static {
      for (MessageTypes value : values()) {
        map.put(value.type, value);
      }
    }

    public static MessageTypes fromByte(byte type) {
      return map.get(type);
    }
  }

  private static class TestMessage {

    protected MessageTypes type;
    protected byte[] data;

    public TestMessage(byte[] data) {
      this.type = MessageTypes.TEST;
      this.data = data;
    }

  }

}
