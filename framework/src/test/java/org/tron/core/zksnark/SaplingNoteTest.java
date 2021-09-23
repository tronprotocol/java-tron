package org.tron.core.zksnark;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testng.Assert;
import org.tron.common.utils.ByteArray;
import org.tron.core.config.args.Args;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.services.http.FullNodeHttpApiService;
import org.tron.core.zen.address.DiversifierT;
import org.tron.core.zen.address.PaymentAddress;
import org.tron.core.zen.address.SpendingKey;
import org.tron.core.zen.note.Note;


public class SaplingNoteTest {

  @BeforeClass
  public static void init() {
    Args.setFullNodeAllowShieldedTransaction(true);
    // Args.getInstance().setAllowShieldedTransaction(1);
    FullNodeHttpApiService.librustzcashInitZksnarkParams();
  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
  }

  private static int randomInt(int minInt, int maxInt) {
    return (int) Math.round(Math.random() * (maxInt - minInt) + minInt);
  }

  @Test
  public void testVectors() throws ZksnarkException {

    long v = 0;
    long position = 0;
    byte[] d = {(byte) 0xf1, (byte) 0x9d, (byte) 0x9b, 0x79, 0x7e, 0x39, (byte) 0xf3, 0x37, 0x44,
        0x58, 0x39};
    byte[] sk = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00};
    byte[] pkD = {(byte) 0xdb, 0x4c, (byte) 0xd2, (byte) 0xb0, (byte) 0xaa, (byte) 0xc4,
        (byte) 0xf7, (byte) 0xeb, (byte) 0x8c, (byte) 0xa1, 0x31, (byte) 0xf1, 0x65, 0x67,
        (byte) 0xc4, 0x45, (byte) 0xa9, 0x55, 0x51, 0x26, (byte) 0xd3, (byte) 0xc2, (byte) 0x9f,
        0x14, (byte) 0xe3, (byte) 0xd7, 0x76, (byte) 0xe8, 0x41, (byte) 0xae, 0x74, 0x15};
    byte[] r = {0x39, 0x17, 0x6d, (byte) 0xac, 0x39, (byte) 0xac, (byte) 0xe4, (byte) 0x98, 0x0e,
        (byte) 0xcc, (byte) 0x8d, 0x77, (byte) 0x8e, (byte) 0x89, (byte) 0x86, 0x02, 0x55,
        (byte) 0xec, 0x36, 0x15, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00};
    byte[] cm = {(byte) 0xcb, 0x3c, (byte) 0xf9, 0x15, 0x32, 0x70, (byte) 0xd5, 0x7e, (byte) 0xb9,
        0x14, (byte) 0xc6, (byte) 0xc2, (byte) 0xbc, (byte) 0xc0, 0x18, 0x50, (byte) 0xc9,
        (byte) 0xfe, (byte) 0xd4, 0x4f, (byte) 0xce, 0x08, 0x06, 0x27, (byte) 0x8f, 0x08, 0x3e,
        (byte) 0xf2, (byte) 0xdd, 0x07, 0x64, 0x39};
    String nf = "b7560da1a2913fb2d873bb3c596a891a4e2e3abd83a852bcdfc9d410e7834117";

    // test commit
    Note note = new Note(new DiversifierT(d), pkD, v, r);
    Assert.assertEquals(ByteArray.toHexString(note.cm()), ByteArray.toHexString(cm));

    // test nullifier
    SpendingKey spendingKey = new SpendingKey(sk);
    Assert
        .assertEquals(ByteArray.toHexString(note.nullifier(spendingKey.fullViewingKey(), position)),
            nf);

  }


  @Test
  public void testRandom() throws BadItemException, ZksnarkException {

    SpendingKey spendingKey = SpendingKey.random();
    PaymentAddress address = spendingKey.defaultAddress();

    Note note1 = new Note(address, randomInt(0, 99999));
    Note note2 = new Note(address, randomInt(0, 99999));

    Assert.assertEquals(ByteArray.toHexString(note1.getD().getData()),
        ByteArray.toHexString(note2.getD().getData()));
    Assert
        .assertEquals(ByteArray.toHexString(note1.getPkD()), ByteArray.toHexString(note2.getPkD()));
    Assert.assertNotEquals(note1.getValue(), note2.getValue());
    Assert.assertNotEquals(ByteArray.toHexString(note1.getRcm()),
        ByteArray.toHexString(note2.getRcm()));

    // Test diversifier and pkD are not the same for different spending keys
    Note note3 = new Note(SpendingKey.random().defaultAddress(), randomInt(0, 99999));
    Assert.assertNotEquals(ByteArray.toHexString(note1.getD().getData()),
        ByteArray.toHexString(note3.getD().getData()));
    Assert.assertNotEquals(ByteArray.toHexString(note1.getPkD()),
        ByteArray.toHexString(note3.getPkD()));


  }

}
