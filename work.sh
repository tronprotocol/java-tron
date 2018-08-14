#!/bin/bash

# Function: to start, stop and restart java-tron.
# Usage: bash work.sh start|stop|restart.
# Note: modify the paths and private key to your own.

# Auther: haoyouqiang
# Since: 2018/5/27 
# Version: 1.0

if [ $# -ne 1 ]; then
    echo "Usage: bash work.sh start|stop|restart."
    exit 1
fi

# Increase memory limit that JVM can use to avoid OOM error:
# 80% of your physical memory may be a proper ceiling that JVM can use.
# By default there, JVM initializes with 1g memory and can use 32g at most.
JVM_OPTIONS="-Xms1g -Xmx32g"

JAR_FILE_PATH="./build/libs/java-tron.jar"
PID_FILE_PATH="java-tron.pid"
LOG_FILE_PATH="java-tron.log"

CONF_FILE_PATH="./build/resources/main/config.conf"

PRIVATE_KEY="650950B193DDDDB35B6E48912DD28F7AB0E7140C1BFDEFD493348F02295BD812"

case "${1}" in
    start)
        # Already running
        if [ -f ${PID_FILE_PATH} ]; then
            pid=$(cat ${PID_FILE_PATH})
            if $(ps -p ${pid} > /dev/null); then
                echo "Already running [PID: ${pid}], you can stop it and retry."
                exit 1
            fi
        fi

        nohup java ${JVM_OPTIONS} \
            -jar ${JAR_FILE_PATH} \
            -p ${PRIVATE_KEY} --witness \
            -c ${CONF_FILE_PATH} \
            > ${LOG_FILE_PATH} 2>&1 \
            & echo $! > ${PID_FILE_PATH}

        if [ $? -eq 0 ]; then
            echo "Succeeded to start java-tron."
        else
            echo "Failed to start java-tron."
        fi
    ;;
    stop)
        kill $(cat ${PID_FILE_PATH})

        if [ $? -eq 0 ]; then
            rm ${PID_FILE_PATH}
            echo "Succeeded to stop java-tron."
        else
            echo "Failed to stop java-tron."
        fi
    ;;
    restart)
        ${0} stop && sleep 1 && ${0} start
    ;;
esac

