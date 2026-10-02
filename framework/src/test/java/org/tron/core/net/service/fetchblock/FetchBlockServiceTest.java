package org.tron.core.net.service.fetchblock;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.Assert;
import org.junit.Test;

public class FetchBlockServiceTest {

  @Test
  public void testFetchBlockInfoIsVolatile() throws Exception {
    Field fetchBlockInfo = FetchBlockService.class.getDeclaredField("fetchBlockInfo");

    Assert.assertTrue(Modifier.isVolatile(fetchBlockInfo.getModifiers()));
  }
}
