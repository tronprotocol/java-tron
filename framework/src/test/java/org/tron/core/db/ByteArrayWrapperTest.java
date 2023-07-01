package org.tron.core.db;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.testng.Assert;
import org.tron.common.utils.ByteArray;

@Slf4j
public class ByteArrayWrapperTest {

  @Test
  public void createByteArray() {
    ByteArrayWrapper byteArrayWrapper1 = new ByteArrayWrapper(ByteArray.fromHexString("1"));
    ByteArrayWrapper byteArrayWrapper2 = new ByteArrayWrapper(ByteArray.fromHexString("2"));
    Assert.assertEquals(byteArrayWrapper1.compareTo(byteArrayWrapper2), -1);
    Assert.assertFalse(byteArrayWrapper1.equals(byteArrayWrapper2));
    Assert.assertFalse(byteArrayWrapper1.getData().equals(byteArrayWrapper2.getData()));
    Assert.assertTrue(byteArrayWrapper1.hashCode() != byteArrayWrapper2.hashCode());
    Assert.assertEquals(byteArrayWrapper1.toString().equals(byteArrayWrapper2.toString()),false);
  }

}