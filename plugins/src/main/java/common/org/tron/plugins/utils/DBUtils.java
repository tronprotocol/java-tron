package org.tron.plugins.utils;

import static org.fusesource.leveldbjni.JniDBFactory.factory;

import com.google.common.primitives.Ints;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import lombok.Getter;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.BloomFilter;
import org.rocksdb.ComparatorOptions;
import org.rocksdb.Options;
import org.tron.common.arch.Arch;
import org.tron.common.utils.MarketOrderPriceComparatorForLevelDB;
import org.tron.common.utils.MarketOrderPriceComparatorForRocksDB;
import org.tron.protos.Protocol;

public class DBUtils {


  public enum Operator {
    CREATE((byte) 0),
    MODIFY((byte) 1),
    DELETE((byte) 2),
    PUT((byte) 3);

    @Getter
    private byte value;

    Operator(byte value) {
      this.value = value;
    }

    static Operator valueOf(byte b) {
      switch (b) {
        case 0:
          return Operator.CREATE;
        case 1:
          return Operator.MODIFY;
        case 2:
          return Operator.DELETE;
        case 3:
          return Operator.PUT;
        default:
          return null;
      }
    }
  }

  public static final String SPLIT_BLOCK_NUM = "split_block_num";
  public static final String MARKET_PAIR_PRICE_TO_ORDER = "market_pair_price_to_order";
  public static final String CHECKPOINT_DB_V2 = "checkpoint";
  public static final String TMP = "tmp";

  public static final int NODE_TYPE_LIGHT_NODE = 1;

  public static final String KEY_ENGINE = "ENGINE";
  public static final String FILE_ENGINE = "engine.properties";
  public static final String LEVELDB = "LEVELDB";
  public static final String ROCKSDB = "ROCKSDB";

  public static DB newLevelDb(Path db) throws IOException {
    Arch.throwIfUnsupportedArm64Exception(LEVELDB);
    File file = db.toFile();
    org.iq80.leveldb.Options dbOptions = newDefaultLevelDbOptions();
    if (MARKET_PAIR_PRICE_TO_ORDER.equalsIgnoreCase(file.getName())) {
      dbOptions.comparator(new MarketOrderPriceComparatorForLevelDB());
    }
    return factory.open(file, dbOptions);
  }

  public static org.iq80.leveldb.Options newDefaultLevelDbOptions() {
    org.iq80.leveldb.Options dbOptions = new org.iq80.leveldb.Options();
    dbOptions.createIfMissing(true);
    dbOptions.paranoidChecks(true);
    dbOptions.verifyChecksums(true);
    dbOptions.compressionType(CompressionType.SNAPPY);
    dbOptions.blockSize(4 * 1024);
    dbOptions.writeBufferSize(10 * 1024 * 1024);
    dbOptions.cacheSize(10 * 1024 * 1024L);
    dbOptions.maxOpenFiles(1000);
    return dbOptions;
  }

  /**
   * Creates a new RocksDB Options.
   *
   * <p><b>CRITICAL:</b> Must be closed after use to prevent native memory leaks.
   * Use try-with-resources.
   *
   * <pre>{@code
   * try (Options options = newDefaultRocksDbOptions(false, name)) {
   *     // do something
   * }
   * }</pre>
   *
   * @param forBulkLoad if true, optimizes for bulk loading
   * @param name db name
   * @return a new Options instance that must be closed
   */
  public static Options newDefaultRocksDbOptions(boolean forBulkLoad, String name) {
    Options options = new Options();
    options.setCreateIfMissing(true);
    options.setIncreaseParallelism(1);
    options.setNumLevels(7);
    options.setMaxOpenFiles(5000);
    options.setTargetFileSizeBase(64 * 1024 * 1024);
    options.setTargetFileSizeMultiplier(1);
    options.setMaxBytesForLevelBase(512 * 1024 * 1024);
    options.setMaxBackgroundCompactions(StrictMath.max(
        1, Runtime.getRuntime().availableProcessors()));
    options.setLevel0FileNumCompactionTrigger(4);
    options.setLevelCompactionDynamicLevelBytes(true);
    final BlockBasedTableConfig tableCfg;
    options.setTableFormatConfig(tableCfg = new BlockBasedTableConfig());
    tableCfg.setBlockSize(64 * 1024);
    tableCfg.setBlockCacheSize(32 * 1024 * 1024);
    tableCfg.setCacheIndexAndFilterBlocks(true);
    tableCfg.setPinL0FilterAndIndexBlocksInCache(true);
    tableCfg.setFilter(new BloomFilter(10, false));
    if (forBulkLoad) {
      options.prepareForBulkLoad();
    }
    if (MARKET_PAIR_PRICE_TO_ORDER.equalsIgnoreCase(name)) {
      options.setComparator(new MarketOrderPriceComparatorForRocksDB(new ComparatorOptions()));
    }
    return options;
  }

  public static String simpleDecode(byte[] bytes) {
    byte[] lengthBytes = Arrays.copyOf(bytes, 4);
    int length = Ints.fromByteArray(lengthBytes);
    byte[] value = Arrays.copyOfRange(bytes, 4, 4 + length);
    return new String(value);
  }

  public static Sha256Hash getTransactionId(Protocol.Transaction transaction) {
    return Sha256Hash.of(true,
        transaction.getRawData().toByteArray());
  }
}
