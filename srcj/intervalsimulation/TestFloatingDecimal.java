import com.sun.electric.tool.simulation.interval.FloatingDecimal;
import java.lang.Math;

class TestFloatingDecimal {
    String s;

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

    static public void main(String[] args) {
	long l0 = 720579591101481l;
	long l = l0*100;
	String s = l0 + "e2";
	new TestFloatingDecimal(s);
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
}
