package org.tron.plugins;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "keystore",
    mixinStandardHelpOptions = true,
    version = "keystore command 1.0",
    description = "Manage keystore files for account keys.",
    subcommands = {CommandLine.HelpCommand.class,
        KeystoreNew.class,
        KeystoreImport.class,
        KeystoreList.class,
        KeystoreUpdate.class
    },
    commandListHeading = "%nCommands:%n%nThe most commonly used keystore commands are:%n"
)
public class Keystore {
}
