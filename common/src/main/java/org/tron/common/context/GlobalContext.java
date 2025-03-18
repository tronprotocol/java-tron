package org.tron.common.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.tron.common.utils.Sha256Hash;

public class GlobalContext {

  private static final ThreadLocal<Long> HEADER = new ThreadLocal<>();
  private static final ThreadLocal<Map<Long, Sha256Hash>> BLOCK_HASHES =
      ThreadLocal.withInitial(() -> Collections.synchronizedMap(new HashMap<>()));

  private GlobalContext() {
  }

  public static Optional<Long> getHeader() {
    return Optional.ofNullable(HEADER.get());
  }

  public static void setHeader(long header) {
    HEADER.set(header);
  }

  public static void removeHeader() {
    HEADER.remove();
  }

  public static void putBlockHash(long blockNumber, Sha256Hash blockHash) {
    BLOCK_HASHES.get().put(blockNumber, blockHash);
  }

  public static Optional<Sha256Hash> popBlockHash(long blockNumber) {
    return Optional.ofNullable(BLOCK_HASHES.get().remove(blockNumber));
  }

  public static void clearBlockHashes() {
    BLOCK_HASHES.get().clear();
  }

  public static void clear() {
    removeHeader();
    clearBlockHashes();
  }

}
