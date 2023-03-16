package org.tron.core.state.annotation;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.tron.common.parameter.CommonParameter;

public class NeedWorldStateTrieStoreCondition implements Condition {
  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    return CommonParameter.getInstance().getStorage().isAllowStateRoot();
  }
}
