package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.db.AssetIssueStore;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.AccountUpdateContract;
import org.tron.protos.Contract.UpdateFreeAssetNetLimitContract;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j
public class UpdateFreeAssetNetLimitActuator extends AbstractActuator {

  private UpdateFreeAssetNetLimitContract updateFreeAssetNetLimitContract;
  private long newLimit;
  private byte[] ownerAddress;
  private long fee;

  UpdateFreeAssetNetLimitActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
    try {
      updateFreeAssetNetLimitContract = contract.unpack(UpdateFreeAssetNetLimitContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.error(e.getMessage(), e);
    }
    newLimit = updateFreeAssetNetLimitContract.getNewLimit();
    ownerAddress = updateFreeAssetNetLimitContract.getOwnerAddress().toByteArray();
    fee = calcFee();
  }

  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
    try {
      AssetIssueStore assetIssueStore = dbManager.getAssetIssueStore();
      AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
      AssetIssueCapsule assetIssueCapsule =
          assetIssueStore.get(accountCapsule.getAssetIssuedName().toByteArray());

      assetIssueCapsule.setFreeAssetNetLimit(newLimit);
      assetIssueStore.put(assetIssueCapsule.createDbKey(), assetIssueCapsule);

      ret.setStatus(fee, code.SUCESS);
    } catch (Exception e) {
      logger.debug(e.getMessage(),e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }

    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (this.dbManager == null) {
      throw new ContractValidateException("No dbManager!");
    }
    if (updateFreeAssetNetLimitContract == null) {
      throw new ContractValidateException(
          "contract type error,expected type [UpdateFreeAssetNetLimitContract],real type["
              + contract.getClass() + "]");
    }

    if (!Wallet.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalidate ownerAddress");
    }

    AccountCapsule account = dbManager.getAccountStore().get(ownerAddress);
    if (account == null) {
      throw new ContractValidateException("Account has not existed");
    }

    if (account.getAssetIssuedName().isEmpty()) {
      throw new ContractValidateException("Account has not issue any asset");
    }

    assert (dbManager.getAssetIssueStore().get(account.getAssetIssuedName().toByteArray()) != null);

    if (newLimit < 0 || newLimit>= ChainConstant.ONE_DAY_NET_LIMIT) {
      throw new ContractValidateException("Invalid FreeAssetNetLimit");
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(AccountUpdateContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }
}
