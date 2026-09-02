package org.tron.core.capsule.utils;

import static java.util.Arrays.copyOfRange;
import static org.tron.common.utils.ByteUtil.byteArrayToInt;

import org.tron.common.crypto.Hash;

/**
 * Recursive Length Prefix (RLP) encoding, as used by the account-state trie.
 *
 * <p>RLP encodes arbitrarily nested arrays of binary data. It describes structure only: an item is
 * either a byte array or a list of items, and how atomic types map onto byte arrays is left to the
 * caller. See https://github.com/ethereum/wiki/wiki/%5BEnglish%5D-RLP
 *
 * <p>TRON serializes its own data with protobuf; the only consumer of this class is
 * {@code org.tron.core.trie.TrieImpl}, which needs RLP because the trie node layout it implements
 * is defined in terms of RLP-encoded lists. Accordingly this class carries only what that layout
 * requires: list encoding, lazy list decoding, and the {@link LList} view over a decoded list.
 *
 * @author Roman Mandeleil
 * @since 01.04.2014
 */
public class RLP {

  /**
   * Reason for threshold according to Vitalik Buterin: - 56 bytes maximizes the benefit of both
   * options - if we went with 60 then we would have only had 4 slots for long strings so RLP would
   * not have been able to store objects above 4gb - if we went with 48 then RLP would be fine for
   * 2^128 space, but that's way too much - so 56 and 2^64 space seems like the right place to put
   * the cutoff - also, that's where Bitcoin's varint does the cutof
   */
  private static final int SIZE_THRESHOLD = 56;

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

  public static final byte[] EMPTY_ELEMENT_RLP = Hash.encodeElement(new byte[0]);

  /**
   * [0xc0] If the total payload of a list (i.e. the combined length of all its items) is 0-55 bytes
   * long, the RLP encoding consists of a single byte with value 0xc0 plus the length of the list
   * followed by the concatenation of the RLP encodings of the items. The range of the first byte is
   * thus [0xc0, 0xf7].
   */
  private static final int OFFSET_SHORT_LIST = 0xc0;

  /**
   * [0xf7] If the total payload of a list is more than 55 bytes long, the RLP encoding consists of
   * a single byte with value 0xf7 plus the length of the length of the list in binary form,
   * followed by the length of the list, followed by the concatenation of the RLP encodings of the
   * items. The range of the first byte is thus [0xf8, 0xff].
   */
  private static final int OFFSET_LONG_LIST = 0xf7;

  /* ******************************************************
   *                      DECODING                        *
   * ******************************************************/

  /**
   * Compares supplied length information with maximum possible.
   *
   * @param suppliedLength Length info from header
   * @param availableLength Length of remaining object
   * @throws RuntimeException if supplied length is bigger than available
   */
  private static void verifyLength(int suppliedLength, int availableLength) {
    if (suppliedLength > availableLength) {
      throw new RuntimeException(String.format("Length parsed from RLP (%s bytes) is greater "
          + "than possible size of data (%s bytes)", suppliedLength, availableLength));
    }
  }

  public static LList decodeLazyList(byte[] data) {
    LList lList = decodeLazyList(data, 0, data.length);
    return lList == null ? null : lList.getList(0);
  }

  public static LList decodeLazyList(byte[] data, int pos, int length) {
    if (data == null || data.length < 1) {
      return null;
    }
    LList ret = new LList(data);
    int end = pos + length;

    while (pos < end) {
      int prefix = data[pos] & 0xFF;
      if (prefix == OFFSET_SHORT_ITEM) {  // 0x80
        ret.add(pos, 0, false); // means no length or 0
        pos++;
      } else if (prefix < OFFSET_SHORT_ITEM) {  // [0x00, 0x7f]
        ret.add(pos, 1, false); // means no length or 0
        pos++;
      } else if (prefix <= OFFSET_LONG_ITEM) {  // [0x81, 0xb7]
        int len = prefix - OFFSET_SHORT_ITEM; // length of the encoded bytes
        ret.add(pos + 1, len, false);
        pos += len + 1;
      } else if (prefix < OFFSET_SHORT_LIST) {  // [0xb8, 0xbf]
        int lenlen = prefix - OFFSET_LONG_ITEM; // length of length the encoded bytes
        int lenbytes = byteArrayToInt(
            copyOfRange(data, pos + 1, pos + 1 + lenlen)); // length of encoded bytes
        // check that length is in payload bounds
        verifyLength(lenbytes, data.length - pos - 1 - lenlen);
        ret.add(pos + 1 + lenlen, lenbytes, false);
        pos += 1 + lenlen + lenbytes;
      } else if (prefix <= OFFSET_LONG_LIST) {  // [0xc0, 0xf7]
        int len = prefix - OFFSET_SHORT_LIST; // length of the encoded list
        ret.add(pos + 1, len, true);
        pos += 1 + len;
      } else if (prefix <= 0xFF) {  // [0xf8, 0xff]
        int lenlen = prefix - OFFSET_LONG_LIST; // length of length the encoded list
        int lenlist = byteArrayToInt(
            copyOfRange(data, pos + 1, pos + 1 + lenlen)); // length of encoded bytes
        // check that length is in payload bounds
        verifyLength(lenlist, data.length - pos - 1 - lenlen);
        ret.add(pos + 1 + lenlen, lenlist, true);
        pos += 1 + lenlen + lenlist; // start at position of first element in list
      } else {
        throw new RuntimeException(
            "Only byte values between 0x00 and 0xFF are supported, but got: " + prefix);
      }
    }
    return ret;
  }

  /* ******************************************************
   *                      ENCODING                        *
   * ******************************************************/

  public static byte[] encodeList(byte[]... elements) {

    if (elements == null) {
      return new byte[]{(byte) OFFSET_SHORT_LIST};
    }

    int totalLength = 0;
    for (byte[] element1 : elements) {
      totalLength += element1.length;
    }

    byte[] data;
    int copyPos;
    if (totalLength < SIZE_THRESHOLD) {

      data = new byte[1 + totalLength];
      data[0] = (byte) (OFFSET_SHORT_LIST + totalLength);
      copyPos = 1;
    } else {
      // length of length = BX
      // prefix = [BX, [length]]
      int tmpLength = totalLength;
      byte byteNum = 0;
      while (tmpLength != 0) {
        ++byteNum;
        tmpLength = tmpLength >> 8;
      }
      tmpLength = totalLength;
      byte[] lenBytes = new byte[byteNum];
      for (int i = 0; i < byteNum; ++i) {
        lenBytes[byteNum - 1 - i] = (byte) ((tmpLength >> (8 * i)) & 0xFF);
      }
      // first byte = F7 + bytes.length
      data = new byte[1 + lenBytes.length + totalLength];
      data[0] = (byte) (OFFSET_LONG_LIST + byteNum);
      System.arraycopy(lenBytes, 0, data, 1, lenBytes.length);

      copyPos = lenBytes.length + 1;
    }
    for (byte[] element : elements) {
      System.arraycopy(element, 0, data, copyPos, element.length);
      copyPos += element.length;
    }
    return data;
  }

  public static byte[] encodeList(Object... elements) {

    if (elements == null) {
      return new byte[]{(byte) OFFSET_SHORT_LIST};
    }

    int totalLength = 0;
    for (Object element1 : elements) {
      byte[] value = (byte[]) element1;
      totalLength += value.length;
    }

    byte[] data;
    int copyPos;
    if (totalLength < SIZE_THRESHOLD) {

      data = new byte[1 + totalLength];
      data[0] = (byte) (OFFSET_SHORT_LIST + totalLength);
      copyPos = 1;
    } else {
      // length of length = BX
      // prefix = [BX, [length]]
      int tmpLength = totalLength;
      byte byteNum = 0;
      while (tmpLength != 0) {
        ++byteNum;
        tmpLength = tmpLength >> 8;
      }
      tmpLength = totalLength;
      byte[] lenBytes = new byte[byteNum];
      for (int i = 0; i < byteNum; ++i) {
        lenBytes[byteNum - 1 - i] = (byte) ((tmpLength >> (8 * i)) & 0xFF);
      }
      // first byte = F7 + bytes.length
      data = new byte[1 + lenBytes.length + totalLength];
      data[0] = (byte) (OFFSET_LONG_LIST + byteNum);
      System.arraycopy(lenBytes, 0, data, 1, lenBytes.length);

      copyPos = lenBytes.length + 1;
    }
    for (Object object : elements) {
      byte[] element = (byte[]) object;
      System.arraycopy(element, 0, data, copyPos, element.length);
      copyPos += element.length;
    }
    return data;
  }

  /**
   * A lazy view over a decoded RLP list: the elements are recorded as offsets into the original
   * payload and materialised only when read.
   */
  public static final class LList {

    private final byte[] rlp;
    private final int[] offsets = new int[32];
    private final int[] lens = new int[32];
    private int cnt;

    public LList(byte[] rlp) {
      this.rlp = rlp;
    }

    public byte[] getEncoded() {
      byte[][] encoded = new byte[cnt][];
      for (int i = 0; i < cnt; i++) {
        encoded[i] = Hash.encodeElement(getBytes(i));
      }
      return encodeList(encoded);
    }

    public void add(int off, int len, boolean isList) {
      offsets[cnt] = off;
      lens[cnt] = isList ? (-1 - len) : len;
      cnt++;
    }

    public byte[] getBytes(int idx) {
      int len = lens[idx];
      len = len < 0 ? (-len - 1) : len;
      byte[] ret = new byte[len];
      System.arraycopy(rlp, offsets[idx], ret, 0, len);
      return ret;
    }

    public LList getList(int idx) {
      return decodeLazyList(rlp, offsets[idx], -lens[idx] - 1);
    }

    public boolean isList(int idx) {
      return lens[idx] < 0;
    }

    public int size() {
      return cnt;
    }
  }
}
