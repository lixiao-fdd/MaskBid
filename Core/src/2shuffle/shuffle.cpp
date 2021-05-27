#include "shuffle.h"

Shuffle::Shuffle(array< string, 2> codes, string round) :codes(codes), round(round) {
	mod = El.get_group().get_mod();
	ord = El.get_group().get_ord();
}
//创建Prover角色
void Shuffle::creatProver(bool bigMe) {
	this->bigMe = bigMe;
	cipher_in = new vector<vector<Cipher_elg>*>(m);  //输入的密文
	cipher_out = new vector<vector<Cipher_elg>*>(m);  //输出的密文
	string fileName;
	if (bigMe) {//大号用小号的shuffle结果作为输入
		//string shuffleResult;
		//NOTE: 和java交互，先发送后接收
		//net.mReceive(shuffleResult);
		//vector<string> shuffleResult_str;
		//net.deserialization(shuffleResult, shuffleResult_str);
		fileName = filesPath + "cipherSR" + codes[1] + "-R" + round + ".txt";//比较结果的密文
		net.fReceive(fileName);
		//ost.open(fileName, ios::out);
		//waitFile(fileName, ist);
		//if (!ist)
		//{
		//	cout << "[" << codes[0] << "] - " << "Can't open " << fileName << endl;
		//	exit(1);
		//}
		//for (int i = 0; i < 32; i++) {
		//	ost << shuffleResult_str[i] << endl;
		//}
		//ost.close();
	}
	else {//小号用大号的比较结果作为输入
		fileName = filesPath + "cipherCR" + codes[1] + "-R" + round + ".txt";//比较结果的密文
	}
	
	//读入密文
	readCipher(fileName, cipher_in);

}
//创建Verifier角色
void Shuffle::creatVerifier(bool bigMe) {
	this->bigMe = bigMe;
	cipher_in = new vector<vector<Cipher_elg>*>(m);  //输入的密文
	cipher_out = new vector<vector<Cipher_elg>*>(m);  //输出的密文
	int index = 1;
	if (!vMode)
		index = 0;
	//读取未shuffle的密文
	//作为第一轮shuffle者
	string fileName;
	if (!vMode) {
		if (bigMe) {//大号读取比较结果
			fileName = filesPath + "cipherCR" + codes[index] + "-R" + round + ".txt";//shuffe过一轮的密文
		}
		else {//小号读取混淆结果
			fileName = filesPath + "cipherSR" + codes[index] + "-R" + round + ".txt";//比较结果的密文
		}
	}
	else
	{
		if (bigMe) {//大号读取混淆结果
			fileName = filesPath + "cipherSR" + codes[index] + "-R" + round + ".txt";//shuffe过一轮的密文
		}
		else {//小号读取比较结果
			fileName = filesPath + "cipherCR" + codes[index] + "-R" + round + ".txt";//比较结果的密文
		}
	}
	readCipher(fileName, cipher_in);
	//读取shuffle过的密文
	fileName = filesPath + "cipherSR" + codes[!index] + "-R" + round + ".txt";
	readCipher(fileName, cipher_out);

}
//生成承诺
void Shuffle::prove() {
	clock_t tstart = GetTickCount();
	//生成证明
	vector<int> num = { m, n, omega_mulex, omega_sw, omega_LL, mu, m_r, mu_h };
	Prover_toom* P = new Prover_toom(cipher_out, R, pi, num, codes[0]);
	string fileName = filesPath + "proveShuffle" + codes[0] + "-R" + round + ".txt";
	P->prove(codes, fileName);
	delete P;
	//计时
	clock_t tstop = GetTickCount();
	double ttime = (tstop - tstart) / (double)CLOCKS_PER_SEC * 1000;
	cout << "[" << codes[0] << "] - " << "prove shuffle " << ttime << " ms" << endl;
	//交换证明
	string fileName1 = filesPath + "proveShuffle" + codes[1] + "-R" + round + ".txt";
	//NOTE: 和java交互，先发送后接收

	if (bigMe) {
		net.fReceive(fileName1);
		net.fSend(fileName);
	}
	else {
		net.fSend(fileName);
		net.fReceive(fileName1);
	}
}
//正确性验证
bool Shuffle::verify() {
	clock_t tstart = GetTickCount();
	int index = 0;
	if (!vMode)
		index = 1;
	vector<int> num = { m, n, omega_mulex, omega_sw, omega_LL, mu, m_r, mu_h };
	string fileName = filesPath + "proveShuffle" + codes[index] + "-R" + round + ".txt";
	Verifier_toom* V = new Verifier_toom(num);
	ans = V->verify(codes, fileName, cipher_in, cipher_out);
	delete V;

	clock_t tstop = GetTickCount();
	double ttime = (tstop - tstart) / (double)CLOCKS_PER_SEC * 1000;
	cout << "[" << codes[0] << "] - " << "verify shuffle " << ttime << " ms" << endl;
	return ans;
}
//读取文件中的密文，保存为16×2的矩阵形式
void Shuffle::readCipher(string fileName, vector<vector<Cipher_elg>*>* Cipher) {
	ist.close();
	ist.open(fileName, ios::in);
	waitFile(fileName, ist);
	if (!ist)
	{
		cout << "[" << codes[0] << "] - " << "Can't open " << fileName << endl;
		exit(1);
	}
	string in_temp, u_str, v_str;
	size_t pos_start, pos_mid, pos_end;
	for (int row = 0; row < m; row++) {
		vector<Cipher_elg>* r = new vector<Cipher_elg>(n);
		for (int col = 0; col < n; col++) {
			ist >> in_temp;
			pos_start = in_temp.find("(");
			pos_mid = in_temp.find(",");
			pos_end = in_temp.find(")");
			u_str = in_temp.substr(pos_start + 1, pos_mid - 1);
			v_str = in_temp.substr(pos_mid + 1, pos_end - pos_mid - 1);
			ZZ u, v;
			//cout << in_temp << " | (" << u_str << "," << v_str << ")\n";
			conv(u, u_str.c_str());
			conv(v, v_str.c_str());
			Cipher_elg Cipher_temp = Cipher_elg(u, v, H.get_mod());
			r->at(col) = Cipher_temp;
		}
		Cipher->at(row) = r;
	}
	ist.close();
}
//进行shuffle操作
void Shuffle::shuffle() {
	//clock_t tstart = GetTickCount();
	R = new vector<vector<ZZ>*>(m);
	pi = new vector<vector<vector<int>*>*>(m);
	perm_matrix(pi);//生成用于shuffle的向量pi，内容为32个整数
	randomEl(R);//生成用于重加密的随机数矩阵R，内容为32个随机数

	//使用pi和R对密文cipher_in进行重新加密，生成32个(u,v)密文组，并输出
	string fileName = filesPath + "cipherSR" + codes[0] + "-R" + round + ".txt";
	ost.open(fileName, ios::out);
	if (!ost)
	{
		cout << "[" << codes[0] << "] - " << "Can't create " << fileName << endl;
		exit(1);
	}
	stringstream ss;
	reencryptCipher(ss);
	ost.close();
	/*Functions::decryptCipher(c, num, 0);
	Functions::decryptCipher(C, num, 1);*/
	/*clock_t  tstop = GetTickCount();
	double ttime = (tstop - tstart) / (double)CLOCKS_PER_SEC * 1000;
	cout << "[" << codes[0] << "] - " << "shuffle " << ttime << " ms" << endl;*/

	string cipher_1, cipher_2;
	ss >> cipher_1;
	if (bigMe) {//大号将结果发送给小号
		//NOTE: 和java交互，先发送后接收
		//net.mSend(cipher_1);
		net.fSend(fileName);
	}
	else {//小号将结果发送给大号，并接收大号的结果
		//NOTE: 和java交互，先发送后接收
		net.fSend(fileName);
		fileName = filesPath + "cipherSR" + codes[1] + "-R" + round + ".txt";
		net.fReceive(fileName);
		/*net.mSend(cipher_1);
		net.mReceive(cipher_2);
		vector<string> ciphertext_2_str;
		net.deserialization(cipher_2, ciphertext_2_str);
		ost.open(fileName, ios::out);
		if (!ost)
		{
			cout << "[" << codes[0] << "] - " << "Can't create " << fileName << endl;
			exit(1);
		}
		for (int i = 0; i < 32; i++)
			ost << ciphertext_2_str[i] << endl;
		ost.close();*/
	}
}
//生成随机替换序列
void Shuffle::permutation(vector<int>* v, int N)
{
	int r, temp;
	//new seed for the randomness
	SetSeed(to_ZZ((unsigned int)time(0)));
	//vector containing the values 1 to N ordered
	for (int i = 0; i < N; i++)
	{
		v->at(i) = i + 1;
	}
	//create N times a random number <N, calculates r = i+r%N and switchs the values v[i] and v[r]
	for (int i = 0; i < N; i++)
	{//随机互换
		r = (RandomBnd(N) + i) % N;
		temp = v->at(i);
		v->at(i) = v->at(r);
		v->at(r) = temp;
	}
}
//生成随机替换矩阵
void Shuffle::perm_matrix(vector<vector<vector<int>* >* >* pi) {
	int i, j, k, t_1, t_2;
	vector<int>* v = new vector<int>(n * m);
	//生成随机替换序列
	permutation(v, n * m);
	for (i = 0; i < m; i++) {
		vector<vector<int>* >* r = new vector<vector<int>* >(n);
		for (j = 0; j < n; j++) {
			k = i * n + j;//v的index 
			t_1 = v->at(k) / n;//shuffle后的行
			t_2 = v->at(k) % n;//shuffle后的列
			if (t_2 == 0)
			{
				t_1 = t_1 - 1;
				t_2 = n - 1;
			}
			else
			{
				t_2 = t_2 - 1;
			}
			vector<int>* el = new vector<int>(2);
			el->at(0) = t_1;
			el->at(1) = t_2;
			r->at(j) = el;
		}
		pi->at(i) = r;
	}
	delete v;
}
//生成随机数矩阵
void Shuffle::randomEl(vector<vector<ZZ>*>* R)
{
	for (int i = 0; i < m; i++)
	{
		vector<ZZ>* r = new vector<ZZ>(n);
		for (int j = 0; j < n; j++)
		{
			r->at(j) = RandomBnd(ord);
		}
		R->at(i) = r;
	}
}
//重加密
void Shuffle::reencryptCipher(stringstream& ss) {
	for (int i = 0; i < m; i++)
	{
		vector<Cipher_elg>* r = new vector<Cipher_elg>(n);
		for (int j = 0; j < n; j++)
		{
			Cipher_elg temp = El.encrypt(1, R->at(i)->at(j));				//生成随机加密的密文1
			int row = pi->at(i)->at(j)->at(0);								//shuffle后需要移动的行
			int col = pi->at(i)->at(j)->at(1);								//shuffle后需要移动的列
			Cipher_elg::mult(r->at(j), temp, cipher_in->at(row)->at(col));  //同态乘法
			ost << r->at(j) << endl;
			ss << r->at(j) << ";";
		}
		cipher_out->at(i) = r;
	}
}
