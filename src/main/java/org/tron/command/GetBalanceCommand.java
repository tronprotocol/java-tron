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
import org.tron.protos.core.TronTXOutput;

import java.util.ArrayList;

public class GetBalanceCommand extends Command {
    public GetBalanceCommand() {
    }

    @Override
    public void execute(Peer peer, String[] parameters) {
        byte[] pubKeyHash = peer.getWallet().getEcKey().getPubKey();
        ArrayList<TronTXOutput.TXOutput> utxos = peer.getUTXOSet().findUTXO(pubKeyHash);

        long balance = 0;

        for (TronTXOutput.TXOutput txOutput : utxos) {
            balance += txOutput.getValue();
        }

        System.out.println(balance);
    }

    @Override
    public void usage() {
        System.out.println("");
        System.out.println("USAGE [getbalance]:");
        System.out.println("Command: getbalance");
        System.out.println("Description: Get your balance.");
        System.out.println("");
    }

    @Override
    public boolean check(String[] parameters) {
        return true;
    }
}
