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
import java.security.MessageDigest;


/**
 * A class that holds and delegates all operations to its inner bytes field.
 *
 * <p>
 * This class may be used to create more types that represent bytes, but need a different name for business logic.
 */
public class DelegatingMutableBytes implements MutableBytes {

  final MutableBytes delegate;

  protected DelegatingMutableBytes(MutableBytes delegate) {
    this.delegate = delegate;
  }

  @Override
  public void set(int i, byte b) {
    delegate.set(i, b);
  }

  @Override
  public void set(int i, Bytes b) {
    delegate.set(i, b);
  }

  @Override
  public void setInt(int i, int value) {
    delegate.setInt(i, value);
  }

  @Override
  public void setLong(int i, long value) {
    delegate.setLong(i, value);
  }

  @Override
  public MutableBytes increment() {
    return delegate.increment();
  }

  @Override
  public MutableBytes decrement() {
    return delegate.decrement();
  }

  @Override
  public MutableBytes mutableSlice(int i, int length) {
    return delegate.mutableSlice(i, length);
  }

  @Override
  public void fill(byte b) {
    delegate.fill(b);
  }

  @Override
  public void clear() {
    delegate.clear();
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
  public int getInt(int i) {
    return delegate.getInt(i);
  }

  @Override
  public int toInt() {
    return delegate.toInt();
  }

  @Override
  public long getLong(int i) {
    return delegate.getLong(i);
  }

  @Override
  public long toLong() {
    return delegate.toLong();
  }

  @Override
  public BigInteger toBigInteger() {
    return delegate.toBigInteger();
  }

  @Override
  public BigInteger toUnsignedBigInteger() {
    return delegate.toUnsignedBigInteger();
  }

  @Override
  public boolean isZero() {
    return delegate.isZero();
  }

  @Override
  public int numberOfLeadingZeros() {
    return delegate.numberOfLeadingZeros();
  }

  @Override
  public int numberOfLeadingZeroBytes() {
    return delegate.numberOfLeadingZeroBytes();
  }

  @Override
  public boolean hasLeadingZeroByte() {
    return delegate.hasLeadingZeroByte();
  }

  @Override
  public boolean hasLeadingZero() {
    return delegate.hasLeadingZero();
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
  public <T extends MutableBytes> T not(T result) {
    return delegate.not(result);
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
  public Bytes slice(int index) {
    return delegate.slice(index);
  }

  @Override
  public Bytes slice(int index, int length) {
    return delegate.slice(index, length);
  }

  @Override
  public Bytes copy() {
    return Bytes.wrap(delegate.toArray());
  }

  @Override
  public MutableBytes mutableCopy() {
    return MutableBytes.wrap(delegate.toArray());
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
  public int commonPrefixLength(Bytes other) {
    return delegate.commonPrefixLength(other);
  }

  @Override
  public Bytes commonPrefix(Bytes other) {
    return delegate.commonPrefix(other);
  }

  @Override
  public void update(MessageDigest digest) {
    delegate.update(digest);
  }

  @Override
  public byte[] toArray() {
    return delegate.toArray();
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
  public String toHexString() {
    return delegate.toHexString();
  }

  @Override
  public String toShortHexString() {
    return delegate.toShortHexString();
  }

  @Override
  public boolean equals(Object obj) {
    return delegate.equals(obj);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }
}
