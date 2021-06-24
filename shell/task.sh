#!/bin/bash
tenderCounts=1
bidderCounts=2
port=17500
if [ $3 ]; then
    tenderCounts=$1
    bidderCounts=$2
    port=$3
fi
killall sBid >/dev/null 2>&1
killall java >/dev/null 2>&1
SHELL_FOLDER=$(dirname $(readlink -f "$0"))
main="${SHELL_FOLDER}/required/main/"
user0="${SHELL_FOLDER}/required/0/"
user1="${SHELL_FOLDER}/required/1/"
user2="${SHELL_FOLDER}/required/2/"
user3="${SHELL_FOLDER}/required/3/"
root="${SHELL_FOLDER}/runtime/"
rename="${root}/main/"
hashName=""
end=2
cd $main
stat ./MaskBidClient-1.0.jar | grep 最近更改

if [ $2 ]; then
    if [ $2 -lt 2 ]; then
        echo "parameters error"
        exit 0
    fi
    rm -rf ${root}
    mkdir ${root}

    index=1
    while (($index <= $tenderCounts)); do
        newFolder="${root}/tender${index}"
        cp -r $main $root
        mv $rename $newFolder
        cd $newFolder
        sleep 1
        java -jar ./MaskBidClient-1.0.jar --server.port=$port >client.log 2>&1 &
        echo "启动 tender${index} | 端口: ${port}"
        let "index++"
        let "port++"
    done

    index=1
    while (($index <= $bidderCounts)); do
        newFolder="${root}/bidder${index}"
        cp -r $main $root
        mv $rename $newFolder
        cd $newFolder
        sleep 1
        java -jar ./MaskBidClient-1.0.jar --server.port=$port >client.log 2>&1 &
        echo "启动 bidder${index} | 端口: ${port}"
        let "index++"
        let "port++"
    done
    exit 0
elif [ $1 ]; then
    if [ $1 -lt 2 ]; then
        echo "parameters error"
        exit 0
    fi
    end=$1
fi

rm -rf ${root}
mkdir ${root}

newFolder="${root}/tender1"
rename0="${root}/tender1/0"
newFolder0="${root}/tender1/Files_040734ee3a13d81d0894ff39a4704eb0c3364f5a"
cp -r $main $root
mv $rename $newFolder
cp -r $user0 $newFolder
mv $rename0 $newFolder0
cd $newFolder
sleep 1
java -jar ./MaskBidClient-1.0.jar --server.port=$port >client.log 2>&1 &
echo "启动 tender1 | 端口: ${port}"
let "port++"
index=1
while (($index <= $end)); do
    newFolder="${root}/bidder${index}"
    cp -r $main $root
    mv $rename $newFolder
    cd $newFolder
    sleep 1
    java -jar ./MaskBidClient-1.0.jar --server.port=$port >client.log 2>&1 &
    echo "启动 bidder${index} | 端口: ${port}"
    let "index++"
    let "port++"
done
