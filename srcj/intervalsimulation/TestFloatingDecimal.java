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
    double[] nskDouble = new double[2];

    float[] myFloat = new float[8];
    boolean exceptFloat;
    float defFloat;
    float stdFloat;
    float[] nskFloat = new float[2];

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
	    su.nsk.nbsp.Interval d = new su.nsk.nbsp.Interval("["+s+"]");
	    nskDouble[0] = d.inf();
	    nskDouble[1] = d.sup();

	    try {
		myFloat[i] = fd.floatValue(i);
	    } catch (ArithmeticException e) {
		assert i == 7;
		exceptFloat = true;
	    }
	    defFloat = fd.floatValue();
	    stdFloat = Float.parseFloat(s);
	    su.nsk.nbsp.Intervalf f = new su.nsk.nbsp.Intervalf("["+s+"]");
	    nskFloat[0] = f.inf();
	    nskFloat[1] = f.sup();
	}
	check();
	//show();
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
		(new BigDecimal(Double.MAX_VALUE/2)).multiply(new BigDecimal(-2));
	    BigDecimal hb = h != Double.POSITIVE_INFINITY ?
		new BigDecimal(h) :
		(new BigDecimal(Double.MAX_VALUE/2)).multiply(new BigDecimal(2));
	    BigDecimal ld = bd.subtract(lb);
	    BigDecimal hd = hb.subtract(bd);
	    if ((lx >= 0) != (hx >=0)) {
		System.out.println("Different signs");
		error = true;
	    }
	    if ((hx - lx) != (hx >= 0 ? 1 : -1)) {
		System.out.println("Not an ulp");
		error = true;
	    }
	    if (l != Double.NEGATIVE_INFINITY && ld.signum() <= 0) {
		System.out.println("Inf bad");
		error = true;
	    }
	    if (h != Double.POSITIVE_INFINITY && hd.signum() <= 0) {
		System.out.println("Sup bad");
		error = true;
	    }
	    double x;
	    x = myDouble[BigDecimal.ROUND_UP];
	    if (x != (bd.signum() > 0 ? h : l)) {
		System.out.println("ROUND_UP");
		error = true;
	    }
	    x = myDouble[BigDecimal.ROUND_DOWN];
	    if (x != (bd.signum() > 0 ? l : h)) {
		System.out.println("ROUND_DOWN");
		error = true;
	    }
	    int diff = ld.compareTo(hd);
	    x = myDouble[BigDecimal.ROUND_HALF_UP];
	    if (x != ((bd.signum() > 0 ? diff >= 0 : diff <= 0) ? h : l)) {
		System.out.println("ROUND_HALF_UP");
		error = true;
	    }
	    x = myDouble[BigDecimal.ROUND_HALF_DOWN];
	    if (x != ((bd.signum() > 0 ? diff > 0 : diff < 0) ? h : l)) {
		System.out.println("ROUND_HALF_UP");
		error = true;
	    }
	    x = myDouble[BigDecimal.ROUND_HALF_EVEN];
	    if (diff == 0) {
		if (x != ((lx&1) == 0 ? l : h)) {
		    System.out.println("ROUND_HALF_EVEN");
		    error = true;
		}
	    } else if (x != ((bd.signum() > 0 ? diff > 0: diff < 0) ? h : l)) {
		System.out.println("ROUND_HALF_EVEN");
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
// 	    if (nskDouble[0] != myDouble[BigDecimal.ROUND_FLOOR]) {
// 		System.out.println("nsk.inf");
// 		error = true;
// 	    }
// 	    if (nskDouble[1] != myDouble[BigDecimal.ROUND_CEILING]) {
// 		System.out.println("nsk.sup");
// 		error = true;
// 	    }
	} else {
	    double d = myDouble[BigDecimal.ROUND_UNNECESSARY];
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
// 	    if (nskDouble[0] != d) {
// 		System.out.println("nsk.inf");
// 		error = true;
// 	    }
// 	    if (nskDouble[1] != d) {
// 		System.out.println("nsk.sup");
// 		error = true;
// 	    }
	}

	if (exceptFloat) {
	    //show();
	    float l = myFloat[BigDecimal.ROUND_FLOOR];
	    float h = myFloat[BigDecimal.ROUND_CEILING];
	    int lx = Float.floatToIntBits(l);
	    int hx = Float.floatToIntBits(h);
	    BigDecimal lb = l != Float.NEGATIVE_INFINITY ?
		new BigDecimal(l) :
		(new BigDecimal(Float.MAX_VALUE/2)).multiply(new BigDecimal(-2));
	    BigDecimal hb = h != Float.POSITIVE_INFINITY ?
		new BigDecimal(h) :
		(new BigDecimal(Float.MAX_VALUE/2)).multiply(new BigDecimal(2));
	    BigDecimal ld = bd.subtract(lb);
	    BigDecimal hd = hb.subtract(bd);
	    if ((lx >= 0) != (hx >=0)) {
		System.out.println("Different signs");
		error = true;
	    }
	    if ((hx - lx) != (hx >= 0 ? 1 : -1)) {
		System.out.println("Not an ulp");
		error = true;
	    }
	    if (l != Float.NEGATIVE_INFINITY && ld.signum() <= 0) {
		System.out.println("Inf bad");
		error = true;
	    }
	    if (h != Float.POSITIVE_INFINITY && hd.signum() <= 0) {
		System.out.println("Sup bad");
		error = true;
	    }
	    float x;
	    x = myFloat[BigDecimal.ROUND_UP];
	    if (x != (bd.signum() > 0 ? h : l)) {
		System.out.println("ROUND_UP");
		error = true;
	    }
	    x = myFloat[BigDecimal.ROUND_DOWN];
	    if (x != (bd.signum() > 0 ? l : h)) {
		System.out.println("ROUND_DOWN");
		error = true;
	    }
	    int diff = ld.compareTo(hd);
	    x = myFloat[BigDecimal.ROUND_HALF_UP];
	    if (x != ((bd.signum() > 0 ? diff >= 0 : diff <= 0) ? h : l)) {
		System.out.println("ROUND_HALF_UP");
		error = true;
	    }
	    x = myFloat[BigDecimal.ROUND_HALF_DOWN];
	    if (x != ((bd.signum() > 0 ? diff > 0 : diff < 0) ? h : l)) {
		System.out.println("ROUND_HALF_UP");
		error = true;
	    }
	    x = myFloat[BigDecimal.ROUND_HALF_EVEN];
	    if (diff == 0) {
		if (x != ((lx&1) == 0 ? l : h)) {
		    System.out.println("ROUND_HALF_EVEN");
		    error = true;
		}
	    } else if (x != ((bd.signum() > 0 ? diff > 0: diff < 0) ? h : l)) {
		System.out.println("ROUND_HALF_EVEN");
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
// 	    if (nskFloat[0] != myFloat[BigDecimal.ROUND_FLOOR]) {
// 		System.out.println("nsk.inf");
// 		error = true;
// 	    }
// 	    if (nskFloat[1] != myFloat[BigDecimal.ROUND_CEILING]) {
// 		System.out.println("nsk.sup");
// 		error = true;
// 	    }
	} else {
	    float d = myFloat[BigDecimal.ROUND_UNNECESSARY];
	    if (Float.isInfinite(d)) {
		System.out.println("Is infinity");
		error = true;
	    }
	    if (bd.compareTo(new BigDecimal(d)) != 0) {
		System.out.println("d="+d);
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
// 	    if (nskFloat[0] != d) {
// 		System.out.println("nsk.inf");
// 		error = true;
// 	    }
// 	    if (nskFloat[1] != d) {
// 		System.out.println("nsk.sup");
// 		error = true;
// 	    }
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
	System.out.println("\tnskDouble=["+nskDouble[0]+","+nskDouble[1]+"]");

	if (exceptFloat)
	    System.out.println("\tFloat inexact");
	for (int i = 0; i < 8; i++) {
	    System.out.println("\t"+i+" "+myFloat[i]);
	}
	System.out.println("\tdefFloat="+defFloat);
	System.out.println("\tstdFloat="+stdFloat);
	System.out.println("\tnskFloat=["+nskFloat[0]+","+nskFloat[1]+"]");
    }

    public static void main(String[] args) {
	String ss[] = { "0", "1", "2", "3", "3e100", "0.5", "3.1231431231231892e100", "0.1", "0.2", "0.3", "3e-100", "720579591101481e2" };
	for (int i = 0; i < ss.length; i++) {
	    String s = ss[i];
	    new TestFloatingDecimal(s);
	}
// 	long l0 = 720579591101481l;
// 	long l = l0*100;
// 	double d = sun.misc.FloatingDecimal.readJavaFormatString(s).doubleValue();
// 	float v1 = sun.misc.FloatingDecimal.readJavaFormatString(s).floatValue();
// 	float v2 = v1 + Math.ulp(v1);

// 	System.out.println("l=" + l + "=0x" + Long.toHexString(l));
// 	System.out.println("s=" + s);
// 	System.out.println("d =" + Double.toHexString(d));
// 	System.out.println("v1=" + Float.toHexString(v1) + " |l-v1|=" + Math.abs(l-(long)v1));
// 	System.out.println("v2=" + Float.toHexString(v2) + " |l-v2|=" + Math.abs(l-(long)v2));
	
// 	System.out.println("FloatingDecimal returns v1, but v2 is nearer");
    }

    static void perform() {
	String s = "3.1231431231231892e100";
	//String s = "3e-100";
	//String s = "1.23e36";
	//String s = "1.23e37";
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
