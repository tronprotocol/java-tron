package org.tron.common.runtime.vm;

import com.google.protobuf.ByteString;
import java.io.File;
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
import org.tron.core.capsule.IncrementalMerkleTreeCapsule;
import org.tron.core.capsule.PedersenHashCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.services.http.FullNodeHttpApiService;
import org.tron.core.vm.PrecompiledContracts;
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
    Args.setParam(new String[] {"--output-directory", dbPath}, "config-test.conf");
    context = new TronApplicationContext(DefaultConfig.class);
    DEFAULT_OVK = ByteArray
        .fromHexString("030c8c2bc59fb3eb8afb047a8ea4b028743d23e7d38c6fa30908358431e2314d");
    SHIELDED_CONTRACT_ADDRESS = WalletClient.decodeFromBase58Check(SHIELDED_CONTRACT_ADDRESS_STR);
    PUBLIC_TO_ADDRESS = WalletClient.decodeFromBase58Check(PUBLIC_TO_ADDRESS_STR);
    FullNodeHttpApiService.librustzcashInitZksnarkParams();
  }

  PrecompiledContracts.VerifyMintProof mintContract = new VerifyMintProof();
  PrecompiledContracts.VerifyTransferProof transferContract = new VerifyTransferProof();
  PrecompiledContracts.VerifyBurnProof burnContract = new VerifyBurnProof();

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
    int totalCountNum = 10;
    long leafCount = 0;
    long value = 100L;
    byte[] frontier = new byte[32 * 33];

    for (int countNum = 0; countNum < totalCountNum; countNum++) {
      ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
      builder.setTransparentToAmount(value);
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
      Pair<Boolean, byte[]> contarctResult = mintContract.execute(inputData);
      byte[] result = contarctResult.getRight();

      Assert.assertEquals(1, result[31]);

      //update frontier and leafCount
      //if slot == 0, frontier[0:31]=noteCommitment
      if (result[32] == 0) {
        System.arraycopy(inputData, 0, frontier, 0, 32);
      } else {
        int srcPos = (result[32] - 1) * 32 + 33;
        int destPos = result[32] * 32;
        System.arraycopy(result, srcPos, frontier, destPos, 32);
      }
      leafCount++;
    }
  }

  @Test
  public void verifyTransferProofCorrect() throws ZksnarkException {
    int totalCountNum = 10;
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

      {//for mint1
        ShieldedTRC20ParametersBuilder mintBuilder1 = new ShieldedTRC20ParametersBuilder();
        mintBuilder1.setTransparentToAmount(30);
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

      {//for mint2
        ShieldedTRC20ParametersBuilder mintBuilder2 = new ShieldedTRC20ParametersBuilder();
        mintBuilder2.setTransparentToAmount(70);
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

      {//for transfer
        ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
        builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.TRANSFER);
        builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        builder.setTransparentFromAmount(0);
        builder.setTransparentToAmount(0);
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

        byte[] inputData = abiEncodeForTransfer(params, frontier, leafCount);
        Pair<Boolean, byte[]> contractResult = verifyTransfer(inputData);
        byte[] result = contractResult.getRight();
        Assert.assertEquals(1, result[31]);

        //update frontier and leafCount
        int idx = 32;
        for (int i = 0; i < 2; i++) {
          if (result[idx] == 0) {
            byte[] noteCommitment = params.getReceiveDescription(i).getNoteCommitment()
                .toByteArray();
            System.arraycopy(noteCommitment, 0, frontier, 0, 32);
          } else {
            int destPos = result[idx] * 32;
            int srcPos = (result[idx] - 1) * 32 + idx + 1;
            System.arraycopy(result, srcPos, frontier, destPos, 32);
          }
          idx += result[idx] * 32 + 1;
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

  Pair<Boolean, byte[]> verifyTransfer(byte[] input) {
    transferContract.getEnergyForData(input);
    transferContract.setVmShouldEndInUs(System.nanoTime() / 1000 + 500 * 1000);
    Pair<Boolean, byte[]> ret = transferContract.execute(input);
    return ret;
  }

  @Test
  public void verifyBurnProofCorrect() throws ZksnarkException {
    int totalCountNum = 10;
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

      {//for mint
        ShieldedTRC20ParametersBuilder mintBuilder = new ShieldedTRC20ParametersBuilder();
        mintBuilder.setTransparentToAmount(value);
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

      {//for burn
        ShieldedTRC20ParametersBuilder builder = new ShieldedTRC20ParametersBuilder();
        builder.setShieldedTRC20ParametersType(ShieldedTRC20ParametersType.BURN);
        builder.setShieldedTRC20Address(SHIELDED_CONTRACT_ADDRESS);
        builder.setTransparentToAmount(value);
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

  public IncrementalMerkleVoucherContainer addSimpleMerkleVoucherContainer(
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

  public byte[] decodePath(byte[] encodedPath) {
    Assert.assertEquals(1065, encodedPath.length);
    byte[] path = new byte[32 * 32];
    for (int i = 0; i < 32; i++) {
      System.arraycopy(encodedPath, (i * 33) + 2, path, i * 32, 32);
    }
    return path;
  }

  public byte[] abiEncodeForMint(ShieldedTRC20Parameters params, long value,
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

  private byte[] abiEncodeForTransfer(ShieldedTRC20Parameters params, byte[] frontier,
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
    byte[] inputOffsetbytes = longTo32Bytes(1280);
    long spendCount = spendDescs.size();
    byte[] spendCountBytes = longTo32Bytes(spendCount);
    byte[] authOffsetBytes = longTo32Bytes(1280 + 32 + 320 * spendCount);
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
    byte[] outputOffsetbytes = longTo32Bytes(1280 + 32 + 320 * spendCount + 32 + 64 * spendCount);
    mergedBytes = ByteUtil.merge(inputOffsetbytes,
        authOffsetBytes,
        outputOffsetbytes,
        params.getBindingSignature().toByteArray(),
        params.getMessageHash().toByteArray(),
        frontier,
        longTo32Bytes(leafCount),
        spendCountBytes,
        input,
        spendCountBytes,
        spendAuthSig,
        recvCountBytes,
        output
    );
    //logger.info(ByteArray.toHexString(mergedBytes));
    return mergedBytes;
  }

  public byte[] abiEncodeForBurn(ShieldedTRC20Parameters params, long value) {
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

  public byte[] longTo32Bytes(long value) {
    byte[] longBytes = ByteArray.fromLong(value);
    byte[] zeroBytes = new byte[24];
    return ByteUtil.merge(zeroBytes, longBytes);
  }

}
