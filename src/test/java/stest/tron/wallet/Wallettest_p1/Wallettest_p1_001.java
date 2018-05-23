package stest.tron.wallet.Wallettest_p1;

import java.util.HashMap;
import java.util.Optional;

import org.tron.api.GrpcAPI;
//import org.tron.api.GrpcAPI.AccountList;
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

    //testng001、testng002、testng003、testng004
    private final static  String testKey001     = "8CB4480194192F30907E14B52498F594BD046E21D7C4D8FE866563A6760AC891";
    private final static  String testKey002     = "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
    private final static  String testKey003     = "6815B367FDDE637E53E9ADC8E69424E07724333C9A2B973CFA469975E20753FC";
    private final static  String testKey004     = "592BB6C9BB255409A6A43EFD18E6A74FECDDCCE93A40D96B70FBE334E6361E32";


    //testng001、testng002、testng003、testng004
    private static final byte[] BACK_ADDRESS    = Base58.decodeFromBase58Check("27YcHNYcxHGRf5aujYzWQaJSpQ4WN4fJkiU");
    private static final byte[] FROM_ADDRESS    = Base58.decodeFromBase58Check("27WvzgdLiUvNAStq2BCvA1LZisdD3fBX8jv");
    private static final byte[] TO_ADDRESS      = Base58.decodeFromBase58Check("27iDPGt91DX3ybXtExHaYvrgDt5q5d6EtFM");
    private static final byte[] NEED_CR_ADDRESS = Base58.decodeFromBase58Check("27QEkeaPHhUSQkw9XbxX3kCKg684eC2w67T");


    @BeforeClass(enabled = false)
    public void beforeClass(){
        walletClient = new WalletClient(testKey001);
        walletClient.init(0);
        walletClient.freezeBalance(1000000000,3);
    }

    @Test(enabled = false)
    public void checkvote(){
        //1 vote = 1 trx = 1_000_000 drop
        //check vote
        Optional<GrpcAPI.WitnessList> witnessResult = walletClient.listWitnesses();

        HashMap<String, String> witnesshash =  new HashMap();

        HashMap<String, Long>    beforehash =  new HashMap();


        if (witnessResult.isPresent()) {
            GrpcAPI.WitnessList WitnessList = witnessResult.get();
            WitnessList.getWitnessesList().forEach(witness -> {

                //input
                for(long i = 2000000000L;i < 10000000000L; i++)
                {
                    witnesshash.put(Base58.encode58Check(witness.getAddress().toByteArray()), Long.toString(i));
                    logger.info("vote witness :" + Base58.encode58Check(witness.getAddress().toByteArray()) + ":" + Long.toString(i));
                    //boolean ret = walletClient.voteWitness(witnesshash);
                    //Assert.assertTrue(ret);
                    //witnesshash.clear();
                    break;
                }
                //boolean ret = walletClient.voteWitness(witnesshash);
                //Assert.assertTrue(ret);
                //witnesshash.put(Base58.encode58Check(witness.getAddress().toByteArray()), "12");
                //votecount
                //beforehash.put(Base58.encode58Check(witness.getAddress().toByteArray()),witness.getVoteCount());

                //
                //logger.info(Base58.encode58Check(witness.getAddress().toByteArray()));
                //logger.info(Long.toString(witness.getVoteCount()));
            });

            boolean ret = walletClient.voteWitness(witnesshash);
            Assert.assertTrue(ret);

            //get list again
            witnessResult = walletClient.listWitnesses();

            if (witnessResult.isPresent()) {
                WitnessList = witnessResult.get();
                WitnessList.getWitnessesList().forEach(witness -> {
                    //to do :
                    //Assert.assertTrue(beforehash.get(Base58.encode58Check(witness.getAddress().toByteArray())) + 11 ==
                    //witness.getVoteCount());

                    //logger.info(Long.toString(witness.getVoteCount()));

                    Thread thread = new Thread();
                    thread.start();
                    try {
                        //thread.sleep(50000);
                    }
                    catch(Exception e){
                    }

                    //logger.info(Long.toString(witness.getVoteCount()));
                    //Assert.assertTrue(witness.getVoteCount() > 1000000);
                });
            }
        }
    }

    @AfterClass
    public void afterClass(){
    }

}
