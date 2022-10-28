/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.utils;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.SignUtils;
import org.tron.core.config.Parameter.ChainConstant;

@Slf4j(topic = "app")
public class LocalWitnesses {

  private List<String> privateKeys = Lists.newArrayList();

  private byte[] witnessAccountAddress;

  private static String AESKey = "abcdefg111111111";

  public LocalWitnesses() {
  }

  public LocalWitnesses(String privateKey) {
    addPrivateKeys(privateKey);
  }

  public LocalWitnesses(List<String> privateKeys) {
    setPrivateKeys(privateKeys);
  }

  public byte[] getWitnessAccountAddress(boolean isECKeyCryptoEngine) {
    if (witnessAccountAddress == null) {
      byte[] privateKey = ByteArray.fromHexString(getPrivateKey());
      final SignInterface cryptoEngine = SignUtils.fromPrivate(privateKey, isECKeyCryptoEngine);
      this.witnessAccountAddress = cryptoEngine.getAddress();
    }
    return witnessAccountAddress;
  }

  public void setWitnessAccountAddress(final byte[] localWitnessAccountAddress) {
    this.witnessAccountAddress = localWitnessAccountAddress;
  }

  public void initWitnessAccountAddress(boolean isECKeyCryptoEngine) {
    if (witnessAccountAddress == null) {
      byte[] privateKey = ByteArray.fromHexString(getPrivateKey());
      final SignInterface ecKey = SignUtils.fromPrivate(privateKey,
              isECKeyCryptoEngine);
      this.witnessAccountAddress = ecKey.getAddress();
    }
  }

  /**
   * Private key of ECKey.
   */
  public void setPrivateKeys2(final List<String> privateKeys) {
    if (CollectionUtils.isEmpty(privateKeys)) {
      return;
    }
    for (String privateKey : privateKeys) {
      validate(privateKey);
    }

    this.privateKeys = privateKeys;
  }

  public void setPrivateKeys(final List<String> privateKeys) {
    List<String> privateKeysByAes = new ArrayList<>();
    if (CollectionUtils.isEmpty(privateKeys)) {
      return;
    }
    for (String privateKey : privateKeys) {
      validate(privateKey);
      try {
        String encrypt = MyAESUtil.encrypt(privateKey, AESKey);
        privateKeysByAes.add(encrypt);
        String decrypt = MyAESUtil.decrypt(encrypt, AESKey);

        if (privateKey.equals(decrypt)) {
          System.out.println("11");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    this.privateKeys = privateKeysByAes;
  }


  private void validate(String privateKey) {
    if (StringUtils.startsWithIgnoreCase(privateKey, "0X")) {
      privateKey = privateKey.substring(2);
    }

    if (StringUtils.isNotBlank(privateKey)
            && privateKey.length() != ChainConstant.PRIVATE_KEY_LENGTH) {
      throw new IllegalArgumentException(
              String.format("private key must be %d-bits hex string, actual: %d",
                      ChainConstant.PRIVATE_KEY_LENGTH, privateKey.length()));
    }
  }

  public void addPrivateKeys1(String privateKey) {
    validate(privateKey);
    this.privateKeys.add(privateKey);
  }

  public void addPrivateKeys(String privateKey) {
    validate(privateKey);
    try {
      this.privateKeys.add(MyAESUtil.encrypt(privateKey, AESKey));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  //get the first one recently
  public String getPrivateKey() {
    if (CollectionUtils.isEmpty(privateKeys)) {
      logger.warn("PrivateKey is null.");
      return null;
    }
    try {
      return MyAESUtil.decrypt(privateKeys.get(0), AESKey);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public byte[] getPublicKey() {
    if (CollectionUtils.isEmpty(privateKeys)) {
      logger.warn("PrivateKey is null.");
      return null;
    }
    byte[] privateKey = ByteArray.fromHexString(getPrivateKey());
    final ECKey ecKey = ECKey.fromPrivate(privateKey);
    return ecKey.getAddress();
  }

  public List<String> getPrivateKeys() {
    return privateKeys.stream().map(key -> {
      String decrypt = null;
      try {
        decrypt = MyAESUtil.decrypt(key, AESKey);
      } catch (Exception e) {
        e.printStackTrace();
      }
      return decrypt;
    }).collect(Collectors.toList());
  }
}
