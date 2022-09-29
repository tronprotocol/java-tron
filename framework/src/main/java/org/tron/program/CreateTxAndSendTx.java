package org.tron.program;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI.BlockExtention;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.crypto.Hash;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.args.Args;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.AssetIssueContractOuterClass.TransferAssetContract;
import org.tron.protos.contract.BalanceContract.TransferContract;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;

@Slf4j
public class CreateTxAndSendTx {

  private static ExecutorService executorService;
  private static WalletGrpc.WalletBlockingStub blockingStubFull;
  private static WalletGrpc.WalletBlockingStub blockingStubFull2;

  public static void main(String[] args) {
    //read the parameter
//    String fullnode = args[0];
//    int threadNum = Integer.parseInt(args[1]);
    String fullnode = "47.253.205.239:50051";
    String fullnode2 = "47.253.167.85:50051";
    int threadNum = 100;
    executorService = Executors.newFixedThreadPool(threadNum);
    //construct grpc stub
    ManagedChannel channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    ManagedChannel channelFull2 = ManagedChannelBuilder.forTarget(fullnode2)
        .usePlaintext(true).build();
    blockingStubFull2 = WalletGrpc.newBlockingStub(channelFull2);
    CreateTx createTx = new CreateTx();
    createTx.createTRX(100000);
    System.exit(0);
  }

  private static void sendTx(List<Transaction> list) {
    List<Future<Boolean>> futureList = new ArrayList<>(list.size());
    int count = 0;
    for (Transaction transaction : list) {
      final int value = count;
      futureList.add(executorService.submit(() -> {
        if (value % 2 == 0) {
          blockingStubFull.broadcastTransaction(transaction);
        } else {
          blockingStubFull2.broadcastTransaction(transaction);
        }
        return true;
      }));
      ++count;
    }
//    list.forEach(transaction -> {
//      futureList.add(executorService.submit(() -> {
//        blockingStubFull.broadcastTransaction(transaction);
//        blockingStubFull2.broadcastTransaction(transaction);
//        return true;
//      }));
//    });
    futureList.forEach(ret -> {
      try {
        ret.get();
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    });
  }

  public static class CreateTx {

    private static String pk = "cc8516538984f13931fa2010898ac7983ff07cb57e1b4ac83cb95012f8204b4a";
    private static ByteString ownerAddress = ByteString.copyFrom(
        Hex.decode("417DDAC588B99E52B737AED2E44644D797E8661BE4"));
    private static ByteString toAddress = ByteString.copyFrom(
        Hex.decode("413AD3AD42C722E9FD297629EE4062025F7082A8D1"));
    private static ByteString contractAddress = ByteString.copyFrom(
        Hex.decode("413AD3AD42C722E9FD297629EE4062025F7082A8D1"));

    private void createTRX(int number) {
      List<Transaction> transactionList = Collections.synchronizedList(new ArrayList<>());
      List<Future<Boolean>> futureList = new ArrayList<>(number);
      BlockId blockId = getBlockId();
      for (int i = 1; i <= number; i++) {
        TransferContract transferContract = TransferContract.newBuilder()
            .setAmount(i)
            .setOwnerAddress(ownerAddress)
            .setToAddress(toAddress)
            .build();
        futureList.add(executorService.submit(() -> {
          transactionList.add(
              createTransaction(ContractType.TransferContract, blockId, transferContract));
          return true;
        }));
      }
      futureList.forEach(ret -> {
        try {
          ret.get();
        } catch (InterruptedException | ExecutionException e) {
          e.printStackTrace();
        }
      });
      System.out.println("一共创建 " + number + " 笔交易。");
      sendTx(transactionList);
      System.out.println("成功发送 " + number + " 笔交易。");
    }

    private BlockId getBlockId() {
      BlockExtention blockExtention = blockingStubFull.getNowBlock2(
          EmptyMessage.newBuilder().build());
      BlockId blockId = new BlockId(
          Sha256Hash.of(CommonParameter.getInstance().isECKeyCryptoEngine(),
              blockExtention.getBlockHeader().getRawData().toByteArray()),
          blockExtention.getBlockHeader().getRawData().getNumber());
      return blockId;
    }

    private Transaction createTransaction(ContractType contractType, BlockId blockId,
        Message contract) {
      Transaction transaction = Transaction.newBuilder().setRawData(Transaction.raw.newBuilder()
          .addContract(Contract.newBuilder().setType(contractType).setParameter(
              Any.pack(contract)).build()).build()).build();
      transaction = createTransaction(blockId, transaction);
      transaction = addTransactionSign(transaction, pk);
      return transaction;
    }

    private void create10(String tokenId, int number) {
      List<Transaction> transactionList = Collections.synchronizedList(new ArrayList<>());
      List<Future<Boolean>> futureList = new ArrayList<>(number);
      BlockId blockId = getBlockId();
      for (int i = 1; i <= number; i++) {
        TransferAssetContract transferAssetContract = TransferAssetContract.newBuilder()
            .setOwnerAddress(ownerAddress).setToAddress(toAddress).setAmount(1)
            .setAssetName(ByteString.copyFromUtf8(tokenId)).build();
        futureList.add(executorService.submit(() -> {
          transactionList.add(
              createTransaction(ContractType.TransferContract, blockId, transferAssetContract));
          return true;
        }));
      }
      futureList.forEach(ret -> {
        try {
          ret.get();
        } catch (InterruptedException | ExecutionException e) {
          e.printStackTrace();
        }
      });
      System.out.println("一共创建 " + number + " 笔交易。");
      sendTx(transactionList);
      System.out.println("成功发送 " + number + " 笔交易。");
    }

    private void create20(int number, String methodSign, String argsStr) {
      List<Transaction> transactionList = Collections.synchronizedList(new ArrayList<>());
      List<Future<Boolean>> futureList = new ArrayList<>(number);
      BlockId blockId = getBlockId();
      for (int i = 1; i <= number; i++) {
        byte[] input = Hex.decode(parseSelector(methodSign) + argsStr);
        TriggerSmartContract triggerSmartContract = TriggerSmartContract.newBuilder()
            .setOwnerAddress(ownerAddress).setContractAddress(contractAddress).setCallValue(i)
            .setData(ByteString.copyFrom(input)).setTokenId(0L).setCallTokenValue(0L).build();
        futureList.add(executorService.submit(() -> {
          transactionList.add(
              createTransaction(ContractType.TriggerSmartContract, blockId, triggerSmartContract));
          return true;
        }));
      }
      futureList.forEach(ret -> {
        try {
          ret.get();
        } catch (InterruptedException | ExecutionException e) {
          e.printStackTrace();
        }
      });
      System.out.println("一共创建 " + number + " 笔交易。");
      sendTx(transactionList);
      System.out.println("成功发送 " + number + " 笔交易。");
    }

    private Transaction createTransaction(BlockId blockId, Transaction transaction) {
      try {
        long expiration = System.currentTimeMillis() + Args.getInstance()
            .getTrxExpirationTimeInMilliseconds();
        byte[] refBlockNum = ByteArray.fromLong(blockId.getNum());
        Transaction.raw rawData = transaction.getRawData().toBuilder()
            .setRefBlockHash(ByteString.copyFrom(ByteArray.subArray(blockId.getBytes(), 8, 16)))
            .setRefBlockBytes(ByteString.copyFrom(ByteArray.subArray(refBlockNum, 6, 8)))
            .setExpiration(expiration)
            .setTimestamp(System.currentTimeMillis())
            .build();
        return transaction.toBuilder().setRawData(rawData).build();
      } catch (Exception e) {
        e.printStackTrace();
      }
      return null;
    }

    public static Transaction addTransactionSign(Transaction transaction, String priKey) {
      ECKey ecKey = null;
      try {
        ecKey = ECKey.fromPrivate(new BigInteger(priKey, 16));
      } catch (Exception ex) {
        ex.printStackTrace();
      }

      Transaction.Builder transactionBuilderSigned = transaction.toBuilder();
      byte[] hash = Sha256Hash.hash(CommonParameter.getInstance().isECKeyCryptoEngine(),
          transaction.getRawData().toByteArray());

      ECDSASignature signature = ecKey.sign(hash);
      ByteString bsSign = ByteString.copyFrom(signature.toByteArray());
      transactionBuilderSigned.addSignature(bsSign);
      transaction = transactionBuilderSigned.build();
      return transaction;
    }

    public static String parseSelector(String methodSign) {
      byte[] selector = new byte[4];
      System.arraycopy(Hash.sha3(methodSign.getBytes()), 0, selector, 0, 4);
      return Hex.toHexString(selector);
    }

  }
}
