package com.sun.electric.tool.ncc.result;

import java.io.Serializable;

import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.netlist.Part;
import com.sun.electric.tool.ncc.result.PartReport.PartReportable;


public class SizeMismatch {
	public static abstract class Mismatch implements Serializable {
		private StringBuffer sb = new StringBuffer();
		private void aln(String s) {sb.append(s); sb.append("\n");}
		public final double min, max;
		public final PartReport minPart, maxPart;
        public final int minNdx, maxNdx;
		Mismatch(double min, PartReportable minPart, int minNdx, 
                 double max, PartReportable maxPart, int maxNdx) {
			this.min=min; this.max=max;
            this.minNdx = minNdx; this.maxNdx = maxNdx;
			this.minPart = new PartReport(minPart); 
			this.maxPart = new PartReport(maxPart);
		}
		public double relErr() {return (max-min)/min;}
		public double absErr() {return max-min;}
		public abstract String widLen();
		public abstract String wl();
		@Override public String toString() {
			// don't round if error will round to zero
			double relErr, absErr, minSz, maxSz;
			if (relErr()*100<.1 || absErr()<.1) {
				relErr = relErr()*100;
				absErr = absErr();
				minSz = min;
				maxSz = max;
			} else {
				relErr = NccUtils.round(relErr()*100,1);
				absErr = NccUtils.round(absErr(),2);
				minSz = NccUtils.round(min,2);
				maxSz = NccUtils.round(max,2);
			}
			aln("    "+minPart.getTypeString()+
				" "+widLen()+"s don't match. "+
				" relativeError="+relErr+"%"+
				" absoluteError="+absErr);
			aln("      "+wl()+"="+minSz+" for "+minPart.fullDescription());
			aln("      "+wl()+"="+maxSz+" for "+maxPart.fullDescription());
			return sb.toString();
		}
	}
    public static class LengthMismatch extends Mismatch {
    	static final long serialVersionUID = 0;


    	@Override public String widLen() {return "length";}		
		@Override public String wl() {return "L";}
		public LengthMismatch(double min, PartReportable minPart, int minNdx, 
                              double max, PartReportable maxPart, int maxNdx) {
			super(min, minPart, minNdx, max, maxPart, maxNdx);
		}
	}
    public static class WidthMismatch extends Mismatch {
    	static final long serialVersionUID = 0;

		@Override public String widLen() {return "width";}
		@Override public String wl() {return "W";}
		public WidthMismatch(double min, PartReportable minPart, int minNdx, 
                             double max, PartReportable maxPart, int maxNdx) {
			super(min, minPart, minNdx, max, maxPart, maxNdx);
		}
	}
}
