import net.sourceforge.interval.ia_math.*;
//import java.lang.Double;
//import Jama.util.Maths;
//import java.io.*;
import java.util.*;
//import java.text.DecimalFormat;

/**
 * This class contains a set of equation. They can be evaluated in different
 * ways - as point value, as interval, gradients will be added also.
 * If-then-else expression can be used to represent piecewise smooth functions,
 * which are defined in different regions by different analitical expressions.
 */

class ExprEval {

    private LinkedList el = new LinkedList();
    private boolean printDetailed = false;
    boolean hasBoth = false;

    void printAll( boolean detailed )
    {
	printDetailed = detailed;
	for(Iterator itr = el.iterator(); itr.hasNext();) {
	    Expr e = (Expr)itr.next();
	    //if (bp->_opref <= 0 || !detailed && (bp->_opref <= 1 || bp->isConst())) continue;
	    System.out.println(/*"[" + e._opref + "] " +*/ (e.name != null ? e.name : "_") + "." + e.id + " = " + e);
	}
	printDetailed = false;
    }

    void calcAll()
    {
	for (Iterator itr = el.iterator(); itr.hasNext();) {
	    Expr e = (Expr)itr.next();
	    //if (bp->_opref == 0) continue;
	    e.calcVal();
	    if (false) {
		System.out.println(/*"[" + e._opref + "] " +*/ (e.name != null ? e.name : "_") + "." + e.id + " = " + e + " = " + e.valString());
	    }
	}
    }

    boolean calcIntervalAll()
    {
	hasBoth = false;
	//setFpu( true );
	for (Iterator itr = el.iterator(); itr.hasNext();) {
	    Expr e = (Expr)itr.next();
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
	void setName(String name) { this.name = name; }
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

    abstract class DoubleExpr extends Expr {
	private double v;
	private RealInterval iv;
	DoubleExpr() {
	    v = 0.0;
	    iv = new RealInterval();
	}
	final double v() { return v; }
	final void setV(double v) {
	    this.v = v;
	}
	final RealInterval iv() { return iv; }
	final void setIV(RealInterval iv) {
	    this.iv = iv;
	}
	String valString() {
	    return "" + v(); // ???
	}
	String intervalString() {
	    return iv().toString();
	}
    }

    class DoubleConstExpr extends DoubleExpr {
	DoubleConstExpr(double v) {
	    setV(v);
	    setIV(new RealInterval(v));
	}
	void setInterval(double lo, double hi) {
	    setIV(new RealInterval(lo,hi));
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
		    setIV( IAMath.add(e1.iv(), e2.iv()) );
		    break;
		case BOP_MINUS:
		    setIV( IAMath.sub(e1.iv(), e2.iv()) );
		    break;
		case BOP_TIMES:
		    setIV( IAMath.mul(e1.iv(), e2.iv()) );
		    break;
		case BOP_SLASH:
		    setIV( IAMath.odiv(e1.iv(), e2.iv()) );
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
		    setIV( IAMath.uminus(e1.iv()) );
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
	    RealInterval a = e1.iv();
	    switch (fun)
	    {
		case FUN1_EXP:
		    setIV( IAMath.exp( a ) );
		    break;
		case FUN1_LOG:
		    if (a.hi() <= 0.0)
			setIV( RealInterval.fullInterval() );
		    setIV( IAMath.log( a ) );
		    break;
		case FUN1_ABS:
		    if (a.lo() >= 0.0)
			setIV( a );
		    else if (a.hi() <= 0.0)
			setIV( IAMath.uminus(a) );
		    else
			setIV( new RealInterval(0.0, Math.max(-a.lo(),a.hi())) );
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
	    RealInterval t = th.iv();
	    RealInterval e = el.iv();
	    if (cond.iboth())
	    {
		setIV( new RealInterval(Math.min(t.lo(),e.lo()), Math.max(t.hi(),e.hi())) );
		hasBoth = true;
	    } else if (cond.v())
		setIV( t );
	    else
		setIV( e );
	}
    }

    abstract class BooleanExpr extends Expr {
	private boolean v;
	private boolean iboth;
	final boolean v() { return v; }
	final void setV(boolean v) { this.v = v; }
	final boolean iboth() { return iboth; }
	final void setIBoth(boolean v) { this.iboth = v; }
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
	    RealInterval l = e1.iv();
	    RealInterval r = e2.iv();
	    switch( bop )
	    {
		case BOP_LT:
		    c = l.hi() < r.lo();
		    p = l.lo() < r.hi();
		    break;
		case BOP_GT:
		    c = l.lo() > r.hi();
		    p = l.hi() > r.lo();
		    break;
		case BOP_LE:
		    c = l.hi() <= r.lo();
		    p = l.lo() <= r.hi();
		    break;
		case BOP_GE:
		    c = l.lo() >= r.hi();
		    p = l.hi() >= r.lo();
		    break;
	    }
	    setIBoth( c != p );
	    setV( c );
	}
    }

    /* Static methods, creating expressions */

    DoubleConstExpr newConst() {
	return new DoubleConstExpr(0.0);
    }

    DoubleConstExpr newConst( double v ) {
	return new DoubleConstExpr(v);
    }

    DoubleBinopExpr newPlus( DoubleExpr e1, DoubleExpr e2 ) {
	return new DoubleBinopExpr(e1, DoubleBinopExpr.BOP_PLUS, e2);
    }

    DoubleBinopExpr newMinus( DoubleExpr e1, DoubleExpr e2 ) {
	return new DoubleBinopExpr(e1, DoubleBinopExpr.BOP_MINUS, e2);
    }

    DoubleBinopExpr newTimes( DoubleExpr e1, DoubleExpr e2 ) {
	return new DoubleBinopExpr(e1, DoubleBinopExpr.BOP_TIMES, e2);
    }

    DoubleBinopExpr newSlash( DoubleExpr e1, DoubleExpr e2 ) {
	return new DoubleBinopExpr(e1, DoubleBinopExpr.BOP_SLASH, e2);
    }

    DoubleUnopExpr newNegate( DoubleExpr e1) {
	return new DoubleUnopExpr(DoubleUnopExpr.UOP_NEG, e1);
    }

    DoubleFunExpr newExp( DoubleExpr e1) {
	return new DoubleFunExpr(DoubleFunExpr.FUN1_EXP, e1);
    }

    DoubleFunExpr newLog( DoubleExpr e1) {
	return new DoubleFunExpr(DoubleFunExpr.FUN1_LOG, e1);
    }

    DoubleFunExpr newAbs( DoubleExpr e1) {
	return new DoubleFunExpr(DoubleFunExpr.FUN1_ABS, e1);
    }

    DoubleIteExpr newIte( BooleanExpr cond, DoubleExpr th, DoubleExpr el) {
	return new DoubleIteExpr(cond, th, el);
    }

    BooleanBinopExpr newOr( BooleanExpr e1, BooleanExpr e2) {
	return new BooleanBinopExpr(e1, BooleanBinopExpr.BOP_OR, e2);
    }

    BooleanBinopExpr newAnd( BooleanExpr e1, BooleanExpr e2) {
	return new BooleanBinopExpr(e1, BooleanBinopExpr.BOP_AND, e2);
    }

    BooleanCompExpr newLt( DoubleExpr e1, DoubleExpr e2) {
	return new BooleanCompExpr(e1, BooleanCompExpr.BOP_LT, e2);
    }

    BooleanCompExpr newGt( DoubleExpr e1, DoubleExpr e2) {
	return new BooleanCompExpr(e1, BooleanCompExpr.BOP_GT, e2);
    }

    BooleanCompExpr newLe( DoubleExpr e1, DoubleExpr e2) {
	return new BooleanCompExpr(e1, BooleanCompExpr.BOP_LE, e2);
    }

    BooleanCompExpr newGe( DoubleExpr e1, DoubleExpr e2) {
	return new BooleanCompExpr(e1, BooleanCompExpr.BOP_GE, e2);
    }
}
