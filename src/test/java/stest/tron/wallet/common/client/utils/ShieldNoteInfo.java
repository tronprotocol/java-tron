package stest.tron.wallet.common.client.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.utils.ByteArray;

@AllArgsConstructor
public class ShieldNoteInfo {
  @Setter
  @Getter
  public long value = 0;
  @Setter
  @Getter
  public String paymentAddress;
  @Setter
  @Getter
  public byte[] r; // 256
  @Setter
  @Getter
  public String trxId;
  @Setter
  @Getter
  public int index;
  @Setter
  @Getter
  public long noteIndex;
  @Setter
  @Getter
  public  byte[] memo;

  public ShieldNoteInfo(){
  }

  /**
   * format shield note to a string.
   * @return
   */
  public String encode() {
    String encodeString = noteIndex + ";";
    encodeString += paymentAddress;
    encodeString += ";";
    encodeString += ByteArray.toHexString(r);
    encodeString += ";";
    encodeString += trxId;
    encodeString += ";";
    encodeString += String.valueOf(value);
    encodeString += ";";
    encodeString += String.valueOf(index);
    encodeString += ";";
    encodeString += ByteArray.toHexString(memo);
    return encodeString;
  }

  /**
   * constructor.
   */
  public boolean decode(final String data) {
    String[] sourceStrArray = data.split(";");
    if (sourceStrArray.length != 7) {
      System.out.println("len is not right.");
      return false;
    }
    noteIndex = Long.valueOf(sourceStrArray[0]);
    paymentAddress = sourceStrArray[1];
    r = ByteArray.fromHexString(sourceStrArray[2]);
    trxId = sourceStrArray[3];
    value = Long.valueOf(sourceStrArray[4]);
    index = Integer.valueOf(sourceStrArray[5]);
    memo = ByteArray.fromHexString(sourceStrArray[6]);
    return true;
  }

}

