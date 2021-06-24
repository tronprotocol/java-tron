package org.tron.core.services.jsonrpc;

import static org.tron.core.Wallet.CONTRACT_VALIDATE_ERROR;
import static org.tron.core.Wallet.CONTRACT_VALIDATE_EXCEPTION;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getMethodSign;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getTxID;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.triggerCallContract;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.common.crypto.Hash;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Commons;
import org.tron.common.utils.DecodeUtil;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.VMIllegalException;
import org.tron.core.services.NodeInfoService;
import org.tron.core.store.StorageRowStore;
import org.tron.core.vm.program.Storage;
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
  String regexAddressHash = "(0x)?[a-zA-Z0-9]{42}$";

  private NodeInfoService nodeInfoService;
  private Wallet wallet;
  private Manager manager;

  public TronJsonRpcImpl() {
  }

  public TronJsonRpcImpl(NodeInfoService nodeInfoService, Wallet wallet, Manager manager) {
    this.nodeInfoService = nodeInfoService;
    this.wallet = wallet;
    this.manager = manager;
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
    byte[] input;
    try {
      input = ByteArray.fromHexString(data);
    } catch (Exception e) {
      throw new JsonRpcApiException("invalid input value");
    }

    byte[] result = Hash.sha3(input);
    return ByteArray.toJsonHex(result);
  }

  @Override
  public String ethGetBlockTransactionCountByHash(String blockHash) throws Exception {
    Block b = getBlockByJsonHash(blockHash);
    if (b == null) {
      return null;
    }

    long n = b.getTransactionsList().size();
    return ByteArray.toJsonHex(n);
  }

  @Override
  public String ethGetBlockTransactionCountByNumber(String blockNumOrTag) throws Exception {
    List<Transaction> list = wallet.getTransactionsByJsonBlockId(blockNumOrTag);
    if (list == null) {
      return null;
    }

    long n = list.size();
    return ByteArray.toJsonHex(n);
  }

  @Override
  public BlockResult ethGetBlockByHash(String blockHash, Boolean fullTransactionObjects)
      throws Exception {
    final Block b = getBlockByJsonHash(blockHash);
    return getBlockResult(b, fullTransactionObjects);
  }

  @Override
  public BlockResult ethGetBlockByNumber(String blockNumOrTag, Boolean fullTransactionObjects) {
    final Block b = wallet.getByJsonBlockId(blockNumOrTag);
    return (b == null ? null : getBlockResult(b, fullTransactionObjects));
  }

  private byte[] hashToByteArray(String hash) {
    if (!Pattern.matches(regexHash, hash)) {
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

  private byte[] addressHashToByteArray(String hash) {
   // if (!Pattern.matches(regexAddressHash, hash)) {
   //   throw new JsonRpcApiException("invalid address hash value");
   // }

    byte[] bHash;
    try {
      bHash = ByteArray.fromHexString(hash);
      if (bHash.length != DecodeUtil.ADDRESS_SIZE / 2
          && bHash.length != DecodeUtil.ADDRESS_SIZE / 2 - 1) {
        throw new JsonRpcApiException("invalid address hash value");
      }
      if (bHash.length == DecodeUtil.ADDRESS_SIZE/2 - 1) {
        bHash = ByteUtil.merge(new byte[] {DecodeUtil.addressPreFixByte}, bHash);
      }
    } catch (Exception e) {
      throw new JsonRpcApiException(e.getMessage());
    }
    return bHash;
  }

  private Block getBlockByJsonHash(String blockHash) {
    byte[] bHash = hashToByteArray(blockHash);
    return wallet.getBlockById(ByteString.copyFrom(bHash));
  }

  private BlockResult getBlockResult(Block block, boolean fullTx) {
    if (block == null) {
      return null;
    }

    BlockCapsule blockCapsule = new BlockCapsule(block);
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
    br.extraData = null; // no value
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

  @Override
  public String getNetVersion() {
    //network id，不能跟metamask已有的id冲突
    return ByteArray.toJsonHex(100);
  }

  @Override
  public String ethChainId() {
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
  public String getTrxBalance(String address, String blockNumOrTag) {
    if ("earliest".equalsIgnoreCase(blockNumOrTag)
        || "pending".equalsIgnoreCase(blockNumOrTag)) {
      throw new JsonRpcApiException("TAG [earliest | pending] not supported");
    } else if ("latest".equalsIgnoreCase(blockNumOrTag)) {
      //某个用户的trx余额，以sun为单位
      byte[] addressData = addressHashToByteArray(address);

      Account account = Account.newBuilder().setAddress(ByteString.copyFrom(addressData)).build();
      Account reply = wallet.getAccount(account);
      long balance = 0;

      if (reply != null) {
        balance = reply.getBalance();
      }
      return ByteArray.toJsonHex(balance);
    } else {
      try {
        ByteArray.hexToBigInteger(blockNumOrTag);
      } catch (Exception e) {
        throw new JsonRpcApiException("invalid block number");
      }

      throw new JsonRpcApiException("QUANTITY not supported, just support TAG as latest");
    }
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
      result = ByteArray.toJsonHex(listBytes);
    } else {
      logger.error("trigger contract failed.");
    }
    return result;
  }

  @Override
  public String getStorageAt(String address, String storageIdx, String blockNumOrTag) {
    if ("earliest".equalsIgnoreCase(blockNumOrTag)
        || "pending".equalsIgnoreCase(blockNumOrTag)) {
      throw new JsonRpcApiException("TAG [earliest | pending] not supported");
    } else if ("latest".equalsIgnoreCase(blockNumOrTag)) {
      byte[] addressByte = addressHashToByteArray(address);

      StorageRowStore store = manager.getStorageRowStore();
      Storage storage = new Storage(addressByte, store);

      DataWord value = storage.getValue(new DataWord(ByteArray.fromHexString(storageIdx)));
      return value == null ? null : ByteArray.toJsonHex(value.getData());
    } else {
      try {
        ByteArray.hexToBigInteger(blockNumOrTag);
      } catch (Exception e) {
        throw new JsonRpcApiException("invalid block number");
      }

      throw new JsonRpcApiException("QUANTITY not supported, just support TAG as latest");
    }
  }

  @Override
  public String getSendTransactionCountOfAddress(String address, String blockNumOrTag) {
    //发起人为某个地址的交易总数。FullNode无法实现该功能
    return ByteArray.toJsonHex(
        wallet.getNowBlock().getBlockHeader().getRawData().getTimestamp() + 60 * 1000);
  }

  @Override
  public String getABIofSmartContract(String contractAddress, String blockNumOrTag) {
    if ("earliest".equalsIgnoreCase(blockNumOrTag)
        || "pending".equalsIgnoreCase(blockNumOrTag)) {
      throw new JsonRpcApiException("TAG [earliest | pending] not supported");
    } else if ("latest".equalsIgnoreCase(blockNumOrTag)) {
      //获取某个合约地址的字节码
      byte[] addressData = addressHashToByteArray(contractAddress);

      BytesMessage.Builder build = BytesMessage.newBuilder();
      BytesMessage bytesMessage = build.setValue(ByteString.copyFrom(addressData)).build();
      SmartContract smartContract = wallet.getContract(bytesMessage);

      if (smartContract != null) {
        return ByteArray.toJsonHex(smartContract.getBytecode().toByteArray());
      } else {
        return "0x";
      }

    } else {
      try {
        ByteArray.hexToBigInteger(blockNumOrTag);
      } catch (Exception e) {
        throw new JsonRpcApiException("invalid block number");
      }

      throw new JsonRpcApiException("QUANTITY not supported, just support TAG as latest");
    }
  }

  @Override
  public String getCoinbase() {
    //获取最新块的产块sr地址
    byte[] witnessAddress = wallet.getNowBlock().getBlockHeader().getRawData().getWitnessAddress()
        .toByteArray();
    if (witnessAddress == null || witnessAddress.length != 21) {
      throw new JsonRpcApiException("invalid witness address");
    }
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
  public String estimateGas(CallArguments args) {
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
    throw new UnsupportedOperationException(
        "the method eth_getCompilers does not exist/is not available");
  }

  @Override
  public CompilationResult ethCompileSolidity(String contract) {
    throw new UnsupportedOperationException(
        "the method eth_compileSolidity does not exist/is not available");
  }

  @Override
  public CompilationResult ethCompileLLL(String contract) {
    throw new UnsupportedOperationException(
        "the method eth_compileLLL does not exist/is not available");
  }

  @Override
  public CompilationResult ethCompileSerpent(String contract) {
    throw new UnsupportedOperationException(
        "the method eth_compileSerpent does not exist/is not available");
  }

  @Override
  public TransactionResult getTransactionByHash(String txid) {
    byte[] txHash = hashToByteArray(txid);

    TransactionInfo transactionInfo = wallet
        .getTransactionInfoById(ByteString.copyFrom(txHash));
    if (transactionInfo == null) {
      return null;
    }

    long blockNum = transactionInfo.getBlockNumber();
    Block block = wallet.getBlockByNum(blockNum);
    if (block == null) {
      return null;
    }

    return formatRpcTransaction(transactionInfo, block);
  }

  private TransactionResult formatRpcTransaction(TransactionInfo transactioninfo, Block block) {
    String txid = ByteArray.toHexString(transactioninfo.getId().toByteArray());

    Transaction transaction = null;
    int transactionIndex = -1;

    List<Transaction> txList = block.getTransactionsList();
    for (int index = 0; index < txList.size(); index++) {
      transaction = txList.get(index);
      if (getTxID(transaction).equals(txid)) {
        transactionIndex = index;
        break;
      }
    }

    if (transactionIndex == -1) {
      return null;
    }

    return new TransactionResult(block, transactionIndex, transaction, wallet);
  }

  public TransactionResult getTransactionByBlockAndIndex(Block block, String index) {
    int txIndex;
    try {
      txIndex = ByteArray.jsonHexToInt(index);
    } catch (Exception e) {
      throw new JsonRpcApiException("invalid index value");
    }

    if (txIndex >= block.getTransactionsCount()) {
      return null;
    }
    Transaction transaction = block.getTransactions(txIndex);
    return new TransactionResult(block, txIndex, transaction, wallet);
  }

  @Override
  public TransactionResult getTransactionByBlockHashAndIndex(String blockHash, String index) {
    final Block block = getBlockByJsonHash(blockHash);

    if (block == null) {
      return null;
    }

    return getTransactionByBlockAndIndex(block, index);
  }

  @Override
  public TransactionResult getTransactionByBlockNumberAndIndex(String blockNumOrTag, String index) {
    Block block = wallet.getByJsonBlockId(blockNumOrTag);
    if (block == null) {
      return null;
    }

    return getTransactionByBlockAndIndex(block, index);
  }

  @Override
  public TransactionReceipt getTransactionReceipt(String txid) {
    byte[] txHash = hashToByteArray(txid);

    TransactionInfo transactionInfo = wallet.getTransactionInfoById(ByteString.copyFrom(txHash));
    if (transactionInfo == null) {
      return null;
    }

    long blockNum = transactionInfo.getBlockNumber();
    Block block = wallet.getBlockByNum(blockNum);
    if (block == null) {
      return null;
    }

    return new TransactionReceipt(block, transactionInfo, wallet);
  }

  @Override
  public String getCall(CallArguments transactionCall, String blockNumOrTag) {
    if ("earliest".equalsIgnoreCase(blockNumOrTag)
        || "pending".equalsIgnoreCase(blockNumOrTag)) {
      throw new JsonRpcApiException("TAG [earliest | pending] not supported");
    } else if ("latest".equalsIgnoreCase(blockNumOrTag)) {
      //静态调用合约方法。
      byte[] addressData = addressHashToByteArray(transactionCall.from);
      byte[] contractAddressData = addressHashToByteArray(transactionCall.to);

      return call(addressData, contractAddressData, ByteArray.fromHexString(transactionCall.data));
    } else {
      try {
        ByteArray.hexToBigInteger(blockNumOrTag).longValue();
      } catch (Exception e) {
        throw new JsonRpcApiException("invalid block number");
      }

      throw new JsonRpcApiException("QUANTITY not supported, just support TAG as latest");
    }
  }

  //生成一个调用 eth_call api的参数，可以自由修改
  private String generateCallParameter1() {
    String ownerAddress = "TXvRyjomvtNWSKvNouTvAedRGD4w9RXLZD";
    String usdjAddress = "TLBaRhANQoJFTqre9Nf1mjuwNWjCJeYqUL"; // nile测试环境udsj地址

    byte[] addressData = Commons.decodeFromBase58Check(ownerAddress);
    byte[] addressDataWord = new byte[32];
    System.arraycopy(Commons.decodeFromBase58Check(ownerAddress), 0, addressDataWord,
        32 - addressData.length, addressData.length);
    String data = getMethodSign("balanceOf(address)") + Hex.toHexString(addressDataWord);

    CallArguments transactionCall = new CallArguments();
    transactionCall.from = ByteArray.toHexString(Commons.decodeFromBase58Check(ownerAddress));
    transactionCall.to = ByteArray.toHexString(Commons.decodeFromBase58Check(usdjAddress));
    transactionCall.data = data;

    StringBuffer sb = new StringBuffer("{\"jsonrpc\":\"2.0\",\"method\":\"eth_call\",\"params\":[");
    sb.append(transactionCall);
    sb.append(", \"latest\"],\"id\":1}");
    return sb.toString();
  }

  //生成一个调用 eth_call api的参数，可以自由修改
  private String generateCallParameter2() {
    String ownerAddress = "TRXPT6Ny7EFvTPv7mFUqaFUST39WUZ4zzz";
    String usdjAddress = "TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf"; // nile测试环境udsj地址

    byte[] addressData = Commons.decodeFromBase58Check(ownerAddress);
    byte[] addressDataWord = new byte[32];
    System.arraycopy(Commons.decodeFromBase58Check(ownerAddress), 0, addressDataWord,
        32 - addressData.length, addressData.length);
    String data = getMethodSign("name()");

    CallArguments transactionCall = new CallArguments();
    transactionCall.from = ByteArray.toHexString(Commons.decodeFromBase58Check(ownerAddress));
    transactionCall.to = ByteArray.toHexString(Commons.decodeFromBase58Check(usdjAddress));
    transactionCall.data = data;

    StringBuffer sb = new StringBuffer("{\"jsonrpc\":\"2.0\",\"method\":\"eth_call\",\"params\":[");
    sb.append(transactionCall);
    sb.append(", \"latest\"],\"id\":1}");
    return sb.toString();
  }

  private String generateStorageParameter() {
    // nile合约：TXEphLzyv5jFwvjzwMok9UoehaSn294ZhN
    String contractAddress = "41E94EAD5F4CA072A25B2E5500934709F1AEE3C64B";

    // nile测试环境：TXvRyjomvtNWSKvNouTvAedRGD4w9RXLZD
    String sendAddress = "41F0CC5A2A84CD0F68ED1667070934542D673ACBD8";
    String index = "01";
    byte[] byte1 = new DataWord(new DataWord(sendAddress).getLast20Bytes()).getData();
    byte[] byte2 = new DataWord(new DataWord(index).getLast20Bytes()).getData();
    byte[] byte3 = ByteUtil.merge(byte1, byte2);
    String position = ByteArray.toJsonHex(Hash.sha3(byte3));

    StringBuffer sb = new StringBuffer(
        "{\"jsonrpc\":\"2.0\",\"method\":\"eth_getStorageAt\",\"params\":[\"0x");
    sb.append(contractAddress + "\",\"");
    sb.append(position + "\",");
    sb.append("\"latest\"],\"id\":1}");
    return sb.toString();
  }

  @Override
  public String getPeerCount() {
    //返回当前节点所连接的peer节点数量
    return ByteArray.toJsonHex(nodeInfoService.getNodeInfo().getPeerList().size());
  }

  @Override
  public Object getSyncingStatus() {
    //查询同步状态。未同步返回false，否则返回 SyncingResult
    if (nodeInfoService.getNodeInfo().getPeerList().isEmpty()) {
      return false;
    }

    long startingBlockNum = nodeInfoService.getNodeInfo().getBeginSyncNum();
    Block nowBlock = wallet.getNowBlock();
    long currentBlockNum = nowBlock.getBlockHeader().getRawData().getNumber();
    long diff = (System.currentTimeMillis()
        - nowBlock.getBlockHeader().getRawData().getTimestamp()) / 3000;
    diff = diff > 0 ? diff : 0;
    long highestBlockNum = currentBlockNum + diff; //预测的最高块号

    return new SyncingResult(ByteArray.toJsonHex(startingBlockNum),
        ByteArray.toJsonHex(currentBlockNum),
        ByteArray.toJsonHex(highestBlockNum)
    );
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
  public List<Object> ethGetWork() {
    Block block = wallet.getNowBlock();
    String blockHash = null;

    if (block != null) {
      blockHash = ByteArray.toJsonHex(new BlockCapsule(block).getBlockId().getBytes());
    }

    return Arrays.asList(
        blockHash,
        null,
        null
    );
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

  @Override
  public String[] getAccounts() {
    return new String[0];
  }

  @Override
  public boolean ethSubmitWork(String nonceHex, String headerHex, String digestHex) {
    throw new UnsupportedOperationException(
        "the method eth_submitWork does not exist/is not available");
  }

  @Override
  public String ethSendRawTransaction(String rawData) {
    throw new UnsupportedOperationException(
        "the method eth_sendRawTransaction does not exist/is not available");
  }

  @Override
  public String ethSendTransaction(CallArguments args) {
    throw new UnsupportedOperationException(
        "the method eth_sendTransaction does not exist/is not available");
  }

  @Override
  public String ethSign(String address, String msg) {
    throw new UnsupportedOperationException(
        "the method eth_sign does not exist/is not available");
  }

  @Override
  public String ethSignTransaction(CallArguments transactionArgs) {
    throw new UnsupportedOperationException(
        "the method eth_signTransaction does not exist/is not available");
  }

  public static void main(String[] args) {
    TronJsonRpcImpl impl = new TronJsonRpcImpl();
    System.out.println(impl.generateCallParameter1());
    System.out.println(impl.generateCallParameter2());
    System.out.println(impl.generateStorageParameter());
  }
}
