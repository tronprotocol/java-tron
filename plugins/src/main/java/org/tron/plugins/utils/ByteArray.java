package org.tron.plugins.utils;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.TypeRegistry;
import com.google.protobuf.util.JsonFormat;
import java.math.BigInteger;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.tron.protos.Protocol;
import org.tron.protos.contract.AccountContract;
import org.tron.protos.contract.AssetIssueContractOuterClass;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.ExchangeContract;
import org.tron.protos.contract.MarketContract;
import org.tron.protos.contract.ProposalContract;
import org.tron.protos.contract.ShieldContract;
import org.tron.protos.contract.SmartContractOuterClass;
import org.tron.protos.contract.StorageContract;
import org.tron.protos.contract.VoteAssetContractOuterClass;
import org.tron.protos.contract.WitnessContract;


public class ByteArray {

  public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
  public static final byte[] ZERO_BYTE_ARRAY = new byte[]{0};
  public static final int WORD_SIZE = 32;

  public static JsonFormat.Printer PRINTER;

  static {
    TypeRegistry.Builder registry = TypeRegistry.newBuilder();
    // see ContractType
    registry.add(AccountContract.AccountCreateContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(BalanceContract.TransferContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(AssetIssueContractOuterClass.TransferAssetContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(VoteAssetContractOuterClass.VoteAssetContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(WitnessContract.VoteWitnessContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(WitnessContract.WitnessCreateContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(AssetIssueContractOuterClass.AssetIssueContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(WitnessContract.WitnessUpdateContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(AssetIssueContractOuterClass.ParticipateAssetIssueContract
        .getDefaultInstance()
        .getDescriptorForType());
    registry.add(AccountContract.AccountUpdateContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(BalanceContract.FreezeBalanceContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(BalanceContract.UnfreezeBalanceContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(BalanceContract.WithdrawBalanceContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(AssetIssueContractOuterClass.UnfreezeAssetContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(AssetIssueContractOuterClass.UpdateAssetContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(ProposalContract.ProposalCreateContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(ProposalContract.ProposalApproveContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(ProposalContract.ProposalDeleteContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(AccountContract.SetAccountIdContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(SmartContractOuterClass.CreateSmartContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(SmartContractOuterClass.TriggerSmartContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(SmartContractOuterClass.UpdateSettingContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(ExchangeContract.ExchangeCreateContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(ExchangeContract.ExchangeInjectContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(ExchangeContract.ExchangeWithdrawContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(ExchangeContract.ExchangeTransactionContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(SmartContractOuterClass.UpdateEnergyLimitContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(AccountContract.AccountPermissionUpdateContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(SmartContractOuterClass.ClearABIContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(StorageContract.UpdateBrokerageContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(ShieldContract.ShieldedTransferContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(MarketContract.MarketSellAssetContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(MarketContract.MarketCancelOrderContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(BalanceContract.FreezeBalanceV2Contract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(BalanceContract.UnfreezeBalanceV2Contract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(BalanceContract.WithdrawExpireUnfreezeContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(BalanceContract.DelegateResourceContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(BalanceContract.UnDelegateResourceContract.getDefaultInstance()
        .getDescriptorForType());
    registry.add(BalanceContract.CancelAllUnfreezeV2Contract.getDefaultInstance()
        .getDescriptorForType());
    PRINTER = JsonFormat.printer().usingTypeRegistry(registry.build()).sortingMapKeys();
  }

  /**
   * get bytes data from string data.
   */
  public static byte[] fromString(String s) {
    return StringUtils.isBlank(s) ? null : s.getBytes();
  }

  /**
   * get string data from bytes data.
   */
  public static String toStr(byte[] b) {
    return ArrayUtils.isEmpty(b) ? null : new String(b);
  }

  public static byte[] fromLong(long val) {
    return Longs.toByteArray(val);
  }

  /**
   * get long data from bytes data.
   */
  public static long toLong(byte[] b) {
    return ArrayUtils.isEmpty(b) ? 0 : new BigInteger(1, b).longValue();
  }

  public static byte[] fromInt(int val) {
    return Ints.toByteArray(val);
  }

  /**
   * get int data from bytes data.
   */
  public static int toInt(byte[] b) {
    return ArrayUtils.isEmpty(b) ? 0 : new BigInteger(1, b).intValue();
  }

  public static int compareUnsigned(byte[] a, byte[] b) {
    if (a == b) {
      return 0;
    }
    if (a == null) {
      return -1;
    }
    if (b == null) {
      return 1;
    }
    int minLen = StrictMath.min(a.length, b.length);
    for (int i = 0; i < minLen; ++i) {
      int aVal = a[i] & 0xFF;
      int bVal = b[i] & 0xFF;
      if (aVal < bVal) {
        return -1;
      }
      if (aVal > bVal) {
        return 1;
      }
    }
    if (a.length < b.length) {
      return -1;
    }
    if (a.length > b.length) {
      return 1;
    }
    return 0;
  }

  public static String toHexString(byte[] data) {
    return data == null ? "" : Hex.toHexString(data);
  }

  /**
   * get bytes data from hex string data.
   */
  public static byte[] fromHexString(String data) {
    if (data == null) {
      return EMPTY_BYTE_ARRAY;
    }
    if (data.startsWith("0x")) {
      data = data.substring(2);
    }
    if (data.length() % 2 != 0) {
      data = "0" + data;
    }
    return Hex.decode(data);
  }

  public static Protocol.Block toBlock(byte[] b) {
    try {
      return Protocol.Block.parseFrom(b);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }
  }

  public static Protocol.Account toAccount(byte[] b) {
    try {
      return Protocol.Account.parseFrom(b);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }
  }

  public static Protocol.Exchange toExchange(byte[] b) {
    try {
      return Protocol.Exchange.parseFrom(b);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }
  }

  public static String toJson(MessageOrBuilder messageOrBuilder) {
    try {
      return PRINTER.print(messageOrBuilder);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }
  }
}