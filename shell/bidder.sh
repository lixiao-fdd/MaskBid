#!/bin/bash

if [ $4 ]; then
    tenderAccountName=$1
    BidCode=$2
    counts=$3
    ports=($@)
    index=1
    portIndex=4
    while (($index <= $counts)); do
        port=${ports[portIndex]}
        # 随机账户名
        randName=$(date +%s%N | md5sum | head -c 10)
        # 随机金额
        randAmount=${RANDOM}
        # 防止金额为零
        let "randAmount++"
        # 随机等待
        randomWait=`shuf -i 5-30 -n1`
        echo `date '+%Y-%m-%d %H:%M:%S'`" - 等待$randomWait秒"
        sleep $randomWait
        # 取消登录
        curl -H "Content-Type:application/json" -s -d '{"act":"logout"}' http://192.168.1.125:${port}/data >/dev/null 2>&1
        # 注册
        curl -H "Content-Type:application/json" -s -d '{"act":"checkSignUp","data":{"newAccountName":"'"${randName}"'","newAccountRole":"1"}}' http://192.168.1.125:${port}/data >/dev/null 2>&1
        url="http://192.168.1.125:${port}/signup?newAccountName=${randName}&newAccountRole=1"
        curl -s ${url} >/dev/null 2>&1
        curl -H "Content-Type:application/json" -s -d '{"act":"listAccountInfo"}' http://192.168.1.125:${port}/data >/dev/null 2>&1
        # 投标
        echo -e `date '+%Y-%m-%d %H:%M:%S'`" - Bidder${index}: ${randName} 端口: ${port} 金额: ${randAmount} \c"
        curl -H "Content-Type:application/json" -s -d '{"act":"postBidAmount","data":{"tenderName":"'"${tenderAccountName}"'","bidCode":"'"${BidCode}"'","bidAmount":"'"${randAmount}"'"}}' http://192.168.1.125:${port}/data | jq -r '.data.postBidStatus'
        let "index++"
        let "portIndex++"
    done
else
    echo "parameters error"
fi
