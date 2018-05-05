package stest.tron.wallet;

import java.util.Optional;

import org.tron.api.GrpcAPI.AccountList;
import org.tron.common.utils.ByteArray;
import stest.tron.wallet.common.WalletClient;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.*;
import org.testng.Assert;


@Slf4j
public class Wallettest_p0_001 {

    private WalletClient walletClient;

    public static void main(String[] args){
        logger.info("test man.");

    }

    @BeforeClass
    public void beforeClass() {
        walletClient = new WalletClient();
        walletClient.init();
        logger.info("this is before class");
    }

    @Test
    public void TestNgLearn() {
        logger.info("this is TestNG test case");
        Assert.assertEquals( 1 ,1 );
    }

    @Test
    public void checkAcc() {

        Optional<AccountList> result = walletClient.listAccounts();

        if (result.isPresent()) {
            AccountList accountList = result.get();
            accountList.getAccountsList().forEach(account -> {
                logger.info(ByteArray.toHexString(account.getAddress().toByteArray()));
                logger.info(ByteArray.toStr(account.getAccountName().toByteArray()));
                logger.info(Long.toString(account.getAllowance()));
                logger.info(ByteArray.toHexString(account.getCode().toByteArray()));
                logger.info(Integer.toString(account.getAssetCount()));
                logger.info(account.getType().name());
                logger.info(Long.toString(account.getBalance()));

            });
        }
    }

    @Test
    public void checkTrade() {
        logger.info("this is TestNG test case");
    }

    @AfterClass
    public void afterClass() {
        logger.info("this is after class");
    }

}
