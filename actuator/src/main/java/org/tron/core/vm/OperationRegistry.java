package org.tron.core.vm;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import org.tron.core.vm.config.VMConfig;

public class OperationRegistry {

  private static final Operation DEFAULT_MLOAD = new Operation(
      Op.MLOAD, 1, 1, EnergyCost::getMloadCost, OperationActions::mLoadAction);

  private static final Operation DEFAULT_MSTORE = new Operation(
      Op.MSTORE, 2, 0, EnergyCost::getMStoreCost, OperationActions::mStoreAction);

  private static final Operation DEFAULT_MSTORE8 = new Operation(
      Op.MSTORE8, 2, 0, EnergyCost::getMStore8Cost, OperationActions::mStore8Action);

  private static final Operation ADJUSTED_MLOAD =
      DEFAULT_MLOAD.adjustCost(EnergyCost::getMloadCost2);

  private static final Operation ADJUSTED_MSTORE =
      DEFAULT_MSTORE.adjustCost(EnergyCost::getMStoreCost2);

  private static final Operation ADJUSTED_MSTORE8 =
      DEFAULT_MSTORE8.adjustCost(EnergyCost::getMStore8Cost2);

  private static final Operation DEFAULT_VOTEWITNESS = new Operation(
      Op.VOTEWITNESS, 4, 1, EnergyCost::getVoteWitnessCost,
      OperationActions::voteWitnessAction, VMConfig::allowTvmVote);

  private static final Operation ADJUSTED_VOTEWITNESS =
      DEFAULT_VOTEWITNESS.adjustCost(EnergyCost::getVoteWitnessCost2);

  private static final Operation OSAKA_VOTEWITNESS =
      DEFAULT_VOTEWITNESS.adjustCost(EnergyCost::getVoteWitnessCost3);

  private static final Operation DEFAULT_SUICIDE = new Operation(
      Op.SUICIDE, 1, 0, EnergyCost::getSuicideCost, OperationActions::suicideAction);

  private static final Operation ADJUSTED_SUICIDE =
      DEFAULT_SUICIDE.adjustCost(EnergyCost::getSuicideCost2);

  private static final Operation RESTRICTED_SUICIDE =
      DEFAULT_SUICIDE.adjustCost(EnergyCost::getSuicideCost3)
          .adjustAction(OperationActions::suicideAction2);

  public enum Version {
    TRON_V1_5,
    // add more
    // TRON_V2,
    // ETH
  }

  private static final Map<Version, JumpTable> tableMap = new HashMap<>();

  // The newest version in use. Bump this when a newer operation set is added,
  // together with newLatestOperationSet() below.
  private static final Version LATEST_VERSION = Version.TRON_V1_5;

  static {
    tableMap.put(LATEST_VERSION, newLatestOperationSet());
  }

  // Constant calls get a dedicated instance of the newest table, isolated from
  // the shared consensus table above.
  private static final JumpTable CONSTANT_CALL_TABLE = newLatestOperationSet();

  // The single place that decides which operation set is the newest.
  private static JumpTable newLatestOperationSet() {
    return newTronV15OperationSet();
  }

  public static JumpTable newTronV10OperationSet() {
    JumpTable table = newBaseOperationSet();
    appendTransferTrc10Operations(table);
    appendConstantinopleOperations(table);
    appendSolidity059Operations(table);
    appendIstanbulOperations(table);
    appendFreezeOperations(table);
    appendVoteOperations(table);
    appendLondonOperations(table);
    return table;
  }

  public static JumpTable newTronV11OperationSet() {
    return newTronV10OperationSet();
  }

  public static JumpTable newTronV12OperationSet() {
    JumpTable table = newTronV11OperationSet();
    appendFreezeV2Operations(table);
    appendDelegateOperations(table);
    return table;
  }

  public static JumpTable newTronV13OperationSet() {
    JumpTable table = newTronV12OperationSet();
    appendShangHaiOperations(table);
    return table;
  }

  public static JumpTable newTronV14OperationSet() {
    JumpTable table = newTronV13OperationSet();
    appendCancunOperations(table);
    return table;
  }

  public static JumpTable newTronV15OperationSet() {
    JumpTable table = newTronV14OperationSet();
    appendOsakaOperations(table);
    return table;
  }

  // Just for warming up class to avoid out_of_time
  public static void init() {}

  public static JumpTable prepareAndGetTable(boolean isConstantCall) {
    JumpTable table = getTable(isConstantCall);
    // Apply configuration-dependent changes once at the top level.
    adjustTable(table);
    return table;
  }

  public static JumpTable getTable(boolean isConstantCall) {
    return isConstantCall ? CONSTANT_CALL_TABLE : tableMap.get(LATEST_VERSION);
  }

  private static void adjustTable(JumpTable table) {
    // Make the corresponding changes, excluding opcode activation.
    adjustMemOperations(table);
    adjustVoteWitness(table);
    adjustSelfdestruct(table);
  }

  public static JumpTable newBaseOperationSet() {
    JumpTable table = new JumpTable();

    table.set(new Operation(
        Op.STOP, 0, 0,
        EnergyCost::getZeroTierCost,
        OperationActions::stopAction));

    table.set(new Operation(
        Op.ADD, 2, 1,
        EnergyCost::getVeryLowTierCost,
        OperationActions::addAction));

    table.set(new Operation(
        Op.MUL, 2, 1,
        EnergyCost::getLowTierCost,
        OperationActions::mulAction));

    table.set(new Operation(
        Op.SUB, 2, 1,
        EnergyCost::getVeryLowTierCost,
        OperationActions::subAction));

    table.set(new Operation(
        Op.DIV, 2, 1,
        EnergyCost::getLowTierCost,
        OperationActions::divAction));

    table.set(new Operation(
        Op.SDIV, 2, 1,
        EnergyCost::getLowTierCost,
        OperationActions::sdivAction));

    table.set(new Operation(
        Op.MOD, 2, 1,
        EnergyCost::getLowTierCost,
        OperationActions::modAction));

    table.set(new Operation(
        Op.SMOD, 2, 1,
        EnergyCost::getLowTierCost,
        OperationActions::sModAction));

    table.set(new Operation(
        Op.ADDMOD, 3, 1,
        EnergyCost::getMidTierCost,
        OperationActions::addModAction));

    table.set(new Operation(
        Op.MULMOD, 3, 1,
        EnergyCost::getMidTierCost,
        OperationActions::mulModAction));

    table.set(new Operation(
        Op.EXP, 2, 1,
        EnergyCost::getExpCost,
        OperationActions::expAction));

    table.set(new Operation(
        Op.SIGNEXTEND, 2, 1,
        EnergyCost::getLowTierCost,
        OperationActions::signExtendAction));

    table.set(new Operation(
        Op.LT, 2, 1,
        EnergyCost::getVeryLowTierCost,
        OperationActions::ltAction));

    table.set(new Operation(
        Op.GT, 2, 1,
        EnergyCost::getVeryLowTierCost,
        OperationActions::gtAction));

    table.set(new Operation(
        Op.SLT, 2, 1,
        EnergyCost::getVeryLowTierCost,
        OperationActions::sltAction));

    table.set(new Operation(
        Op.SGT, 2, 1,
        EnergyCost::getVeryLowTierCost,
        OperationActions::sgtAction));

    table.set(new Operation(
        Op.EQ, 2, 1,
        EnergyCost::getVeryLowTierCost,
        OperationActions::eqAction));

    table.set(new Operation(
        Op.ISZERO, 1, 1,
        EnergyCost::getVeryLowTierCost,
        OperationActions::isZeroAction));

    table.set(new Operation(
        Op.AND, 2, 1,
        EnergyCost::getVeryLowTierCost,
        OperationActions::andAction));

    table.set(new Operation(
        Op.OR, 2, 1,
        EnergyCost::getVeryLowTierCost,
        OperationActions::orAction));

    table.set(new Operation(
        Op.XOR, 2, 1,
        EnergyCost::getVeryLowTierCost,
        OperationActions::xorAction));

    table.set(new Operation(
        Op.NOT, 1, 1,
        EnergyCost::getVeryLowTierCost,
        OperationActions::notAction));

    table.set(new Operation(
        Op.BYTE, 2, 1,
        EnergyCost::getVeryLowTierCost,
        OperationActions::byteAction));

    table.set(new Operation(
        Op.SHA3, 2, 1,
        EnergyCost::getSha3Cost,
        OperationActions::sha3Action));

    table.set(new Operation(
        Op.ADDRESS, 0, 1,
        EnergyCost::getBaseTierCost,
        OperationActions::addressAction));

    table.set(new Operation(
        Op.BALANCE, 1, 1,
        EnergyCost::getBalanceCost,
        OperationActions::balanceAction));

    table.set(new Operation(
        Op.ORIGIN, 0, 1,
        EnergyCost::getBaseTierCost,
        OperationActions::originAction));

    table.set(new Operation(
        Op.CALLER, 0, 1,
        EnergyCost::getBaseTierCost,
        OperationActions::callerAction));

    table.set(new Operation(
        Op.CALLVALUE, 0, 1,
        EnergyCost::getBaseTierCost,
        OperationActions::callValueAction));

    table.set(new Operation(
        Op.CALLDATALOAD, 1, 1,
        EnergyCost::getVeryLowTierCost,
        OperationActions::callDataLoadAction));

    table.set(new Operation(
        Op.CALLDATASIZE, 0, 1,
        EnergyCost::getBaseTierCost,
        OperationActions::callDataSizeAction));

    table.set(new Operation(
        Op.CALLDATACOPY, 3, 0,
        EnergyCost::getCallDataCopyCost,
        OperationActions::callDataCopyAction));

    table.set(new Operation(
        Op.CODESIZE, 0, 1,
        EnergyCost::getBaseTierCost,
        OperationActions::codeSizeAction));

    table.set(new Operation(
        Op.CODECOPY, 3, 0,
        EnergyCost::getCodeCopyCost,
        OperationActions::codeCopyAction));

    table.set(new Operation(
        Op.RETURNDATASIZE, 0, 1,
        EnergyCost::getBaseTierCost,
        OperationActions::returnDataSizeAction));

    table.set(new Operation(
        Op.RETURNDATACOPY, 3, 0,
        EnergyCost::getReturnDataCopyCost,
        OperationActions::returnDataCopyAction));

    table.set(new Operation(
        Op.GASPRICE, 0, 1,
        EnergyCost::getBaseTierCost,
        OperationActions::gasPriceAction));

    table.set(new Operation(
        Op.EXTCODESIZE, 1, 1,
        EnergyCost::getExtCodeSizeCost,
        OperationActions::extCodeSizeAction));

    table.set(new Operation(
        Op.EXTCODECOPY, 4, 0,
        EnergyCost::getExtCodeCopyCost,
        OperationActions::extCodeCopyAction));

    table.set(new Operation(
        Op.BLOCKHASH, 1, 1,
        EnergyCost::getExtTierCost,
        OperationActions::blockHashAction));

    table.set(new Operation(
        Op.COINBASE, 0, 1,
        EnergyCost::getBaseTierCost,
        OperationActions::coinBaseAction));

    table.set(new Operation(
        Op.TIMESTAMP, 0, 1,
        EnergyCost::getBaseTierCost,
        OperationActions::timeStampAction));

    table.set(new Operation(
        Op.NUMBER, 0, 1,
        EnergyCost::getBaseTierCost,
        OperationActions::numberAction));

    table.set(new Operation(
        Op.DIFFICULTY, 0, 1,
        EnergyCost::getBaseTierCost,
        OperationActions::difficultyAction));

    table.set(new Operation(
        Op.GASLIMIT, 0, 1,
        EnergyCost::getBaseTierCost,
        OperationActions::gasLimitAction));

    table.set(new Operation(
        Op.POP, 1, 0,
        EnergyCost::getBaseTierCost,
        OperationActions::popAction));

    table.set(DEFAULT_MLOAD);

    table.set(DEFAULT_MSTORE);

    table.set(DEFAULT_MSTORE8);

    table.set(new Operation(
        Op.SLOAD, 1, 1,
        EnergyCost::getSloadCost,
        OperationActions::sLoadAction));

    table.set(new Operation(
        Op.SSTORE, 2, 0,
        EnergyCost::getSstoreCost,
        OperationActions::sStoreAction));

    table.set(new Operation(
        Op.JUMP, 1, 0,
        EnergyCost::getMidTierCost,
        OperationActions::jumpAction));

    table.set(new Operation(
        Op.JUMPI, 2, 0,
        EnergyCost::getHighTierCost,
        OperationActions::jumpIAction));

    table.set(new Operation(
        Op.PC, 0, 1,
        EnergyCost::getBaseTierCost,
        OperationActions::pcAction));

    table.set(new Operation(
        Op.MSIZE, 0, 1,
        EnergyCost::getBaseTierCost,
        OperationActions::mSizeAction));

    table.set(new Operation(
        Op.GAS, 0, 1,
        EnergyCost::getBaseTierCost,
        OperationActions::gasAction));

    table.set(new Operation(
        Op.JUMPDEST, 0, 0,
        EnergyCost::getSpecialTierCost,
        OperationActions::jumpDestAction));

    for (int i = 0; i < 32; i++) {
      table.set(new Operation(
          Op.PUSH1 + i, 0, 1,
          EnergyCost::getVeryLowTierCost,
          OperationActions::pushAction));
    }

    for (int i = 0; i < 16; i++) {
      table.set(new Operation(
          Op.DUP1 + i, 1 + i, 2 + i,
          EnergyCost::getVeryLowTierCost,
          OperationActions::dupAction));
    }

    for (int i = 0; i < 16; i++) {
      table.set(new Operation(
          Op.SWAP1 + i, 2 + i, 2 + i,
          EnergyCost::getVeryLowTierCost,
          OperationActions::swapAction));
    }

    for (int i = 0; i <= 4; i++) {
      table.set(new Operation(
          Op.LOG0 + i, 2 + i, 0,
          EnergyCost::getLogCost,
          OperationActions::logAction));
    }

    table.set(new Operation(
        Op.CREATE, 3, 1,
        EnergyCost::getCreateCost,
        OperationActions::createAction));

    table.set(new Operation(
        Op.CALL, 7, 1,
        EnergyCost::getCallCost,
        OperationActions::callAction));

    table.set(new Operation(
        Op.CALLCODE, 7, 1,
        EnergyCost::getCallCodeCost,
        OperationActions::callCodeAction));

    table.set(new Operation(
        Op.RETURN, 2, 0,
        EnergyCost::getReturnCost,
        OperationActions::returnAction));

    table.set(new Operation(
        Op.DELEGATECALL, 6, 1,
        EnergyCost::getDelegateCallCost,
        OperationActions::delegateCallAction));

    table.set(new Operation(
        Op.STATICCALL, 6, 1,
        EnergyCost::getStaticCallCost,
        OperationActions::staticCallAction));

    table.set(new Operation(
        Op.REVERT, 2, 0,
        EnergyCost::getRevertCost,
        OperationActions::revertAction));

    table.set(DEFAULT_SUICIDE);

    return table;
  }

  public static void appendTransferTrc10Operations(JumpTable table) {
    BooleanSupplier proposal = VMConfig::allowTvmTransferTrc10;

    table.set(new Operation(
        Op.CALLTOKEN, 8, 1,
        EnergyCost::getCallTokenCost,
        OperationActions::callTokenAction,
        proposal));

    table.set(new Operation(
        Op.TOKENBALANCE, 2, 1,
        EnergyCost::getBalanceCost,
        OperationActions::tokenBalanceAction,
        proposal));

    table.set(new Operation(
        Op.CALLTOKENVALUE, 0, 1,
        EnergyCost::getBaseTierCost,
        OperationActions::callTokenValueAction,
        proposal));

    table.set(new Operation(
        Op.CALLTOKENID, 0, 1,
        EnergyCost::getBaseTierCost,
        OperationActions::callTokenIdAction,
        proposal));
  }

  public static void appendConstantinopleOperations(JumpTable table) {
    BooleanSupplier proposal = VMConfig::allowTvmConstantinople;

    table.set(new Operation(
        Op.SHL, 2, 1,
        EnergyCost::getVeryLowTierCost,
        OperationActions::shlAction,
        proposal));

    table.set(new Operation(
        Op.SHR, 2, 1,
        EnergyCost::getVeryLowTierCost,
        OperationActions::shrAction,
        proposal));

    table.set(new Operation(
        Op.SAR, 2, 1,
        EnergyCost::getVeryLowTierCost,
        OperationActions::sarAction,
        proposal));

    table.set(new Operation(
        Op.CREATE2, 4, 1,
        EnergyCost::getCreate2Cost,
        OperationActions::create2Action,
        proposal));

    table.set(new Operation(
        Op.EXTCODEHASH, 1, 1,
        EnergyCost::getExtCodeHashCost,
        OperationActions::extCodeHashAction,
        proposal));
  }

  public static void appendSolidity059Operations(JumpTable table) {
    BooleanSupplier proposal = VMConfig::allowTvmSolidity059;

    table.set(new Operation(
        Op.ISCONTRACT, 1, 1,
        EnergyCost::getBalanceCost,
        OperationActions::isContractAction,
        proposal));
  }

  public static void appendIstanbulOperations(JumpTable table) {
    BooleanSupplier proposal = VMConfig::allowTvmIstanbul;

    table.set(new Operation(
        Op.CHAINID, 0, 1,
        EnergyCost::getBaseTierCost,
        OperationActions::chainIdAction,
        proposal));

    table.set(new Operation(
        Op.SELFBALANCE, 0, 1,
        EnergyCost::getLowTierCost,
        OperationActions::selfBalanceAction,
        proposal));
  }

  public static void appendFreezeOperations(JumpTable table) {
    BooleanSupplier proposal = VMConfig::allowTvmFreeze;

    table.set(new Operation(
        Op.FREEZE, 3, 1,
        EnergyCost::getFreezeCost,
        OperationActions::freezeAction,
        proposal));

    table.set(new Operation(
        Op.UNFREEZE, 2, 1,
        EnergyCost::getUnfreezeCost,
        OperationActions::unfreezeAction,
        proposal));

    table.set(new Operation(
        Op.FREEZEEXPIRETIME, 2, 1,
        EnergyCost::getFreezeExpireTimeCost,
        OperationActions::freezeExpireTimeAction,
        proposal));
  }

  public static void appendVoteOperations(JumpTable table) {
    BooleanSupplier proposal = VMConfig::allowTvmVote;

    table.set(DEFAULT_VOTEWITNESS);

    table.set(new Operation(
        Op.WITHDRAWREWARD, 0, 1,
        EnergyCost::getWithdrawRewardCost,
        OperationActions::withdrawRewardAction,
        proposal));
  }

  public static void appendLondonOperations(JumpTable table) {
    BooleanSupplier proposal = VMConfig::allowTvmLondon;

    table.set(new Operation(
        Op.BASEFEE, 0, 1,
        EnergyCost::getBaseTierCost,
        OperationActions::baseFeeAction,
        proposal));
  }

  public static void appendFreezeV2Operations(JumpTable table) {
    BooleanSupplier proposal = VMConfig::allowTvmFreezeV2;

    table.set(new Operation(
        Op.FREEZEBALANCEV2, 2, 1,
        EnergyCost::getFreezeBalanceV2Cost,
        OperationActions::freezeBalanceV2Action,
        proposal));

    table.set(new Operation(
        Op.UNFREEZEBALANCEV2, 2, 1,
        EnergyCost::getUnfreezeBalanceV2Cost,
        OperationActions::unfreezeBalanceV2Action,
        proposal));

    table.set(new Operation(
        Op.WITHDRAWEXPIREUNFREEZE, 0, 1,
        EnergyCost::getWithdrawExpireUnfreezeCost,
        OperationActions::withdrawExpireUnfreezeAction,
        proposal));

    table.set(new Operation(
        Op.CANCELALLUNFREEZEV2, 0, 1,
        EnergyCost::getCancelAllUnfreezeV2Cost,
        OperationActions::cancelAllUnfreezeV2Action,
        proposal));
  }

  public static void appendDelegateOperations(JumpTable table) {
    BooleanSupplier proposal = VMConfig::allowTvmFreezeV2;

    table.set(new Operation(
        Op.DELEGATERESOURCE, 3, 1,
        EnergyCost::getDelegateResourceCost,
        OperationActions::delegateResourceAction,
        proposal));

    table.set(new Operation(
        Op.UNDELEGATERESOURCE, 3, 1,
        EnergyCost::getUnDelegateResourceCost,
        OperationActions::unDelegateResourceAction,
        proposal));
  }

  public static void appendShangHaiOperations(JumpTable table) {
    BooleanSupplier proposal = VMConfig::allowTvmShanghai;

    table.set(new Operation(
        Op.PUSH0, 0, 1,
        EnergyCost::getBaseTierCost,
        OperationActions::push0Action,
        proposal));
  }

  public static void appendCancunOperations(JumpTable table) {
    BooleanSupplier proposal = VMConfig::allowTvmCancun;
    BooleanSupplier tvmBlobProposal = VMConfig::allowTvmBlob;

    table.set(new Operation(
        Op.TLOAD, 1, 1,
        EnergyCost::getTLoadCost,
        OperationActions::tLoadAction,
        proposal));

    table.set(new Operation(
        Op.TSTORE, 2, 0,
        EnergyCost::getTStoreCost,
        OperationActions::tStoreAction,
        proposal));

    table.set(new Operation(
        Op.MCOPY, 3, 0,
        EnergyCost::getMCopyCost,
        OperationActions::mCopyAction,
        proposal));

    table.set(new Operation(
        Op.BLOBHASH, 1, 1,
        EnergyCost::getVeryLowTierCost,
        OperationActions::blobHashAction,
        tvmBlobProposal));

    table.set(new Operation(
        Op.BLOBBASEFEE, 0, 1,
        EnergyCost::getBaseTierCost,
        OperationActions::blobBaseFeeAction,
        tvmBlobProposal));
  }

  public static void appendOsakaOperations(JumpTable table) {
    BooleanSupplier proposal = VMConfig::allowTvmOsaka;

    table.set(new Operation(
        Op.CLZ, 1, 1,
        EnergyCost::getLowTierCost,
        OperationActions::clzAction,
        proposal));
  }

  public static void adjustMemOperations(JumpTable table) {
    boolean adjusted = VMConfig.allowHigherLimitForMaxCpuTimeOfOneTx();
    table.set(adjusted ? ADJUSTED_MLOAD : DEFAULT_MLOAD);
    table.set(adjusted ? ADJUSTED_MSTORE : DEFAULT_MSTORE);
    table.set(adjusted ? ADJUSTED_MSTORE8 : DEFAULT_MSTORE8);
  }

  public static void adjustVoteWitness(JumpTable table) {
    if (VMConfig.allowTvmOsaka()) {
      table.set(OSAKA_VOTEWITNESS);
    } else if (VMConfig.allowEnergyAdjustment()) {
      table.set(ADJUSTED_VOTEWITNESS);
    } else {
      table.set(DEFAULT_VOTEWITNESS);
    }
  }

  public static void adjustSelfdestruct(JumpTable table) {
    if (VMConfig.allowTvmSelfdestructRestriction()) {
      table.set(RESTRICTED_SUICIDE);
    } else if (VMConfig.allowEnergyAdjustment()) {
      table.set(ADJUSTED_SUICIDE);
    } else {
      table.set(DEFAULT_SUICIDE);
    }
  }
}
