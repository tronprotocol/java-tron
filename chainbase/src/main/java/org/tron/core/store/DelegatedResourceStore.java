package org.tron.core.store;

import com.google.protobuf.ByteString;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.DelegatedResourceCapsule;
import org.tron.core.db.TronStoreWithRevoking;

@Component
public class DelegatedResourceStore extends TronStoreWithRevoking<DelegatedResourceCapsule> {

  @Autowired
  public DelegatedResourceStore(@Value("DelegatedResource") String dbName) {
    super(dbName);
  }

  @Override
  public DelegatedResourceCapsule get(byte[] key) {

    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new DelegatedResourceCapsule(value);
  }

  @Deprecated
  public List<DelegatedResourceCapsule> getByFrom(byte[] key) {
    return revokingDB.getValuesNext(key, Long.MAX_VALUE).stream()
        .map(DelegatedResourceCapsule::new)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  public void unLockExpireResource(byte[] from, byte[] to, long now) {
    byte[] lockKey = DelegatedResourceCapsule
        .createDbKeyV2(from, to, true);
    DelegatedResourceCapsule lockResource = get(lockKey);
    if (lockResource == null) {
      return;
    }
    if (lockResource.getExpireTimeForEnergy() >= now
        && lockResource.getExpireTimeForBandwidth() >= now) {
      return;
    }

    byte[] unlockKey = DelegatedResourceCapsule
        .createDbKeyV2(from, to, false);
    DelegatedResourceCapsule unlockResource = get(unlockKey);
    if (unlockResource == null) {
      unlockResource = new DelegatedResourceCapsule(ByteString.copyFrom(from),
          ByteString.copyFrom(to));
    }
    if (lockResource.getExpireTimeForEnergy() < now) {
      unlockResource.addFrozenBalanceForEnergy(
          lockResource.getFrozenBalanceForEnergy(), 0);
      lockResource.setFrozenBalanceForEnergy(0, 0);
    }
    if (lockResource.getExpireTimeForBandwidth() < now) {
      unlockResource.addFrozenBalanceForBandwidth(
          lockResource.getFrozenBalanceForBandwidth(), 0);
      lockResource.setFrozenBalanceForBandwidth(0, 0);
    }
    if (lockResource.getFrozenBalanceForBandwidth() == 0
        && lockResource.getFrozenBalanceForEnergy() == 0) {
      delete(lockKey);
    } else {
      put(lockKey, lockResource);
    }
    put(unlockKey, unlockResource);
  }

}