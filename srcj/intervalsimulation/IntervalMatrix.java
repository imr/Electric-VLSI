import Jama.*;
import net.sourceforge.interval.ia_math.*;

/**
 * An Interval Matrix is a matrix of real intervals.
 * Each interval is represented by its center and radius.
 * NOTE. This implementation doesn't consider properly
 *       floatin-point rounding errors.
 */
class IntervalMatrix {
    Matrix center;
    Matrix delta;
    IntervalMatrix(Matrix center, Matrix delta) {
	if (center.getRowDimension() != delta.getRowDimension() ||
	    center.getColumnDimension() != delta.getRowDimension()) {
	    throw new IllegalArgumentException("Interval Matrix inner dimensions must agree.");
	}
	this.center = center;
	this.delta = delta;
    }
    IntervalMatrix(Matrix center) {
	this.center = center;
	this.delta = new Matrix(center.getRowDimension(), center.getColumnDimension());
    }
    static IntervalMatrix newLoHi(Matrix lo, Matrix hi) {
	if (lo.getRowDimension() != hi.getRowDimension() ||
	    lo.getColumnDimension() != hi.getColumnDimension()) {
	    throw new IllegalArgumentException("Interval Matrix inner dimensions must agree.");
	}
	return new IntervalMatrix(lo.plus(hi).times(0.5), abs(hi.minus(lo).times(0.5)));
    }
    int getRowDimension() { return center.getRowDimension(); }
    int getColumnDimension() { return center.getColumnDimension(); }
    Matrix mag() {
	int m = getRowDimension();
	int n = getColumnDimension();
	double[][] c = center.getArray();
	double[][] d = delta.getArray();
	Matrix x = new Matrix(m,n);
	double[][] mg = x.getArray();
	for (int i = 0; i < m; i++) {
	    double[] cr = c[i];
	    double[] dr = d[i];
	    double[] xr = mg[i];
	    for (int j = 0; j < n; j++)
		xr[j] = Math.abs(cr[j]) + dr[j];
	}
	return x;
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
    IntervalMatrix transpose() {
	return new IntervalMatrix(center.transpose(), delta.transpose());
    }
    IntervalMatrix plus(IntervalMatrix B) {
	return new IntervalMatrix(center.plus(B.center), delta.plus(B.delta));
    }
    IntervalMatrix plus(Matrix B) {
	return new IntervalMatrix(center.plus(B), delta);
    }
    IntervalMatrix times(IntervalMatrix B) {
	if (getColumnDimension() != B.getRowDimension()) {
	    throw new IllegalArgumentException("Matrix inner dimensions must agree.");
	}
	int m = getRowDimension();
	int mn = getColumnDimension();
	int n = B.getColumnDimension();
	double[][] ac = center.getArray();
	double[][] ad = delta.getArray();
	double[][] bc = B.center.getArray();
	double[][] bd = B.delta.getArray();
	Matrix xc = new Matrix(m,n);
	Matrix xd = new Matrix(m,n);
	double[][] cc = xc.getArray();
	double[][] cd = xd.getArray();
	double[] bccolj = new double[mn];
	double[] bdcolj = new double[mn];
	for (int j = 0; j < n; j++) {
	    for (int k = 0; k < mn; k++) {
		bccolj[k] = bc[k][j];
		bdcolj[k] = bd[k][j];
	    }
	    for (int i = 0; i < m; i++) {
		double[] acrowi = ac[i];
		double[] adrowi = ad[i];
		double sc = 0.0, sd = 0.0;
		for (int k = 0; k < mn; k++) {
		    double a_c = acrowi[k];
		    double a_d = adrowi[k];
		    double b_c = bccolj[k];
		    double b_d = bdcolj[k];
		    double p1 = (a_c - a_d)*(b_c - b_d);
		    double p2 = (a_c - a_d)*(b_c + b_d);
		    double p3 = (a_c + a_d)*(b_c - b_d);
		    double p4 = (a_c + a_d)*(b_c + b_d);
		    double pl = Math.min(Math.min(p1,p2),Math.min(p3,p4));
		    double ph = Math.max(Math.max(p1,p2),Math.max(p3,p4));
		    sc += 0.5*(pl+ph);
		    sd += 0.5*(ph-pl);
		}
		cc[i][j] = sc;
		cd[i][j] = sd;
	    }
	}
	return new IntervalMatrix(xc,xd);
    }

    IntervalMatrix times(double s) {
	return new IntervalMatrix(center.times(s), delta.times(Math.abs(s)));
    }

    boolean isPositive(int[] permute)
	/* try to prove that summetric interval matrix is always positively defined
	   using Cholesky's algorithm
	*/
    {
	if (getColumnDimension() != getRowDimension()) {
	    throw new IllegalArgumentException("Square matrix expected");
	}
	int n = getColumnDimension();
	if (permute != null && permute.length != n) {
	    throw new IllegalArgumentException("Permute length");
	}
	double[][] c = center.getArray();
	double[][] d = delta.getArray();

	RealInterval l[][] = new RealInterval[n][n];
	for (int i = 0; i < n; i++) {
	    for (int j = 0; j <= i; j++) {
		double cij = permute != null ? c[permute[i]][permute[j]] : c[i][j];
		double dij = permute != null ? d[permute[i]][permute[j]] : d[i][j];
		l[i][j] = new RealInterval(cij - dij, cij + dij);
	    }
	}
	for (int i = 0; i < n; i++)
	{
	    RealInterval s = l[i][i];
	    for (int p = 0; p < i; p++)
	    {
		RealInterval lip = l[i][p];
		RealInterval sq;
		if (lip.lo() < 0 && lip.hi() > 0) {
		    double amax = Math.max(-lip.lo(), lip.hi());
		    sq = new RealInterval(0, amax*amax);
		} else {
		    sq = IAMath.mul(lip,lip);
		}
		s = IAMath.sub(s,sq);
	    }
	    if (s.lo() <= 0) return false;
	    l[i][i] = new RealInterval(Math.sqrt(s.lo()), Math.sqrt(s.hi()));
	    for (int j = i + 1; j < n; j++) {
		s = l[j][i];
		for (int p = 0; p < i; p++) {
		    RealInterval lip = l[i][p];
		    RealInterval ljp = l[j][p];
		    s = IAMath.sub(s, IAMath.mul(lip, ljp));
		}
		l[j][i] = IAMath.div(s, l[i][i]);
	    }
	}
	return true;
    }
}
