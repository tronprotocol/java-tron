package org.tron.core.net;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.net.message.handshake.HelloMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.p2p.connection.Channel;
import org.tron.protos.Protocol;

public final class PeerSyncTestSupport {

  private PeerSyncTestSupport() {
  }

  public static BlockId blockId(long number) {
    return new BlockId(Sha256Hash.ZERO_HASH, number);
  }

  public static HelloMessage helloMessage(long headNum) throws Exception {
    return new HelloMessage(Protocol.HelloMessage.newBuilder()
        .setHeadBlockId(Protocol.HelloMessage.BlockId.newBuilder()
            .setHash(blockId(headNum).getByteString()).setNumber(headNum))
        .build().toByteArray());
  }

  public static PeerConnection peer(int port) {
    PeerConnection peer = spy(new PeerConnection());
    Channel channel = mock(Channel.class);
    InetSocketAddress address = new InetSocketAddress("127.0.0.1", port);
    when(channel.getInetSocketAddress()).thenReturn(address);
    when(channel.getInetAddress()).thenReturn(address.getAddress());
    ReflectUtils.setFieldValue(peer, "channel", channel);
    doNothing().when(peer).sendMessage(any());
    doNothing().when(peer).disconnect(any());
    return peer;
  }
}
