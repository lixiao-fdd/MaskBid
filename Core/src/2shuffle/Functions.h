#ifndef FUNCTIONS_H_
#define FUNCTIONS_H_

#include "../global.h"
NTL_CLIENT

class Functions {
public:
	Functions();
	virtual ~Functions();
	static void Hadamard(vector<ZZ>* ret, vector<ZZ>* x, vector<ZZ>* y);
	//Calculates the bilinear map Z^n x Z^ -> Z: x(y¡t)^T
	static ZZ bilinearMap(vector<ZZ>* x, vector<ZZ>* y, vector<ZZ>* t);

	//help functions to delete matrices
	static void delete_vector(vector<vector<ZZ>* >* v);
	static void delete_vector(vector<vector<long>* >* v);
	static void delete_vector(vector<vector<Cipher_elg>* >* v);
	static void delete_vector(vector<vector<vector<long>* >*>* v);
	static void delete_vector(vector<vector<vector<ZZ>* >*>* v);

	// help functions, which pick random values and commit to a vector/matrix
	static void commit(vector<ZZ>* a, ZZ& r, Mod_p& com);
	static void commit(vector<vector<ZZ>* >* a, vector<ZZ>* r, vector<Mod_p>* com);
	static void commit_op(vector<ZZ>* a, ZZ& r, Mod_p& com);
	static void commit_op(vector<vector<ZZ>* >* a, vector<ZZ>* r, vector<Mod_p>* com);
};

#endif /* FUNCTIONS_H_ */
