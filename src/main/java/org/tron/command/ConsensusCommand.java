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

import org.tron.consensus.client.Client;
import org.tron.consensus.server.Server;

import static org.fusesource.jansi.Ansi.ansi;

public class ConsensusCommand {

    public ConsensusCommand() {

    }

    public void server() {
        Server.serverRun();
    }

    public void putClient(String[] args) {
        Client.putMessage(args);
    }

    public void getClient(String[] args) {
        Client.getMessage(args[0]);
    }

    public void usage() {
        System.out.println("");

        System.out.println( ansi().eraseScreen().render(
                "@|magenta,bold USAGE|@\n\t@|bold consensus|@"
        ) );

        System.out.println("");

        System.out.println( ansi().eraseScreen().render(
                "@|magenta,bold DESCRIPTION|@\n\t@|bold The command 'consensus' create a server.|@"
        ) );

        System.out.println("");

        System.out.println( ansi().eraseScreen().render(
                "@|magenta,bold USAGE|@\n\t@|bold getmessage [key]|@"
        ) );

        System.out.println("");

        System.out.println( ansi().eraseScreen().render(
                "@|magenta,bold DESCRIPTION|@\n\t@|bold The command 'getmessage' get a consensus message.|@"
        ) );

        System.out.println("");

        System.out.println( ansi().eraseScreen().render(
                "@|magenta,bold USAGE|@\n\t@|bold putmessage [key] [value]|@"
        ) );

        System.out.println("");

        System.out.println( ansi().eraseScreen().render(
                "@|magenta,bold DESCRIPTION|@\n\t@|bold The command 'putmessage' put a consensus message.|@"
        ) );

        System.out.println("");
    }

}
