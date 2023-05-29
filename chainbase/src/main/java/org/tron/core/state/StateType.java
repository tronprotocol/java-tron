package org.tron.core.state;

import com.google.common.primitives.Bytes;
import java.util.Arrays;
import lombok.Getter;

public enum StateType {

  UNDEFINED((byte) 0x00, "undefined"),

  Account((byte) 0x01, "account"),
  AccountAsset((byte) 0x02, "account-asset"),
  AccountIndex((byte) 0x03, "account-index"),
  AccountIdIndex((byte) 0x04, "accountid-index"),
  AssetIssue((byte) 0x05, "asset-issue-v2"),
  Code((byte) 0x07, "code"),
  Contract((byte) 0x08, "contract"),
  Delegation((byte) 0x09, "delegation"),
  DelegatedResource((byte) 0x0a, "DelegatedResource"),
  DelegatedResourceAccountIndex((byte) 0x0b, "DelegatedResourceAccountIndex"),
  Exchange((byte) 0x0c, "exchange"),
  ExchangeV2((byte) 0x0d, "exchange-v2"),
  IncrementalMerkleTree((byte) 0x0e, "IncrementalMerkleTree"),
  MarketAccount((byte) 0x0f, "market_account"),
  MarketOrder((byte) 0x10, "market_order"),
  MarketPairPriceToOrder((byte) 0x11, "market_pair_price_to_order"),
  MarketPairToPrice((byte) 0x12, "market_pair_to_price"),
  Nullifier((byte) 0x13, "nullifier"),
  Properties((byte) 0x14, "properties"),
  Proposal((byte) 0x15, "proposal"),
  StorageRow((byte) 0x16, "storage-row"),
  Votes((byte) 0x17, "votes"),
  Witness((byte) 0x18, "witness"),
  WitnessSchedule((byte) 0x19, "witness_schedule"),
  ContractState((byte) 0x20, "contract-state");


  private final byte value;
  @Getter
  private final String name;

  StateType(byte value, String name) {
    this.value = value;
    this.name = name;
  }

  public byte value() {
    return this.value;
  }

  public static StateType get(String name) {
    return Arrays.stream(StateType.values()).filter(type -> type.name.equals(name))
        .findFirst().orElse(UNDEFINED);
  }

  public static StateType get(byte value) {
    return Arrays.stream(StateType.values()).filter(type -> type.value == value)
        .findFirst().orElse(UNDEFINED);
  }

  public static byte[] encodeKey(StateType type, byte[] key) {
    byte[] p = new byte[]{type.value};
    return Bytes.concat(p, key);
  }

  public static byte[] decodeKey(byte[] key) {
    return Arrays.copyOfRange(key, 1, key.length);
  }

}
