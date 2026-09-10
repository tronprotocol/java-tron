package org.tron.core.db2.archive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.tron.core.db2.stateroot.PathStateBlockTransition;
import org.tron.core.db2.stateroot.PathStateCanonicalizer.P66Phase;
import org.tron.core.db2.stateroot.PathStateMutation;
import org.tron.core.db2.stateroot.PathStateTransitionCollector;

/** Consumes exact physical mutations after P66 materialization; no projection or scans. */
public final class PhysicalSnapshotPathStateCollector implements PathStateTransitionCollector {
  @Override
  public PathStateBlockTransition collect(BlockChangeView view) {
    boolean enabled = SnapshotOldValueCollector.resolveTargetAssetOptimization(view);
    List<PathStateMutation> mutations = new ArrayList<>();
    for (BlockChangeView.DatabaseChanges database : view.getDatabases()) {
      for (BlockChangeView.Change change : database.getChanges()) {
        byte[] key = change.getKey();
        byte[] oldValue = database.getPrevious(key);
        byte[] value = change.getPostValue().isPresent()
            ? change.getPostValue().getValue() : null;
        if (!Arrays.equals(oldValue, value)) {
          mutations.add((value == null ? PathStateMutation.delete(database.getDbName(), key)
              : PathStateMutation.put(database.getDbName(), key, value))
              .withPreviousPhysicalValue(oldValue));
        }
      }
    }
    BlockSnapshotMeta meta = view.getMeta();
    return new PathStateBlockTransition(meta.getBlockNumber(), meta.getBlockHash(),
        meta.getParentHash(), meta.getTimestamp(), enabled ? P66Phase.P66_ON : P66Phase.P66_OFF,
        mutations);
  }
}
