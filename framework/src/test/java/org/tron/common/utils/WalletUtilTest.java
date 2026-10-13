package org.tron.common.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;

import com.google.protobuf.ByteString;
import java.util.Arrays;
import java.util.Collections;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.mockito.MockedStatic;

public class WalletUtilTest {

  @Test
  public void encodesAddressSizesAsBase58AndDoesNotModifyInput() {
    int[] sizes = {20, 21};
    String[] expected = {"6x36TdFVhCQBxJz1MzEoySdejiLcwZMed",
        "TFvF6WzyeRKMM8NcbddxL88mZXTeuUWA8Z"};
    for (int i = 0; i < sizes.length; i++) {
      byte[] input = bytes(sizes[i]);
      byte[] original = input.clone();
      assertEquals(expected[i], StringUtil.encode58Check(input));
      assertEquals(expected[i], WalletUtil.getAddressString(input));
      assertArrayEquals(original, input);
    }
  }

  @Test
  public void formatsShortInputsAsFullHex() {
    assertEquals("invalid-address(,len=0)", WalletUtil.getAddressString(bytes(0)));
    assertEquals("invalid-address(41,len=1)", WalletUtil.getAddressString(bytes(1)));
    assertEquals("invalid-address(" + StringUtils.repeat("41", 19) + ",len=19)",
        WalletUtil.getAddressString(bytes(19)));
  }

  @Test
  public void formatsNullAsInvalidAddress() {
    assertEquals("invalid-address(null)", WalletUtil.getAddressString(null));
  }

  @Test
  public void formatsOversizedInputsWithoutHashingOrBase58Conversion() {
    for (int size : new int[]{22, 4096, 16384, 65536}) {
      byte[] input = bytes(size);
      try (MockedStatic<Sha256Hash> hash = mockStatic(Sha256Hash.class);
           MockedStatic<Base58> base58 = mockStatic(Base58.class)) {
        String expected = "invalid-address(" + StringUtils.repeat("41", 21)
            + "...,len=" + size + ")";
        assertEquals(expected, WalletUtil.getAddressString(input));
        hash.verifyNoInteractions();
        base58.verifyNoInteractions();
      }
    }
  }

  @Test
  public void preservesOrdinaryWitnessLists() {
    ByteString address = ByteString.copyFrom(bytes(21));
    assertEquals(Collections.nCopies(27, StringUtil.encode58Check(address.toByteArray())),
        WalletUtil.getAddressStringList(Collections.nCopies(27, address)));
    assertTrue(WalletUtil.getAddressStringList(Collections.emptyList()).isEmpty());
  }

  private static byte[] bytes(int size) {
    byte[] input = new byte[size];
    Arrays.fill(input, (byte) 0x41);
    return input;
  }
}
