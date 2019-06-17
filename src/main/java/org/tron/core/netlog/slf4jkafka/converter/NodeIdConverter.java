package org.tron.core.netlog.slf4jkafka.converter;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.net.InetAddress;
import org.tron.core.config.args.Args;


public class NodeIdConverter extends ClassicConverter {

  @Override
  public String convert(ILoggingEvent event) {
    String name = "";
    String ip = "";
    String port = "";
    try {
      name = InetAddress.getLocalHost().getHostName();
    } catch (Exception e) {
      name = "unknown";
    }

    ip = Args.getInstance().getNodeExternalIp();
    port = String.valueOf(Args.getInstance().getNodeListenPort());

    return String.join("::", ip, port, name);
  }
}