import com.sun.electric.tool.simulation.interval.FloatingDecimal;
import java.lang.Math;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

class TestFloatingDecimal {
    String s;
    BigDecimal bd;

    double[] myDouble = new double[8];
    boolean exceptDouble;
    double stdDouble;

    float[] myFloat = new float[8];
    boolean exceptFloat;
    float stdFloat;

    static final String infiniteLoop = // String from bug report 4421494 which causes infinity loop
	    "0.000000000000000000000000000000000000000000000000000000000000000000000000000000"+
	    "00000000000000000000000000000000000000000000000000000000000000000000000000000000"+
	    "00000000000000000000000000000000000000000000000000000000000000000000000000000000"+
	    "00000000000000000000000000000000000000000000000000000000000000000000022250738585"+
	    "07201136057409796709131975934819546351645648023426109724822222021076945516529523"+
	    "90813508791414915891303962110687008643869459464552765720740782062174337998814106"+
	    "32673292535522868813721490129811224514518898490572223072852551331557550159143974"+
	    "76397983411801999323962548289017107081850690630666655994938275772572015763062690"+
	    "66333264756530000924588831643303777979186961204949739037782970490505108060994073"+
	    "02629371289589500035837999672072543043602840788957717961509455167482434710307026"+
	    "09144621572289880258182545180325707018860872113128079512233426288368622321503775"+
	    "66662250398253433597456888442390026549819838548794829220689472168983109969836584"+
	    "68140228542433306603398508864458040010349339704275671864433837704860378616227717"+
	    "38545623065874679014086723327636718751";

    static final String tests[] = {
	"0",
	"-0",

	"1e-1000",
	"1.5e-324",
	"-1.5e-324",

	"2.4703282292062327208e-324",

	// From bug report 4396272
	"0.000000000000000000000000000000000000000000000000000000000000000000000000000000"+
	"00000000000000000000000000000000000000000000000000000000000000000000000000000000"+
	"00000000000000000000000000000000000000000000000000000000000000000000000000000000"+
	"00000000000000000000000000000000000000000000000000000000000000000000000000000000"+
	"00000247032822920623272088284396434110686182529901307162382212792841250337753635"+
	"10437593264991818081799618989828234772285886546332835517796989819938739800539093"+
	"90631503565951557022639229085839244910518443593180284993653615250031937045767824"+
	"92193656236698636584807570015857692699037063119282795585513329278343384093519780"+
	"15531246597263579574622766465272827220056374006485499977096599470454020828166226"+
	"23785739345073633900796776193057750674017632467360096895134053553745851666113422"+
	"37666786041621596804619144672918403005300575308490487653917113865916462395249126"+
	"23653881879636239373280423891018672348497668235089863388587925628302755995657524"+
	"45550725518931369083625477918694866799496832404970582102851318545139621383772282"+
	"6145437693412532098591327667236328125",

	"2.4703282292062327209e-324",
	"3.1e-324",
	"4.9e-324",
	"4.940656458412465441765687928682213723650598026e-324",
	"4.940656458412465441765687928682213723650598027e-324",

	"1e-309",

	// From bug report 4421494
	"0.000000000000000000000000000000000000000000000000000000000000000000000000000000"+
	"00000000000000000000000000000000000000000000000000000000000000000000000000000000"+
	"00000000000000000000000000000000000000000000000000000000000000000000000000000000"+
	"00000000000000000000000000000000000000000000000000000000000000000000022250738585"+
	"07201136057409796709131975934819546351645648023426109724822222021076945516529523"+
	"90813508791414915891303962110687008643869459464552765720740782062174337998814106"+
	"32673292535522868813721490129811224514518898490572223072852551331557550159143974"+
	"76397983411801999323962548289017107081850690630666655994938275772572015763062690"+
	"66333264756530000924588831643303777979186961204949739037782970490505108060994073"+
	"02629371289589500035837999672072543043602840788957717961509455167482434710307026"+
	"09144621572289880258182545180325707018860872113128079512233426288368622321503775"+
	"66662250398253433597456888442390026549819838548794829220689472168983109969836584"+
	"68140228542433306603398508864458040010349339704275671864433837704860378616227717"+
	"3854562306587467901408672332763671875",

	// From bug report 4421494
	infiniteLoop,

	"2.2250738585072013832e-308",
	"2.225073858507201383090232717332404064219215980462331830553327416887204434e-308",
	"2.225073858507201383090232717332404064219215980462331830553327416887204435e-308",

	"3e-100",

	"0.3e-45",
	"0.7e-45",
	"7.00649232162408535461864791644958065640130970e-46",
	"7.00649232162408535461864791644958065640130970938257885878534141944895541342930300743319094181060791015624e-46",
	"7.00649232162408535461864791644958065640130970938257885878534141944895541342930300743319094181060791015625e-46",

	// From bug report 4396272
	"0.000000000000000000000000000000000000000000000700649232162408535461864791644958"+
	"065640130970938257885878534141944895541342930300743319094181060791015625",

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

	// From bug report 4154676
	"3.4028235677973366E38",

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

	"1.797693134862315708145274237317043567980705675258449965989174768031572607800285"+
	"38760589558632766878171540458953514382464234321326889464182768467546703537516986"+
	"04991057655128207624549009038932894407586850845513394230458323690322294816580855"+
	"9332123348274797826204144723168738177180919299881250404026184124858368e+308",

	"1.797693134862315708145274237317043567980705675258449965989174768031572607800285"+
	"38760589558632766878171540458953514382464234321326889464182768467546703537516986"+
	"04991057655128207624549009038932894407586850845513394230458323690322294816580855"+
	"93321233482747978262041447231687381771809192998812504040261841248583680000000000"+
	"00000000000000000000000000000000000000000000000000000000000000000000000000000000"+
	"0000000000000000000001e+308",

	// From bug report 4154676
	"1.79769313486231579979201547673599e308",

	"1.797693134862315807937289714053e+308",

	"1.797693134862315807937289714053034150799341327100378269361737789804449682927647"+
	"50946649017977587207096330286416692887910946555547851940402630657488671505820681"+
	"90890200070838367627385484581771153176447573027006985557136695962284291481986083"+
	"49364752927190741684443655107043427115596995080930428801779041744977919999999999"+
	"99999999999999999999999999999999999999999999999999999999999999999999999999999999"+
	"99999999999999999999999999999999999999999e308",

	"1.797693134862315807937289714053034150799341327100378269361737789804449682927647"+
	"50946649017977587207096330286416692887910946555547851940402630657488671505820681"+
	"90890200070838367627385484581771153176447573027006985557136695962284291481986083"+
	"4936475292719074168444365510704342711559699508093042880177904174497792e308",

	"1.797693134862315807937289714053034150799341327100378269361737789804449682927647"+
	"50946649017977587207096330286416692887910946555547851940402630657488671505820681"+
	"90890200070838367627385484581771153176447573027006985557136695962284291481986083"+
	"49364752927190741684443655107043427115596995080930428801779041744977920000000000"+
	"00000000000000000000000000000000000000000000000000000000000000000000000000000000"+
	"00000000000000000000000000000000000000001e308",

	"1.797693134862315807937289714054e+308",
	"1.7976931348623159e+308",
	"1.7976931348623160e+308",
	"-1.7976931348623160e+308",
	"7e1000",

	// Invalid double-rounding in floatValue()
	"720579591101481e2"

	// From bug report 4135022 we can't check them with BigInteger
// 	"100000000e2147483639",
// 	"1000000000e2147483639",
// 	"0.0000000001e-2147483639",
// 	".00000000001e-2147483639",
// 	"1e10000000000",

    };

    TestFloatingDecimal(String s) {
	this.s = s;

	FloatingDecimal fd = FloatingDecimal.readJavaFormatString(s);
	for (int mode = 0; mode < 8; mode++) {
	    try {
		myDouble[mode] = fd.doubleValue(mode);
	    } catch (ArithmeticException e) {
		assert mode == BigDecimal.ROUND_UNNECESSARY;
		exceptDouble = true;
	    }
	    if (s != infiniteLoop)
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
	bd = new BigDecimal(s);
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

	for (int i = 0; i < 8; i++) {
	    System.out.println("\t"+RoundingMode.valueOf(i)+(exceptDouble && i == 7 ? "\tInexact" : ("      \t"+myDouble[i])));
	}
	System.out.println(s == infiniteLoop ? "\tstdDouble\tinfiniteLoop" : "\tstdDouble\t"+stdDouble);

	for (int i = 0; i < 8; i++) {
	    System.out.println("\t"+RoundingMode.valueOf(i)+(exceptFloat && i == 7 ? "\tInexact" : ("      \t"+myFloat[i])));
	}
	System.out.println("\tstdFloat\t"+stdFloat);
    }

    public static void main(String[] args) {
	boolean performanceTest = args.length > 0;
	if (performanceTest) {
	    for (int i = 0; i < tests.length; i++)
		perform(tests[i]);
	} else {
	    for (int i = 0; i < tests.length; i++)
		new TestFloatingDecimal(tests[i]);
	}
    }

    static void perform(String s) {
	sun.misc.FloatingDecimal fd0 = sun.misc.FloatingDecimal.readJavaFormatString(s);
	FloatingDecimal fd = FloatingDecimal.readJavaFormatString(s);
	for (int i = 0; i < 10000; i++) {
	    if (s != infiniteLoop)
		fd0.doubleValue();
	    fd.doubleValue(BigDecimal.ROUND_HALF_EVEN);
	}

	long startTime0 = System.currentTimeMillis();
	if (s == infiniteLoop) {
	    System.out.println("!!! Infinite loop");
	} else {
	    for (int i = 0; i < 100000; i++) {
		fd0.doubleValue();
	    }
	}
	long endTime0 = System.currentTimeMillis();

	long startTime1 = System.currentTimeMillis();
	for (int i = 0; i < 100000; i++) {
	    fd.doubleValue(BigDecimal.ROUND_HALF_EVEN);
	}
	long endTime1 = System.currentTimeMillis();

	System.out.println("** " + (endTime0-startTime0)/10.0 +
	    " ns \t| " + (endTime1 - startTime1)/10.0 + " ns \ts=" + s);
    }
}
