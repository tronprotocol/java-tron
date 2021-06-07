package org.tron.core.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Streams;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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

  private static final String PARACHAINS_KEY = "k_parachain";
  private static final String PARA_CHAIN_REGISTER_NUMS_KEY = "parachain_register_num";
  private static final String VOTED_KEY = "voted_";
  private static final String PARACHAIN__HISTORY = "parachain_history";
  private static final String CROSS_CHAIN_UPDATE = "CROSS_CHAIN_UPDATE";

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

  public byte[] getChainInfo(long registerNum) {
    return getUnchecked(buildRegisterKey(String.valueOf(registerNum))).getData();
  }

  public void putChainInfo(long registerNum, byte[] chainInfo) {
    byte[] key = buildRegisterKey(String.valueOf(registerNum));
    this.put(key, new BytesCapsule(chainInfo));
  }

  // todo: vote-infos are only stored in the db, but not stored on the chain,
  // can track the details of the withdraw and deposit
  public void putChainVote(int round, long registerNum, String address, byte[] chainVoteInfo) {
    this.put(buildVoteKey(round, String.valueOf(registerNum), address),
            new BytesCapsule(chainVoteInfo));
  }

  public void deleteChainVote(int round, long registerNum, String address) {
    this.delete(buildVoteKey(round, String.valueOf(registerNum), address));
  }

  public byte[] getChainVote(int round, long registerNum, String address) {
    return getUnchecked(buildVoteKey(round, String.valueOf(registerNum), address)).getData();
  }

  public void updateTotalChainVote(int round, long registerNum, long amount) {
    BytesCapsule value = getUnchecked(buildVoteChainKey(round, String.valueOf(registerNum)));
    if (value != null && !ByteUtil.isNullOrZeroArray(value.getData())) {
      amount += ByteArray.toLong(value.getData());
    }
    put(buildVoteChainKey(round, String.valueOf(registerNum)),
            new BytesCapsule(ByteArray.fromLong(amount)));
  }

  public long getTotalChainVote(int round, long registerNum) {
    BytesCapsule value = getUnchecked(buildVoteChainKey(round, String.valueOf(registerNum)));
    if (value != null && !ByteUtil.isNullOrZeroArray(value.getData())) {
      return ByteArray.toLong(value.getData());
    } else {
      return 0L;
    }
  }

  public List<Pair<Long, Long>> getChainVoteCountList(int round, long minAuctionVoteCount) {
    String startStr = VOTED_KEY + round + "_";
    return Streams.stream(iterator())
            .filter(entry -> Objects.requireNonNull(ByteArray.toStr(entry.getKey()))
                    .startsWith(startStr))
            .filter(entry -> ByteArray.toLong(entry.getValue().getData()) >= minAuctionVoteCount)
            .map(entry -> new Pair<Long, Long>(Long.valueOf(ByteArray.toStr(entry.getKey())
                    .substring((startStr).length())),
                    ByteArray.toLong(entry.getValue().getData())))
            .sorted((v1, v2) -> Long.compare(v2.getValue(), v1.getValue()))
            .collect(Collectors.toList());
  }

  public void updateParaChains(int round, List<String> chainIds) {
    put(buildParaChainsKey(round), new BytesCapsule(JsonUtil.obj2Json(chainIds).getBytes()));
  }

  public void deleteParaChains(int round) {
    delete(buildParaChainsKey(round));
  }

  public void updateParaChainsHistory(List<String> chainIds) {
    BytesCapsule data = getUnchecked(PARACHAIN__HISTORY.getBytes());
    HashSet<String> paraChains;
    if (data == null) {
      paraChains = new HashSet<>(chainIds);
    } else {
      paraChains = JsonUtil.json2Obj(ByteArray.toStr(data.getData()),
              new TypeReference<HashSet<String>>() {
              });
      if (paraChains != null) {
        paraChains.addAll(chainIds);
      }
    }

    if (paraChains != null) {
      put(PARACHAIN__HISTORY.getBytes(), new BytesCapsule(JsonUtil.obj2Json(paraChains).getBytes()));
    }
  }

  public Collection<String> getParaChainsHistory() {
    BytesCapsule data = getUnchecked(PARACHAIN__HISTORY.getBytes());
    if (data != null) {
      return JsonUtil.json2Obj(ByteArray.toStr(data.getData()),
              new TypeReference<HashSet<String>>() {
              });
    } else {
      return Collections.emptySet();
    }
  }

  public List<String> getParaChainList(int round) {
    BytesCapsule value = getUnchecked(buildParaChainsKey(round));
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
            .filter(entry -> Objects.requireNonNull(ByteArray.toStr(entry.getKey())).startsWith("register_"))
            .map(entry -> entry.getValue().getData())
            .skip(offset)
            .limit(limit)
            .collect(Collectors.toList());
  }

  public void updateParaChainRegisterNums(int round, List<Long> registerNums) {
    put(buildParaChainRegisterNumsKey(round),
            new BytesCapsule(JsonUtil.obj2Json(registerNums).getBytes()));
  }

  public void deleteParaChainRegisterNums(int round) {
    delete(buildParaChainRegisterNumsKey(round));
  }

  public List<Long> getParaChainRegisterNumList(int round) {
    BytesCapsule value = getUnchecked(buildParaChainRegisterNumsKey(round));
    if (value != null && !ByteUtil.isNullOrZeroArray(value.getData())) {
      return JsonUtil.json2Obj(ByteArray.toStr(value.getData()),
              new TypeReference<List<Long>>() {
              });
    } else {
      return Collections.emptyList();
    }
  }

  public List<byte[]> getCrossChainVoteDetailList(long offset, long limit, long registerNum, int round) {
    if (offset < 0 || limit < 0) {
      return null;
    }
    String startStr = "vote_" + round + "_" + registerNum;
    return Streams.stream(iterator())
            .filter(entry -> Objects.requireNonNull(ByteArray.toStr(entry.getKey())).startsWith(startStr))
            .map(entry -> entry.getValue().getData())
            .skip(offset)
            .limit(limit)
            .collect(Collectors.toList());
  }

  public List<Pair<Long, Long>> getCrossChainTotalVoteList(long offset, long limit, int round) {
    if (offset < 0 || limit < 0) {
      return null;
    }
    String startStr = VOTED_KEY + round + "_";
    return Streams.stream(iterator())
            .filter(entry ->
                    Objects.requireNonNull(ByteArray.toStr(entry.getKey())).startsWith(startStr))
            .map(entry ->
                    new Pair<Long, Long>(Long.valueOf(ByteArray.toStr(entry.getKey())
                            .substring(startStr.length())),
                    ByteArray.toLong(entry.getValue().getData())))
            .skip(offset)
            .limit(limit)
            .collect(Collectors.toList());
  }

  public long getCrossChainUpdate(long registerNum) {
    BytesCapsule data = getUnchecked(buildKey(CROSS_CHAIN_UPDATE, registerNum));
    if (data != null && !ByteUtil.isNullOrZeroArray(data.getData())) {
      return ByteArray.toLong(data.getData());
    } else {
      return 0L;
    }
  }

  public List<Pair<Long, Long>> getAllCrossChainUpdate () {
    String startStr = CROSS_CHAIN_UPDATE + "_";
    return Streams.stream(iterator())
            .filter(entry ->
                    Objects.requireNonNull(ByteArray.toStr(entry.getKey())).startsWith(startStr))
            .map(entry ->
                    new Pair<Long, Long>(Long.valueOf(ByteArray.toStr(entry.getKey())
                            .substring(startStr.length())),
                            ByteArray.toLong(entry.getValue().getData())))
            .collect(Collectors.toList());
  }

  public void saveCrossChainUpdate(long registerNum, long number) {
    this.put(buildKey(CROSS_CHAIN_UPDATE, registerNum), new BytesCapsule(ByteArray.fromLong(number)));
  }

  public void deleteCrossChainUpdate(long registerNum) {
    delete(buildKey(CROSS_CHAIN_UPDATE, registerNum));
  }

  private byte[] buildKey(String prefix, long registerNum) {
    return (prefix + "_" + registerNum).getBytes();
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

  private byte[] buildVoteKey(int round, String chainId, String address) {
    return ("vote_" + round + "_" + chainId + address).getBytes();
  }

  private byte[] buildVoteChainKey(int round, String chainId) {
    return (VOTED_KEY + round + "_" + chainId).getBytes();
  }

  private byte[] buildParaChainsKey(int round) {
    return (PARACHAINS_KEY + round).getBytes();
  }

  private byte[] buildParaChainRegisterNumsKey(int round) {
    return (PARA_CHAIN_REGISTER_NUMS_KEY + round).getBytes();
  }

}
