count=1
while [ $count -le 60 ]; do
  pid=`ps -ef |grep FullNode.jar |grep -v grep |awk '{print $2}'`
  if [ -n "$pid" ]; then
    kill -15 $pid
    echo "kill -15 java-tron, counter $count"
    sleep 1
  else
    echo "java-tron killed"
    break
  fi
done

total=`cat /proc/meminfo  |grep MemTotal |awk -F ' ' '{print $2}'`
xmx=`echo "$total/1024/1024*0.8" | bc |awk -F. '{print $1"g"}'`
logtime=`date +%Y-%m-%d_%H-%M-%S`
nohup java -Xmx$xmx -jar build/libs/FullNode.jar -c config.conf  >> start.log 2>&1 &
pid=`ps -ef |grep FullNode.jar |grep -v grep |awk '{print $2}'`
echo "start java-tron with pid $pid on $HOSTNAME"