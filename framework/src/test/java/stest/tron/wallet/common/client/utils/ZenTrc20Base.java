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
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.spongycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.Note;
import org.tron.common.utils.ByteUtil;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.vm.trace.Op;
import org.tron.core.zen.address.DiversifierT;
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

@Slf4j
public class ZenTrc20Base {

  public final String foundationAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  public final byte[] foundationAccountAddress = PublicMethed.getFinalAddress(foundationAccountKey);
  public final String zenTrc20TokenOwnerKey = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTrc20TokenOwnerKey");
  public final byte[] zenTrc20TokenOwnerAddress = PublicMethed.getFinalAddress(zenTrc20TokenOwnerKey);
  public final String zenTrc20TokenOwnerAddressString = PublicMethed.getAddressString(zenTrc20TokenOwnerKey);
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
  public Wallet wallet = new Wallet();

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
            maxFeeLimit, 0L, 100, null,zenTrc20TokenOwnerKey, zenTrc20TokenOwnerAddress, blockingStubFull);

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
            maxFeeLimit, 0L, 100, null, zenTrc20TokenOwnerKey, zenTrc20TokenOwnerAddress, blockingStubFull);
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



  public GrpcAPI.ShieldedTRC20Parameters createShieldedTrc20Parameters(BigInteger publicFromAmount,
      List<Note> inputNoteList,List<Note> outputNoteList,  String publicToAddress,Long pubicToAmount,
      WalletGrpc.WalletBlockingStub blockingStubFull) {


    GrpcAPI.PrivateShieldedTRC20Parameters.Builder builder = GrpcAPI.PrivateShieldedTRC20Parameters.newBuilder();
    builder.setFromAmount(publicFromAmount.toString());
    builder.setShieldedTRC20ContractAddress(shieldAddressByteString);
    long valueBalance = 0;
    if (outputNoteList.size() > 0) {
      for (int i = 0; i < outputNoteList.size(); i++) {
        GrpcAPI.Note note = outputNoteList.get(i);
        valueBalance = Math.subtractExact(valueBalance, note.getValue());
        builder.addShieldedReceives(
            GrpcAPI.ReceiveNote.newBuilder().setNote(note).build());
      }
    }


    GrpcAPI.ShieldedTRC20Parameters parameters =
        blockingStubFull.createShieldedContractParameters(builder.build());
    return parameters;
  }


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



/*  public Optional<ShieldedAddressInfo> getNewShieldedAddress(WalletGrpc.WalletBlockingStub blockingStubFull) {
    ShieldedAddressInfo addressInfo = new ShieldedAddressInfo();
    try {
      Optional<BytesMessage> sk = Optional.of(blockingStubFull.getSpendingKey(
          EmptyMessage.newBuilder().build()));
      Optional<GrpcAPI.DiversifierMessage> d = Optional.of(blockingStubFull.getDiversifier(EmptyMessage.newBuilder().build()));

      Optional<GrpcAPI.ExpandedSpendingKeyMessage> expandedSpendingKeyMessage = Optional.of(blockingStubFull
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

      GrpcAPI.IncomingViewingKeyDiversifierMessage.Builder builder = GrpcAPI.IncomingViewingKeyDiversifierMessage
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


      if (addressInfo.validateCheck()) {
        return Optional.of(addressInfo);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    logger.info("address:" + addressInfo.getAddress());
    return Optional.empty();
  }*/


  public Optional<ShieldedAddressInfo> getNewShieldedAddress(WalletGrpc.WalletBlockingStub blockingStubFull) {
    ShieldedAddressInfo addressInfo = new ShieldedAddressInfo();


    try {
      Optional<BytesMessage> sk = Optional.of(blockingStubFull.getSpendingKey(EmptyMessage.newBuilder().build()));
      Optional<GrpcAPI.DiversifierMessage> d = Optional.of(blockingStubFull.getDiversifier(
          EmptyMessage.newBuilder().build()));

      Optional<GrpcAPI.ExpandedSpendingKeyMessage> expandedSpendingKeyMessage = Optional.of(blockingStubFull
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

      GrpcAPI.IncomingViewingKeyDiversifierMessage.Builder builder = GrpcAPI.IncomingViewingKeyDiversifierMessage
          .newBuilder();
      builder.setD(d.get());
      builder.setIvk(ivk.get());
      Optional<GrpcAPI.PaymentAddressMessage> addressMessage = Optional.of(blockingStubFull.
          getZenPaymentAddress(builder.build()));
      addressInfo.setSk(sk.get().getValue().toByteArray());
      addressInfo.setD(new DiversifierT(d.get().getD().toByteArray()));
      addressInfo.setIvk(ivk.get().getIvk().toByteArray());
      addressInfo.setOvk(expandedSpendingKeyMessage.get().getOvk().toByteArray());
      addressInfo.setPkD(addressMessage.get().getPkD().toByteArray());


      System.out.println("ivk " + ByteArray.toHexString(ivk.get().getIvk().toByteArray()));
      System.out.println("ovk " + ByteArray.toHexString(expandedSpendingKeyMessage.get().getOvk().toByteArray()));

/*      if (addressInfo.validateCheck()) {

      }*/
      return Optional.of(addressInfo);

    } catch (Exception e) {
      e.printStackTrace();
    }

    return Optional.empty();
  }




  /**
   * constructor.
   */
  public static List<Note> addShieldOutputList(List<Note> shieldOutList, String shieldToAddress,
      Long toAmount, String menoString) {


      Note.Builder noteBuild = Note.newBuilder();
      noteBuild.setPaymentAddress(shieldToAddress);
      noteBuild.setPaymentAddress(shieldToAddress);
      noteBuild.setValue(toAmount);
    try {
      noteBuild.setRcm(ByteString.copyFrom(org.tron.core.zen.note.Note.generateR()));
    } catch (Exception e) {
      System.out.println(e);
    }
      noteBuild.setMemo(ByteString.copyFrom(menoString.getBytes()));
    shieldOutList.add(noteBuild.build());
    return  shieldOutList;
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

    String hexBalance = Hex.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    for(int i = 0; i < hexBalance.length();i++) {
      if (hexBalance.charAt(i) != '0') {
        hexBalance = hexBalance.substring(i);
        break;
      }
    }
    logger.info(hexBalance);
    return Long.parseLong(hexBalance,16);
  }

  public GrpcAPI.DecryptNotesTRC20 scanShieldedTRC20NoteByIvk(ShieldedAddressInfo shieldedAddressInfo,
      WalletGrpc.WalletBlockingStub blockingStubFull) throws Exception {
    Long currentBlockNum = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
        .getBlockHeader().getRawData().getNumber();

    System.out.println("sk :" + ByteArray.toHexString(shieldedAddressInfo.getSk()));
    String spendingKey = ByteArray.toHexString(shieldedAddressInfo.getSk());
    BytesMessage sk = BytesMessage.newBuilder()
        .setValue(ByteString.copyFrom(ByteArray.fromHexString(spendingKey))).build();
    Optional<GrpcAPI.ExpandedSpendingKeyMessage> esk = Optional
        .of(blockingStubFull.getExpandedSpendingKey(sk));
    System.out.println("ask:" + ByteArray.toHexString(esk.get().getAsk().toByteArray()));
    System.out.println("nsk:" + ByteArray.toHexString(esk.get().getNsk().toByteArray()));
    System.out.println("ovk:" + ByteArray.toHexString(esk.get().getOvk().toByteArray()));

    String ask = ByteArray.toHexString(esk.get().getAsk().toByteArray());

    BytesMessage ask1 = BytesMessage.newBuilder()
        .setValue(ByteString.copyFrom(ByteArray.fromHexString(ask))).build();
    Optional<BytesMessage> ak = Optional.of(blockingStubFull.getAkFromAsk(ask1));
    String akString = ByteArray.toHexString(ak.get().getValue().toByteArray());
    System.out.println("ak:" + ByteArray.toHexString(ak.get().getValue().toByteArray()));

    String nsk = ByteArray.toHexString(esk.get().getNsk().toByteArray());

    BytesMessage nsk1 = BytesMessage.newBuilder()
        .setValue(ByteString.copyFrom(ByteArray.fromHexString(nsk))).build();
    Optional<BytesMessage> nk = Optional.of(blockingStubFull.getNkFromNsk(nsk1));
    System.out.println("nk:" + ByteArray.toHexString(nk.get().getValue().toByteArray()));
    String nkString = ByteArray.toHexString(nk.get().getValue().toByteArray());
    System.out.println("ivk:" + ByteArray.toHexString(shieldedAddressInfo.getIvk()));
    String ivkString = ByteArray.toHexString(shieldedAddressInfo.getIvk());

    System.out.println("shield address:" + shieldedAddressInfo.getAddress());
    System.out.println("start:" + (currentBlockNum - 99));
    System.out.println("end:" + (currentBlockNum));
    GrpcAPI.IvkDecryptTRC20Parameters parameters = GrpcAPI.IvkDecryptTRC20Parameters
        .newBuilder()
        .setStartBlockIndex(currentBlockNum - 99)
        .setEndBlockIndex(currentBlockNum)
        .setShieldedTRC20ContractAddress(ByteString.copyFrom(shieldAddressByte))
        .setIvk(ByteString.copyFrom(ByteArray.fromHexString(ivkString)))
        .setAk(ByteString.copyFrom(ByteArray.fromHexString(akString)))
        .setNk(ByteString.copyFrom(ByteArray.fromHexString(nkString)))
        .build();
    logger.info("check");
    try {
      return blockingStubFull.scanShieldedTRC20NotesbyIvk(parameters);
    } catch (Exception e) {
      Status status = Status.fromThrowable(e);
      System.out.println("ScanShieldedTRC20NoteByIvk failed,error " + status.getDescription());

    }
    return null;
  }







}
