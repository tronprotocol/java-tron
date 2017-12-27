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
        System.out.println("consensus server");
        System.out.println("Command: consensus");
        System.out.println("Description: Create a server.");
        System.out.println("");

        System.out.println("");
        System.out.println("get Message");
        System.out.println("Command: getmessage [key]");
        System.out.println("Description: Get consensus Message");
        System.out.println("");

        System.out.println("");
        System.out.println("put Message");
        System.out.println("Command: putmessage [key] [value]");
        System.out.println("Description: Put a consensus Message");
        System.out.println("");
    }

}
