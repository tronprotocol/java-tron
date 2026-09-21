package org.tron.core.vm.repository;

import org.junit.Assert;
import org.junit.Test;

public class RepositoryImplSelfDestructTest {

  private static final byte[] ADDRESS = new byte[] {1};

  @Test
  public void committedSelfDestructMarkerIsVisibleToParentAndSibling() {
    Repository root = RepositoryImpl.createRoot(null);
    Repository child = root.newRepositoryChild();

    child.markSelfDestruct(ADDRESS);
    Assert.assertTrue(child.isSelfDestructed(ADDRESS));
    Assert.assertFalse(root.isSelfDestructed(ADDRESS));

    child.commit();
    Assert.assertTrue(root.isSelfDestructed(ADDRESS));
    Assert.assertTrue(root.newRepositoryChild().isSelfDestructed(ADDRESS));
  }

  @Test
  public void nestedMarkerDoesNotLeakWhenOuterCallIsReverted() {
    Repository root = RepositoryImpl.createRoot(null);
    Repository outerCall = root.newRepositoryChild();
    Repository nestedCall = outerCall.newRepositoryChild();

    nestedCall.markSelfDestruct(ADDRESS);
    nestedCall.commit();

    Assert.assertTrue(outerCall.isSelfDestructed(ADDRESS));
    Assert.assertFalse(root.isSelfDestructed(ADDRESS));
  }
}
