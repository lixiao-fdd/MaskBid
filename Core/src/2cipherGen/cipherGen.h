#pragma once
#include "../global.h"
#include "../1commitment/commitment.h"
extern G_q G;               // group used for the Pedersen commitment
extern G_q H;               // group used for the the encryption
extern ElGamal El;         // The class for encryption and decryption
extern Network net;
extern bool vMode;
class CipherGen {
private:
	ifstream ist;
	ofstream ost;
	array<string, 2> codes;//自己和对方的编号，第一个是自己的，第二个是对方的
	string round;//当前轮数
	array<ZZ, 32> plaintext;//竞价二进制明文
	ZZ amount;//竞价十进制明文
	array<Cipher_elg, 32> ciphertext;    //密文
	array<Cipher_elg, 32> ciphertext_2;    //上一轮的密文
	array<ZZ, 32> ran_1;
	array<ZZ, 32> ran_2;
	Cipher_elg ciphertextZero;
	ZZ ranZero;
	//int plaintext_int;
	int cipherNum = 32;
	bool bigMe;
	ZZ mod;
	ZZ ord;
	Mod_p g;
	Mod_p h;
	Mod_p y;
	Mod_p y_1;
	SHA256 sha;

	//读取明文
	void readPlaintext();
	//生成密文并读取对方生成的密文
	void createCipher();

public:
	CipherGen(array<string, 2> codes, string round, bool bigMe);
	CipherGen(array<string, 2> codes);
	//读取明文并用私有公钥加密，保存密文
	void chainPrepare();
	//生成密文( h^r , g^m × y^r )
	void gen(array<Cipher_elg, 32>& Ciphertext, array<ZZ, 32>& Plaintext, ZZ& RanZero, array<ZZ, 32>& Ran);
	//生成证明
	void prove();
	void proveConsistency(string lastFinishRoundMe, string lastFinishRoundOp);
	//验证证明
	bool verify();
	bool verifyConsistency(string lastFinishRoundOp);
};