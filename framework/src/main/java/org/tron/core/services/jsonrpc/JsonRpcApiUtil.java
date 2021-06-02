package org.tron.core.services.jsonrpc;

import com.google.common.base.Throwables;
import com.google.common.primitives.Longs;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.StringUtil;
import org.tron.core.Wallet;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.exception.TronException;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.AccountContract.AccountCreateContract;
import org.tron.protos.contract.AccountContract.AccountPermissionUpdateContract;
import org.tron.protos.contract.AccountContract.AccountUpdateContract;
import org.tron.protos.contract.AccountContract.SetAccountIdContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract.FrozenSupply;
import org.tron.protos.contract.AssetIssueContractOuterClass.ParticipateAssetIssueContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.TransferAssetContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.UnfreezeAssetContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.UpdateAssetContract;
import org.tron.protos.contract.BalanceContract.FreezeBalanceContract;
import org.tron.protos.contract.BalanceContract.TransferContract;
import org.tron.protos.contract.BalanceContract.UnfreezeBalanceContract;
import org.tron.protos.contract.BalanceContract.WithdrawBalanceContract;
import org.tron.protos.contract.ExchangeContract.ExchangeCreateContract;
import org.tron.protos.contract.ExchangeContract.ExchangeInjectContract;
import org.tron.protos.contract.ExchangeContract.ExchangeTransactionContract;
import org.tron.protos.contract.ExchangeContract.ExchangeWithdrawContract;
import org.tron.protos.contract.MarketContract.MarketCancelOrderContract;
import org.tron.protos.contract.MarketContract.MarketSellAssetContract;
import org.tron.protos.contract.ProposalContract.ProposalApproveContract;
import org.tron.protos.contract.ProposalContract.ProposalCreateContract;
import org.tron.protos.contract.ProposalContract.ProposalDeleteContract;
import org.tron.protos.contract.ShieldContract.ShieldedTransferContract;
import org.tron.protos.contract.SmartContractOuterClass.ClearABIContract;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.UpdateEnergyLimitContract;
import org.tron.protos.contract.SmartContractOuterClass.UpdateSettingContract;
import org.tron.protos.contract.StorageContract.UpdateBrokerageContract;
import org.tron.protos.contract.VoteAssetContractOuterClass.VoteAssetContract;
import org.tron.protos.contract.WitnessContract.VoteWitnessContract;
import org.tron.protos.contract.WitnessContract.VoteWitnessContract.Vote;
import org.tron.protos.contract.WitnessContract.WitnessCreateContract;
import org.tron.protos.contract.WitnessContract.WitnessUpdateContract;

@Slf4j(topic = "API")
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

  public static byte[] convertToTronAddress(byte[] address) {
    byte[] newAddress = new byte[21];
    byte[] temp = new byte[] {Wallet.getAddressPreFixByte()};
    System.arraycopy(temp, 0, newAddress, 0, temp.length);

    if (address.length <= 20) {
      int start = 20 - address.length;
      System.arraycopy(address, 0, newAddress, temp.length + start, address.length);
    } else {
      int start = address.length - 20;
      System.arraycopy(address, start, newAddress, temp.length, 20);
    }
    return newAddress;
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

  public static String getBlockID(Block block) {
    long blockNum = block.getBlockHeader().getRawData().getNumber();
    byte[] blockHash = Sha256Hash.of(true, block.getBlockHeader().getRawData().toByteArray())
        .getByteString().toByteArray();
    byte[] numBytes = Longs.toByteArray(blockNum);
    byte[] hash = new byte[blockHash.length];
    System.arraycopy(numBytes, 0, hash, 0, 8);
    System.arraycopy(blockHash, 8, hash, 8, blockHash.length - 8);
    return "0x" + ByteArray.toHexString(hash);
  }

  public static byte[] getOwner(Transaction.Contract contract) {
    ByteString owner = null;
    try {
      Any contractParameter = contract.getParameter();
      switch (contract.getType()) {
        case AccountCreateContract:
          owner = contractParameter.unpack(AccountCreateContract.class).getOwnerAddress();
          break;
        case AccountUpdateContract:
          owner = contractParameter.unpack(AccountUpdateContract.class).getOwnerAddress();
          break;
        case SetAccountIdContract:
          owner = contractParameter.unpack(SetAccountIdContract.class).getOwnerAddress();
          break;
        case TransferContract:
          owner = contractParameter.unpack(TransferContract.class).getOwnerAddress();
          break;
        case TransferAssetContract:
          owner = contractParameter.unpack(TransferAssetContract.class).getOwnerAddress();
          break;
        case VoteAssetContract:
          owner = contractParameter.unpack(VoteAssetContract.class).getOwnerAddress();
          break;
        case VoteWitnessContract:
          owner = contractParameter.unpack(VoteWitnessContract.class).getOwnerAddress();
          break;
        case WitnessCreateContract:
          owner = contractParameter.unpack(WitnessCreateContract.class).getOwnerAddress();
          break;
        case AssetIssueContract:
          owner = contractParameter.unpack(AssetIssueContract.class).getOwnerAddress();
          break;
        case WitnessUpdateContract:
          owner = contractParameter.unpack(WitnessUpdateContract.class).getOwnerAddress();
          break;
        case ParticipateAssetIssueContract:
          owner = contractParameter.unpack(ParticipateAssetIssueContract.class).getOwnerAddress();
          break;
        case FreezeBalanceContract:
          owner = contractParameter.unpack(FreezeBalanceContract.class).getOwnerAddress();
          break;
        case UnfreezeBalanceContract:
          owner = contractParameter.unpack(UnfreezeBalanceContract.class).getOwnerAddress();
          break;
        case UnfreezeAssetContract:
          owner = contractParameter.unpack(UnfreezeAssetContract.class).getOwnerAddress();
          break;
        case WithdrawBalanceContract:
          owner = contractParameter.unpack(WithdrawBalanceContract.class).getOwnerAddress();
          break;
        case CreateSmartContract:
          owner = contractParameter.unpack(CreateSmartContract.class).getOwnerAddress();
          break;
        case TriggerSmartContract:
          owner = contractParameter.unpack(TriggerSmartContract.class).getOwnerAddress();
          break;
        case UpdateAssetContract:
          owner = contractParameter.unpack(UpdateAssetContract.class).getOwnerAddress();
          break;
        case ProposalCreateContract:
          owner = contractParameter.unpack(ProposalCreateContract.class).getOwnerAddress();
          break;
        case ProposalApproveContract:
          owner = contractParameter.unpack(ProposalApproveContract.class).getOwnerAddress();
          break;
        case ProposalDeleteContract:
          owner = contractParameter.unpack(ProposalDeleteContract.class).getOwnerAddress();
          break;
        // case BuyStorageContract:
        //   owner = contractParameter.unpack(BuyStorageContract.class).getOwnerAddress();
        //   break;
        // case BuyStorageBytesContract:
        //   owner = contractParameter.unpack(BuyStorageBytesContract.class).getOwnerAddress();
        //   break;
        // case SellStorageContract:
        //   owner = contractParameter.unpack(SellStorageContract.class).getOwnerAddress();
        //   break;
        case UpdateSettingContract:
          owner = contractParameter.unpack(UpdateSettingContract.class)
              .getOwnerAddress();
          break;
        case ExchangeCreateContract:
          owner = contractParameter.unpack(ExchangeCreateContract.class).getOwnerAddress();
          break;
        case ExchangeInjectContract:
          owner = contractParameter.unpack(ExchangeInjectContract.class).getOwnerAddress();
          break;
        case ExchangeWithdrawContract:
          owner = contractParameter.unpack(ExchangeWithdrawContract.class).getOwnerAddress();
          break;
        case ExchangeTransactionContract:
          owner = contractParameter.unpack(ExchangeTransactionContract.class).getOwnerAddress();
          break;
        case UpdateEnergyLimitContract:
          owner = contractParameter.unpack(UpdateEnergyLimitContract.class).getOwnerAddress();
          break;
        case AccountPermissionUpdateContract:
          owner = contractParameter.unpack(AccountPermissionUpdateContract.class).getOwnerAddress();
          break;
        case ClearABIContract:
          owner = contractParameter.unpack(ClearABIContract.class).getOwnerAddress();
          break;
        case UpdateBrokerageContract:
          owner = contractParameter.unpack(UpdateBrokerageContract.class)
              .getOwnerAddress();
          break;
        case ShieldedTransferContract:
          owner = contractParameter.unpack(ShieldedTransferContract.class)
              .getTransparentFromAddress();
          break;
        case MarketSellAssetContract:
          owner = contractParameter.unpack(MarketSellAssetContract.class)
              .getOwnerAddress();
          break;
        case MarketCancelOrderContract:
          owner = contractParameter.unpack(MarketCancelOrderContract.class)
              .getOwnerAddress();
          break;
        default:
          return null;
      }
      return owner.toByteArray();
    } catch (Exception ex) {
      ex.printStackTrace();
    } catch (Throwable t) {
      //捕获unpack抛出的java.lang.NoClassDefFoundError，否则线程中断。上一个不能捕获
      t.printStackTrace();
    }

    return owner == null ? null : owner.toByteArray();
  }

  public static byte[] getToAddress(Transaction transaction) {
    List<ByteString> toAddressList = getTo(transaction);
    if (!toAddressList.isEmpty()) {
      return toAddressList.get(0).toByteArray();
    } else {
      return null;
    }
  }

  public static List<ByteString> getTo(Transaction transaction) {
    Transaction.Contract contract = transaction.getRawData().getContract(0);
    List<ByteString> list = new ArrayList<>();
    try {
      Any contractParameter = contract.getParameter();
      switch (contract.getType()) {
        case AccountCreateContract:
          list.add(contractParameter.unpack(AccountCreateContract.class).getAccountAddress());
          break;
        case AccountUpdateContract:
          break;
        case SetAccountIdContract:
          break;
        case TransferContract:
          list.add(contractParameter.unpack(TransferContract.class).getToAddress());
          break;
        case TransferAssetContract:
          list.add(contractParameter.unpack(TransferAssetContract.class).getToAddress());
          break;
        case VoteAssetContract:
          list.addAll(contractParameter.unpack(VoteAssetContract.class).getVoteAddressList());
          break;
        case VoteWitnessContract:
          for (Vote vote : contractParameter.unpack(VoteWitnessContract.class).getVotesList()) {
            list.add(vote.getVoteAddress());
          }
          break;
        case WitnessCreateContract:
          break;
        case AssetIssueContract:
          break;
        case WitnessUpdateContract:
          break;
        case ParticipateAssetIssueContract:
          list.add(contractParameter.unpack(ParticipateAssetIssueContract.class).getToAddress());
          break;
        case FreezeBalanceContract:
          ByteString receiverAddress = contractParameter.unpack(FreezeBalanceContract.class)
              .getReceiverAddress();
          if (receiverAddress != null && !receiverAddress.isEmpty()) {
            list.add(receiverAddress);
          }
          break;
        case UnfreezeBalanceContract:
          receiverAddress = contractParameter.unpack(UnfreezeBalanceContract.class)
              .getReceiverAddress();
          if (receiverAddress != null && !receiverAddress.isEmpty()) {
            list.add(receiverAddress);
          }
          break;
        case UnfreezeAssetContract:
          break;
        case WithdrawBalanceContract:
          break;
        case CreateSmartContract:
          list.add(ByteString.copyFrom(generateContractAddress(transaction)));
          break;
        case TriggerSmartContract:
          list.add(contractParameter.unpack(TriggerSmartContract.class).getContractAddress());
          break;
        case UpdateAssetContract:
          break;
        case ProposalCreateContract:
          break;
        case ProposalApproveContract:
          break;
        case ProposalDeleteContract:
          break;
        // case BuyStorageContract:
        //   owner = contractParameter.unpack(BuyStorageContract.class).getOwnerAddress();
        //   break;
        // case BuyStorageBytesContract:
        //   owner = contractParameter.unpack(BuyStorageBytesContract.class).getOwnerAddress();
        //   break;
        // case SellStorageContract:
        //   owner = contractParameter.unpack(SellStorageContract.class).getOwnerAddress();
        //   break;
        case UpdateSettingContract:
          list.add(contractParameter.unpack(UpdateSettingContract.class).getContractAddress());
          break;
        case ExchangeCreateContract:
          break;
        case ExchangeInjectContract:
          break;
        case ExchangeWithdrawContract:
          break;
        case ExchangeTransactionContract:
          break;
        case UpdateEnergyLimitContract:
          list.add(contractParameter.unpack(UpdateEnergyLimitContract.class).getContractAddress());
          break;
        case AccountPermissionUpdateContract:
          break;
        case ClearABIContract:
          list.add(contractParameter.unpack(ClearABIContract.class).getContractAddress());
          break;
        case UpdateBrokerageContract:
          break;
        case ShieldedTransferContract:
          ShieldedTransferContract shieldedTransferContract = contract.getParameter()
              .unpack(ShieldedTransferContract.class);
          if (!shieldedTransferContract.getTransparentToAddress().isEmpty()) {
            list.add(shieldedTransferContract.getTransparentToAddress());
          }
          break;
        default:
          break;
      }
      return list;
    } catch (Exception ex) {
      ex.printStackTrace();
    } catch (Throwable t) {
      t.printStackTrace();
    }
    return list;
  }

  public static byte[] generateContractAddress(Transaction trx) {

    CreateSmartContract contract = ContractCapsule.getSmartContractFromTransaction(trx);
    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();

    byte[] txRawDataHash = Sha256Hash.hash(true, trx.getRawData().toByteArray());

    byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
    System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
    System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);

    return Hash.sha3omit12(combined);
  }

  public static String getTxID(Transaction transaction) {
    return ByteArray.toHexString(Sha256Hash.hash(true, transaction.getRawData().toByteArray()));
  }

  public static long getTransactionAmount(Transaction.Contract contract, String hash,
      Wallet wallet) {
    long amount = 0;
    try {
      switch (contract.getType()) {
        case UnfreezeBalanceContract:
        case WithdrawBalanceContract:
          TransactionInfo transactionInfo = wallet
              .getTransactionInfoById(ByteString.copyFrom(ByteArray.fromHexString(hash)));
          amount = getAmountFromTransactionInfo(hash, contract.getType(), transactionInfo);
          break;
        default:
          amount = getTransactionAmount(contract, hash, 0, null, wallet);
          break;
      }
    } catch (Exception e) {
      logger.error("Exception happens when get amount. Exception = [{}]",
          Throwables.getStackTraceAsString(e));
    } catch (Throwable t) {
      t.printStackTrace();
    }

    return amount;
  }

  /**
   * 获取交易的 amount
   */
  public static long getTransactionAmount(Transaction.Contract contract, String hash,
      long blockNum, TransactionInfo transactionInfo, Wallet wallet) {
    long amount = 0;
    try {
      Any contractParameter = contract.getParameter();
      switch (contract.getType()) {
        case TransferContract:
          amount = contractParameter.unpack(TransferContract.class).getAmount();
          break;
        case TransferAssetContract:
          amount = contractParameter.unpack(TransferAssetContract.class).getAmount();
          break;
        case VoteWitnessContract:
          List<Vote> votesList = contractParameter.unpack(VoteWitnessContract.class).getVotesList();
          long voteNumber = 0L;
          for (Vote vote : votesList) {
            voteNumber += vote.getVoteCount();
          }
          amount = voteNumber;
          break;
        case WitnessCreateContract:
          amount = 9999_000_000L;
          break;
        case AssetIssueContract:
        case ExchangeCreateContract:
          amount = 1024_000_000L;
          break;
        case ParticipateAssetIssueContract:
//          long token = DataImporter.getTokenID(blockNum,
//              contractParameter.unpack(ParticipateAssetIssueContract.class).getAssetName());
//          //获取token的比例，计算出10币的数量
//          long trxNum = contractParameter.unpack(ParticipateAssetIssueContract.class).getAmount();
//          Token10Entity entity = DataImporter.getTokenEntity(token);
//          long exchangeAmount = Math.multiplyExact(trxNum, entity.getNum());
//          exchangeAmount = Math.floorDiv(exchangeAmount, entity.getTrxNum());
//          amount = exchangeAmount;
          break;
        case FreezeBalanceContract:
          amount = contractParameter.unpack(FreezeBalanceContract.class).getFrozenBalance();
          break;
        case TriggerSmartContract:
          amount = contractParameter.unpack(TriggerSmartContract.class).getCallValue();
          break;
        case ExchangeInjectContract:
          amount = contractParameter.unpack(ExchangeInjectContract.class).getQuant();
          break;
        case ExchangeWithdrawContract:
          amount = contractParameter.unpack(ExchangeWithdrawContract.class).getQuant();
          break;
        case ExchangeTransactionContract:
          amount = contractParameter.unpack(ExchangeTransactionContract.class).getQuant();
          break;
        case AccountPermissionUpdateContract:
          amount = 100_000_000L;
          break;
        case ShieldedTransferContract:
          ShieldedTransferContract shieldedTransferContract = contract.getParameter()
              .unpack(ShieldedTransferContract.class);
          //from_amount 和 to_amount 不可能同时大于0
          if (shieldedTransferContract.getFromAmount() > 0L) {
            amount = shieldedTransferContract.getFromAmount();
          } else if (shieldedTransferContract.getToAmount() > 0L) {
            amount = shieldedTransferContract.getToAmount();
          }
          break;
        case UnfreezeBalanceContract:
        case WithdrawBalanceContract:
          amount = getAmountFromTransactionInfo(hash, contract.getType(), transactionInfo);
          break;
        case UnfreezeAssetContract:
          amount = getUnfreezeAssetAmount(contractParameter.unpack(UnfreezeAssetContract.class)
              .getOwnerAddress().toByteArray(), wallet);
          break;
        default:
      }
    } catch (Exception e) {
      logger.error("Exception happens when get amount. Exception = [{}]",
          Throwables.getStackTraceAsString(e));
    } catch (Throwable t) {
      t.printStackTrace();
    }
    return amount;
  }

  public static long getAmountFromTransactionInfo(String hash, ContractType contractType,
      TransactionInfo transactionInfo) {
    long amount = 0L;
    try {

      if (transactionInfo != null) {

        switch (contractType) {
          case UnfreezeBalanceContract:
            amount = transactionInfo.getUnfreezeAmount();
            break;
          case WithdrawBalanceContract:
            amount = transactionInfo.getWithdrawAmount();
            break;
          case ExchangeInjectContract:
            amount = transactionInfo.getExchangeInjectAnotherAmount();
            break;
          case ExchangeWithdrawContract:
            amount = transactionInfo.getExchangeWithdrawAnotherAmount();
            break;
          case ExchangeTransactionContract:
            amount = transactionInfo.getExchangeReceivedAmount();
            break;
          default:
            break;
        }
      } else {
        logger.error("Can't find transaction {} ", hash);
      }
    } catch (Exception e) {
      logger.warn("Exception happens when get amount from transactionInfo. Exception = [{}]",
          Throwables.getStackTraceAsString(e));
    } catch (Throwable t) {
      t.printStackTrace();
    }
    return amount;
  }

  public static long getUnfreezeAssetAmount(byte[] addressBytes, Wallet wallet) {
    long amount = 0L;
    try {
      if (addressBytes == null) {
        return amount;
      }

      AssetIssueList assetIssueList = wallet
          .getAssetIssueByAccount(ByteString.copyFrom(addressBytes));
      if (assetIssueList != null) {
        if (assetIssueList.getAssetIssueCount() != 1) {
          return amount;
        } else {
          AssetIssueContract assetIssue = assetIssueList.getAssetIssue(0);
          Iterator<FrozenSupply> iterator = assetIssue.getFrozenSupplyList().iterator();
          while (iterator.hasNext()) {
            FrozenSupply frozenSupply = iterator.next();
            amount += frozenSupply.getFrozenAmount();
          }
        }
      }
    } catch (Exception e) {
      logger.warn("Exception happens when get token10 frozenAmount. Exception = [{}]",
          Throwables.getStackTraceAsString(e));
    }
    return amount;
  }

  public static String int2HexString(int i) {
    return "0x" + Integer.toUnsignedString(i, 16);
  }

  public static String long2HexString(long l) {
    return "0x" + Long.toUnsignedString(l, 16);
  }
}
