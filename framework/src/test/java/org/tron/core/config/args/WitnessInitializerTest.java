package org.tron.core.config.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bouncycastle.util.encoders.Hex;
import org.junit.After;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.tron.common.crypto.SignInterface;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.LocalWitnesses;
import org.tron.common.utils.PublicMethod;
import org.tron.common.utils.client.utils.Base58;
import org.tron.core.exception.TronError;
import org.tron.core.exception.TronError.ErrCode;
import org.tron.keystore.Credentials;
import org.tron.keystore.WalletUtils;

public class WitnessInitializerTest {

  private static final String privateKey = PublicMethod.getRandomPrivateKey();
  private static final String address = Base58.encode58Check(
      ByteArray.fromHexString(PublicMethod.getHexAddressByPrivateKey(privateKey)));
  private static final String invalidAddress = "RJCzdnv88Hvqa2jB1C9dMmMYHr5DFdF2R3";

  @After
  public void clear() {
    Args.clearParam();
  }

  @Test
  public void testInitFromCLI() {
    // privateKey only
    LocalWitnesses result =
        WitnessInitializer.initFromCLIPrivateKey(privateKey, null);
    assertNotNull(result);
    assertFalse(result.getPrivateKeys().isEmpty());
    assertEquals(privateKey, result.getPrivateKeys().get(0));

    // with valid witnessAddress
    result = WitnessInitializer.initFromCLIPrivateKey(privateKey, address);
    assertNotNull(result);
    assertFalse(result.getPrivateKeys().isEmpty());

    // with invalid witnessAddress
    TronError err = assertThrows(TronError.class,
        () -> WitnessInitializer.initFromCLIPrivateKey(
            privateKey, invalidAddress));
    assertEquals(ErrCode.WITNESS_INIT, err.getErrCode());
  }

  @Test
  public void testInitFromConfig() {
    // single private key, no address
    LocalWitnesses result = WitnessInitializer.initFromCFGPrivateKey(
        Collections.singletonList(privateKey), null);
    assertFalse(result.getPrivateKeys().isEmpty());

    // single key + valid address
    result = WitnessInitializer.initFromCFGPrivateKey(
        Collections.singletonList(privateKey), address);
    assertFalse(result.getPrivateKeys().isEmpty());

    // multiple keys, no address
    result = WitnessInitializer.initFromCFGPrivateKey(
        Arrays.asList(privateKey, privateKey), null);
    assertFalse(result.getPrivateKeys().isEmpty());

    // single key + invalid address
    TronError err = assertThrows(TronError.class,
        () -> WitnessInitializer.initFromCFGPrivateKey(
            Collections.singletonList(privateKey), invalidAddress));
    assertEquals(ErrCode.WITNESS_INIT, err.getErrCode());

    // multiple keys + address = error
    err = assertThrows(TronError.class,
        () -> WitnessInitializer.initFromCFGPrivateKey(
            Arrays.asList(privateKey, privateKey), address));
    assertEquals(ErrCode.WITNESS_INIT, err.getErrCode());
  }

  @Test
  public void testInitFromKeystore() {
    List<String> keystores = Arrays.asList("keystore1.json", "keystore2.json");

    try (MockedStatic<WalletUtils> mockedWallet = mockStatic(WalletUtils.class);
         MockedStatic<ByteArray> mockedByteArray = mockStatic(ByteArray.class)) {

      Credentials credentials = mock(Credentials.class);
      SignInterface signInterface = mock(SignInterface.class);
      when(credentials.getSignInterface()).thenReturn(signInterface);
      byte[] keyBytes = Hex.decode(privateKey);
      when(signInterface.getPrivateKey()).thenReturn(keyBytes);
      mockedWallet.when(() -> WalletUtils.loadCredentials(
          anyString(), any(File.class))).thenReturn(credentials);
      mockedByteArray.when(() -> ByteArray.toHexString(any()))
          .thenReturn(privateKey);
      mockedByteArray.when(() -> ByteArray.fromHexString(anyString()))
          .thenReturn(keyBytes);

      LocalWitnesses result = WitnessInitializer.initFromKeystore(
          keystores, "password", null);
      assertFalse(result.getPrivateKeys().isEmpty());
    }
  }

  @Test
  public void testResolveWitnessAddress() {
    // null address -> null
    LocalWitnesses witnesses = new LocalWitnesses(privateKey);
    byte[] result = WitnessInitializer.resolveWitnessAddress(witnesses, null);
    assertNull(result);

    // empty address -> null
    result = WitnessInitializer.resolveWitnessAddress(witnesses, "");
    assertNull(result);

    // valid address with single key
    result = WitnessInitializer.resolveWitnessAddress(witnesses, address);
    assertNotNull(result);

    // invalid address
    TronError err = assertThrows(TronError.class,
        () -> WitnessInitializer.resolveWitnessAddress(
            new LocalWitnesses(privateKey), invalidAddress));
    assertEquals(ErrCode.WITNESS_INIT, err.getErrCode());

    // multiple keys + address = error
    LocalWitnesses multiKey = new LocalWitnesses();
    List<String> keys = new ArrayList<>();
    keys.add(privateKey);
    keys.add(privateKey);
    multiKey.setPrivateKeys(keys);
    err = assertThrows(TronError.class,
        () -> WitnessInitializer.resolveWitnessAddress(multiKey, address));
    assertEquals(ErrCode.WITNESS_INIT, err.getErrCode());
  }
}
