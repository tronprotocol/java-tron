package org.tron.common.zksnark.sapling.note;

import java.util.Optional;
import org.tron.common.zksnark.sapling.transaction.Ciphertext.SaplingEncCiphertext;
import org.tron.common.zksnark.sapling.transaction.Ciphertext.SaplingEncPlaintext;
import org.tron.common.zksnark.sapling.transaction.Ciphertext.SaplingOutCiphertext;
import org.tron.common.zksnark.sapling.transaction.Ciphertext.SaplingOutPlaintext;

public class NoteEncryption {

  public static Optional<SaplingEncPlaintext> AttemptSaplingEncDecryption(
      SaplingEncCiphertext ciphertext, byte[] ivk, byte[] epk) {
    return null;
  }

  public static Optional<SaplingEncPlaintext> AttemptSaplingEncDecryption(
      SaplingEncCiphertext ciphertext, byte[] epk, byte[] esk, byte[] pk_d) {
    return null;
  }

  public static Optional<SaplingOutPlaintext> AttemptSaplingOutDecryption(
      SaplingOutCiphertext ciphertext, byte[] ovk, byte[] cv, byte[] cm, byte[] epk) {

    return null;
  }
}
