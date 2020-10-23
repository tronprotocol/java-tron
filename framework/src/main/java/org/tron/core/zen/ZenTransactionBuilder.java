package org.tron.core.zen;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.utils.ByteArray;
import org.tron.common.zksnark.IncrementalMerkleVoucherContainer;
import org.tron.common.zksnark.JLibrustzcash;
import org.tron.common.zksnark.LibrustzcashParam.BindingSigParams;
import org.tron.common.zksnark.LibrustzcashParam.OutputProofParams;
import org.tron.common.zksnark.LibrustzcashParam.SpendProofParams;
import org.tron.common.zksnark.LibrustzcashParam.SpendSigParams;
import org.tron.core.Wallet;
import org.tron.core.capsule.ReceiveDescriptionCapsule;
import org.tron.core.capsule.SpendDescriptionCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.zen.address.DiversifierT;
import org.tron.core.zen.address.ExpandedSpendingKey;
import org.tron.core.zen.address.PaymentAddress;
import org.tron.core.zen.note.Note;
import org.tron.core.zen.note.Note.NotePlaintextEncryptionResult;
import org.tron.core.zen.note.NoteEncryption;
import org.tron.core.zen.note.OutgoingPlaintext;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.ShieldContract.ShieldedTransferContract;

@Slf4j
public class ZenTransactionBuilder {

  @Setter
  @Getter
  private String from;
  @Setter
  @Getter
  private List<SpendDescriptionInfo> spends = new ArrayList<>();
  @Setter
  @Getter
  private List<ReceiveDescriptionInfo> receives = new ArrayList<>();

  private Wallet wallet;

  @Getter
  private long valueBalance = 0;

  @Setter
  @Getter
  private long timeout = 0;

  @Getter
  private ShieldedTransferContract.Builder contractBuilder = ShieldedTransferContract.newBuilder();

  public ZenTransactionBuilder(Wallet wallet) {
    this.wallet = wallet;
  }

  public ZenTransactionBuilder() {
  }

  public void addSpend(SpendDescriptionInfo spendDescriptionInfo) {
    spends.add(spendDescriptionInfo);
    valueBalance += spendDescriptionInfo.note.getValue();
  }

  public void addSpend(
      ExpandedSpendingKey expsk,
      Note note,
      byte[] anchor,
      IncrementalMerkleVoucherContainer voucher) throws ZksnarkException {
    spends.add(new SpendDescriptionInfo(expsk, note, anchor, voucher));
    valueBalance += note.getValue();
  }

  public void addSpend(
      ExpandedSpendingKey expsk,
      Note note,
      byte[] alpha,
      byte[] anchor,
      IncrementalMerkleVoucherContainer voucher) {
    spends.add(new SpendDescriptionInfo(expsk, note, alpha, anchor, voucher));
    valueBalance += note.getValue();
  }

  public void addSpend(
      byte[] ak,
      byte[] nsk,
      byte[] ovk,
      Note note,
      byte[] alpha,
      byte[] anchor,
      IncrementalMerkleVoucherContainer voucher) {
    spends.add(new SpendDescriptionInfo(ak, nsk, ovk, note, alpha, anchor, voucher));
    valueBalance += note.getValue();
  }

  public void addOutput(byte[] ovk, PaymentAddress to, long value, byte[] memo)
      throws ZksnarkException {
    Note note = new Note(to, value);
    note.setMemo(memo);
    receives.add(new ReceiveDescriptionInfo(ovk, note));
    valueBalance -= value;
  }

  public void addOutput(byte[] ovk, DiversifierT d, byte[] pkD, long value, byte[] r, byte[] memo) {
    Note note = new Note(d, pkD, value, r);
    note.setMemo(memo);
    receives.add(new ReceiveDescriptionInfo(ovk, note));
    valueBalance -= value;
  }

  public void setTransparentInput(byte[] address, long value) {
    contractBuilder.setTransparentFromAddress(ByteString.copyFrom(address))
        .setFromAmount(value);
  }

  public void setTransparentOutput(byte[] address, long value) {
    contractBuilder.setTransparentToAddress(ByteString.copyFrom(address))
        .setToAmount(value);
  }

  public TransactionCapsule buildWithoutAsk() throws ZksnarkException {
    return build(false);
  }

  public TransactionCapsule build() throws ZksnarkException {
    return build(true);
  }

  public TransactionCapsule build(boolean withAsk) throws ZksnarkException {
    TransactionCapsule transactionCapsule;
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

    try {
      // Create SpendDescriptions
      for (SpendDescriptionInfo spend : spends) {
        SpendDescriptionCapsule spendDescriptionCapsule = generateSpendProof(spend, ctx);
        contractBuilder.addSpendDescription(spendDescriptionCapsule.getInstance());
      }

      // Create OutputDescriptions
      for (ReceiveDescriptionInfo receive : receives) {
        ReceiveDescriptionCapsule receiveDescriptionCapsule = generateOutputProof(receive, ctx);
        contractBuilder.addReceiveDescription(receiveDescriptionCapsule.getInstance());
      }

      // Empty output script
      byte[] dataHashToBeSigned; //256
      transactionCapsule = wallet.createTransactionCapsuleWithoutValidate(
          contractBuilder.build(), ContractType.ShieldedTransferContract, timeout);

      dataHashToBeSigned = TransactionCapsule
          .getShieldTransactionHashIgnoreTypeException(transactionCapsule.getInstance());

      if (dataHashToBeSigned == null) {
        throw new ZksnarkException("cal transaction hash failed");
      }

      // Create spendAuth and binding signatures
      if (withAsk) {
        createSpendAuth(dataHashToBeSigned);
      }

      byte[] bindingSig = new byte[64];
      JLibrustzcash.librustzcashSaplingBindingSig(
          new BindingSigParams(ctx,
              valueBalance,
              dataHashToBeSigned,
              bindingSig)
      );
      contractBuilder.setBindingSignature(ByteString.copyFrom(bindingSig));
    } catch (ZksnarkException e) {
      throw e;
    } finally {
      JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
    }
    Transaction.raw.Builder rawBuilder = transactionCapsule.getInstance().toBuilder()
        .getRawDataBuilder()
        .clearContract()
        .addContract(
            Transaction.Contract.newBuilder().setType(ContractType.ShieldedTransferContract)
                .setParameter(
                    Any.pack(contractBuilder.build())).build());
    Transaction transaction = transactionCapsule.getInstance().toBuilder().clearRawData()
        .setRawData(rawBuilder).build();
    return new TransactionCapsule(transaction);
  }

  public void createSpendAuth(byte[] dataToBeSigned) throws ZksnarkException {
    for (int i = 0; i < spends.size(); i++) {
      byte[] result = new byte[64];
      JLibrustzcash.librustzcashSaplingSpendSig(
          new SpendSigParams(spends.get(i).expsk.getAsk(),
              spends.get(i).alpha,
              dataToBeSigned,
              result));
      contractBuilder.getSpendDescriptionBuilder(i)
          .setSpendAuthoritySignature(ByteString.copyFrom(result));
    }
  }

  // Note: should call librustzcashSaplingProvingCtxFree in the caller
  public SpendDescriptionCapsule generateSpendProof(SpendDescriptionInfo spend,
      long ctx) throws ZksnarkException {

    byte[] cm = spend.note.cm();

    // check if ak exists
    byte[] ak;
    byte[] nf;
    byte[] nsk;
    if (!ArrayUtils.isEmpty(spend.ak)) {
      ak = spend.ak;
      nf = spend.note.nullifier(ak, JLibrustzcash.librustzcashNskToNk(spend.nsk),
          spend.voucher.position());
      nsk = spend.nsk;
    } else {
      ak = spend.expsk.fullViewingKey().getAk();
      nf = spend.note.nullifier(spend.expsk.fullViewingKey(), spend.voucher.position());
      nsk = spend.expsk.getNsk();
    }

    if (ByteArray.isEmpty(cm) || ByteArray.isEmpty(nf)) {
      throw new ZksnarkException("Spend is invalid");
    }

    byte[] voucherPath = spend.voucher.path().encode();

    byte[] cv = new byte[32];
    byte[] rk = new byte[32];
    byte[] zkproof = new byte[192];
    if (!JLibrustzcash.librustzcashSaplingSpendProof(
        new SpendProofParams(ctx,
            ak,
            nsk,
            spend.note.getD().getData(),
            spend.note.getRcm(),
            spend.alpha,
            spend.note.getValue(),
            spend.anchor,
            voucherPath,
            cv,
            rk,
            zkproof))) {
      throw new ZksnarkException("Spend proof failed");
    }
    SpendDescriptionCapsule spendDescriptionCapsule = new SpendDescriptionCapsule();
    spendDescriptionCapsule.setValueCommitment(cv);
    spendDescriptionCapsule.setRk(rk);
    spendDescriptionCapsule.setZkproof(zkproof);
    spendDescriptionCapsule.setAnchor(spend.anchor);
    spendDescriptionCapsule.setNullifier(nf);
    return spendDescriptionCapsule;
  }

  // Note: should call librustzcashSaplingProvingCtxFree in the caller
  public ReceiveDescriptionCapsule generateOutputProof(ReceiveDescriptionInfo output, long ctx)
      throws ZksnarkException {
    byte[] cm = output.getNote().cm();
    if (ByteArray.isEmpty(cm)) {
      throw new ZksnarkException("Output is invalid");
    }

    Optional<NotePlaintextEncryptionResult> res = output.getNote()
        .encrypt(output.getNote().getPkD());
    if (!res.isPresent()) {
      throw new ZksnarkException("Failed to encrypt note");
    }

    NotePlaintextEncryptionResult enc = res.get();
    NoteEncryption encryptor = enc.getNoteEncryption();

    byte[] cv = new byte[32];
    byte[] zkProof = new byte[192];
    if (!JLibrustzcash.librustzcashSaplingOutputProof(
        new OutputProofParams(ctx,
            encryptor.getEsk(),
            output.getNote().getD().getData(),
            output.getNote().getPkD(),
            output.getNote().getRcm(),
            output.getNote().getValue(),
            cv,
            zkProof))) {
      throw new ZksnarkException("Output proof failed");
    }

    if (ArrayUtils.isEmpty(output.ovk) || output.ovk.length != 32) {
      throw new ZksnarkException("ovk is null or invalid and ovk should be 32 bytes (256 bit)");
    }

    ReceiveDescriptionCapsule receiveDescriptionCapsule = new ReceiveDescriptionCapsule();
    receiveDescriptionCapsule.setValueCommitment(cv);
    receiveDescriptionCapsule.setNoteCommitment(cm);
    receiveDescriptionCapsule.setEpk(encryptor.getEpk());
    receiveDescriptionCapsule.setCEnc(enc.getEncCiphertext());
    receiveDescriptionCapsule.setZkproof(zkProof);

    OutgoingPlaintext outPlaintext =
        new OutgoingPlaintext(output.getNote().getPkD(), encryptor.getEsk());
    receiveDescriptionCapsule.setCOut(outPlaintext
        .encrypt(output.ovk, receiveDescriptionCapsule.getValueCommitment().toByteArray(),
            receiveDescriptionCapsule.getCm().toByteArray(),
            encryptor).getData());
    return receiveDescriptionCapsule;
  }

  public static class SpendDescriptionInfo {

    @Getter
    @Setter
    private ExpandedSpendingKey expsk;
    @Getter
    @Setter
    private Note note;
    @Getter
    @Setter
    private byte[] alpha;
    @Getter
    @Setter
    private byte[] anchor;
    @Getter
    @Setter
    private IncrementalMerkleVoucherContainer voucher;
    @Getter
    @Setter
    private byte[] ak;
    @Getter
    @Setter
    private byte[] nsk;
    @Getter
    @Setter
    private byte[] ovk;

    public SpendDescriptionInfo(
        ExpandedSpendingKey expsk,
        Note note,
        byte[] anchor,
        IncrementalMerkleVoucherContainer voucher) throws ZksnarkException {
      this.expsk = expsk;
      this.note = note;
      this.anchor = anchor;
      this.voucher = voucher;
      alpha = new byte[32];
      JLibrustzcash.librustzcashSaplingGenerateR(alpha);
    }

    public SpendDescriptionInfo(
        ExpandedSpendingKey expsk,
        Note note,
        byte[] alpha,
        byte[] anchor,
        IncrementalMerkleVoucherContainer voucher) {
      this.expsk = expsk;
      this.note = note;
      this.anchor = anchor;
      this.voucher = voucher;
      this.alpha = alpha;
    }

    public SpendDescriptionInfo(
        byte[] ak,
        byte[] nsk,
        byte[] ovk,
        Note note,
        byte[] alpha,
        byte[] anchor,
        IncrementalMerkleVoucherContainer voucher) {
      this.ak = ak;
      this.nsk = nsk;
      this.note = note;
      this.anchor = anchor;
      this.voucher = voucher;
      this.alpha = alpha;
      this.ovk = ovk;
    }
  }

  @AllArgsConstructor
  public class ReceiveDescriptionInfo {

    @Getter
    private byte[] ovk;
    @Getter
    private Note note;
  }

}
