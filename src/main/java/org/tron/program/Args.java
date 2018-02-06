package org.tron.program;

import com.beust.jcommander.Parameter;

public class Args {

  @Parameter(names = {"-d", "--outputDirectory"}, description = "Directory")
  private String outputDirectory = new String("");
  @Parameter(names = {"-h", "--help"}, help = true, description = "Directory")
  private boolean help = false;

  public String getOutputDirectory() {
    return outputDirectory;
  }

  public boolean isHelp() {
    return help;
  }
}
