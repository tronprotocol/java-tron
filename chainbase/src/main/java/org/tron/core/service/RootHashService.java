package org.tron.core.service;

import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.context.GlobalContext;
import org.tron.common.error.TronDBException;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.MerkleRoot;
import org.tron.common.utils.Pair;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.db.TronDatabase;
import org.tron.core.db2.common.Value;
import org.tron.core.store.AccountAssetStore;
import org.tron.core.store.CorruptedCheckpointStore;
import org.tron.protos.Protocol;

@Slf4j(topic = "DB")
@Component
public class RootHashService {

  private static final byte[] HEADER_KEY = "latest_block_header_number".getBytes();

  private static Optional<CorruptedCheckpointStore> corruptedCheckpointStore = Optional.empty();
  private static AccountAssetStore assetStore;
  private static final List<String> stateDbs = Arrays.asList(
      "account", "account-asset", "asset-issue-v2",
      "code", "contract", "contract-state", "storage-row",
      "delegation", "DelegatedResource",
      "exchange-v2",
      "market_account", "market_order", "market_pair_price_to_order", "market_pair_to_price",
      "properties", "proposal",
      "votes", "witness", "witness_schedule"
  );
  private static final byte[] CURRENT_SHUFFLED_WITNESSES = "current_shuffled_witnesses".getBytes();
  private static final String FORK_PREFIX = "FORK_VERSION_";
  private static final String DONE_SUFFIX = "_DONE";
  private static final String ACCOUNT_VOTE_SUFFIX = "-account-vote";
  private static final Set<String> ignoredProperties = Sets.newHashSet(
      "VOTE_REWARD_RATE", "SINGLE_REPEAT", "NON_EXISTENT_ACCOUNT_TRANSFER_MIN",
      "ALLOW_TVM_ASSET_ISSUE", "ALLOW_TVM_STAKE",
      "MAX_VOTE_NUMBER", "MAX_FROZEN_NUMBER", "MAINTENANCE_TIME_INTERVAL",
      "LATEST_SOLIDIFIED_BLOCK_NUM", "BLOCK_NET_USAGE",
      "BLOCK_FILLED_SLOTS_INDEX", "BLOCK_FILLED_SLOTS_NUMBER", "BLOCK_FILLED_SLOTS");

  @Autowired
  public RootHashService(@Autowired CorruptedCheckpointStore corruptedCheckpointStore,
  @Autowired AccountAssetStore assetStore) {
    RootHashService.corruptedCheckpointStore = Optional.ofNullable(corruptedCheckpointStore);
    RootHashService.assetStore = assetStore;
  }

  public static Pair<Optional<Long>, Sha256Hash> getRootHash(Map<byte[], byte[]> rows) {
    try {
      Map<byte[], byte[]> preparedStateData = preparedStateData(rows);
      AtomicReference<Optional<Long>> height = new AtomicReference<>(Optional.empty());
      List<Sha256Hash> ids = Streams.stream(preparedStateData.entrySet()).parallel().map(entry -> {
        if (Arrays.equals(HEADER_KEY, entry.getKey())) {
          height.set(Optional.of(ByteArray.toLong(entry.getValue())));
        }
        return getHash(entry);
      }).sorted().collect(Collectors.toList());
      Sha256Hash actual = MerkleRoot.root(ids);
      long num = height.get().orElseThrow(() -> new TronDBException("blockNum is null"));
      Optional<Sha256Hash> expected = GlobalContext.popBlockHash(num);
      if (expected.isPresent() && !Objects.equals(expected.get(), actual)) {
        corruptedCheckpointStore.ifPresent(TronDatabase::reset);
        corruptedCheckpointStore.ifPresent(store -> store.updateByBatch(rows));
        throw new TronDBException(String.format(
            "Root hash mismatch for blockNum: %s, expected: %s, actual: %s",
            num, expected, actual));
      }
      return new Pair<>(height.get(), actual);
    } catch (IOException e) {
      throw new TronDBException(e);
    }
  }

  private static Map<byte[], byte[]> preparedStateData(Map<byte[], byte[]> rows)
      throws IOException {
    Map<byte[], byte[]> preparedStateData = new HashMap<>(rows.size());
    for (Map.Entry<byte[], byte[]> e : rows.entrySet()) {
      byte[] key = e.getKey();
      String dbName = simpleDecode(key);
      if (!stateDbs.contains(dbName)) {
        continue;
      }
      byte[] realKey = Arrays.copyOfRange(key, dbName.getBytes().length + Integer.BYTES,
          key.length);
      if ("witness_schedule".equals(dbName) && Arrays.equals(realKey, CURRENT_SHUFFLED_WITNESSES)) {
        continue;
      }
      if ("properties".equals(dbName)) {
        String keyStr = new String(realKey);
        if (ignoredProperties.contains(keyStr)
            || keyStr.startsWith(FORK_PREFIX) || keyStr.endsWith(DONE_SUFFIX)) {
          continue;
        }
      }
      byte[] value = e.getValue();
      byte[] realValue = value.length == 1 ? null : Arrays.copyOfRange(value, 1, value.length);
      if (realValue != null) {
        if ("witness".equals(dbName)) {
          realValue = Protocol.Witness.parseFrom(realValue)
              .toBuilder().clearTotalMissed()
              .build().toByteArray(); // ignore totalMissed
        }
        if ("account".equals(dbName)) {
          Protocol.Account account = Protocol.Account.parseFrom(realValue);
          Map<String, Long> assets = new TreeMap<>(assetStore.getAllAssets(account));
          assets.entrySet().removeIf(entry -> entry.getValue() == 0);
          realValue = account.toBuilder().clearAsset().clearAssetV2().clearAssetOptimized()
              .putAllAssetV2(assets)
              .build().toByteArray();
        }
        if ("delegation".equals(dbName) && new String(key).endsWith(ACCOUNT_VOTE_SUFFIX)) {
          Protocol.Account account = Protocol.Account.parseFrom(realValue);
          realValue = Protocol.Account.newBuilder().addAllVotes(account.getVotesList())
              .build().toByteArray();
        }
      }
      if (realValue != null) {
        preparedStateData.put(realKey, realValue);
      } else {
        if (Value.Operator.DELETE.getValue() != value[0]) {
          preparedStateData.put(realKey, ByteString.EMPTY.toByteArray());
        }
      }
    }
    return preparedStateData;
  }

  private static String simpleDecode(byte[] bytes) {
    byte[] lengthBytes = Arrays.copyOf(bytes, Integer.BYTES);
    int length = Ints.fromByteArray(lengthBytes);
    byte[] value = Arrays.copyOfRange(bytes, Integer.BYTES, Integer.BYTES + length);
    return new String(value);
  }

  private static Sha256Hash getHash(Map.Entry<byte[], byte[]> entry) {
    return  Sha256Hash.of(true, Bytes.concat(entry.getKey(), entry.getValue()));
  }
}
