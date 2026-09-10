package org.tron.core.db2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.junit.Test;
import org.tron.common.BaseMethodTest;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.store.AccountAssetStore;
import org.tron.protos.Protocol.Account;

public class P66SnapshotRecoveryTest extends BaseMethodTest {

  @Test
  public void legacyAccountRedoCompletesBeforePhysicalSnapshotWritesTakeOver() throws Exception {
    SnapshotManager manager = context.getBean(SnapshotManager.class);
    AccountAssetStore assets = chainBaseManager.getAccountAssetStore();
    Chainbase accounts = manager.getDbs().stream()
        .filter(db -> "account".equals(db.getDbName())).findFirst().get();
    Chainbase properties = manager.getDbs().stream()
        .filter(db -> "properties".equals(db.getDbName())).findFirst().get();
    properties.getHead().getRoot().put("ALLOW_ASSET_OPTIMIZATION"
        .getBytes(StandardCharsets.US_ASCII), Longs.toByteArray(1));
    assets.enableSnapshots(manager, true);
    byte[] address = new byte[21];
    address[0] = 0x41;
    address[20] = 99;
    byte[] asset = Bytes.concat(address, new byte[]{'1'});
    Account oldWalAccount = Account.newBuilder().setAddress(ByteString.copyFrom(address))
        .putAssetV2("1", 23).build();
    SnapshotRoot root = (SnapshotRoot) accounts.getHead().getRoot();
    root.applyCheckpointMutations(Collections.singletonMap(WrappedByteArray.of(address),
        WrappedByteArray.of(oldWalAccount.toByteArray())));
    Account persisted = Account.parseFrom(root.get(address));
    assertTrue(persisted.getAssetOptimized());
    assertEquals(0, persisted.getAssetV2Count());
    assertEquals(23, Longs.fromByteArray(assets.getFromRoot(asset)));
    // Replaying the same pre-Snapshot WAL must remain idempotent.
    root.applyCheckpointMutations(Collections.singletonMap(WrappedByteArray.of(address),
        WrappedByteArray.of(oldWalAccount.toByteArray())));
    assertEquals(23, Longs.fromByteArray(assets.getFromRoot(asset)));
    assets.finishSnapshotRecovery(manager);
    assertThrows(IllegalStateException.class,
        () -> assets.updateByBatchSynced(Collections.singletonMap(asset, Longs.toByteArray(24))));
  }
}
