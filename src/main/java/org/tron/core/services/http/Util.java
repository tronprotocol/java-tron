package org.tron.core.services.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.util.StringUtil;
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
import org.tron.protos.Contract.CancelDeferredTransactionContract;
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
import org.tron.protos.Protocol.DeferredTransaction;
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.Transaction;

import javax.servlet.http.HttpServletRequest;


@Slf4j(topic = "API")
public class Util {

  public static String printErrorMsg(Exception e) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("Error", e.getClass() + " : " + e.getMessage());
    return jsonObject.toJSONString();
  }

  public static String printBlockList(BlockList list, boolean selfType ) {
    List<Block> blocks = list.getBlockList();
    JSONObject jsonObject = JSONObject.parseObject(JsonFormat.printToString(list, selfType));
    JSONArray jsonArray = new JSONArray();
    blocks.stream().forEach(block -> {
      jsonArray.add(printBlockToJSON(block, selfType));
    });
    jsonObject.put("block", jsonArray);

    return jsonObject.toJSONString();
  }

  public static String printBlock(Block block, boolean selfType ) {
    return printBlockToJSON(block, selfType).toJSONString();
  }

  public static JSONObject printBlockToJSON(Block block,boolean selfType ) {
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

  public static String printTransactionList(TransactionList list, boolean selfType ) {
    List<Transaction> transactions = list.getTransactionList();
    JSONObject jsonObject = JSONObject.parseObject(JsonFormat.printToString(list, selfType ));
    JSONArray jsonArray = new JSONArray();
    transactions.stream().forEach(transaction -> {
      jsonArray.add(printTransactionToJSON(transaction, selfType ));
    });
    jsonObject.put("transaction", jsonArray);

    return jsonObject.toJSONString();
  }

  public static JSONArray printTransactionListToJSON(List<TransactionCapsule> list, boolean selfType ) {
    JSONArray transactions = new JSONArray();
    list.stream().forEach(transactionCapsule -> {
      transactions.add(printTransactionToJSON(transactionCapsule.getInstance(), selfType));
    });
    return transactions;
  }

  public static String printEasyTransferResponse(EasyTransferResponse response, boolean selfType ) {
    JSONObject jsonResponse = JSONObject.parseObject(JsonFormat.printToString(response, selfType));
    jsonResponse.put("transaction", printTransactionToJSON(response.getTransaction(), selfType ));
    return jsonResponse.toJSONString();
  }

  public static String printTransaction(Transaction transaction, boolean selfType ) {
    return printTransactionToJSON(transaction, selfType).toJSONString();
  }

  public static String printTransactionExtention(TransactionExtention transactionExtention, boolean selfType ) {
    String string = JsonFormat.printToString(transactionExtention, selfType );
    JSONObject jsonObject = JSONObject.parseObject(string);
    if (transactionExtention.getResult().getResult()) {
      jsonObject.put("transaction", printTransactionToJSON(transactionExtention.getTransaction(),
       selfType       ));
    }
    return jsonObject.toJSONString();
  }

  public static String printTransactionSignWeight(TransactionSignWeight transactionSignWeight, boolean selfType ) {
    String string = JsonFormat.printToString(transactionSignWeight, selfType);
    JSONObject jsonObject = JSONObject.parseObject(string);
    JSONObject jsonObjectExt = jsonObject.getJSONObject("transaction");
    jsonObjectExt
        .put("transaction",
            printTransactionToJSON(transactionSignWeight.getTransaction().getTransaction(),
                    selfType ));
    jsonObject.put("transaction", jsonObjectExt);
    return jsonObject.toJSONString();
  }

  public static String printTransactionApprovedList(
      TransactionApprovedList transactionApprovedList, boolean selfType ) {
    String string = JsonFormat.printToString(transactionApprovedList, selfType );
    JSONObject jsonObject = JSONObject.parseObject(string);
    JSONObject jsonObjectExt = jsonObject.getJSONObject("transaction");
    jsonObjectExt
        .put("transaction",
            printTransactionToJSON(transactionApprovedList.getTransaction().getTransaction(), selfType));
    jsonObject.put("transaction", jsonObjectExt);
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

  public static JSONObject printDeferredTransactionToJSON(DeferredTransaction deferredTransaction,
                                                          boolean selfType ) {
    String string = JsonFormat.printToString(deferredTransaction, selfType );
    JSONObject jsonObject = JSONObject.parseObject(string);
    jsonObject.put("transaction", printTransactionToJSON(deferredTransaction.getTransaction(), selfType ));
    return jsonObject;
  }

  public static JSONObject printTransactionToJSON(Transaction transaction, boolean selfType ) {
    JSONObject jsonTransaction = JSONObject.parseObject(JsonFormat.printToString(transaction,
            selfType ));
    JSONArray contracts = new JSONArray();
    transaction.getRawData().getContractList().stream().forEach(contract -> {
      try {
        JSONObject contractJson = null;
        Any contractParameter = contract.getParameter();
        switch (contract.getType()) {
          case AccountCreateContract:
            AccountCreateContract accountCreateContract = contractParameter
                .unpack(AccountCreateContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(accountCreateContract, selfType));
            break;
          case TransferContract:
            TransferContract transferContract = contractParameter.unpack(TransferContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(transferContract, selfType));
            break;
          case TransferAssetContract:
            TransferAssetContract transferAssetContract = contractParameter
                .unpack(TransferAssetContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(transferAssetContract,
             selfType       ));
            break;
          case VoteAssetContract:
            VoteAssetContract voteAssetContract = contractParameter.unpack(VoteAssetContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(voteAssetContract,
                    selfType ));
            break;
          case VoteWitnessContract:
            VoteWitnessContract voteWitnessContract = contractParameter
                .unpack(VoteWitnessContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(voteWitnessContract,
                    selfType ));
            break;
          case WitnessCreateContract:
            WitnessCreateContract witnessCreateContract = contractParameter
                .unpack(WitnessCreateContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(witnessCreateContract,
             selfType       ));
            break;
          case AssetIssueContract:
            AssetIssueContract assetIssueContract = contractParameter
                .unpack(AssetIssueContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(assetIssueContract,
                    selfType ));
            break;
          case WitnessUpdateContract:
            WitnessUpdateContract witnessUpdateContract = contractParameter
                .unpack(WitnessUpdateContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(witnessUpdateContract,
             selfType       ));
            break;
          case ParticipateAssetIssueContract:
            ParticipateAssetIssueContract participateAssetIssueContract = contractParameter
                .unpack(ParticipateAssetIssueContract.class);
            contractJson = JSONObject
                .parseObject(JsonFormat.printToString(participateAssetIssueContract, selfType ));
            break;
          case AccountUpdateContract:
            AccountUpdateContract accountUpdateContract = contractParameter
                .unpack(AccountUpdateContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(accountUpdateContract,
             selfType       ));
            break;
          case FreezeBalanceContract:
            FreezeBalanceContract freezeBalanceContract = contractParameter
                .unpack(FreezeBalanceContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(freezeBalanceContract,
             selfType       ));
            break;
          case UnfreezeBalanceContract:
            UnfreezeBalanceContract unfreezeBalanceContract = contractParameter
                .unpack(UnfreezeBalanceContract.class);
            contractJson = JSONObject
                .parseObject(JsonFormat.printToString(unfreezeBalanceContract, selfType ));
            break;
          case UnfreezeAssetContract:
            UnfreezeAssetContract unfreezeAssetContract = contractParameter
                .unpack(UnfreezeAssetContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(unfreezeAssetContract,
             selfType       ));
            break;
          case WithdrawBalanceContract:
            WithdrawBalanceContract withdrawBalanceContract = contractParameter
                .unpack(WithdrawBalanceContract.class);
            contractJson = JSONObject
                .parseObject(JsonFormat.printToString(withdrawBalanceContract, selfType ));
            break;
          case UpdateAssetContract:
            UpdateAssetContract updateAssetContract = contractParameter
                .unpack(UpdateAssetContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(updateAssetContract,
                    selfType ));
            break;
          case CreateSmartContract:
            CreateSmartContract deployContract = contractParameter
                .unpack(CreateSmartContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(deployContract,
                    selfType ));
            byte[] ownerAddress = deployContract.getOwnerAddress().toByteArray();
            byte[] contractAddress = generateContractAddress(transaction, ownerAddress);
            jsonTransaction.put("contract_address", ByteArray.toHexString(contractAddress));
            break;
          case TriggerSmartContract:
            TriggerSmartContract triggerSmartContract = contractParameter
                .unpack(TriggerSmartContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(triggerSmartContract,
                    selfType ));
            break;
          case ProposalCreateContract:
            ProposalCreateContract proposalCreateContract = contractParameter
                .unpack(ProposalCreateContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(proposalCreateContract
                    , selfType ));
            break;
          case ProposalApproveContract:
            ProposalApproveContract proposalApproveContract = contractParameter
                .unpack(ProposalApproveContract.class);
            contractJson = JSONObject
                .parseObject(JsonFormat.printToString(proposalApproveContract, selfType ));
            break;
          case ProposalDeleteContract:
            ProposalDeleteContract proposalDeleteContract = contractParameter
                .unpack(ProposalDeleteContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(proposalDeleteContract
                    , selfType ));
            break;
          case SetAccountIdContract:
            Contract.SetAccountIdContract  setAccountIdContract =
                    contractParameter.unpack(Contract.SetAccountIdContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(setAccountIdContract
                    , selfType ));
            break;
          case ExchangeCreateContract:
            ExchangeCreateContract exchangeCreateContract = contractParameter
                .unpack(ExchangeCreateContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(exchangeCreateContract
                    , selfType ));
            break;
          case ExchangeInjectContract:
            ExchangeInjectContract exchangeInjectContract = contractParameter
                .unpack(ExchangeInjectContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(exchangeInjectContract
                    , selfType ));
            break;
          case ExchangeWithdrawContract:
            ExchangeWithdrawContract exchangeWithdrawContract = contractParameter
                .unpack(ExchangeWithdrawContract.class);
            contractJson = JSONObject
                .parseObject(JsonFormat.printToString(exchangeWithdrawContract, selfType ));
            break;
          case ExchangeTransactionContract:
            ExchangeTransactionContract exchangeTransactionContract = contractParameter
                .unpack(ExchangeTransactionContract.class);
            contractJson = JSONObject
                .parseObject(JsonFormat.printToString(exchangeTransactionContract, selfType ));
            break;
          case AccountPermissionUpdateContract:
            AccountPermissionUpdateContract accountPermissionUpdateContract = contractParameter
                .unpack(AccountPermissionUpdateContract.class);
            contractJson = JSONObject
                .parseObject(JsonFormat.printToString(accountPermissionUpdateContract, selfType ));
            break;
          case UpdateSettingContract:
            UpdateSettingContract updateSettingContract = contractParameter
                .unpack(UpdateSettingContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(updateSettingContract,
             selfType       ));
            break;
          case UpdateEnergyLimitContract:
            UpdateEnergyLimitContract updateEnergyLimitContract = contractParameter
                .unpack(UpdateEnergyLimitContract.class);
            contractJson = JSONObject
                .parseObject(JsonFormat.printToString(updateEnergyLimitContract, selfType ));
            break;
          case CancelDeferredTransactionContract:
            CancelDeferredTransactionContract cancelDeferredTransactionContract = contractParameter
                .unpack(CancelDeferredTransactionContract.class);
            contractJson = JSONObject
                .parseObject(JsonFormat.printToString(cancelDeferredTransactionContract,
                        selfType ));
            break;
          // todo add other contract
          default:
        }
        JSONObject parameter = new JSONObject();
        parameter.put("value", contractJson);
        parameter.put("type_url", contract.getParameterOrBuilder().getTypeUrl());
        JSONObject jsonContract = new JSONObject();
        jsonContract.put("parameter", parameter);
        jsonContract.put("type", contract.getType());
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

    if (Objects.nonNull(transaction.getRawData().getDeferredStage()) &&
        transaction.getRawData().getDeferredStage().getDelaySeconds() > 0) {
      jsonTransaction.put("delaySeconds", transaction.getRawData().getDeferredStage().getDelaySeconds());
      jsonTransaction.put("deferredStage", transaction.getRawData().getDeferredStage().getStage());
    }

    return jsonTransaction;
  }

  public static Transaction packTransaction(String strTransaction, boolean selfType ) {
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
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                accountCreateContractBuilder, selfType);
            any = Any.pack(accountCreateContractBuilder.build());
            break;
          case "TransferContract":
            TransferContract.Builder transferContractBuilder = TransferContract.newBuilder();
            JsonFormat
                .merge(parameter.getJSONObject("value").toJSONString(), transferContractBuilder, selfType);
            any = Any.pack(transferContractBuilder.build());
            break;
          case "TransferAssetContract":
            TransferAssetContract.Builder transferAssetContractBuilder = TransferAssetContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                transferAssetContractBuilder, selfType);
            any = Any.pack(transferAssetContractBuilder.build());
            break;
          case "VoteAssetContract":
            VoteAssetContract.Builder voteAssetContractBuilder = VoteAssetContract.newBuilder();
            JsonFormat
                .merge(parameter.getJSONObject("value").toJSONString(), voteAssetContractBuilder, selfType);
            any = Any.pack(voteAssetContractBuilder.build());
            break;
          case "VoteWitnessContract":
            VoteWitnessContract.Builder voteWitnessContractBuilder = VoteWitnessContract
                .newBuilder();
            JsonFormat
                .merge(parameter.getJSONObject("value").toJSONString(), voteWitnessContractBuilder, selfType);
            any = Any.pack(voteWitnessContractBuilder.build());
            break;
          case "WitnessCreateContract":
            WitnessCreateContract.Builder witnessCreateContractBuilder = WitnessCreateContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                witnessCreateContractBuilder, selfType);
            any = Any.pack(witnessCreateContractBuilder.build());
            break;
          case "AssetIssueContract":
            AssetIssueContract.Builder assetIssueContractBuilder = AssetIssueContract.newBuilder();
            JsonFormat
                .merge(parameter.getJSONObject("value").toJSONString(), assetIssueContractBuilder, selfType);
            any = Any.pack(assetIssueContractBuilder.build());
            break;
          case "WitnessUpdateContract":
            WitnessUpdateContract.Builder witnessUpdateContractBuilder = WitnessUpdateContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                witnessUpdateContractBuilder, selfType);
            any = Any.pack(witnessUpdateContractBuilder.build());
            break;
          case "ParticipateAssetIssueContract":
            ParticipateAssetIssueContract.Builder participateAssetIssueContractBuilder =
                ParticipateAssetIssueContract.newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                participateAssetIssueContractBuilder, selfType);
            any = Any.pack(participateAssetIssueContractBuilder.build());
            break;
          case "AccountUpdateContract":
            AccountUpdateContract.Builder accountUpdateContractBuilder = AccountUpdateContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                accountUpdateContractBuilder, selfType);
            any = Any.pack(accountUpdateContractBuilder.build());
            break;
          case "FreezeBalanceContract":
            FreezeBalanceContract.Builder freezeBalanceContractBuilder = FreezeBalanceContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                freezeBalanceContractBuilder, selfType);
            any = Any.pack(freezeBalanceContractBuilder.build());
            break;
          case "UnfreezeBalanceContract":
            UnfreezeBalanceContract.Builder unfreezeBalanceContractBuilder = UnfreezeBalanceContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                unfreezeBalanceContractBuilder, selfType);
            any = Any.pack(unfreezeBalanceContractBuilder.build());
            break;
          case "UnfreezeAssetContract":
            UnfreezeAssetContract.Builder unfreezeAssetContractBuilder = UnfreezeAssetContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                unfreezeAssetContractBuilder, selfType);
            any = Any.pack(unfreezeAssetContractBuilder.build());
            break;
          case "WithdrawBalanceContract":
            WithdrawBalanceContract.Builder withdrawBalanceContractBuilder = WithdrawBalanceContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                withdrawBalanceContractBuilder, selfType);
            any = Any.pack(withdrawBalanceContractBuilder.build());
            break;
          case "UpdateAssetContract":
            UpdateAssetContract.Builder updateAssetContractBuilder = UpdateAssetContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(), updateAssetContractBuilder, selfType);
            any = Any.pack(updateAssetContractBuilder.build());
            break;
          case "SmartContract":
            SmartContract.Builder smartContractBuilder = SmartContract.newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(), smartContractBuilder, selfType);
            any = Any.pack(smartContractBuilder.build());
            break;
          case "TriggerSmartContract":
            TriggerSmartContract.Builder triggerSmartContractBuilder = TriggerSmartContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                    triggerSmartContractBuilder, selfType);
            any = Any.pack(triggerSmartContractBuilder.build());
            break;
          case "CreateSmartContract":
            CreateSmartContract.Builder createSmartContractBuilder = CreateSmartContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                    createSmartContractBuilder, selfType);
            any = Any.pack(createSmartContractBuilder.build());
            break;
          case "ExchangeCreateContract":
            ExchangeCreateContract.Builder exchangeCreateContractBuilder = ExchangeCreateContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                    exchangeCreateContractBuilder, selfType);
            any = Any.pack(exchangeCreateContractBuilder.build());
            break;
          case "ExchangeInjectContract":
            ExchangeInjectContract.Builder exchangeInjectContractBuilder = ExchangeInjectContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                    exchangeInjectContractBuilder, selfType);
            any = Any.pack(exchangeInjectContractBuilder.build());
            break;
          case "ExchangeTransactionContract":
            ExchangeTransactionContract.Builder exchangeTransactionContractBuilder =
                ExchangeTransactionContract.newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                    exchangeTransactionContractBuilder, selfType);
            any = Any.pack(exchangeTransactionContractBuilder.build());
            break;
          case "ExchangeWithdrawContract":
            ExchangeWithdrawContract.Builder exchangeWithdrawContractBuilder =
                ExchangeWithdrawContract.newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                    exchangeWithdrawContractBuilder, selfType);
            any = Any.pack(exchangeWithdrawContractBuilder.build());
            break;
          case "ProposalCreateContract":
            ProposalCreateContract.Builder ProposalCreateContractBuilder = ProposalCreateContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                    ProposalCreateContractBuilder, selfType);
            any = Any.pack(ProposalCreateContractBuilder.build());
            break;
          case "ProposalApproveContract":
            ProposalApproveContract.Builder ProposalApproveContractBuilder = ProposalApproveContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                    ProposalApproveContractBuilder, selfType);
            any = Any.pack(ProposalApproveContractBuilder.build());
            break;
          case "ProposalDeleteContract":
            ProposalDeleteContract.Builder ProposalDeleteContractBuilder = ProposalDeleteContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                    ProposalDeleteContractBuilder, selfType);
            any = Any.pack(ProposalDeleteContractBuilder.build());
            break;
          case "AccountPermissionUpdateContract":
            AccountPermissionUpdateContract.Builder AccountPermissionUpdateContractBuilder =
                AccountPermissionUpdateContract.newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                    AccountPermissionUpdateContractBuilder, selfType);
            any = Any.pack(AccountPermissionUpdateContractBuilder.build());
            break;
          case "UpdateSettingContract":
            UpdateSettingContract.Builder UpdateSettingContractBuilder = UpdateSettingContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                    UpdateSettingContractBuilder, selfType);
            any = Any.pack(UpdateSettingContractBuilder.build());
            break;
          case "UpdateEnergyLimitContract":
            UpdateEnergyLimitContract.Builder UpdateEnergyLimitContractBuilder = UpdateEnergyLimitContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                    UpdateEnergyLimitContractBuilder, selfType);
            any = Any.pack(UpdateEnergyLimitContractBuilder.build());
            break;

          case "CancelDeferredTransactionContract":
            CancelDeferredTransactionContract.Builder CancelDeferredTransactionContractBuilder =
                CancelDeferredTransactionContract.newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                    CancelDeferredTransactionContractBuilder, selfType );
            any = Any.pack(CancelDeferredTransactionContractBuilder.build());
            break;
          // todo add other contract
          default:
        }
        if (any != null) {
          String value = ByteArray.toHexString(any.getValue().toByteArray());
          parameter.put("value", value);
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

  public static boolean getVisible(final HttpServletRequest request )
  {
    boolean visible = false;
    if ( StringUtil.isNotBlank(request.getParameter("visible")) ) {
      visible = Boolean.valueOf(request.getParameter("visible"));
    }
    return visible;
  }

  public static boolean getVisiblePost(final String input )
  {
    boolean visible = false;
    JSONObject jsonObject = JSON.parseObject(input);
    if ( jsonObject.containsKey("visible") ) {
      visible = jsonObject.getBoolean("visible");
    }

    return visible;
  }

  public static String getHexAddress(final String address) {
    if (address != null) {
      byte[] addressByte = Wallet.decodeFromBase58Check(address);
      return ByteArray.toHexString(addressByte);
    } else {
      return  null;
    }
  }

  public static String getHexString(final String string) {
    return ByteArray.toHexString(ByteString.copyFromUtf8( string ).toByteArray());
  }
}
