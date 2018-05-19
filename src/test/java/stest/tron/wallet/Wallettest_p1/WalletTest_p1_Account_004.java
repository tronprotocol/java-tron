package stest.tron.wallet.Wallettest_p1;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.testng.annotations.*;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.*;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.protos.Contract;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Contract.FreezeBalanceContract;
import org.tron.protos.Contract.UnfreezeBalanceContract;
import org.tron.protos.Contract.WithdrawBalanceContract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.TransactionUtils;
import org.testng.Assert;
import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WalletTest_p1_Account_004 {

    //testng001、testng002、testng003、testng004
    private final static  String testKey001     = "8CB4480194192F30907E14B52498F594BD046E21D7C4D8FE866563A6760AC891";
    private final static  String testKey002     = "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
    private final static  String testKey003     = "6815B367FDDE637E53E9ADC8E69424E07724333C9A2B973CFA469975E20753FC";
    private final static  String testKey004     = "592BB6C9BB255409A6A43EFD18E6A74FECDDCCE93A40D96B70FBE334E6361E32";
    private final static  String no_frozen_balance_testKey = "8CB4480194192F30907E14B52498F594BD046E21D7C4D8FE866563A6760AC891";


    //testng001、testng002、testng003、testng004
    private static final byte[] BACK_ADDRESS    = Base58.decodeFromBase58Check("27YcHNYcxHGRf5aujYzWQaJSpQ4WN4fJkiU");
    private static final byte[] FROM_ADDRESS    = Base58.decodeFromBase58Check("27WvzgdLiUvNAStq2BCvA1LZisdD3fBX8jv");
    private static final byte[] TO_ADDRESS      = Base58.decodeFromBase58Check("27iDPGt91DX3ybXtExHaYvrgDt5q5d6EtFM");
    private static final byte[] NEED_CR_ADDRESS = Base58.decodeFromBase58Check("27QEkeaPHhUSQkw9XbxX3kCKg684eC2w67T");
    private static final byte[] NO_FROZEN_ADDRESS = Base58.decodeFromBase58Check("27YcHNYcxHGRf5aujYzWQaJSpQ4WN4fJkiU");

    private ManagedChannel channelFull = null;
    private ManagedChannel search_channelFull = null;
    private WalletGrpc.WalletBlockingStub blockingStubFull = null;
    private WalletGrpc.WalletBlockingStub search_blockingStubFull = null;
    //private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list").get(0);
    //private String search_fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list").get(1);
    private String fullnode = "39.105.111.178:50051";
    private String search_fullnode = "39.105.104.137:50051";

    @BeforeClass
    public void beforeClass(){
        channelFull = ManagedChannelBuilder.forTarget(fullnode)
                .usePlaintext(true)
                .build();
        blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

        search_channelFull = ManagedChannelBuilder.forTarget(search_fullnode)
                .usePlaintext(true)
                .build();
        search_blockingStubFull = WalletGrpc.newBlockingStub(search_channelFull);


    }


    @Test(enabled = true)
    public void TestFreezeBalance(){
        //冻结金额大于目前余额，冻结失败
        Assert.assertFalse(FreezeBalance(FROM_ADDRESS, 9000000000000000000L, 3L,testKey002));
        //冻结金额小于1Trx,冻结失败
        Assert.assertFalse(FreezeBalance(FROM_ADDRESS,999999L, 3L,testKey002));
        //冻结时间不为3天，冻结失败
        Assert.assertFalse(FreezeBalance(FROM_ADDRESS,1000000L,2L,testKey002));
        //如果冻结时间未到，则解锁失败
        Assert.assertFalse(UnFreezeBalance(FROM_ADDRESS, testKey002));
        try {
            Thread.sleep(16000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //冻结资产功能正常
        Assert.assertTrue(FreezeBalance(FROM_ADDRESS,1000000L, 3L, testKey002));
    }

    @Test(enabled = false)
    public void TestUnFreezeBalance(){
        //如果没有冻结资产，则解锁失败
        Assert.assertFalse(UnFreezeBalance(NO_FROZEN_ADDRESS, no_frozen_balance_testKey));
        logger.info("Test unfreezebalance");

    }

    @AfterClass
    public void shutdown() throws InterruptedException {
        if (channelFull != null) {
            channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
        if (search_channelFull != null) {
            search_channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    public Boolean FreezeBalance(byte[] Address, long freezeBalance, long freezeDuration, String priKey){
        byte[] address = Address;
        long frozen_balance = freezeBalance;
        long frozen_duration = freezeDuration;

        //String priKey = testKey002;
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ECKey ecKey= temKey;
        Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
        Long beforeBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
        Account beforeFronzen = queryAccount(ecKey, blockingStubFull);
        Long beforeFrozenBalance = 0L;
        Long beforeBandwidth     = beforeFronzen.getBandwidth();
        if(beforeFronzen.getFrozenCount()!= 0){
            beforeFrozenBalance = beforeFronzen.getFrozen(0).getFrozenBalance();
            //beforeBandwidth     = beforeFronzen.getBandwidth();
            logger.info(Long.toString(beforeFronzen.getBandwidth()));
            logger.info(Long.toString(beforeFronzen.getFrozen(0).getFrozenBalance()));
        }

        Contract.FreezeBalanceContract.Builder builder = Contract.FreezeBalanceContract.newBuilder();
        ByteString byteAddreess = ByteString.copyFrom(address);

        builder.setOwnerAddress(byteAddreess).setFrozenBalance(frozen_balance)
                .setFrozenDuration(frozen_duration);


        Contract.FreezeBalanceContract contract = builder.build();
        Transaction transaction = blockingStubFull.freezeBalance(contract);

        if (transaction == null || transaction.getRawData().getContractCount() == 0){
            return false;
        }

        transaction = TransactionUtils.setTimestamp(transaction);
        transaction = TransactionUtils.sign(transaction, ecKey);
        GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);

        if (response.getResult() == false){
            return false;
        }

        Long afterBlockNum = 0L;

        while(afterBlockNum < beforeBlockNum) {
            Block currentBlock1 = search_blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
            afterBlockNum = currentBlock1.getBlockHeader().getRawData().getNumber();
            try {
                Thread.sleep(2000);
                logger.info("wait 2 second");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Account afterFronzen = queryAccount(ecKey, search_blockingStubFull);
        Long afterFrozenBalance = afterFronzen.getFrozen(0).getFrozenBalance();
        Long afterBandwidth     = afterFronzen.getBandwidth();
        logger.info(Long.toString(afterFronzen.getBandwidth()));
        logger.info(Long.toString(afterFronzen.getFrozen(0).getFrozenBalance()));
        //logger.info(Integer.toString(search.getFrozenCount()));
        logger.info("beforefronen" + beforeFrozenBalance.toString() + "    afterfronzen" + afterFrozenBalance.toString());
        Assert.assertTrue(afterFrozenBalance - beforeFrozenBalance == freezeBalance);
        Assert.assertTrue(afterBandwidth - beforeBandwidth == freezeBalance * frozen_duration);
        return true;


    }

    public boolean UnFreezeBalance(byte[] Address, String priKey) {
        byte[] address = Address;

        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ECKey ecKey= temKey;
        Account search = queryAccount(ecKey, blockingStubFull);

        Contract.UnfreezeBalanceContract.Builder builder = Contract.UnfreezeBalanceContract
                .newBuilder();
        ByteString byteAddreess = ByteString.copyFrom(address);

        builder.setOwnerAddress(byteAddreess);

        Contract.UnfreezeBalanceContract contract = builder.build();


        Transaction transaction = blockingStubFull.unfreezeBalance(contract);

        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            return false;
        }

        transaction = TransactionUtils.setTimestamp(transaction);
        transaction = TransactionUtils.sign(transaction, ecKey);
        GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
        if (response.getResult() == false){
            return false;
        }
        else{
            return true;
        }
    }


    public Account queryAccount(ECKey ecKey,WalletGrpc.WalletBlockingStub blockingStubFull) {
        byte[] address;
        if (ecKey == null) {
            String pubKey = loadPubKey(); //04 PubKey[128]
            if (StringUtils.isEmpty(pubKey)) {
                logger.warn("Warning: QueryAccount failed, no wallet address !!");
                return null;
            }
            byte[] pubKeyAsc = pubKey.getBytes();
            byte[] pubKeyHex = Hex.decode(pubKeyAsc);
            ecKey = ECKey.fromPublicOnly(pubKeyHex);
        }
        return grpcQueryAccount(ecKey.getAddress(), blockingStubFull);
    }

    public static String loadPubKey() {
        char[] buf = new char[0x100];
        return String.valueOf(buf, 32, 130);
    }

    public byte[] getAddress(ECKey ecKey) {
        return ecKey.getAddress();
    }

    public Account grpcQueryAccount(byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
        ByteString addressBS = ByteString.copyFrom(address);
        Account request = Account.newBuilder().setAddress(addressBS).build();
        return blockingStubFull.getAccount(request);
        }

    public Block getBlock(long blockNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
        NumberMessage.Builder builder = NumberMessage.newBuilder();
        builder.setNum(blockNum);
        return blockingStubFull.getBlockByNum(builder.build());

    }
}


