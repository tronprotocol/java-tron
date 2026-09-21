package org.tron.core.actuator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.tron.common.utils.ForkController;
import org.tron.core.config.Parameter.ForkBlockVersionEnum;
import org.tron.core.vm.program.Program.OutOfTimeException;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;

public class ContractHashValidationTest {

  @Test
  public void acceptsHashFieldsBeforeActivation() {
    SmartContract contract = SmartContract.newBuilder()
        .setCodeHash(ByteString.copyFromUtf8("code"))
        .setTrxHash(ByteString.copyFromUtf8("transaction"))
        .build();

    runWithActivation(false, () -> VMActuator.checkContractHashFields(contract));
  }

  @Test
  public void rejectsCodeHashAfterActivation() {
    SmartContract contract = SmartContract.newBuilder()
        .setCodeHash(ByteString.copyFromUtf8("code"))
        .build();

    OutOfTimeException exception = assertThrows(OutOfTimeException.class,
        () -> runWithActivation(true, () -> VMActuator.checkContractHashFields(contract)));

    assertEquals("CPU timeout for contract hash fields", exception.getMessage());
  }

  @Test
  public void rejectsTransactionHashAfterActivation() {
    SmartContract contract = SmartContract.newBuilder()
        .setTrxHash(ByteString.copyFromUtf8("transaction"))
        .build();

    OutOfTimeException exception = assertThrows(OutOfTimeException.class,
        () -> runWithActivation(true, () -> VMActuator.checkContractHashFields(contract)));

    assertEquals("CPU timeout for contract hash fields", exception.getMessage());
  }

  @Test
  public void acceptsEmptyHashFieldsAfterActivation() {
    runWithActivation(true,
        () -> VMActuator.checkContractHashFields(SmartContract.getDefaultInstance()));
  }

  private void runWithActivation(boolean activated, Runnable action) {
    ForkController controller = mock(ForkController.class);
    when(controller.pass(ForkBlockVersionEnum.VERSION_4_8_2_2)).thenReturn(activated);
    try (MockedStatic<ForkController> controllerMock = Mockito.mockStatic(ForkController.class)) {
      controllerMock.when(ForkController::instance).thenReturn(controller);
      action.run();
    }
  }
}
