#pragma once
#include "../global.h"
extern ElGamal El;         // The class for encryption and decryption
extern Network net;

class Commitment {
private:
	ifstream ist;
	ofstream ost;
	array<string, 2> codes;//自己和对方的编号，第一个是自己的，第二个是对方的
	string round;//当前轮数
	array<ZZ, 32> plaintext;//竞价二进制明文
	ZZ plaintext_s;
	array<ZZ, 32> plaintext_1;//自己的二进制明文
	array<ZZ, 32> plaintext_round2;
	array<Cipher_elg, 32> ciphertext;	   //参与commit的密文
	array<Cipher_elg, 32> ciphertext_1;    //自己的密文
	array<Cipher_elg, 32> ciphertext_2;    //对方的密文或上一轮自己的密文
	array<Cipher_elg, 32> ciphertext_round2;
	array<Cipher_elg, 32> ciphertext_round3;
	array<ZZ, 32> ran;
	array<ZZ, 32> ran_1;
	array<ZZ, 32> ran_2;//上一轮的随机数
	array<ZZ, 32> ran_round2;
	array<Mod_p, 32> base1;
	array<Mod_p, 32> base2;
	Cipher_elg cipherZero_1;//自己的0加密密文
	Cipher_elg cipherZero_2;//对方的0加密密文
	array<ZZ, 32> c2;
	array<ZZ, 32> dk;
	ZZ ranZero;
	int cipherNum = 32;
	ZZ mod;
	ZZ ord;
	Mod_p g;
	Mod_p h;
	Mod_p y;//主公钥
	Mod_p y_1;//上一轮的主公钥
	Mod_p y1;//个人公钥
	Mod_p x1;//私钥
	SHA256 sha;
	bool bigMe;
	string fileName;
	//sigma协议承诺
	void sigma();
	//sigma协议检验
	bool checkSigma();

	//表示证明
	void indicates();
	//表示证明检验
	bool indicatesCheck();

	//离散对数证明
	void discreteLogarithm(int flag);
	//离散对数证明检验
	bool discreteLogarithmCheck(int flag);

	//线性等式证明
	void linearEquation(int flag);
	//线性等式证明检验
	bool linearEquationCheck(int flag);

	//等式证明
	void equation();
	//线性等式证明检验
	bool equationCheck();

	//比较正确性前置证明
	void compareCommit0();
	//比较正确性前置证明验证
	bool compareCommitCheck0();

	//比较正确性第一轮证明
	void compareCommit1();
	//比较正确性第一轮证明验证
	bool compareCommitCheck1();

	//比较正确性第二轮证明
	void compareCommit2();
	//比较正确性第二轮证明验证
	bool compareCommitCheck2();

	//比较正确性第三轮证明
	void compareCommit3();
	//比较正确性第三轮证明验证
	bool compareCommitCheck3();

	//比较正确性第四轮证明
	void compareCommit4();
	//比较正确性第四轮证明验证
	bool compareCommitCheck4();

	//比较正确性第五轮证明
	void compareCommit5();
	//比较正确性第五轮证明验证
	bool compareCommitCheck5();


public:
	//密文一致性证明
	Commitment(array<string, 2> codes, string round, array<ZZ, 32> plaintext, array<Cipher_elg, 32> ciphertext_1, array<Cipher_elg, 32> ciphertext_2, array<ZZ, 32> ran_1, array<ZZ, 32> ran_2, Mod_p y_1, bool bigMe, string fileName);
	//加密正确性证明
	Commitment(array<string, 2> codes, string round, array<ZZ, 32> plaintext, array<Cipher_elg, 32> ciphertext, array<ZZ, 32> ran, bool bigMe, string fileName);
	//比较正确性证明
	Commitment(array<string, 2> codes, string round, array<ZZ, 32> plaintext_1, array<Cipher_elg, 32> ciphertext_1, array<Cipher_elg, 32> ciphertext_2, array<ZZ, 32> ran_1, Cipher_elg cipherZero_1, Cipher_elg cipherZero_2, ZZ ranZero, bool bigMe, string fileName);
	//解密正确性证明
	Commitment(array<string, 2> codes, string round, array<ZZ, 32> c2, array<ZZ, 32> dk, bool bigMe, string fileName);
	//验证
	Commitment(array<string, 2> codes, string round, array<Cipher_elg, 32> ciphertext, bool bigMe, string fileName);
	//验证
	Commitment(array<string, 2> codes, string round, array<Cipher_elg, 32> ciphertext_1, array<Cipher_elg, 32> ciphertext_2, Mod_p y, Mod_p y_1, bool bigMe, string fileName);
	//解密正确性验证
	Commitment(array<string, 2> codes, string round, array<Cipher_elg, 32> ciphertext, array<ZZ, 32> dk, Mod_p y1, bool bigMe, string fileName);
	//比较正确性证明
	void compareCommit();
	//比较正确性证明验证
	bool compareCheck(Cipher_elg cipherZero);

	void cipherCommit();
	bool cipherCheck();

	void decryptCommit();
	bool decryptCheck();

	void ciphertextConsistencyCommit();
	bool ciphertextConsistencyCheck();

};