package stest.tron.wallet.common;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountList;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.NodeList;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.api.WalletGrpc;
import org.tron.protos.Contract;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;

public class GrpcClient {

    private final ManagedChannel channel;
    private final WalletGrpc.WalletBlockingStub blockingStub;

    public GrpcClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build();
        blockingStub = WalletGrpc.newBlockingStub(channel);
    }

    public GrpcClient(String host) {
        channel = ManagedChannelBuilder.forTarget(host)
                .usePlaintext(true)
                .build();
        blockingStub = WalletGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public Account queryAccount(byte[] address) {
        ByteString addressBS = ByteString.copyFrom(address);
        Account request = Account.newBuilder().setAddress(addressBS).build();
        return blockingStub.getAccount(request);
    }

    public Transaction createTransaction(Contract.TransferContract contract) {
        return blockingStub.createTransaction(contract);
    }

    public Transaction createTransferAssetTransaction(Contract.TransferAssetContract contract) {
        return blockingStub.transferAsset(contract);
    }

    public Transaction createParticipateAssetIssueTransaction(
            Contract.ParticipateAssetIssueContract contract) {
        return blockingStub.participateAssetIssue(contract);
    }

    public Transaction createAssetIssue(Contract.AssetIssueContract contract) {
        return blockingStub.createAssetIssue(contract);
    }

    public Transaction voteWitnessAccount(Contract.VoteWitnessContract contract) {
        return blockingStub.voteWitnessAccount(contract);
    }

    public Transaction createWitness(Contract.WitnessCreateContract contract) {
        return blockingStub.createWitness(contract);
    }

    public boolean broadcastTransaction(Transaction signaturedTransaction) {
        GrpcAPI.Return response = blockingStub.broadcastTransaction(signaturedTransaction);
        return response.getResult();
    }

    public Block getBlock(long blockNum) {
        if (blockNum < 0) {
            return blockingStub.getNowBlock(EmptyMessage.newBuilder().build());
        }
        NumberMessage.Builder builder = NumberMessage.newBuilder();
        builder.setNum(blockNum);
        return blockingStub.getBlockByNum(builder.build());
    }

    public Optional<AccountList> listAccounts() {
        AccountList accountList = blockingStub.listAccounts(EmptyMessage.newBuilder().build());
        if (accountList != null) {
            return Optional.of(accountList);
        }
        return Optional.empty();
    }

    public Optional<WitnessList> listWitnesses() {
        WitnessList witnessList = blockingStub.listWitnesses(EmptyMessage.newBuilder().build());
        if (witnessList != null) {
            return Optional.of(witnessList);
        }
        return Optional.empty();
    }

    public Optional<AssetIssueList> getAssetIssueList() {
        AssetIssueList assetIssueList = blockingStub
                .getAssetIssueList(EmptyMessage.newBuilder().build());
        if (assetIssueList != null) {
            return Optional.of(assetIssueList);
        }
        return Optional.empty();
    }

    public Optional<NodeList> listNodes() {
        NodeList nodeList = blockingStub
                .listNodes(EmptyMessage.newBuilder().build());
        if (nodeList != null) {
            return Optional.of(nodeList);
        }
        return Optional.empty();
    }

    public Optional<AssetIssueList> getAssetIssueByAccount(byte[] address) {
        ByteString addressBS = ByteString.copyFrom(address);
        Account request = Account.newBuilder().setAddress(addressBS).build();
        AssetIssueList assetIssueList = blockingStub
                .getAssetIssueByAccount(request);
        if (assetIssueList != null) {
            return Optional.of(assetIssueList);
        }
        return Optional.empty();
    }

    public AssetIssueContract getAssetIssueByName(String assetName) {
        ByteString assetNameBs = ByteString.copyFrom(assetName.getBytes());
        BytesMessage request = BytesMessage.newBuilder().setValue(assetNameBs).build();
        return blockingStub.getAssetIssueByName(request);
    }

    public NumberMessage getTotalTransaction(){
        return blockingStub.totalTransaction(EmptyMessage.newBuilder().build());
    }
}