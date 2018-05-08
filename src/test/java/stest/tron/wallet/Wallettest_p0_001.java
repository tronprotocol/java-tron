package stest.tron.wallet;

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

    //testKey001 frozen fail
    private final static  String testKey001        = "85a449304487085205d48a402c30877e888fcb34391d65cfdc9cad420127826f";
    private final static  String testKey002        = "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
    private final static  String testKey003        = "6815B367FDDE637E53E9ADC8E69424E07724333C9A2B973CFA469975E20753FC";

    //BACK_ADDRESS  == testKey001
    private static final byte[] BACK_ADDRESS = Base58.decodeFromBase58Check("27YcHNYcxHGRf5aujYzWQaJSpQ4WN4fJkiU");
    private static final byte[] FROM_ADDRESS = Base58.decodeFromBase58Check("27WvzgdLiUvNAStq2BCvA1LZisdD3fBX8jv");
    private static final byte[] TO_ADDRESS   = Base58.decodeFromBase58Check("27iDPGt91DX3ybXtExHaYvrgDt5q5d6EtFM");

    private static final Long AMOUNT = 1000000L;

    public static void main(String[] args){
        logger.info("test man.");
    }

    @BeforeClass
    public void beforeClass() {
        walletClient = new WalletClient(testKey002);
        walletClient.init(0);
        walletClient.freezeBalance(1000000L,3);
        logger.info("freeze amount:");
        logger.info(Long.toString(walletClient.queryAccount(FROM_ADDRESS).getFrozenCount()));
        logger.info("this is before class");
    }


    @Test(enabled = true)
    public void checkTrade() {

        logger.info(ByteArray.toStr(walletClient.queryAccount(FROM_ADDRESS).getAccountName().toByteArray()));
        logger.info(Long.toString(walletClient.queryAccount(FROM_ADDRESS).getBalance()));
        logger.info(Long.toString(walletClient.queryAccount(FROM_ADDRESS).getFrozenCount()));
        logger.info(ByteArray.toStr(walletClient.queryAccount(TO_ADDRESS).getAccountName().toByteArray()));
        logger.info(Long.toString(walletClient.queryAccount(TO_ADDRESS).getBalance()));

        long   frozenbefore   =  walletClient.queryAccount(FROM_ADDRESS).getFrozenCount();
        boolean ret           =  walletClient.freezeBalance(AMOUNT,3);
        long   frozenafter    =  walletClient.queryAccount(FROM_ADDRESS).getFrozenCount();
        Assert.assertTrue(ret);
        Assert.assertEquals( AMOUNT.longValue() ,(frozenbefore - frozenafter) );
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
