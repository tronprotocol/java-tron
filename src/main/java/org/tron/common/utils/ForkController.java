package org.tron.common.utils;

import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.google.protobuf.ByteString;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.Parameter.ForkBlockVersionConsts;
import org.tron.core.db.Manager;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ForkController {

  private static final byte VERSION_UPGRADE = (byte) 1;
  private static final byte HARD_FORK_EFFECTIVE = (byte) 2;
  private static final byte[] check;
  private static final byte[] check2;
  static {
    check = new byte[1024];
    Arrays.fill(check, VERSION_UPGRADE);
    check2 = new byte[1024];
    Arrays.fill(check2, HARD_FORK_EFFECTIVE);
  }

  @Getter
  private Manager manager;

  private Set<Integer> passSet = new HashSet<>();

  public void init(Manager manager) {
    this.manager = manager;
    passSet.clear();
  }

  public synchronized boolean pass(int version) {
    if (!checkEnergy(version)) {
      return false;
    }

    if (passSet.contains(version)) {
      return true;
    }

    byte[] stats = manager.getDynamicPropertiesStore().statsByVersion(version);
    boolean pass;
    if (version == ForkBlockVersionConsts.ENERGY_LIMIT) {
      pass = check(stats);
    } else {
      pass = check2(stats);
    }

    if (pass) {
      passSet.add(version);
    }
    return pass;
  }

  // when block.version = 5,
  // it make block use new energy to handle transaction when block number >= 4727890L.
  // version !=5, skip this.
  private boolean checkEnergy(int version) {
    if (version != ForkBlockVersionConsts.ENERGY_LIMIT) {
      return true;
    }

    long blockNum = manager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
    return blockNum >= 4727890L;
  }

  private boolean check(byte[] stats) {
    return check(check, stats);
  }

  private boolean check2(byte[] stats) {
    return check(check2, stats);
  }

  private boolean check(byte[] check, byte[] stats) {
    if (stats == null || stats.length == 0) {
      return false;
    }

    for (int i = 0; i < stats.length; i++) {
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
    if (version < ForkBlockVersionConsts.ENERGY_LIMIT || passSet.contains(version)) {
      return;
    }

    byte[] stats = manager.getDynamicPropertiesStore().statsByVersion(version);
    if (check(stats) || check2(stats)) {
      return;
    }

    if (stats == null) {
      stats = new byte[witnesses.size()];
    }

    stats[slot] = VERSION_UPGRADE;
    manager.getDynamicPropertiesStore().statsByVersion(version, stats);
    logger.info(
        "*******update hard fork:{}, witness size:{}, solt:{}, witness:{}, version:{}",
        Streams.zip(witnesses.stream(), Stream.of(ArrayUtils.toObject(stats)), Maps::immutableEntry)
            .map(e -> Maps.immutableEntry(Wallet.encode58Check(e.getKey().toByteArray()), e.getValue()))
            .map(e -> Maps.immutableEntry(StringUtils.substring(e.getKey(), e.getKey().length() - 4), e.getValue()))
            .collect(Collectors.toList()),
        witnesses.size(),
        slot,
        Wallet.encode58Check(witness.toByteArray()),
        version);
  }

  public synchronized void updateWhenMaintenance(BlockCapsule blockCapsule) {
    int version = blockCapsule.getInstance().getBlockHeader().getRawData().getVersion();
    if (version < ForkBlockVersionConsts.VERSION_3_2_2 || passSet.contains(version)) {
      return;
    }

    byte[] stats = manager.getDynamicPropertiesStore().statsByVersion(version);
    if (check2(stats)) {
      return;
    }

    if (check(stats)) {
      Arrays.fill(stats, HARD_FORK_EFFECTIVE);
      manager.getDynamicPropertiesStore().statsByVersion(version, stats);
      logger.info("*******hard fork is effective in the maintenance, version is {}", version);
    }
  }

  public synchronized void reset(BlockCapsule blockCapsule) {
    int version = blockCapsule.getInstance().getBlockHeader().getRawData().getVersion();
    if (version < ForkBlockVersionConsts.ENERGY_LIMIT || passSet.contains(version)) {
      return;
    }

    byte[] stats = manager.getDynamicPropertiesStore().statsByVersion(version);
    if (check(stats) || check2(stats)) {
      return;
    }

    if (stats != null) {
      Arrays.fill(stats, (byte) 0);
      manager.getDynamicPropertiesStore().statsByVersion(version, stats);
    }
  }

  public static ForkController instance() {
    return ForkControllerEnum.INSTANCE.getInstance();
  }

  private enum ForkControllerEnum {
    INSTANCE;

    private ForkController instance;

    ForkControllerEnum() {
      instance = new ForkController();
    }

    private ForkController getInstance() {
      return instance;
    }
  }
}
