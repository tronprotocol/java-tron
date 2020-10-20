package org.tron.core.store;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.db.TronStoreWithRevoking;

@Slf4j
@Component
public class DelegationStore extends TronStoreWithRevoking<BytesCapsule> {

  public static final long REMARK = -1L;
  public static final int DEFAULT_BROKERAGE = 20;

  @Autowired
  public DelegationStore(@Value("delegation") String dbName) {
    super(dbName);
  }

  @Override
  public BytesCapsule get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new BytesCapsule(value);
  }

  public void addReward(long cycle, byte[] address, long value) {
    byte[] key = buildRewardKey(cycle, address);
    BytesCapsule bytesCapsule = get(key);
    if (bytesCapsule == null) {
      put(key, new BytesCapsule(ByteArray.fromLong(value)));
    } else {
      put(key, new BytesCapsule(ByteArray
          .fromLong(ByteArray.toLong(bytesCapsule.getData()) + value)));
    }
  }

  public long getReward(long cycle, byte[] address) {
    BytesCapsule bytesCapsule = get(buildRewardKey(cycle, address));
    if (bytesCapsule == null) {
      return 0L;
    } else {
      return ByteArray.toLong(bytesCapsule.getData());
    }
  }

  public void setBeginCycle(byte[] address, long number) {
    put(address, new BytesCapsule(ByteArray.fromLong(number)));
  }

  public long getBeginCycle(byte[] address) {
    BytesCapsule bytesCapsule = get(address);
    return bytesCapsule == null ? 0 : ByteArray.toLong(bytesCapsule.getData());
  }

  public void setEndCycle(byte[] address, long number) {
    put(buildEndCycleKey(address), new BytesCapsule(ByteArray.fromLong(number)));
  }

  public long getEndCycle(byte[] address) {
    BytesCapsule bytesCapsule = get(buildEndCycleKey(address));
    return bytesCapsule == null ? REMARK : ByteArray.toLong(bytesCapsule.getData());
  }

  public void setWitnessVote(long cycle, byte[] address, long value) {
    put(buildVoteKey(cycle, address), new BytesCapsule(ByteArray.fromLong(value)));
  }

  public long getWitnessVote(long cycle, byte[] address) {
    BytesCapsule bytesCapsule = get(buildVoteKey(cycle, address));
    if (bytesCapsule == null) {
      return REMARK;
    } else {
      return ByteArray.toLong(bytesCapsule.getData());
    }
  }

  public void setAccountVote(long cycle, byte[] address, AccountCapsule accountCapsule) {
    put(buildAccountVoteKey(cycle, address), new BytesCapsule(accountCapsule.getData()));
  }

  public AccountCapsule getAccountVote(long cycle, byte[] address) {
    BytesCapsule bytesCapsule = get(buildAccountVoteKey(cycle, address));
    if (bytesCapsule == null) {
      return null;
    } else {
      return new AccountCapsule(bytesCapsule.getData());
    }
  }

  public void setBrokerage(long cycle, byte[] address, int brokerage) {
    put(buildBrokerageKey(cycle, address), new BytesCapsule(ByteArray.fromInt(brokerage)));
  }

  public int getBrokerage(long cycle, byte[] address) {
    BytesCapsule bytesCapsule = get(buildBrokerageKey(cycle, address));
    if (bytesCapsule == null) {
      return DEFAULT_BROKERAGE;
    } else {
      return ByteArray.toInt(bytesCapsule.getData());
    }
  }

  public void setBrokerage(byte[] address, int brokerage) {
    setBrokerage(-1, address, brokerage);
  }

  public int getBrokerage(byte[] address) {
    return getBrokerage(-1, address);
  }

  private byte[] buildVoteKey(long cycle, byte[] address) {
    return (cycle + "-" + Hex.toHexString(address) + "-vote").getBytes();
  }

  private byte[] buildRewardKey(long cycle, byte[] address) {
    return (cycle + "-" + Hex.toHexString(address) + "-reward").getBytes();
  }

  private byte[] buildAccountVoteKey(long cycle, byte[] address) {
    return (cycle + "-" + Hex.toHexString(address) + "-account-vote").getBytes();
  }

  private byte[] buildEndCycleKey(byte[] address) {
    return ("end-" + Hex.toHexString(address)).getBytes();
  }

  private byte[] buildBrokerageKey(long cycle, byte[] address) {
    return (cycle + "-" + Hex.toHexString(address) + "-brokerage").getBytes();
  }

}
