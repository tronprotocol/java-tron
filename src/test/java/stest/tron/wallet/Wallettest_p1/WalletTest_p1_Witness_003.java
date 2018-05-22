package stest.tron.wallet.Wallettest_p1;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.TransactionUtils;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

//import stest.tron.wallet.common.client.AccountComparator;

@Slf4j
public class WalletTest_p1_Witness_003 {

    //testng001、testng002、testng003、testng004
    private final static  String testKey001     = "8CB4480194192F30907E14B52498F594BD046E21D7C4D8FE866563A6760AC891";
    private final static  String testKey002     = "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
    private final static  String testKey003     = "6815B367FDDE637E53E9ADC8E69424E07724333C9A2B973CFA469975E20753FC";
    private final static  String testKey004     = "592BB6C9BB255409A6A43EFD18E6A74FECDDCCE93A40D96B70FBE334E6361E32";
    private final static  String lowBalTest     = "86ff0c39337e9e97526c80af51f0e80411f5a1251473035f380f3671c1aa2b4b";

    //testng001、testng002、testng003、testng004
    private static final byte[] BACK_ADDRESS    = Base58.decodeFromBase58Check("27YcHNYcxHGRf5aujYzWQaJSpQ4WN4fJkiU");
    private static final byte[] FROM_ADDRESS    = Base58.decodeFromBase58Check("27WvzgdLiUvNAStq2BCvA1LZisdD3fBX8jv");
    private static final byte[] TO_ADDRESS      = Base58.decodeFromBase58Check("27iDPGt91DX3ybXtExHaYvrgDt5q5d6EtFM");
    private static final byte[] NEED_CR_ADDRESS = Base58.decodeFromBase58Check("27QEkeaPHhUSQkw9XbxX3kCKg684eC2w67T");
    private static final byte[] Low_Bal_ADDRESS = Base58.decodeFromBase58Check("27XeWZUtufGk8jdjF3m1tuPnnRqqKgzS3pT");


    private static final Long costForCreateWitness = 9999000000L;
    String  createWitnessUrl = "http://www.createwitnessurl.com";
    String  updateWitnessUrl = "http://www.updatewitnessurl.com";
    byte[]  createUrl = createWitnessUrl.getBytes();
    byte[]  updateUrl = updateWitnessUrl.getBytes();
    private ManagedChannel channelFull = null;
    private WalletGrpc.WalletBlockingStub blockingStubFull = null;
    //private String fullnode = "39.105.111.178:50051";
    private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list").get(0);

    @BeforeClass
    public void beforeClass(){
        channelFull = ManagedChannelBuilder.forTarget(fullnode)
                .usePlaintext(true)
                .build();
        blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    }


    @Test
    public void TestCreateWitness(){
        //如果已经是候选人，则申请失败（暂时先默认FROM_ADDRESS账户在服务器初始化后就是witness）
        CreateWitness(FROM_ADDRESS,createUrl, testKey002);
        Assert.assertFalse(CreateWitness(FROM_ADDRESS,createUrl, testKey002));

        //从一个账户转入足额到资产到Low_Bal_ADDRESS账户，使该账户可以申请成为witness

        Assert.assertTrue(Sendcoin(Low_Bal_ADDRESS, costForCreateWitness,FROM_ADDRESS, testKey002));
        Account search1 = queryAccount(lowBalTest, blockingStubFull);
        Long beforeCreateWitnessBalance = search1.getBalance();

        //Assert.assertTrue(CreateWitness(Low_Bal_ADDRESS,Low_Bal_ADDRESS, lowBalTest));
        if(CreateWitness(Low_Bal_ADDRESS,createUrl, lowBalTest) == false){
            logger.info("Maybe the LowBalanceAccount had become witness yet, please test this case for manual");
            Sendcoin(Low_Bal_ADDRESS, costForCreateWitness,FROM_ADDRESS, testKey002);
            Assert.assertTrue(Sendcoin(FROM_ADDRESS,costForCreateWitness,Low_Bal_ADDRESS,lowBalTest));
        }
        else{
            try {
                Thread.sleep(15000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.info("Low balance acctount Create Witness succesfully");
            Account search2 = queryAccount(lowBalTest, blockingStubFull);
            Long afterCreateWitnessBalance = search2.getBalance();
            logger.info("beforecreatewitnessbalance + " + Long.toString(beforeCreateWitnessBalance));
            logger.info("aftercreatewitnessbalance + " + Long.toString(afterCreateWitnessBalance));
            Assert.assertTrue(beforeCreateWitnessBalance - afterCreateWitnessBalance == costForCreateWitness);
        }
    }

    @Test
    public void TestUpdateWitness(){
        //更新Witness到URL
        //Assert.assertTrue(UpdatWitness(Low_Bal_ADDRESS,updateUrl,lowBalTest));
        try {
            Thread.sleep(18000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(UpdateWitness(Low_Bal_ADDRESS,updateUrl,lowBalTest));

    }

    @AfterClass
    public void shutdown() throws InterruptedException {
        if (channelFull != null) {
            channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }


    public  Boolean CreateWitness(byte[] owner, byte[] url, String priKey){
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ECKey ecKey= temKey;

        Contract.WitnessCreateContract.Builder builder = Contract.WitnessCreateContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setUrl(ByteString.copyFrom(url));
        Contract.WitnessCreateContract contract = builder.build();
        Protocol.Transaction transaction = blockingStubFull.createWitness(contract);
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            return false;
        }
        transaction = signTransaction(ecKey,transaction);
        GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
        if (response.getResult() == false){
            return false;
        }
        else{
            return true;
        }

    }

    public  Boolean UpdateWitness(byte[] owner, byte[] url, String priKey){
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ECKey ecKey= temKey;

        //Contract.WitnessCreateContract.Builder builder = Contract.WitnessCreateContract.newBuilder();
        Contract.WitnessUpdateContract.Builder builder = Contract.WitnessUpdateContract.newBuilder();
        //builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        //builder.setUrl(ByteString.copyFrom(url));
        builder.setUpdateUrl(ByteString.copyFrom(url));
        //Contract.WitnessCreateContract contract = builder.build();
        Contract.WitnessUpdateContract contract = builder.build();
        //Protocol.Transaction transaction = blockingStubFull.createWitness(contract);
        Protocol.Transaction transaction = blockingStubFull.updateWitness(contract);
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            logger.info("transaction == null");
            return false;
        }
        transaction = signTransaction(ecKey,transaction);
        GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
        if (response.getResult() == false){
            logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
            logger.info("response.getRestult() == false");
            return false;
        }
        else{
            return true;
        }

    }


    public Boolean Sendcoin(byte[] to, long amount, byte[] owner, String priKey){

        //String priKey = testKey002;
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ECKey ecKey= temKey;
        //Account search = queryAccount(ecKey, blockingStubFull);

        Contract.TransferContract.Builder builder = Contract.TransferContract.newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsOwner = ByteString.copyFrom(owner);
        builder.setToAddress(bsTo);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);

        Contract.TransferContract contract =  builder.build();
        Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            return false;
        }
        transaction = signTransaction(ecKey,transaction);
        GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
        if (response.getResult() == false){
            return false;
        }
        else{
            return true;
        }
    }


    public Account queryAccount(String priKey,WalletGrpc.WalletBlockingStub blockingStubFull) {
        byte[] address;
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ECKey ecKey= temKey;
        if (ecKey == null) {
            String pubKey = loadPubKey(); //04 PubKey[128]
            if (StringUtils.isEmpty(pubKey)) {
                logger.warn("Warning: QueryAccount failed, no wallet address !!");
                return null;
            }
            byte[] pubKeyAsc = pubKey.getBytes();
            byte[] pubKeyHex = Hex.decode(pubKeyAsc);
            ecKey = ECKey.fromPublicOnly(pubKeyHex);
        }
        return grpcQueryAccount(ecKey.getAddress(), blockingStubFull);
    }

    public static String loadPubKey() {
        char[] buf = new char[0x100];
        return String.valueOf(buf, 32, 130);
    }

    public byte[] getAddress(ECKey ecKey) {
        return ecKey.getAddress();
    }

    public Account grpcQueryAccount(byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
        ByteString addressBS = ByteString.copyFrom(address);
        Account request = Account.newBuilder().setAddress(addressBS).build();
        return blockingStubFull.getAccount(request);
        }

    public Block getBlock(long blockNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
        NumberMessage.Builder builder = NumberMessage.newBuilder();
        builder.setNum(blockNum);
        return blockingStubFull.getBlockByNum(builder.build());

    }

    private Protocol.Transaction signTransaction(ECKey ecKey, Protocol.Transaction transaction) {
        if (ecKey == null || ecKey.getPrivKey() == null) {
            logger.warn("Warning: Can't sign,there is no private key !!");
            return null;
        }
        transaction = TransactionUtils.setTimestamp(transaction);
        return TransactionUtils.sign(transaction, ecKey);
    }
}


