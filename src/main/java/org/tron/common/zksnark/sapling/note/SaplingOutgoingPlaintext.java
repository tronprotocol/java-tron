package org.tron.common.zksnark.sapling.note;

import java.util.Optional;
import lombok.AllArgsConstructor;
import org.tron.common.zksnark.sapling.note.NoteEncryption.OutCiphertext;
import org.tron.common.zksnark.sapling.note.NoteEncryption.OutPlaintext;

import static org.tron.common.zksnark.sapling.ZkChainParams.*;

@AllArgsConstructor
public class SaplingOutgoingPlaintext {

  public byte[] pk_d;
  public byte[] esk;

  Optional<SaplingOutgoingPlaintext> decrypt(OutCiphertext ciphertext, byte[] ovk, byte[] cv, byte[] cm, byte[] epk) {

    Optional<OutPlaintext> pt = NoteEncryption.AttemptSaplingOutDecryption(ciphertext, ovk, cv, cm, epk);
    if(pt.isPresent()) {
      return Optional.empty();
    }

    SaplingOutgoingPlaintext ret;

    ret = SaplingOutgoingPlaintext.decode(pt.get());

    return Optional.of(ret);
    //    Optional<OutPlaintext> pt = NoteEncryption.AttemptSaplingOutDecryption(ciphertext,
    // ovk, cv, cm, epk);
    //    if (!pt.isPresent()) {
    //      return  none;
    //    }
    //
    //    // Deserialize from the plaintext
    //    CDataStream ss (SER_NETWORK, PROTOCOL_VERSION);
    //    ss << pt.get();
    //
    //    SaplingOutgoingPlaintext ret;
    //    ss >> ret;
    //
    //    assert (ss.size() == 0);
    //
    //    return ret;
    //return null;
  }

  public OutCiphertext encrypt(byte[] ovk, byte[] cv, byte[] cm, SaplingNoteEncryption enc) {

    OutPlaintext pt = this.encode();
    return enc.encrypt_to_ourselves(ovk, cv, cm, pt);
  }

  // todo:
  private OutPlaintext encode() {
    OutPlaintext ret = new OutPlaintext();

    ret.data = new byte[ZC_SAPLING_OUTPLAINTEXT_SIZE];

    System.arraycopy(pk_d, 0, ret.data, 0, ZC_JUBJUB_SCALAR_SIZE);
    System.arraycopy(esk, 0, ret.data, ZC_JUBJUB_SCALAR_SIZE, ZC_JUBJUB_POINT_SIZE);

    return ret;
  }


  private static SaplingOutgoingPlaintext decode(OutPlaintext outPlaintext) {
    byte[] data = outPlaintext.data;

    SaplingOutgoingPlaintext ret = new SaplingOutgoingPlaintext(new byte[ZC_JUBJUB_SCALAR_SIZE], new byte[ZC_JUBJUB_POINT_SIZE]);

    // ZC_SAPLING_OUTPLAINTEXT_SIZE = (ZC_JUBJUB_POINT_SIZE + ZC_JUBJUB_SCALAR_SIZE)
    System.arraycopy(data, 0, ret.pk_d, 0, ZC_JUBJUB_SCALAR_SIZE);

    System.arraycopy(data, ZC_JUBJUB_SCALAR_SIZE , ret.esk, 0, ZC_JUBJUB_POINT_SIZE);

    return ret;
  }
}
