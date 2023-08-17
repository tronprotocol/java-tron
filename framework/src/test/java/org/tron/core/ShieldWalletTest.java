package org.tron.core;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyCollectionOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.tron.core.services.http.FullNodeHttpApiService.librustzcashInitZksnarkParams;

import java.math.BigInteger;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.api.GrpcAPI.PrivateParameters;
import org.tron.api.GrpcAPI.PrivateParametersWithoutAsk;
import org.tron.api.GrpcAPI.PrivateShieldedTRC20Parameters;
import org.tron.api.GrpcAPI.PrivateShieldedTRC20ParametersWithoutAsk;
import org.tron.api.GrpcAPI.ShieldedAddressInfo;
import org.tron.api.GrpcAPI.ShieldedTRC20Parameters;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.services.http.JsonFormat;
import org.tron.core.services.http.JsonFormat.ParseException;


public class ShieldWalletTest extends BaseTest {

  @Resource
  private Wallet wallet;

  static {
    dbPath = "shield_wallet_test";
    Args.setParam(new String[] {"-d", dbPath}, Constant.TEST_CONF);
  }

  @Test
  public void testCreateShieldedTransaction1() {
    librustzcashInitZksnarkParams();
    String transactionStr1 = "{\n"
        + "    \"transparent_from_address\": \"4C90A72AC3DAEF2E689245EF40089D641CEAC7743243AB31316000\",\n"
        + "    \"from_amount\": 100000000,\n"
        + "    \"ovk\":\"5fd1ecdfd679b2ada337b250b1024718f31d9cb71cae6dfcc9a1a67ba4703d8c\",\n"
        + "    \"shielded_receives\": [\n"
        + "        {\n"
        + "            \"note\": {\n"
        + "                \"value\": 90000000,\n"
        + "                \"payment_address\": \"ztron1yam3jr2ptmyplpp0tyx8ytpz6sjdxcdez0j33fscmejv7nhc4aq3mvkhrurgrwvqe92cjlh5k3g\",\n"
        + "                \"rcm\": \"723053bcbfecdf5da66c18ab0376476ef308c61b7abe891b2c01e903bcb87c0e\",\n"
        + "                \"memo\": \"22222\"\n"
        + "            }\n"
        + "        }\n"
        + "    ]\n"
        + "}";

    PrivateParameters.Builder builder1 = PrivateParameters.newBuilder();
    try {
      JsonFormat.merge(transactionStr1, builder1, false);
    } catch (ParseException e) {
      Assert.fail();
    }

    try {
      TransactionCapsule transactionCapsule = wallet.createShieldedTransaction(builder1.build());
      Assert.assertNotNull(transactionCapsule);
    } catch (Exception e) {
      Assert.fail();
    }
  }

  @Test
  public void testCreateShieldedTransaction2() {
    librustzcashInitZksnarkParams();
    String transactionStr2 = "{\n"
        + "    \"ask\": \"98fd03167f2347b54d77238417f6708d57299d987cba8855de07be24b61dde0d\",\n"
        + "    \"nsk\": \"d0cb30e7d0f4e4535561434c04bac91f09d20e6d6b463ab01be4f518b0689e09\",\n"
        + "    \"ovk\": \"5fd1ecdfd679b2ada337b250b1024718f31d9cb71cae6dfcc9a1a67ba4703d8c\",\n"
        + "    \"shielded_spends\": [\n"
        + "        { \n"
        + "\t\t\t\"note\": {\n"
        + "                \"value\": 90000000,\n"
        + "                \"payment_address\": \"ztron1yam3jr2ptmyplpp0tyx8ytpz6sjdxcdez0j33fscmejv7nhc4aq3mvkhrurgrwvqe92cjlh5k3g\",\n"
        + "                \"rcm\": \"723053bcbfecdf5da66c18ab0376476ef308c61b7abe891b2c01e903bcb87c0e\",\n"
        + "                \"memo\": \"22222\"\n"
        + "            },\n"
        + "            \"alpha\": \"296ee1c52c93f740d3ab1b72e6dcf5abb1d6f49ed8edb6f34ef3fdb6e76c550c\",\n"
        + "            \"voucher\": {\n"
        + "            \"tree\": {\n"
        + "                \"left\": {\n"
        + "                    \"content\": \"4eee2619c48ba2bd025025630cd013bacaf37e9a21b29e398975b7ce8d646a2e\"\n"
        + "                },\n"
        + "                \"right\": {\n"
        + "                    \"content\": \"538c2a91571335c4fa888f9b43b9fadd91c34fcf2befa0391592a5d6e5459b57\"\n"
        + "                }\n"
        + "            },\n"
        + "            \"rt\": \"c597d6c25f11b98a680044889188f8a94ab792e542214a8f64f2634ae54e5551\"\n"
        + "        },\n"
        + "            \"path\": \"2020b2eed031d4d6a4f02a097f80b54cc1541d4163c6b6f5971f88b6e41d35c538142012935f14b676509b81eb49ef25f39269ed72309238b4c145803544b646dca62d20e1f34b034d4a3cd28557e2907ebf990c918f64ecb50a94f01d6fda5ca5c7ef722028e7b841dcbc47cceb69d7cb8d94245fb7cb2ba3a7a6bc18f13f945f7dbd6e2a20a5122c08ff9c161d9ca6fc462073396c7d7d38e8ee48cdb3bea7e2230134ed6a20d2e1642c9a462229289e5b0e3b7f9008e0301cbb93385ee0e21da2545073cb582016d6252968971a83da8521d65382e61f0176646d771c91528e3276ee45383e4a20fee0e52802cb0c46b1eb4d376c62697f4759f6c8917fa352571202fd778fd712204c6937d78f42685f84b43ad3b7b00f81285662f85c6a68ef11d62ad1a3ee0850200769557bc682b1bf308646fd0b22e648e8b9e98f57e29f5af40f6edb833e2c492008eeab0c13abd6069e6310197bf80f9c1ea6de78fd19cbae24d4a520e6cf3023208d5fa43e5a10d11605ac7430ba1f5d81fb1b68d29a640405767749e841527673206aca8448d8263e547d5ff2950e2ed3839e998d31cbc6ac9fd57bc6002b15921620cd1c8dbf6e3acc7a80439bc4962cf25b9dce7c896f3a5bd70803fc5a0e33cf00206edb16d01907b759977d7650dad7e3ec049af1a3d875380b697c862c9ec5d51c201ea6675f9551eeb9dfaaa9247bc9858270d3d3a4c5afa7177a984d5ed1be245120d6acdedf95f608e09fa53fb43dcd0990475726c5131210c9e5caeab97f0e642f20bd74b25aacb92378a871bf27d225cfc26baca344a1ea35fdd94510f3d157082c201b77dac4d24fb7258c3c528704c59430b630718bec486421837021cf75dab65120ec677114c27206f5debc1c1ed66f95e2b1885da5b7be3d736b1de98579473048204777c8776a3b1e69b73a62fa701fa4f7a6282d9aee2c7a6b82e7937d7081c23c20ba49b659fbd0b7334211ea6a9d9df185c757e70aa81da562fb912b84f49bce722043ff5457f13b926b61df552d4e402ee6dc1463f99a535f9a713439264d5b616b207b99abdc3730991cc9274727d7d82d28cb794edbc7034b4f0053ff7c4b68044420d6c639ac24b46bd19341c91b13fdcab31581ddaf7f1411336a271f3d0aa52813208ac9cf9c391e3fd42891d27238a81a8a5c1d3a72b1bcbea8cf44a58ce738961320912d82b2c2bca231f71efcf61737fbf0a08befa0416215aeef53e8bb6d23390a20e110de65c907b9dea4ae0bd83a4b0a51bea175646a64c12b4c9f931b2cb31b4920d8283386ef2ef07ebdbb4383c12a739a953a4d6e0d6fb1139a4036d693bfbb6c20ffe9fc03f18b176c998806439ff0bb8ad193afdb27b2ccbc88856916dd804e3420817de36ab2d57feb077634bca77819c8e0bd298c04f6fed0e6a83cc1356ca155204eee2619c48ba2bd025025630cd013bacaf37e9a21b29e398975b7ce8d646a2e0100000000000000\"\n"
        + "        }\n"
        + "    ],\n"
        + "    \"shielded_receives\": [\n"
        + "        {\n"
        + "            \"note\": {\n"
        + "                \"value\": 80000000,\n"
        + "                \"payment_address\": \"ztron15kezpsmq05h3majnlzh40gryjmgm60x2ehjygc7q8s67mlu857cutqwa348nvpgepn5xk3g5rrx\",\n"
        + "                \"rcm\": \"723053bcbfecdf5da66c18ab0376476ef308c61b7abe891b2c01e903bcb87c0e\",\n"
        + "                \"memo\": \"111111111\"\n"
        + "            }\n"
        + "        }\n"
        + "    ],\n"
        + "    \"visible\" : true\n"
        + "}";

    PrivateParameters.Builder builder2 = PrivateParameters.newBuilder();
    try {
      JsonFormat.merge(transactionStr2, builder2, true);
    } catch (ParseException e) {
      Assert.fail();
    }

    try {
      TransactionCapsule transactionCapsule = wallet.createShieldedTransaction(builder2.build());
      Assert.assertNotNull(transactionCapsule);
    } catch (Exception e) {
      Assert.fail();
    }
  }

  @Test
  public void testCreateShieldedTransactionWithoutSpendAuthSig() {
    librustzcashInitZksnarkParams();
    String transactionStr3 = "{\n"
        + "    \"ak\": \"71ad68c4f50574d5ad37548cbe86601f7296b91a0cb05557314b7808472702de\",\n"
        + "    \"nsk\": \"d0cb30e7d0f4e4535561434c04bac91f09d20e6d6b463ab01be4f518b0689e09\",\n"
        + "    \"ovk\": \"5fd1ecdfd679b2ada337b250b1024718f31d9cb71cae6dfcc9a1a67ba4703d8c\",\n"
        + "    \"shielded_spends\": [\n"
        + "        {\n"
        + "            \"note\": {\n"
        + "                \"value\": 90000000,\n"
        + "                \"payment_address\": \"ztron1yam3jr2ptmyplpp0tyx8ytpz6sjdxcdez0j33fscmejv7nhc4aq3mvkhrurgrwvqe92cjlh5k3g\",\n"
        + "                \"rcm\": \"723053bcbfecdf5da66c18ab0376476ef308c61b7abe891b2c01e903bcb87c0e\",\n"
        + "                \"memo\": \"22222\"\n"
        + "            },\n"
        + "            \"alpha\": \"296ee1c52c93f740d3ab1b72e6dcf5abb1d6f49ed8edb6f34ef3fdb6e76c550c\",\n"
        + "            \"voucher\": {\n"
        + "                \"tree\": {\n"
        + "                    \"left\": {\n"
        + "                        \"content\": \"4eee2619c48ba2bd025025630cd013bacaf37e9a21b29e398975b7ce8d646a2e\"\n"
        + "                    },\n"
        + "                    \"right\": {\n"
        + "                        \"content\": \"538c2a91571335c4fa888f9b43b9fadd91c34fcf2befa0391592a5d6e5459b57\"\n"
        + "                    }\n"
        + "                },\n"
        + "                \"rt\": \"c597d6c25f11b98a680044889188f8a94ab792e542214a8f64f2634ae54e5551\"\n"
        + "            },\n"
        + "            \"path\": \"2020b2eed031d4d6a4f02a097f80b54cc1541d4163c6b6f5971f88b6e41d35c538142012935f14b676509b81eb49ef25f39269ed72309238b4c145803544b646dca62d20e1f34b034d4a3cd28557e2907ebf990c918f64ecb50a94f01d6fda5ca5c7ef722028e7b841dcbc47cceb69d7cb8d94245fb7cb2ba3a7a6bc18f13f945f7dbd6e2a20a5122c08ff9c161d9ca6fc462073396c7d7d38e8ee48cdb3bea7e2230134ed6a20d2e1642c9a462229289e5b0e3b7f9008e0301cbb93385ee0e21da2545073cb582016d6252968971a83da8521d65382e61f0176646d771c91528e3276ee45383e4a20fee0e52802cb0c46b1eb4d376c62697f4759f6c8917fa352571202fd778fd712204c6937d78f42685f84b43ad3b7b00f81285662f85c6a68ef11d62ad1a3ee0850200769557bc682b1bf308646fd0b22e648e8b9e98f57e29f5af40f6edb833e2c492008eeab0c13abd6069e6310197bf80f9c1ea6de78fd19cbae24d4a520e6cf3023208d5fa43e5a10d11605ac7430ba1f5d81fb1b68d29a640405767749e841527673206aca8448d8263e547d5ff2950e2ed3839e998d31cbc6ac9fd57bc6002b15921620cd1c8dbf6e3acc7a80439bc4962cf25b9dce7c896f3a5bd70803fc5a0e33cf00206edb16d01907b759977d7650dad7e3ec049af1a3d875380b697c862c9ec5d51c201ea6675f9551eeb9dfaaa9247bc9858270d3d3a4c5afa7177a984d5ed1be245120d6acdedf95f608e09fa53fb43dcd0990475726c5131210c9e5caeab97f0e642f20bd74b25aacb92378a871bf27d225cfc26baca344a1ea35fdd94510f3d157082c201b77dac4d24fb7258c3c528704c59430b630718bec486421837021cf75dab65120ec677114c27206f5debc1c1ed66f95e2b1885da5b7be3d736b1de98579473048204777c8776a3b1e69b73a62fa701fa4f7a6282d9aee2c7a6b82e7937d7081c23c20ba49b659fbd0b7334211ea6a9d9df185c757e70aa81da562fb912b84f49bce722043ff5457f13b926b61df552d4e402ee6dc1463f99a535f9a713439264d5b616b207b99abdc3730991cc9274727d7d82d28cb794edbc7034b4f0053ff7c4b68044420d6c639ac24b46bd19341c91b13fdcab31581ddaf7f1411336a271f3d0aa52813208ac9cf9c391e3fd42891d27238a81a8a5c1d3a72b1bcbea8cf44a58ce738961320912d82b2c2bca231f71efcf61737fbf0a08befa0416215aeef53e8bb6d23390a20e110de65c907b9dea4ae0bd83a4b0a51bea175646a64c12b4c9f931b2cb31b4920d8283386ef2ef07ebdbb4383c12a739a953a4d6e0d6fb1139a4036d693bfbb6c20ffe9fc03f18b176c998806439ff0bb8ad193afdb27b2ccbc88856916dd804e3420817de36ab2d57feb077634bca77819c8e0bd298c04f6fed0e6a83cc1356ca155204eee2619c48ba2bd025025630cd013bacaf37e9a21b29e398975b7ce8d646a2e0100000000000000\"\n"
        + "        }\n"
        + "    ],\n"
        + "    \"shielded_receives\": [\n"
        + "        {\n"
        + "            \"note\": {\n"
        + "                \"value\": 80000000,\n"
        + "                \"payment_address\": \"ztron15kezpsmq05h3majnlzh40gryjmgm60x2ehjygc7q8s67mlu857cutqwa348nvpgepn5xk3g5rrx\",\n"
        + "                \"rcm\": \"723053bcbfecdf5da66c18ab0376476ef308c61b7abe891b2c01e903bcb87c0e\",\n"
        + "                \"memo\": \"111111111\"\n"
        + "            }\n"
        + "        }\n"
        + "    ],\n"
        + "    \"visible\": true\n"
        + "}";

    PrivateParametersWithoutAsk.Builder builder3 = PrivateParametersWithoutAsk.newBuilder();
    try {
      JsonFormat.merge(transactionStr3, builder3, false);
    } catch (ParseException e) {
      Assert.fail();
    }

    try {
      TransactionCapsule transactionCapsule = wallet.createShieldedTransactionWithoutSpendAuthSig(
          builder3.build());
      Assert.assertNotNull(transactionCapsule);
    } catch (Exception e) {
      Assert.fail();
    }
  }

  @Test
  public void testGetNewShieldedAddress() {
    librustzcashInitZksnarkParams();
    try {
      ShieldedAddressInfo shieldedAddressInfo = wallet.getNewShieldedAddress();
      Assert.assertNotNull(shieldedAddressInfo);
    } catch (Exception e) {
      Assert.fail();
    }
  }

  @Test
  public void testCreateShieldedContractParameters() throws ContractExeException {
    librustzcashInitZksnarkParams();
    Args.getInstance().setFullNodeAllowShieldedTransactionArgs(true);
    Wallet wallet1 = Mockito.mock(Wallet.class);

    Mockito.when(wallet1.getShieldedContractScalingFactor(
            ByteArray.fromHexString("4144007979359ECAC395BBD3CEF8060D3DF2DC3F01")))
        .thenReturn(BigInteger.valueOf(1).toByteArray());

    String parameter = "{\n"
        + "    \"ask\":\"c2513e9e308494932bd82e0ce53662d17421d90b72a8471a0a12b8552a336e02\",\n"
        + "    \"nsk\":\"4c6bf3dd4a0643d20b628f7e45980c5e187f07a51d6f3e86aaf1ab916c07eb0d\",\n"
        + "    \"ovk\":\"17a58d9a5058da6e42ca12cd289d0a6aa169b926c18e19bca518b8d6f8674e43\",\n"
        + "    \"from_amount\":\"100\",\n"
        + "    \"shielded_receives\":[\n"
        + "        {\n"
        + "            \"note\":{\n"
        + "                \"value\":100,\n"
        + "                \"payment_address\":\"ztron1y99u6ejqenupvfkp5g6q6yqkp0a44c48cta0dd5gejtqa4v27hqa2cghfvdxnmneh6qqq03fa75\",\n"
        + "                \"rcm\":\"16b6f5e40444ab7eeab11ae6613c27f35117971efa87b71560b5813829c9390d\"\n"
        + "            }\n"
        + "        }\n"
        + "    ],\n"
        + "    \"shielded_TRC20_contract_address\":\"4144007979359ECAC395BBD3CEF8060D3DF2DC3F01\"\n"
        + "}";
    PrivateShieldedTRC20Parameters.Builder builder = PrivateShieldedTRC20Parameters.newBuilder();
    try {
      JsonFormat.merge(parameter, builder, false);
    } catch (ParseException e) {
      Assert.fail();
    }

    try {
      ShieldedTRC20Parameters shieldedTRC20Parameters = wallet1.createShieldedContractParameters(
          builder.build());
      System.out.println(shieldedTRC20Parameters);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testCreateShieldedContractParametersWithoutAsk() throws ContractExeException {
    librustzcashInitZksnarkParams();
    Args.getInstance().setFullNodeAllowShieldedTransactionArgs(true);
    Wallet wallet1 = Mockito.mock(Wallet.class);

    Mockito.when(wallet1.getShieldedContractScalingFactor(
            ByteArray.fromHexString("4144007979359ECAC395BBD3CEF8060D3DF2DC3F01")))
        .thenReturn(BigInteger.valueOf(1).toByteArray());
    String parameter = "{\n"
        + "    \"ak\":\"0eba73a48e2949ea5daa13bcef4fdf3a5aa9f3b268067cf81123398a838fe3cc\",\n"
        + "    \"nsk\":\"4c6bf3dd4a0643d20b628f7e45980c5e187f07a51d6f3e86aaf1ab916c07eb0d\",\n"
        + "    \"ovk\":\"17a58d9a5058da6e42ca12cd289d0a6aa169b926c18e19bca518b8d6f8674e43\",\n"
        + "    \"from_amount\":\"100\",\n"
        + "    \"shielded_receives\":[\n"
        + "        {\n"
        + "            \"note\":{\n"
        + "                \"value\":100,\n"
        + "                \"payment_address\":\"ztron1y99u6ejqenupvfkp5g6q6yqkp0a44c48cta0dd5gejtqa4v27hqa2cghfvdxnmneh6qqq03fa75\",\n"
        + "                \"rcm\":\"16b6f5e40444ab7eeab11ae6613c27f35117971efa87b71560b5813829c9390d\"\n"
        + "            }\n"
        + "        }\n"
        + "    ],\n"
        + "    \"shielded_TRC20_contract_address\":\"4144007979359ECAC395BBD3CEF8060D3DF2DC3F01\"\n"
        + "}";
    PrivateShieldedTRC20ParametersWithoutAsk.Builder builder = PrivateShieldedTRC20ParametersWithoutAsk.newBuilder();
    try {
      JsonFormat.merge(parameter, builder, false);
    } catch (ParseException e) {
      Assert.fail();
    }
    try {
      ShieldedTRC20Parameters shieldedTRC20Parameters = wallet1.createShieldedContractParametersWithoutAsk(
          builder.build());
      System.out.println(shieldedTRC20Parameters);
      //Assert.assertNotNull(shieldedTRC20Parameters);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
