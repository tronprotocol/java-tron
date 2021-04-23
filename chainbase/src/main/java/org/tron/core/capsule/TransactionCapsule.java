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

import static org.tron.common.utils.StringUtil.encode58Check;
import static org.tron.common.utils.WalletUtil.checkPermissionOperations;
import static org.tron.core.exception.P2pException.TypeEnum.PROTOBUF_ERROR;

import com.google.common.primitives.Bytes;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Internal;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.IOException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.crypto.SignInterface;
import org.tron.common.crypto.SignUtils;
import org.tron.common.overlay.message.Message;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.actuator.TransactionFactory;
import org.tron.core.db.TransactionContext;
import org.tron.core.db.TransactionTrace;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.PermissionException;
import org.tron.core.exception.SignatureFormatException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.Key;
import org.tron.protos.Protocol.Permission;
import org.tron.protos.Protocol.Permission.PermissionType;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.Protocol.Transaction.raw;
import org.tron.protos.contract.AccountContract.AccountCreateContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.ParticipateAssetIssueContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.TransferAssetContract;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.BalanceContract.TransferContract;
import org.tron.protos.contract.ShieldContract.ShieldedTransferContract;
import org.tron.protos.contract.ShieldContract.SpendDescription;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;
import org.tron.protos.contract.WitnessContract.VoteWitnessContract;
import org.tron.protos.contract.WitnessContract.WitnessCreateContract;
import org.tron.protos.contract.WitnessContract.WitnessUpdateContract;

@Slf4j(topic = "capsule")
public class TransactionCapsule implements ProtoCapsule<Transaction> {

  private static final ExecutorService executorService = Executors
      .newFixedThreadPool(CommonParameter.getInstance()
          .getValidContractProtoThreadNum());
  private static final String OWNER_ADDRESS = "ownerAddress_";

  private Transaction transaction;
  @Setter
  private boolean isVerified = false;
  @Setter
  @Getter
  private long blockNum = -1;
  @Getter
  @Setter
  private TransactionTrace trxTrace;

  private StringBuilder toStringBuff = new StringBuilder();
  @Getter
  @Setter
  private long time;
  @Getter
  @Setter
  private long order;

  /**
   * constructor TransactionCapsule.
   */
  public TransactionCapsule(Transaction trx) {
    this.transaction = trx;
  }

  /**
   * get account from bytes data.
   */
  public TransactionCapsule(byte[] data) throws BadItemException {
    try {
      this.transaction = Transaction.parseFrom(Message.getCodedInputStream(data));
    } catch (Exception e) {
      throw new BadItemException("Transaction proto data parse exception");
    }
  }

  public TransactionCapsule(CodedInputStream codedInputStream) throws BadItemException {
    try {
      this.transaction = Transaction.parseFrom(codedInputStream);
    } catch (IOException e) {
      throw new BadItemException("Transaction proto data parse exception");
    }
  }

  public TransactionCapsule(AccountCreateContract contract, AccountStore accountStore) {
    AccountCapsule account = accountStore.get(contract.getOwnerAddress().toByteArray());
    if (account != null && account.getType() == contract.getType()) {
      return; // Account isexit
    }

    createTransaction(contract, ContractType.AccountCreateContract);
  }

  public TransactionCapsule(TransferContract contract, AccountStore accountStore) {

    AccountCapsule owner = accountStore.get(contract.getOwnerAddress().toByteArray());
    if (owner == null || owner.getBalance() < contract.getAmount()) {
      return; //The balance is not enough
    }

    createTransaction(contract, ContractType.TransferContract);
  }

  public TransactionCapsule(VoteWitnessContract voteWitnessContract) {
    createTransaction(voteWitnessContract, ContractType.VoteWitnessContract);
  }

  public TransactionCapsule(WitnessCreateContract witnessCreateContract) {
    createTransaction(witnessCreateContract, ContractType.WitnessCreateContract);
  }

  public TransactionCapsule(WitnessUpdateContract witnessUpdateContract) {
    createTransaction(witnessUpdateContract, ContractType.WitnessUpdateContract);
  }

  public TransactionCapsule(TransferAssetContract transferAssetContract) {
    createTransaction(transferAssetContract, ContractType.TransferAssetContract);
  }

  public TransactionCapsule(ParticipateAssetIssueContract participateAssetIssueContract) {
    createTransaction(participateAssetIssueContract, ContractType.ParticipateAssetIssueContract);
  }

  public TransactionCapsule(raw rawData, List<ByteString> signatureList) {
    this.transaction = Transaction.newBuilder().setRawData(rawData).addAllSignature(signatureList)
        .build();
  }

  @Deprecated
  public TransactionCapsule(AssetIssueContract assetIssueContract) {
    createTransaction(assetIssueContract, ContractType.AssetIssueContract);
  }

  public TransactionCapsule(com.google.protobuf.Message message, ContractType contractType) {
    Transaction.raw.Builder transactionBuilder = Transaction.raw.newBuilder().addContract(
        Transaction.Contract.newBuilder().setType(contractType).setParameter(
            (message instanceof Any ? (Any) message : Any.pack(message))).build());
    transaction = Transaction.newBuilder().setRawData(transactionBuilder.build()).build();
  }

  public static long getWeight(Permission permission, byte[] address) {
    List<Key> list = permission.getKeysList();
    for (Key key : list) {
      if (key.getAddress().equals(ByteString.copyFrom(address))) {
        return key.getWeight();
      }
    }
    return 0;
  }

  public static long checkWeight(Permission permission, List<ByteString> sigs, byte[] hash,
      List<ByteString> approveList)
      throws SignatureException, PermissionException, SignatureFormatException {
    long currentWeight = 0;
    if (sigs.size() > permission.getKeysCount()) {
      throw new PermissionException(
          "Signature count is " + (sigs.size()) + " more than key counts of permission : "
              + permission.getKeysCount());
    }
    HashMap addMap = new HashMap();
    for (ByteString sig : sigs) {
      if (sig.size() < 65) {
        throw new SignatureFormatException(
            "Signature size is " + sig.size());
      }
      String base64 = TransactionCapsule.getBase64FromByteString(sig);
      byte[] address = SignUtils
          .signatureToAddress(hash, base64, CommonParameter.getInstance().isECKeyCryptoEngine());
      long weight = getWeight(permission, address);
      if (weight == 0) {
        throw new PermissionException(
            ByteArray.toHexString(sig.toByteArray()) + " is signed by " + encode58Check(address)
                + " but it is not contained of permission.");
      }
      if (addMap.containsKey(base64)) {
        throw new PermissionException(encode58Check(address) + " has signed twice!");
      }
      addMap.put(base64, weight);
      if (approveList != null) {
        approveList.add(ByteString.copyFrom(address)); //out put approve list.
      }
      currentWeight += weight;
    }
    return currentWeight;
  }

  //make sure that contractType is validated before
  //No exception will be thrown here
  public static byte[] getShieldTransactionHashIgnoreTypeException(Transaction tx) {
    try {
      return hashShieldTransaction(tx, CommonParameter.getInstance()
          .getZenTokenId());
    } catch (ContractValidateException | InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
    return null;
  }

  public static byte[] hashShieldTransaction(Transaction tx, String tokenId)
      throws ContractValidateException, InvalidProtocolBufferException {
    Any contractParameter = tx.getRawData().getContract(0).getParameter();
    if (!contractParameter.is(ShieldedTransferContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [ShieldedTransferContract],real type["
              + contractParameter
              .getClass() + "]");
    }

    ShieldedTransferContract shieldedTransferContract = contractParameter
        .unpack(ShieldedTransferContract.class);
    ShieldedTransferContract.Builder newContract = ShieldedTransferContract.newBuilder();
    newContract.setFromAmount(shieldedTransferContract.getFromAmount());
    newContract.addAllReceiveDescription(shieldedTransferContract.getReceiveDescriptionList());
    newContract.setToAmount(shieldedTransferContract.getToAmount());
    newContract.setTransparentFromAddress(shieldedTransferContract.getTransparentFromAddress());
    newContract.setTransparentToAddress(shieldedTransferContract.getTransparentToAddress());
    for (SpendDescription spendDescription : shieldedTransferContract.getSpendDescriptionList()) {
      newContract
          .addSpendDescription(spendDescription.toBuilder().clearSpendAuthoritySignature().build());
    }

    Transaction.raw.Builder rawBuilder = tx.toBuilder()
        .getRawDataBuilder()
        .clearContract()
        .addContract(
            Transaction.Contract.newBuilder().setType(ContractType.ShieldedTransferContract)
                .setParameter(
                    Any.pack(newContract.build())).build());

    Transaction transaction = tx.toBuilder().clearRawData()
        .setRawData(rawBuilder).build();

    byte[] mergedByte = Bytes.concat(Sha256Hash
            .of(CommonParameter.getInstance().isECKeyCryptoEngine(), tokenId.getBytes()).getBytes(),
        transaction.getRawData().toByteArray());
    return Sha256Hash.of(CommonParameter
        .getInstance().isECKeyCryptoEngine(), mergedByte).getBytes();
  }

  // todo mv this static function to capsule util
  public static byte[] getOwner(Transaction.Contract contract) {
    ByteString owner;
    try {
      Any contractParameter = contract.getParameter();
      switch (contract.getType()) {
        case ShieldedTransferContract: {
          ShieldedTransferContract shieldedTransferContract = contractParameter
              .unpack(ShieldedTransferContract.class);
          if (!shieldedTransferContract.getTransparentFromAddress().isEmpty()) {
            owner = shieldedTransferContract.getTransparentFromAddress();
          } else {
            return new byte[0];
          }
          break;
        }
        // todo add other contract
        default: {
          Class<? extends GeneratedMessageV3> clazz = TransactionFactory
              .getContract(contract.getType());
          if (clazz == null) {
            logger.error("not exist {}", contract.getType());
            return new byte[0];
          }
          GeneratedMessageV3 generatedMessageV3 = contractParameter.unpack(clazz);
          owner = ReflectUtils.getFieldValue(generatedMessageV3, OWNER_ADDRESS);
          if (owner == null) {
            logger.error("not exist [{}] field,{}", OWNER_ADDRESS, clazz);
            return new byte[0];
          }
          break;
        }
      }
      return owner.toByteArray();
    } catch (Exception ex) {
      logger.error(ex.getMessage());
      return new byte[0];
    }
  }

  public static <T extends com.google.protobuf.Message> T parse(Class<T> clazz,
      CodedInputStream codedInputStream) throws InvalidProtocolBufferException {
    T defaultInstance = Internal.getDefaultInstance(clazz);
    return (T) defaultInstance.getParserForType().parseFrom(codedInputStream);
  }

  public static void validContractProto(List<Transaction> transactionList) throws P2pException {
    List<Future<Boolean>> futureList = new ArrayList<>();
    transactionList.forEach(transaction -> {
      Future<Boolean> future = executorService.submit(() -> {
        try {
          validContractProto(transaction.getRawData().getContract(0));
          return true;
        } catch (Exception e) {
          logger.error("{}", e.getMessage());
        }
        return false;
      });
      futureList.add(future);
    });
    for (Future<Boolean> future : futureList) {
      try {
        if (!future.get()) {
          throw new P2pException(PROTOBUF_ERROR, PROTOBUF_ERROR.getDesc());
        }
      } catch (Exception e) {
        throw new P2pException(PROTOBUF_ERROR, PROTOBUF_ERROR.getDesc());
      }
    }
  }

  public static void validContractProto(Transaction.Contract contract)
      throws InvalidProtocolBufferException, P2pException {
    Any contractParameter = contract.getParameter();
    Class clazz = TransactionFactory.getContract(contract.getType());
    if (clazz == null) {
      throw new P2pException(PROTOBUF_ERROR, PROTOBUF_ERROR.getDesc());
    }
    com.google.protobuf.Message src = contractParameter.unpack(clazz);
    com.google.protobuf.Message contractMessage = parse(clazz,
        Message.getCodedInputStream(src.toByteArray()));

    Message.compareBytes(src.toByteArray(), contractMessage.toByteArray());
  }

  // todo mv this static function to capsule util
  public static byte[] getToAddress(Transaction.Contract contract) {
    ByteString to;
    try {
      Any contractParameter = contract.getParameter();
      switch (contract.getType()) {
        case TransferContract:
          to = contractParameter.unpack(TransferContract.class).getToAddress();
          break;
        case TransferAssetContract:
          to = contractParameter.unpack(TransferAssetContract.class).getToAddress();
          break;
        case ParticipateAssetIssueContract:
          to = contractParameter.unpack(ParticipateAssetIssueContract.class).getToAddress();
          break;

        default:
          return new byte[0];
      }
      return to.toByteArray();
    } catch (Exception ex) {
      logger.error(ex.getMessage());
      return new byte[0];
    }
  }

  // todo mv this static function to capsule util
  public static long getCallValue(Transaction.Contract contract) {
    try {
      Any contractParameter = contract.getParameter();
      switch (contract.getType()) {
        case TriggerSmartContract:
          return contractParameter.unpack(TriggerSmartContract.class).getCallValue();

        case CreateSmartContract:
          return contractParameter.unpack(CreateSmartContract.class).getNewContract()
              .getCallValue();
        default:
          return 0L;
      }
    } catch (Exception ex) {
      logger.error(ex.getMessage());
      return 0L;
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

  public static boolean validateSignature(Transaction transaction,
      byte[] hash, AccountStore accountStore, DynamicPropertiesStore dynamicPropertiesStore)
      throws PermissionException, SignatureException, SignatureFormatException {
    Transaction.Contract contract = transaction.getRawData().getContractList().get(0);
    int permissionId = contract.getPermissionId();
    byte[] owner = getOwner(contract);
    AccountCapsule account = accountStore.get(owner);
    Permission permission = null;
    if (account == null) {
      if (permissionId == 0) {
        permission = AccountCapsule.getDefaultPermission(ByteString.copyFrom(owner));
      }
      if (permissionId == 2) {
        permission = AccountCapsule
            .createDefaultActivePermission(ByteString.copyFrom(owner), dynamicPropertiesStore);
      }
    } else {
      permission = account.getPermissionById(permissionId);
    }
    if (permission == null) {
      throw new PermissionException("permission isn't exit");
    }
    checkPermission(permissionId, permission, contract);
    long weight = checkWeight(permission, transaction.getSignatureList(), hash, null);
    if (weight >= permission.getThreshold()) {
      return true;
    }
    return false;
  }

  public void resetResult() {
    if (this.getInstance().getRetCount() > 0) {
      this.transaction = this.getInstance().toBuilder().clearRet().build();
    }
  }

  public void setResult(TransactionResultCapsule transactionResultCapsule) {
    this.transaction = this.getInstance().toBuilder().addRet(transactionResultCapsule.getInstance())
        .build();
  }

  public void setReference(long blockNum, byte[] blockHash) {
    byte[] refBlockNum = ByteArray.fromLong(blockNum);
    Transaction.raw rawData = this.transaction.getRawData().toBuilder()
        .setRefBlockHash(ByteString.copyFrom(ByteArray.subArray(blockHash, 8, 16)))
        .setRefBlockBytes(ByteString.copyFrom(ByteArray.subArray(refBlockNum, 6, 8)))
        .build();
    this.transaction = this.transaction.toBuilder().setRawData(rawData).build();
  }

  public long getExpiration() {
    return transaction.getRawData().getExpiration();
  }

  /**
   * @param expiration must be in milliseconds format
   */
  public void setExpiration(long expiration) {
    Transaction.raw rawData = this.transaction.getRawData().toBuilder().setExpiration(expiration)
        .build();
    this.transaction = this.transaction.toBuilder().setRawData(rawData).build();
  }

  public void setTimestamp() {
    Transaction.raw rawData = this.transaction.getRawData().toBuilder()
        .setTimestamp(System.currentTimeMillis())
        .build();
    this.transaction = this.transaction.toBuilder().setRawData(rawData).build();
  }

  public void setTimestamp(long timestamp) {
    Transaction.raw rawData = this.transaction.getRawData().toBuilder()
        .setTimestamp(timestamp)
        .build();
    this.transaction = this.transaction.toBuilder().setRawData(rawData).build();
  }

  public long getTimestamp() {
    return transaction.getRawData().getTimestamp();
  }

  @Deprecated
  public void createTransaction(com.google.protobuf.Message message, ContractType contractType) {
    Transaction.raw.Builder transactionBuilder = Transaction.raw.newBuilder().addContract(
        Transaction.Contract.newBuilder().setType(contractType).setParameter(
            Any.pack(message)).build());
    transaction = Transaction.newBuilder().setRawData(transactionBuilder.build()).build();
  }

  public Sha256Hash getMerkleHash() {
    byte[] transBytes = this.transaction.toByteArray();
    return Sha256Hash.of(CommonParameter.getInstance().isECKeyCryptoEngine(),
        transBytes);
  }

  private Sha256Hash getRawHash() {
    return Sha256Hash.of(CommonParameter.getInstance().isECKeyCryptoEngine(),
        this.transaction.getRawData().toByteArray());
  }

  public void sign(byte[] privateKey) {
    SignInterface cryptoEngine = SignUtils
        .fromPrivate(privateKey, CommonParameter.getInstance().isECKeyCryptoEngine());
    //    String signature = cryptoEngine.signHash(getRawHash().getBytes());
    //    ByteString sig = ByteString.copyFrom(signature.getBytes());
    ByteString sig = ByteString.copyFrom(cryptoEngine.Base64toBytes(cryptoEngine
        .signHash(getRawHash().getBytes())));
    this.transaction = this.transaction.toBuilder().addSignature(sig).build();
  }

  public void addSign(byte[] privateKey, AccountStore accountStore)
      throws PermissionException, SignatureException, SignatureFormatException {
    Transaction.Contract contract = this.transaction.getRawData().getContract(0);
    int permissionId = contract.getPermissionId();
    byte[] owner = getOwner(contract);
    AccountCapsule account = accountStore.get(owner);
    if (account == null) {
      throw new PermissionException("Account is not exist!");
    }
    Permission permission = account.getPermissionById(permissionId);
    if (permission == null) {
      throw new PermissionException("permission isn't exit");
    }
    checkPermission(permissionId, permission, contract);
    List<ByteString> approveList = new ArrayList<>();
    SignInterface cryptoEngine = SignUtils
        .fromPrivate(privateKey, CommonParameter.getInstance().isECKeyCryptoEngine());
    byte[] address = cryptoEngine.getAddress();
    if (this.transaction.getSignatureCount() > 0) {
      checkWeight(permission, this.transaction.getSignatureList(), this.getRawHash().getBytes(),
          approveList);
      if (approveList.contains(ByteString.copyFrom(address))) {
        throw new PermissionException(encode58Check(address) + " had signed!");
      }
    }

    long weight = getWeight(permission, address);
    if (weight == 0) {
      throw new PermissionException(
          ByteArray.toHexString(privateKey) + "'s address is " + encode58Check(address)
              + " but it is not contained of permission.");
    }
    //    String signature = cryptoEngine.signHash(getRawHash().getBytes());
    ByteString sig = ByteString.copyFrom(cryptoEngine.Base64toBytes(cryptoEngine
        .signHash(getRawHash().getBytes())));
    this.transaction = this.transaction.toBuilder().addSignature(sig).build();
  }
  
  private static void checkPermission(int permissionId, Permission permission, Transaction.Contract contract) throws PermissionException {
    if (permissionId != 0) {
      if (permission.getType() != PermissionType.Active) {
        throw new PermissionException("Permission type is error");
      }
      //check operations
      if (!checkPermissionOperations(permission, contract)) {
        throw new PermissionException("Permission denied");
      }
    }
  }

  /**
   * validate signature
   */
  public boolean validatePubSignature(AccountStore accountStore,
      DynamicPropertiesStore dynamicPropertiesStore)
      throws ValidateSignatureException {
    if (!isVerified) {
      if (this.transaction.getSignatureCount() <= 0
              || this.transaction.getRawData().getContractCount() <= 0) {
        throw new ValidateSignatureException("miss sig or contract");
      }
      if (this.transaction.getSignatureCount() > dynamicPropertiesStore
              .getTotalSignNum()) {
        throw new ValidateSignatureException("too many signatures");
      }

      byte[] hash = this.getRawHash().getBytes();

      try {
        if (!validateSignature(this.transaction, hash, accountStore, dynamicPropertiesStore)) {
          isVerified = false;
          throw new ValidateSignatureException("sig error");
        }
      } catch (SignatureException | PermissionException | SignatureFormatException e) {
        isVerified = false;
        throw new ValidateSignatureException(e.getMessage());
      }
      isVerified = true;
    }
    return true;
  }

  /**
   * validate signature
   */
  public boolean validateSignature(AccountStore accountStore,
      DynamicPropertiesStore dynamicPropertiesStore) throws ValidateSignatureException {
    if (!isVerified) {
      //Do not support multi contracts in one transaction
      Transaction.Contract contract = this.getInstance().getRawData().getContract(0);
      if (contract.getType() != ContractType.ShieldedTransferContract) {
        validatePubSignature(accountStore, dynamicPropertiesStore);
      } else {  //ShieldedTransfer
        byte[] owner = getOwner(contract);
        if (!ArrayUtils.isEmpty(owner)) { //transfer from transparent address
          validatePubSignature(accountStore, dynamicPropertiesStore);
        } else { //transfer from shielded address
          if (this.transaction.getSignatureCount() > 0) {
            throw new ValidateSignatureException("there should be no signatures signed by "
                    + "transparent address when transfer from shielded address");
          }
        }
      }
      isVerified = true;
    }  
    return true;
  }

  public Sha256Hash getTransactionId() {
    return getRawHash();
  }

  @Override
  public byte[] getData() {
    return this.transaction.toByteArray();
  }

  public long getSerializedSize() {
    return this.transaction.getSerializedSize();
  }

  public long getResultSerializedSize() {
    long size = 0;
    for (Result result : this.transaction.getRetList()) {
      size += result.getSerializedSize();
    }
    return size;
  }

  @Override
  public Transaction getInstance() {
    return this.transaction;
  }

  @Override
  public String toString() {

    toStringBuff.setLength(0);
    toStringBuff.append("TransactionCapsule \n[ ");

    toStringBuff.append("hash=").append(getTransactionId()).append("\n");
    AtomicInteger i = new AtomicInteger();
    if (!getInstance().getRawData().getContractList().isEmpty()) {
      toStringBuff.append("contract list:{ ");
      getInstance().getRawData().getContractList().forEach(contract -> {
        toStringBuff.append("[" + i + "] ").append("type: ").append(contract.getType())
            .append("\n");
        toStringBuff.append("from address=").append(getOwner(contract)).append("\n");
        toStringBuff.append("to address=").append(getToAddress(contract)).append("\n");
        if (contract.getType().equals(ContractType.TransferContract)) {
          TransferContract transferContract;
          try {
            transferContract = contract.getParameter()
                .unpack(TransferContract.class);
            toStringBuff.append("transfer amount=").append(transferContract.getAmount())
                .append("\n");
          } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
          }
        } else if (contract.getType().equals(ContractType.TransferAssetContract)) {
          TransferAssetContract transferAssetContract;
          try {
            transferAssetContract = contract.getParameter()
                .unpack(TransferAssetContract.class);
            toStringBuff.append("transfer asset=").append(transferAssetContract.getAssetName())
                .append("\n");
            toStringBuff.append("transfer amount=").append(transferAssetContract.getAmount())
                .append("\n");
          } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
          }
        }
        if (this.transaction.getSignatureList().size() >= i.get() + 1) {
          toStringBuff.append("sign=").append(getBase64FromByteString(
              this.transaction.getSignature(i.getAndIncrement()))).append("\n");
        }
      });
      toStringBuff.append("}\n");
    } else {
      toStringBuff.append("contract list is empty\n");
    }

    toStringBuff.append("]");
    return toStringBuff.toString();
  }

  public void setResult(TransactionContext context) {
    this.setResultCode(context.getProgramResult().getResultCode());
  }

  public void setResultCode(contractResult code) {
    Result ret;
    if (this.transaction.getRetCount() > 0) {
      ret = this.transaction.getRet(0).toBuilder().setContractRet(code).build();

      this.transaction = transaction.toBuilder().setRet(0, ret).build();
      return;
    }
    ret = Result.newBuilder().setContractRet(code).build();
    this.transaction = transaction.toBuilder().addRet(ret).build();
  }

  public Transaction.Result.contractResult getContractResult() {
    if (this.transaction.getRetCount() > 0) {
      return this.transaction.getRet(0).getContractRet();
    }
    return null;
  }



  public contractResult getContractRet() {
    if (this.transaction.getRetCount() <= 0) {
      return null;
    }
    return this.transaction.getRet(0).getContractRet();
  }

  /**
   * Check if a transaction capsule contains a smart contract transaction or not.
   * @return
   */
  public boolean isContractType() {
    try {
      ContractType type = this.getInstance().getRawData().getContract(0).getType();
      return  (type == ContractType.TriggerSmartContract || type == ContractType.CreateSmartContract);
    } catch (Exception ex) {
      logger.warn("check contract type failed, reason {}", ex.getMessage());
      return false;
    }
  }

  public BalanceContract.TransferContract getTransferContract() {
    try {
      return transaction.getRawData()
          .getContract(0)
          .getParameter()
          .unpack(BalanceContract.TransferContract.class);
    } catch (InvalidProtocolBufferException e) {
      return null;
    }
  }
}
