import com.sun.electric.tool.simulation.interval.RawFile;
import net.sourceforge.interval.ia_math.*;
import Jama.*;
//import Jama.util.Maths;
import java.io.*;
//import java.util.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;

class Hermite
{
    static double phi00(double k) { return (1-k)*(1-k)*(1 + 2*k); }
    static double phi01(double k) { return (1-k)*(1-k)*k; }
    static double phi10(double k) { return k*k*(3 - 2*k); }
    static double phi11(double k) { return k*k*(k-1); }

    static double phi00d(double k) { return -6*(1-k)*k; }
    static double phi01d(double k) { return (1-k)*(1-3*k); }
    static double phi10d(double k) { return 6*(1-k)*k; }
    static double phi11d(double k) { return k*(3*k - 2); }

    static double phi00dd(double k) { return 12*k-6; }
    static double phi01dd(double k) { return 6*k-4; }
    static double phi10dd(double k) { return 6-12*k; }
    static double phi11dd(double k) { return 6*k-2; }

    static BigDecimal bone = BigDecimal.valueOf(1);
    static BigDecimal btwo = BigDecimal.valueOf(2);
    static BigDecimal bthree = BigDecimal.valueOf(3);

    static BigDecimal phi00(BigDecimal k) {
	BigDecimal k1 = bone.subtract(k);
	return k1.multiply(k1).multiply(k.multiply(btwo).add(bone));
    }

    static BigDecimal phi01(BigDecimal k) {
	BigDecimal k1 = bone.subtract(k);
	return k1.multiply(k1).multiply(k);
    }

    static BigDecimal phi10(BigDecimal k) {
	return k.multiply(k).multiply(k.multiply(btwo).negate().add(bthree));
    }

    static BigDecimal phi11(BigDecimal k) {
	return k.multiply(k).multiply(k.subtract(bone));
    }

    static void printM(String msg, Matrix m) {
	//DecimalFormat fmt = new DecimalFormat("0.0000E00");
	DecimalFormat fmt = new DecimalFormat("0.0000000000000000000000E00");
	PrintWriter FILE = new PrintWriter(System.out, true);
	FILE.print(msg);
	m.print(FILE,fmt,10);
    }

    static class Optim {
	double t0, tf;
	double h;
	double v0, vf;
	double g0, gf;

	double k1, k2, k3;
	Matrix M = new Matrix (5, 5);
	Matrix MI;
	Matrix Koeff;

	Optim(double t0, double tf, double k1, double k2, double k3) {
	    this.t0 = t0;
	    this.tf = tf;
	    this.h = tf - t0;
	    this.k1 = k1;
	    this.k2 = k2;
	    this.k3 = k3;
	    setM();
	}

	double getV(double t) {
	    double k = (t - t0)/h;
	    return v0*phi00(k) + vf*phi10(k) + h*(g0*phi01(k) + gf*phi11(k));
	}

	double getV1(double t) {
	    double dt = t - t0;
	    double k = dt/h;
	    double k1 = 1-k;
	    double dvt = k*(3-2*k)*(vf-v0)/h + k1*(k1*g0-k*gf);
	    return v0 + dvt*dt;
	}

	double getV2(double t) {
	    double dt = tf - t;
	    double k = dt/h;
	    double k1 = 1-k;
	    double dvt = k*(3-2*k)*(v0-vf)/h - k1*(k1*gf-k*g0);
	    return vf + dvt*dt;
	}

	double getV3(double t) {
	    double dt = t - t0;
	    double k = dt/h;
	    double d3 = 2*(vf-v0)/h - g0 - gf;
	    return v0 + k*((vf-v0) + (tf-t)*((g0-gf)/2 + (k-0.5)*d3));
	}

	double getAccurV(double t) {
	    BigDecimal bv0 = new BigDecimal(v0);
	    BigDecimal bvf = new BigDecimal(vf);
	    BigDecimal bg0 = new BigDecimal(g0);
	    BigDecimal bgf = new BigDecimal(gf);
	    BigDecimal bt0 = new BigDecimal(t0);
	    BigDecimal btf = new BigDecimal(tf);
	    BigDecimal bt = new BigDecimal(t);
	    BigDecimal bh = btf.subtract(bt0);
	    BigDecimal bk = bt.subtract(bt0).divide(bh, 20, BigDecimal.ROUND_HALF_DOWN);
	    BigDecimal br = bv0.multiply(phi00(bk)).add(bvf.multiply(phi10(bk)).
		add((bg0.multiply(phi01(bk)).add(bgf.multiply(phi11(bk))).multiply(bh))));
	    return br.doubleValue();
	}

	private void setM(int row, double k) {
	    M.set(row, 0, phi00(k));
	    M.set(row, 1, phi01(k));
	    M.set(row, 2, phi10(k));
	    M.set(row, 3, phi11(k));
	    M.set(row, 4, row%2 == 0 ? 1 : -1);
	}

	void setM() {
	    setM(0, 0.0);
	    setM(1, k1);
	    setM(2, k2);
	    setM(3, k3);
	    setM(4, 1.0);

	    MI = M.inverse();

	    Matrix X = new Matrix(5, 1);
	    X.set(0, 0, 1.0);
	    X.set(1, 0, Math.exp(-k1*h));
	    X.set(2, 0, Math.exp(-k2*h));
	    X.set(3, 0, Math.exp(-k3*h));
	    X.set(4, 0, Math.exp(-h));
	    Koeff = MI.times(X);
	    v0 = Koeff.get(0, 0);
	    g0 = Koeff.get(1, 0)/h;
	    vf = Koeff.get(2, 0);
	    gf = Koeff.get(3, 0)/h;

	    printM("M=", M);
	    printM("MI=", MI);
	    printM("X=", X.transpose());
	    printM("Res=", Koeff.transpose());
	    System.out.println("v0="+v0+" vf="+vf+" g0="+g0+" gf="+gf);

	    X.set(0, 0, 1.0 - getAccurV(0.0));
	    X.set(1, 0, Math.exp(-k1*h) - getAccurV(k1*h));
	    X.set(2, 0, Math.exp(-k2*h) - getAccurV(k2*h));
	    X.set(3, 0, Math.exp(-k3*h) - getAccurV(k3*h));
	    X.set(4, 0, Math.exp(-h) - getAccurV(h));
	    Koeff = MI.times(X);

	    printM("X=", X.transpose());
	    printM("Res=", Koeff.transpose());

	    v0 += Koeff.get(0, 0);
	    g0 += Koeff.get(1, 0)/h;
	    vf += Koeff.get(2, 0);
	    gf += Koeff.get(3, 0)/h;
	}

	void plotOpt()
	    throws FileNotFoundException
	{
	    int numPoints = 10000;
	    int numVars = 8;
	    RawFile raw = new RawFile(numPoints + 1, numVars);
	    raw.setVar(0, "t",  "time");
	    raw.setVar(1, "f",  "voltage");
	    raw.setVar(2, "s",  "voltage");
	    raw.setVar(3, "p",  "voltage");
	    raw.setVar(4, "a",  "voltage");
	    raw.setVar(5, "s1", "voltage");
	    raw.setVar(6, "s2", "voltage");
	    raw.setVar(7, "s3", "voltage");

	    for (int k = 0; k <= numPoints; k++)
	    {
		double kk = 0.0 + 1.0 / numPoints * k;
		double t = kk*h;
		raw.set(k, 0, t);
	
		raw.set(k, 1, Math.exp(-t));
		raw.set(k, 2, getV(t));
		raw.set(k, 3, 1.-t*(1.-t/2*(1-t/3*(1-t/4*(1-t/5*(1-t/6*(1-t/7*(1-t/8))))))));
		raw.set(k, 4, getAccurV(t));
		raw.set(k, 5, getV1(t));
		raw.set(k, 6, getV2(t));
		raw.set(k, 7, getV3(t));
	    }
	    raw.writeBinary("opt.raw", null);

	}
    }

    static void plotPhi()
	throws FileNotFoundException
    {
	int numPoints = 10000;
	int numVars = 1+12;
	RawFile raw = new RawFile(numPoints + 1, numVars);
	raw.setVar(0, "t",    "time");
	raw.setVar(1, "p00",  "voltage");
	raw.setVar(2, "p00d", "voltage");
	raw.setVar(3, "p01",  "voltage");
	raw.setVar(4, "p01d", "voltage");
	raw.setVar(5, "p10",  "voltage");
	raw.setVar(6, "p10d", "voltage");
	raw.setVar(7, "p11",  "voltage");
	raw.setVar(8, "p11d", "voltage");
	raw.setVar(9, "b00",  "voltage");
	raw.setVar(10,"b01",  "voltage");
	raw.setVar(11,"b10",  "voltage");
	raw.setVar(12,"b11",  "voltage");

	for (int k = 0; k <= numPoints; k++)
	{
	    double t = -0.1 + 1.2 / numPoints * k;
	    raw.set(k, 0, t);

	    raw.set(k, 1, phi00(t));
	    raw.set(k, 2, phi00d(t));
	    raw.set(k, 3, phi01(t));
	    raw.set(k, 4, phi01d(t));
	    raw.set(k, 5, phi10(t));
	    raw.set(k, 6, phi10d(t));
	    raw.set(k, 7, phi11(t));
	    raw.set(k, 8, phi11d(t));

	    BigDecimal kk = new BigDecimal(t);
	    raw.set(k, 9, phi00(kk).doubleValue());
	    raw.set(k,10, phi01(kk).doubleValue());
	    raw.set(k,11, phi10(kk).doubleValue());
	    raw.set(k,12, phi11(kk).doubleValue());
	}
	raw.writeBinary("hermite.raw", null);
    }

    /**
     * Test program plots in raw file diode current versus voltage:
     * at interval [v-delta,v+delta],
     * and at 3 point of this interval - ceneter and borders
     */
    
    public static void main (String argv[])
	throws FileNotFoundException
    {
	plotPhi();
	double h = 0.001;
	h = 10;
	Optim opt = new Optim(0, h, 0.15, 0.5, 0.85);
	opt.plotOpt();
    }
}
