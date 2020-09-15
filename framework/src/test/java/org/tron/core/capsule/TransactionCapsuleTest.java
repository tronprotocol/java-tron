package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testng.Assert;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Result;
import org.tron.protos.Protocol.Transaction.Result.contractResult;

@Slf4j
public class TransactionCapsuleTest {

  private static Manager dbManager;
  private static TronApplicationContext context;
  private static Application AppT;
  private static String dbPath = "output_transactioncapsule_test";
  private static String OWNER_ADDRESS;
  private static String OWNER_KEY =
      "bfa67cb3dc6609b3a0c98e717d66f38ed1a159b5b3421678dfab85961c40de2f";
  private static String TO_ADDRESS;
  private static String OWNER_ACCOUNT_NOT_Exist;
  private static String KEY_11 = "1111111111111111111111111111111111111111111111111111111111111111";
  private static String KEY_12 = "1212121212121212121212121212121212121212121212121212121212121212";
  private static String KEY_13 = "1313131313131313131313131313131313131313131313131313131313131313";
  private static String KEY_21 = "2121212121212121212121212121212121212121212121212121212121212121";
  private static String KEY_22 = "2222222222222222222222222222222222222222222222222222222222222222";
  private static String KEY_23 = "2323232323232323232323232323232323232323232323232323232323232323";
  private static String KEY_31 = "3131313131313131313131313131313131313131313131313131313131313131";
  private static String KEY_32 = "3232323232323232323232323232323232323232323232323232323232323232";
  private static String KEY_33 = "3333333333333333333333333333333333333333333333333333333333333333";

  private static String KEY_ADDRESS_11;
  private static String KEY_ADDRESS_12;
  private static String KEY_ADDRESS_13;
  private static String KEY_ADDRESS_21;
  private static String KEY_ADDRESS_22;
  private static String KEY_ADDRESS_23;
  private static String KEY_ADDRESS_31;
  private static String KEY_ADDRESS_32;
  private static String KEY_ADDRESS_33;

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    AppT = ApplicationFactory.create(context);
    dbManager = context.getBean(Manager.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "03702350064AD5C1A8AA6B4D74B051199CFF8EA7";
    TO_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    OWNER_ACCOUNT_NOT_Exist =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3456";
    KEY_ADDRESS_11 = Wallet.getAddressPreFixString() + "19E7E376E7C213B7E7E7E46CC70A5DD086DAFF2A";
    KEY_ADDRESS_12 = Wallet.getAddressPreFixString() + "1C5A77D9FA7EF466951B2F01F724BCA3A5820B63";
    KEY_ADDRESS_13 = Wallet.getAddressPreFixString() + "03A1BBA60B5AA37094CF16123ADD674C01589488";

    KEY_ADDRESS_21 = Wallet.getAddressPreFixString() + "2BD0C9FE079C8FCA0E3352EB3D02839C371E5C41";
    KEY_ADDRESS_22 = Wallet.getAddressPreFixString() + "1563915E194D8CFBA1943570603F7606A3115508";
    KEY_ADDRESS_23 = Wallet.getAddressPreFixString() + "D3E442496EB66A4748912EC4A3B7A111D0B855D6";

    KEY_ADDRESS_31 = Wallet.getAddressPreFixString() + "77952CE83CA3CAD9F7ADCFABEDA85BD2F1F52008";
    KEY_ADDRESS_32 = Wallet.getAddressPreFixString() + "94622CC2A5B64A58C25A129D48A2BEEC4B65B779";
    KEY_ADDRESS_33 = Wallet.getAddressPreFixString() + "5CBDD86A2FA8DC4BDDD8A8F69DBA48572EEC07FB";
  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    AppT.shutdownServices();
    AppT.shutdown();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  /**
   * create temp Capsule test need.
   */
  @Before
  public void createAccountCapsule() {
    AccountCapsule ownerCapsule = new AccountCapsule(ByteString.copyFromUtf8("owner"),
        StringUtil.hexString2ByteString(OWNER_ADDRESS), AccountType.Normal, 10_000_000_000L);
    dbManager.getAccountStore().put(ownerCapsule.createDbKey(), ownerCapsule);
  }

  /*@Test
  public void getDefaultPermission() {
    String[] names = {"active", "owner", "other"};
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
    for (String name : names) {
      Permission permission = TransactionCapsule
          .getDefaultPermission(address, name);
      Assert.assertEquals(permission.getName(), name);
      Assert.assertEquals(permission.getThreshold(), 1);
      if ("owner".equalsIgnoreCase(name)) {
        Assert.assertEquals(permission.getParent(), "");
      } else {
        Assert.assertEquals(permission.getParent(), "owner");
      }
      Assert.assertEquals(permission.getKeysCount(), 1);
      Key key = permission.getKeys(0);
      Assert.assertEquals(key.getAddress(), address);
      Assert.assertEquals(key.getWeight(), 1);
    }
  }

  public TransferContract createTransferContract(byte[] to, byte[] owner, long amount) {
    TransferContract.Builder builder = TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    return builder.build();
  }

  public PermissionAddKeyContract createPermissionAddKeyContract(byte[] owner, String name,
      byte[] address, int weight) {
    PermissionAddKeyContract.Builder contractBuilder = PermissionAddKeyContract.newBuilder();
    contractBuilder.setOwnerAddress(ByteString.copyFrom(owner));
    contractBuilder.setPermissionName(name);
    Key.Builder keyBuilder = Key.newBuilder();
    keyBuilder.setAddress(ByteString.copyFrom(address));
    keyBuilder.setWeight(weight);
    contractBuilder.setKey(keyBuilder.build());
    return contractBuilder.build();
  }

  public void updatePermission(List<Permission> permissions, byte[] address) {
    Account account = dbManager.getAccountStore().get(address).getInstance();
    Account.Builder builder = account.toBuilder();
    for (Permission permission : permissions) {
      builder.addPermissions(permission);
    }
    dbManager.getAccountStore().put(address, new AccountCapsule(builder.build()));
  }

  public List<Permission> buildPermissions() {
    Permission.Builder builder1 = Permission.newBuilder();
    Key.Builder key11 = Key.newBuilder();
    Key.Builder key12 = Key.newBuilder();
    Key.Builder key13 = Key.newBuilder();
    key11.setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS_11))).setWeight(1);
    key12.setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS_12))).setWeight(1);
    key13.setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS_13))).setWeight(1);
    builder1.setName("owner").setThreshold(2).setParent("").addKeys(key11).addKeys(key12)
        .addKeys(key13);
    Permission.Builder builder2 = Permission.newBuilder();
    Key.Builder key21 = Key.newBuilder();
    Key.Builder key22 = Key.newBuilder();
    Key.Builder key23 = Key.newBuilder();
    key21.setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS_21))).setWeight(1);
    key22.setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS_22))).setWeight(1);
    key23.setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS_23))).setWeight(1);
    builder2.setName("active").setThreshold(2).setParent("").addKeys(key21).addKeys(key22)
        .addKeys(key23);
    Permission.Builder builder3 = Permission.newBuilder();
    Key.Builder key31 = Key.newBuilder();
    Key.Builder key32 = Key.newBuilder();
    Key.Builder key33 = Key.newBuilder();
    key31.setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS_31))).setWeight(1);
    key32.setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS_32))).setWeight(1);
    key33.setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS_33))).setWeight(1);
    builder3.setName("other").setThreshold(2).setParent("").addKeys(key31).addKeys(key32)
        .addKeys(key33);
    List<Permission> list = new ArrayList<>();
    list.add(builder1.build());
    list.add(builder2.build());
    list.add(builder3.build());
    return list;
  }

  @Test
  public void getPermission() {
    //Default "active" permission
    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    Account account = dbManager.getAccountStore().get(owner).getInstance();
    try {
      Permission permission = TransactionCapsule.getPermission(account, "active");
      Permission permission1 = TransactionCapsule
          .getDefaultPermission(ByteString.copyFrom(owner), "active");
      Assert.assertEquals(permission, permission1);
    } catch (PermissionException e) {
      Assert.assertFalse(true);
    }
    //Default "owner" permission
    try {
      Permission permission = TransactionCapsule.getPermission(account, "owner");
      Permission permission1 = TransactionCapsule
          .getDefaultPermission(ByteString.copyFrom(owner), "owner");
      Assert.assertEquals(permission, permission1);
    } catch (PermissionException e) {
      Assert.assertFalse(true);
    }
    //Add 3 permission : owner active other
    List<Permission> permissions = buildPermissions();
    updatePermission(permissions, owner);
    Permission permission1 = permissions.get(0);
    Permission permission2 = permissions.get(1);
    account = dbManager.getAccountStore().get(owner).getInstance();
    try {
      Permission permission = TransactionCapsule.getPermission(account, "owner");
      Assert.assertEquals(permission, permission1);
    } catch (PermissionException e) {
      Assert.assertFalse(true);
    }

    try {
      Permission permission = TransactionCapsule.getPermission(account, "active");
      Assert.assertEquals(permission, permission2);
    } catch (PermissionException e) {
      Assert.assertFalse(true);
    }
  }

  @Test
  public void getWeight() {
    List<Permission> permissions = buildPermissions();
    Permission permission1 = permissions.get(0);
    Permission permission2 = permissions.get(1);
    Permission permission3 = permissions.get(2);
    Assert.assertEquals(1,
        TransactionCapsule.getWeight(permission1, ByteArray.fromHexString(KEY_ADDRESS_11)));
    Assert.assertEquals(1,
        TransactionCapsule.getWeight(permission1, ByteArray.fromHexString(KEY_ADDRESS_12)));
    Assert.assertEquals(1,
        TransactionCapsule.getWeight(permission1, ByteArray.fromHexString(KEY_ADDRESS_13)));
    Assert.assertEquals(0,
        TransactionCapsule.getWeight(permission1, ByteArray.fromHexString(KEY_ADDRESS_21)));
    Assert.assertEquals(0,
        TransactionCapsule.getWeight(permission1, ByteArray.fromHexString(KEY_ADDRESS_22)));
    Assert.assertEquals(0,
        TransactionCapsule.getWeight(permission1, ByteArray.fromHexString(KEY_ADDRESS_23)));
    Assert.assertEquals(0,
        TransactionCapsule.getWeight(permission1, ByteArray.fromHexString(KEY_ADDRESS_31)));
    Assert.assertEquals(0,
        TransactionCapsule.getWeight(permission1, ByteArray.fromHexString(KEY_ADDRESS_32)));
    Assert.assertEquals(0,
        TransactionCapsule.getWeight(permission1, ByteArray.fromHexString(KEY_ADDRESS_33)));

    Assert.assertEquals(0,
        TransactionCapsule.getWeight(permission2, ByteArray.fromHexString(KEY_ADDRESS_11)));
    Assert.assertEquals(0,
        TransactionCapsule.getWeight(permission2, ByteArray.fromHexString(KEY_ADDRESS_12)));
    Assert.assertEquals(0,
        TransactionCapsule.getWeight(permission2, ByteArray.fromHexString(KEY_ADDRESS_13)));
    Assert.assertEquals(1,
        TransactionCapsule.getWeight(permission2, ByteArray.fromHexString(KEY_ADDRESS_21)));
    Assert.assertEquals(1,
        TransactionCapsule.getWeight(permission2, ByteArray.fromHexString(KEY_ADDRESS_22)));
    Assert.assertEquals(1,
        TransactionCapsule.getWeight(permission2, ByteArray.fromHexString(KEY_ADDRESS_23)));
    Assert.assertEquals(0,
        TransactionCapsule.getWeight(permission2, ByteArray.fromHexString(KEY_ADDRESS_31)));
    Assert.assertEquals(0,
        TransactionCapsule.getWeight(permission2, ByteArray.fromHexString(KEY_ADDRESS_32)));
    Assert.assertEquals(0,
        TransactionCapsule.getWeight(permission2, ByteArray.fromHexString(KEY_ADDRESS_33)));

    Assert.assertEquals(0,
        TransactionCapsule.getWeight(permission3, ByteArray.fromHexString(KEY_ADDRESS_11)));
    Assert.assertEquals(0,
        TransactionCapsule.getWeight(permission3, ByteArray.fromHexString(KEY_ADDRESS_12)));
    Assert.assertEquals(0,
        TransactionCapsule.getWeight(permission3, ByteArray.fromHexString(KEY_ADDRESS_13)));
    Assert.assertEquals(0,
        TransactionCapsule.getWeight(permission3, ByteArray.fromHexString(KEY_ADDRESS_21)));
    Assert.assertEquals(0,
        TransactionCapsule.getWeight(permission3, ByteArray.fromHexString(KEY_ADDRESS_22)));
    Assert.assertEquals(0,
        TransactionCapsule.getWeight(permission3, ByteArray.fromHexString(KEY_ADDRESS_23)));
    Assert.assertEquals(1,
        TransactionCapsule.getWeight(permission3, ByteArray.fromHexString(KEY_ADDRESS_31)));
    Assert.assertEquals(1,
        TransactionCapsule.getWeight(permission3, ByteArray.fromHexString(KEY_ADDRESS_32)));
    Assert.assertEquals(1,
        TransactionCapsule.getWeight(permission3, ByteArray.fromHexString(KEY_ADDRESS_33)));
  }

  public ArrayList<ByteString> sign(List<byte[]> priKeys, byte[] hash) {
    ArrayList<ByteString> list = new ArrayList<>();
    for (byte[] priKey : priKeys) {
      ECKey ecKey = ECKey.fromPrivate(priKey);
      ECDSASignature signature = ecKey.sign(hash);
      ByteString result = ByteString.copyFrom(signature.toByteArray());
      list.add(result);
    }
    return list;
  }

  @Test
  public void checkWeight() {
    List<Permission> permissions = buildPermissions();
    Permission permission = permissions.get(0);
    byte[] hash = Sha256Hash.hash("test".getBytes());

    //SignatureFormatException
    ArrayList<ByteString> list = new ArrayList<>();
    ByteString test = ByteString.copyFromUtf8("test");
    list.add(test);
    try {
      TransactionCapsule.checkWeight(permission, list, hash, null);
      Assert.assertFalse(true);
    } catch (SignatureException e) {
      Assert.assertFalse(true);
    } catch (PermissionException e) {
      Assert.assertFalse(true);
    } catch (SignatureFormatException e) {
      Assert.assertEquals(e.getMessage(), "Signature size is " + test.size());
    }
    //SignatureException: Header byte out of range:
    //Ignore more exception case.
    byte[] rand = new byte[65];
    new Random().nextBytes(rand);
    rand[64] = 8;  // v = 8 < 27 v += 35 > 35
    try {
      ArrayList<ByteString> list1 = new ArrayList<>();
      list1.add(ByteString.copyFrom(rand));
      TransactionCapsule.checkWeight(permission, list1, hash, null);
      Assert.assertFalse(true);
    } catch (SignatureException e) {
      Assert.assertEquals(e.getMessage(), "Header byte out of range: 35");
    } catch (PermissionException e) {
      Assert.assertFalse(true);
    } catch (SignatureFormatException e) {
      Assert.assertFalse(true);
    }
    //Permission does not contain KEY
    List<byte[]> prikeys = new ArrayList<>();
    prikeys.add(ByteArray.fromHexString(KEY_11));
    prikeys.add(ByteArray.fromHexString(KEY_21));
    ArrayList<ByteString> sign11_21 = sign(prikeys, hash);
    try {
      TransactionCapsule.checkWeight(permission, sign11_21, hash, null);
      Assert.assertFalse(true);
    } catch (SignatureException e) {
      Assert.assertFalse(true);
    } catch (PermissionException e) {
      ByteString sign21 = sign11_21.get(1);
      Assert.assertEquals(e.getMessage(),
          ByteArray.toHexString(sign21.toByteArray()) + " is signed by " + Wallet
              .encode58Check(ByteArray.fromHexString(KEY_ADDRESS_21))
              + " but it is not contained of permission.");
    } catch (SignatureFormatException e) {
      Assert.assertFalse(true);
    }
    //Too many signature
    prikeys.add(ByteArray.fromHexString(KEY_12));
    prikeys.add(ByteArray.fromHexString(KEY_13));
    ArrayList<ByteString> sign11_21_12_13 = sign(prikeys, hash);
    try {
      TransactionCapsule.checkWeight(permission, sign11_21_12_13, hash, null);
      Assert.assertFalse(true);
    } catch (SignatureException e) {
      Assert.assertFalse(true);
    } catch (PermissionException e) {
      Assert.assertEquals(e.getMessage(),
          "Signature count is " + prikeys.size() + " more than key counts of permission : "
              + permission.getKeysCount());
    } catch (SignatureFormatException e) {
      Assert.assertFalse(true);
    }

    //Sign twice by same key
    prikeys = new ArrayList<>();
    prikeys.add(ByteArray.fromHexString(KEY_11));
    prikeys.add(ByteArray.fromHexString(KEY_12));
    prikeys.add(ByteArray.fromHexString(KEY_11));
    ArrayList<ByteString> sign11_12_11 = sign(prikeys, hash);
    try {
      TransactionCapsule.checkWeight(permission, sign11_12_11, hash, null);
      Assert.assertFalse(true);
    } catch (SignatureException e) {
      Assert.assertFalse(true);
    } catch (PermissionException e) {
      Assert.assertEquals(e.getMessage(),
          WalletUtil.encode58Check(ByteArray.fromHexString(KEY_ADDRESS_11)) + " has signed twice!");
    } catch (SignatureFormatException e) {
      Assert.assertFalse(true);
    }

    //
    prikeys = new ArrayList<>();
    List<ByteString> approveList = new ArrayList<>();
    prikeys.add(ByteArray.fromHexString(KEY_11));
    ArrayList<ByteString> sign11 = sign(prikeys, hash);
    try {
      long weight = TransactionCapsule.checkWeight(permission, sign11, hash, approveList);
      Assert.assertEquals(weight, 1);
      Assert.assertEquals(approveList.size(), 1);
      Assert.assertEquals(approveList.get(0),
          ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS_11)));
    } catch (SignatureException e) {
      Assert.assertFalse(true);
    } catch (PermissionException e) {
      Assert.assertFalse(true);
    } catch (SignatureFormatException e) {
      Assert.assertFalse(true);
    }

    approveList = new ArrayList<>();
    prikeys.add(ByteArray.fromHexString(KEY_12));
    ArrayList<ByteString> sign11_12 = sign(prikeys, hash);
    try {
      long weight = TransactionCapsule.checkWeight(permission, sign11_12, hash, approveList);
      Assert.assertEquals(weight, 2);
      Assert.assertEquals(approveList.size(), 2);
      Assert.assertEquals(approveList.get(0),
          ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS_11)));
      Assert.assertEquals(approveList.get(1),
          ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS_12)));
    } catch (SignatureException e) {
      Assert.assertFalse(true);
    } catch (PermissionException e) {
      Assert.assertFalse(true);
    } catch (SignatureFormatException e) {
      Assert.assertFalse(true);
    }

    approveList = new ArrayList<>();
    prikeys.add(ByteArray.fromHexString(KEY_13));
    ArrayList<ByteString> sign11_12_13 = sign(prikeys, hash);
    try {
      long weight = TransactionCapsule.checkWeight(permission, sign11_12_13, hash, approveList);
      Assert.assertEquals(weight, 3);
      Assert.assertEquals(approveList.size(), 3);
      Assert.assertEquals(approveList.get(0),
          ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS_11)));
      Assert.assertEquals(approveList.get(1),
          ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS_12)));
      Assert.assertEquals(approveList.get(2),
          ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS_13)));
    } catch (SignatureException e) {
      Assert.assertFalse(true);
    } catch (PermissionException e) {
      Assert.assertFalse(true);
    } catch (SignatureFormatException e) {
      Assert.assertFalse(true);
    }
  }

  @Test
  public void addSign() {

    byte[] to = ByteArray.fromHexString(TO_ADDRESS);
    byte[] owner_not_exist = ByteArray.fromHexString(OWNER_ACCOUNT_NOT_Exist);
    TransferContract transferContract = createTransferContract(to, owner_not_exist, 1);
    Transaction.Builder trxBuilder = Transaction.newBuilder();
    Transaction.raw.Builder rawBuilder = Transaction.raw.newBuilder();
    Contract.Builder contractBuilder = Contract.newBuilder();
    contractBuilder.setType(ContractType.TransferContract).setParameter(Any.pack(transferContract))
        .build();
    rawBuilder.addContract(contractBuilder);
    trxBuilder.setRawData(rawBuilder);
    AccountStore accountStore = dbManager.getAccountStore();
    TransactionCapsule transactionCapsule = new TransactionCapsule(trxBuilder.build());
    //Accout not exist
    try {
      transactionCapsule.addSign(ByteArray.fromHexString(KEY_11), accountStore);
      Assert.assertFalse(true);
    } catch (PermissionException e) {
      Assert.assertEquals(e.getMessage(), "Account is not exist!");
    } catch (SignatureException e) {
      Assert.assertFalse(true);
    } catch (SignatureFormatException e) {
      Assert.assertFalse(true);
    }

    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    transferContract = createTransferContract(to, owner, 1);
    transactionCapsule = new TransactionCapsule(transferContract, accountStore);
    //Defalut permission
    try {
      transactionCapsule.addSign(ByteArray.fromHexString(KEY_11), accountStore);
      Assert.assertFalse(true);
    } catch (PermissionException e) {
      Assert.assertEquals(e.getMessage(),
          KEY_11 + "'s address is " + WalletUtil
          .encode58Check(ByteArray.fromHexString(KEY_ADDRESS_11))
              + " but it is not contained of permission.");
    } catch (SignatureException e) {
      Assert.assertFalse(true);
    } catch (SignatureFormatException e) {
      Assert.assertFalse(true);
    }

    try {
      transactionCapsule.addSign(ByteArray.fromHexString(OWNER_KEY), accountStore);
      Assert.assertEquals(transactionCapsule.getInstance().getSignatureCount(), 1);
      ByteString signature = transactionCapsule.getInstance().getSignature(0);
      Assert.assertEquals(signature.size(), 65);
      byte[] sign = signature.toByteArray();
      byte[] r = ByteArray.subArray(sign, 0, 32);
      byte[] s = ByteArray.subArray(sign, 32, 64);
      byte v = sign[64];
      ECDSASignature ecdsaSignature = ECDSASignature.fromComponents(r, s, (byte) (v + 27));
      byte[] address = ECKey
          .signatureToAddress(transactionCapsule.getTransactionId().getBytes(), ecdsaSignature);
      Assert.assertTrue(Arrays.equals(address, ByteArray.fromHexString(OWNER_ADDRESS)));
    } catch (PermissionException e) {
      Assert.assertFalse(true);
    } catch (SignatureException e) {
      Assert.assertFalse(true);
    } catch (SignatureFormatException e) {
      Assert.assertFalse(true);
    }
    // Sign twice
    try {
      transactionCapsule.addSign(ByteArray.fromHexString(OWNER_KEY), accountStore);
      Assert.assertFalse(true);
    } catch (PermissionException e) {
      Assert.assertEquals(e.getMessage(),
          WalletUtil.encode58Check(ByteArray.fromHexString(OWNER_ADDRESS)) + " had signed!");
    } catch (SignatureException e) {
      Assert.assertFalse(true);
    } catch (SignatureFormatException e) {
      Assert.assertFalse(true);
    }
    //Update permission, can signed by key21 key22 key23
    List<Permission> permissions = buildPermissions();
    Account account = accountStore.get(ByteArray.fromHexString(OWNER_ADDRESS)).getInstance();
    Account.Builder builder = account.toBuilder();
    builder.addPermissions(permissions.get(0));
    builder.addPermissions(permissions.get(1));
    builder.addPermissions(permissions.get(2));
    accountStore.put(ByteArray.fromHexString(OWNER_ADDRESS), new AccountCapsule(builder.build()));

    transactionCapsule = new TransactionCapsule(transferContract, accountStore);
    try {
      transactionCapsule.addSign(ByteArray.fromHexString(OWNER_KEY), accountStore);
      Assert.assertFalse(true);
    } catch (PermissionException e) {
      Assert.assertEquals(e.getMessage(),
          OWNER_KEY + "'s address is " + Wallet
              .encode58Check(ByteArray.fromHexString(OWNER_ADDRESS))
              + " but it is not contained of permission.");
    } catch (SignatureException e) {
      Assert.assertFalse(true);
    } catch (SignatureFormatException e) {
      Assert.assertFalse(true);
    }
    //Sign KEY_21
    try {
      transactionCapsule.addSign(ByteArray.fromHexString(KEY_21), accountStore);
      Assert.assertEquals(transactionCapsule.getInstance().getSignatureCount(), 1);
      ByteString signature = transactionCapsule.getInstance().getSignature(0);
      Assert.assertEquals(signature.size(), 65);
      byte[] sign = signature.toByteArray();
      byte[] r = ByteArray.subArray(sign, 0, 32);
      byte[] s = ByteArray.subArray(sign, 32, 64);
      byte v = sign[64];
      ECDSASignature ecdsaSignature = ECDSASignature.fromComponents(r, s, (byte) (v + 27));
      byte[] address = ECKey
          .signatureToAddress(transactionCapsule.getTransactionId().getBytes(), ecdsaSignature);
      Assert.assertTrue(Arrays.equals(address, ByteArray.fromHexString(KEY_ADDRESS_21)));
    } catch (PermissionException e) {
      Assert.assertFalse(true);
    } catch (SignatureException e) {
      Assert.assertFalse(true);
    } catch (SignatureFormatException e) {
      Assert.assertFalse(true);
    }
    //Sign KEY_12
    try {
      transactionCapsule.addSign(ByteArray.fromHexString(KEY_22), accountStore);
      Assert.assertEquals(transactionCapsule.getInstance().getSignatureCount(), 2);
      ByteString signature = transactionCapsule.getInstance().getSignature(0);
      Assert.assertEquals(signature.size(), 65);
      byte[] sign = signature.toByteArray();
      byte[] r21 = ByteArray.subArray(sign, 0, 32);
      byte[] s21 = ByteArray.subArray(sign, 32, 64);
      byte v21 = sign[64];
      ECDSASignature ecdsaSignature11 = ECDSASignature.fromComponents(r21, s21, (byte) (v21 + 27));
      byte[] address21 = ECKey
          .signatureToAddress(transactionCapsule.getTransactionId().getBytes(), ecdsaSignature11);
      Assert.assertTrue(Arrays.equals(address21, ByteArray.fromHexString(KEY_ADDRESS_21)));

      ByteString signature1 = transactionCapsule.getInstance().getSignature(1);

      byte[] r22 = ByteArray.subArray(signature1.toByteArray(), 0, 32);
      byte[] s22 = ByteArray.subArray(signature1.toByteArray(), 32, 64);
      byte v22 = signature1.toByteArray()[64];
      ECDSASignature ecdsaSignature12 = ECDSASignature.fromComponents(r22, s22, (byte) (v22 + 27));
      byte[] address22 = ECKey
          .signatureToAddress(transactionCapsule.getTransactionId().getBytes(), ecdsaSignature12);
      Assert.assertTrue(Arrays.equals(address22, ByteArray.fromHexString(KEY_ADDRESS_22)));
    } catch (PermissionException e) {
      Assert.assertFalse(true);
    } catch (SignatureException e) {
      Assert.assertFalse(true);
    } catch (SignatureFormatException e) {
      Assert.assertFalse(true);
    }
    //Sign KEY_23
    try {
      transactionCapsule.addSign(ByteArray.fromHexString(KEY_23), accountStore);
      Assert.assertEquals(transactionCapsule.getInstance().getSignatureCount(), 3);
      ByteString signature = transactionCapsule.getInstance().getSignature(0);
      Assert.assertEquals(signature.size(), 65);
      byte[] sign = signature.toByteArray();
      byte[] r21 = ByteArray.subArray(sign, 0, 32);
      byte[] s21 = ByteArray.subArray(sign, 32, 64);
      byte v21 = sign[64];
      ECDSASignature ecdsaSignature21 = ECDSASignature.fromComponents(r21, s21, (byte) (v21 + 27));
      byte[] address21 = ECKey
          .signatureToAddress(transactionCapsule.getTransactionId().getBytes(), ecdsaSignature21);
      Assert.assertTrue(Arrays.equals(address21, ByteArray.fromHexString(KEY_ADDRESS_21)));

      ByteString signature1 = transactionCapsule.getInstance().getSignature(1);
      Assert.assertEquals(signature1.size(), 65);
      byte[] sign1 = signature1.toByteArray();
      byte[] r22 = ByteArray.subArray(sign1, 0, 32);
      byte[] s22 = ByteArray.subArray(sign1, 32, 64);
      byte v22 = sign1[64];
      ECDSASignature ecdsaSignature22 = ECDSASignature.fromComponents(r22, s22, (byte) (v22 + 27));
      byte[] address22 = ECKey
          .signatureToAddress(transactionCapsule.getTransactionId().getBytes(), ecdsaSignature22);
      Assert.assertTrue(Arrays.equals(address22, ByteArray.fromHexString(KEY_ADDRESS_22)));

      ByteString signature2 = transactionCapsule.getInstance().getSignature(2);
      Assert.assertEquals(signature2.size(), 65);
      byte[] sign2 = signature2.toByteArray();
      byte[] r23 = ByteArray.subArray(sign2, 0, 32);
      byte[] s23 = ByteArray.subArray(sign2, 32, 64);
      byte v23 = sign2[64];
      ECDSASignature ecdsaSignature23 = ECDSASignature.fromComponents(r23, s23, (byte) (v23 + 27));
      byte[] address23 = ECKey
          .signatureToAddress(transactionCapsule.getTransactionId().getBytes(), ecdsaSignature23);
      Assert.assertTrue(Arrays.equals(address23, ByteArray.fromHexString(KEY_ADDRESS_23)));
    } catch (PermissionException e) {
      Assert.assertFalse(true);
    } catch (SignatureException e) {
      Assert.assertFalse(true);
    } catch (SignatureFormatException e) {
      Assert.assertFalse(true);
    }
    //Sign KEY_11, throw exception
    try {
      transactionCapsule.addSign(ByteArray.fromHexString(KEY_11), accountStore);
      Assert.assertFalse(true);
    } catch (PermissionException e) {
      Assert.assertEquals(e.getMessage(),
          KEY_11 + "'s address is " + Wallet
              .encode58Check(ByteArray.fromHexString(KEY_ADDRESS_11))
              + " but it is not contained of permission.");
    } catch (SignatureException e) {
      Assert.assertFalse(true);
    } catch (SignatureFormatException e) {
      Assert.assertFalse(true);
    }
    //invalidate signature
    transactionCapsule = new TransactionCapsule(transferContract, accountStore);
    Transaction.Builder builder1 = transactionCapsule.getInstance().toBuilder();
    builder1.addSignature(ByteString.copyFromUtf8("test"));
    transactionCapsule = new TransactionCapsule(builder1.build());
    //Sign KEY_21, throw exception
    try {
      transactionCapsule.addSign(ByteArray.fromHexString(KEY_11), accountStore);
      Assert.assertFalse(true);
    } catch (PermissionException e) {
      Assert.assertFalse(true);
    } catch (SignatureException e) {
      Assert.assertFalse(true);
    } catch (SignatureFormatException e) {
      Assert.assertEquals(e.getMessage(), "Signature size is " + "test".length());
    }

    //invalidate signature
    transactionCapsule = new TransactionCapsule(transferContract, accountStore);
    builder1 = transactionCapsule.getInstance().toBuilder();
    builder1.addSignature(ByteString.copyFromUtf8("test"));
    transactionCapsule = new TransactionCapsule(builder1.build());
    //Sign KEY_21, throw exception
    try {
      transactionCapsule.addSign(ByteArray.fromHexString(KEY_11), accountStore);
      Assert.assertFalse(true);
    } catch (PermissionException e) {
      Assert.assertFalse(true);
    } catch (SignatureException e) {
      Assert.assertFalse(true);
    } catch (SignatureFormatException e) {
      Assert.assertEquals(e.getMessage(), "Signature size is " + "test".length());
    }
    //transaction already have a signature signed by a invalidate key
    //that the key is not in the permission.
    transactionCapsule = new TransactionCapsule(transferContract, accountStore);
    List<byte[]> prikeys = new ArrayList<>();
    prikeys.add(ByteArray.fromHexString(KEY_11));
    ArrayList<ByteString> sign11 = sign(prikeys, transactionCapsule.getTransactionId().getBytes());
    builder1 = transactionCapsule.getInstance().toBuilder();
    builder1.addAllSignature(sign11);
    transactionCapsule = new TransactionCapsule(builder1.build());

    try {
      transactionCapsule.addSign(ByteArray.fromHexString(KEY_21), accountStore);
      Assert.assertFalse(true);
    } catch (PermissionException e) {
      Assert.assertEquals(e.getMessage(),
          ByteArray.toHexString(sign11.get(0).toByteArray()) + " is signed by " + Wallet
              .encode58Check(ByteArray.fromHexString(KEY_ADDRESS_11))
              + " but it is not contained of permission.");
    } catch (SignatureException e) {
      Assert.assertFalse(true);
    } catch (SignatureFormatException e) {
      Assert.assertFalse(true);
    }
  }

  @Test
  // test   public static boolean validateSignature(Transaction.Contract contract,
   ByteString sigs, byte[] hash, AccountStore accountStore)
  public void validateSignature0() {
    //Update permission, can signed by key21 key22 key23
    AccountStore accountStore = dbManager.getAccountStore();
    List<Permission> permissions = buildPermissions();

    byte[] to = ByteArray.fromHexString(TO_ADDRESS);
    byte[] owner_not_exist = ByteArray.fromHexString(OWNER_ACCOUNT_NOT_Exist);
    TransferContract transferContract = createTransferContract(to, owner_not_exist, 1);
    Transaction.Builder trxBuilder = Transaction
        .newBuilder();
    Transaction.raw.Builder rawBuilder = Transaction.raw.newBuilder();
    Contract.Builder contractBuilder = Contract.newBuilder();
    contractBuilder.setType(ContractType.TransferContract).setParameter(Any.pack(transferContract));
    rawBuilder.addContract(contractBuilder.build());
    trxBuilder.setRawData(rawBuilder.build());
    List<byte[]> prikeys = new ArrayList<>();
    prikeys.add(ByteArray.fromHexString(KEY_21));
    ArrayList<ByteString> sign = sign(prikeys, Sha256Hash.hash(rawBuilder.build().toByteArray()));
    trxBuilder.addAllSignature(sign);

    Account account = accountStore.get(ByteArray.fromHexString(OWNER_ADDRESS)).getInstance();
    Account.Builder builder = account.toBuilder();
    builder.clearPermissions();
    builder.addPermissions(permissions.get(0));
    builder.addPermissions(permissions.get(1));
    builder.addPermissions(permissions.get(2));
    accountStore.put(ByteArray.fromHexString(OWNER_ADDRESS), new AccountCapsule(builder.build()));
    byte[] hash = Sha256Hash.hash("test".getBytes());

    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    transferContract = createTransferContract(to, owner, 1);
    contractBuilder = Contract.newBuilder();
    contractBuilder.setParameter(Any.pack(transferContract)).setType(ContractType.TransferContract);
    rawBuilder.clearContract().addContract(contractBuilder.build());
    trxBuilder.setRawData(rawBuilder.build());

    //SignatureFormatException
    ByteString test = ByteString.copyFromUtf8("test");
    trxBuilder.clearSignature().addSignature(test);
    try {
      TransactionCapsule.validateSignature(trxBuilder.build(), hash, accountStore);
      Assert.assertFalse(true);
    } catch (SignatureException e) {
      Assert.assertFalse(true);
    } catch (PermissionException e) {
      Assert.assertFalse(true);
    } catch (SignatureFormatException e) {
      Assert.assertEquals(e.getMessage(), "Signature size is " + test.size());
    }
    //SignatureException: Header byte out of range:
    //Ignore more exception case.
    byte[] rand = new byte[65];
    new Random().nextBytes(rand);
    rand[64] = 8;  // v = 8 < 27 v += 35 > 35
    trxBuilder.clearSignature().addSignature(ByteString.copyFrom(rand));
    try {
      TransactionCapsule.validateSignature(trxBuilder.build(), hash,
          accountStore);
      Assert.assertFalse(true);
    } catch (SignatureException e) {
      Assert.assertEquals(e.getMessage(), "Header byte out of range: 35");
    } catch (PermissionException e) {
      Assert.assertFalse(true);
    } catch (SignatureFormatException e) {
      Assert.assertFalse(true);
    }
    //Permission is not contain KEY
    prikeys = new ArrayList<>();
    prikeys.clear();
    prikeys.add(ByteArray.fromHexString(KEY_21));
    prikeys.add(ByteArray.fromHexString(KEY_11));
    ArrayList<ByteString> sign21_11 = sign(prikeys, hash);
    trxBuilder.clearSignature().addAllSignature(sign21_11);
    try {
      TransactionCapsule.validateSignature(trxBuilder.build(), hash, accountStore);
      Assert.assertFalse(true);
    } catch (SignatureException e) {
      Assert.assertFalse(true);
    } catch (PermissionException e) {
      ByteString sign21 = sign21_11.get(1);
      Assert.assertEquals(e.getMessage(),
          ByteArray.toHexString(sign21.toByteArray()) + " is signed by " + Wallet
              .encode58Check(ByteArray.fromHexString(KEY_ADDRESS_11))
              + " but it is not contained of permission.");
    } catch (SignatureFormatException e) {
      Assert.assertFalse(true);
    }
    //Too many signature
    prikeys.add(ByteArray.fromHexString(KEY_22));
    prikeys.add(ByteArray.fromHexString(KEY_23));
    ArrayList<ByteString> sign21_11_22_23 = sign(prikeys, hash);
    trxBuilder.clearSignature().addAllSignature(sign21_11_22_23);
    try {
      TransactionCapsule
          .validateSignature(trxBuilder.build(), hash, accountStore);
      Assert.assertFalse(true);
    } catch (SignatureException e) {
      Assert.assertFalse(true);
    } catch (PermissionException e) {
      Assert.assertEquals(e.getMessage(),
          "Signature count is " + prikeys.size() + " more than key counts of permission : "
              + permissions.get(1).getKeysCount());
    } catch (SignatureFormatException e) {
      Assert.assertFalse(true);
    }

    //Sign twices by same key
    prikeys = new ArrayList<>();
    prikeys.add(ByteArray.fromHexString(KEY_21));
    prikeys.add(ByteArray.fromHexString(KEY_22));
    prikeys.add(ByteArray.fromHexString(KEY_21));
    ArrayList<ByteString> sign21_22_21 = sign(prikeys, hash);
    trxBuilder.clearSignature().addAllSignature(sign21_22_21);
    try {
      TransactionCapsule
          .validateSignature(trxBuilder.build(), hash, accountStore);
      Assert.assertFalse(true);
    } catch (SignatureException e) {
      Assert.assertFalse(true);
    } catch (PermissionException e) {
      Assert.assertEquals(e.getMessage(),
          WalletUtil.encode58Check(ByteArray.fromHexString(KEY_ADDRESS_21)) + " has signed twice!");
    } catch (SignatureFormatException e) {
      Assert.assertFalse(true);
    }

    //
    prikeys = new ArrayList<>();
    prikeys.add(ByteArray.fromHexString(KEY_21));
    ArrayList<ByteString> sign21 = sign(prikeys, hash);
    trxBuilder.clearSignature().addAllSignature(sign21);
    try {
      boolean result = TransactionCapsule
          .validateSignature(trxBuilder.build(), hash, accountStore);
      Assert.assertFalse(result);
    } catch (SignatureException e) {
      Assert.assertFalse(true);
    } catch (PermissionException e) {
      Assert.assertFalse(true);
    } catch (SignatureFormatException e) {
      Assert.assertFalse(true);
    }

    prikeys.add(ByteArray.fromHexString(KEY_22));
    ArrayList<ByteString> sign21_22 = sign(prikeys, hash);
    trxBuilder.clearSignature().addAllSignature(sign21_22);
    try {
      boolean result = TransactionCapsule
          .validateSignature(trxBuilder.build(), hash, accountStore);
      Assert.assertTrue(result);
    } catch (SignatureException e) {
      Assert.assertFalse(true);
    } catch (PermissionException e) {
      Assert.assertFalse(true);
    } catch (SignatureFormatException e) {
      Assert.assertFalse(true);
    }

    prikeys.add(ByteArray.fromHexString(KEY_23));
    ArrayList<ByteString> sign21_22_23 = sign(prikeys, hash);
    trxBuilder.clearSignature().addAllSignature(sign21_22_23);
    try {
      boolean result = TransactionCapsule
          .validateSignature(trxBuilder.build(), hash, accountStore);
      Assert.assertTrue(result);
    } catch (SignatureException e) {
      Assert.assertFalse(true);
    } catch (PermissionException e) {
      Assert.assertFalse(true);
    } catch (SignatureFormatException e) {
      Assert.assertFalse(true);
    }
  }

  @Test
  // test   public boolean validateSignature(AccountStore accountStore)
  public void validateSignature1() {
    //Update permission, can signed by key21 key22 key23
    List<Permission> permissions = buildPermissions();
    Account account = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS))
        .getInstance();
    Account.Builder builder = account.toBuilder();
    builder.clearPermissions();
    builder.addPermissions(permissions.get(0));
    builder.addPermissions(permissions.get(1));
    builder.addPermissions(permissions.get(2));
    dbManager.getAccountStore()
        .put(ByteArray.fromHexString(OWNER_ADDRESS), new AccountCapsule(builder.build()));

    byte[] to = ByteArray.fromHexString(TO_ADDRESS);
    byte[] owner_not_exist = ByteArray.fromHexString(OWNER_ACCOUNT_NOT_Exist);
    TransferContract transferContract = createTransferContract(to, owner_not_exist, 1);
    Transaction.Builder trxBuilder = Transaction.newBuilder();
    Transaction.raw.Builder rawBuilder = Transaction.raw.newBuilder();
    Contract.Builder contractBuilder = Contract.newBuilder();
    contractBuilder.setType(ContractType.TransferContract).setParameter(Any.pack(transferContract))
        .build();
    rawBuilder.addContract(contractBuilder);
    trxBuilder.setRawData(rawBuilder);
    TransactionCapsule transactionCapsule = new TransactionCapsule(trxBuilder.build());
    List<byte[]> prikeys = new ArrayList<>();
    prikeys.add(ByteArray.fromHexString(KEY_21));
    ArrayList<ByteString> sign = sign(prikeys, Sha256Hash.hash(rawBuilder.build().toByteArray()));
    trxBuilder.addAllSignature(sign);
    transactionCapsule = new TransactionCapsule(trxBuilder.build());

    // no contract
    prikeys.clear();
    prikeys.add(ByteArray.fromHexString(KEY_21));
    trxBuilder = Transaction.newBuilder();
    rawBuilder = Transaction.raw.newBuilder();
    rawBuilder.setTimestamp(System.currentTimeMillis());
    trxBuilder.setRawData(rawBuilder);
    sign = sign(prikeys, Sha256Hash.hash(rawBuilder.build().toByteArray()));
    trxBuilder.addAllSignature(sign);
    transactionCapsule = new TransactionCapsule(trxBuilder.build());
    try {
      transactionCapsule.validateSignature(dbManager);
      Assert.assertFalse(true);
    } catch (ValidateSignatureException e) {
      Assert.assertEquals(e.getMessage(), "miss sig or contract");
    }
    // no sign
    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    transferContract = createTransferContract(to, owner, 1);
    transactionCapsule = new TransactionCapsule(transferContract, dbManager.getAccountStore());
    try {
      transactionCapsule.validateSignature(dbManager);
      Assert.assertFalse(true);
    } catch (ValidateSignatureException e) {
      Assert.assertEquals(e.getMessage(), "miss sig or contract");
    }

    transactionCapsule = new TransactionCapsule(transferContract, dbManager.getAccountStore());
    byte[] hash = transactionCapsule.getTransactionId().getBytes();
    trxBuilder = transactionCapsule.getInstance().toBuilder();
    //SignatureFormatException
    ByteString test = ByteString.copyFromUtf8("test");
    trxBuilder.clearSignature();
    trxBuilder.addSignature(test);
    transactionCapsule = new TransactionCapsule(trxBuilder.build());
    try {
      transactionCapsule.validateSignature(dbManager);
      Assert.assertFalse(true);
    } catch (ValidateSignatureException e) {
      Assert.assertEquals(e.getMessage(), "Signature size is " + test.size());
    }
    //SignatureException: Header byte out of range:
    //Ignore more exception case.
    byte[] rand = new byte[65];
    new Random().nextBytes(rand);
    rand[64] = 8;  // v = 8 < 27 v += 35 > 35
    trxBuilder.clearSignature();
    trxBuilder.addSignature(ByteString.copyFrom(rand));
    transactionCapsule = new TransactionCapsule(trxBuilder.build());
    try {
      transactionCapsule.validateSignature(dbManager);
      Assert.assertFalse(true);
    } catch (ValidateSignatureException e) {
      Assert.assertEquals(e.getMessage(), "Header byte out of range: 35");
    }
    //Permission is not contain KEY
    prikeys.clear();
    prikeys.add(ByteArray.fromHexString(KEY_21));
    prikeys.add(ByteArray.fromHexString(KEY_11));
    ArrayList<ByteString> sign21_11 = sign(prikeys, hash);
    trxBuilder.clearSignature();
    trxBuilder.addAllSignature(sign21_11);
    transactionCapsule = new TransactionCapsule(trxBuilder.build());
    try {
      transactionCapsule.validateSignature(dbManager);
      Assert.assertFalse(true);
    } catch (ValidateSignatureException e) {
      ByteString sign21 = sign21_11.get(1);
      Assert.assertEquals(e.getMessage(),
          ByteArray.toHexString(sign21.toByteArray()) + " is signed by " + Wallet
              .encode58Check(ByteArray.fromHexString(KEY_ADDRESS_11))
              + " but it is not contained of permission.");
    }
    //Too many signature
    prikeys.add(ByteArray.fromHexString(KEY_22));
    prikeys.add(ByteArray.fromHexString(KEY_23));
    ArrayList<ByteString> sign21_11_22_23 = sign(prikeys, hash);
    trxBuilder.clearSignature();
    trxBuilder.addAllSignature(sign21_11_22_23);
    transactionCapsule = new TransactionCapsule(trxBuilder.build());
    try {
      transactionCapsule.validateSignature(dbManager);
      Assert.assertFalse(true);
    } catch (ValidateSignatureException e) {
      Assert.assertEquals(e.getMessage(),
          "Signature count is " + prikeys.size() + " more than key counts of permission : "
              + permissions.get(1).getKeysCount());
    }

    //Sign twices by same key
    prikeys = new ArrayList<>();
    prikeys.add(ByteArray.fromHexString(KEY_21));
    prikeys.add(ByteArray.fromHexString(KEY_22));
    prikeys.add(ByteArray.fromHexString(KEY_21));
    ArrayList<ByteString> sign21_22_21 = sign(prikeys, hash);
    trxBuilder.clearSignature();
    trxBuilder.addAllSignature(sign21_22_21);
    transactionCapsule = new TransactionCapsule(trxBuilder.build());
    try {
      transactionCapsule.validateSignature(dbManager);
      Assert.assertFalse(true);
    } catch (ValidateSignatureException e) {
      Assert.assertEquals(e.getMessage(),
          WalletUtil.encode58Check(ByteArray.fromHexString(KEY_ADDRESS_21)) + " has signed twice!");
    }

    //
    prikeys = new ArrayList<>();
    prikeys.add(ByteArray.fromHexString(KEY_21));
    ArrayList<ByteString> sign21 = sign(prikeys, hash);
    trxBuilder.clearSignature();
    trxBuilder.addAllSignature(sign21);
    transactionCapsule = new TransactionCapsule(trxBuilder.build());
    try {
      transactionCapsule.validateSignature(dbManager);
      Assert.assertFalse(true);
    } catch (ValidateSignatureException e) {
      Assert.assertEquals(e.getMessage(), "sig error");
    }

    prikeys.add(ByteArray.fromHexString(KEY_22));
    ArrayList<ByteString> sign21_22 = sign(prikeys, hash);
    trxBuilder.clearSignature();
    trxBuilder.addAllSignature(sign21_22);
    transactionCapsule = new TransactionCapsule(trxBuilder.build());
    try {
      boolean result = transactionCapsule.validateSignature(dbManager);
      Assert.assertTrue(result);
    } catch (ValidateSignatureException e) {
      Assert.assertFalse(true);
    }

    prikeys.add(ByteArray.fromHexString(KEY_23));
    ArrayList<ByteString> sign21_22_23 = sign(prikeys, hash);
    trxBuilder.clearSignature();
    trxBuilder.addAllSignature(sign21_22_23);
    transactionCapsule = new TransactionCapsule(trxBuilder.build());
    try {
      boolean result = transactionCapsule.validateSignature(dbManager);
      Assert.assertTrue(result);
    } catch (ValidateSignatureException e) {
      Assert.assertFalse(true);
    }
  }*/

  @Test
  public void trxCapsuleClearTest() {
    Transaction tx = Transaction.newBuilder()
        .addRet(Result.newBuilder().setContractRet(contractResult.OUT_OF_TIME).build()).build();
    TransactionCapsule trxCap = new TransactionCapsule(tx);
    Result.contractResult contractResult = trxCap.getContractResult();
    trxCap.resetResult();
    Assert.assertEquals(trxCap.getInstance().getRetCount(), 0);
    trxCap.setResultCode(contractResult);
    Assert.assertEquals(trxCap.getInstance()
        .getRet(0).getContractRet(), contractResult.OUT_OF_TIME);
  }
}