package org.tron.common.utils;

import static org.tron.core.Constant.ADD_PRE_FIX_BYTE_MAINNET;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

@Slf4j(topic = "Commons")
public class DecodeUtil {

  public static final int ADDRESS_SIZE = 42;
  public static byte addressPreFixByte = ADD_PRE_FIX_BYTE_MAINNET;

  public static boolean addressValid(byte[] address) {
    if (ArrayUtils.isEmpty(address)) {
      logger.warn("Warning: Address is empty !!");
      return false;
    }
    if (address.length != ADDRESS_SIZE / 2) {
      logger.warn(
          "Warning: Address length need " + ADDRESS_SIZE + " but " + address.length
              + " !!");
      return false;
    }

    if (address[0] != addressPreFixByte) {
      logger.warn("Warning: Address need prefix with " + addressPreFixByte + " but "
          + address[0] + " !!");
      return false;
    }
    //Other rule;
    return true;
  }

  public static String createReadableString(byte[] bytes) {
    return ByteArray.toHexString(bytes);
  }

}
