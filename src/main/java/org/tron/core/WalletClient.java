package org.tron.core;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.Hash;
import org.tron.common.crypto.SymmEncoder;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Utils;
import org.tron.protos.core.TronTransaction.Transaction;

public class WalletClient {

  private static final Logger logger = LoggerFactory.getLogger("WalletClient");
  private static final String FilePath = "Wallet";
  private ECKey ecKey = null;
  private boolean loginState = false;

  /**
   * Creates a new WalletClient with a random ECKey or no ECKey.
   */
  public WalletClient(boolean genEcKey) {
    if (genEcKey) {
      this.ecKey = new ECKey(Utils.getRandom());
    }
  }

  //  Create Wallet with a pritKey
  public WalletClient(String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    this.ecKey = temKey;
  }

  public static byte[] getPassWord(String password) {
    if (password == null || "".equals(password)) {
      return null;
    }
    byte[] pwd;
    pwd = Hash.sha256(password.getBytes());
    pwd = Hash.sha256(pwd);
    pwd = Arrays.copyOfRange(pwd, 0, 16);
    return pwd;
  }

  public static byte[] getEncKey(String password) {
    if (password == null || "".equals(password)) {
      return null;
    }
    byte[] encKey;
    encKey = Hash.sha256(password.getBytes());
    encKey = Arrays.copyOfRange(encKey, 0, 16);
    return encKey;
  }

  public static boolean checkPassWord(String password) {
    if (password == null || "".equals(password)) {
      return false;
    }
    byte[] pwd = getPassWord(password);
    String pwdAsc = ByteArray.toHexString(pwd);
    String pwdInstore = loadPassword();
    return pwdAsc.equals(pwdInstore);
  }

  public static boolean passwordValid(String password) {
    if (password == null || "".equals(password)) {
      return false;
    }
    if (password.length() < 6) {
      return false;
    }
    //Other rule;
    return true;
  }

  public boolean login(String password) {
    if (password == null || "".equals(password)) {
      return false;
    }
    loginState = checkPassWord(password);
    return loginState;
  }

  public boolean isLoginState() {
    return loginState;
  }

  public void logout() {
    loginState = false;
  }

  /**
   * Get a Wallet from storage
   */
  public static WalletClient GetWalletByStorage(String password) {
    String priKeyEnced = loadPriKey();
    if (priKeyEnced == null) {
      return null;
    }
    //dec priKey
    byte[] priKeyAscEnced = priKeyEnced.getBytes();
    byte[] priKeyHexEnced = Hex.decode(priKeyAscEnced);
    byte[] aesKey = getEncKey(password);
    byte[] priKeyHexPlain = SymmEncoder.AES128EcbDec(priKeyHexEnced, aesKey);
    String priKeyPlain = Hex.toHexString(priKeyHexPlain);

    return new WalletClient(priKeyPlain);
  }

  /**
   * Creates a Wallet with an existing ECKey.
   */

  public WalletClient(final ECKey ecKey) {
    this.ecKey = ecKey;
  }

  public ECKey getEcKey() {
    return ecKey;
  }

  public byte[] getAddress() {
    return ecKey.getAddress();
  }

  /**
   * Get a Wallet from storage
   */
  public static WalletClient GetWalletByStorageIgnorPrivKey() {
    try {
      String pubKey = loadPubKey(); //04 PubKey[128]
      byte[] pubKeyAsc = pubKey.getBytes();
      byte[] pubKeyHex = Hex.decode(pubKeyAsc);
      ECKey eccKey = ECKey.fromPublicOnly(pubKeyHex);
      return new WalletClient(eccKey);
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  public static String getAddressByStorage() {
    try {
      String pubKey = loadPubKey(); //04 PubKey[128]
      byte[] pubKeyAsc = pubKey.getBytes();
      byte[] pubKeyHex = Hex.decode(pubKeyAsc);
      ECKey eccKey = ECKey.fromPublicOnly(pubKeyHex);
      return ByteArray.toHexString(eccKey.getAddress());
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  public void store(String password) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      logger.warn("store wallet fail, PrivKey is null !!");
    }
    byte[] pwd = getPassWord(password);
    String pwdAsc = ByteArray.toHexString(pwd);
    byte[] privKeyPlain = ecKey.getPrivKeyBytes();
    //encrypted by password
    byte[] aseKey = getEncKey(password);
    byte[] privKeyEnced = SymmEncoder.AES128EcbEnc(privKeyPlain, aseKey);
    String privKeyStr = ByteArray.toHexString(privKeyEnced);
    byte[] pubKeyBytes = ecKey.getPubKey();
    String pubKeyStr = ByteArray.toHexString(pubKeyBytes);
    // SAVE PASSWORD
    FileUtil.saveData(FilePath, pwdAsc, false);//ofset:0 len:32
    // SAVE PUBKEY
    FileUtil.saveData(FilePath, pubKeyStr, true);//ofset:32 len:130
    // SAVE PRIKEY
    FileUtil.saveData(FilePath, privKeyStr, true);
  }

  private static String loadPassword() {
    char[] buf = new char[0x100];
    int len = FileUtil.readData(FilePath, buf);
    if (len != 226) {
      return null;
    }
    return String.valueOf(buf, 0, 32);
  }

  public static String loadPubKey() {
    char[] buf = new char[0x100];
    int len = FileUtil.readData(FilePath, buf);
    if (len != 226) {
      return null;
    }
    return String.valueOf(buf, 32, 130);
  }

  private static String loadPriKey() {
    char[] buf = new char[0x100];
    int len = FileUtil.readData(FilePath, buf);
    if (len != 226) {
      return null;
    }
    return String.valueOf(buf, 162, 64);
  }


  public long getBalance() {
    byte[] address;
    if (this.ecKey == null) {

    } else {
      address = getAddress();
    }
    //getBalance(address);//call rpc
    return 0;
  }

  public Transaction signTransaction(Transaction tx, List<ByteString> pubKeyHashList) {
    if (this.ecKey == null || this.ecKey.getPrivKey() == null) {
      logger.error("ERROR: Can't sign,there is no private key !!");
      return null;
    }
    return TransactionUtils.sign(tx, this.ecKey, pubKeyHashList);
  }

}
