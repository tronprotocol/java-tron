package org.tron.core.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.core.capsule.BytesCapsule;

@Slf4j
@Component
public class CrossRevokingStore extends TronStoreWithRevoking<BytesCapsule> {

  public CrossRevokingStore() {
    super("cross-revoke-database");
  }

  public void saveTokenMapping(String sourceToken, String descToken) {
    this.put(buildTokenKey(sourceToken), new BytesCapsule(ByteArray.fromString(descToken)));
  }

  public String getDestTokenFromMapping(String sourceToken) {
    BytesCapsule data = getUnchecked(buildTokenKey(sourceToken));
    if (data != null && !ByteUtil.isNullOrZeroArray(data.getData())) {
      return ByteArray.toStr(data.getData());
    }
    return null;
  }

  public void saveOutTokenCount(String tokenId, long count) {
    this.put(buildOutKey(tokenId), new BytesCapsule(ByteArray.fromLong(count)));
  }

  public Long getOutTokenCount(String tokenId) {
    BytesCapsule data = getUnchecked(buildOutKey(tokenId));
    if (data != null && !ByteUtil.isNullOrZeroArray(data.getData())) {
      return ByteArray.toLong(data.getData());
    } else {
      return null;
    }
  }

  public void saveInTokenCount(String tokenId, long count) {
    this.put(buildInKey(tokenId), new BytesCapsule(ByteArray.fromLong(count)));
  }

  public Long getInTokenCount(String tokenId) {
    BytesCapsule data = getUnchecked(buildInKey(tokenId));
    if (data != null && !ByteUtil.isNullOrZeroArray(data.getData())) {
      return ByteArray.toLong(data.getData());
    } else {
      return null;
    }
  }

  private byte[] buildTokenKey(String tokenId) {
    return ("token_" + tokenId).getBytes();
  }

  private byte[] buildOutKey(String tokenId) {
    return ("out_" + tokenId).getBytes();
  }

  private byte[] buildInKey(String tokenId) {
    return ("in_" + tokenId).getBytes();
  }
}
