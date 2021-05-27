#include "network.h"

//网络初始化
bool Network::start(int port) {//作为服务端
	this->port = port;

	time(&rawtime);
	info = localtime(&rawtime);
	strftime(buffer, 80, "%Y%m%d%H%M%S", info);
	logFileName.append(buffer);
	logFileName.append(to_string(port));
	logFileName.append(".log");
	logger.open(logFileName, ios::out);

	//创建socket
	if ((this->sockSer = socket(AF_INET, SOCK_STREAM, 0)) == -1)
	{
		logger << "ERROR - Socket: " << strerror(errno) << endl;
		return false;
	}
	//填充套接字地址结构，包括地址族，ip和端口号
	bzero(&this->addrSer, sizeof(struct sockaddr_in));
	inet_aton((const char*)IP.c_str(), &(this->addrSer.sin_addr));
	this->addrSer.sin_family = AF_INET;
	this->addrSer.sin_port = htons(port);
	int opt = SO_REUSEADDR;
	setsockopt(this->sockSer, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
	// 接收缓冲区
	int nRecvBuf = 1024; //设置为1K
	setsockopt(this->sockSer, SOL_SOCKET, SO_RCVBUF, (const char*)&nRecvBuf, sizeof(int));
	// 发送缓冲区
	int nSendBuf = 1024; //设置为1K
	setsockopt(this->sockSer, SOL_SOCKET, SO_SNDBUF, (const char*)&nSendBuf, sizeof(int));
	//绑定
	if (bind(sockSer, (struct sockaddr*)(&this->addrSer), sizeof(struct sockaddr)) == -1)
	{
		logger << "ERROR - Bind: " << strerror(errno) << endl;
		return false;
	}
	// 监听
	if (listen(sockSer, 1) == -1)
	{
		logger << "ERROR - Listen: " << strerror(errno) << endl;
		return false;
	}
	logTime();
	logger << "+---- Waiting for connection" << endl;
	return true;
}

//接收连接
bool Network::acceptConnect() {
	socklen_t naddr = sizeof(struct sockaddr_in);
	if ((this->sockCli = accept(this->sockSer, (struct sockaddr*)(&this->addrCli), &naddr)) == -1)
	{
		//if (errno == EINTR) {
		//	printf("Client leave\n");
		//	continue;//必须加上这一句，当一个客户端断开时会引发中断异常，也就是这里的EINTR，通过这句话来重新accept，不然就会直接退出
		//}
		logger << "ERROR - Accept: " << strerror(errno) << endl;
		return false;
	}
	if (getpeername(this->sockCli, (struct sockaddr*)(&this->addrCli), &naddr) == -1)
	{
		logger << "ERROR - Get Peer: " << strerror(errno) << endl;
		return false;
	}
	logger << "+---- Accept connection from " << inet_ntoa(this->addrCli.sin_addr) << ":" << ntohs(this->addrCli.sin_port) << endl;

	return true;
}

//发送一个string
bool Network::mSend(int fd, string send_string)
{
	size_t size = send_string.size();
	string size_str = to_string(size);//需要发送的数据大小
	size_str = "SIZE" + size_str + "SIZE";

	char* head = new char[HEAD_SIZE];
	memset(head, '\0', HEAD_SIZE);
	strcpy(head, size_str.c_str());
	logger << "|head [";
	for (int i = 0; i < HEAD_SIZE; i++) {
		logger << head[i] << ", ";
	}
	logger << "]\n";
	if (send(fd, head, HEAD_SIZE, 0) == -1)
	{ //告知对方数据量
		logger << "ERROR - Head: " << strerror(errno) << endl;
		exit(1);
	}
	logger << "|size " << size << endl;
	if ((send(fd, send_string.c_str(), size, 0)) == -1)
	{ //发送数据
		logger << "ERROR - Send: " << strerror(errno) << endl;
		exit(1);
	}
	delete[] head;
	return true;
}

//接收一个string
bool Network::mReceive(int fd, string& recv_string)
{
	recv_string.clear();
	if (recv(fd, this->headBuf, HEAD_SIZE, 0) == -1)
	{ //接收缓冲区尺寸
		logger << "ERROR - Head: " << strerror(errno) << endl;
		exit(1);
	}
	logger << "|head [";
	for (int i = 0; i < HEAD_SIZE; i++) {
		logger << this->headBuf[i] << ", ";
	}
	logger << "]\n";
	string str(this->headBuf);
	int start = str.find_first_of("SIZE");
	int end = str.find("SIZE", start + 1);
	str = str.substr(start + 4, end - (start + 4));
	logger << "|size " << str << endl;

	int file_size = stoi(str.c_str()), recv_num; //信息大小
	char* cstr = new char[BUF_SIZE];           //接收缓冲区
	int buffSize = BUF_SIZE;
	memset(cstr, '\0', buffSize);
	while (file_size > 1) {
		if (file_size < BUF_SIZE) {
			delete[] cstr;
			cstr = new char[file_size];
			buffSize = file_size;
		}
		if ((recv_num = recv(fd, cstr, buffSize, 0)) == -1)
		{ //接收数据
			logger << "ERROR - Read: " << strerror(errno) << endl;
			exit(1);
		}
		recv_string.append(cstr, recv_num);
		file_size -= recv_num;
		logger << "|read " << recv_num << " remain: " << file_size << endl;
		memset(cstr, '\0', buffSize);
	}
	delete[] cstr;
	return true;
}

//发送一个文件
void Network::fSend(string fileName) {
	ifstream ist;
	ist.open(fileName, ios::in);
	if (!ist)
	{
		logger << "ERROR - File open: " << strerror(errno) << endl;
		exit(1);
	}
	string temp, container;
	while (ist >> temp) {
		container += (temp + "\n");
	}
	ist.close();

	logger << "\n";
	logTime();
	logger << "+---- Sending " << fileName << endl;
	mSend(this->sockCli,container);
	logger << "+---- Sent " << fileName << endl;
	logTime();
}

//接收一个文件
void Network::fReceive(string fileName) {
	ofstream ost;
	ost.open(fileName, ios::out);
	if (!ost)
	{
		logger << "ERROR - File create: " << strerror(errno) << endl;
		exit(1);
	}
	logger << "\n";
	logTime();
	logger << "+---- Receiving " << fileName << endl;
	string  container;
	mReceive(this->sockCli, container);
	ost << container;
	ost.close();
	logger << "+---- Received " << fileName << endl;
	logTime();
}
//关闭套接字
Network::~Network()
{
	if (sockSer)
		close(sockSer);
	if (sockCli)
		close(sockCli);
	logger.close();
}