package org.tron.common.crypto.jce;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.tron.common.crypto.Hash;
import org.tron.common.crypto.cryptohash.Keccak512;

public class TronCastleProviderTest {

  @Test
  public void testExistingProvider() throws Exception {
    assertProviderIsolation(new Provider("BC", 1.0, "Unrelated test provider") { });
  }

  @Test
  public void testNoRegisteredProvider() throws Exception {
    assertProviderIsolation(null);
  }

  private void assertProviderIsolation(Provider existing) throws Exception {
    Provider original = Security.getProvider("BC");
    int position = Arrays.asList(Security.getProviders()).indexOf(original) + 1;
    Security.removeProvider("BC");
    try (URLClassLoader loader = newCryptoClassLoader()) {
      if (existing != null) {
        Security.addProvider(existing);
      }
      Class<?> providerClass = loader.loadClass(TronCastleProvider.class.getName());
      Provider provider = (Provider) providerClass.getMethod("getInstance").invoke(null);
      assertEquals(BouncyCastleProvider.class, provider.getClass());
      assertNotSame(existing, provider);
      assertSame(provider, providerClass.getMethod("getInstance").invoke(null));
      assertSame(existing, Security.getProvider("BC"));

      byte[] empty = new byte[0];
      byte[] expected = Hex.decode(
          "c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470");
      assertArrayEquals(expected,
          MessageDigest.getInstance("TRON-KECCAK-256", provider).digest(empty));
      assertArrayEquals(new Keccak512().digest(empty),
          MessageDigest.getInstance("TRON-KECCAK-512", provider).digest(empty));
      assertNotNull(ECKeyPairGenerator.getInstance(provider, new SecureRandom()).generateKeyPair());

      Class<?> hashClass = loader.loadClass(Hash.class.getName());
      assertArrayEquals(expected,
          (byte[]) hashClass.getMethod("sha3", byte[].class).invoke(null, empty));
      if (existing != null) {
        assertSame(existing, Security.getProvider("BC"));
        assertNull(existing.get("MessageDigest.TRON-KECCAK-256"));
        assertNull(existing.get("MessageDigest.TRON-KECCAK-512"));
      }
    } finally {
      Security.removeProvider("BC");
      if (original != null) {
        Security.insertProviderAt(original, position);
      }
    }
  }

  private URLClassLoader newCryptoClassLoader() {
    URL location = TronCastleProvider.class.getProtectionDomain().getCodeSource().getLocation();
    // Reload static initializers regardless of which crypto tests ran earlier in this JVM.
    return new URLClassLoader(new URL[]{location}, getClass().getClassLoader()) {
      @Override
      protected synchronized Class<?> loadClass(String name, boolean resolve)
          throws ClassNotFoundException {
        if (name.equals(Hash.class.getName())
            || name.equals(TronCastleProvider.class.getName())
            || name.startsWith(TronCastleProvider.class.getName() + "$")) {
          Class<?> loaded = findLoadedClass(name);
          if (loaded == null) {
            loaded = findClass(name);
          }
          if (resolve) {
            resolveClass(loaded);
          }
          return loaded;
        }
        return super.loadClass(name, resolve);
      }
    };
  }
}
