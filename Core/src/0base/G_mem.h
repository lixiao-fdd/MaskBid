#ifndef G_MEM_H_
#define G_MEM_H_

#include "base.h"
NTL_CLIENT

class G_mem {
private:
	ZZ val; //Value of the group member
public:
	//Constructors & destructor
	G_mem();
	G_mem(ZZ x);
	G_mem(long x);
	virtual ~G_mem();

	//Access to the variable val
	ZZ value() const;
	//Return the inverse of an element
	G_mem inv();
	//Operators
	G_mem op(const G_mem& x, const G_mem& y);
	bool operator==(const G_mem& y) const;
};



#endif /* G_MEM_H_ */
