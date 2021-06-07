#!/bin/bash
counts=2
if [ $1 ]; then
    counts=$1
fi
chmod +x /home/qqy/Demo/required/main/sBid
./task.sh $counts
BidCode=`./create.sh $counts`
echo "BidCode $BidCode"
./prepare.sh $counts $BidCode
./start.sh $counts
echo -e "中标金额: \c"
curl -H "Content-Type:application/json" -s -d '{"act":"showBidDetail","name":"0号招标","bidCode":"'"$BidCode"'"}' http://192.168.1.125:18001/json | jq -r '.amount' 
