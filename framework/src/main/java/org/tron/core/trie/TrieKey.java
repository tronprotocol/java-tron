package org.tron.core.trie;

import static org.tron.common.utils.ByteArray.toHexString;
import static org.tron.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;

/**
 * Created by Anton Nashatyrev on 13.02.2017.
 */
public final class TrieKey {

  public static final int ODD_OFFSET_FLAG = 0x1;
  public static final int TERMINATOR_FLAG = 0x2;
  private final byte[] key;
  private final int off;
  private final boolean terminal;

  public TrieKey(byte[] key, int off, boolean terminal) {
    this.terminal = terminal;
    this.off = off;
    this.key = key;
  }

  private TrieKey(byte[] key) {
    this(key, 0, true);
  }

  public static TrieKey fromNormal(byte[] key) {
    return new TrieKey(key);
  }

  public static TrieKey fromPacked(byte[] key) {
    return new TrieKey(key, ((key[0] >> 4) & ODD_OFFSET_FLAG) != 0 ? 1 : 2,
        ((key[0] >> 4) & TERMINATOR_FLAG) != 0);
  }

  public static TrieKey empty(boolean terminal) {
    return new TrieKey(EMPTY_BYTE_ARRAY, 0, terminal);
  }

  public static TrieKey singleHex(int hex) {
    TrieKey ret = new TrieKey(new byte[1], 1, false);
    ret.setHex(0, hex);
    return ret;
  }

  public byte[] toPacked() {
    int flags = ((off & 1) != 0 ? ODD_OFFSET_FLAG : 0) | (terminal ? TERMINATOR_FLAG : 0);
    byte[] ret = new byte[getLength() / 2 + 1];
    int toCopy = (flags & ODD_OFFSET_FLAG) != 0 ? ret.length : ret.length - 1;
    System.arraycopy(key, key.length - toCopy, ret, ret.length - toCopy, toCopy);
    ret[0] &= 0x0F;
    ret[0] |= flags << 4;
    return ret;
  }

  public byte[] toNormal() {
    if ((off & 1) != 0) {
      throw new RuntimeException(
          "Can't convert a key with odd number of hexes to normal: " + this);
    }
    int arrLen = key.length - off / 2;
    byte[] ret = new byte[arrLen];
    System.arraycopy(key, key.length - arrLen, ret, 0, arrLen);
    return ret;
  }

  public boolean isTerminal() {
    return terminal;
  }

  public boolean isEmpty() {
    return getLength() == 0;
  }

  public TrieKey shift(int hexCnt) {
    return new TrieKey(this.key, off + hexCnt, terminal);
  }

  public TrieKey getCommonPrefix(TrieKey k) {
    // TODO can be optimized
    int prefixLen = 0;
    int thisLength = getLength();
    int kLength = k.getLength();
    while (prefixLen < thisLength && prefixLen < kLength && getHex(prefixLen) == k
        .getHex(prefixLen)) {
      prefixLen++;
    }
    byte[] prefixKey = new byte[(prefixLen + 1) >> 1];
    TrieKey ret = new TrieKey(prefixKey, (prefixLen & 1) == 0 ? 0 : 1,
        prefixLen == getLength() && prefixLen == k.getLength() && terminal && k.isTerminal());
    for (int i = 0; i < prefixLen; i++) {
      ret.setHex(i, k.getHex(i));
    }
    return ret;
  }

  public TrieKey matchAndShift(TrieKey k) {
    int len = getLength();
    int kLen = k.getLength();
    if (len < kLen) {
      return null;
    }

    if ((off & 1) == (k.off & 1)) {
      // optimization to compare whole keys bytes
      if ((off & 1) == 1 && getHex(0) != k.getHex(0)) {
        return null;
      }
      int idx1 = (off + 1) >> 1;
      int idx2 = (k.off + 1) >> 1;
      int l = kLen >> 1;
      for (int i = 0; i < l; i++, idx1++, idx2++) {
        if (key[idx1] != k.key[idx2]) {
          return null;
        }
      }
    } else {
      for (int i = 0; i < kLen; i++) {
        if (getHex(i) != k.getHex(i)) {
          return null;
        }
      }
    }
    return shift(kLen);
  }

  public int getLength() {
    return (key.length << 1) - off;
  }

  private void setHex(int idx, int hex) {
    int byteIdx = (off + idx) >> 1;
    if (((off + idx) & 1) == 0) {
      key[byteIdx] &= 0x0F;
      key[byteIdx] |= hex << 4;
    } else {
      key[byteIdx] &= 0xF0;
      key[byteIdx] |= hex;
    }
  }

  public int getHex(int idx) {
    byte b = key[(off + idx) >> 1];
    return (((off + idx) & 1) == 0 ? (b >> 4) : b) & 0xF;
  }

  public TrieKey concat(TrieKey k) {
    if (isTerminal()) {
      throw new RuntimeException("Can' append to terminal key: " + this + " + " + k);
    }
    int len = getLength();
    int kLen = k.getLength();
    int newLen = len + kLen;
    byte[] newKeyBytes = new byte[(newLen + 1) >> 1];
    TrieKey ret = new TrieKey(newKeyBytes, newLen & 1, k.isTerminal());
    for (int i = 0; i < len; i++) {
      ret.setHex(i, getHex(i));
    }
    for (int i = 0; i < kLen; i++) {
      ret.setHex(len + i, k.getHex(i));
    }
    return ret;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (this.getClass() != obj.getClass()) {
      return false;
    }
    TrieKey k = (TrieKey) obj;
    int len = getLength();

    if (len != k.getLength()) {
      return false;
    }
    // TODO can be optimized
    for (int i = 0; i < len; i++) {
      if (getHex(i) != k.getHex(i)) {
        return false;
      }
    }
    return isTerminal() == k.isTerminal();
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public String toString() {
    return toHexString(key).substring(off) + (isTerminal() ? "T" : "");
  }
}
