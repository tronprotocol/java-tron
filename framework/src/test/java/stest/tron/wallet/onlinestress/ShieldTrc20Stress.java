package stest.tron.wallet.onlinestress;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.Note;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.ShieldedAddressInfo;
import stest.tron.wallet.common.client.utils.ZenTrc20Base;

@Slf4j
public class ShieldTrc20Stress extends ZenTrc20Base {
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  Optional<ShieldedAddressInfo> sendShieldAddressInfo;
  private BigInteger publicFromAmount;
  List<Note> shieldOutList = new ArrayList<>();

  private String httpnode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(0);
  Long startNum;
  Long endNum;

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    publicFromAmount = getRandomAmount();
    startNum = HttpMethed.getNowBlockNum(httpnode);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, threadPoolSize = 30, invocationCount = 30)
  public void test01ShieldTrc20TransactionByTypeMint() throws Exception {
    ManagedChannel channelFull = null;
    WalletGrpc.WalletBlockingStub blockingStubFull = null;
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    ManagedChannel channelFull1 = null;
    WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);



    BigInteger publicFromAmount = getRandomAmount();
    Optional<ShieldedAddressInfo> sendShieldAddressInfo = getNewShieldedAddress(blockingStubFull);
    Optional<ShieldedAddressInfo> receiverShieldAddressInfo = getNewShieldedAddress(blockingStubFull);
    String memo = "Shield trc20 from T account to shield account in" + System.currentTimeMillis();
    String sendShieldAddress = sendShieldAddressInfo.get().getAddress();

    List<Note> shieldOutList = new ArrayList<>();
    shieldOutList.clear();
    shieldOutList = addShieldTrc20OutputList(shieldOutList, sendShieldAddress,
        "" + publicFromAmount, memo,blockingStubFull);

    //Create shiled trc20 parameters
    GrpcAPI.ShieldedTRC20Parameters shieldedTrc20Parameters
        = createShieldedTrc20Parameters(publicFromAmount,
        null,null,shieldOutList,"",0L,blockingStubFull
    );
    String data = encodeMintParamsToHexString(shieldedTrc20Parameters, publicFromAmount);

    //Do mint transaction type
    String txid = PublicMethed.triggerContract(shieldAddressByte,
        mint, data, true, 0, maxFeeLimit, zenTrc20TokenOwnerAddress,
        zenTrc20TokenOwnerKey, blockingStubFull);
    sendShieldAddress = sendShieldAddressInfo.get().getAddress();
    List<ShieldedAddressInfo> inputShieldAddressList = new ArrayList<>();
    GrpcAPI.DecryptNotesTRC20 sendNote;
    Integer times = 10;
    while (times-- > 0) {
      receiverShieldAddressInfo = getNewShieldedAddress(blockingStubFull);
      //Scan sender note
      String receiverShieldAddress = receiverShieldAddressInfo.get().getAddress();
      sendNote = scanShieldedTrc20NoteByIvk(sendShieldAddressInfo.get(),
          blockingStubFull1);

      while (sendNote.getNoteTxsCount() == 0) {
        sendNote = scanShieldedTrc20NoteByIvk(sendShieldAddressInfo.get(),
            blockingStubFull1);
      }

      String transferMemo = "Transfer type test " + System.currentTimeMillis();

      shieldOutList.clear();
      shieldOutList = addShieldTrc20OutputList(shieldOutList, receiverShieldAddress,
          "" + publicFromAmount, transferMemo,blockingStubFull1);
      inputShieldAddressList.clear();
      inputShieldAddressList.add(sendShieldAddressInfo.get());
      //inputNoteList.add(senderNote);
      //Create transfer parameters
      shieldedTrc20Parameters
          = createShieldedTrc20Parameters(BigInteger.valueOf(0),
          sendNote,inputShieldAddressList,shieldOutList,"",0L,blockingStubFull1
      );

      data = encodeTransferParamsToHexString(shieldedTrc20Parameters);
      txid = PublicMethed.triggerContract(shieldAddressByte,
          transfer, data, true, 0, maxFeeLimit, zenTrc20TokenOwnerAddress,
          zenTrc20TokenOwnerKey, blockingStubFull);

      sendShieldAddressInfo = receiverShieldAddressInfo;


    }

  }

  /**
   * constructor.
   */
  @Test(enabled = false, threadPoolSize = 1, invocationCount = 1)
  public void test02QueryResult() throws Exception {
    Integer success = 0;
    Integer failed = 0;
    startNum = 31012L;
    endNum = 31095L;
    while (startNum < endNum) {
      HttpResponse response = HttpMethed.getTransactionInfoByBlocknum(httpnode,startNum++);
      List<JSONObject> responseContentByBlocknum = HttpMethed
          .parseResponseContentArray(response);
      for (int i = 0; i < responseContentByBlocknum.size();i++) {
        logger.info(responseContentByBlocknum.get(i).toString());
        logger.info(responseContentByBlocknum.get(i).getJSONObject("receipt").getString("result"));
        if (responseContentByBlocknum.get(i).getJSONObject("receipt").getString("result").equals("SUCCESS")) {
          success++;
        } else {
          failed++;
        }
      }

    }

    logger.info("Success times:" + success);
    logger.info("Failed  times:" + failed);


  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    endNum = HttpMethed.getNowBlockNum(httpnode);
    logger.info("startNum:" + startNum);
    logger.info("endNum:" + endNum);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


