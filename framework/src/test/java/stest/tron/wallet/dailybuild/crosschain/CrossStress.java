package stest.tron.wallet.dailybuild.crosschain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.CrossChainBase;
import stest.tron.wallet.common.client.utils.PublicMethed;


@Slf4j
public class CrossStress extends CrossChainBase {

  private Long sendAmount = 12L;

  AtomicInteger times = new AtomicInteger();

  ConcurrentLinkedDeque<String> queue = new ConcurrentLinkedDeque<>();

  @Test(enabled = true, threadPoolSize = 5, invocationCount = 5)
  public void test01CreateCrossToken() throws Exception {
    Random random = new Random();
    long randNumber = (long)(random.nextInt(1000) + 15);


    String method = "increment(address,address,uint256)";
    String argsStr = "\"" + Base58.encode58Check(contractAddress) + "\"" + "," + "\""
        + Base58.encode58Check(crossContractAddress) + "\"" + ",\"1\"";


    while (times.getAndAdd(1) < 500) {
      randNumber = (long)(random.nextInt(1000) + 200);
      argsStr = "\"" + Base58.encode58Check(contractAddress) + "\"" + "," + "\""
          + Base58.encode58Check(crossContractAddress) + "\"" + ",\"" + randNumber + "\"";
      /*      String txid = createCrossTrc10Transfer(trc10TokenAccountAddress,
          trc10TokenAccountAddress,assetAccountId1,6,randNumber,name1,chainId,crossChainId,
          trc10TokenAccountKey,blockingStubFull);*/

      String txid = createTriggerContractForCross(trc10TokenAccountAddress,registerAccountAddress,
          contractAddress, crossContractAddress, method,argsStr,chainId,crossChainId,
          trc10TokenAccountKey,blockingStubFull);


      queue.offer(txid);
      //Thread.sleep(1000);
    }

  }

  /**
   * constructor.
   */
  @AfterTest
  public void afterStressTest() {
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("Total send transaction: " + queue.size());
    List<String> totalList = new ArrayList<>();
    List<String> cross1List = new ArrayList<>();
    List<String> cross2List = new ArrayList<>();
    Optional<Transaction> byId;
    while (!queue.isEmpty()) {
      String txid = queue.poll();
      totalList.add(txid);
      byId = PublicMethed.getTransactionById(txid, blockingStubFull);
      if (byId.get().getRawData().getContractCount() != 0) {
        cross1List.add(txid);
      }
      byId = PublicMethed.getTransactionById(txid, crossBlockingStubFull);
      if (byId.get().getRawData().getContractCount() != 0) {
        cross2List.add(txid);
      }
    }


    for (int i = 0; i < totalList.size();i++) {


      if (cross1List.contains(totalList.get(i)) && !cross2List.contains(totalList.get(i))) {
        logger.info("txid : " + totalList.get(i) + " , not package in para chain");
        continue;
      }

      if (!cross1List.contains(totalList.get(i)) && !cross2List.contains(totalList.get(i))) {
        logger.info("txid : " + totalList.get(i) + " , not package in any chain");
        continue;
      }


    }


  }
}


