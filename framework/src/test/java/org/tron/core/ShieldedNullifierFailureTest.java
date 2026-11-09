package org.tron.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;

import com.google.protobuf.ByteString;
import java.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import org.tron.api.GrpcAPI;
import org.tron.common.zksnark.JLibrustzcash;
import org.tron.common.zksnark.LibrustzcashParam.ComputeNfParams;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.zen.address.DiversifierT;
import org.tron.core.zen.address.KeyIo;
import org.tron.core.zen.address.PaymentAddress;
import org.tron.protos.contract.ShieldContract.IncrementalMerkleTree;
import org.tron.protos.contract.ShieldContract.IncrementalMerkleVoucher;
import org.tron.protos.contract.ShieldContract.IncrementalMerkleVoucherInfo;
import org.tron.protos.contract.ShieldContract.PedersenHash;

public class ShieldedNullifierFailureTest {

  private boolean allowShieldedApi;

  @Before
  public void setUp() {
    allowShieldedApi = Args.getInstance().isAllowShieldedTransactionApi();
    Args.getInstance().setAllowShieldedTransactionApi(true);
  }

  @After
  public void tearDown() {
    Args.getInstance().setAllowShieldedTransactionApi(allowShieldedApi);
  }

  private static GrpcAPI.Note note() {
    PaymentAddress address = new PaymentAddress(
        new DiversifierT(Hex.decode("4c418cf65a7210d9b66588")),
        Hex.decode("4f05edbf1abe79773a46dbf16f43aa05290225487b656413533c405272b9bd5d"));
    return GrpcAPI.Note.newBuilder().setPaymentAddress(KeyIo.encodePaymentAddress(address))
        .setRcm(ByteString.copyFrom(new byte[32])).build();
  }

  private static IncrementalMerkleVoucher voucher() {
    return IncrementalMerkleVoucher.newBuilder().setTree(IncrementalMerkleTree.newBuilder()
        .setLeft(PedersenHash.newBuilder().setContent(ByteString.copyFrom(new byte[32])))).build();
  }

  private static GrpcAPI.NfParameters request() {
    return GrpcAPI.NfParameters.newBuilder().setNote(note()).setVoucher(voucher())
        .setAk(ByteString.copyFrom(new byte[32])).setNk(ByteString.copyFrom(new byte[32])).build();
  }

  @Test
  public void nullifierApiReportsFailureInsteadOfReturningNull() {
    try (MockedStatic<JLibrustzcash> nativeApi = mockStatic(JLibrustzcash.class)) {
      ZksnarkException error = assertThrows(ZksnarkException.class,
          () -> new Wallet().createShieldNullifier(request()));
      assertEquals("compute nullifier error", error.getMessage());
      nativeApi.verify(() -> JLibrustzcash.librustzcashComputeNf(any()));
    }
  }

  @Test
  public void failedNullifierNeverReachesSpentStore() throws Exception {
    Wallet wallet = spy(new Wallet());
    ChainBaseManager manager = mock(ChainBaseManager.class);
    ReflectionTestUtils.setField(wallet, "chainBaseManager", manager);
    doReturn(IncrementalMerkleVoucherInfo.newBuilder().addVouchers(voucher()).build())
        .when(wallet).getMerkleTreeVoucherInfo(any());
    GrpcAPI.NoteParameters request = GrpcAPI.NoteParameters.newBuilder().setNote(note())
        .setAk(ByteString.copyFrom(new byte[32])).setNk(ByteString.copyFrom(new byte[32]))
        .setTxid(ByteString.copyFrom(new byte[32])).build();
    try (MockedStatic<JLibrustzcash> nativeApi = mockStatic(JLibrustzcash.class)) {
      ZksnarkException error = assertThrows(ZksnarkException.class, () -> wallet.isSpend(request));
      assertEquals("compute nullifier error", error.getMessage());
      verifyNoInteractions(manager);
      nativeApi.verify(() -> JLibrustzcash.librustzcashComputeNf(any()));
    }
  }

  @Test
  public void nullifierApiPreservesSuccessfulResultIncludingZero() throws Exception {
    try (MockedStatic<JLibrustzcash> nativeApi = mockStatic(JLibrustzcash.class)) {
      for (byte fill : new byte[]{0, 7}) {
        byte[] expected = new byte[32];
        Arrays.fill(expected, fill);
        nativeApi.when(() -> JLibrustzcash.librustzcashComputeNf(any())).thenAnswer(invocation -> {
          ComputeNfParams params = invocation.getArgument(0);
          System.arraycopy(expected, 0, params.getResult(), 0, expected.length);
          return true;
        });
        assertArrayEquals(expected,
            new Wallet().createShieldNullifier(request()).getValue().toByteArray());
      }
    }
  }
}
