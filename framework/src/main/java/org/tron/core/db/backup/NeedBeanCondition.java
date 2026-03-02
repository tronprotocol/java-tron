package org.tron.core.db.backup;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.tron.core.config.args.Args;

public class NeedBeanCondition implements Condition {

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    return ("ROCKSDB".equals(Args.getInstance().getStorage().getDbEngine().toUpperCase()))
        && Args.getInstance().getDbBackupConfig().isEnable() && !Args.getInstance().isWitness();
  }
}