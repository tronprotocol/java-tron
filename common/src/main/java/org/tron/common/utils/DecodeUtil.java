package org.tron.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.core.Constant;

@Slf4j(topic = "Commons")
public class DecodeUtil {

  public static final int ADDRESS_SIZE = 42;
  public static byte addressPreFixByte = Constant.ADD_PRE_FIX_BYTE_MAINNET;

  public static String addressPreFixString = Constant.ADD_PRE_FIX_STRING_MAINNET;

  public static boolean addressValid(byte[] address) {
    if (ArrayUtils.isEmpty(address)) {
      logger.debug("Address is empty !!");
      return false;
    }
    if (address.length != ADDRESS_SIZE / 2) {
      logger.debug("Address length need {} but {} !!", ADDRESS_SIZE / 2, address.length);
      return false;
    }

    if (address[0] != addressPreFixByte) {
      logger.debug("Address need prefix with {} but {} !!", addressPreFixByte, address[0]);
      return false;
    }
    return true;
  }

}
