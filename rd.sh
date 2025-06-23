export currentBlockNum=`curl -s -X POST  http://127.0.0.1:50090/wallet/getnowblock | jq .block_header.raw_data.number`
count=0
while [ -z "$currentBlockNum" ]
do
  sleep 10
  export currentBlockNum=`curl -s -X POST  http://127.0.0.1:50090/wallet/getnowblock | jq .block_header.raw_data.number`
  count=$[$count+1]
  if [ $count -eq 10 ];then
    echo "Try to start java-tron"
    cd /data/java-tron/
    sh start.sh
  fi
  if [ $count -eq 20 ];then
    break
  fi
done
currentBlockNum=$[$currentBlockNum-300]
export startNum=startNum_replace_by_deploy
cp /data/workspace/replay_workspace/templet_of_collect.sh /data/workspace/replay_workspace/collect_mainNet_flow.sh
sed -i "s/startNum_replace_by_deploy/$currentBlockNum/g" /data/workspace/replay_workspace/collect_mainNet_flow.sh


testnet=(
172.16.200.231
172.16.200.123
172.16.200.114
172.16.200.216
172.16.200.151
172.16.200.79
172.16.200.146
172.16.200.77
)

fullnodenet=(
172.16.200.79
172.16.200.146
172.16.200.77
)

echo "Start build java-tron"
cd /data/workspace/replay_workspace/server_workspace/
rm -rf java-tron
cp -r daily-build-java-tron/ java-tron
cd /data/workspace/replay_workspace/server_workspace/java-tron/
git pull
git pull
sed -i "s/for (int i = 1\; i < slot/\/\*for (int i = 1\; i < slot/g" /data/workspace/replay_workspace/server_workspace/java-tron/consensus/src/main/java/org/tron/consensus/dpos/StatisticManager.java
sed -i "s/consensusDelegate.applyBlock(true)/consensusDelegate.applyBlock(true)\*\//g" /data/workspace/replay_workspace/server_workspace/java-tron/consensus/src/main/java/org/tron/consensus/dpos/StatisticManager.java
sed -i "s/long headBlockTime = chainBaseManager.getHeadBlockTimeStamp()/\/\*long headBlockTime = chainBaseManager.getHeadBlockTimeStamp()/g" /data/workspace/replay_workspace/server_workspace/java-tron/framework/src/main/java/org/tron/core/db/Manager.java
sed -i "s/void validateDup(TransactionCapsule/\*\/\}void validateDup(TransactionCapsule/g" /data/workspace/replay_workspace/server_workspace/java-tron/framework/src/main/java/org/tron/core/db/Manager.java
sed -i "s/validateTapos(trxCap)/\/\/validateTapos(trxCap)/g" /data/workspace/replay_workspace/server_workspace/java-tron/framework/src/main/java/org/tron/core/db/Manager.java
sed -i "s/validateCommon(trxCap)/\/\/validateCommon(trxCap)/g" /data/workspace/replay_workspace/server_workspace/java-tron/framework/src/main/java/org/tron/core/db/Manager.java

sed -i 's/ApplicationFactory.create(context);/ApplicationFactory.create(context);saveNextMaintenanceTime(context);/g' /data/workspace/replay_workspace/server_workspace/java-tron/framework/src/main/java/org/tron/program/FullNode.java
#sed -i 's/shutdown(appT);/shutdown(appT);mockWitness(context);/g' /data/workspace/replay_workspace/server_workspace/java-tron/framework/src/main/java/org/tron/program/FullNode.java
sed -i 's/context.registerShutdownHook();/context.registerShutdownHook();mockWitness(context);/g' /data/workspace/replay_workspace/server_workspace/java-tron/framework/src/main/java/org/tron/program/FullNode.java
sed -i '$d' /data/workspace/replay_workspace/server_workspace/java-tron/framework/src/main/java/org/tron/program/FullNode.java
sed -i "2a `cat /data/workspace/replay_workspace/server_workspace/build_insert/FullNode_import | xargs`" /data/workspace/replay_workspace/server_workspace/java-tron/framework/src/main/java/org/tron/program/FullNode.java
cat /data/workspace/replay_workspace/server_workspace/build_insert/FullNode_insert >> /data/workspace/replay_workspace/server_workspace/java-tron/framework/src/main/java/org/tron/program/FullNode.java

sed -i "s/private volatile boolean needSyncFromPeer = true/private volatile boolean needSyncFromPeer = false/g" /data/workspace/replay_workspace/server_workspace/java-tron/framework/src/main/java/org/tron/core/net/peer/PeerConnection.java
sed -i "s/private volatile boolean needSyncFromUs = true/private volatile boolean needSyncFromUs = false/g" /data/workspace/replay_workspace/server_workspace/java-tron/framework/src/main/java/org/tron/core/net/peer/PeerConnection.java
sed -i 's/if (getExpiration() < nextSlotTime) {/if (false \&\& getExpiration() < nextSlotTime) {/g' /data/workspace/replay_workspace/server_workspace/java-tron/chainbase/src/main/java/org/tron/core/capsule/TransactionCapsule.java
#sed -i "s/public static final long FROZEN_PERIOD = 86_400_000L/public static final long FROZEN_PERIOD = 1_000L/g" /data/workspace/replay_workspace/server_workspace/java-tron/common/src/main/java/org/tron/core/config/Parameter.java
#sed -i "s/public static final long DELEGATE_PERIOD = 3 * 86_400_000L/public static final long DELEGATE_PERIOD = 1_000L/g" /data/workspace/replay_workspace/server_workspace/java-tron/common/src/main/java/org/tron/core/config/Parameter.java
./gradlew clean build -x test -x check
if [ $? = 1 ];then
./gradlew clean build -x test -x check
if [ $? = 1 ];then
slack "压力测试/流量回放环境代码编译失败，提前退出"
exit 1
fi
fi


rm -rf /data/workspace/replay_workspace/server_workspace/java-tron-1.0.0/

unzip -o -d /data/workspace/replay_workspace/server_workspace/ /data/workspace/replay_workspace/server_workspace/java-tron/build/distributions/java-tron-1.0.0.zip
sed -i '$a-XX:+HeapDumpOnOutOfMemoryError' /data/workspace/replay_workspace/server_workspace/java-tron-1.0.0/bin/java-tron.vmoptions
sed -i '$a-XX:HeapDumpPath=/data/databackup/java-tron/heapdump/' /data/workspace/replay_workspace/server_workspace/java-tron-1.0.0/bin/java-tron.vmoptions
sed -i '$a-Dcom.sun.management.jmxremote' /data/workspace/replay_workspace/server_workspace/java-tron-1.0.0/bin/java-tron.vmoptions
sed -i '$a-Dcom.sun.management.jmxremote.port=9996' /data/workspace/replay_workspace/server_workspace/java-tron-1.0.0/bin/java-tron.vmoptions
sed -i '$a-Dcom.sun.management.jmxremote.authenticate=false' /data/workspace/replay_workspace/server_workspace/java-tron-1.0.0/bin/java-tron.vmoptions
sed -i '$a-Dcom.sun.management.jmxremote.ssl=false' /data/workspace/replay_workspace/server_workspace/java-tron-1.0.0/bin/java-tron.vmoptions
cp /data/workspace/replay_workspace/server_workspace/java-tron.vmoptions_cms /data/workspace/replay_workspace/server_workspace/java-tron-1.0.0/bin/java-tron.vmoptions

cd /data/workspace/replay_workspace/server_workspace/


for i in ${testnet[@]}; do
  ssh -p 22008 java-tron@$i 'cd /data/databackup/java-tron && rm -rf java-tron-1.0.0'
  tar -c java-tron-1.0.0/ |pigz |ssh -p 22008 java-tron@$i "gzip -d|tar -xC /data/databackup/java-tron/"
  scp -P 22008  /data/workspace/replay_workspace/server_workspace/java-tron/build/libs/Toolkit.jar java-tron@$i:/data/databackup/java-tron/
  scp -P 22008  /data/workspace/replay_workspace/server_workspace/conf/config.conf_$i java-tron@$i:/data/databackup/java-tron/config.conf
  scp -P 22008  /data/workspace/replay_workspace/server_workspace/stop_new.sh java-tron@$i:/data/databackup/java-tron/stop.sh
  scp -P 22008  /data/workspace/replay_workspace/server_workspace/start_new_witness.sh java-tron@$i:/data/databackup/java-tron/start.sh
  echo "Send java-tron.jar and config.conf and start.sh to ${i} completed"
done
scp -P 22008  /data/workspace/replay_workspace/server_workspace/stop_15.sh java-tron@172.16.200.79:/data/databackup/java-tron/stop.sh
cd /data/workspace/replay_workspace/server_workspace/java-tron/

for i in ${fullnodenet[@]}; do
  scp -P 22008 /data/workspace/replay_workspace/server_workspace/start_new_fullnode.sh java-tron@$i:/data/databackup/java-tron/start.sh
done

for i in ${testnet[@]}; do
  ssh -p 22008 java-tron@$i 'source ~/.bash_profile && cd /data/databackup/java-tron && sh /data/databackup/java-tron/stop.sh'
  echo "Stop java-tron on ${i} completed"
done
backup_logname="`date +%Y%m%d%H%M%S`_backup.log"
for i in ${testnet[@]}; do
  ssh -p 22008 java-tron@$i "mv /data/databackup/java-tron/logs/tron.log /data/databackup/java-tron/logs/$backup_logname"
  echo "Backup tron.log of ${i} complete"
done

cd /data/java-tron/
sh /data/java-tron/stop.sh
sleep 10
#rm -rf liteDatabase/*
#java -jar LiteFullNodeTool.jar -o split -t snapshot --fn-data-path output-directory/database --dataset-path liteDatabase/
#mkdir liteDatabase/output-directory
#mv liteDatabase/snapshot/ liteDatabase/output-directory/
#mv liteDatabase/output-directory/snapshot/ liteDatabase/output-directory/database

for i in ${testnet[@]}; do
  ssh -p 22008 java-tron@$i 'sudo rm -rf /data/databackup/java-tron/output-directory/'
  echo "Delete database file of ${i} completed"
  ssh -p 22008 java-tron@$i 'sudo rm -rf /data/databackup/java-tron/bak*'
done

cd /data/java-tron/liteDatabase
rm -rf output-directory/database/account-trace/ output-directory/database/balance-trace/
for node in ${testnet[@]}; do {
tar -c output-directory/ |pigz |ssh -p 22008 java-tron@$node "gzip -d|tar -xC /data/databackup/java-tron/" > /data/workspace/replay_workspace/server_workspace/${node}DBsend.log
} &
done
wait

sleep 10
cd /data/java-tron/
sh /data/java-tron/start.sh

for i in ${testnet[@]}; do
  ssh -p 22008 java-tron@$i 'cd /data/databackup/java-tron && rm -rf backup-output-directory'
  ssh -p 22008 java-tron@$i 'cd /data/databackup/java-tron && java -jar Toolkit.jar db cp output-directory/database/ backup-output-directory/database/'
done


for   k in $(seq 10); do
currentminute=`date +%M | sed -r 's/^0+//'`
if [ x"$currentminute" = x"" ] ;then
        break;
fi;
remainder=$(($currentminute % 5))
echo $remainder
if [ $remainder = 0 ] || [ $remainder = 1 ]; then
break
else
       echo $currentminute
       sleep 20;
fi;
done

for i in ${testnet[@]}; do
  ssh -p 22008 java-tron@$i 'source ~/.bash_profile && cd /data/databackup/java-tron && sh /data/databackup/java-tron/start.sh'
  ssh -p 22008 java-tron@$i 'find /data/databackup/java-tron/logs/ -mtime +10 -name "*" -exec rm -rf {} \;'
  echo "Start java-tron on ${i} completed"
done


sleep 200
queryClient=172.16.200.79
ssh -p 22008 java-tron@$queryClient 'source ~/.bash_profile && cd /data/databackup/mainnetApiQueryTask && sh kill_query_task.sh '
sleep 1
ssh -p 22008 java-tron@$queryClient "/data/databackup/mainnetApiQueryTask/do_mainnet_query_task.sh > /dev/null 2>&1 &"

sh /data/workspace/replay_workspace/server_workspace/send_deploy_result.sh

#cd /data/workspace/compression_workspace/
#sh deploy_stress_precondition.sh
