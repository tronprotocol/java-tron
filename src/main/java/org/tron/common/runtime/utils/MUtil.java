package org.tron.common.runtime.utils;

import com.google.protobuf.ByteString;
import java.util.Arrays;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.Hash;
import org.tron.common.storage.Deposit;
import org.tron.core.Wallet;
import org.tron.core.actuator.Actuator;
import org.tron.core.actuator.ActuatorFactory;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

/**
 * @author Guo Yonggang
 * @since 02.05.2018
 */
public class MUtil {

  public static void transfer(Deposit deposit, byte[] fromAddress, byte[] toAddress, long amount)
      throws ContractValidateException {
    transferValidate(deposit, fromAddress, toAddress, amount);
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

  public static void transferValidate(Deposit deposit, byte[] fromAddress, byte[] toAddress,
      long amount)
      throws ContractValidateException {
    Contract.TransferContract.Builder builder = Contract.TransferContract.newBuilder();
    builder.setAmount(amount);
    builder.setOwnerAddress(ByteString.copyFrom(fromAddress));
    builder.setToAddress(ByteString.copyFrom(toAddress));
    TransferContract contract = builder.build();
    TransactionCapsule trx = new TransactionCapsule(contract,
        ContractType.TransferContract);
    Actuator actuator = ActuatorFactory.createActuator(trx, deposit.getDbManager()).get(0);
    actuator.validate();
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
