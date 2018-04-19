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
package org.tron.program.cat;

import static org.tron.common.crypto.Hash.sha256;

import com.google.protobuf.ByteString;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Time;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.AccountStore;
import org.tron.core.db.Manager;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;

public class ExportBlockData {
  private static final long COUNT = 10000;
  private static long currntNumber = 1;
  private static String currntParentHash = "cafabcd5e02545199ba49be971bf427b6728369a5e7fa956eba89ae761f97792";

  private static final String TEST_CAT = "cat/config-cat.conf";

  private static Manager dbManager = new Manager();
  private static String dbPath = "output_cat_push_block";

  private static ECKey myKey;

  public static void main(String[] args) throws IOException {
    init();

    File f = new File("blocks.txt");
    FileOutputStream fos = new FileOutputStream(f);

    for(int i = 0; i < COUNT; ++i) {
      BlockCapsule blockCapsule = createBlockCapsule();

      currntNumber += 1;
      currntParentHash = blockCapsule.getBlockId().toString();

      System.out.println("Count: " + i);
      blockCapsule.getInstance().writeDelimitedTo(fos);
    }

    fos.close();
    f.exists();
  }

  private static void init() {
    Args.setParam(new String[]{"-d", dbPath, "-w"},
        TEST_CAT);
    dbManager.init();
    myKey = ECKey.fromPrivate(ByteArray.fromHexString(Args.getInstance().getLocalWitnesses().getPrivateKeys().get(0)));
  }

  private static BlockCapsule createBlockCapsule() {
    BlockCapsule blockCapsule = new BlockCapsule(currntNumber, ByteString.copyFrom(ByteArray
        .fromHexString(currntParentHash)),
        Time.getCurrentMillis(),
        ByteString.copyFrom(
            ECKey.fromPrivate(ByteArray
                .fromHexString(Args.getInstance().getLocalWitnesses().getPrivateKey()))
                .getAddress()));

    int trxCount = Utils.getRandom().nextInt(100);
    for (int i = 0; i < trxCount; ++i) {
      byte[] owner = myKey.getAddress();
      TransferContract contract = createTransferContract(ByteArray.fromHexString(
          Wallet.getAddressPreFixString() + "ED738B3A0FE390EAA71B768B6D02CDBD18FB207B"), owner, 1L);
      Transaction transaction = createTransaction(contract);
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        return null;
      }

      transaction = signTransaction(transaction);

      blockCapsule.addTransaction(new TransactionCapsule(transaction));
    }

    blockCapsule.setMerkleRoot();
    blockCapsule.sign(
        ByteArray.fromHexString(Args.getInstance().getLocalWitnesses().getPrivateKey()));

    return blockCapsule;
  }

  private static Transaction createTransaction(TransferContract contract) {
    ByteString fromBs = contract.getOwnerAddress();
    ByteString toBs = contract.getToAddress();
    long amount = contract.getAmount();
    if (fromBs != null && toBs != null && amount > 0) {
      AccountStore accountStore = dbManager.getAccountStore();
      Transaction trx = new TransactionCapsule(contract, accountStore).getInstance();
      return trx;
    }

    return null;
  }

  private static TransferContract createTransferContract(byte[] to, byte[] owner,
      long amount) {
    TransferContract.Builder builder = TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    return builder.build();
  }

  private static Transaction signTransaction(Transaction transaction) {
    if (myKey == null || myKey.getPrivKey() == null) {
      return null;
    }
    transaction = setTimestamp(transaction);
    return sign(transaction, myKey);
  }

  private static Transaction sign(Transaction transaction, ECKey myKey) {
    Transaction.Builder transactionBuilderSigned = transaction.toBuilder();

    if (transaction.getRawData().getType() == Transaction.TransactionType.ContractType) {
      byte[] hash = sha256(transaction.getRawData().toByteArray());
      List<Contract> listContract = transaction.getRawData().getContractList();
      for (int i = 0; i < listContract.size(); i++) {
        ECDSASignature signature = myKey.sign(hash);
        ByteString bsSign = ByteString.copyFrom(signature.toByteArray());
        transactionBuilderSigned.addSignature(bsSign);//Each contract may be signed with a different private key in the future.
      }
    }

    transaction = transactionBuilderSigned.build();
    return transaction;
  }

  private static Transaction setTimestamp(Transaction transaction){
    long currenTime = System.nanoTime();
    Transaction.Builder builder = transaction.toBuilder();
    org.tron.protos.Protocol.Transaction.raw.Builder rowBuilder = transaction.getRawData()
        .toBuilder();
    rowBuilder.setTimestamp(currenTime);
    builder.setRawData(rowBuilder.build());
    return builder.build();
  }
}
