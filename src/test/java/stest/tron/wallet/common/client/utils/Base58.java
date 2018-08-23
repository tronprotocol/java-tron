package stest.tron.wallet.common.client.utils;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import org.tron.common.utils.Sha256Hash;

public class Base58 {
  private static final int BASE58CHECK_ADDRESS_SIZE = 35;
  private static final int ADDRESS_SIZE = 21;
  private static final byte ADD_PRE_FIX_BYTE = (byte) 0xa0;

  public static final char[] ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
            .toCharArray();
  private static final int[] INDEXES = new int[128];

  static {
    for (int i = 0; i < INDEXES.length; i++) {
      INDEXES[i] = -1;
    }
    for (int i = 0; i < ALPHABET.length; i++) {
      INDEXES[ALPHABET[i]] = i;
    }
  }

  public static String encode(byte[] input) {
    if (input.length == 0) {
      return "";
    }
    input = copyOfRange(input, 0, input.length);
    int zeroCount = 0;
    while (zeroCount < input.length && input[zeroCount] == 0) {
      ++zeroCount;
    }
    byte[] temp = new byte[input.length * 2];
    int j = temp.length;

    int startAt = zeroCount;
    while (startAt < input.length) {
      byte mod = divmod58(input, startAt);
      if (input[startAt] == 0) {
        ++startAt;
      }
      temp[--j] = (byte) ALPHABET[mod];
    }

    // Strip extra '1' if there are some after decoding.
    while (j < temp.length && temp[j] == ALPHABET[0]) {
      ++j;
    }
    // Add as many leading '1' as there were leading zeros.
    while (--zeroCount >= 0) {
      temp[--j] = (byte) ALPHABET[0];
    }

    byte[] output = copyOfRange(temp, j, temp.length);
    try {
      return new String(output, "US-ASCII");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);  // Cannot happen.
    }
  }

  public static byte[] decode(String input) throws IllegalArgumentException {
    if (input.length() == 0) {
      return new byte[0];
    }
    byte[] input58 = new byte[input.length()];
    // Transform the String to a base58 byte sequence
    for (int i = 0; i < input.length(); ++i) {
      char c = input.charAt(i);

      int digit58 = -1;
      if (c >= 0 && c < 128) {
        digit58 = INDEXES[c];
      }
      if (digit58 < 0) {
        throw new IllegalArgumentException("Illegal character " + c + " at " + i);
      }

      input58[i] = (byte) digit58;
    }
    // Count leading zeroes
    int zeroCount = 0;
    while (zeroCount < input58.length && input58[zeroCount] == 0) {
      ++zeroCount;
    }
    byte[] temp = new byte[input.length()];
    int j = temp.length;

    int startAt = zeroCount;
    while (startAt < input58.length) {
      byte mod = divmod256(input58, startAt);
      if (input58[startAt] == 0) {
        ++startAt;
      }

      temp[--j] = mod;
    }
    // Do no add extra leading zeroes, move j to first non null byte.
    while (j < temp.length && temp[j] == 0) {
      ++j;
    }

    return copyOfRange(temp, j - zeroCount, temp.length);
  }

  public static BigInteger decodeToBigInteger(String input) throws IllegalArgumentException {
    return new BigInteger(1, decode(input));
  }

  private static byte divmod58(byte[] number, int startAt) {
    int remainder = 0;
    for (int i = startAt; i < number.length; i++) {
      int digit256 = (int) number[i] & 0xFF;
      int temp = remainder * 256 + digit256;

      number[i] = (byte) (temp / 58);

      remainder = temp % 58;
    }

    return (byte) remainder;
  }


  private static byte divmod256(byte[] number58, int startAt) {
    int remainder = 0;
    for (int i = startAt; i < number58.length; i++) {
      int digit58 = (int) number58[i] & 0xFF;
      int temp = remainder * 58 + digit58;

      number58[i] = (byte) (temp / 256);

      remainder = temp % 256;
    }

    return (byte) remainder;
  }

  private static byte[] copyOfRange(byte[] source, int from, int to) {
    byte[] range = new byte[to - from];
    System.arraycopy(source, from, range, 0, range.length);

    return range;
  }

  public static byte[] decodeFromBase58Check(String addressBase58) {
    if (addressBase58 == null || addressBase58.length() == 0) {
      System.out.println("Warning: Address is empty !!");
      return null;
    }
    if (addressBase58.length() != BASE58CHECK_ADDRESS_SIZE) {
      System.out.println(
                    "Warning: Base58 address length need " + BASE58CHECK_ADDRESS_SIZE + " but "
                            + addressBase58.length()
                            + " !!");
      return null;
    }
    byte[] address = decode58Check(addressBase58);
    if (!addressValid(address)) {
      return null;
    }
    return address;
  }

  private static byte[] decode58Check(String input) {
    byte[] decodeCheck = Base58.decode(input);
    if (decodeCheck.length <= 4) {
      return null;
    }
    byte[] decodeData = new byte[decodeCheck.length - 4];
    System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
    byte[] hash0 = Sha256Hash.hash(decodeData);
    byte[] hash1 = Sha256Hash.hash(hash0);
    if (hash1[0] == decodeCheck[decodeData.length]
        && hash1[1] == decodeCheck[decodeData.length + 1]
        && hash1[2] == decodeCheck[decodeData.length + 2]
        && hash1[3] == decodeCheck[decodeData.length + 3]) {
      return decodeData;
    }
    return null;
  }

  public static boolean addressValid(byte[] address) {
    if (address == null || address.length == 0) {
      System.out.println("Warning: Address is empty !!");
      return false;
    }
    if (address.length != ADDRESS_SIZE) {
      System.out.println(
                    "Warning: Address length need " + ADDRESS_SIZE + " but " + address.length
                            + " !!");
      return false;
    }
    byte preFixbyte = address[0];
    if (preFixbyte != ADD_PRE_FIX_BYTE) {
      System.out.println("Warning: Address need prefix with " + ADD_PRE_FIX_BYTE + " but "
                    + preFixbyte + " !!");
      return false;
    }
    //Other rule;
    return true;
  }

  public static String encode58Check(byte[] input) {
    byte[] hash0 = Sha256Hash.hash(input);
    byte[] hash1 = Sha256Hash.hash(hash0);
    byte[] inputCheck = new byte[input.length + 4];
    System.arraycopy(input, 0, inputCheck, 0, input.length);
    System.arraycopy(hash1, 0, inputCheck, input.length, 4);
    return Base58.encode(inputCheck);
  }
}