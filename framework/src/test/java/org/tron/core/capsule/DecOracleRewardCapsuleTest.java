package org.tron.core.capsule;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.entity.Dec;
import org.tron.common.utils.Pair;

@Slf4j(topic = "capsule")
public class DecOracleRewardCapsuleTest {


  @Test
  public void testConstructors() {
    Dec balance = Dec.newDec(1000);
    Map<String, Dec> asset = new HashMap<>();
    asset.put("stable-01", Dec.newDec(100));
    asset.put("stable-02", Dec.newDec(100));
    DecOracleRewardCapsule reward = new DecOracleRewardCapsule(balance, asset);
    DecOracleRewardCapsule reward1 = new DecOracleRewardCapsule(reward.getInstance());
    Assert.assertEquals(reward, reward1);
    DecOracleRewardCapsule reward2 = new DecOracleRewardCapsule(
        BigInteger.valueOf(balance.roundLong()),
        asset.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
            e -> BigInteger.valueOf(e.getValue().roundLong()))));
    Assert.assertEquals(reward, reward2);
    DecOracleRewardCapsule reward3 = new DecOracleRewardCapsule(reward2.getData());
    Assert.assertEquals(reward, reward3);
    DecOracleRewardCapsule reward4 = new DecOracleRewardCapsule(new OracleRewardCapsule(
        balance.roundLong(), asset.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().roundLong()))));
    Assert.assertEquals(reward, reward4);
    asset = new HashMap<>();
    asset.put("stable-01", Dec.newDec(100));
    asset.put("stable-02", Dec.newDec(100));
    asset.put("stable-03", Dec.newDec(0));
    DecOracleRewardCapsule reward5 = new DecOracleRewardCapsule(balance, asset);
    Assert.assertEquals(reward, reward5);
  }

  @Test
  public void testData() {

    DecOracleRewardCapsule reward3 = new DecOracleRewardCapsule();

    Assert.assertNotNull(reward3.getData());

  }

  @Test
  public void testAdd() {
    Dec balance = Dec.newDec(1000);
    Map<String, Dec> asset = new HashMap<>();
    asset.put("stable-01", Dec.newDec(100));
    asset.put("stable-02", Dec.newDec(100));
    DecOracleRewardCapsule reward = new DecOracleRewardCapsule(balance, asset);
    DecOracleRewardCapsule reward1 = new DecOracleRewardCapsule(reward.getInstance());
    balance = Dec.newDec(1000).mul(2);
    asset = new HashMap<>();
    asset.put("stable-01", Dec.newDec(100).mul(2));
    asset.put("stable-02", Dec.newDec(100).mul(2));

    DecOracleRewardCapsule reward3 = new DecOracleRewardCapsule(balance, asset);

    Assert.assertEquals(reward3, reward.add(reward1));

  }

  @Test
  public void testSub() {
    Dec balance = Dec.newDec(1000);
    Map<String, Dec> asset = new HashMap<>();
    asset.put("stable-01", Dec.newDec(100));
    asset.put("stable-02", Dec.newDec(100));
    DecOracleRewardCapsule reward = new DecOracleRewardCapsule(balance, asset);
    DecOracleRewardCapsule reward1 = new DecOracleRewardCapsule(reward.getData());
    DecOracleRewardCapsule reward2 = new DecOracleRewardCapsule();
    Assert.assertEquals(reward2, reward.sub(reward1));
    Assert.assertTrue(reward.sub(reward1).isZero());
  }

  @Test
  public void testMul() {
    Dec balance = Dec.newDec(1000);
    Map<String, Dec> asset = new HashMap<>();
    asset.put("stable-01", Dec.newDec(100));
    asset.put("stable-02", Dec.newDec(100));
    DecOracleRewardCapsule reward = new DecOracleRewardCapsule(balance, asset);
    balance = Dec.newDec(1000).mul(2);
    asset = new HashMap<>();
    asset.put("stable-01", Dec.newDec(100).mul(2));
    asset.put("stable-02", Dec.newDec(100).mul(2));
    DecOracleRewardCapsule reward2 = new DecOracleRewardCapsule(balance, asset);
    Assert.assertFalse(reward2.isZero());
    Assert.assertEquals(reward2, reward.mul(2));
    Assert.assertEquals(reward2, reward.mulTruncate(2));
    Assert.assertEquals(reward2, reward.mul(Dec.newDec(2)));
    Assert.assertEquals(reward2, reward.mulTruncate(Dec.newDec(2)));
  }

  @Test
  public void testQuo() {
    Dec balance = Dec.newDec(1000);
    Map<String, Dec> asset = new HashMap<>();
    asset.put("stable-01", Dec.newDec(100));
    asset.put("stable-02", Dec.newDec(100));
    DecOracleRewardCapsule reward = new DecOracleRewardCapsule(balance, asset);
    balance = Dec.newDec(1000).quo(2);
    asset = new HashMap<>();
    asset.put("stable-01", Dec.newDec(100).quo(2));
    asset.put("stable-02", Dec.newDec(100).quo(2));
    DecOracleRewardCapsule reward2 = new DecOracleRewardCapsule(balance, asset);
    Assert.assertEquals(reward2, reward.quo(2));
    Assert.assertEquals(reward2, reward.quoTruncate(2));
    Assert.assertEquals(reward2, reward.quo(Dec.newDec(2)));
    Assert.assertEquals(reward2, reward.quoTruncate(Dec.newDec(2)));
  }

  @Test
  public void testTruncate() {
    Dec balance = Dec.newDec(1000);
    Map<String, Dec> asset = new HashMap<>();
    asset.put("stable-01", Dec.newDec(100));
    asset.put("stable-02", Dec.newDec(100));
    DecOracleRewardCapsule reward = new DecOracleRewardCapsule(balance, asset);
    OracleRewardCapsule reward2 = reward.truncateDecimal();
    Assert.assertEquals(reward, new DecOracleRewardCapsule(reward2));
    Pair<OracleRewardCapsule, DecOracleRewardCapsule> pair = reward.truncateDecimalAndRemainder();
    Assert.assertEquals(reward, new DecOracleRewardCapsule(pair.getKey()));
    Assert.assertTrue(pair.getValue().isZero());
  }

  @Test
  public void testIntersect() {
    Dec balance = Dec.newDec(1000);
    Map<String, Dec> asset = new HashMap<>();
    asset.put("stable-01", Dec.newDec(100));
    asset.put("stable-02", Dec.newDec(100));
    asset.put("stable-04", Dec.newDec(1));
    DecOracleRewardCapsule reward = new DecOracleRewardCapsule(balance, asset);

    balance = Dec.newDec(500);
    asset = new HashMap<>();
    asset.put("stable-01", Dec.newDec(200));
    asset.put("stable-02", Dec.newDec(10));
    asset.put("stable-03", Dec.newDec(10));
    DecOracleRewardCapsule reward2 = new DecOracleRewardCapsule(balance, asset);

    balance = Dec.newDec(500);
    asset = new HashMap<>();
    asset.put("stable-01", Dec.newDec(100));
    asset.put("stable-02", Dec.newDec(10));
    DecOracleRewardCapsule reward3 = new DecOracleRewardCapsule(balance, asset);
    Assert.assertEquals(reward3, reward.intersect(reward2));
    Assert.assertEquals(reward3, reward2.intersect(reward));
  }

  @Test
  public void testValid() {
    Dec balance = Dec.newDec(1000);
    Map<String, Dec> asset = new HashMap<>();
    asset.put("stable-01", Dec.newDec(100));
    asset.put("stable-02", Dec.newDec(100));
    DecOracleRewardCapsule reward = new DecOracleRewardCapsule(balance, asset);
    DecOracleRewardCapsule reward1 = new DecOracleRewardCapsule(reward.getData());
    try {
      reward.sub(reward1).sub(reward1);
    } catch (IllegalArgumentException e) {
      Assert.assertEquals(e.getMessage(),"balance:-1000");
    }
  }

}