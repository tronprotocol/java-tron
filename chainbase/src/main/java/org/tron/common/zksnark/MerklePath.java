package org.tron.common.zksnark;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.Getter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.core.exception.ZksnarkException;

public class MerklePath {

  @Getter
  private List<List<Boolean>> authenticationPath;

  @Getter
  private List<Boolean> index;

  public MerklePath(List<List<Boolean>> authenticationPath, List<Boolean> index) {
    this.authenticationPath = authenticationPath;
    this.index = index;
  }

  private static byte[] listList2Bytes(List<List<Byte>> v) {
    List<byte[]> resultList = Lists.newArrayList();
    resultList.add(writeCompactSize(v.size()));
    for (List<Byte> list : v) {
      resultList.add(writeCompactSize(list.size()));
      for (Byte b : list) {
        byte[] bytes = {b};
        resultList.add(bytes);
      }
    }
    int sum = resultList.stream().mapToInt(bytes -> bytes.length).sum();
    byte[] resultBytes = new byte[sum];
    int index = 0;
    for (byte[] bytes : resultList) {
      System.arraycopy(bytes, 0, resultBytes, index, bytes.length);
      index += bytes.length;
    }

    return resultBytes;
  }

  private static byte[] writeCompactSize(long nSize) {
    byte[] result;
    if (nSize < 253) {
      result = new byte[1];
      result[0] = (byte) nSize;
    } else if (nSize <= 0xFFFF) {
      result = new byte[3];
      result[0] = (byte) 253;
      result[1] = (byte) nSize;
      result[2] = (byte) (nSize >> 8);
    } else if (nSize <= 0xFFFFFFFF) {
      result = new byte[4];
      System.arraycopy(ByteArray.fromInt((int) nSize), 0, result, 1, 8);
    } else {
      result = new byte[5];
      System.arraycopy(ByteArray.fromLong(nSize), 0, result, 1, 8);
    }
    return result;
  }

  private static long convertVectorToLong(List<Boolean> v) throws ZksnarkException {
    if (v.size() > 64) {
      throw new ZksnarkException("boolean vector can't be larger than 64 bits");
    }
    long result = 0;
    for (int i = 0; i < v.size(); i++) {
      if (v.get(i)) {
        result |= (long) 1 << ((v.size() - 1) - i);
      }
    }
    return result;
  }

  public byte[] encode() throws ZksnarkException {
    assert (authenticationPath.size() == index.size());
    List<List<Byte>> pathByteList = Lists.newArrayList();
    long indexLong; // 64
    for (int i = 0; i < authenticationPath.size(); i++) {
      pathByteList.add(Lists.newArrayList());
      for (int p = 0; p < authenticationPath.get(i).size(); p++) {
        byte bByte = (byte) (authenticationPath.get(i).get(p) ? 1
            : 0);
        int i1;
        if (pathByteList.get(i).size() > (p / 8)) {
          i1 = pathByteList.get(i).get(p / 8) | bByte << (7 - (p % 8));
          pathByteList.get(i).set(p / 8, (byte) i1);
        } else {
          i1 = bByte << (7 - (p % 8));
          pathByteList.get(i).add((byte) i1);
        }

      }
    }
    indexLong = convertVectorToLong(index);
    byte[] indexBytes = ByteArray.fromLong(indexLong);
    ByteUtil.reverse(indexBytes);
    byte[] pathByteArray = listList2Bytes(pathByteList);
    byte[] result = new byte[pathByteArray.length + 8];
    System.arraycopy(pathByteArray, 0, result, 0, pathByteArray.length);
    System.arraycopy(indexBytes, 0, result,
        pathByteArray.length, 8);
    return result;
  }
}
