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


import java.security.MessageDigest;
import java.util.List;

import static org.apache.tuweni.bytes.Checks.checkArgument;
import static org.apache.tuweni.bytes.Checks.checkElementIndex;

final class ConcatenatedBytes extends AbstractBytes {

  private final Bytes[] values;
  private final int size;

  private ConcatenatedBytes(Bytes[] values, int totalSize) {
    this.values = values;
    this.size = totalSize;
  }

  static Bytes wrap(Bytes... values) {
    if (values.length == 0) {
      return EMPTY;
    }
    if (values.length == 1) {
      return values[0];
    }

    int count = 0;
    int totalSize = 0;

    for (Bytes value : values) {
      int size = value.size();
      try {
        totalSize = Math.addExact(totalSize, size);
      } catch (ArithmeticException e) {
        throw new IllegalArgumentException("Combined length of values is too long (> Integer.MAX_VALUE)");
      }
      if (value instanceof ConcatenatedBytes) {
        count += ((ConcatenatedBytes) value).values.length;
      } else if (size != 0) {
        count += 1;
      }
    }

    if (count == 0) {
      return Bytes.EMPTY;
    }
    if (count == values.length) {
      return new ConcatenatedBytes(values, totalSize);
    }

    Bytes[] concatenated = new Bytes[count];
    int i = 0;
    for (Bytes value : values) {
      if (value instanceof ConcatenatedBytes) {
        Bytes[] subvalues = ((ConcatenatedBytes) value).values;
        System.arraycopy(subvalues, 0, concatenated, i, subvalues.length);
        i += subvalues.length;
      } else if (value.size() != 0) {
        concatenated[i++] = value;
      }
    }
    return new ConcatenatedBytes(concatenated, totalSize);
  }

  static Bytes wrap(List<Bytes> values) {
    if (values.size() == 0) {
      return EMPTY;
    }
    if (values.size() == 1) {
      return values.get(0);
    }

    int count = 0;
    int totalSize = 0;

    for (Bytes value : values) {
      int size = value.size();
      try {
        totalSize = Math.addExact(totalSize, size);
      } catch (ArithmeticException e) {
        throw new IllegalArgumentException("Combined length of values is too long (> Integer.MAX_VALUE)");
      }
      if (value instanceof ConcatenatedBytes) {
        count += ((ConcatenatedBytes) value).values.length;
      } else if (size != 0) {
        count += 1;
      }
    }

    if (count == 0) {
      return Bytes.EMPTY;
    }
    if (count == values.size()) {
      return new ConcatenatedBytes(values.toArray(new Bytes[0]), totalSize);
    }

    Bytes[] concatenated = new Bytes[count];
    int i = 0;
    for (Bytes value : values) {
      if (value instanceof ConcatenatedBytes) {
        Bytes[] subvalues = ((ConcatenatedBytes) value).values;
        System.arraycopy(subvalues, 0, concatenated, i, subvalues.length);
        i += subvalues.length;
      } else if (value.size() != 0) {
        concatenated[i++] = value;
      }
    }
    return new ConcatenatedBytes(concatenated, totalSize);
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public byte get(int i) {
    checkElementIndex(i, size);
    for (Bytes value : values) {
      int vSize = value.size();
      if (i < vSize) {
        return value.get(i);
      }
      i -= vSize;
    }
    throw new IllegalStateException("element sizes do not match total size");
  }

  @Override
  public Bytes slice(int i, final int length) {
    if (i == 0 && length == size) {
      return this;
    }
    if (length == 0) {
      return Bytes.EMPTY;
    }

    checkElementIndex(i, size);
    checkArgument(
        (i + length) <= size,
        "Provided length %s is too large: the value has size %s and has only %s bytes from %s",
        length,
        size,
        size - i,
        i);

    int j = 0;
    int vSize;
    while (true) {
      vSize = values[j].size();
      if (i < vSize) {
        break;
      }
      i -= vSize;
      ++j;
    }

    if ((i + length) < vSize) {
      return values[j].slice(i, length);
    }

    int remaining = length - (vSize - i);
    Bytes firstValue = this.values[j].slice(i);
    int firstOffset = j;

    while (remaining > 0) {
      if (++j >= this.values.length) {
        throw new IllegalStateException("element sizes do not match total size");
      }
      vSize = this.values[j].size();
      if (length < vSize + firstValue.size()) {
        break;
      }
      remaining -= vSize;
    }

    Bytes[] combined = new Bytes[j - firstOffset + 1];
    combined[0] = firstValue;
    if (remaining > 0) {
      if (combined.length > 2) {
        System.arraycopy(this.values, firstOffset + 1, combined, 1, combined.length - 2);
      }
      combined[combined.length - 1] = this.values[j].slice(0, remaining);
    } else if (combined.length > 1) {
      System.arraycopy(this.values, firstOffset + 1, combined, 1, combined.length - 1);
    }
    return new ConcatenatedBytes(combined, length);
  }

  @Override
  public Bytes copy() {
    return mutableCopy();
  }

  @Override
  public MutableBytes mutableCopy() {
    if (size == 0) {
      return MutableBytes.EMPTY;
    }
    MutableBytes result = MutableBytes.create(size);
    copyToUnchecked(result, 0);
    return result;
  }

  @Override
  public void copyTo(MutableBytes destination, int destinationOffset) {
    if (size == 0) {
      return;
    }

    checkElementIndex(destinationOffset, destination.size());
    checkArgument(
        destination.size() - destinationOffset >= size,
        "Cannot copy %s bytes, destination has only %s bytes from index %s",
        size,
        destination.size() - destinationOffset,
        destinationOffset);

    copyToUnchecked(destination, destinationOffset);
  }

  @Override
  public void update(MessageDigest digest) {
    for (Bytes value : values) {
      value.update(digest);
    }
  }

  @Override
  public byte[] toArray() {
    if (size == 0) {
      return new byte[0];
    }

    MutableBytes result = MutableBytes.create(size);
    copyToUnchecked(result, 0);
    return result.toArrayUnsafe();
  }

  private void copyToUnchecked(MutableBytes destination, int destinationOffset) {
    int offset = 0;
    for (Bytes value : values) {
      int vSize = value.size();
      if ((offset + vSize) > size) {
        throw new IllegalStateException("element sizes do not match total size");
      }
      value.copyTo(destination, destinationOffset);
      offset += vSize;
      destinationOffset += vSize;
    }
  }

  @Override
  public int hashCode() {
    return computeHashcode();
  }
}
