#DATE= $(date "+%Y-%m-%d")
#date =`date +"%Y-%m-%d" -d "-1day"`
curl http://60.205.215.34/Daily_Build_Task_Report
echo "Build on `date +"%Y-%m-%d"` 3:00:00 (CST), UTC +8"
echo "Please visit following website to download java-tron.jar on browsers"
echo "--- http://60.205.215.34/Daily_Build/java-tron.jar"
echo "To view Daily Rplay and Stress Test logs please visit website below on browsers"
echo "--- http://60.205.215.34/latestReplayLog"
echo "--- http://60.205.215.34/latestStressLog"
echo "The following compressed package is provided for user to set up Fullnode. Please use Linux OS to Download"
echo "--- curl -# -O http://60.205.215.34/Daily_Build/java-tron.tar.gz"
echo "To unzip file use the command below"
echo "--- tar -xzvf java-tron.tar.gz"