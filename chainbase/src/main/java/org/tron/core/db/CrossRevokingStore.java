package org.tron.core.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Streams;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
  public void putChainVote(String chainId, String address, byte[] chainVoteInfo) {
    this.put(buildVoteKey(chainId, address), new BytesCapsule(chainVoteInfo));
  }

  public void deleteChainVote(String chainId, String address) {
    this.delete(buildVoteKey(chainId, address));
  }

  public byte[] getChainVote(String chainId, String address) {
    return getUnchecked(buildVoteKey(chainId, address)).getData();
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

  public List<Pair<String, Long>> getChainVoteCountList() {
    return Streams.stream(iterator())
        .filter(
            entry -> "voted_".startsWith(Objects.requireNonNull(ByteArray.toStr(entry.getKey()))))
        .map(entry -> new Pair<String, Long>(ByteArray.toStr(entry.getKey()).substring(6),
            ByteArray.toLong(entry.getValue().getData())))
        .sorted((v1, v2) -> Long.compare(v2.getValue(), v1.getValue()))
        .collect(Collectors.toList());
  }

  public List<Pair<String, Long>> getEligibleChainLists() {
    // todo: 3 should be a variant
    List<Pair<String, Long>> chainVoteCountList = getChainVoteCountList();
    if (chainVoteCountList.size() < 3) {
      return chainVoteCountList;
    } else {
      return getChainVoteCountList().subList(0, 3);
    }
  }

  public void updateParaChains(List<String> chainIds) {
    put(PARACHAINS_KEY, new BytesCapsule(JsonUtil.obj2Json(chainIds).getBytes()));
  }

  public List<String> getParaChainList() {
    BytesCapsule value = getUnchecked(PARACHAINS_KEY);
    if (value != null && !ByteUtil.isNullOrZeroArray(value.getData())) {
      return JsonUtil.json2Obj(ByteArray.toStr(value.getData()),
          new TypeReference<List<String>>() {
          });
    } else {
      return Collections.emptyList();
    }
  }

  public List<byte[]> getRegisterChainList(long offset, long limit) {
    if (offset < 0 || limit < 0) {
      return null;
    }
    return Streams.stream(iterator())
        .filter(entry -> "register_".startsWith(Objects.requireNonNull(ByteArray.toStr(entry.getKey()))))
        .map(entry -> entry.getValue().getData())
        .skip(offset)
        .limit(limit)
        .collect(Collectors.toList());
  }

  public List<byte[]> getCrossChainVoteDetailList(long offset, long limit, String chainId) {
    if (offset < 0 || limit < 0) {
      return null;
    }
    String startStr = "vote_" + chainId;
    return Streams.stream(iterator())
            .filter(entry -> startStr.startsWith(Objects.requireNonNull(ByteArray.toStr(entry.getKey()))))
            .map(entry -> entry.getValue().getData())
            .skip(offset)
            .limit(limit)
            .collect(Collectors.toList());
  }

  public List<Pair<String, Long>> getCrossChainTotalVoteList(long offset, long limit) {
    if (offset < 0 || limit < 0) {
      return null;
    }

    return Streams.stream(iterator())
            .filter(entry -> "voted_".startsWith(Objects.requireNonNull(ByteArray.toStr(entry.getKey()))))
            .map(entry -> new Pair<String, Long>(ByteArray.toStr(entry.getKey()).substring(6),
                    ByteArray.toLong(entry.getValue().getData())))
            .skip(offset)
            .limit(limit)
            .collect(Collectors.toList());
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
    return ("register_" + chainId).getBytes();
  }

  private byte[] buildVoteKey(String chainId, String address) {
    return ("vote_" + chainId + address).getBytes();
  }

  private byte[] buildVoteChainKey(String chainId) {
    return ("voted_" + chainId).getBytes();
  }

}
