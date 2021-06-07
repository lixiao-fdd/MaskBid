#!/bin/bash

if [ $2 ]; then
    counts=$1
    BidCode=$2

    index=1
    port=19001
    while (($index <= $counts)); do
        # 随机账户名
        randName=$(date +%s%N | md5sum | head -c 10)
        # 随机金额
        randAmount=${RANDOM}
        # 防止金额为零
        let "randAmount++"
        # 注册
        curl -H "Content-Type:application/json" -s -d '{"act":"createAccount","name":"'"${randName}:1"'"}' http://192.168.1.125:${port}/json >/dev/null 2>&1
        # 登录
        curl -H "Content-Type:application/json" -s -d '{"act":"login","name":"'"${randName}:1"'"}' http://192.168.1.125:${port}/json >/dev/null 2>&1
        # 加载账户
        curl -H "Content-Type:application/json" -s -d '{"act":"listAccountInfo"}' http://192.168.1.125:${port}/json >/dev/null 2>&1
        # 加载招标
        curl -H "Content-Type:application/json" -s -d '{"act":"setBid","tenderName":"0号招标","bidCode":"'"${BidCode}"'"}' http://192.168.1.125:${port}/json >/dev/null 2>&1
        # 投标
        echo -e "Bidder${index}: ${randName} 金额: ${randAmount} \c"
        curl -H "Content-Type:application/json" -s -d '{"act":"setBidContent","name":"'"${randName}:1"'","amount":"'"${randAmount}"'","tenderName":"0号招标","bidCode":"'"${BidCode}"'"}' http://192.168.1.125:${port}/json | jq -r '.registResult'
        let "index++"
        let "port++"
    done
else
    echo "parameters error"
fi
