/**
 * #pragma ident "@(#)Intervalf.java 1.2 (Sun Microsystems, Inc.) 02/12/06"
 */
/**
 *  Copyright © 2002 Sun Microsystems, Inc.
 *  Alexei Agafonov aga@nbsp.nsk.su
 *  All rights reserved.
 */
/**
	Java float interval class
*/
import java.io.*;
import java.lang.*;
import java.util.*;
import Arith;
import Interval;
import Intervalio;
import java.net.URL;

public class Intervalf
{
    /* Fields */

    private float inf;
    private float sup;

    /* Constants */

    private final static float zero	= 0.0f;
    private final static float half	= 0.5f;
    private final static float one	= 1.0f;
    private final static float two24	= 16777216.0f;
    private final static double pihi	= 3.14159265358979356010e+00;
    private final static double pilo	= -3.21624529935327320104e-16;
    private final static double pi	= 3.14159265358979311600;	/* pi chopped */
    private final static double pit	= 1.22464679914735320717e-16;	/* tail of pi rounded up */
    private final static double twopi	= 6.28318530717958623200;	/* 2 pi chopped */
    private final static double twopit	= 2.44929359829470641435e-16;	/* tail of 2 pi rounded up */
    private final static int maxaccure		= 7;
    private final static int buffersize		= 64;
    private final static int AngularIval	= 1;
    private final static int SINGLE_NUMBER_FORMAT	= 1;
    private final static int VE_NUMBER_FORMAT		= 3;
    private final static int VG_NUMBER_FORMAT		= 4;

    /* Constructors */

    public Intervalf() {
	inf = -zero;
	sup = +zero;
    }

    public Intervalf(URL url) {
	inf = -zero;
	sup = +zero;
    }

    public Intervalf(long x) {
	if (x == 0l) {
		inf = -zero;
		sup = +zero;
	}
	else
		inf = sup = (float)x;
    }

    public Intervalf(long x, long y) {
	if (x > y) {
		inf = Float.NEGATIVE_INFINITY;
		sup = Float.POSITIVE_INFINITY;
	}
	else
	{
		inf = x == 0l ? -zero : (float)x;
		sup = y == 0l ? +zero : (float)y;
	}
    }

    public Intervalf(float x) {
	if (Float.isNaN(x)) {
		inf = Float.NEGATIVE_INFINITY;
		sup = Float.POSITIVE_INFINITY;
		return;
	}
	if (x == zero) {
		inf = -zero;
		sup = +zero;
		return;
	}
	if (Float.isInfinite(x))
		if ( x > zero)
		{
			inf = +Float.MAX_VALUE;
			sup = x;
			return;
		}
		else
		{
			inf = x;
			sup = -Float.MAX_VALUE;
			return;
		}
	inf = sup = x;
    }

    private Intervalf(float x, boolean o) {
	if (o)
		if (Float.isNaN(x)) {
			inf = Float.NEGATIVE_INFINITY;
			sup = Float.POSITIVE_INFINITY;
			return;
		}
	if (x == zero) {
		inf = -zero;
		sup = +zero;
		return;
	}
	if (Float.isInfinite(x))
		if ( x > zero)
		{
			inf = +Float.MAX_VALUE;
			sup = x;
			return;
		}
		else
		{
			inf = x;
			sup = -Float.MAX_VALUE;
			return;
		}
	inf = sup = x;
    }

    public Intervalf(float x, float y) {
	if (Float.isNaN(x) || Float.isNaN(y)) {
		inf = Float.NEGATIVE_INFINITY;
		sup = Float.POSITIVE_INFINITY;
		return;
	}
	if (x > y) {
		inf = Float.NEGATIVE_INFINITY;
		sup = Float.POSITIVE_INFINITY;
		return;
	}
	if (x == y && Float.isInfinite(x))
		if ( x > zero)
		{
			inf = +Float.MAX_VALUE;
			sup = x;
			return;
		}
		else
		{
			inf = x;
			sup = -Float.MAX_VALUE;
			return;
		}
	inf = x == zero ? -zero : x;
	sup = y == zero ? +zero : y;
    }

    public Intervalf(double x) {
	if (Double.isNaN(x))
	{
		inf = Float.NEGATIVE_INFINITY;
		sup = Float.POSITIVE_INFINITY;
		return;
	}
	if (x == 0.0)
	{
		inf = -zero;
		sup = +zero;
	}
	else
		inf = sup = (float)x;
    }

    public Intervalf(double x, double y) {
	if (Double.isNaN(x) || Double.isNaN(y))
	{
		inf = Float.NEGATIVE_INFINITY;
		sup = Float.POSITIVE_INFINITY;
		return;
	}
	if (x > y)
	{
		inf = Float.NEGATIVE_INFINITY;
		sup = Float.POSITIVE_INFINITY;
	}
	else
	{
		inf = x == 0.0 ? -zero : (float)x;
		sup = y == 0.0 ? +zero : (float)y;
	}
    }

    public Intervalf(Intervalf x) {
	inf = x.inf;
	sup = x.sup;
    }

    public Intervalf(Interval x) {
	inf = (float)x.inf();
	sup = (float)x.sup();
	if ((double)inf > x.inf())
		inf =  Arith.prevf(inf);
	if ((double)sup < x.sup())
		sup =  Arith.nextf(inf);
    }

    public Intervalf(String s) {
	stringToIntervalf(s);
    }

    public Intervalf(char[] b) {
	bufToIntervalf(b);
    }

    /* Pure - No Native Class */

    public static boolean isNativef() {
	return false;
    }

    /* Assigns */

    public Intervalf assign(long x) {
	if (x == 0l) {
		this.inf = -zero;
		this.sup = +zero;
	}
	else
		this.inf = this.sup = (float)x;
	return this;
    }

    public Intervalf assign(long x, long y) {
	if (x > y) {
		this.inf = Float.NEGATIVE_INFINITY;
		this.sup = Float.POSITIVE_INFINITY;
	}
	else
	{
		this.inf = x == 0l ? -zero : (float)x;
		this.sup = y == 0l ? +zero : (float)y;
	}
	return this;
    }

    public Intervalf assign(float x) {
	if (Float.isNaN(x)) {
		this.inf = Float.NEGATIVE_INFINITY;
		this.sup = Float.POSITIVE_INFINITY;
		return this;
	}
	if (x == zero) {
		this.inf = -zero;
		this.sup = +zero;
		return this;
	}
	if (Float.isInfinite(x))
		if ( x > zero)
		{
			this.inf = +Float.MAX_VALUE;
			this.sup = x;
			return this;
		}
		else
		{
			this.inf = x;
			this.sup = -Float.MAX_VALUE;
			return this;
		}
	this.inf = this.sup = x;
	return this;
    }

    public Intervalf assign(float x, float y) {
	if (Float.isNaN(x) || Float.isNaN(y)) {
		this.inf = Float.NEGATIVE_INFINITY;
		this.sup = Float.POSITIVE_INFINITY;
		return this;
	}
	if (x > y) {
		this.inf = Float.NEGATIVE_INFINITY;
		this.sup = Float.POSITIVE_INFINITY;
		return this;
	}
	if (x == y && Float.isInfinite(x))
		if ( x > zero)
		{
			this.inf = +Float.MAX_VALUE;
			this.sup = x;
			return this;
		}
		else
		{
			this.inf = x;
			this.sup = -Float.MAX_VALUE;
			return this;
		}
	this.inf = x == zero ? -zero : x;
	this.sup = y == zero ? +zero : y;
	return this;
    }

    public Intervalf assign(double x) {
	if (Double.isNaN(x))
	{
		this.inf = Float.NEGATIVE_INFINITY;
		this.sup = Float.POSITIVE_INFINITY;
		return this;
	}
	if (x == 0.0)
	{
		this.inf = -zero;
		this.sup = +zero;
	}
	else
		this.inf = this.sup = (float)x;
	return this;
    }

    public Intervalf assign(double x, double y) {
	if (Double.isNaN(x) || Double.isNaN(y))
	{
		this.inf = Float.NEGATIVE_INFINITY;
		this.sup = Float.POSITIVE_INFINITY;
		return this;
	}
	if (x > y)
	{
		this.inf = Float.NEGATIVE_INFINITY;
		this.sup = Float.POSITIVE_INFINITY;
	}
	else
	{
		this.inf = x == 0.0 ? -zero : (float)x;
		this.sup = y == 0.0 ? +zero : (float)y;
	}
	return this;
    }

    public Intervalf assign(Intervalf x) {
	this.inf = x.inf;
	this.sup = x.sup;
	return this;
    }

    public Intervalf assign(Interval x) {
	this.inf = (float)x.inf();
	this.sup = (float)x.sup();
	if ((double)this.inf > x.inf())
		this.inf =  Arith.prevf(this.inf);
	if ((double)this.sup < x.sup())
		this.sup =  Arith.nextf(this.inf);
	return this;
    }

    public Intervalf assign(String s) {
	return stringToIntervalf(s);
    }

    public Intervalf assign(char[] b) {
	return bufToIntervalf(b);
    }

    public Intervalf clon() {
	try {
		return (Intervalf)clone();
	}
	catch (CloneNotSupportedException e) {
		return new Intervalf(inf, sup);
	}
    }

    /* Bounds */

    public float inf() {
	return(this.inf);
    }

    public float sup() {
	return(this.sup);
    }

    public boolean isEmpty() {
	/**/
	return(this.inf != this.inf && this.sup != this.sup);
	/**/
	/**
	return(Float.isNaN(this.inf) && Float.isNaN(this.sup));
	**/
    }

    public boolean isEntire() {
	return(-this.inf == this.sup && Float.isInfinite(this.inf));
    }

    public boolean isDegenerate() {
	return(this.inf == this.sup);
    }

    /* Java Interval Arithmetic Operations */
    /*
	add
	sub
	div
	mul
	dsub
	ddiv
	divix
	dmul
    */

    public Intervalf add(Intervalf y) {
	this.inf = Arith.addnegative(this.inf, y.inf);
	this.sup = Arith.addpositive(this.sup, y.sup);
	return this;
    }

    public Intervalf sub(Intervalf y) {
	float l;
	l = Arith.subnegative(this.inf, y.sup);
	this.sup = Arith.subpositive(this.sup, y.inf);
	this.inf = l;
	return this;
    }

    public Intervalf div(Intervalf y) {
	float	l, u;
	int	idx;
	idx	= (y.inf > 0)?1:(y.sup < 0)?2:(y.inf == y.inf)?0:16;
	idx	|= (this.inf == this.inf)?0:32;
	if (idx == 0)		/* 0 in y */
	{
		this.inf = Float.NEGATIVE_INFINITY;
		this.sup = Float.POSITIVE_INFINITY;
		return this;
	}
	idx |= (this.inf > 0)?4:(this.sup < 0)?8:0;
	switch (idx)
	{
		case 5:		/* x > 0 && y > 0 */
			l = Arith.divnegative(this.inf, y.sup);
			u = Arith.divpositive(this.sup, y.inf);
			this.inf = l;
			this.sup = u;
			return this;
		case 6:		/* x > 0 && y < 0 */
			l = Arith.divnegative(this.sup, y.sup);
			u = Arith.divpositive(this.inf, y.inf);
			this.inf = l;
			this.sup = u;
			return this;
		case 9:		/* x < 0 && y > 0 */
			l = Arith.divnegative(this.inf, y.inf);
			u = Arith.divpositive(this.sup, y.sup);
			this.inf = l;
			this.sup = u;
			return this;
		case 10:	/* x < 0 && y < 0 */
			l = Arith.divnegative(this.sup, y.inf);
			u = Arith.divpositive(this.inf, y.sup);
			this.inf = l;
			this.sup = u;
			return this;
		case 1:		/* 0 in x && y > 0 */
			l = Arith.divnegative(this.inf, y.inf);
			u = Arith.divpositive(this.sup, y.inf);
			this.inf = l;
			this.sup = u;
			return this;
		case 2:		/* 0 in x && y < 0 */
			l = Arith.divnegative(this.sup, y.sup);
			u = Arith.divpositive(this.inf, y.sup);
			this.inf = l;
			this.sup = u;
			return this;
		case 16:	/* y is empty */
		case 20:	/* y is empty */
		case 24:	/* y is empty */
			this.inf = Float.NaN;
			this.sup = Float.NaN;
			return this;
		case 32:	/* x is empty */
		case 33:	/* x is empty */
		case 34:	/* x is empty */
		case 48:	/* x and y is empty */
			this.inf = Float.NaN;
			this.sup = Float.NaN;
			return this;
		default:	/* An illegal construction */
			this.inf = Float.NEGATIVE_INFINITY;
			this.sup = Float.POSITIVE_INFINITY;
			return this;
	}
    }

    public Intervalf mul(Intervalf y) {
	float	l, u;
	float	ol, ou;
	int	idx;
	idx	= (this.inf > 0)?1:(this.sup < 0)?2:(this.inf == this.inf)?0:16;
	idx	|= (y.inf > 0)?4:(y.sup < 0)?8:(y.inf == y.inf)?0:32;
	switch (idx)
	{
		case 5:		/* x > 0 && y > 0 */
			l = Arith.mulnegative(this.inf, y.inf);
			u = Arith.mulpositive(this.sup, y.sup);
			this.inf = l;
			this.sup = u;
			return this;
		case 9:		/* x > 0 && y < 0 */
			l = Arith.mulnegative(this.sup, y.inf);
			u = Arith.mulpositive(this.inf, y.sup);
			this.inf = l;
			this.sup = u;
			return this;
		case 6:		/* x < 0 && y > 0 */
			l = Arith.mulnegative(this.inf, y.sup);
			u = Arith.mulpositive(this.sup, y.inf);
			this.inf = l;
			this.sup = u;
			return this;
		case 10:	/* x < 0 && y < 0 */
			l = Arith.mulnegative(this.sup, y.sup);
			u = Arith.mulpositive(this.inf, y.inf);
			this.inf = l;
			this.sup = u;
			return this;
		case 1:		/* x > 0 && 0 in y */
			l = Arith.mulnegative(this.sup, y.inf);
			u = Arith.mulpositive(this.sup, y.sup);
			if (l != l || u != u)
			{
				this.inf = Float.NEGATIVE_INFINITY;
				this.sup = Float.POSITIVE_INFINITY;
				return this;
			}
			this.inf = l;
			this.sup = u;
			return this;
		case 2:		/* x < 0 && 0 in y */
			l = Arith.mulnegative(this.inf, y.sup);
			u = Arith.mulpositive(this.inf, y.inf);
			if (l != l || u != u)
			{
				this.inf = Float.NEGATIVE_INFINITY;
				this.sup = Float.POSITIVE_INFINITY;
				return this;
			}
			this.inf = l;
			this.sup = u;
			return this;
		case 4:		/* 0 in x && y > 0 */
			l = Arith.mulnegative(this.inf, y.sup);
			u = Arith.mulpositive(this.sup, y.sup);
			if (l != l || u != u)
			{
				this.inf = Float.NEGATIVE_INFINITY;
				this.sup = Float.POSITIVE_INFINITY;
				return this;
			}
			this.inf = l;
			this.sup = u;
			return this;
		case 8:		/* 0 in x && y < 0 */
			l = Arith.mulnegative(this.sup, y.inf);
			u = Arith.mulpositive(this.inf, y.inf);
			if (l != l || u != u)
			{
				this.inf = Float.NEGATIVE_INFINITY;
				this.sup = Float.POSITIVE_INFINITY;
				return this;
			}
			this.inf = l;
			this.sup = u;
			return this;
		case 0:		/* 0 in x && 0 in y */
			l = Arith.mulnegative(this.inf, y.sup);
			ol = Arith.mulnegative(this.sup, y.inf);
			u = Arith.mulpositive(this.inf, y.inf);
			ou = Arith.mulpositive(this.sup, y.sup);
			if (l != l || u != u || ol != ol || ou != ou)
			{
				this.inf = Float.NEGATIVE_INFINITY;
				this.sup = Float.POSITIVE_INFINITY;
				return this;
			}
			if (l > ol)
				l = ol;
			if (u < ou)
				u = ou;
			this.inf = l;
			this.sup = u;
			return this;
		case 16:	/* x is empty */
		case 20:	/* x is empty */
		case 24:	/* x is empty */
			this.inf = Float.NaN;
			this.sup = Float.NaN;
			return this;
		case 32:	/* y is empty */
		case 33:	/* y is empty */
		case 34:	/* y is empty */
		case 48:	/* x and y is empty */
			this.inf = Float.NaN;
			this.sup = Float.NaN;
			return this;
		default:	/* An illegal construction */
			this.inf = Float.NEGATIVE_INFINITY;
			this.sup = Float.POSITIVE_INFINITY;
			return this;
	}
    }

    public Intervalf dsub(Intervalf y) {
	float l, u;
	if (this.inf != this.inf)		/* x is empty */
	{
		if (y.inf == y.inf)	/* y is not empty */
		{
			this.inf = Float.NaN;
			this.sup = Float.NaN;
			return this;
		}
		else
		{
			this.inf = Float.NEGATIVE_INFINITY;
			this.sup = Float.POSITIVE_INFINITY;
			return this;
		}
	}
	if (y.inf != y.inf)		/* x is not empty, y is empty */
	{
		this.inf = Float.NEGATIVE_INFINITY;
		this.sup = Float.POSITIVE_INFINITY;
		return this;
	}
	l = Arith.subnegative(this.inf, y.inf);
	u = Arith.subpositive(this.sup, y.sup);
	if (l > u)
	{
		this.inf = Float.NEGATIVE_INFINITY;
		this.sup = Float.POSITIVE_INFINITY;
		return this;
	}
	if (l != l)
		l = this.inf;
	if (u != u)
		u = this.sup;
	if (l == u && Float.isInfinite(l))
		if (l > 0)
			l = -l;
		else
			u = -u;
	this.inf = l;
	this.sup = u;
	return this;
    }

    public Intervalf ddiv(Intervalf a) {
	float	l, u;
	int	idx;

	/* Build the indethis. */
	/* x_case */
	idx =
	(this.inf < 0) ? /* 2-9 */
		(this.sup > 0) ? /* 4,5,8,9 */
			(this.inf == Float.NEGATIVE_INFINITY) ?  /* 4-5 */
				(this.sup == Float.POSITIVE_INFINITY) ? 5 : 4
			: /* 8,9 */
				(this.sup == Float.POSITIVE_INFINITY) ? 9 : 8
		: /* 2,3,6,7 */
			(this.inf == Float.NEGATIVE_INFINITY) ?  /* 2,3 */
				(this.sup == 0) ? 3 : 2
			: /* 6,7 */
				(this.sup == 0) ? 7 : 6
	: /* 1, 10-14 */
		(this.sup == Float.POSITIVE_INFINITY) ?  /* 12,14 */
			(this.inf == 0) ? 12 : 14
		: /* 1,10,11,13 */
			(this.inf == 0) ? /* 10,11 */
				(this.sup == 0) ? 10 : 11
			: /* 1,13 */
				(this.inf == this.inf) ? 13 : 1;
	/* a_case */
	idx |=
	(a.inf < 0) ? /* 2-9 */
		(a.sup > 0) ? /* 4,5,8,9 */
			(a.inf == Float.NEGATIVE_INFINITY) ? /* 4-5 */
				(a.sup == Float.POSITIVE_INFINITY) ? (5 << 4) : (4 << 4)
			: /* 8,9 */
				(a.sup == Float.POSITIVE_INFINITY) ? (9 << 4) : (8 << 4)
		: /* 2,3,6,7 */
			(a.inf == Float.NEGATIVE_INFINITY) ? /* 2,3 */
				(a.sup == 0) ? (3 << 4) : (2 << 4)
			: /* 6,7 */
				(a.sup == 0) ? (7 << 4) : (6 << 4)
	: /* 1, 10-14 */
		(a.sup == Float.POSITIVE_INFINITY) ? /* 12,14 */
			(a.inf == 0) ? (12 << 4) : (14 << 4)
		: /* 1,10,11,13 */
			(a.inf == 0 ) ? /* 10,11 */
				(a.sup == 0) ? (10 << 4) : (11 << 4)
			: /* 1,13 */
				(a.inf == a.inf) ? (13 << 4) : (1 << 4);
	/* Jump to the relevant case */
	switch (idx)
	{
		case 6+(6 << 4):
			l = Arith.divnegative(this.sup, a.sup);
			u = Arith.divpositive(this.inf, a.inf);
			/* simplified va.infidity check
				(-,-) .ddiv. (-,-)
				|this.inf| >= |this.sup|
				|a.inf| >= |a.sup|
				a.sup/a.inf >= this.sup/this.inf
				this.sup/a.sup <= this.inf/a.inf
			*/
			if (l > u)
			{
				this.inf = Float.NEGATIVE_INFINITY;
				this.sup = Float.POSITIVE_INFINITY;
				return this;
			}
			this.inf = l;
			this.sup = u;
			return this;
		case 6+(13 << 4):
			l = Arith.divnegative(this.inf, a.sup);
			u = Arith.divpositive(this.sup, a.inf);
			/* simplified va.infidity check
				(-,-) .ddiv. (+,+)
				|this.inf| >= |this.sup|
				|a.inf| <= |a.sup|
				a.inf/a.sup >= this.sup/this.inf
				this.inf/a.sup <= this.sup/a.inf
			*/
			if (l > u)
			{
				this.inf = Float.NEGATIVE_INFINITY;
				this.sup = Float.POSITIVE_INFINITY;
				return this;
			}
			this.inf = l;
			this.sup = u;
			return this;
		case 8+(6 << 4):
		case 8+(7 << 4):
			l = Arith.divnegative(this.sup, a.inf);
			u = Arith.divpositive(this.inf, a.inf);
			/* no va.infidity check
				{a.inf/a.sup,a.sup/a.inf} >= 0 > {this.inf/this.sup,this.sup/this.inf} < 0
			*/
			this.inf = l;
			this.sup = u;
			return this;
		case 8+(8 << 4):
			if (this.sup <= -this.inf )
			{
				l = Arith.divnegative(this.sup, -this.inf);
			}
			else
			{
				l = Arith.divnegative(-this.inf, this.sup);
			}
			if (a.sup <= -a.inf)
			{
				if(Arith.divpositive(a.sup, -a.inf) <= l)
				{
					l = Arith.divnegative(this.sup, a.inf);
					u = Arith.divpositive(this.inf, a.inf);
				}
				else
				{
					this.inf = Float.NEGATIVE_INFINITY;
					this.sup = Float.POSITIVE_INFINITY;
					return this;
				}
			}
			else
			{
				if (Arith.divpositive(-a.inf, a.sup) <= l)
				{
					l = Arith.divnegative(this.inf, a.sup);
					u = Arith.divpositive(this.sup, a.sup);
				}
				else
				{
					this.inf = Float.NEGATIVE_INFINITY;
					this.sup = Float.POSITIVE_INFINITY;
					return this;
				}
			}
			this.inf = l;
			this.sup = u;
			return this;
		case 8+(11 << 4):
		case 8+(13 << 4):
			l = Arith.divnegative(this.inf, a.sup);
			u = Arith.divpositive(this.sup, a.sup);
			/* no va.infidity check
				(-,+) .ddiv. (+,+)
				{a.inf/a.sup} >= 0 > {this.inf/this.sup,this.sup/this.inf} < 0
			*/
			this.inf = l;
			this.sup = u;
			return this;
		case 13+(6 << 4):
			l = Arith.divnegative(this.sup, a.inf);
			u = Arith.divpositive(this.inf, a.sup);
			/* simplified va.infidity check
				(+,+) .ddiv. (-,-)
				|this.sup| >= |this.inf|
				|a.inf| >= |a.sup|
				a.inf/a.sup >= this.sup/this.inf
				this.inf/a.sup >= this.sup/a.inf
			*/
			if (l > u)
			{
				this.inf = Float.NEGATIVE_INFINITY;
				this.sup = Float.POSITIVE_INFINITY;
				return this;
			}
			this.inf = l;
			this.sup = u;
			return this;
		case 13+(13 << 4):
			l = Arith.divnegative(this.inf, a.inf);
			u = Arith.divpositive(this.sup, a.sup);
			/* simplified va.infidity check
				(+,+) .ddiv. (+,+) 
				|this.inf| >= |this.sup|
				|a.inf| >= |a.sup|
				a.inf/a.sup >= this.inf/this.sup
				this.sup/a.sup >= this.inf/a.inf
			*/
			if (l > u)
			{
				this.inf = Float.NEGATIVE_INFINITY;
				this.sup = Float.POSITIVE_INFINITY;
				return this;
			}
			this.inf = l;
			this.sup = u;
			return this;
		case 2+(2 << 4):
		case 2+(6 << 4):
			l = Arith.divnegative(this.sup, a.sup);
			u = Float.POSITIVE_INFINITY;
			this.inf = l;
			this.sup = u;
			return this;
		case 2+(13 << 4):
		case 2+(14 << 4):
			l = Float.NEGATIVE_INFINITY;
			u = Arith.divpositive(this.sup, a.inf);
			this.inf = l;
			this.sup = u;
			return this;
		case 3+(7 << 4):
			if (a.inf < -one)
			{
				l = -zero;
				u = Float.MAX_VALUE;
			}
			else
			{
				this.inf = Float.NEGATIVE_INFINITY;
				this.sup = Float.POSITIVE_INFINITY;
				return this;
			}
			this.inf = l;
			this.sup = u;
			return this;
		case 3+(11 << 4):
			if (a.sup > one)
			{
				l = -Float.MAX_VALUE;
				u = zero;
			}
			else
			{
				this.inf = Float.NEGATIVE_INFINITY;
				this.sup = Float.POSITIVE_INFINITY;
				return this;
			}
			this.inf = l;
			this.sup = u;
			return this;
		case 12+(7 << 4):
			if (a.inf < -one)
			{
				l = -Float.MAX_VALUE;
				u = zero;
			}
			else
			{
				this.inf = Float.NEGATIVE_INFINITY;
				this.sup = Float.POSITIVE_INFINITY;
				return this;
			}
			this.inf = l;
			this.sup = u;
			return this;
		case 12+(11 << 4):
			if (a.sup > one)
			{
				l = -zero;
				u = Float.MAX_VALUE;
			}
			else
			{
				this.inf = Float.NEGATIVE_INFINITY;
				this.sup = Float.POSITIVE_INFINITY;
				return this;
			}
			this.inf = l;
			this.sup = u;
			return this;
		case 3+(3 << 4):
		case 3+(6 << 4):
		case 12+(12 << 4):
		case 12+(13 << 4):
			l = -0.0f;
			u = Float.POSITIVE_INFINITY;
			this.inf = l;
			this.sup = u;
			return this;
		case 3+(2 << 4):
			if (a.sup > -one)
			{
				l = Float.MIN_VALUE;
				u = Float.POSITIVE_INFINITY;
			}
			else
			{
				this.inf = Float.NEGATIVE_INFINITY;
				this.sup = Float.POSITIVE_INFINITY;
				return this;
			}
			this.inf = l;
			this.sup = u;
			return this;
		case 3+(14 << 4):
			if (a.inf < one)
			{
				l = Float.NEGATIVE_INFINITY;
				u = -Float.MIN_VALUE;
			}
			else
			{
				this.inf = Float.NEGATIVE_INFINITY;
				this.sup = Float.POSITIVE_INFINITY;
				return this;
			}
			this.inf = l;
			this.sup = u;
			return this;
		case 12+(2 << 4):
			if (a.sup > -one)
			{
				l = Float.NEGATIVE_INFINITY;
				u = -Float.MIN_VALUE;
			}
			else
			{
				this.inf = Float.NEGATIVE_INFINITY;
				this.sup = Float.POSITIVE_INFINITY;
				return this;
			}
			this.inf = l;
			this.sup = u;
			return this;
		case 12+(14 << 4):
			if (a.inf <  one)
			{
				l = Float.MIN_VALUE;
				u = Float.POSITIVE_INFINITY;
			}
			else
			{
				this.inf = Float.NEGATIVE_INFINITY;
				this.sup = Float.POSITIVE_INFINITY;
				return this;
			}
			this.inf = l;
			this.sup = u;
			return this;
		case 3+(12 << 4):
		case 3+(13 << 4):
		case 12+(3 << 4):
		case 12+(6 << 4):
			l = Float.NEGATIVE_INFINITY;
			u = +0.0f;
			this.inf = l;
			this.sup = u;
			return this;
		case 4+(4 << 4):
		case 11+(11 << 4):
		case 11+(13 << 4):
			l = -zero;
			u = Arith.divpositive(this.sup, a.sup);
			this.inf = l;
			this.sup = u;
			return this;
		case 4+(6 << 4):
			l = Arith.divnegative(this.sup, a.inf);
			u = Float.POSITIVE_INFINITY;
			this.inf = l;
			this.sup = u;
			return this;
		case 4+(8 << 4):
			if (a.sup <= -a.inf)
			{
				if (Arith.divpositive(a.sup, -a.inf) < Arith.divnegative(this.sup, -Float.MAX_VALUE))
				{
					l = Arith.divnegative(this.sup, a.inf);
					u = Arith.divpositive(this.sup, a.sup);
				}
				else
				{
					this.inf = Float.NEGATIVE_INFINITY;
					this.sup = Float.POSITIVE_INFINITY;
					return this;
				}
			}
			else
			{
				if (Arith.divpositive(-a.inf, a.sup) < Arith.divnegative(this.sup, -Float.MAX_VALUE))
				{
					l = Arith.divnegative(this.sup, a.inf);
					u = Arith.divpositive(this.sup, a.sup);
				}
				else
				{
					this.inf = Float.NEGATIVE_INFINITY;
					this.sup = Float.POSITIVE_INFINITY;
					return this;
				}
			}
			this.inf = l;
			this.sup = u;
			return this;
		case 4+(9 << 4):
		case 11+(6 << 4):
		case 11+(7 << 4):
			l = Arith.divnegative(this.sup, a.inf);
			u = zero;
			this.inf = l;
			this.sup = u;
			return this;
		case 4+(13 << 4):
			l = Float.NEGATIVE_INFINITY;
			u = Arith.divpositive(this.sup, a.sup);
			this.inf = l;
			this.sup = u;
			return this;
		case 7+(6 << 4):
		case 7+(7 << 4):
		case 9+(9 << 4):
			l = -zero;
			u = Arith.divpositive(this.inf, a.inf);
			this.inf = l;
			this.sup = u;
			return this;
		case 7+(11 << 4):
		case 7+(13 << 4):
		case 9+(4 << 4):
			l = Arith.divnegative(this.inf, a.sup);
			u = zero;
			this.inf = l;
			this.sup = u;
			return this;
		case 9+(6 << 4):
			l = Float.NEGATIVE_INFINITY;
			u = Arith.divpositive(this.inf, a.inf);
			this.inf = l;
			this.sup = u;
			return this;
		case 9+(8 << 4):
			if (a.sup <= -a.inf)
			{
				if (Arith.divpositive(a.sup, -a.inf) < Arith.divnegative(-this.inf, Float.MAX_VALUE))
				{
					l = Arith.divnegative(this.inf, a.sup);
					u = Arith.divpositive(this.inf, a.inf);
				}
				else
				{
					this.inf = Float.NEGATIVE_INFINITY;
					this.sup = Float.POSITIVE_INFINITY;
					return this;
				}
			}
			else
			{
				if (Arith.divpositive(-a.inf, a.sup) < Arith.divnegative(-this.inf, Float.MAX_VALUE))
				{
					l = Arith.divnegative(this.inf, a.sup);
					u = Arith.divpositive(this.inf, a.inf);
				}
				else
				{
					this.inf = Float.NEGATIVE_INFINITY;
					this.sup = Float.POSITIVE_INFINITY;
					return this;
				}
			}
			this.inf = l;
			this.sup = u;
			return this;
		case 9+(13 << 4):
			l = Arith.divnegative(this.inf, a.sup);
			u = Float.POSITIVE_INFINITY;
			return this;
		case 10+(6 << 4):
		case 10+(7 << 4):
		case 10+(8 << 4):
		case 10+(11 << 4):
		case 10+(13 << 4):
			this.inf = -0.0f;
			this.sup = +0.0f;
			return this;
		case 14+(2 << 4):
		case 14+(6 << 4):
			l = Float.NEGATIVE_INFINITY;
			u = Arith.divpositive(this.inf, a.sup);
			this.inf = l;
			this.sup = u;
			return this;
		case 14+(13 << 4):
		case 14+(14 << 4):
			l = Arith.divnegative(this.inf, a.inf);
			u = Float.POSITIVE_INFINITY;
			this.inf = l;
			this.sup = u;
			return this;
		case 1+(2 << 4):
		case 1+(3 << 4):
		case 1+(4 << 4):
		case 1+(5 << 4):
		case 1+(6 << 4):
		case 1+(7 << 4):
		case 1+(8 << 4):
		case 1+(9 << 4):
		case 1+(10 << 4):
		case 1+(11 << 4):
		case 1+(12 << 4):
		case 1+(13 << 4):
		case 1+(14 << 4):
			l = Float.NaN;
			u = Float.NaN;
			this.inf = l;
			this.sup = u;
			return this;
		default:
			this.inf = Float.NEGATIVE_INFINITY;
			this.sup = Float.POSITIVE_INFINITY;
			return this;
	}
    }

    public Intervalf divix(Intervalf y, Intervalf c) {
	Intervalf	o, yl, yu, e, f;
	float		l, u;
	if (isEmpty())
		return this;
	if (y.isEmpty() || c.isEmpty())
	{
		this.inf = Float.NaN;
		this.sup = Float.NaN;
		return this;
	}

	/* the denominator does not contain zero */
	/* usual division */
	o = new Intervalf(this);
	if (y.inf > zero || y.sup < zero)
	{
		o.div(y);
		o.ix(c);
		this.assign(o);
		return(this);
	}

	/* the numerator contains zero and the denominator contains zero */
	if (this.inf <= zero && this.sup >= zero)
	{
		this.assign(c);
		return(this);
	}
	yl = new Intervalf(y.inf);
	yu = new Intervalf(y.sup);
	e = new Intervalf(Float.NaN, false);
	f = new Intervalf(Float.NaN, false);

	/* Kahan division */
	if (this.inf > zero)
	{
		if (yl.inf != zero)
		{
			e.assign(o).div(yl);
			l = Float.NEGATIVE_INFINITY;
			u = e.sup;
			e.assign(l, u);
			e.ix(c);
		}
		if (yu.sup != zero)
		{
			f.assign(o).div(yu);
			l = f.inf;
			u = Float.POSITIVE_INFINITY;
			f.assign(l, u);
			f.ix(c);
		}
	}
	else
	{
		if (yu.sup != zero)
		{
			e.assign(o).div(yu);
			l = Float.NEGATIVE_INFINITY;
			u = e.sup;
			e.assign(l, u);
			e.ix(c);
		}
		if (yl.inf != zero)
		{
			f.assign(o).div(yl);
			l = f.inf;
			u = Float.POSITIVE_INFINITY;
			f.assign(l, u);
			f.ix(c);
		}
	}
	e.ih(f);
	this.assign(e);
	return(this);
    }

    public Intervalf dmul(Intervalf y) {
	float	l, u;
	int	idx;
	idx	= (this.inf > 0)?1:(this.sup < 0)?2:(this.inf == this.inf)?0:16;
	idx	|= (y.inf == y.inf)?0:32;
	if (idx == 0)		/* 0 in x */
	{
		this.inf = Float.NEGATIVE_INFINITY;
		this.sup = Float.POSITIVE_INFINITY;
		return this;
	}
	idx |= (y.inf > 0)?4:(y.sup < 0)?8:0;
	switch (idx)
	{
		case 5:		/* x > 0 && y > 0 */
			l = Arith.mulnegative(this.sup, y.inf);
			u = Arith.mulpositive(this.inf, y.sup);
			break;
		case 9:		/* x > 0 && y < 0 */
			l = Arith.mulnegative(this.inf, y.inf);
			u = Arith.mulpositive(this.sup, y.sup);
			break;
		case 6:		/* x < 0 && y > 0 */
			l = Arith.mulnegative(this.sup, y.sup);
			u = Arith.mulpositive(this.inf, y.inf);
			break;
		case 10:	/* x < 0 && y < 0 */
			l = Arith.mulnegative(this.inf, y.sup);
			u = Arith.mulpositive(this.sup, y.inf);
			break;
		case 1:		/* x > 0 && 0 in y */
			l = Arith.mulnegative(this.inf, y.inf);
			u = Arith.mulpositive(this.inf, y.sup);
			break;
		case 2:		/* x < 0 && 0 in y */
			l = Arith.mulnegative(this.sup, y.sup);
			u = Arith.mulpositive(this.sup, y.inf);
			break;
		case 16:	/* x is empty */
		case 20:	/* x is empty */
		case 24:	/* x is empty */
			this.inf = Float.NaN;
			this.sup = Float.NaN;
			return this;
		default:	/* An illegal construction */
			this.inf = Float.NEGATIVE_INFINITY;
			this.sup = Float.POSITIVE_INFINITY;
			return this;
	}
	if (l > u)
	{
		this.inf = Float.NEGATIVE_INFINITY;
		this.sup = Float.POSITIVE_INFINITY;
		return this;
	}
	if (l != l)
		l = this.inf;
	if (u != u)
		u = this.sup;
	if (l == u && Float.isInfinite(l))
		if (l > 0)
			l = -l;
		else
			u = -u;
	this.inf = l;
	this.sup = u;
	return this;
    }

    /* Java Interval Functions */

    public long intl() {
	float a, b;
	if (isEmpty())
		return(Long.MAX_VALUE);
	b = mid();
	a = Arith.aintf(b);
	if (Arith.fabsf(a) > Long.MAX_VALUE)
		if (a > zero)
			return(Long.MAX_VALUE);
		else
			return(Long.MIN_VALUE);
	return((long)a);
    }

    public Intervalf max(Intervalf y) {
	float l, u;
	if (isEmpty())
	{
		this.inf = y.inf;
		this.sup = y.sup;
		return this;
	}
	if (y.isEmpty())
		return this;
	l = (this.inf > y.inf ? this.inf : y.inf);
	u = (this.sup > y.sup ? this.sup : y.sup);
	this.inf = l;
	this.sup = u;
	return this;
    }

    public Intervalf min(Intervalf y) {
	float l, u;
	if (isEmpty())
	{
		this.inf = y.inf;
		this.sup = y.sup;
		return this;
	}
	if (y.isEmpty())
		return this;
	l = (this.inf < y.inf ? this.inf : y.inf);
	u = (this.sup < y.sup ? this.sup : y.sup);
	this.inf = l;
	this.sup = u;
	return this;
    }

    public float mig() {
	return(this.inf <= zero && this.sup >= zero ? zero : this.inf < zero ? -this.sup : this.inf);
    }

    public float mag() {
	float xl, xu;
	xl = Arith.fabsf(this.inf);
	xu = Arith.fabsf(this.sup);
	return(xl >= xu ? xl : xu);
    }

    public float mid() {
	return(this.inf == this.sup ? this.inf : -this.inf == this.sup ? zero : half * this.inf + half * this.sup);
    }

    public float wid() {
	return(Arith.subpositive(this.sup, this.inf));
    }

    public Intervalf aint() {
	float l, u;
	if (isEmpty())
		return this;
	this.inf = Arith.aintf(this.inf);
	this.sup = Arith.aintf(this.sup);
	return this;
    }

    public Intervalf anint() {
	float l, u;
	if (isEmpty())
		return this;
	this.inf = Arith.anintf(this.inf);
	this.sup = Arith.anintf(this.sup);
	return this;
    }

    public Intervalf negate() {
	float l;
	l = this.inf;
	this.inf = -this.sup;
	this.sup = -l;
	return this;
    }

    public int ceiling() {
	float a;
	if (isEmpty())
		return(Integer.MAX_VALUE);
	a = (float)Math.ceil((double)this.sup);
	if (Arith.fabsf(a) > Integer.MAX_VALUE)
		if (a > zero)
			return(Integer.MAX_VALUE);
		else
			return(Integer.MIN_VALUE);
	return((int)a);
    }

    public int floor() {
	float a;
	if (isEmpty())
		return(Integer.MAX_VALUE);
	a = (float)Math.floor((double)this.inf);
	if (Arith.fabsf(a) > Integer.MAX_VALUE)
		if (a > zero)
			return(Integer.MAX_VALUE);
		else
			return(Integer.MIN_VALUE);
	return((int)a);
    }

    public Intervalf ix(Intervalf y) {
	float l, u;
	if (isEmpty() || y.isEmpty()) {
		this.inf = Float.NaN;
		this.sup = Float.NaN;
		return this;
	}
	l = (this.inf > y.inf ? this.inf : y.inf);
	u = (this.sup < y.sup ? this.sup : y.sup);
	if (l > u) {
		this.inf = Float.NaN;
		this.sup = Float.NaN;
		return this;
	}
	this.inf = l;
	this.sup = u;
	return this;
    }

    public Intervalf ih(Intervalf y) {
	float l, u;
	if (isEmpty())
	{
		this.inf = y.inf;
		this.sup = y.sup;
		return this;
	}
	if (y.isEmpty())
		return this;
	l = (this.inf < y.inf ? this.inf : y.inf);
	u = (this.sup > y.sup ? this.sup : y.sup);
	this.inf = l;
	this.sup = u;
	return this;
    }

    public int ndigits() {
	return(Intervalio.ndigitsf(this.inf, this.sup));
    }

    /* Java Interval Relations */

    public boolean ceq(Intervalf y) {
	return(this.sup <= y.inf && this.inf >= y.sup);
    }

    public boolean cgt(Intervalf y) {
	return(this.inf > y.sup);
    }

    public boolean cge(Intervalf y) {
	return(this.inf >= y.sup);
    }

    public boolean clt(Intervalf y) {
	return(this.sup < y.inf);
    }

    public boolean cle(Intervalf y) {
	return(this.sup <= y.inf);
    }

    public boolean cne(Intervalf y) {
	return(!(this.inf <= y.sup && this.sup >= y.inf));
    }

    public boolean peq(Intervalf y) {
	return(this.inf <= y.sup && this.sup >= y.inf);
    }

    public boolean plt(Intervalf y) {
	return(this.inf < y.sup);
    }

    public boolean ple(Intervalf y) {
	return(this.inf <= y.sup);
    }

    public boolean pgt(Intervalf y) {
	return(this.sup > y.inf);
    }

    public boolean pge(Intervalf y) {
	return(this.sup >= y.inf);
    }

    public boolean pne(Intervalf y) {
	return(!(this.sup <= y.inf && this.inf >= y.sup));
    }

    public boolean sp(Intervalf y) {
	if (y.isEmpty()) return(true);
	return(this.inf <= y.inf && this.sup >= y.sup);
    }

    public boolean psp(Intervalf y) {
	if (isEmpty()) return(false);
	if (y.isEmpty()) return(true);
	return(
		(this.inf <= y.inf && this.sup >= y.sup &&
		(this.inf < y.inf || this.sup > y.sup))
	);
    }

    public boolean sb(Intervalf y) {
	if (isEmpty()) return(true);
	return(this.inf >= y.inf && this.sup <= y.sup);
    }

    public boolean psb(Intervalf y) {
	if (y.isEmpty()) return(false);
	if (isEmpty()) return(true);
	return(
		(this.inf >= y.inf && this.sup <= y.sup &&
		(this.inf > y.inf || this.sup < y.sup))
	);
    }

    public boolean interior(Intervalf y) {
	if (isEmpty()) return(true);
	return(this.inf > y.inf && this.sup < y.sup);
    }

    public boolean in(long x) {
	double a, b;
	if (x >= Integer.MIN_VALUE && x <= Integer.MAX_VALUE) {
		a = (double)x;
		return(a >= this.inf && a <= this.sup);
	}
	a = Arith.longTodoublenegative(x);
	b = Arith.longTodoublepositive(x);
	return(
		(a >= (double)this.inf && a <= (double)this.sup) &&
		(b >= (double)this.inf && b <= (double)this.sup)
	);
    }

    public boolean in(float x) {
	return(x >= this.inf && x <= this.sup);
    }

    public boolean in(double x) {
	return(x >= this.inf && x <= this.sup);
    }

    public boolean dj(Intervalf y) {
	return(!(this.inf <= y.sup && this.sup >= y.inf));
    }

    public boolean seq(Intervalf y) {
	return(
			(this.inf == y.inf && this.sup == y.sup)
			||
			(isEmpty() && y.isEmpty())
	);
    }

    public boolean sne(Intervalf y) {
	return(!(
			(this.inf == y.inf && this.sup == y.sup)
			||
			(isEmpty() && y.isEmpty())
	));
    }

    public boolean slt(Intervalf y) {
	return(this.inf < y.inf && this.sup < y.sup);
    }

    public boolean sle(Intervalf y) {
	if (isEmpty()) return(y.isEmpty());
	if (y.isEmpty()) return(false);
	return(this.inf <= y.inf && this.sup <= y.sup);
    }

    public boolean sgt(Intervalf y) {
	return(this.inf > y.inf && this.sup > y.sup);
    }

    public boolean sge(Intervalf y) {
	if (isEmpty()) return(y.isEmpty());
	if (y.isEmpty()) return(false);
	return(this.inf >= y.inf && this.sup >= y.sup);

    }

    /* Java Interval Mathematical Functions */
    /*
	exp
	log
	log10
	sqrt
	sqr
	cbrt
	sign
	mod
	abs
	cos
	sin
	tan
	acos
	asin
	atan
	atan2
	cosh
	sinh
	tanh
	pow
    */

    /*
	Based on interval exp from Sun FP group
    */
    public Intervalf exp() {
	if (isEmpty())
		return this;
	this.inf = Arith.expnegative(this.inf);
	this.sup = Arith.exppositive(this.sup);
	return this;
    }

    /*
	Based on interval log from Sun FP group
    */
    public Intervalf log() {
	float l, u;
	if (isEmpty())
		return this;
	if (this.sup < zero) {
		this.inf = Float.NaN;
		this.sup = Float.NaN;
		return this;
	}
	if (this.inf <= zero) {
		l = Float.NEGATIVE_INFINITY;
		if (this.sup == zero) {
			u = -Float.MAX_VALUE;
			this.inf = l;
			this.sup = u;
			return this;
		}
		u = Arith.logpositive(this.sup);
		this.inf = l;
		this.sup = u;
		return this;
	}
	this.inf = Arith.lognegative(this.inf);
	this.sup = Arith.logpositive(this.sup);
	return this;
    }

    /*
	Based on interval log10 from Sun FP group
    */
    public Intervalf log10() {
	float l, u;
	if (isEmpty())
		return this;
	if (this.sup < zero) {
		this.inf = Float.NaN;
		this.sup = Float.NaN;
		return this;
	}
	if (this.inf <= zero) {
		l = Float.NEGATIVE_INFINITY;
		if (this.sup == zero) {
			u = -Float.MAX_VALUE;
			this.inf = l;
			this.sup = u;
			return this;
		}
		u = Arith.log10positive(this.sup);
		this.inf = l;
		this.sup = u;
		return this;
	}
	this.inf = Arith.log10negative(this.inf);
	this.sup = Arith.log10positive(this.sup);
	return this;
    }

    /*
	Based on interval sqrt from Sun FP group
    */
    public Intervalf sqrt() {
	float l, u;
	if (isEmpty())
		return this;
	if (this.sup < zero) {
		this.inf = Float.NaN;
		this.sup = Float.NaN;
		return this;
	}
	if (this.inf <= zero) {
		l = -zero;
		if (this.sup == zero) {
			u = +zero;
			this.inf = l;
			this.sup = u;
			return this;
		}
		u = Arith.sqrtpositive(this.sup);
		this.inf = l;
		this.sup = u;
		return this;
	}
	this.inf = Arith.sqrtnegative(this.inf);
	this.sup = Arith.sqrtpositive(this.sup);
	return this;
    }

    /*
	Based on interval sqr from Sun FP group
    */
    public Intervalf sqr() {
	float l, u;
	if (isEmpty())
		return this;
	if (this.inf > zero) {
		this.inf = Arith.sqrnegative(this.inf);
		this.sup = Arith.sqrpositive(this.sup);
		return this;
	}
	if (this.sup < zero) {
		l = Arith.sqrnegative(this.sup);
		u = Arith.sqrpositive(this.inf);
		this.inf = l;
		this.sup = u;
		return this;
	}
	l = -zero;
	if (Arith.fabsf(this.inf) > this.sup)
		u = Arith.sqrpositive(this.inf);
	else
		u = Arith.sqrpositive(this.sup);
	this.inf = l;
	this.sup = u;
	return this;
    }

    /*
	Based on interval cbrt from Sun FP group
    */
    public Intervalf cbrt() {
	if (isEmpty())
		return this;
	this.inf = Arith.cbrtnegative(this.inf);
	this.sup = Arith.cbrtpositive(this.sup);
	return this;
}

    /*
	Based on interval sign from Gnu Fortarn group
    */
    public Intervalf sign(Intervalf y)
    {
	float	l, u, lr1, ur1, lr2, ur2;
	float	xl, xu, yl, yu;
	int	hxl, hxu;
	/* Check whether the second input interval is empty */
	/* make sure the intervals are valid */
	if (isEmpty() || y.isEmpty())
	{
		this.inf = Float.NaN;
		this.sup = Float.NaN;
		return this;
	}
	else
	{
		if (-this.inf == this.sup && Float.isInfinite(this.sup))
			return this;
		hxl = Float.floatToIntBits(this.inf);
		hxu = Float.floatToIntBits(this.sup);
		xl = this.inf;
		xu = this.sup;
		yl = y.inf;
		yu = y.sup;
		if (xl <= zero && hxu >= 0)
			lr1 = zero;
		else
			if (hxl < 0)
				lr1 = -xu;
			else
				lr1 = xl;
		if (hxl < 0)
			xl = -xl;
		if (hxu < 0)
			xu = -xu;
		if (xl < xu)
			ur1 = xu;
		else
			ur1 = xl;
		lr2 = -ur1;
		ur2 = -lr1;
		if (yl >= zero)
		{
			l = lr1;
			u = ur1;
		}
		else
			if (yu < zero)
			{
				l = lr2;
				u = ur2;
			}
			else
			{
				l = lr1 < lr2 ? lr1 : lr2;
				u = ur1 > ur2 ? ur1 : ur2;
			}
	}
	this.inf = l;
	this.sup = u;
	return this;
    }

    /*
	Based on interval fmod from Sun FP group
    */
    /*
    *  Algorithm: (Assume fmod available)
    *
    *  As fmod(x,y) = fmod(x,|y|), we first map the argument box to the
    *  upper half plane.  Let the corners of the resulting box be (xl,yl)
    *  and (xu,yu).  We consider several cases depending on whether and
    *  where the box straddles the lines y = x/m, m an integer.
    *
    *  If the box contains any part of the region lying strictly above
    *  the graph of y = |x|, then the extreme values of fmod(x,y) will be
    *  taken in that region, so we can find them by inspection.  (This
    *  includes all cases in which yu is infinite.)
    *
    *  Assume that the box lies entirely on or below the graph of y = |x|.
    *  As fmod(x,y) = -fmod(-x,y), we can map the box to the first quadrant.
    *  Letting m denote the largest integer such that m <= xu/yu, we have
    *  three possibilities:
    *
    *  1) if xl/yu < m, then the line y = x/m passes through the top of
    *  the box, and fmod(x,y) varies over the interval [0,yu);
    *
    *  2) otherwise, if xu/yl >= m+1, then the line y = x/(m+1) passes
    *  through the right edge of the box, and fmod(x,y) varies over the
    *  interval [0,xu/(m+1));
    *
    *  3) otherwise, the entire box lies between the lines y = x/m and
    *  y = x/(m+1), and fmod(x,y) varies over the interval [fmod(xl,yu),
    *  fmod(xu,yl)].
    *
    *  Several points to note:
    *
    *  If xu-xl >= yu, then we must be in case 1, so we need not compute m.
    *
    *  If xl == xu, yl != yu, and m >= 2^53, then xu/(m+1) > nextafter(yu,0)
    *  so we are in case 2 and the nearest representable upper bound for
    *  fmod(x,y) is yu.  In this case, the result is inexact, as it may be
    *  whenever we are in case 2.
    *
    *  Notes:
    *
    *  1. If any argument endpoint is NaN, we return a result with both
    *     endpoints NaN.
    *
    *  2. If the second argument contains the zero then
    *	we return [-inf, +inf]
    *
    *  3. If either argument is [+0,+0], [-0,-0], [+inf,+inf], or [-inf,-inf],
    *     the result is undefined (i.e., it's whatever you get).
    *	Note : now intervals [+0,+0], [-0,-0] convert to [-0,+0]
    */
    public Intervalf mod(Intervalf y)
    {
	float		m, m1, tl, tu, xl, xu, yl, yu;
	int		hxu,  hyl, hyu;
	float		l, u;
	/* make sure the intervals are valid */
	if (isEmpty() || y.isEmpty())
	{
		this.inf = Float.NaN;
		this.sup = Float.NaN;
		return this;
	}
	/* copy the arguments and get the high order parts */
	/* correction of sign of arguments */
	xl = this.inf == zero ? -zero : this.inf;
	xu = this.sup == zero ? +zero : this.sup;
	hxu = Float.floatToIntBits(xu);
	/* correction of sign of arguments */
	yl = y.inf == zero ? -zero : y.inf;
	yu = y.sup == zero ? +zero : y.sup;
	hyl = Float.floatToIntBits(yl);
	hyu = Float.floatToIntBits(yu);
	/* 0 in y therefore by IAS mod(x, y) = [-inf, +inf] */
	if (hyl <= 0 && hyu >= 0)
	{
		this.inf = Float.NEGATIVE_INFINITY;
		this.sup = Float.POSITIVE_INFINITY;
		return this;
	}
	/* reduce to nonnegative y */
	if (hyu < 0)
	{
		m = yl;
		yl = -yu;
		yu = -m;
		hyu = hyl & ~0x80000000;
	}
	/* check for trivial cases */
	if (xu > -yu && xl < yu)
	{
		/* box contains part of the region above the graph of y = |x| */
		l = ((xl <= -yu)? -yu : xl);
		u = ((xu >= yu)? yu : xu);
		if (yl <= xu || yl <= -xl)
		{
			/* box contains part of the graph of y = |x| */
			if (l >= zero)
				l = -zero;
			if (u <= zero)
				u = zero;
		}
		this.inf = l;
		this.sup = u;
		return this;
	}
	/* map to first quadrant */
	if (hxu < 0)
	{
		m = xl;
		xl = -xu;
		xu = -m;
	}
	/* check whether x spans an entire period */
	m = Arith.subnegative(xu, xl);
	if (m >= yu)
	{
		tl = -zero;
		tu = yu;
		/* done */
		if (hxu < 0)
		{
			l = -tu;
			u = -tl;
		}
		else
		{
			l = tl;
			u = tu;
		}
		this.inf = l;
		this.sup = u;
		return this;
	}
	/* check for degenerate x */
	if (xl == xu)
	{
		/* if y is degenerate, use the point fmod function */
		if (yl == yu)
		{
			tl = tu = Arith.fmodf(xu, yu);
			if (tl == zero)
				tl = -zero;
			/* done */
			if (hxu < 0)
			{
				l = -tu;
				u = -tl;
			}
			else
			{
				l = tl;
				u = tu;
			}
			this.inf = l;
			this.sup = u;
			return this;
		}
		/* if x is huge relative to y, we are trivially in case 2 */
		m = Arith.divnegative(xu, yu);
		if (m >= two24)
		{
			tl = -zero;
			tu = yu;
			/* done */
			if (hxu < 0)
			{
				l = -tu;
				u = -tl;
			}
			else
			{
				l = tl;
				u = tu;
			}
			this.inf = l;
			this.sup = u;
			return this;
		}
	}
	else
		m = Arith.divnegative(xu, yu);
	/* now m < 2^24; chop to an integer */
	m = Arith.aintf(m);
	/* check for case 1 */
	m1 = Arith.divnegative(xl, yu);
	if (m1 < m)
	{
		tl = -zero;
		tu = yu;
		/* done */
		if (hxu < 0)
		{
			l = -tu;
			u = -tl;
		}
		else
		{
			l = tl;
			u = tu;
		}
		this.inf = l;
		this.sup = u;
		return this;
	}
	/* check for case 2 */
	m1 = Arith.divnegative(xu, yl);
	if (m1 >= m + one)
	{
		tl = -zero;
		tu = Arith.divpositive(xu, (m + one));
		/* done */
		if (hxu < 0)
		{
			l = -tu;
			u = -tl;
		}
		else
		{
			l = tl;
			u = tu;
		}
		this.inf = l;
		this.sup = u;
		return this;
	}
	/* case 3: compute remainders at opposite corners of the box */
	tl = (float)((double)xl - (double)m * (double)yu);
	tu = (float)((double)xu - (double)m * (double)yl);
	/* done */
	if (hxu < 0)
	{
		l = -tu;
		u = -tl;
	}
	else
	{
		l = tl;
		u = tu;
	}
	this.inf = l;
	this.sup = u;
	return this;
    }

    /*
	Based on interval fabs from Sun FP group
    */
    public Intervalf abs()
    {
	int		hl, hu;
	float		l, u;
	/* make sure the interval is valid */
	if (isEmpty())
		return this;
	/* get the high order parts of the endpoints */
	hl = Float.floatToIntBits(this.inf);
	hu = Float.floatToIntBits(this.sup);
	if (hl < 0)
	{
		if (hu < 0)
		{
			l = Arith.fabsf(this.sup);
			u = Arith.fabsf(this.inf);
		}
		else
		{
			/* interval definitely contains zero */
			l = -zero;
			u = Arith.fabsf(this.inf);
			if (this.sup > u)
				u = this.sup;
		}
	}
	else
	{
		l = this.inf;
		u = Arith.fabsf(this.sup); /* in case u is -0 */
	}
	this.inf = l;
	this.sup = u;
	return this;
    }

    /*
	Based on interval cos from Sun FP group
    */
    public Intervalf cos()
    {
	float		xl, xu, l, u, ol, ou;
	double		o, vl, vu;
	double[]	rl, ru;
	int		n, nl, nu;
	int		hl, hu, il, iu, imax;
	boolean		isinf, issup;
	int		ml, mu, sl, su;
	if (isEmpty())
		return this;
	/* copy the arguments and get the high order parts */
	xl = this.inf;
	xu = this.sup;
	vl = (double)xl;
	vu = (double)xu;
	isinf = false;
	issup = false;
	/* cos(-x) = cos(x) */
	if (xu < zero)
	{
		l = -xl;
		xl = -xu;
		xu = l;
	}
	else
	{
		if (xl <= zero)
		{
			issup = true;
			if (-xl > xu)
				xu = -xl;
			xl = zero;
		}
	}
	l = -one;
	u = +one;
	hl = Float.floatToIntBits(xl);
	hu = Float.floatToIntBits(xu);
	il = hl & ~0x80000000;
	iu = hu & ~0x80000000;
	/* now xl >= zero */
	/* check for small endpoints */
	imax = ((il > iu)? il : iu);
	/* both endpoints are smaller than pi/2 */
	/* imax <= 1.5707960128784180 < pi/2 */
	if (imax <= 0x3fc90fd8)
	{
		/* both endpoints are smaller than pi/4 */
		/* imax <= 0.7853980064392090 < pi/4 */
		if (imax <= 0x3f490fd8)
		{
			/* revers endpoints so that cos(x) = [cos(xu),cos(xl)] */
			l = Arith.cosnegative(xu);
			if (! issup)
				u = Arith.cospositive(xl);
			this.inf = l;
			this.sup = u;
			return this;
		}
		/* revers endpoints so that cos(x) = [cos(xu),cos(xl)] */
		rl = new double[2];
		rl[0] = xu;
		rl[1] = zero;
		nl = Arith.sunmath_reduce(rl);
		l = Arith.cosnegativef(rl[0], rl[1], nl);
		if (! issup)
		{
			rl[0] = xl;
			rl[1] = zero;
			nl = Arith.sunmath_reduce(rl);
			u = Arith.cospositivef(rl[0], rl[1], nl);
		}
		this.inf = l;
		this.sup = u;
		return this;
	}
	/* check for large endpoints */
	if (imax > 0x5b000000)
	{
		if (xl != xu || imax >= 0x7f800000)
		{
			this.inf = l;
			this.sup = u;
			return this;
		}
	}
	o = vu - vl;
	if (o >= twopi)
	{
		if (o > twopi)
		{
			this.inf = l;
			this.sup = u;
			return this;
		}
		if (Arith.subpositive(vu, vl) >= twopi)
		{
			this.inf = l;
			this.sup = u;
			return this;
		}
	}
	rl = new double[2];
	rl[0] = xl;
	rl[1] = zero;
	nl = Arith.sunmath_reduce(rl);
	if (rl[0] < zero)
		ml = 7;
	else
		if (xl == zero)
			ml = 0;
		else
			ml = 1;
	ru = new double[2];
	ru[0] = xu;
	ru[1] = zero;
	nu = Arith.sunmath_reduce(ru);
	if (ru[0] < zero)
		mu = 7;
	else
		if (xu == zero)
			mu = 0;
		else
			mu = 1;
	sl = ml + (nl << 1);
	su = mu + (nu << 1);
	sl &= 0x7;
	su &= 0x7;
	if (sl == su)
	{
		if ((nl != nu && ml == mu) || (nl == nu && ml != mu))
		{
			this.inf = l;
			this.sup = u;
			return this;
		}
		if (sl == 0)
			issup = true;
		else
			if (sl == 4)
				isinf = true;
		if (isinf && issup)
		{
			this.inf = l;
			this.sup = u;
			return this;
		}
	}
	else
	{
		for (;;)
		{
			if (sl == 0)
				issup = true;
			else
				if (sl == 4)
					isinf = true;
			if (isinf && issup)
			{
				this.inf = l;
				this.sup = u;
				return this;
			}
			if (sl == su)
				break;
			sl++;
			sl &= 0x7;
		}
	}
	if (isinf || issup)
	{
		if (isinf)
		{
			ol = Arith.cospositivef(rl[0], rl[1], nl);
			ou = Arith.cospositivef(ru[0], ru[1], nu);
			if (ol > ou)
				u = ol;
			else
				u = ou;
		}
		else
		{
			ol = Arith.cosnegativef(rl[0], rl[1], nl);
			ou = Arith.cosnegativef(ru[0], ru[1], nu);
			if (ol < ou)
				l = ol;
			else
				l = ou;
		}
	}
	else
	{
		if (sl <= 4)
		{
			l = Arith.cosnegativef(ru[0], ru[1], nu);
			u = Arith.cospositivef(rl[0], rl[1], nl);
		}
		else
		{
			l = Arith.cosnegativef(rl[0], rl[1], nl);
			u = Arith.cospositivef(ru[0], ru[1], nu);
		}
	}
	this.inf = l;
	this.sup = u;
	return this;
    }

    /*
	Based on interval sin from Sun FP group
    */
    public Intervalf sin()
    {
	float		xl, xu, l, u, ol, ou;
	double		o, vl, vu;
	double[]	rl, ru;
	int		n, nl, nu;
	int		hl, hu, il, iu, imax;
	boolean		isinf, issup;
	int		ml, mu, sl, su;
	if (isEmpty())
		return this;
	/* copy the arguments and get the high order parts */
	l = -one;
	u = +one;
	xl = this.inf;
	xu = this.sup;
	isinf = false;
	issup = false;
	hl = Float.floatToIntBits(xl);
	hu = Float.floatToIntBits(xu);
	il = hl & ~0x80000000;
	iu = hu & ~0x80000000;
	/* check for small endpoints */
	imax = ((il > iu)? il : iu);
	/* both endpoints are smaller than pi/2 */
	/* imax <= 1.5707960128784180 < pi/2 */
	if (imax <= 0x3fc90fd8)
	{
		/* both endpoints are smaller than pi/4 */
		/* imax <= 0.7853980064392090 < pi/4 */
		if (imax <= 0x3f490fd8)
		{
			this.inf = Arith.sinnegative(xl);
			this.sup = Arith.sinpositive(xu);
			return this;
		}
		rl = new double[2];
		rl[0] = xl;
		rl[1] = zero;
		nl = Arith.sunmath_reduce(rl);
		this.inf = Arith.sinnegativef(rl[0], rl[1], nl);
		rl[0] = xu;
		rl[1] = zero;
		nl = Arith.sunmath_reduce(rl);
		this.sup = Arith.sinpositivef(rl[0], rl[1], nl);
		return this;
	}
	/* check for large endpoints */
	if (imax > 0x5b000000)
	{
		if (xl != xu || imax >= 0x7f800000)
		{
			this.inf = l;
			this.sup = u;
			return this;
		}
	}
	vl = (double)xl;
	vu = (double)xu;
	o = vu - vl;
	if (o >= twopi)
	{
		if (o > twopi)
		{
			this.inf = l;
			this.sup = u;
			return this;
		}
		if (Arith.subpositive(vu, vl) >= twopi)
		{
			this.inf = l;
			this.sup = u;
			return this;
		}
	}
	rl = new double[2];
	rl[0] = xl;
	rl[1] = zero;
	nl = Arith.sunmath_reduce(rl);
	if (rl[0] < zero)
		ml = 7;
	else
		if (xl == zero)
			ml = 0;
		else
			ml = 1;
	ru = new double[2];
	ru[0] = xu;
	ru[1] = zero;
	nu = Arith.sunmath_reduce(ru);
	if (ru[0] < zero)
		mu = 7;
	else
		if (xu == zero)
			mu = 0;
		else
			mu = 1;
	sl = ml + (nl << 1);
	su = mu + (nu << 1);
	sl &= 0x7;
	su &= 0x7;
	if (sl == su)
	{
		if ((nl != nu && ml == mu) || (nl == nu && ml != mu))
		{
			this.inf = l;
			this.sup = u;
			return this;
		}
		if (sl == 2)
			issup = true;
		else
			if (sl == 6)
				isinf = true;
		if (isinf && issup)
		{
			this.inf = l;
			this.sup = u;
			return this;
		}
	}
	else
	{
		for (;;)
		{
			if (sl == 2)
				issup = true;
			else
				if (sl == 6)
					isinf = true;
			if (isinf && issup)
			{
				this.inf = l;
				this.sup = u;
				return this;
			}
			if (sl == su)
				break;
			sl++;
			sl &= 0x7;
		}
	}
	if (isinf || issup)
	{
		if (isinf)
		{
			ol = Arith.sinpositivef(rl[0], rl[1], nl);
			ou = Arith.sinpositivef(ru[0], ru[1], nu);
			if (ol > ou)
				u = ol;
			else
				u = ou;
		}
		else
		{
			ol = Arith.sinnegativef(rl[0], rl[1], nl);
			ou = Arith.sinnegativef(ru[0], ru[1], nu);
			if (ol < ou)
				l = ol;
			else
				l = ou;
		}
	}
	else
	{
		if (sl >= 2 && sl <= 6)
		{
			l = Arith.sinnegativef(ru[0], ru[1], nu);
			u = Arith.sinpositivef(rl[0], rl[1], nl);
		}
		else
		{
			l = Arith.sinnegativef(rl[0], rl[1], nl);
			u = Arith.sinpositivef(ru[0], ru[1], nu);
		}
	}
	this.inf = l;
	this.sup = u;
	return this;
    }

    /*
	Based on interval tan from Sun FP group
    */
    public Intervalf tan()
    {
	float	xl, xu, l, u;
	double	o, vl, vu;
	int	il, iu, imax;
	if (isEmpty())
		return this;
	/* copy the arguments and get the high order parts */
	l = Float.NEGATIVE_INFINITY;
	u = Float.POSITIVE_INFINITY;
	xl = this.inf;
	xu = this.sup;
	il = Float.floatToIntBits(xl) & ~0x80000000;
	iu = Float.floatToIntBits(xu) & ~0x80000000;
	/* check for small endpoints */
	imax = ((il > iu)? il : iu);
	/* both endpoints are smaller than pi/2 */
	/* imax <= 1.5707960128784180 < pi/2 */
	if (imax <= 0x3ff921fb)
	{
		this.inf = Arith.tannegative(xl);
		this.sup = Arith.tanpositive(xu);
		return this;
	}
	/* check for large endpoints */
	if (imax > 0x5b000000)
	{
		if (xl != xu || imax >= 0x7f800000)
		{
			this.inf = l;
			this.sup = u;
			return this;
		}
	}
	vl = (double)xl;
	vu = (double)xu;
	o = vu - vl;
	if (o >= pi)
	{
		if (o > pi)
		{
			this.inf = l;
			this.sup = u;
			return this;
		}
		if (Arith.subpositive(vu, vl) >= pi)
		{
			this.inf = l;
			this.sup = u;
			return this;
		}
	}
	l = Arith.tannegative(xl);
	u = Arith.tanpositive(xu);
	if (l >= u)
	{
		this.inf = Float.NEGATIVE_INFINITY;
		this.sup = Float.POSITIVE_INFINITY;
		return this;
	}
	this.inf = l;
	this.sup = u;
	return this;
    }

    /*
	Based on interval acos from Sun FP group
    */
    public Intervalf acos() {
	float xl, xu;
	if (isEmpty())
		return this;
	if (this.inf > +one || this.sup < -one) {
		this.inf = Float.NaN;
		this.sup = Float.NaN;
		return this;
	}
	xl = this.inf;
	xu = this.sup;
	if (xl < -one)
		xl = -one;
	if (xu > +one)
		xu = +one;
	this.inf = Arith.acosnegative(xu);
	this.sup = Arith.acospositive(xl);
	return this;
    }

    /*
	Based on interval asin from Sun FP group
    */
    public Intervalf asin() {
	float xl, xu;
	if (isEmpty())
		return this;
	if (this.inf > +one || this.sup < -one) {
		this.inf = Float.NaN;
		this.sup = Float.NaN;
		return this;
	}
	xl = this.inf;
	xu = this.sup;
	if (xl < -one)
		xl = -one;
	if (xu > +one)
		xu = +one;
	this.inf = Arith.asinnegative(xl);
	this.sup = Arith.asinpositive(xu);
	return this;
    }

    /*
	Based on interval atan from Sun FP group
    */
    public Intervalf atan() {
	if (isEmpty())
		return this;
	this.inf = Arith.atannegative(this.inf);
	this.sup = Arith.atanpositive(this.sup);
	return this;
    }

    /*
	Based on interval atan2 from Sun FP group
    */
    public Intervalf atan2(Intervalf x)
    {
	float		al, au, xl, xu, yl, yu;
	int			hxl, hxu,  hyl, hyu;
	float		l, u;
	double		t, tal, tau;
	int		angex;
	/* make sure the intervals are valid */
	if (isEmpty() || x.isEmpty())
	{
		this.inf = Float.NaN;
		this.sup = Float.NaN;
		return this;
	}
	/* copy the arguments and get the high order parts */
	/* correction of sign of arguments */
	yl = this.inf == zero ? -zero : this.inf;
	yu = this.sup == zero ? +zero : this.sup;
	hyl = Float.floatToIntBits(yl);
	hyu = Float.floatToIntBits(yu);
	/* correction of sign of arguments */
	xl = x.inf == zero ? -zero : x.inf;
	xu = x.sup == zero ? +zero : x.sup;
	hxl = Float.floatToIntBits(xl);
	hxu = Float.floatToIntBits(xu);
	/* figure out which vertices of the box give the extreme bounds */
	angex = 0;
	if ((hyl ^ hyu) < 0)
	{
		/* box straddles x axis */
		if (hxl < 0)
		{
			/* include right zero in interval [-a,0], a >= 0 */
			if ( hxu >= 0 || AngularIval == 0 ) 
			{
				/* box contains part of the branch cut,
				   if not AngularIval or box contains
				   (0,0), return [-P,P] and raise inexact */
				t = pihi;
				t += pilo;
				u = Arith.doubleTofloatpositive(pihi);
				l = -u;
				this.inf = l;
				this.sup = u;
				return this;
			}
			else
			{
				xl = xu;
				hyl ^= 0x80000000;
				/* see IAS subsection 18.6 the table at the end */
				if (-yl < yu)
				{
					angex = 1;
					hyu |= 0x80000000;
					al = yl;
					yl = yu;
					yu = al;
				}
				else
				{
					if (-yl == yu)
					{
						if (yl != zero) angex = 2;
						yl = yu;
					}
					else
					{
						angex = 3;
						hyu |= 0x80000000;
						al = yl;
						yl = yu;
						yu = al;
					}
				}
			}
		}
		else
		{
			/* box is in the right half plane */
			xu = xl;
		}
	}
	else if (hyu < 0)
	{
		/* box is in the lower half plane */
		if (hxl < 0)
			yl = this.sup;
		if (hxu < 0)
			yu = this.inf;
	}
	else
	{
		/* box is in the upper half plane */
		xl = x.sup;
		xu = x.inf;
		if (hxl < 0)
			yu = this.inf;
		if (hxu < 0)
			yl = this.sup;
	}
	l = Arith.atan2negative(yl, xl);
	u = Arith.atan2positive(yu, xu);
	/* fixed the result to cross over P according to angex */
	if (angex != 0) 
	{
		tal = 2 * pihi;
		tau = 2 * pilo;
		tal = Arith.addpositive(tal, tau);
		al = Arith.doubleTofloatpositive(tal);
		switch (angex)
		{
			case 1 :
				u = Arith.addpositive(u, al);
				break;
			case 2 :
				u = Arith.subpositive(al, l);
				break;
			case 3 :
				l = Arith.subnegative(l, al);
		}
	}
	this.inf = l;
	this.sup = u;
	return this;
    }

    /*
	Based on interval cosh from Sun FP group
    */
    public Intervalf cosh() {
	float l, u;
	if (isEmpty())
		return this;
	if (this.inf > zero) {
		this.inf = Arith.coshnegative(this.inf);
		this.sup = Arith.coshpositive(this.sup);
		return this;
	}
	if (this.sup < zero) {
		l = Arith.coshnegative(this.sup);
		u = Arith.coshpositive(this.inf);
		this.inf = l;
		this.sup = u;
		return this;
	}
	l = one;
	if (Arith.fabsf(this.inf) > this.sup)
		u = Arith.coshpositive(this.inf);
	else
		u = Arith.coshpositive(this.sup);
	this.inf = l;
	this.sup = u;
	return this;
    }

    /*
	Based on interval sinh from Sun FP group
    */
    public Intervalf sinh() {
	if (isEmpty())
		return this;
	this.inf = Arith.sinhnegative(this.inf);
	this.sup = Arith.sinhpositive(this.sup);
	return this;
    }

    /*
	Based on interval tanh from Sun FP group
    */
    public Intervalf tanh() {
	if (isEmpty())
		return this;
	this.inf = Arith.tanhnegative(this.inf);
	this.sup = Arith.tanhpositive(this.sup);
	return this;
    }

    /*
	Based on interval pow from Sun Alexander Semenov and Bill Walster
    */
    /*
	the algorithm from Alexander Semenov
    */
    private Intervalf pow(int y) {
	float	c, d;
	double	l, u;
	double	a, b;
	if (isEmpty())
		return this;
	if (y == zero)
	{
		this.inf = one;
		this.sup = one;
		return this;
	}
	a = (double)y;
	if (this.inf > zero)				/* x > 0 */
	{
		if (y > zero)				/* y > 0 */
		{
			l = Arith.powernegative((double)this.inf, a);
			u = Arith.powerpositive((double)this.sup, a);
		}
		else					/* y < 0 */
		{
			l = Arith.powernegative((double)this.sup, a);
			u = Arith.powerpositive((double)this.inf, a);
		}
	}
	else						/* x < 0 or 0 /in x */
	{
		if (this.sup < zero)			/* x < 0 */
		{
			if (
				y > 0 && (! Arith.odd(y))/* y > 0 and y is even */
				||
				y < 0 && (Arith.odd(y))/* y < 0 and y is odd */
			)
			{
				l = Arith.powernegative((double)this.sup, a);
				u = Arith.powerpositive((double)this.inf, a);
			}
			else
			{
				l = Arith.powernegative((double)this.inf, a);
				u = Arith.powerpositive((double)this.sup, a);
			}
		}
		else					/* xl <= 0 <= xu */
		{
			if (y < 0)
			{
				if (! Arith.odd(y))		/* y is even */
				{
					if (-this.inf > this.sup)
						b = (double)-this.inf;
					else
						b = (double)this.sup;
					l = Arith.powernegative(b, a);
					u = Float.POSITIVE_INFINITY;
				}
				else
				{
					this.inf = Float.NEGATIVE_INFINITY;
					this.sup = Float.POSITIVE_INFINITY;
					return this;
				}
			}
			else
			{
				if (! Arith.odd(y))		/* y is even */
				{
					l = -zero;
					if (-this.inf > this.sup)
						b = (double)-this.inf;
					else
						b = (double)this.sup;
					u = Arith.powerpositive(b, a);
				}
				else			/* y is odd */
				{
					l = Arith.powernegative((double)this.inf, a);
					u = Arith.powerpositive((double)this.sup, a);
				}
			}
		}
	}
	c = Arith.doubleTofloatnegative(l);
	d = Arith.doubleTofloatpositive(u);
	this.inf = c;
	this.sup = d;
	return this;
    }

    /*
	Based on interval pow from Sun Alexander Semenov and Bill Walster
    */
    public Intervalf pow(long x) {
	int n;
	if (x >= Integer.MIN_VALUE && x <= Integer.MAX_VALUE)
		n = (int)x;
	else
		n = Arith.longToint(x);
	return(pow(n));
    }

    /*
	Based on interval pow from Sun Alexander Semenov and Bill Walster
    */
    /*
	the algorithm from Alexander Semenov
    */
    private Intervalf powloc(float y) {
	float l, u;
	if (y == zero)
	{
		if (this.inf > zero)				/* x > 0 */
		{
			if (Float.isInfinite(this.sup))		/* x == [a, +inf] */
			{
				this.inf = -zero;
				this.sup = Float.POSITIVE_INFINITY;
				return this;
			}
			else				/* xu < +inf */
			{
				this.inf = one;
				this.sup = one;
				return this;
			}
		}
		else				/* x < 0 or xl <= 0 <= xu */
		{
			if (this.sup < zero)		/* x < 0 */
			{
				this.inf = Float.NaN;
				this.sup = Float.NaN;
				return this;
			}
			else			/* xl <= 0 <= xu : 0^0 */
			{
				this.inf = -zero;
				this.sup = Float.POSITIVE_INFINITY;
				return this;
			}
		}
	}
	if (this.inf > zero)
	{
		if (Float.isInfinite(y) && this.inf <= one && this.sup >= one)
		{
			this.inf = -zero;
			this.sup = Float.POSITIVE_INFINITY;
			return this;
		}
		if (y > zero)
		{
			l = Arith.powernegative(this.inf, y);
			u = Arith.powerpositive(this.sup, y);
		}
		else				/* y < 0 */
		{
			l = Arith.powernegative(this.sup, y);
			u = Arith.powerpositive(this.inf, y);
		}
	}
	else					/* x < 0 or 0 /in x */
	{
		if (this.sup < zero)			/* x < 0 */
		{
			this.inf = Float.NaN;
			this.sup = Float.NaN;
			return this;
		}
		else				/* 0 /in x */
		{
			if (y > zero)
			{
				l = -zero;
				if (Float.isInfinite(y) && this.sup >= one)
				{
					u = Float.POSITIVE_INFINITY;
					this.inf = l;
					this.sup = u;
					return this;
				}
				u = Arith.powerpositive(this.sup, y);
			}
			else			/* y < 0 */
			{
				l = Arith.powernegative(this.sup, y);
				u = Float.POSITIVE_INFINITY;
			}
		}
	}
	this.inf = l;
	this.sup = u;
	return this;
    }

    /*
	Based on interval pow from Sun Alexander Semenov and Bill Walster
    */
    /*
	the algorithm from Alexander Semenov
    */
    private Intervalf BA(Intervalf y) {
	float al, au;
	float l, u;
	/* this.inf > 0 */
	if (y.inf <= zero && y.sup >= zero)
		if (Float.isInfinite(this.sup))
		{
			this.inf = -zero;
			this.sup = Float.POSITIVE_INFINITY;
			return this;
		}
	if (this.inf <= one && this.sup >= one)
		if (Float.isInfinite(y.inf) || Float.isInfinite(y.sup))
		{
			this.inf = -zero;
			this.sup = Float.POSITIVE_INFINITY;
			return this;
		}
	if (this.sup <= one)
	{
							/* x > 0 & x <= one */
		if (y.sup <= zero)
		{
							/* y <= 0 */
			l = Arith.powernegative(this.sup, y.sup);
			u = Arith.powerpositive(this.inf, y.inf);
		}
		else					/* not y <= 0 */
		{
			if (y.inf >= zero)
			{
							/* y >= 0 */
				l = Arith.powernegative(this.inf, y.sup);
				u = Arith.powerpositive(this.sup, y.inf);
			}
			else			/* y.inf < 0 and y.sup > 0 */
			{
				l = Arith.powernegative(this.inf, y.sup);
				u = Arith.powerpositive(this.inf, y.inf);
			}
		}
	}
	else
	{
		if (this.inf >= one)
		{
							/* x > 0 & x >= one */
			if (y.sup <= zero)
			{
							/* y <= 0 */
				l = Arith.powernegative(this.sup, y.inf);
				u = Arith.powerpositive(this.inf, y.sup);
			}
			else				/* not y <= 0 */
			{
				if (y.inf >= zero)
				{
							/* y >= 0 */
					l = Arith.powernegative(this.inf, y.inf);
					u = Arith.powerpositive(this.sup, y.sup);
				}
				else		/* y.inf < 0 and y.sup > 0 */
				{
					l = Arith.powernegative(this.sup, y.inf);
					u = Arith.powerpositive(this.sup, y.sup);
				}
			}
		}
		else					/* 1 in x */
		{
			if (y.sup <= zero)
			{
							/* y <= 0 */
				l = Arith.powernegative(this.sup, y.inf);
				u = Arith.powerpositive(this.inf, y.inf);
			}
			else				/* not y <= 0 */
			{
				if (y.inf >= zero)
				{
							/* y >= 0 */
					l = Arith.powernegative(this.inf, y.sup);
					u = Arith.powerpositive(this.sup, y.sup);
				}
				else			/* 0 in y */
				{
					al = Arith.powernegative(this.inf, y.sup);
					l = Arith.powernegative(this.sup, y.inf);
					au = Arith.powerpositive(this.sup, y.sup);
					u = Arith.powerpositive(this.inf, y.inf);
					if (al < l)
						l = al;
					if (au > u)
						u = au;
				}
			}
		}
	}
	this.inf = l;
	this.sup = u;
	return this;
    }

    /*
	Based on interval pow from Sun Alexander Semenov and Bill Walster
    */
    /*
	the algorithm from Alexander Semenov
    */
    public Intervalf pow(Intervalf y) {
	float l, u;
	if (isEmpty() || y.isEmpty()) {
		this.inf = Float.NaN;
		this.sup = Float.NaN;
		return this;
	}
	/* y is degenerate */
	if (y.inf == y.sup)
		return(this.powloc(y.inf));
	/* y is not degenerate */
	/* x > 0 */
	if (this.inf > zero)
		return(this.BA(y));
	/* x < 0 */
	if (this.sup < zero) {
		this.inf = Float.NaN;
		this.sup = Float.NaN;
		return this;
	}
	/* zero in x */
	/* zero in zero degree */
	if (y.inf <= zero && y.sup >= zero) {
		this.inf = -zero;
		this.sup = Float.POSITIVE_INFINITY;
		return this;
	}
	/* y < 0 or y > 0 */
	if ((Float.isInfinite(y.inf) || Float.isInfinite(y.sup)) && this.sup >= one) {
		this.inf = -zero;
		this.sup = Float.POSITIVE_INFINITY;
		return this;
	}
	/* x = x /intersection [0, +inf] */
	if (y.inf > 0) {
		l = -zero;
		if (this.sup < one)
			u = Arith.powerpositive(this.sup, y.inf);
		else
			u = Arith.powerpositive(this.sup, y.sup);
	}
	else
	{
		if (this.sup < one)
			l = Arith.powernegative(this.sup, y.sup);
		else
			l = Arith.powernegative(this.sup, y.inf);
		u = Float.POSITIVE_INFINITY;
	}
	this.inf = l;
	this.sup = u;
	return this;
    }

    /* Java Interval Input Output */

    public Intervalf inToIntervalf(InputStream in) throws IOException
    {
	float[]	l, u;
	int	retval;
	l = new float[1];
	u = new float[1];
	retval = Intervalio.inTointervalf(in, l, u);
	this.inf = l[0];
	this.sup = u[0];
	return this;
    }

    private Intervalf stringToIntervalf(String x) {
	float[]	l, u;
	int	retval;
	l = new float[1];
	u = new float[1];
	retval = Intervalio.stringTointervalf(x, l, u);
	this.inf = l[0];
	this.sup = u[0];
	return this;
    }

    private Intervalf bufToIntervalf(char[] x) {
	float[]	l, u;
	int	retval;
	l = new float[1];
	u = new float[1];
	retval = Intervalio.bufTointervalf(x, l, u);
	this.inf = l[0];
	this.sup = u[0];
	return this;
    }

    public String toOctalString() {
	int	i;
	String	l, u;
	l = Integer.toOctalString(Float.floatToIntBits(this.inf));
	u = Integer.toOctalString(Float.floatToIntBits(this.sup));
	for(i = l.length(); i < 11; i++)
		l = "0" + l;
	for(i = u.length(); i < 11; i++)
		u = "0" + u;
	return "[" + l + "," + u + "]";
    }  

    public String toHexString() {
	int	i;
	String	l, u;
	l = Integer.toHexString(Float.floatToIntBits(this.inf));
	u = Integer.toHexString(Float.floatToIntBits(this.sup));
	for(i = l.length(); i < 8; i++)
		l = "0" + l;
	for(i = u.length(); i < 8; i++)
		u = "0" + u;
	return "[" + l + "," + u + "]";
    }  

    /*
	Note : the parameter "d" should be <= 7 else containment lost
    */
    public String toYstring(int w, int d, int e) {
	String str;
	str = Intervalio.intervalfTostring
		(this.inf, this.sup, w, d, e, SINGLE_NUMBER_FORMAT);
	return(str);
    }

    public String toYstring() {
	return toYstring(0, 0, 0);
    }

    public String toVEstring(int w, int d, int e) {
	String str;
	str = Intervalio.intervalfTostring
		(this.inf, this.sup, w, d, e, VE_NUMBER_FORMAT);
	return(str);
    }

    public String toVEstring() {
	return toVEstring(0, 0, 0);
    }

    public String toVENstring(int w, int d, int e) {
	return toVEstring(w, d, e);
    }

    public String toVENstring() {
	return toVENstring(0, 0, 0);
    }

    public String toVESstring(int w, int d, int e) {
	return toVEstring(w, d, e);
    }

    public String toVESstring() {
	return toVESstring(0, 0, 0);
    }

    public String toVFstring(int w, int d, int e) {
	return toVEstring(w, d, e);
    }

    public String toVFstring() {
	return toVFstring(0, 0, 0);
    }

    public String toVGstring(int w, int d, int e) {
	String str;
	str = Intervalio.intervalfTostring
		(this.inf, this.sup, w, d, e, VG_NUMBER_FORMAT);
	return(str);
    }

    public String toVGstring() {
	return toVGstring(0, 0, 0);
    }

    public String toSingleNumberString(int w, int d, int e) {
	return toYstring(w, d, e);
    }

    public String toSingleNumberString() {
	return toSingleNumberString(0, 0, 0);
    }

    public String toString() {
	return(toVEstring());
    }

    /* Java Random interval */

    public Intervalf nextRandomIval(Random o) {
	float l, u;

	l = o.nextFloat();
	u = o.nextFloat();
	if (l == one)
		l = Arith.prevf(l);
	if (u == one)
		u = Arith.prevf(u);
	if (l <= u) {
		this.inf = l;
		this.sup = u;
	} else {
		this.inf = u;
		this.sup = l;
	}
	return this;
    }
}
