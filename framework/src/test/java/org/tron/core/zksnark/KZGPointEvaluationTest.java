package org.tron.core.zksnark;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mockStatic;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.tron.common.crypto.ckzg4844.CKZG4844JNI;
import org.tron.common.crypto.ckzg4844.CKZGException;
import org.tron.common.crypto.ckzg4844.CellsAndProofs;
import org.tron.common.crypto.ckzg4844.ProofAndY;
import org.tron.core.exception.TronError;
import org.tron.core.zen.KZGPointEvaluationInitService;

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
    Assert.assertArrayEquals(expectedCommitment, commitment);

    byte[] proof = CKZG4844JNI.computeBlobKzgProof(blob, commitment);
    byte[] expectedProof =
        Hex.decode("b7f576f2442febaa035d3c6f34bbdad6acebcaec70236ff40b3373b"
            + "d2ca00547d3ca7bb1d0ed3e728ca9dab610a4cfa4");
    Assert.assertArrayEquals(expectedProof, proof);

    boolean isValidProof = CKZG4844JNI.verifyBlobKzgProof(blob, commitment, proof);
    Assert.assertTrue(isValidProof);

    byte[] z =
        Hex.decode("0000000000000000000000000000000000000000000000000000000000000065");
    byte[] y =
        Hex.decode("60f557194475973322b33dc989896381844508234bfa6fbeefe5fa165ae15a0a");
    ProofAndY proofAndY = CKZG4844JNI.computeKzgProof(blob, z);
    byte[] expectedZProof =
        Hex.decode("879f9a41956deae578bc65e7133f164394b8677bc2e7b1356be61"
            + "d47720ed2a3326bfddebc67cd37ee9e7537d7814afe");
    Assert.assertArrayEquals(y, proofAndY.getY());
    Assert.assertArrayEquals(expectedZProof, proofAndY.getProof());

    CellsAndProofs cellsAndProofs = CKZG4844JNI.computeCellsAndKzgProofs(blob);
    byte[] blockCells = Arrays.copyOfRange(cellsAndProofs.getCells(), 0, 2048);
    byte[] blockProof = Arrays.copyOfRange(cellsAndProofs.getProofs(), 0, 48);
    long[] cellIndices = new long[1];
    boolean isValid =
        CKZG4844JNI.verifyCellKzgProofBatch(commitment, cellIndices, blockCells, blockProof);
    Assert.assertTrue(isValid);

    long[] errorCellIndices = new long[2];
    errorCellIndices[1] = 1;
    CKZGException expectedE = new CKZGException(1,
        "Invalid cellIndices size. Expected 1 bytes but got 2.");
    try {
      CKZG4844JNI.verifyCellKzgProofBatch(commitment, errorCellIndices, blockCells, blockProof);
    } catch (Exception e) {
      Assert.assertEquals(CKZGException.class, e.getClass());
      CKZGException exception = (CKZGException) e;
      Assert.assertEquals(expectedE.getError(), exception.getError());
      Assert.assertEquals(expectedE.getErrorMessage(), exception.getErrorMessage());
    }

    try {
      String testSetupResource = "/kzg-trusted-setups/test.txt";
      CKZG4844JNI.loadTrustedSetupFromResource(
          testSetupResource, KZGPointEvaluationTest.class, 0);
    } catch (Exception e) {
      Assert.assertEquals(RuntimeException.class, e.getClass());
      Assert.assertEquals(
          "Trusted Setup is already loaded. Free it before loading a new one.",
          e.getMessage());
    }

    KZGPointEvaluationInitService.freeSetup();

    try (MockedStatic<CKZG4844JNI> mock = mockStatic(CKZG4844JNI.class)) {
      mock.when(CKZG4844JNI::loadNativeLibrary).thenThrow(new RuntimeException());
      TronError thrown = assertThrows(TronError.class,
          KZGPointEvaluationInitService::initCKZG4844);
      assertEquals(TronError.ErrCode.CKZG_INIT, thrown.getErrCode());
    }
  }
}
