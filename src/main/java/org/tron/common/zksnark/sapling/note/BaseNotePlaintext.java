package org.tron.common.zksnark.sapling.note;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.tron.common.zksnark.sapling.Librustzcash;
import org.tron.common.zksnark.sapling.address.DiversifierT;
import org.tron.common.zksnark.sapling.address.IncomingViewingKey;
import org.tron.common.zksnark.sapling.address.PaymentAddress;
import org.tron.common.zksnark.sapling.note.BaseNote.Note;
import org.tron.common.zksnark.sapling.note.NoteEncryption.EncCiphertext;
import org.tron.common.zksnark.sapling.note.NoteEncryption.EncPlaintext;
import static org.tron.common.zksnark.sapling.ZkChainParams.*;

public class BaseNotePlaintext {

  public long value = 0L; // 64
  public byte[] memo = new byte[ZC_MEMO_SIZE];

  @AllArgsConstructor
  public class SaplingNotePlaintextEncryptionResult {

    public NoteEncryption.EncCiphertext encCiphertext;
    public SaplingNoteEncryption noteEncryption;
    // pair<EncCiphertext, SaplingNoteEncryption> SaplingNotePlaintextEncryptionResult;
  }

  public static class NotePlaintext extends BaseNotePlaintext {

    public DiversifierT d;
    public byte[] rcm;

    public NotePlaintext() {
      d = new DiversifierT();
      rcm = new byte[ZC_R_SIZE];
    }

    public NotePlaintext(Note note, byte[] memo) {
      d = note.d;
      rcm = note.r;
      value = note.value;
      memo = memo;
    }

    public Optional<Note> note(IncomingViewingKey ivk) {
      Optional<PaymentAddress> addr = ivk.address(d);
      if (addr.isPresent()) {
        return Optional.of(new Note(d, addr.get().getPkD(), value, rcm));
      } else {
        return Optional.empty();
      }
    }

    public static Optional<NotePlaintext> decrypt(
        NoteEncryption.EncCiphertext ciphertext, byte[] ivk, byte[] epk, byte[] cmu) {

      Optional<NoteEncryption.EncPlaintext> pt =
          NoteEncryption.AttemptSaplingEncDecryption(ciphertext, ivk, epk);
      if (!pt.isPresent()) {
        return Optional.empty();
      }

      NotePlaintext ret = NotePlaintext.decode(pt.get());

      byte[] pk_d = new byte[32];
      if (!Librustzcash.librustzcashIvkToPkd(ivk, ret.d.getData(), pk_d)) {
        return Optional.empty();
      }

      byte[] cmu_expected = new byte[32];
      if (!Librustzcash.librustzcashSaplingComputeCm(
          ret.d.getData(), pk_d, ret.value, ret.rcm, cmu_expected)) {
        return Optional.empty();
      }

      if (!Arrays.equals(cmu, cmu_expected)) {
        return Optional.empty();
      }

      return Optional.of(ret);
    }

    public static Optional<NotePlaintext> decrypt(
        NoteEncryption.EncCiphertext ciphertext, byte[] epk, byte[] esk, byte[] pk_d, byte[] cmu) {
      //      auto pt = AttemptSaplingEncDecryption(ciphertext, epk, esk, pk_d);
      ////      if (!pt) {
      ////        return Optional.empty();
      ////      }
      ////
      ////      // Deserialize from the plaintext
      ////      CDataStream ss (SER_NETWORK, PROTOCOL_VERSION);
      ////      ss << pt.get();
      ////
      ////      NotePlaintext ret;
      ////      ss >> ret;
      ////
      ////      byte[] cmu_expected;
      ////      if (!librustzcash_sapling_compute_cm(
      ////          ret.d.data(),
      ////          pk_d.begin(),
      ////          ret.value(),
      ////          ret.rcm.begin(),
      ////          cmu_expected.begin()
      ////      )) {
      ////        return Optional.empty();
      ////      }
      ////
      ////      if (cmu_expected != cmu) {
      ////        return Optional.empty();
      ////      }
      ////
      ////      assert (ss.size() == 0);
      ////
      ////      return ret;
      return null;
    }

    public Optional<SaplingNotePlaintextEncryptionResult> encrypt(byte[] pk_d) {

      // Get the encryptor
      Optional<SaplingNoteEncryption> sne = SaplingNoteEncryption.FromDiversifier(d);
      if (!sne.isPresent()) {
        return Optional.empty();
      }
      SaplingNoteEncryption enc = sne.get();

      // Create the plaintext
      EncPlaintext pt = this.encode();

      // Encrypt the plaintext
      Optional<EncCiphertext> encciphertext = enc.encryptToRecipient(pk_d, pt);
      if (!encciphertext.isPresent()) {
        return Optional.empty();
      }
      return Optional.of(new SaplingNotePlaintextEncryptionResult(encciphertext.get(), enc));
    }

    // todo:
    public NoteEncryption.EncPlaintext encode() {
      ByteBuffer buffer = ByteBuffer.allocate(ZC_V_SIZE);
      byte[] valueLong;
      byte[] data;
      NoteEncryption.EncPlaintext ret = new NoteEncryption.EncPlaintext();

      ret.data = new byte[ZC_SAPLING_ENCPLAINTEXT_SIZE];
      data = ret.data;

      //将long转换为byte[]
      buffer.putLong(0, value);
      valueLong =  buffer.array();

      data[0] = 0x01;
      System.arraycopy(d, 0, data, ZC_NOTEPLAINTEXT_LEADING, ZC_DIVERSIFIER_SIZE);
      System.arraycopy(valueLong, 0, data,
              ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE, ZC_V_SIZE);
      System.arraycopy(rcm, 0, data,
              ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE + ZC_V_SIZE, ZC_R_SIZE);
      System.arraycopy(memo, 0, data,
              ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE + ZC_V_SIZE + ZC_R_SIZE, ZC_MEMO_SIZE);

      return ret;
    }

    public static NotePlaintext decode(NoteEncryption.EncPlaintext encPlaintext) {
      byte[] data = encPlaintext.data;
      byte[] valueLong = new byte[ZC_V_SIZE];
      ByteBuffer buffer = ByteBuffer.allocate(ZC_V_SIZE);

//      READWRITE(leadingByte);
//
//      if (leadingByte != 0x01) {
//        throw std::ios_base::failure("lead byte of SaplingNotePlaintext is not recognized");
//      }
//
//      READWRITE(d);           // 11 bytes
//      READWRITE(value_);      // 8 bytes
//      READWRITE(rcm);         // 32 bytes
//      READWRITE(memo_);       // 512 bytes

      if(encPlaintext.data[0] != 0x01) {
        throw new RuntimeException("lead byte of SaplingNotePlaintext is not recognized");
      }

      NotePlaintext ret = new NotePlaintext();
      byte[] diversifierData = new byte[ZC_DIVERSIFIER_SIZE]; // ZC_DIVERSIFIER_SIZE

      //(ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE + ZC_V_SIZE + ZC_R_SIZE + ZC_MEMO_SIZE);
      System.arraycopy(data, ZC_NOTEPLAINTEXT_LEADING,
              diversifierData, 0, ZC_DIVERSIFIER_SIZE);
      ret.d.setData(diversifierData);

      System.arraycopy(data, ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE,
              valueLong, 0, ZC_V_SIZE);
      System.arraycopy(data, ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE + ZC_V_SIZE,
              ret.rcm, 0, ZC_R_SIZE);
      System.arraycopy(data, ZC_NOTEPLAINTEXT_LEADING + ZC_DIVERSIFIER_SIZE + ZC_V_SIZE + ZC_R_SIZE,
              ret.memo, 0, ZC_MEMO_SIZE);

      // TODO C++是按照大字节序保存的
      for(int i=0; i<valueLong.length/2; i++) {
        byte temp = valueLong[i];
        valueLong[i] = valueLong[valueLong.length - 1 - i];
        valueLong[valueLong.length - 1 - i] = temp;
      }

      //将byte[]转换成long
      buffer.put(valueLong, 0, valueLong.length);
      buffer.flip();
      ret.value = buffer.getLong();
      //ret.value = Long.reverse(ret.value);

      return ret;
    }

    public static void main(String args[]) {
      EncCiphertext ciphertext = new EncCiphertext();
      byte[] data = {-60,-107,77,-106,-26,98,119,-99,99,-116,-36,-47,97,-18,-33,-34,54,53,-33,81,-43,-115,34,36,-41,-71,-16,-56,49,106,71,113,-29,-48,10,-53,-123,-78,-119,-119,-52,-107,-26,-55,81,-51,-80,108,127,118,-124,90,25,-7,-47,-40,-45,102,-82,56,-49,108,28,41,110,113,-126,-113,-82,-107,-16,-86,40,7,110,-78,108,85,-118,-27,-21,-104,54,69,2,92,98,82,-57,69,-106,19,-48,59,105,-16,7,-8,-56,-106,-83,108,44,-23,-69,66,-121,76,121,-89,-24,55,18,92,-53,114,-117,117,-58,-18,-38,50,-98,-16,-92,-45,-88,-72,-91,109,109,115,-99,-2,-49,-86,9,109,123,-93,83,110,-10,-97,-73,24,-81,20,114,89,99,-49,91,-87,82,-11,-49,16,-98,-124,97,90,40,40,21,58,86,43,52,127,38,-52,-10,13,-31,-23,-123,-34,-30,-82,-83,54,-43,15,-13,27,108,35,-39,-110,100,127,20,-125,-103,-81,-82,79,125,114,-97,-52,-101,60,-33,-93,-55,-42,37,107,115,-104,75,3,-26,-122,87,-30,99,-92,-28,-65,75,57,-62,55,23,104,-85,-108,113,96,111,52,-72,120,-21,71,47,96,-121,-71,1,84,17,51,-53,113,93,-85,-66,-101,21,-21,-66,-115,-71,43,102,39,-73,30,-100,22,23,77,73,-41,-116,-15,5,5,119,-68,-79,95,96,111,38,-49,-76,51,8,100,47,-86,109,-49,59,-28,-90,-94,66,49,-33,-106,-72,-123,-125,-80,15,-121,70,44,66,-81,86,-90,-57,-8,-104,-11,-51,-10,41,-32,-24,78,-12,44,35,-92,65,66,91,125,-25,-52,-122,4,23,-70,-128,84,66,5,-75,79,15,-76,96,-120,-77,107,-66,-52,87,-34,-84,-81,14,75,103,18,-86,17,0,49,66,10,47,-94,-96,-110,49,93,7,52,95,-127,-64,-53,43,97,117,88,-17,-81,-15,-111,-51,-62,109,-114,63,99,-23,-119,43,32,-61,115,-112,-12,-42,96,70,-106,70,18,27,-73,65,-80,-76,110,-104,57,-86,114,-33,34,125,97,-95,-35,38,101,-52,15,47,55,-21,122,39,57,-65,-58,-102,-119,75,-77,-86,-51,38,-100,-6,-89,-76,13,-21,59,92,-118,66,-47,42,-112,-61,96,93,-72,24,102,69,87,112,-105,-122,11,-6,64,-31,-51,95,67,93,-13,69,26,-70,123,-83,11,87,-31,-127,-67,80,4,-46,-22,21,-16,29,-60,50,-43,117,2,-44,-32,7,99,-38,48,-33,70,-115,11,-72,-72,-127,113,87,-36,31,-73,7,-27,64,-4,-7,-104,-102,117,125,81,31,-13,-20,50,-47,-68,103,93,97,9,-93,22,-111,20,102,106,-122,-48,60,-23,78,34,40,118,-72,-84,-109,98,-94,-97,-114,-59,-67,-16,19,90,99,-72,-65,-35,-80,45,105,-106,-56,106,6,-40,-23,-19,103,-29,-68,-35,-66,72,-89,4,35,-111,81,43};

      ciphertext.data = data;

      byte[] ivk = {-73,11,124,-48,-19,3,-53,-33,-41,-83,-87,80,46,-30,69,-79,62,86,-99,84,-91,113,-99,45,-86,15,95,20,81,71,-110,4};
      byte[] epk = {106,-40,-51,-28,83,31,126,109,44,-88,85,-99,77,-95,-10,126,-101,-114,-79,69,106,85,-26,109,26,76,42,79,68,78,-73,-86};
      byte[] cmu = {81,122,-106,2,-128,93,100,-43,117,-44,-54,32,-62,-91,-87,-3,37,-71,78,88,-59,73,-5,85,41,-86,-47,107,-100,3,112,9};

      Optional<NotePlaintext>  ret = decrypt(ciphertext, ivk, epk, cmu);
      NotePlaintext result = ret.get();

      System.out.println("\nplain text rcm:");
      for(byte b : result.rcm) {
        System.out.print(b + ",");
      }
      System.out.println();

      System.out.println("plain text memo size:" +  result.memo.length);
      for(byte b : result.memo) {
        System.out.print(b + ",");
      }
      System.out.println();

      System.out.println("plain text value:" + result.value);

      System.out.println("plain text d:");
      for(byte b : result.d.getData()) {
        System.out.print(b + ",");
      }
      System.out.println();

    }
  }
}
