package org.tron.plugins;

import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(name = "tron", subcommands = { CommandLine.HelpCommand.class, Db.class})
public class Toolkit implements Callable<Integer> {


  public static void main(String[] args) {
    int exitCode = new CommandLine(new Toolkit()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws Exception {
    return 0;
  }
}
