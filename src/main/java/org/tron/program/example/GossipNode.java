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

import org.apache.gossip.LocalMember;
import org.tron.common.overlay.gossip.LocalNode;

import java.util.List;
import java.util.Scanner;

public class GossipNode {
    private static LocalNode localNode = LocalNode.getInstance();

    public static void main(String[] args) {
        localNode.getGossipManager().registerSharedDataSubscriber((key, oldValue, newValue) -> {
            System.out.println("new message: " + newValue);
        });

        Scanner scanner = new Scanner(System.in);
        System.out.println("enter your message:");
        while (true) {
            String v = scanner.nextLine();

            String[] inputStrings = v.trim().split("\\s+", 2);

            switch (inputStrings[0]) {
                case "send":
                    if (inputStrings.length > 1) {
                    }
                    break;
                case "live":
                    printLiveMembers();
                    break;
                case "dead":
                    printDeadMembers();
                    break;
                default:
                    break;
            }

        }
    }

    public static void printLiveMembers() {
        List<LocalMember> memberList = localNode.getLiveMembers();

        if (memberList.isEmpty()) {
            System.out.println("live none");
            return;
        }

        for (LocalMember member :
                memberList) {
            System.out.println(member);
        }
    }

    public static void printDeadMembers() {
        List<LocalMember> memberList = localNode.getDeadMembers();

        if (memberList.isEmpty()) {
            System.out.println("dead none");
            return;
        }

        for (LocalMember member :
                memberList) {
            System.out.println(member);
        }
    }
}
