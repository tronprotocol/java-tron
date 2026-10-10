package org.tron.common.utils.client.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.crypto.Hash;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.Sha256Hash;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.BalanceContract.TransferContract;

public class TransactionUtilsTest {

  private String originalCryptoEngine;
  private Transaction signed;

  @Before
  public void setUp() {
    originalCryptoEngine = CommonParameter.getInstance().getCryptoEngine();
    CommonParameter.getInstance().setCryptoEngine("ECKey");
    ECKey key = ECKey.fromPrivate(BigInteger.TEN);
    signed = TransactionUtils.sign(createTransaction(key.getAddress()), key);
  }

  @After
  public void tearDown() {
    CommonParameter.getInstance().setCryptoEngine(originalCryptoEngine);
  }

  @Test
  public void testValidSignature() {
    assertTrue(TransactionUtils.validTransaction(signed));
  }

  @Test
  public void testShortSignatures() {
    for (int length : new int[]{0, 64}) {
      Transaction transaction = signed.toBuilder()
          .setSignature(0, ByteString.copyFrom(new byte[length])).build();
      assertFalse(TransactionUtils.validTransaction(transaction));
    }
  }

  @Test
  public void testPaddedSignatures() {
    for (int length : new int[]{66, 68, 69}) {
      byte[] signature = Arrays.copyOf(signed.getSignature(0).toByteArray(), length);
      Transaction transaction = signed.toBuilder()
          .setSignature(0, ByteString.copyFrom(signature)).build();
      assertFalse(TransactionUtils.validTransaction(transaction));
    }
  }

  @Test
  public void testInvalidScalars() {
    BigInteger order = ECKey.CURVE.getN();
    for (BigInteger scalar : Arrays.asList(BigInteger.ZERO, order, order.add(BigInteger.ONE))) {
      for (ECDSASignature signature : Arrays.asList(
          new ECDSASignature(scalar, BigInteger.ONE),
          new ECDSASignature(BigInteger.ONE, scalar))) {
        signature.v = 27;
        Transaction transaction = signed.toBuilder()
            .setSignature(0, ByteString.copyFrom(signature.toByteArray())).build();
        assertFalse(TransactionUtils.validTransaction(transaction));
      }
    }
  }

  @Test
  public void testPointAtInfinity() throws SignatureException {
    byte[] owner = Hash.computeAddress(new byte[]{0});
    Transaction transaction = createTransaction(owner);
    byte[] hash = Sha256Hash.hash(true, transaction.getRawData().toByteArray());
    ECDSASignature signature = new ECDSASignature(
        ECKey.CURVE.getG().getAffineXCoord().toBigInteger(),
        new BigInteger(1, hash).mod(ECKey.CURVE.getN()));
    signature.v = 27;
    assertArrayEquals(owner, ECKey.signatureToAddress(hash, signature, false));

    Transaction signedTransaction = transaction.toBuilder()
        .addSignature(ByteString.copyFrom(signature.toByteArray())).build();
    assertFalse(TransactionUtils.validTransaction(signedTransaction));
  }

  private Transaction createTransaction(byte[] owner) {
    TransferContract transfer = TransferContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(owner))
        .setToAddress(ByteString.copyFrom(owner)).setAmount(1).build();
    Contract contract = Contract.newBuilder().setType(ContractType.TransferContract)
        .setParameter(Any.pack(transfer)).build();
    return Transaction.newBuilder()
        .setRawData(Transaction.raw.newBuilder().addContract(contract)).build();
  }
}
