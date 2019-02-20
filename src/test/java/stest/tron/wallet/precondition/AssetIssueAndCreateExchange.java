package stest.tron.wallet.precondition;


import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

import java.util.concurrent.TimeUnit;


@Slf4j
public class AssetIssueAndCreateExchange {
    //testng001、testng002、testng003、testng004
    //fromAssetIssue
    private final String testKey001 =
            "0528dc17428585fc4dece68b79fa7912270a1fe8e85f244372f59eb7e8925e04";
    //toAssetIssue
    private final String testKey002 =
            "553c7b0dee17d3f5b334925f5a90fe99fb0b93d47073d69ec33eead8459d171e";

    //Default
    //TDZdB4ogHSgU1CGrun8WXaMb2QDDkvAKQm
    private final String defaultKey =
            "549c7797b351e48ab1c6bb5857138b418012d97526fc2acba022357d49c93ac0";

    private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey001);

    private final byte[] defaultAddress = PublicMethed.getFinalAddress(defaultKey);
    ByteString assetAccountId1;
    ByteString assetAccountId2;
    Long firstTokenInitialBalance = 500000000L;
    Long secondTokenInitialBalance = 500000000L;

    private Long start;
    private Long end;

    private ManagedChannel channelFull = null;
    private WalletGrpc.WalletBlockingStub blockingStubFull = null;

    private String fullnode = "47.94.239.172:50051";
    protected String commonContractAddress1 = "TYzeSFcC391njszpDz4mGkiDmEXuXwHPo8";
    byte[] contractaddress = Wallet.decodeFromBase58Check(commonContractAddress1);
    private static ByteString assetAccountId = null;


    @BeforeSuite
    public void beforeSuite() {
        Wallet wallet = new Wallet();
        Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    }

    /**
     * constructor.
     */

    @BeforeClass(enabled = true)
    public void beforeClass() {

        channelFull = ManagedChannelBuilder.forTarget(fullnode)
                .usePlaintext(true)
                .build();
        blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    }

    @Test(enabled = true)
    public void continueRun() {
        Account fromAccount = PublicMethed.queryAccount(testKey001,blockingStubFull);
        Account toAccount   = PublicMethed.queryAccount(defaultKey,blockingStubFull);

        PublicMethed.sendcoin(defaultAddress,10000000000000L,fromAddress,testKey001,blockingStubFull);
        PublicMethed.sendcoin(defaultAddress,10000000000000L,fromAddress,testKey001,blockingStubFull);

        logger.info("sendcoin");
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);

        if(toAccount.getAssetCount()==0)
        {
            start = System.currentTimeMillis() + 2000;
            end = System.currentTimeMillis() + 1000000000;
            PublicMethed.createAssetIssue(defaultAddress, "xxd", 50000000000000L,
                    1, 1, start, end, 1, "wwwwww", "wwwwwwww", 100000L,
                    100000L, 1L, 1L, defaultKey, blockingStubFull);
            logger.info("createAssetIssue");
            PublicMethed.waitProduceNextBlock(blockingStubFull);
            PublicMethed.waitProduceNextBlock(blockingStubFull);
            PublicMethed.waitProduceNextBlock(blockingStubFull);
            assetAccountId = PublicMethed
                    .queryAccount(defaultAddress, blockingStubFull).getAssetIssuedID();
            PublicMethed.transferAsset(contractaddress,assetAccountId.toByteArray(),3000000000000L,defaultAddress,defaultKey,blockingStubFull);
            logger.info("transferAsset To ContractAddress");
        }

//        if(fromAccount.getAssetCount()==0)
//        {
//            start = System.currentTimeMillis() + 2000;
//            end = System.currentTimeMillis() + 1000000000;
//            PublicMethed.createAssetIssue(fromAddress, "xxd", 50000000000000L,
//                    1, 1, start, end, 1, "wwwwww", "wwwwwwww", 100000L,
//                    100000L, 1L, 1L, testKey001, blockingStubFull);
//            logger.info("createAssetIssue");
//        }
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);

        Account getAssetIdFromThisAccount1;
        Account getAssetIdFromThisAccount2;
        getAssetIdFromThisAccount1 = PublicMethed.queryAccount(defaultAddress,blockingStubFull);
        assetAccountId1 =getAssetIdFromThisAccount1.getAssetIssuedID();
        logger.info("assetAccountId1: "+ assetAccountId1.toStringUtf8());
        getAssetIdFromThisAccount2 = PublicMethed.queryAccount(fromAddress,blockingStubFull);
        assetAccountId2 =getAssetIdFromThisAccount2.getAssetIssuedID();

        String trx = "_";
        byte[] b = trx.getBytes();
        Assert.assertTrue(PublicMethed.exchangeCreate(assetAccountId1.toByteArray(),firstTokenInitialBalance,
                b,secondTokenInitialBalance,defaultAddress,
                defaultKey,blockingStubFull));

//        PublicMethed.exchangeCreate(assetAccountId2.toByteArray(),firstTokenInitialBalance,
//                b,secondTokenInitialBalance,fromAddress,
//                testKey001,blockingStubFull);

        logger.info("exchangeCreate");
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);
    }


    /**
     * constructor.
     */

    @AfterClass(enabled = true)
    public void shutdown() throws InterruptedException {
        if (channelFull != null) {
            channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }

    }
}

