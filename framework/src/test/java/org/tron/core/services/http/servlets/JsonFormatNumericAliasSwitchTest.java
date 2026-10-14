package org.tron.core.services.http.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.TestConstants;
import org.tron.core.config.args.Args;
import org.tron.core.config.args.NodeConfig;
import org.tron.protos.contract.AccountContract.AccountPermissionUpdateContract;

/**
 * Behaviour of the node.allowNumericFieldAlias switch. Field numbers used:
 * AccountPermissionUpdateContract.owner = 2 (Permission); Permission.threshold = 4 (int64).
 * The hex "2003" is Permission.threshold (field 4, varint) = 3.
 *
 * <p>With the switch ON the parser behaves exactly as before: a single-digit key is looked up by
 * field number and its value merged. With it OFF the digit key is not resolved to a field, so it
 * falls through to the existing unknown-field handling.
 */
public class JsonFormatNumericAliasSwitchTest {

  @Before
  public void setUp() {
    // Load config without overriding the switch, so ON tests exercise the real default.
    Args.setParam(new String[0], TestConstants.TEST_CONF);
  }

  @After
  public void tearDown() {
    Args.clearParam();
  }

  private static AccountPermissionUpdateContract merge(String json) throws Exception {
    AccountPermissionUpdateContract.Builder b = AccountPermissionUpdateContract.newBuilder();
    JsonFormat.merge(json, b, false);
    return b.build();
  }

  @Test
  public void testSwitchDefaultsToTrueWhenKeyAbsent() {
    // config-test.conf does not set the key, so it must bind to the reference.conf default (true).
    assertTrue(Args.getInstance().isNodeAllowNumericFieldAlias());
  }

  @Test
  public void testConfigBindingRespectsExplicitValue() {
    Config off = ConfigFactory.parseString("node.allowNumericFieldAlias = false")
        .withFallback(ConfigFactory.load(TestConstants.TEST_CONF));
    assertFalse(NodeConfig.fromConfig(off).isAllowNumericFieldAlias());

    Config on = ConfigFactory.parseString("node.allowNumericFieldAlias = true")
        .withFallback(ConfigFactory.load(TestConstants.TEST_CONF));
    assertTrue(NodeConfig.fromConfig(on).isAllowNumericFieldAlias());

    Config absent = ConfigFactory.load(TestConstants.TEST_CONF);
    assertTrue(NodeConfig.fromConfig(absent).isAllowNumericFieldAlias());
  }

  @Test
  public void testOnHexAliasMaterializes() throws Exception {
    assertEquals(3, merge("{\"2\":\"2003\"}").getOwner().getThreshold());
  }

  @Test
  public void testOnObjectAliasRejected() {
    // The legacy numeric-alias path only accepts a hex string value, never an object.
    JsonFormat.ParseException e = assertThrows(JsonFormat.ParseException.class,
        () -> merge("{\"2\":{\"threshold\":3}}"));
    assertTrue(e.getMessage(), e.getMessage().contains("Expected string."));
  }

  @Test
  public void testOffHexAliasSkipped() throws Exception {
    Args.getInstance().setNodeAllowNumericFieldAlias(false);
    assertFalse(merge("{\"2\":\"2003\"}").hasOwner());
  }

  @Test
  public void testOffObjectAliasSkipped() throws Exception {
    Args.getInstance().setNodeAllowNumericFieldAlias(false);
    assertFalse(merge("{\"2\":{\"threshold\":3}}").hasOwner());
  }

  @Test
  public void testOffArrayAliasSkipped() throws Exception {
    Args.getInstance().setNodeAllowNumericFieldAlias(false);
    assertEquals(0, merge("{\"4\":[\"2003\",\"2003\"]}").getActivesCount());
  }

  @Test
  public void testOffNamedFieldParsedWhileInnerNumericAliasIgnored() throws Exception {
    Args.getInstance().setNodeAllowNumericFieldAlias(false);
    // The named "owner" key is unaffected by the switch and still builds the sub-message; the
    // named inner "threshold" is parsed, while the numeric inner alias "4" is ignored.
    assertEquals(3, merge("{\"owner\":{\"threshold\":3}}").getOwner().getThreshold());
    AccountPermissionUpdateContract viaAlias = merge("{\"owner\":{\"4\":3}}");
    assertTrue(viaAlias.hasOwner());
    assertEquals(0, viaAlias.getOwner().getThreshold());
  }

  @Test
  public void testConfigFileDisablesAliasEndToEnd() throws Exception {
    // Exercise the full config-file -> Args -> JsonFormat path, so a future omission in
    // Args.applyNodeConfig that drops the switch assignment is caught here, not only the
    // NodeConfig binding checked above.
    File conf = File.createTempFile("numeric-alias-off", ".conf");
    try {
      Files.write(conf.toPath(), ("include classpath(\"" + TestConstants.TEST_CONF
          + "\")\nnode.allowNumericFieldAlias = false\n").getBytes(StandardCharsets.UTF_8));
      Args.clearParam();
      Args.setParam(new String[] {"-c", conf.getAbsolutePath()}, TestConstants.TEST_CONF);

      assertFalse(Args.getInstance().isNodeAllowNumericFieldAlias());
      assertFalse(merge("{\"2\":\"2003\"}").hasOwner());
      assertEquals(3, merge("{\"owner\":{\"threshold\":3}}").getOwner().getThreshold());
    } finally {
      conf.delete();
    }
  }
}
