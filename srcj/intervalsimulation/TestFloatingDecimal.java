import com.sun.electric.tool.simulation.interval.FloatingDecimal;
import com.sun.electric.tool.simulation.interval.DirectedFloatingDecimal;
import java.lang.Math;
import java.math.BigDecimal;
import java.math.BigInteger;

class TestFloatingDecimal {
    String s;
    BigDecimal bd;

    double[] myDouble = new double[8];
    boolean exceptDouble;
    double stdDouble;

    float[] myFloat = new float[8];
    boolean exceptFloat;
    float stdFloat;

    TestFloatingDecimal(String s) {
	this.s = s;
	bd = new BigDecimal(s);

	FloatingDecimal fd = FloatingDecimal.readJavaFormatString(s);
	for (int mode = 0; mode < 8; mode++) {
	    try {
		myDouble[mode] = fd.doubleValue(mode);
	    } catch (ArithmeticException e) {
		assert mode == BigDecimal.ROUND_UNNECESSARY;
		exceptDouble = true;
	    }
	    stdDouble = Double.parseDouble(s);

	    try {
		myFloat[mode] = fd.floatValue(mode);
	    } catch (ArithmeticException e) {
		assert mode == BigDecimal.ROUND_UNNECESSARY;
		exceptFloat = true;
	    }
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
		System.out.println("!!! Double NaN");
		error = true;
	    }
	    if ((lx >= 0) != (hx >=0)) {
		System.out.println("!!! Double Different signs");
		error = true;
	    }
	    if ((hx - lx) != (hx >= 0 ? 1 : -1)) {
		System.out.println("!!! Double Not an ulp");
		error = true;
	    }
	    if (l != Double.NEGATIVE_INFINITY && ld.signum() <= 0) {
		System.out.println("!!! Double Inf bad");
		error = true;
	    }
	    if (h != Double.POSITIVE_INFINITY && hd.signum() <= 0) {
		System.out.println("!!! Double Sup bad");
		error = true;
	    }
	    double x;
	    x = myDouble[BigDecimal.ROUND_UP];
	    if (x != (bd.signum() > 0 ? h : l)) {
		System.out.println("!!! Double ROUND_UP");
		error = true;
	    }
	    x = myDouble[BigDecimal.ROUND_DOWN];
	    if (x != (bd.signum() > 0 ? l : h)) {
		System.out.println("!!! Double ROUND_DOWN");
		error = true;
	    }
	    int diff = ld.compareTo(hd);
	    x = myDouble[BigDecimal.ROUND_HALF_UP];
	    if (x != ((bd.signum() > 0 ? diff >= 0 : diff > 0) ? h : l)) {
		System.out.println("!!! Double ROUND_HALF_UP");
		error = true;
	    }
	    x = myDouble[BigDecimal.ROUND_HALF_DOWN];
	    if (x != ((bd.signum() > 0 ? diff > 0 : diff >= 0) ? h : l)) {
		System.out.println("!!! Double ROUND_HALF_DOWN");
		error = true;
	    }
	    x = myDouble[BigDecimal.ROUND_HALF_EVEN];
	    if (diff == 0) {
		if (x != ((lx&1) == 0 ? l : h)) {
		    System.out.println("!!! Double ROUND_HALF_EVEN");
		    error = true;
		}
	    } else if (x != (diff > 0 ? h : l)) {
		System.out.println("!!! Double ROUND_HALF_EVEN");
		error = true;
	    }
	    if (stdDouble != x) {
		System.out.println("!!! stdDouble");
		error = true;
	    }
	} else {
	    double d = myDouble[BigDecimal.ROUND_UNNECESSARY];
	    if (d != d) {
		System.out.println("!!! NaN");
		error = true;
	    }
	    if (Double.isInfinite(d)) {
		System.out.println("!!! Is infinity");
		error = true;
	    }
	    if (bd.compareTo(new BigDecimal(d)) != 0) {
		System.out.println("!!! d="+d);
		error = true;
	    }
	    for (int i = 0; i < 8; i++) {
		if (Double.doubleToRawLongBits(d) != Double.doubleToRawLongBits(myDouble[i])) {
		    System.out.println("!!! " + s + " myDouble["+i+"]="+myDouble[i]);
		    error = true;
		};
	    }
	    if (stdDouble != d) {
		System.out.println("!!! stdDouble");
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
		System.out.println("!!! Float NaN");
		error = true;
	    }
	    if ((lx >= 0) != (hx >=0)) {
		System.out.println("!!! Float Different signs");
		error = true;
	    }
	    if ((hx - lx) != (hx >= 0 ? 1 : -1)) {
		System.out.println("!!! Float Not an ulp");
		error = true;
	    }
	    if (l != Float.NEGATIVE_INFINITY && ld.signum() <= 0) {
		System.out.println("!!! Float Inf bad");
		error = true;
	    }
	    if (h != Float.POSITIVE_INFINITY && hd.signum() <= 0) {
		System.out.println("!!! Float Sup bad");
		error = true;
	    }
	    float x;
	    x = myFloat[BigDecimal.ROUND_UP];
	    if (x != (bd.signum() > 0 ? h : l)) {
		System.out.println("!!! Float ROUND_UP");
		error = true;
	    }
	    x = myFloat[BigDecimal.ROUND_DOWN];
	    if (x != (bd.signum() > 0 ? l : h)) {
		System.out.println("!!! Float ROUND_DOWN");
		error = true;
	    }
	    int diff = ld.compareTo(hd);
	    x = myFloat[BigDecimal.ROUND_HALF_UP];
	    if (x != ((bd.signum() > 0 ? diff >= 0 : diff > 0) ? h : l)) {
		System.out.println("!!! Float ROUND_HALF_UP");
		error = true;
	    }
	    x = myFloat[BigDecimal.ROUND_HALF_DOWN];
	    if (x != ((bd.signum() > 0 ? diff > 0 : diff >= 0) ? h : l)) {
		System.out.println("!!! Float ROUND_HALF_DOWN");
		error = true;
	    }
	    x = myFloat[BigDecimal.ROUND_HALF_EVEN];
	    if (diff == 0) {
		if (x != ((lx&1) == 0 ? l : h)) {
		    System.out.println("!!! Float ROUND_HALF_EVEN");
		    error = true;
		}
	    } else if (x != (diff > 0 ? h : l)) {
		System.out.println("!!! Float ROUND_HALF_EVEN");
		error = true;
	    }
	    if (stdFloat != x) {
		System.out.println("!!! stdFloat");
		error = true;
	    }
	} else {
	    float d = myFloat[BigDecimal.ROUND_UNNECESSARY];
	    if (d != d) {
		System.out.println("!!! Float NaN");
		error = true;
	    }
	    if (Float.isInfinite(d)) {
		System.out.println("!!! Float infinity");
		error = true;
	    }
	    if (bd.compareTo(new BigDecimal(d)) != 0) {
		System.out.println("!!! f="+d);
		error = true;
	    }
	    for (int i = 0; i < 8; i++) {
		if (Float.floatToRawIntBits(d) != Float.floatToRawIntBits(myFloat[i])) {
		    System.out.println("!!! " + s + " myFloat["+i+"]="+myFloat[i]);
		    error = true;
		};
	    }
	    if (stdFloat != d) {
		System.out.println("!!! stdFloat");
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
	System.out.println("\tstdDouble="+stdDouble);

	if (exceptFloat)
	    System.out.println("\tFloat inexact");
	for (int i = 0; i < 8; i++) {
	    System.out.println("\t"+i+" "+myFloat[i]);
	}
	System.out.println("\tstdFloat="+stdFloat);
    }

    static void calc() {
	BigInteger t = BigInteger.ONE.shiftLeft(0x7fffffff);
	t.setBit(0);
	t.toByteArray();
	System.out.println("t.bitLenghth()=" + t.bitLength());
	BigDecimal v = (new BigDecimal(0x1.0p-126));//.add(new BigDecimal(Math.ulp(Float.MAX_VALUE)/2));
	String s = v.toString();
	System.out.println("v="+s+" Lentgh="+s.length());
    }

    public static void main(String[] args) {
	boolean performanceTest = false;

	//calc();
	String ss[] = {
	    "0",
	    "-0",

	    "1e-1000",
	    "1.5e-324",
	    "-1.5e-324",
	    "2.4703282292062327208e-324",
	    "2.4703282292062327209e-324",
	    "3.1e-324",
	    "4.9e-324",
	    "4.940656458412465441765687928682213723650598026e-324",
	    "4.940656458412465441765687928682213723650598027e-324",

	    "1e-309",

	    "2.2250738585072013832e-308",
	    "2.225073858507201383090232717332404064219215980462331830553327416887204435e-308",
	    "2.225073858507201383090232717332404064219215980462331830553327416887204434e-308",

	    "3e-100",

	    "0.3e-45",
	    "0.7e-45",
	    "7.00649232162408535461864791644958065640130970e-46",
	    "7.00649232162408535461864791644958065640130970938257885878534141944895541342930300743319094181060791015624e-46",
	    "7.00649232162408535461864791644958065640130970938257885878534141944895541342930300743319094181060791015625e-46",
	    "7.00649232162408535461864791644958065640130970938257885878534141944895541342930300743319094181060791015626e-46",
	    "1.4e-45",
	    "-1.4e-45",
	    "1.401298464324817070923e-45",
	    "1.401298464324817070924e-45",

	    "1.1754943508222875079687365372222456778186655567720875215087517062784172e-38",
	    "1.1754943508222875079687365372222456778186655567720875215087517062784173e-38",
	    "1.175494350822287509e-38",

	    "1.23e-21",
	    "1.23e-20",

	    "0.1",
	    "0.2",
	    "0.3",
	    "-0.3",
	    "0.5",
	    "1",
	    "2",
	    "3",
	    "-3",

	    "1.23e36",
	    "1.23e37",

	    "3.40282346638528859811704183484516925439e+38",
	    "3.4028234663852885981170418348451692544e+38",
	    "3.40282346638528859811704183484516925441e+38",
	    "3.4028235e+38",
	    "3.402823567797336616375393954581425684479e+38",
	    "3.40282356779733661637539395458142568448e+38",
	    "3.4028236e+38",
	    "3.40282365e+38",
	    "3.4028237e+38",
	    "-3.4028237e+38",

	    "3e100",
	    "3.1231431231231892e100",

	    "1.7976931348623157e+308",
	    "1.7976931348623158e+308",
	    "1.797693134862315808e+308",
	    "1.79769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368e+308",
	    "1.797693134862315708145274237317043567980705675258449965989174768031572607800285387605895586327668781715404589535143824642343213268894641827684675467035375169860499105765512820762454900903893289440758685084551339423045832369032229481658085593321233482747978262041447231687381771809192998812504040261841248583680000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001e+308",
	    "1.797693134862315807937289714053e+308",
	    "1.7976931348623158079372897140530341507993413271003782693617377898044496829276475094664901797758720709633028641669288791094655554785194040263065748867150582068190890200070838367627385484581771153176447573027006985557136695962284291481986083493647529271907416844436551070434271155969950809304288017790417449779199999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999e308",
	    "1.79769313486231580793728971405303415079934132710037826936173778980444968292764750946649017977587207096330286416692887910946555547851940402630657488671505820681908902000708383676273854845817711531764475730270069855571366959622842914819860834936475292719074168444365510704342711559699508093042880177904174497792e308",
	    "1.7976931348623158079372897140530341507993413271003782693617377898044496829276475094664901797758720709633028641669288791094655554785194040263065748867150582068190890200070838367627385484581771153176447573027006985557136695962284291481986083493647529271907416844436551070434271155969950809304288017790417449779200000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001e308",
	    "1.797693134862315807937289714054e+308",
	    "1.7976931348623159e+308",
	    "1.7976931348623160e+308",
	    "-1.7976931348623160e+308",
	    "7e1000",
	    "720579591101481e2"
	};
	for (int i = 0; i < ss.length; i++) {
	    String s = ss[i];
	    new TestFloatingDecimal(s);
	}
	if (performanceTest) {
	    for (int i = 0; i < ss.length; i++) {
		String s = ss[i];
		perform(s);
	    }
	}
    }

    static void perform(String s) {
	FloatingDecimal fd = FloatingDecimal.readJavaFormatString(s);
	for (int i = 0; i < 100000; i++) {
// 	    fd.doubleValue();
// 	    fd.doubleValue(BigDecimal.ROUND_HALF_EVEN);
	    Double.parseDouble(s);
	    FloatingDecimal.readJavaFormatString(s).doubleValue(BigDecimal.ROUND_HALF_EVEN);
	}
	double d = 0;

	long startTime1 = System.currentTimeMillis();
	for (int i = 0; i < 1000000; i++) {
//	    d = fd.doubleValue();
	    Double.parseDouble(s);
	}
	long endTime1 = System.currentTimeMillis();

	long startTime2 = System.currentTimeMillis();
	for (int i = 0; i < 1000000; i++) {
//	    d = fd.doubleValue(BigDecimal.ROUND_HALF_EVEN);
	    FloatingDecimal.readJavaFormatString(s).doubleValue(BigDecimal.ROUND_HALF_EVEN);
	}
	long endTime2 = System.currentTimeMillis();

	System.out.println("** " + (endTime1-startTime1) +
	    " mSec \t| " + (endTime2 - startTime2) + " mSec \ts=" + s);
    }
}
