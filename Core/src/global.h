#pragma once
#include "./0base/base.h"
#include "./0base/Mod_p.h"
#include "./0base/G_q.h"
#include "./0base/Cyclic_group.h"
#include "./0base/sha256.h"
#include "./0base/network.h"
#include "./1ElGamal/ElGamal.h"
#include "./1Pedersen/Pedersen.h"

extern G_q G;               // group used for the Pedersen commitment
extern G_q H;               // group used for the the encryption
extern ElGamal El;         // The class for encryption and decryption
extern Pedersen Ped;        // Object which calculates the commitments
extern bool vMode;
extern bool debug;
extern string filesPath;

inline void waitFile(string fileName, ifstream& ist) {
	int index = 0;
	while (!ist && ++index < 30000) {
		usleep(1);
		ist.close();
		ist.open(fileName, ios::in);
	}
}

inline clock_t GetTickCount()
{
	struct timeval tv;
	gettimeofday(&tv, NULL);
	return (tv.tv_sec * 1000 + tv.tv_usec / 1000) * 1000;
}
inline void error_quit(const char* msg)
{
	perror(msg);
	exit(-1);
}
inline void change_path(const char* path)
{
	//printf("Leave %s Successed . . .\n", getcwd(NULL, 0));
	if (chdir(path) == -1)
		error_quit("chdir");
	//printf("Entry %s Successed . . .\n", getcwd(NULL, 0));
}
inline void rm_dir(const char* path)
{
	DIR* dir;
	struct dirent* dirp;
	struct stat  buf;
	char* p = getcwd(NULL, 0);
	if ((dir = opendir(path)) == NULL)
		error_quit("OpenDir");
	change_path(path);
	while (dirp = readdir(dir))
	{
		if ((strcmp(dirp->d_name, ".") == 0) || (strcmp(dirp->d_name, "..") == 0))
			continue;
		if (stat(dirp->d_name, &buf) == -1)
			error_quit("stat");
		if (S_ISDIR(buf.st_mode))
		{
			rm_dir(dirp->d_name);
			continue;
		}
		if (remove(dirp->d_name) == -1)
			error_quit("remove");
		//printf("rm %s Successed . . .\n", dirp->d_name);
	}
	closedir(dir);
	change_path(p);
	if (rmdir(path) == -1)
		error_quit("rmdir");
	//printf("rm %s Successed . . .\n", path);
}