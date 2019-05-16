# How to quick start

## Introduction

This guide walks the user through the TRON Quickstart (v2.0.0) image setup.   
The image exposes a Full Node, Solidity Node, and Event Server. Through TRON Quickstart, the user can deploy DApps, smart contracts, and interact via the TronWeb library.

## Dependencies  

### Docker

Please refer to the Docker official website to download and install the latest Docker version:
* Docker Installation for [Mac](https://docs.docker.com/docker-for-mac/install/)
* Docker Installation for [Windows](https://docs.docker.com/docker-for-windows/install/)   

### Node.JS Console
  This will be used to interact with the Full and Solidity Nodes via Tron-Web.  
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
Run the docker run command to launch TRON Quickstart. TRON Quickstart exposes port 9090 for Full Node, Solidity Node, and Event Server.
```shell
docker run -it \
  -p 9090:9090 \
  --rm \
  --name tron \
  trontools/quickstart
```  

**Run Output:**
```shell
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

Private Keys
==================

(0) 2b2bddbeea87cecedcaf51eef55877b65725f709d2c0fcdfea0cb52d80acd52b
(1) f08759925316dc6344af538ebe3a619aeab836a0c254adca903cc764f87b0ee9
(2) 1afc9f033cf9c6058db366b78a9f1b9c909b1b83397c9aed795afa05e9017511
(3) f8f5bc70e91fc177eefea43b68c97b66536ac317a9300639e9d32a9db2f18a1f
(4) 031015272915917056c117d3cc2a03491a8f22ef450af83f6783efddf7064c59
(5) 5eb25e2c1144f216aa99bbe2139d84bb6dedfb2c1ed72f3df6684a4c6d2cd96b
(6) f0b781da23992e6a3f536cb60917c3eb6a9c5434fcf441fcb8d7c58c01d6b70e
(7) 158f60a4379688a77d4a420e2f2a3e014ebf9ed0a1a093d7dc01ba23ebc5c970
(8) e9342bb9108f46573804890a5301530c2834dce3703cd51ab77fba6161afec00
(9) 2e9f0c507d2ea98dc4005a1afb1b743c629f7c145ccb55f38f75ae73cf8f605c

HD Wallet
==================
Mnemonic:      border pulse twenty cruise grief shy need raw clean possible begin climb
Base HD Path:  m/44'/60'/0'/0/{account_index}
```
## Docker Commands
A few Docker commands are useful for managing the TRON Quickstart Docker container on your machine.   

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
