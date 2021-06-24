#! /bin/bash

port=20000
if [ $1 ]; then
    port=$1
fi

tenderCounts=1
bidderCounts=$(shuf -i 2-8 -n1)
waitTime=$(shuf -i 4-8 -n1)
echo $(date '+%Y-%m-%d %H:%M:%S')" - 创建${bidderCounts}个投标者,${waitTime}分钟后开始竞标"

# 创建招标者
# 随机账户名
randName=$(date +%s%N)
tenderAccountName="apiTender_$randName"

SHELL_FOLDER=$(dirname $(readlink -f "$0"))
main="${SHELL_FOLDER}/required/main/"
root="${SHELL_FOLDER}/autoRun/"
rename="${root}/main/"
cd $main
rm -rf ${root}
mkdir ${root}

newFolder="${root}/tender1"
cp -r $main $root
mv $rename $newFolder
cd $newFolder

# 检查端口
pid=$(lsof -i:$port)
while [ -n "$pid" ]; do
    echo $(date '+%Y-%m-%d %H:%M:%S')" - 端口 $port 已被占用"
    let "port++"
    pid=$(lsof -i:$port)
done
ports[0]=$port

process="java -jar ./MaskBidClient-1.0.jar --server.port=$port --autorun=true"
eval $process >client.log 2>&1 &
echo $(date '+%Y-%m-%d %H:%M:%S')" - 创建 tender1 | 端口: ${port}"
let "port++"

index=1
while (($index <= $bidderCounts)); do
    newFolder="${root}/bidder${index}"
    cp -r $main $root
    mv $rename $newFolder
    cd $newFolder
    sleep 1

    # 检查端口
    pid=$(lsof -i:$port)
    while [ -n "$pid" ]; do
        echo $(date '+%Y-%m-%d %H:%M:%S')" - 端口 $port 已被占用"
        let "port++"
        pid=$(lsof -i:$port)
    done
    ports[$index]=$port

    process="java -jar ./MaskBidClient-1.0.jar --server.port=$port --autorun=true"
    eval $process >client.log 2>&1 &
    echo $(date '+%Y-%m-%d %H:%M:%S')" - 创建 bidder${index} | 端口: ${port}"
    let "index++"
    let "port++"
done

sleep 1
cd $SHELL_FOLDER
BidCode=$(./tender.sh $tenderAccountName $waitTime ${ports[0]})

startTimeStr=$(date '+%Y-%m-%d %H:%M:%S')
startTime=$(date +%s -d "$startTimeStr")
duration=$((${waitTime} * 60))
endTime=$((${startTime} + ${duration}))
endTimeStr=$(date -d @$endTime '+%Y-%m-%d %H:%M:%S')
echo "$startTimeStr - 创建新标的: $BidCode"
echo "$startTimeStr - 竞标开始于: $endTimeStr"
sleep 2
./bidder.sh $tenderAccountName $BidCode $bidderCounts ${ports[@]}
sleep 1
timeNow=$(date +%s)
while (($endTime > $timeNow)); do
    diff=$(($endTime - $timeNow))
    diffMin=$(($diff / 60 + 1))
    echo $(date '+%Y-%m-%d %H:%M:%S')" - 等待$diffMin分钟"
    sleep 1m
    timeNow=$(date +%s)
done
echo $(date '+%Y-%m-%d %H:%M:%S')" - 运行结束"
