#!/bin/bash

cat tron > ~/.ssh/id_rsa
chmod 600 ~/.ssh/id_rsa
./gradlew clean shadowJar
ssh deploy@47.93.9.236 -p 22008 touch bbb.txt
ssh deploy@47.93.9.236 -p 22008 mkdir tron
scp -P 22008 build/libs/java-tron.jar deploy@47.93.9.236:/home/deploy/tron/
scp -P 22008 start.sh deploy@47.93.9.236:/home/deploy/tron/
# ssh deploy@47.93.9.236 -p 22008 sh tron/start.sh
ls -a
