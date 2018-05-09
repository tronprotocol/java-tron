package stest.tron.wallet.Wallettest_p0;

import java.util.Optional;

import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountList;
import org.tron.api.GrpcAPI.NodeList;
import org.tron.common.utils.ByteArray;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.*;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import org.testng.annotations.*;
import org.testng.Assert;

@Slf4j
public class Wallettest_p0_001 {

    private WalletClient walletClient;

    //Devaccount
    private final static  String testKey001        = "effa55b420a2fe39e3f73d14b8c46824fd0d5ee210840b9c27b2e2f42a09f1f9";
    //Zion
    private final static  String testKey002        = "32012d7b024b2e62e0ca145f137bcfd2468cac99a1880b275e2e499b23af265c";
    //Sun
    private final static  String testKey003        = "85a449304487085205d48a402c30877e888fcb34391d65cfdc9cad420127826f";

    //Devaccount
    private static final byte[] BACK_ADDRESS = Base58.decodeFromBase58Check("27d3byPxZXKQWfXX7sJvemJJuv5M65F3vjS");
    //Zion
    private static final byte[] FROM_ADDRESS = Base58.decodeFromBase58Check("27fXgQ46DcjEsZ444tjZPKULcxiUfDrDjqj");
    //Sun
    private static final byte[] TO_ADDRESS   = Base58.decodeFromBase58Check("27SWXcHuQgFf9uv49FknBBBYBaH3DUk4JPx");

    private static final Long AMOUNT     = 1000000L;
    private static final Long F_DURATION = 3L;


    public static void main(String[] args){
        logger.info("test man.");
    }

    @BeforeClass
    public void beforeClass() {
        walletClient = new WalletClient(testKey002);
        walletClient.init(0);


        boolean ret = walletClient.freezeBalance(AMOUNT,F_DURATION);
        Assert.assertTrue(ret);

        logger.info("freeze amount:");
        logger.info(Integer.toString(walletClient.queryAccount(FROM_ADDRESS).getFrozenCount()));
        logger.info(Long.toString(walletClient.queryAccount(FROM_ADDRESS).getBandwidth()));
        logger.info("this is before class");

    }


    @Test(enabled = true)
    public void checkTrade() {

        logger.info(ByteArray.toStr(walletClient.queryAccount(FROM_ADDRESS).getAccountName().toByteArray()));
        logger.info(Long.toString(walletClient.queryAccount(FROM_ADDRESS).getBalance()));
        long   frozenbefore   =  walletClient.queryAccount(FROM_ADDRESS).getBandwidth();
        boolean ret           =  walletClient.freezeBalance(AMOUNT,F_DURATION);
        long   frozenafter    =  walletClient.queryAccount(FROM_ADDRESS).getBandwidth();
        Assert.assertTrue(ret);
        Assert.assertEquals( (frozenafter - frozenbefore), AMOUNT.longValue() * F_DURATION );

        boolean ret1  = walletClient.sendCoin(TO_ADDRESS,AMOUNT);

        logger.info(ByteArray.toStr(walletClient.queryAccount(FROM_ADDRESS).getAccountName().toByteArray()));
        logger.info(Long.toString(walletClient.queryAccount(FROM_ADDRESS).getBalance()));
        logger.info(ByteArray.toStr(walletClient.queryAccount(TO_ADDRESS).getAccountName().toByteArray()));
        logger.info(Long.toString(walletClient.queryAccount(TO_ADDRESS).getBalance()));
        Assert.assertTrue(ret1);

        logger.info("this is TestNG test case");
    }


    @Test(enabled = false)
    public void checkAcc() {

        Optional<AccountList> result = walletClient.listAccounts();

        if (result.isPresent()) {
            AccountList accountList = result.get();
            byte[]  fromaddr;
            byte[]  toaddr;
            accountList.getAccountsList().forEach(account -> {
                logger.info(ByteArray.toStr(walletClient.queryAccount(account.getAddress().toByteArray()).getAccountName().toByteArray()));
                logger.info(Long.toString(walletClient.queryAccount(account.getAddress().toByteArray()).getBalance()));
                logger.info(ByteArray.toHexString(account.getAddress().toByteArray()));
                //logger.info(ByteArray.toStr(account.getAccountName().toByteArray()));
                //logger.info(Long.toString(account.getAllowance()));
                //logger.info(ByteArray.toHexString(account.getCode().toByteArray()));
                //logger.info(Integer.toString(account.getAssetCount()));
                //logger.info(account.getType().name());
                //logger.info(Long.toString(account.getBalance()));

            });
        }
    }

    @Test(enabled = false)
    public void checkNode() {

        Optional<GrpcAPI.NodeList> result = walletClient.listNodes();

        if (result.isPresent()) {
            NodeList nodeList = result.get();
            nodeList.getNodesList().forEach(node -> {
                logger.info(Integer.toString(node.hashCode()));
                logger.info(Integer.toString(node.getSerializedSize()));
                logger.info(Integer.toString(node.getAddress().getSerializedSize()));
                logger.info(Integer.toString(node.getAddress().getPort()));
                logger.info(ByteArray.toStr(node.getAddress().getHost().toByteArray()));
            });
        }
    }


    @AfterClass
    public void afterClass() {
        logger.info("this is after class");
    }

}
