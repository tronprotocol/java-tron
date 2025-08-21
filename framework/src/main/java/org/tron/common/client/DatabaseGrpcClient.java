package org.tron.common.client;

import io.grpc.LoadBalancerRegistry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.internal.PickFirstLoadBalancerProvider;
import org.tron.api.DatabaseGrpc;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.DynamicProperties;

public class DatabaseGrpcClient {

  private final ManagedChannel channel;
  private final DatabaseGrpc.DatabaseBlockingStub databaseBlockingStub;

  static {
    LoadBalancerRegistry
        .getDefaultRegistry()
        .register(new PickFirstLoadBalancerProvider());
  }

  public DatabaseGrpcClient(String host, int port) {
    channel = ManagedChannelBuilder.forAddress(host, port)
        .usePlaintext()
        .build();
    databaseBlockingStub = DatabaseGrpc.newBlockingStub(channel);
  }

  public DatabaseGrpcClient(String host) {
    channel = ManagedChannelBuilder.forTarget(host)
        .usePlaintext()
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
