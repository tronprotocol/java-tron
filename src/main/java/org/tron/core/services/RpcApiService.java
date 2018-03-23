package org.tron.core.services;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountList;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.NodeList;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.common.application.Application;
import org.tron.common.application.Service;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.args.Args;
import org.tron.protos.Contract;
import org.tron.protos.Contract.AccountCreateContract;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Contract.ParticipateAssetIssueContract;
import org.tron.protos.Contract.TransferAssetContract;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Contract.VoteWitnessContract;
import org.tron.protos.Contract.WitnessCreateContract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;

public class RpcApiService implements Service {

  private static final Logger logger = Logger.getLogger(RpcApiService.class.getName());
  private int port = 50051;
  private Server apiServer;
  private Application app;

  public RpcApiService(Application app) {
    this.app = app;
  }

  @Override
  public void init() {

  }

  @Override
  public void init(Args args) {

  }

  @Override
  public void start() {
    try {
      apiServer = ServerBuilder.forPort(port)
          .addService(new WalletApi(app))
          .build()
          .start();
    } catch (IOException e) {
      e.printStackTrace();
    }

    logger.info("Server started, listening on " + port);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.err.println("*** shutting down gRPC server since JVM is shutting down");
      //server.this.stop();
      System.err.println("*** server shut down");
    }));
  }

  private class WalletApi extends org.tron.api.WalletGrpc.WalletImplBase {

    private Application app;
    private Wallet wallet;

    private WalletApi(Application app) {
      this.app = app;
      this.wallet = new Wallet(this.app);
    }


    @Override
    public void getAccount(Account req, StreamObserver<Account> responseObserver) {
      ByteString addressBs = req.getAddress();
      if (addressBs != null) {
        //      byte[] addressBa = addressBs.toByteArray();
        //     long balance = wallet.getBalance(addressBa);
        //    Account reply = Account.newBuilder().setBalance(balance).build();
        Account reply = wallet.getBalance(req);
        responseObserver.onNext(reply);
      } else {
        responseObserver.onNext(null);
      }
      responseObserver.onCompleted();
    }

    @Override
    public void createTransaction(TransferContract req,
        StreamObserver<Transaction> responseObserver) {
      ByteString fromBs = req.getOwnerAddress();
      ByteString toBs = req.getToAddress();
      long amount = req.getAmount();
      if (fromBs != null && toBs != null && amount > 0) {
        Transaction trx = wallet.createTransaction(req);
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

    @Override
    public void createAccount(AccountCreateContract request,
        StreamObserver<Transaction> responseObserver) {
      if (request.getType() == null || request.getAccountName() == null
          || request.getOwnerAddress() == null) {
        responseObserver.onNext(null);
      } else {
        Transaction trx = wallet.createAccount(request);
        responseObserver.onNext(trx);
      }
      responseObserver.onCompleted();
    }


    @Override
    public void createAssetIssue(AssetIssueContract request,
        StreamObserver<Transaction> responseObserver) {
      ByteString owner = request.getOwnerAddress();
      if (owner != null) {
        Transaction trx = wallet.createTransaction(request);
        responseObserver.onNext(trx);
      } else {
        responseObserver.onNext(null);
      }
      responseObserver.onCompleted();
    }

    //refactor、test later
    private void checkVoteWitnessAccount(VoteWitnessContract req) {
      //send back to cli
      ByteString ownerAddress = req.getOwnerAddress();
      Preconditions.checkNotNull(ownerAddress, "OwnerAddress is null");

      AccountCapsule account = app.getDbManager().getAccountStore().get(ownerAddress.toByteArray());
      Preconditions.checkNotNull(account, "OwnerAddress[" + ownerAddress + "] not exists");

      int votesCount = req.getVotesCount();
      Preconditions.checkArgument(votesCount <= 0, "VotesCount[" + votesCount + "] <= 0");
      Preconditions.checkArgument(
              account.getShare() < votesCount,
              "Share[" + account.getShare() + "] <  VotesCount[" + votesCount + "]");

      req.getVotesList().forEach(vote -> {
        ByteString voteAddress = vote.getVoteAddress();
        WitnessCapsule witness = app.getDbManager().getWitnessStore().get(voteAddress.toByteArray());
        Preconditions.checkNotNull(witness, "witness[" + voteAddress + "] not exists");
        Preconditions.checkArgument(
                vote.getVoteCount() <= 0,
                "VoteAddress[" + voteAddress + "],VotesCount[" + vote.getVoteCount() + "] <= 0");
      });
    }

    @Override
    public void voteWitnessAccount(VoteWitnessContract req,
        StreamObserver<Transaction> response) {

      try {
//        checkVoteWitnessAccount(req);//to be complemented later
        Transaction trx = wallet.createTransaction(req);
        response.onNext(trx);
      } catch (Exception ex) {
        response.onNext(null);
      }
      response.onCompleted();
    }

    @Override
    public void createWitness(WitnessCreateContract req,
        StreamObserver<Transaction> responseObserver) {
      ByteString fromBs = req.getOwnerAddress();

      if (fromBs != null) {
        Transaction trx = wallet.createTransaction(req);
        responseObserver.onNext(trx);
      } else {
        responseObserver.onNext(null);
      }
      responseObserver.onCompleted();
    }

    @Override
    public void updateWitness(Contract.WitnessUpdateContract req,
        StreamObserver<Transaction> responseObserver) {
      if (req.getOwnerAddress() != null) {
        Transaction trx = wallet.createTransaction(req);
        responseObserver.onNext(trx);
      } else {
        responseObserver.onNext(null);
      }

      responseObserver.onCompleted();
    }

    @Override
    public void getNowBlock(EmptyMessage request, StreamObserver<Block> responseObserver) {
      responseObserver.onNext(wallet.getNowBlock());
      responseObserver.onCompleted();
    }

    @Override
    public void getBlockByNum(NumberMessage request, StreamObserver<Block> responseObserver) {
      responseObserver.onNext(wallet.getBlockByNum(request.getNum()));
      responseObserver.onCompleted();
    }

    @Override
    public void listAccounts(EmptyMessage request, StreamObserver<AccountList> responseObserver) {
      responseObserver.onNext(wallet.getAllAccounts());
      responseObserver.onCompleted();
    }

    @Override
    public void listWitnesses(EmptyMessage request, StreamObserver<WitnessList> responseObserver) {
      responseObserver.onNext(wallet.getWitnessList());
      responseObserver.onCompleted();
    }

    @Override
    public void listNodes(EmptyMessage request, StreamObserver<NodeList> responseObserver) {
      // TODO: this.app.getP2pNode().getActiveNodes();
      super.listNodes(request, responseObserver);
    }

    @Override
    public void transferAsset(TransferAssetContract request,
        StreamObserver<Transaction> responseObserver) {
      ByteString fromBs = request.getOwnerAddress();

      if (fromBs != null) {
        Transaction trx = wallet.createTransaction(request);
        responseObserver.onNext(trx);
      } else {
        responseObserver.onNext(null);
      }
      responseObserver.onCompleted();
    }

    @Override
    public void participateAssetIssue(ParticipateAssetIssueContract request,
        StreamObserver<Transaction> responseObserver) {
      ByteString fromBs = request.getOwnerAddress();

      if (fromBs != null) {
        Transaction trx = wallet.createTransaction(request);
        responseObserver.onNext(trx);
      } else {
        responseObserver.onNext(null);
      }
      responseObserver.onCompleted();
    }

    @Override
    public void getAssetIssueList(EmptyMessage request,
        StreamObserver<AssetIssueList> responseObserver) {
      responseObserver.onNext(wallet.getAssetIssueList());
      responseObserver.onCompleted();
    }

    @Override
    public void getAssetIssueByAccount(Account request,
        StreamObserver<AssetIssueList> responseObserver) {
      ByteString fromBs = request.getAddress();

      if (fromBs != null) {
        responseObserver.onNext(wallet.getAssetIssueByAccount(fromBs));
      } else {
        responseObserver.onNext(null);
      }
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
    if (apiServer != null) {
      try {
        apiServer.awaitTermination();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
