package org.tron.core.actuator;

import static org.tron.core.zen.note.ZenChainParams.ZC_ENCCIPHERTEXT_SIZE;
import static org.tron.core.zen.note.ZenChainParams.ZC_OUTCIPHERTEXT_SIZE;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.tron.common.utils.Commons;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.zksnark.JLibrustzcash;
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
import org.tron.core.exception.ZkProofValidateException;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.zen.merkle.IncrementalMerkleTreeContainer;
import org.tron.core.zen.merkle.MerkleContainer;
import org.tron.protos.Contract.ReceiveDescription;
import org.tron.protos.Contract.ShieldedTransferContract;
import org.tron.protos.Contract.SpendDescription;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.code;


@Slf4j(topic = "actuator")
public class ShieldedTransferActuator extends AbstractActuator {

  private TransactionCapsule tx;
  private ShieldedTransferContract shieldedTransferContract;
  public static final String zenTokenId = Args.getInstance().getZenTokenId();

  ShieldedTransferActuator(Any contract, Manager dbManager, TransactionCapsule tx) {
    super(contract, dbManager);
    this.tx = tx;
  }

  @Override
  public boolean execute(TransactionResultCapsule ret)
      throws ContractExeException {
    long fee = 0;
    long shieldedTransactionFee = calcFee();

    try {
      shieldedTransferContract = contract.unpack(ShieldedTransferContract.class);
      if (shieldedTransferContract.getTransparentFromAddress().toByteArray().length > 0) {
        executeTransparentFrom(shieldedTransferContract.getTransparentFromAddress().toByteArray(),
            shieldedTransferContract.getFromAmount(), ret);
      }
      dbManager.adjustAssetBalanceV2(dbManager.getAccountStore().getBlackhole().createDbKey(),
          zenTokenId, shieldedTransactionFee);
    } catch (BalanceInsufficientException|InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      ret.setShieldedTransactionFee(shieldedTransactionFee);
      throw new ContractExeException(e.getMessage());
    }

    executeShielded(shieldedTransferContract.getSpendDescriptionList(),
        shieldedTransferContract.getReceiveDescriptionList(), ret);

    if (shieldedTransferContract.getTransparentToAddress().toByteArray().length > 0) {
      executeTransparentTo(shieldedTransferContract.getTransparentToAddress().toByteArray(),
          shieldedTransferContract.getToAmount(), ret);
    }

    //adjust and verify total shielded pool value
    try {
      dbManager.adjustTotalShieldedPoolValue(
          Math.addExact(Math.subtractExact(shieldedTransferContract.getToAmount(),
              shieldedTransferContract.getFromAmount()), shieldedTransactionFee));
    } catch (ArithmeticException|BalanceInsufficientException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      ret.setShieldedTransactionFee(shieldedTransactionFee);
      throw new ContractExeException(e.getMessage());
    }

    ret.setStatus(fee, code.SUCESS);
    ret.setShieldedTransactionFee(shieldedTransactionFee);
    return true;
  }

  private void executeTransparentFrom(byte[] ownerAddress, long amount,
      TransactionResultCapsule ret)
      throws ContractExeException {
    try {
      dbManager.adjustAssetBalanceV2(ownerAddress, zenTokenId, -amount);
    } catch (BalanceInsufficientException e) {
      ret.setStatus(0, code.FAILED);
      ret.setShieldedTransactionFee(calcFee());
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
      ret.setStatus(0, code.FAILED);
      ret.setShieldedTransactionFee(calcFee());
      throw new ContractExeException(e.getMessage());
    }
  }

  //record shielded transaction data.
  private void executeShielded(List<SpendDescription> spends, List<ReceiveDescription> receives,
      TransactionResultCapsule ret)
      throws ContractExeException {

    long fee = 0;
    long shieldedTransactionFee = calcFee();

    //handle spends
    for (SpendDescription spend : spends) {
      if (dbManager.getNullfierStore().has(
          new BytesCapsule(spend.getNullifier().toByteArray()).getData())) {
        ret.setStatus(fee, code.FAILED);
        ret.setShieldedTransactionFee(shieldedTransactionFee);
        throw new ContractExeException("double spend");
      }
      dbManager.getNullfierStore().put(new BytesCapsule(spend.getNullifier().toByteArray()));
    }
    if (Args.getInstance().isFullNodeAllowShieldedTransaction()) {
      MerkleContainer merkleContainer = dbManager.getMerkleContainer();
      IncrementalMerkleTreeContainer currentMerkle = merkleContainer.getCurrentMerkle();
      try {
        currentMerkle.wfcheck();
      } catch (ZksnarkException e) {
        ret.setStatus(fee, code.FAILED);
        ret.setShieldedTransactionFee(shieldedTransactionFee);
        throw new ContractExeException(e.getMessage());
      }
      //handle receives
      for (ReceiveDescription receive : receives) {
        try {
          merkleContainer
              .saveCmIntoMerkleTree(currentMerkle, receive.getNoteCommitment().toByteArray());
        } catch (ZksnarkException e) {
          ret.setStatus(fee, code.FAILED);
          ret.setShieldedTransactionFee(shieldedTransactionFee);
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
      shieldedTransferContract = contract.unpack(ShieldedTransferContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    if (dbManager.getDynamicPropertiesStore().getAllowSameTokenName() != 1) {
      throw new ContractValidateException("shielded transaction is not allowed before "
          + "ALLOW_SAME_TOKEN_NAME is opened by the committee");
    }

    if (!dbManager.getDynamicPropertiesStore().supportShieldedTransaction()) {
      throw new ContractValidateException("Not support Shielded Transaction, need to be opened by"
          + " the committee");
    }

    //transparent verification
    checkSender(shieldedTransferContract);
    checkReceiver(shieldedTransferContract);
    validateTransparent(shieldedTransferContract);

    List<SpendDescription> spendDescriptions = shieldedTransferContract.getSpendDescriptionList();
    // check duplicate sapling nullifiers
    if (CollectionUtils.isNotEmpty(spendDescriptions)) {
      HashSet<ByteString> nfSet = new HashSet<>();
      for (SpendDescription spendDescription : spendDescriptions) {
        if (nfSet.contains(spendDescription.getNullifier())) {
          throw new ContractValidateException("duplicate sapling nullifiers in this transaction");
        }
        nfSet.add(spendDescription.getNullifier());
        if (Args.getInstance().isFullNodeAllowShieldedTransaction() && !dbManager
            .getMerkleContainer()
            .merkleRootExist(spendDescription.getAnchor().toByteArray())) {
          throw new ContractValidateException("Rt is invalid.");
        }
        if (dbManager.getNullfierStore().has(spendDescription.getNullifier().toByteArray())) {
          throw new ContractValidateException("note has been spend in this transaction");
        }
      }
    }

    List<ReceiveDescription> receiveDescriptions = shieldedTransferContract
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

    //check spendProofs receiveProofs and Binding sign hash
    try {
      checkProof(spendDescriptions, receiveDescriptions);
    } catch (ZkProofValidateException e) {
      if (e.isFirstValidated()) {
        recordProof(tx.getTransactionId(), false);
      }
      throw e;
    }

    return true;
  }

  private void checkProof(List<SpendDescription> spendDescriptions,
      List<ReceiveDescription> receiveDescriptions) throws ZkProofValidateException {

    if (dbManager.getProofStore().has(tx.getTransactionId().getBytes())) {
      if (dbManager.getProofStore().get(tx.getTransactionId().getBytes())) {
        return;
      } else {
        throw new ZkProofValidateException("record is fail, skip proof", false);
      }
    }

    long shieldedTransactionFee = calcFee();
    byte[] signHash = TransactionCapsule.getShieldTransactionHashIgnoreTypeException(tx);

    if (CollectionUtils.isNotEmpty(spendDescriptions)
        || CollectionUtils.isNotEmpty(receiveDescriptions)) {
      long ctx = JLibrustzcash.librustzcashSaplingVerificationCtxInit();
      try {
        for (SpendDescription spendDescription : spendDescriptions) {
          if (!JLibrustzcash.librustzcashSaplingCheckSpend(
              new CheckSpendParams(ctx,
                  spendDescription.getValueCommitment().toByteArray(),
                  spendDescription.getAnchor().toByteArray(),
                  spendDescription.getNullifier().toByteArray(),
                  spendDescription.getRk().toByteArray(),
                  spendDescription.getZkproof().toByteArray(),
                  spendDescription.getSpendAuthoritySignature().toByteArray(),
                  signHash)
          )) {
            throw new ZkProofValidateException("librustzcashSaplingCheckSpend error", true);
          }
        }

        for (ReceiveDescription receiveDescription : receiveDescriptions) {
          if (receiveDescription.getCEnc().size() != ZC_ENCCIPHERTEXT_SIZE
              || receiveDescription.getCOut().size() != ZC_OUTCIPHERTEXT_SIZE) {
            throw new ZkProofValidateException("Cout or CEnc size error", true);
          }
          if (!JLibrustzcash.librustzcashSaplingCheckOutput(
              new CheckOutputParams(ctx,
                  receiveDescription.getValueCommitment().toByteArray(),
                  receiveDescription.getNoteCommitment().toByteArray(),
                  receiveDescription.getEpk().toByteArray(),
                  receiveDescription.getZkproof().toByteArray())
          )) {
            throw new ZkProofValidateException("librustzcashSaplingCheckOutput error", true);
          }
        }

        long valueBalance;
        long totalShieldedPoolValue = dbManager.getDynamicPropertiesStore()
            .getTotalShieldedPoolValue();
        try {
          valueBalance = Math.addExact(Math.subtractExact(shieldedTransferContract.getToAmount(),
              shieldedTransferContract.getFromAmount()), shieldedTransactionFee);
          totalShieldedPoolValue = Math.subtractExact(totalShieldedPoolValue, valueBalance);
        } catch (ArithmeticException e) {
          logger.debug(e.getMessage(), e);
          throw new ZkProofValidateException(e.getMessage(), true);
        }

        if (totalShieldedPoolValue < 0) {
          throw new ZkProofValidateException("shieldedPoolValue error", true);
        }

        if (!JLibrustzcash.librustzcashSaplingFinalCheck(
            new FinalCheckParams(ctx,
                valueBalance,
                shieldedTransferContract.getBindingSignature().toByteArray(),
                signHash)
        )) {
          throw new ZkProofValidateException("librustzcashSaplingFinalCheck error", true);
        }
      } catch (ZksnarkException e) {
        throw new ZkProofValidateException(e.getMessage(), true);
      } finally {
        JLibrustzcash.librustzcashSaplingVerificationCtxFree(ctx);
      }
    }

    recordProof(tx.getTransactionId(), true);
  }

  private void recordProof(Sha256Hash tid, boolean result) {
    dbManager.getProofStore().put(tid.getBytes(), result);
  }


  private void checkSender(ShieldedTransferContract shieldedTransferContract)
      throws ContractValidateException {
    if (!shieldedTransferContract.getTransparentFromAddress().isEmpty()
        && shieldedTransferContract.getSpendDescriptionCount() > 0) {
      throw new ContractValidateException("ShieldedTransferContract error, more than 1 senders");
    }
    if (shieldedTransferContract.getTransparentFromAddress().isEmpty()
        && shieldedTransferContract.getSpendDescriptionCount() == 0) {
      throw new ContractValidateException("ShieldedTransferContract error, no sender");
    }
    if (shieldedTransferContract.getSpendDescriptionCount() > 1) {
      throw new ContractValidateException("ShieldedTransferContract error, number of spend notes"
          + " should not be more than 1");
    }
  }

  private void checkReceiver(ShieldedTransferContract shieldedTransferContract)
      throws ContractValidateException {
    if (shieldedTransferContract.getReceiveDescriptionCount() == 0) {
      throw new ContractValidateException("ShieldedTransferContract error, no output cm");
    }
    if (shieldedTransferContract.getReceiveDescriptionCount() > 2) {
      throw new ContractValidateException("ShieldedTransferContract error, number of receivers"
          + " should not be more than 2");
    }
  }

  private void validateTransparent(ShieldedTransferContract shieldedTransferContract)
      throws ContractValidateException {
    boolean hasTransparentFrom;
    boolean hasTransparentTo;
    byte[] toAddress = shieldedTransferContract.getTransparentToAddress().toByteArray();
    byte[] ownerAddress = shieldedTransferContract.getTransparentFromAddress().toByteArray();

    hasTransparentFrom = (ownerAddress.length > 0);
    hasTransparentTo = (toAddress.length > 0);

    long fromAmount = shieldedTransferContract.getFromAmount();
    long toAmount = shieldedTransferContract.getToAmount();
    if (fromAmount < 0) {
      throw new ContractValidateException("from_amount should not be less than 0");
    }
    if (toAmount < 0) {
      throw new ContractValidateException("to_amount should not be less than 0");
    }

    if (hasTransparentFrom && !Commons.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid transparent_from_address");
    }
    if (!hasTransparentFrom && fromAmount != 0) {
      throw new ContractValidateException("no transparent_from_address, from_amount should be 0");
    }
    if (hasTransparentTo && !Commons.addressValid(toAddress)) {
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
        throw new ContractValidateException("Validate ShieldedTransferContract error, "
            + "no OwnerAccount");
      }
      long balance = getZenBalance(ownerAccount);
      if (fromAmount <= 0) {
        throw new ContractValidateException("from_amount must be greater than 0");
      }
      if (balance < fromAmount) {
        throw new ContractValidateException(
            "Validate ShieldedTransferContract error, balance is not sufficient");
      }
      if (fromAmount <= calcFee()) {
        throw new ContractValidateException(
            "Validate ShieldedTransferContract error, fromAmount should be great than fee");
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
    ByteString owner = contract.unpack(ShieldedTransferContract.class).getTransparentFromAddress();
    if (Commons.addressValid(owner.toByteArray())) {
      return owner;
    } else {
      return null;
    }
  }

  @Override
  public long calcFee() {
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    return fee;
  }
}
