package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Date;

import static junit.framework.TestCase.fail;

@Slf4j
public class UpdateAssetActuatorTest {
    private static TronApplicationContext context;
    private static Manager dbManager;
    private static final String dbPath = "output_updateAsset_test";
    private static final String OWNER_ADDRESS;
    private static final String OWNER_ADDRESS_ACCOUNT_NAME = "test_account";
    private static final String SECOND_ACCOUNT_ADDRESS;
    private static final String OWNER_ADDRESS_NOTEXIST;
    private static final String OWNER_ADDRESS_INVALID = "aaaa";
    private static final String NAME = "trx-my";
    private static final long TOTAL_SUPPLY = 10000L;
    private static final String DESCRIPTION = "myCoin";
    private static final String URL = "tron-my.com";

    static {
        Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
        context = new TronApplicationContext(DefaultConfig.class);
        OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
        OWNER_ADDRESS_NOTEXIST = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
        SECOND_ACCOUNT_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d427122222";
    }

    /**
     * Init data.
     */
    @BeforeClass
    public static void init() {
        dbManager = context.getBean(Manager.class);
    }

    /**
     * create temp Capsule test need.
     */
    @Before
    public void createCapsule() {
        // address in accountStore and the owner of contract
        AccountCapsule accountCapsule =
                new AccountCapsule(
                        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
                        ByteString.copyFromUtf8(OWNER_ADDRESS_ACCOUNT_NAME),
                        Protocol.AccountType.Normal);

        // add asset issue
        AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(getAssetIssueContract());
        dbManager.getAssetIssueStore().put(assetIssueCapsule.createDbKey(), assetIssueCapsule);

        accountCapsule.setAssetIssuedName(assetIssueCapsule.createDbKey());
        accountCapsule.addAsset(assetIssueCapsule.createDbKey(), TOTAL_SUPPLY);
        dbManager.getAccountStore().put(ByteArray.fromHexString(OWNER_ADDRESS), accountCapsule);

        // address in accountStore not the owner of contract
        AccountCapsule secondAccount =
                new AccountCapsule(
                        ByteString.copyFrom(ByteArray.fromHexString(SECOND_ACCOUNT_ADDRESS)),
                        ByteString.copyFromUtf8(OWNER_ADDRESS_ACCOUNT_NAME),
                        Protocol.AccountType.Normal);
        dbManager.getAccountStore().put(ByteArray.fromHexString(SECOND_ACCOUNT_ADDRESS), secondAccount);

        // address does not exist in accountStore
        dbManager.getAccountStore().delete(ByteArray.fromHexString(OWNER_ADDRESS_NOTEXIST));
    }

    /**
     * Release resources.
     */
    @AfterClass
    public static void destroy() {
        Args.clearParam();
        if (FileUtil.deleteDir(new File(dbPath))) {
            logger.info("Release resources successful.");
        } else {
            logger.info("Release resources failure.");
        }
        context.destroy();
    }

    private Any getContract(String accountAddress, String description,
                            String url, long newLimit, long newPublicLimit ) {
        return Any.pack(
                Contract.UpdateAssetContract.newBuilder()
                        .setOwnerAddress(StringUtil.hexString2ByteString(accountAddress))
                        .setDescription(ByteString.copyFromUtf8(description))
                        .setUrl(ByteString.copyFromUtf8(url))
                        .setNewLimit(newLimit)
                        .setNewPublicLimit(newPublicLimit).build());
    }

    private Contract.AssetIssueContract getAssetIssueContract() {
        long nowTime = new Date().getTime();
        return Contract.AssetIssueContract.newBuilder()
                        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
                        .setName(ByteString.copyFromUtf8(NAME))
                        .setTotalSupply(TOTAL_SUPPLY)
                        .setTrxNum(100)
                        .setNum(10)
                        .setStartTime(nowTime)
                        .setEndTime(nowTime + 24 * 3600 * 1000)
                        .setOrder(0)
                        .setDescription(ByteString.copyFromUtf8("assetTest"))
                        .setUrl(ByteString.copyFromUtf8("tron.test.com"))
                        .build();
    }

    @Test
    public void successUpdateAsset() {
        TransactionResultCapsule ret = new TransactionResultCapsule();
        UpdateAssetActuator actuator;
        actuator = new UpdateAssetActuator(
                getContract(OWNER_ADDRESS, DESCRIPTION, URL, 500L, 8000L), dbManager);
        try {
            actuator.validate();
            actuator.execute(ret);
            Assert.assertEquals(ret.getInstance().getRet(), Protocol.Transaction.Result.code.SUCESS);
            AssetIssueCapsule assetIssueCapsule =
                    dbManager.getAssetIssueStore().get(ByteString.copyFromUtf8(NAME).toByteArray());
            Assert.assertNotNull(assetIssueCapsule);
            Assert.assertEquals(DESCRIPTION, assetIssueCapsule.getInstance().getDescription().toStringUtf8());
            Assert.assertEquals(URL, assetIssueCapsule.getInstance().getUrl().toStringUtf8());
            Assert.assertEquals(assetIssueCapsule.getFreeAssetNetLimit(), 500L);
            Assert.assertEquals(assetIssueCapsule.getPublicFreeAssetNetLimit(), 8000L);
        } catch (ContractValidateException e) {
            Assert.assertFalse(e instanceof ContractValidateException);
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    @Test
    public void invalidAddress() {
        UpdateAssetActuator actuator = new UpdateAssetActuator(
                getContract(OWNER_ADDRESS_INVALID, DESCRIPTION, URL, 500L, 8000L), dbManager);

        TransactionResultCapsule ret = new TransactionResultCapsule();
        try {
            actuator.validate();
            actuator.execute(ret);

            fail("Invalid ownerAddress");
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("Invalid ownerAddress", e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    @Test
    public void noExistAccount() {
        UpdateAssetActuator actuator = new UpdateAssetActuator(
                getContract(OWNER_ADDRESS_NOTEXIST, DESCRIPTION, URL, 500L, 8000L), dbManager);

        TransactionResultCapsule ret = new TransactionResultCapsule();
        try {
            actuator.validate();
            actuator.execute(ret);

            fail("Account has not existed");
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("Account has not existed", e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    @Test
    public void noAsset() {
        UpdateAssetActuator actuator = new UpdateAssetActuator(
                getContract(SECOND_ACCOUNT_ADDRESS, DESCRIPTION, URL, 500L, 8000L), dbManager);

        TransactionResultCapsule ret = new TransactionResultCapsule();
        try {
            actuator.validate();
            actuator.execute(ret);

            fail("Account has not issue any asset");
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("Account has not issue any asset", e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    /*
     * empty url
     */
    @Test
    public void invalidAssetUrl() {
        String localUrl = "";
        UpdateAssetActuator actuator = new UpdateAssetActuator(
                getContract(OWNER_ADDRESS, DESCRIPTION, localUrl,500L, 8000L), dbManager);

        TransactionResultCapsule ret = new TransactionResultCapsule();
        try {
            actuator.validate();
            actuator.execute(ret);

            fail("Invalid url");
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("Invalid url", e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    /*
     * description is more than 200 character
     */
    @Test
    public void invalidAssetDescription() {
        String localDescription = "abchefghijklmnopqrstuvwxyzabchefghijklmnopqrstuvwxyzabchefghijklmnopqrstuv" +
                "wxyzabchefghijklmnopqrstuvwxyzabchefghijklmnopqrstuvwxyzabchefghijklmnopqrstuvwxyzabchefghij" +
                "klmnopqrstuvwxyzabchefghijklmnopqrstuvwxyzabchefghijklmnopqrstuvwxyz";

        UpdateAssetActuator actuator = new UpdateAssetActuator(
                getContract(OWNER_ADDRESS, localDescription, URL, 500L, 8000L), dbManager);
        TransactionResultCapsule ret = new TransactionResultCapsule();
        try {
            actuator.validate();
            actuator.execute(ret);

            fail("Invalid description");
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("Invalid description", e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    /*
     * new limit is more than 57_600_000_000
     */
    @Test
    public void invalidNewLimit() {
        long localNewLimit = 57_600_000_001L;
        UpdateAssetActuator actuator = new UpdateAssetActuator(
                getContract(OWNER_ADDRESS, DESCRIPTION, URL, localNewLimit, 8000L), dbManager);

        TransactionResultCapsule ret = new TransactionResultCapsule();
        try {
            actuator.validate();
            actuator.execute(ret);

            fail("Invalid FreeAssetNetLimit");
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("Invalid FreeAssetNetLimit", e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    @Test
    public void invalidNewPublicLimit() {
        long localNewPublicLimit = -1L;
        UpdateAssetActuator actuator = new UpdateAssetActuator(
                getContract(OWNER_ADDRESS, DESCRIPTION, URL, 500L, localNewPublicLimit), dbManager);

        TransactionResultCapsule ret = new TransactionResultCapsule();
        try {
            actuator.validate();
            actuator.execute(ret);

            fail("Invalid PublicFreeAssetNetLimit");
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals("Invalid PublicFreeAssetNetLimit", e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

}
