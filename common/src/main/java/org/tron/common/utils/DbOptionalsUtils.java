package org.tron.common.utils;

import java.util.Arrays;
import java.util.List;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.Options;


public class DbOptionalsUtils {

  public static final CompressionType DEFAULT_COMPRESSION_TYPE = CompressionType.SNAPPY;
  public static final int DEFAULT_BLOCK_SIZE = 4 * 1024;
  public static final int DEFAULT_WRITE_BUFFER_SIZE = 16 * 1024 * 1024;
  public static final int DEFAULT_WRITE_BUFFER_SIZE_M = 64 * 1024 * 1024;
  public static final long DEFAULT_CACHE_SIZE = 32 * 1024 * 1024L;
  public static final int DEFAULT_MAX_OPEN_FILES = 100;
  /**
   * defaultM = {
   *   maxOpenFiles = 500
   *   }
   *   add defaultL settings into storage to overwrite 100
   */
  public static final int DEFAULT_MAX_OPEN_FILES_M = 100;
  /**
   * defaultL = {
   *   maxOpenFiles = 1000
   *   }
   *   add defaultL settings into storage to overwrite 100
   */
  public static final int DEFAULT_MAX_OPEN_FILES_L = 100;
  // Read a lot
  public static final List<String> DB_M = Arrays.asList("code", "contract");
  // Read frequently
  public static final List<String> DB_L = Arrays.asList("account", "delegation",
      "storage-row");

  private DbOptionalsUtils() {
    throw new IllegalStateException("DbOptionalsUtils class");
  }

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


    if (DB_M.contains(name)) {
      adjustDefaultDbOptionsForM(dbOptions);
    }

    if (DB_L.contains(name)) {
      adjustDefaultDbOptionsForL(dbOptions);
    }

    return dbOptions;
  }

  private static void adjustDefaultDbOptionsForM(Options defaultOptions) {
    defaultOptions.maxOpenFiles(DEFAULT_MAX_OPEN_FILES_M);
    defaultOptions.writeBufferSize(DEFAULT_WRITE_BUFFER_SIZE_M);
  }

  private static void adjustDefaultDbOptionsForL(Options defaultOptions) {
    defaultOptions.maxOpenFiles(DEFAULT_MAX_OPEN_FILES_L);
    defaultOptions.writeBufferSize(DEFAULT_WRITE_BUFFER_SIZE_M);
  }

}
