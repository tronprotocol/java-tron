package org.tron.common.utils;

import java.util.Arrays;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.config.args.Parameter.ForkBlockVersionConsts;
import org.tron.core.config.args.Parameter.ForkBlockVersionEnum;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.WitnessStore;

@Slf4j(topic = "utils")
public class ForkUtils {

  protected static final byte VERSION_DOWNGRADE = (byte) 0;
  protected static final byte VERSION_UPGRADE = (byte) 1;
  protected static final byte[] check;

  static {
    check = new byte[1024];
    Arrays.fill(check, VERSION_UPGRADE);
  }

  @Setter
  @Getter
  protected DynamicPropertiesStore dynamicPropertiesStore;

  protected WitnessStore witnessStore;

  public void init(DynamicPropertiesStore dynamicPropertiesStore) {
    this.dynamicPropertiesStore = dynamicPropertiesStore;
  }

  public boolean pass(ForkBlockVersionEnum forkBlockVersionEnum) {
    return pass(forkBlockVersionEnum.getValue());
  }

  public synchronized boolean pass(int version) {
    if (version > ForkBlockVersionEnum.VERSION_4_0.getValue()) {
      return passNew(version);
    } else {
      return passOld(version);
    }
  }

  private boolean passOld(int version) {
    if (version == ForkBlockVersionConsts.ENERGY_LIMIT) {
      return checkForEnergyLimit();
    }

    byte[] stats = dynamicPropertiesStore.statsByVersion(version);
    return check(stats);
  }

  private boolean passNew(int version) {
    ForkBlockVersionEnum versionEnum = ForkBlockVersionEnum.getForkBlockVersionEnum(version);
    if (versionEnum == null) {
      logger.error("not exist block version: {}", version);
      return false;
    }
    long latestBlockTime = dynamicPropertiesStore.getLatestBlockHeaderTimestamp();
    long maintenanceTimeInterval = dynamicPropertiesStore.getMaintenanceTimeInterval();
    long hardForkTime = ((versionEnum.getHardForkTime() - 1) / maintenanceTimeInterval + 1)
        * maintenanceTimeInterval;
    if (latestBlockTime < hardForkTime) {
      return false;
    }
    byte[] stats = dynamicPropertiesStore.statsByVersion(version);
    if (stats == null || stats.length == 0) {
      return false;
    }
    int count = 0;
    for (int i = 0; i < stats.length; i++) {
      if (check[i] == stats[i]) {
        ++count;
      }
    }
    return count >= versionEnum.getHardForkCount();
  }

  // when block.version = 5,
  // it make block use new energy to handle transaction when block number >= 4727890L.
  // version !=5, skip this.
  private boolean checkForEnergyLimit() {
    long blockNum = dynamicPropertiesStore.getLatestBlockHeaderNumber();
    return blockNum >= DBConfig.getBlockNumForEneryLimit();
  }

  protected boolean check(byte[] stats) {
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

  protected void downgrade(int version, int slot) {
    for (ForkBlockVersionEnum versionEnum : ForkBlockVersionEnum.values()) {
      int versionValue = versionEnum.getValue();
      if (versionValue > version) {
        byte[] stats = dynamicPropertiesStore.statsByVersion(versionValue);
        if (!check(stats) && Objects.nonNull(stats)) {
          stats[slot] = VERSION_DOWNGRADE;
          dynamicPropertiesStore.statsByVersion(versionValue, stats);
        }
      }
    }
  }

  protected void upgrade(int version, int slotSize) {
    for (ForkBlockVersionEnum versionEnum : ForkBlockVersionEnum.values()) {
      int versionValue = versionEnum.getValue();
      if (versionValue < version) {
        byte[] stats = dynamicPropertiesStore.statsByVersion(versionValue);
        if (!check(stats)) {
          if (stats == null || stats.length == 0) {
            stats = new byte[slotSize];
          }
          Arrays.fill(stats, VERSION_UPGRADE);
          dynamicPropertiesStore.statsByVersion(versionValue, stats);
        }
      }
    }
  }


  public synchronized void reset() {
    for (ForkBlockVersionEnum versionEnum : ForkBlockVersionEnum.values()) {
      int versionValue = versionEnum.getValue();
      byte[] stats = dynamicPropertiesStore.statsByVersion(versionValue);
      if (!check(stats) && Objects.nonNull(stats)) {
        Arrays.fill(stats, VERSION_DOWNGRADE);
        dynamicPropertiesStore.statsByVersion(versionValue, stats);
      }
    }
  }
}
