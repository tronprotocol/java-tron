///*
// * java-tron is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * java-tron is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//package org.tron.common.overlay.gossip;
//
//import org.tron.common.overlay.message.Message;
//import org.tron.common.overlay.message.Type;
//
//import java.net.InetAddress;
//import java.net.UnknownHostException;
//import java.util.Scanner;
//
//public class NodeSingleton {
//    private final static String CLUSTER = "mycluster";
//    private final static String PORT = ":10000";
//
//    public static void main(String[] args) {
//        String uri = "";
//        String id = "";
//        try {
//            String ip = InetAddress.getLocalHost().getHostAddress();
//            uri = "udp://" + ip + PORT;
//            id = ip + PORT;
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//        }
//
//        System.out.println("start node: " + uri);
//        System.out.println("id: " + id);
//        LocalNode standNode = new LocalNode(CLUSTER, uri, id);
//
//        standNode.getGossipManager().registerSharedDataSubscriber((key, oldValue, newValue) -> {
//            System.out.println("new message: " + newValue);
//        });
//
//        Scanner scanner = new Scanner(System.in);
//        System.out.println("enter your message:");
//        while (true) {
//            String v = scanner.nextLine();
//
//            String[] inputStrings = v.trim().split("\\s+", 2);
//
//            switch (inputStrings[0]) {
//                case "send":
//                    if (inputStrings.length > 1) {
//                        standNode.broadcast(new Message(inputStrings[1], Type.BLOCK));
//                    }
//                    break;
//                case "live":
//                    standNode.printLiveMembers();
//                    break;
//                case "dead":
//                    standNode.printDeadMambers();
//                    break;
//                default:
//                    break;
//            }
//
//        }
//    }
//}
