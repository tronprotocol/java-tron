package org.tron.plugins;

import static org.fusesource.leveldbjni.JniDBFactory.asString;
import static org.fusesource.leveldbjni.JniDBFactory.bytes;
import static org.fusesource.leveldbjni.JniDBFactory.factory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.Callable;
import me.tongfei.progressbar.ProgressBar;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBComparator;
import org.iq80.leveldb.DBException;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.ReadOptions;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "check", description = "DB run env check.")
public class DbCheck implements Callable<Integer> {

  @CommandLine.Spec
  CommandLine.Model.CommandSpec spec;

  @CommandLine.Option(names = {"-h", "--help"}, help = true, description = "display a help message")
  boolean help;

  @Override
  public Integer call() throws Exception {
    if (help) {
      spec.commandLine().usage(System.out);
      return 0;
    }

    List<T> list = new ArrayList<>();
    list.add(new TestOpen());
    list.add(new TestCurd());
    list.add(new TestBloomFilter());
    list.add(new TestIterator());
    list.add(new TestWriteBatch());
    list.add(new TestSeekAndIterator());
    list.add(new TestReuseLogs());
    list.add(new TestMaxFileSize());

    ProgressBar.wrap(list.stream(), "db check").forEach(t -> {
      try {
        t.test();
      } catch (IOException e) {
        spec.commandLine().getErr().println(e);
      }
    });
    spec.commandLine().getOut().println("db check done.");
    deleteDir(new File("test-data"));
    return 0;
  }

  private  boolean deleteDir(File dir) {
    if (dir.isDirectory()) {
      String[] children = dir.list();
      assert children != null;
      for (String child : children) {
        boolean success = deleteDir(new File(dir, child));
        if (!success) {
          spec.commandLine().getErr().println("can't delete dir:" + dir);
          return false;
        }
      }
    }
    return dir.delete();
  }

  interface T {

    void test() throws IOException, DBException;

  }

  static class TestOpen extends Test {

    @Override
    public void test() throws IOException, DBException {
      Options options = new Options().createIfMissing(true);

      DB db = factory.open(allocate(), options);
      db.close();
    }
  }

  static class TestCurd extends Test {

    @Override
    public void test() throws IOException, DBException {
      Options options = new Options().createIfMissing(true).bitsPerKey(10).paranoidChecks(true);

      DB db = factory.open(allocate(), options);

      db.put(bytes("Tampa"), bytes("green"));
      db.put(bytes("London"), bytes("red"));
      db.put(bytes("New York"), bytes("blue"));

      ReadOptions ro = new ReadOptions().fillCache(true).verifyChecksums(true);
      assertEquals(db.get(bytes("Tampa"), ro), bytes("green"));
      assertEquals(db.get(bytes("London"), ro), bytes("red"));
      assertEquals(db.get(bytes("New York"), ro), bytes("blue"));

      WriteOptions wo = new WriteOptions().sync(false);
      db.delete(bytes("New York"), wo);
      assertNull(db.get(bytes("New York"), ro));

      // leveldb does not consider deleting something that does not exist an error.
      db.delete(bytes("New York"), wo);

      db.close();
    }
  }

  static class TestBloomFilter extends Test {

    @Override
    public void test() throws IOException, DBException {

      try (DB db = factory.open(allocate(),
          new Options().createIfMissing(true).bitsPerKey(10))) {
        for (int i = 0; i < 100; i++) {
          String v = UUID.randomUUID().toString();
          db.put(bytes(v), bytes(v));
        }
      }
    }
  }

  static class TestIterator extends Test {

    @Override
    public void test() throws IOException, DBException {

      Options options = new Options().createIfMissing(true);
      DB db = factory.open(allocate(), options);

      db.put(bytes("Tampa"), bytes("green"));
      db.put(bytes("London"), bytes("red"));
      db.put(bytes("New York"), bytes("blue"));

      ArrayList<String> expecting = new ArrayList<>();
      expecting.add("London");
      expecting.add("New York");
      expecting.add("Tampa");

      ArrayList<String> actual = new ArrayList<>();

      DBIterator iterator = db.iterator();
      for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
        actual.add(asString(iterator.peekNext().getKey()));
      }
      iterator.close();
      if (!expecting.equals(actual)) {
        throw new RuntimeException();
      }

      db.close();
    }
  }

  static class TestWriteBatch extends Test {

    @Override
    public void test() throws IOException, DBException {

      Options options = new Options().createIfMissing(true);
      DB db = factory.open(allocate(), options);

      db.put(bytes("NA"), bytes("Na"));

      WriteBatch batch = db.createWriteBatch();
      batch.delete(bytes("NA"));
      batch.put(bytes("Tampa"), bytes("green"));
      batch.put(bytes("London"), bytes("red"));
      batch.put(bytes("New York"), bytes("blue"));
      db.write(batch);
      batch.close();

      ArrayList<String> expecting = new ArrayList<>();
      expecting.add("London");
      expecting.add("New York");
      expecting.add("Tampa");

      ArrayList<String> actual = new ArrayList<>();

      DBIterator iterator = db.iterator();
      for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
        actual.add(asString(iterator.peekNext().getKey()));
      }
      iterator.close();
      if (!expecting.equals(actual)) {
        throw new RuntimeException();
      }
      db.close();
    }

  }

  static class TestSeekAndIterator extends Test {

    @Override
    public void test() throws IOException, DBException {
      final byte[] key_001 = newKey((byte) 1);
      final byte[] key_025 = newKey((byte) 25);
      final byte[] key_050 = newKey((byte) 50);
      final byte[] key_055 = newKey((byte) 55);
      final byte[] key_065 = newKey((byte) 65);
      final byte[] key_075 = newKey((byte) 75);
      final byte[] key_100 = newKey((byte) 100);
      final byte[] value_025 = bytes("25");
      final byte[] value_050 = bytes("50");
      final byte[] value_075 = bytes("75");


      Options options = new Options().createIfMissing(true);
      options.comparator(byteComparator);

      DB db = factory.open(allocate(), options);

      db.put(key_025, value_025);
      db.put(key_050, value_050);
      db.put(key_075, value_075);

      DBIterator it = db.iterator();

      //
      // check hasNext:
      //
      it.seek(key_001);
      assertTrue(it.hasNext());
      assertEquals(key_025, it.key());
      assertEquals(value_025, it.value());


      it.seek(key_025);
      assertTrue(it.hasNext());
      assertEquals(key_025, it.key());
      assertEquals(value_025, it.value());

      it.seek(key_050);
      assertTrue(it.hasNext());
      assertEquals(key_050, it.key());
      assertEquals(value_050, it.value());

      it.seek(key_055);
      assertTrue(it.hasNext());
      assertEquals(key_075, it.key());
      assertEquals(value_075, it.value());

      it.seek(key_065);
      assertTrue(it.hasNext());
      assertEquals(key_075, it.key());
      assertEquals(value_075, it.value());

      it.seek(key_075);
      assertTrue(it.hasNext());
      assertEquals(key_075, it.key());
      assertEquals(value_075, it.value());

      it.seek(key_100);
      assertFalse(it.hasNext());
      Map.Entry<byte[], byte[]> entry;
      //
      // check next:
      //
      it.seek(key_001);
      entry = it.next();
      assertEquals(key_025, entry.getKey());
      assertEquals(value_025, entry.getValue());

      it.seek(key_025);
      entry = it.next();
      assertEquals(key_025, entry.getKey());
      assertEquals(value_025, entry.getValue());

      it.seek(key_050);
      entry = it.next();
      assertEquals(key_050, entry.getKey());
      assertEquals(value_050, entry.getValue());

      it.seek(key_055);
      entry = it.next();
      assertEquals(key_075, entry.getKey());
      assertEquals(value_075, entry.getValue());

      it.seek(key_065);
      entry = it.next();
      assertEquals(key_075, entry.getKey());
      assertEquals(value_075, entry.getValue());

      it.seek(key_075);
      entry = it.next();
      assertEquals(key_075, entry.getKey());
      assertEquals(value_075, entry.getValue());

      it.seek(key_100);
      try {
        it.next();
      } catch (NoSuchElementException ex) {
        assertTrue(true);
      }

      //
      // check peekNext:
      //
      it.seek(key_001);
      entry = it.peekNext();
      assertEquals(key_025, entry.getKey());
      assertEquals(value_025, entry.getValue());

      it.seek(key_025);
      entry = it.peekNext();
      assertEquals(key_025, entry.getKey());
      assertEquals(value_025, entry.getValue());

      it.seek(key_050);
      entry = it.peekNext();
      assertEquals(key_050, entry.getKey());
      assertEquals(value_050, entry.getValue());

      it.seek(key_055);
      entry = it.peekNext();
      assertEquals(key_075, entry.getKey());
      assertEquals(value_075, entry.getValue());

      it.seek(key_065);
      entry = it.peekNext();
      assertEquals(key_075, entry.getKey());
      assertEquals(value_075, entry.getValue());

      it.seek(key_075);
      entry = it.peekNext();
      assertEquals(key_075, entry.getKey());
      assertEquals(value_075, entry.getValue());

      it.seek(key_100);
      try {
        it.peekNext();
      } catch (NoSuchElementException ex) {
        assertTrue(true);
      }

      //
      // check hasPrev
      //
      it.seek(key_001);
      assertTrue(it.hasPrev());
      it.seek(key_025);
      assertTrue(it.hasPrev());
      it.seek(key_050);
      assertTrue(it.hasPrev());
      it.seek(key_055);
      assertTrue(it.hasPrev());
      it.seek(key_075);
      assertTrue(it.hasPrev());
      it.seek(key_100);
      assertFalse(it.hasPrev());

      //
      // check prev:
      //
      it.seekToFirst();
      try {
        it.prev(); // return head point to null
        assertFalse(it.Valid());
        it.prev();
      } catch (NoSuchElementException ex) {
        assertTrue(true);
      }

      it.seek(key_025);
      entry = it.prev();
      assertEquals(key_025, entry.getKey());
      assertEquals(value_025, entry.getValue());

      it.seek(key_050);
      it.prev();
      assertEquals(key_025, it.key());
      assertEquals(value_025, it.value());

      it.seek(key_055);
      it.prev();
      assertEquals(key_050, it.key());
      assertEquals(value_050, it.value());

      it.seek(key_065);
      entry = it.prev();
      assertEquals(key_075, entry.getKey());
      assertEquals(value_075, entry.getValue());

      it.seek(key_075);
      entry = it.prev();
      assertEquals(key_050, it.key());
      assertEquals(value_050, it.value());
      assertEquals(key_075, entry.getKey());
      assertEquals(value_075, entry.getValue());

      it.seek(key_100);
      try {
        it.prev();
      } catch (NoSuchElementException ex) {
        assertTrue(true);
      }

      //
      // check peekPrev:
      //
      it.seek(key_001);
      entry = it.peekPrev();
      assertEquals(key_025, entry.getKey());
      assertEquals(value_025, entry.getValue());

      it.seek(key_025);
      entry = it.peekPrev();
      assertEquals(key_025, entry.getKey());
      assertEquals(value_025, entry.getValue());


      it.seek(key_050);
      entry = it.peekPrev();
      assertEquals(key_050, entry.getKey());
      assertEquals(value_050, entry.getValue());

      it.seek(key_055);
      entry = it.peekPrev();
      assertEquals(key_075, entry.getKey());
      assertEquals(value_075, entry.getValue());

      it.seek(key_065);
      entry = it.peekPrev();
      assertEquals(key_075, entry.getKey());
      assertEquals(value_075, entry.getValue());

      it.seek(key_075);
      entry = it.peekPrev();
      assertEquals(key_075, entry.getKey());
      assertEquals(value_075, entry.getValue());

      it.seek(key_100);
      try {
        it.peekPrev();
      } catch (NoSuchElementException e) {
        assertTrue(true);
      }

      it.close();
      db.close();
    }
  }


  static class TestReuseLogs extends Test {

    @Override
    public void test() throws IOException, DBException {
      Options options = new Options().createIfMissing(true).reuseLogs(true);
      File path = allocate();
      DB db = factory.open(path, options);
      db.put("halibobo".getBytes(StandardCharsets.UTF_8), "hello".getBytes(StandardCharsets.UTF_8));
      db.close();
      DB reopenDb = factory.open(path, options);
      assertTrue(Arrays.equals("hello".getBytes(StandardCharsets.UTF_8),
          reopenDb.get("halibobo".getBytes(StandardCharsets.UTF_8))));
      reopenDb.close();
    }
  }

  static class TestMaxFileSize extends Test {

    @Override
    public void test() throws IOException, DBException {
      Options options = new Options().createIfMissing(true).maxFileSize(4 * 1024 * 1024);
      DB db = factory.open(allocate(), options);
      for (int i = 0; i < 1000000; i++) {
        byte[] bytes = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
        db.put(bytes, bytes);
      }
      db.close();
    }
  }

  abstract static class Test implements T {

    DBComparator byteComparator = new DBComparator() {

      public int compare(byte[] key1, byte[] key2) {
        return key1[0] - key2[0];
      }

      public String name() {
        return "ByteComparator";
      }

      public byte[] findShortestSeparator(byte[] start, byte[] limit) {
        return start;
      }

      public byte[] findShortSuccessor(byte[] key) {
        return key;
      }
    };

    File allocate() {
      File path = new File("test-data", name());
      if (path.mkdirs()) {
        path.deleteOnExit();
      }
      return path;
    }

    byte[] newKey(byte value) {
      final byte[] result = new byte[1];
      result[0] = value;
      return result;
    }

    void assertNull(byte[] arg) {
      if (arg != null) {
        throw new RuntimeException();
      }
    }

    void assertTrue(boolean b) {
      if (!b) {
        throw new RuntimeException();
      }
    }

    void assertFalse(boolean b) {
      if (b) {
        throw new RuntimeException();
      }
    }

    void assertEquals(byte[] arg1, byte[] arg2) {
      if (!Arrays.equals(arg1, arg2)) {
        throw new RuntimeException();
      }
    }

    public String name() {
      return this.getClass().getSimpleName();
    }
  }

}
