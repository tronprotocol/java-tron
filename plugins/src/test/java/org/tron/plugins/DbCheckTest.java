package org.tron.plugins;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import picocli.CommandLine;

@Slf4j
public class DbCheckTest {

  @Test
  public void test() {
    String[] args = new String[] {"db", "check"};
    CommandLine cli = new CommandLine(new Toolkit());
    Assert.assertEquals(0, cli.execute(args));
  }
}
