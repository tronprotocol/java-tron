package org.tron.plugins;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import org.tron.plugins.utils.FileUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Slf4j(topic = "move")
@Command(name = "mv", aliases = "move",
    description = "Move db to pre-set new path . For example HDD,reduce storage expenses.")
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
      converter = PathConverter.class,
      description = "database directory path. Default: ${DEFAULT-VALUE}")
  static Path database;

  @CommandLine.Option(names = {"-c", "--config"},
      defaultValue = "config.conf",
      converter = ConfigConverter.class,
      description = " config file. Default: ${DEFAULT-VALUE}")
  Config config;

  @CommandLine.Option(names = {"-h", "--help"})
  static boolean help;

  @Override
  public Integer call() throws Exception {
    if (help) {
      spec.commandLine().usage(System.out);
      help = false;
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
      List<Property> toBeMove = new ArrayList<>();
      for (Config c : dbs) {
        try {
          toBeMove.add(new Property(c.getString(NAME_CONFIG_KEY),
              Paths.get(database.toString(), dbPath, c.getString(NAME_CONFIG_KEY)),
              Paths.get(c.getString(PATH_CONFIG_KEY), dbPath, c.getString(NAME_CONFIG_KEY))));
        } catch (IOException e) {
          spec.commandLine().getErr().println(e);
          return 2;
        }
      }
      boolean allCopied = ProgressBar.wrap(toBeMove.stream(), "copy task")
          .allMatch(this::copy);
      if (!allCopied) {
        cleanupDestinations(toBeMove);
        return 1;
      }

      boolean allMoved = ProgressBar.wrap(toBeMove.stream(), "link task")
          .map(this::replaceSourceWithLink).reduce(Boolean.TRUE, Boolean::logicalAnd);
      if (!allMoved) {
        return 1;
      }
      spec.commandLine().getOut().println("move db done.");
    } else {
      printNotExist();
      return 0;
    }
    return 0;
  }

  private boolean copy(Property p) {
    if (!p.destination.toFile().mkdirs()) {
      spec.commandLine().getErr().println(String.format("%s create failed.", p.destination));
      return false;
    }

    AtomicBoolean hasError = new AtomicBoolean(false);
    try (Stream<Path> files = Files.walk(p.original)) {
      ProgressBar.wrap(files.parallel(), p.name).forEach(source -> {
        if (hasError.get()) {
          return;
        }
        try {
          copyEntry(p, source);
        } catch (IOException e) {
          hasError.set(true);
          spec.commandLine().getErr().println(e);
        }
      });
    } catch (IOException | UncheckedIOException e) {
      hasError.set(true);
      spec.commandLine().getErr().println(e);
    }

    if (hasError.get()) {
      spec.commandLine().getErr().println(String.format(
          "%s copy to %s failed, source kept.",
          p.original, p.destination));
      return false;
    }
    return true;
  }

  private void copyEntry(Property p, Path source) throws IOException {
    BasicFileAttributes attributes = Files.readAttributes(
        source, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    Path destination = p.destination.resolve(p.original.relativize(source));
    if (attributes.isDirectory()) {
      Files.createDirectories(destination);
    } else if (attributes.isRegularFile()) {
      Files.createDirectories(destination.getParent());
      Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    } else {
      throw new IOException(String.format(
          "%s is neither a regular file nor a directory, can not be moved.", source));
    }
  }

  private boolean replaceSourceWithLink(Property p) {
    try {
      if (!FileUtils.deleteDir(p.original.toFile())) {
        spec.commandLine().getErr().println(String.format(
            "%s delete failed and may be incomplete; the only complete copy is at %s, keep it.",
            p.original, p.destination));
        printRecoveryHint(p);
        return false;
      }
      Files.createSymbolicLink(p.original, p.destination);
      return true;
    } catch (IOException | RuntimeException x) {
      spec.commandLine().getErr().println(x);
      spec.commandLine().getErr().println(String.format(
          "%s move failed; the complete copy is at %s, keep it.",
          p.original, p.destination));
      printRecoveryHint(p);
      return false;
    }
  }

  private void printRecoveryHint(Property p) {
    spec.commandLine().getErr().println(String.format(
        "To recover manually: remove %s if present, then create a symbolic link at %s"
            + " pointing to %s.",
        p.original, p.original, p.destination));
  }

  private void cleanupDestinations(List<Property> properties) {
    boolean allCleaned = properties.stream().map(property -> {
      File destination = property.destination.toFile();
      if (Files.notExists(destination.toPath(), LinkOption.NOFOLLOW_LINKS)) {
        return true;
      }
      try {
        if (FileUtils.deleteDir(destination)) {
          return true;
        }
      } catch (RuntimeException e) {
        spec.commandLine().getErr().println(e);
      }
      spec.commandLine().getErr().println(String.format(
          "%s cleanup failed; remove the leftover copy before retrying.",
          property.destination));
      return false;
    }).reduce(Boolean.TRUE, Boolean::logicalAnd);

    if (allCleaned) {
      spec.commandLine().getErr().println(
          "move db failed; all source databases were kept, please retry.");
    } else {
      spec.commandLine().getErr().println(
          "move db failed; all source databases were kept, but leftover copies remain.");
    }
  }

  private void printNotExist() {
    spec.commandLine().getErr().println(NOT_FIND);
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
      if (FileUtils.isSymbolicLink(original.toFile())) {
        throw new IOException(original + " is  symbolicLink!");
      }
      this.destination = destination.toFile().getCanonicalFile().toPath();
      if (!Files.notExists(this.destination, LinkOption.NOFOLLOW_LINKS)) {
        throw new IOException(this.destination + " already exist!");
      }
      if (this.destination.equals(this.original)) {
        throw new IOException("destination and original can not be same:[" + this.original + "]!");
      }
    }
  }

  static class ConfigConverter implements CommandLine.ITypeConverter<Config> {
    private  final Exception notFind =
        new IllegalArgumentException("There is no database to be moved,please check.");

    ConfigConverter() {
    }

    public Config convert(String value) throws Exception {
      if (help) {
        return null;
      }
      File file  = Paths.get(value).toFile();
      if (file.exists() && file.isFile()) {
        Config config = ConfigFactory.parseFile(Paths.get(value).toFile());
        if (config.hasPath(PROPERTIES_CONFIG_KEY)) {
          List<? extends Config> dbs = config.getConfigList(PROPERTIES_CONFIG_KEY);
          if (dbs.isEmpty()) {
            throw notFind;
          }
          dbs = dbs.stream()
              .filter(c -> c.hasPath(NAME_CONFIG_KEY) && c.hasPath(PATH_CONFIG_KEY))
              .collect(Collectors.toList());

          if (dbs.isEmpty()) {
            throw notFind;
          }
          Set<String> toBeMove = new HashSet<>();
          for (Config c : dbs) {
            String name = c.getString(NAME_CONFIG_KEY);
            if (!toBeMove.add(name)) {
              throw new IllegalArgumentException(
                  "DB config has duplicate key:[" + name + "],please check! ");
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

  static class PathConverter implements CommandLine.ITypeConverter<Path> {
    PathConverter() {
    }

    public Path convert(String value) throws IOException {
      if (help) {
        return null;
      }
      File file  = Paths.get(value).toFile();
      if (file.exists() && file.isDirectory()) {
        return file.toPath();
      } else {
        throw new IOException("DB path [" + value + "] not exist!");
      }
    }
  }
}
