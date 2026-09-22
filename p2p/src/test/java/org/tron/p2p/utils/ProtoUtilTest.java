package org.tron.p2p.utils;

import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.connection.message.keepalive.PingMessage;
import org.tron.p2p.protos.Connect;

public class ProtoUtilTest {

  @Test
  public void testCompressMessage() throws Exception {
    PingMessage p1 = new PingMessage();

    Connect.CompressMessage message = ProtoUtil.compressMessage(p1.getData());

    byte[] d1 = ProtoUtil.uncompressMessage(message);

    PingMessage p2 = new PingMessage(d1);

    Assert.assertTrue(p1.getTimeStamp() == p2.getTimeStamp());


    Connect.CompressMessage m2 = ProtoUtil.compressMessage(new byte[1000]);

    byte[] d2 = ProtoUtil.uncompressMessage(m2);

    Assert.assertTrue(d2.length == 1000);
    Assert.assertTrue(d2[0] == 0);
  }
}
