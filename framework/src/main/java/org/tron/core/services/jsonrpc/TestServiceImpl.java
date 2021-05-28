package org.tron.core.services.jsonrpc;

import static org.tron.common.utils.Commons.decode58Check;
import static org.tron.common.utils.DecodeUtil.addressValid;
import static org.tron.core.Wallet.CONTRACT_VALIDATE_ERROR;
import static org.tron.core.Wallet.CONTRACT_VALIDATE_EXCEPTION;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.exception.VMIllegalException;
import org.tron.core.services.NodeInfoService;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;

@Slf4j(topic = "API")
@Component
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
    byte[] addressData = decodeFromBase58Check(address);
    Account account = Account.newBuilder().setAddress(ByteString.copyFrom(addressData)).build();
    return wallet.getAccount(account).getBalance();
  }

  private String getMethodSign(String method) {
    byte[] selector = new byte[4];
    System.arraycopy(Hash.sha3(method.getBytes()), 0, selector, 0, 4);
    return Hex.toHexString(selector);
  }

  public byte[] decodeFromBase58Check(String addressBase58) {
    if (StringUtils.isEmpty(addressBase58)) {
      // logger.warn("Warning: Address is empty !!");
      return null;
    }

    byte[] address;
    try {
      address = decode58Check(addressBase58);
      if (!addressValid(address)) {
        return null;
      }
    } catch (Exception e) {
      // logger.warn("decodeFromBase58Check exception, address is " + addressBase58);
      return null;
    }

    return address;
  }

  public TriggerSmartContract triggerCallContract(byte[] address, byte[] contractAddress,
      long callValue, byte[] data, long tokenValue, String tokenId) {
    TriggerSmartContract.Builder builder = TriggerSmartContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(address));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setData(ByteString.copyFrom(data));
    builder.setCallValue(callValue);
    if (tokenId != null && tokenId != "") {
      builder.setCallTokenValue(tokenValue);
      builder.setTokenId(Long.parseLong(tokenId));
    }
    return builder.build();
  }

  @Override
  public BigInteger getTrc20Balance(String ownerAddress, String contractAddress,
      String blockNumOrTag) {
    //某个用户拥有的某个token20余额，带精度
    byte[] addressData = decodeFromBase58Check(ownerAddress);
    byte[] addressDataWord = new byte[32];
    System.arraycopy(addressData, 0, addressDataWord, 32 - addressData.length, addressData.length);
    String dataStr = getMethodSign("balanceOf(address)") + Hex.toHexString(addressDataWord);

    //构造静态合约时，只需要3个字段
    TriggerSmartContract triggerContract = triggerCallContract(addressData,
        decodeFromBase58Check(contractAddress), 0,
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
  public int getSendTransactionCountOfAddress(String address, String blockNumOrTag) {
    //发起人为某个地址的交易总数。FullNode无法实现该功能
    return -1;
  }

  @Override
  public String getABIofSmartContract(String contractAddress) {
    //获取某个合约地址的字节码
    byte[] addressData = decodeFromBase58Check(contractAddress);
    BytesMessage.Builder build = BytesMessage.newBuilder();
    BytesMessage bytesMessage = build.setValue(ByteString.copyFrom(addressData)).build();
    SmartContract smartContract = wallet.getContract(bytesMessage);
    return ByteArray.toHexString(smartContract.getBytecode().toByteArray());
  }
}