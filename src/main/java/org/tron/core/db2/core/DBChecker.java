package org.tron.core.db2.core;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Sha256Hash;

@Slf4j
public class DBChecker {
  public static void check(List<byte[]> capsules) {
    List<String> hashs = capsules.stream()
      .map(ByteUtil::toHexString)
      .sorted(String::compareTo)
      .collect(Collectors.toList());
    String sha256Hash = Sha256Hash.of(hashs.toString().getBytes()).toString();
    System.out.println("check account hash, block:{}, account hash:{}" + sha256Hash);
  }

  public static void check(String blockId, List<byte[]> capsules) {
    List<String> hashs = capsules.stream()
      .map(ByteUtil::toHexString)
      .sorted(String::compareTo)
      .collect(Collectors.toList());
    String sha256Hash = Sha256Hash.of(hashs.toString().getBytes()).toString();
    System.out.println("block:"+blockId+ ", account hash:" + sha256Hash);
  }

}
