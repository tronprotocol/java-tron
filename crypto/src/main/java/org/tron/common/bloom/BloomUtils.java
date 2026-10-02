package org.tron.common.bloom;

import com.google.protobuf.ByteString;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteUtil;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfo.Log;
import org.tron.protos.Protocol.TransactionRet;

/**
 * Shared log bloom encoding for the node and offline tools.
 */
public final class BloomUtils {

  public static final int BLOOM_BIT_SIZE = 2048;
  public static final int BLOOM_BYTE_SIZE = BLOOM_BIT_SIZE / 8;

  private BloomUtils() {
  }

  /**
   * Creates a bloom from the first six bytes of a Keccak-256 hash.
   */
  public static byte[] create(byte[] hash) {
    byte[] bloom = new byte[BLOOM_BYTE_SIZE];
    setBits(bloom, hash);
    return bloom;
  }

  /**
   * Returns the block's bloom, or null when there are no logs.
   */
  public static byte[] createBloom(TransactionRet transactionRet) {
    if (transactionRet == null) {
      return null;
    }

    byte[] bloom = null;
    for (TransactionInfo transactionInfo : transactionRet.getTransactioninfoList()) {
      for (Log log : transactionInfo.getLogList()) {
        if (bloom == null) {
          bloom = new byte[BLOOM_BYTE_SIZE];
        }
        // Log addresses already omit the TRON address prefix (0x41).
        setBits(bloom, Hash.sha3(log.getAddress().toByteArray()));
        for (ByteString topic : log.getTopicsList()) {
          setBits(bloom, Hash.sha3(topic.toByteArray()));
        }
      }
    }
    return bloom;
  }

  private static void setBits(byte[] bloom, byte[] hash) {
    //only use first six byte
    for (int i = 0; i < 6; i += 2) {
      int position = ((hash[i] & 0x07) << 8) | (hash[i + 1] & 0xff);
      ByteUtil.setBit(bloom, position, 1);
    }
  }
}
