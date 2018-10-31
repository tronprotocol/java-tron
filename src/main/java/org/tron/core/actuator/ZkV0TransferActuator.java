package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.MerkelRoot;
import org.tron.protos.Contract.ZksnarkV0TransferContract;
import org.tron.protos.Contract.zkv0proof;

@Slf4j
public class ZkV0TransferActuator extends AbstractActuator {

  ZkV0TransferActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
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
    }
    if (vFromPub < 0) {
      throw new ContractValidateException("vFromPub can not less than 0.");
    }
    if (ownerAddress.isEmpty() ^ (vFromPub == 0)) {
      throw new ContractValidateException(
          "OwnerAddress needs to be empty and the vFromPub is zero, or neither.");
    }

    ByteString toAddress = zkContract.getToAddress();
    long vToPub = zkContract.getVToPub();
    if (!toAddress.isEmpty()) {
      if (!Wallet.addressValid(toAddress.toByteArray())) {
        throw new ContractValidateException("Invalid toAddress");
      }
      if (toAddress.equals(ownerAddress)) {
        throw new ContractValidateException("Cannot transfer trx to yourself.");
      }
    }
    if (vToPub < 0) {
      throw new ContractValidateException("vToPub can not less than 0.");
    }
    if (toAddress.isEmpty() ^ (vToPub == 0)) {
      throw new ContractValidateException(
          "ToAddress needs to be empty and the vToPub is zero, or neither.");
    }

    MerkelRoot rt = zkContract.getRt();
    ByteString nf1 = zkContract.getNf1();
    ByteString nf2 = zkContract.getNf2();
    if (rt == MerkelRoot.getDefaultInstance()) {
      if (nf1 != ByteString.EMPTY || nf2 != ByteString.EMPTY) {
        throw new ContractValidateException("Merkel root is null, nf1 nf2 need empty.");
      }
    } else {
      //check rt
      if (nf1 == ByteString.EMPTY && nf2 == ByteString.EMPTY) {
        throw new ContractValidateException("Merkel root is not null, both nf1 nf2 is empty.");
      }
      if (nf1.equals(nf2)) {
        throw new ContractValidateException("Nf1 equals to nf2.");
      }
      //check nf1 nf2
    }
    if (ownerAddress.isEmpty() && nf1.isEmpty() && nf2.isEmpty()) {
      throw new ContractValidateException("All from address is empty.");
    }

    ByteString cm1 = zkContract.getCm1();
    ByteString cm2 = zkContract.getCm2();
    if (toAddress.isEmpty() && cm1.isEmpty() && cm2.isEmpty()) {
      throw new ContractValidateException("All to address is empty.");
    }

    if (nf1.isEmpty() && nf2.isEmpty() && cm1.isEmpty() && cm2.isEmpty()) {
      throw new ContractValidateException("No shield from and to address.");
    }

    if (!cm1.isEmpty() || !cm2.isEmpty()) {
      if (zkContract.getRandomSeed().isEmpty()) {
        throw new ContractValidateException("randomSeed is empty.");
      }
      if (zkContract.getEpk().isEmpty()) {
        throw new ContractValidateException("Epk is empty.");
      }
    }
    if (nf1.isEmpty() ^ zkContract.getH1().isEmpty()) {
      throw new ContractValidateException(
          "Needs both of nf1 and h1 are empty, or neither.");
    }
    if (nf2.isEmpty() ^ zkContract.getH2().isEmpty()) {
      throw new ContractValidateException(
          "Needs both of nf2 and h2 are empty, or neither.");
    }
    if (cm1.isEmpty() ^ zkContract.getC1().isEmpty()) {
      throw new ContractValidateException(
          "Needs both of cm1 and C1 are empty, or neither.");
    }
    if (cm2.isEmpty() ^ zkContract.getC2().isEmpty()) {
      throw new ContractValidateException(
          "Needs both of cm2 and C2 are empty, or neither.");
    }

    if (zkContract.getProof() == zkv0proof.getDefaultInstance()) {
      throw new ContractValidateException("Proof is null.");
    }

    //computer witness
    //verify
    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(ZksnarkV0TransferContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return ChainConstant.TRANSFER_FEE;
  }

}