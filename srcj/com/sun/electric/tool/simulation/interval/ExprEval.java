/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ExprEval.java
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

import com.sun.electric.tool.simulation.interval.MutableInterval;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * This class contains a set of equation. They can be evaluated in different
 * ways - as point value, as interval, gradients will be added also.
 * If-then-else expression can be used to represent piecewise smooth functions,
 * which are defined in different regions by different analitical expressions.
 */

public class ExprEval {

    private LinkedList<Expr> el = new LinkedList<Expr>();
    private boolean printDetailed = false;
    boolean hasBoth = false;

    public void printAll( boolean detailed )
    {
		printDetailed = detailed;
		for(Expr e : el) {
			//if (bp->_opref <= 0 || !detailed && (bp->_opref <= 1 || bp->isConst())) continue;
			System.out.println(/*"[" + e._opref + "] " +*/ (e.name != null ? e.name : "_") + "." + e.id + " = " + e);
		}
		printDetailed = false;
    }

    public void calcAll()
    {
		for (Expr e : el) {
			//if (bp->_opref == 0) continue;
			e.calcVal();
			if (false) {
				System.out.println(/*"[" + e._opref + "] " +*/ (e.name != null ? e.name : "_") + "." + e.id + " = " + e + " = " + e.valString());
			}
		}
    }

    public boolean calcIntervalAll()
    {
		hasBoth = false;
		//setFpu( true );
		for (Expr e : el) {
			//if (bp->_opref == 0) continue;
			e.calcInterval();
			if (false) {
				System.out.println(/*"[" + e._opref + "] " +*/ (e.name != null ? e.name : "_") + "." + e.id + " = " + e + " = " + e.intervalString());
			}
		}
		//setFpu( false );
		return hasBoth;
    }

    abstract class Expr {
		private String name;
		private int id;
		Expr() {
			id = el.size();;
			el.add(this);
		}
		String name() { return name; };
		public void setName(String name) { this.name = name; }
		public String toString() { return toString(0); }
		abstract String toString(int prio);
		String refString(int prio) {
			return (name != null ? name : "_") + "." + id;
		}
		abstract void calcVal();
		abstract void calcInterval();
		abstract String valString();
		abstract String intervalString();
    }

    public abstract class DoubleExpr extends Expr {
		private double v;
		MutableInterval iv;
		DoubleExpr() {
			v = 0.0;
			iv = new MutableInterval();
		}

		public double v() { return v; }

		public void setV(double v) {
			this.v = v;
		}

		final MutableInterval iv() { return iv; }

		public double inf() { return iv.inf(); }

		public double sup() { return iv.sup(); }

		public DoubleExpr add(DoubleExpr augend ) {
			return new DoubleBinopExpr(this, DoubleBinopExpr.BOP_PLUS, augend);
		}

		public DoubleExpr subtract(DoubleExpr subtrahend ) {
			return new DoubleBinopExpr(this, DoubleBinopExpr.BOP_MINUS, subtrahend);
		}

		public DoubleExpr multiply(DoubleExpr multiplicand ) {
			return new DoubleBinopExpr(this, DoubleBinopExpr.BOP_TIMES, multiplicand);
		}

		public DoubleExpr divide(DoubleExpr divisor ) {
			return new DoubleBinopExpr(this, DoubleBinopExpr.BOP_SLASH, divisor);
		}

		public DoubleExpr negate() {
			return new DoubleUnopExpr(DoubleUnopExpr.UOP_NEG, this);
		}

		public DoubleExpr exp() {
			return new DoubleFunExpr(DoubleFunExpr.FUN1_EXP, this);
		}

		public DoubleExpr log() {
			return new DoubleFunExpr(DoubleFunExpr.FUN1_LOG, this);
		}

		public DoubleExpr abs() {
			return new DoubleFunExpr(DoubleFunExpr.FUN1_ABS, this);
		}

		public BooleanExpr lt(DoubleExpr e) {
			return new BooleanCompExpr(this, BooleanCompExpr.BOP_LT, e);
		}

		public BooleanExpr gt(DoubleExpr e) {
			return new BooleanCompExpr(this, BooleanCompExpr.BOP_GT, e);
		}

		public BooleanExpr le(DoubleExpr e) {
			return new BooleanCompExpr(this, BooleanCompExpr.BOP_LE, e);
		}

		public BooleanExpr ge(DoubleExpr e) {
			return new BooleanCompExpr(this, BooleanCompExpr.BOP_GE, e);
		}

		String valString() {
			return "" + v(); // ???
		}

		String intervalString() {
			return iv().toString();
		}
    }

    public class DoubleConstExpr extends DoubleExpr {
		DoubleConstExpr(double v) {
			setV(v);
			this.iv.assign(v);
		}
		public void setInterval(double lo, double hi) {
			this.iv.assign(lo, hi);
		}
		String toString(int prio) {
			return "" + v(); // ???
		}
		void calcVal() {}
		void calcInterval() {}
    }

    class DoubleBinopExpr extends DoubleExpr {
		static final int BOP_PLUS = 1;
		static final int BOP_MINUS = 2;
		static final int BOP_TIMES = 3;
		static final int BOP_SLASH = 4;
		private DoubleExpr e1;
		private int bop;
		private DoubleExpr e2;

		DoubleBinopExpr(DoubleExpr e1, int bop, DoubleExpr e2) {
			this.e1 = e1;
			this.bop = bop;
			this.e2 = e2;
			calcVal();
		}

		String toString(int prio) {
			String s = null;
			int pr = 0;

			switch( bop )
			{
				case BOP_PLUS:
					s = " + ";
					pr = 3;
					break;
				case BOP_MINUS:
					s = " - ";
					pr = 3;
					break;
				case BOP_TIMES:
					s = " * ";
					pr = 4;
					break;
				case BOP_SLASH:
					s = " / ";
					pr = 4;
					break;
			}

			s = e1.refString(pr) + s + e2.refString(pr+1);
			if (prio > pr) s = "( " + s + " )";
			return s;
		}
	
		void calcVal() {
			switch( bop )
			{
				case BOP_PLUS: setV( e1.v() + e2.v() ); break;
				case BOP_MINUS: setV( e1.v() - e2.v() ); break;
				case BOP_TIMES: setV( e1.v() * e2.v() ); break;
				case BOP_SLASH: setV( e1.v() / e2.v() ); break;
			}
		}

		void calcInterval() {
			switch (bop)
			{
				case BOP_PLUS:
					this.iv.assign(e1.iv()).add( e2.iv() );
					break;
				case BOP_MINUS:
					this.iv.assign(e1.iv()).sub( e2.iv() );
					break;
				case BOP_TIMES:
					this.iv.assign(e1.iv()).mul( e2.iv() );
					break;
				case BOP_SLASH:
					this.iv.assign(e1.iv()).div( e2.iv() );
					break;
			}
		}
    }

    class DoubleUnopExpr extends DoubleExpr {
		static final int UOP_NEG = 1;
		private int uop;
		private DoubleExpr e1;

		DoubleUnopExpr(int uop, DoubleExpr e1) {
			this.uop = uop;
			this.e1 = e1;
			calcVal();
		}

		String toString(int prio) {
			String s = null;
			switch( uop )
			{
				case UOP_NEG: s = "- "; break;
			}
			s = s + e1.refString(4);
			if (prio > 3) s = "( " + s + " )";
			return s;
		}

		void calcVal() {
			switch( uop )
			{
				case UOP_NEG: setV( - e1.v() ); break;
			}
		}

		void calcInterval() {
			switch (uop)
			{
				case UOP_NEG:
					this.iv.assign(e1.iv()).negate();
					break;
			}
		}
    }

    class DoubleFunExpr extends DoubleExpr {
		static final int FUN1_EXP = 1;
		static final int FUN1_LOG = 2;
		static final int FUN1_ABS = 3;
		private int fun;
		private DoubleExpr e1;

		DoubleFunExpr(int fun, DoubleExpr e1) {
			this.fun = fun;
			this.e1 = e1;
			calcVal();
		}

		String toString(int prio) {
			String s = null;
			switch( fun )
			{
				case FUN1_EXP: s = "exp( "; break;
				case FUN1_LOG: s = "log( "; break;
				case FUN1_ABS: s = "abs( "; break;
			}
			s = s + e1.refString(0) + " )";
			return s;
		}

		void calcVal() {
			switch( fun )
			{
				case FUN1_EXP: setV( Math.exp( e1.v() ) ); break;
				case FUN1_LOG: setV( Math.log( e1.v() ) ); break;
				case FUN1_ABS: setV( Math.abs( e1.v() ) ); break;
			}
		}

		void calcInterval() {
			MutableInterval a = e1.iv();
			switch (fun)
			{
				case FUN1_EXP:
					this.iv.assign(a).exp();
					break;
				case FUN1_LOG:
					this.iv.assign(a).log();
					break;
				case FUN1_ABS:
					this.iv.assign(a).abs();
					break;
			}
		}
    }

    class DoubleIteExpr extends DoubleExpr {
		private BooleanExpr cond;
		private DoubleExpr th;
		private DoubleExpr el;

		DoubleIteExpr(BooleanExpr cond, DoubleExpr th, DoubleExpr el) {
			this.cond = cond;
			this.th = th;
			this.el = el;
			calcVal();
		}

		String toString(int prio) {
			String s = null;
			s = cond.refString(1) + " ? " + th.refString(1) + " : " + el.refString(0);
			if (prio > 0) s = "( " + s + " )";
			return s;
		}

		void calcVal() {
			setV (cond.v() ? th.v() : el.v() );
		}

		void calcInterval() {
			MutableInterval t = th.iv();
			MutableInterval e = el.iv();
			if (cond.iboth())
			{
				this.iv.assign(t).interval_hull(e);
				hasBoth = true;
			} else if (cond.v())
				this.iv.assign(t);
			else
				this.iv.assign(e);
		}
    }

    public abstract class BooleanExpr extends Expr {
		private boolean v;
		private boolean iboth;
		final boolean v() { return v; }
		final void setV(boolean v) { this.v = v; }
		final boolean iboth() { return iboth; }
		final void setIBoth(boolean v) { this.iboth = v; }

		public DoubleExpr ite(DoubleExpr th, DoubleExpr el) {
			return new DoubleIteExpr(this, th, el);
		}

		BooleanExpr or(BooleanExpr be) {
			return new BooleanBinopExpr(this, BooleanBinopExpr.BOP_OR, be);
		}

		BooleanBinopExpr and(BooleanExpr be) {
			return new BooleanBinopExpr(this, BooleanBinopExpr.BOP_AND, be);
		}

		String valString() {
			return "" + v(); // ???
		}
		String intervalString() {
			return (iboth() ? "both" : valString());
		}
    }

    class BooleanBinopExpr extends BooleanExpr {
		static final int BOP_AND = 1;
		static final int BOP_OR = 2;
		private BooleanExpr e1;
		private int bop;
		private BooleanExpr e2;
	
		BooleanBinopExpr(BooleanExpr e1, int bop, BooleanExpr e2) {
			this.e1 = e1;
			this.bop = bop;
			this.e2 = e2;
			calcVal();
		}

		String toString(int prio) {
			String s = null;
			switch( bop )
			{
				case BOP_AND: s = " && "; break;
				case BOP_OR : s = " || "; break;
			}
			s = e1.refString(2) + s + e2.refString(2);
			if (prio > 1) s = "( " + s + ") ";
			return s;
		}

		void calcVal() {
			switch( bop )
			{
				case BOP_AND: setV( e1.v() && e2.v() ); break;
				case BOP_OR: setV( e1.v() || e2.v() ); break;
			}
		}

		void calcInterval() {
			switch( bop )
			{
				case BOP_AND:
					if (e1.iboth() && e2.iboth() ||
						e1.iboth() && e2.v() ||
						e2.iboth() && e1.v())
					{
						setIBoth( true );
					} else
					{
						setIBoth( false );
						setV( e1.v() && e2.v() );
					}
					break;
				case BOP_OR:
					if (e1.iboth() && e2.iboth() ||
						e1.iboth() && !e2.v() ||
						e2.iboth() && !e1.v())
					{
						setIBoth( true );
					} else
					{
						setIBoth( false );
						setV( e1.v() || e2.v() );
					}
					break;
			}
		}
    }

    class BooleanCompExpr extends BooleanExpr {
		static final int BOP_LT = 1;
		static final int BOP_GT = 2;
		static final int BOP_LE = 3;
		static final int BOP_GE = 4;
		private DoubleExpr e1;
		private int bop;
		private DoubleExpr e2;

		BooleanCompExpr(DoubleExpr e1, int bop, DoubleExpr e2) {
			this.e1 = e1;
			this.bop = bop;
			this.e2 = e2;
			calcVal();
		}

		String toString(int prio) {
			String s = null;
			switch( bop )
			{
				case BOP_LT: s = " < "; break;
				case BOP_GT: s = " > "; break;
				case BOP_LE: s = " <= "; break;
				case BOP_GE: s = " >= "; break;
			}
			s = e1.refString(3) + s + e2.refString(3);
			if (prio > 2) s = "( " + s + ") ";
			return s;
		}

		void calcVal() {
			switch( bop )
			{
				case BOP_LT: setV( e1.v() < e2.v() ); break;
				case BOP_GT: setV( e1.v() > e2.v() ); break;
				case BOP_LE: setV( e1.v() <= e2.v() ); break;
				case BOP_GE: setV( e1.v() >= e2.v() ); break;
			}
		}

		void calcInterval() {
			boolean c = false; /* certainly */
			boolean p = false; /* possibly */
			MutableInterval l = e1.iv();
			MutableInterval r = e2.iv();
			switch( bop )
			{
				case BOP_LT:
					c = l.clt(r);
					p = l.plt(r);
					break;
				case BOP_GT:
					c = l.cgt(r);
					p = l.pgt(r);
					break;
				case BOP_LE:
					c = l.cle(r);
					p = l.ple(r);
					break;
				case BOP_GE:
					c = l.cge(r);
					p = l.pge(r);
					break;
			}
			setIBoth( c != p );
			setV( c );
		}
    }

    /* Static methods, creating expressions */

    public DoubleConstExpr newConst() {
		return new DoubleConstExpr(0.0);
    }

    public DoubleConstExpr newConst( double v ) {
		return new DoubleConstExpr(v);
    }
}
