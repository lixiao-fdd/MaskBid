#include"paraGen.h"
//生成参数
int ParaGen::parametersGen(int lp, int lq) {
	int flag = 1, index = 0;
	pq = new vector<ZZ>(6);
	while (flag || pq->at(0) != pq->at(4))
	{
		SetSeed(to_ZZ((unsigned int)time(0) + clock()));
		//cout << endl;
		flag = find_groups(pq, lq, lp, lp, 16);
		if (++index > exitMax)
		{
			cout << "\nParameter generation failed" << endl;
			return(1);
		}
	}
	string fileName = filesPath + "parameters.txt";
	ost.open(fileName, ios::out);
	if (!ost)
	{
		cout << "Can't create " << fileName << endl;
		exit(1);
	}
	ost << pq->at(0) << endl;//p
	ost << pq->at(1) << endl;//q
	ost << pq->at(2) << endl;//gen_h
	ost << pq->at(5) << endl;//gen_g
	ost.close();
	return 0;
}
//finds prime numbers q,p such that p = 2*a*q+1 using test provided by Mau94, lp,lq are the number of bits of q,p
int ParaGen::find_group(vector<ZZ>* pq, long lq, long lp, long m)
{
	long l, i, j, logl, index = 0;
	ZZ mod30;
	ZZ q, q1, q2, p, a, an, gcd, gcd_1, gcd_2, temp, temp_1, gen, gen2, genq;
	bool b, bo, bol;
	int count = 0;
	//q-1 needs to be divisible by 2*m, such that we can find a 2m-root of unity
	//cout << "Expectation: p " << lp << " bit | q " << lq << " bit" << endl;
	if ((lp - lq) > 2)
	{
		bol = false;
		while (bol == false)
		{
			l = lq - NumBits(2 * m);
			//generates q as 2*2*m*q1*q2+1 and tests if q can be prime
			b = false;
			bol = true;
			while (b == false)
			{
				b = new_q(q, q1, q2, m, l);
				cout << "." << flush;
			}
			b = false;
			while (b == false)
			{
				b = check_q(a, q, q1, q2, m, l);
				cout << "." << flush;
			}
			genq = a;

			l = lp - lq;
			bo = false;
			//Generate p as 2*q*q1+1 and test if p is possible prime
			while (bo == false)
			{
				bo = new_p(p, q1, q, l);
				//bo=new_p(p,q1, q2,q,l);
				cout << "." << flush;
				if (index++ > threshold)
					return 1;
			}
			logl = 20 * log(l);
			j = 0;
			b = false;
			//If after log tries no p=2*q*q1+1 is prime a new q is picked
			while (j < logl && b == false)
			{
				b = true;

				if (q1 * q2 > q)
				{
					b = check_p(a, p, q1, q, l, j);
					//b=check_p(a, p, q1, q2,q,l,j);
				}
				else
				{
					b = check_p(a, p, q, q1, l, j);
				}
			}
			if (j == logl)
			{
				bol = false;
			}
		}
		//Generator of G_q in Z_p
		gen = PowerMod(a, 2 * q1, p);
	}
	else
	{ //Sophie Germain prime p=2*q+1
		bol = false;
		count = 0;
		while (bol == false)
		{
			l = lq - NumBits(2 * m);
			b = false;
			count++;
			while (b == false)
			{
				//Generate q as 2*2*m*q1*q2+1
				b = new_q(q, q1, q2, m, l);

				//q has to be 11,23,39 mod 30 to be part of a Sophie Germain prime
				mod30 = q % 30;
				if (mod30 == to_ZZ(11))
				{
					b = true;
				}
				if (mod30 == to_ZZ(23))
				{
					b = true;
				}
				if (mod30 == to_ZZ(29))
				{
					b = true;
				}
				cout << "." << flush;
			}
			b = false;
			while (b == false)
			{
				b = check_q(a, q, q1, q2, m, l);
				cout << "." << flush;
			}
			genq = a;

			p = 2 * q + 1;
			bol = probPrime(p);
			if (bol == true)
			{
				//Checks for random a, if a is a generator
				for (i = 0; i < 100; i++)
				{
					a = RandomBnd(p);
					an = PowerMod(a, p - 1, p);
					if (a != 1 && an == 1)
					{
						temp = PowerMod(a, q, p);
						if (temp != 1)
						{
							break;
						}
					}
				}
				if (i == 100)
				{
					bol = false;
				}
			}
		}
		//Generator of G_q in Z_p
		gen = PowerMod(a, 2, p);
	}
	pq->at(0) = p;
	pq->at(1) = q;
	pq->at(2) = gen;
	//Generator of Z_q
	pq->at(3) = genq;
	return 0;
}
//finds prime numbers q,p, p1 such that p = 2*a*q+1 and p1=2*b*q+1 using test provided by Mau94, lp,lq are the number of bits of q,p
int ParaGen::find_groups(vector<ZZ>* pq, long lq, long lp, long lp1, long m)
{
	ZZ q, q1, p1, a, gen;
	bool b, bo, bol;
	long logl, l, j;
	string name;
	int flag = 0, index = 0;

	bol = false;
	while (bol == false)
	{
		bol = true;
		flag = find_group(pq, lq, lp, m);
		if (flag)
			return flag;

		q = pq->at(1);
		l = lp1 - lq;
		if (l <= 1)
			exit(1);
		//Generate p1 as 2*q*q1+1 and test if p1 is possible prime
		bo = false;
		while (bo == false)
		{
			bo = new_p(p1, q1, q, l);
			cout << "." << flush;
			if (index++ > threshold)
				return 1;
		}
		logl = log(l);
		j = 0;
		b = false;
		//If after log tries no p=2*q*q1+1 is prime a new q and p is picked
		while (j < logl && b == false)
		{
			b = true;

			if (q1 > q)
			{
				b = check_p(a, p1, q1, q, l, j);
			}
			else
			{
				b = check_p(a, p1, q, q1, l, j);
			}
			cout << "." << flush;
		}
		if (j == logl)
		{
			bol = false;
		}
	}
	gen = PowerMod(a, 2 * q1, p1);

	pq->at(4) = p1;
	pq->at(5) = gen;
	return 0;
}

bool ParaGen::new_q(ZZ& q, ZZ& q1, ZZ& q2, long m, long l)
{
	bool b;

	//Generate q as 2*2*m*q1*q2+1
	q1 = GenPrime_ZZ(l / 2 + 1);
	q2 = GenPrime_ZZ(l / 2 - 1);
	q = 2 * 2 * m * q1 * q2 + 1;

	b = probPrime(q);
	return b;
}
//Checks if a integer q is probably prime, calls the MillerRabin Test only with 1 witness
bool ParaGen::probPrime(ZZ q)
{
	bool b;

	b = false;
	if (q % 3 == 0)
	{
	}
	else if (q % 5 == 0)
	{
	}
	else if (q % 7 == 0)
	{
	}
	else if (q % 11 == 0)
	{
	}
	else if (q % 13 == 0)
	{
	}
	else if (q % 17 == 0)
	{
	}
	else if (q % 19 == 0)
	{
	}
	else if (ProbPrime(q, 1) == 0)
	{
	}
	else
		b = true;

	return b;
}
bool ParaGen::check_q(ZZ& a, ZZ& q, ZZ& q1, ZZ& q2, long m, long l)
{
	bool b, bo;
	long i;

	b = true;
	//Test condition of Lemma 1 of Mau94 with F=2*m*q1 and R = q2, F>sqrt(q) with different random integers a
	i = checkL1(a, q, q1);
	//If no a satisfies condition of Lemma 1, new values for q1, q2 are picked
	if (i == 100)
	{
		bo = false;
		while (bo == false)
		{
			bo = new_q(q, q1, q2, m, l);
		}
		b = false;
	}
	else
	{
		//checks if a is primitive (p.143 Mau94)
		b = checkPow(a, q2, q);
	}
	return b;
}
long ParaGen::checkL1(ZZ& a, ZZ q, ZZ q1)
{
	long i;
	ZZ an;

	for (i = 0; i < 100; i++)
	{
		a = RandomBnd(q);
		an = PowerMod(a, q - 1, q);
		if (a != 1 && an == 1 && checkGCD(a, q1, q) && checkGCD(a, to_ZZ(2), q))
		{
			break;
		}
	}

	return i;
}
bool ParaGen::checkPow(ZZ a, ZZ q1, ZZ q)
{
	bool b;
	ZZ temp;

	b = true;
	temp = PowerMod(a, (q - 1) / q1, q);
	if (temp == 1)
	{
		b = false;
	}
	return b;
}
bool ParaGen::new_p(ZZ& p, ZZ& q1, ZZ q, long l)
{
	bool b;
	q1 = GenPrime_ZZ(l);
	p = 2 * q * q1 + 1;
	//cout<<" in new p: ";
	b = probPrime(p);
	return b;
}
bool ParaGen::check_p(ZZ& a, ZZ& p, ZZ& q1, ZZ q, long l, long& j)
{
	bool b, bo;
	long i;

	b = true;
	//cout<<" in check";
	i = checkL1(a, p, q1);
	//If no a satisfies the condition, pick new prime q1
	if (i == 100)
	{
		bo = false;
		while (bo == false)
		{
			bo = new_p(p, q1, q, l);
		}
		b = false;
		j++;
	}
	else
	{ //Test if a is primitive, following Mau94 p.143
		b = checkPow(a, q, p);
	}
	return b;
}
bool ParaGen::checkGCD(ZZ a, ZZ q1, ZZ q)
{
	bool b;
	ZZ temp, gcd;

	b = false;

	temp = PowerMod(a, (q - 1) / q1, q);
	gcd = GCD(temp - 1, q);
	if (gcd == 1)
	{
		b = true;
	}
	return b;
}
