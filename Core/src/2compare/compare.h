#pragma once
#include "../global.h"
#include "../1commitment/commitment.h"
extern G_q G;               // group used for the Pedersen commitment
extern G_q H;               // group used for the the encryption
extern ElGamal El;         // The class for encryption and decryption
extern Network net;

class Compare {
private:
	ifstream ist;
	ofstream ost;
	array<string, 2> codes;//自己和对方的编号，第一个是自己的，第二个是对方的
	string round;//当前轮数
	array<ZZ, 32> plaintext;//竞价二进制明文
	array<Cipher_elg, 32> ciphertext;    //密文
	array<Cipher_elg, 32> ciphertext_2;  //对方的密文
	array<Cipher_elg, 33> Wj;
	array<Cipher_elg, 32> compareResults;//比较结果密文
	array<ZZ, 32> ran_1;
	Cipher_elg cipherZero;
	Cipher_elg cipherZero_2;
	ZZ ranZero;
	int cipherNum = 32;
	bool bigMe;
	ZZ mod;
	ZZ ord;
	Mod_p g;
	Mod_p h;
	Mod_p y;
	SHA256 sha;
	//读取对方的密文
	void readCipher();
	//从高到低逐位比较
	void cmp();

public:
	Compare(array<string, 2> codes, string round, array<ZZ, 32> plaintext, array<Cipher_elg, 32> ciphertext, array<ZZ, 32> ran_1, ZZ ranZero, bool bigMe);
	Compare(array<string, 2> codes, string round, array<Cipher_elg, 32> ciphertext, bool bigMe);
	//比较
	void compare();
	//生成证明
	void prove();
	//验证证明
	bool verify();
};