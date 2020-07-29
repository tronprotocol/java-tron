package org.tron.core.utils;

public class ZenChainParams {

  public static final int NOTEENCRYPTION_AUTH_BYTES = 16;
  public static final int ZC_NOTEPLAINTEXT_LEADING = 1;
  public static final int ZC_V_SIZE = 8;
  public static final int ZC_R_SIZE = 32;
  public static final int ZC_MEMO_SIZE = 512;
  public static final int ZC_DIVERSIFIER_SIZE = 11;
  public static final int ZC_JUBJUB_POINT_SIZE = 32;
  public static final int ZC_JUBJUB_SCALAR_SIZE = 32;

  public static final int ZC_ENCPLAINTEXT_SIZE =
      (ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE + ZC_V_SIZE + ZC_R_SIZE + ZC_MEMO_SIZE);
  public static final int ZC_OUTPLAINTEXT_SIZE = (ZC_JUBJUB_POINT_SIZE + ZC_JUBJUB_SCALAR_SIZE);

  public static final int ZC_ENCCIPHERTEXT_SIZE =
      (ZC_ENCPLAINTEXT_SIZE + NOTEENCRYPTION_AUTH_BYTES);
  public static final int ZC_OUTCIPHERTEXT_SIZE =
      (ZC_OUTPLAINTEXT_SIZE + NOTEENCRYPTION_AUTH_BYTES);
}
