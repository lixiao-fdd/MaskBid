#!/bin/bash

if [ $1 ]; then
    counts=$1
    index=1
    port=19001
    while (($index <= $counts)); do
        curl -H "Content-Type:application/json" -s -d '{"act":"prepareBid"}' http://192.168.1.125:${port}/json >/dev/null 2>&1 &
        let "index++"
        let "port++"
    done
    echo "竞标中..."
    wait
else
    echo "parameters error"
fi
