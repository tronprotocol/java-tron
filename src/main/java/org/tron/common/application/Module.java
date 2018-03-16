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

import com.typesafe.config.Config;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.core.api.WalletApi;
import org.tron.core.config.Configuration;
import org.tron.core.config.args.*;
import org.tron.core.services.RpcApiService;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class Module extends AbstractModule {

  private static final Logger logger = LoggerFactory.getLogger("Module");


  private final Config config;
  private Args args;

  public Module(Config config, Args args) {
    this.config = config;
    this.args = args;
  }

  @Override
  protected void configure() {

  }

  @Provides
  @Singleton
  public Config buildConfig() {
    return this.config;
  }

  /**
   * build transaction database.
   */
  @Provides
  @Singleton
  @Named("transaction")
  public LevelDbDataSourceImpl buildTransactionDb() {
    LevelDbDataSourceImpl db = new LevelDbDataSourceImpl(Args.getInstance().getOutputDirectory(),
        TRANSACTION_DB_NAME);
    db.initDB();
    return db;
  }

  /**
   * build block database.
   */
  @Provides
  @Singleton
  @Named("block")
  public LevelDbDataSourceImpl buildBlockDb() {
    LevelDbDataSourceImpl db = new LevelDbDataSourceImpl(Args.getInstance().getOutputDirectory(), BLOCK_DB_NAME);
    db.initDB();
    return db;
  }

  @Provides
  @Singleton
  public Storage buildStorage() {
    Storage storage = new Storage();
    storage.setDirectory(Optional.ofNullable(args.getStorageDirectory())
            .filter(StringUtils::isNotEmpty)
            .orElse(config.getString("storage.directory")));
    return storage;
  }

  @Provides
  @Singleton
  public Overlay buildOverlay() {
    Overlay overlay = new Overlay();
    overlay.setPort(Optional.ofNullable(args.getOverlayPort())
            .filter(i -> 0 != i)
            .orElse(config.getInt("overlay.port")));
    return overlay;
  }

  @Provides
  @Singleton
  public SeedNode buildSeedNode() {
    SeedNode seedNode = new SeedNode();
    seedNode.setIpList(Optional.ofNullable(args.getSeedNodes())
            .filter(s -> 0 != s.size())
            .orElse(config.getStringList("seed.node.ip.list")));
    return seedNode;
  }

  @Provides
  @Singleton
  public LocalWitnesses buildLocalWitnesses() {
    LocalWitnesses localWitness = new LocalWitnesses();
    List<String> localwitness = config.getStringList("localwitness");
    if (localwitness.size() > 1) {
      logger.warn("localwitness size must be one,get the first one");
      localwitness = localwitness.subList(0, 1);
    }
    localWitness.setPrivateKeys(localwitness);
    return localWitness;
  }

  public GenesisBlock buildGenesisBlock() {
    GenesisBlock genesisBlock = new GenesisBlock();
    genesisBlock.setTimeStamp(config.getString("genesis.block.timestamp"));
    genesisBlock.setParentHash(config.getString("genesis.block.parentHash"));
    genesisBlock.setHash(config.getString("genesis.block.hash"));
    genesisBlock.setNumber(config.getString("genesis.block.number"));

    if (config.hasPath("genesis.block.assets")) {
      genesisBlock.setAssets(Configuration.getAccountsFromConfig(config));
    }
    if (config.hasPath("genesis.block.witnesses")) {
      genesisBlock.setWitnesses(Configuration.getWitnessesFromConfig(config));
    }

    return genesisBlock;
  }
}
