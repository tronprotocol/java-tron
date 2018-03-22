<h1 align="center">
  <br>
  <img width=20% src="https://raw.githubusercontent.com/tronprotocol/wiki/master/images/java-tron.png">
  <br>
  java-tron
  <br>
</h1>

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
[![Slack](https://img.shields.io/badge/chat-on%20slack-brightgreen.svg)](https://join.slack.com/t/tronfoundation/shared_invite/enQtMzAzNzg4NTI4NDM3LTAyZGQzMzEzMjNkNDU0ZjNkNTA4OTYyNTA5YWZmYjE3MTEyOWZhNzljNzQwODM3NDQ0OWRiMTIyMDhlYzgyOGQ)
[![Build Status](https://travis-ci.org/tronprotocol/java-tron.svg?branch=develop)](https://travis-ci.org/tronprotocol/java-tron)
[![GitHub issues](https://img.shields.io/github/issues/tronprotocol/java-tron.svg)](https://github.com/tronprotocol/java-tron/issues) 
[![GitHub pull requests](https://img.shields.io/github/issues-pr/tronprotocol/java-tron.svg)](https://github.com/tronprotocol/java-tron/pulls)
[![GitHub contributors](https://img.shields.io/github/contributors/tronprotocol/java-tron.svg)](https://github.com/tronprotocol/java-tron/graphs/contributors) 
[![license](https://img.shields.io/github/license/tronprotocol/java-tron.svg)](LICENSE)

# What's TRON?
TRON is a block chain-based decentralized smart protocol and an application development platform. It allows each user to freely publish, store and own contents and data, and in the decentralized autonomous form, decides an incentive mechanism and enables application developers and content creators through digital asset distribution, circulation and transaction, thus forming a decentralized content entertainment ecosystem.

TRON is a product of Web 4.0 and the decentralized internet of next generation.

Resources
===================

1. [TRON Website](https://tron.network/)
2. [Documentation](https://github.com/tronprotocol/java-tron)<br>(Comming soon. You are seeing part of it.)
3. [Blog](https://tronprotocol.github.io/tron-blog/)
4. [Community Telegram Group](https://t.me/tronnetworkEN)
5. [Slack Workspace](https://tronfoundation.slack.com/)
6. White Paper(Comming soon)
7. Roadmap(Comming soon)
8. [Tron Wiki](http://wiki.tron.network/en/latest/)<br>
9. [Tron Protocol](https://github.com/tronprotocol/protocol)<br>
10.[Wallet Client](https://github.com/tronprotocol/wallet-cli)<br>
11.[Wallet Web](https://github.com/tronprotocol/Wallet_Web)<br>
12.[Progress Report](http://192.168.1.188:8090/pages/viewpage.action?pageId=1310722)<br>

# Set up the environment

## Supported Operating System
Tron currently supports the following operating systems:  
1. Centos 7.  
2. Fedora 25 and higher (Fedora 27 recommended).  
3. Mint 18.  
4. Ubuntu 16.04 (Ubuntu 16.10 recommended).  
5. MacOS Darwin 10.12 and higher (MacOS 10.13.x recommended). 
# How to Build

## Getting the code
* Use Git from the Terminal, see the [Setting up Git](https://help.github.com/articles/set-up-git/) and [Fork a Repo](https://help.github.com/articles/fork-a-repo/) articles.
In the shell command, type:<br>
```bash
git clone https://github.com/tronprotocol/java-tron.git
```

* For Mac, you can also install **[GitHub for Mac](https://mac.github.com/)** then **[fork and clone our repository](https://guides.github.com/activities/forking/)**. 

* If you'd rather not use Git, use the `Download ZIP` button on the right to get the source directly.
## Prepare dependencies

## Building source code
* Build in the Terminal
```bash
cd java-tron
./gradlew build
```
The building will normally finish in less than one minute.

* Build in [IntelliJ IDEA](https://www.jetbrains.com/idea/) (community version is enough):
> a). Start IntelliJ Idea. Select `File` -> `Open`, then locate to the java-tron folder which you have git cloned to your local drive. Then click `Open` button on the right bottom.<br>

> b). Check on `Use auto-import` on the `Import Project from Gradle` dialog. Select JDK 1.8 in the `Gradle JVM` option. Then click `OK`.<br>

> c). IntelliJ will open the project and start gradle syncing, which will take several minutes, depending on your network connection and your IntelliJ configuration. 

> d). After the syncing finished, select `Gradle` -> `Tasks` -> `build`, and then double click `build` option.  The project will start building, which will normally take less than one minute to finish.

    git clone https://github.com/tronprotocol/java-tron.git
    ./gradlew build
    
# How To Run

## Running a private testnet

### Running a full node
* In the Terminal
```bash
./gradlew run
```

* In IntelliJ IDEA
> 1).  After the building finishes, locate `FullNode` in the project structure view panel, which is on the path `java-tron/src/main/java/org.tron/program/FullNode`.

> 2).  Select `FullNode`, right click on it, and select `Run 'FullNode.main()'`, then `FullNode` starts running.

### Running a witness node
* In the Terminal
```bash
./gradlew run -Pwitness
```

* In IntelliJ IDEA<br>

### Running multi-nodes

## Running a local node and connecting to the public testnet 

### Running a full node

### Running a witness node 


# Quick Start

Read the [Quick Start](http://wiki.tron.network/en/latest/quick_start.html).


# Commands
Read the [Commands](http://wiki.tron.network/en/latest/quick_start.html#commands).

# Contact

Chat with us via [Gitter](https://gitter.im/tronprotocol/java-tron).

# Contribution
Contributions are welcomed and greatly appreciated. Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details on submitting patches and the contribution workflow.

