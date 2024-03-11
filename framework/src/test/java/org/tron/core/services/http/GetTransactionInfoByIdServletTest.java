package org.tron.core.services.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.tron.common.utils.client.utils.HttpMethed.createRequest;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;

import java.io.UnsupportedEncodingException;
import javax.annotation.Resource;

import org.apache.http.client.methods.HttpPost;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionInfoCapsule;
import org.tron.core.capsule.TransactionRetCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.TransactionStore;
import org.tron.core.db.TransactionStoreTest;
import org.tron.core.store.TransactionRetStore;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;

public class GetTransactionInfoByIdServletTest extends BaseTest {

  @Resource
  private GetTransactionInfoByIdServlet getTransactionInfoByIdServlet;
  @Resource
  private TransactionStore transactionStore;
  @Resource
  private TransactionRetStore transactionRetStore;

  private static final String OWNER_ADDRESS =
          Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
  private static final String TO_ADDRESS =
          Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
  private static final long AMOUNT = 100;
  private static final byte[] KEY_1 = TransactionStoreTest.randomBytes(21);

  static {
    Args.setParam(
            new String[]{
                "--output-directory", dbPath(),
            }, Constant.TEST_CONF
    );
  }

  @Before
  public void init() {
    byte[] blockNum = ByteArray.fromLong(100);
    TransactionInfoCapsule transactionInfoCapsule = new TransactionInfoCapsule();

    transactionInfoCapsule.setId(KEY_1);
    transactionInfoCapsule.setFee(1000L);
    transactionInfoCapsule.setBlockNumber(100L);
    transactionInfoCapsule.setBlockTimeStamp(200L);

    TransactionRetCapsule transactionRetCapsule = new TransactionRetCapsule();
    transactionRetCapsule.addTransactionInfo(transactionInfoCapsule.getInstance());
    chainBaseManager.getTransactionRetStore()
            .put(blockNum, transactionRetCapsule);
    transactionRetStore.put(blockNum, transactionRetCapsule);

    AccountCapsule owner = new AccountCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            ByteString.copyFromUtf8("owner"),
            Protocol.AccountType.forNumber(1));
    owner.setBalance(1000000L);

    AccountCapsule to = new AccountCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
            ByteString.copyFromUtf8("to"),
            Protocol.AccountType.forNumber(1));
    to.setBalance(1000000L);

    chainBaseManager.getAccountStore().put(owner.createDbKey(),
            owner);
    chainBaseManager.getAccountStore().put(to.createDbKey(),
            to);
    BalanceContract.TransferContract transferContract =
            getContract(AMOUNT, OWNER_ADDRESS, TO_ADDRESS);
    TransactionCapsule transactionCapsule = new TransactionCapsule(transferContract,
            chainBaseManager.getAccountStore());
    transactionCapsule.setBlockNum(100L);
    transactionStore.put(KEY_1, transactionCapsule);
  }

  private BalanceContract.TransferContract getContract(long count,
                                                       String owneraddress, String toaddress) {
    return BalanceContract.TransferContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(owneraddress)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(toaddress)))
            .setAmount(count)
            .build();
  }

  @Test
  public void testGetInfoById() {
    String jsonParam = "{\"value\" : "
            + "\"" + ByteArray.toHexString(KEY_1) + "\"}";
    MockHttpServletRequest request = createRequest(HttpPost.METHOD_NAME);
    request.setContentType("application/json");
    request.setContent(jsonParam.getBytes(UTF_8));
    MockHttpServletResponse response = new MockHttpServletResponse();

    getTransactionInfoByIdServlet.doPost(request, response);
    Assert.assertEquals(200, response.getStatus());
    try {
      String contentAsString = response.getContentAsString();
      JSONObject jsonObject = JSONObject.parseObject(contentAsString);
      Assert.assertEquals(1000, jsonObject.get("fee"));
      Assert.assertEquals(100, jsonObject.get("blockNumber"));
    } catch (UnsupportedEncodingException e) {
      Assert.fail(e.getMessage());
    }
  }
}
