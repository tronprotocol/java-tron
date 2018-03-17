package org.tron.common.crypto;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.utils.ByteArray;

public class ECKeyTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  @Test
  public void testGeClientTestEcKey() {
    final ECKey key = ECKey.fromPrivate(
        Hex.decode("1cd5a70741c6e583d2dd3c5f17231e608eb1e52437210d948c5085e141c2d830"));

    logger.info("address = {}", ByteArray.toHexString(key.getAddress()));

    assertEquals("125b6c87b3d67114b3873977888c34582f27bbb0",
        ByteArray.toHexString(key.getAddress()));
  }
}