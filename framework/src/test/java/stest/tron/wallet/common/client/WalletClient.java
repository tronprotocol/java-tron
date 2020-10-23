package stest.tron.wallet.common.client;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI.AccountNetMessage;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.GrpcAPI.BlockList;
import org.tron.api.GrpcAPI.NodeList;
import org.tron.api.GrpcAPI.TransactionList;
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.common.crypto.ECKey;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.Utils;
import org.tron.core.exception.CancelException;
import org.tron.core.exception.CipherException;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Witness;
import org.tron.protos.contract.AccountContract.AccountCreateContract;
import org.tron.protos.contract.AccountContract.AccountUpdateContract;
import org.tron.protos.contract.AssetIssueContractOuterClass;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.ParticipateAssetIssueContract;
import org.tron.protos.contract.BalanceContract.FreezeBalanceContract;
import org.tron.protos.contract.BalanceContract.TransferContract;
import org.tron.protos.contract.BalanceContract.UnfreezeBalanceContract;
import org.tron.protos.contract.BalanceContract.WithdrawBalanceContract;
import org.tron.protos.contract.WitnessContract;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.TransactionUtils;

public class WalletClient {

  private static final Logger logger = LoggerFactory.getLogger("WalletClient");
  private static final String FilePath = "Wallet";
  private static GrpcClient rpcCli;
  private static String dbPath;
  private static String txtPath;
  private static byte addressPreFixByte = CommonConstant.ADD_PRE_FIX_BYTE_MAINNET;
  private ECKey ecKey = null;
  private boolean loginState = false;

  /**
   * Creates a new WalletClient with a random ECKey or no ECKey.
   */

  public WalletClient(boolean genEcKey) {
    if (genEcKey) {
      this.ecKey = new ECKey(Utils.getRandom());
    }
  }

  /**
   * constructor.
   */

  //  Create Wallet with a pritKey
  public WalletClient(String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    this.ecKey = temKey;
  }

  /**
   * Creates a Wallet with an existing ECKey.
   */

  public WalletClient(final ECKey ecKey) {
    this.ecKey = ecKey;
  }

  /**
   * constructor.
   */

  public static boolean init(int itype) {
    Config config = Configuration.getByPath("testng.conf");
    dbPath = config.getString("CityDb.DbPath");
    txtPath = System.getProperty("user.dir") + '/' + config.getString("CityDb.TxtPath");

    String fullNodepathname = "";

    if (1000 == itype) {
      fullNodepathname = "checkfullnode.ip.list";
    } else {
      fullNodepathname = "fullnode.ip.list";
    }
    String fullNode = "";
    String solidityNode = "";
    if (config.hasPath("soliditynode.ip.list")) {
      solidityNode = config.getStringList("soliditynode.ip.list").get(0);
    }
    if (config.hasPath(fullNodepathname)) {
      fullNode = config.getStringList(fullNodepathname).get(itype);
    }
    if (config.hasPath("net.type") && "mainnet".equalsIgnoreCase(config.getString("net.type"))) {
      WalletClient.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    } else {
      WalletClient.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_TESTNET);
    }
    rpcCli = new GrpcClient(fullNode, solidityNode);
    return true;
  }

  /**
   * constructor.
   */

  public static GrpcClient init() {
    //Config config = org.tron.core.config.Configuration.getByPath("config.conf");
    Config config = Configuration.getByPath("testng.conf");
    dbPath = config.getString("CityDb.DbPath");
    txtPath = System.getProperty("user.dir") + "/" + config.getString("CityDb.TxtPath");

    String fullNode = "";
    String solidityNode = "";
    if (config.hasPath("soliditynode.ip.list")) {
      solidityNode = config.getStringList("soliditynode.ip.list").get(0);
    }
    if (config.hasPath("fullnode.ip.list")) {
      fullNode = config.getStringList("fullnode.ip.list").get(0);
    }
    if (config.hasPath("net.type") && "mainnet".equalsIgnoreCase(config.getString("net.type"))) {
      WalletClient.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    } else {
      WalletClient.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_TESTNET);
    }
    return new GrpcClient(fullNode, solidityNode);
  }

  public static byte getAddressPreFixByte() {
    return addressPreFixByte;
  }

  public static void setAddressPreFixByte(byte addressPreFixByte) {
    WalletClient.addressPreFixByte = addressPreFixByte;
  }

  /**
   * constructor.
   */

  public static String selectFullNode() {
    Map<String, String> witnessMap = new HashMap<>();
    Config config = Configuration.getByPath("config.conf");
    List list = config.getObjectList("witnesses.witnessList");
    for (int i = 0; i < list.size(); i++) {
      ConfigObject obj = (ConfigObject) list.get(i);
      String ip = obj.get("ip").unwrapped().toString();
      String url = obj.get("url").unwrapped().toString();
      witnessMap.put(url, ip);
    }

    Optional<WitnessList> result = rpcCli.listWitnesses();
    long minMissedNum = 100000000L;
    String minMissedWitness = "";
    if (result.isPresent()) {
      List<Witness> witnessList = result.get().getWitnessesList();
      for (Witness witness : witnessList) {
        String url = witness.getUrl();
        long missedBlocks = witness.getTotalMissed();
        if (missedBlocks < minMissedNum) {
          minMissedNum = missedBlocks;
          minMissedWitness = url;
        }
      }
    }
    if (witnessMap.containsKey(minMissedWitness)) {
      return witnessMap.get(minMissedWitness);
    } else {
      return "";
    }
  }

  public static String getDbPath() {
    return dbPath;
  }

  public static String getTxtPath() {
    return txtPath;
  }

  public static Account queryAccount(byte[] address) {
    return rpcCli.queryAccount(address);//call rpc
  }

  /**
   * constructor.
   */

  public Account queryAccount() {
    byte[] address;
    if (this.ecKey == null) {
      String pubKey = loadPubKey(); //04 PubKey[128]
      if (StringUtils.isEmpty(pubKey)) {
        logger.warn("Warning: QueryAccount failed, no wallet address !!");
        return null;
      }
      byte[] pubKeyAsc = pubKey.getBytes();
      byte[] pubKeyHex = Hex.decode(pubKeyAsc);
      this.ecKey = ECKey.fromPublicOnly(pubKeyHex);
    }
    return queryAccount(getAddress());
  }

  /**
   * constructor.
   */

  public static Transaction createTransferAssetTransaction(byte[] to, byte[] assertName,
      byte[] owner, long amount) {
    AssetIssueContractOuterClass.TransferAssetContract contract = createTransferAssetContract(to,
        assertName, owner,
        amount);
    return rpcCli.createTransferAssetTransaction(contract);
  }

  /**
   * constructor.
   */

  public static Transaction participateAssetIssueTransaction(byte[] to, byte[] assertName,
      byte[] owner, long amount) {
    AssetIssueContractOuterClass.ParticipateAssetIssueContract contract =
        participateAssetIssueContract(to, assertName, owner, amount);
    return rpcCli.createParticipateAssetIssueTransaction(contract);
  }

  /**
   * constructor.
   */

  public static Transaction updateAccountTransaction(byte[] addressBytes, byte[] accountNameBytes) {
    AccountUpdateContract contract = createAccountUpdateContract(accountNameBytes,
        addressBytes);
    return rpcCli.createTransaction(contract);
  }

  /**
   * constructor.
   */

  public static boolean broadcastTransaction(byte[] transactionBytes)
      throws InvalidProtocolBufferException {
    Transaction transaction = Transaction.parseFrom(transactionBytes);
    if (false == TransactionUtils.validTransaction(transaction)) {
      return false;
    }
    return rpcCli.broadcastTransaction(transaction);
  }

  /**
   * constructor.
   */

  public static Transaction createWitnessTransaction(byte[] owner, byte[] url) {
    WitnessContract.WitnessCreateContract contract = createWitnessCreateContract(owner, url);
    return rpcCli.createWitness(contract);
  }

  public static Transaction createVoteWitnessTransaction(byte[] owner,
      HashMap<String, String> witness) {
    WitnessContract.VoteWitnessContract contract = createVoteWitnessContract(owner, witness);
    return rpcCli.voteWitnessAccount(contract);
  }

  public static Transaction createAssetIssueTransaction(AssetIssueContract contract) {
    return rpcCli.createAssetIssue(contract);
  }

  public static Block getGetBlock(long blockNum) {
    return rpcCli.getBlock(blockNum);
  }

  /**
   * constructor.
   */

  public static TransferContract createTransferContract(byte[] to, byte[] owner,
      long amount) {
    TransferContract.Builder builder = TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);
    return builder.build();
  }

  /**
   * constructor.
   */

  public static AssetIssueContractOuterClass.TransferAssetContract createTransferAssetContract(
      byte[] to, byte[] assertName, byte[] owner, long amount) {
    AssetIssueContractOuterClass.TransferAssetContract.Builder builder =
        AssetIssueContractOuterClass.TransferAssetContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsName = ByteString.copyFrom(assertName);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setAssetName(bsName);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    return builder.build();
  }

  /**
   * constructor.
   */

  public static ParticipateAssetIssueContract participateAssetIssueContract(
      byte[] to, byte[] assertName, byte[] owner, long amount) {
    ParticipateAssetIssueContract.Builder builder =
        ParticipateAssetIssueContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsName = ByteString.copyFrom(assertName);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setAssetName(bsName);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    return builder.build();
  }

  public static Transaction createTransaction4Transfer(TransferContract contract) {
    Transaction transaction = rpcCli.createTransaction(contract);
    return transaction;
  }

  /**
   * constructor.
   */

  public static AccountCreateContract createAccountCreateContract(byte[] owner,
      byte[] address) {
    AccountCreateContract.Builder builder = AccountCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setAccountAddress(ByteString.copyFrom(address));
    return builder.build();
  }

  /**
   * constructor.
   */

  public static AccountCreateContract createAccountCreateContract(
      AccountType accountType, byte[] accountName, byte[] address) {
    AccountCreateContract.Builder builder = AccountCreateContract.newBuilder();
    ByteString bsAddress = ByteString.copyFrom(address);
    ByteString bsAccountName = ByteString.copyFrom(accountName);
    builder.setType(accountType);
    builder.setAccountAddress(bsAccountName);
    builder.setOwnerAddress(bsAddress);
    return builder.build();
  }

  /**
   * constructor.
   */

  public static Transaction createAccountTransaction(byte[] owner, byte[] address) {
    AccountCreateContract contract = createAccountCreateContract(owner, address);
    return rpcCli.createAccount(contract);
  }

  /**
   * constructor.
   */

  public static AccountUpdateContract createAccountUpdateContract(byte[] accountName,
      byte[] address) {
    AccountUpdateContract.Builder builder = AccountUpdateContract.newBuilder();
    ByteString bsAddress = ByteString.copyFrom(address);
    ByteString bsAccountName = ByteString.copyFrom(accountName);

    builder.setAccountName(bsAccountName);
    builder.setOwnerAddress(bsAddress);

    return builder.build();
  }

  /**
   * constructor.
   */

  public static WitnessContract.WitnessCreateContract createWitnessCreateContract(byte[] owner,
      byte[] url) {
    WitnessContract.WitnessCreateContract.Builder builder = WitnessContract.WitnessCreateContract
        .newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setUrl(ByteString.copyFrom(url));

    return builder.build();
  }

  /**
   * constructor.
   */

  public static WitnessContract.VoteWitnessContract createVoteWitnessContract(
      byte[] owner, HashMap<String, String> witness) {
    WitnessContract.VoteWitnessContract.Builder builder = WitnessContract.VoteWitnessContract
        .newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    for (String addressBase58 : witness.keySet()) {
      String value = witness.get(addressBase58);
      long count = Long.parseLong(value);
      WitnessContract.VoteWitnessContract.Vote.Builder voteBuilder
          = WitnessContract.VoteWitnessContract.Vote.newBuilder();
      byte[] address = WalletClient.decodeFromBase58Check(addressBase58);
      if (address == null) {
        continue;
      }
      voteBuilder.setVoteAddress(ByteString.copyFrom(address));
      voteBuilder.setVoteCount(count);
      builder.addVotes(voteBuilder.build());
    }

    return builder.build();
  }

  public static AccountNetMessage getAccountNet(byte[] address) {
    return rpcCli.getAccountNet(address);
  }

  private static String loadPassword() {
    char[] buf = new char[0x100];
    int len = FileUtil.readData(FilePath, buf);
    if (len != 226) {
      return null;
    }
    return String.valueOf(buf, 0, 32);
  }

  /**
   * constructor.
   */

  public static String loadPubKey() {
    char[] buf = new char[0x100];
    int len = FileUtil.readData(FilePath, buf);
    if (len != 226) {
      return null;
    }
    return String.valueOf(buf, 32, 130);
  }

  private static String loadPriKey() {
    char[] buf = new char[0x100];
    int len = FileUtil.readData(FilePath, buf);
    if (len != 226) {
      return null;
    }
    return String.valueOf(buf, 162, 64);
  }

  /**
   * Get a Wallet from storage.
   */

  public static WalletClient getWalletByStorageIgnorPrivKey() {
    try {
      String pubKey = loadPubKey(); //04 PubKey[128]
      if (StringUtils.isEmpty(pubKey)) {
        return null;
      }
      byte[] pubKeyAsc = pubKey.getBytes();
      byte[] pubKeyHex = Hex.decode(pubKeyAsc);
      ECKey eccKey = ECKey.fromPublicOnly(pubKeyHex);
      return new WalletClient(eccKey);
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  /**
   * constructor.
   */

  public static String getAddressByStorage() {
    try {
      String pubKey = loadPubKey(); //04 PubKey[128]
      if (StringUtils.isEmpty(pubKey)) {
        return null;
      }
      byte[] pubKeyAsc = pubKey.getBytes();
      byte[] pubKeyHex = Hex.decode(pubKeyAsc);
      ECKey eccKey = ECKey.fromPublicOnly(pubKeyHex);
      return ByteArray.toHexString(eccKey.getAddress());
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  /**
   * constructor.
   */

  public static byte[] getPassWord(String password) {
    if (!passwordValid(password)) {
      return null;
    }
    byte[] pwd;
    pwd = Sha256Hash.hash(CommonParameter
        .getInstance().isECKeyCryptoEngine(), password.getBytes());
    pwd = Sha256Hash.hash(CommonParameter
        .getInstance().isECKeyCryptoEngine(), pwd);
    pwd = Arrays.copyOfRange(pwd, 0, 16);
    return pwd;
  }

  /**
   * constructor.
   */

  public static byte[] getEncKey(String password) {
    if (!passwordValid(password)) {
      return null;
    }
    byte[] encKey;
    encKey = Sha256Hash.hash(CommonParameter
        .getInstance().isECKeyCryptoEngine(), password.getBytes());
    encKey = Arrays.copyOfRange(encKey, 0, 16);
    return encKey;
  }

  /**
   * constructor.
   */

  public static boolean checkPassWord(String password) {
    byte[] pwd = getPassWord(password);
    if (pwd == null) {
      return false;
    }
    String pwdAsc = ByteArray.toHexString(pwd);
    String pwdInstore = loadPassword();
    return pwdAsc.equals(pwdInstore);
  }

  /**
   * constructor.
   */

  public static boolean passwordValid(String password) {
    if (StringUtils.isEmpty(password)) {
      logger.warn("Warning: Password is empty !!");
      return false;
    }
    if (password.length() < 6) {
      logger.warn("Warning: Password is too short !!");
      return false;
    }
    //Other rule;
    return true;
  }

  /**
   * constructor.
   */

  public static boolean addressValid(byte[] address) {
    if (address == null || address.length == 0) {
      logger.warn("Warning: Address is empty !!");
      return false;
    }
    if (address.length != CommonConstant.ADDRESS_SIZE) {
      logger.warn(
          "Warning: Address length need " + CommonConstant.ADDRESS_SIZE + " but " + address.length
              + " !!");
      return false;
    }
    byte preFixbyte = address[0];
    if (preFixbyte != getAddressPreFixByte()) {
      logger.warn("Warning: Address need prefix with " + getAddressPreFixByte() + " but "
          + preFixbyte + " !!");
      return false;
    }
    //Other rule;
    return true;
  }

  /**
   * constructor.
   */

  public static String encode58Check(byte[] input) {
    byte[] hash0 = Sha256Hash.hash(CommonParameter
        .getInstance().isECKeyCryptoEngine(), input);
    byte[] hash1 = Sha256Hash.hash(CommonParameter
        .getInstance().isECKeyCryptoEngine(), hash0);
    byte[] inputCheck = new byte[input.length + 4];
    System.arraycopy(input, 0, inputCheck, 0, input.length);
    System.arraycopy(hash1, 0, inputCheck, input.length, 4);
    return Base58.encode(inputCheck);
  }

  private static byte[] decode58Check(String input) {
    byte[] decodeCheck = Base58.decode(input);
    if (decodeCheck.length <= 4) {
      return null;
    }
    byte[] decodeData = new byte[decodeCheck.length - 4];
    System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
    byte[] hash0 = Sha256Hash.hash(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), decodeData);
    byte[] hash1 = Sha256Hash.hash(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), hash0);
    if (hash1[0] == decodeCheck[decodeData.length]
        && hash1[1] == decodeCheck[decodeData.length + 1]
        && hash1[2] == decodeCheck[decodeData.length + 2]
        && hash1[3] == decodeCheck[decodeData.length + 3]) {
      return decodeData;
    }
    return null;
  }

  /**
   * constructor.
   */

  public static byte[] decodeFromBase58Check(String addressBase58) {
    if (StringUtils.isEmpty(addressBase58)) {
      logger.warn("Warning: Address is empty !!");
      return null;
    }
    byte[] address = decode58Check(addressBase58);
    if (!addressValid(address)) {
      return null;
    }
    return address;
  }

  /**
   * constructor.
   */

  public static boolean priKeyValid(String priKey) {
    if (StringUtils.isEmpty(priKey)) {
      logger.warn("Warning: PrivateKey is empty !!");
      return false;
    }
    if (priKey.length() != 64) {
      logger.warn("Warning: PrivateKey length need 64 but " + priKey.length() + " !!");
      return false;
    }
    //Other rule;
    return true;
  }

  /**
   * constructor.
   */
  public static Optional<WitnessList> listWitnesses() {
    Optional<WitnessList> result = rpcCli.listWitnesses();
    if (result.isPresent()) {
      WitnessList witnessList = result.get();
      List<Witness> list = witnessList.getWitnessesList();
      List<Witness> newList = new ArrayList();
      newList.addAll(list);
      newList.sort(new WitnessComparator());
      WitnessList.Builder builder = WitnessList.newBuilder();
      newList.forEach(witness -> builder.addWitnesses(witness));
      result = Optional.of(builder.build());
    }
    return result;
  }

  public static Optional<AssetIssueList> getAssetIssueList() {
    return rpcCli.getAssetIssueList();
  }

  public static Optional<NodeList> listNodes() {
    return rpcCli.listNodes();
  }

  public static Optional<TransactionList> getTransactionsFromThis(byte[] address) {
    return rpcCli.getTransactionsFromThis(address);
  }

  public static Optional<TransactionList> getTransactionsToThis(byte[] address) {
    return rpcCli.getTransactionsToThis(address);
  }

  public static Block getBlock(long blockNum) {
    return rpcCli.getBlock(blockNum);
  }

  public static Optional<Block> getBlockById(String blockId) {
    return rpcCli.getBlockById(blockId);
  }

  public static Optional<BlockList> getBlockByLimitNext(long start, long end) {
    return rpcCli.getBlockByLimitNext(start, end);
  }

  public static Optional<BlockList> getBlockByLatestNum(long num) {
    return rpcCli.getBlockByLatestNum(num);
  }

  public boolean login(String password) {
    loginState = checkPassWord(password);
    return loginState;
  }

  public boolean isLoginState() {
    return loginState;
  }

  public void logout() {
    loginState = false;
  }

  public ECKey getEcKey() {
    return ecKey;
  }

  public byte[] getAddress() {
    return ecKey.getAddress();
  }

  private Transaction signTransaction(Transaction transaction) {
    if (this.ecKey == null || this.ecKey.getPrivKey() == null) {
      logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, this.ecKey);
  }

  /*    public static Optional<AssetIssueList> getAssetIssueListByTimestamp(long timestamp) {
        return rpcCli.getAssetIssueListByTimestamp(timestamp);
  }*/

  /*    public static Optional<TransactionList> getTransactionsByTimestamp(
  long start, long end, int offset, int limit) {
        return rpcCli.getTransactionsByTimestamp(start, end, offset, limit);
  }*/

  /**
   * constructor.
   */

  public boolean sendCoin(byte[] to, long amount) {
    byte[] owner = getAddress();
    TransferContract contract = createTransferContract(to, owner, amount);
    Transaction transaction = rpcCli.createTransaction(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(transaction);
    return rpcCli.broadcastTransaction(transaction);
  }

  /**
   * constructor.
   */

  public boolean updateAccount(byte[] addressBytes, byte[] accountNameBytes) {
    AccountUpdateContract contract = createAccountUpdateContract(accountNameBytes,
        addressBytes);
    Transaction transaction = rpcCli.createTransaction(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    transaction = signTransaction(transaction);
    return rpcCli.broadcastTransaction(transaction);
  }

  /*    public static Optional<AssetIssueList> getAssetIssueByAccount(byte[] address) {
        return rpcCli.getAssetIssueByAccount(address);
  }

    public static AssetIssueContract getAssetIssueByName(String assetName) {
        return rpcCli.getAssetIssueByName(assetName);
  }

    public static GrpcAPI.NumberMessage getTotalTransaction() {
        return rpcCli.getTotalTransaction();
  }*/

  /**
   * constructor.
   */

  public boolean transferAsset(byte[] to, byte[] assertName, long amount) {
    byte[] owner = getAddress();
    Transaction transaction = createTransferAssetTransaction(to, assertName, owner, amount);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(transaction);
    return rpcCli.broadcastTransaction(transaction);
  }

  /**
   * constructor.
   */

  public boolean participateAssetIssue(byte[] to, byte[] assertName, long amount) {
    byte[] owner = getAddress();
    Transaction transaction = participateAssetIssueTransaction(to, assertName, owner, amount);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(transaction);
    return rpcCli.broadcastTransaction(transaction);
  }

  /*    public static Optional<Transaction> getTransactionById(String txID) {
        return rpcCli.getTransactionById(txID);
  }*/

  /**
   * constructor.
   */

  public boolean createAssetIssue(AssetIssueContract contract) {
    Transaction transaction = rpcCli.createAssetIssue(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(transaction);
    return rpcCli.broadcastTransaction(transaction);
  }

  /**
   * constructor.
   */

  public boolean createWitness(byte[] url) {
    byte[] owner = getAddress();
    Transaction transaction = createWitnessTransaction(owner, url);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(transaction);
    return rpcCli.broadcastTransaction(transaction);
  }

  /**
   * constructor.
   */

  public boolean voteWitness(HashMap<String, String> witness) {
    byte[] owner = getAddress();
    WitnessContract.VoteWitnessContract contract = createVoteWitnessContract(owner, witness);
    Transaction transaction = rpcCli.voteWitnessAccount(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(transaction);
    return rpcCli.broadcastTransaction(transaction);
  }

  /**
   * constructor.
   */

  public boolean createAccount(byte[] address)
      throws CipherException, IOException, CancelException {
    byte[] owner = getAddress();
    Transaction transaction = createAccountTransaction(owner, address);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    transaction = signTransaction(transaction);
    return rpcCli.broadcastTransaction(transaction);
  }

  /**
   * constructor.
   */

  public boolean freezeBalance(long frozenBalance, long frozenDuration) {

    FreezeBalanceContract contract = createFreezeBalanceContract(frozenBalance,
        frozenDuration);

    Transaction transaction = rpcCli.createTransaction(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    transaction = signTransaction(transaction);
    return rpcCli.broadcastTransaction(transaction);
  }

  private FreezeBalanceContract createFreezeBalanceContract(long frozenBalance,
      long frozenDuration) {
    byte[] address = getAddress();
    FreezeBalanceContract.Builder builder = FreezeBalanceContract.newBuilder();
    ByteString byteAddress = ByteString.copyFrom(address);

    builder.setOwnerAddress(byteAddress).setFrozenBalance(frozenBalance)
        .setFrozenDuration(frozenDuration);

    return builder.build();
  }

  /**
   * constructor.
   */

  public boolean unfreezeBalance() {
    UnfreezeBalanceContract contract = createUnfreezeBalanceContract();

    Transaction transaction = rpcCli.createTransaction(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    transaction = signTransaction(transaction);
    return rpcCli.broadcastTransaction(transaction);
  }

  private UnfreezeBalanceContract createUnfreezeBalanceContract() {

    byte[] address = getAddress();
    UnfreezeBalanceContract.Builder builder = UnfreezeBalanceContract
        .newBuilder();
    ByteString byteAddress = ByteString.copyFrom(address);
    builder.setOwnerAddress(byteAddress);

    return builder.build();
  }

  /**
   * constructor.
   */

  public boolean withdrawBalance() {
    WithdrawBalanceContract contract = createWithdrawBalanceContract();

    Transaction transaction = rpcCli.createTransaction(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    transaction = signTransaction(transaction);
    return rpcCli.broadcastTransaction(transaction);
  }

  private WithdrawBalanceContract createWithdrawBalanceContract() {

    byte[] address = getAddress();
    WithdrawBalanceContract.Builder builder = WithdrawBalanceContract
        .newBuilder();
    ByteString byteAddress = ByteString.copyFrom(address);

    builder.setOwnerAddress(byteAddress);

    return builder.build();
  }
}
