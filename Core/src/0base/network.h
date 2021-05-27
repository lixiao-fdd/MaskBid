#pragma once
#include "base.h"
extern bool debug;
using namespace std;

const int BUF_SIZE = 1024;
const int HEAD_SIZE = 32;

class Network
{
public:
	void fSend(string fileName);							//发送一个文件
	void fReceive(string fileName);							//接收一个文件
	bool start(int port);
	bool acceptConnect();
	~Network();
private:
	string IP = "0.0.0.0";
	int port;
	struct sockaddr_in addrSer, addrCli;
	int sockSer = -1, sockCli = -1;
	bool bigMe;
	string codeName;
	string delimiter = ";";
	char recvSizeBuf[BUF_SIZE];
	char checkBuf[BUF_SIZE];
	char headBuf[HEAD_SIZE];
	bool mSend(int fd, string send_string);
	bool mReceive(int fd, string& recv_string);
	string logFileName = "./log/coreNet_";
	ofstream logger;
	time_t rawtime;
	struct tm* info;
	char buffer[80];
	inline void logTime() {
		time(&rawtime);
		info = localtime(&rawtime);
		strftime(buffer, 80, "%Y-%m-%d %H:%M:%S", info);
		logger << "+---- " << buffer << endl;
	}
};