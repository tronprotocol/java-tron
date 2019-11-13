package org.tron.core.store;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
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

  public void saveLatestBlockHeaderHash(String chainId, String blockHash) {
    this.put(buildHeaderHashKey(chainId), new BytesCapsule(blockHash.getBytes()));
  }

  public String getLatestBlockHeaderHash(String chainId) {
    return Optional.ofNullable(getUnchecked(buildHeaderHashKey(chainId)))
        .map(BytesCapsule::getData)
        .map(String::new)
        .orElseThrow(
            () -> new IllegalArgumentException("not found latest block header hash"));
  }

  public void saveCurrentSrList(String chainId, List<ByteString> srList) {
    List<String> srs = srList.stream().map(bytes -> bytes.toStringUtf8())
        .collect(Collectors.toList());
    logger.info("update {} chain sr list = {}", chainId, srs);

    this.put(buildSrListKey(chainId), new BytesCapsule(JSON.toJSONString(srs).getBytes()));
  }

  public List<ByteString> getCurrentSrList(String chainId) {
    BytesCapsule bytesCapsule = getUnchecked(buildSrListKey(chainId));
    if (bytesCapsule == null || ByteUtil.isNullOrZeroArray(bytesCapsule.getData())) {
      return null;
    }
    String srString = new String(bytesCapsule.getData());
    return JSON.parseArray(srString, String.class).stream().map(sr -> ByteString.copyFromUtf8(sr))
        .collect(Collectors.toList());
  }

  private byte[] buildSolidityKey(String chainId) {
    return ("solidity_" + chainId).getBytes();
  }

  private byte[] buildHeaderKey(String chainId) {
    return ("header_" + chainId).getBytes();
  }

  private byte[] buildHeaderHashKey(String chainId) {
    return ("headerHash_" + chainId).getBytes();
  }

  private byte[] buildSrListKey(String chainId) {
    return ("srList_" + chainId).getBytes();
  }

}
