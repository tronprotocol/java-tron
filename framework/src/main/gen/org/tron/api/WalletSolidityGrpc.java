package org.tron.api;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.9.0)",
    comments = "Source: api/api.proto")
public final class WalletSolidityGrpc {

  private WalletSolidityGrpc() {}

  public static final String SERVICE_NAME = "protocol.WalletSolidity";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetAccountMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Protocol.Account,
      org.tron.protos.Protocol.Account> METHOD_GET_ACCOUNT = getGetAccountMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Protocol.Account,
      org.tron.protos.Protocol.Account> getGetAccountMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Protocol.Account,
      org.tron.protos.Protocol.Account> getGetAccountMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Protocol.Account, org.tron.protos.Protocol.Account> getGetAccountMethod;
    if ((getGetAccountMethod = WalletSolidityGrpc.getGetAccountMethod) == null) {
      synchronized (WalletSolidityGrpc.class) {
        if ((getGetAccountMethod = WalletSolidityGrpc.getGetAccountMethod) == null) {
          WalletSolidityGrpc.getGetAccountMethod = getGetAccountMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Protocol.Account, org.tron.protos.Protocol.Account>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletSolidity", "GetAccount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Account.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Account.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletSolidityMethodDescriptorSupplier("GetAccount"))
                  .build();
          }
        }
     }
     return getGetAccountMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetAccountByIdMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Protocol.Account,
      org.tron.protos.Protocol.Account> METHOD_GET_ACCOUNT_BY_ID = getGetAccountByIdMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Protocol.Account,
      org.tron.protos.Protocol.Account> getGetAccountByIdMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Protocol.Account,
      org.tron.protos.Protocol.Account> getGetAccountByIdMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Protocol.Account, org.tron.protos.Protocol.Account> getGetAccountByIdMethod;
    if ((getGetAccountByIdMethod = WalletSolidityGrpc.getGetAccountByIdMethod) == null) {
      synchronized (WalletSolidityGrpc.class) {
        if ((getGetAccountByIdMethod = WalletSolidityGrpc.getGetAccountByIdMethod) == null) {
          WalletSolidityGrpc.getGetAccountByIdMethod = getGetAccountByIdMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Protocol.Account, org.tron.protos.Protocol.Account>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletSolidity", "GetAccountById"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Account.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Account.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletSolidityMethodDescriptorSupplier("GetAccountById"))
                  .build();
          }
        }
     }
     return getGetAccountByIdMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getListWitnessesMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.api.GrpcAPI.WitnessList> METHOD_LIST_WITNESSES = getListWitnessesMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.api.GrpcAPI.WitnessList> getListWitnessesMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.api.GrpcAPI.WitnessList> getListWitnessesMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage, org.tron.api.GrpcAPI.WitnessList> getListWitnessesMethod;
    if ((getListWitnessesMethod = WalletSolidityGrpc.getListWitnessesMethod) == null) {
      synchronized (WalletSolidityGrpc.class) {
        if ((getListWitnessesMethod = WalletSolidityGrpc.getListWitnessesMethod) == null) {
          WalletSolidityGrpc.getListWitnessesMethod = getListWitnessesMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.EmptyMessage, org.tron.api.GrpcAPI.WitnessList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletSolidity", "ListWitnesses"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.WitnessList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletSolidityMethodDescriptorSupplier("ListWitnesses"))
                  .build();
          }
        }
     }
     return getListWitnessesMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetAssetIssueListMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.api.GrpcAPI.AssetIssueList> METHOD_GET_ASSET_ISSUE_LIST = getGetAssetIssueListMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.api.GrpcAPI.AssetIssueList> getGetAssetIssueListMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.api.GrpcAPI.AssetIssueList> getGetAssetIssueListMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage, org.tron.api.GrpcAPI.AssetIssueList> getGetAssetIssueListMethod;
    if ((getGetAssetIssueListMethod = WalletSolidityGrpc.getGetAssetIssueListMethod) == null) {
      synchronized (WalletSolidityGrpc.class) {
        if ((getGetAssetIssueListMethod = WalletSolidityGrpc.getGetAssetIssueListMethod) == null) {
          WalletSolidityGrpc.getGetAssetIssueListMethod = getGetAssetIssueListMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.EmptyMessage, org.tron.api.GrpcAPI.AssetIssueList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletSolidity", "GetAssetIssueList"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.AssetIssueList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletSolidityMethodDescriptorSupplier("GetAssetIssueList"))
                  .build();
          }
        }
     }
     return getGetAssetIssueListMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetPaginatedAssetIssueListMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.PaginatedMessage,
      org.tron.api.GrpcAPI.AssetIssueList> METHOD_GET_PAGINATED_ASSET_ISSUE_LIST = getGetPaginatedAssetIssueListMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.PaginatedMessage,
      org.tron.api.GrpcAPI.AssetIssueList> getGetPaginatedAssetIssueListMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.PaginatedMessage,
      org.tron.api.GrpcAPI.AssetIssueList> getGetPaginatedAssetIssueListMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.PaginatedMessage, org.tron.api.GrpcAPI.AssetIssueList> getGetPaginatedAssetIssueListMethod;
    if ((getGetPaginatedAssetIssueListMethod = WalletSolidityGrpc.getGetPaginatedAssetIssueListMethod) == null) {
      synchronized (WalletSolidityGrpc.class) {
        if ((getGetPaginatedAssetIssueListMethod = WalletSolidityGrpc.getGetPaginatedAssetIssueListMethod) == null) {
          WalletSolidityGrpc.getGetPaginatedAssetIssueListMethod = getGetPaginatedAssetIssueListMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.PaginatedMessage, org.tron.api.GrpcAPI.AssetIssueList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletSolidity", "GetPaginatedAssetIssueList"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.PaginatedMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.AssetIssueList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletSolidityMethodDescriptorSupplier("GetPaginatedAssetIssueList"))
                  .build();
          }
        }
     }
     return getGetPaginatedAssetIssueListMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetAssetIssueByNameMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.protos.Contract.AssetIssueContract> METHOD_GET_ASSET_ISSUE_BY_NAME = getGetAssetIssueByNameMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.protos.Contract.AssetIssueContract> getGetAssetIssueByNameMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.protos.Contract.AssetIssueContract> getGetAssetIssueByNameMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage, org.tron.protos.Contract.AssetIssueContract> getGetAssetIssueByNameMethod;
    if ((getGetAssetIssueByNameMethod = WalletSolidityGrpc.getGetAssetIssueByNameMethod) == null) {
      synchronized (WalletSolidityGrpc.class) {
        if ((getGetAssetIssueByNameMethod = WalletSolidityGrpc.getGetAssetIssueByNameMethod) == null) {
          WalletSolidityGrpc.getGetAssetIssueByNameMethod = getGetAssetIssueByNameMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.BytesMessage, org.tron.protos.Contract.AssetIssueContract>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletSolidity", "GetAssetIssueByName"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.AssetIssueContract.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletSolidityMethodDescriptorSupplier("GetAssetIssueByName"))
                  .build();
          }
        }
     }
     return getGetAssetIssueByNameMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetAssetIssueListByNameMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.api.GrpcAPI.AssetIssueList> METHOD_GET_ASSET_ISSUE_LIST_BY_NAME = getGetAssetIssueListByNameMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.api.GrpcAPI.AssetIssueList> getGetAssetIssueListByNameMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.api.GrpcAPI.AssetIssueList> getGetAssetIssueListByNameMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage, org.tron.api.GrpcAPI.AssetIssueList> getGetAssetIssueListByNameMethod;
    if ((getGetAssetIssueListByNameMethod = WalletSolidityGrpc.getGetAssetIssueListByNameMethod) == null) {
      synchronized (WalletSolidityGrpc.class) {
        if ((getGetAssetIssueListByNameMethod = WalletSolidityGrpc.getGetAssetIssueListByNameMethod) == null) {
          WalletSolidityGrpc.getGetAssetIssueListByNameMethod = getGetAssetIssueListByNameMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.BytesMessage, org.tron.api.GrpcAPI.AssetIssueList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletSolidity", "GetAssetIssueListByName"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.AssetIssueList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletSolidityMethodDescriptorSupplier("GetAssetIssueListByName"))
                  .build();
          }
        }
     }
     return getGetAssetIssueListByNameMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetAssetIssueByIdMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.protos.Contract.AssetIssueContract> METHOD_GET_ASSET_ISSUE_BY_ID = getGetAssetIssueByIdMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.protos.Contract.AssetIssueContract> getGetAssetIssueByIdMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.protos.Contract.AssetIssueContract> getGetAssetIssueByIdMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage, org.tron.protos.Contract.AssetIssueContract> getGetAssetIssueByIdMethod;
    if ((getGetAssetIssueByIdMethod = WalletSolidityGrpc.getGetAssetIssueByIdMethod) == null) {
      synchronized (WalletSolidityGrpc.class) {
        if ((getGetAssetIssueByIdMethod = WalletSolidityGrpc.getGetAssetIssueByIdMethod) == null) {
          WalletSolidityGrpc.getGetAssetIssueByIdMethod = getGetAssetIssueByIdMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.BytesMessage, org.tron.protos.Contract.AssetIssueContract>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletSolidity", "GetAssetIssueById"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.AssetIssueContract.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletSolidityMethodDescriptorSupplier("GetAssetIssueById"))
                  .build();
          }
        }
     }
     return getGetAssetIssueByIdMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetNowBlockMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.protos.Protocol.Block> METHOD_GET_NOW_BLOCK = getGetNowBlockMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.protos.Protocol.Block> getGetNowBlockMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.protos.Protocol.Block> getGetNowBlockMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage, org.tron.protos.Protocol.Block> getGetNowBlockMethod;
    if ((getGetNowBlockMethod = WalletSolidityGrpc.getGetNowBlockMethod) == null) {
      synchronized (WalletSolidityGrpc.class) {
        if ((getGetNowBlockMethod = WalletSolidityGrpc.getGetNowBlockMethod) == null) {
          WalletSolidityGrpc.getGetNowBlockMethod = getGetNowBlockMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.EmptyMessage, org.tron.protos.Protocol.Block>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletSolidity", "GetNowBlock"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Block.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletSolidityMethodDescriptorSupplier("GetNowBlock"))
                  .build();
          }
        }
     }
     return getGetNowBlockMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetNowBlock2Method()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.api.GrpcAPI.BlockExtention> METHOD_GET_NOW_BLOCK2 = getGetNowBlock2Method();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.api.GrpcAPI.BlockExtention> getGetNowBlock2Method;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.api.GrpcAPI.BlockExtention> getGetNowBlock2Method() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage, org.tron.api.GrpcAPI.BlockExtention> getGetNowBlock2Method;
    if ((getGetNowBlock2Method = WalletSolidityGrpc.getGetNowBlock2Method) == null) {
      synchronized (WalletSolidityGrpc.class) {
        if ((getGetNowBlock2Method = WalletSolidityGrpc.getGetNowBlock2Method) == null) {
          WalletSolidityGrpc.getGetNowBlock2Method = getGetNowBlock2Method = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.EmptyMessage, org.tron.api.GrpcAPI.BlockExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletSolidity", "GetNowBlock2"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BlockExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletSolidityMethodDescriptorSupplier("GetNowBlock2"))
                  .build();
          }
        }
     }
     return getGetNowBlock2Method;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetBlockByNumMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.NumberMessage,
      org.tron.protos.Protocol.Block> METHOD_GET_BLOCK_BY_NUM = getGetBlockByNumMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.NumberMessage,
      org.tron.protos.Protocol.Block> getGetBlockByNumMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.NumberMessage,
      org.tron.protos.Protocol.Block> getGetBlockByNumMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.NumberMessage, org.tron.protos.Protocol.Block> getGetBlockByNumMethod;
    if ((getGetBlockByNumMethod = WalletSolidityGrpc.getGetBlockByNumMethod) == null) {
      synchronized (WalletSolidityGrpc.class) {
        if ((getGetBlockByNumMethod = WalletSolidityGrpc.getGetBlockByNumMethod) == null) {
          WalletSolidityGrpc.getGetBlockByNumMethod = getGetBlockByNumMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.NumberMessage, org.tron.protos.Protocol.Block>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletSolidity", "GetBlockByNum"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.NumberMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Block.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletSolidityMethodDescriptorSupplier("GetBlockByNum"))
                  .build();
          }
        }
     }
     return getGetBlockByNumMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetBlockByNum2Method()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.NumberMessage,
      org.tron.api.GrpcAPI.BlockExtention> METHOD_GET_BLOCK_BY_NUM2 = getGetBlockByNum2Method();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.NumberMessage,
      org.tron.api.GrpcAPI.BlockExtention> getGetBlockByNum2Method;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.NumberMessage,
      org.tron.api.GrpcAPI.BlockExtention> getGetBlockByNum2Method() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.NumberMessage, org.tron.api.GrpcAPI.BlockExtention> getGetBlockByNum2Method;
    if ((getGetBlockByNum2Method = WalletSolidityGrpc.getGetBlockByNum2Method) == null) {
      synchronized (WalletSolidityGrpc.class) {
        if ((getGetBlockByNum2Method = WalletSolidityGrpc.getGetBlockByNum2Method) == null) {
          WalletSolidityGrpc.getGetBlockByNum2Method = getGetBlockByNum2Method = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.NumberMessage, org.tron.api.GrpcAPI.BlockExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletSolidity", "GetBlockByNum2"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.NumberMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BlockExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletSolidityMethodDescriptorSupplier("GetBlockByNum2"))
                  .build();
          }
        }
     }
     return getGetBlockByNum2Method;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetTransactionCountByBlockNumMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.NumberMessage,
      org.tron.api.GrpcAPI.NumberMessage> METHOD_GET_TRANSACTION_COUNT_BY_BLOCK_NUM = getGetTransactionCountByBlockNumMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.NumberMessage,
      org.tron.api.GrpcAPI.NumberMessage> getGetTransactionCountByBlockNumMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.NumberMessage,
      org.tron.api.GrpcAPI.NumberMessage> getGetTransactionCountByBlockNumMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.NumberMessage, org.tron.api.GrpcAPI.NumberMessage> getGetTransactionCountByBlockNumMethod;
    if ((getGetTransactionCountByBlockNumMethod = WalletSolidityGrpc.getGetTransactionCountByBlockNumMethod) == null) {
      synchronized (WalletSolidityGrpc.class) {
        if ((getGetTransactionCountByBlockNumMethod = WalletSolidityGrpc.getGetTransactionCountByBlockNumMethod) == null) {
          WalletSolidityGrpc.getGetTransactionCountByBlockNumMethod = getGetTransactionCountByBlockNumMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.NumberMessage, org.tron.api.GrpcAPI.NumberMessage>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletSolidity", "GetTransactionCountByBlockNum"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.NumberMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.NumberMessage.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletSolidityMethodDescriptorSupplier("GetTransactionCountByBlockNum"))
                  .build();
          }
        }
     }
     return getGetTransactionCountByBlockNumMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetDelegatedResourceMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.DelegatedResourceMessage,
      org.tron.api.GrpcAPI.DelegatedResourceList> METHOD_GET_DELEGATED_RESOURCE = getGetDelegatedResourceMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.DelegatedResourceMessage,
      org.tron.api.GrpcAPI.DelegatedResourceList> getGetDelegatedResourceMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.DelegatedResourceMessage,
      org.tron.api.GrpcAPI.DelegatedResourceList> getGetDelegatedResourceMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.DelegatedResourceMessage, org.tron.api.GrpcAPI.DelegatedResourceList> getGetDelegatedResourceMethod;
    if ((getGetDelegatedResourceMethod = WalletSolidityGrpc.getGetDelegatedResourceMethod) == null) {
      synchronized (WalletSolidityGrpc.class) {
        if ((getGetDelegatedResourceMethod = WalletSolidityGrpc.getGetDelegatedResourceMethod) == null) {
          WalletSolidityGrpc.getGetDelegatedResourceMethod = getGetDelegatedResourceMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.DelegatedResourceMessage, org.tron.api.GrpcAPI.DelegatedResourceList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletSolidity", "GetDelegatedResource"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.DelegatedResourceMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.DelegatedResourceList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletSolidityMethodDescriptorSupplier("GetDelegatedResource"))
                  .build();
          }
        }
     }
     return getGetDelegatedResourceMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetDelegatedResourceAccountIndexMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.protos.Protocol.DelegatedResourceAccountIndex> METHOD_GET_DELEGATED_RESOURCE_ACCOUNT_INDEX = getGetDelegatedResourceAccountIndexMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.protos.Protocol.DelegatedResourceAccountIndex> getGetDelegatedResourceAccountIndexMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.protos.Protocol.DelegatedResourceAccountIndex> getGetDelegatedResourceAccountIndexMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage, org.tron.protos.Protocol.DelegatedResourceAccountIndex> getGetDelegatedResourceAccountIndexMethod;
    if ((getGetDelegatedResourceAccountIndexMethod = WalletSolidityGrpc.getGetDelegatedResourceAccountIndexMethod) == null) {
      synchronized (WalletSolidityGrpc.class) {
        if ((getGetDelegatedResourceAccountIndexMethod = WalletSolidityGrpc.getGetDelegatedResourceAccountIndexMethod) == null) {
          WalletSolidityGrpc.getGetDelegatedResourceAccountIndexMethod = getGetDelegatedResourceAccountIndexMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.BytesMessage, org.tron.protos.Protocol.DelegatedResourceAccountIndex>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletSolidity", "GetDelegatedResourceAccountIndex"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.DelegatedResourceAccountIndex.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletSolidityMethodDescriptorSupplier("GetDelegatedResourceAccountIndex"))
                  .build();
          }
        }
     }
     return getGetDelegatedResourceAccountIndexMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetExchangeByIdMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.protos.Protocol.Exchange> METHOD_GET_EXCHANGE_BY_ID = getGetExchangeByIdMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.protos.Protocol.Exchange> getGetExchangeByIdMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.protos.Protocol.Exchange> getGetExchangeByIdMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage, org.tron.protos.Protocol.Exchange> getGetExchangeByIdMethod;
    if ((getGetExchangeByIdMethod = WalletSolidityGrpc.getGetExchangeByIdMethod) == null) {
      synchronized (WalletSolidityGrpc.class) {
        if ((getGetExchangeByIdMethod = WalletSolidityGrpc.getGetExchangeByIdMethod) == null) {
          WalletSolidityGrpc.getGetExchangeByIdMethod = getGetExchangeByIdMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.BytesMessage, org.tron.protos.Protocol.Exchange>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletSolidity", "GetExchangeById"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Exchange.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletSolidityMethodDescriptorSupplier("GetExchangeById"))
                  .build();
          }
        }
     }
     return getGetExchangeByIdMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getListExchangesMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.api.GrpcAPI.ExchangeList> METHOD_LIST_EXCHANGES = getListExchangesMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.api.GrpcAPI.ExchangeList> getListExchangesMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.api.GrpcAPI.ExchangeList> getListExchangesMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage, org.tron.api.GrpcAPI.ExchangeList> getListExchangesMethod;
    if ((getListExchangesMethod = WalletSolidityGrpc.getListExchangesMethod) == null) {
      synchronized (WalletSolidityGrpc.class) {
        if ((getListExchangesMethod = WalletSolidityGrpc.getListExchangesMethod) == null) {
          WalletSolidityGrpc.getListExchangesMethod = getListExchangesMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.EmptyMessage, org.tron.api.GrpcAPI.ExchangeList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletSolidity", "ListExchanges"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.ExchangeList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletSolidityMethodDescriptorSupplier("ListExchanges"))
                  .build();
          }
        }
     }
     return getListExchangesMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetTransactionByIdMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.protos.Protocol.Transaction> METHOD_GET_TRANSACTION_BY_ID = getGetTransactionByIdMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.protos.Protocol.Transaction> getGetTransactionByIdMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.protos.Protocol.Transaction> getGetTransactionByIdMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage, org.tron.protos.Protocol.Transaction> getGetTransactionByIdMethod;
    if ((getGetTransactionByIdMethod = WalletSolidityGrpc.getGetTransactionByIdMethod) == null) {
      synchronized (WalletSolidityGrpc.class) {
        if ((getGetTransactionByIdMethod = WalletSolidityGrpc.getGetTransactionByIdMethod) == null) {
          WalletSolidityGrpc.getGetTransactionByIdMethod = getGetTransactionByIdMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.BytesMessage, org.tron.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletSolidity", "GetTransactionById"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletSolidityMethodDescriptorSupplier("GetTransactionById"))
                  .build();
          }
        }
     }
     return getGetTransactionByIdMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetTransactionInfoByIdMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.protos.Protocol.TransactionInfo> METHOD_GET_TRANSACTION_INFO_BY_ID = getGetTransactionInfoByIdMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.protos.Protocol.TransactionInfo> getGetTransactionInfoByIdMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.protos.Protocol.TransactionInfo> getGetTransactionInfoByIdMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage, org.tron.protos.Protocol.TransactionInfo> getGetTransactionInfoByIdMethod;
    if ((getGetTransactionInfoByIdMethod = WalletSolidityGrpc.getGetTransactionInfoByIdMethod) == null) {
      synchronized (WalletSolidityGrpc.class) {
        if ((getGetTransactionInfoByIdMethod = WalletSolidityGrpc.getGetTransactionInfoByIdMethod) == null) {
          WalletSolidityGrpc.getGetTransactionInfoByIdMethod = getGetTransactionInfoByIdMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.BytesMessage, org.tron.protos.Protocol.TransactionInfo>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletSolidity", "GetTransactionInfoById"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.TransactionInfo.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletSolidityMethodDescriptorSupplier("GetTransactionInfoById"))
                  .build();
          }
        }
     }
     return getGetTransactionInfoByIdMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGenerateAddressMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.api.GrpcAPI.AddressPrKeyPairMessage> METHOD_GENERATE_ADDRESS = getGenerateAddressMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.api.GrpcAPI.AddressPrKeyPairMessage> getGenerateAddressMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.api.GrpcAPI.AddressPrKeyPairMessage> getGenerateAddressMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage, org.tron.api.GrpcAPI.AddressPrKeyPairMessage> getGenerateAddressMethod;
    if ((getGenerateAddressMethod = WalletSolidityGrpc.getGenerateAddressMethod) == null) {
      synchronized (WalletSolidityGrpc.class) {
        if ((getGenerateAddressMethod = WalletSolidityGrpc.getGenerateAddressMethod) == null) {
          WalletSolidityGrpc.getGenerateAddressMethod = getGenerateAddressMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.EmptyMessage, org.tron.api.GrpcAPI.AddressPrKeyPairMessage>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletSolidity", "GenerateAddress"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.AddressPrKeyPairMessage.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletSolidityMethodDescriptorSupplier("GenerateAddress"))
                  .build();
          }
        }
     }
     return getGenerateAddressMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static WalletSolidityStub newStub(io.grpc.Channel channel) {
    return new WalletSolidityStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static WalletSolidityBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new WalletSolidityBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static WalletSolidityFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new WalletSolidityFutureStub(channel);
  }

  /**
   */
  public static abstract class WalletSolidityImplBase implements io.grpc.BindableService {

    /**
     */
    public void getAccount(org.tron.protos.Protocol.Account request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Account> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAccountMethod(), responseObserver);
    }

    /**
     */
    public void getAccountById(org.tron.protos.Protocol.Account request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Account> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAccountByIdMethod(), responseObserver);
    }

    /**
     */
    public void listWitnesses(org.tron.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.WitnessList> responseObserver) {
      asyncUnimplementedUnaryCall(getListWitnessesMethod(), responseObserver);
    }

    /**
     */
    public void getAssetIssueList(org.tron.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.AssetIssueList> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAssetIssueListMethod(), responseObserver);
    }

    /**
     */
    public void getPaginatedAssetIssueList(org.tron.api.GrpcAPI.PaginatedMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.AssetIssueList> responseObserver) {
      asyncUnimplementedUnaryCall(getGetPaginatedAssetIssueListMethod(), responseObserver);
    }

    /**
     */
    public void getAssetIssueByName(org.tron.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Contract.AssetIssueContract> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAssetIssueByNameMethod(), responseObserver);
    }

    /**
     */
    public void getAssetIssueListByName(org.tron.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.AssetIssueList> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAssetIssueListByNameMethod(), responseObserver);
    }

    /**
     */
    public void getAssetIssueById(org.tron.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Contract.AssetIssueContract> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAssetIssueByIdMethod(), responseObserver);
    }

    /**
     * <pre>
     *Please use GetNowBlock2 instead of this function.
     * </pre>
     */
    public void getNowBlock(org.tron.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Block> responseObserver) {
      asyncUnimplementedUnaryCall(getGetNowBlockMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of GetNowBlock.
     * </pre>
     */
    public void getNowBlock2(org.tron.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.BlockExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getGetNowBlock2Method(), responseObserver);
    }

    /**
     * <pre>
     *Please use GetBlockByNum2 instead of this function.
     * </pre>
     */
    public void getBlockByNum(org.tron.api.GrpcAPI.NumberMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Block> responseObserver) {
      asyncUnimplementedUnaryCall(getGetBlockByNumMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of GetBlockByNum.
     * </pre>
     */
    public void getBlockByNum2(org.tron.api.GrpcAPI.NumberMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.BlockExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getGetBlockByNum2Method(), responseObserver);
    }

    /**
     */
    public void getTransactionCountByBlockNum(org.tron.api.GrpcAPI.NumberMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.NumberMessage> responseObserver) {
      asyncUnimplementedUnaryCall(getGetTransactionCountByBlockNumMethod(), responseObserver);
    }

    /**
     */
    public void getDelegatedResource(org.tron.api.GrpcAPI.DelegatedResourceMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.DelegatedResourceList> responseObserver) {
      asyncUnimplementedUnaryCall(getGetDelegatedResourceMethod(), responseObserver);
    }

    /**
     */
    public void getDelegatedResourceAccountIndex(org.tron.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.DelegatedResourceAccountIndex> responseObserver) {
      asyncUnimplementedUnaryCall(getGetDelegatedResourceAccountIndexMethod(), responseObserver);
    }

    /**
     */
    public void getExchangeById(org.tron.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Exchange> responseObserver) {
      asyncUnimplementedUnaryCall(getGetExchangeByIdMethod(), responseObserver);
    }

    /**
     */
    public void listExchanges(org.tron.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.ExchangeList> responseObserver) {
      asyncUnimplementedUnaryCall(getListExchangesMethod(), responseObserver);
    }

    /**
     */
    public void getTransactionById(org.tron.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getGetTransactionByIdMethod(), responseObserver);
    }

    /**
     */
    public void getTransactionInfoById(org.tron.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.TransactionInfo> responseObserver) {
      asyncUnimplementedUnaryCall(getGetTransactionInfoByIdMethod(), responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public void generateAddress(org.tron.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.AddressPrKeyPairMessage> responseObserver) {
      asyncUnimplementedUnaryCall(getGenerateAddressMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getGetAccountMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Protocol.Account,
                org.tron.protos.Protocol.Account>(
                  this, METHODID_GET_ACCOUNT)))
          .addMethod(
            getGetAccountByIdMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Protocol.Account,
                org.tron.protos.Protocol.Account>(
                  this, METHODID_GET_ACCOUNT_BY_ID)))
          .addMethod(
            getListWitnessesMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.EmptyMessage,
                org.tron.api.GrpcAPI.WitnessList>(
                  this, METHODID_LIST_WITNESSES)))
          .addMethod(
            getGetAssetIssueListMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.EmptyMessage,
                org.tron.api.GrpcAPI.AssetIssueList>(
                  this, METHODID_GET_ASSET_ISSUE_LIST)))
          .addMethod(
            getGetPaginatedAssetIssueListMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.PaginatedMessage,
                org.tron.api.GrpcAPI.AssetIssueList>(
                  this, METHODID_GET_PAGINATED_ASSET_ISSUE_LIST)))
          .addMethod(
            getGetAssetIssueByNameMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.BytesMessage,
                org.tron.protos.Contract.AssetIssueContract>(
                  this, METHODID_GET_ASSET_ISSUE_BY_NAME)))
          .addMethod(
            getGetAssetIssueListByNameMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.BytesMessage,
                org.tron.api.GrpcAPI.AssetIssueList>(
                  this, METHODID_GET_ASSET_ISSUE_LIST_BY_NAME)))
          .addMethod(
            getGetAssetIssueByIdMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.BytesMessage,
                org.tron.protos.Contract.AssetIssueContract>(
                  this, METHODID_GET_ASSET_ISSUE_BY_ID)))
          .addMethod(
            getGetNowBlockMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.EmptyMessage,
                org.tron.protos.Protocol.Block>(
                  this, METHODID_GET_NOW_BLOCK)))
          .addMethod(
            getGetNowBlock2Method(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.EmptyMessage,
                org.tron.api.GrpcAPI.BlockExtention>(
                  this, METHODID_GET_NOW_BLOCK2)))
          .addMethod(
            getGetBlockByNumMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.NumberMessage,
                org.tron.protos.Protocol.Block>(
                  this, METHODID_GET_BLOCK_BY_NUM)))
          .addMethod(
            getGetBlockByNum2Method(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.NumberMessage,
                org.tron.api.GrpcAPI.BlockExtention>(
                  this, METHODID_GET_BLOCK_BY_NUM2)))
          .addMethod(
            getGetTransactionCountByBlockNumMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.NumberMessage,
                org.tron.api.GrpcAPI.NumberMessage>(
                  this, METHODID_GET_TRANSACTION_COUNT_BY_BLOCK_NUM)))
          .addMethod(
            getGetDelegatedResourceMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.DelegatedResourceMessage,
                org.tron.api.GrpcAPI.DelegatedResourceList>(
                  this, METHODID_GET_DELEGATED_RESOURCE)))
          .addMethod(
            getGetDelegatedResourceAccountIndexMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.BytesMessage,
                org.tron.protos.Protocol.DelegatedResourceAccountIndex>(
                  this, METHODID_GET_DELEGATED_RESOURCE_ACCOUNT_INDEX)))
          .addMethod(
            getGetExchangeByIdMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.BytesMessage,
                org.tron.protos.Protocol.Exchange>(
                  this, METHODID_GET_EXCHANGE_BY_ID)))
          .addMethod(
            getListExchangesMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.EmptyMessage,
                org.tron.api.GrpcAPI.ExchangeList>(
                  this, METHODID_LIST_EXCHANGES)))
          .addMethod(
            getGetTransactionByIdMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.BytesMessage,
                org.tron.protos.Protocol.Transaction>(
                  this, METHODID_GET_TRANSACTION_BY_ID)))
          .addMethod(
            getGetTransactionInfoByIdMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.BytesMessage,
                org.tron.protos.Protocol.TransactionInfo>(
                  this, METHODID_GET_TRANSACTION_INFO_BY_ID)))
          .addMethod(
            getGenerateAddressMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.EmptyMessage,
                org.tron.api.GrpcAPI.AddressPrKeyPairMessage>(
                  this, METHODID_GENERATE_ADDRESS)))
          .build();
    }
  }

  /**
   */
  public static final class WalletSolidityStub extends io.grpc.stub.AbstractStub<WalletSolidityStub> {
    private WalletSolidityStub(io.grpc.Channel channel) {
      super(channel);
    }

    private WalletSolidityStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WalletSolidityStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new WalletSolidityStub(channel, callOptions);
    }

    /**
     */
    public void getAccount(org.tron.protos.Protocol.Account request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Account> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAccountMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getAccountById(org.tron.protos.Protocol.Account request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Account> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAccountByIdMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void listWitnesses(org.tron.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.WitnessList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListWitnessesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getAssetIssueList(org.tron.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.AssetIssueList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAssetIssueListMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getPaginatedAssetIssueList(org.tron.api.GrpcAPI.PaginatedMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.AssetIssueList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetPaginatedAssetIssueListMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getAssetIssueByName(org.tron.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Contract.AssetIssueContract> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAssetIssueByNameMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getAssetIssueListByName(org.tron.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.AssetIssueList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAssetIssueListByNameMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getAssetIssueById(org.tron.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Contract.AssetIssueContract> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAssetIssueByIdMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use GetNowBlock2 instead of this function.
     * </pre>
     */
    public void getNowBlock(org.tron.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Block> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetNowBlockMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of GetNowBlock.
     * </pre>
     */
    public void getNowBlock2(org.tron.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.BlockExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetNowBlock2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use GetBlockByNum2 instead of this function.
     * </pre>
     */
    public void getBlockByNum(org.tron.api.GrpcAPI.NumberMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Block> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetBlockByNumMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of GetBlockByNum.
     * </pre>
     */
    public void getBlockByNum2(org.tron.api.GrpcAPI.NumberMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.BlockExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetBlockByNum2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getTransactionCountByBlockNum(org.tron.api.GrpcAPI.NumberMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.NumberMessage> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetTransactionCountByBlockNumMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getDelegatedResource(org.tron.api.GrpcAPI.DelegatedResourceMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.DelegatedResourceList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetDelegatedResourceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getDelegatedResourceAccountIndex(org.tron.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.DelegatedResourceAccountIndex> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetDelegatedResourceAccountIndexMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getExchangeById(org.tron.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Exchange> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetExchangeByIdMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void listExchanges(org.tron.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.ExchangeList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListExchangesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getTransactionById(org.tron.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetTransactionByIdMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getTransactionInfoById(org.tron.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.TransactionInfo> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetTransactionInfoByIdMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public void generateAddress(org.tron.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.AddressPrKeyPairMessage> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGenerateAddressMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class WalletSolidityBlockingStub extends io.grpc.stub.AbstractStub<WalletSolidityBlockingStub> {
    private WalletSolidityBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private WalletSolidityBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WalletSolidityBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new WalletSolidityBlockingStub(channel, callOptions);
    }

    /**
     */
    public org.tron.protos.Protocol.Account getAccount(org.tron.protos.Protocol.Account request) {
      return blockingUnaryCall(
          getChannel(), getGetAccountMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.protos.Protocol.Account getAccountById(org.tron.protos.Protocol.Account request) {
      return blockingUnaryCall(
          getChannel(), getGetAccountByIdMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.WitnessList listWitnesses(org.tron.api.GrpcAPI.EmptyMessage request) {
      return blockingUnaryCall(
          getChannel(), getListWitnessesMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.AssetIssueList getAssetIssueList(org.tron.api.GrpcAPI.EmptyMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetAssetIssueListMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.AssetIssueList getPaginatedAssetIssueList(org.tron.api.GrpcAPI.PaginatedMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetPaginatedAssetIssueListMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.protos.Contract.AssetIssueContract getAssetIssueByName(org.tron.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetAssetIssueByNameMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.AssetIssueList getAssetIssueListByName(org.tron.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetAssetIssueListByNameMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.protos.Contract.AssetIssueContract getAssetIssueById(org.tron.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetAssetIssueByIdMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use GetNowBlock2 instead of this function.
     * </pre>
     */
    public org.tron.protos.Protocol.Block getNowBlock(org.tron.api.GrpcAPI.EmptyMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetNowBlockMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of GetNowBlock.
     * </pre>
     */
    public org.tron.api.GrpcAPI.BlockExtention getNowBlock2(org.tron.api.GrpcAPI.EmptyMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetNowBlock2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use GetBlockByNum2 instead of this function.
     * </pre>
     */
    public org.tron.protos.Protocol.Block getBlockByNum(org.tron.api.GrpcAPI.NumberMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetBlockByNumMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of GetBlockByNum.
     * </pre>
     */
    public org.tron.api.GrpcAPI.BlockExtention getBlockByNum2(org.tron.api.GrpcAPI.NumberMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetBlockByNum2Method(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.NumberMessage getTransactionCountByBlockNum(org.tron.api.GrpcAPI.NumberMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetTransactionCountByBlockNumMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.DelegatedResourceList getDelegatedResource(org.tron.api.GrpcAPI.DelegatedResourceMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetDelegatedResourceMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.protos.Protocol.DelegatedResourceAccountIndex getDelegatedResourceAccountIndex(org.tron.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetDelegatedResourceAccountIndexMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.protos.Protocol.Exchange getExchangeById(org.tron.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetExchangeByIdMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.ExchangeList listExchanges(org.tron.api.GrpcAPI.EmptyMessage request) {
      return blockingUnaryCall(
          getChannel(), getListExchangesMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.protos.Protocol.Transaction getTransactionById(org.tron.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetTransactionByIdMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.protos.Protocol.TransactionInfo getTransactionInfoById(org.tron.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetTransactionInfoByIdMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public org.tron.api.GrpcAPI.AddressPrKeyPairMessage generateAddress(org.tron.api.GrpcAPI.EmptyMessage request) {
      return blockingUnaryCall(
          getChannel(), getGenerateAddressMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class WalletSolidityFutureStub extends io.grpc.stub.AbstractStub<WalletSolidityFutureStub> {
    private WalletSolidityFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private WalletSolidityFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WalletSolidityFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new WalletSolidityFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.Account> getAccount(
        org.tron.protos.Protocol.Account request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAccountMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.Account> getAccountById(
        org.tron.protos.Protocol.Account request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAccountByIdMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.WitnessList> listWitnesses(
        org.tron.api.GrpcAPI.EmptyMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getListWitnessesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.AssetIssueList> getAssetIssueList(
        org.tron.api.GrpcAPI.EmptyMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAssetIssueListMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.AssetIssueList> getPaginatedAssetIssueList(
        org.tron.api.GrpcAPI.PaginatedMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetPaginatedAssetIssueListMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Contract.AssetIssueContract> getAssetIssueByName(
        org.tron.api.GrpcAPI.BytesMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAssetIssueByNameMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.AssetIssueList> getAssetIssueListByName(
        org.tron.api.GrpcAPI.BytesMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAssetIssueListByNameMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Contract.AssetIssueContract> getAssetIssueById(
        org.tron.api.GrpcAPI.BytesMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAssetIssueByIdMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use GetNowBlock2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.Block> getNowBlock(
        org.tron.api.GrpcAPI.EmptyMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetNowBlockMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of GetNowBlock.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.BlockExtention> getNowBlock2(
        org.tron.api.GrpcAPI.EmptyMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetNowBlock2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use GetBlockByNum2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.Block> getBlockByNum(
        org.tron.api.GrpcAPI.NumberMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetBlockByNumMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of GetBlockByNum.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.BlockExtention> getBlockByNum2(
        org.tron.api.GrpcAPI.NumberMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetBlockByNum2Method(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.NumberMessage> getTransactionCountByBlockNum(
        org.tron.api.GrpcAPI.NumberMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetTransactionCountByBlockNumMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.DelegatedResourceList> getDelegatedResource(
        org.tron.api.GrpcAPI.DelegatedResourceMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetDelegatedResourceMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.DelegatedResourceAccountIndex> getDelegatedResourceAccountIndex(
        org.tron.api.GrpcAPI.BytesMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetDelegatedResourceAccountIndexMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.Exchange> getExchangeById(
        org.tron.api.GrpcAPI.BytesMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetExchangeByIdMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.ExchangeList> listExchanges(
        org.tron.api.GrpcAPI.EmptyMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getListExchangesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.Transaction> getTransactionById(
        org.tron.api.GrpcAPI.BytesMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetTransactionByIdMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.TransactionInfo> getTransactionInfoById(
        org.tron.api.GrpcAPI.BytesMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetTransactionInfoByIdMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.AddressPrKeyPairMessage> generateAddress(
        org.tron.api.GrpcAPI.EmptyMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGenerateAddressMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_ACCOUNT = 0;
  private static final int METHODID_GET_ACCOUNT_BY_ID = 1;
  private static final int METHODID_LIST_WITNESSES = 2;
  private static final int METHODID_GET_ASSET_ISSUE_LIST = 3;
  private static final int METHODID_GET_PAGINATED_ASSET_ISSUE_LIST = 4;
  private static final int METHODID_GET_ASSET_ISSUE_BY_NAME = 5;
  private static final int METHODID_GET_ASSET_ISSUE_LIST_BY_NAME = 6;
  private static final int METHODID_GET_ASSET_ISSUE_BY_ID = 7;
  private static final int METHODID_GET_NOW_BLOCK = 8;
  private static final int METHODID_GET_NOW_BLOCK2 = 9;
  private static final int METHODID_GET_BLOCK_BY_NUM = 10;
  private static final int METHODID_GET_BLOCK_BY_NUM2 = 11;
  private static final int METHODID_GET_TRANSACTION_COUNT_BY_BLOCK_NUM = 12;
  private static final int METHODID_GET_DELEGATED_RESOURCE = 13;
  private static final int METHODID_GET_DELEGATED_RESOURCE_ACCOUNT_INDEX = 14;
  private static final int METHODID_GET_EXCHANGE_BY_ID = 15;
  private static final int METHODID_LIST_EXCHANGES = 16;
  private static final int METHODID_GET_TRANSACTION_BY_ID = 17;
  private static final int METHODID_GET_TRANSACTION_INFO_BY_ID = 18;
  private static final int METHODID_GENERATE_ADDRESS = 19;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final WalletSolidityImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(WalletSolidityImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_ACCOUNT:
          serviceImpl.getAccount((org.tron.protos.Protocol.Account) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Account>) responseObserver);
          break;
        case METHODID_GET_ACCOUNT_BY_ID:
          serviceImpl.getAccountById((org.tron.protos.Protocol.Account) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Account>) responseObserver);
          break;
        case METHODID_LIST_WITNESSES:
          serviceImpl.listWitnesses((org.tron.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.WitnessList>) responseObserver);
          break;
        case METHODID_GET_ASSET_ISSUE_LIST:
          serviceImpl.getAssetIssueList((org.tron.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.AssetIssueList>) responseObserver);
          break;
        case METHODID_GET_PAGINATED_ASSET_ISSUE_LIST:
          serviceImpl.getPaginatedAssetIssueList((org.tron.api.GrpcAPI.PaginatedMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.AssetIssueList>) responseObserver);
          break;
        case METHODID_GET_ASSET_ISSUE_BY_NAME:
          serviceImpl.getAssetIssueByName((org.tron.api.GrpcAPI.BytesMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Contract.AssetIssueContract>) responseObserver);
          break;
        case METHODID_GET_ASSET_ISSUE_LIST_BY_NAME:
          serviceImpl.getAssetIssueListByName((org.tron.api.GrpcAPI.BytesMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.AssetIssueList>) responseObserver);
          break;
        case METHODID_GET_ASSET_ISSUE_BY_ID:
          serviceImpl.getAssetIssueById((org.tron.api.GrpcAPI.BytesMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Contract.AssetIssueContract>) responseObserver);
          break;
        case METHODID_GET_NOW_BLOCK:
          serviceImpl.getNowBlock((org.tron.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Block>) responseObserver);
          break;
        case METHODID_GET_NOW_BLOCK2:
          serviceImpl.getNowBlock2((org.tron.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.BlockExtention>) responseObserver);
          break;
        case METHODID_GET_BLOCK_BY_NUM:
          serviceImpl.getBlockByNum((org.tron.api.GrpcAPI.NumberMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Block>) responseObserver);
          break;
        case METHODID_GET_BLOCK_BY_NUM2:
          serviceImpl.getBlockByNum2((org.tron.api.GrpcAPI.NumberMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.BlockExtention>) responseObserver);
          break;
        case METHODID_GET_TRANSACTION_COUNT_BY_BLOCK_NUM:
          serviceImpl.getTransactionCountByBlockNum((org.tron.api.GrpcAPI.NumberMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.NumberMessage>) responseObserver);
          break;
        case METHODID_GET_DELEGATED_RESOURCE:
          serviceImpl.getDelegatedResource((org.tron.api.GrpcAPI.DelegatedResourceMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.DelegatedResourceList>) responseObserver);
          break;
        case METHODID_GET_DELEGATED_RESOURCE_ACCOUNT_INDEX:
          serviceImpl.getDelegatedResourceAccountIndex((org.tron.api.GrpcAPI.BytesMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.DelegatedResourceAccountIndex>) responseObserver);
          break;
        case METHODID_GET_EXCHANGE_BY_ID:
          serviceImpl.getExchangeById((org.tron.api.GrpcAPI.BytesMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Exchange>) responseObserver);
          break;
        case METHODID_LIST_EXCHANGES:
          serviceImpl.listExchanges((org.tron.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.ExchangeList>) responseObserver);
          break;
        case METHODID_GET_TRANSACTION_BY_ID:
          serviceImpl.getTransactionById((org.tron.api.GrpcAPI.BytesMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction>) responseObserver);
          break;
        case METHODID_GET_TRANSACTION_INFO_BY_ID:
          serviceImpl.getTransactionInfoById((org.tron.api.GrpcAPI.BytesMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.TransactionInfo>) responseObserver);
          break;
        case METHODID_GENERATE_ADDRESS:
          serviceImpl.generateAddress((org.tron.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.AddressPrKeyPairMessage>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class WalletSolidityBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    WalletSolidityBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.tron.api.GrpcAPI.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("WalletSolidity");
    }
  }

  private static final class WalletSolidityFileDescriptorSupplier
      extends WalletSolidityBaseDescriptorSupplier {
    WalletSolidityFileDescriptorSupplier() {}
  }

  private static final class WalletSolidityMethodDescriptorSupplier
      extends WalletSolidityBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    WalletSolidityMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (WalletSolidityGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new WalletSolidityFileDescriptorSupplier())
              .addMethod(getGetAccountMethod())
              .addMethod(getGetAccountByIdMethod())
              .addMethod(getListWitnessesMethod())
              .addMethod(getGetAssetIssueListMethod())
              .addMethod(getGetPaginatedAssetIssueListMethod())
              .addMethod(getGetAssetIssueByNameMethod())
              .addMethod(getGetAssetIssueListByNameMethod())
              .addMethod(getGetAssetIssueByIdMethod())
              .addMethod(getGetNowBlockMethod())
              .addMethod(getGetNowBlock2Method())
              .addMethod(getGetBlockByNumMethod())
              .addMethod(getGetBlockByNum2Method())
              .addMethod(getGetTransactionCountByBlockNumMethod())
              .addMethod(getGetDelegatedResourceMethod())
              .addMethod(getGetDelegatedResourceAccountIndexMethod())
              .addMethod(getGetExchangeByIdMethod())
              .addMethod(getListExchangesMethod())
              .addMethod(getGetTransactionByIdMethod())
              .addMethod(getGetTransactionInfoByIdMethod())
              .addMethod(getGenerateAddressMethod())
              .build();
        }
      }
    }
    return result;
  }
}
