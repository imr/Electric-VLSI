/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MutableInterval.java
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

/**
 *
 * @author  Dmitry Nadezhin
 */
public class UnivariateTaylorModel {
    private static double[] NULL_DOUBLE_ARRAY = {};
    private static MutableInterval ENTIRE = new MutableInterval().assignEntire();
    
    private final double coeff[];
    private final MutableInterval remainder;
    private final MutableInterval evalRemainder;
    
    private UnivariateTaylorModel(double[] coeff, double inf, double sup) {
        this.coeff = coeff;
        remainder = new MutableInterval(inf, sup);
        double evalRound = 0;
        evalRemainder = new MutableInterval(-evalRound, evalRound);
        evalRemainder.add(remainder);
    }
    
    private UnivariateTaylorModel(double[] coeff) {
        this.coeff = coeff;
        remainder = evalRemainder = ENTIRE;
    }
    
    public static UnivariateTaylorModel newInstance(double x) {
        double[] coeff = NULL_DOUBLE_ARRAY;
        double inf = 0;
        double sup = 0;
        if (Double.NEGATIVE_INFINITY < x && x < Double.POSITIVE_INFINITY) {
            coeff = new double[] { x };
        } else if (x == Double.POSITIVE_INFINITY) {
            coeff = new double[] { Double.MAX_VALUE };
            sup = x;
        } else if (x == Double.NEGATIVE_INFINITY) {
            inf = x;
            coeff = new double[] { -Double.MAX_VALUE };
        } else {
            inf = -Double.MAX_VALUE;
            sup = Double.MAX_VALUE;
        }
        return new UnivariateTaylorModel(coeff, inf, sup);
    }
    
    public static UnivariateTaylorModel newInstance(MutableInterval x) {
        return null;
    }

    public int size() { return coeff.length; }
    public double coeff(int k) { return k < coeff.length ? coeff[k] : 0; }
    public MutableInterval getRemainder() { return new MutableInterval(remainder); }
    
    public void eval(MutableInterval result, double t) {
        if (!(-1 <= t && t <= 1)) {
            result.assignEntire();
            return;
        }
        double value = 0;
        double sumAbs = 0;
        for (int k = coeff.length - 1; k >= 0; k--) {
            value = value*t + coeff[k];
            sumAbs += Math.abs(coeff[k]);
        }
        result.add(evalRemainder);
    }
    
    public void eval2(MutableInterval result, double t) {
        if (!(-1 <= t && t <= 1)) {
            result.assignEntire();
            return;
        }
        result.assign(0);
        MutableInterval ti = new MutableInterval(t);
        for (int k = coeff.length - 1; k >= 0; k--)
            result.mul(ti).add(new MutableInterval(coeff[k]));
        result.add(remainder);
    }
    
    public void eval3(MutableInterval result, double t) {
        if (!(-1 <= t && t <= 1)) {
            result.assignEntire();
            return;
        }
        BigDecimal tb = BigDecimal.valueOf(t);
        BigDecimal ib = BigDecimal.valueOf(0);
        BigDecimal sb = BigDecimal.valueOf(0);
        for (int k = coeff.length - 1; k >= 0; k--) {
            ib.multiply(tb);
            sb.multiply(tb);
            if (t < 0) {
                BigDecimal b = ib;
                ib = sb;
                sb = b;
            }
            BigDecimal cb = BigDecimal.valueOf(coeff[k]);
            ib.add(cb);
            sb.add(cb);
        }
        ib.add(new BigDecimal(remainder.inf()));
        sb.add(new BigDecimal(remainder.sup()));
        double inf = ib.doubleValue();
        if (!ib.equals(BigDecimal.valueOf(inf)))
            inf = MutableInterval.prev(inf);
        double sup = sb.doubleValue();
        if (!sb.equals(BigDecimal.valueOf(sup)))
            sup = MutableInterval.next(sup);
        result.assign(inf, sup);
    }
    
    public void eval3(MutableInterval[] results, double t) {
        if (!(-1 <= t && t <= 1)) {
            for (MutableInterval result: results)
                result.assignEntire();
            return;
        }
        BigDecimal tb = BigDecimal.valueOf(t);
        BigDecimal[] lb = new BigDecimal[results.length];
        BigDecimal[] hb = new BigDecimal[results.length];
        for (int i = 0; i < results.length; i++)
            lb[i] = hb[i] = BigDecimal.valueOf(0);
//        for (int k = coeff.length - 1; k >= 0; k--) {
//            lb.multiply(tb);
//            hb.multiply(tb);
//            if (t < 0) {
//                BigDecimal b = lb;
//                lb = hb;
//                hb = b;
//            }
//            BigDecimal cb = BigDecimal.valueOf(coeff[k]);
//            lb.add(cb);
//            hb.add(cb);
//        }
//        lb.add(new BigDecimal(remainder.inf()));
//        hb.add(new BigDecimal(remainder.sup()));
//        double inf = lb.doubleValue();
//        if (!lb.equals(BigDecimal.valueOf(inf)))
//            inf = MutableInterval.prev(inf);
//        double sup = hb.doubleValue();
//        if (!hb.equals(BigDecimal.valueOf(sup)))
//            sup = MutableInterval.next(sup);
//        result.assign(inf, sup);
    }
    
    public void evalAtZero(MutableInterval result) {
        if (coeff.length >= 1) {
            result.assign(coeff[0]);
            result.add(remainder);
        } else {
            result.assignEmpty();
        }
    }
    
    public void evalAtPlusOne(MutableInterval result) {
        result.assign(0);
        for (int k = coeff.length - 1; k >= 0; k--)
            result.add(coeff[k]);
        result.add(remainder);
    }
    
    public void evalAtMinusOne(MutableInterval result) {
        result.assign(0);
        for (int k = coeff.length - 1; k >= 0; k--)
            result.add((k & 1) == 0 ? coeff[k] : -coeff[k]);
        result.add(remainder);
    }
    
    public UnivariateTaylorModel negate() {
        double[] newCoeff = new double[coeff.length];
        for (int k = 0; k < coeff.length; k++)
            newCoeff[k] = -coeff[k];
        return new UnivariateTaylorModel(newCoeff, -remainder.sup(), -remainder.inf());
    }
    
    public UnivariateTaylorModel multiplyNaive(double x) {
        double[] newCoeff = new double[size()];
        for (int k = 0; k < newCoeff.length; k++)
            newCoeff[k] = coeff[k]*x;
        return new UnivariateTaylorModel(newCoeff);
    }
    
    public UnivariateTaylorModel addNaive(UnivariateTaylorModel x, int maxSize) {
        maxSize = Math.min(maxSize, Math.max(size(), x.size()));
        double[] newCoeff = new double[maxSize];
        for (int k = 0; k < newCoeff.length; k++)
            newCoeff[k] = coeff(k) + x.coeff(k);
        return new UnivariateTaylorModel(newCoeff);
    }
    
    public UnivariateTaylorModel subtractNaive(UnivariateTaylorModel x, int maxSize) {
        maxSize = Math.min(maxSize, Math.max(size(), x.size()));
        double[] newCoeff = new double[maxSize];
        for (int k = 0; k < newCoeff.length; k++)
            newCoeff[k] = coeff(k) + x.coeff(k);
        return new UnivariateTaylorModel(newCoeff);
    }
    
    public UnivariateTaylorModel multiplyNaive(UnivariateTaylorModel x, int maxSize) {
        maxSize = Math.min(maxSize, size() + x.size() - 1);
        if (x.size() < maxSize)
            return x.multiplyNaive(this, maxSize);
        double[] newCoeff = new double[maxSize];
        for (int k = 0; k < newCoeff.length; k++) {
            double s = 0;
            for (int l = 0; l < size(); l++)
                s += coeff(l)*x.coeff(k - l);
            newCoeff[k] = s;
        }
        return new UnivariateTaylorModel(newCoeff);
    }
    
    private UnivariateTaylorModel check() {
        for (double c: coeff)
            assert -Double.MAX_VALUE <= c && c <= Double.MAX_VALUE;
        return this;
    }
}
