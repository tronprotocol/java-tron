package org.tron.core.db2.common;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.AccountCapsule;

@Slf4j
public class DBChecker {
  public static void check(String blockId, List<byte[]> capsules) {
    List<String> hashs = capsules.stream()
        .map(ByteUtil::toHexString)
        .sorted(String::compareTo)
        .collect(Collectors.toList());
    String sha256Hash = Sha256Hash.of(hashs.toString().getBytes()).toString();
    logger.info("check account hash, block:{}, account hash:{}", blockId, sha256Hash);
  }
}
