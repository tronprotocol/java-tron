package org.tron.p2p.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.ByteString;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.P2pEventHandler;
import org.tron.p2p.exception.P2pException;
import org.tron.p2p.exception.P2pException.TypeEnum;
import org.tron.p2p.protos.Discover;
import org.tron.p2p.utils.ByteArray;

@Data
public class Parameter {

  public static int version = 1;

  public static final int TCP_NETTY_WORK_THREAD_NUM = 0;

  public static final int UDP_NETTY_WORK_THREAD_NUM = 1;

  public static final int CONN_MAX_QUEUE_SIZE = 10;

  public static final int NODE_CONNECTION_TIMEOUT = 2000;

  public static final int KEEP_ALIVE_TIMEOUT = 20_000;

  public static final int PING_TIMEOUT = 20_000;

  public static final int NETWORK_TIME_DIFF = 1000;

  public static final long DEFAULT_BAN_TIME = 60_000;

  public static final int MAX_MESSAGE_LENGTH = 5 * 1024 * 1024;

  public static volatile P2pConfig p2pConfig;

  public static volatile List<P2pEventHandler> handlerList = new ArrayList<>();

  public static volatile Map<Byte, P2pEventHandler> handlerMap = new HashMap<>();

  public static void addP2pEventHandle(P2pEventHandler p2PEventHandler) throws P2pException {
    if (p2PEventHandler.getMessageTypes() != null) {
      for (Byte type : p2PEventHandler.getMessageTypes()) {
        if (handlerMap.get(type) != null) {
          throw new P2pException(TypeEnum.TYPE_ALREADY_REGISTERED, "type:" + type);
        }
      }
      for (Byte type : p2PEventHandler.getMessageTypes()) {
        handlerMap.put(type, p2PEventHandler);
      }
    }
    handlerList.add(p2PEventHandler);
  }

  public static Discover.Endpoint getHomeNode() {
    Discover.Endpoint.Builder builder = Discover.Endpoint.newBuilder()
      .setNodeId(ByteString.copyFrom(Parameter.p2pConfig.getNodeID()))
      .setPort(Parameter.p2pConfig.getPort());
    if (StringUtils.isNotEmpty(Parameter.p2pConfig.getIp())) {
      builder.setAddress(ByteString.copyFrom(
        ByteArray.fromString(Parameter.p2pConfig.getIp())));
    }
    if (StringUtils.isNotEmpty(Parameter.p2pConfig.getIpv6())) {
      builder.setAddressIpv6(ByteString.copyFrom(
        ByteArray.fromString(Parameter.p2pConfig.getIpv6())));
    }
    return builder.build();
  }
}
