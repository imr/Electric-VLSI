import com.sun.electric.tool.simulation.interval.FloatingDecimal;
import java.lang.Math;
import java.math.BigDecimal;

class TestFloatingDecimal {
    String s;
    BigDecimal bd;

    double[] myDouble = new double[8];
    boolean exceptDouble;
    double defDouble;
    double stdDouble;

    float[] myFloat = new float[8];
    boolean exceptFloat;
    float defFloat;
    float stdFloat;

    TestFloatingDecimal(String s) {
	this.s = s;
	bd = new BigDecimal(s);

	FloatingDecimal fd = FloatingDecimal.readJavaFormatString(s);
	for (int i = 0; i < 8; i++) {
	    try {
		myDouble[i] = fd.doubleValue(i);
	    } catch (ArithmeticException e) {
		assert i == 7;
		exceptDouble = true;
	    }
	    defDouble = fd.doubleValue();
	    stdDouble = Double.parseDouble(s);

	    try {
		myFloat[i] = fd.floatValue(i);
	    } catch (ArithmeticException e) {
		assert i == 7;
		exceptFloat = true;
	    }
	    defFloat = fd.floatValue();
	    stdFloat = Float.parseFloat(s);
	}
	//show();
	check();
    }

    void check() {
	boolean error = false;

	if (exceptDouble) {
	    double l = myDouble[BigDecimal.ROUND_FLOOR];
	    double h = myDouble[BigDecimal.ROUND_CEILING];
	    long lx = Double.doubleToLongBits(l);
	    long hx = Double.doubleToLongBits(h);
	    BigDecimal lb = l != Double.NEGATIVE_INFINITY ?
		new BigDecimal(l) :
		(new BigDecimal(-Double.MAX_VALUE)).subtract(new BigDecimal(Math.ulp(Double.MAX_VALUE)));
	    BigDecimal hb = h != Double.POSITIVE_INFINITY ?
		new BigDecimal(h) :
		(new BigDecimal(Double.MAX_VALUE)).add(new BigDecimal(Math.ulp(Double.MAX_VALUE)));
	    BigDecimal ld = bd.subtract(lb);
	    BigDecimal hd = hb.subtract(bd);
	    if (l != l || h != h) {
		System.out.println("Double NaN");
		error = true;
	    }
	    if ((lx >= 0) != (hx >=0)) {
		System.out.println("Double Different signs");
		error = true;
	    }
	    if ((hx - lx) != (hx >= 0 ? 1 : -1)) {
		System.out.println("Double Not an ulp");
		error = true;
	    }
	    if (l != Double.NEGATIVE_INFINITY && ld.signum() <= 0) {
		System.out.println("Double Inf bad");
		error = true;
	    }
	    if (h != Double.POSITIVE_INFINITY && hd.signum() <= 0) {
		System.out.println("Double Sup bad");
		error = true;
	    }
	    double x;
	    x = myDouble[BigDecimal.ROUND_UP];
	    if (x != (bd.signum() > 0 ? h : l)) {
		System.out.println("Double ROUND_UP");
		error = true;
	    }
	    x = myDouble[BigDecimal.ROUND_DOWN];
	    if (x != (bd.signum() > 0 ? l : h)) {
		System.out.println("Double ROUND_DOWN");
		error = true;
	    }
	    int diff = ld.compareTo(hd);
	    x = myDouble[BigDecimal.ROUND_HALF_UP];
	    if (x != ((bd.signum() > 0 ? diff >= 0 : diff > 0) ? h : l)) {
		System.out.println("Double ROUND_HALF_UP");
		error = true;
	    }
	    x = myDouble[BigDecimal.ROUND_HALF_DOWN];
	    if (x != ((bd.signum() > 0 ? diff > 0 : diff >= 0) ? h : l)) {
		System.out.println("Double ROUND_HALF_DOWN");
		error = true;
	    }
	    x = myDouble[BigDecimal.ROUND_HALF_EVEN];
	    if (diff == 0) {
		if (x != ((lx&1) == 0 ? l : h)) {
		    System.out.println("Double ROUND_HALF_EVEN");
		    error = true;
		}
	    } else if (x != (diff > 0 ? h : l)) {
		System.out.println("Double ROUND_HALF_EVEN");
		error = true;
	    }
	    if (defDouble != x) {
		System.out.println("defDouble");
		error = true;
	    }
	    if (stdDouble != x) {
		System.out.println("stdDouble");
		error = true;
	    }
	} else {
	    double d = myDouble[BigDecimal.ROUND_UNNECESSARY];
	    if (d != d) {
		System.out.println("NaN");
		error = true;
	    }
	    if (Double.isInfinite(d)) {
		System.out.println("Is infinity");
		error = true;
	    }
	    if (bd.compareTo(new BigDecimal(d)) != 0) {
		System.out.println("d="+d);
		error = true;
	    }
	    for (int i = 0; i < 8; i++) {
		if (Double.doubleToRawLongBits(d) != Double.doubleToRawLongBits(myDouble[i])) {
		    System.out.println(s + " myDouble["+i+"]="+myDouble[i]);
		    error = true;
		};
	    }
	    if (defDouble != d) {
		System.out.println("defDouble");
		error = true;
	    }
	    if (stdDouble != d) {
		System.out.println("stdDouble");
		error = true;
	    }
	}

	if (exceptFloat) {
	    float l = myFloat[BigDecimal.ROUND_FLOOR];
	    float h = myFloat[BigDecimal.ROUND_CEILING];
	    int lx = Float.floatToIntBits(l);
	    int hx = Float.floatToIntBits(h);
	    BigDecimal lb = l != Float.NEGATIVE_INFINITY ?
		new BigDecimal(l) :
		(new BigDecimal(-Float.MAX_VALUE)).subtract(new BigDecimal(Math.ulp(Float.MAX_VALUE)));
	    BigDecimal hb = h != Float.POSITIVE_INFINITY ?
		new BigDecimal(h) :
		(new BigDecimal(Float.MAX_VALUE)).add(new BigDecimal(Math.ulp(Float.MAX_VALUE)));
	    BigDecimal ld = bd.subtract(lb);
	    BigDecimal hd = hb.subtract(bd);
	    if (l != l || h != h) {
		System.out.println("Float NaN");
		error = true;
	    }
	    if ((lx >= 0) != (hx >=0)) {
		System.out.println("Float Different signs");
		error = true;
	    }
	    if ((hx - lx) != (hx >= 0 ? 1 : -1)) {
		System.out.println("Float Not an ulp");
		error = true;
	    }
	    if (l != Float.NEGATIVE_INFINITY && ld.signum() <= 0) {
		System.out.println("Float Inf bad");
		error = true;
	    }
	    if (h != Float.POSITIVE_INFINITY && hd.signum() <= 0) {
		System.out.println("Float Sup bad");
		error = true;
	    }
	    float x;
	    x = myFloat[BigDecimal.ROUND_UP];
	    if (x != (bd.signum() > 0 ? h : l)) {
		System.out.println("Float ROUND_UP");
		error = true;
	    }
	    x = myFloat[BigDecimal.ROUND_DOWN];
	    if (x != (bd.signum() > 0 ? l : h)) {
		System.out.println("Float ROUND_DOWN");
		error = true;
	    }
	    int diff = ld.compareTo(hd);
	    x = myFloat[BigDecimal.ROUND_HALF_UP];
	    if (x != ((bd.signum() > 0 ? diff >= 0 : diff > 0) ? h : l)) {
		System.out.println("Float ROUND_HALF_UP");
		error = true;
	    }
	    x = myFloat[BigDecimal.ROUND_HALF_DOWN];
	    if (x != ((bd.signum() > 0 ? diff > 0 : diff >= 0) ? h : l)) {
		System.out.println("Float ROUND_HALF_DOWN");
		error = true;
	    }
	    x = myFloat[BigDecimal.ROUND_HALF_EVEN];
	    if (diff == 0) {
		if (x != ((lx&1) == 0 ? l : h)) {
		    System.out.println("Float ROUND_HALF_EVEN");
		    error = true;
		}
	    } else if (x != (diff > 0 ? h : l)) {
		System.out.println("Float ROUND_HALF_EVEN");
		error = true;
	    }
	    if (defFloat != x) {
		System.out.println("defFloat");
		error = true;
	    }
	    if (stdFloat != x) {
		System.out.println("stdFloat");
		error = true;
	    }
	} else {
	    float d = myFloat[BigDecimal.ROUND_UNNECESSARY];
	    if (d != d) {
		System.out.println("Float NaN");
		error = true;
	    }
	    if (Float.isInfinite(d)) {
		System.out.println("Float infinity");
		error = true;
	    }
	    if (bd.compareTo(new BigDecimal(d)) != 0) {
		System.out.println("f="+d);
		error = true;
	    }
	    for (int i = 0; i < 8; i++) {
		if (Float.floatToRawIntBits(d) != Float.floatToRawIntBits(myFloat[i])) {
		    System.out.println(s + " myFloat["+i+"]="+myFloat[i]);
		    error = true;
		};
	    }
	    if (defFloat != d) {
		System.out.println("defFloat");
		error = true;
	    }
	    if (stdFloat != d) {
		System.out.println("stdFloat");
		error = true;
	    }
	}

	if (error)
	    show();
    }

    void show() {
	System.out.println("s="+s);

	if (exceptDouble)
	    System.out.println("\tDouble inexact");
	for (int i = 0; i < 8; i++) {
	    System.out.println("\t"+i+" "+myDouble[i]);
	}
	System.out.println("\tdefDouble="+defDouble);
	System.out.println("\tstdDouble="+stdDouble);

	if (exceptFloat)
	    System.out.println("\tFloat inexact");
	for (int i = 0; i < 8; i++) {
	    System.out.println("\t"+i+" "+myFloat[i]);
	}
	System.out.println("\tdefFloat="+defFloat);
	System.out.println("\tstdFloat="+stdFloat);
    }

    public static void main(String[] args) {
	String ss[] = {
	    "1.7976931348623160e+308",
	    "1.7976931348623159e+308",
	    "1.7976931348623158e+308",
	    "1.7976931348623157e+308",
	    "0.3e-45",
	    "0.7e-45",
	    "1.4e-45",
	    "1.5e-324",
	    "3.1e-324",
	    "4.9e-324",
	    "0",
	    "-0",
	    "1",
	    "2",
	    "3",
	    "-3",
	    "3e100",
	    "0.5",
	    "3.1231431231231892e100",
	    "0.1",
	    "0.2",
	    "0.3",
	    "-0.3",
	    "3e-100",
	    "1e-309",
	    "720579591101481e2"
	};
	for (int i = 0; i < ss.length; i++) {
	    String s = ss[i];
	    new TestFloatingDecimal(s);
	}
	//perform();
    }

    static void perform() {
	//String s = "3.1231431231231892e100";
	//String s = "3e-100";
	//String s = "1.23e36";
	String s = "1.23e37";
	//String s = "1.23e-21";
	//String s = "3.1231431234e100";
	//String s = "0.3";
	
	FloatingDecimal fd = FloatingDecimal.readJavaFormatString(s);
	for (int i = 0; i < 100000; i++) {
	    fd.doubleValue();
	    fd.doubleValue(BigDecimal.ROUND_HALF_EVEN);
	}
	System.out.println("Warm");
	double d = 0;

	long startTime1 = System.currentTimeMillis();
	for (int i = 0; i < 1000000; i++) {
	    d = fd.doubleValue();
	}
	long endTime1 = System.currentTimeMillis();
	System.out.println("**** doubleValue() took " + (endTime1-startTime1) + " mSec d=" + d);

	long startTime2 = System.currentTimeMillis();
	for (int i = 0; i < 1000000; i++) {
	    d = fd.doubleValue(BigDecimal.ROUND_HALF_EVEN);
	}
	long endTime2 = System.currentTimeMillis();
	System.out.println("**** doubleValue() took " + (endTime2-startTime2) + " mSec d=" + d);
    }
}
