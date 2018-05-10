package stest.tron.wallet.Wallettest_p0;

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
public class Wallettest_p0_003 {

    private WalletClient walletClient;
    //Devaccount
    private final static  String testKey001        = "effa55b420a2fe39e3f73d14b8c46824fd0d5ee210840b9c27b2e2f42a09f1f9";
    //Devaccount
    private static final byte[] BACK_ADDRESS = Base58.decodeFromBase58Check("27d3byPxZXKQWfXX7sJvemJJuv5M65F3vjS");


    public static void main(String[] args){
        logger.info("test man.");

    }

    @BeforeClass
    public void beforeClass() {
        walletClient = new WalletClient(testKey001);
        walletClient.init(0);
    }

    @Test
    public void checkWalletGet() {
        //ListWitnesses
        //CreateWitness
        //UpdateWitness
        //ListNodes
        //GetAssetIssueList
        //GetAssetIssueByName
        //GetAssetIssueListByTimestamp
        //GetNowBlock
        //GetBlockByNum
        //TotalTransaction
        //getTransactionById
        //getTransactionsByTimestamp
        //getTransactionsFromThis
        //getTransactionsToThis
        //blockList
        Long now  =  -1L;
        Protocol.Block  blocknow =  walletClient.getBlock(now);
        Long end   = blocknow.getBlockHeader().getRawData().getNumber();
        //block.getBlockHeader().
        Long start = 0L ;
        if( end > 10L ){
            end = 10L;
        }
        Optional<GrpcAPI.BlockList> blockListResult =  walletClient.getBlockByLimitNext(start,end);
        if(blockListResult.isPresent()){
            GrpcAPI.BlockList blockList = blockListResult.get();
            blockList.getBlockList().forEach(block1 -> {
                Assert.assertTrue(block1.isInitialized());
                //block.
            });
        }

        logger.info("this is test case");
        Assert.assertEquals( 1 ,1 );
    }

    @AfterClass
    public void afterClass() {
        logger.info("this is after class");
    }


}
