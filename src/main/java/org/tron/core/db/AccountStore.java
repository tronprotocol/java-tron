package org.tron.core.db;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.protos.Protocal.Account;

public class AccountStore extends TronDatabase {

  private static final Logger logger = LoggerFactory.getLogger("AccountStore");

  private AccountStore(String dbName) {
    super(dbName);
  }

  private static AccountStore instance;

  /**
   * create fun.
   *
   * @param dbName the name of database
   */
  public static AccountStore create(String dbName) {
    if (instance == null) {
      synchronized (AccountStore.class) {
        if (instance == null) {
          instance = new AccountStore(dbName);
        }
      }
    }
    return instance;
  }

  public Account getAccount(ByteString voteAddress) {
    logger.info("voteAddress is {} ", voteAddress);

    try {
      byte[] value = dbSource.getData(voteAddress.toByteArray());
      if (null == value) {
        return null;
      }
      return Account.parseFrom(value);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    return null;
  }

  public void putAccount(ByteString voteAddress, Account account) {
    logger.info("voteAddress is {} ", voteAddress);

    dbSource.putData(voteAddress.toByteArray(), account.toByteArray());
  }

  @Override
  void add() {

  }

  @Override
  void del() {

  }

  @Override
  void fetch() {

  }

  public boolean createAccount(byte[] address, byte[] account) {
    dbSource.putData(address, account);
    logger.info("address is {},account is {}", address, account);
    return true;
  }

  public boolean isAccountExist(byte[] address) {
    byte[] account = dbSource.getData(address);
    logger.info("address is {},account is {}", address, account);
    return null != account;
  }
  
}
