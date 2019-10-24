package org.tron.core.store;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.db.TronStoreWithRevoking;

@Slf4j(topic = "DB")
@Component
public class HeaderDynamicPropertiesStore extends TronStoreWithRevoking<BytesCapsule> {

  private static final byte[] LATEST_SOLIDIFIED_BLOCK_NUM = "LATEST_SOLIDIFIED_BLOCK_NUM"
      .getBytes();

  @Autowired
  private HeaderDynamicPropertiesStore(@Value("header-properties") String dbName) {
    super(dbName);
  }

  public void saveLatestSolidifiedBlockNum(String chainId, long number) {
    this.put(buildSolidityKey(chainId), new BytesCapsule(ByteArray.fromLong(number)));
  }

  public long getLatestSolidifiedBlockNum(String chainId) {
    return Optional.ofNullable(getUnchecked(buildSolidityKey(chainId)))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElse(0L);
  }

  public void saveLatestBlockHeaderNumber(String chainId, long n) {
    logger.info("update {} chain latest block header number = {}", chainId, n);
    this.put(buildHeaderKey(chainId), new BytesCapsule(ByteArray.fromLong(n)));
  }

  public long getLatestBlockHeaderNumber(String chainId) {
    return Optional.ofNullable(getUnchecked(buildHeaderKey(chainId)))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found latest block header number"));
  }

  private byte[] buildSolidityKey(String chainId) {
    return ("solidity_" + chainId).getBytes();
  }

  private byte[] buildHeaderKey(String chainId) {
    return ("header_" + chainId).getBytes();
  }

}
