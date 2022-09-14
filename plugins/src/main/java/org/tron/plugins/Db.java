package org.tron.plugins;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import picocli.CommandLine;

@CommandLine.Command(name = "db",
    mixinStandardHelpOptions = true,
    version = "db command 1.0",
    description = "An rich command set that provides high-level operations  for dbs.",
    subcommands = {CommandLine.HelpCommand.class, DbMove.class},
    commandListHeading = "%nCommands:%n%nThe most commonly used git commands are:%n"
)
public class Db {

  static class ConfigConverter implements CommandLine.ITypeConverter<Config> {
    ConfigConverter() {
    }

    public Config convert(String value) throws IOException {
      File file  = Paths.get(value).toFile();
      if (file.exists() && file.isFile()) {
        return ConfigFactory.parseFile(Paths.get(value).toFile());
      } else {
        throw new IOException("DB config [" + value + "] not exist!");
      }
    }
  }

  static class PathConverter implements CommandLine.ITypeConverter<Path> {
    PathConverter() {
    }

    public Path convert(String value) throws IOException {
      File file  = Paths.get(value).toFile();
      if (file.exists() && file.isDirectory()) {
        return file.toPath();
      } else {
        throw new IOException("DB path [" + value + "] not exist!");
      }
    }
  }
}
