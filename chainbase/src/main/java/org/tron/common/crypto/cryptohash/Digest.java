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

package org.tron.common.crypto.cryptohash;

/**
 * Copyright (c) 2007-2010  Projet RNRT SAPHIR
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


public interface Digest {

  /**
   * Insert one more input data byte.
   *
   * @param in the input byte
   */
  void update(byte in);

  /**
   * Insert some more bytes.
   *
   * @param inbuf the data bytes
   */
  void update(byte[] inbuf);

  /**
   * Insert some more bytes.
   *
   * @param inbuf the data buffer
   * @param off the data offset in {@code inbuf}
   * @param len the data length (in bytes)
   */
  void update(byte[] inbuf, int off, int len);

  /**
   * Finalize the current hash computation and return the hash value in a newly-allocated array. The
   * object is resetted.
   *
   * @return the hash output
   */
  byte[] digest();

  /**
   * Input some bytes, then finalize the current hash computation and return the hash value in a
   * newly-allocated array. The object is resetted.
   *
   * @param inbuf the input data
   * @return the hash output
   */
  byte[] digest(byte[] inbuf);

  /**
   * Finalize the current hash computation and store the hash value in the provided output buffer.
   * The {@code len} parameter contains the maximum number of bytes that should be written; no more
   * bytes than the natural hash function output length will be produced. If {@code len} is smaller
   * than the natural hash output length, the hash output is truncated to its first {@code len}
   * bytes. The object is resetted.
   *
   * @param outbuf the output buffer
   * @param off the output offset within {@code outbuf}
   * @param len the requested hash output length (in bytes)
   * @return the number of bytes actually written in {@code outbuf}
   */
  int digest(byte[] outbuf, int off, int len);

  /**
   * Get the natural hash function output length (in bytes).
   *
   * @return the digest output length (in bytes)
   */
  int getDigestLength();

  /**
   * Reset the object: this makes it suitable for a new hash computation. The current computation,
   * if any, is discarded.
   */
  void reset();

  /**
   * Clone the current state. The returned object evolves independantly of this object.
   *
   * @return the clone
   */
  Digest copy();

  /**
   * <p>Return the "block length" for the hash function. This value is naturally defined for
   * iterated hash functions (Merkle-Damgard). It is used in HMAC (that's what the <a
   * href="http://tools.ietf.org/html/rfc2104">HMAC specification</a> names the "{@code B}"
   * parameter).</p> <p> <p>If the function is "block-less" then this function may return {@code -n}
   * where {@code n} is an integer such that the block length for HMAC ("{@code B}") will be
   * inferred from the key length, by selecting the smallest multiple of {@code n} which is no
   * smaller than the key length. For instance, for the Fugue-xxx hash functions, this function
   * returns -4: the virtual block length B is the HMAC key length, rounded up to the next multiple
   * of 4.</p>
   *
   * @return the internal block length (in bytes), or {@code -n}
   */
  int getBlockLength();

  /**
   * <p>Get the display name for this function (e.g. {@code "SHA-1"} for SHA-1).</p>
   *
   * @see Object
   */
  String toString();
}
