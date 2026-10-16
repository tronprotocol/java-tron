package org.tron.core;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import java.lang.reflect.Field;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.common.utils.Base58;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.store.AccountStore;

public class WalletContractAddressLoggingTest {

  @Test
  public void oversizedMissingAddressIsLoggedWithoutBase58() throws Exception {
    Wallet wallet = new Wallet();
    ChainBaseManager chainBaseManager = mock(ChainBaseManager.class);
    AccountStore accountStore = mock(AccountStore.class);
    when(accountStore.get(any(byte[].class))).thenReturn((AccountCapsule) null);
    when(chainBaseManager.getAccountStore()).thenReturn(accountStore);
    Field field = Wallet.class.getDeclaredField("chainBaseManager");
    field.setAccessible(true);
    field.set(wallet, chainBaseManager);

    for (int size : new int[] {22, 32 * 1024}) {
      BytesMessage message = BytesMessage.newBuilder()
          .setValue(ByteString.copyFrom(new byte[size])).build();
      try (MockedStatic<Base58> base58 = mockStatic(Base58.class)) {
        assertNull(wallet.getContract(message));
        assertNull(wallet.getContractInfo(message));
        base58.verifyNoInteractions();
      }
    }
  }
}
