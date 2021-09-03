package org.tron.tool.litefullnode.db;

import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "tool")
public abstract class TronDB implements DBInterface {

  protected static final Map<String, TronDB> DB_MAP = Maps.newConcurrentMap();

  protected byte[] simpleEncode(String s) {
    byte[] bytes = s.getBytes();
    byte[] length = Ints.toByteArray(bytes.length);
    byte[] r = new byte[4 + bytes.length];
    System.arraycopy(length, 0, r, 0, 4);
    System.arraycopy(bytes, 0, r, 4, bytes.length);
    return r;
  }

  public String simpleDecode(byte[] bytes) {
    byte[] lengthBytes = Arrays.copyOf(bytes, 4);
    int length = Ints.fromByteArray(lengthBytes);
    byte[] value = Arrays.copyOfRange(bytes, 4, 4 + length);
    return new String(value);
  }

  /**
   * return true if byte array is null or length is 0.
   *
   * @param b bytes
   * @return true or false
   */
  protected boolean isEmptyBytes(byte[] b) {
    if (b != null) {
      return b.length == 0;
    }
    return true;
  }

  public static TronDB getDB(String path) {
    return DB_MAP.get(path);
  }

  public static boolean containsDB(String path) {
    return DB_MAP.containsKey(path);
  }

  public static TronDB removeDB(String path) {
    return DB_MAP.remove(path);
  }

  public static void closeAll() {
    Iterator<Map.Entry<String, TronDB>> iterator = DB_MAP.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, TronDB> next = iterator.next();
      try {
        next.getValue().close();
      } catch (IOException e) {
        logger.error("close db failed, db: {}", next.getKey(), e);
      }
      iterator.remove();
    }
  }

}
