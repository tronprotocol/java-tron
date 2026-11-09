package org.tron.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.util.Arrays;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.api.GrpcAPI;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.zen.address.FullViewingKey;
import org.tron.core.zen.address.KeyIo;
import org.tron.core.zen.address.PaymentAddress;
import org.tron.core.zen.address.SpendingKey;
import org.tron.core.zen.note.Note;
import org.tron.core.zen.note.NoteEncryption;
import org.tron.core.zen.note.NoteEncryption.Encryption;
import org.tron.core.zen.note.NoteEncryption.Encryption.EncCiphertext;
import org.tron.core.zen.note.NoteEncryption.Encryption.EncPlaintext;
import org.tron.core.zen.note.OutgoingPlaintext;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.ShieldContract.ReceiveDescription;
import org.tron.protos.contract.ShieldContract.ShieldedTransferContract;

public class WalletShieldedNoteScanTest {

  private static boolean previousAllow;
  private static PaymentAddress address;
  private static byte[] ivk;
  private static byte[] ovk;

  @BeforeClass
  public static void setUpClass() throws Exception {
    previousAllow = Args.getInstance().isAllowShieldedTransactionApi();
    Args.getInstance().setAllowShieldedTransactionApi(true);
    SpendingKey key = new SpendingKey(new byte[32]);
    FullViewingKey viewingKey = key.fullViewingKey();
    address = key.defaultAddress();
    ivk = viewingKey.inViewingKey().getValue();
    ovk = viewingKey.getOvk();
  }

  @AfterClass
  public static void tearDownClass() {
    Args.getInstance().setAllowShieldedTransactionApi(previousAllow);
  }

  private static Note note(long value) throws Exception {
    Note note = new Note(address, value);
    note.setMemo(new byte[]{7, 8, 9});
    return note;
  }

  private static class EncryptedNote {

    private final Note note;
    private final NoteEncryption encryptor;
    private final EncCiphertext ciphertext;
    private final byte[] commitment;
    private final byte[] cout;

    private EncryptedNote(Note note, EncPlaintext plaintext) throws Exception {
      this.note = note;
      commitment = note.cm();
      encryptor = NoteEncryption.fromDiversifier(address.getD()).get();
      ciphertext = encryptor.encryptToRecipient(address.getPkD(), plaintext).get();
      cout = new OutgoingPlaintext(address.getPkD(), encryptor.getEsk())
          .encrypt(ovk, new byte[32], commitment, encryptor).getData();
      // A malformed authenticated plaintext, rather than damaged ciphertext, reaches the bug.
      assertArrayEquals(plaintext.getData(), Encryption.attemptEncDecryption(
          ciphertext.getData(), ivk, encryptor.getEpk()).get().getData());
    }

    private ReceiveDescription description() {
      return ReceiveDescription.newBuilder()
          .setNoteCommitment(ByteString.copyFrom(commitment))
          .setValueCommitment(ByteString.copyFrom(new byte[32]))
          .setEpk(ByteString.copyFrom(encryptor.getEpk()))
          .setCEnc(ByteString.copyFrom(ciphertext.getData()))
          .setCOut(ByteString.copyFrom(cout)).build();
    }

    private TransactionInfo.Log log(byte[] contractAddress, int position) {
      byte[] data = new byte[800];
      System.arraycopy(ByteArray.fromLong(position), 0, data, 24, 8);
      System.arraycopy(commitment, 0, data, 32, 32);
      System.arraycopy(encryptor.getEpk(), 0, data, 96, 32);
      System.arraycopy(ciphertext.getData(), 0, data, 128, 580);
      System.arraycopy(cout, 0, data, 708, 80);
      return TransactionInfo.Log.newBuilder()
          .setAddress(ByteString.copyFrom(Arrays.copyOfRange(contractAddress, 1, 21)))
          .addTopics(ByteString.copyFrom(Hash.sha3(ByteArray.fromString(
              "MintNewLeaf(uint256,bytes32,bytes32,bytes32,bytes32[21])"))))
          .setData(ByteString.copyFrom(data)).build();
    }

    private void assertRejected() throws Exception {
      assertFalse(Note.decrypt(ciphertext.getData(), ivk, encryptor.getEpk(), commitment)
          .isPresent());
      assertFalse(Note.decrypt(ciphertext, encryptor.getEpk(), encryptor.getEsk(),
          address.getPkD(), commitment).isPresent());
    }
  }

  @Test
  public void malformedAuthenticatedPlaintextsAreSkippedByBothDecryptors() throws Exception {
    Note note = note(42);
    for (int header : new int[]{0, 2, 255}) {
      EncPlaintext plaintext = note.encode();
      plaintext.getData()[0] = (byte) header;
      assertThrows(ZksnarkException.class, () -> Note.decode(plaintext));
      new EncryptedNote(note, plaintext).assertRejected();
    }
  }

  @Test
  public void negativeAuthenticatedValuesAreSkippedByBothDecryptors() throws Exception {
    Note note = note(42);
    for (long value : new long[]{Long.MIN_VALUE, -1}) {
      Note invalid = new Note(address.getD(), address.getPkD(), value, note.getRcm());
      assertEquals(value, Note.decode(invalid.encode()).getValue());
      new EncryptedNote(note, invalid.encode()).assertRejected();
    }
  }

  @Test
  public void validNotesAndExistingRejectionsArePreserved() throws Exception {
    for (long value : new long[]{0, 42, Long.MAX_VALUE}) {
      Note note = note(value);
      EncryptedNote encrypted = new EncryptedNote(note, note.encode());
      Note incoming = Note.decrypt(encrypted.ciphertext.getData(), ivk,
          encrypted.encryptor.getEpk(), encrypted.commitment).get();
      Note outgoing = Note.decrypt(encrypted.ciphertext, encrypted.encryptor.getEpk(),
          encrypted.encryptor.getEsk(), address.getPkD(), encrypted.commitment).get();
      for (Note decrypted : new Note[]{incoming, outgoing}) {
        assertEquals(note.getValue(), decrypted.getValue());
        assertArrayEquals(note.getD().getData(), decrypted.getD().getData());
        assertArrayEquals(note.getRcm(), decrypted.getRcm());
        assertArrayEquals(note.getMemo(), decrypted.getMemo());
      }
      assertThrows(ZksnarkException.class, () -> Note.decrypt(encrypted.ciphertext.getData(),
          new byte[31], encrypted.encryptor.getEpk(), encrypted.commitment));
      encrypted.commitment[0] ^= 1;
      encrypted.assertRejected();
      encrypted.commitment[0] ^= 1;
      encrypted.ciphertext.getData()[0] ^= 1;
      encrypted.assertRejected();
    }
  }

  private static EncryptedNote[] mixedNotes() throws Exception {
    Note valid = note(42);
    EncPlaintext invalidHeader = valid.encode();
    invalidHeader.getData()[0] = 2;
    Note invalidValue = new Note(address.getD(), address.getPkD(), -1, valid.getRcm());
    Note last = note(84);
    return new EncryptedNote[]{new EncryptedNote(valid, valid.encode()),
        new EncryptedNote(valid, invalidHeader), new EncryptedNote(valid, invalidValue.encode()),
        new EncryptedNote(last, last.encode())};
  }

  private static Wallet scanner(Transaction transaction) {
    Wallet scanner = spy(new Wallet());
    GrpcAPI.BlockList blocks = GrpcAPI.BlockList.newBuilder()
        .addBlock(Block.newBuilder().addTransactions(transaction)).build();
    doReturn(blocks).when(scanner).getBlocksByLimitNext(0, 1);
    return scanner;
  }

  @Test
  public void trc20ScansReturnValidNotesOnBothSidesOfMalformedNotes() throws Exception {
    byte[] contractAddress = new byte[21];
    contractAddress[0] = Wallet.getAddressPreFixByte();
    contractAddress[20] = 1;
    EncryptedNote[] notes = mixedNotes();
    Transaction transaction = Transaction.getDefaultInstance();
    ByteString txid = new TransactionCapsule(transaction).getTransactionId().getByteString();
    Wallet scanner = scanner(transaction);
    TransactionInfo.Builder info = TransactionInfo.newBuilder();
    for (int i = 0; i < notes.length; i++) {
      info.addLog(notes[i].log(contractAddress, i));
    }
    doReturn(info.build()).when(scanner).getTransactionInfoById(txid);

    GrpcAPI.DecryptNotesTRC20 incoming = scanner.scanShieldedTRC20NotesByIvk(
        0, 1, contractAddress, ivk, null, null);
    GrpcAPI.DecryptNotesTRC20 outgoing = scanner.scanShieldedTRC20NotesByOvk(
        0, 1, ovk, contractAddress);
    for (GrpcAPI.DecryptNotesTRC20 result : new GrpcAPI.DecryptNotesTRC20[]{incoming, outgoing}) {
      assertEquals(2, result.getNoteTxsCount());
      for (int i = 0; i < 2; i++) {
        int index = i * 3;
        GrpcAPI.DecryptNotesTRC20.NoteTx actual = result.getNoteTxs(i);
        assertEquals(txid, actual.getTxid());
        assertEquals(index, actual.getIndex());
        assertEquals(notes[index].note.getValue(), actual.getNote().getValue());
        assertEquals(ByteString.copyFrom(notes[index].note.getRcm()), actual.getNote().getRcm());
        assertEquals(ByteString.copyFrom(new byte[]{7, 8, 9}), actual.getNote().getMemo());
        assertEquals(KeyIo.encodePaymentAddress(address), actual.getNote().getPaymentAddress());
      }
    }
    assertEquals(0, incoming.getNoteTxs(0).getPosition());
    assertEquals(3, incoming.getNoteTxs(1).getPosition());
  }

  @Test
  public void nativeScansReturnValidNotesOnBothSidesOfMalformedNotes() throws Exception {
    EncryptedNote[] notes = mixedNotes();
    ShieldedTransferContract.Builder shielded = ShieldedTransferContract.newBuilder();
    for (EncryptedNote note : notes) {
      shielded.addReceiveDescription(note.description());
    }
    Transaction transaction = Transaction.newBuilder()
        .setRawData(Transaction.raw.newBuilder().addContract(Contract.newBuilder()
            .setType(Contract.ContractType.ShieldedTransferContract)
            .setParameter(Any.pack(shielded.build())))).build();
    Wallet scanner = scanner(transaction);
    GrpcAPI.DecryptNotes incoming = scanner.scanNoteByIvk(0, 1, ivk);
    GrpcAPI.DecryptNotes outgoing = scanner.scanNoteByOvk(0, 1, ovk);
    for (GrpcAPI.DecryptNotes result : new GrpcAPI.DecryptNotes[]{incoming, outgoing}) {
      assertEquals(2, result.getNoteTxsCount());
      assertEquals(0, result.getNoteTxs(0).getIndex());
      assertEquals(3, result.getNoteTxs(1).getIndex());
      assertEquals(42, result.getNoteTxs(0).getNote().getValue());
      assertEquals(84, result.getNoteTxs(1).getNote().getValue());
      assertEquals(ByteString.copyFrom(notes[0].note.getRcm()),
          result.getNoteTxs(0).getNote().getRcm());
      assertEquals(ByteString.copyFrom(notes[3].note.getRcm()),
          result.getNoteTxs(1).getNote().getRcm());
    }
  }
}
