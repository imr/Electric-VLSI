/**
 * #pragma ident "@(#)Numberio.java 1.1 (Sun Microsystems, Inc.) 02/12/06"
 */
/**
 *  Copyright © 2002 Sun Microsystems, Inc.
 *  Alexei Agafonov aga@nbsp.nsk.su
 *  All rights reserved.
 */
/**
	Java number input output class
*/
import java.io.*;

public class Numberio
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
    private final static float zerof	= 0.0f;

    private final static int fpZero		= 0;
    private final static int fpSubNormal	= 1;
    private final static int fpNormal		= 2;
    private final static int fpInfinity		= 3;
    private final static int fpNaN		= 4;

    private final static int dslength		= 512;

    private final static int fpValiD		= 0;
    private final static int fpBaDFormat	= -4;
    private final static int fpBaDScaleFactor	= -5;

    private final static char letter	= 'E';

    private final static int E_NUMBER_FORMAT	= 3;
    private final static int G_NUMBER_FORMAT	= 4;

    /* Constructor */

    private Numberio() {
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

    private static double fabs(double x) {
	long l;
	l = Double.doubleToLongBits(x) & 0x7fffffffffffffffl;
	return(Double.longBitsToDouble(l));
    }

    private static float fabsf(float x) {
	int i;
	i = Float.floatToIntBits(x) & 0x7fffffff;
	return(Float.intBitsToFloat(i));
    }

    private static boolean signbit(double x) {
	return(Double.doubleToLongBits(x) < 0l);
    }

    private static boolean signbitf(float x) {
	return(Float.floatToIntBits(x) < 0);
    }

    private static void flushbuffer(char buf[], int n) {
	while (--n >= 0)
		buf[n] = (char)0;
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

    private static void readin(char[] buf, InputStream in) throws IOException
    {
	int	m, n, o;
	char	s;
	boolean	noneblank;
	o		= dslength;
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
		if (s != ' ' && s != '\t' && s != '\n')
			noneblank = true;
		if (s == ' ' || s == '\t' || s == '\n')
		{
			if (! noneblank)
				continue;
			buf[m] = (char)0;
			break;
		}
		buf[m++] = s;
		if (s == (char)0)
			break;
		o--;
	}
    }

    private static void readinFromFile(char[] buf, FileInputStream in) throws IOException
    {
	int	m, n, o;
	char	s;
	boolean	noneblank;
	boolean	comment;
	o		= dslength;
	noneblank	= false;
	comment		= false;
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
		if (s == '#')
			comment = true;
		if (comment)
		{
			if (s == '\n')
				comment = false;
			continue;
		}
		if (s != ' ' && s != '\t' && s != '\n')
			noneblank = true;
		if (s == ' ' || s == '\t' || s == '\n')
		{
			if (! noneblank)
				continue;
			buf[m] = (char)0;
			break;
		}
		buf[m++] = s;
		if (s == (char)0)
			break;
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

    public static int inTointFromFile(FileInputStream in) throws IOException
    {
	int a;
	char[] buf;
	String str;
	buf = new char[dslength];
	readinFromFile(buf, in);
	str = arrayTostring(buf);
	a = stringToint(str);
	return(a);
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

    private static void minusone(Numberio bf)
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

    private static void plusone(Numberio bf)
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

    private static void fillByasterisk(char[] buf, Numberio bf, int w)
    {
	int i;
	for (i = 0; i < w; ++i)
		buf[bf.index++] = '*';
    }	/* fillByasterisk() */

    private static void wrtNaN(char[] buf, Numberio bf, int w)
    {
	int i;
	buf[bf.index++] = 'N';
	buf[bf.index++] = 'a';
	buf[bf.index++] = 'N';
	for (i = 4; i < (w-1); ++i)
		buf[bf.index++] = ' ';

    }	/* wrtNaN() */

    private static void wrtInfinity
	(char[] buf, Numberio bf, int w, boolean sign)
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

    private static void wrtNan
	(char[] buf, Numberio bf, int w, boolean sign)
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
	(char[] buf, Numberio bf, int w, int d, int e,
		char expletter, boolean sign)
    {
	int i;
	int k;
	int n;
	int signwidth;
	int width;
	int retval;

	retval = fpValiD;

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

    private static int wrtEnormal
	(char[] buf, Numberio bf, int w, int d, int e,
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

	retval		= fpValiD;

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
    }	/* wrtEnormal() */

    private static void doubleTodecimal
	(Numberio bf, double x, int ndigits)
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
	bf.sign		= signbit(x);
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
	x = fabs(x);
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
	strcpy(bf.ds, buf, dslength);
	bf.fpclass	= fpNormal;
	bf.exponent	= exp;
	bf.ndigits	= ndigits;
	if (isround)
	{
		bf.sign = false;
		if (bf.ds[ndigits] >= '5')
			plusone(bf);
		bf.sign = sign;
	}
	for (idx = ndigits; idx < dslength; idx++)
		bf.ds[idx] = (char)0;
    }

    private static void floatTodecimal
	(Numberio bf, float x, int ndigits)
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
	bf.sign		= signbitf(x);
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
	x = fabsf(x);
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
	strcpy(bf.ds, buf, dslength);
	bf.fpclass	= fpNormal;
	bf.exponent	= exp;
	bf.ndigits	= ndigits;
	if (isround)
	{
		bf.sign = false;
		if (bf.ds[ndigits] >= '5')
			plusone(bf);
		bf.sign = sign;
	}
	for (idx = ndigits; idx < dslength; idx++)
		bf.ds[idx] = (char)0;
    }

    private static int wrtEnumber
	(char[] buf, Numberio bf, int w, int d, int e,
		double x)
    {
	int	k;
	int	ndigits;
	boolean	sign;
	int	retval;

	retval = fpValiD;

	if (x == 0)
	{
		sign = signbit(x);
		retval = wrtEzero(buf, bf, w, d, e, letter, sign);
	}
	else
	{
		if (Double.isInfinite(x))
		{
			sign = signbit(x);
			wrtInfinity(buf, bf, w, sign);
			return(retval);
		}
		if (Double.isNaN(x))
		{
			sign = signbit(x);
			wrtNan(buf, bf, w, sign);
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
		doubleTodecimal(bf, x, ndigits);
		retval = wrtEnormal
				(buf, bf, w, d, e, letter, bf.sign,
					bf.ndigits + bf.exponent, bf.ds);
	}
	return(retval);
    }	/* wrtEnumber() */

    private static int wrtEnumberf
	(char[] buf, Numberio bf, int w, int d, int e,
		float x)
    {
	int	k;
	int	ndigits;
	boolean	sign;
	int	retval;

	retval = fpValiD;

	if (x == 0)
	{
		sign = signbitf(x);
		retval = wrtEzero(buf, bf, w, d, e, letter, sign);
	}
	else
	{
		if (Float.isInfinite(x))
		{
			sign = signbitf(x);
			wrtInfinity(buf, bf, w, sign);
			return(retval);
		}
		if (Float.isNaN(x))
		{
			sign = signbitf(x);
			wrtNan(buf, bf, w, sign);
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
		floatTodecimal(bf, x, ndigits);
		retval = wrtEnormal
				(buf, bf, w, d, e, letter, bf.sign,
					bf.ndigits + bf.exponent, bf.ds);
	}
	return(retval);
    }	/* wrtEnumberf() */

    private static int wrtE
	(char[] buf, Numberio bf, int w, int d, int e,
		double l)
    {
	int retval;

	retval = fpValiD;

	if (w < 8)
	{
		fillByasterisk(buf, bf, w);
		return(retval);
	}
	if (Double.isNaN(l))
	{
		wrtNaN(buf, bf, w);
		return(retval);
	}
	retval = wrtEnumber(buf, bf, w, d, e, l);
	buf[bf.index++] = (char)0;
	return(retval);
    }	/* wrtE() */

    private static int wrtEf
	(char[] buf, Numberio bf, int w, int d, int e,
		float l)
    {
	int retval;

	retval = fpValiD;

	if (w < 7)
	{
		fillByasterisk(buf, bf, w);
		return(retval);
	}
	if (Float.isNaN(l))
	{
		wrtNaN(buf, bf, w);
		return(retval);
	}
	retval = wrtEnumberf(buf, bf, w, d, e, l);
	buf[bf.index++] = (char)0;
	return(retval);
    }	/* wrtEf() */

    public static String doubleNumberTostring
	(double l, int w, int d, int e, int format)
    {
	Numberio	bf;
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
			w = 23;
			d = 16;
			e = 3;
		}
		else
		{
			w = 23;
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
	bf	= new Numberio();
	flushbuffer(buf, dslength);
	switch (format)
	{
		case 3:
			retval = wrtE(buf, bf, w, d, e, l);
			break;
		case 4:
			retval = wrtG(buf, bf, w, d, e, l);
			break;
		default:
			retval = wrtG(buf, bf, w, d, e, l);
			break;
	}
	str = arrayTostring(buf);
	return(str);
    }	/* doubleNumberTostring(double l,  ...) */

    public static String floatNumberTostring
	(float l, int w, int d, int e, int format)
    {
	Numberio	bf;
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
			w = 13;
			d = 7;
			e = 2;
		}
		else
		{
			w = 13;
			d = 7;
			e = 2;
		}
	}
	if (d <= 0)
	{
		retval = fpBaDFormat;
		return(str);
	}
	buf	= new char[dslength];
	flushbuffer(buf, dslength);
	bf	= new Numberio();
	flushbuffer(buf, dslength);
	switch (format)
	{
		case 3:
			retval = wrtEf(buf, bf, w, d, e, l);
			break;
		case 4:
			retval = wrtGf(buf, bf, w, d, e, l);
			break;
		default:
			retval = wrtGf(buf, bf, w, d, e, l);
			break;
	}
	str = arrayTostring(buf);
	return(str);
    }	/* floatNumberTostring(float l,  ...) */

    private static int wrtGzero
	(char[] buf, Numberio bf, int w, int d, int e, boolean sign)
    {
	int n;
	int signwidth;
	int retval;

	retval = fpValiD;

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

    private static int wrtGnearest
	(char[] buf, Numberio bf, int w, int d, int e,
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

	retval = fpValiD;

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
		retval = wrtEnormal(
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
    }	/* wrtGnearest() */

    private static int wrtGnumber
	(char[] buf, Numberio bf, int w, int d, int e,
		double x)
    {
	int	ndigits;
	boolean	sign;
	int	retval;

	retval = fpValiD;

	if (w < 0 || d < 0)
		return(retval);
	if (w < 3)
	{
		fillByasterisk(buf, bf, w);
		return(retval);
	}
	if (x == 0)
	{
		sign = signbit(x);
		retval = wrtGzero(buf, bf, w, d, e, sign);
		return(retval);
	}
	if (Double.isInfinite(x))
	{
		sign = signbit(x);
		wrtInfinity(buf, bf, w, sign);
		return(retval);
	}
	if (Double.isNaN(x))
	{
		sign = signbit(x);
		wrtNan(buf, bf, w, sign);
		return(retval);
	}
	/*ndigits = d + 2;*/
	ndigits = d;
	if (ndigits > dslength - 1)
		  ndigits = dslength - 1;
	doubleTodecimal(bf, x, ndigits);
		retval = wrtGnearest
				(buf, bf, w, d, e, bf.sign, bf.ndigits,
					bf.exponent, bf.ds);
	return(retval);
    }	/* wrtGnumber() */

    private static int wrtGnumberf
	(char[] buf, Numberio bf, int w, int d, int e,
		float x)
    {
	int	ndigits;
	boolean	sign;
	int	retval;

	retval = fpValiD;

	if (w < 0 || d < 0)
		return(retval);
	if (w < 3)
	{
		fillByasterisk(buf, bf, w);
		return(retval);
	}
	if (x == 0)
	{
		sign = signbitf(x);
		retval = wrtGzero(buf, bf, w, d, e, sign);
		return(retval);
	}
	if (Float.isInfinite(x))
	{
		sign = signbitf(x);
		wrtInfinity(buf, bf, w, sign);
		return(retval);
	}
	if (Float.isNaN(x))
	{
		sign = signbitf(x);
		wrtNan(buf, bf, w, sign);
		return(retval);
	}
	/*ndigits = d + 2;*/
	ndigits = d;
	if (ndigits > dslength - 1)
		  ndigits = dslength - 1;
	floatTodecimal(bf, x, ndigits);
		retval = wrtGnearest
				(buf, bf, w, d, e, bf.sign, bf.ndigits,
					bf.exponent, bf.ds);
	return(retval);
    }	/* wrtGnumberf() */

    private static int wrtG
	(char[] buf, Numberio bf, int w, int d, int e,
		double l)
    {
	int retval;

	retval = fpValiD;

	if (w < 0 || d < 0)
		return(retval);
	if (w < 3)
	{
		fillByasterisk(buf, bf, w);
		return(retval);
	}
	if (Double.isNaN(l))
	{
		wrtNaN(buf, bf, w);
		return(retval);
	}
	retval = wrtGnumber(buf, bf, w, d, e, l);
	return(retval);
    }	/* wrtG() */

    private static int wrtGf
	(char[] buf, Numberio bf, int w, int d, int e,
		float l)
    {
	int retval;

	retval = fpValiD;

	if (w < 0 || d < 0)
		return(retval);
	if (w < 3)
	{
		fillByasterisk(buf, bf, w);
		return(retval);
	}
	if (Float.isNaN(l))
	{
		wrtNaN(buf, bf, w);
		return(retval);
	}
	retval = wrtGnumberf(buf, bf, w, d, e, l);
	return(retval);
    }	/* wrtGf() */



    /* Double format */

    public static String toEstring(double l, int w, int d, int e) {
	String str;
	str = doubleNumberTostring
		(l, w, d, e, E_NUMBER_FORMAT);
	return(str);
    }

    public static String toEstring(double l) {
	return toEstring(l, 0, 0, 0);
    }

    public static String toGstring(double l, int w, int d, int e) {
	String str;
	str = doubleNumberTostring
		(l, w, d, e, G_NUMBER_FORMAT);
	return(str);
    }

    public static String toGstring(double l) {
	return toGstring(l, 0, 0, 0);
    }

    public static String toString(double l) {
	return(toEstring(l));
    }

    /* Float format */

    public static String toEstringf(float l, int w, int d, int e) {
	String str;
	str = floatNumberTostring
		(l, w, d, e, E_NUMBER_FORMAT);
	return(str);
    }

    public static String toEstringf(float l) {
	return toEstringf(l, 0, 0, 0);
    }

    public static String toGstringf(float l, int w, int d, int e) {
	String str;
	str = floatNumberTostring
		(l, w, d, e, G_NUMBER_FORMAT);
	return(str);
    }

    public static String toGstringf(float l) {
	return toGstringf(l, 0, 0, 0);
    }

    public static String toStringf(float l) {
	return(toEstringf(l));
    }
}
