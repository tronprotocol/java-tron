package org.tron.core.db;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.DelegatedResourceCapsule;

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

}