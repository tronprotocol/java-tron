#DATE= $(date "+%Y-%m-%d")
#date =`date +"%Y-%m-%d" -d "-1day"`
curl http://60.205.215.34/Daily_Build_Task_Report

PassFlag=`curl http://60.205.215.34/Daily_Build_Task_Report | grep "Failed: 0"`
echo "PassFlag is: $PassFlag"

if [$PassFlag == ""]; then
    exit 0
else
    exit 1
fi