/**
 * #pragma ident "@(#)Arith.java 1.1 (Sun Microsystems, Inc.) 02/12/06"
 */
/**
 *  Copyright © 2002 Sun Microsystems, Inc.
 *  Alexei Agafonov aga@nbsp.nsk.su
 *  All rights reserved.
 */
/**
	Java arithmetic class
*/
import java.lang.*;

public class Arith extends Functions
{
    /* Fields absent */

    /* Constants */

    private final static double zero		= 0.0;
    private final static double half		= 0.5;
    private final static double one		= 1.0;
    private final static double two		= 2.0;
    private final static double three		= 3.0;
    private final static double ten		= 10.0;
    private final static double twentytwo	= 22.0;
    private final static double onetwentytwo	= 1.0e22;
    private final static double splitter	= 134217728.0; /* 2^27 */
    private final static double splittero	= one / splitter; /* 2^-27 */

    private final static float zerof		= 0.0f;
    private final static float halff		= 0.5f;
    private final static float onef		= 1.0f;
    private final static float twof		= 2.0f;
    private final static float tenf		= 10.0f;
    private final static float twentytwof	= 22.0f;
    private final static float onetwentytwof	= 1.0e22f;

    private static void printh(double a) {
	long l;
	l = Double.doubleToLongBits(a);
	System.out.println(Long.toHexString(l));
    }

    private static void printhf(float a) {
	int i;
	i = Float.floatToIntBits(a);
	System.out.println(Integer.toHexString(i));
    }

    private static double roundnegative(double x, int n) {
	while (n-- > 0)
		x = prev(x);
	return(x);
    }

    private static double roundpositive(double x, int n) {
	while (n-- > 0)
		x = next(x);
	return(x);
    }

    public static double addnegative(double x, double y) {
	double z;
	z = x + y;
	if (z - y > x || z - x > y)
		z = prev(z);
	return(z);
    }

    public static double addpositive(double x, double y) {
	double z;
	z = x + y;
	if (z - y < x || z - x < y)
		z = next(z);
	return(z);
    }

    public static double subnegative(double x, double y) {
	double z;
	z = x - y;
	if (z + y > x || x - z < y)
		z = prev(z);
	return(z);
    }

    public static double subpositive(double x, double y) {
	double z;
	z = x - y;
	if (z + y < x || x - z > y)
		z = next(z);
	return(z);
    }

    /**
    Note :
	The algorithms for division, multiplication,
		square, square root and cube root borrowed
		from "Quad-double and Double-double Precision Software",
			see link :
			http://www.nersc.gov/~dhbailey/mpdist/mpdist.html
    **/

    public static double divnegative(double x, double y) {
	double	z, zu, zl, yu, yl, t;
	long	l;
	int	h, e;
	z = x / y;
	if (Double.isInfinite(z))
	{
		if (z > zero)
			return(+Double.MAX_VALUE);
		return(z);
	}
	if (z == zero)
	{
		if (x == zero)
			return(-zero);
		if ((x > zero && y > zero) || (x < zero && y < zero))
			return(-zero);
		return(-Double.MIN_VALUE);
	}
	l = Double.doubleToLongBits(z);
	h = (int)(l >> 32);
	h &= 0x7fffffff;
	e = (h >> 20) - 1023;
	if (e < 1024 - 27)
	{
		t = splitter * z;
		zu = t - (t - z);
	}
	else
	{
		t = splittero * z;
		zu = z - (z - t);
	}
	zl = z - zu;

	l = Double.doubleToLongBits(y);
	h = (int)(l >> 32);
	h &= 0x7fffffff;
	e = (h >> 20) - 1023;
	if (e < 1024 - 27)
	{
		t = splitter * y;
		yu = t - (t - y);
	}
	else
	{
		t = splittero * y;
		yu = y - (y - t);
	}
	yl = y - yu;

	/* Compute  z * y - x */
	t = ((zu * yu - x) + zu * yl + zl * yu) + zl * yl;

	if (y > zero)
	{
		if (t > zero)
			return(prev(z));
	}
	else
	{
		if (t < zero)
			return(prev(z));
	}
	return(z);
    }

    public static double divpositive(double x, double y) {
	double	z, zu, zl, yu, yl, t;
	long	l;
	int	h, e;
	z = x / y;
	if (Double.isInfinite(z))
	{
		if (z < zero)
			return(-Double.MAX_VALUE);
		return(z);
	}
	if (z == zero)
	{
		if (x == zero)
			return(+zero);
		if ((x > zero && y < zero) || (x < zero && y > zero))
			return(+zero);
		return(+Double.MIN_VALUE);
	}
	l = Double.doubleToLongBits(z);
	h = (int)(l >> 32);
	h &= 0x7fffffff;
	e = (h >> 20) - 1023;
	if (e < 1024 - 27)
	{
		t = splitter * z;
		zu = t - (t - z);
	}
	else
	{
		t = splittero * z;
		zu = z - (z - t);
	}
	zl = z - zu;

	l = Double.doubleToLongBits(y);
	h = (int)(l >> 32);
	h &= 0x7fffffff;
	e = (h >> 20) - 1023;
	if (e < 1024 - 27)
	{
		t = splitter * y;
		yu = t - (t - y);
	}
	else
	{
		t = splittero * y;
		yu = y - (y - t);
	}
	yl = y - yu;

	/* Compute  z * y - x */
	t = ((zu * yu - x) + zu * yl + zl * yu) + zl * yl;

	if (y > zero)
	{
		if (t < zero)
			return(next(z));
	}
	else
	{
		if (t > zero)
			return(next(z));
	}
	return(z);
    }

    public static double mulnegative(double x, double y) {
	double	z, xu, xl, yu, yl, t;
	long	l;
	int	h, e;
	z = x * y;
	if (Double.isInfinite(z))
	{
		if (z > zero)
			return(+Double.MAX_VALUE);
		return(z);
	}
	if (z == zero)
	{
		if (x == zero || y == zero)
			return(-zero);
		if ((x > zero && y > zero) || (x < zero && y < zero))
			return(-zero);
		return(-Double.MIN_VALUE);
	}
	l = Double.doubleToLongBits(x);
	h = (int)(l >> 32);
	h &= 0x7fffffff;
	e = (h >> 20) - 1023;
	if (e < 1024 - 27)
	{
		t = splitter * x;
		xu = t - (t - x);
	}
	else
	{
		t = splittero * x;
		xu = x - (x - t);
	}
	xl = x - xu;

	l = Double.doubleToLongBits(y);
	h = (int)(l >> 32);
	h &= 0x7fffffff;
	e = (h >> 20) - 1023;
	if (e < 1024 - 27)
	{
		t = splitter * y;
		yu = t - (t - y);
	}
	else
	{
		t = splittero * y;
		yu = y - (y - t);
	}
	yl = y - yu;

	/* Compute x * y - z */
	t = ((xu * yu - z) + xu * yl + xl * yu) + xl * yl;

	if (t < zero)
		return(prev(z));
	return(z);
    }

    public static double mulpositive(double x, double y) {
	double	z, xu, xl, yu, yl, t;
	long	l;
	int	h, e;
	z = x * y;
	if (Double.isInfinite(z))
	{
		if (z < zero)
			return(-Double.MAX_VALUE);
		return(z);
	}
	if (z == zero)
	{
		if (x == zero || y == zero)
			return(+zero);
		if ((x > zero && y < zero) || (x < zero && y > zero))
			return(+zero);
		return(+Double.MIN_VALUE);
	}
	l = Double.doubleToLongBits(x);
	h = (int)(l >> 32);
	h &= 0x7fffffff;
	e = (h >> 20) - 1023;
	if (e < 1024 - 27)
	{
		t = splitter * x;
		xu = t - (t - x);
	}
	else
	{
		t = splittero * x;
		xu = x - (x - t);
	}
	xl = x - xu;

	l = Double.doubleToLongBits(y);
	h = (int)(l >> 32);
	h &= 0x7fffffff;
	e = (h >> 20) - 1023;
	if (e < 1024 - 27)
	{
		t = splitter * y;
		yu = t - (t - y);
	}
	else
	{
		t = splittero * y;
		yu = y - (y - t);
	}
	yl = y - yu;

	/* Compute x * y - z */
	t = ((xu * yu - z) + xu * yl + xl * yu) + xl * yl;

	if (t > zero)
		return(next(z));
	return(z);
    }

    public static double sqrtnegative(double x) {
	double	z, zu, zl, t;
	long	l;
	int	h, e;
	z = Math.sqrt(x);
	if (Double.isInfinite(z))
		return(+Double.MAX_VALUE);
	if (z == zero)
		return(-zero);
	l = Double.doubleToLongBits(z);
	h = (int)(l >> 32);
	h &= 0x7fffffff;
	e = (h >> 20) - 1023;
	if (e < 1024 - 27)
	{
		t = splitter * z;
		zu = t - (t - z);
	}
	else
	{
		t = splittero * z;
		zu = z - (z - t);
	}
	zl = z - zu;

	/* Compute z * z - x */
	t = ((zu * zu - x) + two * zu * zl) + zl * zl;

	if (t > zero)
		return(prev(z));
	return(z);
    }

    public static double sqrtpositive(double x) {
	double	z, zu, zl, t;
	long	l;
	int	h, e;
	z = Math.sqrt(x);
	if (Double.isInfinite(z))
		return(+Double.MAX_VALUE);
	if (z == zero)
		if (x == zero)
			return(+zero);
		else
			return(+Double.MIN_VALUE);
	l = Double.doubleToLongBits(z);
	h = (int)(l >> 32);
	h &= 0x7fffffff;
	e = (h >> 20) - 1023;
	if (e < 1024 - 27)
	{
		t = splitter * z;
		zu = t - (t - z);
	}
	else
	{
		t = splittero * z;
		zu = z - (z - t);
	}
	zl = z - zu;

	/* Compute z * z - x */
	t = ((zu * zu - x) + two * zu * zl) + zl * zl;

	if (t < zero)
		return(next(z));
	return(z);
    }

    public static double sqrnegative(double x) {
	double	z, xu, xl, t;
	long	l;
	int	h, e;
	z = x * x;
	if (Double.isInfinite(z))
		return(+Double.MAX_VALUE);
	if (z == zero)
		return(-zero);
	l = Double.doubleToLongBits(x);
	h = (int)(l >> 32);
	h &= 0x7fffffff;
	e = (h >> 20) - 1023;
	if (e < 1024 - 27)
	{
		t = splitter * x;
		xu = t - (t - x);
	}
	else
	{
		t = splittero * x;
		xu = x - (x - t);
	}
	xl = x - xu;

	/* Compute x * x - z */
	t = ((xu * xu - z) + two * xu * xl) + xl * xl;

	if (t < zero)
		return(prev(z));
	return(z);
    }

    public static double sqrpositive(double x) {
	double	z, xu, xl, t;
	long	l;
	int	h, e;
	z = x * x;
	if (Double.isInfinite(z))
		return(z);
	if (z == zero)
		if (x == zero)
			return(+zero);
		else
			return(+Double.MIN_VALUE);
	l = Double.doubleToLongBits(x);
	h = (int)(l >> 32);
	h &= 0x7fffffff;
	e = (h >> 20) - 1023;
	if (e < 1024 - 27)
	{
		t = splitter * x;
		xu = t - (t - x);
	}
	else
	{
		t = splittero * x;
		xu = x - (x - t);
	}
	xl = x - xu;

	/* Compute x * x - z */
	t = ((xu * xu - z) + two * xu * xl) + xl * xl;

	if (t > zero)
		return(next(z));
	return(z);
    }

    public static double cbrtnegative(double x) {
	double	z, zu, zl, m, mu, ml, n, nu, nl, t, o;
	long	l;
	int	h, e;
	z = cbrt(x);
	if (Double.isInfinite(z))
	{
		if (z > zero)
			return(+Double.MAX_VALUE);
		return(z);
	}
	if (z == zero)
	{
		if (x == zero)
			return(-zero);
		if (x > zero)
			return(-zero);
		return(-Double.MIN_VALUE);
	}
	l = Double.doubleToLongBits(z);
	h = (int)(l >> 32);
	h &= 0x7fffffff;
	e = (h >> 20) - 1023;
	if (e < 1024 - 27)
	{
		t = splitter * z;
		zu = t - (t - z);
	}
	else
	{
		t = splittero * z;
		zu = z - (z - t);
	}
	zl = z - zu;

	/* Compute z * z * z - x */
	m = zu * zu;
	l = Double.doubleToLongBits(m);
	h = (int)(l >> 32);
	h &= 0x7fffffff;
	e = (h >> 20) - 1023;
	if (e < 1024 - 27)
	{
		t = splitter * m;
		mu = t - (t - m);
	}
	else
	{
		t = splittero * m;
		mu = m - (m - t);
	}
	ml = m - mu;

	n = zl * zl;
	l = Double.doubleToLongBits(m);
	h = (int)(l >> 32);
	h &= 0x7fffffff;
	e = (h >> 20) - 1023;
	if (e < 1024 - 27)
	{
		t = splitter * n;
		nu = t - (t - n);
	}
	else
	{
		t = splittero * n;
		nu = n - (n - t);
	}
	nl = n - nu;

	t = (mu * zu - x) + ml * zu + three * (mu * zl + ml * zl);
	o = ((zu + zl) + zu + zu) * nu + ((zu + zl) + zu + zu) * nl;
	t += o;

	if (t > zero)
		return(prev(z));
	return(z);
    }

    public static double cbrtpositive(double x) {
	double	z, zu, zl, m, mu, ml, n, nu, nl, t, o;
	long	l;
	int	h, e;
	z = cbrt(x);
	if (Double.isInfinite(z))
	{
		if (z < zero)
			return(-Double.MAX_VALUE);
		return(z);
	}
	if (z == zero)
	{
		if (x == zero)
			return(+zero);
		if (x < zero)
			return(+zero);
		return(+Double.MIN_VALUE);
	}
	l = Double.doubleToLongBits(z);
	h = (int)(l >> 32);
	h &= 0x7fffffff;
	e = (h >> 20) - 1023;
	if (e < 1024 - 27)
	{
		t = splitter * z;
		zu = t - (t - z);
	}
	else
	{
		t = splittero * z;
		zu = z - (z - t);
	}
	zl = z - zu;

	/* Compute z * z * z - x */
	m = zu * zu;
	l = Double.doubleToLongBits(m);
	h = (int)(l >> 32);
	h &= 0x7fffffff;
	e = (h >> 20) - 1023;
	if (e < 1024 - 27)
	{
		t = splitter * m;
		mu = t - (t - m);
	}
	else
	{
		t = splittero * m;
		mu = m - (m - t);
	}
	ml = m - mu;

	n = zl * zl;
	l = Double.doubleToLongBits(m);
	h = (int)(l >> 32);
	h &= 0x7fffffff;
	e = (h >> 20) - 1023;
	if (e < 1024 - 27)
	{
		t = splitter * n;
		nu = t - (t - n);
	}
	else
	{
		t = splittero * n;
		nu = n - (n - t);
	}
	nl = n - nu;

	t = (mu * zu - x) + ml * zu + three * (mu * zl + ml * zl);
	o = ((zu + zl) + zu + zu) * nu + ((zu + zl) + zu + zu) * nl;
	t += o;

	if (t < zero)
		return(next(z));
	return(z);
    }

    public static double powernegative(double a, double b) {
	double c;
	if (a == zero && b > zero)
		return(-zero);
	if (b == one)
		return(a);
	if (a == one)
		return(a);
	if (b == zero)
		return(one);
	if (b == half)
		return(sqrtnegative(a));
	if (b == -one)
		return(divnegative(one, a));
	if (b == two)
		return(sqrnegative(a));
	if (a == -one)
		if (even(b))
			return(one);
		else
			return(-one);
	c = Math.pow(a, b);
	if (Double.isInfinite(c))
	{
		if (c > zero)
			return(Double.MAX_VALUE);
		return(c);
	}
	if (c == zero)
	{
		if (a > zero || even(b))
			return(-zero);
		return(-Double.MIN_VALUE);
	}
	if (a > zero && a < one && b < zero && b > -one)
		if (c <= one)
			return(one);
	if (a > one && b > zero && b < one)
		if (c <= one)
			return(one);
	return(roundnegative(c, 1));
    }

    public static double powerpositive(double a, double b) {
	double c;
	if (a == zero && b > zero)
		return(+zero);
	if (b == one)
		return(a);
	if (a == one)
		return(a);
	if (b == zero)
		return(one);
	if (b == half)
		return(sqrtpositive(a));
	if (b == -one)
		return(divpositive(one, a));
	if (b == two)
		return(sqrpositive(a));
	if (a == -one)
		if (even(b))
			return(one);
		else
			return(-one);
	c = Math.pow(a, b);
	if (Double.isInfinite(c))
	{
		if (c < zero)
			return(-Double.MAX_VALUE);
		return(c);
	}
	if (c == zero)
	{
		if (a > zero || even(b))
			return(Double.MIN_VALUE);
		return(+zero);
	}
	if (a > zero && a < one && b > zero && b < one)
		if (c >= one)
			return(one);
	if (a > one && b < zero && b > -one)
		if (c >= one)
			return(one);
	return(roundpositive(c, 1));
    }

    public static double expnegative(double x) {
	double z;
	if (x == zero)
		return(one);
	z = Math.exp(x);
	z = roundnegative(z, 1);
	if (z <= zero)
		return(-zero);
	if (x > zero && z <= one)
		return(one);
	return(z);
    }

    public static double exppositive(double x) {
	double z;
	if (x == zero)
		return(one);
	z = Math.exp(x);
	z = roundpositive(z, 1);
	if (x < zero && z >= one)
		return(one);
	return(z);
    }

    public static double lognegative(double x) {
	double z;
	if (x == one)
		return(-zero);
	z = Math.log(x);
	return(roundnegative(z, 1));
    }

    public static double logpositive(double x) {
	double z;
	if (x == one)
		return(+zero);
	z = Math.log(x);
	return(roundpositive(z, 1));
    }

    public static double log10negative(double x) {
	double z;
	if (x == one)
		return(-zero);
	z = log10(x);
	if (x >= ten && x <= onetwentytwo && aint(x) == x &&
		z >= one && z <= twentytwo && aint(z) == z)
			return(z);
	return(roundnegative(z, 2));
    }

    public static double log10positive(double x) {
	double z;
	if (x == one)
		return(+zero);
	z = log10(x);
	if (x >= ten && x <= onetwentytwo && aint(x) == x &&
		z >= one && z <= twentytwo && aint(z) == z)
			return(z);
	return(roundpositive(z, 2));
    }

    public static double cosnegative(double x) {
	double z;
	if (x == zero)
		return(one);
	z = kernel_cos(x, zero);
	z = roundnegative(z, 1);
	if (z <= -one)
		return(-one);
	return(z);
    }

    public static double cospositive(double x) {
	double z;
	if (x == zero)
		return(one);
	z = kernel_cos(x, zero);
	z = roundpositive(z, 1);
	if (z >= +one)
		return(+one);
	return(z);
    }

    public static double cosnegative(double x, double y, int n) {
	double z;
	if (x == zero)
		return(one);
	z = k_cos(x, y, n);
	z = roundnegative(z, 1);
	if (z <= -one)
		return(-one);
	return(z);
    }

    public static double cospositive(double x, double y, int n) {
	double z;
	if (x == zero)
		return(one);
	z = k_cos(x, y, n);
	z = roundpositive(z, 1);
	if (z >= +one)
		return(+one);
	return(z);
    }

    public static double sinnegative(double x) {
	double z;
	if (x == zero)
		return(-zero);
	z = kernel_sin(x, zero, true);
	z = roundnegative(z, 1);
	if (z <= -one)
		return(-one);
	return(z);
    }

    public static double sinpositive(double x) {
	double z;
	if (x == zero)
		return(+zero);
	z = kernel_sin(x, zero, true);
	z = roundpositive(z, 1);
	if (z >= +one)
		return(+one);
	return(z);
    }

    public static double sinnegative(double x, double y, int n) {
	double z;
	if (x == zero)
		return(-zero);
	z = k_sin(x, y, n);
	z = roundnegative(z, 1);
	if (z <= -one)
		return(-one);
	return(z);
    }

    public static double sinpositive(double x, double y, int n) {
	double z;
	if (x == zero)
		return(+zero);
	z = k_sin(x, y, n);
	z = roundpositive(z, 1);
	if (z >= +one)
		return(+one);
	return(z);
    }

    public static double tannegative(double x) {
	double z;
	if (x == zero)
		return(-zero);
	z = Math.tan(x);
	return(roundnegative(z, 1));
    }

    public static double tanpositive(double x) {
	double z;
	if (x == zero)
		return(+zero);
	z = Math.tan(x);
	return(roundpositive(z, 1));
    }

    public static double acosnegative(double x) {
	double z;
	if (x == one)
		return(-zero);
	z = Math.acos(x);
	return(roundnegative(z, 1));
    }

    public static double acospositive(double x) {
	double z;
	if (x == one)
		return(+zero);
	z = Math.acos(x);
	return(roundpositive(z, 1));
    }

    public static double asinnegative(double x) {
	double z;
	if (x == zero)
		return(-zero);
	z = Math.asin(x);
	return(roundnegative(z, 1));
    }

    public static double asinpositive(double x) {
	double z;
	if (x == zero)
		return(+zero);
	z = Math.asin(x);
	return(roundpositive(z, 1));
    }

    public static double atannegative(double x) {
	double z;
	if (x == zero)
		return(-zero);
	z = Math.atan(x);
	return(roundnegative(z, 2));
    }

    public static double atanpositive(double x) {
	double z;
	if (x == zero)
		return(+zero);
	z = Math.atan(x);
	return(roundpositive(z, 1));
    }

    public static double atan2negative(double y, double x) {
	double z;
	if (y == zero)
		return(-zero);
	z = Math.atan2(y, x);
	return(roundnegative(z, 1));
    }

    public static double atan2positive(double y, double x) {
	double z;
	if (y == zero)
		return(+zero);
	z = Math.atan2(y, x);
	return(roundpositive(z, 1));
    }

    public static double coshnegative(double x) {
	double z;
	x = fabs(x);
	z = cosh(x);
	if (x < half)
		if (x == zero)
			return(one);
		else
		{
			if (z <= one)
				return(one);
			return(roundnegative(z, 1));
		}
	if (x >= 20)
		return(roundnegative(z, 1));
	return(roundnegative(z, 3));
    }

    public static double coshpositive(double x) {
	double z;
	x = fabs(x);
	z = cosh(x);
	if (x < half)
		if (x == zero)
			return(one);
		else
			return(roundpositive(z, 1));
	if (x >= 20)
		return(roundpositive(z, 1));
	return(roundpositive(z, 3));
    }

    private static double sinhl(double x) {
	double	z;
	int	h;
	h = highword(x) & 0x7fffffff;
	if (h < 0x3e500000)
		if (x == zero)
			return(-zero);
		else
			if (x > zero)
				return(x);
			else
				return(roundnegative(x, 1));
	z = sinh(x);
	if (fabs(x) < 20)
		return(roundnegative(z, 2));
	return(roundnegative(z, 1));
    }

    private static double sinhu(double x) {
	double	z;
	int	h;
	h = highword(x) & 0x7fffffff;
	if (h < 0x3e500000)
		if (x == zero)
			return(+zero);
		else
			if (x > zero)
				return(roundpositive(x, 1));
			else
				return(x);
	z = sinh(x);
	if (fabs(x) < 20)
		return(roundpositive(z, 2));
	return(roundpositive(z, 1));
    }

    public static double sinhnegative(double x) {
	if (x >= zero)
		return sinhl(x);
	else
		return -sinhu(-x);
    }

    public static double sinhpositive(double x) {
	if (x >= zero)
		return sinhu(x);
	else
		return -sinhl(-x);
    }

    private static double tanhl(double x) {
	double	z;
	int	h;
	h = highword(x) & 0x7fffffff;
	if (h < 0x3c800000)
		if (x == zero)
			return(-zero);
		else
			if (x > zero)
				return(roundnegative(x, 1));
			else
				return(x);
	z = tanh(x);
	if (h <= 0x3ff00000)
		return(roundnegative(z, 3));
	if (z <= -one)
		return(-one);
	return(roundnegative(z, 1));
    }

    private static double tanhu(double x) {
	double	z;
	int	h;
	h = highword(x) & 0x7fffffff;
	if (h < 0x3c800000)
		if (x == zero)
			return(+zero);
		else
			if (x > zero)
				return(x);
			else
				return(roundpositive(x, 1));
	z = tanh(x);
	if (h <= 0x3ff00000)
		return(roundpositive(z, 3));
	if (z >= +one)
		return(+one);
	return(roundpositive(z, 1));
    }

    public static double tanhnegative(double x) {
	if (x >= zero)
		return tanhl(x);
	else
		return -tanhu(-x);
    }

    public static double tanhpositive(double x) {
	if (x >= zero)
		return tanhu(x);
	else
		return -tanhl(-x);
    }

    private static float roundnegativef(float x, int n) {
	while (n-- > 0)
		x = prevf(x);
	return(x);
    }

    private static float roundpositivef(float x, int n) {
	while (n-- > 0)
		x = nextf(x);
	return(x);
    }

    public static float addnegative(float x, float y) {
	float z;
	z = x + y;
	if (z - y > x || z - x > y)
		z = prevf(z);
	return(z);
    }

    public static float addpositive(float x, float y) {
	float z;
	z = x + y;
	if (z - y < x || z - x < y)
		z = nextf(z);
	return(z);
    }

    public static float subnegative(float x, float y) {
	float z;
	z = x - y;
	if (z + y > x || x - z < y)
		z = prevf(z);
	return(z);
    }

    public static float subpositive(float x, float y) {
	float z;
	z = x - y;
	if (z + y < x || x - z > y)
		z = nextf(z);
	return(z);
    }

    public static float divnegative(float x, float y) {
	float	z;
	double	xu, yu, zu;
	xu = (double)x;
	yu = (double)y;
	zu = xu / yu;
	z = (float)zu;
	if (Float.isInfinite(z))
	{
		if (z > zerof)
			return(+Float.MAX_VALUE);
		return(Float.NEGATIVE_INFINITY);
	}
	/**
	if (z == zerof)
	{
		if (x == zerof)
			return(-zerof);
		if ((x > zerof && y > zerof) || (x < zerof && y < zerof))
			return(-zerof);
		return(-Float.MIN_VALUE);
	}
	**/
	if ((double)z > zu)
		z = prevf(z);
	return(z);
    }

    public static float divpositive(float x, float y) {
	float	z;
	double	xu, yu, zu;
	xu = (double)x;
	yu = (double)y;
	zu = xu / yu;
	z = (float)zu;
	if (Float.isInfinite(z))
	{
		if (z < zerof)
			return(-Float.MAX_VALUE);
		return(Float.POSITIVE_INFINITY);
	}
	/**
	if (z == zerof)
	{
		if (x == zerof)
			return(+zerof);
		if ((x > zerof && y < zerof) || (x < zerof && y > zerof))
			return(+zerof);
		return(+Float.MIN_VALUE);
	}
	**/
	if ((double)z < zu)
		z = nextf(z);
	return(z);
    }

    public static float mulnegative(float x, float y) {
	float	z;
	double	xu, yu, zu;
	xu = (double)x;
	yu = (double)y;
	zu = xu * yu;
	z = (float)zu;
	if (Float.isInfinite(z))
	{
		if (z > zerof)
			return(+Float.MAX_VALUE);
		return(Float.NEGATIVE_INFINITY);
	}
	/**
	if (z == zerof)
	{
		if (x == zerof || y == zerof)
			return(-zerof);
		if ((x > zerof && y > zerof) || (x < zerof && y < zerof))
			return(-zerof);
		return(-Float.MIN_VALUE);
	}
	**/
	if ((double)z > zu)
		z = prevf(z);
	return(z);
    }

    public static float mulpositive(float x, float y) {
	float	z;
	double	xu, yu, zu;
	xu = (double)x;
	yu = (double)y;
	zu = xu * yu;
	z = (float)zu;
	if (Float.isInfinite(z))
	{
		if (z < zerof)
			return(-Float.MAX_VALUE);
		return(Float.POSITIVE_INFINITY);
	}
	/**
	if (z == zerof)
	{
		if (x == zerof || y == zerof)
			return(+zerof);
		if ((x > zerof && y < zerof) || (x < zerof && y > zerof))
			return(+zerof);
		return(+Float.MIN_VALUE);
	}
	**/
	if ((double)z < zu)
		z = nextf(z);
	return(z);
    }

    public static float sqrtnegative(float x) {
	float	z;
	double	xu, zu;
	xu = (double)x;
	zu = Math.sqrt(xu);
	z = (float)zu;
	if (Float.isInfinite(z))
		return(+Float.MAX_VALUE);
	if ((double)z > zu)
		z = prevf(z);
	return(z);
    }

    public static float sqrtpositive(float x) {
	float	z;
	double	xu, zu;
	xu = (double)x;
	zu = Math.sqrt(xu);
	z = (float)zu;
	/**
	if (Float.isInfinite(z))
		return(Float.POSITIVE_INFINITY);
	**/
	if ((double)z < zu)
		z = nextf(z);
	return(z);
    }

    public static float sqrnegative(float x) {
	float	z;
	double	xu;
	xu = (double)x;
	xu *= xu;
	z = (float)xu;
	if (Float.isInfinite(z))
		return(+Float.MAX_VALUE);
	if ((double)z > xu)
		z = prevf(z);
	return(z);
    }

    public static float sqrpositive(float x) {
	float	z;
	double	xu;
	xu = (double)x;
	xu *= xu;
	z = (float)xu;
	/**
	if (Float.isInfinite(z))
		return(Float.POSITIVE_INFINITY);
	**/
	if ((double)z < xu)
		z = nextf(z);
	return(z);
    }

    public static float cbrtnegative(float x) {
	float	z;
	double	xu, zu;
	xu = (double)x;
	zu = cbrt(xu);
	z = (float)zu;
	if (Float.isInfinite(z))
	{
		if (z > zerof)
			return(+Float.MAX_VALUE);
		return(Float.NEGATIVE_INFINITY);
	}
	if ((double)z > zu)
		z = prevf(z);
	return(z);
    }

    public static float cbrtpositive(float x) {
	float	z;
	double	xu, zu;
	xu = (double)x;
	zu = cbrt(xu);
	z = (float)zu;
	if (Float.isInfinite(z))
	{
		if (z < zerof)
			return(-Float.MAX_VALUE);
		return(Float.POSITIVE_INFINITY);
	}
	if ((double)z < zu)
		z = nextf(z);
	return(z);
    }

    public static float powernegative(float a, float b) {
	float	c;
	double	d;
	if (a == zerof && b > zerof)
		return(-zerof);
	if (b == onef)
		return(a);
	if (a == onef)
		return(a);
	if (b == zerof)
		return(onef);
	if (b == halff)
		return(sqrtnegative(a));
	if (b == -onef)
		return(divnegative(onef, a));
	if (b == twof)
		return(sqrnegative(a));
	if (a == -onef)
		if (evenf(b))
			return(onef);
		else
			return(-onef);
	d = Math.pow((double)a, (double)b);
	c = (float)d;
	if (Float.isInfinite(c))
	{
		if (c > zerof)
			return(Float.MAX_VALUE);
		return(c);
	}
	if (c == zerof)
	{
		if (a > zerof || evenf(b))
			return(-zerof);
		return(-Float.MIN_VALUE);
	}
	if (a > zerof && a < onef && b < zerof && b > -onef)
		if (c <= onef)
			return(onef);
	if (a > onef && b > zerof && b < onef)
		if (c <= onef)
			return(onef);
	if (c < d)
		return(c);
	d = prev(d);
	return(doubleTofloatnegative(d));
    }

    public static float powerpositive(float a, float b) {
	float	c;
	double	d;
	if (a == zerof && b > zerof)
		return(+zerof);
	if (b == onef)
		return(a);
	if (a == onef)
		return(a);
	if (b == zerof)
		return(onef);
	if (b == halff)
		return(sqrtpositive(a));
	if (b == -onef)
		return(divpositive(onef, a));
	if (b == twof)
		return(sqrpositive(a));
	if (a == -onef)
		if (evenf(b))
			return(onef);
		else
			return(-onef);
	d = Math.pow((double)a, (double)b);
	c = (float)d;
	if (Float.isInfinite(c))
	{
		if (c < zerof)
			return(-Float.MAX_VALUE);
		return(c);
	}
	if (c == zerof)
	{
		if (a > zerof || evenf(b))
			return(Float.MIN_VALUE);
		return(+zerof);
	}
	if (a > zerof && a < onef && b > zerof && b < onef)
		if (c >= onef)
			return(onef);
	if (a > onef && b < zerof && b > -onef)
		if (c >= onef)
			return(onef);
	if (c > d)
		return(c);
	d = next(d);
	return(doubleTofloatpositive(d));
    }

    public static float expnegative(float x) {
	double a;
	if (x == zerof)
		return(onef);
	a = Math.exp((double)x);
	a = prev(a);
	if (a <= zero)
		return(-zerof);
	if (x > zerof && a <= one)
		return(onef);
	return(doubleTofloatnegative(a));
    }

    public static float exppositive(float x) {
	double a;
	if (x == zerof)
		return(onef);
	a = Math.exp((double)x);
	a = next(a);
	if (x < zerof && a >= one)
		return(onef);
	return(doubleTofloatpositive(a));
    }

    public static float lognegative(float x) {
	double a;
	if (x == onef)
		return(-zerof);
	a = Math.log((double)x);
	a = prev(a);
	return(doubleTofloatnegative(a));
    }

    public static float logpositive(float x) {
	double a;
	if (x == onef)
		return(+zerof);
	a = Math.log((double)x);
	a = next(a);
	return(doubleTofloatpositive(a));
    }

    public static float log10negative(float x) {
	double a;
	if (x == onef)
		return(-zerof);
	a = log10((double)x);
	if (x >= tenf && x <= onetwentytwof && aintf(x) == x &&
		a >= one && a <= twentytwof && aint(a) == a)
			return((float)a);
	a = prev(a);
	return(doubleTofloatnegative(a));
    }

    public static float log10positive(float x) {
	double a;
	if (x == onef)
		return(+zerof);
	a = log10((double)x);
	if (x >= tenf && x <= onetwentytwof && aintf(x) == x &&
		a >= one && a <= twentytwof && aint(a) == a)
			return((float)a);
	a = next(a);
	return(doubleTofloatpositive(a));
    }

    public static float cosnegative(float x) {
	double a;
	if (x == zerof)
		return(onef);
	a = kernel_cos((double)x, zero);
	a = prev(a);
	if (a <= -one)
		return(-onef);
	return(doubleTofloatnegative(a));
    }

    public static float cospositive(float x) {
	double a;
	if (x == zerof)
		return(onef);
	a = kernel_cos((double)x, zero);
	a = next(a);
	if (a >= +one)
		return(+onef);
	return(doubleTofloatpositive(a));
    }

    public static float cosnegativef(double x, double y, int n) {
	double a;
	if (x == zerof)
		return(onef);
	a = k_cos(x, y, n);
	a = prev(a);
	if (a <= -one)
		return(-onef);
	return(doubleTofloatnegative(a));
    }

    public static float cospositivef(double x, double y, int n) {
	double a;
	if (x == zerof)
		return(onef);
	a = k_cos(x, y, n);
	a = next(a);
	if (a >= +one)
		return(+onef);
	return(doubleTofloatpositive(a));
    }

    public static float sinnegative(float x) {
	double a;
	if (x == zerof)
		return(-zerof);
	a = kernel_sin((double)x, zero, true);
	a = prev(a);
	if (a <= -one)
		return(-onef);
	return(doubleTofloatnegative(a));
    }

    public static float sinpositive(float x) {
	double a;
	if (x == zerof)
		return(+zerof);
	a = kernel_sin((double)x, zero, true);
	a = next(a);
	if (a >= +one)
		return(+onef);
	return(doubleTofloatpositive(a));
    }

    public static float sinnegativef(double x, double y, int n) {
	double a;
	if (x == zerof)
		return(-zerof);
	a = k_sin(x, y, n);
	a = prev(a);
	if (a <= -one)
		return(-onef);
	return(doubleTofloatnegative(a));
    }

    public static float sinpositivef(double x, double y, int n) {
	double a;
	if (x == zerof)
		return(+zerof);
	a = k_sin(x, y, n);
	a = next(a);
	if (a >= +one)
		return(+onef);
	return(doubleTofloatpositive(a));
    }

    public static float tannegative(float x) {
	double a;
	if (x == zerof)
		return(-zerof);
	a = Math.tan((double)x);
	a = prev(a);
	return(doubleTofloatnegative(a));
    }

    public static float tanpositive(float x) {
	double a;
	if (x == zerof)
		return(+zerof);
	a = Math.tan((double)x);
	a = next(a);
	return(doubleTofloatpositive(a));
    }

    public static float acosnegative(float x) {
	double a;
	if (x == onef)
		return(-zerof);
	a = Math.acos((double)x);
	a = prev(a);
	return(doubleTofloatnegative(a));
    }

    public static float acospositive(float x) {
	double a;
	if (x == onef)
		return(+zerof);
	a = Math.acos((double)x);
	a = next(a);
	return(doubleTofloatpositive(a));
    }

    public static float asinnegative(float x) {
	double a;
	if (x == zerof)
		return(-zerof);
	a = Math.asin((double)x);
	a = prev(a);
	return(doubleTofloatnegative(a));
    }

    public static float asinpositive(float x) {
	double a;
	if (x == zerof)
		return(+zerof);
	a = Math.asin((double)x);
	a = next(a);
	return(doubleTofloatpositive(a));
    }

    public static float atannegative(float x) {
	double a;
	if (x == zerof)
		return(-zerof);
	a = Math.atan((double)x);
	a = prev(a);
	return(doubleTofloatnegative(a));
    }

    public static float atanpositive(float x) {
	double a;
	if (x == zerof)
		return(+zerof);
	a = Math.atan((double)x);
	a = next(a);
	return(doubleTofloatpositive(a));
    }

    public static float atan2negative(float y, float x) {
	double a;
	if (y == zerof)
		return(-zerof);
	a = Math.atan2((double)y, (double)x);
	a = prev(a);
	return(doubleTofloatnegative(a));
    }

    public static float atan2positive(float y, float x) {
	double a;
	if (y == zerof)
		return(+zerof);
	a = Math.atan2((double)y, (double)x);
	a = next(a);
	return(doubleTofloatpositive(a));
    }

    public static float coshnegative(float x) {
	double a;
	if (x == zerof)
		return(onef);
	x = fabsf(x);
	a = cosh((double)x);
	a = prev(a);
	if (a <= one)
		return(onef);
	return(doubleTofloatnegative(a));
    }

    public static float coshpositive(float x) {
	double a;
	if (x == zerof)
		return(onef);
	x = fabsf(x);
	a = cosh((double)x);
	a = next(a);
	return(doubleTofloatpositive(a));
    }

    private static float sinhl(float x) {
	double a;
	if (x == zerof)
		return(-zerof);
	a = sinh((double)x);
	a = prev(a);
	return(doubleTofloatnegative(a));
    }

    private static float sinhu(float x) {
	double a;
	if (x == zerof)
		return(+zerof);
	a = sinh((double)x);
	a = next(a);
	return(doubleTofloatpositive(a));
    }

    public static float sinhnegative(float x) {
	if (x >= zerof)
		return sinhl(x);
	else
		return -sinhu(-x);
    }

    public static float sinhpositive(float x) {
	if (x >= zerof)
		return sinhu(x);
	else
		return -sinhl(-x);
    }

    private static float tanhl(float x) {
	double a;
	if (x == zerof)
		return(-zerof);
	a = tanh((double)x);
	a = prev(a);
	if (a <= -one)
		return(-onef);
	return(doubleTofloatnegative(a));
    }

    private static float tanhu(float x) {
	double a;
	if (x == zerof)
		return(+zerof);
	a = tanh((double)x);
	a = next(a);
	if (a >= +one)
		return(+onef);
	return(doubleTofloatpositive(a));
    }

    public static float tanhnegative(float x) {
	if (x >= zerof)
		return tanhl(x);
	else
		return -tanhu(-x);
    }

    public static float tanhpositive(float x) {
	if (x >= zerof)
		return tanhu(x);
	else
		return -tanhl(-x);
    }
}
