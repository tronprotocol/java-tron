package org.tron.core.services.jsonrpc;

import static org.tron.core.Wallet.CONTRACT_VALIDATE_ERROR;
import static org.tron.core.Wallet.CONTRACT_VALIDATE_EXCEPTION;
import static org.tron.core.services.http.Util.setTransactionExtraData;
import static org.tron.core.services.http.Util.setTransactionPermissionId;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.addressHashToByteArray;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getTxID;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.triggerCallContract;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.common.crypto.Hash;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.JsonRpcInternalException;
import org.tron.core.exception.JsonRpcInvalidParamsException;
import org.tron.core.exception.JsonRpcInvalidRequestException;
import org.tron.core.exception.VMIllegalException;
import org.tron.core.services.NodeInfoService;
import org.tron.core.services.http.JsonFormat;
import org.tron.core.services.http.Util;
import org.tron.core.store.StorageRowStore;
import org.tron.core.vm.program.Storage;
import org.tron.program.Version;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.AssetIssueContractOuterClass.TransferAssetContract;
import org.tron.protos.contract.BalanceContract.TransferContract;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;

@Slf4j(topic = "API")
public class TronJsonRpcImpl implements TronJsonRpc {

  String regexHash = "(0x)?[a-zA-Z0-9]{64}$";
  private final int chainId = 100;
  private final int networkId = 100;

  private NodeInfoService nodeInfoService;
  private Wallet wallet;
  private Manager manager;

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
  public String web3Sha3(String data) throws JsonRpcInvalidParamsException {
    byte[] input;
    try {
      input = ByteArray.fromHexString(data);
    } catch (Exception e) {
      throw new JsonRpcInvalidParamsException("invalid input value");
    }

    byte[] result = Hash.sha3(input);
    return ByteArray.toJsonHex(result);
  }

  @Override
  public String ethGetBlockTransactionCountByHash(String blockHash)
      throws JsonRpcInvalidParamsException {
    Block b = getBlockByJsonHash(blockHash);
    if (b == null) {
      return null;
    }

    long n = b.getTransactionsList().size();
    return ByteArray.toJsonHex(n);
  }

  @Override
  public String ethGetBlockTransactionCountByNumber(String blockNumOrTag)
      throws JsonRpcInvalidParamsException {
    List<Transaction> list = wallet.getTransactionsByJsonBlockId(blockNumOrTag);
    if (list == null) {
      return null;
    }

    long n = list.size();
    return ByteArray.toJsonHex(n);
  }

  @Override
  public BlockResult ethGetBlockByHash(String blockHash, Boolean fullTransactionObjects)
      throws JsonRpcInvalidParamsException {
    final Block b = getBlockByJsonHash(blockHash);
    return getBlockResult(b, fullTransactionObjects);
  }

  @Override
  public BlockResult ethGetBlockByNumber(String blockNumOrTag, Boolean fullTransactionObjects)
      throws JsonRpcInvalidParamsException {
    final Block b = wallet.getByJsonBlockId(blockNumOrTag);
    return (b == null ? null : getBlockResult(b, fullTransactionObjects));
  }

  private byte[] hashToByteArray(String hash) throws JsonRpcInvalidParamsException {
    if (!Pattern.matches(regexHash, hash)) {
      throw new JsonRpcInvalidParamsException("invalid hash value");
    }

    byte[] bHash;
    try {
      bHash = ByteArray.fromHexString(hash);
    } catch (Exception e) {
      throw new JsonRpcInvalidParamsException(e.getMessage());
    }
    return bHash;
  }

  private Block getBlockByJsonHash(String blockHash) throws JsonRpcInvalidParamsException {
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
    // network id
    return ByteArray.toJsonHex(networkId);
  }

  @Override
  public String ethChainId() {
    return ByteArray.toJsonHex(chainId);
  }

  @Override
  public boolean isListening() {
    int activeConnectCount = nodeInfoService.getNodeInfo().getActiveConnectCount();
    return activeConnectCount >= 1;
  }

  @Override
  public String getProtocolVersion() {
    return ByteArray.toJsonHex(wallet.getNowBlock().getBlockHeader().getRawData().getVersion());
  }

  @Override
  public String getLatestBlockNum() {
    return ByteArray.toJsonHex(wallet.getNowBlock().getBlockHeader().getRawData().getNumber());
  }

  @Override
  public String getTrxBalance(String address, String blockNumOrTag)
      throws JsonRpcInvalidParamsException {
    if ("earliest".equalsIgnoreCase(blockNumOrTag)
        || "pending".equalsIgnoreCase(blockNumOrTag)) {
      throw new JsonRpcInvalidParamsException("TAG [earliest | pending] not supported");
    } else if ("latest".equalsIgnoreCase(blockNumOrTag)) {
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
        throw new JsonRpcInvalidParamsException("invalid block number");
      }

      throw new JsonRpcInvalidParamsException("QUANTITY not supported, just support TAG as latest");
    }
  }

  /**
   * @param data Hash of the method signature and encoded parameters. for example:
   * getMethodSign(methodName(uint256,uint256)) || data1 || data2
   */
  private String call(byte[] ownerAddressByte, byte[] contractAddressByte, byte[] data) {

    TriggerSmartContract triggerContract = triggerCallContract(
        ownerAddressByte,
        contractAddressByte,
        0,
        data,
        0,
        null
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
  public String getStorageAt(String address, String storageIdx, String blockNumOrTag)
      throws JsonRpcInvalidParamsException {
    if ("earliest".equalsIgnoreCase(blockNumOrTag)
        || "pending".equalsIgnoreCase(blockNumOrTag)) {
      throw new JsonRpcInvalidParamsException("TAG [earliest | pending] not supported");
    } else if ("latest".equalsIgnoreCase(blockNumOrTag)) {
      byte[] addressByte = addressHashToByteArray(address);

      StorageRowStore store = manager.getStorageRowStore();
      Storage storage = new Storage(addressByte, store);

      DataWord value = storage.getValue(new DataWord(ByteArray.fromHexString(storageIdx)));
      return ByteArray.toJsonHex(value == null ? new byte[32] : value.getData());
    } else {
      try {
        ByteArray.hexToBigInteger(blockNumOrTag);
      } catch (Exception e) {
        throw new JsonRpcInvalidParamsException("invalid block number");
      }

      throw new JsonRpcInvalidParamsException("QUANTITY not supported, just support TAG as latest");
    }
  }

  @Override
  public String getABIofSmartContract(String contractAddress, String blockNumOrTag)
      throws JsonRpcInvalidParamsException {
    if ("earliest".equalsIgnoreCase(blockNumOrTag)
        || "pending".equalsIgnoreCase(blockNumOrTag)) {
      throw new JsonRpcInvalidParamsException("TAG [earliest | pending] not supported");
    } else if ("latest".equalsIgnoreCase(blockNumOrTag)) {
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
        throw new JsonRpcInvalidParamsException("invalid block number");
      }

      throw new JsonRpcInvalidParamsException("QUANTITY not supported, just support TAG as latest");
    }
  }

  @Override
  public String getCoinbase() throws JsonRpcInternalException {
    byte[] witnessAddress = wallet.getNowBlock().getBlockHeader().getRawData().getWitnessAddress()
        .toByteArray();
    if (witnessAddress == null || witnessAddress.length != 21) {
      throw new JsonRpcInternalException("invalid witness address");
    }
    return ByteArray.toJsonHexAddress(witnessAddress);
  }

  // return energy fee
  @Override
  public String gasPrice() {
    BigInteger gasPrice;
    // 1sun as 1 Gwei(1 eth = 10^9Gwei), return wei(1 eth=10^18 wei), so 1 sun as 10^9 wei
    BigInteger multiplier = new BigInteger("1000000000", 10);

    gasPrice = BigInteger.valueOf(wallet.getEnergyFee());
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
  public CompilationResult ethSubmitHashrate(String hashrate, String id) {
    throw new UnsupportedOperationException(
        "the method eth_submitHashrate does not exist/is not available");
  }

  @Override
  public TransactionResult getTransactionByHash(String txid) throws JsonRpcInvalidParamsException {
    TransactionInfo transactionInfo = wallet
        .getTransactionInfoById(ByteString.copyFrom(hashToByteArray(txid)));
    if (transactionInfo == null) {
      return null;
    }

    Block block = wallet.getBlockByNum(transactionInfo.getBlockNumber());
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

  public TransactionResult getTransactionByBlockAndIndex(Block block, String index)
      throws JsonRpcInvalidParamsException {
    int txIndex;
    try {
      txIndex = ByteArray.jsonHexToInt(index);
    } catch (Exception e) {
      throw new JsonRpcInvalidParamsException("invalid index value");
    }

    if (txIndex >= block.getTransactionsCount()) {
      return null;
    }
    Transaction transaction = block.getTransactions(txIndex);
    return new TransactionResult(block, txIndex, transaction, wallet);
  }

  @Override
  public TransactionResult getTransactionByBlockHashAndIndex(String blockHash, String index)
      throws JsonRpcInvalidParamsException {
    final Block block = getBlockByJsonHash(blockHash);

    if (block == null) {
      return null;
    }

    return getTransactionByBlockAndIndex(block, index);
  }

  @Override
  public TransactionResult getTransactionByBlockNumberAndIndex(String blockNumOrTag, String index)
      throws JsonRpcInvalidParamsException {
    Block block = wallet.getByJsonBlockId(blockNumOrTag);
    if (block == null) {
      return null;
    }

    return getTransactionByBlockAndIndex(block, index);
  }

  @Override
  public TransactionReceipt getTransactionReceipt(String txid)
      throws JsonRpcInvalidParamsException {
    TransactionInfo transactionInfo =
        wallet.getTransactionInfoById(ByteString.copyFrom(hashToByteArray(txid)));
    if (transactionInfo == null) {
      return null;
    }

    Block block = wallet.getBlockByNum(transactionInfo.getBlockNumber());
    if (block == null) {
      return null;
    }

    return new TransactionReceipt(block, transactionInfo, wallet);
  }

  @Override
  public String getCall(CallArguments transactionCall, String blockNumOrTag)
      throws JsonRpcInvalidParamsException {
    if ("earliest".equalsIgnoreCase(blockNumOrTag)
        || "pending".equalsIgnoreCase(blockNumOrTag)) {
      throw new JsonRpcInvalidParamsException("TAG [earliest | pending] not supported");
    } else if ("latest".equalsIgnoreCase(blockNumOrTag)) {
      //静态调用合约方法。
      byte[] addressData = addressHashToByteArray(transactionCall.from);
      byte[] contractAddressData = addressHashToByteArray(transactionCall.to);

      return call(addressData, contractAddressData, ByteArray.fromHexString(transactionCall.data));
    } else {
      try {
        ByteArray.hexToBigInteger(blockNumOrTag).longValue();
      } catch (Exception e) {
        throw new JsonRpcInvalidParamsException("invalid block number");
      }

      throw new JsonRpcInvalidParamsException("QUANTITY not supported, just support TAG as latest");
    }
  }

  @Override
  public String getPeerCount() {
    // return the peer list count
    return ByteArray.toJsonHex(nodeInfoService.getNodeInfo().getPeerList().size());
  }

  @Override
  public Object getSyncingStatus() {
    if (nodeInfoService.getNodeInfo().getPeerList().isEmpty()) {
      return false;
    }

    long startingBlockNum = nodeInfoService.getNodeInfo().getBeginSyncNum();
    Block nowBlock = wallet.getNowBlock();
    long currentBlockNum = nowBlock.getBlockHeader().getRawData().getNumber();
    long diff = (System.currentTimeMillis()
        - nowBlock.getBlockHeader().getRawData().getTimestamp()) / 3000;
    diff = diff > 0 ? diff : 0;
    long highestBlockNum = currentBlockNum + diff; // estimated the highest block number

    return new SyncingResult(ByteArray.toJsonHex(startingBlockNum),
        ByteArray.toJsonHex(currentBlockNum),
        ByteArray.toJsonHex(highestBlockNum)
    );
  }

  @Override
  public BlockResult getUncleByBlockHashAndIndex(String blockHash, String index) {
    return null;
  }

  @Override
  public BlockResult getUncleByBlockNumberAndIndex(String blockNumOrTag, String index) {
    return null;
  }

  @Override
  public String getUncleCountByBlockHash(String blockHash) {
    return "0x0";
  }

  @Override
  public String getUncleCountByBlockNumber(String blockNumOrTag) {
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
    return "0x0";
  }

  @Override
  public boolean isMining() {
    return false;
  }

  @Override
  public String[] getAccounts() {
    return new String[0];
  }

  private TransactionJson buildCreateSmartContractTransaction(byte[] ownerAddress,
      BuildArguments args) throws JsonRpcInvalidRequestException, JsonRpcInternalException {
    try {
      CreateSmartContract.Builder build = CreateSmartContract.newBuilder();

      build.setOwnerAddress(ByteString.copyFrom(ownerAddress));

      build.setCallTokenValue(args.tokenValue)
          .setTokenId(args.tokenId);

      ABI.Builder abiBuilder = ABI.newBuilder();
      if (StringUtils.isNotEmpty(args.abi)) {
        StringBuffer abiSB = new StringBuffer("{");
        abiSB.append("\"entrys\":");
        abiSB.append(args.abi);
        abiSB.append("}");
        JsonFormat.merge(abiSB.toString(), abiBuilder, args.visible);
      }

      SmartContract.Builder smartBuilder = SmartContract.newBuilder();
      smartBuilder
          .setAbi(abiBuilder)
          .setCallValue(args.parseValue())
          .setConsumeUserResourcePercent(args.consumeUserResourcePercent)
          .setOriginEnergyLimit(args.originEnergyLimit);

      smartBuilder.setOriginAddress(ByteString.copyFrom(ownerAddress));

      // bytecode + parameter
      smartBuilder.setBytecode(ByteString.copyFrom(ByteArray.fromHexString(args.data)));

      if (StringUtils.isNotEmpty(args.name)) {
        smartBuilder.setName(args.name);
      }

      build.setNewContract(smartBuilder);

      Transaction tx = wallet
          .createTransactionCapsule(build.build(), ContractType.CreateSmartContract).getInstance();
      Transaction.Builder txBuilder = tx.toBuilder();
      Transaction.raw.Builder rawBuilder = tx.getRawData().toBuilder();
      rawBuilder.setFeeLimit(args.feeLimit);

      txBuilder.setRawData(rawBuilder);
      tx = setTransactionPermissionId(args.permissionId, txBuilder.build());

      TransactionJson transactionJson = new TransactionJson();
      transactionJson.transaction = JSON.parseObject(Util.printCreateTransaction(tx, false));

      return transactionJson;
    } catch (ContractValidateException e) {
      throw new JsonRpcInvalidRequestException(e.getMessage());
    } catch (Exception e) {
      throw new JsonRpcInternalException(e.getMessage());
    }
  }

  // from and to should not be null
  private TransactionJson buildTriggerSmartContractTransaction(byte[] ownerAddress,
      BuildArguments args) throws JsonRpcInvalidParamsException, JsonRpcInvalidRequestException,
      JsonRpcInternalException {
    byte[] contractAddress = addressHashToByteArray(args.to);

    TriggerSmartContract.Builder build = TriggerSmartContract.newBuilder();
    TransactionExtention.Builder trxExtBuilder = TransactionExtention.newBuilder();
    Return.Builder retBuilder = Return.newBuilder();

    try {

      build.setOwnerAddress(ByteString.copyFrom(ownerAddress))
          .setContractAddress(ByteString.copyFrom(contractAddress));

      if (StringUtils.isNotEmpty(args.data)) {
        build.setData(ByteString.copyFrom(ByteArray.fromHexString(args.data)));
      } else {
        build.setData(ByteString.copyFrom(new byte[0]));
      }

      build.setCallTokenValue(args.tokenValue)
          .setTokenId(args.tokenId)
          .setCallValue(args.parseValue());

      Transaction tx = wallet
          .createTransactionCapsule(build.build(), ContractType.TriggerSmartContract).getInstance();

      Transaction.Builder txBuilder = tx.toBuilder();
      Transaction.raw.Builder rawBuilder = tx.getRawData().toBuilder();
      rawBuilder.setFeeLimit(args.feeLimit);
      txBuilder.setRawData(rawBuilder);

      Transaction trx = wallet
          .triggerContract(build.build(), new TransactionCapsule(txBuilder.build()), trxExtBuilder,
              retBuilder);
      trx = setTransactionPermissionId(args.permissionId, trx);
      trxExtBuilder.setTransaction(trx);
    } catch (ContractValidateException e) {
      throw new JsonRpcInvalidRequestException(e.getMessage());
    } catch (Exception e) {
      String errString = "invalid json request";
      if (e.getMessage() != null) {
        errString = e.getMessage().replaceAll("[\"]", "\'");
      }

      throw new JsonRpcInternalException(errString);
    }

    String jsonString = Util.printTransaction(trxExtBuilder.build().getTransaction(), args.visible);
    TransactionJson transactionJson = new TransactionJson();
    transactionJson.transaction = JSON.parseObject(jsonString);

    return transactionJson;
  }

  private TransactionJson createTransactionJson(GeneratedMessageV3.Builder<?> build,
      ContractType contractTyp, BuildArguments args)
      throws JsonRpcInvalidRequestException, JsonRpcInternalException {
    try {
      Transaction tx = wallet
          .createTransactionCapsule(build.build(), contractTyp)
          .getInstance();
      tx = setTransactionPermissionId(args.permissionId, tx);
      tx = setTransactionExtraData(args.extraData, tx, args.visible);

      String jsonString = Util.printCreateTransaction(tx, args.visible);

      TransactionJson transactionJson = new TransactionJson();
      transactionJson.transaction = JSON.parseObject(jsonString);

      return transactionJson;
    } catch (ContractValidateException e) {
      throw new JsonRpcInvalidRequestException(e.getMessage());
    } catch (Exception e) {
      throw new JsonRpcInternalException(e.getMessage());
    }
  }

  private TransactionJson buildTransferContractTransaction(byte[] ownerAddress,
      BuildArguments args) throws JsonRpcInvalidParamsException, JsonRpcInvalidRequestException,
      JsonRpcInternalException {
    long amount = args.parseValue();

    TransferContract.Builder build = TransferContract.newBuilder();
    build.setOwnerAddress(ByteString.copyFrom(ownerAddress))
        .setToAddress(ByteString.copyFrom(addressHashToByteArray(args.to)))
        .setAmount(amount);

    return createTransactionJson(build, ContractType.TransferContract, args);
  }

  private TransactionJson buildTransferAssetContractTransaction(byte[] ownerAddress,
      BuildArguments args) throws JsonRpcInvalidParamsException, JsonRpcInvalidRequestException,
      JsonRpcInternalException {
    TransferAssetContract.Builder build = TransferAssetContract.newBuilder();
    build.setOwnerAddress(ByteString.copyFrom(ownerAddress))
        .setToAddress(ByteString.copyFrom(addressHashToByteArray(args.to)))
        .setAssetName(ByteString.copyFrom(ByteArray.fromString(String.valueOf(args.tokenId))))
        .setAmount(args.tokenValue);

    return createTransactionJson(build, ContractType.TransferAssetContract, args);
  }

  @Override
  public TransactionJson buildTransaction(BuildArguments args)
      throws JsonRpcInvalidParamsException, JsonRpcInvalidRequestException,
      JsonRpcInternalException {
    byte[] fromAddressData;
    try {
      fromAddressData = addressHashToByteArray(args.from);
    } catch (JsonRpcInvalidParamsException e) {
      throw new JsonRpcInvalidRequestException("invalid json request");
    }

    // check possible ContractType
    ContractType contractType = args.getContractType(wallet);
    switch (contractType.getNumber()) {
      case ContractType.CreateSmartContract_VALUE:
        return buildCreateSmartContractTransaction(fromAddressData, args);
      case ContractType.TriggerSmartContract_VALUE:
        return buildTriggerSmartContractTransaction(fromAddressData, args);
      case ContractType.TransferContract_VALUE:
        return buildTransferContractTransaction(fromAddressData, args);
      case ContractType.TransferAssetContract_VALUE:
        return buildTransferAssetContractTransaction(fromAddressData, args);
      default:
        break;
    }

    return null;
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

  @Override
  public String parityNextNonce(String address) {
    throw new UnsupportedOperationException(
        "the method parity_nextNonce does not exist/is not available");
  }

  @Override
  public String getSendTransactionCountOfAddress(String address, String blockNumOrTag) {
    throw new UnsupportedOperationException(
        "the method eth_getTransactionCount does not exist/is not available");
  }
}
