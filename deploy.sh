#!/bin/bash
./gradlew clean shadowJar

HOST_IP_1=47.93.9.236
HOST_IP_2=47.93.33.201
HOST_IP_3=123.56.10.6
HOST_IP_4=39.107.80.135
HOST_IP_5=47.93.184.2

for HOST_IP in $HOST_IP_1 $HOST_IP_2 $HOST_IP_3 $HOST_IP_4 $HOST_IP_5
do
    ssh tron@$HOST_IP -p 22008 mkdir java-tron
    scp -P 22008 build/libs/java-tron.jar tron@$HOST_IP:/home/tron/java-tron/
    scp -P 22008 start.sh tron@$HOST_IP:/home/tron/java-tron/
    ssh tron@$HOST_IP -p 22008 sh java-tron/start.sh
    ls -a
done