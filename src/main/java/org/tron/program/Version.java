package org.tron.program;

import lombok.Getter;

public class Version {
  private static final String version = "3.5";
    @Getter
    private String versionName = "";
    @Getter
    private String versionCode = "";

    public static String getVersion() {
      return version;
    }
}

