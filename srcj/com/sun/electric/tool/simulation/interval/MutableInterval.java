/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MutableInterval.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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

import java.math.BigDecimal;

/**
 * Mutable class for representation of intervals X = [a, b].
 * a and b are double precision floation point numbers with a <= b. 
 * 
 * Closed ("extended") interval system is implemented, which is the extension
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
public class MutableInterval
{
    /* Fields */

    private double inf;
    private double sup;

    /* Constants */

    /**
     * A constant holding 3/4*ulp(1).
     * next(x) is round-to-nearest
	 * of (x + x*ULP_EPS) for positive normalized doble numbers.
     */
    private static final double ULP_EPS = 1.5/(1L << 53); // 0x1.8p-53;
    /**
     * A constant holding prev(1).
     * prev(x) is round-to-nearest
	 * if (x*SCALE_DOWN) for positive normalized double numbers.
     */
    private static final double SCALE_DOWN = 1.0 - 1.0/(1L << 53); // 0x0.fffffffffffffp0;
    /**
     * A constant holding minimal positive normalized double number.
     */
    private static final double MIN_NORMAL = Double.MIN_VALUE*(1L << 52); // 0x1.0p-1022;
    /**
     * A constant limiting those x for which x*ULP_EPS is noramlized.
     */
    private static final double ULP_EPS_NORMAL = MIN_NORMAL / ULP_EPS; // 0x1.5555555555555p-970
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

	// -----------------------------------------------------------------------
	// Constructors
	// -----------------------------------------------------------------------

	/**
	 * Constructs a point interval [0,0].
	 */
	public MutableInterval() {
	}

	/**
	 * Constructs a point interval [x,x].
	 */
	public MutableInterval(int x) {
		assign(x);
	}

	/**
	 * Constructs sharpest interval containing x.
	 */
	public MutableInterval(long x) {
		assign(x);
	}

	/**
	 * Constructs a point interval [x,x].
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> if x == +INF then [ +INF ] is constructed
	 * <LI> if x == -INF then [ -INF ] is constructed
	 * <LI> if x == NaN then the entire interval [ -INF,+INF ] is constructed
	 * </UL>
	 */
    public MutableInterval(double x) {
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
	public MutableInterval(int inf, int sup) {
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
	public MutableInterval(long inf, long sup) {
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
	public MutableInterval(double inf, double sup) {
		assign(inf, sup);
	}

	/**
	 * Constructs the interval same as x.
	 */
	public MutableInterval(MutableInterval x) {
		assign(x);
	}

	/**
	 * Constructs the interval from string.
	 */
	public MutableInterval(String s) {
		assign(s);
	}

	/**
	 * Constructs the interval from character array.
	 */
	public MutableInterval(char[] b) {
		assign(b);
	}

	// -----------------------------------------------------------------------
	// Assigns
	// -----------------------------------------------------------------------

	/**
	 * Assigns a point interval [x,x].
	 */
	public MutableInterval assign(int x) {
		inf = sup = (double)x;
		return this;
	}

	/**
	 * Assigns sharpest interval containing x.
	 */
	public MutableInterval assign(long x) {
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
    public MutableInterval assign(double x) {
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
	public MutableInterval assign(int inf, int sup) {
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
	public MutableInterval assign(long inf, long sup) {
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
	public MutableInterval assign(double inf, double sup) {
		if (inf <= sup) {
			this.inf = (inf == Double.POSITIVE_INFINITY ? Double.MAX_VALUE : inf);
			this.sup = (sup == Double.NEGATIVE_INFINITY ? -Double.MAX_VALUE : sup);
		} else
			assignEntire();
		return this;
	}

	/**
	 * Assigns interval same as x.
	 */
	public MutableInterval assign(MutableInterval x) {
		this.inf = x.inf;
		this.sup = x.sup;
		return this;
	}

	/**
	 * Assigns the interval from string.
	 */
	public MutableInterval assign(String s) {
		parse(s);
		return this;
	}

	/**
	 * Assigns the interval from character array.
	 */
	public MutableInterval assign(char[] b) {
		parse(new String(b));
		return this;
	}

	/**
     * Creates and returns a copy of this MutableInterval.
	 */
	public MutableInterval clon() {
		try {
			return (MutableInterval)clone();
		}
		catch (CloneNotSupportedException e) {
			return new MutableInterval(inf, sup);
		}
	}

	/**
	 * Assigns entire interval [ -INF, +INF ].
	 */
	public MutableInterval assignEntire() {
		inf = Double.NEGATIVE_INFINITY;
		sup = Double.POSITIVE_INFINITY;
		return this;
	}

	/**
	 * Assigns empty interval [-EMPTY ].
	 */
	public MutableInterval assignEmpty() {
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
	 * Returns true iff this interval is a point interval.
	 * i.e. inf() == sup().
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
	 * Returns true iff this interval has an ulp accuracy of n.
	 * I.e. x.inf() and x.sup() have a distance of at most n machine numbers.  
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
	 * Returns double number nearest to the midpoint of this interval, i.e.
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
		double mid = 0.5*(inf + sup);
		if (mid > Double.NEGATIVE_INFINITY && mid < Double.POSITIVE_INFINITY)
			return mid;
		return -inf == sup ? 0 : 0.5*inf + 0.5*sup;
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
		return addPosUp(sup, -inf);
	}

	/**
	 * Returns an upper bound for the radius of this interval, i.e.
	 *
	 * x.mid() - x.rad() <= x.inf() <= x.sup <= x.mid() + x.rad()
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.rad() == NaN for x == [ EMPTY ] 
	 * <LI> x.rad() == +INF for any infinite interval
	 * </UL>
	 */
	public double rad() {
		double mid = (inf + sup) * 0.5;
		if (!(mid > Double.NEGATIVE_INFINITY && mid < Double.POSITIVE_INFINITY)) {
			if (inf == Double.NEGATIVE_INFINITY || sup == Double.POSITIVE_INFINITY) return Double.POSITIVE_INFINITY;
			mid = 0.5*inf + 0.5*sup;
		}
		return Math.max(addPosUp(sup, -mid), addPosUp(mid, -inf));
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
	public MutableInterval abs()
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
	public MutableInterval min(MutableInterval y) {
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
	public MutableInterval max(MutableInterval y) {
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
	double dist(MutableInterval x) {

		if (isEmpty() || x.isEmpty())
			return Double.NaN;

		if ( this.inf == x.inf && this.sup == x.sup)
			return 0;

		if (isInfinite() || x.isInfinite())
			return Double.POSITIVE_INFINITY;;

		double dinf = this.inf > x.inf ? addPosUp(this.inf, -x.inf) : addPosUp(-this.inf, x.inf);
		double dsup = this.sup > x.sup ? addPosUp(this.sup, -x.sup) : addPosUp(-this.sup, x.sup);

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
	public MutableInterval intersect(MutableInterval y) {
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
	public MutableInterval ix(MutableInterval y) {
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
	public MutableInterval interval_hull(MutableInterval y) {
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
	public MutableInterval interval_hull(double x) {
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
	public MutableInterval ih(MutableInterval y) {
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
	public boolean disjoint(MutableInterval y) {
		return !(this.inf <= y.sup && y.inf <= this.sup);
	}
  
	/**
	 * Same as x.disjoint(y)
	 */
	public boolean dj(MutableInterval y) {
		return !(this.inf <= y.sup && y.inf <= this.sup);
	}

	/**
	 * Returns true iff double number x is contained in this interval.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.contains(y) == false for x == [ EMPTY ] or y == NaN
	 * </UL>
	 */
	public boolean in(long x) {
		double xd = (double)x;
		if (xd >= -EXACT_LONG && xd <= EXACT_LONG)
			return inf <= xd && xd <= sup;
		long xx = (long)xd;
		return (xx > x || x == Long.MAX_VALUE ? inf < xd : inf <= xd) &&
			(xx < x ? xd < sup : xd <= sup);
	}

	/**
	 * Returns true iff double number x is contained in this interval.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.contains(y) == false for x == [ EMPTY ] or y == NaN
	 * </UL>
	 */
	public boolean in(double y) {
		return y >= this.inf && y <= this.sup;
	}

	/** 
	 * Returns true iff this interval is in interior of interval x.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.in_interior(y) == true for x == [ EMPTY ] 
	 * </UL>
	 */
	public boolean in_interior(MutableInterval y) {
		return (inf > y.inf && sup < y.sup) || isEmpty();
	}

	/**
	 * Same as x.interior(y)
	 */
	public boolean interior(MutableInterval y) {
		return in_interior(y);
	}


	/**
	 * Returns true iff this interval is a proper subset of interval x.
	 * 
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.proper_subset(y) == true for x == [ EMPTY ] and y != [ EMPTY ] 
	 * </UL>
	 */
	public boolean proper_subset(MutableInterval y) {
		return (inf >= y.inf && sup <= y.sup && (inf > y.inf || sup < y.sup)) ||
			(isEmpty() && ! y.isEmpty());
	}

	/**
	 * Returns true iff this interval is a subset of interval x.
	 * Special cases in the extended system:
	 *
	 * <UL>
	 * <LI> x.subset(y) == true for x == [ EMPTY ] 
	 * </UL>   
	 */
	public boolean subset(MutableInterval y) {
		return y.inf <= inf && sup <= y.sup || isEmpty();
	}
  
	/**
	 * Returns true iff this interval is a proper superset of interval x.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.proper_superset(y) == true for x != [ EMPTY ] and y == [ EMPTY ] 
	 * </UL>
	 */
	public boolean proper_superset(MutableInterval y) {
		return (inf <= y.inf && y.sup <= sup && (inf < y.inf || y.sup < sup)) ||
			(y.isEmpty() && ! isEmpty());
	}

	/**
	 * Returns true iff this interval is a superset of interval x.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.superset(y) == true for y == [ EMPTY ] 
	 * </UL>
	 */
	public boolean superset(MutableInterval y) {
		return inf <= y.inf && y.sup <= sup || y.isEmpty();
	}
  
	/**
	 * Returns true iff this interval is set-equal to interval x.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.seq(y) == true for x == y == [ EMPTY ] 
	 * </UL>
	 */
	public boolean seq(MutableInterval y) {
		return (inf == y.inf && sup == y.sup) || isEmpty() && y.isEmpty();
	}
  
	/**
	 * Returns true iff this interval is set-not-equal to interval x.
	 */
	public boolean sne(MutableInterval y) {
		return !seq(y);
	}

	/**
	 * Returns true iff this interval is set-greater-or-equal to 
	 * interval x.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.sge(y) == true for x == y == [ EMPTY ] 
	 * </UL>
	 */
	public boolean sge(MutableInterval y) {
		return inf >= y.inf && sup >= y.sup || isEmpty() && y.isEmpty();
	}

	/**
	 * Returns true iff this interval is set-greater than interval x.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.sgt(y) == false for x == [ EMPTY ] or y == [ EMPTY ] 
	 * </UL>
	 */
	public boolean sgt(MutableInterval y) {
		return inf > y.inf && sup > y.sup;
	}
  
	/**
	 * Returns true iff this interval is set-less-or-equal to 
	 * interval x.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.sle(y) == true for x == y == [ EMPTY ] 
	 * </UL>
	 */
	public boolean sle(MutableInterval y) {
		return inf <= y.inf && sup <= y.sup || isEmpty() && y.isEmpty();
	}

	/**
	 * Returns true iff this interval is set-less than interval x.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.slt(y) == false for x == [ EMPTY ] or y == [ EMPTY ] 
	 * </UL>
	 */
	public boolean slt(MutableInterval y) {
		return inf < y.inf && sup < y.sup;
	}
  
	// -----------------------------------------------------------------------
	// Certainly relations
	// -----------------------------------------------------------------------

	/**
	 * Returns true iff this interval is certainly-equal to interval x.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.ceq(y) == false for x == [ EMPTY ] or y == [ EMPTY ] 
	 * </UL>
	 */
	public boolean ceq(MutableInterval y) {
		return sup <= y.inf && inf >= y.sup;
	}

	/**
	 * Returns true iff this interval is certainly-not-equal to interval x.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.cne(y) == true for x == [ EMPTY ] or y == [ EMPTY ] 
	 * </UL>
	 */
	public boolean cne(MutableInterval y) {
		return !(inf <= y.sup && y.inf <= sup);
	}

	/**
	 * Returns true iff this interval is certainly-greater-or-equal to 
	 * interval x.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.cge(y) == false for x == [ EMPTY ] or y == [ EMPTY ]
	 * </UL>
	 */
	public boolean cge(MutableInterval y) {
		return inf >= y.sup;
	}

	/**
	 * Returns true iff this interval is certainly-greater than interval x.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.cgt(y) == false for x == [ EMPTY ] or y == [ EMPTY ] 
	 * </UL>
	 */
	public boolean cgt(MutableInterval y) {
		return inf > y.sup;
	}

	/**
	 * Returns true iff this interval is certainly-less-or-equal to 
	 * interval x.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.cle(y) == false for x == [ EMPTY ] or y == [ EMPTY ]
	 * </UL>
	 */
	public boolean cle(MutableInterval y) {
		return sup <= y.inf;
	}

	/**
	 * Returns true iff this interval is certainly-less than interval x.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.clt(y) == false for x == [ EMPTY ] or y == [ EMPTY ] 
	 * </UL>
	 */
	public boolean clt(MutableInterval y) {
		return sup < y.inf;
	}

	// -----------------------------------------------------------------------
	// Possibly relations
	// -----------------------------------------------------------------------
  
	/**
	 * Returns true iff this interval is possibly-equal to interval x.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.peq(y) == false for x == [ EMPTY ] or y == [ EMPTY ] 
	 * </UL>
	 */
	public boolean peq(MutableInterval y) {
		return inf <= y.sup && sup >= y.inf;
	}

	/**
	 * Returns true iff this interval is possibly-not-equal to interval x.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.pne(y) == true for x == [ EMPTY ] or y == [ EMPTY ] 
	 * </UL>
	 */
	public boolean pne(MutableInterval y) {
		return !(sup <= y.inf && inf >= y.sup);
	}

	/**
	 * Returns true iff this interval is possibly-greater-or-equal to 
	 * interval x.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.pge(y) == false for x == [ EMPTY ] or y == [ EMPTY ]
	 * </UL>
	 */
	public boolean pge(MutableInterval y) {
		return sup >= y.inf;
	}

	/**
	 * Returns true iff this interval is possibly-greater than interval x.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.pgt(y) == false for x == [ EMPTY ] or y == [ EMPTY ] 
	 * </UL>
	 */
	public boolean pgt(MutableInterval y) {
		return sup > y.inf;
	}

	/**
	 * Returns true iff this interval is possibly-less-or-equal to 
	 * interval x.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.ple(y) == false for x == [ EMPTY ] or y == [ EMPTY ]
	 * </UL>
	 */
	public boolean ple(MutableInterval y) {
		return inf <= y.sup;
	}

	/**
	 * Returns true iff this interval is possibly-less than interval x.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x.plt(y) == false for x == [ EMPTY ] or y == [ EMPTY ] 
	 * </UL>
	 */
	public boolean plt(MutableInterval y) {
		return inf < y.sup;
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
	public MutableInterval negate() {
		double l = inf;
		inf = -sup;
		sup = -l;
		return this;
    }

	/**
	 * Adds interval y to this interval and returns the sum.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x += y == [ EMPTY ] for x == [ EMPTY ] or y == [ EMPTY ]
	 * </UL>
	 */
	public MutableInterval add(MutableInterval y) {
		double l = this.inf + y.inf;
		if (l - this.inf > y.inf || l - y.inf > this.inf) {
			assert Math.abs(l) >= MIN_NORMAL*2;
			l = (l < 0 ? (l < -ULP_EPS_NORMAL ? l + l*ULP_EPS : l/SCALE_DOWN) : l <= Double.MAX_VALUE ? l*SCALE_DOWN : Double.MAX_VALUE);
		}
		double h = this.sup + y.sup;
		if (h - this.sup < y.sup || h - y.sup < this.sup) {
			assert Math.abs(h) >= MIN_NORMAL*2;
			h = (h > 0 ? (h > ULP_EPS_NORMAL ? h + h*ULP_EPS : h/SCALE_DOWN) : h >= -Double.MAX_VALUE ? h*SCALE_DOWN : -Double.MAX_VALUE);
		}
		inf = l;
		sup = h;
		return this;
	}

	/**
	 * Adds double number y to this interval and returns the sum.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x += y == [ EMPTY ] for x == [ EMPTY ] or y == NaN
	 * </UL>
	 */
	public MutableInterval add(double y) {
		double l = this.inf + y;
		if (l - this.inf > y || l - y > this.inf) {
			assert Math.abs(l) >= MIN_NORMAL*2;
			l = (l < 0 ? (l < -ULP_EPS_NORMAL ? l + l*ULP_EPS : l/SCALE_DOWN) : l <= Double.MAX_VALUE ? l*SCALE_DOWN : Double.MAX_VALUE);
		} else if (!(l < Double.POSITIVE_INFINITY)) {
			if (y >= this.sup) // x is not [EMPTY] and y is not NaN 
				l = Double.MAX_VALUE;
		}
		double h = this.sup + y;
		if (h - this.sup < y || h - y < this.sup) {
			assert Math.abs(h) >= MIN_NORMAL*2;
			h = (h > 0 ? (h > ULP_EPS_NORMAL ? h + h*ULP_EPS : h/SCALE_DOWN) : h >= -Double.MAX_VALUE ? h*SCALE_DOWN : -Double.MAX_VALUE);
		} else if (!(h > Double.NEGATIVE_INFINITY)) {
			if (y <= this.inf) // x is not [EMPTY] and y is not NaN
				h = -Double.MAX_VALUE;
		}
		inf = l;
		sup = h;
		return this;
		
	}

	/**
	 * Subtracts interval y from this interval and returns the difference.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x -= y == [ EMPTY ] for x == [ EMPTY ] or y == [ EMPTY ]
	 * </UL>
	 */
	public MutableInterval sub(MutableInterval y) {
		double l = this.inf - y.sup;
		if (this.inf - l < y.sup || l + y.sup > this.inf) {
			assert Math.abs(l) >= MIN_NORMAL*2;
			l = (l < 0 ? (l < -ULP_EPS_NORMAL ? l + l*ULP_EPS : l/SCALE_DOWN) : l <= Double.MAX_VALUE ? l*SCALE_DOWN : Double.MAX_VALUE);
		}
		double h = this.sup - y.inf;
		if (this.sup - h >  y.inf || h + y.inf < this.sup) {
			assert Math.abs(h) >= MIN_NORMAL*2;
			h = (h > 0 ? (h > ULP_EPS_NORMAL ? h + h*ULP_EPS : h/SCALE_DOWN) : h >= -Double.MAX_VALUE ? h*SCALE_DOWN : -Double.MAX_VALUE);
		}
		inf = l;
		sup = h;
		return this;
    }

	/**
	 * Subtracts double number y from this interval and returns the difference.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x -= y == [ EMPTY ] for x == [ EMPTY ] or y == NaN
	 * </UL>
	 */
	public MutableInterval sub(double y) {
		double l = this.inf - y;
		if (this.inf - l < y || l + y > this.inf) {
			assert Math.abs(l) >= MIN_NORMAL*2;
			l = (l < 0 ? (l < -ULP_EPS_NORMAL ? l + l*ULP_EPS : l/SCALE_DOWN) : l <= Double.MAX_VALUE ? l*SCALE_DOWN : Double.MAX_VALUE);
		} else if (!(l < Double.POSITIVE_INFINITY)) {
			if (y >= this.sup) // x is not [EMPTY] and y is not NaN 
				l = Double.MAX_VALUE;
		}
		double h = this.sup - y;
		if (this.sup - h >  y || h + y < this.sup) {
			assert Math.abs(h) >= MIN_NORMAL*2;
			h = (h > 0 ? (h > ULP_EPS_NORMAL ? h + h*ULP_EPS : h/SCALE_DOWN) : h >= -Double.MAX_VALUE ? h*SCALE_DOWN : -Double.MAX_VALUE);
		} else if (!(h > Double.NEGATIVE_INFINITY)) {
			if (y <= this.inf) // x is not [EMPTY] and y is not NaN
				h = -Double.MAX_VALUE;
		}
		inf = l;
		sup = h;
		return this;
    }

	/**
	 * Multiplies this interval by interval y and returns the product.
	 *
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x *= y == [ EMPTY ] for x == [ EMPTY ] or y == [ EMPTY ]
	 * </UL>
	 */
	public MutableInterval mul(MutableInterval y) {
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
			l = prevPos(l);
		else if (l < 0)
			l = prevNeg(l);
		else if (l == 0)
			l = (this.inf >= 0 && y.inf >= 0 || this.sup <= 0 && y.sup <= 0) ? 0 : -Double.MIN_VALUE;
		else if (this.inf == this.inf && y.inf == y.inf)
			return assignEntire();

		if (h > 0)
			h = nextPos(h);
		else if (h < 0)
			h = nextNeg(h);
		else if (h == 0)
			h = (this.inf >= 0 && y.sup <= 0 || this.sup <= 0 && y.inf >= 0) ? 0 : Double.MIN_VALUE;
		else if (this.inf == this.inf && y.inf == y.inf)
			return assignEntire();

		inf = l;
		sup = h;
		return this;
	}

	/**
	 * Divides this interval by interval y and returns the quotient.
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> x /= y == [ EMPTY ] for x == [ EMPTY ] or y == [ EMPTY ]
	 * <LI> x /= y == [ ENTIRE ] if y contains 0
	 * </UL>
	 */
	public MutableInterval div(MutableInterval y) {
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
			l = prevPos(l);
		else if (l < 0)
			l = prevNeg(l);
		else if (l == 0)
			l = (this.inf >= 0 && y.inf > 0 || this.sup <= 0 && y.sup < 0) ? 0 : -Double.MIN_VALUE;
		else if (this.inf == this.inf && y.inf == y.inf)
			return assignEntire();

		if (h > 0)
			h = nextPos(h);
		else if (h < 0)
			h = nextNeg(h);
		else if (h == 0)
			h = (this.inf >= 0 && y.sup < 0 || this.sup <= 0 && y.inf > 0) ? 0 : Double.MIN_VALUE;
		else if (this.inf == this.inf && y.inf == y.inf)
			return assignEntire();

		inf = l;
		sup = h;
		return this;
	}

	// -----------------------------------------------------------------------
	// Elementary functions
	// -----------------------------------------------------------------------

	/**
	 * Replaces this interval by an interval enclosure of its exponential.
	 */
    public MutableInterval exp() {
		double l = Math.exp(this.inf);
		if (l > 0)
			l = prevPos(l);
		double h = Math.exp(this.sup);
		if (h > 0)
			h = nextPos(h);
		else if (h == 0)
			h = Double.MIN_VALUE;
		inf = l;
		sup = h;
		return this;
    }

	/**
	 * Replaces this interval by an interval enclosure of its natural logarithm.
	 */
	public MutableInterval log() {
		double l = Math.log(this.inf);
		double h = Math.log(this.sup);
		if (l > 0)
			l = prevPos(l);
		else if (l < 0)
			l = prevNeg(l);
		else if (l != l && h == h)
			l = Double.NEGATIVE_INFINITY;
		if (h > 0)
			h = nextPos(h);
		else if (h < 0)
			h = nextNeg(h);
		inf = l;
		sup = h;
		return this;
	}

	// -----------------------------------------------------------------------
	// I/O
	// -----------------------------------------------------------------------

	/**
	 * Return string representation.
	 */

	public String toString() {
		if (isEmpty())
			return "[EMPTY                                          ]";
		StringBuffer buf = new StringBuffer(49);
		buf.append('[');
		append(buf, inf, false);
		buf.append(',');
		append(buf, sup, true);
		buf.append(']');
		return buf.toString();
	}

	private static void append(StringBuffer buf, double x, boolean isSup) {
		final int d = 16; // digits after floating point
		if (x == Double.NEGATIVE_INFINITY)
			buf.append("              -Infinity");
		else if (x == Double.POSITIVE_INFINITY)
			buf.append("               Infinity");
		else if (x == 0 && !isSup)
			buf.append("-.0000000000000000E+000");
		else if (x == 0 && isSup)
			buf.append("0.0000000000000000E+000");
		else {
			BigDecimal bx = new BigDecimal(x);
			String s = bx.unscaledValue().abs().toString();
			int drop = 0;
			if (s.length() != d) {
				drop = s.length() - d;
				bx = bx.setScale(bx.scale() - drop, isSup ? BigDecimal.ROUND_CEILING : BigDecimal.ROUND_FLOOR);
			}
			s = bx.unscaledValue().abs().toString();
			if (s.length() > d) {
				assert s.length() == d + 1 && s.charAt(s.length() - 1) == '0';
				drop++;
				bx = bx.setScale(bx.scale() - 1, BigDecimal.ROUND_UNNECESSARY);
				s = bx.unscaledValue().abs().toString();
			}
		
			buf.append(bx.signum() < 0 ? '-' : '0');
			buf.append('.');
			buf.append(s);
			buf.append('E');
			int exp = d - bx.scale();
			if (exp >= 0)
				buf.append('+');
			else {
				buf.append('-');
				exp = -exp;
			}
			assert (exp < 1000);
			buf.append((char)('0' + exp/100));
			exp %= 100;
			buf.append((char)('0' + exp/10));
			exp %= 10;
			buf.append((char)('0' + exp));
		}
	}

	private void parse(String s) {
		s = s.trim();
		if (s.length() < 2 || s.charAt(0) != '[' || s.charAt(s.length() - 1) != ']')
			throw new NumberFormatException();
		int comma = s.indexOf(',');
		String ls = s.substring(1, comma < 0 ? s.length() - 1 : comma).trim();
		BigDecimal lb = null;
		if (ls.equals("NaN") || ls.equals("+NaN") || ls.equals("-NaN"))
			inf = Double.NaN;
		else if (ls.equals("Infinity") || ls.equals("+Infinity"))
			inf = Double.POSITIVE_INFINITY;
		else if (ls.equals("-Infinity"))
			inf = Double.NEGATIVE_INFINITY;
		else if (ls.equals("EMPTY")) {
			if (comma >= 0)
				throw new NumberFormatException();
			assignEmpty();
			return;
		} else {
			lb = new BigDecimal(ls);
			inf = lb.doubleValue();
		}
		if (comma >= 0) {
			String rs = s.substring(comma + 1, s.length() - 1).trim();
			BigDecimal rb = null;
			if (rs.equals("NaN") || rs.equals("+NaN") || rs.equals("-NaN"))
				sup = Double.NaN;
			else if (rs.equals("Infinity") || rs.equals("+Infinity"))
				sup = Double.POSITIVE_INFINITY;
			else if (rs.equals("-Infinity"))
				sup = Double.NEGATIVE_INFINITY;
			else {
				rb = new BigDecimal(rs);
				sup = rb.doubleValue();
				if (inf >= sup && (inf > sup || lb.compareTo(rb) > 0)) {
					assignEntire();
					return;
				}
				sup = correct(rb, sup, true);
			}
		} else {
			sup = correct(lb, inf, true);
		}
		inf = correct(lb, inf, false);
		if (inf != inf || sup != sup || inf > sup) {
			assignEntire();
			return;
		}
		if (inf == Double.POSITIVE_INFINITY)
			inf = Double.MAX_VALUE;
		if (sup == Double.NEGATIVE_INFINITY)
			sup = -Double.MAX_VALUE;
	}

	private static double correct(BigDecimal b, double d, boolean isSup) {
		if (b == null || Double.isInfinite(d))
			return d;
		int diff = b.compareTo(new BigDecimal(d));
		if (isSup) {
			if (diff > 0)
				d = next(d);
		} else {
			if (diff < 0)
				d = prev(d);
		}
		return d;
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
			if (d > ULP_EPS_NORMAL)
				return (d + d*ULP_EPS) - d;
			if (d >= MIN_NORMAL*2)
				return d/SCALE_DOWN - d;
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
		if (x <= MIN_NORMAL) {
			if (x < -ULP_EPS_NORMAL)
				return x + x*ULP_EPS;
			else if (x <= -MIN_NORMAL)
				return x/SCALE_DOWN;
			else if (x == 0)
				return -Double.MIN_VALUE;
			else
				return Double.longBitsToDouble(Double.doubleToLongBits(x) + (x > 0 ? -1 : 1));
		} else if (x > Double.MAX_VALUE)
			return Double.MAX_VALUE;
		else
			return x*SCALE_DOWN;
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
		if (x >= -MIN_NORMAL) {
			if (x > ULP_EPS_NORMAL)
				return x + x*ULP_EPS;
			else if (x >= MIN_NORMAL)
				return x/SCALE_DOWN;
			else if (x == 0)
				return Double.MIN_VALUE;
			else
				return Double.longBitsToDouble(Double.doubleToLongBits(x) + (x > 0 ? 1 : -1));
		} else if (x < -Double.MAX_VALUE)
			return -Double.MAX_VALUE;
		else
			return x*SCALE_DOWN;
	}

	private static double nextPos(double x) {
		assert x > 0;
		return x > ULP_EPS_NORMAL ? x + x*ULP_EPS : x >= MIN_NORMAL ? x/SCALE_DOWN : Double.longBitsToDouble(Double.doubleToLongBits(x) + 1);
	}

	private static double nextNeg(double x) {
		assert x < 0;
		return x <= -MIN_NORMAL ? (x >= -Double.MAX_VALUE ? x*SCALE_DOWN : -Double.MAX_VALUE) : Double.longBitsToDouble(Double.doubleToLongBits(x) - 1);
	}

	private static double prevPos(double x) {
		assert x > 0;
		return x >= MIN_NORMAL ? (x <= Double.MAX_VALUE ? x*SCALE_DOWN : Double.MAX_VALUE) : Double.longBitsToDouble(Double.doubleToLongBits(x) - 1);
	}

	private static double prevNeg(double x) {
		assert x < 0;
		return x < -ULP_EPS_NORMAL ? x + x*ULP_EPS : x <= -MIN_NORMAL ? x/SCALE_DOWN : Double.longBitsToDouble(Double.doubleToLongBits(x) + 1);
	}

	public static double addUp(double x, double y) {
		double z = x + y;
		if (z - x < y || z - y < x) {
			assert Math.abs(z) >= MIN_NORMAL*2;
			return (z > 0 ? (z > ULP_EPS_NORMAL ? z + z*ULP_EPS : z/SCALE_DOWN) : z >= -Double.MAX_VALUE ? z*SCALE_DOWN : -Double.MAX_VALUE);
		}
		return (z == z || x != x || y != y ? z : Double.POSITIVE_INFINITY);
	}

	public static double addPosUp(double x, double y) {
		double z = x + y;
		assert z >= 0 || z != z;
		if (z - x < y || z - y < x) {
			assert z >= MIN_NORMAL*2;
			return (z > ULP_EPS_NORMAL ? z + z*ULP_EPS : z/SCALE_DOWN);
		}
		return z;
	}

}
