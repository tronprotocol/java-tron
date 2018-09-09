#!/bin/bash

echo "$TRAVIS_BRANCH"

if [ "$TRAVIS_BRANCH" = "develop" ];then
  echo "init env"
  ssh tron@47.93.9.236 -p 22008 sh /home/tron/workspace/deploy_all.sh
  echo "stest start"
  ./gradlew stest | tee stest.log
  echo "stest end"

  echo $?
  ret=$(cat stest.log | grep "stest FAILED" | wc -l)

  if [ $ret != 0 ];then
    echo $ret
    exit 1
  fi

fi
echo "bye bye"

exit 0
