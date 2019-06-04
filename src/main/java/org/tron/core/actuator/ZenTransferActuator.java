package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.sun.jna.Pointer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.tron.common.zksnark.Librustzcash;
import org.tron.common.zksnark.LibrustzcashParam.CheckOutputParams;
import org.tron.common.zksnark.LibrustzcashParam.CheckSpendParams;
import org.tron.common.zksnark.LibrustzcashParam.FinalCheckParams;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.zen.merkle.IncrementalMerkleTreeContainer;
import org.tron.core.zen.merkle.MerkleContainer;
import org.tron.protos.Contract.ReceiveDescription;
import org.tron.protos.Contract.SpendDescription;
import org.tron.protos.Contract.ZenTransferContract;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.code;
import static org.tron.core.zen.note.ZenChainParams.*;

@Slf4j(topic = "actuator")
public class ZenTransferActuator extends AbstractActuator {

  private TransactionCapsule tx;
  private ZenTransferContract zenTransferContract;
  static public String zenTokenId = "000001";

  ZenTransferActuator(Any contract, Manager dbManager, TransactionCapsule tx) {
    super(contract, dbManager);
    this.tx = tx;
  }

  @Override
  public boolean execute(TransactionResultCapsule ret)
      throws ContractExeException {
    long fee = calcFee();
    try {
      zenTransferContract = contract.unpack(ZenTransferContract.class);
      if (zenTransferContract.getTransparentFromAddress().toByteArray().length > 0) {
        executeTransparentFrom(zenTransferContract.getTransparentFromAddress().toByteArray(),
            zenTransferContract.getFromAmount(), ret);
      }
      dbManager.adjustAssetBalanceV2(dbManager.getAccountStore().getBlackhole().createDbKey(),
          zenTokenId, fee);
    } catch (BalanceInsufficientException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }

    executeShielded(zenTransferContract.getSpendDescriptionList(),
        zenTransferContract.getReceiveDescriptionList(), ret);

    if (zenTransferContract.getTransparentToAddress().toByteArray().length > 0) {
      executeTransparentTo(zenTransferContract.getTransparentToAddress().toByteArray(),
          zenTransferContract.getToAmount(), ret);
    }

    //adjust and verify total shielded pool value
    try {
      dbManager.adjustTotalShieldedPoolValue(
          Math.addExact(Math.subtractExact(zenTransferContract.getToAmount(),
              zenTransferContract.getFromAmount()), fee));
    } catch (ArithmeticException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    } catch (BalanceInsufficientException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }

    ret.setStatus(fee, code.SUCESS);
    return true;
  }

  private void executeTransparentFrom(byte[] ownerAddress, long amount,
      TransactionResultCapsule ret)
      throws ContractExeException {
    try {
      dbManager.adjustAssetBalanceV2(ownerAddress, zenTokenId, -amount);
    } catch (BalanceInsufficientException e) {
      ret.setStatus(calcFee(), code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
  }

  private void executeTransparentTo(byte[] toAddress, long amount, TransactionResultCapsule ret)
      throws ContractExeException {
    try {
      AccountCapsule toAccount = dbManager.getAccountStore().get(toAddress);
      if (toAccount == null) {
        boolean withDefaultPermission =
            dbManager.getDynamicPropertiesStore().getAllowMultiSign() == 1;
        toAccount = new AccountCapsule(ByteString.copyFrom(toAddress), AccountType.Normal,
            dbManager.getHeadBlockTimeStamp(), withDefaultPermission, dbManager);
        dbManager.getAccountStore().put(toAddress, toAccount);
      }
      dbManager.adjustAssetBalanceV2(toAddress, zenTokenId, amount);
    } catch (BalanceInsufficientException e) {
      ret.setStatus(calcFee(), code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
  }

  //record shielded transaction data.
  private void executeShielded(List<SpendDescription> spends, List<ReceiveDescription> receives,
      TransactionResultCapsule ret)
      throws ContractExeException {
    //handle spends
    for (SpendDescription spend : spends) {
      if (dbManager.getNullfierStore().has(
          new BytesCapsule(spend.getNullifier().toByteArray()).getData())) {
        ret.setStatus(calcFee(), code.FAILED);
        throw new ContractExeException("double spend");
      }
      dbManager.getNullfierStore().put(new BytesCapsule(spend.getNullifier().toByteArray()));
    }
    if (Args.getInstance().isAllowShieldedTransaction()) {
      MerkleContainer merkleContainer = dbManager.getMerkleContainer();
      IncrementalMerkleTreeContainer currentMerkle = merkleContainer.getCurrentMerkle();
      try {
        currentMerkle.wfcheck();
      } catch (ZksnarkException e) {
        ret.setStatus(calcFee(), code.FAILED);
        throw new ContractExeException(e.getMessage());
      }
      //handle receives
      for (ReceiveDescription receive : receives) {
        try {
          merkleContainer
              .saveCmIntoMerkleTree(currentMerkle, receive.getNoteCommitment().toByteArray());
        } catch (ZksnarkException e) {
          ret.setStatus(calcFee(), code.FAILED);
          throw new ContractExeException(e.getMessage());
        }
      }
      merkleContainer.setCurrentMerkle(currentMerkle);
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
    try {
      zenTransferContract = contract.unpack(ZenTransferContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    if (!dbManager.getDynamicPropertiesStore().supportZKSnarkTransaction()) {
      throw new ContractValidateException("Not support ZKSnarkTransaction, need to be opened by" +
          " the committee");
    }

    byte[] signHash = TransactionCapsule.getShieldTransactionHashIgnoreTypeException(tx);

    //transparent verification
    checkSender(zenTransferContract);
    checkReceiver(zenTransferContract);
    validateTransparent(zenTransferContract);

    long fee = calcFee();

    List<SpendDescription> spendDescriptions = zenTransferContract.getSpendDescriptionList();
    // check duplicate sapling nullifiers
    if (CollectionUtils.isNotEmpty(spendDescriptions)) {
      HashSet<ByteString> nfSet = new HashSet<>();
      for (SpendDescription spendDescription : spendDescriptions) {
        if (nfSet.contains(spendDescription.getNullifier())) {
          throw new ContractValidateException("duplicate sapling nullifiers in this transaction");
        }
        nfSet.add(spendDescription.getNullifier());
        if (Args.getInstance().isAllowShieldedTransaction() && !dbManager.getMerkleContainer()
            .merkleRootExist(spendDescription.getAnchor().toByteArray())) {
          throw new ContractValidateException("Rt is invalid.");
        }
        if (dbManager.getNullfierStore().has(spendDescription.getNullifier().toByteArray())) {
          throw new ContractValidateException("note has been spend in this transaction");
        }
      }
    }

    List<ReceiveDescription> receiveDescriptions = zenTransferContract
        .getReceiveDescriptionList();

    HashSet<ByteString> receiveSet = new HashSet<>();
    for (ReceiveDescription receiveDescription : receiveDescriptions) {
      if (receiveSet.contains(receiveDescription.getNoteCommitment())) {
        throw new ContractValidateException("duplicate cm in receive_description");
      }
      receiveSet.add(receiveDescription.getNoteCommitment());
    }
    if (CollectionUtils.isEmpty(spendDescriptions)
        && CollectionUtils.isEmpty(receiveDescriptions)) {
      throw new ContractValidateException("no Description found in transaction");
    }

    if (CollectionUtils.isNotEmpty(spendDescriptions)
        || CollectionUtils.isNotEmpty(receiveDescriptions)) {
      Pointer ctx = Librustzcash.librustzcashSaplingVerificationCtxInit();
      try {
        for (SpendDescription spendDescription : spendDescriptions) {
          if (!Librustzcash.librustzcashSaplingCheckSpend(
              new CheckSpendParams(ctx,
                  spendDescription.getValueCommitment().toByteArray(),
                  spendDescription.getAnchor().toByteArray(),
                  spendDescription.getNullifier().toByteArray(),
                  spendDescription.getRk().toByteArray(),
                  spendDescription.getZkproof().toByteArray(),
                  spendDescription.getSpendAuthoritySignature().toByteArray(),
                  signHash)
          )) {
            throw new ContractValidateException("librustzcashSaplingCheckSpend error");
          }
        }

        for (ReceiveDescription receiveDescription : receiveDescriptions) {
          if (receiveDescription.getCEnc().size() != ZC_ENCCIPHERTEXT_SIZE
              || receiveDescription.getCOut().size() != ZC_OUTCIPHERTEXT_SIZE) {
            throw new ContractValidateException("Cout or CEnc size error");
          }
          if (!Librustzcash.librustzcashSaplingCheckOutput(
              new CheckOutputParams(ctx,
                  receiveDescription.getValueCommitment().toByteArray(),
                  receiveDescription.getNoteCommitment().toByteArray(),
                  receiveDescription.getEpk().toByteArray(),
                  receiveDescription.getZkproof().toByteArray())
          )) {
            throw new ContractValidateException("librustzcashSaplingCheckOutput error");
          }
        }

        long valueBalance;
        long totalShieldedPoolValue = dbManager.getDynamicPropertiesStore()
            .getTotalShieldedPoolValue();
        try {
          valueBalance = Math.addExact(Math.subtractExact(zenTransferContract.getToAmount(),
              zenTransferContract.getFromAmount()), fee);
          totalShieldedPoolValue = Math.subtractExact(totalShieldedPoolValue, valueBalance);
        } catch (ArithmeticException e) {
          logger.debug(e.getMessage(), e);
          throw new ContractValidateException(e.getMessage());
        }

        if (totalShieldedPoolValue < 0) {
          throw new ContractValidateException("shieldedPoolValue error");
        }

        if (!Librustzcash.librustzcashSaplingFinalCheck(
            new FinalCheckParams(ctx,
                valueBalance,
                zenTransferContract.getBindingSignature().toByteArray(),
                signHash)
        )) {
          throw new ContractValidateException("librustzcashSaplingFinalCheck error");
        }
      } catch (ZksnarkException e) {
        throw new ContractValidateException(e.getMessage());
      } finally {
        Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);
      }
    }

    return true;
  }

  private void checkSender(ZenTransferContract zenTransferContract)
      throws ContractValidateException {
    if (!zenTransferContract.getTransparentFromAddress().isEmpty()
        && zenTransferContract.getSpendDescriptionCount() > 0) {
      throw new ContractValidateException("ZenTransferContract error, more than 1 senders");
    }
    if (zenTransferContract.getTransparentFromAddress().isEmpty()
        && zenTransferContract.getSpendDescriptionCount() == 0) {
      throw new ContractValidateException("ZenTransferContract error, no sender");
    }
    if (zenTransferContract.getSpendDescriptionCount() > 10) {
      throw new ContractValidateException("ZenTransferContract error, number of spend notes"
          + " should not be more than 10");
    }
  }

  private void checkReceiver(ZenTransferContract zenTransferContract)
      throws ContractValidateException {
    if (zenTransferContract.getTransparentToAddress().isEmpty()
        && zenTransferContract.getReceiveDescriptionCount() == 0) {
      throw new ContractValidateException("ZenTransferContract error, no receiver");
    }
    if (zenTransferContract.getReceiveDescriptionCount() > 10) {
      throw new ContractValidateException("ZenTransferContract error, number of receivers"
          + " should not be more than 10");
    }
  }

  private void validateTransparent(ZenTransferContract zenTransferContract)
      throws ContractValidateException {
    boolean hasTransparentFrom;
    boolean hasTransparentTo;
    byte[] toAddress = zenTransferContract.getTransparentToAddress().toByteArray();
    byte[] ownerAddress = zenTransferContract.getTransparentFromAddress().toByteArray();

    hasTransparentFrom = (ownerAddress.length > 0);
    hasTransparentTo = (toAddress.length > 0);

    long fromAmount = zenTransferContract.getFromAmount();
    long toAmount = zenTransferContract.getToAmount();
    if (fromAmount < 0) {
      throw new ContractValidateException("from_amount should not be less than 0");
    }
    if (toAmount < 0) {
      throw new ContractValidateException("to_amount should not be less than 0");
    }

    if (hasTransparentFrom && !Wallet.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid transparent_from_address");
    }
    if (!hasTransparentFrom && fromAmount != 0) {
      throw new ContractValidateException("no transparent_from_address, from_amount should be 0");
    }
    if (hasTransparentTo && !Wallet.addressValid(toAddress)) {
      throw new ContractValidateException("Invalid transparent_to_address");
    }
    if (!hasTransparentTo && toAmount != 0) {
      throw new ContractValidateException("no transparent_to_address, to_amount should be 0");
    }
    if (hasTransparentFrom && hasTransparentTo && Arrays.equals(toAddress, ownerAddress)) {
      throw new ContractValidateException("Can't transfer zen to yourself");
    }

    if (hasTransparentFrom) {
      AccountCapsule ownerAccount = dbManager.getAccountStore().get(ownerAddress);
      if (ownerAccount == null) {
        throw new ContractValidateException("Validate ZenTransferContract error, "
            + "no OwnerAccount");
      }
      long balance = getZenBalance(ownerAccount);
      if (fromAmount <= 0) {
        throw new ContractValidateException("from_amount must be greater than 0");
      }
      if (balance < fromAmount) {
        throw new ContractValidateException(
            "Validate ZenTransferContract error, balance is not sufficient");
      }
    }

    if (hasTransparentTo) {
      if (toAmount <= 0) {
        throw new ContractValidateException("to_amount must be greater than 0");
      }
      AccountCapsule toAccount = dbManager.getAccountStore().get(toAddress);
      if (toAccount != null) {
        try {
          Math.addExact(getZenBalance(toAccount), toAmount);
        } catch (ArithmeticException e) {
          logger.debug(e.getMessage(), e);
          throw new ContractValidateException(e.getMessage());
        }
      }
    }
  }

  private long getZenBalance(AccountCapsule account) {
    if (account.getAssetMapV2().get(zenTokenId) == null) {
      return 0L;
    } else {
      return account.getAssetMapV2().get(zenTokenId);
    }
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return null;
  }

  @Override
  public long calcFee() {
    long fee = 0;
    fee += dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    return fee;
  }
}
