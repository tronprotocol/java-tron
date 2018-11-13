package org.tron.core.db2;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.SessionOptional;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db2.RevokingDBWithCacheNewValue.TestRevokingTronStore;
import org.tron.core.db2.RevokingDBWithCacheNewValue.TestSnapshotManager;
import org.tron.core.db2.core.ISession;
import org.tron.core.db2.core.Snapshot;
import org.tron.core.db2.core.SnapshotManager;
import org.tron.core.db2.core.SnapshotRoot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SnapshotRootTest {
    private TestRevokingTronStore tronDatabase;
    private TronApplicationContext context;
    private Application appT;
    private SnapshotManager revokingDatabase;

    @Before
    public void init() {
        Args.setParam(new String[]{"-d", "output_revokingStore_test"},
                Constant.TEST_CONF);
        context = new TronApplicationContext(DefaultConfig.class);
        appT = ApplicationFactory.create(context);
    }

    @After
    public void removeDb() {
        Args.clearParam();
        appT.shutdownServices();
        appT.shutdown();
        context.destroy();
        FileUtil.deleteDir(new File("output_revokingStore_test"));
    }

    @Test
    public synchronized void testRemove(){
        TestProtoCapsule testProtoCapsule = new TestProtoCapsule("test".getBytes());
        tronDatabase = new TestRevokingTronStore("testSnapshotRoot-testRemove");
        tronDatabase.put("test".getBytes(), testProtoCapsule);
        Assert.assertEquals(testProtoCapsule, tronDatabase.get("test".getBytes()));

        tronDatabase.delete("test".getBytes());
        Assert.assertEquals(null, tronDatabase.get("test".getBytes()));
        tronDatabase.close();
    }

    @Test
    public synchronized void testMerge(){
        TestProtoCapsule testProtoCapsule = new TestProtoCapsule("test".getBytes());
        tronDatabase = new TestRevokingTronStore("testSnapshotRoot-testMerge");
        revokingDatabase = new TestSnapshotManager();
        revokingDatabase.enable();
        revokingDatabase.add(tronDatabase.getRevokingDB());

        SessionOptional dialog = SessionOptional.instance().setValue(revokingDatabase.buildSession());
        tronDatabase.put("merge".getBytes(), testProtoCapsule);
        revokingDatabase.getDbs().forEach(db -> db.getHead().getRoot().merge(db.getHead()));
        dialog.reset();
        Assert.assertEquals(tronDatabase.get("merge".getBytes()), testProtoCapsule);

        tronDatabase.close();
    }

    @Test
    public synchronized void testMergeList(){
        TestProtoCapsule testProtoCapsule = new TestProtoCapsule("test".getBytes());
        tronDatabase = new TestRevokingTronStore("testSnapshotRoot-testMergeList");
        revokingDatabase = new TestSnapshotManager();
        revokingDatabase.enable();
        revokingDatabase.add(tronDatabase.getRevokingDB());

        SessionOptional.instance().setValue(revokingDatabase.buildSession());
        tronDatabase.put("merge".getBytes(), testProtoCapsule);
        for (int i = 1; i < 11; i++) {
            TestProtoCapsule tmpProtoCapsule = new TestProtoCapsule(("mergeList" + i).getBytes());
            try (ISession tmpSession = revokingDatabase.buildSession()) {
                tronDatabase.put(tmpProtoCapsule.getData(), tmpProtoCapsule);
                tmpSession.commit();
            }
        }
        revokingDatabase.getDbs().forEach(db -> {
            List<Snapshot> snapshots = new ArrayList<>();
            SnapshotRoot root = (SnapshotRoot) db.getHead().getRoot();
            Snapshot next = root;
            for (int i = 0; i < 11; ++i) {
                next = next.getNext();
                snapshots.add(next);
            }
            root.merge(snapshots);
            root.resetSolidity();

            for (int i = 1; i < 11; i++) {
                TestProtoCapsule tmpProtoCapsule = new TestProtoCapsule(("mergeList" + i).getBytes());
                Assert.assertEquals(tmpProtoCapsule, tronDatabase.get(tmpProtoCapsule.getData()));
            }

        });
        revokingDatabase.updateSolidity(10);
        tronDatabase.close();
    }

}
