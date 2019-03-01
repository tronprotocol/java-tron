package stest.tron.wallet.precondition;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ExchangeList;
import org.tron.api.GrpcAPI.ProposalList;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Configuration;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.ChainParameters;
import org.tron.protos.Protocol.Exchange;
import org.tron.protos.Protocol.SmartContract;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.PublicMethed;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.CharArrayWriter;
import java.io.InputStreamReader;
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

  protected String commonContractAddress1 = Configuration.getByPath("stress.conf")
      .getString("address.commonContractAddress1");
  protected String commonContractAddress2 = Configuration.getByPath("stress.conf")
      .getString("address.commonContractAddress2");
  protected String commonContractAddress3 = Configuration.getByPath("stress.conf")
      .getString("address.commonContractAddress3");
  protected String commontokenid = Configuration.getByPath("stress.conf")
      .getString("param.commontokenid");
  protected long commonexchangeid = Configuration.getByPath("stress.conf")
      .getLong("param.commonexchangeid");

  protected String delegateResourceAddress = Configuration.getByPath("stress.conf")
      .getString("address.delegateResourceAddress");
  protected String delegateResourceKey = Configuration.getByPath("stress.conf")
      .getString("privateKey.delegateResourceKey");

  protected String assetIssueOwnerAddress = Configuration.getByPath("stress.conf").getString("address.assetIssueOwnerAddress");
  protected String assetIssueOwnerKey = Configuration.getByPath("stress.conf").getString("privateKey.assetIssueOwnerKey");
  protected String participateOwnerAddress = Configuration.getByPath("stress.conf").getString("address.participateOwnerAddress");
  protected String participateOwnerPrivateKey = Configuration.getByPath("stress.conf").getString("privateKey.participateOwnerPrivateKey");
  protected String exchangeOwnerAddress = Configuration.getByPath("stress.conf").getString("address.exchangeOwnerAddress");
  protected String exchangeOwnerKey = Configuration.getByPath("stress.conf").getString("privateKey.exchangeOwnerKey");
  private String mutiSignOwnerAddress = Configuration.getByPath("stress.conf").getString("address.mutiSignOwnerAddress");
  private String mutiSignOwnerKey = Configuration.getByPath("stress.conf").getString("privateKey.mutiSignOwnerKey");

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
  private String fullnode = stest.tron.wallet.common.client.Configuration.getByPath("stress.conf").getStringList("fullnode.ip.list")
      .get(0);
  ByteString assetIssueId;
  Optional<ExchangeList> listExchange;

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
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();

    if (getChainParameters.get().getChainParameter(20).getValue() == 0L) {
      proposalMap.put(20L, 1L);
    }

    if (getChainParameters.get().getChainParameter(21).getValue() == 0L) {
      proposalMap.put(21L, 1L);
    }

    if (!proposalMap.isEmpty()) {
      PublicMethed.createProposal(witness001Address,witnessKey001,
          proposalMap,blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      ProposalList proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
      Optional<ProposalList> listProposals =  Optional.ofNullable(proposalList);
      final Integer proposalId = listProposals.get().getProposalsCount();
      PublicMethed.approveProposal(witness001Address,witnessKey001,proposalId,
          true,blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      PublicMethed.approveProposal(witness002Address,witnessKey002,proposalId,
          true,blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      PublicMethed.approveProposal(witness003Address,witnessKey003,proposalId,
          true,blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      PublicMethed.approveProposal(witness004Address,witnessKey004,proposalId,
          true,blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      PublicMethed.approveProposal(witness005Address,witnessKey005,proposalId,
          true,blockingStubFull);
      try {
        Thread.sleep(700000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
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
    logger.info("commonOwnerAddress " + PublicMethed.queryAccount(commonOwnerPrivateKey,blockingStubFull).getBalance());
    logger.info("triggerOwnerAddress " + PublicMethed.queryAccount(triggerOwnerKey,blockingStubFull).getBalance());
    logger.info("commonToAddress " + PublicMethed.queryAccount(commonToPrivateKey,blockingStubFull).getBalance());
    logger.info("assetIssueOwnerAddress " + PublicMethed.queryAccount(assetIssueOwnerKey,blockingStubFull).getBalance());
    logger.info("participateOwnerAddress " + PublicMethed.queryAccount(participateOwnerPrivateKey,blockingStubFull).getBalance());
    logger.info("exchangeOwnerKey " + PublicMethed.queryAccount(exchangeOwnerKey,blockingStubFull).getBalance());
    logger.info("mutiSignOwnerKey " + PublicMethed.queryAccount(mutiSignOwnerKey,blockingStubFull).getBalance());
    PublicMethed.freezeBalanceGetEnergy(PublicMethed.getFinalAddress(triggerOwnerKey),50000000000000L,3,1,triggerOwnerKey,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.freezeBalanceGetEnergy(PublicMethed.getFinalAddress(triggerOwnerKey),50000000000000L,3,0,triggerOwnerKey,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true)
  public void test3DeploySmartContract1() {
    String contractName = "tokenTest";
    String code = "608060405260e2806100126000396000f300608060405260043610603e5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416633be9ece781146043575b600080fd5b606873ffffffffffffffffffffffffffffffffffffffff60043516602435604435606a565b005b60405173ffffffffffffffffffffffffffffffffffffffff84169082156108fc029083908590600081818185878a8ad094505050505015801560b0573d6000803e3d6000fd5b505050505600a165627a7a72305820d7ac1a3b49eeff286b7f2402b93047e60deb6dba47f4f889d921dbcb3bb81f8a0029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"},{\"name\":\"id\",\"type\":\"trcToken\"},{\"name\":\"amount\",\"type\":\"uint256\"}],\"name\":\"TransferTokenTo\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    byte[] commonContractAddress1 = PublicMethed
        .deployContract(contractName, abi, code, "", 1000000000L,
            0L, 100, 10000, "0",
            0, null, triggerOwnerKey, PublicMethed.getFinalAddress(triggerOwnerKey),
            blockingStubFull);

    newContractAddress = WalletClient.encode58Check(commonContractAddress1);

    oldAddress = readWantedText("stress.conf","commonContractAddress1");
    newAddress = "  commonContractAddress1 = " + newContractAddress;
    logger.info("oldAddress " + oldAddress);
    logger.info("newAddress " + newAddress);
    replacAddressInConfig("stress.conf",oldAddress,newAddress);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true)
  public void test4DeploySmartContract2() {
    String contractName = "BTest";
    String code = "60806040526000805560c5806100166000396000f30060806040526004361060485763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166362548c7b8114604a578063890eba68146050575b005b6048608c565b348015605b57600080fd5b50d38015606757600080fd5b50d28015607357600080fd5b50607a6093565b60408051918252519081900360200190f35b6001600055565b600054815600a165627a7a723058204c4f1bb8eca0c4f1678cc7cc1179e03d99da2a980e6792feebe4d55c89c022830029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"setFlag\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"flag\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    byte[] commonContractAddress2 = PublicMethed
        .deployContract(contractName, abi, code, "", 1000000000L,
            0L, 100, 10000, "0",
            0, null, triggerOwnerKey, PublicMethed.getFinalAddress(triggerOwnerKey),
            blockingStubFull);

    newContractAddress = WalletClient.encode58Check(commonContractAddress2);

    oldAddress = readWantedText("stress.conf","commonContractAddress2");
    newAddress = "  commonContractAddress2 = " + newContractAddress;
    logger.info("oldAddress " + oldAddress);
    logger.info("newAddress " + newAddress);
    replacAddressInConfig("stress.conf",oldAddress,newAddress);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true)
  public void test5DeploySmartContract3() {
    String contractName = "TestSStore";
    String code = "608060405234801561001057600080fd5b5061045c806100206000396000f30060806040526004361061006d576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff16806304c58438146100725780634f2be91f1461009f578063812db772146100b657806393cd5755146100e3578063d1cd64e914610189575b600080fd5b34801561007e57600080fd5b5061009d600480360381019080803590602001909291905050506101a0565b005b3480156100ab57600080fd5b506100b4610230565b005b3480156100c257600080fd5b506100e1600480360381019080803590602001909291905050506102a2565b005b3480156100ef57600080fd5b5061010e600480360381019080803590602001909291905050506102c3565b6040518080602001828103825283818151815260200191508051906020019080838360005b8381101561014e578082015181840152602081019050610133565b50505050905090810190601f16801561017b5780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b34801561019557600080fd5b5061019e61037e565b005b6000600190505b8181101561022c5760008060018154018082558091505090600182039060005260206000200160006040805190810160405280600881526020017f31323334353637380000000000000000000000000000000000000000000000008152509091909150908051906020019061021d92919061038b565b505080806001019150506101a7565b5050565b60008060018154018082558091505090600182039060005260206000200160006040805190810160405280600881526020017f61626364656667680000000000000000000000000000000000000000000000008152509091909150908051906020019061029e92919061038b565b5050565b6000600190505b81811115156102bf5780806001019150506102a9565b5050565b6000818154811015156102d257fe5b906000526020600020016000915090508054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156103765780601f1061034b57610100808354040283529160200191610376565b820191906000526020600020905b81548152906001019060200180831161035957829003601f168201915b505050505081565b6000808060010191505050565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f106103cc57805160ff19168380011785556103fa565b828001600101855582156103fa579182015b828111156103f95782518255916020019190600101906103de565b5b509050610407919061040b565b5090565b61042d91905b80821115610429576000816000905550600101610411565b5090565b905600a165627a7a7230582087d9880a135295a17100f63b8941457f4369204d3ccc9ce4a1abf99820eb68480029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"index\",\"type\":\"uint256\"}],\"name\":\"add2\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"add\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"index\",\"type\":\"uint256\"}],\"name\":\"fori2\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"args\",\"outputs\":[{\"name\":\"\",\"type\":\"string\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"fori\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"}]";
    byte[] commonContractAddress3 = PublicMethed
        .deployContract(contractName, abi, code, "", 1000000000L,
            0L, 100, 10000, "0",
            0, null, triggerOwnerKey, PublicMethed.getFinalAddress(triggerOwnerKey),
            blockingStubFull);

    newContractAddress = WalletClient.encode58Check(commonContractAddress3);

    oldAddress = readWantedText("stress.conf","commonContractAddress3");
    newAddress = "  commonContractAddress3 = " + newContractAddress;
    logger.info("oldAddress " + oldAddress);
    logger.info("newAddress " + newAddress);
    replacAddressInConfig("stress.conf",oldAddress,newAddress);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true)
  public void test6CreateToken() {
    if (PublicMethed.queryAccount(assetIssueOwnerKey, blockingStubFull).getAssetIssuedID().isEmpty()) {
      Long start = System.currentTimeMillis() + 20000;
      Long end = System.currentTimeMillis() + 1000000000;
      PublicMethed.createAssetIssue(PublicMethed.getFinalAddress(assetIssueOwnerKey), "xxd", 50000000000000L,
          1, 1, start, end, 1, "wwwwww", "wwwwwwww", 100000L,
          100000L, 1L, 1L, assetIssueOwnerKey, blockingStubFull);
      logger.info("createAssetIssue");
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
    }

    assetIssueId = PublicMethed.queryAccount(assetIssueOwnerKey, blockingStubFull).getAssetIssuedID();

    logger.info(ByteArray.toStr(assetIssueId.toByteArray()));
    String commonContractAddress1 = Configuration.getByPath("stress.conf")
        .getString("address.commonContractAddress1");
    String commonContractAddress2 = Configuration.getByPath("stress.conf")
        .getString("address.commonContractAddress2");

    PublicMethed.transferAsset(WalletClient.decodeFromBase58Check(commonContractAddress1),assetIssueId.toByteArray(),3000000000000L,PublicMethed.getFinalAddress(assetIssueOwnerKey),assetIssueOwnerKey,blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.transferAsset(WalletClient.decodeFromBase58Check(commonContractAddress2),assetIssueId.toByteArray(),3000000000000L,PublicMethed.getFinalAddress(assetIssueOwnerKey),assetIssueOwnerKey,blockingStubFull);


    String newTokenId = ByteArray.toStr(assetIssueId.toByteArray());
    String oldTokenIdString = readWantedText("stress.conf","commontokenid");
    logger.info("oldTokenIdString " + oldTokenIdString);
    String newTokenIdInConfig = "commontokenid = " + newTokenId;
    logger.info("newTokenIdInConfig " + newTokenIdInConfig);
    replacAddressInConfig("stress.conf",oldTokenIdString,newTokenIdInConfig);
  }

  @Test(enabled = true)
  public void test7CreateExchange() {
    listExchange = PublicMethed.getExchangeList(blockingStubFull);
    Long exchangeId = 0L;
    assetIssueId = PublicMethed.queryAccount(exchangeOwnerKey, blockingStubFull).getAssetIssuedID();

    for (Integer i = 0; i < listExchange.get().getExchangesCount(); i++) {
      if (ByteArray.toHexString(listExchange.get().getExchanges(i)
          .getCreatorAddress().toByteArray()).equalsIgnoreCase(ByteArray.toHexString(PublicMethed.getFinalAddress(exchangeOwnerKey)))) {
        logger.info("id is " + listExchange.get().getExchanges(i).getExchangeId());
        exchangeId = listExchange.get().getExchanges(i).getExchangeId();
        break;
      }
    }


    if (exchangeId == 0L) {
      String trx = "_";
      byte[] b = trx.getBytes();
      PublicMethed.exchangeCreate(assetIssueId.toByteArray(),firstTokenInitialBalance,
          b,secondTokenInitialBalance,PublicMethed.getFinalAddress(exchangeOwnerKey),
          exchangeOwnerKey,blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      listExchange = PublicMethed.getExchangeList(blockingStubFull);
      for (Integer i = 0; i < listExchange.get().getExchangesCount(); i++) {
        if (ByteArray.toHexString(listExchange.get().getExchanges(i)
            .getCreatorAddress().toByteArray()).equalsIgnoreCase(ByteArray.toHexString(PublicMethed.getFinalAddress(exchangeOwnerKey)))) {
          logger.info("id is " + listExchange.get().getExchanges(i).getExchangeId());
          exchangeId = listExchange.get().getExchanges(i).getExchangeId();
          break;
        }
      }
    }

    String newExchangeId = "" + exchangeId;
    String oldExchangeIdString = readWantedText("stress.conf","commonexchangeid");
    logger.info("oldExchangeIdString " + oldExchangeIdString);
    String newTokenIdInConfig = "commonexchangeid = " + newExchangeId;
    logger.info("newTokenIdInConfig " + newTokenIdInConfig);
    replacAddressInConfig("stress.conf",oldExchangeIdString,newTokenIdInConfig);
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
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey005) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey002) + "\",\"weight\":1}"
            + "]}]}";

    logger.info(accountPermissionJson);
    PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,PublicMethed.getFinalAddress(mutiSignOwnerKey),mutiSignOwnerKey,
        blockingStubFull,ownerKeyString);

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
    if (PublicMethed.queryAccount(key,blockingStubFull).getBalance() <= 498879998803847L) {
      PublicMethed.sendcoin(PublicMethed.getFinalAddress(key),998879998803847L,witness004Address,witnessKey004,blockingStubFull);
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




}


