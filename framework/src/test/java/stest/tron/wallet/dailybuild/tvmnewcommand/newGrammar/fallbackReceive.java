package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class fallbackReceive {
    private final String testNetAccountKey = Configuration.getByPath("testng.conf")
            .getString("foundationAccount.key2");
    private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
    byte[] contractAddressCaller = null;
    byte[] contractAddressTest0 = null;
    byte[] contractAddressTest1 = null;
    byte[] contractAddressTest2 = null;
    byte[] contractAddressTestPayable = null;
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] contractExcAddress = ecKey1.getAddress();
    String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    private Long maxFeeLimit = Configuration.getByPath("testng.conf")
            .getLong("defaultParameter.maxFeeLimit");
    private ManagedChannel channelSolidity = null;
    private ManagedChannel channelFull = null;
    private WalletGrpc.WalletBlockingStub blockingStubFull = null;
    private ManagedChannel channelFull1 = null;
    private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
    private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
    private String fullnode = Configuration.getByPath("testng.conf")
            .getStringList("fullnode.ip.list").get(0);
    private String fullnode1 = Configuration.getByPath("testng.conf")
            .getStringList("fullnode.ip.list").get(1);
    private String soliditynode = Configuration.getByPath("testng.conf")
            .getStringList("solidityNode.ip.list").get(0);

    @BeforeSuite
    public void beforeSuite() {
        Wallet wallet = new Wallet();
        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    }

    /**
     * constructor.
     */

    @BeforeClass(enabled = true)
    public void beforeClass() {
        PublicMethed.printAddress(contractExcKey);
        channelFull = ManagedChannelBuilder.forTarget(fullnode)
                .usePlaintext(true)
                .build();
        blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
        channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
                .usePlaintext(true)
                .build();
        blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

        channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
                .usePlaintext(true)
                .build();
        blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
        PublicMethed
                .sendcoin(contractExcAddress, 1000_000_000_000L, testNetAccountAddress, testNetAccountKey,
                        blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        String filePath = "src/test/resources/soliditycode/fallbackUpgrade.sol";
        String contractName = "Caller";
        HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
        String code = retMap.get("byteCode").toString();
        String abi = retMap.get("abI").toString();
        contractAddressCaller = PublicMethed
                .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100,
                        null, contractExcKey,
                        contractExcAddress, blockingStubFull);
        //PublicMethed.waitProduceNextBlock(blockingStubFull);

        contractName = "Test0";
        retMap = PublicMethed.getBycodeAbi(filePath, contractName);
        code = retMap.get("byteCode").toString();
        abi = retMap.get("abI").toString();
        contractAddressTest0 = PublicMethed
                .deployContract(contractName, abi, code, "", maxFeeLimit, 0L,
                        100,null, contractExcKey,
                        contractExcAddress, blockingStubFull);
        //PublicMethed.waitProduceNextBlock(blockingStubFull);

        contractName = "Test1";
        retMap = PublicMethed.getBycodeAbi(filePath, contractName);
        code = retMap.get("byteCode").toString();
        abi = retMap.get("abI").toString();
        contractAddressTest1 = PublicMethed
                .deployContractFallback(contractName, abi, code, "", maxFeeLimit, 0L,
                        100,null, contractExcKey,
                        contractExcAddress, blockingStubFull);

        contractName = "Test2";
        retMap = PublicMethed.getBycodeAbi(filePath, contractName);
        code = retMap.get("byteCode").toString();
        abi = retMap.get("abI").toString();
        contractAddressTest2 = PublicMethed
                .deployContractFallback(contractName, abi, code, "", maxFeeLimit, 0L,
                        100,null, contractExcKey,
                        contractExcAddress, blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);

        contractName = "TestPayable";
        retMap = PublicMethed.getBycodeAbi(filePath, contractName);
        code = retMap.get("byteCode").toString();
        abi = retMap.get("abI").toString();
        contractAddressTestPayable = PublicMethed
                .deployContractFallback(contractName, abi, code, "", maxFeeLimit, 0L,
                        100,null, contractExcKey,
                        contractExcAddress, blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);
    }

    @Test(enabled = true, description = "contract has no fallback method")
    public void test01NoFallback() {
        String txid = "";
        String method = "callTest0(address)";
        String para = "\"" + Base58.encode58Check(contractAddressTest0) + "\"";
        txid = PublicMethed.triggerContract(contractAddressCaller,
                method, para, false,
                0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
        Optional<Protocol.TransactionInfo> infoById = null;
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
        logger.info("getResult: " + infoById.get().getResultValue());
        Assert.assertEquals("FAILED",infoById.get().getResult().toString());
    }

    @Test(enabled = true,description = "contract has fallback method")
    public void test02Fallback(){
        String txid = "";
        String method = "callTest1(address)";
        String para = "\"" + Base58.encode58Check(contractAddressTest1) + "\"";
        txid = PublicMethed.triggerContract(contractAddressCaller,
                method, para, false,
                0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
        Optional<Protocol.TransactionInfo> infoById = null;
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
        logger.info("getResult: " + infoById.get().getResultValue());
        Assert.assertEquals(2,infoById.get().getInternalTransactionsCount());
        Assert.assertEquals("SUCESS",infoById.get().getResult().toString());
    }

    @Test(enabled = true,description = "contract has fallback payable method")
    public void test03FallbackPayable(){
        Protocol.Account info;
        GrpcAPI.AccountResourceMessage resourceInfo = PublicMethed
                .getAccountResource(contractExcAddress, blockingStubFull);
        info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
        Long beforeBalance = info.getBalance();
        logger.info("beforeBalance:" + beforeBalance);
        String txid = "";
        String method = "callTest2(address)";
        String para = "\"" + Base58.encode58Check(contractAddressTest2) + "\"";
        long value = 10000;
        txid = PublicMethed.triggerContract(contractAddressCaller,method, para, false, value,
                maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
        Optional<Protocol.TransactionInfo> infoById = null;
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
        logger.info("getResult: " + infoById.get().getResultValue());
        logger.info("getresult:"+infoById.get().getResult().toString());
        Assert.assertEquals(2,infoById.get().getInternalTransactionsCount());
        Assert.assertEquals("SUCESS",infoById.get().getResult().toString());

        Long fee = infoById.get().getFee();
        logger.info("fee:" + fee);
        Protocol.Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull1);
        GrpcAPI.AccountResourceMessage resourceInfoafter = PublicMethed
                .getAccountResource(contractExcAddress, blockingStubFull1);
        Long afterBalance = infoafter.getBalance();
        logger.info("afterBalance:" + afterBalance);
        Assert.assertTrue(afterBalance + fee +value == beforeBalance);
    }

    @Test(enabled = true,description = "contract has fallback and receive")
    public void test04FallbackReceive(){
        String txid = "";
        String method = "callTestPayable1(address)";
        String para = "\"" + Base58.encode58Check(contractAddressTestPayable) + "\"";
        txid = PublicMethed.triggerContract(contractAddressCaller,
                method, para, false,
                0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
        Optional<Protocol.TransactionInfo> infoById = null;
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
        logger.info("getResult: " + infoById.get().getResultValue());
        Assert.assertEquals(2,infoById.get().getInternalTransactionsCount());
        Assert.assertEquals("SUCESS",infoById.get().getResult().toString());

        Protocol.Account info;
        GrpcAPI.AccountResourceMessage resourceInfo = PublicMethed
                .getAccountResource(contractExcAddress, blockingStubFull);
        info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
        Long beforeBalance = info.getBalance();
        logger.info("beforeBalance:" + beforeBalance);
        long value = 10000;
        txid = PublicMethed.triggerContract(contractAddressCaller,method, para, false, value,
                maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
        logger.info("getResultValue: " + infoById.get().getResultValue());
        logger.info("getresult: "+infoById.get().getResult().toString());
        Assert.assertEquals(2,infoById.get().getInternalTransactionsCount());
        Assert.assertEquals("SUCESS",infoById.get().getResult().toString());

        Long fee = infoById.get().getFee();
        logger.info("fee:" + fee);
        Protocol.Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull1);
        GrpcAPI.AccountResourceMessage resourceInfoafter = PublicMethed
                .getAccountResource(contractExcAddress, blockingStubFull1);
        Long afterBalance = infoafter.getBalance();
        logger.info("afterBalance:" + afterBalance);
        Assert.assertTrue(afterBalance + fee +value == beforeBalance);
    }

    //@AfterClass
    public void shutdown() throws InterruptedException {
        PublicMethed
                .freedResource(contractAddressTest0, contractExcKey, testNetAccountAddress, blockingStubFull);
        PublicMethed
                .freedResource(contractAddressTest1, contractExcKey, testNetAccountAddress, blockingStubFull);
        PublicMethed
                .freedResource(contractAddressTest2, contractExcKey, testNetAccountAddress, blockingStubFull);
        PublicMethed
                .freedResource(contractAddressTestPayable, contractExcKey, testNetAccountAddress, blockingStubFull);
        PublicMethed
                .freedResource(contractAddressCaller, contractExcKey, testNetAccountAddress, blockingStubFull);
        if (channelFull != null) {
            channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
        if (channelFull1 != null) {
            channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
        if (channelSolidity != null) {
            channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}