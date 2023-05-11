package org.tron.common.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.sm2.SM2;
import org.tron.common.crypto.sm2.SM2Signer;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.client.Parameter;
import org.tron.common.utils.client.utils.Base58;
import org.tron.common.utils.client.utils.TransactionUtils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.SmartContractOuterClass;

@Slf4j
public class PublicMethod {

  private static byte addressPreFixByte = Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET;

  public static String getRandomPrivateKey() {
    ECKey key = new ECKey(Utils.getRandom());
    return Hex.toHexString(Objects.requireNonNull(key.getPrivKeyBytes()));
  }

  public static String getHexAddressByPrivateKey(String privateKey) {
    BigInteger priK = new BigInteger(privateKey, 16);
    ECKey temKey = ECKey.fromPrivate(priK);
    return ByteArray.toHexString(temKey.getAddress());
  }

  public static byte[] getAddressByteByPrivateKey(String privateKey) {
    BigInteger priK = new BigInteger(privateKey, 16);
    ECKey temKey = ECKey.fromPrivate(priK);
    return temKey.getAddress();
  }

  public static String getPublicByPrivateKey(String privateKey) {
    BigInteger priK = new BigInteger(privateKey, 16);
    ECKey temKey = ECKey.fromPrivate(priK);
    return Hex.toHexString(temKey.getPubKey());
  }

  public static byte[] getPublicKeyFromPrivate(String privateKey) {
    BigInteger tmpKey = new BigInteger(privateKey, 16);
    return ECKey.publicKeyFromPrivate(tmpKey, true);
  }

  public static String getCompressedPubString(String privateKey) {
    byte[] publicKeyFromPrivate = getPublicKeyFromPrivate(privateKey);
    return Hex.toHexString(publicKeyFromPrivate);
  }

  public static String getSM2RandomPrivateKey() {
    SM2 key = new SM2(Utils.getRandom());
    return Hex.toHexString(Objects.requireNonNull(key.getPrivKeyBytes()));
  }

  public static String getSM2PublicByPrivateKey(String privateKey) {
    BigInteger priK = new BigInteger(privateKey, 16);
    SM2 key = SM2.fromPrivate(priK);
    return Hex.toHexString(key.getPubKey());
  }

  public static String getSM2AddressByPrivateKey(String privateKey) {
    BigInteger priK = new BigInteger(privateKey, 16);
    SM2 key = SM2.fromPrivate(priK);
    return ByteArray.toHexString(key.getAddress());
  }

  public static byte[] getSM2PublicKeyFromPrivate(String privateKey) {
    BigInteger tmpKey = new BigInteger(privateKey, 16);
    return SM2.publicKeyFromPrivate(tmpKey, true);
  }

  public static String getSM2CompressedPubString(String privateKey) {
    byte[] publicKeyFromPrivate = getSM2PublicKeyFromPrivate(privateKey);
    return Hex.toHexString(publicKeyFromPrivate);
  }

  public static byte[] getSM2HashByPubKey(byte[] pubKey, String message) {
    SM2 key = SM2.fromPublicOnly(pubKey);
    SM2Signer signer = key.getSM2SignerForHash();
    return signer.generateSM3Hash(message.getBytes());
  }


  /** constructor. */
  public static SmartContractOuterClass.SmartContract.ABI jsonStr2Abi(String jsonStr) {
    if (jsonStr == null) {
      return null;
    }

    JsonParser jsonParser = new JsonParser();
    JsonElement jsonElementRoot = jsonParser.parse(jsonStr);
    JsonArray jsonRoot = jsonElementRoot.getAsJsonArray();
    SmartContractOuterClass.SmartContract.ABI.Builder abiBuilder =
            SmartContractOuterClass.SmartContract.ABI.newBuilder();
    for (int index = 0; index < jsonRoot.size(); index++) {
      JsonElement abiItem = jsonRoot.get(index);
      boolean anonymous =
          abiItem.getAsJsonObject().get("anonymous") != null
          && abiItem.getAsJsonObject().get("anonymous").getAsBoolean();
      final boolean constant =
          abiItem.getAsJsonObject().get("constant") != null
          && abiItem.getAsJsonObject().get("constant").getAsBoolean();
      final String name =
          abiItem.getAsJsonObject().get("name") != null
          ? abiItem.getAsJsonObject().get("name").getAsString()
            : null;
      JsonArray inputs =
          abiItem.getAsJsonObject().get("inputs") != null
          ? abiItem.getAsJsonObject().get("inputs").getAsJsonArray() : null;
      final JsonArray outputs =
          abiItem.getAsJsonObject().get("outputs") != null
          ? abiItem.getAsJsonObject().get("outputs").getAsJsonArray() : null;
      String type =
          abiItem.getAsJsonObject().get("type") != null
          ? abiItem.getAsJsonObject().get("type").getAsString() : null;
      final boolean payable =
              abiItem.getAsJsonObject().get("payable") != null
                      && abiItem.getAsJsonObject().get("payable").getAsBoolean();
      final String stateMutability =
              abiItem.getAsJsonObject().get("stateMutability") != null
                      ? abiItem.getAsJsonObject().get("stateMutability").getAsString()
                      : null;
      if (type == null) {
        logger.error("No type!");
        return null;
      }
      if (!type.equalsIgnoreCase("fallback") && null == inputs) {
        logger.error("No inputs!");
        return null;
      }

      SmartContractOuterClass.SmartContract.ABI.Entry.Builder entryBuilder =
              SmartContractOuterClass.SmartContract.ABI.Entry.newBuilder();
      entryBuilder.setAnonymous(anonymous);
      entryBuilder.setConstant(constant);
      if (name != null) {
        entryBuilder.setName(name);
      }

      /* { inputs : optional } since fallback function not requires inputs*/
      if (inputs != null) {
        for (int j = 0; j < inputs.size(); j++) {
          JsonElement inputItem = inputs.get(j);
          if (inputItem.getAsJsonObject().get("name") == null
                  || inputItem.getAsJsonObject().get("type") == null) {
            logger.error("Input argument invalid due to no name or no type!");
            return null;
          }
          String inputName = inputItem.getAsJsonObject().get("name").getAsString();
          String inputType = inputItem.getAsJsonObject().get("type").getAsString();
          SmartContractOuterClass.SmartContract.ABI.Entry.Param.Builder paramBuilder
                  = SmartContractOuterClass.SmartContract.ABI.Entry.Param.newBuilder();
          JsonElement indexed = inputItem.getAsJsonObject().get("indexed");

          paramBuilder.setIndexed((indexed != null) && indexed.getAsBoolean());
          paramBuilder.setName(inputName);
          paramBuilder.setType(inputType);
          entryBuilder.addInputs(paramBuilder.build());
        }
      }

      /* { outputs : optional } */
      if (outputs != null) {
        for (int k = 0; k < outputs.size(); k++) {
          JsonElement outputItem = outputs.get(k);
          if (outputItem.getAsJsonObject().get("name") == null
                  || outputItem.getAsJsonObject().get("type") == null) {
            logger.error("Output argument invalid due to no name or no type!");
            return null;
          }
          String outputName = outputItem.getAsJsonObject().get("name").getAsString();
          String outputType = outputItem.getAsJsonObject().get("type").getAsString();
          SmartContractOuterClass.SmartContract.ABI.Entry.Param.Builder paramBuilder =
                  SmartContractOuterClass.SmartContract.ABI.Entry.Param.newBuilder();
          JsonElement indexed = outputItem.getAsJsonObject().get("indexed");

          paramBuilder.setIndexed((indexed != null) && indexed.getAsBoolean());
          paramBuilder.setName(outputName);
          paramBuilder.setType(outputType);
          entryBuilder.addOutputs(paramBuilder.build());
        }
      }

      entryBuilder.setType(getEntryType(type));
      entryBuilder.setPayable(payable);
      if (stateMutability != null) {
        entryBuilder.setStateMutability(getStateMutability(stateMutability));
      }

      abiBuilder.addEntrys(entryBuilder.build());
    }

    return abiBuilder.build();
  }

  /** constructor. */
  public static SmartContractOuterClass.SmartContract.ABI.Entry.EntryType
          getEntryType(String type) {
    switch (type) {
      case "constructor":
        return SmartContractOuterClass.SmartContract.ABI.Entry.EntryType.Constructor;
      case "function":
        return SmartContractOuterClass.SmartContract.ABI.Entry.EntryType.Function;
      case "event":
        return SmartContractOuterClass.SmartContract.ABI.Entry.EntryType.Event;
      case "fallback":
        return SmartContractOuterClass.SmartContract.ABI.Entry.EntryType.Fallback;
      case "error":
        return SmartContractOuterClass.SmartContract.ABI.Entry.EntryType.Error;
      default:
        return SmartContractOuterClass.SmartContract.ABI.Entry.EntryType.UNRECOGNIZED;
    }
  }


  /** constructor. */
  public static SmartContractOuterClass.SmartContract.ABI.Entry.StateMutabilityType
            getStateMutability(String stateMutability) {
    switch (stateMutability) {
      case "pure":
        return SmartContractOuterClass.SmartContract.ABI.Entry.StateMutabilityType.Pure;
      case "view":
        return SmartContractOuterClass.SmartContract.ABI.Entry.StateMutabilityType.View;
      case "nonpayable":
        return SmartContractOuterClass.SmartContract.ABI.Entry.StateMutabilityType.Nonpayable;
      case "payable":
        return SmartContractOuterClass.SmartContract.ABI.Entry.StateMutabilityType.Payable;
      default:
        return SmartContractOuterClass.SmartContract.ABI.Entry.StateMutabilityType.UNRECOGNIZED;
    }
  }

  /**
   * Convert to pub.
   * @param priKey private key
   * @return public addr
   */
  public static byte[] getFinalAddress(String priKey) {
    Wallet.setAddressPreFixByte((byte) 0x41);
    ECKey key = ECKey.fromPrivate(new BigInteger(priKey, 16));
    return key.getAddress();
  }

  public static byte getAddressPreFixByte() {
    return addressPreFixByte;
  }

  public static void setAddressPreFixByte(byte addressPreFixByte) {
    PublicMethod.addressPreFixByte = addressPreFixByte;
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
   * Transfer TRX.
   * @param to addr receives the asset
   * @param amount asset amount
   * @param owner  sender
   * @param priKey private key of the sender
   * @param blockingStubFull Grpc interface
   * @return true or false
   */
  public static Boolean sendcoin(byte[] to, long amount, byte[] owner, String priKey,
                                 WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte((byte) 0x41);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    int times = 0;
    while (times++ <= 2) {

      BalanceContract.TransferContract.Builder builder =
          BalanceContract.TransferContract.newBuilder();
      com.google.protobuf.ByteString bsTo = com.google.protobuf.ByteString.copyFrom(to);
      com.google.protobuf.ByteString bsOwner = ByteString.copyFrom(owner);
      builder.setToAddress(bsTo);
      builder.setOwnerAddress(bsOwner);
      builder.setAmount(amount);

      BalanceContract.TransferContract contract = builder.build();
      Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        continue;
      }
      transaction = signTransaction(ecKey, transaction);
      GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
      return response.getResult();
    }
    return false;
  }

  /**
   * Sign TX.
   * @param ecKey ecKey of the private key
   * @param transaction transaction object
   */
  public static Protocol.Transaction signTransaction(ECKey ecKey,
                                                     Protocol.Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
  }

  /**
   * Broadcast TX.
   * @param transaction transaction object
   * @param blockingStubFull Grpc interface
   */
  public static GrpcAPI.Return broadcastTransaction(
      Protocol.Transaction transaction, WalletGrpc.WalletBlockingStub blockingStubFull) {
    int i = 10;
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    while (!response.getResult() && response.getCode() == GrpcAPI.Return.response_code.SERVER_BUSY
        && i > 0) {
      try {
        Thread.sleep(300);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      i--;
      response = blockingStubFull.broadcastTransaction(transaction);
    }
    return response;
  }
}
