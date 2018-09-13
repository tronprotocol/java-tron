#!/bin/bash




echo "$TRAVIS_BRANCH"

if [ "$TRAVIS_BRANCH" = "transfer_stest_to_docker" ];then
  echo "init env"
  ssh tron@39.106.62.219 -p 22008 sh /data/workspace/docker_workspace/stest.sh >stest.log 2>&1
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
