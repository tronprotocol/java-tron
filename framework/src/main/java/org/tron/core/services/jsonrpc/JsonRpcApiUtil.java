package org.tron.core.services.jsonrpc;

import com.google.protobuf.ByteString;
import java.nio.charset.StandardCharsets;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.common.utils.StringUtil;
import org.tron.core.Wallet;
import org.tron.core.exception.TronException;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;

public class JsonRpcApiUtil {

  // transform the Tron address to Ethereum Address
  public static String tronToEthAddress(String tronAddress) {
    byte[] tronBytes = Commons.decodeFromBase58Check(tronAddress);
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
  public static String ethToTronAddress(String ethAddress) {
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
  public static String toChecksumAddress(String address) throws TronException {
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

  public static String getMethodSign(String method) {
    byte[] selector = new byte[4];
    System.arraycopy(Hash.sha3(method.getBytes()), 0, selector, 0, 4);
    return Hex.toHexString(selector);
  }

  public static TriggerSmartContract triggerCallContract(byte[] address, byte[] contractAddress,
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
}
