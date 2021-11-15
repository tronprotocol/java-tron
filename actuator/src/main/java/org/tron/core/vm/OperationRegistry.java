package org.tron.core.vm;

import org.tron.core.vm.config.VMConfig;

public class OperationRegistry {

  private static final int NUM_OPERATIONS = 256;

  private static final Operation[] operations = new Operation[NUM_OPERATIONS];

  public static Operation get(final byte opcode) {
    return operations[opcode & 0xff];
  }

  static {
    operations[Op.STOP] = new Operation(0x00, 0, 0,
        NewEnergyCost::getZeroTierCost, OperationActions::stopAction);
    operations[Op.ADD] = new Operation(0x01, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::addAction);
    operations[Op.MUL] = new Operation(0x02, 2, 1,
        NewEnergyCost::getLowTierCost, OperationActions::mulAction);
    operations[Op.SUB] = new Operation(0x03, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::subAction);
    operations[Op.DIV] = new Operation(0x04, 2, 1,
        NewEnergyCost::getLowTierCost, OperationActions::divAction);
    operations[Op.SDIV] = new Operation(0x05, 2, 1,
        NewEnergyCost::getLowTierCost, OperationActions::sdivAction);
    operations[Op.MOD] = new Operation(0x06, 2, 1,
        NewEnergyCost::getLowTierCost, OperationActions::modAction);
    operations[Op.SMOD] = new Operation(0x07, 2, 1,
        NewEnergyCost::getLowTierCost, OperationActions::sModAction);
    operations[Op.ADDMOD] = new Operation(0x08, 3, 1,
        NewEnergyCost::getMidTierCost, OperationActions::addModAction);
    operations[Op.MULMOD] = new Operation(0x09, 3, 1,
        NewEnergyCost::getMidTierCost, OperationActions::mulModAction);
    operations[Op.EXP] = new Operation(0x0a, 2, 1,
        NewEnergyCost::getExpCost, OperationActions::expAction);
    operations[Op.SIGNEXTEND] = new Operation(0x0b, 2, 1,
        NewEnergyCost::getLowTierCost, OperationActions::signExtendAction);
    operations[Op.LT] = new Operation(0X10, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::ltAction);
    operations[Op.GT] = new Operation(0X11, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::gtAction);
    operations[Op.SLT] = new Operation(0X12, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::sltAction);
    operations[Op.SGT] = new Operation(0X13, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::sgtAction);
    operations[Op.EQ] = new Operation(0X14, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::eqAction);
    operations[Op.ISZERO] = new Operation(0x15, 1, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::isZeroAction);
    operations[Op.AND] = new Operation(0x16, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::andAction);
    operations[Op.OR] = new Operation(0x17, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::orAction);
    operations[Op.XOR] = new Operation(0x18, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::xorAction);
    operations[Op.NOT] = new Operation(0x19, 1, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::notAction);
    operations[Op.BYTE] = new Operation(0x1a, 1, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::byteAction);

    if (VMConfig.allowTvmConstantinople()) {
      operations[Op.SHL] = new Operation(0x1b, 2, 1,
          NewEnergyCost::getVeryLowTierCost, OperationActions::shlAction);
      operations[Op.SHR] = new Operation(0x1c, 2, 1,
          NewEnergyCost::getVeryLowTierCost, OperationActions::shrAction);
      operations[Op.SAR] = new Operation(0x1d, 2, 1,
          NewEnergyCost::getVeryLowTierCost, OperationActions::sarAction);
      operations[Op.CREATE2] = new Operation(0xf5, 4, 1,
          NewEnergyCost::getCreate2Cost, OperationActions::create2Action);
      operations[Op.EXTCODEHASH] = new Operation(0x3f, 1, 1,
          NewEnergyCost::getExtTierCost, OperationActions::extCodeHashAction);
    }

    operations[Op.SHA3] = new Operation(0x20, 2, 1,
        NewEnergyCost::getSha3Cost, OperationActions::sha3Action);
    operations[Op.ADDRESS] = new Operation(0x30, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::addressAction);
    operations[Op.BALANCE] = new Operation(0x31, 1, 1,
        NewEnergyCost::getBalanceCost, OperationActions::balanceAction);
    operations[Op.ORIGIN] = new Operation(0x32, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::originAction);
    operations[Op.CALLER] = new Operation(0x33, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::callerAction);
    operations[Op.CALLVALUE] = new Operation(0x34, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::callValueAction);
    operations[Op.CALLDATALOAD] = new Operation(0x35, 1, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::callDataLoadAction);
    operations[Op.CALLDATASIZE] = new Operation(0x36, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::callDataSizeAction);
    operations[Op.CALLDATACOPY] = new Operation(0x37, 3, 0,
        NewEnergyCost::getVeryLowTierCost, OperationActions::callDataCopyAction);
    operations[Op.CODESIZE] = new Operation(0x38, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::codeSizeAction);
    operations[Op.CODECOPY] = new Operation(0x39, 3, 0,
        NewEnergyCost::getVeryLowTierCost, OperationActions::codeCopyAction);
    operations[Op.RETURNDATASIZE] = new Operation(0x3d, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::returnDataSizeAction);
    operations[Op.RETURNDATACOPY] = new Operation(0x3e, 3, 0,
        NewEnergyCost::getVeryLowTierCost, OperationActions::returnDataCopyAction);
    operations[Op.GASPRICE] = new Operation(0x3a, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::gasPriceAction);
    operations[Op.EXTCODESIZE] = new Operation(0x3b, 1, 1,
        NewEnergyCost::getExtTierCost, OperationActions::extCodeSizeAction);
    operations[Op.EXTCODECOPY] = new Operation(0x3c, 4, 0,
        NewEnergyCost::getExtTierCost, OperationActions::extCodeCopyAction);
    operations[Op.BLOCKHASH] = new Operation(0x40, 1, 1,
        NewEnergyCost::getExtTierCost, OperationActions::blockHashAction);
    operations[Op.COINBASE] = new Operation(0x41, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::coinBaseAction);
    operations[Op.TIMESTAMP] = new Operation(0x42, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::timeStampAction);
    operations[Op.NUMBER] = new Operation(0x43, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::numberAction);
    operations[Op.DIFFICULTY] = new Operation(0x44, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::difficultyAction);
    operations[Op.GASLIMIT] = new Operation(0x45, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::gasLimitAction);

    if (VMConfig.allowTvmIstanbul()) {
      operations[Op.CHAINID] = new Operation(0x46, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::chainIdAction);
      operations[Op.SELFBALANCE] = new Operation(0x47, 0, 1,
        NewEnergyCost::getLowTierCost, OperationActions::selfBalanceAction);
    }

    if (VMConfig.allowTvmLondon()) {
      operations[Op.BASEFEE] = new Operation(0x48, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::baseFeeAction);
    }
    operations[Op.POP] = new Operation(0x50, 1, 0,
        NewEnergyCost::getBaseTierCost, OperationActions::popAction);
    operations[Op.MLOAD] = new Operation(0x51, 1, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::mLoadAction);
    operations[Op.MSTORE] = new Operation(0x52, 2, 0,
        NewEnergyCost::getVeryLowTierCost, OperationActions::mStoreAction);
    operations[Op.MSTORE8] = new Operation(0x53, 2, 0,
        NewEnergyCost::getVeryLowTierCost, OperationActions::mStore8Action);
    operations[Op.SLOAD] = new Operation(0x54, 1, 1,
        NewEnergyCost::getSloadCost, OperationActions::sLoadAction);
    operations[Op.SSTORE] = new Operation(0x55, 2, 0,
        NewEnergyCost::getSstoreCost, OperationActions::sStoreAction);
    operations[Op.JUMP] = new Operation(0x56, 1, 0,
        NewEnergyCost::getMidTierCost, OperationActions::jumpAction);
    operations[Op.JUMPI] = new Operation(0x57, 2, 0,
        NewEnergyCost::getHighTierrCost, OperationActions::jumpIAction);
    operations[Op.PC] = new Operation(0x58, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::pcAction);
    operations[Op.MSIZE] = new Operation(0x59, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::mSizeAction);
    operations[Op.GAS] = new Operation(0x5a, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::gasAction);
    operations[Op.JUMPDEST] = new Operation(0x5b, 0, 0,
        NewEnergyCost::getSpecialTierCost, OperationActions::jumpDestAction);

    for (int i = 0; i <= 31; i++) {
      operations[Op.PUSH1 + i] = new Operation(0x60 + i, 0, 1,
          NewEnergyCost::getVeryLowTierCost, OperationActions::pushAction);
    }
    for (int i = 0; i <= 15; i++) {
      operations[Op.DUP1 + i] = new Operation(0x80 + i, 1 + i, 2 + i,
          NewEnergyCost::getVeryLowTierCost, OperationActions::dupAction);
    }
    for (int i = 0; i <= 15; i++) {
      operations[Op.SWAP1 + i] = new Operation(0x90 + i, 2 + i, 2 + i,
          NewEnergyCost::getVeryLowTierCost, OperationActions::swapAction);
    }
    for (int i = 0; i <= 4; i++) {
      operations[Op.LOG0 + i] = new Operation(0xa0 + i, 2 + i, 0,
          NewEnergyCost::getLogCost, OperationActions::logAction);
    }

    if (VMConfig.allowTvmTransferTrc10()) {
      operations[Op.CALLTOKEN] = new Operation(0xd0, 8, 0,
          NewEnergyCost::getCallTokenCost, OperationActions::callTokenAction);
      operations[Op.TOKENBALANCE] = new Operation(0xd1, 2, 1,
          NewEnergyCost::getBalanceCost, OperationActions::tokenBalanceAction);
      operations[Op.CALLTOKENVALUE] = new Operation(0xd2, 0, 1,
          NewEnergyCost::getBaseTierCost, OperationActions::callTokenValueAction);
      operations[Op.CALLTOKENID] = new Operation(0xd3, 0, 1,
          NewEnergyCost::getBaseTierCost, OperationActions::callTokenIdAction);
    }

    if (VMConfig.allowTvmSolidity059()) {
      operations[Op.ISCONTRACT] = new Operation(0xd4, 1, 1,
        NewEnergyCost::getBalanceCost, OperationActions::isContractAction);
    }

    if (VMConfig.allowTvmFreeze()) {
      operations[Op.FREEZE] = new Operation(0xd5, 3, 1,
        NewEnergyCost::getFreezeCost, OperationActions::freezeAction);
      operations[Op.UNFREEZE] = new Operation(0xd6, 2, 1,
        NewEnergyCost::getUnfreezeCost, OperationActions::unfreezeAction);
      operations[Op.FREEZEEXPIRETIME] = new Operation(0xd7, 2, 1,
        NewEnergyCost::getFreezeExpireTimeCost, OperationActions::freezeExpireTimeAction);
    }

    if (VMConfig.allowTvmVote()) {
      operations[Op.VOTEWITNESS] = new Operation(0xd8, 4, 1,
        NewEnergyCost::getVoteWitnessCost, OperationActions::voteWitnessAction);
      operations[Op.WITHDRAWREWARD] = new Operation(0xd9, 0, 1,
        NewEnergyCost::getWithdrawRewardCost, OperationActions::withdrawRewardAction);
    }

    operations[Op.CREATE] = new Operation(0xf0, 3, 1,
        NewEnergyCost::getCreateCost, OperationActions::createAction);
    operations[Op.CALL] = new Operation(0xf1, 7, 1,
        NewEnergyCost::getCallCost, OperationActions::callAction);
    operations[Op.CALLCODE] = new Operation(0xf2, 7, 1,
        NewEnergyCost::getCallCodeCost, OperationActions::callCodeAction);
    operations[Op.RETURN] = new Operation(0xf3, 2, 0,
        NewEnergyCost::getZeroTierCost, OperationActions::returnAction);
    operations[Op.DELEGATECALL] = new Operation(0xf4, 6, 1,
        NewEnergyCost::getDelegateCallCost, OperationActions::delegateCallAction);
    operations[Op.STATICCALL] = new Operation(0xfa, 6, 1,
        NewEnergyCost::getStaticCallCost, OperationActions::staticCallAction);
    operations[Op.REVERT] = new Operation(0xfd, 2, 0,
        NewEnergyCost::getZeroTierCost, OperationActions::revertAction);
    operations[Op.SUICIDE] = new Operation(0xff, 1, 0,
        NewEnergyCost::getZeroTierCost, OperationActions::suicideAction);
  }
  
}
