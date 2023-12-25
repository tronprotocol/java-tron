# How to quick start

## Introduction

This guide provides two ways for TRON quickstart:
- Set up a FullNode using the official tools: providing a wealth of configurable parameters to startup a FullNode
- Set up a complete private network for Tron development using a third-party tool: [docker-tron-quickstart](https://github.com/TRON-US/docker-tron-quickstart)

## Dependencies

### Docker

Please download and install the latest Docker from Docker official website:
* Docker Installation for [Mac](https://docs.docker.com/docker-for-mac/install/)
* Docker Installation for [Windows](https://docs.docker.com/docker-for-windows/install/)   

## Quickstart based on official tools

### Build the docker image from source

#### Clone the java-tron repo

Clone the java-tron repo from github and enter the directory `java-tron`:
```
git clone https://github.com/tronprotocol/java-tron.git
cd java-tron
```

#### Build the docker image

Use below command to start the build:
```
docker build -t tronprotocol/java-tron .
```

#### Using the official Docker images

Download the official docker image from the Dockerhub with below command if you'd like to use the official images:
```
docker pull tronprotocol/java-tron
```

### Run the container

You can run the command below to start the java-tron:
```
docker run -it -d -p 8090:8090 -p 8091:8091 -p 18888:18888 -p 50051:50051 --restart always tronprotocol/java-tron 
```

The `-p` flag defines the ports that the container needs to be mapped on the host machine. By default the container will start and join in the mainnet
using the built-in configuration file, you can specify other configuration file by mounting a directory and using the flag `-c`.
This image also supports customizing some startup parameters，here is an example for running a FullNode as an SR in production env:
```
docker run -it -d -p 8080:8080 -p 8090:8090 -p 18888:18888 -p 50051:50051 \
           -v /Users/quan/tron/docker/conf:/java-tron/conf \
           -v /Users/quan/tron/docker/datadir:/java-tron/data \
           tronprotocol/java-tron \
           -jvm "{-Xmx10g -Xms10g}" \
           -c /java-tron/conf/config-localtest.conf \
           -d /java-tron/data \
           -w 
```
Note: The directory `/Users/tron/docker/conf` must contain the file `config-localtest.conf`. The jvm parameters must be enclosed in double quotes and braces.

## Quickstart for using docker-tron-quickstart

The image exposes a Full Node, Solidity Node, and Event Server. Through TRON Quickstart, users can deploy DApps, smart contracts, and interact with the TronWeb library.
Check more information at [Quickstart:](https://github.com/TRON-US/docker-tron-quickstart)

### Node.JS Console
  Node.JS is used to interact with the Full and Solidity Nodes via Tron-Web.  
  [Node.JS](https://nodejs.org/en/) Console Download
  
### Clone TRON Quickstart  
```shell
git clone https://github.com/TRON-US/docker-tron-quickstart.git
```  

### Pull the image using docker:
```shell
docker pull trontools/quickstart
```  

## Setup TRON Quickstart   
### TRON Quickstart Run
Run the "docker run" command to launch TRON Quickstart. TRON Quickstart exposes port 9090 for Full Node, Solidity Node, and Event Server.
```shell
docker run -it \
  -p 9090:9090 \
  --rm \
  --name tron \
  trontools/quickstart
```  
Notice: the option --rm automatically removes the container after it exits. This is very important because the container cannot be restarted, it MUST be run from scratch to correctly configure the environment.

### Testing

If everything goes well, your terminal console output will look like following : 
 <details>

<summary>Run Console Output </summary>
<!-- **Run Output:** -->

    [PM2] Spawning PM2 daemon with pm2_home=/root/.pm2
    [PM2] PM2 Successfully daemonized
    [PM2][WARN] Applications eventron not running, starting...
    [PM2] App [eventron] launched (1 instances)
    ┌──────────┬────┬─────────┬──────┬─────┬────────┬─────────┬────────┬─────┬───────────┬──────┬──────────┐
    │ App name │ id │ version │ mode │ pid │ status │ restart │ uptime │ cpu │ mem       │ user │ watching │
    ├──────────┼────┼─────────┼──────┼─────┼────────┼─────────┼────────┼─────┼───────────┼──────┼──────────┤
    │ eventron │ 0  │ N/A     │ fork │ 60  │ online │ 0       │ 0s     │ 0%  │ 25.4 MB   │ root │ disabled │
    └──────────┴────┴─────────┴──────┴─────┴────────┴─────────┴────────┴─────┴───────────┴──────┴──────────┘
    Use `pm2 show <id|name>` to get more details about an app
    Start the http proxy for dApps...
    [HPM] Proxy created: /  ->  http://127.0.0.1:18191
    [HPM] Proxy created: /  ->  http://127.0.0.1:18190
    [HPM] Proxy created: /  ->  http://127.0.0.1:8060

    Tron Quickstart listening on http://127.0.0.1:9090



    ADMIN /admin/accounts-generation
    Sleeping for 1 second...Waiting when nodes are ready to generate 10 accounts...
    (1) Waiting for sync...
    Slept.
    ...
    Loading the accounts and waiting for the node to mine the transactions...
    (1) Waiting for receipts...
    Sending 10000 TRX to TSjfWSWcKCrJ1DbgMZSCbSqNK8DsEfqM9p
    Sending 10000 TRX to THpWnj3dBQ5FrqW1KMVXXYSbHPtcBKeUJY
    Sending 10000 TRX to TWFTHaKdeHWi3oPoaBokyZFfA7q1iiiAAb
    Sending 10000 TRX to TFDGQo6f6dm9ikoV4Rc9NyTxMD5NNiSFJD
    Sending 10000 TRX to TDZZNigWitFp5aE6j2j8YcycF7DVjtogBu
    Sending 10000 TRX to TT8NRMcwdS9P3X9pvPC8JWi3x2zjwxZuhs
    Sending 10000 TRX to TBBJw6Bk7w2NSZeqmzfUPnsn6CwDJAXTv8
    Sending 10000 TRX to TVcgSLpT97mvoiyv5ChyhQ6hWbjYLWdCVB
    Sending 10000 TRX to TYjQd4xrLZQGYMdLJqsTCuXVGapPqUp9ZX
    Sending 10000 TRX to THCw6hPZpFcLCWDcsZg3W77rXZ9rJQPncD
    Sleeping for 3 seconds... Slept.
    (2) Waiting for receipts...
    Sleeping for 3 seconds... Slept.
    (3) Waiting for receipts...
    Sleeping for 3 seconds... Slept.
    (4) Waiting for receipts...
    Sleeping for 3 seconds... Slept.
    (5) Waiting for receipts...
    Sleeping for 3 seconds... Slept.
    (6) Waiting for receipts...
    Sleeping for 3 seconds... Slept.
    (7) Waiting for receipts...
    Done.

    Available Accounts
    ==================

    (0) TSjfWSWcKCrJ1DbgMZSCbSqNK8DsEfqM9p (10000 TRX)
    (1) THpWnj3dBQ5FrqW1KMVXXYSbHPtcBKeUJY (10000 TRX)
    (2) TWFTHaKdeHWi3oPoaBokyZFfA7q1iiiAAb (10000 TRX)
    (3) TFDGQo6f6dm9ikoV4Rc9NyTxMD5NNiSFJD (10000 TRX)
    (4) TDZZNigWitFp5aE6j2j8YcycF7DVjtogBu (10000 TRX)
    (5) TT8NRMcwdS9P3X9pvPC8JWi3x2zjwxZuhs (10000 TRX)
    (6) TBBJw6Bk7w2NSZeqmzfUPnsn6CwDJAXTv8 (10000 TRX)
    (7) TVcgSLpT97mvoiyv5ChyhQ6hWbjYLWdCVB (10000 TRX)
    (8) TYjQd4xrLZQGYMdLJqsTCuXVGapPqUp9ZX (10000 TRX)
    (9) THCw6hPZpFcLCWDcsZg3W77rXZ9rJQPncD (10000 TRX)

</details>
  

### web browser ###
1. open your web browser
2. enter : http://127.0.0.1:9090/
3. there will be a response JSON data: 

```
 {"Welcome to":"TronGrid v2.2.8"}
```

## Docker Commands 
Here are some useful docker commands, which will help you manage the TRON Quickstart Docker container on your machine. 

**To list all active containers on your machine, run:**
```shell
docker container ps
```  
**Output:**
```shell
docker container ps

CONTAINER ID        IMAGE               COMMAND                 CREATED             STATUS              PORTS                                              NAMES
513078dc7816        tron                "./quickstart v2.0.0"   About an hour ago   Up About an hour    0.0.0.0:9090->9090/tcp, 0.0.0.0:18190->18190/tcp   tron
```  
**To kill an active container, run:**
```shell
docker container kill 513078dc7816   // use your container ID
```  

### How to check the logs of the FullNode ###
```
  docker exec -it tron tail -f /tron/FullNode/logs/tron.log 
```

 <details>

<summary>Output: something like following </summary>

  ```
  number=204
  parentId=00000000000000cb0985978b3c780e4219dc51e4329beecabe7b71f99d269985
  witness address=41928c9af0651632157ef27a2cf17ca72c575a4d21
  generated by myself=true
  generate time=2019-12-09 18:33:33.0
  txs are empty
  ]
  18:33:33.008 INFO  [Thread-5] [DB](Manager.java:1095) pushBlock block number:204, cost/txs:1/0
  18:33:33.008 INFO  [Thread-5] [witness](WitnessService.java:283) Produce block successfully, blockNumber:204, abSlot[525305471], blockId:00000000000000ccc37f1f5c2ceb574d14c490e3d0b86909855646f9384ba666, transactionSize:0, blockTime:2019-12-09T18:33:33.000Z, parentBlockId:00000000000000cb0985978b3c780e4219dc51e4329beecabe7b71f99d269985
  18:33:33.008 INFO  [Thread-5] [net](AdvService.java:156) Ready to broadcast block Num:204,ID:00000000000000ccc37f1f5c2ceb574d14c490e3d0b86909855646f9384ba666
  ........  etc
  ```
</details>
