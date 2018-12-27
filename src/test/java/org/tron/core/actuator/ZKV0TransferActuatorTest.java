package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.Parameter;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;

import java.io.File;
import java.util.Date;
import java.util.HashMap;

import static junit.framework.TestCase.fail;

@Slf4j
public class ZKV0TransferActuatorTest {

    private static Manager dbManager;
    private static final String dbPath = "output_zksnarktransaction_test";
    private static TronApplicationContext context;
    private static final String OWNER_ADDRESS;
    private static final String TO_ADDRESS;
    private static final String INVALID_OWNER_ADDRESS = "aaaa";;
    private static final long OWNER_BALANCE = 20_000_000L;
    private static final long TO_BALANCE = 100001;

    static {
        Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
        context = new TronApplicationContext(DefaultConfig.class);
        OWNER_ADDRESS = Wallet.getAddressPreFixString() + "5a523b449890854c8fc460ab602df9f31fe4293f";
        TO_ADDRESS = Wallet.getAddressPreFixString() + "a7d8a35b260395c14aa456297662092ba3b76fc0";
    }

    /**
     * Init data.
     */
    @BeforeClass
    public static void init() {
        dbManager = context.getBean(Manager.class);
    }

    /**
     * Release resources.
     */
    @AfterClass
    public static void destroy() {
        Args.clearParam();
        context.destroy();
        if (FileUtil.deleteDir(new File(dbPath))) {
            logger.info("Release resources successful.");
        } else {
            logger.info("Release resources failure.");
        }
    }

    /**
     * create temp Capsule test need.
     */
    @Before
    public void initTest() {
        AccountCapsule ownerCapsule =
                new AccountCapsule(
                        ByteString.copyFromUtf8("owner"),
                        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
                        Protocol.AccountType.Normal,
                        OWNER_BALANCE);
        AccountCapsule toAccountCapsule =
                new AccountCapsule(
                        ByteString.copyFromUtf8("toAccount"),
                        ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
                        Protocol.AccountType.Normal,
                        TO_BALANCE);

        dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
        dbManager.getAccountStore().put(toAccountCapsule.getAddress().toByteArray(), toAccountCapsule);
        dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
        dbManager.getMerkleContainer().getBestMerkle();
    }

    private Any getRightContract() {
        Contract.zkv0proof proof = null;
        try {
            proof = Contract.zkv0proof.parseFrom( ByteArray.fromHexString("0a440a2026c78f585c0cdf83cc61a375bb09ad486f654ab227ad94451193b6e5c930188612200f3634efaf9621b21438184fd5f9804c6c35594ee46c7a9f1714386de353e8f412440a200b57c26beb2ee0d305864dcdb41fb3c952378b673759eb8524c466b0a13342901220061f9009dbf8263ea530b34d7d8458f84bd8f1187f33184276ab8181b5709cd51a88010a200bb2c80c604668ab6d67c9850901db2dddcef10cfcab5c29d97104e7944dd92b1220170d71bb83633ccf2a501536672ebae1922830fc48478ad3a8312be0ecb7ec9b1a20119d61ee1dd6c6ad7a93d3cd34dc0c37dca9f1bca926e83c11241d54130bdec02220099956c62ad02f8fc1d8d8f74f82a45e58f309284aa501040b7c722b2ebe1a9c22440a201fd6e095d3e1da81e284e88b4677f82a3696351462f1048193c1f8a805aecad4122010318bd32f82a81b46100f9ff80d013ae59409448424902caee62a470e5fd6752a440a201ea3dd70be5de39b05fe1fcac2877bf4cf821c831591350778c4efc6a7b589ad12200cc0e9204446ee98f5a184fbbad01483f01bb4283d6844a0ba18cb4813abae5932440a20154a8464cc230a8bdfab3f671df42674d1b8024845cfb882519d47efe59220e912201d1c5a9c28b27095db956e89418c9884505c3fe193111aa30e33ba97077742d93a440a2016dd88b1b834e8d66e53f50de11ed246cdae54f6dbe18d8612c5b44045db5d2b122010dac4e3f9cd8360dd119e5f623d74e287ae6823d4bf01ed6465aa2e49f924b842440a2008263bbd105097d0218c0c7468ceef5a1db4dc606680be203ccf12acb1c7d4a512201f6bf721402eeba55bd6a09c31e80d97cdce9118dd9636837160e5dcc4664f60"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return Any.pack(
                Contract.ZksnarkV0TransferContract.newBuilder()
                        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
                        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
                        .setVFromPub(11_000_000L)
                        .setVToPub(5_000_000L)
                        .setRt(ByteString.copyFrom(ByteArray.fromHexString("83455f310e4a075e5625e998ec5954623992839264727ead195f8a8132136134")))
                        .setNf1(ByteString.copyFrom(ByteArray.fromHexString("6d188c56fd3853e09afdbc2562733828797be219a06fe2beff55f6ff797de07f")))
                        .setNf2(ByteString.copyFrom(ByteArray.fromHexString("ff6d72e75a66ce1b870d0a902accec5569476b4db5b87096209505ca87015729")))
                        .setCm1(ByteString.copyFrom(ByteArray.fromHexString("b67abd8002244c7ccffb1f9badfac4bd69dbf7d698667d5fbe53c5739c722d45")))
                        .setCm2(ByteString.copyFrom(ByteArray.fromHexString("316ff656685edb1c1b5b5590393b8a8538fe264537e37a3eecfa320d8c2a4faf")))
                        .setPksig(ByteString.copyFrom(ByteArray.fromHexString("bc14319e30af8cbc7e36f92bd9b44dcbd579f40fee24ff8a71e9d844e5499720")))
                        .setRandomSeed(ByteString.copyFrom(ByteArray.fromHexString("5595aa2c186f8ddcdda50d6a65cccc314e26e290f98d34ae18895df15f4d91ca")))
                        .setEpk(ByteString.copyFrom(ByteArray.fromHexString("63c7dd3dbb705e7612d2b49dc700a0ee8adc8d048cccd6c5917b9dadf692e35f")))
                        .setFee(1_000_000L)
                        .setH1(ByteString.copyFrom(ByteArray.fromHexString("5f2fec36e2f5d52d8b36b06bfd0fe1ac3e384fa18fb8f3c749255fa3f606ca4b")))
                        .setH2(ByteString.copyFrom(ByteArray.fromHexString("f22262a6dda3cc82484e95481b8a15c3bfc76f1acf207e2ec4bd745f7f5c8ef3")))
                        .setC1(ByteString.copyFrom(ByteArray.fromHexString("ec1873d483c9463c87d036a265272467f20e8976a111459a275cf68bc0f74396d2e9549631cdc4c7207fb3e4c4beaf2890e238ee198a91bd6717b594f67c2f77e21d06ef9b3421b0a7352ca86edf0aab64d08addb38b20588f8e7b86901acae3dab58f1b47ce66b860491c354e11e30485075296190630d4689388b160ddaf60e95b1b297a3f89271a06872b94bcd0750f992f01f4e6019a971729695bc07f8e7e4ea35d2bdd861ea31cbee14024794b2e248bdbbab46340aacb6606a17a5d00bfdce8147f9a388e8f2dbb428a2556f4be4f437de0759bf7e5a2ebdf9df0c21f8057a857096ef04d9957849014e08fa454263b497e28353099b871f8e207e0138d97139dc0b75c29b9a6b0c1b6ae2f9eba097d8164046e3173fab590f476345b6febc1b2005eb06def17651373ce039386a99d95a652fa466c506eea1db96b398dbc08b8cda4969416b3d62d4b803ff6ed3ead1259df32dc36eb5e368a7ffd7cb5e5a0e3c700cb39f7e86173216e0a038394ae2a3dbb8d11ea5e17dabd028be0794160c5b2b606b631eea4fb66d362cc613e5c4370157a6f86ec9f7333d9df6861603ad581b32526412aad3746de73f86378a5b3a4021108b996fc270cec1bdc04ba67518c2d8002395b14314b303e15170998d964ba107f8f52036bdc5b90326949f9d344cb9914e90b2740690d04f31475127437815218d233891c98ae89f0f517c3d74b03a2668ff57fc050dd32badfd2139ab1993030f3e7f8da584206b1644d923daf8ed57a46be2e5e44fb30e47016a79ce29022229659bbec323f482638830e5881a8e2bc876ffa971c7aa4ee9987681f07ab10763c")))
                        .setC2(ByteString.copyFrom(ByteArray.fromHexString("3962fbd00c86f3d43c8441f16187a4daf9de53a65e347236c80af57f54ba1449583cda84b90b7a3550b27366e14076b16e3a343af9bb40a9b656a833d970ce2811ca0f181f0be7850cfd42671d206c286cfec6418bff49279fcbc9fff3a94885f3438fe5903a7ddbf154c22edf9a62f9c61990f6722af10c0e69efba3009528fec1b0ceb61330134f47690aecd464f4fc0b2241e342c977888e1601f6a6d03c1e658125b3d3a6b175da64129f5832ba726c352d7f6ae74eea44b244d8d5e89e81d26bd2e02dd9e64f7aa53ce26b22598cb7f122bfe80e9390af0cf7fdf1441df9ffc9092d0fba37c6aa805cb8d07645f99a11c60d7f133f1dab995496cbe2137b3854c8769efcbbd394f65bc74ad6e0bf81c6b10ad53d3eac6c57c6ff1b808ee521b93304014cb9165625f678bdec45e60d84f9d0a88d511b027becf49ec72bb1eb53d2cf1d9e19cdcf8680be6fbc23985f52faf432723533d64c36aa4255371e8ab278d60cc734d246eb548d6d8903ed9a33f26005df10048061eb9c06c96394eda71a77f62dc7b481f5a4d5a6fd3e7c4330aa22e9a6468bf4df6f849408d22cc27e25abe6eb31a8b1c157e4fb3d50c221184bbbbb9ab767924e7988fa0c88b75adc227f98d60df88fcf6a57a3273ea28478e0853a61534fcb1983f52ec0d4fbb3fbf24d8fa8a2230d0a1a325eec0d36d0faa2613650faec4994412bdc81f36095b39a43f70ca05b32691a5042f896171274cb5cad917d95267f3ae4c738c7efe95ff18501f61e92609bcfcb5caa456ba2cf20c0e167d40367867b5dcad4829b9b2eb659ba92c3b2a1e4996e7e276149acbea587c1efa0d64")))
                        .setProof(proof)
                        .build());
    }

    private Any getInvalidAddress(final String ownerAddress, final String toAddress) {
        Contract.zkv0proof proof = null;
        try {
            proof = Contract.zkv0proof.parseFrom( ByteArray.fromHexString("0a440a2026c78f585c0cdf83cc61a375bb09ad486f654ab227ad94451193b6e5c930188612200f3634efaf9621b21438184fd5f9804c6c35594ee46c7a9f1714386de353e8f412440a200b57c26beb2ee0d305864dcdb41fb3c952378b673759eb8524c466b0a13342901220061f9009dbf8263ea530b34d7d8458f84bd8f1187f33184276ab8181b5709cd51a88010a200bb2c80c604668ab6d67c9850901db2dddcef10cfcab5c29d97104e7944dd92b1220170d71bb83633ccf2a501536672ebae1922830fc48478ad3a8312be0ecb7ec9b1a20119d61ee1dd6c6ad7a93d3cd34dc0c37dca9f1bca926e83c11241d54130bdec02220099956c62ad02f8fc1d8d8f74f82a45e58f309284aa501040b7c722b2ebe1a9c22440a201fd6e095d3e1da81e284e88b4677f82a3696351462f1048193c1f8a805aecad4122010318bd32f82a81b46100f9ff80d013ae59409448424902caee62a470e5fd6752a440a201ea3dd70be5de39b05fe1fcac2877bf4cf821c831591350778c4efc6a7b589ad12200cc0e9204446ee98f5a184fbbad01483f01bb4283d6844a0ba18cb4813abae5932440a20154a8464cc230a8bdfab3f671df42674d1b8024845cfb882519d47efe59220e912201d1c5a9c28b27095db956e89418c9884505c3fe193111aa30e33ba97077742d93a440a2016dd88b1b834e8d66e53f50de11ed246cdae54f6dbe18d8612c5b44045db5d2b122010dac4e3f9cd8360dd119e5f623d74e287ae6823d4bf01ed6465aa2e49f924b842440a2008263bbd105097d0218c0c7468ceef5a1db4dc606680be203ccf12acb1c7d4a512201f6bf721402eeba55bd6a09c31e80d97cdce9118dd9636837160e5dcc4664f60"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return Any.pack(
                Contract.ZksnarkV0TransferContract.newBuilder()
                        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
                        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(toAddress)))
                        .setVFromPub(11_000_000L)
                        .setVToPub(5_000_000L)
                        .setRt(ByteString.copyFrom(ByteArray.fromHexString("83455f310e4a075e5625e998ec5954623992839264727ead195f8a8132136134")))
                        .setNf1(ByteString.copyFrom(ByteArray.fromHexString("6d188c56fd3853e09afdbc2562733828797be219a06fe2beff55f6ff797de07f")))
                        .setNf2(ByteString.copyFrom(ByteArray.fromHexString("ff6d72e75a66ce1b870d0a902accec5569476b4db5b87096209505ca87015729")))
                        .setCm1(ByteString.copyFrom(ByteArray.fromHexString("b67abd8002244c7ccffb1f9badfac4bd69dbf7d698667d5fbe53c5739c722d45")))
                        .setCm2(ByteString.copyFrom(ByteArray.fromHexString("316ff656685edb1c1b5b5590393b8a8538fe264537e37a3eecfa320d8c2a4faf")))
                        .setPksig(ByteString.copyFrom(ByteArray.fromHexString("bc14319e30af8cbc7e36f92bd9b44dcbd579f40fee24ff8a71e9d844e5499720")))
                        .setRandomSeed(ByteString.copyFrom(ByteArray.fromHexString("5595aa2c186f8ddcdda50d6a65cccc314e26e290f98d34ae18895df15f4d91ca")))
                        .setEpk(ByteString.copyFrom(ByteArray.fromHexString("63c7dd3dbb705e7612d2b49dc700a0ee8adc8d048cccd6c5917b9dadf692e35f")))
                        .setFee(1_000_000L)
                        .setH1(ByteString.copyFrom(ByteArray.fromHexString("5f2fec36e2f5d52d8b36b06bfd0fe1ac3e384fa18fb8f3c749255fa3f606ca4b")))
                        .setH2(ByteString.copyFrom(ByteArray.fromHexString("f22262a6dda3cc82484e95481b8a15c3bfc76f1acf207e2ec4bd745f7f5c8ef3")))
                        .setC1(ByteString.copyFrom(ByteArray.fromHexString("ec1873d483c9463c87d036a265272467f20e8976a111459a275cf68bc0f74396d2e9549631cdc4c7207fb3e4c4beaf2890e238ee198a91bd6717b594f67c2f77e21d06ef9b3421b0a7352ca86edf0aab64d08addb38b20588f8e7b86901acae3dab58f1b47ce66b860491c354e11e30485075296190630d4689388b160ddaf60e95b1b297a3f89271a06872b94bcd0750f992f01f4e6019a971729695bc07f8e7e4ea35d2bdd861ea31cbee14024794b2e248bdbbab46340aacb6606a17a5d00bfdce8147f9a388e8f2dbb428a2556f4be4f437de0759bf7e5a2ebdf9df0c21f8057a857096ef04d9957849014e08fa454263b497e28353099b871f8e207e0138d97139dc0b75c29b9a6b0c1b6ae2f9eba097d8164046e3173fab590f476345b6febc1b2005eb06def17651373ce039386a99d95a652fa466c506eea1db96b398dbc08b8cda4969416b3d62d4b803ff6ed3ead1259df32dc36eb5e368a7ffd7cb5e5a0e3c700cb39f7e86173216e0a038394ae2a3dbb8d11ea5e17dabd028be0794160c5b2b606b631eea4fb66d362cc613e5c4370157a6f86ec9f7333d9df6861603ad581b32526412aad3746de73f86378a5b3a4021108b996fc270cec1bdc04ba67518c2d8002395b14314b303e15170998d964ba107f8f52036bdc5b90326949f9d344cb9914e90b2740690d04f31475127437815218d233891c98ae89f0f517c3d74b03a2668ff57fc050dd32badfd2139ab1993030f3e7f8da584206b1644d923daf8ed57a46be2e5e44fb30e47016a79ce29022229659bbec323f482638830e5881a8e2bc876ffa971c7aa4ee9987681f07ab10763c")))
                        .setC2(ByteString.copyFrom(ByteArray.fromHexString("3962fbd00c86f3d43c8441f16187a4daf9de53a65e347236c80af57f54ba1449583cda84b90b7a3550b27366e14076b16e3a343af9bb40a9b656a833d970ce2811ca0f181f0be7850cfd42671d206c286cfec6418bff49279fcbc9fff3a94885f3438fe5903a7ddbf154c22edf9a62f9c61990f6722af10c0e69efba3009528fec1b0ceb61330134f47690aecd464f4fc0b2241e342c977888e1601f6a6d03c1e658125b3d3a6b175da64129f5832ba726c352d7f6ae74eea44b244d8d5e89e81d26bd2e02dd9e64f7aa53ce26b22598cb7f122bfe80e9390af0cf7fdf1441df9ffc9092d0fba37c6aa805cb8d07645f99a11c60d7f133f1dab995496cbe2137b3854c8769efcbbd394f65bc74ad6e0bf81c6b10ad53d3eac6c57c6ff1b808ee521b93304014cb9165625f678bdec45e60d84f9d0a88d511b027becf49ec72bb1eb53d2cf1d9e19cdcf8680be6fbc23985f52faf432723533d64c36aa4255371e8ab278d60cc734d246eb548d6d8903ed9a33f26005df10048061eb9c06c96394eda71a77f62dc7b481f5a4d5a6fd3e7c4330aa22e9a6468bf4df6f849408d22cc27e25abe6eb31a8b1c157e4fb3d50c221184bbbbb9ab767924e7988fa0c88b75adc227f98d60df88fcf6a57a3273ea28478e0853a61534fcb1983f52ec0d4fbb3fbf24d8fa8a2230d0a1a325eec0d36d0faa2613650faec4994412bdc81f36095b39a43f70ca05b32691a5042f896171274cb5cad917d95267f3ae4c738c7efe95ff18501f61e92609bcfcb5caa456ba2cf20c0e167d40367867b5dcad4829b9b2eb659ba92c3b2a1e4996e7e276149acbea587c1efa0d64")))
                        .setProof(proof)
                        .build());
    }

    /**
     *  failure if vp file change
     */
    @Test
    public void successZksnarkTransaction() {
        ZkV0TransferActuator actuator = new ZkV0TransferActuator(getRightContract(), dbManager);
        TransactionResultCapsule ret = new TransactionResultCapsule();
        try {
            actuator.validate();
            actuator.execute(ret);
            Assert.assertEquals(ret.getInstance().getRet(), Protocol.Transaction.Result.code.SUCESS);
            AccountCapsule owner =
                    dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
            AccountCapsule toAccount =
                    dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

            Assert.assertEquals(owner.getBalance(),
                    OWNER_BALANCE - 11_000_000L);
            Assert.assertEquals(toAccount.getBalance(), TO_BALANCE + 5_000_000L);
            Assert.assertTrue(true);
        } catch (ContractValidateException e) {
            Assert.assertFalse(e instanceof ContractValidateException);
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    /**
     * Invalid contract type
     */
    @Test
    public void invalidContractType() {
        Any contract = Any.pack(
                Contract.TransferContract.newBuilder()
                        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
                        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
                        .setAmount(100)
                        .build());
        ZkV0TransferActuator actuator = new ZkV0TransferActuator(contract, dbManager);
        TransactionResultCapsule ret = new TransactionResultCapsule();
        try {
            actuator.validate();
            actuator.execute(ret);
            fail("Invalid contract type");
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals(
                    "contract type error,expected type [ZksnarkV0TransferContract],real type[class com.google.protobuf.Any]", e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    /**
     * zksnark transaction not active
     */
    @Test
    public void zksnarkTransactionNotActive() {
        dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(0);

        ZkV0TransferActuator actuator = new ZkV0TransferActuator(getRightContract(), dbManager);
        TransactionResultCapsule ret = new TransactionResultCapsule();
        try {
            actuator.validate();
            actuator.execute(ret);
            fail("Invalid contract type");
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals(
                    "Not support ZKSnarkTransaction, need to be opened by the committee", e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    /**
     * Invalid ownerAddress
     */
    @Test
    public void invalidOwnerAddress() {
        ZkV0TransferActuator actuator =
                new ZkV0TransferActuator(getInvalidAddress(INVALID_OWNER_ADDRESS, TO_ADDRESS), dbManager);
        TransactionResultCapsule ret = new TransactionResultCapsule();
        try {
            actuator.validate();
            actuator.execute(ret);
            fail("Invalid contract type");
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals(
                    "Invalid ownerAddress", e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    /**
     * Invalid toAddress
     */
    @Test
    public void invalidToAddress() {
        ZkV0TransferActuator actuator =
                new ZkV0TransferActuator(getInvalidAddress(OWNER_ADDRESS, INVALID_OWNER_ADDRESS), dbManager);
        TransactionResultCapsule ret = new TransactionResultCapsule();
        try {
            actuator.validate();
            actuator.execute(ret);
            fail("fail");
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals(
                    "Invalid toAddress", e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    /**
     * owner address same to to_address
     */
    @Test
    public void ownerAddressSameToAddress() {
        ZkV0TransferActuator actuator =
                new ZkV0TransferActuator(getInvalidAddress(OWNER_ADDRESS, OWNER_ADDRESS), dbManager);
        TransactionResultCapsule ret = new TransactionResultCapsule();
        try {
            actuator.validate();
            actuator.execute(ret);
            fail("fail");
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals(
                    "Cannot transfer trx to yourself.", e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    /**
     * no OwnerAccount
     */
    @Test
    public void noOwnerAccount() {
        dbManager.getAccountStore().delete(ByteArray.fromHexString(OWNER_ADDRESS));

        ZkV0TransferActuator actuator = new ZkV0TransferActuator(getRightContract(), dbManager);
        TransactionResultCapsule ret = new TransactionResultCapsule();
        try {
            actuator.validate();
            actuator.execute(ret);
            fail("no owner account");
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals(
                    "Validate ZkV0TransferActuator error, no OwnerAccount.", e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    /**
     * no ToAccount
     */
    @Test
    public void noToAccount() {
        dbManager.getAccountStore().delete(ByteArray.fromHexString(TO_ADDRESS));

        ZkV0TransferActuator actuator = new ZkV0TransferActuator(getRightContract(), dbManager);
        TransactionResultCapsule ret = new TransactionResultCapsule();
        try {
            actuator.validate();
            actuator.execute(ret);
            fail("fail");
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals(
                    "Validate ZkV0TransferActuator error, no toAccount.", e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    /**
     * balance is not enough
     */
    @Test
    public void notEnoughBalance() {
        try {
            dbManager.adjustBalance(ByteArray.fromHexString(OWNER_ADDRESS), -20_000_000L);
        } catch (Exception e) {
        }

        ZkV0TransferActuator actuator = new ZkV0TransferActuator(getRightContract(), dbManager);
        TransactionResultCapsule ret = new TransactionResultCapsule();
        try {
            actuator.validate();
            actuator.execute(ret);
            fail("no owner account");
        } catch (ContractValidateException e) {
            Assert.assertTrue(e instanceof ContractValidateException);
            Assert.assertEquals(
                    "Validate ZkV0TransferActuator error, balance is not sufficient.", e.getMessage());
        } catch (ContractExeException e) {
            Assert.assertFalse(e instanceof ContractExeException);
        }
    }

    /**
     * Invalid param
     */
    @Test
    public void invalidParam() {
        Contract.ZksnarkV0TransferContract.Builder builder = Contract.ZksnarkV0TransferContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
                .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
                .setVFromPub(11_000_000L)
                .setVToPub(5_000_000L)
                .setRt(ByteString.copyFrom(ByteArray.fromHexString("83455f310e4a075e5625e998ec5954623992839264727ead195f8a8132136134")))
                .setNf1(ByteString.copyFrom(ByteArray.fromHexString("6d188c56fd3853e09afdbc2562733828797be219a06fe2beff55f6ff797de07f")))
                .setNf2(ByteString.copyFrom(ByteArray.fromHexString("ff6d72e75a66ce1b870d0a902accec5569476b4db5b87096209505ca87015729")))
                .setCm1(ByteString.copyFrom(ByteArray.fromHexString("b67abd8002244c7ccffb1f9badfac4bd69dbf7d698667d5fbe53c5739c722d45")))
                .setCm2(ByteString.copyFrom(ByteArray.fromHexString("316ff656685edb1c1b5b5590393b8a8538fe264537e37a3eecfa320d8c2a4faf")))
                .setPksig(ByteString.copyFrom(ByteArray.fromHexString("bc14319e30af8cbc7e36f92bd9b44dcbd579f40fee24ff8a71e9d844e5499720")))
                .setRandomSeed(ByteString.copyFrom(ByteArray.fromHexString("5595aa2c186f8ddcdda50d6a65cccc314e26e290f98d34ae18895df15f4d91ca")))
                .setEpk(ByteString.copyFrom(ByteArray.fromHexString("63c7dd3dbb705e7612d2b49dc700a0ee8adc8d048cccd6c5917b9dadf692e35f")))
                .setFee(1_000_000L)
                .setH1(ByteString.copyFrom(ByteArray.fromHexString("5f2fec36e2f5d52d8b36b06bfd0fe1ac3e384fa18fb8f3c749255fa3f606ca4b")))
                .setH2(ByteString.copyFrom(ByteArray.fromHexString("f22262a6dda3cc82484e95481b8a15c3bfc76f1acf207e2ec4bd745f7f5c8ef3")))
                .setC1(ByteString.copyFrom(ByteArray.fromHexString("ec1873d483c9463c87d036a265272467f20e8976a111459a275cf68bc0f74396d2e9549631cdc4c7207fb3e4c4beaf2890e238ee198a91bd6717b594f67c2f77e21d06ef9b3421b0a7352ca86edf0aab64d08addb38b20588f8e7b86901acae3dab58f1b47ce66b860491c354e11e30485075296190630d4689388b160ddaf60e95b1b297a3f89271a06872b94bcd0750f992f01f4e6019a971729695bc07f8e7e4ea35d2bdd861ea31cbee14024794b2e248bdbbab46340aacb6606a17a5d00bfdce8147f9a388e8f2dbb428a2556f4be4f437de0759bf7e5a2ebdf9df0c21f8057a857096ef04d9957849014e08fa454263b497e28353099b871f8e207e0138d97139dc0b75c29b9a6b0c1b6ae2f9eba097d8164046e3173fab590f476345b6febc1b2005eb06def17651373ce039386a99d95a652fa466c506eea1db96b398dbc08b8cda4969416b3d62d4b803ff6ed3ead1259df32dc36eb5e368a7ffd7cb5e5a0e3c700cb39f7e86173216e0a038394ae2a3dbb8d11ea5e17dabd028be0794160c5b2b606b631eea4fb66d362cc613e5c4370157a6f86ec9f7333d9df6861603ad581b32526412aad3746de73f86378a5b3a4021108b996fc270cec1bdc04ba67518c2d8002395b14314b303e15170998d964ba107f8f52036bdc5b90326949f9d344cb9914e90b2740690d04f31475127437815218d233891c98ae89f0f517c3d74b03a2668ff57fc050dd32badfd2139ab1993030f3e7f8da584206b1644d923daf8ed57a46be2e5e44fb30e47016a79ce29022229659bbec323f482638830e5881a8e2bc876ffa971c7aa4ee9987681f07ab10763c")))
                .setC2(ByteString.copyFrom(ByteArray.fromHexString("3962fbd00c86f3d43c8441f16187a4daf9de53a65e347236c80af57f54ba1449583cda84b90b7a3550b27366e14076b16e3a343af9bb40a9b656a833d970ce2811ca0f181f0be7850cfd42671d206c286cfec6418bff49279fcbc9fff3a94885f3438fe5903a7ddbf154c22edf9a62f9c61990f6722af10c0e69efba3009528fec1b0ceb61330134f47690aecd464f4fc0b2241e342c977888e1601f6a6d03c1e658125b3d3a6b175da64129f5832ba726c352d7f6ae74eea44b244d8d5e89e81d26bd2e02dd9e64f7aa53ce26b22598cb7f122bfe80e9390af0cf7fdf1441df9ffc9092d0fba37c6aa805cb8d07645f99a11c60d7f133f1dab995496cbe2137b3854c8769efcbbd394f65bc74ad6e0bf81c6b10ad53d3eac6c57c6ff1b808ee521b93304014cb9165625f678bdec45e60d84f9d0a88d511b027becf49ec72bb1eb53d2cf1d9e19cdcf8680be6fbc23985f52faf432723533d64c36aa4255371e8ab278d60cc734d246eb548d6d8903ed9a33f26005df10048061eb9c06c96394eda71a77f62dc7b481f5a4d5a6fd3e7c4330aa22e9a6468bf4df6f849408d22cc27e25abe6eb31a8b1c157e4fb3d50c221184bbbbb9ab767924e7988fa0c88b75adc227f98d60df88fcf6a57a3273ea28478e0853a61534fcb1983f52ec0d4fbb3fbf24d8fa8a2230d0a1a325eec0d36d0faa2613650faec4994412bdc81f36095b39a43f70ca05b32691a5042f896171274cb5cad917d95267f3ae4c738c7efe95ff18501f61e92609bcfcb5caa456ba2cf20c0e167d40367867b5dcad4829b9b2eb659ba92c3b2a1e4996e7e276149acbea587c1efa0d64")));

        // vFromPub < 0
        {
            builder.setVFromPub(-1L);
            ZkV0TransferActuator actuator = new ZkV0TransferActuator(Any.pack(builder.build()), dbManager);
            TransactionResultCapsule ret = new TransactionResultCapsule();
            try {
                actuator.validate();
                actuator.execute(ret);
                fail("fail");
            } catch (ContractValidateException e) {
                Assert.assertTrue(e instanceof ContractValidateException);
                Assert.assertEquals(
                        "vFromPub can not less than 0.", e.getMessage());
            } catch (ContractExeException e) {
                Assert.assertFalse(e instanceof ContractExeException);
            }
            builder.setVFromPub(11_000_000L);
        }

        // ownerAddress.isEmpty() ^ (vFromPub == 0)
        {
            builder.setVFromPub(0);
            ZkV0TransferActuator actuator = new ZkV0TransferActuator(Any.pack(builder.build()), dbManager);
            TransactionResultCapsule ret = new TransactionResultCapsule();
            try {
                actuator.validate();
                actuator.execute(ret);
                fail("fail");
            } catch (ContractValidateException e) {
                Assert.assertTrue(e instanceof ContractValidateException);
                Assert.assertEquals(
                        "OwnerAddress needs to be empty and the vFromPub is zero, or neither.", e.getMessage());
            } catch (ContractExeException e) {
                Assert.assertFalse(e instanceof ContractExeException);
            }
            builder.setVFromPub(11_000_000L);
        }

        // invalid fee
        {
            builder.setFee(100L);
            ZkV0TransferActuator actuator = new ZkV0TransferActuator(Any.pack(builder.build()), dbManager);
            TransactionResultCapsule ret = new TransactionResultCapsule();
            try {
                actuator.validate();
                actuator.execute(ret);
                fail("fail");
            } catch (ContractValidateException e) {
                Assert.assertTrue(e instanceof ContractValidateException);
                Assert.assertEquals(
                        "Contract transaction fee is inconsistent with system transaction fee", e.getMessage());
            } catch (ContractExeException e) {
                Assert.assertFalse(e instanceof ContractExeException);
            }
            builder.setFee(1_000_000L);
        }

        // vToPub < 0
        {
            builder.setVToPub(-1);
            ZkV0TransferActuator actuator = new ZkV0TransferActuator(Any.pack(builder.build()), dbManager);
            TransactionResultCapsule ret = new TransactionResultCapsule();
            try {
                actuator.validate();
                actuator.execute(ret);
                fail("fail");
            } catch (ContractValidateException e) {
                Assert.assertTrue(e instanceof ContractValidateException);
                Assert.assertEquals(
                        "vToPub can not less than 0.", e.getMessage());
            } catch (ContractExeException e) {
                Assert.assertFalse(e instanceof ContractExeException);
            }
            builder.setVToPub(5_000_000L);
        }

        // vToPub = 0
        {
            builder.setVToPub(0);
            ZkV0TransferActuator actuator = new ZkV0TransferActuator(Any.pack(builder.build()), dbManager);
            TransactionResultCapsule ret = new TransactionResultCapsule();
            try {
                actuator.validate();
                actuator.execute(ret);
                fail("fail");
            } catch (ContractValidateException e) {
                Assert.assertTrue(e instanceof ContractValidateException);
                Assert.assertEquals(
                        "ToAddress needs to be empty and the vToPub is zero, or neither.", e.getMessage());
            } catch (ContractExeException e) {
                Assert.assertFalse(e instanceof ContractExeException);
            }
            builder.setVToPub(5_000_000L);
        }

        // rt empty
        {
            builder.setRt(ByteString.copyFrom(ByteArray.fromHexString("")));
            ZkV0TransferActuator actuator = new ZkV0TransferActuator(Any.pack(builder.build()), dbManager);
            TransactionResultCapsule ret = new TransactionResultCapsule();
            try {
                actuator.validate();
                actuator.execute(ret);
                fail("fail");
            } catch (ContractValidateException e) {
                Assert.assertTrue(e instanceof ContractValidateException);
                Assert.assertEquals(
                        "Merkel root is invalid.", e.getMessage());
            } catch (ContractExeException e) {
                Assert.assertFalse(e instanceof ContractExeException);
            }
            builder.setRt(ByteString.copyFrom(ByteArray.fromHexString("83455f310e4a075e5625e998ec5954623992839264727ead195f8a8132136134")));
        }

        // rt is not exist
        {
            builder.setRt(ByteString.copyFrom(ByteArray.fromHexString(
                    "123456310e4a075e5625e998ec5954623992839264727ead195f8a8132136134")));
            ZkV0TransferActuator actuator = new ZkV0TransferActuator(Any.pack(builder.build()), dbManager);
            TransactionResultCapsule ret = new TransactionResultCapsule();
            try {
                actuator.validate();
                actuator.execute(ret);
                fail("fail");
            } catch (ContractValidateException e) {
                Assert.assertTrue(e instanceof ContractValidateException);
                Assert.assertEquals(
                        "Rt is invalid.", e.getMessage());
            } catch (ContractExeException e) {
                Assert.assertFalse(e instanceof ContractExeException);
            }
            builder.setRt(ByteString.copyFrom(ByteArray.fromHexString("83455f310e4a075e5625e998ec5954623992839264727ead195f8a8132136134")));
        }

        // invalid nf1
        {
            builder.setNf1(ByteString.copyFrom(ByteArray.fromHexString(
                    "188c56fd3853e09afdbc2562733828797be219a06fe2beff55f6ff797de07f")));
            ZkV0TransferActuator actuator = new ZkV0TransferActuator(Any.pack(builder.build()), dbManager);
            TransactionResultCapsule ret = new TransactionResultCapsule();
            try {
                actuator.validate();
                actuator.execute(ret);
                fail("fail");
            } catch (ContractValidateException e) {
                Assert.assertTrue(e instanceof ContractValidateException);
                Assert.assertEquals(
                        "Nf1 is invalid.", e.getMessage());
            } catch (ContractExeException e) {
                Assert.assertFalse(e instanceof ContractExeException);
            }
            builder.setNf1(ByteString.copyFrom(ByteArray.fromHexString(
                    "6d188c56fd3853e09afdbc2562733828797be219a06fe2beff55f6ff797de07f")));
        }

        // invalid nf2
        {
            builder.setNf2(ByteString.copyFrom(ByteArray.fromHexString("d72e75a66ce1b870d0a902accec5569476b4db5b87096209505ca87015729")));
            ZkV0TransferActuator actuator = new ZkV0TransferActuator(Any.pack(builder.build()), dbManager);
            TransactionResultCapsule ret = new TransactionResultCapsule();
            try {
                actuator.validate();
                actuator.execute(ret);
                fail("fail");
            } catch (ContractValidateException e) {
                Assert.assertTrue(e instanceof ContractValidateException);
                Assert.assertEquals(
                        "Nf2 is invalid.", e.getMessage());
            } catch (ContractExeException e) {
                Assert.assertFalse(e instanceof ContractExeException);
            }
            builder.setNf2(ByteString.copyFrom(ByteArray.fromHexString("ff6d72e75a66ce1b870d0a902accec5569476b4db5b87096209505ca87015729")));
        }

        // nf1 same to nf2
        {
            builder.setNf1(ByteString.copyFrom(ByteArray.fromHexString(
                    "ff6d72e75a66ce1b870d0a902accec5569476b4db5b87096209505ca87015729")));
            ZkV0TransferActuator actuator = new ZkV0TransferActuator(Any.pack(builder.build()), dbManager);
            TransactionResultCapsule ret = new TransactionResultCapsule();
            try {
                actuator.validate();
                actuator.execute(ret);
                fail("fail");
            } catch (ContractValidateException e) {
                Assert.assertTrue(e instanceof ContractValidateException);
                Assert.assertEquals(
                        "Nf1 equals to nf2.", e.getMessage());
            } catch (ContractExeException e) {
                Assert.assertFalse(e instanceof ContractExeException);
            }
            builder.setNf1(ByteString.copyFrom(ByteArray.fromHexString(
                    "6d188c56fd3853e09afdbc2562733828797be219a06fe2beff55f6ff797de07f")));
        }

        // nf1 is exist
        {
            byte[] nf1 = ByteArray.fromHexString(
                    "6d188c56fd3853e09afdbc2562733828797be219a06fe2beff55f6ff797de07f");
            dbManager.getNullfierStore().put(nf1, new BytesCapsule(nf1));

            ZkV0TransferActuator actuator = new ZkV0TransferActuator(Any.pack(builder.build()), dbManager);
            TransactionResultCapsule ret = new TransactionResultCapsule();
            try {
                actuator.validate();
                actuator.execute(ret);
                fail("fail");
            } catch (ContractValidateException e) {
                Assert.assertTrue(e instanceof ContractValidateException);
                Assert.assertEquals(
                        "Nf1 is exist.", e.getMessage());
            } catch (ContractExeException e) {
                Assert.assertFalse(e instanceof ContractExeException);
            }
            dbManager.getNullfierStore().delete(nf1);
        }

        // nf2 is exist
        {
            byte[] nf2 = ByteArray.fromHexString(
                    "ff6d72e75a66ce1b870d0a902accec5569476b4db5b87096209505ca87015729");
            dbManager.getNullfierStore().put(nf2, new BytesCapsule(nf2));

            ZkV0TransferActuator actuator = new ZkV0TransferActuator(Any.pack(builder.build()), dbManager);
            TransactionResultCapsule ret = new TransactionResultCapsule();
            try {
                actuator.validate();
                actuator.execute(ret);
                fail("fail");
            } catch (ContractValidateException e) {
                Assert.assertTrue(e instanceof ContractValidateException);
                Assert.assertEquals(
                        "Nf2 is exist.", e.getMessage());
            } catch (ContractExeException e) {
                Assert.assertFalse(e instanceof ContractExeException);
            }
            dbManager.getNullfierStore().delete(nf2);
        }

        // invalid cm1
        {
            builder.setCm1(ByteString.copyFrom(ByteArray.fromHexString(
                    "abd8002244c7ccffb1f9badfac4bd69dbf7d698667d5fbe53c5739c722d45")));
            ZkV0TransferActuator actuator = new ZkV0TransferActuator(Any.pack(builder.build()), dbManager);
            TransactionResultCapsule ret = new TransactionResultCapsule();
            try {
                actuator.validate();
                actuator.execute(ret);
                fail("fail");
            } catch (ContractValidateException e) {
                Assert.assertTrue(e instanceof ContractValidateException);
                Assert.assertEquals(
                        "Cm1 is invalid.", e.getMessage());
            } catch (ContractExeException e) {
                Assert.assertFalse(e instanceof ContractExeException);
            }
            builder.setCm1(ByteString.copyFrom(ByteArray.fromHexString(
                    "b67abd8002244c7ccffb1f9badfac4bd69dbf7d698667d5fbe53c5739c722d45")));
        }

        // invalid cm2
        {
            builder.setCm2(ByteString.copyFrom(ByteArray.fromHexString(
                    "f656685edb1c1b5b5590393b8a8538fe264537e37a3eecfa320d8c2a4faf")));
            ZkV0TransferActuator actuator = new ZkV0TransferActuator(Any.pack(builder.build()), dbManager);
            TransactionResultCapsule ret = new TransactionResultCapsule();
            try {
                actuator.validate();
                actuator.execute(ret);
                fail("fail");
            } catch (ContractValidateException e) {
                Assert.assertTrue(e instanceof ContractValidateException);
                Assert.assertEquals(
                        "Cm2 is invalid.", e.getMessage());
            } catch (ContractExeException e) {
                Assert.assertFalse(e instanceof ContractExeException);
            }
            builder.setCm2(ByteString.copyFrom(ByteArray.fromHexString(
                    "316ff656685edb1c1b5b5590393b8a8538fe264537e37a3eecfa320d8c2a4faf")));
        }

        // cm1 same to cm2
        {
            builder.setCm1(ByteString.copyFrom(ByteArray.fromHexString(
                    "316ff656685edb1c1b5b5590393b8a8538fe264537e37a3eecfa320d8c2a4faf")));
            ZkV0TransferActuator actuator = new ZkV0TransferActuator(Any.pack(builder.build()), dbManager);
            TransactionResultCapsule ret = new TransactionResultCapsule();
            try {
                actuator.validate();
                actuator.execute(ret);
                fail("fail");
            } catch (ContractValidateException e) {
                Assert.assertTrue(e instanceof ContractValidateException);
                Assert.assertEquals(
                        "Cm1 equals to Cm2.", e.getMessage());
            } catch (ContractExeException e) {
                Assert.assertFalse(e instanceof ContractExeException);
            }
            builder.setCm1(ByteString.copyFrom(ByteArray.fromHexString(
                    "b67abd8002244c7ccffb1f9badfac4bd69dbf7d698667d5fbe53c5739c722d45")));
        }

        // invalid pksig
        {
            builder.setPksig(ByteString.copyFrom(ByteArray.fromHexString(
                    "319e30af8cbc7e36f92bd9b44dcbd579f40fee24ff8a71e9d844e5499720")));
            ZkV0TransferActuator actuator = new ZkV0TransferActuator(Any.pack(builder.build()), dbManager);
            TransactionResultCapsule ret = new TransactionResultCapsule();
            try {
                actuator.validate();
                actuator.execute(ret);
                fail("fail");
            } catch (ContractValidateException e) {
                Assert.assertTrue(e instanceof ContractValidateException);
                Assert.assertEquals(
                        "Pksig is invalid.", e.getMessage());
            } catch (ContractExeException e) {
                Assert.assertFalse(e instanceof ContractExeException);
            }
            builder.setPksig(ByteString.copyFrom(ByteArray.fromHexString(
                    "bc14319e30af8cbc7e36f92bd9b44dcbd579f40fee24ff8a71e9d844e5499720")));
        }

        // invalid randomseed
        {
            builder.setRandomSeed(ByteString.copyFrom(ByteArray.fromHexString(
                    "5aa2c186f8ddcdda50d6a65cccc314e26e290f98d34ae18895df15f4d91ca")));
            ZkV0TransferActuator actuator = new ZkV0TransferActuator(Any.pack(builder.build()), dbManager);
            TransactionResultCapsule ret = new TransactionResultCapsule();
            try {
                actuator.validate();
                actuator.execute(ret);
                fail("fail");
            } catch (ContractValidateException e) {
                Assert.assertTrue(e instanceof ContractValidateException);
                Assert.assertEquals(
                        "RandomSeed is invalid.", e.getMessage());
            } catch (ContractExeException e) {
                Assert.assertFalse(e instanceof ContractExeException);
            }
            builder.setRandomSeed(ByteString.copyFrom(ByteArray.fromHexString(
                    "5595aa2c186f8ddcdda50d6a65cccc314e26e290f98d34ae18895df15f4d91ca")));
        }

        // invalid Epk
        {
            builder.setEpk(ByteString.copyFrom(ByteArray.fromHexString(
                    "dd3dbb705e7612d2b49dc700a0ee8adc8d048cccd6c5917b9dadf692e35f")));
            ZkV0TransferActuator actuator = new ZkV0TransferActuator(Any.pack(builder.build()), dbManager);
            TransactionResultCapsule ret = new TransactionResultCapsule();
            try {
                actuator.validate();
                actuator.execute(ret);
                fail("fail");
            } catch (ContractValidateException e) {
                Assert.assertTrue(e instanceof ContractValidateException);
                Assert.assertEquals(
                        "Epk is invalid.", e.getMessage());
            } catch (ContractExeException e) {
                Assert.assertFalse(e instanceof ContractExeException);
            }
            builder.setEpk(ByteString.copyFrom(ByteArray.fromHexString(
                    "63c7dd3dbb705e7612d2b49dc700a0ee8adc8d048cccd6c5917b9dadf692e35f")));
        }

        // invalid H1
        {
            builder.setH1(ByteString.copyFrom(ByteArray.fromHexString(
                    "ec36e2f5d52d8b36b06bfd0fe1ac3e384fa18fb8f3c749255fa3f606ca4b")));
            ZkV0TransferActuator actuator = new ZkV0TransferActuator(Any.pack(builder.build()), dbManager);
            TransactionResultCapsule ret = new TransactionResultCapsule();
            try {
                actuator.validate();
                actuator.execute(ret);
                fail("fail");
            } catch (ContractValidateException e) {
                Assert.assertTrue(e instanceof ContractValidateException);
                Assert.assertEquals(
                        "H1 is invalid.", e.getMessage());
            } catch (ContractExeException e) {
                Assert.assertFalse(e instanceof ContractExeException);
            }
            builder.setH1(ByteString.copyFrom(ByteArray.fromHexString(
                    "5f2fec36e2f5d52d8b36b06bfd0fe1ac3e384fa18fb8f3c749255fa3f606ca4b")));
        }

        // invalid H2
        {
            builder.setH2(ByteString.copyFrom(ByteArray.fromHexString(
                    "62a6dda3cc82484e95481b8a15c3bfc76f1acf207e2ec4bd745f7f5c8ef3")));
            ZkV0TransferActuator actuator = new ZkV0TransferActuator(Any.pack(builder.build()), dbManager);
            TransactionResultCapsule ret = new TransactionResultCapsule();
            try {
                actuator.validate();
                actuator.execute(ret);
                fail("fail");
            } catch (ContractValidateException e) {
                Assert.assertTrue(e instanceof ContractValidateException);
                Assert.assertEquals(
                        "H2 is invalid.", e.getMessage());
            } catch (ContractExeException e) {
                Assert.assertFalse(e instanceof ContractExeException);
            }
            builder.setH2(ByteString.copyFrom(ByteArray.fromHexString(
                    "f22262a6dda3cc82484e95481b8a15c3bfc76f1acf207e2ec4bd745f7f5c8ef3")));
        }

        // invalid C1
        {
            builder.setC1(ByteString.copyFrom(ByteArray.fromHexString(
                    "")));
            ZkV0TransferActuator actuator = new ZkV0TransferActuator(Any.pack(builder.build()), dbManager);
            TransactionResultCapsule ret = new TransactionResultCapsule();
            try {
                actuator.validate();
                actuator.execute(ret);
                fail("fail");
            } catch (ContractValidateException e) {
                Assert.assertTrue(e instanceof ContractValidateException);
                Assert.assertEquals(
                        "C1 is empty.", e.getMessage());
            } catch (ContractExeException e) {
                Assert.assertFalse(e instanceof ContractExeException);
            }
            builder.setC1(ByteString.copyFrom(ByteArray.fromHexString(
                    "ec1873d483c9463c87d036a265272467f20e8976a111459a275cf68bc0f74396d2e9549631cdc4c7207fb3e4c4beaf2890e238ee198a91bd6717b594f67c2f77e21d06ef9b3421b0a7352ca86edf0aab64d08addb38b20588f8e7b86901acae3dab58f1b47ce66b860491c354e11e30485075296190630d4689388b160ddaf60e95b1b297a3f89271a06872b94bcd0750f992f01f4e6019a971729695bc07f8e7e4ea35d2bdd861ea31cbee14024794b2e248bdbbab46340aacb6606a17a5d00bfdce8147f9a388e8f2dbb428a2556f4be4f437de0759bf7e5a2ebdf9df0c21f8057a857096ef04d9957849014e08fa454263b497e28353099b871f8e207e0138d97139dc0b75c29b9a6b0c1b6ae2f9eba097d8164046e3173fab590f476345b6febc1b2005eb06def17651373ce039386a99d95a652fa466c506eea1db96b398dbc08b8cda4969416b3d62d4b803ff6ed3ead1259df32dc36eb5e368a7ffd7cb5e5a0e3c700cb39f7e86173216e0a038394ae2a3dbb8d11ea5e17dabd028be0794160c5b2b606b631eea4fb66d362cc613e5c4370157a6f86ec9f7333d9df6861603ad581b32526412aad3746de73f86378a5b3a4021108b996fc270cec1bdc04ba67518c2d8002395b14314b303e15170998d964ba107f8f52036bdc5b90326949f9d344cb9914e90b2740690d04f31475127437815218d233891c98ae89f0f517c3d74b03a2668ff57fc050dd32badfd2139ab1993030f3e7f8da584206b1644d923daf8ed57a46be2e5e44fb30e47016a79ce29022229659bbec323f482638830e5881a8e2bc876ffa971c7aa4ee9987681f07ab10763c")));
        }

        // invalid c2
        {
            builder.setC2(ByteString.copyFrom(ByteArray.fromHexString(
                    "")));
            ZkV0TransferActuator actuator = new ZkV0TransferActuator(Any.pack(builder.build()), dbManager);
            TransactionResultCapsule ret = new TransactionResultCapsule();
            try {
                actuator.validate();
                actuator.execute(ret);
                fail("fail");
            } catch (ContractValidateException e) {
                Assert.assertTrue(e instanceof ContractValidateException);
                Assert.assertEquals(
                        "C2 is empty.", e.getMessage());
            } catch (ContractExeException e) {
                Assert.assertFalse(e instanceof ContractExeException);
            }
            builder.setC2(ByteString.copyFrom(ByteArray.fromHexString(
                    "3962fbd00c86f3d43c8441f16187a4daf9de53a65e347236c80af57f54ba1449583cda84b90b7a3550b27366e14076b16e3a343af9bb40a9b656a833d970ce2811ca0f181f0be7850cfd42671d206c286cfec6418bff49279fcbc9fff3a94885f3438fe5903a7ddbf154c22edf9a62f9c61990f6722af10c0e69efba3009528fec1b0ceb61330134f47690aecd464f4fc0b2241e342c977888e1601f6a6d03c1e658125b3d3a6b175da64129f5832ba726c352d7f6ae74eea44b244d8d5e89e81d26bd2e02dd9e64f7aa53ce26b22598cb7f122bfe80e9390af0cf7fdf1441df9ffc9092d0fba37c6aa805cb8d07645f99a11c60d7f133f1dab995496cbe2137b3854c8769efcbbd394f65bc74ad6e0bf81c6b10ad53d3eac6c57c6ff1b808ee521b93304014cb9165625f678bdec45e60d84f9d0a88d511b027becf49ec72bb1eb53d2cf1d9e19cdcf8680be6fbc23985f52faf432723533d64c36aa4255371e8ab278d60cc734d246eb548d6d8903ed9a33f26005df10048061eb9c06c96394eda71a77f62dc7b481f5a4d5a6fd3e7c4330aa22e9a6468bf4df6f849408d22cc27e25abe6eb31a8b1c157e4fb3d50c221184bbbbb9ab767924e7988fa0c88b75adc227f98d60df88fcf6a57a3273ea28478e0853a61534fcb1983f52ec0d4fbb3fbf24d8fa8a2230d0a1a325eec0d36d0faa2613650faec4994412bdc81f36095b39a43f70ca05b32691a5042f896171274cb5cad917d95267f3ae4c738c7efe95ff18501f61e92609bcfcb5caa456ba2cf20c0e167d40367867b5dcad4829b9b2eb659ba92c3b2a1e4996e7e276149acbea587c1efa0d64")));
        }

        // Proof is null.
        {
            builder.setProof(Contract.zkv0proof.getDefaultInstance());
            ZkV0TransferActuator actuator = new ZkV0TransferActuator(Any.pack(builder.build()), dbManager);
            TransactionResultCapsule ret = new TransactionResultCapsule();
            try {
                actuator.validate();
                actuator.execute(ret);
                fail("fail");
            } catch (ContractValidateException e) {
                Assert.assertTrue(e instanceof ContractValidateException);
                Assert.assertEquals(
                        "Proof is null.", e.getMessage());
            } catch (ContractExeException e) {
                Assert.assertFalse(e instanceof ContractExeException);
            }
        }

        // verify failure
        {
            Contract.zkv0proof proof = null;
            try {
                proof = Contract.zkv0proof.parseFrom( ByteArray.fromHexString("0a440a202ff536bb3600e4e31e0fe6dcb5c32a321654df30d9c0f84f733b77791ad349e912201073662d43b0143fa69b34d21fce25f3d0c11123092b2e8ec81b6e377bc0892612440a200c48ab0bf3e88dbd8e093180d0f0104df7bce8fb6067cb5b55fcc057b4fe11d21220207eb56f2918bea471bd614ea3c387de02039bb57a30cbbfc4528c32389addea1a88010a203051f4ff0ce6fd2cd6e347bf30b1748a2463c6366c35c7e723de5ec89ade1e5712200e3059f71306a1ddf653b54bfaddd12ce956679c723e601dd0417edc7f4c99671a201263e491746cda9f9c775914ba65ed481e24222b0652ee1baf071f70be25d2ad222022c5c529e5c224e9fbc67ae3ee2326adcf546ba81d542c8d4cf1816dc9202e6322440a201a458db08dcd61b3589395f881623288cc59d6896ae495686b277a15e77765751220156fcc6ba5b0a9a3f4905670a5469faf8f415cc1db5aceba5f9cfd7c6ec4a37d2a440a202d68b28daa36bc4c7643a824af0fa1e4fc6abb9127ee5eeecdaf61c9cc340f0f12202ae00b2b588ca010c08f34c4ddedaa1010eada0e28f5981151be0731f3c8a6c632440a200a866a04aeba0d15c599ddf5871254a307159d8d83c6d29d0a74b707c658b2c1122002427a6350912f68103f9b52fe76d3dc11a83f020232df8290ea6f96fea6f1373a440a201535512c5269b981560b18f4d2843a81db744f6835a7974abf33b484e60b39bb1220177b2a52b8c4712cc60ba05dbab5a0ed5adb70dc3b358b79ed62418ee111f9b142440a200a58c61374e9113621ea6a9e249fb6aaeba5e995b11bbc31d8303d55b6a2c5aa122029b202ab3e3f2b45a2adf8426e91e0c6326c677851bce0dec6d49bcb48e6dc08"));
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

            builder.setProof(proof);
            ZkV0TransferActuator actuator = new ZkV0TransferActuator(Any.pack(builder.build()), dbManager);
            TransactionResultCapsule ret = new TransactionResultCapsule();
            try {
                actuator.validate();
                actuator.execute(ret);
                fail("fail");
            } catch (ContractValidateException e) {
                Assert.assertTrue(e instanceof ContractValidateException);
                Assert.assertEquals(
                        "verify failed return 4 .", e.getMessage());
            } catch (ContractExeException e) {
                Assert.assertFalse(e instanceof ContractExeException);
            }
        }




















    }
}
