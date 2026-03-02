package org.tron.core.db.api.pojo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import org.junit.Test;

public class PojoTest {

  @Test
  public void testAccountCreation() {
    Account account = Account.of();
    account.setName("testName");
    account.setAddress("testAddress");
    account.setBalance(1622548800000L);

    assertNotNull(account);
    assertEquals("testName", account.getName());
    assertEquals("testAddress", account.getAddress());
    assertEquals(1622548800000L, account.getBalance());
  }

  @Test
  public void testAssetIssueCreation() {
    AssetIssue assetIssue = AssetIssue.of();
    assetIssue.setName("testName");
    assetIssue.setAddress("testAddress");
    assetIssue.setStart(1622548800000L);
    assetIssue.setEnd(1654084800000L);

    assertNotNull(assetIssue);
    assertEquals("testName", assetIssue.getName());
    assertEquals("testAddress", assetIssue.getAddress());
    assertEquals(1622548800000L, assetIssue.getStart());
    assertEquals(1654084800000L, assetIssue.getEnd());
  }

  @Test
  public void testBlockCreation() {
    Block block = Block.of();
    block.setId("7654321");
    block.setNumber(1000L);
    block.setTransactionIds(Lists.newArrayList("1234567"));

    assertNotNull(block);
    assertEquals("7654321", block.getId());
    assertEquals(1000L, block.getNumber());
    assertEquals("1234567", block.getTransactionIds().get(0));
  }

  @Test
  public void testTransactionCreation() {
    Transaction transaction = Transaction.of();
    transaction.setId("7654321");
    transaction.setFrom("from");
    transaction.setTo("to");

    assertNotNull(transaction);
    assertEquals("7654321", transaction.getId());
    assertEquals("from", transaction.getFrom());
    assertEquals("to", transaction.getTo());
  }

  @Test
  public void testWitnessCreation() {
    Witness witness = Witness.of();
    witness.setAddress("testAddress");
    witness.setJobs(true);
    witness.setUrl("https://cryptoai.com");
    witness.setPublicKey("wergfsioejgbrotjsdfdoqwj");

    assertNotNull(witness);
    assertEquals("testAddress", witness.getAddress());
    assertTrue(witness.isJobs());
    assertEquals("https://cryptoai.com", witness.getUrl());
    assertEquals("wergfsioejgbrotjsdfdoqwj", witness.getPublicKey());
  }
}
