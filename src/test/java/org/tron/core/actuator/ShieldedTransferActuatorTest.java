package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.Any.Builder;
import com.google.protobuf.ByteString;
import com.sun.jna.Pointer;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.crypto.zksnark.ZksnarkUtils;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.zksnark.Librustzcash;
import org.tron.common.zksnark.LibrustzcashParam.InitZksnarkParams;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.IncrementalMerkleTreeCapsule;
import org.tron.core.capsule.PedersenHashCapsule;
import org.tron.core.capsule.ReceiveDescriptionCapsule;
import org.tron.core.capsule.SpendDescriptionCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.zen.ZenTransactionBuilder;
import org.tron.core.zen.ZenTransactionBuilder.SpendDescriptionInfo;
import org.tron.core.zen.address.ExpandedSpendingKey;
import org.tron.core.zen.address.PaymentAddress;
import org.tron.core.zen.address.SpendingKey;
import org.tron.core.zen.merkle.IncrementalMerkleTreeContainer;
import org.tron.core.zen.merkle.IncrementalMerkleVoucherContainer;
import org.tron.core.zen.note.Note;
import org.tron.protos.Contract;
import org.tron.protos.Contract.PedersenHash;
import org.tron.protos.Protocol.AccountType;

@Slf4j
public class ShieldedTransferActuatorTest {

  private static Manager dbManager;
  private static final String dbPath = "output_transfer_test";
  private static TronApplicationContext context;
  private static final String OWNER_ADDRESS;
  private static final String TO_ADDRESS;
  private static final long AMOUNT = 100;
  private static final long OWNER_BALANCE = 9999999;
  private static final long TO_BALANCE = 100001;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String TO_ADDRESS_INVALID = "bbb";
  private static final String OWNER_ACCOUNT_INVALID;
  private static final String OWNER_NO_BALANCE;
  private static final String To_ACCOUNT_INVALID;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    TO_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    OWNER_ACCOUNT_INVALID =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3456";
    OWNER_NO_BALANCE = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3433";
    To_ACCOUNT_INVALID =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3422";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    //    Args.setParam(new String[]{"--output-directory", dbPath},
    //        "config-junit.conf");
    //    dbManager = new Manager();
    //    dbManager.init();
  }

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  /**
   * create temp Capsule test need.
   */
  @Before
  public void createCapsule() {
    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            OWNER_BALANCE);
    AccountCapsule toAccountCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("toAccount"),
            ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
            AccountType.Normal,
            TO_BALANCE);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    dbManager.getAccountStore().put(toAccountCapsule.getAddress().toByteArray(), toAccountCapsule);
  }

  private Any getTransparentOutContract(long outAmount) throws BadItemException, ZksnarkException {
    return Any.pack(
        Contract.ShieldedTransferContract.newBuilder()
            .setTransparentFromAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setFromAmount(outAmount)
            .setSpendDescription(0,
                generateSpendDescription().getInstance()) //get a spend description
            .build());
  }

  private Any getTransparentToContract(long inAmount) throws BadItemException, ZksnarkException {
    return Any.pack(
        Contract.ShieldedTransferContract.newBuilder()
            .setTransparentToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setToAmount(inAmount)
            .setSpendDescription(0,
                generateSpendDescription().getInstance()) //get a spend description
            .build());
  }

  private Any getTransparentOutToContract(long outAmount, long inAmount)
      throws BadItemException, ZksnarkException {
    return Any.pack(
        Contract.ShieldedTransferContract.newBuilder()
            .setTransparentFromAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setFromAmount(outAmount)
            .setTransparentToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setToAmount(inAmount)
            .setSpendDescription(0,
                generateSpendDescription().getInstance()) //get a spend description
            .build());
  }

  private Builder getRawShiedledTransactionContract() {
//    return Any.pack(
//        Contract.ShieldedTransferContract.newBuilder()
//            .setSpe
//    )
//
    return null;
  }

  private String getParamsFile(String fileName) {
    return ShieldedTransferActuatorTest.class.getClassLoader()
        .getResource("params" + File.separator + fileName).getFile();
  }

  private void librustzcashInitZksnarkParams() throws ZksnarkException {

    String spendPath = getParamsFile("sapling-spend.params");
    String spendHash = "8270785a1a0d0bc77196f000ee6d221c9c9894f55307bd9357c3f0105d31ca63991ab91324160d8f53e2bbd3c2633a6eb8bdf5205d822e7f3f73edac51b2b70c";

    String outputPath = getParamsFile("sapling-output.params");
    String outputHash = "657e3d38dbb5cb5e7dd2970e8b03d69b4787dd907285b5a7f0790dcc8072f60bf593b32cc2d1c030e00ff5ae64bf84c5c3beb84ddc841d48264b4a171744d028";

    Librustzcash.librustzcashInitZksnarkParams(
        new InitZksnarkParams(spendPath.getBytes(), spendPath.length(), spendHash,
            outputPath.getBytes(), outputPath.length(), outputHash));
  }

  private PedersenHash String2PedersenHash(String str) {
    PedersenHashCapsule compressCapsule1 = new PedersenHashCapsule();
    byte[] bytes1 = ByteArray.fromHexString(str);
    ZksnarkUtils.sort(bytes1);
    compressCapsule1.setContent(ByteString.copyFrom(bytes1));
    return compressCapsule1.getInstance();
  }

  private PedersenHash ByteArray2PedersenHash(byte[] bytes) {
    PedersenHashCapsule compressCapsule_in = new PedersenHashCapsule();
    compressCapsule_in.setContent(ByteString.copyFrom(bytes));
    return compressCapsule_in.getInstance();
  }


  private SpendDescriptionCapsule generateSpendDescription()
      throws BadItemException, ZksnarkException {
    librustzcashInitZksnarkParams();

    ZenTransactionBuilder builder = null; //= new ZenTransactionBuilder();

    //generate extended spending key

    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();

    //generate note cm to merkle root
    IncrementalMerkleTreeContainer tree =
        new IncrementalMerkleTreeContainer(new IncrementalMerkleTreeCapsule());

    String s1 = "556f3af94225d46b1ef652abc9005dee873b2e245eef07fd5be587e0f21023b0";
    PedersenHash a = String2PedersenHash(s1);

    String s2 = "5814b127a6c6b8f07ed03f0f6e2843ff04c9851ff824a4e5b4dad5b5f3475722";
    PedersenHash b = String2PedersenHash(s2);

    String s3 = "6c030e6d7460f91668cc842ceb78cdb54470469e78cd59cf903d3a6e1aa03e7c";
    PedersenHash c = String2PedersenHash(s3);

    Note note = new Note(address, 100);
    PedersenHash p_in = ByteArray2PedersenHash(note.cm());

//    tree.append(a);
//    tree.append(b);
    tree.append(p_in);
    IncrementalMerkleVoucherContainer voucher = tree.toVoucher();
//    voucher.append(c);

    byte[] anchor = voucher.root().getContent().toByteArray();

    SpendDescriptionInfo spend = new SpendDescriptionInfo(expsk, note, anchor, voucher);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    SpendDescriptionCapsule sdesc = builder.generateSpendProof(spend, ctx);
    return sdesc;
  }

//  private ReceiveDescriptionCapsule generateReceiveDescription() {
//    byte[] cm = output.getNote().cm();
//    if (ByteArray.isEmpty(cm)) {
//      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
//      throw new RuntimeException("Output is invalid");
//    }
//
//    NotePlaintext notePlaintext = new NotePlaintext(output.getNote(), output.getMemo());
//
//    Optional<NotePlaintextEncryptionResult> res = notePlaintext
//        .encrypt(output.getNote().pkD);
//    if (!res.isPresent()) {
//      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
//      throw new RuntimeException("Failed to encrypt note");
//    }
//
//    NotePlaintextEncryptionResult enc = res.get();
//    NoteEncryption encryptor = enc.noteEncryption;
//
//    byte[] cv = new byte[32];
//    byte[] zkProof = new byte[192];
//    if (!Librustzcash.librustzcashSaplingOutputProof(
//        ctx,
//        encryptor.esk,
//        output.getNote().d.data,
//        output.getNote().pkD,
//        output.getNote().r,
//        output.getNote().value,
//        cv,
//        zkProof)) {
//      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
//      throw new RuntimeException("Output proof failed");
//

  private ReceiveDescriptionCapsule generateReceiveDescription() {
    return null;
  }


  @Test
  public void rightTransfer() {

  }

  @Test
  public void perfectTransfer() {

  }

  @Test
  public void moreTransfer() {

  }

  @Test
  public void iniviateOwnerAddress() {

  }

  @Test
  public void iniviateToAddress() {

  }

  @Test
  public void iniviateTrx() {

  }

  @Test
  public void noExitOwnerAccount() {

  }

  @Test
  /**
   * If to account not exit, create it.
   */
  public void noExitToAccount() {

  }

  @Test
  public void zeroAmountTest() {

  }

  @Test
  public void negativeAmountTest() {

  }

  @Test
  public void addOverflowTest() {

  }

  @Test
  public void insufficientFee() {

  }

}
