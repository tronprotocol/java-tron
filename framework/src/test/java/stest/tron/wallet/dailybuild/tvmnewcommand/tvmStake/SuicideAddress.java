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
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

import java.util.HashMap;
import java.util.Optional;

public class SuicideAddress {

    private String testFoundationKey = Configuration.getByPath("testng.conf")
            .getString("foundationAccount.key2");
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
        PublicMethed.printAddress(testKey001);
        channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
        blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
        PublicMethed
                .sendcoin(testAddress002, 100000L, testFoundationAddress, testFoundationKey,
                        blockingStubFull);
        PublicMethed
                .sendcoin(testAddress001, 1000_000_00000L, testFoundationAddress, testFoundationKey,
                        blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        String filePath = "src/test/resources/soliditycode/testStakeSuicide.sol";
        String contractName = "testStakeSuicide";
        HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
        String code = retMap.get("byteCode").toString();
        String abi = retMap.get("abI").toString();
        contractAddress = PublicMethed
                .deployContract(contractName, abi, code, "", maxFeeLimit, 1000_000_0000L, 100, null, testKey001,
                        testAddress001, blockingStubFull);

        PublicMethed.waitProduceNextBlock(blockingStubFull);
    }

    @Test(enabled = true, description = "Vote for witness")
    void tvmStakeTest001() {
        System.out.println("dddddd "+Base58.encode58Check(testAddress002));
        System.out.println("dddddd "+Base58.encode58Check(testAddress001));
        String methodStr = "Stake(address,uint256)";
        String argsStr = "\"" + Base58.encode58Check(testWitnessAddress) + "\","  + 1000000 ;
        String txid  = PublicMethed
                .triggerContract(contractAddress, methodStr, argsStr,
                        false, 0, maxFeeLimit,
                        testAddress001, testKey001, blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        Optional<Protocol.TransactionInfo> info =  PublicMethed.getTransactionInfoById(txid,blockingStubFull);
        int contractResult = ByteArray.toInt(info.get().getContractResult(0).toByteArray());
        Assert.assertEquals(contractResult,1);

        Protocol.Account request = Protocol.Account.newBuilder().setAddress(ByteString.copyFrom(contractAddress)).build();
        byte[] voteAddress = (blockingStubFull.getAccount(request).getVotesList().get(0).getVoteAddress().toByteArray());
        Assert.assertEquals(testWitnessAddress,voteAddress);
        Assert.assertEquals(1,blockingStubFull.getAccount(request).getVotes(0).getVoteCount());

        String methodStr_Suicide = "SelfdestructTest(address)";
        String argsStr_Suicide = "\"" + Base58.encode58Check(testAddress002) + "\""  ;
        String txid_Suicide  = PublicMethed
                .triggerContract(contractAddress, methodStr_Suicide, argsStr_Suicide,
                        false, 0, maxFeeLimit,
                        testAddress001, testKey001, blockingStubFull);
        System.out.println("aaaa"+txid_Suicide);


    }
}
