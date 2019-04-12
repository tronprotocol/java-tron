package org.tron.core.services.http;

import java.util.HashMap;
import java.util.Map;

public class HttpSelfFormatFieldName {
    public static final Map<String, Integer> AddressFieldNameMap = new HashMap<>();
    public static final Map<String, Integer> NameFieldNameMap = new HashMap<>();

    static {
        //***** api.proto *****
        //DelegatedResourceMessage
        AddressFieldNameMap.put("fromAddress", 1);
        AddressFieldNameMap.put("toAddress", 1);
        //EasyTransferMessage
        AddressFieldNameMap.put("toAddress", 1);
        //EasyTransferAssetMessage
        AddressFieldNameMap.put("toAddress", 1);
        //EasyTransferByPrivateMessage
        AddressFieldNameMap.put("toAddress", 1);
        //TransactionSignWeight
        AddressFieldNameMap.put("approved_list", 1);
        //TransactionApprovedList
        AddressFieldNameMap.put("approved_list", 1);

        //***** Contract.proto *****
        //AccountCreateContract
        AddressFieldNameMap.put("owner_address", 1);
        AddressFieldNameMap.put("account_address", 1);
        //AccountUpdateContract
        AddressFieldNameMap.put("owner_address", 1);
        //SetAccountIdContract
        AddressFieldNameMap.put("owner_address", 1);
        //TransferContract
        AddressFieldNameMap.put("owner_address", 1);
        AddressFieldNameMap.put("to_address", 1);
        //TransferAssetContract
        AddressFieldNameMap.put("owner_address", 1);
        AddressFieldNameMap.put("to_address", 1);
        //VoteAssetContract
        AddressFieldNameMap.put("owner_address", 1);
        AddressFieldNameMap.put("vote_address", 1);
        //VoteWitnessContract
        AddressFieldNameMap.put("owner_address", 1);
        AddressFieldNameMap.put("contract_address", 1);
        //UpdateEnergyLimitContract
        AddressFieldNameMap.put("owner_address", 1);
        AddressFieldNameMap.put("contract_address", 1);
        //WitnessCreateContract
        AddressFieldNameMap.put("owner_address", 1);
        //WitnessUpdateContract
        AddressFieldNameMap.put("owner_address", 1);
        //AssetIssueContract
        AddressFieldNameMap.put("owner_address", 1);
        //ParticipateAssetIssueContract
        AddressFieldNameMap.put("owner_address", 1);
        AddressFieldNameMap.put("to_address", 1);
        //FreezeBalanceContract
        AddressFieldNameMap.put("owner_address", 1);
        AddressFieldNameMap.put("receiver_address", 1);
        //UnfreezeBalanceContract
        AddressFieldNameMap.put("owner_address", 1);
        AddressFieldNameMap.put("receiver_address", 1);
        //UnfreezeAssetContract
        AddressFieldNameMap.put("owner_address", 1);
        //WithdrawBalanceContract
        AddressFieldNameMap.put("owner_address", 1);
        //UpdateAssetContract
        AddressFieldNameMap.put("owner_address", 1);
        //ProposalCreateContract
        AddressFieldNameMap.put("owner_address", 1);
        //ProposalApproveContract
        AddressFieldNameMap.put("owner_address", 1);
        //ProposalDeleteContract
        AddressFieldNameMap.put("owner_address", 1);
        //CreateSmartContract
        AddressFieldNameMap.put("owner_address", 1);
        //TriggerSmartContract
        AddressFieldNameMap.put("owner_address", 1);
        AddressFieldNameMap.put("contract_address", 1);
        //BuyStorageContract
        AddressFieldNameMap.put("owner_address", 1);
        //BuyStorageBytesContract
        AddressFieldNameMap.put("owner_address", 1);
        //SellStorageContract
        AddressFieldNameMap.put("owner_address", 1);
        //ExchangeCreateContract
        AddressFieldNameMap.put("owner_address", 1);
        //ExchangeInjectContract
        AddressFieldNameMap.put("owner_address", 1);
        //ExchangeWithdrawContract
        AddressFieldNameMap.put("owner_address", 1);
        //ExchangeTransactionContract
        AddressFieldNameMap.put("owner_address", 1);
        //AccountPermissionUpdateContract
        AddressFieldNameMap.put("owner_address", 1);

        //***** Tron.proto *****
        //AccountId
        AddressFieldNameMap.put("address", 1);
        //Vote
        AddressFieldNameMap.put("vote_address", 1);
        //Proposal
        AddressFieldNameMap.put("proposer_address", 1);
        AddressFieldNameMap.put("approvals", 1);
        //Exchange
        AddressFieldNameMap.put("creator_address", 1);
        //Account
        AddressFieldNameMap.put("address", 1);
        //Key
        AddressFieldNameMap.put("address", 1);
        //DelegatedResource
        AddressFieldNameMap.put("from", 1);
        AddressFieldNameMap.put("to", 1);
        //Witness
        AddressFieldNameMap.put("address", 1);
        //Votes
        AddressFieldNameMap.put("address", 1);
        //TransactionInfo
        AddressFieldNameMap.put("address", 1);
        AddressFieldNameMap.put("contract_address", 1);
        //BlockHeader
        AddressFieldNameMap.put("witness_address", 1);
        //SmartContract
        AddressFieldNameMap.put("origin_address", 1);
        AddressFieldNameMap.put("contract_address", 1);
        //InternalTransaction
        AddressFieldNameMap.put("caller_address", 1);
        AddressFieldNameMap.put("transferTo_address", 1);
        //DelegatedResourceAccountIndex
        AddressFieldNameMap.put("account", 1);
        AddressFieldNameMap.put("fromAccounts", 1);
        AddressFieldNameMap.put("toAccounts", 1);


        //***** api.proto *****
        //Return
        NameFieldNameMap.put("message", 1);
        //Address
        NameFieldNameMap.put("host", 1);

        //***** Contract.proto *****
        //AccountUpdateContract
        NameFieldNameMap.put("account_name", 1);
        //SetAccountIdContract
        NameFieldNameMap.put("account_id", 1);
        //TransferAssetContract
        NameFieldNameMap.put("asset_name", 1);
        //WitnessCreateContract
        NameFieldNameMap.put("url", 1);
        //WitnessUpdateContract
        NameFieldNameMap.put("update_url", 1);
        //AssetIssueContract
        NameFieldNameMap.put("name", 1);
        NameFieldNameMap.put("abbr", 1);
        NameFieldNameMap.put("description", 1);
        NameFieldNameMap.put("url", 1);
        //ParticipateAssetIssueContract
        NameFieldNameMap.put("asset_name", 1);
        //UpdateAssetContract
        NameFieldNameMap.put("description", 1);
        NameFieldNameMap.put("url", 1);
        //ExchangeCreateContract
        NameFieldNameMap.put("first_token_id", 1);
        NameFieldNameMap.put("second_token_id", 1);
        //ExchangeInjectContract
        NameFieldNameMap.put("token_id", 1);
        //ExchangeWithdrawContract
        NameFieldNameMap.put("token_id", 1);
        //ExchangeTransactionContract
        NameFieldNameMap.put("token_id", 1);

        //***** Tron.proto *****
        //AccountId
        NameFieldNameMap.put("name", 1);
        //Exchange
        NameFieldNameMap.put("first_token_id", 1);
        NameFieldNameMap.put("second_token_id", 1);
        //Account
        NameFieldNameMap.put("account_name", 1);
        NameFieldNameMap.put("asset_issued_name", 1);
        NameFieldNameMap.put("asset_issued_ID", 1);
        NameFieldNameMap.put("account_id", 1);
        //authority
        NameFieldNameMap.put("permission_name", 1);
        //Transaction
        NameFieldNameMap.put("ContractName", 1);
        //TransactionInfo
        NameFieldNameMap.put("topics", 1);
        NameFieldNameMap.put("resMessage", 1);
    }

    public static boolean isAddressFormat(final String name) {
        return AddressFieldNameMap.containsKey(name);
    }

    public static boolean isNameStringFormat(final String name) {
        return NameFieldNameMap.containsKey(name);
    }
}
