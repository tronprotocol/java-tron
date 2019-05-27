package org.tron.core.zen.note;

import static org.tron.core.zen.note.ZenChainParams.ZC_JUBJUB_POINT_SIZE;
import static org.tron.core.zen.note.ZenChainParams.ZC_JUBJUB_SCALAR_SIZE;
import static org.tron.core.zen.note.ZenChainParams.ZC_OUTPLAINTEXT_SIZE;

import java.util.Optional;
import lombok.AllArgsConstructor;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.zen.note.NoteEncryption.Encryption;
import org.tron.core.zen.note.NoteEncryption.Encryption.OutCiphertext;
import org.tron.core.zen.note.NoteEncryption.Encryption.OutPlaintext;

@AllArgsConstructor
public class OutgoingPlaintext {

  public byte[] pk_d;
  public byte[] esk;

  public static Optional<OutgoingPlaintext> decrypt(OutCiphertext ciphertext, byte[] ovk,
      byte[] cv, byte[] cm, byte[] epk) throws ZksnarkException{
    Optional<OutPlaintext> pt = Encryption
        .AttemptOutDecryption(ciphertext, ovk, cv, cm, epk);
    if (!pt.isPresent()) {
      return Optional.empty();
    }
    OutgoingPlaintext ret = OutgoingPlaintext.decode(pt.get());
    return Optional.of(ret);
  }

  public OutCiphertext encrypt(byte[] ovk, byte[] cv, byte[] cm, NoteEncryption enc)
      throws ZksnarkException{
    OutPlaintext pt = this.encode();
    return enc.encryptToOurselves(ovk, cv, cm, pt);
  }

  private OutPlaintext encode() {
    OutPlaintext ret = new OutPlaintext();
    ret.data = new byte[ZC_OUTPLAINTEXT_SIZE];
    System.arraycopy(pk_d, 0, ret.data, 0, ZC_JUBJUB_SCALAR_SIZE);
    System.arraycopy(esk, 0, ret.data, ZC_JUBJUB_SCALAR_SIZE, ZC_JUBJUB_POINT_SIZE);
    return ret;
  }

  private static OutgoingPlaintext decode(OutPlaintext outPlaintext) {
    byte[] data = outPlaintext.data;
    OutgoingPlaintext ret = new OutgoingPlaintext(new byte[ZC_JUBJUB_SCALAR_SIZE],
        new byte[ZC_JUBJUB_POINT_SIZE]);
    // ZC_OUTPLAINTEXT_SIZE = (ZC_JUBJUB_POINT_SIZE + ZC_JUBJUB_SCALAR_SIZE)
    System.arraycopy(data, 0, ret.pk_d, 0, ZC_JUBJUB_SCALAR_SIZE);
    System.arraycopy(data, ZC_JUBJUB_SCALAR_SIZE, ret.esk, 0, ZC_JUBJUB_POINT_SIZE);
    return ret;
  }

  public static void main(String[] args) throws ZksnarkException {
    OutCiphertext outCiphertext = new OutCiphertext();
    byte[] data = {5, -111, -54, 98, -101, -92, 109, 120, -84, -118, -81, 35, -25, 41, 107, -74,
        -47, 84, 55, -53, 44, -1, -13, -69, 61, -101, 51, -100, -2, 118, 88, 109, 22, -126, 88, 89,
        -119, -93, 74, 82, -66, -94, -48, 3, -102, -118, 99, -9, 105, -9, 103, -84, -81, -19, -120,
        -106, 9, -89, -92, 89, 123, 91, 100, 85, 99, 51, 13, 7, 25, -11, 13, 63, -63, -111, -12, 87,
        17, 68, 124, -115};

    outCiphertext.data = data;

    byte[] ovk = {-115, -81, 92, 25, -119, -37, 45, 84, 121, 46, -43, -88, -1, 66, 36, 58, -10, -11,
        22, -115, 29, -34, -12, 62, 15, -26, -42, 120, 31, 81, 1, -36};
    byte[] cv = {-68, -52, 127, -32, -24, 94, 95, 16, -8, -67, 106, -3, 98, 12, 46, -15, 36, 17,
        -12, 47, -103, -45, 94, 59, -59, -46, 126, 22, -48, -33, -62, 71};
    byte[] cm = {20, 23, -9, 124, 1, -71, 80, -43, 23, -15, -73, 44, -18, -17, 4, 84, 114, 46, 41,
        92, 87, -52, 62, 89, -44, -61, -127, -63, 110, 88, 21, -70};
    byte[] epk = {17, 110, 18, 94, -116, 26, 68, -70, 73, 113, -32, -86, -33, 26, -73, 74, 46, -116,
        -92, 107, 33, 60, -87, -20, 66, -121, -30, -49, -10, -29, -16, 22};

    Optional<OutgoingPlaintext> ret = decrypt(outCiphertext, ovk, cv, cm, epk);
    OutgoingPlaintext result = ret.get();

    System.out.println("plain text pkD size:" + result.pk_d.length);
    for (byte b : result.pk_d) {
      System.out.print(b + ",");
    }
    System.out.println();

    System.out.println("plain text pkD size:" + result.esk.length);
    for (byte b : result.esk) {
      System.out.print(b + ",");
    }
    System.out.println();

    Encryption.EncCiphertext ciphertext = new Encryption.EncCiphertext();
    byte[] data2 = {11, -22, -42, 12, 43, 55, 93, 0, 125, -5, 70, 123, -31, 13, 22, 67, 104, 126,
        -81, 83, -120, -112, 64, 35, -117, 90, 22, -21, 90, 99, -79, 18, 78, 109, 42, 70, 73, -3,
        -53, -116, -9, 11, -75, 83, -71, 39, -112, -74, 97, 104, 82, 66, -9, -30, 91, 90, 18, 64,
        -125, -43, 25, 3, 63, -49, -106, -127, 76, 42, 78, 76, -95, -115, -31, -7, -17, -115, -59,
        41, 23, -31, 97, 84, -8, 85, -9, -29, 89, 3, 53, 81, -19, 73, -40, 114, 67, 120, -68, -96,
        11, -43, -28, 90, 41, 86, -71, -43, -89, -84, -53, -74, 59, 28, 51, 59, 69, -71, 35, -117,
        -102, 119, 47, -31, -30, 14, -42, -7, 115, 14, -109, 17, 105, 55, -38, 73, 101, 108, -116,
        111, 17, 86, 31, 70, 36, -71, 17, -115, 74, 64, -7, 85, 79, 25, -39, -12, 3, -24, 11, 93,
        -113, 60, 68, -18, 99, -4, 6, 42, 56, 31, -72, 112, -71, -24, 29, 70, 22, -49, -92, 67,
        -126, -26, -101, -8, 70, 16, -17, -30, -14, 0, 93, 79, 20, 122, 47, 83, 11, -122, -2, -128,
        -102, 93, -47, 15, -89, -106, -50, 59, -17, 72, 108, 86, -103, -27, -86, 47, 89, -14, -76,
        -110, -21, -82, 17, 2, 122, -95, -112, -74, 37, -81, -88, -112, -38, 26, 0, 93, 31, -20, 44,
        101, 31, -65, -48, -25, 113, 101, -85, 105, -102, 22, 83, -73, -20, -87, 52, -59, 77, 69,
        -70, 39, -24, 88, 27, -34, -52, -50, 29, -14, -31, 57, -13, -39, 42, 25, -109, 45, 10, -80,
        -102, 20, 21, 26, -93, 61, 103, 117, -27, -106, -114, 33, -50, 105, 66, -29, 83, -57, -108,
        -19, 95, 29, 101, 81, -113, 124, 67, 72, 76, 18, -91, -62, 33, -20, -85, -47, -82, 109, 33,
        27, 80, -117, -1, 103, -86, 100, 95, -91, 55, -117, 72, -34, 41, 88, 82, 3, -97, 54, -28,
        -21, -81, -23, 127, 116, 83, -8, 61, 87, 59, -105, 85, -36, -23, 54, 22, -25, 90, -70, 40,
        -43, -23, 97, 3, 45, 6, 34, 106, 52, -73, 95, -107, 25, -36, -47, -53, -81, -22, -60, -62,
        5, 54, -90, -71, -33, 30, 110, 90, 77, -87, -124, 5, 103, 45, 55, -124, 42, 62, 17, -68, 68,
        119, 26, 9, -128, -30, -29, -9, -105, -8, 7, 93, 5, 32, 121, 83, 105, -95, 109, 11, 85, -23,
        65, -103, -86, -102, -65, 25, 106, 12, 77, -87, -4, -83, 88, 102, 63, -92, 52, 25, 53, -50,
        -100, -88, 17, 55, -23, 27, 58, -25, -75, -24, -21, -114, -30, 106, -43, 117, -54, -31, -99,
        26, -3, 121, -17, -115, -32, 18, 57, 26, 32, -65, -28, -17, 55, 22, 83, 38, -118, 102, 65,
        41, -113, 114, -99, -105, 110, 75, 39, 39, -60, 24, -87, 117, 16, -36, 25, 108, -123, -12,
        -81, 23, -35, 45, 47, 38, -114, 104, 125, 88, -43, 117, -63, -125, -69, -122, -84, -98, 42,
        -42, 29, 29, 63, -1, 94, 44, -53, 18, 124, -85, -45, -124, 82, 112, 4, -28, -75, -80, 65,
        -36, 62, 89, 81, 98, -22, 79, 49, -90, 90, 68, -86, 124, -89, -80, -77, 37, 113, 40, 121,
        -85, -20, 96, 2, 98, 103, 17, -46, 107, 80, -47, -12, 15, -63, -125, -97, -84, -1, 26, -126,
        113, -89, -119, -102, -25, -115};
    ciphertext.data = data2;

    byte[] cmu = {-122, 33, 122, -36, -48, -82, 25, 8, -10, 52, 30, -68, -62, -77, 116, -66, -121,
        -113, 126, 7, -93, 42, -58, -111, 118, 14, 72, -50, 115, -59, -6, 14};

    Optional<NotePlaintext> ret2 = NotePlaintext
        .decrypt(ciphertext, epk, result.esk, result.pk_d, cmu);
    NotePlaintext result2 = ret2.get();

    System.out.println("\n result2 rcm:");
    for (byte b : result2.rcm) {
      System.out.print(b + ",");
    }
    System.out.println();

    System.out.println("result2 memo size:" + result2.memo.length);
    for (byte b : result2.memo) {
      System.out.print(b + ",");
    }
    System.out.println();

    System.out.println("result2 value:" + result2.value);

    System.out.println("result2 d:");
    for (byte b : result2.d.getData()) {
      System.out.print(b + ",");
    }
    System.out.println();

    return;
  }
}
