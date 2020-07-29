#!/bin/bash
if [[ "$TRAVIS_BRANCH" = "develop" || "$TRAVIS_BRANCH" = "master" ]];then
    stestlogname="`date +%Y%m%d%H%M%S`_stest.log"
    echo "Trying to connect to stest_server network"
    timeout 10 ping -c 5  47.93.42.145 > /dev/null || exit 1
    timeout 10 ping -c 5  47.93.18.60  > /dev/null || exit 1
    echo "Successfully connected to stest_server network"
    stest_server=""
    docker_num_in_145=`ssh -p 22008 -t java-tron@47.93.42.145 'docker ps -a | wc -l'`
    docker_num_in_145=`echo $docker_num_in_145 | tr -d "\r"`
    docker_num_in_60=`ssh -p 22008 -t java-tron@47.93.18.60 'docker ps -a | wc -l'`
    docker_num_in_60=`echo $docker_num_in_60 | tr -d "\r"`
    if [ $docker_num_in_145 -le $docker_num_in_60 ];
      then
      docker_num=$docker_num_in_145
      stest_server=47.93.42.145
      else
        docker_num=$docker_num_in_60
        stest_server=47.93.18.60
    fi

    if [[ ${docker_num} -le 3 ]];
    then
    echo $stest_server
    else
        stest_server=""
      fi

    if [ "$stest_server" = "" ]
    then
    echo "All docker servers are busy, stest FAILED"
    exit 1
    fi

    change_branch_CMD="sed -i '1c branch_name_in_CI=$TRAVIS_BRANCH' /data/workspace/docker_workspace/do_stest.sh"

    echo "Initialize the docker stest env"
    echo "'$stest_server' is selected as stest server in this instance"
    ssh java-tron@$stest_server -p 22008 $change_branch_CMD
    `ssh java-tron@$stest_server -p 22008 sh /data/workspace/docker_workspace/do_stest.sh >$stestlogname 2>&1` &
    sleep 300 && echo $TRAVIS_BRANCH &
    wait
    if [[ `find $stestlogname -type f | xargs grep "Connection refused"` =~ "Connection refused" || `find $stestlogname -type f | xargs grep "stest FAILED"` =~ "stest FAILED" ]];
    then
      rm -f $stestlogname
      echo "Retrying stest task for the first time"
      ssh java-tron@$stest_server -p 22008 $change_branch_CMD
      `ssh java-tron@$stest_server -p 22008 sh /data/workspace/docker_workspace/do_stest.sh >$stestlogname 2>&1` &
      sleep 300 && echo $TRAVIS_BRANCH &
      wait
    fi
    if [[ `find $stestlogname -type f | xargs grep "Connection refused"` =~ "Connection refused" || `find $stestlogname -type f | xargs grep "stest FAILED"` =~ "stest FAILED" ]];
    then
      rm -f $stestlogname
      echo "Retrying stest task for the second time"
      ssh java-tron@$stest_server -p 22008 $change_branch_CMD
      `ssh java-tron@$stest_server -p 22008 sh /data/workspace/docker_workspace/do_stest.sh >$stestlogname 2>&1` &
      sleep 300 && echo $TRAVIS_BRANCH &
      wait
    fi
    echo "Starting stest"
    cat $stestlogname | grep "Stest result is:" -A 10000
    echo "stest end"

    echo $?
    ret=$(cat $stestlogname | grep "stest FAILED" | wc -l)

    if [ $ret != 0 ];then
      echo $ret
      rm -f $stestlogname
      exit 1
    fi
fi
echo "Closing"
echo $stest_server
rm -f $stestlogname
exit 0
