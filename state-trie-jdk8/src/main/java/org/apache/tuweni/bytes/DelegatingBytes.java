/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.bytes;


import io.vertx.core.buffer.Buffer;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;

/**
 * A class that holds and delegates all operations to its inner bytes field.
 *
 * <p>
 * This class may be used to create more types that represent bytes, but need a different name for business logic.
 */
public class DelegatingBytes extends AbstractBytes implements Bytes {

  final Bytes delegate;

  protected DelegatingBytes(Bytes delegate) {
    this.delegate = delegate;
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public byte get(int i) {
    return delegate.get(i);
  }

  @Override
  public Bytes slice(int index, int length) {
    return delegate.slice(index, length);
  }

  @Override
  public Bytes copy() {
    return Bytes.wrap(toArray());
  }

  @Override
  public MutableBytes mutableCopy() {
    return MutableBytes.wrap(toArray());
  }

  @Override
  public byte[] toArray() {
    return delegate.toArray();
  }

  @Override
  public byte[] toArray(ByteOrder byteOrder) {
    return delegate.toArray(byteOrder);
  }

  @Override
  public byte[] toArrayUnsafe() {
    return delegate.toArrayUnsafe();
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

  @Override
  public int getInt(int i) {
    return delegate.getInt(i);
  }

  @Override
  public int getInt(int i, ByteOrder order) {
    return delegate.getInt(i, order);
  }

  @Override
  public int toInt() {
    return delegate.toInt();
  }

  @Override
  public int toInt(ByteOrder order) {
    return delegate.toInt(order);
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public long getLong(int i) {
    return delegate.getLong(i);
  }

  @Override
  public long getLong(int i, ByteOrder order) {
    return delegate.getLong(i, order);
  }

  @Override
  public long toLong() {
    return delegate.toLong();
  }

  @Override
  public long toLong(ByteOrder order) {
    return delegate.toLong(order);
  }

  @Override
  public BigInteger toBigInteger() {
    return delegate.toBigInteger();
  }

  @Override
  public BigInteger toBigInteger(ByteOrder order) {
    return delegate.toBigInteger(order);
  }

  @Override
  public BigInteger toUnsignedBigInteger() {
    return delegate.toUnsignedBigInteger();
  }

  @Override
  public BigInteger toUnsignedBigInteger(ByteOrder order) {
    return delegate.toUnsignedBigInteger(order);
  }

  @Override
  public boolean isZero() {
    return delegate.isZero();
  }

  @Override
  public boolean hasLeadingZero() {
    return delegate.hasLeadingZero();
  }

  @Override
  public int numberOfLeadingZeros() {
    return delegate.numberOfLeadingZeros();
  }

  @Override
  public boolean hasLeadingZeroByte() {
    return delegate.hasLeadingZeroByte();
  }

  @Override
  public int numberOfLeadingZeroBytes() {
    return delegate.numberOfLeadingZeroBytes();
  }

  @Override
  public int numberOfTrailingZeroBytes() {
    return delegate.numberOfTrailingZeroBytes();
  }

  @Override
  public int bitLength() {
    return delegate.bitLength();
  }

  @Override
  public Bytes and(Bytes other) {
    return delegate.and(other);
  }

  @Override
  public <T extends MutableBytes> T and(Bytes other, T result) {
    return delegate.and(other, result);
  }

  @Override
  public Bytes or(Bytes other) {
    return delegate.or(other);
  }

  @Override
  public <T extends MutableBytes> T or(Bytes other, T result) {
    return delegate.or(other, result);
  }

  @Override
  public Bytes xor(Bytes other) {
    return delegate.xor(other);
  }

  @Override
  public <T extends MutableBytes> T xor(Bytes other, T result) {
    return delegate.xor(other, result);
  }

  @Override
  public <T extends MutableBytes> T shiftRight(int distance, T result) {
    return delegate.shiftRight(distance, result);
  }

  @Override
  public <T extends MutableBytes> T shiftLeft(int distance, T result) {
    return delegate.shiftLeft(distance, result);
  }

  @Override
  public Bytes slice(int i) {
    return delegate.slice(i);
  }

  @Override
  public void copyTo(MutableBytes destination) {
    delegate.copyTo(destination);
  }

  @Override
  public void copyTo(MutableBytes destination, int destinationOffset) {
    delegate.copyTo(destination, destinationOffset);
  }

  @Override
  public void appendTo(ByteBuffer byteBuffer) {
    delegate.appendTo(byteBuffer);
  }

  @Override
  public void appendTo(Buffer buffer) {
    delegate.appendTo(buffer);
  }

  @Override
  public <T extends Appendable> T appendHexTo(T appendable) {
    return delegate.appendHexTo(appendable);
  }

  @Override
  public int commonPrefixLength(Bytes other) {
    return delegate.commonPrefixLength(other);
  }

  @Override
  public Bytes commonPrefix(Bytes other) {
    return delegate.commonPrefix(other);
  }

  @Override
  public Bytes trimLeadingZeros() {
    return delegate.trimLeadingZeros();
  }

  @Override
  public void update(MessageDigest digest) {
    delegate.update(digest);
  }

  @Override
  public Bytes reverse() {
    return delegate.reverse();
  }

  @Override
  public String toHexString() {
    return delegate.toHexString();
  }

  @Override
  public String toUnprefixedHexString() {
    return delegate.toUnprefixedHexString();
  }

  @Override
  public String toEllipsisHexString() {
    return delegate.toEllipsisHexString();
  }

  @Override
  public String toShortHexString() {
    return delegate.toShortHexString();
  }

  @Override
  public String toQuantityHexString() {
    return delegate.toQuantityHexString();
  }

  @Override
  public String toBase64String() {
    return delegate.toBase64String();
  }

  @Override
  public int compareTo(Bytes b) {
    return delegate.compareTo(b);
  }

  @Override
  public boolean equals(final Object o) {
    return delegate.equals(o);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

}
