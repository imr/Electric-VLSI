/**
 * #pragma ident "@(#)Intervalio.java 1.1 (Sun Microsystems, Inc.) 02/12/06"
 */
/**
 *  Copyright © 2002 Sun Microsystems, Inc.
 *  Alexei Agafonov aga@nbsp.nsk.su
 *  All rights reserved.
 */
/**
	Java interval input output class
*/
import java.io.*;
import java.lang.*;
import Arith;

public class Intervalio
{
    /* Fields */

    private	int	fpclass;
    private	boolean	sign;
    private	int	exponent;
    private	char[]	ds;
    private	boolean	more;
    private	int	ndigits;
    private	int	index;

    /* Constants */

    private final static double zero	= 0.0;
    private final static double ten	= 10.0;
    private final static float zerof	= 0.0f;
    private final static float tenf	= 10.0f;

    private final static int maxAccuracy	= 16;
    private final static int maxAccuracyf	= 7;

    private final static int fpNearest		= 0;
    private final static int fpToZero		= 1;
    private final static int fpPositive		= 2;
    private final static int fpNegative		= 3;
    private final static int fpChop		= 4;

    private final static int fpZero		= 0;
    private final static int fpSubNormal	= 1;
    private final static int fpNormal		= 2;
    private final static int fpInfinity		= 3;
    private final static int fpNaN		= 4;

    private final static int dslength		= 512;

    private final static int fpValiDival			= 0;
    private final static int fpUnassociatedInputVariable	= -1;
    private final static int fpInValiDival			= -2;
    private final static int fpBaDival				= -3;
    private final static int fpBaDFormat			= -4;
    private final static int fpBaDScaleFactor			= -5;

    private final static char letter	= 'E';

    /* Constructor */

    private Intervalio() {
	fpclass		= fpZero;
	sign		= false;
	exponent	= 0;
	ds		= new char[dslength];
	flushbuffer(ds, dslength);
	more		= false;
	ndigits		= 0;
	index		= 0;
    }

    private static void printbuffer(char[] buf) {
	int n;
	for (n = 0;; n++) {
		System.out.print(buf[n]);
		if (buf[n] == (char)0)
			break;
	}
	System.out.println();
    }

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

    private static boolean isEmpty(double l, double u) {
	return(Double.isNaN(l) && Double.isNaN(u));
    }

    private static boolean isEmptyf(float l, float u) {
	return(Float.isNaN(l) && Float.isNaN(u));
    }

    private static boolean isDegenerate(double l, double u) {
	return(l == u);
    }

    private static boolean isDegeneratef(float l, float u) {
	return(l == u);
    }

    private static boolean isTiny(double x)
    {
	long l;
	l = Double.doubleToLongBits(x) & 0x7fffffffffffffffl;
	if (l < 0x0000000000000005l)
		return(true);
	return(false);
    }

    private static boolean isTinyf(float x)
    {
	int i;
	i = Float.floatToIntBits(x) & 0x7fffffff;
	if (i < 0x00000010)
		return(true);
	return(false);
    }

    private static boolean isDigit(char x)
    {
	return(
		x == '0' ||
		x == '1' ||
		x == '2' ||
		x == '3' ||
		x == '4' ||
		x == '5' ||
		x == '6' ||
		x == '7' ||
		x == '8' ||
		x == '9'
	);
    }

    private static boolean isAlpha(char x) {
	return(x == 'e' || x == 'E');
    }

    private static void flushbuffer(char buf[], int n) {
	while (--n >= 0)
		buf[n] = (char)0;
    }

    private static int ilog10(double x) {
	int o;
	if (x < zero)
		x = -x;
	o = (int)Math.floor(Arith.log10(x));
	return(o);
    }

    private static int ilog10f(float x) {
	int o;
	if (x < zerof)
		x = -x;
	o = (int)Math.floor(Arith.log10((double)x));
	return(o);
    }

    private static String doubleTostring(double x) {
	String str;
	str = String.valueOf(x);
	/* or
	str = Double.toString(x);
	or */
	return(str);
    }

    private static double stringTodouble(String x) {
	double z;
	Double y;
	y = Double.valueOf(x);
	z = y.doubleValue();
	/* or
	z = Double.parseDouble(x);
	or */
	return(z);
    }

    private static String floatTostring(float x) {
	String str;
	str = String.valueOf(x);
	/* or
	str = Float.toString(x);
	or */
	return(str);
    }

    private static float stringTofloat(String x) {
	float z;
	Float y;
	y = Float.valueOf(x);
	z = y.floatValue();
	/* or
	z = Float.parseFloat(x);
	or */
	return(z);
    }

    private static String intTostring(int x) {
	String str;
	str = String.valueOf(x);
	/* or
	str = Integer.toString(x);
	or */
	return(str);
    }

    private static int stringToint(String x) {
	int	z;
	Integer	y;
	y = Integer.valueOf(x);
	z = y.intValue();
	return(z);
    }

    private static String longTostring(long x) {
	String str;
	str = String.valueOf(x);
	/* or
	str = Long.toString(x);
	or */
	return(str);
    }

    private static long stringTolong(String x) {
	long z;
	Long y;
	y = Long.valueOf(x);
	z = y.longValue();
	return(z);
    }

    private static boolean isExact(double x) {
	int n;
	long l;
	n = Arith.ilogb(x);
	if (n < -20 || n > 52)
		return(false);
	l = Double.doubleToLongBits(x);
	l &= 0x000fffffffffffffl;
	if (l == 0l)
		return(true);
	if (n < -1)
		return(false);
	l &= 0x00000000ffffffffl;
	if (l == 0l)
		return(true);
	if (Arith.aint(x) == x)
		return(true);
	return(false);
    }

    private static boolean isExactf(float x) {
	int i, n;
	n = Arith.ilogbf(x);
	if (n < -10 || n > 23)
		return(false);
	i = Float.floatToIntBits(x);
	i &= 0x007fffff;
	if (i == 0)
		return(true);
	if ( n < -1)
		return(false);
	i &= 0x0000ffff;
	if (i == 0)
		return(true);
	if (Arith.aintf(x) == x)
		return(true);
	return(false);
    }

    private static void stringToarray(String x, char[] y) {
	int ln;
	ln = x.length();
	x.getChars(0, ln, y, 0);
    }

    private static String arrayTostring(char[] x) {
	String str;
	int n;
	for (n = 0; n < dslength; n++)
		if (x[n] == (char)0)
			break;
	str = String.valueOf(x, 0, n);
	return(str);
    }

    private static int strlen(char buf[]) {
	int n;
	n = 0;
	while (buf[n] != (char)0)
		n++;
	return(n);
    }

    private static boolean strcmp(char bufto[], char buffrom[]) {
	int n;
	n = 0;
	for (;;)
	{
		if (bufto[n] != buffrom[n])
			return(false);
		if (bufto[n] == (char)0 || buffrom[n] == (char)0)
			break;
		n++;
	}
	return(true);
    }

    private static void strcpy(char bufto[], char buffrom[], int n) {
	while (--n >= 0)
		bufto[n] = buffrom[n];
    }

    private static char intTochar(int x) {
	char s;
	if (x < 0)
		x = -x;
	switch(x)
	{
		case 0:
			s = '0';
			break;
		case 1:
			s = '1';
			break;
		case 2:
			s = '2';
			break;
		case 3:
			s = '3';
			break;
		case 4:
			s = '4';
			break;
		case 5:
			s = '5';
			break;
		case 6:
			s = '6';
			break;
		case 7:
			s = '7';
			break;
		case 8:
			s = '8';
			break;
		case 9:
			s = '9';
			break;
		default:
			s = (char)0;
			break;
	}
	return(s);
    }

    private static int charToint(char x) {
	int i;
	switch(x)
	{
		case '0':
			i = 0;
			break;
		case '1':
			i = 1;
			break;
		case '2':
			i = 2;
			break;
		case '3':
			i = 3;
			break;
		case '4':
			i = 4;
			break;
		case '5':
			i = 5;
			break;
		case '6':
			i = 6;
			break;
		case '7':
			i = 7;
			break;
		case '8':
			i = 8;
			break;
		case '9':
			i = 9;
			break;
		default:
			i = -1;
			break;
	}
	return(i);
    }

    private static int getExponent(char buf[]) {
	int	exp, d, n, i;
	boolean	sign, isexp;
	char	sym;
	exp	= 0;
	d	= 1;
	n	= 0;
	i	= 0;
	sign	= false;
	isexp	= false;
	for (;;)
	{
		sym = buf[i];
		if (sym == 'e' || sym =='E')
			isexp = true;
		if (sym == (char)0)
			break;
		i++;
	}
	if (! isexp)
		return(exp);
	for (;;)
	{
		sym = buf[--i];
		if (sym == 'e' || sym == 'E')
			break;
		if (sym == '-')
		{
			sign = true;
			continue;
		}
		if (sym == '+')
			continue;
		n = charToint(sym);
		exp += d * n;
		d *= 10;
	}
	if (sign)
		exp = -exp;
	return(exp);
    }

    private static void recordExponent(char buf[], int to, int exp) {
	int	n, o, m;
	char[]	bufexp;
	bufexp	= new char[16];
	flushbuffer(bufexp, 16);
	for (;;)
	{
		if (buf[to] == (char)0)
			break;
		to++;
	}
	buf[to++] = 'e';
	if (exp < 0)
	{
		buf[to++] = '-';
		exp = -exp;
	}
	else
		buf[to++] = '+';
	if (exp <= 9)
	{
		buf[to++] = intTochar(exp);
		buf[to] = (char)0;
		return;
	}
	m = 0;
	while (exp > 0)
	{
		o = exp / 10;
		n = exp - 10 * o;
		bufexp[m++] = intTochar(n);
		exp = o;
	}
	while (m > 0)
		buf[to++] = bufexp[--m];
	buf[to] = (char)0;
	return;
    }

    private static int correctExponent(char buffrom[], int exp)
    {
	int		from;
	boolean		waspoint;
	char		sym;
	from		= 0;
	waspoint	= false;
	while (buffrom[from] == '0')
		from++;
	for (;;)
	{
		sym = buffrom[from];
		if (sym == 'e' || sym == 'E' || sym == (char)0)
			break;
		if (waspoint)
			exp--;
		if (sym == '.')
			waspoint = true;
		from++;
	}
	for (;;)
	{
		sym = buffrom[--from];
		if (sym == '0' || sym == '.')
		{
			if (sym == '0')
				exp++;
		}
		else
			break;
	}
	return(exp);
    }

    private static int normFraction(char buffrom[], char bufto[])
    {
	int	from, to;
	char	sym;
	from	= 0;
	to	= 0;
	for (;;)
	{
		sym = buffrom[from];
		if (sym == '0' || sym == '.')
		{
			from++;
			continue;
		}
		break;
	}
	for (;;)
	{
		sym = buffrom[from++];
		if (sym == 'e' || sym == 'E' || sym == (char)0)
			break;
		if (sym == '.')
			continue;
		bufto[to++] = sym;
	}
	while(bufto[--to] == '0')
		bufto[to] = (char)0;
	return(++to);
    }

    private static boolean allzero(char buf[], int ndigits)
    {
	if (ndigits <= 0)
		return(true);
	for (ndigits--; ndigits >= 0; ndigits--)
		if (buf[ndigits] != '0')
			return(false);
	return(true);
    }	/* allzero() */

    private static boolean stringminusone(char buf[], int ndigits)
    {
	int	n;
	char	sym;
	if (ndigits <= 0)
		return(false);
	n = --ndigits;
	for (; ndigits >= 0; ndigits--)
	{
		sym = buf[ndigits];
		if (sym == '0')
			buf[ndigits] = '9';
		else
		{
			buf[ndigits] = (char)((int)sym - 1);
			if (ndigits == 0 && n > 0)
			{
				if (buf[ndigits] == '0')
				{
					buf[ndigits]	= '9';
					buf[n]		= (char)0;
					return(true);
				}
			}
			return(false);
		}
	}
	return(false);
    }	/* stringminusone() */

    private static boolean stringplusone(char buf[], int ndigits)
    {
	int	n;
	char	sym;
	if (ndigits <= 0)
		return(false);
	n = ndigits--;
	for (; ndigits >= 0; ndigits--)
	{
		sym = buf[ndigits];
		if (sym == '9')
		{
			if (ndigits == 0)
			{
				buf[ndigits]	= '1';
				buf[n++]	= '0';
				buf[n]		= (char)0;
				return(true);
			}
			buf[ndigits] = '0';
		}
		else
		{
			buf[ndigits] = (char)((int)sym + 1);
			return(false);
		}
	}
	return(false);
    }	/* stringplusone() */

    private static void minusone(Intervalio bf)
    {
	if (bf.fpclass == fpSubNormal)
		bf.fpclass = fpNormal;
	if ((bf.fpclass != fpNormal) && (bf.fpclass != fpZero))
		return;
	if ((bf.fpclass == fpZero)	||
		(bf.ndigits == 0)	||
		allzero(bf.ds, bf.ndigits)
	) {
		bf.fpclass	= fpNormal;
		bf.ndigits	= 1;
		bf.sign		= true;
		bf.more		= false;
		bf.ds[0]	= '1';
		bf.ds[1]	= (char)0;
		return;
	}
	if (! bf.sign && ! allzero(bf.ds, bf.ndigits))
	{
		if (stringminusone(bf.ds, bf.ndigits))
			bf.ndigits--;
		else
			if (allzero(bf.ds, bf.ndigits))
			{
				bf.fpclass	= fpZero;
				bf.ndigits	= 0;
				bf.ds[0]	= (char)0;
			}
	}
	else
	{
		if (stringplusone(bf.ds, bf.ndigits))
			bf.ndigits++;
		bf.sign = true;
	}
    }	/* minusone() */

    private static void plusone(Intervalio bf)
    {
	if (bf.fpclass == fpSubNormal)
		bf.fpclass = fpNormal;
	if ((bf.fpclass != fpNormal) && (bf.fpclass != fpZero))
		return;
	if ((bf.fpclass == fpZero)	||
		(bf.ndigits == 0)	||
		allzero(bf.ds, bf.ndigits)
	) {
		bf.fpclass	= fpNormal;
		bf.ndigits	= 1;
		bf.sign		= false;
		bf.more		= false;
		bf.ds[0]	= '1';
		bf.ds[1]	= (char)0;
		return;
	}
	if (bf.sign && ! allzero(bf.ds, bf.ndigits))
	{
		if (stringminusone(bf.ds, bf.ndigits))
			bf.ndigits--;
		else
			if (allzero(bf.ds, bf.ndigits))
			{
				bf.fpclass	= fpZero;
				bf.ndigits	= 0;
				bf.ds[0]	= (char)0;
			}
	}
	else
	{
		if (stringplusone(bf.ds, bf.ndigits))
			bf.ndigits++;
		bf.sign = false;
	}
    }	/* plusone() */

    private static void cpyivalio(Intervalio x, Intervalio y)
    {
	x.fpclass	= y.fpclass;
	x.sign		= y.sign;
	x.exponent	= y.exponent;
	x.more		= y.more;
	x.ndigits	= y.ndigits;
	x.index		= y.index;
	strcpy(x.ds, y.ds, dslength);
    }

    private static int testzero(Intervalio re, Intervalio im, int retval)
    {
	if ((re.fpclass == fpNormal) && (re.ndigits == 0))
		re.fpclass = fpZero;
	if ((im.fpclass == fpNormal) && (im.ndigits == 0))
		im.fpclass = fpZero;
	return(retval);
    }

    private static int onlyonefield
	(Intervalio re, Intervalio im, int lbracket, int retval)
    {
	cpyivalio(im, re);
	if (lbracket == 0)
	{
		minusone(re);
		plusone(im);
	}
	return(testzero(re, im, retval));
    }

    private static int fillreandfinish
    (
	Intervalio	re,
	Intervalio	im,
	int		lbracket,
	int		ndigits,
	int		dexp,
	int		retval
    ) {
	re.ds[ndigits] = (char)0;
	re.ndigits = ndigits;
	re.exponent = dexp;
	return(onlyonefield(re, im, lbracket, retval));
    }

    private static int torightend
    (
	Intervalio	re,
	Intervalio	im,
	char[]		buf,
	char		c,
	int		idx,
	int		idxend,
	boolean		empty,
	int		retval
    ) {
	while (c == ' ' || c == '\t')
	{
		if (++idx >= idxend)
			return(fpBaDival);
		c = buf[idx];
	}
	if ((c != ')') && (c != ']'))
		return(fpBaDival);
	if (empty)
	{
		if (c != ']')
			return(fpBaDival);
		/* Founded EMPTY interval */
		re.fpclass	= im.fpclass	= fpNaN;
		re.sign		= im.sign	= false;
		re.ds[0]	= im.ds[0]	= letter;
		return(retval);
	}
	return(testzero(re, im, retval));
    }

    private static int fromleftend
    (
	Intervalio	re,
	Intervalio	im,
	char[]		buf,
	char		c,
	int		dexp,
	boolean		digitseen,
	int		exponent,
	int		ndigits,
	int		idx,
	int		idxend,
	int		lbracket,
	int		rbracket,
	boolean		empty,
	boolean		nexp,
	int		retval
    ) {
	while (c == ' ' || c == '\t')
	{
		if (++idx >= idxend)
		{
			if (lbracket == 0)
				return(onlyonefield(re, im, lbracket, retval));
			return(fpBaDival);
		}
		c = buf[idx];
	}
	if ((lbracket == 0) && (c == ','))
	{
		cpyivalio(im, re);
		if (lbracket == 0)
		{
			minusone(re);
			plusone(im);
		}
		return(testzero(re, im, retval));
	}
	if (c == ']')
	{
		rbracket = 1;
		return(onlyonefield(re, im, lbracket, retval));
	}
	if (c == ')')
	{
		rbracket = 2;
		return(onlyonefield(re, im, lbracket, retval));
	}
	if (c != ',')
		return(fpBaDival);
	for (;;)
	{
		if (++idx >= idxend)
			return(fpBaDival);
		c = buf[idx];
		if (c != ' ' && c != '\t')
			break;
	}
	if (c == '+' || c == '-')
	{
		if (c == '-')
			im.sign = true;
		if (++idx >= idxend)
			return(fpBaDival);
		c = buf[idx];
	}
	if ((c == 'i') || (c == 'I'))
	{
		if ((idxend - idx++) < 2)
			return(fpBaDival);
		c = buf[idx++];
		if ((c != 'n') && (c != 'N'))
			return(fpBaDival);
		c = buf[idx];
		if ((c != 'f') && (c != 'F'))
			return(fpBaDival);
		/* Founded Infinity for right interval bound */
		im.fpclass = fpInfinity;
		if (++idx >= idxend)
			return(fpBaDival);
		c = buf[idx];
		if ((c == 'i') || (c == 'I'))
		{
			if ((idxend - idx++) < 3)
				return(fpBaDival);
			c = buf[idx++];
			if ((c != 'n') && (c != 'N'))
				return(fpBaDival);
			c = buf[idx++];
			if ((c != 'i') && (c != 'I'))
				return(fpBaDival);
			c = buf[idx++];
			if ((c != 't') && (c != 'T'))
				return(fpBaDival);
			c = buf[idx];
			if ((c != 'y') && (c != 'Y'))
				return(fpBaDival);
			if (++idx >= idxend)
				return(fpBaDival);
			c = buf[idx];
		}
		return(torightend(
			re, im,
			buf, c, idx, idxend,
			empty, retval)
		);
	}
	if ((c == 'n') || (c == 'N'))
	{
		if ((idxend - idx++) < 2)
			return(fpBaDival);
		c = buf[idx++];
		if ((c != 'a') && (c != 'A'))
			return(fpBaDival);
		c = buf[idx];
		if ((c != 'n') && (c != 'N'))
			return(fpBaDival);
		/* Founded NaN for right interval bound */
		im.fpclass = fpNaN;
		if (++idx >= idxend)
			return(fpBaDival);
		c = buf[idx];
		return(torightend(
			re, im,
			buf, c, idx, idxend,
			empty, retval)
		);
	}
	dexp		= 0;
	digitseen	= false;
	ndigits		= 0;
	while (c == '0')
	{
		digitseen = true;
		if (++idx >= idxend)
			return(fpBaDival);
		c = buf[idx];
	}
	while (c >= '0' && c <= '9')
	{
		digitseen = true;
		if (ndigits < (dslength - 1))
		{
			im.ds[ndigits] = c;
			++ndigits;
		}
		else
			if (c != '0')
				im.more = true;
		if (++idx >= idxend)
			return(fpBaDival);
		c = buf[idx];
	}
	if (c == '.')
		if (++idx >= idxend)
			return(fpBaDival);
	c = buf[idx];
	if (ndigits == 0) 
		while (c == '0')
		{
			digitseen = true;
			--dexp;
			if (++idx >= idxend)
				return(fpBaDival);
		 	c = buf[idx];
		}
	while (c >= '0' && c <= '9')
	{
		digitseen = true;
		if (ndigits < (dslength - 1))
		{
			--dexp;
			im.ds[ndigits] = c;
			++ndigits;
		}
		else
			if (c != (char)0)
				im.more = true;
		if (++idx >= idxend)
			return(fpBaDival);
		c = buf[idx];
	}
	if (! digitseen)
		return(fpBaDival);
	im.ds[ndigits] = (char)0;
	im.ndigits = ndigits;
	im.exponent = dexp;
	if (c == 'E' || c == 'e' || c == 'D' || c == 'd' || c == 'Q' || c == 'q')
	{
		if (++idx >= idxend)
			return(fpBaDival);
		digitseen = false;
		exponent = 0;
		nexp = false;
		c = buf[idx];
		if (c == '+' || c == '-')
		{
			if (c == '-')
				nexp = true;
			if (++idx >= idxend)
				return(fpBaDival);
			c = buf[idx];
		}
		while (c >= '0' && c <= '9')
		{
			digitseen = true;
			exponent = 10 * exponent + charToint(c);
			if (++idx >= idxend)
				return(fpBaDival);
			c = buf[idx];
		}
		if (! digitseen)
			return(fpBaDival);
		if (nexp)	im.exponent = dexp - exponent;
		else		im.exponent = dexp + exponent;
	}
	return(torightend(
		re, im,
		buf, c, idx, idxend,
		empty, retval)
	);
    }

    private static int decimalival(Intervalio re, Intervalio im, char[] buf)
    {
	char	c;
	int	dexp;
	boolean	digitseen;
	int	exponent;
	int	ndigits;
	int	idx;
	int	idxend;
	int	lbracket;
	int	rbracket;
	boolean	empty;
	boolean	nexp;
	int	retval;

	c		= (char)0;
	dexp		= 0;
	digitseen	= false;
	exponent	= 0;
	ndigits		= 0;
	idx		= 0;
	idxend		= 0;
	lbracket	= 0;
	rbracket	= 0;
	empty		= false;
	nexp		= false;
	retval		= fpValiDival;
	for (idx = 0; idx < dslength; idx++)
		if (buf[idx] == (char)0)
			break;
	idxend		= idx;
	lbracket	= 0;	/*  1 - '[' ;  2 - '(' */
	rbracket	= 0;	/*  1 - ']' ;  2 - ')' */
	re.fpclass	= im.fpclass	= fpNormal;
	re.sign		= im.sign	= false;
	re.ndigits	= im.ndigits	= 0;
	re.more		= im.more	= false;
	re.ds[0]	= im.ds[0]	= (char)0;
	re.exponent	= im.exponent	= 0;
	idx		= 0;
	while (buf[idx] == ' ')
		idx++;
	if (buf[idx] == '[')
		lbracket = 1;
	else
		if (buf[idx] == '(')
			lbracket = 2;
		else
			idx--;
	for (;;)
	{
		if (++idx >= idxend)
			return(fpBaDival);
		c = buf[idx];
		if (c != ' ' && c != '\t')
			break;
	}
	if ((lbracket == 1) && ((c == 'e') || (c == 'E')))
	{
		if ((idxend - idx++) < 4)
			return(fpBaDival);
		c = buf[idx++];
		if ((c != 'm') && (c != 'M'))
			return(fpBaDival);
		c = buf[idx++];
		if ((c != 'p') && (c != 'P'))
			return(fpBaDival);
		c = buf[idx++];
		if ((c != 't') && (c != 'T'))
			return(fpBaDival);
		c = buf[idx++];
		if ((c != 'y') && (c != 'Y'))
			return(fpBaDival);
		c = buf[idx];
		empty = true;
		return(torightend(
			re, im,
			buf, c, idx, idxend,
			empty, retval)
		);
	}
	if (c == '+' || c == '-')
	{
		if (c == '-')
			re.sign = true;
		if (++idx >= idxend)
			return(fpBaDival);
		c = buf[idx];
	}
	if ((c=='i') || (c=='I'))
	{
		if ((idxend - idx) < 2)
			return(fpBaDival);
		c = buf[++idx];
		if ((c != 'n') && (c !='N'))
			return(fpBaDival);
		c = buf[++idx];
		if ((c !='f') && (c != 'F'))
			return(fpBaDival);
		/* Founded Infinity for left interval bound */
		re.fpclass = fpInfinity;
		if (++idx >= idxend)
		{
			if (lbracket == 0)
				return(onlyonefield(re, im, lbracket, retval));
			else
				return(fpBaDival);
		}
		c = buf[idx];
		if ((c == 'i') || (c == 'I'))
		{
			if ((idxend - idx++) < 3)
				return(fpBaDival);
			c = buf[idx];
			if ((c != 'n') && (c != 'N'))
				return(fpBaDival);
			c = buf[++idx];
			if ((c != 'i') && (c != 'I'))
				return(fpBaDival);
			c = buf[++idx];
			if ((c != 't') && (c != 'T'))
				return(fpBaDival);
			c = buf[++idx];
			if ((c != 'y') && (c != 'Y'))
				return(fpBaDival);
			if (++idx >= idxend)
			{
				if (lbracket == 0)
					return(onlyonefield(re, im, lbracket, retval));
				else
					return(fpBaDival);
			}
			c = buf[idx];
		}
		return(fromleftend(
			re,
			im,
			buf,
			c,
			dexp,
			digitseen,
			exponent,
			ndigits,
			idx,
			idxend,
			lbracket,
			rbracket,
			empty,
			nexp,
			retval)
		);
	}
	if ((c == 'n') || (c == 'N'))
	{
		if ((idxend - idx++) < 2)
			return(fpBaDival);
		c = buf[idx++];
		if ((c != 'a') && (c != 'A'))
			return(fpBaDival);
		c = buf[idx];
		if ((c != 'n') && (c != 'N'))
			return(fpBaDival);

		/* Founded NaN for left interval bound */
		re.fpclass = fpNaN;
		if (++idx >= idxend)
		{
			if (lbracket == 0)
				return(onlyonefield(re, im, lbracket, retval));
			else
				return(fpBaDival);
		}
		c = buf[idx];
		return(fromleftend(
			re,
			im,
			buf,
			c,
			dexp,
			digitseen,
			exponent,
			ndigits,
			idx,
			idxend,
			lbracket,
			rbracket,
			empty,
			nexp,
			retval)
		);
	}
	dexp		= 0;
	digitseen	= false;
	ndigits		= 0;
	while (c == '0')
	{
		digitseen = true;
		if (++idx >= idxend)
		{
			if (lbracket == 0)
				return(onlyonefield(re, im, lbracket, retval));
			else
				return(fpBaDival);
		}
		c = buf[idx];
	}
	while (c >= '0' && c <= '9')
	{
		digitseen = true;
		if (ndigits < (dslength - 1))
		{
			re.ds[ndigits] = c;
			++ndigits;
		}
		else
			if (c != '0')
				re.more = true;
		if (++idx >= idxend)
		{
			if (lbracket == 0)
			{
				re.ds[ndigits] = (char)0;
				re.ndigits = ndigits;
				re.exponent = dexp;
				return(onlyonefield(re, im, lbracket, retval));
			}
			return(fpBaDival);
		}
		c = buf[idx];
	}
	if (c == '.')
	{
		if (++idx >= idxend)
		{
			if (lbracket == 0)
			{
				return(fillreandfinish(
					re,
					im,
					lbracket,
					ndigits,
					dexp,
					retval
					)
				);
			}
			return(fpBaDival);
		}
	}
	c = buf[idx];
	if (ndigits == 0)
	{
		while (c == '0')
		{
			digitseen = true;
			--dexp;
		 	if (++idx >= idxend)
		 	{
				if (lbracket == 0)
				{
					return(fillreandfinish(
						re,
						im,
						lbracket,
						ndigits,
						dexp,
						retval
						)
					);
				}
				return(fpBaDival);
			}
			c = buf[idx];
		}
	}
	while (c >= '0' && c <= '9')
	{
		digitseen = true;
		if (ndigits < (dslength - 1))
		{
			--dexp;
			re.ds[ndigits] = c;
			++ndigits;
		}
		else
			if (c != (char)0)
				im.more = true;
		if (++idx >= idxend)
		{
			if (lbracket == 0)
			{
				return(fillreandfinish(
					re,
					im,
					lbracket,
					ndigits,
					dexp,
					retval
					)
				);
			}
			return(fpBaDival);
		}
		c = buf[idx];
	}
	if (! digitseen)
		return(fpBaDival);
	re.ds[ndigits]	= (char)0;
	re.ndigits		= ndigits;
	re.exponent		= dexp;
	if (c == 'E' || c == 'e' || c == 'D' || c == 'd' || c == 'Q' || c == 'q')
	{
		if (++idx >= idxend)
			return(fpBaDival);
		digitseen	= false;
		exponent	= 0;
		nexp		= false;
		c		= buf[idx];
		if (c == '+' || c == '-')
		{
			if (c == '-')
				nexp = true;
			if (++idx >= idxend)
				return(fpBaDival);
			c = buf[idx];
		}
		while (c >= '0' && c <= '9')
		{
			digitseen = true;
			exponent = 10 * exponent + charToint(c);
			if (++idx >= idxend)
			{
				if (lbracket == 0)
				{
					if (nexp)
						re.exponent = dexp - exponent;
					else
						re.exponent = dexp + exponent;
					return(onlyonefield(re, im, lbracket, retval));
				}
				return(fpBaDival);
			}
			c = buf[idx];
		}
		if (! digitseen)
			return(fpBaDival);
		if (nexp)
			re.exponent = dexp - exponent;
		else
			re.exponent = dexp + exponent;
	}
	/*
	fromleftend()
	*/
	while (c == ' ' || c == '\t')
	{
		if (++idx >= idxend)
		{
			if (lbracket == 0)
				return(onlyonefield(re, im, lbracket, retval));
			return(fpBaDival);
		}
		c = buf[idx];
	}
	if ((lbracket == 0) && (c == ','))
	{
		cpyivalio(im, re);
		if (lbracket == 0)
		{
			minusone(re);
			plusone(im);
		}
		return(testzero(re, im, retval));
	}
	if (c == ']')
	{
		rbracket = 1;
		return(onlyonefield(re, im, lbracket, retval));
	}
	if (c == ')')
	{
		rbracket = 2;
		return(onlyonefield(re, im, lbracket, retval));
	}
	if (c != ',')
		return(fpBaDival);
	for (;;)
	{
		if (++idx >= idxend)
			return(fpBaDival);
		c = buf[idx];
		if (c != ' ' && c != '\t')
			break;
	}
	if (c == '+' || c == '-')
	{
		if (c == '-')
			im.sign = true;
		if (++idx >= idxend)
			return(fpBaDival);
		c = buf[idx];
	}
	if ((c == 'i') || (c == 'I'))
	{
		if ((idxend - idx++) < 2)
			return(fpBaDival);
		c = buf[idx++];
		if ((c != 'n') && (c != 'N'))
			return(fpBaDival);
		c = buf[idx];
		if ((c != 'f') && (c != 'F'))
			return(fpBaDival);
		/* Founded Infinity for right interval bound */
		im.fpclass = fpInfinity;
		if (++idx >= idxend)
			return(fpBaDival);
		c = buf[idx];
		if ((c == 'i') || (c == 'I'))
		{
			if ((idxend - idx++) < 3)
				return(fpBaDival);
			c = buf[idx++];
			if ((c != 'n') && (c != 'N'))
				return(fpBaDival);
			c = buf[idx++];
			if ((c != 'i') && (c != 'I'))
				return(fpBaDival);
			c = buf[idx++];
			if ((c != 't') && (c != 'T'))
				return(fpBaDival);
			c = buf[idx];
			if ((c != 'y') && (c != 'Y'))
				return(fpBaDival);
			if (++idx >= idxend)
				return(fpBaDival);
			c = buf[idx];
		}
		return(torightend(
			re, im,
			buf, c, idx, idxend,
			empty, retval)
		);
	}
	if ((c == 'n') || (c == 'N'))
	{
		if ((idxend - idx++) < 2)
			return(fpBaDival);
		c = buf[idx++];
		if ((c != 'a') && (c != 'A'))
			return(fpBaDival);
		c = buf[idx];
		if ((c != 'n') && (c != 'N'))
			return(fpBaDival);
		/* Founded NaN for right interval bound */
		im.fpclass = fpNaN;
		if (++idx >= idxend)
			return(fpBaDival);
		c = buf[idx];
		return(torightend(
			re, im,
			buf, c, idx, idxend,
			empty, retval)
		);
	}
	dexp		= 0;
	digitseen	= false;
	ndigits		= 0;
	while (c == '0')
	{
		digitseen = true;
		if (++idx >= idxend)
			return(fpBaDival);
		c = buf[idx];
	}
	while (c >= '0' && c <= '9')
	{
		digitseen = true;
		if (ndigits < (dslength - 1))
		{
			im.ds[ndigits] = c;
			++ndigits;
		}
		else
			if (c != '0')
				im.more = true;
		if (++idx >= idxend)
			return(fpBaDival);
		c = buf[idx];
	}
	if (c == '.')
		if (++idx >= idxend)
			return(fpBaDival);
	c = buf[idx];
	if (ndigits == 0) 
		while (c == '0')
		{
			digitseen = true;
			--dexp;
			if (++idx >= idxend)
				return(fpBaDival);
		 	c = buf[idx];
		}
	while (c >= '0' && c <= '9')
	{
		digitseen = true;
		if (ndigits < (dslength - 1))
		{
			--dexp;
			im.ds[ndigits] = c;
			++ndigits;
		}
		else
			if (c != (char)0)
				im.more = true;
		if (++idx >= idxend)
			return(fpBaDival);
		c = buf[idx];
	}
	if (! digitseen)
		return(fpBaDival);
	im.ds[ndigits]	= (char)0;
	im.ndigits		= ndigits;
	im.exponent		= dexp;
	if (c == 'E' || c == 'e' || c == 'D' || c == 'd' || c == 'Q' || c == 'q')
	{
		if (++idx >= idxend)
			return(fpBaDival);
		digitseen	= false;
		exponent	= 0;
		nexp		= false;
		c = buf[idx];
		if (c == '+' || c == '-')
		{
			if (c == '-')
				nexp = true;
			if (++idx >= idxend)
				return(fpBaDival);
			c = buf[idx];
		}
		while (c >= '0' && c <= '9')
		{
			digitseen = true;
			exponent = 10 * exponent + charToint(c);
			if (++idx >= idxend)
				return(fpBaDival);
			c = buf[idx];
		}
		if (! digitseen)
			return(fpBaDival);
		if (nexp)	im.exponent = dexp - exponent;
		else		im.exponent = dexp + exponent;
	}
	return(torightend(
		re, im,
		buf, c, idx, idxend,
		empty, retval)
	);
	/*
	end fromleftend()
	*/
    }	/* decimalival() */

    private static void readin(char[] buf, InputStream in) throws IOException
    {
	int	m, n, o;
	char	s;
	boolean	leftbracket, rightbracket, noneblank;
	o		= dslength;
	leftbracket	= false;
	rightbracket	= false;
	noneblank	= false;
	flushbuffer(buf, dslength);
	m = 0;
	for (;;)
	{
		if ((n = in.read()) == -1)
		{
			buf[m] = (char)0;
			break;
		}
		if (o <= 1)
		{
			buf[m] = (char)0;
			break;
		}
		s = (char)n;
		if (s == '\n')
		{
			buf[m] = (char)0;
			break;
		}
		if (s == (char)0)
		{
			buf[m] = (char)0;
			break;
		}
		if (! leftbracket)
		{
			if (s == '[')
				leftbracket = true;
		}
		if (! rightbracket)
		{
			if (s == ']')
				rightbracket = true;
		}
		if (! leftbracket && ! rightbracket)
		{
			if (s == ' ' || s == '\t')
			{
				if (! noneblank)
					continue;
				buf[m] = (char)0;
				break;
			}
		}
		if (rightbracket)
		{
			if (s == ' ' || s == '\t')
			{
				buf[m] = (char)0;
				break;
			}
		}
		if (s != ' ' || s != '\t')
			noneblank = true;
		buf[m++] = s;
		o--;
	}
    }

    public static void readin(char[] buf, InputStream in, int o)
    	throws IOException
    {
	int	m, n;
	char	s;
	boolean	leftbracket, rightbracket, noneblank;
	if (o <= 0)
		return;
	if (o > dslength)
		o = dslength;
	leftbracket	= false;
	rightbracket	= false;
	noneblank	= false;
	flushbuffer(buf, o);
	m = 0;
	for (;;)
	{
		if ((n = in.read()) == -1)
		{
			buf[m] = (char)0;
			break;
		}
		if (o <= 1)
		{
			buf[m] = (char)0;
			break;
		}
		s = (char)n;
		if (s == '\n')
		{
			buf[m] = (char)0;
			break;
		}
		if (s == (char)0)
		{
			buf[m] = (char)0;
			break;
		}
		if (! leftbracket)
		{
			if (s == '[')
				leftbracket = true;
		}
		if (! rightbracket)
		{
			if (s == ']')
				rightbracket = true;
		}
		if (! leftbracket && ! rightbracket)
		{
			if (s == ' ' || s == '\t')
			{
				if (! noneblank)
					continue;
				buf[m] = (char)0;
				break;
			}
		}
		if (rightbracket)
		{
			if (s == ' ' || s == '\t')
			{
				buf[m] = (char)0;
				break;
			}
		}
		if (s != ' ' || s != '\t')
			noneblank = true;
		buf[m++] = s;
		o--;
	}
    }

    public static double inTodouble(InputStream in) throws IOException
    {
	double a;
	char[] buf;
	String str;
	buf = new char[dslength];
	readin(buf, in);
	str = arrayTostring(buf);
	a = stringTodouble(str);
	return(a);
    }

    public static float inTofloat(InputStream in) throws IOException
    {
	float a;
	char[] buf;
	String str;
	buf = new char[dslength];
	readin(buf, in);
	str = arrayTostring(buf);
	a = stringTofloat(str);
	return(a);
    }

    public static int inToint(InputStream in) throws IOException
    {
	int a;
	char[] buf;
	String str;
	buf = new char[dslength];
	readin(buf, in);
	str = arrayTostring(buf);
	a = stringToint(str);
	return(a);
    }

    public static long inTolong(InputStream in) throws IOException
    {
	long a;
	char[] buf;
	String str;
	buf = new char[dslength];
	readin(buf, in);
	str = arrayTostring(buf);
	a = stringTolong(str);
	return(a);
    }

    private static double decimalTodouble(Intervalio bf, int rnd)
    {
	double	z;
	String	str;
	int	fpclass, n, exp, idx, ndigits;
	boolean	sign, isround;
	char[]	buf;

	sign = bf.sign;
	fpclass = bf.fpclass;
	switch (fpclass)
	{
		case fpZero:
			if (sign)
				return(-zero);
			else
				return(+zero);
		case fpNaN:
			if (sign)
				return(-Double.NaN);
			else
				return(+Double.NaN);
		case fpInfinity:
			if (sign)
				return(Double.NEGATIVE_INFINITY);
			else
				return(Double.POSITIVE_INFINITY);
		default:
			if (fpclass != fpNormal && fpclass != fpSubNormal)
				if (sign)
					return(-Double.NaN);
				else
					return(+Double.NaN);
			break;
	}
	ndigits = bf.ndigits;
	if (ndigits <= 0)
	{
		bf.fpclass = fpZero;
		if (sign)
			return(-zero);
		else
			return(+zero);
	}
	buf = new char[dslength];
	flushbuffer(buf, dslength);
	idx = 0;
	exp = bf.exponent;
	if (sign)
		buf[idx++] = '-';
	else
		buf[idx++] = '+';
	n = 0;
	buf[idx++] = bf.ds[n];
	buf[idx++] = '.';
	if (ndigits <= 1)
		buf[idx++] = '0';
	else
	{
		for (n = 1; n < ndigits; n++)
		{
			buf[idx++] = bf.ds[n];
			exp++;
		}
		while (--idx > 0)
		{
			if (buf[idx] == '0')
			{
				buf[idx] = (char)0;
				ndigits--;
			}
			else
			{
				idx++;
				break;
			}
		}
	}
	recordExponent(buf, idx, exp);
	isround = false;
	str = arrayTostring(buf);
	z = stringTodouble(str);
	if (z == zero)
		if (sign)
			switch (rnd)
			{
				case fpNegative:
					z = Arith.prev(z);
					return(z);
				case fpPositive:
					return(+zero);
				case fpToZero:
					return(-zero);
				case fpNearest:
					return(-zero);
				default:
					return(-zero);
			}
		else
			switch (rnd)
			{
				case fpNegative:
					return(-zero);
				case fpPositive:
					z = Arith.next(z);
					return(z);
				case fpToZero:
					return(+zero);
				case fpNearest:
					return(+zero);
				default:
					return(+zero);
			}
	if (ndigits > maxAccuracy)
		isround = true;
	else
		isround = ! isExact(z);
	if (isround)
		switch (rnd)
		{
			case fpNegative:
				z = Arith.prev(z);
				return(z);
			case fpPositive:
				z = Arith.next(z);
				return(z);
			case fpToZero:
				if (z < zero)
					z = Arith.next(z);
				else
					z = Arith.prev(z);
				return(z);
			case fpNearest:
				return(z);
			default:
				return(z);
		}
	return(z);
    }

    private static float decimalTofloat(Intervalio bf, int rnd)
    {
	float	z;
	double	o;
	String	str;
	int	fpclass, n, exp, idx, ndigits;
	boolean	sign, isround;
	char[]	buf;

	sign = bf.sign;
	fpclass = bf.fpclass;
	switch (fpclass)
	{
		case fpZero:
			if (sign)
				return(-zerof);
			else
				return(+zerof);
		case fpNaN:
			if (sign)
				return(-Float.NaN);
			else
				return(+Float.NaN);
		case fpInfinity:
			if (sign)
				return(Float.NEGATIVE_INFINITY);
			else
				return(Float.POSITIVE_INFINITY);
		default:
			if (fpclass != fpNormal && fpclass != fpSubNormal)
				if (sign)
					return(-Float.NaN);
				else
					return(+Float.NaN);
			break;
	}
	ndigits = bf.ndigits;
	if (ndigits <= 0)
	{
		bf.fpclass = fpZero;
		if (sign)
			return(-zerof);
		else
			return(+zerof);
	}
	buf = new char[dslength];
	flushbuffer(buf, dslength);
	idx = 0;
	exp = bf.exponent;
	if (sign)
		buf[idx++] = '-';
	else
		buf[idx++] = '+';
	n = 0;
	buf[idx++] = bf.ds[n];
	buf[idx++] = '.';
	if (ndigits <= 1)
		buf[idx++] = '0';
	else
	{
		for (n = 1; n < ndigits; n++)
		{
			buf[idx++] = bf.ds[n];
			exp++;
		}
		while (--idx > 0)
		{
			if (buf[idx] == '0')
			{
				buf[idx] = (char)0;
				ndigits--;
			}
			else
			{
				idx++;
				break;
			}
		}
	}
	recordExponent(buf, idx, exp);
	isround = false;
	str = arrayTostring(buf);
	z = stringTofloat(str);
	if (z == zerof)
		if (sign)
			switch (rnd)
			{
				case fpNegative:
					z = Arith.prevf(z);
					return(z);
				case fpPositive:
					return(+zerof);
				case fpToZero:
					return(-zerof);
				case fpNearest:
					return(-zerof);
				default:
					return(-zerof);
			}
		else
			switch (rnd)
			{
				case fpNegative:
					return(-zerof);
				case fpPositive:
					z = Arith.nextf(z);
					return(z);
				case fpToZero:
					return(+zerof);
				case fpNearest:
					return(+zerof);
				default:
					return(+zerof);
			}
	o = stringTodouble(str);
	if (ndigits > maxAccuracyf)
		isround = true;
	else
		isround = ! isExactf(z);
	if (isround)
	{
		switch (rnd)
		{
			case fpNegative:
				o = Arith.prev(o);
				z = Arith.doubleTofloatnegative(o);
				return(z);
			case fpPositive:
				o = Arith.next(o);
				z = Arith.doubleTofloatpositive(o);
				return(z);
			case fpToZero:
				if (z < zerof)
				{
					o = Arith.next(o);
					z = Arith.doubleTofloatpositive(o);
				}
				else
				{
					o = Arith.prev(o);
					z = Arith.doubleTofloatnegative(o);
				}
				return(z);
			case fpNearest:
				return(z);
			default:
				return(z);
		}
	}
	return(z);
    }

    public static int inTointerval(InputStream in, double[] l, double[] u)
	throws IOException
    {
	int		n, retval;
	Intervalio	re, im;
	char[]		buf;

	retval	= fpValiDival;
	buf	= new char[dslength];
	readin(buf, in);
	n = strlen(buf);
	if (n <= 0)
	{
		retval = fpUnassociatedInputVariable;
		l[0] = -Double.NaN;
		u[0] = +Double.NaN;
		return(retval);
	}
	re = new Intervalio();
	im = new Intervalio();
	retval = decimalival(re, im, buf);
	if (retval != fpValiDival)
	{
		l[0] = -Double.NaN;
		u[0] = +Double.NaN;
		return(retval);
	}
	if ((re.fpclass == fpNaN) && (im.fpclass == fpNaN) && (re.ds[0] == letter))
	{
		l[0] = -Double.NaN;
		u[0] = +Double.NaN;
		return(retval);
	}
	if (re.fpclass == fpZero)
		l[0] = re.sign ? -zero : zero;
	else
		l[0] = decimalTodouble(re, fpNegative);
	if (im.fpclass == fpZero)
		u[0] = im.sign ? -zero : zero;
	else
		u[0] = decimalTodouble(im, fpPositive);
	if (l[0] == u[0] && Double.isInfinite(l[0]))
		if (l[0] > zero)
			l[0] = +Double.MAX_VALUE;
		else
			u[0] = -Double.MAX_VALUE;
	if (Double.isNaN(l[0]) || Double.isNaN(l[0]) || l[0] > u[0])
	{
		retval = fpInValiDival;
		l[0] = Double.NEGATIVE_INFINITY;
		u[0] = Double.POSITIVE_INFINITY;
	}
	return(retval);
    }	/* inTointerval(InputStream, double[], double[]) */

    public static int stringTointerval(String x, double[] l, double[] u)
    {
	int		n, retval;
	Intervalio	re, im;
	char[]		buf;

	retval	= fpValiDival;
	buf	= new char[dslength];
	flushbuffer(buf, dslength);
	stringToarray(x, buf);
	n = strlen(buf);
	if (n <= 0)
	{
		retval = fpUnassociatedInputVariable;
		l[0] = -Double.NaN;
		u[0] = +Double.NaN;
		return(retval);
	}
	re = new Intervalio();
	im = new Intervalio();
	retval = decimalival(re, im, buf);
	if (retval != fpValiDival)
	{
		l[0] = -Double.NaN;
		u[0] = +Double.NaN;
		return(retval);
	}
	if ((re.fpclass == fpNaN) && (im.fpclass == fpNaN) && (re.ds[0] == letter))
	{
		l[0] = -Double.NaN;
		u[0] = +Double.NaN;
		return(retval);
	}
	if (re.fpclass == fpZero)
		l[0] = re.sign ? -zero : zero;
	else
		l[0] = decimalTodouble(re, fpNegative);
	if (im.fpclass == fpZero)
		u[0] = im.sign ? -zero : zero;
	else
		u[0] = decimalTodouble(im, fpPositive);
	if (l[0] == u[0] && Double.isInfinite(l[0]))
		if (l[0] > zero)
			l[0] = +Double.MAX_VALUE;
		else
			u[0] = -Double.MAX_VALUE;
	if (Double.isNaN(l[0]) || Double.isNaN(l[0]) || l[0] > u[0])
	{
		retval = fpInValiDival;
		l[0] = Double.NEGATIVE_INFINITY;
		u[0] = Double.POSITIVE_INFINITY;
	}
	return(retval);
    }	/* stringTointerval(String, double[], double[]) */

    public static int bufTointerval(char[] x, double[] l, double[] u)
    {
	int		n, retval;
	Intervalio	re, im;
	char[]		buf;

	retval	= fpValiDival;
	buf	= new char[dslength];
	flushbuffer(buf, dslength);
	n = strlen(x);
	if (n > dslength)
	{
		retval = fpUnassociatedInputVariable;
		l[0] = -Double.NaN;
		u[0] = +Double.NaN;
		return(retval);
	}
	strcpy(buf, x, n);
	if (n <= 0)
	{
		retval = fpUnassociatedInputVariable;
		l[0] = -Double.NaN;
		u[0] = +Double.NaN;
		return(retval);
	}
	re = new Intervalio();
	im = new Intervalio();
	retval = decimalival(re, im, buf);
	if (retval != fpValiDival)
	{
		l[0] = -Double.NaN;
		u[0] = +Double.NaN;
		return(retval);
	}
	if ((re.fpclass == fpNaN) && (im.fpclass == fpNaN) && (re.ds[0] == letter))
	{
		l[0] = -Double.NaN;
		u[0] = +Double.NaN;
		return(retval);
	}
	if (re.fpclass == fpZero)
		l[0] = re.sign ? -zero : zero;
	else
		l[0] = decimalTodouble(re, fpNegative);
	if (im.fpclass == fpZero)
		u[0] = im.sign ? -zero : zero;
	else
		u[0] = decimalTodouble(im, fpPositive);
	if (l[0] == u[0] && Double.isInfinite(l[0]))
		if (l[0] > zero)
			l[0] = +Double.MAX_VALUE;
		else
			u[0] = -Double.MAX_VALUE;
	if (Double.isNaN(l[0]) || Double.isNaN(l[0]) || l[0] > u[0])
	{
		retval = fpInValiDival;
		l[0] = Double.NEGATIVE_INFINITY;
		u[0] = Double.POSITIVE_INFINITY;
	}
	return(retval);
    }	/* bufTointerval(String, double[], double[]) */

    public static int inTointervalf(InputStream in, float[] l, float[] u)
	throws IOException
    {
	Intervalio	re;
	Intervalio	im;
	char[]		buf;
	int		n, retval;

	retval = fpValiDival;
	buf = new char[dslength];
	readin(buf, in);
	n = strlen(buf);
	if (n <= 0)
	{
		retval = fpUnassociatedInputVariable;
		l[0] = -Float.NaN;
		u[0] = +Float.NaN;
		return(retval);
	}
	re = new Intervalio();
	im = new Intervalio();
	retval = decimalival(re, im, buf);
	if (retval != fpValiDival)
	{
		l[0] = -Float.NaN;
		u[0] = +Float.NaN;
		return(retval);
	}
	if ((re.fpclass == fpNaN) && (im.fpclass == fpNaN) && (re.ds[0] == letter))
	{
		l[0] = -Float.NaN;
		u[0] = +Float.NaN;
		return(retval);
	}
	if (re.fpclass == fpZero)
		l[0] = re.sign ? -zerof : zerof;
	else
		l[0] = decimalTofloat(re, fpNegative);
	if (im.fpclass == fpZero)
		u[0] = im.sign ? -zerof : zerof;
	else
		u[0] = decimalTofloat(im, fpPositive);
	if (l[0] == u[0] && Float.isInfinite(l[0]))
		if (l[0] > zero)
			l[0] = +Float.MAX_VALUE;
		else
			u[0] = -Float.MAX_VALUE;
	if (Float.isNaN(l[0]) || Float.isNaN(l[0]) || l[0] > u[0])
	{
		retval = fpInValiDival;
		l[0] = Float.NEGATIVE_INFINITY;
		u[0] = Float.POSITIVE_INFINITY;
	}
	return(retval);
    }	/* inTointervalf(InputStream, float[], float[]) */

    public static int stringTointervalf(String x, float[] l, float[] u)
    {
	Intervalio	re;
	Intervalio	im;
	char[]		buf;
	int		n, retval;

	retval	= fpValiDival;
	buf	= new char[dslength];
	flushbuffer(buf, dslength);
	stringToarray(x, buf);
	n = strlen(buf);
	if (n <= 0)
	{
		retval = fpUnassociatedInputVariable;
		l[0] = -Float.NaN;
		u[0] = +Float.NaN;
		return(retval);
	}
	re = new Intervalio();
	im = new Intervalio();
	retval = decimalival(re, im, buf);
	if (retval != fpValiDival)
	{
		l[0] = -Float.NaN;
		u[0] = +Float.NaN;
		return(retval);
	}
	if ((re.fpclass == fpNaN) && (im.fpclass == fpNaN) && (re.ds[0] == letter))
	{
		l[0] = -Float.NaN;
		u[0] = +Float.NaN;
		return(retval);
	}
	if (re.fpclass == fpZero)
		l[0] = re.sign ? -zerof : zerof;
	else
		l[0] = decimalTofloat(re, fpNegative);
	if (im.fpclass == fpZero)
		u[0] = im.sign ? -zerof : zerof;
	else
		u[0] = decimalTofloat(im, fpPositive);
	if (l[0] == u[0] && Float.isInfinite(l[0]))
		if (l[0] > zero)
			l[0] = +Float.MAX_VALUE;
		else
			u[0] = -Float.MAX_VALUE;
	if (Float.isNaN(l[0]) || Float.isNaN(l[0]) || l[0] > u[0])
	{
		retval = fpInValiDival;
		l[0] = Float.NEGATIVE_INFINITY;
		u[0] = Float.POSITIVE_INFINITY;
	}
	return(retval);
    }	/* stringTointervalf(String, float[], float[]) */

    public static int bufTointervalf(char[] x, float[] l, float[] u)
    {
	Intervalio	re;
	Intervalio	im;
	char[]		buf;
	int		n, retval;

	retval	= fpValiDival;
	buf	= new char[dslength];
	flushbuffer(buf, dslength);
	n = strlen(x);
	if (n > dslength)
	{
		retval = fpUnassociatedInputVariable;
		l[0] = -Float.NaN;
		u[0] = +Float.NaN;
		return(retval);
	}
	strcpy(buf, x, n);
	if (n <= 0)
	{
		retval = fpUnassociatedInputVariable;
		l[0] = -Float.NaN;
		u[0] = +Float.NaN;
		return(retval);
	}
	re = new Intervalio();
	im = new Intervalio();
	retval = decimalival(re, im, buf);
	if (retval != fpValiDival)
	{
		l[0] = -Float.NaN;
		u[0] = +Float.NaN;
		return(retval);
	}
	if ((re.fpclass == fpNaN) && (im.fpclass == fpNaN) && (re.ds[0] == letter))
	{
		l[0] = -Float.NaN;
		u[0] = +Float.NaN;
		return(retval);
	}
	if (re.fpclass == fpZero)
		l[0] = re.sign ? -zerof : zerof;
	else
		l[0] = decimalTofloat(re, fpNegative);
	if (im.fpclass == fpZero)
		u[0] = im.sign ? -zerof : zerof;
	else
		u[0] = decimalTofloat(im, fpPositive);
	if (l[0] == u[0] && Float.isInfinite(l[0]))
		if (l[0] > zero)
			l[0] = +Float.MAX_VALUE;
		else
			u[0] = -Float.MAX_VALUE;
	if (Float.isNaN(l[0]) || Float.isNaN(l[0]) || l[0] > u[0])
	{
		retval = fpInValiDival;
		l[0] = Float.NEGATIVE_INFINITY;
		u[0] = Float.POSITIVE_INFINITY;
	}
	return(retval);
    }	/* bufTointervalf(char[], float[], float[]) */

    private static void fillByasterisk(char[] buf, Intervalio bf, int w)
    {
	int i;
	for (i = 0; i < w; ++i)
		buf[bf.index++] = '*';
    }	/* fillByasterisk() */

    private static void wrtEmpty(char[] buf, Intervalio bf, int w)
    {
	int i;
	buf[bf.index++] = '[';
	buf[bf.index++] = 'E';
	buf[bf.index++] = 'M';
	buf[bf.index++] = 'P';
	buf[bf.index++] = 'T';
	buf[bf.index++] = 'Y';
	for (i = 6; i < (w-1); ++i)
		buf[bf.index++] = ' ';
	buf[bf.index++] = ']';

    }	/* wrtEmpty() */

    private static void wrtinfinity
	(char[] buf, Intervalio bf, int w, boolean sign)
    {
	int i;
	int n;
	int signwidth;
	int width;

	if (sign)
		signwidth = 1;
	else
		signwidth = 0;
	if (w == 0)
	{
		width = signwidth + 3;
		if (sign)
			buf[bf.index++] = '-';
		buf[bf.index++] = 'I';
		buf[bf.index++] = 'n';
		buf[bf.index++] = 'f';
	}
	else
	{
		if (signwidth + 3 > w)
		{
			for (i = 0; i < w; ++i)
				buf[bf.index++] = '*';
		}
		else if (signwidth + 8 > w)
		{
			n = w - (signwidth + 3);
			for (i = 0; i < n; ++i)
				buf[bf.index++] = ' ';
			if (sign)
				buf[bf.index++] = '-';
			buf[bf.index++] = 'I';
			buf[bf.index++] = 'n';
			buf[bf.index++] = 'f';
		}
		else
		{
			n = w - (signwidth + 8);
			for (i = 0; i < n; ++i)
				buf[bf.index++] = ' ';
			if (sign)
				buf[bf.index++] = '-';
			buf[bf.index++] = 'I';
			buf[bf.index++] = 'n';
			buf[bf.index++] = 'f';
			buf[bf.index++] = 'i';
			buf[bf.index++] = 'n';
			buf[bf.index++] = 'i';
			buf[bf.index++] = 't';
			buf[bf.index++] = 'y';
		}
	}
    }

    private static void wrtnan
	(char[] buf, Intervalio bf, int w, boolean sign)
    {
	int i;
	int n;
	int signwidth;
	int width;

	if (sign)
		signwidth = 1;
	else
		signwidth = 0;
	if (w == 0)
	{
		width = signwidth + 3;
		if (sign)
			buf[bf.index++] = '-';
		buf[bf.index++] = 'N';
		buf[bf.index++] = 'a';
		buf[bf.index++] = 'N';
	}
	else
	{
		if (signwidth + 3 > w)
		{
			for (i = 0; i < w; ++i)
				buf[bf.index++] = '*';
		}
		else
		{
			n = w - (signwidth + 3);
			for (i = 0; i < n; ++i)
				buf[bf.index++] = ' ';
			if (sign)
				buf[bf.index++] = '-';
			buf[bf.index++] = 'N';
			buf[bf.index++] = 'a';
			buf[bf.index++] = 'N';
		}
	}
    }

    private static int wrtEzero
	(char[] buf, Intervalio bf, int w, int d, int e,
		char expletter, boolean sign)
    {
	int i;
	int k;
	int n;
	int signwidth;
	int width;
	int retval;

	retval = fpValiDival;

	if (sign)
		signwidth = 1;
	else
		signwidth = 0;
	k = 0;
	if (k <= 0 && -d < k)
	{
		width = signwidth + d + e + 3;
		if (w < width)
		{
			for (i = 0; i < w; ++i)
				buf[bf.index++] = '*';
		}
		else
		{
			n = w - width;
			if (w > width)
				--n;
			for (i = 0; i < n; ++i)
				buf[bf.index++] = ' ';
			if (signwidth != 0)
				buf[bf.index++] = '-';
			if (w > width)
				buf[bf.index++] = '0';
			buf[bf.index++] = '.';
			for (i = 0; i < d; ++i)
				buf[bf.index++] = '0';
			buf[bf.index++] = expletter;
			buf[bf.index++] = '+';
			for (i = 0; i < e; ++i)
				buf[bf.index++] = '0';
		}
	}
	else
		if (k > 0 && k < d + 2)
		{
			width = signwidth + d + e + 4;
			if (w < width)
			{
				for (i = 0; i < w; ++i)
					buf[bf.index++] = '*';
			}
			else
			{
				n = w - width;
				for (i = 0; i < n; ++i)
					buf[bf.index++] = ' ';
				if (signwidth != 0)
					buf[bf.index++] = '+';
				for (i = 0; i < k; ++i)
					buf[bf.index++] = '0';
				buf[bf.index++] = '.';
				n = d - k + 1;
				for (i = 0; i < n; ++i)
					buf[bf.index++] = '0';
				buf[bf.index++] = expletter;
				buf[bf.index++] = '+';
				for (i = 0; i < e; ++i)
					buf[bf.index++] = '0';
			}
		}
		else
			retval = fpBaDScaleFactor;
	return(retval);
    }

    private static int wrtEnormalival
	(char[] buf, Intervalio bf, int w, int d, int e,
		char expletter, boolean sign, int decpt, char[] s)
    {
	char	buffexp[];
	int	exponent;
	int	i;
	int	j;
	int	k;
	int	n;
	boolean	nexp;
	int	quo;
	int	signwidth;
	int	width;
	boolean	widthexpletter;
	int	idx;
	int	retval;

	retval		= fpValiDival;
	widthexpletter	= true;
	buffexp		= new char[32];
	flushbuffer(buffexp, 32);
	if (sign)
		signwidth = 1;
	else
		signwidth = 0;
	idx = 0;
	while (s[idx] == '0')
	{
		--decpt;
		idx++;
	}
	k		= 0;
	exponent	= decpt - k;
	nexp		= false;
	if (exponent < 0)
	{
		nexp		= true;
		exponent	= -exponent;
	}
	j = 0;
	while (exponent > 0)
	{
		quo		= exponent / 10;
		buffexp[j++]	= intTochar(exponent - 10 * quo);
		exponent	= quo;
	}
	if (j == (e+1))
		widthexpletter = false;
	else
		if (j > e)
		{
			while (--w >= 0)
				buf[bf.index++] = '*';
			return(retval);
		}
	if (k <= 0)
	{
		width = signwidth + d + e + 3;
		if (width > w)
		{
			while (--w >= 0)
				buf[bf.index++] = '*';
			return(retval);
		}
		n = w - width;
		if (n > 0) --n;
		for (i = 0; i < n; ++i)
			buf[bf.index++] = ' ';
		if (sign)
			buf[bf.index++] = '-';
		else if (signwidth != 0)
				buf[bf.index++] = '+';
		if (w > width)
			buf[bf.index++] = '0';
		buf[bf.index++] = '.';
		n = -k;
		for (i = 0; i < n; ++i)
			buf[bf.index++] = '0';
		n = d + k;
		for (i = 0; i < n && s[i] != (char)0; ++i)
			buf[bf.index++] = s[i];
		for (; i < n; ++i)
			buf[bf.index++] = '0';
	}
	else
	{
		width = signwidth + d + e + 4;
		if (width > w)
		{
			while (--w >= 0)
				buf[bf.index++] = '*';
			return(retval);
		}
		n = w - width;
		for (i = 0; i < n; ++i)
			buf[bf.index++] = ' ';
		if (sign)
			buf[bf.index++] = '-';
		else if (signwidth != 0)
			buf[bf.index++] = '+';
		for (i = 0; i < k && s[idx] != (char)0; ++i)
			buf[bf.index++] = s[idx++];
		for (; i < k; ++i)
			buf[bf.index++] = '0';
		buf[bf.index++] = '.';
		for (; i <= d && s[idx] != (char)0; ++i)
			buf[bf.index++] = s[idx++];
		for (; i <= d; ++i)
			buf[bf.index++] = '0';
	}
	if (widthexpletter)
		buf[bf.index++] = expletter;
	if (nexp)
		buf[bf.index++] = '-';
	else
		buf[bf.index++] = '+';
	while (--e >= j)
		buf[bf.index++] = '0';
	while (--j >= 0)
		buf[bf.index++] = buffexp[j];
	return(retval);
    }	/* wrtEnormalival() */

    private static void doubleTodecimal
	(Intervalio bf, double x, int ndigits, int rnd)
    {
	String	str;
	char[]	buf, bufn;
	char	sym;
	int	exp, idx;
	boolean	sign, isround;

	flushbuffer(bf.ds, dslength);
	bf.fpclass	= fpNormal;
	bf.exponent	= 0;
	bf.more		= false;
	bf.ndigits	= 0;
	exp		= 0;
	idx		= 0;
	sign		=
	bf.sign		= Arith.signbit(x);
	if (x == zero)
	{
		bf.ds[idx] = '0';
		bf.fpclass = fpZero;
		return;
	}
	if (Double.isNaN(x))
	{
		bf.ds[idx++] = 'N';
		bf.ds[idx++] = 'a';
		bf.ds[idx]   = 'N';
		bf.fpclass = fpNaN;
		return;
	}
	if (Double.isInfinite(x))
	{
		bf.ds[idx++] = 'i';
		bf.ds[idx++] = 'n';
		bf.ds[idx++] = 'f';
		bf.ds[idx++] = 'i';
		bf.ds[idx++] = 'n';
		bf.ds[idx++] = 'i';
		bf.ds[idx++] = 't';
		bf.ds[idx]   = 'y';
		bf.fpclass = fpInfinity;
		return;
	}
	isround	= false;
	buf	= new char[dslength];
	bufn	= new char[dslength];
	flushbuffer(buf, dslength);
	flushbuffer(bufn, dslength);
	if (isTiny(x))
		if (rnd == fpNegative)
		{
			if (x < zero)
				x = Arith.prev(x);
		}
		else
			if (rnd == fpPositive)
				if (x > zero)
					x = Arith.next(x);
	x = Arith.fabs(x);
	str = doubleTostring(x);
	stringToarray(str, buf);
	exp = getExponent(buf);
	exp = correctExponent(buf, exp);
	idx = normFraction(buf, bufn);
	flushbuffer(buf, dslength);
	strcpy(buf, bufn, dslength);
	flushbuffer(bufn, dslength);
	while (idx < ndigits)
	{
		buf[idx++] = '0';
		exp--;
	}
	if (idx > ndigits)
	{
		isround = true;
		exp += idx - ndigits;
	}
	else
		isround = ! isExact(x);
	strcpy(bf.ds, buf, dslength);
	bf.fpclass	= fpNormal;
	bf.exponent	= exp;
	bf.ndigits	= ndigits;
	if (isround)
		switch (rnd)
		{
			case fpNegative:
				if (! sign && idx > ndigits)
					break;
				minusone(bf);
				break;
			case fpPositive:
				if (sign && idx > ndigits)
					break;
				plusone(bf);
				break;
			case fpNearest:
				bf.sign = false;
				if (bf.ds[ndigits] >= '5')
					plusone(bf);
				bf.sign = sign;
				break;
			case fpToZero:
				if (idx > ndigits)
					break;
				bf.sign = false;
				minusone(bf);
				bf.sign = sign;
				break;
			case fpChop:
				break;
			default:
				break;
		}
	for (idx = ndigits; idx < dslength; idx++)
		bf.ds[idx] = (char)0;
    }

    private static void floatTodecimal
	(Intervalio bf, float x, int ndigits, int rnd)
    {
	String	str;
	char[]	buf, bufn;
	char	sym;
	int	exp, idx;
	boolean	sign, isround;

	flushbuffer(bf.ds, dslength);
	bf.fpclass	= fpNormal;
	bf.exponent	= 0;
	bf.more		= false;
	bf.ndigits	= 0;
	exp		= 0;
	idx		= 0;
	sign		=
	bf.sign		= Arith.signbitf(x);
	if (x == zerof)
	{
		bf.ds[idx] = '0';
		bf.fpclass = fpZero;
		return;
	}
	if (Float.isNaN(x))
	{
		bf.ds[idx++] = 'N';
		bf.ds[idx++] = 'a';
		bf.ds[idx]   = 'N';
		bf.fpclass = fpNaN;
		return;
	}
	if (Float.isInfinite(x))
	{
		bf.ds[idx++] = 'i';
		bf.ds[idx++] = 'n';
		bf.ds[idx++] = 'f';
		bf.ds[idx++] = 'i';
		bf.ds[idx++] = 'n';
		bf.ds[idx++] = 'i';
		bf.ds[idx++] = 't';
		bf.ds[idx]   = 'y';
		bf.fpclass = fpInfinity;
		return;
	}
	isround	= false;
	buf	= new char[dslength];
	bufn	= new char[dslength];
	flushbuffer(buf, dslength);
	flushbuffer(bufn, dslength);
	if (isTinyf(x))
		if (rnd == fpNegative)
		{
			if (x < zerof)
				x = Arith.prevf(x);
		}
		else
			if (rnd == fpPositive)
				if (x > zerof)
					x = Arith.nextf(x);
	x = Arith.fabsf(x);
	str = doubleTostring((double)x);
	stringToarray(str, buf);
	exp = getExponent(buf);
	exp = correctExponent(buf, exp);
	idx = normFraction(buf, bufn);
	flushbuffer(buf, dslength);
	strcpy(buf, bufn, dslength);
	flushbuffer(bufn, dslength);
	while (idx < ndigits)
	{
		buf[idx++] = '0';
		exp--;
	}
	if (idx > ndigits)
	{
		isround = true;
		exp += idx - ndigits;
	}
	else
		isround = ! isExactf(x);
	strcpy(bf.ds, buf, dslength);
	bf.fpclass	= fpNormal;
	bf.exponent	= exp;
	bf.ndigits	= ndigits;
	if (isround)
		switch (rnd)
		{
			case fpNegative:
				if (! sign && idx > ndigits)
					break;
				minusone(bf);
				break;
			case fpPositive:
				if (sign && idx > ndigits)
					break;
				plusone(bf);
				break;
			case fpNearest:
				bf.sign = false;
				if (bf.ds[ndigits] >= '5')
					plusone(bf);
				bf.sign = sign;
				break;
			case fpToZero:
				if (idx > ndigits)
					break;
				bf.sign = false;
				minusone(bf);
				bf.sign = sign;
				break;
			case fpChop:
				break;
			default:
				break;
		}
	for (idx = ndigits; idx < dslength; idx++)
		bf.ds[idx] = (char)0;
    }

    private static int wrtEivalnumber
	(char[] buf, Intervalio bf, int w, int d, int e,
		double x, int m)
    {
	int	k;
	int	ndigits;
	boolean	sign;
	int	retval;

	retval = fpValiDival;
	if (x == 0)
	{
		sign = Arith.signbit(x);
		retval = wrtEzero(buf, bf, w, d, e, letter, sign);
	}
	else
	{
		if (Double.isInfinite(x))
		{
			sign = Arith.signbit(x);
			wrtinfinity(buf, bf, w, sign);
			return(retval);
		}
		if (Double.isNaN(x))
		{
			sign = Arith.signbit(x);
			wrtnan(buf, bf, w, sign);
			return(retval);
		}
		k = 0;
		/* ORIGINAL k = p.scale_factor */
		if (k <= 0 && -d < k)
			ndigits = d + k;
		else
			if (k > 0 && k < d + 2)
				ndigits = d + 1;
			else
			{
				retval = fpBaDScaleFactor;
				return(retval);
			}
		if (ndigits > dslength - 1)
			ndigits = dslength - 1;
		doubleTodecimal(bf, x, ndigits, m);
		retval = wrtEnormalival
				(buf, bf, w, d, e, letter, bf.sign,
					bf.ndigits + bf.exponent, bf.ds);
	}
	return(retval);
    }	/* wrtEivalnumber() */

    private static int wrtEivalfnumberf
	(char[] buf, Intervalio bf, int w, int d, int e,
		float x, int m)
    {
	int	k;
	int	ndigits;
	boolean	sign;
	int	retval;

	retval = fpValiDival;
	if (x == 0)
	{
		sign = Arith.signbitf(x);
		retval = wrtEzero(buf, bf, w, d, e, letter, sign);
	}
	else
	{
		if (Float.isInfinite(x))
		{
			sign = Arith.signbitf(x);
			wrtinfinity(buf, bf, w, sign);
			return(retval);
		}
		if (Float.isNaN(x))
		{
			sign = Arith.signbitf(x);
			wrtnan(buf, bf, w, sign);
			return(retval);
		}
		k = 0;
		/* ORIGINAL k = p.scale_factor */
		if (k <= 0 && -d < k)
			ndigits = d + k;
		else
			if (k > 0 && k < d + 2)
				ndigits = d + 1;
			else
			{
				retval = fpBaDScaleFactor;
				return(retval);
			}
		if (ndigits > dslength - 1)
			ndigits = dslength - 1;
		floatTodecimal(bf, x, ndigits, m);
		retval = wrtEnormalival
				(buf, bf, w, d, e, letter, bf.sign,
					bf.ndigits + bf.exponent, bf.ds);
	}
	return(retval);
    }	/* wrtEivalfnumberf() */

    private static int wrtEival
	(char[] buf, Intervalio bf, int w, int d, int e,
		double l, double u)
    {
	int ww;
	int retval;

	retval = fpValiDival;
	if (w < 7)
	{
		fillByasterisk(buf, bf, w);
		return(retval);
	}
	if (isEmpty(l, u))
	{
		wrtEmpty(buf, bf, w);
		return(retval);
	}
	ww = (w - 3) >> 1;
	if ((w & 1) == 0)
		buf[bf.index++] = ' ';
	buf[bf.index++] = '[';
	retval = wrtEivalnumber(buf, bf, ww, d, e, l, fpNegative);
	if (retval != fpValiDival)
		return(retval);
	buf[bf.index++] = ',';
	retval = wrtEivalnumber(buf, bf, ww, d, e, u, fpPositive);
	if (retval != fpValiDival)
		return(retval);
	buf[bf.index++] = ']';
	buf[bf.index++] = (char)0;
	return(retval);
    }	/* wrtEival() */

    private static int wrtEivalf
	(char[] buf, Intervalio bf, int w, int d, int e,
		float l, float u)
    {
	int ww;
	int retval;

	retval = fpValiDival;
	if (w < 7)
	{
		fillByasterisk(buf, bf, w);
		return(retval);
	}
	if (isEmptyf(l, u))
	{
		wrtEmpty(buf, bf, w);
		return(retval);
	}
	ww = (w - 3) >> 1;
	if ((w & 1) == 0)
		buf[bf.index++] = ' ';
	buf[bf.index++] = '[';
	retval = wrtEivalfnumberf(buf, bf, ww, d, e, l, fpNegative);
	if (retval != fpValiDival)
		return(retval);
	buf[bf.index++] = ',';
	retval = wrtEivalfnumberf(buf, bf, ww, d, e, u, fpPositive);
	if (retval != fpValiDival)
		return(retval);
	buf[bf.index++] = ']';
	buf[bf.index++] = (char)0;
	return(retval);
    }	/* wrtEivalf() */

    public static String intervalTostring(double l, double u, int w, int d, int e, int format)
    {
	Intervalio	bf;
	char		buf[];
	String		str;
	int		retval;

	str = null;
	if (w < 0 || d < 0)
	{
		retval = fpBaDFormat;
		return(str);
	}
	if (w == 0)
	{
		if (format == 1)
		{
			w = 27;
			d = 5;
			e = 3;
		}
		else
		{
			w = 49;
			d = 16;
			e = 3;
		}
	}
	if (d <= 0)
	{
		retval = fpBaDFormat;
		return(str);
	}
	buf	= new char[dslength];
	flushbuffer(buf, dslength);
	bf	= new Intervalio();
	flushbuffer(buf, dslength);
	switch (format)
	{
		case 1:
			retval = wrtYival(buf, bf, w, d, e, l, u, true);
			break;
		case 3:
			retval = wrtEival(buf, bf, w, d, e, l, u);
			break;
		case 4:
			retval = wrtGival(buf, bf, w, d, e, l, u);
			break;
		default:
			retval = wrtGival(buf, bf, w, d, e, l, u);
			break;
	}
	str = arrayTostring(buf);
	return(str);
    }	/* intervalTostring(double l, double u,  ...) */

    public static String intervalfTostring(float l, float u, int w, int d, int e, int format)
    {
	Intervalio	bf;
	char		buf[];
	String		str;
	int		retval;

	str = null;
	if (w < 0 || d < 0)
	{
		retval = fpBaDFormat;
		return(str);
	}
	if (w == 0)
	{
		if (format == 1)
		{
			w = 25;
			d = 4;
			e = 3;
		}
		else
		{
			w = 33;
			d = 8;
			e = 3;
		}
	}
	if (d <= 0)
	{
		retval = fpBaDFormat;
		return(str);
	}
	buf	= new char[dslength];
	flushbuffer(buf, dslength);
	bf	= new Intervalio();
	flushbuffer(buf, dslength);
	switch (format)
	{
		case 1:
			retval = wrtYivalf(buf, bf, w, d, e, l, u, true);
			break;
		case 3:
			retval = wrtEivalf(buf, bf, w, d, e, l, u);
			break;
		case 4:
			retval = wrtGivalf(buf, bf, w, d, e, l, u);
			break;
		default:
			retval = wrtGivalf(buf, bf, w, d, e, l, u);
			break;
	}
	str = arrayTostring(buf);
	return(str);
    }	/* intervalffTostring(float l, float u,  ...) */

    /**************** Y-format ****************/

    private static boolean cmp_decimal_records
	(Intervalio dr, Intervalio dr2, Intervalio dr3)
    {
	Intervalio dr4;

	dr4 = new Intervalio();
	cpyivalio(dr4, dr);
	minusone(dr4);
	if (dr4.ndigits < dr2.ndigits)
	{
		if (dr2.ndigits > 1)
		{
			dr2.ndigits--;
			dr2.ds[dr2.ndigits] = (char)0;
			dr2.exponent++;
		}
		else
		{
			dr2.ds[0] = '1';
			dr2.ds[1] = (char)0;
			dr2.exponent++;
			cpyivalio(dr3, dr2);
			return(true);
		}
	}
	if (strcmp(dr4.ds, dr2.ds) || (dr4.fpclass == fpZero))
	{
		cpyivalio(dr3, dr);
		return(true);
	}
	cpyivalio(dr3, dr4);
	minusone(dr4);
	if (dr4.ndigits < dr2.ndigits)
	{
		if (dr2.ndigits > 1)
		{
			dr2.ndigits--;
			dr2.ds[dr2.ndigits] = (char)0;
			dr2.exponent++;
		}
		else
		{
			dr2.ds[0] = '1';
			dr2.ds[1] = (char)0;
			dr2.exponent++;
			cpyivalio(dr3, dr2);
			return(true);
		}
	}
	if (strcmp(dr4.ds, dr2.ds))
		return(true);
	cpyivalio(dr3, dr);
	if (dr3.ndigits > 1)
	{
		dr3.ndigits--;
		dr3.ds[dr3.ndigits] = (char)0;
		dr3.exponent++;
	}
	else
	{
		dr3.ds[0] = '1';
		dr3.exponent++;
	}
	return(false);
    }	/* cmp_decimal_records() */

    private static boolean test_degenerate_ival_string
	(double xx, char[] str, int idx)
    {
	double		x1, x2;
	int		exp, idxstr, dotposition;
	boolean		signexp;
	char		sym;
	Intervalio	dr;

	if (idx < 0)
		return(true);
	idxstr		= idx + 1;
	dr		= new Intervalio();
	dr.fpclass	= fpNormal;
	dr.sign		= false;
	dr.ndigits	= 0;
	dr.more		= false;
	exp		= 0;
	signexp		= false;
	if (str[idx] != '[')
		return true;  /* OK */
	while (str[idxstr] == ' ')
		idxstr++;
	if (str[idxstr] == '+')
		idxstr++;
	else
		if (str[idxstr] == '-')
		{
			idxstr++;
			dr.sign = true;
		}
	while ((sym = str[idxstr++]) != '.')
		dr.ds[dr.ndigits++] = sym;
	dotposition = dr.ndigits;
	while (isDigit(sym = str[idxstr++]))
		dr.ds[dr.ndigits++] = sym;
	dr.ds[dr.ndigits] = (char)0;
	if (isAlpha(sym))  /* E - exponent */
		sym = str[idxstr++];
	if (sym == '+')
		sym = str[idxstr++];
	else
		if (sym == '-')
		{
			sym = str[idxstr++];
			signexp = true;
		}
	while (isDigit(sym = str[idxstr++]))
		exp = exp * 10 + charToint(sym);
	dr.exponent = (signexp) ? -exp : exp + (dotposition - dr.ndigits);
	x1 = decimalTodouble(dr, fpNegative);
	x2 = decimalTodouble(dr, fpPositive);
	if (x1 <= xx && x2 >= xx)
		return true;	/* OK */
	return false;		/* NOT */
    }	/* test_degenerate_ival_string() */

    private static boolean test_degenerate_ivalf_string
	(float xx, char[] str, int idx)
    {
	float		x1, x2;
	int		exp, idxstr, dotposition;
	boolean		signexp;
	char		sym;
	Intervalio	dr;

	if (idx < 0)
		return(true);
	idxstr		= idx + 1;
	dr		= new Intervalio();
	dr.fpclass	= fpNormal;
	dr.sign		= false;
	dr.ndigits	= 0;
	dr.more		= false;
	exp		= 0;
	signexp		= false;
	if (str[idx] != '[')
		return true;  /* OK */
	while (str[idxstr] == ' ')
		idxstr++;
	if (str[idxstr] == '+')
		idxstr++;
	else
		if (str[idxstr] == '-')
		{
			idxstr++;
			dr.sign = true;
		}
	while ((sym = str[idxstr++]) != '.')
		dr.ds[dr.ndigits++] = sym;
	dotposition = dr.ndigits;
	while (isDigit(sym = str[idxstr++]))
		dr.ds[dr.ndigits++] = sym;
	dr.ds[dr.ndigits] = (char)0;
	if (isAlpha(sym))  /* E - exponent */
		sym = str[idxstr++];
	if (sym == '+')
		sym = str[idxstr++];
	else
		if (sym == '-')
		{
			sym = str[idxstr++];
			signexp = true;
		}
	while (isDigit(sym = str[idxstr++]))
		exp = exp * 10 + charToint(sym);
	dr.exponent = (signexp) ? -exp : exp + (dotposition - dr.ndigits);
	x1 = decimalTofloat(dr, fpNegative);
	x2 = decimalTofloat(dr, fpPositive);
	if (x1 <= xx && x2 >= xx)
		return true;	/* OK */
	return false;		/* NOT */
    }	/* test_degenerate_ivalf_string() */

    private static int wrtYFstring
	(char[] buf, Intervalio bf, char[] cp, int decpt,
		int w, int e, int d,
			int sign, boolean flagYE)
    {
	int	len;
	int	lenexp;
	int	lenfwi, lenfwf, lenfai, lenfaf;
	int	lenewf, leneaf;
	int	exp, i, dpp;
	int	intzero;
	char	bufexp[];
	int	sp;
	int	idx;
	boolean	signexp;
	int	retval;

	retval	= fpValiDival;
	len	= strlen(cp);
	bufexp	= new char[16];
	flushbuffer(bufexp, 16);
	sp	= bf.index;
	idx	= 0;
	for (i=0; i<w; i++)
		buf[sp++] = ' ';
	/* Decimal point position, counting from the right IAS p.69 */
	dpp = e + d + 4;
	/*  Here:	w >= d + e + 7  (or w >= d + 10)
	e - set always in this procedure (default value 3)
	Therefore  w - dpp >= 3 - "[xx."
	Therefore at least two position before decimal point.
	Therefore if  w == d + e + 7  and  sign,
	then "-0." can be output before decimal point
	*/
	lenfwf = e + d + 2;	/* = dpp - 2 */	/* avalable width for fraction part */
	lenfwi = w - dpp - 1;			/* avalable width for integer part */
	lenfaf = len - decpt;			/* actual width for fraction part */
	lenfai = (decpt > 0 ? decpt : 0) + sign;/* actual width for integer part */
	if ((decpt > len) || (lenfai > lenfwi))
		/* Leading zeros or Trailing zeros or Not enouph place
			to locate decimal point in position  p = e + d + 4 */
		flagYE = true;
	lenewf = d;		/* avalable width for fraction part :(e+d+4)-(e+2)-2 */
	leneaf = len;	/* actual width for fraction part */
	if ((decpt < 0) && (lenfaf > lenfwf) && ((lenfwf + decpt) < Math.min(lenewf, leneaf)))
		/* Leading zeros. (lenfaf - lenfwf) digits will be cut.
			(len - (lenfaf - lenfwf)) = lenfwf + decpt digits remain. */
		flagYE = true;
	if (! flagYE)  /* F-format */
	{
		intzero = ((decpt <= 0) && (lenfai < lenfwi)) ? 1 : 0;
		sp = bf.index + w - dpp - lenfai - intzero;
		if (sign == 1)
			buf[sp++] = '-';
		if (intzero == 1)
			buf[sp++] = '0';
		for (i = 0; i < lenfai - sign; i++)
			buf[sp++] = cp[idx++];
		buf[sp++] = '.';
		if (decpt < 0)
			for (i = decpt; i < 0; i++)
				buf[sp++] = '0';
		for (i = 0; i < Math.min(decpt < 0 ? len : lenfaf, lenfwf); i++)
			buf[sp++] = cp[idx++];
	}
	else	/* E-format */
	{
		sp = bf.index + w - dpp - sign - 1;
		if (sign == 1)
			buf[sp++] = '-';
		buf[sp++] = '0';
		buf[sp++] = '.';
		if ((exp = decpt) >= 0)
			signexp	= false;
		else
		{
			signexp	= true;
			exp	= -exp;
		}
		for (i = 0; i < Math.min(lenewf, leneaf); i++)
			buf[sp++] = cp[idx++];
		recordExponent(bufexp, 0, exp);
		if ((lenexp = strlen(bufexp) - 2) > (e + 1))
		{
			  fillByasterisk(buf, bf, w);
				return(retval);
		}
		sp = bf.index + w - (e + 3);
		if (lenexp < e + 1)
			buf[sp++] = letter;
		buf[sp++] = signexp ? '-' : '+';
		for (i = 0; i < e - lenexp; i++)
			buf[sp++] = '0';
		idx = 2;
		while (lenexp-- > 0)
			buf[sp++] = bufexp[idx++];
	}
	bf.index += w;
	return(retval);
    }	/* wrtYFstring() */


    private static int wrtYDstring
	(char[] buf, Intervalio bf, char[] cp, int decpt,
		int w, int e, int d,
			int sign, boolean flagYE)
    {
	int	len;
	int	lenexp;
	int	lenfai, lenfaf;
	int	exp, i, d2;
	int	intzero;
	char	bufexp[];
	int	sp;
	int	idx;
	boolean	signexp;
	int	savess;
	int	retval;

	retval	= fpValiDival;
	len	= strlen(cp);
	bufexp	= new char[16];
	flushbuffer(bufexp, 16);
	sp	= savess = bf.index;
	idx	= 0;
	buf[sp++] = '[';
	for (i=2; i<w; i++)
		buf[sp++] = ' ';
	if (! flagYE)	/* F-format */
	{
		lenfaf = len - decpt - 1;	/* actual width for fraction part */
		lenfai = (decpt > 0 ? decpt : 0) + sign;/* actual width for integer part */
		d2 = d + e + 2;
		intzero = (decpt <= 0) ? 1 : 0;
		if (((lenfai + 1 + d2 + intzero) > (w - 2)) || (lenfaf > d2))
		{
			/*
			from go to
			*/
			if (((4 + d + e + sign) > (w - 2)) || (len > d))
			{
				retval = wrtYFstring
						(buf, bf, cp, decpt, w, e, d,
							sign, flagYE);
				return(retval);
			}
			sp = savess + w - 5 - d - e - sign;
			if (sign == 1)
				buf[sp++] = '-';
			buf[sp++] = '0';
			buf[sp++] = '.';
			if ((exp = decpt) >= 0)
				signexp	= false;
			else
			{
				signexp	= true;
				exp	= -exp;
			}
			for (i = 0; i < Math.min(d, len); i++)
				buf[sp++] = cp[idx++];
			for (; i < d; i++)
				buf[sp++] = '0';
			recordExponent(bufexp, 0, exp);
			if ((lenexp = strlen(bufexp) - 2) > (e + 1))
			{
				retval = wrtYFstring
						(buf, bf, cp, decpt, w, e, d,
							sign, flagYE);
				return(retval);
			}
			sp = savess + w - (e + 3);
			if (lenexp < e + 1)
				buf[sp++] = letter;
			buf[sp++] = signexp ? '-' : '+';
			for (i = 0; i < e - lenexp; i++)
				buf[sp++] = '0';
			idx = 2;
			while (lenexp-- > 0)
				buf[sp++] = bufexp[idx++];
			bf.index += w - 1;
			buf[bf.index++] = ']';
			return(retval);
			/*
			end from go to
			*/
		}
		sp = savess + w - 2 - lenfai - d2 - intzero;
		if (sign == 1)
			buf[sp++] = '-';
		if (intzero == 1)
			buf[sp++] = '0';
		for (i = 0; i < Math.min(lenfai - sign, len); i++)
			buf[sp++] = cp[idx++];
		if (decpt > 0)
			for (i = 0; i < (decpt - len); i++)
				buf[sp++] = '0';
		buf[sp++] = '.';
		if (decpt < 0)
			for (i = 0; i < Math.min(-decpt, d2); i++)
				buf[sp++] = '0';
		else
			i = 0;
		for (; i < d2; i++)
		{
			if (cp[idx] == (char)0)
				break;
			buf[sp++] = cp[idx++];
		}
		for (; i < d2; i++)
			buf[sp++] = '0';
	}
	else	/* E-format */
	{
		if (((4 + d + e + sign) > (w - 2)) || (len > d))
		{
			retval = wrtYFstring
					(buf, bf, cp, decpt, w, e, d,
						sign, flagYE);
			return(retval);
		}
		sp = savess + w - 5 - d - e - sign;
		if (sign == 1)
			buf[sp++] = '-';
		buf[sp++] = '0';
		buf[sp++] = '.';
		if ((exp = decpt) >= 0)
			signexp	= false;
		else
		{
			signexp	= true;
			exp	= -exp;
		}
		for (i = 0; i < Math.min(d, len); i++)
			buf[sp++] = cp[idx++];
		for (; i < d; i++)
			buf[sp++] = '0';
		recordExponent(bufexp, 0, exp);
		if ((lenexp = strlen(bufexp) - 2) > (e + 1))
		{
			retval = wrtYFstring
					(buf, bf, cp, decpt, w, e, d,
						sign, flagYE);
			return(retval);
		}
		sp = savess + w - (e + 3);
		if (lenexp < e + 1)
			buf[sp++] = letter;
		buf[sp++] = signexp ? '-' : '+';
		for (i = 0; i < e - lenexp; i++)
			buf[sp++] = '0';
		idx = 2;
		while (lenexp-- > 0)
			buf[sp++] = bufexp[idx++];
	}
	bf.index += w - 1;
	buf[bf.index++] = ']';
	return(retval);
    }	/* wrtYDstring() */

    private static int tryYGival
	(char[] buf, Intervalio bf,
		int w, int d, int e, double xx, boolean flagE)
    {
	Intervalio	dr;
	int		i, len, step;
	int		m;
	int		savess;
	int		ndigits;
	int		retval;

	retval	= fpValiDival;
	step	= 0;
	savess	= bf.index;
	/*
	m	= fpNearest;
	*/
	m	= fpChop;
	dr	= new Intervalio();
	for (;;)
	{
		ndigits = maxAccuracy + step;
		doubleTodecimal(dr, xx, ndigits, m);
		if (dr.fpclass == fpNaN)
		{
			buf[bf.index++] = '[';
			wrtnan(buf, bf, w-2, dr.sign);
			buf[bf.index++] = ']';
			break;
		}
		else	if (dr.fpclass == fpInfinity)
			{
				buf[bf.index++] = '[';
				wrtinfinity(buf, bf, w-2, dr.sign);
				buf[bf.index++] = ']';
				break;
			}
		else	if (dr.fpclass == fpZero)
			{
				buf[bf.index++] = '[';
				retval = wrtEzero(buf, bf, w-2, d, e, letter, dr.sign);
				buf[bf.index++] = ']';
				break;
			}
		else
		{
			for (i = dr.ndigits - 1; i > 0; i--)
			{
				if (dr.ds[i] != '0')
					break;
				else
				{
					dr.ndigits--;
					dr.exponent++;
				}
				dr.ds[dr.ndigits] = (char)0;
			}
			retval = wrtYDstring
					(buf, bf, dr.ds, dr.ndigits+dr.exponent, 
						w, e, d, dr.sign?1:0, flagE);
			if (retval != fpValiDival)
			{
				retval = fpValiDival;
				return(retval);
			}
			if (! test_degenerate_ival_string(xx, buf, savess - w))
			{
				if (step++ == 0)
					continue;
				else
				{
					buf[savess - w] = ' ';
					buf[savess - 1] = ' ';
				}
			}
			break;
		}
	}
	retval = fpValiDival;
	return(retval);
    }	/* tryYGival() */

    private static int tryYGivalf
	(char[] buf, Intervalio bf,
		int w, int d, int e, float xx, boolean flagE)
    {
	Intervalio	dr;
	int		i, len, step;
	int		m;
	int		savess;
	int		ndigits;
	int		retval;

	retval	= fpValiDival;
	step	= 0;
	savess	= bf.index;
	/*
	m	= fpNearest;
	*/
	m	= fpChop;
	dr	= new Intervalio();
	for (;;)
	{
		ndigits = maxAccuracyf + step;
		floatTodecimal(dr, xx, ndigits, m);
		if (dr.fpclass == fpNaN)
		{
			buf[bf.index++] = '[';
			wrtnan(buf, bf, w-2, dr.sign);
			buf[bf.index++] = ']';
			break;
		}
		else	if (dr.fpclass == fpInfinity)
			{
				buf[bf.index++] = '[';
				wrtinfinity(buf, bf, w-2, dr.sign);
				buf[bf.index++] = ']';
				break;
			}
		else	if (dr.fpclass == fpZero)
			{
				buf[bf.index++] = '[';
				retval = wrtEzero(buf, bf, w-2, d, e, letter, dr.sign);
				buf[bf.index++] = ']';
				break;
			}
		else
		{
			for (i = dr.ndigits - 1; i > 0; i--)
			{
				if (dr.ds[i] != '0')
					break;
				else
				{
					dr.ndigits--;
					dr.exponent++;
				}
				dr.ds[dr.ndigits] = (char)0;
			}
			retval = wrtYDstring
					(buf, bf, dr.ds, dr.ndigits+dr.exponent, 
						w, e, d, dr.sign?1:0, flagE);
			if (retval != fpValiDival)
			{
				retval = fpValiDival;
				return(retval);
			}
			if (! test_degenerate_ivalf_string(xx, buf, savess-w))
			{
				if (step++ == 0)
					continue;
				else
				{
					buf[savess - w] = ' ';
					buf[savess - 1] = ' ';
				}
			}
			break;
		}
	}
	retval = fpValiDival;
	return(retval);
    }	/* tryYGivalf() */

    private static int tryYGivaltwo
	(char[] buf, Intervalio bf,
		int w, int d, int e,
			double l, double u, boolean[] flagasterisk)
    {
	int	i;
	boolean	fasterisk;
	int	sp;
	int	retval;

	retval		= fpValiDival;
	fasterisk	= false;
	sp		= bf.index;

	if (d < 1)
		d = 1;
	for (; d > 0; d--)
	{
		wrtGival(buf, bf, w, d, e, l, u);
		fasterisk = false;
		sp = bf.index - w;
		for (i = 0; i < w; i++)
		{
			if (buf[sp++] == '*')
			{
				if (! flagasterisk[0] || (d > 1))
					bf.index -= w;
				fasterisk = true;
				break;
			}
		}
		if (! fasterisk)
			break;
	}
	flagasterisk[0] = fasterisk;
	return(retval);
    }	/* tryYGivaltwo() */

    private static int tryYGivalftwo
	(char[] buf, Intervalio bf,
		int w, int d, int e,
			float l, float u, boolean[] flagasterisk)
    {
	int	i;
	boolean	fasterisk;
	int	sp;
	int	retval;

	retval		= fpValiDival;
	fasterisk	= false;
	sp		= bf.index;

	if (d < 1)
		d = 1;
	for (; d > 0; d--)
	{
		wrtGivalf(buf, bf, w, d, e, l, u);
		fasterisk = false;
		sp = bf.index - w;
		for (i = 0; i < w; i++)
		{
			if (buf[sp++] == '*')
			{
				if (! flagasterisk[0] || (d > 1))
					bf.index -= w;
				fasterisk = true;
				break;
			}
		}
		if (! fasterisk)
			break;
	}
	flagasterisk[0] = fasterisk;
	return(retval);
    }	/* tryYGivalftwo() */

    private static int wrtGzero
	(char[] buf, Intervalio bf, int w, int d, int e, boolean sign)
    {
	int n;
	int signwidth;
	int retval;

	retval = fpValiDival;
	if (sign)
		signwidth = 1;
	else
		signwidth = 0;
	if (d == 0)
	{
		n = signwidth + e + 5;
		if (n > w)
		{
			while (--w >= 0)
				buf[bf.index++] = '*';
		}
		else
		{
			while (--w >= n)
				buf[bf.index++] = ' ';
			if (signwidth == 1)
				buf[bf.index++] = '-';
			buf[0] = '0';
			buf[1] = '.';
			buf[2] = '0';
			buf[3] = letter;
			buf[4] = '+';
			bf.index += 5;
			while (--e >= 0)
				buf[bf.index++] = '0';
		}
	}
	else	if (d == 1)
		{
			e += 2;
			n = signwidth + e + 2;
			if (n > w)
			{
				while (--w >= 0)
					buf[bf.index++] = '*';
			}
			while (--w >= n)
				buf[bf.index++] = ' ';
			if (signwidth == 1)
				buf[bf.index++] = '-';
			buf[bf.index++] = '0';
			buf[bf.index++] = '.';
			while (--e >= 0)
				buf[bf.index++] = ' ';
		}
	else
	{
		e += 2;
		n = signwidth + d + e;
		if (n > w)
		{
			while (--w >= 0)
			buf[bf.index++] = '*';
		}
		else
		{
			if (w > n)
			{
				while (--w > n)
					buf[bf.index++] = ' ';
				if (signwidth == 1)
					buf[bf.index++] = '-';
				buf[bf.index++] = '0';
			}
			else
				if (signwidth == 1)
					buf[bf.index++] = '-';
			buf[bf.index++] = '.';
			while (--d > 0)
				buf[bf.index++] = '0';
			while (--e >= 0)
				buf[bf.index++] = ' ';
		}
	}
	return(retval);
    }

    private static int wrtGnegative
	(char[] buf, Intervalio bf, int w, int d, int e,
		boolean sign, int ndigits, int decpt, char[] s)
    {
	char	c;
	int	left;
	int	m;
	int	n;
	int	right;
	int	idx;
	int	idxend;
	int	signwidth;
	int	t;
	int	retval;

	retval = fpValiDival;
	if (sign)
		signwidth = 1;
	else
		signwidth = 0;
	c = s[0];
	if (! (c >= '0' && c <= '9'))
	{
		while (--w >= 0)
			buf[bf.index++] = '*';
		return(retval);
	}
	idx = 0;
	for (idxend = 0; s[idxend] != (char)0; ++idxend)
		continue;
	left = ndigits + decpt;
	if (left < 0 || left > d)
	{
		if (0 + d < idxend)
			s[d] = (char)0;
		retval = wrtEnormalival(
				buf, bf, w, d, e, letter, sign,
					ndigits + decpt, s);
	}
	else
	{
		if (left == d)
			right = 0;
		else
			right = d - left;
		left = ndigits + decpt;
		n = signwidth + left + right + e + 3;
		if (n > w)
		{
			while (--w >= 0)
				buf[bf.index++] = '*';
			return(retval);
		}
		if (left == 0)
		{
			if (w > n)
			{
				while (--w > n)
					buf[bf.index++] = ' ';
				if (sign)
					buf[bf.index++] = '-';
				else
					if (signwidth == 1)
						buf[bf.index++] = '+';
				buf[bf.index++] = '0';
			}
			else
			{
				while (--w >= n)
					buf[bf.index++] = ' ';
				if (sign)
					buf[bf.index++] = '-';
				else
					if (signwidth == 1)
						buf[bf.index++] = '+';
			}
			buf[bf.index++] = '.';
			while (s[idx] != (char)0 && --right >= 0)
				buf[bf.index++] = s[idx++];
			if (right > 0)
			{
				while (--right >= 0)
					buf[bf.index++] = '0';
			}
		}
		else
		{
			while (--w >= n)
				buf[bf.index++] = ' ';
			if (sign)
				buf[bf.index++] = '-';
			else
				if (signwidth == 1)
					buf[bf.index++] = '+';
			while (s[idx] != (char)0 && --left >= 0)
				buf[bf.index++] = s[idx++];
			if (left > 0)
			{
				while (--left >= 0)
					buf[bf.index++] = '0';
			}
			buf[bf.index++] = '.';
			while (s[idx] != (char)0 && --right >= 0)
				buf[bf.index++] = s[idx++];
			if (right > 0)
			{
				while (--right >= 0)
					buf[bf.index++] = '0';
			}
		}
		n = e + 2;
		while (--n >= 0)
			buf[bf.index++] = ' ';
	}
	return(retval);
    }	/* wrtGnegative() */

    private static int wrtGpositive
	(char[] buf, Intervalio bf, int w, int d, int e,
		boolean sign, int ndigits, int decpt, char[] s)
    {
	char	c;
	char[]	charone;
	int	left;
	int	m;
	int	n;
	int	nineidxend;
	int	right;
	int	roundout;
	boolean	roundup;
	int	idx;
	int	idxend;
	int	restzero;
	int	signwidth;
	int	t;
	int	retval;

	retval		= fpValiDival;
	roundout	= 0;
	roundup		= false;
	charone		= new char[2];
	charone[0]	= '1';
	charone[1]	= (char)0;
	if (sign)
		signwidth = 1;
	else
		signwidth = 0;
	c = s[0];
	if (! (c >= '0' && c <= '9'))
	{
		while (--w >= 0)
			buf[bf.index++] = '*';
		return(retval);
	}
	idx = 0;
	for (idxend = 0; s[idxend] != (char)0; ++idxend)
		continue;
	for (nineidxend = 0; s[nineidxend] == '9'; ++nineidxend)
		continue;
	if (0 + d < idxend)
	{
		for (restzero = 0 + d; restzero < idxend; restzero++)
		{
			if (s[restzero] != '0')
			{
				roundup = true;
				break;
			}
		}
		if (roundup)
		{
			if (nineidxend >= 0 + d)
				roundout = 1;
		}
	}
	left = ndigits + decpt + roundout;
	if (left < 0 || left > d)
	{
		if (roundout == 1)
				retval = wrtEnormalival
						(buf, bf, w, d, e, letter, sign,
							left, charone); 
		else
		{
			if (0 + d < idxend)
				s[d] = (char)0;
			if (roundup)
			{
				t = 0 + d;
				for (;;)
				{
					c = (char)((int)s[--t] + 1);
					if (c > '9')
						s[t] = '0';
					else
					{
						s[t] = c;
						break;
					}
				}
			}
			retval = wrtEnormalival
					(buf, bf, w, d, e, letter, sign,
						ndigits + decpt, s);
		}
	}
	else
	{
		if (left == d)
			right = 0;
		else
			right = d - left;
		roundup = false;
		left = ndigits + decpt;
		n = left + right;
		if (0 + n < idxend)
		{
			for (restzero = 0 + n; restzero < idxend; restzero++)
			{
				if (s[restzero] != '0')
				{
					roundup = true;
					break;
				}
			}
			if (roundup)
			{
				if (nineidxend >= 0 + n)
				{
					++left;
					s[0] = '1';
					s[1] = (char)0;
					idx = 0;
				}
				else
				{
					t = 0 + n;
					for (;;)
					{
						c = (char)((int)s[--t] + 1);
						if (c > '9')
							s[t] = '0';
						else
						{
							s[t] = c;
							break;
						}
					}
				}
			}
		}
		n = signwidth + left + right + e + 3;
		if (n > w)
		{
			while (--w >= 0)
			buf[bf.index++] = '*';
			return(retval);
		}
		if (left == 0)
		{
			if (w > n)
			{
				while (--w > n)
					buf[bf.index++] = ' ';
				if (sign)
					buf[bf.index++] = '-';
				else
					if (signwidth == 1)
						buf[bf.index++] = '+';
				buf[bf.index++] = '0';
			}
			else
			{
				while (--w >= n)
					buf[bf.index++] = ' ';
				if (sign)
					buf[bf.index++] = '-';
				else
					if (signwidth == 1)
						buf[bf.index++] = '+';
			}
			buf[bf.index++] = '.';
			while (s[idx] != (char)0 && --right >= 0)
				buf[bf.index++] = s[idx++];
			if (right > 0)
			{
				while (--right >= 0)
					buf[bf.index++] = '0';
			}
		}
		else
		{
			while (--w >= n)
				buf[bf.index++] = ' ';
			if (sign)
				buf[bf.index++] = '-';
			else
				if (signwidth == 1)
					buf[bf.index++] = '+';
			while (s[idx] != (char)0 && --left >= 0)
				buf[bf.index++] = s[idx++];
			if (left > 0)
			{
				while (--left >= 0)
					buf[bf.index++] = '0';
			}
			buf[bf.index++] = '.';
			while (s[idx] != (char)0 && --right >= 0)
				buf[bf.index++] = s[idx++];
			if (right > 0)
			{
				while (--right >= 0)
					buf[bf.index++] = '0';
			}
		}
		n = e + 2;
		while (--n >= 0)
			buf[bf.index++] = ' ';
	}
	return(retval);
    }	/* wrtGpositive() */

    private static int wrtGivalnumber
	(char[] buf, Intervalio bf, int w, int d, int e,
		double x, int m)
    {
	int	ndigits;
	boolean	sign;
	int	retval;

	retval = fpValiDival;
	if (w < 0 || d < 0)
		return(retval);
	if (w < 7)
	{
		fillByasterisk(buf, bf, w);
		return(retval);
	}
	if (x == 0)
	{
		sign = Arith.signbit(x);
		retval = wrtGzero(buf, bf, w, d, e, sign);
		return(retval);
	}
	if (Double.isInfinite(x))
	{
		sign = Arith.signbit(x);
		wrtinfinity(buf, bf, w, sign);
		return(retval);
	}
	if (Double.isNaN(x))
	{
		sign = Arith.signbit(x);
		wrtnan(buf, bf, w, sign);
		return(retval);
	}
	ndigits = d + 2;
	if (ndigits > dslength - 1)
		  ndigits = dslength - 1;
	doubleTodecimal(bf, x, ndigits, m);
	if (((m == fpNegative) && bf.sign) || ((m == fpPositive) && ! bf.sign))
		retval = wrtGpositive
				(buf, bf, w, d, e, bf.sign, bf.ndigits,
					bf.exponent, bf.ds);
	else
		retval = wrtGnegative
				(buf, bf, w, d, e, bf.sign, bf.ndigits,
					bf.exponent, bf.ds);
	return(retval);
    }	/* wrtGivalnumber() */

    private static int wrtGivalfnumberf
	(char[] buf, Intervalio bf, int w, int d, int e,
		float x, int m)
    {
	int	ndigits;
	boolean	sign;
	int	retval;

	retval = fpValiDival;
	if (w < 0 || d < 0)
		return(retval);
	if (w < 7)
	{
		fillByasterisk(buf, bf, w);
		return(retval);
	}
	if (x == 0)
	{
		sign = Arith.signbitf(x);
		retval = wrtGzero(buf, bf, w, d, e, sign);
		return(retval);
	}
	if (Float.isInfinite(x))
	{
		sign = Arith.signbitf(x);
		wrtinfinity(buf, bf, w, sign);
		return(retval);
	}
	if (Float.isNaN(x))
	{
		sign = Arith.signbitf(x);
		wrtnan(buf, bf, w, sign);
		return(retval);
	}
	ndigits = d + 2;
	if (ndigits > dslength - 1)
		  ndigits = dslength - 1;
	floatTodecimal(bf, x, ndigits, m);
	if (((m == fpNegative) && bf.sign) || ((m == fpPositive) && ! bf.sign))
		retval = wrtGpositive
				(buf, bf, w, d, e, bf.sign, bf.ndigits,
					bf.exponent, bf.ds);
	else
		retval = wrtGnegative
				(buf, bf, w, d, e, bf.sign, bf.ndigits,
					bf.exponent, bf.ds);
	return(retval);
    }	/* wrtGivalfnumberf() */

    private static int wrtGival
	(char[] buf, Intervalio bf, int w, int d, int e,
		double l, double u)
    {
	int ww;
	int retval;

	retval = fpValiDival;
	if (w < 0 || d < 0)
		return(retval);
	if (w < 7)
	{
		fillByasterisk(buf, bf, w);
		return(retval);
	}
	if (isEmpty(l, u))
	{
		wrtEmpty(buf, bf, w);
		return(retval);
	}
	ww = (w - 3) >> 1;
	if ((w & 1) == 0)
		buf[bf.index++] = ' ';
	buf[bf.index++] = '[';
	retval = wrtGivalnumber(buf, bf, ww, d, e, l, fpNegative);
	if (retval != fpValiDival)
		return(retval);
	buf[bf.index++] = ',';
	retval = wrtGivalnumber(buf, bf, ww, d, e, u, fpPositive);
	if (retval != fpValiDival)
		return(retval);
	buf[bf.index++] = ']';
	return(retval);
    }	/* wrtGival() */

    private static int wrtGivalf
	(char[] buf, Intervalio bf, int w, int d, int e,
		float l, float u)
    {
	int ww;
	int retval;

	retval = fpValiDival;
	if (w < 0 || d < 0)
		return(retval);
	if (w < 7)
	{
		fillByasterisk(buf, bf, w);
		return(retval);
	}
	if (isEmptyf(l, u))
	{
		wrtEmpty(buf, bf, w);
		return(retval);
	}
	ww = (w - 3) >> 1;
	if ((w & 1) == 0)
		buf[bf.index++] = ' ';
	buf[bf.index++] = '[';
	retval = wrtGivalfnumberf(buf, bf, ww, d, e, l, fpNegative);
	if (retval != fpValiDival)
		return(retval);
	buf[bf.index++] = ',';
	retval = wrtGivalfnumberf(buf, bf, ww, d, e, u, fpPositive);
	if (retval != fpValiDival)
		return(retval);
	buf[bf.index++] = ']';
	return(retval);
    }	/* wrtGivalf() */

    private static int wrtYival
	(char[] buf, Intervalio bf, int w, int d, int e,
		double xl, double xu, boolean flagE)
    {
	Intervalio	dr, dr2, dr3;
	double		o;
	int		i, ww;
	int		nl, nu, nm;
	int		ndigit, decpt;
	int		ndigits;
	boolean		signival;
	boolean[]	flagasterisk;
	char[]		s;
	char[]		charzero;
	char[]		charone;
	int		retval;

	retval		= fpValiDival;
	flagasterisk	= new boolean[1];
	charzero	= new char[2];
	charzero[0]	= '0';
	charzero[1]	= (char)0;
	charone		= new char[2];
	charone[0]	= '1';
	charone[1]	= (char)0;
	if (w < 0 || d < 0)
	{
		fillByasterisk(buf, bf, w);
		return(retval);
	}
	/* test width/length correlations */
	if (w < (d + e + 7) || (w < d + 10))
	{
				/* see: IAS */
		fillByasterisk(buf, bf, w);
		return(retval);
	}
	/* 1 - empty interval */
	if (isEmpty(xl, xu))
	{
		wrtEmpty(buf, bf, w);
		return(retval);
	}
	/* interval boundaries */
	/* 2.1 - Infinity interval	[-inf, +inf]  */
	/* 2.2 - Infinity interval	[-inf, -inf]  or  [+inf, +inf] */
	/* 2.3 - Infinity interval	[-inf, <finite>] or [<finite>, inf]  */
	if (
		Double.isInfinite(xl)	||
		Double.isInfinite(xu)	||
		Double.isNaN(xl)	||
		Double.isNaN(xu)
	) {
		flagasterisk[0] = true;
		tryYGivaltwo(buf, bf, w, d, e, xl, xu, flagasterisk);
		return(retval);
	}	/* 2 - Infinity or NaN interval */
	/* 2 - test bad interval */
	if (xl > xu)
	{
		flagasterisk[0] = true;
		retval = tryYGivaltwo(buf, bf, w, d, e, xl, xu, flagasterisk);
		return(retval);
	}	/* 2 - test bad interval */
	/* 3 - degenerate interval */
	if (isDegenerate(xl, xu))
	{
		retval = tryYGival(buf, bf, w, d, e, xu, flagE);
		return(retval);
	}	/* 3 - degenerate interval */
	/* 4 - interval endpoints differ in sign */
	if (xl < zero && xu > zero)
	{
		flagasterisk[0] = false;
		if ((retval = tryYGivaltwo
				(buf, bf, w, d, e, xl, xu, flagasterisk)) != 0)
			return(retval);
		if (! flagasterisk[0])
			return(retval);
		i		= ilog10(Math.max(-xl, xu));
		dr		= new Intervalio();
		dr.fpclass	= fpNormal;
		dr.sign		= false;
		dr.ndigits	= 1;
		dr.exponent	= i;
		dr.more		= false;
		dr.ds[0]	= '1';
		dr.ds[1]	= (char)0;
		/*
		To guarantee containment we must set fp rounding to zero !
		*/
		o = decimalTodouble(dr, fpToZero);
		if (o < Math.max(-xl, xu))
			i++;
		retval = wrtYFstring(buf, bf, charzero, i+1, w, e, d, 0, flagE);
		return(retval);
	}	/* 4 - interval endpoints differ in sign */
	/* 5 - right endpoint is zero */
	if (xl < zero && xu == zero)
	{
		flagasterisk[0] = false;
		if ((retval = tryYGivaltwo
				(buf, bf, w, d, e, xl, xu, flagasterisk)) != 0)
			return(retval);
		if (! flagasterisk[0])
			return(retval);
		i		= ilog10(-xl);
		dr		= new Intervalio();
		dr.fpclass	= fpNormal;
		dr.sign		= true;
		dr.ndigits	= 1;
		dr.exponent	= i;
		dr.more		= false;
		dr.ds[0]	= '2';
		dr.ds[1]	= (char)0;
		/*
		To guarantee containment we must set fp rounding to zero !
		*/
		o = decimalTodouble(dr, fpToZero);
		if (o > xl)
			i++;
		retval = wrtYFstring(buf, bf, charone, i+1, w, e, d, 1, flagE);
		return(retval);
	}	/* 5 - right endpoint is zero */
	/* 6 - left endpoint is zero */
	if (xl == zero && xu > zero)
	{
		flagasterisk[0] = false;
		if ((retval = tryYGivaltwo
				(buf, bf, w, d, e, xl, xu, flagasterisk)) != 0)
			return(retval);
		if (! flagasterisk[0])
			return(retval);
		i		= ilog10(xu);
		dr		= new Intervalio();
		dr.fpclass	= fpNormal;
		dr.sign		= false;
		dr.ndigits	= 1;
		dr.exponent	= i;
		dr.more		= false;
		dr.ds[0]	= '2';
		dr.ds[1]	= (char)0;
		/*
		To guarantee containment we must set fp rounding to zero !
		*/
		o = decimalTodouble(dr, fpToZero);
		if (o < xu)
			i++;
		retval = wrtYFstring(buf, bf, charone, i+1, w, e, d, 0, flagE);
		return(retval);
	}	/* 6 - left endpoint is zero */
	/*
		7 - normal case
		interval endpoints identical in sign
			&& finite && non_degenerate
	*/
	signival = (xl < zero);
	if (signival)
	{
		/* make positive interval */
		o = xl;
		xl = -xu;
		xu = -o;
	}
	nl = ilog10(xl) + 1;
	nu = ilog10(xu) + 1;
	/*
	More than one digits can be output
	in the case when nl == nu  [12345.567, 12347.758] or
	in the case when abs(nu - nl) == 1  [99.0, 101.0]
	But this comparison does not answer to qestion
	about ndidits.

	More convenient test is:
	logarithm of end points difference must be
	exactly less then logarithm of right endpoint.
	(Remember that both endpoints are positive !)
	In this case ndigit = logarithms difference,
	may be plus 1.

	See:

	[1234., 1237.] .  123.		(ndigit = 3)
	[1234., 1236.] .  1234.		(ndigit = 4)
	*/
	if (xu / xl >= ten)
		ndigit = 0;
	else
	if (nu - nl > 1)
		ndigit = 0;
	else
	if ((nm = ilog10(xu - xl) + 1) >= nu)
		ndigit = 0;
	else
		ndigit = Math.min(nu - nm, maxAccuracy /*- 1*/); 
	/*
		Restriction !
		Max number of reliable digits for double = 16
		(Below used (ndigit + 1))
	*/
	/*  It's known now that (ndigit) or (ndigit + 1) digits
	can be write. Try to find precise value.
	This is necessary, because:
	[123456., 123458.]  .  123457.	(ndigit = 6)
	[123456., 123459.]  .  12345.	(ndigit = 5)
	*/
	dr = new Intervalio();
	dr2 = new Intervalio();
	dr3 = new Intervalio();
	for (;;)
	{
	ndigits = ndigit + 1;
	doubleTodecimal(dr, xu, ndigits, fpPositive);
	ndigits = ndigit + ((nu == nl) ? 1 : 0);
	doubleTodecimal(dr2, xl, ndigits, fpNegative);
	if (! cmp_decimal_records(dr, dr2, dr3))
		if (--ndigit >= 0)
			continue;
		break;
	}
	decpt = dr3.ndigits + dr3.exponent;
	/* Write founded string */
	if (
		(strlen(dr3.ds) == 1) &&
		((dr3.ds[0] == '0') || (dr3.ds[0] == '1'))
	) {
		flagasterisk[0] = false;
		if (signival)
		{
			/* make source interval */
			o = xl;
			xl = -xu;
			xu = -o;
		}
		if ((retval = tryYGivaltwo
				(buf, bf, w, d, e, xl, xu, flagasterisk)) != 0)
			return(retval);
		if (! flagasterisk[0])
			return(retval);
	}
	retval = wrtYFstring
		(buf, bf, dr3.ds, decpt, w, e, d, signival ? 1 : 0, flagE);
	/* 7 - normal case. interval endpoints identical in sign */
	return(retval);
    }	/* wrtYival() */

    private static int wrtYivalf
	(char[] buf, Intervalio bf, int w, int d, int e,
		float xl, float xu, boolean flagE)
    {
	Intervalio	dr, dr2, dr3;
	float		o;
	int		i, ww;
	int		nl, nu, nm;
	int		ndigit, decpt;
	int		ndigits;
	boolean		signival;
	boolean[]	flagasterisk;
	char[]		s;
	char[]		charzero;
	char[]		charone;
	int		retval;

	retval = fpValiDival;
	flagasterisk = new boolean[1];
	charzero = new char[2];
	charzero[0] = '0';
	charzero[1] = (char)0;
	charone = new char[2];
	charone[0] = '1';
	charone[1] = (char)0;
	if (w < 0 || d < 0)
	{
		fillByasterisk(buf, bf, w);
		return(retval);
	}
	/* test width/length correlations */
	if (w < (d + e + 7) || (w < d + 10))
	{
				/* see: IAS */
		fillByasterisk(buf, bf, w);
		return(retval);
	}
	/* 1 - empty interval */
	if (isEmptyf(xl, xu))
	{
		wrtEmpty(buf, bf, w);
		return(retval);
	}
	/* interval boundaries */
	/* 2.1 - Infinity interval	[-inf, +inf]  */
	/* 2.2 - Infinity interval	[-inf, -inf]  or  [+inf, +inf] */
	/* 2.3 - Infinity interval	[-inf, <finite>] or [<finite>, inf]  */
	if (
		Float.isInfinite(xl)	||
		Float.isInfinite(xu)	||
		Float.isNaN(xl)		||
		Float.isNaN(xu)
	) {
		flagasterisk[0] = true;
		tryYGivalftwo(buf, bf, w, d, e, xl, xu, flagasterisk);
		return(retval);
	}	/* 2 - Infinity or NaN interval */
	/* 2 - test bad interval */
	if (xl > xu)
	{
		flagasterisk[0] = true;
		retval = tryYGivalftwo(buf, bf, w, d, e, xl, xu, flagasterisk);
		return(retval);
	}	/* 2 - test bad interval */
	/* 3 - degenerate interval */
	if (isDegeneratef(xl, xu))
	{
		retval = tryYGivalf(buf, bf, w, d, e, xu, flagE);
		return(retval);
	}	/* 3 - degenerate interval */
	/* 4 - interval endpoints differ in sign */
	if (xl < zerof && xu > zerof)
	{
		flagasterisk[0] = false;
		if ((retval = tryYGivalftwo
				(buf, bf, w, d, e, xl, xu, flagasterisk)) != 0)
			return(retval);
		if (! flagasterisk[0])
			return(retval);
		i		= ilog10f(Math.max(-xl, xu));
		dr		= new Intervalio();
		dr.fpclass	= fpNormal;
		dr.sign		= false;
		dr.ndigits	= 1;
		dr.exponent	= i;
		dr.more		= false;
		dr.ds[0]	= '1';
		dr.ds[1]	= (char)0;
		/*
		To guarantee containment we must set fp rounding to zero !
		*/
		o = decimalTofloat(dr, fpToZero);
		if (o < Math.max(-xl, xu))
			i++;
		retval = wrtYFstring(buf, bf, charzero, i+1, w, e, d, 0, flagE);
		return(retval);
	}	/* 4 - interval endpoints differ in sign */
	/* 5 - right endpoint is zero */
	if (xl < zerof && xu == zerof)
	{
		flagasterisk[0] = false;
		if ((retval = tryYGivalftwo
				(buf, bf, w, d, e, xl, xu, flagasterisk)) != 0)
			return(retval);
		if (! flagasterisk[0])
			return(retval);
		i		= ilog10f(-xl);
		dr		= new Intervalio();
		dr.fpclass	= fpNormal;
		dr.sign		= true;
		dr.ndigits	= 1;
		dr.exponent	= i;
		dr.more		= false;
		dr.ds[0]	= '2';
		dr.ds[1]	= (char)0;
		/*
		To guarantee containment we must set fp rounding to zero !
		*/
		o = decimalTofloat(dr, fpToZero);
		if (o > xl)
			i++;
		retval = wrtYFstring(buf, bf, charone, i+1, w, e, d, 1, flagE);
		return(retval);
	}	/* 5 - right endpoint is zero */
	/* 6 - left endpoint is zero */
	if (xl == zerof && xu > zerof)
	{
		flagasterisk[0] = false;
		if ((retval = tryYGivalftwo
				(buf, bf, w, d, e, xl, xu, flagasterisk)) != 0)
			return(retval);
		if (! flagasterisk[0])
			return(retval);
		i		= ilog10f(xu);
		dr		= new Intervalio();
		dr.fpclass	= fpNormal;
		dr.sign		= false;
		dr.ndigits	= 1;
		dr.exponent	= i;
		dr.more		= false;
		dr.ds[0]	= '2';
		dr.ds[1]	= (char)0;
		/*
		To guarantee containment we must set fp rounding to zero !
		*/
		o = decimalTofloat(dr, fpToZero);
		if (o < xu)
			i++;
		retval = wrtYFstring(buf, bf, charone, i+1, w, e, d, 0, flagE);
		return(retval);
	}	/* 6 - left endpoint is zero */
	/*
		7 - normal case
		interval endpoints identical in sign
			&& finite && non_degenerate
	*/
	signival = (xl < zerof);
	if (signival)
	{
		/* make positive interval */
		o = xl;
		xl = -xu;
		xu = -o;
	}
	nl = ilog10f(xl) + 1;
	nu = ilog10f(xu) + 1;
	/*
	More than one digits can be output
	in the case when nl == nu  [12345.567, 12347.758] or
	in the case when abs(nu - nl) == 1  [99.0, 101.0]
	But this comparison does not answer to qestion
	about ndidits.

	More convenient test is:
	logarithm of end points difference must be
	exactly less then logarithm of right endpoint.
	(Remember that both endpoints are positive !)
	In this case ndigit = logarithms difference,
	may be plus 1.

	See:

	[1234., 1237.] .  123.		(ndigit = 3)
	[1234., 1236.] .  1234.		(ndigit = 4)
	*/
	if (xu / xl >= tenf)
		ndigit = 0;
	else
	if (nu - nl > 1)
		ndigit = 0;
	else
	if ((nm = ilog10f(xu - xl) + 1) >= nu)
		ndigit = 0;
	else
		ndigit = Math.min(nu - nm, maxAccuracyf /*- 1*/); 
	/*
		Restriction !
		Max number of reliable digits for double = 7
		(Below used (ndigit + 1))
	*/
	/*  It's known now that (ndigit) or (ndigit + 1) digits
	can be write. Try to find precise value.
	This is necessary, because:
	[123456., 123458.]  .  123457.	(ndigit = 6)
	[123456., 123459.]  .  12345.	(ndigit = 5)
	*/
	dr = new Intervalio();
	dr2 = new Intervalio();
	dr3 = new Intervalio();
	for (;;)
	{
	ndigits = ndigit + 1;
	floatTodecimal(dr, xu, ndigits, fpPositive);
	ndigits = ndigit + ((nu == nl) ? 1 : 0);
	floatTodecimal(dr2, xl, ndigits, fpNegative);
	if (! cmp_decimal_records(dr, dr2, dr3))
		if (--ndigit >= 0)
			continue;
		break;
	}
	decpt = dr3.ndigits + dr3.exponent;
	/* Write founded string */
	if (
		(strlen(dr3.ds) == 1) &&
		((dr3.ds[0] == '0') || (dr3.ds[0] == '1'))
	) {
		flagasterisk[0] = false;
		if (signival)
		{
			/* make source interval */
			o = xl;
			xl = -xu;
			xu = -o;
		}
		if ((retval = tryYGivalftwo
				(buf, bf, w, d, e, xl, xu, flagasterisk)) != 0)
			return(retval);
		if (! flagasterisk[0])
			return(retval);
	}
	retval = wrtYFstring
		(buf, bf, dr3.ds, decpt, w, e, d, signival ? 1 : 0, flagE);
	/* 7 - normal case. interval endpoints identical in sign */
	return(retval);
    }	/* wrtYivalf() */

    public static int ndigits(double xl, double xu)
    {
	Intervalio	dr, dr2, dr3;
	double		o;
	int		nl, nu, nm;
	int		ndigit, ndigits;

	/* 1 - empty interval */
	if (isEmpty(xl, xu))
		return(0);
	/* interval boundaries */
	/* 2.1 - Infinity interval	[-inf, +inf]  */
	/* 2.2 - Infinity interval	[-inf, -inf]  or  [+inf, +inf] */
	/* 2.3 - Infinity interval	[-inf, <finite>] or [<finite>, inf]  */
	if (
		Double.isInfinite(xl)	||
		Double.isInfinite(xu)	||
		Double.isNaN(xl)	||
		Double.isNaN(xu)
	)
		return(0);
		/* 2 - Infinity or NaN interval */
	/* 2 - test bad interval */
	if (xl > xu)
		return(0);
	/* 3 - degenerate interval */
	if (isDegenerate(xl, xu))
		return(Integer.MAX_VALUE);
	/* the interval contains zero */
	/* 4 - interval endpoints differ in sign */
	/* 5 - right endpoint is zero */
	/* 6 - left endpoint is zero */
	if (xl <= zero && xu >= zero)
		return(1);
	/*
		7 - normal case
		interval endpoints identical in sign
			&& finite && non_degenerate
	*/
	if (xl < zero)
	{
		/* make positive interval */
		o = xl;
		xl = -xu;
		xu = -o;
	}
	if (xu / xl >= ten)
		return(1);
	nl = ilog10(xl) + 1;
	nu = ilog10(xu) + 1;
	if (nu - nl > 1)
		return(1);
	/*
	More than one digits can be output
	in the case when nl == nu  [12345.567, 12347.758] or
	in the case when abs(nu - nl) == 1  [99.0, 101.0]
	But this comparison does not answer to qestion
	about ndidits.

	More convenient test is:
	logarithm of end points difference must be
	exactly less then logarithm of right endpoint.
	(Remember that both endpoints are positive !)
	In this case ndigit = logarithms difference,
	may be plus 1.

	See:

	[1234., 1237.] .  123.		(ndigit = 3)
	[1234., 1236.] .  1234.		(ndigit = 4)
	*/
	if ((nm = ilog10(xu - xl) + 1) >= nu)
		ndigit = 0;
	else
		ndigit = Math.min(nu - nm, maxAccuracy /*- 1*/); 
	/*
		Restriction !
		Max number of reliable digits for double = 16
		(Below used (ndigit + 1))
	*/
	/*  It's known now that (ndigit) or (ndigit + 1) digits
	can be write. Try to find precise value.
	This is necessary, because:
	[123456., 123458.]  .  123457.	(ndigit = 6)
	[123456., 123459.]  .  12345.	(ndigit = 5)
	*/
	dr = new Intervalio();
	dr2 = new Intervalio();
	dr3 = new Intervalio();
	for (;;)
	{
	ndigits = ndigit + 1;
	doubleTodecimal(dr, xu, ndigits, fpPositive);
	ndigits = ndigit + ((nu == nl) ? 1 : 0);
	doubleTodecimal(dr2, xl, ndigits, fpNegative);
	if (! cmp_decimal_records(dr, dr2, dr3))
		if (--ndigit >= 0)
			continue;
		break;
	}
	if (dr3.ndigits > maxAccuracy)
		return(maxAccuracy);
	return(dr3.ndigits);
    }	/* ndigits() */

    public static int ndigitsf(float xl, float xu)
    {
	Intervalio	dr, dr2, dr3;
	float		o;
	int		nl, nu, nm;
	int		ndigit, ndigits;

	/* 1 - empty interval */
	if (isEmptyf(xl, xu))
		return(0);
	/* interval boundaries */
	/* 2.1 - Infinity interval	[-inf, +inf]  */
	/* 2.2 - Infinity interval	[-inf, -inf]  or  [+inf, +inf] */
	/* 2.3 - Infinity interval	[-inf, <finite>] or [<finite>, inf]  */
	if (
		Float.isInfinite(xl)	||
		Float.isInfinite(xu)	||
		Float.isNaN(xl)		||
		Float.isNaN(xu)
	)
		return(0);
		/* 2 - Infinity or NaN interval */
	/* 2 - test bad interval */
	if (xl > xu)
		return(0);
	/* 3 - degenerate interval */
	if (isDegeneratef(xl, xu))
		return(Integer.MAX_VALUE);
	/* the interval contains zero */
	/* 4 - interval endpoints differ in sign */
	/* 5 - right endpoint is zero */
	/* 6 - left endpoint is zero */
	if (xl <= zerof && xu >= zerof)
		return(1);
	/*
		7 - normal case
		interval endpoints identical in sign
			&& finite && non_degenerate
	*/
	if (xl < zerof)
	{
		/* make positive interval */
		o = xl;
		xl = -xu;
		xu = -o;
	}
	if (xu / xl >= tenf)
		return(1);
	nl = ilog10f(xl) + 1;
	nu = ilog10f(xu) + 1;
	if (nu - nl > 1)
		return(1);
	/*
	More than one digits can be output
	in the case when nl == nu  [12345.567, 12347.758] or
	in the case when abs(nu - nl) == 1  [99.0, 101.0]
	But this comparison does not answer to qestion
	about ndidits.

	More convenient test is:
	logarithm of end points difference must be
	exactly less then logarithm of right endpoint.
	(Remember that both endpoints are positive !)
	In this case ndigit = logarithms difference,
	may be plus 1.

	See:

	[1234., 1237.] .  123.		(ndigit = 3)
	[1234., 1236.] .  1234.		(ndigit = 4)
	*/
	if ((nm = ilog10f(xu - xl) + 1) >= nu)
		ndigit = 0;
	else
		ndigit = Math.min(nu - nm, maxAccuracyf /*- 1*/); 
	/*
		Restriction !
		Max number of reliable digits for double = 7
		(Below used (ndigit + 1))
	*/
	/*  It's known now that (ndigit) or (ndigit + 1) digits
	can be write. Try to find precise value.
	This is necessary, because:
	[123456., 123458.]  .  123457.	(ndigit = 6)
	[123456., 123459.]  .  12345.	(ndigit = 5)
	*/
	dr = new Intervalio();
	dr2 = new Intervalio();
	dr3 = new Intervalio();
	for (;;)
	{
	ndigits = ndigit + 1;
	floatTodecimal(dr, xu, ndigits, fpPositive);
	ndigits = ndigit + ((nu == nl) ? 1 : 0);
	floatTodecimal(dr2, xl, ndigits, fpNegative);
	if (! cmp_decimal_records(dr, dr2, dr3))
		if (--ndigit >= 0)
			continue;
		break;
	}
	if (dr3.ndigits > maxAccuracyf)
		return(maxAccuracyf);
	return(dr3.ndigits);
    }	/* ndigitsf() */
}
