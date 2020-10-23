package org.tron.keystore;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.SignUtils;
import org.tron.common.utils.Utils;
import org.tron.core.config.args.Args;
import org.tron.core.exception.CipherException;

/**
 * Utility functions for working with Wallet files.
 */
public class WalletUtils {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public static String generateFullNewWalletFile(String password, File destinationDirectory)
      throws NoSuchAlgorithmException, NoSuchProviderException,
      InvalidAlgorithmParameterException, CipherException, IOException {

    return generateNewWalletFile(password, destinationDirectory, true);
  }

  public static String generateLightNewWalletFile(String password, File destinationDirectory)
      throws NoSuchAlgorithmException, NoSuchProviderException,
      InvalidAlgorithmParameterException, CipherException, IOException {

    return generateNewWalletFile(password, destinationDirectory, false);
  }

  public static String generateNewWalletFile(
      String password, File destinationDirectory, boolean useFullScrypt)
      throws CipherException, IOException, InvalidAlgorithmParameterException,
      NoSuchAlgorithmException, NoSuchProviderException {

    SignInterface ecKeyPair = SignUtils.getGeneratedRandomSign(Utils.getRandom(),
        Args.getInstance().isECKeyCryptoEngine());
    return generateWalletFile(password, ecKeyPair, destinationDirectory, useFullScrypt);
  }

  public static String generateWalletFile(
      String password, SignInterface ecKeyPair, File destinationDirectory, boolean useFullScrypt)
      throws CipherException, IOException {

    WalletFile walletFile;
    if (useFullScrypt) {
      walletFile = Wallet.createStandard(password, ecKeyPair);
    } else {
      walletFile = Wallet.createLight(password, ecKeyPair);
    }

    String fileName = getWalletFileName(walletFile);
    File destination = new File(destinationDirectory, fileName);

    objectMapper.writeValue(destination, walletFile);

    return fileName;
  }

  public static Credentials loadCredentials(String password, File source)
      throws IOException, CipherException {
    WalletFile walletFile = objectMapper.readValue(source, WalletFile.class);
    return Credentials.create(Wallet.decrypt(password, walletFile));
  }

  private static String getWalletFileName(WalletFile walletFile) {
    DateTimeFormatter format = DateTimeFormatter.ofPattern(
        "'UTC--'yyyy-MM-dd'T'HH-mm-ss.nVV'--'");
    ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

    return now.format(format) + walletFile.getAddress() + ".json";
  }

  public static String getDefaultKeyDirectory() {
    return getDefaultKeyDirectory(System.getProperty("os.name"));
  }

  static String getDefaultKeyDirectory(String osName1) {
    String osName = osName1.toLowerCase();

    if (osName.startsWith("mac")) {
      return String.format(
          "%s%sLibrary%sEthereum", System.getProperty("user.home"), File.separator,
          File.separator);
    } else if (osName.startsWith("win")) {
      return String.format("%s%sEthereum", System.getenv("APPDATA"), File.separator);
    } else {
      return String.format("%s%s.ethereum", System.getProperty("user.home"), File.separator);
    }
  }

  public static String getTestnetKeyDirectory() {
    return String.format(
        "%s%stestnet%skeystore", getDefaultKeyDirectory(), File.separator, File.separator);
  }

  public static String getMainnetKeyDirectory() {
    return String.format("%s%skeystore", getDefaultKeyDirectory(), File.separator);
  }

  public static boolean passwordValid(String password) {
    if (StringUtils.isEmpty(password)) {
      return false;
    }
    if (password.length() < 6) {
      return false;
    }
    //Other rule;
    return true;
  }

  public static String inputPassword() {
    Scanner in = null;
    String password;
    Console cons = System.console();
    if (cons == null) {
      in = new Scanner(System.in);
    }
    while (true) {
      if (cons != null) {
        char[] pwd = cons.readPassword("password: ");
        password = String.valueOf(pwd);
      } else {
        String input = in.nextLine().trim();
        password = input.split("\\s+")[0];
      }
      if (passwordValid(password)) {
        return password;
      }
      System.out.println("Invalid password, please input again.");
    }
  }

  public static String inputPassword2Twice() {
    String password0;
    while (true) {
      System.out.println("Please input password.");
      password0 = inputPassword();
      System.out.println("Please input password again.");
      String password1 = inputPassword();
      if (password0.equals(password1)) {
        break;
      }
      System.out.println("Two passwords do not match, please input again.");
    }
    return password0;
  }
}
