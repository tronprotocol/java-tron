package org.tron.program;

import lombok.Getter;

public class Version {
  private static final String version = "3.5";
    @Getter
    private String versionName = "Odyssey-v3.5.0.1-4-g789710111";
    @Getter
    private String versionCode = "9597";

    public static String getVersion() {
      return version;
    }
}

