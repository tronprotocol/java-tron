package org.tron.core.store;

import com.google.common.collect.Streams;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.AbiCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;

@Slf4j(topic = "DB")
@Component
public class ContractStore extends TronStoreWithRevoking<ContractCapsule> {

  @Autowired
  private AbiStore abiStore;

  @Autowired
  private ContractStore(@Value("contract") String dbName) {
    super(dbName);
  }

  @Override
  public ContractCapsule get(byte[] key) {
    ContractCapsule contractCapsule = getUnchecked(key);
    if (contractCapsule == null) {
      return null;
    }

    AbiCapsule abiCapsule = abiStore.get(key);
    if (abiCapsule != null) {
      contractCapsule = new ContractCapsule(contractCapsule.getInstance()
          .toBuilder().setAbi(abiCapsule.getInstance()).build());
    }
    return contractCapsule;
  }

  /**
   * get total transaction.
   */
  public long getTotalContracts() {
    return Streams.stream(revokingDB.iterator()).count();
  }

  /**
   * find a transaction  by it's id.
   */
  public byte[] findContractByHash(byte[] trxHash) {
    return revokingDB.getUnchecked(trxHash);
  }

  /**
   *
   * @param contractAddress
   * @return
   */
  public SmartContract.ABI getABI(byte[] contractAddress) {
    ContractCapsule contractCapsule = get(contractAddress);
    if (contractCapsule == null) {
      return null;
    }

    return contractCapsule.getInstance().getAbi();
  }

}
