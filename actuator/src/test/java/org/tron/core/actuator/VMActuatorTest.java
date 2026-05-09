package org.tron.core.actuator;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VMActuatorTest {

  @Test
  public void testConstantCallUsesConfiguredTimeoutVerbatim() {
    assertEquals(123_000L, VMActuator.calculateCpuLimitInUs(true, 80L, 5.0, 123L));
  }

  @Test
  public void testConstantCallWithoutConfiguredTimeoutUsesNetworkDeadline() {
    assertEquals(400_000L, VMActuator.calculateCpuLimitInUs(true, 80L, 5.0, 0L));
  }

  @Test
  public void testNonConstantCallIgnoresConfiguredTimeout() {
    assertEquals(400_000L, VMActuator.calculateCpuLimitInUs(false, 80L, 5.0, 123L));
  }
}
