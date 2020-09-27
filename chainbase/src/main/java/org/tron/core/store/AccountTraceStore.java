package org.tron.core.store;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.spongycastle.pqc.math.linearalgebra.ByteUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.StringUtil;
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

  private long xor(long l) {
    return l ^ Long.MAX_VALUE;
  }

  public void recordBalanceWithBlock(byte[] address, long number, long balance) {
//    Pair<Long, Long> pair = getPrevBalance(address, number);
//    logger.info("recordBalanceWithBlock===== address:{} number:{} balance:{}", StringUtil.encode58Check(address), number, balance);
    byte[] key = Bytes.concat(address, Longs.toByteArray(xor(number)));
    put(key, new AccountTraceCapsule(balance));
  }

  public Pair<Long, Long> getPrevBalance(byte[] address, long number) {
    byte[] key = Bytes.concat(address, Longs.toByteArray(xor(number)));
    Map<byte[], byte[]> result = revokingDB.getNext(key, 1);

    if (MapUtils.isEmpty(result)) {
      return Pair.of(number, 0L);
    }

    Map.Entry<byte[], byte[]> entry = new ArrayList<>(result.entrySet()).get(0);
    byte[] resultAddress = Arrays.copyOf(entry.getKey(), 21);
    if (!Arrays.equals(address, resultAddress)) {
      return Pair.of(number, 0L);
    }

    try {
      byte[] numberbytes = Arrays.copyOfRange(entry.getKey(), 21, 29);
      return Pair.of(xor(Longs.fromByteArray(numberbytes)), new AccountTraceCapsule(entry.getValue()).getBalance());
    } catch (BadItemException e) {
      return Pair.of(number, 0L);
    }
  }

}
