package stest.tron.wallet.common.client.utils;

import io.netty.util.internal.StringUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.utils.Base58;
import org.tron.common.utils.ByteArray;

@AllArgsConstructor
public class ShieldedTRC20NoteInfo {
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
  public long position;
  @Setter
  @Getter
  public byte[] memo;

  public ShieldedTRC20NoteInfo() {
  }

  /**
   * format shieldedTRC20 note to a string
   *
   * @return
   */
  public String encode(byte[] encryptKey) throws CipherException {
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
    encodeString += String.valueOf(position);
    encodeString += ";";
    String stringMemo = ByteArray.toHexString(memo);
    if (StringUtil.isNullOrEmpty(stringMemo)) {
      encodeString += "null";
    } else {
      encodeString += stringMemo;
    }
    byte[] chipherText = ZenUtils.aesCtrEncrypt(encodeString.getBytes(), encryptKey);
    encodeString = Base58.encode(chipherText);
    return encodeString;
  }

  /**
   * parse string to get shieldedTRC20 note
   *
   * @param data
   * @return
   */
  public boolean decode(String data, byte[] encryptKey) throws CipherException {
    byte[] chipherText = Base58.decode(data);
    byte[] text = ZenUtils.aesCtrDecrypt(chipherText, encryptKey);
    data = new String(text);

    String[] sourceStrArray = data.split(";");
    if (sourceStrArray.length != 8) {
      System.out.println("len is not right.");
      return false;
    }
    noteIndex = Long.valueOf(sourceStrArray[0]);
    paymentAddress = sourceStrArray[1];
    r = ByteArray.fromHexString(sourceStrArray[2]);
    trxId = sourceStrArray[3];
    value = Long.valueOf(sourceStrArray[4]);
    index = Integer.valueOf(sourceStrArray[5]);
    position = Long.valueOf(sourceStrArray[6]);
    if (sourceStrArray[7].equals("null")) {
      memo = ByteArray.fromHexString("");
    } else {
      memo = ByteArray.fromHexString(sourceStrArray[7]);
    }
    return true;
  }
}
