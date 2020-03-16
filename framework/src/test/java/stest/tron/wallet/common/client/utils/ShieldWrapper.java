package stest.tron.wallet.common.client.utils;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.netty.util.internal.StringUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Getter;
import lombok.Setter;
import org.tron.api.GrpcAPI.DecryptNotes;
import org.tron.api.GrpcAPI.DecryptNotes.NoteTx;
import org.tron.api.GrpcAPI.IvkDecryptParameters;
import org.tron.api.GrpcAPI.Note;
import org.tron.api.GrpcAPI.NoteParameters;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.SpendResult;
import org.tron.api.WalletGrpc;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol.Block;
import org.tron.protos.contract.ShieldContract.IncrementalMerkleVoucherInfo;
import org.tron.protos.contract.ShieldContract.OutputPoint;
import org.tron.protos.contract.ShieldContract.OutputPointInfo;
import stest.tron.wallet.common.client.Configuration;

//import org.tron.walletserver.WalletApi;
//import stest.tron.wallet.common.client.Parameter.CommonConstant;

public class ShieldWrapper {

  private static final String PREFIX_FOLDER = "WalletShield";
  private static final String IVK_AND_NUM_FILE_NAME = PREFIX_FOLDER + "/scanblocknumber";
  private static final String UNSPEND_NOTE_FILE_NAME = PREFIX_FOLDER + "/unspendnote";
  private static final String SPEND_NOTE_FILE_NAME = PREFIX_FOLDER + "/spendnote";
  private static final String SHIELD_ADDRESS_FILE_NAME = PREFIX_FOLDER + "/shieldaddress";
  //private WalletApi wallet;
  private static AtomicLong nodeIndex = new AtomicLong(0L);
  @Getter
  @Setter
  public Map<String, Long> ivkMapScanBlockNum = new ConcurrentHashMap();
  @Getter
  @Setter
  public Map<Long, ShieldNoteInfo> utxoMapNote = new ConcurrentHashMap();

  //Wallet wallet = new Wallet();
  @Getter
  @Setter
  public List<ShieldNoteInfo> spendUtxoList = new ArrayList<>();
  @Setter
  @Getter
  Map<String, ShieldAddressInfo> shieldAddressInfoMap = new ConcurrentHashMap();
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private ManagedChannel channelFull = ManagedChannelBuilder.forTarget(fullnode)
      .usePlaintext(true)
      .build();
  private WalletGrpc.WalletBlockingStub blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  private Thread thread;
  @Setter
  private boolean resetNote = false;

  /*  public void setWallet(WalletApi walletApi) {
    wallet = walletApi;
    if (!thread.isAlive()) {
      thread.start();
    }
  }*/

  private void resetShieldNote() {
    ivkMapScanBlockNum.clear();
    for (Entry<String, ShieldAddressInfo> entry : getShieldAddressInfoMap().entrySet()) {
      ivkMapScanBlockNum.put(ByteArray.toHexString(entry.getValue().getIvk()), 0L);
    }

    utxoMapNote.clear();
    spendUtxoList.clear();

    ZenUtils.clearFile(IVK_AND_NUM_FILE_NAME);
    ZenUtils.clearFile(UNSPEND_NOTE_FILE_NAME);
    ZenUtils.clearFile(SPEND_NOTE_FILE_NAME);
    nodeIndex.set(0L);

    updateIvkAndBlockNumFile();
  }

  private void scanBlockByIvk() {
    try {
      NumberMessage.Builder builder1 = NumberMessage.newBuilder();
      builder1.setNum(-1);
      Block block = blockingStubFull.getBlockByNum(builder1.build());
      if (block != null) {
        long blockNum = block.getBlockHeader().toBuilder().getRawData().getNumber();
        for (Entry<String, Long> entry : ivkMapScanBlockNum.entrySet()) {

          long start = entry.getValue();
          long end = start;
          while (end < blockNum) {
            if (blockNum - start > 1000) {
              end = start + 1000;
            } else {
              end = blockNum;
            }

            IvkDecryptParameters.Builder builder = IvkDecryptParameters.newBuilder();
            builder.setStartBlockIndex(start);
            builder.setEndBlockIndex(end);
            builder.setIvk(ByteString.copyFrom(ByteArray.fromHexString(entry.getKey())));
            DecryptNotes notes = blockingStubFull.scanNoteByIvk(builder.build());
            if (notes != null) {
              for (int i = 0; i < notes.getNoteTxsList().size(); ++i) {
                NoteTx noteTx = notes.getNoteTxsList().get(i);
                ShieldNoteInfo noteInfo = new ShieldNoteInfo();
                noteInfo.setPaymentAddress(noteTx.getNote().getPaymentAddress());
                noteInfo.setR(noteTx.getNote().getRcm().toByteArray());
                noteInfo.setValue(noteTx.getNote().getValue());
                noteInfo.setTrxId(ByteArray.toHexString(noteTx.getTxid().toByteArray()));
                noteInfo.setIndex(noteTx.getIndex());
                noteInfo.setNoteIndex(nodeIndex.getAndIncrement());
                noteInfo.setMemo(noteTx.getNote().getMemo().toByteArray());

                utxoMapNote.put(noteInfo.getNoteIndex(), noteInfo);
              }
              saveUnspendNoteToFile();
            }
            start = end;
          }

          ivkMapScanBlockNum.put(entry.getKey(), blockNum);
        }
        updateIvkAndBlockNumFile();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void updateNoteWhetherSpend() {
    try {
      for (Entry<Long, ShieldNoteInfo> entry : utxoMapNote.entrySet()) {
        ShieldNoteInfo noteInfo = entry.getValue();

        OutputPointInfo.Builder request = OutputPointInfo.newBuilder();
        OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
        outPointBuild.setHash(ByteString.copyFrom(ByteArray.fromHexString(noteInfo.getTrxId())));
        outPointBuild.setIndex(noteInfo.getIndex());
        request.addOutPoints(outPointBuild.build());

        IncrementalMerkleVoucherInfo merkleVoucherInfo = blockingStubFull.getMerkleTreeVoucherInfo(
            request.build());
        if (merkleVoucherInfo.getVouchersCount() > 0) {
          ShieldAddressInfo addressInfo = getShieldAddressInfoMap().get(
              noteInfo.getPaymentAddress());
          NoteParameters.Builder builder = NoteParameters.newBuilder();
          builder.setAk(ByteString.copyFrom(addressInfo.getFullViewingKey().getAk()));
          builder.setNk(ByteString.copyFrom(addressInfo.getFullViewingKey().getNk()));

          Note.Builder noteBuild = Note.newBuilder();
          noteBuild.setPaymentAddress(noteInfo.getPaymentAddress());
          noteBuild.setValue(noteInfo.getValue());
          noteBuild.setRcm(ByteString.copyFrom(noteInfo.getR()));
          noteBuild.setMemo(ByteString.copyFrom(noteInfo.getMemo()));
          builder.setNote(noteBuild.build());
          //builder.setVoucher(merkleVoucherInfo.getVouchers(0));

          SpendResult result = blockingStubFull.isSpend(builder.build());

          if (result.getResult()) {
            spendNote(entry.getKey());
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public boolean init() {
    ZenUtils.checkFolderExist(PREFIX_FOLDER);

    loadAddressFromFile();
    loadIvkFromFile();
    loadUnSpendNoteFromFile();
    loadSpendNoteFromFile();

    thread = new Thread(new scanIvkRunable());
    return true;
  }

  /**
   * constructor.
   */
  public boolean spendNote(long noteIndex) {
    ShieldNoteInfo noteInfo = utxoMapNote.get(noteIndex);
    if (noteInfo != null) {
      utxoMapNote.remove(noteIndex);
      spendUtxoList.add(noteInfo);

      saveUnspendNoteToFile();
      saveSpendNoteToFile(noteInfo);
    } else {
      System.err.println("Find note failure. index:" + noteIndex);
    }
    return true;
  }

  /**
   * constructor.
   */
  public boolean addNewShieldAddress(final ShieldAddressInfo addressInfo) {
    appendAddressInfoToFile(addressInfo);
    long blockNum = 0;
    try {
      NumberMessage.Builder builder1 = NumberMessage.newBuilder();
      builder1.setNum(-1);
      Block block = blockingStubFull.getBlockByNum(builder1.build());
      if (block != null) {
        blockNum = block.getBlockHeader().toBuilder().getRawData().getNumber();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    ivkMapScanBlockNum.put(ByteArray.toHexString(addressInfo.getIvk()), blockNum);
    updateIvkAndBlockNum(ByteArray.toHexString(addressInfo.getIvk()), blockNum);

    return true;
  }

  /**
   * constructor.
   */
  private boolean updateIvkAndBlockNum(final String ivk, long blockNum) {
    synchronized (IVK_AND_NUM_FILE_NAME) {
      String date = ivk + ";" + blockNum;
      ZenUtils.appendToFileTail(IVK_AND_NUM_FILE_NAME, date);
    }
    return true;
  }

  /**
   * constructor.
   */
  private boolean updateIvkAndBlockNumFile() {
    synchronized (IVK_AND_NUM_FILE_NAME) {
      ZenUtils.clearFile(IVK_AND_NUM_FILE_NAME);
      for (Entry<String, Long> entry : ivkMapScanBlockNum.entrySet()) {
        String date = entry.getKey() + ";" + entry.getValue();
        ZenUtils.appendToFileTail(IVK_AND_NUM_FILE_NAME, date);
      }
    }
    return true;
  }

  /**
   * constructor.
   */
  private boolean loadIvkFromFile() {
    ivkMapScanBlockNum.clear();
    List<String> list = ZenUtils.getListFromFile(IVK_AND_NUM_FILE_NAME);
    for (int i = 0; i < list.size(); ++i) {
      String[] sourceStrArray = list.get(i).split(";");
      if (sourceStrArray.length != 2) {
        System.err.println("len is not right.");
        return false;
      }
      ivkMapScanBlockNum.put(sourceStrArray[0], Long.valueOf(sourceStrArray[1]));
    }
    return true;
  }

  /**
   * get shield address list.
   */
  public List<String> getShieldAddressList() {
    List<String> addressList = new ArrayList<>();
    for (Entry<String, ShieldAddressInfo> entry : shieldAddressInfoMap.entrySet()) {
      addressList.add(entry.getKey());
    }
    return addressList;
  }

  /**
   * update unspend note.
   */
  private boolean saveUnspendNoteToFile() {
    ZenUtils.clearFile(UNSPEND_NOTE_FILE_NAME);
    for (Entry<Long, ShieldNoteInfo> entry : utxoMapNote.entrySet()) {
      String date = entry.getValue().encode();
      ZenUtils.appendToFileTail(UNSPEND_NOTE_FILE_NAME, date);
    }
    return true;
  }

  /**
   * load unspend note from file.
   */
  private boolean loadUnSpendNoteFromFile() {
    utxoMapNote.clear();

    List<String> list = ZenUtils.getListFromFile(UNSPEND_NOTE_FILE_NAME);
    for (int i = 0; i < list.size(); ++i) {
      ShieldNoteInfo noteInfo = new ShieldNoteInfo();
      noteInfo.decode(list.get(i));
      utxoMapNote.put(noteInfo.getNoteIndex(), noteInfo);

      if (noteInfo.getNoteIndex() > nodeIndex.get()) {
        nodeIndex.set(noteInfo.getNoteIndex());
      }
    }
    return true;
  }

  /**
   * append spend note to file tail.
   */
  private boolean saveSpendNoteToFile(ShieldNoteInfo noteInfo) {
    String date = noteInfo.encode();
    ZenUtils.appendToFileTail(SPEND_NOTE_FILE_NAME, date);
    return true;
  }

  /**
   * load spend note from file.
   */
  private boolean loadSpendNoteFromFile() {
    spendUtxoList.clear();
    List<String> list = ZenUtils.getListFromFile(SPEND_NOTE_FILE_NAME);
    for (int i = 0; i < list.size(); ++i) {
      ShieldNoteInfo noteInfo = new ShieldNoteInfo();
      noteInfo.decode(list.get(i));
      spendUtxoList.add(noteInfo);
    }
    return true;
  }

  /**
   * load shield address from file.
   */
  public boolean loadAddressFromFile() {
    List<String> addressList = ZenUtils.getListFromFile(SHIELD_ADDRESS_FILE_NAME);

    shieldAddressInfoMap.clear();
    for (String addressString : addressList) {
      ShieldAddressInfo addressInfo = new ShieldAddressInfo();
      if (addressInfo.decode(addressString)) {
        shieldAddressInfoMap.put(addressInfo.getAddress(), addressInfo);
      } else {
        System.out.println("*******************");
      }
    }
    return true;
  }

  /**
   * constructor.
   */
  public boolean appendAddressInfoToFile(final ShieldAddressInfo addressInfo) {
    String shieldAddress = addressInfo.getAddress();
    if (!StringUtil.isNullOrEmpty(shieldAddress)) {
      String addressString = addressInfo.encode();
      ZenUtils.appendToFileTail(SHIELD_ADDRESS_FILE_NAME, addressString);

      shieldAddressInfoMap.put(shieldAddress, addressInfo);
    }
    return true;
  }

  /**
   * sort by value of UTXO.
   */
  public List<String> getvalidateSortUtxoList() {
    List<Map.Entry<Long, ShieldNoteInfo>> list = new ArrayList<>(utxoMapNote.entrySet());
    Collections.sort(list, (Entry<Long, ShieldNoteInfo> o1, Entry<Long, ShieldNoteInfo> o2) -> {
      if (o1.getValue().getValue() < o2.getValue().getValue()) {
        return 1;
      } else {
        return -1;
      }
    });

    List<String> utxoList = new ArrayList<>();
    for (Map.Entry<Long, ShieldNoteInfo> entry : list) {
      String string = entry.getKey() + " " + entry.getValue().getPaymentAddress() + " ";
      string += entry.getValue().getValue();
      string += " ";
      string += entry.getValue().getTrxId();
      string += " ";
      string += entry.getValue().getIndex();
      string += " ";
      string += "UnSpend";
      string += " ";
      string += ZenUtils.getMemo(entry.getValue().getMemo());
      utxoList.add(string);
    }
    return utxoList;
  }

  public class scanIvkRunable implements Runnable {

    public void run() {
      for (; ; ) {
        try {
          scanBlockByIvk();
          updateNoteWhetherSpend();
          //wait for 2.5 seconds
          for (int i = 0; i < 5; ++i) {
            Thread.sleep(500);
            if (resetNote) {
              resetShieldNote();
              resetNote = false;
              System.out.println("Reset shield note success!");
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }


}
