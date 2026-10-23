package org.tron.core.db2.core;

import static org.tron.common.TestConstants.TEST_CONF;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.storage.rocksdb.RocksDbDataSourceImpl;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.capsule.utils.MarketUtils;
import org.tron.core.config.args.Args;
import org.tron.core.db2.common.RocksDB;
import org.tron.core.db2.common.Value.Operator;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.db2.core.Chainbase.Cursor;

public class SnapshotImplCollectTest {

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static final long LIMIT = 100L;
  private static final byte[] PREFIX = bytes("p-");
  private static final byte[] START_KEY = bytes("p-1");
  private static final String PUT = Operator.PUT.name();

  private static final Map<String, String> LAYER0_ENTRIES = ImmutableMap.of(
      "p-1", "layer0", "p-9", "layer0-shared");
  private static final Map<String, String> LAYER0_STATE = ImmutableMap.of(
      "p-0", "root", "p-1", "layer0", "p-9", "layer0-shared");
  private static final Set<String> LAYER0_VALUES = ImmutableSet.of("layer0", "layer0-shared");
  private static final Map<String, String> LAYER0_OPERATORS = ImmutableMap.of(
      "p-1", PUT, "p-9", PUT);

  private static final Map<String, String> LAYER1_ENTRIES = ImmutableMap.of(
      "p-1", "layer0", "p-2", "layer1", "p-9", "layer1-shared");
  private static final Map<String, String> LAYER1_STATE = ImmutableMap.of(
      "p-0", "root", "p-1", "layer0", "p-2", "layer1", "p-9", "layer1-shared");
  private static final Set<String> LAYER1_VALUES = ImmutableSet.of(
      "layer0", "layer1", "layer1-shared");
  private static final Map<String, String> LAYER1_OPERATORS = ImmutableMap.of(
      "p-1", PUT, "p-2", PUT, "p-9", PUT);

  private static final Map<String, String> LAYER2_STATE = ImmutableMap.of(
      "p-1", "layer0", "p-2", "layer1", "p-3", "layer2", "p-9", "layer2-shared");
  private static final Set<String> LAYER2_VALUES = ImmutableSet.of(
      "layer0", "layer1", "layer2", "layer2-shared");
  private static final Map<String, String> LAYER2_OPERATORS = ImmutableMap.of(
      "p-0", Operator.DELETE.name(), "p-1", PUT, "p-2", PUT, "p-3", PUT, "p-9", PUT);

  private Chainbase chainbase;
  private SnapshotRoot root;
  private SnapshotImpl layer0;
  private SnapshotImpl layer1;
  private SnapshotImpl layer2;

  @BeforeClass
  public static void initArgs() throws IOException {
    Args.setParam(new String[] {"--output-directory",
        temporaryFolder.newFolder().toString()}, TEST_CONF);
  }

  @AfterClass
  public static void clearArgs() {
    Args.clearParam();
  }

  @Before
  public void buildChain() throws IOException {
    RocksDbDataSourceImpl dataSource = new RocksDbDataSourceImpl(
        temporaryFolder.newFolder().toString(), "snapshot-collect");
    chainbase = new Chainbase(new SnapshotRoot(new RocksDB(dataSource)));
    root = (SnapshotRoot) chainbase.getHead();
    root.put(bytes("p-0"), bytes("root"));

    layer0 = advance();
    layer0.put(bytes("p-1"), bytes("layer0"));
    layer0.put(bytes("p-9"), bytes("layer0-shared"));

    layer1 = advance();
    layer1.put(bytes("p-2"), bytes("layer1"));
    layer1.put(bytes("p-9"), bytes("layer1-shared"));

    layer2 = advance();
    layer2.put(bytes("p-3"), bytes("layer2"));
    layer2.put(bytes("p-9"), bytes("layer2-shared"));
    layer2.remove(bytes("p-0"));

    layer2.updateSolidity();
    layer2.updateSolidity();
    Assert.assertSame(layer1, root.getSolidity());
  }

  @After
  public void closeChain() {
    chainbase.setCursor(Cursor.HEAD);
    chainbase.close();
  }

  @Test
  public void solidityRangeReadsExcludeUnsolidifiedLayers() {
    chainbase.setCursor(Cursor.SOLIDITY);
    Assert.assertSame(layer1, chainbase.getHead());
    assertRangeReads(LAYER1_ENTRIES, LAYER1_VALUES, LAYER1_STATE, LAYER1_OPERATORS);
  }

  @Test
  public void pbftRangeReadsExcludeLayersAbovePbftSnapshot() {
    chainbase.setCursor(Cursor.PBFT, 2L);
    Assert.assertSame(layer0, chainbase.getHead());
    assertRangeReads(LAYER0_ENTRIES, LAYER0_VALUES, LAYER0_STATE, LAYER0_OPERATORS);
  }

  @Test
  public void collectOnSoliditySnapshotIgnoresLayersRelinkedByFlush() {
    chainbase.setCursor(Cursor.SOLIDITY);
    SnapshotImpl solidity = (SnapshotImpl) chainbase.getHead();
    chainbase.setCursor(Cursor.HEAD);

    refreshSolidifiedLayers(2);
    Assert.assertSame(root, root.getSolidity());
    Assert.assertSame(layer2, root.getNext());

    Assert.assertEquals(LAYER1_ENTRIES, collect(solidity));
    Assert.assertEquals(LAYER1_ENTRIES, collectWithPrefix(solidity));
    Assert.assertEquals(LAYER1_OPERATORS, collectUnique(solidity));
    Assert.assertEquals(LAYER1_STATE, toStrings(solidity.iterator()));
  }

  @Test
  public void headRangeReadsIncludeLatestLayer() {
    chainbase.setCursor(Cursor.HEAD);
    Assert.assertSame(layer2, chainbase.getHead());
    assertRangeReads(LAYER2_STATE, LAYER2_VALUES, LAYER2_STATE, LAYER2_OPERATORS);
  }

  @Test
  public void headRangeReadsIgnoreRetreatedLayer() {
    chainbase.setCursor(Cursor.HEAD);
    chainbase.setHead(chainbase.getHead().retreat());
    Assert.assertSame(layer1, chainbase.getHead());
    Assert.assertSame(layer2, layer1.getNext());
    assertRangeReads(LAYER1_ENTRIES, LAYER1_VALUES, LAYER1_STATE, LAYER1_OPERATORS);
  }

  @Test
  public void keysNextIsBoundedByCursorSnapshot() throws IOException {
    byte[] sellTokenId = bytes("_");
    byte[] buyTokenId = bytes("1000001");
    byte[] headKey = MarketUtils.getPairPriceHeadKey(sellTokenId, buyTokenId);
    byte[] rootKey = MarketUtils.createPairPriceKey(sellTokenId, buyTokenId, 1L, 1L);
    byte[] solidKey = MarketUtils.createPairPriceKey(sellTokenId, buyTokenId, 1L, 2L);
    byte[] aboveKey1 = MarketUtils.createPairPriceKey(sellTokenId, buyTokenId, 1L, 3L);
    byte[] aboveKey2 = MarketUtils.createPairPriceKey(sellTokenId, buyTokenId, 1L, 4L);

    Chainbase market = new Chainbase(new SnapshotRoot(new RocksDB(new RocksDbDataSourceImpl(
        temporaryFolder.newFolder().toString(), "snapshot-collect-market"))));
    try {
      market.getHead().put(rootKey, bytes("root"));
      market.setHead(market.getHead().advance());
      market.getHead().put(solidKey, bytes("solid"));
      market.setHead(market.getHead().advance());
      market.getHead().put(aboveKey1, bytes("above1"));
      market.setHead(market.getHead().advance());
      market.getHead().put(aboveKey2, bytes("above2"));
      market.getHead().remove(solidKey);
      market.getHead().updateSolidity();

      market.setCursor(Cursor.SOLIDITY);
      Assert.assertEquals(toHex(Arrays.asList(rootKey, solidKey)),
          toHex(market.getKeysNext(headKey, LIMIT)));

      market.setCursor(Cursor.HEAD);
      Assert.assertEquals(toHex(Arrays.asList(rootKey, aboveKey1, aboveKey2)),
          toHex(market.getKeysNext(headKey, LIMIT)));
    } finally {
      market.setCursor(Cursor.HEAD);
      market.close();
    }
  }

  private void assertRangeReads(Map<String, String> entries, Set<String> values,
      Map<String, String> state, Map<String, String> operators) {
    Assert.assertEquals(entries, toStrings(chainbase.getNext(START_KEY, LIMIT)));
    Assert.assertEquals(values, toStrings(chainbase.getValuesNext(START_KEY, LIMIT)));
    Assert.assertEquals(state, wrappedKeysToStrings(chainbase.prefixQuery(PREFIX)));
    Assert.assertEquals(state, toStrings(chainbase.iterator()));
    Assert.assertEquals(operators, collectUnique((SnapshotImpl) chainbase.getHead()));
  }

  private void refreshSolidifiedLayers(int flushCount) {
    SnapshotManager manager = new SnapshotManager("");
    ReflectUtils.setFieldValue(manager, "flushCount", flushCount);
    ReflectUtils.invokeMethod(manager, "refreshOne", new Class[] {Chainbase.class}, chainbase);
  }

  private SnapshotImpl advance() {
    chainbase.setHead(chainbase.getHead().advance());
    return (SnapshotImpl) chainbase.getHead();
  }

  private static Map<String, String> collect(SnapshotImpl snapshot) {
    Map<WrappedByteArray, WrappedByteArray> all = new HashMap<>();
    snapshot.collect(all);
    return wrappedToStrings(all);
  }

  private static Map<String, String> collectWithPrefix(SnapshotImpl snapshot) {
    Map<WrappedByteArray, WrappedByteArray> all = new HashMap<>();
    snapshot.collect(all, PREFIX);
    return wrappedToStrings(all);
  }

  private static Map<String, String> collectUnique(SnapshotImpl snapshot) {
    Map<WrappedByteArray, Operator> all = new HashMap<>();
    snapshot.collectUnique(all);
    Map<String, String> operators = new TreeMap<>();
    all.forEach((k, v) -> operators.put(ByteArray.toStr(k.getBytes()), v.name()));
    return operators;
  }

  private static Map<String, String> wrappedToStrings(
      Map<WrappedByteArray, WrappedByteArray> entries) {
    Map<String, String> strings = new TreeMap<>();
    entries.forEach((k, v) -> strings.put(ByteArray.toStr(k.getBytes()),
        ByteArray.toStr(v.getBytes())));
    return strings;
  }

  private static Map<String, String> wrappedKeysToStrings(Map<WrappedByteArray, byte[]> entries) {
    Map<String, String> strings = new TreeMap<>();
    entries.forEach((k, v) -> strings.put(ByteArray.toStr(k.getBytes()), ByteArray.toStr(v)));
    return strings;
  }

  private static Map<String, String> toStrings(Map<byte[], byte[]> entries) {
    Map<String, String> strings = new TreeMap<>();
    entries.forEach((k, v) -> strings.put(ByteArray.toStr(k), ByteArray.toStr(v)));
    return strings;
  }

  private static Map<String, String> toStrings(Iterator<Map.Entry<byte[], byte[]>> entries) {
    Map<String, String> strings = new TreeMap<>();
    entries.forEachRemaining(e -> strings.put(ByteArray.toStr(e.getKey()),
        ByteArray.toStr(e.getValue())));
    return strings;
  }

  private static Set<String> toStrings(Set<byte[]> values) {
    Set<String> strings = new TreeSet<>();
    values.forEach(v -> strings.add(ByteArray.toStr(v)));
    return strings;
  }

  private static List<String> toHex(List<byte[]> keys) {
    return keys.stream().map(ByteArray::toHexString).collect(Collectors.toList());
  }

  private static byte[] bytes(String value) {
    return ByteArray.fromString(value);
  }
}
