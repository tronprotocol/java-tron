package org.tron.core.services.jsonrpc;

import static org.tron.core.Wallet.CONTRACT_VALIDATE_ERROR;
import static org.tron.core.Wallet.CONTRACT_VALIDATE_EXCEPTION;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.convertToTronAddress;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.generateContractAddress;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getBlockID;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getMethodSign;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getOwner;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getTo;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getTransactionAmount;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getTxID;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.int2HexString;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.long2HexString;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.triggerCallContract;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.tronToEthAddress;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.GrpcAPI.TransactionInfoList;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Commons;
import org.tron.common.utils.StringUtil;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.exception.VMIllegalException;
import org.tron.core.services.NodeInfoService;
import org.tron.program.Version;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;

@Slf4j(topic = "API")
public class TestServiceImpl implements TestService {

  private NodeInfoService nodeInfoService;
  private Wallet wallet;

  public TestServiceImpl() {
  }

  public TestServiceImpl(NodeInfoService nodeInfoService, Wallet wallet) {
    this.nodeInfoService = nodeInfoService;
    this.wallet = wallet;
  }

  @Override
  public int getInt(int code) {
    return code;
  }

  public String web3ClientVersion() {
    Pattern shortVersion = Pattern.compile("(\\d\\.\\d).*");
    Matcher matcher = shortVersion.matcher(System.getProperty("java.version"));
    matcher.matches();

    return Arrays.asList(
        "TRON", "v" + Version.getVersion(),
        System.getProperty("os.name"),
        "Java" + matcher.group(1),
        Version.VERSION_NAME).stream()
        .collect(Collectors.joining("/"));
  }

  public String web3Sha3(String data) {
    byte[] result = Hash.sha3(ByteArray.fromHexString(data));
    return ByteArray.toJsonHex(result);
  }

  public String ethGetBlockTransactionCountByHash(String blockHash) throws Exception {
    Block b = getBlockByJSonHash(blockHash);
    if (b == null) return null;

    long n = b.getTransactionsList().size();
    return ByteArray.toJsonHex(n);
  }

  public String ethGetBlockTransactionCountByNumber(String bnOrId) throws Exception {
    List<Transaction> list = wallet.getTransactionsByJsonBlockId(bnOrId);
    if (list == null) {
      return null;
    }

    long n = list.size();
    return ByteArray.toJsonHex(n);
  }

  public BlockResult ethGetBlockByHash(String blockHash, Boolean fullTransactionObjects) throws Exception {
    final Block b = getBlockByJSonHash(blockHash);
    return getBlockResult(b, fullTransactionObjects);
  }

  private Block getBlockByJSonHash(String blockHash) throws Exception {
    return wallet.getBlockById(ByteString.copyFrom(ByteArray.fromHexString(blockHash)));
  }

  protected BlockResult getBlockResult(Block block, boolean fullTx) {
    if (block == null)
      return null;

    BlockCapsule blockCapsule = new BlockCapsule(block);
    boolean isPending = false;
    BlockResult br = new BlockResult();
    br.number = ByteArray.toJsonHex(blockCapsule.getNum());
    br.hash = ByteArray.toJsonHex(blockCapsule.getBlockId().getBytes());
    br.parentHash = ByteArray.toJsonHex(blockCapsule.getParentBlockId().getBytes());
    br.nonce = ""; // no value
    br.sha3Uncles= ""; // no value
    br.logsBloom = ""; // no value
    br.transactionsRoot = ByteArray.toJsonHex(block.getBlockHeader().getRawData().getTxTrieRoot().toByteArray());
    br.stateRoot = ByteArray.toJsonHex(block.getBlockHeader().getRawData().getAccountStateRoot().toByteArray());
    br.receiptsRoot = ""; // no value
    br.miner = ByteArray.toJsonHex(blockCapsule.getWitnessAddress().toByteArray());
    br.difficulty = ""; // no value
    br.totalDifficulty = ""; // no value
    // br.extraData // no value
    br.size = ByteArray.toJsonHex(block.getSerializedSize());
    br.gasLimit = "";
    br.gasUsed = "";
    br.timestamp = ByteArray.toJsonHex(blockCapsule.getTimeStamp());

    List<Object> txes = new ArrayList<>();
    if (fullTx) {
      for (int i = 0; i < block.getTransactionsList().size(); i++) {
        txes.add(new TransactionResultDTO(block, i, block.getTransactionsList().get(i), wallet));
      }
    } else {
      for (Transaction tx : block.getTransactionsList()) {
        txes.add(ByteArray.toJsonHex(new TransactionCapsule(tx).getTransactionId().getBytes()));
      }
    }
    br.transactions = txes.toArray();

    List<String> ul = new ArrayList<>();
    br.uncles = ul.toArray(new String[ul.size()]);

    return br;
  }

  @Override
  public int getNetVersion() {
    //当前链的id，不能跟metamask已有的id冲突
    return 100;
  }

  @Override
  public boolean isListening() {
    int activeConnectCount = nodeInfoService.getNodeInfo().getActiveConnectCount();
    return activeConnectCount >= 1;
  }

  @Override
  public int getProtocolVersion() {
    //当前块的版本号。实际是与代码版本对应的。
    return wallet.getNowBlock().getBlockHeader().getRawData().getVersion();
  }

  @Override
  public int getLatestBlockNum() {
    //当前节点同步的最新块号
    return (int) wallet.getNowBlock().getBlockHeader().getRawData().getNumber();
  }

  @Override
  public long getTrxBalance(String address, String blockNumOrTag) throws ItemNotFoundException {
    //某个用户的trx余额，以sun为单位
    byte[] addressData = Commons.decodeFromBase58Check(address);
    Account account = Account.newBuilder().setAddress(ByteString.copyFrom(addressData)).build();
    return wallet.getAccount(account).getBalance();
  }

  @Override
  public BigInteger getTrc20Balance(String ownerAddress, String contractAddress,
      String blockNumOrTag) {
    //某个用户拥有的某个token20余额，带精度
    byte[] addressData = Commons.decodeFromBase58Check(ownerAddress);
    byte[] addressDataWord = new byte[32];
    System.arraycopy(addressData, 0, addressDataWord, 32 - addressData.length, addressData.length);
    String dataStr = getMethodSign("balanceOf(address)") + Hex.toHexString(addressDataWord);

    //构造静态合约时，只需要3个字段
    TriggerSmartContract triggerContract = triggerCallContract(addressData,
        Commons.decodeFromBase58Check(contractAddress), 0,
        ByteArray.fromHexString(dataStr), 0, null);

    TransactionExtention.Builder trxExtBuilder = TransactionExtention.newBuilder();
    Return.Builder retBuilder = Return.newBuilder();
    TransactionExtention trxExt;

    try {
      TransactionCapsule trxCap = wallet.createTransactionCapsule(triggerContract,
          ContractType.TriggerSmartContract);
      Transaction trx = wallet.triggerConstantContract(
          triggerContract,
          trxCap,
          trxExtBuilder,
          retBuilder);

      retBuilder.setResult(true).setCode(response_code.SUCCESS);
      trxExtBuilder.setTransaction(trx);
      trxExtBuilder.setTxid(trxCap.getTransactionId().getByteString());
      trxExtBuilder.setResult(retBuilder);
    } catch (ContractValidateException | VMIllegalException e) {
      retBuilder.setResult(false).setCode(response_code.CONTRACT_VALIDATE_ERROR)
          .setMessage(ByteString.copyFromUtf8(CONTRACT_VALIDATE_ERROR + e.getMessage()));
      trxExtBuilder.setResult(retBuilder);
      logger.warn(CONTRACT_VALIDATE_EXCEPTION, e.getMessage());
    } catch (RuntimeException e) {
      retBuilder.setResult(false).setCode(response_code.CONTRACT_EXE_ERROR)
          .setMessage(ByteString.copyFromUtf8(e.getClass() + " : " + e.getMessage()));
      trxExtBuilder.setResult(retBuilder);
      logger.warn("When run constant call in VM, have RuntimeException: " + e.getMessage());
    } catch (Exception e) {
      retBuilder.setResult(false).setCode(response_code.OTHER_ERROR)
          .setMessage(ByteString.copyFromUtf8(e.getClass() + " : " + e.getMessage()));
      trxExtBuilder.setResult(retBuilder);
      logger.warn("Unknown exception caught: " + e.getMessage(), e);
    } finally {
      trxExt = trxExtBuilder.build();
    }

    String result = null;
    String code = trxExt.getResult().getCode().toString();
    if ("SUCCESS".equals(code)) {
      List<ByteString> list = trxExt.getConstantResultList();
      byte[] listBytes = new byte[0];
      for (ByteString bs : list) {
        listBytes = ByteUtil.merge(listBytes, bs.toByteArray());
      }
      result = Hex.toHexString(listBytes);
    } else {
      logger.error("trigger contract to get scaling factor error.");
    }

    if (Objects.isNull(result)) {
      return BigInteger.valueOf(0);
    }
    if (result.length() > 64) {
      result = result.substring(0, 64);
    }
    return new BigInteger(1, ByteArray.fromHexString(result));
  }

  @Override
  public long getSendTransactionCountOfAddress(String address, String blockNumOrTag) {
    //发起人为某个地址的交易总数。FullNode无法实现该功能
    return wallet.getNowBlock().getBlockHeader().getRawData().getTimestamp() + 86400 * 1000;
  }

  @Override
  public String getABIofSmartContract(String contractAddress) {
    //获取某个合约地址的字节码
    byte[] addressData = Commons.decodeFromBase58Check(contractAddress);
    BytesMessage.Builder build = BytesMessage.newBuilder();
    BytesMessage bytesMessage = build.setValue(ByteString.copyFrom(addressData)).build();
    SmartContract smartContract = wallet.getContract(bytesMessage);
    return ByteArray.toHexString(smartContract.getBytecode().toByteArray());
  }

  @Override
  public Object isSyncing() {
    return true;
  }

  @Override
  public String getCoinbase() {
    byte[] witnessAddress = wallet.getNowBlock().getBlockHeader().getRawData().getWitnessAddress()
        .toByteArray();
    return StringUtil.encode58Check(witnessAddress);
  }

  @Override
  public String gasPrice() {
    BigInteger gasPrice;
    BigInteger multiplier = new BigInteger("1000000000", 10); // Gwei: 10^9

    if ("getTransactionFee".equals(wallet.getChainParameters().getChainParameter(3).getKey())) {
      gasPrice = BigInteger.valueOf(wallet.getChainParameters().getChainParameter(3).getValue());
    } else {
      gasPrice = BigInteger.valueOf(140);
    }
    return "0x" + gasPrice.multiply(multiplier).toString(16);
  }

  @Override
  public String estimateGas() {
    BigInteger feeLimit = BigInteger.valueOf(100);  // set fee limit: 100 trx
    BigInteger precision = new BigInteger("1000000000000000000"); // 1ether = 10^18 wei
    BigInteger gasPrice = new BigInteger(gasPrice().substring(2), 16);
    if (gasPrice.compareTo(BigInteger.ZERO) > 0) {
      return "0x" + feeLimit.multiply(precision).divide(gasPrice).toString(16);
    } else {
      return "0x0";
    }
  }

  @Override
  public String[] getCompilers() {
    return new String[] {"solidity"};
  }

  @Override
  public String compileSolidity(String source) {
    //耗费cpu太高，采用trongrid中心化服务器实现。
    return null;
  }

  @Override
  public JSONObject getTransactionByHash(String txid) {
    Transaction transaction = wallet
        .getTransactionById(ByteString.copyFrom(ByteArray.fromHexString(txid)));
    TransactionInfo transactionInfo = wallet
        .getTransactionInfoById(ByteString.copyFrom(ByteArray.fromHexString(txid)));

    long blockNum = transactionInfo.getBlockNumber();
    Block block = wallet.getBlockByNum(blockNum);

    return formatRpcTransaction(transaction, transactionInfo, block);
  }

  private JSONObject formatRpcTransaction(Transaction transaction, TransactionInfo transactionInfo,
      Block block) {
    String txid = ByteArray.toHexString(transactionInfo.getId().toByteArray());
    long blockNum = block.getBlockHeader().getRawData().getNumber();
    JSONObject jsonObject = new JSONObject(true);
    jsonObject.put("blockHash", getBlockID(block));
    jsonObject.put("blockNumber", long2HexString(blockNum));

    jsonObject.put("gas", null); //暂时不填
    jsonObject.put("gasPrice", null); //暂时不填
    jsonObject.put("hash", "0x" + txid);
    jsonObject.put("input", null); //暂时不填data字段
    jsonObject.put("nonce", null); //暂时不写
    byte[] owner = getOwner(transaction.getRawData().getContract(0));
    ArrayList<ByteString> toAddressList = getTo(transaction);
    jsonObject.put("from", owner != null ? StringUtil.encode58Check(owner) : null);
    jsonObject.put("to", toAddressList.size() > 0 ?
        StringUtil.encode58Check(toAddressList.get(0).toByteArray()) : null);

    int transactionIndex = -1;
    for (int index = 0; index < block.getTransactionsCount(); index++) {
      if (getTxID(block.getTransactions(index)).equals(txid)) {
        transactionIndex = index;
        break;
      }
    }
    jsonObject.put("transactionIndex", int2HexString(transactionIndex));
    long amount = getTransactionAmount(transaction.getRawData().getContract(0), txid,
        blockNum, transactionInfo, wallet);
    jsonObject.put("value", long2HexString(amount));

    ByteString signature = transaction.getSignature(0); // r[32] + s[32] + 符号位v[1]
    byte[] signData = signature.toByteArray();
    byte v = (byte) (signData[64] + 27); //参考函数 Base64toBytes
    byte[] r = Arrays.copyOfRange(signData, 0, 32);
    byte[] s = Arrays.copyOfRange(signData, 32, 64);
    jsonObject.put("v", int2HexString(v));
    jsonObject.put("r", "0x" + ByteArray.toHexString(r));
    jsonObject.put("s", "0x" + ByteArray.toHexString(s));

    return jsonObject;
  }

  @Override
  public JSONObject getTransactionByBlockHashAndIndex(String blockHash, int index) {
    Block block = wallet.getBlockById(ByteString.copyFrom(ByteArray.fromHexString(blockHash)));
    if (block == null) {
      return null;
    }
    if (index > block.getTransactionsCount() - 1) {
      return null;
    }
    Transaction transaction = block.getTransactions(index);
    String txid = getTxID(transaction);
    TransactionInfo transactionInfo = wallet.getTransactionInfoById(
        ByteString.copyFrom(ByteArray.fromHexString(txid)));

    return formatRpcTransaction(transaction, transactionInfo, block);
  }

  @Override
  public JSONObject getTransactionByBlockNumberAndIndex(int blockNum, int index) {
    Block block = wallet.getBlockByNum(blockNum);
    if (block == null) {
      return null;
    }
    if (index > block.getTransactionsCount() - 1) {
      return null;
    }
    Transaction transaction = block.getTransactions(index);
    String txid = getTxID(transaction);
    TransactionInfo transactionInfo = wallet.getTransactionInfoById(
        ByteString.copyFrom(ByteArray.fromHexString(txid)));

    return formatRpcTransaction(transaction, transactionInfo, block);
  }

  @Override
  public JSONObject getTransactionReceipt(String txid) {

    Transaction transaction = wallet
        .getTransactionById(ByteString.copyFrom(ByteArray.fromHexString(txid)));
    TransactionInfo transactionInfo = wallet
        .getTransactionInfoById(ByteString.copyFrom(ByteArray.fromHexString(txid)));
    if (transaction == null || transactionInfo == null) {
      return null;
    }

    long blockNum = transactionInfo.getBlockNumber();
    Block block = wallet.getBlockByNum(blockNum);
    JSONObject jsonObject = formatRpcTransaction(transaction, transactionInfo, block);
    String transactionHash = (String) jsonObject.remove("hash");
    jsonObject.put("transactionHash", transactionHash);
    jsonObject.remove("gas");
    jsonObject.remove("gasPrice");
    jsonObject.remove("input");
    jsonObject.remove("nonce");
    jsonObject.remove("value");
    jsonObject.remove("v");
    jsonObject.remove("r");
    jsonObject.remove("s");

    long cumulativeGasUsed = 0;
    TransactionInfoList reply = wallet.getTransactionInfoByBlockNum(blockNum);
    for (TransactionInfo info : reply.getTransactionInfoList()) {
      cumulativeGasUsed += info.getFee();
    }
    jsonObject.put("cumulativeGasUsed", long2HexString(cumulativeGasUsed));
    jsonObject.put("gasUsed", long2HexString(transactionInfo.getFee()));

    String contractAddress = null;
    if (transaction.getRawData().getContract(0).getType() == ContractType.CreateSmartContract) {
      contractAddress = StringUtil.encode58Check(generateContractAddress(transaction));
    }
    jsonObject.put("contractAddress", contractAddress);

    //统一的log
    JSONArray logArray = new JSONArray();
    for (int index = 0; index < transactionInfo.getLogCount(); index++) {
      TransactionInfo.Log log = transactionInfo.getLogList().get(index);
      JSONObject logItem = new JSONObject(true);
      logItem.put("logIndex", int2HexString(index + 1)); //log的索引从1开始
      logItem.put("transactionHash", jsonObject.getString("transactionHash"));
      logItem.put("transactionIndex", jsonObject.getString("transactionIndex"));
      logItem.put("blockHash", jsonObject.getString("blockHash"));
      logItem.put("blockNumber", jsonObject.getString("blockNumber"));
      logItem.put("address",
          StringUtil.encode58Check(convertToTronAddress(log.getAddress().toByteArray())));
      logItem.put("data", "0x" + ByteArray.toHexString(log.getData().toByteArray()));
      String[] topics = new String[log.getTopicsCount()];
      for (int i = 0; i < log.getTopicsCount(); i++) {
        topics[i] = "0x" + ByteArray.toHexString(log.getTopics(i).toByteArray());
      }
      logItem.put("topics", topics);
      logArray.add(logItem);
    }
    jsonObject.put("logs", logArray);
    jsonObject.put("logsBloom", ""); //暂时不填

    return jsonObject;
  }
}
