package org.tron.core.zen;

public class ZkChainParams {

  public static int BIP44CoinType = 133; // todo:

  public static int SerializedSaplingPaymentAddressSize = 43;
  public static int SerializedSaplingFullViewingKeySize = 96;
  public static int SerializedSaplingExpandedSpendingKeySize = 96;
  public static int SerializedSaplingSpendingKeySize = 32;
  public static String APLING_PAYMENT_ADDRESS = "zs";

  public static int ZC_NUM_JS_INPUTS = 2;
  public static int ZC_NUM_JS_OUTPUTS = 2;
  public static int INCREMENTAL_MERKLE_TREE_DEPTH = 29;
  public static int INCREMENTAL_MERKLE_TREE_DEPTH_TESTING = 4;

  public static int SAPLING_INCREMENTAL_MERKLE_TREE_DEPTH = 32;

  public static int NOTEENCRYPTION_AUTH_BYTES = 16;

  public static int ZC_NOTEPLAINTEXT_LEADING = 1;
  public static int ZC_V_SIZE = 8;
  public static int ZC_RHO_SIZE = 32;
  public static int ZC_R_SIZE = 32;
  public static int ZC_MEMO_SIZE = 512;
  public static int ZC_DIVERSIFIER_SIZE = 11;
  public static int ZC_JUBJUB_POINT_SIZE = 32;
  public static int ZC_JUBJUB_SCALAR_SIZE = 32;

  // 583
  public static int ZC_NOTEPLAINTEXT_SIZE =
      (ZC_NOTEPLAINTEXT_LEADING + ZC_V_SIZE + ZC_RHO_SIZE + ZC_R_SIZE + ZC_MEMO_SIZE);

  public static int ZC_SAPLING_ENCPLAINTEXT_SIZE =
      (ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE + ZC_V_SIZE + ZC_R_SIZE + ZC_MEMO_SIZE);
  public static int ZC_SAPLING_OUTPLAINTEXT_SIZE = (ZC_JUBJUB_POINT_SIZE + ZC_JUBJUB_SCALAR_SIZE);

  public static int ZC_SAPLING_ENCCIPHERTEXT_SIZE =
      (ZC_SAPLING_ENCPLAINTEXT_SIZE + NOTEENCRYPTION_AUTH_BYTES);
  public static int ZC_SAPLING_OUTCIPHERTEXT_SIZE =
      (ZC_SAPLING_OUTPLAINTEXT_SIZE + NOTEENCRYPTION_AUTH_BYTES);
}
