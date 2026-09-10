package org.tron.core.db2.core;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import com.google.common.primitives.UnsignedBytes;
import com.google.protobuf.InvalidProtocolBufferException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.tron.core.db2.common.Key;
import org.tron.core.db2.common.Value;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.protos.Protocol.Account;

/** Materializes coupled physical writes once, inside the active revocable block layer. */
public final class P66CoupledMutationMaterializer {
  private static final byte[] FLAG =
      "ALLOW_ASSET_OPTIMIZATION".getBytes(StandardCharsets.US_ASCII);
  private final Chainbase accounts;
  private final Chainbase assets;
  private final Chainbase properties;

  public P66CoupledMutationMaterializer(Chainbase accounts, Chainbase assets,
      Chainbase properties) {
    this.accounts = accounts;
    this.assets = assets;
    this.properties = properties;
  }

  public Statistics materialize() {
    if (!Snapshot.isImpl(accounts.getHead()) || !Snapshot.isImpl(assets.getHead())
        || !Snapshot.isImpl(properties.getHead())) {
      throw new IllegalStateException("P66 materialization requires active Snapshot layers");
    }
    Statistics stats = new Statistics();
    byte[] flag = properties.getUnchecked(FLAG);
    if (flag == null || flag.length != Long.BYTES
        || (Longs.fromByteArray(flag) != 0 && Longs.fromByteArray(flag) != 1)) {
      throw new IllegalStateException("P66 post-state property must be 0 or 1");
    }
    byte[] previous = properties.getHead().getPrevious().get(FLAG);
    if (previous != null && previous.length == Long.BYTES
        && Longs.fromByteArray(previous) == 1 && Longs.fromByteArray(flag) == 0) {
      throw new IllegalStateException("P66 phase cannot move backwards");
    }
    if (Longs.fromByteArray(flag) == 0) {
      return stats;
    }
    // Build the complete deterministic plan before modifying either Snapshot map.
    Map<byte[], byte[]> accountPlan = new TreeMap<>(UnsignedBytes.lexicographicalComparator());
    Map<byte[], byte[]> assetPlan = new TreeMap<>(UnsignedBytes.lexicographicalComparator());
    List<Map.Entry<Key, Value>> changed = new ArrayList<>();
    ((SnapshotImpl) accounts.getHead()).getDb().forEach(changed::add);
    for (Map.Entry<Key, Value> entry : changed) {
      byte[] address = entry.getKey().getBytes();
      if (address.length != 21) {
        throw new IllegalStateException("P66 Account key must be exactly 21 bytes");
      }
      stats.changedAccounts++;
      if (entry.getValue().getOperator() == Value.Operator.DELETE) {
        stats.prefixQueries++;
        Map<WrappedByteArray, byte[]> rows = assets.prefixQuery(address);
        stats.prefixRows += rows.size();
        rows.forEach((key, value) -> assetPlan.put(key.getBytes(), null));
        continue;
      }
      Account account;
      try {
        account = Account.parseFrom(entry.getValue().getBytes());
      } catch (InvalidProtocolBufferException invalid) {
        throw new IllegalStateException("Invalid P66 Account bytes", invalid);
      }
      if (!Arrays.equals(address, account.getAddress().toByteArray())) {
        throw new IllegalStateException("P66 Account key/address mismatch");
      }
      if (account.getAssetV2Map().isEmpty()) {
        stats.emptyAssetSkips++;
      }
      boolean migrated = false;
      for (Map.Entry<String, Long> asset : account.getAssetV2Map().entrySet()) {
        byte[] key = Bytes.concat(address, asset.getKey().getBytes(StandardCharsets.UTF_8));
        long value = asset.getValue();
        if (!account.getAssetOptimized() && value == 0) {
          continue;
        }
        assetPlan.put(key, value == 0 ? null : Longs.toByteArray(value));
        migrated |= !account.getAssetOptimized() && value != 0;
      }
      if (migrated) {
        stats.migratedAccounts++;
      }
      accountPlan.put(address, account.toBuilder().clearAsset().clearAssetV2()
          .setAssetOptimized(true).build().toByteArray());
    }
    assetPlan.forEach((key, value) -> {
      if (value == null) {
        assets.delete(key);
        stats.assetDeletes++;
      } else {
        assets.put(key, value);
        stats.assetPuts++;
      }
    });
    accountPlan.forEach(accounts::put);
    return stats;
  }

  public static final class Statistics {
    public long changedAccounts;
    public long migratedAccounts;
    public long emptyAssetSkips;
    public long assetPuts;
    public long assetDeletes;
    public long prefixQueries;
    public long prefixRows;
  }
}
