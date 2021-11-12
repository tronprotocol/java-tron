package org.tron.core.vm;

public class OperationRegistry {

  private static final int STOP = 0x00;
  private static final int ADD = 0x01;
  private static final int MUL = 0x02;
  private static final int SUB = 0x03;
  private static final int DIV = 0x04;
  private static final int SDIV = 0x05;
  private static final int MOD = 0x06;
  private static final int SMOD = 0x07;
  private static final int ADDMOD = 0x08;
  private static final int MULMOD = 0x09;
  private static final int EXP = 0x0a;
  private static final int SIGNEXTEND = 0x0b;
  private static final int LT = 0X10;
  private static final int GT = 0X11;
  private static final int SLT = 0X12;
  private static final int SGT = 0X13;
  private static final int EQ = 0X14;
  private static final int ISZERO = 0x15;
  private static final int AND = 0x16;
  private static final int OR = 0x17;
  private static final int XOR = 0x18;
  private static final int NOT = 0x19;
  private static final int BYTE = 0x1a;
  private static final int SHL = 0x1b;
  private static final int SHR = 0x1c;
  private static final int SAR = 0x1d;
  private static final int SHA3 = 0x20;
  private static final int ADDRESS = 0x30;
  private static final int BALANCE = 0x31;
  private static final int ORIGIN = 0x32;
  private static final int CALLER = 0x33;
  private static final int CALLVALUE = 0x34;
  private static final int CALLDATALOAD = 0x35;
  private static final int CALLDATASIZE = 0x36;
  private static final int CALLDATACOPY = 0x37;
  private static final int CODESIZE = 0x38;
  private static final int CODECOPY = 0x39;
  private static final int RETURNDATASIZE = 0x3d;
  private static final int RETURNDATACOPY = 0x3e;
  private static final int GASPRICE = 0x3a;
  private static final int EXTCODESIZE = 0x3b;
  private static final int EXTCODECOPY = 0x3c;
  private static final int EXTCODEHASH = 0x3f;
  private static final int BLOCKHASH = 0x40;
  private static final int COINBASE = 0x41;
  private static final int TIMESTAMP = 0x42;
  private static final int NUMBER = 0x43;
  private static final int DIFFICULTY = 0x44;
  private static final int GASLIMIT = 0x45;
  private static final int CHAINID = 0x46;
  private static final int SELFBALANCE = 0x47;
  private static final int BASEFEE = 0x48;
  private static final int POP = 0x50;
  private static final int MLOAD = 0x51;
  private static final int MSTORE = 0x52;
  private static final int MSTORE8 = 0x53;
  private static final int SLOAD = 0x54;
  private static final int SSTORE = 0x55;
  private static final int JUMP = 0x56;
  private static final int JUMPI = 0x57;
  private static final int PC = 0x58;
  private static final int MSIZE = 0x59;
  private static final int GAS = 0x5a;
  private static final int JUMPDEST = 0x5b;
  private static final int PUSH = 0x60;
  private static final int DUP = 0x80;
  private static final int SWAP = 0x90;
  private static final int LOG = 0xa0;
  private static final int CALLTOKEN = 0xd0;
  private static final int TOKENBALANCE = 0xd1;
  private static final int CALLTOKENVALUE = 0xd2;
  private static final int CALLTOKENID = 0xd3;
  private static final int ISCONTRACT = 0xd4;
  private static final int FREEZE = 0xd5;
  private static final int UNFREEZE = 0xd6;
  private static final int FREEZEEXPIRETIME = 0xd7;
  private static final int VOTEWITNESS = 0xd8;
  private static final int WITHDRAWREWARD = 0xd9;
  private static final int CREATE = 0xf0;
  private static final int CALL = 0xf1;
  private static final int CALLCODE = 0xf2;
  private static final int DELEGATECALL = 0xf4;
  private static final int STATICCALL = 0xfa;
  private static final int RETURN = 0xf3;
  private static final int CREATE2 = 0xf5;
  private static final int REVERT = 0xfd;
  private static final int SUICIDE = 0xff;

  public static Operation[] getBaseOperations() {
    Operation[] operations = new Operation[256];
    operations[STOP] = new Operation(0x00, 0, 0,
        NewEnergyCost::getZeroTierCost, OperationActions::stopAction);
    operations[ADD] = new Operation(0x01, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::addAction);
    operations[MUL] = new Operation(0x02, 2, 1,
        NewEnergyCost::getLowTierCost, OperationActions::mulAction);
    operations[SUB] = new Operation(0x03, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::subAction);
    operations[DIV] = new Operation(0x04, 2, 1,
        NewEnergyCost::getLowTierCost, OperationActions::divAction);
    operations[SDIV] = new Operation(0x05, 2, 1,
        NewEnergyCost::getLowTierCost, OperationActions::sdivAction);
    operations[MOD] = new Operation(0x06, 2, 1,
        NewEnergyCost::getLowTierCost, OperationActions::modAction);
    operations[SMOD] = new Operation(0x07, 2, 1,
        NewEnergyCost::getLowTierCost, OperationActions::sModAction);
    operations[ADDMOD] = new Operation(0x08, 3, 1,
        NewEnergyCost::getMidTierCost, OperationActions::addModAction);
    operations[MULMOD] = new Operation(0x09, 3, 1,
        NewEnergyCost::getMidTierCost, OperationActions::mulModAction);
    operations[EXP] = new Operation(0x0a, 2, 1,
        NewEnergyCost::getExpCost, OperationActions::expAction);
    operations[SIGNEXTEND] = new Operation(0x0b, 2, 1,
        NewEnergyCost::getLowTierCost, OperationActions::signExtendAction);
    operations[LT] = new Operation(0X10, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::ltAction);
    operations[GT] = new Operation(0X11, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::gtAction);
    operations[SLT] = new Operation(0X12, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::sltAction);
    operations[SGT] = new Operation(0X13, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::sgtAction);
    operations[EQ] = new Operation(0X14, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::eqAction);
    operations[ISZERO] = new Operation(0x15, 1, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::isZeroAction);
    operations[AND] = new Operation(0x16, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::andAction);
    operations[OR] = new Operation(0x17, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::orAction);
    operations[XOR] = new Operation(0x18, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::xorAction);
    operations[NOT] = new Operation(0x19, 1, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::notAction);
    operations[BYTE] = new Operation(0x1a, 1, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::byteAction);
    operations[SHL] = new Operation(0x1b, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::shlAction);
    operations[SHR] = new Operation(0x1c, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::shrAction);
    operations[SAR] = new Operation(0x1d, 2, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::sarAction);
    operations[SHA3] = new Operation(0x20, 2, 1,
        NewEnergyCost::getSha3Cost, OperationActions::sha3Action);
    operations[ADDRESS] = new Operation(0x30, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::addressAction);
    operations[BALANCE] = new Operation(0x31, 1, 1,
        NewEnergyCost::getBalanceCost, OperationActions::balanceAction);
    operations[ORIGIN] = new Operation(0x32, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::originAction);
    operations[CALLER] = new Operation(0x33, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::callerAction);
    operations[CALLVALUE] = new Operation(0x34, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::callValueAction);
    operations[CALLDATALOAD] = new Operation(0x35, 1, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::callDataLoadAction);
    operations[CALLDATASIZE] = new Operation(0x36, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::callDataSizeAction);
    operations[CALLDATACOPY] = new Operation(0x37, 3, 0,
        NewEnergyCost::getVeryLowTierCost, OperationActions::callDataCopyAction);
    operations[CODESIZE] = new Operation(0x38, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::codeSizeAction);
    operations[CODECOPY] = new Operation(0x39, 3, 0,
        NewEnergyCost::getVeryLowTierCost, OperationActions::codeCopyAction);
    operations[RETURNDATASIZE] = new Operation(0x3d, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::returnDataSizeAction);
    operations[RETURNDATACOPY] = new Operation(0x3e, 3, 0,
        NewEnergyCost::getVeryLowTierCost, OperationActions::returnDataCopyAction);
    operations[GASPRICE] = new Operation(0x3a, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::gasPriceAction);
    operations[EXTCODESIZE] = new Operation(0x3b, 1, 1,
        NewEnergyCost::getExtTierCost, OperationActions::extCodeSizeAction);
    operations[EXTCODECOPY] = new Operation(0x3c, 4, 0,
        NewEnergyCost::getExtTierCost, OperationActions::extCodeCopyAction);
    operations[EXTCODEHASH] = new Operation(0x3f, 1, 1,
        NewEnergyCost::getExtTierCost, OperationActions::extCodeHashAction);
    operations[BLOCKHASH] = new Operation(0x40, 1, 1,
        NewEnergyCost::getExtTierCost, OperationActions::blockHashAction);
    operations[COINBASE] = new Operation(0x41, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::coinBaseAction);
    operations[TIMESTAMP] = new Operation(0x42, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::timeStampAction);
    operations[NUMBER] = new Operation(0x43, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::numberAction);
    operations[DIFFICULTY] = new Operation(0x44, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::difficultyAction);
    operations[GASLIMIT] = new Operation(0x45, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::gasLimitAction);
    operations[CHAINID] = new Operation(0x46, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::chainIdAction);
    operations[SELFBALANCE] = new Operation(0x47, 0, 1,
        NewEnergyCost::getLowTierCost, OperationActions::selfBalanceAction);
    operations[BASEFEE] = new Operation(0x48, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::baseFeeAction);
    operations[POP] = new Operation(0x50, 1, 0,
        NewEnergyCost::getBaseTierCost, OperationActions::popAction);
    operations[MLOAD] = new Operation(0x51, 1, 1,
        NewEnergyCost::getVeryLowTierCost, OperationActions::mLoadAction);
    operations[MSTORE] = new Operation(0x52, 2, 0,
        NewEnergyCost::getVeryLowTierCost, OperationActions::mStoreAction);
    operations[MSTORE8] = new Operation(0x53, 2, 0,
        NewEnergyCost::getVeryLowTierCost, OperationActions::mStore8Action);
    operations[SLOAD] = new Operation(0x54, 1, 1,
        NewEnergyCost::getSloadCost, OperationActions::sLoadAction);
    operations[SSTORE] = new Operation(0x55, 2, 0,
        NewEnergyCost::getSstoreCost, OperationActions::sStoreAction);
    operations[JUMP] = new Operation(0x56, 1, 0,
        NewEnergyCost::getMidTierCost, OperationActions::jumpAction);
    operations[JUMPI] = new Operation(0x57, 2, 0,
        NewEnergyCost::getHighTierrCost, OperationActions::jumpIAction);
    operations[PC] = new Operation(0x58, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::pcAction);
    operations[MSIZE] = new Operation(0x59, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::mSizeAction);
    operations[GAS] = new Operation(0x5a, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::gasAction);
    operations[JUMPDEST] = new Operation(0x5b, 0, 0,
        NewEnergyCost::getSpecialTierCost, OperationActions::jumpDestAction);
    for (int i = 0; i <= 31; i++) {
      operations[PUSH + i] = new Operation(0x60 + i, 0, 1,
          NewEnergyCost::getVeryLowTierCost, OperationActions::pushAction);
    }
    for (int i = 0; i <= 15; i++) {
      operations[DUP + i] = new Operation(0x80 + i, 1 + i, 2 + i,
          NewEnergyCost::getVeryLowTierCost, OperationActions::dupAction);
    }
    for (int i = 0; i <= 15; i++) {
      operations[SWAP + i] = new Operation(0x90 + i, 2 + i, 2 + i,
          NewEnergyCost::getVeryLowTierCost, OperationActions::swapAction);
    }
    for (int i = 0; i <= 4; i++) {
      operations[LOG + i] = new Operation(0xa0 + i, 2 + i, 0,
          NewEnergyCost::getLogCost, OperationActions::logAction);
    }
    operations[CALLTOKEN] = new Operation(0xd0, 8, 0,
        NewEnergyCost::getCallCost, OperationActions::callAction);
    operations[TOKENBALANCE] = new Operation(0xd1, 2, 1,
        NewEnergyCost::getBalanceCost, OperationActions::tokenBalanceAction);
    operations[CALLTOKENVALUE] = new Operation(0xd2, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::callTokenValueAction);
    operations[CALLTOKENID] = new Operation(0xd3, 0, 1,
        NewEnergyCost::getBaseTierCost, OperationActions::callTokenIdAction);
    operations[ISCONTRACT] = new Operation(0xd4, 1, 1,
        NewEnergyCost::getBalanceCost, OperationActions::isContractAction);
    operations[FREEZE] = new Operation(0xd5, 3, 1,
        NewEnergyCost::getFreezeCost, OperationActions::freezeAction);
    operations[UNFREEZE] = new Operation(0xd6, 2, 1,
        NewEnergyCost::getUnfreezeCost, OperationActions::unfreezeAction);
    operations[FREEZEEXPIRETIME] = new Operation(0xd7, 2, 1,
        NewEnergyCost::getFreezeExpireTimeCost, OperationActions::freezeExpireTimeAction);
    operations[VOTEWITNESS] = new Operation(0xd8, 4, 1,
        NewEnergyCost::getVoteWitnessCost, OperationActions::voteWitnessAction);
    operations[WITHDRAWREWARD] = new Operation(0xd9, 0, 1,
        NewEnergyCost::getWithdrawRewardCost, OperationActions::withdrawRewardAction);
    operations[CREATE] = new Operation(0xf0, 3, 1,
        NewEnergyCost::getCreateCost, OperationActions::createAction);
    operations[CALL] = new Operation(0xf1, 7, 1,
        NewEnergyCost::getCallCost, OperationActions::callAction);
    operations[CALLCODE] = new Operation(0xf2, 7, 1,
        NewEnergyCost::getCallCost, OperationActions::callAction);
    operations[RETURN] = new Operation(0xf3, 2, 0,
        NewEnergyCost::getZeroTierCost, OperationActions::returnAction);
    operations[DELEGATECALL] = new Operation(0xf4, 6, 1,
        NewEnergyCost::getCallCost, OperationActions::callAction);
    operations[CREATE2] = new Operation(0xf5, 4, 1,
        NewEnergyCost::getCreate2Cost, OperationActions::create2Action);
    operations[STATICCALL] = new Operation(0xfa, 6, 1,
        NewEnergyCost::getCallCost, OperationActions::callAction);
    operations[REVERT] = new Operation(0xfd, 2, 0,
        NewEnergyCost::getZeroTierCost, OperationActions::revertAction);
    operations[SUICIDE] = new Operation(0xff, 1, 0,
        NewEnergyCost::getZeroTierCost, OperationActions::suicideAction);

    return operations;
  }

}
