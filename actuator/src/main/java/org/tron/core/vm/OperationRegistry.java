package org.tron.core.vm;

public class OperationRegistry {

  private static final int NUM_OPERATIONS = 256;

  private static final Operation[] operations = new Operation[NUM_OPERATIONS];

  public static Operation get(final int opcode) {
    return operations[opcode];
  }

  // only for test
  public static void clearOperations() {
    for (int i = 0; i < NUM_OPERATIONS; i++) {
      operations[i] = null;
    }
  }

  public static void newBaseOperation() {
    // if already loaded, return
    if (operations[Op.SUICIDE] != null) {
      return;
    }

    operations[Op.STOP] = new Operation(Op.STOP, 0, 0,
        NewEnergyCost::getZeroTierCost, OperationActions::stopAction);

    operations[Op.ADD] = new Operation(Op.ADD, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::addAction);

    operations[Op.MUL] = new Operation(Op.MUL, 2, 1,
        NewEnergyCost::getLowTierCost, OperationActions::mulAction);

    operations[Op.SUB] = new Operation(Op.SUB, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::subAction);

    operations[Op.DIV] = new Operation(Op.DIV, 2, 1,
        NewEnergyCost::getLowTierCost, OperationActions::divAction);

    operations[Op.SDIV] = new Operation(Op.SDIV, 2, 1,
        NewEnergyCost::getLowTierCost, OperationActions::sdivAction);

    operations[Op.MOD] = new Operation(Op.MOD, 2, 1,
        NewEnergyCost::getLowTierCost, OperationActions::modAction);

    operations[Op.SMOD] = new Operation(Op.SMOD, 2, 1,
        NewEnergyCost::getLowTierCost, OperationActions::sModAction);

    operations[Op.ADDMOD] = new Operation(Op.ADDMOD, 3, 1,
        NewEnergyCost::getMidTierCost, OperationActions::addModAction);

    operations[Op.MULMOD] = new Operation(Op.MULMOD, 3, 1,
        NewEnergyCost::getMidTierCost, OperationActions::mulModAction);

    operations[Op.EXP] = new Operation(Op.EXP, 2, 1,
        NewEnergyCost::getExpCost, OperationActions::expAction);

    operations[Op.SIGNEXTEND] = new Operation(Op.SIGNEXTEND, 2, 1,
        NewEnergyCost::getLowTierCost, OperationActions::signExtendAction);

    operations[Op.LT] = new Operation(Op.LT, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::ltAction);

    operations[Op.GT] = new Operation(Op.GT, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::gtAction);

    operations[Op.SLT] = new Operation(Op.SLT, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::sltAction);

    operations[Op.SGT] = new Operation(Op.SGT, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::sgtAction);

    operations[Op.EQ] = new Operation(Op.EQ, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::eqAction);

    operations[Op.ISZERO] = new Operation(Op.ISZERO, 1, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::isZeroAction);

    operations[Op.AND] = new Operation(Op.AND, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::andAction);

    operations[Op.OR] = new Operation(Op.OR, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::orAction);

    operations[Op.XOR] = new Operation(Op.XOR, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::xorAction);

    operations[Op.NOT] = new Operation(Op.NOT, 1, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::notAction);

    operations[Op.BYTE] = new Operation(Op.BYTE, 1, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::byteAction);

    operations[Op.SHA3] = new Operation(Op.SHA3, 2, 1,
        NewEnergyCost::getSha3Cost, OperationActions::sha3Action);

    operations[Op.ADDRESS] = new Operation(Op.ADDRESS, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::addressAction);

    operations[Op.BALANCE] = new Operation(Op.BALANCE, 1, 1,
        NewEnergyCost::getBalanceCost, OperationActions::balanceAction);

    operations[Op.ORIGIN] = new Operation(Op.ORIGIN, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::originAction);

    operations[Op.CALLER] = new Operation(Op.CALLER, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::callerAction);

    operations[Op.CALLVALUE] = new Operation(Op.CALLVALUE, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::callValueAction);

    operations[Op.CALLDATALOAD] = new Operation(Op.CALLDATALOAD, 1, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::callDataLoadAction);

    operations[Op.CALLDATASIZE] = new Operation(Op.CALLDATASIZE, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::callDataSizeAction);

    operations[Op.CALLDATACOPY] = new Operation(Op.CALLDATACOPY, 3, 0,
        NewEnergyCost::getCallDataCopyCost, OperationActions::callDataCopyAction);

    operations[Op.CODESIZE] = new Operation(Op.CODESIZE, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::codeSizeAction);

    operations[Op.CODECOPY] = new Operation(Op.CODECOPY, 3, 0,
        NewEnergyCost::getCodeCopyCost, OperationActions::codeCopyAction);

    operations[Op.RETURNDATASIZE] = new Operation(Op.RETURNDATASIZE, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::returnDataSizeAction);

    operations[Op.RETURNDATACOPY] = new Operation(Op.RETURNDATACOPY, 3, 0,
        NewEnergyCost::getReturnDataCopyCost, OperationActions::returnDataCopyAction);

    operations[Op.GASPRICE] = new Operation(Op.GASPRICE, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::gasPriceAction);

    operations[Op.EXTCODESIZE] = new Operation(Op.EXTCODESIZE, 1, 1,
        NewEnergyCost::getExtCodeSizeCost, OperationActions::extCodeSizeAction);

    operations[Op.EXTCODECOPY] = new Operation(Op.EXTCODECOPY, 4, 0,
        NewEnergyCost::getExtCodeCopyCost, OperationActions::extCodeCopyAction);

    operations[Op.BLOCKHASH] = new Operation(Op.BLOCKHASH, 1, 1,
        NewEnergyCost::getExtTierCost, OperationActions::blockHashAction);

    operations[Op.COINBASE] = new Operation(Op.COINBASE, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::coinBaseAction);

    operations[Op.TIMESTAMP] = new Operation(Op.TIMESTAMP, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::timeStampAction);

    operations[Op.NUMBER] = new Operation(Op.NUMBER, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::numberAction);

    operations[Op.DIFFICULTY] = new Operation(Op.DIFFICULTY, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::difficultyAction);

    operations[Op.GASLIMIT] = new Operation(Op.GASLIMIT, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::gasLimitAction);

    operations[Op.POP] = new Operation(Op.POP, 1, 0,
        NewEnergyCost::getBaseTierCost, OperationActions::popAction);

    operations[Op.MLOAD] = new Operation(Op.MLOAD, 1, 1,
        NewEnergyCost::getMloadCost, OperationActions::mLoadAction);

    operations[Op.MSTORE] = new Operation(Op.MSTORE, 2, 0,
        NewEnergyCost::getMStoreCost, OperationActions::mStoreAction);

    operations[Op.MSTORE8] = new Operation(Op.MSTORE8, 2, 0,
        NewEnergyCost::getMStore8Cost, OperationActions::mStore8Action);

    operations[Op.SLOAD] = new Operation(Op.SLOAD, 1, 1,
        NewEnergyCost::getSloadCost, OperationActions::sLoadAction);

    operations[Op.SSTORE] = new Operation(Op.SSTORE, 2, 0,
        NewEnergyCost::getSstoreCost, OperationActions::sStoreAction);

    operations[Op.JUMP] = new Operation(Op.JUMP, 1, 0,
        NewEnergyCost::getMidTierCost, OperationActions::jumpAction);

    operations[Op.JUMPI] = new Operation(Op.JUMPI, 2, 0,
        NewEnergyCost::getHighTierCost, OperationActions::jumpIAction);

    operations[Op.PC] = new Operation(Op.PC, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::pcAction);

    operations[Op.MSIZE] = new Operation(Op.MSIZE, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::mSizeAction);

    operations[Op.GAS] = new Operation(Op.GAS, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::gasAction);

    operations[Op.JUMPDEST] = new Operation(Op.JUMPDEST, 0, 0,
        NewEnergyCost::getSpecialTierCost, OperationActions::jumpDestAction);

    for (int i = 0; i < 32; i++) {
      operations[Op.PUSH1 + i] = new Operation(Op.PUSH1 + i, 0, 1,
          NewEnergyCost::getVeryLowTierCost, OperationActions::pushAction);
    }

    for (int i = 0; i < 16; i++) {
      operations[Op.DUP1 + i] = new Operation(Op.DUP1 + i, 1 + i, 2 + i,
          NewEnergyCost::getVeryLowTierCost, OperationActions::dupAction);
    }

    for (int i = 0; i < 16; i++) {
      operations[Op.SWAP1 + i] = new Operation(Op.SWAP1 + i, 2 + i, 2 + i,
          NewEnergyCost::getVeryLowTierCost, OperationActions::swapAction);
    }

    for (int i = 0; i <= 4; i++) {
      operations[Op.LOG0 + i] = new Operation(Op.LOG0 + i, 2 + i, 0,
          NewEnergyCost::getLogCost, OperationActions::logAction);
    }

    operations[Op.CREATE] = new Operation(Op.CREATE, 3, 1,
        NewEnergyCost::getCreateCost, OperationActions::createAction);

    operations[Op.CALL] = new Operation(Op.CALL, 7, 1,
        NewEnergyCost::getCallCost, OperationActions::callAction);

    operations[Op.CALLCODE] = new Operation(Op.CALLCODE, 7, 1,
        NewEnergyCost::getCallCodeCost, OperationActions::callCodeAction);

    operations[Op.RETURN] = new Operation(Op.RETURN, 2, 0,
        NewEnergyCost::getReturnCost, OperationActions::returnAction);

    operations[Op.DELEGATECALL] = new Operation(Op.DELEGATECALL, 6, 1,
        NewEnergyCost::getDelegateCallCost, OperationActions::delegateCallAction);

    operations[Op.STATICCALL] = new Operation(Op.STATICCALL, 6, 1,
        NewEnergyCost::getStaticCallCost, OperationActions::staticCallAction);

    operations[Op.REVERT] = new Operation(Op.REVERT, 2, 0,
        NewEnergyCost::getRevertCost, OperationActions::revertAction);

    operations[Op.SUICIDE] = new Operation(Op.SUICIDE, 1, 0,
        NewEnergyCost::getSuicideCost, OperationActions::suicideAction);
  }

  public static void newAllowTvmTransferTrc10Operation() {
    // if already loaded, return
    if (operations[Op.CALLTOKENID] != null) {
      return;
    }
    operations[Op.CALLTOKEN] = new Operation(Op.CALLTOKEN, 8, 0,
        NewEnergyCost::getCallTokenCost, OperationActions::callTokenAction);

    operations[Op.TOKENBALANCE] = new Operation(Op.TOKENBALANCE, 2, 1,
        NewEnergyCost::getBalanceCost, OperationActions::tokenBalanceAction);

    operations[Op.CALLTOKENVALUE] = new Operation(Op.CALLTOKENVALUE, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::callTokenValueAction);

    operations[Op.CALLTOKENID] = new Operation(Op.CALLTOKENID, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::callTokenIdAction);
  }

  public static void newAllowTvmConstantinopleOperation() {
    // if already loaded, return
    if (operations[Op.EXTCODEHASH] != null) {
      return;
    }
    operations[Op.SHL] = new Operation(Op.SHL, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::shlAction);

    operations[Op.SHR] = new Operation(Op.SHR, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::shrAction);

    operations[Op.SAR] = new Operation(Op.SAR, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::sarAction);

    operations[Op.CREATE2] = new Operation(Op.CREATE2, 4, 1,
        NewEnergyCost::getCreate2Cost, OperationActions::create2Action);

    operations[Op.EXTCODEHASH] = new Operation(Op.EXTCODEHASH, 1, 1,
        NewEnergyCost::getExtCodeHashCost, OperationActions::extCodeHashAction);
  }

  public static void newAllowTvmSolidity059Operation() {
    // if already loaded, return
    if (operations[Op.ISCONTRACT] != null) {
      return;
    }
    operations[Op.ISCONTRACT] = new Operation(Op.ISCONTRACT, 1, 1,
        NewEnergyCost::getBalanceCost, OperationActions::isContractAction);
  }

  public static void newAllowTvmIstanbulOperation() {
    // if already loaded, return
    if (operations[Op.SELFBALANCE] != null) {
      return;
    }
    operations[Op.CHAINID] = new Operation(Op.CHAINID, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::chainIdAction);

    operations[Op.SELFBALANCE] = new Operation(Op.SELFBALANCE, 0, 1,
        NewEnergyCost::getLowTierCost, OperationActions::selfBalanceAction);
  }

  public static void newAllowTvmFreezeOperation() {
    // if already loaded, return
    if (operations[Op.FREEZEEXPIRETIME] != null) {
      return;
    }
    operations[Op.FREEZE] = new Operation(Op.FREEZE, 3, 1,
        NewEnergyCost::getFreezeCost, OperationActions::freezeAction);

    operations[Op.UNFREEZE] = new Operation(Op.UNFREEZE, 2, 1,
        NewEnergyCost::getUnfreezeCost, OperationActions::unfreezeAction);

    operations[Op.FREEZEEXPIRETIME] = new Operation(Op.FREEZEEXPIRETIME, 2, 1,
        NewEnergyCost::getFreezeExpireTimeCost, OperationActions::freezeExpireTimeAction);
  }

  public static void newAllowTvmVoteOperation() {
    // if already loaded, return
    if (operations[Op.WITHDRAWREWARD] != null) {
      return;
    }
    operations[Op.VOTEWITNESS] = new Operation(Op.VOTEWITNESS, 4, 1,
        NewEnergyCost::getVoteWitnessCost, OperationActions::voteWitnessAction);

    operations[Op.WITHDRAWREWARD] = new Operation(Op.WITHDRAWREWARD, 0, 1,
        NewEnergyCost::getWithdrawRewardCost, OperationActions::withdrawRewardAction);
  }

  public static void newAllowTvmLondonOperation() {
    // if already loaded, return
    if (operations[Op.BASEFEE] != null) {
      return;
    }
    operations[Op.BASEFEE] = new Operation(Op.BASEFEE, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::baseFeeAction);
  }

}
