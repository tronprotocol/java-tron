package org.tron.core.db.fast;

public class FastSyncStoreConstant {

  public static final String DYNAMIC_PROPERTIES_STORE_KEY = "1";
  public static final String ASSET_ISSUE_STORE_KEY = "2";
  public static final String ASSET_ISSUE_V_2_STORE_KEY = "3";
  public static final String EXCHANGE_STORE_KEY = "4";
  public static final String EXCHANGE_V_2_STORE_KEY = "5";
  public static final String DELEGATED_RESOURCE_STORE_KEY = "6";
  public static final String CONTRACT_STORE_KEY = "7";
  public static final String DELEGATED_RESOURCE_ACCOUNT_STORE_KEY = "8";
  public static final String WITNESS_STORE_KEY = "9";
  public static final String PROPOSAL_STORE_KEY = "10";
  public static final String ACCOUNT_ID_INDEX_STORE_KEY = "11";
  public static final String VOTES_STORE_KEY = "12";
  public static final String ACCOUNT_INDEX_STORE_KEY = "13";
  public static final String STORAGE_STORE_KEY = "14";

  public enum TrieEnum {
    DYNAMIC(DYNAMIC_PROPERTIES_STORE_KEY),
    ASSET(ASSET_ISSUE_STORE_KEY),
    ASSET2(ASSET_ISSUE_V_2_STORE_KEY),
    EXCHANGE(EXCHANGE_STORE_KEY),
    EXCHANGE2(EXCHANGE_V_2_STORE_KEY),
    DELEGATED_RESOURCE(DELEGATED_RESOURCE_STORE_KEY),
    CONTRACT(CONTRACT_STORE_KEY),
    DELEGATED_RESOURCE_ACCOUNT_INDEX(DELEGATED_RESOURCE_ACCOUNT_STORE_KEY),
    WITNESS(WITNESS_STORE_KEY),
    PROPOSAL(PROPOSAL_STORE_KEY),
    ACCOUNT_ID_INDEX(ACCOUNT_ID_INDEX_STORE_KEY),
    VOTES(VOTES_STORE_KEY),
    ACCOUNT_INDEX(ACCOUNT_INDEX_STORE_KEY),
    STORAGE(STORAGE_STORE_KEY);

    TrieEnum(String key) {
      this.key = key;
    }

    private String key;

    public String getKey() {
      return key;
    }
  }

}
