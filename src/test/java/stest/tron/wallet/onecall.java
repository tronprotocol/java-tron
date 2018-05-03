package stest.tron.wallet;


import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.*;
import org.testng.Assert;

import stest.tron.wallet.common.GrpcClient;

@Slf4j
public class onecall {

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