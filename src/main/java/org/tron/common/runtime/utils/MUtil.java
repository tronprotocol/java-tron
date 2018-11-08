package org.tron.common.runtime.utils;

import org.tron.common.storage.Deposit;
import org.tron.core.Wallet;
import org.tron.core.actuator.TransferActuator;
import org.tron.core.exception.ContractValidateException;

public class MUtil {
  private MUtil() {}

  public static void transfer(Deposit deposit, byte[] fromAddress, byte[] toAddress, long amount)
      throws ContractValidateException {
    if (0 == amount) {
      return;
    }
    TransferActuator.validateForSmartContract(deposit, fromAddress, toAddress, amount);
    deposit.addBalance(toAddress, amount);
    deposit.addBalance(fromAddress, -amount);
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
}
