package stest.tron.wallet.Wallettest_p0;

import com.google.protobuf.ByteString;
import com.googlecode.cqengine.query.simple.In;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import javafx.scene.AmbientLight;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.*;
import org.testng.Assert;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.TransactionUtils;

import java.math.BigInteger;
import java.util.Optional;


@Slf4j
public class Wallettest_p0_002 {

    private WalletClient walletClient;

    //Devaccount
    private final static String testKey001 = "effa55b420a2fe39e3f73d14b8c46824fd0d5ee210840b9c27b2e2f42a09f1f9";
    //Zion
    private final static String testKey002 = "32012d7b024b2e62e0ca145f137bcfd2468cac99a1880b275e2e499b23af265c";
    //Sun
    private final static String testKey003 = "85a449304487085205d48a402c30877e888fcb34391d65cfdc9cad420127826f";

    //Devaccount
    private static final byte[] BACK_ADDRESS = Base58.decodeFromBase58Check("27d3byPxZXKQWfXX7sJvemJJuv5M65F3vjS");
    //Zion
    private static final byte[] FROM_ADDRESS = Base58.decodeFromBase58Check("27fXgQ46DcjEsZ444tjZPKULcxiUfDrDjqj");
    //Sun
    private static final byte[] TO_ADDRESS = Base58.decodeFromBase58Check("27SWXcHuQgFf9uv49FknBBBYBaH3DUk4JPx");

    private static final Long AMOUNT = 101L;

    private static final long now = System.currentTimeMillis();
    private static  String name = "testAssetIssue_" + Long.toString(now);
    private static final long TotalSupply = now;
    String Description = "just-test";
    String Url = "https://github.com/tronprotocol/wallet-cli/";

    private ManagedChannel channelFull = null;
    private WalletGrpc.WalletBlockingStub blockingStubFull = null;
    //private String fullnode = "39.105.111.178:50051";
    private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list").get(0);
    //private String search_fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list").get(1);

    public static void main(String[] args) {
        logger.info("test man.");

    }

    @BeforeClass
    public void beforeClass() {
        logger.info("this is before class");
        walletClient = new WalletClient(testKey002);
        walletClient.init(0);

        channelFull = ManagedChannelBuilder.forTarget(fullnode)
                .usePlaintext(true)
                .build();
        blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    }

    @Test(enabled = true)
    public void TestAssetIssue() {

        ByteString addressBS1 = ByteString.copyFrom(FROM_ADDRESS);
        Protocol.Account request1 = Protocol.Account.newBuilder().setAddress(addressBS1).build();
        GrpcAPI.AssetIssueList assetIssueList1 = blockingStubFull
                .getAssetIssueByAccount(request1);
        Optional<GrpcAPI.AssetIssueList> queryAssetByAccount = Optional.ofNullable(assetIssueList1);
        if (queryAssetByAccount.get().getAssetIssueCount() == 0){
            try {
                Thread.sleep(16000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //新建一笔通证
            Assert.assertTrue(CreateAssetIssue(FROM_ADDRESS,name,TotalSupply, 1,100,now +900000,now+10000000000L,
                    1, Description, Url, testKey002));
        }
        else{
            logger.info("This account already create an assetisue");
            Optional<GrpcAPI.AssetIssueList> queryAssetByAccount1 = Optional.ofNullable(assetIssueList1);
            name = ByteArray.toStr(queryAssetByAccount1.get().getAssetIssue(0).getName().toByteArray());

        }

    }

    @Test(enabled = true)
    public void TestGetAssetIssueByName() {
        Contract.AssetIssueContract ret = walletClient.getAssetIssueByName(name);
        Assert.assertTrue(ret.getOwnerAddress() != null);
        //Assert.assertEquals(ret.getTotalSupply(), TotalSupply);


        //logger.info(ByteArray.toStr(walletClient.getAssetIssueByName(name).getName().toByteArray()));
        //logger.info(Integer.toString(walletClient.getAssetIssueByName(name).getNum()));
        //logger.info(Integer.toString(walletClient.getAssetIssueByName(name).getTrxNum()));
        //logger.info(Long.toString(walletClient.getAssetIssueByName(name).getTotalSupply()));
        //logger.info(Long.toString(walletClient.getAssetIssueByName(name).getDecayRatio()));
        //logger.info(ByteArray.toStr(walletClient.getAssetIssueByName(name).getDescription().toByteArray()));

        //logger.info("this is TestAssetIssueByname test case");
    }


    @Test(enabled = true)
    public void TestTransferAsset() {
        //byte assertName[] = name.getBytes();
        //logger.info(Long.toString(walletClient.getAssetIssueByName(name).getTotalSupply()));
        Boolean ret = walletClient.transferAsset(TO_ADDRESS, name.getBytes(), AMOUNT);
        if (ret == false){
            try {
                Thread.sleep(16000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ret = walletClient.transferAsset(TO_ADDRESS, name.getBytes(), AMOUNT);

        }

        //logger.info(Long.toString(walletClient.getAssetIssueByName(name).getTotalSupply()));
        Assert.assertTrue(ret);
        logger.info("this is TestTransferAsset");


    }


    @Test(enabled = true)
    public void TestGetAssetIssueByAccount() {
        Optional<GrpcAPI.AssetIssueList> result = walletClient.getAssetIssueByAccount(FROM_ADDRESS);
        Assert.assertTrue(result.get().getAssetIssueCount() == 1);

        //GrpcAPI.AssetIssueList assetissuelist = result.get();
        //logger.info(Integer.toString(result.get().getAssetIssue(0).getDecayRatio()));
        //logger.info(Long.toString(result.get().getAssetIssue(0).getTotalSupply()));
        //logger.info(ByteArray.toStr(result.get().getAssetIssue(result.get().getAssetIssueCount()-1).getName().toByteArray()));
        //logger.info(ByteArray.toStr(result.get().getAssetIssue(0).getName().toByteArray()));
        //logger.info(ByteArray.toStr(result.get().getAssetIssue(0).getName().toByteArray()));
        //logger.info(Integer.toString(result.get().getAssetIssueCount()));
/*
        Boolean foundThisName = false;
        for (int j = 0; j < result.get().getAssetIssueCount(); j++) {
            logger.info(result.get().getAssetIssue(j).getName().toString());
            logger.info(ByteArray.toStr(result.get().getAssetIssue(j).getName().toByteArray()));
            //Assert.assertEquals(result.get().getAssetIssue(j).getName().toString(), name);
            if (result.get().getAssetIssue(j).getTotalSupply() == TotalSupply) {
                //if ((result.get().getAssetIssue(j).getName().toString()).equals(name)){
                logger.info("Has in!!!!!!!");
                foundThisName = true;
            }
        }
        Assert.assertTrue(foundThisName);
        logger.info("TestGetAssetIssueByAccount");*/

    }




    @Test(enabled = true)
    public void TestGetAssetIssueList(){
        Optional<GrpcAPI.AssetIssueList> result = walletClient.getAssetIssueList();
        GrpcAPI.AssetIssueList getAssetIssueList = result.get();
        logger.info(Integer.toString(result.get().getAssetIssueCount()));
        Assert.assertTrue(result.get().getAssetIssueCount() > 0);

/*        Boolean foundThisName = false;
        for (int j =0; j<result.get().getAssetIssueCount(); j++) {
            logger.info(ByteArray.toStr(result.get().getAssetIssue(j).getName().toByteArray()));
            if (result.get().getAssetIssue(j).getTotalSupply() == TotalSupply){
                //if (name.equals((result.get().getAssetIssue(j).getName().toString()))){
                foundThisName = true;
            }
        }
        Assert.assertTrue(foundThisName);
        logger.info("TestGetAssetIssueList");*/
    }

    @Test(enabled = false)
    public void TestGetAssetIssueByTimestamp(){
        long now = System.currentTimeMillis();
        Optional<GrpcAPI.AssetIssueList> result = walletClient.getAssetIssueListByTimestamp(now);
        logger.info(Integer.toString(result.get().getAssetIssueCount()));

        Boolean foundThisName = false;
        for (int j =0; j<result.get().getAssetIssueCount(); j++) {
            logger.info(ByteArray.toStr(result.get().getAssetIssue(j).getName().toByteArray()));
            if (result.get().getAssetIssue(j).getTotalSupply() == TotalSupply){
                foundThisName = true;
            }
        }
        Assert.assertTrue(foundThisName);
        logger.info("This is TestGetAssetIssueByTimestamp.");

    }

    @Test(enabled = true)
    public void TestParticipateAssetIssue(){
        Contract.ParticipateAssetIssueContract result = walletClient.participateAssetIssueContract(TO_ADDRESS, name.getBytes(),FROM_ADDRESS, AMOUNT);

        Assert.assertTrue(result.getAmount() == AMOUNT);
        //Assert.assertTrue(AMOUNT != 0);
        //logger.info(Long.toString(result.getAmount()));
        //logger.info(result.getAssetName());
        //logger.info("This is TestParticipateAssetIssue.");
    }

    @AfterClass
    public void afterClass() {
        logger.info("this is after class");
    }

    public Boolean CreateAssetIssue(byte[] address, String name, Long TotalSupply, Integer TrxNum, Integer IcoNum, Long StartTime, Long EndTime,
                                    Integer VoteScore, String Description, String URL, String priKey){
        //long TotalSupply = 100000000L;
        //int TrxNum = 1;
        //int IcoNum = 100;
        //long StartTime = 1522583680000L;
        //long EndTime = 1525089280000L;
        //int DecayRatio = 1;
        //int VoteScore = 2;
        //String Description = "just-test";
        //String Url = "https://github.com/tronprotocol/wallet-cli/";
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ECKey ecKey= temKey;
        //Protocol.Account search = queryAccount(ecKey, blockingStubFull);

        try {
            Contract.AssetIssueContract.Builder builder = Contract.AssetIssueContract.newBuilder();
            builder.setOwnerAddress(ByteString.copyFrom(address));
            builder.setName(ByteString.copyFrom(name.getBytes()));
            builder.setTotalSupply(TotalSupply);
            builder.setTrxNum(TrxNum);
            builder.setNum(IcoNum);
            builder.setStartTime(StartTime);
            builder.setEndTime(EndTime);
            //builder.setDecayRatio(DecayRatio);
            builder.setVoteScore(VoteScore);
            builder.setDescription(ByteString.copyFrom(Description.getBytes()));
            builder.setUrl(ByteString.copyFrom(URL.getBytes()));

            Protocol.Transaction transaction = blockingStubFull.createAssetIssue(builder.build());
            if (transaction == null || transaction.getRawData().getContractCount() == 0) {
                logger.info("transaction == null, create assetissue failed");
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

    private Protocol.Transaction signTransaction(ECKey ecKey, Protocol.Transaction transaction) {
        if (ecKey == null || ecKey.getPrivKey() == null) {
            logger.warn("Warning: Can't sign,there is no private key !!");
            return null;
        }
        transaction = TransactionUtils.setTimestamp(transaction);
        return TransactionUtils.sign(transaction, ecKey);
    }

}
