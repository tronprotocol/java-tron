package stest.tron.wallet.precondition;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ExchangeList;
import org.tron.api.GrpcAPI.ProposalList;
import org.tron.api.WalletGrpc;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Configuration;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.ChainParameters;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;

@Slf4j
public class StressPrecondition {

  protected String commonOwnerAddress = Configuration.getByPath("stress.conf")
      .getString("address.commonOwnerAddress");
  protected String triggerOwnerAddress = Configuration.getByPath("stress.conf")
      .getString("address.triggerOwnerAddress");
  protected String triggerOwnerKey = Configuration.getByPath("stress.conf")
      .getString("privateKey.triggerOwnerKey");
  protected String commonOwnerPrivateKey = Configuration.getByPath("stress.conf")
      .getString("privateKey.commonOwnerPrivateKey");
  protected String commonToAddress = Configuration.getByPath("stress.conf")
      .getString("address.commonToAddress");
  protected String commonToPrivateKey = Configuration.getByPath("stress.conf")
      .getString("privateKey.commonToPrivateKey");
  protected String commonWitnessAddress = Configuration.getByPath("stress.conf")
      .getString("address.commonWitnessAddress");
  protected String commonWitnessPrivateKey = Configuration.getByPath("stress.conf")
      .getString("privateKey.commonWitnessPrivateKey");

/*  protected String commonContractAddress1 = Configuration.getByPath("stress.conf")
      .getString("address.commonContractAddress1");
  protected String commonContractAddress2 = Configuration.getByPath("stress.conf")
      .getString("address.commonContractAddress2");
  protected String commonContractAddress3 = Configuration.getByPath("stress.conf")
      .getString("address.commonContractAddress3");
  protected String commontokenid = Configuration.getByPath("stress.conf")
      .getString("param.commontokenid");
  protected long commonexchangeid = Configuration.getByPath("stress.conf")
      .getLong("param.commonexchangeid");*/

  protected String delegateResourceAddress = Configuration.getByPath("stress.conf")
      .getString("address.delegateResourceAddress");
  protected String delegateResourceKey = Configuration.getByPath("stress.conf")
      .getString("privateKey.delegateResourceKey");

  protected String assetIssueOwnerAddress = Configuration.getByPath("stress.conf")
      .getString("address.assetIssueOwnerAddress");
  protected String assetIssueOwnerKey = Configuration.getByPath("stress.conf")
      .getString("privateKey.assetIssueOwnerKey");
  protected String participateOwnerAddress = Configuration.getByPath("stress.conf")
      .getString("address.participateOwnerAddress");
  protected String participateOwnerPrivateKey = Configuration.getByPath("stress.conf")
      .getString("privateKey.participateOwnerPrivateKey");
  protected String exchangeOwnerAddress = Configuration.getByPath("stress.conf")
      .getString("address.exchangeOwnerAddress");
  protected String exchangeOwnerKey = Configuration.getByPath("stress.conf")
      .getString("privateKey.exchangeOwnerKey");
  private String mutiSignOwnerAddress = Configuration.getByPath("stress.conf")
      .getString("address.mutiSignOwnerAddress");
  private String mutiSignOwnerKey = Configuration.getByPath("stress.conf")
      .getString("privateKey.mutiSignOwnerKey");

  Long firstTokenInitialBalance = 500000000L;
  Long secondTokenInitialBalance = 500000000L;

  //TXtrbmfwZ2LxtoCveEhZT86fTss1w8rwJE
  String witnessKey001 = Configuration.getByPath("stress.conf").getString("permissioner.key1");
  //TWKKwLswTTcK5cp31F2bAteQrzU8cYhtU5
  String witnessKey002 = Configuration.getByPath("stress.conf").getString("permissioner.key2");
  //TT4MHXVApKfbcq7cDLKnes9h9wLSD4eMJi
  String witnessKey003 = Configuration.getByPath("stress.conf").getString("permissioner.key3");
  //TCw4yb4hS923FisfMsxAzQ85srXkK6RWGk
  String witnessKey004 = Configuration.getByPath("stress.conf").getString("permissioner.key4");
  //TLYUrci5Qw5fUPho2GvFv38kAK4QSmdhhN
  String witnessKey005 = Configuration.getByPath("stress.conf").getString("permissioner.key5");

  private final byte[] witness001Address = PublicMethed.getFinalAddress(witnessKey001);
  private final byte[] witness002Address = PublicMethed.getFinalAddress(witnessKey002);
  private final byte[] witness003Address = PublicMethed.getFinalAddress(witnessKey003);
  private final byte[] witness004Address = PublicMethed.getFinalAddress(witnessKey004);
  private final byte[] witness005Address = PublicMethed.getFinalAddress(witnessKey005);

  private ManagedChannel channelFull = null;

  private WalletGrpc.WalletBlockingStub blockingStubFull = null;


  private String oldAddress;
  private String newAddress;
  private String newContractAddress;
  private String fullnode = stest.tron.wallet.common.client.Configuration.getByPath("stress.conf")
      .getStringList("fullnode.ip.list")
      .get(0);
  ByteString assetIssueId;
  Optional<ExchangeList> listExchange;
  byte[] commonContractAddress1;
  byte[] commonContractAddress2;
  byte[] commonContractAddress3;
  byte[] commonContractAddress4;


  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true)
  public void test1CreateProposal() {
    ChainParameters chainParameters = blockingStubFull
        .getChainParameters(EmptyMessage.newBuilder().build());
    Optional<ChainParameters> getChainParameters = Optional.ofNullable(chainParameters);
    logger.info(Long.toString(getChainParameters.get().getChainParameterCount()));
    for (Integer i = 0; i < getChainParameters.get().getChainParameterCount(); i++) {
      logger.info(getChainParameters.get().getChainParameter(i).getKey());
      logger.info(Long.toString(getChainParameters.get().getChainParameter(i).getValue()));
    }
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
/*    if (getChainParameters.get().getChainParameter(15).getValue() == 0) {
      proposalMap.put(15L, 1L);
    }
   if (getChainParameters.get().getChainParameter(16).getValue() == 0) {
      proposalMap.put(16L, 1L);
    }
    if (getChainParameters.get().getChainParameter(18).getValue() == 0) {
      proposalMap.put(18L, 1L);
    }*/
  /*if (getChainParameters.get().getChainParameter(22).getValue() == 0L) {
      logger.info("24 value is " + getChainParameters.get().getChainParameter(24).getValue());
      proposalMap.put(24L, 1L);
    }*/
    if (getChainParameters.get().getChainParameter(28).getValue() == 0L) {
      proposalMap.put(28L, 1L);
      proposalMap.put(30L, 1L);
    }
    if (getChainParameters.get().getChainParameter(27).getValue() == 0L) {
      proposalMap.put(29L, 1L);
    }

    if (proposalMap.size() >= 1) {

      PublicMethed.createProposal(witness001Address, witnessKey001,
          proposalMap, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      ProposalList proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
      Optional<ProposalList> listProposals = Optional.ofNullable(proposalList);
      final Integer proposalId = listProposals.get().getProposalsCount();
      PublicMethed.approveProposal(witness001Address, witnessKey001, proposalId,
          true, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      PublicMethed.approveProposal(witness002Address, witnessKey002, proposalId,
          true, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      PublicMethed.approveProposal(witness003Address, witnessKey003, proposalId,
          true, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      PublicMethed.approveProposal(witness004Address, witnessKey004, proposalId,
          true, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      PublicMethed.approveProposal(witness005Address, witnessKey005, proposalId,
          true, blockingStubFull);
      waitProposalApprove(32,blockingStubFull);
    }
  }

  @Test(enabled = true)
  public void test2SendCoinToStressAccount() {
    sendCoinToStressAccount(commonOwnerPrivateKey);
    sendCoinToStressAccount(triggerOwnerKey);
    sendCoinToStressAccount(commonToPrivateKey);
    sendCoinToStressAccount(assetIssueOwnerKey);
    sendCoinToStressAccount(participateOwnerPrivateKey);
    sendCoinToStressAccount(exchangeOwnerKey);
    sendCoinToStressAccount(mutiSignOwnerKey);
    logger.info(
        "commonOwnerAddress " + PublicMethed.queryAccount(commonOwnerPrivateKey, blockingStubFull)
            .getBalance());
    logger.info(
        "triggerOwnerAddress " + PublicMethed.queryAccount(triggerOwnerKey, blockingStubFull)
            .getBalance());
    logger.info("commonToAddress " + PublicMethed.queryAccount(commonToPrivateKey, blockingStubFull)
        .getBalance());
    logger.info(
        "assetIssueOwnerAddress " + PublicMethed.queryAccount(assetIssueOwnerKey, blockingStubFull)
            .getBalance());
    logger.info("participateOwnerAddress " + PublicMethed
        .queryAccount(participateOwnerPrivateKey, blockingStubFull).getBalance());
    logger.info("exchangeOwnerKey " + PublicMethed.queryAccount(exchangeOwnerKey, blockingStubFull)
        .getBalance());
    logger.info("mutiSignOwnerKey " + PublicMethed.queryAccount(mutiSignOwnerKey, blockingStubFull)
        .getBalance());
    PublicMethed
        .freezeBalanceGetEnergy(PublicMethed.getFinalAddress(triggerOwnerKey), 50000000000000L, 3,
            1, triggerOwnerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed
        .freezeBalanceGetEnergy(PublicMethed.getFinalAddress(triggerOwnerKey), 50000000000000L, 3,
            0, triggerOwnerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true)
  public void test3DeploySmartContract1() {
    String contractName = "tokenTest";
    String code = "608060405260e2806100126000396000f300608060405260043610603e5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416633be9ece781146043575b600080fd5b606873ffffffffffffffffffffffffffffffffffffffff60043516602435604435606a565b005b60405173ffffffffffffffffffffffffffffffffffffffff84169082156108fc029083908590600081818185878a8ad094505050505015801560b0573d6000803e3d6000fd5b505050505600a165627a7a72305820d7ac1a3b49eeff286b7f2402b93047e60deb6dba47f4f889d921dbcb3bb81f8a0029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"},{\"name\":\"id\",\"type\":\"trcToken\"},{\"name\":\"amount\",\"type\":\"uint256\"}],\"name\":\"TransferTokenTo\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    commonContractAddress1 = PublicMethed
        .deployContract(contractName, abi, code, "", 1000000000L,
            0L, 100, 10000, "0",
            0, null, triggerOwnerKey, PublicMethed.getFinalAddress(triggerOwnerKey),
            blockingStubFull);

    newContractAddress = WalletClient.encode58Check(commonContractAddress1);

    oldAddress = readWantedText("stress.conf", "commonContractAddress1");
    newAddress = "  commonContractAddress1 = " + newContractAddress;
    logger.info("oldAddress " + oldAddress);
    logger.info("newAddress " + newAddress);
    replacAddressInConfig("stress.conf", oldAddress, newAddress);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

  }

  @Test(enabled = true)
  public void test4DeploySmartContract2() {
    String contractName = "BTest";
    String code = "60806040526000805560c5806100166000396000f30060806040526004361060485763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166362548c7b8114604a578063890eba68146050575b005b6048608c565b348015605b57600080fd5b50d38015606757600080fd5b50d28015607357600080fd5b50607a6093565b60408051918252519081900360200190f35b6001600055565b600054815600a165627a7a723058204c4f1bb8eca0c4f1678cc7cc1179e03d99da2a980e6792feebe4d55c89c022830029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"setFlag\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"flag\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    commonContractAddress2 = PublicMethed
        .deployContract(contractName, abi, code, "", 1000000000L,
            0L, 100, 10000, "0",
            0, null, triggerOwnerKey, PublicMethed.getFinalAddress(triggerOwnerKey),
            blockingStubFull);

    newContractAddress = WalletClient.encode58Check(commonContractAddress2);

    oldAddress = readWantedText("stress.conf", "commonContractAddress2");
    newAddress = "  commonContractAddress2 = " + newContractAddress;
    logger.info("oldAddress " + oldAddress);
    logger.info("newAddress " + newAddress);
    replacAddressInConfig("stress.conf", oldAddress, newAddress);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true)
  public void test5DeploySmartContract3() {
    String contractName = "TestSStore";
    String code = "608060405234801561001057600080fd5b5061045c806100206000396000f30060806040526004361061006d576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff16806304c58438146100725780634f2be91f1461009f578063812db772146100b657806393cd5755146100e3578063d1cd64e914610189575b600080fd5b34801561007e57600080fd5b5061009d600480360381019080803590602001909291905050506101a0565b005b3480156100ab57600080fd5b506100b4610230565b005b3480156100c257600080fd5b506100e1600480360381019080803590602001909291905050506102a2565b005b3480156100ef57600080fd5b5061010e600480360381019080803590602001909291905050506102c3565b6040518080602001828103825283818151815260200191508051906020019080838360005b8381101561014e578082015181840152602081019050610133565b50505050905090810190601f16801561017b5780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b34801561019557600080fd5b5061019e61037e565b005b6000600190505b8181101561022c5760008060018154018082558091505090600182039060005260206000200160006040805190810160405280600881526020017f31323334353637380000000000000000000000000000000000000000000000008152509091909150908051906020019061021d92919061038b565b505080806001019150506101a7565b5050565b60008060018154018082558091505090600182039060005260206000200160006040805190810160405280600881526020017f61626364656667680000000000000000000000000000000000000000000000008152509091909150908051906020019061029e92919061038b565b5050565b6000600190505b81811115156102bf5780806001019150506102a9565b5050565b6000818154811015156102d257fe5b906000526020600020016000915090508054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156103765780601f1061034b57610100808354040283529160200191610376565b820191906000526020600020905b81548152906001019060200180831161035957829003601f168201915b505050505081565b6000808060010191505050565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f106103cc57805160ff19168380011785556103fa565b828001600101855582156103fa579182015b828111156103f95782518255916020019190600101906103de565b5b509050610407919061040b565b5090565b61042d91905b80821115610429576000816000905550600101610411565b5090565b905600a165627a7a7230582087d9880a135295a17100f63b8941457f4369204d3ccc9ce4a1abf99820eb68480029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"index\",\"type\":\"uint256\"}],\"name\":\"add2\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"add\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"index\",\"type\":\"uint256\"}],\"name\":\"fori2\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"args\",\"outputs\":[{\"name\":\"\",\"type\":\"string\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"fori\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"}]";
    commonContractAddress3 = PublicMethed
        .deployContract(contractName, abi, code, "", 1000000000L,
            0L, 100, 10000, "0",
            0, null, triggerOwnerKey, PublicMethed.getFinalAddress(triggerOwnerKey),
            blockingStubFull);

    newContractAddress = WalletClient.encode58Check(commonContractAddress3);

    oldAddress = readWantedText("stress.conf", "commonContractAddress3");
    newAddress = "  commonContractAddress3 = " + newContractAddress;
    logger.info("oldAddress " + oldAddress);
    logger.info("newAddress " + newAddress);
    replacAddressInConfig("stress.conf", oldAddress, newAddress);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true)
  public void test6CreateToken() {
    if (PublicMethed.queryAccount(assetIssueOwnerKey, blockingStubFull).getAssetIssuedID()
        .isEmpty()) {
      Long start = System.currentTimeMillis() + 20000;
      Long end = System.currentTimeMillis() + 1000000000;
      PublicMethed.createAssetIssue(PublicMethed.getFinalAddress(assetIssueOwnerKey), "xxd",
          50000000000000L,
          1, 1, start, end, 1, "wwwwww", "wwwwwwww", 100000L,
          100000L, 1L, 1L, assetIssueOwnerKey, blockingStubFull);
      logger.info("createAssetIssue");
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
    }

    assetIssueId = PublicMethed.queryAccount(assetIssueOwnerKey, blockingStubFull)
        .getAssetIssuedID();
    logger.info("AssetIssueId is " + ByteArray.toStr(assetIssueId.toByteArray()));

    logger.info("commonContractAddress1 is " + Wallet.encode58Check(commonContractAddress1));
    PublicMethed.transferAsset(commonContractAddress1, assetIssueId.toByteArray(), 300000000000L,
        PublicMethed.getFinalAddress(assetIssueOwnerKey), assetIssueOwnerKey, blockingStubFull);
    PublicMethed.transferAsset(commonContractAddress1, assetIssueId.toByteArray(), 300000000000L,
        PublicMethed.getFinalAddress(assetIssueOwnerKey), assetIssueOwnerKey, blockingStubFull);
    PublicMethed.transferAsset(commonContractAddress1, assetIssueId.toByteArray(), 300000000000L,
        PublicMethed.getFinalAddress(assetIssueOwnerKey), assetIssueOwnerKey, blockingStubFull);
    PublicMethed.transferAsset(commonContractAddress1, assetIssueId.toByteArray(), 300000000000L,
        PublicMethed.getFinalAddress(assetIssueOwnerKey), assetIssueOwnerKey, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.transferAsset(commonContractAddress2, assetIssueId.toByteArray(), 300000000000L,
        PublicMethed.getFinalAddress(assetIssueOwnerKey), assetIssueOwnerKey, blockingStubFull);
    PublicMethed.transferAsset(commonContractAddress2, assetIssueId.toByteArray(), 300000000000L,
        PublicMethed.getFinalAddress(assetIssueOwnerKey), assetIssueOwnerKey, blockingStubFull);
    PublicMethed.transferAsset(commonContractAddress2, assetIssueId.toByteArray(), 300000000000L,
        PublicMethed.getFinalAddress(assetIssueOwnerKey), assetIssueOwnerKey, blockingStubFull);
    PublicMethed.transferAsset(commonContractAddress2, assetIssueId.toByteArray(), 300000000000L,
        PublicMethed.getFinalAddress(assetIssueOwnerKey), assetIssueOwnerKey, blockingStubFull);

    String newTokenId = ByteArray.toStr(assetIssueId.toByteArray());
    String oldTokenIdString = readWantedText("stress.conf", "commontokenid");
    logger.info("oldTokenIdString " + oldTokenIdString);
    String newTokenIdInConfig = "commontokenid = " + newTokenId;
    logger.info("newTokenIdInConfig " + newTokenIdInConfig);
    replacAddressInConfig("stress.conf", oldTokenIdString, newTokenIdInConfig);
  }

  @Test(enabled = true)
  public void test7CreateExchange() {
    listExchange = PublicMethed.getExchangeList(blockingStubFull);
    Long exchangeId = 0L;
    assetIssueId = PublicMethed.queryAccount(exchangeOwnerKey, blockingStubFull).getAssetIssuedID();

    for (Integer i = 0; i < listExchange.get().getExchangesCount(); i++) {
      if (ByteArray.toHexString(listExchange.get().getExchanges(i)
          .getCreatorAddress().toByteArray()).equalsIgnoreCase(
          ByteArray.toHexString(PublicMethed.getFinalAddress(exchangeOwnerKey)))) {
        logger.info("id is " + listExchange.get().getExchanges(i).getExchangeId());
        exchangeId = listExchange.get().getExchanges(i).getExchangeId();
        break;
      }
    }

    if (exchangeId == 0L) {
      String trx = "_";
      byte[] b = trx.getBytes();
      PublicMethed.exchangeCreate(assetIssueId.toByteArray(), firstTokenInitialBalance,
          b, secondTokenInitialBalance, PublicMethed.getFinalAddress(exchangeOwnerKey),
          exchangeOwnerKey, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      listExchange = PublicMethed.getExchangeList(blockingStubFull);
      for (Integer i = 0; i < listExchange.get().getExchangesCount(); i++) {
        if (ByteArray.toHexString(listExchange.get().getExchanges(i)
            .getCreatorAddress().toByteArray()).equalsIgnoreCase(
            ByteArray.toHexString(PublicMethed.getFinalAddress(exchangeOwnerKey)))) {
          logger.info("id is " + listExchange.get().getExchanges(i).getExchangeId());
          exchangeId = listExchange.get().getExchanges(i).getExchangeId();
          break;
        }
      }
    }

    String newExchangeId = "" + exchangeId;
    String oldExchangeIdString = readWantedText("stress.conf", "commonexchangeid");
    logger.info("oldExchangeIdString " + oldExchangeIdString);
    String newTokenIdInConfig = "commonexchangeid = " + newExchangeId;
    logger.info("newTokenIdInConfig " + newTokenIdInConfig);
    replacAddressInConfig("stress.conf", oldExchangeIdString, newTokenIdInConfig);
  }


  @Test(enabled = true)
  public void test8MutiSignUpdate() {
    String[] permissionKeyString = new String[5];
    String[] ownerKeyString = new String[1];
    permissionKeyString[0] = witnessKey001;
    permissionKeyString[1] = witnessKey002;
    permissionKeyString[2] = witnessKey003;
    permissionKeyString[3] = witnessKey004;
    permissionKeyString[4] = witnessKey005;

    ownerKeyString[0] = mutiSignOwnerKey;

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":5,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey002) + "\",\"weight\":1}"
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey003) + "\",\"weight\":1}"
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey004) + "\",\"weight\":1}"
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey005)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey002) + "\",\"weight\":1}"
            + "]}]}";

    logger.info(accountPermissionJson);
    PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        PublicMethed.getFinalAddress(mutiSignOwnerKey), mutiSignOwnerKey,
        blockingStubFull, ownerKeyString);

  }

  @Test(enabled = true)
  public void test9DeploySmartContract4() {
    String contractName = "TRC20_TRON";
    String abi = "[{\"constant\":true,\"inputs\":[],\"name\":\"name\",\"outputs\":[{\"name\":\"\",\"type\":\"string\"}],\"payable\":false,\"type\":\"function\",\"stateMutability\":\"view\"},{\"constant\":false,\"inputs\":[],\"name\":\"stop\",\"outputs\":[],\"payable\":false,\"type\":\"function\",\"stateMutability\":\"nonpayable\"},{\"constant\":false,\"inputs\":[{\"name\":\"_spender\",\"type\":\"address\"},{\"name\":\"_value\",\"type\":\"uint256\"}],\"name\":\"approve\",\"outputs\":[{\"name\":\"success\",\"type\":\"bool\"}],\"payable\":false,\"type\":\"function\",\"stateMutability\":\"nonpayable\"},{\"constant\":true,\"inputs\":[],\"name\":\"totalSupply\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"type\":\"function\",\"stateMutability\":\"view\"},{\"constant\":false,\"inputs\":[{\"name\":\"_from\",\"type\":\"address\"},{\"name\":\"_to\",\"type\":\"address\"},{\"name\":\"_value\",\"type\":\"uint256\"}],\"name\":\"transferFrom\",\"outputs\":[{\"name\":\"success\",\"type\":\"bool\"}],\"payable\":false,\"type\":\"function\",\"stateMutability\":\"nonpayable\"},{\"constant\":true,\"inputs\":[],\"name\":\"decimals\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"type\":\"function\",\"stateMutability\":\"view\"},{\"constant\":false,\"inputs\":[{\"name\":\"_value\",\"type\":\"uint256\"}],\"name\":\"burn\",\"outputs\":[],\"payable\":false,\"type\":\"function\",\"stateMutability\":\"nonpayable\"},{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"address\"}],\"name\":\"balanceOf\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"type\":\"function\",\"stateMutability\":\"view\"},{\"constant\":true,\"inputs\":[],\"name\":\"stopped\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"type\":\"function\",\"stateMutability\":\"view\"},{\"constant\":true,\"inputs\":[],\"name\":\"symbol\",\"outputs\":[{\"name\":\"\",\"type\":\"string\"}],\"payable\":false,\"type\":\"function\",\"stateMutability\":\"view\"},{\"constant\":false,\"inputs\":[{\"name\":\"_to\",\"type\":\"address\"},{\"name\":\"_value\",\"type\":\"uint256\"}],\"name\":\"transfer\",\"outputs\":[{\"name\":\"success\",\"type\":\"bool\"}],\"payable\":false,\"type\":\"function\",\"stateMutability\":\"nonpayable\"},{\"constant\":false,\"inputs\":[],\"name\":\"start\",\"outputs\":[],\"payable\":false,\"type\":\"function\",\"stateMutability\":\"nonpayable\"},{\"constant\":false,\"inputs\":[{\"name\":\"_name\",\"type\":\"string\"}],\"name\":\"setName\",\"outputs\":[],\"payable\":false,\"type\":\"function\",\"stateMutability\":\"nonpayable\"},{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"address\"},{\"name\":\"\",\"type\":\"address\"}],\"name\":\"allowance\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"type\":\"function\",\"stateMutability\":\"view\"},{\"inputs\":[],\"payable\":false,\"type\":\"constructor\",\"stateMutability\":\"nonpayable\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"name\":\"_from\",\"type\":\"address\"},{\"indexed\":true,\"name\":\"_to\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"_value\",\"type\":\"uint256\"}],\"name\":\"Transfer\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"name\":\"_owner\",\"type\":\"address\"},{\"indexed\":true,\"name\":\"_spender\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"_value\",\"type\":\"uint256\"}],\"name\":\"Approval\",\"type\":\"event\"}]";
    String code = "6060604052604060405190810160405280600681526020017f54726f6e697800000000000000000000000000000000000000000000000000008152506000908051906020019062000052929190620001b6565b50604060405190810160405280600381526020017f545258000000000000000000000000000000000000000000000000000000000081525060019080519060200190620000a1929190620001b6565b50600660025560006005556000600660006101000a81548160ff0219169083151502179055506000600660016101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555034156200011257fe5b5b33600660016101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555067016345785d8a000060058190555067016345785d8a0000600360003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020819055505b62000265565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f10620001f957805160ff19168380011785556200022a565b828001600101855582156200022a579182015b82811115620002295782518255916020019190600101906200020c565b5b5090506200023991906200023d565b5090565b6200026291905b808211156200025e57600081600090555060010162000244565b5090565b90565b61111480620002756000396000f300606060405236156100ce576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff16806306fdde03146100d057806307da68f514610169578063095ea7b31461017b57806318160ddd146101d257806323b872dd146101f8578063313ce5671461026e57806342966c681461029457806370a08231146102b457806375f12b21146102fe57806395d89b4114610328578063a9059cbb146103c1578063be9a655514610418578063c47f00271461042a578063dd62ed3e14610484575bfe5b34156100d857fe5b6100e06104ed565b604051808060200182810382528381815181526020019150805190602001908083836000831461012f575b80518252602083111561012f5760208201915060208101905060208303925061010b565b505050905090810190601f16801561015b5780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b341561017157fe5b61017961058b565b005b341561018357fe5b6101b8600480803573ffffffffffffffffffffffffffffffffffffffff16906020019091908035906020019091905050610603565b604051808215151515815260200191505060405180910390f35b34156101da57fe5b6101e26107cb565b6040518082815260200191505060405180910390f35b341561020057fe5b610254600480803573ffffffffffffffffffffffffffffffffffffffff1690602001909190803573ffffffffffffffffffffffffffffffffffffffff169060200190919080359060200190919050506107d1565b604051808215151515815260200191505060405180910390f35b341561027657fe5b61027e610b11565b6040518082815260200191505060405180910390f35b341561029c57fe5b6102b26004808035906020019091905050610b17565b005b34156102bc57fe5b6102e8600480803573ffffffffffffffffffffffffffffffffffffffff16906020019091905050610c3f565b6040518082815260200191505060405180910390f35b341561030657fe5b61030e610c57565b604051808215151515815260200191505060405180910390f35b341561033057fe5b610338610c6a565b6040518080602001828103825283818151815260200191508051906020019080838360008314610387575b80518252602083111561038757602082019150602081019050602083039250610363565b505050905090810190601f1680156103b35780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b34156103c957fe5b6103fe600480803573ffffffffffffffffffffffffffffffffffffffff16906020019091908035906020019091905050610d08565b604051808215151515815260200191505060405180910390f35b341561042057fe5b610428610f31565b005b341561043257fe5b610482600480803590602001908201803590602001908080601f01602080910402602001604051908101604052809392919081815260200183838082843782019150505050505091905050610fa9565b005b341561048c57fe5b6104d7600480803573ffffffffffffffffffffffffffffffffffffffff1690602001909190803573ffffffffffffffffffffffffffffffffffffffff1690602001909190505061101e565b6040518082815260200191505060405180910390f35b60008054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156105835780601f1061055857610100808354040283529160200191610583565b820191906000526020600020905b81548152906001019060200180831161056657829003601f168201915b505050505081565b3373ffffffffffffffffffffffffffffffffffffffff16600660019054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff161415156105e457fe5b6001600660006101000a81548160ff0219169083151502179055505b5b565b6000600660009054906101000a900460ff1615151561061e57fe5b3373ffffffffffffffffffffffffffffffffffffffff1660001415151561064157fe5b60008214806106cc57506000600460003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002054145b15156106d85760006000fd5b81600460003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020819055508273ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff167f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b925846040518082815260200191505060405180910390a3600190505b5b5b92915050565b60055481565b6000600660009054906101000a900460ff161515156107ec57fe5b3373ffffffffffffffffffffffffffffffffffffffff1660001415151561080f57fe5b81600360008673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020541015151561085e5760006000fd5b600360008473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000205482600360008673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000205401101515156108ee5760006000fd5b81600460008673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020541015151561097a5760006000fd5b81600360008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206000828254019250508190555081600360008673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206000828254039250508190555081600460008673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020600082825403925050819055508273ffffffffffffffffffffffffffffffffffffffff168473ffffffffffffffffffffffffffffffffffffffff167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef846040518082815260200191505060405180910390a3600190505b5b5b9392505050565b60025481565b80600360003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000205410151515610b665760006000fd5b80600360003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020600082825403925050819055508060036000600073ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206000828254019250508190555060003373ffffffffffffffffffffffffffffffffffffffff167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef836040518082815260200191505060405180910390a35b50565b60036020528060005260406000206000915090505481565b600660009054906101000a900460ff1681565b60018054600181600116156101000203166002900480601f016020809104026020016040519081016040528092919081815260200182805460018160011615610100020316600290048015610d005780601f10610cd557610100808354040283529160200191610d00565b820191906000526020600020905b815481529060010190602001808311610ce357829003601f168201915b505050505081565b6000600660009054906101000a900460ff16151515610d2357fe5b3373ffffffffffffffffffffffffffffffffffffffff16600014151515610d4657fe5b81600360003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000205410151515610d955760006000fd5b600360008473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000205482600360008673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020540110151515610e255760006000fd5b81600360003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206000828254039250508190555081600360008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020600082825401925050819055508273ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef846040518082815260200191505060405180910390a3600190505b5b5b92915050565b3373ffffffffffffffffffffffffffffffffffffffff16600660019054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16141515610f8a57fe5b6000600660006101000a81548160ff0219169083151502179055505b5b565b3373ffffffffffffffffffffffffffffffffffffffff16600660019054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1614151561100257fe5b8060009080519060200190611018929190611043565b505b5b50565b6004602052816000526040600020602052806000526040600020600091509150505481565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061108457805160ff19168380011785556110b2565b828001600101855582156110b2579182015b828111156110b1578251825591602001919060010190611096565b5b5090506110bf91906110c3565b5090565b6110e591905b808211156110e15760008160009055506001016110c9565b5090565b905600a165627a7a723058204858328431ff0a4e0db74ff432e5805ce4bcf91a1c59650a93bd7c1aec5e0fe10029";
    commonContractAddress4 = PublicMethed
        .deployContract(contractName, abi, code, "", 1000000000L,
            0L, 100, 10000, "0",
            0, null, triggerOwnerKey, PublicMethed.getFinalAddress(triggerOwnerKey),
            blockingStubFull);

    newContractAddress = WalletClient.encode58Check(commonContractAddress4);

    oldAddress = readWantedText("stress.conf", "commonContractAddress4");
    newAddress = "  commonContractAddress4 = " + newContractAddress;
    logger.info("oldAddress " + oldAddress);
    logger.info("newAddress " + newAddress);
    replacAddressInConfig("stress.conf", oldAddress, newAddress);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }



  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  public void sendCoinToStressAccount(String key) {
    if (PublicMethed.queryAccount(key, blockingStubFull).getBalance() <= 498879998803847L) {
      PublicMethed.sendcoin(PublicMethed.getFinalAddress(key), 998879998803847L, witness004Address,
          witnessKey004, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
    }
  }


  public void replacAddressInConfig(String path, String oldAddress, String newAddress) {
    try {
      File file = new File(path);
      FileReader in = new FileReader(file);
      BufferedReader bufIn = new BufferedReader(in);
      CharArrayWriter tempStream = new CharArrayWriter();
      String line = null;
      while ((line = bufIn.readLine()) != null) {
        line = line.replaceAll(oldAddress, newAddress);
        tempStream.write(line);
        tempStream.append(System.getProperty("line.separator"));
      }
      bufIn.close();
      FileWriter out = new FileWriter(file);
      tempStream.writeTo(out);
      out.close();
      System.out.println("====path:" + path);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static String readWantedText(String url, String wanted) {
    try {
      FileReader fr = new FileReader(url);
      BufferedReader br = new BufferedReader(fr);
      String temp = "";
      while (temp != null) {
        temp = br.readLine();
        if (temp != null && temp.contains(wanted)) {
          System.out.println(temp);
          return temp;
        }
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return "";
  }

  public static void waitProposalApprove(Integer proposalIndex,WalletGrpc.WalletBlockingStub blockingStubFull) {
    Long currentTime = System.currentTimeMillis();
    while (System.currentTimeMillis() <= currentTime + 610000) {
      ChainParameters chainParameters = blockingStubFull
          .getChainParameters(EmptyMessage.newBuilder().build());
      Optional<ChainParameters> getChainParameters = Optional.ofNullable(chainParameters);
      if (getChainParameters.get().getChainParameter(proposalIndex).getValue() == 1L) {
        logger.info("Proposal has been approval");
        return;
      }
      PublicMethed.waitProduceNextBlock(blockingStubFull);
    }



  }


}


