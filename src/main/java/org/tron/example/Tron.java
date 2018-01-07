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
package org.tron.example;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.tron.command.Cli;
import org.tron.config.Configer;
import org.tron.peer.Peer;
import org.tron.peer.PeerBuilder;
import org.tron.peer.PeerType;

public class Tron {

    @Parameter(names = {"--type", "-t"}, validateWith = PeerType.class)
    private String type = "normal";

    private static Peer peer;

    public static void main(String[] args) {
        Tron tron = new Tron();
        JCommander.newBuilder()
                .addObject(tron)
                .build()
                .parse(args);
        tron.run();
    }

    public void run() {
        peer = new PeerBuilder()
                .setKey(Configer.getMyKey())
                .setType(type)
                .build();

        Cli cli = new Cli();
        cli.run(peer);
    }

    public static Peer getPeer() {
        return peer;
    }
}
