package org.tron.core.db;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import org.tron.common.overlay.discover.Node;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;

public class PeersStore extends TronDatabase<Set<Node>> {

  protected PeersStore(String dbName) {
    super(dbName);
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
  public void close() {
    super.close();
  }

  @Override
  public void put(byte[] key, Set<Node> nodes) {
    StringBuffer sb = new StringBuffer();
    nodes.forEach(node -> sb.append(node.getEnodeURL()).append("||"));
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
      while( st.hasMoreElements() ){
        nodes.add(new Node(st.nextToken()));
      }
    }
    return nodes;
  }

  @Override
  public boolean has(byte[] key) {
    return dbSource.getData(key) != null;
  }
}
