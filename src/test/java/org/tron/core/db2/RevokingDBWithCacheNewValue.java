package org.tron.core.db2;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.common.utils.SessionOptional;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.db2.core.ISession;
import org.tron.core.db2.core.SnapshotManager;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.exception.RevokingStoreIllegalStateException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class RevokingDBWithCacheNewValue {
    private SnapshotManager revokingDatabase;
    private  TronApplicationContext context;
    private Application appT;
    private TestRevokingTronStore tronDatabase;
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
        tronDatabase.close();
        FileUtil.deleteDir(new File("output_revokingStore_test"));
    }

    @Test
    public synchronized void testPop() throws RevokingStoreIllegalStateException {
        revokingDatabase = new TestSnapshotManager();
        revokingDatabase.enable();
        tronDatabase = new TestRevokingTronStore("testRevokingDBWithCacheNewValue-testPop");
        revokingDatabase.add(tronDatabase.getRevokingDB());

        while (revokingDatabase.size() != 0) {
            revokingDatabase.pop();
        }

        for (int i = 1; i < 11; i++) {
            TestProtoCapsule testProtoCapsule = new TestProtoCapsule(("pop" + i).getBytes());
            try (ISession tmpSession = revokingDatabase.buildSession()) {
                tronDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
                Assert.assertEquals(revokingDatabase.getActiveSession(), 1);
                tmpSession.commit();
                Assert.assertEquals(revokingDatabase.getSize(), i);
                Assert.assertEquals(revokingDatabase.getActiveSession(), 0);
            }
        }

        for (int i = 1; i < 11; i++) {
            revokingDatabase.pop();
            Assert.assertEquals(10 - i, revokingDatabase.getSize());
        }

        Assert.assertEquals(revokingDatabase.getSize(), 0);
    }
    @Test
    public synchronized void testMerge() throws  BadItemException, ItemNotFoundException{
        revokingDatabase = new TestSnapshotManager();
        revokingDatabase.enable();
        tronDatabase = new TestRevokingTronStore("testRevokingDBWithCacheNewValue-testMerge");
        revokingDatabase.add(tronDatabase.getRevokingDB());

        while (revokingDatabase.size() != 0) {
            revokingDatabase.pop();
        }
        SessionOptional dialog = SessionOptional.instance().setValue(revokingDatabase.buildSession());
        dialog.setValue(revokingDatabase.buildSession());
        TestProtoCapsule testProtoCapsule = new TestProtoCapsule("merge".getBytes());
        TestProtoCapsule testProtoCapsule2 = new TestProtoCapsule("merge2".getBytes());

        tronDatabase.put("merge".getBytes(), testProtoCapsule);

        try (ISession tmpSession = revokingDatabase.buildSession()) {
            tronDatabase.put(testProtoCapsule.getData(), testProtoCapsule2);
            tmpSession.merge();
        }
        Assert.assertEquals(testProtoCapsule2, tronDatabase.get("merge".getBytes()));

        try (ISession tmpSession = revokingDatabase.buildSession()) {
            tronDatabase.delete("merge".getBytes());
            tmpSession.merge();
        }
        Assert.assertEquals(null, tronDatabase.get("merge".getBytes()));
        dialog.reset();
    }


    @Test
    public synchronized void testRevoke() throws  BadItemException, ItemNotFoundException {
        revokingDatabase = new TestSnapshotManager();
        revokingDatabase.enable();
        tronDatabase = new TestRevokingTronStore("testRevokingDBWithCacheNewValue-testRevoke");
        revokingDatabase.add(tronDatabase.getRevokingDB());

        while (revokingDatabase.size() != 0) {
            revokingDatabase.pop();
        }
        SessionOptional dialog = SessionOptional.instance().setValue(revokingDatabase.buildSession());
        for (int i = 0; i < 10; i++) {
            TestProtoCapsule testProtoCapsule = new TestProtoCapsule(("undo" + i).getBytes());
            try (ISession tmpSession = revokingDatabase.buildSession()) {
                tronDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
                Assert.assertEquals(revokingDatabase.getSize(), 2);
                tmpSession.merge();
                Assert.assertEquals(revokingDatabase.getSize(), 1);
            }
        }

        Assert.assertEquals(revokingDatabase.getSize(), 1);
        dialog.reset();
        Assert.assertTrue(revokingDatabase.getSize() == 0);
        Assert.assertEquals(revokingDatabase.getActiveSession(), 0);

        dialog.setValue(revokingDatabase.buildSession());
        TestProtoCapsule testProtoCapsule = new TestProtoCapsule("revoke".getBytes());
        TestProtoCapsule testProtoCapsule2 = new TestProtoCapsule("revoke2".getBytes());
        tronDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
        dialog.setValue(revokingDatabase.buildSession());


        try (ISession tmpSession = revokingDatabase.buildSession()) {
            tronDatabase.put(testProtoCapsule.getData(), testProtoCapsule2);
            tmpSession.merge();
        }

        try (ISession tmpSession = revokingDatabase.buildSession()) {
            tronDatabase.put(testProtoCapsule.getData(), new TestProtoCapsule("revoke22".getBytes()));
            tmpSession.merge();
        }

        try (ISession tmpSession = revokingDatabase.buildSession()) {
            tronDatabase.put(testProtoCapsule.getData(), new TestProtoCapsule("revoke222".getBytes()));
            tmpSession.merge();
        }

        try (ISession tmpSession = revokingDatabase.buildSession()) {
            tronDatabase.delete(testProtoCapsule.getData());
            tmpSession.merge();
        }

        dialog.reset();

        logger.info("**********testProtoCapsule:" + String.valueOf(tronDatabase.getUnchecked(testProtoCapsule.getData())));
        Assert.assertEquals(testProtoCapsule, tronDatabase.get(testProtoCapsule.getData()));
    }
    
    public static class  TestRevokingTronStore extends TronStoreWithRevoking<TestProtoCapsule> {
        protected TestRevokingTronStore(String dbName) {
            super(dbName);
        }

        @Override
        public TestProtoCapsule get(byte[] key) {
            byte[] value = this.revokingDB.getUnchecked(key);
            return ArrayUtils.isEmpty(value) ? null : new TestProtoCapsule(value);
        }
    }

    public static class TestSnapshotManager extends SnapshotManager {

    }
}



