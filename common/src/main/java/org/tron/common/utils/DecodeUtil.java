package org.tron.common.utils;

import static java.util.Arrays.copyOfRange;
import static org.tron.common.utils.Hash.sha3;
import static org.tron.core.Constant.ADD_PRE_FIX_BYTE_MAINNET;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.spongycastle.math.ec.ECPoint;

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

  public static byte[] computeAddress(ECPoint pubPoint) {
    return computeAddress(pubPoint.getEncoded(/* uncompressed */ false));
  }

  /**
   * Calculates RIGTMOST160(SHA3(input)). This is used in address calculations. *
   *
   * @param input - data
   * @return - add_pre_fix + 20 right bytes of the hash keccak of the data
   */
  public static byte[] sha3omit12(byte[] input) {
    byte[] hash = sha3(input);
    byte[] address = copyOfRange(hash, 11, hash.length);
    address[0] = DecodeUtil.addressPreFixByte;
    return address;
  }

  public static byte[] computeAddress(byte[] pubBytes) {
    return sha3omit12(
        Arrays.copyOfRange(pubBytes, 1, pubBytes.length));
  }
}
