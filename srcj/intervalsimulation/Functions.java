/**
 * #pragma ident "@(#)Functions.java 1.1 (Sun Microsystems, Inc.) 02/12/06"
 */
/**
 *  Copyright © 2002 Sun Microsystems, Inc.
 *  Alexei Agafonov aga@nbsp.nsk.su
 *  All rights reserved.
 */
/**
	Java functions class
*/
import java.lang.*;

public class Functions
{
    /* Fields absent */

    /* Constants */

    private final static double zero	= 0.0;
    private final static double half	= 0.5;
    private final static double one	= 1.0;
    private final static double two	= 2.0;
    private final static double huge	= 1.0e+300;
    private final static double tiny	= 1.0e-300;
    private final static double shuge	= 1.0e+308;
    private final static double stiny	= 1.0e-308;

    private final static double two24	= 16777216.0;
    private final static double twon24	= 5.960464477539062500E-8;
    private final static double two54	= 1.80143985094819840000e+16;	/* 0x43500000, 0x00000000 */
    private final static double twom54	= 5.55111512312578270212e-17;	/* 0x3C900000, 0x00000000 */

    private final static float zerof	= 0.0f;
    private final static float onef	= 1.0f;
    private final static float twof	= 2.0f;

    public static long word(double x) {
	return(Double.doubleToLongBits(x));
    }

    public static int word(float x) {
	return(Float.floatToIntBits(x));
    }

    public static double toword(long l) {
	return(Double.longBitsToDouble(l));
    }

    public static float toword(int i) {
	return(Float.intBitsToFloat(i));
    }

    public static int highword(double x) {
	return((int)(Double.doubleToLongBits(x) >> 32));
    }

    public static int lowword(double x) {
	return((int)Double.doubleToLongBits(x));
    }

    public static long unsignedTolong(int x) {
	if (x < 0)
		return((long)(x & 0x7fffffff) | 0x0000000080000000l);
	return((long)x);
    }

    public static int longToint(long x) {
	if (x < 0l)
		return((int)x | 0x80000000);
	return((int)x);
    }

    public static double tohighword(double x, int y) {
	long l;
	l = Double.doubleToLongBits(x) & 0x00000000ffffffffl;
	l |= unsignedTolong(y) << 32;
	return(Double.longBitsToDouble(l));
    }

    public static double tolowword(double x, int y) {
	long l;
	l = Double.doubleToLongBits(x) & 0xffffffff00000000l;
	l |= unsignedTolong(y);
	return(Double.longBitsToDouble(l));
    }

    public static double fabs(double x) {
	long l;
	l = Double.doubleToLongBits(x) & 0x7fffffffffffffffl;
	return(Double.longBitsToDouble(l));
    }

    public static float fabsf(float x) {
	int i;
	i = Float.floatToIntBits(x) & 0x7fffffff;
	return(Float.intBitsToFloat(i));
    }

    public static boolean signbit(double x) {
	return(Double.doubleToLongBits(x) < 0l);
    }

    public static boolean signbitf(float x) {
	return(Float.floatToIntBits(x) < 0);
    }

    public static double nextafter(double x, double y) {
	double	z;
	long	l;
	if (y > x)
	{
		l = Double.doubleToLongBits(x) & 0x7fffffffffffffffl;
		if (x >= zero)
		{
			z = Double.longBitsToDouble(++l);
			return(z);
		}
		z = -Double.longBitsToDouble(--l);
		return(z);
	}
	if (y < x)
	{
		l = Double.doubleToLongBits(x) & 0x7fffffffffffffffl;
		if (x > zero)
		{
			z = Double.longBitsToDouble(--l);
			return(z);
		}
		z = -Double.longBitsToDouble(++l);
		return(z);
	}
	return(x);
    }

    public static float nextafterf(float x, float y) {
	float	z;
	int	i;
	if (y > x)
	{
		i = Float.floatToIntBits(x) & 0x7fffffff;
		if (x >= zerof)
		{
			z = Float.intBitsToFloat(++i);
			return(z);
		}
		z = -Float.intBitsToFloat(--i);
		return(z);
	}
	if (y < x)
	{
		i = Float.floatToIntBits(x) & 0x7fffffff;
		if (x > zerof)
		{
			z = Float.intBitsToFloat(--i);
			return(z);
		}
		z = -Float.intBitsToFloat(++i);
		return(z);
	}
	return(x);
    }

    public static double next(double x) {
	long	l;
	l = Double.doubleToLongBits(x);
	if (l >= 0l)
	{
		if (l < 0x7ff0000000000000l)
			x = Double.longBitsToDouble(++l);
	}
	else
	{
		l &= 0x7fffffffffffffffl;
		if (l == 0l)
			x = +Double.MIN_VALUE;
		else
			if (l < 0x7ff0000000000001l)
				x = -Double.longBitsToDouble(--l);
	}
	return(x);
    }

    public static double prev(double x) {
	long	l;
	l = Double.doubleToLongBits(x);
	if (l >= 0l)
	{
		if (l == 0l)
			x = -Double.MIN_VALUE;
		else
			if (l < 0x7ff0000000000001l)
				x = Double.longBitsToDouble(--l);
	}
	else
	{
		l &= 0x7fffffffffffffffl;
		if (l < 0x7ff0000000000000l)
			x = -Double.longBitsToDouble(++l);
	}
	return(x);
    }

    public static float nextf(float x) {
	int	i;
	i = Float.floatToIntBits(x);
	if (i >= 0)
	{
		if (i < 0x7f800000)
			x = Float.intBitsToFloat(++i);
	}
	else
	{
		i &=  0x7fffffff;
		if (i == 0)
			x = +Float.MIN_VALUE;
		else
			if (i < 0x7f800001)
				x = -Float.intBitsToFloat(--i);
	}
	return(x);
    }

    public static float prevf(float x) {
	int	i;
	i = Float.floatToIntBits(x);
	if (i >= 0)
	{
		if (i == 0)
			x = -Float.MIN_VALUE;
		else
			if (i < 0x7f800001)
				x = Float.intBitsToFloat(--i);
	}
	else
	{
		i &=  0x7fffffff;
		if (i < 0x7f800000)
			x = -Float.intBitsToFloat(++i);
	}
	return(x);
    }

    public static float doubleTofloatnegative(double x) {
	float z;
	z = (float)x;
	if (z > x)
		z = prevf(z);
	if (Float.isInfinite(z) && z > zerof)
		return(+Float.MAX_VALUE);
	return(z);
    }

    public static float doubleTofloatpositive(double x) {
	float z;
	z = (float)x;
	if (z < x)
		z = nextf(z);
	if (Float.isInfinite(z) && z < zerof)
		return(-Float.MAX_VALUE);
	return(z);
    }

    public static float intTofloatnegative(int x) {
	return(doubleTofloatnegative((double)x));
    }

    public static float intTofloatpositive(int x) {
	return(doubleTofloatpositive((double)x));
    }

    public static double longTodoublenegative(long x) {
	int	i;
	int	firstbitone;
	int	lastbitone;
	int	numberbits;
	boolean	first;
	long	l;
	double	z;
	if (x == 0l)
		return(-zero);
	if (x == Long.MIN_VALUE)
		return((double)x);
	firstbitone	= 0;
	lastbitone	= 0;
	numberbits	= 0;
	first		= false;
	if (x < 0l)
		l = -x;
	else
		l = +x;
	for (i = 0; i < 63; i++)
	{
		if ((l & 0x00000000000000001l) == 0x00000000000000001l)
		{
			if (! first)
			{
				first = true;
				firstbitone = i;
			}
			lastbitone = i;
		}
		l >>= 1;
	}
	numberbits = lastbitone - firstbitone + 1;
	z = (double)x;
	if (numberbits > 53)
		if (x < 0l)
			z = prev(z);
	return(z);
    }

    public static double longTodoublepositive(long x) {
	int	i;
	int	firstbitone;
	int	lastbitone;
	int	numberbits;
	boolean	first;
	long	l;
	double	z;
	if (x == 0l)
		return(+zero);
	if (x == Long.MIN_VALUE)
		return((double)x);
	firstbitone	= 0;
	lastbitone	= 0;
	numberbits	= 0;
	first		= false;
	if (x < 0l)
		l = -x;
	else
		l = +x;
	for (i = 0; i < 63; i++)
	{
		if ((l & 0x00000000000000001l) == 0x00000000000000001l)
		{
			if (! first)
			{
				first = true;
				firstbitone = i;
			}
			lastbitone = i;
		}
		l >>= 1;
	}
	numberbits = lastbitone - firstbitone + 1;
	z = (double)x;
	if (numberbits > 53)
		if (x > 0l)
			z = next(z);
	return(z);
    }

    public static boolean even(double x) {
	return(fmod(x, two) == zero);
    }

    public static boolean evenf(float x) {
	return(fmodf(x, twof) == zerof);
    }

    public static boolean odd(int x) {
	return((x & 0x1) == 1);
    }

    public static double aint(double x) {
	double a;
	a = Math.rint(x);
	if (x >= zero)
	{
		if (a > x)
			a -= one;
		return(a);
	}
	if (a < x)
		a += one;
	return(a);
    }

    public static float aintf(float x) {
	float a;
	a = (float)Math.rint((double)x);
	if (x >= zerof)
	{
		if (a > x)
			a -= onef;
		return(a);
	}
	if (a < x)
		a += onef;
	return(a);
    }

    public static double anint(double x) {
	return(Math.rint(x));
    }

    public static float anintf(float x) {
	return((float)Math.rint((double)x));
    }

    /*
     * Based on the fdlibm ilogb()
     * Returns the binary exponent of non-zero x
     */
    public static int ilogb(double x)
    {
	int	ix;
	long	l;
	l = Double.doubleToLongBits(x) & 0x7fffffffffffffffl;	/* word of x */
	if (l < 0x0010000000000000l)
	{
		if (l == 0l) 
			return 0x80000001;	/* ilogb(0) = 0x80000001 */
		else			/* subnormal x */
			for (ix = -1022, l <<= 11; l > 0; l <<= 1)
				ix -= 1;
		return ix;
	}
	else
		if (l < 0x7ff0000000000000l)
			return((int)(l >> 52) - 1023);
		else
			return 0x7fffffff;
    }

    public static int ilogbf(float x)
    {
	int i, ix;
	i = Float.floatToIntBits(x) & 0x7fffffff;	/* word of x */
	if (i < 0x00800000)
	{
		if (i == 0) 
			return 0x80000001;	/* ilogbf(0) = 0x80000001 */
		else			/* subnormal x */
			for (ix = -127, i <<= 8; i > 0; i <<= 1)
				ix -= 1;
		return ix;
	}
	else
		if (i < 0x7f800000)
			return((i >> 23) - 128);
		else
			return 0x7fffffff;
    }

    /*
     * Based on the fdlibm cbrt()
     * Returns cube root of x
     */
    private final static int B1		= 715094163; /* B1 = (682-0.03306235651)*2**20 */
    private final static int B2		= 696219795; /* B2 = (664-0.03306235651)*2**20 */

    private final static double C	=  5.42857142857142815906e-01; /* 19/35     = 0x3FE15F15, 0xF15F15F1 */
    private final static double D	= -7.05306122448979611050e-01; /* -864/1225 = 0xBFE691DE, 0x2532C834 */
    private final static double E	=  1.41428571428571436819e+00; /* 99/70     = 0x3FF6A0EA, 0x0EA0EA0F */
    private final static double F	=  1.60714285714285720630e+00; /* 45/28     = 0x3FF9B6DB, 0x6DB6DB6E */
    private final static double G	=  3.57142857142857150787e-01; /* 5/14      = 0x3FD6DB6D, 0xB6DB6DB7 */

    public static double cbrt(double x)
    {
	double	r, s, t, w;
	int	n, hx, i;
	long	lx;
	boolean	sign;

	t = zero;
	lx = Double.doubleToLongBits(x);	/* word of x */
	sign = lx < 0l;				/* sign = sign(x) */
	lx &= 0x7fffffffffffffffl;
	if(lx >= 0x7ff0000000000000l)
		return(x + x);			/* cbrt(NaN,INF) is itself */
	if(lx == 0l)
		return(x);			/* cbrt(0) is itself */
	if ((lx & 0x000fffffffffffffl) == 0)
	{
		n = (int)((lx & 0x7ff0000000000000l) >> 32);
		n >>= 20;
		n -= 0x000003ff;
		i = n / 3;
		if ((i * 3) == n)
		{
			n = i + 0x000003ff;
			n <<= 20;
			t = Double.longBitsToDouble((long)n << 32);
			if (sign)
				t = -t;
			return(t);
		}
	}
	x = Double.longBitsToDouble(lx);	/* x <- |x| */
	/* rough cbrt to 5 bits */
	if(lx < 0x0010000000000000l) 		/* subnormal number */
	{
		t = Double.longBitsToDouble(0x4350000000000000l);/* set t= 2**54 */
		t*= x;
		n = highword(t);
		n = n / 3 + B2;
		t = tohighword(t, n);
	}
	else
	{
		hx = (int)(lx >> 32);
		n = hx / 3 + B1;
		t = tohighword(t, n);
	}
	/* new cbrt to 23 bits, may be implemented in single precision */
	r = t * t / x;
	s = C + r * t;
	t *= G + F / (s + E + D / s);	
	/* chopped to 20 bits and make it larger than cbrt(x) */ 
	t = tolowword(t, 0);
	n = highword(t);
	n += 0x00000001;
	t = tohighword(t, n);
	/* one step newton iteration to 53 bits with error less than 0.667 ulps */
	s = t * t;		/* t * t is exact */
	r = x / s;
	w = t + t;
	r = (r - t) / (w + r);	/* r-s is exact */
	t = t + t * r;
	/* restore the sign bit */
	if (sign)
		t = -t;
	return(t);
    }

    public static float cbrtf(float x)
    {
	double	r, s, t, w, o;
	int	n, h, i;
	boolean	sign;

	t = zero;
	h = Float.floatToIntBits(x);	/* word of x */
	sign = h < 0;	 		/* sign = sign(x) */
	h &= 0x7fffffff;
	if(h >= 0x7f800000)
		return(x + x);		/* cbrt(NaN,INF) is itself */
	if(h == 0)
		return(x);		/* cbrt(0) is itself */
	if ((h & 0x007fffff) == 0)
	{
		n = h & 0x7f800000;
		n >>= 23;
		n -= 0x000000ff;
		i = n / 3;
		if ((i * 3) == n)
		{
			n = i + 0x000000ff;
			n <<= 23;
			x = Float.intBitsToFloat(n);
			if (sign)
				x = -x;
			return(x);
		}
	}
	o = (double)Float.intBitsToFloat(h);	/* x <- |x| */
	/* rough cbrt to 5 bits */
	if(h < 0x00800000) 			/* subnormal number */
	{
		t = Double.longBitsToDouble(0x4350000000000000l);/* set t= 2**54 */
		t*= o;
		n = highword(t);
		n = n / 3 + B2;
		t = tohighword(t, n);
	}
	else
	{
		h = highword(o);
		n = h / 3 + B1;
		t = tohighword(t, n);
	}
	/* new cbrt to 23 bits, may be implemented in single precision */
	r = t * t / o;
	s = C + r * t;
	t *= G + F / (s + E + D / s);	
	if (sign)
		t = -t;
	return((float)t);
    }

    /*
     * Based on the fdlibm and interval log10()
     * Returns the base 10 logarithm of x
     */
    /*
     * Method :
     *	Let log10_2hi = leading 40 bits of log10(2) and
     *	    log10_2lo = log10(2) - log10_2hi,
     *	    ivln10    = 1/log(10) rounded.
     *	Then
     *		n = ilogb(x), 
     *		if (n<0)  n = n+1;
     *		x = scalbn(x,-n);
     *		log10(x) := n*log10_2hi + (n*log10_2lo + ivln10*log(x))
     *
     * Note 1:
     *	To guarantee log10(10**n)=n, where 10**n is normal, the rounding 
     *	mode must set to Round-to-Nearest.
     * Note 2:
     *	[1/log(10)] rounded to 53 bits has error  .198   ulps;
     *	log10 is monotonic at all binary break points.
     *
     * Special cases:
     *	log10(x) is NaN with signal if x < 0; 
     *	log10(+INF) is +INF with no signal; log10(0) is -INF with signal;
     *	log10(NaN) is that NaN with no signal;
     *	log10(10**N) = N  for N=0,1,...,22.
     *
     * Constants:
     * The hexadecimal values are the intended ones for the following constants.
     * The decimal values may be used, provided that the compiler will convert
     * from decimal to binary accurately enough to produce the hexadecimal values
     * shown.
     */

    private final static double ivln10		=  4.34294481903251816668e-01; /* 0x3FDBCB7B, 0x1526E50E */
    private final static double log10_2hi	=  3.01029995663611771306e-01; /* 0x3FD34413, 0x509F6000 */
    private final static double log10_2lo	=  3.69423907715893078616e-13; /* 0x3D59FEF3, 0x11F12B36 */

    public static double log10(double x)
    {
	double	y, z;
	int	i, k, hx;
	long	lx;

	lx = Double.doubleToLongBits(x);	/* word of x */
	k = 0;
	if (lx < 0x0010000000000000l)		/* x < 2**-1022  */
	{
		if ((lx & 0x7fffffffffffffffl) == 0l)
			return -two54 / zero;	/* log(+-0)=-inf */
		if (lx < 0l)
			return(+Double.NaN);	/* log(-#) = NaN */
			/** return(x - x) / zero; **/	/* log(-#) = NaN */
		k -= 54; x *= two54;		/* subnormal number, scale up x */
		lx = Double.doubleToLongBits(x);/* word of x */
	}
	if (lx >= 0x7ff0000000000000l)
		return x + x;
	hx = (int)(lx >> 32);
	k += (hx >> 20) - 1023;
	if (k < 0)
		i = 1;
	else
		i = 0;
	hx = (hx & 0x000fffff) | ((0x3ff - i) << 20);
	y = (double)(k + i);
	x = tohighword(x, hx);
	z = y * log10_2lo + ivln10 * Math.log(x);
	return z + y * log10_2hi;
    }

    /*
    * Based on the fdlibm and interval expm1()
    * Returns exp(x)-1
    * K.C. Ng, 1998.5.5
    */
    /*
    * Method
    *   1. Arugment reduction:
    *	Given x, find r and integer k such that
    *
    *               x = k*ln2 + r,  |r| <= 0.5*ln2 ~ 0.34658  
    *
    *      Here a correction term c will be computed to compensate 
    *	the error in r when rounded to a floating-point number.
    *
    *   2. Approximating expm1(r) by a special rational function on
    *	the interval [0,0.34658]:
    *	Since
    *	    r*(exp(r)+1)/(exp(r)-1) = 2+ r^2/6 - r^4/360 + ...
    *	we define R1(r*r) by
    *	    r*(exp(r)+1)/(exp(r)-1) = 2+ r^2/6 * R1(r*r)
    *	That is,
    *	    R1(r**2) = 6/r *((exp(r)+1)/(exp(r)-1) - 2/r)
    *		     = 6/r * ( 1 + 2.0*(1/(exp(r)-1) - 1/r))
    *		     = 1 - r^2/60 + r^4/2520 - r^6/100800 + ...
    *      We use a special Reme algorithm on [0,0.347] to generate 
    * 	a polynomial of degree 5 in r*r to approximate R1. The 
    *	maximum error of this polynomial approximation is bounded 
    *	by 2**-61. In other words,
    *	    R1(z) ~ 1.0 + Q1*z + Q2*z**2 + Q3*z**3 + Q4*z**4 + Q5*z**5
    *	where 	Q1  =  -1.6666666666666567384E-2,
    * 		Q2  =   3.9682539681370365873E-4,
    * 		Q3  =  -9.9206344733435987357E-6,
    * 		Q4  =   2.5051361420808517002E-7,
    * 		Q5  =  -6.2843505682382617102E-9;
    *  	(where z=r*r, and the values of Q1 to Q5 are listed below)
    *	with error bounded by
    *	    |                  5           |     -61
    *	    | 1.0+Q1*z+...+Q5*z   -  R1(z) | <= 2 
    *	    |                              |
    *	
    *	expm1(r) = exp(r)-1 is then computed by the following 
    * 	specific way which minimize the accumulation rounding error: 
    *			       2     3
    *			      r     r    [ 3 - (R1 + R1*r/2)  ]
    *	      expm1(r) = r + --- + --- * [--------------------]
    *		              2     2    [ 6 - r*(3 - R1*r/2) ]
    *	
    *	To compensate the error in the argument reduction, we use
    *		expm1(r+c) = expm1(r) + c + expm1(r)*c 
    *			   ~ expm1(r) + c + r*c 
    *	Thus c+r*c will be added in as the correction terms for
    *	expm1(r+c). Now rearrange the term to avoid optimization 
    * 	screw up:
    *		        (      2                                    2 )
    *		        ({  ( r    [ R1 -  (3 - R1*r/2) ]  )  }    r  )
    *	 expm1(r+c)~r - ({r*(--- * [--------------------]-c)-c} - --- )
    *	                ({  ( 2    [ 6 - r*(3 - R1*r/2) ]  )  }    2  )
    *                   (                                             )
    *    	
    *		   = r - E
    *   3. Scale back to obtain expm1(x):
    *	From step 1, we have
    *	   expm1(x) = either 2^k*[expm1(r)+1] - 1
    *		    = or     2^k*[expm1(r) + (1-2^-k)]
    *   4. Implementation notes:
    *	(A). To save one multiplication, we scale the coefficient Qi
    *	     to Qi*2^i, and replace z by (x^2)/2.
    *	(B). To achieve maximum accuracy, we compute expm1(x) by
    *	  (i)   if x < -56*ln2, return -1.0, (raise inexact if x!=inf)
    *	  (ii)  if k=0, return r-E
    *	  (iii) if k=-1, return 0.5*(r-E)-0.5
    *     (iv)	if k=1 if r < -0.25, return 2*((r+0.5)- E)
    *	       	       else	     return  1.0+2.0*(r-E);
    *	  (v)   if (k<-2||k>56) return 2^k(1-(E-r)) - 1 (or exp(x)-1)
    *	  (vi)  if k <= 20, return 2^k((1-2^-k)-(E-r)), else
    *	  (vii) return 2^k(1-((E+2^-k)-r)) 
    *
    * Special cases:
    *	expm1(INF) is INF, expm1(NaN) is NaN;
    *	expm1(-INF) is -1, and
    *	for finite argument, only expm1(0)=0 is exact.
    *
    * Accuracy:
    *	according to an error analysis, under the rounded-to-nearest mode,
    *	the error is always less than 1 ulp (unit in the last place).
    *
    * Misc. info.
    *	For IEEE double 
    *	    if x >  7.09782712893383973096e+02 then expm1(x) overflow
    *
    * Constants:
    * The hexadecimal values are the intended ones for the following 
    * constants. The decimal values may be used, provided that the 
    * compiler will convert from decimal to binary accurately enough
    * to produce the hexadecimal values shown.
    */

    /* scaled coefficients related to expm1 */
    private final static double Q1		= -3.33333333333331316428e-02;	/* BFA11111 111110F4 */
    private final static double Q2		= 1.58730158725481460165e-03;	/* 3F5A01A0 19FE5585 */
    private final static double Q3		= -7.93650757867487942473e-05;	/* BF14CE19 9EAADBB7 */
    private final static double Q4		= 4.00821782732936239552e-06;	/* 3ED0CFCA 86E65239 */
    private final static double Q5		= -2.01099218183624371326e-07;	/* BE8AFDB7 6E09C32D */

    private final static double othreshold	= 7.09782712893383973096e+02;	/* 40862E42 FEFA39EF */
    private final static double ln2hi		= 6.93147180369123816490e-01;	/* 3FE62E42 FEE00000 */
    private final static double ln2lo		= 1.90821492927058770002e-10;	/* 3DEA39EF 35793C76 */
    private final static double invln2		= 1.44269504088896338700e+00;	/* 3FF71547 652B82FE */

    public static double expm1(double x)
    {
	double	y, hi, lo, c, t, e, hxs, hfx, r1;
	int	hx, k, xsb;
	int	o;

	hx = highword(x);			/* high word of x */
	xsb = hx & 0x80000000;			/* sign bit of x */
	if (xsb == 0)
		y = x;
	else
		y = -x;				/* y = |x| */
	hx &= ~(0x80000000);			/* high word of |x| */
	c = zero;
	/* filter out huge and non-finite arugment */
	if (hx >= 0x4043687a)			/* if |x|>=56*ln2 */
	{
		if (hx >= 0x40862e42)		/* if |x|>=709.78... */
		{
			if (hx >= 0x7ff00000)
			{
				if (((hx & 0xfffff) | lowword(x)) != 0)
					return x + x;	/* NaN */
				else		/* expm1(+-inf)={inf,-1} */
					return xsb == 0 ? x : -one;	
			}
			if (x > othreshold)
				return huge * huge;	/* overflow */
		}
		if (xsb != 0)		/* x < -56*ln2, return -1.0 w/inexact */
		{
			if (x + tiny < zero)		/* raise inexact */
				return tiny - one;	/* return -1 */
		}
	}
	/* argument reduction */
	if (hx > 0x3fd62e42)			/* if  |x| > 0.5 ln2 */
	{
		if (hx < 0x3ff0a2b2)		/* and |x| < 1.5 ln2 */
		{
			if (xsb == 0)
			{
				hi = x - ln2hi;
				lo = ln2lo;
				k = 1;
			}
			else
			{
				hi = x + ln2hi;
				lo = -ln2lo;
				k = -1;
			}
		}
		else
		{
			k = (int)(invln2 * x + (xsb == 0 ? half : -half));
			t = k;
			hi = x - t * ln2hi;	/* t*ln2hi is exact here */
			lo = t * ln2lo;
		}
		x = hi - lo;
		c = (hi - x) - lo;
	}
	else	if (hx < 0x3c900000)	/* when |x|<2**-54, return x */
	{
			t = huge + x;	/* return x w/inexact when x != 0 */
			return x - (t - (huge + x));
	}
	else
		k = 0;
	/* x is now in primary range */
	hfx = half * x;
	hxs = x * hfx;
	r1 = one + hxs * (Q1 + hxs * (Q2 + hxs * (Q3 + hxs * (Q4 + hxs * Q5))));
	t = 3.0 - r1 * hfx;
	e = hxs * ((r1 - t) / (6.0 - x * t));
	if (k == 0)
		return x - (x * e - hxs);	/* c is 0 */
	else
	{
		e = (x * (e - c) - c);
		e -= hxs;
		if (k == -1)
			return half * (x - e) - half;
		if (k == 1)
			if (x < -0.25)
				return -two * (e - (x + half));
			else
				return one + two * (x - e);
		if (k <= -2 || k > 56)		/* suffice to return exp(x)-1 */
		{
			y = one - (e - x);
			o = highword(y);
			o += k << 20;
			y = tohighword(y, o); /*HIGH_WORD(y) += k << 20;*/	/* add k to y's exp't */
			return y - one;
		}
		t = one;
		if (k < 20)
		{
			t = tohighword(t, 0x3ff00000 - (0x200000 >> k));
			/*HIGH_WORD(t) = 0x3ff00000 - (0x200000 >> k);*/
							/* t = 1 - 2^-k */
			y = t - (e - x);
			o = highword(y);
			o += k << 20;
			y = tohighword(y, o); /*HIGH_WORD(y) += k << 20;*/	/* add k to y's exp't */
		}
		else
		{
			t = tohighword(t, (0x3ff - k) << 20);
			/*HIGH_WORD(t) = (0x3ff - k) << 20;*/	/* 2^-k */
			y = x - (e + t);
			y += one;
			o = highword(y);
			o += k << 20;
			y = tohighword(y, o); /*HIGH_WORD(y) += k << 20;*/	/* add k to y's exp't */
		}
	}
	return y;
    }

    /*
    * Based on the fdlibm and interval cosh()
    * K.C. Ng, 5/11/98
    */
    /*
    * cosh(x)
    * Method :
    * mathematically cosh(x) if defined to be (exp(x)+exp(-x))/2
    *	1. Replace x by |x| (cosh(-x) = cosh(x)). 
    *	2.
    *		                                        E*E 
    *	    0        <= x <= ln2/2  :  cosh(x) := 1 + --------, E=expm1(x)
    *			       			       2*(E+1)
    *
    *		                                  exp(x) +  1/exp(x)
    *	    ln2/2    <= x <= 22     :  cosh(x) := -------------------
    *			       			          2
    *	    22       <= x <= lnovft :  cosh(x) := exp(x)/2 
    *	    lnovft   <= x <= ln2ovft:  cosh(x) := exp(x/2)/2 * exp(x/2)
    *	    ln2ovft  <  x	    :  cosh(x) := huge*huge (overflow)
    *
    * Special cases:
    *	cosh(x) is |x| if x is +INF, -INF, or NaN.
    *	only cosh(0)=1 is exact for finite x.
    *
    * coshpositive, coshnegative
    * Method:
    *	According to an error analysis, the error in cosh 
    *   is less than 2 ulp overall and less than 1 ulp for x < 0.5 and x >= 20. 
    *	Also note that for large x, 
    *		cosh(x) ~ 1/2 exp(x) 
    *	and for tiny x,
    *		cosh(x) = 1 + x**2 / 2! + x**4 / 4! + ...
    *
    *       Thus, under rounded-to-nearest mode, assume x >= 0, and let 
    *	y  = cosh(x), we define coshpositive by 
    *		 1+x	... if x=0,inf,or nan
    *		 y+ulp 	... for x<0.5, x>=20, note that inf + ulp = inf
    *		 y+2ulp ... for 0.5<= x < 20
    *	and coshnegative by
    *		 1+x	... if x=0,inf,or nan
    *		 y-ulp	... for x<0.5,x>=20,except when y = 1, return 1.
    *		 y-2ulp ... for 0.5<= x < 20
    *
    */

    public static double cosh(double x)	
    {	
	double	t, w;
	long	ix;

	/* word of |x| */
	ix = Double.doubleToLongBits(x);
	if (ix < 0l)
	{
		x = -x;
		ix &= ~0x8000000000000000l;
	}
	/* x is INF or NaN */
	if (ix >= 0x7ff0000000000000l)
		return x + x;	
	/* |x| in [0,0.5*ln2], return 1+h*E*E/(E+1.0) */
	if (ix < 0x3fd62e4300000000l)
	{
		t = expm1(x);
		w = one + t;
		if (ix < 0x3c80000000000000l)
			return w;  		/* cosh(tiny) = 1 */
		return one + (t * t) / (w + w);
	}
	/* |x| in [0.5*ln2,22], return 0.5*(E+1/E) */
	if (ix < 0x4036000000000000l)		/* |x|<22 */
	{
		t = Math.exp(x);
		return half * (t + one / t);
	}
	/* |x| in [22, log(maxdouble)] return 0.5*exp(|x|) */
	if (ix < 0x40862e42fefa39efl)
		return half * Math.exp(x);
	/* |x| in [log(maxdouble), overflowthresold] */
	if (ix <= 0x408633ce8fb9f87dl) {
		w = Math.exp(half * x);
		t = half * w;
		return t * w;
	}
	/* |x| > overflowthresold, cosh(x) overflow */
	return shuge * x;
    }

    /*
    * Based on the fdlibm and interval sinh()
    * K.C. Ng, 5/1/98
    */
    /*
    * sinh(x)
    * Method :
    * mathematically sinh(x) if defined to be (exp(x)-exp(-x))/2
    *	1. Replace x by |x| (sinh(-x) = -sinh(x)). 
    *	2.  					   2E-E*E/(E+1)
    *	    0        <= x < 1       :  sinh(x) =  --------------, E=expm1(x)
    *						        2
    *		                                    E + E/(E+1)
    *	    1        <= x < 22      :  sinh(x) := --------------
    *			       			        2
    *
    *	    22       <= x <= lnovft :  sinh(x) := exp(x)/2 
    *	    lnovft   <= x <= ln2ovft:  sinh(x) := exp(x/2)/2 * exp(x/2)
    *	    ln2ovft  <  x	    :  sinh(x) := x*shuge (overflow)
    *
    * Special cases:
    *	sinh(x) is |x| if x is +INF, -INF, or NaN.
    *	only sinh(0)=0 is exact for finite x.
    *
    * sinhpositive, sinhnegative
    * Method:
    *	According to an error analysis, the error in sinh 
    *	is less than 2 ulp.
    *
    *	Note that for large x, 
    *		sinh(x) ~ 1/2 exp(x) 
    *	and for tiny x,
    *		sinh(x) = x + x**3 / 3! + x**5 / 5! + ...
    *
    *	For x >= 0, under rounded-to-nearest mode, define static double function 
    *	sinh_u by 
    *		sinhu(x) = x		... if x=0 or x=inf
    *		sinhu(x) = x+ulp	... for x < 2**-26
    *		sinhu(x) = sinh(x)+2ulp	... for x < 20
    *		sinhu(x) = sinh(x)+ulp	... for x < ln2ovft
    *		sinhu(x) = sinh(x) = inf... for x > ln2ovft (overflow if not inf)
    *	and sinh_l by
    *		sinhl(x) = x		... if x < 2**-26 or x = inf
    *		sinhl(x) = sinh(x)-2ulp	... for x < 20
    *		sinhl(x) = sinh(x)-ulp	... for x < ln2ovft
    *		sinhl(x) = max double number	... for inf > x > ln2ovft
    *
    *	Then
    *	(1) if x >= 0,
    *		sinhpositive = sinhu(x)
    *		sinhnegative = sinhl(x);
    *	(2) if x <  0,
    *		sinhpositive = -sinhl(-x)
    *		sinhnegative = -sinhu(-x);
    */

    public static double sinh(double x)	
    {	
	double	t, w, h;
	long	ix, jx;

	/* word of |x| */
	jx = Double.doubleToLongBits(x);
	ix = jx & 0x7fffffffffffffffl;
	/* x is INF or NaN */
	if (ix >= 0x7ff0000000000000l)
		return x + x;	
	h = half;
	if (jx < 0l)
		h = -h;
	/* |x| in [0,22], return sign(x)*0.5*(E+E/(E+1))) */
	if (ix < 0x4036000000000000l)		/* |x|<22 */
	{
		if (ix < 0x3e30000000000000l)	/* |x|<2**-28 */
			if (shuge + x > one)
				return x;	/* sinh(tiny) = tiny with inexact */
		t = expm1(fabs(x));
		if (ix < 0x3ff0000000000000l)
			return h * (two * t - t * t / (t + one));
		return h * (t + t / (t + one));
	}
	/* |x| in [22, log(maxdouble)] return 0.5*exp(|x|) */
	if (ix < 0x40862e42fefa39efl)
		return h * Math.exp(fabs(x));
	/* |x| in [log(maxdouble), overflowthresold] */
	if (ix <= 0x408633ce8fb9f87dl) {
		w = Math.exp(half * fabs(x));
		t = h * w;
		return t * w;
	}
	/* |x| > overflowthresold, sinh(x) overflow */
	w = x * shuge;
	return w * shuge;
    }

    /*
    * Based on the fdlibm and interval tanh()
    * K.C. Ng, 5/1/98
    */
    /*
    * tanh(x)
    * Method :
    *				   x    -x
    *				  e  - e
    *	tanh(x) is defined to be -----------
    *				   x    -x
    *				  e  + e
    *	1. reduce x to non-negative by tanh(-x) = -tanh(x).
    *	2.  0      <= x <= 2**-55 : tanh(x) := x*(one+x)
    *					        -t
    *	    2**-55 <  x <=  1     : tanh(x) := -----; t = expm1(-2x)
    *					       t + 2
    *						     2
    *	    1      <= x <=  22.0  : tanh(x) := 1-  -----; t=expm1(2x)
    *						   t + 2
    *	    22.0   <  x <= INF    : tanh(x) := 1.
    *
    * Special cases:
    *	tanh(NaN) is NaN;
    *	only tanh(0)=0 is exact for finite argument.
    *
    * tanhpositive, tanhnegative
    * Method:
    *	According to an intensive error checking program, the error in tanh 
    *	is less than 3 ulp when x < 1, and less than 1 ulp when x > 1, which can 
    *	be proved. 
    *
    *	Note that for large x, 
    *		tanh(x) ~ 1
    *	and for tiny x,
    *		tanh(x) = x - x**3 / 3 + ...
    *
    *	For x >= 0, under rounded-to-nearest mode, define static double function 
    *	tanh_u by 
    *		tanhu(x) = x		... if x < 2**-55
    *		tanhu(x) = tanh(x)+3ulp	... for x <= 1
    *		tanhu(x) = tanh(x)+ulp	... for x > 1, unless 1
    *	and tanh_l by
    *		tanhl(x) = x - ulp	... if x < 2**-55, unless x = 0
    *		tanhl(x) = tanh(x)-3ulp	... for x <= 1
    *		tanhl(x) = tanh(x)-ulp	... for x > 1
    *
    *	Then
    *	(1) if x >= 0,
    *		tanhpositive = tanhu(x)
    *		tanhnegative = tanhl(x);
    *	(2) if x <  0,
    *		tanhpositive = -tanhl(-x)
    *		tanhnegative = -tanhu(-x);
    */

    public static double tanh(double x)	
    {	
	double	t, z;
	long	jx, ix;

	/* word of |x| */
	jx = Double.doubleToLongBits(x);
	ix = jx & 0x7fffffffffffffffl;
	/* x is INF or NaN */
	if (ix >= 0x7ff0000000000000l)
	{ 
		if (ix == 0x7ff0000000000000l)
			return one / x + one;	/* tanh(+-inf)=+-1 */
		else
			return one / x - one;	/* tanh(NaN) = NaN */
	}
	/* |x| < 22 */
	if (ix < 0x4036000000000000l)		/* |x|<22 */
	{
		if (ix < 0x3c80000000000000l)	/* |x|<2**-55 */
			return x * (one + x);	/* tanh(small) = small */
		if (ix >= 0x3ff0000000000000l)	/* |x|>=1  */
		{
			t = expm1(two * fabs(x));
			z = one - two / (t + two);
		}
		else
		{
			t = expm1(-two * fabs(x));
			z = -t / (t + two);
		}
	}
	else
		/* |x| > 22, return +-1 */
		z = one - stiny;		/* raised inexact flag */
	return(jx >= 0l) ? z : -z;
    }

    /*
     * Based on the fdlibm and interval fmod()
     */
    public static double fmod(double x, double y)
    {
	int	n,hx,hy,hz,ix,iy,sx,i;
	int	lx,ly,lz;
	int	o;
	double	Zero[];

	hx = highword(x);		/* high word of x */
	lx = lowword(x);		/* low  word of x */
	hy = highword(y);		/* high word of y */
	ly = lowword(y);		/* low  word of y */
	sx = hx&0x80000000;		/* sign of x */
	hx ^=sx;			/* |x| */
	hy &= 0x7fffffff;		/* |y| */

	Zero = new double[2];
	Zero[0] = zero;
	Zero[1] = -zero;
    /* purge off exception values */
	if ((hy|ly)==0||(hx>=0x7ff00000)||	/* y=0,or x not finite */
	  ((hy|(((ly|-ly)>>31))&0x1)>0x7ff00000))	/* or y is NaN */
	    return(x*y)/(x*y);
	if (hx<=hy) {
	    if ((hx<hy)||(lx<ly)) return x;	/* |x|<|y| return x */
	    if (lx==ly) 
		return Zero[(sx>>31)&0x1];	/* |x|=|y| return x*0*/
	}

    /* determine ix = ilogb(x) */
	if (hx<0x00100000) {	/* subnormal x */
	    if (hx==0) {
		for (ix = -1043, i=lx; i>0; i<<=1) ix -=1;
	    } else {
		for (ix = -1022,i=(hx<<11); i>0; i<<=1) ix -=1;
	    }
	} else ix = (hx>>20)-1023;

    /* determine iy = ilogb(y) */
	if (hy<0x00100000) {	/* subnormal y */
	    if (hy==0) {
		for (iy = -1043, i=ly; i>0; i<<=1) iy -=1;
	    } else {
		for (iy = -1022,i=(hy<<11); i>0; i<<=1) iy -=1;
	    }
	} else iy = (hy>>20)-1023;

    /* set up {hx,lx}, {hy,ly} and align y to x */
	if (ix >= -1022) 
	    hx = 0x00100000|(0x000fffff&hx);
	else {		/* subnormal x, shift x to normal */
	    n = -1022-ix;
	    if (n<=31) {
		o = lx >>> 32-n;
		hx = (hx<<n)|o;
		lx <<= n;
	    } else {
		hx = lx<<(n-32);
		lx = 0;
	    }
	}
	if (iy >= -1022) 
	    hy = 0x00100000|(0x000fffff&hy);
	else {		/* subnormal y, shift y to normal */
	    n = -1022-iy;
	    if (n<=31) {
		o = ly >>> 32-n;
		hy = (hy<<n)|o;
		ly <<= n;
	    } else {
		hy = ly<<(n-32);
		ly = 0;
	    }
	}

    /* fix point fmod */
	n = ix - iy;
	while (n-- > 0) {
	    hz=hx-hy;lz=lx-ly; if (lx<ly) hz -= 1;
	    if (hz<0) {hx = hx+hx+((lx>>31)&0x1); lx = lx+lx;}
	    else {
	    	if ((hz|lz)==0) 		/* return sign(x)*0 */
		    return Zero[(sx>>31)&0x1];
	    	hx = hz+hz+((lz>>31)&0x1); lx = lz+lz;
	    }
	}
	hz=hx-hy;lz=lx-ly; if (lx<ly) hz -= 1;
	if (hz>=0) {hx=hz;lx=lz;}

    /* convert back to floating value and restore the sign */
	if ((hx|lx)==0) 			/* return sign(x)*0 */
	    return Zero[(sx>>31)&0x1];	
	while (hx<0x00100000) {		/* normalize x */
	    hx = hx+hx+((lx>>31)&0x1); lx = lx+lx;
	    iy -= 1;
	}
	if (iy>= -1022) {	/* normalize output */
	    hx = ((hx-0x00100000)|((iy+1023)<<20));
	    x = tohighword(x, hx|sx);
	    x= tolowword(x, lx);
	} else {		/* subnormal output */
	    n = -1022 - iy;
	    if (n<=20) {
		o = lx >>> n;
		lx = o|(hx<<(32-n));
		hx >>= n;
	    } else if (n<=31) {
		o = lx >>> n;
		lx = (hx<<(32-n))|o; hx = sx;
	    } else {
		lx = hx>>(n-32); hx = sx;
	    }
	    x = tohighword(x, hx|sx);
	    x = tolowword(x, lx);
	    x *= one;		/* create necessary signal */
	}
	return x;		/* exact output */
    }

    /*
     * Based on the R fmodf()
     */
    private final static int is		= (int)0x80000000;
    private final static int im		= 0x007fffff;
    private final static int ii		= 0x7f800000;
    private final static int iu		= 0x00800000;

    public static float fmodf(float x, float y)
    {
	float	w;
	int	ix,iy,iz,n,k,ia,ib;
	ix = word(x)&0x7fffffff;
	iy = word(y)&0x7fffffff;

    /* purge off exception values */
	if (ix>=ii||iy>ii||iy==0) {
	    w = x*y; w = w/w; 
	} else if (ix<=iy) {
	    if (ix<iy) 	w = x;		/* return x if |x|<|y| */
	    else 	w = zerof*x;	/* return sign(x)*0.0  */
	} else if (iy<0x0c800000) 
	    w = (float) fmod((double)x,(double)y);
	else
    {

    /* a,b is in reasonable range. Now prepare for fix point arithmetic */
	ia = ix&ii; ib = iy&ii;
	n  = ((ia-ib)>>23);
	ix = iu|(ix&im);
	iy = iu|(iy&im);

    /* fix point fmod */
	/*
	while (n-- > 0) {
	    iz=ix-iy;
	    if (iz<0) ix = ix+ix;
	    else if (iz==0) {w = toword(is&word(x)); return(w)}
	    else ix = iz+iz;
	}
	*/
    /* unroll the above loop 4 times to gain performance */
	k = n&3; n >>= 2;
	while (n-- > 0) {
	    iz=ix-iy;
	    if (iz<0) ix = ix+ix;
	    else if (iz==0) {w = toword(is&word(x)); return(w);}
	    else ix = iz+iz;
	    iz=ix-iy;
	    if (iz<0) ix = ix+ix;
	    else if (iz==0) {w = toword(is&word(x)); return(w);}
	    else ix = iz+iz;
	    iz=ix-iy;
	    if (iz<0) ix = ix+ix;
	    else if (iz==0) {w = toword(is&word(x)); return(w);}
	    else ix = iz+iz;
	    iz=ix-iy;
	    if (iz<0) ix = ix+ix;
	    else if (iz==0) {w = toword(is&word(x)); return(w);}
	    else ix = iz+iz;
	}
	switch(k) {
	case 3:
	    iz=ix-iy;
	    if (iz<0) ix = ix+ix;
	    else if (iz==0) {w = toword(is&word(x)); return(w);}
	    else ix = iz+iz;
	    /* FALLTHRU */
	case 2:
	    iz=ix-iy;
	    if (iz<0) ix = ix+ix;
	    else if (iz==0) {w = toword(is&word(x)); return(w);}
	    else ix = iz+iz;
	    /* FALLTHRU */
	case 1: 
	    iz=ix-iy;
	    if (iz<0) ix = ix+ix;
	    else if (iz==0) {w = toword(is&word(x)); return(w);}
	    else ix = iz+iz;
	}
	iz=ix-iy;
	if (iz>=0) ix = iz;

    /* convert back to floating value and restore the sign */
	if (ix==0) {w = toword(is&word(x)); return(w);}
	while ((ix&iu)==0) {
	    ix += ix;
	    ib -= iu;
	}
	ib |= word(x)&is;
	w = toword(ib|(ix&im));
    }
    return(w);
    }

    /*
     * Based on the fdlibm and interval scalbn()
     */
    public static double scalbn(double x, int n)
    {
	double	z;
	int	k,hx,lx;
	hx = highword(x);
	lx = lowword(x);
	k = (hx&0x7ff00000)>>20;		/* extract exponent */
	if (k==0) {				/* 0 or subnormal x */
	    if ((lx|(hx&0x7fffffff))==0) return x; /* +-0 */
	    x *= two54; 
	    hx = highword(x);
	    k = ((hx&0x7ff00000)>>20) - 54; 
	    if (n < -50000) return tiny*x; 	/*underflow*/
	}
	if (k==0x7ff) return x+x;		/* NaN or Inf */
	k = k+n; 
	if (k > 0x7fe) {
		z = huge;
		if (hx<0) z = -z;
		/* return huge*copysign(huge,x); (overflow)  */
		return huge*z;
	}
	if (k > 0) 				/* normal result */
	    {x = tohighword(x, (hx&0x800fffff)|(k<<20)); return x;}
	if (k <= -54)
	    if (n > 50000) { 	/* in case integer overflow in n+k */
		z =  huge;
		if (hx<0) z = -z;
		/* return huge*copysign(huge,x);	(overflow) */
		return huge*z;
	    } else {
		z =  tiny;
		if (hx<0) z = -z;
		/* return tiny*copysign(tiny,x); 	(underflow) */
		return tiny*z;
	    }
	k += 54;				/* subnormal result */
	x = tohighword(x, (hx&0x800fffff)|(k<<20));
	return x*twom54;
    }

    /*
     * Based on the fdlibm and interval *rem_pio2*()
     */
    private final static int init_jk[] = {2,3,4,6}; /* initial value for jk */

    /*
     * 396 Hex digits (476 decimal) of 2/pi 
     */
    private final static int ipio2[] = {
	0xA2F983, 0x6E4E44, 0x1529FC, 0x2757D1, 0xF534DD, 0xC0DB62, 
	0x95993C, 0x439041, 0xFE5163, 0xABDEBB, 0xC561B7, 0x246E3A, 
	0x424DD2, 0xE00649, 0x2EEA09, 0xD1921C, 0xFE1DEB, 0x1CB129, 
	0xA73EE8, 0x8235F5, 0x2EBB44, 0x84E99C, 0x7026B4, 0x5F7E41, 
	0x3991D6, 0x398353, 0x39F49C, 0x845F8B, 0xBDF928, 0x3B1FF8, 
	0x97FFDE, 0x05980F, 0xEF2F11, 0x8B5A0A, 0x6D1F6D, 0x367ECF, 
	0x27CB09, 0xB74F46, 0x3F669E, 0x5FEA2D, 0x7527BA, 0xC7EBE5, 
	0xF17B3D, 0x0739F7, 0x8A5292, 0xEA6BFB, 0x5FB11F, 0x8D5D08, 
	0x560330, 0x46FC7B, 0x6BABF0, 0xCFBC20, 0x9AF436, 0x1DA9E3, 
	0x91615E, 0xE61B08, 0x659985, 0x5F14A0, 0x68408D, 0xFFD880, 
	0x4D7327, 0x310606, 0x1556CA, 0x73A8C9, 0x60E27B, 0xC08C6B, 
    };

    /*
     * Constants:
     * The hexadecimal values are the intended ones for the following constants.
     * The decimal values may be used, provided that the compiler will convert
     * from decimal to binary accurately enough to produce the hexadecimal values
     * shown.
     */

    private final static double pio2_inf[] = {
	1.57079625129699707031e+00,
	7.54978941586159635335e-08,
	5.39030252995776476554e-15,
	3.28200341580791294123e-22,
	1.27065575308067607349e-29,
	1.22933308981111328932e-36,
	2.73370053816464559624e-44,
	2.16741683877804819444e-51,
    };

    /*
     * int rem_pio2m(x,y,e0,nx,prec,ipio2)
     * double x[],y[]; int e0,nx,prec; const int ipio2[];
     * 
     * rem_pio2m return the last three digits of N with 
     *		y = x - N*pi/2
     * so that |y| < pi/2.
     *
     * The method is to compute the integer (mod 8) and fraction parts of 
     * (2/pi)*x without doing the full multiplication. In general we
     * skip the part of the product that are known to be a huge integer (
     * more accurately, = 0 mod 8 ). Thus the number of operations are
     * independent of the exponent of the input.
     *
     * (2/PI) is represented by an array of 24-bit integers in ipio2[].
     * Here PI could as well be a machine value pi.
     *
     * Input parameters:
     * 	x[]	The input value (must be positive) is broken into nx 
     *		pieces of 24-bit integers in double precision format.
     *		x[i] will be the i-th 24 bit of x. The scaled exponent 
     *		of x[0] is given in input parameter e0 (i.e., x[0]*2^e0 
     *		match x's up to 24 bits.
     *
     *		Example of breaking a double z into x[0]+x[1]+x[2]:
     *			e0 = ilogb(z)-23
     *			z  = scalbn(z,-e0)
     *		for i = 0,1,2
     *			x[i] =  floor(z)
     *			z    = (z-x[i])*2**24
     *
     *
     *	y[]	ouput result in an array of double precision numbers.
     *		The dimension of y[] is:
     *			24-bit  precision	1
     *			53-bit  precision	2
     *			64-bit  precision	2
     *			113-bit precision	3
     *		The actual value is the sum of them. Thus for 113-bit
     *		precsion, one may have to do something like:
     *
     *		long double t,w,r_head, r_tail;
     *		t = (long double)y[2] + (long double)y[1];
     *		w = (long double)y[0];
     *		r_head = t+w;
     *		r_tail = w - (r_head - t);
     *
     *	e0	The exponent of x[0]
     *
     *	nx	dimension of x[]
     *
     *  prec	an interger indicating the precision:
     *			0	24  bits (single)
     *			1	53  bits (double)
     *			2	64  bits (extended)
     *			3	113 bits (quad)
     *
     *	ipio2[]
     *		integer array, contains the (24*i)-th to (24*i+23)-th 
     *		bit of 2/pi or 2/PI after binary point. The corresponding 
     *		floating value is
     *
     *			ipio2[i] * 2^(-24(i+1)).
     *
     * External or static function:
     *	double scalbn( ), floor( );
     *
     *
     * Here is the description of some local variables:
     *
     * 	jk	jk+1 is the initial number of terms of ipio2[] needed
     *		in the computation. The recommended value is 2,3,4,
     *		6 for single, double, extended,and quad.
     *
     * 	jz	local integer variable indicating the number of 
     *		terms of ipio2[] used. 
     *
     *	jx	nx - 1
     *
     *	jv	index for pointing to the suitable ipio2[] for the
     *		computation. In general, we want
     *			( 2^e0*x[0] * ipio2[jv-1]*2^(-24jv) )/8
     *		is an integer. Thus
     *			e0-3-24*jv >= 0 or (e0-3)/24 >= jv
     *		Hence jv = max(0,(e0-3)/24).
     *
     *	jp	jp+1 is the number of terms in pio2[] needed, jp = jk.
     *
     * 	q[]	double array with integral value, representing the
     *		24-bits chunk of the product of x and 2/pi.
     *
     *	q0	the corresponding exponent of q[0]. Note that the
     *		exponent for q[i] would be q0-24*i.
     *
     *	pio2[]	double precision array, obtained by cutting pi/2 or PI/2
     *		into 24 bits chunks. 
     *
     *	f[]	ipio2[] in floating point 
     *
     *	iq[]	integer array by breaking up q[] in 24-bits chunk.
     *
     *	fq[]	final product of x*(2/pi) in fq[0],..,fq[jk]
     *
     *	ih	integer. If >0 it indicats q[] is >= 0.5, hence
     *		it also indicates the *sign* of the result.
     *
     */

    public static int rem_pio2m(double[] x, double[] y, int e0, int nx, int prec)
    {
	int		jz,jx,jv,jp,jk,carry,n,i,j,k,m,q0,ih;
	double		z,fw;
	int[]		iq;
	double[]	f, fq, q;

	iq	= new int[20];
	f	= new double[20];
	fq	= new double[20];
	q	= new double[20];

    /* initialize jk,pio2 */
	jk = init_jk[prec];

	jp = jk;

    /* determine jx,jv,q0, note that 3>q0 */
	jx =  nx-1;
	jv = (e0-3)/24; if (jv<0) jv=0;
	q0 =  e0-24*(jv+1);

    /* set up f[0] to f[jx+jk] where f[jx+jk] = ipio2[jv+jk] */
	j = jv-jx; m = jx+jk;
	for (i=0;i<=m;i++,j++) f[i] = (j<0)? zero : (double)ipio2[j];

    /* compute q[0],q[1],...q[jk] */
	for (i=0;i<=jk;i++) {
	    for (j=0,fw=zero;j<=jx;j++) fw += x[j]*f[jx+i-j]; q[i] = fw;
	}

	jz = jk;
    /* recompute: */
	for (;;)
	{
    /* distill q[] into iq[] reversingly */
	for (i=0,j=jz,z=q[jz];j>0;i++,j--) {
	    fw    =  (double)((int)(twon24* z));
	    iq[i] =  (int)(z-two24*fw);
	    z     =  q[j-1]+fw;
	}

    /* compute n */
	z  = scalbn(z,q0);		/* actual value of z */
	z -= 8.0*Math.floor(z*0.125);	/* trim off integer >= 8 */
	n  = (int)z;
	z -= (double)n;
	ih = 0;
	if (q0>0) {	/* need iq[jz-1] to determine n */
	    i = (iq[jz-1]>>(24-q0)); n += i;
	    iq[jz-1] -= i<<(24-q0);
	    ih = iq[jz-1]>>(23-q0);
	} 
	else if (q0==0) ih = iq[jz-1]>>23;
	else if (z>=half) ih=2;

	if (ih>0) {	/* q > 0.5 */
	    n += 1; carry = 0;
	    for (i=0;i<jz;i++) {	/* compute 1-q */
		j = iq[i];
		if (carry==0) {
		    if (j!=0) {
			carry = 1; iq[i] = 0x1000000- j;
		    }
		} else  iq[i] = 0xffffff - j;
	    }
	    if (q0>0) {		/* rare case: chance is 1 in 12 */
		switch(q0) {
		case 1:
	    	   iq[jz-1] &= 0x7fffff; break;
	    	case 2:
	    	   iq[jz-1] &= 0x3fffff; break;
		}
	    }
	    if (ih==2) {
		z = one - z;
		if (carry!=0) z -= scalbn(one,q0);
	    }
	}

    /* check if recomputation is needed */
	if (z==zero) {
	    j = 0;
	    for (i=jz-1;i>=jk;i--) j |= iq[i];
	    if (j==0) { /* need recomputation */
		for (k=1;iq[jk-k]==0;k++);   /* k = no. of terms needed */

		for (i=jz+1;i<=jz+k;i++) {   /* add q[jz+1] to q[jz+k] */
		    f[jx+i] = (double)ipio2[jv+i];
		    for (j=0,fw=zero;j<=jx;j++) fw += x[j]*f[jx+i-j];
		    q[i] = fw;
		}
		jz += k;
		/* goto recompute; */
		continue;
	    }
	}
	break;
	}
    /* cut out zero terms */
	if (z==zero) {
	    jz -= 1; q0 -= 24;
	    while (iq[jz]==0) { jz--; q0-=24;}
	} else { /* break z into 24-bit if neccessary */
	    z = scalbn(z,-q0);
	    if (z>=two24) { 
		fw = (double)((int)(twon24*z));
		iq[jz] = (int)(z-two24*fw);
		jz += 1; q0 += 24;
		iq[jz] = (int)fw;
	    } else iq[jz] = (int)z;
	}

    /* convert integer "bit" chunk to floating-point value */
	fw = scalbn(one,q0);
	for (i=jz;i>=0;i--) {
	    q[i] = fw*(double)iq[i]; fw*=twon24;
	}

    /* compute pio2[0,...,jp]*q[jz,...,0] */
	for (i=jz;i>=0;i--) {
	    for (fw=zero,k=0;k<=jp&&k<=jz-i;k++) fw += pio2_inf[k]*q[i+k];
	    fq[jz-i] = fw;
	}

    /* compress fq[] into y[] */
	switch(prec) {
	    case 0:
		fw = zero;
		for (i=jz;i>=0;i--) fw += fq[i];
		y[0] = (ih==0)? fw: -fw; 
		break;
	    case 1:
	    case 2:
		fw = zero;
		for (i=jz;i>=0;i--) fw += fq[i]; 
		y[0] = (ih==0)? fw: -fw; 
		fw = fq[0]-fw;
		for (i=1;i<=jz;i++) fw += fq[i];
		y[1] = (ih==0)? fw: -fw; 
		break;
	    case 3:	/* painful */
		for (i=jz;i>0;i--) {
		    fw      = fq[i-1]+fq[i]; 
		    fq[i]  += fq[i-1]-fw;
		    fq[i-1] = fw;
		}
		for (i=jz;i>1;i--) {
		    fw      = fq[i-1]+fq[i]; 
		    fq[i]  += fq[i-1]-fw;
		    fq[i-1] = fw;
		}
		for (fw=zero,i=jz;i>=2;i--) fw += fq[i]; 
		if (ih==0) {
		    y[0] =  fq[0]; y[1] =  fq[1]; y[2] =  fw;
		} else {
		    y[0] = -fq[0]; y[1] = -fq[1]; y[2] = -fw;
		}
	}
	return n&7;
    }

    /*
     * Based on the fdlibm and interval *rem_pio2*(), *reduce*()
     */
    /*
    *  Generic large argument reduction of the form x = y + n pi/2
    *
    *  On entry, y[0] is the argument to be reduced
    *
    *  On exit, y[0] and y[1] are the high and low parts of the reduced
    *  argument, and n mod 8 is returned
    */

    private final static int npio2hw[] = {
	0x3FF921FB, 0x400921FB, 0x4012D97C, 0x401921FB, 0x401F6A7A, 0x4022D97C,
	0x4025FDBB, 0x402921FB, 0x402C463A, 0x402F6A7A, 0x4031475C, 0x4032D97C,
	0x40346B9C, 0x4035FDBB, 0x40378FDB, 0x403921FB, 0x403AB41B, 0x403C463A,
	0x403DD85A, 0x403F6A7A, 0x40407E4C, 0x4041475C, 0x4042106C, 0x4042D97C,
	0x4043A28C, 0x40446B9C, 0x404534AC, 0x4045FDBB, 0x4046C6CB, 0x40478FDB,
	0x404858EB, 0x404921FB,
    };

    private final static double invpio2	=  6.36619772367581382433e-01; /* 0x3FE45F30, 0x6DC9C883 */
    private final static double pio2_1	=  1.57079632673412561417e+00; /* 0x3FF921FB, 0x54400000 */
    private final static double pio2_1t	=  6.07710050650619224932e-11; /* 0x3DD0B461, 0x1A626331 */
    private final static double pio2_2	=  6.07710050630396597660e-11; /* 0x3DD0B461, 0x1A600000 */
    private final static double pio2_2t	=  2.02226624879595063154e-21; /* 0x3BA3198A, 0x2E037073 */
    private final static double pio2_3	=  2.02226624871116645580e-21; /* 0x3BA3198A, 0x2E000000 */
    private final static double pio2_3t	=  8.47842766036889956997e-32; /* 0x397B839A, 0x252049C1 */

    public static int sunmath_reduce(double[] y)
    {
	double		tx;
	double[]	tt;
	int		hx;
	int			e0, nx;
	int		j;
	double		x, z, w, t, r, fn;
	int		i, n, ix;
	boolean		sign;
	long		o;
	x = y[0];
	hx = highword(x);
	ix = hx&0x7fffffff;
	if (ix<=0x3fe921fb)   /* |x| ~<= pi/4 , no need for reduction */
	    {y[0] = x; y[1] = 0; return 0;}
	if (ix<0x4002d97c) {  /* |x| < 3pi/4, special case with n=+-1 */
	    if (hx>0) {
		z = x - pio2_1;
		if (ix!=0x3ff921fb) {    /* 33+53 bit pi is good enough */
		    y[0] = z - pio2_1t;
		    y[1] = (z-y[0])-pio2_1t;
		} else {		/* near pi/2, use 33+33+53 bit pi */
		    z -= pio2_2;
		    y[0] = z - pio2_2t;
		    y[1] = (z-y[0])-pio2_2t;
		}
		return 1;
	    } else {    /* negative x */
		z = x + pio2_1;
		if (ix!=0x3ff921fb) {    /* 33+53 bit pi is good enough */
		    y[0] = z + pio2_1t;
		    y[1] = (z-y[0])+pio2_1t;
		} else {		/* near pi/2, use 33+33+53 bit pi */
		    z += pio2_2;
		    y[0] = z + pio2_2t;
		    y[1] = (z-y[0])+pio2_2t;
		}
		return -1 & 7;
	    }    
	}
	if (ix<=0x413921fb) { /* |x| ~<= 2^19*(pi/2), medium size */
	    t  = fabs(x);
	    n  = (int)(t*invpio2+half);
	    fn = (double)n;
	    r  = t-fn*pio2_1;
	    w  = fn*pio2_1t;	/* 1st round good to 85 bit */
	    if (n<32&&ix!=npio2hw[n-1]) {	
		y[0] = r-w;	/* quick check no cancellation */
	    } else {
		j = ix>>20;
		y[0] = r-w; 
		i = j-((highword(y[0])>>20)&0x7ff);
		if (i>16) {  /* 2nd iteration needed, good to 118 */
		    t  = r;
		    w  = fn*pio2_2;	
		    r  = t-w;
		    w  = fn*pio2_2t-((t-r)-w);	
		    y[0] = r-w;
		    i = j-((highword(y[0])>>20)&0x7ff);
		    if (i>49)  {	/* 3rd iteration need, 151 bits acc */
		    	t  = r;	/* will cover all possible cases */
		    	w  = fn*pio2_3;	
		    	r  = t-w;
		    	w  = fn*pio2_3t-((t-r)-w);	
		    	y[0] = r-w;
		    }
		}
	    }
	    y[1] = (r-y[0])-w;
	    if (hx<0) 	{y[0] = -y[0]; y[1] = -y[1]; return -n & 7;}
	    else	 return n & 7;
	}
	/* 
	* all other (large) arguments
	*/
	if (ix>=0x7ff00000) {		/* x is inf or NaN */
	    y[0]=y[1]=x-x; return 0;
	}
	sign = hx < 0;
	e0 = (ix >> 20) - 1046;
	tx = zero;
	/**/
	o = (long)((ix & 0xfffff) | 0x41600000) << 32;
	o |= unsignedTolong(lowword(y[0]));
	tx = Double.longBitsToDouble(o);
	/**/
	/**
	tx = tohighword(tx, 0x41600000 | (ix & 0xfffff));
	tx = tolowword(tx, lowword(y[0]));
	**/
	tt = new double[3];
	tt[0] = (double)((int)tx);
	tx = (tx - tt[0]) * two24;
	if (tx != zero)
	{
		nx = 2;
		tt[1] = (double)((int)tx);
		tt[2] = (tx - tt[1]) * two24;
		if (tt[2] != zero)
			nx = 3;
	}
	else
	{
		nx = 1;
		tt[1] = tt[2] = zero;
	}
	nx = rem_pio2m(tt, y, e0, nx, 2);
	if (sign)
	{
		nx = -nx;
		y[0] = -y[0];
		y[1] = -y[1];
	}
	return nx & 7;
    }

    /*
     * Based on the fdlibm __kernel_cos()
     */
     /*
      * __kernel_cos( x,  y )
      * kernel cos function on [-pi/4, pi/4], pi/4 ~ 0.785398164
      * Input x is assumed to be bounded by ~pi/4 in magnitude.
      * Input y is the tail of x. 
      *
      * Algorithm
      *	1. Since cos(-x) = cos(x), we need only to consider positive x.
      *	2. if x < 2^-27 (hx<0x3e400000 0), return 1 with inexact if x!=0.
      *	3. cos(x) is approximated by a polynomail of degree 14 on
      *	   [0,pi/4]
      *		  	                 4            14
      *	   	cos(x) ~ 1 - x*x/2 + C1*x + ... + C6*x
      *	   where the remez error is
      *	
      * 	|              2     4     6     8     10    12     14 |     -58
      * 	|cos(x)-(1-.5*x +C1*x +C2*x +C3*x +C4*x +C5*x  +C6*x  )| <= 2
      * 	|    					               | 
      * 
      * 	               4     6     8     10    12     14 
      *	4. let r = C1*x +C2*x +C3*x +C4*x +C5*x  +C6*x  , then
      *	       cos(x) = 1 - x*x/2 + r
      *	   since cos(x+y) ~ cos(x) - sin(x)*y 
      *			  ~ cos(x) - x*y,
      *	   a correction term is necessary in cos(x) and hence
      *		cos(x+y) = 1 - (x*x/2 - (r - x*y))
      *	   For better accuracy when x > 0.3, let qx = |x|/4 with
      *	   the last 32 bits mask off. Then
      *		cos(x+y) = (1-qx) - ((x*x/2-qx) - (r-x*y)).
      *	   Note that 1-qx and (x*x/2-qx) is EXACT here, and the
      *	   magnitude of the latter is at least a quater of x*x/2,
      *	   thus, reducing the rounding error in the subtraction.
      */

    private final static double C1	=  4.16666666666666019037e-02; /* 0x3FA55555, 0x5555554C */
    private final static double C2	= -1.38888888888741095749e-03; /* 0xBF56C16C, 0x16C15177 */
    private final static double C3	=  2.48015872894767294178e-05; /* 0x3EFA01A0, 0x19CB1590 */
    private final static double C4	= -2.75573143513906633035e-07; /* 0xBE927E4F, 0x809C52AD */
    private final static double C5	=  2.08757232129817482790e-09; /* 0x3E21EE9E, 0xBDB4B1C4 */
    private final static double C6	= -1.13596475577881948265e-11; /* 0xBDA8FAE9, 0xBE8838D4 */

    public static double kernel_cos(double x, double y)
    {
	double	a,hz,z,r,qx;
	long	ix;
	ix = Double.doubleToLongBits(x) & 0x7fffffffffffffffl;	/* ix = |x|'s word*/
	if(ix < 0x3e40000000000000l) {				/* if x < 2**-27 */
		if(((int)x) == 0)
			return one;				/* generate inexact */
	}
	z  = x * x;
	r  = z * (C1 + z * (C2 + z * (C3 + z * (C4 + z * (C5 + z * C6)))));
	if(ix < 0x3fd3333333333333l) 				/* if |x| < 0.3 */
		return one - (half * z - (z * r - x * y));
	else {
		if(ix > 0x3fe9000000000000l) {			/* x > 0.78125 */
			qx = 0.28125;
		} else {
			qx = Double.longBitsToDouble(ix-0x0020000000000000l);	/* x/4 */
		}
		hz	= half * z - qx;
		a	= one - qx;
		return a - (hz - (z * r - x * y));
	}
    }

    /*
     * Based on the fdlibm __kernel_sin()
     */
     /* __kernel_sin( x, y, iy)
      * kernel sin function on [-pi/4, pi/4], pi/4 ~ 0.7854
      * Input x is assumed to be bounded by ~pi/4 in magnitude.
      * Input y is the tail of x.
      * Input iy indicates whether y is 0. (if iy=0, y assume to be 0). 
      *
      * Algorithm
      *	1. Since sin(-x) = -sin(x), we need only to consider positive x. 
      *	2. if x < 2^-27 (hx<0x3e400000 0), return x with inexact if x!=0.
      *	3. sin(x) is approximated by a polynomail of degree 13 on
      *	   [0,pi/4]
      *		  	         3            13
      *	   	sin(x) ~ x + S1*x + ... + S6*x
      *	   where
      *	
      * 	|sin(x)         2     4     6     8     10     12  |     -58
      * 	|----- - (1+S1*x +S2*x +S3*x +S4*x +S5*x  +S6*x   )| <= 2
      * 	|  x 					           | 
      * 
      *	4. sin(x+y) = sin(x) + sin'(x')*y
      *		    ~ sin(x) + (1-x*x/2)*y
      *	   For better accuracy, let 
      *		     3      2      2      2      2
      *		r = x *(S2+x *(S3+x *(S4+x *(S5+x *S6))))
      *	   then                   3    2
      *		sin(x) = x + (S1*x + (x *(r-y/2)+y))
      */

    private final static double S1	= -1.66666666666666324348e-01; /* 0xBFC55555, 0x55555549 */
    private final static double S2	=  8.33333333332248946124e-03; /* 0x3F811111, 0x1110F8A6 */
    private final static double S3	= -1.98412698298579493134e-04; /* 0xBF2A01A0, 0x19C161D5 */
    private final static double S4	=  2.75573137070700676789e-06; /* 0x3EC71DE3, 0x57B1FE7D */
    private final static double S5	= -2.50507602534068634195e-08; /* 0xBE5AE5E6, 0x8A2B9CEB */
    private final static double S6	=  1.58969099521155010221e-10; /* 0x3DE5D93A, 0x5ACFD57C */

    public static double kernel_sin(double x, double y, boolean o)
    {
						/* o is true if y is zero */
	double	z,r,v;
	long	ix;
	ix = Double.doubleToLongBits(x) & 0x7fffffffffffffffl;	/* word of x */
	if(ix < 0x3e40000000000000l)				/* |x| < 2**-27 */
		if((int)x == 0)
			return x;				/* generate inexact */
	z	=  x * x;
	v	=  z * x;
	r	=  S2 + z * (S3 + z * (S4 + z * (S5 + z * S6)));
	if(o)
		return x + v * (S1 + z * r);
	else
		return x - ((z * (half * y - v * r) - y) - v * S1);
    }

    /*
     * Based on the fdlibm cos()
     */
     /* cos(x)
      * Return cosine function of x.
      *
      * kernel function:
      *	__kernel_sin		... sine function on [-pi/4,pi/4]
      *	__kernel_cos		... cosine function on [-pi/4,pi/4]
      *	__ieee754_rem_pio2	... argument reduction routine
      *
      * Method.
      *      Let S,C and T denote the sin, cos and tan respectively on 
      *	[-PI/4, +PI/4]. Reduce the argument x to y1+y2 = x-k*pi/2 
      *	in [-pi/4 , +pi/4], and let n = k mod 4.
      *	We have
      *
      *          n        sin(x)      cos(x)        tan(x)
      *     ----------------------------------------------------------
      *	    0	       S	   C		 T
      *	    1	       C	  -S		-1/T
      *	    2	      -S	  -C		 T
      *	    3	      -C	   S		-1/T
      *     ----------------------------------------------------------
      *
      * Special cases:
      *      Let trig be any of sin, cos, or tan.
      *      trig(+-INF)  is NaN, with signals;
      *      trig(NaN)    is that NaN;
      *
      * Accuracy:
      *	TRIG(x) returns trig(x) nearly rounded 
      */

    public static double k_cos(double x, double y, int n) {
	switch(n & 3) {
		case 0 :	return	+kernel_cos(x, y);
		case 1 :	return	-kernel_sin(x, y, false);
		case 2 :	return	-kernel_cos(x, y);
		default :	return	+kernel_sin(x, y, false);
	}
    }

    /*
     * Based on the fdlibm sin()
     */
     /* sin(x)
      * Return sine function of x.
      *
      * kernel function:
      *	__kernel_sin		... sine function on [-pi/4,pi/4]
      *	__kernel_cos		... cose function on [-pi/4,pi/4]
      *	__ieee754_rem_pio2	... argument reduction routine
      *
      * Method.
      *      Let S,C and T denote the sin, cos and tan respectively on 
      *	[-PI/4, +PI/4]. Reduce the argument x to y1+y2 = x-k*pi/2 
      *	in [-pi/4 , +pi/4], and let n = k mod 4.
      *	We have
      *
      *          n        sin(x)      cos(x)        tan(x)
      *     ----------------------------------------------------------
      *	    0	       S	   C		 T
      *	    1	       C	  -S		-1/T
      *	    2	      -S	  -C		 T
      *	    3	      -C	   S		-1/T
      *     ----------------------------------------------------------
      *
      * Special cases:
      *      Let trig be any of sin, cos, or tan.
      *      trig(+-INF)  is NaN, with signals;
      *      trig(NaN)    is that NaN;
      *
      * Accuracy:
      *	TRIG(x) returns trig(x) nearly rounded 
      */

    public static double k_sin(double x, double y, int n) {
	switch(n & 3 ) {
		case 0 :	return	+kernel_sin(x, y, false);
		case 1 :	return	+kernel_cos(x, y);
		case 2 :	return	-kernel_sin(x, y, false);
		default :	return	-kernel_cos(x, y);
	}
    }
}
