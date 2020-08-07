package org.tron.core.vm.nativecontract;

import org.tron.common.utils.ByteArray;
import org.tron.common.utils.DecodeUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.utils.TransactionUtil;
import org.tron.core.vm.nativecontract.param.TokenIssueParam;
import org.tron.core.vm.repository.Repository;

import java.util.Objects;

import static org.tron.core.vm.nativecontract.ContractProcessorConstant.*;

public class TokenIssueProcessor implements IContractProcessor {

    private TokenIssueProcessor(){}

    public static TokenIssueProcessor getInstance(){
        return TokenIssueProcessor.Singleton.INSTANCE.getInstance();
    }

    private enum Singleton {
        INSTANCE;
        private TokenIssueProcessor instance;
        Singleton() {
            instance = new TokenIssueProcessor();
        }
        public TokenIssueProcessor getInstance() {
            return instance;
        }
    }

    @Override
    public boolean execute(Object contract, Repository repository) throws ContractExeException {
        TokenIssueParam tokenIssueParam = (TokenIssueParam) contract;
        long tokenIdNum = repository.getTokenIdNum();
        tokenIdNum++;
        repository.saveTokenIdNum(tokenIdNum);
        AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(tokenIssueParam.getOwnerAddress(),
                Long.toString(tokenIdNum), ByteArray.toStr(tokenIssueParam.getName()),
                ByteArray.toStr(tokenIssueParam.getAbbr()), tokenIssueParam.getTotalSupply(),
                tokenIssueParam.getPrecision());
        AssetIssueCapsule assetIssueCapsuleV2 = new AssetIssueCapsule(tokenIssueParam.getOwnerAddress(),
                Long.toString(tokenIdNum), ByteArray.toStr(tokenIssueParam.getName()),
                ByteArray.toStr(tokenIssueParam.getAbbr()), tokenIssueParam.getTotalSupply(),
                        tokenIssueParam.getPrecision());
        if (repository.getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
            assetIssueCapsuleV2.setPrecision(0);
            repository.putAssetIssueValue(assetIssueCapsule.createDbKey(), assetIssueCapsule);
            repository.putAssetIssueValue(assetIssueCapsuleV2.createDbV2Key(), assetIssueCapsuleV2);
        } else {
            repository.putAssetIssueValue(assetIssueCapsuleV2.createDbV2Key(), assetIssueCapsuleV2);
        }
        AccountCapsule accountCapsule = repository.getAccount(tokenIssueParam.getOwnerAddress());
        if (repository.getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
            accountCapsule
                    .addAsset(assetIssueCapsule.createDbKey(), tokenIssueParam.getTotalSupply());
        }
        accountCapsule.setAssetIssuedName(assetIssueCapsule.createDbKey());
        accountCapsule.setAssetIssuedID(assetIssueCapsule.createDbV2Key());
        accountCapsule
                .addAssetV2(assetIssueCapsuleV2.createDbV2Key(), tokenIssueParam.getTotalSupply());
        accountCapsule.setInstance(accountCapsule.getInstance().toBuilder().build());
        repository.putAccountValue(tokenIssueParam.getOwnerAddress(), accountCapsule);
        // spend 1024trx for assetissue
        accountCapsule.setBalance(accountCapsule.getBalance()-TOKEN_ISSUE_FEE);
        repository.updateAccount(tokenIssueParam.getOwnerAddress(), accountCapsule);
        return true;
    }

    @Override
    public boolean validate(Object contract, Repository repository) throws ContractValidateException {
        if (!(contract instanceof TokenIssueParam)) {
            throw new ContractValidateException(
                    "contract type error,expected type [TokenIssuedContract],real type[" + contract
                            .getClass() + "]");
        }
        TokenIssueParam tokenIssueParam = (TokenIssueParam) contract;
        if (Objects.isNull(tokenIssueParam)) {
            throw new ContractValidateException(CONTRACT_NULL);
        }

        if (repository == null) {
            throw new ContractValidateException(STORE_NOT_EXIST);
        }

        if (!DecodeUtil.addressValid(tokenIssueParam.getOwnerAddress())) {
            throw new ContractValidateException("Invalid ownerAddress");
        }

        if (!TransactionUtil.validAssetName(tokenIssueParam.getName())) {
            throw new ContractValidateException("Invalid assetName");
        }
        if ((TRX.equals(tokenIssueParam.getName().toString().toLowerCase()))) {
            throw new ContractValidateException("assetName can't be trx");
        }
        if (tokenIssueParam.getPrecision() < 0 || tokenIssueParam.getPrecision() > 6) {
            throw new ContractValidateException("precision cannot exceed 6");
        }

        if (Objects.nonNull(tokenIssueParam.getAbbr()) && !TransactionUtil.validAssetName(tokenIssueParam.getAbbr())) {
            throw new ContractValidateException("Invalid abbreviation for token");
        }

        if (tokenIssueParam.getTotalSupply() <= 0) {
            throw new ContractValidateException("TotalSupply must greater than 0!");
        }

        AccountCapsule accountCapsule = repository.getAccount(tokenIssueParam.getOwnerAddress());
        if (accountCapsule == null) {
            throw new ContractValidateException("Account not exists");
        }
        if(accountCapsule.getBalance() < TOKEN_ISSUE_FEE) {
            throw new ContractValidateException("Account insufficient balance");
        }
        if (!accountCapsule.getAssetIssuedName().isEmpty()) {
            throw new ContractValidateException("An account can only issue one asset");
        }

        if (accountCapsule.getBalance() < repository.getDynamicPropertiesStore().getAssetIssueFee()) {
            throw new ContractValidateException("No enough balance for fee!");
        }
        return true;
    }
}
