package org.tron.core.services.http;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.tron.api.GrpcAPI;
import org.tron.common.TestConstants;
import org.tron.core.Wallet;
import org.tron.core.actuator.TransactionFactory;
import org.tron.core.config.args.Args;
import org.tron.json.JSONObject;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.BalanceContract.TransferContract;

public class BroadcastServletTest {

  @Before
  public void setUp() {
    Args.setParam(new String[0], TestConstants.TEST_CONF);
  }

  @After
  public void tearDown() {
    Args.clearParam();
  }

  @Test
  public void doPostTest() throws IOException {
    BroadcastServlet servlet = new BroadcastServlet();
    Wallet wallet = mock(Wallet.class);
    ReflectionTestUtils.setField(servlet, "wallet", wallet);
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    String postData = "{\"signature\":[\"97c825b41c77de2a8bd65b3df55cd4c0df59c307c0187e"
        + "42321dcc1cc455ddba583dd9502e17cfec5945b34cad0511985a6165999092a6dec84c2bdd9"
        + "7e649fc01\"],\"txID\":\"454f156bf1256587ff6ccdbc56e64ad0c51e4f8efea5490dcbc7"
        + "20ee606bc7b8\",\"raw_data\":{\"contract\":[{\"parame"
        + "ter\":{\"value\":{\"amount\":1000,\"owner_address\":\"41e552f6"
        + "487585c2b58bc2c9bb4492bc1f17132cd0\",\"to_address\":\"41d1e7a6bc354106cb410e"
        + "65ff8b181c600ff14292\"},\"type_url\":\"type.googl"
        + "eapis.com/protocol.TransferContract\"},\"type\":\"TransferCon"
        + "tract\"}],\"ref_block_bytes\":\"267e\",\"ref_block_hash\":\"9a447d222e8"
        + "de9f2\",\"expiration\":1530893064000,\"timestamp\":1530893006233}}";
    when(request.getReader()).thenReturn(new BufferedReader(new StringReader(postData)));
    when(wallet.broadcastTransaction(org.mockito.ArgumentMatchers.any(Transaction.class)))
        .thenReturn(GrpcAPI.Return.newBuilder().setResult(true).build());
    StringWriter body = new StringWriter();
    try (MockedStatic<TransactionFactory> contracts = Mockito.mockStatic(TransactionFactory.class);
        PrintWriter writer = new PrintWriter(body)) {
      contracts.when(() -> TransactionFactory.getContract(ContractType.TransferContract))
          .thenReturn(TransferContract.class);
      when(response.getWriter()).thenReturn(writer);
      servlet.doPost(request, response);
      writer.flush();
      JSONObject result = JSONObject.parseObject(body.toString());
      Assert.assertEquals(Boolean.TRUE, result.get("result"));
      Assert.assertNotNull(result.getString("txid"));
      ArgumentCaptor<Transaction> transaction = ArgumentCaptor.forClass(Transaction.class);
      verify(wallet).broadcastTransaction(transaction.capture());
      Assert.assertEquals(1, transaction.getValue().getRawData().getContractCount());
    }
  }
}
