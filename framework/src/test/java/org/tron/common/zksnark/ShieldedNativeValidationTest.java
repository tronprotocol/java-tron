package org.tron.common.zksnark;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.tron.common.zksnark.LibrustzcashParam.OutputProofParams;
import org.tron.common.zksnark.LibrustzcashParam.SpendProofParams;
import org.tron.common.zksnark.LibrustzcashParam.Zip32XfvkAddressParams;
import org.tron.common.zksnark.LibrustzcashParam.Zip32XskDeriveParams;
import org.tron.common.zksnark.LibrustzcashParam.Zip32XskMasterParams;
import org.tron.core.exception.ZksnarkException;

public class ShieldedNativeValidationTest {

  private Librustzcash original;
  private Librustzcash nativeApi;

  @Before
  public void setUp() {
    original = (Librustzcash) ReflectionTestUtils.getField(JLibrustzcash.class, "INSTANCE");
    nativeApi = mock(Librustzcash.class);
    ReflectionTestUtils.setField(JLibrustzcash.class, "INSTANCE", nativeApi);
  }

  @After
  public void tearDown() {
    ReflectionTestUtils.setField(JLibrustzcash.class, "INSTANCE", original);
  }

  private static byte[] validAk() {
    return Hex.decode("db8c9492f599646369a4855b4260fe3b957cec8e1c33de7036ec61d8059d52d1");
  }

  private static byte[] identity(boolean sign) {
    byte[] ak = new byte[32];
    ak[0] = 1;
    ak[31] = sign ? (byte) 0x80 : 0;
    return ak;
  }

  private static SpendProofParams spend(byte[] ak) throws ZksnarkException {
    byte[] path = new byte[1065];
    path[0] = 32;
    for (int i = 0; i < 32; i++) {
      path[1 + i * 33] = 32;
    }
    return new SpendProofParams(1, ak, new byte[32], new byte[11], new byte[32],
        new byte[32], 0, new byte[32], path, new byte[32], new byte[32], new byte[192]);
  }

  @Test
  public void rejectsBothIdentityEncodingsBeforeNativeProof() {
    for (boolean sign : new boolean[]{false, true}) {
      ZksnarkException error = assertThrows(ZksnarkException.class,
          () -> spend(identity(sign)));
      assertEquals("Invalid spend ak: identity point", error.getMessage());
    }
    verifyNoInteractions(nativeApi);
  }

  @Test
  public void acceptsNonIdentityWithByte30SetForBothSignBits() throws ZksnarkException {
    for (boolean sign : new boolean[]{false, true}) {
      byte[] ak = identity(sign);
      ak[30] = 1;
      // Only the Java identity filter is under test, not native point or subgroup validity.
      LibrustzcashParam.validSpendAk(ak);
    }
    verifyNoInteractions(nativeApi);
  }

  @Test
  public void rejectsInvalidAkLengths() {
    for (byte[] ak : new byte[][]{null, new byte[0], new byte[31], new byte[33]}) {
      assertThrows(ZksnarkException.class, () -> spend(ak));
    }
    verifyNoInteractions(nativeApi);
  }

  @Test
  public void revalidatesSetterAndArrayMutationsAtNativeBoundary() throws Exception {
    SpendProofParams params = spend(validAk());
    params.setAk(identity(true));
    assertThrows(ZksnarkException.class,
        () -> JLibrustzcash.librustzcashSaplingSpendProof(params));

    byte[] ak = validAk();
    SpendProofParams aliased = spend(ak);
    System.arraycopy(identity(false), 0, ak, 0, ak.length);
    assertThrows(ZksnarkException.class,
        () -> JLibrustzcash.librustzcashSaplingSpendProof(aliased));
    verifyNoInteractions(nativeApi);
  }

  @Test
  public void rejectsIdentityDerivedFromZeroAsk() {
    byte[] ak = new byte[32];
    original.librustzcashAskToAk(new byte[32], ak);
    assertThrows(ZksnarkException.class, () -> spend(ak));
    verifyNoInteractions(nativeApi);
  }

  @Test
  public void acceptsNormalAkWithSignBitAndPreservesNativeFailure() throws Exception {
    SpendProofParams params = spend(validAk());
    when(nativeApi.librustzcashSaplingSpendProof(
        org.mockito.ArgumentMatchers.anyLong(), any(), any(), any(), any(), any(),
        org.mockito.ArgumentMatchers.anyLong(), any(), any(), any(), any(), any()))
        .thenReturn(true, false);
    assertTrue(JLibrustzcash.librustzcashSaplingSpendProof(params));
    org.junit.Assert.assertFalse(JLibrustzcash.librustzcashSaplingSpendProof(params));
    org.junit.Assert.assertArrayEquals(validAk(), params.getAk());
  }

  private static OutputProofParams output() throws ZksnarkException {
    return new OutputProofParams(1, new byte[32], new byte[11], new byte[32],
        new byte[32], 0, new byte[32], new byte[192]);
  }

  @Test
  public void revalidatesOutputParametersBeforeNativeCalls() throws Exception {
    for (byte[] d : new byte[][]{null, new byte[10], new byte[12]}) {
      OutputProofParams params = output();
      params.setD(d);
      assertThrows(ZksnarkException.class,
          () -> JLibrustzcash.librustzcashSaplingOutputProof(params));
    }
    OutputProofParams shortProof = output();
    shortProof.setZkproof(new byte[191]);
    assertThrows(ZksnarkException.class,
        () -> JLibrustzcash.librustzcashSaplingOutputProof(shortProof));
    OutputProofParams negativeValue = output();
    negativeValue.setValue(-1);
    assertThrows(ZksnarkException.class,
        () -> JLibrustzcash.librustzcashSaplingOutputProof(negativeValue));
    verifyNoInteractions(nativeApi);
  }

  @Test
  public void rejectsInvalidDiversifierBeforeNativeOutputProof() throws Exception {
    OutputProofParams params = output();
    when(nativeApi.librustzcashCheckDiversifier(params.getD())).thenReturn(false);

    ZksnarkException error = assertThrows(ZksnarkException.class,
        () -> JLibrustzcash.librustzcashSaplingOutputProof(params));

    assertEquals("Invalid output diversifier", error.getMessage());
    verify(nativeApi).librustzcashCheckDiversifier(params.getD());
    verifyNoMoreInteractions(nativeApi);
  }

  @Test
  public void acceptsValidDiversifierAndPreservesNativeOutputProofResult() throws Exception {
    OutputProofParams params = output();
    when(nativeApi.librustzcashCheckDiversifier(params.getD())).thenReturn(true);
    when(nativeApi.librustzcashSaplingOutputProof(params.getCtx(), params.getEsk(),
        params.getD(), params.getPkD(), params.getR(), params.getValue(), params.getCv(),
        params.getZkproof())).thenReturn(true, false);

    assertTrue(JLibrustzcash.librustzcashSaplingOutputProof(params));
    assertFalse(JLibrustzcash.librustzcashSaplingOutputProof(params));
  }

  @Test
  public void validatesZip32MasterSeedBoundsAndRechecksMutation() throws Exception {
    assertThrows(ZksnarkException.class,
        () -> new Zip32XskMasterParams(null, 0, new byte[169]));
    assertThrows(ZksnarkException.class,
        () -> new Zip32XskMasterParams(new byte[32], -1, new byte[169]));
    assertThrows(ZksnarkException.class,
        () -> new Zip32XskMasterParams(new byte[32], 33, new byte[169]));
    Zip32XskMasterParams params = new Zip32XskMasterParams(new byte[32], 32, new byte[169]);
    params.setSize(33);
    assertThrows(ZksnarkException.class,
        () -> JLibrustzcash.librustzcashZip32XskMaster(params));
    verifyNoInteractions(nativeApi);
  }

  @Test
  public void preservesEmptyAndPrefixSeedsAndHardenedChildIndices() throws Exception {
    byte[] output = new byte[169];
    byte[] seed = new byte[32];
    JLibrustzcash.librustzcashZip32XskMaster(new Zip32XskMasterParams(seed, 16, output));
    verify(nativeApi).librustzcashZip32XskMaster(seed, 16, output);
    byte[] emptySeed = new byte[0];
    JLibrustzcash.librustzcashZip32XskMaster(new Zip32XskMasterParams(emptySeed, 0, output));
    verify(nativeApi).librustzcashZip32XskMaster(emptySeed, 0, output);
    byte[] parent = new byte[169];
    JLibrustzcash.librustzcashZip32XskDerive(
        new Zip32XskDeriveParams(parent, Integer.MIN_VALUE, output));
    verify(nativeApi).librustzcashZip32XskDerive(parent, Integer.MIN_VALUE, output);
  }

  @Test
  public void rejectsShortZip32KeysAndOutputBuffers() throws Exception {
    assertThrows(ZksnarkException.class,
        () -> new Zip32XskDeriveParams(new byte[168], 0, new byte[169]));
    assertThrows(ZksnarkException.class,
        () -> new Zip32XskDeriveParams(new byte[169], 0, new byte[168]));
    for (int field = 0; field < 4; field++) {
      byte[][] arrays = {new byte[169], new byte[11], new byte[11], new byte[43]};
      arrays[field] = Arrays.copyOf(arrays[field], arrays[field].length - 1);
      assertThrows(ZksnarkException.class,
          () -> new Zip32XfvkAddressParams(arrays[0], arrays[1], arrays[2], arrays[3]));
    }
    Zip32XskDeriveParams derive = new Zip32XskDeriveParams(new byte[169], 0, new byte[169]);
    derive.setData(new byte[1]);
    assertThrows(ZksnarkException.class,
        () -> JLibrustzcash.librustzcashZip32XskDerive(derive));
    Zip32XfvkAddressParams address =
        new Zip32XfvkAddressParams(new byte[169], new byte[11], new byte[11], new byte[43]);
    address.setAddr_ret(new byte[1]);
    assertThrows(ZksnarkException.class,
        () -> JLibrustzcash.librustzcashZip32XfvkAddress(address));
    verifyNoInteractions(nativeApi);
  }
}
