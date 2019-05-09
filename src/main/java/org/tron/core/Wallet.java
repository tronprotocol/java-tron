/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.*;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.api.GrpcAPI.TransactionExtention.Builder;
import org.tron.api.GrpcAPI.TransactionSignWeight.Result;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.Hash;
import org.tron.common.overlay.discover.node.NodeHandler;
import org.tron.common.overlay.discover.node.NodeManager;
import org.tron.common.overlay.message.Message;
import org.tron.common.runtime.Runtime;
import org.tron.common.runtime.RuntimeImpl;
import org.tron.common.runtime.config.VMConfig;
import org.tron.common.runtime.vm.program.ProgramResult;
import org.tron.common.runtime.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.tron.common.storage.DepositImpl;
import org.tron.common.utils.*;
import org.tron.common.zksnark.Librustzcash;
import org.tron.core.actuator.Actuator;
import org.tron.core.actuator.ActuatorFactory;
import org.tron.core.capsule.*;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.config.args.Args;
import org.tron.core.db.*;
import org.tron.core.exception.*;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.TronNetService;
import org.tron.core.net.message.TransactionMessage;
import org.tron.core.zen.*;
import org.tron.core.zen.address.*;
import org.tron.core.zen.merkle.*;
import org.tron.core.zen.note.*;
import org.tron.core.zen.transaction.Recipient;
import org.tron.core.zen.utils.KeyIo;
import org.tron.core.zen.walletdb.CKeyMetadata;
import org.tron.core.zen.zip32.ExtendedSpendingKey;
import org.tron.core.zen.zip32.HDSeed;
import org.tron.core.zen.zip32.HdChain;
import org.tron.protos.Contract.MerklePath;
import org.tron.protos.Contract.*;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.*;
import org.tron.protos.Protocol.Permission.PermissionType;
import org.tron.protos.Protocol.SmartContract.ABI;
import org.tron.protos.Protocol.SmartContract.ABI.Entry.StateMutabilityType;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;

import java.security.SignatureException;
import java.util.*;

import static org.tron.core.config.Parameter.DatabaseConstants.EXCHANGE_COUNT_LIMIT_MAX;
import static org.tron.core.config.Parameter.DatabaseConstants.PROPOSAL_COUNT_LIMIT_MAX;
import static org.tron.core.zen.zip32.ExtendedSpendingKey.ZIP32_HARDENED_KEY_LIMIT;

@Slf4j
@Component
public class Wallet {

  @Getter
  private final ECKey ecKey;
  @Autowired
  private TronNetService tronNetService;
  @Autowired
  private TronNetDelegate tronNetDelegate;
  @Autowired
  private Manager dbManager;
  @Autowired
  private NodeManager nodeManager;
  private static String addressPreFixString = Constant.ADD_PRE_FIX_STRING_MAINNET;  //default testnet
  private static byte addressPreFixByte = Constant.ADD_PRE_FIX_BYTE_MAINNET;

  private int minEffectiveConnection = Args.getInstance().getMinEffectiveConnection();

  /**
   * Creates a new Wallet with a random ECKey.
   */
  public Wallet() {
    this.ecKey = new ECKey(Utils.getRandom());
  }

  /**
   * Creates a Wallet with an existing ECKey.
   */
  public Wallet(final ECKey ecKey) {
    this.ecKey = ecKey;
    logger.info("wallet address: {}", ByteArray.toHexString(this.ecKey.getAddress()));
  }

  public static boolean isConstant(ABI abi, TriggerSmartContract triggerSmartContract)
      throws ContractValidateException {
    try {
      boolean constant = isConstant(abi, getSelector(triggerSmartContract.getData().toByteArray()));
      if (constant) {
        if (!Args.getInstance().isSupportConstant()) {
          throw new ContractValidateException("this node don't support constant");
        }
      }
      return constant;
    } catch (ContractValidateException e) {
      throw e;
    } catch (Exception e) {
      return false;
    }
  }

  public byte[] getAddress() {
    return ecKey.getAddress();
  }

  public static String getAddressPreFixString() {
    return addressPreFixString;
  }

  public static void setAddressPreFixString(String addressPreFixString) {
    Wallet.addressPreFixString = addressPreFixString;
  }

  public static byte getAddressPreFixByte() {
    return addressPreFixByte;
  }

  public static void setAddressPreFixByte(byte addressPreFixByte) {
    Wallet.addressPreFixByte = addressPreFixByte;
  }

  public static boolean addressValid(byte[] address) {
    if (ArrayUtils.isEmpty(address)) {
      logger.warn("Warning: Address is empty !!");
      return false;
    }
    if (address.length != Constant.ADDRESS_SIZE / 2) {
      logger.warn(
          "Warning: Address length need " + Constant.ADDRESS_SIZE + " but " + address.length
              + " !!");
      return false;
    }
    if (address[0] != addressPreFixByte) {
      logger.warn("Warning: Address need prefix with " + addressPreFixByte + " but "
          + address[0] + " !!");
      return false;
    }
    //Other rule;
    return true;
  }

  public static String encode58Check(byte[] input) {
    byte[] hash0 = Sha256Hash.hash(input);
    byte[] hash1 = Sha256Hash.hash(hash0);
    byte[] inputCheck = new byte[input.length + 4];
    System.arraycopy(input, 0, inputCheck, 0, input.length);
    System.arraycopy(hash1, 0, inputCheck, input.length, 4);
    return Base58.encode(inputCheck);
  }

  private static byte[] decode58Check(String input) {
    byte[] decodeCheck = Base58.decode(input);
    if (decodeCheck.length <= 4) {
      return null;
    }
    byte[] decodeData = new byte[decodeCheck.length - 4];
    System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
    byte[] hash0 = Sha256Hash.hash(decodeData);
    byte[] hash1 = Sha256Hash.hash(hash0);
    if (hash1[0] == decodeCheck[decodeData.length] &&
        hash1[1] == decodeCheck[decodeData.length + 1] &&
        hash1[2] == decodeCheck[decodeData.length + 2] &&
        hash1[3] == decodeCheck[decodeData.length + 3]) {
      return decodeData;
    }
    return null;
  }

  public static byte[] generateContractAddress(Transaction trx) {

    CreateSmartContract contract = ContractCapsule.getSmartContractFromTransaction(trx);
    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
    TransactionCapsule trxCap = new TransactionCapsule(trx);
    byte[] txRawDataHash = trxCap.getTransactionId().getBytes();

    byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
    System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
    System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);

    return Hash.sha3omit12(combined);

  }

  public static byte[] generateContractAddress(byte[] ownerAddress, byte[] txRawDataHash) {

    byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
    System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
    System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);

    return Hash.sha3omit12(combined);

  }

  public static byte[] generateContractAddress(byte[] transactionRootId, long nonce) {
    byte[] nonceBytes = Longs.toByteArray(nonce);
    byte[] combined = new byte[transactionRootId.length + nonceBytes.length];
    System.arraycopy(transactionRootId, 0, combined, 0, transactionRootId.length);
    System.arraycopy(nonceBytes, 0, combined, transactionRootId.length, nonceBytes.length);

    return Hash.sha3omit12(combined);
  }


  public static byte[] tryDecodeFromBase58Check(String address) {
    try {
      return Wallet.decodeFromBase58Check(address);
    } catch (Exception ex) {
      return null;
    }
  }

  public static byte[] decodeFromBase58Check(String addressBase58) {
    if (StringUtils.isEmpty(addressBase58)) {
      logger.warn("Warning: Address is empty !!");
      return null;
    }
    byte[] address = decode58Check(addressBase58);
    if (address == null) {
      return null;
    }

    if (!addressValid(address)) {
      return null;
    }

    return address;
  }

//  public ShieldAddress generateShieldAddress() {
//    ShieldAddress.Builder builder = ShieldAddress.newBuilder();
//    ShieldAddressGenerator shieldAddressGenerator = new ShieldAddressGenerator();
//
//    byte[] privateKey = shieldAddressGenerator.generatePrivateKey();
//    byte[] publicKey = shieldAddressGenerator.generatePublicKey(privateKey);
//
//    byte[] privateKeyEnc = shieldAddressGenerator.generatePrivateKeyEnc(privateKey);
//    byte[] publicKeyEnc = shieldAddressGenerator.generatePublicKeyEnc(privateKeyEnc);
//
//    byte[] addPrivate = ByteUtil.merge(privateKey, privateKeyEnc);
//    byte[] addPublic = ByteUtil.merge(publicKey, publicKeyEnc);
//
//    builder.setPrivateAddress(ByteString.copyFrom(addPrivate));
//    builder.setPublicAddress(ByteString.copyFrom(addPublic));
//    return builder.build();
//  }

  public Account getAccount(Account account) {
    AccountStore accountStore = dbManager.getAccountStore();
    AccountCapsule accountCapsule = accountStore.get(account.getAddress().toByteArray());
    if (accountCapsule == null) {
      return null;
    }
    BandwidthProcessor processor = new BandwidthProcessor(dbManager);
    processor.updateUsage(accountCapsule);

    EnergyProcessor energyProcessor = new EnergyProcessor(dbManager);
    energyProcessor.updateUsage(accountCapsule);

    long genesisTimeStamp = dbManager.getGenesisBlock().getTimeStamp();
    accountCapsule.setLatestConsumeTime(genesisTimeStamp
        + ChainConstant.BLOCK_PRODUCED_INTERVAL * accountCapsule.getLatestConsumeTime());
    accountCapsule.setLatestConsumeFreeTime(genesisTimeStamp
        + ChainConstant.BLOCK_PRODUCED_INTERVAL * accountCapsule.getLatestConsumeFreeTime());
    accountCapsule.setLatestConsumeTimeForEnergy(genesisTimeStamp
        + ChainConstant.BLOCK_PRODUCED_INTERVAL * accountCapsule.getLatestConsumeTimeForEnergy());

    return accountCapsule.getInstance();
  }


  public Account getAccountById(Account account) {
    AccountStore accountStore = dbManager.getAccountStore();
    AccountIdIndexStore accountIdIndexStore = dbManager.getAccountIdIndexStore();
    byte[] address = accountIdIndexStore.get(account.getAccountId());
    if (address == null) {
      return null;
    }
    AccountCapsule accountCapsule = accountStore.get(address);
    if (accountCapsule == null) {
      return null;
    }
    BandwidthProcessor processor = new BandwidthProcessor(dbManager);
    processor.updateUsage(accountCapsule);

    EnergyProcessor energyProcessor = new EnergyProcessor(dbManager);
    energyProcessor.updateUsage(accountCapsule);

    return accountCapsule.getInstance();
  }

  /**
   * Create a transaction.
   */
  /*public Transaction createTransaction(byte[] address, String to, long amount) {
    long balance = getBalance(address);
    return new TransactionCapsule(address, to, amount, balance, utxoStore).getInstance();
  } */

  /**
   * Create a transaction by contract.
   */
  @Deprecated
  public Transaction createTransaction(TransferContract contract) {
    AccountStore accountStore = dbManager.getAccountStore();
    return new TransactionCapsule(contract, accountStore).getInstance();
  }

  public TransactionCapsule createTransactionCapsuleWithoutValidate(
      com.google.protobuf.Message message,
      ContractType contractType) throws ContractValidateException {
    TransactionCapsule trx = new TransactionCapsule(message, contractType);
    try {
      BlockId blockId = dbManager.getHeadBlockId();
      if (Args.getInstance().getTrxReferenceBlock().equals("solid")) {
        blockId = dbManager.getSolidBlockId();
      }
      trx.setReference(blockId.getNum(), blockId.getBytes());
      long expiration =
          dbManager.getHeadBlockTimeStamp() + Args.getInstance()
              .getTrxExpirationTimeInMilliseconds();
      trx.setExpiration(expiration);
      trx.setTimestamp();
    } catch (Exception e) {
      logger.error("Create transaction capsule failed.", e);
    }
    return trx;
  }

  public TransactionCapsule createTransactionCapsule(com.google.protobuf.Message message,
      ContractType contractType) throws ContractValidateException {
    TransactionCapsule trx = new TransactionCapsule(message, contractType);
    if (contractType != ContractType.CreateSmartContract
        && contractType != ContractType.TriggerSmartContract) {
      List<Actuator> actList = ActuatorFactory.createActuator(trx, dbManager);
      for (Actuator act : actList) {
        act.validate();
      }
    }

    if (contractType == ContractType.CreateSmartContract) {

      CreateSmartContract contract = ContractCapsule
          .getSmartContractFromTransaction(trx.getInstance());
      long percent = contract.getNewContract().getConsumeUserResourcePercent();
      if (percent < 0 || percent > 100) {
        throw new ContractValidateException("percent must be >= 0 and <= 100");
      }
    }

    try {
      BlockId blockId = dbManager.getHeadBlockId();
      if (Args.getInstance().getTrxReferenceBlock().equals("solid")) {
        blockId = dbManager.getSolidBlockId();
      }
      trx.setReference(blockId.getNum(), blockId.getBytes());
      long expiration =
          dbManager.getHeadBlockTimeStamp() + Args.getInstance()
              .getTrxExpirationTimeInMilliseconds();
      trx.setExpiration(expiration);
      trx.setTimestamp();
    } catch (Exception e) {
      logger.error("Create transaction capsule failed.", e);
    }
    return trx;
  }

  /**
   * Broadcast a transaction.
   */
  public GrpcAPI.Return broadcastTransaction(Transaction signaturedTransaction) {
    GrpcAPI.Return.Builder builder = GrpcAPI.Return.newBuilder();
    TransactionCapsule trx = new TransactionCapsule(signaturedTransaction);
    Message message = new TransactionMessage(signaturedTransaction);

    try {
      if (minEffectiveConnection != 0) {
        if (tronNetDelegate.getActivePeer().isEmpty()) {
          logger.warn("Broadcast transaction {} failed, no connection.", trx.getTransactionId());
          return builder.setResult(false).setCode(response_code.NO_CONNECTION)
              .setMessage(ByteString.copyFromUtf8("no connection"))
              .build();
        }

        int count = (int) tronNetDelegate.getActivePeer().stream()
            .filter(p -> !p.isNeedSyncFromUs() && !p.isNeedSyncFromPeer())
            .count();

        if (count < minEffectiveConnection) {
          String info = "effective connection:" + count + " lt minEffectiveConnection:"
              + minEffectiveConnection;
          logger.warn("Broadcast transaction {} failed, {}.", trx.getTransactionId(), info);
          return builder.setResult(false).setCode(response_code.NOT_ENOUGH_EFFECTIVE_CONNECTION)
              .setMessage(ByteString.copyFromUtf8(info))
              .build();
        }
      }

      if (dbManager.isTooManyPending()) {
        logger.warn("Broadcast transaction {} failed, too many pending.", trx.getTransactionId());
        return builder.setResult(false).setCode(response_code.SERVER_BUSY).build();
      }

      if (dbManager.isGeneratingBlock()) {
        logger
            .warn("Broadcast transaction {} failed, is generating block.", trx.getTransactionId());
        return builder.setResult(false).setCode(response_code.SERVER_BUSY).build();
      }

      if (dbManager.getTransactionIdCache().getIfPresent(trx.getTransactionId()) != null) {
        logger.warn("Broadcast transaction {} failed, is already exist.", trx.getTransactionId());
        return builder.setResult(false).setCode(response_code.DUP_TRANSACTION_ERROR).build();
      } else {
        dbManager.getTransactionIdCache().put(trx.getTransactionId(), true);
      }
      if (dbManager.getDynamicPropertiesStore().supportVM()) {
        trx.resetResult();
      }
      dbManager.pushTransaction(trx);
      tronNetService.broadcast(message);
      logger.info("Broadcast transaction {} successfully.", trx.getTransactionId());
      return builder.setResult(true).setCode(response_code.SUCCESS).build();
    } catch (ValidateSignatureException e) {
      logger.error("Broadcast transaction {} failed, {}.", trx.getTransactionId(), e.getMessage());
      return builder.setResult(false).setCode(response_code.SIGERROR)
          .setMessage(ByteString.copyFromUtf8("validate signature error " + e.getMessage()))
          .build();
    } catch (ContractValidateException e) {
      logger.error("Broadcast transaction {} failed, {}.", trx.getTransactionId(), e.getMessage());
      return builder.setResult(false).setCode(response_code.CONTRACT_VALIDATE_ERROR)
          .setMessage(ByteString.copyFromUtf8("contract validate error : " + e.getMessage()))
          .build();
    } catch (ContractExeException e) {
      logger.error("Broadcast transaction {} failed, {}.", trx.getTransactionId(), e.getMessage());
      return builder.setResult(false).setCode(response_code.CONTRACT_EXE_ERROR)
          .setMessage(ByteString.copyFromUtf8("contract execute error : " + e.getMessage()))
          .build();
    } catch (AccountResourceInsufficientException e) {
      logger.error("Broadcast transaction {} failed, {}.", trx.getTransactionId(), e.getMessage());
      return builder.setResult(false).setCode(response_code.BANDWITH_ERROR)
          .setMessage(ByteString.copyFromUtf8("AccountResourceInsufficient error"))
          .build();
    } catch (DupTransactionException e) {
      logger.error("Broadcast transaction {} failed, {}.", trx.getTransactionId(), e.getMessage());
      return builder.setResult(false).setCode(response_code.DUP_TRANSACTION_ERROR)
          .setMessage(ByteString.copyFromUtf8("dup transaction"))
          .build();
    } catch (TaposException e) {
      logger.error("Broadcast transaction {} failed, {}.", trx.getTransactionId(), e.getMessage());
      return builder.setResult(false).setCode(response_code.TAPOS_ERROR)
          .setMessage(ByteString.copyFromUtf8("Tapos check error"))
          .build();
    } catch (TooBigTransactionException e) {
      logger.error("Broadcast transaction {} failed, {}.", trx.getTransactionId(), e.getMessage());
      return builder.setResult(false).setCode(response_code.TOO_BIG_TRANSACTION_ERROR)
          .setMessage(ByteString.copyFromUtf8("transaction size is too big"))
          .build();
    } catch (TransactionExpirationException e) {
      logger.error("Broadcast transaction {} failed, {}.", trx.getTransactionId(), e.getMessage());
      return builder.setResult(false).setCode(response_code.TRANSACTION_EXPIRATION_ERROR)
          .setMessage(ByteString.copyFromUtf8("transaction expired"))
          .build();
    } catch (Exception e) {
      logger.error("Broadcast transaction {} failed, {}.", trx.getTransactionId(), e.getMessage());
      return builder.setResult(false).setCode(response_code.OTHER_ERROR)
          .setMessage(ByteString.copyFromUtf8("other error : " + e.getMessage()))
          .build();
    }
  }

  public TransactionCapsule getTransactionSign(TransactionSign transactionSign) {
    byte[] privateKey = transactionSign.getPrivateKey().toByteArray();
    TransactionCapsule trx = new TransactionCapsule(transactionSign.getTransaction());
    trx.sign(privateKey);
    return trx;
  }

  public TransactionCapsule addSign(TransactionSign transactionSign)
      throws PermissionException, SignatureException, SignatureFormatException {
    byte[] privateKey = transactionSign.getPrivateKey().toByteArray();
    TransactionCapsule trx = new TransactionCapsule(transactionSign.getTransaction());
    trx.addSign(privateKey, dbManager.getAccountStore());
    return trx;
  }

  public static boolean checkPermissionOprations(Permission permission, Contract contract)
      throws PermissionException {
    ByteString operations = permission.getOperations();
    if (operations.size() != 32) {
      throw new PermissionException("operations size must 32");
    }
    int contractType = contract.getTypeValue();
    boolean b = (operations.byteAt(contractType / 8) & (1 << (contractType % 8))) != 0;
    return b;
  }

  public TransactionSignWeight getTransactionSignWeight(Transaction trx) {
    TransactionSignWeight.Builder tswBuilder = TransactionSignWeight.newBuilder();
    TransactionExtention.Builder trxExBuilder = TransactionExtention.newBuilder();
    trxExBuilder.setTransaction(trx);
    trxExBuilder.setTxid(ByteString.copyFrom(Sha256Hash.hash(trx.getRawData().toByteArray())));
    Return.Builder retBuilder = Return.newBuilder();
    retBuilder.setResult(true).setCode(response_code.SUCCESS);
    trxExBuilder.setResult(retBuilder);
    tswBuilder.setTransaction(trxExBuilder);
    Result.Builder resultBuilder = Result.newBuilder();
    try {
      Contract contract = trx.getRawData().getContract(0);
      byte[] owner = TransactionCapsule.getOwner(contract);
      AccountCapsule account = dbManager.getAccountStore().get(owner);
      if (account == null) {
        throw new PermissionException("Account is not exist!");
      }
      int permissionId = contract.getPermissionId();
      Permission permission = account.getPermissionById(permissionId);
      if (permission == null) {
        throw new PermissionException("permission isn't exit");
      }
      if (permissionId != 0) {
        if (permission.getType() != PermissionType.Active) {
          throw new PermissionException("Permission type is error");
        }
        //check oprations
        if (!checkPermissionOprations(permission, contract)) {
          throw new PermissionException("Permission denied");
        }
      }
      tswBuilder.setPermission(permission);
      if (trx.getSignatureCount() > 0) {
        List<ByteString> approveList = new ArrayList<ByteString>();
        long currentWeight = TransactionCapsule.checkWeight(permission, trx.getSignatureList(),
            Sha256Hash.hash(trx.getRawData().toByteArray()), approveList);
        tswBuilder.addAllApprovedList(approveList);
        tswBuilder.setCurrentWeight(currentWeight);
      }
      if (tswBuilder.getCurrentWeight() >= permission.getThreshold()) {
        resultBuilder.setCode(Result.response_code.ENOUGH_PERMISSION);
      } else {
        resultBuilder.setCode(Result.response_code.NOT_ENOUGH_PERMISSION);
      }
    } catch (SignatureFormatException signEx) {
      resultBuilder.setCode(Result.response_code.SIGNATURE_FORMAT_ERROR);
      resultBuilder.setMessage(signEx.getMessage());
    } catch (SignatureException signEx) {
      resultBuilder.setCode(Result.response_code.COMPUTE_ADDRESS_ERROR);
      resultBuilder.setMessage(signEx.getMessage());
    } catch (PermissionException permEx) {
      resultBuilder.setCode(Result.response_code.PERMISSION_ERROR);
      resultBuilder.setMessage(permEx.getMessage());
    } catch (Exception ex) {
      resultBuilder.setCode(Result.response_code.OTHER_ERROR);
      resultBuilder.setMessage(ex.getClass() + " : " + ex.getMessage());
    }
    tswBuilder.setResult(resultBuilder);
    return tswBuilder.build();
  }

  public TransactionApprovedList getTransactionApprovedList(Transaction trx) {
    TransactionApprovedList.Builder tswBuilder = TransactionApprovedList.newBuilder();
    TransactionExtention.Builder trxExBuilder = TransactionExtention.newBuilder();
    trxExBuilder.setTransaction(trx);
    trxExBuilder.setTxid(ByteString.copyFrom(Sha256Hash.hash(trx.getRawData().toByteArray())));
    Return.Builder retBuilder = Return.newBuilder();
    retBuilder.setResult(true).setCode(response_code.SUCCESS);
    trxExBuilder.setResult(retBuilder);
    tswBuilder.setTransaction(trxExBuilder);
    TransactionApprovedList.Result.Builder resultBuilder = TransactionApprovedList.Result
        .newBuilder();
    try {
      Contract contract = trx.getRawData().getContract(0);
      byte[] owner = TransactionCapsule.getOwner(contract);
      AccountCapsule account = dbManager.getAccountStore().get(owner);
      if (account == null) {
        throw new PermissionException("Account is not exist!");
      }

      if (trx.getSignatureCount() > 0) {
        List<ByteString> approveList = new ArrayList<ByteString>();
        byte[] hash = Sha256Hash.hash(trx.getRawData().toByteArray());
        for (ByteString sig : trx.getSignatureList()) {
          if (sig.size() < 65) {
            throw new SignatureFormatException(
                "Signature size is " + sig.size());
          }
          String base64 = TransactionCapsule.getBase64FromByteString(sig);
          byte[] address = ECKey.signatureToAddress(hash, base64);
          approveList.add(ByteString.copyFrom(address)); //out put approve list.
        }
        tswBuilder.addAllApprovedList(approveList);
      }
      resultBuilder.setCode(TransactionApprovedList.Result.response_code.SUCCESS);
    } catch (SignatureFormatException signEx) {
      resultBuilder.setCode(TransactionApprovedList.Result.response_code.SIGNATURE_FORMAT_ERROR);
      resultBuilder.setMessage(signEx.getMessage());
    } catch (SignatureException signEx) {
      resultBuilder.setCode(TransactionApprovedList.Result.response_code.COMPUTE_ADDRESS_ERROR);
      resultBuilder.setMessage(signEx.getMessage());
    } catch (Exception ex) {
      resultBuilder.setCode(TransactionApprovedList.Result.response_code.OTHER_ERROR);
      resultBuilder.setMessage(ex.getClass() + " : " + ex.getMessage());
    }
    tswBuilder.setResult(resultBuilder);
    return tswBuilder.build();
  }

  public byte[] pass2Key(byte[] passPhrase) {
    return Sha256Hash.hash(passPhrase);
  }

  public byte[] createAdresss(byte[] passPhrase) {
    byte[] privateKey = pass2Key(passPhrase);
    ECKey ecKey = ECKey.fromPrivate(privateKey);
    return ecKey.getAddress();
  }

  public Block getNowBlock() {
    List<BlockCapsule> blockList = dbManager.getBlockStore().getBlockByLatestNum(1);
    if (CollectionUtils.isEmpty(blockList)) {
      return null;
    } else {
      return blockList.get(0).getInstance();
    }
  }

  public Block getBlockByNum(long blockNum) {
    try {
      return dbManager.getBlockByNum(blockNum).getInstance();
    } catch (StoreException e) {
      logger.info(e.getMessage());
      return null;
    }
  }

  public long getTransactionCountByBlockNum(long blockNum) {
    long count = 0;

    try {
      Block block = dbManager.getBlockByNum(blockNum).getInstance();
      count = block.getTransactionsCount();
    } catch (StoreException e) {
      logger.error(e.getMessage());
    }

    return count;
  }

  public WitnessList getWitnessList() {
    WitnessList.Builder builder = WitnessList.newBuilder();
    List<WitnessCapsule> witnessCapsuleList = dbManager.getWitnessStore().getAllWitnesses();
    witnessCapsuleList
        .forEach(witnessCapsule -> builder.addWitnesses(witnessCapsule.getInstance()));
    return builder.build();
  }

  public ProposalList getProposalList() {
    ProposalList.Builder builder = ProposalList.newBuilder();
    List<ProposalCapsule> proposalCapsuleList = dbManager.getProposalStore().getAllProposals();
    proposalCapsuleList
        .forEach(proposalCapsule -> builder.addProposals(proposalCapsule.getInstance()));
    return builder.build();
  }

  public DelegatedResourceList getDelegatedResource(ByteString fromAddress, ByteString toAddress) {
    DelegatedResourceList.Builder builder = DelegatedResourceList.newBuilder();
    byte[] dbKey = DelegatedResourceCapsule
        .createDbKey(fromAddress.toByteArray(), toAddress.toByteArray());
    DelegatedResourceCapsule delegatedResourceCapsule = dbManager.getDelegatedResourceStore()
        .get(dbKey);
    if (delegatedResourceCapsule != null) {
      builder.addDelegatedResource(delegatedResourceCapsule.getInstance());
    }
    return builder.build();
  }

  public DelegatedResourceAccountIndex getDelegatedResourceAccountIndex(ByteString address) {
    DelegatedResourceAccountIndexCapsule accountIndexCapsule =
        dbManager.getDelegatedResourceAccountIndexStore().get(address.toByteArray());
    if (accountIndexCapsule != null) {
      return accountIndexCapsule.getInstance();
    } else {
      return null;
    }
  }

  public ExchangeList getExchangeList() {
    ExchangeList.Builder builder = ExchangeList.newBuilder();
    List<ExchangeCapsule> exchangeCapsuleList = dbManager.getExchangeStoreFinal().getAllExchanges();

    exchangeCapsuleList
        .forEach(exchangeCapsule -> builder.addExchanges(exchangeCapsule.getInstance()));
    return builder.build();
  }

  public Protocol.ChainParameters getChainParameters() {
    Protocol.ChainParameters.Builder builder = Protocol.ChainParameters.newBuilder();

    // MAINTENANCE_TIME_INTERVAL, //ms  ,0
    builder.addChainParameter(
        Protocol.ChainParameters.ChainParameter.newBuilder()
            .setKey("getMaintenanceTimeInterval")
            .setValue(dbManager.getDynamicPropertiesStore().getMaintenanceTimeInterval())
            .build());
    //    ACCOUNT_UPGRADE_COST, //drop ,1
    builder.addChainParameter(
        Protocol.ChainParameters.ChainParameter.newBuilder()
            .setKey("getAccountUpgradeCost")
            .setValue(dbManager.getDynamicPropertiesStore().getAccountUpgradeCost())
            .build());
    //    CREATE_ACCOUNT_FEE, //drop ,2
    builder.addChainParameter(
        Protocol.ChainParameters.ChainParameter.newBuilder()
            .setKey("getCreateAccountFee")
            .setValue(dbManager.getDynamicPropertiesStore().getCreateAccountFee())
            .build());
    //    TRANSACTION_FEE, //drop ,3
    builder.addChainParameter(
        Protocol.ChainParameters.ChainParameter.newBuilder()
            .setKey("getTransactionFee")
            .setValue(dbManager.getDynamicPropertiesStore().getTransactionFee())
            .build());
    //    ASSET_ISSUE_FEE, //drop ,4
    builder.addChainParameter(
        Protocol.ChainParameters.ChainParameter.newBuilder()
            .setKey("getAssetIssueFee")
            .setValue(dbManager.getDynamicPropertiesStore().getAssetIssueFee())
            .build());
    //    WITNESS_PAY_PER_BLOCK, //drop ,5
    builder.addChainParameter(
        Protocol.ChainParameters.ChainParameter.newBuilder()
            .setKey("getWitnessPayPerBlock")
            .setValue(dbManager.getDynamicPropertiesStore().getWitnessPayPerBlock())
            .build());
    //    WITNESS_STANDBY_ALLOWANCE, //drop ,6
    builder.addChainParameter(
        Protocol.ChainParameters.ChainParameter.newBuilder()
            .setKey("getWitnessStandbyAllowance")
            .setValue(dbManager.getDynamicPropertiesStore().getWitnessStandbyAllowance())
            .build());
    //    CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT, //drop ,7
    builder.addChainParameter(
        Protocol.ChainParameters.ChainParameter.newBuilder()
            .setKey("getCreateNewAccountFeeInSystemContract")
            .setValue(
                dbManager.getDynamicPropertiesStore().getCreateNewAccountFeeInSystemContract())
            .build());
    //    CREATE_NEW_ACCOUNT_BANDWIDTH_RATE, // 1 ~ ,8
    builder.addChainParameter(
        Protocol.ChainParameters.ChainParameter.newBuilder()
            .setKey("getCreateNewAccountBandwidthRate")
            .setValue(dbManager.getDynamicPropertiesStore().getCreateNewAccountBandwidthRate())
            .build());
    //    ALLOW_CREATION_OF_CONTRACTS, // 0 / >0 ,9
    builder.addChainParameter(
        Protocol.ChainParameters.ChainParameter.newBuilder()
            .setKey("getAllowCreationOfContracts")
            .setValue(dbManager.getDynamicPropertiesStore().getAllowCreationOfContracts())
            .build());
    //    REMOVE_THE_POWER_OF_THE_GR,  // 1 ,10
    builder.addChainParameter(
        Protocol.ChainParameters.ChainParameter.newBuilder()
            .setKey("getRemoveThePowerOfTheGr")
            .setValue(dbManager.getDynamicPropertiesStore().getRemoveThePowerOfTheGr())
            .build());
    //    ENERGY_FEE, // drop, 11
    builder.addChainParameter(
        Protocol.ChainParameters.ChainParameter.newBuilder()
            .setKey("getEnergyFee")
            .setValue(dbManager.getDynamicPropertiesStore().getEnergyFee())
            .build());
    //    EXCHANGE_CREATE_FEE, // drop, 12
    builder.addChainParameter(
        Protocol.ChainParameters.ChainParameter.newBuilder()
            .setKey("getExchangeCreateFee")
            .setValue(dbManager.getDynamicPropertiesStore().getExchangeCreateFee())
            .build());
    //    MAX_CPU_TIME_OF_ONE_TX, // ms, 13
    builder.addChainParameter(
        Protocol.ChainParameters.ChainParameter.newBuilder()
            .setKey("getMaxCpuTimeOfOneTx")
            .setValue(dbManager.getDynamicPropertiesStore().getMaxCpuTimeOfOneTx())
            .build());
    //    ALLOW_UPDATE_ACCOUNT_NAME, // 1, 14
    builder.addChainParameter(
        Protocol.ChainParameters.ChainParameter.newBuilder()
            .setKey("getAllowUpdateAccountName")
            .setValue(dbManager.getDynamicPropertiesStore().getAllowUpdateAccountName())
            .build());
    //    ALLOW_SAME_TOKEN_NAME, // 1, 15
    builder.addChainParameter(
        Protocol.ChainParameters.ChainParameter.newBuilder()
            .setKey("getAllowSameTokenName")
            .setValue(dbManager.getDynamicPropertiesStore().getAllowSameTokenName())
            .build());
    //    ALLOW_DELEGATE_RESOURCE, // 0, 16
    builder.addChainParameter(
        Protocol.ChainParameters.ChainParameter.newBuilder()
            .setKey("getAllowDelegateResource")
            .setValue(dbManager.getDynamicPropertiesStore().getAllowDelegateResource())
            .build());
    //    TOTAL_ENERGY_LIMIT, // 50,000,000,000, 17
    builder.addChainParameter(
        Protocol.ChainParameters.ChainParameter.newBuilder()
            .setKey("getTotalEnergyLimit")
            .setValue(dbManager.getDynamicPropertiesStore().getTotalEnergyLimit())
            .build());
    //    ALLOW_TVM_TRANSFER_TRC10, // 1, 18
    builder.addChainParameter(
        Protocol.ChainParameters.ChainParameter.newBuilder()
            .setKey("getAllowTvmTransferTrc10")
            .setValue(dbManager.getDynamicPropertiesStore().getAllowTvmTransferTrc10())
            .build());
    //    TOTAL_CURRENT_ENERGY_LIMIT, // 50,000,000,000, 19
    builder.addChainParameter(
        Protocol.ChainParameters.ChainParameter.newBuilder()
            .setKey("getTotalEnergyCurrentLimit")
            .setValue(dbManager.getDynamicPropertiesStore().getTotalEnergyCurrentLimit())
            .build());
    //    ALLOW_MULTI_SIGN, // 1, 20
    builder.addChainParameter(
        Protocol.ChainParameters.ChainParameter.newBuilder()
            .setKey("getAllowMultiSign")
            .setValue(dbManager.getDynamicPropertiesStore().getAllowMultiSign())
            .build());
    //    ALLOW_ADAPTIVE_ENERGY, // 1, 21
    builder.addChainParameter(
        Protocol.ChainParameters.ChainParameter.newBuilder()
            .setKey("getAllowAdaptiveEnergy")
            .setValue(dbManager.getDynamicPropertiesStore().getAllowAdaptiveEnergy())
            .build());
    //other chainParameters
    builder.addChainParameter(Protocol.ChainParameters.ChainParameter.newBuilder()
        .setKey("getTotalEnergyTargetLimit")
        .setValue(dbManager.getDynamicPropertiesStore().getTotalEnergyTargetLimit())
        .build());

    builder.addChainParameter(Protocol.ChainParameters.ChainParameter.newBuilder()
        .setKey("getTotalEnergyAverageUsage")
        .setValue(dbManager.getDynamicPropertiesStore().getTotalEnergyAverageUsage())
        .build());

    builder.addChainParameter(Protocol.ChainParameters.ChainParameter.newBuilder()
        .setKey("getUpdateAccountPermissionFee")
        .setValue(dbManager.getDynamicPropertiesStore().getUpdateAccountPermissionFee())
        .build());

    builder.addChainParameter(Protocol.ChainParameters.ChainParameter.newBuilder()
        .setKey("getMultiSignFee")
        .setValue(dbManager.getDynamicPropertiesStore().getMultiSignFee())
        .build());

    return builder.build();
  }

  public static String makeUpperCamelMethod(String originName) {
    return "get" + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, originName)
        .replace("_", "");
  }

  public AssetIssueList getAssetIssueList() {
    AssetIssueList.Builder builder = AssetIssueList.newBuilder();

    dbManager.getAssetIssueStoreFinal().getAllAssetIssues()
        .forEach(issueCapsule -> builder.addAssetIssue(issueCapsule.getInstance()));

    return builder.build();
  }


  public AssetIssueList getAssetIssueList(long offset, long limit) {
    AssetIssueList.Builder builder = AssetIssueList.newBuilder();

    List<AssetIssueCapsule> assetIssueList =
        dbManager.getAssetIssueStoreFinal().getAssetIssuesPaginated(offset, limit);

    if (CollectionUtils.isEmpty(assetIssueList)) {
      return null;
    }

    assetIssueList.forEach(issueCapsule -> builder.addAssetIssue(issueCapsule.getInstance()));
    return builder.build();
  }

  public AssetIssueList getAssetIssueByAccount(ByteString accountAddress) {
    if (accountAddress == null || accountAddress.isEmpty()) {
      return null;
    }

    List<AssetIssueCapsule> assetIssueCapsuleList =
        dbManager.getAssetIssueStoreFinal().getAllAssetIssues();

    AssetIssueList.Builder builder = AssetIssueList.newBuilder();
    assetIssueCapsuleList.stream()
        .filter(assetIssueCapsule -> assetIssueCapsule.getOwnerAddress().equals(accountAddress))
        .forEach(issueCapsule -> {
          builder.addAssetIssue(issueCapsule.getInstance());
        });

    return builder.build();
  }

  public AccountNetMessage getAccountNet(ByteString accountAddress) {
    if (accountAddress == null || accountAddress.isEmpty()) {
      return null;
    }
    AccountNetMessage.Builder builder = AccountNetMessage.newBuilder();
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(accountAddress.toByteArray());
    if (accountCapsule == null) {
      return null;
    }

    BandwidthProcessor processor = new BandwidthProcessor(dbManager);
    processor.updateUsage(accountCapsule);

    long netLimit = processor
        .calculateGlobalNetLimit(accountCapsule);
    long freeNetLimit = dbManager.getDynamicPropertiesStore().getFreeNetLimit();
    long totalNetLimit = dbManager.getDynamicPropertiesStore().getTotalNetLimit();
    long totalNetWeight = dbManager.getDynamicPropertiesStore().getTotalNetWeight();

    Map<String, Long> assetNetLimitMap = new HashMap<>();
    Map<String, Long> allFreeAssetNetUsage;
    if (dbManager.getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
      allFreeAssetNetUsage = accountCapsule.getAllFreeAssetNetUsage();
      allFreeAssetNetUsage.keySet().forEach(asset -> {
        byte[] key = ByteArray.fromString(asset);
        assetNetLimitMap
            .put(asset, dbManager.getAssetIssueStore().get(key).getFreeAssetNetLimit());
      });
    } else {
      allFreeAssetNetUsage = accountCapsule.getAllFreeAssetNetUsageV2();
      allFreeAssetNetUsage.keySet().forEach(asset -> {
        byte[] key = ByteArray.fromString(asset);
        assetNetLimitMap
            .put(asset, dbManager.getAssetIssueV2Store().get(key).getFreeAssetNetLimit());
      });
    }

    builder.setFreeNetUsed(accountCapsule.getFreeNetUsage())
        .setFreeNetLimit(freeNetLimit)
        .setNetUsed(accountCapsule.getNetUsage())
        .setNetLimit(netLimit)
        .setTotalNetLimit(totalNetLimit)
        .setTotalNetWeight(totalNetWeight)
        .putAllAssetNetUsed(allFreeAssetNetUsage)
        .putAllAssetNetLimit(assetNetLimitMap);
    return builder.build();
  }

  public AccountResourceMessage getAccountResource(ByteString accountAddress) {
    if (accountAddress == null || accountAddress.isEmpty()) {
      return null;
    }
    AccountResourceMessage.Builder builder = AccountResourceMessage.newBuilder();
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(accountAddress.toByteArray());
    if (accountCapsule == null) {
      return null;
    }

    BandwidthProcessor processor = new BandwidthProcessor(dbManager);
    processor.updateUsage(accountCapsule);

    EnergyProcessor energyProcessor = new EnergyProcessor(dbManager);
    energyProcessor.updateUsage(accountCapsule);

    long netLimit = processor
        .calculateGlobalNetLimit(accountCapsule);
    long freeNetLimit = dbManager.getDynamicPropertiesStore().getFreeNetLimit();
    long totalNetLimit = dbManager.getDynamicPropertiesStore().getTotalNetLimit();
    long totalNetWeight = dbManager.getDynamicPropertiesStore().getTotalNetWeight();
    long energyLimit = energyProcessor
        .calculateGlobalEnergyLimit(accountCapsule);
    long totalEnergyLimit = dbManager.getDynamicPropertiesStore().getTotalEnergyCurrentLimit();
    long totalEnergyWeight = dbManager.getDynamicPropertiesStore().getTotalEnergyWeight();

    long storageLimit = accountCapsule.getAccountResource().getStorageLimit();
    long storageUsage = accountCapsule.getAccountResource().getStorageUsage();

    Map<String, Long> assetNetLimitMap = new HashMap<>();
    Map<String, Long> allFreeAssetNetUsage;
    if (dbManager.getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
      allFreeAssetNetUsage = accountCapsule.getAllFreeAssetNetUsage();
      allFreeAssetNetUsage.keySet().forEach(asset -> {
        byte[] key = ByteArray.fromString(asset);
        assetNetLimitMap
            .put(asset, dbManager.getAssetIssueStore().get(key).getFreeAssetNetLimit());
      });
    } else {
      allFreeAssetNetUsage = accountCapsule.getAllFreeAssetNetUsageV2();
      allFreeAssetNetUsage.keySet().forEach(asset -> {
        byte[] key = ByteArray.fromString(asset);
        assetNetLimitMap
            .put(asset, dbManager.getAssetIssueV2Store().get(key).getFreeAssetNetLimit());
      });
    }

    builder.setFreeNetUsed(accountCapsule.getFreeNetUsage())
        .setFreeNetLimit(freeNetLimit)
        .setNetUsed(accountCapsule.getNetUsage())
        .setNetLimit(netLimit)
        .setTotalNetLimit(totalNetLimit)
        .setTotalNetWeight(totalNetWeight)
        .setEnergyLimit(energyLimit)
        .setEnergyUsed(accountCapsule.getAccountResource().getEnergyUsage())
        .setTotalEnergyLimit(totalEnergyLimit)
        .setTotalEnergyWeight(totalEnergyWeight)
        .setStorageLimit(storageLimit)
        .setStorageUsed(storageUsage)
        .putAllAssetNetUsed(allFreeAssetNetUsage)
        .putAllAssetNetLimit(assetNetLimitMap);
    return builder.build();
  }

  public AssetIssueContract getAssetIssueByName(ByteString assetName)
      throws NonUniqueObjectException {
    if (assetName == null || assetName.isEmpty()) {
      return null;
    }

    if (dbManager.getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
      // fetch from old DB, same as old logic ops
      AssetIssueCapsule assetIssueCapsule =
          dbManager.getAssetIssueStore().get(assetName.toByteArray());
      return assetIssueCapsule != null ? assetIssueCapsule.getInstance() : null;
    } else {
      // get asset issue by name from new DB
      List<AssetIssueCapsule> assetIssueCapsuleList =
          dbManager.getAssetIssueV2Store().getAllAssetIssues();
      AssetIssueList.Builder builder = AssetIssueList.newBuilder();
      assetIssueCapsuleList
          .stream()
          .filter(assetIssueCapsule -> assetIssueCapsule.getName().equals(assetName))
          .forEach(
              issueCapsule -> {
                builder.addAssetIssue(issueCapsule.getInstance());
              });

      // check count
      if (builder.getAssetIssueCount() > 1) {
        throw new NonUniqueObjectException("get more than one asset, please use getassetissuebyid");
      } else {
        // fetch from DB by assetName as id
        AssetIssueCapsule assetIssueCapsule =
            dbManager.getAssetIssueV2Store().get(assetName.toByteArray());

        if (assetIssueCapsule != null) {
          // check already fetch
          if (builder.getAssetIssueCount() > 0
              && builder.getAssetIssue(0).getId().equals(assetIssueCapsule.getInstance().getId())) {
            return assetIssueCapsule.getInstance();
          }

          builder.addAssetIssue(assetIssueCapsule.getInstance());
          // check count
          if (builder.getAssetIssueCount() > 1) {
            throw new NonUniqueObjectException(
                "get more than one asset, please use getassetissuebyid");
          }
        }
      }

      if (builder.getAssetIssueCount() > 0) {
        return builder.getAssetIssue(0);
      } else {
        return null;
      }
    }
  }

  public AssetIssueList getAssetIssueListByName(ByteString assetName) {
    if (assetName == null || assetName.isEmpty()) {
      return null;
    }

    List<AssetIssueCapsule> assetIssueCapsuleList =
        dbManager.getAssetIssueStoreFinal().getAllAssetIssues();

    AssetIssueList.Builder builder = AssetIssueList.newBuilder();
    assetIssueCapsuleList.stream()
        .filter(assetIssueCapsule -> assetIssueCapsule.getName().equals(assetName))
        .forEach(issueCapsule -> {
          builder.addAssetIssue(issueCapsule.getInstance());
        });

    return builder.build();
  }

  public AssetIssueContract getAssetIssueById(String assetId) {
    if (assetId == null || assetId.isEmpty()) {
      return null;
    }
    AssetIssueCapsule assetIssueCapsule = dbManager.getAssetIssueV2Store()
        .get(ByteArray.fromString(assetId));
    return assetIssueCapsule != null ? assetIssueCapsule.getInstance() : null;
  }

  public NumberMessage totalTransaction() {
    NumberMessage.Builder builder = NumberMessage.newBuilder()
        .setNum(dbManager.getTransactionStore().getTotalTransactions());
    return builder.build();
  }

  public NumberMessage getNextMaintenanceTime() {
    NumberMessage.Builder builder = NumberMessage.newBuilder()
        .setNum(dbManager.getDynamicPropertiesStore().getNextMaintenanceTime());
    return builder.build();
  }

  public Block getBlockById(ByteString BlockId) {
    if (Objects.isNull(BlockId)) {
      return null;
    }
    Block block = null;
    try {
      block = dbManager.getBlockStore().get(BlockId.toByteArray()).getInstance();
    } catch (StoreException e) {
    }
    return block;
  }

  public BlockList getBlocksByLimitNext(long number, long limit) {
    if (limit <= 0) {
      return null;
    }
    BlockList.Builder blockListBuilder = BlockList.newBuilder();
    dbManager.getBlockStore().getLimitNumber(number, limit).forEach(
        blockCapsule -> blockListBuilder.addBlock(blockCapsule.getInstance()));
    return blockListBuilder.build();
  }

  public BlockList getBlockByLatestNum(long getNum) {
    BlockList.Builder blockListBuilder = BlockList.newBuilder();
    dbManager.getBlockStore().getBlockByLatestNum(getNum).forEach(
        blockCapsule -> blockListBuilder.addBlock(blockCapsule.getInstance()));
    return blockListBuilder.build();
  }

  public BlockList getZKBlocksByLimitNext(long number, long limit) {
    if (limit <= 0) {
      return null;
    }
    BlockList.Builder blockListBuilder = BlockList.newBuilder();
    dbManager.getBlockStore().getLimitNumber(number, limit).forEach(
        blockCapsule -> blockListBuilder.addBlock(blockCapsule.getZKInstance()));
    return blockListBuilder.build();
  }

  public Transaction getTransactionById(ByteString transactionId) {
    if (Objects.isNull(transactionId)) {
      return null;
    }
    TransactionCapsule transactionCapsule = null;
    try {
      transactionCapsule = dbManager.getTransactionStore()
          .get(transactionId.toByteArray());
    } catch (StoreException e) {
    }
    if (transactionCapsule != null) {
      return transactionCapsule.getInstance();
    }
    return null;
  }

  public TransactionInfo getTransactionInfoById(ByteString transactionId) {
    if (Objects.isNull(transactionId)) {
      return null;
    }
    TransactionInfoCapsule transactionInfoCapsule = null;
    try {
      transactionInfoCapsule = dbManager.getTransactionHistoryStore()
          .get(transactionId.toByteArray());
    } catch (StoreException e) {
    }
    if (transactionInfoCapsule != null) {
      return transactionInfoCapsule.getInstance();
    }
    return null;
  }

  public Proposal getProposalById(ByteString proposalId) {
    if (Objects.isNull(proposalId)) {
      return null;
    }
    ProposalCapsule proposalCapsule = null;
    try {
      proposalCapsule = dbManager.getProposalStore()
          .get(proposalId.toByteArray());
    } catch (StoreException e) {
    }
    if (proposalCapsule != null) {
      return proposalCapsule.getInstance();
    }
    return null;
  }

  public Exchange getExchangeById(ByteString exchangeId) {
    if (Objects.isNull(exchangeId)) {
      return null;
    }
    ExchangeCapsule exchangeCapsule = null;
    try {
      exchangeCapsule = dbManager.getExchangeStoreFinal().get(exchangeId.toByteArray());
    } catch (StoreException e) {
    }
    if (exchangeCapsule != null) {
      return exchangeCapsule.getInstance();
    }
    return null;
  }

  public BytesMessage getNullifier(ByteString id) {
    if (Objects.isNull(id)) {
      return null;
    }
    BytesCapsule trxId = null;
    trxId = dbManager.getNullfierStore().get(id.toByteArray());

    if (trxId != null) {
      return BytesMessage.newBuilder().setValue(ByteString.copyFrom(trxId.getData())).build();
    }
    return null;
  }

  public MerklePath getMerklePath(ByteString rt) {
    if (Objects.isNull(rt)) {
      return null;
    }

    if (!dbManager.getMerkleContainer().merkleRootExist(rt.toByteArray())) {
      return null;
    }

    org.tron.core.zen.merkle.MerklePath merklePath = null;
    try {
      merklePath = dbManager.getMerkleContainer().merklePath(rt.toByteArray());
    } catch (Exception ex) {
      logger.error("get merkle path error, ", ex);
    }

    if (merklePath != null) {
      MerklePath.Builder builder = MerklePath.newBuilder();
      List<List<Boolean>> authenticationPath = merklePath.getAuthenticationPath();
      List<Boolean> index = merklePath.getIndex();
      builder.setRt(ByteString.copyFrom(rt.toByteArray()));
      builder.addAllIndex(index);
      authenticationPath.forEach(
          path ->
              builder.addAuthenticationPaths(
                  AuthenticationPath.newBuilder().addAllValue(path).build()));
      return builder.build();
    }
    return null;
  }

  public byte[] getBestMerkleRoot() {
    IncrementalMerkleTreeContainer lastTree = dbManager.getMerkleContainer().getBestMerkle();
    if (lastTree != null) {
      return lastTree.getRootArray();
    }

    return null;
  }

  public IncrementalMerkleVoucher getMerkleTreeWitness(byte[] hash, int index) {
    if (Objects.isNull(hash) || index < 0) {
      return null;
    }

    IncrementalMerkleVoucherCapsule merkleWitnessCapsule =
        dbManager.getMerkleContainer().getVoucher(hash, index);

    if (merkleWitnessCapsule != null) {
      logger.info("getMerkleTreeWitness");
      merkleWitnessCapsule.resetRt();
      return merkleWitnessCapsule.getInstance();
    }

    return null;
  }

  private long getBlockNumber(OutputPoint outPoint)
      throws ItemNotFoundException, BadItemException,
      InvalidProtocolBufferException {
    ByteString txId = outPoint.getHash();

    //Get blockNum from transactionInfo
    TransactionInfoCapsule transactionInfoCapsule1 = dbManager.getTransactionHistoryStore()
        .get(txId.toByteArray());
    return transactionInfoCapsule1.getBlockNumber();
  }

  //in:outPoint,out:blockNumber
  private IncrementalMerkleVoucherContainer createWitness(OutputPoint outPoint, Long blockNumber)
      throws ItemNotFoundException, BadItemException,
      InvalidProtocolBufferException {
    ByteString txId = outPoint.getHash();

    //Get the tree in blockNum-1 position
    byte[] treeRoot = dbManager.getMerkleTreeIndexStore().get(blockNumber - 1);
    IncrementalMerkleTreeContainer tree = dbManager.getMerkleTreeStore()
        .get(treeRoot).toMerkleTreeContainer();

    //Get the block of blockNum
    BlockCapsule block = dbManager.getBlockByNum(blockNumber);

    IncrementalMerkleVoucherContainer witness = null;

    //get the witness in three parts
    boolean found = false;
    for (Transaction transaction1 : block.getInstance().getTransactionsList()) {

      Contract contract1 = transaction1.getRawData().getContract(0);
      if (contract1.getType() == ContractType.ShieldedTransferContract) {
        ShieldedTransferContract zkContract = contract1.getParameter()
            .unpack(ShieldedTransferContract.class);

        System.out.println("Update existing witness");

        if (new TransactionCapsule(transaction1).getTransactionId().getByteString().equals(txId)) {
          System.out.println("foundTx");
          found = true;
          int index = 0;
          for(org.tron.protos.Contract.ReceiveDescription receiveDescription:
              zkContract.getReceiveDescriptionList()){
            PedersenHashCapsule cmCapsule = new PedersenHashCapsule();
            cmCapsule.setContent(receiveDescription.getNoteCommitment());
            PedersenHash cm = cmCapsule.getInstance();

            if (outPoint.getIndex() < index) {
              tree.append(cm);
            }else if(outPoint.getIndex() == index){
              tree.append(cm);
              witness = tree.getTreeCapsule().deepCopy()
                  .toMerkleTreeContainer().toVoucher();
            }else {
              witness.append(cm);
            }
          }

        } else {
          for(org.tron.protos.Contract.ReceiveDescription receiveDescription:
              zkContract.getReceiveDescriptionList()){
            PedersenHashCapsule cmCapsule = new PedersenHashCapsule();
            cmCapsule.setContent(receiveDescription.getNoteCommitment());
            PedersenHash cm = cmCapsule.getInstance();
            if (witness != null) {
              witness.append(cm);
            }else {
              tree.append(cm);
            }

          }
        }
      }
    }

    if (!found) {
      logger.warn("not found valid cm");
      return null;
    }

    return witness;

  }

  private void updateBothWitness(List<IncrementalMerkleVoucherContainer> witnessList, long large,
      int synBlockNum)
      throws ItemNotFoundException, BadItemException,
      InvalidProtocolBufferException {

    long start = large;
    long end = large + synBlockNum - 1;

    long latestBlockHeaderNumber = dbManager.getDynamicPropertiesStore()
        .getLatestBlockHeaderNumber();

    if (end >= latestBlockHeaderNumber) {
      throw new RuntimeException(
          "synBlockNum is too large, cmBlockNum plus synBlockNum must be less than latestBlockNumber");
    }

    for (long n = start; n <= end; n++) {
      BlockCapsule block = dbManager.getBlockByNum(n);
      for (Transaction transaction1 : block.getInstance().getTransactionsList()) {

        Contract contract1 = transaction1.getRawData().getContract(0);
        if (contract1.getType() == ContractType.ShieldedTransferContract) {

          ShieldedTransferContract zkContract = contract1.getParameter()
              .unpack(ShieldedTransferContract.class);

          PedersenHashCapsule cmCapsule1 = new PedersenHashCapsule();
          cmCapsule1.setContent(zkContract.getReceiveDescriptionList().get(0).getNoteCommitment());
          PedersenHash cm1 = cmCapsule1.getInstance();

          PedersenHashCapsule cmCapsule2 = new PedersenHashCapsule();
          cmCapsule2.setContent(zkContract.getReceiveDescriptionList().get(1).getNoteCommitment());
          PedersenHash cm2 = cmCapsule2.getInstance();

          witnessList.forEach(wit -> {
            wit.append(cm1);
            wit.append(cm2);
          });

        }
      }
    }
  }


  private void updateLowWitness(IncrementalMerkleVoucherContainer witness1, long blockNum1,
      IncrementalMerkleVoucherContainer witness2, long blockNum2)
      throws ItemNotFoundException, BadItemException,
      InvalidProtocolBufferException {
    IncrementalMerkleVoucherContainer witness;
    long start;
    long end;
    if (blockNum1 < blockNum2) {
      start = blockNum1 + 1;
      end = blockNum2;
      witness = witness1;
    } else {
      start = blockNum2 + 1;
      end = blockNum1;
      witness = witness2;
    }

    for (long n = start; n <= end; n++) {
      BlockCapsule block = dbManager.getBlockByNum(n);
      for (Transaction transaction1 : block.getInstance().getTransactionsList()) {

        Contract contract1 = transaction1.getRawData().getContract(0);
        if (contract1.getType() == ContractType.ShieldedTransferContract) {

          ShieldedTransferContract zkContract = contract1.getParameter()
              .unpack(ShieldedTransferContract.class);

          PedersenHashCapsule cmCapsule1 = new PedersenHashCapsule();
          cmCapsule1.setContent(zkContract.getReceiveDescriptionList().get(0).getNoteCommitment());
          PedersenHash cm1 = cmCapsule1.getInstance();

          PedersenHashCapsule cmCapsule2 = new PedersenHashCapsule();
          cmCapsule2.setContent(zkContract.getReceiveDescriptionList().get(1).getNoteCommitment());
          PedersenHash cm2 = cmCapsule2.getInstance();

          witness.append(cm1);
          witness.append(cm2);

        }
      }
    }
  }

  private void validateInput(OutputPointInfo request) throws BadItemException {

    if (request.getBlockNum() < 0) {
      throw new BadItemException("request.getBlockNum() < 0");
    }

    if (!request.hasOutPoint1() && !request.hasOutPoint2()) {
      throw new BadItemException("!request.hasOutPoint1() && !request.hasOutPoint2()");
    }

    if (request.hasOutPoint1()) {
      OutputPoint outPoint1 = request.getOutPoint1();
      if (outPoint1.getHash() == null || outPoint1.getIndex() > 1 || outPoint1.getIndex() < 0) {
        throw new BadItemException(
            "outPoint1.getHash() == null || outPoint1.getIndex() > 1 || outPoint1.getIndex() < 0");
      }
    }
    if (request.hasOutPoint2()) {
      OutputPoint outPoint2 = request.getOutPoint2();

      if (outPoint2.getHash() == null || outPoint2.getIndex() > 1 || outPoint2.getIndex() < 0) {
        throw new BadItemException(
            "outPoint2.getHash() == null || outPoint2.getIndex() > 1 || outPoint2.getIndex() < 0");
      }
    }

  }

  public IncrementalMerkleVoucherInfo getMerkleTreeWitnessInfo(OutputPointInfo request)
      throws ItemNotFoundException, BadItemException,
      InvalidProtocolBufferException {

    validateInput(request);
    IncrementalMerkleVoucherInfo.Builder result = IncrementalMerkleVoucherInfo.newBuilder();
    result.setBlockNum(request.getBlockNum());

    IncrementalMerkleVoucherContainer witness1 = null;
    IncrementalMerkleVoucherContainer witness2 = null;

    int synBlockNum = request.getBlockNum();

    if (request.hasOutPoint1() && request.hasOutPoint2()) {
      OutputPoint outPoint1 = request.getOutPoint1();
      OutputPoint outPoint2 = request.getOutPoint2();

      Long blockNum1 = getBlockNumber(outPoint1);
      Long blockNum2 = getBlockNumber(outPoint2);

      witness1 = createWitness(outPoint1, blockNum1);
      witness2 = createWitness(outPoint2, blockNum2);

      //Skip the next step when the two witness blocks are equal
      if (!blockNum1.equals(blockNum2)) {
        //Get the block between two witness blockNum, [block1+1, block2], update the low witness, make the root the same
        updateLowWitness(witness1, blockNum1, witness2, blockNum2);
      }

      if (synBlockNum != 0) {
        long large = Math.max(blockNum1, blockNum2) + 1;
        //According to the blockNum in the request, obtain the block before [block2+1, blockNum], and update the two witnesses.
        List<IncrementalMerkleVoucherContainer> list = new ArrayList<>();
        list.add(witness1);
        list.add(witness2);
        updateBothWitness(list, large, synBlockNum);
      }
    }

    if (request.hasOutPoint1() && !request.hasOutPoint2()) {
      OutputPoint outPoint1 = request.getOutPoint1();

      Long blockNum1 = getBlockNumber(outPoint1);

      witness1 = createWitness(outPoint1, blockNum1);

      if (synBlockNum != 0) {
        //According to the blockNum in the request, obtain the block before [block2+1, blockNum], and update the two witnesses.
        List<IncrementalMerkleVoucherContainer> list = new ArrayList<>();
        list.add(witness1);
        updateBothWitness(list, blockNum1, synBlockNum);
      }
    }

    if (!request.hasOutPoint1() && request.hasOutPoint2()) {
      OutputPoint outPoint2 = request.getOutPoint2();

      Long blockNum2 = getBlockNumber(outPoint2);

      witness2 = createWitness(outPoint2, blockNum2);

      if (synBlockNum != 0) {
        //According to the blockNum in the request, obtain the block before [block2+1, blockNum], and update the two witnesses.
        List<IncrementalMerkleVoucherContainer> list = new ArrayList<>();
        list.add(witness2);
        updateBothWitness(list, blockNum2, synBlockNum);
      }
    }

    if (witness1 != null) {
      witness1.getVoucherCapsule().resetRt();
      result.setVoucher1(witness1.getVoucherCapsule().getInstance());
      result.setPath1(ByteString.copyFrom(witness1.path().encode()));
    }

    if (witness2 != null) {
      witness2.getVoucherCapsule().resetRt();
      result.setVoucher2(witness2.getVoucherCapsule().getInstance());
      result.setPath2(ByteString.copyFrom(witness2.path().encode()));
    }

    return result.build();
  }

  public IncrementalMerkleTree getMerkleTreeOfBlock(long blockNum) {
    if (blockNum < 0) {
      return null;
    }

    try {
      if (dbManager.getMerkleTreeIndexStore().has(ByteArray.fromLong(blockNum))) {
        return IncrementalMerkleTree.parseFrom(dbManager.getMerkleTreeIndexStore().get(blockNum));
      }
    } catch (Exception ex) {
      return null;
    }
    return null;
  }

  public long getShieldedTransactionFee() {
    return dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
  }

  public TransactionCapsule createShieldedTransaction(PrivateParameters request)
      throws ContractValidateException, RuntimeException {
    ZenTransactionBuilder builder = new ZenTransactionBuilder(this);

    byte[] transparentFromAddress = request.getTransparentFromAddress().toByteArray();
    byte[] ask = request.getAsk().toByteArray();
    byte[] nsk = request.getNsk().toByteArray();
    byte[] ovk = request.getOvk().toByteArray();

    if (ArrayUtils.isEmpty(transparentFromAddress) && (ArrayUtils.isEmpty(ask) || ArrayUtils
        .isEmpty(nsk) || ArrayUtils.isEmpty(ovk))) {
      throw new ContractValidateException("No input address");
    }

    long fromAmount = request.getFromAmount();
    if (!ArrayUtils.isEmpty(transparentFromAddress) && fromAmount <= 0) {
      throw new ContractValidateException("Input amount must > 0");
    }

    List<SpendNote> shieldedSpends = request.getShieldedSpendsList();
    if (!(ArrayUtils.isEmpty(ask) || ArrayUtils.isEmpty(nsk) || ArrayUtils.isEmpty(ovk))
        && shieldedSpends.isEmpty()) {
      throw new ContractValidateException("No input note");
    }

    List<ReceiveNote> shieldedReceives = request.getShieldedReceivesList();
    byte[] transparentToAddress = request.getTransparentToAddress().toByteArray();
    if (shieldedReceives.isEmpty() && ArrayUtils.isEmpty(transparentToAddress)) {
      throw new ContractValidateException("No output address");
    }

    long toAmount = request.getToAmount();
    if (!ArrayUtils.isEmpty(transparentToAddress) && toAmount <= 0) {
      throw new ContractValidateException("Output amount must > 0");
    }

    // add
    if (!ArrayUtils.isEmpty(transparentFromAddress)) {
      builder.setTransparentInput(transparentFromAddress, fromAmount);
    }

    if (!ArrayUtils.isEmpty(transparentToAddress)) {
      builder.setTransparentOutput(transparentToAddress, toAmount);
    }

    // sapling input
    if (!(ArrayUtils.isEmpty(ask) || ArrayUtils.isEmpty(nsk) || ArrayUtils.isEmpty(ovk))) {
      ExpandedSpendingKey expsk = new ExpandedSpendingKey(ask, nsk, ovk);
      for (SpendNote spendNote : shieldedSpends) {
        IncrementalMerkleTreeCapsule merkleTreeCapsule = dbManager.getMerkleContainer()
            .getMerkleTree(spendNote.getAnchor().toByteArray());

        GrpcAPI.Note note = spendNote.getNote();
        BaseNote.Note baseNote = new BaseNote.Note(new DiversifierT(note.getD().toByteArray()),
            note.getPkD().toByteArray(), note.getValue(), note.getRcm().toByteArray());

        builder.addSaplingSpend(expsk, baseNote, spendNote.getAlpha().toByteArray(),
            spendNote.getAnchor().toByteArray(),
            merkleTreeCapsule.toMerkleTreeContainer().toVoucher());
      }
    }

    // sapling output
    for (ReceiveNote receiveNote : shieldedReceives) {
      DiversifierT diversifierT = new DiversifierT(receiveNote.getNote().getD().toByteArray());
      builder.addSaplingOutput(ovk,
          diversifierT,
          receiveNote.getNote().getPkD().toByteArray(),
          receiveNote.getNote().getValue(),
          receiveNote.getNote().getRcm().toByteArray(),
          new byte[512]);
    }

    TransactionCapsule transactionCapsule = null;
    try {
      transactionCapsule = builder.build();
    } catch (RuntimeException e) {
      logger.error("createShieldedTransaction except, error is " + e.toString());
    }
    return transactionCapsule;

  }

  public BytesMessage getSpendingKey() {
    byte[] sk;
    try {
      sk = SpendingKey.random().getValue();
      return BytesMessage.newBuilder().setValue(ByteString.copyFrom(sk)).build();
    } catch (Exception e) {
      return null;
    }
  }

  public ExpandedSpendingKeyMessage getExpandedSpendingKey(ByteString spendingKey) {
    if (Objects.isNull(spendingKey)) {
      return null;
    }

    ExpandedSpendingKey expandedSpendingKey = null;
    try {
      SpendingKey sk = new SpendingKey(spendingKey.toByteArray());
      expandedSpendingKey = sk.expandedSpendingKey();

      ExpandedSpendingKeyMessage.Builder responseBuild = ExpandedSpendingKeyMessage.newBuilder();
      responseBuild.setAsk(ByteString.copyFrom(expandedSpendingKey.getAsk()))
          .setNsk(ByteString.copyFrom(expandedSpendingKey.getNsk()))
          .setOvk(ByteString.copyFrom(expandedSpendingKey.getOvk()));

      System.out.println(
          "sk.expandedSpendingKey() is: " + ByteUtil.toHexString(expandedSpendingKey.encode()));
      System.out.println("ask hexstring is: " + ByteUtil.toHexString(expandedSpendingKey.getAsk()));
      System.out.println("nsk hexstring is: " + ByteUtil.toHexString(expandedSpendingKey.getNsk()));

      return responseBuild.build();
    } catch (Exception e) {
      return null;
    }
  }

  public BytesMessage getAkFromAsk(ByteString ask) {
    if (Objects.isNull(ask)) {
      return null;
    }

    byte[] ak;
    try {
      ak = ExpandedSpendingKey.getAkFromAsk(ask.toByteArray());
      return BytesMessage.newBuilder().setValue(ByteString.copyFrom(ak)).build();
    } catch (Exception e) {
      return null;
    }
  }

  public BytesMessage getNkFromNsk(ByteString nsk) {
    if (Objects.isNull(nsk)) {
      return null;
    }

    byte[] nk;
    try {
      nk = ExpandedSpendingKey.getNkFromNsk(nsk.toByteArray());
      return BytesMessage.newBuilder().setValue(ByteString.copyFrom(nk)).build();
    } catch (Exception e) {
      return null;
    }
  }

  public IncomingViewingKeyMessage getIncomingViewingKey(byte[] ak, byte[] nk) {

    if (ak.length != 32 || nk.length != 32) {
      return null;
    }

    byte[] ivk = new byte[32]; // the incoming viewing key
    Librustzcash.librustzcashCrhIvk(ak, nk, ivk);
    return IncomingViewingKeyMessage.newBuilder()
        .setIvk(ByteString.copyFrom(ivk))
        .build();
  }

  public DiversifierMessage getDiversifier() {
    byte[] d;
    while (true) {
      d = org.tron.keystore.Wallet.generateRandomBytes(Constant.ZC_DIVERSIFIER_SIZE);
      if (Librustzcash.librustzcashCheckDiversifier(d)) {
        break;
      }
    }
    DiversifierMessage diversifierMessage = DiversifierMessage.newBuilder()
        .setD(ByteString.copyFrom(d))
        .build();

    return diversifierMessage;
  }

  public BytesMessage getRcm() {
    byte[] rcm;
    try {
      rcm = BaseNote.Note.generateR();
      return BytesMessage.newBuilder().setValue(ByteString.copyFrom(rcm)).build();
    } catch (Exception e) {
      return null;
    }
  }

  public SaplingPaymentAddressMessage getSaplingPaymentAddress(IncomingViewingKey ivk,
      DiversifierT d) {
    //get pk_d from paymentAddress
    SaplingPaymentAddressMessage spa = null;

    if (!Librustzcash.librustzcashCheckDiversifier(d.getData())) {
      return null;
    }

    Optional<PaymentAddress> op = ivk.address(d);
    if (op.isPresent()) {
      DiversifierMessage ds = DiversifierMessage.newBuilder()
          .setD(ByteString.copyFrom(d.getData()))
          .build();

      spa = SaplingPaymentAddressMessage.newBuilder()
          .setD(ds)
          .setPkD(ByteString.copyFrom(op.get().getPkD()))
          .build();

    }
    return spa;
  }

  public NodeList listNodes() {
    List<NodeHandler> handlerList = nodeManager.dumpActiveNodes();

    Map<String, NodeHandler> nodeHandlerMap = new HashMap<>();
    for (NodeHandler handler : handlerList) {
      String key = handler.getNode().getHexId() + handler.getNode().getHost();
      nodeHandlerMap.put(key, handler);
    }

    NodeList.Builder nodeListBuilder = NodeList.newBuilder();

    nodeHandlerMap.entrySet().stream()
        .forEach(v -> {
          org.tron.common.overlay.discover.node.Node node = v.getValue().getNode();
          nodeListBuilder.addNodes(Node.newBuilder().setAddress(
              Address.newBuilder()
                  .setHost(ByteString.copyFrom(ByteArray.fromString(node.getHost())))
                  .setPort(node.getPort())));
        });
    return nodeListBuilder.build();
  }

  public Transaction deployContract(CreateSmartContract createSmartContract,
      TransactionCapsule trxCap) {

    // do nothing, so can add some useful function later
    // trxcap contract para cacheUnpackValue has value
    return trxCap.getInstance();
  }

  public Transaction triggerContract(TriggerSmartContract triggerSmartContract,
      TransactionCapsule trxCap, Builder builder,
      Return.Builder retBuilder)
      throws ContractValidateException, ContractExeException, HeaderNotFound, VMIllegalException {

    ContractStore contractStore = dbManager.getContractStore();
    byte[] contractAddress = triggerSmartContract.getContractAddress().toByteArray();
    SmartContract.ABI abi = contractStore.getABI(contractAddress);
    if (abi == null) {
      throw new ContractValidateException("No contract or not a smart contract");
    }

    byte[] selector = getSelector(triggerSmartContract.getData().toByteArray());

    if (!isConstant(abi, selector)) {
      return trxCap.getInstance();
    } else {
      if (!Args.getInstance().isSupportConstant()) {
        throw new ContractValidateException("this node don't support constant");
      }
      DepositImpl deposit = DepositImpl.createRoot(dbManager);

      Block headBlock;
      List<BlockCapsule> blockCapsuleList = dbManager.getBlockStore().getBlockByLatestNum(1);
      if (CollectionUtils.isEmpty(blockCapsuleList)) {
        throw new HeaderNotFound("latest block not found");
      } else {
        headBlock = blockCapsuleList.get(0).getInstance();
      }

      Runtime runtime = new RuntimeImpl(trxCap.getInstance(), new BlockCapsule(headBlock), deposit,
          new ProgramInvokeFactoryImpl(), true);
      VMConfig.initVmHardFork();
      VMConfig.initAllowTvmTransferTrc10(
          dbManager.getDynamicPropertiesStore().getAllowTvmTransferTrc10());
      VMConfig.initAllowMultiSign(dbManager.getDynamicPropertiesStore().getAllowMultiSign());
      runtime.execute();
      runtime.go();
      runtime.finalization();
      // TODO exception
      if (runtime.getResult().getException() != null) {
        RuntimeException e = runtime.getResult().getException();
        logger.warn("Constant call has error {}", e.getMessage());
        throw e;
      }

      ProgramResult result = runtime.getResult();
      TransactionResultCapsule ret = new TransactionResultCapsule();

      builder.addConstantResult(ByteString.copyFrom(result.getHReturn()));
      ret.setStatus(0, code.SUCESS);
      if (StringUtils.isNoneEmpty(runtime.getRuntimeError())) {
        ret.setStatus(0, code.FAILED);
        retBuilder.setMessage(ByteString.copyFromUtf8(runtime.getRuntimeError())).build();
      }
      trxCap.setResult(ret);
      return trxCap.getInstance();
    }
  }

  public SmartContract getContract(GrpcAPI.BytesMessage bytesMessage) {
    byte[] address = bytesMessage.getValue().toByteArray();
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(address);
    if (accountCapsule == null) {
      logger.error(
          "Get contract failed, the account is not exist or the account does not have code hash!");
      return null;
    }

    ContractCapsule contractCapsule = dbManager.getContractStore()
        .get(bytesMessage.getValue().toByteArray());
    if (Objects.nonNull(contractCapsule)) {
      return contractCapsule.getInstance();
    }
    return null;
  }


  private static byte[] getSelector(byte[] data) {
    if (data == null ||
        data.length < 4) {
      return null;
    }

    byte[] ret = new byte[4];
    System.arraycopy(data, 0, ret, 0, 4);
    return ret;
  }

  private static boolean isConstant(SmartContract.ABI abi, byte[] selector) {

    if (selector == null || selector.length != 4 || abi.getEntrysList().size() == 0) {
      return false;
    }

    for (int i = 0; i < abi.getEntrysCount(); i++) {
      ABI.Entry entry = abi.getEntrys(i);
      if (entry.getType() != ABI.Entry.EntryType.Function) {
        continue;
      }

      int inputCount = entry.getInputsCount();
      StringBuffer sb = new StringBuffer();
      sb.append(entry.getName());
      sb.append("(");
      for (int k = 0; k < inputCount; k++) {
        ABI.Entry.Param param = entry.getInputs(k);
        sb.append(param.getType());
        if (k + 1 < inputCount) {
          sb.append(",");
        }
      }
      sb.append(")");

      byte[] funcSelector = new byte[4];
      System.arraycopy(Hash.sha3(sb.toString().getBytes()), 0, funcSelector, 0, 4);
      if (Arrays.equals(funcSelector, selector)) {
        if (entry.getConstant() == true || entry.getStateMutability()
            .equals(StateMutabilityType.View)) {
          return true;
        } else {
          return false;
        }
      }
    }

    return false;
  }

  /*
  input
  offset:100,limit:10
  return
  id: 101~110
   */
  public ProposalList getPaginatedProposalList(long offset, long limit) {

    if (limit < 0 || offset < 0) {
      return null;
    }

    long latestProposalNum = dbManager.getDynamicPropertiesStore().getLatestProposalNum();
    if (latestProposalNum <= offset) {
      return null;
    }
    limit = limit > PROPOSAL_COUNT_LIMIT_MAX ? PROPOSAL_COUNT_LIMIT_MAX : limit;
    long end = offset + limit;
    end = end > latestProposalNum ? latestProposalNum : end;
    ProposalList.Builder builder = ProposalList.newBuilder();

    ImmutableList<Long> rangeList = ContiguousSet
        .create(Range.openClosed(offset, end), DiscreteDomain.longs()).asList();
    rangeList.stream().map(ProposalCapsule::calculateDbKey).map(key -> {
      try {
        return dbManager.getProposalStore().get(key);
      } catch (Exception ex) {
        return null;
      }
    }).filter(Objects::nonNull)
        .forEach(proposalCapsule -> builder.addProposals(proposalCapsule.getInstance()));
    return builder.build();
  }

  public ExchangeList getPaginatedExchangeList(long offset, long limit) {

    if (limit < 0 || offset < 0) {
      return null;
    }

    long latestExchangeNum = dbManager.getDynamicPropertiesStore().getLatestExchangeNum();
    if (latestExchangeNum <= offset) {
      return null;
    }
    limit = limit > EXCHANGE_COUNT_LIMIT_MAX ? EXCHANGE_COUNT_LIMIT_MAX : limit;
    long end = offset + limit;
    end = end > latestExchangeNum ? latestExchangeNum : end;

    ExchangeList.Builder builder = ExchangeList.newBuilder();
    ImmutableList<Long> rangeList = ContiguousSet
        .create(Range.openClosed(offset, end), DiscreteDomain.longs()).asList();
    rangeList.stream().map(ExchangeCapsule::calculateDbKey).map(key -> {
      try {
        return dbManager.getExchangeStoreFinal().get(key);
      } catch (Exception ex) {
        return null;
      }
    }).filter(Objects::nonNull)
        .forEach(exchangeCapsule -> builder.addExchanges(exchangeCapsule.getInstance()));
    return builder.build();

  }

  //for shield transactions

  public String getNewZenAddress() {
    //seed
    //AccountCounter

    // Create new metadata
    long nCreationTime = System.currentTimeMillis();
    CKeyMetadata metadata = new CKeyMetadata(nCreationTime);

    // Try to get the seed
    HDSeed seed = KeyStore.seed;
    // init data for test
    seed.random(32);

    if (seed == null) {
      throw new RuntimeException("CWallet::GenerateNewSaplingZKey(): HD seed not found");
    }

    ExtendedSpendingKey master = ExtendedSpendingKey.Master(seed);
    int bip44CoinType = ZkChainParams.BIP44CoinType;

    // We use a fixed keypath scheme of m/32'/coin_type'/account'
    // Derive m/32'
    ExtendedSpendingKey master32h = master.Derive(32 | ZIP32_HARDENED_KEY_LIMIT);
    // Derive m/32'/coin_type'
    ExtendedSpendingKey master32hCth = master32h.Derive(bip44CoinType | ZIP32_HARDENED_KEY_LIMIT);

    System.out.println("depth=" + ByteArray.toInt(master32hCth.getDepth()));
    System.out.println("depth=" + ByteArray.toStr(master32hCth.getParentFVKTag()));
    System.out.println("depth=" + ByteArray.toInt(master32hCth.getChildIndex()));

    // Derive account key at next index, skip keys already known to the wallet
    ExtendedSpendingKey xsk = null;

    while (xsk == null || KeyStore.haveSpendingKey(xsk.getExpsk().fullViewingKey())) {
      //
      xsk = master32hCth.Derive(HdChain.saplingAccountCounter | ZIP32_HARDENED_KEY_LIMIT);
      metadata.hdKeyPath = "m/32'/" + bip44CoinType + "'/" + HdChain.saplingAccountCounter + "'";
      metadata.seedFp = HdChain.seedFp;
      // Increment childkey index
      HdChain.saplingAccountCounter++;
    }

    // Update the chain model in the database
//    if (fFileBacked && !CWalletDB(strWalletFile).WriteHDChain(hdChain))
//      throw new RuntimeException("CWallet::GenerateNewSaplingZKey(): Writing HD chain model failed");

    IncomingViewingKey ivk = xsk.getExpsk().fullViewingKey().inViewingKey();
    ZenWallet.mapSaplingZKeyMetadata.put(ivk, metadata);

    PaymentAddress addr = xsk.DefaultAddress();
    if (!ZenWallet.AddSaplingZKey(xsk, addr)) {
      throw new RuntimeException("CWallet::GenerateNewSaplingZKey(): AddSaplingZKey failed");
    }

    // return default sapling payment address.
    String paymentAddress = KeyIo.EncodePaymentAddress(addr);
    System.out.println("paymentAddress = " + paymentAddress);
    return paymentAddress;
  }

  public void sendZenCoinShield(String[] params) {
    String fromAddr = params[0];
    List<Recipient> outputs = new ArrayList<>();

    ZenTransactionBuilderFactory constructor =
        new ZenTransactionBuilderFactory(fromAddr, outputs);
    TransactionCapsule result = constructor.build();
//    broadcastTX();
  }


  public long getBalanceZenAddress(String address, int minDepth, boolean requireSpendingKey) {
    long balance = 0;
    List<NoteEntry> saplingEntries;

    PaymentAddress filterAddresses = null;// TODO can be null
    if (address.length() > 0) {
      filterAddresses = KeyIo.decodePaymentAddress(address);
    }

    saplingEntries = ZenWallet.GetFilteredNotes(filterAddresses, true, requireSpendingKey);
    for (NoteEntry entry : saplingEntries) {
      balance += entry.note.value;
    }

    return balance;
  }

  /*
   * try to get cm belongs to ivk
   */
  public GrpcAPI.DecryptNotes scanNoteByIvk(long startNum, long endNum, byte[] ivk) {

    GrpcAPI.DecryptNotes.Builder builder = GrpcAPI.DecryptNotes.newBuilder();

    if (!(endNum > 0 && endNum > startNum)) {
      return builder.build();
    }

    BlockList blockList = this.getBlocksByLimitNext(startNum, endNum - startNum);

    blockList.getBlockList().forEach(block -> {

      for (Transaction transaction : block.getTransactionsList()) {

        TransactionCapsule transactionCapsule = new TransactionCapsule(transaction);
        byte[] txid = transactionCapsule.getTransactionId().getBytes();

        List<org.tron.protos.Protocol.Transaction.Contract> contracts = transaction.getRawData()
            .getContractList();
        if (contracts.size() == 0) {
          continue;
        }

        org.tron.protos.Protocol.Transaction.Contract c = contracts.get(0);

        if (c.getType() != Protocol.Transaction.Contract.ContractType.ShieldedTransferContract) {
          continue;
        }

        org.tron.protos.Contract.ShieldedTransferContract stContract = null;
        try {
          stContract = c.getParameter()
              .unpack(org.tron.protos.Contract.ShieldedTransferContract.class);
        } catch (InvalidProtocolBufferException e) {
          throw new RuntimeException("unpack ShieldedTransferContract failed.");
        }

        for (int index = 0; index < stContract.getReceiveDescriptionList().size(); index++) {
          org.tron.protos.Contract.ReceiveDescription r = stContract.getReceiveDescription(index);

          Optional<BaseNotePlaintext.NotePlaintext> notePlaintext = BaseNotePlaintext.NotePlaintext
              .decrypt(
                  r.getCEnc().toByteArray(),//ciphertext
                  ivk,
                  r.getEpk().toByteArray(),//epk
                  r.getNoteCommitment().toByteArray() //cmu
              );

          if (notePlaintext.isPresent()) {
            BaseNotePlaintext.NotePlaintext noteText = notePlaintext.get();

            byte[] pk_d = new byte[32];
            if (!Librustzcash.librustzcashIvkToPkd(ivk, noteText.d.getData(), pk_d)) {
              continue;
            }

            Note note = Note.newBuilder()
                .setD(ByteString.copyFrom(noteText.d.getData()))
                .setValue(noteText.value)
                .setRcm(ByteString.copyFrom(noteText.rcm))
                .setPkD(ByteString.copyFrom(pk_d))
                .build();
            DecryptNotes.NoteTx noteTx = DecryptNotes.NoteTx.newBuilder()
                .setNote(note)
                .setTxid(ByteString.copyFrom(txid))
                .setIndex(index)
                .build();

            builder.addNoteTxs(noteTx);
          }
        } // end of ReceiveDescriptionList

      } // end of transaction

    }); //end of blocklist

    GrpcAPI.DecryptNotes decryptNotes = builder.build();
    System.out.println("decryptNotes size: " + decryptNotes.getNoteTxsList().size());
    return decryptNotes;
  }

  /*
   * try to get cm belongs to ovk
   */
  public GrpcAPI.DecryptNotes scanNoteByOvk(long startNum, long endNum, byte[] ovk) {

    GrpcAPI.DecryptNotes.Builder builder = GrpcAPI.DecryptNotes.newBuilder();

    if (!(endNum > 0 && endNum > startNum)) {
      return builder.build();
    }

    BlockList blockList = this.getBlocksByLimitNext(startNum, endNum - startNum);

    blockList.getBlockList().forEach(block -> {

      for (Transaction transaction : block.getTransactionsList()) {

        TransactionCapsule transactionCapsule = new TransactionCapsule(transaction);
        byte[] txid = transactionCapsule.getTransactionId().getBytes();

        List<org.tron.protos.Protocol.Transaction.Contract> contracts = transaction.getRawData()
            .getContractList();
        if (contracts.size() == 0) {
          continue;
        }

        org.tron.protos.Protocol.Transaction.Contract c = contracts.get(0);

        if (c.getType() != Protocol.Transaction.Contract.ContractType.ShieldedTransferContract) {
          continue;
        }

        org.tron.protos.Contract.ShieldedTransferContract stContract = null;
        try {
          stContract = c.getParameter()
              .unpack(org.tron.protos.Contract.ShieldedTransferContract.class);
        } catch (InvalidProtocolBufferException e) {
          throw new RuntimeException("unpack ShieldedTransferContract failed.");
        }

        for (int index = 0; index < stContract.getReceiveDescriptionList().size(); index++) {
          org.tron.protos.Contract.ReceiveDescription r = stContract.getReceiveDescription(index);

          NoteEncryption.OutCiphertext c_out = new NoteEncryption.OutCiphertext();
          c_out.data = r.getCOut().toByteArray();

          Optional<SaplingOutgoingPlaintext> notePlaintext = SaplingOutgoingPlaintext
              .decrypt(c_out,//ciphertext
                  ovk,
                  r.getValueCommitment().toByteArray(), //cv
                  r.getNoteCommitment().toByteArray(), //cmu
                  r.getEpk().toByteArray() //epk
              );

          if (notePlaintext.isPresent()) {

            SaplingOutgoingPlaintext decrypted_out_ct_unwrapped = notePlaintext.get();

            //decode c_enc with pkd、esk
            NoteEncryption.EncCiphertext ciphertext = new NoteEncryption.EncCiphertext();
            ciphertext.data = r.getCEnc().toByteArray();

            Optional<BaseNotePlaintext.NotePlaintext> foo = BaseNotePlaintext.NotePlaintext
                .decrypt(ciphertext,
                    r.getEpk().toByteArray(),
                    decrypted_out_ct_unwrapped.esk,
                    decrypted_out_ct_unwrapped.pk_d,
                    r.getNoteCommitment().toByteArray());

            if (foo.isPresent()) {

              BaseNotePlaintext.NotePlaintext bar = foo.get();

              Note note = Note.newBuilder()
                  .setD(ByteString.copyFrom(bar.d.getData()))
                  .setValue(bar.value)
                  .setRcm(ByteString.copyFrom(bar.rcm))
                  .setPkD(ByteString.copyFrom(decrypted_out_ct_unwrapped.pk_d))
                  .build();

              DecryptNotes.NoteTx noteTx = DecryptNotes.NoteTx.newBuilder()
                  .setNote(note)
                  .setTxid(ByteString.copyFrom(txid))
                  .setIndex(index)
                  .build();

              builder.addNoteTxs(noteTx);
            }
          }

        } // end of ReceiveDescriptionList

      } // end of transaction

    }); //end of blocklist

    return builder.build();
  }
}
