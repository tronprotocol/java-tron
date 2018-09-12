package stest.tron.wallet.common.client.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.Hash;
import org.tron.core.Wallet;

public class AbiUtil {

  static Pattern paramTypeBytes = Pattern.compile("^bytes([0-9]*)$");
  static Pattern paramTypeNumber = Pattern.compile("^(u?int)([0-9]*)$");
  static Pattern paramTypeArray = Pattern.compile("^(.*)\\[([0-9]*)\\]$");
  //

  static abstract class Coder {
    boolean dynamic = false;
    String name;
    String type;

    //    DataWord[] encode
    abstract byte[] encode(String value);

    abstract byte[] decode();

  }

  public static String[] getTypes(String methodSign) {
    int start = methodSign.indexOf('(') + 1;
    int end = methodSign.indexOf(')');

    String typeString = methodSign.subSequence(start,end).toString();

    return typeString.split(",");
  }

  public static String geMethodId(String methodSign) {
    return null;
  }

  public static Coder getParamCoder(String type) {

    switch (type) {
      case "address":
        return new CoderAddress();
      case "string":
        return new CoderString();
      case "bool":
        return new CoderBool();
      case "bytes":
        return new CoderDynamicBytes();
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

      List<Object> strings = null;
      try {
        ObjectMapper mapper = new ObjectMapper();
        strings = mapper.readValue(arrayValues, List.class);
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }

      List<Coder> coders = new ArrayList<>();

      if (this.length == -1) {
        for (int i = 0; i < strings.size(); i++) {
          coders.add(coder);
        }
      } else {
        for (int i = 0; i < this.length; i++) {
          coders.add(coder);
        }
      }

      if (this.length == -1) {
        return concat(new DataWord(strings.size()).getData(), pack(coders, strings));
      } else {
        return pack(coders, strings);
      }
    }

    @Override
    byte[] decode() {
      return new byte[0];
    }
  }

  static class CoderNumber extends  Coder {

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

  static class CoderFixedBytes extends  Coder {

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

  static class CoderDynamicBytes extends  Coder {

    CoderDynamicBytes() {
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

  static class CoderBool extends  Coder {

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
      byte[] address = Wallet.decodeFromBase58Check(value);
      return new DataWord(address).getData();
    }

    @Override
    byte[] decode() {
      return new byte[0];
    }
  }

  static class CoderString extends  Coder {
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

  public static byte[] encodeDynamicBytes(String value) {
    byte[] data = value.getBytes();
    List<DataWord> ret = new ArrayList<>();
    ret.add(new DataWord(data.length));

    int readInx = 0;
    int len = value.getBytes().length;
    while (readInx < value.getBytes().length) {
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

  public static byte[] pack(List<Coder> codes, List<Object> values) {

    int staticSize = 0;
    int dynamicSize = 0;

    List<byte[]> encodedList = new ArrayList<>();

    for (int idx = 0;idx < codes.size();  idx++) {
      Coder coder = codes.get(idx);
      String value = values.get(idx).toString();

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
        System.arraycopy(new DataWord(dynamicOffset).getData(), 0,data, offset, 32);
        offset += 32;

        System.arraycopy(encodedList.get(idx), 0,data, dynamicOffset, encodedList.get(idx).length);
        dynamicOffset += encodedList.get(idx).length;
      } else {
        System.arraycopy(encodedList.get(idx), 0,data, offset, encodedList.get(idx).length);
        offset += encodedList.get(idx).length;
      }
    }

    return data;
  }

  public static String parseMethod(String methodSign, String params) {
    return parseMethod(methodSign, params, false);
  }

  public static String parseMethod(String methodSign, String input, boolean isHex) {
    byte[] selector = new byte[4];
    System.arraycopy(Hash.sha3(methodSign.getBytes()), 0, selector,0, 4);
    System.out.println(methodSign + ":" + Hex.toHexString(selector));
    if (input.length() == 0) {
      return Hex.toHexString(selector);
    }
    if (isHex) {
      return Hex.toHexString(selector) + input;
    }
    byte[] encodedParms = encodeInput(methodSign, input);

    return Hex.toHexString(selector) + Hex.toHexString(encodedParms);
  }

  public static byte[] encodeInput(String methodSign, String input) {
    ObjectMapper mapper = new ObjectMapper();
    input = "[" + input + "]";
    List<Object> items = null;
    try {
      items = mapper.readValue(input, List.class);
    } catch (IOException e) {
      e.printStackTrace();
    }

    List<Coder> coders = new ArrayList<>();
    for (String s: getTypes(methodSign)) {
      Coder c = getParamCoder(s);
      coders.add(c);
    }

    return pack(coders, items);
  }

  public  static void main(String[] args) {
    //    String method = "test(address,string,int)";
    String method = "test(string,int2,string)";
    String params = "asdf,3123,adf";

    String arrayMethod1 = "test(uint,uint256[3])";
    String arrayMethod2 = "test(uint,uint256[])";
    String arrayMethod3 = "test(uint,address[])";
    String byteMethod1 = "test(bytes32,bytes11)";

    String method1 = "test(uint256,string,string,uint256[])";
    String expected1  = "db103cf30000000000000000000000000000000000000000000000000000000000000005000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000c0000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000014200000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000143000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000003000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000003";
    String method2 = "test(uint256,string,string,uint256[3])";
    String expected2 = "000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000003";
    String listString = "1 ,\"B\",\"C\", [1, 2, 3]";
    System.out.println(parseMethod(method1, listString));
    System.out.println(parseMethod(method2, listString));

    String bytesValue1 = "\"0112313\",112313";
    String bytesValue2 = "123123123";

    System.out.println(parseMethod(byteMethod1, bytesValue1));

  }

  public static byte[] concat(byte[]... bytesArray) {
    int length = 0;
    for (byte[] bytes: bytesArray) {
      length += bytes.length;
    }
    byte[] ret = new byte[length];
    int index = 0;
    for (byte[] bytes: bytesArray) {
      System.arraycopy(bytes, 0, ret, index, bytes.length);
      index += bytes.length;
    }
    return ret;
  }



}
