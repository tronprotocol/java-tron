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

  public void saveTokenMapping(String chainId, String sourceToken, String descToken) {
    this.put(buildTokenKey(chainId, sourceToken),
        new BytesCapsule(ByteArray.fromString(descToken)));
  }

  public String getDestTokenFromMapping(String chainId, String sourceToken) {
    BytesCapsule data = getUnchecked(buildTokenKey(chainId, sourceToken));
    if (data != null && !ByteUtil.isNullOrZeroArray(data.getData())) {
      return ByteArray.toStr(data.getData());
    }
    return null;
  }

  public void saveOutTokenCount(String toChainId, String tokenId, long count) {
    this.put(buildOutKey(toChainId, tokenId), new BytesCapsule(ByteArray.fromLong(count)));
  }

  public Long getOutTokenCount(String toChainId, String tokenId) {
    BytesCapsule data = getUnchecked(buildOutKey(toChainId, tokenId));
    if (data != null && !ByteUtil.isNullOrZeroArray(data.getData())) {
      return ByteArray.toLong(data.getData());
    } else {
      return null;
    }
  }

  public void saveInTokenCount(String fromChainId, String tokenId, long count) {
    this.put(buildInKey(fromChainId, tokenId), new BytesCapsule(ByteArray.fromLong(count)));
  }

  public Long getInTokenCount(String fromChainId, String tokenId) {
    BytesCapsule data = getUnchecked(buildInKey(fromChainId, tokenId));
    if (data != null && !ByteUtil.isNullOrZeroArray(data.getData())) {
      return ByteArray.toLong(data.getData());
    } else {
      return null;
    }
  }

  private byte[] buildTokenKey(String chainId, String tokenId) {
    return ("token_" + chainId + "_" + tokenId).getBytes();
  }

  private byte[] buildOutKey(String toChainId, String tokenId) {
    return ("out_" + toChainId + "_" + tokenId).getBytes();
  }

  private byte[] buildInKey(String fromChainId, String tokenId) {
    return ("in_" + fromChainId + "_" + tokenId).getBytes();
  }
}
