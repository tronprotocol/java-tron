package org.tron.common.logsfilter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.pf4j.util.StringUtils;
import org.spongycastle.crypto.OutputLengthException;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.logsfilter.trigger.ContractEventTrigger;
import org.tron.common.runtime.vm.DataWord;
import org.tron.protos.Protocol.SmartContract.ABI;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  public static boolean topicsMatched(ContractEventTrigger trigger, ABI.Entry entry){
    List<byte[]> topicList = trigger.getTopicList();
    if (topicList == null || topicList.isEmpty()){
      return true;
    }
    int indexSize = 1; // the first is signature
    for (ABI.Entry.Param param : entry.getInputsList()){
      if (param.getIndexed()) {
        indexSize++;
      }
    }
    return indexSize == topicList.size();
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
    if (topicList == null || topicList.isEmpty()){
      return map;
    }

    // the first is the signature.
    int index = 1;
    List<ABI.Entry.Param> list = entry.getInputsList();

    // in case indexed topics doesn't match
    if (topicsMatched(trigger, entry)){
      for (Integer i = 0; i < list.size(); ++i) {
        ABI.Entry.Param param = list.get(i);
        if (!param.getIndexed()) {
          continue;
        }

        if (index >= topicList.size()) {
          break;
        }
        Object obj = parseTopic(topicList.get(index++), param.getType());
        map.put(param.getName(), obj);
        map.put(i.toString(), obj);
      }
    }else{
      for (Integer i = 1; i < topicList.size(); ++i) {
        map.put("" + (i - 1), DataWord.shortHex(topicList.get(i)));
      }
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
    // in case indexed topics doesn't match
    if (!topicsMatched(trigger, entry)){
      map.put("" + (trigger.getTopicList().size() - 1), Hex.toHexString(data));
      return map;
    }

    // the first is the signature.
    List<ABI.Entry.Param> list = entry.getInputsList();
    Integer startIndex = 0;
    try{
      // this one starts from the first position.
      int index = 0;
      for (Integer i = 0; i < list.size(); ++i) {
        ABI.Entry.Param param = list.get(i);
        if (param.getIndexed()){
          continue;
        }
        if (startIndex == 0){
          startIndex = i;
        }

        Object obj = parseDataBytes(data, param.getType(), index++);
        map.put(param.getName(), obj);
        // position 0 is the signature.
        map.put(i.toString(), obj);
      }
    }catch (UnsupportedOperationException e){
      logger.warn("UnsupportedOperationException", e);
      map.clear();
      map.put(startIndex.toString(), Hex.toHexString(data));
    }
    return map;
  }

  private static Object parseDataBytes(byte[] data, String typeStr, int index) {

    try{
      byte[] startBytes = subBytes(data, index * DATAWORD_UNIT_SIZE, DATAWORD_UNIT_SIZE);
      Type type = basicType(typeStr);

      if (type == Type.INT_NUMBER){
        return new BigInteger(startBytes).toString();
      }else if (type == Type.BOOL) {
        return !DataWord.isZero(startBytes);
      }else if (type == Type.FIXED_BYTES){
        return Hex.toHexString(startBytes);
      }else if (typeStr.equals("string") || typeStr.equals("bytes") || type == Type.ADDRESS){
        int start = intValueExact(startBytes);
        byte[] lengthBytes = subBytes(data, start, DATAWORD_UNIT_SIZE);
        // this length is byte count. no need X 32
        int length = intValueExact(lengthBytes);
        byte[] realBytes = subBytes(data, start + DATAWORD_UNIT_SIZE, length);
        return typeStr.equals("string")? new String(realBytes) : DataWord.shortHex(realBytes);//Hex.toHexString(realBytes);
      }
    }catch (OutputLengthException | ArithmeticException e){
      logger.warn("", e);
    }
    throw new UnsupportedOperationException("unsupported type:" + typeStr);
  }

  // don't support this type yet : bytes32[10][10]  OR  bytes32[][10]
  private static Type basicType(String type){
    if (!Pattern.matches("^.*\\[\\d*\\]$", type)){
      // ignore not valide type such as "int92", "bytes33", these types will be compiled failed.
      if ((type.startsWith("int") || type.startsWith("uint"))){
        return Type.INT_NUMBER;
      }else if (type.equals("bool")){
        return Type.BOOL;
      }else if (type.equals("address")){
        return Type.ADDRESS;
      }else if (Pattern.matches("^bytes\\d+$", type)){
        return Type.FIXED_BYTES;
      }
    }
    return Type.UNKNOWN;
  }

  private static Integer intValueExact(byte[] data) {
    return new BigInteger(data).intValueExact();
  }

  private static byte[] subBytes(byte[] src, int start, int length) {
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
    if (ArrayUtils.isEmpty(bytes) || StringUtils.isNullOrEmpty(typeStr)){
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
