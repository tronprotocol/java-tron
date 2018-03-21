package org.tron.common.crypto;

import static org.junit.Assert.assertEquals;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.utils.ByteArray;

@Slf4j
public class ECKeyTest {
  @Test
  public void testGeClientTestEcKey() {
    final ECKey key = ECKey.fromPrivate(
        Hex.decode("1cd5a70741c6e583d2dd3c5f17231e608eb1e52437210d948c5085e141c2d830"));

    logger.info("address = {}", ByteArray.toHexString(key.getAddress()));

    assertEquals("125b6c87b3d67114b3873977888c34582f27bbb0",
        ByteArray.toHexString(key.getAddress()));
  }
}