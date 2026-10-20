package org.tron.common.utils;

import org.apache.commons.lang3.ArrayUtils;
import org.tron.core.Constant;

public class DecodeUtil {

  public static final int ADDRESS_SIZE = 42;
  public static byte addressPreFixByte = Constant.ADD_PRE_FIX_BYTE_MAINNET;

  public static String addressPreFixString = Constant.ADD_PRE_FIX_STRING_MAINNET;

  public static boolean addressValid(byte[] address) {
    if (ArrayUtils.isEmpty(address)) {
      return false;
    }
    if (address.length != ADDRESS_SIZE / 2) {
      return false;
    }

    if (address[0] != addressPreFixByte) {
      return false;
    }
    return true;
  }

}
