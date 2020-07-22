package org.tron.common.runtime.vm;

import com.google.protobuf.ByteString;
import java.io.File;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.api.GrpcAPI.ShieldedTRC20Parameters;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.FileUtil;
import org.tron.common.zksnark.IncrementalMerkleTreeContainer;
import org.tron.common.zksnark.IncrementalMerkleVoucherContainer;
import org.tron.common.zksnark.JLibrustzcash;
import org.tron.common.zksnark.LibrustzcashParam;
import org.tron.core.capsule.IncrementalMerkleTreeCapsule;
import org.tron.core.capsule.PedersenHashCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.services.http.FullNodeHttpApiService;
import org.tron.core.vm.PrecompiledContracts.MerkleHash;
import org.tron.core.vm.PrecompiledContracts.VerifyBurnProof;
import org.tron.core.vm.PrecompiledContracts.VerifyMintProof;
import org.tron.core.vm.PrecompiledContracts.VerifyTransferProof;
import org.tron.core.zen.ShieldedTRC20ParametersBuilder;
import org.tron.core.zen.ShieldedTRC20ParametersBuilder.ShieldedTRC20ParametersType;
import org.tron.core.zen.address.DiversifierT;
import org.tron.core.zen.address.ExpandedSpendingKey;
import org.tron.core.zen.address.FullViewingKey;
import org.tron.core.zen.address.IncomingViewingKey;
import org.tron.core.zen.address.PaymentAddress;
import org.tron.core.zen.address.SpendingKey;
import org.tron.core.zen.note.Note;
import org.tron.keystore.Wallet;
import org.tron.protos.contract.ShieldContract;
import stest.tron.wallet.common.client.WalletClient;

@Slf4j
public class PrecompiledContractsVerifyProofTest {

  private static final String dbPath = "output_PrecompiledContracts_VerifyProof_test";
  private static final String SHIELDED_CONTRACT_ADDRESS_STR = "TGAmX5AqVUoXCf8MoHxbuhQPmhGfWTnEgA";
  private static final byte[] SHIELDED_CONTRACT_ADDRESS;
  private static final String PUBLIC_TO_ADDRESS_STR = "TBaBXpRAeBhs75TZT751LwyhrcR25XeUot";
  private static final byte[] PUBLIC_TO_ADDRESS;
  private static final byte[] DEFAULT_OVK;
  private static TronApplicationContext context;
  private static Manager dbManager;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, "config-test.conf");
    context = new TronApplicationContext(DefaultConfig.class);
    DEFAULT_OVK = ByteArray
        .fromHexString("030c8c2bc59fb3eb8afb047a8ea4b028743d23e7d38c6fa30908358431e2314d");
    SHIELDED_CONTRACT_ADDRESS = WalletClient.decodeFromBase58Check(SHIELDED_CONTRACT_ADDRESS_STR);
    PUBLIC_TO_ADDRESS = WalletClient.decodeFromBase58Check(PUBLIC_TO_ADDRESS_STR);
    FullNodeHttpApiService.librustzcashInitZksnarkParams();
  }

  VerifyMintProof mintContract = new VerifyMintProof();
  VerifyTransferProof transferContract = new VerifyTransferProof();
  VerifyBurnProof burnContract = new VerifyBurnProof();
  MerkleHash merkleHash = new MerkleHash();

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
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

  @Test
  public void verifyMintProofCorrect() throws ZksnarkException {
    int totalCountNum = 2;
    long leafCount = 0;
    long value = 100L;
    byte[] frontier = new byte[32 * 33];

    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
      builder.setTransparentFromAmount(BigInteger.valueOf(value));
      builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
      builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);

      //ReceiveNote
      SpendingKey recvSk = SpendingKey.random();
      FullViewingKey fullViewingKey = recvSk.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
      builder.addOutput(DEFAULT_OVK, paymentAddress, value, new byte[512]);
      ShieldedTRC20Parameters params = builder.build(false);

      byte[] inputData = abiEncodeForMint(params, value, frontier, leafCount);
      Pair<Boolean, byte[]> contractResult = mintContract.execute(inputData);
      byte[] result = contractResult.getRight();

      Assert.assertEquals(1, result[31]);

      //update frontier and leafCount
      //if slot == 0, frontier[0:31]=noteCommitment
      int slot = result[63];
      if (slot == 0) {
        System.arraycopy(inputData, 0, frontier, 0, 32);
      } else {
        int srcPos = (slot + 1) * 32;
        int destPos = slot * 32;
        System.arraycopy(result, srcPos, frontier, destPos, 32);
      }
      leafCount++;
    }
  }

  @Test
  public void verifyTransferProofCorrect() throws ZksnarkException {
    int totalCountNum = 2;
    long leafCount = 0;
    byte[] frontier = new byte[32 * 33];

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      SpendingKey senderSk = SpendingKey.random();
      ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
      FullViewingKey senderFvk = senderSk.fullViewingKey();
      byte[] senderOvk = senderFvk.getOvk();
      IncomingViewingKey senderIvk = senderFvk.inViewingKey();
      byte[] rcm1 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm1);
      byte[] rcm2 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm2);
      PaymentAddress senderPaymentAddress1 = senderIvk.address(DiversifierT.random()).get();
      PaymentAddress senderPaymentAddress2 = senderIvk.address(DiversifierT.random()).get();

      { //for mint1
        ShieldedTRC20ParametersBuilder mintBuilder1 = new ShieldedTRC20ParametersBuilder();
        mintBuilder1.setTransparentFromAmount(BigInteger.valueOf(30));
        mintBuilder1.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder1.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder1.addOutput(DEFAULT_OVK, senderPaymentAddress1.getD(),
            senderPaymentAddress1.getPkD(), 30, rcm1, new byte[512]);
        ShieldedTRC20Parameters mintParams1 = mintBuilder1.build(false);

        byte[] mintInputData1 = abiEncodeForMint(mintParams1, 30, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult1 = mintContract.execute(mintInputData1);
        byte[] mintResult1 = mintContractResult1.getRight();
        Assert.assertEquals(1, mintResult1[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        int slot = mintResult1[63];
        if (slot == 0) {
          System.arraycopy(mintInputData1, 0, frontier, 0, 32);
        } else {
          int srcPos = (slot + 1) * 32;
          int destPos = slot * 32;
          System.arraycopy(mintResult1, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for mint2
        ShieldedTRC20ParametersBuilder mintBuilder2 = new ShieldedTRC20ParametersBuilder();
        mintBuilder2.setTransparentFromAmount(BigInteger.valueOf(70));
        mintBuilder2.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder2.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder2.addOutput(DEFAULT_OVK, senderPaymentAddress2.getD(),
            senderPaymentAddress2.getPkD(), 70, rcm2, new byte[512]);
        ShieldedTRC20Parameters mintParams2 = mintBuilder2.build(false);

        byte[] mintInputData2 = abiEncodeForMint(mintParams2, 70, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult2 = mintContract.execute(mintInputData2);
        byte[] mintResult2 = mintContractResult2.getRight();

        Assert.assertEquals(1, mintResult2[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        int slot = mintResult2[63];
        if (slot == 0) {
          System.arraycopy(mintInputData2, 0, frontier, 0, 32);
        } else {
          int srcPos = (slot + 1) * 32;
          int destPos = slot * 32;
          System.arraycopy(mintResult2, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for transfer
        ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
        builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.TRANSFER);
        builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        builder.setTransparentFromAmount(BigInteger.ZERO);
        builder.setTransparentToAmount(BigInteger.ZERO);
        //spendNote1
        Note senderNote1 = new Note(senderPaymentAddress1.getD(), senderPaymentAddress1.getPkD(),
            30, rcm1, new byte[512]);
        byte[][] cm1 = new byte[1][32];
        System.arraycopy(senderNote1.cm(), 0, cm1[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher1 = addSimpleMerkleVoucherContainer(tree, cm1);
        byte[] path1 = decodePath(voucher1.path().encode());
        byte[] anchor1 = voucher1.root().getContent().toByteArray();
        long position1 = voucher1.position();
        builder.addSpend(senderExpsk, senderNote1, anchor1, path1, position1);

        //spendNote2
        Note senderNote2 = new Note(senderPaymentAddress2.getD(), senderPaymentAddress2.getPkD(),
            70, rcm2, new byte[512]);
        byte[][] cm2 = new byte[1][32];
        System.arraycopy(senderNote2.cm(), 0, cm2[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher2 = addSimpleMerkleVoucherContainer(tree, cm2);
        byte[] path2 = decodePath(voucher2.path().encode());
        byte[] anchor2 = voucher2.root().getContent().toByteArray();
        long position2 = voucher2.position();
        builder.addSpend(senderExpsk, senderNote2, anchor2, path2, position2);

        //receiveNote1
        SpendingKey receiveSk1 = SpendingKey.random();
        FullViewingKey receiveFvk1 = receiveSk1.fullViewingKey();
        IncomingViewingKey receiveIvk1 = receiveFvk1.inViewingKey();
        PaymentAddress receivePaymentAddress1 = receiveIvk1.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress1, 40, new byte[512]);

        //receiveNote2
        SpendingKey receiveSk2 = SpendingKey.random();
        FullViewingKey receiveFvk2 = receiveSk2.fullViewingKey();
        IncomingViewingKey receiveIvk2 = receiveFvk2.inViewingKey();
        PaymentAddress receivePaymentAddress2 = receiveIvk2.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress2, 60, new byte[512]);

        ShieldedTRC20Parameters params = builder.build(true);

        byte[] inputData = abiEncodeForTransfer(params, frontier, leafCount, 0);
        Pair<Boolean, byte[]> contractResult = verifyTransfer(inputData);
        byte[] result = contractResult.getRight();
        Assert.assertEquals(1, result[31]);

        //update frontier and leafCount
        int idx = 32;
        for (int i = 0; i < 2; i++) {
          idx += 31;
          int slot = result[idx];
          idx += 1;
          if (slot == 0) {
            byte[] noteCommitment = params.getReceiveDescription(i).getNoteCommitment()
                .toByteArray();
            System.arraycopy(noteCommitment, 0, frontier, 0, 32);
          } else {
            int destPos = slot * 32;
            int srcPos = (slot - 1) * 32 + idx;
            System.arraycopy(result, srcPos, frontier, destPos, 32);
          }
          idx += slot * 32;
          leafCount++;
        }

        byte[][] cm = new byte[2][32];
        for (int i = 0; i < 2; i++) {
          byte[] noteCommitment = params.getReceiveDescription(i).getNoteCommitment()
              .toByteArray();
          System.arraycopy(noteCommitment, 0, cm[i], 0, 32);
        }
        IncrementalMerkleVoucherContainer voucher = addSimpleMerkleVoucherContainer(tree, cm);
        byte[] root = voucher.root().getContent().toByteArray();

        Assert.assertArrayEquals(root, Arrays.copyOfRange(result, idx, idx + 32));
      }
    }
  }

  @Test
  public void verifyTransfer1v1ProofCorrect() throws ZksnarkException {
    int totalCountNum = 2;
    long leafCount = 0;
    byte[] frontier = new byte[32 * 33];

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      SpendingKey senderSk = SpendingKey.random();
      ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
      FullViewingKey senderFvk = senderSk.fullViewingKey();
      byte[] senderOvk = senderFvk.getOvk();
      IncomingViewingKey senderIvk = senderFvk.inViewingKey();
      byte[] rcm1 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm1);
      PaymentAddress senderPaymentAddress1 = senderIvk.address(DiversifierT.random()).get();

      { //for mint1
        ShieldedTRC20ParametersBuilder mintBuilder1 = new ShieldedTRC20ParametersBuilder();
        mintBuilder1.setTransparentFromAmount(BigInteger.valueOf(100));
        mintBuilder1.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder1.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder1.addOutput(DEFAULT_OVK, senderPaymentAddress1.getD(),
            senderPaymentAddress1.getPkD(), 100, rcm1, new byte[512]);
        ShieldedTRC20Parameters mintParams1 = mintBuilder1.build(false);

        byte[] mintInputData1 = abiEncodeForMint(mintParams1, 100, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult1 = mintContract.execute(mintInputData1);
        byte[] mintResult1 = mintContractResult1.getRight();
        Assert.assertEquals(1, mintResult1[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        int slot = mintResult1[63];
        if (slot == 0) {
          System.arraycopy(mintInputData1, 0, frontier, 0, 32);
        } else {
          int srcPos = (slot + 1) * 32;
          int destPos = slot * 32;
          System.arraycopy(mintResult1, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for transfer
        ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
        builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.TRANSFER);
        builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        builder.setTransparentFromAmount(BigInteger.ZERO);
        builder.setTransparentToAmount(BigInteger.ZERO);
        //spendNote1
        Note senderNote1 = new Note(senderPaymentAddress1.getD(), senderPaymentAddress1.getPkD(),
            100, rcm1, new byte[512]);
        byte[][] cm1 = new byte[1][32];
        System.arraycopy(senderNote1.cm(), 0, cm1[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher1 = addSimpleMerkleVoucherContainer(tree, cm1);
        byte[] path1 = decodePath(voucher1.path().encode());
        byte[] anchor1 = voucher1.root().getContent().toByteArray();
        long position1 = voucher1.position();
        builder.addSpend(senderExpsk, senderNote1, anchor1, path1, position1);

        //receiveNote1
        SpendingKey receiveSk1 = SpendingKey.random();
        FullViewingKey receiveFvk1 = receiveSk1.fullViewingKey();
        IncomingViewingKey receiveIvk1 = receiveFvk1.inViewingKey();
        PaymentAddress receivePaymentAddress1 = receiveIvk1.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress1, 100, new byte[512]);

        ShieldedTRC20Parameters params = builder.build(true);

        byte[] inputData = abiEncodeForTransfer(params, frontier, leafCount, 0);
        Pair<Boolean, byte[]> contractResult = verifyTransfer(inputData);
        byte[] result = contractResult.getRight();
        Assert.assertEquals(1, result[31]);

        //update frontier and leafCount
        int idx = 63;
        int slot = result[idx];
        if (slot == 0) {
          byte[] noteCommitment = params.getReceiveDescription(0).getNoteCommitment()
              .toByteArray();
          System.arraycopy(noteCommitment, 0, frontier, 0, 32);
        } else {
          int destPos = slot * 32;
          int srcPos = (slot - 1) * 32 + idx + 1;
          System.arraycopy(result, srcPos, frontier, destPos, 32);
        }
        idx += slot * 32 + 1;
        leafCount++;

        byte[][] cm = new byte[1][32];
        for (int i = 0; i < 1; i++) {
          byte[] noteCommitment = params.getReceiveDescription(i).getNoteCommitment()
              .toByteArray();
          System.arraycopy(noteCommitment, 0, cm[i], 0, 32);
        }
        IncrementalMerkleVoucherContainer voucher = addSimpleMerkleVoucherContainer(tree, cm);
        byte[] root = voucher.root().getContent().toByteArray();

        Assert.assertArrayEquals(root, Arrays.copyOfRange(result, idx, idx + 32));
      }
    }
  }

  @Test
  public void verifyBurnWithCmCorrect() throws ZksnarkException {
    int totalCountNum = 2;
    long leafCount = 0;
    byte[] frontier = new byte[32 * 33];

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      SpendingKey senderSk = SpendingKey.random();
      ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
      FullViewingKey senderFvk = senderSk.fullViewingKey();
      byte[] senderOvk = senderFvk.getOvk();
      IncomingViewingKey senderIvk = senderFvk.inViewingKey();
      byte[] rcm1 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm1);
      PaymentAddress senderPaymentAddress1 = senderIvk.address(DiversifierT.random()).get();

      { //for mint1
        ShieldedTRC20ParametersBuilder mintBuilder1 = new ShieldedTRC20ParametersBuilder();
        mintBuilder1.setTransparentFromAmount(BigInteger.valueOf(100));
        mintBuilder1.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder1.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder1.addOutput(DEFAULT_OVK, senderPaymentAddress1.getD(),
            senderPaymentAddress1.getPkD(), 100, rcm1, new byte[512]);
        ShieldedTRC20Parameters mintParams1 = mintBuilder1.build(false);

        byte[] mintInputData1 = abiEncodeForMint(mintParams1, 100, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult1 = mintContract.execute(mintInputData1);
        byte[] mintResult1 = mintContractResult1.getRight();
        Assert.assertEquals(1, mintResult1[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        int slot = mintResult1[63];
        if (slot == 0) {
          System.arraycopy(mintInputData1, 0, frontier, 0, 32);
        } else {
          int srcPos = (slot + 1) * 32;
          int destPos = slot * 32;
          System.arraycopy(mintResult1, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for burn
        ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
        builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.BURN);
        builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        builder.setTransparentFromAmount(BigInteger.ZERO);
        builder.setTransparentToAmount(BigInteger.valueOf(50L));
        builder.setTransparentToAddress(PUBLIC_TO_ADDRESS);
        //spendNote1
        Note senderNote1 = new Note(senderPaymentAddress1.getD(), senderPaymentAddress1.getPkD(),
            100, rcm1, new byte[512]);
        byte[][] cm1 = new byte[1][32];
        System.arraycopy(senderNote1.cm(), 0, cm1[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher1 = addSimpleMerkleVoucherContainer(tree, cm1);
        byte[] path1 = decodePath(voucher1.path().encode());
        byte[] anchor1 = voucher1.root().getContent().toByteArray();
        long position1 = voucher1.position();
        builder.addSpend(senderExpsk, senderNote1, anchor1, path1, position1);

        //receiveNote1
        SpendingKey receiveSk1 = SpendingKey.random();
        FullViewingKey receiveFvk1 = receiveSk1.fullViewingKey();
        IncomingViewingKey receiveIvk1 = receiveFvk1.inViewingKey();
        PaymentAddress receivePaymentAddress1 = receiveIvk1.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress1, 50, new byte[512]);

        ShieldedTRC20Parameters params = builder.build(true);

        byte[] inputData = abiEncodeForTransfer(params, frontier, leafCount, 50);
        Pair<Boolean, byte[]> contractResult = verifyTransfer(inputData);
        byte[] result = contractResult.getRight();
        Assert.assertEquals(1, result[31]);

        //update frontier and leafCount
        int idx = 63;
        int slot = result[idx];
        if (slot == 0) {
          byte[] noteCommitment = params.getReceiveDescription(0).getNoteCommitment()
              .toByteArray();
          System.arraycopy(noteCommitment, 0, frontier, 0, 32);
        } else {
          int destPos = slot * 32;
          int srcPos = (slot - 1) * 32 + idx + 1;
          System.arraycopy(result, srcPos, frontier, destPos, 32);
        }
        idx += slot * 32 + 1;
        leafCount++;

        byte[][] cm = new byte[1][32];
        for (int i = 0; i < 1; i++) {
          byte[] noteCommitment = params.getReceiveDescription(i).getNoteCommitment()
              .toByteArray();
          System.arraycopy(noteCommitment, 0, cm[i], 0, 32);
        }
        IncrementalMerkleVoucherContainer voucher = addSimpleMerkleVoucherContainer(tree, cm);
        byte[] root = voucher.root().getContent().toByteArray();

        Assert.assertArrayEquals(root, Arrays.copyOfRange(result, idx, idx + 32));
      }
    }
  }

  @Test
  public void verifyTransfer1v2ProofCorrect() throws ZksnarkException {
    int totalCountNum = 5;
    long leafCount = 0;
    byte[] frontier = new byte[32 * 33];

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      SpendingKey senderSk = SpendingKey.random();
      ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
      FullViewingKey senderFvk = senderSk.fullViewingKey();
      byte[] senderOvk = senderFvk.getOvk();
      IncomingViewingKey senderIvk = senderFvk.inViewingKey();
      byte[] rcm1 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm1);
      PaymentAddress senderPaymentAddress1 = senderIvk.address(DiversifierT.random()).get();

      { //for mint1
        ShieldedTRC20ParametersBuilder mintBuilder1 = new ShieldedTRC20ParametersBuilder();
        mintBuilder1.setTransparentFromAmount(BigInteger.valueOf(100));
        mintBuilder1.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder1.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder1.addOutput(DEFAULT_OVK, senderPaymentAddress1.getD(),
            senderPaymentAddress1.getPkD(), 100, rcm1, new byte[512]);
        ShieldedTRC20Parameters mintParams1 = mintBuilder1.build(false);

        byte[] mintInputData1 = abiEncodeForMint(mintParams1, 100, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult1 = mintContract.execute(mintInputData1);
        byte[] mintResult1 = mintContractResult1.getRight();
        Assert.assertEquals(1, mintResult1[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        int slot = mintResult1[63];
        if (slot == 0) {
          System.arraycopy(mintInputData1, 0, frontier, 0, 32);
        } else {
          int srcPos = (slot + 1) * 32;
          int destPos = slot * 32;
          System.arraycopy(mintResult1, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for transfer
        ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
        builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.TRANSFER);
        builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        builder.setTransparentFromAmount(BigInteger.ZERO);
        builder.setTransparentToAmount(BigInteger.ZERO);
        //spendNote1
        Note senderNote1 = new Note(senderPaymentAddress1.getD(), senderPaymentAddress1.getPkD(),
            100, rcm1, new byte[512]);
        byte[][] cm1 = new byte[1][32];
        System.arraycopy(senderNote1.cm(), 0, cm1[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher1 = addSimpleMerkleVoucherContainer(tree, cm1);
        byte[] path1 = decodePath(voucher1.path().encode());
        byte[] anchor1 = voucher1.root().getContent().toByteArray();
        long position1 = voucher1.position();
        builder.addSpend(senderExpsk, senderNote1, anchor1, path1, position1);

        //receiveNote1
        SpendingKey receiveSk1 = SpendingKey.random();
        FullViewingKey receiveFvk1 = receiveSk1.fullViewingKey();
        IncomingViewingKey receiveIvk1 = receiveFvk1.inViewingKey();
        PaymentAddress receivePaymentAddress1 = receiveIvk1.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress1, 40, new byte[512]);

        //receiveNote2
        SpendingKey receiveSk2 = SpendingKey.random();
        FullViewingKey receiveFvk2 = receiveSk2.fullViewingKey();
        IncomingViewingKey receiveIvk2 = receiveFvk2.inViewingKey();
        PaymentAddress receivePaymentAddress2 = receiveIvk2.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress2, 60, new byte[512]);

        ShieldedTRC20Parameters params = builder.build(true);

        byte[] inputData = abiEncodeForTransfer(params, frontier, leafCount, 0);
        Pair<Boolean, byte[]> contractResult = verifyTransfer(inputData);
        byte[] result = contractResult.getRight();
        Assert.assertEquals(1, result[31]);

        //update frontier and leafCount
        int idx = 32;
        for (int i = 0; i < 2; i++) {
          idx += 31;
          int slot = result[idx];
          idx += 1;
          if (slot == 0) {
            byte[] noteCommitment = params.getReceiveDescription(i).getNoteCommitment()
                .toByteArray();
            System.arraycopy(noteCommitment, 0, frontier, 0, 32);
          } else {
            int destPos = slot * 32;
            int srcPos = (slot - 1) * 32 + idx;
            System.arraycopy(result, srcPos, frontier, destPos, 32);
          }
          idx += slot * 32;
          leafCount++;
        }

        byte[][] cm = new byte[2][32];
        for (int i = 0; i < 2; i++) {
          byte[] noteCommitment = params.getReceiveDescription(i).getNoteCommitment()
              .toByteArray();
          System.arraycopy(noteCommitment, 0, cm[i], 0, 32);
        }
        IncrementalMerkleVoucherContainer voucher = addSimpleMerkleVoucherContainer(tree, cm);
        byte[] root = voucher.root().getContent().toByteArray();

        Assert.assertArrayEquals(root, Arrays.copyOfRange(result, idx, idx + 32));
      }
    }
  }

  @Test
  public void verifyTransfer2v1ProofCorrect() throws ZksnarkException {
    int totalCountNum = 2;
    long leafCount = 0;
    byte[] frontier = new byte[32 * 33];

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      SpendingKey senderSk = SpendingKey.random();
      ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
      FullViewingKey senderFvk = senderSk.fullViewingKey();
      byte[] senderOvk = senderFvk.getOvk();
      IncomingViewingKey senderIvk = senderFvk.inViewingKey();
      byte[] rcm1 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm1);
      byte[] rcm2 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm2);
      PaymentAddress senderPaymentAddress1 = senderIvk.address(DiversifierT.random()).get();
      PaymentAddress senderPaymentAddress2 = senderIvk.address(DiversifierT.random()).get();

      { //for mint1
        ShieldedTRC20ParametersBuilder mintBuilder1 = new ShieldedTRC20ParametersBuilder();
        mintBuilder1.setTransparentFromAmount(BigInteger.valueOf(30));
        mintBuilder1.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder1.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder1.addOutput(DEFAULT_OVK, senderPaymentAddress1.getD(),
            senderPaymentAddress1.getPkD(), 30, rcm1, new byte[512]);
        ShieldedTRC20Parameters mintParams1 = mintBuilder1.build(false);

        byte[] mintInputData1 = abiEncodeForMint(mintParams1, 30, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult1 = mintContract.execute(mintInputData1);
        byte[] mintResult1 = mintContractResult1.getRight();
        Assert.assertEquals(1, mintResult1[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        int slot = mintResult1[63];
        if (slot == 0) {
          System.arraycopy(mintInputData1, 0, frontier, 0, 32);
        } else {
          int srcPos = (slot + 1) * 32;
          int destPos = slot * 32;
          System.arraycopy(mintResult1, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for mint2
        ShieldedTRC20ParametersBuilder mintBuilder2 = new ShieldedTRC20ParametersBuilder();
        mintBuilder2.setTransparentFromAmount(BigInteger.valueOf(70));
        mintBuilder2.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder2.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder2.addOutput(DEFAULT_OVK, senderPaymentAddress2.getD(),
            senderPaymentAddress2.getPkD(), 70, rcm2, new byte[512]);
        ShieldedTRC20Parameters mintParams2 = mintBuilder2.build(false);

        byte[] mintInputData2 = abiEncodeForMint(mintParams2, 70, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult2 = mintContract.execute(mintInputData2);
        byte[] mintResult2 = mintContractResult2.getRight();

        Assert.assertEquals(1, mintResult2[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        int slot = mintResult2[63];
        if (slot == 0) {
          System.arraycopy(mintInputData2, 0, frontier, 0, 32);
        } else {
          int srcPos = (slot + 1) * 32;
          int destPos = slot * 32;
          System.arraycopy(mintResult2, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for transfer
        ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
        builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.TRANSFER);
        builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        builder.setTransparentFromAmount(BigInteger.ZERO);
        builder.setTransparentToAmount(BigInteger.ZERO);
        //spendNote1
        Note senderNote1 = new Note(senderPaymentAddress1.getD(), senderPaymentAddress1.getPkD(),
            30, rcm1, new byte[512]);
        byte[][] cm1 = new byte[1][32];
        System.arraycopy(senderNote1.cm(), 0, cm1[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher1 = addSimpleMerkleVoucherContainer(tree, cm1);
        byte[] path1 = decodePath(voucher1.path().encode());
        byte[] anchor1 = voucher1.root().getContent().toByteArray();
        long position1 = voucher1.position();
        builder.addSpend(senderExpsk, senderNote1, anchor1, path1, position1);

        //spendNote2
        Note senderNote2 = new Note(senderPaymentAddress2.getD(), senderPaymentAddress2.getPkD(),
            70, rcm2, new byte[512]);
        byte[][] cm2 = new byte[1][32];
        System.arraycopy(senderNote2.cm(), 0, cm2[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher2 = addSimpleMerkleVoucherContainer(tree, cm2);
        byte[] path2 = decodePath(voucher2.path().encode());
        byte[] anchor2 = voucher2.root().getContent().toByteArray();
        long position2 = voucher2.position();
        builder.addSpend(senderExpsk, senderNote2, anchor2, path2, position2);

        //receiveNote1
        SpendingKey receiveSk1 = SpendingKey.random();
        FullViewingKey receiveFvk1 = receiveSk1.fullViewingKey();
        IncomingViewingKey receiveIvk1 = receiveFvk1.inViewingKey();
        PaymentAddress receivePaymentAddress1 = receiveIvk1.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress1, 100, new byte[512]);

        ShieldedTRC20Parameters params = builder.build(true);

        byte[] inputData = abiEncodeForTransfer(params, frontier, leafCount, 0);
        Pair<Boolean, byte[]> contractResult = verifyTransfer(inputData);
        byte[] result = contractResult.getRight();
        Assert.assertEquals(1, result[31]);

        //update frontier and leafCount
        int idx = 63;
        int slot = result[idx];
        if (slot == 0) {
          byte[] noteCommitment = params.getReceiveDescription(0).getNoteCommitment()
              .toByteArray();
          System.arraycopy(noteCommitment, 0, frontier, 0, 32);
        } else {
          int destPos = slot * 32;
          int srcPos = (slot - 1) * 32 + idx + 1;
          System.arraycopy(result, srcPos, frontier, destPos, 32);
        }
        idx += slot * 32 + 1;
        leafCount++;

        byte[][] cm = new byte[1][32];
        for (int i = 0; i < 1; i++) {
          byte[] noteCommitment = params.getReceiveDescription(i).getNoteCommitment()
              .toByteArray();
          System.arraycopy(noteCommitment, 0, cm[i], 0, 32);
        }
        IncrementalMerkleVoucherContainer voucher = addSimpleMerkleVoucherContainer(tree, cm);
        byte[] root = voucher.root().getContent().toByteArray();

        Assert.assertArrayEquals(root, Arrays.copyOfRange(result, idx, idx + 32));
      }
    }
  }


  @Test
  public void verifyBurnProofCorrect() throws ZksnarkException {
    int totalCountNum = 2;
    long leafCount = 0;
    long value = 100L;
    byte[] frontier = new byte[32 * 33];

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      SpendingKey senderSk = SpendingKey.random();
      ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
      FullViewingKey senderFvk = senderSk.fullViewingKey();
      byte[] senderOvk = senderFvk.getOvk();
      IncomingViewingKey senderIvk = senderFvk.inViewingKey();
      byte[] rcm = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm);
      PaymentAddress senderPaymentAddress = senderIvk.address(DiversifierT.random()).get();

      { //for mint
        ShieldedTRC20ParametersBuilder mintBuilder = new ShieldedTRC20ParametersBuilder();
        mintBuilder.setTransparentFromAmount(BigInteger.valueOf(value));
        mintBuilder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder.addOutput(DEFAULT_OVK, senderPaymentAddress.getD(),
            senderPaymentAddress.getPkD(), value, rcm, new byte[512]);
        ShieldedTRC20Parameters mintParams = mintBuilder.build(false);

        byte[] mintInputData = abiEncodeForMint(mintParams, value, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult = mintContract.execute(mintInputData);
        byte[] mintResult = mintContractResult.getRight();
        Assert.assertEquals(1, mintResult[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        int slot = mintResult[63];
        if (slot == 0) {
          System.arraycopy(mintInputData, 0, frontier, 0, 32);
        } else {
          int srcPos = (slot + 1) * 32;
          int destPos = slot * 32;
          System.arraycopy(mintResult, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for burn
        ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
        builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.BURN);
        builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        builder.setTransparentToAmount(BigInteger.valueOf(value));
        builder.setTransparentToAddress(PUBLIC_TO_ADDRESS);
        //spendNote1
        Note senderNote = new Note(senderPaymentAddress.getD(), senderPaymentAddress.getPkD(),
            value, rcm, new byte[512]);
        byte[][] cm = new byte[1][32];
        System.arraycopy(senderNote.cm(), 0, cm[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher = addSimpleMerkleVoucherContainer(tree, cm);
        byte[] path = decodePath(voucher.path().encode());
        byte[] anchor = voucher.root().getContent().toByteArray();
        long position = voucher.position();
        builder.addSpend(senderExpsk, senderNote, anchor, path, position);
        ShieldedTRC20Parameters params = builder.build(true);

        byte[] inputData = abiEncodeForBurn(params, value);
        Pair<Boolean, byte[]> contractResult = burnContract.execute(inputData);
        byte[] result = contractResult.getRight();
        Assert.assertEquals(1, result[31]);
      }
    }
  }

  @Test
  public void merkleHashCorrectTest() throws ZksnarkException {
    int totalCountNum = 2;
    byte[][] uncommitted = new byte[32][32];
    //initialize uncommitted
    uncommitted[0] = ByteArray.fromHexString(
        "0100000000000000000000000000000000000000000000000000000000000000");
    try {
      for (int i = 0; i < 31; i++) {
        JLibrustzcash.librustzcashMerkleHash(
            new LibrustzcashParam.MerkleHashParams(
                i, uncommitted[i], uncommitted[i], uncommitted[i + 1]));
      }
    } catch (Throwable any) {
      any.printStackTrace();
    }
    for (int cnt = 0; cnt < totalCountNum; cnt++) {
      SpendingKey sk = SpendingKey.random();
      FullViewingKey fvk = sk.fullViewingKey();
      IncomingViewingKey ivk = fvk.inViewingKey();
      byte[] rcm = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm);
      PaymentAddress paymentAddress = ivk.address(DiversifierT.random()).get();
      Note note = new Note(paymentAddress.getD(), paymentAddress.getPkD(),
          randomLong(), rcm, new byte[512]);
      byte[] node = note.cm();

      for (int i = 0; i < 32; i++) {
        byte[] input = ByteUtil.merge(longTo32Bytes(i), node, uncommitted[i]);
        node = merkleHash.execute(input).getRight();
      }

      IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
          new IncrementalMerkleTreeCapsule());
      byte[][] noteCommitment = new byte[1][32];
      System.arraycopy(note.cm(), 0, noteCommitment[0], 0, 32);
      IncrementalMerkleVoucherContainer voucher =
          addSimpleMerkleVoucherContainer(tree, noteCommitment);
      byte[] anchor = voucher.root().getContent().toByteArray();

      Assert.assertArrayEquals(anchor, node);
    }
  }

  @Test
  public void verifyBurnWithCmWrong() throws ZksnarkException {
    long leafCount = 0;
    byte[] frontier = new byte[32 * 33];

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());

    SpendingKey senderSk = SpendingKey.random();
    ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
    FullViewingKey senderFvk = senderSk.fullViewingKey();
    byte[] senderOvk = senderFvk.getOvk();
    IncomingViewingKey senderIvk = senderFvk.inViewingKey();
    byte[] rcm1 = new byte[32];
    JLibrustzcash.librustzcashSaplingGenerateR(rcm1);
    PaymentAddress senderPaymentAddress1 = senderIvk.address(DiversifierT.random()).get();

    { //for mint1
      ShieldedTRC20ParametersBuilder mintBuilder1 = new ShieldedTRC20ParametersBuilder();
      mintBuilder1.setTransparentFromAmount(BigInteger.valueOf(100));
      mintBuilder1.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
      mintBuilder1.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
      mintBuilder1.addOutput(DEFAULT_OVK, senderPaymentAddress1.getD(),
          senderPaymentAddress1.getPkD(), 100, rcm1, new byte[512]);
      ShieldedTRC20Parameters mintParams1 = mintBuilder1.build(false);

      byte[] mintInputData1 = abiEncodeForMint(mintParams1, 100, frontier, leafCount);
      Pair<Boolean, byte[]> mintContractResult1 = mintContract.execute(mintInputData1);
      byte[] mintResult1 = mintContractResult1.getRight();
      Assert.assertEquals(1, mintResult1[31]);

      //update frontier and leafCount
      //if slot == 0, frontier[0:31]=noteCommitment
      int slot = mintResult1[63];
      if (slot == 0) {
        System.arraycopy(mintInputData1, 0, frontier, 0, 32);
      } else {
        int srcPos = (slot + 1) * 32;
        int destPos = slot * 32;
        System.arraycopy(mintResult1, srcPos, frontier, destPos, 32);
      }
      leafCount++;
    }

    { //for burn
      ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
      builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.BURN);
      builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
      builder.setTransparentFromAmount(BigInteger.ZERO);
      builder.setTransparentToAmount(BigInteger.valueOf(40));
      builder.setTransparentToAddress(PUBLIC_TO_ADDRESS);
      //spendNote1
      Note senderNote1 = new Note(senderPaymentAddress1.getD(), senderPaymentAddress1.getPkD(),
          100, rcm1, new byte[512]);
      byte[][] cm1 = new byte[1][32];
      System.arraycopy(senderNote1.cm(), 0, cm1[0], 0, 32);
      IncrementalMerkleVoucherContainer voucher1 = addSimpleMerkleVoucherContainer(tree, cm1);
      byte[] path1 = decodePath(voucher1.path().encode());
      byte[] anchor1 = voucher1.root().getContent().toByteArray();
      long position1 = voucher1.position();
      builder.addSpend(senderExpsk, senderNote1, anchor1, path1, position1);

      //receiveNote1
      SpendingKey receiveSk1 = SpendingKey.random();
      FullViewingKey receiveFvk1 = receiveSk1.fullViewingKey();
      IncomingViewingKey receiveIvk1 = receiveFvk1.inViewingKey();
      PaymentAddress receivePaymentAddress1 = receiveIvk1.address(new DiversifierT()).get();
      builder.addOutput(senderOvk, receivePaymentAddress1, 50, new byte[512]);

      ShieldedTRC20Parameters params = builder.build(true);

      byte[] inputData = abiEncodeForTransfer(params, frontier, leafCount, 40);
      Pair<Boolean, byte[]> contractResult = verifyTransfer(inputData);
      byte[] result = contractResult.getRight();
      Assert.assertEquals(0, result[31]);
    }

  }

  @Test
  public void merkleHashWrongInput() {
    long[] levelList = {-1, 64, (1L << 32)};

    for (long level : levelList) {
      byte[] left = Wallet.generateRandomBytes(32);
      byte[] right = Wallet.generateRandomBytes(32);
      byte[] input = ByteUtil.merge(longTo32Bytes(level), left, right);
      boolean result = merkleHash.execute(input).getLeft();

      Assert.assertFalse(result);
    }
  }

  @Test
  public void merkleHashWrongInputLengthIncrease() {
    long[] levelList = {-1, 64, (1L << 32)};

    for (long level : levelList) {
      byte[] left = Wallet.generateRandomBytes(32);
      byte[] right = Wallet.generateRandomBytes(32);
      byte[] input = ByteUtil.merge(longTo32Bytes(level), left, right, new byte[10]);
      boolean result = merkleHash.execute(input).getLeft();

      Assert.assertFalse(result);
    }
  }

  @Test
  public void merkleHashWrongInputLengthReduce() {
    long[] levelList = {-1, 64, (1L << 32)};

    for (long level : levelList) {
      byte[] left = Wallet.generateRandomBytes(31);
      byte[] right = Wallet.generateRandomBytes(31);
      byte[] input = ByteUtil.merge(longTo32Bytes(level), left, right);
      boolean result = merkleHash.execute(input).getLeft();

      Assert.assertFalse(result);
    }
  }

  @Test
  public void verifyMintWrongDataLength() throws ZksnarkException {
    long leafCount = 0;
    long value = 100L;
    byte[] frontier = new byte[32 * 33];

    ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
    builder.setTransparentFromAmount(BigInteger.valueOf(value));
    builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
    builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);

    //ReceiveNote
    SpendingKey recvSk = SpendingKey.random();
    FullViewingKey fullViewingKey = recvSk.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
    builder.addOutput(DEFAULT_OVK, paymentAddress, value, new byte[512]);
    ShieldedTRC20Parameters params = builder.build(false);

    byte[] inputData = abiEncodeForMint(params, value, frontier, leafCount);
    byte[] mergedBytes = ByteUtil.merge(inputData, new byte[1]);
    Pair<Boolean, byte[]> contractResult = mintContract.execute(mergedBytes);
    byte[] result = contractResult.getRight();

    Assert.assertEquals(0, result[31]);
  }

  @Test
  public void verifyTransferWrongDataLength() throws ZksnarkException {
    long leafCount = 0;
    byte[] frontier = new byte[32 * 33];

    SpendingKey senderSk = SpendingKey.random();
    ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
    FullViewingKey senderFvk = senderSk.fullViewingKey();
    byte[] senderOvk = senderFvk.getOvk();
    IncomingViewingKey senderIvk = senderFvk.inViewingKey();
    byte[] rcm1 = new byte[32];
    JLibrustzcash.librustzcashSaplingGenerateR(rcm1);
    byte[] rcm2 = new byte[32];
    JLibrustzcash.librustzcashSaplingGenerateR(rcm2);
    PaymentAddress senderPaymentAddress1 = senderIvk.address(DiversifierT.random()).get();
    PaymentAddress senderPaymentAddress2 = senderIvk.address(DiversifierT.random()).get();

    ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
    builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.TRANSFER);
    builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
    builder.setTransparentFromAmount(BigInteger.ZERO);
    builder.setTransparentToAmount(BigInteger.ZERO);

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    //spendNote1
    Note senderNote1 = new Note(senderPaymentAddress1.getD(), senderPaymentAddress1.getPkD(),
        30, rcm1, new byte[512]);
    byte[][] cm1 = new byte[1][32];
    System.arraycopy(senderNote1.cm(), 0, cm1[0], 0, 32);
    IncrementalMerkleVoucherContainer voucher1 = addSimpleMerkleVoucherContainer(tree, cm1);
    byte[] path1 = decodePath(voucher1.path().encode());
    byte[] anchor1 = voucher1.root().getContent().toByteArray();
    long position1 = voucher1.position();
    builder.addSpend(senderExpsk, senderNote1, anchor1, path1, position1);

    //spendNote2
    Note senderNote2 = new Note(senderPaymentAddress2.getD(), senderPaymentAddress2.getPkD(),
        70, rcm2, new byte[512]);
    byte[][] cm2 = new byte[1][32];
    System.arraycopy(senderNote2.cm(), 0, cm2[0], 0, 32);
    IncrementalMerkleVoucherContainer voucher2 = addSimpleMerkleVoucherContainer(tree, cm2);
    byte[] path2 = decodePath(voucher2.path().encode());
    byte[] anchor2 = voucher2.root().getContent().toByteArray();
    long position2 = voucher2.position();
    builder.addSpend(senderExpsk, senderNote2, anchor2, path2, position2);

    //receiveNote1
    SpendingKey receiveSk1 = SpendingKey.random();
    FullViewingKey receiveFvk1 = receiveSk1.fullViewingKey();
    IncomingViewingKey receiveIvk1 = receiveFvk1.inViewingKey();
    PaymentAddress receivePaymentAddress1 = receiveIvk1.address(new DiversifierT()).get();
    builder.addOutput(senderOvk, receivePaymentAddress1, 40, new byte[512]);

    //receiveNote2
    SpendingKey receiveSk2 = SpendingKey.random();
    FullViewingKey receiveFvk2 = receiveSk2.fullViewingKey();
    IncomingViewingKey receiveIvk2 = receiveFvk2.inViewingKey();
    PaymentAddress receivePaymentAddress2 = receiveIvk2.address(new DiversifierT()).get();
    builder.addOutput(senderOvk, receivePaymentAddress2, 60, new byte[512]);
    ShieldedTRC20Parameters params = builder.build(true);

    byte[] inputData = abiEncodeForTransfer(params, frontier, leafCount, 0);
    byte[] mergedBytes = ByteUtil.merge(inputData, new byte[1]);
    Pair<Boolean, byte[]> contractResult = verifyTransfer(mergedBytes);
    byte[] result = contractResult.getRight();

    Assert.assertEquals(0, result[31]);
  }

  @Test
  public void verifyBurnWrongDataLength() throws ZksnarkException {
    long value = 100L;
    SpendingKey senderSk = SpendingKey.random();
    ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
    FullViewingKey senderFvk = senderSk.fullViewingKey();
    IncomingViewingKey senderIvk = senderFvk.inViewingKey();
    byte[] rcm = new byte[32];
    JLibrustzcash.librustzcashSaplingGenerateR(rcm);
    PaymentAddress senderPaymentAddress = senderIvk.address(DiversifierT.random()).get();

    ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
    builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.BURN);
    builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
    builder.setTransparentToAmount(BigInteger.valueOf(value));
    builder.setTransparentToAddress(PUBLIC_TO_ADDRESS);
    //spendNote
    Note senderNote = new Note(senderPaymentAddress.getD(), senderPaymentAddress.getPkD(),
        value, rcm, new byte[512]);
    byte[][] cm = new byte[1][32];
    System.arraycopy(senderNote.cm(), 0, cm[0], 0, 32);
    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    IncrementalMerkleVoucherContainer voucher = addSimpleMerkleVoucherContainer(tree, cm);
    byte[] path = decodePath(voucher.path().encode());
    byte[] anchor = voucher.root().getContent().toByteArray();
    long position = voucher.position();
    builder.addSpend(senderExpsk, senderNote, anchor, path, position);
    ShieldedTRC20Parameters params = builder.build(true);

    byte[] inputData = abiEncodeForBurn(params, value);
    byte[] data = ByteUtil.merge(inputData, new byte[1]);
    Pair<Boolean, byte[]> contractResult = burnContract.execute(data);
    byte[] result = contractResult.getRight();

    Assert.assertEquals(0, result[31]);
  }

  @Test
  public void verifyMintWrongLeafcount() throws ZksnarkException {
    long value = 100L;
    byte[] frontier = new byte[32 * 33];
    long[] leafCountList = {-1, 1L << 32};

    for (long leafCount : leafCountList) {
      ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
      builder.setTransparentFromAmount(BigInteger.valueOf(value));
      builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
      builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);

      //ReceiveNote
      SpendingKey recvSk = SpendingKey.random();
      FullViewingKey fullViewingKey = recvSk.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
      builder.addOutput(DEFAULT_OVK, paymentAddress, value, new byte[512]);
      ShieldedTRC20Parameters params = builder.build(false);

      byte[] inputData = abiEncodeForMint(params, value, frontier, leafCount);
      Pair<Boolean, byte[]> contractResult = mintContract.execute(inputData);
      byte[] result = contractResult.getRight();

      Assert.assertEquals(0, result[31]);
    }
  }

  @Test
  public void verifyTransferWrongLeafcount() throws ZksnarkException {
    byte[] frontier = new byte[32 * 33];
    long[] leafCountList = {-1, (1L << 32) - 1};

    SpendingKey senderSk = SpendingKey.random();
    ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
    FullViewingKey senderFvk = senderSk.fullViewingKey();
    byte[] senderOvk = senderFvk.getOvk();
    IncomingViewingKey senderIvk = senderFvk.inViewingKey();
    byte[] rcm1 = new byte[32];
    JLibrustzcash.librustzcashSaplingGenerateR(rcm1);
    byte[] rcm2 = new byte[32];
    JLibrustzcash.librustzcashSaplingGenerateR(rcm2);
    PaymentAddress senderPaymentAddress1 = senderIvk.address(DiversifierT.random()).get();
    PaymentAddress senderPaymentAddress2 = senderIvk.address(DiversifierT.random()).get();

    for (long leafCount : leafCountList) {
      ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
      builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.TRANSFER);
      builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
      builder.setTransparentFromAmount(BigInteger.ZERO);
      builder.setTransparentToAmount(BigInteger.ZERO);
      IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
          new IncrementalMerkleTreeCapsule());
      //spendNote1
      Note senderNote1 = new Note(senderPaymentAddress1.getD(), senderPaymentAddress1.getPkD(),
          30, rcm1, new byte[512]);
      byte[][] cm1 = new byte[1][32];
      System.arraycopy(senderNote1.cm(), 0, cm1[0], 0, 32);
      IncrementalMerkleVoucherContainer voucher1 = addSimpleMerkleVoucherContainer(tree, cm1);
      byte[] path1 = decodePath(voucher1.path().encode());
      byte[] anchor1 = voucher1.root().getContent().toByteArray();
      long position1 = voucher1.position();
      builder.addSpend(senderExpsk, senderNote1, anchor1, path1, position1);

      //spendNote2
      Note senderNote2 = new Note(senderPaymentAddress2.getD(), senderPaymentAddress2.getPkD(),
          70, rcm2, new byte[512]);
      byte[][] cm2 = new byte[1][32];
      System.arraycopy(senderNote2.cm(), 0, cm2[0], 0, 32);
      IncrementalMerkleVoucherContainer voucher2 = addSimpleMerkleVoucherContainer(tree, cm2);
      byte[] path2 = decodePath(voucher2.path().encode());
      byte[] anchor2 = voucher2.root().getContent().toByteArray();
      long position2 = voucher2.position();
      builder.addSpend(senderExpsk, senderNote2, anchor2, path2, position2);

      //receiveNote1
      SpendingKey receiveSk1 = SpendingKey.random();
      FullViewingKey receiveFvk1 = receiveSk1.fullViewingKey();
      IncomingViewingKey receiveIvk1 = receiveFvk1.inViewingKey();
      PaymentAddress receivePaymentAddress1 = receiveIvk1.address(new DiversifierT()).get();
      builder.addOutput(senderOvk, receivePaymentAddress1, 40, new byte[512]);

      //receiveNote2
      SpendingKey receiveSk2 = SpendingKey.random();
      FullViewingKey receiveFvk2 = receiveSk2.fullViewingKey();
      IncomingViewingKey receiveIvk2 = receiveFvk2.inViewingKey();
      PaymentAddress receivePaymentAddress2 = receiveIvk2.address(new DiversifierT()).get();
      builder.addOutput(senderOvk, receivePaymentAddress2, 60, new byte[512]);
      ShieldedTRC20Parameters params = builder.build(true);

      byte[] inputData = abiEncodeForTransfer(params, frontier, leafCount, 0);
      Pair<Boolean, byte[]> contractResult = verifyTransfer(inputData);
      byte[] result = contractResult.getRight();

      Assert.assertEquals(0, result[31]);
    }
  }

  @Test
  public void verifyTransferDuplicateNf() throws ZksnarkException {
    byte[] frontier = new byte[32 * 33];
    long leafCount = 0;

    SpendingKey senderSk = SpendingKey.random();
    ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
    FullViewingKey senderFvk = senderSk.fullViewingKey();
    byte[] senderOvk = senderFvk.getOvk();
    IncomingViewingKey senderIvk = senderFvk.inViewingKey();
    byte[] rcm = new byte[32];
    JLibrustzcash.librustzcashSaplingGenerateR(rcm);
    PaymentAddress senderPaymentAddress = senderIvk.address(DiversifierT.random()).get();

    ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
    builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.TRANSFER);
    builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
    builder.setTransparentFromAmount(BigInteger.ZERO);
    builder.setTransparentToAmount(BigInteger.ZERO);

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    //spendNote1
    Note senderNote = new Note(senderPaymentAddress.getD(), senderPaymentAddress.getPkD(),
        50, rcm, new byte[512]);
    byte[][] cm = new byte[1][32];
    System.arraycopy(senderNote.cm(), 0, cm[0], 0, 32);
    IncrementalMerkleVoucherContainer voucher = addSimpleMerkleVoucherContainer(tree, cm);
    byte[] path = decodePath(voucher.path().encode());
    byte[] anchor = voucher.root().getContent().toByteArray();
    long position = voucher.position();
    builder.addSpend(senderExpsk, senderNote, anchor, path, position);
    builder.addSpend(senderExpsk, senderNote, anchor, path, position);

    //receiveNote1
    SpendingKey receiveSk1 = SpendingKey.random();
    FullViewingKey receiveFvk1 = receiveSk1.fullViewingKey();
    IncomingViewingKey receiveIvk1 = receiveFvk1.inViewingKey();
    PaymentAddress receivePaymentAddress1 = receiveIvk1.address(new DiversifierT()).get();
    builder.addOutput(senderOvk, receivePaymentAddress1, 40, new byte[512]);

    //receiveNote2
    SpendingKey receiveSk2 = SpendingKey.random();
    FullViewingKey receiveFvk2 = receiveSk2.fullViewingKey();
    IncomingViewingKey receiveIvk2 = receiveFvk2.inViewingKey();
    PaymentAddress receivePaymentAddress2 = receiveIvk2.address(new DiversifierT()).get();
    builder.addOutput(senderOvk, receivePaymentAddress2, 60, new byte[512]);
    ShieldedTRC20Parameters params = builder.build(true);

    byte[] inputData = abiEncodeForTransfer(params, frontier, leafCount, 0);
    Pair<Boolean, byte[]> contractResult = verifyTransfer(inputData);
    byte[] result = contractResult.getRight();

    Assert.assertEquals(0, result[31]);
  }

  @Test
  public void verifyTransferDuplicateReceiveNotes() throws ZksnarkException {
    byte[] frontier = new byte[32 * 33];
    long leafCount = 0;

    SpendingKey senderSk = SpendingKey.random();
    ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
    FullViewingKey senderFvk = senderSk.fullViewingKey();
    byte[] senderOvk = senderFvk.getOvk();
    IncomingViewingKey senderIvk = senderFvk.inViewingKey();
    byte[] rcm1 = new byte[32];
    JLibrustzcash.librustzcashSaplingGenerateR(rcm1);
    byte[] rcm2 = new byte[32];
    JLibrustzcash.librustzcashSaplingGenerateR(rcm2);
    PaymentAddress senderPaymentAddress1 = senderIvk.address(DiversifierT.random()).get();
    PaymentAddress senderPaymentAddress2 = senderIvk.address(DiversifierT.random()).get();

    ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
    builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.TRANSFER);
    builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
    builder.setTransparentFromAmount(BigInteger.ZERO);
    builder.setTransparentToAmount(BigInteger.ZERO);

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    //spendNote1
    Note senderNote1 = new Note(senderPaymentAddress1.getD(), senderPaymentAddress1.getPkD(),
        30, rcm1, new byte[512]);
    byte[][] cm1 = new byte[1][32];
    System.arraycopy(senderNote1.cm(), 0, cm1[0], 0, 32);
    IncrementalMerkleVoucherContainer voucher1 = addSimpleMerkleVoucherContainer(tree, cm1);
    byte[] path1 = decodePath(voucher1.path().encode());
    byte[] anchor1 = voucher1.root().getContent().toByteArray();
    long position1 = voucher1.position();
    builder.addSpend(senderExpsk, senderNote1, anchor1, path1, position1);

    //spendNote2
    Note senderNote2 = new Note(senderPaymentAddress2.getD(), senderPaymentAddress2.getPkD(),
        70, rcm2, new byte[512]);
    byte[][] cm2 = new byte[1][32];
    System.arraycopy(senderNote2.cm(), 0, cm2[0], 0, 32);
    IncrementalMerkleVoucherContainer voucher2 = addSimpleMerkleVoucherContainer(tree, cm2);
    byte[] path2 = decodePath(voucher2.path().encode());
    byte[] anchor2 = voucher2.root().getContent().toByteArray();
    long position2 = voucher2.position();
    builder.addSpend(senderExpsk, senderNote2, anchor2, path2, position2);

    //receiveNote1
    SpendingKey receiveSk = SpendingKey.random();
    FullViewingKey receiveFvk = receiveSk.fullViewingKey();
    IncomingViewingKey receiveIvk = receiveFvk.inViewingKey();
    PaymentAddress receivePaymentAddress = receiveIvk.address(new DiversifierT()).get();
    byte[] r = new byte[32];
    JLibrustzcash.librustzcashSaplingGenerateR(r);
    builder.addOutput(senderOvk, receivePaymentAddress.getD(), receivePaymentAddress.getPkD(),
        50, r, new byte[512]);
    builder.addOutput(senderOvk, receivePaymentAddress.getD(), receivePaymentAddress.getPkD(),
        50, r, new byte[512]);
    ShieldedTRC20Parameters params = builder.build(true);

    byte[] inputData = abiEncodeForTransfer(params, frontier, leafCount, 0);
    Pair<Boolean, byte[]> contractResult = verifyTransfer(inputData);
    byte[] result = contractResult.getRight();

    Assert.assertEquals(0, result[31]);
  }

  @Test
  public void verifyMintWrongValue() throws ZksnarkException {
    long leafCount = 0;
    byte[] frontier = new byte[32 * 33];
    long[] negativeValueList = {100, 1000};

    for (long value : negativeValueList) {
      ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
      builder.setTransparentFromAmount(BigInteger.valueOf(value));
      builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
      builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);

      //ReceiveNote
      SpendingKey recvSk = SpendingKey.random();
      FullViewingKey fullViewingKey = recvSk.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
      builder.addOutput(DEFAULT_OVK, paymentAddress, 50, new byte[512]);
      ShieldedTRC20Parameters params = builder.build(false);

      byte[] inputData = abiEncodeForMint(params, value, frontier, leafCount);
      Pair<Boolean, byte[]> contractResult = mintContract.execute(inputData);
      byte[] result = contractResult.getRight();

      Assert.assertEquals(0, result[31]);
    }
  }

  @Test
  public void verifyBurnWrongValue() throws ZksnarkException {
    long value = 100L;
    SpendingKey senderSk = SpendingKey.random();
    ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
    FullViewingKey senderFvk = senderSk.fullViewingKey();
    byte[] senderOvk = senderFvk.getOvk();
    IncomingViewingKey senderIvk = senderFvk.inViewingKey();
    byte[] rcm = new byte[32];
    JLibrustzcash.librustzcashSaplingGenerateR(rcm);
    PaymentAddress senderPaymentAddress = senderIvk.address(DiversifierT.random()).get();

    ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
    builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.BURN);
    builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
    builder.setTransparentToAmount(BigInteger.valueOf(value));
    builder.setTransparentToAddress(PUBLIC_TO_ADDRESS);
    //spendNote
    Note senderNote = new Note(senderPaymentAddress.getD(), senderPaymentAddress.getPkD(),
        50, rcm, new byte[512]);
    byte[][] cm = new byte[1][32];
    System.arraycopy(senderNote.cm(), 0, cm[0], 0, 32);
    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    IncrementalMerkleVoucherContainer voucher = addSimpleMerkleVoucherContainer(tree, cm);
    byte[] path = decodePath(voucher.path().encode());
    byte[] anchor = voucher.root().getContent().toByteArray();
    long position = voucher.position();
    builder.addSpend(senderExpsk, senderNote, anchor, path, position);
    ShieldedTRC20Parameters params = builder.build(true);

    byte[] inputData = abiEncodeForBurn(params, value);
    Pair<Boolean, byte[]> contractResult = burnContract.execute(inputData);
    byte[] result = contractResult.getRight();

    Assert.assertEquals(0, result[31]);
  }

  @Test
  public void verifyMintProofWrongCM() throws ZksnarkException {
    int totalCountNum = 1;
    long leafCount = 0;
    long value = 100L;
    byte[] frontier = new byte[32 * 33];

    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
      builder.setTransparentFromAmount(BigInteger.valueOf(value));
      builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
      builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);

      //ReceiveNote
      SpendingKey recvSk = SpendingKey.random();
      FullViewingKey fullViewingKey = recvSk.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
      builder.addOutput(DEFAULT_OVK, paymentAddress, value, new byte[512]);
      ShieldedTRC20Parameters params = builder.build(false);

      byte[] inputData = abiEncodeForMintWrongCM(params, value, frontier, leafCount);
      Pair<Boolean, byte[]> contractResult = mintContract.execute(inputData);
      byte[] result = contractResult.getRight();

      Assert.assertEquals(0, result[31]);
    }
  }


  @Test
  public void verifyMintProofWrongCV() throws ZksnarkException {
    int totalCountNum = 1;
    long leafCount = 0;
    long value = 100L;
    byte[] frontier = new byte[32 * 33];

    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
      builder.setTransparentFromAmount(BigInteger.valueOf(value));
      builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
      builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);

      //ReceiveNote
      SpendingKey recvSk = SpendingKey.random();
      FullViewingKey fullViewingKey = recvSk.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
      builder.addOutput(DEFAULT_OVK, paymentAddress, value, new byte[512]);
      ShieldedTRC20Parameters params = builder.build(false);

      byte[] inputData = abiEncodeForMintWrongCV(params, value, frontier, leafCount);
      Pair<Boolean, byte[]> contractResult = mintContract.execute(inputData);
      byte[] result = contractResult.getRight();

      Assert.assertEquals(0, result[31]);
    }
  }

  @Test
  public void verifyMintProofWrongEpk() throws ZksnarkException {
    int totalCountNum = 1;
    long leafCount = 0;
    long value = 100L;
    byte[] frontier = new byte[32 * 33];

    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
      builder.setTransparentFromAmount(BigInteger.valueOf(value));
      builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
      builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);

      //ReceiveNote
      SpendingKey recvSk = SpendingKey.random();
      FullViewingKey fullViewingKey = recvSk.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
      builder.addOutput(DEFAULT_OVK, paymentAddress, value, new byte[512]);
      ShieldedTRC20Parameters params = builder.build(false);

      byte[] inputData = abiEncodeForMintWrongEpk(params, value, frontier, leafCount);
      Pair<Boolean, byte[]> contractResult = mintContract.execute(inputData);
      byte[] result = contractResult.getRight();

      Assert.assertEquals(0, result[31]);
    }
  }

  @Test
  public void verifyMintProofWrongProof() throws ZksnarkException {
    int totalCountNum = 1;
    long leafCount = 0;
    long value = 100L;
    byte[] frontier = new byte[32 * 33];

    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
      builder.setTransparentFromAmount(BigInteger.valueOf(value));
      builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
      builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);

      //ReceiveNote
      SpendingKey recvSk = SpendingKey.random();
      FullViewingKey fullViewingKey = recvSk.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
      builder.addOutput(DEFAULT_OVK, paymentAddress, value, new byte[512]);
      ShieldedTRC20Parameters params = builder.build(false);

      byte[] inputData = abiEncodeForMintWrongProof(params, value, frontier, leafCount);
      Pair<Boolean, byte[]> contractResult = mintContract.execute(inputData);
      byte[] result = contractResult.getRight();

      Assert.assertEquals(0, result[31]);
    }
  }

  @Test
  public void verifyMintProofWrongBindingSignature() throws ZksnarkException {
    int totalCountNum = 1;
    long leafCount = 0;
    long value = 100L;
    byte[] frontier = new byte[32 * 33];

    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
      builder.setTransparentFromAmount(BigInteger.valueOf(value));
      builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
      builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);

      //ReceiveNote
      SpendingKey recvSk = SpendingKey.random();
      FullViewingKey fullViewingKey = recvSk.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
      builder.addOutput(DEFAULT_OVK, paymentAddress, value, new byte[512]);
      ShieldedTRC20Parameters params = builder.build(false);

      byte[] inputData = abiEncodeForMintWrongProof(params, value, frontier, leafCount);
      Pair<Boolean, byte[]> contractResult = mintContract.execute(inputData);
      byte[] result = contractResult.getRight();

      Assert.assertEquals(0, result[31]);
    }
  }

  @Test
  public void verifyMintProofWrongHash() throws ZksnarkException {
    int totalCountNum = 1;
    long leafCount = 0;
    long value = 100L;
    byte[] frontier = new byte[32 * 33];

    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
      builder.setTransparentFromAmount(BigInteger.valueOf(value));
      builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
      builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);

      //ReceiveNote
      SpendingKey recvSk = SpendingKey.random();
      FullViewingKey fullViewingKey = recvSk.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
      builder.addOutput(DEFAULT_OVK, paymentAddress, value, new byte[512]);
      ShieldedTRC20Parameters params = builder.build(false);

      byte[] inputData = abiEncodeForMintWrongHash(params, value, frontier, leafCount);
      Pair<Boolean, byte[]> contractResult = mintContract.execute(inputData);
      byte[] result = contractResult.getRight();

      Assert.assertEquals(0, result[31]);
    }
  }

  @Test
  public void verifyTransferProofWrongNf() throws ZksnarkException {
    int totalCountNum = 1;
    long leafCount = 0;
    byte[] frontier = new byte[32 * 33];

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      SpendingKey senderSk = SpendingKey.random();
      ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
      FullViewingKey senderFvk = senderSk.fullViewingKey();
      byte[] senderOvk = senderFvk.getOvk();
      IncomingViewingKey senderIvk = senderFvk.inViewingKey();
      byte[] rcm1 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm1);
      byte[] rcm2 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm2);
      PaymentAddress senderPaymentAddress1 = senderIvk.address(DiversifierT.random()).get();
      PaymentAddress senderPaymentAddress2 = senderIvk.address(DiversifierT.random()).get();

      { //for mint1
        ShieldedTRC20ParametersBuilder mintBuilder1 = new ShieldedTRC20ParametersBuilder();
        mintBuilder1.setTransparentFromAmount(BigInteger.valueOf(30));
        mintBuilder1.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder1.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder1.addOutput(DEFAULT_OVK, senderPaymentAddress1.getD(),
            senderPaymentAddress1.getPkD(), 30, rcm1, new byte[512]);
        ShieldedTRC20Parameters mintParams1 = mintBuilder1.build(false);

        byte[] mintInputData1 = abiEncodeForMint(mintParams1, 30, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult1 = mintContract.execute(mintInputData1);
        byte[] mintResult1 = mintContractResult1.getRight();
        Assert.assertEquals(1, mintResult1[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult1[32] == 0) {
          System.arraycopy(mintInputData1, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult1[32] - 1) * 32 + 33;
          int destPos = mintResult1[32] * 32;
          System.arraycopy(mintResult1, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for mint2
        ShieldedTRC20ParametersBuilder mintBuilder2 = new ShieldedTRC20ParametersBuilder();
        mintBuilder2.setTransparentFromAmount(BigInteger.valueOf(70));
        mintBuilder2.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder2.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder2.addOutput(DEFAULT_OVK, senderPaymentAddress2.getD(),
            senderPaymentAddress2.getPkD(), 70, rcm2, new byte[512]);
        ShieldedTRC20Parameters mintParams2 = mintBuilder2.build(false);

        byte[] mintInputData2 = abiEncodeForMint(mintParams2, 70, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult2 = mintContract.execute(mintInputData2);
        byte[] mintResult2 = mintContractResult2.getRight();

        Assert.assertEquals(1, mintResult2[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult2[32] == 0) {
          System.arraycopy(mintInputData2, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult2[32] - 1) * 32 + 33;
          int destPos = mintResult2[32] * 32;
          System.arraycopy(mintResult2, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for transfer
        ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
        builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.TRANSFER);
        builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        builder.setTransparentFromAmount(BigInteger.ZERO);
        builder.setTransparentToAmount(BigInteger.ZERO);
        //spendNote1
        Note senderNote1 = new Note(senderPaymentAddress1.getD(), senderPaymentAddress1.getPkD(),
            30, rcm1, new byte[512]);
        byte[][] cm1 = new byte[1][32];
        System.arraycopy(senderNote1.cm(), 0, cm1[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher1 = addSimpleMerkleVoucherContainer(tree, cm1);
        byte[] path1 = decodePath(voucher1.path().encode());
        byte[] anchor1 = voucher1.root().getContent().toByteArray();
        long position1 = voucher1.position();
        builder.addSpend(senderExpsk, senderNote1, anchor1, path1, position1);

        //spendNote2
        Note senderNote2 = new Note(senderPaymentAddress2.getD(), senderPaymentAddress2.getPkD(),
            70, rcm2, new byte[512]);
        byte[][] cm2 = new byte[1][32];
        System.arraycopy(senderNote2.cm(), 0, cm2[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher2 = addSimpleMerkleVoucherContainer(tree, cm2);
        byte[] path2 = decodePath(voucher2.path().encode());
        byte[] anchor2 = voucher2.root().getContent().toByteArray();
        long position2 = voucher2.position();
        builder.addSpend(senderExpsk, senderNote2, anchor2, path2, position2);

        //receiveNote1
        SpendingKey receiveSk1 = SpendingKey.random();
        FullViewingKey receiveFvk1 = receiveSk1.fullViewingKey();
        IncomingViewingKey receiveIvk1 = receiveFvk1.inViewingKey();
        PaymentAddress receivePaymentAddress1 = receiveIvk1.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress1, 40, new byte[512]);

        //receiveNote2
        SpendingKey receiveSk2 = SpendingKey.random();
        FullViewingKey receiveFvk2 = receiveSk2.fullViewingKey();
        IncomingViewingKey receiveIvk2 = receiveFvk2.inViewingKey();
        PaymentAddress receivePaymentAddress2 = receiveIvk2.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress2, 60, new byte[512]);

        ShieldedTRC20Parameters params = builder.build(true);

        byte[] inputData = abiEncodeForTransferWrongNf(params, frontier, leafCount);
        Pair<Boolean, byte[]> contractResult = verifyTransfer(inputData);
        byte[] result = contractResult.getRight();
        Assert.assertEquals(0, result[31]);
      }
    }
  }

  @Test
  public void verifyTransferProofWrongRoot() throws ZksnarkException {
    int totalCountNum = 1;
    long leafCount = 0;
    byte[] frontier = new byte[32 * 33];

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      SpendingKey senderSk = SpendingKey.random();
      ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
      FullViewingKey senderFvk = senderSk.fullViewingKey();
      byte[] senderOvk = senderFvk.getOvk();
      IncomingViewingKey senderIvk = senderFvk.inViewingKey();
      byte[] rcm1 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm1);
      byte[] rcm2 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm2);
      PaymentAddress senderPaymentAddress1 = senderIvk.address(DiversifierT.random()).get();
      PaymentAddress senderPaymentAddress2 = senderIvk.address(DiversifierT.random()).get();

      { //for mint1
        ShieldedTRC20ParametersBuilder mintBuilder1 = new ShieldedTRC20ParametersBuilder();
        mintBuilder1.setTransparentFromAmount(BigInteger.valueOf(30));
        mintBuilder1.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder1.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder1.addOutput(DEFAULT_OVK, senderPaymentAddress1.getD(),
            senderPaymentAddress1.getPkD(), 30, rcm1, new byte[512]);
        ShieldedTRC20Parameters mintParams1 = mintBuilder1.build(false);

        byte[] mintInputData1 = abiEncodeForMint(mintParams1, 30, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult1 = mintContract.execute(mintInputData1);
        byte[] mintResult1 = mintContractResult1.getRight();
        Assert.assertEquals(1, mintResult1[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult1[32] == 0) {
          System.arraycopy(mintInputData1, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult1[32] - 1) * 32 + 33;
          int destPos = mintResult1[32] * 32;
          System.arraycopy(mintResult1, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for mint2
        ShieldedTRC20ParametersBuilder mintBuilder2 = new ShieldedTRC20ParametersBuilder();
        mintBuilder2.setTransparentFromAmount(BigInteger.valueOf(70));
        mintBuilder2.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder2.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder2.addOutput(DEFAULT_OVK, senderPaymentAddress2.getD(),
            senderPaymentAddress2.getPkD(), 70, rcm2, new byte[512]);
        ShieldedTRC20Parameters mintParams2 = mintBuilder2.build(false);

        byte[] mintInputData2 = abiEncodeForMint(mintParams2, 70, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult2 = mintContract.execute(mintInputData2);
        byte[] mintResult2 = mintContractResult2.getRight();

        Assert.assertEquals(1, mintResult2[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult2[32] == 0) {
          System.arraycopy(mintInputData2, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult2[32] - 1) * 32 + 33;
          int destPos = mintResult2[32] * 32;
          System.arraycopy(mintResult2, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for transfer
        ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
        builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.TRANSFER);
        builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        builder.setTransparentFromAmount(BigInteger.ZERO);
        builder.setTransparentToAmount(BigInteger.ZERO);
        //spendNote1
        Note senderNote1 = new Note(senderPaymentAddress1.getD(), senderPaymentAddress1.getPkD(),
            30, rcm1, new byte[512]);
        byte[][] cm1 = new byte[1][32];
        System.arraycopy(senderNote1.cm(), 0, cm1[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher1 = addSimpleMerkleVoucherContainer(tree, cm1);
        byte[] path1 = decodePath(voucher1.path().encode());
        byte[] anchor1 = voucher1.root().getContent().toByteArray();
        long position1 = voucher1.position();
        builder.addSpend(senderExpsk, senderNote1, anchor1, path1, position1);

        //spendNote2
        Note senderNote2 = new Note(senderPaymentAddress2.getD(), senderPaymentAddress2.getPkD(),
            70, rcm2, new byte[512]);
        byte[][] cm2 = new byte[1][32];
        System.arraycopy(senderNote2.cm(), 0, cm2[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher2 = addSimpleMerkleVoucherContainer(tree, cm2);
        byte[] path2 = decodePath(voucher2.path().encode());
        byte[] anchor2 = voucher2.root().getContent().toByteArray();
        long position2 = voucher2.position();
        builder.addSpend(senderExpsk, senderNote2, anchor2, path2, position2);

        //receiveNote1
        SpendingKey receiveSk1 = SpendingKey.random();
        FullViewingKey receiveFvk1 = receiveSk1.fullViewingKey();
        IncomingViewingKey receiveIvk1 = receiveFvk1.inViewingKey();
        PaymentAddress receivePaymentAddress1 = receiveIvk1.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress1, 40, new byte[512]);

        //receiveNote2
        SpendingKey receiveSk2 = SpendingKey.random();
        FullViewingKey receiveFvk2 = receiveSk2.fullViewingKey();
        IncomingViewingKey receiveIvk2 = receiveFvk2.inViewingKey();
        PaymentAddress receivePaymentAddress2 = receiveIvk2.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress2, 60, new byte[512]);

        ShieldedTRC20Parameters params = builder.build(true);

        byte[] inputData = abiEncodeForTransferWrongRoot(params, frontier, leafCount);
        Pair<Boolean, byte[]> contractResult = verifyTransfer(inputData);
        byte[] result = contractResult.getRight();
        Assert.assertEquals(0, result[31]);

      }
    }
  }

  @Test
  public void verifyTransferProofWrongSpendCV() throws ZksnarkException {
    int totalCountNum = 1;
    long leafCount = 0;
    byte[] frontier = new byte[32 * 33];

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      SpendingKey senderSk = SpendingKey.random();
      ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
      FullViewingKey senderFvk = senderSk.fullViewingKey();
      byte[] senderOvk = senderFvk.getOvk();
      IncomingViewingKey senderIvk = senderFvk.inViewingKey();
      byte[] rcm1 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm1);
      byte[] rcm2 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm2);
      PaymentAddress senderPaymentAddress1 = senderIvk.address(DiversifierT.random()).get();
      PaymentAddress senderPaymentAddress2 = senderIvk.address(DiversifierT.random()).get();

      { //for mint1
        ShieldedTRC20ParametersBuilder mintBuilder1 = new ShieldedTRC20ParametersBuilder();
        mintBuilder1.setTransparentFromAmount(BigInteger.valueOf(30));
        mintBuilder1.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder1.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder1.addOutput(DEFAULT_OVK, senderPaymentAddress1.getD(),
            senderPaymentAddress1.getPkD(), 30, rcm1, new byte[512]);
        ShieldedTRC20Parameters mintParams1 = mintBuilder1.build(false);

        byte[] mintInputData1 = abiEncodeForMint(mintParams1, 30, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult1 = mintContract.execute(mintInputData1);
        byte[] mintResult1 = mintContractResult1.getRight();
        Assert.assertEquals(1, mintResult1[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult1[32] == 0) {
          System.arraycopy(mintInputData1, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult1[32] - 1) * 32 + 33;
          int destPos = mintResult1[32] * 32;
          System.arraycopy(mintResult1, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for mint2
        ShieldedTRC20ParametersBuilder mintBuilder2 = new ShieldedTRC20ParametersBuilder();
        mintBuilder2.setTransparentFromAmount(BigInteger.valueOf(70));
        mintBuilder2.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder2.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder2.addOutput(DEFAULT_OVK, senderPaymentAddress2.getD(),
            senderPaymentAddress2.getPkD(), 70, rcm2, new byte[512]);
        ShieldedTRC20Parameters mintParams2 = mintBuilder2.build(false);

        byte[] mintInputData2 = abiEncodeForMint(mintParams2, 70, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult2 = mintContract.execute(mintInputData2);
        byte[] mintResult2 = mintContractResult2.getRight();

        Assert.assertEquals(1, mintResult2[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult2[32] == 0) {
          System.arraycopy(mintInputData2, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult2[32] - 1) * 32 + 33;
          int destPos = mintResult2[32] * 32;
          System.arraycopy(mintResult2, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for transfer
        ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
        builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.TRANSFER);
        builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        builder.setTransparentFromAmount(BigInteger.ZERO);
        builder.setTransparentToAmount(BigInteger.ZERO);
        //spendNote1
        Note senderNote1 = new Note(senderPaymentAddress1.getD(), senderPaymentAddress1.getPkD(),
            30, rcm1, new byte[512]);
        byte[][] cm1 = new byte[1][32];
        System.arraycopy(senderNote1.cm(), 0, cm1[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher1 = addSimpleMerkleVoucherContainer(tree, cm1);
        byte[] path1 = decodePath(voucher1.path().encode());
        byte[] anchor1 = voucher1.root().getContent().toByteArray();
        long position1 = voucher1.position();
        builder.addSpend(senderExpsk, senderNote1, anchor1, path1, position1);

        //spendNote2
        Note senderNote2 = new Note(senderPaymentAddress2.getD(), senderPaymentAddress2.getPkD(),
            70, rcm2, new byte[512]);
        byte[][] cm2 = new byte[1][32];
        System.arraycopy(senderNote2.cm(), 0, cm2[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher2 = addSimpleMerkleVoucherContainer(tree, cm2);
        byte[] path2 = decodePath(voucher2.path().encode());
        byte[] anchor2 = voucher2.root().getContent().toByteArray();
        long position2 = voucher2.position();
        builder.addSpend(senderExpsk, senderNote2, anchor2, path2, position2);

        //receiveNote1
        SpendingKey receiveSk1 = SpendingKey.random();
        FullViewingKey receiveFvk1 = receiveSk1.fullViewingKey();
        IncomingViewingKey receiveIvk1 = receiveFvk1.inViewingKey();
        PaymentAddress receivePaymentAddress1 = receiveIvk1.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress1, 40, new byte[512]);

        //receiveNote2
        SpendingKey receiveSk2 = SpendingKey.random();
        FullViewingKey receiveFvk2 = receiveSk2.fullViewingKey();
        IncomingViewingKey receiveIvk2 = receiveFvk2.inViewingKey();
        PaymentAddress receivePaymentAddress2 = receiveIvk2.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress2, 60, new byte[512]);

        ShieldedTRC20Parameters params = builder.build(true);

        byte[] inputData = abiEncodeForTransferWrongSpendCV(params, frontier, leafCount);
        Pair<Boolean, byte[]> contractResult = verifyTransfer(inputData);
        byte[] result = contractResult.getRight();
        Assert.assertEquals(0, result[31]);
      }
    }
  }

  @Test
  public void verifyTransferProofWrongRk() throws ZksnarkException {
    int totalCountNum = 1;
    long leafCount = 0;
    byte[] frontier = new byte[32 * 33];

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      SpendingKey senderSk = SpendingKey.random();
      ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
      FullViewingKey senderFvk = senderSk.fullViewingKey();
      byte[] senderOvk = senderFvk.getOvk();
      IncomingViewingKey senderIvk = senderFvk.inViewingKey();
      byte[] rcm1 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm1);
      byte[] rcm2 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm2);
      PaymentAddress senderPaymentAddress1 = senderIvk.address(DiversifierT.random()).get();
      PaymentAddress senderPaymentAddress2 = senderIvk.address(DiversifierT.random()).get();

      { //for mint1
        ShieldedTRC20ParametersBuilder mintBuilder1 = new ShieldedTRC20ParametersBuilder();
        mintBuilder1.setTransparentFromAmount(BigInteger.valueOf(30));
        mintBuilder1.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder1.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder1.addOutput(DEFAULT_OVK, senderPaymentAddress1.getD(),
            senderPaymentAddress1.getPkD(), 30, rcm1, new byte[512]);
        ShieldedTRC20Parameters mintParams1 = mintBuilder1.build(false);

        byte[] mintInputData1 = abiEncodeForMint(mintParams1, 30, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult1 = mintContract.execute(mintInputData1);
        byte[] mintResult1 = mintContractResult1.getRight();
        Assert.assertEquals(1, mintResult1[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult1[32] == 0) {
          System.arraycopy(mintInputData1, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult1[32] - 1) * 32 + 33;
          int destPos = mintResult1[32] * 32;
          System.arraycopy(mintResult1, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for mint2
        ShieldedTRC20ParametersBuilder mintBuilder2 = new ShieldedTRC20ParametersBuilder();
        mintBuilder2.setTransparentFromAmount(BigInteger.valueOf(70));
        mintBuilder2.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder2.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder2.addOutput(DEFAULT_OVK, senderPaymentAddress2.getD(),
            senderPaymentAddress2.getPkD(), 70, rcm2, new byte[512]);
        ShieldedTRC20Parameters mintParams2 = mintBuilder2.build(false);

        byte[] mintInputData2 = abiEncodeForMint(mintParams2, 70, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult2 = mintContract.execute(mintInputData2);
        byte[] mintResult2 = mintContractResult2.getRight();

        Assert.assertEquals(1, mintResult2[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult2[32] == 0) {
          System.arraycopy(mintInputData2, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult2[32] - 1) * 32 + 33;
          int destPos = mintResult2[32] * 32;
          System.arraycopy(mintResult2, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for transfer
        ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
        builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.TRANSFER);
        builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        builder.setTransparentFromAmount(BigInteger.ZERO);
        builder.setTransparentToAmount(BigInteger.ZERO);
        //spendNote1
        Note senderNote1 = new Note(senderPaymentAddress1.getD(), senderPaymentAddress1.getPkD(),
            30, rcm1, new byte[512]);
        byte[][] cm1 = new byte[1][32];
        System.arraycopy(senderNote1.cm(), 0, cm1[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher1 = addSimpleMerkleVoucherContainer(tree, cm1);
        byte[] path1 = decodePath(voucher1.path().encode());
        byte[] anchor1 = voucher1.root().getContent().toByteArray();
        long position1 = voucher1.position();
        builder.addSpend(senderExpsk, senderNote1, anchor1, path1, position1);

        //spendNote2
        Note senderNote2 = new Note(senderPaymentAddress2.getD(), senderPaymentAddress2.getPkD(),
            70, rcm2, new byte[512]);
        byte[][] cm2 = new byte[1][32];
        System.arraycopy(senderNote2.cm(), 0, cm2[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher2 = addSimpleMerkleVoucherContainer(tree, cm2);
        byte[] path2 = decodePath(voucher2.path().encode());
        byte[] anchor2 = voucher2.root().getContent().toByteArray();
        long position2 = voucher2.position();
        builder.addSpend(senderExpsk, senderNote2, anchor2, path2, position2);

        //receiveNote1
        SpendingKey receiveSk1 = SpendingKey.random();
        FullViewingKey receiveFvk1 = receiveSk1.fullViewingKey();
        IncomingViewingKey receiveIvk1 = receiveFvk1.inViewingKey();
        PaymentAddress receivePaymentAddress1 = receiveIvk1.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress1, 40, new byte[512]);

        //receiveNote2
        SpendingKey receiveSk2 = SpendingKey.random();
        FullViewingKey receiveFvk2 = receiveSk2.fullViewingKey();
        IncomingViewingKey receiveIvk2 = receiveFvk2.inViewingKey();
        PaymentAddress receivePaymentAddress2 = receiveIvk2.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress2, 60, new byte[512]);

        ShieldedTRC20Parameters params = builder.build(true);

        byte[] inputData = abiEncodeForTransferWrongRk(params, frontier, leafCount);
        Pair<Boolean, byte[]> contractResult = verifyTransfer(inputData);
        byte[] result = contractResult.getRight();
        Assert.assertEquals(0, result[31]);
      }
    }
  }

  @Test
  public void verifyTransferProofWrongSpendProof() throws ZksnarkException {
    int totalCountNum = 1;
    long leafCount = 0;
    byte[] frontier = new byte[32 * 33];

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      SpendingKey senderSk = SpendingKey.random();
      ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
      FullViewingKey senderFvk = senderSk.fullViewingKey();
      byte[] senderOvk = senderFvk.getOvk();
      IncomingViewingKey senderIvk = senderFvk.inViewingKey();
      byte[] rcm1 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm1);
      byte[] rcm2 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm2);
      PaymentAddress senderPaymentAddress1 = senderIvk.address(DiversifierT.random()).get();
      PaymentAddress senderPaymentAddress2 = senderIvk.address(DiversifierT.random()).get();

      { //for mint1
        ShieldedTRC20ParametersBuilder mintBuilder1 = new ShieldedTRC20ParametersBuilder();
        mintBuilder1.setTransparentFromAmount(BigInteger.valueOf(30));
        mintBuilder1.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder1.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder1.addOutput(DEFAULT_OVK, senderPaymentAddress1.getD(),
            senderPaymentAddress1.getPkD(), 30, rcm1, new byte[512]);
        ShieldedTRC20Parameters mintParams1 = mintBuilder1.build(false);

        byte[] mintInputData1 = abiEncodeForMint(mintParams1, 30, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult1 = mintContract.execute(mintInputData1);
        byte[] mintResult1 = mintContractResult1.getRight();
        Assert.assertEquals(1, mintResult1[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult1[32] == 0) {
          System.arraycopy(mintInputData1, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult1[32] - 1) * 32 + 33;
          int destPos = mintResult1[32] * 32;
          System.arraycopy(mintResult1, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for mint2
        ShieldedTRC20ParametersBuilder mintBuilder2 = new ShieldedTRC20ParametersBuilder();
        mintBuilder2.setTransparentFromAmount(BigInteger.valueOf(70));
        mintBuilder2.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder2.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder2.addOutput(DEFAULT_OVK, senderPaymentAddress2.getD(),
            senderPaymentAddress2.getPkD(), 70, rcm2, new byte[512]);
        ShieldedTRC20Parameters mintParams2 = mintBuilder2.build(false);

        byte[] mintInputData2 = abiEncodeForMint(mintParams2, 70, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult2 = mintContract.execute(mintInputData2);
        byte[] mintResult2 = mintContractResult2.getRight();

        Assert.assertEquals(1, mintResult2[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult2[32] == 0) {
          System.arraycopy(mintInputData2, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult2[32] - 1) * 32 + 33;
          int destPos = mintResult2[32] * 32;
          System.arraycopy(mintResult2, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for transfer
        ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
        builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.TRANSFER);
        builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        builder.setTransparentFromAmount(BigInteger.ZERO);
        builder.setTransparentToAmount(BigInteger.ZERO);
        //spendNote1
        Note senderNote1 = new Note(senderPaymentAddress1.getD(), senderPaymentAddress1.getPkD(),
            30, rcm1, new byte[512]);
        byte[][] cm1 = new byte[1][32];
        System.arraycopy(senderNote1.cm(), 0, cm1[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher1 = addSimpleMerkleVoucherContainer(tree, cm1);
        byte[] path1 = decodePath(voucher1.path().encode());
        byte[] anchor1 = voucher1.root().getContent().toByteArray();
        long position1 = voucher1.position();
        builder.addSpend(senderExpsk, senderNote1, anchor1, path1, position1);

        //spendNote2
        Note senderNote2 = new Note(senderPaymentAddress2.getD(), senderPaymentAddress2.getPkD(),
            70, rcm2, new byte[512]);
        byte[][] cm2 = new byte[1][32];
        System.arraycopy(senderNote2.cm(), 0, cm2[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher2 = addSimpleMerkleVoucherContainer(tree, cm2);
        byte[] path2 = decodePath(voucher2.path().encode());
        byte[] anchor2 = voucher2.root().getContent().toByteArray();
        long position2 = voucher2.position();
        builder.addSpend(senderExpsk, senderNote2, anchor2, path2, position2);

        //receiveNote1
        SpendingKey receiveSk1 = SpendingKey.random();
        FullViewingKey receiveFvk1 = receiveSk1.fullViewingKey();
        IncomingViewingKey receiveIvk1 = receiveFvk1.inViewingKey();
        PaymentAddress receivePaymentAddress1 = receiveIvk1.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress1, 40, new byte[512]);

        //receiveNote2
        SpendingKey receiveSk2 = SpendingKey.random();
        FullViewingKey receiveFvk2 = receiveSk2.fullViewingKey();
        IncomingViewingKey receiveIvk2 = receiveFvk2.inViewingKey();
        PaymentAddress receivePaymentAddress2 = receiveIvk2.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress2, 60, new byte[512]);

        ShieldedTRC20Parameters params = builder.build(true);

        byte[] inputData = abiEncodeForTransferWrongSpendProof(params, frontier, leafCount);
        Pair<Boolean, byte[]> contractResult = verifyTransfer(inputData);
        byte[] result = contractResult.getRight();
        Assert.assertEquals(0, result[31]);
      }
    }
  }

  @Test
  public void verifyTransferProofWrongCm() throws ZksnarkException {
    int totalCountNum = 1;
    long leafCount = 0;
    byte[] frontier = new byte[32 * 33];

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      SpendingKey senderSk = SpendingKey.random();
      ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
      FullViewingKey senderFvk = senderSk.fullViewingKey();
      byte[] senderOvk = senderFvk.getOvk();
      IncomingViewingKey senderIvk = senderFvk.inViewingKey();
      byte[] rcm1 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm1);
      byte[] rcm2 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm2);
      PaymentAddress senderPaymentAddress1 = senderIvk.address(DiversifierT.random()).get();
      PaymentAddress senderPaymentAddress2 = senderIvk.address(DiversifierT.random()).get();

      { //for mint1
        ShieldedTRC20ParametersBuilder mintBuilder1 = new ShieldedTRC20ParametersBuilder();
        mintBuilder1.setTransparentFromAmount(BigInteger.valueOf(30));
        mintBuilder1.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder1.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder1.addOutput(DEFAULT_OVK, senderPaymentAddress1.getD(),
            senderPaymentAddress1.getPkD(), 30, rcm1, new byte[512]);
        ShieldedTRC20Parameters mintParams1 = mintBuilder1.build(false);

        byte[] mintInputData1 = abiEncodeForMint(mintParams1, 30, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult1 = mintContract.execute(mintInputData1);
        byte[] mintResult1 = mintContractResult1.getRight();
        Assert.assertEquals(1, mintResult1[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult1[32] == 0) {
          System.arraycopy(mintInputData1, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult1[32] - 1) * 32 + 33;
          int destPos = mintResult1[32] * 32;
          System.arraycopy(mintResult1, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for mint2
        ShieldedTRC20ParametersBuilder mintBuilder2 = new ShieldedTRC20ParametersBuilder();
        mintBuilder2.setTransparentFromAmount(BigInteger.valueOf(70));
        mintBuilder2.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder2.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder2.addOutput(DEFAULT_OVK, senderPaymentAddress2.getD(),
            senderPaymentAddress2.getPkD(), 70, rcm2, new byte[512]);
        ShieldedTRC20Parameters mintParams2 = mintBuilder2.build(false);

        byte[] mintInputData2 = abiEncodeForMint(mintParams2, 70, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult2 = mintContract.execute(mintInputData2);
        byte[] mintResult2 = mintContractResult2.getRight();

        Assert.assertEquals(1, mintResult2[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult2[32] == 0) {
          System.arraycopy(mintInputData2, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult2[32] - 1) * 32 + 33;
          int destPos = mintResult2[32] * 32;
          System.arraycopy(mintResult2, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for transfer
        ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
        builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.TRANSFER);
        builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        builder.setTransparentFromAmount(BigInteger.ZERO);
        builder.setTransparentToAmount(BigInteger.ZERO);
        //spendNote1
        Note senderNote1 = new Note(senderPaymentAddress1.getD(), senderPaymentAddress1.getPkD(),
            30, rcm1, new byte[512]);
        byte[][] cm1 = new byte[1][32];
        System.arraycopy(senderNote1.cm(), 0, cm1[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher1 = addSimpleMerkleVoucherContainer(tree, cm1);
        byte[] path1 = decodePath(voucher1.path().encode());
        byte[] anchor1 = voucher1.root().getContent().toByteArray();
        long position1 = voucher1.position();
        builder.addSpend(senderExpsk, senderNote1, anchor1, path1, position1);

        //spendNote2
        Note senderNote2 = new Note(senderPaymentAddress2.getD(), senderPaymentAddress2.getPkD(),
            70, rcm2, new byte[512]);
        byte[][] cm2 = new byte[1][32];
        System.arraycopy(senderNote2.cm(), 0, cm2[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher2 = addSimpleMerkleVoucherContainer(tree, cm2);
        byte[] path2 = decodePath(voucher2.path().encode());
        byte[] anchor2 = voucher2.root().getContent().toByteArray();
        long position2 = voucher2.position();
        builder.addSpend(senderExpsk, senderNote2, anchor2, path2, position2);

        //receiveNote1
        SpendingKey receiveSk1 = SpendingKey.random();
        FullViewingKey receiveFvk1 = receiveSk1.fullViewingKey();
        IncomingViewingKey receiveIvk1 = receiveFvk1.inViewingKey();
        PaymentAddress receivePaymentAddress1 = receiveIvk1.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress1, 40, new byte[512]);

        //receiveNote2
        SpendingKey receiveSk2 = SpendingKey.random();
        FullViewingKey receiveFvk2 = receiveSk2.fullViewingKey();
        IncomingViewingKey receiveIvk2 = receiveFvk2.inViewingKey();
        PaymentAddress receivePaymentAddress2 = receiveIvk2.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress2, 60, new byte[512]);

        ShieldedTRC20Parameters params = builder.build(true);

        byte[] inputData = abiEncodeForTransferWrongCM(params, frontier, leafCount);
        Pair<Boolean, byte[]> contractResult = verifyTransfer(inputData);
        byte[] result = contractResult.getRight();
        Assert.assertEquals(0, result[31]);
      }
    }
  }

  @Test
  public void verifyTransferProofWrongReceiveCV() throws ZksnarkException {
    int totalCountNum = 1;
    long leafCount = 0;
    byte[] frontier = new byte[32 * 33];

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      SpendingKey senderSk = SpendingKey.random();
      ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
      FullViewingKey senderFvk = senderSk.fullViewingKey();
      byte[] senderOvk = senderFvk.getOvk();
      IncomingViewingKey senderIvk = senderFvk.inViewingKey();
      byte[] rcm1 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm1);
      byte[] rcm2 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm2);
      PaymentAddress senderPaymentAddress1 = senderIvk.address(DiversifierT.random()).get();
      PaymentAddress senderPaymentAddress2 = senderIvk.address(DiversifierT.random()).get();

      { //for mint1
        ShieldedTRC20ParametersBuilder mintBuilder1 = new ShieldedTRC20ParametersBuilder();
        mintBuilder1.setTransparentFromAmount(BigInteger.valueOf(30));
        mintBuilder1.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder1.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder1.addOutput(DEFAULT_OVK, senderPaymentAddress1.getD(),
            senderPaymentAddress1.getPkD(), 30, rcm1, new byte[512]);
        ShieldedTRC20Parameters mintParams1 = mintBuilder1.build(false);

        byte[] mintInputData1 = abiEncodeForMint(mintParams1, 30, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult1 = mintContract.execute(mintInputData1);
        byte[] mintResult1 = mintContractResult1.getRight();
        Assert.assertEquals(1, mintResult1[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult1[32] == 0) {
          System.arraycopy(mintInputData1, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult1[32] - 1) * 32 + 33;
          int destPos = mintResult1[32] * 32;
          System.arraycopy(mintResult1, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for mint2
        ShieldedTRC20ParametersBuilder mintBuilder2 = new ShieldedTRC20ParametersBuilder();
        mintBuilder2.setTransparentFromAmount(BigInteger.valueOf(70));
        mintBuilder2.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder2.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder2.addOutput(DEFAULT_OVK, senderPaymentAddress2.getD(),
            senderPaymentAddress2.getPkD(), 70, rcm2, new byte[512]);
        ShieldedTRC20Parameters mintParams2 = mintBuilder2.build(false);

        byte[] mintInputData2 = abiEncodeForMint(mintParams2, 70, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult2 = mintContract.execute(mintInputData2);
        byte[] mintResult2 = mintContractResult2.getRight();

        Assert.assertEquals(1, mintResult2[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult2[32] == 0) {
          System.arraycopy(mintInputData2, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult2[32] - 1) * 32 + 33;
          int destPos = mintResult2[32] * 32;
          System.arraycopy(mintResult2, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for transfer
        ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
        builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.TRANSFER);
        builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        builder.setTransparentFromAmount(BigInteger.ZERO);
        builder.setTransparentToAmount(BigInteger.ZERO);
        //spendNote1
        Note senderNote1 = new Note(senderPaymentAddress1.getD(), senderPaymentAddress1.getPkD(),
            30, rcm1, new byte[512]);
        byte[][] cm1 = new byte[1][32];
        System.arraycopy(senderNote1.cm(), 0, cm1[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher1 = addSimpleMerkleVoucherContainer(tree, cm1);
        byte[] path1 = decodePath(voucher1.path().encode());
        byte[] anchor1 = voucher1.root().getContent().toByteArray();
        long position1 = voucher1.position();
        builder.addSpend(senderExpsk, senderNote1, anchor1, path1, position1);

        //spendNote2
        Note senderNote2 = new Note(senderPaymentAddress2.getD(), senderPaymentAddress2.getPkD(),
            70, rcm2, new byte[512]);
        byte[][] cm2 = new byte[1][32];
        System.arraycopy(senderNote2.cm(), 0, cm2[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher2 = addSimpleMerkleVoucherContainer(tree, cm2);
        byte[] path2 = decodePath(voucher2.path().encode());
        byte[] anchor2 = voucher2.root().getContent().toByteArray();
        long position2 = voucher2.position();
        builder.addSpend(senderExpsk, senderNote2, anchor2, path2, position2);

        //receiveNote1
        SpendingKey receiveSk1 = SpendingKey.random();
        FullViewingKey receiveFvk1 = receiveSk1.fullViewingKey();
        IncomingViewingKey receiveIvk1 = receiveFvk1.inViewingKey();
        PaymentAddress receivePaymentAddress1 = receiveIvk1.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress1, 40, new byte[512]);

        //receiveNote2
        SpendingKey receiveSk2 = SpendingKey.random();
        FullViewingKey receiveFvk2 = receiveSk2.fullViewingKey();
        IncomingViewingKey receiveIvk2 = receiveFvk2.inViewingKey();
        PaymentAddress receivePaymentAddress2 = receiveIvk2.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress2, 60, new byte[512]);

        ShieldedTRC20Parameters params = builder.build(true);

        byte[] inputData = abiEncodeForTransferWrongReceiveCV(params, frontier, leafCount);
        Pair<Boolean, byte[]> contractResult = verifyTransfer(inputData);
        byte[] result = contractResult.getRight();
        Assert.assertEquals(0, result[31]);
      }
    }
  }

  @Test
  public void verifyTransferProofWrongEpk() throws ZksnarkException {
    int totalCountNum = 1;
    long leafCount = 0;
    byte[] frontier = new byte[32 * 33];

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      SpendingKey senderSk = SpendingKey.random();
      ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
      FullViewingKey senderFvk = senderSk.fullViewingKey();
      byte[] senderOvk = senderFvk.getOvk();
      IncomingViewingKey senderIvk = senderFvk.inViewingKey();
      byte[] rcm1 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm1);
      byte[] rcm2 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm2);
      PaymentAddress senderPaymentAddress1 = senderIvk.address(DiversifierT.random()).get();
      PaymentAddress senderPaymentAddress2 = senderIvk.address(DiversifierT.random()).get();

      { //for mint1
        ShieldedTRC20ParametersBuilder mintBuilder1 = new ShieldedTRC20ParametersBuilder();
        mintBuilder1.setTransparentFromAmount(BigInteger.valueOf(30));
        mintBuilder1.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder1.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder1.addOutput(DEFAULT_OVK, senderPaymentAddress1.getD(),
            senderPaymentAddress1.getPkD(), 30, rcm1, new byte[512]);
        ShieldedTRC20Parameters mintParams1 = mintBuilder1.build(false);

        byte[] mintInputData1 = abiEncodeForMint(mintParams1, 30, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult1 = mintContract.execute(mintInputData1);
        byte[] mintResult1 = mintContractResult1.getRight();
        Assert.assertEquals(1, mintResult1[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult1[32] == 0) {
          System.arraycopy(mintInputData1, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult1[32] - 1) * 32 + 33;
          int destPos = mintResult1[32] * 32;
          System.arraycopy(mintResult1, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for mint2
        ShieldedTRC20ParametersBuilder mintBuilder2 = new ShieldedTRC20ParametersBuilder();
        mintBuilder2.setTransparentFromAmount(BigInteger.valueOf(70));
        mintBuilder2.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder2.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder2.addOutput(DEFAULT_OVK, senderPaymentAddress2.getD(),
            senderPaymentAddress2.getPkD(), 70, rcm2, new byte[512]);
        ShieldedTRC20Parameters mintParams2 = mintBuilder2.build(false);

        byte[] mintInputData2 = abiEncodeForMint(mintParams2, 70, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult2 = mintContract.execute(mintInputData2);
        byte[] mintResult2 = mintContractResult2.getRight();

        Assert.assertEquals(1, mintResult2[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult2[32] == 0) {
          System.arraycopy(mintInputData2, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult2[32] - 1) * 32 + 33;
          int destPos = mintResult2[32] * 32;
          System.arraycopy(mintResult2, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for transfer
        ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
        builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.TRANSFER);
        builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        builder.setTransparentFromAmount(BigInteger.ZERO);
        builder.setTransparentToAmount(BigInteger.ZERO);
        //spendNote1
        Note senderNote1 = new Note(senderPaymentAddress1.getD(), senderPaymentAddress1.getPkD(),
            30, rcm1, new byte[512]);
        byte[][] cm1 = new byte[1][32];
        System.arraycopy(senderNote1.cm(), 0, cm1[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher1 = addSimpleMerkleVoucherContainer(tree, cm1);
        byte[] path1 = decodePath(voucher1.path().encode());
        byte[] anchor1 = voucher1.root().getContent().toByteArray();
        long position1 = voucher1.position();
        builder.addSpend(senderExpsk, senderNote1, anchor1, path1, position1);

        //spendNote2
        Note senderNote2 = new Note(senderPaymentAddress2.getD(), senderPaymentAddress2.getPkD(),
            70, rcm2, new byte[512]);
        byte[][] cm2 = new byte[1][32];
        System.arraycopy(senderNote2.cm(), 0, cm2[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher2 = addSimpleMerkleVoucherContainer(tree, cm2);
        byte[] path2 = decodePath(voucher2.path().encode());
        byte[] anchor2 = voucher2.root().getContent().toByteArray();
        long position2 = voucher2.position();
        builder.addSpend(senderExpsk, senderNote2, anchor2, path2, position2);

        //receiveNote1
        SpendingKey receiveSk1 = SpendingKey.random();
        FullViewingKey receiveFvk1 = receiveSk1.fullViewingKey();
        IncomingViewingKey receiveIvk1 = receiveFvk1.inViewingKey();
        PaymentAddress receivePaymentAddress1 = receiveIvk1.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress1, 40, new byte[512]);

        //receiveNote2
        SpendingKey receiveSk2 = SpendingKey.random();
        FullViewingKey receiveFvk2 = receiveSk2.fullViewingKey();
        IncomingViewingKey receiveIvk2 = receiveFvk2.inViewingKey();
        PaymentAddress receivePaymentAddress2 = receiveIvk2.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress2, 60, new byte[512]);

        ShieldedTRC20Parameters params = builder.build(true);

        byte[] inputData = abiEncodeForTransferWrongEpk(params, frontier, leafCount);
        Pair<Boolean, byte[]> contractResult = verifyTransfer(inputData);
        byte[] result = contractResult.getRight();
        Assert.assertEquals(0, result[31]);
      }
    }
  }

  @Test
  public void verifyTransferProofWrongReceiveProof() throws ZksnarkException {
    int totalCountNum = 1;
    long leafCount = 0;
    byte[] frontier = new byte[32 * 33];

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      SpendingKey senderSk = SpendingKey.random();
      ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
      FullViewingKey senderFvk = senderSk.fullViewingKey();
      byte[] senderOvk = senderFvk.getOvk();
      IncomingViewingKey senderIvk = senderFvk.inViewingKey();
      byte[] rcm1 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm1);
      byte[] rcm2 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm2);
      PaymentAddress senderPaymentAddress1 = senderIvk.address(DiversifierT.random()).get();
      PaymentAddress senderPaymentAddress2 = senderIvk.address(DiversifierT.random()).get();

      { //for mint1
        ShieldedTRC20ParametersBuilder mintBuilder1 = new ShieldedTRC20ParametersBuilder();
        mintBuilder1.setTransparentFromAmount(BigInteger.valueOf(30));
        mintBuilder1.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder1.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder1.addOutput(DEFAULT_OVK, senderPaymentAddress1.getD(),
            senderPaymentAddress1.getPkD(), 30, rcm1, new byte[512]);
        ShieldedTRC20Parameters mintParams1 = mintBuilder1.build(false);

        byte[] mintInputData1 = abiEncodeForMint(mintParams1, 30, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult1 = mintContract.execute(mintInputData1);
        byte[] mintResult1 = mintContractResult1.getRight();
        Assert.assertEquals(1, mintResult1[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult1[32] == 0) {
          System.arraycopy(mintInputData1, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult1[32] - 1) * 32 + 33;
          int destPos = mintResult1[32] * 32;
          System.arraycopy(mintResult1, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for mint2
        ShieldedTRC20ParametersBuilder mintBuilder2 = new ShieldedTRC20ParametersBuilder();
        mintBuilder2.setTransparentFromAmount(BigInteger.valueOf(70));
        mintBuilder2.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder2.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder2.addOutput(DEFAULT_OVK, senderPaymentAddress2.getD(),
            senderPaymentAddress2.getPkD(), 70, rcm2, new byte[512]);
        ShieldedTRC20Parameters mintParams2 = mintBuilder2.build(false);

        byte[] mintInputData2 = abiEncodeForMint(mintParams2, 70, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult2 = mintContract.execute(mintInputData2);
        byte[] mintResult2 = mintContractResult2.getRight();

        Assert.assertEquals(1, mintResult2[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult2[32] == 0) {
          System.arraycopy(mintInputData2, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult2[32] - 1) * 32 + 33;
          int destPos = mintResult2[32] * 32;
          System.arraycopy(mintResult2, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for transfer
        ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
        builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.TRANSFER);
        builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        builder.setTransparentFromAmount(BigInteger.ZERO);
        builder.setTransparentToAmount(BigInteger.ZERO);
        //spendNote1
        Note senderNote1 = new Note(senderPaymentAddress1.getD(), senderPaymentAddress1.getPkD(),
            30, rcm1, new byte[512]);
        byte[][] cm1 = new byte[1][32];
        System.arraycopy(senderNote1.cm(), 0, cm1[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher1 = addSimpleMerkleVoucherContainer(tree, cm1);
        byte[] path1 = decodePath(voucher1.path().encode());
        byte[] anchor1 = voucher1.root().getContent().toByteArray();
        long position1 = voucher1.position();
        builder.addSpend(senderExpsk, senderNote1, anchor1, path1, position1);

        //spendNote2
        Note senderNote2 = new Note(senderPaymentAddress2.getD(), senderPaymentAddress2.getPkD(),
            70, rcm2, new byte[512]);
        byte[][] cm2 = new byte[1][32];
        System.arraycopy(senderNote2.cm(), 0, cm2[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher2 = addSimpleMerkleVoucherContainer(tree, cm2);
        byte[] path2 = decodePath(voucher2.path().encode());
        byte[] anchor2 = voucher2.root().getContent().toByteArray();
        long position2 = voucher2.position();
        builder.addSpend(senderExpsk, senderNote2, anchor2, path2, position2);

        //receiveNote1
        SpendingKey receiveSk1 = SpendingKey.random();
        FullViewingKey receiveFvk1 = receiveSk1.fullViewingKey();
        IncomingViewingKey receiveIvk1 = receiveFvk1.inViewingKey();
        PaymentAddress receivePaymentAddress1 = receiveIvk1.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress1, 40, new byte[512]);

        //receiveNote2
        SpendingKey receiveSk2 = SpendingKey.random();
        FullViewingKey receiveFvk2 = receiveSk2.fullViewingKey();
        IncomingViewingKey receiveIvk2 = receiveFvk2.inViewingKey();
        PaymentAddress receivePaymentAddress2 = receiveIvk2.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress2, 60, new byte[512]);

        ShieldedTRC20Parameters params = builder.build(true);

        byte[] inputData = abiEncodeForTransferWrongReceivProof(params, frontier, leafCount);
        Pair<Boolean, byte[]> contractResult = verifyTransfer(inputData);
        byte[] result = contractResult.getRight();
        Assert.assertEquals(0, result[31]);
      }
    }
  }

  @Test
  public void verifyTransferProofWrongBindingSignature() throws ZksnarkException {
    int totalCountNum = 1;
    long leafCount = 0;
    byte[] frontier = new byte[32 * 33];

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      SpendingKey senderSk = SpendingKey.random();
      ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
      FullViewingKey senderFvk = senderSk.fullViewingKey();
      byte[] senderOvk = senderFvk.getOvk();
      IncomingViewingKey senderIvk = senderFvk.inViewingKey();
      byte[] rcm1 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm1);
      byte[] rcm2 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm2);
      PaymentAddress senderPaymentAddress1 = senderIvk.address(DiversifierT.random()).get();
      PaymentAddress senderPaymentAddress2 = senderIvk.address(DiversifierT.random()).get();

      { //for mint1
        ShieldedTRC20ParametersBuilder mintBuilder1 = new ShieldedTRC20ParametersBuilder();
        mintBuilder1.setTransparentFromAmount(BigInteger.valueOf(30));
        mintBuilder1.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder1.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder1.addOutput(DEFAULT_OVK, senderPaymentAddress1.getD(),
            senderPaymentAddress1.getPkD(), 30, rcm1, new byte[512]);
        ShieldedTRC20Parameters mintParams1 = mintBuilder1.build(false);

        byte[] mintInputData1 = abiEncodeForMint(mintParams1, 30, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult1 = mintContract.execute(mintInputData1);
        byte[] mintResult1 = mintContractResult1.getRight();
        Assert.assertEquals(1, mintResult1[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult1[32] == 0) {
          System.arraycopy(mintInputData1, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult1[32] - 1) * 32 + 33;
          int destPos = mintResult1[32] * 32;
          System.arraycopy(mintResult1, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for mint2
        ShieldedTRC20ParametersBuilder mintBuilder2 = new ShieldedTRC20ParametersBuilder();
        mintBuilder2.setTransparentFromAmount(BigInteger.valueOf(70));
        mintBuilder2.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder2.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder2.addOutput(DEFAULT_OVK, senderPaymentAddress2.getD(),
            senderPaymentAddress2.getPkD(), 70, rcm2, new byte[512]);
        ShieldedTRC20Parameters mintParams2 = mintBuilder2.build(false);

        byte[] mintInputData2 = abiEncodeForMint(mintParams2, 70, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult2 = mintContract.execute(mintInputData2);
        byte[] mintResult2 = mintContractResult2.getRight();

        Assert.assertEquals(1, mintResult2[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult2[32] == 0) {
          System.arraycopy(mintInputData2, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult2[32] - 1) * 32 + 33;
          int destPos = mintResult2[32] * 32;
          System.arraycopy(mintResult2, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for transfer
        ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
        builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.TRANSFER);
        builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        builder.setTransparentFromAmount(BigInteger.ZERO);
        builder.setTransparentToAmount(BigInteger.ZERO);
        //spendNote1
        Note senderNote1 = new Note(senderPaymentAddress1.getD(), senderPaymentAddress1.getPkD(),
            30, rcm1, new byte[512]);
        byte[][] cm1 = new byte[1][32];
        System.arraycopy(senderNote1.cm(), 0, cm1[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher1 = addSimpleMerkleVoucherContainer(tree, cm1);
        byte[] path1 = decodePath(voucher1.path().encode());
        byte[] anchor1 = voucher1.root().getContent().toByteArray();
        long position1 = voucher1.position();
        builder.addSpend(senderExpsk, senderNote1, anchor1, path1, position1);

        //spendNote2
        Note senderNote2 = new Note(senderPaymentAddress2.getD(), senderPaymentAddress2.getPkD(),
            70, rcm2, new byte[512]);
        byte[][] cm2 = new byte[1][32];
        System.arraycopy(senderNote2.cm(), 0, cm2[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher2 = addSimpleMerkleVoucherContainer(tree, cm2);
        byte[] path2 = decodePath(voucher2.path().encode());
        byte[] anchor2 = voucher2.root().getContent().toByteArray();
        long position2 = voucher2.position();
        builder.addSpend(senderExpsk, senderNote2, anchor2, path2, position2);

        //receiveNote1
        SpendingKey receiveSk1 = SpendingKey.random();
        FullViewingKey receiveFvk1 = receiveSk1.fullViewingKey();
        IncomingViewingKey receiveIvk1 = receiveFvk1.inViewingKey();
        PaymentAddress receivePaymentAddress1 = receiveIvk1.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress1, 40, new byte[512]);

        //receiveNote2
        SpendingKey receiveSk2 = SpendingKey.random();
        FullViewingKey receiveFvk2 = receiveSk2.fullViewingKey();
        IncomingViewingKey receiveIvk2 = receiveFvk2.inViewingKey();
        PaymentAddress receivePaymentAddress2 = receiveIvk2.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress2, 60, new byte[512]);

        ShieldedTRC20Parameters params = builder.build(true);

        byte[] inputData = abiEncodeForTransferWrongBindingSignature(params, frontier, leafCount);
        Pair<Boolean, byte[]> contractResult = verifyTransfer(inputData);
        byte[] result = contractResult.getRight();
        Assert.assertEquals(0, result[31]);
      }
    }
  }

  @Test
  public void verifyTransferProofWrongHash() throws ZksnarkException {
    int totalCountNum = 1;
    long leafCount = 0;
    byte[] frontier = new byte[32 * 33];

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      SpendingKey senderSk = SpendingKey.random();
      ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
      FullViewingKey senderFvk = senderSk.fullViewingKey();
      byte[] senderOvk = senderFvk.getOvk();
      IncomingViewingKey senderIvk = senderFvk.inViewingKey();
      byte[] rcm1 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm1);
      byte[] rcm2 = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm2);
      PaymentAddress senderPaymentAddress1 = senderIvk.address(DiversifierT.random()).get();
      PaymentAddress senderPaymentAddress2 = senderIvk.address(DiversifierT.random()).get();

      { //for mint1
        ShieldedTRC20ParametersBuilder mintBuilder1 = new ShieldedTRC20ParametersBuilder();
        mintBuilder1.setTransparentFromAmount(BigInteger.valueOf(30));
        mintBuilder1.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder1.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder1.addOutput(DEFAULT_OVK, senderPaymentAddress1.getD(),
            senderPaymentAddress1.getPkD(), 30, rcm1, new byte[512]);
        ShieldedTRC20Parameters mintParams1 = mintBuilder1.build(false);

        byte[] mintInputData1 = abiEncodeForMint(mintParams1, 30, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult1 = mintContract.execute(mintInputData1);
        byte[] mintResult1 = mintContractResult1.getRight();
        Assert.assertEquals(1, mintResult1[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult1[32] == 0) {
          System.arraycopy(mintInputData1, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult1[32] - 1) * 32 + 33;
          int destPos = mintResult1[32] * 32;
          System.arraycopy(mintResult1, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for mint2
        ShieldedTRC20ParametersBuilder mintBuilder2 = new ShieldedTRC20ParametersBuilder();
        mintBuilder2.setTransparentFromAmount(BigInteger.valueOf(70));
        mintBuilder2.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder2.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder2.addOutput(DEFAULT_OVK, senderPaymentAddress2.getD(),
            senderPaymentAddress2.getPkD(), 70, rcm2, new byte[512]);
        ShieldedTRC20Parameters mintParams2 = mintBuilder2.build(false);

        byte[] mintInputData2 = abiEncodeForMint(mintParams2, 70, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult2 = mintContract.execute(mintInputData2);
        byte[] mintResult2 = mintContractResult2.getRight();

        Assert.assertEquals(1, mintResult2[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult2[32] == 0) {
          System.arraycopy(mintInputData2, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult2[32] - 1) * 32 + 33;
          int destPos = mintResult2[32] * 32;
          System.arraycopy(mintResult2, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for transfer
        ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
        builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.TRANSFER);
        builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        builder.setTransparentFromAmount(BigInteger.ZERO);
        builder.setTransparentToAmount(BigInteger.ZERO);
        //spendNote1
        Note senderNote1 = new Note(senderPaymentAddress1.getD(), senderPaymentAddress1.getPkD(),
            30, rcm1, new byte[512]);
        byte[][] cm1 = new byte[1][32];
        System.arraycopy(senderNote1.cm(), 0, cm1[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher1 = addSimpleMerkleVoucherContainer(tree, cm1);
        byte[] path1 = decodePath(voucher1.path().encode());
        byte[] anchor1 = voucher1.root().getContent().toByteArray();
        long position1 = voucher1.position();
        builder.addSpend(senderExpsk, senderNote1, anchor1, path1, position1);

        //spendNote2
        Note senderNote2 = new Note(senderPaymentAddress2.getD(), senderPaymentAddress2.getPkD(),
            70, rcm2, new byte[512]);
        byte[][] cm2 = new byte[1][32];
        System.arraycopy(senderNote2.cm(), 0, cm2[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher2 = addSimpleMerkleVoucherContainer(tree, cm2);
        byte[] path2 = decodePath(voucher2.path().encode());
        byte[] anchor2 = voucher2.root().getContent().toByteArray();
        long position2 = voucher2.position();
        builder.addSpend(senderExpsk, senderNote2, anchor2, path2, position2);

        //receiveNote1
        SpendingKey receiveSk1 = SpendingKey.random();
        FullViewingKey receiveFvk1 = receiveSk1.fullViewingKey();
        IncomingViewingKey receiveIvk1 = receiveFvk1.inViewingKey();
        PaymentAddress receivePaymentAddress1 = receiveIvk1.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress1, 40, new byte[512]);

        //receiveNote2
        SpendingKey receiveSk2 = SpendingKey.random();
        FullViewingKey receiveFvk2 = receiveSk2.fullViewingKey();
        IncomingViewingKey receiveIvk2 = receiveFvk2.inViewingKey();
        PaymentAddress receivePaymentAddress2 = receiveIvk2.address(new DiversifierT()).get();
        builder.addOutput(senderOvk, receivePaymentAddress2, 60, new byte[512]);

        ShieldedTRC20Parameters params = builder.build(true);

        byte[] inputData = abiEncodeForTransferWrongHash(params, frontier, leafCount);
        Pair<Boolean, byte[]> contractResult = verifyTransfer(inputData);
        byte[] result = contractResult.getRight();
        Assert.assertEquals(0, result[31]);
      }
    }
  }

  @Test
  public void verifyBurnWrongNF() throws ZksnarkException {
    int totalCountNum = 1;
    long leafCount = 0;
    long value = 100L;
    byte[] frontier = new byte[32 * 33];

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      SpendingKey senderSk = SpendingKey.random();
      ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
      FullViewingKey senderFvk = senderSk.fullViewingKey();
      byte[] senderOvk = senderFvk.getOvk();
      IncomingViewingKey senderIvk = senderFvk.inViewingKey();
      byte[] rcm = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm);
      PaymentAddress senderPaymentAddress = senderIvk.address(DiversifierT.random()).get();

      { //for mint
        ShieldedTRC20ParametersBuilder mintBuilder = new ShieldedTRC20ParametersBuilder();
        mintBuilder.setTransparentFromAmount(BigInteger.valueOf(value));
        mintBuilder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder.addOutput(DEFAULT_OVK, senderPaymentAddress.getD(),
            senderPaymentAddress.getPkD(), value, rcm, new byte[512]);
        ShieldedTRC20Parameters mintParams = mintBuilder.build(false);

        byte[] mintInputData = abiEncodeForMint(mintParams, value, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult = mintContract.execute(mintInputData);
        byte[] mintResult = mintContractResult.getRight();
        Assert.assertEquals(1, mintResult[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult[32] == 0) {
          System.arraycopy(mintInputData, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult[32] - 1) * 32 + 33;
          int destPos = mintResult[32] * 32;
          System.arraycopy(mintResult, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for burn
        ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
        builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.BURN);
        builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        builder.setTransparentToAmount(BigInteger.valueOf(value));
        builder.setTransparentToAddress(PUBLIC_TO_ADDRESS);
        //spendNote1
        Note senderNote = new Note(senderPaymentAddress.getD(), senderPaymentAddress.getPkD(),
            value, rcm, new byte[512]);
        byte[][] cm = new byte[1][32];
        System.arraycopy(senderNote.cm(), 0, cm[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher = addSimpleMerkleVoucherContainer(tree, cm);
        byte[] path = decodePath(voucher.path().encode());
        byte[] anchor = voucher.root().getContent().toByteArray();
        long position = voucher.position();
        builder.addSpend(senderExpsk, senderNote, anchor, path, position);
        ShieldedTRC20Parameters params = builder.build(true);

        byte[] inputData = abiEncodeForBurnWrongNF(params, value);
        Pair<Boolean, byte[]> contractResult = burnContract.execute(inputData);
        byte[] result = contractResult.getRight();
        Assert.assertEquals(0, result[31]);
      }
    }
  }

  @Test
  public void verifyBurnWrongRoot() throws ZksnarkException {
    int totalCountNum = 1;
    long leafCount = 0;
    long value = 100L;
    byte[] frontier = new byte[32 * 33];

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      SpendingKey senderSk = SpendingKey.random();
      ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
      FullViewingKey senderFvk = senderSk.fullViewingKey();
      byte[] senderOvk = senderFvk.getOvk();
      IncomingViewingKey senderIvk = senderFvk.inViewingKey();
      byte[] rcm = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm);
      PaymentAddress senderPaymentAddress = senderIvk.address(DiversifierT.random()).get();

      { //for mint
        ShieldedTRC20ParametersBuilder mintBuilder = new ShieldedTRC20ParametersBuilder();
        mintBuilder.setTransparentFromAmount(BigInteger.valueOf(value));
        mintBuilder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder.addOutput(DEFAULT_OVK, senderPaymentAddress.getD(),
            senderPaymentAddress.getPkD(), value, rcm, new byte[512]);
        ShieldedTRC20Parameters mintParams = mintBuilder.build(false);

        byte[] mintInputData = abiEncodeForMint(mintParams, value, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult = mintContract.execute(mintInputData);
        byte[] mintResult = mintContractResult.getRight();
        Assert.assertEquals(1, mintResult[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult[32] == 0) {
          System.arraycopy(mintInputData, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult[32] - 1) * 32 + 33;
          int destPos = mintResult[32] * 32;
          System.arraycopy(mintResult, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for burn
        ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
        builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.BURN);
        builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        builder.setTransparentToAmount(BigInteger.valueOf(value));
        builder.setTransparentToAddress(PUBLIC_TO_ADDRESS);
        //spendNote1
        Note senderNote = new Note(senderPaymentAddress.getD(), senderPaymentAddress.getPkD(),
            value, rcm, new byte[512]);
        byte[][] cm = new byte[1][32];
        System.arraycopy(senderNote.cm(), 0, cm[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher = addSimpleMerkleVoucherContainer(tree, cm);
        byte[] path = decodePath(voucher.path().encode());
        byte[] anchor = voucher.root().getContent().toByteArray();
        long position = voucher.position();
        builder.addSpend(senderExpsk, senderNote, anchor, path, position);
        ShieldedTRC20Parameters params = builder.build(true);

        byte[] inputData = abiEncodeForBurnWrongRoot(params, value);
        Pair<Boolean, byte[]> contractResult = burnContract.execute(inputData);
        byte[] result = contractResult.getRight();
        Assert.assertEquals(0, result[31]);
      }
    }
  }


  @Test
  public void verifyBurnWrongCV() throws ZksnarkException {
    int totalCountNum = 1;
    long leafCount = 0;
    long value = 100L;
    byte[] frontier = new byte[32 * 33];

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      SpendingKey senderSk = SpendingKey.random();
      ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
      FullViewingKey senderFvk = senderSk.fullViewingKey();
      byte[] senderOvk = senderFvk.getOvk();
      IncomingViewingKey senderIvk = senderFvk.inViewingKey();
      byte[] rcm = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm);
      PaymentAddress senderPaymentAddress = senderIvk.address(DiversifierT.random()).get();

      { //for mint
        ShieldedTRC20ParametersBuilder mintBuilder = new ShieldedTRC20ParametersBuilder();
        mintBuilder.setTransparentFromAmount(BigInteger.valueOf(value));
        mintBuilder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder.addOutput(DEFAULT_OVK, senderPaymentAddress.getD(),
            senderPaymentAddress.getPkD(), value, rcm, new byte[512]);
        ShieldedTRC20Parameters mintParams = mintBuilder.build(false);

        byte[] mintInputData = abiEncodeForMint(mintParams, value, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult = mintContract.execute(mintInputData);
        byte[] mintResult = mintContractResult.getRight();
        Assert.assertEquals(1, mintResult[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult[32] == 0) {
          System.arraycopy(mintInputData, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult[32] - 1) * 32 + 33;
          int destPos = mintResult[32] * 32;
          System.arraycopy(mintResult, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for burn
        ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
        builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.BURN);
        builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        builder.setTransparentToAmount(BigInteger.valueOf(value));
        builder.setTransparentToAddress(PUBLIC_TO_ADDRESS);
        //spendNote1
        Note senderNote = new Note(senderPaymentAddress.getD(), senderPaymentAddress.getPkD(),
            value, rcm, new byte[512]);
        byte[][] cm = new byte[1][32];
        System.arraycopy(senderNote.cm(), 0, cm[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher = addSimpleMerkleVoucherContainer(tree, cm);
        byte[] path = decodePath(voucher.path().encode());
        byte[] anchor = voucher.root().getContent().toByteArray();
        long position = voucher.position();
        builder.addSpend(senderExpsk, senderNote, anchor, path, position);
        ShieldedTRC20Parameters params = builder.build(true);

        byte[] inputData = abiEncodeForBurnWrongCV(params, value);
        Pair<Boolean, byte[]> contractResult = burnContract.execute(inputData);
        byte[] result = contractResult.getRight();
        Assert.assertEquals(0, result[31]);
      }
    }
  }

  @Test
  public void verifyBurnWrongRk() throws ZksnarkException {
    int totalCountNum = 1;
    long leafCount = 0;
    long value = 100L;
    byte[] frontier = new byte[32 * 33];

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      SpendingKey senderSk = SpendingKey.random();
      ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
      FullViewingKey senderFvk = senderSk.fullViewingKey();
      byte[] senderOvk = senderFvk.getOvk();
      IncomingViewingKey senderIvk = senderFvk.inViewingKey();
      byte[] rcm = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm);
      PaymentAddress senderPaymentAddress = senderIvk.address(DiversifierT.random()).get();

      { //for mint
        ShieldedTRC20ParametersBuilder mintBuilder = new ShieldedTRC20ParametersBuilder();
        mintBuilder.setTransparentFromAmount(BigInteger.valueOf(value));
        mintBuilder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder.addOutput(DEFAULT_OVK, senderPaymentAddress.getD(),
            senderPaymentAddress.getPkD(), value, rcm, new byte[512]);
        ShieldedTRC20Parameters mintParams = mintBuilder.build(false);

        byte[] mintInputData = abiEncodeForMint(mintParams, value, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult = mintContract.execute(mintInputData);
        byte[] mintResult = mintContractResult.getRight();
        Assert.assertEquals(1, mintResult[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult[32] == 0) {
          System.arraycopy(mintInputData, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult[32] - 1) * 32 + 33;
          int destPos = mintResult[32] * 32;
          System.arraycopy(mintResult, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for burn
        ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
        builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.BURN);
        builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        builder.setTransparentToAmount(BigInteger.valueOf(value));
        builder.setTransparentToAddress(PUBLIC_TO_ADDRESS);
        //spendNote1
        Note senderNote = new Note(senderPaymentAddress.getD(), senderPaymentAddress.getPkD(),
            value, rcm, new byte[512]);
        byte[][] cm = new byte[1][32];
        System.arraycopy(senderNote.cm(), 0, cm[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher = addSimpleMerkleVoucherContainer(tree, cm);
        byte[] path = decodePath(voucher.path().encode());
        byte[] anchor = voucher.root().getContent().toByteArray();
        long position = voucher.position();
        builder.addSpend(senderExpsk, senderNote, anchor, path, position);
        ShieldedTRC20Parameters params = builder.build(true);

        byte[] inputData = abiEncodeForBurnWrongRK(params, value);
        Pair<Boolean, byte[]> contractResult = burnContract.execute(inputData);
        byte[] result = contractResult.getRight();
        Assert.assertEquals(0, result[31]);
      }
    }
  }

  @Test
  public void verifyBurnWrongProof() throws ZksnarkException {
    int totalCountNum = 1;
    long leafCount = 0;
    long value = 100L;
    byte[] frontier = new byte[32 * 33];

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      SpendingKey senderSk = SpendingKey.random();
      ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
      FullViewingKey senderFvk = senderSk.fullViewingKey();
      byte[] senderOvk = senderFvk.getOvk();
      IncomingViewingKey senderIvk = senderFvk.inViewingKey();
      byte[] rcm = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm);
      PaymentAddress senderPaymentAddress = senderIvk.address(DiversifierT.random()).get();

      { //for mint
        ShieldedTRC20ParametersBuilder mintBuilder = new ShieldedTRC20ParametersBuilder();
        mintBuilder.setTransparentFromAmount(BigInteger.valueOf(value));
        mintBuilder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder.addOutput(DEFAULT_OVK, senderPaymentAddress.getD(),
            senderPaymentAddress.getPkD(), value, rcm, new byte[512]);
        ShieldedTRC20Parameters mintParams = mintBuilder.build(false);

        byte[] mintInputData = abiEncodeForMint(mintParams, value, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult = mintContract.execute(mintInputData);
        byte[] mintResult = mintContractResult.getRight();
        Assert.assertEquals(1, mintResult[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult[32] == 0) {
          System.arraycopy(mintInputData, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult[32] - 1) * 32 + 33;
          int destPos = mintResult[32] * 32;
          System.arraycopy(mintResult, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for burn
        ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
        builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.BURN);
        builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        builder.setTransparentToAmount(BigInteger.valueOf(value));
        builder.setTransparentToAddress(PUBLIC_TO_ADDRESS);
        //spendNote1
        Note senderNote = new Note(senderPaymentAddress.getD(), senderPaymentAddress.getPkD(),
            value, rcm, new byte[512]);
        byte[][] cm = new byte[1][32];
        System.arraycopy(senderNote.cm(), 0, cm[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher = addSimpleMerkleVoucherContainer(tree, cm);
        byte[] path = decodePath(voucher.path().encode());
        byte[] anchor = voucher.root().getContent().toByteArray();
        long position = voucher.position();
        builder.addSpend(senderExpsk, senderNote, anchor, path, position);
        ShieldedTRC20Parameters params = builder.build(true);

        byte[] inputData = abiEncodeForBurnWrongProof(params, value);
        Pair<Boolean, byte[]> contractResult = burnContract.execute(inputData);
        byte[] result = contractResult.getRight();
        Assert.assertEquals(0, result[31]);
      }
    }
  }

  @Test
  public void verifyBurnWrongAuthoritySingature() throws ZksnarkException {
    int totalCountNum = 1;
    long leafCount = 0;
    long value = 100L;
    byte[] frontier = new byte[32 * 33];

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      SpendingKey senderSk = SpendingKey.random();
      ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
      FullViewingKey senderFvk = senderSk.fullViewingKey();
      byte[] senderOvk = senderFvk.getOvk();
      IncomingViewingKey senderIvk = senderFvk.inViewingKey();
      byte[] rcm = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm);
      PaymentAddress senderPaymentAddress = senderIvk.address(DiversifierT.random()).get();

      { //for mint
        ShieldedTRC20ParametersBuilder mintBuilder = new ShieldedTRC20ParametersBuilder();
        mintBuilder.setTransparentFromAmount(BigInteger.valueOf(value));
        mintBuilder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder.addOutput(DEFAULT_OVK, senderPaymentAddress.getD(),
            senderPaymentAddress.getPkD(), value, rcm, new byte[512]);
        ShieldedTRC20Parameters mintParams = mintBuilder.build(false);

        byte[] mintInputData = abiEncodeForMint(mintParams, value, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult = mintContract.execute(mintInputData);
        byte[] mintResult = mintContractResult.getRight();
        Assert.assertEquals(1, mintResult[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult[32] == 0) {
          System.arraycopy(mintInputData, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult[32] - 1) * 32 + 33;
          int destPos = mintResult[32] * 32;
          System.arraycopy(mintResult, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for burn
        ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
        builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.BURN);
        builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        builder.setTransparentToAmount(BigInteger.valueOf(value));
        builder.setTransparentToAddress(PUBLIC_TO_ADDRESS);
        //spendNote1
        Note senderNote = new Note(senderPaymentAddress.getD(), senderPaymentAddress.getPkD(),
            value, rcm, new byte[512]);
        byte[][] cm = new byte[1][32];
        System.arraycopy(senderNote.cm(), 0, cm[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher = addSimpleMerkleVoucherContainer(tree, cm);
        byte[] path = decodePath(voucher.path().encode());
        byte[] anchor = voucher.root().getContent().toByteArray();
        long position = voucher.position();
        builder.addSpend(senderExpsk, senderNote, anchor, path, position);
        ShieldedTRC20Parameters params = builder.build(true);

        byte[] inputData = abiEncodeForBurnWrongAuthoritySignature(params, value);
        Pair<Boolean, byte[]> contractResult = burnContract.execute(inputData);
        byte[] result = contractResult.getRight();
        Assert.assertEquals(0, result[31]);
      }
    }
  }

  @Test
  public void verifyBurnWrongBindingSingature() throws ZksnarkException {
    int totalCountNum = 1;
    long leafCount = 0;
    long value = 100L;
    byte[] frontier = new byte[32 * 33];

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      SpendingKey senderSk = SpendingKey.random();
      ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
      FullViewingKey senderFvk = senderSk.fullViewingKey();
      byte[] senderOvk = senderFvk.getOvk();
      IncomingViewingKey senderIvk = senderFvk.inViewingKey();
      byte[] rcm = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm);
      PaymentAddress senderPaymentAddress = senderIvk.address(DiversifierT.random()).get();

      { //for mint
        ShieldedTRC20ParametersBuilder mintBuilder = new ShieldedTRC20ParametersBuilder();
        mintBuilder.setTransparentFromAmount(BigInteger.valueOf(value));
        mintBuilder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder.addOutput(DEFAULT_OVK, senderPaymentAddress.getD(),
            senderPaymentAddress.getPkD(), value, rcm, new byte[512]);
        ShieldedTRC20Parameters mintParams = mintBuilder.build(false);

        byte[] mintInputData = abiEncodeForMint(mintParams, value, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult = mintContract.execute(mintInputData);
        byte[] mintResult = mintContractResult.getRight();
        Assert.assertEquals(1, mintResult[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult[32] == 0) {
          System.arraycopy(mintInputData, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult[32] - 1) * 32 + 33;
          int destPos = mintResult[32] * 32;
          System.arraycopy(mintResult, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for burn
        ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
        builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.BURN);
        builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        builder.setTransparentToAmount(BigInteger.valueOf(value));
        builder.setTransparentToAddress(PUBLIC_TO_ADDRESS);
        //spendNote1
        Note senderNote = new Note(senderPaymentAddress.getD(), senderPaymentAddress.getPkD(),
            value, rcm, new byte[512]);
        byte[][] cm = new byte[1][32];
        System.arraycopy(senderNote.cm(), 0, cm[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher = addSimpleMerkleVoucherContainer(tree, cm);
        byte[] path = decodePath(voucher.path().encode());
        byte[] anchor = voucher.root().getContent().toByteArray();
        long position = voucher.position();
        builder.addSpend(senderExpsk, senderNote, anchor, path, position);
        ShieldedTRC20Parameters params = builder.build(true);

        byte[] inputData = abiEncodeForBurnWrongBingSignature(params, value);
        Pair<Boolean, byte[]> contractResult = burnContract.execute(inputData);
        byte[] result = contractResult.getRight();
        Assert.assertEquals(0, result[31]);
      }
    }
  }

  @Test
  public void verifyBurnWrongHash() throws ZksnarkException {
    int totalCountNum = 1;
    long leafCount = 0;
    long value = 100L;
    byte[] frontier = new byte[32 * 33];

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      SpendingKey senderSk = SpendingKey.random();
      ExpandedSpendingKey senderExpsk = senderSk.expandedSpendingKey();
      FullViewingKey senderFvk = senderSk.fullViewingKey();
      byte[] senderOvk = senderFvk.getOvk();
      IncomingViewingKey senderIvk = senderFvk.inViewingKey();
      byte[] rcm = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(rcm);
      PaymentAddress senderPaymentAddress = senderIvk.address(DiversifierT.random()).get();

      { //for mint
        ShieldedTRC20ParametersBuilder mintBuilder = new ShieldedTRC20ParametersBuilder();
        mintBuilder.setTransparentFromAmount(BigInteger.valueOf(value));
        mintBuilder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        mintBuilder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.MINT);
        mintBuilder.addOutput(DEFAULT_OVK, senderPaymentAddress.getD(),
            senderPaymentAddress.getPkD(), value, rcm, new byte[512]);
        ShieldedTRC20Parameters mintParams = mintBuilder.build(false);

        byte[] mintInputData = abiEncodeForMint(mintParams, value, frontier, leafCount);
        Pair<Boolean, byte[]> mintContractResult = mintContract.execute(mintInputData);
        byte[] mintResult = mintContractResult.getRight();
        Assert.assertEquals(1, mintResult[31]);

        //update frontier and leafCount
        //if slot == 0, frontier[0:31]=noteCommitment
        if (mintResult[32] == 0) {
          System.arraycopy(mintInputData, 0, frontier, 0, 32);
        } else {
          int srcPos = (mintResult[32] - 1) * 32 + 33;
          int destPos = mintResult[32] * 32;
          System.arraycopy(mintResult, srcPos, frontier, destPos, 32);
        }
        leafCount++;
      }

      { //for burn
        ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
        builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.BURN);
        builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        builder.setTransparentToAmount(BigInteger.valueOf(value));
        builder.setTransparentToAddress(PUBLIC_TO_ADDRESS);
        //spendNote1
        Note senderNote = new Note(senderPaymentAddress.getD(), senderPaymentAddress.getPkD(),
            value, rcm, new byte[512]);
        byte[][] cm = new byte[1][32];
        System.arraycopy(senderNote.cm(), 0, cm[0], 0, 32);
        IncrementalMerkleVoucherContainer voucher = addSimpleMerkleVoucherContainer(tree, cm);
        byte[] path = decodePath(voucher.path().encode());
        byte[] anchor = voucher.root().getContent().toByteArray();
        long position = voucher.position();
        builder.addSpend(senderExpsk, senderNote, anchor, path, position);
        ShieldedTRC20Parameters params = builder.build(true);

        byte[] inputData = abiEncodeForBurnWrongHash(params, value);
        Pair<Boolean, byte[]> contractResult = burnContract.execute(inputData);
        byte[] result = contractResult.getRight();
        Assert.assertEquals(0, result[31]);
      }
    }
  }

  private Pair<Boolean, byte[]> verifyTransfer(byte[] input) {
    transferContract.getEnergyForData(input);
    transferContract.setVmShouldEndInUs(System.nanoTime() / 1000 + 50 * 1000);
    Pair<Boolean, byte[]> ret = transferContract.execute(input);
    return ret;
  }

  private IncrementalMerkleVoucherContainer addSimpleMerkleVoucherContainer(
      IncrementalMerkleTreeContainer tree, byte[][] cm)
      throws ZksnarkException {
    for (int i = 0; i < cm.length; i++) {
      PedersenHashCapsule compressCapsule = new PedersenHashCapsule();
      compressCapsule.setContent(ByteString.copyFrom(cm[i]));
      ShieldContract.PedersenHash a = compressCapsule.getInstance();
      tree.append(a);
    }
    IncrementalMerkleVoucherContainer voucher = tree.toVoucher();
    return voucher;
  }

  private byte[] decodePath(byte[] encodedPath) {
    Assert.assertEquals(1065, encodedPath.length);
    byte[] path = new byte[32 * 32];
    for (int i = 0; i < 32; i++) {
      System.arraycopy(encodedPath, (i * 33) + 2, path, i * 32, 32);
    }
    return path;
  }


  private byte[] abiEncodeForMint(ShieldedTRC20Parameters params, long value,
      byte[] frontier, long leafCount) {
    byte[] mergedBytes;
    ShieldContract.ReceiveDescription revDesc = params.getReceiveDescription(0);
    mergedBytes = ByteUtil.merge(
        revDesc.getNoteCommitment().toByteArray(),
        revDesc.getValueCommitment().toByteArray(),
        revDesc.getEpk().toByteArray(),
        revDesc.getZkproof().toByteArray(),
        params.getBindingSignature().toByteArray(),
        longTo32Bytes(value),
        params.getMessageHash().toByteArray(),
        frontier,
        longTo32Bytes(leafCount)
    );
    return mergedBytes;
  }

  private byte[] abiEncodeForMintWrongCM(ShieldedTRC20Parameters params, long value,
      byte[] frontier, long leafCount) {
    byte[] mergedBytes;
    ShieldContract.ReceiveDescription revDesc = params.getReceiveDescription(0);
    mergedBytes = ByteUtil.merge(
        Wallet.generateRandomBytes(32),
        revDesc.getValueCommitment().toByteArray(),
        revDesc.getEpk().toByteArray(),
        revDesc.getZkproof().toByteArray(),
        params.getBindingSignature().toByteArray(),
        longTo32Bytes(value),
        params.getMessageHash().toByteArray(),
        frontier,
        longTo32Bytes(leafCount)
    );
    return mergedBytes;
  }

  private byte[] abiEncodeForMintWrongCV(ShieldedTRC20Parameters params, long value,
      byte[] frontier, long leafCount) {
    byte[] mergedBytes;
    ShieldContract.ReceiveDescription revDesc = params.getReceiveDescription(0);
    mergedBytes = ByteUtil.merge(
        revDesc.getNoteCommitment().toByteArray(),
        Wallet.generateRandomBytes(32),
        revDesc.getEpk().toByteArray(),
        revDesc.getZkproof().toByteArray(),
        params.getBindingSignature().toByteArray(),
        longTo32Bytes(value),
        params.getMessageHash().toByteArray(),
        frontier,
        longTo32Bytes(leafCount)
    );
    return mergedBytes;
  }

  private byte[] abiEncodeForMintWrongEpk(ShieldedTRC20Parameters params, long value,
      byte[] frontier, long leafCount) {
    byte[] mergedBytes;
    ShieldContract.ReceiveDescription revDesc = params.getReceiveDescription(0);
    mergedBytes = ByteUtil.merge(
        revDesc.getNoteCommitment().toByteArray(),
        revDesc.getValueCommitment().toByteArray(),
        Wallet.generateRandomBytes(32),
        revDesc.getZkproof().toByteArray(),
        params.getBindingSignature().toByteArray(),
        longTo32Bytes(value),
        params.getMessageHash().toByteArray(),
        frontier,
        longTo32Bytes(leafCount)
    );
    return mergedBytes;
  }

  private byte[] abiEncodeForMintWrongProof(ShieldedTRC20Parameters params, long value,
      byte[] frontier, long leafCount) {
    byte[] mergedBytes;
    ShieldContract.ReceiveDescription revDesc = params.getReceiveDescription(0);
    mergedBytes = ByteUtil.merge(
        revDesc.getNoteCommitment().toByteArray(),
        revDesc.getValueCommitment().toByteArray(),
        revDesc.getEpk().toByteArray(),
        Wallet.generateRandomBytes(192),
        params.getBindingSignature().toByteArray(),
        longTo32Bytes(value),
        params.getMessageHash().toByteArray(),
        frontier,
        longTo32Bytes(leafCount)
    );
    return mergedBytes;
  }

  private byte[] abiEncodeForMintWrongBindingSignature(ShieldedTRC20Parameters params, long value,
      byte[] frontier, long leafCount) {
    byte[] mergedBytes;
    ShieldContract.ReceiveDescription revDesc = params.getReceiveDescription(0);
    mergedBytes = ByteUtil.merge(
        revDesc.getNoteCommitment().toByteArray(),
        revDesc.getValueCommitment().toByteArray(),
        revDesc.getEpk().toByteArray(),
        revDesc.getZkproof().toByteArray(),
        Wallet.generateRandomBytes(64),
        longTo32Bytes(value),
        params.getMessageHash().toByteArray(),
        frontier,
        longTo32Bytes(leafCount)
    );
    return mergedBytes;
  }

  private byte[] abiEncodeForMintWrongHash(ShieldedTRC20Parameters params, long value,
      byte[] frontier, long leafCount) {
    byte[] mergedBytes;
    ShieldContract.ReceiveDescription revDesc = params.getReceiveDescription(0);
    mergedBytes = ByteUtil.merge(
        revDesc.getNoteCommitment().toByteArray(),
        revDesc.getValueCommitment().toByteArray(),
        revDesc.getEpk().toByteArray(),
        revDesc.getZkproof().toByteArray(),
        params.getBindingSignature().toByteArray(),
        longTo32Bytes(value),
        Wallet.generateRandomBytes(32),
        frontier,
        longTo32Bytes(leafCount)
    );
    return mergedBytes;
  }

  private byte[] abiEncodeForTransfer(ShieldedTRC20Parameters params, byte[] frontier,
      long leafCount, long valueBalance) {
    byte[] input = new byte[0];
    byte[] spendAuthSig = new byte[0];
    byte[] output = new byte[0];
    byte[] mergedBytes;
    List<ShieldContract.SpendDescription> spendDescs = params.getSpendDescriptionList();
    for (ShieldContract.SpendDescription spendDesc : spendDescs) {
      input = ByteUtil.merge(input,
          spendDesc.getNullifier().toByteArray(),
          spendDesc.getAnchor().toByteArray(),
          spendDesc.getValueCommitment().toByteArray(),
          spendDesc.getRk().toByteArray(),
          spendDesc.getZkproof().toByteArray()
      );
      spendAuthSig = ByteUtil.merge(
          spendAuthSig, spendDesc.getSpendAuthoritySignature().toByteArray());
    }
    byte[] inputOffsetbytes = longTo32Bytes(1312);
    long spendCount = spendDescs.size();
    byte[] spendCountBytes = longTo32Bytes(spendCount);
    byte[] authOffsetBytes = longTo32Bytes(1312 + 32 + 320 * spendCount);
    List<ShieldContract.ReceiveDescription> recvDescs = params.getReceiveDescriptionList();
    for (ShieldContract.ReceiveDescription recvDesc : recvDescs) {
      output = ByteUtil.merge(output,
          recvDesc.getNoteCommitment().toByteArray(),
          recvDesc.getValueCommitment().toByteArray(),
          recvDesc.getEpk().toByteArray(),
          recvDesc.getZkproof().toByteArray()
      );
    }
    long recvCount = recvDescs.size();
    byte[] recvCountBytes = longTo32Bytes(recvCount);
    byte[] outputOffsetbytes = longTo32Bytes(1312 + 32 + 320 * spendCount + 32 + 64 * spendCount);
    mergedBytes = ByteUtil.merge(inputOffsetbytes,
        authOffsetBytes,
        outputOffsetbytes,
        params.getBindingSignature().toByteArray(),
        params.getMessageHash().toByteArray(),
        longTo32Bytes(valueBalance),
        frontier,
        longTo32Bytes(leafCount),
        spendCountBytes,
        input,
        spendCountBytes,
        spendAuthSig,
        recvCountBytes,
        output
    );
    return mergedBytes;
  }

  private byte[] abiEncodeForTransferWrongNf(ShieldedTRC20Parameters params, byte[] frontier,
      long leafCount) {
    byte[] input = new byte[0];
    byte[] spendAuthSig = new byte[0];
    byte[] output = new byte[0];
    byte[] mergedBytes;
    List<ShieldContract.SpendDescription> spendDescs = params.getSpendDescriptionList();
    for (ShieldContract.SpendDescription spendDesc : spendDescs) {
      input = ByteUtil.merge(input,
          Wallet.generateRandomBytes(32),
          spendDesc.getAnchor().toByteArray(),
          spendDesc.getValueCommitment().toByteArray(),
          spendDesc.getRk().toByteArray(),
          spendDesc.getZkproof().toByteArray()
      );
      spendAuthSig = ByteUtil.merge(
          spendAuthSig, spendDesc.getSpendAuthoritySignature().toByteArray());
    }
    byte[] inputOffsetbytes = longTo32Bytes(1312);
    long spendCount = spendDescs.size();
    byte[] spendCountBytes = longTo32Bytes(spendCount);
    byte[] authOffsetBytes = longTo32Bytes(1312 + 32 + 320 * spendCount);
    List<ShieldContract.ReceiveDescription> recvDescs = params.getReceiveDescriptionList();
    for (ShieldContract.ReceiveDescription recvDesc : recvDescs) {
      output = ByteUtil.merge(output,
          recvDesc.getNoteCommitment().toByteArray(),
          recvDesc.getValueCommitment().toByteArray(),
          recvDesc.getEpk().toByteArray(),
          recvDesc.getZkproof().toByteArray()
      );
    }
    long recvCount = recvDescs.size();
    byte[] recvCountBytes = longTo32Bytes(recvCount);
    byte[] outputOffsetbytes = longTo32Bytes(1312 + 32 + 320 * spendCount + 32 + 64 * spendCount);
    mergedBytes = ByteUtil.merge(inputOffsetbytes,
        authOffsetBytes,
        outputOffsetbytes,
        params.getBindingSignature().toByteArray(),
        params.getMessageHash().toByteArray(),
        longTo32Bytes(0),
        frontier,
        longTo32Bytes(leafCount),
        spendCountBytes,
        input,
        spendCountBytes,
        spendAuthSig,
        recvCountBytes,
        output
    );
    return mergedBytes;
  }

  private byte[] abiEncodeForTransferWrongRoot(ShieldedTRC20Parameters params, byte[] frontier,
      long leafCount) {
    byte[] input = new byte[0];
    byte[] spendAuthSig = new byte[0];
    byte[] output = new byte[0];
    byte[] mergedBytes;
    List<ShieldContract.SpendDescription> spendDescs = params.getSpendDescriptionList();
    for (ShieldContract.SpendDescription spendDesc : spendDescs) {
      input = ByteUtil.merge(input,
          spendDesc.getNullifier().toByteArray(),
          Wallet.generateRandomBytes(32),
          spendDesc.getValueCommitment().toByteArray(),
          spendDesc.getRk().toByteArray(),
          spendDesc.getZkproof().toByteArray()
      );
      spendAuthSig = ByteUtil.merge(
          spendAuthSig, spendDesc.getSpendAuthoritySignature().toByteArray());
    }
    byte[] inputOffsetbytes = longTo32Bytes(1312);
    long spendCount = spendDescs.size();
    byte[] spendCountBytes = longTo32Bytes(spendCount);
    byte[] authOffsetBytes = longTo32Bytes(1312 + 32 + 320 * spendCount);
    List<ShieldContract.ReceiveDescription> recvDescs = params.getReceiveDescriptionList();
    for (ShieldContract.ReceiveDescription recvDesc : recvDescs) {
      output = ByteUtil.merge(output,
          recvDesc.getNoteCommitment().toByteArray(),
          recvDesc.getValueCommitment().toByteArray(),
          recvDesc.getEpk().toByteArray(),
          recvDesc.getZkproof().toByteArray()
      );
    }
    long recvCount = recvDescs.size();
    byte[] recvCountBytes = longTo32Bytes(recvCount);
    byte[] outputOffsetbytes = longTo32Bytes(1312 + 32 + 320 * spendCount + 32 + 64 * spendCount);
    mergedBytes = ByteUtil.merge(inputOffsetbytes,
        authOffsetBytes,
        outputOffsetbytes,
        params.getBindingSignature().toByteArray(),
        params.getMessageHash().toByteArray(),
        longTo32Bytes(0),
        frontier,
        longTo32Bytes(leafCount),
        spendCountBytes,
        input,
        spendCountBytes,
        spendAuthSig,
        recvCountBytes,
        output
    );
    return mergedBytes;
  }


  private byte[] abiEncodeForTransferWrongSpendCV(ShieldedTRC20Parameters params, byte[] frontier,
      long leafCount) {
    byte[] input = new byte[0];
    byte[] spendAuthSig = new byte[0];
    byte[] output = new byte[0];
    byte[] mergedBytes;
    List<ShieldContract.SpendDescription> spendDescs = params.getSpendDescriptionList();
    for (ShieldContract.SpendDescription spendDesc : spendDescs) {
      input = ByteUtil.merge(input,
          spendDesc.getNullifier().toByteArray(),
          spendDesc.getAnchor().toByteArray(),
          Wallet.generateRandomBytes(32),
          spendDesc.getRk().toByteArray(),
          spendDesc.getZkproof().toByteArray()
      );
      spendAuthSig = ByteUtil.merge(
          spendAuthSig, spendDesc.getSpendAuthoritySignature().toByteArray());
    }
    byte[] inputOffsetbytes = longTo32Bytes(1312);
    long spendCount = spendDescs.size();
    byte[] spendCountBytes = longTo32Bytes(spendCount);
    byte[] authOffsetBytes = longTo32Bytes(1312 + 32 + 320 * spendCount);
    List<ShieldContract.ReceiveDescription> recvDescs = params.getReceiveDescriptionList();
    for (ShieldContract.ReceiveDescription recvDesc : recvDescs) {
      output = ByteUtil.merge(output,
          recvDesc.getNoteCommitment().toByteArray(),
          recvDesc.getValueCommitment().toByteArray(),
          recvDesc.getEpk().toByteArray(),
          recvDesc.getZkproof().toByteArray()
      );
    }
    long recvCount = recvDescs.size();
    byte[] recvCountBytes = longTo32Bytes(recvCount);
    byte[] outputOffsetbytes = longTo32Bytes(1312 + 32 + 320 * spendCount + 32 + 64 * spendCount);
    mergedBytes = ByteUtil.merge(inputOffsetbytes,
        authOffsetBytes,
        outputOffsetbytes,
        params.getBindingSignature().toByteArray(),
        params.getMessageHash().toByteArray(),
        longTo32Bytes(0),
        frontier,
        longTo32Bytes(leafCount),
        spendCountBytes,
        input,
        spendCountBytes,
        spendAuthSig,
        recvCountBytes,
        output
    );
    return mergedBytes;
  }

  private byte[] abiEncodeForTransferWrongRk(ShieldedTRC20Parameters params, byte[] frontier,
      long leafCount) {
    byte[] input = new byte[0];
    byte[] spendAuthSig = new byte[0];
    byte[] output = new byte[0];
    byte[] mergedBytes;
    List<ShieldContract.SpendDescription> spendDescs = params.getSpendDescriptionList();
    for (ShieldContract.SpendDescription spendDesc : spendDescs) {
      input = ByteUtil.merge(input,
          spendDesc.getNullifier().toByteArray(),
          spendDesc.getAnchor().toByteArray(),
          spendDesc.getValueCommitment().toByteArray(),
          Wallet.generateRandomBytes(32),
          spendDesc.getZkproof().toByteArray()
      );
      spendAuthSig = ByteUtil.merge(
          spendAuthSig, spendDesc.getSpendAuthoritySignature().toByteArray());
    }
    byte[] inputOffsetbytes = longTo32Bytes(1312);
    long spendCount = spendDescs.size();
    byte[] spendCountBytes = longTo32Bytes(spendCount);
    byte[] authOffsetBytes = longTo32Bytes(1312 + 32 + 320 * spendCount);
    List<ShieldContract.ReceiveDescription> recvDescs = params.getReceiveDescriptionList();
    for (ShieldContract.ReceiveDescription recvDesc : recvDescs) {
      output = ByteUtil.merge(output,
          recvDesc.getNoteCommitment().toByteArray(),
          recvDesc.getValueCommitment().toByteArray(),
          recvDesc.getEpk().toByteArray(),
          recvDesc.getZkproof().toByteArray()
      );
    }
    long recvCount = recvDescs.size();
    byte[] recvCountBytes = longTo32Bytes(recvCount);
    byte[] outputOffsetbytes = longTo32Bytes(1312 + 32 + 320 * spendCount + 32 + 64 * spendCount);
    mergedBytes = ByteUtil.merge(inputOffsetbytes,
        authOffsetBytes,
        outputOffsetbytes,
        params.getBindingSignature().toByteArray(),
        params.getMessageHash().toByteArray(),
        longTo32Bytes(0),
        frontier,
        longTo32Bytes(leafCount),
        spendCountBytes,
        input,
        spendCountBytes,
        spendAuthSig,
        recvCountBytes,
        output
    );
    return mergedBytes;
  }

  private byte[] abiEncodeForTransferWrongSpendProof(ShieldedTRC20Parameters params,
      byte[] frontier,
      long leafCount) {
    byte[] input = new byte[0];
    byte[] spendAuthSig = new byte[0];
    byte[] output = new byte[0];
    byte[] mergedBytes;
    List<ShieldContract.SpendDescription> spendDescs = params.getSpendDescriptionList();
    for (ShieldContract.SpendDescription spendDesc : spendDescs) {
      input = ByteUtil.merge(input,
          spendDesc.getNullifier().toByteArray(),
          spendDesc.getAnchor().toByteArray(),
          spendDesc.getValueCommitment().toByteArray(),
          spendDesc.getRk().toByteArray(),
          Wallet.generateRandomBytes(192)
      );
      spendAuthSig = ByteUtil.merge(
          spendAuthSig, spendDesc.getSpendAuthoritySignature().toByteArray());
    }
    byte[] inputOffsetbytes = longTo32Bytes(1312);
    long spendCount = spendDescs.size();
    byte[] spendCountBytes = longTo32Bytes(spendCount);
    byte[] authOffsetBytes = longTo32Bytes(1312 + 32 + 320 * spendCount);
    List<ShieldContract.ReceiveDescription> recvDescs = params.getReceiveDescriptionList();
    for (ShieldContract.ReceiveDescription recvDesc : recvDescs) {
      output = ByteUtil.merge(output,
          recvDesc.getNoteCommitment().toByteArray(),
          recvDesc.getValueCommitment().toByteArray(),
          recvDesc.getEpk().toByteArray(),
          recvDesc.getZkproof().toByteArray()
      );
    }
    long recvCount = recvDescs.size();
    byte[] recvCountBytes = longTo32Bytes(recvCount);
    byte[] outputOffsetbytes = longTo32Bytes(1312 + 32 + 320 * spendCount + 32 + 64 * spendCount);
    mergedBytes = ByteUtil.merge(inputOffsetbytes,
        authOffsetBytes,
        outputOffsetbytes,
        params.getBindingSignature().toByteArray(),
        params.getMessageHash().toByteArray(),
        longTo32Bytes(0),
        frontier,
        longTo32Bytes(leafCount),
        spendCountBytes,
        input,
        spendCountBytes,
        spendAuthSig,
        recvCountBytes,
        output
    );
    return mergedBytes;
  }

  private byte[] abiEncodeForTransferWrongCM(ShieldedTRC20Parameters params, byte[] frontier,
      long leafCount) {
    byte[] input = new byte[0];
    byte[] spendAuthSig = new byte[0];
    byte[] output = new byte[0];
    byte[] mergedBytes;
    List<ShieldContract.SpendDescription> spendDescs = params.getSpendDescriptionList();
    for (ShieldContract.SpendDescription spendDesc : spendDescs) {
      input = ByteUtil.merge(input,
          spendDesc.getNullifier().toByteArray(),
          spendDesc.getAnchor().toByteArray(),
          spendDesc.getValueCommitment().toByteArray(),
          spendDesc.getRk().toByteArray(),
          spendDesc.getZkproof().toByteArray()
      );
      spendAuthSig = ByteUtil.merge(
          spendAuthSig, spendDesc.getSpendAuthoritySignature().toByteArray());
    }
    byte[] inputOffsetbytes = longTo32Bytes(1312);
    long spendCount = spendDescs.size();
    byte[] spendCountBytes = longTo32Bytes(spendCount);
    byte[] authOffsetBytes = longTo32Bytes(1312 + 32 + 320 * spendCount);
    List<ShieldContract.ReceiveDescription> recvDescs = params.getReceiveDescriptionList();
    for (ShieldContract.ReceiveDescription recvDesc : recvDescs) {
      output = ByteUtil.merge(output,
          Wallet.generateRandomBytes(32),
          recvDesc.getValueCommitment().toByteArray(),
          recvDesc.getEpk().toByteArray(),
          recvDesc.getZkproof().toByteArray()
      );
    }
    long recvCount = recvDescs.size();
    byte[] recvCountBytes = longTo32Bytes(recvCount);
    byte[] outputOffsetbytes = longTo32Bytes(1312 + 32 + 320 * spendCount + 32 + 64 * spendCount);
    mergedBytes = ByteUtil.merge(inputOffsetbytes,
        authOffsetBytes,
        outputOffsetbytes,
        params.getBindingSignature().toByteArray(),
        params.getMessageHash().toByteArray(),
        longTo32Bytes(0),
        frontier,
        longTo32Bytes(leafCount),
        spendCountBytes,
        input,
        spendCountBytes,
        spendAuthSig,
        recvCountBytes,
        output
    );
    return mergedBytes;
  }


  private byte[] abiEncodeForTransferWrongReceiveCV(ShieldedTRC20Parameters params, byte[] frontier,
      long leafCount) {
    byte[] input = new byte[0];
    byte[] spendAuthSig = new byte[0];
    byte[] output = new byte[0];
    byte[] mergedBytes;
    List<ShieldContract.SpendDescription> spendDescs = params.getSpendDescriptionList();
    for (ShieldContract.SpendDescription spendDesc : spendDescs) {
      input = ByteUtil.merge(input,
          spendDesc.getNullifier().toByteArray(),
          spendDesc.getAnchor().toByteArray(),
          spendDesc.getValueCommitment().toByteArray(),
          spendDesc.getRk().toByteArray(),
          spendDesc.getZkproof().toByteArray()
      );
      spendAuthSig = ByteUtil.merge(
          spendAuthSig, spendDesc.getSpendAuthoritySignature().toByteArray());
    }
    byte[] inputOffsetbytes = longTo32Bytes(1312);
    long spendCount = spendDescs.size();
    byte[] spendCountBytes = longTo32Bytes(spendCount);
    byte[] authOffsetBytes = longTo32Bytes(1312 + 32 + 320 * spendCount);
    List<ShieldContract.ReceiveDescription> recvDescs = params.getReceiveDescriptionList();
    for (ShieldContract.ReceiveDescription recvDesc : recvDescs) {
      output = ByteUtil.merge(output,
          recvDesc.getNoteCommitment().toByteArray(),
          Wallet.generateRandomBytes(32),
          recvDesc.getEpk().toByteArray(),
          recvDesc.getZkproof().toByteArray()
      );
    }
    long recvCount = recvDescs.size();
    byte[] recvCountBytes = longTo32Bytes(recvCount);
    byte[] outputOffsetbytes = longTo32Bytes(1312 + 32 + 320 * spendCount + 32 + 64 * spendCount);
    mergedBytes = ByteUtil.merge(inputOffsetbytes,
        authOffsetBytes,
        outputOffsetbytes,
        params.getBindingSignature().toByteArray(),
        params.getMessageHash().toByteArray(),
        longTo32Bytes(0),
        frontier,
        longTo32Bytes(leafCount),
        spendCountBytes,
        input,
        spendCountBytes,
        spendAuthSig,
        recvCountBytes,
        output
    );
    return mergedBytes;
  }

  private byte[] abiEncodeForTransferWrongEpk(ShieldedTRC20Parameters params, byte[] frontier,
      long leafCount) {
    byte[] input = new byte[0];
    byte[] spendAuthSig = new byte[0];
    byte[] output = new byte[0];
    byte[] mergedBytes;
    List<ShieldContract.SpendDescription> spendDescs = params.getSpendDescriptionList();
    for (ShieldContract.SpendDescription spendDesc : spendDescs) {
      input = ByteUtil.merge(input,
          spendDesc.getNullifier().toByteArray(),
          spendDesc.getAnchor().toByteArray(),
          spendDesc.getValueCommitment().toByteArray(),
          spendDesc.getRk().toByteArray(),
          spendDesc.getZkproof().toByteArray()
      );
      spendAuthSig = ByteUtil.merge(
          spendAuthSig, spendDesc.getSpendAuthoritySignature().toByteArray());
    }
    byte[] inputOffsetbytes = longTo32Bytes(1312);
    long spendCount = spendDescs.size();
    byte[] spendCountBytes = longTo32Bytes(spendCount);
    byte[] authOffsetBytes = longTo32Bytes(1312 + 32 + 320 * spendCount);
    List<ShieldContract.ReceiveDescription> recvDescs = params.getReceiveDescriptionList();
    for (ShieldContract.ReceiveDescription recvDesc : recvDescs) {
      output = ByteUtil.merge(output,
          recvDesc.getNoteCommitment().toByteArray(),
          recvDesc.getValueCommitment().toByteArray(),
          Wallet.generateRandomBytes(32),
          recvDesc.getZkproof().toByteArray()
      );
    }
    long recvCount = recvDescs.size();
    byte[] recvCountBytes = longTo32Bytes(recvCount);
    byte[] outputOffsetbytes = longTo32Bytes(1312 + 32 + 320 * spendCount + 32 + 64 * spendCount);
    mergedBytes = ByteUtil.merge(inputOffsetbytes,
        authOffsetBytes,
        outputOffsetbytes,
        params.getBindingSignature().toByteArray(),
        params.getMessageHash().toByteArray(),
        longTo32Bytes(0),
        frontier,
        longTo32Bytes(leafCount),
        spendCountBytes,
        input,
        spendCountBytes,
        spendAuthSig,
        recvCountBytes,
        output
    );
    return mergedBytes;
  }


  private byte[] abiEncodeForTransferWrongReceivProof(ShieldedTRC20Parameters params,
      byte[] frontier,
      long leafCount) {
    byte[] input = new byte[0];
    byte[] spendAuthSig = new byte[0];
    byte[] output = new byte[0];
    byte[] mergedBytes;
    List<ShieldContract.SpendDescription> spendDescs = params.getSpendDescriptionList();
    for (ShieldContract.SpendDescription spendDesc : spendDescs) {
      input = ByteUtil.merge(input,
          spendDesc.getNullifier().toByteArray(),
          spendDesc.getAnchor().toByteArray(),
          spendDesc.getValueCommitment().toByteArray(),
          spendDesc.getRk().toByteArray(),
          spendDesc.getZkproof().toByteArray()
      );
      spendAuthSig = ByteUtil.merge(
          spendAuthSig, spendDesc.getSpendAuthoritySignature().toByteArray());
    }
    byte[] inputOffsetbytes = longTo32Bytes(1312);
    long spendCount = spendDescs.size();
    byte[] spendCountBytes = longTo32Bytes(spendCount);
    byte[] authOffsetBytes = longTo32Bytes(1312 + 32 + 320 * spendCount);
    List<ShieldContract.ReceiveDescription> recvDescs = params.getReceiveDescriptionList();
    for (ShieldContract.ReceiveDescription recvDesc : recvDescs) {
      output = ByteUtil.merge(output,
          recvDesc.getNoteCommitment().toByteArray(),
          recvDesc.getValueCommitment().toByteArray(),
          recvDesc.getEpk().toByteArray(),
          Wallet.generateRandomBytes(192)
      );
    }
    long recvCount = recvDescs.size();
    byte[] recvCountBytes = longTo32Bytes(recvCount);
    byte[] outputOffsetbytes = longTo32Bytes(1312 + 32 + 320 * spendCount + 32 + 64 * spendCount);
    mergedBytes = ByteUtil.merge(inputOffsetbytes,
        authOffsetBytes,
        outputOffsetbytes,
        params.getBindingSignature().toByteArray(),
        params.getMessageHash().toByteArray(),
        longTo32Bytes(0),
        frontier,
        longTo32Bytes(leafCount),
        spendCountBytes,
        input,
        spendCountBytes,
        spendAuthSig,
        recvCountBytes,
        output
    );
    return mergedBytes;
  }

  private byte[] abiEncodeForTransferWrongBindingSignature(ShieldedTRC20Parameters params,
      byte[] frontier,
      long leafCount) {
    byte[] input = new byte[0];
    byte[] spendAuthSig = new byte[0];
    byte[] output = new byte[0];
    byte[] mergedBytes;
    List<ShieldContract.SpendDescription> spendDescs = params.getSpendDescriptionList();
    for (ShieldContract.SpendDescription spendDesc : spendDescs) {
      input = ByteUtil.merge(input,
          spendDesc.getNullifier().toByteArray(),
          spendDesc.getAnchor().toByteArray(),
          spendDesc.getValueCommitment().toByteArray(),
          spendDesc.getRk().toByteArray(),
          spendDesc.getZkproof().toByteArray()
      );
      spendAuthSig = ByteUtil.merge(
          spendAuthSig, spendDesc.getSpendAuthoritySignature().toByteArray());
    }
    byte[] inputOffsetbytes = longTo32Bytes(1312);
    long spendCount = spendDescs.size();
    byte[] spendCountBytes = longTo32Bytes(spendCount);
    byte[] authOffsetBytes = longTo32Bytes(1312 + 32 + 320 * spendCount);
    List<ShieldContract.ReceiveDescription> recvDescs = params.getReceiveDescriptionList();
    for (ShieldContract.ReceiveDescription recvDesc : recvDescs) {
      output = ByteUtil.merge(output,
          recvDesc.getNoteCommitment().toByteArray(),
          recvDesc.getValueCommitment().toByteArray(),
          recvDesc.getEpk().toByteArray(),
          recvDesc.getZkproof().toByteArray()
      );
    }
    long recvCount = recvDescs.size();
    byte[] recvCountBytes = longTo32Bytes(recvCount);
    byte[] outputOffsetbytes = longTo32Bytes(1312 + 32 + 320 * spendCount + 32 + 64 * spendCount);
    mergedBytes = ByteUtil.merge(inputOffsetbytes,
        authOffsetBytes,
        outputOffsetbytes,
        Wallet.generateRandomBytes(64),
        params.getMessageHash().toByteArray(),
        longTo32Bytes(0),
        frontier,
        longTo32Bytes(leafCount),
        spendCountBytes,
        input,
        spendCountBytes,
        spendAuthSig,
        recvCountBytes,
        output
    );
    return mergedBytes;
  }


  private byte[] abiEncodeForTransferWrongHash(ShieldedTRC20Parameters params, byte[] frontier,
      long leafCount) {
    byte[] input = new byte[0];
    byte[] spendAuthSig = new byte[0];
    byte[] output = new byte[0];
    byte[] mergedBytes;
    List<ShieldContract.SpendDescription> spendDescs = params.getSpendDescriptionList();
    for (ShieldContract.SpendDescription spendDesc : spendDescs) {
      input = ByteUtil.merge(input,
          spendDesc.getNullifier().toByteArray(),
          spendDesc.getAnchor().toByteArray(),
          spendDesc.getValueCommitment().toByteArray(),
          spendDesc.getRk().toByteArray(),
          spendDesc.getZkproof().toByteArray()
      );
      spendAuthSig = ByteUtil.merge(
          spendAuthSig, spendDesc.getSpendAuthoritySignature().toByteArray());
    }
    byte[] inputOffsetbytes = longTo32Bytes(1312);
    long spendCount = spendDescs.size();
    byte[] spendCountBytes = longTo32Bytes(spendCount);
    byte[] authOffsetBytes = longTo32Bytes(1312 + 32 + 320 * spendCount);
    List<ShieldContract.ReceiveDescription> recvDescs = params.getReceiveDescriptionList();
    for (ShieldContract.ReceiveDescription recvDesc : recvDescs) {
      output = ByteUtil.merge(output,
          recvDesc.getNoteCommitment().toByteArray(),
          recvDesc.getValueCommitment().toByteArray(),
          recvDesc.getEpk().toByteArray(),
          recvDesc.getZkproof().toByteArray()
      );
    }
    long recvCount = recvDescs.size();
    byte[] recvCountBytes = longTo32Bytes(recvCount);
    byte[] outputOffsetbytes = longTo32Bytes(1312 + 32 + 320 * spendCount + 32 + 64 * spendCount);
    mergedBytes = ByteUtil.merge(inputOffsetbytes,
        authOffsetBytes,
        outputOffsetbytes,
        params.getBindingSignature().toByteArray(),
        longTo32Bytes(0),
        Wallet.generateRandomBytes(32),
        frontier,
        longTo32Bytes(leafCount),
        spendCountBytes,
        input,
        spendCountBytes,
        spendAuthSig,
        recvCountBytes,
        output
    );
    return mergedBytes;
  }

  private byte[] abiEncodeForBurn(ShieldedTRC20Parameters params, long value) {
    byte[] mergedBytes;
    ShieldContract.SpendDescription spendDesc = params.getSpendDescription(0);
    mergedBytes = ByteUtil.merge(
        spendDesc.getNullifier().toByteArray(),
        spendDesc.getAnchor().toByteArray(),
        spendDesc.getValueCommitment().toByteArray(),
        spendDesc.getRk().toByteArray(),
        spendDesc.getZkproof().toByteArray(),
        spendDesc.getSpendAuthoritySignature().toByteArray(),
        longTo32Bytes(value),
        params.getBindingSignature().toByteArray(),
        params.getMessageHash().toByteArray()
    );
    return mergedBytes;
  }

  private byte[] abiEncodeForBurnWrongNF(ShieldedTRC20Parameters params, long value) {
    byte[] mergedBytes;
    ShieldContract.SpendDescription spendDesc = params.getSpendDescription(0);
    mergedBytes = ByteUtil.merge(
        Wallet.generateRandomBytes(32),
        spendDesc.getAnchor().toByteArray(),
        spendDesc.getValueCommitment().toByteArray(),
        spendDesc.getRk().toByteArray(),
        spendDesc.getZkproof().toByteArray(),
        spendDesc.getSpendAuthoritySignature().toByteArray(),
        longTo32Bytes(value),
        params.getBindingSignature().toByteArray(),
        params.getMessageHash().toByteArray()
    );
    return mergedBytes;
  }

  private byte[] abiEncodeForBurnWrongRoot(ShieldedTRC20Parameters params, long value) {
    byte[] mergedBytes;
    ShieldContract.SpendDescription spendDesc = params.getSpendDescription(0);
    mergedBytes = ByteUtil.merge(
        spendDesc.getNullifier().toByteArray(),
        Wallet.generateRandomBytes(32),
        spendDesc.getValueCommitment().toByteArray(),
        spendDesc.getRk().toByteArray(),
        spendDesc.getZkproof().toByteArray(),
        spendDesc.getSpendAuthoritySignature().toByteArray(),
        longTo32Bytes(value),
        params.getBindingSignature().toByteArray(),
        params.getMessageHash().toByteArray()
    );
    return mergedBytes;
  }

  private byte[] abiEncodeForBurnWrongCV(ShieldedTRC20Parameters params, long value) {
    byte[] mergedBytes;
    ShieldContract.SpendDescription spendDesc = params.getSpendDescription(0);
    mergedBytes = ByteUtil.merge(
        spendDesc.getNullifier().toByteArray(),
        spendDesc.getAnchor().toByteArray(),
        Wallet.generateRandomBytes(32),
        spendDesc.getRk().toByteArray(),
        spendDesc.getZkproof().toByteArray(),
        spendDesc.getSpendAuthoritySignature().toByteArray(),
        longTo32Bytes(value),
        params.getBindingSignature().toByteArray(),
        params.getMessageHash().toByteArray()
    );
    return mergedBytes;
  }

  private byte[] abiEncodeForBurnWrongRK(ShieldedTRC20Parameters params, long value) {
    byte[] mergedBytes;
    ShieldContract.SpendDescription spendDesc = params.getSpendDescription(0);
    mergedBytes = ByteUtil.merge(
        spendDesc.getNullifier().toByteArray(),
        spendDesc.getAnchor().toByteArray(),
        spendDesc.getValueCommitment().toByteArray(),
        Wallet.generateRandomBytes(32),
        spendDesc.getZkproof().toByteArray(),
        spendDesc.getSpendAuthoritySignature().toByteArray(),
        longTo32Bytes(value),
        params.getBindingSignature().toByteArray(),
        params.getMessageHash().toByteArray()
    );
    return mergedBytes;
  }

  private byte[] abiEncodeForBurnWrongProof(ShieldedTRC20Parameters params, long value) {
    byte[] mergedBytes;
    ShieldContract.SpendDescription spendDesc = params.getSpendDescription(0);
    mergedBytes = ByteUtil.merge(
        spendDesc.getNullifier().toByteArray(),
        spendDesc.getAnchor().toByteArray(),
        spendDesc.getValueCommitment().toByteArray(),
        spendDesc.getRk().toByteArray(),
        Wallet.generateRandomBytes(192),
        spendDesc.getSpendAuthoritySignature().toByteArray(),
        longTo32Bytes(value),
        params.getBindingSignature().toByteArray(),
        params.getMessageHash().toByteArray()
    );
    return mergedBytes;
  }

  private byte[] abiEncodeForBurnWrongAuthoritySignature(ShieldedTRC20Parameters params,
      long value) {
    byte[] mergedBytes;
    ShieldContract.SpendDescription spendDesc = params.getSpendDescription(0);
    mergedBytes = ByteUtil.merge(
        spendDesc.getNullifier().toByteArray(),
        spendDesc.getAnchor().toByteArray(),
        spendDesc.getValueCommitment().toByteArray(),
        spendDesc.getRk().toByteArray(),
        spendDesc.getZkproof().toByteArray(),
        Wallet.generateRandomBytes(64),
        longTo32Bytes(value),
        params.getBindingSignature().toByteArray(),
        params.getMessageHash().toByteArray()
    );
    return mergedBytes;
  }

  private byte[] abiEncodeForBurnWrongBingSignature(ShieldedTRC20Parameters params, long value) {
    byte[] mergedBytes;
    ShieldContract.SpendDescription spendDesc = params.getSpendDescription(0);
    mergedBytes = ByteUtil.merge(
        spendDesc.getNullifier().toByteArray(),
        spendDesc.getAnchor().toByteArray(),
        spendDesc.getValueCommitment().toByteArray(),
        spendDesc.getRk().toByteArray(),
        spendDesc.getZkproof().toByteArray(),
        spendDesc.getSpendAuthoritySignature().toByteArray(),
        longTo32Bytes(value),
        Wallet.generateRandomBytes(64),
        params.getMessageHash().toByteArray()
    );
    return mergedBytes;
  }

  private byte[] abiEncodeForBurnWrongHash(ShieldedTRC20Parameters params, long value) {
    byte[] mergedBytes;
    ShieldContract.SpendDescription spendDesc = params.getSpendDescription(0);
    mergedBytes = ByteUtil.merge(
        spendDesc.getNullifier().toByteArray(),
        spendDesc.getAnchor().toByteArray(),
        spendDesc.getValueCommitment().toByteArray(),
        spendDesc.getRk().toByteArray(),
        spendDesc.getZkproof().toByteArray(),
        spendDesc.getSpendAuthoritySignature().toByteArray(),
        longTo32Bytes(value),
        params.getBindingSignature().toByteArray(),
        Wallet.generateRandomBytes(21)
    );
    return mergedBytes;
  }

  private byte[] longTo32Bytes(long value) {
    byte[] longBytes = ByteArray.fromLong(value);
    byte[] zeroBytes = new byte[24];
    return ByteUtil.merge(zeroBytes, longBytes);
  }

  private long randomLong() {
    return (long) Math.round(Math.random() * Long.MAX_VALUE / 2);
  }

}
