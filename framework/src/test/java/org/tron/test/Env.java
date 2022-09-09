package org.tron.test;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;

@Slf4j(topic = "test")
public  class Env {


  /**
   * Init necessary resource. Works with {@link org.junit.Before},{@link org.junit.BeforeClass}
   * @return context
   */
  public static AnnotationConfigApplicationContext  init(
      String[] params, String config, String db) {

    // 1. clear up database
    cleanUpDatabase(db);
    // 2. init args
    Args.setParam(params, config);
    // 2. init context
    return new TronApplicationContext(DefaultConfig.class);
  }


  /**
   * Destroy env except context. Works with {@link org.junit.After},{@link org.junit.AfterClass}
   */
  public static void destroy(AnnotationConfigApplicationContext context, String db) {
    // 1. destroy context
    context.destroy();
    // 2. clear params
    Args.clearParam();
    // 3. clear up database
    cleanUpDatabase(db);
  }

  /**
   *  Delete the database.
   */
  public static void cleanUpDatabase(String db) {
    File f = new File(db);
    if (f.exists()) {
      if (FileUtil.deleteDir(f)) {
        logger.info("Release resources successful.");
      } else {
        logger.info("Release resources failure.");
      }
    }
  }

}
