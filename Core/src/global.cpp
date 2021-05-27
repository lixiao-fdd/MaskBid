#include "global.h"

G_q G = G_q();                    // group used for the Pedersen commitment
G_q H = G_q();                    // group used for the the encryption
ElGamal El = ElGamal();         // The class for encryption and decryption
Pedersen Ped = Pedersen();        // Object which calculates the commitments
Network net = Network();
bool vMode = false;
bool debug = false;
string filesPath;