echo "current branch is : "$BUILDKITE_BRANCH
if [ $BUILDKITE_PULL_REQUEST = "false" ]; then
	SonarStatus_Url="https://sonarcloud.io/api/qualitygates/project_status?projectKey=java-tron&branch="$BUILDKITE_BRANCH
	Status=`curl -s $SonarStatus_Url | jq '.projectStatus.status'`
	echo "current branch sonarcloud status is : "$Status
		if [ $Status = null ]; then
	      echo "wait for check finish, 5m ....."
	      sleep 300
	      SonarStatus_Url="https://sonarcloud.io/api/qualitygates/project_status?projectKey=java-tron&branch="$BUILDKITE_BRANCH
	      Status=`curl -s $SonarStatus_Url | jq '.projectStatus.status'`
	fi

	if [ x"$Status" = x'"OK"' ];then
    		echo "Sonar Check Pass"
    		exit 0
    else
    	echo "Sonar Check Failed"
    	echo "Please visit https://sonarcloud.io/dashboard?branch="$BUILDKITE_BRANCH"&id=java-tron for more details"
    	exit 1
    fi
else
	echo "current PR number is : "$BUILDKITE_PULL_REQUEST
	SonarStatus_Url="https://sonarcloud.io/api/qualitygates/project_status?projectKey=java-tron&pullRequest="$BUILDKITE_PULL_REQUEST
	Status=`curl -s $SonarStatus_Url | jq '.projectStatus.status'`
	if [ $Status = null ]; then
	      echo "wait for check finish, 5m ....."
	      sleep 300
	      SonarStatus_Url="https://sonarcloud.io/api/qualitygates/project_status?projectKey=java-tron&pullRequest="$BUILDKITE_PULL_REQUEST
	      Status=`curl -s $SonarStatus_Url | jq '.projectStatus.status'`
	fi

	echo "current pullRequest sonarcloud status is : "$Status
	if [ x"$Status" = x'"OK"' ];then
    		echo "Sonar Check Pass"
    		exit 0
    else
    		echo "Sonar Check Failed"
    		echo "Please visit https://sonarcloud.io/dashboard?id=java-tron&pullRequest="$BUILDKITE_PULL_REQUEST" for more details"
    		exit 1
    fi
fi