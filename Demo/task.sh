#!/bin/bash
tenderCounts=$1
bidderCounts=$2
killall sBid
killall java
SHELL_FOLDER=$(dirname $(readlink -f "$0"))
main="${SHELL_FOLDER}/required/main/"
user0="${SHELL_FOLDER}/required/0/"
user1="${SHELL_FOLDER}/required/1/"
user2="${SHELL_FOLDER}/required/2/"
user3="${SHELL_FOLDER}/required/3/"
root="${SHELL_FOLDER}/runtime/"
rename="${root}/main/"
if [ $2 ]; then
    rm -rf ${root}
    mkdir ${root}

    index=1
    while (($index <= $tenderCounts)); do
        newFolder="${root}/tender${index}"
        port="1800${index}"
        cp -r $main $root
        mv $rename $newFolder
        cd $newFolder
        sleep 1
        java -jar ./sBidBC.jar $port >!Archive.log 2>&1 &
        echo "start tender${index} at ${port}"
        let "index++"
    done

    index=1
    while (($index <= $bidderCounts)); do
        newFolder="${root}/bidder${index}"
        port="1900${index}"
        cp -r $main $root
        mv $rename $newFolder
        cd $newFolder
        sleep 1
        java -jar ./sBidBC.jar $port >!Archive.log 2>&1 &
        echo "start bidder${index} at ${port}"
        let "index++"
    done
else
    rm -rf ${root}
    mkdir ${root}

    newFolder="${root}/tender1"
    rename0="${root}/tender1/0"
    newFolder0="${root}/tender1/Files_040734ee3a13d81d0894ff39a4704eb0c3364f5a"
    port="18001"
    cp -r $main $root
    mv $rename $newFolder
    cp -r $user0 $newFolder
    mv $rename0 $newFolder0
    cd $newFolder
    sleep 1
    java -jar ./sBidBC.jar $port >!Archive.log 2>&1 &
    echo "start tender1 at ${port}"

    newFolder="${root}/bidder1"
    rename1="${root}/bidder1/1"
    newFolder1="${root}/bidder1/Files_565257b028087aff557e3205b810256f27c0c9c8"
    port="19001"
    cp -r $main $root
    mv $rename $newFolder
    cp -r $user1 $newFolder
    mv $rename1 $newFolder1
    cd $newFolder
    sleep 1
    java -jar ./sBidBC.jar $port >!Archive.log 2>&1 &
    echo "start bidder1 at ${port}"

    newFolder="${root}/bidder2"
    rename2="${root}/bidder2/2"
    newFolder2="${root}/bidder2/Files_b6b30e609e51531bf63189e501899742777b4923"
    port="19002"
    cp -r $main $root
    mv $rename $newFolder
    cp -r $user2 $newFolder
    mv $rename2 $newFolder2
    cd $newFolder
    sleep 1
    java -jar ./sBidBC.jar $port >!Archive.log 2>&1 &
    echo "start bidder2 at ${port}"

    newFolder="${root}/bidder3"
    rename3="${root}/bidder3/3"
    newFolder3="${root}/bidder3/Files_f57253daf5fcee353d3e3d7e669c6bb9ab953ddd"
    port="19003"
    cp -r $main $root
    mv $rename $newFolder
    cp -r $user3 $newFolder
    mv $rename3 $newFolder3
    cd $newFolder
    sleep 1
    java -jar ./sBidBC.jar $port >!Archive.log 2>&1 &
    echo "start bidder3 at ${port}"
fi
