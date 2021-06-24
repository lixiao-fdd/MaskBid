#!/bin/bash
if [ $3 ]; then
    tenderAccountName=$1
    newBidDuration=$2
    port=$3
    date=$(date '+%Y年%m月%d日 %H时%M分')
    # 取消登录
    curl -H "Content-Type:application/json" -s -d '{"act":"logout"}' http://192.168.1.125:${port}/data >/dev/null 2>&1
    # 注册
    curl -H "Content-Type:application/json" -s -d '{"act":"checkSignUp","data":{"newAccountName":"'"${tenderAccountName}"'","newAccountRole":"0"}}' http://192.168.1.125:${port}/data >/dev/null 2>&1
    url="http://192.168.1.125:${port}/signup?newAccountName=${tenderAccountName}&newAccountRole=0"
    curl -s ${url} >/dev/null 2>&1
    curl -H "Content-Type:application/json" -s -d '{"act":"listAccountInfo"}' http://192.168.1.125:${port}/data >/dev/null 2>&1
    # 发布新标的
    curl -H "Content-Type:application/json" -s -d '{"act":"postNewBid","data":{"newBidName":"api测试","newBidDateStart":"'"${date}"'","newBidDuration":"'"${newBidDuration}"'","newBidDurationUnit":"minutes","newBidContent":""}}' http://192.168.1.125:${port}/data | jq -r '.data.bidCode'
    curl -H "Content-Type:application/json" -s -d '{"act":"autoDown"}' http://192.168.1.125:${port}/data >/dev/null 2>&1
fi
