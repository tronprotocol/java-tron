# 模块化介绍

## 模块化的初衷

基于以太坊上的CryptoKitties游戏高峰时期甚至占据了以太坊16%的流量，造成严重的网络拥堵；即便波场的性能约是以太坊的100倍，但依然存在极限，为了更好的进行水平扩展需要对 java-tron 进行模块化拆分，模块化后的 java-tron 可以让应用开发者能够轻易的研发并部署一条区块链，而不仅仅是研发部署一个链上的应用（One Dapp is One Chain)，这将会降低区块链基础设施开发的成本，而且模块化可以帮助开发者更有效的定制符合自身业务的模块，比如抽象后的共识模块可以帮助业务针对具体的场景来选择合适的共识机制。模块后的 java-tron 将区块链本身的底层实现细节对开发者屏蔽，让应用开发者更加专注于业务场景。

java-tron 模块化的目的是为了帮助开发者方便地构建出特定应用的区块链，一个应用即是一条链。有以下几点优势：

1. 代码层面上的模块化将使系统架构更清晰，代码更加易于维护扩展
2. 各个模块皆是独立组件，模块化后有利于组件产品化和提高产品成熟度
3. 面向接口的开发模式使模块更加解耦，实现模块可插拔，满足不同业务需求

## 模块化的 java-tron 架构介绍

![modular-structure](https://github.com/tronprotocol/java-tron/blob/develop/docs/images/module.png)

模块化后的 java-tron 目前分为6个模块：framework、protocol、common、chainbase、consensus、actuator，下面分别简单介绍一下各个模块的作用。

### framework

framework 是 java-tron 的核心模块，不仅是整个链的入口模块，同时也充当粘合剂的作用将其他模块有机地组织起来，framework 模块负责各个模块的初始化、流程的跳转。

### protocol

对于区块链这种分布式网络，简洁高效的数据交换协议尤为重要，protocol 模块定义了外界与 java-tron 交互的二进制协议格式，是 java-tron 实现跨语言跨平台的基础，该模块同时定义了：
1. java-tron 内部节点间的通信协议
2. java-tron 对外提供的服务协议

### common

common 模块对公共组件和一些工具类进行了封装，以方便其他模块调用。

### chainbase

chainbase 模块是数据库层面的抽象，像 PoW、PoS、DPoS 这类基于概率性的共识算法不可避免的会以一定的概率发生切链，因此 chainbase 定义了一个支持可回退数据库的接口标准，该接口要求数据库实现状态回滚机制、checkpoint容灾机制等。
另外 chainbase 模块具有良好的接口抽象设计，任何满足接口实现的数据库都可以作为区块链的底层存储，赋予开发者更多的灵活性，LevelDB和RocksDB是默认提供的两种具体实现。

下面简单的为大家介绍一下 chainbase 模块中重要的几个实现类和接口：
1. RevokingDatabase: 是数据库容器的接口，用于所有可回退数据库的管理，SnapshotManager 是该接口的一个实现
2. TronStoreWithRevoking: 支持可回退的数据库的基类，Chainbase 类是它的具体实现

### consensus

共识机制是区块链中非常重要的模块，常见的有 PoW、PoS、DPoS、PBFT 等，联盟链以及其他一些可信网络中也会采用 Paxos、Raft 等共识机制，共识的选择需要和业务场景相匹配，比如对共识效率敏感实时游戏类就不适合采用 PoW，而对实时性要求极高的交易所来说 PBFT 可能是首选。所以支持可替换的共识将是一个有非常有想象力的创造，同时也是实现特定应用区块链的重要一环，即便像 Cosmos SDK 这样的明星区块链项目依然也停留在应用层为开发者提供一些自主性，底层的共识依然受限于 Tendermint 共识。
consensus 模块最终目标是能够让应用开发者能够像配置参数那样简单的切换共识机制。

consensus 模块将共识过程抽象成几个重要的部分，定义在 ConsensusInterface 接口中：
1. start: 启动共识服务，可以自定制启动参数
2. stop: 停止共识服务
3. receiveBlock: 定义接收区块的共识逻辑
4. validBlock: 定义验证区块的共识逻辑
5. applyBlock: 定义处理区块的共识逻辑

应用开发者可以通过实现 ConsensusInterface 接口来定制共识，同时社区也正在探索 PBFT 和 DPoS 组合共识的方案，进一步降低区块校验延时来满足对实时性要求更高的场景，届时这个混合共识将成为可替换共识的具体案例。

### actuator

以太坊初创性的引入了虚拟机并定义了智能合约这种开发方式，但对于一些复杂的应用，智能合约不够灵活且受限于性能，这也是 java-tron 提供创建应用链的一个原因。为此 java-tron 独立出来了 actuator 模块，该模块为应用开发者提供一种新的开发范式：可以将应用代码直接植入链中而不再将应用代码跑在虚拟机中。
actuator 是交易的执行器，可以将应用看成是不同交易类型组成的交易集，每类交易都由对应的 actuator 负责执行。

actuator模块定义了 Actuator 接口，该接口有4个方法：
1. execute: 负责交易具体需要执行的动作，可以是状态修改、流程跳转、逻辑判断...
2. validate: 负责验证交易的正确性
3. getOwnerAddress: 获取交易发起方的地址
4. calcFee: 定义交易手续费计算逻辑

开发者可以根据自身业务实现 Actuator 接口，就能实现自定义交易类型的处理。
 