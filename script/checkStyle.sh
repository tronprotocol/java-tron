./gradlew clean build -x test > build.log 2>&1
if [ $? != 0 ];then
    echo "run ./gradlew build fail, Please check you code, Or just retry this test"
    exit 1
fi
echo "------------------------------    checkStyle  check    ------------------------------"
#echo $BUILDKITE_PULL_REQUEST_BASE_BRANCH
#if [[ x"$BUILDKITE_PULL_REQUEST_BASE_BRANCH" != x'develop' && x"$BUILDKITE_PULL_REQUEST_BASE_BRANCH" != x'master' ]];then
#	echo "BUILDKITE_PULL_REQUEST_BASE_BRANCH isnot develop or master, SKIPED"
#	exit 0
#fi

grep -v ":checkstyleMain\|:checkstyleTest\|:lint" build.log |grep "ant:checkstyle" > checkStyle.log 2>&1
checkNum=`cat checkStyle.log | wc -l`
if [ ${checkNum} -gt 0 ];then
    echo "please fix checkStyle problem,"
    echo "run [ ./gradlew clean build -x test ], and you can find checkStyle report in framework/build/reports/checkstyle/"
    echo "!!!!!   checkStyle Num  ${checkNum}   !!!!!"
    cat checkStyle.log

   echo "checkStyle Failed, please fix checkStyle problem"
    touch checkFailTag
else
     echo "checkStyle problem zero"
fi
exit 0
