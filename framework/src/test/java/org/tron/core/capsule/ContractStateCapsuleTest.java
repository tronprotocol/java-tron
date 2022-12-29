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
            .setEnergyFactor(5000L)
            .setUpdateCycle(1000L)
            .build());

    Assert.assertFalse(capsule.catchUpToCycle(1000L, 2_000_000L, 2000L, 10_00L));
    Assert.assertEquals(1000L, capsule.getUpdateCycle());
    Assert.assertEquals(1_000_000L, capsule.getEnergyUsage());
    Assert.assertEquals(5000L, capsule.getEnergyFactor());

    Assert.assertTrue(capsule.catchUpToCycle(1010L, 900_000L, 1000L, 10_000L));
    Assert.assertEquals(1010L, capsule.getUpdateCycle());
    Assert.assertEquals(0L, capsule.getEnergyUsage());
    Assert.assertEquals(3137L, capsule.getEnergyFactor());

    capsule = new ContractStateCapsule(
        SmartContractOuterClass.ContractState.newBuilder()
            .setEnergyUsage(1_000_000L)
            .setEnergyFactor(5000L)
            .setUpdateCycle(1000L)
            .build());

    Assert.assertTrue(capsule.catchUpToCycle(1001L, 2_000_000L, 2000L, 10_000L));
    Assert.assertEquals(1001L, capsule.getUpdateCycle());
    Assert.assertEquals(0L, capsule.getEnergyUsage());
    Assert.assertEquals(4250L, capsule.getEnergyFactor());

    capsule = new ContractStateCapsule(
        SmartContractOuterClass.ContractState.newBuilder()
            .setEnergyUsage(1_000_000L)
            .setEnergyFactor(5000L)
            .setUpdateCycle(1000L)
            .build());

    Assert.assertTrue(capsule.catchUpToCycle(1001L, 1_000_000L, 2000L, 10_000L));
    Assert.assertEquals(1001L, capsule.getUpdateCycle());
    Assert.assertEquals(0L, capsule.getEnergyUsage());
    Assert.assertEquals(4250L, capsule.getEnergyFactor());

    capsule = new ContractStateCapsule(
        SmartContractOuterClass.ContractState.newBuilder()
            .setEnergyUsage(1_000_000L)
            .setEnergyFactor(5000L)
            .setUpdateCycle(1000L)
            .build());

    Assert.assertTrue(capsule.catchUpToCycle(1001L, 900_000L, 2000L, 10_000L));
    Assert.assertEquals(1001L, capsule.getUpdateCycle());
    Assert.assertEquals(0L, capsule.getEnergyUsage());
    Assert.assertEquals(8000L, capsule.getEnergyFactor());

    capsule = new ContractStateCapsule(
        SmartContractOuterClass.ContractState.newBuilder()
            .setEnergyUsage(1_000_000L)
            .setEnergyFactor(5000L)
            .setUpdateCycle(1000L)
            .build());

    Assert.assertTrue(capsule.catchUpToCycle(1001L, 900_000L, 5000L, 10_000L));
    Assert.assertEquals(1001L, capsule.getUpdateCycle());
    Assert.assertEquals(0L, capsule.getEnergyUsage());
    Assert.assertEquals(10_000L, capsule.getEnergyFactor());

    capsule = new ContractStateCapsule(
        SmartContractOuterClass.ContractState.newBuilder()
            .setEnergyUsage(1_000_000L)
            .setEnergyFactor(5000L)
            .setUpdateCycle(1000L)
            .build());

    Assert.assertTrue(capsule.catchUpToCycle(1002L, 900_000L, 5000L, 10_000L));
    Assert.assertEquals(1002L, capsule.getUpdateCycle());
    Assert.assertEquals(0L, capsule.getEnergyUsage());
    Assert.assertEquals(7500L, capsule.getEnergyFactor());

    capsule = new ContractStateCapsule(
        SmartContractOuterClass.ContractState.newBuilder()
            .setEnergyUsage(1_000_000L)
            .setEnergyFactor(5000L)
            .setUpdateCycle(1000L)
            .build());

    Assert.assertTrue(capsule.catchUpToCycle(1003L, 900_000L, 5000L, 10_000L));
    Assert.assertEquals(1003L, capsule.getUpdateCycle());
    Assert.assertEquals(0L, capsule.getEnergyUsage());
    Assert.assertEquals(5312L, capsule.getEnergyFactor());

    capsule = new ContractStateCapsule(
        SmartContractOuterClass.ContractState.newBuilder()
            .setEnergyUsage(1_000_000L)
            .setEnergyFactor(5000L)
            .setUpdateCycle(1000L)
            .build());

    Assert.assertTrue(capsule.catchUpToCycle(1004L, 900_000L, 5000L, 10_000L));
    Assert.assertEquals(1004L, capsule.getUpdateCycle());
    Assert.assertEquals(0L, capsule.getEnergyUsage());
    Assert.assertEquals(3398L, capsule.getEnergyFactor());

    capsule = new ContractStateCapsule(
        SmartContractOuterClass.ContractState.newBuilder()
            .setEnergyUsage(1_000_000L)
            .setEnergyFactor(5000L)
            .setUpdateCycle(1000L)
            .build());

    Assert.assertTrue(capsule.catchUpToCycle(1005L, 900_000L, 5000L, 10_000L));
    Assert.assertEquals(1005L, capsule.getUpdateCycle());
    Assert.assertEquals(0L, capsule.getEnergyUsage());
    Assert.assertEquals(1723L, capsule.getEnergyFactor());

    capsule = new ContractStateCapsule(
        SmartContractOuterClass.ContractState.newBuilder()
            .setEnergyUsage(1_000_000L)
            .setEnergyFactor(5000L)
            .setUpdateCycle(1000L)
            .build());

    Assert.assertTrue(capsule.catchUpToCycle(1005L, 900_000L, 5000L, 10_000L));
    Assert.assertEquals(1005L, capsule.getUpdateCycle());
    Assert.assertEquals(0L, capsule.getEnergyUsage());
    Assert.assertEquals(1723L, capsule.getEnergyFactor());

    capsule = new ContractStateCapsule(
        SmartContractOuterClass.ContractState.newBuilder()
            .setEnergyUsage(1_000_000L)
            .setEnergyFactor(5000L)
            .setUpdateCycle(1000L)
            .build());

    Assert.assertTrue(capsule.catchUpToCycle(1006L, 900_000L, 5000L, 10_000L));
    Assert.assertEquals(1006L, capsule.getUpdateCycle());
    Assert.assertEquals(0L, capsule.getEnergyUsage());
    Assert.assertEquals(258L, capsule.getEnergyFactor());

    capsule = new ContractStateCapsule(
        SmartContractOuterClass.ContractState.newBuilder()
            .setEnergyUsage(1_000_000L)
            .setEnergyFactor(5000L)
            .setUpdateCycle(1000L)
            .build());

    Assert.assertTrue(capsule.catchUpToCycle(1007L, 900_000L, 5000L, 10_000L));
    Assert.assertEquals(1007L, capsule.getUpdateCycle());
    Assert.assertEquals(0L, capsule.getEnergyUsage());
    Assert.assertEquals(0L, capsule.getEnergyFactor());

  }

}
