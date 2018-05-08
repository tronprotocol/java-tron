package stest.tron.wallet.common;


import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.typesafe.config.Config;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.tron.api.GrpcAPI.AccountList;
import org.tron.api.GrpcAPI.NodeList;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Account;
import org.testng.Assert;
import stest.tron.wallet.common.TransactionUtils;


@Slf4j
public class WalletClient {

    private GrpcClient rpcCli;
    private ECKey ecKey;
    private String TARGET_GRPC_ADDRESS = "grpc.address";
    private String CHECK_GRPC_ADDRESS  = "grpc.checkaddress";

    private Config config = Configuration.getByPath("testng.conf");

    public WalletClient() {

    }

    public WalletClient(String priKey) {
        ECKey temKey = null;
        try {
            temKey = ECKey.fromPrivate(ByteArray.fromHexString(priKey));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        this.ecKey = temKey;
    }

    public void init(int iType ) {
        String grpcaddress = "";

        if(0 == iType)
        {
            grpcaddress = TARGET_GRPC_ADDRESS;
        }
        else if(1 == iType)
        {
            grpcaddress = CHECK_GRPC_ADDRESS;
        }
        else
        {
            logger.error("have no ip:port to init");
            return;
        }

        if (!config.hasPath("net")) {
            logger.error("no target: {} = ip:host", grpcaddress);
            return;
        }
        String target = config.getString(grpcaddress);
        logger.info("target: {}" + target);
        rpcCli = new GrpcClient(target);
    }

    public Optional<AccountList> listAccounts() {
        return rpcCli.listAccounts();
    }

    public Optional<NodeList> listNodes() {
        return rpcCli.listNodes();
    }

    public Account  getAccount(byte[] address) { return rpcCli.queryAccount( address);}

    public boolean sendCoin(byte[] to, long amount) {

        byte[] owner = getAddress();
        logger.info("sendCoin");

        Contract.TransferContract contract = createTransferContract(to, owner, amount);
        Transaction transaction = createTransaction(contract);
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            logger.info("sendCoin fail ");
            Assert.assertTrue(false);
            return false;
        }

        transaction = signTransaction(transaction);
        return rpcCli.broadcastTransaction(transaction);
    }

    public boolean freezeCoin(long amount){

        byte[] owner = getAddress();

        Contract.FreezeBalanceContract contract = createFreezeBalanceContract(owner,amount);
        rpcCli.freezeBalance(contract);

        return true;
    }

    public static Contract.FreezeBalanceContract createFreezeBalanceContract(byte[] owner,long amount){
        Contract.FreezeBalanceContract.Builder builder = Contract.FreezeBalanceContract.newBuilder();
        ByteString bsOwner = ByteString.copyFrom(owner);
        builder.setOwnerAddress(bsOwner);
        builder.setFrozenBalance(amount);

        return builder.build();
    }

    public static Contract.TransferContract createTransferContract(byte[] to, byte[] owner,
                                                                   long amount) {
        Contract.TransferContract.Builder builder = Contract.TransferContract.newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsOwner = ByteString.copyFrom(owner);
        builder.setToAddress(bsTo);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);

        logger.info("createTransferContract");

        return builder.build();
    }

    private Transaction signTransaction(Transaction transaction) {
        if (this.ecKey == null || this.ecKey.getPrivKey() == null) {
            logger.info("signTransaction fail");
            Assert.assertTrue(false);
            return null;
        }

        logger.info("signTransaction");

        transaction = TransactionUtils.setTimestamp(transaction);
        return TransactionUtils.sign(transaction, this.ecKey);
    }

    public byte[] getAddress() {
        return ecKey.getAddress();
    }

    public static Transaction createTransaction(TransferContract contract) {
        logger.info("createTransaction");

        Transaction.Builder transactionBuilder = Transaction.newBuilder();
        Transaction.Contract.Builder contractBuilder = Transaction.Contract.newBuilder();
        try {
            Any anyTo = Any.pack(contract);
            contractBuilder.setParameter(anyTo);
        } catch (Exception e) {
            logger.info("createTransaction fail");
            Assert.assertTrue(false);
            return null;
        }
        contractBuilder.setType(Transaction.Contract.ContractType.TransferContract);
        transactionBuilder.getRawDataBuilder().addContract(contractBuilder);
        //transactionBuilder.getRawDataBuilder()..(Transaction..ContractType);
        Transaction transaction = transactionBuilder.build();

        return transaction;
    }
}