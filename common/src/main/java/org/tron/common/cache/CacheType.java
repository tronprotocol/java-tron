package org.tron.common.cache;

import java.util.Arrays;

public enum CacheType {
  // for 127 SR
  witnessStandby("witnessStandby"),
  // for leveldb or rocksdb cache
  recentBlock("recent-block"),
  witness("witness"),
  witnessSchedule("witness_schedule"),
  delegatedResource("DelegatedResource"),
  delegatedResourceAccountIndex("DelegatedResourceAccountIndex"),
  votes("votes"),
  abi("abi"),
  code("code"),
  contract("contract"),
  assetIssueV2("asset-issue-v2"),
  properties("properties"),
  delegation("delegation"),
  storageRow("storage-row"),
  account("account"),
  // for leveldb or rocksdb cache

  // archive node
  worldStateQueryInstance("worldStateQueryInstance"),
  worldStateTrie("world-state-trie"),
  worldStateTrieDelegatedResource("world-state-trie.DelegatedResource"),
  worldStateTrieDelegatedResourceAccountIndex("world-state-trie.DelegatedResourceAccountIndex"),
  worldStateTrieVotes("world-state-trie.votes"),
  worldStateTrieStorageRow("world-state-trie.storage-row"),
  worldStateTrieAccount("world-state-trie.account"),
  worldStateTrieCode("world-state-trie.code"),
  worldStateTrieContract("world-state-trie.contract");

  public final String type;

  CacheType(String type) {
    this.type = type;
  }

  public static CacheType findByType(String type) {
    return Arrays.stream(CacheType.values()).filter(c -> c.type.equals(type)).findFirst()
        .orElseThrow(() ->  new  IllegalArgumentException(type));
  }

  @Override
  public String toString() {
    return type;
  }






}
