package org.tron.common.arch;

public final class Arch {

  private Arch() {
  }

  public static String withAll() {
    final StringBuilder info = new StringBuilder();
    info.append("os.name").append(": ").append(getOsName()).append("\n");
    info.append("os.arch").append(": ").append(getOsArch()).append("\n");
    info.append("bit.model").append(": ").append(getBitModel()).append("\n");
    info.append("java.version").append(": ").append(javaVersion()).append("\n");
    info.append("java.specification.version").append(": ").append(javaSpecificationVersion())
        .append("\n");
    info.append("java.vendor").append(": ").append(javaVendor()).append("\n");
    return info.toString();
  }

  public static String getOsName() {
    return System.getProperty("os.name").toLowerCase().trim();

  }
  public static String getOsArch() {
    return System.getProperty("os.arch").toLowerCase().trim();
  }

  public static int getBitModel() {
    String prop = System.getProperty("sun.arch.data.model");
    if (prop == null) {
      prop = System.getProperty("com.ibm.vm.bitmode");
    }
    if (prop != null) {
      return Integer.parseInt(prop);
    }
    // GraalVM support, see https://github.com/fusesource/jansi/issues/162
    String arch = System.getProperty("os.arch");
    if (arch.endsWith("64") && "Substrate VM".equals(System.getProperty("java.vm.name"))) {
      return 64;
    }
    return -1; // we don't know...
  }

  public static String javaVersion() {
    return System.getProperty("java.version").toLowerCase().trim();
  }

  public static String javaSpecificationVersion() {
    return System.getProperty("java.specification.version").toLowerCase().trim();
  }

  public static String javaVendor() {
    return System.getProperty("java.vendor").toLowerCase().trim();
  }

  public static boolean isArm64() {
    String osArch = getOsArch();
    return osArch.contains("arm64") || osArch.contains("aarch64");
  }
}
