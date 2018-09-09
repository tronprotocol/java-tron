package org.tron.core.net.node;

import static org.tron.core.net.message.MessageTypes.P2P_DISCONNECT;
import static org.tron.core.net.message.MessageTypes.P2P_HELLO;
import static org.tron.protos.Protocol.ReasonCode.DUPLICATE_PEER;
import static org.tron.protos.Protocol.ReasonCode.FORKED;
import static org.tron.protos.Protocol.ReasonCode.INCOMPATIBLE_CHAIN;
import static org.tron.protos.Protocol.ReasonCode.INCOMPATIBLE_VERSION;

import com.google.common.cache.CacheBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.overlay.discover.node.Node;
import org.tron.common.overlay.message.DisconnectMessage;
import org.tron.common.overlay.message.HelloMessage;
import org.tron.common.overlay.message.Message;
import org.tron.common.overlay.message.P2pMessage;
import org.tron.common.overlay.message.P2pMessageFactory;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.args.Args;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol.Block;

@Slf4j
public class TcpNetTest extends BaseNetTest {

  private static final String dbPath = "output-nodeImplTest-tcpNet";
  private static final String dbDirectory = "db_tcp_test";
  private static final String indexDirectory = "index_tcp_test";
  public static final int sleepTime = 1000;
  private boolean finish = false;
  private final static int tryTimes = 10;
  private final static int port = 17899;

  Node node = new Node(
      "enode://e437a4836b77ad9d9ffe73ee782ef2614e6d8370fcf62191a6e488276e23717147073a7ce0b444d485fff5a0c34c4577251a7a990cf80d8542e21b95aa8c5e6c@127.0.0.1:17889");


  public TcpNetTest() {
    super(dbPath, dbDirectory, indexDirectory, port);
  }

  private enum TestType {
    normal, errorGenesisBlock, errorVersion, errorSolid, repeatConnect
  }

  private class HandshakeHandler extends ByteToMessageDecoder {

    private P2pMessageFactory messageFactory = new P2pMessageFactory();

    private TestType testType;

    public HandshakeHandler(TestType testType) {
      this.testType = testType;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out)
        throws Exception {
      byte[] encoded = new byte[buffer.readableBytes()];
      buffer.readBytes(encoded);
      P2pMessage msg = messageFactory.create(encoded);

      logger.info("Handshake Receive from {}, {}", ctx.channel().remoteAddress(), msg);
      switch (msg.getType()) {
        case P2P_HELLO:
          logger.info("HandshakeHandler success");
          break;
        case P2P_DISCONNECT:
          logger.info("getReasonCode : {}", ((DisconnectMessage) msg).getReasonCode());
          break;
        default:
          return;
      }

      switch (testType) {
        case normal:
          Assert.assertEquals(msg.getType(), P2P_HELLO);
          break;
        case errorGenesisBlock:
          Assert.assertEquals(msg.getType(), P2P_DISCONNECT);
          Assert.assertEquals(((DisconnectMessage) msg).getReasonCode(), INCOMPATIBLE_CHAIN);
          break;
        case errorVersion:
          Assert.assertEquals(msg.getType(), P2P_DISCONNECT);
          Assert.assertEquals(((DisconnectMessage) msg).getReasonCode(), INCOMPATIBLE_VERSION);
          break;
        case errorSolid:
          Assert.assertEquals(msg.getType(), P2P_DISCONNECT);
          Assert.assertEquals(((DisconnectMessage) msg).getReasonCode(), FORKED);
          break;
        case repeatConnect:
          Assert.assertEquals(msg.getType(), P2P_DISCONNECT);
          Assert.assertEquals(((DisconnectMessage) msg).getReasonCode(), DUPLICATE_PEER);
          break;
        default:
          break;
      }

      finish = true;
    }
  }

  //Unpooled.wrappedBuffer(ArrayUtils.add("nihao".getBytes(), 0, (byte) 1))

  //  @Test
  public void normalTest() throws InterruptedException {
    Channel channel = createClient(new HandshakeHandler(TestType.normal));
    HelloMessage message = new HelloMessage(node, System.currentTimeMillis(),
        manager.getGenesisBlockId(), manager.getSolidBlockId(), manager.getHeadBlockId());
    sendMessage(channel, message);
    validResultCloseConnect(channel);
  }

  //  @Test
  public void errorGenesisBlockIdTest() throws InterruptedException {
    Channel channel = createClient(new HandshakeHandler(TestType.errorGenesisBlock));
    BlockId genesisBlockId = new BlockId();
    HelloMessage message = new HelloMessage(node, System.currentTimeMillis(), genesisBlockId,
        manager.getSolidBlockId(), manager.getHeadBlockId());
    sendMessage(channel, message);

    validResultCloseConnect(channel);
  }

  //  @Test
  public void errorVersionTest() throws InterruptedException {
    Channel channel = createClient(new HandshakeHandler(TestType.errorVersion));
    Args.getInstance().setNodeP2pVersion(1);
    HelloMessage message = new HelloMessage(node, System.currentTimeMillis(),
        manager.getGenesisBlockId(), manager.getSolidBlockId(), manager.getHeadBlockId());
    Args.getInstance().setNodeP2pVersion(2);
    sendMessage(channel, message);

    validResultCloseConnect(channel);
  }

  //  @Test
  public void errorSolidBlockIdTest() throws InterruptedException {
    Channel channel = createClient(new HandshakeHandler(TestType.errorSolid));
    HelloMessage message = new HelloMessage(node, System.currentTimeMillis(),
        manager.getGenesisBlockId(), new BlockId(), manager.getHeadBlockId());
    sendMessage(channel, message);
    validResultCloseConnect(channel);
  }

  //  @Test
  public void repeatConnectTest() throws InterruptedException {
    Channel channel = createClient(new HandshakeHandler(TestType.normal));
    HelloMessage message = new HelloMessage(node, System.currentTimeMillis(),
        manager.getGenesisBlockId(), manager.getSolidBlockId(), manager.getHeadBlockId());
    sendMessage(channel, message);
    validResultUnCloseConnect();
    Channel repeatChannel = createClient(new HandshakeHandler(TestType.repeatConnect));
    sendMessage(repeatChannel, message);
    validResultCloseConnect(repeatChannel);
    clearConnect(channel);
  }

  //  @Test
  public void unHandshakeTest() throws InterruptedException {
    List<PeerConnection> beforeActivePeers = ReflectUtils.getFieldValue(pool, "activePeers");
    int beforeSize = beforeActivePeers.size();
    Channel channel = createClient(new HandshakeHandler(TestType.normal));
    BlockMessage message = new BlockMessage(new BlockCapsule(Block.getDefaultInstance()));
    sendMessage(channel, message);
    List<PeerConnection> afterActivePeers = ReflectUtils.getFieldValue(pool, "activePeers");
    int afterSize = afterActivePeers.size();
    Assert.assertEquals(beforeSize, afterSize);
    clearConnect(channel);
  }

  //  @Test
  public void errorMsgTest() throws InterruptedException {
    Channel channel = createClient(new HandshakeHandler(TestType.normal));
    HelloMessage message = new HelloMessage(node, System.currentTimeMillis(),
        manager.getGenesisBlockId(), manager.getSolidBlockId(), manager.getHeadBlockId());
    sendMessage(channel, message);
    validResultUnCloseConnect();
    List<PeerConnection> beforeActivePeers = ReflectUtils.getFieldValue(pool, "activePeers");
    int beforeSize = beforeActivePeers.size();
    logger.info("beforeSize : {}", beforeSize);
    channel.writeAndFlush(Unpooled.wrappedBuffer(ArrayUtils.add("nihao".getBytes(), 0, (byte) 1)))
        .addListener((ChannelFutureListener) future -> {
          if (future.isSuccess()) {
            logger.info("send msg success");
          } else {
            logger.error("send msg fail", future.cause());
          }
        });
    Thread.sleep(2000);
    List<PeerConnection> afterActivePeers = ReflectUtils.getFieldValue(pool, "activePeers");
    int afterSize = afterActivePeers.size();
    logger.info("afterSize : {}", afterSize);
    Assert.assertEquals(beforeSize, afterSize + 1);
    clearConnect(channel);
  }

  private void sendMessage(Channel channel, Message message) {
    channel.writeAndFlush(message.getSendData())
        .addListener((ChannelFutureListener) future -> {
          if (future.isSuccess()) {
            logger.info("send msg success");
          } else {
            logger.error("send msg fail", future.cause());
          }
        });
  }

  private void validResultCloseConnect(Channel channel) throws InterruptedException {
    int trys = 0;
    while (!finish && ++trys < tryTimes) {
      Thread.sleep(sleepTime);
    }
    Assert.assertEquals(finish, true);
    finish = false;
    channel.close();
    Thread.sleep(sleepTime);
    ReflectUtils.setFieldValue(channelManager, "recentlyDisconnected", CacheBuilder.newBuilder().maximumSize(1000)
        .expireAfterWrite(30, TimeUnit.SECONDS).recordStats().build());
    ReflectUtils.setFieldValue(pool, "activePeers",
        Collections.synchronizedList(new ArrayList<PeerConnection>()));
    ReflectUtils.setFieldValue(channelManager, "activePeers", new ConcurrentHashMap<>());
  }

  private void validResultUnCloseConnect() throws InterruptedException {
    int trys = 0;
    while (!finish && ++trys < tryTimes) {
      Thread.sleep(sleepTime);
    }
    Assert.assertEquals(finish, true);
    finish = false;
  }

  private void clearConnect(Channel channel) throws InterruptedException {
    channel.close();
    Thread.sleep(sleepTime);
    ReflectUtils.setFieldValue(channelManager, "recentlyDisconnected", CacheBuilder.newBuilder().maximumSize(1000)
        .expireAfterWrite(30, TimeUnit.SECONDS).recordStats().build());
    ReflectUtils.setFieldValue(pool, "activePeers",
        Collections.synchronizedList(new ArrayList<PeerConnection>()));
    ReflectUtils.setFieldValue(channelManager, "activePeers", new ConcurrentHashMap<>());
  }

  @Test
  public void testAll() throws InterruptedException {
      logger.info("begin normal test ");
      normalTest();
      logger.info("begin errorGenesisBlockId test ");
      errorGenesisBlockIdTest();
      logger.info("begin errorVersion test ");
      errorVersionTest();
      logger.info("begin errorSolidBlockId test ");
      errorSolidBlockIdTest();
      logger.info("begin repeatConnect test");
      repeatConnectTest();
      logger.info("begin unHandshake test");
      unHandshakeTest();
      logger.info("begin errorMsg test");
      errorMsgTest();
  }
}
