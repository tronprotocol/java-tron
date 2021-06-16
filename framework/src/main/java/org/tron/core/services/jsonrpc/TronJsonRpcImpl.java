package org.tron.core.services.jsonrpc;

import static org.tron.core.Wallet.CONTRACT_VALIDATE_ERROR;
import static org.tron.core.Wallet.CONTRACT_VALIDATE_EXCEPTION;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.balanceOfTopic;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.convertToTronAddress;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.encode58Check;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.generateContractAddress;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getMethodSign;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getTxID;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.triggerCallContract;

import com.google.protobuf.ByteString;
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
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;

@Slf4j(topic = "API")
public class TronJsonRpcImpl implements TronJsonRpc {
  String regexHash = "(0x)?[a-zA-Z0-9]{64}$";

  private NodeInfoService nodeInfoService;
  private Wallet wallet;

  public TronJsonRpcImpl() {
  }

  public TronJsonRpcImpl(NodeInfoService nodeInfoService, Wallet wallet) {
    this.nodeInfoService = nodeInfoService;
    this.wallet = wallet;
  }

  @Override
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

  @Override
  public String web3Sha3(String data) {
    byte[] result = Hash.sha3(ByteArray.fromHexString(data));
    return ByteArray.toJsonHex(result);
  }

  @Override
  public String ethGetBlockTransactionCountByHash(String blockHash) throws Exception {
    Block b = getBlockByJSonHash(blockHash);
    if (b == null) {
      return null;
    }

    long n = b.getTransactionsList().size();
    return ByteArray.toJsonHex(n);
  }

  @Override
  public String ethGetBlockTransactionCountByNumber(String bnOrId) throws Exception {
    List<Transaction> list = wallet.getTransactionsByJsonBlockId(bnOrId);
    if (list == null) {
      return null;
    }

    long n = list.size();
    return ByteArray.toJsonHex(n);
  }

  @Override
  public BlockResult ethGetBlockByHash(String blockHash, Boolean fullTransactionObjects)
      throws Exception {
    final Block b = getBlockByJSonHash(blockHash);
    return getBlockResult(b, fullTransactionObjects);
  }

  @Override
  public BlockResult ethGetBlockByNumber(String bnOrId, Boolean fullTransactionObjects) {
    final Block b;
    if ("pending".equalsIgnoreCase(bnOrId)) {
      b = null;
    } else {
      b = wallet.getByJsonBlockId(bnOrId);
    }
    return (b == null ? null : getBlockResult(b, fullTransactionObjects));
  }

  private byte[] hashToByteArray(String hash) {
    if(!Pattern.matches(regexHash, hash)) {
      throw new JsonRpcApiException("invalid hash value");
    }

    byte[] bHash;
    try {
      bHash = ByteArray.fromHexString(hash);
    } catch (Exception e) {
      throw new JsonRpcApiException(e.getMessage());
    }
    return bHash;
  }

  private Block getBlockByJSonHash(String blockHash) throws Exception {
    byte[] bHash = hashToByteArray(blockHash);
    return wallet.getBlockById(ByteString.copyFrom(bHash));
  }

  protected BlockResult getBlockResult(Block block, boolean fullTx) {
    if (block == null) {
      return null;
    }

    BlockCapsule blockCapsule = new BlockCapsule(block);
    boolean isPending = false;
    BlockResult br = new BlockResult();
    br.number = ByteArray.toJsonHex(blockCapsule.getNum());
    br.hash = ByteArray.toJsonHex(blockCapsule.getBlockId().getBytes());
    br.parentHash = ByteArray.toJsonHex(blockCapsule.getParentBlockId().getBytes());
    br.nonce = null; // no value
    br.sha3Uncles = null; // no value
    br.logsBloom = null; // no value
    br.transactionsRoot = ByteArray
        .toJsonHex(block.getBlockHeader().getRawData().getTxTrieRoot().toByteArray());
    br.stateRoot = ByteArray
        .toJsonHex(block.getBlockHeader().getRawData().getAccountStateRoot().toByteArray());
    br.receiptsRoot = null; // no value
    br.miner = ByteArray.toJsonHex(blockCapsule.getWitnessAddress().toByteArray());
    br.difficulty = null; // no value
    br.totalDifficulty = null; // no value
    // br.extraData // no value
    br.size = ByteArray.toJsonHex(block.getSerializedSize());
    br.gasLimit = null;
    br.gasUsed = null;
    br.timestamp = ByteArray.toJsonHex(blockCapsule.getTimeStamp());

    List<Object> txes = new ArrayList<>();
    if (fullTx) {
      for (int i = 0; i < block.getTransactionsList().size(); i++) {
        txes.add(new TransactionResult(block, i, block.getTransactionsList().get(i), wallet));
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

  public String ethCall(CallArguments args, String bnOrId) throws Exception {
    //静态调用合约方法。
    byte[] addressData = Commons.decodeFromBase58Check(args.from);
    byte[] contractAddressData = Commons.decodeFromBase58Check(args.to);

    return call(addressData, contractAddressData, ByteArray.fromHexString(args.data));
  }

  @Override
  public String getNetVersion() {
    //当前链的id，不能跟metamask已有的id冲突
    return ByteArray.toJsonHex(100);
  }

  @Override
  public boolean isListening() {
    int activeConnectCount = nodeInfoService.getNodeInfo().getActiveConnectCount();
    return activeConnectCount >= 1;
  }

  @Override
  public String getProtocolVersion() {
    //当前块的版本号。实际是与代码版本对应的。
    return ByteArray.toJsonHex(wallet.getNowBlock().getBlockHeader().getRawData().getVersion());
  }

  @Override
  public String getLatestBlockNum() {
    //当前节点同步的最新块号
    return ByteArray.toJsonHex(wallet.getNowBlock().getBlockHeader().getRawData().getNumber());
  }

  @Override
  public String getTrxBalance(String address, String blockNumOrTag) throws ItemNotFoundException {
    //某个用户的trx余额，以sun为单位
    byte[] addressData = ByteArray.fromHexString(address);
    Account account = Account.newBuilder().setAddress(ByteString.copyFrom(addressData)).build();
    long balance = wallet.getAccount(account).getBalance();
    return ByteArray.toJsonHex(balance);
  }


  /**
   * @param data Hash of the method signature and encoded parameters.
   * for example: getMethodSign(methodName(uint256,uint256)) || data1 || data2
   */
  private String call(byte[] ownerAddressByte, byte[] contractAddressByte, byte[] data) {

    //构造静态合约时，只需要3个字段
    TriggerSmartContract triggerContract = triggerCallContract(
        ownerAddressByte,
        contractAddressByte,
        0, //给合约发送的trx，静态合约不需要
        data,
        0, //给合约发送的 token10 的金额，静态合约不需要
        null //给合约发送的 token10 ID，静态合约不需要
    );

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
      logger.error("trigger contract failed.");
    }
    return result;
  }

  @Override
  public String getTrc20Balance(String ownerAddress, String contractAddress,
      String blockNumOrTag) {
    //某个用户拥有的某个token20余额，带精度
    byte[] addressData = ByteArray.fromHexString(ownerAddress);
    //byte[] addressData = Commons.decodeFromBase58Check(ownerAddress);

    //生成dataStr
    byte[] addressDataWord = new byte[32];
    System.arraycopy(addressData, 0, addressDataWord, 32 - addressData.length, addressData.length);
    String dataStr = balanceOfTopic + Hex.toHexString(addressDataWord);

    String result = call(addressData, ByteArray.fromHexString(contractAddress),
        ByteArray.fromHexString(dataStr));

    if (Objects.isNull(result)) {
      return "0x0";
    }
    if (result.length() > 64) {
      result = result.substring(0, 64);
    }

    return ByteArray.toJsonHex(ByteUtil.stripLeadingZeroes(ByteArray.fromHexString(result)));
  }

  @Override
  public String getSendTransactionCountOfAddress(String address, String blockNumOrTag) {
    //发起人为某个地址的交易总数。FullNode无法实现该功能
    return ByteArray.toJsonHex(
        wallet.getNowBlock().getBlockHeader().getRawData().getTimestamp() + 86400 * 1000);
  }

  @Override
  public String getABIofSmartContract(String contractAddress) {
    //获取某个合约地址的字节码
    byte[] addressData = ByteArray.fromHexString(contractAddress);
    BytesMessage.Builder build = BytesMessage.newBuilder();
    BytesMessage bytesMessage = build.setValue(ByteString.copyFrom(addressData)).build();
    SmartContract smartContract = wallet.getContract(bytesMessage);
    return ByteArray.toJsonHex(smartContract.getBytecode().toByteArray());
  }

  @Override
  public Object isSyncing() {
    return true;
  }

  @Override
  public String getCoinbase() {
    //获取最新块的产块sr地址
    byte[] witnessAddress = wallet.getNowBlock().getBlockHeader().getRawData().getWitnessAddress()
        .toByteArray();
    return ByteArray.toJsonHex(witnessAddress);
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
  public TransactionResult getTransactionByHash(String txid) {
    Transaction transaction = wallet
        .getTransactionById(ByteString.copyFrom(ByteArray.fromHexString(txid)));
    TransactionInfo transactionInfo = wallet
        .getTransactionInfoById(ByteString.copyFrom(ByteArray.fromHexString(txid)));
    if (transaction == null || transactionInfo == null) {
      return null;
    }
    long blockNum = transactionInfo.getBlockNumber();
    Block block = wallet.getBlockByNum(blockNum);
    if (block == null) {
      return null;
    }
    return formatRpcTransaction(transaction, block);
  }

  private TransactionResult formatRpcTransaction(Transaction transaction, Block block) {
    String txid = ByteArray.toHexString(
        new TransactionCapsule(transaction).getTransactionId().getBytes());
    int transactionIndex = -1;
    for (int index = 0; index < block.getTransactionsCount(); index++) {
      if (getTxID(block.getTransactions(index)).equals(txid)) {
        transactionIndex = index;
        break;
      }
    }
    return new TransactionResult(block, transactionIndex, transaction, wallet);
  }

  @Override
  public TransactionResult getTransactionByBlockHashAndIndex(String blockHash, int index) {
    Block block = wallet.getBlockById(ByteString.copyFrom(ByteArray.fromHexString(blockHash)));
    if (block == null) {
      return null;
    }
    if (index > block.getTransactionsCount() - 1) {
      return null;
    }
    Transaction transaction = block.getTransactions(index);
    return formatRpcTransaction(transaction, block);
  }

  @Override
  public TransactionResult getTransactionByBlockNumberAndIndex(int blockNum, int index) {
    Block block = wallet.getBlockByNum(blockNum);
    if (block == null) {
      return null;
    }
    if (index > block.getTransactionsCount() - 1) {
      return null;
    }
    Transaction transaction = block.getTransactions(index);
    return formatRpcTransaction(transaction, block);
  }

  @Override
  public TransactionReceipt getTransactionReceipt(String txid) {

    Transaction transaction = wallet
        .getTransactionById(ByteString.copyFrom(ByteArray.fromHexString(txid)));
    TransactionInfo transactionInfo = wallet
        .getTransactionInfoById(ByteString.copyFrom(ByteArray.fromHexString(txid)));
    if (transaction == null || transactionInfo == null) {
      return null;
    }

    long blockNum = transactionInfo.getBlockNumber();
    Block block = wallet.getBlockByNum(blockNum);
    TransactionResult dto = formatRpcTransaction(transaction, block);
    TransactionReceipt receipt = new TransactionReceipt();
    receipt.blockHash = dto.blockHash;
    receipt.blockNumber = dto.blockNumber;
    receipt.transactionIndex = dto.transactionIndex;
    receipt.transactionHash = dto.hash;
    receipt.from = dto.from;
    receipt.fromBase58 = dto.fromBase58;
    receipt.to = dto.to;
    receipt.toBase58 = dto.toBase58;

    long cumulativeGasUsed = 0;
    TransactionInfoList infoList = wallet.getTransactionInfoByBlockNum(blockNum);
    for (TransactionInfo info : infoList.getTransactionInfoList()) {
      cumulativeGasUsed += info.getFee();
    }
    receipt.cumulativeGasUsed = ByteArray.toJsonHex(cumulativeGasUsed);
    receipt.gasUsed = ByteArray.toJsonHex(transactionInfo.getFee());

    String contractAddress = null;
    if (transaction.getRawData().getContract(0).getType() == ContractType.CreateSmartContract) {
      contractAddress = encode58Check(generateContractAddress(transaction));
    }
    receipt.contractAddress = contractAddress;

    //统一的log
    List<TransactionReceipt.TransactionLog> logList = new ArrayList<>();
    for (int index = 0; index < transactionInfo.getLogCount(); index++) {
      TransactionInfo.Log log = transactionInfo.getLogList().get(index);

      TransactionReceipt.TransactionLog transactionLog = new TransactionReceipt.TransactionLog();
      transactionLog.logIndex = ByteArray.toJsonHex(index + 1); //log的索引从1开始
      transactionLog.transactionHash = dto.hash;
      transactionLog.transactionIndex = dto.transactionIndex;
      transactionLog.blockHash = dto.blockHash;
      transactionLog.blockNumber = dto.blockNumber;
      byte[] addressByte = convertToTronAddress(log.getAddress().toByteArray());
      transactionLog.address = ByteArray.toJsonHex(addressByte);
      transactionLog.addressBase58 = encode58Check(addressByte);
      transactionLog.data = ByteArray.toJsonHex(log.getData().toByteArray());
      String[] topics = new String[log.getTopicsCount()];
      for (int i = 0; i < log.getTopicsCount(); i++) {
        topics[i] = ByteArray.toJsonHex(log.getTopics(i).toByteArray());
      }
      transactionLog.topics = topics;

      logList.add(transactionLog);
    }
    receipt.logs = logList.toArray(new TransactionReceipt.TransactionLog[logList.size()]);
    receipt.logsBloom = null; //暂时不填

    return receipt;
  }

  @Override
  public String getCall(CallArguments transactionCall, String blockNumOrTag) {
    //静态调用合约方法。
    byte[] addressData = ByteArray.fromHexString(transactionCall.from);
    byte[] contractAddressData = ByteArray.fromHexString(transactionCall.to);

    return call(addressData, contractAddressData, ByteArray.fromHexString(transactionCall.data));
  }

  //生成一个调用call api的参数，可以自由修改
  private void generateCallParameter() {
    String ownerAddress = "TXvRyjomvtNWSKvNouTvAedRGD4w9RXLZD";
    String usdjAddress = "TLBaRhANQoJFTqre9Nf1mjuwNWjCJeYqUL"; // nile测试环境udsj地址

    byte[] addressData = Commons.decodeFromBase58Check(ownerAddress);
    byte[] addressDataWord = new byte[32];
    System.arraycopy(Commons.decodeFromBase58Check(ownerAddress), 0, addressDataWord,
        32 - addressData.length, addressData.length);
    String data = getMethodSign("balanceOf(address)") + Hex.toHexString(addressDataWord);

    CallArguments transactionCall = new CallArguments();
    transactionCall.from = ownerAddress;
    transactionCall.to = usdjAddress;
    transactionCall.data = data;

    StringBuffer sb = new StringBuffer("{\"jsonrpc\":\"2.0\",\"method\":\"eth_call\",\"params\":[");
    sb.append(transactionCall);
    sb.append(", \"latest\"],\"id\":1}");
    System.out.println(sb);
  }

  @Override
  public String getPeerCount() {
    //返回当前节点所连接的peer节点数量
    return ByteArray.toJsonHex(nodeInfoService.getNodeInfo().getPeerList().size());
  }

  @Override
  public Object getSyncingStatus() {
    //查询同步状态。未同步返回false，否则返回 SyncingResult
    if (nodeInfoService.getNodeInfo().getPeerList().size() == 0) {
      return false;
    }
    long startingBlock = nodeInfoService.getNodeInfo().getBeginSyncNum();
    Block nowBlock = wallet.getNowBlock();
    long currentBlock = nowBlock.getBlockHeader().getRawData().getNumber();
    long diff = (System.currentTimeMillis() - nowBlock.getBlockHeader().getRawData()
        .getTimestamp()) / 3000;
    diff = diff > 0 ? diff : 0;
    long highestBlock = currentBlock + diff; //预测的最高块号
    SyncingResult syncingResult = new SyncingResult(ByteArray.toJsonHex(startingBlock),
        ByteArray.toJsonHex(currentBlock),
        ByteArray.toJsonHex(highestBlock)
    );
    return syncingResult;
  }

  @Override
  public BlockResult getUncleByBlockHashAndIndex(String blockHash, int index) {
    //查询指定块hash的第几个分叉
    return null;
  }

  @Override
  public BlockResult getUncleByBlockNumberAndIndex(String blockNumOrTag, int index) {
    //查询指定块号的第几个分叉
    return null;
  }

  @Override
  public String getUncleCountByBlockHash(String blockHash) {
    //查询指定块hash的分叉个数
    return "0x0";
  }

  @Override
  public String getUncleCountByBlockNumber(String blockNumOrTag) {
    //查询指定块号的分叉个数
    return "0x0";
  }

  @Override
  public String getHashRate() {
    //读取当前挖矿节点的每秒钟哈希值算出数量，无用
    return "0x0";
  }

  @Override
  public boolean isMining() {
    //检查节点是否在进行挖矿，无用
    return false;
  }

  public static void main(String[] args) {
    TronJsonRpcImpl impl = new TronJsonRpcImpl();
    impl.generateCallParameter();
  }
}
