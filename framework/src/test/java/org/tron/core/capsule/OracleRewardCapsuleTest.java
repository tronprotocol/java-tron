package org.tron.core.capsule;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class OracleRewardCapsuleTest {


  @Test
  public void testConstructors() {
    long balance = 1000;
    Map<String, Long> asset = new HashMap<>();
    asset.put("stable-01", 100L);
    asset.put("stable-02", 100L);
    OracleRewardCapsule reward = new OracleRewardCapsule(balance, asset);
    OracleRewardCapsule reward1 = new OracleRewardCapsule(reward.getInstance());
    Assert.assertEquals(reward, reward1);
    OracleRewardCapsule reward2 = new OracleRewardCapsule(reward.getData());
    Assert.assertEquals(reward, reward2);
    Assert.assertTrue(new OracleRewardCapsule().isZero());
    Assert.assertFalse(reward.isZero());
  }

  @Test
  public void testAdd() {
    long balance = 1000;
    Map<String, Long> asset = new HashMap<>();
    asset.put("stable-01", 100L);
    asset.put("stable-02", 100L);
    OracleRewardCapsule reward = new OracleRewardCapsule(balance, asset);
    OracleRewardCapsule reward1 = new OracleRewardCapsule(reward.getInstance());
    balance = balance * 2;
    asset = new HashMap<>();
    asset.put("stable-01", 100L * 2);
    asset.put("stable-02", 100L * 2);
    OracleRewardCapsule reward3 = new OracleRewardCapsule(balance, asset);
    Assert.assertEquals(reward3, reward.add(reward1));

  }

  @Test
  public void testSub() {
    long balance = 1000;
    Map<String, Long> asset = new HashMap<>();
    asset.put("stable-01", 100L);
    asset.put("stable-02", 100L);
    OracleRewardCapsule reward = new OracleRewardCapsule(balance, asset);
    OracleRewardCapsule reward1 = new OracleRewardCapsule(reward.getInstance());

    Assert.assertEquals(new OracleRewardCapsule(), reward.sub(reward1));
  }

  @Test
  public void testValid() {
    long balance = 1000;
    Map<String, Long> asset = new HashMap<>();
    asset.put("stable-01", 100L);
    asset.put("stable-02", 100L);
    OracleRewardCapsule reward = new OracleRewardCapsule(balance, asset);
    OracleRewardCapsule reward1 = new OracleRewardCapsule(reward.getInstance());

    try {
      reward.sub(reward1).sub(reward1);
    } catch (IllegalArgumentException e) {
      Assert.assertEquals(e.getMessage(),"balance:-1000");
    }
  }
}
