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
public final class WalletGrpc {

  private WalletGrpc() {}

  public static final String SERVICE_NAME = "protocol.Wallet";

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
    if ((getGetAccountMethod = WalletGrpc.getGetAccountMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetAccountMethod = WalletGrpc.getGetAccountMethod) == null) {
          WalletGrpc.getGetAccountMethod = getGetAccountMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Protocol.Account, org.tron.protos.Protocol.Account>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetAccount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Account.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Account.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetAccount"))
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
    if ((getGetAccountByIdMethod = WalletGrpc.getGetAccountByIdMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetAccountByIdMethod = WalletGrpc.getGetAccountByIdMethod) == null) {
          WalletGrpc.getGetAccountByIdMethod = getGetAccountByIdMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Protocol.Account, org.tron.protos.Protocol.Account>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetAccountById"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Account.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Account.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetAccountById"))
                  .build();
          }
        }
     }
     return getGetAccountByIdMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getCreateTransactionMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.TransferContract,
      org.tron.protos.Protocol.Transaction> METHOD_CREATE_TRANSACTION = getCreateTransactionMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.TransferContract,
      org.tron.protos.Protocol.Transaction> getCreateTransactionMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.TransferContract,
      org.tron.protos.Protocol.Transaction> getCreateTransactionMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.TransferContract, org.tron.protos.Protocol.Transaction> getCreateTransactionMethod;
    if ((getCreateTransactionMethod = WalletGrpc.getCreateTransactionMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getCreateTransactionMethod = WalletGrpc.getCreateTransactionMethod) == null) {
          WalletGrpc.getCreateTransactionMethod = getCreateTransactionMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.TransferContract, org.tron.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "CreateTransaction"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.TransferContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("CreateTransaction"))
                  .build();
          }
        }
     }
     return getCreateTransactionMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getCreateTransaction2Method()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.TransferContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_CREATE_TRANSACTION2 = getCreateTransaction2Method();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.TransferContract,
      org.tron.api.GrpcAPI.TransactionExtention> getCreateTransaction2Method;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.TransferContract,
      org.tron.api.GrpcAPI.TransactionExtention> getCreateTransaction2Method() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.TransferContract, org.tron.api.GrpcAPI.TransactionExtention> getCreateTransaction2Method;
    if ((getCreateTransaction2Method = WalletGrpc.getCreateTransaction2Method) == null) {
      synchronized (WalletGrpc.class) {
        if ((getCreateTransaction2Method = WalletGrpc.getCreateTransaction2Method) == null) {
          WalletGrpc.getCreateTransaction2Method = getCreateTransaction2Method = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.TransferContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "CreateTransaction2"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.TransferContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("CreateTransaction2"))
                  .build();
          }
        }
     }
     return getCreateTransaction2Method;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getBroadcastTransactionMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Protocol.Transaction,
      org.tron.api.GrpcAPI.Return> METHOD_BROADCAST_TRANSACTION = getBroadcastTransactionMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Protocol.Transaction,
      org.tron.api.GrpcAPI.Return> getBroadcastTransactionMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Protocol.Transaction,
      org.tron.api.GrpcAPI.Return> getBroadcastTransactionMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Protocol.Transaction, org.tron.api.GrpcAPI.Return> getBroadcastTransactionMethod;
    if ((getBroadcastTransactionMethod = WalletGrpc.getBroadcastTransactionMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getBroadcastTransactionMethod = WalletGrpc.getBroadcastTransactionMethod) == null) {
          WalletGrpc.getBroadcastTransactionMethod = getBroadcastTransactionMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Protocol.Transaction, org.tron.api.GrpcAPI.Return>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "BroadcastTransaction"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Transaction.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.Return.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("BroadcastTransaction"))
                  .build();
          }
        }
     }
     return getBroadcastTransactionMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getUpdateAccountMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.AccountUpdateContract,
      org.tron.protos.Protocol.Transaction> METHOD_UPDATE_ACCOUNT = getUpdateAccountMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.AccountUpdateContract,
      org.tron.protos.Protocol.Transaction> getUpdateAccountMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.AccountUpdateContract,
      org.tron.protos.Protocol.Transaction> getUpdateAccountMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.AccountUpdateContract, org.tron.protos.Protocol.Transaction> getUpdateAccountMethod;
    if ((getUpdateAccountMethod = WalletGrpc.getUpdateAccountMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getUpdateAccountMethod = WalletGrpc.getUpdateAccountMethod) == null) {
          WalletGrpc.getUpdateAccountMethod = getUpdateAccountMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.AccountUpdateContract, org.tron.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "UpdateAccount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.AccountUpdateContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("UpdateAccount"))
                  .build();
          }
        }
     }
     return getUpdateAccountMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getSetAccountIdMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.SetAccountIdContract,
      org.tron.protos.Protocol.Transaction> METHOD_SET_ACCOUNT_ID = getSetAccountIdMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.SetAccountIdContract,
      org.tron.protos.Protocol.Transaction> getSetAccountIdMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.SetAccountIdContract,
      org.tron.protos.Protocol.Transaction> getSetAccountIdMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.SetAccountIdContract, org.tron.protos.Protocol.Transaction> getSetAccountIdMethod;
    if ((getSetAccountIdMethod = WalletGrpc.getSetAccountIdMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getSetAccountIdMethod = WalletGrpc.getSetAccountIdMethod) == null) {
          WalletGrpc.getSetAccountIdMethod = getSetAccountIdMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.SetAccountIdContract, org.tron.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "SetAccountId"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.SetAccountIdContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("SetAccountId"))
                  .build();
          }
        }
     }
     return getSetAccountIdMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getUpdateAccount2Method()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.AccountUpdateContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_UPDATE_ACCOUNT2 = getUpdateAccount2Method();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.AccountUpdateContract,
      org.tron.api.GrpcAPI.TransactionExtention> getUpdateAccount2Method;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.AccountUpdateContract,
      org.tron.api.GrpcAPI.TransactionExtention> getUpdateAccount2Method() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.AccountUpdateContract, org.tron.api.GrpcAPI.TransactionExtention> getUpdateAccount2Method;
    if ((getUpdateAccount2Method = WalletGrpc.getUpdateAccount2Method) == null) {
      synchronized (WalletGrpc.class) {
        if ((getUpdateAccount2Method = WalletGrpc.getUpdateAccount2Method) == null) {
          WalletGrpc.getUpdateAccount2Method = getUpdateAccount2Method = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.AccountUpdateContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "UpdateAccount2"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.AccountUpdateContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("UpdateAccount2"))
                  .build();
          }
        }
     }
     return getUpdateAccount2Method;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getVoteWitnessAccountMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.VoteWitnessContract,
      org.tron.protos.Protocol.Transaction> METHOD_VOTE_WITNESS_ACCOUNT = getVoteWitnessAccountMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.VoteWitnessContract,
      org.tron.protos.Protocol.Transaction> getVoteWitnessAccountMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.VoteWitnessContract,
      org.tron.protos.Protocol.Transaction> getVoteWitnessAccountMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.VoteWitnessContract, org.tron.protos.Protocol.Transaction> getVoteWitnessAccountMethod;
    if ((getVoteWitnessAccountMethod = WalletGrpc.getVoteWitnessAccountMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getVoteWitnessAccountMethod = WalletGrpc.getVoteWitnessAccountMethod) == null) {
          WalletGrpc.getVoteWitnessAccountMethod = getVoteWitnessAccountMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.VoteWitnessContract, org.tron.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "VoteWitnessAccount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.VoteWitnessContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("VoteWitnessAccount"))
                  .build();
          }
        }
     }
     return getVoteWitnessAccountMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getUpdateSettingMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.UpdateSettingContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_UPDATE_SETTING = getUpdateSettingMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.UpdateSettingContract,
      org.tron.api.GrpcAPI.TransactionExtention> getUpdateSettingMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.UpdateSettingContract,
      org.tron.api.GrpcAPI.TransactionExtention> getUpdateSettingMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.UpdateSettingContract, org.tron.api.GrpcAPI.TransactionExtention> getUpdateSettingMethod;
    if ((getUpdateSettingMethod = WalletGrpc.getUpdateSettingMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getUpdateSettingMethod = WalletGrpc.getUpdateSettingMethod) == null) {
          WalletGrpc.getUpdateSettingMethod = getUpdateSettingMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.UpdateSettingContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "UpdateSetting"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.UpdateSettingContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("UpdateSetting"))
                  .build();
          }
        }
     }
     return getUpdateSettingMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getUpdateEnergyLimitMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.UpdateEnergyLimitContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_UPDATE_ENERGY_LIMIT = getUpdateEnergyLimitMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.UpdateEnergyLimitContract,
      org.tron.api.GrpcAPI.TransactionExtention> getUpdateEnergyLimitMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.UpdateEnergyLimitContract,
      org.tron.api.GrpcAPI.TransactionExtention> getUpdateEnergyLimitMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.UpdateEnergyLimitContract, org.tron.api.GrpcAPI.TransactionExtention> getUpdateEnergyLimitMethod;
    if ((getUpdateEnergyLimitMethod = WalletGrpc.getUpdateEnergyLimitMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getUpdateEnergyLimitMethod = WalletGrpc.getUpdateEnergyLimitMethod) == null) {
          WalletGrpc.getUpdateEnergyLimitMethod = getUpdateEnergyLimitMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.UpdateEnergyLimitContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "UpdateEnergyLimit"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.UpdateEnergyLimitContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("UpdateEnergyLimit"))
                  .build();
          }
        }
     }
     return getUpdateEnergyLimitMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getVoteWitnessAccount2Method()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.VoteWitnessContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_VOTE_WITNESS_ACCOUNT2 = getVoteWitnessAccount2Method();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.VoteWitnessContract,
      org.tron.api.GrpcAPI.TransactionExtention> getVoteWitnessAccount2Method;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.VoteWitnessContract,
      org.tron.api.GrpcAPI.TransactionExtention> getVoteWitnessAccount2Method() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.VoteWitnessContract, org.tron.api.GrpcAPI.TransactionExtention> getVoteWitnessAccount2Method;
    if ((getVoteWitnessAccount2Method = WalletGrpc.getVoteWitnessAccount2Method) == null) {
      synchronized (WalletGrpc.class) {
        if ((getVoteWitnessAccount2Method = WalletGrpc.getVoteWitnessAccount2Method) == null) {
          WalletGrpc.getVoteWitnessAccount2Method = getVoteWitnessAccount2Method = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.VoteWitnessContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "VoteWitnessAccount2"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.VoteWitnessContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("VoteWitnessAccount2"))
                  .build();
          }
        }
     }
     return getVoteWitnessAccount2Method;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getCreateAssetIssueMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.AssetIssueContract,
      org.tron.protos.Protocol.Transaction> METHOD_CREATE_ASSET_ISSUE = getCreateAssetIssueMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.AssetIssueContract,
      org.tron.protos.Protocol.Transaction> getCreateAssetIssueMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.AssetIssueContract,
      org.tron.protos.Protocol.Transaction> getCreateAssetIssueMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.AssetIssueContract, org.tron.protos.Protocol.Transaction> getCreateAssetIssueMethod;
    if ((getCreateAssetIssueMethod = WalletGrpc.getCreateAssetIssueMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getCreateAssetIssueMethod = WalletGrpc.getCreateAssetIssueMethod) == null) {
          WalletGrpc.getCreateAssetIssueMethod = getCreateAssetIssueMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.AssetIssueContract, org.tron.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "CreateAssetIssue"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.AssetIssueContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("CreateAssetIssue"))
                  .build();
          }
        }
     }
     return getCreateAssetIssueMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getCreateAssetIssue2Method()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.AssetIssueContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_CREATE_ASSET_ISSUE2 = getCreateAssetIssue2Method();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.AssetIssueContract,
      org.tron.api.GrpcAPI.TransactionExtention> getCreateAssetIssue2Method;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.AssetIssueContract,
      org.tron.api.GrpcAPI.TransactionExtention> getCreateAssetIssue2Method() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.AssetIssueContract, org.tron.api.GrpcAPI.TransactionExtention> getCreateAssetIssue2Method;
    if ((getCreateAssetIssue2Method = WalletGrpc.getCreateAssetIssue2Method) == null) {
      synchronized (WalletGrpc.class) {
        if ((getCreateAssetIssue2Method = WalletGrpc.getCreateAssetIssue2Method) == null) {
          WalletGrpc.getCreateAssetIssue2Method = getCreateAssetIssue2Method = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.AssetIssueContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "CreateAssetIssue2"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.AssetIssueContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("CreateAssetIssue2"))
                  .build();
          }
        }
     }
     return getCreateAssetIssue2Method;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getUpdateWitnessMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.WitnessUpdateContract,
      org.tron.protos.Protocol.Transaction> METHOD_UPDATE_WITNESS = getUpdateWitnessMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.WitnessUpdateContract,
      org.tron.protos.Protocol.Transaction> getUpdateWitnessMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.WitnessUpdateContract,
      org.tron.protos.Protocol.Transaction> getUpdateWitnessMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.WitnessUpdateContract, org.tron.protos.Protocol.Transaction> getUpdateWitnessMethod;
    if ((getUpdateWitnessMethod = WalletGrpc.getUpdateWitnessMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getUpdateWitnessMethod = WalletGrpc.getUpdateWitnessMethod) == null) {
          WalletGrpc.getUpdateWitnessMethod = getUpdateWitnessMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.WitnessUpdateContract, org.tron.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "UpdateWitness"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.WitnessUpdateContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("UpdateWitness"))
                  .build();
          }
        }
     }
     return getUpdateWitnessMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getUpdateWitness2Method()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.WitnessUpdateContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_UPDATE_WITNESS2 = getUpdateWitness2Method();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.WitnessUpdateContract,
      org.tron.api.GrpcAPI.TransactionExtention> getUpdateWitness2Method;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.WitnessUpdateContract,
      org.tron.api.GrpcAPI.TransactionExtention> getUpdateWitness2Method() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.WitnessUpdateContract, org.tron.api.GrpcAPI.TransactionExtention> getUpdateWitness2Method;
    if ((getUpdateWitness2Method = WalletGrpc.getUpdateWitness2Method) == null) {
      synchronized (WalletGrpc.class) {
        if ((getUpdateWitness2Method = WalletGrpc.getUpdateWitness2Method) == null) {
          WalletGrpc.getUpdateWitness2Method = getUpdateWitness2Method = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.WitnessUpdateContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "UpdateWitness2"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.WitnessUpdateContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("UpdateWitness2"))
                  .build();
          }
        }
     }
     return getUpdateWitness2Method;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getCreateAccountMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.AccountCreateContract,
      org.tron.protos.Protocol.Transaction> METHOD_CREATE_ACCOUNT = getCreateAccountMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.AccountCreateContract,
      org.tron.protos.Protocol.Transaction> getCreateAccountMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.AccountCreateContract,
      org.tron.protos.Protocol.Transaction> getCreateAccountMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.AccountCreateContract, org.tron.protos.Protocol.Transaction> getCreateAccountMethod;
    if ((getCreateAccountMethod = WalletGrpc.getCreateAccountMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getCreateAccountMethod = WalletGrpc.getCreateAccountMethod) == null) {
          WalletGrpc.getCreateAccountMethod = getCreateAccountMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.AccountCreateContract, org.tron.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "CreateAccount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.AccountCreateContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("CreateAccount"))
                  .build();
          }
        }
     }
     return getCreateAccountMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getCreateAccount2Method()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.AccountCreateContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_CREATE_ACCOUNT2 = getCreateAccount2Method();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.AccountCreateContract,
      org.tron.api.GrpcAPI.TransactionExtention> getCreateAccount2Method;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.AccountCreateContract,
      org.tron.api.GrpcAPI.TransactionExtention> getCreateAccount2Method() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.AccountCreateContract, org.tron.api.GrpcAPI.TransactionExtention> getCreateAccount2Method;
    if ((getCreateAccount2Method = WalletGrpc.getCreateAccount2Method) == null) {
      synchronized (WalletGrpc.class) {
        if ((getCreateAccount2Method = WalletGrpc.getCreateAccount2Method) == null) {
          WalletGrpc.getCreateAccount2Method = getCreateAccount2Method = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.AccountCreateContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "CreateAccount2"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.AccountCreateContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("CreateAccount2"))
                  .build();
          }
        }
     }
     return getCreateAccount2Method;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getCreateWitnessMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.WitnessCreateContract,
      org.tron.protos.Protocol.Transaction> METHOD_CREATE_WITNESS = getCreateWitnessMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.WitnessCreateContract,
      org.tron.protos.Protocol.Transaction> getCreateWitnessMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.WitnessCreateContract,
      org.tron.protos.Protocol.Transaction> getCreateWitnessMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.WitnessCreateContract, org.tron.protos.Protocol.Transaction> getCreateWitnessMethod;
    if ((getCreateWitnessMethod = WalletGrpc.getCreateWitnessMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getCreateWitnessMethod = WalletGrpc.getCreateWitnessMethod) == null) {
          WalletGrpc.getCreateWitnessMethod = getCreateWitnessMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.WitnessCreateContract, org.tron.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "CreateWitness"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.WitnessCreateContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("CreateWitness"))
                  .build();
          }
        }
     }
     return getCreateWitnessMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getCreateWitness2Method()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.WitnessCreateContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_CREATE_WITNESS2 = getCreateWitness2Method();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.WitnessCreateContract,
      org.tron.api.GrpcAPI.TransactionExtention> getCreateWitness2Method;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.WitnessCreateContract,
      org.tron.api.GrpcAPI.TransactionExtention> getCreateWitness2Method() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.WitnessCreateContract, org.tron.api.GrpcAPI.TransactionExtention> getCreateWitness2Method;
    if ((getCreateWitness2Method = WalletGrpc.getCreateWitness2Method) == null) {
      synchronized (WalletGrpc.class) {
        if ((getCreateWitness2Method = WalletGrpc.getCreateWitness2Method) == null) {
          WalletGrpc.getCreateWitness2Method = getCreateWitness2Method = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.WitnessCreateContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "CreateWitness2"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.WitnessCreateContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("CreateWitness2"))
                  .build();
          }
        }
     }
     return getCreateWitness2Method;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getTransferAssetMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.TransferAssetContract,
      org.tron.protos.Protocol.Transaction> METHOD_TRANSFER_ASSET = getTransferAssetMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.TransferAssetContract,
      org.tron.protos.Protocol.Transaction> getTransferAssetMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.TransferAssetContract,
      org.tron.protos.Protocol.Transaction> getTransferAssetMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.TransferAssetContract, org.tron.protos.Protocol.Transaction> getTransferAssetMethod;
    if ((getTransferAssetMethod = WalletGrpc.getTransferAssetMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getTransferAssetMethod = WalletGrpc.getTransferAssetMethod) == null) {
          WalletGrpc.getTransferAssetMethod = getTransferAssetMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.TransferAssetContract, org.tron.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "TransferAsset"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.TransferAssetContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("TransferAsset"))
                  .build();
          }
        }
     }
     return getTransferAssetMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getTransferAsset2Method()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.TransferAssetContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_TRANSFER_ASSET2 = getTransferAsset2Method();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.TransferAssetContract,
      org.tron.api.GrpcAPI.TransactionExtention> getTransferAsset2Method;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.TransferAssetContract,
      org.tron.api.GrpcAPI.TransactionExtention> getTransferAsset2Method() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.TransferAssetContract, org.tron.api.GrpcAPI.TransactionExtention> getTransferAsset2Method;
    if ((getTransferAsset2Method = WalletGrpc.getTransferAsset2Method) == null) {
      synchronized (WalletGrpc.class) {
        if ((getTransferAsset2Method = WalletGrpc.getTransferAsset2Method) == null) {
          WalletGrpc.getTransferAsset2Method = getTransferAsset2Method = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.TransferAssetContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "TransferAsset2"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.TransferAssetContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("TransferAsset2"))
                  .build();
          }
        }
     }
     return getTransferAsset2Method;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getParticipateAssetIssueMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.ParticipateAssetIssueContract,
      org.tron.protos.Protocol.Transaction> METHOD_PARTICIPATE_ASSET_ISSUE = getParticipateAssetIssueMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.ParticipateAssetIssueContract,
      org.tron.protos.Protocol.Transaction> getParticipateAssetIssueMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.ParticipateAssetIssueContract,
      org.tron.protos.Protocol.Transaction> getParticipateAssetIssueMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.ParticipateAssetIssueContract, org.tron.protos.Protocol.Transaction> getParticipateAssetIssueMethod;
    if ((getParticipateAssetIssueMethod = WalletGrpc.getParticipateAssetIssueMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getParticipateAssetIssueMethod = WalletGrpc.getParticipateAssetIssueMethod) == null) {
          WalletGrpc.getParticipateAssetIssueMethod = getParticipateAssetIssueMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.ParticipateAssetIssueContract, org.tron.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "ParticipateAssetIssue"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.ParticipateAssetIssueContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("ParticipateAssetIssue"))
                  .build();
          }
        }
     }
     return getParticipateAssetIssueMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getParticipateAssetIssue2Method()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.ParticipateAssetIssueContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_PARTICIPATE_ASSET_ISSUE2 = getParticipateAssetIssue2Method();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.ParticipateAssetIssueContract,
      org.tron.api.GrpcAPI.TransactionExtention> getParticipateAssetIssue2Method;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.ParticipateAssetIssueContract,
      org.tron.api.GrpcAPI.TransactionExtention> getParticipateAssetIssue2Method() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.ParticipateAssetIssueContract, org.tron.api.GrpcAPI.TransactionExtention> getParticipateAssetIssue2Method;
    if ((getParticipateAssetIssue2Method = WalletGrpc.getParticipateAssetIssue2Method) == null) {
      synchronized (WalletGrpc.class) {
        if ((getParticipateAssetIssue2Method = WalletGrpc.getParticipateAssetIssue2Method) == null) {
          WalletGrpc.getParticipateAssetIssue2Method = getParticipateAssetIssue2Method = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.ParticipateAssetIssueContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "ParticipateAssetIssue2"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.ParticipateAssetIssueContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("ParticipateAssetIssue2"))
                  .build();
          }
        }
     }
     return getParticipateAssetIssue2Method;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getFreezeBalanceMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.FreezeBalanceContract,
      org.tron.protos.Protocol.Transaction> METHOD_FREEZE_BALANCE = getFreezeBalanceMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.FreezeBalanceContract,
      org.tron.protos.Protocol.Transaction> getFreezeBalanceMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.FreezeBalanceContract,
      org.tron.protos.Protocol.Transaction> getFreezeBalanceMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.FreezeBalanceContract, org.tron.protos.Protocol.Transaction> getFreezeBalanceMethod;
    if ((getFreezeBalanceMethod = WalletGrpc.getFreezeBalanceMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getFreezeBalanceMethod = WalletGrpc.getFreezeBalanceMethod) == null) {
          WalletGrpc.getFreezeBalanceMethod = getFreezeBalanceMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.FreezeBalanceContract, org.tron.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "FreezeBalance"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.FreezeBalanceContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("FreezeBalance"))
                  .build();
          }
        }
     }
     return getFreezeBalanceMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getFreezeBalance2Method()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.FreezeBalanceContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_FREEZE_BALANCE2 = getFreezeBalance2Method();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.FreezeBalanceContract,
      org.tron.api.GrpcAPI.TransactionExtention> getFreezeBalance2Method;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.FreezeBalanceContract,
      org.tron.api.GrpcAPI.TransactionExtention> getFreezeBalance2Method() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.FreezeBalanceContract, org.tron.api.GrpcAPI.TransactionExtention> getFreezeBalance2Method;
    if ((getFreezeBalance2Method = WalletGrpc.getFreezeBalance2Method) == null) {
      synchronized (WalletGrpc.class) {
        if ((getFreezeBalance2Method = WalletGrpc.getFreezeBalance2Method) == null) {
          WalletGrpc.getFreezeBalance2Method = getFreezeBalance2Method = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.FreezeBalanceContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "FreezeBalance2"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.FreezeBalanceContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("FreezeBalance2"))
                  .build();
          }
        }
     }
     return getFreezeBalance2Method;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getUnfreezeBalanceMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.UnfreezeBalanceContract,
      org.tron.protos.Protocol.Transaction> METHOD_UNFREEZE_BALANCE = getUnfreezeBalanceMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.UnfreezeBalanceContract,
      org.tron.protos.Protocol.Transaction> getUnfreezeBalanceMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.UnfreezeBalanceContract,
      org.tron.protos.Protocol.Transaction> getUnfreezeBalanceMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.UnfreezeBalanceContract, org.tron.protos.Protocol.Transaction> getUnfreezeBalanceMethod;
    if ((getUnfreezeBalanceMethod = WalletGrpc.getUnfreezeBalanceMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getUnfreezeBalanceMethod = WalletGrpc.getUnfreezeBalanceMethod) == null) {
          WalletGrpc.getUnfreezeBalanceMethod = getUnfreezeBalanceMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.UnfreezeBalanceContract, org.tron.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "UnfreezeBalance"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.UnfreezeBalanceContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("UnfreezeBalance"))
                  .build();
          }
        }
     }
     return getUnfreezeBalanceMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getUnfreezeBalance2Method()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.UnfreezeBalanceContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_UNFREEZE_BALANCE2 = getUnfreezeBalance2Method();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.UnfreezeBalanceContract,
      org.tron.api.GrpcAPI.TransactionExtention> getUnfreezeBalance2Method;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.UnfreezeBalanceContract,
      org.tron.api.GrpcAPI.TransactionExtention> getUnfreezeBalance2Method() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.UnfreezeBalanceContract, org.tron.api.GrpcAPI.TransactionExtention> getUnfreezeBalance2Method;
    if ((getUnfreezeBalance2Method = WalletGrpc.getUnfreezeBalance2Method) == null) {
      synchronized (WalletGrpc.class) {
        if ((getUnfreezeBalance2Method = WalletGrpc.getUnfreezeBalance2Method) == null) {
          WalletGrpc.getUnfreezeBalance2Method = getUnfreezeBalance2Method = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.UnfreezeBalanceContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "UnfreezeBalance2"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.UnfreezeBalanceContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("UnfreezeBalance2"))
                  .build();
          }
        }
     }
     return getUnfreezeBalance2Method;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getUnfreezeAssetMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.UnfreezeAssetContract,
      org.tron.protos.Protocol.Transaction> METHOD_UNFREEZE_ASSET = getUnfreezeAssetMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.UnfreezeAssetContract,
      org.tron.protos.Protocol.Transaction> getUnfreezeAssetMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.UnfreezeAssetContract,
      org.tron.protos.Protocol.Transaction> getUnfreezeAssetMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.UnfreezeAssetContract, org.tron.protos.Protocol.Transaction> getUnfreezeAssetMethod;
    if ((getUnfreezeAssetMethod = WalletGrpc.getUnfreezeAssetMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getUnfreezeAssetMethod = WalletGrpc.getUnfreezeAssetMethod) == null) {
          WalletGrpc.getUnfreezeAssetMethod = getUnfreezeAssetMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.UnfreezeAssetContract, org.tron.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "UnfreezeAsset"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.UnfreezeAssetContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("UnfreezeAsset"))
                  .build();
          }
        }
     }
     return getUnfreezeAssetMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getUnfreezeAsset2Method()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.UnfreezeAssetContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_UNFREEZE_ASSET2 = getUnfreezeAsset2Method();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.UnfreezeAssetContract,
      org.tron.api.GrpcAPI.TransactionExtention> getUnfreezeAsset2Method;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.UnfreezeAssetContract,
      org.tron.api.GrpcAPI.TransactionExtention> getUnfreezeAsset2Method() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.UnfreezeAssetContract, org.tron.api.GrpcAPI.TransactionExtention> getUnfreezeAsset2Method;
    if ((getUnfreezeAsset2Method = WalletGrpc.getUnfreezeAsset2Method) == null) {
      synchronized (WalletGrpc.class) {
        if ((getUnfreezeAsset2Method = WalletGrpc.getUnfreezeAsset2Method) == null) {
          WalletGrpc.getUnfreezeAsset2Method = getUnfreezeAsset2Method = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.UnfreezeAssetContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "UnfreezeAsset2"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.UnfreezeAssetContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("UnfreezeAsset2"))
                  .build();
          }
        }
     }
     return getUnfreezeAsset2Method;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getWithdrawBalanceMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.WithdrawBalanceContract,
      org.tron.protos.Protocol.Transaction> METHOD_WITHDRAW_BALANCE = getWithdrawBalanceMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.WithdrawBalanceContract,
      org.tron.protos.Protocol.Transaction> getWithdrawBalanceMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.WithdrawBalanceContract,
      org.tron.protos.Protocol.Transaction> getWithdrawBalanceMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.WithdrawBalanceContract, org.tron.protos.Protocol.Transaction> getWithdrawBalanceMethod;
    if ((getWithdrawBalanceMethod = WalletGrpc.getWithdrawBalanceMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getWithdrawBalanceMethod = WalletGrpc.getWithdrawBalanceMethod) == null) {
          WalletGrpc.getWithdrawBalanceMethod = getWithdrawBalanceMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.WithdrawBalanceContract, org.tron.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "WithdrawBalance"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.WithdrawBalanceContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("WithdrawBalance"))
                  .build();
          }
        }
     }
     return getWithdrawBalanceMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getWithdrawBalance2Method()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.WithdrawBalanceContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_WITHDRAW_BALANCE2 = getWithdrawBalance2Method();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.WithdrawBalanceContract,
      org.tron.api.GrpcAPI.TransactionExtention> getWithdrawBalance2Method;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.WithdrawBalanceContract,
      org.tron.api.GrpcAPI.TransactionExtention> getWithdrawBalance2Method() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.WithdrawBalanceContract, org.tron.api.GrpcAPI.TransactionExtention> getWithdrawBalance2Method;
    if ((getWithdrawBalance2Method = WalletGrpc.getWithdrawBalance2Method) == null) {
      synchronized (WalletGrpc.class) {
        if ((getWithdrawBalance2Method = WalletGrpc.getWithdrawBalance2Method) == null) {
          WalletGrpc.getWithdrawBalance2Method = getWithdrawBalance2Method = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.WithdrawBalanceContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "WithdrawBalance2"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.WithdrawBalanceContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("WithdrawBalance2"))
                  .build();
          }
        }
     }
     return getWithdrawBalance2Method;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getUpdateAssetMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.UpdateAssetContract,
      org.tron.protos.Protocol.Transaction> METHOD_UPDATE_ASSET = getUpdateAssetMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.UpdateAssetContract,
      org.tron.protos.Protocol.Transaction> getUpdateAssetMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.UpdateAssetContract,
      org.tron.protos.Protocol.Transaction> getUpdateAssetMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.UpdateAssetContract, org.tron.protos.Protocol.Transaction> getUpdateAssetMethod;
    if ((getUpdateAssetMethod = WalletGrpc.getUpdateAssetMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getUpdateAssetMethod = WalletGrpc.getUpdateAssetMethod) == null) {
          WalletGrpc.getUpdateAssetMethod = getUpdateAssetMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.UpdateAssetContract, org.tron.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "UpdateAsset"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.UpdateAssetContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("UpdateAsset"))
                  .build();
          }
        }
     }
     return getUpdateAssetMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getUpdateAsset2Method()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.UpdateAssetContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_UPDATE_ASSET2 = getUpdateAsset2Method();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.UpdateAssetContract,
      org.tron.api.GrpcAPI.TransactionExtention> getUpdateAsset2Method;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.UpdateAssetContract,
      org.tron.api.GrpcAPI.TransactionExtention> getUpdateAsset2Method() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.UpdateAssetContract, org.tron.api.GrpcAPI.TransactionExtention> getUpdateAsset2Method;
    if ((getUpdateAsset2Method = WalletGrpc.getUpdateAsset2Method) == null) {
      synchronized (WalletGrpc.class) {
        if ((getUpdateAsset2Method = WalletGrpc.getUpdateAsset2Method) == null) {
          WalletGrpc.getUpdateAsset2Method = getUpdateAsset2Method = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.UpdateAssetContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "UpdateAsset2"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.UpdateAssetContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("UpdateAsset2"))
                  .build();
          }
        }
     }
     return getUpdateAsset2Method;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getProposalCreateMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.ProposalCreateContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_PROPOSAL_CREATE = getProposalCreateMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.ProposalCreateContract,
      org.tron.api.GrpcAPI.TransactionExtention> getProposalCreateMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.ProposalCreateContract,
      org.tron.api.GrpcAPI.TransactionExtention> getProposalCreateMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.ProposalCreateContract, org.tron.api.GrpcAPI.TransactionExtention> getProposalCreateMethod;
    if ((getProposalCreateMethod = WalletGrpc.getProposalCreateMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getProposalCreateMethod = WalletGrpc.getProposalCreateMethod) == null) {
          WalletGrpc.getProposalCreateMethod = getProposalCreateMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.ProposalCreateContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "ProposalCreate"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.ProposalCreateContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("ProposalCreate"))
                  .build();
          }
        }
     }
     return getProposalCreateMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getProposalApproveMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.ProposalApproveContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_PROPOSAL_APPROVE = getProposalApproveMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.ProposalApproveContract,
      org.tron.api.GrpcAPI.TransactionExtention> getProposalApproveMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.ProposalApproveContract,
      org.tron.api.GrpcAPI.TransactionExtention> getProposalApproveMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.ProposalApproveContract, org.tron.api.GrpcAPI.TransactionExtention> getProposalApproveMethod;
    if ((getProposalApproveMethod = WalletGrpc.getProposalApproveMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getProposalApproveMethod = WalletGrpc.getProposalApproveMethod) == null) {
          WalletGrpc.getProposalApproveMethod = getProposalApproveMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.ProposalApproveContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "ProposalApprove"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.ProposalApproveContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("ProposalApprove"))
                  .build();
          }
        }
     }
     return getProposalApproveMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getProposalDeleteMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.ProposalDeleteContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_PROPOSAL_DELETE = getProposalDeleteMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.ProposalDeleteContract,
      org.tron.api.GrpcAPI.TransactionExtention> getProposalDeleteMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.ProposalDeleteContract,
      org.tron.api.GrpcAPI.TransactionExtention> getProposalDeleteMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.ProposalDeleteContract, org.tron.api.GrpcAPI.TransactionExtention> getProposalDeleteMethod;
    if ((getProposalDeleteMethod = WalletGrpc.getProposalDeleteMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getProposalDeleteMethod = WalletGrpc.getProposalDeleteMethod) == null) {
          WalletGrpc.getProposalDeleteMethod = getProposalDeleteMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.ProposalDeleteContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "ProposalDelete"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.ProposalDeleteContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("ProposalDelete"))
                  .build();
          }
        }
     }
     return getProposalDeleteMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getBuyStorageMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.BuyStorageContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_BUY_STORAGE = getBuyStorageMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.BuyStorageContract,
      org.tron.api.GrpcAPI.TransactionExtention> getBuyStorageMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.BuyStorageContract,
      org.tron.api.GrpcAPI.TransactionExtention> getBuyStorageMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.BuyStorageContract, org.tron.api.GrpcAPI.TransactionExtention> getBuyStorageMethod;
    if ((getBuyStorageMethod = WalletGrpc.getBuyStorageMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getBuyStorageMethod = WalletGrpc.getBuyStorageMethod) == null) {
          WalletGrpc.getBuyStorageMethod = getBuyStorageMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.BuyStorageContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "BuyStorage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.BuyStorageContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("BuyStorage"))
                  .build();
          }
        }
     }
     return getBuyStorageMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getBuyStorageBytesMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.BuyStorageBytesContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_BUY_STORAGE_BYTES = getBuyStorageBytesMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.BuyStorageBytesContract,
      org.tron.api.GrpcAPI.TransactionExtention> getBuyStorageBytesMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.BuyStorageBytesContract,
      org.tron.api.GrpcAPI.TransactionExtention> getBuyStorageBytesMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.BuyStorageBytesContract, org.tron.api.GrpcAPI.TransactionExtention> getBuyStorageBytesMethod;
    if ((getBuyStorageBytesMethod = WalletGrpc.getBuyStorageBytesMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getBuyStorageBytesMethod = WalletGrpc.getBuyStorageBytesMethod) == null) {
          WalletGrpc.getBuyStorageBytesMethod = getBuyStorageBytesMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.BuyStorageBytesContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "BuyStorageBytes"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.BuyStorageBytesContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("BuyStorageBytes"))
                  .build();
          }
        }
     }
     return getBuyStorageBytesMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getSellStorageMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.SellStorageContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_SELL_STORAGE = getSellStorageMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.SellStorageContract,
      org.tron.api.GrpcAPI.TransactionExtention> getSellStorageMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.SellStorageContract,
      org.tron.api.GrpcAPI.TransactionExtention> getSellStorageMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.SellStorageContract, org.tron.api.GrpcAPI.TransactionExtention> getSellStorageMethod;
    if ((getSellStorageMethod = WalletGrpc.getSellStorageMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getSellStorageMethod = WalletGrpc.getSellStorageMethod) == null) {
          WalletGrpc.getSellStorageMethod = getSellStorageMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.SellStorageContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "SellStorage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.SellStorageContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("SellStorage"))
                  .build();
          }
        }
     }
     return getSellStorageMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getExchangeCreateMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.ExchangeCreateContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_EXCHANGE_CREATE = getExchangeCreateMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.ExchangeCreateContract,
      org.tron.api.GrpcAPI.TransactionExtention> getExchangeCreateMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.ExchangeCreateContract,
      org.tron.api.GrpcAPI.TransactionExtention> getExchangeCreateMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.ExchangeCreateContract, org.tron.api.GrpcAPI.TransactionExtention> getExchangeCreateMethod;
    if ((getExchangeCreateMethod = WalletGrpc.getExchangeCreateMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getExchangeCreateMethod = WalletGrpc.getExchangeCreateMethod) == null) {
          WalletGrpc.getExchangeCreateMethod = getExchangeCreateMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.ExchangeCreateContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "ExchangeCreate"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.ExchangeCreateContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("ExchangeCreate"))
                  .build();
          }
        }
     }
     return getExchangeCreateMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getExchangeInjectMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.ExchangeInjectContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_EXCHANGE_INJECT = getExchangeInjectMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.ExchangeInjectContract,
      org.tron.api.GrpcAPI.TransactionExtention> getExchangeInjectMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.ExchangeInjectContract,
      org.tron.api.GrpcAPI.TransactionExtention> getExchangeInjectMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.ExchangeInjectContract, org.tron.api.GrpcAPI.TransactionExtention> getExchangeInjectMethod;
    if ((getExchangeInjectMethod = WalletGrpc.getExchangeInjectMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getExchangeInjectMethod = WalletGrpc.getExchangeInjectMethod) == null) {
          WalletGrpc.getExchangeInjectMethod = getExchangeInjectMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.ExchangeInjectContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "ExchangeInject"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.ExchangeInjectContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("ExchangeInject"))
                  .build();
          }
        }
     }
     return getExchangeInjectMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getExchangeWithdrawMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.ExchangeWithdrawContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_EXCHANGE_WITHDRAW = getExchangeWithdrawMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.ExchangeWithdrawContract,
      org.tron.api.GrpcAPI.TransactionExtention> getExchangeWithdrawMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.ExchangeWithdrawContract,
      org.tron.api.GrpcAPI.TransactionExtention> getExchangeWithdrawMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.ExchangeWithdrawContract, org.tron.api.GrpcAPI.TransactionExtention> getExchangeWithdrawMethod;
    if ((getExchangeWithdrawMethod = WalletGrpc.getExchangeWithdrawMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getExchangeWithdrawMethod = WalletGrpc.getExchangeWithdrawMethod) == null) {
          WalletGrpc.getExchangeWithdrawMethod = getExchangeWithdrawMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.ExchangeWithdrawContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "ExchangeWithdraw"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.ExchangeWithdrawContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("ExchangeWithdraw"))
                  .build();
          }
        }
     }
     return getExchangeWithdrawMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getExchangeTransactionMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.ExchangeTransactionContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_EXCHANGE_TRANSACTION = getExchangeTransactionMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.ExchangeTransactionContract,
      org.tron.api.GrpcAPI.TransactionExtention> getExchangeTransactionMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.ExchangeTransactionContract,
      org.tron.api.GrpcAPI.TransactionExtention> getExchangeTransactionMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.ExchangeTransactionContract, org.tron.api.GrpcAPI.TransactionExtention> getExchangeTransactionMethod;
    if ((getExchangeTransactionMethod = WalletGrpc.getExchangeTransactionMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getExchangeTransactionMethod = WalletGrpc.getExchangeTransactionMethod) == null) {
          WalletGrpc.getExchangeTransactionMethod = getExchangeTransactionMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.ExchangeTransactionContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "ExchangeTransaction"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.ExchangeTransactionContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("ExchangeTransaction"))
                  .build();
          }
        }
     }
     return getExchangeTransactionMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getListNodesMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.api.GrpcAPI.NodeList> METHOD_LIST_NODES = getListNodesMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.api.GrpcAPI.NodeList> getListNodesMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.api.GrpcAPI.NodeList> getListNodesMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage, org.tron.api.GrpcAPI.NodeList> getListNodesMethod;
    if ((getListNodesMethod = WalletGrpc.getListNodesMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getListNodesMethod = WalletGrpc.getListNodesMethod) == null) {
          WalletGrpc.getListNodesMethod = getListNodesMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.EmptyMessage, org.tron.api.GrpcAPI.NodeList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "ListNodes"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.NodeList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("ListNodes"))
                  .build();
          }
        }
     }
     return getListNodesMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetAssetIssueByAccountMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Protocol.Account,
      org.tron.api.GrpcAPI.AssetIssueList> METHOD_GET_ASSET_ISSUE_BY_ACCOUNT = getGetAssetIssueByAccountMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Protocol.Account,
      org.tron.api.GrpcAPI.AssetIssueList> getGetAssetIssueByAccountMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Protocol.Account,
      org.tron.api.GrpcAPI.AssetIssueList> getGetAssetIssueByAccountMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Protocol.Account, org.tron.api.GrpcAPI.AssetIssueList> getGetAssetIssueByAccountMethod;
    if ((getGetAssetIssueByAccountMethod = WalletGrpc.getGetAssetIssueByAccountMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetAssetIssueByAccountMethod = WalletGrpc.getGetAssetIssueByAccountMethod) == null) {
          WalletGrpc.getGetAssetIssueByAccountMethod = getGetAssetIssueByAccountMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Protocol.Account, org.tron.api.GrpcAPI.AssetIssueList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetAssetIssueByAccount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Account.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.AssetIssueList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetAssetIssueByAccount"))
                  .build();
          }
        }
     }
     return getGetAssetIssueByAccountMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetAccountNetMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Protocol.Account,
      org.tron.api.GrpcAPI.AccountNetMessage> METHOD_GET_ACCOUNT_NET = getGetAccountNetMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Protocol.Account,
      org.tron.api.GrpcAPI.AccountNetMessage> getGetAccountNetMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Protocol.Account,
      org.tron.api.GrpcAPI.AccountNetMessage> getGetAccountNetMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Protocol.Account, org.tron.api.GrpcAPI.AccountNetMessage> getGetAccountNetMethod;
    if ((getGetAccountNetMethod = WalletGrpc.getGetAccountNetMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetAccountNetMethod = WalletGrpc.getGetAccountNetMethod) == null) {
          WalletGrpc.getGetAccountNetMethod = getGetAccountNetMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Protocol.Account, org.tron.api.GrpcAPI.AccountNetMessage>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetAccountNet"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Account.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.AccountNetMessage.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetAccountNet"))
                  .build();
          }
        }
     }
     return getGetAccountNetMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetAccountResourceMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Protocol.Account,
      org.tron.api.GrpcAPI.AccountResourceMessage> METHOD_GET_ACCOUNT_RESOURCE = getGetAccountResourceMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Protocol.Account,
      org.tron.api.GrpcAPI.AccountResourceMessage> getGetAccountResourceMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Protocol.Account,
      org.tron.api.GrpcAPI.AccountResourceMessage> getGetAccountResourceMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Protocol.Account, org.tron.api.GrpcAPI.AccountResourceMessage> getGetAccountResourceMethod;
    if ((getGetAccountResourceMethod = WalletGrpc.getGetAccountResourceMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetAccountResourceMethod = WalletGrpc.getGetAccountResourceMethod) == null) {
          WalletGrpc.getGetAccountResourceMethod = getGetAccountResourceMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Protocol.Account, org.tron.api.GrpcAPI.AccountResourceMessage>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetAccountResource"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Account.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.AccountResourceMessage.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetAccountResource"))
                  .build();
          }
        }
     }
     return getGetAccountResourceMethod;
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
    if ((getGetAssetIssueByNameMethod = WalletGrpc.getGetAssetIssueByNameMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetAssetIssueByNameMethod = WalletGrpc.getGetAssetIssueByNameMethod) == null) {
          WalletGrpc.getGetAssetIssueByNameMethod = getGetAssetIssueByNameMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.BytesMessage, org.tron.protos.Contract.AssetIssueContract>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetAssetIssueByName"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.AssetIssueContract.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetAssetIssueByName"))
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
    if ((getGetAssetIssueListByNameMethod = WalletGrpc.getGetAssetIssueListByNameMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetAssetIssueListByNameMethod = WalletGrpc.getGetAssetIssueListByNameMethod) == null) {
          WalletGrpc.getGetAssetIssueListByNameMethod = getGetAssetIssueListByNameMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.BytesMessage, org.tron.api.GrpcAPI.AssetIssueList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetAssetIssueListByName"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.AssetIssueList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetAssetIssueListByName"))
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
    if ((getGetAssetIssueByIdMethod = WalletGrpc.getGetAssetIssueByIdMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetAssetIssueByIdMethod = WalletGrpc.getGetAssetIssueByIdMethod) == null) {
          WalletGrpc.getGetAssetIssueByIdMethod = getGetAssetIssueByIdMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.BytesMessage, org.tron.protos.Contract.AssetIssueContract>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetAssetIssueById"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.AssetIssueContract.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetAssetIssueById"))
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
    if ((getGetNowBlockMethod = WalletGrpc.getGetNowBlockMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetNowBlockMethod = WalletGrpc.getGetNowBlockMethod) == null) {
          WalletGrpc.getGetNowBlockMethod = getGetNowBlockMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.EmptyMessage, org.tron.protos.Protocol.Block>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetNowBlock"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Block.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetNowBlock"))
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
    if ((getGetNowBlock2Method = WalletGrpc.getGetNowBlock2Method) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetNowBlock2Method = WalletGrpc.getGetNowBlock2Method) == null) {
          WalletGrpc.getGetNowBlock2Method = getGetNowBlock2Method = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.EmptyMessage, org.tron.api.GrpcAPI.BlockExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetNowBlock2"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BlockExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetNowBlock2"))
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
    if ((getGetBlockByNumMethod = WalletGrpc.getGetBlockByNumMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetBlockByNumMethod = WalletGrpc.getGetBlockByNumMethod) == null) {
          WalletGrpc.getGetBlockByNumMethod = getGetBlockByNumMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.NumberMessage, org.tron.protos.Protocol.Block>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetBlockByNum"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.NumberMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Block.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetBlockByNum"))
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
    if ((getGetBlockByNum2Method = WalletGrpc.getGetBlockByNum2Method) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetBlockByNum2Method = WalletGrpc.getGetBlockByNum2Method) == null) {
          WalletGrpc.getGetBlockByNum2Method = getGetBlockByNum2Method = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.NumberMessage, org.tron.api.GrpcAPI.BlockExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetBlockByNum2"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.NumberMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BlockExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetBlockByNum2"))
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
    if ((getGetTransactionCountByBlockNumMethod = WalletGrpc.getGetTransactionCountByBlockNumMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetTransactionCountByBlockNumMethod = WalletGrpc.getGetTransactionCountByBlockNumMethod) == null) {
          WalletGrpc.getGetTransactionCountByBlockNumMethod = getGetTransactionCountByBlockNumMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.NumberMessage, org.tron.api.GrpcAPI.NumberMessage>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetTransactionCountByBlockNum"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.NumberMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.NumberMessage.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetTransactionCountByBlockNum"))
                  .build();
          }
        }
     }
     return getGetTransactionCountByBlockNumMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetBlockByIdMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.protos.Protocol.Block> METHOD_GET_BLOCK_BY_ID = getGetBlockByIdMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.protos.Protocol.Block> getGetBlockByIdMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.protos.Protocol.Block> getGetBlockByIdMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage, org.tron.protos.Protocol.Block> getGetBlockByIdMethod;
    if ((getGetBlockByIdMethod = WalletGrpc.getGetBlockByIdMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetBlockByIdMethod = WalletGrpc.getGetBlockByIdMethod) == null) {
          WalletGrpc.getGetBlockByIdMethod = getGetBlockByIdMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.BytesMessage, org.tron.protos.Protocol.Block>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetBlockById"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Block.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetBlockById"))
                  .build();
          }
        }
     }
     return getGetBlockByIdMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetBlockByLimitNextMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BlockLimit,
      org.tron.api.GrpcAPI.BlockList> METHOD_GET_BLOCK_BY_LIMIT_NEXT = getGetBlockByLimitNextMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BlockLimit,
      org.tron.api.GrpcAPI.BlockList> getGetBlockByLimitNextMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BlockLimit,
      org.tron.api.GrpcAPI.BlockList> getGetBlockByLimitNextMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BlockLimit, org.tron.api.GrpcAPI.BlockList> getGetBlockByLimitNextMethod;
    if ((getGetBlockByLimitNextMethod = WalletGrpc.getGetBlockByLimitNextMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetBlockByLimitNextMethod = WalletGrpc.getGetBlockByLimitNextMethod) == null) {
          WalletGrpc.getGetBlockByLimitNextMethod = getGetBlockByLimitNextMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.BlockLimit, org.tron.api.GrpcAPI.BlockList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetBlockByLimitNext"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BlockLimit.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BlockList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetBlockByLimitNext"))
                  .build();
          }
        }
     }
     return getGetBlockByLimitNextMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetBlockByLimitNext2Method()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BlockLimit,
      org.tron.api.GrpcAPI.BlockListExtention> METHOD_GET_BLOCK_BY_LIMIT_NEXT2 = getGetBlockByLimitNext2Method();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BlockLimit,
      org.tron.api.GrpcAPI.BlockListExtention> getGetBlockByLimitNext2Method;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BlockLimit,
      org.tron.api.GrpcAPI.BlockListExtention> getGetBlockByLimitNext2Method() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BlockLimit, org.tron.api.GrpcAPI.BlockListExtention> getGetBlockByLimitNext2Method;
    if ((getGetBlockByLimitNext2Method = WalletGrpc.getGetBlockByLimitNext2Method) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetBlockByLimitNext2Method = WalletGrpc.getGetBlockByLimitNext2Method) == null) {
          WalletGrpc.getGetBlockByLimitNext2Method = getGetBlockByLimitNext2Method = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.BlockLimit, org.tron.api.GrpcAPI.BlockListExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetBlockByLimitNext2"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BlockLimit.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BlockListExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetBlockByLimitNext2"))
                  .build();
          }
        }
     }
     return getGetBlockByLimitNext2Method;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetBlockByLatestNumMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.NumberMessage,
      org.tron.api.GrpcAPI.BlockList> METHOD_GET_BLOCK_BY_LATEST_NUM = getGetBlockByLatestNumMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.NumberMessage,
      org.tron.api.GrpcAPI.BlockList> getGetBlockByLatestNumMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.NumberMessage,
      org.tron.api.GrpcAPI.BlockList> getGetBlockByLatestNumMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.NumberMessage, org.tron.api.GrpcAPI.BlockList> getGetBlockByLatestNumMethod;
    if ((getGetBlockByLatestNumMethod = WalletGrpc.getGetBlockByLatestNumMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetBlockByLatestNumMethod = WalletGrpc.getGetBlockByLatestNumMethod) == null) {
          WalletGrpc.getGetBlockByLatestNumMethod = getGetBlockByLatestNumMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.NumberMessage, org.tron.api.GrpcAPI.BlockList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetBlockByLatestNum"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.NumberMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BlockList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetBlockByLatestNum"))
                  .build();
          }
        }
     }
     return getGetBlockByLatestNumMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetBlockByLatestNum2Method()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.NumberMessage,
      org.tron.api.GrpcAPI.BlockListExtention> METHOD_GET_BLOCK_BY_LATEST_NUM2 = getGetBlockByLatestNum2Method();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.NumberMessage,
      org.tron.api.GrpcAPI.BlockListExtention> getGetBlockByLatestNum2Method;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.NumberMessage,
      org.tron.api.GrpcAPI.BlockListExtention> getGetBlockByLatestNum2Method() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.NumberMessage, org.tron.api.GrpcAPI.BlockListExtention> getGetBlockByLatestNum2Method;
    if ((getGetBlockByLatestNum2Method = WalletGrpc.getGetBlockByLatestNum2Method) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetBlockByLatestNum2Method = WalletGrpc.getGetBlockByLatestNum2Method) == null) {
          WalletGrpc.getGetBlockByLatestNum2Method = getGetBlockByLatestNum2Method = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.NumberMessage, org.tron.api.GrpcAPI.BlockListExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetBlockByLatestNum2"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.NumberMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BlockListExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetBlockByLatestNum2"))
                  .build();
          }
        }
     }
     return getGetBlockByLatestNum2Method;
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
    if ((getGetTransactionByIdMethod = WalletGrpc.getGetTransactionByIdMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetTransactionByIdMethod = WalletGrpc.getGetTransactionByIdMethod) == null) {
          WalletGrpc.getGetTransactionByIdMethod = getGetTransactionByIdMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.BytesMessage, org.tron.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetTransactionById"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetTransactionById"))
                  .build();
          }
        }
     }
     return getGetTransactionByIdMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getDeployContractMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.CreateSmartContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_DEPLOY_CONTRACT = getDeployContractMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.CreateSmartContract,
      org.tron.api.GrpcAPI.TransactionExtention> getDeployContractMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.CreateSmartContract,
      org.tron.api.GrpcAPI.TransactionExtention> getDeployContractMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.CreateSmartContract, org.tron.api.GrpcAPI.TransactionExtention> getDeployContractMethod;
    if ((getDeployContractMethod = WalletGrpc.getDeployContractMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getDeployContractMethod = WalletGrpc.getDeployContractMethod) == null) {
          WalletGrpc.getDeployContractMethod = getDeployContractMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.CreateSmartContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "DeployContract"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.CreateSmartContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("DeployContract"))
                  .build();
          }
        }
     }
     return getDeployContractMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetContractMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.protos.Protocol.SmartContract> METHOD_GET_CONTRACT = getGetContractMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.protos.Protocol.SmartContract> getGetContractMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.protos.Protocol.SmartContract> getGetContractMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage, org.tron.protos.Protocol.SmartContract> getGetContractMethod;
    if ((getGetContractMethod = WalletGrpc.getGetContractMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetContractMethod = WalletGrpc.getGetContractMethod) == null) {
          WalletGrpc.getGetContractMethod = getGetContractMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.BytesMessage, org.tron.protos.Protocol.SmartContract>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetContract"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.SmartContract.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetContract"))
                  .build();
          }
        }
     }
     return getGetContractMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getTriggerContractMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.TriggerSmartContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_TRIGGER_CONTRACT = getTriggerContractMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.TriggerSmartContract,
      org.tron.api.GrpcAPI.TransactionExtention> getTriggerContractMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.TriggerSmartContract,
      org.tron.api.GrpcAPI.TransactionExtention> getTriggerContractMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.TriggerSmartContract, org.tron.api.GrpcAPI.TransactionExtention> getTriggerContractMethod;
    if ((getTriggerContractMethod = WalletGrpc.getTriggerContractMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getTriggerContractMethod = WalletGrpc.getTriggerContractMethod) == null) {
          WalletGrpc.getTriggerContractMethod = getTriggerContractMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.TriggerSmartContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "TriggerContract"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.TriggerSmartContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("TriggerContract"))
                  .build();
          }
        }
     }
     return getTriggerContractMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getTriggerConstantContractMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.TriggerSmartContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_TRIGGER_CONSTANT_CONTRACT = getTriggerConstantContractMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.TriggerSmartContract,
      org.tron.api.GrpcAPI.TransactionExtention> getTriggerConstantContractMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.TriggerSmartContract,
      org.tron.api.GrpcAPI.TransactionExtention> getTriggerConstantContractMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.TriggerSmartContract, org.tron.api.GrpcAPI.TransactionExtention> getTriggerConstantContractMethod;
    if ((getTriggerConstantContractMethod = WalletGrpc.getTriggerConstantContractMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getTriggerConstantContractMethod = WalletGrpc.getTriggerConstantContractMethod) == null) {
          WalletGrpc.getTriggerConstantContractMethod = getTriggerConstantContractMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.TriggerSmartContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "TriggerConstantContract"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.TriggerSmartContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("TriggerConstantContract"))
                  .build();
          }
        }
     }
     return getTriggerConstantContractMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getClearContractABIMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.ClearABIContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_CLEAR_CONTRACT_ABI = getClearContractABIMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.ClearABIContract,
      org.tron.api.GrpcAPI.TransactionExtention> getClearContractABIMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.ClearABIContract,
      org.tron.api.GrpcAPI.TransactionExtention> getClearContractABIMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.ClearABIContract, org.tron.api.GrpcAPI.TransactionExtention> getClearContractABIMethod;
    if ((getClearContractABIMethod = WalletGrpc.getClearContractABIMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getClearContractABIMethod = WalletGrpc.getClearContractABIMethod) == null) {
          WalletGrpc.getClearContractABIMethod = getClearContractABIMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.ClearABIContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "ClearContractABI"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.ClearABIContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("ClearContractABI"))
                  .build();
          }
        }
     }
     return getClearContractABIMethod;
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
    if ((getListWitnessesMethod = WalletGrpc.getListWitnessesMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getListWitnessesMethod = WalletGrpc.getListWitnessesMethod) == null) {
          WalletGrpc.getListWitnessesMethod = getListWitnessesMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.EmptyMessage, org.tron.api.GrpcAPI.WitnessList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "ListWitnesses"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.WitnessList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("ListWitnesses"))
                  .build();
          }
        }
     }
     return getListWitnessesMethod;
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
    if ((getGetDelegatedResourceMethod = WalletGrpc.getGetDelegatedResourceMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetDelegatedResourceMethod = WalletGrpc.getGetDelegatedResourceMethod) == null) {
          WalletGrpc.getGetDelegatedResourceMethod = getGetDelegatedResourceMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.DelegatedResourceMessage, org.tron.api.GrpcAPI.DelegatedResourceList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetDelegatedResource"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.DelegatedResourceMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.DelegatedResourceList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetDelegatedResource"))
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
    if ((getGetDelegatedResourceAccountIndexMethod = WalletGrpc.getGetDelegatedResourceAccountIndexMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetDelegatedResourceAccountIndexMethod = WalletGrpc.getGetDelegatedResourceAccountIndexMethod) == null) {
          WalletGrpc.getGetDelegatedResourceAccountIndexMethod = getGetDelegatedResourceAccountIndexMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.BytesMessage, org.tron.protos.Protocol.DelegatedResourceAccountIndex>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetDelegatedResourceAccountIndex"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.DelegatedResourceAccountIndex.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetDelegatedResourceAccountIndex"))
                  .build();
          }
        }
     }
     return getGetDelegatedResourceAccountIndexMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getListProposalsMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.api.GrpcAPI.ProposalList> METHOD_LIST_PROPOSALS = getListProposalsMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.api.GrpcAPI.ProposalList> getListProposalsMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.api.GrpcAPI.ProposalList> getListProposalsMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage, org.tron.api.GrpcAPI.ProposalList> getListProposalsMethod;
    if ((getListProposalsMethod = WalletGrpc.getListProposalsMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getListProposalsMethod = WalletGrpc.getListProposalsMethod) == null) {
          WalletGrpc.getListProposalsMethod = getListProposalsMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.EmptyMessage, org.tron.api.GrpcAPI.ProposalList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "ListProposals"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.ProposalList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("ListProposals"))
                  .build();
          }
        }
     }
     return getListProposalsMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetPaginatedProposalListMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.PaginatedMessage,
      org.tron.api.GrpcAPI.ProposalList> METHOD_GET_PAGINATED_PROPOSAL_LIST = getGetPaginatedProposalListMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.PaginatedMessage,
      org.tron.api.GrpcAPI.ProposalList> getGetPaginatedProposalListMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.PaginatedMessage,
      org.tron.api.GrpcAPI.ProposalList> getGetPaginatedProposalListMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.PaginatedMessage, org.tron.api.GrpcAPI.ProposalList> getGetPaginatedProposalListMethod;
    if ((getGetPaginatedProposalListMethod = WalletGrpc.getGetPaginatedProposalListMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetPaginatedProposalListMethod = WalletGrpc.getGetPaginatedProposalListMethod) == null) {
          WalletGrpc.getGetPaginatedProposalListMethod = getGetPaginatedProposalListMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.PaginatedMessage, org.tron.api.GrpcAPI.ProposalList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetPaginatedProposalList"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.PaginatedMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.ProposalList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetPaginatedProposalList"))
                  .build();
          }
        }
     }
     return getGetPaginatedProposalListMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetProposalByIdMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.protos.Protocol.Proposal> METHOD_GET_PROPOSAL_BY_ID = getGetProposalByIdMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.protos.Protocol.Proposal> getGetProposalByIdMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.protos.Protocol.Proposal> getGetProposalByIdMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage, org.tron.protos.Protocol.Proposal> getGetProposalByIdMethod;
    if ((getGetProposalByIdMethod = WalletGrpc.getGetProposalByIdMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetProposalByIdMethod = WalletGrpc.getGetProposalByIdMethod) == null) {
          WalletGrpc.getGetProposalByIdMethod = getGetProposalByIdMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.BytesMessage, org.tron.protos.Protocol.Proposal>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetProposalById"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Proposal.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetProposalById"))
                  .build();
          }
        }
     }
     return getGetProposalByIdMethod;
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
    if ((getListExchangesMethod = WalletGrpc.getListExchangesMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getListExchangesMethod = WalletGrpc.getListExchangesMethod) == null) {
          WalletGrpc.getListExchangesMethod = getListExchangesMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.EmptyMessage, org.tron.api.GrpcAPI.ExchangeList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "ListExchanges"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.ExchangeList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("ListExchanges"))
                  .build();
          }
        }
     }
     return getListExchangesMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetPaginatedExchangeListMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.PaginatedMessage,
      org.tron.api.GrpcAPI.ExchangeList> METHOD_GET_PAGINATED_EXCHANGE_LIST = getGetPaginatedExchangeListMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.PaginatedMessage,
      org.tron.api.GrpcAPI.ExchangeList> getGetPaginatedExchangeListMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.PaginatedMessage,
      org.tron.api.GrpcAPI.ExchangeList> getGetPaginatedExchangeListMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.PaginatedMessage, org.tron.api.GrpcAPI.ExchangeList> getGetPaginatedExchangeListMethod;
    if ((getGetPaginatedExchangeListMethod = WalletGrpc.getGetPaginatedExchangeListMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetPaginatedExchangeListMethod = WalletGrpc.getGetPaginatedExchangeListMethod) == null) {
          WalletGrpc.getGetPaginatedExchangeListMethod = getGetPaginatedExchangeListMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.PaginatedMessage, org.tron.api.GrpcAPI.ExchangeList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetPaginatedExchangeList"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.PaginatedMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.ExchangeList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetPaginatedExchangeList"))
                  .build();
          }
        }
     }
     return getGetPaginatedExchangeListMethod;
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
    if ((getGetExchangeByIdMethod = WalletGrpc.getGetExchangeByIdMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetExchangeByIdMethod = WalletGrpc.getGetExchangeByIdMethod) == null) {
          WalletGrpc.getGetExchangeByIdMethod = getGetExchangeByIdMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.BytesMessage, org.tron.protos.Protocol.Exchange>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetExchangeById"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Exchange.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetExchangeById"))
                  .build();
          }
        }
     }
     return getGetExchangeByIdMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetChainParametersMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.protos.Protocol.ChainParameters> METHOD_GET_CHAIN_PARAMETERS = getGetChainParametersMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.protos.Protocol.ChainParameters> getGetChainParametersMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.protos.Protocol.ChainParameters> getGetChainParametersMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage, org.tron.protos.Protocol.ChainParameters> getGetChainParametersMethod;
    if ((getGetChainParametersMethod = WalletGrpc.getGetChainParametersMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetChainParametersMethod = WalletGrpc.getGetChainParametersMethod) == null) {
          WalletGrpc.getGetChainParametersMethod = getGetChainParametersMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.EmptyMessage, org.tron.protos.Protocol.ChainParameters>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetChainParameters"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.ChainParameters.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetChainParameters"))
                  .build();
          }
        }
     }
     return getGetChainParametersMethod;
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
    if ((getGetAssetIssueListMethod = WalletGrpc.getGetAssetIssueListMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetAssetIssueListMethod = WalletGrpc.getGetAssetIssueListMethod) == null) {
          WalletGrpc.getGetAssetIssueListMethod = getGetAssetIssueListMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.EmptyMessage, org.tron.api.GrpcAPI.AssetIssueList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetAssetIssueList"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.AssetIssueList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetAssetIssueList"))
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
    if ((getGetPaginatedAssetIssueListMethod = WalletGrpc.getGetPaginatedAssetIssueListMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetPaginatedAssetIssueListMethod = WalletGrpc.getGetPaginatedAssetIssueListMethod) == null) {
          WalletGrpc.getGetPaginatedAssetIssueListMethod = getGetPaginatedAssetIssueListMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.PaginatedMessage, org.tron.api.GrpcAPI.AssetIssueList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetPaginatedAssetIssueList"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.PaginatedMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.AssetIssueList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetPaginatedAssetIssueList"))
                  .build();
          }
        }
     }
     return getGetPaginatedAssetIssueListMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getTotalTransactionMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.api.GrpcAPI.NumberMessage> METHOD_TOTAL_TRANSACTION = getTotalTransactionMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.api.GrpcAPI.NumberMessage> getTotalTransactionMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.api.GrpcAPI.NumberMessage> getTotalTransactionMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage, org.tron.api.GrpcAPI.NumberMessage> getTotalTransactionMethod;
    if ((getTotalTransactionMethod = WalletGrpc.getTotalTransactionMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getTotalTransactionMethod = WalletGrpc.getTotalTransactionMethod) == null) {
          WalletGrpc.getTotalTransactionMethod = getTotalTransactionMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.EmptyMessage, org.tron.api.GrpcAPI.NumberMessage>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "TotalTransaction"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.NumberMessage.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("TotalTransaction"))
                  .build();
          }
        }
     }
     return getTotalTransactionMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetNextMaintenanceTimeMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.api.GrpcAPI.NumberMessage> METHOD_GET_NEXT_MAINTENANCE_TIME = getGetNextMaintenanceTimeMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.api.GrpcAPI.NumberMessage> getGetNextMaintenanceTimeMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.api.GrpcAPI.NumberMessage> getGetNextMaintenanceTimeMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage, org.tron.api.GrpcAPI.NumberMessage> getGetNextMaintenanceTimeMethod;
    if ((getGetNextMaintenanceTimeMethod = WalletGrpc.getGetNextMaintenanceTimeMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetNextMaintenanceTimeMethod = WalletGrpc.getGetNextMaintenanceTimeMethod) == null) {
          WalletGrpc.getGetNextMaintenanceTimeMethod = getGetNextMaintenanceTimeMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.EmptyMessage, org.tron.api.GrpcAPI.NumberMessage>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetNextMaintenanceTime"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.NumberMessage.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetNextMaintenanceTime"))
                  .build();
          }
        }
     }
     return getGetNextMaintenanceTimeMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetTransactionSignMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Protocol.TransactionSign,
      org.tron.protos.Protocol.Transaction> METHOD_GET_TRANSACTION_SIGN = getGetTransactionSignMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Protocol.TransactionSign,
      org.tron.protos.Protocol.Transaction> getGetTransactionSignMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Protocol.TransactionSign,
      org.tron.protos.Protocol.Transaction> getGetTransactionSignMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Protocol.TransactionSign, org.tron.protos.Protocol.Transaction> getGetTransactionSignMethod;
    if ((getGetTransactionSignMethod = WalletGrpc.getGetTransactionSignMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetTransactionSignMethod = WalletGrpc.getGetTransactionSignMethod) == null) {
          WalletGrpc.getGetTransactionSignMethod = getGetTransactionSignMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Protocol.TransactionSign, org.tron.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetTransactionSign"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.TransactionSign.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetTransactionSign"))
                  .build();
          }
        }
     }
     return getGetTransactionSignMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetTransactionSign2Method()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Protocol.TransactionSign,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_GET_TRANSACTION_SIGN2 = getGetTransactionSign2Method();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Protocol.TransactionSign,
      org.tron.api.GrpcAPI.TransactionExtention> getGetTransactionSign2Method;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Protocol.TransactionSign,
      org.tron.api.GrpcAPI.TransactionExtention> getGetTransactionSign2Method() {
    io.grpc.MethodDescriptor<org.tron.protos.Protocol.TransactionSign, org.tron.api.GrpcAPI.TransactionExtention> getGetTransactionSign2Method;
    if ((getGetTransactionSign2Method = WalletGrpc.getGetTransactionSign2Method) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetTransactionSign2Method = WalletGrpc.getGetTransactionSign2Method) == null) {
          WalletGrpc.getGetTransactionSign2Method = getGetTransactionSign2Method = 
              io.grpc.MethodDescriptor.<org.tron.protos.Protocol.TransactionSign, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetTransactionSign2"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.TransactionSign.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetTransactionSign2"))
                  .build();
          }
        }
     }
     return getGetTransactionSign2Method;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getCreateAddressMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.api.GrpcAPI.BytesMessage> METHOD_CREATE_ADDRESS = getCreateAddressMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.api.GrpcAPI.BytesMessage> getCreateAddressMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage,
      org.tron.api.GrpcAPI.BytesMessage> getCreateAddressMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.BytesMessage, org.tron.api.GrpcAPI.BytesMessage> getCreateAddressMethod;
    if ((getCreateAddressMethod = WalletGrpc.getCreateAddressMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getCreateAddressMethod = WalletGrpc.getCreateAddressMethod) == null) {
          WalletGrpc.getCreateAddressMethod = getCreateAddressMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.BytesMessage, org.tron.api.GrpcAPI.BytesMessage>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "CreateAddress"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BytesMessage.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("CreateAddress"))
                  .build();
          }
        }
     }
     return getCreateAddressMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getEasyTransferAssetMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EasyTransferAssetMessage,
      org.tron.api.GrpcAPI.EasyTransferResponse> METHOD_EASY_TRANSFER_ASSET = getEasyTransferAssetMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EasyTransferAssetMessage,
      org.tron.api.GrpcAPI.EasyTransferResponse> getEasyTransferAssetMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EasyTransferAssetMessage,
      org.tron.api.GrpcAPI.EasyTransferResponse> getEasyTransferAssetMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EasyTransferAssetMessage, org.tron.api.GrpcAPI.EasyTransferResponse> getEasyTransferAssetMethod;
    if ((getEasyTransferAssetMethod = WalletGrpc.getEasyTransferAssetMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getEasyTransferAssetMethod = WalletGrpc.getEasyTransferAssetMethod) == null) {
          WalletGrpc.getEasyTransferAssetMethod = getEasyTransferAssetMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.EasyTransferAssetMessage, org.tron.api.GrpcAPI.EasyTransferResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "EasyTransferAsset"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.EasyTransferAssetMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.EasyTransferResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("EasyTransferAsset"))
                  .build();
          }
        }
     }
     return getEasyTransferAssetMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getEasyTransferAssetByPrivateMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EasyTransferAssetByPrivateMessage,
      org.tron.api.GrpcAPI.EasyTransferResponse> METHOD_EASY_TRANSFER_ASSET_BY_PRIVATE = getEasyTransferAssetByPrivateMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EasyTransferAssetByPrivateMessage,
      org.tron.api.GrpcAPI.EasyTransferResponse> getEasyTransferAssetByPrivateMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EasyTransferAssetByPrivateMessage,
      org.tron.api.GrpcAPI.EasyTransferResponse> getEasyTransferAssetByPrivateMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EasyTransferAssetByPrivateMessage, org.tron.api.GrpcAPI.EasyTransferResponse> getEasyTransferAssetByPrivateMethod;
    if ((getEasyTransferAssetByPrivateMethod = WalletGrpc.getEasyTransferAssetByPrivateMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getEasyTransferAssetByPrivateMethod = WalletGrpc.getEasyTransferAssetByPrivateMethod) == null) {
          WalletGrpc.getEasyTransferAssetByPrivateMethod = getEasyTransferAssetByPrivateMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.EasyTransferAssetByPrivateMessage, org.tron.api.GrpcAPI.EasyTransferResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "EasyTransferAssetByPrivate"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.EasyTransferAssetByPrivateMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.EasyTransferResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("EasyTransferAssetByPrivate"))
                  .build();
          }
        }
     }
     return getEasyTransferAssetByPrivateMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getEasyTransferMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EasyTransferMessage,
      org.tron.api.GrpcAPI.EasyTransferResponse> METHOD_EASY_TRANSFER = getEasyTransferMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EasyTransferMessage,
      org.tron.api.GrpcAPI.EasyTransferResponse> getEasyTransferMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EasyTransferMessage,
      org.tron.api.GrpcAPI.EasyTransferResponse> getEasyTransferMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EasyTransferMessage, org.tron.api.GrpcAPI.EasyTransferResponse> getEasyTransferMethod;
    if ((getEasyTransferMethod = WalletGrpc.getEasyTransferMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getEasyTransferMethod = WalletGrpc.getEasyTransferMethod) == null) {
          WalletGrpc.getEasyTransferMethod = getEasyTransferMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.EasyTransferMessage, org.tron.api.GrpcAPI.EasyTransferResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "EasyTransfer"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.EasyTransferMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.EasyTransferResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("EasyTransfer"))
                  .build();
          }
        }
     }
     return getEasyTransferMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getEasyTransferByPrivateMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EasyTransferByPrivateMessage,
      org.tron.api.GrpcAPI.EasyTransferResponse> METHOD_EASY_TRANSFER_BY_PRIVATE = getEasyTransferByPrivateMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EasyTransferByPrivateMessage,
      org.tron.api.GrpcAPI.EasyTransferResponse> getEasyTransferByPrivateMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EasyTransferByPrivateMessage,
      org.tron.api.GrpcAPI.EasyTransferResponse> getEasyTransferByPrivateMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EasyTransferByPrivateMessage, org.tron.api.GrpcAPI.EasyTransferResponse> getEasyTransferByPrivateMethod;
    if ((getEasyTransferByPrivateMethod = WalletGrpc.getEasyTransferByPrivateMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getEasyTransferByPrivateMethod = WalletGrpc.getEasyTransferByPrivateMethod) == null) {
          WalletGrpc.getEasyTransferByPrivateMethod = getEasyTransferByPrivateMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.EasyTransferByPrivateMessage, org.tron.api.GrpcAPI.EasyTransferResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "EasyTransferByPrivate"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.EasyTransferByPrivateMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.EasyTransferResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("EasyTransferByPrivate"))
                  .build();
          }
        }
     }
     return getEasyTransferByPrivateMethod;
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
    if ((getGenerateAddressMethod = WalletGrpc.getGenerateAddressMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGenerateAddressMethod = WalletGrpc.getGenerateAddressMethod) == null) {
          WalletGrpc.getGenerateAddressMethod = getGenerateAddressMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.EmptyMessage, org.tron.api.GrpcAPI.AddressPrKeyPairMessage>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GenerateAddress"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.AddressPrKeyPairMessage.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GenerateAddress"))
                  .build();
          }
        }
     }
     return getGenerateAddressMethod;
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
    if ((getGetTransactionInfoByIdMethod = WalletGrpc.getGetTransactionInfoByIdMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetTransactionInfoByIdMethod = WalletGrpc.getGetTransactionInfoByIdMethod) == null) {
          WalletGrpc.getGetTransactionInfoByIdMethod = getGetTransactionInfoByIdMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.BytesMessage, org.tron.protos.Protocol.TransactionInfo>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetTransactionInfoById"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.TransactionInfo.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetTransactionInfoById"))
                  .build();
          }
        }
     }
     return getGetTransactionInfoByIdMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getAccountPermissionUpdateMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Contract.AccountPermissionUpdateContract,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_ACCOUNT_PERMISSION_UPDATE = getAccountPermissionUpdateMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Contract.AccountPermissionUpdateContract,
      org.tron.api.GrpcAPI.TransactionExtention> getAccountPermissionUpdateMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Contract.AccountPermissionUpdateContract,
      org.tron.api.GrpcAPI.TransactionExtention> getAccountPermissionUpdateMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Contract.AccountPermissionUpdateContract, org.tron.api.GrpcAPI.TransactionExtention> getAccountPermissionUpdateMethod;
    if ((getAccountPermissionUpdateMethod = WalletGrpc.getAccountPermissionUpdateMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getAccountPermissionUpdateMethod = WalletGrpc.getAccountPermissionUpdateMethod) == null) {
          WalletGrpc.getAccountPermissionUpdateMethod = getAccountPermissionUpdateMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Contract.AccountPermissionUpdateContract, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "AccountPermissionUpdate"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Contract.AccountPermissionUpdateContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("AccountPermissionUpdate"))
                  .build();
          }
        }
     }
     return getAccountPermissionUpdateMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getAddSignMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Protocol.TransactionSign,
      org.tron.api.GrpcAPI.TransactionExtention> METHOD_ADD_SIGN = getAddSignMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Protocol.TransactionSign,
      org.tron.api.GrpcAPI.TransactionExtention> getAddSignMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Protocol.TransactionSign,
      org.tron.api.GrpcAPI.TransactionExtention> getAddSignMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Protocol.TransactionSign, org.tron.api.GrpcAPI.TransactionExtention> getAddSignMethod;
    if ((getAddSignMethod = WalletGrpc.getAddSignMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getAddSignMethod = WalletGrpc.getAddSignMethod) == null) {
          WalletGrpc.getAddSignMethod = getAddSignMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Protocol.TransactionSign, org.tron.api.GrpcAPI.TransactionExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "AddSign"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.TransactionSign.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("AddSign"))
                  .build();
          }
        }
     }
     return getAddSignMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetTransactionSignWeightMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Protocol.Transaction,
      org.tron.api.GrpcAPI.TransactionSignWeight> METHOD_GET_TRANSACTION_SIGN_WEIGHT = getGetTransactionSignWeightMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Protocol.Transaction,
      org.tron.api.GrpcAPI.TransactionSignWeight> getGetTransactionSignWeightMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Protocol.Transaction,
      org.tron.api.GrpcAPI.TransactionSignWeight> getGetTransactionSignWeightMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Protocol.Transaction, org.tron.api.GrpcAPI.TransactionSignWeight> getGetTransactionSignWeightMethod;
    if ((getGetTransactionSignWeightMethod = WalletGrpc.getGetTransactionSignWeightMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetTransactionSignWeightMethod = WalletGrpc.getGetTransactionSignWeightMethod) == null) {
          WalletGrpc.getGetTransactionSignWeightMethod = getGetTransactionSignWeightMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Protocol.Transaction, org.tron.api.GrpcAPI.TransactionSignWeight>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetTransactionSignWeight"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Transaction.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionSignWeight.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetTransactionSignWeight"))
                  .build();
          }
        }
     }
     return getGetTransactionSignWeightMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetTransactionApprovedListMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.protos.Protocol.Transaction,
      org.tron.api.GrpcAPI.TransactionApprovedList> METHOD_GET_TRANSACTION_APPROVED_LIST = getGetTransactionApprovedListMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.protos.Protocol.Transaction,
      org.tron.api.GrpcAPI.TransactionApprovedList> getGetTransactionApprovedListMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.protos.Protocol.Transaction,
      org.tron.api.GrpcAPI.TransactionApprovedList> getGetTransactionApprovedListMethod() {
    io.grpc.MethodDescriptor<org.tron.protos.Protocol.Transaction, org.tron.api.GrpcAPI.TransactionApprovedList> getGetTransactionApprovedListMethod;
    if ((getGetTransactionApprovedListMethod = WalletGrpc.getGetTransactionApprovedListMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetTransactionApprovedListMethod = WalletGrpc.getGetTransactionApprovedListMethod) == null) {
          WalletGrpc.getGetTransactionApprovedListMethod = getGetTransactionApprovedListMethod = 
              io.grpc.MethodDescriptor.<org.tron.protos.Protocol.Transaction, org.tron.api.GrpcAPI.TransactionApprovedList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetTransactionApprovedList"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.Transaction.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.TransactionApprovedList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetTransactionApprovedList"))
                  .build();
          }
        }
     }
     return getGetTransactionApprovedListMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetNodeInfoMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.protos.Protocol.NodeInfo> METHOD_GET_NODE_INFO = getGetNodeInfoMethod();

  private static volatile io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.protos.Protocol.NodeInfo> getGetNodeInfoMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage,
      org.tron.protos.Protocol.NodeInfo> getGetNodeInfoMethod() {
    io.grpc.MethodDescriptor<org.tron.api.GrpcAPI.EmptyMessage, org.tron.protos.Protocol.NodeInfo> getGetNodeInfoMethod;
    if ((getGetNodeInfoMethod = WalletGrpc.getGetNodeInfoMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetNodeInfoMethod = WalletGrpc.getGetNodeInfoMethod) == null) {
          WalletGrpc.getGetNodeInfoMethod = getGetNodeInfoMethod = 
              io.grpc.MethodDescriptor.<org.tron.api.GrpcAPI.EmptyMessage, org.tron.protos.Protocol.NodeInfo>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetNodeInfo"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.tron.protos.Protocol.NodeInfo.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetNodeInfo"))
                  .build();
          }
        }
     }
     return getGetNodeInfoMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static WalletStub newStub(io.grpc.Channel channel) {
    return new WalletStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static WalletBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new WalletBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static WalletFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new WalletFutureStub(channel);
  }

  /**
   */
  public static abstract class WalletImplBase implements io.grpc.BindableService {

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
     * <pre>
     *Please use CreateTransaction2 instead of this function.
     * </pre>
     */
    public void createTransaction(org.tron.protos.Contract.TransferContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateTransactionMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of CreateTransaction.
     * </pre>
     */
    public void createTransaction2(org.tron.protos.Contract.TransferContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateTransaction2Method(), responseObserver);
    }

    /**
     */
    public void broadcastTransaction(org.tron.protos.Protocol.Transaction request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.Return> responseObserver) {
      asyncUnimplementedUnaryCall(getBroadcastTransactionMethod(), responseObserver);
    }

    /**
     * <pre>
     *Please use UpdateAccount2 instead of this function.
     * </pre>
     */
    public void updateAccount(org.tron.protos.Contract.AccountUpdateContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getUpdateAccountMethod(), responseObserver);
    }

    /**
     */
    public void setAccountId(org.tron.protos.Contract.SetAccountIdContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getSetAccountIdMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of UpdateAccount.
     * </pre>
     */
    public void updateAccount2(org.tron.protos.Contract.AccountUpdateContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getUpdateAccount2Method(), responseObserver);
    }

    /**
     * <pre>
     *Please use VoteWitnessAccount2 instead of this function.
     * </pre>
     */
    public void voteWitnessAccount(org.tron.protos.Contract.VoteWitnessContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getVoteWitnessAccountMethod(), responseObserver);
    }

    /**
     * <pre>
     *modify the consume_user_resource_percent
     * </pre>
     */
    public void updateSetting(org.tron.protos.Contract.UpdateSettingContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getUpdateSettingMethod(), responseObserver);
    }

    /**
     * <pre>
     *modify the energy_limit
     * </pre>
     */
    public void updateEnergyLimit(org.tron.protos.Contract.UpdateEnergyLimitContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getUpdateEnergyLimitMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of VoteWitnessAccount.
     * </pre>
     */
    public void voteWitnessAccount2(org.tron.protos.Contract.VoteWitnessContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getVoteWitnessAccount2Method(), responseObserver);
    }

    /**
     * <pre>
     *Please use CreateAssetIssue2 instead of this function.
     * </pre>
     */
    public void createAssetIssue(org.tron.protos.Contract.AssetIssueContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateAssetIssueMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of CreateAssetIssue.
     * </pre>
     */
    public void createAssetIssue2(org.tron.protos.Contract.AssetIssueContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateAssetIssue2Method(), responseObserver);
    }

    /**
     * <pre>
     *Please use UpdateWitness2 instead of this function.
     * </pre>
     */
    public void updateWitness(org.tron.protos.Contract.WitnessUpdateContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getUpdateWitnessMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of UpdateWitness.
     * </pre>
     */
    public void updateWitness2(org.tron.protos.Contract.WitnessUpdateContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getUpdateWitness2Method(), responseObserver);
    }

    /**
     * <pre>
     *Please use CreateAccount2 instead of this function.
     * </pre>
     */
    public void createAccount(org.tron.protos.Contract.AccountCreateContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateAccountMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of CreateAccount.
     * </pre>
     */
    public void createAccount2(org.tron.protos.Contract.AccountCreateContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateAccount2Method(), responseObserver);
    }

    /**
     * <pre>
     *Please use CreateWitness2 instead of this function.
     * </pre>
     */
    public void createWitness(org.tron.protos.Contract.WitnessCreateContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateWitnessMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of CreateWitness.
     * </pre>
     */
    public void createWitness2(org.tron.protos.Contract.WitnessCreateContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateWitness2Method(), responseObserver);
    }

    /**
     * <pre>
     *Please use TransferAsset2 instead of this function.
     * </pre>
     */
    public void transferAsset(org.tron.protos.Contract.TransferAssetContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getTransferAssetMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of TransferAsset.
     * </pre>
     */
    public void transferAsset2(org.tron.protos.Contract.TransferAssetContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getTransferAsset2Method(), responseObserver);
    }

    /**
     * <pre>
     *Please use ParticipateAssetIssue2 instead of this function.
     * </pre>
     */
    public void participateAssetIssue(org.tron.protos.Contract.ParticipateAssetIssueContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getParticipateAssetIssueMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of ParticipateAssetIssue.
     * </pre>
     */
    public void participateAssetIssue2(org.tron.protos.Contract.ParticipateAssetIssueContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getParticipateAssetIssue2Method(), responseObserver);
    }

    /**
     * <pre>
     *Please use FreezeBalance2 instead of this function.
     * </pre>
     */
    public void freezeBalance(org.tron.protos.Contract.FreezeBalanceContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getFreezeBalanceMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of FreezeBalance.
     * </pre>
     */
    public void freezeBalance2(org.tron.protos.Contract.FreezeBalanceContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getFreezeBalance2Method(), responseObserver);
    }

    /**
     * <pre>
     *Please use UnfreezeBalance2 instead of this function.
     * </pre>
     */
    public void unfreezeBalance(org.tron.protos.Contract.UnfreezeBalanceContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getUnfreezeBalanceMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of UnfreezeBalance.
     * </pre>
     */
    public void unfreezeBalance2(org.tron.protos.Contract.UnfreezeBalanceContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getUnfreezeBalance2Method(), responseObserver);
    }

    /**
     * <pre>
     *Please use UnfreezeAsset2 instead of this function.
     * </pre>
     */
    public void unfreezeAsset(org.tron.protos.Contract.UnfreezeAssetContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getUnfreezeAssetMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of UnfreezeAsset.
     * </pre>
     */
    public void unfreezeAsset2(org.tron.protos.Contract.UnfreezeAssetContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getUnfreezeAsset2Method(), responseObserver);
    }

    /**
     * <pre>
     *Please use WithdrawBalance2 instead of this function.
     * </pre>
     */
    public void withdrawBalance(org.tron.protos.Contract.WithdrawBalanceContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getWithdrawBalanceMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of WithdrawBalance.
     * </pre>
     */
    public void withdrawBalance2(org.tron.protos.Contract.WithdrawBalanceContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getWithdrawBalance2Method(), responseObserver);
    }

    /**
     * <pre>
     *Please use UpdateAsset2 instead of this function.
     * </pre>
     */
    public void updateAsset(org.tron.protos.Contract.UpdateAssetContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getUpdateAssetMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of UpdateAsset.
     * </pre>
     */
    public void updateAsset2(org.tron.protos.Contract.UpdateAssetContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getUpdateAsset2Method(), responseObserver);
    }

    /**
     */
    public void proposalCreate(org.tron.protos.Contract.ProposalCreateContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getProposalCreateMethod(), responseObserver);
    }

    /**
     */
    public void proposalApprove(org.tron.protos.Contract.ProposalApproveContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getProposalApproveMethod(), responseObserver);
    }

    /**
     */
    public void proposalDelete(org.tron.protos.Contract.ProposalDeleteContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getProposalDeleteMethod(), responseObserver);
    }

    /**
     */
    public void buyStorage(org.tron.protos.Contract.BuyStorageContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getBuyStorageMethod(), responseObserver);
    }

    /**
     */
    public void buyStorageBytes(org.tron.protos.Contract.BuyStorageBytesContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getBuyStorageBytesMethod(), responseObserver);
    }

    /**
     */
    public void sellStorage(org.tron.protos.Contract.SellStorageContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getSellStorageMethod(), responseObserver);
    }

    /**
     */
    public void exchangeCreate(org.tron.protos.Contract.ExchangeCreateContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getExchangeCreateMethod(), responseObserver);
    }

    /**
     */
    public void exchangeInject(org.tron.protos.Contract.ExchangeInjectContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getExchangeInjectMethod(), responseObserver);
    }

    /**
     */
    public void exchangeWithdraw(org.tron.protos.Contract.ExchangeWithdrawContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getExchangeWithdrawMethod(), responseObserver);
    }

    /**
     */
    public void exchangeTransaction(org.tron.protos.Contract.ExchangeTransactionContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getExchangeTransactionMethod(), responseObserver);
    }

    /**
     */
    public void listNodes(org.tron.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.NodeList> responseObserver) {
      asyncUnimplementedUnaryCall(getListNodesMethod(), responseObserver);
    }

    /**
     */
    public void getAssetIssueByAccount(org.tron.protos.Protocol.Account request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.AssetIssueList> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAssetIssueByAccountMethod(), responseObserver);
    }

    /**
     */
    public void getAccountNet(org.tron.protos.Protocol.Account request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.AccountNetMessage> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAccountNetMethod(), responseObserver);
    }

    /**
     */
    public void getAccountResource(org.tron.protos.Protocol.Account request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.AccountResourceMessage> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAccountResourceMethod(), responseObserver);
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
    public void getBlockById(org.tron.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Block> responseObserver) {
      asyncUnimplementedUnaryCall(getGetBlockByIdMethod(), responseObserver);
    }

    /**
     * <pre>
     *Please use GetBlockByLimitNext2 instead of this function.
     * </pre>
     */
    public void getBlockByLimitNext(org.tron.api.GrpcAPI.BlockLimit request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.BlockList> responseObserver) {
      asyncUnimplementedUnaryCall(getGetBlockByLimitNextMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of GetBlockByLimitNext.
     * </pre>
     */
    public void getBlockByLimitNext2(org.tron.api.GrpcAPI.BlockLimit request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.BlockListExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getGetBlockByLimitNext2Method(), responseObserver);
    }

    /**
     * <pre>
     *Please use GetBlockByLatestNum2 instead of this function.
     * </pre>
     */
    public void getBlockByLatestNum(org.tron.api.GrpcAPI.NumberMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.BlockList> responseObserver) {
      asyncUnimplementedUnaryCall(getGetBlockByLatestNumMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of GetBlockByLatestNum.
     * </pre>
     */
    public void getBlockByLatestNum2(org.tron.api.GrpcAPI.NumberMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.BlockListExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getGetBlockByLatestNum2Method(), responseObserver);
    }

    /**
     */
    public void getTransactionById(org.tron.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getGetTransactionByIdMethod(), responseObserver);
    }

    /**
     */
    public void deployContract(org.tron.protos.Contract.CreateSmartContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getDeployContractMethod(), responseObserver);
    }

    /**
     */
    public void getContract(org.tron.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.SmartContract> responseObserver) {
      asyncUnimplementedUnaryCall(getGetContractMethod(), responseObserver);
    }

    /**
     */
    public void triggerContract(org.tron.protos.Contract.TriggerSmartContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getTriggerContractMethod(), responseObserver);
    }

    /**
     */
    public void triggerConstantContract(org.tron.protos.Contract.TriggerSmartContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getTriggerConstantContractMethod(), responseObserver);
    }

    /**
     */
    public void clearContractABI(org.tron.protos.Contract.ClearABIContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getClearContractABIMethod(), responseObserver);
    }

    /**
     */
    public void listWitnesses(org.tron.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.WitnessList> responseObserver) {
      asyncUnimplementedUnaryCall(getListWitnessesMethod(), responseObserver);
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
    public void listProposals(org.tron.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.ProposalList> responseObserver) {
      asyncUnimplementedUnaryCall(getListProposalsMethod(), responseObserver);
    }

    /**
     */
    public void getPaginatedProposalList(org.tron.api.GrpcAPI.PaginatedMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.ProposalList> responseObserver) {
      asyncUnimplementedUnaryCall(getGetPaginatedProposalListMethod(), responseObserver);
    }

    /**
     */
    public void getProposalById(org.tron.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Proposal> responseObserver) {
      asyncUnimplementedUnaryCall(getGetProposalByIdMethod(), responseObserver);
    }

    /**
     */
    public void listExchanges(org.tron.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.ExchangeList> responseObserver) {
      asyncUnimplementedUnaryCall(getListExchangesMethod(), responseObserver);
    }

    /**
     */
    public void getPaginatedExchangeList(org.tron.api.GrpcAPI.PaginatedMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.ExchangeList> responseObserver) {
      asyncUnimplementedUnaryCall(getGetPaginatedExchangeListMethod(), responseObserver);
    }

    /**
     */
    public void getExchangeById(org.tron.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Exchange> responseObserver) {
      asyncUnimplementedUnaryCall(getGetExchangeByIdMethod(), responseObserver);
    }

    /**
     */
    public void getChainParameters(org.tron.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.ChainParameters> responseObserver) {
      asyncUnimplementedUnaryCall(getGetChainParametersMethod(), responseObserver);
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
    public void totalTransaction(org.tron.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.NumberMessage> responseObserver) {
      asyncUnimplementedUnaryCall(getTotalTransactionMethod(), responseObserver);
    }

    /**
     */
    public void getNextMaintenanceTime(org.tron.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.NumberMessage> responseObserver) {
      asyncUnimplementedUnaryCall(getGetNextMaintenanceTimeMethod(), responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     *Please use GetTransactionSign2 instead of this function.
     * </pre>
     */
    public void getTransactionSign(org.tron.protos.Protocol.TransactionSign request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getGetTransactionSignMethod(), responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     *Use this function instead of GetTransactionSign.
     * </pre>
     */
    public void getTransactionSign2(org.tron.protos.Protocol.TransactionSign request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getGetTransactionSign2Method(), responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public void createAddress(org.tron.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.BytesMessage> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateAddressMethod(), responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public void easyTransferAsset(org.tron.api.GrpcAPI.EasyTransferAssetMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.EasyTransferResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getEasyTransferAssetMethod(), responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public void easyTransferAssetByPrivate(org.tron.api.GrpcAPI.EasyTransferAssetByPrivateMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.EasyTransferResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getEasyTransferAssetByPrivateMethod(), responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public void easyTransfer(org.tron.api.GrpcAPI.EasyTransferMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.EasyTransferResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getEasyTransferMethod(), responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public void easyTransferByPrivate(org.tron.api.GrpcAPI.EasyTransferByPrivateMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.EasyTransferResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getEasyTransferByPrivateMethod(), responseObserver);
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

    /**
     */
    public void getTransactionInfoById(org.tron.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.TransactionInfo> responseObserver) {
      asyncUnimplementedUnaryCall(getGetTransactionInfoByIdMethod(), responseObserver);
    }

    /**
     */
    public void accountPermissionUpdate(org.tron.protos.Contract.AccountPermissionUpdateContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getAccountPermissionUpdateMethod(), responseObserver);
    }

    /**
     */
    public void addSign(org.tron.protos.Protocol.TransactionSign request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getAddSignMethod(), responseObserver);
    }

    /**
     */
    public void getTransactionSignWeight(org.tron.protos.Protocol.Transaction request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionSignWeight> responseObserver) {
      asyncUnimplementedUnaryCall(getGetTransactionSignWeightMethod(), responseObserver);
    }

    /**
     */
    public void getTransactionApprovedList(org.tron.protos.Protocol.Transaction request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionApprovedList> responseObserver) {
      asyncUnimplementedUnaryCall(getGetTransactionApprovedListMethod(), responseObserver);
    }

    /**
     */
    public void getNodeInfo(org.tron.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.NodeInfo> responseObserver) {
      asyncUnimplementedUnaryCall(getGetNodeInfoMethod(), responseObserver);
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
            getCreateTransactionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.TransferContract,
                org.tron.protos.Protocol.Transaction>(
                  this, METHODID_CREATE_TRANSACTION)))
          .addMethod(
            getCreateTransaction2Method(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.TransferContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_CREATE_TRANSACTION2)))
          .addMethod(
            getBroadcastTransactionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Protocol.Transaction,
                org.tron.api.GrpcAPI.Return>(
                  this, METHODID_BROADCAST_TRANSACTION)))
          .addMethod(
            getUpdateAccountMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.AccountUpdateContract,
                org.tron.protos.Protocol.Transaction>(
                  this, METHODID_UPDATE_ACCOUNT)))
          .addMethod(
            getSetAccountIdMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.SetAccountIdContract,
                org.tron.protos.Protocol.Transaction>(
                  this, METHODID_SET_ACCOUNT_ID)))
          .addMethod(
            getUpdateAccount2Method(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.AccountUpdateContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_UPDATE_ACCOUNT2)))
          .addMethod(
            getVoteWitnessAccountMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.VoteWitnessContract,
                org.tron.protos.Protocol.Transaction>(
                  this, METHODID_VOTE_WITNESS_ACCOUNT)))
          .addMethod(
            getUpdateSettingMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.UpdateSettingContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_UPDATE_SETTING)))
          .addMethod(
            getUpdateEnergyLimitMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.UpdateEnergyLimitContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_UPDATE_ENERGY_LIMIT)))
          .addMethod(
            getVoteWitnessAccount2Method(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.VoteWitnessContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_VOTE_WITNESS_ACCOUNT2)))
          .addMethod(
            getCreateAssetIssueMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.AssetIssueContract,
                org.tron.protos.Protocol.Transaction>(
                  this, METHODID_CREATE_ASSET_ISSUE)))
          .addMethod(
            getCreateAssetIssue2Method(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.AssetIssueContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_CREATE_ASSET_ISSUE2)))
          .addMethod(
            getUpdateWitnessMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.WitnessUpdateContract,
                org.tron.protos.Protocol.Transaction>(
                  this, METHODID_UPDATE_WITNESS)))
          .addMethod(
            getUpdateWitness2Method(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.WitnessUpdateContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_UPDATE_WITNESS2)))
          .addMethod(
            getCreateAccountMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.AccountCreateContract,
                org.tron.protos.Protocol.Transaction>(
                  this, METHODID_CREATE_ACCOUNT)))
          .addMethod(
            getCreateAccount2Method(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.AccountCreateContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_CREATE_ACCOUNT2)))
          .addMethod(
            getCreateWitnessMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.WitnessCreateContract,
                org.tron.protos.Protocol.Transaction>(
                  this, METHODID_CREATE_WITNESS)))
          .addMethod(
            getCreateWitness2Method(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.WitnessCreateContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_CREATE_WITNESS2)))
          .addMethod(
            getTransferAssetMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.TransferAssetContract,
                org.tron.protos.Protocol.Transaction>(
                  this, METHODID_TRANSFER_ASSET)))
          .addMethod(
            getTransferAsset2Method(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.TransferAssetContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_TRANSFER_ASSET2)))
          .addMethod(
            getParticipateAssetIssueMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.ParticipateAssetIssueContract,
                org.tron.protos.Protocol.Transaction>(
                  this, METHODID_PARTICIPATE_ASSET_ISSUE)))
          .addMethod(
            getParticipateAssetIssue2Method(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.ParticipateAssetIssueContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_PARTICIPATE_ASSET_ISSUE2)))
          .addMethod(
            getFreezeBalanceMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.FreezeBalanceContract,
                org.tron.protos.Protocol.Transaction>(
                  this, METHODID_FREEZE_BALANCE)))
          .addMethod(
            getFreezeBalance2Method(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.FreezeBalanceContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_FREEZE_BALANCE2)))
          .addMethod(
            getUnfreezeBalanceMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.UnfreezeBalanceContract,
                org.tron.protos.Protocol.Transaction>(
                  this, METHODID_UNFREEZE_BALANCE)))
          .addMethod(
            getUnfreezeBalance2Method(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.UnfreezeBalanceContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_UNFREEZE_BALANCE2)))
          .addMethod(
            getUnfreezeAssetMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.UnfreezeAssetContract,
                org.tron.protos.Protocol.Transaction>(
                  this, METHODID_UNFREEZE_ASSET)))
          .addMethod(
            getUnfreezeAsset2Method(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.UnfreezeAssetContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_UNFREEZE_ASSET2)))
          .addMethod(
            getWithdrawBalanceMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.WithdrawBalanceContract,
                org.tron.protos.Protocol.Transaction>(
                  this, METHODID_WITHDRAW_BALANCE)))
          .addMethod(
            getWithdrawBalance2Method(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.WithdrawBalanceContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_WITHDRAW_BALANCE2)))
          .addMethod(
            getUpdateAssetMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.UpdateAssetContract,
                org.tron.protos.Protocol.Transaction>(
                  this, METHODID_UPDATE_ASSET)))
          .addMethod(
            getUpdateAsset2Method(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.UpdateAssetContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_UPDATE_ASSET2)))
          .addMethod(
            getProposalCreateMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.ProposalCreateContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_PROPOSAL_CREATE)))
          .addMethod(
            getProposalApproveMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.ProposalApproveContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_PROPOSAL_APPROVE)))
          .addMethod(
            getProposalDeleteMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.ProposalDeleteContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_PROPOSAL_DELETE)))
          .addMethod(
            getBuyStorageMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.BuyStorageContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_BUY_STORAGE)))
          .addMethod(
            getBuyStorageBytesMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.BuyStorageBytesContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_BUY_STORAGE_BYTES)))
          .addMethod(
            getSellStorageMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.SellStorageContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_SELL_STORAGE)))
          .addMethod(
            getExchangeCreateMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.ExchangeCreateContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_EXCHANGE_CREATE)))
          .addMethod(
            getExchangeInjectMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.ExchangeInjectContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_EXCHANGE_INJECT)))
          .addMethod(
            getExchangeWithdrawMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.ExchangeWithdrawContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_EXCHANGE_WITHDRAW)))
          .addMethod(
            getExchangeTransactionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.ExchangeTransactionContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_EXCHANGE_TRANSACTION)))
          .addMethod(
            getListNodesMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.EmptyMessage,
                org.tron.api.GrpcAPI.NodeList>(
                  this, METHODID_LIST_NODES)))
          .addMethod(
            getGetAssetIssueByAccountMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Protocol.Account,
                org.tron.api.GrpcAPI.AssetIssueList>(
                  this, METHODID_GET_ASSET_ISSUE_BY_ACCOUNT)))
          .addMethod(
            getGetAccountNetMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Protocol.Account,
                org.tron.api.GrpcAPI.AccountNetMessage>(
                  this, METHODID_GET_ACCOUNT_NET)))
          .addMethod(
            getGetAccountResourceMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Protocol.Account,
                org.tron.api.GrpcAPI.AccountResourceMessage>(
                  this, METHODID_GET_ACCOUNT_RESOURCE)))
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
            getGetBlockByIdMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.BytesMessage,
                org.tron.protos.Protocol.Block>(
                  this, METHODID_GET_BLOCK_BY_ID)))
          .addMethod(
            getGetBlockByLimitNextMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.BlockLimit,
                org.tron.api.GrpcAPI.BlockList>(
                  this, METHODID_GET_BLOCK_BY_LIMIT_NEXT)))
          .addMethod(
            getGetBlockByLimitNext2Method(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.BlockLimit,
                org.tron.api.GrpcAPI.BlockListExtention>(
                  this, METHODID_GET_BLOCK_BY_LIMIT_NEXT2)))
          .addMethod(
            getGetBlockByLatestNumMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.NumberMessage,
                org.tron.api.GrpcAPI.BlockList>(
                  this, METHODID_GET_BLOCK_BY_LATEST_NUM)))
          .addMethod(
            getGetBlockByLatestNum2Method(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.NumberMessage,
                org.tron.api.GrpcAPI.BlockListExtention>(
                  this, METHODID_GET_BLOCK_BY_LATEST_NUM2)))
          .addMethod(
            getGetTransactionByIdMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.BytesMessage,
                org.tron.protos.Protocol.Transaction>(
                  this, METHODID_GET_TRANSACTION_BY_ID)))
          .addMethod(
            getDeployContractMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.CreateSmartContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_DEPLOY_CONTRACT)))
          .addMethod(
            getGetContractMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.BytesMessage,
                org.tron.protos.Protocol.SmartContract>(
                  this, METHODID_GET_CONTRACT)))
          .addMethod(
            getTriggerContractMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.TriggerSmartContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_TRIGGER_CONTRACT)))
          .addMethod(
            getTriggerConstantContractMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.TriggerSmartContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_TRIGGER_CONSTANT_CONTRACT)))
          .addMethod(
            getClearContractABIMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.ClearABIContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_CLEAR_CONTRACT_ABI)))
          .addMethod(
            getListWitnessesMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.EmptyMessage,
                org.tron.api.GrpcAPI.WitnessList>(
                  this, METHODID_LIST_WITNESSES)))
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
            getListProposalsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.EmptyMessage,
                org.tron.api.GrpcAPI.ProposalList>(
                  this, METHODID_LIST_PROPOSALS)))
          .addMethod(
            getGetPaginatedProposalListMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.PaginatedMessage,
                org.tron.api.GrpcAPI.ProposalList>(
                  this, METHODID_GET_PAGINATED_PROPOSAL_LIST)))
          .addMethod(
            getGetProposalByIdMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.BytesMessage,
                org.tron.protos.Protocol.Proposal>(
                  this, METHODID_GET_PROPOSAL_BY_ID)))
          .addMethod(
            getListExchangesMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.EmptyMessage,
                org.tron.api.GrpcAPI.ExchangeList>(
                  this, METHODID_LIST_EXCHANGES)))
          .addMethod(
            getGetPaginatedExchangeListMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.PaginatedMessage,
                org.tron.api.GrpcAPI.ExchangeList>(
                  this, METHODID_GET_PAGINATED_EXCHANGE_LIST)))
          .addMethod(
            getGetExchangeByIdMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.BytesMessage,
                org.tron.protos.Protocol.Exchange>(
                  this, METHODID_GET_EXCHANGE_BY_ID)))
          .addMethod(
            getGetChainParametersMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.EmptyMessage,
                org.tron.protos.Protocol.ChainParameters>(
                  this, METHODID_GET_CHAIN_PARAMETERS)))
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
            getTotalTransactionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.EmptyMessage,
                org.tron.api.GrpcAPI.NumberMessage>(
                  this, METHODID_TOTAL_TRANSACTION)))
          .addMethod(
            getGetNextMaintenanceTimeMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.EmptyMessage,
                org.tron.api.GrpcAPI.NumberMessage>(
                  this, METHODID_GET_NEXT_MAINTENANCE_TIME)))
          .addMethod(
            getGetTransactionSignMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Protocol.TransactionSign,
                org.tron.protos.Protocol.Transaction>(
                  this, METHODID_GET_TRANSACTION_SIGN)))
          .addMethod(
            getGetTransactionSign2Method(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Protocol.TransactionSign,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_GET_TRANSACTION_SIGN2)))
          .addMethod(
            getCreateAddressMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.BytesMessage,
                org.tron.api.GrpcAPI.BytesMessage>(
                  this, METHODID_CREATE_ADDRESS)))
          .addMethod(
            getEasyTransferAssetMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.EasyTransferAssetMessage,
                org.tron.api.GrpcAPI.EasyTransferResponse>(
                  this, METHODID_EASY_TRANSFER_ASSET)))
          .addMethod(
            getEasyTransferAssetByPrivateMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.EasyTransferAssetByPrivateMessage,
                org.tron.api.GrpcAPI.EasyTransferResponse>(
                  this, METHODID_EASY_TRANSFER_ASSET_BY_PRIVATE)))
          .addMethod(
            getEasyTransferMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.EasyTransferMessage,
                org.tron.api.GrpcAPI.EasyTransferResponse>(
                  this, METHODID_EASY_TRANSFER)))
          .addMethod(
            getEasyTransferByPrivateMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.EasyTransferByPrivateMessage,
                org.tron.api.GrpcAPI.EasyTransferResponse>(
                  this, METHODID_EASY_TRANSFER_BY_PRIVATE)))
          .addMethod(
            getGenerateAddressMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.EmptyMessage,
                org.tron.api.GrpcAPI.AddressPrKeyPairMessage>(
                  this, METHODID_GENERATE_ADDRESS)))
          .addMethod(
            getGetTransactionInfoByIdMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.BytesMessage,
                org.tron.protos.Protocol.TransactionInfo>(
                  this, METHODID_GET_TRANSACTION_INFO_BY_ID)))
          .addMethod(
            getAccountPermissionUpdateMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Contract.AccountPermissionUpdateContract,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_ACCOUNT_PERMISSION_UPDATE)))
          .addMethod(
            getAddSignMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Protocol.TransactionSign,
                org.tron.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_ADD_SIGN)))
          .addMethod(
            getGetTransactionSignWeightMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Protocol.Transaction,
                org.tron.api.GrpcAPI.TransactionSignWeight>(
                  this, METHODID_GET_TRANSACTION_SIGN_WEIGHT)))
          .addMethod(
            getGetTransactionApprovedListMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.protos.Protocol.Transaction,
                org.tron.api.GrpcAPI.TransactionApprovedList>(
                  this, METHODID_GET_TRANSACTION_APPROVED_LIST)))
          .addMethod(
            getGetNodeInfoMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.tron.api.GrpcAPI.EmptyMessage,
                org.tron.protos.Protocol.NodeInfo>(
                  this, METHODID_GET_NODE_INFO)))
          .build();
    }
  }

  /**
   */
  public static final class WalletStub extends io.grpc.stub.AbstractStub<WalletStub> {
    private WalletStub(io.grpc.Channel channel) {
      super(channel);
    }

    private WalletStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WalletStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new WalletStub(channel, callOptions);
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
     * <pre>
     *Please use CreateTransaction2 instead of this function.
     * </pre>
     */
    public void createTransaction(org.tron.protos.Contract.TransferContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateTransactionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of CreateTransaction.
     * </pre>
     */
    public void createTransaction2(org.tron.protos.Contract.TransferContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateTransaction2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void broadcastTransaction(org.tron.protos.Protocol.Transaction request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.Return> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getBroadcastTransactionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use UpdateAccount2 instead of this function.
     * </pre>
     */
    public void updateAccount(org.tron.protos.Contract.AccountUpdateContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUpdateAccountMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void setAccountId(org.tron.protos.Contract.SetAccountIdContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSetAccountIdMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of UpdateAccount.
     * </pre>
     */
    public void updateAccount2(org.tron.protos.Contract.AccountUpdateContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUpdateAccount2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use VoteWitnessAccount2 instead of this function.
     * </pre>
     */
    public void voteWitnessAccount(org.tron.protos.Contract.VoteWitnessContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getVoteWitnessAccountMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *modify the consume_user_resource_percent
     * </pre>
     */
    public void updateSetting(org.tron.protos.Contract.UpdateSettingContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUpdateSettingMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *modify the energy_limit
     * </pre>
     */
    public void updateEnergyLimit(org.tron.protos.Contract.UpdateEnergyLimitContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUpdateEnergyLimitMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of VoteWitnessAccount.
     * </pre>
     */
    public void voteWitnessAccount2(org.tron.protos.Contract.VoteWitnessContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getVoteWitnessAccount2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use CreateAssetIssue2 instead of this function.
     * </pre>
     */
    public void createAssetIssue(org.tron.protos.Contract.AssetIssueContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateAssetIssueMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of CreateAssetIssue.
     * </pre>
     */
    public void createAssetIssue2(org.tron.protos.Contract.AssetIssueContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateAssetIssue2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use UpdateWitness2 instead of this function.
     * </pre>
     */
    public void updateWitness(org.tron.protos.Contract.WitnessUpdateContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUpdateWitnessMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of UpdateWitness.
     * </pre>
     */
    public void updateWitness2(org.tron.protos.Contract.WitnessUpdateContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUpdateWitness2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use CreateAccount2 instead of this function.
     * </pre>
     */
    public void createAccount(org.tron.protos.Contract.AccountCreateContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateAccountMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of CreateAccount.
     * </pre>
     */
    public void createAccount2(org.tron.protos.Contract.AccountCreateContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateAccount2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use CreateWitness2 instead of this function.
     * </pre>
     */
    public void createWitness(org.tron.protos.Contract.WitnessCreateContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateWitnessMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of CreateWitness.
     * </pre>
     */
    public void createWitness2(org.tron.protos.Contract.WitnessCreateContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateWitness2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use TransferAsset2 instead of this function.
     * </pre>
     */
    public void transferAsset(org.tron.protos.Contract.TransferAssetContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getTransferAssetMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of TransferAsset.
     * </pre>
     */
    public void transferAsset2(org.tron.protos.Contract.TransferAssetContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getTransferAsset2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use ParticipateAssetIssue2 instead of this function.
     * </pre>
     */
    public void participateAssetIssue(org.tron.protos.Contract.ParticipateAssetIssueContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getParticipateAssetIssueMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of ParticipateAssetIssue.
     * </pre>
     */
    public void participateAssetIssue2(org.tron.protos.Contract.ParticipateAssetIssueContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getParticipateAssetIssue2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use FreezeBalance2 instead of this function.
     * </pre>
     */
    public void freezeBalance(org.tron.protos.Contract.FreezeBalanceContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getFreezeBalanceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of FreezeBalance.
     * </pre>
     */
    public void freezeBalance2(org.tron.protos.Contract.FreezeBalanceContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getFreezeBalance2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use UnfreezeBalance2 instead of this function.
     * </pre>
     */
    public void unfreezeBalance(org.tron.protos.Contract.UnfreezeBalanceContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUnfreezeBalanceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of UnfreezeBalance.
     * </pre>
     */
    public void unfreezeBalance2(org.tron.protos.Contract.UnfreezeBalanceContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUnfreezeBalance2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use UnfreezeAsset2 instead of this function.
     * </pre>
     */
    public void unfreezeAsset(org.tron.protos.Contract.UnfreezeAssetContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUnfreezeAssetMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of UnfreezeAsset.
     * </pre>
     */
    public void unfreezeAsset2(org.tron.protos.Contract.UnfreezeAssetContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUnfreezeAsset2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use WithdrawBalance2 instead of this function.
     * </pre>
     */
    public void withdrawBalance(org.tron.protos.Contract.WithdrawBalanceContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getWithdrawBalanceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of WithdrawBalance.
     * </pre>
     */
    public void withdrawBalance2(org.tron.protos.Contract.WithdrawBalanceContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getWithdrawBalance2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use UpdateAsset2 instead of this function.
     * </pre>
     */
    public void updateAsset(org.tron.protos.Contract.UpdateAssetContract request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUpdateAssetMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of UpdateAsset.
     * </pre>
     */
    public void updateAsset2(org.tron.protos.Contract.UpdateAssetContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUpdateAsset2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void proposalCreate(org.tron.protos.Contract.ProposalCreateContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getProposalCreateMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void proposalApprove(org.tron.protos.Contract.ProposalApproveContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getProposalApproveMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void proposalDelete(org.tron.protos.Contract.ProposalDeleteContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getProposalDeleteMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void buyStorage(org.tron.protos.Contract.BuyStorageContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getBuyStorageMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void buyStorageBytes(org.tron.protos.Contract.BuyStorageBytesContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getBuyStorageBytesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void sellStorage(org.tron.protos.Contract.SellStorageContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSellStorageMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void exchangeCreate(org.tron.protos.Contract.ExchangeCreateContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getExchangeCreateMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void exchangeInject(org.tron.protos.Contract.ExchangeInjectContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getExchangeInjectMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void exchangeWithdraw(org.tron.protos.Contract.ExchangeWithdrawContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getExchangeWithdrawMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void exchangeTransaction(org.tron.protos.Contract.ExchangeTransactionContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getExchangeTransactionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void listNodes(org.tron.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.NodeList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListNodesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getAssetIssueByAccount(org.tron.protos.Protocol.Account request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.AssetIssueList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAssetIssueByAccountMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getAccountNet(org.tron.protos.Protocol.Account request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.AccountNetMessage> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAccountNetMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getAccountResource(org.tron.protos.Protocol.Account request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.AccountResourceMessage> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAccountResourceMethod(), getCallOptions()), request, responseObserver);
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
    public void getBlockById(org.tron.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Block> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetBlockByIdMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use GetBlockByLimitNext2 instead of this function.
     * </pre>
     */
    public void getBlockByLimitNext(org.tron.api.GrpcAPI.BlockLimit request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.BlockList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetBlockByLimitNextMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of GetBlockByLimitNext.
     * </pre>
     */
    public void getBlockByLimitNext2(org.tron.api.GrpcAPI.BlockLimit request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.BlockListExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetBlockByLimitNext2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use GetBlockByLatestNum2 instead of this function.
     * </pre>
     */
    public void getBlockByLatestNum(org.tron.api.GrpcAPI.NumberMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.BlockList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetBlockByLatestNumMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of GetBlockByLatestNum.
     * </pre>
     */
    public void getBlockByLatestNum2(org.tron.api.GrpcAPI.NumberMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.BlockListExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetBlockByLatestNum2Method(), getCallOptions()), request, responseObserver);
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
    public void deployContract(org.tron.protos.Contract.CreateSmartContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeployContractMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getContract(org.tron.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.SmartContract> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetContractMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void triggerContract(org.tron.protos.Contract.TriggerSmartContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getTriggerContractMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void triggerConstantContract(org.tron.protos.Contract.TriggerSmartContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getTriggerConstantContractMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void clearContractABI(org.tron.protos.Contract.ClearABIContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getClearContractABIMethod(), getCallOptions()), request, responseObserver);
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
    public void listProposals(org.tron.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.ProposalList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListProposalsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getPaginatedProposalList(org.tron.api.GrpcAPI.PaginatedMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.ProposalList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetPaginatedProposalListMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getProposalById(org.tron.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Proposal> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetProposalByIdMethod(), getCallOptions()), request, responseObserver);
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
    public void getPaginatedExchangeList(org.tron.api.GrpcAPI.PaginatedMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.ExchangeList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetPaginatedExchangeListMethod(), getCallOptions()), request, responseObserver);
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
    public void getChainParameters(org.tron.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.ChainParameters> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetChainParametersMethod(), getCallOptions()), request, responseObserver);
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
    public void totalTransaction(org.tron.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.NumberMessage> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getTotalTransactionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getNextMaintenanceTime(org.tron.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.NumberMessage> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetNextMaintenanceTimeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     *Please use GetTransactionSign2 instead of this function.
     * </pre>
     */
    public void getTransactionSign(org.tron.protos.Protocol.TransactionSign request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetTransactionSignMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     *Use this function instead of GetTransactionSign.
     * </pre>
     */
    public void getTransactionSign2(org.tron.protos.Protocol.TransactionSign request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetTransactionSign2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public void createAddress(org.tron.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.BytesMessage> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateAddressMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public void easyTransferAsset(org.tron.api.GrpcAPI.EasyTransferAssetMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.EasyTransferResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getEasyTransferAssetMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public void easyTransferAssetByPrivate(org.tron.api.GrpcAPI.EasyTransferAssetByPrivateMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.EasyTransferResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getEasyTransferAssetByPrivateMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public void easyTransfer(org.tron.api.GrpcAPI.EasyTransferMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.EasyTransferResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getEasyTransferMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public void easyTransferByPrivate(org.tron.api.GrpcAPI.EasyTransferByPrivateMessage request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.EasyTransferResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getEasyTransferByPrivateMethod(), getCallOptions()), request, responseObserver);
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

    /**
     */
    public void getTransactionInfoById(org.tron.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.TransactionInfo> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetTransactionInfoByIdMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void accountPermissionUpdate(org.tron.protos.Contract.AccountPermissionUpdateContract request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getAccountPermissionUpdateMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void addSign(org.tron.protos.Protocol.TransactionSign request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getAddSignMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getTransactionSignWeight(org.tron.protos.Protocol.Transaction request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionSignWeight> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetTransactionSignWeightMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getTransactionApprovedList(org.tron.protos.Protocol.Transaction request,
        io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionApprovedList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetTransactionApprovedListMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getNodeInfo(org.tron.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.tron.protos.Protocol.NodeInfo> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetNodeInfoMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class WalletBlockingStub extends io.grpc.stub.AbstractStub<WalletBlockingStub> {
    private WalletBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private WalletBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WalletBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new WalletBlockingStub(channel, callOptions);
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
     * <pre>
     *Please use CreateTransaction2 instead of this function.
     * </pre>
     */
    public org.tron.protos.Protocol.Transaction createTransaction(org.tron.protos.Contract.TransferContract request) {
      return blockingUnaryCall(
          getChannel(), getCreateTransactionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of CreateTransaction.
     * </pre>
     */
    public org.tron.api.GrpcAPI.TransactionExtention createTransaction2(org.tron.protos.Contract.TransferContract request) {
      return blockingUnaryCall(
          getChannel(), getCreateTransaction2Method(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.Return broadcastTransaction(org.tron.protos.Protocol.Transaction request) {
      return blockingUnaryCall(
          getChannel(), getBroadcastTransactionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use UpdateAccount2 instead of this function.
     * </pre>
     */
    public org.tron.protos.Protocol.Transaction updateAccount(org.tron.protos.Contract.AccountUpdateContract request) {
      return blockingUnaryCall(
          getChannel(), getUpdateAccountMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.protos.Protocol.Transaction setAccountId(org.tron.protos.Contract.SetAccountIdContract request) {
      return blockingUnaryCall(
          getChannel(), getSetAccountIdMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of UpdateAccount.
     * </pre>
     */
    public org.tron.api.GrpcAPI.TransactionExtention updateAccount2(org.tron.protos.Contract.AccountUpdateContract request) {
      return blockingUnaryCall(
          getChannel(), getUpdateAccount2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use VoteWitnessAccount2 instead of this function.
     * </pre>
     */
    public org.tron.protos.Protocol.Transaction voteWitnessAccount(org.tron.protos.Contract.VoteWitnessContract request) {
      return blockingUnaryCall(
          getChannel(), getVoteWitnessAccountMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *modify the consume_user_resource_percent
     * </pre>
     */
    public org.tron.api.GrpcAPI.TransactionExtention updateSetting(org.tron.protos.Contract.UpdateSettingContract request) {
      return blockingUnaryCall(
          getChannel(), getUpdateSettingMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *modify the energy_limit
     * </pre>
     */
    public org.tron.api.GrpcAPI.TransactionExtention updateEnergyLimit(org.tron.protos.Contract.UpdateEnergyLimitContract request) {
      return blockingUnaryCall(
          getChannel(), getUpdateEnergyLimitMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of VoteWitnessAccount.
     * </pre>
     */
    public org.tron.api.GrpcAPI.TransactionExtention voteWitnessAccount2(org.tron.protos.Contract.VoteWitnessContract request) {
      return blockingUnaryCall(
          getChannel(), getVoteWitnessAccount2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use CreateAssetIssue2 instead of this function.
     * </pre>
     */
    public org.tron.protos.Protocol.Transaction createAssetIssue(org.tron.protos.Contract.AssetIssueContract request) {
      return blockingUnaryCall(
          getChannel(), getCreateAssetIssueMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of CreateAssetIssue.
     * </pre>
     */
    public org.tron.api.GrpcAPI.TransactionExtention createAssetIssue2(org.tron.protos.Contract.AssetIssueContract request) {
      return blockingUnaryCall(
          getChannel(), getCreateAssetIssue2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use UpdateWitness2 instead of this function.
     * </pre>
     */
    public org.tron.protos.Protocol.Transaction updateWitness(org.tron.protos.Contract.WitnessUpdateContract request) {
      return blockingUnaryCall(
          getChannel(), getUpdateWitnessMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of UpdateWitness.
     * </pre>
     */
    public org.tron.api.GrpcAPI.TransactionExtention updateWitness2(org.tron.protos.Contract.WitnessUpdateContract request) {
      return blockingUnaryCall(
          getChannel(), getUpdateWitness2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use CreateAccount2 instead of this function.
     * </pre>
     */
    public org.tron.protos.Protocol.Transaction createAccount(org.tron.protos.Contract.AccountCreateContract request) {
      return blockingUnaryCall(
          getChannel(), getCreateAccountMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of CreateAccount.
     * </pre>
     */
    public org.tron.api.GrpcAPI.TransactionExtention createAccount2(org.tron.protos.Contract.AccountCreateContract request) {
      return blockingUnaryCall(
          getChannel(), getCreateAccount2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use CreateWitness2 instead of this function.
     * </pre>
     */
    public org.tron.protos.Protocol.Transaction createWitness(org.tron.protos.Contract.WitnessCreateContract request) {
      return blockingUnaryCall(
          getChannel(), getCreateWitnessMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of CreateWitness.
     * </pre>
     */
    public org.tron.api.GrpcAPI.TransactionExtention createWitness2(org.tron.protos.Contract.WitnessCreateContract request) {
      return blockingUnaryCall(
          getChannel(), getCreateWitness2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use TransferAsset2 instead of this function.
     * </pre>
     */
    public org.tron.protos.Protocol.Transaction transferAsset(org.tron.protos.Contract.TransferAssetContract request) {
      return blockingUnaryCall(
          getChannel(), getTransferAssetMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of TransferAsset.
     * </pre>
     */
    public org.tron.api.GrpcAPI.TransactionExtention transferAsset2(org.tron.protos.Contract.TransferAssetContract request) {
      return blockingUnaryCall(
          getChannel(), getTransferAsset2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use ParticipateAssetIssue2 instead of this function.
     * </pre>
     */
    public org.tron.protos.Protocol.Transaction participateAssetIssue(org.tron.protos.Contract.ParticipateAssetIssueContract request) {
      return blockingUnaryCall(
          getChannel(), getParticipateAssetIssueMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of ParticipateAssetIssue.
     * </pre>
     */
    public org.tron.api.GrpcAPI.TransactionExtention participateAssetIssue2(org.tron.protos.Contract.ParticipateAssetIssueContract request) {
      return blockingUnaryCall(
          getChannel(), getParticipateAssetIssue2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use FreezeBalance2 instead of this function.
     * </pre>
     */
    public org.tron.protos.Protocol.Transaction freezeBalance(org.tron.protos.Contract.FreezeBalanceContract request) {
      return blockingUnaryCall(
          getChannel(), getFreezeBalanceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of FreezeBalance.
     * </pre>
     */
    public org.tron.api.GrpcAPI.TransactionExtention freezeBalance2(org.tron.protos.Contract.FreezeBalanceContract request) {
      return blockingUnaryCall(
          getChannel(), getFreezeBalance2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use UnfreezeBalance2 instead of this function.
     * </pre>
     */
    public org.tron.protos.Protocol.Transaction unfreezeBalance(org.tron.protos.Contract.UnfreezeBalanceContract request) {
      return blockingUnaryCall(
          getChannel(), getUnfreezeBalanceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of UnfreezeBalance.
     * </pre>
     */
    public org.tron.api.GrpcAPI.TransactionExtention unfreezeBalance2(org.tron.protos.Contract.UnfreezeBalanceContract request) {
      return blockingUnaryCall(
          getChannel(), getUnfreezeBalance2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use UnfreezeAsset2 instead of this function.
     * </pre>
     */
    public org.tron.protos.Protocol.Transaction unfreezeAsset(org.tron.protos.Contract.UnfreezeAssetContract request) {
      return blockingUnaryCall(
          getChannel(), getUnfreezeAssetMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of UnfreezeAsset.
     * </pre>
     */
    public org.tron.api.GrpcAPI.TransactionExtention unfreezeAsset2(org.tron.protos.Contract.UnfreezeAssetContract request) {
      return blockingUnaryCall(
          getChannel(), getUnfreezeAsset2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use WithdrawBalance2 instead of this function.
     * </pre>
     */
    public org.tron.protos.Protocol.Transaction withdrawBalance(org.tron.protos.Contract.WithdrawBalanceContract request) {
      return blockingUnaryCall(
          getChannel(), getWithdrawBalanceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of WithdrawBalance.
     * </pre>
     */
    public org.tron.api.GrpcAPI.TransactionExtention withdrawBalance2(org.tron.protos.Contract.WithdrawBalanceContract request) {
      return blockingUnaryCall(
          getChannel(), getWithdrawBalance2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use UpdateAsset2 instead of this function.
     * </pre>
     */
    public org.tron.protos.Protocol.Transaction updateAsset(org.tron.protos.Contract.UpdateAssetContract request) {
      return blockingUnaryCall(
          getChannel(), getUpdateAssetMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of UpdateAsset.
     * </pre>
     */
    public org.tron.api.GrpcAPI.TransactionExtention updateAsset2(org.tron.protos.Contract.UpdateAssetContract request) {
      return blockingUnaryCall(
          getChannel(), getUpdateAsset2Method(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.TransactionExtention proposalCreate(org.tron.protos.Contract.ProposalCreateContract request) {
      return blockingUnaryCall(
          getChannel(), getProposalCreateMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.TransactionExtention proposalApprove(org.tron.protos.Contract.ProposalApproveContract request) {
      return blockingUnaryCall(
          getChannel(), getProposalApproveMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.TransactionExtention proposalDelete(org.tron.protos.Contract.ProposalDeleteContract request) {
      return blockingUnaryCall(
          getChannel(), getProposalDeleteMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.TransactionExtention buyStorage(org.tron.protos.Contract.BuyStorageContract request) {
      return blockingUnaryCall(
          getChannel(), getBuyStorageMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.TransactionExtention buyStorageBytes(org.tron.protos.Contract.BuyStorageBytesContract request) {
      return blockingUnaryCall(
          getChannel(), getBuyStorageBytesMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.TransactionExtention sellStorage(org.tron.protos.Contract.SellStorageContract request) {
      return blockingUnaryCall(
          getChannel(), getSellStorageMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.TransactionExtention exchangeCreate(org.tron.protos.Contract.ExchangeCreateContract request) {
      return blockingUnaryCall(
          getChannel(), getExchangeCreateMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.TransactionExtention exchangeInject(org.tron.protos.Contract.ExchangeInjectContract request) {
      return blockingUnaryCall(
          getChannel(), getExchangeInjectMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.TransactionExtention exchangeWithdraw(org.tron.protos.Contract.ExchangeWithdrawContract request) {
      return blockingUnaryCall(
          getChannel(), getExchangeWithdrawMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.TransactionExtention exchangeTransaction(org.tron.protos.Contract.ExchangeTransactionContract request) {
      return blockingUnaryCall(
          getChannel(), getExchangeTransactionMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.NodeList listNodes(org.tron.api.GrpcAPI.EmptyMessage request) {
      return blockingUnaryCall(
          getChannel(), getListNodesMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.AssetIssueList getAssetIssueByAccount(org.tron.protos.Protocol.Account request) {
      return blockingUnaryCall(
          getChannel(), getGetAssetIssueByAccountMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.AccountNetMessage getAccountNet(org.tron.protos.Protocol.Account request) {
      return blockingUnaryCall(
          getChannel(), getGetAccountNetMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.AccountResourceMessage getAccountResource(org.tron.protos.Protocol.Account request) {
      return blockingUnaryCall(
          getChannel(), getGetAccountResourceMethod(), getCallOptions(), request);
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
    public org.tron.protos.Protocol.Block getBlockById(org.tron.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetBlockByIdMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use GetBlockByLimitNext2 instead of this function.
     * </pre>
     */
    public org.tron.api.GrpcAPI.BlockList getBlockByLimitNext(org.tron.api.GrpcAPI.BlockLimit request) {
      return blockingUnaryCall(
          getChannel(), getGetBlockByLimitNextMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of GetBlockByLimitNext.
     * </pre>
     */
    public org.tron.api.GrpcAPI.BlockListExtention getBlockByLimitNext2(org.tron.api.GrpcAPI.BlockLimit request) {
      return blockingUnaryCall(
          getChannel(), getGetBlockByLimitNext2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use GetBlockByLatestNum2 instead of this function.
     * </pre>
     */
    public org.tron.api.GrpcAPI.BlockList getBlockByLatestNum(org.tron.api.GrpcAPI.NumberMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetBlockByLatestNumMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of GetBlockByLatestNum.
     * </pre>
     */
    public org.tron.api.GrpcAPI.BlockListExtention getBlockByLatestNum2(org.tron.api.GrpcAPI.NumberMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetBlockByLatestNum2Method(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.protos.Protocol.Transaction getTransactionById(org.tron.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetTransactionByIdMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.TransactionExtention deployContract(org.tron.protos.Contract.CreateSmartContract request) {
      return blockingUnaryCall(
          getChannel(), getDeployContractMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.protos.Protocol.SmartContract getContract(org.tron.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetContractMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.TransactionExtention triggerContract(org.tron.protos.Contract.TriggerSmartContract request) {
      return blockingUnaryCall(
          getChannel(), getTriggerContractMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.TransactionExtention triggerConstantContract(org.tron.protos.Contract.TriggerSmartContract request) {
      return blockingUnaryCall(
          getChannel(), getTriggerConstantContractMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.TransactionExtention clearContractABI(org.tron.protos.Contract.ClearABIContract request) {
      return blockingUnaryCall(
          getChannel(), getClearContractABIMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.WitnessList listWitnesses(org.tron.api.GrpcAPI.EmptyMessage request) {
      return blockingUnaryCall(
          getChannel(), getListWitnessesMethod(), getCallOptions(), request);
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
    public org.tron.api.GrpcAPI.ProposalList listProposals(org.tron.api.GrpcAPI.EmptyMessage request) {
      return blockingUnaryCall(
          getChannel(), getListProposalsMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.ProposalList getPaginatedProposalList(org.tron.api.GrpcAPI.PaginatedMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetPaginatedProposalListMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.protos.Protocol.Proposal getProposalById(org.tron.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetProposalByIdMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.ExchangeList listExchanges(org.tron.api.GrpcAPI.EmptyMessage request) {
      return blockingUnaryCall(
          getChannel(), getListExchangesMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.ExchangeList getPaginatedExchangeList(org.tron.api.GrpcAPI.PaginatedMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetPaginatedExchangeListMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.protos.Protocol.Exchange getExchangeById(org.tron.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetExchangeByIdMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.protos.Protocol.ChainParameters getChainParameters(org.tron.api.GrpcAPI.EmptyMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetChainParametersMethod(), getCallOptions(), request);
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
    public org.tron.api.GrpcAPI.NumberMessage totalTransaction(org.tron.api.GrpcAPI.EmptyMessage request) {
      return blockingUnaryCall(
          getChannel(), getTotalTransactionMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.NumberMessage getNextMaintenanceTime(org.tron.api.GrpcAPI.EmptyMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetNextMaintenanceTimeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     *Please use GetTransactionSign2 instead of this function.
     * </pre>
     */
    public org.tron.protos.Protocol.Transaction getTransactionSign(org.tron.protos.Protocol.TransactionSign request) {
      return blockingUnaryCall(
          getChannel(), getGetTransactionSignMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     *Use this function instead of GetTransactionSign.
     * </pre>
     */
    public org.tron.api.GrpcAPI.TransactionExtention getTransactionSign2(org.tron.protos.Protocol.TransactionSign request) {
      return blockingUnaryCall(
          getChannel(), getGetTransactionSign2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public org.tron.api.GrpcAPI.BytesMessage createAddress(org.tron.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getCreateAddressMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public org.tron.api.GrpcAPI.EasyTransferResponse easyTransferAsset(org.tron.api.GrpcAPI.EasyTransferAssetMessage request) {
      return blockingUnaryCall(
          getChannel(), getEasyTransferAssetMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public org.tron.api.GrpcAPI.EasyTransferResponse easyTransferAssetByPrivate(org.tron.api.GrpcAPI.EasyTransferAssetByPrivateMessage request) {
      return blockingUnaryCall(
          getChannel(), getEasyTransferAssetByPrivateMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public org.tron.api.GrpcAPI.EasyTransferResponse easyTransfer(org.tron.api.GrpcAPI.EasyTransferMessage request) {
      return blockingUnaryCall(
          getChannel(), getEasyTransferMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public org.tron.api.GrpcAPI.EasyTransferResponse easyTransferByPrivate(org.tron.api.GrpcAPI.EasyTransferByPrivateMessage request) {
      return blockingUnaryCall(
          getChannel(), getEasyTransferByPrivateMethod(), getCallOptions(), request);
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

    /**
     */
    public org.tron.protos.Protocol.TransactionInfo getTransactionInfoById(org.tron.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetTransactionInfoByIdMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.TransactionExtention accountPermissionUpdate(org.tron.protos.Contract.AccountPermissionUpdateContract request) {
      return blockingUnaryCall(
          getChannel(), getAccountPermissionUpdateMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.TransactionExtention addSign(org.tron.protos.Protocol.TransactionSign request) {
      return blockingUnaryCall(
          getChannel(), getAddSignMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.TransactionSignWeight getTransactionSignWeight(org.tron.protos.Protocol.Transaction request) {
      return blockingUnaryCall(
          getChannel(), getGetTransactionSignWeightMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.api.GrpcAPI.TransactionApprovedList getTransactionApprovedList(org.tron.protos.Protocol.Transaction request) {
      return blockingUnaryCall(
          getChannel(), getGetTransactionApprovedListMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.tron.protos.Protocol.NodeInfo getNodeInfo(org.tron.api.GrpcAPI.EmptyMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetNodeInfoMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class WalletFutureStub extends io.grpc.stub.AbstractStub<WalletFutureStub> {
    private WalletFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private WalletFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WalletFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new WalletFutureStub(channel, callOptions);
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
     * <pre>
     *Please use CreateTransaction2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.Transaction> createTransaction(
        org.tron.protos.Contract.TransferContract request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateTransactionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of CreateTransaction.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> createTransaction2(
        org.tron.protos.Contract.TransferContract request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateTransaction2Method(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.Return> broadcastTransaction(
        org.tron.protos.Protocol.Transaction request) {
      return futureUnaryCall(
          getChannel().newCall(getBroadcastTransactionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use UpdateAccount2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.Transaction> updateAccount(
        org.tron.protos.Contract.AccountUpdateContract request) {
      return futureUnaryCall(
          getChannel().newCall(getUpdateAccountMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.Transaction> setAccountId(
        org.tron.protos.Contract.SetAccountIdContract request) {
      return futureUnaryCall(
          getChannel().newCall(getSetAccountIdMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of UpdateAccount.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> updateAccount2(
        org.tron.protos.Contract.AccountUpdateContract request) {
      return futureUnaryCall(
          getChannel().newCall(getUpdateAccount2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use VoteWitnessAccount2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.Transaction> voteWitnessAccount(
        org.tron.protos.Contract.VoteWitnessContract request) {
      return futureUnaryCall(
          getChannel().newCall(getVoteWitnessAccountMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *modify the consume_user_resource_percent
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> updateSetting(
        org.tron.protos.Contract.UpdateSettingContract request) {
      return futureUnaryCall(
          getChannel().newCall(getUpdateSettingMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *modify the energy_limit
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> updateEnergyLimit(
        org.tron.protos.Contract.UpdateEnergyLimitContract request) {
      return futureUnaryCall(
          getChannel().newCall(getUpdateEnergyLimitMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of VoteWitnessAccount.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> voteWitnessAccount2(
        org.tron.protos.Contract.VoteWitnessContract request) {
      return futureUnaryCall(
          getChannel().newCall(getVoteWitnessAccount2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use CreateAssetIssue2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.Transaction> createAssetIssue(
        org.tron.protos.Contract.AssetIssueContract request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateAssetIssueMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of CreateAssetIssue.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> createAssetIssue2(
        org.tron.protos.Contract.AssetIssueContract request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateAssetIssue2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use UpdateWitness2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.Transaction> updateWitness(
        org.tron.protos.Contract.WitnessUpdateContract request) {
      return futureUnaryCall(
          getChannel().newCall(getUpdateWitnessMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of UpdateWitness.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> updateWitness2(
        org.tron.protos.Contract.WitnessUpdateContract request) {
      return futureUnaryCall(
          getChannel().newCall(getUpdateWitness2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use CreateAccount2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.Transaction> createAccount(
        org.tron.protos.Contract.AccountCreateContract request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateAccountMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of CreateAccount.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> createAccount2(
        org.tron.protos.Contract.AccountCreateContract request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateAccount2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use CreateWitness2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.Transaction> createWitness(
        org.tron.protos.Contract.WitnessCreateContract request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateWitnessMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of CreateWitness.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> createWitness2(
        org.tron.protos.Contract.WitnessCreateContract request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateWitness2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use TransferAsset2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.Transaction> transferAsset(
        org.tron.protos.Contract.TransferAssetContract request) {
      return futureUnaryCall(
          getChannel().newCall(getTransferAssetMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of TransferAsset.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> transferAsset2(
        org.tron.protos.Contract.TransferAssetContract request) {
      return futureUnaryCall(
          getChannel().newCall(getTransferAsset2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use ParticipateAssetIssue2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.Transaction> participateAssetIssue(
        org.tron.protos.Contract.ParticipateAssetIssueContract request) {
      return futureUnaryCall(
          getChannel().newCall(getParticipateAssetIssueMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of ParticipateAssetIssue.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> participateAssetIssue2(
        org.tron.protos.Contract.ParticipateAssetIssueContract request) {
      return futureUnaryCall(
          getChannel().newCall(getParticipateAssetIssue2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use FreezeBalance2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.Transaction> freezeBalance(
        org.tron.protos.Contract.FreezeBalanceContract request) {
      return futureUnaryCall(
          getChannel().newCall(getFreezeBalanceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of FreezeBalance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> freezeBalance2(
        org.tron.protos.Contract.FreezeBalanceContract request) {
      return futureUnaryCall(
          getChannel().newCall(getFreezeBalance2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use UnfreezeBalance2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.Transaction> unfreezeBalance(
        org.tron.protos.Contract.UnfreezeBalanceContract request) {
      return futureUnaryCall(
          getChannel().newCall(getUnfreezeBalanceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of UnfreezeBalance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> unfreezeBalance2(
        org.tron.protos.Contract.UnfreezeBalanceContract request) {
      return futureUnaryCall(
          getChannel().newCall(getUnfreezeBalance2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use UnfreezeAsset2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.Transaction> unfreezeAsset(
        org.tron.protos.Contract.UnfreezeAssetContract request) {
      return futureUnaryCall(
          getChannel().newCall(getUnfreezeAssetMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of UnfreezeAsset.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> unfreezeAsset2(
        org.tron.protos.Contract.UnfreezeAssetContract request) {
      return futureUnaryCall(
          getChannel().newCall(getUnfreezeAsset2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use WithdrawBalance2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.Transaction> withdrawBalance(
        org.tron.protos.Contract.WithdrawBalanceContract request) {
      return futureUnaryCall(
          getChannel().newCall(getWithdrawBalanceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of WithdrawBalance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> withdrawBalance2(
        org.tron.protos.Contract.WithdrawBalanceContract request) {
      return futureUnaryCall(
          getChannel().newCall(getWithdrawBalance2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use UpdateAsset2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.Transaction> updateAsset(
        org.tron.protos.Contract.UpdateAssetContract request) {
      return futureUnaryCall(
          getChannel().newCall(getUpdateAssetMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of UpdateAsset.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> updateAsset2(
        org.tron.protos.Contract.UpdateAssetContract request) {
      return futureUnaryCall(
          getChannel().newCall(getUpdateAsset2Method(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> proposalCreate(
        org.tron.protos.Contract.ProposalCreateContract request) {
      return futureUnaryCall(
          getChannel().newCall(getProposalCreateMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> proposalApprove(
        org.tron.protos.Contract.ProposalApproveContract request) {
      return futureUnaryCall(
          getChannel().newCall(getProposalApproveMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> proposalDelete(
        org.tron.protos.Contract.ProposalDeleteContract request) {
      return futureUnaryCall(
          getChannel().newCall(getProposalDeleteMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> buyStorage(
        org.tron.protos.Contract.BuyStorageContract request) {
      return futureUnaryCall(
          getChannel().newCall(getBuyStorageMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> buyStorageBytes(
        org.tron.protos.Contract.BuyStorageBytesContract request) {
      return futureUnaryCall(
          getChannel().newCall(getBuyStorageBytesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> sellStorage(
        org.tron.protos.Contract.SellStorageContract request) {
      return futureUnaryCall(
          getChannel().newCall(getSellStorageMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> exchangeCreate(
        org.tron.protos.Contract.ExchangeCreateContract request) {
      return futureUnaryCall(
          getChannel().newCall(getExchangeCreateMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> exchangeInject(
        org.tron.protos.Contract.ExchangeInjectContract request) {
      return futureUnaryCall(
          getChannel().newCall(getExchangeInjectMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> exchangeWithdraw(
        org.tron.protos.Contract.ExchangeWithdrawContract request) {
      return futureUnaryCall(
          getChannel().newCall(getExchangeWithdrawMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> exchangeTransaction(
        org.tron.protos.Contract.ExchangeTransactionContract request) {
      return futureUnaryCall(
          getChannel().newCall(getExchangeTransactionMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.NodeList> listNodes(
        org.tron.api.GrpcAPI.EmptyMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getListNodesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.AssetIssueList> getAssetIssueByAccount(
        org.tron.protos.Protocol.Account request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAssetIssueByAccountMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.AccountNetMessage> getAccountNet(
        org.tron.protos.Protocol.Account request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAccountNetMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.AccountResourceMessage> getAccountResource(
        org.tron.protos.Protocol.Account request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAccountResourceMethod(), getCallOptions()), request);
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
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.Block> getBlockById(
        org.tron.api.GrpcAPI.BytesMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetBlockByIdMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use GetBlockByLimitNext2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.BlockList> getBlockByLimitNext(
        org.tron.api.GrpcAPI.BlockLimit request) {
      return futureUnaryCall(
          getChannel().newCall(getGetBlockByLimitNextMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of GetBlockByLimitNext.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.BlockListExtention> getBlockByLimitNext2(
        org.tron.api.GrpcAPI.BlockLimit request) {
      return futureUnaryCall(
          getChannel().newCall(getGetBlockByLimitNext2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use GetBlockByLatestNum2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.BlockList> getBlockByLatestNum(
        org.tron.api.GrpcAPI.NumberMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetBlockByLatestNumMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of GetBlockByLatestNum.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.BlockListExtention> getBlockByLatestNum2(
        org.tron.api.GrpcAPI.NumberMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetBlockByLatestNum2Method(), getCallOptions()), request);
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
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> deployContract(
        org.tron.protos.Contract.CreateSmartContract request) {
      return futureUnaryCall(
          getChannel().newCall(getDeployContractMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.SmartContract> getContract(
        org.tron.api.GrpcAPI.BytesMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetContractMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> triggerContract(
        org.tron.protos.Contract.TriggerSmartContract request) {
      return futureUnaryCall(
          getChannel().newCall(getTriggerContractMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> triggerConstantContract(
        org.tron.protos.Contract.TriggerSmartContract request) {
      return futureUnaryCall(
          getChannel().newCall(getTriggerConstantContractMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> clearContractABI(
        org.tron.protos.Contract.ClearABIContract request) {
      return futureUnaryCall(
          getChannel().newCall(getClearContractABIMethod(), getCallOptions()), request);
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
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.ProposalList> listProposals(
        org.tron.api.GrpcAPI.EmptyMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getListProposalsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.ProposalList> getPaginatedProposalList(
        org.tron.api.GrpcAPI.PaginatedMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetPaginatedProposalListMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.Proposal> getProposalById(
        org.tron.api.GrpcAPI.BytesMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetProposalByIdMethod(), getCallOptions()), request);
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
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.ExchangeList> getPaginatedExchangeList(
        org.tron.api.GrpcAPI.PaginatedMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetPaginatedExchangeListMethod(), getCallOptions()), request);
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
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.ChainParameters> getChainParameters(
        org.tron.api.GrpcAPI.EmptyMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetChainParametersMethod(), getCallOptions()), request);
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
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.NumberMessage> totalTransaction(
        org.tron.api.GrpcAPI.EmptyMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getTotalTransactionMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.NumberMessage> getNextMaintenanceTime(
        org.tron.api.GrpcAPI.EmptyMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetNextMaintenanceTimeMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     *Please use GetTransactionSign2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.Transaction> getTransactionSign(
        org.tron.protos.Protocol.TransactionSign request) {
      return futureUnaryCall(
          getChannel().newCall(getGetTransactionSignMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     *Use this function instead of GetTransactionSign.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> getTransactionSign2(
        org.tron.protos.Protocol.TransactionSign request) {
      return futureUnaryCall(
          getChannel().newCall(getGetTransactionSign2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.BytesMessage> createAddress(
        org.tron.api.GrpcAPI.BytesMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateAddressMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.EasyTransferResponse> easyTransferAsset(
        org.tron.api.GrpcAPI.EasyTransferAssetMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getEasyTransferAssetMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.EasyTransferResponse> easyTransferAssetByPrivate(
        org.tron.api.GrpcAPI.EasyTransferAssetByPrivateMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getEasyTransferAssetByPrivateMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.EasyTransferResponse> easyTransfer(
        org.tron.api.GrpcAPI.EasyTransferMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getEasyTransferMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.EasyTransferResponse> easyTransferByPrivate(
        org.tron.api.GrpcAPI.EasyTransferByPrivateMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getEasyTransferByPrivateMethod(), getCallOptions()), request);
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

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.TransactionInfo> getTransactionInfoById(
        org.tron.api.GrpcAPI.BytesMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetTransactionInfoByIdMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> accountPermissionUpdate(
        org.tron.protos.Contract.AccountPermissionUpdateContract request) {
      return futureUnaryCall(
          getChannel().newCall(getAccountPermissionUpdateMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionExtention> addSign(
        org.tron.protos.Protocol.TransactionSign request) {
      return futureUnaryCall(
          getChannel().newCall(getAddSignMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionSignWeight> getTransactionSignWeight(
        org.tron.protos.Protocol.Transaction request) {
      return futureUnaryCall(
          getChannel().newCall(getGetTransactionSignWeightMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.api.GrpcAPI.TransactionApprovedList> getTransactionApprovedList(
        org.tron.protos.Protocol.Transaction request) {
      return futureUnaryCall(
          getChannel().newCall(getGetTransactionApprovedListMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.tron.protos.Protocol.NodeInfo> getNodeInfo(
        org.tron.api.GrpcAPI.EmptyMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetNodeInfoMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_ACCOUNT = 0;
  private static final int METHODID_GET_ACCOUNT_BY_ID = 1;
  private static final int METHODID_CREATE_TRANSACTION = 2;
  private static final int METHODID_CREATE_TRANSACTION2 = 3;
  private static final int METHODID_BROADCAST_TRANSACTION = 4;
  private static final int METHODID_UPDATE_ACCOUNT = 5;
  private static final int METHODID_SET_ACCOUNT_ID = 6;
  private static final int METHODID_UPDATE_ACCOUNT2 = 7;
  private static final int METHODID_VOTE_WITNESS_ACCOUNT = 8;
  private static final int METHODID_UPDATE_SETTING = 9;
  private static final int METHODID_UPDATE_ENERGY_LIMIT = 10;
  private static final int METHODID_VOTE_WITNESS_ACCOUNT2 = 11;
  private static final int METHODID_CREATE_ASSET_ISSUE = 12;
  private static final int METHODID_CREATE_ASSET_ISSUE2 = 13;
  private static final int METHODID_UPDATE_WITNESS = 14;
  private static final int METHODID_UPDATE_WITNESS2 = 15;
  private static final int METHODID_CREATE_ACCOUNT = 16;
  private static final int METHODID_CREATE_ACCOUNT2 = 17;
  private static final int METHODID_CREATE_WITNESS = 18;
  private static final int METHODID_CREATE_WITNESS2 = 19;
  private static final int METHODID_TRANSFER_ASSET = 20;
  private static final int METHODID_TRANSFER_ASSET2 = 21;
  private static final int METHODID_PARTICIPATE_ASSET_ISSUE = 22;
  private static final int METHODID_PARTICIPATE_ASSET_ISSUE2 = 23;
  private static final int METHODID_FREEZE_BALANCE = 24;
  private static final int METHODID_FREEZE_BALANCE2 = 25;
  private static final int METHODID_UNFREEZE_BALANCE = 26;
  private static final int METHODID_UNFREEZE_BALANCE2 = 27;
  private static final int METHODID_UNFREEZE_ASSET = 28;
  private static final int METHODID_UNFREEZE_ASSET2 = 29;
  private static final int METHODID_WITHDRAW_BALANCE = 30;
  private static final int METHODID_WITHDRAW_BALANCE2 = 31;
  private static final int METHODID_UPDATE_ASSET = 32;
  private static final int METHODID_UPDATE_ASSET2 = 33;
  private static final int METHODID_PROPOSAL_CREATE = 34;
  private static final int METHODID_PROPOSAL_APPROVE = 35;
  private static final int METHODID_PROPOSAL_DELETE = 36;
  private static final int METHODID_BUY_STORAGE = 37;
  private static final int METHODID_BUY_STORAGE_BYTES = 38;
  private static final int METHODID_SELL_STORAGE = 39;
  private static final int METHODID_EXCHANGE_CREATE = 40;
  private static final int METHODID_EXCHANGE_INJECT = 41;
  private static final int METHODID_EXCHANGE_WITHDRAW = 42;
  private static final int METHODID_EXCHANGE_TRANSACTION = 43;
  private static final int METHODID_LIST_NODES = 44;
  private static final int METHODID_GET_ASSET_ISSUE_BY_ACCOUNT = 45;
  private static final int METHODID_GET_ACCOUNT_NET = 46;
  private static final int METHODID_GET_ACCOUNT_RESOURCE = 47;
  private static final int METHODID_GET_ASSET_ISSUE_BY_NAME = 48;
  private static final int METHODID_GET_ASSET_ISSUE_LIST_BY_NAME = 49;
  private static final int METHODID_GET_ASSET_ISSUE_BY_ID = 50;
  private static final int METHODID_GET_NOW_BLOCK = 51;
  private static final int METHODID_GET_NOW_BLOCK2 = 52;
  private static final int METHODID_GET_BLOCK_BY_NUM = 53;
  private static final int METHODID_GET_BLOCK_BY_NUM2 = 54;
  private static final int METHODID_GET_TRANSACTION_COUNT_BY_BLOCK_NUM = 55;
  private static final int METHODID_GET_BLOCK_BY_ID = 56;
  private static final int METHODID_GET_BLOCK_BY_LIMIT_NEXT = 57;
  private static final int METHODID_GET_BLOCK_BY_LIMIT_NEXT2 = 58;
  private static final int METHODID_GET_BLOCK_BY_LATEST_NUM = 59;
  private static final int METHODID_GET_BLOCK_BY_LATEST_NUM2 = 60;
  private static final int METHODID_GET_TRANSACTION_BY_ID = 61;
  private static final int METHODID_DEPLOY_CONTRACT = 62;
  private static final int METHODID_GET_CONTRACT = 63;
  private static final int METHODID_TRIGGER_CONTRACT = 64;
  private static final int METHODID_TRIGGER_CONSTANT_CONTRACT = 65;
  private static final int METHODID_CLEAR_CONTRACT_ABI = 66;
  private static final int METHODID_LIST_WITNESSES = 67;
  private static final int METHODID_GET_DELEGATED_RESOURCE = 68;
  private static final int METHODID_GET_DELEGATED_RESOURCE_ACCOUNT_INDEX = 69;
  private static final int METHODID_LIST_PROPOSALS = 70;
  private static final int METHODID_GET_PAGINATED_PROPOSAL_LIST = 71;
  private static final int METHODID_GET_PROPOSAL_BY_ID = 72;
  private static final int METHODID_LIST_EXCHANGES = 73;
  private static final int METHODID_GET_PAGINATED_EXCHANGE_LIST = 74;
  private static final int METHODID_GET_EXCHANGE_BY_ID = 75;
  private static final int METHODID_GET_CHAIN_PARAMETERS = 76;
  private static final int METHODID_GET_ASSET_ISSUE_LIST = 77;
  private static final int METHODID_GET_PAGINATED_ASSET_ISSUE_LIST = 78;
  private static final int METHODID_TOTAL_TRANSACTION = 79;
  private static final int METHODID_GET_NEXT_MAINTENANCE_TIME = 80;
  private static final int METHODID_GET_TRANSACTION_SIGN = 81;
  private static final int METHODID_GET_TRANSACTION_SIGN2 = 82;
  private static final int METHODID_CREATE_ADDRESS = 83;
  private static final int METHODID_EASY_TRANSFER_ASSET = 84;
  private static final int METHODID_EASY_TRANSFER_ASSET_BY_PRIVATE = 85;
  private static final int METHODID_EASY_TRANSFER = 86;
  private static final int METHODID_EASY_TRANSFER_BY_PRIVATE = 87;
  private static final int METHODID_GENERATE_ADDRESS = 88;
  private static final int METHODID_GET_TRANSACTION_INFO_BY_ID = 89;
  private static final int METHODID_ACCOUNT_PERMISSION_UPDATE = 90;
  private static final int METHODID_ADD_SIGN = 91;
  private static final int METHODID_GET_TRANSACTION_SIGN_WEIGHT = 92;
  private static final int METHODID_GET_TRANSACTION_APPROVED_LIST = 93;
  private static final int METHODID_GET_NODE_INFO = 94;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final WalletImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(WalletImplBase serviceImpl, int methodId) {
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
        case METHODID_CREATE_TRANSACTION:
          serviceImpl.createTransaction((org.tron.protos.Contract.TransferContract) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction>) responseObserver);
          break;
        case METHODID_CREATE_TRANSACTION2:
          serviceImpl.createTransaction2((org.tron.protos.Contract.TransferContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_BROADCAST_TRANSACTION:
          serviceImpl.broadcastTransaction((org.tron.protos.Protocol.Transaction) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.Return>) responseObserver);
          break;
        case METHODID_UPDATE_ACCOUNT:
          serviceImpl.updateAccount((org.tron.protos.Contract.AccountUpdateContract) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction>) responseObserver);
          break;
        case METHODID_SET_ACCOUNT_ID:
          serviceImpl.setAccountId((org.tron.protos.Contract.SetAccountIdContract) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction>) responseObserver);
          break;
        case METHODID_UPDATE_ACCOUNT2:
          serviceImpl.updateAccount2((org.tron.protos.Contract.AccountUpdateContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_VOTE_WITNESS_ACCOUNT:
          serviceImpl.voteWitnessAccount((org.tron.protos.Contract.VoteWitnessContract) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction>) responseObserver);
          break;
        case METHODID_UPDATE_SETTING:
          serviceImpl.updateSetting((org.tron.protos.Contract.UpdateSettingContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_UPDATE_ENERGY_LIMIT:
          serviceImpl.updateEnergyLimit((org.tron.protos.Contract.UpdateEnergyLimitContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_VOTE_WITNESS_ACCOUNT2:
          serviceImpl.voteWitnessAccount2((org.tron.protos.Contract.VoteWitnessContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_CREATE_ASSET_ISSUE:
          serviceImpl.createAssetIssue((org.tron.protos.Contract.AssetIssueContract) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction>) responseObserver);
          break;
        case METHODID_CREATE_ASSET_ISSUE2:
          serviceImpl.createAssetIssue2((org.tron.protos.Contract.AssetIssueContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_UPDATE_WITNESS:
          serviceImpl.updateWitness((org.tron.protos.Contract.WitnessUpdateContract) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction>) responseObserver);
          break;
        case METHODID_UPDATE_WITNESS2:
          serviceImpl.updateWitness2((org.tron.protos.Contract.WitnessUpdateContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_CREATE_ACCOUNT:
          serviceImpl.createAccount((org.tron.protos.Contract.AccountCreateContract) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction>) responseObserver);
          break;
        case METHODID_CREATE_ACCOUNT2:
          serviceImpl.createAccount2((org.tron.protos.Contract.AccountCreateContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_CREATE_WITNESS:
          serviceImpl.createWitness((org.tron.protos.Contract.WitnessCreateContract) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction>) responseObserver);
          break;
        case METHODID_CREATE_WITNESS2:
          serviceImpl.createWitness2((org.tron.protos.Contract.WitnessCreateContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_TRANSFER_ASSET:
          serviceImpl.transferAsset((org.tron.protos.Contract.TransferAssetContract) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction>) responseObserver);
          break;
        case METHODID_TRANSFER_ASSET2:
          serviceImpl.transferAsset2((org.tron.protos.Contract.TransferAssetContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_PARTICIPATE_ASSET_ISSUE:
          serviceImpl.participateAssetIssue((org.tron.protos.Contract.ParticipateAssetIssueContract) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction>) responseObserver);
          break;
        case METHODID_PARTICIPATE_ASSET_ISSUE2:
          serviceImpl.participateAssetIssue2((org.tron.protos.Contract.ParticipateAssetIssueContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_FREEZE_BALANCE:
          serviceImpl.freezeBalance((org.tron.protos.Contract.FreezeBalanceContract) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction>) responseObserver);
          break;
        case METHODID_FREEZE_BALANCE2:
          serviceImpl.freezeBalance2((org.tron.protos.Contract.FreezeBalanceContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_UNFREEZE_BALANCE:
          serviceImpl.unfreezeBalance((org.tron.protos.Contract.UnfreezeBalanceContract) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction>) responseObserver);
          break;
        case METHODID_UNFREEZE_BALANCE2:
          serviceImpl.unfreezeBalance2((org.tron.protos.Contract.UnfreezeBalanceContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_UNFREEZE_ASSET:
          serviceImpl.unfreezeAsset((org.tron.protos.Contract.UnfreezeAssetContract) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction>) responseObserver);
          break;
        case METHODID_UNFREEZE_ASSET2:
          serviceImpl.unfreezeAsset2((org.tron.protos.Contract.UnfreezeAssetContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_WITHDRAW_BALANCE:
          serviceImpl.withdrawBalance((org.tron.protos.Contract.WithdrawBalanceContract) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction>) responseObserver);
          break;
        case METHODID_WITHDRAW_BALANCE2:
          serviceImpl.withdrawBalance2((org.tron.protos.Contract.WithdrawBalanceContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_UPDATE_ASSET:
          serviceImpl.updateAsset((org.tron.protos.Contract.UpdateAssetContract) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction>) responseObserver);
          break;
        case METHODID_UPDATE_ASSET2:
          serviceImpl.updateAsset2((org.tron.protos.Contract.UpdateAssetContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_PROPOSAL_CREATE:
          serviceImpl.proposalCreate((org.tron.protos.Contract.ProposalCreateContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_PROPOSAL_APPROVE:
          serviceImpl.proposalApprove((org.tron.protos.Contract.ProposalApproveContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_PROPOSAL_DELETE:
          serviceImpl.proposalDelete((org.tron.protos.Contract.ProposalDeleteContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_BUY_STORAGE:
          serviceImpl.buyStorage((org.tron.protos.Contract.BuyStorageContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_BUY_STORAGE_BYTES:
          serviceImpl.buyStorageBytes((org.tron.protos.Contract.BuyStorageBytesContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_SELL_STORAGE:
          serviceImpl.sellStorage((org.tron.protos.Contract.SellStorageContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_EXCHANGE_CREATE:
          serviceImpl.exchangeCreate((org.tron.protos.Contract.ExchangeCreateContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_EXCHANGE_INJECT:
          serviceImpl.exchangeInject((org.tron.protos.Contract.ExchangeInjectContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_EXCHANGE_WITHDRAW:
          serviceImpl.exchangeWithdraw((org.tron.protos.Contract.ExchangeWithdrawContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_EXCHANGE_TRANSACTION:
          serviceImpl.exchangeTransaction((org.tron.protos.Contract.ExchangeTransactionContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_LIST_NODES:
          serviceImpl.listNodes((org.tron.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.NodeList>) responseObserver);
          break;
        case METHODID_GET_ASSET_ISSUE_BY_ACCOUNT:
          serviceImpl.getAssetIssueByAccount((org.tron.protos.Protocol.Account) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.AssetIssueList>) responseObserver);
          break;
        case METHODID_GET_ACCOUNT_NET:
          serviceImpl.getAccountNet((org.tron.protos.Protocol.Account) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.AccountNetMessage>) responseObserver);
          break;
        case METHODID_GET_ACCOUNT_RESOURCE:
          serviceImpl.getAccountResource((org.tron.protos.Protocol.Account) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.AccountResourceMessage>) responseObserver);
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
        case METHODID_GET_BLOCK_BY_ID:
          serviceImpl.getBlockById((org.tron.api.GrpcAPI.BytesMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Block>) responseObserver);
          break;
        case METHODID_GET_BLOCK_BY_LIMIT_NEXT:
          serviceImpl.getBlockByLimitNext((org.tron.api.GrpcAPI.BlockLimit) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.BlockList>) responseObserver);
          break;
        case METHODID_GET_BLOCK_BY_LIMIT_NEXT2:
          serviceImpl.getBlockByLimitNext2((org.tron.api.GrpcAPI.BlockLimit) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.BlockListExtention>) responseObserver);
          break;
        case METHODID_GET_BLOCK_BY_LATEST_NUM:
          serviceImpl.getBlockByLatestNum((org.tron.api.GrpcAPI.NumberMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.BlockList>) responseObserver);
          break;
        case METHODID_GET_BLOCK_BY_LATEST_NUM2:
          serviceImpl.getBlockByLatestNum2((org.tron.api.GrpcAPI.NumberMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.BlockListExtention>) responseObserver);
          break;
        case METHODID_GET_TRANSACTION_BY_ID:
          serviceImpl.getTransactionById((org.tron.api.GrpcAPI.BytesMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction>) responseObserver);
          break;
        case METHODID_DEPLOY_CONTRACT:
          serviceImpl.deployContract((org.tron.protos.Contract.CreateSmartContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_GET_CONTRACT:
          serviceImpl.getContract((org.tron.api.GrpcAPI.BytesMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.SmartContract>) responseObserver);
          break;
        case METHODID_TRIGGER_CONTRACT:
          serviceImpl.triggerContract((org.tron.protos.Contract.TriggerSmartContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_TRIGGER_CONSTANT_CONTRACT:
          serviceImpl.triggerConstantContract((org.tron.protos.Contract.TriggerSmartContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_CLEAR_CONTRACT_ABI:
          serviceImpl.clearContractABI((org.tron.protos.Contract.ClearABIContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_LIST_WITNESSES:
          serviceImpl.listWitnesses((org.tron.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.WitnessList>) responseObserver);
          break;
        case METHODID_GET_DELEGATED_RESOURCE:
          serviceImpl.getDelegatedResource((org.tron.api.GrpcAPI.DelegatedResourceMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.DelegatedResourceList>) responseObserver);
          break;
        case METHODID_GET_DELEGATED_RESOURCE_ACCOUNT_INDEX:
          serviceImpl.getDelegatedResourceAccountIndex((org.tron.api.GrpcAPI.BytesMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.DelegatedResourceAccountIndex>) responseObserver);
          break;
        case METHODID_LIST_PROPOSALS:
          serviceImpl.listProposals((org.tron.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.ProposalList>) responseObserver);
          break;
        case METHODID_GET_PAGINATED_PROPOSAL_LIST:
          serviceImpl.getPaginatedProposalList((org.tron.api.GrpcAPI.PaginatedMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.ProposalList>) responseObserver);
          break;
        case METHODID_GET_PROPOSAL_BY_ID:
          serviceImpl.getProposalById((org.tron.api.GrpcAPI.BytesMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Proposal>) responseObserver);
          break;
        case METHODID_LIST_EXCHANGES:
          serviceImpl.listExchanges((org.tron.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.ExchangeList>) responseObserver);
          break;
        case METHODID_GET_PAGINATED_EXCHANGE_LIST:
          serviceImpl.getPaginatedExchangeList((org.tron.api.GrpcAPI.PaginatedMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.ExchangeList>) responseObserver);
          break;
        case METHODID_GET_EXCHANGE_BY_ID:
          serviceImpl.getExchangeById((org.tron.api.GrpcAPI.BytesMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Exchange>) responseObserver);
          break;
        case METHODID_GET_CHAIN_PARAMETERS:
          serviceImpl.getChainParameters((org.tron.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.ChainParameters>) responseObserver);
          break;
        case METHODID_GET_ASSET_ISSUE_LIST:
          serviceImpl.getAssetIssueList((org.tron.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.AssetIssueList>) responseObserver);
          break;
        case METHODID_GET_PAGINATED_ASSET_ISSUE_LIST:
          serviceImpl.getPaginatedAssetIssueList((org.tron.api.GrpcAPI.PaginatedMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.AssetIssueList>) responseObserver);
          break;
        case METHODID_TOTAL_TRANSACTION:
          serviceImpl.totalTransaction((org.tron.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.NumberMessage>) responseObserver);
          break;
        case METHODID_GET_NEXT_MAINTENANCE_TIME:
          serviceImpl.getNextMaintenanceTime((org.tron.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.NumberMessage>) responseObserver);
          break;
        case METHODID_GET_TRANSACTION_SIGN:
          serviceImpl.getTransactionSign((org.tron.protos.Protocol.TransactionSign) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.Transaction>) responseObserver);
          break;
        case METHODID_GET_TRANSACTION_SIGN2:
          serviceImpl.getTransactionSign2((org.tron.protos.Protocol.TransactionSign) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_CREATE_ADDRESS:
          serviceImpl.createAddress((org.tron.api.GrpcAPI.BytesMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.BytesMessage>) responseObserver);
          break;
        case METHODID_EASY_TRANSFER_ASSET:
          serviceImpl.easyTransferAsset((org.tron.api.GrpcAPI.EasyTransferAssetMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.EasyTransferResponse>) responseObserver);
          break;
        case METHODID_EASY_TRANSFER_ASSET_BY_PRIVATE:
          serviceImpl.easyTransferAssetByPrivate((org.tron.api.GrpcAPI.EasyTransferAssetByPrivateMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.EasyTransferResponse>) responseObserver);
          break;
        case METHODID_EASY_TRANSFER:
          serviceImpl.easyTransfer((org.tron.api.GrpcAPI.EasyTransferMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.EasyTransferResponse>) responseObserver);
          break;
        case METHODID_EASY_TRANSFER_BY_PRIVATE:
          serviceImpl.easyTransferByPrivate((org.tron.api.GrpcAPI.EasyTransferByPrivateMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.EasyTransferResponse>) responseObserver);
          break;
        case METHODID_GENERATE_ADDRESS:
          serviceImpl.generateAddress((org.tron.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.AddressPrKeyPairMessage>) responseObserver);
          break;
        case METHODID_GET_TRANSACTION_INFO_BY_ID:
          serviceImpl.getTransactionInfoById((org.tron.api.GrpcAPI.BytesMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.TransactionInfo>) responseObserver);
          break;
        case METHODID_ACCOUNT_PERMISSION_UPDATE:
          serviceImpl.accountPermissionUpdate((org.tron.protos.Contract.AccountPermissionUpdateContract) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_ADD_SIGN:
          serviceImpl.addSign((org.tron.protos.Protocol.TransactionSign) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionExtention>) responseObserver);
          break;
        case METHODID_GET_TRANSACTION_SIGN_WEIGHT:
          serviceImpl.getTransactionSignWeight((org.tron.protos.Protocol.Transaction) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionSignWeight>) responseObserver);
          break;
        case METHODID_GET_TRANSACTION_APPROVED_LIST:
          serviceImpl.getTransactionApprovedList((org.tron.protos.Protocol.Transaction) request,
              (io.grpc.stub.StreamObserver<org.tron.api.GrpcAPI.TransactionApprovedList>) responseObserver);
          break;
        case METHODID_GET_NODE_INFO:
          serviceImpl.getNodeInfo((org.tron.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<org.tron.protos.Protocol.NodeInfo>) responseObserver);
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

  private static abstract class WalletBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    WalletBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.tron.api.GrpcAPI.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Wallet");
    }
  }

  private static final class WalletFileDescriptorSupplier
      extends WalletBaseDescriptorSupplier {
    WalletFileDescriptorSupplier() {}
  }

  private static final class WalletMethodDescriptorSupplier
      extends WalletBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    WalletMethodDescriptorSupplier(String methodName) {
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
      synchronized (WalletGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new WalletFileDescriptorSupplier())
              .addMethod(getGetAccountMethod())
              .addMethod(getGetAccountByIdMethod())
              .addMethod(getCreateTransactionMethod())
              .addMethod(getCreateTransaction2Method())
              .addMethod(getBroadcastTransactionMethod())
              .addMethod(getUpdateAccountMethod())
              .addMethod(getSetAccountIdMethod())
              .addMethod(getUpdateAccount2Method())
              .addMethod(getVoteWitnessAccountMethod())
              .addMethod(getUpdateSettingMethod())
              .addMethod(getUpdateEnergyLimitMethod())
              .addMethod(getVoteWitnessAccount2Method())
              .addMethod(getCreateAssetIssueMethod())
              .addMethod(getCreateAssetIssue2Method())
              .addMethod(getUpdateWitnessMethod())
              .addMethod(getUpdateWitness2Method())
              .addMethod(getCreateAccountMethod())
              .addMethod(getCreateAccount2Method())
              .addMethod(getCreateWitnessMethod())
              .addMethod(getCreateWitness2Method())
              .addMethod(getTransferAssetMethod())
              .addMethod(getTransferAsset2Method())
              .addMethod(getParticipateAssetIssueMethod())
              .addMethod(getParticipateAssetIssue2Method())
              .addMethod(getFreezeBalanceMethod())
              .addMethod(getFreezeBalance2Method())
              .addMethod(getUnfreezeBalanceMethod())
              .addMethod(getUnfreezeBalance2Method())
              .addMethod(getUnfreezeAssetMethod())
              .addMethod(getUnfreezeAsset2Method())
              .addMethod(getWithdrawBalanceMethod())
              .addMethod(getWithdrawBalance2Method())
              .addMethod(getUpdateAssetMethod())
              .addMethod(getUpdateAsset2Method())
              .addMethod(getProposalCreateMethod())
              .addMethod(getProposalApproveMethod())
              .addMethod(getProposalDeleteMethod())
              .addMethod(getBuyStorageMethod())
              .addMethod(getBuyStorageBytesMethod())
              .addMethod(getSellStorageMethod())
              .addMethod(getExchangeCreateMethod())
              .addMethod(getExchangeInjectMethod())
              .addMethod(getExchangeWithdrawMethod())
              .addMethod(getExchangeTransactionMethod())
              .addMethod(getListNodesMethod())
              .addMethod(getGetAssetIssueByAccountMethod())
              .addMethod(getGetAccountNetMethod())
              .addMethod(getGetAccountResourceMethod())
              .addMethod(getGetAssetIssueByNameMethod())
              .addMethod(getGetAssetIssueListByNameMethod())
              .addMethod(getGetAssetIssueByIdMethod())
              .addMethod(getGetNowBlockMethod())
              .addMethod(getGetNowBlock2Method())
              .addMethod(getGetBlockByNumMethod())
              .addMethod(getGetBlockByNum2Method())
              .addMethod(getGetTransactionCountByBlockNumMethod())
              .addMethod(getGetBlockByIdMethod())
              .addMethod(getGetBlockByLimitNextMethod())
              .addMethod(getGetBlockByLimitNext2Method())
              .addMethod(getGetBlockByLatestNumMethod())
              .addMethod(getGetBlockByLatestNum2Method())
              .addMethod(getGetTransactionByIdMethod())
              .addMethod(getDeployContractMethod())
              .addMethod(getGetContractMethod())
              .addMethod(getTriggerContractMethod())
              .addMethod(getTriggerConstantContractMethod())
              .addMethod(getClearContractABIMethod())
              .addMethod(getListWitnessesMethod())
              .addMethod(getGetDelegatedResourceMethod())
              .addMethod(getGetDelegatedResourceAccountIndexMethod())
              .addMethod(getListProposalsMethod())
              .addMethod(getGetPaginatedProposalListMethod())
              .addMethod(getGetProposalByIdMethod())
              .addMethod(getListExchangesMethod())
              .addMethod(getGetPaginatedExchangeListMethod())
              .addMethod(getGetExchangeByIdMethod())
              .addMethod(getGetChainParametersMethod())
              .addMethod(getGetAssetIssueListMethod())
              .addMethod(getGetPaginatedAssetIssueListMethod())
              .addMethod(getTotalTransactionMethod())
              .addMethod(getGetNextMaintenanceTimeMethod())
              .addMethod(getGetTransactionSignMethod())
              .addMethod(getGetTransactionSign2Method())
              .addMethod(getCreateAddressMethod())
              .addMethod(getEasyTransferAssetMethod())
              .addMethod(getEasyTransferAssetByPrivateMethod())
              .addMethod(getEasyTransferMethod())
              .addMethod(getEasyTransferByPrivateMethod())
              .addMethod(getGenerateAddressMethod())
              .addMethod(getGetTransactionInfoByIdMethod())
              .addMethod(getAccountPermissionUpdateMethod())
              .addMethod(getAddSignMethod())
              .addMethod(getGetTransactionSignWeightMethod())
              .addMethod(getGetTransactionApprovedListMethod())
              .addMethod(getGetNodeInfoMethod())
              .build();
        }
      }
    }
    return result;
  }
}
