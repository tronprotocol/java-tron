package org.tron.plugins;

import com.typesafe.config.Config;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
  private static final String NOT_FIND = "The database to be moved cannot be found.";

  @CommandLine.Spec
  CommandLine.Model.CommandSpec spec;

  @CommandLine.Option(names = {"-d", "--database-directory"},
      defaultValue = "output-directory",
      description = "database directory path. Default: ${DEFAULT-VALUE}")
  String database;

  @CommandLine.Option(names = {"-c", "--config"},
      defaultValue = "config.conf",
      converter = Db.ConfigConverter.class,
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
        spec.commandLine().getOut().println(NOT_FIND);
        return 0;
      }
      String dbPath = config.hasPath(DB_DIRECTORY_CONFIG_KEY)
          ? config.getString(DB_DIRECTORY_CONFIG_KEY) : DEFAULT_DB_DIRECTORY;

      dbs = dbs.stream()
          .filter(c -> c.hasPath(NAME_CONFIG_KEY) && c.hasPath(PATH_CONFIG_KEY))
          .collect(Collectors.toList());

      if (dbs.isEmpty()) {
        spec.commandLine().getOut().println(NOT_FIND);
        return 0;
      }
      List<Property> toBeMove = dbs.stream()
          .map(c -> {
            try {
              return new Property(c.getString(NAME_CONFIG_KEY),
                  Paths.get(database, dbPath, c.getString(NAME_CONFIG_KEY)),
                  Paths.get(c.getString(PATH_CONFIG_KEY), dbPath, c.getString(NAME_CONFIG_KEY)));
            } catch (IOException e) {
              spec.commandLine().getErr().println(e);
            }
            return null;
          }).filter(Objects::nonNull)
          .filter(p -> !p.destination.equals(p.original)).collect(Collectors.toList());

      if (toBeMove.isEmpty()) {
        spec.commandLine().getOut().println(NOT_FIND);
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
        spec.commandLine().getOut().println(NOT_FIND);
        return 0;
      }
      ProgressBar.wrap(toBeMove.stream(), "mv task").forEach(this::run);
      spec.commandLine().getOut().println("move db done.");

    } else {
      spec.commandLine().getOut().println(NOT_FIND);
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
      this.destination = destination.toFile().getCanonicalFile().toPath();
    }
  }
}
