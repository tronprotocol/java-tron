package org.tron.plugins;

import java.io.IOException;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import picocli.CommandLine;

public class DbCopyTest extends DbTest {

  @Test
  public void testRun() {
    String[] args = new String[] { "db", "cp",  INPUT_DIRECTORY,
        genarateTmpDir()};
    Assert.assertEquals(0, cli.execute(args));
  }

  @Test
  public void testHelp() {
    String[] args = new String[] {"db", "cp", "-h"};
    CommandLine cli = new CommandLine(new Toolkit());
    Assert.assertEquals(0, cli.execute(args));
  }

  @Test
  public void testNotExist() {
    String[] args = new String[] {"db", "cp", UUID.randomUUID().toString(),
        UUID.randomUUID().toString()};
    Assert.assertEquals(404, cli.execute(args));
  }

  @Test
  public void testEmpty() throws IOException {
    String[] args = new String[] {"db", "cp", temporaryFolder.newFolder().toString(),
        genarateTmpDir()};
    Assert.assertEquals(0, cli.execute(args));
  }

  @Test
  public void testDestIsExist() throws IOException {
    String[] args = new String[] {"db", "cp", temporaryFolder.newFile().toString(),
        temporaryFolder.newFolder().toString()};
    Assert.assertEquals(402, cli.execute(args));
  }

  @Test
  public void testSrcIsFile() throws IOException {
    String[] args = new String[] {"db", "cp", temporaryFolder.newFile().toString(),
        genarateTmpDir()};
    Assert.assertEquals(403, cli.execute(args));
  }

}
