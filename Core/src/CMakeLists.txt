ADD_EXECUTABLE(sBid 
 "main.cpp"
 "global.cpp"
 "0base/base.h"
 "0base/Cipher_elg.cpp"
 "0base/Cyclic_group.cpp"
 "0base/G_mem.cpp"
 "0base/G_q.cpp"
 "0base/Mod_p.cpp"
 "0base/sha256.cpp"
 "0base/network.cpp" 
 "1ElGamal/ElGamal.cpp"
 "1Pedersen/Pedersen.cpp"
 "1Pedersen/multi_expo.cpp"
 "1commitment/commitment.cpp"
 "2paraGen/paraGen.cpp"
 "2cipherGen/cipherGen.cpp" 
 "2compare/compare.cpp"
 "2shuffle/shuffle.cpp"
 "2shuffle/func_pro.cpp"
 "2shuffle/func_ver.cpp"
 "2shuffle/Functions.cpp"
 "2shuffle/Prover_toom.cpp"
 "2shuffle/Verifier_toom.cpp" 
 "2decrypt/decrypt.cpp"
 "3sBid/sBid.cpp"   
 "3server/server.cpp")

SET(EXECUTABLE_OUTPUT_PATH ${sBid_SOURCE_DIR}/bin)

# GMP
find_path(GMP_INCLUDE_DIR NAMES gmp.h)
find_path(GMPXX_INCLUDE_DIR NAMES gmpxx.h)
find_library(GMP_LIBRARIES NAMES gmp libgmp)
find_library(GMPXX_LIBRARIES NAMES gmpxx libgmpxx)

# NTL
find_path(NTL_INCLUDE_DIR NAMES NTL)
find_library(NTL_LIBRARIES NAMES ntl libntl)

target_link_libraries(sBid ntl gmp gmpxx)