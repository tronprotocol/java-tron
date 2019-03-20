package org.tron.common.zksnark.sapling.note;

import java.util.Optional;

public class NoteEncryption {

  public static class EncCiphertext {

    public byte[] data; // ZC_SAPLING_ENCCIPHERTEXT_SIZE
  }

  public static class EncPlaintext {

    public byte[] data; // ZC_SAPLING_ENCPLAINTEXT_SIZE
  }

  public static class OutCiphertext {

    public byte[] data; // ZC_SAPLING_OUTCIPHERTEXT_SIZE
  }

  public static class OutPlaintext {

    public byte[] data; // ZC_SAPLING_OUTPLAINTEXT_SIZE
  }

  // todo:
  public static Optional<EncPlaintext> AttemptSaplingEncDecryption(
      EncCiphertext ciphertext, byte[] ivk, byte[] epk) {
    return null;
  }

  public static Optional<EncPlaintext> AttemptSaplingEncDecryption(
      EncCiphertext ciphertext, byte[] epk, byte[] esk, byte[] pk_d) {
    return null;
  }

  public static Optional<OutPlaintext> AttemptSaplingOutDecryption(
      OutCiphertext ciphertext, byte[] ovk, byte[] cv, byte[] cm, byte[] epk) {

    return null;
  }
}
