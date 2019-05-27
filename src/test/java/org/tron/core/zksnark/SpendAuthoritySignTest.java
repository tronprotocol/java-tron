package org.tron.core.zksnark;

import com.sun.jna.Pointer;
import org.junit.Test;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.zksnark.Librustzcash;
import org.tron.common.zksnark.LibrustzcashParam.SaplingCheckSpendParams;
import org.tron.common.zksnark.LibrustzcashParam.SaplingSpendSigParams;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.zen.address.SpendingKey;

public class SpendAuthoritySignTest {

  private String text = "this a test transaction";

  public byte[] getHash() {
    return Sha256Hash.of(text.getBytes()).getBytes();
  }


  public SpendingKey getKey() {
    SpendingKey sk = SpendingKey
        .decode("0b862f0e70048551c08518ff49a19db027d62cdeeb2fa974db91c10e6ebcdc16");
    return sk;
  }

  public byte[] getAlpha() throws ZksnarkException {
    byte[] alpha = new byte[32];
    Librustzcash.librustzcashSaplingGenerateR(alpha);
    return alpha;
  }

  @Test
  public void sign() throws ZksnarkException {
    byte[] result = new byte[64];
    Librustzcash.librustzcashSaplingSpendSig(
        new SaplingSpendSigParams(getKey().expandedSpendingKey().getAsk(),
            getAlpha(),
            getHash(),
            result));
    System.out.println(ByteUtil.toHexString(result));

    Pointer ctx = Librustzcash.librustzcashSaplingVerificationCtxInit();

    Librustzcash.librustzcashSaplingCheckSpend(
        new SaplingCheckSpendParams(ctx,
            null,
            null,
            null,
            null,
            null,
            result,
            getHash())
    );

//    Librustzcash.librustzcashSaplingCheckSpend(
//        ctx,
//        spendDescription.getValueCommitment().toByteArray(),
//        spendDescription.getAnchor().toByteArray(),
//        spendDescription.getNullifier().toByteArray(),
//        spendDescription.getRk().toByteArray(),
//        spendDescription.getZkproof().toByteArray(),
//        spendDescription.getSpendAuthoritySignature().toByteArray(),
//        signHash
//    )
  }

}
