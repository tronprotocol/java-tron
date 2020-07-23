package org.tron.common.logsfilter;

import java.math.BigInteger;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.pf4j.util.StringUtils;
import org.spongycastle.crypto.OutputLengthException;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.StringUtil;
import org.tron.core.db.TransactionTrace;

@Slf4j(topic = "Parser")
public class ContractEventParser {

  private static final int DATAWORD_UNIT_SIZE = 32;

  protected static String parseDataBytes(byte[] data, String typeStr, int index) {
    try {
      byte[] startBytes = subBytes(data, index * DATAWORD_UNIT_SIZE, DATAWORD_UNIT_SIZE);
      Type type = basicType(typeStr);

      if (type == Type.INT_NUMBER) {
        return new BigInteger(startBytes).toString();
      } else if (type == Type.BOOL) {
        return String.valueOf(!DataWord.isZero(startBytes));
      } else if (type == Type.FIXED_BYTES) {
        return Hex.toHexString(startBytes);
      } else if (type == Type.ADDRESS) {
        byte[] last20Bytes = Arrays.copyOfRange(startBytes, 12, startBytes.length);
        return StringUtil.encode58Check(TransactionTrace.convertToTronAddress(last20Bytes));
      } else if (type == Type.STRING || type == Type.BYTES) {
        int start = intValueExact(startBytes);
        byte[] lengthBytes = subBytes(data, start, DATAWORD_UNIT_SIZE);
        // this length is byte count. no need X 32
        int length = intValueExact(lengthBytes);
        byte[] realBytes =
            length > 0 ? subBytes(data, start + DATAWORD_UNIT_SIZE, length) : new byte[0];
        return type == Type.STRING ? new String(realBytes) : Hex.toHexString(realBytes);
      }
    } catch (OutputLengthException | ArithmeticException e) {
      logger.debug("parseDataBytes ", e);
    }
    throw new UnsupportedOperationException("unsupported type:" + typeStr);
  }

  // don't support these type yet : bytes32[10][10]  OR  bytes32[][10]
  protected static Type basicType(String type) {
    if (!Pattern.matches("^.*\\[\\d*\\]$", type)) {
      // ignore not valid type such as "int92", "bytes33", these types will be compiled failed.
      if (type.startsWith("int") || type.startsWith("uint") || type.startsWith("trcToken")) {
        return Type.INT_NUMBER;
      } else if ("bool".equals(type)) {
        return Type.BOOL;
      } else if ("address".equals(type)) {
        return Type.ADDRESS;
      } else if (Pattern.matches("^bytes\\d+$", type)) {
        return Type.FIXED_BYTES;
      } else if ("string".equals(type)) {
        return Type.STRING;
      } else if ("bytes".equals(type)) {
        return Type.BYTES;
      }
    }
    return Type.UNKNOWN;
  }

  protected static Integer intValueExact(byte[] data) {
    return new BigInteger(data).intValueExact();
  }

  protected static byte[] subBytes(byte[] src, int start, int length) {
    if (ArrayUtils.isEmpty(src) || start >= src.length || length < 0) {
      throw new OutputLengthException("data start:" + start + ", length:" + length);
    }
    byte[] dst = new byte[length];
    System.arraycopy(src, start, dst, 0, Math.min(length, src.length - start));
    return dst;
  }

  /**
   * support: uint m, (m ∈ [8, 256], m % 8 == 0), int m, (m ∈ [8, 256], m % 8 == 0) uint (solidity
   * abi will auto convert to uint256) int (solidity abi will auto convert to int256) bool
   *
   * otherwise, returns hexString
   *
   * This is only for decode Topic. Since Topic and Data use different encode methods when deal
   * dynamic length types, such as bytes and string.
   */
  protected static String parseTopic(byte[] bytes, String typeStr) {
    if (ArrayUtils.isEmpty(bytes) || StringUtils.isNullOrEmpty(typeStr)) {
      return "";
    }
    Type type = basicType(typeStr);
    if (type == Type.INT_NUMBER) {
      return DataWord.bigIntValue(bytes);
    } else if (type == Type.BOOL) {
      return String.valueOf(!DataWord.isZero(bytes));
    } else if (type == Type.ADDRESS) {
      byte[] last20Bytes = Arrays.copyOfRange(bytes, 12, bytes.length);
      return StringUtil.encode58Check(TransactionTrace.convertToTronAddress(last20Bytes));
    }
    return Hex.toHexString(bytes);
  }

  private enum Type {
    UNKNOWN,
    INT_NUMBER,
    BOOL,
    FLOAT_NUMBER,
    FIXED_BYTES,
    ADDRESS,
    STRING,
    BYTES,
  }
}
