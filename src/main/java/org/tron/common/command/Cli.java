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

import java.util.Arrays;
import java.util.Scanner;

public class Cli {

  public Cli() {

  }

  public void run(CliApplication app) {
    Scanner in = new Scanner(System.in);

    while (true) {
      String cmd = in.nextLine().trim();

      String[] cmdArray = cmd.split("\\s+");
      // split on trim() string will always return at the minimum: [""]
      if ("".equals(cmdArray[0])) {
        continue;
      }

      String[] cmdParameters = Arrays.copyOfRange(cmdArray, 1, cmdArray.length);

      switch (cmdArray[0]) {
        case "version":
          new VersionCommand().execute(app, cmdParameters);
          break;
        case "account":
          new AccountCommand().execute(app, cmdParameters);
          break;
        case "getbalance":
          new GetBalanceCommand().execute(app, cmdParameters);
          break;
        case "send":
          //app.getInjector().getInstance(ConsensusCommand.class).execute(app, cmdParameters);
          break;
        case "printblockchain":
          new PrintBlockchainCommand().execute(app, cmdParameters);
          break;
        case "listen":
          //new ConsensusCommand().getClient(peer);
          break;
        case "exit":
        case "quit":
        case "bye":
          new ExitCommand().execute(app, cmdParameters);
        case "help":
        default:
          new HelpCommand().execute(app, cmdParameters);
          break;
      }
    }
  }
}
