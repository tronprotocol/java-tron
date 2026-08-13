package org.tron.p2p.dns.tree;


import com.google.protobuf.InvalidProtocolBufferException;
import java.net.UnknownHostException;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tron.p2p.dns.DnsNode;
import org.tron.p2p.exception.DnsException;
import org.tron.p2p.exception.DnsException.TypeEnum;

@Slf4j(topic = "net")
public class NodesEntry implements Entry {

  private final String represent;
  @Getter
  private final List<DnsNode> nodes;

  public NodesEntry(String represent, List<DnsNode> nodes) {
    this.represent = represent;
    this.nodes = nodes;
  }

  public static NodesEntry parseEntry(String e) throws DnsException {
    String content = e.substring(nodesPrefix.length());
    List<DnsNode> nodeList;
    try {
      nodeList = DnsNode.decompress(content.replace("\"",""));
    } catch (InvalidProtocolBufferException | UnknownHostException ex) {
      throw new DnsException(TypeEnum.INVALID_NODES, ex);
    }
    return new NodesEntry(e, nodeList);
  }

  @Override
  public String toString() {
    return represent;
  }
}
