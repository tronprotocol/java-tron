package org.tron.core.actuator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.tron.common.utils.ForkController;
import org.tron.core.config.Parameter.ForkBlockVersionEnum;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;

public class ContractNameValidationTest {

  @Test
  public void acceptsThirtyTwoByteNameAfterActivation() throws Throwable {
    SmartContract contract = contractWithName("12345678901234567890123456789012");

    runWithActivation(true, () -> VMActuator.checkContractNameLength(contract));
  }

  @Test
  public void rejectsThirtyThreeByteNameAfterActivation() {
    SmartContract contract = contractWithName("123456789012345678901234567890123");

    ContractValidateException exception = assertThrows(ContractValidateException.class,
        () -> runWithActivation(true, () -> VMActuator.checkContractNameLength(contract)));

    assertEquals("contractName's length cannot be greater than 32", exception.getMessage());
  }

  @Test
  public void countsMultibyteNameUsingProtobufBytesAfterActivation() {
    SmartContract contract = contractWithName("合合合合合合合合合合合");
    assertEquals(33, contract.getNameBytes().size());

    assertThrows(ContractValidateException.class,
        () -> runWithActivation(true, () -> VMActuator.checkContractNameLength(contract)));
  }

  @Test
  public void preservesNameValidationBeforeActivation() {
    SmartContract contract = contractWithName("123456789012345678901234567890123");

    assertThrows(ContractValidateException.class,
        () -> runWithActivation(false, () -> VMActuator.checkContractNameLength(contract)));
  }

  private SmartContract contractWithName(String name) {
    return SmartContract.newBuilder().setName(name).build();
  }

  private void runWithActivation(boolean activated, ThrowingRunnable action) throws Throwable {
    ForkController controller = mock(ForkController.class);
    when(controller.pass(ForkBlockVersionEnum.VERSION_4_8_2_2)).thenReturn(activated);
    try (MockedStatic<ForkController> controllerMock = Mockito.mockStatic(ForkController.class)) {
      controllerMock.when(ForkController::instance).thenReturn(controller);
      action.run();
    }
  }
}
