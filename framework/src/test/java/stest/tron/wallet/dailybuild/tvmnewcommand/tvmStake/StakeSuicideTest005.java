package stest.tron.wallet.dailybuild.tvmnewcommand.tvmStake;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Account.Frozen;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

import java.util.HashMap;
import java.util.Optional;

public class StakeSuicideTest005 {

    private String testFoundationKey = Configuration.getByPath("testng.conf")
        .getString("foundationAccount.key1");
    private byte[] testFoundationAddress = PublicMethed.getFinalAddress(testFoundationKey);
    private String testWitnessKey = Configuration.getByPath("testng.conf")
        .getString("witness.key1");
    private String testWitnessKey2 = Configuration.getByPath("testng.conf")
        .getString("witness.key3");
    private byte[] testWitnessAddress = PublicMethed.getFinalAddress(testWitnessKey);
    private byte[] testWitnessAddress2 = PublicMethed.getFinalAddress(testWitnessKey2);



    private Long maxFeeLimit = Configuration.getByPath("testng.conf")
        .getLong("defaultParameter.maxFeeLimit");
    private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
        .get(0);
    private ManagedChannel channelFull = null;
    private WalletGrpc.WalletBlockingStub blockingStubFull = null;

    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] testAddress001 = ecKey1.getAddress();
    String testKey001 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ECKey ecKey2 = new ECKey(Utils.getRandom());
    byte[] testAddress002 = ecKey2.getAddress();
    String testKey002 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    private byte[] contractAddress;
    String filePath = "src/test/resources/soliditycode/testStakeSuicide.sol";
    String contractName = "testStakeSuicide";
    String code = "";
    String abi = "";

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
        System.out.println(testKey001);
        PublicMethed.printAddress(testKey001);
        channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
        blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

        PublicMethed
            .sendcoin(testAddress001, 1000_000_00000L, testFoundationAddress, testFoundationKey,
                blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);

        HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
        code = retMap.get("byteCode").toString();
        abi = retMap.get("abI").toString();
        contractAddress = PublicMethed
            .deployContract(contractName, abi, code, "", maxFeeLimit, 1000_000000L, 100, null, testKey001,
                testAddress001, blockingStubFull);

        PublicMethed.waitProduceNextBlock(blockingStubFull);
    }

    @Test(enabled = true, description = "targetAddress is account no TRX, and no frozen")
    void tvmStakeSuicideTest001() {
        ECKey ecKey_targetAddress = new ECKey(Utils.getRandom());
        byte[] targetAddress = ecKey_targetAddress.getAddress();
        String testKey_targetAddress = ByteArray.toHexString(ecKey_targetAddress.getPrivKeyBytes());

        contractAddress = PublicMethed
            .deployContract(contractName, abi, code, "", maxFeeLimit, 1000_000000L, 100, null, testKey001,
                testAddress001, blockingStubFull);

        Account ownerAccount = PublicMethed.queryAccount(contractAddress,blockingStubFull);
        Long ownerBalance = ownerAccount.getBalance();

        String methodStr_Suicide = "SelfdestructTest(address)";
        String argsStr_Suicide = "\"" + Base58.encode58Check(targetAddress) + "\""  ;
        String txid_Suicide  = PublicMethed
            .triggerContract(contractAddress, methodStr_Suicide, argsStr_Suicide,
                false, 0, maxFeeLimit,
                testAddress001, testKey001, blockingStubFull);

        PublicMethed.waitProduceNextBlock(blockingStubFull);
        Optional<TransactionInfo> ex = PublicMethed.getTransactionInfoById(txid_Suicide, blockingStubFull);
        ex = PublicMethed.getTransactionInfoById(txid_Suicide,blockingStubFull);
        Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);

        Account targetAccount = PublicMethed.queryAccount(targetAddress,blockingStubFull);
        Long targetBalance = targetAccount.getBalance();

        System.out.println(targetBalance);
        Assert.assertEquals(ownerBalance,targetBalance);

    }

    @Test(enabled = true, description = "targetAddress is account 1 TRX, and no frozen")
    void tvmStakeSuicideTest002() {
        ECKey ecKey_targetAddress = new ECKey(Utils.getRandom());
        byte[] targetAddress = ecKey_targetAddress.getAddress();
        String testKey_targetAddress = ByteArray.toHexString(ecKey_targetAddress.getPrivKeyBytes());

        PublicMethed
            .sendcoin(targetAddress, 1_000000L, testFoundationAddress, testFoundationKey,
                blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);

        contractAddress = PublicMethed
            .deployContract(contractName, abi, code, "", maxFeeLimit, 1000_000000L, 100, null, testKey001,
                testAddress001, blockingStubFull);

        Account ownerAccount = PublicMethed.queryAccount(contractAddress,blockingStubFull);
        Long ownerBalance = ownerAccount.getBalance();

        String methodStr_Suicide = "SelfdestructTest(address)";
        String argsStr_Suicide = "\"" + Base58.encode58Check(targetAddress) + "\""  ;
        String txid_Suicide  = PublicMethed
            .triggerContract(contractAddress, methodStr_Suicide, argsStr_Suicide,
                false, 0, maxFeeLimit,
                testAddress001, testKey001, blockingStubFull);

        PublicMethed.waitProduceNextBlock(blockingStubFull);
        Optional<TransactionInfo> ex = PublicMethed.getTransactionInfoById(txid_Suicide, blockingStubFull);
        ex = PublicMethed.getTransactionInfoById(txid_Suicide,blockingStubFull);
        Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);

        Account targetAccount = PublicMethed.queryAccount(targetAddress,blockingStubFull);
        Long targetBalance = targetAccount.getBalance() - 1_000000L;

        Assert.assertEquals(ownerBalance,targetBalance);

        Assert.assertTrue(PublicMethed
            .freezeBalance(targetAddress,1_000000L,3,testKey_targetAddress,blockingStubFull));
        PublicMethed.waitProduceNextBlock(blockingStubFull);

    }

    @Test(enabled = true, description = "targetAddress is account 1 TRX, and 1 frozen")
    void tvmStakeSuicideTest003() {
        ECKey ecKey_targetAddress = new ECKey(Utils.getRandom());
        byte[] targetAddress = ecKey_targetAddress.getAddress();
        String testKey_targetAddress = ByteArray.toHexString(ecKey_targetAddress.getPrivKeyBytes());
        Assert.assertTrue(PublicMethed
            .sendcoin(targetAddress, 10_000000L, testFoundationAddress, testFoundationKey,
                blockingStubFull));
        PublicMethed.waitProduceNextBlock(blockingStubFull);

        Assert.assertTrue(PublicMethed
            .freezeBalance(targetAddress,1_000000L,3,testKey_targetAddress,blockingStubFull));
        PublicMethed.waitProduceNextBlock(blockingStubFull);

        Account targetAccount = PublicMethed.queryAccount(targetAddress,blockingStubFull);
        Frozen targetFrozenBefore = targetAccount.getFrozen(0);
        contractAddress = PublicMethed
            .deployContract(contractName, abi, code, "", maxFeeLimit, 1000_000000L, 100, null, testKey001,
                testAddress001, blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);

        Account ownerAccount = PublicMethed.queryAccount(contractAddress,blockingStubFull);
        Long ownerBalance = ownerAccount.getBalance();
        String methodStr_Suicide = "SelfdestructTest(address)";
        String argsStr_Suicide = "\"" + Base58.encode58Check(targetAddress) + "\""  ;
        String txid_Suicide  = PublicMethed
            .triggerContract(contractAddress, methodStr_Suicide, argsStr_Suicide,
                false, 0, maxFeeLimit,
                testAddress001, testKey001, blockingStubFull);

        PublicMethed.waitProduceNextBlock(blockingStubFull);
        Optional<TransactionInfo> ex = PublicMethed.getTransactionInfoById(txid_Suicide, blockingStubFull);
        Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        Account targetAccountAfter = PublicMethed.queryAccount(targetAddress,blockingStubFull);
        Frozen targetFrozenAfter = targetAccountAfter.getFrozen(0);
        Long targetBalance = targetAccountAfter.getBalance() - 9_000000L;
        Assert.assertEquals(targetFrozenBefore,targetFrozenAfter);
        Assert.assertEquals(ownerBalance,targetBalance);

    }

}


