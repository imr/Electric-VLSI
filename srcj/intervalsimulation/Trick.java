import Jama.*;
import Jama.util.Maths;
import java.io.*;
import java.util.*;
import java.text.DecimalFormat;

class Segment
{
    static final int OFF = 0;
    static final int ON = 1;
    static final int PLUS = 2;
    static final int MINUS = 3;

    double t0;
    double t1;
    Matrix v0;
    int type;

    Segment(int type, double t0, Matrix v0)
    {
	this.type = type;
	this.t0 = t0;
	switch (type) {
	    case OFF:
		this.v0 = v0.copy();
		break;
	    case ON:
		this.v0 = v0.minus(Trick.es.times(Math.sin(Trick.omega*t0))).minus(Trick.ec.times(Math.cos(Trick.omega*t0)));
		break;
	    case PLUS:
		this.v0 = v0.minus(Trick.ej);
		break;
	    case MINUS:
		this.v0 = v0.plus(Trick.ej);
		break;
	}
    }

    void eval(double t, Matrix vv, Matrix vg)
    {
	Matrix expA;

	switch (type) {
	    case OFF:
		vv.setMatrix(0, 2, 0, 0, Trick.expAofft(t - t0).times(v0));
		vg.setMatrix(0, 2, 0, 0, Trick.Aoff.times(vv));
		break;
	    case ON:
		vv.setMatrix(0, 2, 0, 0, Trick.expAont(t - t0).times(v0));
		vg.setMatrix(0, 2, 0, 0, Trick.Aon.times(vv));
		vv.setMatrix(0, 2, 0, 0, vv.plus(Trick.es.times(Math.sin(Trick.omega*t))).plus(Trick.ec.times(Math.cos(Trick.omega*t))));
		vg.setMatrix(0, 2, 0, 0, vg.plus(Trick.es.times(Trick.omega*Math.cos(Trick.omega*t))).plus(Trick.ec.times(-Trick.omega*Math.sin(Trick.omega*t))));
		break;
	    case PLUS:
		vv.setMatrix(0, 2, 0, 0, Trick.expAofft(t - t0).times(v0));
		vg.setMatrix(0, 2, 0, 0, Trick.Aoff.times(vv));
		vv.setMatrix(0, 2, 0, 0, vv.plus(Trick.ej));
		break;
	    case MINUS:
		vv.setMatrix(0, 2, 0, 0, Trick.expAofft(t - t0).times(v0));
		vg.setMatrix(0, 2, 0, 0, Trick.Aoff.times(vv));
		vv.setMatrix(0, 2, 0, 0, vv.minus(Trick.ej));
		break;
	}
    }

    double nextT()
    {
	Matrix vv = new Matrix(3, 1);
	Matrix vg = new Matrix(3, 1);
	double t = t0;
	boolean on = (type == ON);
	do {
	    t += 1e-4;
	    eval(t, vv, vg);
	    //System.out.println("t="+t+" sin="+Math.sin(Trick.omega*t)+" v="+vv.get(0,0)+" diff="+(Math.sin(Trick.omega*t) - vv.get(0, 0)));
	} while (on == (Math.sin(Trick.omega*t) - vv.get(0, 0) >= 0.0));
	int i;
	for (i = 0; i < 10; i++) {
	    double v = Math.sin(Trick.omega*t) - vv.get(0,0);
	    double g = Trick.omega*Math.cos(Trick.omega*t) - vg.get(0,0);
	    double dt = -v/g;
	    t += dt;
	    //System.out.println("v="+v+" g="+g+" dt="+dt+" t="+t);
	    if (Math.abs(dt) < 1e-12) break;
	    eval(t, vv, vg);
	}
	t1 = t;
	return t;
    }
}

/** Example of use of Matrix Class, featuring magic squares. **/

public class Trick {

   /** Shorten spelling of print. **/

   private static void printM(String msg, Matrix m) {
       DecimalFormat fmt = new DecimalFormat("0.0000E00");
       PrintWriter FILE = new PrintWriter(System.out, true);
       FILE.print(msg);
       m.print(FILE,fmt,10);
   }

    static double Gd = 1;
    static Matrix C, C1, Goff, Gon;
    static Matrix Aon, Don, Von, Von1;
    static Matrix Aoff, Doff, Voff, Voff1;
    static double[] donr, doni;
    static double[] doffr, doffi;

    private static Matrix prepareA(double gd)
    {
	Matrix G = Goff.copy(); 
	G.set(0,0,gd);
	return C1.times(G).uminus();
    }

    private static IntervalMatrix prepareIA(double gd_min, double gd_max)
    {
	Matrix a_min = prepareA(gd_min);
	Matrix a_max = prepareA(gd_max);
	int n = a_min.getRowDimension();
	Matrix c = new Matrix(n,n);
	Matrix d = new Matrix(n,n);
	for (int i = 0; i < n; i++) {
	    for (int j = 0; j < n; j++) {
		double a_min_ij = a_min.get(i, j);
		double a_max_ij = a_max.get(i, j);
		c.set(i, j, (a_min_ij + a_max_ij)*0.5);
		d.set(i, j, Math.abs((a_max_ij - a_min_ij))*0.5);
	    }
	}
	return new IntervalMatrix(c, d);
    }

    private static void prepare () {
	
	double C_val[][] = {
	    {1e-3,    0,   0},
	    {   0, 1e-3,   0},
	    {   0,    0, 0.1}};
	double Gon_val[][] = {
	    {Gd,      0,   1},
	    { 0,  1/1e3,  -1},
	    {-1,      1,   0}};
	double Goff_val[][] = {
	    { 0,      0,   1},
	    { 0,  1/1e3,  -1},
	    {-1,      1,   0}};
	
	C = new Matrix(C_val);
	C1 = C.inverse();
	Goff = new Matrix(Goff_val);
	Gon = Goff.copy();
	Gon.set(0,0,Gd);
	
	Aoff = prepareA(0.0);
	Aon = prepareA(Gd);

	EigenvalueDecomposition Eon =
	    new EigenvalueDecomposition(Aon);
	donr = Eon.getRealEigenvalues();
	doni = Eon.getImagEigenvalues();
	Don = Eon.getD();
	Von = Eon.getV();
	Von1 = Von.inverse();

	EigenvalueDecomposition Eoff =
	    new EigenvalueDecomposition(Aoff);
	doffr = Eoff.getRealEigenvalues();
	doffi = Eoff.getImagEigenvalues();
	Doff = Eoff.getD();
	Voff = Eoff.getV();
	Voff1 = Voff.inverse();
    }

    static Matrix expAont(double t) {
	Matrix expDt = new Matrix(Don.getColumnDimension(), Don.getColumnDimension());
	for (int i = 0; i < donr.length  ; i++)
	{
	    double ex = Math.exp(donr[i]*t);
	    if (doni[i] == 0.0) {
		expDt.set(i, i, ex);
	    } else {
		double co = ex*Math.cos(doni[i]*t);
		double si = ex*Math.sin(doni[i]*t);
		expDt.set(i, i, co);
		expDt.set(i, i+1, si);
		expDt.set(i+1, i, -si);
		expDt.set(i+1, i+1, co);
		i++;
	    }
	}
	return Von.times(expDt).times(Von1);
    }

    static Matrix expAofft(double t) {
	Matrix expDt = new Matrix(Doff.getColumnDimension(), Doff.getColumnDimension());
	for (int i = 0; i < doffr.length  ; i++)
	{
	    double ex = Math.exp(doffr[i]*t);
	    if (doffi[i] == 0.0) {
		expDt.set(i, i, ex);
	    } else {
		double co = ex*Math.cos(doffi[i]*t);
		double si = ex*Math.sin(doffi[i]*t);
		expDt.set(i, i, co);
		expDt.set(i, i+1, si);
		expDt.set(i+1, i, -si);
		expDt.set(i+1, i+1, co);
		i++;
	    }
	}
	return Voff.times(expDt).times(Voff1);
    }

    static double lam = 0.4929625;
    //static double lam = 0.49;
    static IntervalMatrix ID;

    static boolean checkEnergy(double gd_min, double gd_max, int maxDepth)
    {
	IntervalMatrix IA = prepareIA( gd_min, gd_max);
	IntervalMatrix IY = IA.transpose().times(ID).plus(ID.times(IA)).plus(ID.times(lam));

	//int[] permute = {1,2,0}; 
	int[] permute = {0,1,2}; 
	if (IY.times(-1).isPositive(permute))
	{
	    System.out.println("["+gd_min+","+gd_max+"]");
	    return true;
	}
	if (maxDepth == 0) {
	    System.out.println("?["+gd_min+","+gd_max+"]");
	    return false;
	}
	boolean ok = true;
	if (!checkEnergy(gd_min, 0.5*(gd_min+gd_max), maxDepth - 1)) ok = false;
	if (!checkEnergy(0.5*(gd_min+gd_max), gd_max, maxDepth - 1)) ok = false;
	return ok;
    }

    static void energy()
    {
	double E[] = new double[3];
	for (int i = 0; i < 3; i++)
	{
	    double v1 = Voff.get(0,i);
	    double v2 = Voff.get(1,i);
	    double j = Voff.get(2,i);
	    E[i] = (v1*v1*1e-3 + v2*v2*1e-3 + j*j*0.1)*0.5;
	    System.out.println("E"+i+" = "+E[i]);
	}
	Matrix scale = new Matrix(3,3);
	double scale0 = 1/Math.sqrt(E[0]*2);
	double scale1 = 1.0/Math.sqrt(E[1]+E[2]);
	scale.set(0, 0, scale0);
	scale.set(1, 1, scale1);
	scale.set(2, 2, scale1);

	Matrix S = Voff.times(scale);
	Matrix D = S.inverse().transpose().times(S.inverse());
	printM("energy=",D);
	    
	Matrix Xoff = S.inverse().times(Aoff).times(S);
	eigens("Xoff", Xoff.plus(Xoff.transpose()));
	
	Matrix Xon = S.inverse().times(Aon).times(S);
	eigens("Xon", Xon.plus(Xon.transpose()));

	Matrix Yoff = Aoff.transpose().times(D).plus(D.times(Aoff)).plus(D.times(lam));
	eigens("Yoff",Yoff);

	Matrix Yon = Aon.transpose().times(D).plus(D.times(Aon)).plus(D.times(lam));
	eigens("Yon",Yon);

	ID = new IntervalMatrix(D);

	checkEnergy(0, Gd, 20);
	//int[] permute = {1,2,0}; 
	//System.out.println("IY.isPositive="+ IY.times(-1).isPositive(permute));

    }

    static void halfperiod() {
	double t = Math.PI/Math.abs(doffi[1]);
	System.out.println("halfperiod = "+t);
	Matrix half = expAofft(t);
	Matrix id = Matrix.identity(3, 3);
	Matrix v = id.plus(half).inverse().times(id.minus(half)).times(ej);
	printM("v", v);

	Segment seg = new Segment(Segment.MINUS, 0, v);
	Matrix vv = new Matrix(3, 1);
	Matrix vg = new Matrix(3, 1);
	seg.eval(t, vv, vg);
	printM("hvv", vv);
	printM("hvg", vg);
	Segment seg1 = new Segment(Segment.PLUS, t, vv);
	seg1.eval(t*2, vv, vg);
	printM("hvv1", vv);
    }

    static void exponent() {
	int dim = 3;
	int pow = 10;
	int n = 1 << pow;
	double T = 0.02;
	double minT = T / n;
	Matrix soff = expAofft(minT);
	Matrix son = expAont(minT);

	IntervalMatrix s = IntervalMatrix.newLoHi(soff, son);
	eigens("soff", soff);
	eigens("son", son);
	for (int i = 0; i < pow; i++) {
	    System.out.println("dt="+ minT*(1 << i));
	    printM("Sc", s.center);
	    printM("Sd", s.delta);
	    eigens("mag", s.mag());
	    s = s.times(s);
	}

	Matrix S = Matrix.identity(dim,dim);
	IntervalMatrix Si = new IntervalMatrix(S);
	IntervalMatrix Si2 = null;;
	Matrix[] Sa = new Matrix[n+1];
	Matrix[] Sm = new Matrix[n+1];
	Sa[0] = abs(S);
	Sm[0] = Si.mag();
	for (int i = 0; i < n; i++) {
	    S = s.center.times(S);
	    Sa[i+1] = abs(S);
	    Si = s.times(Si);
	    
	    Matrix Sum = new Matrix(dim,dim);
	    for (int j = 0; j < i; j++) {
		Sum = Sum.plus(Sm[j].times(s.delta).times(Sa[i-j]));
	    }
	    Si2 = new IntervalMatrix(S, Sum);
	    Sm[i+1] = Si2.mag();
	}

	printM("Si2.c", Si2.center);
	printM("Si2.d", Si2.delta);
    }

    static double omega = 2*Math.PI*50.0;
    static Matrix ec, es, ej;

    private static void excitement()
    {
	int n = C.getColumnDimension();

	Matrix exc = new Matrix(2*n, 2*n);
	exc.setMatrix(0, n-1, 0, n-1, Gon);
	exc.setMatrix(n, 2*n-1, n, 2*n-1, Gon);
	exc.setMatrix(0, n-1, n, 2*n-1, C.times(-omega));
	exc.setMatrix(n, 2*n-1, 0, n-1, C.times(omega));

	LUDecomposition lu = new LUDecomposition(exc);

	Matrix b = new Matrix(2*n, 1);
	b.set(0, 0, Gd);
	Matrix r = lu.solve(b);
	es = new Matrix(3, 1);
	ec = new Matrix(3, 1);
	es.setMatrix(0, 2, 0, 0, r.getMatrix(0, 2, 0, 0));
	ec.setMatrix(0, 2, 0, 0, r.getMatrix(3, 5, 0, 0));
	printM("exc=", lu.solve(b));

	ej = new Matrix(3, 1);
	ej.set(0, 0, 1.0);
	ej = Goff.inverse().times(ej);
	printM("ej=", ej);
    }

    static LinkedList sl = new LinkedList();

    private static void makeList()
    {
	Matrix vv = new Matrix(3, 1);
	Matrix vg = new Matrix(3, 1);
	double t = 0;
	int type = Segment.ON;
	for (int i = 0; i < 100; i++) {
 	    Segment seg = new Segment(type, t, vv);
	    sl.add(seg);
	    t = seg.nextT();
	    seg.eval(t, vv, vg);
	    type = (type == Segment.ON ? Segment.OFF : Segment.ON);
	    //System.out.println("nextT="+t);
	}
    }

    static void listEval(double t, Matrix vv, Matrix vg) {
	Iterator itr = sl.iterator();
	Segment seg = null;
	while(itr.hasNext()) {
	    Segment s = (Segment)itr.next();
	    if (s.t0 > t) break;
	    seg = s;
	}
	seg.eval(t, vv, vg);
    }

    static Matrix listSense(double t1, double t2) {
	if (t2 < t1)
	    return listSense(t2, t1).inverse();
	Iterator itr = sl.iterator();
	Matrix M = Matrix.identity(3, 3);
	while(itr.hasNext()) {
	    Segment s = (Segment)itr.next();
	    if (t1 >= s.t1) continue;
	    if (t2 <= s.t0) break;
	    double dt =  Math.min(t2, s.t1) - Math.max(t1, s.t0);
	    M = (s.type == Segment.ON?expAont(dt):expAofft(dt)).times(M);
	}
	return M;
    }

    private static double integrate(Segment s, double t1) {
	double minT=1e-4;
	int n = (int)((s.t1 - s.t0) / minT) + 1;
	double sum = 0.0;
	for (int i = 0; i <= n; i++) {
	    double sen = Math.abs(listSense(s.t0 + (s.t1-s.t0)/n*i, t1).get(0, 0));
	    sum += (i == 0 || i == n ? 0.5 : 1.0)*sen;
		
	}
	return sum*(s.t1-s.t0)/n;
    }

    private static void calcExcite(Segment sg) {
	Iterator itr = sl.iterator();
	double con = 0.0, coff = 0.0, ion = 0.0, ioff = 0.0;
	while(itr.hasNext()) {
	    Segment s = (Segment)itr.next();
	    if (s == sg) break;
	    double sen = Math.abs(listSense(s.t0, sg.t0).get(0,0));
	    double integr = integrate(s, sg.t1);
	    if (s.type == Segment.ON) {
		con += sen;
		ion += integr;
	    } else {
		coff += sen;
		ioff += integr;
	    }
	}
	System.out.println("sg.t0="+sg.t0+"\tsg.type="+sg.type+"\tcon="+con+"\tcoff="+coff+"\tion="+ion+"\tioff="+ioff);
    }

    private static void plotResponse()
       throws FileNotFoundException
    {
	int k;
	int numPoints = 10000;
	int numVars = 1 + 3*3*2*2;
	Matrix raw = new Matrix(numPoints + 1, numVars);
	String varName[] = new String[numVars];
	String varType[] = new String[numVars];
	varName[0] = "t";
	varType[0] = "time";
	for (k = 0; k < 3; k++) {
	    varName[1 + k*12 + 0] = "va" + k;
	    varType[1 + k*12 + 0] = "voltage";
	    varName[1 + k*12 + 1] = "vb" + k;
	    varType[1 + k*12 + 1] = "voltage";
	    varName[1 + k*12 + 2] = "il" + k;
	    varType[1 + k*12 + 2] = "current";
	    varName[1 + k*12 + 3] = "dva" + k;
	    varType[1 + k*12 + 3] = "voltage";
	    varName[1 + k*12 + 4] = "dvb" + k;
	    varType[1 + k*12 + 4] = "voltage";
	    varName[1 + k*12 + 5] = "dil" + k;
	    varType[1 + k*12 + 5] = "current";

	    varName[1 + k*12 + 6] = "fva" + k;
	    varType[1 + k*12 + 6] = "voltage";
	    varName[1 + k*12 + 7] = "fvb" + k;
	    varType[1 + k*12 + 7] = "voltage";
	    varName[1 + k*12 + 8] = "fil" + k;
	    varType[1 + k*12 + 8] = "current";
	    varName[1 + k*12 + 9] = "dfva" + k;
	    varType[1 + k*12 + 9] = "voltage";
	    varName[1 + k*12 +10] = "dfvb" + k;
	    varType[1 + k*12 +10] = "voltage";
	    varName[1 + k*12 +11] = "dfil" + k;
	    varType[1 + k*12 +11] = "current";
	}
	double T = 20.0;
	for (int i = 0; i <= numPoints; i++) {
	    double t = T / (numPoints + 1) * i;
	    Matrix expAon = expAont(t);
	    raw.set(i, 0, t);
	    for (k = 0; k < 3; k++) {
		Matrix v = expAon.getMatrix(0, 2, k, k);
		raw.set(i, 1 + k*12 + 0, v.get(0,0));
		raw.set(i, 1 + k*12 + 1, v.get(1,0));
		raw.set(i, 1 + k*12 + 2, v.get(2,0));
		v = Aon.times(v);
		raw.set(i, 1 + k*12 + 3, v.get(0,0));
		raw.set(i, 1 + k*12 + 4, v.get(1,0));
		raw.set(i, 1 + k*12 + 5, v.get(2,0));
	    }

	    Matrix expAoff = expAofft(t);
	    raw.set(i, 0, t);
	    for (k = 0; k < 3; k++) {
		Matrix v = expAoff.getMatrix(0, 2, k, k);
		raw.set(i, 1 + k*12 + 6, v.get(0,0));
		raw.set(i, 1 + k*12 + 7, v.get(1,0));
		raw.set(i, 1 + k*12 + 8, v.get(2,0));
		v = Aoff.times(v);
		raw.set(i, 1 + k*12 + 9, v.get(0,0));
		raw.set(i, 1 + k*12 +10, v.get(1,0));
		raw.set(i, 1 + k*12 +11, v.get(2,0));
	    }
	}
	RawWriter.write("trick.raw", varName, varType, raw);
    }

    private static void plotExcite(double te, boolean end)
       throws FileNotFoundException
    {
	int k;
	int numPoints = 10000;
	int numVars = 11+3*3;
	Matrix raw = new Matrix(numPoints + 1, numVars);
	String varName[] = new String[numVars];
	String varType[] = new String[numVars];
	varName[0] = "t";
	varType[0] = "time";
	varName[1] = "vi";
	varType[1] = "voltage";
	varName[2] = "va";
	varType[2] = "voltage";
	varName[3] = "vb";
	varType[3] = "voltage";
	varName[4] = "il";
	varType[4] = "current";
	varName[5] = "eva";
	varType[5] = "voltage";
	varName[6] = "evb";
	varType[6] = "voltage";
	varName[7] = "eil";
	varType[7] = "current";
	varName[8] = "sva";
	varType[8] = "voltage";
	varName[9] = "svb";
	varType[9] = "voltage";
	varName[10] = "sil";
	varType[10] = "current";
	for (k = 0; k < 3; k++) {
	    varName[11+k*3+0] = "va"+k;
	    varType[11+k*3+0] = "voltage";
	    varName[11+k*3+1] = "vb"+k;
	    varType[11+k*3+1] = "voltage";
	    varName[11+k*3+2] = "il"+k;
	    varType[11+k*3+2] = "current";
	}

	double T = 0.5;
	System.out.println("te="+te);
	Matrix vv = new Matrix(3, 1);
	Matrix vg = new Matrix(3, 1);
	for (int i = 0; i <= numPoints; i++) {
	    double t = T / (numPoints + 1) * i;
	    raw.set(i, 0, t);
	    raw.set(i, 1, Math.sin(omega*t));
	    raw.set(i, 2, es.get(0,0)*Math.sin(omega*t) + ec.get(0,0)*Math.cos(omega*t));
	    raw.set(i, 3, es.get(1,0)*Math.sin(omega*t) + ec.get(1,0)*Math.cos(omega*t));
	    raw.set(i, 4, es.get(2,0)*Math.sin(omega*t) + ec.get(2,0)*Math.cos(omega*t));
	    Matrix expAon = expAont(t);
	    Matrix v = expAon.times(ec);
	    raw.set(i, 5, es.get(0,0)*Math.sin(omega*t) + ec.get(0,0)*Math.cos(omega*t) - v.get(0,0));
	    raw.set(i, 6, es.get(1,0)*Math.sin(omega*t) + ec.get(1,0)*Math.cos(omega*t) - v.get(1,0));
	    raw.set(i, 7, es.get(2,0)*Math.sin(omega*t) + ec.get(2,0)*Math.cos(omega*t) - v.get(2,0));
	    listEval(t, vv, vg);
	    raw.set(i, 8, vv.get(0,0));
	    raw.set(i, 9, vv.get(1,0));
	    raw.set(i,10, vv.get(2,0));
	    Matrix s = end ? listSense(t, te) : listSense(te, t);
	    for (k = 0; k < 3; k++) {
		raw.set(i, 11+k*3+0, s.get(0, k));
		raw.set(i, 11+k*3+1, s.get(1, k));
		raw.set(i, 11+k*3+2, s.get(2, k));
	    }
	}
	RawWriter.write("excite.raw", varName, varType, raw);
    }

    static Matrix abs(Matrix S) {
	int m = S.getRowDimension();
	int n = S.getColumnDimension();
	Matrix A = new Matrix(m,n);
	for (int i = 0; i < m; i++)
	    for (int j = 0; j < n; j++)
		A.set(i, j, Math.abs(S.get(i,j)));
	return A;
    }

    static void eigens(String msg, Matrix S) {
	EigenvalueDecomposition ES =
	    new EigenvalueDecomposition(S);
	double[] dsr = ES.getRealEigenvalues();
	double[] dsi = ES.getImagEigenvalues();
	Matrix Ds = ES.getD();
	Matrix Vs = ES.getV();
	Matrix Vs1 = Vs.inverse();

       for (int i = 0; i < 3; i++) {
	   System.out.println(msg+".ds"+i+" "+dsr[i]+" "+dsi[i]+" ("+Maths.hypot(dsr[i],dsi[i])+")");
       }
       printM(msg+".S=", S);
       printM(msg+".Vs=", Vs);
       printM(msg+".Vs1=", Vs1);
    }

    static void evolution() {
	double T = 0.02;
	double T1 = 0.0013;
	T1 = 0.0020;

	Matrix S = expAont( T1 ).times( expAofft(T - T1) );
	
	eigens("S", S);
	eigens("SA", abs(S));
	Matrix P = S;
	for (int i = 2; i <= 8; i++) {
	    P = P.times(S);
	    eigens("S"+i, P);
	    eigens("S"+i+"A", abs(P));
	}

	EigenvalueDecomposition ES =
	    new EigenvalueDecomposition(S);
	double[] dsr = ES.getRealEigenvalues();
	double[] dsi = ES.getImagEigenvalues();
	Matrix Ds = ES.getD();
	Matrix Vs = ES.getV();
	Matrix Vs1 = Vs.inverse();

	printM("Off=", Vs1.times(expAofft(T-T1)).times(Vs));
	printM("On=", Vs1.times(expAont(T1)).times(Vs));

	T1 = 0.0014;
	S = expAont( T1 ).times( expAofft(T - T1) );
	printM("Both=", Vs1.times(S).times(Vs));
    }

   public static void main (String argv[])
       throws FileNotFoundException
    {
       prepare();
       for (int i = 0; i < 3; i++) {
	   System.out.println("don"+i+" "+donr[i]+" "+doni[i]);
       }
       printM("Von=", Von);
       printM("Von1=", Von1);
       for (int i = 0; i < 3; i++) {
	   System.out.println("doff"+i+" "+doffr[i]+" "+doffi[i]);
       }
       printM("Voff=", Voff);
       printM("Voff1=", Voff1);
       //energy();
       //exponent();
       excitement();
       halfperiod();
      
       makeList();
       evolution();

       //plotResponse();
       /*
       boolean on = false;
       do {
	   Iterator itr = sl.iterator();
	   while(itr.hasNext()) {
	       Segment sg = (Segment)itr.next();
	       if (sg.on == on)
		   calcExcite(sg);
	   }
	   on = !on;
       } while (on);
       */

       Segment sg = (Segment)sl.get(0);
       plotExcite(sg.t0, false);
       
    }
}
