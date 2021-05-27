#ifndef ELGAMMAL_H_
#define ELGAMMAL_H_
#include "../0base/Mod_p.h"
#include "../0base/G_q.h"
#include "../0base/Cipher_elg.h"
NTL_CLIENT

class ElGamal {
private:
	G_q G;  //Group used for encryption
	ZZ sk;	//secret key
	Mod_p pk_1; //public key self
	Mod_p pk; //public key
public:
	ZZ sk_main_debug;
	//Constructor & destructor
	ElGamal();
	ElGamal(long s, Mod_p p, G_q H);
	ElGamal(ZZ s, Mod_p p, G_q H);
	ElGamal(long s, G_q H);
	ElGamal(ZZ s, G_q H);
	ElGamal(Mod_p gen, long o, long mod, long s);
	ElGamal(Mod_p gen, long o, ZZ mod, long s);
	ElGamal(Mod_p gen, long o, ZZ mod, ZZ s);
	ElGamal(Mod_p gen, ZZ o, ZZ mod, long s);
	ElGamal(Mod_p gen, ZZ o, ZZ mod, ZZ s);
	ElGamal(Mod_p gen, long o, long mod, long s, Mod_p p);
	ElGamal(Mod_p gen, long o, ZZ mod, long s, Mod_p p);
	ElGamal(Mod_p gen, long o, ZZ mod, ZZ s, Mod_p p);
	ElGamal(Mod_p gen, ZZ o, ZZ mod, long s, Mod_p p);
	ElGamal(Mod_p gen, ZZ o, ZZ mod, ZZ s, Mod_p p);
	ElGamal(long o, long mod, long s, Mod_p p);
	ElGamal(long o, ZZ mod, long s, Mod_p p);
	ElGamal(ZZ o, ZZ mod, long s, Mod_p p);
	ElGamal(long o, ZZ mod, ZZ s, Mod_p p);
	ElGamal(ZZ o, ZZ mod, ZZ s, Mod_p p);
	ElGamal(long o, long mod, long s);
	ElGamal(long o, ZZ mod, long s);
	ElGamal(ZZ o, ZZ mod, long s);
	ElGamal(long o, ZZ mod, ZZ s);
	ElGamal(ZZ o, ZZ mod, ZZ s);
	virtual ~ElGamal();

	//Access to the variables
	G_q get_group() const;
	Mod_p get_pk_1() const;
	Mod_p get_pk() const;
	ZZ get_sk() const;

	//functions to change parameters
	void set_group(G_q G);
	void set_sk(ZZ s);
	void set_sk(long s);
	void set_pk(ZZ pk);
	void set_key(ZZ s, ZZ p);
	void keyGen(string pk_2);

	//encryption and decryption functions
	Cipher_elg encrypt(Mod_p m);
	Cipher_elg encrypt(ZZ m);
	Cipher_elg encrypt(long m);

	Cipher_elg encrypt(Mod_p m, long ran_1);
	Cipher_elg encrypt(Mod_p m, ZZ ran_1);
	Cipher_elg encrypt(ZZ m, long ran_1);
	Cipher_elg encrypt(ZZ m, ZZ ran_1);
	Cipher_elg encrypt(long m, long ran_1);
	Cipher_elg encrypt(long m, ZZ ran_1);
	Cipher_elg encrypt_g(ZZ m, ZZ ran_1);

	//decryption function
	ZZ decrypt(Cipher_elg c);
	//ZZ decrypt(Cipher_elg c, int flag);
	ZZ decrypt_debug(Cipher_elg c);
	ZZ get_m(ZZ temp);
	//Assigment operator
	void operator =(const ElGamal& el);

};

#endif /* ELGAMMAL_H_ */
