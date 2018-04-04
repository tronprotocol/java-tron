#!/bin/bash
kill -9 `cat pid.txt`
nohup  java -jar /home/tron/java-tron/java-tron.jar -c /home/tron/java-tron/config.conf > tron-shell.log 2>&1 & echo $! >pid.txt