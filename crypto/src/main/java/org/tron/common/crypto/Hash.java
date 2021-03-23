/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.crypto;

import static java.util.Arrays.copyOfRange;
import static org.tron.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;
import static org.tron.common.utils.ByteUtil.isNullOrZeroArray;
import static org.tron.common.utils.ByteUtil.isSingleZero;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.math.ec.ECPoint;
import org.tron.common.crypto.jce.TronCastleProvider;
import org.tron.common.utils.DecodeUtil;

@Slf4j(topic = "crypto")
public class Hash {

  public static final byte[] EMPTY_TRIE_HASH;
  private static final Provider CRYPTO_PROVIDER;
  private static final String HASH_256_ALGORITHM_NAME;
  private static final String HASH_512_ALGORITHM_NAME;
  private static final String ALGORITHM_NOT_FOUND = "Can't find such algorithm";
  /**
   * [0x80] If a string is 0-55 bytes long, the RLP encoding consists of a single byte with value
   * 0x80 plus the length of the string followed by the string. The range of the first byte is thus
   * [0x80, 0xb7].
   */
  private static final int OFFSET_SHORT_ITEM = 0x80;

  /**
   * [0xb7] If a string is more than 55 bytes long, the RLP encoding consists of a single byte with
   * value 0xb7 plus the length of the length of the string in binary form, followed by the length
   * of the string, followed by the string. For example, a length-1024 string would be encoded as
   * \xb9\x04\x00 followed by the string. The range of the first byte is thus [0xb8, 0xbf].
   */
  private static final int OFFSET_LONG_ITEM = 0xb7;

  /**
   * Reason for threshold according to Vitalik Buterin: - 56 bytes maximizes the benefit of both
   * options - if we went with 60 then we would have only had 4 slots for long strings so RLP would
   * not have been able to store objects above 4gb - if we went with 48 then RLP would be fine for
   * 2^128 space, but that's way too much - so 56 and 2^64 space seems like the right place to put
   * the cutoff - also, that's where Bitcoin's varint does the cutof
   */
  private static final int SIZE_THRESHOLD = 56;

  static {
    Security.addProvider(TronCastleProvider.getInstance());
    CRYPTO_PROVIDER = Security.getProvider("SC");
    HASH_256_ALGORITHM_NAME = "TRON-KECCAK-256";
    HASH_512_ALGORITHM_NAME = "TRON-KECCAK-512";
    EMPTY_TRIE_HASH = sha3(encodeElement(EMPTY_BYTE_ARRAY));
  }

  public static byte[] sha3(byte[] input) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance(HASH_256_ALGORITHM_NAME,
          CRYPTO_PROVIDER);
      digest.update(input);
      return digest.digest();
    } catch (NoSuchAlgorithmException e) {
      logger.error(ALGORITHM_NOT_FOUND, e);
      throw new RuntimeException(e);
    }

  }

  public static byte[] sha3(byte[] input1, byte[] input2) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance(HASH_256_ALGORITHM_NAME,
          CRYPTO_PROVIDER);
      digest.update(input1, 0, input1.length);
      digest.update(input2, 0, input2.length);
      return digest.digest();
    } catch (NoSuchAlgorithmException e) {
      logger.error(ALGORITHM_NOT_FOUND, e);
      throw new RuntimeException(e);
    }
  }

  /**
   * hashing chunk of the data
   *
   * @param input - data for hash
   * @param start - start of hashing chunk
   * @param length - length of hashing chunk
   * @return - keccak hash of the chunk
   */
  public static byte[] sha3(byte[] input, int start, int length) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance(HASH_256_ALGORITHM_NAME,
          CRYPTO_PROVIDER);
      digest.update(input, start, length);
      return digest.digest();
    } catch (NoSuchAlgorithmException e) {
      logger.error(ALGORITHM_NOT_FOUND, e);
      throw new RuntimeException(e);
    }
  }

  public static byte[] encodeElement(byte[] srcData) {

    // [0x80]
    if (isNullOrZeroArray(srcData)) {
      return new byte[]{(byte) OFFSET_SHORT_ITEM};

      // [0x00]
    } else if (isSingleZero(srcData)) {
      return srcData;

      // [0x01, 0x7f] - single byte, that byte is its own RLP encoding
    } else if (srcData.length == 1 && (srcData[0] & 0xFF) < 0x80) {
      return srcData;

      // [0x80, 0xb7], 0 - 55 bytes
    } else if (srcData.length < SIZE_THRESHOLD) {
      // length = 8X
      byte length = (byte) (OFFSET_SHORT_ITEM + srcData.length);
      byte[] data = Arrays.copyOf(srcData, srcData.length + 1);
      System.arraycopy(data, 0, data, 1, srcData.length);
      data[0] = length;

      return data;
      // [0xb8, 0xbf], 56+ bytes
    } else {
      // length of length = BX
      // prefix = [BX, [length]]
      int tmpLength = srcData.length;
      byte lengthOfLength = 0;
      while (tmpLength != 0) {
        ++lengthOfLength;
        tmpLength = tmpLength >> 8;
      }

      // set length Of length at first byte
      byte[] data = new byte[1 + lengthOfLength + srcData.length];
      data[0] = (byte) (OFFSET_LONG_ITEM + lengthOfLength);

      // copy length after first byte
      tmpLength = srcData.length;
      for (int i = lengthOfLength; i > 0; --i) {
        data[i] = (byte) (tmpLength & 0xFF);
        tmpLength = tmpLength >> 8;
      }

      // at last copy the number bytes after its length
      System.arraycopy(srcData, 0, data, 1 + lengthOfLength, srcData.length);

      return data;
    }
  }

  public static byte[] computeAddress(ECPoint pubPoint) {
    return computeAddress(pubPoint.getEncoded(/* uncompressed */ false));
  }

  public static byte[] computeAddress(byte[] pubBytes) {
    return sha3omit12(
        Arrays.copyOfRange(pubBytes, 1, pubBytes.length));
  }

  /**
   * Calculates RIGTMOST160(SHA3(input)). This is used in address calculations. *
   *
   * @param input - data
   * @return - add_pre_fix + 20 right bytes of the hash keccak of the data
   */
  public static byte[] sha3omit12(byte[] input) {
    byte[] hash = Hash.sha3(input);
    byte[] address = copyOfRange(hash, 11, hash.length);
    address[0] = DecodeUtil.addressPreFixByte;
    return address;
  }
}
