package org.tron.core.netlog.slf4jkafka.converter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import java.net.InetAddress;
import java.net.UnknownHostException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.tron.core.config.args.Args;

@Slf4j
public class NodeIdConverterTest {

  @Test
  public void testNodeIdConverter() {
    NodeIdConverter converter = new NodeIdConverter();
    ILoggingEvent event = new LoggingEvent();

    Args.getInstance().setNodeExternalIp("201.202.203.204");
    Args.getInstance().setNodeListenPort(18888);
    String hostname;
    try {
      hostname = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      hostname = "unknown";
    }
    String id_normal = converter.convert(event);
    Assert.assertTrue(("201.202.203.204::18888::" + hostname).equals(id_normal));
  }
}