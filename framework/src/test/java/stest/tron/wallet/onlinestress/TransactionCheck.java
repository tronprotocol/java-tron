package stest.tron.wallet.onlinestress;

import com.google.protobuf.ByteString;
import org.testng.annotations.Test;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.crypto.SignUtils;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Sha256Hash;

public class TransactionCheck {

  @Test
  public void hexToTransaction() throws Exception {
    String targetHex1 = "";
    String targetHex2 = "";
    String hex = targetHex1;
    org.tron.protos.Protocol.Transaction transaction = org.tron.protos.Protocol.Transaction
        .parseFrom(ByteArray.fromHexString(hex));
    getBase64FromByteString(transaction.getSignature(0));
    String base64 = getBase64FromByteString(transaction.getSignature(0));
    byte[] address = SignUtils
        .signatureToAddress((Sha256Hash
            .hash(CommonParameter.getInstance().isECKeyCryptoEngine(),
                transaction.getRawData().toByteArray())), base64,
            CommonParameter.getInstance().isECKeyCryptoEngine());
    String addressStr = WalletClient.encode58Check(address);
    String data = String.valueOf(transaction.getRawData().getData().toStringUtf8());
    System.out.println(addressStr);
    System.out.println(data);
  }


  /**
   * constructor.
   */
  public static String getBase64FromByteString(ByteString sign) {
    byte[] r = sign.substring(0, 32).toByteArray();
    byte[] s = sign.substring(32, 64).toByteArray();
    byte v = sign.byteAt(64);
    if (v < 27) {
      v += 27; //revId -> v
    }
    ECDSASignature signature = ECDSASignature.fromComponents(r, s, v);
    return signature.toBase64();
  }




}
