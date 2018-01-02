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

import org.tron.peer.Peer;

public class ExitCommand extends Command {
    public ExitCommand() {
    }

    @Override
    public void execute(Peer peer, String[] parameters) {
        System.exit(0);
    }

    @Override
    public void usage() {
        System.out.println("");
        System.out.println("USAGE [exit]:");
        System.out.println("Command: exit | quit | bye");
        System.out.println("Description: Exit the program.");
        System.out.println("");
    }

    @Override
    public boolean check(String[] parameters) {
        return true;
    }
}
