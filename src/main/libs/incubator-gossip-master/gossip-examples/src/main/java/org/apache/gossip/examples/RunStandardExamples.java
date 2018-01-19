/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gossip.examples;

import java.io.IOException;

public class RunStandardExamples {

  private static boolean WILL_READ = true;

  private static boolean WILL_NOT_READ = false;

  public static void main(String[] args) {
    if ((args.length < 1) || args[0].equals("-h") || args[0].equals("--help") || args.length < 2) {
      System.out.print(usage());
      return;
    }
    try {
      int example = intFromString(args[0]);
      int channel = intFromString(args[1]);
      if ((example < 1) || (example > 4) || (channel < 0) || (channel > 2)) {
        System.out.print(usage());
        return;
      }
      runExaple(example, channel);
    } catch (Exception e) {
      System.out.print(usage());
    }
  }

  private static void runExaple(int exampleNumber, int channel) throws IOException {
    String[] args = stanardArgs(channel, new String[4]);
    if (exampleNumber == 1) {
      StandAloneNode example = new StandAloneNode(args);
      example.exec(WILL_NOT_READ);
    } else if (exampleNumber == 2) {
      StandAloneNodeCrdtOrSet example = new StandAloneNodeCrdtOrSet(args);
      example.exec(WILL_READ);
    } else if (exampleNumber == 3) {
      StandAlonePNCounter example = new StandAlonePNCounter(args);
      example.exec(WILL_READ);
    } else if (exampleNumber == 4) {
      args = extendedArgs(channel, new String[6]);
      StandAloneDatacenterAndRack example = new StandAloneDatacenterAndRack(args);
      example.exec(WILL_READ);
    }
  }

  private static String[] stanardArgs(int channel, String[] args) {
    // see README.md for examples
    args[0] = "udp://localhost:1000" + channel;
    args[1] = "" + channel;
    args[2] = "udp://localhost:10000";
    args[3] = "0";
    return args;
  }

  private static String[] extendedArgs(int channel, String[] args) {
    args = stanardArgs(channel, args);
    // see README.md for examples
    if (channel == 0) {
      args[4] = "1";
      args[5] = "2";
    }
    if (channel == 1) {
      args[4] = "1";
      args[5] = "3";
    }
    if (channel == 2) {
      args[4] = "2";
      args[5] = "2";
    }
    return args;
  }

  private static int intFromString(String string) {
    return Integer.parseInt(string);
  }

  private static String usage() {
    return "Select and run (usually in a seperate terminal window) \n"
            + "one of the the standard Examples,\n" + " 1. StandAloneNode\n"
            + " 2. StandAloneNodeCrdtOrSet\n" + " 3. StandAlonePNCounter\n"
            + " 4. StandAloneDatacenterAndRack\n" + "(See README.md in this modules)\n" + "\n"
            + "Usage: mvn exec:java -Dexec.mainClass=org.apache.gossip.examples.RunStandardExamples  -Dexec.args=\"s c\"\n"
            + "where...\n" + "  s - int - the example number from above\n"
            + "  c - int - the channel number: 0, 1, or 2\n";
  }

}
