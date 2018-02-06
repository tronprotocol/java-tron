package org.tron.core.db;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.utils.ByteArray;

public class DynamicPropertiesStore extends TronDatabase {

  private static final Logger logger = LoggerFactory.getLogger("DynamicPropertiesStore");

  private static final byte[] LATEST_BLOCK_HEADER_TIMESTAMP = "latest_block_header_timestamp"
      .getBytes();
  private static final byte[] LATEST_BLOCK_HEADER_NUMBER = "latest_block_header_number".getBytes();
  private static final byte[] LATEST_BLOCK_HEADER_HASH = "latest_block_header_hash".getBytes();

  private DynamicPropertiesStore(String dbName) {
    super(dbName);

    try {
      this.getLatestBlockHeaderTimestamp();
    } catch (IllegalArgumentException e) {
      this.saveLatestBlockHeaderTimestamp(0);
    }

    try {
      this.getLatestBlockHeaderNumber();
    } catch (IllegalArgumentException e) {
      this.saveLatestBlockHeaderNumber(0);
    }

    try {
      this.getLatestBlockHeaderHash();
    } catch (IllegalArgumentException e) {
      this.saveLatestBlockHeaderHash(ByteString.copyFrom(ByteArray.fromHexString("00")));
    }
  }

  private static DynamicPropertiesStore instance;

  /**
   * create fun.
   *
   * @param dbName the name of database
   */
  public static DynamicPropertiesStore create(String dbName) {
    if (instance == null) {
      synchronized (DynamicPropertiesStore.class) {
        if (instance == null) {
          instance = new DynamicPropertiesStore(dbName);
        }
      }
    }
    return instance;
  }

  @Override
  void add() {

  }

  @Override
  void del() {

  }

  @Override
  void fetch() {

  }

  /**
   * get timestamp of creating global latest block.
   */
  public long getLatestBlockHeaderTimestamp() {
    byte[] t = this.dbSource.getData(LATEST_BLOCK_HEADER_TIMESTAMP);

    if (t == null || t.length == 0) {
      throw new IllegalArgumentException("not found latest block header timestamp");
    }

    return ByteArray.toLong(t);
  }

  /**
   * get number of global latest block.
   */
  public long getLatestBlockHeaderNumber() {
    byte[] n = this.dbSource.getData(LATEST_BLOCK_HEADER_NUMBER);

    if (n == null || n.length == 0) {
      throw new IllegalArgumentException("not found latest block header number");
    }

    return ByteArray.toLong(n);
  }

  /**
   * get hash of global latest block.
   */
  public ByteString getLatestBlockHeaderHash() {
    byte[] h = this.dbSource.getData(LATEST_BLOCK_HEADER_HASH);

    if (h == null || h.length == 0) {
      throw new IllegalArgumentException("not found latest block header hash");
    }

    return ByteString.copyFrom(h);
  }

  /**
   * save timestamp of creating global latest block.
   */
  public void saveLatestBlockHeaderTimestamp(long t) {
    logger.info("update latest block header timestamp = {}", t);
    this.dbSource.putData(LATEST_BLOCK_HEADER_TIMESTAMP, ByteArray.fromLong(t));
  }

  /**
   * save number of global latest block.
   */
  public void saveLatestBlockHeaderNumber(long n) {
    logger.info("update latest block header number = {}", n);
    this.dbSource.putData(LATEST_BLOCK_HEADER_NUMBER, ByteArray.fromLong(n));
  }

  /**
   * save hash of global latest block.
   */
  public void saveLatestBlockHeaderHash(ByteString h) {
    logger.info("update latest block header hash = {}", ByteArray.toHexString(h.toByteArray()));
    this.dbSource.putData(LATEST_BLOCK_HEADER_HASH, h.toByteArray());
  }
}
