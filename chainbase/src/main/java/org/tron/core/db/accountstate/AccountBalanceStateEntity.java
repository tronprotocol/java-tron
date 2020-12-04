package org.tron.core.db.accountstate;


import lombok.extern.slf4j.Slf4j;
import static org.tron.common.utils.WalletUtil.encode58Check;
import org.tron.protos.Protocol.AccountBalance;

@Slf4j(topic = "AccountBalanceStateEntity")
public class AccountBalanceStateEntity {

    private AccountBalance accountBalance;

    public AccountBalanceStateEntity() {
    }

    public AccountBalanceStateEntity(AccountBalance account) {
        AccountBalance.Builder builder = AccountBalance.newBuilder();
        builder.setAddress(account.getAddress());
        builder.setBalance(account.getBalance());
        //builder.putAllAssetV2(account.getAssetV2Map());
        builder.setAllowance(account.getAllowance());
        this.accountBalance = builder.build();
    }

    public static AccountBalanceStateEntity parse(byte[] data) {
        try {
            return new AccountBalanceStateEntity().setAccount(AccountBalance.parseFrom(data));
        } catch (Exception e) {
            logger.error("parse to AccountBalanceStateEntity error! reason: {}", e.getMessage());
        }
        return null;
    }

    public AccountBalance getAccount() {
        return accountBalance;
    }

    public AccountBalanceStateEntity setAccount(AccountBalance accountBalance) {
        this.accountBalance = accountBalance;
        return this;
    }

    public byte[] toByteArrays() {
        return accountBalance.toByteArray();
    }

    @Override
    public String toString() {
        return "address:" + encode58Check(accountBalance.getAddress().toByteArray()) + "; "
                + accountBalance
                .toString();
    }

}
