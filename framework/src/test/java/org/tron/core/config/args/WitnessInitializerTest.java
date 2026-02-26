package org.tron.core.config.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.bouncycastle.util.encoders.Hex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.tron.common.crypto.SignInterface;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.LocalWitnesses;
import org.tron.common.utils.PublicMethod;
import org.tron.common.utils.client.utils.Base58;
import org.tron.core.Constant;
import org.tron.core.exception.TronError;
import org.tron.core.exception.TronError.ErrCode;
import org.tron.keystore.Credentials;
import org.tron.keystore.WalletUtils;

public class WitnessInitializerTest {

  private Config config;
  private WitnessInitializer witnessInitializer;

  private static final String privateKey = PublicMethod.getRandomPrivateKey();
  private static final String address = Base58.encode58Check(
      ByteArray.fromHexString(PublicMethod.getHexAddressByPrivateKey(privateKey)));
  private static final String invalidAddress = "RJCzdnv88Hvqa2jB1C9dMmMYHr5DFdF2R3";

  @Before
  public void setUp() {
    config = ConfigFactory.empty();
    witnessInitializer = new WitnessInitializer(config);
  }

  @After
  public void clear() {
    Args.clearParam();
  }

  @Test
  public void testInitLocalWitnessesEmpty() {
    Args.PARAMETER.setWitness(false);

    LocalWitnesses result = witnessInitializer.initLocalWitnesses();
    assertNotNull(result);
    assertTrue(result.getPrivateKeys().isEmpty());

    Args.PARAMETER.setWitness(true);
    LocalWitnesses localWitnesses = witnessInitializer.initLocalWitnesses();
    assertTrue(localWitnesses.getPrivateKeys().isEmpty());

    String configString = "localwitness = [] \n localwitnesskeystore = []";
    config = ConfigFactory.parseString(configString);
    witnessInitializer = new WitnessInitializer(config);
    localWitnesses = witnessInitializer.initLocalWitnesses();
    assertTrue(localWitnesses.getPrivateKeys().isEmpty());
  }

  @Test
  public void testTryInitFromCommandLine()
      throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException,
      InvocationTargetException {
    Field privateKeyField = CommonParameter.class.getDeclaredField("privateKey");
    privateKeyField.setAccessible(true);
    privateKeyField.set(Args.getInstance(), "");

    witnessInitializer = new WitnessInitializer(config);
    Method method = WitnessInitializer.class.getDeclaredMethod(
        "tryInitFromCommandLine");
    method.setAccessible(true);
    boolean result = (boolean) method.invoke(witnessInitializer);
    assertFalse(result);

    privateKeyField.set(Args.getInstance(), privateKey);
    method.invoke(witnessInitializer);
    result = (boolean) method.invoke(witnessInitializer);
    assertTrue(result);

    Field witnessAddress = CommonParameter.class.getDeclaredField("witnessAddress");
    witnessAddress.setAccessible(true);
    witnessAddress.set(Args.getInstance(), address);
    result = (boolean) method.invoke(witnessInitializer);
    assertTrue(result);

    witnessAddress.set(Args.getInstance(), invalidAddress);
    InvocationTargetException thrown = assertThrows(InvocationTargetException.class,
        () -> method.invoke(witnessInitializer));
    TronError targetException = (TronError) thrown.getTargetException();
    assertEquals(ErrCode.WITNESS_INIT, targetException.getErrCode());
  }

  @Test
  public void testTryInitFromConfig()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    witnessInitializer = new WitnessInitializer(config);
    Method method = WitnessInitializer.class.getDeclaredMethod(
        "tryInitFromConfig");
    method.setAccessible(true);
    boolean result = (boolean) method.invoke(witnessInitializer);
    assertFalse(result);

    String configString = "localwitness = []";
    config = ConfigFactory.parseString(configString);
    witnessInitializer = new WitnessInitializer(config);
    result = (boolean) method.invoke(witnessInitializer);
    assertFalse(result);

    configString = "localwitness = [" + privateKey + "]";
    config = ConfigFactory.parseString(configString);
    witnessInitializer = new WitnessInitializer(config);
    result = (boolean) method.invoke(witnessInitializer);
    assertTrue(result);

    configString = "localWitnessAccountAddress = " + address + "\n"
        + "localwitness = [\n" + privateKey + "]";
    config = ConfigFactory.parseString(configString);
    witnessInitializer = new WitnessInitializer(config);
    result = (boolean) method.invoke(witnessInitializer);
    assertTrue(result);

    configString = "localwitness = [\n" + privateKey + "\n" + privateKey + "]";
    config = ConfigFactory.parseString(configString);
    witnessInitializer = new WitnessInitializer(config);
    result = (boolean) method.invoke(witnessInitializer);
    assertTrue(result);

    configString = "localWitnessAccountAddress = " + invalidAddress + "\n"
        + "localwitness = [\n" + privateKey + "]";
    config = ConfigFactory.parseString(configString);
    witnessInitializer = new WitnessInitializer(config);
    InvocationTargetException thrown = assertThrows(InvocationTargetException.class,
        () -> method.invoke(witnessInitializer));
    TronError targetException = (TronError) thrown.getTargetException();
    assertEquals(ErrCode.WITNESS_INIT, targetException.getErrCode());

    configString = "localWitnessAccountAddress = " + address + "\n"
        + "localwitness = [\n" + privateKey + "\n" + privateKey + "]";
    config = ConfigFactory.parseString(configString);
    witnessInitializer = new WitnessInitializer(config);
    thrown = assertThrows(InvocationTargetException.class,
        () -> method.invoke(witnessInitializer));
    targetException = (TronError) thrown.getTargetException();
    assertEquals(ErrCode.WITNESS_INIT, targetException.getErrCode());
  }

  @Test
  public void testTryInitFromKeystore()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException,
      NoSuchFieldException {
    witnessInitializer = new WitnessInitializer(config);
    Method method = WitnessInitializer.class.getDeclaredMethod(
        "tryInitFromKeystore");
    method.setAccessible(true);
    method.invoke(witnessInitializer);
    Field localWitnessField = WitnessInitializer.class.getDeclaredField("localWitnesses");
    localWitnessField.setAccessible(true);
    LocalWitnesses localWitnesses = (LocalWitnesses) localWitnessField.get(witnessInitializer);
    assertTrue(localWitnesses.getPrivateKeys().isEmpty());

    String configString = "localwitnesskeystore = []";
    Config emptyListConfig = ConfigFactory.parseString(configString);
    witnessInitializer = new WitnessInitializer(emptyListConfig);
    method.invoke(witnessInitializer);
    localWitnesses = (LocalWitnesses) localWitnessField.get(witnessInitializer);
    assertTrue(localWitnesses.getPrivateKeys().isEmpty());
  }

  @Test
  public void testTryInitFromKeyStore2()
      throws NoSuchFieldException, IllegalAccessException {
    Args.PARAMETER.setWitness(true);
    Config mockConfig = mock(Config.class);
    when(mockConfig.hasPath(Constant.LOCAL_WITNESS_KEYSTORE)).thenReturn(false);
    witnessInitializer = new WitnessInitializer(mockConfig);
    witnessInitializer.initLocalWitnesses();
    verify(mockConfig, never()).getStringList(anyString());

    when(mockConfig.hasPath(Constant.LOCAL_WITNESS_KEYSTORE)).thenReturn(true);
    when(mockConfig.getStringList(Constant.LOCAL_WITNESS_KEYSTORE)).thenReturn(new ArrayList<>());
    witnessInitializer = new WitnessInitializer(mockConfig);
    witnessInitializer.initLocalWitnesses();
    verify(mockConfig, times(1)).getStringList(Constant.LOCAL_WITNESS_KEYSTORE);

    List<String> keystores = new ArrayList<>();
    keystores.add("keystore1.json");
    keystores.add("keystore2.json");
    when(mockConfig.hasPath(Constant.LOCAL_WITNESS_KEYSTORE)).thenReturn(true);
    when(mockConfig.getStringList(Constant.LOCAL_WITNESS_KEYSTORE)).thenReturn(keystores);

    Field password = CommonParameter.class.getDeclaredField("password");
    password.setAccessible(true);
    password.set(Args.getInstance(), "password");

    try (MockedStatic<WalletUtils> mockedWalletUtils = mockStatic(WalletUtils.class);
        MockedStatic<ByteArray> mockedByteArray = mockStatic(ByteArray.class)) {
      // Mock WalletUtils.loadCredentials
      Credentials credentials = mock(Credentials.class);
      SignInterface signInterface = mock(SignInterface.class);
      when(credentials.getSignInterface()).thenReturn(signInterface);
      byte[] keyBytes = Hex.decode(privateKey);
      when(signInterface.getPrivateKey()).thenReturn(keyBytes);
      mockedWalletUtils.when(() -> WalletUtils.loadCredentials(anyString(), any(File.class)))
          .thenReturn(credentials);
      mockedByteArray.when(() -> ByteArray.toHexString(any())).thenReturn(privateKey);
      mockedByteArray.when(() -> ByteArray.fromHexString(anyString())).thenReturn(keyBytes);

      witnessInitializer = new WitnessInitializer(mockConfig);
      Field localWitnessField = WitnessInitializer.class.getDeclaredField("localWitnesses");
      localWitnessField.setAccessible(true);
      localWitnessField.set(witnessInitializer, new LocalWitnesses(privateKey));
      LocalWitnesses localWitnesses = witnessInitializer.initLocalWitnesses();
      assertFalse(localWitnesses.getPrivateKeys().isEmpty());
    }
  }

  @Test
  public void testGetWitnessAddress()
      throws InvocationTargetException, IllegalAccessException, NoSuchMethodException,
      NoSuchFieldException {
    witnessInitializer = new WitnessInitializer(config);
    Method method = WitnessInitializer.class.getDeclaredMethod(
        "getWitnessAddress");
    method.setAccessible(true);
    byte[] result = (byte[]) method.invoke(witnessInitializer);
    assertNull(result);

    String configString = "localWitnessAccountAddress = " + address;
    config = ConfigFactory.parseString(configString);
    witnessInitializer = new WitnessInitializer(config);
    Field localWitnessField = WitnessInitializer.class.getDeclaredField("localWitnesses");
    localWitnessField.setAccessible(true);
    localWitnessField.set(witnessInitializer, new LocalWitnesses(privateKey));
    result = (byte[]) method.invoke(witnessInitializer);
    assertNotNull(result);

    configString = "localWitnessAccountAddress = " + invalidAddress;
    config = ConfigFactory.parseString(configString);
    witnessInitializer = new WitnessInitializer(config);
    InvocationTargetException thrown = assertThrows(InvocationTargetException.class,
        () -> method.invoke(witnessInitializer));
    TronError targetException = (TronError) thrown.getTargetException();
    assertEquals(ErrCode.WITNESS_INIT, targetException.getErrCode());
  }
}
