import com.sun.electric.tool.simulation.interval.Interval;
import java.lang.Math;
import java.util.Random;
import java.math.BigDecimal;
import java.math.MathContext;

class Sum {

    private static Random rand = new Random(1234567);
    private static double mean = 0.0;
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
	double sh;
	double sl;
	double norm;
	int n;
	String name;

	Accumulator(String name) {
	    this.name = name;
	}

	String name() {
	    return name;
	}

	abstract void add(double v);
	double sum() { return sh; }
	double suml() { return sl; }
	double errSup() { return sl; }
	double errInf() { return sl; }

	double sum(double[] v) {
	    for (int i = 0; i < v.length; i++)
		add(v[i]);
	    return sum();
	}

    }

    static class Add extends Accumulator {

	Add() {
	    super("add");
	}

	void add(double v) {
	    sh += v;
	}

	double sum(double[] v) {
	    for (int i = 0; i < v.length; i++)
		add(v[i]);
	    return sum();
	}

	double errInf() {
	    return Double.NaN;
	}

	double errSup() {
	    return Double.NaN;
	}
    }

    static class Add0 extends Accumulator {

	Add0() {
	    super("add0");
	}

	void add(double v) {
	    sh += v;
	    norm += (sh >= 0 ? sh : -sh);
	    n++;
	}

	double sum(double[] v) {
	    for (int i = 0; i < v.length; i++)
		add(v[i]);
	    return sum();
	}

	double errInf() {
	    return - norm*(1.0 + n*0x1.0p-53)*0x1.0p-53;
	}

	double errSup() {
	    return + norm*(1.0 + n*0x1.0p-53)*0x1.0p-53;
	}
    }

    static class Add1 extends Accumulator {

	Add1() {
	    super("add1");
	}

	void add(double v) {
	    norm += (v >= 0 ? v : -v);
	    double x = sh + v;
	    sl += (sh - x) + v;
	    sh = x;
	    n++;
	}

	double sum(double[] v) {
	    for (int i = 0; i < v.length; i++)
		add(v[i]);
	    return sum();
	}

	double errInf() {
	    return sl - norm*(1.0 + n*0x1.0p-53)*0x1.0p-53;
	}

	double errSup() {
	    return sl + norm*(1.0 + n*0x1.0p-53)*0x1.0p-53;
	}
    }

    static class Add2 extends Accumulator {

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
	    norm += (sl >= 0 ? sl : -sl);
	    sh = x;
	    n++;
	}

	double sum(double[] v) {
	    for (int i = 0; i < v.length; i++)
		add(v[i]);
	    return sum();
	}

	double errInf() {
	    return sl - norm*(1.0 + n*0x1.0p-53)*0x1.0p-53;
	}

	double errSup() {
	    return sl + norm*(1.0 + n*0x1.0p-53)*0x1.0p-53;
	}
    }

    static class AddM extends Accumulator {
	Interval s = new Interval(0);
	Interval vv = new Interval();
	AddM() {
	    super("addm");
	}

	void add(double v) {
	    s.add(vv.assign(v));
	}

	double sum() {
	    return s.mid();
	}

	double sum(double[] v) {
	    for (int i = 0; i < v.length; i++)
		add(v[i]);
	    return sum();
	}
    }

    private double val, vall, errInf = 0, errSup = 0;

    private void sum() {
	Add s = new Add();
	val = s.sum(v);
	vall = s.suml();
	errInf = s.errInf();
	errSup = s.errSup();
    }

    private void sum0() {
	Add0 s = new Add0();
	val = s.sum(v);
	vall = s.suml();
	errInf = s.errInf();
	errSup = s.errSup();
    }

    private void dsum() {
	Add1 s = new Add1();
	val = s.sum(v);
	vall = s.suml();
	errInf = s.errInf();
	errSup = s.errSup();
    }

    private void dsum2() {
	Add2 s = new Add2();
	val = s.sum(v);
	vall = s.suml();
	errInf = s.errInf();
	errSup = s.errSup();
    }

    private void msum() {
	Interval s = new Interval(0);
	Interval vv = new Interval();
	for (int i = 0; i < v.length; i++) {
	    vv.assign(v[i]);
	    s.add(vv);
	}
	val = s.mid();
	vall = 0;
	errInf = s.inf() - val;
	errSup = s.sup() - val;
    }
    
    private void nsum() {
	su.nsk.nbsp.Interval s = new su.nsk.nbsp.Interval(0);
	su.nsk.nbsp.Interval vv = new su.nsk.nbsp.Interval();
	for (int i = 0; i < v.length; i++) {
	    vv.assign(v[i]);
	    s.add(vv);
	}
	val = s.mid();
	vall = 0;
	errInf = s.inf() - val;
	errSup = s.sup() - val;
    }
    
    public String toString() {
	return (val-bsh+vall-bsl)+ " ["+(val-bsh+errInf-bsl)+","+(val-bsh+errSup-bsl)+"]";
    }

    public static void main (String argv[])
    {
	long startTime = System.currentTimeMillis();
	for (int i = 0; i < v.length; i++)
	    v[i] = rand.nextGaussian() + mean;
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
	    s.sum0();
	    s.dsum();
	    s.dsum2();
	    s.msum();
	    s.nsum();
	}
	System.out.println("warm");

	long startTime2 = System.currentTimeMillis();
	for (int i = 0; i < 100; i++)
	    s.sum();
	long endTime2 = System.currentTimeMillis();
	System.out.println("**** sum took " + (endTime2-startTime2) + " mSec =" + s);

	long startTime2a = System.currentTimeMillis();
	for (int i = 0; i < 100; i++)
	    s.sum0();
	long endTime2a = System.currentTimeMillis();
	System.out.println("**** sum0 took " + (endTime2a-startTime2a) + " mSec =" + s);

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
    }
}
