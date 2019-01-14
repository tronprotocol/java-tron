package stest.tron.wallet.onlineStress;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Random;
//import java.io.FileWriter;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
//import java.io.BufferedWriter;

import stest.tron.wallet.common.client.utils.PublicMethed;



@Slf4j
public class TestMapBigLongAndNumbers {

  //testng001、testng002、testng003、testng004
  //testng001、testng002、testng003、testng004
  private final String testKey002 =
      "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  //private final String testAddress41 = ByteArray.toHexString(fromAddress);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);


  String kittyCoreAddressAndCut = "";
  byte[] kittyCoreContractAddress = null;
  byte[] saleClockAuctionContractAddress = null;
  byte[] siringClockAuctionContractAddress = null;
  byte[] geneScienceInterfaceContractAddress = null;
  //Integer consumeUserResourcePercent = 20;
  Integer consumeUserResourcePercent = 100;
  String txid = "";
  Optional<TransactionInfo> infoById = null;


  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] triggerAddress = ecKey2.getAddress();
  String triggerKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());




  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(triggerKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);


  }

  @Test(enabled = true,threadPoolSize = 10, invocationCount = 10)
  public void deployErc721KittyCore() {

    Long maxFeeLimit = 1000000000L;

    String contractName = "MappingExample";
    String code = "608060405234801561001057600080fd5b506108e3806100206000396000f3006080604052600436106100985763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663058294c5811461009d5780630f8286c61461011d57806327e235e3146101b35780634a53ac83146101e65780637d9656881461020757806386b714e21461021f578063931fdba214610234578063babfe8e114610258578063e68c24ae146102bc575b600080fd5b3480156100a957600080fd5b506040805160206004803580820135601f810184900484028501840190955284845261010194369492936024939284019190819084018382808284375094975050509235600160a060020a031693506102e092505050565b60408051600160a060020a039092168252519081900360200190f35b34801561012957600080fd5b5061013e600160a060020a0360043516610310565b6040805160208082528351818301528351919283929083019185019080838360005b83811015610178578181015183820152602001610160565b50505050905090810190601f1680156101a55780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b3480156101bf57600080fd5b506101d4600160a060020a03600435166103aa565b60408051918252519081900360200190f35b3480156101f257600080fd5b5061013e600160a060020a03600435166103bc565b34801561021357600080fd5b506101d4600435610422565b34801561022b57600080fd5b5061013e610442565b34801561024057600080fd5b50610101600160a060020a036004351660243561049d565b34801561026457600080fd5b506040805160206004803580820135601f810184900484028501840190955284845261010194369492936024939284019190819084018382808284375094975050509235600160a060020a0316935061066892505050565b3480156102c857600080fd5b50610101600435600160a060020a0360243516610690565b600160a060020a03811660009081526002602090815260408220845161030892860190610790565b509092915050565b60016020818152600092835260409283902080548451600294821615610100026000190190911693909304601f81018390048302840183019094528383529192908301828280156103a25780601f10610377576101008083540402835291602001916103a2565b820191906000526020600020905b81548152906001019060200180831161038557829003601f168201915b505050505081565b60006020819052908152604090205481565b600260208181526000928352604092839020805484516001821615610100026000190190911693909304601f81018390048302840183019094528383529192908301828280156103a25780601f10610377576101008083540402835291602001916103a2565b600080805b8381101561043b5760019182019101610427565b5092915050565b6003805460408051602060026001851615610100026000190190941693909304601f810184900484028201840190925281815292918301828280156103a25780601f10610377576101008083540402835291602001916103a2565b6040805180820190915260018082527f6100000000000000000000000000000000000000000000000000000000000000602090920191825260009182916104e691600391610790565b50600090505b82811015610619576003805460408051602060026001851615610100026000190190941693909304601f81018490048402820184019092528181526105fc9361058e93919290918301828280156105845780601f1061055957610100808354040283529160200191610584565b820191906000526020600020905b81548152906001019060200180831161056757829003601f168201915b50505050506106af565b60038054604080516020601f600260001961010060018816150201909516949094049384018190048102820181019092528281526105f093909290918301828280156105845780601f1061055957610100808354040283529160200191610584565b9063ffffffff6106d516565b805161061091600391602090910190610790565b506001016104ec565b60036001600086600160a060020a0316600160a060020a03168152602001908152602001600020908054600181600116156101000203166002900461065f92919061080e565b50929392505050565b600160a060020a03811660009081526001602090815260408220845161030892860190610790565b600160a060020a03811660009081526020819052604090209190915590565b6106b7610883565b50604080518082019091528151815260209182019181019190915290565b606080600083600001518560000151016040519080825280601f01601f191660200182016040528015610712578160200160208202803883390190505b50915060208201905061072e818660200151876000015161074c565b845160208501518551610744928401919061074c565b509392505050565b60005b60208210610771578251845260209384019390920191601f199091019061074f565b50905182516020929092036101000a6000190180199091169116179052565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f106107d157805160ff19168380011785556107fe565b828001600101855582156107fe579182015b828111156107fe5782518255916020019190600101906107e3565b5061080a92915061089a565b5090565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061084757805485556107fe565b828001600101855582156107fe57600052602060002091601f016020900482015b828111156107fe578254825591600101919060010190610868565b604080518082019091526000808252602082015290565b6108b491905b8082111561080a57600081556001016108a0565b905600a165627a7a72305820f826fbe07a4ee048e7cd47a00e3ca43677011f846fb9e00e1f8202a84a5159540029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"amount\",\"type\":\"bytes\"},{\"name\":\"addr3\",\"type\":\"address\"}],\"name\":\"update3\",\"outputs\":[{\"name\":\"addr\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"address\"}],\"name\":\"balances1\",\"outputs\":[{\"name\":\"\",\"type\":\"string\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"address\"}],\"name\":\"balances\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"address\"}],\"name\":\"balances3\",\"outputs\":[{\"name\":\"\",\"type\":\"bytes\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"a\",\"type\":\"uint256\"}],\"name\":\"testUseCpu\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"s\",\"outputs\":[{\"name\":\"\",\"type\":\"string\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"addr2\",\"type\":\"address\"},{\"name\":\"times\",\"type\":\"uint256\"}],\"name\":\"update1\",\"outputs\":[{\"name\":\"addr\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"amount\",\"type\":\"string\"},{\"name\":\"addr3\",\"type\":\"address\"}],\"name\":\"update2\",\"outputs\":[{\"name\":\"addr\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"amount\",\"type\":\"uint256\"},{\"name\":\"addr1\",\"type\":\"address\"}],\"name\":\"update\",\"outputs\":[{\"name\":\"addr\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    kittyCoreContractAddress = PublicMethed.deployContract(contractName,abi,code,"",
            maxFeeLimit, 0L, consumeUserResourcePercent,null,testKey002,
            fromAddress,blockingStubFull);

    String data1 = "a";
    String data2 = "b";
    String data3 = "c";
    String data4 = "d";

    for (int i = 0;i < 13;i++) {
      data1 += data1;
    }

    for (int i = 0; i < 12; i++) {
      data2 += data2;
    }
    for (int i = 0; i < 11; i++) {
      data3 += data3;
    }
    for (int i = 0; i < 10; i++) {
      data4 += data4;
    }
    String data;
    data = data1 + data2 + data3 + data4;

    String data5 = "a";

    Account account = PublicMethed.queryAccountByAddress(fromAddress, blockingStubFull);
    System.out.println(Long.toString(account.getBalance()));
    long accountBalance = account.getBalance();

    Random random = new Random();
    int randNumber = random.nextInt(15) + 15;

    System.out.println("random number:" + randNumber);

    try {
      Thread.sleep(randNumber);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    for (int ii = 1; ii < 111100000; ii++) {
      ECKey ecKey1 = new ECKey(Utils.getRandom());
      byte[] userAddress = ecKey1.getAddress();
      String inputKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
      String addresstest = Base58.encode58Check(userAddress);

      String saleContractString = "\"" + data + "\"" + "," + "\""
          + Base58.encode58Check(userAddress) + "\"";


      System.out.println("long string address:" + addresstest);

      txid = PublicMethed.triggerContract(kittyCoreContractAddress,"update2(string,address)",
          saleContractString,false, 0,1000000000L,fromAddress,testKey002,blockingStubFull);
      logger.info(txid);


      String saleContractString1 = "\"" + data5 + "\"" + "," + "\""
          + Base58.encode58Check(userAddress) + "\"";


      System.out.println("short string address:" + addresstest);

      txid = PublicMethed.triggerContract(kittyCoreContractAddress,"update2(string,address)",
          saleContractString1,false, 0,1000000000L,fromAddress,testKey002,blockingStubFull);
      logger.info(txid);

      System.out.println("time out");

      txid = PublicMethed.triggerContract(kittyCoreContractAddress,"testUseCpu(uint256)",
          "1000000000",false, 0,1000000000L,fromAddress,testKey002,blockingStubFull);

      infoById = PublicMethed.getTransactionInfoById(txid,blockingStubFull);

      infoById.get().getResultValue();

      String isSuccess;

      if (infoById.get().getResultValue() == 0) {
        logger.info("success:" + " Number：" + ii);
        isSuccess = "success";
      } else {
        logger.info("failed" + " Number:" + ii);
        isSuccess = "fail";
      }
    }
  }


  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}


