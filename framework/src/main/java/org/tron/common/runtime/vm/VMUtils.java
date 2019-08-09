/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tron.common.runtime.vm;

import static java.lang.String.format;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.runtime.config.VMConfig;
import org.tron.common.storage.Deposit;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.exception.ContractValidateException;

@Slf4j(topic = "VM")
public final class VMUtils {

  private VMUtils() {
  }

  public static void closeQuietly(Closeable closeable) {
    try {
      if (closeable != null) {
        closeable.close();
      }
    } catch (IOException ioe) {
// ignore
    }
  }

  private static File createProgramTraceFile(VMConfig config, String txHash) {
    File result = null;

    if (config.vmTrace()) {

      File file = new File(new File("./", "vm_trace"), txHash + ".json");

      if (file.exists()) {
        if (file.isFile() && file.canWrite()) {
          result = file;
        }
      } else {
        try {
          file.getParentFile().mkdirs();
          file.createNewFile();
          result = file;
        } catch (IOException e) {
          // ignored
        }
      }
    }

    return result;
  }

  private static void writeStringToFile(File file, String data) {
    OutputStream out = null;
    try {
      out = new FileOutputStream(file);
      if (data != null) {
        out.write(data.getBytes("UTF-8"));
      }
    } catch (Exception e) {
      logger.error(format("Cannot write to file '%s': ", file.getAbsolutePath()), e);
    } finally {
      closeQuietly(out);
    }
  }

  public static void saveProgramTraceFile(VMConfig config, String txHash, String content) {
    File file = createProgramTraceFile(config, txHash);
    if (file != null) {
      writeStringToFile(file, content);
    }
  }

  private static final int BUF_SIZE = 4096;

  private static void write(InputStream in, OutputStream out, int bufSize) throws IOException {
    try {
      byte[] buf = new byte[bufSize];
      for (int count = in.read(buf); count != -1; count = in.read(buf)) {
        out.write(buf, 0, count);
      }
    } finally {
      closeQuietly(in);
      closeQuietly(out);
    }
  }

  public static byte[] compress(byte[] bytes) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    ByteArrayInputStream in = new ByteArrayInputStream(bytes);
    DeflaterOutputStream out = new DeflaterOutputStream(baos, new Deflater(), BUF_SIZE);

    write(in, out, BUF_SIZE);

    return baos.toByteArray();
  }

  public static byte[] compress(String content) throws IOException {
    return compress(content.getBytes("UTF-8"));
  }

  public static byte[] decompress(byte[] data) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length);

    ByteArrayInputStream in = new ByteArrayInputStream(data);
    InflaterOutputStream out = new InflaterOutputStream(baos, new Inflater(), BUF_SIZE);

    write(in, out, BUF_SIZE);

    return baos.toByteArray();
  }

  public static String zipAndEncode(String content) {
    try {
      return encodeBase64String(compress(content));
    } catch (Exception e) {
      logger.error("Cannot zip or encode: ", e);
      return content;
    }
  }

  public static String unzipAndDecode(String content) {
    try {
      byte[] decoded = decodeBase64(content);
      return new String(decompress(decoded), "UTF-8");
    } catch (Exception e) {
      logger.error("Cannot unzip or decode: ", e);
      return content;
    }
  }

  public static boolean validateForSmartContract(Deposit deposit, byte[] ownerAddress,
      byte[] toAddress, long amount) throws ContractValidateException {
    if (!Wallet.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid ownerAddress");
    }
    if (!Wallet.addressValid(toAddress)) {
      throw new ContractValidateException("Invalid toAddress");
    }

    if (Arrays.equals(toAddress, ownerAddress)) {
      throw new ContractValidateException("Cannot transfer trx to yourself.");
    }

    AccountCapsule ownerAccount = deposit.getAccount(ownerAddress);
    if (ownerAccount == null) {
      throw new ContractValidateException("Validate InternalTransfer error, no OwnerAccount.");
    }

    AccountCapsule toAccount = deposit.getAccount(toAddress);
    if (toAccount == null) {
      throw new ContractValidateException(
          "Validate InternalTransfer error, no ToAccount. And not allowed to create account in smart contract.");
    }

    long balance = ownerAccount.getBalance();

    if (amount < 0) {
      throw new ContractValidateException("Amount must greater than or equals 0.");
    }

    try {
      if (balance < amount) {
        throw new ContractValidateException(
            "Validate InternalTransfer error, balance is not sufficient.");
      }

      if (toAccount != null) {
        long toAddressBalance = Math.addExact(toAccount.getBalance(), amount);
      }
    } catch (ArithmeticException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    return true;
  }

  public static boolean validateForSmartContract(Deposit deposit, byte[] ownerAddress,
      byte[] toAddress, byte[] tokenId, long amount) throws ContractValidateException {
    if (deposit == null) {
      throw new ContractValidateException("No deposit!");
    }

    long fee = 0;
    byte[] tokenIdWithoutLeadingZero = ByteUtil.stripLeadingZeroes(tokenId);

    if (!Wallet.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid ownerAddress");
    }
    if (!Wallet.addressValid(toAddress)) {
      throw new ContractValidateException("Invalid toAddress");
    }
//    if (!TransactionUtil.validAssetName(assetName)) {
//      throw new ContractValidateException("Invalid assetName");
//    }
    if (amount <= 0) {
      throw new ContractValidateException("Amount must greater than 0.");
    }

    if (Arrays.equals(ownerAddress, toAddress)) {
      throw new ContractValidateException("Cannot transfer asset to yourself.");
    }

    AccountCapsule ownerAccount = deposit.getAccount(ownerAddress);
    if (ownerAccount == null) {
      throw new ContractValidateException("No owner account!");
    }

    if (deposit.getAssetIssue(tokenIdWithoutLeadingZero) == null) {
      throw new ContractValidateException("No asset !");
    }
    if (!deposit.getDbManager().getAssetIssueStoreFinal().has(tokenIdWithoutLeadingZero)) {
      throw new ContractValidateException("No asset !");
    }

    Map<String, Long> asset;
    if (deposit.getDbManager().getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
      asset = ownerAccount.getAssetMap();
    } else {
      asset = ownerAccount.getAssetMapV2();
    }
    if (asset.isEmpty()) {
      throw new ContractValidateException("Owner no asset!");
    }

    Long assetBalance = asset.get(ByteArray.toStr(tokenIdWithoutLeadingZero));
    if (null == assetBalance || assetBalance <= 0) {
      throw new ContractValidateException("assetBalance must greater than 0.");
    }
    if (amount > assetBalance) {
      throw new ContractValidateException("assetBalance is not sufficient.");
    }

    AccountCapsule toAccount = deposit.getAccount(toAddress);
    if (toAccount != null) {
      if (deposit.getDbManager().getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
        assetBalance = toAccount.getAssetMap().get(ByteArray.toStr(tokenIdWithoutLeadingZero));
      } else {
        assetBalance = toAccount.getAssetMapV2().get(ByteArray.toStr(tokenIdWithoutLeadingZero));
      }
      if (assetBalance != null) {
        try {
          assetBalance = Math.addExact(assetBalance, amount); //check if overflow
        } catch (Exception e) {
          logger.debug(e.getMessage(), e);
          throw new ContractValidateException(e.getMessage());
        }
      }
    } else {
      throw new ContractValidateException(
          "Validate InternalTransfer error, no ToAccount. And not allowed to create account in smart contract.");
    }

    return true;
  }

}
