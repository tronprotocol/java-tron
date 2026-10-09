package org.tron.core.vm;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.tron.core.vm.config.VMConfig;

public class OperationRegistryTest {

  @Test
  public void constantAndTransactionExecutionsUseDedicatedTables() {
    JumpTable transactionTable = OperationRegistry.prepareAndGetTable(false);
    JumpTable constantCallTable = OperationRegistry.prepareAndGetTable(true);

    assertNotSame(transactionTable, constantCallTable);
    assertSame(transactionTable, OperationRegistry.getTable(false));
    assertSame(constantCallTable, OperationRegistry.getTable(true));
  }

  @Test
  public void transactionExecutionsReuseTable() {
    JumpTable first = OperationRegistry.prepareAndGetTable(false);
    JumpTable second = OperationRegistry.prepareAndGetTable(false);

    assertSame(first, second);
  }

  @Test
  public void constantExecutionsReuseTable() {
    JumpTable first = OperationRegistry.prepareAndGetTable(true);
    JumpTable second = OperationRegistry.prepareAndGetTable(true);

    assertSame(first, second);
  }

  @Test
  public void constantAdjustmentsDoNotMutateTransactionTable() {
    boolean previousHigherLimit = VMConfig.allowHigherLimitForMaxCpuTimeOfOneTx();
    JumpTable transactionTable = OperationRegistry.getTable(false);
    JumpTable constantCallTable = OperationRegistry.getTable(true);
    try {
      VMConfig.initAllowHigherLimitForMaxCpuTimeOfOneTx(0);
      OperationRegistry.adjustMemOperations(transactionTable);
      OperationRegistry.adjustMemOperations(constantCallTable);
      Operation transactionMload = transactionTable.get(Op.MLOAD);
      Operation constantMload = constantCallTable.get(Op.MLOAD);

      VMConfig.initAllowHigherLimitForMaxCpuTimeOfOneTx(1);
      OperationRegistry.adjustMemOperations(constantCallTable);

      assertSame(transactionMload, transactionTable.get(Op.MLOAD));
      assertNotSame(constantMload, constantCallTable.get(Op.MLOAD));
    } finally {
      VMConfig.initAllowHigherLimitForMaxCpuTimeOfOneTx(previousHigherLimit ? 1 : 0);
      OperationRegistry.adjustMemOperations(transactionTable);
      OperationRegistry.adjustMemOperations(constantCallTable);
    }
  }

  @Test
  public void adjustedOperationsReuseCachedVariants() {
    boolean previousHigherLimit = VMConfig.allowHigherLimitForMaxCpuTimeOfOneTx();
    boolean previousEnergyAdjustment = VMConfig.allowEnergyAdjustment();
    boolean previousOsaka = VMConfig.allowTvmOsaka();
    boolean previousSelfdestructRestriction = VMConfig.allowTvmSelfdestructRestriction();
    JumpTable table = OperationRegistry.newTronV15OperationSet();

    Operation defaultMload = table.get(Op.MLOAD);
    Operation defaultMstore = table.get(Op.MSTORE);
    Operation defaultMstore8 = table.get(Op.MSTORE8);
    Operation defaultVoteWitness = table.get(Op.VOTEWITNESS);
    Operation defaultSuicide = table.get(Op.SUICIDE);

    try {
      VMConfig.initAllowHigherLimitForMaxCpuTimeOfOneTx(1);
      VMConfig.initAllowEnergyAdjustment(1);
      VMConfig.initAllowTvmOsaka(0);
      VMConfig.initAllowTvmSelfdestructRestriction(0);
      adjustOperations(table);

      Operation adjustedMload = table.get(Op.MLOAD);
      Operation adjustedMstore = table.get(Op.MSTORE);
      Operation adjustedMstore8 = table.get(Op.MSTORE8);
      Operation adjustedVoteWitness = table.get(Op.VOTEWITNESS);
      Operation adjustedSuicide = table.get(Op.SUICIDE);

      assertNotSame(defaultMload, adjustedMload);
      assertNotSame(defaultMstore, adjustedMstore);
      assertNotSame(defaultMstore8, adjustedMstore8);
      assertNotSame(defaultVoteWitness, adjustedVoteWitness);
      assertNotSame(defaultSuicide, adjustedSuicide);

      adjustOperations(table);
      assertSame(adjustedMload, table.get(Op.MLOAD));
      assertSame(adjustedMstore, table.get(Op.MSTORE));
      assertSame(adjustedMstore8, table.get(Op.MSTORE8));
      assertSame(adjustedVoteWitness, table.get(Op.VOTEWITNESS));
      assertSame(adjustedSuicide, table.get(Op.SUICIDE));

      VMConfig.initAllowTvmOsaka(1);
      VMConfig.initAllowTvmSelfdestructRestriction(1);
      adjustOperations(table);

      Operation osakaVoteWitness = table.get(Op.VOTEWITNESS);
      Operation restrictedSuicide = table.get(Op.SUICIDE);
      assertNotSame(adjustedVoteWitness, osakaVoteWitness);
      assertNotSame(adjustedSuicide, restrictedSuicide);

      adjustOperations(table);
      assertSame(adjustedMload, table.get(Op.MLOAD));
      assertSame(adjustedMstore, table.get(Op.MSTORE));
      assertSame(adjustedMstore8, table.get(Op.MSTORE8));
      assertSame(osakaVoteWitness, table.get(Op.VOTEWITNESS));
      assertSame(restrictedSuicide, table.get(Op.SUICIDE));

      VMConfig.initAllowHigherLimitForMaxCpuTimeOfOneTx(0);
      VMConfig.initAllowEnergyAdjustment(0);
      VMConfig.initAllowTvmOsaka(0);
      VMConfig.initAllowTvmSelfdestructRestriction(0);
      adjustOperations(table);

      assertSame(defaultMload, table.get(Op.MLOAD));
      assertSame(defaultMstore, table.get(Op.MSTORE));
      assertSame(defaultMstore8, table.get(Op.MSTORE8));
      assertSame(defaultVoteWitness, table.get(Op.VOTEWITNESS));
      assertSame(defaultSuicide, table.get(Op.SUICIDE));
    } finally {
      VMConfig.initAllowHigherLimitForMaxCpuTimeOfOneTx(previousHigherLimit ? 1 : 0);
      VMConfig.initAllowEnergyAdjustment(previousEnergyAdjustment ? 1 : 0);
      VMConfig.initAllowTvmOsaka(previousOsaka ? 1 : 0);
      VMConfig.initAllowTvmSelfdestructRestriction(
          previousSelfdestructRestriction ? 1 : 0);
    }
  }

  private static void adjustOperations(JumpTable table) {
    OperationRegistry.adjustMemOperations(table);
    OperationRegistry.adjustVoteWitness(table);
    OperationRegistry.adjustSelfdestruct(table);
  }
}
