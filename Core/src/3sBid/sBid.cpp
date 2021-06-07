#include "sBid.h"

//生成参数
void SBid::parametersGen() {
	int port = 17000;
	net.start(port);
	net.acceptConnect();
	//创建文件夹
	filesPath = "./parameters/";
	if (access(filesPath.c_str(), 0))
		mkdir(filesPath.c_str(), S_IRWXU | S_IRWXG | S_IRWXO);
	ParaGen paraGen;
	cout << "Ready to generate parameters" << endl;
	while (paraGen.parametersGen(pBits, qBits));
	cout << "\nAlready generated parameters" << endl;
	net.fSend(filesPath + "parameters.txt");
}
//生成密钥及密文用于链上注册
void SBid::registration(string code) {
	codes[0] = code;
	int port = 17000;
	port += stoi(codes[0]);
	cout << "[" << codes[0] << "] - port: " << port << endl;
	net.start(port);
	net.acceptConnect();
	readParameters();
	string fileName = filesPath + "plaintext_int" + codes[0] + ".txt";
	net.fReceive(fileName);
	creatElGamal();
	CipherGen cipherGen(codes);
	cipherGen.chainPrepare();//用个人公钥加密的密文( h^r , m×y_1^r )
}
//竞拍准备操作
void SBid::prepare(array<int, 6> codes_in) {
	codes[0] = to_string(codes_in[0]);//自己的index
	codes[1] = to_string(codes_in[1]);//对方的index
	round = to_string(codes_in[2]);//轮数
	codeBig = (stoi(codes[0]) > stoi(codes[1])) ? codes[0] : codes[1];
	codeSmall = (stoi(codes[0]) < stoi(codes[1])) ? codes[0] : codes[1];
	bigMe = codes_in[0] > codes_in[1];
	lastFinishRoundMe = to_string(codes_in[3]);
	lastFinishRoundOp = to_string(codes_in[4]);
	strategyFlag = codes_in[5];
	cout << "[" << codes[0] << "] - No." << codes[0] << " vs No." << codes[1] << " - Round: " << round << endl;

	int port = 17000;
	port += stoi(codes[0]);
	cout << "[" << codes[0] << "] - port: " << port << endl;
	net.start(port);
	net.acceptConnect();
	readParameters();
}

//开始竞标
void SBid::bid() {
	//creatElGamal();
	readElGamal();
	pkExchange();
	ciphertextOp();
	compareOp();
	shuffleOp();
	decryptOp();
}
//验证
void SBid::verify() {
	cout << "[" << codes[0] << "] - " << "===========Verify===========" << endl;
	bool flag = true;
	flag &= ciphertextVerify();
	//cout << "ciphertextVerify OK" << endl;
	flag &= compareVerify();
	//cout << "compareVerify OK" << endl;
	flag &= shuffleVerify();
	//cout << "shuffleVerify OK" << endl;
	flag &= decryptVerify();
	//cout << "decryptVerify OK" << endl;
	cout << "[" << codes[0] << "] - " << "Verify results: " << ans[flag] << endl;
	cout << "[" << codes[0] << "] - " << "============OVER============" << endl;
	string fileName = filesPath + "verify-R" + round + ".txt";
	ost.open(fileName, ios::out);
	if (!ost)
	{
		cout << "[" << codes[0] << "] - " << "Can't create " << fileName << endl;
		exit(1);
	}
	ost << ans[flag] << endl;
	ost.close();
	net.fSend(fileName);
}
//读取群的参数并生成群
void SBid::readParameters() {
	filesPath = "./files_" + codes[0] + "/";
	if (access(filesPath.c_str(), 0)) {
		mkdir(filesPath.c_str(), S_IRWXU | S_IRWXG | S_IRWXO);
		cout << "[" << codes[0] << "] - " << "created: " << filesPath << endl;
	}
	string logPath = "./log";
	if (access(logPath.c_str(), 0)) {
		mkdir(logPath.c_str(), S_IRWXU | S_IRWXG | S_IRWXO);
		cout << "[" << codes[0] << "] - " << "created: " << filesPath << endl;
	}
	string fileName = filesPath + "parameters.txt";
	ist.open(fileName, ios::in);
	if (!ist) {
		net.fReceive(fileName);
		ist.open(fileName, ios::in);
	}
	//waitFile(fileName, ist);
	if (!ist)
	{
		cout << "[" << codes[0] << "] - " << "Can't open " << fileName << endl;
		exit(1);
	}
	ist >> mod;
	ist >> ord;
	ist >> gen_h;
	ist >> gen_g;
	ist.close();
	H = G_q(gen_h, ord, mod); //生成元h 阶数q 模数p
	G = G_q(gen_h, ord, mod);
	H.set_g(gen_g);
	G.set_g(gen_g);
}
//生成ElGamal公私钥
int SBid::creatElGamal() {
	El.set_group(H);
	string fileName = filesPath + "pk" + codes[0] + ".txt";
	ist.open(fileName, ios::in);
	if (ist)
	{//公钥存在
		net.fSend(fileName);
		ZZ pk, sk;
		ist >> pk;
		ist.close();
		fileName = filesPath + "sk" + codes[0] + ".txt";
		ist.open(fileName, ios::in);
		if (ist)
		{//私钥存在
			ist >> sk;
			cout << "[" << codes[0] << "] - " << "The key already exists " << fileName << endl;
			ist.close();
			El.set_key(sk, pk);
			return 1;
		}
	}
	ist.close();
	//生成公私钥
	cout << "[" << codes[0] << "] - A new key will be generated randomly" << endl;
	ZZ x = RandomBnd(H.get_ord());//随机生成私钥
	El.set_sk(x);//生成公钥
	//输出公私钥
	ost.open(fileName, ios::out);
	if (!ost)
	{
		cout << "[" << codes[0] << "] - " << "Can't create " << fileName << endl;
		exit(1);
	}
	ost << El.get_pk_1() << endl;
	ost.close();
	net.fSend(fileName);
	fileName = filesPath + "sk" + codes[0] + ".txt";
	ost.open(fileName, ios::out);
	if (!ost)
	{
		cout << "[" << codes[0] << "] - " << "Can't create " << fileName << endl;
		exit(1);
	}
	ost << El.get_sk() << endl;
	ost.close();
	net.fSend(fileName);
	return 0;
}
//读取ElGamal公私钥
int SBid::readElGamal() {
	//读取公私钥
	El.set_group(H);
	string fileName = filesPath + "pk" + codes[0] + ".txt";
	ist.open(fileName, ios::in);
	if (!ist)
	{
		cout << "[" << codes[0] << "] - " << "Can't open " << fileName << endl;
		exit(1);
	}
	string sk_str, pk_str;
	getline(ist, pk_str);
	conv(pk, pk_str.c_str());
	ist.close();
	fileName = filesPath + "sk" + codes[0] + ".txt";
	ist.open(fileName, ios::in);
	if (!ist)
	{
		cout << "[" << codes[0] << "] - " << "Can't open " << fileName << endl;
		exit(1);
	}
	getline(ist, sk_str);
	conv(sk, sk_str.c_str());
	El.set_key(sk, pk);
	ist.close();
	return 0;
}
//将生成的公钥传递给对方
void SBid::pkExchange() {
	string fileName = filesPath + "pk" + codes[0] + ".txt";
	string fileName1 = filesPath + "pk" + codes[1] + ".txt";

	net.fReceive(fileName1);

	ist.open(fileName1, ios::in);
	waitFile(fileName1, ist);
	if (!ist)
	{
		cout << "[" << codes[0] << "] - " << "Can't open " << fileName1 << endl;
		exit(1);
	}
	string pk_2;
	ist >> pk_2;
	ist.close();

	//cout << "pk_1: " << El.get_pk_1() << " | pk_2: " << pk_2 << endl;
	//生成主公钥
	El.keyGen(pk_2);
}
//加密并生成证明
void SBid::ciphertextOp() {
	
	CipherGen cipherGen(codes, round, bigMe);
	cipherGen.gen(ciphertext, plaintext, ranZero, ran_1);//生成密文( h^r , g^m × y^r )

	cipherGen.prove();//生成密文证明
	cipherGen.proveConsistency(lastFinishRoundMe, lastFinishRoundOp);
}
//验证加密
bool SBid::ciphertextVerify() {
	CipherGen cipherVerify(codes, round, bigMe);
	bool flag = cipherVerify.verify();
	flag &= cipherVerify.verifyConsistency(lastFinishRoundOp);
	return flag;
}
//比较并生成证明
void SBid::compareOp() {
	
	Compare compare(codes, round, plaintext, ciphertext, ran_1, ranZero, bigMe);
	compare.compare();
	
	compare.prove();
}
//验证比较
bool SBid::compareVerify() {
	Compare compare(codes, round, ciphertext, bigMe);
	bool flag = compare.verify();
	if ((bigMe && !vMode) || (!bigMe && vMode))//大号参与者以及小号验证者则跳过
		return true;
	else
		return flag;
}
//混淆并生成证明
void SBid::shuffleOp() {
	
	Shuffle prover(codes, round);
	prover.creatProver(bigMe);
	prover.shuffle();
	
	prover.prove();
}
//验证混淆
bool SBid::shuffleVerify() {
	Shuffle verifier(codes, round);
	verifier.creatVerifier(bigMe);
	bool flag = verifier.verify();
	return flag;
}
//解密并生成证明
void SBid::decryptOp() {
	Decrypt decrypt(codes, round, codeBig, codeSmall, bigMe);
	int ans = decrypt.decrypt();
	decrypt.prove();
	string fileName = filesPath + "ans" + codes[0] + "-R" + round + ".txt";
	ost.open(fileName, ios::out);
	if (!ost)
	{
		cout << "Can't create " << fileName << endl;
		exit(1);
	}
	/*cout << "ans: " << ans << endl;
	cout << "strategy: " << strategyFlag << endl;
	cout << "codeBig: " << codeBig << endl;
	cout << "codeSmall: " << codeSmall << endl;*/
	ost << "No." << codes[0] << "_vs_No." << codes[1] << "_Round_" << round << endl;
	if (strategyFlag == 1) {//大的赢
		switch (ans)
		{
			case 0://小号大于大号
				cout << "[" << codes[0] << "] - Winner No." << codeSmall << endl;
				ost << (bigMe ? "LOSE" : "WIN") << endl;
				break;
			case 1:
				cout << "[" << codes[0] << "] - - Winner DRAW" << endl;
				ost << "DRAW" << endl;
				break;
			case 2://小号小于大号
				cout << "[" << codes[0] << "] - Winner No." << codeBig << endl;
				ost << (bigMe ? "WIN" : "LOSE") << endl;
				break;
			default:
				break;
		}
	}
	else {//小的赢
		switch (ans)
		{
			case 0://大号小于小号
				cout << "[" << codes[0] << "] - Winner No." << codeBig << endl;
				ost << (bigMe ? "WIN" : "LOSE") << endl;
				break;
			case 1:
				cout << "[" << codes[0] << "] - - Winner DRAW" << endl;
				ost << "DRAW" << endl;
				break;
			case 2://大号大于小号
				cout << "[" << codes[0] << "] - Winner No." << codeSmall << endl;
				ost << (bigMe ? "LOSE" : "WIN") << endl;
				break;
			default:
				break;
		}
	}

	ost.close();
	net.fSend(fileName);
}
//验证解密
bool SBid::decryptVerify() {
	Decrypt decrypt(codes, round, codeBig, codeSmall, bigMe);
	bool flag = decrypt.verify();
	return flag;
}
//解密密文
void SBid::decrypt(array<string, 3> paras) {
	int port = 17000;
	cout << "[" << codes[0] << "] - port: " << port << endl;
	net.start(port);
	net.acceptConnect();
	clock_t cStart = GetTickCount();
	Cipher_elg cipher_amount;
	stringstream ss;
	ss << paras[0];//cipherAmount
	ss >> cipher_amount;
	string skDecrypt = paras[1];//sk
	string modDecrypt = paras[2];//mod
	ZZ mod, sk;
	conv(sk, skDecrypt.c_str());
	conv(mod, modDecrypt.c_str());

	cout << "cipher_amount: " << cipher_amount << endl;
	cout << "sk: " << sk << endl;
	cout << "mod: " << mod << endl;
	if (sk == 0)
		cout << "can not decrypt, need secret key" << endl;
	Mod_p ans;
	ZZ temp;
	temp = InvMod(cipher_amount.get_u(), mod);
	temp = PowerMod(temp, sk, mod);
	temp = MulMod(temp, cipher_amount.get_v(), mod);
	string fileName = "./plaintextAmount.txt";
	ost.open(fileName, ios::out);
	if (!ost)
	{
		cout << "Can't create " << fileName << endl;
		exit(1);
	}
	ost << temp << endl;
	ost.close();
	net.fSend(fileName);
	cout << "decrypt done" << endl;
	clock_t cEnd = GetTickCount();
	double cTime = (cEnd - cStart) / (double)CLOCKS_PER_SEC * 1000;
	cout  << "decrypt " << cTime << " ms" << endl;
}
//接收证明文件压缩包并解压为独立的文件
void SBid::unzip(array<string, 3> paras) {
	codes[0] = paras[0];//自己的index
	//codes[1] = paras[1];//对方的index
	round = paras[1];//轮数
	lastFinishRoundOp = paras[2];
	int port = 17000;
	port += stoi(codes[0]);
	cout << "[" << codes[0] << "] - port: " << port << endl;
	net.start(port);
	net.acceptConnect();
	//创建文件夹
	filesPath = "./verify_" + codes[0] + "/";
	if (access(filesPath.c_str(), 0))
		mkdir(filesPath.c_str(), S_IRWXU | S_IRWXG | S_IRWXO);

	string logPath = "./log";
	if (access(logPath.c_str(), 0))
		mkdir(logPath.c_str(), S_IRWXU | S_IRWXG | S_IRWXO);
	//接收parameters.txt
	string fileName = filesPath + "parameters.txt";
	net.fReceive(fileName);
	ist.open(fileName, ios::in);
	if (!ist)
	{
		cout << "[" << codes[0] << "] - " << "Can't open " << fileName << endl;
		exit(1);
	}
	ist >> mod;
	ist >> ord;
	ist >> gen_h;
	ist >> gen_g;
	ist.close();
	H = G_q(gen_h, ord, mod); //生成元h 阶数q 模数p
	G = G_q(gen_h, ord, mod);
	H.set_g(gen_g);
	G.set_g(gen_g);
	El.set_group(H);
	//接收fileZip
	fileName = filesPath + "fileZip.txt";
	net.fReceive(fileName);
	ist.open(fileName, ios::in);
	if (!ist)
	{
		cout << "[" << codes[0] << "] - " << "Can't open " << fileName << endl;
		exit(1);
	}
	string temp, fileZip;
	while (ist >> temp) {
		fileZip += (temp + "\n");
	}
	ist.close();
	//解压fileZip
	size_t pos_start = 0, pos_end = 0;
	string fileNames, fileContent;
	while ((pos_end = fileZip.find("@", pos_end)) != string::npos) {
		string fileFull = fileZip.substr(pos_start, pos_end - pos_start);
		size_t file_start = fileFull.find("#");
		fileNames = fileFull.substr(0, file_start);
		fileContent = fileFull.substr(file_start + 1, fileFull.length() - file_start - 1);
		//输出
		ost.open(filesPath + fileNames, ios::out);
		if (!ost)
		{
			cout << "[" << codes[0] << "] - " << "Can't create " << fileNames << endl;
			exit(1);
		}
		ost << fileContent << endl;
		ost.close();
		pos_start = ++pos_end;
	}
	cout << "Unzip " << fileName << " success" << endl;
	fileName = filesPath + "opponentIndex-R" + round + ".txt";
	ist.open(fileName, ios::in);
	if (!ist)
	{
		cout << "[" << codes[0] << "] - " << "Can't open " << fileName << endl;
		exit(1);
	}
	ist >> codes[1];
	ist.close();
	bigMe = stoi(codes[0]) > stoi(codes[1]);
	codeBig = (stoi(codes[0]) > stoi(codes[1])) ? codes[0] : codes[1];
	codeSmall = (stoi(codes[0]) < stoi(codes[1])) ? codes[0] : codes[1];
}