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

import io.scalecube.cluster.Member;
import io.scalecube.transport.Message;
import java.util.Collection;
import java.util.Scanner;
import org.tron.common.overlay.node.GossipLocalNode;

public class GossipNode {

  private static GossipLocalNode localNode = GossipLocalNode.getInstance();

  public static void main(String[] args) {
    localNode.getCluster().listen().subscribe(msg -> {
      System.out.println(msg);
    });

    Scanner scanner = new Scanner(System.in);
    System.out.println("enter your message:");
    while (true) {
      String v = scanner.nextLine();

      String[] inputStrings = v.trim().split("\\s+", 2);

      switch (inputStrings[0]) {
        case "send":
          if (inputStrings.length > 1) {
            localNode.getCluster().otherMembers().forEach(member -> {
              localNode.getCluster().send(member, Message.fromData(inputStrings[1]));
            });
          }
          break;
        case "members":
          Collection<Member> memberList = localNode.getMembers();
          for (Member m : memberList) {
            System.out.println(m);
          }
          break;
        case "shutdown":
          localNode.getCluster().shutdown();
          System.exit(0);
          break;
        default:
          break;
      }

    }
  }
}
