package org.tron.common.utils;

/*
 * Copyright 2011 Google Inc.
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.io.ByteStreams;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;


/**
 * A Sha256Hash just wraps a byte[] so that equals and hashcode work correctly, allowing it to be
 * used as keys in a map. It also checks that the length is correct and provides a bit more type
 * safety.
 */
public class Sha256Hash implements Serializable, Comparable<Sha256Hash> {

  public static final int LENGTH = 32; // bytes
  public static final Sha256Hash ZERO_HASH = wrap(new byte[LENGTH]);

  private final byte[] bytes;

  private byte[] generateBlockId(long blockNum, Sha256Hash blockHash) {
    byte[] numBytes = Longs.toByteArray(blockNum);
    byte[] hash = new byte[blockHash.getBytes().length];
    System.arraycopy(numBytes, 0, hash, 0, 8);
    System.arraycopy(blockHash.getBytes(), 8, hash, 8, blockHash.getBytes().length - 8);
    return hash;
  }

  private byte[] generateBlockId(long blockNum, byte[] blockHash) {
    byte[] numBytes = Longs.toByteArray(blockNum);
    byte[] hash = new byte[blockHash.length];
    System.arraycopy(numBytes, 0, hash, 0, 8);
    System.arraycopy(blockHash, 8, hash, 8, blockHash.length - 8);
    return hash;
  }

  public Sha256Hash(long num, byte[] hash) {
    byte[] rawHashBytes = this.generateBlockId(num, hash);
    checkArgument(rawHashBytes.length == LENGTH);
    this.bytes = rawHashBytes;
  }

  public Sha256Hash(long num, Sha256Hash hash) {
    byte[] rawHashBytes = this.generateBlockId(num, hash);
    checkArgument(rawHashBytes.length == LENGTH);
    this.bytes = rawHashBytes;
  }

  /**
   * Use {@link #wrap(byte[])} instead.
   */
  @Deprecated
  public Sha256Hash(byte[] rawHashBytes) {
    checkArgument(rawHashBytes.length == LENGTH);
    this.bytes = rawHashBytes;
  }

  /**
   * Creates a new instance that wraps the given hash value.
   *
   * @param rawHashBytes the raw hash bytes to wrap
   * @return a new instance
   * @throws IllegalArgumentException if the given array length is not exactly 32
   */
  @SuppressWarnings("deprecation") // the constructor will be made private in the future
  public static Sha256Hash wrap(byte[] rawHashBytes) {
    return new Sha256Hash(rawHashBytes);
  }

  public static Sha256Hash wrap(ByteString rawHashByteString) {
    return wrap(rawHashByteString.toByteArray());
  }

  /**
   * Use {@link #of(byte[])} instead: this old name is ambiguous.
   */
  @Deprecated
  public static Sha256Hash create(byte[] contents) {
    return of(contents);
  }

  /**
   * Creates a new instance containing the calculated (one-time) hash of the given bytes.
   *
   * @param contents the bytes on which the hash value is calculated
   * @return a new instance containing the calculated (one-time) hash
   */
  public static Sha256Hash of(byte[] contents) {
    return wrap(hash(contents));
  }

  /**
   * Creates a new instance containing the calculated (one-time) hash of the given file's contents.
   * The file contents are read fully into memory, so this method should only be used with small
   * files.
   *
   * @param file the file on which the hash value is calculated
   * @return a new instance containing the calculated (one-time) hash
   * @throws IOException if an error occurs while reading the file
   */
  public static Sha256Hash of(File file) throws IOException {

    try (FileInputStream in = new FileInputStream(file)) {
      return of(ByteStreams.toByteArray(in));
    }
  }

  /**
   * Use {@link #twiceOf(byte[])} instead: this old name is ambiguous.
   */
  @Deprecated
  public static Sha256Hash createDouble(byte[] contents) {
    return twiceOf(contents);
  }

  /**
   * Creates a new instance containing the hash of the calculated hash of the given bytes.
   *
   * @param contents the bytes on which the hash value is calculated
   * @return a new instance containing the calculated (two-time) hash
   */
  public static Sha256Hash twiceOf(byte[] contents) {
    return wrap(hashTwice(contents));
  }

  /**
   * Returns a new SHA-256 MessageDigest instance. This is a convenience method which wraps the
   * checked exception that can never occur with a RuntimeException.
   *
   * @return a new SHA-256 MessageDigest instance
   */
  public static MessageDigest newDigest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);  // Can't happen.
    }
  }

  /**
   * Calculates the SHA-256 hash of the given bytes.
   *
   * @param input the bytes to hash
   * @return the hash (in big-endian order)
   */
  public static byte[] hash(byte[] input) {
    return hash(input, 0, input.length);
  }

  /**
   * Calculates the SHA-256 hash of the given byte range.
   *
   * @param input the array containing the bytes to hash
   * @param offset the offset within the array of the bytes to hash
   * @param length the number of bytes to hash
   * @return the hash (in big-endian order)
   */
  public static byte[] hash(byte[] input, int offset, int length) {
    MessageDigest digest = newDigest();
    digest.update(input, offset, length);
    return digest.digest();
  }

  /**
   * Calculates the SHA-256 hash of the given bytes, and then hashes the resulting hash again.
   *
   * @param input the bytes to hash
   * @return the double-hash (in big-endian order)
   */
  public static byte[] hashTwice(byte[] input) {
    return hashTwice(input, 0, input.length);
  }

  /**
   * Calculates the SHA-256 hash of the given byte range, and then hashes the resulting hash again.
   *
   * @param input the array containing the bytes to hash
   * @param offset the offset within the array of the bytes to hash
   * @param length the number of bytes to hash
   * @return the double-hash (in big-endian order)
   */
  public static byte[] hashTwice(byte[] input, int offset, int length) {
    MessageDigest digest = newDigest();
    digest.update(input, offset, length);
    return digest.digest(digest.digest());
  }

  /**
   * Calculates the hash of hash on the given byte ranges. This is equivalent to concatenating the
   * two ranges and then passing the result to {@link #hashTwice(byte[])}.
   */
  public static byte[] hashTwice(byte[] input1, int offset1, int length1,
      byte[] input2, int offset2, int length2) {
    MessageDigest digest = newDigest();
    digest.update(input1, offset1, length1);
    digest.update(input2, offset2, length2);
    return digest.digest(digest.digest());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || !(o instanceof Sha256Hash)) {
      return false;
    }
    return Arrays.equals(bytes, ((Sha256Hash) o).bytes);
  }

  @Override
  public String toString() {
    return ByteArray.toHexString(bytes);
  }

  /**
   * Returns the last four bytes of the wrapped hash. This should be unique enough to be a suitable
   * hash code even for blocks, where the goal is to try and get the first bytes to be zeros (i.e.
   * the value as a big integer lower than the target value).
   */
  @Override
  public int hashCode() {
    // Use the last 4 bytes, not the first 4 which are often zeros in Bitcoin.
    return Ints
        .fromBytes(bytes[LENGTH - 4], bytes[LENGTH - 3], bytes[LENGTH - 2], bytes[LENGTH - 1]);
  }

  /**
   * Returns the bytes interpreted as a positive integer.
   */
  public BigInteger toBigInteger() {
    return new BigInteger(1, bytes);
  }

  /**
   * Returns the internal byte array, without defensively copying. Therefore do NOT modify the
   * returned array.
   */
  public byte[] getBytes() {
    return bytes;
  }

  /**
   * For pb return ByteString.
   */
  public ByteString getByteString() {
    return ByteString.copyFrom(bytes);
  }

  @Override
  public int compareTo(final Sha256Hash other) {
    for (int i = LENGTH - 1; i >= 0; i--) {
      final int thisByte = this.bytes[i] & 0xff;
      final int otherByte = other.bytes[i] & 0xff;
      if (thisByte > otherByte) {
        return 1;
      }
      if (thisByte < otherByte) {
        return -1;
      }
    }
    return 0;
  }

  private static int ROTR32(int x, int s) {
    return (((x) >>> (s)) | ((x) << (32 - (s))));
  }

  private static int R(int x, int s) {
    return ((x) >>> (s));
  }

  private static int S(int x, int s) {
    return ROTR32(x, s);
  }

  private static int SIG0(int x) {
    return S(x, 2) ^ S(x, 13) ^ S(x, 22);
  }

  private static int SIG1(int x) {
    return S(x, 6) ^ S(x, 11) ^ S(x, 25);
  }

  private static int sig0(int x) {
    return S(x, 7) ^ S(x, 18) ^ R(x, 3);
  }

  private static int sig1(int x) {
    return S(x, 17) ^ S(x, 19) ^ R(x, 10);
  }

  private static int CH(int x, int y, int z) {
    return ((x & (y ^ z)) ^ z);
  }

  private static int MAJ(int x, int y, int z) {
    return (((x | y) & z) | (x & y));
  }

  private static int byte2Int(byte[] a, int offset) {
    int b = 0;
    b += ((int) (a[offset++]) << 24)&0xFF000000;
    b += ((int) (a[offset++]) << 16)&0x00FF0000;
    b += ((int) (a[offset++]) << 8)&0x0000FF00;
    b += ((int) (a[offset++]))&0x000000FF;
    return b;
  }

  private static int byte2Int_(byte[] a, int offset) {
    int b = 0;
    b += ((int) (a[offset++]))&0x000000FF;
    b += ((int) (a[offset++]) << 8)&0x0000FF00;
    b += ((int) (a[offset++]) << 16)&0x00FF0000;
    b += ((int) (a[offset++]) << 24)&0xFF000000;
    return b;
  }

  private static void int2Byte(byte[] a, int offset, int b) {
    a[offset++] = (byte) ((b & 0xFF000000) >>> 24);
    a[offset++] = (byte) ((b & 0x00FF0000) >>> 16);
    a[offset++] = (byte) ((b & 0x0000FF00) >>> 8);
    a[offset++] = (byte) ((b & 0x000000FF));
  }

  public static byte[] Sha256OneBlock(byte[] message) {
    if (ArrayUtils.isEmpty(message) || message.length != 64) {
      return null;
    }
    byte[] Iv = ByteArray
        .fromHexString("6A09E667BB67AE853C6EF372A54FF53A510E527F9B05688C1F83D9AB5BE0CD19");
    int[] k = new int[]
        {
            0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4,
            0xab1c5ed5,
            0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7,
            0xc19bf174,
            0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc,
            0x76f988da,
            0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351,
            0x14292967,
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e,
            0x92722c85,
            0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585,
            0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f,
            0x682e6ff3,
            0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7,
            0xc67178f2
        };
    byte[][] gi = new byte[][]
        {
            new byte[]{0, 1, 2, 3, 4, 5, 6, 7},
            new byte[]{7, 0, 1, 2, 3, 4, 5, 6},
            new byte[]{6, 7, 0, 1, 2, 3, 4, 5},
            new byte[]{5, 6, 7, 0, 1, 2, 3, 4},
            new byte[]{4, 5, 6, 7, 0, 1, 2, 3},
            new byte[]{3, 4, 5, 6, 7, 0, 1, 2},
            new byte[]{2, 3, 4, 5, 6, 7, 0, 1},
            new byte[]{1, 2, 3, 4, 5, 6, 7, 0}
        };

    int u1Index;
    int u1Idx;
    int[] u4Data = new int[64];
    int[] u4H = new int[8];
    int A;
    int B;
    int C;
    int D;
    int E;
    int F;
    int G;
    int H;
    int temp;

    for (u1Index = 0; u1Index < 32; u1Index += 0x04) {
      u4H[u1Index / 4] = byte2Int(Iv, u1Index);
      u4Data[u1Index / 4] = byte2Int(message, u1Index);
    }
    for (; u1Index < 64; u1Index += 0x04) {
      u4Data[u1Index / 4] = byte2Int(message, u1Index);
    }
    u1Index = 48;
    int offset = 16;
    while (u1Index-- > 0) {
      temp =
          sig1(u4Data[offset - 2]) + u4Data[offset - 7] + sig0(u4Data[offset - 15]) + u4Data[offset
              - 16];
      u4Data[offset++] = temp;
    }

    A = u4H[0];
    B = u4H[1];
    C = u4H[2];
    D = u4H[3];
    E = u4H[4];
    F = u4H[5];
    G = u4H[6];
    H = u4H[7];

    for (u1Index = 0; u1Index < 64; u1Index++) {
      u1Idx = (u1Index & 0x07);
      temp =
          u4H[gi[u1Idx][7]] + SIG1(u4H[gi[u1Idx][4]]) + CH(u4H[gi[u1Idx][4]], u4H[gi[u1Idx][5]],
              u4H[gi[u1Idx][6]]) + k[u1Index] + u4Data[u1Index];
      u4H[gi[u1Idx][7]] = temp + SIG0(u4H[gi[u1Idx][0]]) + MAJ(u4H[gi[u1Idx][0]], u4H[gi[u1Idx][1]],
          u4H[gi[u1Idx][2]]);
      u4H[gi[u1Idx][3]] += temp;
    }

    u4H[0] += A;
    u4H[1] += B;
    u4H[2] += C;
    u4H[3] += D;
    u4H[4] += E;
    u4H[5] += F;
    u4H[6] += G;
    u4H[7] += H;

    for (u1Index = 0; u1Index < 32; u1Index += 0x04) {
      int2Byte(Iv, u1Index, u4H[u1Index / 4]);
    }

    return Iv;
  }

  public static void main(String[] args){
    byte[] msg = ByteArray.fromHexString("6D38B7C9D29C104292D92219BDB70139AA86585B70B728FBADB2F5DE9CB4C14DFC338BAEDE9DA5B2D2EE9DA485F3151A57A935A1EDA8239A4EF020DE8518BC5E");
    byte[] hash = Sha256OneBlock(msg);
    System.out.println(ByteArray.toHexString(hash));
  }
}
