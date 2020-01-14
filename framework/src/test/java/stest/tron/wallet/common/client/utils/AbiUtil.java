package stest.tron.wallet.common.client.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Commons;

public class AbiUtil {

  private static Pattern paramTypeBytes = Pattern.compile("^bytes([0-9]*)$");
  private static Pattern paramTypeNumber = Pattern.compile("^(u?int)([0-9]*)$");
  private static Pattern paramTypeArray = Pattern.compile("^(.*)\\[([0-9]*)]$");

  public static String[] getTypes(String methodSign) {
    int start = methodSign.indexOf('(') + 1;
    int end = methodSign.indexOf(')');
    if (start == end) {
      return new String[0];
    }
    String typeString = methodSign.subSequence(start, end).toString();
    return typeString.split(",");
  }

  private static Coder getParamCoder(String type) {

    switch (type) {
      case "address":
        return new CoderAddress();
      case "string":
        return new CoderString();
      case "bool":
        return new CoderBool();
      case "bytes":
        return new CoderDynamicBytes();
      case "trcToken":
        return new CoderNumber();
      default:
        System.out.println(type);
    }

    if (paramTypeBytes.matcher(type).find()) {
      return new CoderFixedBytes();
    }

    if (paramTypeNumber.matcher(type).find()) {
      return new CoderNumber();
    }

    Matcher m = paramTypeArray.matcher(type);
    if (m.find()) {
      String arrayType = m.group(1);
      int length = -1;
      if (!m.group(2).equals("")) {
        length = Integer.valueOf(m.group(2));
      }
      return new CoderArray(arrayType, length);
    }
    return null;
  }

  private static byte[] encodeDynamicBytes(String value, boolean hex) {
    byte[] data;
    if (hex) {
      if (value.startsWith("0x")) {
        value = value.substring(2);
      }
      data = Hex.decode(value);
    } else {
      data = value.getBytes();
    }
    return encodeDynamicBytes(data);
  }

  private static byte[] encodeDynamicBytes(byte[] data) {
    List<DataWord> ret = new ArrayList<>();
    ret.add(new DataWord(data.length));

    int readInx = 0;
    int len = data.length;
    while (readInx < data.length) {
      byte[] wordData = new byte[32];
      int readLen = len - readInx >= 32 ? 32 : (len - readInx);
      System.arraycopy(data, readInx, wordData, 0, readLen);
      DataWord word = new DataWord(wordData);
      ret.add(word);
      readInx += 32;
    }

    byte[] retBytes = new byte[ret.size() * 32];
    int retIndex = 0;

    for (DataWord w : ret) {
      System.arraycopy(w.getData(), 0, retBytes, retIndex, 32);
      retIndex += 32;
    }

    return retBytes;
  }

  private static byte[] encodeDynamicBytes(String value) {
    byte[] data = value.getBytes();
    List<DataWord> ret = new ArrayList<>();
    ret.add(new DataWord(data.length));
    return encodeDynamicBytes(data);
  }

  public static byte[] pack(List<Coder> codes, List<Object> values) {

    int staticSize = 0;
    int dynamicSize = 0;

    List<byte[]> encodedList = new ArrayList<>();

    for (int idx = 0; idx < codes.size(); idx++) {
      Coder coder = codes.get(idx);
      Object parameter = values.get(idx);
      String value;
      if (parameter instanceof List) {
        StringBuilder sb = new StringBuilder();
        for (Object item : (List) parameter) {
          if (sb.length() != 0) {
            sb.append(",");
          }
          sb.append("\"").append(item).append("\"");
        }
        value = "[" + sb.toString() + "]";
      } else {
        value = parameter.toString();
      }
      byte[] encoded = coder.encode(value);
      encodedList.add(encoded);

      if (coder.dynamic) {
        staticSize += 32;
        dynamicSize += encoded.length;
      } else {
        staticSize += encoded.length;
      }
    }

    int offset = 0;
    int dynamicOffset = staticSize;

    byte[] data = new byte[staticSize + dynamicSize];

    for (int idx = 0; idx < codes.size(); idx++) {
      Coder coder = codes.get(idx);

      if (coder.dynamic) {
        System.arraycopy(new DataWord(dynamicOffset).getData(), 0, data, offset, 32);
        offset += 32;

        System.arraycopy(encodedList.get(idx), 0, data, dynamicOffset,
            encodedList.get(idx).length);
        dynamicOffset += encodedList.get(idx).length;
      } else {
        System.arraycopy(encodedList.get(idx), 0, data, offset, encodedList.get(idx).length);
        offset += encodedList.get(idx).length;
      }
    }

    return data;
  }

  public static String parseMethod(String methodSign, String params) {
    return parseMethod(methodSign, params, false);
  }

  public static String parseMethod(String methodSign, String input, boolean isHex) {
    if (isHex) {
      return parseSelector(methodSign) + input;
    } else {
      return parseSelector(methodSign) + parseParameters(methodSign, input);
    }
  }

  public static String parseMethod(String methodSign, List<Object> parameters) {
    return parseSelector(methodSign) + parseParameters(methodSign, parameters);
  }

  public static String parseParameters(String methodSign, List<Object> parameters) {
    String[] inputArr = new String[parameters.size()];
    int i = 0;
    for (Object parameter : parameters) {
      if (parameter instanceof List) {
        StringBuilder sb = new StringBuilder();
        for (Object item : (List) parameter) {
          if (sb.length() != 0) {
            sb.append(",");
          }
          sb.append("\"").append(item).append("\"");
        }
        inputArr[i++] = "[" + sb.toString() + "]";
      } else {
        inputArr[i++] =
            (parameter instanceof String) ? ("\"" + parameter + "\"") : ("" + parameter);
      }
    }
    String input = StringUtils.join(inputArr, ',');
    return parseParameters(methodSign, input);
  }

  public static String parseParameters(String methodSign, String input) {
    byte[] encodedParms = encodeInput(methodSign, input);
    return Hex.toHexString(encodedParms);
  }

  public static String parseSelector(String methodSign) {
    byte[] selector = new byte[4];
    System.arraycopy(Hash.sha3(methodSign.getBytes()), 0, selector, 0, 4);
    return Hex.toHexString(selector);
  }

  public static byte[] encodeInput(String methodSign, String input) {
    ObjectMapper mapper = new ObjectMapper();
    input = "[" + input + "]";
    List items;
    try {
      items = mapper.readValue(input, List.class);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }

    List<Coder> coders = new ArrayList<>();
    for (String s : getTypes(methodSign)) {
      Coder c = getParamCoder(s);
      coders.add(c);
    }

    return pack(coders, items);
  }

  public static void main(String[] args) {
    String method = "test(string,int2,string)";
    String params = "asdf,3123,adf";

    String arrayMethod1 = "test(uint,uint256[3])";
    String arrayMethod2 = "test(uint,uint256[])";
    String arrayMethod3 = "test(uint,address[])";
    String byteMethod1 = "test(bytes32,bytes11)";
    String tokenMethod = "test(trcToken,uint256)";
    String tokenParams = "\"nmb\",111";

    System.out.println("token:" + parseMethod(tokenMethod, tokenParams));

    String method1 = "test(uint256,string,string,uint256[])";
    String expected1 = "db103cf30000000000000000000000000000000000000000000000000000000000000005000"
        + "0000000000000000000000000000000000000000000000000000000000080000000000000000000000000000"
        + "00000000000000000000000000000000000c0000000000000000000000000000000000000000000000000000"
        + "0000000000100000000000000000000000000000000000000000000000000000000000000000142000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000143000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000000030000000000000000000"
        + "0000000000000000000000000000000000000000000010000000000000000000000000000000000000000000"
        + "0000000000000000000020000000000000000000000000000000000000000000000000000000000000003";
    String method2 = "test(uint256,string,string,uint256[3])";
    String listString = "1 ,\"B\",\"C\", [1, 2, 3]";
    System.out.println(parseMethod(method1, listString));
    System.out.println(parseMethod(method2, listString));

    String bytesValue1 = "\"0112313\",112313";

    System.out.println(parseMethod(byteMethod1, bytesValue1));
  }

  abstract static class Coder {

    boolean dynamic = false;

    // DataWord[] encode
    abstract byte[] encode(String value);

    abstract byte[] decode();

  }

  static class CoderArray extends Coder {

    private String elementType;
    private int length;

    CoderArray(String arrayType, int length) {
      this.elementType = arrayType;
      this.length = length;
      if (length == -1) {
        this.dynamic = true;
      }
      this.dynamic = true;
    }

    @Override
    byte[] encode(String arrayValues) {

      Coder coder = getParamCoder(elementType);

      List items;
      try {
        ObjectMapper mapper = new ObjectMapper();
        items = mapper.readValue(arrayValues, List.class);
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }

      List<Coder> coders = new ArrayList<>();

      if (this.length == -1) {
        for (int i = 0; i < items.size(); i++) {
          coders.add(coder);
        }
      } else {
        for (int i = 0; i < this.length; i++) {
          coders.add(coder);
        }
      }

      if (this.length == -1) {
        return ByteUtil.merge(new DataWord(items.size()).getData(), pack(coders, items));
      } else {
        return pack(coders, items);
      }
    }

    @Override
    byte[] decode() {
      return new byte[0];
    }
  }

  static class CoderNumber extends Coder {

    @Override
    byte[] encode(String value) {
      long n = Long.valueOf(value);
      DataWord word = new DataWord(Math.abs(n));
      if (n < 0) {
        word.negate();
      }
      return word.getData();
    }

    @Override
    byte[] decode() {
      return new byte[0];
    }
  }

  static class CoderFixedBytes extends Coder {

    @Override
    byte[] encode(String value) {

      if (value.startsWith("0x")) {
        value = value.substring(2);
      }

      if (value.length() % 2 != 0) {
        value = "0" + value;
      }

      byte[] result = new byte[32];
      byte[] bytes = Hex.decode(value);
      System.arraycopy(bytes, 0, result, 0, bytes.length);
      return result;
    }

    @Override
    byte[] decode() {
      return new byte[0];
    }
  }

  static class CoderDynamicBytes extends Coder {

    CoderDynamicBytes() {
      dynamic = true;
    }

    @Override
    byte[] encode(String value) {
      return encodeDynamicBytes(value, true);
    }

    @Override
    byte[] decode() {
      return new byte[0];
    }
  }

  static class CoderBool extends Coder {

    @Override
    byte[] encode(String value) {
      if (value.equals("true") || value.equals("1")) {
        return new DataWord(1).getData();
      } else {
        return new DataWord(0).getData();
      }

    }

    @Override
    byte[] decode() {
      return new byte[0];
    }
  }

  static class CoderAddress extends Coder {

    @Override
    byte[] encode(String value) {
      byte[] address = Commons.decodeFromBase58Check(value);
      if (address == null) {
        return null;
      }
      return new DataWord(address).getData();
    }

    @Override
    byte[] decode() {
      return new byte[0];
    }
  }

  static class CoderString extends Coder {

    CoderString() {
      dynamic = true;
    }

    @Override
    byte[] encode(String value) {
      return encodeDynamicBytes(value);
    }

    @Override
    byte[] decode() {
      return new byte[0];
    }
  }
}
