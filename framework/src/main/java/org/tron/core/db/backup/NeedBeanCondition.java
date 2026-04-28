package org.tron.core.db.backup;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.tron.core.config.args.Args;

public class NeedBeanCondition implements Condition {

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    if (Args.getInstance() == null || Args.getInstance().getStorage() == null
        || Args.getInstance().getStorage().getDbEngine() == null
        || Args.getInstance().getDbBackupConfig() == null) {
      return false;
    }
    return "ROCKSDB".equalsIgnoreCase(Args.getInstance().getStorage().getDbEngine())
        && Args.getInstance().getDbBackupConfig().isEnable() && !Args.getInstance().isWitness();
  }
}