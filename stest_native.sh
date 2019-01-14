#!/bin/bash
#if you can't ssh to the docker server, add your id_rsa.pub from dict "~/.ssh" to the server authorized_keys file.
stestlogname="`date +%Y%m%d%H%M%S`_stest.log"
testnet=(
47.94.10.122
47.94.231.67
)
stest_server=""
for i in ${testnet[@]}; do
  docker_num=`ssh -p 22008 -t java-tron@$i 'docker ps -a | wc -l'`
  echo $docker_num
  docker_num=`echo $docker_num | tr -d "\r"`
  echo $docker_num
  if [[ ${docker_num} -le 4 ]];
  then
  stest_server=$i
  echo $stest_server
  break
  else
    continue
  fi
done
if [ "$stest_server" = "" ]
then
echo "All docker server is busy, stest FAILED"
exit 1
fi

echo "init env"
ssh java-tron@$stest_server -p 22008 sh /data/workspace/docker_workspace/stest.sh >$stestlogname 2>&1
if [[ `find $stestlogname -type f | xargs grep "Connection refused"` =~ "Connection refused" || `find $stestlogname -type f | xargs grep "stest FAILED"` =~ "stest FAILED" ]];
then
  rm -f $stestlogname
  echo "first if"
  ssh java-tron@$stest_server -p 22008 sh /data/workspace/docker_workspace/stest.sh >$stestlogname 2>&1
fi
if [[ `find $stestlogname -type f | xargs grep "Connection refused"` =~ "Connection refused" || `find $stestlogname -type f | xargs grep "stest FAILED"` =~ "stest FAILED" ]];
then
  rm -f $stestlogname
  echo "second if"
  ssh java-tron@$stest_server -p 22008 sh /data/workspace/docker_workspace/stest.sh >$stestlogname 2>&1
fi
echo "stest start"
cat $stestlogname | grep "Stest result is:" -A 10000
echo "stest end"

echo $?
ret=$(cat $stestlogname | grep "stest FAILED" | wc -l)

if [ $ret != 0 ];then
  echo $ret
  rm -f $stestlogname
  exit 1
fi

echo "bye bye"
rm -f $stestlogname
exit 0
