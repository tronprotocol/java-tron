package org.tron.program.cat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.DefaultConfig;
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

  static ApplicationContext context;

  private static Manager dbManager;
  private static String dbPath = "output_cat_push_block";

  public static void main(String[] args)
      throws IOException, ContractExeException, UnLinkedBlockException, ValidateScheduleException, ContractValidateException, ValidateSignatureException {
    init();

    File f = new File("blocks.txt");
    FileInputStream fis = new FileInputStream(f);

    Block block;
    while ((block = Block.parseDelimitedFrom(fis)) != null) {
      dbManager.pushBlock(new BlockCapsule(Block.newBuilder(block).build()));
    }

    removeDb();
  }

  private static void init() {
    Args.setParam(new String[]{"-d", dbPath, "-w"},
        TEST_CAT);

    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
    dbManager = context.getBean(Manager.class);
  }

  private static void removeDb() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
    dbManager.destory();
  }

}
