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

import static org.fusesource.jansi.Ansi.Color.MAGENTA;
import static org.fusesource.jansi.Ansi.ansi;

import org.tron.common.application.CliApplication;

public class VersionCommand extends Command {
  public VersionCommand() {
  }

  @Override
  public void execute(CliApplication app, String[] parameters) {
    usage();
  }

  @Override
  public void usage() {
    System.out.println("");

    String versionBanner = "" +
        "     __                                __                                      \n" +
        "    |__|____ ___  _______            _/  |________  ____   ____                \n" +
        "    |  \\__  \\\\  \\/ /\\__  \\    ______ \\   __\\_  __ \\/  _ \\ /    \\    \n" +
        "    |  |/ __ \\\\   /  / __ \\_ /_____/  |  |  |  | \\(  <_> )   |  \\         \n" +
        "/\\__|  (____  /\\_/  (____  /          |__|  |__|   \\____/|___|  /           \n" +
        "\\______|    \\/           \\/                                   \\/           \n";

    System.out.println(ansi().eraseScreen().fg(MAGENTA).a(versionBanner).reset());

    System.out.println("");
  }

  @Override
  public boolean check(String[] parameters) {
    return true;
  }
}
