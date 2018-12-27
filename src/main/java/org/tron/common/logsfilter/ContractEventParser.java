package org.tron.common.logsfilter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math.exception.OutOfRangeException;
import org.pf4j.util.StringUtils;
import org.spongycastle.crypto.OutputLengthException;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.Hash;
import org.tron.common.logsfilter.trigger.ContractEventTrigger;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;
import org.tron.common.logsfilter.trigger.ContractTrigger;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.LogInfo;
import org.tron.common.utils.StringUtil;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.protos.Protocol.SmartContract.ABI;

import javax.xml.crypto.Data;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j(topic = "Parser")
public class ContractEventParser {
  private static final int DATAWORD_UNIT_SIZE = 32;
  private enum Type{
    UNKNOWN,
    INT_NUMBER,
    BOOL,
    FLOAT_NUMBER,
    FIXED_BYTES,
    ADDRESS,
  }

  /**
   * parse Event Topic into map
   *    NOTICE: In solidity, Indexed Dynamic types's topic is just EVENT_INDEXED_ARGS
   * @param trigger
   * @return
   */
  public static Map<String, Object> parseTopics(ContractEventTrigger trigger, ABI.Entry entry) {
    List<byte[]> topicList = trigger.getTopicList();
    Map<String, Object> map = new HashMap<>();
    if (topicList.size() <= 0){
      return map;
    }

    // topic0,  sha3 of the signature
    map.put("0", Hex.toHexString(topicList.get(0)));

    // the first is the signature.
    int index = 1;
    List<ABI.Entry.Param> list = entry.getInputsList();
    for (int i = 0; i < list.size(); ++i) {
      ABI.Entry.Param param = list.get(i);
      if (!param.getIndexed()){
        continue;
      }
      if (index >= topicList.size()){
        break;
      }
      Object obj = parseTopic(topicList.get(index++), param.getType());
      map.put(param.getName(), obj);
      map.put("" + (i + 1), obj);
    }
    return map;
  }

  /**
   * parse Event Data into map<String, Object>
   *   If parser failed, then return {"0", Hex.toHexString(data)}
   *   Only support basic solidity type, String, Bytes.
   *   Fixed Array or dynamic Array are not support yet (then return {"0": Hex.toHexString(data)}).
   * @param trigger
   * @return
   */
  public static Map<String, Object> parseEventData(ContractEventTrigger trigger, ABI.Entry entry) {
    byte[] data = trigger.getData();
    Map<String, Object> map = new HashMap<>();
    if (ArrayUtils.isEmpty(data)){
      return map;
    }
    // the first is the signature.
    List<ABI.Entry.Param> list = entry.getInputsList();
    try{
      for (int i = 0; i < list.size(); ++i) {
        ABI.Entry.Param param = list.get(i);
        if (param.getIndexed()){
          continue;
        }

        Object obj = parseDataBytes(data, param.getType(), i);
        map.put(param.getName(), obj);
        map.put("" + (i + 1), obj);
      }
    }catch (UnsupportedOperationException e){
      logger.warn("UnsupportedOperationException", e);
      map.clear();
      map.put("0", Hex.toHexString(data));
    }
    return map;
  }

  private static Object parseDataBytes(byte[] data, String typeStr, int index) throws UnsupportedOperationException{

    try{
      byte[] startBytes = subBytes(data, index * DATAWORD_UNIT_SIZE, DATAWORD_UNIT_SIZE);
      Type type = basicType(typeStr);

      if (type == Type.INT_NUMBER){
        return new BigInteger(startBytes).toString();
      }else if (type == Type.BOOL) {
        return !DataWord.isZero(startBytes);
      }else if (type == Type.FIXED_BYTES){
        return Hex.toHexString(startBytes);
      }else if (typeStr.equals("string") || typeStr.equals("bytes")){
        byte[] lengthBytes = subBytes(data, intValueExact(data), DATAWORD_UNIT_SIZE);
        int length = new BigInteger(lengthBytes).intValueExact();
        byte[] realBytes = subBytes(data, length,
            (int) (Math.ceil(length * 1.0 / DATAWORD_UNIT_SIZE)) * DATAWORD_UNIT_SIZE);
        return typeStr.equals("string")? new String(realBytes) : Hex.toHexString(realBytes);
      }
//      else if (Pattern.matches("\\[\\d*\\]$", typeStr)){
//        throw new UnsupportedOperationException("unsupported type:" + typeStr);
//      }
    }catch (OutputLengthException | ArithmeticException e){
      logger.warn("", e);
    }
    throw new UnsupportedOperationException("unsupported type:" + typeStr);
  }

  // don't support this type yet : bytes32[10][10]  OR  bytes32[][10]
  private static Type basicType(String type){
    if (!Pattern.matches("\\[\\d?\\]", type)){
      // ignore not valide type such as "int92", "bytes33", these types will be compiled failed.
      if (type.startsWith("int") || type.startsWith("uint")){
        return Type.INT_NUMBER;
      }else if (type.equals("bool")){
        return Type.BOOL;
      }else if (type.equals("address")){
        return Type.ADDRESS;
      }else if (Pattern.matches("bytes\\d+", type)){
        return Type.FIXED_BYTES;
      }
    }
    return Type.UNKNOWN;
  }

  private static Integer intValueExact(byte[] data) throws ArithmeticException {
    return new BigInteger(data).intValueExact();
  }

  private static byte[] subBytes(byte[] src, int start, int length) throws OutputLengthException {
    if (ArrayUtils.isEmpty(src) || start >= src.length || length < 0){
      throw new OutputLengthException("data start:" + start + ", length:" + length);
    }
    byte[] dst = new byte[length];
    System.arraycopy(src, start, dst, 0, Math.min(length, src.length - start));
    return dst;
  }

  /**
   * support:
   *    uint<m> (m ∈ [8, 256], m % 8 == 0),
   *    int<m> (m ∈ [8, 256], m % 8 == 0)
   *    uint (solidity abi will auto convert to uint256)
   *    int (solidity abi will auto convert to int256)
   *    bool
   *
   *  otherwise, returns hexString
   *
   *  This is only for decode Topic.
   *  Since Topic and Data use different encode methods when deal dynamic length types,
   *  such as bytes and string.
   * @param bytes
   * @param typeStr
   * @return
   */
  private static Object parseTopic(byte[] bytes, String typeStr){
    if (bytes == null || ArrayUtils.isEmpty(bytes) || StringUtils.isNullOrEmpty(typeStr)){
      return "";
    }
    Type type = basicType(typeStr);
    if (type == Type.INT_NUMBER){
      return DataWord.bigIntValue(bytes);
    }else if (type == Type.BOOL){
      return !DataWord.isZero(bytes);
    }
    return DataWord.shortHex(bytes);
  }
}
