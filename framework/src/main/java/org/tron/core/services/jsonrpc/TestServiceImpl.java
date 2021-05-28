package org.tron.core.services.jsonrpc;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.common.utils.StringUtil;
import org.tron.core.Wallet;
import org.tron.core.exception.TronException;
import org.tron.core.services.NodeInfoService;
import org.tron.program.Version;

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
  public Object isSyncing() {
    return true;
  }

  @Override
  public String getCoinbase() {
    byte[] tronAddress = wallet.getNowBlock().getBlockHeader().getRawData().getWitnessAddress()
        .toByteArray();
    return tronToEthAddress(StringUtil.encode58Check(tronAddress));
  }

  // transform the Tron address to Ethereum Address
  private String tronToEthAddress(String tronAddress) {
    byte[] tronBytes =  Commons.decodeFromBase58Check(tronAddress);
    byte[] ethBytes = new byte[20];
    try {
      if ((tronBytes.length != 21 && tronBytes[0] != Wallet.getAddressPreFixByte())) {
        throw new TronException("invalid Tron address");
      }
      System.arraycopy(tronBytes, 1, ethBytes, 0, 20);
      return toChecksumAddress(ByteArray.toHexString(ethBytes));
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  // transform the Ethereum address to Tron Address
  private String ethToTronAddress(String ethAddress) {
    byte[] address = ByteArray.fromHexString(ethAddress);
    byte[] tronAddress = new byte[21];
    try {
      if (address.length != 20) {
        throw new TronException("invalid Ethereum address");
      }
      System.arraycopy(address, 0, tronAddress, 1, 20);
      tronAddress[0] = Wallet.getAddressPreFixByte();
      return StringUtil.encode58Check(tronAddress);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  //reference: https://github.com/ethereum/EIPs/blob/master/EIPS/eip-55.md
  private String toChecksumAddress(String address) throws TronException {
    StringBuffer sb = new StringBuffer();
    int nibble;

    if (address.startsWith("0x")) {
      address = address.substring(2);
    }
    String hashedAddress = ByteArray
        .toHexString(Hash.sha3(address.getBytes(StandardCharsets.UTF_8)));
    sb.append("0x");
    for (int i = 0; i < address.length(); i++) {
      if ("0123456789".contains(String.valueOf(address.charAt(i)))) {
        sb.append(address.charAt(i));
      } else if ("abcdef".contains(String.valueOf(address.charAt(i)))) {
        nibble = Integer.parseInt(String.valueOf(hashedAddress.charAt(i)), 16);
        if (nibble > 7) {
          sb.append(String.valueOf(address.charAt(i)).toUpperCase());
        } else {
          sb.append(address.charAt(i));
        }
      } else {
        throw new TronException("invalid hex character in address");
      }
    }
    return sb.toString();
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
}
