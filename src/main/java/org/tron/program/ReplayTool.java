package org.tron.program;

import java.nio.file.Paths;
import java.util.Iterator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.BlockStore;
import org.tron.core.db.Manager;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.UnLinkedBlockException;
import org.tron.core.exception.ValidateSignatureException;


@Slf4j
public class ReplayTool {

  public static void cleanDb(String dataBaseDir) {
    dataBaseDir += "/database";
    String[] dbs = new String[]{
        "account",
        "asset-issue",
        "block-index",
        "block_KDB",
        "peers",
        "trans",
        "utxo",
        "witness",
        "witness_schedule",
        "nodeId.properties"
    };

    for (String db : dbs) {
      System.out.println(Paths.get(dataBaseDir, db).toString());
      FileUtil.recursiveDelete(Paths.get(dataBaseDir, db).toString());
    }
  }

  public static void main(String[] args) throws BadBlockException {
    Args.setParam(args, Constant.TESTNET_CONF);
    Args cfgArgs = Args.getInstance();

    String dataBaseDir = cfgArgs.getOutputDirectory();
    cleanDb(dataBaseDir);


    ApplicationContext context = new AnnotationConfigApplicationContext(DefaultConfig.class);
    Manager dbManager = context.getBean(Manager.class);

    if (cfgArgs.getReplayTo() > 0) {
      replayBlock(dbManager, cfgArgs.getReplayTo());
    } else {
      replayBlock(dbManager);
    }
    System.exit(0);
  }


  public static void replayBlock(Manager dbManager) throws BadBlockException {
    long replayTo = dbManager.getDynamicPropertiesStore()
        .getLatestSolidifiedBlockNum();

    replayBlock(dbManager, replayTo);
  }


  public static void replayBlock(Manager dbManager, long replayTo) throws BadBlockException {
    BlockStore localBlockStore = dbManager.getBlockStore();

    logger.info("To replay block to:" + replayTo);

    dbManager.resetToGenesisBlock();
    Iterator iterator = localBlockStore.iterator();
    long replayIndex = 0;

    logger.info("Replay solidified block start");
    while (replayIndex <= replayTo && iterator.hasNext()) {
      BlockCapsule blockCapsule = (BlockCapsule) iterator.next();
      if (replayIndex == 0) {
        // skip Genesis Block
        replayIndex++;
        continue;
      }
      try {
        dbManager.replayBlock(blockCapsule);
        dbManager.getDynamicPropertiesStore()
            .saveLatestSolidifiedBlockNum(replayIndex);
        logger.info(String.format("replay block %d success", replayIndex));
        replayIndex++;
      } catch (ValidateSignatureException e) {
        throw new BadBlockException("validate signature exception");
      } catch (ContractValidateException e) {
        throw new BadBlockException("validate contract exception");
      } catch (UnLinkedBlockException e) {
        throw new BadBlockException("validate unlink exception");
      } catch (ContractExeException e) {
        throw new BadBlockException("validate contract exe exception");
      }
    }

    logger.info("delete non-solidified block start");
    if (replayIndex != 1L) {
      while (iterator.hasNext()) {
        BlockCapsule blockCapsule = (BlockCapsule) iterator.next();
        logger.info("delete block:" + blockCapsule.toString());
        dbManager.getBlockStore().delete(blockCapsule.getBlockId().getBytes());
      }
    }
    logger.info("Delete non-solidified block complete");

    logger.info("Replay solidified block complete");
    logger.info("Local LatestSolidifiedBlockNum:" + replayTo);
    logger.info("Current LatestSolidifiedBlockNum:" + dbManager.getDynamicPropertiesStore()
        .getLatestSolidifiedBlockNum());

  }

}
