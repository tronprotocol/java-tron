package org.tron.core.capsule;

import org.junit.Assert;
import org.junit.Test;
import org.tron.protos.contract.SmartContractOuterClass;

public class ContractStateCapsuleTest {

  @Test
  public void testCatchUpCycle() {
    ContractStateCapsule capsule = new ContractStateCapsule(
        SmartContractOuterClass.ContractState.newBuilder()
            .setEnergyUsage(1_000_000L)
            .setEnergyFactor(150L)
            .setUpdateCycle(1000L)
            .build());

    Assert.assertFalse(capsule.catchUpToCycle(1000L, 2_000_000L, 120, 200));
    Assert.assertEquals(1000L, capsule.getUpdateCycle());
    Assert.assertEquals(1_000_000L, capsule.getEnergyUsage());
    Assert.assertEquals(150L, capsule.getEnergyFactor());

    Assert.assertTrue(capsule.catchUpToCycle(1010L, 1_000_000L, 110, 200));
    Assert.assertEquals(1010L, capsule.getUpdateCycle());
    Assert.assertEquals(0L, capsule.getEnergyUsage());
    Assert.assertEquals(100L, capsule.getEnergyFactor());

    capsule = new ContractStateCapsule(
        SmartContractOuterClass.ContractState.newBuilder()
            .setEnergyUsage(1_000_000L)
            .setEnergyFactor(150L)
            .setUpdateCycle(1000L)
            .build());

    Assert.assertTrue(capsule.catchUpToCycle(1009L, 1_000_000L, 110, 200));
    Assert.assertEquals(1009L, capsule.getUpdateCycle());
    Assert.assertEquals(0L, capsule.getEnergyUsage());
    Assert.assertEquals(134L, capsule.getEnergyFactor());

    capsule = new ContractStateCapsule(
        SmartContractOuterClass.ContractState.newBuilder()
            .setEnergyUsage(1_000_000L)
            .setEnergyFactor(150L)
            .setUpdateCycle(1000L)
            .build());

    Assert.assertTrue(capsule.catchUpToCycle(1001L, 2_000_000L, 120, 200));
    Assert.assertEquals(1001L, capsule.getUpdateCycle());
    Assert.assertEquals(0L, capsule.getEnergyUsage());
    Assert.assertEquals(142L, capsule.getEnergyFactor());

    capsule = new ContractStateCapsule(
        SmartContractOuterClass.ContractState.newBuilder()
            .setEnergyUsage(1_000_000L)
            .setEnergyFactor(150L)
            .setUpdateCycle(1000L)
            .build());

    Assert.assertTrue(capsule.catchUpToCycle(1001L, 1_000_000L, 120, 200));
    Assert.assertEquals(1001L, capsule.getUpdateCycle());
    Assert.assertEquals(0L, capsule.getEnergyUsage());
    Assert.assertEquals(180L, capsule.getEnergyFactor());

    capsule = new ContractStateCapsule(
        SmartContractOuterClass.ContractState.newBuilder()
            .setEnergyUsage(1_000_000L)
            .setEnergyFactor(150L)
            .setUpdateCycle(1000L)
            .build());

    Assert.assertTrue(capsule.catchUpToCycle(1001L, 1_000_000L, 150, 200));
    Assert.assertEquals(1001L, capsule.getUpdateCycle());
    Assert.assertEquals(0L, capsule.getEnergyUsage());
    Assert.assertEquals(200L, capsule.getEnergyFactor());

    capsule = new ContractStateCapsule(
        SmartContractOuterClass.ContractState.newBuilder()
            .setEnergyUsage(1_000_000L)
            .setEnergyFactor(150L)
            .setUpdateCycle(1000L)
            .build());

    Assert.assertTrue(capsule.catchUpToCycle(1002L, 1_000_000L, 150, 200));
    Assert.assertEquals(1002L, capsule.getUpdateCycle());
    Assert.assertEquals(0L, capsule.getEnergyUsage());
    Assert.assertEquals(175L, capsule.getEnergyFactor());

    capsule = new ContractStateCapsule(
        SmartContractOuterClass.ContractState.newBuilder()
            .setEnergyUsage(1_000_000L)
            .setEnergyFactor(150L)
            .setUpdateCycle(1000L)
            .build());

    Assert.assertTrue(capsule.catchUpToCycle(1003L, 1_000_000L, 150, 200));
    Assert.assertEquals(1003L, capsule.getUpdateCycle());
    Assert.assertEquals(0L, capsule.getEnergyUsage());
    Assert.assertEquals(153L, capsule.getEnergyFactor());

    capsule = new ContractStateCapsule(
        SmartContractOuterClass.ContractState.newBuilder()
            .setEnergyUsage(1_000_000L)
            .setEnergyFactor(150L)
            .setUpdateCycle(1000L)
            .build());

    Assert.assertTrue(capsule.catchUpToCycle(1004L, 1_000_000L, 150, 200));
    Assert.assertEquals(1004L, capsule.getUpdateCycle());
    Assert.assertEquals(0L, capsule.getEnergyUsage());
    Assert.assertEquals(133L, capsule.getEnergyFactor());

    capsule = new ContractStateCapsule(
        SmartContractOuterClass.ContractState.newBuilder()
            .setEnergyUsage(1_000_000L)
            .setEnergyFactor(150L)
            .setUpdateCycle(1000L)
            .build());

    Assert.assertTrue(capsule.catchUpToCycle(1005L, 1_000_000L, 150, 200));
    Assert.assertEquals(1005L, capsule.getUpdateCycle());
    Assert.assertEquals(0L, capsule.getEnergyUsage());
    Assert.assertEquals(117L, capsule.getEnergyFactor());

    capsule = new ContractStateCapsule(
        SmartContractOuterClass.ContractState.newBuilder()
            .setEnergyUsage(1_000_000L)
            .setEnergyFactor(150L)
            .setUpdateCycle(1000L)
            .build());

    Assert.assertTrue(capsule.catchUpToCycle(1005L, 1_000_000L, 150, 200));
    Assert.assertEquals(1005L, capsule.getUpdateCycle());
    Assert.assertEquals(0L, capsule.getEnergyUsage());
    Assert.assertEquals(117L, capsule.getEnergyFactor());

    capsule = new ContractStateCapsule(
        SmartContractOuterClass.ContractState.newBuilder()
            .setEnergyUsage(1_000_000L)
            .setEnergyFactor(150L)
            .setUpdateCycle(1000L)
            .build());

    Assert.assertTrue(capsule.catchUpToCycle(1006L, 1_000_000L, 150, 200));
    Assert.assertEquals(1006L, capsule.getUpdateCycle());
    Assert.assertEquals(0L, capsule.getEnergyUsage());
    Assert.assertEquals(102L, capsule.getEnergyFactor());

    capsule = new ContractStateCapsule(
        SmartContractOuterClass.ContractState.newBuilder()
            .setEnergyUsage(1_000_000L)
            .setEnergyFactor(150L)
            .setUpdateCycle(1000L)
            .build());

    Assert.assertTrue(capsule.catchUpToCycle(1007L, 1_000_000L, 150, 200));
    Assert.assertEquals(1007L, capsule.getUpdateCycle());
    Assert.assertEquals(0L, capsule.getEnergyUsage());
    Assert.assertEquals(100L, capsule.getEnergyFactor());

  }

}
