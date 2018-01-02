package org.tron.core;


import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.config.SystemProperties;
import org.tron.dbStore.BlockStoreInter;
import org.tron.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.protos.core.TronBlock;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;

import static org.tron.core.Constant.LAST_HASH;

@Component
public class TronBlockChainImpl implements TronBlockChain, org.tron.facade
        .TronBlockChain {

    private static final Logger logger = LoggerFactory.getLogger("blockchain");

    SystemProperties config = SystemProperties.getDefault();

    @Autowired
    protected BlockStoreInter blockStoreInter;

    //private static TronBlock.Block bestBlock;

    private BigInteger totalDifficulty = BigInteger.ZERO;

    @Override
    public BlockStoreInter getBlockStoreInter() {
        return blockStoreInter;
    }


    @Override
    public BigInteger getTotalDifficulty() {
        return totalDifficulty;
    }


    // getData the last block
    @Override
    public synchronized TronBlock.Block getBestBlock() {
        TronBlock.Block bestBlock = null;
        LevelDbDataSourceImpl levelDbDataSource = initBD();
        byte[] lastHash = levelDbDataSource.getData(LAST_HASH);
        byte[] value = levelDbDataSource.getData(lastHash);
        try {
            bestBlock = TronBlock.Block.parseFrom(value)
                    .toBuilder()
                    .build();
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        return bestBlock;
    }

    // *******
    public synchronized void addBlockToChain(TronBlock.Block block) {
        TronBlock.Block bestBlock = getBestBlock();

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

    // initDB level DB blockStoreInter
    private static LevelDbDataSourceImpl initBD() {
        LevelDbDataSourceImpl levelDbDataSource = new LevelDbDataSourceImpl
                ("blockStoreInter");
        levelDbDataSource.initDB();
        return levelDbDataSource;
    }

    private void recordBlock(TronBlock.Block block) {
        if (!config.recordBlocks()) return;

        String dumpDir = config.databaseDir() + "/" + config.dumpDir();

        File dumpFile = new File(dumpDir + "/blocks-rec.dmp");
        FileWriter fw = null;
        BufferedWriter bw = null;

        try {

            dumpFile.getParentFile().mkdirs();
            if (!dumpFile.exists()) dumpFile.createNewFile();

            fw = new FileWriter(dumpFile.getAbsoluteFile(), true);
            bw = new BufferedWriter(fw);

//            if (bestBlock.isGenesis()) {
//                bw.write(Hex.toHexString(bestBlock.toByteArray()));
//                bw.write("\n");
//            }

            bw.write(Hex.toHexString(block.toByteArray()));
            bw.write("\n");

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            try {
                if (bw != null) bw.close();
                if (fw != null) fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
