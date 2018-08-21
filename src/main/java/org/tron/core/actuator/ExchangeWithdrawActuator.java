package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.StringUtil;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.ExchangeCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.Parameter.ChainParameters;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.protos.Contract.ExchangeWithdrawContract;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j
public class ExchangeWithdrawActuator extends AbstractActuator {

  ExchangeWithdrawActuator(final Any contract, final Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
    long fee = calcFee();
    try {
      final ExchangeWithdrawContract exchangeWithdrawContract = this.contract
          .unpack(ExchangeWithdrawContract.class);
      AccountCapsule accountCapsule = dbManager.getAccountStore()
          .get(exchangeWithdrawContract.getOwnerAddress().toByteArray());

      ExchangeCapsule exchangeCapsule = dbManager.getExchangeStore().
          get(ByteArray.fromLong(exchangeWithdrawContract.getExchangeId()));

      byte[] firstTokenID = exchangeCapsule.getFirstTokenId();
      byte[] secondTokenID = exchangeCapsule.getSecondTokenId();
      long firstTokenBalance = exchangeCapsule.getFirstTokenBalance();
      long secondTokenBalance = exchangeCapsule.getSecondTokenBalance();

      byte[] tokenID = exchangeWithdrawContract.getTokenId().toByteArray();
      long tokenQuant = exchangeWithdrawContract.getQuant();

      byte[] anotherTokenID;
      long anotherTokenQuant;

      if (Arrays.equals(tokenID, firstTokenID)) {
        anotherTokenID = secondTokenID;
        anotherTokenQuant = Math
            .floorDiv(Math.multiplyExact(secondTokenBalance, tokenQuant), firstTokenBalance);
        exchangeCapsule.setBalance(firstTokenBalance - tokenQuant,
            secondTokenBalance - anotherTokenQuant);
      } else {
        anotherTokenID = firstTokenID;
        anotherTokenQuant = Math
            .floorDiv(Math.multiplyExact(firstTokenBalance, tokenQuant), secondTokenBalance);
        exchangeCapsule.setBalance(firstTokenBalance - anotherTokenQuant,
            secondTokenBalance - tokenQuant);
      }

      long newBalance = accountCapsule.getBalance() - calcFee();

      if (tokenID == "_".getBytes()) {
        accountCapsule.setBalance(newBalance + tokenQuant);
      } else {
        accountCapsule.addAssetAmount(tokenID, tokenQuant);
      }

      if (anotherTokenID == "_".getBytes()) {
        accountCapsule.setBalance(newBalance + anotherTokenQuant);
      } else {
        accountCapsule.addAssetAmount(anotherTokenID, anotherTokenQuant);
      }

      dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
      dbManager.getExchangeStore().put(exchangeCapsule.createDbKey(), exchangeCapsule);

      ret.setStatus(fee, code.SUCESS);
    } catch (ItemNotFoundException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
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
    if (!this.contract.is(ExchangeWithdrawContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [ExchangeWithdrawContract],real type[" + contract
              .getClass() + "]");
    }
    final ExchangeWithdrawContract contract;
    try {
      contract = this.contract.unpack(ExchangeWithdrawContract.class);
    } catch (InvalidProtocolBufferException e) {
      throw new ContractValidateException(e.getMessage());
    }

    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
    String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);

    if (!Wallet.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }

    if (!this.dbManager.getAccountStore().has(ownerAddress)) {
      throw new ContractValidateException("account[" + readableOwnerAddress + "] not exists");
    }

    AccountCapsule accountCapsule = this.dbManager.getAccountStore().get(ownerAddress);

    if (accountCapsule.getBalance() < calcFee()) {
      throw new ContractValidateException("No enough balance for exchange withdraw fee!");
    }

    ExchangeCapsule exchangeCapsule;
    try {
      exchangeCapsule = dbManager.getExchangeStore().
          get(ByteArray.fromLong(contract.getExchangeId()));
    } catch (ItemNotFoundException ex) {
      throw new ContractValidateException("Exchange[" + contract.getExchangeId() + "] not exists");
    }

    if (!accountCapsule.getAddress().equals(exchangeCapsule.getCreatorAddress())) {
      throw new ContractValidateException("account[" + readableOwnerAddress + "] is not creator");
    }

    byte[] firstTokenID = exchangeCapsule.getFirstTokenId();
    byte[] secondTokenID = exchangeCapsule.getSecondTokenId();
    long firstTokenBalance = exchangeCapsule.getFirstTokenBalance();
    long secondTokenBalance = exchangeCapsule.getSecondTokenBalance();

    byte[] tokenID = contract.getTokenId().toByteArray();
    long tokenQuant = contract.getQuant();

    long anotherTokenQuant;

    if (!Arrays.equals(tokenID, firstTokenID) && !Arrays.equals(tokenID, secondTokenID)) {
      throw new ContractValidateException("token is not in exchange");
    }

    if (tokenQuant <= 0) {
      throw new ContractValidateException("withdraw token balance must greater than zero");
    }

    if (firstTokenBalance == 0 || secondTokenBalance == 0) {
      throw new ContractValidateException("Token balance in exchange is equal with 0,the exchange has been closed");
    }

    if (Arrays.equals(tokenID, firstTokenID)) {
      anotherTokenQuant = Math
          .floorDiv(Math.multiplyExact(secondTokenBalance, tokenQuant), firstTokenBalance);
      if (firstTokenBalance < tokenQuant || secondTokenBalance < anotherTokenQuant) {
        throw new ContractValidateException("exchange balance is not enough");
      }
    } else {
      anotherTokenQuant = Math
          .floorDiv(Math.multiplyExact(firstTokenBalance, tokenQuant), secondTokenBalance);
      if (secondTokenBalance < tokenQuant || firstTokenBalance < anotherTokenQuant) {
        throw new ContractValidateException("exchange balance is not enough");
      }
    }

    return true;
  }


  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(ExchangeWithdrawContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

  private boolean validKey(long idx) {
    return idx >= 0 && idx < ChainParameters.values().length;
  }

}
