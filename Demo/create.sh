#!/bin/bash
counts=2
if [ $1 ]; then
    counts=$1
fi
curl -H "Content-Type:application/json" -s -d '{"act":"login","name":"0号招标:0"}' http://192.168.1.125:18001/json >/dev/null 2>&1 
curl -H "Content-Type:application/json" -s -d '{"act":"listAccountInfo"}' http://192.168.1.125:18001/json >/dev/null 2>&1 
curl -H "Content-Type:application/json" -s -d '{"act":"listOldBids","name":"0号招标"}' http://192.168.1.125:18001/json >/dev/null 2>&1 
curl -H "Content-Type:application/json" -s -d '{"act":"createNewBid","name":"0号招标","counts":"'"${counts}"'","dateStart":"2021-06-06T19:35","dateEnd":"2021-06-07T19:35","bidName":"api测试"}' http://192.168.1.125:18001/json | jq -r '.bidCode'
