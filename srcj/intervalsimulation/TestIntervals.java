import sun.misc.FpUtils;
import java.lang.Math;
import java.util.List;
import java.util.ArrayList;
import com.sun.electric.tool.simulation.interval.Interval;
import net.sourceforge.interval.ia_math.RMath;
import net.sourceforge.interval.ia_math.RealInterval;
import net.sourceforge.interval.ia_math.IAMath;

class TestIntervals;
{
    private static final double MIN_NORMAL = 0x1.0p-1022;
 
    static abstract class Test {
	double x0, x1, x2, x3, x4, x5, x6, x7, x8, x9;
	double y0, y1, y2, y3, y4, y5, y6, y7, y8, y9;
	Interval mx0, mx1, mx2, mx3, mx4, mx5, mx6, mx7, mx8, mx9;
	Interval my0, my1, my2, my3, my4, my5, my6, my7, my8, my9;
	Interval madd = new Interval(-MIN_NORMAL,+MIN_NORMAL);
	su.nsk.nbsp.Interval nx0, nx1, nx2, nx3, nx4, nx5, nx6, nx7, nx8, nx9;
	su.nsk.nbsp.Interval ny0, ny1, ny2, ny3, ny4, ny5, ny6, ny7, ny8, ny9;
	su.nsk.nbsp.Interval nadd = new su.nsk.nbsp.Interval(-MIN_NORMAL,+MIN_NORMAL);
	String name;

	Test(String name) {
	    this.name = name;
	    my0 = new Interval();
	    my1 = new Interval();
	    my2 = new Interval();
	    my3 = new Interval();
	    my4 = new Interval();
	    my5 = new Interval();
	    my6 = new Interval();
	    my7 = new Interval();
	    my8 = new Interval();
	    my9 = new Interval();
	}

	void cycle() {
	    long startTime = System.currentTimeMillis();
	    for (int i = 0; i < 1000*1000; i++) {
		calc();
	    }
	    long endTime = System.currentTimeMillis();
	    System.out.println("**** " + name + " took " + (endTime-startTime) + " nSec");
	    // check();
	} 
	
	abstract void calc();

	double getX(int i) {
	    switch (i) {
		case 0: return x0;
		case 1: return x1;
		case 2: return x2;
		case 3: return x3;
		case 4: return x4;
		case 5: return x5;
		case 6: return x6;
		case 7: return x7;
		case 8: return x8;
		case 9: return x9;
		default: assert false; return 0;
	    }
	}

	double getY(int i) {
	    switch (i) {
		case 0: return y0;
		case 1: return y1;
		case 2: return y2;
		case 3: return y3;
		case 4: return y4;
		case 5: return y5;
		case 6: return y6;
		case 7: return y7;
		case 8: return y8;
		case 9: return y9;
		default: assert false; return 0;
	    }
	}

	void setX(int i, double v) {
	    switch (i) {
		case 0: x0 = v; mx0 = new Interval(v); nx0 = new su.nsk.nbsp.Interval(v); break;
		case 1: x1 = v; mx1 = new Interval(v); nx1 = new su.nsk.nbsp.Interval(v); break;
		case 2: x2 = v; mx2 = new Interval(v); nx2 = new su.nsk.nbsp.Interval(v); break;
		case 3: x3 = v; mx3 = new Interval(v); nx3 = new su.nsk.nbsp.Interval(v); break;
		case 4: x4 = v; mx4 = new Interval(v); nx4 = new su.nsk.nbsp.Interval(v); break;
		case 5: x5 = v; mx5 = new Interval(v); nx5 = new su.nsk.nbsp.Interval(v); break;
		case 6: x6 = v; mx6 = new Interval(v); nx6 = new su.nsk.nbsp.Interval(v); break;
		case 7: x7 = v; mx7 = new Interval(v); nx7 = new su.nsk.nbsp.Interval(v); break;
		case 8: x8 = v; mx8 = new Interval(v); nx8 = new su.nsk.nbsp.Interval(v); break;
		case 9: x9 = v; mx9 = new Interval(v); nx9 = new su.nsk.nbsp.Interval(v); break;
		default: assert false;
	    }
	}

	void check() {
	    for (int i = 0; i < 10; i++) {
		double x = getX(i);
		double y = getY(i);
		double z = su.nsk.nbsp.Functions.next(x);
		long mathBits = Double.doubleToRawLongBits(z);
		long thisBits = Double.doubleToRawLongBits(y);
		if (mathBits != thisBits)
		    System.out.println("!!!!!!\t"
			+ "x=" + x + "=" + Double.toHexString(x)
			+ " y=" + y + "=" + Double.toHexString(y)
			+ " next(x)=" + Double.toHexString(z));
	    }
	}

	// Test data

	void fillSuite() {
	    setX(0, 0.0);
	    setX(1, 1.0/3);
	    setX(2, -1.0);
	    setX(3, Double.NaN);
	    setX(4, -Double.POSITIVE_INFINITY);
	    setX(5, 0x1.fffffffffffffp1);
	    setX(6, -Math.E);
	    setX(7, Math.PI);
	    setX(8, -MIN_NORMAL*2.1*(1L << 52));
	    setX(9, Double.MAX_VALUE);
	}

	void fillHardSuite() {
	    setX(0, 0.0);
	    setX(1, 1.0/3);
	    setX(2, -1.0);
	    setX(3, Double.NaN);
	    setX(4, -Double.POSITIVE_INFINITY);
	    setX(5, 0x1.fffffffffffffp1);
	    setX(6, -Double.MIN_VALUE);
	    setX(7, Math.PI);
	    setX(8, -MIN_NORMAL*2.1);
	    setX(9, Double.MAX_VALUE);
	}

	void fillZeros() {
	    for (int j = 0; j < 10; j++)
		setX(j, 0.0);
	}

	void fillPI() {
	    for (int j = 0; j < 10; j++)
		setX(j, j%2 == 0 ? Math.PI : -Math.PI);
	}

	void fillMinValue() {
	    for (int j = 0; j < 10; j++)
		setX(j, j%2 == 0 ? Double.MIN_VALUE : -Double.MIN_VALUE);
	}

	void fillMaxValue() {
	    for (int j = 0; j < 10; j++)
		setX(j, j%2 == 0 ? Double.MAX_VALUE : -Double.MAX_VALUE);
	}

	void fillInfinity() {
	    for (int j = 0; j < 10; j++)
		setX(j, j%2 == 0 ? Double.POSITIVE_INFINITY : -Double.NEGATIVE_INFINITY);
	}

	void fillNaN() {
	    for (int j = 0; j < 10; j++)
		setX(j, Double.NaN);
	}

	void fillHard() {
	    for (int j = 0; j < 10; j++)
		setX(j, j%2 == 0 ? MIN_NORMAL*2.1 : -MIN_NORMAL*2.1);
	}

    }

    static class TestNskNext extends Test {

	TestNskNext() { super("nsk.next"); }

	void calc() {
	    y0 = su.nsk.nbsp.Functions.next(x0);
	    y1 = su.nsk.nbsp.Functions.next(x1);
	    y2 = su.nsk.nbsp.Functions.next(x2);
	    y3 = su.nsk.nbsp.Functions.next(x3);
	    y4 = su.nsk.nbsp.Functions.next(x4);
	    y5 = su.nsk.nbsp.Functions.next(x5);
	    y6 = su.nsk.nbsp.Functions.next(x6);
	    y7 = su.nsk.nbsp.Functions.next(x7);
	    y8 = su.nsk.nbsp.Functions.next(x8);
	    y9 = su.nsk.nbsp.Functions.next(x9);
	}
    }

    static class TestIntervalNext extends Test {

	TestIntervalNext() { super("Interval.next"); }

	void calc() {
	    y0 = Interval.next(x0);
	    y1 = Interval.next(x1);
	    y2 = Interval.next(x2);
	    y3 = Interval.next(x3);
	    y4 = Interval.next(x4);
	    y5 = Interval.next(x5);
	    y6 = Interval.next(x6);
	    y7 = Interval.next(x7);
	    y8 = Interval.next(x8);
	    y9 = Interval.next(x9);
	}
    }

    static class TestRMathNext extends Test {

	TestRMathNext() { super("RMath.next"); }

	void calc() {
	    y0 = RMath.nextfp(x0);
	    y1 = RMath.nextfp(x1);
	    y2 = RMath.nextfp(x2);
	    y3 = RMath.nextfp(x3);
	    y4 = RMath.nextfp(x4);
	    y5 = RMath.nextfp(x5);
	    y6 = RMath.nextfp(x6);
	    y7 = RMath.nextfp(x7);
	    y8 = RMath.nextfp(x8);
	    y9 = RMath.nextfp(x9);
	}
    }

    static class TestStrictMathExp extends Test {

	TestStrictMathExp() { super("StrictMath.exp"); }

	void calc() {
	    y0 = StrictMath.exp(x0);
	    y1 = StrictMath.exp(x1);
	    y2 = StrictMath.exp(x2);
	    y3 = StrictMath.exp(x3);
	    y4 = StrictMath.exp(x4);
	    y5 = StrictMath.exp(x5);
	    y6 = StrictMath.exp(x6);
	    y7 = StrictMath.exp(x7);
	    y8 = StrictMath.exp(x8);
	    y9 = StrictMath.exp(x9);
	}
    }

    static class TestMathExp extends Test {

	TestMathExp() { super("Math.exp"); }

	void calc() {
	    y0 = Math.exp(x0);
	    y1 = Math.exp(x1);
	    y2 = Math.exp(x2);
	    y3 = Math.exp(x3);
	    y4 = Math.exp(x4);
	    y5 = Math.exp(x5);
	    y6 = Math.exp(x6);
	    y7 = Math.exp(x7);
	    y8 = Math.exp(x8);
	    y9 = Math.exp(x9);
	}
    }

    static class TestRMathExpHi extends Test {

	TestRMathExpHi() { super("RMath.exp_hi"); }

	void calc() {
	    y0 = RMath.exp_hi(x0);
	    y1 = RMath.exp_hi(x1);
	    y2 = RMath.exp_hi(x2);
	    y3 = RMath.exp_hi(x3);
	    y4 = RMath.exp_hi(x4);
	    y5 = RMath.exp_hi(x5);
	    y6 = RMath.exp_hi(x6);
	    y7 = RMath.exp_hi(x7);
	    y8 = RMath.exp_hi(x8);
	    y9 = RMath.exp_hi(x9);
	}
    }

    static class TestArithExpHi extends Test {

	TestArithExpHi() { super("Arth.exppositive"); }

	void calc() {
	    y0 = su.nsk.nbsp.Arith.exppositive(x0);
	    y1 = su.nsk.nbsp.Arith.exppositive(x1);
	    y2 = su.nsk.nbsp.Arith.exppositive(x2);
	    y3 = su.nsk.nbsp.Arith.exppositive(x3);
	    y4 = su.nsk.nbsp.Arith.exppositive(x4);
	    y5 = su.nsk.nbsp.Arith.exppositive(x5);
	    y6 = su.nsk.nbsp.Arith.exppositive(x6);
	    y7 = su.nsk.nbsp.Arith.exppositive(x7);
	    y8 = su.nsk.nbsp.Arith.exppositive(x8);
	    y9 = su.nsk.nbsp.Arith.exppositive(x9);
	}
    }

    static class TestNewInterval extends Test {

	TestNewInterval() { super("new Interval"); }

	void calc() {
	    my0 = new Interval(x0);
	    my1 = new Interval(x1);
	    my2 = new Interval(x2);
	    my3 = new Interval(x3);
	    my4 = new Interval(x4);
	    my5 = new Interval(x5);
	    my6 = new Interval(x6);
	    my7 = new Interval(x7);
	    my8 = new Interval(x8);
	    my9 = new Interval(x9);
	}
    }

    static class TestAdd extends Test {

	TestAdd() { super("add"); }

	void calc() {
	    mx0.add(madd);
	    mx1.add(madd);
	    mx2.add(madd);
	    mx3.add(madd);
	    mx4.add(madd);
	    mx5.add(madd);
	    mx6.add(madd);
	    mx7.add(madd);
	    mx8.add(madd);
	    mx9.add(madd);
	}
    }

    static class TestNskAdd extends Test {

	TestNskAdd() { super("nsk.add"); }

	void calc() {
	    nx0.add(nadd);
	    nx1.add(nadd);
	    nx2.add(nadd);
	    nx3.add(nadd);
	    nx4.add(nadd);
	    nx5.add(nadd);
	    nx6.add(nadd);
	    nx7.add(nadd);
	    nx8.add(nadd);
	    nx9.add(nadd);
	}
    }

    static class TestAssign extends Test {

	TestAssign() { super("assign"); }

	void calc() {
	    my0.assign(mx0);
	    my1.assign(mx1);
	    my2.assign(mx2);
	    my3.assign(mx3);
	    my4.assign(mx4);
	    my5.assign(mx5);
	    my6.assign(mx6);
	    my7.assign(mx7);
	    my8.assign(mx8);
	    my9.assign(mx9);
	}
    }

    public static void main (String argv[])
    {
	List<Test> tests = new ArrayList<Test>();
	tests.add(new TestNskNext());
	tests.add(new TestIntervalNext());
	tests.add(new TestRMathNext());
	//	tests.add(new TestStrictMathExp());
	//	tests.add(new TestMathExp());
	//	tests.add(new TestRMathExpHi());
	//	tests.add(new TestArithExpHi());
	tests.add(new TestNewInterval());
	tests.add(new TestAdd());
	tests.add(new TestNskAdd());
	tests.add(new TestAssign());

	/* Warm */
	System.out.println("Warm");
	for (Test t: tests) t.fillSuite();
	for (Test t: tests) t.cycle();
	for (Test t: tests) t.cycle();

	System.out.println("Suite");
	for (Test t: tests) t.fillSuite();
	for (Test t: tests) t.cycle();

	System.out.println("Zeros");
	for (Test t: tests) t.fillZeros();
	for (Test t: tests) t.cycle();

	System.out.println("PI");
	for (Test t: tests) t.fillPI();
	for (Test t: tests) t.cycle();

	System.out.println("MinValue");
	for (Test t: tests) t.fillMinValue();
	for (Test t: tests) t.cycle();

	System.out.println("MaxValue");
	for (Test t: tests) t.fillMaxValue();
	for (Test t: tests) t.cycle();

	System.out.println("Infinity");
	for (Test t: tests) t.fillInfinity();
	for (Test t: tests) t.cycle();

	System.out.println("NaN");
	for (Test t: tests) t.fillNaN();
	for (Test t: tests) t.cycle();

	System.out.println("Hard");
	for (Test t: tests) t.fillHard();
	for (Test t: tests) t.cycle();

	System.out.println("HardSuite");
	for (Test t: tests) t.fillHardSuite();
	for (Test t: tests) t.cycle();

	System.out.println("Suite twice");
	for (Test t: tests) t.fillSuite();
	for (Test t: tests) t.cycle();
	for (Test t: tests) t.cycle();
    }
}
