#!/bin/bash

if [[ $TRAVIS_BRANCH == 'deploy_test' ]]
    ssh tron@47.93.9.236 -p 22008 sh /home/tron/workspace/deploy_all.sh
    ./gradlew stest
else
  #show me the money
fi