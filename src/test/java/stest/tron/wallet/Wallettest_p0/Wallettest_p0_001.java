package stest.tron.wallet.Wallettest_p0;


import java.util.HashMap;
import java.util.Optional;
import stest.tron.wallet.common.client.Configuration;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.tron.api.GrpcAPI;
//import org.tron.api.GrpcAPI.AccountList;
import org.tron.api.GrpcAPI.NodeList;
import org.tron.api.WalletGrpc;
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
public class Wallettest_p0_001 {

    private Base58       base58;
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

    private static final Long AMOUNT         = 1000000L;
    private static final Long F_DURATION     = 3L;
    private static final Long ZUIDIXIAOFEI   = 100000L;

    private ManagedChannel channelFull = null;
    private WalletGrpc.WalletBlockingStub blockingStubFull = null;
    private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list").get(0);

    public static void main(String[] args){
        logger.info("test man.");
    }

    @BeforeClass
    public void beforeClass() {
        walletClient = new WalletClient(testKey002);
        walletClient.init(0);

        channelFull = ManagedChannelBuilder.forTarget(fullnode)
                .usePlaintext(true)
                .build();
        blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

        //check config-beta env
        //Assert.assertTrue(checkENV());

        boolean ret = walletClient.freezeBalance(10000000000L,F_DURATION);
        Assert.assertTrue(ret);

        //logger.info("freeze amount:");
        //logger.info(Integer.toString(walletClient.queryAccount(FROM_ADDRESS).getFrozenCount()));
        //logger.info(Long.toString(walletClient.queryAccount(FROM_ADDRESS).getBandwidth()));
        //logger.info("this is before class");

    }


    @Test(enabled = true)
    public void checkTrxCoinTrade() {

        //init check node client
        WalletClient checkclient = new WalletClient(testKey001);
        checkclient.init(1);


        //check freezeBalance
        //walletClient.freezeBalance(AMOUNT, F_DURATION);
        long frozenbefore = walletClient.queryAccount(FROM_ADDRESS).getBandwidth();
        boolean ret = walletClient.freezeBalance(AMOUNT, F_DURATION);
        long frozenafter = walletClient.queryAccount(FROM_ADDRESS).getBandwidth();
        Assert.assertTrue(ret);
        logger.info(Long.toString(frozenbefore));
        logger.info(Long.toString(frozenafter));


        //fraeeze trans
        boolean test = ((frozenafter - frozenbefore) == AMOUNT * F_DURATION ) || ((frozenafter - frozenbefore) == AMOUNT * F_DURATION -ZUIDIXIAOFEI );

        //Assert.assertTrue(test); ;
        Assert.assertEquals(checkclient.queryAccount(FROM_ADDRESS).getBandwidth(), walletClient.queryAccount(FROM_ADDRESS).getBandwidth());

        //check sendcoin
        long balancebefore = walletClient.queryAccount(FROM_ADDRESS).getBalance();
        ret = walletClient.sendCoin(TO_ADDRESS, AMOUNT);
        Assert.assertEquals(walletClient.queryAccount(FROM_ADDRESS).getBalance(), balancebefore - AMOUNT);
        Assert.assertEquals(walletClient.queryAccount(FROM_ADDRESS).getBalance(), checkclient.queryAccount(FROM_ADDRESS).getBalance());
        Assert.assertTrue(ret);

        //check transaction count
        //GrpcAPI.NumberMessage GetTotalTransaction = blockingStubFull.totalTransaction(GrpcAPI.EmptyMessage.newBuilder().build());
        //logger.info(Long.toString(GetTotalTransaction.getNum()));

        //Long transactionCnt =  GetTotalTransaction.getNum();
        //Assert.assertTrue(transactionCnt > 0 );

        /*

        //getTransactionsFromThis
        Optional<GrpcAPI.TransactionList> transactionResult = walletClient.getTransactionsFromThis(FROM_ADDRESS);

        if(transactionResult.isPresent()){
            GrpcAPI.TransactionList transactionList = transactionResult.get();
            transactionList.getTransactionList().forEach(transaction ->{
                //
                //getTransactionById
                Optional<Protocol.Transaction>  transactionget = checkclient.getTransactionById(
                        ByteArray.toStr(Hash.sha256(transaction.getRawData().toByteArray()))
                );
                if(transactionget.isPresent()){
                    Protocol.Transaction transactioncheck = transactionget.get();
                    Assert.assertTrue(transactioncheck.equals(transaction));
                }
                //transaction.getRawData();
            });
        }

        //getTransactionsToThis
        Optional<GrpcAPI.TransactionList> transactionResult1 = walletClient.getTransactionsToThis(TO_ADDRESS);
        if(transactionResult1.isPresent()){
            GrpcAPI.TransactionList transactionList = transactionResult1.get();
            transactionList.getTransactionList().forEach(transaction ->{
                //
                //getTransactionById
                Optional<Protocol.Transaction>  transactionget = checkclient.getTransactionById(
                        ByteArray.toStr(Hash.sha256(transaction.getRawData().toByteArray()))
                );
                if(transactionget.isPresent()){
                    Protocol.Transaction transactioncheck = transactionget.get();
                    Assert.assertTrue(transactioncheck.equals(transaction));
                }
            });
        }

        */
        //to do
        //walletClient.getTransactionsByTimestamp()

    }


    //check vote
    @Test(enabled = true)
    public void checkTrxCoinVote() {

        //1 vote = 1 trx = 1000000 drop
        //check vote
        //GrpcAPI.WitnessList witnesslist = blockingStubFull.listWitnesses(GrpcAPI.EmptyMessage.newBuilder().build());
        //Optional<GrpcAPI.WitnessList> witnessResult = Optional.ofNullable(witnesslist);
        Optional<GrpcAPI.WitnessList> witnessResult = walletClient.listWitnesses();

        HashMap<String, String> witnesshash =  new HashMap();

        HashMap<String, Long>    beforehash =  new HashMap();


        if (witnessResult.isPresent()) {
            GrpcAPI.WitnessList WitnessList = witnessResult.get();
            WitnessList.getWitnessesList().forEach(witness -> {

                //input
                witnesshash.put(Base58.encode58Check(witness.getAddress().toByteArray()), "12");
                //votecount
                beforehash.put(Base58.encode58Check(witness.getAddress().toByteArray()),witness.getVoteCount());

                //
                logger.info(Base58.encode58Check(witness.getAddress().toByteArray()));
                logger.info(Long.toString(witness.getVoteCount()));
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
                    logger.info(Long.toString(witness.getVoteCount()));
                    //Assert.assertTrue(witness.getVoteCount() > 1000000);
                });
            }
        }
    }

    //check env: nodelist;witnesslist;accountlist.
    public boolean checkENV(){
        //check account
        //Optional<AccountList> accountResult = walletClient.listAccounts();

/*        GrpcAPI.AccountList accountlist = blockingStubFull.listAccounts(GrpcAPI.EmptyMessage.newBuilder().build());
        Optional<GrpcAPI.AccountList> accountResult = Optional.ofNullable(accountlist);

        if (accountResult.isPresent()) {
            AccountList accountList = accountResult.get();
            Assert.assertTrue(accountList.getAccountsCount() >= 4);
            accountList.getAccountsList().forEach(account -> {
                logger.info(ByteArray.toStr(account.getAccountName().toByteArray()));

                if(!(account.getAccountName().isEmpty()) && ByteArray.toStr(account.getAccountName().toByteArray()).contentEquals("Zion")){
                    Assert.assertTrue( account.getBalance() > 10000000000L );
                }
                logger.info(Long.toString(account.getTypeValue()));
                Assert.assertTrue(account.getTypeValue() >= 0);
            });
        }*/

        Optional<NodeList> nodeResult = walletClient.listNodes();
        if (nodeResult.isPresent()) {
            NodeList nodeList = nodeResult.get();
            Assert.assertTrue(nodeList.getNodesCount() > 0);
            nodeList.getNodesList().forEach(node -> {
                Assert.assertTrue(node.isInitialized());
            });
        }

        Optional<GrpcAPI.WitnessList> witnessResult1 = walletClient.listWitnesses();
        if (witnessResult1.isPresent()) {
            GrpcAPI.WitnessList WitnessList = witnessResult1.get();
            Assert.assertTrue(WitnessList.getWitnessesCount() > 0 );
            WitnessList.getWitnessesList().forEach(witness -> {
                Assert.assertTrue(witness.isInitialized());
            });
        }

        return true;

    }



/*    @Test(enabled = false)
    public void checkAcc(){

        Optional<AccountList> result = walletClient.listAccounts();

        if (result.isPresent()) {
            AccountList accountList = result.get();

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
    }*/

    @Test(enabled = true)
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
