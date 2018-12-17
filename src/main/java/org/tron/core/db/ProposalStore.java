package org.tron.core.db;

import static org.tron.core.db.fast.FastSyncStoreConstant.TrieEnum.PROPOSAL;

import com.google.common.collect.Streams;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.ProposalCapsule;
import org.tron.core.db.fast.callback.FastSyncCallBack;
import org.tron.core.db.fast.storetrie.ProposalStoreTrie;

@Component
public class ProposalStore extends TronStoreWithRevoking<ProposalCapsule> {

  @Autowired
  private FastSyncCallBack fastSyncCallBack;

  @Autowired
  private ProposalStoreTrie proposalStoreTrie;

  @Autowired
  public ProposalStore(@Value("proposal") String dbName) {
    super(dbName);
  }

  @Override
  public ProposalCapsule get(byte[] key) {
    byte[] value = getValue(key);
    return ArrayUtils.isEmpty(value) ? null : new ProposalCapsule(value);
  }

  /**
   * get all proposals.
   */
  public List<ProposalCapsule> getAllProposals() {
    List<ProposalCapsule> proposalCapsuleList = proposalStoreTrie.getAllProposals();
    if (CollectionUtils.isNotEmpty(proposalCapsuleList)) {
      return proposalCapsuleList;
    }
    return Streams.stream(iterator())
        .map(Map.Entry::getValue)
        .sorted(
            (ProposalCapsule a, ProposalCapsule b) -> a.getCreateTime() <= b.getCreateTime() ? 1
                : -1)
        .collect(Collectors.toList());
  }

  @Override
  public void put(byte[] key, ProposalCapsule item) {
    super.put(key, item);
    fastSyncCallBack.callBack(key, item.getData(), PROPOSAL);
  }

  @Override
  public void delete(byte[] key) {
    super.delete(key);
    fastSyncCallBack.delete(key, PROPOSAL);
  }

  public byte[] getValue(byte[] key) {
    byte[] value = proposalStoreTrie.getValue(key);
    if (ArrayUtils.isEmpty(value)) {
      value = revokingDB.getUnchecked(key);
    }
    return value;
  }

  @Override
  public void close() {
    super.close();
    proposalStoreTrie.close();
  }
}