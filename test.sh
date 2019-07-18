BUILDKITE_BRANCH=develop
echo "current branch is : "$BUILDKITE_BRANCH
SonarStatus_Url="https://sonarcloud.io/api/qualitygates/project_status?projectKey=java-tron&branch="$BUILDKITE_BRANCH
Status=`curl -s $SonarStatus_Url | jq '.projectStatus.status'`
echo "current branch sonarcloud status is : "$Status
if [ "$Status"x = "ERROR"x ]; then
    echo "Sonar Check Failed"
    echo "Please visit https://sonarcloud.io/dashboard?branch="$BUILDKITE_BRANCH"&id=java-tron for more details"
    exit 1
else
    echo "Sonar Check Pass"
    exit 0
fi