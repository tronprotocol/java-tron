package stest.tron.wallet.dailybuild.crosschain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.CrossChainBase;
import stest.tron.wallet.common.client.utils.PublicMethed;


@Slf4j
public class CrossStress extends CrossChainBase {

  private Long sendAmount = 12L;

  AtomicInteger times = new AtomicInteger();

  ConcurrentLinkedDeque<String> queue = new ConcurrentLinkedDeque<>();

  @Test(enabled = true, threadPoolSize = 23, invocationCount = 23)
  public void test01CreateCrossToken() throws Exception {
    Random random = new Random();
    long randNumber = (long)(random.nextInt(1000) + 15);


    String method = "increment(address,address,uint256)";
    String argsStr = "\"" + Base58.encode58Check(contractAddress) + "\"" + "," + "\""
        + Base58.encode58Check(crossContractAddress) + "\"" + ",\"1\"";

    String txid = "";
    while (times.getAndAdd(1) < 10000) {
      randNumber = (long)(random.nextInt(100) + 200);
      argsStr = "\"" + Base58.encode58Check(contractAddress) + "\"" + "," + "\""
          + Base58.encode58Check(crossContractAddress) + "\"" + ",\"" + randNumber + "\"";

      txid = createCrossTrc10Transfer(trc10TokenAccountAddress,
          trc10TokenAccountAddress,assetAccountId1,chainId,6,randNumber,name1,chainId,crossChainId,
          trc10TokenAccountKey,blockingStubFull);


      txid = createCrossTrc10Transfer(trc10TokenAccountAddress,
          trc10TokenAccountAddress,assetAccountIdCrossChain,crossChainId,6,
          randNumber,name2,crossChainId,chainId,
          trc10TokenAccountKey,crossBlockingStubFull);
      logger.info("name2:" + name2);
      logger.info("name2 token id:" + assetAccountId2.toStringUtf8());
      logger.info("name2 token id:" +  ByteArray.toStr(assetAccountId2.toByteArray()));
      if (txid != null) {
        queue.offer(txid);
      }


      createCrossTrc10Transfer(trc10TokenAccountAddress,
          trc10TokenAccountAddress,assetAccountId1,chainId,6,randNumber - 99,name1,
          crossChainId,chainId,
          trc10TokenAccountKey,crossBlockingStubFull);

      createCrossTrc10Transfer(trc10TokenAccountAddress,
          trc10TokenAccountAddress,assetAccountId1,chainId,6,randNumber - 99,name1,
          crossChainId,chainId,
          trc10TokenAccountKey,crossBlockingStubFull);

      txid = createTriggerContractForCross(trc10TokenAccountAddress,registerAccountAddress,
          contractAddress, crossContractAddress, method,argsStr,chainId,crossChainId,
          trc10TokenAccountKey,blockingStubFull);
      //queue.offer(txid);

      argsStr = "\"" + Base58.encode58Check(crossContractAddress) + "\"" + "," + "\""
          + Base58.encode58Check(contractAddress) + "\"" + ",\"" + randNumber + "\"";

      txid = createTriggerContractForCross(trc10TokenAccountAddress,registerAccountAddress,
          crossContractAddress, contractAddress, method,argsStr,crossChainId,chainId,
          trc10TokenAccountKey,crossBlockingStubFull);
      //queue.offer(txid);

      /*
      PublicMethed.transferAsset(foundationAddress, assetAccountId1.toByteArray(),
          randNumber  - 50, trc10TokenAccountAddress, trc10TokenAccountKey, blockingStubFull);
      PublicMethed.transferAsset(foundationAddress, assetAccountId2.toByteArray(),
          randNumber  - 50, trc10TokenAccountAddress, trc10TokenAccountKey, crossBlockingStubFull);
      PublicMethed.transferAsset(foundationAddress, assetAccountIdCrossChain.toByteArray(),
          randNumber  - 50, trc10TokenAccountAddress, trc10TokenAccountKey, crossBlockingStubFull);
      */

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

    //logger.info("Before register balance for chain A:" + registerBeforeBalance);
    logger.info("After register balance for chain A:"
        + PublicMethed.queryAccount(registerAccountAddress,blockingStubFull).getBalance());
    //logger.info("Befpre register balance for chain B:" + crossRegisterBeforeBalance);



    logger.info("After register balance for chain B:"
        + PublicMethed.queryAccount(registerAccountAddress,crossBlockingStubFull).getBalance());

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


