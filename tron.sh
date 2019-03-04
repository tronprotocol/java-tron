#!/bin/bash

if [ $encrypted_e5855cb9e09c_key ];then
  openssl aes-256-cbc -K $encrypted_e5855cb9e09c_key -iv $encrypted_e5855cb9e09c_iv -in tron.enc -out tron -d
  cat tron > ~/.ssh/id_rsa
  chmod 600 ~/.ssh/id_rsa
  echo "Add docker server success"
  sonar-scanner
fi

cp -f config/checkstyle/checkStyle.xml config/checkstyle/checkStyleAll.xml
