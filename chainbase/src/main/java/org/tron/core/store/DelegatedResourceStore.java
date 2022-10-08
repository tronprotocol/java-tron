package org.tron.core.store;

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
    super(dbName, DelegatedResourceCapsule.class);
  }

  @Override
  public DelegatedResourceCapsule get(byte[] key) {
    return getNonEmpty(key);
  }

  @Deprecated
  public List<DelegatedResourceCapsule> getByFrom(byte[] key) {
    return revokingDB.getValuesNext(key, Long.MAX_VALUE).stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

}