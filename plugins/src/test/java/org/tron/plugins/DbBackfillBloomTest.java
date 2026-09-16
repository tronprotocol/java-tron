package org.tron.plugins;

import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import com.google.protobuf.ByteString;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import me.tongfei.progressbar.ProgressBar;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;
import org.tron.common.TestConstants;
import org.tron.common.arch.Arch;
import org.tron.common.bloom.BloomUtils;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.TransactionRetCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.SectionBloomStore;
import org.tron.plugins.utils.DBUtils;
import org.tron.plugins.utils.db.DBInterface;
import org.tron.plugins.utils.db.DBIterator;
import org.tron.plugins.utils.db.DbTool;
import org.tron.plugins.utils.db.DbTool.DbType;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfo.Log;
import org.tron.protos.Protocol.TransactionRet;
import picocli.CommandLine;

public class DbBackfillBloomTest {

  private static final byte[] HEADER_KEY =
      "latest_block_header_number".getBytes(StandardCharsets.UTF_8);

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private File databaseRoot;
  private DBInterface transactions;
  private DBInterface bloom;
  private DBInterface properties;
  private DBIterator iterator;
  private TransactionRet transactionRet;
  private StringWriter output;
  private StringWriter errors;
  private Logger logger;
  private ListAppender<ILoggingEvent> appender;

  @Before
  public void setUp() throws Exception {
    databaseRoot = temporaryFolder.newFolder();
    Files.createDirectories(new File(databaseRoot, "properties").toPath());
    Files.createDirectories(new File(databaseRoot, "transactionRetStore").toPath());
    transactions = mock(DBInterface.class);
    bloom = mock(DBInterface.class);
    properties = mock(DBInterface.class);
    iterator = mock(DBIterator.class);
    when(properties.get(aryEq(HEADER_KEY))).thenReturn(ByteArray.fromLong(40));
    when(transactions.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true);
    when(iterator.getKey()).thenReturn(ByteArray.fromLong(1));
    transactionRet = createTransactionRet(81);
    logger = (Logger) LoggerFactory.getLogger("backfill-bloom");
    appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
  }

  @After
  public void tearDown() {
    logger.detachAppender(appender);
    appender.stop();
    DbTool.close();
  }

  @Test
  public void testHelp() throws Exception {
    PrintStream originalOut = System.out;
    ByteArrayOutputStream help = new ByteArrayOutputStream();
    try (PrintStream capture = new PrintStream(help, true, "UTF-8")) {
      System.setOut(capture);
      Assert.assertEquals(0, execute("-h"));
      String helpText = help.toString("UTF-8").replaceAll("\\s+", " ");
      Assert.assertTrue(helpText
          .contains("The same block range can be safely rerun after interruption."));
      Assert.assertTrue(helpText.contains("Default or 0: earliest non-zero block"));
      Assert.assertTrue(helpText.contains("Default or 0: latest persisted block header"));
    } finally {
      System.setOut(originalOut);
    }
  }

  @Test
  public void testInvalidParameters() {
    try (MockedStatic<DbTool> dbTool = mockDatabases()) {
      for (String[] options : new String[][] {{"-s", "-1"}, {"-e", "-1"},
          {"-c", "0"}, {"-c", "129"}}) {
        Assert.assertEquals(Arrays.toString(options), 1, execute(options));
        Assert.assertFalse(errors.toString().isEmpty());
        if ("-e".equals(options[0])) {
          Assert.assertTrue(errors.toString().contains("End block must be >= zero"));
        }
        assertNoDatabasesOpened(dbTool);
      }
      Assert.assertEquals(1, execute("-s", "41", "-e", "40"));
      Assert.assertTrue(errors.toString().contains("End block"));
    }
  }

  @Test
  public void testInvalidDatabasePaths() throws Exception {
    File missingProperties = temporaryFolder.newFolder();
    Files.createDirectories(new File(missingProperties, "transactionRetStore").toPath());
    File missingTransactions = temporaryFolder.newFolder();
    Files.createDirectories(new File(missingTransactions, "properties").toPath());
    Assert.assertTrue(new File(databaseRoot, "section-bloom").createNewFile());
    File[] roots = {new File(temporaryFolder.getRoot(), "missing"), temporaryFolder.newFile(),
        missingProperties, missingTransactions, databaseRoot};
    String[] messages = {"Database directory does not exist", "Database directory does not exist",
        "Required database 'properties'", "Required database 'transactionRetStore'",
        "Database 'section-bloom' is not a directory"};
    for (int i = 0; i < roots.length; i++) {
      databaseRoot = roots[i];
      try (MockedStatic<DbTool> dbTool = mockDatabases()) {
        Assert.assertEquals(1, execute());
        Assert.assertTrue(errors.toString(), errors.toString().contains(messages[i]));
        assertNoDatabasesOpened(dbTool);
      }
    }
  }

  @Test
  public void testDatabaseInitializationFailure() {
    try (MockedStatic<DbTool> dbTool = mockDatabases()) {
      dbTool.when(() -> DbTool.getDB(anyString(), anyString(), any(DbType.class)))
          .thenThrow(new RuntimeException("open failed"));
      Assert.assertEquals(1, execute());
      Assert.assertTrue(errors.toString().contains("Failed to initialize database connections"));
    }
  }

  @Test
  public void testMissingOrUnreadableHeader() {
    when(properties.get(aryEq("LATEST_SOLIDIFIED_BLOCK_NUM".getBytes(StandardCharsets.UTF_8))))
        .thenReturn(ByteArray.fromLong(22));
    when(properties.get(aryEq(HEADER_KEY))).thenReturn(null)
        .thenThrow(new RuntimeException("read failed"));
    try (MockedStatic<DbTool> ignored = mockDatabases()) {
      for (String message : new String[] {"Latest persisted block header number does not exist",
          "Failed to read latest persisted block header number"}) {
        Assert.assertEquals(1, execute());
        Assert.assertTrue(errors.toString().contains(message));
        Assert.assertFalse(output.toString().contains("Starting SectionBloom backfill"));
      }
    }
  }

  @Test
  public void testUnavailableTransactionResults() throws Exception {
    try (MockedStatic<DbTool> ignored = mockDatabases()) {
      when(iterator.hasNext()).thenReturn(false);
      Assert.assertEquals(1, execute());
      Assert.assertTrue(errors.toString().contains("does not contain any non-zero block"));
      verify(iterator).seek(aryEq(ByteArray.fromLong(1)));
      verify(iterator).close();
      when(transactions.iterator()).thenThrow(new RuntimeException("iterator failed"));
      Assert.assertEquals(1, execute());
      Assert.assertTrue(errors.toString().contains("Failed to determine the first transaction"));
    }
  }

  @Test(timeout = 30_000)
  public void testProgressAndSummary() {
    when(properties.get(aryEq(HEADER_KEY))).thenReturn(ByteArray.fromLong(10_000));
    try (MockedStatic<DbTool> ignored = mockDatabases()) {
      Assert.assertEquals(0, execute("-c", "1"));
      Assert.assertTrue(output.toString().contains("Total blocks scanned: 10000"));
      Assert.assertTrue(output.toString().contains("Successfully processed: 10000"));
      Assert.assertTrue(output.toString().contains("Success rate: 100.00%"));
      Assert.assertTrue(output.toString().contains("Backfill completed successfully!"));
      Assert.assertFalse(output.toString().contains("Backfill progress:"));
      Assert.assertEquals(1L, appender.list.stream().map(ILoggingEvent::getFormattedMessage)
          .filter(message -> message.startsWith("Backfill progress: 10000/10000 blocks (100.00%)"))
          .count());
    }
  }

  @Test(timeout = 30_000)
  public void testWorkerErrorsFailSummaryAndPreserveCauses() {
    when(properties.get(aryEq(HEADER_KEY))).thenReturn(ByteArray.fromLong(2050));
    Error[] failures = {new AssertionError("worker failed"),
        new NoClassDefFoundError("missing worker dependency")};
    when(transactions.get(aryEq(ByteArray.fromLong(1)))).thenThrow(failures[0]);
    when(transactions.get(aryEq(ByteArray.fromLong(2048)))).thenThrow(failures[1]);
    try (MockedStatic<DbTool> ignored = mockDatabases()) {
      Assert.assertEquals(1, execute("-c", "2"));
      Assert.assertTrue(output.toString().contains("Errors encountered: 2"));
      Assert.assertTrue(output.toString().contains("Total blocks scanned: 2"));
      Assert.assertTrue(output.toString().contains("Successfully processed: 0"));
      Assert.assertTrue(output.toString().contains("Backfill failed;"));
      Assert.assertFalse(output.toString().contains("Backfill completed successfully!"));
      verify(transactions, never()).get(aryEq(ByteArray.fromLong(2)));
      verify(transactions, never()).get(aryEq(ByteArray.fromLong(2049)));
      for (int i = 0; i < failures.length; i++) {
        String message = "Error processing section " + (i == 0 ? "1 to 2047" : "2048 to 2050");
        Assert.assertTrue(errors.toString().contains(message));
        ILoggingEvent event = appender.list.stream()
            .filter(entry -> message.equals(entry.getFormattedMessage())).findFirst().orElse(null);
        Assert.assertNotNull(event);
        ThrowableProxy throwable = (ThrowableProxy) event.getThrowableProxy();
        Assert.assertNotNull(throwable);
        Assert.assertSame(failures[i], throwable.getThrowable().getCause());
      }
    }
  }

  @Test(timeout = 30_000)
  public void testSummaryReportsProgressFailureWithoutBlockErrors() {
    RuntimeException failure = new RuntimeException("progress close failed");
    try (MockedStatic<DbTool> ignored = mockDatabases();
        MockedConstruction<ProgressBar> progressBars = mockConstruction(ProgressBar.class,
            (bar, context) -> doThrow(failure).when(bar).close())) {
      Assert.assertEquals(1, execute("-e", "1"));
      Assert.assertEquals(1, progressBars.constructed().size());
      Assert.assertTrue(output.toString().contains("Successfully processed: 1"));
      Assert.assertTrue(output.toString().contains("Errors encountered: 0"));
      Assert.assertTrue(output.toString().contains("Backfill failed;"));
      Assert.assertFalse(output.toString().contains("Backfill completed successfully!"));
      Assert.assertTrue(errors.toString().contains("Error in progress tracking"));
    }
  }

  @Test(timeout = 30_000)
  public void testDatabaseWriteFailurePreservesOriginalCause() {
    when(transactions.get(aryEq(ByteArray.fromLong(1)))).thenReturn(transactionRet.toByteArray());
    RuntimeException failure = new RuntimeException("section-bloom", new IOException("disk full"));
    doThrow(failure).when(bloom).put(any(byte[].class), any(byte[].class));
    try (MockedStatic<DbTool> ignored = mockDatabases()) {
      Assert.assertEquals(1, execute("-e", "1"));
      verify(bloom).put(any(byte[].class), any(byte[].class));
      Assert.assertTrue(errors.toString().contains("Error processing block 1"));
      Assert.assertFalse(output.toString().contains("Error processing block 1"));
      Assert.assertTrue(output.toString().contains("Errors encountered: 1"));
      Assert.assertTrue(output.toString().contains("Successfully processed: 0"));
      Assert.assertFalse(output.toString().contains("Backfill completed successfully!"));
      ILoggingEvent event = appender.list.stream()
          .filter(entry -> "Error processing block 1".equals(entry.getFormattedMessage()))
          .findFirst().orElse(null);
      Assert.assertNotNull(event);
      ThrowableProxy throwable = (ThrowableProxy) event.getThrowableProxy();
      Assert.assertNotNull(throwable);
      Assert.assertSame(failure, throwable.getThrowable());
      Assert.assertEquals("disk full", throwable.getCause().getMessage());
    }
  }

  @Test(timeout = 30_000)
  public void testMalformedProtobufFailsWithoutWritingBloom() throws Exception {
    writeSource(1, 1, 1);
    openDb("transactionRetStore").put(ByteArray.fromLong(1), new byte[] {(byte) 0x80});
    DbTool.close();
    Assert.assertEquals(1, execute());
    Assert.assertTrue(errors.toString().contains("Error processing block 1"));
    Assert.assertTrue(readBloomEntries().isEmpty());
  }

  @Test(timeout = 30_000)
  public void testEngineSelectionUsesExistingBloomOrTransactionStore() throws Exception {
    for (boolean existingBloom : new boolean[] {false, true}) {
      for (String engine : new String[] {"LEVELDB", "ROCKSDB", null}) {
        databaseRoot = temporaryFolder.newFolder();
        writeEngine("transactionRetStore", engine);
        writeEngine("properties", engine);
        if (existingBloom) {
          writeEngine("section-bloom", engine);
        }
        DbType type = "ROCKSDB".equals(engine) ? DbType.RocksDB : DbType.LevelDB;
        String reference = existingBloom ? "section-bloom" : "transactionRetStore";
        try (MockedStatic<Arch> arch = mockStatic(Arch.class, CALLS_REAL_METHODS);
            MockedStatic<DbTool> dbTool = mockDatabases()) {
          arch.when(Arch::getOsArch).thenReturn("amd64");
          dbTool.when(() -> DbTool.getDbType(anyString(), anyString())).thenCallRealMethod();
          Assert.assertEquals(0, execute("-e", "1"));
          dbTool.verify(() -> DbTool.getDbType(databaseRoot.toString(), reference));
          dbTool.verify(() -> DbTool.getDbType(anyString(), anyString()), times(1));
          for (String name : new String[] {"transactionRetStore", "section-bloom", "properties"}) {
            dbTool.verify(() -> DbTool.getDB(databaseRoot.toString(), name, type));
          }
          arch.verify(() -> Arch.throwIfUnsupportedArm64Exception(anyString()),
              type == DbType.LevelDB ? times(1) : never());
        }
      }
    }
  }

  @Test(timeout = 30_000)
  public void testArmRejectsLegacyEnginesBeforeOpeningDatabases() throws Exception {
    for (boolean existingBloom : new boolean[] {false, true}) {
      for (String engine : new String[] {"LEVELDB", null}) {
        databaseRoot = temporaryFolder.newFolder();
        writeEngine("transactionRetStore", engine);
        writeEngine("properties", engine);
        if (existingBloom) {
          writeEngine("section-bloom", engine);
        }
        String reference = existingBloom ? "section-bloom" : "transactionRetStore";
        try (MockedStatic<Arch> arch = mockStatic(Arch.class, CALLS_REAL_METHODS);
            MockedStatic<DbTool> dbTool = mockDatabases()) {
          arch.when(Arch::getOsArch).thenReturn(existingBloom ? "arm64" : "aarch64");
          dbTool.when(() -> DbTool.getDbType(anyString(), anyString())).thenCallRealMethod();
          Assert.assertEquals(1, execute());
          Assert.assertTrue(errors.toString().contains("LevelDB database '" + reference
              + "': unsupported"));
          assertNoDatabasesOpened(dbTool);
          Assert.assertFalse(new File(databaseRoot, reference + "/CURRENT").exists());
          Assert.assertEquals(existingBloom, new File(databaseRoot, "section-bloom").exists());
        }
      }
    }
  }

  @Test(timeout = 60_000)
  public void testRealBackfillMatchesNodeAcrossSectionBoundaryAndRerun() throws Exception {
    TransactionRet second = createTransactionRet(82);
    TransactionRet empty = TransactionRet.newBuilder()
        .addTransactioninfo(TransactionInfo.getDefaultInstance()).build();
    openDb("transactionRetStore").put(ByteArray.fromLong(2047), transactionRet.toByteArray());
    openDb("transactionRetStore").put(ByteArray.fromLong(2048), second.toByteArray());
    openDb("transactionRetStore").put(ByteArray.fromLong(2049), empty.toByteArray());
    openDb("properties").put(HEADER_KEY, ByteArray.fromLong(2050));
    DbTool.close();
    Assert.assertFalse(new File(databaseRoot, "section-bloom").exists());

    Args.setParam(new String[] {"--output-directory", temporaryFolder.newFolder().toString(),
        "--storage-db-engine", "ROCKSDB"}, TestConstants.TEST_CONF);
    SectionBloomStore nodeStore = null;
    try {
      nodeStore = new SectionBloomStore("section-bloom");
      nodeStore.initBlockSection(new TransactionRetCapsule(transactionRet.toByteArray()));
      nodeStore.write(2047);
      nodeStore.initBlockSection(new TransactionRetCapsule(second.toByteArray()));
      nodeStore.write(2048);
      nodeStore.initBlockSection(new TransactionRetCapsule(empty.toByteArray()));
      nodeStore.write(2049);
      for (int run = 0; run < 3; run++) {
        if (run == 1) {
          // Seed only the older bit so the existing store must also be backfilled.
          nodeStore.initBlockSection(new TransactionRetCapsule(transactionRet.toByteArray()));
          nodeStore.write(7);
          BitSet existing = new BitSet();
          existing.set(7);
          writeBloom(ByteUtil.compress(existing.toByteArray()));
        }
        Assert.assertEquals(0, execute("-c", "2"));
        Assert.assertTrue(output.toString().contains("Blocks with logs: 2"));
        Assert.assertTrue(output.toString().contains("Successfully processed: 4"));
        Assert.assertTrue(output.toString().contains("Processing 2 sections with 2 threads"));
        Assert.assertEquals(DbType.RocksDB,
            DbTool.getDbType(databaseRoot.toString(), "section-bloom"));
        Map<ByteString, byte[]> expected = readNodeSections(nodeStore);
        Map<ByteString, byte[]> actual = readBloomEntries();
        Assert.assertFalse(expected.isEmpty());
        Assert.assertEquals(expected.keySet(), actual.keySet());
        for (Map.Entry<ByteString, byte[]> entry : expected.entrySet()) {
          Assert.assertArrayEquals(entry.getValue(), actual.get(entry.getKey()));
        }
      }
    } finally {
      try {
        if (nodeStore != null) {
          nodeStore.close();
        }
      } finally {
        Args.clearParam();
      }
    }
  }

  @Test(timeout = 30_000)
  public void testEndBlockUsesPersistedHead() throws Exception {
    String[][] options = {{}, {"-e", "30"}, {"-e", "40"}, {"-e", "60"}, {"-e", "0"}};
    int[] expectedEnds = {40, 30, 40, 40, 40};
    for (int i = 0; i < options.length; i++) {
      databaseRoot = temporaryFolder.newFolder();
      writeSource(1, 41, 40);
      Assert.assertEquals(0, execute(options[i]));
      Assert.assertTrue(output.toString().contains("Total blocks scanned: " + expectedEnds[i]));
      assertIndexedBlocks(1, expectedEnds[i]);
      if (i == 3) {
        Assert.assertTrue(output.toString().contains("number 40; using 40 instead."));
      }
    }
  }

  @Test(timeout = 30_000)
  public void testStartBlockUsesFirstNonZeroTransactionResult() throws Exception {
    writeSource(25, 41, 40);
    openDb("transactionRetStore").put(ByteArray.fromLong(0), transactionRet.toByteArray());
    DbTool.close();
    for (String[] options : new String[][] {{"-e", "30"}, {"-s", "0", "-e", "30"},
        {"-s", "1", "-e", "30"}}) {
      Assert.assertEquals(0, execute(options));
      Assert.assertTrue(output.toString().contains("Total blocks scanned: 6"));
      assertIndexedBlocks(25, 30);
    }
    Assert.assertTrue(output.toString().contains(
        "Start block 1 is earlier than the first available transaction result block 25"));
  }

  @Test(timeout = 30_000)
  public void testEmptyBloomValuesAreRebuilt() throws Exception {
    writeSource(1, 1, 1);
    for (byte[] value : new byte[][] {new byte[0], ByteUtil.compress(new byte[0])}) {
      writeBloom(value);
      Assert.assertEquals(0, execute());
      Assert.assertTrue(output.toString().contains("Errors encountered: 0"));
      assertIndexedBlocks(1, 1);
    }
  }

  private int execute(String... options) {
    output = new StringWriter();
    errors = new StringWriter();
    appender.list.clear();
    List<String> args = new ArrayList<>(Arrays.asList("db", "backfill-bloom", "-d",
        databaseRoot.toString()));
    Collections.addAll(args, options);
    return new CommandLine(new Toolkit()).setOut(new PrintWriter(output))
        .setErr(new PrintWriter(errors)).execute(args.toArray(new String[0]));
  }

  private MockedStatic<DbTool> mockDatabases() {
    MockedStatic<DbTool> dbTool = mockStatic(DbTool.class);
    dbTool.when(() -> DbTool.getDbType(anyString(), anyString())).thenReturn(DbType.RocksDB);
    dbTool.when(() -> DbTool.getDB(anyString(), anyString(), any(DbType.class)))
        .thenAnswer(invocation -> {
          switch (invocation.getArgument(1, String.class)) {
            case "transactionRetStore":
              return transactions;
            case "section-bloom":
              return bloom;
            case "properties":
              return properties;
            default:
              throw new AssertionError("Unexpected database");
          }
        });
    return dbTool;
  }

  private void assertNoDatabasesOpened(MockedStatic<DbTool> dbTool) {
    dbTool.verify(() -> DbTool.getDB(anyString(), anyString()), never());
    dbTool.verify(() -> DbTool.getDB(anyString(), anyString(), any(DbType.class)), never());
  }

  private void writeEngine(String name, String engine) throws Exception {
    File directory = new File(databaseRoot, name);
    Files.createDirectories(directory.toPath());
    if (engine != null) {
      Files.write(directory.toPath().resolve(DBUtils.FILE_ENGINE),
          ("ENGINE=" + engine).getBytes(StandardCharsets.UTF_8));
    }
  }

  private DBInterface openDb(String name) throws Exception {
    return DbTool.getDB(databaseRoot.toString(), name, DbType.RocksDB);
  }

  private void writeSource(int first, int last, long head) throws Exception {
    try {
      DBInterface source = openDb("transactionRetStore");
      for (int block = first; block <= last; block++) {
        source.put(ByteArray.fromLong(block), transactionRet.toByteArray());
      }
      openDb("properties").put(HEADER_KEY, ByteArray.fromLong(head));
      openDb("properties").put("LATEST_SOLIDIFIED_BLOCK_NUM".getBytes(StandardCharsets.UTF_8),
          ByteArray.fromLong(StrictMath.min(22, head)));
    } finally {
      DbTool.close();
    }
  }

  private void writeBloom(byte[] value) throws Exception {
    try {
      BitSet indexes = BitSet.valueOf(BloomUtils.createBloom(transactionRet));
      Assert.assertFalse(indexes.isEmpty());
      for (int bit = indexes.nextSetBit(0); bit >= 0; bit = indexes.nextSetBit(bit + 1)) {
        openDb("section-bloom").put(bloomKey(0, bit), value);
      }
    } finally {
      DbTool.close();
    }
  }

  private void assertIndexedBlocks(int first, int last) throws Exception {
    Map<ByteString, byte[]> actual = readBloomEntries();
    BitSet indexes = BitSet.valueOf(BloomUtils.createBloom(transactionRet));
    Assert.assertFalse(indexes.isEmpty());
    Assert.assertEquals(indexes.cardinality(), actual.size());
    BitSet expected = new BitSet();
    expected.set(first, last + 1);
    for (int bit = indexes.nextSetBit(0); bit >= 0; bit = indexes.nextSetBit(bit + 1)) {
      byte[] value = actual.get(ByteString.copyFrom(bloomKey(0, bit)));
      Assert.assertNotNull(value);
      Assert.assertEquals(expected, BitSet.valueOf(ByteUtil.decompress(value)));
    }
  }

  private Map<ByteString, byte[]> readBloomEntries() throws Exception {
    Map<ByteString, byte[]> result = new HashMap<>();
    try {
      DBInterface database = DbTool.getDB(databaseRoot.toString(), "section-bloom");
      try (DBIterator entries = database.iterator()) {
        entries.seekToFirst();
        while (entries.hasNext()) {
          result.put(ByteString.copyFrom(entries.getKey()), entries.getValue());
          entries.next();
        }
      }
    } finally {
      DbTool.close();
    }
    return result;
  }

  private Map<ByteString, byte[]> readNodeSections(SectionBloomStore store) {
    Map<ByteString, byte[]> result = new HashMap<>();
    for (int section = 0; section < 2; section++) {
      for (int bit = 0; bit < 2048; bit++) {
        byte[] key = bloomKey(section, bit);
        BytesCapsule value = store.get(key);
        if (value != null) {
          result.put(ByteString.copyFrom(key), value.getData());
        }
      }
    }
    return result;
  }

  private byte[] bloomKey(int section, int bit) {
    return Long.toHexString(section * 1_000_000L + bit).getBytes(StandardCharsets.UTF_8);
  }

  private TransactionRet createTransactionRet(long seed) {
    Random random = new Random(seed);
    TransactionRet.Builder result = TransactionRet.newBuilder();
    for (int transaction = 0; transaction < 2; transaction++) {
      TransactionInfo.Builder info = TransactionInfo.newBuilder();
      for (int log = 0; log < 2; log++) {
        byte[] address = new byte[20];
        byte[] topic = new byte[32];
        random.nextBytes(address);
        random.nextBytes(topic);
        info.addLog(Log.newBuilder().setAddress(ByteString.copyFrom(address))
            .addTopics(ByteString.copyFrom(topic)));
      }
      result.addTransactioninfo(info);
    }
    return result.build();
  }
}
