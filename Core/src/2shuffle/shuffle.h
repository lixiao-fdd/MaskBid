#pragma once
#include "../global.h"
#include "Functions.h"
#include "Prover_toom.h"
#include "Verifier_toom.h"

extern G_q G;               // group used for the Pedersen commitment
extern G_q H;              // group used for the the encryption
extern ElGamal El;         // The class for encryption and decryption
extern Pedersen Ped;        // Object which calculates the commitments
extern Network net;

class Shuffle {
private:
	ifstream ist;
	ofstream ost;
	vector<vector<Cipher_elg>*>* cipher_in;    //原始输入的密文
	vector<vector<Cipher_elg>*>* cipher_out;   //重加密的密文
	vector<vector<vector<int>*>*>* pi;        //Permutation，用于shuffle
	vector<vector<ZZ>*>* R;			           //用于重加密的随机数
	string codeName;
	array< string, 2> codes;
	string round;//当前轮数
	int mu = 4;                      // number of rows after reduction
	int m_r = 4;                     // number of rows after reduction
	int mu_h = 7;					  // 2*mu-1, number of extra elements in the reduction
	int omega_mulex = 7;			  //windowsize for sliding-window technique
	int omega_sw = 6;				  //windowsize for multi-expo technique
	int omega_LL = 5;				  //windowsize for multi-expo technique
	int m = 16;//行数
	int n = 2;//列数
	int ans = 0;//验证结果
	bool bigMe;
	ZZ mod;
	ZZ ord;

	//读取群的参数并生成群
	//void readParameters();
	//设置ElGamal公私钥
	//void creatElGamal();
	//读取文件中的密文，保存为16×2的矩阵形式
	void readCipher(string fileName, vector<vector<Cipher_elg>*>* Cipher);
	//生成随机替换序列
	void permutation(vector<int>* v, int N);
	//生成随机替换矩阵
	void perm_matrix(vector<vector<vector<int>* >* >* pi);
	//生成随机数矩阵
	void randomEl(vector<vector<ZZ>*>* R);
	//重加密
	void reencryptCipher(stringstream& ss);
public:
	Shuffle(array< string, 2> codes, string round);
	//创建Prover角色
	void creatProver(bool bigMe);
	//创建Verifier角色
	void creatVerifier(bool bigMe);
	//进行shuffle操作
	void shuffle();
	//生成承诺
	void prove();
	//正确性验证
	bool verify();
};

