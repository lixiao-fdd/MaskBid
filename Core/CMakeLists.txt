cmake_minimum_required (VERSION 3.8)

project ("sBid" CXX)

SET(CMAKE_CXX_FLAGS "-std=c++17 -O3 -pthread")
SET(CMAKE_BINARY_DIR ${sBid_SOURCE_DIR}/build)

ADD_SUBDIRECTORY(src bin)
