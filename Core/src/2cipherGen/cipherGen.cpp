#include "cipherGen.h"

CipherGen::CipherGen(array<string, 2> codes, string round, bool bigMe) :codes(codes), round(round), bigMe(bigMe) {
	SetSeed(to_ZZ((long)time(0) + (long)clock()));
	y = El.get_pk();
	mod = El.get_group().get_mod();
	ord = El.get_group().get_ord();
	g = El.get_group().get_g();
	h = El.get_group().get_gen();
}

CipherGen::CipherGen(array<string, 2> codes) :codes(codes) {
	SetSeed(to_ZZ((long)time(0) + (long)clock()));
	y = El.get_pk();
	mod = El.get_group().get_mod();
	ord = El.get_group().get_ord();
	g = El.get_group().get_g();
	h = El.get_group().get_gen();
}

//读取明文并用私有公钥加密，保存密文
void CipherGen::chainPrepare() {
	readPlaintext();
	//加密明文十进制金额，用于上链
	string fileName = filesPath + "cipherAmount" + codes[0] + ".txt";
	ost.open(fileName, ios::out);
	if (!ost) {
		cout << "[" << codes[0] << "] - " << "Can't create " << fileName << endl;
		exit(1);
	}
	Cipher_elg cipher_amount = El.encrypt(amount);
	ost << cipher_amount << endl;//输出密文
	ost.close();
	net.fSend(fileName);
}
//生成密文( h^r , g^m × y^r )
void CipherGen::gen(array<Cipher_elg, 32>& Ciphertext, array<ZZ, 32>& Plaintext, ZZ& RanZero, array<ZZ, 32>& Ran) {
	readPlaintext();
	createCipher();
	Ciphertext = ciphertext;
	Plaintext = plaintext;
	RanZero = ranZero;
	Ran = ran_1;
	//return ciphertext;
}
//读取明文
void CipherGen::readPlaintext() {
	int plaintext_int;
	bitset<32> plaintext_inv;
	//读取明文金额
	string fileName = filesPath + "plaintext_int" + codes[0] + ".txt";
	ist.open(fileName, ios::in);
	waitFile(fileName, ist);
	if (!ist) {
		cout << "[" << codes[0] << "] - " << "Can't open " << fileName << endl;
		exit(1);
	}
	ist >> plaintext_int;
	amount = ZZ(plaintext_int);
	cout << "[" << codes[0] << "] - " << "Amount: " << amount << endl;
	plaintext_inv = plaintext_int;
	ist.close();
	//fileName = "plaintext" + codes[0] + ".txt";
	//反转二进制值
	for (int i = 0; i < cipherNum; i++) {
		plaintext[i] = ZZ(plaintext_inv[cipherNum - i - 1]);
		//cout << plaintext[i];
	}
	//cout << endl;
}
//生成密文并读取对方生成的密文
void CipherGen::createCipher() {
	string fileName = filesPath + "ciphertext" + codes[0] + "-R" + round + ".txt";
	ost.open(fileName, ios::out);
	if (!ost) {
		cout << "[" << codes[0] << "] - " << "Can't create " << fileName << endl;
		exit(1);
	}
	stringstream ss;
	ost << El.get_pk() << endl;//输出主密钥
	ss << El.get_pk() << ";";
	for (int i = 0; i < cipherNum; i++)
	{
		ran_1[i] = RandomBnd(ord);								 //随机数r，也被称作临时密钥
		Cipher_elg temp = El.encrypt_g(ZZ(plaintext[i]), ran_1[i]);//得到(u,v)密文组，u = h^r，v = g^m×y^r，y为公钥
		ciphertext[i] = temp;									 //顺序读入
		ost << temp << endl;										 //输出密文
		ss << temp << ";";
	}
	//加密0
	ranZero = RandomBnd(ord);					  //随机数r
	ciphertextZero = El.encrypt_g(ZZ(0), ranZero);//得到(h^r,y^r)密文组
	ost << ciphertextZero << endl;
	ss << ciphertextZero << ";";
	ost.close();
	//读取对方生成的密文
	string cipher;
	//NOTE: 和java交互，先发送后接收
	string fileName1 = filesPath + "ciphertext" + codes[1] + "-R" + round + ".txt";
	if (bigMe) {
		net.fSend(fileName);
		net.fReceive(fileName1);
	}
	else {
		net.fReceive(fileName1);
		net.fSend(fileName);
	}
	/*
	vector<string> ciphertext_2_str;
	net.deserialization(cipher, ciphertext_2_str);
	fileName = filesPath + "ciphertext" + codes[1] + "-R" + round + ".txt";
	ost.open(fileName, ios::out);
	if (!ost)
	{
		cout << "[" << codes[0] << "] - " << "Can't create " << fileName << endl;
		exit(1);
	}
	for (int i = 0; i < ciphertext_2_str.size(); i++)
		ost << ciphertext_2_str[i] << endl;
	ost.close();*/
	//保存随机数，下一轮生成密文一致性证明使用
	fileName = filesPath + "ran" + codes[0] + "-R" + round + ".txt";
	ost.open(fileName, ios::out);
	if (!ost)
	{
		cout << "[" << codes[0] << "] - " << "Can't create " << fileName << endl;
		exit(1);
	}
	for (int i = 0; i < ran_1.size(); i++)
		ost << ran_1[i] << endl;
	ost.close();
}
//生成证明
void CipherGen::prove() {
	//生成证明
	string fileName = filesPath + "proveCipher" + codes[0] + "-R" + round + ".txt";
	Commitment com(codes, round, plaintext, ciphertext, ran_1, bigMe, fileName);
	com.cipherCommit();//生成本轮密文正确性证明
	//交换证明
	string fileName1 = filesPath + "proveCipher" + codes[1] + "-R" + round + ".txt";
	//NOTE: 和java交互，先发送后接收
	if (bigMe) {
		net.fSend(fileName);
		net.fReceive(fileName1);
	}
	else {
		net.fReceive(fileName1);
		net.fSend(fileName);
	}
}
//生成证明
void CipherGen::proveConsistency(string lastFinishRoundMe, string lastFinishRoundOp) {
	//生成密文一致性证明
	if (lastFinishRoundMe.compare("0") != 0) {
		//读入上一次参与竞标生成的密文
		string fileName = filesPath + "ciphertext" + codes[0] + "-R" + lastFinishRoundMe + ".txt";
		ist.open(fileName, ios::in);
		if (!ist) {
			cout << "[" << codes[0] << "] - " << "Can't open " << fileName << endl;
			exit(1);
		}
		string container;
		ist >> container;
		y_1.toModP(container, mod);
		for (int i = 0; i < cipherNum; i++) {
			ist >> ciphertext_2[i];
		}
		ist.close();
		//读入上一轮的随机数
		fileName = filesPath + "ran" + codes[0] + "-R" + lastFinishRoundMe + ".txt";
		ist.open(fileName, ios::in);
		if (!ist) {
			cout << "[" << codes[0] << "] - " << "Can't open " << fileName << endl;
			exit(1);
		}
		for (int i = 0; i < cipherNum; i++) {
			ist >> ran_2[i];
		}
		ist.close();
		//生成证明
		fileName = filesPath + "proveConsistency" + codes[0] + "-R" + round + ".txt";
		Commitment com2(codes, round, plaintext, ciphertext, ciphertext_2, ran_1, ran_2, y_1, bigMe, fileName);
		com2.ciphertextConsistencyCommit();
		//发送证明
		//NOTE: 和java交互，先发送后接收
		net.fSend(fileName);
	}
	if (lastFinishRoundOp.compare("0") != 0) {
		string fileName = filesPath + "ciphertext" + codes[1] + "-R" + lastFinishRoundOp + ".txt";
		net.fReceive(fileName);
		fileName = filesPath + "proveConsistency" + codes[1] + "-R" + round + ".txt";
		net.fReceive(fileName);
	}
}
//验证证明
bool CipherGen::verify() {
	int index = 0;
	if (!vMode)
		index = 1;

	bool flag = true;
	//读入密文
	string fileName = filesPath + "ciphertext" + codes[index] + "-R" + round + ".txt";
	ist.open(fileName, ios::in);
	waitFile(fileName, ist);
	if (!ist) {
		cout << "[" << codes[0] << "] - " << "Can't open " << fileName << endl;
		exit(1);
	}
	string container;
	ist >> container;
	y.toModP(container, H.get_mod());
	for (int i = 0; i < cipherNum; i++) {
		ist >> ciphertext[i];
	}
	ist.close();
	//读入证明
	fileName = filesPath + "proveCipher" + codes[index] + "-R" + round + ".txt";
	Commitment com(codes, round, ciphertext, bigMe, fileName);
	flag &= com.cipherCheck();
	return flag;
}
//验证证明
bool CipherGen::verifyConsistency(string lastFinishRoundOp) {
	//密文一致性验证
	int index = 0;
	if (!vMode)
		index = 1;

	bool flag = true;
	if (lastFinishRoundOp.compare("0") != 0) {
		//获取对方上一次参与竞标生成的密文
		string fileName = filesPath + "ciphertext" + codes[index] + "-R" + lastFinishRoundOp + ".txt";
		ist.open(fileName, ios::in);
		if (!ist) {
			cout << "[" << codes[0] << "] - " << "Can't open " << fileName << endl;
			exit(1);
		}
		string container;
		ist >> container;
		y_1.toModP(container, mod);
		for (int i = 0; i < cipherNum; i++) {
			ist >> ciphertext_2[i];
		}
		ist.close();
		//读入证明
		fileName = filesPath + "proveConsistency" + codes[index] + "-R" + round + ".txt";
		Commitment com2(codes, round, ciphertext, ciphertext_2, y, y_1, bigMe, fileName);
		flag &= com2.ciphertextConsistencyCheck();//生成本轮密文正确性证明
	}
	return flag;
}
