package org.tron.plugins.utils;

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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import org.bouncycastle.crypto.digests.SM3Digest;


/**
 * A Sha256Hash just wraps a byte[] so that equals and hashcode work correctly, allowing it to be
 * used as keys in a map. It also checks that the length is correct and provides a bit more type
 * safety.
 */
public class Sha256Hash implements Serializable, Comparable<Sha256Hash> {

  public static final int LENGTH = 32; // bytes

  private final byte[] bytes;

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


  /**
   * Creates a new instance containing the calculated (one-time) hash of the given bytes.
   *
   * @param contents the bytes on which the hash value is calculated
   * @return a new instance containing the calculated (one-time) hash
   */
  public static Sha256Hash of(boolean isSha256, byte[] contents) {
    return wrap(hash(isSha256, contents));
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
  public static Sha256Hash of(boolean isSha256, File file) throws IOException {

    try (FileInputStream in = new FileInputStream(file)) {
      return of(isSha256, ByteStreams.toByteArray(in));
    }
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
   * Returns a new SM3 MessageDigest instance. This is a convenience method which wraps the checked
   * exception that can never occur with a RuntimeException.
   *
   * @return a new SM3 MessageDigest instance
   */
  public static SM3Digest newSM3Digest() {
    return new SM3Digest();
  }

  /**
   * Calculates the SHA-256 hash of the given bytes.
   *
   * @param input the bytes to hash
   * @return the hash (in big-endian order)
   */
  public static byte[] hash(boolean isSha256, byte[] input) {
    return hash(isSha256, input, 0, input.length);
  }

  /**
   * Calculates the SHA-256 hash of the given byte range.
   *
   * @param input the array containing the bytes to hash
   * @param offset the offset within the array of the bytes to hash
   * @param length the number of bytes to hash
   * @return the hash (in big-endian order)
   */
  public static byte[] hash(boolean isSha256, byte[] input, int offset, int length) {
    if (isSha256) {
      MessageDigest digest = newDigest();
      digest.update(input, offset, length);
      return digest.digest();
    } else {
      SM3Digest digest = newSM3Digest();
      digest.update(input, offset, length);
      byte[] eHash = new byte[digest.getDigestSize()];
      digest.doFinal(eHash, 0);
      return eHash;
    }

  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Sha256Hash)) {
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
   * Returns the internal byte array, without defensively copying. Therefore do NOT modify the
   * returned array.
   */
  public byte[] getBytes() {
    return bytes;
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
}
