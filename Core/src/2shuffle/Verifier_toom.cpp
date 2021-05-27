#include "Verifier_toom.h"
NTL_CLIENT


G_q* Group;
extern G_q H;
extern G_q G;
extern Pedersen Ped;
extern ElGamal El;

Verifier_toom::Verifier_toom()
{
	// TODO Auto-generated constructor stub
}

Verifier_toom::Verifier_toom(vector<int> num)
{
	// sets the values of the matrix according to the input
	m = num[0];		   //number of rows
	n = num[1];		   //number of columns
	omega = num[2];	   //windowsize for multi-expo-technique
	omega_sw = num[3]; //windowsize for multi-expo-technique sliding window and LL
	omega_LL = num[4]; //windowsize for multi-expo-technique of LL
	mu = num[5];
	m_r = num[6];
	mu_h = num[7];
	this->num = num;

	c_A = new vector<Mod_p>(m + 1);					 //allocate the storage for the commitments of Y
	c_B = new vector<Mod_p>(m);						 //allocate the storage for the commitments of T
	c_B_small = new vector<Mod_p>(m_r);				 //commitments after reduction with challenges x
	C_small = new vector<vector<Cipher_elg>*>(m_r); //reduced Ciphertexte, with challenges x

	chal_x6 = new vector<ZZ>(2 * m);					   // allocate the storage for the vector of Vandermonde challenges t, ... t^n
	chal_y6 = new vector<ZZ>(n);						   // allocate the storage for the vector of Vandermonde challenges t, ... t^n
	chal_x8 = new vector<ZZ>(2 * m + 1);				   //challenge from round 8
	basis_chal_x8 = new vector<vector<long>*>(2 * m + 2); //basis of vector e for multi-expo technique
	mul_chal_x8 = new vector<ZZ>(2 * m + 2);			   //shifted vector e, e(0) = 1, used for multi-expo
	x = new vector<ZZ>(mu_h);							   //challenges to reduce ciphertexts

	//Commitments vectors
	c_Dh = new vector<Mod_p>(m);		 //commitments to the matrix W
	c_Ds = new vector<Mod_p>(m + 1);	 //contains a_W*t_1
	c_Dl = new vector<Mod_p>(2 * m + 1); //commitments to the values Cl
	c_a_c = new vector<Mod_p>(mu_h);	 //commitment to the values used to reencrypt the E_x
	c_a = new vector<Mod_p>(2 * mu);	 //commitment to the values in the matrix a
	//Vector of product of the diagonals of permuted Ciphertexts from round 5
	E = new vector<Cipher_elg>(2 * mu);
	C_c = new vector<Cipher_elg>(mu_h); //Ciphertexts to prove correctness of reduction

	D_h_bar = new vector<ZZ>(n);   //sum over the rows in D_h
	d_bar = new vector<ZZ>(n);	   // chal_x8*D_h(m-1) +d
	Delta_bar = new vector<ZZ>(n); //chal_x8*d_h+Delta
	B_bar = new vector<ZZ>(n);	   // sum over the rows in B multiplied by chal^i

	A_bar = new vector<ZZ>(n);	//sum over the rows in Y times the challenges
	Ds_bar = new vector<ZZ>(n); // sum over the rows in U times thes challenges
}

Verifier_toom::~Verifier_toom()
{

	delete c_A;
	delete c_B;
	delete chal_x6;
	delete chal_y6;
	delete chal_x8;
	delete mul_chal_x8;
	Functions::delete_vector(basis_chal_x8);
	delete x;
	delete c_Dh;
	delete c_Ds;
	delete c_Dl;
	delete c_a;
	delete E;
	delete D_h_bar;
	delete d_bar;
	delete Delta_bar;
	delete B_bar;
	delete A_bar;
	delete Ds_bar;

	delete c_B_small;
	delete c_a_c;
	delete C_c;
}

int Verifier_toom::verify(array<string, 2> codes, string fileName, vector<vector<Cipher_elg>*>* cc, vector<vector<Cipher_elg>*>* Cc)
{
	int b;
	long i;
	ifstream ist;
	stringstream pk_ss;

	ZZ ord = H.get_ord();
	string container1 = "\0", container2;
	//string fileName = "proveShuffle" + codeName + "-R" + round + ".txt";
	ist.open(fileName, ios::in);
	waitFile(fileName, ist);
	if (!ist)
	{
		cout << "[" << codes[0] << "] - " << "Can't open " << fileName << endl;
		exit(1);
	}
	//reads the values out of the file name
	//-1 group
	ZZ g_in, gen_in, ord_in, mod_in, pk_in;
	ist >> g_in;
	ist >> gen_in;
	ist >> ord_in;
	ist >> mod_in;
	ist >> pk_in;

	Group = new G_q(g_in, gen_in, ord_in, mod_in);
	H = *Group;
	G = *Group;
	El.set_group(H);
	El.set_pk(pk_in);
	//0 pedersen
	vector<Mod_p>* pedGen = new vector<Mod_p>(n + 1);
	for (int i = 0; i <= n; i++)
	{
		ist >> container2;
		pedGen->at(i).toModP(container2, H.get_mod());
	}
	Ped = Pedersen(n, *Group, pedGen);
	Ped.set_omega(omega, omega_LL, omega_sw);
	//round 2

	//1 c_A
	for (int i = 0; i < m; i++)
	{ //接收round_1中Prover的承诺
		ist >> container2;
		c_A->at(i).toModP(container2, H.get_mod());
		container1 += container2;
	}
	pk_ss << El.get_pk().get_val();
	container1 += pk_ss.str();
	hashStr[0] = sha.hash(container1);

	//round 4
	container1.clear();
	container2.clear();
	//2 c_B
	for (int i = 0; i < m; i++)
	{
		ist >> container2;
		c_B->at(i).toModP(container2, H.get_mod());
		container1 += container2;
	}
	//3 chal_x2;
	ist >> container2;
	conv(chal_x2, container2.c_str());
	container1 += container2;

	container1 += pk_ss.str();
	container2 += pk_ss.str();
	hashStr[1] = sha.hash(container1);
	hashStr[2] = sha.hash(container2);

	//round 6
	container1.clear();
	container2.clear();
	//4 c_z
	ist >> container2;
	/*ZZ c_z_ZZ;
	conv(c_z_ZZ, container2.c_str());
	c_z = Mod_p(c_z_ZZ, H.get_mod());*/
	c_z.toModP(container2, H.get_mod());
	container1 += container2;
	//5 c_Dh
	for (int i = 0; i < m; i++)
	{
		ist >> container2;
		c_Dh->at(i).toModP(container2, H.get_mod());
		container1 += container2;
	}
	//6 C_c
	for (int i = 0; i < mu_h; i++)
	{
		ist >> container2;
		C_c->at(i).toCipher(container2);
		container1 += container2;
	}
	//7 c_a_c
	for (int i = 0; i < mu_h; i++)
	{
		ist >> container2;
		c_a_c->at(i).toModP(container2, H.get_mod());
		container1 += container2;
	}
	//8 chal_y4
	ist >> container2;
	conv(chal_y4, container2.c_str());
	container1 += container2;
	//9 chal_z4
	ist >> container2;
	conv(chal_z4, container2.c_str());
	container1 += container2;

	container1 += pk_ss.str();
	container2 += pk_ss.str();
	hashStr[3] = sha.hash(container1);
	hashStr[4] = sha.hash(container2);

	//round 8
	container1.clear();
	container2.clear();
	//10 c_Dl
	for (i = 0; i <= 2 * m; i++)
	{ //33
		ist >> container2;
		c_Dl->at(i).toModP(container2, H.get_mod());
		container1 += container2;
	}
	//11 c_D0
	ist >> container2;
	//conv(c_D0, container2.c_str());
	c_D0.toModP(container2, H.get_mod());
	container1 += container2;
	//12 c_Dm
	ist >> container2;
	//conv(c_Dm, container2.c_str());
	c_Dm.toModP(container2, H.get_mod());
	container1 += container2;
	//13 c_d
	ist >> container2;
	//conv(c_d, container2.c_str());
	c_d.toModP(container2, H.get_mod());
	container1 += container2;
	//14 c_Delta
	ist >> container2;
	//conv(c_Delta, container2.c_str());
	c_Delta.toModP(container2, H.get_mod());
	container1 += container2;
	//15 c_d_h
	ist >> container2;
	//conv(c_dh, container2.c_str());
	c_dh.toModP(container2, H.get_mod());
	container1 += container2;
	//16 a_c_bar
	ist >> container2;
	conv(a_c_bar, container2.c_str());
	container1 += container2;
	//17 r_ac_bar
	ist >> container2;
	conv(r_ac_bar, container2.c_str());
	container1 += container2;
	//18 E
	for (i = 0; i < 8; i++)
	{
		ist >> container2;
		E->at(i).toCipher(container2);
		container1 += container2;
	}
	//19 c_B0
	ist >> container2;
	//conv(c_B0, container2.c_str());
	c_B0.toModP(container2, H.get_mod());
	container1 += container2;
	//20 c_a
	for (i = 0; i < 8; i++)
	{
		ist >> container2;
		c_a->at(i).toModP(container2, H.get_mod());
		container1 += container2;
	}
	//21 chal_x6
	for (i = 0; i < chal_x6->size(); i++)
	{
		ist >> container2;
		conv(chal_x6->at(i), container2.c_str());
		container1 += container2;
	}
	//22 chal_y6
	for (i = 0; i < chal_y6->size(); i++)
	{
		ist >> container2;
		conv(chal_y6->at(i), container2.c_str());
		container1 += container2;
	}

	container1 += pk_ss.str();
	hashStr[5] = sha.hash(container1);

	//round 10
	//23 D_h_bar
	for (i = 0; i < n; i++)
	{
		ist >> D_h_bar->at(i);
	}
	//24 r_Dh_bar
	ist >> r_Dh_bar;
	//25 d_bar
	for (i = 0; i < n; i++)
	{
		ist >> d_bar->at(i);
	}
	//26 r_d_bar
	ist >> r_d_bar;
	//27 Delta_bar
	for (i = 0; i < n; i++)
	{
		ist >> Delta_bar->at(i);
	}
	//28 r_Delta_bar
	ist >> r_Delta_bar;
	//29 A_bar
	for (i = 0; i < n; i++)
	{
		ist >> A_bar->at(i);
	}
	//30 r_A_bar
	ist >> r_A_bar;
	//31 Ds_bar
	for (i = 0; i < n; i++)
	{
		ist >> Ds_bar->at(i);
	}
	//32 r_Ds_bar
	ist >> r_Ds_bar;
	//33 r_Dl_bar
	ist >> r_Dl_bar;
	//34 B_bar
	for (i = 0; i < n; i++)
	{
		ist >> B_bar->at(i);
	}
	//35 r_B_bar
	ist >> r_B_bar;
	//36 a_bar
	ist >> a_bar;
	//37 r_a_bar
	ist >> r_a_bar;
	//38 rho_bar
	ist >> rho_bar;
	//39 chal_x8_temp
	ZZ chal_x8_temp;
	ist >> chal_x8_temp;
	//cout << "chal_x8_temp: " << chal_x8_temp << endl;
	func_ver::fill_x8(chal_x8, basis_chal_x8, mul_chal_x8, omega, chal_x8_temp);
	ist.close();

	array<ZZ, 6> hashChk; //hash验证
	for (int i = 0; i < 6; i++)
	{
		ZZ hashValueZZ;
		conv(hashValueZZ, hashStr[i].c_str());
		Mod_p hashValueModP = Mod_p(hashValueZZ, H.get_mod());
		while (hashValueModP.get_val() > ord)
			hashValueModP.set_val(hashValueModP.get_val() - ord);
		hashChk[i] = hashValueModP.get_val();
	}

	int flag = 0;
	int errorCode = 0;
	//Check that the D_hi's are constructed correctly
	// cout<<"omega_LL: "<<omega_LL<<endl;
	//b = func_ver::check_Dh_op(c_Dh, mul_chal_x8, D_h_bar, r_Dh_bar, omega_LL);
	if (func_ver::check_Dh_op(c_Dh, mul_chal_x8, D_h_bar, r_Dh_bar, omega_LL))
	{//0
		++errorCode;
		//Check that matrix D is constructed correctly
		//b = func_ver::check_D_op(c_D0, c_z, c_A, c_B, chal_x8, chal_y4, A_bar, r_A_bar, n);
		if (func_ver::check_D_op(c_D0, c_z, c_A, c_B, chal_x8, chal_y4, A_bar, r_A_bar, n))
		{//1
			++errorCode;
			//Check that D_s is constructed correctly
			//b = func_ver::check_Ds_op(c_Ds, c_Dh, c_Dm, chal_x6, chal_x8, Ds_bar, r_Ds_bar);
			if (func_ver::check_Ds_op(c_Ds, c_Dh, c_Dm, chal_x6, chal_x8, Ds_bar, r_Ds_bar))
			{//2
				++errorCode;
				//Check that the Dl's are correct
				//b = func_ver::check_Dl_op(c_Dl, chal_x8, A_bar, Ds_bar, chal_y6, r_Dl_bar);
				if (func_ver::check_Dl_op(c_Dl, chal_x8, A_bar, Ds_bar, chal_y6, r_Dl_bar))
				{//3
					++errorCode;
					//Check that vector d was constructed correctly
					//b = func_ver::check_d_op(c_Dh, c_d, chal_x8, d_bar, r_d_bar);
					if (func_ver::check_d_op(c_Dh, c_d, chal_x8, d_bar, r_d_bar))
					{//4
						++errorCode;
						//Check that Deltas are constructed correctly
						//b = func_ver::check_Delta_op(c_dh, c_Delta, chal_x8, Delta_bar, d_bar, r_Delta_bar, chal_x2, chal_z4, chal_y4);
						if (func_ver::check_Delta_op(c_dh, c_Delta, chal_x8, Delta_bar, d_bar, r_Delta_bar, chal_x2, chal_z4, chal_y4))
						{//5
							++errorCode;
							//Check that the commitments a_T contain the right values
							//b = check_B();
							if (check_B())
							{//6
								++errorCode;
								//Check that the reecncryption was done correctly
								//b = check_a();
								if (check_a())
								{//7
									++errorCode;
									//Check that E_c->at(mu-1) contains c and D->at(4) = C
									//b = check_c(c); //Both commitments shoud be com(0,0)
									if (check_c(cc) & (c_a->at(4) == c_a_c->at(3)))
									{//8
										++errorCode;
										//Check correctness of the chiphertexts
										//b = check_E(C);
										if (check_E(Cc))
										{//9
											++errorCode;
											//Check the the reencryption of the E_c is correct
											//b = check_ac();
											if (check_ac())
											{//10
												++errorCode;
												//Check the the hash
												//b = check_hash(hashChk);
												if (check_hash(hashChk))
												{//11
													flag = 1;
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
	//cout << "[" << codeName << "] - " << "shuffle results: " << flush;
	//string ans = (flag) ? "PASS" : "FAIL";
	//cout << ans << endl;
	/*ofstream ost;
	ost.open("ansShuffle" + codeName + ".txt", ios::out);
	ost << ans << endl;
	ost.close();*/
	string errorStr[] = { "Dh","D","Ds","Dl" ,"d" ,"Delta" ,"B" ,"a" ,"c" ,"E" ,"ac" ,"hash" };
	if (!flag)
		cout << "[" << codes[0] << "] - " << "ERROR: " << errorStr[errorCode] << endl;
	return flag;
}

void Verifier_toom::calculate_c(Cipher_elg& c, vector<vector<Cipher_elg>*>* enc)
{
	long i, j;
	Cipher_elg temp;
	ZZ chal_temp;
	ZZ ord = H.get_ord();
	vector<ZZ>* v_chal = 0;

	chal_temp = to_ZZ(1);
	c = Cipher_elg(1, 1, H.get_mod());
	v_chal = new vector<ZZ>(n);
	for (i = 0; i < m; i++)
	{
		for (j = 0; j < n; j++)
		{
			MulMod(chal_temp, chal_temp, chal_x2, ord);
			v_chal->at(j) = chal_temp;
		}
		multi_expo::expo_mult(temp, enc->at(i), v_chal, omega);
		Cipher_elg::mult(c, c, temp);
	}

	delete v_chal;
}

void Verifier_toom::calculate_ac(Mod_p& com)
{
	long i;
	Mod_p temp;

	com = c_a_c->at(0);
	for (i = 1; i < mu_h; i++)
	{
		Mod_p::expo(temp, c_a_c->at(i), x->at(i - 1));
		Mod_p::mult(com, com, temp);
	}
}

void Verifier_toom::reduce_c_B()
{
	long i, j;
	Mod_p temp, temp_1;

	for (i = 0; i < 4 * m_r; i++)
	{
		temp = c_B->at(4 * i);
		for (j = 1; j < mu; j++)
		{
			Mod_p::expo(temp_1, c_B->at(4 * i + j), x->at(j - 1));
			Mod_p::mult(temp, temp, temp_1);
		}
		c_B_small->at(i) = temp;
	}
}

void Verifier_toom::calculate_C(Cipher_elg& C, vector<Cipher_elg>* C_c, vector<ZZ>* x)
{
	long i;
	ZZ t_1;
	ZZ ord = H.get_ord();
	Mod_p temp;
	Mod_p gen = H.get_gen();
	Cipher_elg temp_1;
	NegateMod(t_1, a_c_bar, ord);
	Mod_p::expo(temp, gen, t_1);
	C = El.encrypt(temp, to_ZZ(0));
	Cipher_elg::mult(C, C, C_c->at(0));
	for (i = 1; i < mu_h; i++)
	{
		Cipher_elg::expo(temp_1, C_c->at(i), x->at(i - 1));
		Cipher_elg::mult(C, C, temp_1);
	}
}

int Verifier_toom::check_B()
{
	long i, j;
	Mod_p temp, temp_1, t_B, co_B;
	ZZ mod = Group->get_mod();
	vector<Mod_p>* c_B_small = new vector<Mod_p>(5);
	vector<Mod_p>* c_B_temp = new vector<Mod_p>(4);

	c_B_small->at(0) = c_B0;
	for (i = 0; i < m_r; i++)
	{
		temp = c_B->at(4 * i);
		for (j = 1; j < 4; j++)
		{
			Mod_p::expo(temp_1, c_B->at(4 * i + j), chal_x6->at(j - 1));
			Mod_p::mult(temp, temp, temp_1);
		}
		c_B_small->at(i + 1) = temp;
	}
	t_B = c_B_small->at(0);
	for (i = 1; i < 5; i++)
	{
		Mod_p::expo(temp, c_B_small->at(i), chal_x8->at(i - 1));
		Mod_p::mult(t_B, t_B, temp);
	}

	delete c_B_temp;
	delete c_B_small;

	co_B = Ped.commit_opt(B_bar, r_B_bar);
	//	cout<<"B "<<t_B<<" "<<co_B<<endl;
	if (t_B == co_B)
	{
		return 1;
	}
	return 0;
}

int Verifier_toom::check_a()
{
	long i;
	Mod_p t_a, co_a;
	vector<ZZ>* chal_temp = new vector<ZZ>(8);

	chal_temp->at(0) = 1;
	for (i = 1; i < 8; i++)
	{
		chal_temp->at(i) = chal_x8->at(i - 1);
	}
	multi_expo::multi_expo_LL(t_a, c_a, chal_temp, omega_sw);
	co_a = Ped.commit_sw(a_bar, r_a_bar);

	//cout<<"a "<<t_a<<" "<<co_a<<" "<<c_a->at(4)<<endl;
	delete chal_temp;
	if (t_a == co_a)
	{
		return 1;
	}
	return 0;
}

int Verifier_toom::check_c(vector<vector<Cipher_elg>*>* enc)
{
	Cipher_elg c, C;

	calculate_c(c, enc);

	calculate_C(C, C_c, chal_x6);
	//	cout<<"C "<<C_c->at(mu-1)<<" "<<c<<endl;
	//	cout<<"C "<<E->at(4)<<" "<<C<<endl;
	if (C_c->at(mu - 1) == c & E->at(4) == C)
	{
		return 1;
	}
	return 0;
}

int Verifier_toom::check_E(vector<vector<Cipher_elg>*>* C)
{
	long i, j;
	Mod_p temp;
	Mod_p gen = H.get_gen();
	Cipher_elg temp_1, temp_2, t_D, c_D;
	vector<ZZ>* chal_1_temp = new vector<ZZ>(4);
	vector<ZZ>* chal_2_temp = new vector<ZZ>(4);
	vector<Cipher_elg>* row_C;

	for (i = 0; i < 3; i++)
	{
		chal_1_temp->at(i) = chal_x6->at(2 - i);
	}
	chal_1_temp->at(3) = 1;

	for (i = 0; i < m_r; i++)
	{
		row_C = new vector<Cipher_elg>(n);
		for (j = 0; j < n; j++)
		{
			multi_expo::multi_expo_LL(row_C->at(j), C->at(4 * i)->at(j), C->at(4 * i + 1)->at(j), C->at(4 * i + 2)->at(j), C->at(4 * i + 3)->at(j), chal_1_temp, omega_sw);
		}
		C_small->at(i) = row_C;
	}

	for (i = 0; i < 3; i++)
	{
		chal_2_temp->at(i) = chal_x8->at(2 - i);
	}
	chal_2_temp->at(3) = to_ZZ(1);

	Mod_p::expo(temp, gen, a_bar);
	temp_1 = El.encrypt(temp, rho_bar);
	multi_expo::expo_mult(temp_2, C_small, chal_2_temp, B_bar, omega);
	Cipher_elg::mult(c_D, temp_1, temp_2);
	//c_D=temp_1*temp_2;

	multi_expo::expo_mult(t_D, E, basis_chal_x8, omega);

	/* cout << "\n";
	for (i = 0; i < basis_chal_x8->size(); i++)
	{
		for (int j = 0; j < basis_chal_x8->at(0)->size(); j++)
		{
			cout << basis_chal_x8->at(i)->at(j) << " ";
		}
		cout << "\n";
	}
	cout << "\n"; */

	delete chal_1_temp;
	delete chal_2_temp;
	Functions::delete_vector(C_small);
	// cout<<"E"<<t_D<<endl;
	// cout<<"E"<<c_D<<endl;
	if (t_D == c_D)
	{
		return 1;
	}
	return 0;
}

int Verifier_toom::check_ac()
{
	Mod_p t_a_c, co_a_c, temp;
	int i;

	t_a_c = c_a_c->at(0);
	for (i = 1; i < 7; i++)
	{
		Mod_p::expo(temp, c_a_c->at(i), chal_x6->at(i - 1));
		Mod_p::mult(t_a_c, t_a_c, temp);
	}
	co_a_c = Ped.commit_sw(a_c_bar, r_ac_bar);
	//	cout<<"ac "<<t_a_c<<" "<<c_a_c;
	if (t_a_c == co_a_c)
	{
		return 1;
	}
	return 0;
}

int Verifier_toom::check_hash(array<ZZ, 6> hashChk)
{
	int a = (hashChk[0] == chal_x2) ? 0 : 1;
	int b = (hashChk[1] == chal_z4) ? 0 : 1;
	int c = (hashChk[2] == chal_y4) ? 0 : 1;
	int d = (hashChk[3] == chal_x6->at(0)) ? 0 : 1;
	int e = (hashChk[4] == chal_y6->at(0)) ? 0 : 1;
	int f = (hashChk[5] == chal_x8->at(0)) ? 0 : 1;
	return !(a + b + c + d + e + f);
}
