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

public class HelpCommand extends Command {
    public HelpCommand() {
    }

    @Override
    public void execute(Peer peer, String[] parameters) {
        new ExitCommand().usage();
        new SendCommand().usage();
        new GetBalanceCommand().usage();
        new AccountCommand().usage();
        new PrintBlockchainCommand().usage();
        new ConsensusCommand().usage();
    }

    @Override
    public void usage() {
        System.out.printf("USAGE [help]");
        System.out.println("help");
    }

    @Override
    public boolean check(String[] parameters) {
        return true;
    }
}
