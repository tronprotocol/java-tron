package org.tron.core;

public class Constant {

  // Address
  public static final byte ADD_PRE_FIX_BYTE_MAINNET = (byte) 0x41;
  public static final String ADD_PRE_FIX_STRING_MAINNET = "41";
  public static final int STANDARD_ADDRESS_SIZE = 20;
  public static final int TRON_ADDRESS_SIZE = 21;

  // Node type
  public static final int NODE_TYPE_FULL_NODE = 0;
  public static final int NODE_TYPE_LIGHT_NODE = 1;

  // Transaction
  public static final long TRANSACTION_MAX_BYTE_SIZE = 500 * 1_024L;
  public static final int CREATE_ACCOUNT_TRANSACTION_MIN_BYTE_SIZE = 500;
  public static final int CREATE_ACCOUNT_TRANSACTION_MAX_BYTE_SIZE = 10000;
  public static final long MAXIMUM_TIME_UNTIL_EXPIRATION = 24 * 60 * 60 * 1_000L; //one day
  public static final long TRANSACTION_DEFAULT_EXPIRATION_TIME = 60 * 1_000L; //60 seconds
  public static final long TRANSACTION_FEE_POOL_PERIOD = 1; //1 blocks
  public static final long PER_SIGN_LENGTH = 65L;
  public static final long MAX_CONTRACT_RESULT_SIZE = 2L;

  // Smart contract / Energy
  public static final long SUN_PER_ENERGY = 100; // 1 us = 100 SUN = 100 * 10^-6 TRX
  public static final long ENERGY_LIMIT_IN_CONSTANT_TX = 3_000_000L; // ref: 1 us = 1 energy
  public static final long MAX_RESULT_SIZE_IN_TX = 64; // max 8 * 8 items in result
  public static final long PB_DEFAULT_ENERGY_LIMIT = 0L;
  public static final long CREATOR_DEFAULT_ENERGY_LIMIT = 1000 * 10_000L;

  // Proposal
  public static final long MIN_PROPOSAL_EXPIRE_TIME = 0L; // 0 ms
  public static final long MAX_PROPOSAL_EXPIRE_TIME = 31536003000L; // ms of 365 days + 3000 ms
  public static final long DEFAULT_PROPOSAL_EXPIRE_TIME = 259200000L; // ms of 3 days

  // Dynamic energy
  public static final long DYNAMIC_ENERGY_FACTOR_DECIMAL = 10_000L;
  public static final long DYNAMIC_ENERGY_INCREASE_FACTOR_RANGE = 10_000L;
  public static final long DYNAMIC_ENERGY_MAX_FACTOR_RANGE = 100_000L;
  public static final int DYNAMIC_ENERGY_DECREASE_DIVISION = 4;

  // Numbers
  public static final int ONE_HUNDRED = 100;
  public static final int ONE_THOUSAND = 1000;

  // Crypto
  public static final byte[] ZTRON_EXPANDSEED_PERSONALIZATION = {'Z', 't', 'r', 'o', 'n', '_', 'E',
      'x',
      'p', 'a', 'n', 'd', 'S', 'e', 'e', 'd'};
  public static final int ZC_DIVERSIFIER_SIZE = 11;
  public static final int ZC_OUTPUT_DESC_MAX_SIZE = 10;

  // DB
  public static final String INFO_FILE_NAME = "info.properties";
  public static final String SPLIT_BLOCK_NUM = "split_block_num";
  public static final String MARKET_PAIR_PRICE_TO_ORDER = "market_pair_price_to_order";
  public static final String ROCKSDB = "ROCKSDB";

  // Crypto engine
  public static final String ECKey_ENGINE = "ECKey";

  // Network
  public static final String LOCAL_HOST = "127.0.0.1";

  // JSON parsing (DoS protection)
  public static final int MAX_NESTING_DEPTH = 100;
  public static final int MAX_TOKEN_COUNT = 100_000;

}
