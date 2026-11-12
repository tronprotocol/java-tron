package org.tron.common.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import org.tron.common.VMConfigRule;
import org.tron.common.runtime.InternalTransaction.TrxType;
import org.tron.common.runtime.vm.DataWord;
import org.tron.core.ChainBaseManager;
import org.tron.core.actuator.VMActuator;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.TransactionContext;
import org.tron.core.db.TransactionTrace;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.Op;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Program;
import org.tron.core.vm.program.invoke.ProgramInvoke;
import org.tron.core.vm.repository.Repository;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.contractResult;

@RunWith(Parameterized.class)
public class RuntimeImplRevertTimeoutTest {

  private static final long ENERGY_LIMIT = 100_000L;
  private final boolean enabled;
  private final ContractType contractType;
  private StoreFactory stores;
  private MockedStatic<ChainBaseManager> managers;

  @Rule
  public final VMConfigRule vmConfigRule = new VMConfigRule();

  @Parameterized.Parameters(name = "resourceOptimization={0}, contractType={1}")
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][]{
        {false, ContractType.TriggerSmartContract},
        {true, ContractType.TriggerSmartContract},
        {false, ContractType.CreateSmartContract},
        {true, ContractType.CreateSmartContract}
    });
  }

  public RuntimeImplRevertTimeoutTest(boolean enabled, ContractType contractType) {
    this.enabled = enabled;
    this.contractType = contractType;
  }

  @Before
  public void setUp() {
    VMConfig.Snapshot snapshot = new VMConfig.Snapshot();
    snapshot.allowTvmResourceOptimization = enabled;
    snapshot.allowEnergyAdjustment = true;
    VMConfig.setLocalSnapshot(snapshot);
    VMConfig.setVmTrace(false);

    DynamicPropertiesStore properties = mock(DynamicPropertiesStore.class);
    when(properties.allowTvmResourceOptimization()).thenReturn(enabled);
    when(properties.getLatestVersion()).thenReturn(1);
    ChainBaseManager manager = mock(ChainBaseManager.class);
    when(manager.getDynamicPropertiesStore()).thenReturn(properties);
    stores = mock(StoreFactory.class);
    when(stores.getChainBaseManager()).thenReturn(manager);
    managers = mockStatic(ChainBaseManager.class);
    managers.when(ChainBaseManager::getInstance).thenReturn(manager);
  }

  @After
  public void tearDown() {
    if (managers != null) {
      managers.close();
    }
  }

  @Test
  public void localPostRevertTimeoutRequestsRetryAfterActivation() throws Exception {
    ProgramResult result = executeRevert(true);
    assertTrue(result.isRevert());
    assertTrue(result.getException() instanceof Program.OutOfTimeException);
    assertEquals(ENERGY_LIMIT, result.getEnergyUsed());
    assertEquals(enabled ? contractResult.OUT_OF_TIME : contractResult.REVERT,
        result.getResultCode());

    TransactionTrace trace = trace(result, contractResult.REVERT);
    assertEquals(ENERGY_LIMIT, trace.getReceipt().getEnergyUsageTotal());
    assertEquals(enabled, trace.checkNeedRetry());
    if (enabled) {
      assertThrows(ReceiptCheckErrException.class, trace::check);
    } else {
      trace.check();
    }

    TransactionTrace matching = trace(result, result.getResultCode());
    assertFalse(matching.checkNeedRetry());
    matching.check();
  }

  @Test
  public void producerTimeoutCannotSilentlyMatchNormalRevertAfterActivation() throws Exception {
    ProgramResult producer = executeRevert(true);
    ProgramResult validator = executeRevert(false);
    assertTrue(validator.isRevert());
    assertNull(validator.getException());
    assertEquals(contractResult.REVERT, validator.getResultCode());
    assertEquals(ENERGY_LIMIT, producer.getEnergyUsed());
    assertEquals(6L, validator.getEnergyUsed());

    TransactionTrace trace = trace(validator, producer.getResultCode());
    assertFalse(trace.checkNeedRetry());
    if (enabled) {
      assertThrows(ReceiptCheckErrException.class, trace::check);
    } else {
      // Preserve the historical receipt check before the proposal activates.
      trace.check();
    }
    trace(validator, contractResult.REVERT).check();
  }

  private ProgramResult executeRevert(boolean timeout) throws Exception {
    ProgramInvoke invoke = mock(ProgramInvoke.class);
    when(invoke.getEnergyLimit()).thenReturn(ENERGY_LIMIT);
    when(invoke.getVmShouldEndInUs()).thenReturn(Long.MAX_VALUE);
    when(invoke.getContractAddress()).thenReturn(new DataWord(1));
    // PUSH1 0 PUSH1 0 REVERT. Only the post-execution CPU check is forced to time out.
    Program program = new Program(new byte[]{0x60, 0, 0x60, 0, (byte) Op.REVERT}, new byte[21],
        invoke, mock(InternalTransaction.class)) {
      @Override
      public void checkCPUTimeLimit(String opName) {
        if (timeout && opName.endsWith("(TX_LAST_OP)")) {
          throw Program.Exception.notEnoughTime(opName);
        }
      }
    };
    program.setRootTransactionId(new byte[32]);
    VMActuator actuator = new VMActuator(false);
    actuator.setTrxType(contractType == ContractType.CreateSmartContract
        ? TrxType.TRX_CONTRACT_CREATION_TYPE : TrxType.TRX_CONTRACT_CALL_TYPE);
    ReflectionTestUtils.setField(actuator, "program", program);
    ReflectionTestUtils.setField(actuator, "rootRepository", mock(Repository.class));
    TransactionContext context = new TransactionContext(null, null, stores, false, false);
    actuator.execute(context);
    ProgramResult result = context.getProgramResult();
    ReflectionTestUtils.invokeMethod(new RuntimeImpl(), "setResultCode", result);
    return result;
  }

  private TransactionTrace trace(ProgramResult result, contractResult expected) {
    Transaction transaction = Transaction.newBuilder()
        .setRawData(Transaction.raw.newBuilder().addContract(
            Transaction.Contract.newBuilder().setType(contractType)))
        .addRet(Transaction.Result.newBuilder().setContractRet(expected))
        .build();
    TransactionTrace trace = new TransactionTrace(new TransactionCapsule(transaction), stores,
        new RuntimeImpl());
    trace.init(null);
    trace.getTransactionContext().setProgramResult(result);
    trace.setBill(result.getEnergyUsed());
    trace.setResult();
    return trace;
  }
}
