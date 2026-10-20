package org.tron.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import org.tron.api.GrpcAPI.TransactionApprovedList;
import org.tron.api.GrpcAPI.TransactionSignWeight;
import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.SignUtils;
import org.tron.common.utils.ForkController;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.services.http.servlets.Util;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Key;
import org.tron.protos.Protocol.Permission;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.BalanceContract.TransferContract;

@RunWith(Parameterized.class)
public class SignatureQueryTest {

  @Parameterized.Parameters(name = "strict={0}")
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][] {
        {false}, {true}
    });
  }

  @Parameterized.Parameter
  public boolean strict;

  private Wallet wallet;
  private AccountStore accountStore;
  private Transaction unsigned;
  private Transaction signed;
  private Permission permission;

  @Before
  public void setUp() {
    DynamicPropertiesStore properties = mock(DynamicPropertiesStore.class);
    when(properties.getTotalSignNum()).thenReturn(5);
    when(properties.allowStrictEcdsaValidation()).thenReturn(strict);
    when(properties.getAllowStrictEcdsaValidation()).thenReturn(strict ? 1L : 0L);
    accountStore = mock(AccountStore.class);
    ChainBaseManager manager = mock(ChainBaseManager.class);
    when(manager.getDynamicPropertiesStore()).thenReturn(properties);
    when(manager.getAccountStore()).thenReturn(accountStore);
    wallet = new Wallet();
    ReflectionTestUtils.setField(wallet, "chainBaseManager", manager);

    byte[] privateKey = new byte[32];
    privateKey[31] = 10;
    SignInterface signer = SignUtils.fromPrivate(privateKey);
    ByteString address = ByteString.copyFrom(signer.getAddress());
    permission = Permission.newBuilder().setThreshold(1)
        .addKeys(Key.newBuilder().setAddress(address).setWeight(1)).build();
    setPermission(permission);
    unsigned = Transaction.newBuilder().setRawData(Transaction.raw.newBuilder()
        .addContract(Contract.newBuilder().setType(ContractType.TransferContract)
            .setParameter(Any.pack(TransferContract.newBuilder()
                .setOwnerAddress(address).setToAddress(address).setAmount(1).build())))).build();
    TransactionCapsule capsule = new TransactionCapsule(unsigned);
    capsule.sign(privateKey);
    signed = capsule.getInstance();
    assertEquals(65, signed.getSignature(0).size());
  }

  @Test
  public void rejectsEveryInvalidLengthBeforeConversionOrRecovery() {
    try (MockedStatic<TransactionCapsule> capsules =
        mockStatic(TransactionCapsule.class, CALLS_REAL_METHODS)) {
      for (int length : new int[] {0, 64, 69, 96}) {
        ByteString invalid = ByteString.copyFrom(new byte[length]);
        assertFormatError(unsigned.toBuilder().addSignature(invalid).build());
        assertFormatError(signed.toBuilder().addSignature(invalid).build());
        assertFormatError(unsigned.toBuilder().addSignature(invalid)
            .addSignature(signed.getSignature(0)).build());
      }
      capsules.verify(() -> TransactionCapsule.getBase64FromByteString(any()), never());
    }
    verifyNoInteractions(accountStore);
  }

  @Test
  public void acceptsSupportedPaddingWithoutRewritingTransactionOrChangingId() {
    try (MockedStatic<ForkController> forks = mockStatic(ForkController.class)) {
      forks.when(ForkController::instance).thenReturn(mock(ForkController.class));
      assertQueryResults(signed, TransactionSignWeight.Result.response_code.ENOUGH_PERMISSION,
          TransactionApprovedList.Result.response_code.SUCCESS, 1);
      for (int length = 66; length <= 68; length++) {
        byte[] padding = new byte[length - 65];
        Arrays.fill(padding, (byte) 0x7f);
        Transaction padded = signed.toBuilder().setSignature(0,
            signed.getSignature(0).concat(ByteString.copyFrom(padding))).build();
        assertQueryResults(padded, TransactionSignWeight.Result.response_code.ENOUGH_PERMISSION,
            TransactionApprovedList.Result.response_code.SUCCESS, 1);
        assertEquals(new TransactionCapsule(signed).getTransactionId(),
            new TransactionCapsule(padded).getTransactionId());
      }
    }
  }

  @Test
  public void preservesUnsignedQueryBehavior() {
    assertQueryResults(unsigned, TransactionSignWeight.Result.response_code.NOT_ENOUGH_PERMISSION,
        TransactionApprovedList.Result.response_code.SUCCESS, 0);
    assertFormatError(unsigned.toBuilder().addSignature(ByteString.EMPTY).build());
  }

  @Test
  public void rejectsMalformedComponentsRegardlessOfProposalActivation() {
    Transaction malformed = unsigned.toBuilder()
        .addSignature(ByteString.copyFrom(new byte[65])).build();
    assertQueryResults(malformed, TransactionSignWeight.Result.response_code.COMPUTE_ADDRESS_ERROR,
        TransactionApprovedList.Result.response_code.COMPUTE_ADDRESS_ERROR, 0);
  }

  @Test
  public void validLengthStillRequiresPermission() {
    Permission differentSigner = permission.toBuilder().setKeys(0,
        permission.getKeys(0).toBuilder().setAddress(ByteString.copyFrom(new byte[21]))).build();
    setPermission(differentSigner);
    assertQueryResults(signed, TransactionSignWeight.Result.response_code.PERMISSION_ERROR,
        TransactionApprovedList.Result.response_code.OTHER_ERROR, 0);
  }

  @Test
  public void validLengthDoesNotGuaranteeEnoughWeight() {
    setPermission(permission.toBuilder().setThreshold(2).build());
    try (MockedStatic<ForkController> forks = mockStatic(ForkController.class)) {
      forks.when(ForkController::instance).thenReturn(mock(ForkController.class));
      assertQueryResults(signed, TransactionSignWeight.Result.response_code.NOT_ENOUGH_PERMISSION,
          TransactionApprovedList.Result.response_code.SUCCESS, 1);
    }
  }

  @Test
  public void checksSignatureCountBeforeLengthsOrHashing() {
    Transaction.Builder tooMany = unsigned.toBuilder();
    for (int i = 0; i < 6; i++) {
      tooMany.addSignature(signed.getSignature(0));
    }
    for (Transaction input : Arrays.asList(tooMany.build(),
        tooMany.addSignature(ByteString.EMPTY).build())) {
      Transaction transaction = spy(input);
      Transaction.raw rawData = spy(input.getRawData());
      when(transaction.getRawData()).thenReturn(rawData);
      TransactionSignWeight weight = wallet.getTransactionSignWeight(transaction);
      TransactionApprovedList approved = wallet.getTransactionApprovedList(transaction);
      assertEquals(TransactionSignWeight.Result.response_code.OTHER_ERROR,
          weight.getResult().getCode());
      assertEquals(TransactionApprovedList.Result.response_code.OTHER_ERROR,
          approved.getResult().getCode());
      assertEquals("too many signatures", weight.getResult().getMessage());
      assertEquals("too many signatures", approved.getResult().getMessage());
      assertFalse(weight.hasTransaction());
      assertFalse(approved.hasTransaction());
      verify(transaction, never()).getSignatureList();
      verify(rawData, never()).toByteArray();
    }
    verifyNoInteractions(accountStore);
  }

  @Test
  public void oversizedSignatureProducesSmallErrorResponse() {
    Transaction input = unsigned.toBuilder()
        .addSignature(ByteString.copyFrom(new byte[1024 * 1024])).build();
    assertFormatError(input);
    TransactionSignWeight weight = wallet.getTransactionSignWeight(input);
    TransactionApprovedList approved = wallet.getTransactionApprovedList(input);
    assertTrue(Util.printTransactionSignWeight(weight, false).length() < 256);
    assertTrue(Util.printTransactionApprovedList(approved, false).length() < 256);
    verifyNoInteractions(accountStore);
  }

  private void setPermission(Permission value) {
    when(accountStore.get(any(byte[].class))).thenReturn(new AccountCapsule(Account.newBuilder()
        .setOwnerPermission(value).build()));
  }

  private void assertFormatError(Transaction input) {
    Transaction transaction = spy(input);
    Transaction.raw rawData = spy(input.getRawData());
    when(transaction.getRawData()).thenReturn(rawData);
    TransactionSignWeight weight = wallet.getTransactionSignWeight(transaction);
    TransactionApprovedList approved = wallet.getTransactionApprovedList(transaction);
    assertEquals(TransactionSignWeight.Result.response_code.SIGNATURE_FORMAT_ERROR,
        weight.getResult().getCode());
    assertEquals(TransactionApprovedList.Result.response_code.SIGNATURE_FORMAT_ERROR,
        approved.getResult().getCode());
    assertFalse(weight.getResult().getMessage().isEmpty());
    assertFalse(approved.getResult().getMessage().isEmpty());
    assertEquals(0, weight.getApprovedListCount());
    assertEquals(0, approved.getApprovedListCount());
    assertFalse(weight.hasTransaction());
    assertFalse(approved.hasTransaction());
    verify(rawData, never()).toByteArray();
  }

  private void assertQueryResults(Transaction input,
      TransactionSignWeight.Result.response_code weightCode,
      TransactionApprovedList.Result.response_code approvedCode, int approvedCount) {
    TransactionSignWeight weight = wallet.getTransactionSignWeight(input);
    TransactionApprovedList approved = wallet.getTransactionApprovedList(input);
    assertEquals(weightCode, weight.getResult().getCode());
    assertEquals(approvedCode, approved.getResult().getCode());
    assertEquals(approvedCount, weight.getApprovedListCount());
    assertEquals(approvedCount, approved.getApprovedListCount());
    assertEquals(input, weight.getTransaction().getTransaction());
    assertEquals(input, approved.getTransaction().getTransaction());
    ByteString txId = ByteString.copyFrom(Sha256Hash.hash(input.getRawData().toByteArray()));
    assertEquals(txId, weight.getTransaction().getTxid());
    assertEquals(txId, approved.getTransaction().getTxid());
  }
}
