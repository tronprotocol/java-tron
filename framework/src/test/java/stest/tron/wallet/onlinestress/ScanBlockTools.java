package stest.tron.wallet.onlinestress;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.TransactionInfoList;
import org.tron.api.WalletGrpc;
import org.tron.common.utils.Base58;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Account.AccountResource;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.AccountContract.AccountCreateContract;
import org.tron.protos.contract.AccountContract.AccountPermissionUpdateContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.TransferAssetContract;
import org.tron.protos.contract.BalanceContract.FreezeBalanceContract;
import org.tron.protos.contract.BalanceContract.TransferContract;
import org.tron.protos.contract.BalanceContract.UnfreezeBalanceContract;
import org.tron.protos.contract.BalanceContract.WithdrawBalanceContract;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;
import org.tron.protos.contract.WitnessContract.VoteWitnessContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Sha256Hash;
import stest.tron.wallet.common.client.utils.Sha256Sm3Hash;

@Slf4j
public class ScanBlockTools {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("witness.key5");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("witness.key4");
  private final byte[] testAddress003 = PublicMethed.getFinalAddress(testKey003);

  private final String testKey004 = Configuration.getByPath("testng.conf")
      .getString("witness.key3");
  private final byte[] testAddress004 = PublicMethed.getFinalAddress(testKey004);
  ArrayList<String> txidList = new ArrayList<String>();
  Optional<TransactionInfo> infoById = null;
  Long beforeTime;
  Long afterTime;
  Long beforeBlockNum;
  Long afterBlockNum;
  Block currentBlock;
  Long currentBlockNum;
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode =  "39.106.110.245:50051";
  private String fullnode1 =  "39.106.110.245:50051";



  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testKey002);
    PublicMethed.printAddress(testKey003);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
    currentBlock = blockingStubFull1.getNowBlock(EmptyMessage.newBuilder().build());
    beforeBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    beforeTime = System.currentTimeMillis();
  }

  public static List<String> listForTxid = new ArrayList<>();
  public static HashMap<String,Integer> map = new HashMap<>();
  public static HashMap<Integer,String> witnessMap = new HashMap<>();

  @Test(enabled = true,threadPoolSize = 1, invocationCount = 1)
  public void test01ScanTransaction() {
    getTxidList();
    witnessMap.clear();
    map.clear();
    witnessMap.put(5,"41F08012B4881C320EB40B80F1228731898824E09D");
    witnessMap.put(10,"41DF309FEF25B311E7895562BD9E11AAB2A58816D2");
    witnessMap.put(15,"41BB7322198D273E39B940A5A4C955CB7199A0CDEE");
    witnessMap.put(20,"412080D8A0364E82150DD5235CE7A61A7B40F3F9EF");
    witnessMap.put(25,"4173FC381D3E2AFEFCCED94A57D49520291C38AFBB");
    witnessMap.put(30,"41AF6146B0AD9EE8BBEE811D5858F3252666DFC90C");
    witnessMap.put(35,"41AF6A9D9C0636BD9DF74F687B90C6F44C471A6AB3");
    witnessMap.put(40,"41AF730429E4AB7BF7B53FB15ACB1D45EF5B22F463");
    witnessMap.put(45,"41AF4AEA1C4CBCFA681D98C354C142938381C99389");
    witnessMap.put(50,"41AF53DC31D9DE64DFF59A847125EFCA89D97BC86D");
    witnessMap.put(55,"41AF49468FA1BA966244D76F7D0139FC2CA751FAA5");
    witnessMap.put(60,"41AF5360256F958D2A922D160C429F13D432EFC22F");
    witnessMap.put(65,"41AF5EF33FD79FECB0419A5688035D7BCD3AEFE236");
    witnessMap.put(70,"41AF68F90ED62BA9F6F7A7EABA384E417551CF83E5");
    witnessMap.put(75,"41AF619F8CE75A9E95A19E851BEBE63E89FCB1826E");
    witnessMap.put(80,"41AF71E98F91515D7E5D5379837B9EEFD1AB4650D2");
    witnessMap.put(85,"41AF498B43EE098B26926798CFEAE1AB1154EF4430");
    witnessMap.put(90,"41AF536672333170CB0FBFA78819CD90A05537D872");
    witnessMap.put(95,"41AF5FAC2D62DD1F5C9892BA9D6593337ABBEAAACB");
    witnessMap.put(100,"41AF6981D4562E7B0A6C9E8F8C22D4CCCD03D2F39A");
    witnessMap.put(105,"41AF72A34243836238A533B7E77F3B2B29FD056B14");
    witnessMap.put(110,"41AF49C25D14AED36186B7C89AF405EF37A01EF23D");
    witnessMap.put(115,"41AF53BA37D394575CAD99A2A2C5BE56DEA0227C87");
    witnessMap.put(120,"41AF6A761C941AE2CDC75890D9900AC4B89B7EFCDD");
    witnessMap.put(125,"41AF72B56845F0C4D37388B6E6DC3601A0538ABA71");
    witnessMap.put(130,"41AF4ACF25C1E192285C9BA98522CB3CF20FFBE392");
    witnessMap.put(100000,"416C0214C9995C6F3A61AB23F0EB84B0CDE7FD9C7C");



    for (String txid : listForTxid) {

      long blockNum = PublicMethed.getTransactionInfoById(txid,blockingStubFull)
          .get().getBlockNumber();
      String witnessAddress = ByteArray.toHexString(PublicMethed
          .getBlock(blockNum,blockingStubFull).getBlockHeader().getRawData()
          .getWitnessAddress().toByteArray());

      map.put(witnessAddress.toLowerCase(), map.getOrDefault(witnessAddress,0) + 1);
      logger.info("end");
    }

  }




  @Test(enabled = true,threadPoolSize = 1, invocationCount = 1)
  public void test02ScanBlockGetTransactionAndWriteToCsv() {
    witnessMap.clear();
    map.clear();
    witnessMap.put(5,"41F08012B4881C320EB40B80F1228731898824E09D");
    witnessMap.put(10,"41DF309FEF25B311E7895562BD9E11AAB2A58816D2");
    witnessMap.put(15,"41BB7322198D273E39B940A5A4C955CB7199A0CDEE");
    witnessMap.put(20,"412080D8A0364E82150DD5235CE7A61A7B40F3F9EF");
    witnessMap.put(25,"4173FC381D3E2AFEFCCED94A57D49520291C38AFBB");
    witnessMap.put(30,"41AF6146B0AD9EE8BBEE811D5858F3252666DFC90C");
    witnessMap.put(35,"41AF6A9D9C0636BD9DF74F687B90C6F44C471A6AB3");
    witnessMap.put(40,"41AF730429E4AB7BF7B53FB15ACB1D45EF5B22F463");
    witnessMap.put(45,"41AF4AEA1C4CBCFA681D98C354C142938381C99389");
    witnessMap.put(50,"41AF53DC31D9DE64DFF59A847125EFCA89D97BC86D");
    witnessMap.put(55,"41AF49468FA1BA966244D76F7D0139FC2CA751FAA5");
    witnessMap.put(60,"41AF5360256F958D2A922D160C429F13D432EFC22F");
    witnessMap.put(65,"41AF5EF33FD79FECB0419A5688035D7BCD3AEFE236");
    witnessMap.put(70,"41AF68F90ED62BA9F6F7A7EABA384E417551CF83E5");
    witnessMap.put(75,"41AF619F8CE75A9E95A19E851BEBE63E89FCB1826E");
    witnessMap.put(80,"41AF71E98F91515D7E5D5379837B9EEFD1AB4650D2");
    witnessMap.put(85,"41AF498B43EE098B26926798CFEAE1AB1154EF4430");
    witnessMap.put(90,"41AF536672333170CB0FBFA78819CD90A05537D872");
    witnessMap.put(95,"41AF5FAC2D62DD1F5C9892BA9D6593337ABBEAAACB");
    witnessMap.put(100,"41AF6981D4562E7B0A6C9E8F8C22D4CCCD03D2F39A");
    witnessMap.put(105,"41AF72A34243836238A533B7E77F3B2B29FD056B14");
    witnessMap.put(110,"41AF49C25D14AED36186B7C89AF405EF37A01EF23D");
    witnessMap.put(115,"41AF53BA37D394575CAD99A2A2C5BE56DEA0227C87");
    witnessMap.put(120,"41AF6A761C941AE2CDC75890D9900AC4B89B7EFCDD");
    witnessMap.put(125,"41AF72B56845F0C4D37388B6E6DC3601A0538ABA71");
    witnessMap.put(130,"41AF4ACF25C1E192285C9BA98522CB3CF20FFBE392");
    witnessMap.put(100000,"416C0214C9995C6F3A61AB23F0EB84B0CDE7FD9C7C");


    Long startNum = 30855000L;
    Long endNum = 30858000L;

    Integer totalNum = 0;
    Integer successNum = 0;
    Integer failedNum = 0;
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    while (endNum >= startNum) {
      logger.info("scan block num:" + endNum);
      builder.setNum(endNum);
      Block block = blockingStubFull1.getBlockByNum(builder.build());
      List<Transaction>  transactionList = block.getTransactionsList();
      map.put(ByteArray.toHexString(block.getBlockHeader().getRawData().getWitnessAddress()
          .toByteArray()).toLowerCase(),
          map.getOrDefault(ByteArray.toHexString(block.getBlockHeader().getRawData()
              .getWitnessAddress().toByteArray()).toLowerCase(),0) + 1);
      Integer transactionNumInThisBlock = transactionList.size();
      totalNum = totalNum + transactionNumInThisBlock;
      for (Transaction transaction : transactionList) {
        String txid = ByteArray.toHexString(Sha256Hash.hash(true,
            transaction.getRawData().toByteArray()));
        //String writeData = ByteArray.toHexString(Sha256Hash.hash(true,
        // transaction.getRawData().toByteArray()));
        writeDataToCsvFile("txid-stressss.csv",txid);
        //System.out.println("Fee:" + PublicMethed.getTransactionInfoById(txid,
        // blockingStubFull).get().getFee());
      }
      for (Transaction transaction : transactionList) {
        if (transaction.getRet(0).getContractRet().name().equals("SUCCESS")) {
          successNum++;
        } else {
          failedNum++;

          String writeData = ByteArray.toHexString(Sha256Hash.hash(true,
              transaction.getRawData().toByteArray()));
          logger.info(writeData);
          writeDataToCsvFile("28164160L-28167324L.csv",writeData);
        }
      }
      endNum--;
    }

    logger.info("successNum:" + successNum);
    logger.info("failedNum:" + failedNum);
    logger.info("totalNum:" + totalNum);
    logger.info("Success rate:" + (double)failedNum / (double)totalNum);


  }

  public static Account account;
  public HashSet<ByteString> addressSet = new HashSet<>();
  public HashSet<ByteString> assetIssueSet = new HashSet<>();

  @Test(enabled = true, description = "Get account from transaction and compare "
      + "account info from two different node")
  public void test03CompareTwoNodeAccountStatus() throws Exception {
    account = PublicMethed.queryAccount(
        "7400E3D0727F8A61041A8E8BF86599FE5597CE19DE451E59AED07D60967A5E25",blockingStubFull);
    //扫描到28307530块了
    Long startNum = 29266108L;
    Long endNum = 29266208L;
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(startNum);
    int retryTimes = 0;
    HashSet<ByteString> set = new HashSet<>();
    while (startNum++ <= endNum) {
      //Block block = blockingStubFull412.getNowBlock(EmptyMessage.newBuilder().build());
      builder.setNum(startNum);
      Block block = blockingStubFull.getBlockByNum(builder.build());
      logger.info("Start to scan block :" + block.getBlockHeader().getRawData().getNumber());

      List<Transaction> transactionList = block.getTransactionsList();
      for (Transaction transaction : transactionList) {

        Any any = transaction.getRawData().getContract(0).getParameter();
        Integer contractType =  transaction.getRawData().getContract(0).getType().getNumber();


        try {
          switch (contractType) {
            case 1:
              TransferContract transferContract = any.unpack(TransferContract.class);
              set.add(transferContract.getOwnerAddress());
              break;
            case 2:
              TransferAssetContract transferAssetContract = any.unpack(TransferAssetContract.class);
              doCheck(transferAssetContract.getOwnerAddress());
              if (!addressSet.contains(transferAssetContract.getAssetName())) {
                Assert.assertEquals(PublicMethed.getAssetIssueById(ByteArray
                        .toStr(transferAssetContract.getAssetName().toByteArray()),
                    blockingStubFull),
                    PublicMethed.getAssetIssueById(ByteArray.toStr(transferAssetContract
                        .getAssetName().toByteArray()), blockingStubFull));
                addressSet.add(transferAssetContract.getAssetName());
                logger.info("check token " + ByteArray.toStr(transferAssetContract
                    .getAssetName().toByteArray()) + " successfully");
              }
              break;
            case 31:
              TriggerSmartContract triggerSmartContract = any.unpack(TriggerSmartContract.class);
              doCheck(triggerSmartContract.getOwnerAddress());
              break;
            case 13:
              WithdrawBalanceContract withdrawBalanceContract
                  = any.unpack(WithdrawBalanceContract.class);
              doCheck(withdrawBalanceContract.getOwnerAddress());
              break;
            case 11:
              FreezeBalanceContract freezeBalanceContract = any.unpack(FreezeBalanceContract.class);
              doCheck(freezeBalanceContract.getOwnerAddress());
              break;
            case 0:
              AccountCreateContract accountCreateContract = any.unpack(AccountCreateContract.class);
              doCheck(accountCreateContract.getOwnerAddress());
              break;
              /* case 4:
              VoteWitnessContract voteWitnessContract = any.unpack(VoteWitnessContract.class);
              doCheck(voteWitnessContract.getOwnerAddress());*/
            case 12:
              UnfreezeBalanceContract unfreezeBalanceContract
                  = any.unpack(UnfreezeBalanceContract.class);
              doCheck(unfreezeBalanceContract.getOwnerAddress());
              break;
            case 30:
              CreateSmartContract createSmartContract = any.unpack(CreateSmartContract.class);
              doCheck(createSmartContract.getOwnerAddress());
              break;
            case 46:
              AccountPermissionUpdateContract accountPermissionUpdateContract
                  = any.unpack(AccountPermissionUpdateContract.class);
              doCheck(accountPermissionUpdateContract.getOwnerAddress());
              break;
            default:
              logger.info("Unknown type:" + contractType);
              continue;

          }
        } catch (Exception e) {
          e.printStackTrace();

        }





      }
    }


  }


  @Test(enabled = true, description = "Get all info from smart contract transaction list")
  public void test04GetEventTransactionAllInfoList() throws Exception {



    HashSet<String> contractAndTopicList = new HashSet<>();


    Long startNum = 33662515L - 9500;
    Long endNum = startNum - 1000;

    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(startNum);
    int retryTimes = 0;
    HashSet<ByteString> set = new HashSet<>();
    while (startNum-- >= endNum) {
      logger.info("current block num:" + startNum);
      builder.setNum(startNum);
      TransactionInfoList transactionInfoList = blockingStubFull
          .getTransactionInfoByBlockNum(builder.build());

      for (TransactionInfo transactionInfo : transactionInfoList.getTransactionInfoList()) {
        if (!transactionInfo.getContractAddress().isEmpty() && transactionInfo.getLogCount() > 0) {
          try {
            String txid = ByteArray.toHexString(transactionInfo.getId().toByteArray());
            Any any = PublicMethed.getTransactionById(txid, blockingStubFull).get().getRawData()
                .getContract(0).getParameter();
            TriggerSmartContract triggerSmartContract = any.unpack(TriggerSmartContract.class);
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(ByteArray.toHexString(triggerSmartContract
                .getOwnerAddress().toByteArray()));
            stringBuffer.append(",");
            stringBuffer.append(ByteArray.toHexString(transactionInfo
                .getContractAddress().toByteArray()));
            stringBuffer.append(",");
            stringBuffer.append(ByteArray.toHexString(triggerSmartContract
                .getData().toByteArray()));
            stringBuffer.append(",");
            //stringBuffer.append(blockHash);
            //stringBuffer.append(",");
            //stringBuffer.append(startNum);
            //stringBuffer.append(",");
            stringBuffer.append(txid);




            contractAndTopicList.add(stringBuffer.toString());

            ;
          } catch (Exception e) {
            e.printStackTrace();

          }
        }




      }
    }

    for (String contractAddressAndTopic : contractAndTopicList) {
      writeDataToCsvFile("eth_blockHash.csv", contractAddressAndTopic);
    }



  }


  @Test(enabled = true, description = "Get eth block query information")
  public void test05CreateEthBlockHash() throws Exception {
    HashSet<String> contractAndTopicList = new HashSet<>();


    Long startNum = 33662515L;
    Long endNum = startNum - 20000;

    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(startNum);
    int retryTimes = 0;
    HashSet<ByteString> set = new HashSet<>();
    while (startNum-- >= endNum) {
      logger.info("current block num:" + startNum);
      builder.setNum(startNum);
      String blockHash = ByteArray.toHexString(PublicMethed.getBlock(startNum + 1,
          blockingStubFull).getBlockHeader().getRawData().getParentHash().toByteArray());
      StringBuffer stringBuffer = new StringBuffer();
      stringBuffer.append(blockHash);
      stringBuffer.append(",");
      stringBuffer.append(startNum);
      contractAndTopicList.add(stringBuffer.toString());
    }

    for (String contractAddressAndTopic : contractAndTopicList) {
      writeDataToCsvFile("eth_blockHash.csv", contractAddressAndTopic);
    }



  }


  ConcurrentHashMap<ByteString,Integer> certificationCosts = new ConcurrentHashMap<>();
  Set<ByteString> concurrentHashSet = certificationCosts.newKeySet();
  private static HashSet<String> existAddress = new HashSet<>();
  List<Long> list1 = new ArrayList<>();

  private static AtomicLong blockNum = new AtomicLong(30000523L - 20000L);
  private static AtomicLong times = new AtomicLong(5);

  @Test(enabled = true, threadPoolSize = 10, invocationCount = 10)
  public void test06ScanMainNetMostActiveAccounts() throws Exception {
    getNowAddressList();
    ManagedChannel channelFull = null;
    WalletGrpc.WalletBlockingStub blockingStubFull = null;
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    long blockNumCurrent = blockNum.getAndAdd(-200);
    int times = 200;
    while (times-- >= 0) {
      if (concurrentHashSet.size() > 1000000) {
        break;
      }
      //list1.add(blockNumCurrent);
      builder.setNum(blockNumCurrent--);
      Block block = blockingStubFull.getBlockByNum(builder.build());
      logger.info("Start to scan block :" + block.getBlockHeader().getRawData().getNumber());

      List<Transaction> transactionList = block.getTransactionsList();
      for (Transaction transaction : transactionList) {

        Any any = transaction.getRawData().getContract(0).getParameter();
        Integer contractType =  transaction.getRawData().getContract(0).getType().getNumber();


        try {
          switch (contractType) {
            case 1:
              TransferContract transferContract = any.unpack(TransferContract.class);
              isExist(transferContract.getOwnerAddress());
              isExist(transferContract.getToAddress());
              break;
            case 2:
              TransferAssetContract transferAssetContract = any.unpack(TransferAssetContract.class);
              isExist(transferAssetContract.getOwnerAddress());
              isExist(transferAssetContract.getToAddress());
              break;
            case 31:
              TriggerSmartContract triggerSmartContract = any.unpack(TriggerSmartContract.class);
              isExist(triggerSmartContract.getContractAddress());
              isExist(triggerSmartContract.getOwnerAddress());
              break;
            case 13:
              WithdrawBalanceContract withdrawBalanceContract
                  = any.unpack(WithdrawBalanceContract.class);

              isExist(withdrawBalanceContract.getOwnerAddress());
              break;
            case 11:
              FreezeBalanceContract freezeBalanceContract = any.unpack(FreezeBalanceContract.class);
              isExist(freezeBalanceContract.getOwnerAddress());
              break;
            case 0:
              AccountCreateContract accountCreateContract = any.unpack(AccountCreateContract.class);
              isExist(accountCreateContract.getOwnerAddress());
              isExist(accountCreateContract.getAccountAddress());
              break;
            case 12:
              UnfreezeBalanceContract unfreezeBalanceContract
                  = any.unpack(UnfreezeBalanceContract.class);
              isExist(unfreezeBalanceContract.getOwnerAddress());
              break;
            case 30:
              CreateSmartContract createSmartContract = any.unpack(CreateSmartContract.class);
              isExist(createSmartContract.getOwnerAddress());
              break;
            case 46:
              AccountPermissionUpdateContract accountPermissionUpdateContract
                  = any.unpack(AccountPermissionUpdateContract.class);
              isExist(accountPermissionUpdateContract.getOwnerAddress());
              break;
            case 4:
              VoteWitnessContract voteWitnessContract = any.unpack(VoteWitnessContract.class);
              isExist(voteWitnessContract.getOwnerAddress());
              break;
            default:
              logger.info("Unknown type:" + contractType);
              continue;

          }
        } catch (Exception e) {
          e.printStackTrace();

        }






      }
    }




  }



  @Test(enabled = true, threadPoolSize = 1, invocationCount = 1)
  public void test07ScanAndCalculateTotalValueOrCallValue() throws Exception {
    getNowAddressList();
    ManagedChannel channelFull = null;
    WalletGrpc.WalletBlockingStub blockingStubFull = null;
    channelFull = ManagedChannelBuilder.forTarget("47.252.19.181:50051")
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    int total1 = 0;
    int total2 = 0;
    int totalTrx = 0;
    for (long blockNum = 20450668L; blockNum <= 20450790L;blockNum++) {
      System.out.println("blockNum  " + blockNum);
      TransactionInfoList transactionList = PublicMethed.getTransactionInfoByBlockNum(blockNum,
          blockingStubFull).get();
      for (int i = 0; i < transactionList.getTransactionInfoCount();i++) {
        if (ByteArray.toHexString(transactionList.getTransactionInfo(i).getContractAddress()
            .toByteArray()).equalsIgnoreCase("41DF42D1936F0DC3689BB65A19F279747084E13FBD")) {


          if (ByteArray.toHexString(transactionList.getTransactionInfo(i).getLog(0)
              .getTopics(0).toByteArray()).equalsIgnoreCase(
              "9b217a401a5ddf7c4d474074aff9958a18d48690d77cc2151c4706aa7348b401")) {
            total1 += Integer.parseInt(ByteArray.toHexString(transactionList.getTransactionInfo(i)
                .getLog(0).getData().toByteArray()),16);
          } else if (ByteArray.toHexString(transactionList.getTransactionInfo(i).getLog(0)
              .getTopics(0).toByteArray()).equalsIgnoreCase(
                  "31472eae9e158460fea5622d1fcb0c5bdc65b6ffb51827f7bc9ef5788410c34c")) {
            total2 += Integer.parseInt(ByteArray.toHexString(transactionList.getTransactionInfo(i)
                .getLog(0).getData().toByteArray()),16);
          } else if (ByteArray.toHexString(transactionList.getTransactionInfo(i).getLog(0)
              .getTopics(0).toByteArray()).equalsIgnoreCase(
                  "3e799b2d61372379e767ef8f04d65089179b7a6f63f9be3065806456c7309f1b")) {
            totalTrx += transactionList.getTransactionInfo(i).getInternalTransactions(4)
                .getCallValueInfo(0).getCallValue();
          }

        }
      }
    }

    System.out.println("total1 :" + total1);
    System.out.println("total2 :" + total2);
    System.out.println("total_callValue                          :" + totalTrx);

  }



  @Test
  public void test08ScanAndCalculateWitnessProductBlockStatus() {
    Long startNum = 33694340L;
    Long endNum = 33694388L;

    Integer testgroup014 = 0;
    Integer testgroup015 = 0;
    Integer testgroup016 = 0;
    Integer testgroup017 = 0;
    Integer testgroup018 = 0;

    int transfer = 0;
    int trigger = 0;

    while (startNum++ <= endNum) {
      NumberMessage.Builder builder = NumberMessage.newBuilder();
      builder.setNum(startNum);
      Block block = blockingStubFull.getBlockByNum(builder.build());
      logger.info("current block:" + startNum);

      String currentAddress = ByteArray.toHexString(block.getBlockHeader().getRawData()
          .getWitnessAddress().toByteArray());
      List<Transaction> transactionList = block.getTransactionsList();
      for (Transaction transaction : transactionList) {
        if (transaction.getRawData().getContract(0).getType().equals(ContractType
            .TriggerSmartContract)) {
          trigger++;
        } else {
          transfer++;
        }
      }
      if (currentAddress.equals(getHexAddress(
          "0528dc17428585fc4dece68b79fa7912270a1fe8e85f244372f59eb7e8925e04"))
          || currentAddress
          .equals(getHexAddress(
              "dbc78781ad27f3751358333412d5edc85b13e5eee129a1a77f7232baadafae0e"))
          || currentAddress
          .equals(getHexAddress(
              "a79a37a3d868e66456d76b233cb894d664b75fd91861340f3843db05ab3a8c66"))
          || currentAddress
          .equals(getHexAddress(
              "a8107ea1c97c90cd4d84e79cd79d327def6362cc6fd498fc3d3766a6a71924f6"))
          || currentAddress
          .equals(getHexAddress(
              "b5076206430b2ca069ae2f4dc6f20dd0d74551559878990d1df12a723c228039"))
          || currentAddress
          .equals(getHexAddress(
              "87cc8832b1b4860c3c69994bbfcdae9b520e6ce40cbe2a90566e707a7e04fc70"))
      ) {
        testgroup014++;
        continue;
      }

      if (currentAddress.equals(getHexAddress(
          "553c7b0dee17d3f5b334925f5a90fe99fb0b93d47073d69ec33eead8459d171e"))
          || currentAddress
          .equals(getHexAddress(
              "541a2d585fcea7e9b1803df4eb49af0eb09f1fa2ce06aa5b8ed60ac95655d66d"))
          || currentAddress
          .equals(getHexAddress(
              "7d5a7396d6430edb7f66aa5736ef388f2bea862c9259de8ad8c2cfe080f6f5a0"))
          || currentAddress
          .equals(getHexAddress(
              "7c4977817417495f4ca0c35ab3d5a25e247355d68f89f593f3fea2ab62c8644f"))
          || currentAddress
          .equals(getHexAddress(
              "4521c13f65cc9f5c1daa56923b8598d4015801ad28379675c64106f5f6afec30"))
          || currentAddress
          .equals(getHexAddress(
              "442513e2e801bc42d14d33b8148851dae756d08eeb48881a44e1b2002b3fb700"))
      ) {
        testgroup015++;
        continue;
      }

      if (currentAddress.equals(getHexAddress(
          "324a2052e491e99026442d81df4d2777292840c1b3949e20696c49096c6bacb8"))
          || currentAddress
          .equals(getHexAddress(
              "f33101ea976d90491dcb9669be568db8bbc1ad23d90be4dede094976b67d550e"))
          || currentAddress
          .equals(getHexAddress(
              "1bb32958909299db452d3c9bbfd15fd745160d63e4985357874ee57708435a00"))
          || currentAddress
          .equals(getHexAddress(
              "29c91bd8b27c807d8dc2d2991aa0fbeafe7f54f4de9fac1e1684aa57242e3922"))
          || currentAddress
          .equals(getHexAddress(
              "97317d4d68a0c5ce14e74ad04dfc7521f142f5c0f247b632c8f94c755bdbe669"))
      ) {
        testgroup016++;
        continue;
      }

      if (currentAddress.equals(getHexAddress(
          "ff5d867c4434ac17d264afc6696e15365832d5e8000f75733ebb336d66df148d"))
          || currentAddress
          .equals(getHexAddress(
              "1fe1d91bbe3ac4ac5dc9866c157ef7615ec248e3fd4f7d2b49b0428da5e046b2"))
          || currentAddress
          .equals(getHexAddress(
              "7c37ef485e186e07952bcc8e30cd911a6cd9f2a847736c89132762fb67a42329"))
          || currentAddress
          .equals(getHexAddress(
              "bcc142d57d872cd2cc1235bca454f2efd5a87f612856c979cc5b45a7399272a8"))
          || currentAddress
          .equals(getHexAddress(
              "6054824dc03546f903a06da1f405e72409379b83395d0bbb3d4563f56e828d52"))
      ) {
        testgroup017++;
        continue;
      }

      testgroup018++;
    }


    logger.info(testgroup014 + "   " + testgroup015 + "   "
        + testgroup016 + "   " +   testgroup017 + "   " + testgroup018);

    logger.info(transfer + "     " + trigger);


  }


  @Test
  public void test09GetEthFilterData() {

    HashSet<String> set = new HashSet<>();
    Integer startBlockNumber = 35129811 - 2000;
    Integer endBlockNumber = startBlockNumber - 3000;

    for (int blockNumber = startBlockNumber; blockNumber >= endBlockNumber;blockNumber--
    ) {
      set.clear();
      HttpResponse response = HttpMethed
          .getTransactionInfoByBlocknum("1.1.1.1:90", blockNumber);

      List<JSONObject> content = HttpMethed.parseResponseContentArray(response);

      String blockNumberHex = "0x" + Integer.toHexString(blockNumber);

      System.out.println(content.size());
      for (JSONObject info : content) {
        if (!info.containsKey("log")) {
          continue;
        }
        JSONArray logArray = info.getJSONArray("log");
        for (int i = 0; i < logArray.size();i++) {
          JSONObject log = logArray.getJSONObject(i);
          String address = "0x" + log.getString("address");
          String topic = "0x" + log.getJSONArray("topics").getString(0);
          set.add(address + "," + topic + "," + blockNumberHex);

        }




      }

      for (String data : set) {
        writeDataToCsvFile("ys_filter_api.csv", data);
      }
    }


  }

  public String getHexAddress(String key) {
    return ByteArray.toHexString(PublicMethed.getFinalAddress(key));
  }

  private static HashSet<String> getFileList(String fileName,HashSet<String> set) {
    String line = null;
    try {
      BufferedReader bufferedReader =
          new BufferedReader(new InputStreamReader(new FileInputStream(fileName),"utf-8"));

      while ((line = bufferedReader.readLine()) != null) {
        set.add(line);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return set;
  }




  private static void getNowAddressList() {
    String line = null;
    try {
      //BufferedReader bufferedReader=new BufferedReader(new FileReader(filePath));
      BufferedReader bufferedReader =
          new BufferedReader(new InputStreamReader(new FileInputStream("newAddress.csv"),"utf-8"));

      //int i=0;
      while ((line = bufferedReader.readLine()) != null) {
        existAddress.add(line);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  /**
   * constructor.
   */
  public static void writeDataToCsvFile(String fileName,String writeData) {

    {
      try {
        File file = new File(fileName);

        if (!file.exists()) {
          file.createNewFile();
        }
        FileWriter fileWritter = new FileWriter(file.getName(), true);
        fileWritter.write(writeData + "\n");
        fileWritter.close();
        //System.out.println("finish");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }


  /**
   * constructor.
   */
  public void doCheck(ByteString address) throws Exception {
    if (addressSet.contains(address)) {
      //logger.info("skip :" + ByteArray.toHexString(address.toByteArray()));
      return;
    } else {
      addressSet.add(address);
    }
    logger.info("checking :" + ByteArray.toHexString(address.toByteArray()));
    compareTwoAddress(address);
    compareNet(address);
    compareAccountResource(address);
    return;

  }

  /**
   * constructor.
   */
  public void compareTwoAddress(ByteString address) {

    Assert.assertEquals(
        PublicMethed.queryAccount(address.toByteArray(),blockingStubFull).toBuilder()
            .clearFreeAssetNetUsageV2()
            //.putAllFreeAssetNetUsageV2(account.getFreeAssetNetUsageV2Map())
            .setBalance(1L).setLatestOprationTime(1L)
            .setAccountResource(AccountResource.newBuilder())
            .setFreeNetUsage(1L)
            .setNetUsage(1L)
            .clearAssetV2()
            .setLatestConsumeFreeTime(1L)
            .setLatestConsumeTime(1L)
            .setAllowance(1L)
            .clearAccountResource()
            //.clearOldVotePower()
            .build(),
        PublicMethed.queryAccount(address.toByteArray(),blockingStubFull).toBuilder()
            .clearFreeAssetNetUsageV2()
            //.putAllFreeAssetNetUsageV2(account.getFreeAssetNetUsageV2Map())
            .setBalance(1L).setLatestOprationTime(1L)
            .setAccountResource(AccountResource.newBuilder())
            .setFreeNetUsage(1L)
            .setNetUsage(1L)
            .setLatestConsumeFreeTime(1L)
            .setLatestConsumeTime(1L)
            .clearAssetV2()
            .setAllowance(1L)
            .clearAccountResource()
            .build()
    );

  }


  /**
   * constructor.
   */
  public void compareNet(ByteString address) {
    Assert.assertEquals(
        PublicMethed.getAccountNet(address.toByteArray(),blockingStubFull)
            .toBuilder().setTotalNetWeight(1L)
            .setNetUsed(1L)
            .setFreeNetUsed(1)
            .setNetLimit(1)
            .build(),
        PublicMethed.getAccountNet(address.toByteArray(),blockingStubFull)
            .toBuilder().setTotalNetWeight(1L)
            .setNetUsed(1L)
            .setFreeNetUsed(1)
            .setNetLimit(1)
            .build()
    );
  }

  /**
   * constructor.
   */
  public void compareAccountResource(ByteString address) throws Exception {
    Assert.assertEquals(
        PublicMethed.getAccountResource(address.toByteArray(),blockingStubFull)
            .toBuilder()
            .setFreeNetUsed(1L)
            .setEnergyUsed(1L)
            .setTotalEnergyWeight(1L)
            .setTotalNetWeight(1L)
            .setNetUsed(1L)
            .setNetLimit(1L)
            .setEnergyLimit(1L)
            .build(),
        PublicMethed.getAccountResource(address.toByteArray(),blockingStubFull)
            .toBuilder()
            .setFreeNetUsed(1L)
            .setEnergyUsed(1L)
            .setNetUsed(1L)
            .setNetLimit(1L)
            .setTotalEnergyWeight(1L)
            .setTotalNetWeight(1L)
            .setEnergyLimit(1L)
            .build()
    );

  }

  /**
   * constructor.
   */
  public boolean isEqual(ByteString address) {
    return PublicMethed.getAccountResource(address.toByteArray(),blockingStubFull)
        .toBuilder()
        .setFreeNetUsed(1L)
        .setEnergyUsed(1L)
        .setTotalEnergyWeight(1L)
        .setTotalNetWeight(1L)
        .setNetUsed(1L)
        .setNetLimit(1L)
        .setEnergyLimit(1L)
        .build().equals(PublicMethed.getAccountResource(address.toByteArray(),blockingStubFull)
            .toBuilder()
            .setFreeNetUsed(1L)
            .setEnergyUsed(1L)
            .setTotalEnergyWeight(1L)
            .setTotalNetWeight(1L)
            .setNetUsed(1L)
            .setNetLimit(1L)
            .setEnergyLimit(1L)
            .build());

  }

  /**
   * constructor.
   */
  public void isExist(ByteString address1) {
    byte[] address = address1.toByteArray();
    byte[] hash0 = Sha256Sm3Hash.hash(address);
    byte[] hash1 = Sha256Sm3Hash.hash(hash0);
    byte[] checkSum = Arrays.copyOfRange(hash1, 0, 4);
    byte[] addchecksum = new byte[address.length + 4];
    System.arraycopy(address, 0, addchecksum, 0, address.length);
    System.arraycopy(checkSum, 0, addchecksum, address.length, 4);
    if (!existAddress.contains(Base58.encode(addchecksum))) {
      concurrentHashSet.add(address1);
    }
  }

  /**
   * constructor.
   */
  private static void getTxidList() {
    String line = null;
    try {
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new
          FileInputStream("demo.csv"),"utf-8"));

      while ((line = bufferedReader.readLine()) != null) {
        listForTxid.add(line.toLowerCase());

      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    List<ByteString> list = new ArrayList<>(concurrentHashSet);
    for (ByteString target : list) {
      byte[] address = target.toByteArray();
      byte[] hash0 = Sha256Sm3Hash.hash(address);
      byte[] hash1 = Sha256Sm3Hash.hash(hash0);
      byte[] checkSum = Arrays.copyOfRange(hash1, 0, 4);
      byte[] addchecksum = new byte[address.length + 4];
      System.arraycopy(address, 0, addchecksum, 0, address.length);
      System.arraycopy(checkSum, 0, addchecksum, address.length, 4);
      writeDataToCsvFile("newAddress.csv", Base58.encode(addchecksum));
    }
    Collections.sort(list1);


    int i = 1;
    /*
    afterTime = System.currentTimeMillis();
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    currentBlock = blockingStubFull1.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    afterBlockNum = currentBlock.getBlockHeader().getRawData().getNumber() + 2;
    Long blockNum = beforeBlockNum;
    Integer txsNum = 0;
    Integer topNum = 0;
    Integer totalNum = 0;
    Long energyTotal = 0L;
    String findOneTxid = "";

    NumberMessage.Builder builder = NumberMessage.newBuilder();
    while (blockNum <= afterBlockNum) {
      builder.setNum(blockNum);
      txsNum = blockingStubFull1.getBlockByNum(builder.build()).getTransactionsCount();
      totalNum = totalNum + txsNum;
      if (topNum < txsNum) {
        topNum = txsNum;
        findOneTxid = ByteArray.toHexString(Sha256Hash.hash(blockingStubFull1
            .getBlockByNum(builder.build()).getTransactionsList().get(2)
            .getRawData().toByteArray()));
        //logger.info("find one txid is " + findOneTxid);
      }

      blockNum++;
    }
    Long costTime = (afterTime - beforeTime - 31000) / 1000;
    logger.info("Duration block num is  " + (afterBlockNum - beforeBlockNum - 11));
    logger.info("Cost time are " + costTime);
    logger.info("Top block txs num is " + topNum);
    logger.info("Total transaction is " + (totalNum - 30));
    logger.info("Average Tps is " + (totalNum / costTime));

    infoById = PublicMethed.getTransactionInfoById(findOneTxid, blockingStubFull1);
    Long oneEnergyTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("EnergyTotal is " + oneEnergyTotal);
    logger.info("Average energy is " + oneEnergyTotal * (totalNum / costTime));
    */

    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}