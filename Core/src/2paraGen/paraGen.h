#pragma once
#include "../global.h"
class ParaGen {
private:
	ofstream ost;
	vector<ZZ>* pq;
	int exitMax = 10;
	int threshold = 100;

	//finds prime numbers q,p such that p = 2*a*q+1 using test provided by Mau94, lp,lq are the number of bits of q,p
	int find_group(vector<ZZ>* pq, long lq, long lp, long m);
	//finds prime numbers q,p, p1 such that p = 2*a*q+1 and p1=2*b*q+1 using test provided by Mau94, lp,lq are the number of bits of q,p
	int find_groups(vector<ZZ>* pq, long lq, long lp, long lp1, long m);

	bool new_q(ZZ& q, ZZ& q1, ZZ& q2, long m, long l);
	//Checks if a integer q is probably prime, calls the MillerRabin Test only with 1 witness
	bool probPrime(ZZ q);
	bool check_q(ZZ& a, ZZ& q, ZZ& q1, ZZ& q2, long m, long l);
	long checkL1(ZZ& a, ZZ q, ZZ q1);
	bool checkPow(ZZ a, ZZ q1, ZZ q);
	bool new_p(ZZ& p, ZZ& q1, ZZ q, long l);
	bool check_p(ZZ& a, ZZ& p, ZZ& q1, ZZ q, long l, long& j);
	bool checkGCD(ZZ a, ZZ q1, ZZ q);

public:
	//生成参数,lp为p的位长，lq为q的位长
	int parametersGen(int lp, int lq);
};