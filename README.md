<h1 align="center">
  <br>
  <img width=20% src="https://raw.githubusercontent.com/tronprotocol/wiki/master/images/java-tron.png">
  <br>
  java-tron
  <br>
</h1>

<h4 align="center">
  Java implementation of the <a href="https://tron.network">Tron Protocol</a>
</h4>


<p align="center">
  <a href="https://join.slack.com/t/tronfoundation/shared_invite/enQtMzAzNzg4NTI4NDM3LTAyZGQzMzEzMjNkNDU0ZjNkNTA4OTYyNTA5YWZmYjE3MTEyOWZhNzljNzQwODM3NDQ0OWRiMTIyMDhlYzgyOGQ">
    <img src="https://img.shields.io/badge/chat-on%20slack-brightgreen.svg">
  </a>
    
  <a href="https://travis-ci.org/tronprotocol/java-tron">
    <img src="https://travis-ci.org/tronprotocol/java-tron.svg?branch=develop">
  </a>
  
  <a href="https://github.com/tronprotocol/java-tron/issues">
    <img src="https://img.shields.io/github/issues/tronprotocol/java-tron.svg">
  </a>
  
  <a href="https://github.com/tronprotocol/java-tron/pulls">
    <img src="https://img.shields.io/github/issues-pr/tronprotocol/java-tron.svg">
  </a>
  
  <a href="https://github.com/tronprotocol/java-tron/graphs/contributors"> 
    <img src="https://img.shields.io/github/contributors/tronprotocol/java-tron.svg">
  </a>
  
  <a href="LICENSE">
    <img src="https://img.shields.io/github/license/tronprotocol/java-tron.svg">
  </a>
</p>

<p align="center">
  <a href="#how-to-build">How to Build</a> •
  <a href="#running">How to Run</a> •
  <a href="#links">Links</a> •
  <a href="http://wiki.tron.network">Wiki</a> •
  <a href="CONTRIBUTING.md">Contributing</a> •
  <a href="#community">Community</a>
</p>

## What's TRON?

TRON is a project dedicated to building the infrastructure for a truly decentralized Internet.

The Tron Protocol, one of the largest blockchain based operating systems in the world, offers scalable, high-availability and high-throughput support that underlies all the decentralized applications in the TRON ecosystem. 

TRON enables large-scale development and engagement. With over 2000 TPS, high concurrency, low latency and massive data transmission, TRON is ideal for building decentralized entertainment applications. Free features and incentive systems allow developers to create premium app experiences for users.

TRON Protocol and the TVM allow anyone to develop DAPPs for themselves or their communities, with smart contracts making decentralized crowdfunding and token issuance easier than ever.

# How to Build

## Getting the code

* Use Git from the Terminal, see the [Setting up Git](https://help.github.com/articles/set-up-git/) and [Fork a Repo](https://help.github.com/articles/fork-a-repo/) articles.
In the shell command, type:

```bash
git clone https://github.com/tronprotocol/java-tron.git
```

* For Mac, you can also install **[GitHub for Mac](https://mac.github.com/)** then **[fork and clone our repository](https://guides.github.com/activities/forking/)**. 

* If you'd rather not use Git, [Download the ZIP](https://github.com/tronprotocol/java-tron/archive/develop.zip)

## Prepare dependencies

* JDK 1.8 (JDK 1.9+ are not supported yet)
* On Linux Ubuntu system (e.g. Ubuntu 16.04.4 LTS), ensure that the machine has [__Oracle JDK 8__](https://www.digitalocean.com/community/tutorials/how-to-install-java-with-apt-get-on-ubuntu-16-04), instead of having __Open JDK 8__ in the system. If you are building the source code by using __Open JDK 8__, you will get [__Build Failed__](https://github.com/tronprotocol/java-tron/issues/337) result.

## Building from source code

* Build in the Terminal

```bash
cd java-tron
./gradlew build
```

* Build an executable JAE

```bash
./gradlew clean shadowJar
```

* Build in [IntelliJ IDEA](https://www.jetbrains.com/idea/) (community version is enough):

  1. Start IntelliJ. Select `File` -> `Open`, then locate to the java-tron folder which you have git cloned to your local drive. Then click `Open` button on the right bottom.
  2. Check on `Use auto-import` on the `Import Project from Gradle` dialog. Select JDK 1.8 in the `Gradle JVM` option. Then click `OK`.
  3. IntelliJ will open the project and start gradle syncing, which will take several minutes, depending on your network connection and your IntelliJ configuration
  4. After the syncing finished, select `Gradle` -> `Tasks` -> `build`, and then double click `build` option.
    
# Running

## Running a Private Testnet

### Running a full node

* In the Terminal

```bash
./gradlew run
```

* Use the executable JAE

```bash
cd build/libs 
java -jar java-tron.jar 
```

* In IntelliJ IDEA
  1. After the building finishes, locate `FullNode` in the project structure view panel, which is on the path `java-tron/src/main/java/org.tron/program/FullNode`.
  2. Select `FullNode`, right click on it, and select `Run 'FullNode.main()'`, then `FullNode` starts running.

### Running a Witness Node

* In the Terminal

```bash
./gradlew run -Pwitness=true
```
  
<details>
<summary>Show Output</summary>

```bash
> ./gradlew run -Pwitness=true

> Task :generateProto UP-TO-DATE
Using TaskInputs.file() with something that doesn't resolve to a File object has been deprecated and is scheduled to be removed in Gradle 5.0. Use TaskInputs.files() instead.

> Task :run 
20:39:22.749 INFO [o.t.c.c.a.Args] private.key = 63e62a71ed39e30bac7223097a173924aad5855959de517ff2987b0e0ec89f1a
20:39:22.816 WARN [o.t.c.c.a.Args] localwitness size must be one, get the first one
20:39:22.832 INFO [o.t.p.FullNode] Here is the help message.output-directory/
三月 22, 2018 8:39:23 下午 org.tron.core.services.RpcApiService start
信息: Server started, listening on 50051
20:39:23.706 INFO [o.t.c.o.n.GossipLocalNode] listener message
20:39:23.712 INFO [o.t.c.o.n.GossipLocalNode] sync group = a41d27f10194c53703be90c6f8735bb66ffc53aa10ea9024d92dbe7324b1aee3
20:39:23.716 INFO [o.t.c.s.WitnessService] Sleep : 1296 ms,next time:2018-03-22T20:39:25.000+08:00
20:39:23.734 WARN [i.s.t.BootstrapFactory] Env doesn't support epoll transport
20:39:23.746 INFO [i.s.t.TransportImpl] Bound to: 192.168.10.163:7080
20:39:23.803 INFO [o.t.c.n.n.NodeImpl] other peer is nil, please wait ... 
20:39:25.019 WARN [o.t.c.d.Manager] nextFirstSlotTime:[2018-03-22T17:57:20.001+08:00],now[2018-03-22T20:39:25.067+08:00]
20:39:25.019 INFO [o.t.c.s.WitnessService] ScheduledWitness[448d53b2df0cd78158f6f0aecdf60c1c10b15413],slot[1946]
20:39:25.021 INFO [o.t.c.s.WitnessService] It's not my turn
20:39:25.021 INFO [o.t.c.s.WitnessService] Sleep : 4979 ms,next time:2018-03-22T20:39:30.000+08:00
20:39:30.003 WARN [o.t.c.d.Manager] nextFirstSlotTime:[2018-03-22T17:57:20.001+08:00],now[2018-03-22T20:39:30.052+08:00]
20:39:30.003 INFO [o.t.c.s.WitnessService] ScheduledWitness[6c22c1af7bfbb2b0e07148ecba27b56f81a54fcf],slot[1947]
20:39:30.003 INFO [o.t.c.s.WitnessService] It's not my turn
20:39:30.003 INFO [o.t.c.s.WitnessService] Sleep : 4997 ms,next time:2018-03-22T20:39:35.000+08:00
20:39:33.803 INFO [o.t.c.n.n.NodeImpl] other peer is nil, please wait ... 
20:39:35.005 WARN [o.t.c.d.Manager] nextFirstSlotTime:[2018-03-22T17:57:20.001+08:00],now[2018-03-22T20:39:35.054+08:00]
20:39:35.005 INFO [o.t.c.s.WitnessService] ScheduledWitness[48e447ec869216de76cfeeadf0db37a3d1c8246d],slot[1948]
20:39:35.005 INFO [o.t.c.s.WitnessService] It's not my turn
20:39:35.005 INFO [o.t.c.s.WitnessService] Sleep : 4995 ms,next time:2018-03-22T20:39:40.000+08:00
20:39:40.005 WARN [o.t.c.d.Manager] nextFirstSlotTime:[2018-03-22T17:57:20.001+08:00],now[2018-03-22T20:39:40.055+08:00]
20:39:40.010 INFO [o.t.c.d.Manager] postponedTrxCount[0],TrxLeft[0]
20:39:40.022 INFO [o.t.c.d.DynamicPropertiesStore] update latest block header id = fd30a16160715f3ca1a5bcad18e81991cd6f47265a71815bd2c943129b258cd2
20:39:40.022 INFO [o.t.c.d.TronStoreWithRevoking] Address is [108, 97, 116, 101, 115, 116, 95, 98, 108, 111, 99, 107, 95, 104, 101, 97, 100, 101, 114, 95, 104, 97, 115, 104], BytesCapsule is org.tron.core.capsule.BytesCapsule@2ce0e954
20:39:40.023 INFO [o.t.c.d.DynamicPropertiesStore] update latest block header number = 140
20:39:40.024 INFO [o.t.c.d.TronStoreWithRevoking] Address is [108, 97, 116, 101, 115, 116, 95, 98, 108, 111, 99, 107, 95, 104, 101, 97, 100, 101, 114, 95, 110, 117, 109, 98, 101, 114], BytesCapsule is org.tron.core.capsule.BytesCapsule@83924ab
20:39:40.024 INFO [o.t.c.d.DynamicPropertiesStore] update latest block header timestamp = 1521722380001
20:39:40.024 INFO [o.t.c.d.TronStoreWithRevoking] Address is [108, 97, 116, 101, 115, 116, 95, 98, 108, 111, 99, 107, 95, 104, 101, 97, 100, 101, 114, 95, 116, 105, 109, 101, 115, 116, 97, 109, 112], BytesCapsule is org.tron.core.capsule.BytesCapsule@ca6a6f8
20:39:40.024 INFO [o.t.c.d.Manager] updateWitnessSchedule number:140,HeadBlockTimeStamp:1521722380001
20:39:40.025 WARN [o.t.c.u.RandomGenerator] index[-3] is out of range[0,3],skip
20:39:40.070 INFO [o.t.c.d.TronStoreWithRevoking] Address is [73, 72, -62, -24, -89, 86, -39, 67, 112, 55, -36, -40, -57, -32, -57, 61, 86, 12, -93, -115], AccountCapsule is account_name: "Sun"
address: "IH\302\350\247V\331Cp7\334\330\307\340\307=V\f\243\215"
balance: 9223372036854775387

20:39:40.081 INFO [o.t.c.d.TronStoreWithRevoking] Address is [41, -97, 61, -72, 10, 36, -78, 10, 37, 75, -119, -50, 99, -99, 89, 19, 47, 21, 127, 19], AccountCapsule is type: AssetIssue
address: ")\237=\270\n$\262\n%K\211\316c\235Y\023/\025\177\023"
balance: 420

20:39:40.082 INFO [o.t.c.d.TronStoreWithRevoking] Address is [76, 65, 84, 69, 83, 84, 95, 83, 79, 76, 73, 68, 73, 70, 73, 69, 68, 95, 66, 76, 79, 67, 75, 95, 78, 85, 77], BytesCapsule is org.tron.core.capsule.BytesCapsule@ec1439
20:39:40.083 INFO [o.t.c.d.Manager] there is account List size is 8
20:39:40.084 INFO [o.t.c.d.Manager] there is account ,account address is 448d53b2df0cd78158f6f0aecdf60c1c10b15413
20:39:40.084 INFO [o.t.c.d.Manager] there is account ,account address is 548794500882809695a8a687866e76d4271a146a
20:39:40.084 INFO [o.t.c.d.Manager] there is account ,account address is 48e447ec869216de76cfeeadf0db37a3d1c8246d
20:39:40.084 INFO [o.t.c.d.Manager] there is account ,account address is 55ddae14564f82d5b94c7a131b5fcfd31ad6515a
20:39:40.085 INFO [o.t.c.d.Manager] there is account ,account address is 6c22c1af7bfbb2b0e07148ecba27b56f81a54fcf
20:39:40.085 INFO [o.t.c.d.Manager] there is account ,account address is 299f3db80a24b20a254b89ce639d59132f157f13
20:39:40.085 INFO [o.t.c.d.Manager] there is account ,account address is abd4b9367799eaa3197fecb144eb71de1e049150
20:39:40.085 INFO [o.t.c.d.Manager] there is account ,account address is 4948c2e8a756d9437037dcd8c7e0c73d560ca38d
20:39:40.085 INFO [o.t.c.d.TronStoreWithRevoking] Address is [108, 34, -63, -81, 123, -5, -78, -80, -32, 113, 72, -20, -70, 39, -75, 111, -127, -91, 79, -49], WitnessCapsule is org.tron.core.capsule.WitnessCapsule@4cb4f7fb
20:39:40.086 INFO [o.t.c.d.TronStoreWithRevoking] Address is [41, -97, 61, -72, 10, 36, -78, 10, 37, 75, -119, -50, 99, -99, 89, 19, 47, 21, 127, 19], WitnessCapsule is org.tron.core.capsule.WitnessCapsule@7be2474a
20:39:40.086 INFO [o.t.c.d.TronStoreWithRevoking] Address is [72, -28, 71, -20, -122, -110, 22, -34, 118, -49, -18, -83, -16, -37, 55, -93, -47, -56, 36, 109], WitnessCapsule is org.tron.core.capsule.WitnessCapsule@3e375891
20:39:40.086 INFO [o.t.c.d.TronStoreWithRevoking] Address is [68, -115, 83, -78, -33, 12, -41, -127, 88, -10, -16, -82, -51, -10, 12, 28, 16, -79, 84, 19], WitnessCapsule is org.tron.core.capsule.WitnessCapsule@55d77b83
20:39:40.090 INFO [o.t.c.d.Manager] countWitnessMap size is 0
20:39:40.091 INFO [o.t.c.d.TronStoreWithRevoking] Address is [41, -97, 61, -72, 10, 36, -78, 10, 37, 75, -119, -50, 99, -99, 89, 19, 47, 21, 127, 19], WitnessCapsule is org.tron.core.capsule.WitnessCapsule@310dd876
20:39:40.092 INFO [o.t.c.d.TronStoreWithRevoking] Address is [72, -28, 71, -20, -122, -110, 22, -34, 118, -49, -18, -83, -16, -37, 55, -93, -47, -56, 36, 109], WitnessCapsule is org.tron.core.capsule.WitnessCapsule@151b42bc
20:39:40.092 INFO [o.t.c.d.TronStoreWithRevoking] Address is [108, 34, -63, -81, 123, -5, -78, -80, -32, 113, 72, -20, -70, 39, -75, 111, -127, -91, 79, -49], WitnessCapsule is org.tron.core.capsule.WitnessCapsule@2d0388aa
20:39:40.092 INFO [o.t.c.d.TronStoreWithRevoking] Address is [68, -115, 83, -78, -33, 12, -41, -127, 88, -10, -16, -82, -51, -10, 12, 28, 16, -79, 84, 19], WitnessCapsule is org.tron.core.capsule.WitnessCapsule@478a55e7
20:39:40.101 INFO [o.t.c.d.TronStoreWithRevoking] Address is [-3, 48, -95, 97, 96, 113, 95, 60, -95, -91, -68, -83, 24, -24, 25, -111, -51, 111, 71, 38, 90, 113, -127, 91, -46, -55, 67, 18, -101, 37, -116, -46], BlockCapsule is BlockCapsule{blockId=fd30a16160715f3ca1a5bcad18e81991cd6f47265a71815bd2c943129b258cd2, num=140, parentId=dadeff07c32d342b941cfa97ba82870958615e7ae73fffeaf3c6a334d81fe3bd, generatedByMyself=true}
20:39:40.102 INFO [o.t.c.d.Manager] save block: BlockCapsule{blockId=fd30a16160715f3ca1a5bcad18e81991cd6f47265a71815bd2c943129b258cd2, num=140, parentId=dadeff07c32d342b941cfa97ba82870958615e7ae73fffeaf3c6a334d81fe3bd, generatedByMyself=true}
20:39:40.102 INFO [o.t.c.s.WitnessService] Block is generated successfully, Its Id is fd30a16160715f3ca1a5bcad18e81991cd6f47265a71815bd2c943129b258cd2,number140 
20:39:40.102 INFO [o.t.c.n.n.NodeImpl] Ready to broadcast a block, Its hash is fd30a16160715f3ca1a5bcad18e81991cd6f47265a71815bd2c943129b258cd2
20:39:40.107 INFO [o.t.c.s.WitnessService] Produced
20:39:40.107 INFO [o.t.c.s.WitnessService] Sleep : 4893 ms,next time:2018-03-22T20:39:45.000+08:00
20:39:43.805 INFO [o.t.c.n.n.NodeImpl] other peer is nil, please wait ... 
20:39:45.002 WARN [o.t.c.d.Manager] nextFirstSlotTime:[2018-03-22T20:39:45.001+08:00],now[2018-03-22T20:39:45.052+08:00]
20:39:45.003 INFO [o.t.c.s.WitnessService] ScheduledWitness[48e447ec869216de76cfeeadf0db37a3d1c8246d],slot[1]
20:39:45.003 INFO [o.t.c.s.WitnessService] It's not my turn
20:39:45.003 INFO [o.t.c.s.WitnessService] Sleep : 4997 ms,next time:2018-03-22T20:39:50.000+08:00
20:39:50.002 WARN [o.t.c.d.Manager] nextFirstSlotTime:[2018-03-22T20:39:45.001+08:00],now[2018-03-22T20:39:50.052+08:00]
20:39:50.003 INFO [o.t.c.s.WitnessService] ScheduledWitness[6c22c1af7bfbb2b0e07148ecba27b56f81a54fcf],slot[2]
20:39:50.003 INFO [o.t.c.s.WitnessService] It's not my turn
20:39:50.003 INFO [o.t.c.s.WitnessService] Sleep : 4997 ms,next time:2018-03-22T20:39:55.000+08:00

```

</details>

* Use the executable JAE

```bash
cd build/libs 
java -jar java-tron.jar --witness true 
```

* In IntelliJ IDEA
  
<details>
<summary>

Open the configuration panel:

</summary>

![](docs/images/program_configure.png)

</details>  

<details>
<summary>

In the `Program arguments` option, fill in `--witness`:

</summary>

![](docs/images/set_witness_param.jpeg)

</details> 
  
Then, run `FullNode::main()` again.

### Running multi-nodes

To run TRON on more than one node, you need to specify several seed nodes' IPs in `config.conf` in `seed.node.ip.list`:
For private testnet, the IPs are allocated by yourself.

## Running a local node and connecting to the public testnet 

### Running a Full Node

It is almost the same as that does in the private testnet, except that the IPs in the `config.conf` are officially declared by TRON.

### Running a Witness Node 

It is almost the same as that does in the private testnet, except that the IPs in the `config.conf` are officially declared by TRON.
  
# Quick Start

Read the [Quick Start](http://wiki.tron.network/en/latest/quick_start.html).

# Community

* [Slack](https://join.slack.com/t/tronfoundation/shared_invite/enQtMzAzNzg4NTI4NDM3LTAyZGQzMzEzMjNkNDU0ZjNkNTA4OTYyNTA5YWZmYjE3MTEyOWZhNzljNzQwODM3NDQ0OWRiMTIyMDhlYzgyOGQ)
* [Telegram](https://t.me/tronnetworkEN)

# Links

* [Website](https://tron.network/)
* [Documentation](https://github.com/tronprotocol/java-tron)
* [Blog](https://tronprotocol.github.io/tron-blog/)
* [TRON Wiki](http://wiki.tron.network/en/latest/)

# Projects

* [TRON Protocol](https://github.com/tronprotocol/protocol)
* [Wallet Client](https://github.com/tronprotocol/wallet-cli)
* [Wallet Web](https://github.com/tronprotocol/Wallet_Web)
