/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Interval.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */

package com.sun.electric.tool.simulation.interval;

/**
 * Class for representation of intervals X = [a, b]. a and b are double 
 * precision floation point numbers with a <= b. 
 * 
 * Closed ("extended") interval system is emplemented, which is the extension
 * of regular interval system.
 *
 * The elements of the regular interval system (IR) are intervals with regular 
 * IEEE floating point number bounds.
 *
 * In the closed ("extended") interval system (IR*) the set of regular 
 * intervals IR is extended by infinite intervals and the empty interval.
 * All arithmetic operations and mathematical functions are closed on IR*.
 * For details see the paper 
 * <a href="http://www.mscs.mu.edu/~globsol/Papers/spec.ps">
 * Interval Arithmetic Specification</a> by Chiriaev and Walster. The 
 * implementation is following this specification.  
 * Three special intervals are supported in the extended system:
 * <UL>
 * <LI> [ EMPTY ], represented as [ NaN, NaN ]
 * <LI> [ -INF ], represented as [ -INF, -MAX ]
 * <LI> [ +INF ], represented as [ MAX, +INF] 
 * </UL>
 * where MAX is the largest regular floating point number. All interval 
 * operations are guaranteed to produce results consistent with these 
 * representations.
 */
public class Interval
{
    /* Fields */

    private double inf;
    private double sup;

    /* Constants */

    /**
     * A constant holding 3/4*ulp(1.0). next(x) is round-to-nearest
	 * of (x + x*ULP_EPS) for positive normalized doble numbers.
     */
    private static final double ULP_EPS = 1.5/(1L << 53); // 0x1.8p-53;
    /**
     * A constant holding prev(1.0). prev(x) is round-to-nearest
	 * if (x*SCALE_DOWN) for positive normalized double numbers.
     */
    private static final double SCALE_DOWN = 1.0 - 1.0/(1L << 53); // 0x0.fffffffffffffp0;
    /**
     * A constant holding minimal positive normalized double number.
     */
    private static final double MIN_NORMAL = Double.MIN_VALUE*(1L << 52); // (0x1.0p-1022;
    /**
     * A constant holding ulp(Double.MAX_VALUE).
     */
    private static final double	MAX_ULP = Double.MAX_VALUE/(1L << 53)/SCALE_DOWN; // 0x1.0p971;
    /**
     * A constant holding range of long numbers exactly represented
	 * by double numbers. All longs in [ -EXACT_LONG, EXACT_LONG] are
	 * represented exactly. Some of other longs are not.
     */
	private static final long EXACT_LONG = 1L << 53;
    /**
     * A constant holding Long.MAX_VALUE rounded down.
     */
	private static final double INF_MAX_LONG = SCALE_DOWN*2*(1L << 62); // 0x1.fffffffffffffp62;

	// -----------------------------------------------------------------------
	// Constructors
	// -----------------------------------------------------------------------

	/**
	 * Constructs a point interval [0,0].
	 */
	public Interval() {
	}

	/**
	 * Constructs a point interval [x,x].
	 */
	public Interval(int x) {
		assign(x);
	}

	/**
	 * Constructs sharpest interval containing x.
	 */
	public Interval(long x) {
		assign(x);
	}

	/**
	 * Constructs a point interval.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> if x == +INF then [ +INF ] is constructed
	 * <LI> if x == -INF then [ -INF ] is constructed
	 * <LI> if x == NaN then the entire interval [ -INF,+INF ] is constructed
	 * </UL>
	 */
    public Interval(double x) {
		assign(x);
	}

	/**
	 * Constructs the interval [inf, sup]. 
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> if inf < sup then the entire interval [ -INF,+INF ] is constructed
	 * </UL>
	 *
	 * @param inf The infimum of the interval to be constructed.
	 * @param sup The supremum of the interval to be constructed.
	 */
	public Interval(int inf, int sup) {
		assign(inf, sup);
	}

	/**
	 * Constructs the interval [inf, sup]. 
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> if inf < sup then the entire interval [ -INF,+INF ] is constructed
	 * </UL>
	 *
	 * @param inf The infimum of the interval to be constructed.
	 * @param sup The supremum of the interval to be constructed.
	 */
	public Interval(long inf, long sup) {
		assign(inf, sup);
	}

	/**
	 * Constructs the interval [inf, sup]. 
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> if inf == sup == -INF the interval [ -INF ] is constructed
	 * <LI> if inf == sup == +INF the interval [ +INF ] is constructed
	 * <LI> if inf < sup then the entire interval [ -INF,+INF ] is constructed
	 * <LI> if inf == NaN or sup == NaN then the entire interval [ -INF,+INF ] is constructed
	 * </UL>
	 *
	 * @param inf The infimum of the interval to be constructed.
	 * @param sup The supremum of the interval to be constructed.
	 */
	public Interval(double inf, double sup) {
		assign(inf, sup);
	}

	/**
	 * Constructs the interval same as x.
	 */
	public Interval(Interval x) {
		assign(x);
	}

	/**
	 * Constructs the interval from string.
	 */
	public Interval(String s) {
		assign(s);
	}

	/**
	 * Constructs the interval from character array.
	 */
	public Interval(char[] b) {
		assign(b);
	}

	// -----------------------------------------------------------------------
	// Assigns
	// -----------------------------------------------------------------------

	/**
	 * Assigns a point interval [x,x].
	 */
	public Interval assign(int x) {
		inf = sup = (double)x;
		return this;
	}

	/**
	 * Assigns sharpest interval containing x.
	 */
	public Interval assign(long x) {
		double xd = (double)x;
		inf = sup = xd;
		if (Math.abs(x) > EXACT_LONG) {
			long xx = (long)xd;
			if (xx > x || x == Long.MAX_VALUE)
				inf = prev(xd);
			else if (xx < x)
				sup = next(xd);
		}
		return this;
	}

	/**
	 * Assigns a point interval.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> if x == +INF then [ +INF ] is constructed
	 * <LI> if x == -INF then [ -INF ] is constructed
	 * <LI> if x == NaN then the entire interval [ -INF,+INF ] is constructed
	 * </UL>
	 */
    public Interval assign(double x) {
		inf = sup = x;
		if (x == Double.POSITIVE_INFINITY)
			inf = Double.MAX_VALUE;
		else if (x == Double.NEGATIVE_INFINITY)
			sup = -Double.MAX_VALUE;
		else if (x != x)
			assignEntire();
		return this;
    }

	/**
	 * Assigns the interval [inf, sup]. 
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> if inf < sup then the entire interval [ -INF,+INF ] is constructed
	 * </UL>
	 *
	 * @param inf The infimum of the interval to be constructed.
	 * @param sup The supremum of the interval to be constructed.
	 */
	public Interval assign(int inf, int sup) {
		if (inf <= sup) {
			this.inf = (double)inf;
			this.sup = (double)sup;
		} else
			assignEntire();
		return this;
	}

	/**
	 * Assigns the interval [inf, sup]. 
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> if inf < sup then the the entire interval [ -INF,+INF ] is constructed
	 * </UL>
	 *
	 * @param inf The infimum of the interval to be constructed.
	 * @param sup The supremum of the interval to be constructed.
	 */
	public Interval assign(long inf, long sup) {
		if (inf <= sup) {
			this.inf = (double)inf;
			this.sup = (double)sup;
			if (inf < -EXACT_LONG || sup > EXACT_LONG) {
				if ((long)this.inf > inf || inf == Long.MAX_VALUE)
					this.inf = prev(this.inf);
				if ((long)this.sup < sup)
					this.sup = next(this.sup);
			}
		} else
			assignEntire();
		return this;
	}

	/**
	 * Assigns the interval [inf, sup]. 
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> if inf == sup == -INF the interval [ -INF ] is constructed
	 * <LI> if inf == sup == +INF the interval [ +INF ] is constructed
	 * <LI> if inf < sup then the entire interval [ -INF,+INF ] is constructed
	 * <LI> if inf == NaN or sup == NaN then the entire [ -INF,+INF ] is constructed
	 * </UL>
	 *
	 * @param inf The infimum of the interval to be constructed.
	 * @param sup The supremum of the interval to be constructed.
	 */
	public Interval assign(double inf, double sup) {
		if (inf <= sup) {
			this.inf = (inf == Double.POSITIVE_INFINITY ? Double.MAX_VALUE : inf);
			this.sup = (sup == Double.NEGATIVE_INFINITY ? -Double.MAX_VALUE : sup);
		} else
			assignEntire();
		return this;
	}

	/**
	 * Assigns interval same as x.:
	 */
	public Interval assign(Interval x) {
		this.inf = x.inf;
		this.sup = x.sup;
		return this;
	}

	/**
	 * Assigns the interval from string.
	 */
	public Interval assign(String s) {
		//		return stringToInterval(s);
		return assignEntire();
	}

	/**
	 * Assigns the interval from character array.
	 */
	public Interval assign(char[] b) {
		//		return bufToInterval(b);
		return assignEntire();
	}

	/**
     * Creates and returns a copy of this Interval.
	 */
	public Interval clon() {
		try {
			return (Interval)clone();
		}
		catch (CloneNotSupportedException e) {
			return new Interval(inf, sup);
		}
	}

	/**
	 * Assigns entire interval [ -INF, +INF ].
	 */
	private Interval assignEntire() {
		inf = Double.NEGATIVE_INFINITY;
		sup = Double.POSITIVE_INFINITY;
		return this;
	}

	/**
	 * Assigns empty interval [-EMPTY ].
	 */
	private Interval assignEmpty() {
		inf = sup = Double.NaN;
		return this;
	}

	// -----------------------------------------------------------------------
	// Interval bounds 
	// -----------------------------------------------------------------------

	/**
	 * Returns the infimum of this interval.
	 *
	 * Special cases:
	 * <ul><li> x.inf() == NaN for x == [ EMPTY ].
	 * <li> x.inf() == MAX_DOUBLE for x == [ +INF ].</ul> 
	 */
	public double inf() {
		return inf;
	}

	/**
	 * Returns the supremum of this interval.
	 *
	 * Special cases:
	 * <ul><li> x.sup() == NaN for x == [ EMPTY ].
	 * <li> x.sup() == MAX_DOUBLE for x == [ +INF ].</ul> 
	 */
	public double sup() {
		return sup;
	}

	// -----------------------------------------------------------------------
	// Info functions 
	// -----------------------------------------------------------------------

	/**
	 * Returns true iff this interval is a point interval, i.e. inf() == sup().
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.isPoint() == false for x == [ EMPTY ] 
	 * </UL> 
	 */
	public boolean isPoint() {
		return inf == sup;
	}

	/**
	 * Same as isPoint().
	 */
	public boolean isDegenerate() {
		return inf == sup;
	}

	/**
	 * Returns true if this == [ EMPTY ].
	 */
	public boolean isEmpty() {
		return inf != inf;
	}

	/**
	 * Return true if either x.inf() or x.sup() is infinite.
	 */
	public boolean isInfinite() {
		return inf == Double.NEGATIVE_INFINITY || sup == Double.POSITIVE_INFINITY;
	}

	/**
	 * Returns true iff this interval has an ulp accuracy of n, i.e. x.inf() 
	 * and x.sup() have a distance of at most n machine numbers.  
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.hasUlpAcc(n) == false for x == [ EMPTY ] or any infinite interval 
	 * </UL> 
	 */
	public boolean hasUlpAcc(int n) {
		if (isInfinite())
			return false;
		double x = inf;
		int i = 0; 
		while (i++ < n && x < sup)
			x = next(x);
		return x == sup;
	}

	/**
	 * Returns true iff this interval is an entire interval [ -INF, +INF ].
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.isEntire(n) == false for x == [ EMPTY ] 
	 * </UL> 
	 */
	public boolean isEntire() {
		return -inf == sup && sup == Double.POSITIVE_INFINITY;
	}

	/**
	 * Always returns true indicating that the extended system is currently used.
	 */
	public static boolean isExtended() {
		return true;
	}
  
	/**
	 * Always returns false, indicating that this is not native implementation.
	 */
	public static boolean isNative() {
		return false;
	}

	// -----------------------------------------------------------------------
	// Utility functions 
	// -----------------------------------------------------------------------
 
	/**
	 * Returns an approximation of the midpoint of this interval, i.e.
	 * 
	 * x.mid == (x.inf() + x.sup()) / 2.
	 * 
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.mid() == NaN for x == [ EMPTY ] 
	 * <LI> x.mid() == 0.0 for x == [ ENTIRE ]
	 * <LI> x.mid() == +INF for x == [ +INF ] or x = [ a, +INF ]
	 * <LI> x.mid() == -INF for x == [ -INF ] or x = [ -INF, a]
	 * </UL> 
	 */
    public double mid() {
		return inf == sup ? inf : -inf == sup ? 0 : 0.5*inf + 0.5*sup;
    }

	/**
	 * Returns an upper bound for the width (diameter) of this interval, i.e.
	 * 
	 * x.wid() == x.sup()-x.inf()
	 * 
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.wid() == NaN for x == [ EMPTY ] 
	 * <LI> x.wid() == +INF for any infinite interval
	 * </UL> 
	 */
    public double wid() {
		return add_up(sup, -inf);
	}

	/**
	 * Returns the mignitude of this interval, i.e.
	 *
	 * x.mig() == min{abs(y) : y in x }
	 * 
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.mig() == NaN for x == [ EMPTY ] 
	 * </UL>
	 */
	public double mig() {
		return inf <= 0 && sup >= 0 ? 0 : inf < 0 ? -sup : inf;
	}

	/** 
	 * Returns the magnitude of this interval, i.e.
	 *
	 * x.mag() == max{abs(y) : y in x }
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.mag() == NaN for x == [ EMPTY ] 
	 * <LI> x.mag() == +INF for any infinite interval
	 * </UL>
	 */
	public double mag() {
		return -inf > sup ? -inf : sup;
    }

	/**
	 * Returns the interval of absolute values of this interval, i.e.
	 *
	 * x.abs() == [ x.mig(), x.mag() ]
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.abs() == [ EMPTY ] for x == [ EMPTY ] 
	 * <LI> x.abs() == [ +INF ] for x == [ +/- INF ]
	 * </UL>
	 */
	public Interval abs()
    {
		if (sup <= 0) {
			double h = sup;
			this.sup = -inf;
			this.inf = -h;
		} else if (inf < 0) {
			if (-inf > sup)
				this.sup = -inf;
			// else sup = this.sup;
			inf = 0;
		} // else { inf = this.inf; sup = this.sup; }
		return this;
	}

	/**
	 * Returns an enclosure for the range of minima of this interval and the 
	 * interval x, i.e.
	 *
	 * x.min(y) == { z : z == min(a, b) : a in x, b in y }
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.min(y) == [ EMPTY ] for x == [ EMPTY ] and y == [ EMPTY ] 
	 * </UL>
	 */
	public Interval min(Interval y) {
		if (this.inf != this.inf) // inf = this.inf; sup = this.sup;
			return this;
		inf = (this.inf < y.inf ? this.inf : y.inf);
		sup = (this.sup < y.sup ? this.sup : y.sup);
		return this;
    }

	/**
	 * Returns an enclosure for the range of maxima of this interval and the 
	 * interval x, i.e.
	 *
	 * x.max(y) == { z : z == max(a, b) : a in x, b in y }
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.max(y) == [ EMPTY ] for x == [ EMPTY ] and y == [ EMPTY ]
	 * </UL>
	 */
	public Interval max(Interval y) {
		if (this.inf != this.inf) // inf = this.inf; sup = this.sup;
			return this;
		inf = (this.inf > y.inf ? this.inf : y.inf);
		sup = (this.sup > y.sup ? this.sup : y.sup);
		return this;
    }

	/**
	 * Returns an upper bound for the Hausdorff distance of this interval 
	 * and the interval x, i.e.
	 *
	 * x.dist(y) == max{ abs(x.inf()-y.inf()), abs(x.sup() - y.sup()) }
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.dist(y) == NaN for x == [ EMPTY ] or y == [ EMPTY ] 
	 * </UL>
	 */
	double dist(Interval x) {

		if (isEmpty() || x.isEmpty())
			return Double.NaN;

		if ( this.inf == x.inf && this.sup == x.sup)
			return 0;

		if (isInfinite() || x.isInfinite())
			return Double.POSITIVE_INFINITY;;

		double dinf = this.inf > x.inf ? add_up(this.inf, -x.inf) : add_up(-this.inf, x.inf);
		double dsup = this.sup > x.sup ? add_up(this.sup, -x.sup) : add_up(-this.sup, x.sup);

		return Math.max(dinf, dsup);
	}

	// -----------------------------------------------------------------------
	// Set operations
	// -----------------------------------------------------------------------

	/** 
	 * Returns the intersection of this interval and the interval x.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.intersect(y) == [ EMPTY ] iff x and y are disjoint
	 * </UL>
	 */
	public Interval intersect(Interval y) {
		double l, u;
		if (isEmpty() || y.isEmpty()) {
			this.inf = Double.NaN;
			this.sup = Double.NaN;
			return this;
		}
		l = (this.inf > y.inf ? this.inf : y.inf);
		u = (this.sup < y.sup ? this.sup : y.sup);
		if (l > u) {
			this.inf = Double.NaN;
			this.sup = Double.NaN;
			return this;
		}
		this.inf = l;
		this.sup = u;
		return this;
	}


	/**
	 * Same as x.intersect(y)
	 */
	public Interval ix(Interval y) {
		return intersect(y);
	}
  
	/**
	 * Returns the convex hull of this interval and the interval x.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.interval_hull(y) == [ EMPTY ] for x == y == [ EMPTY ]
	 * </UL>
	 */
	public Interval interval_hull(Interval y) {
		if (isEmpty()) {
			this.inf = y.inf;
			this.sup = y.sup;
			return this;
		}
		if (y.isEmpty())
			return this;
		inf = (this.inf < y.inf ? this.inf : y.inf);
		sup = (this.sup > y.sup ? this.sup : y.sup);
		return this;
    }

	/**
	 * Returns the convex hull of this interval and the double x.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.interval_hull(y) == [ -INF,+INF ] for y == NaN
	 * </UL>
	 */
	Interval interval_hull(double x) {
		if (isEmpty())
			return assign(x);
		if (x != x)
			return assignEntire();
		inf = (this.inf < x ? this.inf : x);
		sup = (this.sup > x ? this.sup : x);
		return this;
	}

	/**
	 * Same as x.interval_hull(y)
	 */
	public Interval ih(Interval y) {
		return interval_hull(y);
	}

	// -----------------------------------------------------------------------
	// Set relations
	// -----------------------------------------------------------------------
  
	/**
	 * Returns true iff this interval and interval x are disjoint., i.e.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.disjoint(y) == true for x or y == [ EMPTY ]
	 * </UL>
	 */
	boolean disjoint(Interval y) {
		return !(this.inf <= y.sup && y.inf <= this.sup);
	}
  
	/**
	 * Same as x.disjoint(y)
	 */
	public boolean dj(Interval y) {
		return !(this.inf <= y.sup && y.inf <= this.sup);
	}

	// -----------------------------------------------------------------------
	// Arithmetic operations
	// -----------------------------------------------------------------------
  
	/** 
	 * Unary operator -. Myltiplies this interval by -1 and returns the product.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> -x == [ EMPTY ] for x == [ EMPTY ]
	 * </UL>
	 */
	public Interval negate() {
		double l = inf;
		inf = -sup;
		sup = -l;
		return this;
    }

	/**
	 * Adds interval x to this interval and returns the sum.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x += y == [ EMPTY ] for x == [ EMPTY ] or y == [ EMPTY ]
	 * </UL>
	 */
	public Interval add(Interval y) {
		double l = this.inf + y.inf;
		if (l - this.inf > y.inf || l - y.inf > this.inf) {
			assert Math.abs(l) >= MIN_NORMAL*2;
			l = (l < 0 ? l + l*ULP_EPS : l < Double.POSITIVE_INFINITY ? l*SCALE_DOWN : Double.MAX_VALUE);
		}
		inf = l;
		double h = this.sup + y.sup;
		if (h - this.sup < y.sup || h - y.sup < this.sup) {
			assert Math.abs(h) >= MIN_NORMAL*2;
			h = (h > 0 ? h + h*ULP_EPS : h > Double.NEGATIVE_INFINITY ? h*SCALE_DOWN : -Double.MAX_VALUE);
		}
		sup = h;
		return this;
	}

	/**
	 * Subtracts interval x from this interval and returns the difference.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x -= y == [ EMPTY ] for x == [ EMPTY ] or y == [ EMPTY ]
	 * </UL>
	 */
	public Interval sub(Interval y) {
		double l = this.inf - y.inf;
		if (this.inf - l < y.inf || l + y.inf > this.inf) {
			assert Math.abs(l) >= MIN_NORMAL*2;
			l = (l < 0 ? l + l*ULP_EPS : l < Double.POSITIVE_INFINITY ? l*SCALE_DOWN : Double.MAX_VALUE);
		}
		inf = l;
		double h = this.sup - y.sup;
		if (this.sup - h >  y.sup || h + y.sup < this.sup) {
			assert Math.abs(l) >= MIN_NORMAL*2;
			h = (h > 0 ? h + h*ULP_EPS : h > Double.NEGATIVE_INFINITY ? h*SCALE_DOWN : -Double.MAX_VALUE);
		}
		sup = h;
		return this;
    }

	/**
	 * Multiplies this interval by interval x and returns the product.
	 *
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x *= y == [ EMPTY ] for x == [ EMPTY ] or y == [ EMPTY ]
	 * </UL>
	 */
	public Interval mul(Interval y) {
		double l, h;

		if (y.inf > 0) {
			if (this.inf > 0) { // x > 0, y > 0
				l = this.inf*y.inf;
				h = this.sup*y.sup;
			} else if (this.sup < 0) { // x < 0, y > 0
				l = this.inf*y.sup;
				h = this.sup*y.inf;
			} else { // 0 in x or x is [NaN,NaN], y > 0
				l = this.inf*y.sup;
				h = this.sup*y.sup;
			}
		} else if (y.sup < 0) {
			if (this.inf > 0) { // x > 0, y < 0
				l = this.sup*y.inf;
				h = this.inf*y.sup;
			} else if (this.sup < 0) { // x < 0, y < 0
				l = this.sup*y.sup;
				h = this.inf*y.inf;
			} else { // 0 in x or x is [NaN,NaN], y < 0
				l = this.sup*y.inf;
				h = this.inf*y.inf;
			}
		} else {
			if (this.inf > 0) { // x > 0, 0 in y or y is [NaN,NaN]
				l = this.sup*y.inf;
				h = this.sup*y.sup;
			} else if (this.sup < 0) { // x < 0, 0 in y or y is [NaN,NaN]
				l = this.inf*y.sup;
				h = this.inf*y.inf;
			} else { // 0 in x or x is [NaN,NaN], 0 in y or y is [NaN,NaN]
				l = this.sup*y.inf;
				double lo = this.inf*y.sup;
				if (l > lo || lo != lo)
					l = lo;
				h = this.inf*y.inf;
				double ho = this.sup*y.sup;
				if (h < ho || ho != ho)
					h = ho;
			}
		}

		if (l > 0)
			inf = prevPos(l);
		else if (l < 0)
			inf = prevNeg(l);
		else if (l == 0)
			inf = (this.inf >= 0 && y.inf >= 0 || this.sup <= 0 && y.sup <= 0) ? 0 : -Double.MIN_VALUE;
		else if (this.inf == this.inf && y.inf == y.inf)
			return assignEntire();
		else
			inf = l;

		if (h > 0)
			sup = nextPos(h);
		else if (h < 0)
			sup = nextNeg(h);
		else if (h == 0)
			sup = (this.inf >= 0 && y.inf <= 0 || this.sup <= 0 && y.sup >= 0) ? 0 : Double.MIN_VALUE;
		else if (this.inf == this.inf && y.inf == y.inf)
			return assignEntire();
		else
			sup = h;

		return this;
	}

	/**
	 * Divides this interval by interval x and returns the quotient.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x /= y == [ EMPTY ] for x == [ EMPTY ] or y == [ EMPTY ]
	 * <LI> x /= y == [ ENTIRE ] if y contains 0
	 * </UL>
	 */
	public Interval div(Interval y) {
		double l, h;

		if (y.inf > 0) {
			if (this.inf > 0) { // x > 0, y > 0
				l = this.inf/y.sup;
				h = this.sup/y.inf;
			} else if (this.sup < 0) { // x < 0, y > 0
				l = this.inf/y.inf;
				h = this.sup/y.sup;
			} else { // 0 in x or x is [NaN,NaN], y > 0
				l = this.inf/y.inf;
				h = this.sup/y.inf;
			}
		} else if (y.sup < 0) {
			if (this.inf > 0) { // x > 0, y < 0
				l = this.sup/y.sup;
				h = this.inf/y.inf;
			} else if (this.sup < 0) { // x < 0, y < 0
				l = this.sup/y.inf;
				h = this.inf/y.sup;
			} else { // 0 in x or x is [NaN,NaN], y < 0
				l = this.sup/y.sup;
				h = this.inf/y.sup;
			}
		} else { // 0 in y or y is [NaN,NaN]
			l = h = Double.NaN;
		}

		if (l > 0)
			inf = prevPos(l);
		else if (l < 0)
			inf = prevNeg(l);
		else if (l == 0)
			inf = (this.inf >= 0 && y.inf > 0 || this.sup <= 0 && y.sup < 0) ? 0 : -Double.MIN_VALUE;
		else if (this.inf == this.inf && y.inf == y.inf)
			return assignEntire();
		else
			inf = l;

		if (h > 0)
			sup = nextPos(h);
		else if (h < 0)
			sup = nextNeg(h);
		else if (h == 0)
			sup = (this.inf >= 0 && y.inf < 0 || this.sup <= 0 && y.sup > 0) ? 0 : Double.MIN_VALUE;
		else if (this.inf == this.inf && y.inf == y.inf)
			return assignEntire();
		else
			sup = h;

		return this;
	}

	// -----------------------------------------------------------------------
	// Elementary functions
	// -----------------------------------------------------------------------

	/**
	 * Replaces this interval by an interval enclosure of its exponential.
	 */
    public Interval exp() {
		if (isEmpty())
			return this;
		inf = prevPos(Math.exp(this.inf));
		sup = nextPos(Math.exp(this.sup));
		return this;
    }

	// -----------------------------------------------------------------------
	// predecessor and successor of a number
	// -----------------------------------------------------------------------

    /**
     * Returns the size of an ulp of the argument.  An ulp of a
     * <code>double</code> value is the positive distance between this
     * floating-point value and the <code>double</code> value next
     * larger in magnitude.  Note that for non-NaN <i>x</i>,
     * <code>ulp(-<i>x</i>) == ulp(<i>x</i>)</code>.
     * 
     * <p>Special Cases:
     * <ul>
     * <li> If the argument is NaN, then the result is NaN.
     * <li> If the argument is positive or negative infinity, then the
     * result is positive infinity.
     * <li> If the argument is positive or negative zero, then the result is
     * <code>Double.MIN_VALUE</code>.
     * <li> If the argument is &plusmn;<code>Double.MAX_VALUE</code>, then
     * the result is equal to 2<sup>971</sup>.
     * </ul>
     *
     * @param d the floating-point value whose ulp is to be returned
     * @return the size of an ulp of the argument
     */
	public static double ulp(double d) {
		if (d < 0)
			d = -d;
		if (d < Double.MAX_VALUE) {
			if (d >= MIN_NORMAL*2)
				return (d + d*ULP_EPS) - d;
			return Double.MIN_VALUE;
		}
		if (d == Double.MAX_VALUE)
			return MAX_ULP;
		return d;
	}

	/**
	 * Returns previous floating point double number.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> prev(NaN) == NaN
	 * <LI> prev(+INF) == Double.MAX_VALUE
	 * <LI> prev(-INF) == -INF
	 * <LI> prev(0) == -Double.MIN_VALUE
	 * </UL>
	 */
	public static double prev(double x) {
		return x < MIN_NORMAL
			? (x <= -MIN_NORMAL*2 ? x + x*ULP_EPS : x - Double.MIN_VALUE)
			: x == Double.POSITIVE_INFINITY ? Double.MAX_VALUE : x*SCALE_DOWN;
	}

	/**
	 * Returns next floating point double number.
	 *
	 * Special cases:
	 * <UL>
	 * <LI> next(NaN) == NaN
	 * <LI> next(+INF) == +INF
	 * <LI> next(-INF) == -Double.MAX_VALUE
	 * <LI> next(0) == Double.MIN_VALUE
	 * </UL>
	 */
	public static double next(double x) {
		return x > -MIN_NORMAL
			? (x >= MIN_NORMAL*2 ? x + x*ULP_EPS : x + Double.MIN_VALUE)
			: x == Double.NEGATIVE_INFINITY ? -Double.MAX_VALUE : x*SCALE_DOWN;
	}

	private static double nextPos(double x) {
		assert x >= 0;
		return x >= MIN_NORMAL*2 ? x + x*ULP_EPS : x + Double.MIN_VALUE;
	}
	private static double nextNeg(double x) {
		assert x <= 0;
		return x <= -MIN_NORMAL ? (x > Double.NEGATIVE_INFINITY ? x*SCALE_DOWN : -Double.MAX_VALUE) : x < 0 ? x + Double.MIN_VALUE : x;
	}

	private static double prevPos(double x) {
		assert x >= 0;
		return x >= MIN_NORMAL ? (x < Double.POSITIVE_INFINITY ? x*SCALE_DOWN : Double.MAX_VALUE ) : x > 0 ? x - Double.MIN_VALUE : x;
	}

	private static double prevNeg(double x) {
		assert x <= 0;
		return x <= MIN_NORMAL*2 ? x + x*ULP_EPS : x - Double.MIN_VALUE;
	}

	private double add_up(double x, double y) {
		double z = x + y;
		assert z > 0;
		if (z - x < y || z - y < x) {
			assert Math.abs(z) > MIN_NORMAL*2;
			z = z + z*ULP_EPS;
		}
		return z;
	}

}
