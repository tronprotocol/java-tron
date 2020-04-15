# Modularization Introduction

## Motivation

An Ethereum-based game called CryptoKitties took up 16% of the platform's traffic at peak hours, making the network heavily congested. Though TRON network is 100 times more efficient than Ethereum, there's still a ceiling to it, so we have to expand the capacity horizontally by splitting java-tron into isolated modules.

The modularized java-tron will allow DApp developers to easily create and deploy their own blockchains rather than simply develop an App on the chain (One DApp is One Chain), thus cutting the cost to build blockchain infrastructure and helping developers customize modules to their own needs, for example, by allowing them to select a well-suited consensus mechanism within an abstract consensus module. By modularizing java-tron, developers no longer get hassled by the underlying implementation details of a blockchain, and thus can focus more on their business scenarios.

The aim of java-tron modularization is to enable developers to easily build a dedicated blockchain for an App. It has great advantages:

1. Modularized code is easy to maintain and expand and making the system architecture clearer.
2. With modularization, each module is an isolated component, making it easier to productize and perfect them.
3. An interface-oriented development decouples the modules further, making them pluggable to adapt to different business scenarios.

## Architecture of modularized java-tron

![modular-structure](https://github.com/tronprotocol/java-tron/blob/develop/docs/images/module.png)

A modularized java-tron consists of six modules: framework, protocol, common, chainbase, consensus and actuator. The function of each module is elaborated below.

### framework

As the core module of java-tron, framework performs as both a gateway to the blockchain and an adhesive that effectively connects all other modules. Framework initializes each module and facilitates communication between modules.

### protocol

A concise and efficient data transfer protocol is essential to a distributed network like blockchain. Protocol module defines the format of the binary protocol under which java-tron interacts with the outside world, allowing java-tron to interact with multiple platforms in diverse languages. The module also defines:

1. the communication protocol between java-tron nodes
2. protocol that java-tron provides to the public

### common

Common module encapsulates common components and tools for other modules to access.

### chainbase

Chainbase is a database module. For probabilistic consensus algorithms such as PoW, PoS and DPoS, situations of switching to a new chain, however unlikely, is inevitable. Because of this, chainbase defines an interface standard supporting databases that can roll back. This interface requires databases to have a state rollback mechanism, a checkpoint-based fault tolerant mechanism and so on.
In addition, chainbase module features a well-designed abstract interface. Any database that implements the interface can be used for underlying storage on the blockchain, granting more flexibility to developers. LevelDB and RocksDB are two default implementations.

Below are a few important implementation classes and interfaces in chainbase module:

1. RevokingDatabase is the interface of database container that manages all databases that can roll back. SnapshotManager is an implementation of the interface.
2. TronStoreWithRevoking is the base abstract class of databases that can roll back. Chainbase class is its implementation.

### consensus

Consensus mechanism is a crucial module in blockchains. Common mechanisms include PoW, PoS, DPoS and PBFT, while Paxos, Raft etc, are applied to consortium blockchains and other trusted networks. The consensus mechanism should match the business scenario. For instance, PoW is not suitable for real-time games that are sensitive to consensus efficiency, while PBFT can make an optimized choice for exchanges demanding high real-time capability. In this sense, replaceable consensus is a creative innovation and an essential link in building application-specific blockchains. Even star blockchain programs like Cosmos SDK is still at a stage where the application layer provides developers with limited autonomy and the consensus at the base level is subject to Tendermint. Therefore, the ultimate goal of the consensus module is making consensus switch as easy as configuring parameters for application developers.

The consensus module divides the consensus process into several important parts that are defined in `ConsensusInterface`:
1. start: start the consensus service with customizable startup parameters
2. stop: stop the consensus service
3. receiveBlock: define the consensus logic of receiving blocks
4. validBlock: define the consensus logic of validating blocks
5. applyBlock: define the consensus logic of processing blocks

Application developers can customize the consensus through ConsensusInterface. Meanwhile, the community is exploring a hybrid consensus mechanism of PBFT and DPoS that can reduce block verification latency to obtain greater real-time capabilities for scenarios with higher demands. This hybrid consensus will be a specific case of replaceable consensus.

### actuator

Ethereum was the first to introduce the virtual machine and defined the smart contract. However, smart contracts are constrained in terms of their functions and not flexible enough to accommodate the needs of complex applications. This is one of the reasons why java-tron support the creation of a chain of application. For the reasons mentioned, java-tron includes a separate module, Actuator, offering application developers a brand new way of development. They can choose to implant their application codes into a chain instead of running them on virtual machines. Actuator, therefore, is the executor of transactions, while applications can be viewed as a cluster of different types of transactions, each of which executed by a corresponding actuator.

Actuator module defines the `Actuator` interface, which includes 4 different methods:
1. execute: execute specific actions of transactions, such as state modification, communication between modules, logic execution, etc.
2. validate: validate authenticity of transactions.
3. getOwnerAddress: acquire the address of transaction initiators
4. calcFee: define the logic of calculating transaction fees

Depending on their businesses, developers may set up Actuator accordingly and customize the processing of different types of transactions.
 