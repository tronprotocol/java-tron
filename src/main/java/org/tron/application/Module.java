package org.tron.application;

import static org.tron.core.Constant.BLOCK_DB_NAME;
import static org.tron.core.Constant.TRANSACTION_DB_NAME;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import javax.inject.Named;
import org.tron.consensus.client.Client;
import org.tron.consensus.server.Server;
import org.tron.core.Blockchain;
import org.tron.core.Constant;
import org.tron.storage.leveldb.LevelDbDataSourceImpl;

public class Module extends AbstractModule {

  @Override
  protected void configure() {

  }

  @Provides
  @Singleton
  public Client buildClient() {
    return new Client();
  }

  @Provides
  @Singleton
  public Server buildServer() {
    return new Server();
  }

  @Provides
  @Singleton
  @Named("transaction")
  public LevelDbDataSourceImpl buildTransactionDb() {
    LevelDbDataSourceImpl db = new LevelDbDataSourceImpl(Constant.NORMAL, TRANSACTION_DB_NAME);
    db.initDB();
    return db;
  }

  @Provides
  @Singleton
  @Named("block")
  public LevelDbDataSourceImpl buildBlockDb() {
    LevelDbDataSourceImpl db = new LevelDbDataSourceImpl(Constant.NORMAL, BLOCK_DB_NAME);
    db.initDB();
    return db;
  }

  @Provides
  @Singleton
  public Blockchain buildBlockchain(@Named("block") LevelDbDataSourceImpl blockDB) {
    return new Blockchain(blockDB);
  }
}
