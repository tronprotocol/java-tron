package org.tron.common.utils;

import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.Options;


public class DbOptionalsUtils {

  public static final CompressionType DEFAULT_COMPRESSION_TYPE = CompressionType.SNAPPY;
  public static final int DEFAULT_BLOCK_SIZE = 4 * 1024;
  public static final int DEFAULT_WRITE_BUFFER_SIZE = 64 * 1024 * 1024;
  public static final long DEFAULT_CACHE_SIZE = 32 * 1024 * 1024L;
  public static final int DEFAULT_MAX_OPEN_FILES = 100;

  public static Options createDefaultDbOptions() {
    Options dbOptions = new Options();

    dbOptions.createIfMissing(true);
    dbOptions.paranoidChecks(true);
    dbOptions.verifyChecksums(true);

    dbOptions.compressionType(DEFAULT_COMPRESSION_TYPE);
    dbOptions.blockSize(DEFAULT_BLOCK_SIZE);
    dbOptions.writeBufferSize(DEFAULT_WRITE_BUFFER_SIZE);
    dbOptions.cacheSize(DEFAULT_CACHE_SIZE);
    dbOptions.maxOpenFiles(DEFAULT_MAX_OPEN_FILES);

    return dbOptions;
  }

  public static Options newDefaultDbOptions(String name ,Options defaultOptions) {
    Options dbOptions = new Options();

    dbOptions.createIfMissing(defaultOptions.createIfMissing());
    dbOptions.paranoidChecks(defaultOptions.paranoidChecks());
    dbOptions.verifyChecksums(defaultOptions.verifyChecksums());

    dbOptions.compressionType(defaultOptions.compressionType());
    dbOptions.blockSize(defaultOptions.blockSize());
    dbOptions.writeBufferSize(defaultOptions.writeBufferSize());
    dbOptions.cacheSize(defaultOptions.cacheSize());
    dbOptions.maxOpenFiles(defaultOptions.maxOpenFiles());

    switch (name) {
      case "block":
      case "transactionHistoryStore":
      case "transactionRetStore":
      case "trans": dbOptions.writeBufferSize(256 * 1024 * 1024);
        break;
      default:
    }

    return dbOptions;
  }
}
