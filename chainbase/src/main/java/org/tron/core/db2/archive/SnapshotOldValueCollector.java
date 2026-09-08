package org.tron.core.db2.archive;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Scheme 2 collector: read old values from the completed block layer's previous view. */
public final class SnapshotOldValueCollector implements OldValueCollector {

  private final AccountAssetArchiveProjector accountAssetProjector;
  private final AccountAssetOldPhysicalAssetsSource oldPhysicalAssetsSource;
  private final TargetAssetOptimizationResolver targetAssetOptimizationResolver;

  public SnapshotOldValueCollector() {
    this(null, null, (TargetAssetOptimizationResolver) null);
  }

  public SnapshotOldValueCollector(AccountAssetArchiveProjector accountAssetProjector,
      AccountAssetOldPhysicalAssetsSource oldPhysicalAssetsSource,
      BooleanSupplier optimizationEnabled) {
    this(accountAssetProjector, oldPhysicalAssetsSource, constant(optimizationEnabled));
  }

  public SnapshotOldValueCollector(AccountAssetArchiveProjector accountAssetProjector,
      AccountAssetOldPhysicalAssetsSource oldPhysicalAssetsSource,
      TargetAssetOptimizationResolver targetAssetOptimizationResolver) {
    this.accountAssetProjector = accountAssetProjector;
    this.oldPhysicalAssetsSource = oldPhysicalAssetsSource;
    this.targetAssetOptimizationResolver = targetAssetOptimizationResolver;
    if (accountAssetProjector != null) {
      Objects.requireNonNull(oldPhysicalAssetsSource, "oldPhysicalAssetsSource");
      Objects.requireNonNull(targetAssetOptimizationResolver,
          "targetAssetOptimizationResolver");
    }
  }

  @Override
  public BlockReverseDiff collect(BlockChangeView view) {
    List<BlockReverseDiff.DbGroup> groups = new ArrayList<>();
    List<BlockReverseDiff.Entry> accountAssetEntries = new ArrayList<>();
    boolean targetAssetOptimizationEnabled = accountAssetProjector != null
        && targetAssetOptimizationResolver.isEnabled(view);
    for (BlockChangeView.DatabaseChanges database : view.getDatabases()) {
      List<BlockReverseDiff.Entry> entries = new ArrayList<>();
      for (BlockChangeView.Change change : database.getChanges()) {
        byte[] key = change.getKey();
        OldValue oldValue = OldValue.fromNullable(database.getPrevious(key));
        BlockChangeView.PostValue postValue = change.getPostValue();
        if (accountAssetProjector != null
            && AccountAssetArchiveProjector.ACCOUNT_DB.equals(database.getDbName())) {
          AccountAssetArchiveProjector.Projection projection =
              accountAssetProjector.projectWithOldPhysicalAssetsSource(
                  key, oldValue.isPresent() ? oldValue.getValue() : null, postValue,
                  targetAssetOptimizationEnabled, oldPhysicalAssetsSource);
          oldValue = projection.oldAccount;
          postValue = projection.postAccount;
          accountAssetEntries.addAll(projection.reverseAssets);
        }
        if (!sameLogicalValue(oldValue, postValue)) {
          entries.add(new BlockReverseDiff.Entry(key, oldValue));
        }
      }
      if (!entries.isEmpty()) {
        groups.add(new BlockReverseDiff.DbGroup(database.getDbName(), entries));
      }
    }
    if (!accountAssetEntries.isEmpty()) {
      groups.add(new BlockReverseDiff.DbGroup(
          AccountAssetArchiveProjector.ACCOUNT_ASSET_DB, accountAssetEntries));
    }
    return new BlockReverseDiff(view.getMeta(), groups);
  }

  /** Resolves proposal 66 from the same immutable target block view being projected. */
  public static boolean resolveTargetAssetOptimization(BlockChangeView view) {
    Objects.requireNonNull(view, "view");
    byte[] propertyKey = HistoricalAccountAssetBalanceResolver.proposal66PhysicalKey();
    for (BlockChangeView.DatabaseChanges database : view.getDatabases()) {
      if (!HistoricalAccountAssetBalanceResolver.PROPERTIES_DATABASE.equals(
          database.getDbName())) {
        continue;
      }
      byte[] targetValue = database.getPrevious(propertyKey);
      for (BlockChangeView.Change change : database.getChanges()) {
        if (Arrays.equals(propertyKey, change.getKey())) {
          targetValue = change.getPostValue().isPresent()
              ? change.getPostValue().getValue() : null;
        }
      }
      if (targetValue == null || targetValue.length != Long.BYTES) {
        throw new ArchivePersistenceException(
            "Target proposal-66 property must be exactly eight bytes");
      }
      long enabled = ByteBuffer.wrap(targetValue).getLong();
      if (enabled != 0L && enabled != 1L) {
        throw new ArchivePersistenceException("Target proposal-66 property must be 0 or 1");
      }
      return enabled == 1L;
    }
    throw new ArchivePersistenceException("Target proposal-66 properties Store is absent");
  }

  private boolean sameLogicalValue(OldValue oldValue, BlockChangeView.PostValue postValue) {
    if (oldValue.isPresent() != postValue.isPresent()) {
      return false;
    }
    return !oldValue.isPresent() || Arrays.equals(oldValue.getValue(), postValue.getValue());
  }

  @FunctionalInterface
  public interface TargetAssetOptimizationResolver {
    boolean isEnabled(BlockChangeView view);
  }

  private static TargetAssetOptimizationResolver constant(BooleanSupplier supplier) {
    BooleanSupplier checked = Objects.requireNonNull(supplier, "optimizationEnabled");
    return view -> checked.getAsBoolean();
  }
}
