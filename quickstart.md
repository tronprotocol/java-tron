# How to quick start

## Introduction

This guide walks the user through the TRON Quickstart (v2.0.0) image setup.   
The image exposes a Full Node, Solidity Node, and Event Server. Through TRON Quickstart, the user can deploy DApps, smart contracts, and interact via the TronWeb library.

## Dependencies  

### Docker

Please refer to the Docker official website to download and install the latest Docker version:
* Docker Installation for Mac(https://docs.docker.com/docker-for-mac/install/)
* Docker Installation for Windows(https://docs.docker.com/docker-for-windows/install/)   

### Node.JS Console
  This will be used to interact with the Full and Solidity Nodes via Tron-Web.  
  Node.JS Console Download(https://nodejs.org/en/)
  
### Clone TRON Quickstart  
```shell
git clone https://github.com/tronprotocol/docker-tron-quickstart.git
```  

## Setup TRON Quickstart   
### TRON Quickstart Build
Run the docker run command to launch TRON Quickstart. TRON Quickstart exposes port 9090 for Full Node, Solidity Node, and Event Server.
```shell
docker run -it --rm -p 9090:9090 --name tron -e "defaultBalance=100000" -e "showQueryString=true" -e "showBody=true" -e "formatJson=true" tron
```  

**Run Output:**
```shell
Tron Quickstart v2.0.0


Start nodes and event server...
[PM2] Spawning PM2 daemon with pm2_home=/root/.pm2
[PM2] PM2 Successfully daemonized
[PM2][WARN] Applications eventron not running, starting...
[PM2] App [eventron] launched (1 instances)
┌──────────┬────┬─────────┬──────┬─────┬────────┬─────────┬────────┬─────┬───────────┬──────┬──────────┐
│ App name │ id │ version │ mode │ pid │ status │ restart │ uptime │ cpu │ mem       │ user │ watching │
├──────────┼────┼─────────┼──────┼─────┼────────┼─────────┼────────┼─────┼───────────┼──────┼──────────┤
│ eventron │ 0  │ N/A     │ fork │ 48  │ online │ 0       │ 0s     │ 0%  │ 24.8 MB   │ root │ disabled │
└──────────┴────┴─────────┴──────┴─────┴────────┴─────────┴────────┴─────┴───────────┴──────┴──────────┘
 Use `pm2 show <id|name>` to get more details about an app
Start the http proxy for dApps...
[HPM] Proxy created: /  ->  http://127.0.0.1:18191
[HPM] Proxy created: /  ->  http://127.0.0.1:18190
[HPM] Proxy created: /  ->  http://127.0.0.1:8060

 Tron Quickstart listening on http://127.0.0.1:9090 



ADMIN /admin/accounts-generation
Sleeping for 1 second... Slept.
Waiting when nodes are ready to generate 10 accounts...
(1) Waiting for sync...
Sleeping for 1 second... Slept.
Sleeping for 1 second... Slept.
Sleeping for 1 second... Slept.
Sleeping for 1 second... Slept.
Sleeping for 1 second... Slept.
(2) Waiting for sync...
Sleeping for 1 second... Slept.
Sleeping for 1 second... Slept.
Sleeping for 1 second... Slept.
Sleeping for 1 second... Slept.
Sleeping for 1 second... Slept.
(3) Waiting for sync...
Sleeping for 1 second... Slept.
Sleeping for 1 second... Slept.
Sleeping for 1 second... Slept.
Sleeping for 1 second... Slept.
Sleeping for 1 second... Slept.
(4) Waiting for sync...
Sleeping for 1 second... Slept.
Sleeping for 1 second... Slept.
Sleeping for 1 second... Slept.
Sleeping for 1 second... Slept.
(5) Waiting for sync...
...
Loading the accounts and waiting for the node to mine the transactions...
(1) Waiting for receipts...
Sending 100000 TRX to TU8E4BzdGg4adqTyRCEZrrkq2EXsUYHG2k
Sending 100000 TRX to TGBN88CQN74i89Gy25749SUZr1HcTufF6z
Sending 100000 TRX to TWtStDLxF7gAkYbrMgSAbKsNdXY2qMUSv5
Sending 100000 TRX to TTvYtVvHKpdg9Q2Sfhc4uSsrNbtJV8DiJS
Sending 100000 TRX to TTiJtZRijXeGDEQgQUK5eXcTchTt69siva
Sending 100000 TRX to TDT6oLuRmVkHp3npeztqQR1g63KxcugFK9
Sending 100000 TRX to TE8BVrC4MoqtFfsMJviERPn8vW1Uh8Mcdn
Sending 100000 TRX to TADf7t8afXBTaHqyYLqRqxyrsPvwykxndv
Sending 100000 TRX to TJ71yAB1Cq4Cw2TEq6Z6dwP6dJTB7vH75C
Sending 100000 TRX to TPF46hEmM3AYWz9D2ix3yVpdePvyjdmJXd
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
Done.

Available Accounts
==================

(0) TU8E4BzdGg4adqTyRCEZrrkq2EXsUYHG2k (100000 TRX)
(1) TGBN88CQN74i89Gy25749SUZr1HcTufF6z (100000 TRX)
(2) TWtStDLxF7gAkYbrMgSAbKsNdXY2qMUSv5 (100000 TRX)
(3) TTvYtVvHKpdg9Q2Sfhc4uSsrNbtJV8DiJS (100000 TRX)
(4) TTiJtZRijXeGDEQgQUK5eXcTchTt69siva (100000 TRX)
(5) TDT6oLuRmVkHp3npeztqQR1g63KxcugFK9 (100000 TRX)
(6) TE8BVrC4MoqtFfsMJviERPn8vW1Uh8Mcdn (100000 TRX)
(7) TADf7t8afXBTaHqyYLqRqxyrsPvwykxndv (100000 TRX)
(8) TJ71yAB1Cq4Cw2TEq6Z6dwP6dJTB7vH75C (100000 TRX)
(9) TPF46hEmM3AYWz9D2ix3yVpdePvyjdmJXd (100000 TRX)

Private Keys
==================

(0) be3179ecdde173172001922024e631f42dbedda4a897990d6f67a8f3075d4b4a
(1) 6983092e286ee240e13e404d828d4ff65eb048c06958b7c956fcc35d8dc72dfa
(2) 14afd09c60731007d728491529dc5e60d416dac0a41cc585fcbf7b24456216af
(3) 3858720883b55c215e8d6cf1c3a273cc1f7f2885bdbf9039908835bc9386c3d8
(4) 2ee3e1b2939b4369c603d53c10c8a0b0365438ab21a6f3bc0dba944a07c3e3b6
(5) 7fe368488e8e291b518733e577c9ba6086831fbd6cb6c15ad1d488641604949b
(6) 415f6afbe240e60d39cb813756d74a9fd596ea37ec188993738f1cc273285ce3
(7) c65f5bb0eb63d6894d56b78a3a07208446e6ed1395fc380d5d6aa90355aa8785
(8) 4c43f458d7866b80ba56a02ad664b3bcd393990efbabebaaca7aa154b4d08362
(9) 93363a1e9ead687aeac03ed40abe30fee72d990b578bf6d36ab90438561dd037

HD Wallet
==================
Mnemonic:      slice beach ensure roof mercy tired sail achieve payment flower suggest sad
Base HD Path:  m/44'/60'/0'/0/{account_index}

GET 200  - 41874.921 ms
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
