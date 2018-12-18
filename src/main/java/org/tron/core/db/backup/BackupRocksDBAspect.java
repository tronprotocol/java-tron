package org.tron.core.db.backup;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.core.capsule.BlockCapsule;

@Slf4j
@Aspect
public class BackupRocksDBAspect {

  @Autowired
  private BackupDbUtil util;

  @Pointcut("execution(** org.tron.core.db.Manager.pushBlock(..)) && args(block)")
  public void pointPushBlock(BlockCapsule block) {

  }

  @Before("pointPushBlock(block)")
  public void backupDb(BlockCapsule block) {
    try {
      util.doBackup(block);
    } catch (Exception e) {
      logger.error("backup db failure: {}", e);
    }
  }

  @AfterThrowing("pointPushBlock(block)")
  public void logErrorPushBlock(BlockCapsule block) {
    logger.info("AfterThrowing pushBlock");
  }
}