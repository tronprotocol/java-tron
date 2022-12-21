package org.tron.core.vm;

import static org.tron.common.crypto.Hash.sha3;
import static org.tron.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.LogInfo;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Program;
import org.tron.core.vm.program.Stack;

public class OperationActions {

  private static final BigInteger _32_ = BigInteger.valueOf(32);

  public static void stopAction(Program program) {
    program.setHReturn(EMPTY_BYTE_ARRAY);
    program.stop();
  }

  public static void addAction(Program program) {
    DataWord word1 = program.stackPop();
    DataWord word2 = program.stackPop();

    word1.add(word2);
    program.stackPush(word1);
    program.step();
  }

  public static void mulAction(Program program) {
    DataWord word1 = program.stackPop();
    DataWord word2 = program.stackPop();

    word1.mul(word2);
    program.stackPush(word1);
    program.step();
  }

  public static void subAction(Program program) {
    DataWord word1 = program.stackPop();
    DataWord word2 = program.stackPop();

    word1.sub(word2);
    program.stackPush(word1);
    program.step();
  }

  public static void divAction(Program program) {
    DataWord word1 = program.stackPop();
    DataWord word2 = program.stackPop();

    word1.div(word2);
    program.stackPush(word1);
    program.step();
  }

  public static void sdivAction(Program program) {
    DataWord word1 = program.stackPop();
    DataWord word2 = program.stackPop();

    word1.sDiv(word2);
    program.stackPush(word1);
    program.step();
  }

  public static void modAction(Program program) {
    DataWord word1 = program.stackPop();
    DataWord word2 = program.stackPop();

    word1.mod(word2);
    program.stackPush(word1);
    program.step();
  }

  public static void sModAction(Program program) {
    DataWord word1 = program.stackPop();
    DataWord word2 = program.stackPop();

    word1.sMod(word2);
    program.stackPush(word1);
    program.step();
  }

  public static void addModAction(Program program) {
    DataWord word1 = program.stackPop();
    DataWord word2 = program.stackPop();
    DataWord word3 = program.stackPop();

    word1.addmod(word2, word3);
    program.stackPush(word1);
    program.step();
  }

  public static void mulModAction(Program program) {
    DataWord word1 = program.stackPop();
    DataWord word2 = program.stackPop();
    DataWord word3 = program.stackPop();

    word1.mulmod(word2, word3);
    program.stackPush(word1);
    program.step();
  }

  public static void expAction(Program program) {
    DataWord word1 = program.stackPop();
    DataWord word2 = program.stackPop();

    word1.exp(word2);
    program.stackPush(word1);
    program.step();
  }

  public static void signExtendAction(Program program) {
    DataWord word1 = program.stackPop();
    BigInteger k = word1.value();

    if (k.compareTo(_32_) < 0) {
      DataWord word2 = program.stackPop();
      word2.signExtend(k.byteValue());
      program.stackPush(word2);
    }
    program.step();
  }

  public static void ltAction(Program program) {
    DataWord word1 = program.stackPop();
    DataWord word2 = program.stackPop();

    if (word1.value().compareTo(word2.value()) < 0) {
      word1.and(DataWord.ZERO);
      word1.getData()[31] = 1;
    } else {
      word1.and(DataWord.ZERO);
    }
    program.stackPush(word1);
    program.step();
  }

  public static void gtAction(Program program) {
    DataWord word1 = program.stackPop();
    DataWord word2 = program.stackPop();

    if (word1.value().compareTo(word2.value()) > 0) {
      word1.and(DataWord.ZERO);
      word1.getData()[31] = 1;
    } else {
      word1.and(DataWord.ZERO);
    }
    program.stackPush(word1);
    program.step();
  }

  public static void sltAction(Program program) {
    DataWord word1 = program.stackPop();
    DataWord word2 = program.stackPop();

    if (word1.sValue().compareTo(word2.sValue()) < 0) {
      word1.and(DataWord.ZERO);
      word1.getData()[31] = 1;
    } else {
      word1.and(DataWord.ZERO);
    }
    program.stackPush(word1);
    program.step();
  }

  public static void sgtAction(Program program) {
    DataWord word1 = program.stackPop();
    DataWord word2 = program.stackPop();

    if (word1.sValue().compareTo(word2.sValue()) > 0) {
      word1.and(DataWord.ZERO);
      word1.getData()[31] = 1;
    } else {
      word1.and(DataWord.ZERO);
    }
    program.stackPush(word1);
    program.step();
  }

  public static void eqAction(Program program) {
    DataWord word1 = program.stackPop();
    DataWord word2 = program.stackPop();

    if (word1.xor(word2).isZero()) {
      word1.and(DataWord.ZERO);
      word1.getData()[31] = 1;
    } else {
      word1.and(DataWord.ZERO);
    }
    program.stackPush(word1);
    program.step();
  }

  public static void isZeroAction(Program program) {
    DataWord word1 = program.stackPop();
    if (word1.isZero()) {
      word1.getData()[31] = 1;
    } else {
      word1.and(DataWord.ZERO);
    }

    program.stackPush(word1);
    program.step();
  }

  public static void andAction(Program program) {
    DataWord word1 = program.stackPop();
    DataWord word2 = program.stackPop();

    word1.and(word2);
    program.stackPush(word1);
    program.step();
  }

  public static void orAction(Program program) {
    DataWord word1 = program.stackPop();
    DataWord word2 = program.stackPop();

    word1.or(word2);
    program.stackPush(word1);
    program.step();
  }

  public static void xorAction(Program program) {
    DataWord word1 = program.stackPop();
    DataWord word2 = program.stackPop();

    word1.xor(word2);
    program.stackPush(word1);
    program.step();
  }

  public static void notAction(Program program) {
    DataWord word1 = program.stackPop();
    word1.bnot();

    program.stackPush(word1);
    program.step();
  }

  public static void byteAction(Program program) {
    DataWord word1 = program.stackPop();
    DataWord word2 = program.stackPop();

    final DataWord result;
    if (word1.value().compareTo(_32_) < 0) {
      byte tmp = word2.getData()[word1.intValue()];
      word2.and(DataWord.ZERO);
      word2.getData()[31] = tmp;
      result = word2;
    } else {
      result = new DataWord();
    }

    program.stackPush(result);
    program.step();
  }

  public static void shlAction(Program program) {
    DataWord word1 = program.stackPop();
    DataWord word2 = program.stackPop();

    final DataWord result = word2.shiftLeft(word1);
    program.stackPush(result);
    program.step();
  }

  public static void shrAction(Program program) {
    DataWord word1 = program.stackPop();
    DataWord word2 = program.stackPop();

    final DataWord result = word2.shiftRight(word1);
    program.stackPush(result);
    program.step();
  }

  public static void sarAction(Program program) {
    DataWord word1 = program.stackPop();
    DataWord word2 = program.stackPop();

    final DataWord result = word2.shiftRightSigned(word1);
    program.stackPush(result);
    program.step();
  }

  public static void sha3Action(Program program) {
    DataWord memOffsetData = program.stackPop();
    DataWord lengthData = program.stackPop();
    byte[] buffer = program
        .memoryChunk(memOffsetData.intValueSafe(), lengthData.intValueSafe());

    byte[] encoded = sha3(buffer);
    DataWord word = new DataWord(encoded);

    program.stackPush(word);
    program.step();
  }

  public static void addressAction(Program program) {
    DataWord address = program.getContractAddress();
    // allowMultiSigns proposal
    if (VMConfig.allowMultiSign()) {
      address = new DataWord(address.getLast20Bytes());
    }

    program.stackPush(address);
    program.step();
  }

  public static void balanceAction(Program program) {
    DataWord address = program.stackPop();
    DataWord balance = program.getBalance(address);

    program.stackPush(balance);
    program.step();
  }

  public static void originAction(Program program) {
    DataWord originAddress = program.getOriginAddress();
    //allowMultiSign proposal
    if (VMConfig.allowMultiSign()) {
      originAddress = new DataWord(originAddress.getLast20Bytes());
    }

    program.stackPush(originAddress);
    program.step();
  }

  public static void callerAction(Program program) {
    DataWord callerAddress = program.getCallerAddress();
    /*
     since we use 21 bytes address instead of 20 as etherum, we need to make sure
     the address length in vm is matching with 20
     */
    callerAddress = new DataWord(callerAddress.getLast20Bytes());

    program.stackPush(callerAddress);
    program.step();
  }

  public static void callValueAction(Program program) {
    DataWord callValue = program.getCallValue();

    program.stackPush(callValue);
    program.step();
  }

  public static void callDataLoadAction(Program program) {
    DataWord dataOffs = program.stackPop();
    DataWord value = program.getDataValue(dataOffs);

    program.stackPush(value);
    program.step();
  }

  public static void callDataSizeAction(Program program) {
    DataWord dataSize = program.getDataSize();

    program.stackPush(dataSize);
    program.step();
  }

  public static void callDataCopyAction(Program program) {
    DataWord memOffsetData = program.stackPop();
    DataWord dataOffsetData = program.stackPop();
    DataWord lengthData = program.stackPop();

    byte[] msgData = program.getDataCopy(dataOffsetData, lengthData);

    program.memorySave(memOffsetData.intValueSafe(), msgData);
    program.step();
  }

  public static void codeSizeAction(Program program) {
    int length = program.getCode().length;

    DataWord codeLength = new DataWord(length);
    program.stackPush(codeLength);
    program.step();
  }

  public static void codeCopyAction(Program program) {
    byte[] fullCode = program.getCode();

    int memOffset = program.stackPop().intValueSafe();
    int codeOffset = program.stackPop().intValueSafe();
    int lengthData = program.stackPop().intValueSafe();

    int sizeToBeCopied = lengthData;
    if ((long) codeOffset + lengthData > fullCode.length) {
      sizeToBeCopied = fullCode.length < codeOffset ? 0 : fullCode.length - codeOffset;
    }

    byte[] codeCopy = new byte[lengthData];

    if (codeOffset < fullCode.length) {
      System.arraycopy(fullCode, codeOffset, codeCopy, 0, sizeToBeCopied);
    }

    program.memorySave(memOffset, codeCopy);
    program.step();
  }

  public static void returnDataSizeAction(Program program) {
    DataWord dataSize = program.getReturnDataBufferSize();

    program.stackPush(dataSize);
    program.step();
  }

  public static void returnDataCopyAction(Program program) {
    DataWord memOffsetData = program.stackPop();
    DataWord dataOffsetData = program.stackPop();
    DataWord lengthData = program.stackPop();

    byte[] msgData = program.getReturnDataBufferData(dataOffsetData, lengthData);

    if (msgData == null) {
      throw new Program.ReturnDataCopyIllegalBoundsException(dataOffsetData, lengthData,
          program.getReturnDataBufferSize().longValueSafe());
    }

    program.memorySave(memOffsetData.intValueSafe(), msgData);
    program.step();
  }

  public static void gasPriceAction(Program program) {
    DataWord energyPrice = DataWord.ZERO();
    if (VMConfig.allowTvmCompatibleEvm() && program.getContractVersion() == 1) {
      energyPrice = new DataWord(program.getContractState()
          .getDynamicPropertiesStore().getEnergyFee());
    }
    program.stackPush(energyPrice);
    program.step();
  }

  public static void extCodeSizeAction(Program program) {
    DataWord address = program.stackPop();

    int length = program.getCodeAt(address).length;
    DataWord codeLength = new DataWord(length);

    program.stackPush(codeLength);
    program.step();
  }

  public static void extCodeCopyAction(Program program) {
    DataWord address = program.stackPop();
    byte[] fullCode = program.getCodeAt(address);

    int memOffset = program.stackPop().intValueSafe();
    int codeOffset = program.stackPop().intValueSafe();
    int lengthData = program.stackPop().intValueSafe();

    int sizeToBeCopied = lengthData;
    if ((long) codeOffset + lengthData > fullCode.length) {
      sizeToBeCopied = fullCode.length < codeOffset ? 0 : fullCode.length - codeOffset;
    }

    byte[] codeCopy = new byte[lengthData];

    if (codeOffset < fullCode.length) {
      System.arraycopy(fullCode, codeOffset, codeCopy, 0, sizeToBeCopied);
    }

    program.memorySave(memOffset, codeCopy);
    program.step();
  }

  public static void extCodeHashAction(Program program) {
    DataWord address = program.stackPop();
    byte[] codeHash = program.getCodeHashAt(address);
    program.stackPush(new DataWord(codeHash));
    program.step();
  }

  public static void blockHashAction(Program program) {
    int blockIndex = program.stackPop().intValueSafe();
    DataWord blockHash = program.getBlockHash(blockIndex);

    program.stackPush(blockHash);
    program.step();
  }

  public static void coinBaseAction(Program program) {
    DataWord coinbase = program.getCoinbase();

    program.stackPush(coinbase);
    program.step();
  }

  public static void timeStampAction(Program program) {
    DataWord timestamp = program.getTimestamp();

    program.stackPush(timestamp);
    program.step();
  }

  public static void numberAction(Program program) {
    DataWord number = program.getNumber();

    program.stackPush(number);
    program.step();
  }

  public static void difficultyAction(Program program) {
    DataWord result = DataWord.ZERO();

    program.stackPush(result);
    program.step();
  }

  public static void gasLimitAction(Program program) {
    DataWord result = DataWord.ZERO();

    program.stackPush(result);
    program.step();
  }

  public static void chainIdAction(Program program) {
    DataWord chainId = program.getChainId();

    program.stackPush(chainId);
    program.step();
  }

  public static void selfBalanceAction(Program program) {
    DataWord selfBalance = program.getBalance(program.getContractAddress());

    program.stackPush(selfBalance);
    program.step();
  }

  public static void baseFeeAction(Program program) {
    DataWord energyFee =
        new DataWord(program.getContractState().getDynamicPropertiesStore().getEnergyFee());

    program.stackPush(energyFee);
    program.step();
  }

  public static void popAction(Program program) {
    program.stackPop();
    program.step();
  }

  public static void mLoadAction(Program program) {
    DataWord addr = program.stackPop();
    DataWord data = program.memoryLoad(addr);

    program.stackPush(data);
    program.step();
  }

  public static void mStoreAction(Program program) {
    DataWord addr = program.stackPop();
    DataWord value = program.stackPop();

    program.memorySave(addr, value);
    program.step();
  }

  public static void mStore8Action(Program program) {
    DataWord addr = program.stackPop();
    DataWord value = program.stackPop();

    byte[] byteVal = {value.getData()[31]};
    program.memorySave(addr.intValueSafe(), byteVal);
    program.step();
  }

  public static void sLoadAction(Program program) {
    DataWord key = program.stackPop();
    DataWord val = program.storageLoad(key);

    if (val == null) {
      val = key.and(DataWord.ZERO);
    }

    program.stackPush(val);
    program.step();
  }

  public static void sStoreAction(Program program) {
    if (program.isStaticCall()) {
      throw new Program.StaticCallModificationException();
    }

    DataWord addr = program.stackPop();
    DataWord value = program.stackPop();

    program.storageSave(addr, value);
    program.step();
  }

  public static void jumpAction(Program program) {
    DataWord pos = program.stackPop();
    int nextPC = program.verifyJumpDest(pos);

    program.setPC(nextPC);
  }

  public static void jumpIAction(Program program) {
    DataWord pos = program.stackPop();
    DataWord cond = program.stackPop();

    if (!cond.isZero()) {
      int nextPC = program.verifyJumpDest(pos);
      program.setPC(nextPC);
    } else {
      program.step();
    }
  }

  public static void pcAction(Program program) {
    int pc = program.getPC();
    DataWord pcWord = new DataWord(pc);

    program.stackPush(pcWord);
    program.step();
  }

  public static void mSizeAction(Program program) {
    int memSize = program.getMemSize();
    DataWord wordMemSize = new DataWord(memSize);

    program.stackPush(wordMemSize);
    program.step();
  }

  public static void gasAction(Program program) {
    DataWord energy = program.getEnergyLimitLeft();

    program.stackPush(energy);
    program.step();
  }

  public static void jumpDestAction(Program program) {
    program.step();
  }

  public static void pushAction(Program program) {
    int n = program.getCurrentOpIntValue() - Op.PUSH1 + 1;
    program.step();
    byte[] data = program.sweep(n);

    program.stackPush(new DataWord(data));
  }

  public static void dupAction(Program program) {
    Stack stack = program.getStack();
    int n = program.getCurrentOpIntValue() - Op.DUP1 + 1;
    DataWord word_1 = stack.get(stack.size() - n);

    program.stackPush(word_1.clone());
    program.step();
  }

  public static void swapAction(Program program) {
    Stack stack = program.getStack();
    int n = program.getCurrentOpIntValue() - Op.SWAP1 + 2;
    stack.swap(stack.size() - 1, stack.size() - n);

    program.step();
  }

  public static void logAction(Program program) {
    Stack stack = program.getStack();
    if (program.isStaticCall()) {
      throw new Program.StaticCallModificationException();
    }
    DataWord address = program.getContractAddress();

    DataWord memStart = stack.pop();
    DataWord memOffset = stack.pop();

    int nTopics = program.getCurrentOpIntValue() - Op.LOG0;

    List<DataWord> topics = new ArrayList<>();
    for (int i = 0; i < nTopics; ++i) {
      DataWord topic = stack.pop();
      topics.add(topic);
    }

    byte[] data = program.memoryChunk(memStart.intValueSafe(), memOffset.intValueSafe());

    LogInfo logInfo =
        new LogInfo(address.getLast20Bytes(), topics, data);

    program.getResult().addLogInfo(logInfo);
    program.step();
  }

  public static void tokenBalanceAction(Program program) {
    DataWord tokenId = program.stackPop();
    DataWord address = program.stackPop();
    DataWord tokenBalance = program.getTokenBalance(address, tokenId);

    program.stackPush(tokenBalance);
    program.step();
  }

  public static void callTokenValueAction(Program program) {
    DataWord tokenValue = program.getTokenValue();

    program.stackPush(tokenValue);
    program.step();
  }

  public static void callTokenIdAction(Program program) {
    DataWord _tokenId = program.getTokenId();

    program.stackPush(_tokenId);
    program.step();
  }

  public static void isContractAction(Program program) {
    DataWord address = program.stackPop();
    DataWord isContract = program.isContract(address);

    program.stackPush(isContract);
    program.step();
  }

  public static void freezeAction(Program program) {
    // after allow vote, check static
    if (VMConfig.allowTvmVote() && program.isStaticCall()) {
      throw new Program.StaticCallModificationException();
    }
    // 0 as bandwidth, 1 as energy
    DataWord resourceType = program.stackPop();
    DataWord frozenBalance = program.stackPop();
    DataWord receiverAddress = program.stackPop();

    if (VMConfig.allowTvmFreezeV2()) {
      // after v2 activated, we just push zero to stack and do nothing
      program.stackPush(DataWord.ZERO());
    } else {
      boolean result = program.freeze(receiverAddress, frozenBalance, resourceType );
      program.stackPush(result ? DataWord.ONE() : DataWord.ZERO());
    }
    program.step();
  }

  public static void unfreezeAction(Program program) {
    if (VMConfig.allowTvmVote() && program.isStaticCall()) {
      throw new Program.StaticCallModificationException();
    }

    DataWord resourceType = program.stackPop();
    DataWord receiverAddress = program.stackPop();

    boolean result = program.unfreeze(receiverAddress, resourceType);
    program.stackPush(result ? DataWord.ONE() : DataWord.ZERO());
    program.step();
  }

  public static void freezeExpireTimeAction(Program program) {
    DataWord resourceType = program.stackPop();
    DataWord targetAddress = program.stackPop();

    long expireTime = program.freezeExpireTime(targetAddress, resourceType);
    program.stackPush(new DataWord(expireTime / 1000));
    program.step();
  }

  public static void freezeBalanceV2Action(Program program) {
    // after allow vote, check static
    if (program.isStaticCall()) {
      throw new Program.StaticCallModificationException();
    }
    DataWord resourceType = program.stackPop();
    DataWord frozenBalance = program.stackPop();

    boolean result = program.freezeBalanceV2(frozenBalance, resourceType);
    program.stackPush(result ? DataWord.ONE() : DataWord.ZERO());
    program.step();
  }

  public static void unfreezeBalanceV2Action(Program program) {
    if (program.isStaticCall()) {
      throw new Program.StaticCallModificationException();
    }

    DataWord resourceType = program.stackPop();
    DataWord unfreezeBalance = program.stackPop();

    boolean result = program.unfreezeBalanceV2(unfreezeBalance, resourceType);
    program.stackPush(result ? DataWord.ONE() : DataWord.ZERO());
    program.step();
  }

  public static void withdrawExpireUnfreezeAction(Program program) {
    if (program.isStaticCall()) {
      throw new Program.StaticCallModificationException();
    }

    long expireUnfreezeBalance = program.withdrawExpireUnfreeze();
    program.stackPush(new DataWord(expireUnfreezeBalance));
    program.step();
  }

  public static void cancelAllUnfreezeV2Action(Program program) {
    if (program.isStaticCall()) {
      throw new Program.StaticCallModificationException();
    }

    boolean result = program.cancelAllUnfreezeV2Action();
    program.stackPush(result ? DataWord.ONE() : DataWord.ZERO());
    program.step();
  }

  public static void delegateResourceAction(Program program) {
    if (program.isStaticCall()) {
      throw new Program.StaticCallModificationException();
    }
    DataWord resourceType = program.stackPop();
    DataWord delegateBalance = program.stackPop();
    DataWord receiverAddress = program.stackPop();

    boolean result = program.delegateResource(receiverAddress, delegateBalance, resourceType);
    program.stackPush(result ? DataWord.ONE() : DataWord.ZERO());
    program.step();
  }

  public static void unDelegateResourceAction(Program program) {
    if (program.isStaticCall()) {
      throw new Program.StaticCallModificationException();
    }
    DataWord resourceType = program.stackPop();
    DataWord unDelegateBalance = program.stackPop();
    DataWord receiverAddress = program.stackPop();

    boolean result = program.unDelegateResource(receiverAddress, unDelegateBalance, resourceType);
    program.stackPush(result ? DataWord.ONE() : DataWord.ZERO());
    program.step();
  }

  public static void voteWitnessAction(Program program) {
    if (program.isStaticCall()) {
      throw new Program.StaticCallModificationException();
    }

    int amountArrayLength = program.stackPop().intValueSafe();
    int amountArrayOffset = program.stackPop().intValueSafe();
    int witnessArrayLength = program.stackPop().intValueSafe();
    int witnessArrayOffset = program.stackPop().intValueSafe();

    boolean result = program.voteWitness(witnessArrayOffset, witnessArrayLength,
        amountArrayOffset, amountArrayLength);
    program.stackPush(result ? DataWord.ONE() : DataWord.ZERO());
    program.step();
  }

  public static void withdrawRewardAction(Program program) {
    if (program.isStaticCall()) {
      throw new Program.StaticCallModificationException();
    }

    long allowance = program.withdrawReward();
    program.stackPush(new DataWord(allowance));
    program.step();
  }

  public static void createAction(Program program) {
    if (program.isStaticCall()) {
      throw new Program.StaticCallModificationException();
    }

    DataWord value = program.stackPop();
    DataWord inOffset = program.stackPop();
    DataWord inSize = program.stackPop();

    program.createContract(value, inOffset, inSize);
    program.step();
  }

  public static void returnAction(Program program) {
    DataWord offset = program.stackPop();
    DataWord size = program.stackPop();

    byte[] hReturn = program.memoryChunk(offset.intValueSafe(), size.intValueSafe());
    program.setHReturn(hReturn);

    program.step();
    program.stop();
  }

  public static void create2Action(Program program) {
    if (program.isStaticCall()) {
      throw new Program.StaticCallModificationException();
    }

    DataWord value = program.stackPop();
    DataWord inOffset = program.stackPop();
    DataWord inSize = program.stackPop();
    DataWord salt = program.stackPop();

    program.createContract2(value, inOffset, inSize, salt);
    program.step();
  }

  public static void callAction(Program program) {
    // use adjustedCallEnergy instead of requested
    program.stackPop();
    DataWord codeAddress = program.stackPop();
    DataWord value = program.stackPop();

    if (program.isStaticCall() && !value.isZero()) {
      throw new Program.StaticCallModificationException();
    }
    DataWord adjustedCallEnergy = program.getAdjustedCallEnergy();
    if (!value.isZero()) {
      adjustedCallEnergy.add(new DataWord(EnergyCost.getStipendCallCost()));
    }
    exeCall(program, adjustedCallEnergy, codeAddress, value, DataWord.ZERO(), false);
  }

  public static void callTokenAction(Program program) {
    program.stackPop();
    DataWord codeAddress = program.stackPop();
    DataWord value = program.stackPop();

    if (program.isStaticCall() && !value.isZero()) {
      throw new Program.StaticCallModificationException();
    }
    DataWord adjustedCallEnergy = program.getAdjustedCallEnergy();
    if (!value.isZero()) {
      adjustedCallEnergy.add(new DataWord(EnergyCost.getStipendCallCost()));
    }
    DataWord tokenId = program.stackPop();
    exeCall(program, adjustedCallEnergy, codeAddress, value, tokenId, VMConfig.allowMultiSign());
  }

  public static void callCodeAction(Program program) {
    program.stackPop();
    DataWord codeAddress = program.stackPop();
    DataWord value = program.stackPop();

    DataWord adjustedCallEnergy = program.getAdjustedCallEnergy();
    if (!value.isZero()) {
      adjustedCallEnergy.add(new DataWord(EnergyCost.getStipendCallCost()));
    }
    exeCall(program, adjustedCallEnergy, codeAddress, value, DataWord.ZERO(), false);
  }

  public static void delegateCallAction(Program program) {
    program.stackPop();
    DataWord codeAddress = program.stackPop();
    DataWord value = DataWord.ZERO;

    DataWord adjustedCallEnergy = program.getAdjustedCallEnergy();
    exeCall(program, adjustedCallEnergy, codeAddress, value, DataWord.ZERO(), false);
  }

  public static void staticCallAction(Program program) {
    program.stackPop();
    DataWord codeAddress = program.stackPop();
    DataWord value = DataWord.ZERO;

    DataWord adjustedCallEnergy = program.getAdjustedCallEnergy();
    exeCall(program, adjustedCallEnergy, codeAddress, value, DataWord.ZERO(), false);
  }

  public static void exeCall(Program program, DataWord adjustedCallEnergy,
      DataWord codeAddress, DataWord value, DataWord tokenId, boolean isTokenTransferMsg) {

    DataWord inDataOffs = program.stackPop();
    DataWord inDataSize = program.stackPop();

    DataWord outDataOffs = program.stackPop();
    DataWord outDataSize = program.stackPop();

    program.memoryExpand(outDataOffs, outDataSize);
    int op = program.getCurrentOpIntValue();
    MessageCall msg = new MessageCall(
        op, adjustedCallEnergy, codeAddress, value, inDataOffs, inDataSize,
        outDataOffs, outDataSize, tokenId, isTokenTransferMsg);

    PrecompiledContracts.PrecompiledContract contract =
        PrecompiledContracts.getContractForAddress(codeAddress);
    if (contract != null) {
      program.callToPrecompiledAddress(msg, contract);
    } else {
      program.callToAddress(msg);
    }
    program.step();
  }

  public static void revertAction(Program program) {
    DataWord offset = program.stackPop();
    DataWord size = program.stackPop();

    byte[] hReturn = program.memoryChunk(offset.intValueSafe(), size.intValueSafe());
    program.setHReturn(hReturn);

    program.step();
    program.stop();

    program.getResult().setRevert();
  }

  public static void suicideAction(Program program) {
    if (program.isStaticCall()) {
      throw new Program.StaticCallModificationException();
    }

    if (!program.canSuicide()) {
      program.getResult().setRevert();
    } else {
      DataWord address = program.stackPop();
      program.suicide(address);
    }

    program.stop();
  }

}
