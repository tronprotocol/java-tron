package org.tron.program;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import java.util.ArrayList;
import java.util.List;

public class Args {

  private final static Args INSTANCE = new Args();


  @Parameter(names = {"-d", "--outputDirectory"}, description = "Directory")
  private String outputDirectory = new String("");

  @Parameter(names = {"-h", "--help"}, help = true, description = "Directory")
  private boolean help = false;

  @Parameter(description = "seedNodes")
  private List<String> seedNodes = new ArrayList<>();

  private Args() {
  }

  public static void setParam(String[] args) {
    JCommander.newBuilder()
        .addObject(INSTANCE)
        .build()
        .parse(args);
  }

  public static Args getInstance() {
    return INSTANCE;
  }

  public String getOutputDirectory() {
    if (outputDirectory != "" && !outputDirectory.endsWith("/")) {
      return outputDirectory + "/";
    }
    return outputDirectory;
  }

  public boolean isHelp() {
    return help;
  }

  public List<String> getSeedNodes() {
    return seedNodes;
  }
}
