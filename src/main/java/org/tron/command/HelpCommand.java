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
package org.tron.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.peer.Peer;

import static org.fusesource.jansi.Ansi.ansi;

public class HelpCommand extends Command {
    public HelpCommand() {
    }

    @Override
    public void execute(Peer peer, String[] parameters) {
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
            case "getmessage":
            case "putmessage":
                new ConsensusCommand().usage();
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

        System.out.println( ansi().eraseScreen().render(
                "@|magenta,bold USAGE|@\n\t@|bold help [arguments]|@"
        ) );

        System.out.println("");

        System.out.println( ansi().eraseScreen().render(
                "@|magenta,bold AVAILABLE COMMANDS|@"
        ) );

        System.out.println("");

        System.out.println( ansi().eraseScreen().render(
                "\t@|bold version\t\tPrint the current java-tron version|@"
        ) );

        System.out.println( ansi().eraseScreen().render(
                "\t@|bold account\t\tGet your wallet address|@"
        ) );

        System.out.println( ansi().eraseScreen().render(
                "\t@|bold getbalance\t\tGet your balance|@"
        ) );

        System.out.println( ansi().eraseScreen().render(
                "\t@|bold send\t\tSend balance to receiver address|@"
        ) );

        System.out.println( ansi().eraseScreen().render(
                "\t@|bold printblockchain\t\tPrint blockchain|@"
        ) );

        System.out.println( ansi().eraseScreen().render(
                "\t@|bold consensus\t\tCreate a server|@"
        ) );

        System.out.println( ansi().eraseScreen().render(
                "\t@|bold getmessage\t\tGet a consensus message|@"
        ) );

        System.out.println( ansi().eraseScreen().render(
                "\t@|bold putmessage\t\tPut a consensus message|@"
        ) );

        System.out.println( ansi().eraseScreen().render(
                "\t@|bold exit\t\tExit java-tron application|@"
        ) );

        System.out.println("");

        System.out.println( ansi().eraseScreen().render(
                "Use @|bold help [topic] for more information about that topic.|@"
        ) );

        System.out.println("");
    }

    @Override
    public boolean check(String[] parameters) {
        return true;
    }
}
