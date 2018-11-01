package org.tron.common.utils;

import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.google.protobuf.ByteString;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.db.Manager;

@Slf4j
@Component
public class ForkController {

  private static final byte[] check;
  static {
    check = new byte[1024];
    Arrays.fill(check, (byte) 1);
  }

  @Getter
  private Manager manager;

  private Set<Integer> passSet = new HashSet<>();

  public void init(Manager manager) {
    this.manager = manager;
  }

  public synchronized boolean pass(int version) {
    if (passSet.contains(version)) {
      return true;
    }

    byte[] stats = manager.getDynamicPropertiesStore().statsByVersion(version);
    boolean pass = check(stats);
    if (pass) {
      passSet.add(version);
    }
    return pass;
  }

  private boolean check(byte[] stats) {
    if (stats == null || stats.length == 0) {
      return false;
    }

    for (int i=0; i<stats.length; i++) {
      if (check[i] != stats[i]) {
        return false;
      }
    }

    return true;
  }

  public synchronized void update(BlockCapsule blockCapsule) {
    List<ByteString> witnesses = manager.getWitnessController().getActiveWitnesses();
    ByteString witness = blockCapsule.getWitnessAddress();
    int slot = witnesses.indexOf(witness);
    if (slot < 0) {
      return;
    }

    int version = blockCapsule.getInstance().getBlockHeader().getRawData().getVersion();
    byte[] stats = manager.getDynamicPropertiesStore().statsByVersion(version);
    if (check(stats)) {
      passSet.add(version);
      return;
    }

    if (stats == null) {
      stats = new byte[witnesses.size()];
    }

    stats[slot] = (byte) (version > 0 ? 1 : 0);
    manager.getDynamicPropertiesStore().statsByVersion(version, stats);
    logger.info(
        "*******update hard fork:{}, witness size:{}, solt:{}, witness:{}, version:{}",
        Streams.zip(witnesses.stream(), Stream.of(ArrayUtils.toObject(stats)), Maps::immutableEntry)
            .map(e -> Maps.immutableEntry(ByteUtil.toHexString(e.getKey().toByteArray()), e.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
        witnesses.size(),
        slot,
        ByteUtil.toHexString(witness.toByteArray()),
        version);
  }

  public synchronized void reset(BlockCapsule blockCapsule) {
    int version = blockCapsule.getInstance().getBlockHeader().getRawData().getVersion();
    if (passSet.contains(version)) {
      return;
    }

    byte[] stats = manager.getDynamicPropertiesStore().statsByVersion(version);
    if (check(stats)) {
      passSet.add(version);
      return;
    }

    if (stats != null) {
      Arrays.fill(stats, (byte) 0);
      manager.getDynamicPropertiesStore().statsByVersion(version, stats);
    }
  }
}
