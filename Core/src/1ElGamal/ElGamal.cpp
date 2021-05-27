#include "ElGamal.h"
NTL_CLIENT
#define DEBUG true
//ZZ sk_main_debug = conv<ZZ>("817266138476070071680325283");

ElGamal::ElGamal() {
	// TODO Auto-generated constructor stub

}

//Creates ElGamal with secret key s, public key p and group H
ElGamal::ElGamal(long s, Mod_p p, G_q H) {
	G = H;
	sk = to_ZZ(s);
	pk = p;

}

//Creates ElGamal with secret key s, public key p and group H
ElGamal::ElGamal(ZZ s, Mod_p p, G_q H) {
	G = H;
	sk = s;
	pk = p;

}

//Creates ElGamal with secret key s and group H, the public key is pk = gen^s , gen generator of H
ElGamal::ElGamal(long s, G_q H) {
	Mod_p temp;
	G = H;
	sk = to_ZZ(s);
	temp = Mod_p(G.get_gen().get_val(), G.get_mod());
	pk = temp.expo(s);
}

//Creates ElGamal with secret key s and group H, the public key is pk = gen^s , gen generator of H
ElGamal::ElGamal(ZZ s, G_q H) {
	Mod_p temp;
	G = H;
	sk = s;
	temp = Mod_p(G.get_gen().get_val(), G.get_mod());
	pk = temp.expo(s);
}

//Set the group to G_q with order o, G_q subset of G_mod_p and generator gen, secret key is s and pk = gen^s
ElGamal::ElGamal(Mod_p gen, long o, long  mod, long s) {

	G = G_q(gen, o, mod);
	sk = to_ZZ(s);
	Mod_p temp;
	temp = Mod_p(gen.get_val(), mod);
	pk = temp.expo(s);
}

//Set the group to G_q with order o, G_q subset of G_mod_p and generator gen, secret key is s and pk = gen^s
ElGamal::ElGamal(Mod_p gen, long o, ZZ  mod, long s) {

	G = G_q(gen, o, mod);
	sk = to_ZZ(s);
	Mod_p temp;
	temp = Mod_p(gen.get_val(), mod);
	pk = temp.expo(s);
}

//Set the group to G_q with order o, G_q subset of G_mod_p and generator gen, secret key is s and pk = gen^s
ElGamal::ElGamal(Mod_p gen, long o, ZZ  mod, ZZ s) {

	G = G_q(gen, o, mod);
	sk = s;
	Mod_p temp;
	temp = Mod_p(gen.get_val(), mod);
	pk = temp.expo(s);
}

//Set the group to G_q with order o, G_q subset of G_mod_p and generator gen, secret key is s and pk = gen^s
ElGamal::ElGamal(Mod_p gen, ZZ o, ZZ  mod, long s) {

	G = G_q(gen, o, mod);
	sk = to_ZZ(s);
	Mod_p temp;
	temp = Mod_p(gen.get_val(), mod);
	pk = temp.expo(s);
}

//Set the group to G_q with order o, G_q subset of G_mod_p and generator gen, secret key is s and pk = gen^s
ElGamal::ElGamal(Mod_p gen, ZZ o, ZZ  mod, ZZ s) {

	G = G_q(gen, o, mod);
	sk = s;
	Mod_p temp;
	temp = Mod_p(gen.get_val(), mod);
	pk = temp.expo(s);
}

//Set the group to G_q with order o, G_q subset of G_mod_p and generator gen, secret key is s and public key p
ElGamal::ElGamal(Mod_p gen, long o, long  mod, long s, Mod_p p) {

	G = G_q(gen, o, mod);
	sk = to_ZZ(s);
	pk = p;
}

//Set the group to G_q with order o, G_q subset of G_mod_p and generator gen, secret key is s and public key p
ElGamal::ElGamal(Mod_p gen, long o, ZZ  mod, long s, Mod_p p) {

	G = G_q(gen, o, mod);
	sk = to_ZZ(s);
	pk = p;
}

//Set the group to G_q with order o, G_q subset of G_mod_p and generator gen, secret key is s and public key p
ElGamal::ElGamal(Mod_p gen, long o, ZZ  mod, ZZ s, Mod_p p) {

	G = G_q(gen, o, mod);
	sk = s;
	pk = p;
}

//Set the group to G_q with order o, G_q subset of G_mod_p and generator gen, secret key is s and public key p
ElGamal::ElGamal(Mod_p gen, ZZ o, ZZ  mod, long s, Mod_p p) {

	G = G_q(gen, o, mod);
	sk = to_ZZ(s);
	pk = p;
}

//Set the group to G_q with order o, G_q subset of G_mod_p and generator gen, secret key is s and public key p
ElGamal::ElGamal(Mod_p gen, ZZ o, ZZ  mod, ZZ s, Mod_p p) {

	G = G_q(gen, o, mod);
	sk = s;
	pk = p;
}

//Set the group to G_q with order o, G_q subset of G_mod_p and generator gen, secret key is s and public key p
ElGamal::ElGamal(long o, long  mod, long s, Mod_p p) {

	G = G_q(o, mod);
	sk = to_ZZ(s);
	pk = p;
}

//Set the group to G_q with order o and modular value mod, secret key is s and public key p
ElGamal::ElGamal(long o, ZZ  mod, long s, Mod_p p) {

	G = G_q(o, mod);
	sk = to_ZZ(s);
	pk = p;
}

//Set the group to G_q with order o and modular value mod, secret key is s and public key p
ElGamal::ElGamal(long o, ZZ  mod, ZZ s, Mod_p p) {

	G = G_q(o, mod);
	sk = s;
	pk = p;
}

//Set the group to G_q with order o and modular value mod, secret key is s and public key p
ElGamal::ElGamal(ZZ o, ZZ  mod, long s, Mod_p p) {

	G = G_q(o, mod);
	sk = to_ZZ(s);
	pk = p;
}

//Set the group to G_q with order o and modular value mod, secret key is s and public key p
ElGamal::ElGamal(ZZ o, ZZ  mod, ZZ s, Mod_p p) {

	G = G_q(o, mod);
	sk = to_ZZ(s);
	pk = p;
}

//Set the group to G_q with order o and modular value mod, secret key is s and public key pk = gen^s
ElGamal::ElGamal(long o, long  mod, long s) {

	G = G_q(o, mod);
	sk = to_ZZ(s);
	Mod_p temp;
	temp = Mod_p(G.get_gen().get_val(), mod);
	pk = temp.expo(s);
}

//Set the group to G_q with order o and modular value mod, secret key is s and public key pk = gen^s
ElGamal::ElGamal(long o, ZZ  mod, long s) {

	G = G_q(o, mod);
	sk = to_ZZ(s);
	Mod_p temp;
	temp = Mod_p(G.get_gen().get_val(), mod);
	pk = temp.expo(s);
}

//Set the group to G_q with order o and modular value mod, secret key is s and public key pk = gen^s
ElGamal::ElGamal(long o, ZZ  mod, ZZ s) {

	G = G_q(o, mod);
	sk = s;
	Mod_p temp;
	temp = Mod_p(G.get_gen().get_val(), mod);
	pk = temp.expo(s);
}

//Set the group to G_q with order o and modular value mod, secret key is s and public key pk = gen^s
ElGamal::ElGamal(ZZ o, ZZ  mod, long s) {

	G = G_q(o, mod);
	sk = to_ZZ(s);
	Mod_p temp;
	temp = Mod_p(G.get_gen().get_val(), mod);
	pk = temp.expo(s);
}

//Set the group to G_q with order o and modular value mod, secret key is s and public key pk = gen^s
ElGamal::ElGamal(ZZ o, ZZ  mod, ZZ s) {

	G = G_q(o, mod);
	sk = s;
	Mod_p temp;
	temp = Mod_p(G.get_gen().get_val(), mod);
	pk = temp.expo(s);
}


ElGamal::~ElGamal() {
	// TODO Auto-generated destructor stub
}

//Access to the parameters
G_q ElGamal::get_group()const {
	return G;
}

Mod_p ElGamal::get_pk_1() const {

	return pk_1;
}
Mod_p ElGamal::get_pk() const {

	return pk;
}

ZZ ElGamal::get_sk()const {

	return sk;
}

//functions to change parameters
void ElGamal::set_group(G_q H) {
	this->G = H;
}

void ElGamal::set_sk(long s) {

	sk = to_ZZ(s);
	pk = G.get_gen().expo(s);
}

void ElGamal::set_sk(ZZ s) {

	sk = s;//私钥x
	pk_1 = G.get_gen().expo(s);//生成公钥，y=g^x
}

void ElGamal::set_key(ZZ s, ZZ p) {
	sk = s;//私钥
	pk_1 = Mod_p(p, G.get_mod());//公钥
}
void ElGamal::set_pk(ZZ p) {
	pk = Mod_p(p, G.get_mod());//公钥
}
//生成ElGamal主公钥
void ElGamal::keyGen(string pk_2_str) {
	ZZ pk_2;
	conv(pk_2, pk_2_str.c_str());
	pk = Mod_p(MulMod(pk_1.get_val(), pk_2, G.get_mod()), G.get_mod());
}
//functions to encrypt value/element
Cipher_elg ElGamal::encrypt(Mod_p el) {
	Cipher_elg c;
	Mod_p temp_1, temp_2;
	ZZ ran_1;
	SetSeed(to_ZZ((unsigned int)time(0)));
	ran_1 = RandomBnd(G.get_ord());
	temp_1 = G.get_gen().expo(ran_1);
	temp_2 = pk.expo(ran_1) * el;
	c = Cipher_elg(temp_1, temp_2);
	return c;

}

//Cipher_elg ElGamal::encrypt(ZZ m) {
//	Cipher_elg c;
//	Mod_p temp_1, temp_2;
//	ZZ ran_1;
//	SetSeed(to_ZZ((unsigned int)time(0)));
//	ran_1 = RandomBnd(G.get_ord());
//	cout << ran_1 << endl;
//	temp_1 = G.get_gen().expo(ran_1);
//	temp_2 = pk.expo(ran_1) * Mod_p(m, G.get_mod());
//	c = Cipher_elg(temp_1, temp_2);
//	return c;
//}

Cipher_elg ElGamal::encrypt(long m) {
	Cipher_elg c;
	Mod_p temp_1, temp_2;
	ZZ ran_1;
	SetSeed(to_ZZ((unsigned int)time(0)));
	ran_1 = RandomBnd(G.get_ord());
	temp_1 = G.get_gen().expo(ran_1);
	temp_2 = pk.expo(ran_1) * Mod_p(m, G.get_mod());
	c = Cipher_elg(temp_1, temp_2);
	return c;
}

Cipher_elg ElGamal::encrypt(Mod_p el, long ran_1) {
	Cipher_elg c;
	Mod_p temp_1, temp_2;
	temp_1 = G.get_gen().expo(ran_1);
	temp_2 = pk.expo(ran_1) * el;
	c = Cipher_elg(temp_1, temp_2);
	return c;
}

Cipher_elg ElGamal::encrypt(Mod_p el, ZZ ran_1) {
	Cipher_elg c;
	Mod_p temp_1, temp_2;
	temp_1 = G.get_gen().expo(ran_1);//h^r
	temp_2 = pk.expo(ran_1) * el;//m×y^r
	c = Cipher_elg(temp_1, temp_2);//得到(u,v)密文组，u = h^r，v = m×y^r
	return c;
}
Cipher_elg ElGamal::encrypt(ZZ m, ZZ ran_1) {
	Cipher_elg c;
	Mod_p temp_1, temp_2;
	temp_1 = G.get_gen().expo(ran_1);
	temp_2 = pk.expo(ran_1) * Mod_p(m, G.get_mod());
	c = Cipher_elg(temp_1, temp_2);
	return c;
}
Cipher_elg ElGamal::encrypt(long m, ZZ ran_1) {
	Cipher_elg c;
	Mod_p temp_1, temp_2;
	temp_1 = G.get_gen().expo(ran_1);//h^r
	temp_2 = pk.expo(ran_1) * Mod_p(m, G.get_mod());//m×y^r
	c = Cipher_elg(temp_1, temp_2);
	return c;
}
Cipher_elg ElGamal::encrypt_g(ZZ m, ZZ ran_1) {
	Mod_p temp_1, temp_2, temp_m;
	temp_1 = G.get_gen().expo(ran_1);//h^r
	temp_m = G.get_g().expo(m);//g^m
	temp_2 = pk.expo(ran_1) * temp_m;//g^m×y^r
	return Cipher_elg(temp_1, temp_2);
}
//上链加密
Cipher_elg ElGamal::encrypt(ZZ m_z) {
	Mod_p temp_1, temp_2;
	Mod_p m = Mod_p(m_z, G.get_mod());
	ZZ ran = RandomBnd(G.get_ord());
	temp_1 = G.get_gen().expo(ran);//h^r
	temp_2 = pk_1.expo(ran) * m;//m×y_1^r
	return Cipher_elg(temp_1, temp_2);
}

Cipher_elg ElGamal::encrypt(ZZ m, long ran_1) {
	Cipher_elg c;
	Mod_p temp_1, temp_2;
	temp_1 = G.get_gen().expo(ran_1);
	temp_2 = pk.expo(ran_1) * Mod_p(m, G.get_mod());
	c = Cipher_elg(temp_1, temp_2);
	return c;
}

Cipher_elg ElGamal::encrypt(long m, long ran_1) {
	Cipher_elg c;
	Mod_p temp_1, temp_2;
	temp_1 = G.get_gen().expo(ran_1);
	temp_2 = pk.expo(ran_1) * Mod_p(m, G.get_mod());
	c = Cipher_elg(temp_1, temp_2);
	return c;
}

//下链解密
ZZ ElGamal::decrypt(Cipher_elg c) {
	if (sk == 0)
		cout << "can not decrypt, need secret key" << endl;
	ZZ temp;
	ZZ mod = G.get_mod();
	temp = InvMod(c.get_u(), mod);
	temp = PowerMod(temp, sk, mod);
	temp = MulMod(temp, c.get_v(), mod);
	return temp;
}
//ZZ ElGamal::decrypt(Cipher_elg c, int flag) {
//	if (sk == 0)
//		cout << "can not decrypt, need secret key" << endl;
//	ZZ temp;
//	ZZ mod = G.get_mod();
//	temp = InvMod(c.get_u(), mod);
//	temp = PowerMod(temp, sk, mod);
//	temp = MulMod(temp, c.get_v(), mod);
//	// cout<<temp<<" "<<flush;
//	return temp;
//}
ZZ ElGamal::get_m(ZZ temp) {
	if (temp == 1) {
		return ZZ(0);
	}
	else if (temp == G.get_g().get_val()) {
		return ZZ(1);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 2, G.get_mod())) {
		return ZZ(2);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 3, G.get_mod())) {
		return ZZ(3);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 4, G.get_mod())) {
		return ZZ(4);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 5, G.get_mod())) {
		return ZZ(5);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 6, G.get_mod())) {
		return ZZ(6);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 7, G.get_mod())) {
		return ZZ(7);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 8, G.get_mod())) {
		return ZZ(8);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 9, G.get_mod())) {
		return ZZ(9);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 10, G.get_mod())) {
		return ZZ(10);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 11, G.get_mod())) {
		return ZZ(11);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 12, G.get_mod())) {
		return ZZ(12);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 13, G.get_mod())) {
		return ZZ(13);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 14, G.get_mod())) {
		return ZZ(14);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 15, G.get_mod())) {
		return ZZ(15);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 16, G.get_mod())) {
		return ZZ(16);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 17, G.get_mod())) {
		return ZZ(17);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 18, G.get_mod())) {
		return ZZ(18);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 19, G.get_mod())) {
		return ZZ(19);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 20, G.get_mod())) {
		return ZZ(20);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 21, G.get_mod())) {
		return ZZ(21);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 22, G.get_mod())) {
		return ZZ(22);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 23, G.get_mod())) {
		return ZZ(23);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 24, G.get_mod())) {
		return ZZ(24);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 25, G.get_mod())) {
		return ZZ(25);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 26, G.get_mod())) {
		return ZZ(26);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 27, G.get_mod())) {
		return ZZ(27);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 28, G.get_mod())) {
		return ZZ(28);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 29, G.get_mod())) {
		return ZZ(29);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 30, G.get_mod())) {
		return ZZ(30);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 31, G.get_mod())) {
		return ZZ(31);
	}
	else if (temp == PowerMod(G.get_g().get_val(), 32, G.get_mod())) {
		return ZZ(32);
	}
	else if (temp == PowerMod(G.get_g().get_val(), -1, G.get_mod())) {
		return ZZ(-1);
	}
	else if (temp == PowerMod(G.get_g().get_val(), -2, G.get_mod())) {
		return ZZ(-2);
	}
	return ZZ(0);
}
ZZ ElGamal::decrypt_debug(Cipher_elg c) {
	ZZ temp{ 0 };
	if (DEBUG)
	{
		ZZ mod = G.get_mod();
		temp = InvMod(c.get_u(), mod);
		temp = PowerMod(temp, sk_main_debug, mod);
		temp = MulMod(temp, c.get_v(), mod);
		temp = get_m(temp);
	}
	return temp;
}
//Assigment operator
void ElGamal::operator=(const ElGamal& el) {

	G = el.get_group();
	sk = el.get_sk();
	pk = el.get_pk();
}

