package org.tron.core.db2;

import lombok.extern.slf4j.Slf4j;
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
import org.tron.core.db.AbstractRevokingStore;
import org.tron.core.db.RevokingDatabase;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.db2.core.ISession;
import org.tron.core.exception.RevokingStoreIllegalStateException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class RevokingDBWithCacheOldValue {
    private AbstractRevokingStore revokingDatabase;
    private TronApplicationContext context;
    private Application appT;
    @Before
    public void init() {
        Args.setParam(new String[]{"-d", "output_revokingStore_test"},
                Constant.TEST_CONF);
        context = new TronApplicationContext(DefaultConfig.class);
        appT = ApplicationFactory.create(context);
        revokingDatabase = new TestRevokingTronDatabase();
        revokingDatabase.enable();
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
    public synchronized void testPop() throws RevokingStoreIllegalStateException {
        revokingDatabase.getStack().clear();
        TestRevokingTronStore tronDatabase = new TestRevokingTronStore(
                "testrevokingtronstore-testPop", revokingDatabase);

        for (int i = 1; i < 11; i++) {
            TestProtoCapsule testProtoCapsule = new TestProtoCapsule(("pop" + i).getBytes());
            try (ISession tmpSession = revokingDatabase.buildSession()) {
                tronDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
                Assert.assertEquals(revokingDatabase.getActiveDialog(), 1);
                tmpSession.commit();
                Assert.assertEquals(revokingDatabase.getStack().size(), i);
                Assert.assertEquals(revokingDatabase.getActiveDialog(), 0);
            }
        }

        for (int i = 1; i < 11; i++) {
            revokingDatabase.pop();
            Assert.assertEquals(10 - i, revokingDatabase.getStack().size());
        }

        tronDatabase.close();

        Assert.assertEquals(revokingDatabase.getStack().size(), 0);
    }

    @Test
    public synchronized void testUndo() throws RevokingStoreIllegalStateException {
        revokingDatabase.getStack().clear();
        TestRevokingTronStore tronDatabase = new TestRevokingTronStore(
                "testrevokingtronstore-testUndo", revokingDatabase);

        SessionOptional dialog = SessionOptional.instance().setValue(revokingDatabase.buildSession());
        for (int i = 0; i < 10; i++) {
            TestProtoCapsule testProtoCapsule = new TestProtoCapsule(("undo" + i).getBytes());
            try (ISession tmpSession = revokingDatabase.buildSession()) {
                tronDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
                Assert.assertEquals(revokingDatabase.getStack().size(), 2);
                tmpSession.merge();
                Assert.assertEquals(revokingDatabase.getStack().size(), 1);
            }
        }

        Assert.assertEquals(revokingDatabase.getStack().size(), 1);

        dialog.reset();

        Assert.assertTrue(revokingDatabase.getStack().isEmpty());
        Assert.assertEquals(revokingDatabase.getActiveDialog(), 0);

        dialog = SessionOptional.instance().setValue(revokingDatabase.buildSession());
        revokingDatabase.disable();
        TestProtoCapsule testProtoCapsule = new TestProtoCapsule("del".getBytes());
        tronDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
        revokingDatabase.enable();

        try (ISession tmpSession = revokingDatabase.buildSession()) {
            tronDatabase.put(testProtoCapsule.getData(), new TestProtoCapsule("del2".getBytes()));
            tmpSession.merge();
        }

        try (ISession tmpSession = revokingDatabase.buildSession()) {
            tronDatabase.put(testProtoCapsule.getData(), new TestProtoCapsule("del22".getBytes()));
            tmpSession.merge();
        }

        try (ISession tmpSession = revokingDatabase.buildSession()) {
            tronDatabase.put(testProtoCapsule.getData(), new TestProtoCapsule("del222".getBytes()));
            tmpSession.merge();
        }

        try (ISession tmpSession = revokingDatabase.buildSession()) {
            tronDatabase.delete(testProtoCapsule.getData());
            tmpSession.merge();
        }

        dialog.reset();

        logger.info("**********testProtoCapsule:" + String.valueOf(tronDatabase.getUnchecked(testProtoCapsule.getData())));
        Assert.assertArrayEquals("del".getBytes(), tronDatabase.getUnchecked(testProtoCapsule.getData()).getData());
        Assert.assertEquals(testProtoCapsule, tronDatabase.getUnchecked(testProtoCapsule.getData()));

        tronDatabase.close();
    }

    @Test
    public void shutdown() throws RevokingStoreIllegalStateException {
        revokingDatabase.getStack().clear();
        TestRevokingTronStore tronDatabase = new TestRevokingTronStore(
                "testrevokingtronstore-shutdown", revokingDatabase);

        List<TestProtoCapsule> capsules = new ArrayList<>();
        for (int i = 1; i < 11; i++) {
            revokingDatabase.buildSession();
            TestProtoCapsule testProtoCapsule = new TestProtoCapsule(("test" + i).getBytes());
            capsules.add(testProtoCapsule);
            tronDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
            Assert.assertEquals(revokingDatabase.getActiveDialog(), i);
            Assert.assertEquals(revokingDatabase.getStack().size(), i);
        }

        for (TestProtoCapsule capsule : capsules) {
            logger.info(new String(capsule.getData()));
            Assert.assertEquals(capsule, tronDatabase.getUnchecked(capsule.getData()));
        }

        revokingDatabase.shutdown();

        for (TestProtoCapsule capsule : capsules) {
            logger.info(tronDatabase.getUnchecked(capsule.getData()).toString());
            Assert.assertEquals(null, tronDatabase.getUnchecked(capsule.getData()).getData());
        }

        Assert.assertEquals(revokingDatabase.getStack().size(), 0);
        tronDatabase.close();

    }
    private static class TestRevokingTronStore extends TronStoreWithRevoking<TestProtoCapsule> {

        protected TestRevokingTronStore(String dbName, RevokingDatabase revokingDatabase) {
            super(dbName, revokingDatabase);
        }
    }

    private static class TestRevokingTronDatabase extends AbstractRevokingStore {

    }
}
