package org.tron.core.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.config.args.Args;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.core.exception.WhitelistException;

@Component
@Slf4j
public class WhitelistService {
  private final static String TEST_FROM = "";
  private final static String TEST_TO = "";
  private static Map<WrappedByteArray, WrappedByteArray> whitelist = new HashMap<>();

  public WhitelistService() {
    // test
//    whitelist.put(WrappedByteArray.of(ByteArray.fromHexString(TEST_FROM)),
//        WrappedByteArray.of(ByteArray.fromHexString(TEST_TO)));
  }

  // TODO
  @PostConstruct
  public void loadFromConfig() {
    Args.getInstance().getBlacklist().forEach((k, v) -> {
      WrappedByteArray key = WrappedByteArray.of(ByteArray.fromHexString(k));
      WrappedByteArray value = WrappedByteArray.of(ByteArray.fromHexString(v));
      whitelist.put(key, value);
    });
  }

  public static void check(byte[] fromAddress, byte[] toAddress) throws WhitelistException {
    WrappedByteArray from = WrappedByteArray.of(fromAddress);
    WrappedByteArray to = WrappedByteArray.of(toAddress);
    WrappedByteArray value = whitelist.get(from);
    if (Objects.nonNull(value) && !value.equals(to)) {
      throw new WhitelistException("Not the specified address. "
          + "from:" + Wallet.encode58Check(fromAddress)
          + ", to:" + Wallet.encode58Check(toAddress));
    }
  }
}
