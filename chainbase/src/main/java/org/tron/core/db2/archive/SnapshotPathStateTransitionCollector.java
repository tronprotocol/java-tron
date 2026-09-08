package org.tron.core.db2.archive;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.tron.core.db2.archive.P66AccountAssetCodec.AssetRow;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.db2.stateroot.PathStateBlockTransition;
import org.tron.core.db2.stateroot.PathStateCanonicalizer;
import org.tron.core.db2.stateroot.PathStateCanonicalizer.P66Phase;
import org.tron.core.db2.stateroot.PathStateMutation;
import org.tron.core.db2.stateroot.PathStateParticipantDescriptor;
import org.tron.core.db2.stateroot.PathStateTransitionCollector;

/** Canonicalizes the shared SnapshotManager block differ for the current path-state root. */
public final class SnapshotPathStateTransitionCollector
    implements PathStateTransitionCollector {

  private final PathStateCanonicalizer canonicalizer = new PathStateCanonicalizer();
  private final AccountAssetArchiveProjector accountAssetProjector;
  private final AccountAssetOldPhysicalAssetsSource oldPhysicalAssetsSource;
  private final ActivationAccountSource activationAccountSource;

  public SnapshotPathStateTransitionCollector(
      AccountAssetOldPhysicalAssetsSource oldPhysicalAssetsSource) {
    this(oldPhysicalAssetsSource, consumer -> {
      throw new IOException("path-state P66 activation Account source is unavailable");
    });
  }

  public SnapshotPathStateTransitionCollector(
      AccountAssetOldPhysicalAssetsSource oldPhysicalAssetsSource,
      ActivationAccountSource activationAccountSource) {
    this.accountAssetProjector = new AccountAssetArchiveProjector();
    this.oldPhysicalAssetsSource = Objects.requireNonNull(oldPhysicalAssetsSource,
        "oldPhysicalAssetsSource");
    this.activationAccountSource = Objects.requireNonNull(activationAccountSource,
        "activationAccountSource");
  }

  @Override
  public PathStateBlockTransition collect(BlockChangeView view) throws IOException {
    BlockChangeView admitted = Objects.requireNonNull(view, "view");
    P66Phase phase = resolvePhase(admitted);
    LinkedHashMap<MutationKey, PathStateMutation> mutations = new LinkedHashMap<>();
    if (phase == P66Phase.P66_ACTIVATION) {
      collectActivationAccounts(mutations);
    }
    for (BlockChangeView.DatabaseChanges database : admitted.getDatabases()) {
      String dbName = database.getDbName();
      PathStateParticipantDescriptor.current().require(dbName);
      if (phase == P66Phase.P66_ACTIVATION
          && (AccountAssetArchiveProjector.ACCOUNT_DB.equals(dbName)
          || AccountAssetArchiveProjector.ACCOUNT_ASSET_DB.equals(dbName))) {
        continue;
      }
      for (BlockChangeView.Change change : database.getChanges()) {
        if (AccountAssetArchiveProjector.ACCOUNT_DB.equals(dbName)) {
          collectAccount(phase, database, change, mutations);
        } else {
          addPhysical(phase, dbName, change.getKey(), database.getPrevious(change.getKey()),
              change.getPostValue(), mutations);
        }
      }
    }
    BlockSnapshotMeta meta = admitted.getMeta();
    return new PathStateBlockTransition(meta.getBlockNumber(), meta.getBlockHash(),
        meta.getParentHash(), meta.getTimestamp(), phase, mutations.values());
  }

  private void collectActivationAccounts(
      Map<MutationKey, PathStateMutation> mutations) throws IOException {
    activationAccountSource.scan((key, rawPost) -> {
      BlockChangeView.PostValue postValue = BlockChangeView.PostValue.present(rawPost);
      AccountAssetArchiveProjector.Projection projection =
          accountAssetProjector.projectWithOldPhysicalAssetsSource(
              key, null, postValue, true, oldPhysicalAssetsSource);
      addCanonical(AccountAssetArchiveProjector.ACCOUNT_DB, key, projection.oldAccount,
          projection.postAccount, P66Phase.P66_ACTIVATION, mutations);
      Map<WrappedByteArray, OldValue> reverseOldAssets = oldAssets(projection);
      for (AssetRow asset : projection.changedAssetRows) {
        OldValue oldValue = requireOldAsset(reverseOldAssets, asset.getPhysicalRawKey());
        addPhysical(P66Phase.P66_ACTIVATION,
            AccountAssetArchiveProjector.ACCOUNT_ASSET_DB,
            asset.getPhysicalRawKey(), nullable(oldValue), asset.getPostValue(), false,
            mutations);
      }
    });
  }

  private void collectAccount(P66Phase phase, BlockChangeView.DatabaseChanges database,
      BlockChangeView.Change change, Map<MutationKey, PathStateMutation> mutations) {
    byte[] key = change.getKey();
    byte[] rawOld = database.getPrevious(key);
    AccountAssetArchiveProjector.Projection projection =
        accountAssetProjector.projectWithOldPhysicalAssetsSource(
            key, rawOld, change.getPostValue(), phase != P66Phase.P66_OFF,
            oldPhysicalAssetsSource);
    addCanonical(AccountAssetArchiveProjector.ACCOUNT_DB, key, projection.oldAccount,
        projection.postAccount, phase, mutations);
    Map<WrappedByteArray, OldValue> reverseOldAssets = oldAssets(projection);
    for (AssetRow asset : projection.changedAssetRows) {
      OldValue oldValue = requireOldAsset(reverseOldAssets, asset.getPhysicalRawKey());
      addPhysical(phase, AccountAssetArchiveProjector.ACCOUNT_ASSET_DB,
          asset.getPhysicalRawKey(), nullable(oldValue), asset.getPostValue(), false, mutations);
    }
  }

  private void addPhysical(P66Phase phase, String dbName, byte[] key, byte[] rawOld,
      BlockChangeView.PostValue rawPost, Map<MutationKey, PathStateMutation> mutations) {
    addPhysical(phase, dbName, key, rawOld, rawPost, true, mutations);
  }

  private void addPhysical(P66Phase phase, String dbName, byte[] key, byte[] rawOld,
      BlockChangeView.PostValue rawPost, boolean physicalPreviousAuthoritative,
      Map<MutationKey, PathStateMutation> mutations) {
    PathStateMutation oldMutation = rawOld == null ? null
        : canonicalizer.put(phase, dbName, key, rawOld);
    PathStateMutation postMutation = rawPost.isPresent()
        ? canonicalizer.put(phase, dbName, key, rawPost.getValue())
        : canonicalizer.delete(phase, dbName, key);
    if (oldMutation != null && same(oldMutation, postMutation)) {
      return;
    }
    add(physicalPreviousAuthoritative
        ? postMutation.withPreviousPhysicalValue(
        oldMutation == null ? null : oldMutation.getPhysicalValue()) : postMutation, mutations);
  }

  private void addCanonical(String dbName, byte[] key, OldValue oldValue,
      BlockChangeView.PostValue postValue, P66Phase phase,
      Map<MutationKey, PathStateMutation> mutations) {
    PathStateMutation oldMutation = oldValue.isPresent()
        ? canonicalizer.put(phase, dbName, key, oldValue.getValue()) : null;
    PathStateMutation postMutation = postValue.isPresent()
        ? canonicalizer.put(phase, dbName, key, postValue.getValue())
        : canonicalizer.delete(phase, dbName, key);
    if (oldMutation != null && same(oldMutation, postMutation)) {
      return;
    }
    add(postMutation.withPreviousPhysicalValue(
        oldMutation == null ? null : oldMutation.getPhysicalValue()), mutations);
  }

  private static Map<WrappedByteArray, OldValue> oldAssets(
      AccountAssetArchiveProjector.Projection projection) {
    Map<WrappedByteArray, OldValue> result = new LinkedHashMap<>();
    for (org.tron.core.db2.archive.BlockReverseDiff.Entry entry : projection.reverseAssets) {
      result.put(WrappedByteArray.copyOf(entry.getKey()), entry.getOldValue());
    }
    return result;
  }

  private static OldValue requireOldAsset(Map<WrappedByteArray, OldValue> oldAssets,
      byte[] physicalKey) {
    OldValue oldValue = oldAssets.get(WrappedByteArray.copyOf(physicalKey));
    if (oldValue == null) {
      throw new ArchivePersistenceException(
          "Changed AccountAsset row has no matching pre-state value");
    }
    return oldValue;
  }

  private static byte[] nullable(OldValue oldValue) {
    return oldValue.isPresent() ? oldValue.getValue() : null;
  }

  private void add(PathStateMutation mutation,
      Map<MutationKey, PathStateMutation> mutations) {
    MutationKey key = new MutationKey(mutation.getDbName(), mutation.getCanonicalKey());
    PathStateMutation previous = mutations.putIfAbsent(key, mutation);
    if (previous != null && (!same(previous, mutation)
        || previous.isPreviousValueKnown() != mutation.isPreviousValueKnown()
        || previous.isPreviousValueKnown()
        && !Arrays.equals(previous.getPreviousPhysicalValue(),
        mutation.getPreviousPhysicalValue()))) {
      throw new ArchivePersistenceException("Conflicting path-state block mutation");
    }
  }

  private P66Phase resolvePhase(BlockChangeView view) throws IOException {
    byte[] propertyKey = HistoricalAccountAssetBalanceResolver.proposal66PhysicalKey();
    for (BlockChangeView.DatabaseChanges database : view.getDatabases()) {
      if (!HistoricalAccountAssetBalanceResolver.PROPERTIES_DATABASE.equals(
          database.getDbName())) {
        continue;
      }
      long previous = decodeP66(database.getPrevious(propertyKey), "previous");
      long target = previous;
      for (BlockChangeView.Change change : database.getChanges()) {
        if (Arrays.equals(propertyKey, change.getKey())) {
          target = decodeP66(change.getPostValue().isPresent()
              ? change.getPostValue().getValue() : null, "target");
        }
      }
      if (previous == 1L && target == 0L) {
        throw new IOException("path-state P66 phase cannot move backwards");
      }
      if (previous == 0L && target == 1L) {
        return P66Phase.P66_ACTIVATION;
      }
      return target == 0L ? P66Phase.P66_OFF : P66Phase.P66_ON;
    }
    throw new IOException("path-state properties Store is absent from block differ");
  }

  private long decodeP66(byte[] value, String label) throws IOException {
    if (value == null || value.length != Long.BYTES) {
      throw new IOException("path-state " + label + " P66 property is invalid");
    }
    long decoded = ByteBuffer.wrap(value).getLong();
    if (decoded != 0L && decoded != 1L) {
      throw new IOException("path-state " + label + " P66 property is invalid");
    }
    return decoded;
  }

  private static boolean same(PathStateMutation left, PathStateMutation right) {
    return left.isDelete() == right.isDelete()
        && left.getDbName().equals(right.getDbName())
        && Arrays.equals(left.getCanonicalKey(), right.getCanonicalKey())
        && Arrays.equals(left.getCanonicalValue(), right.getCanonicalValue());
  }

  /** Scans the canonical post-state Account domain only for the one-time P66 transition. */
  @FunctionalInterface
  public interface ActivationAccountSource {

    void scan(ActivationAccountConsumer consumer) throws IOException;
  }

  @FunctionalInterface
  public interface ActivationAccountConsumer {

    void accept(byte[] key, byte[] value) throws IOException;
  }

  private static final class MutationKey {

    private final String dbName;
    private final byte[] key;

    private MutationKey(String dbName, byte[] key) {
      this.dbName = dbName;
      this.key = Arrays.copyOf(key, key.length);
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof MutationKey)) {
        return false;
      }
      MutationKey that = (MutationKey) other;
      return dbName.equals(that.dbName) && Arrays.equals(key, that.key);
    }

    @Override
    public int hashCode() {
      return 31 * dbName.hashCode() + Arrays.hashCode(key);
    }
  }
}
