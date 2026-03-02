package org.tron.core.actuator.vm;

import org.junit.Test;
import org.tron.core.vm.trace.Serializers;

public class SerializersTest {

  @Test
  public void testSerializeFieldsOnly() {
    Serializers.serializeFieldsOnly("testString", true);
  }
}
