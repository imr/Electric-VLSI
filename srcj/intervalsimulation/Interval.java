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
    private static final double ULP_EPS = 0x1.8p-53;
    /**
     * A constant holding prev(1.0). prev(x) is round-to-nearest
	 * if (x*SCALE_DOWN) for positive normalized double numbers.
     */
    private static final double SCALE_DOWN = 0x0.fffffffffffffp0;
    /**
     * A constant holding minimal positive normalized double number.
     */
    private static final double MIN_NORMAL = 0x1.0p-1022;
    /**
     * A constant holding range of long numbers exactly represented
	 * by double numbers. All longs in [ -EXACT_LONG, EXACT_LONG] are
	 * represented exactly. Some of other longs are not.
     */
	private static final long EXACT_LONG = 1L << 53;
    /**
     * A constant holding Long.MAX_VALUE rounded down.
     */
	private static final double INF_MAX_LONG = 0x1.fffffffffffffp62;

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
	 * <LI> if x == NaN then [ EMPTY ] is constructed
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
	 * <LI> if inf < sup then the interval [ EMPTY ] is constructed
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
	 * <LI> if inf < sup then the interval [ EMPTY ] is constructed
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
	 * <LI> if inf < sup then the interval [ EMPTY ] is constructed
	 * <LI> if inf == NaN or sup == NaN then [ EMPTY ] is constructed
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
	 * <LI> if x == NaN then [ EMPTY ] is constructed
	 * </UL>
	 */
    public Interval assign(double x) {
		inf = sup = x;
		if (x == Double.POSITIVE_INFINITY)
			inf = Double.MAX_VALUE;
		else if (x == Double.NEGATIVE_INFINITY)
			sup = -Double.MAX_VALUE;
		else if (x != x)
			assignEmpty();
		return this;
    }

	/**
	 * Assigns the interval [inf, sup]. 
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> if inf < sup then the interval [ EMPTY ] is constructed
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
			assignEmpty();
		return this;
	}

	/**
	 * Assigns the interval [inf, sup]. 
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> if inf < sup then the interval [ EMPTY ] is constructed
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
			assignEmpty();
		return this;
	}

	/**
	 * Assigns the interval [inf, sup]. 
	 *
	 * Special cases in the extended system:
	 * <UL>
	 * <LI> if inf == sup == -INF the interval [ -INF ] is constructed
	 * <LI> if inf == sup == +INF the interval [ +INF ] is constructed
	 * <LI> if inf < sup then the interval [ EMPTY ] is constructed
	 * <LI> if inf == NaN or sup == NaN then [ EMPTY ] is constructed
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
			assignEmpty();
		return this;
	}

	/**
	 * Constructs interval same as x.:
	 */
	public Interval assign(Interval x) {
		this.inf = x.inf;
		this.sup = x.sup;
		return this;
	}

	/**
	 * Constructs the interval from string.
	 */
	public Interval assign(String s) {
		//		return stringToInterval(s);
		assignEntire();
		return this;
	}

	/**
	 * Constructs the interval from character array.
	 */
	public Interval assign(char[] b) {
		//		return bufToInterval(b);
		assignEntire();
		return this;
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
		double res;
		double d = sup - inf;
		if (sup - d > inf || d + inf < sup) {
			assert  MIN_NORMAL < d && d < Double.POSITIVE_INFINITY;
			d += d*ULP_EPS;
		}
		return d;
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

		if (y.sup >= 0) { // y.sup >= 0
			if (this.sup >= 0) { // x.sup >= 0, y.sup >= 0
				h = this.sup*y.sup;
				if (y.inf <= 0) { // x.sup >= 0, 0 in y
					l = this.sup*y.inf;
					if (this.inf <= 0) { // 0 in x, 0 in y
						double lo = this.inf*y.sup;
						double ho = this.inf*y.inf;
						if (lo == lo && ho == ho) {
							if (l > lo)
								l = lo;
							if (h < ho)
								h = ho;
						} else {
							assignEntire();
							return this;
						}
					} // else x > 0, 0 in y
				} else { // this.sup >= 0, y > 0
					l = this.inf*(this.inf <= 0 ? y.sup : y.inf);
				}
			} else if (this.sup < 0) { // x < 0, y.sup >= 0
				l = this.inf*y.sup;
				h = (y.inf <= 0 ? this.inf : this.sup)*y.inf;
			} else { // x is [NaN,NaN]
				// inf = sup = Double.NaN;
				return this;
			}
		} else if (y.sup < 0) {	// y < 0
			if (this.sup >= 0) { // x.sup >= 0, y < 0
				l = this.sup*y.inf;
				h = this.inf*(this.inf <= 0 ? y.inf : y.sup);
			} else if (this.sup < 0) { // x < 0, y < 0
				l = this.sup*y.sup;
				h = this.inf*y.inf;
			} else { // x is [NaN,NaN]
				// inf = sup = Double.NaN
				return this;
			}
		} else { // y is [NaN,NaN]
			inf = sup = Double.NaN;
			return this;
		}
		if (l > 0)
			inf = prevPos(l);
		else if (l < 0)
			inf = prevNeg(l);
		else if (l == 0)
			inf = (this.inf >= 0 && y.inf >= 0 || this.sup <= 0 && y.sup <= 0) ? 0 : -Double.MIN_VALUE;
		else
			inf = Double.NEGATIVE_INFINITY;
		if (h > 0)
			sup = nextPos(h);
		else if (h < 0)
			sup = nextNeg(h);
		else if (h == 0)
			sup = (this.inf >= 0 && y.inf <= 0 || this.sup <= 0 && y.sup >= 0) ? 0 : Double.MIN_VALUE;
		else
			sup = Double.NEGATIVE_INFINITY;
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
		if (y.sup >= 0) {
			if (y.inf <= 0) { // 0 in y
				if (this.inf == this.inf) {
					assignEntire();
				}
				// else inf = sup = Double.NaN;
				return this;
			} else { // y > 0
				if (this.sup >= 0) { // x.sup >= 0, y > 0
					h = this.sup/y.inf;
					l = this.inf/(this.inf <= 0 ? y.inf : y.sup);
				} else if (this.sup < 0) { // x < 0, y > 0
					l = this.inf/y.inf;
					h = this.sup/y.sup;
				} else { // x is [NaN,NaN]
					// inf = sup = Double.NaN
					return this;
				}
			}
		} else if (y.sup < 0) {
			if (this.sup >= 0) { // x.sup >= 0, y < 0
				l = this.sup/y.sup;
				h = this.inf/(this.inf <= 0 ? y.sup : y.inf);
			} else if (this.sup < 0) { // x < 0, y < 0
				l = this.sup/y.inf;
				h = this.inf/y.sup;
			} else { // x is [NaN,NaN]
				// inf = sup = Double.NaN
				return this;
			}
		} else { // y is [NaN,NaN]
			inf = sup = Double.NaN;
			return this;
		}
		if (l > 0)
			inf = prevPos(l);
		else if (l < 0)
			inf = prevNeg(l);
		else if (l == 0)
			inf = (this.inf >= 0 && y.inf >= 0 || this.sup <= 0 && y.sup <= 0) ? 0 : -Double.MIN_VALUE;
		else
			inf = Double.NEGATIVE_INFINITY;
		if (h > 0)
			sup = nextPos(h);
		else if (h < 0)
			sup = nextNeg(h);
		else if (h == 0)
			sup = (this.inf >= 0 && y.inf <= 0 || this.sup <= 0 && y.sup >= 0) ? 0 : Double.MIN_VALUE;
		else
			sup = Double.NEGATIVE_INFINITY;
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

}
