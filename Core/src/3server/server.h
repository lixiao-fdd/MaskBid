#pragma once
#include "../3sBid/sBid.h"

class Server {
private:
	string IP = "0.0.0.0";
	int port = 18000;
	struct socket_info
	{
		struct sockaddr_in sktaddr;
		int fd;
	};
	int MAX_CONN = 100;
	socket_info soc[100];
public:
	int start() {
		socket_info listenskt, connskt;
		pthread_t tid;
		socklen_t clit_size;
		socket_info soc[MAX_CONN];   //用来存放每个连接的信息
		int i = 0;

		memset(&listenskt, 0, sizeof(listenskt));
		memset(&connskt, 0, sizeof(connskt));

		if ((listenskt.fd = socket(AF_INET, SOCK_STREAM, 0)) == -1)
		{
			cout << "socket create failed : " << strerror(errno) << endl;
			return 0;
		}

		listenskt.sktaddr.sin_family = AF_INET;
		listenskt.sktaddr.sin_port = htons(port);
		listenskt.sktaddr.sin_addr.s_addr = inet_addr(IP.c_str());

		if (bind(listenskt.fd, (sockaddr*)&listenskt.sktaddr, sizeof(listenskt.sktaddr)) == -1)
		{
			cout << "bind error : " << strerror(errno) << endl;
			return 0;
		}

		if (listen(listenskt.fd, 100) == -1)
		{
			cout << "listen error : " << strerror(errno) << endl;
			return 0;
		}

		cout << "Init Success ! " << endl;
		cout << "host ip : " << inet_ntoa(listenskt.sktaddr.sin_addr) << "  port : " << ntohs(listenskt.sktaddr.sin_port) << endl;

		cout << "Waiting for connections ... " << endl;

		while (1)
		{
			clit_size = sizeof(connskt.sktaddr);
			if ((connskt.fd = accept(listenskt.fd, (sockaddr*)&connskt.sktaddr, &clit_size)) == -1)
			{
				if (errno == EINTR)continue;  //必须加上这一句，当一个客户端断开时会引发中断异常，也就是这里的EINTR，通过这句话来重新accept，不然就会直接退出
				cout << "accept error : " << strerror(errno) << endl;
				return 0;
			}
			memcpy(&soc[i], &connskt, sizeof(connskt)); //将新连接的信息拷贝到soc数组中

			pthread_create(&tid, NULL, connfun, (void*)&soc[i++]);
			pthread_detach(tid);

			if (i >= MAX_CONN)  //如果连接数过多，就不再接收连接请求了
			{
				cout << "Reach the max connections : " << strerror(errno) << endl;
				break;
			}
		}

		close(listenskt.fd);
		return 0;
	}

	static void* connfun(void* arg)  //线程入口，处理客户端与服务端的通信
	{
		socket_info* connskt = (socket_info*)arg;  //参数强制转换
		cout << inet_ntoa((*connskt).sktaddr.sin_addr) << ":" << ntohs((*connskt).sktaddr.sin_port) << " connected ...  " << endl;
		while (1)
		{
			char buf[1024];

			int readstate = read(connskt->fd, buf, sizeof(buf));

			if (readstate == -1)
			{
				cout << "read error : " << strerror(errno) << endl;
				break;
			}
			else if (readstate == 0)  //客户端退出
			{
				cout << inet_ntoa((*connskt).sktaddr.sin_addr) << ":" << ntohs((*connskt).sktaddr.sin_port) << " exit ... " << endl;
				break;
			}
			write(STDOUT_FILENO, buf, readstate);  //打印收到的数据
			cout << "      (From " << inet_ntoa((*connskt).sktaddr.sin_addr) << ":" << ntohs((*connskt).sktaddr.sin_port) << ")" << endl;
			for (int i = 0; i < readstate; i++)buf[i] = toupper(buf[i]); //字母转为大写
			write(connskt->fd, buf, readstate);  //发回客户端	
		}
		close(connskt->fd);  //关闭当前连接所使用的文件描述符
		return NULL;
	}

};