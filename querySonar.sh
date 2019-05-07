
PassFlag=`curl -s 'https://sonarcloud.io/api/project_badges/measure?project=java-tron&metric=alert_status'|grep -A4 "quality gate"|grep "pass"|wc -l`
echo "Please visit https://sonarcloud.io/dashboard?id=java-tron for more details"
if [ $PassFlag -eq 0 ]; then
    echo "Sonar Check Failed"
    exit 1
else
    echo "Sonar Check Pass"
    exit 0
fi
