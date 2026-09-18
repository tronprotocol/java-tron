package org.tron.core.config.args;

import org.junit.Assert;
import org.junit.Test;

public class CommandLineArgumentsTest {

  @Test
  public void testNodeModePreservesExplicitOptions() {
    CommandLineArguments arguments = new CommandLineArguments(new String[] {
        "--config", "node.conf", "--p2p-disable", "false"
    });

    Assert.assertFalse(arguments.isAttachMode());
    Assert.assertEquals("node.conf", arguments.getParameters().shellConfFileName);
    Assert.assertFalse(arguments.getParameters().p2pDisable);
    Assert.assertEquals(2, arguments.getAssignedParameters().size());
    Assert.assertTrue(arguments.getAssignedParameters().stream()
        .anyMatch(pd -> "p2pDisable".equals(pd.getParameterized().getName())));
  }

  @Test
  public void testAttachIgnoresNodeOnlyOptions() {
    CommandLineArguments arguments = new CommandLineArguments(new String[] {
        "--attach", "node.sock", "--exec", "admin_example one two", "--witness",
        "--log-config", "unused.xml"
    });

    Assert.assertTrue(arguments.isAttachMode());
    Assert.assertEquals("node.sock", arguments.getParameters().ipcSocketFile);
    Assert.assertEquals("admin_example one two", arguments.getParameters().ipcExecCommand);
  }

  @Test
  public void testHelpAndVersionRetainPrecedenceOverAttachValidation() {
    Assert.assertFalse(new CommandLineArguments(new String[] {
        "--help", "--attach", ""
    }).isAttachMode());
    Assert.assertFalse(new CommandLineArguments(new String[] {
        "--version", "--exec", "help"
    }).isAttachMode());
  }
}
