package org.tron.plugins;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
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

    public Config convert(String value) {
      return ConfigFactory.parseFile(Paths.get(value).toFile());
    }
  }
}
