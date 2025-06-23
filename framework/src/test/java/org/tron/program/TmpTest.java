//package org.tron.program;
//
//import org.iq80.leveldb.*;
//import org.junit.Test;
//import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
//import java.io.File;
//import java.util.*;
//import org.tron.core.config.args.Args;
//import org.tron.core.config.args.Storage;
//
//public class TmpTest {
//  private static Storage storage;
//
//  static {
//    Args.setParam(new String[]{}, "config-test-storagetest.conf");
//    storage = Args.getInstance().getStorage();
//  }
//
//  @Test
//  public void test() {
//    try {
//// Setup a temporary DB directory
//      String dbDir = "leveldb_test";
//      new File(dbDir).mkdirs();
//// Setup LevelDB options
//      Options options = new Options();
//      options.createIfMissing(true);
//// Create DB instance
//      LevelDbDataSourceImpl db = new LevelDbDataSourceImpl(dbDir, "testdb", options, new WriteOptions());
//      db.initDB();
//      // Clear any previous data
//      db.resetDb();
//// Insert predictable keys
//      for (int i = 0; i < 5; i++) {
//        db.putData(("key" + i).getBytes(), ("value" + i).getBytes());
//      }
//// Expect to get the latest 3 values: value4, value3, value2
//      Set<byte[]> latest = db.getlatestValues(3);
//      System.out.println("Expected:[value4, value3, value2]");
//      System.out.print("Actual : [");
//      latest.forEach(value -> System.out.print(new String(value) + ", "));
//      System.out.println("]");
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//
//
//
//    for (int i = 0; i < 5; i++) {
//      db.putData(("key" + i).getBytes(), ("value" + i).getBytes());
//    }
//    Set<byte[]> latest = db.getlatestValues(3);
//    System.out.println("Expected: [value4, value3, value2]");
//    System.out.print("Actual : [");
//    latest.forEach(value -> System.out.print(new String(value) + ", "));
//    System.out.println("]");
//  }
//
//  public Set<byte[]> getlatestValues(long limit) {
//    Set<byte[]> values = new LinkedHashSet<>();
//    try (DBIterator iterator = db.iterator()) {
//      iterator.seekToLast();
//      while (iterator.hasNext() && values.size() < limit) {
//        Map.Entry<byte[], byte[]> entry = iterator.next(); // ❌ Forward traversal!
//        values.add(entry.getValue());
//      }
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//    return values;
//  }
//
//  public Set<byte[]> getlatestValues(long limit) {
//    Set<byte[]> values = new LinkedHashSet<>();
//    try (DBIterator iterator = db.iterator()) {
//      iterator.seekToLast();
//      while (iterator.hasPrev() && values.size() < limit) {
//        Map.Entry<byte[], byte[]> entry = iterator.prev(); // ✅ Correct: backward iteration
//        values.add(entry.getValue());
//      }
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//    return values;
//  }
//}
//
