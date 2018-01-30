/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.command;

import org.tron.common.application.CliApplication;

import static org.fusesource.jansi.Ansi.ansi;

public class HelpCommand extends Command {

  public HelpCommand() {
  }

  @Override
  public void execute(CliApplication app, String[] parameters) {

    if (parameters.length == 0) {
      usage();
      return;
    }

    switch (parameters[0]) {
      case "version":
        new VersionCommand().usage();
        break;
      case "account":
        new AccountCommand().usage();
        break;
      case "getbalance":
        new GetBalanceCommand().usage();
        break;
      case "send":
        new SendCommand().usage();
        break;
      case "printblockchain":
        new PrintBlockchainCommand().usage();
        break;
      case "consensus":
      case "listen":
        //app.getInjector().getInstance(ConsensusCommand.class).usage();
        break;
      case "exit":
      case "quit":
      case "bye":
        new ExitCommand().usage();
        break;
      case "help":
      default:
        new HelpCommand().usage();
        break;
    }
  }

  @Override
  public void usage() {
    System.out.println("");

    System.out.println(ansi().eraseScreen().render(
        "@|magenta,bold USAGE|@\n\t@|bold help [arguments]|@"
    ));

    System.out.println("");

    System.out.println(ansi().eraseScreen().render(
        "@|magenta,bold AVAILABLE COMMANDS|@"
    ));

    System.out.println("");

    System.out.println(ansi().eraseScreen().render(
        String.format("\t@|bold %-20s\tPrint the current java-tron version|@", "version")
    ));

    System.out.println(ansi().eraseScreen().render(
        String.format("\t@|bold %-20s\tGet your wallet address|@", "account")
    ));

    System.out.println(ansi().eraseScreen().render(
        String.format("\t@|bold %-20s\tGet your balance|@", "getbalance")
    ));

    System.out.println(ansi().eraseScreen().render(
        String.format("\t@|bold %-20s\tSend balance to receiver address|@", "send")
    ));

    System.out.println(ansi().eraseScreen().render(
        String.format("\t@|bold %-20s\tPrint blockchain|@", "printblockchain")
    ));

    System.out.println(ansi().eraseScreen().render(
        String.format("\t@|bold %-20s\tExit java-tron application|@", "exit")
    ));

    System.out.println("");

    System.out.println(ansi().eraseScreen().render(
        "Use @|bold help [topic] for more information about that topic.|@"
    ));

    System.out.println("");
  }

  @Override
  public boolean check(String[] parameters) {
    return true;
  }
}
