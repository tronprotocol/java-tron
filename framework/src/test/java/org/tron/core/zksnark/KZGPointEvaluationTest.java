package org.tron.core.zksnark;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.tron.common.crypto.ckzg4844.CKZG4844JNI;
import org.tron.core.zen.KZGPointEvaluationInitService;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
public class KZGPointEvaluationTest {

  @Test
  public void testVerifyBlobKzgProof() {
    KZGPointEvaluationInitService.initCKZG4844();

    byte[] blob = new byte[131072];
    blob[0] = 0x01;
    byte[] commitment = CKZG4844JNI.blobToKzgCommitment(blob);
    byte[] expectedCommitment =
        Hex.decode("a70477b56251e8770969c83eaed665d3ab99b96b72270a41009f27"
            + "52b5c06a06bd089ad48952c12b1dbf83dccd9d373f");
    assertArrayEquals(expectedCommitment, commitment);

    byte[] proof = CKZG4844JNI.computeBlobKzgProof(blob, commitment);
    byte[] expectedProof =
        Hex.decode("b7f576f2442febaa035d3c6f34bbdad6acebcaec70236ff40b3373b"
            + "d2ca00547d3ca7bb1d0ed3e728ca9dab610a4cfa4");
    assertArrayEquals(expectedProof, proof);

    boolean isValidProof = CKZG4844JNI.verifyBlobKzgProof(blob, commitment, proof);
    assertTrue(isValidProof);

    KZGPointEvaluationInitService.freeSetup();
  }
}
