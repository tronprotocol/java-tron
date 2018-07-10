package org.tron.core.db;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.ContractCapsule;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;

@Slf4j
@Component
public class ContractStore extends TronStoreWithRevoking<ContractCapsule> {

  @Autowired
  private ContractStore(@Value("contract") String dbName) {
    super(dbName);
  }

  @Override
  public ContractCapsule get(byte[] key) {
    byte[] value = dbSource.getData(key);
    return ArrayUtils.isEmpty(value) ? null : new ContractCapsule(value);
  }


  @Override
  public boolean has(byte[] key) {
    byte[] contract = dbSource.getData(key);
    return null != contract;
  }

  /**
   * get total transaction.
   */
  public long getTotalContracts() {
    return dbSource.getTotal();
  }

  private static ContractStore instance;

  public static void destory() {
    instance = null;
  }

  void destroy() {
    instance = null;
  }

  /**
   * find a transaction  by it's id.
   */
  public byte[] findContractByHash(byte[] trxHash) {
    return dbSource.getData(trxHash);
  }

  /**
   *
   * @param contractAddress
   * @return
   */
  public Contract.SmartContract.ABI getABI(byte[] contractAddress) {
    byte[] value = dbSource.getData(contractAddress);
    if (ArrayUtils.isEmpty(value)) {
      return null;
    }

    ContractCapsule contractCapsule = new ContractCapsule(value);
    Protocol.Transaction trx = contractCapsule.getInstance();
    Contract.SmartContract smartContract = ContractCapsule.getSmartContractFromTransaction(trx);
    if (smartContract == null) {
      return null;
    }

    return smartContract.getAbi();
  }

}
