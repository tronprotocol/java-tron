package org.tron.common.utils;

import java.util.Objects;
import org.junit.Assert;
import org.junit.Test;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.vm.repository.Type;
import org.tron.core.vm.repository.Value;
import org.tron.protos.Protocol;

public class HashCodeTest {

  @Test
  public void test() {
    Type type = new Type();
    type.setType(Type.NORMAL);
    Assert.assertEquals(Integer.valueOf(Type.NORMAL).hashCode(), type.hashCode());
    Protocol.Account account = Protocol.Account.newBuilder().setBalance(100).build();
    Value<Protocol.Account> value = Value.create(new AccountCapsule(account.toByteArray()));
    Assert.assertEquals(Integer.valueOf(
        type.hashCode() + Objects.hashCode(account)).hashCode(), value.hashCode());
  }
}
