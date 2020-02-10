package org.tron.common.net.udp.message.discover;

import java.util.regex.Pattern;
import org.springframework.util.StringUtils;
import org.tron.common.net.udp.message.Message;
import org.tron.common.overlay.discover.node.Node;
import org.tron.common.overlay.discover.table.KademliaOptions;

public class DiscoverMessageInspector {

  public static final Pattern PATTERN_IP =
      Pattern.compile("^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\"
          + ".(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\"
          + ".(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\"
          + ".(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$");

  private static boolean isFound(String str, Pattern pattern) {
    if (str == null || pattern == null) {
      return false;
    }
    return pattern.matcher(str).find();
  }

  private static boolean validNode(Node node) {
    if (node == null) {
      return false;
    }
    if (!isFound(node.getHost(), PATTERN_IP)
        || node.getId().length != KademliaOptions.NODE_ID_LEN) {
      return false;
    }
    return true;
  }

  private static boolean valid(PingMessage message) {
    return validNode(message.getFrom()) && validNode(message.getTo());
  }

  private static boolean valid(PongMessage message) {
    return validNode(message.getFrom());
  }

  private static boolean valid(FindNodeMessage message) {
    return validNode(message.getFrom())
        && message.getTargetId().length == KademliaOptions.NODE_ID_LEN;
  }

  private static boolean valid(NeighborsMessage message) {
    if (!validNode(message.getFrom())) {
      return false;
    }
    if (!StringUtils.isEmpty(message.getNodes())) {
      if (message.getNodes().size() > KademliaOptions.BUCKET_SIZE) {
        return false;
      }
      for (Node node : message.getNodes()) {
        if (!validNode(node)) {
          return false;
        }
      }
    }
    return true;
  }

  public static boolean valid(Message message) {
    boolean flag = false;
    switch (message.getType()) {
      case DISCOVER_PING:
        flag = valid((PingMessage) message);
        break;
      case DISCOVER_PONG:
        flag = valid((PongMessage) message);
        break;
      case DISCOVER_FIND_NODE:
        flag = valid((FindNodeMessage) message);
        break;
      case DISCOVER_NEIGHBORS:
        flag = valid((NeighborsMessage) message);
        break;
      default:
        break;
    }
    return flag;
  }

}
