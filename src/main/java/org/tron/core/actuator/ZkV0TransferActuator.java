package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.crypto.zksnark.Proof;
import org.tron.common.crypto.zksnark.VerifyingKey;
import org.tron.common.crypto.zksnark.ZkVerify;
import org.tron.common.crypto.zksnark.ZksnarkUtils;
import org.tron.core.zen.merkle.IncrementalMerkleTreeContainer;
import org.tron.core.zen.merkle.MerkleContainer;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.db.Manager;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.ZksnarkV0TransferContract;
import org.tron.protos.Contract.zkv0proof;
import org.tron.protos.Protocol.AccountType;

@Slf4j
public class ZkV0TransferActuator extends AbstractActuator {

  ZkV0TransferActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
    ZksnarkV0TransferContract zkContract;
    try {
      zkContract = contract.unpack(ZksnarkV0TransferContract.class);
      ByteString ownerAddress = zkContract.getOwnerAddress();
      if (!ownerAddress.isEmpty()) {
        long vFromPub = zkContract.getVFromPub();
        dbManager.adjustBalance(ownerAddress.toByteArray(), -vFromPub);
      }

      long fee = zkContract.getFee();

      ByteString toAddress = zkContract.getToAddress();
      if (!toAddress.isEmpty()) {
        long vToPub = zkContract.getVToPub();
        // if account with to_address does not exist, create it first.
        AccountCapsule toAccount = dbManager.getAccountStore().get(toAddress.toByteArray());
        boolean withDefaultPermission =
            dbManager.getDynamicPropertiesStore().getAllowMultiSign() == 1;
        if (toAccount == null) {
          toAccount = new AccountCapsule(toAddress, AccountType.Normal,
              dbManager.getHeadBlockTimeStamp(), withDefaultPermission, dbManager);
          dbManager.getAccountStore().put(toAddress.toByteArray(), toAccount);
        }
        dbManager.adjustBalance(toAccount, vToPub);
      }
      dbManager.adjustBalance(dbManager.getAccountStore().getBlackhole().createDbKey(), fee);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractExeException(e.getMessage());
    } catch (BalanceInsufficientException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractExeException(e.getMessage());
    }

    byte[] nf1 = zkContract.getNf1().toByteArray();
    byte[] nf2 = zkContract.getNf2().toByteArray();
    dbManager.getNullfierStore().put(nf1, new BytesCapsule(nf1));
    dbManager.getNullfierStore().put(nf2, new BytesCapsule(nf2));

    MerkleContainer merkleContainer = dbManager.getMerkleContainer();
    IncrementalMerkleTreeContainer currentMerkle = dbManager.getMerkleContainer()
        .getCurrentMerkle();

    merkleContainer.saveCmIntoMerkleTree(currentMerkle, zkContract.getCm1().toByteArray());
    merkleContainer.saveCmIntoMerkleTree(currentMerkle, zkContract.getCm2().toByteArray());

    merkleContainer.setCurrentMerkle(currentMerkle);

    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (this.contract == null) {
      throw new ContractValidateException("No contract!");
    }
    if (this.dbManager == null) {
      throw new ContractValidateException("No dbManager!");
    }
    if (!this.contract.is(ZksnarkV0TransferContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [ZksnarkV0TransferContract],real type[" + contract
              .getClass() + "]");
    }

    if (!dbManager.getDynamicPropertiesStore().supportZKSnarkTransaction()) {
      throw new ContractValidateException("Not support ZKSnarkTransaction, need to be opened by" +
          " the committee");
    }

    ZksnarkV0TransferContract zkContract;
    try {
      zkContract = contract.unpack(ZksnarkV0TransferContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    ByteString ownerAddress = zkContract.getOwnerAddress();
    long vFromPub = zkContract.getVFromPub();
    if (!ownerAddress.isEmpty()) {
      if (!Wallet.addressValid(ownerAddress.toByteArray())) {
        throw new ContractValidateException("Invalid ownerAddress");
      }
      AccountCapsule ownerAccount = dbManager.getAccountStore().get(ownerAddress.toByteArray());
      if (ownerAccount == null) {
        throw new ContractValidateException(
            "Validate ZkV0TransferActuator error, no OwnerAccount.");
      }

      long balance = ownerAccount.getBalance();
      if (balance < vFromPub) {
        throw new ContractValidateException(
            "Validate ZkV0TransferActuator error, balance is not sufficient.");
      }
    }
    if (vFromPub < 0) {
      throw new ContractValidateException("vFromPub can not less than 0.");
    }
    if (ownerAddress.isEmpty() ^ (vFromPub == 0)) {
      throw new ContractValidateException(
          "OwnerAddress needs to be empty and the vFromPub is zero, or neither.");
    }

    long fee = calcFee();

    ByteString toAddress = zkContract.getToAddress();
    long vToPub = zkContract.getVToPub();
    if (!toAddress.isEmpty()) {
      if (!Wallet.addressValid(toAddress.toByteArray())) {
        throw new ContractValidateException("Invalid toAddress");
      }
      if (toAddress.equals(ownerAddress)) {
        throw new ContractValidateException("Cannot transfer trx to yourself.");
      }
      AccountCapsule toAccount = dbManager.getAccountStore().get(toAddress.toByteArray());
      if (toAccount == null) {
        if (dbManager.getDynamicPropertiesStore().getCreateNewAccountFeeInSystemContract() != 0) {
          fee = Math.addExact(fee,
              dbManager.getDynamicPropertiesStore().getCreateNewAccountFeeInSystemContract());
        } else {
          fee = Math.addExact(fee, dbManager.getDynamicPropertiesStore().getCreateAccountFee());
        }
      } else {
        try {
          Math.addExact(toAccount.getBalance(), vToPub);
        } catch (ArithmeticException e) {
          throw new ContractValidateException(e.getMessage());
        }
      }
    }

    if (zkContract.getFee() != fee) {
      throw new ContractValidateException(
          "Contract transaction fee is inconsistent with system transaction fee");
    }

    if (vToPub < 0) {
      throw new ContractValidateException("vToPub can not less than 0.");
    }
    if (toAddress.isEmpty() ^ (vToPub == 0)) {
      throw new ContractValidateException(
          "ToAddress needs to be empty and the vToPub is zero, or neither.");
    }

    ByteString rt = zkContract.getRt();
    if (rt.isEmpty() || rt.size() != 32) {
      throw new ContractValidateException("Merkel root is invalid.");
    }

    if (!dbManager.getMerkleContainer().merkleRootExist(rt.toByteArray())) {
      throw new ContractValidateException("Rt is invalid.");
    }

    ByteString nf1 = zkContract.getNf1();
    if (nf1.size() != 32) {
      throw new ContractValidateException("Nf1 is invalid.");
    }

    ByteString nf2 = zkContract.getNf2();
    if (nf2.size() != 32) {
      throw new ContractValidateException("Nf2 is invalid.");
    }

    if (nf1.equals(nf2)) {
      throw new ContractValidateException("Nf1 equals to nf2.");
    }

    if (dbManager.getNullfierStore().has(nf1.toByteArray())) {
      throw new ContractValidateException("Nf1 is exist.");
    }

    if (dbManager.getNullfierStore().has(nf2.toByteArray())) {
      throw new ContractValidateException("Nf2 is exist.");
    }

    ByteString cm1 = zkContract.getCm1();
    if (cm1.size() != 32) {
      throw new ContractValidateException("Cm1 is invalid.");
    }

    ByteString cm2 = zkContract.getCm2();
    if (cm2.size() != 32) {
      throw new ContractValidateException("Cm2 is invalid.");
    }

    if (cm1.equals(cm2)) {
      throw new ContractValidateException("Cm1 equals to Cm2.");
    }

    if (zkContract.getPksig().size() != 32) {
      throw new ContractValidateException("Pksig is invalid.");
    }

    if (zkContract.getRandomSeed().size() != 32) {
      throw new ContractValidateException("RandomSeed is invalid.");
    }

    if (zkContract.getEpk().size() != 32) {
      throw new ContractValidateException("Epk is invalid.");
    }

    if (zkContract.getH1().size() != 32) {
      throw new ContractValidateException("H1 is invalid.");
    }

    if (zkContract.getH2().size() != 32) {
      throw new ContractValidateException("H2 is invalid.");
    }

    if (zkContract.getC1().isEmpty()) {
      throw new ContractValidateException("C1 is empty.");
    }

    if (zkContract.getC2().isEmpty()) {
      throw new ContractValidateException("C2 is empty.");
    }

    if (zkv0proof.getDefaultInstance().equals(zkContract.getProof())) {
      throw new ContractValidateException("Proof is null.");
    }

    Proof proof = ZksnarkUtils.zkproof2Proof(zkContract.getProof());
    if (proof == null) {
      throw new ContractValidateException("Proof is invalid.");
    }

    //computer witness rt h_sig h1 h2 nf1 nf2 cm1 cm2 v_pub_old v_pub_new
    byte[] hSig = ZksnarkUtils.computeHSig(zkContract);
    long vPubNew = Math.addExact(vToPub, fee);
    BigInteger[] witness = ZksnarkUtils
        .witnessMap(rt.toByteArray(), hSig, zkContract.getH1().toByteArray(),
            zkContract.getH2().toByteArray(), nf1.toByteArray(), nf2.toByteArray(),
            cm1.toByteArray(), cm2.toByteArray(), vFromPub, vPubNew);
    //verify
    int result = new ZkVerify().verify(VerifyingKey.initVk(), witness, proof);
    if (result != 0) {
      throw new ContractValidateException("verify failed return " + result + " .");
    }
    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(ZksnarkV0TransferContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return dbManager.getDynamicPropertiesStore().getZksnarkTransactionFee();
  }

}