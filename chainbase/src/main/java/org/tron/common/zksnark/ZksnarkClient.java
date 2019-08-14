package org.tron.common.zksnark;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannelBuilder;
import org.tron.api.TronZksnarkGrpc;
import org.tron.api.ZksnarkGrpcAPI.ZksnarkRequest;
import org.tron.api.ZksnarkGrpcAPI.ZksnarkResponse.Code;
import org.tron.protos.Protocol.Transaction;

public class ZksnarkClient {

  public static final ZksnarkClient instance = new ZksnarkClient();

  private TronZksnarkGrpc.TronZksnarkBlockingStub blockingStub;

  public ZksnarkClient() {
    blockingStub = TronZksnarkGrpc.newBlockingStub(ManagedChannelBuilder
        .forTarget("127.0.0.1:60051")
        .usePlaintext()
        .build());
  }

  public boolean checkZksnarkProof(String txId, Transaction transaction, byte[] sighash, long valueBalance) {
    ZksnarkRequest request = ZksnarkRequest.newBuilder()
        .setTransaction(transaction)
        .setTxId(txId)
        .setSighash(ByteString.copyFrom(sighash))
        .setValueBalance(valueBalance)
        .build();
    return blockingStub.checkZksnarkProof(request).getCode() == Code.SUCCESS;
  }

  public static ZksnarkClient getInstance() {
    return instance;
  }
}
