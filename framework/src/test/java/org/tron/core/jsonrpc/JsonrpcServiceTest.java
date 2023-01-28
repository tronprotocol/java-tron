package org.tron.core.jsonrpc;

import com.google.protobuf.ByteString;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.services.NodeInfoService;
import org.tron.core.services.jsonrpc.TronJsonRpcImpl;
import org.tron.core.services.jsonrpc.types.BlockResult;
import org.tron.core.services.jsonrpc.types.TransactionResult;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.BalanceContract.TransferContract;


@Slf4j
public class JsonrpcServiceTest extends BaseTest {
  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_ACCOUNT_NAME = "first";

  private static TronJsonRpcImpl tronJsonRpc;
  @Resource
  private NodeInfoService nodeInfoService;

  private static BlockCapsule blockCapsule;
  private static TransactionCapsule transactionCapsule1;
  @Resource
  private Wallet wallet;

  static {
    dbPath = "output_jsonrpc_service_test";
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);

    OWNER_ADDRESS =
        Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
  }

  @Before
  public void init() {
    AccountCapsule accountCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(OWNER_ADDRESS_ACCOUNT_NAME),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            Protocol.AccountType.Normal,
            10000_000_000L);
    dbManager.getAccountStore().put(accountCapsule.getAddress().toByteArray(), accountCapsule);

    blockCapsule = new BlockCapsule(
        1,
        Sha256Hash.wrap(ByteString.copyFrom(
            ByteArray.fromHexString(
                "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81"))),
        1,
        ByteString.copyFromUtf8("testAddress"));
    TransferContract transferContract1 = TransferContract.newBuilder()
        .setAmount(1L)
        .setOwnerAddress(ByteString.copyFrom("0x0000000000000000000".getBytes()))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(
            (Wallet.getAddressPreFixString() + "A389132D6639FBDA4FBC8B659264E6B7C90DB086"))))
        .build();

    TransferContract transferContract2 = TransferContract.newBuilder()
        .setAmount(2L)
        .setOwnerAddress(ByteString.copyFrom("0x0000000000000000000".getBytes()))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(
            (Wallet.getAddressPreFixString() + "ED738B3A0FE390EAA71B768B6D02CDBD18FB207B"))))
        .build();

    transactionCapsule1 =
        new TransactionCapsule(transferContract1, ContractType.TransferContract);
    transactionCapsule1.setBlockNum(blockCapsule.getNum());
    TransactionCapsule transactionCapsule2 = new TransactionCapsule(transferContract2,
        ContractType.TransferContract);
    transactionCapsule2.setBlockNum(2L);

    blockCapsule.addTransaction(transactionCapsule1);
    blockCapsule.addTransaction(transactionCapsule2);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(1L);
    dbManager.getBlockIndexStore().put(blockCapsule.getBlockId());
    dbManager.getBlockStore().put(blockCapsule.getBlockId().getBytes(), blockCapsule);

    dbManager.getTransactionStore()
        .put(transactionCapsule1.getTransactionId().getBytes(), transactionCapsule1);
    dbManager.getTransactionStore()
        .put(transactionCapsule2.getTransactionId().getBytes(), transactionCapsule2);

    tronJsonRpc = new TronJsonRpcImpl(nodeInfoService, wallet, dbManager);
  }

  @Test
  public void testWeb3Sha3() {
    String result = "";
    try {
      result = tronJsonRpc.web3Sha3("0x1122334455667788");
    } catch (Exception e) {
      Assert.fail();
    }

    Assert.assertEquals("0x1360118a9c9fd897720cf4e26de80683f402dd7c28e000aa98ea51b85c60161c",
        result);

    try {
      tronJsonRpc.web3Sha3("1122334455667788");
    } catch (Exception e) {
      Assert.assertEquals("invalid input value", e.getMessage());
    }
  }

  @Test
  public void testGetBlockTransactionCountByHash() {
    try {
      tronJsonRpc.ethGetBlockTransactionCountByHash("0x111111");
    } catch (Exception e) {
      Assert.assertEquals("invalid hash value", e.getMessage());
    }

    String result = "";
    try {
      result = tronJsonRpc.ethGetBlockTransactionCountByHash(
          "0x1111111111111111111111111111111111111111111111111111111111111111");
    } catch (Exception e) {
      Assert.fail();
    }
    Assert.assertNull(result);

    try {
      result = tronJsonRpc.ethGetBlockTransactionCountByHash(
          Hex.toHexString((blockCapsule.getBlockId().getBytes())));
    } catch (Exception e) {
      Assert.fail();
    }
    Assert.assertEquals(ByteArray.toJsonHex(blockCapsule.getTransactions().size()), result);

  }

  @Test
  public void testGetBlockTransactionCountByNumber() {
    String result = "";
    try {
      result = tronJsonRpc.ethGetBlockTransactionCountByNumber("0x0");
    } catch (Exception e) {
      Assert.assertNull(result);
    }

    try {
      result = tronJsonRpc.ethGetBlockTransactionCountByNumber("pending");
    } catch (Exception e) {
      Assert.assertEquals("TAG pending not supported", e.getMessage());
    }

    try {
      result = tronJsonRpc.ethGetBlockTransactionCountByNumber("qqqqq");
    } catch (Exception e) {
      Assert.assertEquals("invalid block number", e.getMessage());
    }

    try {
      result = tronJsonRpc.ethGetBlockTransactionCountByNumber("latest");
    } catch (Exception e) {
      Assert.fail();
    }
    Assert.assertEquals(ByteArray.toJsonHex(blockCapsule.getTransactions().size()), result);

    try {
      result = tronJsonRpc
          .ethGetBlockTransactionCountByNumber(ByteArray.toJsonHex(blockCapsule.getNum()));
    } catch (Exception e) {
      Assert.fail();
    }
    Assert.assertEquals(ByteArray.toJsonHex(blockCapsule.getTransactions().size()), result);

  }

  @Test
  public void testGetBlockByHash() {
    BlockResult blockResult = null;
    try {
      blockResult = tronJsonRpc
          .ethGetBlockByHash(Hex.toHexString((blockCapsule.getBlockId().getBytes())), false);
    } catch (Exception e) {
      Assert.fail();
    }
    Assert.assertEquals(ByteArray.toJsonHex(blockCapsule.getNum()), blockResult.getNumber());
    Assert
        .assertEquals(blockCapsule.getTransactions().size(), blockResult.getTransactions().length);
  }

  @Test
  public void testGetBlockByNumber() {
    BlockResult blockResult = null;
    try {
      blockResult = tronJsonRpc
          .ethGetBlockByNumber(ByteArray.toJsonHex(blockCapsule.getNum()), false);
    } catch (Exception e) {
      Assert.fail();
    }

    Assert.assertEquals(ByteArray.toJsonHex(blockCapsule.getNum()), blockResult.getNumber());
    Assert
        .assertEquals(blockCapsule.getTransactions().size(), blockResult.getTransactions().length);
    Assert.assertEquals("0x0000000000000000", blockResult.getNonce());
  }


  @Test
  public void testGetTransactionByHash() {
    TransactionResult transactionResult = null;
    try {
      transactionResult = tronJsonRpc.getTransactionByHash(
          "0x1111111111111111111111111111111111111111111111111111111111111111");
    } catch (Exception e) {
      Assert.fail();
    }
    Assert.assertNull(transactionResult);

    try {
      transactionResult = tronJsonRpc.getTransactionByHash(
          ByteArray.toJsonHex(transactionCapsule1.getTransactionId().getBytes()));
    } catch (Exception e) {
      Assert.fail();
    }
    Assert.assertEquals(ByteArray.toJsonHex(transactionCapsule1.getBlockNum()),
        transactionResult.getBlockNumber());
  }

}