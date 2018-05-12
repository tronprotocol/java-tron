package stest.tron.wallet.Wallettest_p0;

import com.google.protobuf.ByteString;
import com.googlecode.cqengine.query.simple.In;
import javafx.scene.AmbientLight;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.*;
import org.testng.Assert;
import org.tron.api.GrpcAPI;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;

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
    private static final String name = "testAssetIssue_" + Long.toString(now);
    private static final long TotalSupply = now;

    public static void main(String[] args) {
        logger.info("test man.");

    }

    @BeforeClass
    public void beforeClass() {
        logger.info("this is before class");
        walletClient = new WalletClient(testKey002);
        walletClient.init(0);

    }

    @Test(enabled = true)
    public void TestAssetIssue() {
        //long TotalSupply = 100000000L;
        int TrxNum = 1;
        int IcoNum = 100;
        long StartTime = 1522583680000L;
        long EndTime = 1525089280000L;
        int DecayRatio = 1;
        int VoteScore = 2;
        String Description = "just-test";
        String Url = "https://github.com/tronprotocol/wallet-cli/";

        try {
            Contract.AssetIssueContract.Builder builder = Contract.AssetIssueContract.newBuilder();
            builder.setOwnerAddress(ByteString.copyFrom(walletClient.getAddress()));
            builder.setName(ByteString.copyFrom(name.getBytes()));
            builder.setTotalSupply(TotalSupply);
            builder.setTrxNum(TrxNum);
            builder.setNum(IcoNum);
            builder.setStartTime(StartTime);
            builder.setEndTime(EndTime);
            builder.setDecayRatio(DecayRatio);
            builder.setVoteScore(VoteScore);
            builder.setDescription(ByteString.copyFrom(Description.getBytes()));
            builder.setUrl(ByteString.copyFrom(Url.getBytes()));

            Boolean ret = walletClient.createAssetIssue(builder.build());
            Assert.assertTrue(ret);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        logger.info("this is TestAssetIssue test case");
    }

    @Test(enabled = true)
    public void TestGetAssetIssueByName() {
        Contract.AssetIssueContract ret = walletClient.getAssetIssueByName(name);
        Assert.assertEquals(ret.getTotalSupply(), TotalSupply);


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
        //logger.info(Long.toString(walletClient.getAssetIssueByName(name).getTotalSupply()));
        Assert.assertTrue(ret);
        logger.info("this is TestTransferAsset");


    }


    @Test(enabled = true)
    public void TestGetAssetIssueByAccount() {
        Optional<GrpcAPI.AssetIssueList> result = walletClient.getAssetIssueByAccount(FROM_ADDRESS);

        GrpcAPI.AssetIssueList assetissuelist = result.get();
        //logger.info(Integer.toString(result.get().getAssetIssue(0).getDecayRatio()));
        //logger.info(Long.toString(result.get().getAssetIssue(0).getTotalSupply()));
        //logger.info(ByteArray.toStr(result.get().getAssetIssue(result.get().getAssetIssueCount()-1).getName().toByteArray()));
        //logger.info(ByteArray.toStr(result.get().getAssetIssue(0).getName().toByteArray()));
        //logger.info(ByteArray.toStr(result.get().getAssetIssue(0).getName().toByteArray()));
        logger.info(Integer.toString(result.get().getAssetIssueCount()));

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
        logger.info("TestGetAssetIssueByAccount");

    }




    @Test(enabled = true)
    public void TestGetAssetIssueList(){
        Optional<GrpcAPI.AssetIssueList> result = walletClient.getAssetIssueList();
        GrpcAPI.AssetIssueList getAssetIssueList = result.get();
        logger.info(Integer.toString(result.get().getAssetIssueCount()));

        Boolean foundThisName = false;
        for (int j =0; j<result.get().getAssetIssueCount(); j++) {
            logger.info(ByteArray.toStr(result.get().getAssetIssue(j).getName().toByteArray()));
            if (result.get().getAssetIssue(j).getTotalSupply() == TotalSupply){
                //if (name.equals((result.get().getAssetIssue(j).getName().toString()))){
                foundThisName = true;
            }
        }
        Assert.assertTrue(foundThisName);
        logger.info("TestGetAssetIssueList");
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
        Assert.assertTrue(AMOUNT != 0);
        logger.info(Long.toString(result.getAmount()));
        //logger.info(result.getAssetName());
        logger.info("This is TestParticipateAssetIssue.");
    }

    @AfterClass
    public void afterClass() {
        logger.info("this is after class");
    }


}
