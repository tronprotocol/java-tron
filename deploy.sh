#!/bin/bash
testnet=(
47.94.231.67
47.94.10.122
)
stest_server=""
for i in ${testnet[@]}; do
  docker_num=`ssh -p 22008 -t java-tron@$i 'docker ps -a | wc -l'`
  echo $docker_num
  docker_num=`echo $docker_num | tr -d "\r"`
  echo $docker_num
  if [[ ${docker_num} -le 6 ]];
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



echo "$TRAVIS_BRANCH"

if [ "$TRAVIS_BRANCH" = "transfer_stest_to_docker" ];then
  echo "init env"
  ssh java-tron@$stest_server -p 22008 sh /data/workspace/docker_workspace/stest.sh >stest.log 2>&1
  echo "stest start"
  cat stest.log | grep "Stest result is:" -A 10000
  #./gradlew stest | tee stest.log
  echo "stest end"

  echo $?
  ret=$(cat stest.log | grep "stest FAILED" | wc -l)

  if [ $ret != 0 ];then
    echo $ret
    rm -f stest.log
    exit 1
  fi

fi
echo "bye bye"
rm -f stest.log
exit 0
