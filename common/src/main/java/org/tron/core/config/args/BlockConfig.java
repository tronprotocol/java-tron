package org.tron.core.config.args;

import static org.tron.core.Constant.DEFAULT_PROPOSAL_EXPIRE_TIME;
import static org.tron.core.Constant.MAX_PROPOSAL_EXPIRE_TIME;
import static org.tron.core.Constant.MIN_PROPOSAL_EXPIRE_TIME;
import static org.tron.core.exception.TronError.ErrCode.PARAMETER_INIT;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.exception.TronError;

/**
 * Block configuration bean. Field names match config.conf keys under the "block" section.
 */
@Slf4j
@Getter
@Setter
public class BlockConfig {

  private boolean needSyncCheck = false;
  private long maintenanceTimeInterval = 21600000L;
  private long proposalExpireTime = DEFAULT_PROPOSAL_EXPIRE_TIME;
  private int checkFrozenTime = 1;

  // Defaults come from reference.conf (loaded globally via Configuration.java)

  /**
   * Create BlockConfig from the "block" section of the application config.
   * Also checks that committee.proposalExpireTime is not used (must use block.proposalExpireTime).
   */
  public static BlockConfig fromConfig(Config config) {
    // Reject legacy committee.proposalExpireTime location
    if (config.hasPath("committee.proposalExpireTime")) {
      throw new TronError("It is not allowed to configure committee.proposalExpireTime in "
          + "config.conf, please set the value in block.proposalExpireTime.", PARAMETER_INIT);
    }

    Config blockSection = config.getConfig("block");
    BlockConfig blockConfig = ConfigBeanFactory.create(blockSection, BlockConfig.class);
    blockConfig.postProcess();
    return blockConfig;
  }

  private void postProcess() {
    if (proposalExpireTime <= MIN_PROPOSAL_EXPIRE_TIME
        || proposalExpireTime >= MAX_PROPOSAL_EXPIRE_TIME) {
      throw new TronError("The value[block.proposalExpireTime] is only allowed to "
          + "be greater than " + MIN_PROPOSAL_EXPIRE_TIME + " and less than "
          + MAX_PROPOSAL_EXPIRE_TIME + "!", PARAMETER_INIT);
    }
  }
}
