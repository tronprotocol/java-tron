package org.tron.plugins;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import me.tongfei.progressbar.ProgressBar;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "mv", aliases = "move",
    description = "mv db to pre-set new path . For example HDD,reduce storage expenses.")
public class DbMove implements Callable<Integer> {

  private static final String PROPERTIES_CONFIG_KEY = "storage.properties";
  private static final String DB_DIRECTORY_CONFIG_KEY = "storage.db.directory";
  private static final String DEFAULT_DB_DIRECTORY = "database";
  private static final String NAME_CONFIG_KEY = "name";
  private static final String PATH_CONFIG_KEY = "path";
  private static final String NOT_FIND = "There is no database to be moved, exist.";

  @CommandLine.Spec
  CommandLine.Model.CommandSpec spec;

  @CommandLine.Option(names = {"-d", "--database-directory"},
      defaultValue = "output-directory",
      converter = Db.PathConverter.class,
      description = "database directory path. Default: ${DEFAULT-VALUE}")
  static Path database;

  @CommandLine.Option(names = {"-c", "--config"},
      defaultValue = "config.conf",
      converter = ConfigConverter.class,
      order = Integer.MAX_VALUE,
      description = " config file. Default: ${DEFAULT-VALUE}")
  Config config;

  @CommandLine.Option(names = {"-h", "--help"}, help = true, description = "display a help message")
  boolean help;

  @Override
  public Integer call() throws Exception {
    if (help) {
      spec.commandLine().usage(System.out);
      return 0;
    }

    if (config.hasPath(PROPERTIES_CONFIG_KEY)) {
      List<? extends Config> dbs = config.getConfigList(PROPERTIES_CONFIG_KEY);
      if (dbs.isEmpty()) {
        printNotExist();
        return 0;
      }
      String dbPath = config.hasPath(DB_DIRECTORY_CONFIG_KEY)
          ? config.getString(DB_DIRECTORY_CONFIG_KEY) : DEFAULT_DB_DIRECTORY;

      dbs = dbs.stream()
          .filter(c -> c.hasPath(NAME_CONFIG_KEY) && c.hasPath(PATH_CONFIG_KEY))
          .collect(Collectors.toList());

      if (dbs.isEmpty()) {
        printNotExist();
        return 0;
      }
      List<Property> toBeMove = dbs.stream()
          .map(c -> {
            try {
              return new Property(c.getString(NAME_CONFIG_KEY),
                  Paths.get(database.toString(), dbPath, c.getString(NAME_CONFIG_KEY)),
                  Paths.get(c.getString(PATH_CONFIG_KEY), dbPath, c.getString(NAME_CONFIG_KEY)));
            } catch (IOException e) {
              spec.commandLine().getErr().println(e);
            }
            return null;
          }).filter(Objects::nonNull)
          .filter(p -> !p.destination.equals(p.original)).collect(Collectors.toList());

      if (toBeMove.isEmpty()) {
        printNotExist();
        return 0;
      }
      toBeMove = toBeMove.stream()
          .filter(property -> {
            if (property.destination.toFile().exists()) {
              spec.commandLine().getOut().println(String.format("%s already exist,skip.",
                  property.destination));
              return false;
            } else {
              return true;
            }
          }).collect(Collectors.toList());

      if (toBeMove.isEmpty()) {
        printNotExist();
        return 0;
      }
      ProgressBar.wrap(toBeMove.stream(), "mv task").forEach(this::run);
      spec.commandLine().getOut().println("move db done.");

    } else {
      printNotExist();
      return 0;
    }
    return 0;
  }

  private void run(Property p) {
    if (p.destination.toFile().mkdirs()) {
      ProgressBar.wrap(Arrays.stream(Objects.requireNonNull(p.original.toFile().listFiles()))
          .filter(File::isFile).map(File::getName).parallel(), p.name).forEach(file -> {
            Path original = Paths.get(p.original.toString(), file);
            Path destination = Paths.get(p.destination.toString(), file);
            try {
              Files.copy(original, destination,
                  StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
              spec.commandLine().getErr().println(e);
            }
          });
      try {
        if (deleteDir(p.original.toFile())) {
          Files.createSymbolicLink(p.original, p.destination);
        }
      } catch (IOException | UnsupportedOperationException x) {
        spec.commandLine().getErr().println(x);
      }
    } else {
      spec.commandLine().getErr().println(String.format("%s create failed.", p.destination));
    }
  }

  private void printNotExist() {
    spec.commandLine().getErr().println(NOT_FIND);
  }

  /**
   * delete directory.
   */
  public static boolean deleteDir(File dir) {
    if (dir.isDirectory()) {
      String[] children = dir.list();
      if (children != null) {
        for (String child : children) {
          deleteDir(new File(dir, child));
        }
      }
    }
    return dir.delete();
  }

  static class Property {

    private final String name;
    private final Path original;
    final Path destination;

    public Property(String name, Path original, Path destination) throws IOException {
      this.name = name;
      this.original = original.toFile().getCanonicalFile().toPath();
      if (!this.original.toFile().exists()) {
        throw new IOException(this.original + " not exist!");
      }
      if (this.original.toFile().isFile()) {
        throw new IOException(this.original + " is a file!");
      }
      if (isSymbolicLink(original.toFile())) {
        throw new IOException(original + " is  symbolicLink!");
      }
      this.destination = destination.toFile().getCanonicalFile().toPath();
      if (this.destination.toFile().exists()) {
        throw new IOException(this.destination + " already exist!");
      }
      if (this.destination.equals(this.original)) {
        throw new IOException("destination and original can not be same:[" + this.original + "]!");
      }
    }

    public boolean isSymbolicLink(File file) throws IOException {
      if (file == null) {
        throw new NullPointerException("File must not be null");
      }

      File canon;
      if (file.getParent() == null) {
        canon = file;
      } else {
        File canonDir = file.getParentFile().getCanonicalFile();
        canon = new File(canonDir, file.getName());
      }
      return !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
    }
  }

  static class ConfigConverter implements CommandLine.ITypeConverter<Config> {
    private  final Exception notFind =
        new IllegalArgumentException("There is no database to be moved,please check.");

    ConfigConverter() {
    }

    public Config convert(String value) throws Exception {
      File file  = Paths.get(value).toFile();
      if (file.exists() && file.isFile()) {
        Config config = ConfigFactory.parseFile(Paths.get(value).toFile());
        if (config.hasPath(PROPERTIES_CONFIG_KEY)) {
          List<? extends Config> dbs = config.getConfigList(PROPERTIES_CONFIG_KEY);
          if (dbs.isEmpty()) {
            throw notFind;
          }
          String dbPath = config.hasPath(DB_DIRECTORY_CONFIG_KEY)
              ? config.getString(DB_DIRECTORY_CONFIG_KEY) : DEFAULT_DB_DIRECTORY;

          dbs = dbs.stream()
              .filter(c -> c.hasPath(NAME_CONFIG_KEY) && c.hasPath(PATH_CONFIG_KEY))
              .collect(Collectors.toList());

          if (dbs.isEmpty()) {
            throw notFind;
          }
          Set<String> toBeMove = new HashSet<>();
          for (Config c : dbs) {
            if (!toBeMove.add(new Property(c.getString(NAME_CONFIG_KEY),
                Paths.get(database.toString(), dbPath, c.getString(NAME_CONFIG_KEY)),
                Paths.get(c.getString(PATH_CONFIG_KEY), dbPath,
                    c.getString(NAME_CONFIG_KEY))).name)) {
              throw new IllegalArgumentException(
                  "DB config has duplicate key:[" + c.getString(NAME_CONFIG_KEY)
                      + "],please check! ");
            }
          }
        } else {
          throw notFind;
        }
        return config;
      } else {
        throw new IOException("DB config [" + value + "] not exist!");
      }
    }
  }
}
