#include "compare.h"

Compare::Compare(array<string, 2> codes, string round, array<ZZ, 32> plaintext, array<Cipher_elg, 32> ciphertext, array<ZZ, 32> ran_1, ZZ ranZero, bool bigMe) :codes(codes), round(round), plaintext(plaintext), ciphertext(ciphertext), ran_1(ran_1), ranZero(ranZero), bigMe(bigMe) {
	SetSeed(to_ZZ((long)time(0) + (long)clock()));
	y = El.get_pk();
	mod = El.get_group().get_mod();
	ord = El.get_group().get_ord();
	g = El.get_group().get_g();
	h = El.get_group().get_gen();
}
Compare::Compare(array<string, 2> codes, string round, array<Cipher_elg, 32> ciphertext, bool bigMe) :codes(codes), round(round), ciphertext(ciphertext), bigMe(bigMe) {
	//SetSeed(to_ZZ((long)time(0) + (long)clock()));
	y = El.get_pk();
	mod = El.get_group().get_mod();
	ord = El.get_group().get_ord();
	g = El.get_group().get_g();
	h = El.get_group().get_gen();
}
//比较
void Compare::compare() {
	readCipher();
	cmp();
}
//读取对方的密文
void Compare::readCipher() {
	string fileName = filesPath + "ciphertext" + codes[1] + "-R" + round + ".txt";
	ist.open(fileName, ios::in);
	waitFile(fileName, ist);
	if (!ist) {
		cout << "[" << codes[0] << "] - " << "Can't open " << fileName << endl;
		exit(1);
	}
	string temp;
	ist >> temp;
	for (int i = 0; i < cipherNum; i++)
		ist >> ciphertext_2[i];
	ist >> cipherZero_2;//读取对方的0密文
	ist.close();
	fileName = filesPath + "ciphertext" + codes[0] + "-R" + round + ".txt";
	ist.open(fileName, ios::in);
	waitFile(fileName, ist);
	if (!ist) {
		cout << "[" << codes[0] << "] - " << "Can't open " << fileName << endl;
		exit(1);
	}
	ist >> temp;
	for (int i = 0; i < cipherNum; i++)
		ist >> temp;
	ist >> cipherZero;//读取自己的0密文
	ist.close();
}
//从高到低逐位比较
void Compare::cmp() {
	if (bigMe) {//大号进行比较操作，并将结果发送给小号
		//clock_t tstart = clock();
		string fileName = filesPath + "cipherCR" + codes[0] + "-R" + round + ".txt";
		ost.open(fileName, ios::out);
		if (!ost)
		{
			cout << "[" << codes[0] << "] - " << "Can't create " << fileName << endl;
			exit(1);
		}
		Cipher_elg a, b, aPb, aTb, twoTaTb, minus2TaTb, b_minus, aMbM1;
		ZZ r = RandomBnd(ord);
		Cipher_elg ONE = El.encrypt_g(ZZ(1), r);//g^0
		r = RandomBnd(ord);
		Wj[0] = El.encrypt_g(ZZ(0), r);//g^0
		Cipher_elg Wj_sum = Wj[0];
		stringstream ss;

		/*net.fSend(filesPath + "sk" + codes[0] + ".txt");
		net.fReceive(filesPath + "sk" + codes[1] + ".txt");
		ist.open(filesPath + "sk" + codes[1] + ".txt", ios::in);
		ZZ sk2;
		ist >> sk2;
		ZZ sks = AddMod(El.get_sk(), sk2, El.get_group().get_mod());
		cout << "sk: " << sks << endl;
		El.sk_main_debug = sks;
		array<ZZ, 32> ansA;
		array<ZZ, 32> ansB;
		array<ZZ, 32> ansAPb;*/

		for (int i = 0; i < cipherNum; i++) {
			a = ciphertext[i];
			b = ciphertext_2[i];
			//ansA[i] = El.decrypt_debug(a);//
			//ansB[i] = El.decrypt_debug(b);//
			aPb = a * b;//a+b

			aTb = Cipher_elg::expo(b, ZZ(plaintext[i]));//a*b 明文参与
			twoTaTb = Cipher_elg::expo(aTb, ZZ(2));//2*a*b
			minus2TaTb = Cipher_elg::inverse(twoTaTb);//-2*a*b
			Wj[i + 1] = aPb * minus2TaTb;//a+b-2*a*b
			Wj_sum = Wj_sum * Wj[i];
			b_minus = Cipher_elg::inverse(b);//-b
			aMbM1 = a * b_minus * ONE;//a-b+1
			compareResults[i] = aMbM1 * Wj_sum;
			//ansAPb[i] = El.decrypt_debug(compareResults[i]);//
			ost << compareResults[i] << endl;
			ss << compareResults[i] << ";";
		}
		ost.close();
		/*for (int i = 0; i < cipherNum; i++)
			cout << ansA[i] << " ";
		cout << endl;
		for (int i = 0; i < cipherNum; i++)
			cout << ansB[i] << " ";
		cout << endl;
		for (int i = 0; i < cipherNum; i++)
			cout << ansAPb[i] << " ";
		cout << endl;*/
		/*clock_t  tstop = clock();
		double ttime = (tstop - tstart) / (double)CLOCKS_PER_SEC * 1000;
		cout << "[" << codes[0] << "] - " << "compare " << ttime << " ms" << endl;*/
		string CR;
		ss >> CR;
		//NOTE: 和java交互，先发送后接收
		net.fSend(fileName);
	}
	else {//小号接收比较结果
		//string CR;
		//NOTE: 和java交互，先发送后接收
		/*net.mReceive(CR);
		vector<string> CR_str;
		net.deserialization(CR, CR_str);
		string fileName = filesPath + "cipherCR" + codes[1] + "-R" + round + ".txt";
		ost.open(fileName, ios::out);
		if (!ost)
		{
			cout << "[" << codes[0] << "] - " << "Can't create " << fileName << endl;
			exit(1);
		}
		for (int i = 0; i < cipherNum; i++)
			ost << CR_str[i] << endl;
		ost.close();*/

		/*net.fReceive(filesPath + "sk" + codes[1] + ".txt");
		net.fSend(filesPath + "sk" + codes[0] + ".txt");*/

		string fileName = filesPath + "cipherCR" + codes[1] + "-R" + round + ".txt";
		net.fReceive(fileName);
	}
}
//生成证明
void Compare::prove() {
	if (bigMe) {
		//生成证明
		string fileName = filesPath + "proveCompare" + codes[0] + "-R" + round + ".txt";
		Commitment com(codes, round, plaintext, ciphertext, ciphertext_2, ran_1, cipherZero, cipherZero_2, ranZero, bigMe, fileName);
		com.compareCommit();
		//发送证明
		//NOTE: 和java交互，先发送后接收
		net.fSend(fileName);
	}
	else {
		//接收证明
		string fileName1 = filesPath + "proveCompare" + codes[1] + "-R" + round + ".txt";
		//NOTE: 和java交互，先发送后接收
		net.fReceive(fileName1);
	}
}
//验证证明
bool Compare::verify() {
	int index = 0;
	if (!vMode)
		index = 1;
	bool flag = true;
	if ((bigMe && !vMode) || (!bigMe && vMode)) {//大号参与者以及小号验证者则跳过
		return true;
	}
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
	for (int i = 0; i < cipherNum; i++) {
		ist >> ciphertext[i];
	}
	ist >> cipherZero;
	ist.close();
	//读入证明
	fileName = filesPath + "proveCompare" + codes[index] + "-R" + round + ".txt";
	ist.open(fileName, ios::in);
	waitFile(fileName, ist);
	if (!ist)
	{
		cout << "[" << codes[0] << "] - " << "Can't open " << fileName << endl;
		exit(1);
	}

	//验证证明
	Commitment com(codes, round, ciphertext, bigMe, fileName);
	flag &= com.compareCheck(cipherZero);

	ist.close();
	return flag;
}
