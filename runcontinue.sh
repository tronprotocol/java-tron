
curl http://60.205.215.34/Daily_Build_Task_Report

PassFlag=`curl http://60.205.215.34/Daily_Build_Task_Report | grep "Failed: 0"`

if [$PassFlag == ""]; then
    echo "Stest Failed"
    exit 1
else
    echo "Stest Pass"
    exit 0
fi