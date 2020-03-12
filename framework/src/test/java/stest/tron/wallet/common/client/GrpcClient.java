package stest.tron.wallet.common.client;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountNetMessage;
import org.tron.api.GrpcAPI.AccountPaginated;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.GrpcAPI.BlockLimit;
import org.tron.api.GrpcAPI.BlockList;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.NodeList;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.PaginatedMessage;
import org.tron.api.GrpcAPI.TransactionList;
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.api.WalletExtensionGrpc;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.AccountContract.AccountCreateContract;
import org.tron.protos.contract.AccountContract.AccountUpdateContract;
import org.tron.protos.contract.AssetIssueContractOuterClass;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.BalanceContract.FreezeBalanceContract;
import org.tron.protos.contract.BalanceContract.UnfreezeBalanceContract;
import org.tron.protos.contract.BalanceContract.WithdrawBalanceContract;
import org.tron.protos.contract.WitnessContract;


public class GrpcClient {

  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private WalletExtensionGrpc.WalletExtensionBlockingStub blockingStubExtension = null;

  //  public GrpcClient(String host, int port) {
  //    channel = ManagedChannelBuilder.forAddress(host, port)
  //        .usePlaintext(true)
  //        .build();
  //    blockingStub = WalletGrpc.newBlockingStub(channel);
  //  }

  /**
   * constructor.
   */

  public GrpcClient(String fullnode, String soliditynode) {
    if (!(fullnode.isEmpty())) {
      channelFull = ManagedChannelBuilder.forTarget(fullnode)
          .usePlaintext(true)
          .build();
      blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    }
    if (!(soliditynode.isEmpty())) {
      channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
          .usePlaintext(true)
          .build();
      blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
      blockingStubExtension = WalletExtensionGrpc.newBlockingStub(channelSolidity);
    }
  }

  /**
   * constructor.
   */

  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  /**
   * constructor.
   */

  public Account queryAccount(byte[] address) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    if (blockingStubSolidity != null) {
      return blockingStubSolidity.getAccount(request);
    } else {
      return blockingStubFull.getAccount(request);
    }
  }

  public Transaction createTransaction(AccountUpdateContract contract) {
    return blockingStubFull.updateAccount(contract);
  }

  public Transaction createTransaction(BalanceContract.TransferContract contract) {
    return blockingStubFull.createTransaction(contract);
  }

  public Transaction createTransaction(FreezeBalanceContract contract) {
    return blockingStubFull.freezeBalance(contract);
  }

  public Transaction createTransaction(WithdrawBalanceContract contract) {
    return blockingStubFull.withdrawBalance(contract);
  }

  public Transaction createTransaction(UnfreezeBalanceContract contract) {
    return blockingStubFull.unfreezeBalance(contract);
  }

  public Transaction createTransferAssetTransaction(
      AssetIssueContractOuterClass.TransferAssetContract contract) {
    return blockingStubFull.transferAsset(contract);
  }

  public Transaction createParticipateAssetIssueTransaction(
      AssetIssueContractOuterClass.ParticipateAssetIssueContract contract) {
    return blockingStubFull.participateAssetIssue(contract);
  }

  public Transaction createAccount(AccountCreateContract contract) {
    return blockingStubFull.createAccount(contract);
  }

  public Transaction createAssetIssue(AssetIssueContract contract) {
    return blockingStubFull.createAssetIssue(contract);
  }

  public Transaction voteWitnessAccount(WitnessContract.VoteWitnessContract contract) {
    return blockingStubFull.voteWitnessAccount(contract);
  }

  public Transaction createWitness(WitnessContract.WitnessCreateContract contract) {
    return blockingStubFull.createWitness(contract);
  }

  public boolean broadcastTransaction(Transaction signaturedTransaction) {
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(signaturedTransaction);
    return response.getResult();
  }

  /**
   * constructor.
   */

  public AccountNetMessage getAccountNet(byte[] address) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccountNet(request);
  }

  /**
   * constructor.
   */

  public Block getBlock(long blockNum) {
    if (blockNum < 0) {
      if (blockingStubSolidity != null) {
        return blockingStubSolidity.getNowBlock(EmptyMessage.newBuilder().build());
      } else {
        return blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build());
      }
    }
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    if (blockingStubSolidity != null) {
      return blockingStubSolidity.getBlockByNum(builder.build());
    } else {
      return blockingStubFull.getBlockByNum(builder.build());
    }
  }

  /*    public Optional<AccountList> listAccounts() {
        if(blockingStubSolidity != null) {
            AccountList accountList = blockingStubSolidity.listAccounts(
            EmptyMessage.newBuilder().build());
            return Optional.ofNullable(accountList);
        }else{
            AccountList accountList = blockingStubFull.listAccounts(
            EmptyMessage.newBuilder().build());
            return Optional.ofNullable(accountList);
        }
    }*/

  /**
   * constructor.
   */
  public Optional<WitnessList> listWitnesses() {
    if (blockingStubSolidity != null) {
      WitnessList witnessList = blockingStubSolidity.listWitnesses(
          EmptyMessage.newBuilder().build());
      return Optional.ofNullable(witnessList);
    } else {
      WitnessList witnessList = blockingStubFull.listWitnesses(
          EmptyMessage.newBuilder().build());
      return Optional.ofNullable(witnessList);
    }
  }

  /**
   * constructor.
   */

  public Optional<AssetIssueList> getAssetIssueList(long offset, long limit) {
    PaginatedMessage.Builder pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);
    if (blockingStubSolidity != null) {
      AssetIssueList assetIssueList = blockingStubSolidity
          .getPaginatedAssetIssueList(pageMessageBuilder.build());
      return Optional.ofNullable(assetIssueList);
    } else {
      AssetIssueList assetIssueList = blockingStubFull
          .getPaginatedAssetIssueList(pageMessageBuilder.build());
      return Optional.ofNullable(assetIssueList);
    }
  }


  /**
   * constructor.
   */

  public Optional<AssetIssueList> getAssetIssueList() {
    if (blockingStubSolidity != null) {
      AssetIssueList assetIssueList = blockingStubSolidity
          .getAssetIssueList(EmptyMessage.newBuilder().build());
      return Optional.ofNullable(assetIssueList);
    } else {
      AssetIssueList assetIssueList = blockingStubFull
          .getAssetIssueList(EmptyMessage.newBuilder().build());
      return Optional.ofNullable(assetIssueList);
    }
  }

  /**
   * constructor.
   */

  public Optional<NodeList> listNodes() {
    NodeList nodeList = blockingStubFull
        .listNodes(EmptyMessage.newBuilder().build());
    return Optional.ofNullable(nodeList);
  }

  /*  public Optional<AssetIssueList> getAssetIssueByAccount(byte[] address) {
      ByteString addressBs = ByteString.copyFrom(address);
      Account request = Account.newBuilder().setAddress(addressBs).build();
      if(blockingStubSolidity != null) {
          AssetIssueList assetIssueList = blockingStubSolidity
                  .getAssetIssueByAccount(request);
          return Optional.ofNullable(assetIssueList);
      } else {
          AssetIssueList assetIssueList = blockingStubFull
                  .getAssetIssueByAccount(request);
          return Optional.ofNullable(assetIssueList);
      }
  }*/
  /*  public AssetIssueContract getAssetIssueByName(String assetName) {
      ByteString assetNameBs = ByteString.copyFrom(assetName.getBytes());
      BytesMessage request = BytesMessage.newBuilder().setValue(assetNameBs).build();
      if(blockingStubSolidity != null) {
          return blockingStubSolidity.getAssetIssueByName(request);
      } else {
          return blockingStubFull.getAssetIssueByName(request);
      }
   }*/

  /*  public NumberMessage getTotalTransaction() {
      if(blockingStubSolidity != null) {
          return blockingStubSolidity.totalTransaction(EmptyMessage.newBuilder().build());
      } else {
          return blockingStubFull.totalTransaction(EmptyMessage.newBuilder().build());
      }
   }*/

  /*    public Optional<AssetIssueList> getAssetIssueListByTimestamp(long time) {
        NumberMessage.Builder timeStamp = NumberMessage.newBuilder();
        timeStamp.setNum(time);
        AssetIssueList assetIssueList = blockingStubSolidity
        .getAssetIssueListByTimestamp(timeStamp.build());
        return Optional.ofNullable(assetIssueList);
    }*/
  /*    public Optional<TransactionList> getTransactionsByTimestamp(
        long start, long end, int offset , int limit) {
        TimeMessage.Builder timeMessage = TimeMessage.newBuilder();
        timeMessage.setBeginInMilliseconds(start);
        timeMessage.setEndInMilliseconds(end);
        TimePaginatedMessage.Builder timePageMessage = TimePaginatedMessage.newBuilder();
        timePageMessage.setTimeMessage(timeMessage);
        timePageMessage.setOffset(offset);
        timePageMessage.setLimit(limit);
        TransactionList transactionList = blockingStubExtension
        .getTransactionsByTimestamp(timePageMessage.build());
        return Optional.ofNullable(transactionList);
    }*/

  /**
   * constructor.
   */

  public Optional<TransactionList> getTransactionsFromThis(byte[] address) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account account = Account.newBuilder().setAddress(addressBs).build();
    AccountPaginated.Builder builder = AccountPaginated.newBuilder().setAccount(account);
    builder.setLimit(1000);
    builder.setOffset(0);
    TransactionList transactionList = blockingStubExtension
        .getTransactionsFromThis(builder.build());
    return Optional.ofNullable(transactionList);
  }

  /**
   * constructor.
   */

  public Optional<TransactionList> getTransactionsToThis(byte[] address) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account account = Account.newBuilder().setAddress(addressBs).build();
    AccountPaginated.Builder builder = AccountPaginated.newBuilder().setAccount(account);
    builder.setLimit(1000);
    builder.setOffset(0);
    TransactionList transactionList = blockingStubExtension.getTransactionsToThis(builder.build());
    return Optional.ofNullable(transactionList);
  }

  /*    public Optional<Transaction> getTransactionById(String txID){
        ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txID));
        BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
        if(blockingStubSolidity != null) {
            Transaction transaction = blockingStubSolidity.getTransactionById(request);
            return Optional.ofNullable(transaction);
        } else {
            Transaction transaction = blockingStubFull.getTransactionById(request);
            return Optional.ofNullable(transaction);
        }
   }*/


  /**
   * constructor.
   */

  public Optional<Block> getBlockById(String blockId) {
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(blockId));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    Block block = blockingStubFull.getBlockById(request);
    return Optional.ofNullable(block);
  }

  /**
   * constructor.
   */

  public Optional<BlockList> getBlockByLimitNext(long start, long end) {
    BlockLimit.Builder builder = BlockLimit.newBuilder();
    builder.setStartNum(start);
    builder.setEndNum(end);
    BlockList blockList = blockingStubFull.getBlockByLimitNext(builder.build());
    return Optional.ofNullable(blockList);
  }

  /**
   * constructor.
   */

  public Optional<BlockList> getBlockByLatestNum(long num) {
    NumberMessage numberMessage = NumberMessage.newBuilder().setNum(num).build();
    BlockList blockList = blockingStubFull.getBlockByLatestNum(numberMessage);
    return Optional.ofNullable(blockList);
  }
}
