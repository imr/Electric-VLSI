import sun.misc.FpUtils;
import java.lang.Math;
import java.util.List;
import java.util.ArrayList;

class Ulp
{
    private static final double ULP_EPS = 0x1.8p-53;
    private static final double MIN_NORMAL = 0x1.0p-1022;
    private static final double	MAX_ULP = 0x1.0p971;
 
    public static double ulp(double x) {
	if (x < 0)
	    x = -x;
	if (x < Double.MAX_VALUE) {
	    if (x >= MIN_NORMAL*2)
		return (x + x*ULP_EPS) - x;
	    return Double.MIN_VALUE;
	}
	if (x == Double.MAX_VALUE)
	    return MAX_ULP;
	return x;
    }

    private static final double two53 = 0x1.0p53;
    private static final double[] ulp_tab = {
	0x1.0p-1074, 0x1.0p-1074, 0x1.0p-1073, 0x1.0p-1072, 0x1.0p-1071, 0x1.0p-1070, 0x1.0p-1069, 0x1.0p-1068,
	0x1.0p-1067, 0x1.0p-1066, 0x1.0p-1065, 0x1.0p-1064, 0x1.0p-1063, 0x1.0p-1062, 0x1.0p-1061, 0x1.0p-1060,
	0x1.0p-1059, 0x1.0p-1058, 0x1.0p-1057, 0x1.0p-1056, 0x1.0p-1055, 0x1.0p-1054, 0x1.0p-1053, 0x1.0p-1052,
	0x1.0p-1051, 0x1.0p-1050, 0x1.0p-1049, 0x1.0p-1048, 0x1.0p-1047, 0x1.0p-1046, 0x1.0p-1045, 0x1.0p-1044,
	0x1.0p-1043, 0x1.0p-1042, 0x1.0p-1041, 0x1.0p-1040, 0x1.0p-1039, 0x1.0p-1038, 0x1.0p-1037, 0x1.0p-1036,
	0x1.0p-1035, 0x1.0p-1034, 0x1.0p-1033, 0x1.0p-1032, 0x1.0p-1031, 0x1.0p-1030, 0x1.0p-1029, 0x1.0p-1028,
	0x1.0p-1027, 0x1.0p-1026, 0x1.0p-1025, 0x1.0p-1024, 0x1.0p-1023, 0x1.0p-1022, 0x1.0p-1021, 0x1.0p-1020,
	0x1.0p-1019, 0x1.0p-1018, 0x1.0p-1017, 0x1.0p-1016, 0x1.0p-1015, 0x1.0p-1014, 0x1.0p-1013, 0x1.0p-1012 };

    public static double ulp1(double x) {
	if (x < 0)
	    x = -x;
	if (x < Double.MAX_VALUE) {
	    if (x >= MIN_NORMAL*two53)
		return (x + x*ULP_EPS) - x;
	    if (x < MIN_NORMAL*2)
		return Double.MIN_VALUE;
	    else {
		int mant = (int)(Double.doubleToLongBits(x) >> 52);
		return ulp_tab[mant & 0xff];
	    }
	}
	if (x == Double.MAX_VALUE)
	    return MAX_ULP;
	return x;
    }

    static abstract class Test {
	double x0, x1, x2, x3, x4, x5, x6, x7, x8, x9;
	double y0, y1, y2, y3, y4, y5, y6, y7, y8, y9;
	String name;

	Test(String name) {
	    this.name = name;
	}

	void cycle() {
	    long startTime = System.currentTimeMillis();
	    for (int i = 0; i < 1000*1000; i++) {
		calc();
	    }
	    long endTime = System.currentTimeMillis();
	    System.out.println("**** " + name + " took " + (endTime-startTime) + " nSec");
	    check();
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
		case 0: x0 = v; break;
		case 1: x1 = v; break;
		case 2: x2 = v; break;
		case 3: x3 = v; break;
		case 4: x4 = v; break;
		case 5: x5 = v; break;
		case 6: x6 = v; break;
		case 7: x7 = v; break;
		case 8: x8 = v; break;
		case 9: x9 = v; break;
		default: assert false;
	    }
	}

	void check() {
	    for (int i = 0; i < 10; i++) {
		double x = getX(i);
		double y = getY(i);
		long mathBits = Double.doubleToRawLongBits(Math.ulp(x));
		long thisBits = Double.doubleToRawLongBits(y);
		if (mathBits != thisBits)
		    System.out.println("!!!!!!\t"
			+ "x=" + x + "=" + Double.toHexString(x)
			+ " y=" + y + "=" + Double.toHexString(y)
			+ " ulp(x)=" + Double.toHexString(Math.ulp(x)));
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

    static class TestFpUtilsUlp extends Test {

	TestFpUtilsUlp() { super("FpUtils.ulp"); }

	void calc() {
	    y0 = FpUtils.ulp(x0);
	    y1 = FpUtils.ulp(x1);
	    y2 = FpUtils.ulp(x2);
	    y3 = FpUtils.ulp(x3);
	    y4 = FpUtils.ulp(x4);
	    y5 = FpUtils.ulp(x5);
	    y6 = FpUtils.ulp(x6);
	    y7 = FpUtils.ulp(x7);
	    y8 = FpUtils.ulp(x8);
	    y9 = FpUtils.ulp(x9);
	}
    }

    static class TestMathUlp extends Test {

	TestMathUlp() { super("Math.ulp"); }

	void calc() {
	    y0 = Math.ulp(x0);
	    y1 = Math.ulp(x1);
	    y2 = Math.ulp(x2);
	    y3 = Math.ulp(x3);
	    y4 = Math.ulp(x4);
	    y5 = Math.ulp(x5);
	    y6 = Math.ulp(x6);
	    y7 = Math.ulp(x7);
	    y8 = Math.ulp(x8);
	    y9 = Math.ulp(x9);
	}
    }

    static class TestPureUlp extends Test {

	TestPureUlp() { super("pure Java ulp"); }

	void calc() {
	    y0 = ulp(x0);
	    y1 = ulp(x1);
	    y2 = ulp(x2);
	    y3 = ulp(x3);
	    y4 = ulp(x4);
	    y5 = ulp(x5);
	    y6 = ulp(x6);
	    y7 = ulp(x7);
	    y8 = ulp(x8);
	    y9 = ulp(x9);
	}
    }

    static class TestPureUlp1 extends Test {

	TestPureUlp1() { super("pure Java ulp1"); }

	void calc() {
	    y0 = ulp1(x0);
	    y1 = ulp1(x1);
	    y2 = ulp1(x2);
	    y3 = ulp1(x3);
	    y4 = ulp1(x4);
	    y5 = ulp1(x5);
	    y6 = ulp1(x6);
	    y7 = ulp1(x7);
	    y8 = ulp1(x8);
	    y9 = ulp1(x9);
	}
    }

    public static void main (String argv[])
    {
	List<Test> tests = new ArrayList<Test>();
	tests.add(new TestFpUtilsUlp());
	tests.add(new TestMathUlp());
	tests.add(new TestPureUlp());
	tests.add(new TestPureUlp1());

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
