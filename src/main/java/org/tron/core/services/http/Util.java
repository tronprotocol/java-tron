package org.tron.core.services.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.math.BigDecimal;
import java.security.InvalidParameterException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.util.StringUtil;
import org.pf4j.util.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI.BlockList;
import org.tron.api.GrpcAPI.EasyTransferResponse;
import org.tron.api.GrpcAPI.TransactionApprovedList;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.GrpcAPI.TransactionList;
import org.tron.api.GrpcAPI.TransactionSignWeight;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.services.http.JsonFormat.ParseException;
import org.tron.protos.Contract;
import org.tron.protos.Contract.AccountCreateContract;
import org.tron.protos.Contract.AccountPermissionUpdateContract;
import org.tron.protos.Contract.AccountUpdateContract;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Contract.CreateSmartContract;
import org.tron.protos.Contract.ExchangeCreateContract;
import org.tron.protos.Contract.ExchangeInjectContract;
import org.tron.protos.Contract.ExchangeTransactionContract;
import org.tron.protos.Contract.ExchangeWithdrawContract;
import org.tron.protos.Contract.FreezeBalanceContract;
import org.tron.protos.Contract.ParticipateAssetIssueContract;
import org.tron.protos.Contract.ProposalApproveContract;
import org.tron.protos.Contract.ProposalCreateContract;
import org.tron.protos.Contract.ProposalDeleteContract;
import org.tron.protos.Contract.TransferAssetContract;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Contract.TriggerSmartContract;
import org.tron.protos.Contract.UnfreezeAssetContract;
import org.tron.protos.Contract.UnfreezeBalanceContract;
import org.tron.protos.Contract.UpdateAssetContract;
import org.tron.protos.Contract.UpdateEnergyLimitContract;
import org.tron.protos.Contract.UpdateSettingContract;
import org.tron.protos.Contract.VoteAssetContract;
import org.tron.protos.Contract.VoteWitnessContract;
import org.tron.protos.Contract.WithdrawBalanceContract;
import org.tron.protos.Contract.WitnessCreateContract;
import org.tron.protos.Contract.WitnessUpdateContract;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;

@Slf4j(topic = "API")
public class Util {

  public static final String PERMISSION_ID = "Permission_id";
  public static final String VISIBLE = "visible";
  public static final String TRANSACTION = "transaction";
  public static final String VALUE = "value";

  public static String printErrorMsg(Exception e) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("Error", e.getClass() + " : " + e.getMessage());
    return jsonObject.toJSONString();
  }

  public static String printBlockList(BlockList list, boolean selfType) {
    List<Block> blocks = list.getBlockList();
    JSONObject jsonObject = JSONObject.parseObject(JsonFormat.printToString(list, selfType));
    JSONArray jsonArray = new JSONArray();
    blocks.stream().forEach(block -> {
      jsonArray.add(printBlockToJSON(block, selfType));
    });
    jsonObject.put("block", jsonArray);

    return jsonObject.toJSONString();
  }

  public static String printBlock(Block block, boolean selfType) {
    return printBlockToJSON(block, selfType).toJSONString();
  }

  public static JSONObject printBlockToJSON(Block block, boolean selfType) {
    BlockCapsule blockCapsule = new BlockCapsule(block);
    String blockID = ByteArray.toHexString(blockCapsule.getBlockId().getBytes());
    JSONObject jsonObject = JSONObject.parseObject(JsonFormat.printToString(block, selfType));
    jsonObject.put("blockID", blockID);
    if (!blockCapsule.getTransactions().isEmpty()) {
      jsonObject.put("transactions", printTransactionListToJSON(blockCapsule.getTransactions(),
          selfType));
    }
    return jsonObject;
  }

  public static String printTransactionList(TransactionList list, boolean selfType) {
    List<Transaction> transactions = list.getTransactionList();
    JSONObject jsonObject = JSONObject.parseObject(JsonFormat.printToString(list, selfType));
    JSONArray jsonArray = new JSONArray();
    transactions.stream().forEach(transaction -> {
      jsonArray.add(printTransactionToJSON(transaction, selfType));
    });
    jsonObject.put(TRANSACTION, jsonArray);

    return jsonObject.toJSONString();
  }

  public static JSONArray printTransactionListToJSON(List<TransactionCapsule> list,
      boolean selfType) {
    JSONArray transactions = new JSONArray();
    list.stream().forEach(transactionCapsule -> {
      transactions.add(printTransactionToJSON(transactionCapsule.getInstance(), selfType));
    });
    return transactions;
  }

  public static String printEasyTransferResponse(EasyTransferResponse response, boolean selfType) {
    JSONObject jsonResponse = JSONObject.parseObject(JsonFormat.printToString(response, selfType));
    jsonResponse.put(TRANSACTION, printTransactionToJSON(response.getTransaction(), selfType));
    return jsonResponse.toJSONString();
  }

  public static String printTransaction(Transaction transaction, boolean selfType) {
    return printTransactionToJSON(transaction, selfType).toJSONString();
  }

  public static String printCreateTransaction(Transaction transaction, boolean selfType) {
    JSONObject jsonObject = printTransactionToJSON(transaction, selfType);
    jsonObject.put(VISIBLE, selfType);
    return jsonObject.toJSONString();
  }

  public static String printTransactionExtention(TransactionExtention transactionExtention,
      boolean selfType) {
    String string = JsonFormat.printToString(transactionExtention, selfType);
    JSONObject jsonObject = JSONObject.parseObject(string);
    if (transactionExtention.getResult().getResult()) {
      JSONObject transactionOjbect = printTransactionToJSON(
          transactionExtention.getTransaction(), selfType);
      transactionOjbect.put(VISIBLE, selfType);
      jsonObject.put(TRANSACTION, transactionOjbect);
    }
    return jsonObject.toJSONString();
  }

  public static String printTransactionSignWeight(TransactionSignWeight transactionSignWeight,
      boolean selfType) {
    String string = JsonFormat.printToString(transactionSignWeight, selfType);
    JSONObject jsonObject = JSONObject.parseObject(string);
    JSONObject jsonObjectExt = jsonObject.getJSONObject(TRANSACTION);
    jsonObjectExt
        .put(TRANSACTION,
            printTransactionToJSON(transactionSignWeight.getTransaction().getTransaction(),
                selfType));
    jsonObject.put(TRANSACTION, jsonObjectExt);
    return jsonObject.toJSONString();
  }

  public static String printTransactionApprovedList(
      TransactionApprovedList transactionApprovedList, boolean selfType) {
    String string = JsonFormat.printToString(transactionApprovedList, selfType);
    JSONObject jsonObject = JSONObject.parseObject(string);
    JSONObject jsonObjectExt = jsonObject.getJSONObject(TRANSACTION);
    jsonObjectExt.put(TRANSACTION,
        printTransactionToJSON(transactionApprovedList.getTransaction().getTransaction(),
            selfType));
    jsonObject.put(TRANSACTION, jsonObjectExt);
    return jsonObject.toJSONString();
  }

  public static byte[] generateContractAddress(Transaction trx, byte[] ownerAddress) {
    // get tx hash
    byte[] txRawDataHash = Sha256Hash.of(trx.getRawData().toByteArray()).getBytes();

    // combine
    byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
    System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
    System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);

    return Hash.sha3omit12(combined);
  }

  public static JSONObject printTransactionToJSON(Transaction transaction, boolean selfType) {
    JSONObject jsonTransaction = JSONObject.parseObject(JsonFormat.printToString(transaction,
        selfType));
    JSONArray contracts = new JSONArray();
    transaction.getRawData().getContractList().stream().forEach(contract -> {
      try {
        JSONObject contractJson = null;
        Any contractParameter = contract.getParameter();
        switch (contract.getType()) {
          case AccountCreateContract:
            AccountCreateContract accountCreateContract = contractParameter
                .unpack(AccountCreateContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(accountCreateContract,
                selfType));
            break;
          case TransferContract:
            TransferContract transferContract = contractParameter.unpack(TransferContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(transferContract,
                selfType));
            break;
          case TransferAssetContract:
            TransferAssetContract transferAssetContract = contractParameter
                .unpack(TransferAssetContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(transferAssetContract,
                selfType));
            break;
          case VoteAssetContract:
            VoteAssetContract voteAssetContract = contractParameter.unpack(VoteAssetContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(voteAssetContract,
                selfType));
            break;
          case VoteWitnessContract:
            VoteWitnessContract voteWitnessContract = contractParameter
                .unpack(VoteWitnessContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(voteWitnessContract,
                selfType));
            break;
          case WitnessCreateContract:
            WitnessCreateContract witnessCreateContract = contractParameter
                .unpack(WitnessCreateContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(witnessCreateContract,
                selfType));
            break;
          case AssetIssueContract:
            AssetIssueContract assetIssueContract = contractParameter
                .unpack(AssetIssueContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(assetIssueContract,
                selfType));
            break;
          case WitnessUpdateContract:
            WitnessUpdateContract witnessUpdateContract = contractParameter
                .unpack(WitnessUpdateContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(witnessUpdateContract,
                selfType));
            break;
          case ParticipateAssetIssueContract:
            ParticipateAssetIssueContract participateAssetIssueContract = contractParameter
                .unpack(ParticipateAssetIssueContract.class);
            contractJson = JSONObject
                .parseObject(JsonFormat.printToString(participateAssetIssueContract, selfType));
            break;
          case AccountUpdateContract:
            AccountUpdateContract accountUpdateContract = contractParameter
                .unpack(AccountUpdateContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(accountUpdateContract,
                selfType));
            break;
          case FreezeBalanceContract:
            FreezeBalanceContract freezeBalanceContract = contractParameter
                .unpack(FreezeBalanceContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(freezeBalanceContract,
                selfType));
            break;
          case UnfreezeBalanceContract:
            UnfreezeBalanceContract unfreezeBalanceContract = contractParameter
                .unpack(UnfreezeBalanceContract.class);
            contractJson = JSONObject
                .parseObject(JsonFormat.printToString(unfreezeBalanceContract, selfType));
            break;
          case WithdrawBalanceContract:
            WithdrawBalanceContract withdrawBalanceContract = contractParameter
                .unpack(WithdrawBalanceContract.class);
            contractJson = JSONObject
                .parseObject(JsonFormat.printToString(withdrawBalanceContract, selfType));
            break;
          case UnfreezeAssetContract:
            UnfreezeAssetContract unfreezeAssetContract = contractParameter
                .unpack(UnfreezeAssetContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(unfreezeAssetContract,
                selfType));
            break;
          case UpdateAssetContract:
            UpdateAssetContract updateAssetContract = contractParameter
                .unpack(UpdateAssetContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(updateAssetContract,
                selfType));
            break;
          case ProposalCreateContract:
            ProposalCreateContract proposalCreateContract = contractParameter
                .unpack(ProposalCreateContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(proposalCreateContract,
                selfType));
            break;
          case ProposalApproveContract:
            ProposalApproveContract proposalApproveContract = contractParameter
                .unpack(ProposalApproveContract.class);
            contractJson = JSONObject
                .parseObject(JsonFormat.printToString(proposalApproveContract, selfType));
            break;
          case ProposalDeleteContract:
            ProposalDeleteContract proposalDeleteContract = contractParameter
                .unpack(ProposalDeleteContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(proposalDeleteContract,
                selfType));
            break;
          case SetAccountIdContract:
            Contract.SetAccountIdContract setAccountIdContract =
                contractParameter.unpack(Contract.SetAccountIdContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(setAccountIdContract,
                selfType));
            break;
          case CreateSmartContract:
            CreateSmartContract deployContract = contractParameter
                .unpack(CreateSmartContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(deployContract,
                selfType));
            byte[] ownerAddress = deployContract.getOwnerAddress().toByteArray();
            byte[] contractAddress = generateContractAddress(transaction, ownerAddress);
            jsonTransaction.put("contract_address", ByteArray.toHexString(contractAddress));
            break;
          case TriggerSmartContract:
            TriggerSmartContract triggerSmartContract = contractParameter
                .unpack(TriggerSmartContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(triggerSmartContract,
                selfType));
            break;
          case UpdateSettingContract:
            UpdateSettingContract updateSettingContract = contractParameter
                .unpack(UpdateSettingContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(updateSettingContract,
                selfType));
            break;
          case ExchangeCreateContract:
            ExchangeCreateContract exchangeCreateContract = contractParameter
                .unpack(ExchangeCreateContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(exchangeCreateContract,
                selfType));
            break;
          case ExchangeInjectContract:
            ExchangeInjectContract exchangeInjectContract = contractParameter
                .unpack(ExchangeInjectContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(exchangeInjectContract,
                selfType));
            break;
          case ExchangeWithdrawContract:
            ExchangeWithdrawContract exchangeWithdrawContract = contractParameter
                .unpack(ExchangeWithdrawContract.class);
            contractJson = JSONObject
                .parseObject(JsonFormat.printToString(exchangeWithdrawContract, selfType));
            break;
          case ExchangeTransactionContract:
            ExchangeTransactionContract exchangeTransactionContract = contractParameter
                .unpack(ExchangeTransactionContract.class);
            contractJson = JSONObject
                .parseObject(JsonFormat.printToString(exchangeTransactionContract, selfType));
            break;
          case UpdateEnergyLimitContract:
            UpdateEnergyLimitContract updateEnergyLimitContract = contractParameter
                .unpack(UpdateEnergyLimitContract.class);
            contractJson = JSONObject
                .parseObject(JsonFormat.printToString(updateEnergyLimitContract, selfType));
            break;
          case AccountPermissionUpdateContract:
            AccountPermissionUpdateContract accountPermissionUpdateContract = contractParameter
                .unpack(AccountPermissionUpdateContract.class);
            contractJson = JSONObject
                .parseObject(JsonFormat.printToString(accountPermissionUpdateContract, selfType));
            break;
          case ClearABIContract:
            Contract.ClearABIContract clearABIContract = contractParameter
                .unpack(Contract.ClearABIContract.class);
            contractJson = JSONObject
                .parseObject(JsonFormat.printToString(clearABIContract, selfType));
            break;
          // todo add other contract
          default:
        }
        JSONObject parameter = new JSONObject();
        parameter.put(VALUE, contractJson);
        parameter.put("type_url", contract.getParameterOrBuilder().getTypeUrl());
        JSONObject jsonContract = new JSONObject();
        jsonContract.put("parameter", parameter);
        jsonContract.put("type", contract.getType());
        if (contract.getPermissionId() > 0) {
          jsonContract.put(PERMISSION_ID, contract.getPermissionId());
        }
        contracts.add(jsonContract);
      } catch (InvalidProtocolBufferException e) {
        logger.debug("InvalidProtocolBufferException: {}", e.getMessage());
      }
    });

    JSONObject rawData = JSONObject.parseObject(jsonTransaction.get("raw_data").toString());
    rawData.put("contract", contracts);
    jsonTransaction.put("raw_data", rawData);
    String rawDataHex = ByteArray.toHexString(transaction.getRawData().toByteArray());
    jsonTransaction.put("raw_data_hex", rawDataHex);
    String txID = ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray()));
    jsonTransaction.put("txID", txID);
    return jsonTransaction;
  }

  public static Transaction packTransaction(String strTransaction, boolean selfType) {
    JSONObject jsonTransaction = JSONObject.parseObject(strTransaction);
    JSONObject rawData = jsonTransaction.getJSONObject("raw_data");
    JSONArray contracts = new JSONArray();
    JSONArray rawContractArray = rawData.getJSONArray("contract");

    for (int i = 0; i < rawContractArray.size(); i++) {
      try {
        JSONObject contract = rawContractArray.getJSONObject(i);
        JSONObject parameter = contract.getJSONObject("parameter");
        String contractType = contract.getString("type");
        Any any = null;
        switch (contractType) {
          case "AccountCreateContract":
            AccountCreateContract.Builder accountCreateContractBuilder = AccountCreateContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                accountCreateContractBuilder, selfType);
            any = Any.pack(accountCreateContractBuilder.build());
            break;
          case "TransferContract":
            TransferContract.Builder transferContractBuilder = TransferContract.newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                transferContractBuilder, selfType);
            any = Any.pack(transferContractBuilder.build());
            break;
          case "TransferAssetContract":
            TransferAssetContract.Builder transferAssetContractBuilder = TransferAssetContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                transferAssetContractBuilder, selfType);
            any = Any.pack(transferAssetContractBuilder.build());
            break;
          case "VoteAssetContract":
            VoteAssetContract.Builder voteAssetContractBuilder = VoteAssetContract.newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                voteAssetContractBuilder, selfType);
            any = Any.pack(voteAssetContractBuilder.build());
            break;
          case "VoteWitnessContract":
            VoteWitnessContract.Builder voteWitnessContractBuilder =
                VoteWitnessContract.newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                voteWitnessContractBuilder, selfType);
            any = Any.pack(voteWitnessContractBuilder.build());
            break;
          case "WitnessCreateContract":
            WitnessCreateContract.Builder witnessCreateContractBuilder = WitnessCreateContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                witnessCreateContractBuilder, selfType);
            any = Any.pack(witnessCreateContractBuilder.build());
            break;
          case "AssetIssueContract":
            AssetIssueContract.Builder assetIssueContractBuilder = AssetIssueContract.newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                assetIssueContractBuilder, selfType);
            any = Any.pack(assetIssueContractBuilder.build());
            break;
          case "WitnessUpdateContract":
            WitnessUpdateContract.Builder witnessUpdateContractBuilder = WitnessUpdateContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                witnessUpdateContractBuilder, selfType);
            any = Any.pack(witnessUpdateContractBuilder.build());
            break;
          case "ParticipateAssetIssueContract":
            ParticipateAssetIssueContract.Builder participateAssetIssueContractBuilder =
                ParticipateAssetIssueContract.newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                participateAssetIssueContractBuilder, selfType);
            any = Any.pack(participateAssetIssueContractBuilder.build());
            break;
          case "AccountUpdateContract":
            AccountUpdateContract.Builder accountUpdateContractBuilder = AccountUpdateContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                accountUpdateContractBuilder, selfType);
            any = Any.pack(accountUpdateContractBuilder.build());
            break;
          case "FreezeBalanceContract":
            FreezeBalanceContract.Builder freezeBalanceContractBuilder = FreezeBalanceContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                freezeBalanceContractBuilder, selfType);
            any = Any.pack(freezeBalanceContractBuilder.build());
            break;
          case "UnfreezeBalanceContract":
            UnfreezeBalanceContract.Builder unfreezeBalanceContractBuilder = UnfreezeBalanceContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                unfreezeBalanceContractBuilder, selfType);
            any = Any.pack(unfreezeBalanceContractBuilder.build());
            break;
          case "WithdrawBalanceContract":
            WithdrawBalanceContract.Builder withdrawBalanceContractBuilder = WithdrawBalanceContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                withdrawBalanceContractBuilder, selfType);
            any = Any.pack(withdrawBalanceContractBuilder.build());
            break;
          case "UnfreezeAssetContract":
            UnfreezeAssetContract.Builder unfreezeAssetContractBuilder = UnfreezeAssetContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                unfreezeAssetContractBuilder, selfType);
            any = Any.pack(unfreezeAssetContractBuilder.build());
            break;
          case "UpdateAssetContract":
            UpdateAssetContract.Builder updateAssetContractBuilder = UpdateAssetContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                updateAssetContractBuilder, selfType);
            any = Any.pack(updateAssetContractBuilder.build());
            break;
          case "ProposalCreateContract":
            ProposalCreateContract.Builder createContractBuilder = ProposalCreateContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                createContractBuilder, selfType);
            any = Any.pack(createContractBuilder.build());
            break;
          case "ProposalApproveContract":
            ProposalApproveContract.Builder approveContractBuilder = ProposalApproveContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                approveContractBuilder, selfType);
            any = Any.pack(approveContractBuilder.build());
            break;
          case "ProposalDeleteContract":
            ProposalDeleteContract.Builder deleteContractBuilder = ProposalDeleteContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                deleteContractBuilder, selfType);
            any = Any.pack(deleteContractBuilder.build());
            break;
          case "SetAccountIdContract":
            Contract.SetAccountIdContract.Builder setAccountid = Contract.SetAccountIdContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                setAccountid, selfType);
            any = Any.pack(setAccountid.build());
            break;
          case "CreateSmartContract":
            CreateSmartContract.Builder createSmartContractBuilder = CreateSmartContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                createSmartContractBuilder, selfType);
            any = Any.pack(createSmartContractBuilder.build());
            break;
          case "TriggerSmartContract":
            TriggerSmartContract.Builder triggerSmartContractBuilder = TriggerSmartContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                triggerSmartContractBuilder, selfType);
            any = Any.pack(triggerSmartContractBuilder.build());
            break;
          case "UpdateSettingContract":
            UpdateSettingContract.Builder updateSettingContractBuilder = UpdateSettingContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                updateSettingContractBuilder, selfType);
            any = Any.pack(updateSettingContractBuilder.build());
            break;
          case "ExchangeCreateContract":
            ExchangeCreateContract.Builder exchangeCreateContractBuilder = ExchangeCreateContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                exchangeCreateContractBuilder, selfType);
            any = Any.pack(exchangeCreateContractBuilder.build());
            break;
          case "ExchangeInjectContract":
            ExchangeInjectContract.Builder exchangeInjectContractBuilder = ExchangeInjectContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                exchangeInjectContractBuilder, selfType);
            any = Any.pack(exchangeInjectContractBuilder.build());
            break;
          case "ExchangeTransactionContract":
            ExchangeTransactionContract.Builder exchangeTransactionContractBuilder =
                ExchangeTransactionContract.newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                exchangeTransactionContractBuilder, selfType);
            any = Any.pack(exchangeTransactionContractBuilder.build());
            break;
          case "ExchangeWithdrawContract":
            ExchangeWithdrawContract.Builder exchangeWithdrawContractBuilder =
                ExchangeWithdrawContract.newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                exchangeWithdrawContractBuilder, selfType);
            any = Any.pack(exchangeWithdrawContractBuilder.build());
            break;
          case "UpdateEnergyLimitContract":
            UpdateEnergyLimitContract.Builder updateEnergyLimitContractBuilder =
                UpdateEnergyLimitContract.newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                updateEnergyLimitContractBuilder, selfType);
            any = Any.pack(updateEnergyLimitContractBuilder.build());
            break;
          case "AccountPermissionUpdateContract":
            AccountPermissionUpdateContract.Builder accountPermissionUpdateContractBuilder =
                AccountPermissionUpdateContract.newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                accountPermissionUpdateContractBuilder, selfType);
            any = Any.pack(accountPermissionUpdateContractBuilder.build());
            break;
          case "ClearABIContract":
            Contract.ClearABIContract.Builder clearABIContract =
                Contract.ClearABIContract.newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(), clearABIContract,
                selfType);
            any = Any.pack(clearABIContract.build());
            break;
          // todo add other contract
          default:
        }
        if (any != null) {
          String value = ByteArray.toHexString(any.getValue().toByteArray());
          parameter.put(VALUE, value);
          contract.put("parameter", parameter);
          contracts.add(contract);
        }
      } catch (ParseException e) {
        logger.debug("ParseException: {}", e.getMessage());
      }
    }
    rawData.put("contract", contracts);
    jsonTransaction.put("raw_data", rawData);
    Transaction.Builder transactionBuilder = Transaction.newBuilder();
    try {
      JsonFormat.merge(jsonTransaction.toJSONString(), transactionBuilder, selfType);
      return transactionBuilder.build();
    } catch (ParseException e) {
      logger.debug("ParseException: {}", e.getMessage());
      return null;
    }
  }

  public static void checkBodySize(String body) throws Exception {
    Args args = Args.getInstance();
    if (body.getBytes().length > args.getMaxMessageSize()) {
      throw new Exception("body size is too big, limit is " + args.getMaxMessageSize());
    }
  }

  public static boolean getVisible(final HttpServletRequest request) {
    boolean visible = false;
    if (StringUtil.isNotBlank(request.getParameter(VISIBLE))) {
      visible = Boolean.valueOf(request.getParameter(VISIBLE));
    }
    return visible;
  }

  public static boolean getVisiblePost(final String input) {
    boolean visible = false;
    JSONObject jsonObject = JSON.parseObject(input);
    if (jsonObject.containsKey(VISIBLE)) {
      visible = jsonObject.getBoolean(VISIBLE);
    }

    return visible;
  }

  public static String getHexAddress(final String address) {
    if (address != null) {
      byte[] addressByte = Wallet.decodeFromBase58Check(address);
      return ByteArray.toHexString(addressByte);
    } else {
      return null;
    }
  }

  public static String getHexString(final String string) {
    return ByteArray.toHexString(ByteString.copyFromUtf8(string).toByteArray());
  }

  public static Transaction setTransactionPermissionId(JSONObject jsonObject,
      Transaction transaction) {
    if (jsonObject.containsKey(PERMISSION_ID)) {
      int permissionId = jsonObject.getInteger(PERMISSION_ID);
      if (permissionId > 0) {
        Transaction.raw.Builder raw = transaction.getRawData().toBuilder();
        Transaction.Contract.Builder contract = raw.getContract(0).toBuilder()
            .setPermissionId(permissionId);
        raw.clearContract();
        raw.addContract(contract);
        return transaction.toBuilder().setRawData(raw).build();
      }
    }
    return transaction;
  }

  public static boolean getVisibleOnlyForSign(JSONObject jsonObject) {
    boolean visible = false;
    if (jsonObject.containsKey(VISIBLE)) {
      visible = jsonObject.getBoolean(VISIBLE);
    } else if (jsonObject.getJSONObject(TRANSACTION).containsKey(VISIBLE)) {
      visible = jsonObject.getJSONObject(TRANSACTION).getBoolean(VISIBLE);
    }
    return visible;
  }

  public static String parseMethod(String methodSign, String input) {
    byte[] selector = new byte[4];
    System.arraycopy(Hash.sha3(methodSign.getBytes()), 0, selector, 0, 4);
    //System.out.println(methodSign + ":" + Hex.toHexString(selector));
    if (StringUtils.isNullOrEmpty(input)) {
      return Hex.toHexString(selector);
    }

    return Hex.toHexString(selector) + input;
  }

  public static long getJsonLongValue(final JSONObject jsonObject, final String key) {
    return getJsonLongValue(jsonObject, key, false);
  }

  public static long getJsonLongValue(JSONObject jsonObject, String key, boolean required) {
    BigDecimal bigDecimal = jsonObject.getBigDecimal(key);
    if (required && bigDecimal == null) {
      throw new InvalidParameterException("key [" + key + "] not exist");
    }
    return (bigDecimal == null) ? 0L : bigDecimal.longValueExact();
  }
}
