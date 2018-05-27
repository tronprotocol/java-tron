package org.tron.common.overlay.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.tron.api.DatabaseGrpc;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.DynamicProperties;

public class DatabaseGrpcClient {

  private final ManagedChannel channel;
  private final DatabaseGrpc.DatabaseBlockingStub databaseBlockingStub;

  public DatabaseGrpcClient(String host, int port) {
    channel = ManagedChannelBuilder.forAddress(host, port)
        .usePlaintext(true)
        .build();
    databaseBlockingStub = DatabaseGrpc.newBlockingStub(channel);
  }

  public DatabaseGrpcClient(String host) {
    channel = ManagedChannelBuilder.forTarget(host)
        .usePlaintext(true)
        .build();
    databaseBlockingStub = DatabaseGrpc.newBlockingStub(channel);
  }


  public Block getBlock(long blockNum) {
    if (blockNum < 0) {
      return databaseBlockingStub.getNowBlock(EmptyMessage.newBuilder().build());
    }
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    return databaseBlockingStub.getBlockByNum(builder.build());
  }

  public void shutdown() {
    channel.shutdown();
  }

  public DynamicProperties getDynamicProperties() {
    return databaseBlockingStub.getDynamicProperties(EmptyMessage.newBuilder().build());
  }
}
