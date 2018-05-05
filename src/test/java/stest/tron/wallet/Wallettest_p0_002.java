package stest.tron.wallet;

import java.util.Optional;
import org.tron.api.GrpcAPI.AccountList;
import org.tron.common.utils.ByteArray;
import stest.tron.wallet.common.WalletClient;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.*;
import org.testng.Assert;


@Slf4j
public class Wallettest_p0_002 {


    public static void main(String[] args){
        logger.info("test man.");

    }

    @BeforeClass
    public void beforeClass() {
        logger.info("this is before class");
    }

    @Test
    public void TestNgLearn() {
        logger.info("this is TestNG test case");
        Assert.assertEquals( 1 ,1 );
    }

    @AfterClass
    public void afterClass() {
        logger.info("this is after class");
    }


}
