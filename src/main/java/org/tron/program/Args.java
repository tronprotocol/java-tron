package org.tron.program;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Args {

  private static final Args INSTANCE = new Args();


  @Parameter(names = {"-d", "--output-directory"}, description = "Directory")
  private String outputDirectory = new String("");

  @Parameter(names = {"-h", "--help"}, help = true, description = "Directory")
  private boolean help = false;

  @Parameter(description = "-seed-nodes")
  private List<String> seedNodes = new ArrayList<>();

  @Parameter(names = {"-p", "--private-key"}, description = "private-key")
  private String privateKey = new String("");

  private Args() {

  }

  public static void setParam(String[] args) {
    JCommander.newBuilder().addObject(INSTANCE).build().parse(args);
  }

  public static Args getInstance() {
    return INSTANCE;
  }

  public String getOutputDirectory() {
    if (!outputDirectory.equals("") && !outputDirectory.endsWith(File.separator)) {
      return outputDirectory + File.separator;
    }
    return outputDirectory;
  }

  public boolean isHelp() {
    return help;
  }

  public List<String> getSeedNodes() {
    return seedNodes;
  }

  public String getPrivateKey() {
    return privateKey;
  }
}
