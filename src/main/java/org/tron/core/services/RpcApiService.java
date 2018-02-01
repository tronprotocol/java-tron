package org.tron.core.services;

import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.logging.Logger;
import org.tron.api.GrpcAPI;
import org.tron.common.application.Application;
import org.tron.common.application.Service;
import org.tron.core.Wallet;
import org.tron.protos.Protocal.Account;
import org.tron.protos.Protocal.Transaction;

public class RpcApiService implements Service {

  private static final Logger logger = Logger.getLogger(RpcApiService.class.getName());
  private int port = 50051;
  private Server ApiServer;
  private Application app;

  public RpcApiService(Application app) {
    this.app = app;
  }

  @Override
  public void init() {

  }

  @Override
  public void start() {
    try {
      ApiServer = ServerBuilder.forPort(port)
          .addService(new WalletApi(app))
          .build()
          .start();
    } catch (IOException e) {
      e.printStackTrace();
    }

    logger.info("Server started, listening on " + port);

    Runtime.getRuntime().addShutdownHook(new Thread() {

      @Override
      public void run() {

        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        //server.this.stop();
        System.err.println("*** server shut down");
      }
    });
  }

  private class WalletApi extends org.tron.api.WalletGrpc.WalletImplBase {

    private Application app;
    private Wallet wallet;

    public WalletApi(Application app) {
      this.app = app;
      this.wallet = new Wallet(this.app);
    }

    @Override
    public void getBalance(Account req, StreamObserver<Account> responseObserver) {
      ByteString addressBS = req.getAddress();
      if (addressBS != null) {
        byte[] addressBA = addressBS.toByteArray();
        long balance = wallet.getBalance(addressBA);
        Account reply = Account.newBuilder().setBalance(balance).build();
        responseObserver.onNext(reply);
      } else {
        responseObserver.onNext(null);
      }
      responseObserver.onCompleted();
    }

    @Override
    public void createTransaction(GrpcAPI.Coin req, StreamObserver<Transaction> responseObserver) {
      ByteString fromBS = req.getFrom();
      ByteString toBS = req.getFrom();
      long amount = req.getAmount();
      if (fromBS != null && toBS != null && amount > 0) {
        byte[] fromBA = fromBS.toByteArray();
        String toBA = toBS.toString();

        Transaction trx = wallet.createTransaction(fromBA, toBA, amount);
        responseObserver.onNext(trx);
      } else {
        responseObserver.onNext(null);
      }
      responseObserver.onCompleted();
    }

    @Override
    public void broadcastTransaction(Transaction req,
        StreamObserver<GrpcAPI.Return> responseObserver) {
      boolean ret = wallet.broadcastTransaction(req);
      GrpcAPI.Return retur = GrpcAPI.Return.newBuilder().setResult(ret).build();
      responseObserver.onNext(retur);
      responseObserver.onCompleted();
    }
  }

  @Override
  public void stop() {

  }

  /**
   * ...
   */
  public void blockUntilShutdown() {
    if (ApiServer != null) {
      try {
        ApiServer.awaitTermination();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
