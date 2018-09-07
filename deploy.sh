#!/bin/bash

echo "$TRAVIS_BRANCH"

if [ "$TRAVIS_BRANCH" = "deploy_test" ];then
  echo "init env"
  ssh tron@47.93.9.236 -p 22008 sh /home/tron/workspace/deploy_all.sh
  echo "stest start"
  ./gradlew stest | tee stest.log
  echo "stest end"

  echo $?
  ret=$(cat test.txt | grep "stest FAILED" | wc -l)

  if [ $ret > 0 ];then
    exit 1
  fi

fi
echo "bye bye"

exit 0