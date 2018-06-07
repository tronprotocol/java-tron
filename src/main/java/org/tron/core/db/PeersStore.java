package org.tron.core.db;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.discover.node.Node;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;

@Component
public class PeersStore extends TronDatabase<Set<Node>> {

  @Autowired
  public PeersStore(ApplicationContext ctx) {
    super("peers");
  }

  @Override
  public LevelDbDataSourceImpl getDbSource() {
    return super.getDbSource();
  }

  @Override
  public void reset() {
    super.reset();
  }

  @Override
  public void put(byte[] key, Set<Node> nodes) {
    StringBuffer sb = new StringBuffer();
    nodes.forEach(node -> sb.append(node.getEnodeURL()).append("&").append(node.getReputation())
        .append("||"));
    dbSource.putData(key, sb.toString().getBytes());
  }

  @Override
  public void delete(byte[] key) {
    dbSource.deleteData(key);
  }

  @Override
  public Set<Node> get(byte[] key) {
    Set<Node> nodes = new HashSet<>();
    byte[] value = dbSource.getData(key);
    if (value != null) {
      StringTokenizer st = new StringTokenizer(new String(value), "||");
      while (st.hasMoreElements()) {
        String strN = st.nextToken();
        int ps = strN.indexOf("&");
        int rept;
        Node n;
        if (ps > 0) {
          n = new Node(strN.substring(0, ps));
          try {
            rept = Integer.parseInt(strN.substring(ps + 1, strN.length()));
          } catch (NumberFormatException e) {
            rept = 0;
          }
        } else {
          n = new Node(strN);
          rept = 0;
        }

        n.setReputation(rept);
        nodes.add(n);
      }
    }
    return nodes;
  }

  @Override
  public boolean has(byte[] key) {
    return dbSource.getData(key) != null;
  }
}
