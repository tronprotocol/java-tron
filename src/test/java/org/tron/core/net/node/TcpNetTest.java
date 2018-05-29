package org.tron.core.net.node;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.tron.common.overlay.discover.Node;
import org.tron.common.overlay.message.HelloMessage;
import org.tron.core.capsule.BlockCapsule.BlockId;

@Slf4j
public class TcpNetTest extends BaseNetTest {

  private static final String dbPath = "output-nodeImplTest/tcpNet";
  private static final String dbDirectory = "db_tcp_test";
  private static final String indexDirectory = "index_tcp_test";

  public TcpNetTest() {
    super(dbPath, dbDirectory, indexDirectory);
  }

  @Test
  public void normalTest() throws InterruptedException {
//    Thread.sleep(2000);
    Channel channel = createClient();
    org.tron.common.overlay.discover.Node node = new Node(
        "enode://e437a4836b77ad9d9ffe73ee782ef2614e6d8370fcf62191a6e488276e23717147073a7ce0b444d485fff5a0c34c4577251a7a990cf80d8542e21b95aa8c5e6c@127.0.0.1:17889");
    BlockId genesisBlockId = new BlockId();
    HelloMessage message = new HelloMessage(node,
        System.currentTimeMillis(), manager.getGenesisBlockId(), manager.getSolidBlockId(),
        manager.getHeadBlockId());
    //Unpooled.wrappedBuffer(ArrayUtils.add("nihao".getBytes(), 0, (byte) 1))
    channel.writeAndFlush(message.getSendData())
        .addListener((ChannelFutureListener) future -> {
          if (future.isSuccess()) {
            logger.info("send msg success");
          } else {
            logger.error("send msg fail", future.cause());
          }
        });

    Thread.sleep(2000);
  }

  @Test
  public void errorGenesisBlockIdTest() throws InterruptedException {
//    Thread.sleep(2000);
    Channel channel = createClient();
    org.tron.common.overlay.discover.Node node = new Node(
        "enode://e437a4836b77ad9d9ffe73ee782ef2614e6d8370fcf62191a6e488276e23717147073a7ce0b444d485fff5a0c34c4577251a7a990cf80d8542e21b95aa8c5e6c@127.0.0.1:17889");
    BlockId genesisBlockId = new BlockId();
    HelloMessage message = new HelloMessage(node,
        System.currentTimeMillis(), genesisBlockId, manager.getSolidBlockId(),
        manager.getHeadBlockId());
    //Unpooled.wrappedBuffer(ArrayUtils.add("nihao".getBytes(), 0, (byte) 1))
    channel.writeAndFlush(message.getSendData())
        .addListener((ChannelFutureListener) future -> {
          if (future.isSuccess()) {
            logger.info("send msg success");
          } else {
            logger.error("send msg fail", future.cause());
          }
        });

    Thread.sleep(2000);
  }
}
