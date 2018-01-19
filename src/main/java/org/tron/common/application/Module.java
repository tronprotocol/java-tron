/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tron.common.application;

import static org.tron.core.Constant.BLOCK_DB_NAME;
import static org.tron.core.Constant.TRANSACTION_DB_NAME;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import javax.inject.Named;
import org.tron.core.consensus.client.Client;
import org.tron.core.consensus.server.Server;
import org.tron.core.Blockchain;
import org.tron.core.Constant;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;

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
