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
import org.spongycastle.crypto.digests.SM3Digest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;


/**
 * A SM3Hash just wraps a byte[] so that equals and hashcode work correctly, allowing it to be
 * used as keys in a map. It also checks that the length is correct and provides a bit more type
 * safety.
 */
public class SM3Hash implements Serializable, Comparable<SM3Hash>, HashInterface {

    public static final int LENGTH = 32; // bytes
    public static final SM3Hash ZERO_HASH = wrap(new byte[LENGTH]);

    private final byte[] bytes;

    public SM3Hash(long num, byte[] hash) {
        byte[] rawHashBytes = this.generateBlockId(num, hash);
        checkArgument(rawHashBytes.length == LENGTH);
        this.bytes = rawHashBytes;
    }

    public SM3Hash(long num, SM3Hash hash) {
        byte[] rawHashBytes = this.generateBlockId(num, hash);
        checkArgument(rawHashBytes.length == LENGTH);
        this.bytes = rawHashBytes;
    }

    /**
     * Use {@link #wrap(byte[])} instead.
     */
    @Deprecated
    public SM3Hash(byte[] rawHashBytes) {
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
    public static SM3Hash wrap(byte[] rawHashBytes) {
        return new SM3Hash(rawHashBytes);
    }

    public static SM3Hash wrap(ByteString rawHashByteString) {
        return wrap(rawHashByteString.toByteArray());
    }

    /**
     * Use {@link #of(byte[])} instead: this old name is ambiguous.
     */
    @Deprecated
    public static SM3Hash create(byte[] contents) {
        return of(contents);
    }

    /**
     * Creates a new instance containing the calculated (one-time) hash of the given bytes.
     *
     * @param contents the bytes on which the hash value is calculated
     * @return a new instance containing the calculated (one-time) hash
     */
    public static SM3Hash of(byte[] contents) {
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
    public static SM3Hash of(File file) throws IOException {

        try (FileInputStream in = new FileInputStream(file)) {
            return of(ByteStreams.toByteArray(in));
        }
    }

    /**
     * Use {@link #twiceOf(byte[])} instead: this old name is ambiguous.
     */
    @Deprecated
    public static SM3Hash createDouble(byte[] contents) {
        return twiceOf(contents);
    }

    /**
     * Creates a new instance containing the hash of the calculated hash of the given bytes.
     *
     * @param contents the bytes on which the hash value is calculated
     * @return a new instance containing the calculated (two-time) hash
     */
    public static SM3Hash twiceOf(byte[] contents) {
        return wrap(hashTwice(contents));
    }

    /**
     * Returns a new SM3 MessageDigest instance. This is a convenience method which wraps the
     * checked exception that can never occur with a RuntimeException.
     *
     * @return a new SM3 MessageDigest instance
     */
    public static SM3Digest newDigest() {
        return new SM3Digest();
    }

    /**
     * Calculates the SM3 hash of the given bytes.
     *
     * @param input the bytes to hash
     * @return the hash (in big-endian order)
     */
    public static byte[] hash(byte[] input) {
        return hash(input, 0, input.length);
    }

    /**
     * Calculates the SM3 hash of the given byte range.
     *
     * @param input the array containing the bytes to hash
     * @param offset the offset within the array of the bytes to hash
     * @param length the number of bytes to hash
     * @return the hash (in big-endian order)
     */
    public static byte[] hash(byte[] input, int offset, int length) {
        SM3Digest digest = newDigest();
        digest.update(input, offset, length);
        byte[] eHash = new byte[digest.getDigestSize()];

        digest.doFinal(eHash, 0);
        return eHash;
    }

    /**
     * Calculates the SM3 hash of the given bytes, and then hashes the resulting hash again.
     *
     * @param input the bytes to hash
     * @return the double-hash (in big-endian order)
     */
    public static byte[] hashTwice(byte[] input) {
        return hashTwice(input, 0, input.length);
    }

    /**
     * Calculates the SM3 hash of the given byte range, and then hashes the resulting hash again.
     *
     * @param input the array containing the bytes to hash
     * @param offset the offset within the array of the bytes to hash
     * @param length the number of bytes to hash
     * @return the double-hash (in big-endian order)
     */
    public static byte[] hashTwice(byte[] input, int offset, int length) {
        SM3Digest digest = newDigest();
        digest.update(input, offset, length);
        byte[] eHash = new byte[digest.getDigestSize()];
        digest.doFinal(eHash, 0);
        digest.reset();
        digest.update(eHash,0,eHash.length);
        digest.doFinal(eHash,0);
        return eHash;
    }

    /**
     * Calculates the hash of hash on the given byte ranges. This is equivalent to concatenating the
     * two ranges and then passing the result to {@link #hashTwice(byte[])}.
     */
    public static byte[] hashTwice(byte[] input1, int offset1, int length1,
                                   byte[] input2, int offset2, int length2) {
        SM3Digest digest = newDigest();
        digest.update(input1, offset1, length1);
        digest.update(input2, offset2, length2);
        byte[] eHash = new byte[digest.getDigestSize()];
        digest.doFinal(eHash, 0);
        digest.doFinal(eHash,0);
        return eHash;
    }

    private byte[] generateBlockId(long blockNum, SM3Hash blockHash) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof SM3Hash)) {
            return false;
        }
        return Arrays.equals(bytes, ((SM3Hash) o).bytes);
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
    public int compareTo(final SM3Hash other) {
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

