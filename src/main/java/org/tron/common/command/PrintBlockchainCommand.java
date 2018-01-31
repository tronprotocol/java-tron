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

import static org.fusesource.jansi.Ansi.ansi;

import org.tron.common.application.CliApplication;
import org.tron.core.BlockUtils;
import org.tron.core.Blockchain;
import org.tron.core.BlockchainIterator;
import org.tron.core.peer.Peer;
import org.tron.protos.Protocal.Block;


public class PrintBlockchainCommand extends Command {

  public PrintBlockchainCommand() {
  }

  @Override
  public void execute(CliApplication app, String[] parameters) {
    Peer peer = app.getPeer();
    Blockchain blockchain = peer.getUTXOSet().getBlockchain();
    BlockchainIterator bi = new BlockchainIterator(blockchain);
    while (bi.hasNext()) {
      Block block = bi.next();
      System.out.println(BlockUtils.toPrintString(block));
    }
  }

  @Override
  public void usage() {
    System.out.println("");

    System.out.println(ansi().eraseScreen().render(
        "@|magenta,bold USAGE|@\n\t@|bold printblockchain|@"
    ));

    System.out.println("");

    System.out.println(ansi().eraseScreen().render(
        "@|magenta,bold DESCRIPTION|@\n\t@|bold The command 'printblockchain' print blockchain.|@"
    ));

    System.out.println("");
  }

  @Override
  public boolean check(String[] parameters) {
    return true;
  }
}
