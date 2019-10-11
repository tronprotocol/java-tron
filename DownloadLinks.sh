PassFlag=`curl -s http://47.95.206.44:50080/Daily_Build_Task_Report | grep "Failed: 0" | wc -c`

if [ $PassFlag -eq 0 ]; then
    echo "Daily Build Stest Fail"
    echo "To view Daily Replay and Stress Test logs please visit website below on browsers"
    echo "--- http://47.95.206.44:50080/latestReplayLog"
    echo "--- http://47.95.206.44:50080/latestStressLog"

else
    echo "Daily Build Stest Pass"
    echo "Build on `date +"%Y-%m-%d"` 3:00:00 (CST), UTC +8"
    echo "Please visit following website to download java-tron.jar on browsers"
    echo "--- http://47.95.206.44:50080/Daily_Build/jFava-tron.jar"
    echo "To view Daily Replay and Stress Test logs please visit website below on browsers"
    echo "--- http://47.95.206.44:50080/latestReplayLog"
    echo "--- http://47.95.206.44:50080/latestStressLog"
    echo "The following compressed package is provided for user to set up Fullnode. Please use Linux OS to Download"
    echo "--- curl -# -O http://47.95.206.44:50080/Daily_Build/java-tron.tar.gz"
    echo "To unzip file use the command below"
    echo "--- tar -xzvf java-tron.tar.gz"
fi