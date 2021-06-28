package stest.tron.wallet.dailybuild.crosschain;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.BalanceContract.CrossContract.CrossDataType;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.AbiUtil;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.CrossChainBase;
import stest.tron.wallet.common.client.utils.PublicMethed;


@Slf4j
public class CrossChainTrc20  extends CrossChainBase {


  @Test(enabled = true,description = "Create trc20 transfer for cross chain")
  public void test01CreateCrossTrc20Transfer() throws Exception {

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);

    String method = "increment(address,address,uint256)";
    String argsStr = "\"" + Base58.encode58Check(contractAddress) + "\"" + "," + "\""
        + Base58.encode58Check(crossContractAddress) + "\"" + ",\"1\"";


    final Long beforeTriggerAccountBalance = PublicMethed
        .queryAccount(trc10TokenAccountAddress, blockingStubFull).getBalance();
    final Long beforeRegisterAccountBalance = PublicMethed
        .queryAccount(registerAccountAddress, blockingStubFull).getBalance();

    final Long beforeCrossTriggerAccountBalance = PublicMethed
        .queryAccount(trc10TokenAccountAddress, crossBlockingStubFull).getBalance();
    final Long beforeCrossRegisterAccountBalance = PublicMethed
        .queryAccount(registerAccountAddress, crossBlockingStubFull).getBalance();


    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,"read()","#",
        false,0,100000000L,"0",0,
        trc10TokenAccountAddress,trc10TokenAccountKey,blockingStubFull);

    final long beforeFirstChainValue = ByteArray.toLong(transactionExtention.getConstantResult(0)
        .toByteArray());

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(crossContractAddress,"read()","#",
        false,0,100000000L,"0",0,
        trc10TokenAccountAddress,trc10TokenAccountKey,crossBlockingStubFull);

    final long beforeSecondChainValue = ByteArray
        .toLong(transactionExtention.getConstantResult(0).toByteArray());


    final long beforeToBalance = PublicMethed.queryAccount(trc10TokenAccountAddress,
        blockingStubFull).getBalance();



    //Create cross contract transaction
    String txid = createTriggerContractForCross(trc10TokenAccountAddress,registerAccountAddress,
        contractAddress, crossContractAddress, method,argsStr,chainId,crossChainId,
        trc10TokenAccountKey,blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);





    //Query first chain
    Optional<Transaction> byId = PublicMethed.getTransactionById(txid, blockingStubFull);
    Any any = byId.get().getRawData().getContract(0).getParameter();
    BalanceContract.CrossContract crossContract = any.unpack(BalanceContract.CrossContract.class);
    Assert.assertEquals(crossContract.getOwnerAddress(),
        ByteString.copyFrom(trc10TokenAccountAddress));
    Assert.assertEquals(crossContract.getToAddress(),
        ByteString.copyFrom(trc10TokenAccountAddress));
    Assert.assertEquals(crossContract.getOwnerChainId(),chainId);
    Assert.assertEquals(crossContract.getToChainId(),crossChainId);
    Assert.assertEquals(crossContract.getType(), CrossDataType.CONTRACT);

    //Query second chain
    byId = PublicMethed.getTransactionById(txid, crossBlockingStubFull);
    any = byId.get().getRawData().getContract(0).getParameter();
    crossContract = any.unpack(BalanceContract.CrossContract.class);
    Assert.assertEquals(crossContract.getOwnerAddress(),
        ByteString.copyFrom(trc10TokenAccountAddress));
    Assert.assertEquals(crossContract.getToAddress(),
        ByteString.copyFrom(trc10TokenAccountAddress));
    Assert.assertEquals(crossContract.getOwnerChainId(),chainId);
    Assert.assertEquals(crossContract.getToChainId(),crossChainId);
    Assert.assertEquals(crossContract.getType(), CrossDataType.CONTRACT);
    final long afterToBalance = PublicMethed.queryAccount(trc10TokenAccountAddress,
        blockingStubFull).getBalance();
    Optional<TransactionInfo> info = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(beforeToBalance - afterToBalance, info.get().getFee());

    final Long afterTriggerAccountBalance = PublicMethed
        .queryAccount(trc10TokenAccountAddress, blockingStubFull).getBalance();
    final Long afterCrossTriggerAccountBalance = PublicMethed
        .queryAccount(trc10TokenAccountAddress, crossBlockingStubFull).getBalance();
    Assert.assertEquals(beforeTriggerAccountBalance
        - afterTriggerAccountBalance, info.get().getFee());

    info = PublicMethed.getTransactionInfoById(txid, crossBlockingStubFull);
    Assert.assertEquals(beforeCrossTriggerAccountBalance
        - afterCrossTriggerAccountBalance, info.get().getFee());


    Assert.assertEquals(beforeTriggerAccountBalance - afterTriggerAccountBalance,
        info.get().getFee());


    transactionExtention = PublicMethed.triggerConstantContractForExtention(contractAddress,
        "read()","#",
        false,0,100000000L,"0",0,
        trc10TokenAccountAddress,trc10TokenAccountKey,blockingStubFull);

    long afterFirstChainValue = ByteArray.toLong(transactionExtention
        .getConstantResult(0).toByteArray());

    transactionExtention = PublicMethed.triggerConstantContractForExtention(crossContractAddress,
        "read()","#",
        false,0,100000000L,"0",0,
        trc10TokenAccountAddress,trc10TokenAccountKey,crossBlockingStubFull);

    long afterSecondChainValue = ByteArray
        .toLong(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(beforeFirstChainValue - afterFirstChainValue,-1);
    Assert.assertEquals(afterSecondChainValue - beforeSecondChainValue,-1);


    //Create cross contract transaction
    createTriggerContractForCross(mutisignTestAddress,registerAccountAddress,
        contractAddress, crossContractAddress, method,argsStr,chainId,crossChainId,
        mutisignTestKey,2,permissionKeyString,blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);


    transactionExtention = PublicMethed.triggerConstantContractForExtention(contractAddress,
        "read()","#",
        false,0,100000000L,"0",0,
        trc10TokenAccountAddress,trc10TokenAccountKey,blockingStubFull);

    long afterMutisignTriggerValue = ByteArray
        .toLong(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(afterFirstChainValue - afterMutisignTriggerValue,-1);


    //register balance not change

    final Long afterRegisterAccountBalance = PublicMethed
        .queryAccount(registerAccountAddress, blockingStubFull).getBalance();


    final Long afterCrossRegisterAccountBalance = PublicMethed
        .queryAccount(registerAccountAddress, crossBlockingStubFull).getBalance();


    Assert.assertEquals(beforeCrossRegisterAccountBalance, afterCrossRegisterAccountBalance);
    Assert.assertEquals(beforeRegisterAccountBalance, afterRegisterAccountBalance);

  }

}


