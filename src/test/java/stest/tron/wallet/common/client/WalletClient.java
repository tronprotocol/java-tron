package stest.tron.wallet.common.client;


import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountNetMessage;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.GrpcAPI.BlockList;
import org.tron.api.GrpcAPI.NodeList;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.TransactionList;
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.Hash;
import org.tron.common.crypto.SymmEncoder;
import org.tron.common.utils.Base58;
import org.tron.common.utils.ByteArray;
import stest.tron.wallet.common.client.utils.FileUtil;
import org.tron.common.utils.Utils;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.TransactionUtils;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import org.tron.protos.Contract;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Contract.FreezeBalanceContract;
import org.tron.protos.Contract.UnfreezeAssetContract;
import org.tron.protos.Contract.UnfreezeBalanceContract;
import org.tron.protos.Contract.WithdrawBalanceContract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Witness;
import sun.security.jca.JCAUtil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class AccountComparator implements Comparator {

    public int compare(Object o1, Object o2) {
        return Long.compare(((Account) o2).getBalance(), ((Account) o1).getBalance());
    }
}

class WitnessComparator implements Comparator {

    public int compare(Object o1, Object o2) {
        return Long.compare(((Witness) o2).getVoteCount(), ((Witness) o1).getVoteCount());
    }
}

public class WalletClient {

    private static final Logger logger = LoggerFactory.getLogger("WalletClient");
    private static final String FilePath = "Wallet";
    private ECKey ecKey = null;
    private boolean loginState = false;

    private static GrpcClient rpcCli ;
    private static String dbPath;
    private static String txtPath;

//  static {
//    new Timer().schedule(new TimerTask() {
//      @Override
//      public void run() {
//        String fullnode = selectFullNode();
//        if(!"".equals(fullnode)) {
//          rpcCli = new GrpcClient(fullnode);
//        }
//      }
//    }, 3 * 60 * 1000, 3 * 60 * 1000);
//  }


    public static boolean init( int itype ) {
        Config config = Configuration.getByPath("testng.conf");
        dbPath  = config.getString("CityDb.DbPath");
        txtPath = System.getProperty("user.dir") + '/' + config.getString("CityDb.TxtPath");

        String  fullNodepathname = "";

        if (1 == itype){
            fullNodepathname = "checkfullnode.ip.list";
        }
        else
        {
            fullNodepathname = "fullnode.ip.list";
        }

        String fullNode = "";
        String solidityNode = "";
        if(config.hasPath("soliditynode.ip.list")) {
            solidityNode = config.getStringList("soliditynode.ip.list").get(0);
        }
        if(config.hasPath(fullNodepathname)){
            fullNode = config.getStringList(fullNodepathname).get(0);
        }
        rpcCli   = new GrpcClient(fullNode, solidityNode);
        return true;
    }

    public static String selectFullNode() {
        Map<String, String> witnessMap = new HashMap<>();
        Config config = Configuration.getByPath("config.conf");
        List list = config.getObjectList("witnesses.witnessList");
        for (int i = 0; i < list.size(); i++) {
            ConfigObject obj = (ConfigObject) list.get(i);
            String ip = obj.get("ip").unwrapped().toString();
            String url = obj.get("url").unwrapped().toString();
            witnessMap.put(url, ip);
        }

        Optional<WitnessList> result = rpcCli.listWitnesses();
        long minMissedNum = 100000000L;
        String minMissedWitness = "";
        if (result.isPresent()) {
            List<Witness> witnessList = result.get().getWitnessesList();
            for (Witness witness : witnessList) {
                String url = witness.getUrl();
                long missedBlocks = witness.getTotalMissed();
                if (missedBlocks < minMissedNum) {
                    minMissedNum = missedBlocks;
                    minMissedWitness = url;
                }
            }
        }
        if (witnessMap.containsKey(minMissedWitness)) {
            return witnessMap.get(minMissedWitness);
        } else {
            return "";
        }
    }


    public static String getDbPath() {
        return dbPath;
    }

    public static String getTxtPath() {
        return txtPath;
    }

    /**
     * Creates a new WalletClient with a random ECKey or no ECKey.
     */
    public WalletClient(boolean genEcKey) {
        if (genEcKey) {
            this.ecKey = new ECKey(Utils.getRandom());
        }
    }

    //  Create Wallet with a pritKey
    public WalletClient(String priKey) {
        ECKey temKey = null;
        try {
            BigInteger priK = new BigInteger(priKey, 16);
            temKey = ECKey.fromPrivate(priK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        this.ecKey = temKey;
    }

    public boolean login(String password) {
        loginState = checkPassWord(password);
        return loginState;
    }

    public boolean isLoginState() {
        return loginState;
    }

    public void logout() {
        loginState = false;
    }

    /**
     * Get a Wallet from storage
     */
    public static WalletClient GetWalletByStorage(String password) {
        byte[] priKeyEnced = loadPriKey();
        if (ArrayUtils.isEmpty(priKeyEnced)) {
            return null;
        }
        //dec priKey
        byte[] salt0 = loadSalt0();
        byte[] aesKey = getEncKey(password, salt0);
        byte[] priKeyHexPlain = SymmEncoder.AES128EcbDec(priKeyEnced, aesKey);
        String priKeyPlain = Hex.toHexString(priKeyHexPlain);

        return new WalletClient(priKeyPlain);
    }

    /**
     * Creates a Wallet with an existing ECKey.
     */

    public WalletClient(final ECKey ecKey) {
        this.ecKey = ecKey;
    }

    public ECKey getEcKey() {
        return ecKey;
    }

    public byte[] getAddress() {
        return ecKey.getAddress();
    }

    public void store(String password) {
        /*
        if (ecKey == null || ecKey.getPrivKey() == null) {
            logger.warn("Warning: Store wallet failed, PrivKey is null !!");
            return;
        }
        byte[] salt0 = new byte[16];
        byte[] salt1 = new byte[16];
        JCAUtil.getSecureRandom().nextBytes(salt0);
        JCAUtil.getSecureRandom().nextBytes(salt1);
        byte[] aseKey = getEncKey(password, salt0);
        byte[] pwd = getPassWord(password, salt1);
        byte[] privKeyPlain = ecKey.getPrivKeyBytes();
        System.out.println("privKey:" + ByteArray.toHexString(privKeyPlain));
        //encrypted by password
        byte[] privKeyEnced = SymmEncoder.AES128EcbEnc(privKeyPlain, aseKey);
        byte[] pubKey = ecKey.getPubKey();
        byte[] walletData = new byte[pwd.length + pubKey.length + privKeyEnced.length + salt0.length
                + salt1.length];

        System.arraycopy(pwd, 0, walletData, 0, pwd.length);
        System.arraycopy(pubKey, 0, walletData, pwd.length, pubKey.length);
        System.arraycopy(privKeyEnced, 0, walletData, pwd.length + pubKey.length, privKeyEnced.length);
        System.arraycopy(salt0, 0, walletData, pwd.length + pubKey.length + privKeyEnced.length,
                salt0.length);
        System.arraycopy(salt1, 0, walletData,
                pwd.length + pubKey.length + privKeyEnced.length + salt0.length, salt1.length);

        FileUtil.saveData(FilePath, walletData,true);
        */
    }

    public Account queryAccount() {
        if (this.ecKey == null) {
            byte[] pubKey = loadPubKey();
            if (ArrayUtils.isEmpty(pubKey)) {
                logger.warn("Warning: QueryAccount failed, no wallet address !!");
                return null;
            }
            this.ecKey = ECKey.fromPublicOnly(pubKey);
        }
        return queryAccount(getAddress());
    }

    public static Account queryAccount(byte[] address) {
        return rpcCli.queryAccount(address);//call rpc
    }

    private Transaction signTransaction(Transaction transaction) {
        if (this.ecKey == null || this.ecKey.getPrivKey() == null) {
            logger.warn("Warning: Can't sign,there is no private key !!");
            return null;
        }
        transaction = TransactionUtils.setTimestamp(transaction);
        return TransactionUtils.sign(transaction, this.ecKey);
    }

    public boolean sendCoin(byte[] to, long amount) {
        byte[] owner = getAddress();
        Contract.TransferContract contract = createTransferContract(to, owner, amount);
        Transaction transaction = rpcCli.createTransaction(contract);
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            return false;
        }
        transaction = signTransaction(transaction);
        System.out.println("--------------------------------");
        System.out.println(
                "txid = " + ByteArray.toHexString(Hash.sha256(transaction.getRawData().toByteArray())));
        System.out.println("--------------------------------");
        return rpcCli.broadcastTransaction(transaction);
    }

    public boolean updateAccount(byte[] accountNameBytes) {
        byte[] owner = getAddress();
        Contract.AccountUpdateContract contract = createAccountUpdateContract(accountNameBytes, owner);
        Transaction transaction = rpcCli.createTransaction(contract);

        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            return false;
        }

        transaction = signTransaction(transaction);
        return rpcCli.broadcastTransaction(transaction);
    }

    public boolean updateAsset(byte[] description, byte[] url, long newLimit, long newPublicLimit) {
        byte[] owner = getAddress();
        Contract.UpdateAssetContract contract
                = createUpdateAssetContract(owner, description, url, newLimit, newPublicLimit);
        Transaction transaction = rpcCli.createTransaction(contract);

        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            return false;
        }

        transaction = signTransaction(transaction);
        return rpcCli.broadcastTransaction(transaction);
    }

    public boolean transferAsset(byte[] to, byte[] assertName, long amount) {
        byte[] owner = getAddress();
        Transaction transaction = createTransferAssetTransaction(to, assertName, owner, amount);
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            return false;
        }
        transaction = signTransaction(transaction);
        return rpcCli.broadcastTransaction(transaction);
    }

    public static Transaction createTransferAssetTransaction(byte[] to, byte[] assertName,
                                                             byte[] owner, long amount) {
        Contract.TransferAssetContract contract = createTransferAssetContract(to, assertName, owner,
                amount);
        return rpcCli.createTransferAssetTransaction(contract);
    }

    public boolean participateAssetIssue(byte[] to, byte[] assertName, long amount) {
        byte[] owner = getAddress();
        Transaction transaction = participateAssetIssueTransaction(to, assertName, owner, amount);
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            return false;
        }
        transaction = signTransaction(transaction);
        return rpcCli.broadcastTransaction(transaction);
    }

    public static Transaction participateAssetIssueTransaction(byte[] to, byte[] assertName,
                                                               byte[] owner, long amount) {
        Contract.ParticipateAssetIssueContract contract = participateAssetIssueContract(to, assertName,
                owner, amount);
        return rpcCli.createParticipateAssetIssueTransaction(contract);
    }

    public static Transaction updateAccountTransaction(byte[] addressBytes, byte[] accountNameBytes) {
        Contract.AccountUpdateContract contract = createAccountUpdateContract(accountNameBytes,
                addressBytes);
        return rpcCli.createTransaction(contract);
    }

    public static boolean broadcastTransaction(byte[] transactionBytes)
            throws InvalidProtocolBufferException {
        Transaction transaction = Transaction.parseFrom(transactionBytes);
        return TransactionUtils.validTransaction(transaction)
                && rpcCli.broadcastTransaction(transaction);
    }

    public boolean createAssetIssue(Contract.AssetIssueContract contract) {
        Transaction transaction = rpcCli.createAssetIssue(contract);
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            return false;
        }
        transaction = signTransaction(transaction);
        return rpcCli.broadcastTransaction(transaction);
    }

    public boolean createWitness(byte[] url) {
        byte[] owner = getAddress();
        Transaction transaction = createWitnessTransaction(owner, url);
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            return false;
        }
        transaction = signTransaction(transaction);
        return rpcCli.broadcastTransaction(transaction);
    }

    public boolean updateWitness(byte[] url) {
        byte[] owner = getAddress();
        Transaction transaction = updateWitnessTransaction(owner, url);
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            return false;
        }
        transaction = signTransaction(transaction);
        return rpcCli.broadcastTransaction(transaction);
    }

    public static Transaction createWitnessTransaction(byte[] owner, byte[] url) {
        Contract.WitnessCreateContract contract = createWitnessCreateContract(owner, url);
        return rpcCli.createWitness(contract);
    }

    public static Transaction updateWitnessTransaction(byte[] owner, byte[] url) {
        Contract.WitnessUpdateContract contract = createWitnessUpdateContract(owner, url);
        return rpcCli.updateWitness(contract);
    }

    public static Transaction createVoteWitnessTransaction(byte[] owner,
                                                           HashMap<String, String> witness) {
        Contract.VoteWitnessContract contract = createVoteWitnessContract(owner, witness);
        return rpcCli.voteWitnessAccount(contract);
    }

    public static Transaction createAssetIssueTransaction(Contract.AssetIssueContract contract) {
        return rpcCli.createAssetIssue(contract);
    }

    public static Block GetBlock(long blockNum) {
        return rpcCli.getBlock(blockNum);
    }

    public boolean voteWitness(HashMap<String, String> witness) {
        byte[] owner = getAddress();
        Contract.VoteWitnessContract contract = createVoteWitnessContract(owner, witness);
        Transaction transaction = rpcCli.voteWitnessAccount(contract);
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            return false;
        }
        transaction = signTransaction(transaction);
        return rpcCli.broadcastTransaction(transaction);
    }

    public static Contract.TransferContract createTransferContract(byte[] to, byte[] owner,
                                                                   long amount) {
        Contract.TransferContract.Builder builder = Contract.TransferContract.newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsOwner = ByteString.copyFrom(owner);
        builder.setToAddress(bsTo);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);

        return builder.build();
    }

    public static Contract.TransferAssetContract createTransferAssetContract(byte[] to,
                                                                             byte[] assertName, byte[] owner,
                                                                             long amount) {
        Contract.TransferAssetContract.Builder builder = Contract.TransferAssetContract.newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsName = ByteString.copyFrom(assertName);
        ByteString bsOwner = ByteString.copyFrom(owner);
        builder.setToAddress(bsTo);
        builder.setAssetName(bsName);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);

        return builder.build();
    }

    public static Contract.ParticipateAssetIssueContract participateAssetIssueContract(byte[] to,
                                                                                       byte[] assertName, byte[] owner,
                                                                                       long amount) {
        Contract.ParticipateAssetIssueContract.Builder builder = Contract.ParticipateAssetIssueContract
                .newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsName = ByteString.copyFrom(assertName);
        ByteString bsOwner = ByteString.copyFrom(owner);
        builder.setToAddress(bsTo);
        builder.setAssetName(bsName);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);

        return builder.build();
    }

    public static Transaction createTransaction4Transfer(Contract.TransferContract contract) {
        Transaction transaction = rpcCli.createTransaction(contract);
        return transaction;
    }

//  public static Contract.AccountCreateContract createAccountCreateContract(AccountType accountType,
//      byte[] accountName, byte[] address) {
//    Contract.AccountCreateContract.Builder builder = Contract.AccountCreateContract.newBuilder();
//    ByteString bsaAdress = ByteString.copyFrom(address);
//    ByteString bsAccountName = ByteString.copyFrom(accountName);
//    builder.setType(accountType);
//    builder.setAccountName(bsAccountName);
//    builder.setOwnerAddress(bsaAdress);
//
//    return builder.build();
//  }

    public static Contract.AccountUpdateContract createAccountUpdateContract(byte[] accountName,
                                                                             byte[] address) {
        Contract.AccountUpdateContract.Builder builder = Contract.AccountUpdateContract.newBuilder();
        ByteString basAddreess = ByteString.copyFrom(address);
        ByteString bsAccountName = ByteString.copyFrom(accountName);
        builder.setAccountName(bsAccountName);
        builder.setOwnerAddress(basAddreess);

        return builder.build();
    }

    public static Contract.UpdateAssetContract createUpdateAssetContract(
            byte[] address,
            byte[] description,
            byte[] url,
            long newLimit,
            long newPublicLimit
    ) {
        Contract.UpdateAssetContract.Builder builder =
                Contract.UpdateAssetContract.newBuilder();
        ByteString basAddreess = ByteString.copyFrom(address);
        builder.setDescription(ByteString.copyFrom(description));
        builder.setUrl(ByteString.copyFrom(url));
        builder.setNewLimit(newLimit);
        builder.setNewPublicLimit(newPublicLimit);
        builder.setOwnerAddress(basAddreess);

        return builder.build();
    }

    public static Contract.WitnessCreateContract createWitnessCreateContract(byte[] owner,
                                                                             byte[] url) {
        Contract.WitnessCreateContract.Builder builder = Contract.WitnessCreateContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setUrl(ByteString.copyFrom(url));

        return builder.build();
    }

    public static Contract.WitnessUpdateContract createWitnessUpdateContract(byte[] owner,
                                                                             byte[] url) {
        Contract.WitnessUpdateContract.Builder builder = Contract.WitnessUpdateContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setUpdateUrl(ByteString.copyFrom(url));

        return builder.build();
    }

    public static Contract.VoteWitnessContract createVoteWitnessContract(byte[] owner,
                                                                         HashMap<String, String> witness) {
        Contract.VoteWitnessContract.Builder builder = Contract.VoteWitnessContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        for (String addressBase58 : witness.keySet()) {
            String value = witness.get(addressBase58);
            long count = Long.parseLong(value);
            Contract.VoteWitnessContract.Vote.Builder voteBuilder = Contract.VoteWitnessContract.Vote
                    .newBuilder();
            byte[] address = WalletClient.decodeFromBase58Check(addressBase58);
            if (address == null) {
                continue;
            }
            voteBuilder.setVoteAddress(ByteString.copyFrom(address));
            voteBuilder.setVoteCount(count);
            builder.addVotes(voteBuilder.build());
        }

        return builder.build();
    }

    private static byte[] loadPassword() {
        byte[] buf = FileUtil.readData(FilePath);
        if (ArrayUtils.isEmpty(buf)) {
            return null;
        }
        if (buf.length != 145) {
            return null;
        }
        return Arrays.copyOfRange(buf, 0, 16);  //16
    }

    public static byte[] loadPubKey() {
        byte[] buf = FileUtil.readData(FilePath);
        if (ArrayUtils.isEmpty(buf)) {
            return null;
        }
        if (buf.length != 145) {
            return null;
        }
        return Arrays.copyOfRange(buf, 16, 81);  //65
    }

    private static byte[] loadPriKey() {
        byte[] buf = FileUtil.readData(FilePath);
        if (ArrayUtils.isEmpty(buf)) {
            return null;
        }
        if (buf.length != 145) {
            return null;
        }
        return Arrays.copyOfRange(buf, 81, 113);  //32
    }

    private static byte[] loadSalt0() {
        byte[] buf = FileUtil.readData(FilePath);
        if (ArrayUtils.isEmpty(buf)) {
            return null;
        }
        if (buf.length != 145) {
            return null;
        }
        return Arrays.copyOfRange(buf, 113, 129);  //16
    }

    private static byte[] loadSalt1() {
        byte[] buf = FileUtil.readData(FilePath);
        if (ArrayUtils.isEmpty(buf)) {
            return null;
        }
        if (buf.length != 145) {
            return null;
        }
        return Arrays.copyOfRange(buf, 129, 145);  //16
    }

    /**
     * Get a Wallet from storage
     */
    public static WalletClient GetWalletByStorageIgnorPrivKey() {
        try {
            byte[] pubKey = loadPubKey(); //04 PubKey
            ECKey eccKey = ECKey.fromPublicOnly(pubKey);
            return new WalletClient(eccKey);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static String getAddressByStorage() {
        try {
            byte[] pubKey = loadPubKey(); //04 PubKey
            return ByteArray.toHexString(ECKey.computeAddress(pubKey));
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static byte[] getPassWord(String password, byte[] salt1) {
        if (!passwordValid(password)) {
            return null;
        }
        byte[] pwd;
        byte[] msg = new byte[password.length() + salt1.length];
        System.arraycopy(password.getBytes(), 0, msg, 0, password.length());
        System.arraycopy(salt1, 0, msg, password.length(), salt1.length);
        pwd = Hash.sha256(msg);
        pwd = Hash.sha256(pwd);
        pwd = Arrays.copyOfRange(pwd, 0, 16);
        return pwd;
    }

    public static byte[] getEncKey(String password, byte[] salt0) {
        if (!passwordValid(password)) {
            return null;
        }
        byte[] encKey;
        byte[] msg = new byte[password.length() + salt0.length];
        System.arraycopy(password.getBytes(), 0, msg, 0, password.length());
        System.arraycopy(salt0, 0, msg, password.length(), salt0.length);
        encKey = Hash.sha256(msg);
        encKey = Arrays.copyOfRange(encKey, 0, 16);
        return encKey;
    }

    public static boolean checkPassWord(String password) {
        byte[] salt1 = loadSalt1();
        if (ArrayUtils.isEmpty(salt1)) {
            return false;
        }
        byte[] pwd = getPassWord(password, salt1);
        byte[] pwdStored = loadPassword();

        return Arrays.equals(pwd, pwdStored);
    }

    public static boolean passwordValid(String password) {
        if (StringUtils.isEmpty(password)) {
            logger.warn("Warning: Password is empty !!");
            return false;
        }
        if (password.length() < 6) {
            logger.warn("Warning: Password is too short !!");
            return false;
        }
        //Other rule;
        return true;
    }

    public static boolean addressValid(byte[] address) {
        if (address == null || address.length == 0) {
            logger.warn("Warning: Address is empty !!");
            return false;
        }
        if (address.length != CommonConstant.ADDRESS_SIZE) {
            logger.warn(
                    "Warning: Address length need " + CommonConstant.ADDRESS_SIZE + " but " + address.length
                            + " !!");
            return false;
        }
        byte preFixbyte = address[0];
        if (preFixbyte != CommonConstant.ADD_PRE_FIX_BYTE) {
            logger.warn("Warning: Address need prefix with " + CommonConstant.ADD_PRE_FIX_BYTE + " but "
                    + preFixbyte + " !!");
            return false;
        }
        //Other rule;
        return true;
    }

    public static String encode58Check(byte[] input) {
        byte[] hash0 = Hash.sha256(input);
        byte[] hash1 = Hash.sha256(hash0);
        byte[] inputCheck = new byte[input.length + 4];
        System.arraycopy(input, 0, inputCheck, 0, input.length);
        System.arraycopy(hash1, 0, inputCheck, input.length, 4);
        return Base58.encode(inputCheck);
    }

    private static byte[] decode58Check(String input) {
        byte[] decodeCheck = Base58.decode(input);
        if (decodeCheck.length <= 4) {
            return null;
        }
        byte[] decodeData = new byte[decodeCheck.length - 4];
        System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
        byte[] hash0 = Hash.sha256(decodeData);
        byte[] hash1 = Hash.sha256(hash0);
        if (hash1[0] == decodeCheck[decodeData.length] &&
                hash1[1] == decodeCheck[decodeData.length + 1] &&
                hash1[2] == decodeCheck[decodeData.length + 2] &&
                hash1[3] == decodeCheck[decodeData.length + 3]) {
            return decodeData;
        }
        return null;
    }

    public static byte[] decodeFromBase58Check(String addressBase58) {
        if (StringUtils.isEmpty(addressBase58)) {
            logger.warn("Warning: Address is empty !!");
            return null;
        }
        if (addressBase58.length() != CommonConstant.BASE58CHECK_ADDRESS_SIZE) {
            logger.warn("Warning: Base58 address length need " + CommonConstant.BASE58CHECK_ADDRESS_SIZE
                    + " but " + addressBase58.length() + " !!");
            return null;
        }
        byte[] address = decode58Check(addressBase58);
        if (!addressValid(address)) {
            return null;
        }
        return address;
    }

    public static boolean priKeyValid(String priKey) {
        if (StringUtils.isEmpty(priKey)) {
            logger.warn("Warning: PrivateKey is empty !!");
            return false;
        }
        if (priKey.length() != 64) {
            logger.warn("Warning: PrivateKey length need 64 but " + priKey.length() + " !!");
            return false;
        }
        //Other rule;
        return true;
    }

//  public static Optional<AccountList> listAccounts() {
//    Optional<AccountList> result = rpcCli.listAccounts();
//    if (result.isPresent()) {
//      AccountList accountList = result.get();
//      List<Account> list = accountList.getAccountsList();
//      List<Account> newList = new ArrayList();
//      newList.addAll(list);
//      newList.sort(new AccountComparator());
//      AccountList.Builder builder = AccountList.newBuilder();
//      newList.forEach(account -> builder.addAccounts(account));
//      result = Optional.of(builder.build());
//    }
//    return result;
//  }

    public static Optional<WitnessList> listWitnesses() {
        Optional<WitnessList> result = rpcCli.listWitnesses();
        if (result.isPresent()) {
            WitnessList witnessList = result.get();
            List<Witness> list = witnessList.getWitnessesList();
            List<Witness> newList = new ArrayList();
            newList.addAll(list);
            newList.sort(new WitnessComparator());
            WitnessList.Builder builder = WitnessList.newBuilder();
            newList.forEach(witness -> builder.addWitnesses(witness));
            result = Optional.of(builder.build());
        }
        return result;
    }

    public static Optional<AssetIssueList> getAssetIssueListByTimestamp(long timestamp) {
        return rpcCli.getAssetIssueListByTimestamp(timestamp);
    }

    public static Optional<TransactionList> getTransactionsByTimestamp(long start, long end, int offset, int limit) {
        return rpcCli.getTransactionsByTimestamp(start, end, offset, limit);
    }

    public static GrpcAPI.NumberMessage getTransactionsByTimestampCount(long start, long end) {
        return rpcCli.getTransactionsByTimestampCount(start,end);
    }

    public static Optional<AssetIssueList> getAssetIssueList() {
        return rpcCli.getAssetIssueList();
    }

    public static Optional<NodeList> listNodes() {
        return rpcCli.listNodes();
    }

    public static Optional<AssetIssueList> getAssetIssueByAccount(byte[] address) {
        return rpcCli.getAssetIssueByAccount(address);
    }

    public static AccountNetMessage getAccountNet(byte[] address) {
        return rpcCli.getAccountNet(address);
    }

    public static AssetIssueContract getAssetIssueByName(String assetName) {
        return rpcCli.getAssetIssueByName(assetName);
    }

    public static GrpcAPI.NumberMessage getTotalTransaction() {
        return rpcCli.getTotalTransaction();
    }

    public static GrpcAPI.NumberMessage getNextMaintenanceTime() {
        return rpcCli.getNextMaintenanceTime();
    }

    public static Optional<TransactionList> getTransactionsFromThis(byte[] address, int offset, int limit) {
        return rpcCli.getTransactionsFromThis(address, offset, limit);
    }

    public static GrpcAPI.NumberMessage   getTransactionsFromThisCount(byte[] address) {
        return rpcCli.getTransactionsFromThisCount(address);
    }

    public static Optional<TransactionList> getTransactionsToThis(byte[] address, int offset, int limit) {
        return rpcCli.getTransactionsToThis(address, offset, limit);
    }

    public static GrpcAPI.NumberMessage getTransactionsToThisCount(byte[] address) {
        return rpcCli.getTransactionsToThisCount(address);
    }

    public static Optional<Transaction> getTransactionById(String txID) {
        return rpcCli.getTransactionById(txID);
    }

    public boolean freezeBalance(long frozen_balance, long frozen_duration) {
        Contract.FreezeBalanceContract contract = createFreezeBalanceContract(frozen_balance,
                frozen_duration);
        Transaction transaction = rpcCli.createTransaction(contract);
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            return false;
        }
        transaction = signTransaction(transaction);
        return rpcCli.broadcastTransaction(transaction);
    }

    private FreezeBalanceContract createFreezeBalanceContract(long frozen_balance,
                                                              long frozen_duration) {
        byte[] address = getAddress();
        Contract.FreezeBalanceContract.Builder builder = Contract.FreezeBalanceContract.newBuilder();
        ByteString byteAddress = ByteString.copyFrom(address);
        builder.setOwnerAddress(byteAddress).setFrozenBalance(frozen_balance)
                .setFrozenDuration(frozen_duration);

        return builder.build();
    }

    public boolean unfreezeBalance() {
        Contract.UnfreezeBalanceContract contract = createUnfreezeBalanceContract();
        Transaction transaction = rpcCli.createTransaction(contract);
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            return false;
        }
        transaction = signTransaction(transaction);
        return rpcCli.broadcastTransaction(transaction);
    }

    private UnfreezeBalanceContract createUnfreezeBalanceContract() {
        byte[] address = getAddress();
        Contract.UnfreezeBalanceContract.Builder builder = Contract.UnfreezeBalanceContract
                .newBuilder();
        ByteString byteAddreess = ByteString.copyFrom(address);
        builder.setOwnerAddress(byteAddreess);

        return builder.build();
    }

    public boolean unfreezeAsset() {
        Contract.UnfreezeAssetContract contract = createUnfreezeAssetContract();
        Transaction transaction = rpcCli.createTransaction(contract);
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            return false;
        }
        transaction = signTransaction(transaction);
        return rpcCli.broadcastTransaction(transaction);
    }

    private UnfreezeAssetContract createUnfreezeAssetContract() {
        byte[] address = getAddress();
        Contract.UnfreezeAssetContract.Builder builder = Contract.UnfreezeAssetContract
                .newBuilder();
        ByteString byteAddreess = ByteString.copyFrom(address);
        builder.setOwnerAddress(byteAddreess);
        return builder.build();
    }

    public boolean withdrawBalance() {
        Contract.WithdrawBalanceContract contract = createWithdrawBalanceContract();
        Transaction transaction = rpcCli.createTransaction(contract);
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            return false;
        }
        transaction = signTransaction(transaction);
        return rpcCli.broadcastTransaction(transaction);
    }

    private WithdrawBalanceContract createWithdrawBalanceContract() {
        byte[] address = getAddress();
        Contract.WithdrawBalanceContract.Builder builder = Contract.WithdrawBalanceContract
                .newBuilder();
        ByteString byteAddreess = ByteString.copyFrom(address);
        builder.setOwnerAddress(byteAddreess);

        return builder.build();
    }

    public static Optional<Block> getBlockById(String blockID) {
        return rpcCli.getBlockById(blockID);
    }

    public static Optional<BlockList> getBlockByLimitNext(long start, long end) {
        return rpcCli.getBlockByLimitNext(start, end);
    }

    public static Optional<BlockList> getBlockByLatestNum(long num) {
        return rpcCli.getBlockByLatestNum(num);
    }
}
