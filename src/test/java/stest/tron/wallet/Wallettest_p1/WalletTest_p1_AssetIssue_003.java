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
import org.tron.common.utils.ByteArray;
import org.tron.core.db.api.pojo.AssetIssue;
import org.tron.protos.Contract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.TransactionUtils;

import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WalletTest_p1_AssetIssue_003 {

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

    private static final long now = System.currentTimeMillis();
    private static final String name = "testAssetIssue_" + Long.toString(now);
    private static final String tooLongName = "qazxswedcvfrtgbnhyujmkiolpoiuytre";
    private static final String chineseAssetIssuename = "中文都名字";
    private static final String tooLongDescription = "1qazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcv";
    private static final String tooLongUrl="qaswqaswqaswqaswqaswqaswqaswqaswqaswqaswqaswqaswqaswqasw1qazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcv";
    private static final long TotalSupply = now;
    String Description = "just-test";
    String Url = "https://github.com/tronprotocol/wallet-cli/";

    private ManagedChannel channelFull = null;
    private WalletGrpc.WalletBlockingStub blockingStubFull = null;
    private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list").get(0);

    @BeforeClass(enabled = true)
    public void beforeClass(){
        channelFull = ManagedChannelBuilder.forTarget(fullnode)
                .usePlaintext(true)
                .build();
        blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
        ByteString addressBS1 = ByteString.copyFrom(FROM_ADDRESS);
        Account request1 = Account.newBuilder().setAddress(addressBS1).build();
        GrpcAPI.AssetIssueList assetIssueList1 = blockingStubFull
                .getAssetIssueByAccount(request1);
        Optional<GrpcAPI.AssetIssueList> queryAssetByAccount = Optional.ofNullable(assetIssueList1);
        if (queryAssetByAccount.get().getAssetIssueCount() == 0){
            try {
                Thread.sleep(16000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Long start = System.currentTimeMillis() + 100000;
            Long end   = System.currentTimeMillis() + 1000000000;
            //Freeze amount is large than total supply, create asset issue failed.
            Assert.assertFalse(CreateAssetIssue(TO_ADDRESS,name,TotalSupply,1,10,start,end,
                    2,Description,Url,9000000000000000000L,1L,testKey003));
            //Freeze day is 0, create failed
            Assert.assertFalse(CreateAssetIssue(TO_ADDRESS,name,TotalSupply,1,10,start,end,
                    2,Description,Url,100L,0L,testKey003));
            //Freeze amount is 0, create failed
            Assert.assertFalse(CreateAssetIssue(TO_ADDRESS,name,TotalSupply,1,10,start,end,
                    2,Description,Url,0L,1L,testKey003));
            //Freeze day is -1, create failed
            Assert.assertFalse(CreateAssetIssue(TO_ADDRESS,name,TotalSupply,1,10,start,end,
                    2,Description,Url,100L,-1L,testKey003));
            //Freeze amount is -1, create failed
            Assert.assertFalse(CreateAssetIssue(TO_ADDRESS,name,TotalSupply,1,10,start,end,
                    2,Description,Url,-1L,1L,testKey003));
            //Freeze day is 3653(10 years + 1 day), create failed
            Assert.assertFalse(CreateAssetIssue(FROM_ADDRESS,name,TotalSupply, 1,10,start,end,
                    2, Description, Url, 1L,3653L,testKey002));
            //Start time is late than end time.
            Assert.assertFalse(CreateAssetIssue(FROM_ADDRESS,name,TotalSupply, 1,10,end,start,
                    2, Description, Url, 1L,2L,testKey002));
            //Start time is early than currently time.
            Assert.assertFalse(CreateAssetIssue(FROM_ADDRESS,name,TotalSupply, 1,10,start-1000000L,end,
                    2, Description, Url, 1L,2L,testKey002));
            //Totalsupply is zero.
            Assert.assertFalse(CreateAssetIssue(FROM_ADDRESS,name,0L, 1,10,start,end,
                    2, Description, Url, 1L,3652L,testKey002));
            //Total supply is -1.
            Assert.assertFalse(CreateAssetIssue(FROM_ADDRESS,name,-1L, 1,10,start,end,
                    2, Description, Url, 1L,3652L,testKey002));
            //TrxNum is zero.
            Assert.assertFalse(CreateAssetIssue(FROM_ADDRESS,name,TotalSupply, 0,10,start,end,
                    2, Description, Url, 1L,3652L,testKey002));
            //TrxNum is -1.
            Assert.assertFalse(CreateAssetIssue(FROM_ADDRESS,name,TotalSupply, -1,10,start,end,
                    2, Description, Url, 1L,3652L,testKey002));
            //IcoNum is 0.
            Assert.assertFalse(CreateAssetIssue(FROM_ADDRESS,name,TotalSupply, 1,0,start,end,
                    2, Description, Url, 1L,3652L,testKey002));
            //IcoNum is -1.
            Assert.assertFalse(CreateAssetIssue(FROM_ADDRESS,name,TotalSupply, 1,-1,start,end,
                    2, Description, Url, 1L,3652L,testKey002));
            //The asset issue name is null.
            Assert.assertFalse(CreateAssetIssue(FROM_ADDRESS,"",TotalSupply, 1,10,start,end,
                    2, Description, Url, 1L,3652L,testKey002));
            //The asset issue name is large than 33 char.
            Assert.assertFalse(CreateAssetIssue(FROM_ADDRESS,tooLongName,TotalSupply, 1,10,start,end,
                    2, Description, Url, 1L,3652L,testKey002));
            //The asset issue name is chinese name.
            Assert.assertFalse(CreateAssetIssue(FROM_ADDRESS,chineseAssetIssuename,TotalSupply, 1,10,start,end,
                    2, Description, Url, 1L,3652L,testKey002));
            //The URL is null.
            Assert.assertFalse(CreateAssetIssue(FROM_ADDRESS,name,TotalSupply, 1,10,start,end,
                    2, Description, "", 1L,3652L,testKey002));
            //The URL is too long.
            Assert.assertFalse(CreateAssetIssue(FROM_ADDRESS,name,TotalSupply, 1,10,start,end,
                    2, Description, tooLongUrl, 1L,3652L,testKey002));
            //The description is too long, create failed.
            Assert.assertFalse(CreateAssetIssue(FROM_ADDRESS,name,TotalSupply, 1,10,start,end,
                    2, tooLongDescription, Url, 1L,3652L,testKey002));
            //The voteScore is 0
            //Assert.assertFalse(CreateAssetIssue(FROM_ADDRESS,name,TotalSupply, 1,10,start,end,
              //      0, Description, Url, 1L,3652L,testKey002));
            //The voteScore is -1
            //Assert.assertFalse(CreateAssetIssue(FROM_ADDRESS,name,TotalSupply, 1,10,start,end,
               //     -1, Description, Url, 1L,3652L,testKey002));



            //Create success
            start = System.currentTimeMillis() + 3000;
            end   = System.currentTimeMillis() + 1000000000;
            Assert.assertTrue(CreateAssetIssue(FROM_ADDRESS,name,TotalSupply, 1,10,start,end,
                    2, Description, Url, 1L,3652L,testKey002));
            //Test not in the duration time, participate failed.
            Assert.assertFalse(participateAssetIssue(FROM_ADDRESS, name.getBytes(),1L, TO_ADDRESS, testKey003));
            //Test another address try to create the same name asset issue, create failed.
            Assert.assertFalse(CreateAssetIssue(TO_ADDRESS,name,TotalSupply, 1,10,start,end,
                    2, Description, Url, 1L,3652L,testKey003));

            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //Assert.assertFalse(participateAssetIssue(FROM_ADDRESS, name.getBytes(),1L, TO_ADDRESS, testKey003));
            GrpcAPI.AssetIssueList assetIssueList = blockingStubFull
                    .getAssetIssueList(GrpcAPI.EmptyMessage.newBuilder().build());
            logger.info(Integer.toString(assetIssueList.getAssetIssue(0).getFrozenSupplyCount()));
            Assert.assertTrue(assetIssueList.getAssetIssue(0).getFrozenSupplyCount() == 1);
            //Assert.assertTrue(assetIssueList.getAssetIssue(j).getFrozenSupplyCount() > 0);
            Assert.assertTrue(assetIssueList.getAssetIssue(0).getFrozenSupply(0).getFrozenAmount()>0);
            Assert.assertTrue(assetIssueList.getAssetIssue(0).getFrozenSupply(0).getFrozenDays() > 0);
            Assert.assertFalse(UnFreezeAsset(FROM_ADDRESS,testKey002));
            logger.info("unfreezeasset test");

            //Test one account only can create one asset issue.
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            start = System.currentTimeMillis() + 3000;
            end   = System.currentTimeMillis() + 1000000000;
            Assert.assertFalse(CreateAssetIssue(FROM_ADDRESS,name,TotalSupply, 1,10,start,end,
                    2, Description, Url, 1L,3652L,testKey002));
            logger.info("FROM ADDRESS create asset issue in this case!!!");

        }
        else{
            logger.info("This account already create an assetisue");
        }
    }

    @Test(enabled = true)
    public void TestGetAllAssetIssue(){
        GrpcAPI.AssetIssueList assetIssueList = blockingStubFull
                .getAssetIssueList(GrpcAPI.EmptyMessage.newBuilder().build());
        Assert.assertTrue(assetIssueList.getAssetIssueCount() >= 1);
        for(Integer j =0; j<assetIssueList.getAssetIssueCount(); j++){
            Assert.assertFalse(assetIssueList.getAssetIssue(j).getOwnerAddress().isEmpty());
            Assert.assertFalse(assetIssueList.getAssetIssue(j).getName().isEmpty());
            Assert.assertFalse(assetIssueList.getAssetIssue(j).getUrl().isEmpty());
            Assert.assertTrue(assetIssueList.getAssetIssue(j).getTotalSupply() > 0);
            logger.info("test get all assetissue");
        }
    }

    @AfterClass(enabled = true)
    public void shutdown() throws InterruptedException {
        if (channelFull != null) {
            channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    public Boolean CreateAssetIssue(byte[] address, String name, Long TotalSupply, Integer TrxNum, Integer IcoNum, Long StartTime, Long EndTime,
                                     Integer VoteScore, String Description, String URL, Long fronzenAmount, Long frozenDay,String priKey){
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ECKey ecKey= temKey;
        Account search = queryAccount(ecKey, blockingStubFull);

            try {
                Contract.AssetIssueContract.Builder builder = Contract.AssetIssueContract.newBuilder();
                builder.setOwnerAddress(ByteString.copyFrom(address));
                builder.setName(ByteString.copyFrom(name.getBytes()));
                builder.setTotalSupply(TotalSupply);
                builder.setTrxNum(TrxNum);
                builder.setNum(IcoNum);
                builder.setStartTime(StartTime);
                builder.setEndTime(EndTime);
                builder.setVoteScore(VoteScore);
                builder.setDescription(ByteString.copyFrom(Description.getBytes()));
                builder.setUrl(ByteString.copyFrom(URL.getBytes()));
                Contract.AssetIssueContract.FrozenSupply.Builder frozenBuilder = Contract.AssetIssueContract.FrozenSupply.newBuilder();
                frozenBuilder.setFrozenAmount(fronzenAmount);
                frozenBuilder.setFrozenDays(frozenDay);
                builder.addFrozenSupply(0,frozenBuilder);

                Transaction transaction = blockingStubFull.createAssetIssue(builder.build());
                if (transaction == null || transaction.getRawData().getContractCount() == 0) {
                    logger.info("transaction == null");
                    return false;
                }
                transaction = signTransaction(ecKey,transaction);
                Return response = blockingStubFull.broadcastTransaction(transaction);
                if (response.getResult() == false){
                    logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
                    return false;
                }
                else{
                    logger.info(name);
                    return true;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
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

    public boolean TransferAsset(byte[] to, byte[] assertName, long amount, byte[] address, String priKey) {
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ECKey ecKey= temKey;


        Contract.TransferAssetContract.Builder builder = Contract.TransferAssetContract.newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsName = ByteString.copyFrom(assertName);
        ByteString bsOwner = ByteString.copyFrom(address);
        builder.setToAddress(bsTo);
        builder.setAssetName(bsName);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);

        Contract.TransferAssetContract contract = builder.build();
        Transaction transaction =  blockingStubFull.transferAsset(contract);
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            return false;
        }
        transaction = signTransaction(ecKey,transaction);
        Return response = blockingStubFull.broadcastTransaction(transaction);
        if (response.getResult() == false){
            return false;
        }
        else{
            Account search = queryAccount(ecKey, blockingStubFull);
            return true;
        }

    }

    public boolean UnFreezeAsset(byte[] Address, String priKey) {
        byte[] address = Address;

        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ECKey ecKey= temKey;

        Contract.UnfreezeAssetContract.Builder builder = Contract.UnfreezeAssetContract
                .newBuilder();
        ByteString byteAddreess = ByteString.copyFrom(address);

        builder.setOwnerAddress(byteAddreess);

        Contract.UnfreezeAssetContract contract = builder.build();


        Transaction transaction = blockingStubFull.unfreezeAsset(contract);

        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            return false;
        }

        transaction = TransactionUtils.setTimestamp(transaction);
        transaction = TransactionUtils.sign(transaction, ecKey);
        GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
        if (response.getResult() == false){
            logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
            return false;
        }
        else{
            return true;
        }
    }


    public boolean participateAssetIssue(byte[] to, byte[] assertName, long amount,byte[] from, String priKey) {
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ECKey ecKey= temKey;

        Contract.ParticipateAssetIssueContract.Builder builder = Contract.ParticipateAssetIssueContract
                .newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsName = ByteString.copyFrom(assertName);
        ByteString bsOwner = ByteString.copyFrom(from);
        builder.setToAddress(bsTo);
        builder.setAssetName(bsName);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);
        Contract.ParticipateAssetIssueContract contract = builder.build();

        Transaction transaction = blockingStubFull.participateAssetIssue(contract);
        transaction = signTransaction(ecKey,transaction);
        Return response = blockingStubFull.broadcastTransaction(transaction);
        if (response.getResult() == false){
            logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
            return false;
        }
        else{
            logger.info(name);
            return true;
        }
    }

}


