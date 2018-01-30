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

package org.tron.program.example;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.tron.common.application.ApplicationImpl;
import org.tron.common.application.Application;
import org.tron.core.peer.Peer;
import org.tron.core.peer.PeerType;
import org.tron.program.Args;

public class Tron {

  private static Peer peer;
  @Parameter(names = {"--type", "-t"}, validateWith = PeerType.class)
  private String type = "normal";


  public static void main(String[] args) {
    Tron tron = new Tron();
    JCommander.newBuilder()
        .addObject(tron)
        .build()
        .parse(args);
    tron.run();
  }

  public static Peer getPeer() {
    return peer;
  }

  public void run() {

//    CliApplication app = new ApplicationFactory()
//        .buildCli();
    Application app = new ApplicationImpl();
    app.initServices(new Args());
    app.startServies();
    app.startup();

//    app.(new Server());
//    app.
//    app.r

//    Peer peer = app.getInjector().getInstance(PeerBuilder.class)
//        .setKey(Configer.getMyKey())
//        .setType(type)
//        .build();

    // app.setPeer(peer);

//    Cli cli = new Cli();
//    cli.run(app);
  }
}
