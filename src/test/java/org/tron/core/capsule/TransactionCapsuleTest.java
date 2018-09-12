package org.tron.core.capsule;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
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

  public void updatePermission(List<Permission> permissions, byte[] address){
    Account account = dbManager.getAccountStore().get(address).getInstance();
    Account.Builder builder = account.toBuilder();
    for(Permission permission:permissions){
      builder.addPermissions(permission);
    }
    dbManager.getAccountStore().put(address, new AccountCapsule(builder.build()));
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
    contract = Contract.newBuilder()
        .setType(ContractType.TransferContract).setParameter(
            Any.pack(transferContract)).build();
    try {
      Permission permission = TransactionCapsule
          .getPermission(dbManager.getAccountStore(), contract);
      Permission permission1 = TransactionCapsule
          .getDefaultPermission(ByteString.copyFrom(owner), "active");
      Assert.assertEquals(permission, permission1);
    } catch (PermissionException e) {
      Assert.assertFalse(true);
    }
    //Default "owner" permission
    PermissionAddKeyContract permissionAddKeyContract =
        createPermissionAddKeyContract(owner, "test", to, 1);
    contract = Contract.newBuilder()
        .setType(ContractType.PermissionAddKeyContract).setParameter(
            Any.pack(permissionAddKeyContract)).build();
    try {
      Permission permission = TransactionCapsule
          .getPermission(dbManager.getAccountStore(), contract);
      Permission permission1 = TransactionCapsule
          .getDefaultPermission(ByteString.copyFrom(owner), "owner");
      Assert.assertEquals(permission, permission1);
    } catch (PermissionException e) {
      Assert.assertFalse(true);
    }


  }

}