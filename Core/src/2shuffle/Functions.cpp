#include "Functions.h"
NTL_CLIENT

using namespace std;

extern G_q H;
extern ElGamal El;
extern Pedersen Ped;
//vector<vector<Cipher_elg>*>* c = 0;	  //原始输入的密文
//vector<vector<Cipher_elg>*>* C = 0;	  //重加密的密文
int threshold = 100;
int exitMax = 10;

#define DEBUG 1

Functions::Functions()
{
	// TODO Auto-generated constructor stub
}

Functions::~Functions()
{
	// TODO Auto-generated destructor stub
}

//Returns the Hadamard product of x and y
void Functions::Hadamard(vector<ZZ>* ret, vector<ZZ>* x, vector<ZZ>* y)
{

	long n, m, i;
	ZZ ord = H.get_ord();
	n = x->size();
	m = y->size();

	if (m != n)
	{
		cout << "Not possible" << endl;
	}
	else
	{
		for (i = 0; i < n; i++)
		{
			MulMod(ret->at(i), x->at(i), y->at(i), ord);
		}
	}
}

//returns the bilinear map of x and y, defined as x(y¡t)^T
ZZ Functions::bilinearMap(vector<ZZ>* x, vector<ZZ>* y, vector<ZZ>* t)
{
	long i, l;
	ZZ result, ord, tem;

	vector<ZZ>* temp = new vector<ZZ>(x->size());

	ord = H.get_ord();
	Hadamard(temp, y, t);
	l = x->size();
	result = 0;
	for (i = 0; i < l; i++)
	{
		MulMod(tem, x->at(i), temp->at(i), ord);
		AddMod(result, result, tem, ord);
	}
	delete temp;
	return result;
}

//help functions to delete matrices
void Functions::delete_vector(vector<vector<ZZ>*>* v)
{
	long i;
	long l = v->size();

	for (i = 0; i < l; i++)
	{
		delete v->at(i);
		v->at(i) = 0;
	}
	delete v;
}

void Functions::delete_vector(vector<vector<long>*>* v)
{
	long i;
	long l = v->size();

	for (i = 0; i < l; i++)
	{
		delete v->at(i);
		v->at(i) = 0;
	}
	delete v;
}
void Functions::delete_vector(vector<vector<Cipher_elg>*>* v)
{
	long i;
	long l = v->size();

	for (i = 0; i < l; i++)
	{
		delete v->at(i);
		v->at(i) = 0;
	}
	delete v;
}

void Functions::delete_vector(vector<vector<vector<long>*>*>* v)
{
	long i;
	long l = v->size();

	for (i = 0; i < l; i++)
	{
		delete_vector(v->at(i));
	}
	delete v;
}

void Functions::delete_vector(vector<vector<vector<ZZ>*>*>* v)
{
	long i;
	long l = v->size();

	for (i = 0; i < l; i++)
	{
		delete_vector(v->at(i));
	}
	delete v;
}

// help functions, which pick random values and commit to a vector/matrix
//picks random value r and commits to the vector a,
void Functions::commit(vector<ZZ>* a, ZZ& r, Mod_p& com)
{
	ZZ ord = H.get_ord();

	r = RandomBnd(ord);
	com = Ped.commit(a, r);
	/*string name = "example.txt";
	ofstream ost;
	ost.open(name.c_str(),ios::app);
	ost<<r<<" "<<com<<endl;*/
}

//picks random values r and commits to the rows of the matrix a, a,r,com are variables of Prover
void Functions::commit(vector<vector<ZZ>*>* a_in, vector<ZZ>* r, vector<Mod_p>* com)
{
	long i, l;
	ZZ ord = H.get_ord();

	l = a_in->size();

	/*	string name = "example.txt";
	ofstream ost;
	ost.open(name.c_str(),ios::app);*/
	for (i = 0; i < l; i++)
	{
		r->at(i) = RandomBnd(ord);
		//		ost<<r->at(i)<<" ";
	}
	//	ost<<endl;
	for (i = 0; i < l; i++)
	{
		com->at(i) = Ped.commit(a_in->at(i), r->at(i));
		//	ost<<com->at(i)<<" ";
	}
	//	ost<<endl;
}

//picks random value r and commits to the vector a,
void Functions::commit_op(vector<ZZ>* a, ZZ& r, Mod_p& com)
{
	ZZ ord = H.get_ord();

	r = RandomBnd(ord);
	com = Ped.commit_opt(a, r);
}

//picks random values r and commits to the rows of the matrix a, a,r,com are variables of Prover
void Functions::commit_op(vector<vector<ZZ>*>* a_in, vector<ZZ>* r, vector<Mod_p>* com)
{
	long i, l;
	ZZ ord = H.get_ord();

	l = a_in->size();

	for (i = 0; i < l; i++)
	{
		r->at(i) = RandomBnd(ord);
	}
	for (i = 0; i < l; i++)
	{
		com->at(i) = Ped.commit_opt(a_in->at(i), r->at(i));
	}
}
