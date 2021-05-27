#pragma once
#include "../global.h"
#include "../1commitment/commitment.h"
extern G_q G;               // group used for the Pedersen commitment
extern G_q H;               // group used for the the encryption
extern ElGamal El;         // The class for encryption and decryption
extern Network net;

class Decrypt {
private:
	ifstream ist;
	ofstream ost;
	array<string, 2> codes;//自己和对方的编号，第一个是自己的，第二个是对方的
	string round;//当前轮数
	string codeBig, codeSmall;
	array<Cipher_elg, 32> ciphertext;	 //经过两轮混淆的密文
	array<ZZ, 32> dk_1;    //自己的解密份额
	array<ZZ, 32> dk_2;  //对方的解密份额
	array<ZZ, 32> c2;
	string ans[2] = { "FAIL","PASS" };
	int cipherNum = 32;
	bool bigMe;
	ZZ mod;
	ZZ ord;
	ZZ sk;
	Mod_p g;
	Mod_p h;
	Mod_p y;
	Mod_p pk;

	//读取经过两轮混淆的密文
	void readCipherShuffled();
	//创建解密份额
	void createDk();
	//读取对方的解密份额
	void readDk();
	//输出结果
	int outputAns();
public:
	Decrypt(array<string, 2> codes, string round, string codeBig, string codeSmall, bool bigMe);
	int decrypt();
	void prove();
	bool verify();
};