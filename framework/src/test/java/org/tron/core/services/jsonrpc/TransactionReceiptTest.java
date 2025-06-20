package org.tron.core.services.jsonrpc;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.ByteString;
import java.util.List;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.tron.api.GrpcAPI.TransactionInfoList;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionRetCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.JsonRpcInternalException;
import org.tron.core.services.jsonrpc.types.TransactionReceipt;
import org.tron.core.services.jsonrpc.types.TransactionReceiptFactory;
import org.tron.core.store.TransactionRetStore;
import org.tron.protos.Protocol;

public class TransactionReceiptTest extends BaseTest {

  @Resource
  private Wallet wallet;

  @Resource
  private TransactionRetStore transactionRetStore;

  static {
    Args.setParam(new String[]{"-d", dbPath()}, Constant.TEST_CONF);
  }

  @Test
  public void testTransactionReceipt() throws JsonRpcInternalException {
    Protocol.TransactionInfo transactionInfo = Protocol.TransactionInfo.newBuilder()
        .setId(ByteString.copyFrom("1".getBytes()))
        .setContractAddress(ByteString.copyFrom("address1".getBytes()))
        .setReceipt(Protocol.ResourceReceipt.newBuilder()
            .setEnergyUsageTotal(0L)
            .setResult(Protocol.Transaction.Result.contractResult.DEFAULT)
            .build())
        .addLog(Protocol.TransactionInfo.Log.newBuilder()
            .setAddress(ByteString.copyFrom("address1".getBytes()))
            .setData(ByteString.copyFrom("data".getBytes()))
            .build())
        .build();
    TransactionRetCapsule transactionRetCapsule = new TransactionRetCapsule();
    transactionRetCapsule.addTransactionInfo(transactionInfo);
    transactionRetStore.put(ByteArray.fromLong(1), transactionRetCapsule);

    Protocol.Transaction.raw.Builder raw = Protocol.Transaction.raw.newBuilder();
    Protocol.Transaction.Contract.Builder contract = Protocol.Transaction.Contract.newBuilder();
    contract.setType(Protocol.Transaction.Contract.ContractType.UpdateBrokerageContract);
    raw.addContract(contract.build());
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().setRawData(raw).build();

    Protocol.Block block = Protocol.Block.newBuilder().setBlockHeader(
        Protocol.BlockHeader.newBuilder().setRawData(
            Protocol.BlockHeader.raw.newBuilder().setNumber(1))).addTransactions(
        transaction).build();

    BlockCapsule blockCapsule = new BlockCapsule(block);
    long blockNum = blockCapsule.getNum();
    TransactionInfoList infoList = wallet.getTransactionInfoByBlockNum(blockNum);
    long energyFee = wallet.getEnergyFee(blockCapsule.getTimeStamp());

    TransactionReceipt transactionReceipt = TransactionReceiptFactory.createFromBlockAndTxInfo(
        blockCapsule, transactionInfo, infoList, energyFee);

    Assert.assertNotNull(transactionReceipt);
    Assert.assertEquals(transactionReceipt.getBlockNumber(),"0x1");
    Assert.assertEquals(transactionReceipt.getTransactionIndex(),"0x0");
    Assert.assertEquals(transactionReceipt.getLogs().length,1);
    Assert.assertEquals(transactionReceipt.getBlockHash(),
        "0x0000000000000001464f071c8a336fd22eb5145dff1b245bda013ec89add8497");


    List<TransactionReceipt> transactionReceiptList = TransactionReceiptFactory.createFromBlock(
        blockCapsule, infoList, energyFee);

    Assert.assertNotNull(transactionReceiptList);
    Assert.assertEquals(transactionReceiptList.size(), 1);
    Assert.assertEquals(
        JSON.toJSONString(transactionReceiptList.get(0)), JSON.toJSONString(transactionReceipt));
  }

}
