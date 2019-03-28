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

  public static Optional<SaplingOutgoingPlaintext> decrypt(OutCiphertext ciphertext, byte[] ovk, byte[] cv, byte[] cm, byte[] epk) {

    Optional<OutPlaintext> pt = NoteEncryption.AttemptSaplingOutDecryption(ciphertext, ovk, cv, cm, epk);
    if(!pt.isPresent()) {
      return Optional.empty();
    }

    SaplingOutgoingPlaintext ret;

    ret = SaplingOutgoingPlaintext.decode(pt.get());

    return Optional.of(ret);
  }

  public OutCiphertext encrypt(byte[] ovk, byte[] cv, byte[] cm, SaplingNoteEncryption enc) {

    OutPlaintext pt = this.encode();
    return enc.encrypt_to_ourselves(ovk, cv, cm, pt);
  }

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

  public static void main(String[] args) {
    OutCiphertext ciphertext = new OutCiphertext();
    byte[] data = {126,71,-120,-28,-28,-44,-47,60,-87,103,23,-110,-36,-92,-17,62,-21,-36,117,16,-77,46,-116,99,-49,-127,35,87,0,-97,-55,-78,84,73,-94,-21,-43,0,-8,105,-120,-22,-16,18,108,-25,18,92,-128,99,-86,-91,110,-35,26,-70,-19,-115,127,56,-103,5,-112,4,16,-75,42,-49,1,66,-112,-114,-77,-13,-84,-51,-81,109,-21,49};
    ciphertext.data = data;

    byte[] ovk = {-104,-47,105,19,-39,-101,4,23,124,-85,-92,79,110,77,34,78,3,-75,-84,3,29,124,-28,94,-122,81,56,-31,-71,-106,-42,59};
    byte[] cv = {-54,-33,24,-13,-44,99,6,3,-18,3,6,-119,89,12,-20,91,-118,27,95,-87,58,58,-23,68,62,-14,-83,-92,-87,-48,-74,-89};
    byte[] cm = {-54,-50,-21,-84,78,29,121,-123,78,-77,-20,-102,-101,127,56,20,105,-13,101,10,84,30,-98,72,-42,104,-96,119,123,-40,-37,55};
    byte[] epk = {109,39,-55,-100,-61,-86,4,48,42,-75,109,24,-9,-7,-46,-105,105,-46,-20,-126,107,-84,59,-83,11,-15,-31,36,-24,-40,-45,5};

    Optional<SaplingOutgoingPlaintext>  ret = decrypt(ciphertext, ovk, cv, cm, epk);
    SaplingOutgoingPlaintext result = ret.get();

    System.out.println("plain text pk_d size:" +  result.pk_d.length);
    for(byte b : result.pk_d) {
      System.out.print(b + ",");
    }
    System.out.println();

    System.out.println("plain text pk_d size:" +  result.esk.length);
    for(byte b : result.esk) {
      System.out.print(b + ",");
    }
    System.out.println();

    return;
  }
}
