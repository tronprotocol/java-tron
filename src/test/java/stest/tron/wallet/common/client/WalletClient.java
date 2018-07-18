package stest.tron.wallet.common.client;


import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI.AccountNetMessage;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.GrpcAPI.BlockList;
import org.tron.api.GrpcAPI.NodeList;
import org.tron.api.GrpcAPI.TransactionList;
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.SymmEncoder;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.Utils;
import org.tron.core.exception.CancelException;
import org.tron.keystore.CipherException;
import org.tron.protos.Contract;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Contract.FreezeBalanceContract;
import org.tron.protos.Contract.UnfreezeBalanceContract;
import org.tron.protos.Contract.WithdrawBalanceContract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Witness;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.TransactionUtils;

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

    private static byte addressPreFixByte = CommonConstant.ADD_PRE_FIX_BYTE_MAINNET;

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

        if (1000 == itype){
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
            fullNode = config.getStringList(fullNodepathname).get(itype);
        }
        if (config.hasPath("net.type") && "mainnet".equalsIgnoreCase(config.getString("net.type"))) {
            WalletClient.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
        } else {
            WalletClient.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_TESTNET);
        }
        rpcCli   = new GrpcClient(fullNode, solidityNode);
        return true;
    }

    public static GrpcClient init() {
        //Config config = org.tron.core.config.Configuration.getByPath("config.conf");
        Config config = Configuration.getByPath("testng.conf");
        dbPath = config.getString("CityDb.DbPath");
        txtPath = System.getProperty("user.dir") + "/" + config.getString("CityDb.TxtPath");

        String fullNode = "";
        String solidityNode = "";
        if (config.hasPath("soliditynode.ip.list")) {
            solidityNode = config.getStringList("soliditynode.ip.list").get(0);
        }
        if (config.hasPath("fullnode.ip.list")) {
            fullNode = config.getStringList("fullnode.ip.list").get(0);
        }
        if (config.hasPath("net.type") && "mainnet".equalsIgnoreCase(config.getString("net.type"))) {
            WalletClient.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
        } else {
            WalletClient.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_TESTNET);
        }
        return new GrpcClient(fullNode, solidityNode);
    }

    public static byte getAddressPreFixByte() {
        return addressPreFixByte;
    }

    public static void setAddressPreFixByte(byte addressPreFixByte) {
        WalletClient.addressPreFixByte = addressPreFixByte;
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
        String priKeyEnced = loadPriKey();
        if (priKeyEnced == null) {
            return null;
        }
        //dec priKey
        byte[] priKeyAscEnced = priKeyEnced.getBytes();
        byte[] priKeyHexEnced = Hex.decode(priKeyAscEnced);
        byte[] aesKey = getEncKey(password);
        byte[] priKeyHexPlain = SymmEncoder.AES128EcbDec(priKeyHexEnced, aesKey);
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
        if (ecKey == null || ecKey.getPrivKey() == null) {
            logger.warn("Warning: Store wallet failed, PrivKey is null !!");
            return;
        }
        byte[] pwd = getPassWord(password);
        String pwdAsc = ByteArray.toHexString(pwd);
        byte[] privKeyPlain = ecKey.getPrivKeyBytes();
        System.out.println("privKey:" + ByteArray.toHexString(privKeyPlain));
        //encrypted by password
        byte[] aseKey = getEncKey(password);
        byte[] privKeyEnced = SymmEncoder.AES128EcbEnc(privKeyPlain, aseKey);
        String privKeyStr = ByteArray.toHexString(privKeyEnced);
        byte[] pubKeyBytes = ecKey.getPubKey();
        String pubKeyStr = ByteArray.toHexString(pubKeyBytes);
        // SAVE PASSWORD
        FileUtil.saveData(FilePath, pwdAsc, false);//ofset:0 len:32
        // SAVE PUBKEY
        FileUtil.saveData(FilePath, pubKeyStr, true);//ofset:32 len:130
        // SAVE PRIKEY
        FileUtil.saveData(FilePath, privKeyStr, true);
    }

    public Account queryAccount() {
        byte[] address;
        if (this.ecKey == null) {
            String pubKey = loadPubKey(); //04 PubKey[128]
            if (StringUtils.isEmpty(pubKey)) {
                logger.warn("Warning: QueryAccount failed, no wallet address !!");
                return null;
            }
            byte[] pubKeyAsc = pubKey.getBytes();
            byte[] pubKeyHex = Hex.decode(pubKeyAsc);
            this.ecKey = ECKey.fromPublicOnly(pubKeyHex);
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
        return rpcCli.broadcastTransaction(transaction);
    }

    public boolean updateAccount(byte[] addressBytes, byte[] accountNameBytes) {
        Contract.AccountUpdateContract contract = createAccountUpdateContract(accountNameBytes,
                addressBytes);
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
        if (false == TransactionUtils.validTransaction(transaction)) {
            return false;
        }
        return rpcCli.broadcastTransaction(transaction);
    }

    public boolean createAssetIssue(AssetIssueContract contract) {
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

    public static Transaction createWitnessTransaction(byte[] owner, byte[] url) {
        Contract.WitnessCreateContract contract = createWitnessCreateContract(owner, url);
        return rpcCli.createWitness(contract);
    }


    public static Transaction createVoteWitnessTransaction(byte[] owner,
                                                           HashMap<String, String> witness) {
        Contract.VoteWitnessContract contract = createVoteWitnessContract(owner, witness);
        return rpcCli.voteWitnessAccount(contract);
    }

    public static Transaction createAssetIssueTransaction(AssetIssueContract contract) {
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

    public static Contract.AccountCreateContract createAccountCreateContract(AccountType accountType,
                                                                             byte[] accountName, byte[] address) {
        Contract.AccountCreateContract.Builder builder = Contract.AccountCreateContract.newBuilder();
        ByteString bsaAdress = ByteString.copyFrom(address);
        ByteString bsAccountName = ByteString.copyFrom(accountName);
        builder.setType(accountType);
        builder.setAccountAddress(bsAccountName);
        builder.setOwnerAddress(bsaAdress);

        return builder.build();
    }

    public boolean createAccount(byte[] address)
        throws CipherException, IOException, CancelException {
        byte[] owner = getAddress();
        Transaction transaction = createAccountTransaction(owner, address);
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            return false;
        }

        transaction = signTransaction(transaction);
        return rpcCli.broadcastTransaction(transaction);
    }

    public static Transaction createAccountTransaction(byte[] owner, byte[] address) {
        Contract.AccountCreateContract contract = createAccountCreateContract(owner, address);
        return rpcCli.createAccount(contract);
    }

    public static Contract.AccountCreateContract createAccountCreateContract(byte[] owner,
        byte[] address) {
        Contract.AccountCreateContract.Builder builder = Contract.AccountCreateContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setAccountAddress(ByteString.copyFrom(address));

        return builder.build();
    }

    public static Contract.AccountUpdateContract createAccountUpdateContract(byte[] accountName,
                                                                             byte[] address) {
        Contract.AccountUpdateContract.Builder builder = Contract.AccountUpdateContract.newBuilder();
        ByteString basAddreess = ByteString.copyFrom(address);
        ByteString bsAccountName = ByteString.copyFrom(accountName);

        builder.setAccountName(bsAccountName);
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

    public static AccountNetMessage getAccountNet(byte[] address) {
        return rpcCli.getAccountNet(address);
    }

    private static String loadPassword() {
        char[] buf = new char[0x100];
        int len = FileUtil.readData(FilePath, buf);
        if (len != 226) {
            return null;
        }
        return String.valueOf(buf, 0, 32);
    }

    public static String loadPubKey() {
        char[] buf = new char[0x100];
        int len = FileUtil.readData(FilePath, buf);
        if (len != 226) {
            return null;
        }
        return String.valueOf(buf, 32, 130);
    }

    private static String loadPriKey() {
        char[] buf = new char[0x100];
        int len = FileUtil.readData(FilePath, buf);
        if (len != 226) {
            return null;
        }
        return String.valueOf(buf, 162, 64);
    }

    /**
     * Get a Wallet from storage
     */
    public static WalletClient GetWalletByStorageIgnorPrivKey() {
        try {
            String pubKey = loadPubKey(); //04 PubKey[128]
            if (StringUtils.isEmpty(pubKey)) {
                return null;
            }
            byte[] pubKeyAsc = pubKey.getBytes();
            byte[] pubKeyHex = Hex.decode(pubKeyAsc);
            ECKey eccKey = ECKey.fromPublicOnly(pubKeyHex);
            return new WalletClient(eccKey);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static String getAddressByStorage() {
        try {
            String pubKey = loadPubKey(); //04 PubKey[128]
            if (StringUtils.isEmpty(pubKey)) {
                return null;
            }
            byte[] pubKeyAsc = pubKey.getBytes();
            byte[] pubKeyHex = Hex.decode(pubKeyAsc);
            ECKey eccKey = ECKey.fromPublicOnly(pubKeyHex);
            return ByteArray.toHexString(eccKey.getAddress());
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static byte[] getPassWord(String password) {
        if (!passwordValid(password)) {
            return null;
        }
        byte[] pwd;
        pwd = Sha256Hash.hash(password.getBytes());
        pwd = Sha256Hash.hash(pwd);
        pwd = Arrays.copyOfRange(pwd, 0, 16);
        return pwd;
    }

    public static byte[] getEncKey(String password) {
        if (!passwordValid(password)) {
            return null;
        }
        byte[] encKey;
        encKey = Sha256Hash.hash(password.getBytes());
        encKey = Arrays.copyOfRange(encKey, 0, 16);
        return encKey;
    }

    public static boolean checkPassWord(String password) {
        byte[] pwd = getPassWord(password);
        if (pwd == null) {
            return false;
        }
        String pwdAsc = ByteArray.toHexString(pwd);
        String pwdInstore = loadPassword();
        return pwdAsc.equals(pwdInstore);
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
        if (preFixbyte != getAddressPreFixByte()) {
            logger.warn("Warning: Address need prefix with " + getAddressPreFixByte() + " but "
                    + preFixbyte + " !!");
            return false;
        }
        //Other rule;
        return true;
    }

    public static String encode58Check(byte[] input) {
        byte[] hash0 = Sha256Hash.hash(input);
        byte[] hash1 = Sha256Hash.hash(hash0);
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
        byte[] hash0 = Sha256Hash.hash(decodeData);
        byte[] hash1 = Sha256Hash.hash(hash0);
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

/*    public static Optional<AccountList> listAccounts() {
        Optional<AccountList> result = rpcCli.listAccounts();
        if (result.isPresent()) {
            AccountList accountList = result.get();
            List<Account> list = accountList.getAccountsList();
            List<Account> newList = new ArrayList();
            newList.addAll(list);
            newList.sort(new AccountComparator());
            AccountList.Builder builder = AccountList.newBuilder();
            newList.forEach(account -> builder.addAccounts(account));
            result = Optional.of(builder.build());
        }
        return result;
    }*/

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

    /*    public static Optional<AssetIssueList> getAssetIssueListByTimestamp(long timestamp) {
        return rpcCli.getAssetIssueListByTimestamp(timestamp);
    }*/

    /*    public static Optional<TransactionList> getTransactionsByTimestamp(long start, long end, int offset, int limit) {
        return rpcCli.getTransactionsByTimestamp(start, end, offset, limit);
    }*/

    public static Optional<AssetIssueList> getAssetIssueList() {
        return rpcCli.getAssetIssueList();
    }

    public static Optional<NodeList> listNodes() {
        return rpcCli.listNodes();
    }

    /*    public static Optional<AssetIssueList> getAssetIssueByAccount(byte[] address) {
        return rpcCli.getAssetIssueByAccount(address);
    }

    public static AssetIssueContract getAssetIssueByName(String assetName) {
        return rpcCli.getAssetIssueByName(assetName);
    }

    public static GrpcAPI.NumberMessage getTotalTransaction() {
        return rpcCli.getTotalTransaction();
    }*/

    public static Optional<TransactionList> getTransactionsFromThis(byte[] address) {
        return rpcCli.getTransactionsFromThis(address);
    }

    public static Optional<TransactionList> getTransactionsToThis(byte[] address) {
        return rpcCli.getTransactionsToThis(address);
    }

    /*    public static Optional<Transaction> getTransactionById(String txID) {
        return rpcCli.getTransactionById(txID);
    }*/

    public boolean freezeBalance(long frozen_balance, long frozen_duration) {

        FreezeBalanceContract contract = createFreezeBalanceContract(frozen_balance,
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
        FreezeBalanceContract.Builder builder = FreezeBalanceContract.newBuilder();
        ByteString byteAddreess = ByteString.copyFrom(address);

        builder.setOwnerAddress(byteAddreess).setFrozenBalance(frozen_balance)
                .setFrozenDuration(frozen_duration);

        return builder.build();
    }

    public boolean unfreezeBalance() {
        UnfreezeBalanceContract contract = createUnfreezeBalanceContract();

        Transaction transaction = rpcCli.createTransaction(contract);

        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            return false;
        }

        transaction = signTransaction(transaction);
        return rpcCli.broadcastTransaction(transaction);
    }

    private UnfreezeBalanceContract createUnfreezeBalanceContract() {

        byte[] address = getAddress();
        UnfreezeBalanceContract.Builder builder = UnfreezeBalanceContract
                .newBuilder();
        ByteString byteAddreess = ByteString.copyFrom(address);

        builder.setOwnerAddress(byteAddreess);

        return builder.build();
    }

    public boolean withdrawBalance() {
        WithdrawBalanceContract contract = createWithdrawBalanceContract();

        Transaction transaction = rpcCli.createTransaction(contract);

        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            return false;
        }

        transaction = signTransaction(transaction);
        return rpcCli.broadcastTransaction(transaction);
    }

    private WithdrawBalanceContract createWithdrawBalanceContract() {

        byte[] address = getAddress();
        WithdrawBalanceContract.Builder builder = WithdrawBalanceContract
                .newBuilder();
        ByteString byteAddreess = ByteString.copyFrom(address);

        builder.setOwnerAddress(byteAddreess);

        return builder.build();
    }

    public static Block getBlock(long blockNum){return rpcCli.getBlock(blockNum);}

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
