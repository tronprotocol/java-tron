package org.tron.core.api;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import org.tron.api.GrpcAPI;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.db.AccountStore;
import org.tron.core.db.WitnessStore;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;

import java.util.Iterator;

public class WalletApi extends org.tron.api.WalletGrpc.WalletImplBase {

  private final AccountStore accountStore;
  private Wallet wallet;
  private WitnessStore witnessStore;

  public WalletApi(Wallet wallet, AccountStore accountStore, WitnessStore witnessStore) {
    this.witnessStore = witnessStore;
    this.accountStore = accountStore;
    this.wallet = wallet;
  }


  @Override
  public void getBalance(Protocol.Account req, StreamObserver<Protocol.Account> responseObserver) {
    ByteString addressBs = req.getAddress();
    if (addressBs != null) {
      //      byte[] addressBa = addressBs.toByteArray();
      //     long balance = wallet.getBalance(addressBa);
      //    Account reply = Account.newBuilder().setBalance(balance).build();
      Protocol.Account reply = wallet.getBalance(req);
      responseObserver.onNext(reply);
    } else {
      responseObserver.onNext(null);
    }
    responseObserver.onCompleted();
  }

  @Override
  public void createTransaction(Contract.TransferContract req,
      StreamObserver<Protocol.Transaction> responseObserver) {
    ByteString fromBs = req.getOwnerAddress();
    ByteString toBs = req.getToAddress();
    long amount = req.getAmount();
    if (fromBs != null && toBs != null && amount > 0) {
      Protocol.Transaction trx = wallet.createTransaction(req);
      responseObserver.onNext(trx);
    } else {
      responseObserver.onNext(null);
    }
    responseObserver.onCompleted();
  }

  @Override
  public void broadcastTransaction(Protocol.Transaction req,
      StreamObserver<GrpcAPI.Return> responseObserver) {
    boolean ret = wallet.broadcastTransaction(req);
    GrpcAPI.Return retur = GrpcAPI.Return.newBuilder().setResult(ret).build();
    responseObserver.onNext(retur);
    responseObserver.onCompleted();
  }

  @Override
  public void createAccount(Contract.AccountCreateContract request,
      StreamObserver<Protocol.Transaction> responseObserver) {
    if (request.getType() == null || request.getAccountName() == null
        || request.getOwnerAddress() == null) {
      responseObserver.onNext(null);
    } else {
      Protocol.Transaction trx = wallet.createAccount(request);
      responseObserver.onNext(trx);
    }
    responseObserver.onCompleted();
  }


  @Override
  public void createAssetIssue(Contract.AssetIssueContract request,
      StreamObserver<Protocol.Transaction> responseObserver) {
    ByteString owner = request.getOwnerAddress();
    if (owner != null) {
      Protocol.Transaction trx = wallet.createTransaction(request);
      responseObserver.onNext(trx);
    } else {
      responseObserver.onNext(null);
    }
    responseObserver.onCompleted();
  }

  //refactor、test later
  private void checkVoteWitnessAccount(Contract.VoteWitnessContract req) {

    //send back to cli
    Preconditions.checkNotNull(req.getOwnerAddress(), "OwnerAddress is null");

    AccountCapsule account = this.accountStore.get(req.getOwnerAddress().toByteArray());

    Preconditions.checkNotNull(account, "OwnerAddress[" + req.getOwnerAddress() + "] not exists");

    Preconditions
        .checkArgument(req.getVotesCount() <= 0, "VotesCount[" + req.getVotesCount() + "] <= 0");

    Preconditions.checkArgument(account.getShare() < req.getVotesCount(),
        "Share[" + account.getShare() + "] <  VotesCount[" + req.getVotesCount() + "]");

    Iterator<Contract.VoteWitnessContract.Vote> iterator = req.getVotesList().iterator();
    while (iterator.hasNext()) {
      Contract.VoteWitnessContract.Vote vote = iterator.next();
      ByteString voteAddress = vote.getVoteAddress();
      WitnessCapsule witness = this.witnessStore.get(voteAddress.toByteArray());
      Preconditions.checkNotNull(witness, "witness[" + voteAddress + "] not exists");

      Preconditions.checkArgument(vote.getVoteCount() <= 0,
          "VoteAddress[" + voteAddress + "],VotesCount[" + vote.getVoteCount()
              + "] <= 0");
    }
  }

  @Override
  public void voteWitnessAccount(Contract.VoteWitnessContract req,
      StreamObserver<Protocol.Transaction> response) {

    try {
//        checkVoteWitnessAccount(req);//to be complemented later
      Protocol.Transaction trx = wallet.createTransaction(req);
      response.onNext(trx);
    } catch (Exception ex) {
      response.onNext(null);
    }
    response.onCompleted();
  }

  @Override
  public void createWitness(Contract.WitnessCreateContract req,
      StreamObserver<Protocol.Transaction> responseObserver) {
    ByteString fromBs = req.getOwnerAddress();

    if (fromBs != null) {
      Protocol.Transaction trx = wallet.createTransaction(req);
      responseObserver.onNext(trx);
    } else {
      responseObserver.onNext(null);
    }
    responseObserver.onCompleted();
  }


  @Override
  public void listAccounts(GrpcAPI.EmptyMessage request, StreamObserver<GrpcAPI.AccountList> responseObserver) {
    responseObserver.onNext(wallet.getAllAccounts());
    responseObserver.onCompleted();
  }

  @Override
  public void listWitnesses(GrpcAPI.EmptyMessage request, StreamObserver<GrpcAPI.WitnessList> responseObserver) {
    responseObserver.onNext(wallet.getWitnessList());
    responseObserver.onCompleted();
  }

  @Override
  public void listNodes(GrpcAPI.EmptyMessage request, StreamObserver<GrpcAPI.NodeList> responseObserver) {
    // TODO: this.app.getP2pNode().getActiveNodes();
    super.listNodes(request, responseObserver);
  }
}
