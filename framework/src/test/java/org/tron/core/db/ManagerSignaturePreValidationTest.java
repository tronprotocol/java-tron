package org.tron.core.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.Transaction;

public class ManagerSignaturePreValidationTest {

  @Test
  public void testRejectStrictInvalidLengthBeforeSubmission() throws Exception {
    ExecutorService validateSignService = mock(ExecutorService.class);
    DynamicPropertiesStore dynamicPropertiesStore = mock(DynamicPropertiesStore.class);
    Manager manager = createManager(validateSignService, dynamicPropertiesStore);
    TransactionCapsule valid = transactionWithSignatures(65);
    TransactionCapsule invalidMultiSign = transactionWithSignatures(65, 66);

    when(dynamicPropertiesStore.allowStrictEcdsaValidation()).thenReturn(true);
    runSubmittedTasks(validateSignService);

    try (MockedStatic<CommonParameter> commonParameter = mockEcKeyEngine(true)) {
      InvocationTargetException exception = assertThrows(InvocationTargetException.class,
          () -> invokePreValidate(manager, Arrays.asList(valid, invalidMultiSign)));

      assertTrue(exception.getCause() instanceof ValidateSignatureException);
      assertEquals("Signature size is 66", exception.getCause().getMessage());
      verify(validateSignService, never()).submit(any(Callable.class));
    }
  }

  @Test
  public void testRejectTruncatedLengthBeforeSubmission() throws Exception {
    ExecutorService validateSignService = mock(ExecutorService.class);
    DynamicPropertiesStore dynamicPropertiesStore = mock(DynamicPropertiesStore.class);
    Manager manager = createManager(validateSignService, dynamicPropertiesStore);
    TransactionCapsule transaction = transactionWithSignatures(64);

    when(dynamicPropertiesStore.allowStrictEcdsaValidation()).thenReturn(false);
    runSubmittedTasks(validateSignService);

    try (MockedStatic<CommonParameter> commonParameter = mockEcKeyEngine(true)) {
      InvocationTargetException exception = assertThrows(InvocationTargetException.class,
          () -> invokePreValidate(manager, Collections.singletonList(transaction)));

      assertTrue(exception.getCause() instanceof ValidateSignatureException);
      assertEquals("Signature size is 64", exception.getCause().getMessage());
      verify(validateSignService, never()).submit(any(Callable.class));
    }
  }

  @Test
  public void testPreserveLegacyPaddedSignature() throws Exception {
    ExecutorService validateSignService = mock(ExecutorService.class);
    DynamicPropertiesStore dynamicPropertiesStore = mock(DynamicPropertiesStore.class);
    Manager manager = createManager(validateSignService, dynamicPropertiesStore);
    TransactionCapsule transaction = transactionWithSignatures(66);

    when(dynamicPropertiesStore.allowStrictEcdsaValidation()).thenReturn(false);
    runSubmittedTasks(validateSignService);

    try (MockedStatic<CommonParameter> commonParameter = mockEcKeyEngine(true)) {
      invokePreValidate(manager, Collections.singletonList(transaction));

      verify(validateSignService).submit(any(Callable.class));
    }
  }

  @Test
  public void testSubmitCanonicalStrictSignature() throws Exception {
    ExecutorService validateSignService = mock(ExecutorService.class);
    DynamicPropertiesStore dynamicPropertiesStore = mock(DynamicPropertiesStore.class);
    Manager manager = createManager(validateSignService, dynamicPropertiesStore);
    TransactionCapsule transaction = transactionWithSignatures(65);

    when(dynamicPropertiesStore.allowStrictEcdsaValidation()).thenReturn(true);
    runSubmittedTasks(validateSignService);

    try (MockedStatic<CommonParameter> commonParameter = mockEcKeyEngine(true)) {
      invokePreValidate(manager, Collections.singletonList(transaction));

      verify(validateSignService).submit(any(Callable.class));
    }
  }

  private Manager createManager(ExecutorService validateSignService,
      DynamicPropertiesStore dynamicPropertiesStore) throws Exception {
    Manager manager = new Manager();
    ChainBaseManager chainBaseManager = mock(ChainBaseManager.class);
    when(chainBaseManager.getAccountStore()).thenReturn(mock(AccountStore.class));
    when(chainBaseManager.getDynamicPropertiesStore()).thenReturn(dynamicPropertiesStore);
    setField(manager, "chainBaseManager", chainBaseManager);
    setField(manager, "validateSignService", validateSignService);
    return manager;
  }

  private TransactionCapsule transactionWithSignatures(int... signatureLengths)
      throws ValidateSignatureException {
    Transaction.Builder transactionBuilder = Transaction.newBuilder();
    for (int signatureLength : signatureLengths) {
      transactionBuilder.addSignature(ByteString.copyFrom(new byte[signatureLength]));
    }
    TransactionCapsule transaction = mock(TransactionCapsule.class);
    when(transaction.getInstance()).thenReturn(transactionBuilder.build());
    when(transaction.validateSignature(any(AccountStore.class),
        any(DynamicPropertiesStore.class))).thenReturn(true);
    return transaction;
  }

  private void runSubmittedTasks(ExecutorService validateSignService) {
    when(validateSignService.submit(any(Callable.class))).thenAnswer(invocation -> {
      Callable<Boolean> task = invocation.getArgument(0);
      FutureTask<Boolean> future = new FutureTask<>(task);
      future.run();
      return future;
    });
  }

  private MockedStatic<CommonParameter> mockEcKeyEngine(boolean ecKeyEngine) {
    CommonParameter parameter = mock(CommonParameter.class);
    when(parameter.isECKeyCryptoEngine()).thenReturn(ecKeyEngine);
    MockedStatic<CommonParameter> mockedStatic = mockStatic(CommonParameter.class);
    mockedStatic.when(CommonParameter::getInstance).thenReturn(parameter);
    return mockedStatic;
  }

  private void invokePreValidate(Manager manager, List<TransactionCapsule> transactions)
      throws Exception {
    Method method = Manager.class.getDeclaredMethod("preValidateTransactionSign", List.class);
    method.setAccessible(true);
    method.invoke(manager, transactions);
  }

  private void setField(Manager manager, String fieldName, Object value) throws Exception {
    Field field = Manager.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(manager, value);
  }
}
