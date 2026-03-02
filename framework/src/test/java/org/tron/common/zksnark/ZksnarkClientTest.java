package org.tron.common.zksnark;

import static org.junit.Assert.assertThrows;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import java.nio.charset.StandardCharsets;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.BalanceContract;

public class ZksnarkClientTest {

  public ZksnarkClient zksnarkClient;

  @Before
  public void setUp() {
    zksnarkClient =  ZksnarkClient.getInstance();
  }

  @Test
  public void testCheckZksnarkProof() {
    BalanceContract.TransferContract transferContract =
        BalanceContract.TransferContract.newBuilder()
            .setAmount(10)
            .setOwnerAddress(ByteString.copyFromUtf8("aaa"))
            .setToAddress(ByteString.copyFromUtf8("bbb"))
            .build();
    Transaction transaction = Transaction.newBuilder().setRawData(Transaction.raw.newBuilder()
            .setData(ByteString.copyFrom("transfer".getBytes(StandardCharsets.UTF_8)))
            .addContract(Transaction.Contract.newBuilder().setParameter(Any.pack(transferContract))
                .setType(Transaction.Contract.ContractType.TransferContract)))
        .addRet(Transaction.Result.newBuilder().setAssetIssueID("1234567").build()).build();
    byte[] b = ByteArray
        .fromHexString("ded9c2181fd7ea468a7a7b1475defe90bb0fc0ca8d0f2096b0617465cea6568c");
    assertThrows(StatusRuntimeException.class, () -> zksnarkClient.checkZksnarkProof(transaction, b,
        10000L));
  }

}