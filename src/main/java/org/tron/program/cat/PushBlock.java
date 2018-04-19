package org.tron.program.cat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.tron.common.utils.FileUtil;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.UnLinkedBlockException;
import org.tron.core.exception.ValidateScheduleException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.protos.Protocol.Block;

public class PushBlock {
  private static final String TEST_CAT = "cat/config-cat.conf";

  private static Manager dbManager = new Manager();
  private static String dbPath = "output_cat_push_block";

  public static void main(String[] args)
      throws IOException, ContractExeException, UnLinkedBlockException, ValidateScheduleException, ContractValidateException, ValidateSignatureException {
    init();

    File f = new File("blocks.txt");
    FileInputStream fis = new FileInputStream(f);

    List<BlockCapsule> blockCapsuleList = new ArrayList<>();
    Block block;
    while ((block = Block.parseDelimitedFrom(fis)) != null) {
      blockCapsuleList.add(new BlockCapsule(Block.newBuilder(block).build()));
    }

    for (BlockCapsule blockCapsule : blockCapsuleList) {
      dbManager.pushBlock(blockCapsule);
    }

    removeDb();
  }

  private static void init() {
    Args.setParam(new String[]{"-d", dbPath, "-w"},
        TEST_CAT);
    dbManager.init();
  }

  private static void removeDb() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
    dbManager.destory();
  }

}
