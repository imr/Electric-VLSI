import com.sun.electric.tool.simulation.interval.Interval;
import net.sourceforge.interval.ia_math.RMath;
import net.sourceforge.interval.ia_math.RealInterval;
import net.sourceforge.interval.ia_math.IAMath;
import su.nsk.nbsp.Functions;
import java.lang.Math;
import java.util.Random;
import java.math.BigDecimal;
import java.math.MathContext;

class Sum {

    private static Random rand = new Random(1234567);
    private static final int n = 1000000;
    private static double[] v = new double[n];
    private static double bsh, bsl;
    private static final double powK = 0x1.0p140;

    private static void bsum() {
	BigDecimal s = BigDecimal.ZERO;
	for (int i = 0; i < v.length; i++)
	    s = s.add(new BigDecimal(Math.rint(v[i]*powK)));
	bsh = s.doubleValue()/powK;
	s = s.subtract(new BigDecimal(Math.rint(bsh*powK)));
	bsl = s.doubleValue()/powK;
    }

    static abstract class Accumulator {
	int n = 0;
	String name;

	Accumulator(String name) {
	    this.name = name;
	}

	String name() {
	    return name;
	}

	abstract void add(double v);
	abstract double sum();
	double suml() { return 0; }

	double sum(double[] v) {
	    for (int i = 0; i < v.length; i++)
		add(v[i]);
	    return sum();
	}

    }

    static class Add0 extends Accumulator {
	private double s = 0;

	Add0() {
	    super("add0");
	}

	void add(double v) {
	    s += v;
	}

	double sum() { return s; }

	double sum(double[] v) {
	    for (int i = 0; i < v.length; i++)
		add(v[i]);
	    return sum();
	}
    }

    static class Add1 extends Accumulator {
	private double sh = 0, sl = 0;

	Add1() {
	    super("add1");
	}

	void add(double v) {
	    double x = sl + v;
	    double y = sh + x;
	    sl = (sh - y) + x;
	    sh = y;
	}

	double sum() { return sh; }
	double suml() { return sl; }

	double sum(double[] v) {
	    for (int i = 0; i < v.length; i++)
		add(v[i]);
	    return sum();
	}
    }

    static class Add2 extends Accumulator {
	private double sh = 0, sl = 0;

	Add2() {
	    super("add2");
	}

	void add(double v) {
	    double l;
	    double x = sh + v;
	    if (v > 0) {
		if (sh > v || -sh > v)
		    sl += (sh - x) + v;
		else
		    sl += (v - x) + sh;
	    } else {
		if (sh < v || -sh < v)
		    sl += (sh - x) + v;
		else
		    sl += (v - x) + sh;
	    }
	    sh = x;
	}

	double sum() { return sh; }
	double suml() { return sl; }

	double sum(double[] v) {
	    for (int i = 0; i < v.length; i++)
		add(v[i]);
	    return sum();
	}
    }

    static class Add3 extends Accumulator {
	private double sh = 0, sl = 0;

	Add3() {
	    super("add3");
	}

	void add(double v) {
	    double x = sh + v;
	    sl += (sh - x) + v;
	    sh = x;
	}

	double sum() { return sh; }
	double suml() { return sl; }

	double sum(double[] v) {
	    for (int i = 0; i < v.length; i++)
		add(v[i]);
	    return sum();
	}
    }

    private double inf, sup, infl = 0, supl = 0;

    private void sum() {
	Add0 s = new Add0();
	inf = sup = s.sum(v);
	infl = supl = s.suml();
    }

    private void dsum() {
	Add1 s = new Add1();
	inf = sup = s.sum(v);
	infl = supl = s.suml();
    }

    private void dsum2() {
	Add2 s = new Add2();
	inf = sup = s.sum(v);
	infl = supl = s.suml();
    }

    private void dsum3() {
	Add3 s = new Add3();
	inf = sup = s.sum(v);
	infl = supl = s.suml();
    }

    private void msum() {
	Interval s = new Interval(0);
	Interval vv = new Interval();
	for (int i = 0; i < v.length; i++) {
	    vv.assign(v[i]);
	    s.add(vv);
	}
	inf = s.inf();
	sup = s.sup();
    }
    
    private void nsum() {
	su.nsk.nbsp.Interval s = new su.nsk.nbsp.Interval(0);
	su.nsk.nbsp.Interval vv = new su.nsk.nbsp.Interval();
	for (int i = 0; i < v.length; i++) {
	    vv.assign(v[i]);
	    s.add(vv);
	}
	inf = s.inf();
	sup = s.sup();
    }
    
    private void tsum() {
	RealInterval s = new RealInterval(0);
	for (int i = 0; i < v.length; i++) {
	    RealInterval vv = new RealInterval(v[i]);
	    s = IAMath.add(s, vv);
	}
	inf = s.lo();
	sup = s.hi();
    }
    
    public String toString() {
	return bsh + "+" + "["+(inf-bsh+infl-bsl)+","+(sup-bsh+supl-bsl)+"]";
    }

    public static void main (String argv[])
    {
	long startTime = System.currentTimeMillis();
	for (int i = 0; i < v.length; i++)
	    v[i] = rand.nextGaussian();
	long endTime = System.currentTimeMillis();
	System.out.println("**** Random took " + (endTime-startTime) + " mSec");

	long startTime1 = System.currentTimeMillis();
	bsum();
	long endTime1 = System.currentTimeMillis();
	System.out.println("**** bsum took " + (endTime1-startTime1) + " mSec =" + bsh + " " + bsl + " ulp=" + Interval.ulp(bsh));

	Sum s = new Sum();

	/* Warm */
	for (int i = 0; i < 10; i++) {
	    s.sum();
	    s.dsum();
	    s.dsum2();
	    s.dsum3();
	    s.msum();
	    s.nsum();
	    s.tsum();
	}
	System.out.println("warm");

	long startTime2a = System.currentTimeMillis();
	for (int i = 0; i < 100; i++)
	    s.sum();
	long endTime2a = System.currentTimeMillis();
	System.out.println("**** sum took " + (endTime2a-startTime2a) + " mSec =" + s);

	long startTime2b = System.currentTimeMillis();
	for (int i = 0; i < 100; i++)
	    s.dsum();
	long endTime2b = System.currentTimeMillis();
	System.out.println("**** dsum took " + (endTime2b-startTime2b) + " mSec =" + s);

	long startTime2c = System.currentTimeMillis();
	for (int i = 0; i < 100; i++)
	    s.dsum2();
	long endTime2c = System.currentTimeMillis();
	System.out.println("**** dsum2 took " + (endTime2c-startTime2c) + " mSec =" + s);

	long startTime2d = System.currentTimeMillis();
	for (int i = 0; i < 100; i++)
	    s.dsum3();
	long endTime2d = System.currentTimeMillis();
	System.out.println("**** dsum3 took " + (endTime2d-startTime2d) + " mSec =" + s);

	long startTime3 = System.currentTimeMillis();
	for (int i = 0; i < 100; i++)
	    s.msum();
	long endTime3 = System.currentTimeMillis();
	System.out.println("**** mSum took " + (endTime3-startTime3) + " mSec =" + s);

	long startTime4 = System.currentTimeMillis();
	for (int i = 0; i < 100; i++)
	    s.nsum();
	long endTime4 = System.currentTimeMillis();
	System.out.println("**** nSum took " + (endTime4-startTime4) + " mSec =" + s);

	long startTime5 = System.currentTimeMillis();
	for (int i = 0; i < 100; i++)
	    s.tsum();
	long endTime5 = System.currentTimeMillis();
	System.out.println("**** tSum took " + (endTime5-startTime5) + " mSec =" + s);

    }
}
