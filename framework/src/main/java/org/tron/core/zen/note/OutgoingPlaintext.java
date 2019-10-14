package org.tron.core.zen.note;

import static org.tron.core.utils.ZenChainParams.ZC_JUBJUB_POINT_SIZE;
import static org.tron.core.utils.ZenChainParams.ZC_JUBJUB_SCALAR_SIZE;
import static org.tron.core.utils.ZenChainParams.ZC_OUTPLAINTEXT_SIZE;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.zen.note.NoteEncryption.Encryption;
import org.tron.core.zen.note.NoteEncryption.Encryption.OutCiphertext;
import org.tron.core.zen.note.NoteEncryption.Encryption.OutPlaintext;

@AllArgsConstructor
public class OutgoingPlaintext {

  @Getter
  @Setter
  private byte[] pkD;
  @Getter
  @Setter
  private byte[] esk;

  private static OutgoingPlaintext decode(OutPlaintext outPlaintext) {
    byte[] data = outPlaintext.getData();
    OutgoingPlaintext ret = new OutgoingPlaintext(new byte[ZC_JUBJUB_SCALAR_SIZE],
        new byte[ZC_JUBJUB_POINT_SIZE]);
    System.arraycopy(data, 0, ret.pkD, 0, ZC_JUBJUB_SCALAR_SIZE);
    System.arraycopy(data, ZC_JUBJUB_SCALAR_SIZE, ret.esk, 0, ZC_JUBJUB_POINT_SIZE);
    return ret;
  }

  public static Optional<OutgoingPlaintext> decrypt(OutCiphertext ciphertext, byte[] ovk,
      byte[] cv, byte[] cm, byte[] epk) throws ZksnarkException {
    Optional<OutPlaintext> pt = Encryption
        .attemptOutDecryption(ciphertext, ovk, cv, cm, epk);
    if (!pt.isPresent()) {
      return Optional.empty();
    }
    OutgoingPlaintext ret = OutgoingPlaintext.decode(pt.get());
    return Optional.of(ret);
  }

  private OutPlaintext encode() {
    OutPlaintext ret = new OutPlaintext();
    ret.setData(new byte[ZC_OUTPLAINTEXT_SIZE]);
    // ZC_OUTPLAINTEXT_SIZE = (ZC_JUBJUB_POINT_SIZE + ZC_JUBJUB_SCALAR_SIZE)
    System.arraycopy(pkD, 0, ret.getData(), 0, ZC_JUBJUB_SCALAR_SIZE);
    System.arraycopy(esk, 0, ret.getData(), ZC_JUBJUB_SCALAR_SIZE, ZC_JUBJUB_POINT_SIZE);
    return ret;
  }

  /**
   * encrypt plain_out with ock to c_out, use NoteEncryption.epk
   */
  public OutCiphertext encrypt(byte[] ovk, byte[] cv, byte[] cm, NoteEncryption enc)
      throws ZksnarkException {
    OutPlaintext pt = this.encode();
    return enc.encryptToOurselves(ovk, cv, cm, pt);
  }
}
