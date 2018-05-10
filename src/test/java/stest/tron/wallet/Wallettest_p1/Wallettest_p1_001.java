package stest.tron.wallet.Wallettest_p1;


import java.util.HashMap;
import java.util.Optional;

import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountList;
import org.tron.api.GrpcAPI.NodeList;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.*;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import org.testng.annotations.*;
import org.testng.Assert;

@Slf4j
public class Wallettest_p1_001 {

    private WalletClient walletClient;

    //Devaccount
    private final static  String testKey001        = "8CB4480194192F30907E14B52498F594BD046E21D7C4D8FE866563A6760AC891";
    //Zion
    private final static  String testKey002        = "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
    //Sun
    private final static  String testKey003        = "6815B367FDDE637E53E9ADC8E69424E07724333C9A2B973CFA469975E20753FC";

    //Devaccount
    private static final byte[] BACK_ADDRESS = Base58.decodeFromBase58Check("27YcHNYcxHGRf5aujYzWQaJSpQ4WN4fJkiU");
    //Zion
    private static final byte[] FROM_ADDRESS = Base58.decodeFromBase58Check("27WvzgdLiUvNAStq2BCvA1LZisdD3fBX8jv");
    //Sun
    private static final byte[] TO_ADDRESS   = Base58.decodeFromBase58Check("27iDPGt91DX3ybXtExHaYvrgDt5q5d6EtFM");


    @BeforeClass
    public void beforeClass(){
        walletClient = new WalletClient(testKey001);
        walletClient.init(0);
    }

    @Test
    public void checkvote(){
    }

    @AfterClass
    public void afterClass(){
    }

}
