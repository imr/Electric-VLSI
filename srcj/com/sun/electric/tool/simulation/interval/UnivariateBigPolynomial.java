/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UnivariateBigPolynomial.java
 *
 * Copyright (c) 2007 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
import java.math.BigInteger;
import java.util.Arrays;

/**
 * This immutable class is a polynomial with arbitrary length integer coefficiens
 * shifted by power of two.
 * <tt>P(t) = 2^(-scale) * SUM k: 0 <= k < size: coeff(k) * t^k</tt>
 * The purpose of this class is mostly for accuracy testing of other classes.
 * 
 * @author  Dmitry Nadezhin
 */
public class UnivariateBigPolynomial {
    private final BigInteger[] coeff;
    private final int scale;
    private BigDecimal pow5scale; // cache for 5^scale
    
    // Bits of Doubles
    /**
     * A constant holding minimal positive normalized double number.
     */
    private static final double MIN_NORMAL = Double.MIN_VALUE*(1L << 52); // 0x1.0p-1022;
    private static final long HIDDEN_MANTISSA_BIT = Double.doubleToLongBits(Double.MIN_NORMAL);
    private static final int MANTISSA_SIZE = Long.numberOfTrailingZeros(HIDDEN_MANTISSA_BIT);
    private static final long MANTISSA_MASK = HIDDEN_MANTISSA_BIT - 1;
    
    /**
     * Constructs UnivariateBigPolynomial from polynom part of UnivariateTaylorModel
     * @param tm UnivariateTaylorModel to construct from
     */
    public UnivariateBigPolynomial(UnivariateTaylorModel tm) {
        BigBinary[] bt = new BigBinary[tm.size()];
        for (int k = 0; k < tm.size(); k++)
            bt[k] = BigBinary.valueOf(tm.coeff(k));
        int maxScale = Integer.MIN_VALUE;
        for (int k = 0; k < tm.size(); k++) {
            BigBinary c = bt[k];
            if (c.signum() == 0) continue;
            assert c.unscaledValue().getLowestSetBit() == 0;
            maxScale = Math.max(maxScale, c.scale());
        }
        scale = maxScale;
        coeff = new BigInteger[tm.size()];
        for (int k = 0; k < tm.size(); k++) {
            BigBinary c = bt[k];
            coeff[k] = c.unscaledValue().shiftLeft(scale - c.scale());
        }
    }

    /**
     * Returns power of polynomial minus one
     * @return power of polynomial minus one
     */
    public int size() {
        return coeff.length;
    }
    
    /**
     * Returns scale of polynomial. Positive power means that
     * has fractianal binary digits
     * @return scale of polynomial
     */
    public int scale() {
        return scale;
    }
    
    /**
     * Returns unscaled cofficient before specified power of t as BigInteger
     * @param k specifies before which power of t is the coefficient
     * @return unscaled cofficient before specified power of t
     */
    public BigInteger coeffAsBigInteger(int k) {
        return k < coeff.length ? coeff[k] : BigInteger.ZERO;
    }
    
    /**
     * Returns caled cofficient before specified power of t as BigDecimal
     * @param k specifies before which power of t is the coefficoent
     * @return scaled cofficient before specified power of t
     */
    public BigDecimal coeffAsBigDecimal(int k) {
        BigInteger bi = coeffAsBigInteger(k);
        if (bi.signum() == 0)
            return BigDecimal.ZERO;
        if (scale <= 0)
            return new BigDecimal(bi.shiftLeft(-scale)).stripTrailingZeros();
        if (pow5scale == null)
            pow5scale = BigDecimal.valueOf(5).pow(scale);
        int n = bi.getLowestSetBit();
        return new BigDecimal(bi.shiftRight(n), scale - n).multiply(pow5scale);
    }
    
    public BigInteger evalPolyApprox(int vaccur, double t) {
        return evalPolyApprox(vaccur, BigBinary.valueOf(t));
    }
    
    public BigInteger[] evalPolyApprox(int nderiv, int vaccur, double t) {
        return evalPolyApprox(nderiv, vaccur, BigBinary.valueOf(t));
    }
    
    public BigInteger evalPolyApprox(int vaccur, BigBinary t) {
        return evalPolyApprox(1, vaccur, t)[0];
    }
    
    public BigInteger[] evalPolyApprox(int nderiv, int vaccur, BigBinary t) {
        if (nderiv < 0)
            throw new IllegalArgumentException();
        BigInteger tUnscaledValue = t.unscaledValue();
        int tScale = t.scale();
        vaccur = Math.max(vaccur, 0);
        BigInteger[] v = new BigInteger[nderiv];
        for (int p = 0; p < v.length; p++)
            v[p] = BigInteger.ZERO;
        int ts = tUnscaledValue.getLowestSetBit();
        if (ts < 0) {
            if (size() != 0)
                v[0] = coeffAsBigInteger(0);
            return v;
        }
        ts = Math.min(ts, tScale);
        tUnscaledValue.shiftRight(ts);
        tScale -= ts;
        assert tScale >= 0;
        if (tUnscaledValue.abs().compareTo(BigInteger.ONE.shiftLeft(tScale)) > 0)
            throw new IllegalArgumentException("Out of Range");
        for (int k = size() - 1; k >= 0; k--) {
            BigInteger c = coeffAsBigInteger(k).shiftLeft(vaccur);
            for (int p = 0; p <= k && p < v.length; p++) {
                v[p] = tUnscaledValue.multiply(v[p]);
                boolean addBit = tScale > 0 && v[p].testBit(tScale - 1);
                v[p] = v[p].shiftRight(tScale);
                if (addBit)
                    v[p] = v[p].add(BigInteger.ONE);
                v[p] = v[p].add(c);
                c = BigInteger.valueOf(k - p).multiply(c);
            }
        }
        for (int p = 0; p < v.length; p++) {
            boolean addBit = vaccur > 0 && v[p].testBit(vaccur - 1);
            v[p] = v[p].shiftRight(1);
            if (addBit)
                v[p] = v[p].add(BigInteger.ONE);
        }
        return v;
    }
    
    public double evalPolyApproxError(int p, int vaccur) {
        int error = Math.max(size() - p - 1, 0);
        return 0.5*(vaccur <= 0 ? error : 1 + error / (double)(1L << vaccur));
    }
    
    public void evalPolyExact(BigBinary[] v, double t) {
        evalPolyExact(v, BigBinary.valueOf(t));
    }
    
    public void evalPolyExact(BigBinary[] results, BigBinary t) {
        if (results.length == 0) return;
        BigInteger tUnscaledValue = t.unscaledValue();
        int tScale = t.scale();
        Arrays.fill(results, BigBinary.ZERO);
        int ts = tUnscaledValue.getLowestSetBit();
        if (ts < 0) {
            if (size() == 0) return;
            BigInteger c = coeffAsBigInteger(0);
            int cs = c.getLowestSetBit();
            if (cs >= 0)
                results[0] = new BigBinary(c.shiftRight(cs), scale - cs);
            return;
        }
        BigInteger[] v = new BigInteger[results.length];
        Arrays.fill(v, BigInteger.ZERO);
        int[] vscale = new int[results.length];
        ts = Math.min(ts, tScale);
        tUnscaledValue.shiftRight(ts);
        tScale -= ts;
        assert tScale >= 0;
        for (int k = size() - 1; k >= 0; k--) {
            BigInteger c = coeffAsBigInteger(k);
            for (int p = 0; p <= k && p < v.length; p++) {
                v[p] = tUnscaledValue.multiply(v[p]);
                vscale[p] += tScale;
                v[p] = v[p].add(c.shiftLeft(vscale[p]));
                int vs = v[p].getLowestSetBit();
                if (vs >= 0) {
                    v[p] = v[p].shiftRight(vs);
                    vscale[p] -= vs;
                } else {
                    vscale[p] = 0;
                }
                c = BigInteger.valueOf(k - p).multiply(c);
            }
        }
        for (int p = 0; p < results.length; p++)
            results[p] = new BigBinary(v[p], vscale[p]);
    }
    
    public BigDecimal evalPolyExact(BigDecimal t) {
        BigDecimal[] results = new BigDecimal[1];
        evalPolyExact(results, t);
        return results [0];
    }
    
    public void evalPolyExact(BigDecimal[] results, BigDecimal t) {
        t = t.stripTrailingZeros();
        for (int p = 0; p < results.length; p++)
            results[p] = BigDecimal.ZERO;
        for (int k = size() - 1; k >= 0; k--) {
            BigDecimal c = coeffAsBigDecimal(k);
            for (int p = 0; p <= k && p < results.length; p++) {
                results[p] = t.multiply(results[p]).add(c).stripTrailingZeros();
                c = BigDecimal.valueOf(k - p).multiply(c);
            }
        }
    }
}
