package org.tron.core.capsule;


import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.protos.Protocol.AccountBalance;
import org.tron.protos.Protocol.AccountType;

@Slf4j(topic = "capsule")
public class AccountBalanceCapsule implements ProtoCapsule<AccountBalance>, Comparable<AccountBalanceCapsule> {

    private AccountBalance accountBalance;

    /**
     * get accountBalance from bytes data.
     */
    public AccountBalanceCapsule(byte[] data) {
        try {
            this.accountBalance = AccountBalance.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage());
        }
    }

    /**
     * initial accountBalance capsule.
     */
    public AccountBalanceCapsule(ByteString address,
                                 long balance, AccountType type) {
        this.accountBalance = AccountBalance.newBuilder()
                .setAddress(address)
                .setBalance(balance)
                .setType(type)
                .build();
    }

    /**
     * get account from address.
     */
    public AccountBalanceCapsule(ByteString address,
                          AccountType accountType,
                          boolean withDefaultPermission) {
        if (withDefaultPermission) {
            this.accountBalance = AccountBalance.newBuilder()
                    .setAddress(address)
                    .setType(accountType)
                    .build();
        } else {
            this.accountBalance = AccountBalance.newBuilder()
                    .setAddress(address)
                    .build();
        }

    }

    public AccountBalanceCapsule(ByteString address, AccountType accountType,
                                 long balance) {
        this.accountBalance = AccountBalance.newBuilder()
                .setAddress(address)
                .setBalance(balance)
                .setType(accountType)
                .build();
    }

    @Override
    public int compareTo(AccountBalanceCapsule otherObject) {
        return Long.compare(otherObject.getBalance(), this.getBalance());
    }

    @Override
    public byte[] getData() {
        return this.accountBalance.toByteArray();
    }

    @Override
    public AccountBalance getInstance() {
        return this.accountBalance;
    }


    public long getBalance() {
        return this.accountBalance.getBalance();
    }

    public void setBalance(long balance) {
        this.accountBalance = this.accountBalance.toBuilder().setBalance(balance).build();
    }

    public ByteString getAddress() {
        return this.accountBalance.getAddress();
    }

    public byte[] createDbKey() {
        return getAddress().toByteArray();
    }

    public AccountType getType() {
        return this.accountBalance.getType();
    }


}
