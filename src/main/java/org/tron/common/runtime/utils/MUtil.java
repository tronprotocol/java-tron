package org.tron.common.runtime.utils;

import java.util.Arrays;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.Hash;
import org.tron.common.storage.Deposit;
import org.tron.core.Wallet;
import org.tron.core.actuator.TransferActuator;
import org.tron.core.exception.ContractValidateException;


/**
 * @author Guo Yonggang
 * @since 02.05.2018
 */
public class MUtil {

  public static void transfer(Deposit deposit, byte[] fromAddress, byte[] toAddress, long amount)
      throws ContractValidateException {
    if (0 == amount) {
      return;
    }
    TransferActuator.validateForSmartContract(deposit, fromAddress, toAddress, amount);
    if (deposit.getBalance(fromAddress) < amount) {
      throw new RuntimeException(
          Hex.toHexString(fromAddress).toUpperCase() + " not enough balance!");
    }
    if (deposit.getBalance(toAddress) + amount < amount) {
      throw new RuntimeException("Long integer overflow!");
    }
    deposit.addBalance(toAddress, amount);
    deposit.addBalance(fromAddress, -amount);
  }


  public static void burn(Deposit deposit, byte[] address, long amount) {
    if (deposit.getBalance(address) < amount) {
      throw new RuntimeException("Not enough balance!");
    }
    deposit.addBalance(address, -amount);
  }

  public static byte[] convertToTronAddress(byte[] address) {
    if (address.length == 20) {
      byte[] newAddress = new byte[21];
      byte[] temp = new byte[]{Wallet.getAddressPreFixByte()};
      System.arraycopy(temp, 0, newAddress, 0, temp.length);
      System.arraycopy(address, 0, newAddress, temp.length, address.length);
      address = newAddress;
    }
    return address;
  }

  public static String get4BytesSha3HexString(String data) {
    return Hex.toHexString(Arrays.copyOf(Hash.sha3(data.getBytes()), 4));
  }
}
