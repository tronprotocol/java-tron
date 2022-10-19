#!/bin/bash

#一个或多个fullnode。若多个用分号(;)分割。必填
fullnodes="10.40.100.117:50051"
#同时向fullnode广播的最大线程数。必填
broadcastThreadNum="100"
#filePath="/data/java-tron-stress/data/2022-08-17_43180631/0_1000000.txt" //交易二进制文件的路径。必填
filePath="/data/stress/SendTx/trc20.txt" #交易二进制文件的路径。必填
onceSendTxNum="1000000" #单次读取并广播交易的行数。必填
maxRows="5000000" #读取文件的最大行数，后面数据被忽略。-1 或 不传此参数表示读取所有行。选填
##############################
type='sendTx'
startBlock=0
step=28800
endBlock=10000
options=''
outputPath=/data/stress/collection/transaction.txt

while [ -n "$1" ]; do
  case "$1" in
  --startBlock)
    options="$options -D$startBlock"
    type='collection'
    startBlock=$2
    shift 2
    ;;
  --endBlock)
    endBlock=$2
    options="$options -D$endBlock"
    shift 2
    ;;
  --path)
    outputPath=$2
    options="$options -D$outputPath"
    shift 2
    ;;
  --fullnode)
    fullnode=$2
    options="$options -D$fullnode"
    shift 2
    ;;
  *)
    echo "warn: option $1 does not exist"
    exit
    ;;
  esac
done


nohup java -Xms5G -Xmx5G -XX:ReservedCodeCacheSize=256m -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m -XX:MaxDirectMemorySize=1G -XX:+PrintGCDetails \
-XX:+PrintGCDateStamps -Xloggc:gc.log -XX:+UseConcMarkSweepGC -XX:NewRatio=2 -XX:+CMSScavengeBeforeRemark -XX:+ParallelRefProcEnabled -XX:+HeapDumpOnOutOfMemoryError \
-XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 -jar SendTx.jar $fullnodes $broadcastThreadNum $filePath $onceSendTxNum $maxRows >> sendtx.log 2>&1 &
