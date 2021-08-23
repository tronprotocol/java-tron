#!/bin/bash
APP=$1
ALL_OPT=$*
NEED_REBUILD=0
ALL_ARR_OPT=($@)
ALL_OPT_NUM=$#

if [[ $1 == '--rewrite--manifest' ]] || [[ $1 == '--r' ]]  ; then
   APP=''
   NEED_REBUILD=1
 elif [[ $2 == '--rewrite--manifest' ]] || [[ $2 == '--r' ]]  ; then
   NEED_REBUILD=1
fi

rebuildManifest() {
 if [[ $NEED_REBUILD == 1 ]] ; then
   buildManifest
 fi
}

buildManifest() {
 ARCHIVE_JAR='ArchiveManifest.jar'
 if [[ -f $archive_jar ]] ; then
  java -jar $archive_jar $all_opt
 else
  echo 'download archivemanifest.jar'
  download=`wget https://github.com/tronprotocol/java-tron/releases/download/greatvoyage-v4.3.0/archivemanifest.jar`
  if [[ $download == 0 ]] ; then
   echo 'download success, rebuild manifest'
   java -jar $archive_jar $all_opt
  fi
 fi

 ret=$?
 if [[ $ret == 0 ]] ; then
     echo 'rebuild manifest success'
 else
     echo 'rebuild manifest fail, log in logs/archive.log'
 fi
 return ret
}

checkmemory() {
 allow_memory=8000000
 allow_max_memory=48000000
 max_matespace_size=' -xx:maxmetaspacesize=512m '
 total=`cat /proc/meminfo  |grep MemTotal |awk -F ' ' '{print $2}'`
 default_memory=true

 position=0
 for param in $ALL_ARR_OPT
  do
   if [[ $param == '-mem' ]]; then
    arr_index=$[position+1]
    memory=${ALL_ARR_OPT[position+1]}
    echo 'input direct memory:' $memory'MB'
    memory=$[memory * 1000]
    if [[ $memory =~ ^[0-9]*$ ]] && [[ $memory -gt $allow_memory ]]; then
      allow_memory=$memory
      default_memory=false
    else
     echo "direct memory must be greater than1111 $allow_memory!, current memory: $total!!"
    fi
   fi
   position=$[position+1]
 done

 if [ $default_memory == true ]; then
  if [[ $total -lt $allow_memory ]] ; then
     echo "direct memory must be greater than $allow_memory!, current memory: $total!!"
     exit
  fi
  if [[ $total -gt $allow_memory ]] && [[ $total -lt $allow_max_memory ]] ; then
     MAX_NEW_SIZE=' -XX:NewSize=3072m -XX:MaxNewSize=3072m '
     MEM_OPT="$max_matespace_size $max_new_size"

  elif [[ $total -gt $allow_memory ]] ; then
     NEW_RATIO=' -XX:NewSize=6144m -XX:MaxNewSize=6144m '
     MEM_OPT="$max_matespace_size $new_ratio"
  fi
else
  NEW_RATIO=2
  max_matespace_size=$[allow_memory / 16]
  MEM_OPT="$max_matespace_size $new_ratio"
 fi
}

if [[ $APP =~ '-' ]]; then
  APP=''
fi


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
checkmemory
startService