/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core.capsule;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.utils.TxInputUtil;
import org.tron.core.capsule.utils.TxOutputUtil;
import org.tron.core.db.AccountStore;
import org.tron.core.db.UtxoStore;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.protos.Contract;
import org.tron.protos.Contract.AccountCreateContract;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Protocol.TXInput;
import org.tron.protos.Protocol.TXOutput;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.TransactionType;

public class TransactionCapsule implements ProtoCapsule<Transaction> {

  private static final Logger logger = LoggerFactory.getLogger("Transaction");

  private Transaction transaction;

  /**
   * constructor TransactionCapsule.
   */
  public TransactionCapsule(Transaction trx) {
    this.transaction = trx;
  }

  /**
   * get account from bytes data.
   */
  public TransactionCapsule(byte[] data) {
    try {
      this.transaction = Transaction.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage());
    }
  }

  public TransactionCapsule(String key, long value) {
    TXInput.raw rawData = TXInput.raw.newBuilder()
        .setTxID(ByteString.copyFrom(new byte[]{}))
        .setVout(-1).build();

    TXInput txi = TXInput.newBuilder()
        .setSignature(ByteString.copyFrom(new byte[]{}))
        .setRawData(rawData).build();

    TXOutput txo = TXOutput.newBuilder()
        .setValue(value)
        .setPubKeyHash(ByteString.copyFrom(ByteArray.fromHexString(key)))
        .build();

    Transaction.raw.Builder rawCoinbaseTransaction = Transaction.raw.newBuilder()
        .addVin(txi)
        .addVout(txo);
    this.transaction = Transaction.newBuilder().setRawData(rawCoinbaseTransaction.build()).build();

  }

  /**
   * constructor TransactionCapsule.
   */
  public TransactionCapsule(
      byte[] address,
      String to,
      long amount,
      long balance,
      UtxoStore utxoStore
  ) {

    Transaction.raw.Builder transactionBuilder = Transaction.raw.newBuilder().addContract(
        Transaction.Contract.newBuilder().setType(ContractType.TransferContract).build());
    List<TXInput> txInputs = new ArrayList<>();
    List<TXOutput> txOutputs = new ArrayList<>();
    long spendableOutputs = balance;

    utxoStore.findSpendableOutputs(address, amount).getUnspentOutputs()
      .forEach((txId, outs) ->
        Arrays.stream(outs)
          .mapToObj(out -> TxInputUtil.newTxInput(ByteArray.fromHexString(txId), out, null, address))
          .forEachOrdered(txInputs::add));

    txOutputs.add(TxOutputUtil.newTxOutput(amount, to));
    txOutputs
        .add(TxOutputUtil.newTxOutput(spendableOutputs - amount, ByteArray.toHexString(address)));

    if (checkBalance(address, to, amount, balance)) {
      txInputs.forEach(transactionBuilder::addVin);
      txOutputs.forEach(transactionBuilder::addVout);
      logger.info("Transaction create succeeded！");
      transaction = Transaction.newBuilder().setRawData(transactionBuilder.build()).build();
    } else {
      logger.error("Transaction create failed！");
      transaction = null;
    }
  }

  public TransactionCapsule(AccountCreateContract contract, AccountStore accountStore) {
    AccountCapsule account = accountStore.get(contract.getOwnerAddress().toByteArray());
    if (account != null && account.getType() == contract.getType()) {
      return; // Account isexit
    }

    Transaction.raw.Builder transactionBuilder = Transaction.raw.newBuilder().setType(
        TransactionType.ContractType).addContract(
        Transaction.Contract.newBuilder().setType(ContractType.AccountCreateContract).setParameter(
            Any.pack(contract)).build());
    logger.info("Transaction create succeeded！");
    transaction = Transaction.newBuilder().setRawData(transactionBuilder.build()).build();
  }

  public TransactionCapsule(TransferContract contract, AccountStore accountStore) {
    Transaction.Contract.Builder contractBuilder = Transaction.Contract.newBuilder();

    AccountCapsule owner = accountStore.get(contract.getOwnerAddress().toByteArray());
    if (owner == null || owner.getBalance() < contract.getAmount()) {
      return; //The balance is not enough
    }

    AccountCapsule to = accountStore.get(contract.getToAddress().toByteArray());

    if (to == null) {
      return; //to is invalid
    }

    Transaction.raw.Builder transactionBuilder = Transaction.raw.newBuilder().setType(
        TransactionType.ContractType).addContract(
        Transaction.Contract.newBuilder().setType(ContractType.TransferContract).setParameter(
            Any.pack(contract)).build());
    logger.info("Transaction create succeeded！");
    transaction = Transaction.newBuilder().setRawData(transactionBuilder.build()).build();
  }

  public TransactionCapsule(Contract.VoteWitnessContract voteWitnessContract) {

    Transaction.raw.Builder transactionBuilder = Transaction.raw.newBuilder().setType(
        TransactionType.ContractType).addContract(
        Transaction.Contract.newBuilder().setType(ContractType.VoteWitnessContract).setParameter(
            Any.pack(voteWitnessContract)).build());
    logger.info("Transaction create succeeded！");
    transaction = Transaction.newBuilder().setRawData(transactionBuilder.build()).build();

  }

  public TransactionCapsule(Contract.WitnessCreateContract witnessCreateContract) {

    Transaction.raw.Builder transactionBuilder = Transaction.raw.newBuilder().setType(
        TransactionType.ContractType).addContract(
        Transaction.Contract.newBuilder().setType(ContractType.WitnessCreateContract).setParameter(
            Any.pack(witnessCreateContract)).build());
    logger.info("Transaction create succeeded！");
    transaction = Transaction.newBuilder().setRawData(transactionBuilder.build()).build();
  }

  public TransactionCapsule(Contract.WitnessUpdateContract witnessUpdateContract) {

    Transaction.raw.Builder transactionBuilder = Transaction.raw.newBuilder().setType(
            TransactionType.ContractType).addContract(
            Transaction.Contract.newBuilder().setType(ContractType.WitnessUpdateContract).setParameter(
                    Any.pack(witnessUpdateContract)).build());
    logger.info("Transaction create succeeded！");
    transaction = Transaction.newBuilder().setRawData(transactionBuilder.build()).build();
  }

  public void setResult(TransactionResultCapsule transactionResultCapsule) {
    //this.getInstance().toBuilder(). (transactionResultCapsule.getInstance());
  }

  public TransactionCapsule(Contract.AssetIssueContract assetIssueContract) {

    Transaction.raw.Builder transactionBuilder = Transaction.raw.newBuilder().setType(
        TransactionType.ContractType).addContract(
        Transaction.Contract.newBuilder().setType(ContractType.AssetIssueContract).setParameter(
            Any.pack(assetIssueContract)).build());
    logger.info("Transaction create succeeded！");
    transaction = Transaction.newBuilder().setRawData(transactionBuilder.build()).build();
  }

  public Sha256Hash getHash() {
    byte[] transBytes = this.transaction.toByteArray();
    return Sha256Hash.of(transBytes);
  }

  public Sha256Hash getRawHash() {
    return Sha256Hash.of(this.transaction.getRawData().toByteArray());
  }

  /**
   * cheack balance of the address.
   */
  public boolean checkBalance(byte[] address, String to, long amount, long balance) {

    if (to.length() != 40) {
      logger.error("address invalid");
      return false;
    }

    if (amount <= 0) {
      logger.error("amount required a positive number");
      return false;
    }

    if (amount > balance) {
      logger.error("don't have enough money");
      return false;
    }

    return true;
  }

  public void sign(byte[] privateKey) {
    ECKey ecKey = ECKey.fromPrivate(privateKey);
    ECDSASignature signature = ecKey.sign(getRawHash().getBytes());
    ByteString sig = ByteString.copyFrom(signature.toBase64().getBytes());
    this.transaction = this.transaction.toBuilder().addSignature(sig).build();
  }


  public static byte[] getOwner(Transaction.Contract contract) {
    ByteString owner;
    try {
      switch (contract.getType()) {
        case AccountCreateContract:
          owner = contract.getParameter()
              .unpack(org.tron.protos.Contract.AccountCreateContract.class).getOwnerAddress();
          break;
        case TransferContract:
          owner = contract.getParameter().unpack(org.tron.protos.Contract.TransferContract.class)
              .getOwnerAddress();
          break;
        case TransferAssertContract:
          owner = contract.getParameter()
              .unpack(org.tron.protos.Contract.TransferAssertContract.class).getOwnerAddress();
          break;
        case VoteAssetContract:
          owner = contract.getParameter().unpack(org.tron.protos.Contract.VoteAssetContract.class)
              .getOwnerAddress();
          break;
        case VoteWitnessContract:
          owner = contract.getParameter().unpack(org.tron.protos.Contract.VoteWitnessContract.class)
              .getOwnerAddress();
          break;
        case WitnessCreateContract:
          owner = contract.getParameter()
              .unpack(org.tron.protos.Contract.WitnessCreateContract.class).getOwnerAddress();
          break;
        case AssetIssueContract:
          owner = contract.getParameter().unpack(org.tron.protos.Contract.AssetIssueContract.class)
              .getOwnerAddress();
          break;
        case DeployContract:
          owner = contract.getParameter().unpack(org.tron.protos.Contract.AssetIssueContract.class)
              .getOwnerAddress();
          break;
        default:
          return null;
      }
      return owner.toByteArray();
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  public static String getBase64FromByteString(ByteString sign) {
    byte[] r = sign.substring(0, 32).toByteArray();
    byte[] s = sign.substring(32, 64).toByteArray();
    byte v = sign.byteAt(64);
    if (v < 27) {
      v += 27; //revId -> v
    }
    ECDSASignature signature = ECDSASignature.fromComponents(r, s, v);
    return signature.toBase64();
  }


  /**
   * validate signature
   */
  public boolean validateSignature() throws ValidateSignatureException {
    if (this.getInstance().getSignatureCount() !=
        this.getInstance().getRawData().getContractCount()) {
      throw new ValidateSignatureException("miss sig or contract");
    }

    List<Transaction.Contract> listContract = this.transaction.getRawData().getContractList();
    for (int i = 0; i < this.transaction.getSignatureCount(); ++i) {
      try {
        Transaction.Contract contract = listContract.get(i);
        byte[] owner = getOwner(contract);
        byte[] address = ECKey.signatureToAddress(getRawHash().getBytes(),
            getBase64FromByteString(this.transaction.getSignature(i)));
        if (!Arrays.equals(owner, address)) {
          throw new ValidateSignatureException("sig error");
        }
      } catch (SignatureException e) {
        throw new ValidateSignatureException(e.getMessage());
      }
    }
    return true;
  }

  public Sha256Hash getTransactionId() {
    return Sha256Hash.of(this.transaction.toByteArray());
  }

  @Override
  public byte[] getData() {
    return this.transaction.toByteArray();
  }

  @Override
  public Transaction getInstance() {
    return this.transaction;
  }

  @Override
  public String toString() {
    return this.transaction.toString();
  }
}
