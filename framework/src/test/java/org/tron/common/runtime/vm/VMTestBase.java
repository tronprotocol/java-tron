package org.tron.common.runtime.vm;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.tron.common.BaseMethodTest;
import org.tron.common.runtime.Runtime;
import org.tron.core.Wallet;
import org.tron.core.db.Manager;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.repository.Repository;
import org.tron.core.vm.repository.RepositoryImpl;
import org.tron.protos.Protocol.AccountType;

@Slf4j
public class VMTestBase extends BaseMethodTest {

  protected Manager manager;
  protected Repository rootRepository;
  protected String OWNER_ADDRESS;
  protected Runtime runtime;

  @Override
  protected String[] extraArgs() {
    return new String[]{"--debug"};
  }

  @Override
  protected void afterInit() {
    manager = dbManager;
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    rootRepository = RepositoryImpl.createRoot(StoreFactory.getInstance());
    rootRepository.createAccount(Hex.decode(OWNER_ADDRESS), AccountType.Normal);
    rootRepository.addBalance(Hex.decode(OWNER_ADDRESS), 30000000000000L);
    rootRepository.commit();
  }
}
