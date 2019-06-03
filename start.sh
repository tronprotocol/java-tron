#!/bin/bash
kill -9 `cat /home/tron/pid.txt`
nohup  java -jar /home/tron/java-tron/java-tron.jar -p $LOCAL_WITNESS_PRIVATE_KEY --witness -c /home/tron/config.conf > /home/tron/tron-shell.log 2>&1 & echo $! >/home/tron/pid.txt