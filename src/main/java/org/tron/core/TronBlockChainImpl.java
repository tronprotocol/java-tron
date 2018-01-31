/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core;


import static org.tron.core.Constant.LAST_HASH;

import com.google.protobuf.InvalidProtocolBufferException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.core.config.SystemProperties;
import org.tron.core.db.BlockStoreInput;
import org.tron.protos.Protocal.Block;

@Component
public class TronBlockChainImpl implements TronBlockChain, org.tron.core.facade.TronBlockChain {

  private static final Logger logger = LoggerFactory.getLogger("Blockchain");
  @Autowired
  protected BlockStoreInput blockStoreInter;
  SystemProperties config = SystemProperties.getDefault();
  private BigInteger totalDifficulty = BigInteger.ZERO;

  /**
   * initDB level DB blockStoreInter
   */
  private static LevelDbDataSourceImpl initBD() {
    LevelDbDataSourceImpl levelDbDataSource = new LevelDbDataSourceImpl(Constant.NORMAL,
        "blockStoreInter");
    levelDbDataSource.initDB();
    return levelDbDataSource;
  }

  @Override
  public BlockStoreInput getBlockStoreInter() {
    return blockStoreInter;
  }

  @Override
  public BigInteger getTotalDifficulty() {
    return totalDifficulty;
  }

  @Override
  public synchronized Block getBestBlock() {
    Block bestBlock = null;
    LevelDbDataSourceImpl levelDbDataSource = initBD();
    byte[] lastHash = levelDbDataSource.getData(LAST_HASH);
    byte[] value = levelDbDataSource.getData(lastHash);
    try {
      bestBlock = Block.parseFrom(value)
          .toBuilder()
          .build();
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    return bestBlock;
  }

  public synchronized void addBlockToChain(Block block) {
    Block bestBlock = getBestBlock();

    if (bestBlock.getBlockHeader().getHash() == block.getBlockHeader()
        .getHash()) {
      byte[] blockByte = block.toByteArray();

      LevelDbDataSourceImpl levelDbDataSource = initBD();
      levelDbDataSource.putData(block.getBlockHeader().getHash()
          .toByteArray(), blockByte);

      byte[] key = LAST_HASH;

      levelDbDataSource.putData(key, block.getBlockHeader().getHash()
          .toByteArray());  // Storage lastHash

    } else {
      System.out.print("lastHash error");
    }
  }

  private void recordBlock(Block block) {
    if (!config.recordBlocks()) {
      return;
    }

    String dumpDir = config.databaseDir() + "/" + config.dumpDir();

    File dumpFile = new File(dumpDir + "/blocks-rec.dmp");
    FileWriter fw = null;
    BufferedWriter bw = null;

    try {

      dumpFile.getParentFile().mkdirs();
      if (!dumpFile.exists()) {
        dumpFile.createNewFile();
      }

      fw = new FileWriter(dumpFile.getAbsoluteFile(), true);
      bw = new BufferedWriter(fw);
      bw.write(Hex.toHexString(block.toByteArray()));
      bw.write("\n");

    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    } finally {
      try {
        if (bw != null) {
          bw.close();
        }
        if (fw != null) {
          fw.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
