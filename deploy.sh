#!/bin/bash

echo "$TRAVIS_BRANCH"

if [ "$TRAVIS_BRANCH" = "deploy_test" ];then
  echo "init env"
  ssh tron@47.93.9.236 -p 22008 sh /home/tron/workspace/deploy_all.sh
  echo "stest start"
  ./gradlew stest | tee stest.log
  echo "stest end"

  echo $?

  echo "stest end endendendendend"


fi
echo "bye bye"

exit 1