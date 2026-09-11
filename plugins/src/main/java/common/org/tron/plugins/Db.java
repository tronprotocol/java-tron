package org.tron.plugins;

import picocli.CommandLine;

@CommandLine.Command(name = "db",
    mixinStandardHelpOptions = true,
    version = "db command 1.0",
    description = "An rich command set that provides high-level operations  for dbs.",
    header = "All `db` tools operate directly on the database files.\n"
        + "Before performing a database operation,\n"
        + "you must stop the currently running FullNode service.\n",
    subcommands = {CommandLine.HelpCommand.class,
        DbMove.class,
        DbArchive.class,
        DbConvert.class,
        DbLite.class,
        DbCopy.class,
        DbRoot.class
    },
    commandListHeading = "%nCommands:%n%nThe most commonly used db commands are:%n"
)
public class Db {
}
