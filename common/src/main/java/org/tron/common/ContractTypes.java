package org.tron.common;

import java.util.Arrays;

public class ContractTypes {

  public static String[] getContractTypes() {
    return Arrays.asList(
            "type",
            "read",
            "put",
            "sig",
            "AccountCreateContract",
            "TransferContract",
            "TransferAssetContract",
            "VoteAssetContract",
            "VoteWitnessContract",
            "WitnessCreateContract",
            "AssetIssueContract",
            "WitnessUpdateContract",
            "ParticipateAssetIssueContract",
            "AccountUpdateContract",
            "FreezeBalanceContract",
            "UnfreezeBalanceContract",
            "WithdrawBalanceContract",
            "UnfreezeAssetContract",
            "UpdateAssetContract",
            "ProposalCreateContract",
            "ProposalApproveContract",
            "ProposalDeleteContract",
            "SetAccountIdContract",
            "CustomContract",
            "CreateSmartContract",
            "TriggerSmartContract",
            "GetContract",
            "UpdateSettingContract",
            "ExchangeCreateContract",
            "ExchangeInjectContract",
            "ExchangeWithdrawContract",
            "ExchangeTransactionContract",
            "UpdateEnergyLimitContract",
            "AccountPermissionUpdateContract",
            "ClearABIContract",
            "UpdateBrokerageContract",
            "ShieldedTransferContract",
            "MarketSellAssetContract",
            "MarketCancelOrderContract",
            "FreezeBalanceV2Contract",
            "UnfreezeBalanceV2Contract",
            "WithdrawExpireUnfreezeContract",
            "DelegateResourceContract",
            "UnDelegateResourceContract",
            "CancelAllUnfreezeV2Contract"
    ).toArray(new String[0]);
  }

}
