package org.tron.core.zen;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import java.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.tron.api.GrpcAPI.SpendAuthSigParameters;
import org.tron.common.zksnark.JLibrustzcash;
import org.tron.common.zksnark.LibrustzcashParam.SpendSigParams;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.zen.address.DiversifierT;
import org.tron.core.zen.address.ExpandedSpendingKey;
import org.tron.core.zen.address.FullViewingKey;
import org.tron.core.zen.note.Note;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

public class ShieldedSignatureFailureTest {

  private static final long CONTEXT = 123L;
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

  private static SpendAuthSigParameters signatureRequest() {
    ByteString scalar = ByteString.copyFrom(new byte[32]);
    return SpendAuthSigParameters.newBuilder()
        .setAsk(scalar).setAlpha(scalar).setTxHash(scalar).build();
  }

  @Test
  public void walletRejectsFailedSignatureInsteadOfReturningZeroBuffer() {
    try (MockedStatic<JLibrustzcash> nativeApi = mockStatic(JLibrustzcash.class)) {
      ZksnarkException error = assertThrows(ZksnarkException.class,
          () -> new Wallet().createSpendAuthSig(signatureRequest()));
      assertEquals("Spend signature generation failed", error.getMessage());
      nativeApi.verify(() -> JLibrustzcash.librustzcashSaplingSpendSig(any()));
    }
  }

  @Test
  public void walletReturnsSuccessfulNativeSignatureUnchanged() throws Exception {
    byte[] expected = new byte[64];
    Arrays.fill(expected, (byte) 7);
    try (MockedStatic<JLibrustzcash> nativeApi = mockStatic(JLibrustzcash.class)) {
      nativeApi.when(() -> JLibrustzcash.librustzcashSaplingSpendSig(any()))
          .thenAnswer(invocation -> {
            SpendSigParams params = invocation.getArgument(0);
            System.arraycopy(expected, 0, params.getResult(), 0, expected.length);
            return true;
          });
      assertArrayEquals(expected,
          new Wallet().createSpendAuthSig(signatureRequest()).getValue().toByteArray());
    }
  }

  private static ExpandedSpendingKey spendingKey() throws Exception {
    ExpandedSpendingKey key = mock(ExpandedSpendingKey.class);
    byte[] ak = Hex.decode("db8c9492f599646369a4855b4260fe3b957cec8e1c33de7036ec61d8059d52d1");
    when(key.fullViewingKey()).thenReturn(new FullViewingKey(ak, new byte[32], new byte[32]));
    when(key.getAsk()).thenReturn(new byte[32]);
    when(key.getNsk()).thenReturn(new byte[32]);
    return key;
  }

  private static Note spendNote() throws Exception {
    Note note = mock(Note.class);
    when(note.cm()).thenReturn(new byte[32]);
    when(note.nullifier(any(FullViewingKey.class), anyLong())).thenReturn(new byte[32]);
    when(note.getD()).thenReturn(new DiversifierT());
    when(note.getRcm()).thenReturn(new byte[32]);
    return note;
  }

  private static void allowSpendProof(MockedStatic<JLibrustzcash> nativeApi) {
    nativeApi.when(JLibrustzcash::librustzcashSaplingProvingCtxInit).thenReturn(CONTEXT);
    nativeApi.when(() -> JLibrustzcash.librustzcashSaplingSpendProof(any())).thenReturn(true);
  }

  private static Wallet transactionWallet() {
    Wallet wallet = mock(Wallet.class);
    when(wallet.createTransactionCapsuleWithoutValidate(any(Message.class),
        eq(ContractType.ShieldedTransferContract), anyLong())).thenAnswer(invocation ->
        new TransactionCapsule((Message) invocation.getArgument(0),
            ContractType.ShieldedTransferContract));
    return wallet;
  }

  @Test
  public void trc20BuildStopsOnSpendSignatureFailureAndFreesContext() throws Exception {
    ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder("transfer");
    builder.setShieldedTRC20Address(new byte[20]);
    builder.addSpend(spendingKey(), spendNote(), new byte[32], new byte[32],
        new byte[1024], 0);
    try (MockedStatic<JLibrustzcash> nativeApi = mockStatic(JLibrustzcash.class)) {
      allowSpendProof(nativeApi);
      ZksnarkException error = assertThrows(ZksnarkException.class, () -> builder.build(true));
      assertTrue(error.getMessage().contains("Spend signature generation failed"));
      assertTrue(builder.getBuilder().getSpendDescription(0)
          .getSpendAuthoritySignature().isEmpty());
      nativeApi.verify(() -> JLibrustzcash.librustzcashSaplingBindingSig(any()), never());
      nativeApi.verify(() -> JLibrustzcash.librustzcashSaplingProvingCtxFree(CONTEXT));
    }
  }

  @Test
  public void zenBuildStopsOnSpendSignatureFailureAndFreesContext() throws Exception {
    ZenTransactionBuilder builder = new ZenTransactionBuilder(transactionWallet());
    org.tron.common.zksnark.IncrementalMerkleVoucherContainer voucher =
        mock(org.tron.common.zksnark.IncrementalMerkleVoucherContainer.class,
            org.mockito.Answers.RETURNS_DEEP_STUBS);
    byte[] path = new byte[1065];
    path[0] = 32;
    for (int i = 0; i < 32; i++) {
      path[1 + i * 33] = 32;
    }
    when(voucher.path().encode()).thenReturn(path);
    builder.addSpend(spendingKey(), spendNote(), new byte[32], new byte[32], voucher);
    try (MockedStatic<JLibrustzcash> nativeApi = mockStatic(JLibrustzcash.class);
         MockedStatic<TransactionCapsule> transactions = mockStatic(TransactionCapsule.class)) {
      allowSpendProof(nativeApi);
      transactions.when(() -> TransactionCapsule.getShieldTransactionHashIgnoreTypeException(any()))
          .thenReturn(new byte[32]);
      ZksnarkException error = assertThrows(ZksnarkException.class, builder::build);
      assertEquals("Spend signature generation failed", error.getMessage());
      assertTrue(builder.getContractBuilder().getSpendDescription(0)
          .getSpendAuthoritySignature().isEmpty());
      nativeApi.verify(() -> JLibrustzcash.librustzcashSaplingBindingSig(any()), never());
      nativeApi.verify(() -> JLibrustzcash.librustzcashSaplingProvingCtxFree(CONTEXT));
    }
  }

  @Test
  public void trc20BuildStopsOnBindingSignatureFailureAndFreesContext() throws Exception {
    ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder("transfer");
    builder.setShieldedTRC20Address(new byte[20]);
    try (MockedStatic<JLibrustzcash> nativeApi = mockStatic(JLibrustzcash.class)) {
      allowSpendProof(nativeApi);
      ZksnarkException error = assertThrows(ZksnarkException.class, () -> builder.build(false));
      assertTrue(error.getMessage().contains("Binding signature generation failed"));
      assertTrue(builder.getBuilder().getBindingSignature().isEmpty());
      nativeApi.verify(() -> JLibrustzcash.librustzcashSaplingBindingSig(any()));
      nativeApi.verify(() -> JLibrustzcash.librustzcashSaplingProvingCtxFree(CONTEXT));
    }
  }

  @Test
  public void zenBuildStopsOnBindingSignatureFailureAndFreesContext() throws Exception {
    ZenTransactionBuilder builder = new ZenTransactionBuilder(transactionWallet());
    try (MockedStatic<JLibrustzcash> nativeApi = mockStatic(JLibrustzcash.class);
         MockedStatic<TransactionCapsule> transactions = mockStatic(TransactionCapsule.class)) {
      allowSpendProof(nativeApi);
      transactions.when(() -> TransactionCapsule.getShieldTransactionHashIgnoreTypeException(any()))
          .thenReturn(new byte[32]);
      ZksnarkException error = assertThrows(ZksnarkException.class, builder::buildWithoutAsk);
      assertEquals("Binding signature generation failed", error.getMessage());
      assertTrue(builder.getContractBuilder().getBindingSignature().isEmpty());
      nativeApi.verify(() -> JLibrustzcash.librustzcashSaplingBindingSig(any()));
      nativeApi.verify(() -> JLibrustzcash.librustzcashSaplingProvingCtxFree(CONTEXT));
    }
  }
}
