package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.sun.jna.Pointer;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.tron.common.zksnark.Librustzcash;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.db.Manager;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.zen.merkle.IncrementalMerkleTreeContainer;
import org.tron.core.zen.merkle.MerkleContainer;
import org.tron.protos.Contract.ReceiveDescription;
import org.tron.protos.Contract.ShieldedTransferContract;
import org.tron.protos.Contract.SpendDescription;
import org.tron.protos.Protocol.Transaction.Result.code;


@Slf4j(topic = "actuator")
public class ShieldedTransferActuator extends AbstractActuator {

  private TransactionCapsule tx;

  ShieldedTransferActuator(Any contract, Manager dbManager, TransactionCapsule tx) {
    super(contract, dbManager);
    this.tx = tx;
  }

  boolean isTransparentOut = false;

  boolean isTransparentIn = false;


  @Override
  public boolean execute(TransactionResultCapsule ret)
      throws ContractExeException {

    long fee = calcFee();
    ShieldedTransferContract shieldedTransferContract;
    try {
      shieldedTransferContract = contract.unpack(ShieldedTransferContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }

    if (shieldedTransferContract.getTransparentFromAddress().toByteArray().length > 0) {
      executeTransparentOut(shieldedTransferContract.getTransparentFromAddress().toByteArray(),
          shieldedTransferContract.getFromAmount(), fee, ret);
    }

    executeShielded(shieldedTransferContract.getSpendDescriptionList(),
        shieldedTransferContract.getReceiveDescriptionList());

    if (shieldedTransferContract.getTransparentToAddress().toByteArray().length > 0) {
      executeTransparentIn(shieldedTransferContract.getTransparentToAddress().toByteArray(),
          shieldedTransferContract.getToAmount());
    }

    return true;
  }

  private void executeTransparentOut(byte[] ownerAddress, long amount, long fee,
      TransactionResultCapsule ret) throws ContractExeException {
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
    for (SpendDescription spend : spends
    ) {
      dbManager.getNullfierStore().put(new BytesCapsule(spend.getNullifier().toByteArray()));
    }

    MerkleContainer merkleContainer = dbManager.getMerkleContainer();
    IncrementalMerkleTreeContainer currentMerkle = dbManager.getMerkleContainer()
        .getCurrentMerkle();

    //handle receives
    for (ReceiveDescription receive : receives) {
      dbManager.processNoteCommitment(receive.getNoteCommitment().toByteArray());
      //add merkle root
      //currentMerkle.append();
    }
    merkleContainer.setCurrentMerkle(currentMerkle);
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
    byte[] signHash;
    try {
      shieldedTransferContract = contract.unpack(ShieldedTransferContract.class);
      signHash = TransactionCapsule.hash(tx);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    //transparent verification
    if (!validateTransparent(shieldedTransferContract)) {
      return false;
    }

    List<SpendDescription> spendDescriptions = shieldedTransferContract.getSpendDescriptionList();
    // check duplicate sapling nullifiers
    if (CollectionUtils.isNotEmpty(spendDescriptions)) {
      for (SpendDescription spendDescription : spendDescriptions) {
        if (!dbManager.getMerkleContainer()
            .merkleRootExist(spendDescription.getAnchor().toByteArray())) {
          throw new ContractValidateException("Rt is invalid.");
        }
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
            spendDescription.getZkproof().getValues().toByteArray(),
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
            receiveDescription.getZkproof().getValues().toByteArray()
        )) {
          Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);
          throw new ContractValidateException("librustzcashSaplingCheckOutput error");
        }
      }

      long valueBalance = shieldedTransferContract.getToAmount() -
          shieldedTransferContract.getFromAmount() + calcFee();
      if (!Librustzcash.librustzcashSaplingFinalCheck(
          ctx,
          valueBalance,
          shieldedTransferContract.getBindingSignature().toByteArray(),
          signHash
      )) {
        Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);
        throw new ContractValidateException("librustzcashSaplingFinalCheck error");
      }
    }
    return true;
  }

  private boolean validateTransparent(ShieldedTransferContract strx)
      throws ContractValidateException {

    byte[] toAddress = strx.getTransparentToAddress().toByteArray();
    byte[] ownerAddress = strx.getTransparentFromAddress().toByteArray();

    isTransparentIn = (toAddress.length > 0);
    isTransparentOut = (ownerAddress.length > 0);

    long amountOut = strx.getFromAmount();
    long amountIn = strx.getToAmount();

    if (isTransparentOut && !Wallet.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid ownerAddress");
    }
    if (isTransparentIn && !Wallet.addressValid(toAddress)) {
      throw new ContractValidateException("Invalid toAddress");
    }

    if (isTransparentIn && isTransparentOut && Arrays.equals(toAddress, ownerAddress)) {
      throw new ContractValidateException("Cannot transfer trx to yourself.");
    }

    if (isTransparentOut) {
      AccountCapsule ownerAccount = dbManager.getAccountStore().get(ownerAddress);
      if (ownerAccount == null) {
        throw new ContractValidateException("Validate TransferContract error, no OwnerAccount.");
      }

      long balance = ownerAccount.getBalance();

      if (amountOut <= 0) {
        throw new ContractValidateException("Amount must greater than 0.");
      }

      if (balance < amountOut) {
        throw new ContractValidateException(
            "Validate TransferContract error, balance is not sufficient.");
      }

      //use shield transaction to create transparent address
//      try {
//
//        AccountCapsule toAccount = dbManager.getAccountStore().get(toAddress);
//        if (toAccount == null) {
//          fee = fee + dbManager.getDynamicPropertiesStore().getCreateNewAccountFeeInSystemContract();
//        }
//
//        if (balance < Math.addExact(amount, fee)) {
//          throw new ContractValidateException(
//              "Validate TransferContract error, balance is not sufficient.");
//        }
//
//        if (toAccount != null) {
//          long toAddressBalance = Math.addExact(toAccount.getBalance(), amount);
//        }
//
//      } catch (ArithmeticException e) {
//        logger.debug(e.getMessage(), e);
//        throw new ContractValidateException(e.getMessage());
//      }

      //TODO: We need check the delta amount in the librustzcash. this delta amount include the fee using to create address.
      //TODO: This delta balance value can be calculated automatically.
    }
    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return null;
  }

  @Override
  public long calcFee() {
    return 10 * 1000000;
  }

  public static void main(String[] args) {
    Pointer ctx = Librustzcash.librustzcashSaplingVerificationCtxInit();
  }
}
