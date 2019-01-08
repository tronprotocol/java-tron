package org.tron.common.logsfilter;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.sun.org.apache.xpath.internal.operations.Mult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.pf4j.util.StringUtils;
import org.spongycastle.crypto.OutputLengthException;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.Hash;
import org.tron.common.runtime.utils.MUtil;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.SmartContract.ABI;

@Slf4j(topic = "Parser")
public class ContractEventParser {

  private static final int DATAWORD_UNIT_SIZE = 32;

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

  /**
   * parse Event Topic into map NOTICE: In solidity, Indexed Dynamic types's topic is just
   * EVENT_INDEXED_ARGS
   */
  public static Map<String, String> parseTopics(List<byte[]> topicList, ABI.Entry entry) {
    Map<String, String> map = new HashMap<>();
    if (topicList == null || topicList.isEmpty()) {
      return map;
    }

    // the first is the signature.
    int index = 1;
    List<ABI.Entry.Param> list = entry.getInputsList();

    // in case indexed topics doesn't match
    if (topicsMatched(topicList, entry)) {
      for (int i = 0; i < list.size(); ++i) {
        ABI.Entry.Param param = list.get(i);
        if (!param.getIndexed()) {
          continue;
        }

        if (index >= topicList.size()) {
          break;
        }
        String str = parseTopic(topicList.get(index++), param.getType());
        if (StringUtils.isNotNullOrEmpty(param.getName())) {
          map.put(param.getName(), str);
        }
        map.put("" + i, str);
      }
    } else {
      for (int i = 1; i < topicList.size(); ++i) {
        map.put("" + (i - 1), DataWord.shortHex(topicList.get(i)));
      }
    }
    return map;
  }

  /**
   * parse Event Data into map<String, Object> If parser failed, then return {"0",
   * Hex.toHexString(data)} Only support basic solidity type, String, Bytes. Fixed Array or dynamic
   * Array are not support yet (then return {"0": Hex.toHexString(data)}).
   */
  public static Map<String, String> parseEventData(byte[] data,
      List<byte[]> topicList, ABI.Entry entry) {
    Map<String, String> map = new HashMap<>();
    if (ArrayUtils.isEmpty(data)) {
      return map;
    }
    // in case indexed topics doesn't match
    if (!topicsMatched(topicList, entry)) {
      map.put("" + (topicList.size() - 1), Hex.toHexString(data));
      return map;
    }

    // the first is the signature.
    List<ABI.Entry.Param> list = entry.getInputsList();
    Integer startIndex = 0;
    try {
      // this one starts from the first position.
      int index = 0;
      for (Integer i = 0; i < list.size(); ++i) {
        ABI.Entry.Param param = list.get(i);
        if (param.getIndexed()) {
          continue;
        }
        if (startIndex == 0) {
          startIndex = i;
        }

        String str = parseDataBytes(data, param.getType(), index++);
        if (StringUtils.isNotNullOrEmpty(param.getName())) {
          map.put(param.getName(), str);
        }
        map.put("" + i, str);

      }
      if (list.size() == 0) {
        map.put("0", Hex.toHexString(data));
      }
    } catch (UnsupportedOperationException e) {
      logger.debug("UnsupportedOperationException", e);
      map.clear();
      map.put(startIndex.toString(), Hex.toHexString(data));
    }
    return map;
  }

  private static boolean topicsMatched(List<byte[]> topicList, ABI.Entry entry) {
    if (topicList == null || topicList.isEmpty()) {
      return true;
    }
    int inputSize = 1;
    for (ABI.Entry.Param param : entry.getInputsList()) {
      if (param.getIndexed()) {
        inputSize++;
      }
    }
    return inputSize == topicList.size();
  }

  private static String parseDataBytes(byte[] data, String typeStr, int index) {

    try {
      byte[] startBytes = subBytes(data, index * DATAWORD_UNIT_SIZE, DATAWORD_UNIT_SIZE);
      Type type = basicType(typeStr);

      if (type == Type.INT_NUMBER) {
        return new BigInteger(startBytes).toString();
      } else if (type == Type.BOOL) {
        return String.valueOf(!DataWord.isZero(startBytes));
      } else if (type == Type.FIXED_BYTES){
        return Hex.toHexString(startBytes);
      } else if (type == Type.ADDRESS){
        byte[] last20Bytes = Arrays.copyOfRange(startBytes, 12, startBytes.length);
        return Wallet.encode58Check(MUtil.convertToTronAddress(last20Bytes));
      } else if (type == Type.STRING || type == Type.BYTES) {
        int start = intValueExact(startBytes);
        byte[] lengthBytes = subBytes(data, start, DATAWORD_UNIT_SIZE);
        // this length is byte count. no need X 32
        int length = intValueExact(lengthBytes);
        byte[] realBytes = subBytes(data, start + DATAWORD_UNIT_SIZE, length);
        return type == Type.STRING ? new String(realBytes) : DataWord.shortHex(realBytes);
      }
    } catch (OutputLengthException | ArithmeticException e) {
      logger.debug("parseDataBytes ", e);
    }
    throw new UnsupportedOperationException("unsupported type:" + typeStr);
  }

  // don't support these type yet : bytes32[10][10]  OR  bytes32[][10]
  private static Type basicType(String type) {
    if (!Pattern.matches("^.*\\[\\d*\\]$", type)) {
      // ignore not valide type such as "int92", "bytes33", these types will be compiled failed.
      if ((type.startsWith("int") || type.startsWith("uint"))) {
        return Type.INT_NUMBER;
      } else if (type.equals("bool")) {
        return Type.BOOL;
      } else if (type.equals("address")) {
        return Type.ADDRESS;
      } else if (Pattern.matches("^bytes\\d+$", type)) {
        return Type.FIXED_BYTES;
      } else if (type.equals("string")){
        return Type.STRING;
      } else if (type.equals("bytes")){
        return Type.BYTES;
      }
    }
    return Type.UNKNOWN;
  }

  private static Integer intValueExact(byte[] data) {
    return new BigInteger(data).intValueExact();
  }

  private static byte[] subBytes(byte[] src, int start, int length) {
    if (ArrayUtils.isEmpty(src) || start >= src.length || length < 0) {
      throw new OutputLengthException("data start:" + start + ", length:" + length);
    }
    byte[] dst = new byte[length];
    System.arraycopy(src, start, dst, 0, Math.min(length, src.length - start));
    return dst;
  }

  /**
   * support: uint<m> (m ∈ [8, 256], m % 8 == 0), int<m> (m ∈ [8, 256], m % 8 == 0) uint (solidity
   * abi will auto convert to uint256) int (solidity abi will auto convert to int256) bool
   *
   * otherwise, returns hexString
   *
   * This is only for decode Topic. Since Topic and Data use different encode methods when deal
   * dynamic length types, such as bytes and string.
   */
  private static String parseTopic(byte[] bytes, String typeStr) {
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
      return Wallet.encode58Check(MUtil.convertToTronAddress(last20Bytes));
    }
    return DataWord.shortHex(bytes);
  }
}
