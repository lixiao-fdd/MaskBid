#! /bin/bash
round=1
while true; do
    ./auto.sh 25000
    echo `date '+%Y-%m-%d %H:%M:%S'`" - 完成$round轮"
    sleep 5
    let "round++"
done
