package org.tron.core.store;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.AccountTraceCapsule;
import org.tron.core.capsule.BlockBalanceTraceCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.exception.BadItemException;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.BalanceContract.TransactionBalanceTrace;


@Component
@Slf4j(topic = "DB")
public class AccountTraceStore extends TronStoreWithRevoking<AccountTraceCapsule>  {

  @Autowired
  protected AccountTraceStore(@Value("account-trace") String dbName) {
    super(dbName);
  }

  public void recordBalanceWithBlock(byte[] address, long number, long balance) {
    byte[] key = Bytes.concat(address, Longs.toByteArray(number));
    put(key, new AccountTraceCapsule(balance));
  }

}
