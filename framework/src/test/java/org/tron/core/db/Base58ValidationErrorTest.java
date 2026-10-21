package org.tron.core.db;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.tron.common.runtime.Runtime;
import org.tron.common.utils.Base58;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.ContractStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.StoreFactory;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;

public class Base58ValidationErrorTest {

  @Test
  public void missingContractKeepsContractValidationExceptionAndConstantinopleGate()
      throws Exception {
    ChainBaseManager manager = mock(ChainBaseManager.class);
    DynamicPropertiesStore properties = mock(DynamicPropertiesStore.class);
    when(properties.getLatestVersion()).thenReturn(1);
    when(manager.getDynamicPropertiesStore()).thenReturn(properties);
    when(manager.getContractStore()).thenReturn(mock(ContractStore.class));
    when(manager.getAccountStore()).thenReturn(mock(AccountStore.class));
    StoreFactory stores = mock(StoreFactory.class);
    when(stores.getChainBaseManager()).thenReturn(manager);
    TransactionCapsule transaction = new TransactionCapsule(TriggerSmartContract.newBuilder()
        .setContractAddress(ByteString.copyFrom(new byte[16384])).build(),
        ContractType.TriggerSmartContract);
    TransactionTrace trace = new TransactionTrace(transaction, stores, mock(Runtime.class));
    try (MockedStatic<Base58> base58 = mockStatic(Base58.class)) {
      ContractValidateException failure = assertThrows(ContractValidateException.class,
          trace::checkIsConstant);
      assertTrue(failure.getMessage(),
          failure.getMessage().contains(
              "invalid-address(" + StringUtils.repeat("00", 21) + "...,len=16384)"));
      when(properties.getAllowTvmConstantinople()).thenReturn(1L);
      trace.checkIsConstant();
      base58.verifyNoInteractions();
    }
  }
}
