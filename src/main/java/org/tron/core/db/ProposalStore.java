package org.tron.core.db;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.ProposalCapsule;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.protos.Protocol.Proposal;

@Component
public class ProposalStore extends TronStoreWithRevoking<ProposalCapsule> {

  @Autowired
  public ProposalStore(@Value("proposal") String dbName) {
    super(dbName);
  }

  @Override
  public ProposalCapsule get(byte[] key) throws ItemNotFoundException {
    byte[] value = dbSource.getData(key);
    if (ArrayUtils.isEmpty(value)) {
      throw new ItemNotFoundException();
    }
    return new ProposalCapsule(value);
  }

  @Override
  public boolean has(byte[] key) {
    byte[] account = dbSource.getData(key);
    return null != account;
  }

  @Override
  public void put(byte[] key, ProposalCapsule item) {
    super.put(key, item);
  }

  /**
   * get all witnesses.
   */
  public List<ProposalCapsule> getAllProposals() {
    return dbSource
        .allValues()
        .stream()
        .map(bytes -> new ProposalCapsule(bytes))
        .sorted(
            (ProposalCapsule a, ProposalCapsule b) -> a.getCreateTime() <= b.getCreateTime() ?
                1 : -1)
        .collect(Collectors.toList());
  }

  public org.tron.core.db.common.iterator.DBIterator getIterator() {
    return dbSource.iterator();
  }
}