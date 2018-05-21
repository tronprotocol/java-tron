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
import org.tron.api.GrpcAPI.Return;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract;
import org.tron.protos.Contract.FreezeBalanceContract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.TransactionUtils;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WalletTest_p1_Transfer_001 {

    //testng001、testng002、testng003、testng004
    private final static  String testKey001     = "8CB4480194192F30907E14B52498F594BD046E21D7C4D8FE866563A6760AC891";
    private final static  String testKey002     = "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
    private final static  String testKey003     = "6815B367FDDE637E53E9ADC8E69424E07724333C9A2B973CFA469975E20753FC";
    private final static  String testKey004     = "592BB6C9BB255409A6A43EFD18E6A74FECDDCCE93A40D96B70FBE334E6361E32";
    private final static  String notexist01     = "DCB620820121A866E4E25905DC37F5025BFA5420B781C69E1BC6E1D83038C88A";
    private final static  String no_bandwitch   = "8CB4480194192F30907E14B52498F594BD046E21D7C4D8FE866563A6760AC891";

    //testng001、testng002、testng003、testng004
    private static final byte[] BACK_ADDRESS    = Base58.decodeFromBase58Check("27YcHNYcxHGRf5aujYzWQaJSpQ4WN4fJkiU");
    private static final byte[] FROM_ADDRESS    = Base58.decodeFromBase58Check("27WvzgdLiUvNAStq2BCvA1LZisdD3fBX8jv");
    private static final byte[] TO_ADDRESS      = Base58.decodeFromBase58Check("27iDPGt91DX3ybXtExHaYvrgDt5q5d6EtFM");
    private static final byte[] NEED_CR_ADDRESS = Base58.decodeFromBase58Check("27QEkeaPHhUSQkw9XbxX3kCKg684eC2w67T");
    private static final byte[] INVAILD_ADDRESS = Base58.decodeFromBase58Check("27cu1ozb4mX3m2afY68FSAqn3HmMp815d48");
    private static final byte[] NO_BANDWITCH_ADDRESS = Base58.decodeFromBase58Check("27YcHNYcxHGRf5aujYzWQaJSpQ4WN4fJkiU");

    private ManagedChannel channelFull = null;
    private WalletGrpc.WalletBlockingStub blockingStubFull = null;
    //private String fullnode = "39.105.111.178:50051";
    private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list").get(0);
    //private String search_fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list").get(1);


    @BeforeClass
    public void beforeClass(){
        channelFull = ManagedChannelBuilder.forTarget(fullnode)
                .usePlaintext(true)
                .build();
        blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    }

/*    //该用例测试如果向一个不存在的地址转账，且转账金额小于1TRX，则转账失败。但是测试账户不存在会pass,第一遍之后账户存在，转账成功。所以暂时自动化结果不正确。
    @Test
    public void TestSendCoin(){
        Assert.assertFalse(Sendcoin(INVAILD_ADDRESS,999999L,NO_BANDWITCH_ADDRESS,no_bandwitch));
        Assert.assertTrue(Sendcoin(INVAILD_ADDRESS, 1000000L, NO_BANDWITCH_ADDRESS,no_bandwitch));
    }*/

    @Test
    public void TestExceptTransferAssetBandwitchDecreaseWithin10Second(){
        try {
            Thread.sleep(16000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("First time");
        Assert.assertTrue(Sendcoin(TO_ADDRESS, 10L, NO_BANDWITCH_ADDRESS,no_bandwitch));
        logger.info("Within 10 seconds");
        //Assert.assertFalse(Sendcoin(TO_ADDRESS, 1000000L, NO_BANDWITCH_ADDRESS,no_bandwitch));
        Assert.assertFalse(Sendcoin(TO_ADDRESS, 10L, NO_BANDWITCH_ADDRESS,no_bandwitch));
        //Assert.assertFalse(Sendcoin(TO_ADDRESS, 1000000L, NO_BANDWITCH_ADDRESS,no_bandwitch));
        try {
            Thread.sleep(16000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(Sendcoin(TO_ADDRESS, 10L, NO_BANDWITCH_ADDRESS,no_bandwitch));
        logger.info("Out 10 seconds");
    }

    @AfterClass
    public void shutdown() throws InterruptedException {
        if (channelFull != null) {
            channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
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
        Account search = queryAccount(ecKey, blockingStubFull);

        Contract.TransferContract.Builder builder = Contract.TransferContract.newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsOwner = ByteString.copyFrom(owner);
        builder.setToAddress(bsTo);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);

        Contract.TransferContract contract =  builder.build();
        Transaction transaction = blockingStubFull.createTransaction(contract);
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

    public Account queryAccount(ECKey ecKey,WalletGrpc.WalletBlockingStub blockingStubFull) {
        byte[] address;
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

    private Transaction signTransaction(ECKey ecKey, Transaction transaction) {
        if (ecKey == null || ecKey.getPrivKey() == null) {
            logger.warn("Warning: Can't sign,there is no private key !!");
            return null;
        }
        transaction = TransactionUtils.setTimestamp(transaction);
        return TransactionUtils.sign(transaction, ecKey);
    }
}


