echo $BUILDKITE_PULL_REQUEST
SonarStatus_Url="https://sonarcloud.io/api/qualitygates/project_status?projectKey=java-tron&pullRequest="$BUILDKITE_PULL_REQUEST
Status=`curl -s $SonarStatus_Url | jq '.projectStatus.status'`
echo $Status
if [ "$Status"=="ERROR" ]; then
    echo "Sonar Check Failed"
    echo "Please visit https://sonarcloud.io/dashboard?id=java-tron&pullRequest="$BUILDKITE_PULL_REQUEST" for more details"
    exit 1
else
    echo "Sonar Check Pass"
    exit 0
fi