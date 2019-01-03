package org.tron.core.db;

import static org.tron.core.db.fast.FastSyncStoreConstant.TrieEnum.CONTRACT;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.db.fast.callback.FastSyncCallBack;
import org.tron.core.db.fast.storetrie.ContractStoreTrie;
import org.tron.protos.Protocol.SmartContract;

@Slf4j(topic = "DB")
@Component
public class ContractStore extends TronStoreWithRevoking<ContractCapsule> {

  @Autowired
  private FastSyncCallBack fastSyncCallBack;

  @Autowired
  private ContractStoreTrie contractStoreTrie;

  @Autowired
  private ContractStore(@Value("contract") String dbName) {
    super(dbName);
  }

  public ContractCapsule getValue(byte[] key) {
    byte[] value = contractStoreTrie.getValue(key);
    if (ArrayUtils.isEmpty(value)) {
      return getUnchecked(key);
    }
    return new ContractCapsule(value);
  }

  @Override
  public ContractCapsule get(byte[] key) {
    return getValue(key);
  }

  /**
   *
   * @param contractAddress
   * @return
   */
  public SmartContract.ABI getABI(byte[] contractAddress) {
    ContractCapsule contractCapsule = getValue(contractAddress);
    if (contractCapsule == null) {
      return null;
    }
    SmartContract smartContract = contractCapsule.getInstance();
    if (smartContract == null) {
      return null;
    }

    return smartContract.getAbi();
  }

  @Override
  public void delete(byte[] key) {
    super.delete(key);
    fastSyncCallBack.delete(key, CONTRACT);
  }

  @Override
  public void put(byte[] key, ContractCapsule item) {
    super.put(key, item);
    fastSyncCallBack.callBack(key, item.getData(), CONTRACT);
  }

  @Override
  public void close() {
    super.close();
    contractStoreTrie.close();
  }
}
