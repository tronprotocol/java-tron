package stest.tron.wallet.common.client.utils;

import com.google.protobuf.ByteString;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.netty.util.internal.StringUtil;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.spongycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI.ShieldedTRC20Parameters;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.Note;
import org.tron.common.utils.ByteUtil;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.vm.trace.Op;
import org.tron.core.zen.address.DiversifierT;
import org.tron.core.zen.address.ExpandedSpendingKey;
import org.tron.core.zen.address.FullViewingKey;
import org.tron.core.zen.address.IncomingViewingKey;
import org.tron.core.zen.address.PaymentAddress;
import org.tron.core.zen.address.SpendingKey;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.protos.contract.ShieldContract;
import org.tron.protos.Protocol.TransactionInfo;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.BeforeSuite;
import org.tron.api.WalletGrpc;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;
import org.tron.protos.contract.ShieldContract;
import org.tron.protos.contract.ShieldContract.SpendDescription;

@Slf4j
public class ZenTrc20Base {

  public final String foundationAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  public final byte[] foundationAccountAddress = PublicMethed.getFinalAddress(foundationAccountKey);
  public final String zenTrc20TokenOwnerKey = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTrc20TokenOwnerKey");
  public final byte[] zenTrc20TokenOwnerAddress = PublicMethed
      .getFinalAddress(zenTrc20TokenOwnerKey);
  public final String zenTrc20TokenOwnerAddressString = PublicMethed
      .getAddressString(zenTrc20TokenOwnerKey);
  public ManagedChannel channelFull = null;
  public WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  public static long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  public com.google.protobuf.ByteString contractAddressByteString;
  public static byte[] contractAddressByte;
  public static String contractAddress;
  public static com.google.protobuf.ByteString shieldAddressByteString;
  public static byte[] shieldAddressByte;
  public static String shieldAddress;
  public static String deployShieldTrc20Txid;
  public static String deployShieldTxid;
  public static String mint = "mint(uint256,bytes32[9],bytes32[2],bytes32[21])";
  public static String transfer = "transfer(bytes32[10][],bytes32[2][],bytes32[9][],bytes32[2],bytes32[21][])";
  public static String burn = "burn(bytes32[10],bytes32[2],uint256,bytes32[2],address)";
  public Wallet wallet = new Wallet();

  /**
   * constructor.
   */
  @BeforeSuite(enabled = true,description = "Deploy shield trc20 depend contract")
  public void deployShieldTrc20DependContract() {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    Assert.assertTrue(PublicMethed.sendcoin(zenTrc20TokenOwnerAddress, 10000000000000L,
        foundationAccountAddress, foundationAccountKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String contractName = "shieldTrc20Token";

    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_shieldTrc20Token");
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_shieldTrc20Token");
    String constructorStr = "constructor(uint256,string,string)";
    String data = "100000000000" + "," + "\"TokenTRC20\"" + "," + "\"zen20\"";
    logger.info("data:" + data);
    deployShieldTrc20Txid = PublicMethed
        .deployContractWithConstantParame(contractName, abi, code, constructorStr, data, "",
            maxFeeLimit, 0L, 100, null,
            zenTrc20TokenOwnerKey, zenTrc20TokenOwnerAddress, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info(deployShieldTrc20Txid);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(deployShieldTrc20Txid, blockingStubFull);
    contractAddressByteString = infoById.get().getContractAddress();
    contractAddressByte = infoById.get().getContractAddress().toByteArray();
    contractAddress = Base58.encode58Check(contractAddressByte);
    logger.info(contractAddress);


    contractName = "shield";
    abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_shield");
    code = Configuration.getByPath("testng.conf")
        .getString("code.code_shield");
    data = "\"" + contractAddress + "\"" + "," + "0";
    constructorStr = "constructor(address,uint256)";
    deployShieldTxid = PublicMethed
        .deployContractWithConstantParame(contractName, abi, code, constructorStr, data, "",
            maxFeeLimit, 0L, 100, null,
            zenTrc20TokenOwnerKey, zenTrc20TokenOwnerAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info(deployShieldTxid);
    infoById = PublicMethed
        .getTransactionInfoById(deployShieldTxid, blockingStubFull);
    shieldAddressByteString = infoById.get().getContractAddress();
    shieldAddressByte = infoById.get().getContractAddress().toByteArray();
    shieldAddress = Base58.encode58Check(shieldAddressByte);
    logger.info(shieldAddress);


    data = "\"" + shieldAddress + "\"" + "," + "10000000000000";
    String txid = PublicMethed.triggerContract(contractAddressByte,
        "approve(address,uint256)", data, false,
        0, maxFeeLimit, zenTrc20TokenOwnerAddress, zenTrc20TokenOwnerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("approve:" + txid);
    Assert.assertTrue(infoById.get().getReceipt().getResultValue() == 1);


  }


  /**
   * constructor.
   */
  public GrpcAPI.ShieldedTRC20Parameters createShieldedTrc20Parameters(BigInteger publicFromAmount,
      List<GrpcAPI.DecryptNotesTRC20> inputNoteList,List<ShieldedAddressInfo> shieldedAddressInfoList,List<Note> outputNoteList, String publicToAddress,Long pubicToAmount,
      WalletGrpc.WalletBlockingStub blockingStubFull) throws ZksnarkException {


    GrpcAPI.PrivateShieldedTRC20Parameters.Builder builder
        = GrpcAPI.PrivateShieldedTRC20Parameters.newBuilder();

    //Mint type should set public from amount to parameter
    if (publicFromAmount.compareTo(BigInteger.ZERO) > 0) {
      builder.setFromAmount(publicFromAmount.toString());
    }

    builder.setShieldedTRC20ContractAddress(ByteString.copyFrom(shieldAddressByte));
    long valueBalance = 0;


    if (inputNoteList != null) {
      logger.info("Enter transfer type code");
      List<String> rootAndPath = new ArrayList<>();
      for (int i = 0; i < inputNoteList.size(); i++) {
        long position = inputNoteList.get(0).getNoteTxs(0).getPosition();
        rootAndPath.add(getRootAndPath(position,blockingStubFull));
      }
      if (rootAndPath.isEmpty() || rootAndPath.size() != inputNoteList.size()) {
        System.out.println("Can't get all merkle tree, please check the notes.");
        return null;
      }
      for(int i = 0; i < rootAndPath.size(); i++) {
        if (rootAndPath.get(i) == null) {
          System.out.println("Can't get merkle path, please check the note " + i + ".");
          return null;
        }
      }

      for (int i = 0; i < inputNoteList.size(); ++i) {
        if (i == 0) {
          String shieldedAddress = inputNoteList.get(i).getNoteTxs(0).getNote().getPaymentAddress();

          String spendingKey = ByteArray.toHexString(shieldedAddressInfoList.get(i).getSk());
          BytesMessage sk = BytesMessage.newBuilder()
              .setValue(ByteString.copyFrom(ByteArray.fromHexString(spendingKey))).build();
          Optional<GrpcAPI.ExpandedSpendingKeyMessage> esk = Optional
              .of(blockingStubFull.getExpandedSpendingKey(sk));

          //ExpandedSpendingKey expandedSpendingKey = spendingKey.expandedSpendingKey();
          builder.setAsk(esk.get().getAsk());
          builder.setNsk(esk.get().getNsk());
          builder.setOvk(esk.get().getOvk());
        }
        Note.Builder noteBuild = Note.newBuilder();
        noteBuild.setPaymentAddress(shieldedAddressInfoList.get(i).getAddress());
        noteBuild.setValue(inputNoteList.get(i).getNoteTxs(0).getNote().getValue());
        noteBuild.setRcm(inputNoteList.get(i).getNoteTxs(0).getNote().getRcm());
        noteBuild.setMemo(inputNoteList.get(i).getNoteTxs(0).getNote().getMemo());

        System.out.println("address " + shieldedAddressInfoList.get(i).getAddress());
        System.out.println("value " + inputNoteList.get(i).getNoteTxs(0).getNote().getValue());
        //System.out.println("rcm " + ByteString.copyFrom(inputNoteList.get(i).getNoteTxs(0).getNote().getRcm()));
        System.out.println("trxId " + inputNoteList.get(i).getNoteTxs(0).getTxid());
        System.out.println("index " + inputNoteList.get(i).getNoteTxs(0).getIndex());
        System.out.println("position " + inputNoteList.get(i).getNoteTxs(0).getPosition());
        //System.out.println("memo " + ZenUtils.getMemo(inputNoteList.get(i).getNoteTxs(0).getNote().getMemo()));

        byte[] eachRootAndPath = ByteArray.fromHexString(rootAndPath.get(i));
        byte[] root = Arrays.copyOfRange(eachRootAndPath, 0, 32);
        byte[] path = Arrays.copyOfRange(eachRootAndPath, 32, 1056);
        GrpcAPI.SpendNoteTRC20.Builder spendTRC20NoteBuilder = GrpcAPI.SpendNoteTRC20.newBuilder();
        spendTRC20NoteBuilder.setNote(noteBuild.build());
        spendTRC20NoteBuilder.setAlpha(ByteString.copyFrom(blockingStubFull.getRcm(
            EmptyMessage.newBuilder().build()).getValue().toByteArray()));
        spendTRC20NoteBuilder.setRoot(ByteString.copyFrom(root));
        spendTRC20NoteBuilder.setPath(ByteString.copyFrom(path));
        spendTRC20NoteBuilder.setPos(inputNoteList.get(i).getNoteTxs(0).getPosition());

        valueBalance = Math.addExact(valueBalance, inputNoteList.get(i).getNoteTxs(0).getNote().getValue());
        builder.addShieldedSpends(spendTRC20NoteBuilder.build());
      }
    } else {
      //@TODO remove randomOvk by sha256.of(privateKey)
      byte[] ovk = getRandomOvk();
      if (ovk != null) {
        builder.setOvk(ByteString.copyFrom(ovk));
      } else {
        System.out.println("Get random ovk from Rpc failure,please check config");
        return null;
      }
    }


    if (outputNoteList != null) {
      for (int i = 0; i < outputNoteList.size(); i++) {
        GrpcAPI.Note note = outputNoteList.get(i);
        valueBalance = Math.subtractExact(valueBalance, note.getValue());
        builder.addShieldedReceives(
            GrpcAPI.ReceiveNote.newBuilder().setNote(note).build());
      }
    }

    if (!StringUtil.isNullOrEmpty(publicToAddress)) {
      byte[] to = wallet.decodeFromBase58Check(publicToAddress);
      if (to == null) {
        return null;
      }
      builder.setTransparentToAddress(ByteString.copyFrom(to));
      builder.setToAmount(pubicToAmount.toString());
    }



    try {
      return blockingStubFull.createShieldedContractParameters(builder.build());
    } catch (Exception e) {
      Status status = Status.fromThrowable(e);
      System.out.println("createShieldedContractParameters failed,error "
          + status.getDescription());
    }
    return null;
  }


  /**
   * constructor.
   */
  public GrpcAPI.ShieldedTRC20Parameters createShieldedTrc20ParametersWithoutAsk(BigInteger publicFromAmount,
      List<GrpcAPI.DecryptNotesTRC20> inputNoteList,List<ShieldedAddressInfo> shieldedAddressInfoList,List<Note> outputNoteList, String publicToAddress,Long pubicToAmount,
      WalletGrpc.WalletBlockingStub blockingStubFull) throws ZksnarkException {


    GrpcAPI.PrivateShieldedTRC20ParametersWithoutAsk.Builder builder
        = GrpcAPI.PrivateShieldedTRC20ParametersWithoutAsk.newBuilder();

    //Mint type should set public from amount to parameter
    if (publicFromAmount.compareTo(BigInteger.ZERO) > 0) {
      builder.setFromAmount(publicFromAmount.toString());
    }

    builder.setShieldedTRC20ContractAddress(ByteString.copyFrom(shieldAddressByte));
    long valueBalance = 0;

    byte[] ask = new byte[32];
    if (inputNoteList != null) {
      List<String> rootAndPath = new ArrayList<>();
      for (int i = 0; i < inputNoteList.size(); i++) {
        long position = inputNoteList.get(0).getNoteTxs(0).getPosition();
        rootAndPath.add(getRootAndPath(position,blockingStubFull));
      }
      if (rootAndPath.isEmpty() || rootAndPath.size() != inputNoteList.size()) {
        System.out.println("Can't get all merkle tree, please check the notes.");
        return null;
      }
      for(int i = 0; i < rootAndPath.size(); i++) {
        if (rootAndPath.get(i) == null) {
          System.out.println("Can't get merkle path, please check the note " + i + ".");
          return null;
        }
      }

      for (int i = 0; i < inputNoteList.size(); ++i) {
        if (i == 0) {
/*          String shieldedAddress = inputNoteList.get(i).getNoteTxs(0).getNote().getPaymentAddress();
          SpendingKey spendingKey = new SpendingKey(shieldedAddressInfoList.get(i).getSk());
          ExpandedSpendingKey expandedSpendingKey = spendingKey.expandedSpendingKey();
          System.arraycopy(expandedSpendingKey.getAsk(), 0, ask, 0, 32);
          builder.setAk(ByteString.copyFrom(
              ExpandedSpendingKey.getAkFromAsk(expandedSpendingKey.getAsk())));
          builder.setNsk(ByteString.copyFrom(expandedSpendingKey.getNsk()));
          builder.setOvk(ByteString.copyFrom(expandedSpendingKey.getOvk()));*/

          String spendingKey = ByteArray.toHexString(shieldedAddressInfoList.get(i).getSk());
          BytesMessage sk = BytesMessage.newBuilder()
              .setValue(ByteString.copyFrom(ByteArray.fromHexString(spendingKey))).build();
          Optional<GrpcAPI.ExpandedSpendingKeyMessage> esk = Optional
              .of(blockingStubFull.getExpandedSpendingKey(sk));


          String ask1 = ByteArray.toHexString(esk.get().getAsk().toByteArray());

          BytesMessage ask2 = BytesMessage.newBuilder()
              .setValue(ByteString.copyFrom(ByteArray.fromHexString(ask1))).build();
          Optional<BytesMessage> ak = Optional.of(blockingStubFull.getAkFromAsk(ask2));
          String akString = ByteArray.toHexString(ak.get().getValue().toByteArray());



          builder.setAk(ByteString.copyFrom(ByteArray.fromHexString(akString)));
          builder.setOvk(esk.get().getOvk());
          builder.setNsk(esk.get().getNsk());

        }
        Note.Builder noteBuild = Note.newBuilder();
        noteBuild.setPaymentAddress(shieldedAddressInfoList.get(i).getAddress());
        noteBuild.setValue(inputNoteList.get(i).getNoteTxs(0).getNote().getValue());
        noteBuild.setRcm(inputNoteList.get(i).getNoteTxs(0).getNote().getRcm());
        noteBuild.setMemo(inputNoteList.get(i).getNoteTxs(0).getNote().getMemo());

        System.out.println("address " + shieldedAddressInfoList.get(i).getAddress());
        System.out.println("value " + inputNoteList.get(i).getNoteTxs(0).getNote().getValue());
        //System.out.println("rcm " + ByteString.copyFrom(inputNoteList.get(i).getNoteTxs(0).getNote().getRcm()));
        System.out.println("trxId " + inputNoteList.get(i).getNoteTxs(0).getTxid());
        System.out.println("index " + inputNoteList.get(i).getNoteTxs(0).getIndex());
        System.out.println("position " + inputNoteList.get(i).getNoteTxs(0).getPosition());
        //System.out.println("memo " + ZenUtils.getMemo(inputNoteList.get(i).getNoteTxs(0).getNote().getMemo()));

        byte[] eachRootAndPath = ByteArray.fromHexString(rootAndPath.get(i));
        byte[] root = Arrays.copyOfRange(eachRootAndPath, 0, 32);
        byte[] path = Arrays.copyOfRange(eachRootAndPath, 32, 1056);
        GrpcAPI.SpendNoteTRC20.Builder spendTRC20NoteBuilder = GrpcAPI.SpendNoteTRC20.newBuilder();
        spendTRC20NoteBuilder.setNote(noteBuild.build());
        spendTRC20NoteBuilder.setAlpha(ByteString.copyFrom(blockingStubFull.getRcm(
            EmptyMessage.newBuilder().build()).getValue().toByteArray()));
        spendTRC20NoteBuilder.setRoot(ByteString.copyFrom(root));
        spendTRC20NoteBuilder.setPath(ByteString.copyFrom(path));
        spendTRC20NoteBuilder.setPos(inputNoteList.get(i).getNoteTxs(0).getPosition());

        builder.addShieldedSpends(spendTRC20NoteBuilder.build());
        valueBalance = Math.addExact(valueBalance, inputNoteList.get(i).getNoteTxs(0).getNote().getValue());
      }
    } else {
      //@TODO remove randomOvk by sha256.of(privateKey)
      byte[] ovk = getRandomOvk();
      if (ovk != null) {
        builder.setOvk(ByteString.copyFrom(ovk));
      } else {
        System.out.println("Get random ovk from Rpc failure,please check config");
        return null;
      }
    }


    if (outputNoteList != null) {
      for (int i = 0; i < outputNoteList.size(); i++) {
        GrpcAPI.Note note = outputNoteList.get(i);
        valueBalance = Math.subtractExact(valueBalance, note.getValue());
        builder.addShieldedReceives(
            GrpcAPI.ReceiveNote.newBuilder().setNote(note).build());
      }
    }

    if (!StringUtil.isNullOrEmpty(publicToAddress)) {
      byte[] to = wallet.decodeFromBase58Check(publicToAddress);
      if (to == null) {
        return null;
      }
      builder.setTransparentToAddress(ByteString.copyFrom(to));
      builder.setToAmount(pubicToAmount.toString());
    }

    ShieldedTRC20Parameters parameters = blockingStubFull.createShieldedContractParametersWithoutAsk(builder.build());
    if (parameters == null) {
      System.out.println("createShieldedContractParametersWithoutAsk failed!");
      return null;
    }


    ByteString messageHash = parameters.getMessageHash();
    List<SpendDescription> spendDescList = parameters.getSpendDescriptionList();
    ShieldedTRC20Parameters.Builder newBuilder =
        ShieldedTRC20Parameters.newBuilder().mergeFrom(parameters);
    for (int i = 0; i < spendDescList.size(); i++) {
      GrpcAPI.SpendAuthSigParameters.Builder builder1 = GrpcAPI.SpendAuthSigParameters.newBuilder();
      builder1.setAsk(ByteString.copyFrom(ask));
      builder1.setTxHash(messageHash);
      builder1.setAlpha(builder.build().getShieldedSpends(i).getAlpha());

      BytesMessage authSig = blockingStubFull.createSpendAuthSig(builder1.build());
      newBuilder.getSpendDescriptionBuilder(i)
          .setSpendAuthoritySignature(
              ByteString.copyFrom(
                  authSig.getValue().toByteArray()));
    }
    return newBuilder.build();
  }




  public String getRootAndPath(long position,WalletGrpc.WalletBlockingStub
      blockingStubFull) {
    String methodStr = "getPath(uint256)";
    byte[] indexBytes = ByteArray.fromLong(position);
    String argsStr = ByteArray.toHexString(indexBytes);
    argsStr = "000000000000000000000000000000000000000000000000" + argsStr;
    TransactionExtention transactionExtention = PublicMethed.triggerConstantContractForExtention(shieldAddressByte, methodStr,argsStr,true,0,1000000000L,"0",0,zenTrc20TokenOwnerAddress,
        zenTrc20TokenOwnerKey,blockingStubFull);
    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    return ByteArray.toHexString(result);
  }


  /**
   * constructor.
   */
  public String encodeMintParamsToHexString(GrpcAPI.ShieldedTRC20Parameters parameters,
      BigInteger value) {
    byte[] mergedBytes;
    ShieldContract.ReceiveDescription revDesc = parameters.getReceiveDescription(0);
    mergedBytes = ByteUtil.merge(
        ByteUtil.bigIntegerToBytes(value, 32),
        revDesc.getNoteCommitment().toByteArray(),
        revDesc.getValueCommitment().toByteArray(),
        revDesc.getEpk().toByteArray(),
        revDesc.getZkproof().toByteArray(),
        parameters.getBindingSignature().toByteArray(),
        revDesc.getCEnc().toByteArray(),
        revDesc.getCOut().toByteArray(),
        new byte[12]
    );
    return ByteArray.toHexString(mergedBytes);
  }

  /**
   * constructor.
   */
  public Optional<ShieldedAddressInfo> getNewShieldedAddress(WalletGrpc.WalletBlockingStub
      blockingStubFull) {
    ShieldedAddressInfo addressInfo = new ShieldedAddressInfo();


    try {
      Optional<BytesMessage> sk = Optional.of(blockingStubFull
          .getSpendingKey(EmptyMessage.newBuilder().build()));
      final Optional<GrpcAPI.DiversifierMessage> d = Optional.of(blockingStubFull.getDiversifier(
          EmptyMessage.newBuilder().build()));

      Optional<GrpcAPI.ExpandedSpendingKeyMessage> expandedSpendingKeyMessage
          = Optional.of(blockingStubFull
          .getExpandedSpendingKey(sk.get()));

      BytesMessage.Builder askBuilder = BytesMessage.newBuilder();
      askBuilder.setValue(expandedSpendingKeyMessage.get().getAsk());
      Optional<BytesMessage> ak = Optional.of(blockingStubFull.getAkFromAsk(askBuilder.build()));

      BytesMessage.Builder nskBuilder = BytesMessage.newBuilder();
      nskBuilder.setValue(expandedSpendingKeyMessage.get().getNsk());
      Optional<BytesMessage> nk = Optional.of(blockingStubFull.getNkFromNsk(nskBuilder.build()));

      GrpcAPI.ViewingKeyMessage.Builder viewBuilder = GrpcAPI.ViewingKeyMessage.newBuilder();
      viewBuilder.setAk(ak.get().getValue());
      viewBuilder.setNk(nk.get().getValue());
      Optional<GrpcAPI.IncomingViewingKeyMessage> ivk = Optional.of(blockingStubFull
          .getIncomingViewingKey(viewBuilder.build()));

      GrpcAPI.IncomingViewingKeyDiversifierMessage.Builder builder
          = GrpcAPI.IncomingViewingKeyDiversifierMessage
          .newBuilder();
      builder.setD(d.get());
      builder.setIvk(ivk.get());
      Optional<GrpcAPI.PaymentAddressMessage> addressMessage = Optional.of(blockingStubFull
          .getZenPaymentAddress(builder.build()));
      addressInfo.setSk(sk.get().getValue().toByteArray());
      addressInfo.setD(new DiversifierT(d.get().getD().toByteArray()));
      addressInfo.setIvk(ivk.get().getIvk().toByteArray());
      addressInfo.setOvk(expandedSpendingKeyMessage.get().getOvk().toByteArray());
      addressInfo.setPkD(addressMessage.get().getPkD().toByteArray());


      System.out.println("ivk " + ByteArray.toHexString(ivk.get().getIvk().toByteArray()));
      System.out.println("ovk " + ByteArray.toHexString(expandedSpendingKeyMessage.get()
          .getOvk().toByteArray()));

      return Optional.of(addressInfo);

    } catch (Exception e) {
      e.printStackTrace();
    }

    return Optional.empty();
  }




  /**
   * constructor.
   */
  public static List<Note> addShieldTrc20OutputList(List<Note> shieldOutList,
      String shieldToAddress, String toAmountString, String menoString,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    String shieldAddress = shieldToAddress;
    String amountString = toAmountString;
    if (menoString.equals("null")) {
      menoString = "";
    }
    long shieldAmount = 0;
    if (!StringUtil.isNullOrEmpty(amountString)) {
      shieldAmount = Long.valueOf(amountString);
    }

    Note.Builder noteBuild = Note.newBuilder();
    noteBuild.setPaymentAddress(shieldAddress);
    //noteBuild.setPaymentAddress(shieldAddress);
    noteBuild.setValue(shieldAmount);
    noteBuild.setRcm(ByteString.copyFrom(blockingStubFull.getRcm(EmptyMessage.newBuilder().build())
        .getValue().toByteArray()));
    noteBuild.setMemo(ByteString.copyFrom(menoString.getBytes()));
    shieldOutList.add(noteBuild.build());
    return shieldOutList;
  }


  /**
   * constructor.
   */
  public Long getBalanceOfShieldTrc20(String queryAddress,byte[] ownerAddress,
      String ownerKey,WalletGrpc.WalletBlockingStub blockingStubFull)  {
    String paramStr = "\"" + queryAddress + "\"";
    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddressByte, "balanceOf(address)",
            paramStr, false, 0, 0, "0", 0,
            ownerAddress, ownerKey, blockingStubFull);

    String hexBalance = Hex.toHexString(transactionExtention
        .getConstantResult(0).toByteArray());
    for (int i = 0; i < hexBalance.length();i++) {
      if (hexBalance.charAt(i) != '0') {
        hexBalance = hexBalance.substring(i);
        break;
      }
    }
    logger.info(hexBalance);
    return Long.parseLong(hexBalance,16);
  }

  /**
   * constructor.
   */
  public GrpcAPI.DecryptNotesTRC20 scanShieldedTrc20NoteByIvk(ShieldedAddressInfo
      shieldedAddressInfo, WalletGrpc.WalletBlockingStub blockingStubFull) throws Exception {
    long currentBlockNum = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();

    Long startNum = currentBlockNum - 90L;
    Long endNum = currentBlockNum;
    if (currentBlockNum < 100) {
      startNum = 1L;
    }

    //System.out.println("sk :" + ByteArray.toHexString(shieldedAddressInfo.getSk()));
    String spendingKey = ByteArray.toHexString(shieldedAddressInfo.getSk());
    BytesMessage sk = BytesMessage.newBuilder()
        .setValue(ByteString.copyFrom(ByteArray.fromHexString(spendingKey))).build();
    Optional<GrpcAPI.ExpandedSpendingKeyMessage> esk = Optional
        .of(blockingStubFull.getExpandedSpendingKey(sk));
    //System.out.println("ask:" + ByteArray.toHexString(esk.get().getAsk().toByteArray()));
    //System.out.println("nsk:" + ByteArray.toHexString(esk.get().getNsk().toByteArray()));
    //System.out.println("ovk:" + ByteArray.toHexString(esk.get().getOvk().toByteArray()));

    String ask = ByteArray.toHexString(esk.get().getAsk().toByteArray());

    BytesMessage ask1 = BytesMessage.newBuilder()
        .setValue(ByteString.copyFrom(ByteArray.fromHexString(ask))).build();
    Optional<BytesMessage> ak = Optional.of(blockingStubFull.getAkFromAsk(ask1));
    String akString = ByteArray.toHexString(ak.get().getValue().toByteArray());
    //System.out.println("ak:" + ByteArray.toHexString(ak.get().getValue().toByteArray()));

    String nsk = ByteArray.toHexString(esk.get().getNsk().toByteArray());

    BytesMessage nsk1 = BytesMessage.newBuilder()
        .setValue(ByteString.copyFrom(ByteArray.fromHexString(nsk))).build();
    Optional<BytesMessage> nk = Optional.of(blockingStubFull.getNkFromNsk(nsk1));
    //System.out.println("nk:" + ByteArray.toHexString(nk.get().getValue().toByteArray()));
    String nkString = ByteArray.toHexString(nk.get().getValue().toByteArray());

    GrpcAPI.ViewingKeyMessage.Builder viewBuilder = GrpcAPI.ViewingKeyMessage.newBuilder();
    viewBuilder.setAk(ak.get().getValue());
    viewBuilder.setNk(nk.get().getValue());
    GrpcAPI.IncomingViewingKeyMessage ivk = blockingStubFull
        .getIncomingViewingKey(viewBuilder.build());




    //ivk.getIvk()
    String ivkString = ByteArray.toHexString(ivk.getIvk().toByteArray());
    //System.out.println("ivkString:" + ivkString);
    String ivkStringOld = ByteArray.toHexString(shieldedAddressInfo.getIvk());
    //System.out.println("ivkStringOld:" + ivkStringOld);
    //System.out.println("shield address:" + shieldedAddressInfo.getAddress());
    //System.out.println("start:" + (currentBlockNum - 99));
    //System.out.println("end:" + (currentBlockNum));
    GrpcAPI.IvkDecryptTRC20Parameters parameters = GrpcAPI.IvkDecryptTRC20Parameters
        .newBuilder()
        .setStartBlockIndex(startNum)
        .setEndBlockIndex(endNum)
        .setShieldedTRC20ContractAddress(ByteString.copyFrom(Base58.decode58Check(shieldAddress)))
        .setIvk(ByteString.copyFrom(ByteArray.fromHexString(ivkString)))
        .setAk(ByteString.copyFrom(ByteArray.fromHexString(akString)))
        .setNk(ByteString.copyFrom(ByteArray.fromHexString(nkString)))
        .build();
    try {
      return blockingStubFull.scanShieldedTRC20NotesbyIvk(parameters);
    } catch (Exception e) {
      System.out.println(e);
      Status status = Status.fromThrowable(e);
      System.out.println("ScanShieldedTRC20NoteByIvk failed,error " + status.getDescription());

    }
    return null;
  }

  /**
   * constructor.
   */
  public GrpcAPI.DecryptNotesTRC20 scanShieldedTrc20NoteByOvk(ShieldedAddressInfo
      shieldedAddressInfo, WalletGrpc.WalletBlockingStub blockingStubFull) throws Exception {
    long currentBlockNum = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();
    Long startNum = currentBlockNum - 90L;
    Long endNum = currentBlockNum;
    if (currentBlockNum < 100) {
      startNum = 1L;
    }


    String ovkString = ByteArray.toHexString(shieldedAddressInfo.getOvk());
    GrpcAPI.OvkDecryptTRC20Parameters parameters = GrpcAPI.OvkDecryptTRC20Parameters.newBuilder()
        .setStartBlockIndex(startNum)
        .setEndBlockIndex(endNum)
        .setOvk(ByteString.copyFrom(ByteArray.fromHexString(ovkString)))
        .setShieldedTRC20ContractAddress(ByteString.copyFrom(Base58.decode58Check(shieldAddress)))
        .build();

    try {
      return blockingStubFull.scanShieldedTRC20NotesbyOvk(parameters);
    } catch (Exception e) {
      System.out.println(e);
      Status status = Status.fromThrowable(e);
      System.out.println("ScanShieldedTRC20NoteByovk failed,error " + status.getDescription());

    }
    return null;



  }

  /**
   * constructor.
   */
  public byte[] getRandomOvk() {
    try {
      Optional<BytesMessage> sk = Optional.of(blockingStubFull
          .getSpendingKey(EmptyMessage.newBuilder().build()));
      Optional<GrpcAPI.ExpandedSpendingKeyMessage> expandedSpendingKeyMessage
          = Optional.of(blockingStubFull
          .getExpandedSpendingKey(sk.get()));
      return expandedSpendingKeyMessage.get().getOvk().toByteArray();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * constructor.
   */
  public BigInteger getRandomAmount() {
    Random random = new Random();
    int x = random.nextInt(100000) + 100;
    return BigInteger.valueOf(x);
  }


  public String encodeTransferParamsToHexString(GrpcAPI.ShieldedTRC20Parameters parameters) {
    byte[] input = new byte[0];
    byte[] spendAuthSig = new byte[0];
    byte[] output = new byte[0];
    byte[] c = new byte[0];
    byte[] bindingSig;
    byte[] mergedBytes;
    List<ShieldContract.SpendDescription> spendDescs = parameters.getSpendDescriptionList();
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
    byte[] inputOffsetbytes = longTo32Bytes(192);
    long spendCount = spendDescs.size();
    byte[] spendCountBytes = longTo32Bytes(spendCount);
    byte[] authOffsetBytes = longTo32Bytes(192 + 32 + 320 * spendCount);
    List<ShieldContract.ReceiveDescription> recvDescs = parameters.getReceiveDescriptionList();
    for (ShieldContract.ReceiveDescription recvDesc : recvDescs) {
      output = ByteUtil.merge(output,
          recvDesc.getNoteCommitment().toByteArray(),
          recvDesc.getValueCommitment().toByteArray(),
          recvDesc.getEpk().toByteArray(),
          recvDesc.getZkproof().toByteArray()
      );
      c = ByteUtil.merge(c,
          recvDesc.getCEnc().toByteArray(),
          recvDesc.getCOut().toByteArray(),
          new byte[12]
      );
    }
    long recvCount = recvDescs.size();
    byte[] recvCountBytes = longTo32Bytes(recvCount);
    byte[] outputOffsetbytes = longTo32Bytes(192 + 32 + 320 * spendCount + 32 + 64 * spendCount);
    byte[] coffsetBytes = longTo32Bytes(192 + 32 + 320 * spendCount + 32 + 64 * spendCount + 32
        + 288 * recvCount);
    bindingSig = parameters.getBindingSignature().toByteArray();
    mergedBytes = ByteUtil.merge(inputOffsetbytes,
        authOffsetBytes,
        outputOffsetbytes,
        bindingSig,
        coffsetBytes,
        spendCountBytes,
        input,
        spendCountBytes,
        spendAuthSig,
        recvCountBytes,
        output,
        recvCountBytes,
        c
    );
    return ByteArray.toHexString(mergedBytes);
  }

  public String encodeBurnParamsToHexString(GrpcAPI.ShieldedTRC20Parameters parameters, BigInteger value,
      String transparentToAddress) {
    byte[] mergedBytes;
    byte[] payTo = new byte[32];
    byte[] transparentToAddressBytes = wallet.decodeFromBase58Check(transparentToAddress);
    System.arraycopy(transparentToAddressBytes, 0, payTo, 11, 21);
    ShieldContract.SpendDescription spendDesc = parameters.getSpendDescription(0);
    mergedBytes = ByteUtil.merge(
        spendDesc.getNullifier().toByteArray(),
        spendDesc.getAnchor().toByteArray(),
        spendDesc.getValueCommitment().toByteArray(),
        spendDesc.getRk().toByteArray(),
        spendDesc.getZkproof().toByteArray(),
        spendDesc.getSpendAuthoritySignature().toByteArray(),
        ByteUtil.bigIntegerToBytes(value, 32),
        parameters.getBindingSignature().toByteArray(),
        payTo
    );
    return ByteArray.toHexString(mergedBytes);
  }



  public byte[] longTo32Bytes(long value) {
    byte[] longBytes = ByteArray.fromLong(value);
    byte[] zeroBytes = new byte[24];
    return ByteUtil.merge(zeroBytes, longBytes);
  }









}
