package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.sun.jna.Pointer;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.tron.common.zksnark.zen.Librustzcash;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.db.Manager;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.ReceiveDescription;
import org.tron.protos.Contract.ShieldedTransferContract;
import org.tron.protos.Contract.SpendDescription;
import org.tron.protos.Protocol.Transaction.Result.code;


@Slf4j(topic = "actuator")
public class ShieldedTransferActuator extends AbstractActuator {

  ShieldedTransferActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultCapsule ret)
      throws ContractExeException {

    long fee = calcFee();
    ShieldedTransferContract strx;
    try {
      strx = contract.unpack(ShieldedTransferContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }

    if (strx.getTransparentFromAddress().toByteArray().length > 0) {
      executeTransparentOut(strx.getTransparentFromAddress().toByteArray(), strx.getFromAmount(), fee, ret);
    }

    executeShielded(strx.getSpendDescriptionList(), strx.getReceiveDescriptionList());

    if (strx.getTransparentToAddress().toByteArray().length > 0) {
      executeTransparentIn(strx.getTransparentToAddress().toByteArray(), strx.getToAmount());
    }

    return true;
  }

  private void executeTransparentOut(byte[] ownerAddress, long amount, long fee, TransactionResultCapsule ret) throws ContractExeException {
    try {
      dbManager.adjustBalance(ownerAddress, -fee);
      dbManager.adjustBalance(dbManager.getAccountStore().getBlackhole().createDbKey(), fee);
      dbManager.adjustBalance(ownerAddress, -amount);
      ret.setStatus(fee, code.SUCESS);
    } catch (BalanceInsufficientException e) {
      throw new ContractExeException(e.getMessage());
    }
  }

  private void executeTransparentIn(byte[] toAddress, long amount) throws ContractExeException {
    try {
      dbManager.adjustBalance(toAddress, amount);
    } catch (BalanceInsufficientException e) {
      throw new ContractExeException(e.getMessage());
    }
  }

  //record shielded transaction data.
  private void executeShielded(List<SpendDescription> spends, List<ReceiveDescription> receives) {
    //handle spends
    for (SpendDescription spend: spends
    ) {
      dbManager.getNullfierStore().put(new BytesCapsule(spend.getNullifier().toByteArray()));
    }

    //handle receives
    for (ReceiveDescription receive : receives)  {
      dbManager.processNoteCommitment(receive.getNoteCommitment().toByteArray());
    }
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (this.contract == null) {
      throw new ContractValidateException("No contract!");
    }
    if (this.dbManager == null) {
      throw new ContractValidateException("No dbManager!");
    }
    if (!this.contract.is(ShieldedTransferContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [ShieldedTransferContract],real type[" + contract
              .getClass() + "]");
    }

    ShieldedTransferContract shieldedTransferContract;
    try {
      shieldedTransferContract = contract.unpack(ShieldedTransferContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    byte[] signHash = new byte[32];
    // TODO generate signhash
    List<SpendDescription> spendDescriptions = shieldedTransferContract.getSpendDescriptionList();
    // check duplicate sapling nullifiers
    if (CollectionUtils.isNotEmpty(spendDescriptions)) {
      for (SpendDescription spendDescription : spendDescriptions) {
        if (dbManager.getNullfierStore().has(spendDescription.getNullifier().toByteArray())) {
          throw new ContractValidateException("duplicate sapling nullifiers in this transaction");
        }
      }
    }
    List<ReceiveDescription> receiveDescriptions = shieldedTransferContract
        .getReceiveDescriptionList();
    if (CollectionUtils.isNotEmpty(spendDescriptions)
        || CollectionUtils.isNotEmpty(receiveDescriptions)) {
      Pointer ctx = Librustzcash.librustzcashSaplingVerificationCtxInit();
      for (SpendDescription spendDescription : spendDescriptions) {
        if (!Librustzcash.librustzcashSaplingCheckSpend(
            ctx,
            spendDescription.getValueCommitment().toByteArray(),
            spendDescription.getAnchor().toByteArray(),
            spendDescription.getNullifier().toByteArray(),
            spendDescription.getRk().toByteArray(),
            spendDescription.getZkproof().toByteArray(),
            spendDescription.getSpendAuthoritySignature().toByteArray(),
            signHash
        )) {
          Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);
          throw new ContractValidateException("librustzcashSaplingCheckSpend error");
        }
      }

      for (ReceiveDescription receiveDescription : receiveDescriptions) {
        if (!Librustzcash.librustzcashSaplingCheckOutput(
            ctx,
            receiveDescription.getValueCommitment().toByteArray(),
            receiveDescription.getNoteCommitment().toByteArray(),
            receiveDescription.getEpk().toByteArray(),
            receiveDescription.getZkproof().toByteArray()
        )) {
          Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);
          throw new ContractValidateException("librustzcashSaplingCheckOutput error");
        }
      }

      if (!Librustzcash.librustzcashSaplingFinalCheck(
          ctx,
          shieldedTransferContract.getValueBalance(),
          shieldedTransferContract.getBindingSignature().toByteArray(),
          signHash
      )) {
        Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);
        throw new ContractValidateException("librustzcashSaplingFinalCheck error");
      }
    }
    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return null;
  }

  @Override
  public long calcFee() {
    return 0;
  }

  public static void main(String[] args) {
    Pointer ctx = Librustzcash.librustzcashSaplingVerificationCtxInit();
  }
}
