package org.tron.core.db;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.JsonUtil;
import org.tron.common.utils.Pair;
import org.tron.core.capsule.BytesCapsule;

@Slf4j
@Component
public class CrossRevokingStore extends TronStoreWithRevoking<BytesCapsule> {

  private static final byte[] PARACHAINS_KEY = "k_parachain".getBytes();
  public static final String CHAIN_VOTED_PREFIX = "voted_";
  public static final String VOTED_PREFIX = "vote_";
  public static final String CHAIN_REGISTER_PREFIX = "register_";

  // todo: this param should can be customized
  public static final int MAX_PARACHAIN_NUM = 3;


  public CrossRevokingStore() {
    super("cross-revoke-database");
  }

  public void saveTokenMapping(String chainId, String sourceToken, String descToken) {
    this.put(buildTokenKey(chainId, sourceToken),
        new BytesCapsule(ByteArray.fromString(descToken)));
    this.put(descToken.getBytes(), new BytesCapsule(new byte[1]));
  }

  public boolean containMapping(String token) {
    BytesCapsule data = getUnchecked(token.getBytes());
    if (data != null && !ByteUtil.isNullOrZeroArray(data.getData())) {
      return true;
    } else {
      return false;
    }
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

  public byte[] getChainInfo(String chainId) {
    return getUnchecked(buildRegisterKey(chainId)).getData();
  }

  public void putChainInfo(String chainId, byte[] chainInfo) {
    byte[] key = buildRegisterKey(chainId);
    this.put(key, new BytesCapsule(chainInfo));
  }

  // todo: vote-infos are only stored in the db, but not stored on the chain,
  // can track the details of the withdraw and deposit
  public void putChainVote(String chainId, String address, long amount) {
    this.put(buildVoteKey(chainId, address), new BytesCapsule(ByteArray.fromLong(amount)));
  }

  public void deleteChainVote(String chainId, String address) {
    this.delete(buildVoteKey(chainId, address));
  }

  public Long getChainVote(String chainId, String address) {
    BytesCapsule data = getUnchecked(buildVoteKey(chainId, address));
    if (data != null && !ByteUtil.isNullOrZeroArray(data.getData())) {
      return ByteArray.toLong(data.getData());
    } else {
      return 0L;
    }
  }

  public void updateTotalChainVote(String chainId, long amount) {
    BytesCapsule value = getUnchecked(buildVoteChainKey(chainId));
    if (value != null && !ByteUtil.isNullOrZeroArray(value.getData())) {
      amount += ByteArray.toLong(value.getData());
    }
    put(buildVoteChainKey(chainId), new BytesCapsule(ByteArray.fromLong(amount)));
  }

  public long getTotalChainVote(String chainId) {
    BytesCapsule value = getUnchecked(buildVoteChainKey(chainId));
    if (value != null && !ByteUtil.isNullOrZeroArray(value.getData())) {
      return ByteArray.toLong(value.getData());
    } else {
      return 0L;
    }
  }

  // todo: if two chains have the same voted, how to deal?
  public List<Pair<String, Long>> getChainVoteCountList() {
    return prefixQuery(CHAIN_VOTED_PREFIX.getBytes()).stream()
            .map(entry -> new Pair<>(ByteArray.toStr(entry.getKey()).substring(6),
                    ByteArray.toLong(entry.getValue().getData())))
            .sorted((v1, v2) -> Long.compare(v2.getValue(), v1.getValue()))
            .collect(Collectors.toList());
  }

  public List<Pair<String, Long>> getEligibleChainLists() {
    List<Pair<String, Long>> chainVoteCountList = getChainVoteCountList();
    if (chainVoteCountList.size() < MAX_PARACHAIN_NUM) {
      return chainVoteCountList;
    } else {
      return getChainVoteCountList().subList(0, MAX_PARACHAIN_NUM);
    }
  }

  public void updateParaChains(List<String> chainIds) {
    put(PARACHAINS_KEY, new BytesCapsule(JsonUtil.obj2Json(chainIds).getBytes()));
  }

  public List<String> getParaChainList() {
    BytesCapsule value = getUnchecked(PARACHAINS_KEY);
    if (value != null && !ByteUtil.isNullOrZeroArray(value.getData())) {
      return JsonUtil.json2Obj(ByteArray.toStr(value.getData()),
              new TypeReference<List<String>>(){});
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

  private byte[] buildRegisterKey(String chainId) {
    return (CHAIN_REGISTER_PREFIX + chainId).getBytes();
  }

  private byte[] buildVoteKey(String chainId, String address) {
    return (VOTED_PREFIX + chainId + address).getBytes();
  }

  private byte[] buildVoteChainKey(String chainId) {
    return (CHAIN_VOTED_PREFIX + chainId).getBytes();
  }

}
