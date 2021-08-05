#!/bin/bash
APP=$1
MANIFEST_OPT=$2
ALL_OPT=$*
NEED_REBUILD=0

if [[ $1 == '--rewrite--manifest' ]] || [[ $1 == '--r' ]]  ; then
   APP=''
   NEED_REBUILD=1
 elif [[ $1 == '--rewrite--manifest' ]] || [[ $1 == '--r' ]]  ; then
   NEED_REBUILD=1
fi

rebuildManifest() {
 if [[ $NEED_REBUILD == 1 ]] ; then
   buildManifest
 fi
}


buildManifest() {
 ARCHIVE_JAR='ArchiveManifest.jar'
 java -jar $ARCHIVE_JAR $ALL_OPT
 ret=$?
 if [[ $ret == 0 ]] ; then
     echo 'rebuild manifest success'
 else
     echo 'rebuild manifest fail, log in logs/archive.log'
 fi
 return ret
}

checkMemory() {
 ALLOW_MEMORY=16000000
 ALLOW_MAX_MEMORY=32000000
 MAX_MATESPACE_SIZE=' -XX:MaxMetaspaceSize=512m '
 total=`cat /proc/meminfo  |grep MemTotal |awk -F ' ' '{print $2}'`
 # total < ALLOW_MEN
 if [ $total -lt $ALLOW_MEMORY ] ; then
    echo "Direct memory must be greater than $ALLOW_MEMORY!, current memory: $total!!"
    exit
 fi
 if [[ $total -gt $ALLOW_MEMORY ]] && [[ $total -lt $ALLOW_MAX_MEMORY ]] ; then
echo 1 $total
    MAX_NEW_SIZE=' -XX:NewSize=3072m -XX:MaxNewSize=3072m '
    MEM_OPT="$MAX_MATESPACE_SIZE $MAX_NEW_SIZE"

 elif [[ $total -gt $ALLOW_MEMORY ]] ; then
echo 2 $total
    NEW_RATIO=' -XX:NewSize=6144m -XX:MaxNewSize=6144m '
    MEM_OPT="$MAX_MATESPACE_SIZE $NEW_RATIO"
 fi
}

APP=${APP:-"FullNode"}
START_OPT=`echo ${@:2}`
JAR_NAME="$APP.jar"
MAX_STOP_TIME=60
MEM_OPT=''

checkpid() {
 pid=`ps -ef | grep $JAR_NAME |grep -v grep | awk '{print $2}'`
 return $pid
}

checkPath(){
  path='output-directory/database'
  flag=1
  for p in ${ALL_OPT}
  do
     if [[ $flag == 0 ]] ; then
      path=`echo $p`
      break
     fi
     if [[ $p == '-d' || $p == '--database-directory' ]] ; then
      path=''
      flag=0
     fi
  done

  if [[ -z "${path}" ]]; then
     echo '-d /path or --database-directory /path'
     return 1
  fi

  if [[ -d ${path} ]]; then
    return 0
  else
    echo $path 'not exist'
    return 1
  fi
}



stopService() {
  count=1
  while [ $count -le $MAX_STOP_TIME ]; do
    checkpid
    if [ $pid ]; then
       kill -15 $pid
       sleep 1
    else
       echo "java-tron stop"
       return
    fi
    count=$[$count+1]
    if [ $count -eq $MAX_STOP_TIME ]; then
      kill -9 $pid
      sleep 1
    fi
  done
}

startService() {
 echo `date` >> start.log
 total=`cat /proc/meminfo  |grep MemTotal |awk -F ' ' '{print $2}'`
 xmx=`echo "$total/1024/1024*0.6" | bc |awk -F. '{print $1"g"}'`
 directmem=`echo "$total/1024/1024*0.1" | bc |awk -F. '{print $1"g"}'`
 logtime=`date +%Y-%m-%d_%H-%M-%S`
 export LD_PRELOAD="/usr/lib64/libtcmalloc.so"
 nohup java -Xms$xmx -Xmx$xmx -XX:+UseConcMarkSweepGC -XX:+PrintGCDetails -Xloggc:./gc.log\
  -XX:+PrintGCDateStamps -XX:+CMSParallelRemarkEnabled -XX:ReservedCodeCacheSize=256m -XX:+UseCodeCacheFlushing\
  $MEM_OPT -XX:MaxDirectMemorySize=$directmem -XX:+HeapDumpOnOutOfMemoryError -jar $JAR_NAME $START_OPT -c config.conf  >> start.log 2>&1 &
 pid=`ps -ef |grep $JAR_NAME |grep -v grep |awk '{print $2}'`
 echo "start java-tron with pid $pid on $HOSTNAME"
}

stopService
checkPath
if [[ 0 ==  $? ]] ; then
 rebuildManifest
else
 exit -1
fi
sleep 5
checkMemory
startService
