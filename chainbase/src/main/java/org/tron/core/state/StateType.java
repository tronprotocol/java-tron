package org.tron.core.state;

public enum StateType {

  UNDEFINED((byte)0x00, "undefined"),

  Account((byte)0x01, "account"),
//  AccountAsset((byte)0x02, "account-asset"),
  AccountIndex((byte)0x03, "account-index"),
  AccountIdIndex((byte)0x04, "accountid-index"),
  AccountIssue((byte)0x05, "asset-issue"),
//  AccountIssueV2((byte)0x05, "asset-issue-v2"),  // same as AccountIssue
  Code((byte)0x07, "code"),
  Contract((byte)0x08, "contract"),
  Delegation((byte)0x09, "delegation"),
  DelegatedResource((byte)0x0a, "DelegatedResource"),
//  DelegatedResourceAccountIndex((byte)0x0b, "DelegatedResourceAccountIndex"),
  Exchange((byte)0x0c, "exchange"),
  ExchangeV2((byte)0x0d, "exchange-v2"),
  IncrementalMerkleTree((byte)0x0e, "IncrementalMerkleTree"),
  MarketAccount((byte)0x0f, "market_account"),
  MarketOrder((byte)0x10, "market_order"),
  MarketPairPriceToOrder((byte)0x11, "market_pair_price_to_order"),
  MarketPairToPrice((byte)0x12, "market_pair_to_price"),
  Nullifier((byte)0x13, "nullifier"),
  Properties((byte)0x14, "properties"),
  Proposal((byte)0x15, "proposal"),
  StorageRow((byte)0x16, "storage-row"),
  Votes((byte)0x17, "votes"),
  Witness((byte)0x18, "witness"),
  WitnessSchedule((byte)0x19, "witness_schedule");

  private byte value;
  private String name;

  StateType(byte value, String name) {
    this.value = value;
    this.name = name;
  }

  public byte value() {
    return this.value;
  }

  public static StateType get(String name) {
    switch (name) {
      case "account":
        return Account;
      case "account-index":
        return AccountIndex;
      case "accountid-index":
        return AccountIdIndex;
      case "asset-issue":
        return AccountIssue;
      case "code":
        return Code;
      case "contract":
        return Contract;
      case "delegation":
        return Delegation;
      case "DelegatedResource":
        return DelegatedResource;
      case "exchange":
        return Exchange;
      case "exchange-v2":
        return ExchangeV2;
      case "IncrementalMerkleTree":
        return IncrementalMerkleTree;
      case "market_account":
        return MarketAccount;
      case "market_order":
        return MarketOrder;
      case "market_pair_price_to_order":
        return MarketPairPriceToOrder;
      case "market_pair_to_price":
        return MarketPairToPrice;
      case "nullifier":
        return Nullifier;
      case "properties":
        return Properties;
      case "proposal":
        return Proposal;
      case "storage-row":
        return StorageRow;
      case "votes":
        return Votes;
      case "witness":
        return Witness;
      case "witness_schedule":
        return WitnessSchedule;
      default:
        return UNDEFINED;
    }
  }
}