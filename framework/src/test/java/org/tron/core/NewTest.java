package org.tron.core;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Hash;

@Slf4j
public class NewTest {

  @Test
  public void checkLogTopics() {
    byte[] SHIELDED_TRC20_LOG_TOPICS = Hash.sha3(ByteArray.fromString(
        "NewLeaf(uint256,bytes32,bytes32,bytes32,bytes32[21])")); //
    Assert.assertArrayEquals(SHIELDED_TRC20_LOG_TOPICS, ByteArray
        .fromHexString("58aa407d312e8d4017790223440ca1f60c54959864d7bd1d1ed37c82f72dfc1d"));
    // ;
    logger.info(ByteArray.toHexString(SHIELDED_TRC20_LOG_TOPICS));
    byte[] SHIELDED_TRC20_LOG_TOPICS_FOR_BURN = Hash.sha3(ByteArray
        .fromString(
            "TokenBurn(address,uint256,bytes32[3])")); //
    Assert.assertArrayEquals(SHIELDED_TRC20_LOG_TOPICS_FOR_BURN, ByteArray
        .fromHexString("1daf70c304f467a9efbc9ac1ca7bfe859a478aa6c4b88131b4dbb1547029b972"));
    logger.info(ByteArray.toHexString(SHIELDED_TRC20_LOG_TOPICS_FOR_BURN));
    // "1daf70c304f467a9efbc9ac1ca7bfe859a478aa6c4b88131b4dbb1547029b972";
  }

}
