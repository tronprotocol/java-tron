package org.tron.core.capsule;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.PermissionException;
import org.tron.protos.Contract.PermissionAddKeyContract;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Key;
import org.tron.protos.Protocol.Permission;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

@Slf4j
public class TransactionCapsuleTest {

  //  private static BlockCapsule blockCapsule0 = new BlockCapsule(1,
//      Sha256Hash.wrap(ByteString
//          .copyFrom(ByteArray
//              .fromHexString("9938a342238077182498b464ac0292229938a342238077182498b464ac029222"))),
//      1234,
//      ByteString.copyFrom("1234567".getBytes()));
  private static Manager dbManager;
  private static TronApplicationContext context;
  private static String dbPath = "output_transactioncapsule_test";
  private static String OWNER_ADDRESS;
  private static String TO_ADDRESS;
  private static String OWNER_ACCOUNT_NOT_Exist;
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
    Args.setParam(new String[]{"-d", dbPath},
        Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    dbManager = context.getBean(Manager.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
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

  /**
   * create temp Capsule test need.
   */
  @Before
  public void createAccountCapsule() {
    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            StringUtil.hexString2ByteString(OWNER_ADDRESS),
            AccountType.Normal,
            0);
    dbManager.getAccountStore().put(ownerCapsule.createDbKey(), ownerCapsule);
  }


  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void getPermissionName() {
    for (ContractType type : ContractType.values()) {
      if (type == ContractType.UNRECOGNIZED) {
        continue;
      }
      Transaction.Contract contract = Contract.newBuilder().setType(type).build();
      String name = TransactionCapsule.getPermissionName(contract);
      Assert.assertTrue(name.equals("active") || name.equals("owner"));
      String name1;
      if (type == ContractType.PermissionAddKeyContract
          || type == ContractType.PermissionUpdateKeyContract
          || type == ContractType.PermissionDeleteKeyContract
          || type == ContractType.AccountPermissionUpdateContract) {
        name1 = "owner";
      } else {
        name1 = "active";
      }
      Assert.assertEquals(name, name1);
    }
  }

  @Test
  public void getDefaultPermission() {
    String[] names = {"active", "owner", "other"};
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
    for (String name : names) {
      Permission permission = TransactionCapsule
          .getDefaultPermission(address, name);
      Assert.assertEquals(permission.getName(), name);
      Assert.assertEquals(permission.getThreshold(), 1);
      Assert.assertEquals(permission.getParent(), "");
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

  public List<Permission> buildPermissions(){
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
    byte[] to = ByteArray.fromHexString(TO_ADDRESS);
    //Owner Account is not exist.
    byte[] owner_not_exist = ByteArray.fromHexString(OWNER_ACCOUNT_NOT_Exist);
    TransferContract transferContract = createTransferContract(to, owner_not_exist, 1);
    Contract contract = Contract.newBuilder()
        .setType(ContractType.TransferContract).setParameter(
            Any.pack(transferContract)).build();
    try {
      Permission permission = TransactionCapsule
          .getPermission(dbManager.getAccountStore(), contract);
      Assert.assertFalse(true);
    } catch (PermissionException e) {
      Assert.assertEquals(e.getMessage(), "Account is not exist!");
    }
    //Default "active" permission
    byte[] owner = ByteArray.fromHexString(OWNER_ADDRESS);
    transferContract = createTransferContract(to, owner, 1);
    Contract contract_active = Contract.newBuilder()
        .setType(ContractType.TransferContract).setParameter(
            Any.pack(transferContract)).build();
    try {
      Permission permission = TransactionCapsule
          .getPermission(dbManager.getAccountStore(), contract_active);
      Permission permission1 = TransactionCapsule
          .getDefaultPermission(ByteString.copyFrom(owner), "active");
      Assert.assertEquals(permission, permission1);
    } catch (PermissionException e) {
      Assert.assertFalse(true);
    }
    //Default "owner" permission
    PermissionAddKeyContract permissionAddKeyContract =
        createPermissionAddKeyContract(owner, "test", to, 1);
    Contract contract_owner = Contract.newBuilder()
        .setType(ContractType.PermissionAddKeyContract).setParameter(
            Any.pack(permissionAddKeyContract)).build();
    try {
      Permission permission = TransactionCapsule
          .getPermission(dbManager.getAccountStore(), contract_owner);
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

    try {
      Permission permission = TransactionCapsule
          .getPermission(dbManager.getAccountStore(), contract_owner);
      Assert.assertEquals(permission, permission1);
    } catch (PermissionException e) {
      Assert.assertFalse(true);
    }

    try {
      Permission permission = TransactionCapsule
          .getPermission(dbManager.getAccountStore(), contract_active);
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
    Assert.assertEquals(1, TransactionCapsule.getWeight(permission1, ByteArray.fromHexString(KEY_ADDRESS_11)));
    Assert.assertEquals(1, TransactionCapsule.getWeight(permission1, ByteArray.fromHexString(KEY_ADDRESS_12)));
    Assert.assertEquals(1, TransactionCapsule.getWeight(permission1, ByteArray.fromHexString(KEY_ADDRESS_13)));
    Assert.assertEquals(0, TransactionCapsule.getWeight(permission1, ByteArray.fromHexString(KEY_ADDRESS_21)));
    Assert.assertEquals(0, TransactionCapsule.getWeight(permission1, ByteArray.fromHexString(KEY_ADDRESS_22)));
    Assert.assertEquals(0, TransactionCapsule.getWeight(permission1, ByteArray.fromHexString(KEY_ADDRESS_23)));
    Assert.assertEquals(0, TransactionCapsule.getWeight(permission1, ByteArray.fromHexString(KEY_ADDRESS_31)));
    Assert.assertEquals(0, TransactionCapsule.getWeight(permission1, ByteArray.fromHexString(KEY_ADDRESS_32)));
    Assert.assertEquals(0, TransactionCapsule.getWeight(permission1, ByteArray.fromHexString(KEY_ADDRESS_33)));

    Assert.assertEquals(0, TransactionCapsule.getWeight(permission2, ByteArray.fromHexString(KEY_ADDRESS_11)));
    Assert.assertEquals(0, TransactionCapsule.getWeight(permission2, ByteArray.fromHexString(KEY_ADDRESS_12)));
    Assert.assertEquals(0, TransactionCapsule.getWeight(permission2, ByteArray.fromHexString(KEY_ADDRESS_13)));
    Assert.assertEquals(1, TransactionCapsule.getWeight(permission2, ByteArray.fromHexString(KEY_ADDRESS_21)));
    Assert.assertEquals(1, TransactionCapsule.getWeight(permission2, ByteArray.fromHexString(KEY_ADDRESS_22)));
    Assert.assertEquals(1, TransactionCapsule.getWeight(permission2, ByteArray.fromHexString(KEY_ADDRESS_23)));
    Assert.assertEquals(0, TransactionCapsule.getWeight(permission2, ByteArray.fromHexString(KEY_ADDRESS_31)));
    Assert.assertEquals(0, TransactionCapsule.getWeight(permission2, ByteArray.fromHexString(KEY_ADDRESS_32)));
    Assert.assertEquals(0, TransactionCapsule.getWeight(permission2, ByteArray.fromHexString(KEY_ADDRESS_33)));

    Assert.assertEquals(0, TransactionCapsule.getWeight(permission3, ByteArray.fromHexString(KEY_ADDRESS_11)));
    Assert.assertEquals(0, TransactionCapsule.getWeight(permission3, ByteArray.fromHexString(KEY_ADDRESS_12)));
    Assert.assertEquals(0, TransactionCapsule.getWeight(permission3, ByteArray.fromHexString(KEY_ADDRESS_13)));
    Assert.assertEquals(0, TransactionCapsule.getWeight(permission3, ByteArray.fromHexString(KEY_ADDRESS_21)));
    Assert.assertEquals(0, TransactionCapsule.getWeight(permission3, ByteArray.fromHexString(KEY_ADDRESS_22)));
    Assert.assertEquals(0, TransactionCapsule.getWeight(permission3, ByteArray.fromHexString(KEY_ADDRESS_23)));
    Assert.assertEquals(1, TransactionCapsule.getWeight(permission3, ByteArray.fromHexString(KEY_ADDRESS_31)));
    Assert.assertEquals(1, TransactionCapsule.getWeight(permission3, ByteArray.fromHexString(KEY_ADDRESS_32)));
    Assert.assertEquals(1, TransactionCapsule.getWeight(permission3, ByteArray.fromHexString(KEY_ADDRESS_33)));
  }

}