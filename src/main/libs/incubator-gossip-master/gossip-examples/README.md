Running the Examples
===================================================

Apache Gossip is designed to run as library used by others. That is, it in intended as a feature to be embedded in other code.

These examples illustrate some simple cases of the invocation of Gossip from an "thin" application layer, to illustrate various 
features of the library. 

For additional information see:
* This [YouTube video](https://www.youtube.com/watch?v=bZXZrp7yBkw&t=39s) illustrating and illuminating the first example.
* This [YouTube video](https://www.youtube.com/watch?v=SqkJs0QDRdk) illustrating and illuminating the second example.
* A [general description of the Gossip Protocol](https://en.wikipedia.org/wiki/Gossip_protocol)

Initial setup - Preconditions
-----------------------------
These instructions assume that you are using a Unix-like command line interface; translate as necessary.

Prior to running these examples you will need to have your environment set up to run java and Maven
commands: 
* install java 8 - [https://java.com/](https://java.com/), and
* install Maven - [https://maven.apache.org/install.html](https://maven.apache.org/install.html)

Then, you will need a local copy of the code. The simplest is to download the project to a local folder:
* browse to [https://github.com/apache/incubator-gossip](https://github.com/apache/incubator-gossip)
* click on the "Clone of Download" button
* click on the "Download ZIP" option
* unzip the file to in a convenient location
In what follows, we will call the resulting project folder **incubator-gossip**.

As an alternative, you can also [clone](https://help.github.com/articles/cloning-a-repository/)
the GitHub repository.

Lastly, you will need to use Maven to build and install the necessary dependencies:
```
cd incubator-gossip
mvn install -DskipTests
```

When all that is finished are you ready to start running the first example...


Running org.apache.gossip.examples.StandAloneNode
-------------------------------------------------
This first example illustrates the basic, underlying, communication layer the sets up and maintains a gossip network cluster.

In the [YouTube video](https://www.youtube.com/watch?v=bZXZrp7yBkw&t=39s) there is a description of
the architecture and a demonstration of running of this example. While the video was created with an earlier version of the system,
these instructions will get you going on that example.

To run this example from the code (in a clone or download of the archive) simply change your working directory
to the gossip-examples module and run the application in maven.

Specifically, after cloning or downloading the repository:
```
cd incubator-gossip/gossip-examples
mvn exec:java -Dexec.mainClass=org.apache.gossip.examples.StandAloneNode -Dexec.args="udp://localhost:10000 0 udp://localhost:10000 0"
```

This sets up a single StandAloneNode that starts out listening to itself. The arguments are:
1. The URI (host and port) for the node - **udp://localhost:10000**
2. The id for the node - **0**
3. The URI for a "seed" node - **udp://localhost:10000**
4. The id for that seed node - **0**

Note. To stop the the example, simply kill the process (control-C, for example).

In this application, the output uses a "terminal escape sequence" that clears the 
terminal display and resets the cursor to the upper left corner.
if, for some reason, this is not working in your case, you can add the (optional) flag '-s' to the args in the 
command line, to suppress this "clear screen" behavior. That is:
```
cd incubator-gossip/gossip-examples
mvn exec:java -Dexec.mainClass=org.apache.gossip.examples.StandAloneNode -Dexec.args="-s udp://localhost:10000 0 udp://localhost:10000 0"
```

Normally, you would set up nodes in a network on separate hosts, but in this case, illustrating the basic communications in gossip,
we are just running all the nodes on the same host, localhost. The seed node will generally be one of the other nodes in the
network: enough information for this node to (eventually) acquire the list of all nodes in its cluster.

You will see that this gossip node prints out two list of know other nodes, live and dead. The live nodes are nodes assumed to be active and
connected, the dead nodes are nodes that have "gone missing" long enough to be assumed dormant or disconnected. See details in the video.

With only a single node running in this cluster, there are no other nodes detected, so both the live and dead lists are empty.

Then, in a separate terminal window, cd to the same folder and enter the the same run command with the first two arguments 
changed to reflect the fact that this is a different node
1. host/port for the node - **udp://localhost:10001**
2. id for the node - **1**

That is:
```
cd incubator-gossip/gossip-examples
mvn exec:java -Dexec.mainClass=org.apache.gossip.examples.StandAloneNode -Dexec.args="udp://localhost:10001 1 udp://localhost:10000 0"
```

Now, because the "seed node" is the first node that we started, this second node is listening to the first node, 
and they exchange list of known nodes. And start listening to each other, so that in short order they both have a list of live notes
with one member, the other live node in the cluster.

Finally, in yet another terminal window, cd to the same folder and enter the the same run command with the first two arguments changed to
1. host/port for the node - **udp://localhost:10002**
2. id for the node - **2**

```
cd incubator-gossip/gossip-examples
mvn exec:java -Dexec.mainClass=org.apache.gossip.examples.StandAloneNode -Dexec.args="udp://localhost:10002 2 udp://localhost:10000 0”
```

Now the lists of live nodes for the cluster converge to reflect each node's connections to the other two nodes.

To see a node moved to the dead list. Kill the process in one of the running terminals: enter control-c in the terminal window.
Then the live-dead list of the two remaining nodes will converge to show one node live and one node dead. 
Start the missing node again, and it will again appear in the live list. 
Note, that it does not matter which node goes dormant, every live node (eventually) communicates with some other 
live node in the cluster and gets the necessary updated status information.

If you read the code, you will see that it defines a cluster (name = 'mycluster') and a number of other setup details. See the video for
a more complete description of the setup and for a more details description of the interactions among the nodes.

Also note, that the process of running these nodes produces a set of JSON files recording node state (in the 'base' directory from where the node
running). This enables a quicker startup as failed nodes recover. In this example, to recover a node that you have killed, 
using control-c, simply re-issue the command to run the node.


Running org.apache.gossip.examples.StandAloneNodeCrdtOrSet
----------------------------------------------------------
This second example illustrates the using the data layer to share structured information: a shared representation of a set
of strings, and a shared counter. The objects representing those shared values are special cases of a Conflict-free Replicated
Data Type (hence, CRDT). 

Here is the problem that is solved by these CRDT objects: since each node in a cluster can message any other node in the cluster
in any order, the messages that reflect the content of data structure can arrive in any order. For example, one node may add an object
to the set and send that modified set to another node that removes that object and sends that message to a third node. If the first node
also sends a message to the third node (with the object in the set), the third node could receive conflicting information. But the CRDT
data structures always contain enough information to resolve the conflict.

As with the first demo there is a You Tube video that illustrates the problem and shows how it is resolved and illustrates the running
example: [https://www.youtube.com/watch?v=SqkJs0QDRdk](https://www.youtube.com/watch?v=SqkJs0QDRdk) .

Again, we will run three instances of the example, to illustrate the actions of the nodes in the cluster. 
The arguments to the application are identical to those in the
first example. The difference in this example is that each node will read "commands" the change the local state of its information. Which we 
will illustrate shortly. But first, lets get the three nodes running:

In the first terminal window:
```
cd incubator-gossip/gossip-examples
mvn exec:java -Dexec.mainClass=org.apache.gossip.examples.StandAloneNodeCrdtOrSet -Dexec.args="udp://localhost:10000 0 udp://localhost:10000 0"
```

In the second terminal window:
```
cd incubator-gossip/gossip-examples
mvn exec:java -Dexec.mainClass=org.apache.gossip.examples.StandAloneNodeCrdtOrSet -Dexec.args="udp://localhost:10001 1 udp://localhost:10000 0"
```

In the third terminal window:
```
cd incubator-gossip/gossip-examples
mvn exec:java -Dexec.mainClass=org.apache.gossip.examples.StandAloneNodeCrdtOrSet -Dexec.args="udp://localhost:10002 2 udp://localhost:10000 0"
```

Now, at any of the terminal windows, you can type a command from the following set of commands to change the data that is stored locally. 
Note, while you are typing, the terminal output continues, forcing you to type blind, but when you hit the return, to end
the input line, the input will be briefly displayed before scrolling off the screen.

When you type one of these commands, you can then watch the propagation of that data through the cluster. The commands are:
```
a string
r string
g number
```
* **a** is the 'add' command; it adds the string to the shared set - you should see the set displayed with the new value, 
    eventually, at all nodes.
* **r** is the 'remove' command; it removes the string from the set (if it exists in the set) - you should see the 
	value eventually leave the set at all nodes
* **g** is the "global increment" command; it assume that it's argument is a number and adds that number to an accumulator.
    Eventually, the accumulator at all nodes will settle to the same value.

The CRDT representations of these values assure that all nodes will reach the same end state for the resulting value regardless of the order
of arrival of information from other nodes in the cluster.

As an augmentation to the video, this [wikipedia article](https://en.wikipedia.org/wiki/Conflict-free_replicated_data_type),
describing various CRDT representations and their usefulness, as well as some information about interesting applications.


Running org.apache.gossip.examples.StandAloneDatacenterAndRack
--------------------------------------------------------------

This final example illustrates more fine grained control over the expected "responsiveness" of nodes in the cluster. 

Apache gossip is designed as a library intended to be embedded in applications which to take advantage of the gossip-protocol 
for peer-to-peer communications. The effectiveness of communications among nodes in a gossip cluster can be tuned
to the expected latency of message transmission and expected responsiveness of other nodes in the network. This example illustrates
one model of this type of control: a "data center and rack" model of node distribution. In this model, nodes that are in the same
'data center' (perhaps on a different 'rack') are assumed to  have very lower latency and high fidelity of communications. 
While, nodes in different data centers are assumed to require more time to communicate, and be subject to
a higher rate of communication failure, so communications can be tuned to tolerate more variation in latency and success of
transmission, but this result in a longer "settle" time.

Accordingly, the application in this example has a couple of extra arguments, a data center id, and a rack id. 

To start the first node (in the first terminal window), type the following:

```
cd incubator-gossip/gossip-examples
mvn exec:java -Dexec.mainClass=org.apache.gossip.examples.StandAloneDatacenterAndRack -Dexec.args="udp://localhost:10000 0 udp://localhost:10000 0 1 2"
```
The first four arguments are the same as in the other two examples, and the last two arguments are the new arguments:
1. The URI (host and port) for the node - **udp://localhost:10000**
2. The id for the node - **0**
3. The URI for a "seed" node - **udp://localhost:10000**
4. The id for that seed node - **0**
5. The data center id - **1**
6. The rack id - **2**

Lets then, set up two additional nodes (each in a separate terminal window), one in the same data center on a different rack,
and the other in a different data center. 
```
cd incubator-gossip/gossip-examples
mvn exec:java -Dexec.mainClass=org.apache.gossip.examples.StandAloneDatacenterAndRack -Dexec.args="udp://localhost:10001 1 udp://localhost:10000 0 1 3"
```

```
cd incubator-gossip/gossip-examples
mvn exec:java -Dexec.mainClass=org.apache.gossip.examples.StandAloneDatacenterAndRack -Dexec.args="udp://localhost:10002 2 udp://localhost:10000 0 2 2"
```

Now, the application running in the first terminal window, is identified as running in data center 1 and on rack 2;
the application in the second terminal window is running in the same data center in a different rack (data center 1, rack 3).
While, the application in the third terminal is running in a different data center (data center 2). 

If you stop the node in the first terminal window (control-c) you will observe that the process in the third terminal window
takes longer to settle to the correct state then the process in the second terminal window, because it is expecting
a greater latency in message transmission and is (therefore) more tolerant to delays (and drops) in messaging, taking it
longer to detect that the killed process is "off line".

Final Notes
-----------

That concludes the description of running the examples.

This project is an Apache [incubator project](http://incubator.apache.org/projects/gossip.html). 
The [official web site](http://gossip.incubator.apache.org/community/) has much additional information: 
see especially 'get involved' under 'community'. Enjoy, and please let us know if you are finding this library helpful in any way.

