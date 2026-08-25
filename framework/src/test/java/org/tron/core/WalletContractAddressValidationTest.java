package org.tron.core;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.common.utils.Base58;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.store.AbiStore;
import org.tron.core.store.AccountStore;
import org.tron.core.store.ContractStore;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;

public class WalletContractAddressValidationTest {

  // the stores are re-mocked per case, but the wallet itself carries no state these tests read;
  // constructing one generates a keypair, so it is built once rather than once per address
  private static Wallet wallet;
  private static Field chainBaseManagerField;

  @BeforeClass
  public static void setUpClass() throws Exception {
    wallet = new Wallet();
    chainBaseManagerField = Wallet.class.getDeclaredField("chainBaseManager");
    chainBaseManagerField.setAccessible(true);
  }

  /**
   * Installs a chain base manager whose account store finds nothing, and hands back that store so
   * the caller can assert whether it was ever consulted.
   */
  private static AccountStore installMissingAccount() throws Exception {
    ChainBaseManager chainBaseManager = mock(ChainBaseManager.class);
    AccountStore accountStore = mock(AccountStore.class);
    when(accountStore.get(any(byte[].class))).thenReturn((AccountCapsule) null);
    when(chainBaseManager.getAccountStore()).thenReturn(accountStore);
    chainBaseManagerField.set(wallet, chainBaseManager);
    return accountStore;
  }

  private static BytesMessage bytes(byte[] raw) {
    return BytesMessage.newBuilder().setValue(ByteString.copyFrom(raw)).build();
  }

  private static byte[] canonicalAddress() {
    byte[] address = new byte[21];
    Arrays.fill(address, (byte) 1);
    address[0] = Wallet.getAddressPreFixByte();
    return address;
  }

  @Test
  public void invalidAddressesAreRejectedBeforeStorageOrBase58() throws Exception {
    byte[][] invalidAddresses = {
        new byte[0],
        new byte[20],
        new byte[22],
        new byte[32 * 1024],
        canonicalAddress()
    };
    invalidAddresses[4][0] = (byte) (Wallet.getAddressPreFixByte() + 1);

    for (byte[] invalidAddress : invalidAddresses) {
      AccountStore accountStore = installMissingAccount();
      try (MockedStatic<Base58> base58 = mockStatic(Base58.class)) {
        assertNull(wallet.getContract(bytes(invalidAddress)));
        assertNull(wallet.getContractInfo(bytes(invalidAddress)));
        verifyNoInteractions(accountStore);
        base58.verifyNoInteractions();
      }
    }
  }

  @Test
  public void canonicalMissingAddressRetainsNoResultBehavior() throws Exception {
    AccountStore accountStore = installMissingAccount();
    byte[] address = canonicalAddress();

    assertNull(wallet.getContract(bytes(address)));
    assertNull(wallet.getContractInfo(bytes(address)));

    verify(accountStore, times(2)).get(aryEq(address));
  }

  @Test
  public void canonicalExistingAddressRetainsContractResult() throws Exception {
    ChainBaseManager chainBaseManager = mock(ChainBaseManager.class);
    AccountStore accountStore = mock(AccountStore.class);
    ContractStore contractStore = mock(ContractStore.class);
    AbiStore abiStore = mock(AbiStore.class);
    ContractCapsule contractCapsule = mock(ContractCapsule.class);
    SmartContract contract = SmartContract.newBuilder().setName("existing").build();
    byte[] address = canonicalAddress();

    when(chainBaseManager.getAccountStore()).thenReturn(accountStore);
    when(chainBaseManager.getContractStore()).thenReturn(contractStore);
    when(chainBaseManager.getAbiStore()).thenReturn(abiStore);
    when(accountStore.get(any(byte[].class))).thenReturn(mock(AccountCapsule.class));
    when(contractStore.get(any(byte[].class))).thenReturn(contractCapsule);
    when(contractCapsule.getInstance()).thenReturn(contract);
    when(abiStore.get(any(byte[].class))).thenReturn(null);
    chainBaseManagerField.set(wallet, chainBaseManager);

    assertSame(contract, wallet.getContract(bytes(address)));
  }
}
