package stest.tron.wallet.Wallettest_p1;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.TransactionUtils;

import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WalletTest_p1_AssetIssue_001 {

    //testng001、testng002、testng003、testng004
    private final static  String testKey001     = "8CB4480194192F30907E14B52498F594BD046E21D7C4D8FE866563A6760AC891";
    private final static  String testKey002     = "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
    private final static  String testKey003     = "6815B367FDDE637E53E9ADC8E69424E07724333C9A2B973CFA469975E20753FC";
    private final static  String testKey004     = "592BB6C9BB255409A6A43EFD18E6A74FECDDCCE93A40D96B70FBE334E6361E32";
    private final static  String notexist01     = "DCB620820121A866E4E25905DC37F5025BFA5420B781C69E1BC6E1D83038C88A";
    private final static  String no_bandwitch   = "8CB4480194192F30907E14B52498F594BD046E21D7C4D8FE866563A6760AC891";

    //testng001、testng002、testng003、testng004
    private static final byte[] BACK_ADDRESS    = Base58.decodeFromBase58Check("27YcHNYcxHGRf5aujYzWQaJSpQ4WN4fJkiU");
    private static final byte[] FROM_ADDRESS    = Base58.decodeFromBase58Check("27WvzgdLiUvNAStq2BCvA1LZisdD3fBX8jv");
    private static final byte[] TO_ADDRESS      = Base58.decodeFromBase58Check("27iDPGt91DX3ybXtExHaYvrgDt5q5d6EtFM");
    private static final byte[] NEED_CR_ADDRESS = Base58.decodeFromBase58Check("27QEkeaPHhUSQkw9XbxX3kCKg684eC2w67T");
    private static final byte[] INVAILD_ADDRESS = Base58.decodeFromBase58Check("27cu1ozb4mX3m2afY68FSAqn3HmMp815d48");
    private static final byte[] NO_BANDWITCH_ADDRESS = Base58.decodeFromBase58Check("27YcHNYcxHGRf5aujYzWQaJSpQ4WN4fJkiU");

    private static final long now = System.currentTimeMillis();
    private static  String name = "testAssetIssue_" + Long.toString(now);
    private static final long TotalSupply = now;
    String Description = "just-test";
    String Url = "https://github.com/tronprotocol/wallet-cli/";

    private ManagedChannel channelFull = null;
    private WalletGrpc.WalletBlockingStub blockingStubFull = null;

    private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list").get(0);

    @BeforeClass(enabled = true)
    public void beforeClass(){
        channelFull = ManagedChannelBuilder.forTarget(fullnode)
                .usePlaintext(true)
                .build();
        blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

        ByteString addressBS1 = ByteString.copyFrom(NO_BANDWITCH_ADDRESS);
        Account request1 = Account.newBuilder().setAddress(addressBS1).build();
        GrpcAPI.AssetIssueList assetIssueList1 = blockingStubFull
                .getAssetIssueByAccount(request1);
        Optional<GrpcAPI.AssetIssueList> queryAssetByAccount = Optional.ofNullable(assetIssueList1);
        if (queryAssetByAccount.get().getAssetIssueCount() == 0){
            try {
                Thread.sleep(16000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            Long now = System.currentTimeMillis();
            //Create AssetIssue failed when start time is before the currently time.
            Assert.assertFalse(CreateAssetIssue(NO_BANDWITCH_ADDRESS,name,TotalSupply, 1,100,now-10000,now+10000000000L,
                    1, Description, Url, no_bandwitch));

            //Create AssetIssue failed when TotalSupply is large than currently balance
            //Assert.assertFalse(CreateAssetIssue(NO_BANDWITCH_ADDRESS,name,9000000000000000000L, 1,1,now+10000,now+10000000000L,
              //      1, Description, Url, no_bandwitch));

            Long start = System.currentTimeMillis() + 2000;
            Long end   = System.currentTimeMillis() + 1000000000;

            //Create a new AssetIssue success.
            Assert.assertTrue(CreateAssetIssue(NO_BANDWITCH_ADDRESS,name,TotalSupply, 1,100,start,end,
                    1, Description, Url, no_bandwitch));
        }
        else{
            logger.info("This account already create an assetisue");
            Optional<GrpcAPI.AssetIssueList> queryAssetByAccount1 = Optional.ofNullable(assetIssueList1);
            name = ByteArray.toStr(queryAssetByAccount1.get().getAssetIssue(0).getName().toByteArray());

        }
    }

    @Test(enabled = true)
    public void TestTransferAssetBandwitchDecreaseWithin10Second(){
/*        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("First time to transfer assetissue");
        Assert.assertTrue(TransferAsset(TO_ADDRESS, name.getBytes(), 100L, NO_BANDWITCH_ADDRESS, no_bandwitch));
        logger.info("Within 10 seconds to transfer assetissue");
        Assert.assertFalse(TransferAsset(TO_ADDRESS, name.getBytes(), 10L, NO_BANDWITCH_ADDRESS, no_bandwitch));
        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("Out 10 seconds to transfer asset");*/
        Assert.assertTrue(TransferAsset(TO_ADDRESS, name.getBytes(), 100L, NO_BANDWITCH_ADDRESS, no_bandwitch));

        //Transfer Asset failed when transfer to yourself
        Assert.assertFalse(TransferAsset(TO_ADDRESS, name.getBytes(), 100L, TO_ADDRESS, testKey003));
        //Transfer Asset failed when the transfer amount is large than the asset balance you have.
        Assert.assertFalse(TransferAsset(FROM_ADDRESS, name.getBytes(), 9100000000000000000L, TO_ADDRESS, testKey003));
        //Transfer Asset failed when the transfer amount is 0
        Assert.assertFalse(TransferAsset(FROM_ADDRESS, name.getBytes(), 0L, TO_ADDRESS, testKey003));
        //Transfer Asset failed when the transfer amount is -1
        Assert.assertFalse(TransferAsset(FROM_ADDRESS, name.getBytes(), -1L, TO_ADDRESS, testKey003));
        //Transfer failed when you want to transfer to an invalid address
        Assert.assertFalse(TransferAsset(INVAILD_ADDRESS, name.getBytes(), 1L, TO_ADDRESS, testKey003));
        //Transfer failed when the asset issue name is not correct.
        Assert.assertFalse(TransferAsset(FROM_ADDRESS, (name+"wrong").getBytes(), 1L, TO_ADDRESS, testKey003));
        //Transfer success.
        Assert.assertTrue(TransferAsset(FROM_ADDRESS, name.getBytes(), 1L, TO_ADDRESS, testKey003));

        //No freeze asset, try to unfreeze asset failed.
        Assert.assertFalse(UnFreezeAsset(NO_BANDWITCH_ADDRESS,no_bandwitch));
        logger.info("Test no asset frozen balance, try to unfreeze asset, no exception. Test OK!!!");

        //Not create asset, try to unfreeze asset failed.No exception.
        Assert.assertFalse(UnFreezeAsset(TO_ADDRESS,testKey003));
        logger.info("Test not create asset issue, try to unfreeze asset, no exception. Test OK!!!");




    }

    @AfterClass(enabled = true)
    public void shutdown() throws InterruptedException {
        if (channelFull != null) {
            channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    public Boolean CreateAssetIssue(byte[] address, String name, Long TotalSupply, Integer TrxNum, Integer IcoNum, Long StartTime, Long EndTime,
                                     Integer VoteScore, String Description, String URL, String priKey){
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ECKey ecKey= temKey;

            try {
                Contract.AssetIssueContract.Builder builder = Contract.AssetIssueContract.newBuilder();
                builder.setOwnerAddress(ByteString.copyFrom(address));
                builder.setName(ByteString.copyFrom(name.getBytes()));
                builder.setTotalSupply(TotalSupply);
                builder.setTrxNum(TrxNum);
                builder.setNum(IcoNum);
                builder.setStartTime(StartTime);
                builder.setEndTime(EndTime);
                builder.setVoteScore(VoteScore);
                builder.setDescription(ByteString.copyFrom(Description.getBytes()));
                builder.setUrl(ByteString.copyFrom(URL.getBytes()));

                Transaction transaction = blockingStubFull.createAssetIssue(builder.build());
                if (transaction == null || transaction.getRawData().getContractCount() == 0) {
                    logger.info("transaction == null");
                    return false;
                }
                transaction = signTransaction(ecKey,transaction);
                GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
                if (response.getResult() == false){
                    logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
                    return false;
                }
                else{
                    logger.info(name);
                    return true;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
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

    private Transaction signTransaction(ECKey ecKey, Transaction transaction) {
        if (ecKey == null || ecKey.getPrivKey() == null) {
            logger.warn("Warning: Can't sign,there is no private key !!");
            return null;
        }
        transaction = TransactionUtils.setTimestamp(transaction);
        return TransactionUtils.sign(transaction, ecKey);
    }

    public boolean TransferAsset(byte[] to, byte[] assertName, long amount, byte[] address, String priKey) {
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ECKey ecKey= temKey;


        Contract.TransferAssetContract.Builder builder = Contract.TransferAssetContract.newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsName = ByteString.copyFrom(assertName);
        ByteString bsOwner = ByteString.copyFrom(address);
        builder.setToAddress(bsTo);
        builder.setAssetName(bsName);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);

        Contract.TransferAssetContract contract = builder.build();
        Transaction transaction =  blockingStubFull.transferAsset(contract);
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            logger.info("transaction == null || transaction.getRawData().getContractCount() == 0");
            return false;
        }
        transaction = signTransaction(ecKey,transaction);
        GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
        if (response.getResult() == false){
            logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
            return false;
        }
        else{
            Account search = queryAccount(ecKey, blockingStubFull);
            return true;
        }

    }
    public boolean UnFreezeAsset(byte[] Address, String priKey) {
        byte[] address = Address;

        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ECKey ecKey= temKey;

        Contract.UnfreezeAssetContract.Builder builder = Contract.UnfreezeAssetContract
                .newBuilder();
        ByteString byteAddreess = ByteString.copyFrom(address);

        builder.setOwnerAddress(byteAddreess);

        Contract.UnfreezeAssetContract contract = builder.build();


        Transaction transaction = blockingStubFull.unfreezeAsset(contract);

        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            return false;
        }

        transaction = TransactionUtils.setTimestamp(transaction);
        transaction = TransactionUtils.sign(transaction, ecKey);
        GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
        if (response.getResult() == false){
            logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
            return false;
        }
        else{
            return true;
        }
    }
}


