package org.tron.common.logsfilter;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.tron.common.logsfilter.capsule.TransactionLogTriggerCapsule;
import org.tron.common.logsfilter.trigger.InternalTransactionPojo;
import org.tron.common.runtime.InternalTransaction;
import org.tron.common.runtime.ProgramResult;
import org.tron.common.runtime.RuntimeImpl;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.ReceiptCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.TransactionTrace;
import org.tron.p2p.utils.ByteArray;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    TransactionLogTriggerCapsule.class,
    TransactionTrace.class
})
public class TransactionLogTriggerCapsuleMockTest {

  private static final String OWNER_ADDRESS = "41548794500882809695a8a687866e76d4271a1abc";
  private static final String RECEIVER_ADDRESS = "41abd4b9367799eaa3197fecb144eb71de1e049150";
  private static final String CONTRACT_ADDRESS = "A0B4750E2CD76E19DCA331BF5D089B71C3C2798548";

  private TransactionCapsule transactionCapsule;
  private BlockCapsule blockCapsule;

  @Before
  public void setup() {
    blockCapsule = new BlockCapsule(1,
        Sha256Hash.ZERO_HASH,
        System.currentTimeMillis(),
        Sha256Hash.ZERO_HASH.getByteString()
    );
  }

  @After
  public void  clearMocks() {
    Mockito.framework().clearInlineMocks();
  }


  @Test
  public void testConstructorWithTransactionTrace() {
    BalanceContract.TransferContract.Builder builder2 =
        BalanceContract.TransferContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)));
    transactionCapsule = spy(new TransactionCapsule(builder2.build(),
        Protocol.Transaction.Contract.ContractType.TransferContract));

    TransactionTrace trace = mock(TransactionTrace.class);
    ReceiptCapsule receiptCapsule = new ReceiptCapsule(Sha256Hash.ZERO_HASH);
    RuntimeImpl runtime = mock(RuntimeImpl.class);
    List<Protocol.TransactionInfo.Log> logs = new ArrayList<>();
    logs.add(Protocol.TransactionInfo.Log.newBuilder()
        .setAddress(ByteString.copyFrom("address".getBytes()))
        .setData(ByteString.copyFrom("data".getBytes()))
        .addTopics(ByteString.copyFrom("topic".getBytes()))
        .build());

    Protocol.TransactionInfo.Builder builder = Protocol.TransactionInfo.newBuilder()
        .addAllLog(logs);

    ProgramResult programResult = ProgramResult.createEmpty();
    programResult.setHReturn("hreturn".getBytes());
    programResult.setContractAddress(CONTRACT_ADDRESS.getBytes());

    when(transactionCapsule.getTrxTrace()).thenReturn(trace);
    when(trace.getReceipt()).thenReturn(receiptCapsule);
    when(trace.getRuntime()).thenReturn(runtime);
    when(runtime.getResult()).thenReturn(programResult);

    transactionCapsule.setTrxTrace(trace);

    TransactionLogTriggerCapsule triggerCapsule = new TransactionLogTriggerCapsule(
        transactionCapsule, blockCapsule,0,0,0,
        builder.build(),0);

    Assert.assertNotNull(triggerCapsule.getTransactionLogTrigger());
  }

  @Test
  public void testGetInternalTransactionList() throws Exception {
    BalanceContract.TransferContract.Builder builder2 =
        BalanceContract.TransferContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)));
    transactionCapsule = new TransactionCapsule(builder2.build(),
        Protocol.Transaction.Contract.ContractType.TransferContract);
    InternalTransaction internalTransaction = new InternalTransaction(
        "parentHash".getBytes(), 10, 0,
        "sendAddress".getBytes(),
        "transferToAddress".getBytes(),
        100L, "data".getBytes(), "note",
        0L, new HashMap<>()
    );
    List<InternalTransaction> internalTransactionList = new ArrayList<>();
    internalTransactionList.add(internalTransaction);
    TransactionLogTriggerCapsule triggerCapsule =
        new TransactionLogTriggerCapsule(transactionCapsule, blockCapsule);

    List<InternalTransactionPojo> pojoList = Whitebox.invokeMethod(triggerCapsule,
        "getInternalTransactionList", internalTransactionList);

    Assert.assertNotNull(pojoList);
  }

}