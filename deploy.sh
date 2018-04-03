#!/bin/bash
./gradlew clean shadowJar
ssh tron@47.93.9.236 -p 22008 touch bbb.txt
ssh tron@47.93.9.236 -p 22008 mkdir tron
scp -P 22008 build/libs/java-tron.jar tron@47.93.9.236:/home/tron/tron/
scp -P 22008 start.sh tron@47.93.9.236:/home/tron/tron/
# ssh tron@47.93.9.236 -p 22008 sh tron/start.sh
ls -a
