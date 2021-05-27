#pragma once
#include "../2paraGen/paraGen.h"
#include "../2cipherGen/cipherGen.h"
#include "../2compare/compare.h"
#include "../2shuffle/shuffle.h"
#include "../2decrypt/decrypt.h"
extern G_q G;               // group used for the Pedersen commitment
extern G_q H;               // group used for the the encryption
extern ElGamal El;         // The class for encryption and decryption
extern Network net;
extern bool vMode;
class SBid {
private:
	ifstream ist;
	ofstream ost;
	array<string, 2> codes;//自己和对方的编号，第一个是自己的，第二个是对方的
	string round;//当前轮数
	string codeBig, codeSmall;
	string pkFileName;
	array<ZZ, 32> plaintext;//竞价二进制明文
	array<Cipher_elg, 32> ciphertext;    //密文
	array<ZZ, 32> ran_1;//加密的随机数
	ZZ ranZero;
	string ans[2] = { "FAIL","PASS" };
	bool bigMe;
	ZZ mod;
	ZZ ord;
	ZZ gen_h;
	ZZ gen_g;
	ZZ sk, pk;
	int cipherNum = 32;
	int pBits = 100;
	int qBits = 90;
	string lastFinishRoundMe = "0";//上一次参与竞标的轮数
	string lastFinishRoundOp = "0";
	int strategyFlag = -1;

	//读取群的参数并生成群
	void readParameters();
	//读取ElGamal公私钥
	int readElGamal();
	//将生成的公钥传递给对方
	void pkExchange();
	//加密并生成证明
	void ciphertextOp();
	//验证加密
	bool ciphertextVerify();
	//比较并生成证明
	void compareOp();
	//验证比较
	bool compareVerify();
	//混淆并生成证明
	void shuffleOp();
	//验证混淆
	bool shuffleVerify();
	//解密并生成证明
	void decryptOp();
	//验证解密
	bool decryptVerify();
	/*void consistencyOp();
	bool consistencyVerify();*/

public:
	//生成参数
	void parametersGen();
	//生成密钥及密文用于链上注册
	void registration(string code);
	//生成ElGamal公私钥
	int creatElGamal();
	//竞拍准备操作
	void prepare(array<int, 6> codes_in);
	//开始竞标
	void bid();
	//验证
	void verify();
	//解密密文
	void decrypt(array<string, 3> paras);
	//接收证明文件压缩包并解压为独立的文件
	void unzip(array<string, 3> paras);
};