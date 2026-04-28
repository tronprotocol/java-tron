package org.tron.core.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.MockitoJUnitRunner;
import org.reflections.Reflections;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.core.actuator.AbstractActuator;
import org.tron.core.actuator.TransferActuator;
import org.tron.core.config.args.Args;
import org.tron.core.exception.TronError;

@RunWith(MockitoJUnitRunner.class)
public class TransactionRegisterTest {

  @Before
  public void init() {
    Args.getInstance().setActuatorSet(new HashSet<>());
    TransactionRegister.resetForTesting();
  }

  @After
  public void destroy() {
    Args.clearParam();
  }

  @Test
  public void testAlreadyRegisteredSkipRegistration() {
    TransactionRegister.registerActuator();
    assertTrue("First registration should be completed", TransactionRegister.isRegistered());

    TransactionRegister.registerActuator();
    assertTrue("Registration should still be true", TransactionRegister.isRegistered());
  }

  @Test
  public void testConcurrentAccessThreadSafe() throws InterruptedException {
    final int threadCount = 5;
    final AtomicBoolean testPassed = new AtomicBoolean(true);
    ExecutorService executor = ExecutorServiceManager
        .newFixedThreadPool("transaction-register-test", threadCount);
    Future<?>[] futures = new Future<?>[threadCount];

    try {
      for (int i = 0; i < threadCount; i++) {
        futures[i] = executor.submit(() -> {
          try {
            TransactionRegister.registerActuator();
          } catch (Throwable e) {
            testPassed.set(false);
            throw e;
          }
        });
      }

      for (Future<?> future : futures) {
        try {
          future.get();
        } catch (ExecutionException e) {
          Assert.fail("Concurrent registration should not throw: " + e.getCause());
        }
      }
    } finally {
      ExecutorServiceManager.shutdownAndAwaitTermination(executor, "transaction-register-test");
    }

    assertTrue("All threads should complete without exceptions", testPassed.get());
    assertTrue("Registration should be completed", TransactionRegister.isRegistered());
  }

  @Test
  public void testDoubleCheckLockingAtomicBoolean() {
    assertFalse("Initial registration state should be false", TransactionRegister.isRegistered());

    TransactionRegister.registerActuator();
    assertTrue("After first call, should be registered", TransactionRegister.isRegistered());

    TransactionRegister.registerActuator();
    assertTrue("After second call, should still be registered", TransactionRegister.isRegistered());
  }

  @Test
  public void testRegistrationRunsExactlyOnce() {
    final AtomicInteger constructorCallCount = new AtomicInteger(0);

    try (MockedConstruction<Reflections> ignored = mockConstruction(Reflections.class,
        (mock, context) -> {
          constructorCallCount.incrementAndGet();
          when(mock.getSubTypesOf(AbstractActuator.class)).thenReturn(Collections.emptySet());
        })) {

      // Call multiple times; Reflections should only be constructed once
      for (int i = 0; i < 5; i++) {
        TransactionRegister.registerActuator();
      }

      assertEquals("Reflections should be constructed exactly once regardless of call count",
          1, constructorCallCount.get());
      assertTrue(TransactionRegister.isRegistered());
    }
  }

  @Test
  public void testMultipleCallsConsistency() {
    assertFalse("Should start unregistered", TransactionRegister.isRegistered());

    TransactionRegister.registerActuator();
    assertTrue("Should be registered after first call", TransactionRegister.isRegistered());

    for (int i = 0; i < 5; i++) {
      TransactionRegister.registerActuator();
      assertTrue("Should remain registered after call " + (i + 2),
          TransactionRegister.isRegistered());
    }
  }

  @Test
  public void testThrowsTronError() {
    try (MockedConstruction<Reflections> ignored = mockConstruction(Reflections.class,
        (mock, context) -> when(mock.getSubTypesOf(AbstractActuator.class))
            .thenReturn(Collections.singleton(TransferActuator.class)));
         MockedConstruction<TransferActuator> ignored1 = mockConstruction(TransferActuator.class,
             (mock, context) -> {
               throw new RuntimeException("boom");
             })) {
      TronError error = assertThrows(TronError.class, TransactionRegister::registerActuator);
      assertEquals(TronError.ErrCode.ACTUATOR_REGISTER, error.getErrCode());
      assertTrue(error.getMessage().contains("TransferActuator"));
    }
  }
}
