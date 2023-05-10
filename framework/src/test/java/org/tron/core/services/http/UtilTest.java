package org.tron.core.services.http;

import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.tron.api.GrpcAPI.TransactionApprovedList;
import org.tron.api.GrpcAPI.TransactionSignWeight;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.config.args.Args;
import org.tron.core.utils.TransactionUtil;
import org.tron.protos.Protocol.Transaction;

public class UtilTest extends BaseTest {

  @Resource
  private Wallet wallet;
  @Resource
  private TransactionUtil transactionUtil;

  static {
    dbPath = "output_util_test";
    Args.setParam(new String[] {"-d", dbPath}, Constant.TEST_CONF);
  }

  @Test
  public void testPackTransactionWithInvalidType() {

    String strTransaction = "{\n"
        + "    \"visible\": false,\n"
        + "    \"signature\": [\n"
        + "        \"5c23bddabccd3e4e5ebdf7d2f21dc58af9f88e0b99620374c5354e0dd9efb3a436167d95b70d2"
        + "d825180bf90bc84525acb13a203f209afd5d397316f6b2c387c01\"\n"
        + "    ],\n"
        + "    \"txID\": \"fc33817936b06e50d4b6f1797e62f52d69af6c0da580a607241a9c03a48e390e\",\n"
        + "    \"raw_data\": {\n"
        + "        \"contract\": [\n"
        + "            {\n"
        + "                \"parameter\": {\n"
        + "                    \"value\": {\n"
        + "                      \"amount\": 10,\n"
        + "                      \"owner_address\":\"41c076305e35aea1fe45a772fcaaab8a36e87bdb55\","
        + "                      \"to_address\": \"415624c12e308b03a1a6b21d9b86e3942fac1ab92b\"\n"
        + "                    },\n"
        + "                    \"type_url\": \"type.googleapis.com/protocol.TransferContract\"\n"
        + "                },\n"
        + "                \"type\": \"TransferContract11111\"\n"
        + "            }\n"
        + "        ],\n"
        + "        \"ref_block_bytes\": \"d8ed\",\n"
        + "        \"ref_block_hash\": \"2e066c3259e756f5\",\n"
        + "        \"expiration\": 1651906644000,\n"
        + "        \"timestamp\": 1651906586162\n"
        + "    },\n"
        + "    \"raw_data_hex\": \"0a02d8ed22082e066c3259e756f540a090bcea89305a65080112610a2d747970"
        + "652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e5472616e73666572436f6e74726163741230"
        + "0a1541c076305e35aea1fe45a772fcaaab8a36e87bdb551215415624c12e308b03a1a6b21d9b86e3942fac1a"
        + "b92b180a70b2ccb8ea8930\"\n"
        + "}";
    Transaction transaction = Util.packTransaction(strTransaction, false);
    TransactionApprovedList transactionApprovedList =
        wallet.getTransactionApprovedList(transaction);
    Assert.assertEquals("Invalid transaction: no valid contract",
        transactionApprovedList.getResult().getMessage());

    TransactionSignWeight txSignWeight = transactionUtil.getTransactionSignWeight(transaction);
    Assert.assertEquals("Invalid transaction: no valid contract",
        txSignWeight.getResult().getMessage());


    strTransaction = "{\n"
        + "    \"visible\": false,\n"
        + "    \"signature\": [\n"
        + "        \"5c23bddabccd3e4e5ebdf7d2f21dc58af9f88e0b99620374c5354e0dd9efb3a436167d95b70d2d"
        + "825180bf90bc84525acb13a203f209afd5d397316f6b2c387c01\"\n"
        + "    ],\n"
        + "    \"txID\": \"fc33817936b06e50d4b6f1797e62f52d69af6c0da580a607241a9c03a48e390e\",\n"
        + "    \"raw_data\": {\n"
        + "        \"contract\": [\n"
        + "            {\n"
        + "                \"parameter\": {\n"
        + "                    \"value\": {\n"
        + "                      \"amount\": 10,\n"
        + "                      \"owner_address\":\"41c076305e35aea1fe45a772fcaaab8a36e87bdb55\","
        + "                      \"to_address\": \"415624c12e308b03a1a6b21d9b86e3942fac1ab92b\"\n"
        + "                    },\n"
        + "                    \"type_url\": \"type.googleapis.com/protocol.TransferContract\"\n"
        + "                }\n"
        + "            }\n"
        + "        ],\n"
        + "        \"ref_block_bytes\": \"d8ed\",\n"
        + "        \"ref_block_hash\": \"2e066c3259e756f5\",\n"
        + "        \"expiration\": 1651906644000,\n"
        + "        \"timestamp\": 1651906586162\n"
        + "    },\n"
        + "    \"raw_data_hex\": \"0a02d8ed22082e066c3259e756f540a090bcea89305a65080112610a2d747970"
        + "652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e5472616e73666572436f6e74726163741230"
        + "0a1541c076305e35aea1fe45a772fcaaab8a36e87bdb551215415624c12e308b03a1a6b21d9b86e3942fac1a"
        + "b92b180a70b2ccb8ea8930\"\n"
        + "}";
    transaction = Util.packTransaction(strTransaction, false);
    transactionApprovedList = wallet.getTransactionApprovedList(transaction);
    Assert.assertEquals("Invalid transaction: no valid contract",
        transactionApprovedList.getResult().getMessage());

    txSignWeight = transactionUtil.getTransactionSignWeight(transaction);
    Assert.assertEquals("Invalid transaction: no valid contract",
        txSignWeight.getResult().getMessage());
  }
}
