#include "Prover_toom.h"
NTL_CLIENT

extern G_q G;
extern G_q H;
extern Pedersen Ped;
extern ElGamal El;


Prover_toom::Prover_toom()
{
	// TODO Auto-generated constructor stub
}

Prover_toom::Prover_toom(vector<vector<Cipher_elg>*>* Cin, vector<vector<ZZ>*>* Rin, vector<vector<vector<int>*>*>* piin, vector<int> num, string code)
{
	// set the dimensions of the row and columns according to the user input
	C = Cin;			  //sets the reencrypted chipertexts to the input
	R = Rin;			  //sets the random elements to the input
	pi = piin;			  // sets the permutation to the input
	m = num[0];			  //number of rows
	n = num[1];			  //number of columns
	omega_mulex = num[2]; //windowsize for sliding-window technique
	omega_sw = num[3];	  //windowsize for multi-expo technique
	omega_LL = num[4];	  //windowsize for multi-expo technique
	mu = num[5];
	m_r = num[6];
	mu_h = num[7];

	//Creates the matrices A，内容为pi
	A = new vector<vector<ZZ>*>(m);
	func_pro::set_A(A, pi, m, n);
	SetSeed(to_ZZ((unsigned int)time(0) + clock()));
	//Allocate the storage needed for the vectors
	chal_x6 = new vector<ZZ>(2 * m);	 //x6, x6^2, ... challenges from round 6
	chal_y6 = new vector<ZZ>(n);		 //y6, y6^2, ... challenges form round 6
	chal_x8 = new vector<ZZ>(2 * m + 1); //x8, x8^2, ... challenges from round 8

	basis_chal_x8 = new vector<vector<long>*>(2 * m + 2); //basis of vector e for multi-expo technique
	mul_chal_x8 = new vector<ZZ>(2 * m + 2);			   //shifted vector e, e(0) = 1, used for multi-expo

	//Allocate the storage needed for the vectors
	c_A = new vector<Mod_p>(m + 1); //commitments to the rows in A
	r_A = new vector<ZZ>(m + 1);	//random elements used for the commitments

	D = new vector<vector<ZZ>*>(m + 1);   //vector containing in the first row random values and in all others y*A(ij) + B(ij)-z
	D_h = new vector<vector<ZZ>*>(m);	   //Vector of the Hadamare products of the rows in D
	D_s = new vector<vector<ZZ>*>(m + 1); //Shifted rows of D_h
	d = new vector<ZZ>(n);				   //containing random elements to proof product of D_hm
	Delta = new vector<ZZ>(n);			   //containing random elements to proof product of D_hm
	d_h = new vector<ZZ>(n);			   // vector containing the last row of D-h

	r_D_h = new vector<ZZ>(m);						 //random elements for commitments to D_h
	c_D_h = new vector<Mod_p>(m + 2);				 //commitments to the rows in D_h
	C_small = new vector<vector<Cipher_elg>*>(m_r); //matrix of reduced ciphertexts

	B = new vector<vector<ZZ>*>(m);				   //matrix of permuted exponents, exponents are x2^i, i=1, ..N
	basis_B = new vector<vector<vector<long>*>*>(m); //basis for the multi-expo, containing the Bij
	B_small = new vector<vector<ZZ>*>(m_r);		   //matrix of reduced exponents
	B_0 = new vector<ZZ>(n);						   //vector containing random exponents
	basis_B0 = new vector<vector<long>*>(n);		   //basis for multi-expo, containing  the B0j
	r_B = new vector<ZZ>(m);						   //random elements used to commit to B
	r_B_small = new vector<ZZ>(m_r);				   //random elements for commitments to B_small
	c_B = new vector<Mod_p>(m);						   //vector of commitments to rows in T
	a = new vector<ZZ>(2 * m);						   //elements used for reencryption in round 5
	r_a = new vector<ZZ>(2 * m);					   //random elements to commit to elements in a
	c_a = new vector<Mod_p>(2 * m);					   //commitments to elements a
	E = new vector<Cipher_elg>(2 * m);				   //vector of the products of the diogonals of A^T generated in round 7
	rho_a = new vector<ZZ>(2 * m);					   //contains random elements used for the reencryption in 7

	C_c = new vector<Cipher_elg>(mu_h); //Ciphertexts to prove correctness of reduction
	c_a_c = new vector<Mod_p>(mu_h);	//vector containing the commitments to value used for the reencryption of E_c
	a_c = new vector<ZZ>(mu_h);			//vector containing the values used for reecnrcyption
	r_c = new vector<ZZ>(mu_h);			//random elements used to commit to a_c
	rho_c = new vector<ZZ>(mu_h);		//random elements used in the reencryption

	a = new vector<ZZ>(2 * mu);			//elements used for reencryption in round 5
	r_a = new vector<ZZ>(2 * mu);		//random elements to commit to elements in a
	c_a = new vector<Mod_p>(2 * mu);	//commitments to elements a
	E = new vector<Cipher_elg>(2 * mu); //vector of the products of the diogonals of Y^T generated in round 9
	rho_a = new vector<ZZ>(2 * mu);		//contains random elements used for the reencryption in 9

	Dl = new vector<ZZ>(2 * m + 1);		 //bilinear_map(Y_pi, U, chal_t)
	r_Dl = new vector<ZZ>(2 * m + 1);	 //random elements to commit to the C_ls
	c_Dl = new vector<Mod_p>(2 * m + 1); //commitments to the C_ls

	d_bar = new vector<ZZ>(n);	   // chal_x8*D_h(m-1) +d
	Delta_bar = new vector<ZZ>(n); //chal_x8*d_h+Delta
	D_h_bar = new vector<ZZ>(n);   //sum over the rows in D_h

	B_bar = new vector<ZZ>(n);	 // sum over the rows in B multiplied by chal^i
	A_bar = new vector<ZZ>(n);	 //sum over the rows in A times the challenges
	D_s_bar = new vector<ZZ>(n); // sum over the rows in D_s times the challenges
}

//Destructor deletes all pointers and frees the storage
Prover_toom::~Prover_toom()
{
	delete chal_x6;
	delete chal_y6;
	delete chal_x8;
	delete c_A;
	delete r_A;

	Functions::delete_vector(D);
	Functions::delete_vector(D_h);
	Functions::delete_vector(D_s);
	delete d;
	delete Delta;
	delete d_h;

	delete r_D_h;
	delete c_D_h;
	Functions::delete_vector(B);
	Functions::delete_vector(basis_B);
	delete B_0;
	Functions::delete_vector(basis_B0);
	delete r_B;
	delete r_B_small;
	delete c_B;
	delete a;
	delete r_a;
	delete c_a;
	delete rho_a;

	delete Dl;
	delete r_Dl;
	delete c_Dl;

	delete D_h_bar;
	delete d_bar;
	delete Delta_bar;
	delete B_bar;
	delete A_bar;
	delete D_s_bar;

	delete C_c;
	delete c_a_c; //vector containing the commitments to value used for the reencryption of E_low_up
	delete a_c;	  //vector containing the exponents
	delete r_c;
	delete rho_c;
	delete E;
}

//round_1 picks random elements and commits to the rows of A
void Prover_toom::round_1()
{
	Ped = Pedersen(n, G);
	Ped.set_omega(omega_mulex, omega_LL, omega_sw);

	//对向量pi的每一行进行承诺
	Functions::commit_op(A, r_A, c_A);


	stringstream ss;
	//-1 group
	ost << G.get_g() << endl;
	ost << G.get_gen() << endl;
	ost << G.get_ord() << endl;
	ost << G.get_mod() << endl;
	ost << El.get_pk() << endl;
	//0 pedersen
	vector<Mod_p>* pedGen = Ped.get_gen();
	for (int i = 0; i <= n; i++)
	{
		ost << pedGen->at(i) << endl;
	}
	//1 c_A
	for (int i = 0; i < m; i++)
	{
		ost << c_A->at(i) << endl;
		ss << c_A->at(i).get_val();
	}
	ord = H.get_ord();
	pk_ss << El.get_pk().get_val();
	hashStr[0] = ss.str() + pk_ss.str();

}

//round_3, permuted the exponents in s,  picks random elements and commits to values
void Prover_toom::round_3()
{
	ZZ x2;
	vector<vector<ZZ>*>* chal_x2 = new vector<vector<ZZ>*>(m);

	// 用hash生成随机挑战x
	string hashValueStr = sha.hash(hashStr[0]);
	ZZ hashValueZZ;
	conv(hashValueZZ, hashValueStr.c_str());
	Mod_p hashValueModP = Mod_p(hashValueZZ, H.get_mod());
	while (hashValueModP.get_val() > ord)
		hashValueModP.set_val(hashValueModP.get_val() - ord);
	x2 = hashValueModP.get_val();

	//生成内容为x2, x2^2, ... , x2^N的顺序矩阵chal_x2
	func_pro::set_x2(chal_x2, x2, m, n);

	//根据向量pi将chal_x2重新排列，生成shuffle后的矩阵B
	func_pro::set_B_op(B, basis_B, chal_x2, pi, omega_mulex);

	//对B的每一行生成随机数和承诺
	Functions::commit_op(B, r_B, c_B);

	//ofstream ost("prove.pro", ios::app);
	stringstream ss1, ss2;
	//2 c_B
	for (int i = 0; i < m; i++)
	{
		ost << c_B->at(i) << "\n";
		ss1 << c_B->at(i).get_val();
	}

	//3 x2
	ost << x2 << endl;
	ss1 << x2;
	ss2 << x2;

	hashStr[1] = ss1.str() + pk_ss.str();
	hashStr[2] = ss2.str() + pk_ss.str();
	//ost.close();

	Functions::delete_vector(chal_x2);
}

//round_5a 构建矩阵D，并对向量chal_z和D_h做承诺
void Prover_toom::round_5a()
{
	long i;
	ZZ temp, t; //temporary variables
	vector<ZZ>* r = new vector<ZZ>(n);
	vector<ZZ>* v_z = new vector<ZZ>(n); //row containing the challenge alpha
	ZZ ord = H.get_ord();
	time_t rawtime;
	time(&rawtime);

	//生成矩阵D: y × A_ij + B_ij - z
	func_pro::set_D(D, A, B, chal_z4, chal_y4);

	//生成用于product argument的矩阵D_h：Set the matrix D_h as the Hadamard product of the rows in D
	func_pro::set_D_h(D_h, D);

	for (i = 0; i < n; i++)
	{
		v_z->at(i) = chal_z4; //fills the vector alpha with the challenge alpha
		NegateMod(r->at(i), to_ZZ(1), ord);
	}

	//将矩阵D的最后一行用“-1”填充
	D->at(m) = r;
	//将生成矩阵A的承诺的随机数向量的最后一个值设为0
	r_A->at(m) = 0;

	//对chal_z做承诺
	Functions::commit_op(v_z, r_z, c_z);
	//对D_h的每一行进行承诺
	Functions::commit_op(D_h, r_D_h, c_D_h);//r_D_h为t

	delete v_z;
	func_pro::set_Rb(B, R, R_b);//b_ij × R_ij的结果累加
	commit_ac();

	/*for (int i = 0; i < basis_B->size(); i++) {
		for (int j = 0; j < basis_B->at(0)->size(); j++){
			for (int k = 0; k < basis_B->at(0)->at(0)->size(); k++)
				cout << basis_B->at(i)->at(j)->at(k) << " ";
			cout << endl;
		}
		cout << endl;
	}*/
	calculate_Cc(C, basis_B);
}

void Prover_toom::round_5()
{
	// 用hash生成随机挑战y,z
	string hashValueStr1 = sha.hash(hashStr[1]);
	ZZ hashValueZZ1;
	conv(hashValueZZ1, hashValueStr1.c_str());
	Mod_p hashValueModP1 = Mod_p(hashValueZZ1, H.get_mod());
	while (hashValueModP1.get_val() > ord)
		hashValueModP1.set_val(hashValueModP1.get_val() - ord);

	string hashValueStr2 = sha.hash(hashStr[2]);
	ZZ hashValueZZ2;
	conv(hashValueZZ2, hashValueStr2.c_str());
	Mod_p hashValueModP2 = Mod_p(hashValueZZ2, H.get_mod());
	while (hashValueModP2.get_val() > ord)
		hashValueModP2.set_val(hashValueModP2.get_val() - ord);

	chal_z4 = hashValueModP1.get_val();
	chal_y4 = hashValueModP2.get_val();

	round_5a();//构建矩阵D，并对向量chal_z和D_h做承诺

	//ofstream ost("prove.pro", ios::app);
	stringstream ss1, ss2;
	//4 c_z
	ost << c_z << endl;
	ss1 << c_z;

	//5 c_D_h
	for (int i = 0; i < m; i++)
	{
		ost << c_D_h->at(i) << endl;
		ss1 << c_D_h->at(i);
	}

	//6 C_c
	for (int i = 0; i < mu_h; i++)
	{
		ost << C_c->at(i) << endl;
		ss1 << C_c->at(i);
	}

	//7 c_a_c
	for (int i = 0; i < mu_h; i++)
	{
		ost << c_a_c->at(i) << endl;
		ss1 << c_a_c->at(i);
	}

	//8 chal_y4
	ost << chal_y4 << endl;
	ss1 << chal_y4;

	//9 chal_z4
	ost << chal_z4 << endl;
	ss1 << chal_z4;
	ss2 << chal_z4;

	hashStr[3] = ss1.str() + pk_ss.str();
	hashStr[4] = ss2.str() + pk_ss.str();

	//ost.close();
}

void Prover_toom::round_7a()
{

	//Set the rows in D_s as D_s(i) = chal_t_1^i+1*D_h(i) for i<m-1 and D_s(m-1) = sum(chal_x6^i+1 * D_s(i+1) and set last row of D_s to random values and also D(0)
	func_pro::set_D_s(D_s, D_h, D, chal_x6, r_Dl_bar);

	//calculate the values Dls as Dl(l) = sum(D(i)*D_s->at(i)*chal_y6) for j=n+i-l and commits to the values
	func_pro::commit_Dl_op(c_Dl, Dl, r_Dl, D, D_s, chal_y6);

	//commitments to D(0) and D_s(m)
	Functions::commit_op(D->at(0), r_D0, c_D0);
	Functions::commit_op(D_s->at(m), r_Dm, c_Dm);

	//commitments to prove that the product over the elements in D_h->at(m) is the desired product of n *y + x2n -z
	func_pro::commit_d_op(d, r_d, c_d);
	func_pro::commit_Delta_op(Delta, d, r_Delta, c_Delta);
	func_pro::commit_d_h_op(D_h, d_h, d, Delta, r_d_h, c_d_h);
	calculate_ac_bar(chal_x6);
	calculate_r_ac_bar(chal_x6);

	double tstart, tstop;
	vector<Cipher_elg>* e = 0;

	tstart = (double)clock() / CLOCKS_PER_SEC;
	reduce_C(C, B, r_B, chal_x6, m_r);
	set_Rb1(chal_x6);
	tstop = (double)clock() / CLOCKS_PER_SEC;
	time_di = time_di + tstop - tstart;

	func_pro::commit_a_op(a, r_a, c_a);
	func_pro::commit_B0_op(B_0, basis_B0, r_B0, c_B0, omega_mulex);

	tstart = (double)clock() / CLOCKS_PER_SEC;
	e = calculate_e();
	tstop = (double)clock() / CLOCKS_PER_SEC;
	time_di = time_di + tstop - tstart;
	//cout<<"To calculate the di's took "<<time_di<<" sec."<<endl;

	calculate_E(e);

	delete e;
	Functions::delete_vector(C_small);
}

void Prover_toom::round_7()
{
	// 用hash生成随机挑战y,z
	string hashValueStr1 = sha.hash(hashStr[3]);
	ZZ hashValueZZ1;
	conv(hashValueZZ1, hashValueStr1.c_str());
	Mod_p hashValueModP1 = Mod_p(hashValueZZ1, H.get_mod());
	while (hashValueModP1.get_val() > ord)
		hashValueModP1.set_val(hashValueModP1.get_val() - ord);

	string hashValueStr2 = sha.hash(hashStr[4]);
	ZZ hashValueZZ2;
	conv(hashValueZZ2, hashValueStr2.c_str());
	Mod_p hashValueModP2 = Mod_p(hashValueZZ2, H.get_mod());
	while (hashValueModP2.get_val() > ord)
		hashValueModP2.set_val(hashValueModP2.get_val() - ord);

	ZZ chal_x6_temp = hashValueModP1.get_val();
	ZZ chal_y6_temp = hashValueModP2.get_val();
	func_ver::fill_vector(chal_x6, chal_x6_temp);
	func_ver::fill_vector(chal_y6, chal_y6_temp);

	round_7a();

	//ofstream ost("prove.pro", ios::app);
	stringstream ss;
	//10 c_Dl
	for (int i = 0; i <= 2 * m; i++)
	{
		ost << c_Dl->at(i) << endl;
		ss << c_Dl->at(i);
	}

	//11 c_D0
	ost << c_D0 << endl;
	ss << c_D0;

	//12 c_Dm
	ost << c_Dm << endl;
	ss << c_Dm;

	//13 c_d
	ost << c_d << endl;
	ss << c_d;

	//14 c_Delta
	ost << c_Delta << endl;
	ss << c_Delta;

	//15 c_d_h
	ost << c_d_h << endl;
	ss << c_d_h;

	//16 a_c_bar
	ost << a_c_bar << endl;
	ss << a_c_bar;

	//17 r_ac_bar
	ost << r_ac_bar << endl;
	ss << r_ac_bar;

	//18 E
	for (int i = 0; i < 8; i++)
	{
		ost << E->at(i) << endl;
		ss << E->at(i);
	}

	//19 c_B0
	ost << c_B0 << endl;
	ss << c_B0;

	//20 c_a
	for (int i = 0; i < 8; i++)
	{
		ost << c_a->at(i) << endl;
		ss << c_a->at(i);
	}

	//21 chal_x6
	for (int i = 0; i < chal_x6->size(); i++)
	{
		ost << chal_x6->at(i) << endl;
		ss << chal_x6->at(i);
	}

	//22 chal_y6
	for (int i = 0; i < chal_y6->size(); i++)
	{
		ost << chal_y6->at(i) << endl;
		ss << chal_y6->at(i);
	}

	hashStr[5] = ss.str() + pk_ss.str();

	//ost.close();
}

void Prover_toom::round_9a()
{

	//Calculate D_h_bar = sum(chal^i*D_h(row(i)))
	func_pro::calculate_D_h_bar(D_h_bar, D_h, chal_x8);

	//calculate r_Dh_bar = sum(chal^i*r_Dh_bar(i)), opening to prove correctness of D_h
	func_pro::calculate_r_Dh_bar(r_D_h, chal_x8, r_Dh_bar);

	//calculate d_bar, r_d_bar, Delta_bar, r_Delta_bar, openings to prove product over elements in D_h->at(m-1)
	func_pro::calculate_dbar_rdbar(D_h, chal_x8, d_bar, d, r_D_h, r_d, r_d_bar);
	func_pro::calculate_Deltabar_rDeltabar(d_h, chal_x8, Delta_bar, Delta, r_d_h, r_Delta, r_Delta_bar);

	//A_bar and r_A_bar, openings to prove permutation in D
	func_pro::calculate_A_bar(D, A_bar, chal_x8);
	func_pro::calculate_r_A_bar(r_D0, r_A, r_B, chal_x8, r_z, chal_y4, r_A_bar);

	//D_s_bar and r_Ds_bar, openings to prove correctness of D_s
	func_pro::calculate_D_s_bar(D_s, D_s_bar, chal_x8);
	func_pro::calculate_r_Ds_bar(r_D_h, chal_x6, chal_x8, r_Ds_bar, r_Dm);

	//sum of the random values used to commit to the Dl's, to prover correctness of them
	func_pro::calculate_r_Dl_bar(r_Dl, chal_x8, r_Dl_bar);

	//calculate B_bar
	func_pro::calculate_B_bar(B_0, B_small, chal_x8, B_bar);
	Functions::delete_vector(B_small);

	//calculate r_B_bar
	func_pro::calculate_r_B_bar(r_B_small, chal_x8, r_B0, r_B_bar);

	//calculate a_bar
	func_pro::calculate_a_bar(a, chal_x8, a_bar);

	//calculate r_a_bar
	func_pro::calculate_r_a_bar(r_a, chal_x8, r_a_bar);

	//calculate rho_a_bar
	func_pro::calculate_rho_a_bar(rho_a, chal_x8, rho_bar);
}

void Prover_toom::round_9() {
	// 用hash生成随机挑战x
	string hashValueStr = sha.hash(hashStr[5]);
	ZZ hashValueZZ;
	conv(hashValueZZ, hashValueStr.c_str());
	Mod_p hashValueModP = Mod_p(hashValueZZ, H.get_mod());
	while (hashValueModP.get_val() > ord)
		hashValueModP.set_val(hashValueModP.get_val() - ord);
	ZZ chal_x8_temp = hashValueModP.get_val();

	//一个随机挑战
	func_pro::fill_x8(chal_x8, chal_x8_temp);

	round_9a();

	//ofstream ost("prove.pro", ios::app);
	//23 D_h_bar
	for (int i = 0; i < n; i++)
	{
		ost << D_h_bar->at(i) << endl;
	}

	//24 r_Dh_bar
	ost << r_Dh_bar << endl;

	//25 d_bar
	for (int i = 0; i < n; i++)
	{
		ost << d_bar->at(i) << endl;
	}

	//26 r_d_bar
	ost << r_d_bar << endl;

	//27 Delta_bar
	for (int i = 0; i < n; i++)
	{
		ost << Delta_bar->at(i) << endl;
	}

	//28 r_Delta_bar
	ost << r_Delta_bar << endl;

	//29 A_bar
	for (int i = 0; i < n; i++)
	{
		ost << A_bar->at(i) << endl;
	}

	//30 r_A_bar
	ost << r_A_bar << endl;

	//31 D_s_bar
	for (int i = 0; i < n; i++)
	{
		ost << D_s_bar->at(i) << endl;
	}

	//32 r_Ds_bar
	ost << r_Ds_bar << endl;

	//33 r_Dl_bar
	ost << r_Dl_bar << endl;

	//34 B_bar
	for (int i = 0; i < n; i++)
	{
		ost << B_bar->at(i) << endl;
	}

	//35 r_B_bar
	ost << r_B_bar << endl;

	//36 a_bar
	ost << a_bar << endl;

	//37 r_a_bar
	ost << r_a_bar << endl;

	//38 rho_bar
	ost << rho_bar << endl;

	//39 chal_x8_temp
	ost << chal_x8_temp << endl;

	//ost.close();
}

int Prover_toom::prove(array<string, 2> codes,string fileName)
{
	ost.open(fileName, ios::out);
	if (!ost)
	{
		cout << "[" << codes[0] << "] - " << "Can't create "<< fileName << endl;
		exit(1);
	}
	this->round_1();
	this->round_3();//生成一个挑战
	this->round_5();//生成两个挑战
	this->round_7();//生成两个挑战
	this->round_9();//生成一个挑战
	ost.close();
	return 0;
}

void Prover_toom::commit_ac()
{
	long i;
	ZZ ord = H.get_ord();

	for (i = 0; i < mu_h; i++)
	{
		a_c->at(i) = RandomBnd(ord);
		r_c->at(i) = RandomBnd(ord);
		rho_c->at(i) = RandomBnd(ord);
	}
	a_c->at(mu - 1) = to_ZZ(0);
	r_c->at(mu - 1) = to_ZZ(0);
	NegateMod(rho_c->at(mu - 1), R_b, ord);
	for (i = 0; i < mu_h; i++)
	{
		c_a_c->at(i) = Ped.commit_sw(a_c->at(i), r_c->at(i));
	}
}

void Prover_toom::calculate_Cc(vector<vector<Cipher_elg>*>* C, vector<vector<vector<long>*>*>* B)
{
	long i, j, l, k;
	ZZ mod = H.get_mod();
	ZZ gen = H.get_gen().get_val();
	Cipher_elg temp, temp_1;
	ZZ t_1;
	double tstart, tstop;

	tstart = (double)clock() / CLOCKS_PER_SEC;
	for (k = 0; k < mu_h; k++)
	{
		temp = Cipher_elg(1, 1, mod);
		for (i = 0; i < mu; i++)
		{
			j = k + 1 - mu + i;
			if (j >= 0 & j < mu)
			{
				for (l = 0; l < m_r; l++)
				{
					multi_expo::expo_mult(temp_1, C->at(4 * l + i), B->at(4 * l + j), omega_mulex);
					Cipher_elg::mult(temp, temp, temp_1);
				}
			}
		}
		PowerMod(t_1, gen, a_c->at(k), mod);
		temp_1 = El.encrypt(t_1, rho_c->at(k));
		Cipher_elg::mult(C_c->at(k), temp, temp_1);
	}
	tstop = (double)clock() / CLOCKS_PER_SEC;
	time_di = 0;
	time_di = time_di + (tstop - tstart);
}

void Prover_toom::calculate_Cc(vector<vector<Cipher_elg>*>* C, vector<vector<ZZ>*>* B)
{
	long i, j, l, k;
	ZZ mod = H.get_mod();
	ZZ gen = H.get_gen().get_val();
	Cipher_elg temp, temp_1;
	ZZ t_1;
	double tstart, tstop;

	tstart = (double)clock() / CLOCKS_PER_SEC;
	for (k = 0; k < mu_h; k++)
	{
		temp = Cipher_elg(1, 1, mod);
		for (i = 0; i < mu; i++)
		{
			j = k + 1 - mu + i;
			if (j >= 0 & j < mu)
			{
				for (l = 0; l < m_r; l++)
				{
					multi_expo::expo_mult(temp_1, C->at(4 * l + i), B->at(4 * l + j), omega_mulex);
					Cipher_elg::mult(temp, temp, temp_1);
				}
			}
		}
		PowerMod(t_1, gen, a_c->at(k), mod);
		temp_1 = El.encrypt(t_1, rho_c->at(k));
		Cipher_elg::mult(C_c->at(k), temp, temp_1);
	}
	tstop = (double)clock() / CLOCKS_PER_SEC;
	time_di = 0;
	time_di = time_di + (tstop - tstart);
}

void Prover_toom::calculate_ac_bar(vector<ZZ>* x)
{
	long i;
	ZZ temp;
	ZZ ord = H.get_ord();

	a_c_bar = a_c->at(0);
	for (i = 1; i < mu_h; i++)
	{
		MulMod(temp, a_c->at(i), x->at(i - 1), ord);
		AddMod(a_c_bar, a_c_bar, temp, ord);
	}
}

void Prover_toom::calculate_r_ac_bar(vector<ZZ>* x)
{
	long i;
	ZZ temp;
	ZZ ord = H.get_ord();

	r_ac_bar = r_c->at(0);
	for (i = 1; i < mu_h; i++)
	{
		MulMod(temp, r_c->at(i), x->at(i - 1), ord);
		AddMod(r_ac_bar, r_ac_bar, temp, ord);
	}
}

void Prover_toom::reduce_C(vector<vector<Cipher_elg>*>* C, vector<vector<ZZ>*>* B, vector<ZZ>* r_B, vector<ZZ>* x, long length)
{
	long i, j;
	ZZ temp, temp_1;
	ZZ ord = H.get_ord();
	double tstart, tstop;
	vector<Cipher_elg>* row_C = 0;
	vector<ZZ>* row_B = 0;
	vector<ZZ>* x_temp = new vector<ZZ>(4);

	tstart = (double)clock() / CLOCKS_PER_SEC;
	x_temp->at(3) = 1;
	x_temp->at(2) = x->at(0);
	x_temp->at(1) = x->at(1);
	x_temp->at(0) = x->at(2);

	for (i = 0; i < length; i++)
	{
		row_C = new vector<Cipher_elg>(n);
		row_B = new vector<ZZ>(n);
		for (j = 0; j < n; j++)
		{
			multi_expo::multi_expo_LL(row_C->at(j), C->at(4 * i)->at(j), C->at(4 * i + 1)->at(j), C->at(4 * i + 2)->at(j), C->at(4 * i + 3)->at(j), x_temp, omega_LL);
			temp = B->at(4 * i)->at(j);
			MulMod(temp_1, B->at(4 * i + 1)->at(j), x_temp->at(2), ord);
			AddMod(temp, temp, temp_1, ord);
			MulMod(temp_1, B->at(4 * i + 2)->at(j), x_temp->at(1), ord);
			AddMod(temp, temp, temp_1, ord);
			MulMod(temp_1, B->at(4 * i + 3)->at(j), x_temp->at(0), ord);
			AddMod(temp, temp, temp_1, ord);
			row_B->at(j) = temp;
		}
		C_small->at(i) = row_C;
		B_small->at(i) = row_B;
		temp = r_B->at(4 * i);
		MulMod(temp_1, r_B->at(4 * i + 1), x_temp->at(2), ord);
		AddMod(temp, temp, temp_1, ord);
		MulMod(temp_1, r_B->at(4 * i + 2), x_temp->at(1), ord);
		AddMod(temp, temp, temp_1, ord);
		MulMod(temp_1, r_B->at(4 * i + 3), x_temp->at(0), ord);
		AddMod(temp, temp, temp_1, ord);
		r_B_small->at(i) = temp;
	}
	tstop = (double)clock() / CLOCKS_PER_SEC;
	time_di = time_di + (tstop - tstart);
	delete x_temp;
}

void Prover_toom::set_Rb1(vector<ZZ>* x)
{
	long i;
	ZZ temp;
	ZZ ord = H.get_ord();

	R_b = rho_c->at(0);
	for (i = 1; i < mu_h; i++)
	{
		MulMod(temp, rho_c->at(i), x->at(i - 1), ord);
		AddMod(R_b, R_b, temp, ord);
	}
}

vector<Cipher_elg>* Prover_toom::calculate_e()
{
	long k, l;
	Cipher_elg temp;
	ZZ ord = H.get_ord();
	ZZ mod = H.get_mod();
	vector<Cipher_elg>* dt = 0;
	vector<Cipher_elg>* e = new vector<Cipher_elg>(2 * m);

	dt = toom4_pow(C_small, B_small);

	multi_expo::expo_mult(e->at(0), C_small->at(mu - 1), basis_B0, omega_mulex);
	for (k = 1; k < mu; k++)
	{
		multi_expo::expo_mult(temp, C_small->at(mu - k - 1), basis_B0, omega_mulex);
		Cipher_elg::mult(e->at(k), temp, dt->at(2 * mu - k - 1));
	}
	l = 2 * mu;
	for (k = mu; k < l; k++)
	{
		e->at(k) = dt->at(2 * mu - k - 1);
	}

	delete dt;
	return e;
}

void Prover_toom::calculate_E(vector<Cipher_elg>* e)
{
	long i, l;
	Mod_p t;
	Mod_p gen = H.get_gen();
	ZZ ord = H.get_ord();

	l = 2 * mu;
	for (i = 0; i < l; i++)
	{
		rho_a->at(i) = RandomBnd(ord);
	}
	rho_a->at(mu) = R_b;
	for (i = 0; i < l; i++)
	{
		t = gen.expo(a->at(i));
		E->at(i) = El.encrypt(t, rho_a->at(i)) * e->at(i);
	}
}

vector<vector<Cipher_elg>*>* Prover_toom::copy_C()
{
	long i, j, l;
	vector<Cipher_elg>* row_C;
	l = mu * m_r;
	vector<vector<Cipher_elg>*>* C_small_temp = new vector<vector<Cipher_elg>*>(l);

	for (i = 0; i < l; i++)
	{
		row_C = new vector<Cipher_elg>(n);
		for (j = 0; j < n; j++)
		{
			row_C->at(j) = C_small->at(i)->at(j);
		}
		C_small_temp->at(i) = row_C;
		delete C_small->at(i);
		C_small->at(i) = 0;
	}
	delete C_small;
	C_small = 0;

	return C_small_temp;
}

vector<vector<ZZ>*>* Prover_toom::copy_B()
{
	long i, j;
	long l = mu * m_r;
	vector<vector<ZZ>*>* B_small_temp = new vector<vector<ZZ>*>(l);
	vector<ZZ>* row_B;

	for (i = 0; i < l; i++)
	{
		row_B = new vector<ZZ>(n);
		for (j = 0; j < n; j++)
		{
			row_B->at(j) = B_small->at(i)->at(j);
		}
		B_small_temp->at(i) = row_B;
		delete B_small->at(i);
		B_small->at(i) = 0;
	}
	delete B_small;

	return B_small_temp;
}

vector<ZZ>* Prover_toom::copy_r_B()
{
	long i;
	long l = mu * m_r;
	vector<ZZ>* r_B_small_temp = new vector<ZZ>(l);
	for (i = 0; i < l; i++)
	{
		r_B_small_temp->at(i) = r_B_small->at(i);
	}
	delete r_B_small;
	r_B_small = 0;

	return r_B_small_temp;
}

vector<vector<ZZ>*>* Prover_toom::evulation(vector<vector<ZZ>*>* p)
{
	vector<vector<ZZ>*>* ret;
	vector<ZZ>* row;
	ZZ p0, p1, p2, p3, ord, temp, temp_1;
	long l, i;
	l = p->at(0)->size();
	ord = H.get_ord();
	ret = new vector<vector<ZZ>*>(l);

	for (i = 0; i < l; i++)
	{
		row = new vector<ZZ>(7);
		AddMod(p0, p->at(2)->at(i), p->at(0)->at(i), ord);
		AddMod(p1, p->at(3)->at(i), p->at(1)->at(i), ord);
		MulMod(temp, p->at(2)->at(i), 2, ord);
		MulMod(temp_1, p->at(0)->at(i), 8, ord);
		AddMod(p2, temp, temp_1, ord);
		MulMod(temp, p->at(1)->at(i), 4, ord);
		AddMod(p3, p->at(3)->at(i), temp, ord);

		row->at(0) = p->at(3)->at(i);
		MulMod(temp_1, p->at(1)->at(i), 2, ord);
		AddMod(temp, temp_1, p->at(0)->at(i), ord);
		MulMod(temp_1, p->at(2)->at(i), 4, ord);
		AddMod(temp, temp, temp_1, ord);
		MulMod(temp_1, p->at(3)->at(i), 8, ord);
		AddMod(row->at(1), temp, temp_1, ord);
		AddMod(row->at(2), p0, p1, ord);
		SubMod(row->at(3), p0, p1, ord);
		AddMod(row->at(4), p2, p3, ord);
		SubMod(row->at(5), p2, p3, ord);
		row->at(6) = p->at(0)->at(i);
		ret->at(i) = row;
	}
	return ret;
}

vector<vector<vector<ZZ>*>*>* Prover_toom::evulation_pow(vector<vector<Cipher_elg>*>* p)
{
	vector<vector<vector<ZZ>*>*>* ret;
	vector<vector<ZZ>*>* ret_u;
	vector<vector<ZZ>*>* ret_v;
	vector<ZZ>* row_u;
	vector<ZZ>* row_v;
	ZZ p0_u, p1_u, p2_u, p3_u, temp_u, temp_1_u;
	ZZ p0_v, p1_v, p2_v, p3_v, temp_v, temp_1_v;
	ZZ mod = H.get_mod();
	long l, i;
	l = p->at(0)->size();

	ret = new vector<vector<vector<ZZ>*>*>(2);
	ret_u = new vector<vector<ZZ>*>(l);
	ret_v = new vector<vector<ZZ>*>(l);

	for (i = 0; i < l; i++)
	{
		row_u = new vector<ZZ>(7);
		row_v = new vector<ZZ>(7);
		MulMod(p0_u, p->at(1)->at(i).get_u(), p->at(3)->at(i).get_u(), mod);
		MulMod(p0_v, p->at(1)->at(i).get_v(), p->at(3)->at(i).get_v(), mod);
		MulMod(p1_u, p->at(0)->at(i).get_u(), p->at(2)->at(i).get_u(), mod);
		MulMod(p1_v, p->at(0)->at(i).get_v(), p->at(2)->at(i).get_v(), mod);
		PowerMod(temp_u, p->at(1)->at(i).get_u(), 2, mod);
		PowerMod(temp_v, p->at(1)->at(i).get_v(), 2, mod);
		PowerMod(temp_1_u, p->at(3)->at(i).get_u(), 8, mod);
		PowerMod(temp_1_v, p->at(3)->at(i).get_v(), 8, mod);
		MulMod(p2_u, temp_u, temp_1_u, mod);
		MulMod(p2_v, temp_v, temp_1_v, mod);
		PowerMod(temp_u, p->at(2)->at(i).get_u(), 4, mod);
		PowerMod(temp_v, p->at(2)->at(i).get_v(), 4, mod);
		MulMod(p3_u, p->at(0)->at(i).get_u(), temp_u, mod);
		MulMod(p3_v, p->at(0)->at(i).get_v(), temp_v, mod);

		row_u->at(0) = p->at(0)->at(i).get_u();
		PowerMod(temp_u, p->at(2)->at(i).get_u(), 2, mod);
		MulMod(temp_u, temp_u, p->at(3)->at(i).get_u(), mod);
		PowerMod(temp_1_u, p->at(1)->at(i).get_u(), 4, mod);
		MulMod(temp_u, temp_u, temp_1_u, mod);
		PowerMod(temp_1_u, p->at(0)->at(i).get_u(), 8, mod);
		MulMod(row_u->at(1), temp_u, temp_1_u, mod);
		MulMod(row_u->at(2), p0_u, p1_u, mod);
		InvMod(temp_u, p1_u, mod);
		MulMod(row_u->at(3), p0_u, temp_u, mod);
		MulMod(row_u->at(4), p2_u, p3_u, mod);
		InvMod(temp_u, p3_u, mod);
		MulMod(row_u->at(5), p2_u, temp_u, mod);
		row_u->at(6) = p->at(3)->at(i).get_u();

		row_v->at(0) = p->at(0)->at(i).get_v();
		PowerMod(temp_v, p->at(2)->at(i).get_v(), 2, mod);
		MulMod(temp_v, temp_v, p->at(3)->at(i).get_v(), mod);
		PowerMod(temp_1_v, p->at(1)->at(i).get_v(), 4, mod);
		MulMod(temp_v, temp_v, temp_1_v, mod);
		PowerMod(temp_1_v, p->at(0)->at(i).get_v(), 8, mod);
		MulMod(row_v->at(1), temp_v, temp_1_v, mod);
		MulMod(row_v->at(2), p0_v, p1_v, mod);
		InvMod(temp_v, p1_v, mod);
		MulMod(row_v->at(3), p0_v, temp_v, mod);
		MulMod(row_v->at(4), p2_v, p3_v, mod);
		InvMod(temp_v, p3_v, mod);
		MulMod(row_v->at(5), p2_v, temp_v, mod);
		row_v->at(6) = p->at(3)->at(i).get_v();

		ret_u->at(i) = row_u;
		ret_v->at(i) = row_v;
	}
	ret->at(0) = ret_u;
	ret->at(1) = ret_v;
	return ret;
}

vector<vector<vector<ZZ>*>*>* Prover_toom::point_pow(vector<vector<vector<ZZ>*>*>* points_p, vector<vector<ZZ>*>* points_q)
{
	long i, j, l;
	vector<vector<vector<ZZ>*>*>* ret;
	vector<vector<ZZ>*>* ret_u;
	vector<vector<ZZ>*>* ret_v;
	vector<ZZ>* row_u;
	vector<ZZ>* row_v;
	ZZ mod = H.get_mod();
	l = points_p->at(0)->size();

	ret = new vector<vector<vector<ZZ>*>*>(2);
	ret_u = new vector<vector<ZZ>*>(l);
	ret_v = new vector<vector<ZZ>*>(l);
	for (j = 0; j < l; j++)
	{
		row_u = new vector<ZZ>(7);
		row_v = new vector<ZZ>(7);
		for (i = 0; i < 7; i++)
		{
			PowerMod(row_u->at(i), points_p->at(0)->at(j)->at(i), points_q->at(j)->at(i), mod);
			PowerMod(row_v->at(i), points_p->at(1)->at(j)->at(i), points_q->at(j)->at(i), mod);
		}
		ret_u->at(j) = row_u;
		ret_v->at(j) = row_v;
	}
	ret->at(0) = ret_u;
	ret->at(1) = ret_v;
	for (i = 0; i < l; i++)
	{
		delete points_p->at(0)->at(i);
		points_p->at(0)->at(i) = 0;
		delete points_p->at(1)->at(i);
		points_p->at(1)->at(i) = 0;
	}
	delete points_p->at(0);
	delete points_p->at(1);
	delete points_p;
	for (i = 0; i < l; i++)
	{
		delete points_q->at(i);
		points_q->at(i) = 0;
	}
	delete points_q;
	return ret;
}

vector<vector<ZZ>*>* Prover_toom::mult_points(vector<vector<vector<ZZ>*>*>* points)
{
	long i, l, j;
	vector<vector<ZZ>*>* ret = new vector<vector<ZZ>*>(2);
	vector<ZZ>* ret_u = new vector<ZZ>(7);
	vector<ZZ>* ret_v = new vector<ZZ>(7);
	l = points->at(0)->size();
	ZZ temp_u, temp_v;
	ZZ mod = H.get_mod();

	for (i = 0; i < 7; i++)
	{
		temp_u = 1;
		temp_v = 1;
		for (j = 0; j < l; j++)
		{
			MulMod(temp_u, temp_u, points->at(0)->at(j)->at(i), mod);
			MulMod(temp_v, temp_v, points->at(1)->at(j)->at(i), mod);
		}
		ret_u->at(i) = temp_u;
		ret_v->at(i) = temp_v;
	}
	for (i = 0; i < l; i++)
	{
		delete points->at(0)->at(i);
		points->at(0)->at(i) = 0;
		delete points->at(1)->at(i);
		points->at(1)->at(i) = 0;
	}
	delete points->at(0);
	points->at(0) = 0;
	delete points->at(1);
	points->at(1) = 0;
	delete points;
	ret->at(0) = ret_u;
	ret->at(1) = ret_v;
	return ret;
}

vector<ZZ>* Prover_toom::interpolation_pow(vector<ZZ>* points)
{
	vector<ZZ>* ret = new vector<ZZ>(7);
	ZZ r1, r2, r3, r4, r5, r6, r7, temp;
	ZZ ord = H.get_ord();
	ZZ mod = H.get_mod();

	r1 = points->at(0);
	r2 = points->at(1);
	r3 = points->at(2);
	r4 = points->at(3);
	r5 = points->at(4);
	r6 = points->at(5);
	r7 = points->at(6);

	MulMod(r2, r2, r5, mod);
	InvMod(temp, r5, mod);
	MulMod(r6, r6, temp, mod);
	InvMod(temp, r3, mod);
	MulMod(r4, r4, temp, mod);
	InvMod(temp, r1, mod);
	MulMod(r5, r5, temp, mod);
	PowerMod(temp, r7, 64, mod);
	InvMod(temp, temp, mod);
	MulMod(r5, r5, temp, mod);
	InvMod(temp, to_ZZ(2), ord);
	PowerMod(r4, r4, temp, mod);
	MulMod(r3, r3, r4, mod);
	PowerMod(temp, r5, 2, mod);
	MulMod(r5, temp, r6, mod);

	PowerMod(temp, r3, 65, mod);
	InvMod(temp, temp, mod);
	MulMod(r2, r2, temp, mod);
	PowerMod(r4, r4, -1, mod);
	PowerMod(r6, r6, -1, mod);
	InvMod(temp, r7, mod);
	MulMod(r3, r3, temp, mod);
	InvMod(temp, r1, mod);
	MulMod(r3, r3, temp, mod);
	PowerMod(temp, r3, 45, mod);
	MulMod(r2, r2, temp, mod);
	PowerMod(temp, r3, 8, mod);
	InvMod(temp, temp, mod);
	MulMod(r5, r5, temp, mod);

	InvMod(temp, to_ZZ(24), ord);
	PowerMod(r5, r5, temp, mod);
	InvMod(temp, r2, mod);
	MulMod(r6, r6, temp, mod);
	PowerMod(temp, r4, 16, mod);
	InvMod(temp, temp, mod);
	MulMod(r2, r2, temp, mod);
	InvMod(temp, to_ZZ(18), ord);
	PowerMod(r2, r2, temp, mod);
	InvMod(temp, r5, mod);
	MulMod(r3, r3, temp, mod);
	InvMod(temp, r2, mod);
	MulMod(r4, r4, temp, mod);
	PowerMod(temp, r2, 30, mod);
	MulMod(r6, r6, temp, mod);
	InvMod(temp, to_ZZ(60), ord);
	PowerMod(r6, r6, temp, mod);
	InvMod(temp, r6, mod);
	MulMod(r2, r2, temp, mod);

	ret->at(0) = r1;
	ret->at(1) = r2;
	ret->at(2) = r3;
	ret->at(3) = r4;
	ret->at(4) = r5;
	ret->at(5) = r6;
	ret->at(6) = r7;

	return ret;
}

vector<Cipher_elg>* Prover_toom::toom4_pow(vector<vector<Cipher_elg>*>* p, vector<vector<ZZ>*>* q)
{
	vector<vector<vector<ZZ>*>*>* points_p;
	vector<vector<ZZ>*>* points_q;
	vector<vector<vector<ZZ>*>*>* points_temp;
	vector<vector<ZZ>*>* points;
	vector<ZZ>* ret_u;
	vector<ZZ>* ret_v;
	vector<Cipher_elg>* ret = new vector<Cipher_elg>(7);
	long i, l;
	ZZ mod = H.get_mod();
	points_p = evulation_pow(p);
	points_q = evulation(q);
	points_temp = point_pow(points_p, points_q);
	points = mult_points(points_temp);
	ret_u = interpolation_pow(points->at(0));
	ret_v = interpolation_pow(points->at(1));
	l = points->size();
	for (i = 0; i < l; i++)
	{
		delete points->at(i);
		points->at(i) = 0;
	}
	delete points;

	for (i = 0; i < 7; i++)
	{
		ret->at(i) = Cipher_elg(ret_u->at(i), ret_v->at(i), mod);
	}
	delete ret_u;
	delete ret_v;
	return ret;
}
