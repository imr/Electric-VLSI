/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: StdCellParams.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.tool.generator.layout;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.prototype.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.database.network.*;
import com.sun.electric.database.variable.*;
import com.sun.electric.technology.*;

/** The bottom of the PMOS well and the top of the NMOS well are at
 * y=0.  PMOS tracks are numbered from 1 to nbPmosTracks(). These
 * numbers correspond to the lowest and highest, respectively,
 * unblocked tracks in the PMOS region.  NMOS tracks are numbered from
 * -1 to -nbNmosTracks().  These numbers correspond to the highest and
 * lowest, respectively, unblocked tracks in the NMOS region. */
public class StdCellParams {
	// ---------------------- private classes --------------------------------
	private static class TrackBlockages {
		private static class Blockage {
			double bot, top;
			Blockage(double center, double width) {
				bot = center - width / 2;
				top = center + width / 2;
			}
		}
		private ArrayList blockages = new ArrayList();
		private double space;

		TrackBlockages(double space) {
			this.space = space;
		}

		void addBlockage(double center, double width) {
			blockages.add(new Blockage(center, width));
		}
		boolean isBlocked(double center, double width) {
			double top = center + width / 2 + space;
			double bot = center + width / 2 - space;
			for (int i = 0; i < blockages.size(); i++) {
				Blockage b = (Blockage) blockages.get(i);
				if (b.bot < top && b.top > bot)
					return true;
			}
			return false;
		}
	}

	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}

	// -------------------------- private data ---------------------------------
	private static final double DEF_SIZE = LayoutLib.DEF_SIZE;
	private double nmosWellHeight = 70;
	private double pmosWellHeight = 70;
	private double gndY = -21.0;
	private double vddY = 21.0;
	private double gndWidth = 10;
	private double vddWidth = 10;
	private double trackPitch = 7;
	private double trackWidth = 4;
	private double metalSpace = 3;
	// An "enable" style Nand gate has a weak pullup. This is how much
	// weaker than normal the PMOS is.
	private double enableGateStrengthRatio = .1;

	// Separate well ties from power and ground to allow threshold
	// control via substrate bias.
	private boolean separateWellTies = false;
	// maximum distance from well tie to any point in well
	private double maxWellTieRadius = 300;
	private String pmosWellTieName = "vnw";
	private String nmosWellTieName = "vsb";
	private double nmosWellTieY, pmosWellTieY;

	private double minGateWid = 3;
	private double diffContWid = 5;
	private double maxMosWidth = 45;

	// Critera for reducing the number of distict sizes.
	// Default to 10% error.
	private double sizeErr = 0.1;

	private static final double selectOverhangsDiffCont = 4.5;
	private static final double selectSpace = 2;
	private static final double m1OverhangsDiffCont = 2;
	private static final double m1Space = 3;
	private static final double selectOverhangsDiff = 2;

	private ArrayList nmosTracks = new ArrayList();
	private ArrayList pmosTracks = new ArrayList();
	private int botNmosTrack, topNmosTrack, botPmosTrack, topPmosTrack;
	private Library schemLib = null;
	private Library layoutLib = null;
	private boolean doubleStrapGate = false;
	private boolean exhaustivePlace = true;
	private int nbPlacerPerms = 40000;
	private boolean simpleName = false;
	private String vddExportName = "vdd";
	private String gndExportName = "gnd";
	private PortProto.Characteristic vddExportRole = PortProto.Characteristic.PWR;
	private PortProto.Characteristic gndExportRole = PortProto.Characteristic.GND;

	// ------------------------ private methods -----------------------------

	private void init(Library lib) {
		layoutLib = lib;
		init();
	}
	private void init() {
		TrackBlockages blockages = new TrackBlockages(metalSpace);

		// The symmetric nand2 and nand3 reserve 2 metal-2 tracks for
		// internal use only to connect the output.
		blockages.addBlockage(11, 4);
		blockages.addBlockage(-11, 4);

		// Power and ground also block metal-2 tracks
		blockages.addBlockage(vddY, vddWidth);
		blockages.addBlockage(gndY, gndWidth);

		generateTracks(blockages);

		nmosWellTieY = gndY;
		pmosWellTieY = vddY;
		if (separateWellTies) {
			// Place well ties close to top and bottom of cells
			nmosWellTieY = getTrackY(- (nbNmosTracks() - 1));
			blockages.addBlockage(nmosWellTieY, trackWidth);
			pmosWellTieY = getTrackY(nbPmosTracks() - 1);
			blockages.addBlockage(pmosWellTieY, trackWidth);
			// recompute available tracks with new blockages
			generateTracks(blockages);
		}
	}

	// create a list of tracks that don't overlap vdd or gnd
	private void generateTracks(TrackBlockages blockages) {
		// this may be called multiple times if setSeparateWellTies is called
		// 0 is an illegal index
		nmosTracks.clear();
		pmosTracks.clear();

		// tracks in PMOS region
		for (double y = trackPitch / 2; y < pmosWellHeight; y += trackPitch) {
			if (!blockages.isBlocked(y, trackWidth))
				pmosTracks.add(new Double(y));
		}

		// tracks in NMOS region
		for (double y = -trackPitch / 2;
			y > -nmosWellHeight;
			y -= trackPitch) {
			if (!blockages.isBlocked(y, trackWidth))
				nmosTracks.add(new Double(y));
		}
	}

	private double quantizeMantissa(double mantissa) {
		double szRatio = (1 + sizeErr) / (1 - sizeErr);
		// find n such that (szRatio^n) is closest to mantissa
		double logBaseSzRatio = Math.log(mantissa) / Math.log(szRatio);
		double floorN = Math.floor(logBaseSzRatio);
		double ceilN = Math.ceil(logBaseSzRatio);
		double hiApprox = Math.pow(szRatio, ceilN);
		double loApprox = Math.pow(szRatio, floorN);

		return (hiApprox - mantissa < mantissa - loApprox)
			? (hiApprox)
			: (loApprox);
	}

	private int calcNbGroups(double maxAvailWid, double totWid, int groupSz) {
		int nbGroups = (int) Math.ceil(totWid / maxAvailWid / groupSz);

		// If groupSz is even then we always create an even number of
		// fingers and we don't have to add more fingers to reduce
		// diffusion capacitance.
		if (groupSz % 2 == 0)
			return nbGroups;

		// If nbGroups is even then we always create an even number of
		// fingers and we don't have to add more fingers to reduce
		// diffusion capacitance
		if (nbGroups % 2 == 0)
			return nbGroups;

		// try adding one more group to get an even number of fingers
		int roundupGroups = nbGroups + 1;
		double wid = totWid / groupSz / roundupGroups;

		// Don't fold if gate width is less than width of diffusion
		// contact.
		if (wid >= diffContWid)
			nbGroups = roundupGroups;

		return nbGroups;
	}

	private void fillDiffNotch(PortInst prevPort, PortInst thisPort, 
	                           FoldedMos mos) {
		double diffWid = mos.getPhysWidth();
		Cell f = mos.getSrcDrn(0).getNodeInst().getParent();
		PrimitiveNode diffCont =
			mos instanceof FoldedPmos ? Tech.pdm1 : Tech.ndm1;
		ArcProto diffArc = mos instanceof FoldedPmos ? Tech.pdiff : Tech.ndiff;
		NodeProto diffNode =
			mos instanceof FoldedPmos ? Tech.pdNode : Tech.ndNode;

		double prevX = prevPort.getBounds().getCenterX();
		double thisX = thisPort.getBounds().getCenterX();
		double dist = thisX - prevX;

		// If they overlap perfectly or if they're so far apart there's no
		// select notch then no notches of any kind are possible.
		if (dist == 0 || (dist >= selectOverhangsDiffCont * 2 + selectSpace))
			return;

		// Fill a notch with diffusion.
		//
		// I can't fill notches using a diffusion arc because the span in
		// the Y coordinate of the port of the diffusion contact may not
		// include the center of such an arc. This occurs for MOS
		// transistors with a single contact cut.
		//
		// I therefore fill the notch using a diffusion node.
		double mosY = mos.getMosCenterY();

		NodeInst dFill = LayoutLib.newNodeInst(diffNode, thisX-dist/2, mosY, 
											   dist, diffWid, 0, f);
		double contY = thisPort.getBounds().getCenterY(); // contact is always on grid
		LayoutLib.newArcInst(diffArc, DEF_SIZE, thisPort, thisX, contY, 
							 dFill.getOnlyPortInst(), 
							 dFill.getOnlyPortInst().getBounds().getCenterX(), 
							 contY);
		addSelAroundDiff(dFill);

		// Never place wide arcs directly onto the FoldedMos
		// diffusion-metal1 contacts because they become bad width hints.
		if (dist < m1OverhangsDiffCont * 2 + m1Space) {
			// m1 is 1 lambda narrower than diffusion contact width
			double m1Wid = mos.getDiffContWidth() - 1;
			NodeInst mFill = 
				LayoutLib.newNodeInst(Tech.m1Node, thisX-dist/2, contY,	dist,
									  m1Wid, 0, f);
			LayoutLib.newArcInst(Tech.m1, DEF_SIZE, thisPort, mFill.getOnlyPortInst());
		}
	}

	private static FoldedMos getRightMos(Object a) {
		if (a instanceof FoldedMos)  return (FoldedMos) a;
		error(!(a instanceof FoldedMos[]), "not FoldedMos or FoldedMos[]");
		FoldedMos[] moss = (FoldedMos[]) a;
		return moss[moss.length - 1];
	}

	private static String trkMsg(Object key, Cell schem) {
		return "Track assignment for export: "
			+ key
			+ " in Cell: "
			+ schem.getName()
			+ ".\n    ";
	}

	//----------------------------- public constants --------------------------
	/** This class allows the user to specify which source/drains
	 * wireVddGnd() should connect to power or ground.  Most users will
	 * be happy with the predefined instances: ODD or EVEN. */
	public interface SelectSrcDrn {
		public boolean connectThisOne(int mosNdx, int srcDrnNdx);
	}

	public static final SelectSrcDrn EVEN = new SelectSrcDrn() {
		public boolean connectThisOne(int mosNdx, int srcDrnNdx) {
			return srcDrnNdx % 2 == 0;
		}
	};

	public static final SelectSrcDrn ODD = new SelectSrcDrn() {
		public boolean connectThisOne(int mosNdx, int srcDrnNdx) {
			return srcDrnNdx % 2 == 1;
		}
	};

	//------------------------ public methods --------------------------------------

	//------------------------------------------------------------------------------
	// Allow user to customize standard cell characteristics
	//
	/** Attach the well ties to special busses rather than to Vdd and Gnd. */
	public void setDoubleStrapGate(boolean val) {
		doubleStrapGate = val;
	}

	/** GasP generators use exhaustive search for gate placement */
	public void setExhaustivePlace(boolean val) {
		exhaustivePlace = val;
	}

	/** Set number of permuations GasP placer should try before giving
	 * up. */
	public void setNbPlacerPerms(int i) {
		nbPlacerPerms = i;
	}

	/** Units of lambda */
	public void setNmosWellHeight(double h) {
		nmosWellHeight = h;
		init();
	}

	/** Units of lambda */
	public void setPmosWellHeight(double h) {
		pmosWellHeight = h;
		init();
	}

	/** Set the maximum width of a each MOS finger. Units of lambda */
	public void setMaxMosWidth(double wid) {
		maxMosWidth = wid;
	}

	/** Turn on Network Consistency Checking after each gate is generated.
	 *<p> This just checks topology and ignores sizes. */
	public void enableNCC(String schemLibFileNm) {
		schemLib = LayoutLib.openLibForRead(schemLibFileNm, schemLibFileNm);
		error(schemLib==null, "can't open schematic library: "+schemLibFileNm);
	}

	public void setVddExportName(String vddNm) {vddExportName=vddNm;}
	public String getVddExportName() {return vddExportName;}
	public void setGndExportName(String gndNm) {gndExportName=gndNm;}
	public String getGndExportName() {return gndExportName;}
	public void setVddExportRole(PortProto.Characteristic vddRole) {
		vddExportRole = vddRole;
	}
	public PortProto.Characteristic getVddExportRole() {
		return vddExportRole;
	}
	public void setGndExportRole(PortProto.Characteristic gndRole) {
		gndExportRole = gndRole;
	}
	public PortProto.Characteristic getGndExportRole() {
		return gndExportRole;
	}

	//------------------------------------------------------------------------------
	// Utilities for gate generators

	public StdCellParams(Library lib) {init(lib);}

	public double getNmosWellHeight() {
		return nmosWellHeight;
	}
	public double getPmosWellHeight() {
		return pmosWellHeight;
	}
	public boolean getDoubleStrapGate() {
		return doubleStrapGate;
	}
	public boolean getExhaustivePlace() {
		return exhaustivePlace;
	}
	public int getNbPlacerPerms() {
		return nbPlacerPerms;
	}
	public double getCellBot() {
		return -nmosWellHeight;
	}
	public double getCellTop() {
		return pmosWellHeight;
	}
	public double getGndY() {
		return gndY;
	}
	public void setGndY(double y) {
		gndY = y;
		init();
	}
	public double getVddY() {
		return vddY;
	}
	public void setVddY(double y) {
		vddY = y;
		init();
	}
	public double getGndWidth() {
		return gndWidth;
	}
	public double getVddWidth() {
		return vddWidth;
	}
	/** Gets the Y coordinate of the ith available track. Available
	 * tracks exclude Vdd, Gnd, and reserved tracks. */
	public double getTrackY(int i) {
		error(i==0, "StdCellParams.getTrackY: 0 is an illegal track index");
		return i>0
			? ((Double) pmosTracks.get(i - 1)).doubleValue()
			: ((Double) nmosTracks.get(-i - 1)).doubleValue();
	}
	/** A physical track number enumerates all tracks regardless of
	 * whether the track is blocked. The value 0 is illegal. Tracks 1
	 * and higher are PMOS tracks. Tracks -1 and lower are NMOS
	 * tracks */
	public double getPhysTrackY(int i) {
		error(i==0, "StdCellParams.getPhysTrackY: 0 is illegal track index");
		return i>0
		    ? trackPitch/2 + (i-1)*trackPitch
		    : -trackPitch/2 + (i+1)*trackPitch;
	}
	/*
	public HashMap physTracksToYCoords(HashMap physTracks) {
	  HashMap yCoords = new HashMap();
	  for (Iterator it=physTracks.keySet().iterator(); it.hasNext();) {
	    String key = (String) it.next();
	    int physTrk = ((Integer)physTracks.get(key)).intValue();
	    Double y = new Double(getPhysTrackY(physTrk));
	    yCoords.put(key, y);
	  }
	  return yCoords;
	}
	*/
	public double getTrackPitch() {return trackPitch;}

	/** Get the number of NMOS tracks. The indices of NMOS tracks
	 * range from -1 to -nbNmosTracks(). The value 0 is an illegal
	 * index. */
	public int nbNmosTracks() {
		return nmosTracks.size();
	}

	/** Get the number of PMOS tracks. The indices of PMOS tracks
	 * range from 1 to nbPmosTracks().  The value 0 is an illegal
	 * index. */
	public int nbPmosTracks() {return pmosTracks.size();}

	public boolean getSeparateWellTies() {return separateWellTies;}
	/** Connect well ties to separate exports rather than vdd and gnd */
	public void setSeparateWellTies(boolean b) {
		separateWellTies = b;
		init();
	}

	public double getNmosWellTieY() {return nmosWellTieY;}
	public double getPmosWellTieY() {return pmosWellTieY;}
	public double getNmosWellTieWidth() {
		return separateWellTies ? trackWidth : gndWidth;
	}
	public double getPmosWellTieWidth() {
		return separateWellTies ? trackWidth : vddWidth;
	}
	public String getNmosWellTieName() {
		return separateWellTies ? "vsb" : gndExportName;
	}
	public String getPmosWellTieName() {
		return separateWellTies ? "vnw" : vddExportName;
	}
	public PortProto.Characteristic getNmosWellTieRole() {
		return separateWellTies ? PortProto.Characteristic.IN : gndExportRole;
	}
	public PortProto.Characteristic getPmosWellTieRole() {
		return separateWellTies ? PortProto.Characteristic.IN : vddExportRole;
	}
	public void setSimpleName(boolean b) {simpleName = b;}
	public boolean getSimpleName() {return simpleName;}

	// maximum distance between well contacts
	public double getWellTiePitch() {
		double tieToPwellTop = 0 - getNmosWellTieY();
		double tieToPwellBot = getNmosWellTieY() - -nmosWellHeight;
		double tieToPwellTopBot = Math.max(tieToPwellTop, tieToPwellBot);
		// Right triangle:
		//  hypotenuse is maxWellTieRadius
		//  delta Y is maxGndToWellTopBot
		//  delta X is nmosWellTieDistance
		double nmosWellTieDistance =
			Math.sqrt(
				Math.pow(maxWellTieRadius, 2) - Math.pow(tieToPwellTopBot, 2));

		double tieToNwellTop = pmosWellHeight - getPmosWellTieY();
		double tieToNwellBot = getPmosWellTieY() - 0;
		double tieToNwellTopBot = Math.max(tieToNwellTop, tieToNwellBot);
		double pmosWellTieDistance =
			Math.sqrt(
				Math.pow(maxWellTieRadius, 2) - Math.pow(tieToNwellTopBot, 2));

		double tiePitch =
			2 * Math.min(nmosWellTieDistance, pmosWellTieDistance);

		// Safety margin: add twice as many ties as necessary
		return (int) tiePitch/2;
	}
	

	// An "enable" style Nand gate has a weak pullup. This is how much
	// weaker than normal the PMOS is.
	public double getEnableGateStrengthRatio() {
		return enableGateStrengthRatio;
	}

	/** round to nearest multiple of 1/2 lambda */
	public double roundGateWidth(double w) {
		return Math.rint(w * 2) / 2;
	}

	/** quantize size.  Temporary hack because it doesn't control errors
	 * precisely */
	public double roundSize(double s) {
		if (s == 0)
			return s;
		double q = quantizeSize(s);
		double e = (s - q) / s;
		double qe = Math.rint(e * 100000) / 100000;
		//System.out.println("desired: "+s+" quantized: "+q+" error: "+qe);
		return q;
		//return ((int) (s*10+.5)) / 10.0;
	}

	public void setSizeQuantizationError(double err) {
		error(err >= 1, "quantization error must be less than 1.0");
		error(err < 0, "quantization error must be positive");
		sizeErr = err;
	}

	public double quantizeSize(double desiredSize) {
		// express desiredSize as (mantisa * 10^exponent)
		double exponent = Math.floor(Math.log(desiredSize) / Math.log(10));
		// 1.0 <= mantissa < 10
		double mantissa = desiredSize / Math.pow(10, exponent);

		double quantMant = sizeErr!=0 ? quantizeMantissa(mantissa) : mantissa;

		// now round the quantized mantissa to 3 decimal places
		double roundMant = Math.rint(quantMant * 100) / 100;

		return Math.pow(10, exponent) * roundMant;
	}

	/** Add qualifiers to Cell name to reflect StdCell parameters
	 * "_NH70" for NMOS region height of 70 lambda
	 * "_PH70" for PMOS region height of 70 lambda
	 * "_MW70" for maximum transistor width of 70 lambda */
	public String parameterizedName(String nm) {
		if (!vddExportName.equals("vdd")) nm += "_pwr";
		if (simpleName) return nm;
		return nm
			+"_NH"+nmosWellHeight+"_PH"+pmosWellHeight
			+"_MW"+maxMosWidth+"_VY"
			+vddY+"_GY"+ gndY;
	}

	/** Add qualifiers to Cell name to reflect StdCell parameters and part strength
	 * "_NH70" for NMOS region height of 70 lambda
	 * "_PH70" for PMOS region height of 70 lambda
	 * "_MW70" for maximum transistor width of 70 lambda
	 * "_X12.5" for strength of 12.5
	 * "{lay}" to indicate this is a layout Cell */
	public String sizedName(String nm, double sz) {
		String num = "" + (sz + 1000); // Add leading zeros to size
		// so Gallery sorts properly.
		num = num.substring(1);
		return parameterizedName(nm) + "_X" + num + "{lay}";
	}

	private NodeInst addNmosWell(double loX, double hiX, double y, Cell cell) {
		NodeInst well =
			LayoutLib.newNodeInst(Tech.pwell, (loX+hiX)/2, y-nmosWellHeight/2,
								hiX-loX, nmosWellHeight, 0, cell);
		well.setHardSelect();
		return well;
	}
	private NodeInst addPmosWell(double loX, double hiX, double y, Cell cell) {
		NodeInst well =
			LayoutLib.newNodeInst(Tech.nwell, (loX+hiX)/2, y+pmosWellHeight/2,
								  hiX-loX, pmosWellHeight, 0, cell);
		well.setHardSelect();
		return well;
	}

	public NodeInst addNmosWell(double loX, double hiX, Cell cell) {
		return addNmosWell(loX, hiX, 0, cell);
	}
	public NodeInst addPmosWell(double loX, double hiX, Cell cell) {
		return addPmosWell(loX, hiX, 0, cell);
	}

	/** Given an array of NodeInsts in a row, add wells to both ends of
	    the row to bring the row to minX and maxX. */
	public void addWellsForRow(
		ArrayList row,
		double minX,
		double maxX,
		Cell cell) {
		NodeInst first = (NodeInst) row.get(row.size() - 1);
		double rowMinX = first.getBounds().getMinX();
		if (rowMinX < minX) {
			addPmosWell(minX, rowMinX, first.getAnchorCenterY(), cell);
			addNmosWell(minX, rowMinX, first.getAnchorCenterY(), cell);
		}
		NodeInst last = (NodeInst) row.get(row.size() - 1);
		double rowMaxX = last.getBounds().getMaxX();
		if (rowMaxX < maxX) {
			addPmosWell(rowMaxX, maxX, first.getAnchorCenterY(), cell);
			addNmosWell(rowMaxX, maxX, first.getAnchorCenterY(), cell);
		}
	}

	/** essential bounds for PMOS only cells */
	public void addPstackEssentialBounds(double loX, double hiX, Cell cell) {
		LayoutLib.newNodeInst(Tech.essentialBounds, loX, 0, DEF_SIZE, DEF_SIZE,
							  180, cell);
		LayoutLib.newNodeInst(Tech.essentialBounds, hiX, pmosWellHeight, DEF_SIZE, 
							  DEF_SIZE, 0, cell);
	}
	/** essential bounds for NMOS only cells */
	public void addNstackEssentialBounds(double loX, double hiX, Cell cell) {
		LayoutLib.newNodeInst(Tech.essentialBounds, loX, -nmosWellHeight, DEF_SIZE, 
							  DEF_SIZE, 180, cell);
		LayoutLib.newNodeInst(Tech.essentialBounds, hiX, 0, DEF_SIZE,
							  DEF_SIZE, 0, cell);
	}
	/** essential bounds for cells with both NMOS and PMOS */
	public void addEssentialBounds(double loX, double hiX, Cell cell) {
		LayoutLib.newNodeInst(Tech.essentialBounds, loX, -nmosWellHeight, DEF_SIZE, 
		                      DEF_SIZE, 180, cell);
		LayoutLib.newNodeInst(Tech.essentialBounds, hiX, pmosWellHeight, DEF_SIZE, 
			                  DEF_SIZE, 0, cell);
	}

	/** Print a warning if strength is less than the minimum allowable.
	 * Always return at least the minimum allowable strength. */
	public double checkMinStrength(
		double specified,
		double minAllowable,
		String gateNm) {
		if (specified<minAllowable) {
			System.out.println("Can't make: "+gateNm+" this small: X="
			                   +specified+", Using X="+minAllowable
			                   +" instead");
		}
		return Math.max(specified, minAllowable);
	}

	/** Calculate the number of folds and the width of a MOS
	 * transistor. Given that there is a limited physical height into
	 * which a MOS transistor must fit, divide the total required width:
	 * totWid into fingers.  Each finger must have width less than or
	 * equal to spaceAvailWid.
	 *
	 * <p> If it is possible, allocate an even number of fingers so that
	 * the left most and right most diffusion contacts may be connected
	 * to power or ground to reducing the capacitance of the inner
	 * switching diffusion contacts.
	 * @param spaceAvailWid the height in the standard cell that is
	 * available for the diffusion of the MOS transistor.
	 * @param totWid the total electrical width required.
	 * @param groupSz This method creates fingers in multiples of
	 * groupSz. For example, if groupSz is 2, then only even numbers of
	 * fingers are created. This is needed when one FoldedMos is
	 * actually going to be wired up as 2 identical, independent
	 * transistors, for example the 2 PMOS pullups for a 2-input NAND
	 * gate. */
	public FoldsAndWidth calcFoldsAndWidth(
		double spaceAvailWid,
		double totWid,
		int groupSz) {
		if (totWid == 0)
			return null;
		double maxAvailWid = Math.min(spaceAvailWid, maxMosWidth);
		int nbGroups = calcNbGroups(maxAvailWid, totWid, groupSz);

		double gateWid = roundGateWidth(totWid / groupSz / nbGroups);

		// If we're unfortunate, rounding up gate width causes gate's width
		// to exceed space available.
		if (gateWid > maxAvailWid) {
			nbGroups = calcNbGroups(maxAvailWid - .5, totWid, groupSz);
			gateWid = roundGateWidth(totWid / groupSz / nbGroups);
		}

		double physWid = Math.max(diffContWid, gateWid);
		if (gateWid < minGateWid)
			return null;
		return new FoldsAndWidth(nbGroups * groupSz, gateWid, physWid);
	}

	/** Fix notch errors between adjacent source/drain regions.
	 *
	 * <p>Mos transistors with source/drain regions that are too close
	 * to each other result in notch errors for diffusion, metal1,
	 * and/or select. Fix these notch errors by running diffusion,
	 * and/or metal1 between the adjacent diffusion regions.
	 * @param moss An array of adjacent FoldedMos transistors arranged
	 * from left to right. */
	public void fillDiffNotches(FoldedMos[] moss) {
		error(moss.length == 0, "fillDiffNotches: no transistors?");
		FoldedMos mos = moss[0];

		for (int i = 1; i < moss.length; i++) {
			PortInst thisPort = moss[i].getSrcDrn(0);
			PortInst prevPort =
				moss[i - 1].getSrcDrn(moss[i - 1].nbSrcDrns() - 1);
			fillDiffNotch(prevPort, thisPort, mos);
		}
	}

	/** Wire pmos or nmos to vdd or gnd, respectively.  Add an export
	 * if there is none. */
	public void wireVddGnd(FoldedMos[] moss, SelectSrcDrn select, Cell p) {
		FoldedMos mos = moss[0];

		PortInst leftDiff = mos.getSrcDrn(0);
		Cell f = leftDiff.getNodeInst().getParent();
		double busWid = mos instanceof FoldedPmos ? vddWidth : gndWidth;
		double busY = mos instanceof FoldedPmos ? vddY : gndY;
		TrackRouter net = new TrackRouterH(Tech.m2, busWid, busY, p);

		String exportNm = mos instanceof FoldedPmos ? vddExportName : gndExportName;
		if (f.findPortProto(exportNm)==null) {
			// The export doesn't yet exist.  Create and export a metal2 pin
			// aligned with the first diffusion.
			double x = leftDiff.getBounds().getCenterX();
			NodeInst pinProt = LayoutLib.newNodeInst(Tech.m2pin, x,
			                                         busY, DEF_SIZE, DEF_SIZE, 0, f);
			PortInst pin = pinProt.getOnlyPortInst();
			Export e = Export.newInstance(f, pin, exportNm);
			PortProto.Characteristic role =	
				mos instanceof FoldedPmos ? vddExportRole : gndExportRole;
			e.setCharacteristic(role);

			// Connect the export to itself using a standard width power or
			// ground strap.  The width of this strap serves as a hint to
			// Electric as to the width to use to connect to this export at
			// the next level up.
			LayoutLib.newArcInst(Tech.m2, busWid, pin, pin);

			net.connect(pin);
		}
		double diffY = leftDiff.getBounds().getCenterY();
		double notchLoY = Math.min(busY - busWid / 2, diffY);
		double notchHiY = Math.max(busY + busWid / 2, diffY);
		PortInst lastDiff = null;
		for (int i=0; i<moss.length; i++) {
			for (int j=0; j<moss[i].nbSrcDrns(); j++) {
				if (select.connectThisOne(i, j)) {
					PortInst thisDiff = moss[i].getSrcDrn(j);
					net.connect(thisDiff);

					if (lastDiff!=null) {
						// Check to see if we just created a notch.
						double leftX = lastDiff.getBounds().getCenterX();
						double rightX = thisDiff.getBounds().getCenterX();
						error(leftX>rightX,
							  "wireVddGnd: trans not sorted left to right");
						double deltaX = rightX - leftX;
						if (deltaX>0
							&& deltaX < m1OverhangsDiffCont*2+m1Space) {
							// Fill notches, sigh! (This is starting to lose 
							// it's novelty value.)
							//
							// Make height integral number of lambdas so 
							// centerY will be on .5 lambda grid and connecting
							// to center won't generate CIF resolution errors.
							double dY = Math.ceil(notchHiY - notchLoY);
							NodeInst patchNode = 
							  LayoutLib.newNodeInst(Tech.m1Node, (leftX+rightX)/2, notchLoY+dY/2,
							  deltaX, dY, 0, f);
							PortInst patch = patchNode.getOnlyPortInst();
							LayoutLib.newArcInst(Tech.m1, DEF_SIZE, patch, thisDiff);
						}
					}
					lastDiff = thisDiff;
				}
			}
		}
	}

	public void wireVddGnd(FoldedMos mos, SelectSrcDrn select, Cell p) {
		wireVddGnd(new FoldedMos[] { mos }, select, p);
	}

	public boolean nccEnabled() {
		return schemLib != null;
	}

	/** Perform Network Consistency Check if the user has so requested */
	public void doNCC(Cell layout, String schemNm) {
		/* Not implemented
		if (schemLib == null)
			return;
		Cell schem = schemLib.findCell(schemNm);
		error(schem == null, "can't find schematic: " + schemNm);

		NccOptions options = new NccOptions();
		options.absTolerance = 200;
		options.checkExportNames = true;
		options.checkSizes = false;
		options.hierarchical = true;
		options.ignorePwrGnd = false;
		options.interactive = false;
		options.mergeParallel = true;
		options.mergeSeries = false;
		options.percentTolerance = 100;
		options.preAnalyze = false;
		options.recurse = false;
		options.verboseText = false;
		options.verboseGraphics = false;

		boolean mismatch =
			Electric.networkConsistencyCheck(schem, null, layout, options);
		error(mismatch, "layout not topologically identical to schematic!");
		*/
	}

	public static double getSize(NodeInst iconInst, VarContext context) {
		Variable var = iconInst.getVar("ATTR_X");
		if (var==null)
			var = iconInst.getVar("ATTR_S");
		if (var==null)
			var = iconInst.getVar("ATTR_SP");
		if (var==null)
			var = iconInst.getVar("ATTR_SN");

		if (var==null) {
			System.out.println("can't find size, using 40");
			return 40;
		}
		Object val = context.evalVar(var);
		if (val instanceof Number)  return ((Number)val).doubleValue();
		error(true, "an Icon's size isn't a numeric value");
		return 0;
	}

	/** Look for parts in layoutLib */
	public Cell findPart(String partNm, double sz) {
		return findPart(sizedName(partNm, sz));
	}
	public Cell findPart(String partNm) {
		return layoutLib.findNodeProto(partNm);
	}
	public Cell newPart(String partNm, double sz) {
		return newPart(sizedName(partNm, sz));
	}
	public Cell newPart(String partNm) {
		error(findPart(partNm) != null, "Cell already exists: " + partNm);
		Cell p = Cell.newInstance(layoutLib, partNm);
		return p;
	}

	public static double getRightDiffX(FoldedMos m) {
		return m.getSrcDrn(m.nbSrcDrns() - 1).getBounds().getCenterX();
	}

	public static double getRightDiffX(FoldedMos[] moss) {
		return getRightDiffX(getRightMos(moss));
	}

	/** Find the X coordinate of the right most diffusion. Objects a and
	 * b may be either a FoldedMos or an array of FoldedMos'es */
	public static double getRightDiffX(Object a, Object b) {
		FoldedMos ra = getRightMos(a);
		FoldedMos rb = getRightMos(b);
		return Math.max(getRightDiffX(ra), getRightDiffX(rb));
	}

	public static void addEssentialBoundsFromChildren(Cell cell) {
		double loX, loY, hiX, hiY;
		loX = loY = Double.MAX_VALUE;
		hiX = hiY = Double.MIN_VALUE;
		for (Iterator it=cell.getNodes(); it.hasNext();) {
			Rectangle2D b = ((NodeInst) it.next()).getBounds();
			loX = Math.min(loX, b.getMinX());
			loY = Math.min(loY, b.getMinY());
			hiX = Math.max(hiX, b.getMaxX());
			hiY = Math.max(hiY, b.getMaxY());
		}
		LayoutLib.newNodeInst(Tech.essentialBounds, loX, loY, 
		                      DEF_SIZE, DEF_SIZE, 180, cell);
		LayoutLib.newNodeInst(Tech.essentialBounds, hiX, hiY,
		                      DEF_SIZE, DEF_SIZE, 0, cell);
	}

	public HashMap getSchemTrackAssign(Cell schem) {
		HashMap schAsgn = new HashMap();
		for (Iterator it = schem.getPorts(); it.hasNext();) {
			Export e = (Export) it.next();
			Object val = e.getVar("ATTR_track");
			String key = e.getName();
			if (val == null)
				continue;
			schAsgn.put(key, val);
		}
		validateTrackAssign(schAsgn, schem);
		return schAsgn;
	}

	public void validateTrackAssign(HashMap asgn, Cell s) {
		HashMap trkToExp = new HashMap();
		for (Iterator it = asgn.keySet().iterator(); it.hasNext();) {
			Object k = it.next();
			Object v = asgn.get(k);
			// check types of key and value
			error(!(k instanceof String),
				  "Track assignment key not String: "+k);
			error(!(v instanceof Integer),
				  trkMsg(k,s)+"Value not Integer: "+v);

			// range check track number
			int track = ((Integer) v).intValue();
			error(track==0,
				  trkMsg(k,s)+"Track must be <=-1 or >=1, 0 is illegal");
			/*
			error(track<min, trkMsg(k,s) + "Track too negative: "+track+
			  ". Only: "+(-min)+" NMOS tracks are available in this cell.");
			error(track>max, trkMsg(k,s) + "Track too positive: "+track+
			  ". Only: "+max+" PMOS tracks are available in this cell.");
			*/

			Object oldK = trkToExp.get(v);
			if (oldK != null) {
				// Issue warning if two exports assigned to the same track
				System.out.println(
					trkMsg(k, s)
						+ "Track: "
						+ v
						+ " is shared by export: "
						+ oldK);
			}
			trkToExp.put(v, k);
		}
	}

	/** Add select node to ensure there is select surrounding the
	 * specified diffusion node*/
	public void addSelAroundDiff(NodeInst diffNode) {
		NodeProto prot = diffNode.getProto();
		error(prot!=Tech.pdNode && prot!=Tech.ndNode,
			  "addSelectAroundDiff: only works with MOSIS CMOS diff nodes");
		NodeProto sel = prot == Tech.pdNode ? Tech.pselNode : Tech.nselNode;
		Rectangle2D r = diffNode.getBounds();
		double w = r.getWidth() + selectOverhangsDiff * 2;
		double h = r.getHeight() + selectOverhangsDiff * 2;
		Cell f = diffNode.getParent();
		LayoutLib.newNodeInst(sel, r.getCenterX(), r.getCenterY(), w, h, 0, f);
	}

}
