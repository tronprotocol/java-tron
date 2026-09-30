package org.tron.core.vm.nativecontract;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;
import static org.tron.core.config.Parameter.ForkBlockVersionEnum.VERSION_4_8_2_2;
import static org.tron.protos.contract.Common.ResourceCode.BANDWIDTH;
import static org.tron.protos.contract.Common.ResourceCode.ENERGY;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.utils.ForkController;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.vm.nativecontract.param.CancelAllUnfreezeV2Param;
import org.tron.core.vm.nativecontract.param.FreezeBalanceV2Param;
import org.tron.core.vm.nativecontract.param.UnfreezeBalanceV2Param;
import org.tron.core.vm.nativecontract.param.WithdrawExpireUnfreezeParam;
import org.tron.core.vm.program.Program.OutOfTimeException;
import org.tron.core.vm.repository.Repository;
import org.tron.protos.Protocol;
import org.tron.protos.contract.Common.ResourceCode;

public class StakeV2AfterSelfDestructTest {

  private static final long NOW = 1_000L;

  @Test
  public void freezeAfterSelfDestructIsForkGated() throws Exception {
    byte[] ownerAddress = address(1);
    AccountCapsule owner = account(ownerAddress, 0, 0);
    owner.setBalance(TRX_PRECISION);

    Repository repository = mock(Repository.class);
    DynamicPropertiesStore dynamicStore = mock(DynamicPropertiesStore.class);
    when(repository.getDynamicPropertiesStore()).thenReturn(dynamicStore);
    when(repository.getAccount(ownerAddress)).thenReturn(owner);
    when(repository.isSelfDestructed(ownerAddress)).thenReturn(true);

    FreezeBalanceV2Param param = new FreezeBalanceV2Param();
    param.setOwnerAddress(ownerAddress);
    param.setFrozenBalance(TRX_PRECISION);
    param.setResourceType(BANDWIDTH);
    FreezeBalanceV2Processor processor = new FreezeBalanceV2Processor();

    ForkController forkController = mock(ForkController.class);
    try (MockedStatic<ForkController> fork = Mockito.mockStatic(ForkController.class)) {
      fork.when(ForkController::instance).thenReturn(forkController);
      when(forkController.pass(VERSION_4_8_2_2)).thenReturn(false);
      processor.validate(param, repository);

      when(forkController.pass(VERSION_4_8_2_2)).thenReturn(true);
      assertFreezeV2Timeout(() -> processor.validate(param, repository));
    }
  }

  @Test
  public void invalidDelegatedBalancesBlockWithdrawAndCancelAfterFork() throws Exception {
    byte[] ownerAddress = address(1);
    Repository repository = mock(Repository.class);
    DynamicPropertiesStore dynamicStore = mock(DynamicPropertiesStore.class);
    when(repository.getDynamicPropertiesStore()).thenReturn(dynamicStore);
    when(dynamicStore.getLatestBlockHeaderTimestamp()).thenReturn(NOW);

    WithdrawExpireUnfreezeParam withdrawParam = new WithdrawExpireUnfreezeParam();
    withdrawParam.setOwnerAddress(ownerAddress);
    WithdrawExpireUnfreezeProcessor withdrawProcessor =
        new WithdrawExpireUnfreezeProcessor();
    CancelAllUnfreezeV2Param cancelParam = new CancelAllUnfreezeV2Param();
    cancelParam.setOwnerAddress(ownerAddress);
    CancelAllUnfreezeV2Processor cancelProcessor = new CancelAllUnfreezeV2Processor();

    ForkController forkController = mock(ForkController.class);
    try (MockedStatic<ForkController> fork = Mockito.mockStatic(ForkController.class)) {
      fork.when(ForkController::instance).thenReturn(forkController);
      when(forkController.pass(VERSION_4_8_2_2)).thenReturn(false);
      when(repository.getAccount(ownerAddress)).thenReturn(account(ownerAddress, -1, 0));
      withdrawProcessor.validate(withdrawParam, repository);
      cancelProcessor.validate(cancelParam, repository);
      when(repository.getAccount(ownerAddress)).thenReturn(account(ownerAddress, 0, -1));
      withdrawProcessor.validate(withdrawParam, repository);
      cancelProcessor.validate(cancelParam, repository);

      when(forkController.pass(VERSION_4_8_2_2)).thenReturn(true);
      when(repository.getAccount(ownerAddress)).thenReturn(account(ownerAddress, -1, 0));
      assertInvalidDelegatedV2Timeout(
          () -> withdrawProcessor.validate(withdrawParam, repository));
      assertInvalidDelegatedV2Timeout(
          () -> cancelProcessor.validate(cancelParam, repository));
      when(repository.getAccount(ownerAddress)).thenReturn(account(ownerAddress, 0, -1));
      assertInvalidDelegatedV2Timeout(
          () -> withdrawProcessor.validate(withdrawParam, repository));
      assertInvalidDelegatedV2Timeout(
          () -> cancelProcessor.validate(cancelParam, repository));
    }
  }

  @Test
  public void invalidDelegatedBalancesBlockUnfreezeAfterFork() throws Exception {
    byte[] ownerAddress = address(1);
    Repository repository = mock(Repository.class);
    DynamicPropertiesStore dynamicStore = mock(DynamicPropertiesStore.class);
    when(repository.getDynamicPropertiesStore()).thenReturn(dynamicStore);
    when(dynamicStore.getLatestBlockHeaderTimestamp()).thenReturn(NOW);

    UnfreezeBalanceV2Param bandwidthParam = unfreezeParam(ownerAddress, BANDWIDTH);
    UnfreezeBalanceV2Param energyParam = unfreezeParam(ownerAddress, ENERGY);
    UnfreezeBalanceV2Processor processor = new UnfreezeBalanceV2Processor();

    ForkController forkController = mock(ForkController.class);
    try (MockedStatic<ForkController> fork = Mockito.mockStatic(ForkController.class)) {
      fork.when(ForkController::instance).thenReturn(forkController);
      when(forkController.pass(VERSION_4_8_2_2)).thenReturn(false);
      when(repository.getAccount(ownerAddress)).thenReturn(
          accountWithFrozenV2(ownerAddress, -1, 0, BANDWIDTH));
      processor.validate(bandwidthParam, repository);
      when(repository.getAccount(ownerAddress)).thenReturn(
          accountWithFrozenV2(ownerAddress, 0, -1, ENERGY));
      processor.validate(energyParam, repository);

      when(forkController.pass(VERSION_4_8_2_2)).thenReturn(true);
      when(repository.getAccount(ownerAddress)).thenReturn(
          accountWithFrozenV2(ownerAddress, -1, 0, BANDWIDTH));
      assertInvalidDelegatedV2Timeout(
          () -> processor.validate(bandwidthParam, repository));
      when(repository.getAccount(ownerAddress)).thenReturn(
          accountWithFrozenV2(ownerAddress, 0, -1, ENERGY));
      assertInvalidDelegatedV2Timeout(() -> processor.validate(energyParam, repository));
    }
  }

  private static AccountCapsule account(byte[] address, long bandwidth, long energy) {
    Protocol.Account.AccountResource resource = Protocol.Account.AccountResource.newBuilder()
        .setDelegatedFrozenV2BalanceForEnergy(energy)
        .build();
    Protocol.Account account = Protocol.Account.newBuilder()
        .setAddress(ByteString.copyFrom(address))
        .setDelegatedFrozenV2BalanceForBandwidth(bandwidth)
        .setAccountResource(resource)
        .build();
    return new AccountCapsule(account);
  }

  private static AccountCapsule accountWithFrozenV2(
      byte[] address, long bandwidth, long energy, ResourceCode resourceCode) {
    AccountCapsule accountCapsule = account(address, bandwidth, energy);
    if (resourceCode == BANDWIDTH) {
      accountCapsule.addFrozenBalanceForBandwidthV2(TRX_PRECISION);
    } else {
      accountCapsule.addFrozenBalanceForEnergyV2(TRX_PRECISION);
    }
    return accountCapsule;
  }

  private static UnfreezeBalanceV2Param unfreezeParam(
      byte[] ownerAddress, ResourceCode resourceCode) {
    UnfreezeBalanceV2Param param = new UnfreezeBalanceV2Param();
    param.setOwnerAddress(ownerAddress);
    param.setResourceType(resourceCode);
    param.setUnfreezeBalance(TRX_PRECISION);
    return param;
  }

  private static byte[] address(int suffix) {
    byte[] address = new byte[21];
    address[0] = DecodeUtil.addressPreFixByte;
    address[address.length - 1] = (byte) suffix;
    return address;
  }

  private static void assertFreezeV2Timeout(ThrowingRunnable runnable) {
    OutOfTimeException exception = Assert.assertThrows(OutOfTimeException.class, runnable);
    Assert.assertEquals(
        "CPU timeout for FreezeBalanceV2 after SELFDESTRUCT", exception.getMessage());
  }

  private static void assertInvalidDelegatedV2Timeout(ThrowingRunnable runnable) {
    OutOfTimeException exception = Assert.assertThrows(OutOfTimeException.class, runnable);
    Assert.assertEquals("CPU timeout for invalid delegated V2 balance", exception.getMessage());
  }
}
