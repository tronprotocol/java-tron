package org.tron.core.db;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.capsule.BytesCapsule;

@Component
public class CommonStore extends TronDatabase<BytesCapsule> {

  private static final byte[] DB_KEY_LOWEST_BLOCK_NUM = "lowest_block_num".getBytes();
  private static final byte[] DB_KEY_NODE_TYPE = "node_type".getBytes();

  @Autowired
  public CommonStore(ApplicationContext ctx) {
    super("common");
  }

  @Override
  public void put(byte[] key, BytesCapsule item) {
    dbSource.putData(key, item.getData());
  }

  @Override
  public void delete(byte[] key) {
    dbSource.deleteData(key);
  }

  @Override
  public BytesCapsule get(byte[] key) {
    return new BytesCapsule(dbSource.getData(key));
  }

  @Override
  public boolean has(byte[] key) {
    return dbSource.getData(key) != null;
  }

  public int getNodeType() {
    int nodeType = 0;
    byte[] bytes = get(DB_KEY_NODE_TYPE).getData();
    if (bytes != null) {
      nodeType = ByteArray.toInt(bytes);
    }
    return nodeType;
  }

  public void setNodeType(int nodeType) {
    put(DB_KEY_NODE_TYPE, new BytesCapsule(ByteArray.fromInt(nodeType)));
  }

  public long getLowestBlockNum() {
    long lowestBlockNum = 0;
    byte[] bytes = get(DB_KEY_LOWEST_BLOCK_NUM).getData();
    if (bytes != null) {
      lowestBlockNum = ByteArray.toLong(bytes);
    }
    return lowestBlockNum;
  }

  public void setLowestBlockNum(long lowestBlockNum) {
    put(DB_KEY_LOWEST_BLOCK_NUM, new BytesCapsule(ByteArray.fromLong(lowestBlockNum)));
  }

}
