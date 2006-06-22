/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ROMGenerator.java
 * Written by: David Harris (David_Harris@hmc.edu)
 * Based on code developed by Frank Lee <chlee@hmc.edu> and Jason Imada <jimada@hmc.edu> 
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
package com.sun.electric.tool.generator;

import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Class to build ROMs from personality tables.
 */
public class ROMGenerator
{
	private static int globalbits;
	private static int folds;
	private static double lambda = 1;
	private static Technology tech;	

	/**
	 * Main entry point for ROM generation.
	 * The method creates layout for a pseudo-nMOS ROM array.
	 *
	 * The personality file has this format:
	 * The first line lists the degree of folding.  For example,
	 * a 256-word x 10-bit ROM with a folding degree of 4 will
	 * be implemented as a 64 x 40 array with 4:1 column multiplexers
	 * to return 10 bits of data while occupying more of a square form
	 * factor. The number of words and degree of folding should be
	 * powers of 2.  The remaining lines of the file list the contents
	 * of each word.  The parser is pretty picky.  There should
	 * be a carriage return after the list word, but no other blank
	 * lines in the file.
	 * 
	 * Here is a sample personality file:
	 *     1
	 *     010101
	 *     011001
	 *     100101
	 *     101010
	 *     4
	 *     00000000
	 *     10000000
	 *     01000000
	 *     11000000
	 *
	 * The tool may be slow, especially for large ROMs.  When done, there may be some
	 * extraneous and bad pins left over; using "Edit / Cleanup Cell / Cleanup
	 * Pins Everywhere" will fix these (though this isn't strictly necessary).
	 *
	 * The ROMs produced should pass DRC and ERC and simulate with IRSIM.  One
	 * was successfully fabricated Spring of 2002 by Genevieve Breed and Matthew
	 * Erler in the MOSIS 0.6 micron AMI process.
	 */
	public static void generateROM()
	{
		// get the personality file
		String romFile = OpenFile.chooseInputFile(FileType.TEXT, null);
		if (romFile == null) return;

		// build the ROM (in a separate Job thread)
		new DoROM(Library.getCurrent(), romFile);
	}

	/**
	 * Method to generate a ROM from a given ROM file.
     * @param destLib destination library.
	 * @param romFile the file to use.
	 */
	public static void generateROM(Library destLib, String romFile)
	{
		// build the ROM (in a separate Job thread)
//		new DoROM(destLib, romFile);
        Cell topLevel = makeAROM(destLib, romFile, "ROMCELL");
	}

	/**
	 * Class to generate a ROM in a separate Job thread.
	 */
	private static class DoROM extends Job
	{
        private Library destLib;
		private String romfile;
		private Cell topLevel;

		private DoROM(Library destLib, String romfile)
		{
			super("ROM Generator", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.destLib = destLib;
			this.romfile = romfile;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// Set the root name of the cells
			String romcell = "ROMCELL";

			// build the ROM
			topLevel = makeAROM(destLib, romfile, romcell);
			fieldVariableChanged("topLevel");
			return true;
		}

        public void terminateOK()
        {
            if (topLevel != null)
                WindowFrame.createEditWindow(topLevel);
        }
	}

	/**
	 * Main method to build a ROM.
     * @param destLib destination library.
	 * @param romfile the disk file with the ROM personality.
	 * @param romcell the root name of all ROM cells.
	 * @return the top-level cell.
	 */
	public static Cell makeAROM(Library destLib, String romfile, String romcell)
	{
		NodeInst ap1, ap2, ap3, ap4;
		PortProto apport1, apport2, apport3, apport4;
		double[] appos1, appos2, appos3, appos4;

		// presume MOSIS CMOS
		tech = Technology.findTechnology("mocmos");	

		int[][] romarray = romarraygen(romfile);
	
		String dpr  = new String(romcell+"_decoderpmos");
		String dnr  = new String(romcell+"_decodernmos");
		String dpm  = new String(romcell+"_decoderpmosmux");
		String dnm  = new String(romcell+"_decodernmosmux");
		String invt = new String(romcell+"_invertertop");
		String invb = new String(romcell+"_inverterbot");
		String romname =  new String(romcell+"_rom");
		String rp =   new String(romcell+"_romplane");
		String ip =   new String(romcell+"_inverterplane");
		String mp =   new String(romcell+"_muxplane");

		if (folds > 1)
		{
			romarray = romfold(romarray);
		}
		romplane(destLib, lambda, romarray, rp);

		int bits =
			(new Double(Math.ceil(Math.log(globalbits)/Math.log((double)2.0)))).intValue();
		int words = (int) (Math.pow(2.0, (double) bits));
		int foldbits =
			(new Double(Math.ceil(Math.log(folds)/Math.log((double) 2.0)))).intValue();

		boolean top = true;
		boolean bot = false;

		// make subcells
		decoderpmos(destLib, lambda, bits, dpr, top);
		decodernmos(destLib, lambda, bits, dnr, top);
		inverterplane(destLib, lambda, romarray.length, folds, ip);
		ininverterplane(destLib, lambda, bits, invt, top, bits);

		ArcProto m1arc = tech.findArcProto("Metal-1");
		ArcProto m2arc = tech.findArcProto("Metal-2");

		////////////// decoderpmos	
		Cell decp = (Cell)destLib.findNodeProto(dpr+"{lay}");
		Rectangle2D decpBounds = decp.getBounds();
		PortProto[] decpin = new PortProto[words];
		PortProto[] decpout = new PortProto[words];
		PortProto[] decpbit = new PortProto[2*bits];
		PortProto decpvdd = decp.findPortProto("vdd");
		PortProto decpvddb = decp.findPortProto("vddb");
		for (int i=0; i<words; i++)
		{
			decpin[i] = decp.findPortProto("wordin"+i);
			decpout[i] = decp.findPortProto("word"+i);
		}
		for (int i=0; i<bits; i++)
		{
			decpbit[2*i] = decp.findPortProto("top_in"+i);
			decpbit[(2*i)+1] = decp.findPortProto("top_in"+i+"_b");
		}

		////////////// decodernmos	
		Cell decn = (Cell)destLib.findNodeProto(dnr+"{lay}");
		Rectangle2D decnBounds = decn.getBounds();
	 	PortProto[] decnout = new PortProto[words];
	 	PortProto[] decnin = new PortProto[words];
	 	PortProto[] decnbit = new PortProto[2*bits];
		
		for (int i=0; i<words; i++)
		{
			decnin[i] = decn.findPortProto("mid"+i);
			decnout[i] = decn.findPortProto("word"+i);
		}
		for (int i=0; i<bits; i++)
		{
			decnbit[2*i] = decn.findPortProto("top_in"+i);
			decnbit[(2*i)+1] = decn.findPortProto("top_in"+i+"_b");
		}
	
		////////////////////// romplane
		Cell romp = (Cell)destLib.findNodeProto(rp+"{lay}");
		Rectangle2D rompBounds = romp.getBounds();
		PortProto[] rompin = new PortProto[globalbits];
		PortProto[] rompout = new PortProto[romarray.length];
		PortProto[] rompgnd = new PortProto[romarray.length/2];
		PortProto rompvdd = romp.findPortProto("vdd");
		PortProto rompgndc = romp.findPortProto("gndc");
		for (int i=0; i<globalbits; i++)
		{
			rompin[i] = romp.findPortProto("wordline_"+i);
		}
		for (int i=0; i<romarray.length; i++)
		{
			rompout[i] = romp.findPortProto("out_"+i);
		}
		for (int i=0; i<romarray.length/2; i++)
		{
			rompgnd[i] = romp.findPortProto("romgnd"+i);
		}
	
		////////////////////// inverterplane
		Cell invp = (Cell)destLib.findNodeProto(ip+"{lay}");
		Rectangle2D invpBounds = invp.getBounds();
		PortProto[] invin = new PortProto[romarray.length];
		PortProto[] invout = new PortProto[romarray.length];
		PortProto[] invgnd = new PortProto[romarray.length/2];
		PortProto invvddc = invp.findPortProto("vdd");
		PortProto invgndc = invp.findPortProto("gnd");
		for (int i=0; i<romarray.length/folds; i++)
		{
			invin[i] = invp.findPortProto("invin"+i);
			invout[i] = invp.findPortProto("invout"+i);
		}
		
		int invplanegnd = romarray.length/folds;
		if (folds == 1)
		{
			invplanegnd = invplanegnd / 2;
		}
	
		for (int i=0; i<invplanegnd; i++)
		{
			invgnd[i] = invp.findPortProto("invgnd"+i);
		}
	
		////////////////////// ininverterplane top
		Cell ininvtp = (Cell)destLib.findNodeProto(invt+"{lay}");
		Rectangle2D ininvtpBounds = ininvtp.getBounds();
		PortProto[] ivttop  = new PortProto[bits];
		PortProto[] ivtbot = new PortProto[bits];
		PortProto[] ivtbar = new PortProto[bits];
		PortProto ivtvdd = ininvtp.findPortProto("vdd");
		PortProto ivtgnd = ininvtp.findPortProto("gnd");
		for (int i=0; i<bits; i++)
		{
			ivttop[i] = ininvtp.findPortProto("in_top"+i);
			ivtbot[i] = ininvtp.findPortProto("in_bot"+i);
			ivtbar[i] = ininvtp.findPortProto("in_b"+i);
		}
	
		// create new layout named "rom{lay}" in destination library
		Cell rom = Cell.newInstance(destLib, romname+"{lay}");
	
		////////// calculate pplane offset
		double offset = (2*bits*(8*lambda)) + (16*lambda);
		double rompoffset = (8*lambda)*2*bits + (12*lambda) + offset;
		double rompoffsety = 8*lambda*(globalbits+1);
		double foldoffsetx = (2*(bits-foldbits)*(8*lambda));
		double muxpoffsety = -6*lambda;
		double foldoffsety = -8*lambda*(folds+1);
		double ininvtoffset = (globalbits+2)*8*lambda+48*lambda;

		// smr added this line to make things line-up properly
		ininvtoffset += 44*lambda;
	
		double invpoffsety = -8*lambda*(folds+1)-16*lambda;
		if (folds == 1)
		{
			invpoffsety = invpoffsety + 24*lambda;
		}
	
		NodeInst nplane =
			makeCStyleNodeInst(decn, decnBounds.getMinX()+offset, decnBounds.getMaxX()+offset, decnBounds.getMinY(),
								 decnBounds.getMaxY(), 0, 0, rom);
		NodeInst pplane =
			makeCStyleNodeInst(decp, decpBounds.getMinX(), decpBounds.getMaxX(), decpBounds.getMinY(), decpBounds.getMaxY(),
								 0, 0, rom);
		NodeInst rompln =
			makeCStyleNodeInst(romp, rompBounds.getMinX()+rompoffset, rompBounds.getMaxX()+rompoffset,
								 rompBounds.getMinY()+rompoffsety, rompBounds.getMaxY()+rompoffsety,
								 0, 2700, rom);
		NodeInst invpln =
			makeCStyleNodeInst(invp, invpBounds.getMinX()+rompoffset, invpBounds.getMaxX()+rompoffset,
								 invpBounds.getMinY()+invpoffsety, invpBounds.getMaxY()+invpoffsety, 0, 0, rom);
		NodeInst ininvtop1 =
			makeCStyleNodeInst(ininvtp,ininvtpBounds.getMinX(),ininvtpBounds.getMaxX(),
								 ininvtpBounds.getMinY()+ininvtoffset, ininvtpBounds.getMaxY()+ininvtoffset,
								 0, 0, rom);
		NodeInst ininvtop2 =
			makeCStyleNodeInst(ininvtp,ininvtpBounds.getMinX()+offset, ininvtpBounds.getMaxX()+offset,
								 ininvtpBounds.getMinY()+ininvtoffset,
								 ininvtpBounds.getMaxY()+ininvtoffset, 0, 0, rom);

		////////////// exports on top level
		for (int i=0; i<bits; i++)
		{
			ap1 = ininvtop1;
			apport1 = ivttop[i];
			makeCStyleExport(rom, ap1, apport1, ("sel"+i), PortCharacteristic.IN);
		}
		for (int i=0; i<romarray.length/folds; i++)
		{
			ap1 = invpln;
			apport1 = invout[i];
			makeCStyleExport(rom, ap1, apport1, ("out"+i), PortCharacteristic.OUT);
		}

		ap2 = rompln;
		apport2 = rompvdd;
		makeCStyleExport(rom, ap2, apport2, ("vdd"), PortCharacteristic.PWR);

		// TODO: this arc comes out diagonal!!!
		ap1 = nplane;
		apport1 = decn.findPortProto("gnd");
		appos1 = getCStylePortPosition(ap1, apport1);
		ap2 = rompln;
		apport2 = rompgndc;
		appos2 = getCStylePortPosition(ap2, apport2);
		makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
							appos1[1], ap2, apport2, appos2[0], appos2[1]);

		apport2 = romp.findPortProto("gnd");

		// decnout, decpin
		for (int i=0; i<words; i++)
		{
			ap1 = pplane;
			apport1 = decpout[i];
			appos1 = getCStylePortPosition(ap1, apport1); 
			ap2 = nplane;
			apport2 = decnin[i];
			appos2 = getCStylePortPosition(ap2, apport2); 
			makeCStyleArcInst(m2arc, 4*lambda, ap1, apport1, appos1[0],
								appos1[1], ap2, apport2, appos2[0], appos2[1]);
		}

		for (int i=0; i<words; i++)
		{
			if (i >= globalbits) continue;

			ap1 = nplane;
			apport1 = decnout[i];
			appos1 = getCStylePortPosition(ap1, apport1); 
			ap2 = rompln;
			apport2 = rompin[i];
			appos2 = getCStylePortPosition(ap2, apport2); 
			makeCStyleArcInst(m2arc, 4*lambda, ap1, apport1, appos1[0],
								appos1[1], ap2, apport2, appos2[0], appos2[1]);
		}
	
		/////// connect rompgnd to invgnd
		if (folds > 1)
		{
			for (int i=0; i<romarray.length/folds; i++)
			{
				ap1 = invpln;
				apport1 = invgnd[i];
				appos1 = getCStylePortPosition(ap1, apport1); 
				ap2 = rompln;
				apport2 = rompgnd[i*folds/2];
				appos2 = getCStylePortPosition(ap2, apport2); 
				makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
									appos1[1], ap2,apport2,appos2[0], appos2[1]);
			}
		} else
		{
			for (int i=0; i<romarray.length/(2*folds); i++)
			{
				ap1 = invpln;
				apport1 = invgnd[i];
				appos1 = getCStylePortPosition(ap1, apport1); 
				ap2 = rompln;
				apport2 = rompgnd[i];
				appos2 = getCStylePortPosition(ap2, apport2); 
				makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
									appos1[1], ap2,apport2,appos2[0], appos2[1]);
			}
		}
	
		/////// connect top ininv1 to ininv2
		for (int i=0; i<bits; i++)
		{
			ap1 = ininvtop1;
			apport1 = ivttop[i];
			appos1 = getCStylePortPosition(ap1, apport1); 
			ap2 = ininvtop2;
			apport2 = ivttop[i];
			appos2 = getCStylePortPosition(ap2, apport2); 
			makeCStyleArcInst(m2arc, 4*lambda, ap1, apport1, appos1[0],
								appos1[1], ap2, apport2, appos2[0], appos2[1]);
		}
	
		/////// connect top ininv1 to ndecoder
		for (int i=0; i<bits; i++)
		{
			ap1 = ininvtop1;
			apport1 = ivtbot[i];
			appos1 = getCStylePortPosition(ap1, apport1); 
			ap2 = pplane;
			apport2 = decpbit[i*2];
			appos2 = getCStylePortPosition(ap2, apport2); 
			makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
								appos1[1], ap2, apport2, appos2[0], appos2[1]);
			ap1 = ininvtop1;
			apport1 = ivtbar[i];
			appos1 = getCStylePortPosition(ap1, apport1); 
			ap2 = pplane;
			apport2 = decpbit[(i*2)+1];
			appos2 = getCStylePortPosition(ap2, apport2); 
			makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
								appos1[1], ap2, apport2, appos2[0], appos2[1]);
		}
	
		/////// connect top ininv2 to pdecoder
		for (int i=0; i<bits; i++)
		{
			ap1 = ininvtop2;
			apport1 = ivtbot[i];
			appos1 = getCStylePortPosition(ap1, apport1); 
			ap2 = nplane;
			apport2 = decnbit[i*2];
			appos2 = getCStylePortPosition(ap2, apport2); 
			makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
								appos1[1], ap2, apport2, appos2[0], appos2[1]);
			ap1 = ininvtop2;
			apport1 = ivtbar[i];
			appos1 = getCStylePortPosition(ap1, apport1); 
			ap2 = nplane;
			apport2 = decnbit[(i*2)+1];
			appos2 = getCStylePortPosition(ap2, apport2); 
			makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
								appos1[1], ap2, apport2, appos2[0], appos2[1]);
		}
	
		//////// connect two top decoder inverterplanes and decoder together (vdd)
		ap1 = ininvtop1;
		apport1 = ivtvdd;
		appos1 = getCStylePortPosition(ap1, apport1); 
		ap2 = ininvtop2;
		apport2 = ivtvdd;
		appos2 = getCStylePortPosition(ap2, apport2);
		ap3 = pplane;
		apport3 = decpvdd;
		appos3 = getCStylePortPosition(ap3, apport3);
		makeCStyleArcInst(m2arc, 4*lambda, ap1, apport1, appos1[0],
							appos1[1], ap2, apport2, appos2[0], appos2[1]);
		makeCStyleArcInst(m2arc, 4*lambda, ap1, apport1, appos1[0],
							appos1[1], ap3, apport3, appos3[0], appos3[1]);
		
		//////// connect two top decoder inverterplanes and romplane together (gnd)
		ap1 = ininvtop1;
		apport1 = ivtgnd;
		appos1 = getCStylePortPosition(ap1, apport1); 
		ap2 = ininvtop2;
		apport2 = ivtgnd;
		appos2 = getCStylePortPosition(ap2, apport2);
		ap3 = rompln;
		apport3 = rompgndc;
		appos3 = getCStylePortPosition(ap3, apport3);
	
		makeCStyleArcInst(m2arc, 4*lambda, ap1, apport1, appos1[0],
							appos1[1], ap2, apport2, appos2[0], appos2[1]);
		makeCStyleArcInst(m1arc, 4*lambda, ap2, apport2, appos2[0],
							appos2[1], ap3, apport3, appos3[0], appos3[1]);
		makeCStyleExport(rom, ap2, apport2, "gnd", PortCharacteristic.GND);

		//////// connect decoder inverter vdd to rom vdd
		ap1 = ininvtop2;
		apport1 = ivtvdd;
		appos1 = getCStylePortPosition(ap1, apport1); 
		ap2 = rompln;
		apport2 = rompvdd;
		appos2 = getCStylePortPosition(ap2, apport2); 
	
		makeCStyleArcInst(m2arc, 4*lambda, ap1, apport1, appos1[0],
							appos1[1], ap2, apport2, appos2[0], appos2[1]);

		// begin (folds > 1)
		if (folds > 1)
		{
			decoderpmos(destLib, lambda, foldbits, dpm, bot);
			decodernmos(destLib, lambda, foldbits, dnm, bot);
			ininverterplane(destLib, lambda, foldbits, invb, bot, bits);
			muxplane(destLib, lambda, folds, romarray.length, mp);
	
			////////////// decodernmosmux
			Cell decpmux = (Cell)destLib.findNodeProto(dpm+"{lay}");
			Rectangle2D decpmuxBounds = decpmux.getBounds();
		 	PortProto[] decpmuxin = new PortProto[folds];
		 	PortProto[] decpmuxout = new PortProto[folds];
		 	PortProto[] decpmuxbit = new PortProto[2*foldbits];
		 	PortProto decpmuxvdd = decpmux.findPortProto("vdd");
			PortProto decpmuxvddb = decpmux.findPortProto("vddb");
	
			for (int i=0; i<folds; i++)
			{
				decpmuxin[i] = decpmux.findPortProto("wordin"+i);
				decpmuxout[i] = decpmux.findPortProto("word"+i);
			}
			for (int i=0; i<foldbits; i++)
			{
				decpmuxbit[2*i] = decpmux.findPortProto("bot_in"+i);
				decpmuxbit[(2*i)+1] = decpmux.findPortProto("bot_in"+i+"_b");
			}
	
			////////////// decoderpmosmux
			Cell decnmux = (Cell)destLib.findNodeProto(dnm+"{lay}");
			Rectangle2D decnmuxBounds = decnmux.getBounds();
		 	PortProto[] decnmuxout = new PortProto[folds];
		 	PortProto[] decnmuxin = new PortProto[folds];
		 	PortProto[] decnmuxbit = new PortProto[2*foldbits];
			for (int i=0; i<folds; i++)
			{
				decnmuxin[i] = decnmux.findPortProto("mid"+i);
				decnmuxout[i] = decnmux.findPortProto("word"+i);
			}
			for (int i=0; i<foldbits; i++)
			{
				decnmuxbit[2*i] = decnmux.findPortProto("bot_in"+i);
				decnmuxbit[(2*i)+1] = decnmux.findPortProto("bot_in"+i+"_b");
			}
			
			////////////////////// muxplane
			Cell muxp = (Cell)destLib.findNodeProto(mp+"{lay}");
			Rectangle2D muxpBounds = muxp.getBounds();
			PortProto[] muxin = new PortProto[romarray.length];
			PortProto[] muxout = new PortProto[romarray.length/folds];
			PortProto[] muxsel = new PortProto[folds];
			for (int i=0; i<romarray.length; i++)
			{
				muxin[i] = muxp.findPortProto("muxin"+i);
			}
			for (int i=0; i<romarray.length/folds; i++)
			{
				muxout[i] = muxp.findPortProto("muxout"+i);
			}
			for (int i=0; i<folds; i++)
			{
				muxsel[i] = muxp.findPortProto("sel"+i);
			}
	
			////////////////////// ininverterplane bottom
			Cell ininvbp = (Cell)destLib.findNodeProto(invb+"{lay}");
			Rectangle2D ininvbpBounds = ininvbp.getBounds();
			PortProto[] ivbtop  = new PortProto[foldbits];
			PortProto[] ivbbot = new PortProto[foldbits];
			PortProto[] ivbbar = new PortProto[foldbits];
			PortProto ivbvdd = ininvbp.findPortProto("vdd");
			PortProto ivbgnd = ininvbp.findPortProto("gnd");
			for (int i=0; i<foldbits; i++)
			{
				ivbtop[i] = ininvbp.findPortProto("in_top"+i);
				ivbbot[i] = ininvbp.findPortProto("in_bot"+i);
				ivbbar[i] = ininvbp.findPortProto("in_b"+i);
			}
	
			NodeInst muxpln =
				makeCStyleNodeInst(muxp, muxpBounds.getMinX()+rompoffset, muxpBounds.getMaxX()+rompoffset,
									 muxpBounds.getMinY()+muxpoffsety, muxpBounds.getMaxY()+muxpoffsety,
									 0, 2700, rom);
			NodeInst pplnmx =
				makeCStyleNodeInst(decpmux, decpmuxBounds.getMinX()+foldoffsetx,
											decpmuxBounds.getMaxX()+foldoffsetx,
											decpmuxBounds.getMinY()+muxpoffsety+foldoffsety,
											decpmuxBounds.getMaxY()+muxpoffsety+foldoffsety, 0, 0, rom);

			NodeInst nplnmx =
				makeCStyleNodeInst(decnmux, decnmuxBounds.getMinX()+foldoffsetx+offset,
									 decnmuxBounds.getMaxX()+foldoffsetx+offset,
									 decnmuxBounds.getMinY()+muxpoffsety+foldoffsety,
									 decnmuxBounds.getMaxY()+muxpoffsety+foldoffsety, 0, 0, rom);
			NodeInst ininvbot1 =
				makeCStyleNodeInst(ininvbp,ininvbpBounds.getMinX()+foldoffsetx,
									 ininvbpBounds.getMaxX()+foldoffsetx, ininvbpBounds.getMinY()+invpoffsety,
									 ininvbpBounds.getMaxY()+invpoffsety, 0, 0, rom);
			NodeInst ininvbot2 =
				makeCStyleNodeInst(ininvbp, ininvbpBounds.getMinX()+foldoffsetx+offset,
									 ininvbpBounds.getMaxX()+foldoffsetx+offset,
									 ininvbpBounds.getMinY()+invpoffsety,
									 ininvbpBounds.getMaxY()+invpoffsety, 0, 0, rom);
	
			for (int i=0; i<foldbits; i++)
			{
				ap1 = ininvbot1;
				apport1 = ivbbot[i];
				makeCStyleExport(rom, ap1, apport1, "colsel"+i, PortCharacteristic.IN);
			}
	
			ap1 = nplane;
			apport1 = decn.findPortProto("gnd");
			appos1 = getCStylePortPosition(ap1, apport1);
			ap3 = pplnmx;
			apport3 = decpmuxvdd;
			appos3 = getCStylePortPosition(ap3, apport3);
			ap4 = pplane;
			apport4 = decpvddb;
			appos4 = getCStylePortPosition(ap4, apport4);
			makeCStyleArcInst(m1arc, 4*lambda, ap4, apport4, appos4[0],
								appos4[1], ap3, apport3, appos3[0], appos3[1]);
	
			ap3 = nplnmx;
			apport3 = decnmux.findPortProto("gnd");
			appos3 = getCStylePortPosition(ap3, apport3);
	
			makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
								appos1[1], ap3, apport3, appos3[0], appos3[1]);
	
			// decnmuxout, decpmuxin
			for (int i=0; i<folds; i++)
			{
				ap1 = pplnmx;
				apport1 = decpmuxout[i];
				appos1 = getCStylePortPosition(ap1, apport1); 
				ap2 = nplnmx;
				apport2 = decnmuxin[i];
				appos2 = getCStylePortPosition(ap2, apport2); 
				makeCStyleArcInst(m2arc, 4*lambda, ap1, apport1,appos1[0],
									appos1[1],ap2,apport2,appos2[0], appos2[1]);
			}
	
			for (int i=0; i<folds; i++)
			{
				ap1 = nplnmx;
				apport1 = decnmuxout[i];
				appos1 = getCStylePortPosition(ap1, apport1); 
				ap2 = muxpln;
				apport2 = muxsel[i];
				appos2 = getCStylePortPosition(ap2, apport2); 
				makeCStyleArcInst(m2arc, 4*lambda, ap1, apport1,appos1[0],
									appos1[1],ap2,apport2,appos2[0], appos2[1]);
			}
			
			///////connect rompout to muxin
			for (int i=0; i<romarray.length; i++)
			{
				ap1 = rompln;
				apport1 = rompout[i];
				appos1 = getCStylePortPosition(ap1, apport1); 
				ap2 = muxpln;
				apport2 = muxin[i];
				appos2 = getCStylePortPosition(ap2, apport2); 
				makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1,appos1[0],
									appos1[1],ap2,apport2,appos2[0], appos2[1]);
			}

			/////// connect muxout to invin
			for (int i=0; i<romarray.length/folds; i++)
			{
				ap1 = invpln;
				apport1 = invin[i];
				appos1 = getCStylePortPosition(ap1, apport1); 
				ap2 = muxpln;
				apport2 = muxout[i];
				appos2 = getCStylePortPosition(ap2, apport2); 
				makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1,appos1[0],
									appos1[1],ap2,apport2,appos2[0], appos2[1]);
			}
			
			/////// connect bot ininv1 to ininv2
			for (int i=0; i<foldbits; i++)
			{
				ap1 = ininvbot1;
				apport1 = ivbbot[i];
				appos1 = getCStylePortPosition(ap1, apport1); 
				ap2 = ininvbot2;
				apport2 = ivbbot[i];
				appos2 = getCStylePortPosition(ap2, apport2); 
				makeCStyleArcInst(m2arc, 4*lambda, ap1, apport1,appos1[0],
									appos1[1],ap2,apport2,appos2[0], appos2[1]);
			}
		
			/////// connect bot ininv1 to nmuxdecoder
			for (int i=0; i<foldbits; i++)
			{
				ap1 = ininvbot1;
				apport1 = ivbtop[i];
				appos1 = getCStylePortPosition(ap1, apport1); 
				ap2 = pplnmx;
				apport2 = decpmuxbit[i*2];
				appos2 = getCStylePortPosition(ap2, apport2); 
				makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
									appos1[1], ap2, apport2, appos2[0], appos2[1]);
				ap1 = ininvbot1;
				apport1 = ivbbar[i];
				appos1 = getCStylePortPosition(ap1, apport1); 
				ap2 = pplnmx;
				apport2 = decpmuxbit[(i*2)+1];
				appos2 = getCStylePortPosition(ap2, apport2); 
				makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
									appos1[1], ap2, apport2, appos2[0], appos2[1]);
			}

			/////// connect bot ininv2 to pmuxdecoder
			for (int i=0; i<foldbits; i++)
			{
				ap1 = ininvbot2;
				apport1 = ivbtop[i];
				appos1 = getCStylePortPosition(ap1, apport1); 
				ap2 = nplnmx;
				apport2 = decnmuxbit[i*2];
				appos2 = getCStylePortPosition(ap2, apport2); 
				makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
									appos1[1], ap2, apport2, appos2[0], appos2[1]);
				ap1 = ininvbot2;
				apport1 = ivbbar[i];
				appos1 = getCStylePortPosition(ap1, apport1); 
				ap2 = nplnmx;
				apport2 = decnmuxbit[(i*2)+1];
				appos2 = getCStylePortPosition(ap2, apport2); 
				makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
									appos1[1], ap2, apport2, appos2[0], appos2[1]);
			}
	
			//////// connect two mux decoder inverterplanes and mux decoder together (vdd)
			ap1 = ininvbot1;
			apport1 = ivbvdd;
			appos1 = getCStylePortPosition(ap1, apport1); 
			ap2 = ininvbot2;
			apport2 = ivbvdd;
			appos2 = getCStylePortPosition(ap2, apport2); 
			ap3 = pplnmx;
			apport3 = decpmuxvddb;
			appos3 = getCStylePortPosition(ap3, apport3);
			
			makeCStyleArcInst(m2arc, 4*lambda, ap1, apport1, appos1[0],
								appos1[1], ap2, apport2, appos2[0], appos2[1]);
	
			makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
								appos1[1], ap3, apport3, appos3[0], appos3[1]);
	
			//////// connect two mux decoder inverterplanes and inverterplane together (gnd)
			ap1 = ininvbot1;
			apport1 = ivbgnd;
			appos1 = getCStylePortPosition(ap1, apport1); 
			ap2 = ininvbot2;
			apport2 = ivbgnd;
			appos2 = getCStylePortPosition(ap2, apport2);
			ap3 = invpln;
			apport3 = invgndc;
			appos3 = getCStylePortPosition(ap3, apport3);
			makeCStyleArcInst(m2arc, 4*lambda, ap1, apport1, appos1[0],
								appos1[1], ap2, apport2, appos2[0], appos2[1]);
			makeCStyleArcInst(m2arc, 4*lambda, ap3, apport3, appos3[0],
								appos3[1], ap2, apport2, appos2[0], appos2[1]);
	
			//////// connect mux decoder to inverter vdd
			ap1 = invpln;
			apport1 = invvddc;
			appos1 = getCStylePortPosition(ap1, apport1); 
			ap2 = ininvbot2;
			apport2 = ivbvdd;
			appos2 = getCStylePortPosition(ap2, apport2); 
			makeCStyleArcInst(m2arc, 4*lambda, ap1, apport1, appos1[0],
								appos1[1], ap2, apport2, appos2[0], appos2[1]);
		}
		// end (folds > 1)
	
		// begin (folds == 1)
		if (folds == 1)
		{
			for (int i=0; i<romarray.length; i++)
			{
				ap1 = invpln;
				apport1 = invin[i];
				appos1 = getCStylePortPosition(ap1, apport1); 
				ap2 = rompln;
				apport2 = rompout[i];
				appos2 = getCStylePortPosition(ap2, apport2); 
				makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
									appos1[1], ap2, apport2, appos2[0], appos2[1]);
			}
		
			// connect vdd of decoderpmos to vdd of inverterplane
			NodeProto m1m2c = tech.findNodeProto("Metal-1-Metal-2-Con");
			PortProto m1m2cport = m1m2c.getPort(0);
			double[] m1m2cbox = {-5*lambda/2, 5*lambda/2, -5*lambda/2, 5*lambda/2}; 
	
			double vddoffsetx = offset - 4*lambda;
			double vddoffsety = invpoffsety - 26*lambda;
	
			NodeInst vddbot =
				makeCStyleNodeInst(m1m2c, m1m2cbox[0]+vddoffsetx, m1m2cbox[1]+vddoffsetx,
									 m1m2cbox[2]+vddoffsety, m1m2cbox[3]+vddoffsety, 0, 0, rom);
	
			ap1 = invpln;
			apport1 = invvddc;
			appos1 = getCStylePortPosition(ap1, apport1); 
			ap2 = vddbot;
			apport2 = m1m2cport;
			appos2 = getCStylePortPosition(ap2, apport2); 
			ap3 = pplane;
			apport3 = decpvddb;
			appos3 = getCStylePortPosition(ap3, apport3); 
			makeCStyleArcInst(m2arc, 4*lambda, ap1, apport1, appos1[0],
								appos1[1], ap2, apport2, appos2[0], appos2[1]);
			makeCStyleArcInst(m1arc, 4*lambda, ap2, apport2, appos2[0],
								appos2[1], ap3, apport3, appos3[0], appos3[1]);
		}

		// return the top-level
		return rom;
	}

	/**
	 */
	private static void romplane(Library destLib, double lambda, int romarray[][], String rp)
	{
		int i, m, o;
		double x, y;
		NodeInst ap1, ap2, ap3, ap4, gnd1, gnd2, intgnd;
		PortProto apport1, apport2, apport3, apport4, gndport1, gndport2,
						   intgndport;
		double[] appos1, appos2, appos3, appos4, gndpos1, gndpos2, intgndpos;
	
		int inputs = romarray[0].length;
		int wordlines = romarray.length;
		
		NodeInst[][] andtrans = new NodeInst[wordlines+2][inputs+2];
		NodeInst[] pulluptrans = new NodeInst[wordlines+2];
		NodeInst[] nwellc = new NodeInst[(wordlines+2)/2];
		NodeInst[][] minpins = new NodeInst[wordlines+2][inputs+2];
		NodeInst[][] diffpins = new NodeInst[wordlines+2][inputs+2];
		NodeInst[][] gndpins = new NodeInst[wordlines/2][inputs+2];
		NodeInst[] gnd_2pins = new NodeInst[wordlines/2];
		NodeInst[] m1polypins = new NodeInst[inputs+2];
		NodeInst[] m1m2pins = new NodeInst[inputs+2];
		NodeInst[] m1m2_2pins = new NodeInst[wordlines+2];
		NodeInst[] m1m2_3pins = new NodeInst[wordlines+2];
		NodeInst[] m1m2_4pins = new NodeInst[wordlines+2];
		NodeInst[] mpac_1pins = new NodeInst[wordlines+2];
		NodeInst[] mpac_2pins = new NodeInst[wordlines+2];
		NodeInst gndpex[] = new NodeInst[1];
		NodeInst gndm1ex[] = new NodeInst[1];
		NodeInst gnd1pin = null;
		NodeInst vdd1pin = null;
		NodeInst vdd2pin = null;
	
		PortProto[] nwellcports = new PortProto[(wordlines+2)/2];
		PortProto[][] minports = new PortProto[wordlines+2][inputs+2];
		PortProto[][] gndports = new PortProto[wordlines/2][inputs+2];
		PortProto[] gnd_2ports = new PortProto[wordlines/2];
		PortProto[][] diffports = new PortProto[wordlines/2][inputs+2];
		PortProto[] m1polyports = new PortProto[inputs+2];
		PortProto[] m1m2ports = new PortProto[inputs+2];
		PortProto[] m1m2_2ports = new PortProto[wordlines+2];
		PortProto[] m1m2_3ports = new PortProto[wordlines+2];
		PortProto[] m1m2_4ports = new PortProto[wordlines+2];
		PortProto[] mpac_1ports = new PortProto[wordlines+2];
		PortProto[] mpac_2ports = new PortProto[wordlines+2];
		PortProto gndpexport[] = new PortProto[1];
		PortProto gndm1export[] = new PortProto[1];
		PortProto gnd1port = null;
		PortProto vdd1port = null;
		PortProto vdd2port = null;
	
		// get pointers to primitives
		NodeProto nmos = tech.findNodeProto("N-Transistor");
		PortProto nmosg1port = nmos.findPortProto("n-trans-poly-right");
		PortProto nmosg2port = nmos.findPortProto("n-trans-poly-left");
		PortProto nmosd1port = nmos.findPortProto("n-trans-diff-top");
		PortProto nmosd2port = nmos.findPortProto("n-trans-diff-bottom");
		double[] nmosbox = {-nmos.getDefWidth()/2-lambda/2,
							nmos.getDefWidth()/2+lambda/2,
							-nmos.getDefHeight()/2,
							nmos.getDefHeight()/2};
					
		NodeProto pmos = tech.findNodeProto("P-Transistor");
		PortProto pmosg1port = pmos.findPortProto("p-trans-poly-right");
		PortProto pmosg2port = pmos.findPortProto("p-trans-poly-left");
		PortProto pmosd1port = pmos.findPortProto("p-trans-diff-top");
		PortProto pmosd2port = pmos.findPortProto("p-trans-diff-bottom");
		double bbb = 15; 
		double ccc = 23;
		double[] pmosbox = {-bbb*lambda/2, bbb*lambda/2, -ccc*lambda/2, ccc*lambda/2};
	
		NodeProto ppin = tech.findNodeProto("Polysilicon-1-Pin");
		PortProto ppinport = ppin.getPort(0);
		double[] ppinbox = {-ppin.getDefWidth()/2,
						 ppin.getDefWidth()/2,
						 -ppin.getDefHeight()/2,
						 ppin.getDefHeight()/2};
		
		NodeProto napin = tech.findNodeProto("N-Active-Pin");
		
		NodeProto m1pin = tech.findNodeProto("Metal-1-Pin");
		PortProto m1pinport = m1pin.getPort(0);
		double[] m1pinbox = {-m1pin.getDefWidth()/2-lambda/2,
						  m1pin.getDefWidth()/2+lambda/2,
						  -m1pin.getDefHeight()/2-lambda/2,
						  m1pin.getDefHeight()/2+lambda/2};
		
		NodeProto m2pin = tech.findNodeProto("Metal-2-Pin");
		PortProto m2pinport = m2pin.getPort(0);
		double[] m2pinbox = {-m2pin.getDefWidth()/2-lambda/2,
						  m2pin.getDefWidth()/2+lambda/2,
						  -m2pin.getDefHeight()/2-lambda/2,
						  m2pin.getDefHeight()/2+lambda/2};
		
		NodeProto diffpin = tech.findNodeProto("Active-Pin");
		PortProto diffpinport = diffpin.getPort(0);
		double[] diffpinbox =
			{-diffpin.getDefWidth()/2-lambda/2,
			 diffpin.getDefWidth()/2+lambda/2,
			 -diffpin.getDefHeight()/2-lambda/2,
			 diffpin.getDefHeight()/2+lambda/2};
					
		NodeProto nwnode = tech.findNodeProto("N-Well-Node");
		double[] nwnodebox =
			{-nwnode.getDefWidth()/2-lambda/2,
			 nwnode.getDefWidth()/2+lambda/2,
			 -nwnode.getDefHeight()/2-lambda/2,
			 nwnode.getDefHeight()/2+lambda/2};
		
		NodeProto pwnode = tech.findNodeProto("P-Well-Node");
		double[] pwnodebox =
			{-pwnode.getDefWidth()/2-lambda/2,
			 pwnode.getDefWidth()/2+lambda/2,
			 -pwnode.getDefHeight()/2-lambda/2,
			 pwnode.getDefHeight()/2+lambda/2};
					
		NodeProto psnode = tech.findNodeProto("P-Select-Node");
		double[] psnodebox =
			{-psnode.getDefWidth()/2-lambda/2,
			 psnode.getDefWidth()/2+lambda/2,
			 -psnode.getDefHeight()/2-lambda/2,
			 psnode.getDefHeight()/2+lambda/2};	   
		
		NodeProto mnac = tech.findNodeProto("Metal-1-N-Active-Con");
		PortProto mnacport = mnac.getPort(0);
		double aaa = 17;
		double[] mnacbox = {-aaa*lambda/2, aaa*lambda/2, -aaa*lambda/2, aaa*lambda/2};
		
		NodeProto mpac = tech.findNodeProto("Metal-1-P-Active-Con");
		PortProto mpacport = mpac.getPort(0);
		double[] mpacbox = {-aaa*lambda/2, aaa*lambda/2, -aaa*lambda/2, aaa*lambda/2};
	
		NodeProto mpwc = tech.findNodeProto("Metal-1-P-Well-Con");
		PortProto mpwcport = mpwc.getPort(0);
		double[] mpwcbox = {-17*lambda/2, 17*lambda/2, -17*lambda/2, 17*lambda/2};
		
		NodeProto mnwc = tech.findNodeProto("Metal-1-N-Well-Con");
		PortProto mnwcport = mnwc.getPort(0);
		double nwellx = 29;
		double nwelly = 17;
		double[] mnwcbox ={-nwellx*lambda/2,nwellx*lambda/2,-nwelly*lambda/2,nwelly*lambda/2};
	
		NodeProto mpc = tech.findNodeProto("Metal-1-Polysilicon-1-Con");
		PortProto mpcport = mpc.getPort(0);
		double mx = 5;
		double[] mpcbox = {-mx*lambda/2, mx*lambda/2, -mx*lambda/2, mx*lambda/2}; 
	
		NodeProto m1m2c = tech.findNodeProto("Metal-1-Metal-2-Con");
		PortProto m1m2cport = m1m2c.getPort(0);
		double[] m1m2cbox = {-mx*lambda/2, mx*lambda/2, -mx*lambda/2, mx*lambda/2}; 
		
		NodeProto nsnode = tech.findNodeProto("N-Select-Node");
		double nselectx = 8; 
		double nselecty = 8;
		double[] nsnodebox =
			{-nselectx*lambda/2, nselectx*lambda/2, -nselecty*lambda/2, nselecty*lambda/2};
	
		ArcProto parc = tech.findArcProto("Polysilicon-1");
		ArcProto m1arc = tech.findArcProto("Metal-1");
		ArcProto m2arc = tech.findArcProto("Metal-2");
		ArcProto ndiffarc = tech.findArcProto("N-Active");
		ArcProto pdiffarc = tech.findArcProto("P-Active");
	
		// create a cell called "romplane{lay}" in the destination library
		Cell romplane = Cell.newInstance(destLib, rp+"{lay}");
	
		NodeInst pwellnode = makeCStyleNodeInst(pwnode,-4*lambda,(8*lambda*(inputs+2)),
										-4*lambda,3*8*lambda*(wordlines)/2,0,0,romplane);
		
		double ptranssize = 20;	
		
		NodeInst pselectnode = makeCStyleNodeInst(psnode,-28*lambda,(ptranssize-28)*lambda,4*lambda,
								 (4+3*8*wordlines/2)*lambda,0,0,romplane);
		NodeInst nselectnode = makeCStyleNodeInst(nsnode,0*lambda,(8*lambda*inputs),4*lambda,
								 (4+3*8*wordlines/2)*lambda,0,0,romplane);
		NodeInst nwellnode = makeCStyleNodeInst(nwnode,-38*lambda,(ptranssize-38)*lambda,20*lambda,
								 (4+3*8*wordlines/2)*lambda,0,0,romplane);
	
		// Create instances of objects on rom plane
		x = 0;
		for (i=0; i<inputs+1; i++)
		{
			x += 8*lambda;
			y = 0;
			if (i < inputs)
			{
				andtrans[0][i] =
					makeCStyleNodeInst(ppin, ppinbox[0]+x, ppinbox[1]+x, ppinbox[2],
										 ppinbox[3], 0, 0, romplane);
				m1polypins[i] =
					makeCStyleNodeInst(mpc, mpcbox[0]+x, mpcbox[1]+x, mpcbox[2],
										 mpcbox[3], 0, 0, romplane);
				m1m2pins[i] =
					makeCStyleNodeInst(m1m2c, m1m2cbox[0]+x, m1m2cbox[1]+x,
										 m1m2cbox[2], m1m2cbox[3], 0, 0, romplane);
				ap1 = m1m2pins[i];
				apport1 = m1m2cport;
				makeCStyleExport(romplane, ap1, apport1, "wordline_"+(inputs-i-1), PortCharacteristic.IN);
			}
			for (m=0; m<wordlines; m++)
			{
				y += 8*lambda;
				if (m%2 == 1)
				{
					if (i%2 == 1)
					{
						gndpins[m/2][i] =
							makeCStyleNodeInst(mnac, mnacbox[0]+x-4*lambda,
												 mnacbox[1]+x-4*lambda, mnacbox[2]+y,
												 mnacbox[3]+y, 0, 0, romplane);
						gndports[m/2][i] = mnacport;
					} else
					{
						if (i == inputs)
						{
							gndpins[m/2][i] =
								makeCStyleNodeInst(mpwc, mpwcbox[0]+x-4*lambda,
													 mpwcbox[1]+x-4*lambda, mpwcbox[2]+y,
													 mpwcbox[3]+y, 0, 0, romplane);
							gndports[m/2][i] = mpwcport;
						} else
						{
							gndpins[m/2][i] =
								makeCStyleNodeInst(m1pin, m1pinbox[0]+x-4*lambda,
								m1pinbox[1]+x-4*lambda, m1pinbox[2]+y, m1pinbox[3]+y,
								0, 0, romplane);
							gndports[m/2][i] = m1pinport;
						}
					}
					if (i == 0)
					{
						gnd_2pins[m/2] =
							makeCStyleNodeInst(m1pin, m1pinbox[0]+x-12*lambda,
								m1pinbox[1]+x-12*lambda, m1pinbox[2]+y, m1pinbox[3]+y,
								0, 0, romplane);
						gnd_2ports[m/2] = m1pinport;
						if (m == 1)
						{
							gndm1ex[m/2] =
								makeCStyleNodeInst(m1pin, m1pinbox[0]+x-12*lambda,
													 m1pinbox[1]+x-12*lambda,
													 m1pinbox[2]+y-16*lambda,
													 m1pinbox[3]+y-16*lambda, 0, 0, romplane);
							gndm1export[m/2] = m1pinport;
							gnd1pin =
								makeCStyleNodeInst(m1pin, m1pinbox[0]+x-8*lambda,
													 m1pinbox[1]+x-8*lambda,
													 m1pinbox[2]+y-16*lambda,
													 m1pinbox[3]+y-16*lambda, 0, 0, romplane);
							gnd1port = m1pinport;
							vdd2pin =
								makeCStyleNodeInst(m2pin, m2pinbox[0]+x-32*lambda,
													 m2pinbox[1]+x-32*lambda,
													 m2pinbox[2]+y-16*lambda,
													 m2pinbox[3]+y-16*lambda, 0, 0, romplane);
							vdd2port = m2pinport;
						}
					}
					y+= 8*lambda;
				}
				if (i < inputs)
				{
					if (romarray[m][i] == 1)
					{
						// create a transistor
						andtrans[m+1][i] =
							makeCStyleNodeInst(nmos, nmosbox[0]+x, nmosbox[1]+x,
												 nmosbox[2]+y, nmosbox[3]+y,1,0,romplane);
					} else
					{
						andtrans[m+1][i] =
							makeCStyleNodeInst(ppin, ppinbox[0]+x, ppinbox[1]+x,
												 ppinbox[2]+y,ppinbox[3]+y,0,0,romplane);
					}
				}
				boolean transcont = false;
				if (i < inputs) transcont = (romarray[m][i] == 1);
				if (i > 1) transcont |= (romarray[m][i-1] == 1);
				if (i%2 == 0 && transcont)
				{
					minpins[m][i] =
						makeCStyleNodeInst(mnac, mnacbox[0]+x-4*lambda,
											 mnacbox[1]+x-4*lambda, mnacbox[2]+y,
											 mnacbox[3]+y, 0, 0, romplane);			
					diffpins[m][i] =
						makeCStyleNodeInst(m1pin, m1pinbox[0]+x-4*lambda,
											 m1pinbox[1]+x-4*lambda, m1pinbox[2]+y,
											 m1pinbox[3]+y, 0, 0, romplane);
					minports[m][i] = mnacport;
				} else
				{
					minpins[m][i] =
						makeCStyleNodeInst(m1pin, m1pinbox[0]+x-4*lambda,
											 m1pinbox[1]+x-4*lambda, m1pinbox[2]+y,
											 m1pinbox[3]+y, 0, 0, romplane);
					if ((transcont) || ((i==1) && (romarray[m][0] == 1))) {
						diffpins[m][i] =
							makeCStyleNodeInst(diffpin, diffpinbox[0]+x-4*lambda,
												 diffpinbox[1]+x-4*lambda, diffpinbox[2]+y,
												 diffpinbox[3]+y, 0, 0, romplane);		
					} else
					{
						diffpins[m][i] =
							makeCStyleNodeInst(m1pin, m1pinbox[0]+x-4*lambda,
								m1pinbox[1]+x-4*lambda,m1pinbox[2]+y,m1pinbox[3]+y,
								0, 0, romplane);
					}					
					minports[m][i] = m1pinport;
				}
				if (i == inputs)
				{
					ap1 = minpins[m][i];
					apport1 = minports[m][i];
					makeCStyleExport(romplane, ap1, apport1, "out_"+m, PortCharacteristic.OUT);
				}
				if (i == 0)
				{
					if (m%2 == 1)
					{
						nwellc[m/2] =
							makeCStyleNodeInst(mnwc, m1m2cbox[0]+x-46*lambda,
												 mnwcbox[1]+x-46*lambda, mnwcbox[2]+y,
												 mnwcbox[3]+y, 0, 0, romplane);
						nwellcports[m/2] = mnwcport;
					}
					m1m2_2pins[m] =
						makeCStyleNodeInst(m1m2c, m1m2cbox[0]+x-4*lambda,
											 m1m2cbox[1]+x-4*lambda, m1m2cbox[2]+y,
											 m1m2cbox[3]+y, 0, 0, romplane);
					m1m2_2ports[m] = m1m2cport;
					m1m2_3pins[m] =
						makeCStyleNodeInst(m1m2c, m1m2cbox[0]+x-20*lambda,
											 m1m2cbox[1]+x-20*lambda, m1m2cbox[2]+y,
											 m1m2cbox[3]+y, 0, 0, romplane);
					m1m2_3ports[m] = m1m2cport;
					mpac_1pins[m] =
						makeCStyleNodeInst(mpac, mpacbox[0]+x-20*lambda,
											 mpacbox[1]+x-20*lambda, mpacbox[2]+y,
											 mpacbox[3]+y, 0, 0, romplane);
					mpac_1ports[m] = mpacport;
					pulluptrans[m] =
						makeCStyleNodeInst(pmos, pmosbox[0]+x-26*lambda,
											 pmosbox[1]+x-26*lambda, pmosbox[2]+y,
											 pmosbox[3]+y, 1, 0, romplane);
					mpac_2pins[m] =
						makeCStyleNodeInst(mpac, mpacbox[0]+x-32*lambda,
											 mpacbox[1]+x-32*lambda, mpacbox[2]+y,
											 mpacbox[3]+y, 0, 0, romplane);
					mpac_2ports[m] = mpacport;
					m1m2_4pins[m] =
						makeCStyleNodeInst(m1m2c, m1m2cbox[0]+x-32*lambda,
											 m1m2cbox[1]+x-32*lambda, m1m2cbox[2]+y,
											 m1m2cbox[3]+y, 0, 0, romplane);
					m1m2_4ports[m] = m1m2cport;
					if (m == 0) {
						gndpex[m] =
							makeCStyleNodeInst(mpc, mpcbox[0]+x-26*lambda,
												 mpcbox[1]+x-26*lambda,
												 mpcbox[2]+y-8*lambda,
												 mpcbox[3]+y-8*lambda, 0, 0, romplane);
						gndpexport[m] = mpcport;			
					}
				} 
			}
		}

		// finished placing objects, start wiring arcs
		for (i=0; i<inputs; i++)
		{
			ap1 = andtrans[0][i];
			apport1 = ppinport;
			appos1 = getCStylePortPosition(ap1, apport1); 
			ap2 = m1polypins[i];
			apport2 = mpcport;
			appos2 = getCStylePortPosition(ap2, apport2); 
			ap3 = m1m2pins[i];
			apport3 = m1m2cport;
			appos3 = getCStylePortPosition(ap3, apport3); 
			makeCStyleArcInst(parc, 2*lambda, ap1, apport1, appos1[0],
								appos1[1], ap2, apport2, appos2[0], appos2[1]);
			makeCStyleArcInst(m1arc, 4*lambda, ap2, apport2, appos2[0],
								appos2[1], ap3, apport3, appos3[0], appos3[1]);
		}
	
		for (i=0; i<inputs; i++)
		{
			ap1 = andtrans[0][i];
			apport1 = ppinport;
			appos1 = getCStylePortPosition(ap1, apport1); 
			for (m=1; m<wordlines+1; m++)
			{
				ap2 = andtrans[m][i];
				if (romarray[m-1][i] == 1)
				{
					apport2 = nmosg1port;
					apport3 = nmosg2port;
				} else
				{
					apport2 = ppinport;
					apport3 = ppinport;
				}
				appos2 = getCStylePortPosition(ap2, apport2);
				appos3 = getCStylePortPosition(ap2, apport3);
				makeCStyleArcInst(parc, 2*lambda, ap1, apport1, appos1[0],
									appos1[1], ap2, apport2,
									appos2[0], appos2[1]);
				ap1 = ap2;
				apport1 = apport3;
				appos1 = appos3;
			}
		}

		// connect m1 wordline lines
		for (m=0; m<wordlines; m++)
		{
			ap1 = minpins[m][0];
			apport1 = minports[m][0];
			appos1 = getCStylePortPosition(ap1, apport1); 
			for (i=1; i<inputs+1; i++)
			{
				ap2 = minpins[m][i];
				apport2 = minports[m][i];
				appos2 = getCStylePortPosition(ap2, apport2); 
				makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
									appos1[1], ap2, apport2,
									appos2[0], appos2[1]);
				ap1 = ap2;
				apport1 = apport2;
				appos1 = appos2;
			}
		}
		
		// connect transistors to wordline lines
		for (m=0; m<wordlines; m++)
		{
			for (i=0; i<inputs; i++)
			{
				if (romarray[m][i] == 1)
				{
					// connect transistor
					ap1 = andtrans[m+1][i];
					gnd1 = ap1;
					if (i%2 == 0)
					{
						apport1 = nmosd1port;
						gndport1 = nmosd2port;
						ap2 = minpins[m][i];
						gnd2 = gndpins[m/2][i+1];
						intgnd = diffpins[m][i+1];
					} else
					{
						apport1 = nmosd2port;
						gndport1 = nmosd1port;
						ap2 = minpins[m][i+1];
						gnd2 = gndpins[m/2][i];
						intgnd = diffpins[m][i];
					}
					appos1 = getCStylePortPosition(ap1, apport1);
					gndpos1 = getCStylePortPosition(gnd1, gndport1);
					apport2 = mnacport;
					appos2 = getCStylePortPosition(ap2, apport2);
					gndport2 = mnacport;
					gndpos2 = getCStylePortPosition(gnd2, gndport2);
					intgndport = diffpinport;
					intgndpos = getCStylePortPosition(intgnd, intgndport);
					makeCStyleArcInst(ndiffarc, 16*lambda, ap1, apport1,
										appos1[0], appos1[1],
										ap2, apport2, appos2[0],
										appos2[1]);
					makeCStyleArcInst(ndiffarc, 16*lambda, gnd1, gndport1,
										gndpos1[0], gndpos1[1],
										intgnd, intgndport, intgndpos[0],
										intgndpos[1]);
					makeCStyleArcInst(ndiffarc, 16*lambda, intgnd, intgndport,
										intgndpos[0], intgndpos[1],
										gnd2, gndport2, gndpos2[0],
										gndpos2[1]);
				}
			}
		}
		
		// connect ground lines
		for (m=0; m<wordlines/2; m++)
		{
			ap1 = gndpins[m][0];
			apport1 = gndports[m][0];
			appos1 = getCStylePortPosition(ap1, apport1); 
			for (i=1; i<inputs+1; i++)
			{
				ap2 = gndpins[m][i];
				apport2 = gndports[m][i];
				appos2 = getCStylePortPosition(ap2, apport2); 
				makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
									appos1[1], ap2, apport2,
									appos2[0], appos2[1]);
				if (i == inputs)
				{
					makeCStyleExport(romplane, ap1, apport1, "romgnd"+m, PortCharacteristic.GND);
				}
				ap1 = ap2;
				apport1 = apport2;
				appos1 = appos2;
			}
		}
		
		// extend the gnd plane
		for (m=0; m<wordlines/2; m++)
		{
	 		ap1 = gndpins[m][0];
			apport1 = gndports[m][0];
			appos1 = getCStylePortPosition(ap1, apport1);
			ap2 = gnd_2pins[m];
			apport2 = gnd_2ports[m];
			appos2 = getCStylePortPosition(ap2, apport2); 
			makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
								appos1[1], ap2, apport2, appos2[0],
								appos2[1]);
		}
	
		// tie up all the gndlines
		ap1 = gnd_2pins[0];
		apport1 = gnd_2ports[0];
		appos1 = getCStylePortPosition(ap1, apport1);
		for (m=0; m<wordlines/2; m++)
		{
			ap2 = gnd_2pins[m];
			apport2 = gnd_2ports[m];
			appos2 = getCStylePortPosition(ap2, apport2); 
			makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
								appos1[1], ap2, apport2, appos2[0], appos2[1]);
			if (m == (wordlines/2 - 1))
			{
				makeCStyleExport(romplane, ap2, apport2, "gnd", PortCharacteristic.GND);
			}
		}
		
		ap2 = gndm1ex[0];
		apport2 = gndm1export[0];
		appos2 = getCStylePortPosition(ap2, apport2); 
		makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
							appos1[1], ap2, apport2, appos2[0], appos2[1]);
		ap1 = gnd1pin;
		apport1 = gnd1port;
		appos1 = getCStylePortPosition(ap1, apport1);
		makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
							appos1[1], ap2, apport2, appos2[0], appos2[1]);
		makeCStyleExport(romplane, ap1, apport1, "gndc", PortCharacteristic.GND);
	
		ap1 = gndpex[0];
		apport1 = gndpexport[0];
		appos1 = getCStylePortPosition(ap1, apport1);
		makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
							appos1[1], ap2, apport2, appos2[0], appos2[1]);
	
		ap2 = pulluptrans[0];
		apport2 = pmosg1port;
		appos2 = getCStylePortPosition(ap2, apport2);
		makeCStyleArcInst(parc, 3*lambda, ap1, apport1, appos1[0],
							appos1[1], ap2, apport2, appos2[0], appos2[1]);
	
		// connect m1m2contact from romplane to m1m2contact before pull-up trans
		for (m=0; m<wordlines; m++)
		{
			ap1 = minpins[m][0];
			apport1 = minports[m][0];
			appos1 = getCStylePortPosition(ap1, apport1); 
			ap2 = m1m2_2pins[m];
			apport2 = m1m2_2ports[m];
			appos2 = getCStylePortPosition(ap2, apport2); 
			ap3 = m1m2_3pins[m];
			apport3 = m1m2_3ports[m];
			appos3 = getCStylePortPosition(ap3, apport3); 
			makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
								appos1[1], ap2, apport2, appos2[0], appos2[1]);
			makeCStyleArcInst(m2arc, 4*lambda, ap2, apport2, appos2[0],
								appos2[1], ap3, apport3, appos3[0], appos3[1]);
		}
		
		// connect m1m2contact from romplane to mpac of pull-up trans
		for (m=0; m<wordlines; m++)
		{
			ap1 = m1m2_3pins[m];
			apport1 = m1m2_3ports[m];
			appos1 = getCStylePortPosition(ap1, apport1); 
			ap2 = mpac_1pins[m];
			apport2 = mpac_1ports[m];
			appos2 = getCStylePortPosition(ap2, apport2); 
			makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
								appos1[1], ap2, apport2, appos2[0], appos2[1]);
		}
		
		// connect pull-up transistors to the mpac
		for (m=0; m<wordlines; m++)
		{
			ap1 = pulluptrans[m];
			ap4 = ap1;
			apport4 = pmosd1port;
			apport1 = pmosd2port;
			ap2= mpac_1pins[m];
			ap3= mpac_2pins[m];
			apport2 = mpacport;
			apport3 = mpacport;
			appos1 = getCStylePortPosition(ap1, apport1);
			appos4 = getCStylePortPosition(ap4, apport4);
			appos2 = getCStylePortPosition(ap2, apport2);
			appos3 = getCStylePortPosition(ap3, apport3);
			makeCStyleArcInst(pdiffarc, 15*lambda, ap1, apport1, appos1[0],
								appos1[1], ap2, apport2, appos2[0], appos2[1]);
			makeCStyleArcInst(pdiffarc, 15*lambda, ap4, apport4, appos4[0],
								appos4[1], ap3, apport3, appos3[0], appos3[1]);
		}
			
		// connect mpac of pull-up trans to m1m2c
		for (m=0; m<wordlines; m++)
		{
			ap1 = m1m2_4pins[m];
			apport1 = m1m2_4ports[m];
			appos1 = getCStylePortPosition(ap1, apport1); 
			ap2 = mpac_2pins[m];
			apport2 = mpac_2ports[m];
			appos2 = getCStylePortPosition(ap2, apport2); 
			makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
								appos1[1], ap2, apport2, appos2[0], appos2[1]);
		}
		
		// connect mpac of pull-up trans to m1m2c
		for (m=0; m<wordlines; m++)
		{
			if (m%2 ==1)
			{
				ap1 = nwellc[m/2];
				apport1 = nwellcports[m/2];
				appos1 = getCStylePortPosition(ap1, apport1); 
				ap2 = mpac_2pins[m];
				apport2 = mpac_2ports[m];
				appos2 = getCStylePortPosition(ap2, apport2); 
				makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
									appos1[1], ap2, apport2, appos2[0], appos2[1]);
			}
		}
		
		// tie up all the vddlines
		ap1 = m1m2_4pins[0];
		apport1 = m1m2_4ports[0];
		appos1 = getCStylePortPosition(ap1, apport1);	
		for (m=0; m<wordlines; m++)
		{
	 		ap2 = m1m2_4pins[m];
			apport2 = m1m2_4ports[m];
			appos2 = getCStylePortPosition(ap2, apport2); 
			makeCStyleArcInst(m2arc, 4*lambda, ap1, apport1, appos1[0],
								appos1[1], ap2, apport2, appos2[0], appos2[1]);
		}
		
		ap2 = vdd2pin;
		apport2 = vdd2port;
		appos2 = getCStylePortPosition(ap2, apport2);
		makeCStyleArcInst(m2arc, 4*lambda, ap1, apport1, appos1[0],
							appos1[1], ap2, apport2, appos2[0], appos2[1]);
	
		makeCStyleExport(romplane, ap2, apport2, "vdd", PortCharacteristic.PWR);
	
		// connect poly for the pull-up transistor
		for (m=0; m<wordlines-1; m++)
		{
		 	ap1 = pulluptrans[m];
			apport1 = pmosg2port;
			appos1 = getCStylePortPosition(ap1, apport1); 
			ap2 = pulluptrans[m+1];
			apport2 = pmosg1port;
			appos2 = getCStylePortPosition(ap2, apport2);
			makeCStyleArcInst(parc, 3*lambda, ap1, apport1, appos1[0],
								appos1[1], ap2, apport2, appos2[0], appos2[1]);
		}
	}

	/**
	 */
	private static void decodernmos(Library destLib, double lambda, int bits, String cellname, boolean top)
	{
		int[][] romplane = generateplane(bits);
		int i, m, o;
		double x, y;
		NodeInst ap1, ap2, ap3, gnd1, gnd2, vdd1, vdd2;
		PortProto apport1, apport2, apport3, gndport1, gndport2,
							vddport1, vddport2;
		double[] appos1, appos2, appos3, gndpos1, gndpos2, vddpos1, vddpos2;
	
		int inputs = romplane[0].length;
		int wordlines = romplane.length;
		
		NodeInst[][] ortrans = new NodeInst[wordlines+3][inputs+2];
		NodeInst[][] minpins = new NodeInst[wordlines+2][inputs+2];
		NodeInst[][] diffpins = new NodeInst[wordlines+2][inputs+2];
		NodeInst[][] gndpins = new NodeInst[wordlines/2][inputs+2];
		NodeInst[][] vddpins = new NodeInst[wordlines][inputs/2];
		NodeInst[] pwrpins = new NodeInst[inputs/2];
		NodeInst[][] m1m2pins = new NodeInst[wordlines+2][inputs+2];
		NodeInst[] pwcpins = new NodeInst[wordlines+2];
		
		
		PortProto[][] minports = new PortProto[wordlines+2][inputs+2];
		PortProto[][] gndports = new PortProto[wordlines/2][inputs+2];
		PortProto[][] vddports = new PortProto[wordlines][inputs/2];
		PortProto[] pwrports = new PortProto[inputs/2];
		PortProto[][] diffports = new PortProto[wordlines/2][inputs+2];
		PortProto[][] m1m2ports = new PortProto[wordlines+2][inputs+2];
		PortProto[] pwcports = new PortProto[wordlines+2];

		// get pointers to primitives
		NodeProto nmos = tech.findNodeProto("N-Transistor");
		PortProto nmosg1port = nmos.findPortProto("n-trans-poly-right");
		PortProto nmosg2port = nmos.findPortProto("n-trans-poly-left");
		PortProto nmosd1port = nmos.findPortProto("n-trans-diff-top");
		PortProto nmosd2port = nmos.findPortProto("n-trans-diff-bottom");
		double[] nmosbox = {-nmos.getDefWidth()/2-lambda/2,
	 					 nmos.getDefWidth()/2+lambda/2,
						 -nmos.getDefHeight()/2,
						 nmos.getDefHeight()/2};
			
		NodeProto pmos = tech.findNodeProto("P-Transistor");
		PortProto pmosg1port = pmos.findPortProto("p-trans-poly-right");
		PortProto pmosg2port = pmos.findPortProto("p-trans-poly-left");
		PortProto pmosd1port = pmos.findPortProto("p-trans-diff-top");
		PortProto pmosd2port = pmos.findPortProto("p-trans-diff-bottom");
		double[] pmosbox = {-pmos.getDefWidth()/2-lambda/2,
						 pmos.getDefWidth()/2+lambda/2,
						 -pmos.getDefHeight()/2,
						 pmos.getDefHeight()/2};
	
		NodeProto ppin = tech.findNodeProto("Polysilicon-1-Pin");
		PortProto ppinport = ppin.getPort(0);
		double[] ppinbox = {-ppin.getDefWidth()/2,
						 ppin.getDefWidth()/2,
						 -ppin.getDefHeight()/2,
						 ppin.getDefHeight()/2};
		
		NodeProto napin = tech.findNodeProto("N-Active-Pin");
		
		NodeProto m1pin = tech.findNodeProto("Metal-1-Pin");
		PortProto m1pinport = m1pin.getPort(0);
		double[] m1pinbox = {-m1pin.getDefWidth()/2-lambda/2,
						 m1pin.getDefWidth()/2+lambda/2,
						 -m1pin.getDefHeight()/2-lambda/2,
						 m1pin.getDefHeight()/2+lambda/2};
		
		NodeProto m2pin = tech.findNodeProto("Metal-2-Pin");
		PortProto m2pinport = m2pin.getPort(0);
		double[] m2pinbox = {-m1pin.getDefWidth()/2-lambda/2,
						 m1pin.getDefWidth()/2+lambda/2,
						 -m1pin.getDefHeight()/2-lambda/2,
						 m1pin.getDefHeight()/2+lambda/2};
		
	
		NodeProto nwnode = tech.findNodeProto("N-Well-Node");
		double[] nwnodebox =
			{-nwnode.getDefWidth()/2-lambda/2,
			 nwnode.getDefWidth()/2+lambda/2,
			 -nwnode.getDefHeight()/2-lambda/2,
			 nwnode.getDefHeight()/2+lambda/2};
		NodeProto pwnode = tech.findNodeProto("P-Well-Node");
		double[] pwnodebox =
			{-pwnode.getDefWidth()/2-lambda/2,
			 pwnode.getDefWidth()/2+lambda/2,
			 -pwnode.getDefHeight()/2-lambda/2,
			 pwnode.getDefHeight()/2+lambda/2};
		
		double mx = 5;
		NodeProto mpc = tech.findNodeProto("Metal-1-Polysilicon-1-Con");
		PortProto mpcport = mpc.getPort(0);
		double[] mpcbox = {-mx*lambda/2, mx*lambda/2, -mx*lambda/2, mx*lambda/2}; 
	
		NodeProto m1m2c = tech.findNodeProto("Metal-1-Metal-2-Con");
		PortProto m1m2cport = m1m2c.getPort(0);
		double[] m1m2cbox = {-5*lambda/2, 5*lambda/2, -5*lambda/2, 5*lambda/2}; 
	
		NodeProto diffpin = tech.findNodeProto("Active-Pin");
		PortProto diffpinport = diffpin.getPort(0);
		double[] diffpinbox =
			{-diffpin.getDefWidth()/2-lambda/2,
			 diffpin.getDefWidth()/2+lambda/2,
			 -diffpin.getDefHeight()/2-lambda/2,
			 diffpin.getDefHeight()/2+lambda/2};
		
		NodeProto mnac = tech.findNodeProto("Metal-1-N-Active-Con");
		PortProto mnacport = mnac.getPort(0);
		double aaa = 17;
		double[] mnacbox = {-aaa*lambda/2, aaa*lambda/2, -aaa*lambda/2, aaa*lambda/2};
		// centers around 6 goes up by multiples of 2
	
		NodeProto mpac = tech.findNodeProto("Metal-1-P-Active-Con");
		PortProto mpacport = mpac.getPort(0);
		double[] mpacbox = {-aaa*lambda/2, aaa*lambda/2, -aaa*lambda/2, aaa*lambda/2};
		// centers around 6 goes up by multiples of 2
	
		NodeProto mpwc = tech.findNodeProto("Metal-1-P-Well-Con");
		PortProto mpwcport = mpwc.getPort(0);
		double[] mpwcbox = {-17*lambda/2, 17*lambda/2, -17*lambda/2, 17*lambda/2};
	
		NodeProto mnwc = tech.findNodeProto("Metal-1-N-Well-Con");
		PortProto mnwcport = mnwc.getPort(0);
		double[] mnwcbox = {-17*lambda/2, 17*lambda/2, -17*lambda/2, 17*lambda/2};
	
		ArcProto parc = tech.findArcProto("Polysilicon-1");
		ArcProto m1arc = tech.findArcProto("Metal-1");
		ArcProto m2arc = tech.findArcProto("Metal-2");
		ArcProto ndiffarc = tech.findArcProto("N-Active");
		ArcProto pdiffarc = tech.findArcProto("P-Active");
	
		// create a cell called cellname+"{lay}" in the destination library
		Cell decn = Cell.newInstance(destLib, cellname+"{lay}");

		NodeProto nsnode = tech.findNodeProto("N-Select-Node");
		int nselectx = 8; 
		int nselecty = 8;
		double[] nsnodebox =
			{-nselectx*lambda/2, nselectx*lambda/2, -nselecty*lambda/2, nselecty*lambda/2};
	
		NodeInst pwellnode =
			makeCStyleNodeInst(pwnode,0,(8*lambda*(2*bits+1)),
										0,8*lambda*(wordlines+1),0,0,decn);
		NodeInst nselectnode =
			makeCStyleNodeInst(nsnode,0,(8*lambda*(2*bits+1)),
										0,8*lambda*(wordlines+1),0,0,decn);
		
		// Create instances of objects on decoder nmos plane
		x = 0;
		for (i=0; i<inputs+1; i++)
		{
			x += 8*lambda;
			y = 0;
			if (i%2 ==1)	
			{
				x += 0*lambda;
			}
			if (i < inputs)
			{
				if (top == true)
				{
					ortrans[0][i] =
						makeCStyleNodeInst(ppin, ppinbox[0]+x, ppinbox[1]+x,
												   ppinbox[2], ppinbox[3], 0, 0, decn);
				} else
				{    
					ortrans[0][i] =
						makeCStyleNodeInst(mpc, mpcbox[0]+x, mpcbox[1]+x,
												  mpcbox[2], mpcbox[3], 0, 0, decn);
				}
			}
			for (m=0; m<wordlines; m++)
			{
				y += 8*lambda;
				if (i%2 == 1)
				{
					vddpins[m][i/2] =
						makeCStyleNodeInst(mnac, mnacbox[0]+x-4*lambda,
											 mnacbox[1]+x-4*lambda,
											 mnacbox[2]+y, mnacbox[3]+y, 0, 0, decn);
					vddports[m][i/2] = mnacport;
					if (m == (wordlines-1))
					{
						pwrpins[i/2] =
							makeCStyleNodeInst(m1pin, m1pinbox[0]+x-4*lambda,
												 m1pinbox[1]+x-4*lambda,
												 m1pinbox[2]+y+(8*lambda),
												 m1pinbox[3]+y+(8*lambda), 0, 0, decn);
						pwrports[i/2] = m1pinport;
					}
				}
				if (i < inputs)
				{
					if (romplane[m][i] == 1)
					{
						// create a transistor
						ortrans[m+1][i] =
							makeCStyleNodeInst(nmos, nmosbox[0]+x, nmosbox[1]+x,
												 nmosbox[2]+y, nmosbox[3]+y, 1, 0, decn);
					} else
					{
						ortrans[m+1][i] =
							makeCStyleNodeInst(ppin, ppinbox[0]+x, ppinbox[1]+x,
												 ppinbox[2]+y, ppinbox[3]+y, 0, 0, decn);
					}
					if (m == wordlines-1)
					{
						if (top == true)
						{
								ortrans[m+2][i] =
									makeCStyleNodeInst(mpc, mpcbox[0]+x, mpcbox[1]+x,
														 mpcbox[2]+y+16*lambda,
														 mpcbox[3]+y+16*lambda,
														 0, 0, decn);
						} else
						{
							ortrans[m+2][i] =
								makeCStyleNodeInst(ppin, ppinbox[0]+x, ppinbox[1]+x,
													 ppinbox[2]+y+4*lambda, ppinbox[3]+y+4*lambda,
													 0, 0, decn);	
						}
					}
				}
				boolean transcont = false;
				if (i < inputs) transcont = (romplane[m][i] == 1);
				if (i > 1) transcont |= (romplane[m][i-1] == 1);
				if (i%2 == 0 && transcont)
				{
					minpins[m][i] =
						makeCStyleNodeInst(mnac, mnacbox[0]+x-4*lambda,
											 mnacbox[1]+x-4*lambda, mnacbox[2]+y,
											 mnacbox[3]+y, 0, 0, decn);			
					minports[m][i] = mnacport;
					m1m2pins[m][i] =
						makeCStyleNodeInst(m1m2c, m1m2cbox[0]+x-4*lambda,
											 m1m2cbox[1]+x-4*lambda, m1m2cbox[2]+y,
											 m1m2cbox[3]+y, 0, 0, decn);
					m1m2ports[m][i] = m1m2cport;
				} else
				{
					minpins[m][i] =
						makeCStyleNodeInst(m2pin, m2pinbox[0]+x-4*lambda,
											 m2pinbox[1]+x-4*lambda, m2pinbox[2]+y,
											 m2pinbox[3]+y, 0, 0, decn);
					minports[m][i] = m2pinport;
					if (i==0)
					{
						m1m2pins[m][i] =
							makeCStyleNodeInst(m1m2c, m1m2cbox[0]+x-4*lambda,
												 m1m2cbox[1]+x-4*lambda, m1m2cbox[2]+y,
												 m1m2cbox[3]+y, 0, 0, decn);
						m1m2ports[m][i] = m1m2cport;
					} else
					{
						m1m2pins[m][i] =
							makeCStyleNodeInst(m2pin, m2pinbox[0]+x-4*lambda,
												 m2pinbox[1]+x-4*lambda, m2pinbox[2]+y,
												 m2pinbox[3]+y, 0, 0, decn);
						m1m2ports[m][i] = m2pinport;
					}
				}
				if (i==0)
				{
					ap1 = m1m2pins[m][i];
					apport1 = m1m2ports[m][i];
					makeCStyleExport(decn, ap1, apport1, "mid"+m, PortCharacteristic.IN);
				}
			}
		}
	
		// finished making instances, start making arcs
		ap1 = pwrpins[0];
		apport1 = pwrports[0];
		appos1 = getCStylePortPosition(ap1, apport1);
		for (i=1; i<inputs/2; i++)
		{
			ap2 = pwrpins[i];
			apport2 = pwrports[i];
			appos2 = getCStylePortPosition(ap2, apport2); 
			makeCStyleArcInst(m1arc, 4*lambda,
								ap1, apport1, appos1[0], appos1[1],
								ap2, apport2, appos2[0], appos2[1]);
			ap1 = ap2;
			apport1 = apport2;
			appos1 = appos2;
		}
		makeCStyleExport(decn, ap1, apport1, "gnd", PortCharacteristic.GND);
	
		m = wordlines - 1;
		for (i=0; i<inputs/2; i++)
		{
			ap1 = vddpins[m][i];
			apport1 = vddports[m][i];
			appos1 = getCStylePortPosition(ap1, apport1); 
			ap2 = pwrpins[i];
			apport2 = pwrports[i];
			appos2 = getCStylePortPosition(ap2, apport2); 
			makeCStyleArcInst(m1arc, 4*lambda,
								ap1, apport1, appos1[0], appos1[1],
								ap2, apport2, appos2[0], appos2[1]);
		}
	
		// connect polysilicon gates
		for (i=0; i<inputs; i++)
		{
			ap1 = ortrans[wordlines+1][i];
			if (top == true)
			{
				apport1 = mpcport;
			} else
			{
				apport1 = ppinport;
			}
			appos1 = getCStylePortPosition(ap1, apport1); 
			if (i%2 == 0)
			{
				makeCStyleExport(decn, ap1, apport1, "top_in"+(i/2), PortCharacteristic.IN);			
			} else
			{
				makeCStyleExport(decn, ap1, apport1, "top_in"+((i-1)/2)+"_b", PortCharacteristic.IN);			
			}
		
			ap1 = ortrans[0][i];
			if (top == true)
			{
				apport1 = ppinport;
			} else
			{
				apport1 = mpcport;
			}
			appos1 = getCStylePortPosition(ap1, apport1); 
	
			if (i%2 == 0)
			{
				makeCStyleExport(decn, ap1, apport1, "bot_in"+(i/2), PortCharacteristic.IN);
			} else
			{
				makeCStyleExport(decn, ap1, apport1, "bot_in"+((i-1)/2)+"_b", PortCharacteristic.IN);
			}
	
			for (m=1; m<wordlines+1; m++)
			{
				ap2 = ortrans[m][i];
				if (romplane[m-1][i] == 1)
				{
					apport2 = nmosg1port;
					apport3 = nmosg2port;
				} else
				{
					apport2 = ppinport;
					apport3 = ppinport;
				}
				appos2 = getCStylePortPosition(ap2, apport2);
				appos3 = getCStylePortPosition(ap2, apport3);
				makeCStyleArcInst(parc, 2*lambda,
									ap1,apport1,appos1[0],appos1[1],
									ap2,apport2,appos2[0],appos2[1]);
				ap1 = ap2;
				apport1 = apport3;
				appos1 = appos3;
			}
			
			ap2 = ortrans[wordlines+1][i];
			if (top == true)
			{
				apport2 = mpcport;
				apport3 = mpcport;
			} else
			{
				apport2 = ppinport;
				apport3 = ppinport;
			}
			appos2 = getCStylePortPosition(ap2, apport2);
			appos3 = getCStylePortPosition(ap2, apport3);
			makeCStyleArcInst(parc, 2*lambda,
								ap1, apport1, appos1[0], appos1[1],
								ap2, apport2, appos2[0], appos2[1]);
		}
	
		// connect m2 wordline lines
		for (m=0; m<wordlines; m++)
		{
			ap1 = m1m2pins[m][0];
			apport1 = m1m2ports[m][0];
			appos1 = getCStylePortPosition(ap1, apport1); 
			for (i=1; i<inputs+1; i++)
			{
				ap2 = m1m2pins[m][i];
				apport2 = m1m2ports[m][i];
				appos2 = getCStylePortPosition(ap2, apport2); 
				makeCStyleArcInst(m2arc, 4*lambda, ap1, apport1,
									appos1[0], appos1[1], ap2, apport2, appos2[0], appos2[1]);
				ap1 = ap2;
				apport1 = apport2;
				appos1 = appos2;
			}
			makeCStyleExport(decn, ap1, apport1, "word"+m, PortCharacteristic.OUT);
		}
	
		// connect transistors to wordline lines
		for (m=0; m<wordlines; m++)
		{
			for (i=0; i<inputs; i++)
			{
				if (romplane[m][i] == 1)
				{
					// connect transistor
					ap1 = ortrans[m+1][i];
					vdd1 = ap1;
					if (i%2 == 0)
					{
						apport1 = nmosd1port;
						vddport1 = nmosd2port;
						ap2 = minpins[m][i];
						ap3 = m1m2pins[m][i];
						vdd2 = vddpins[m][i/2];
					} else
					{
						apport1 = nmosd2port;
						vddport1 = nmosd1port;
						ap2 = minpins[m][i+1];
						ap3 = m1m2pins[m][i+1];
						vdd2 = vddpins[m][i/2];
					}
					apport2 = mnacport;
					apport3 = m1m2cport;
					vddport2 = mnacport;
	
					appos1 = getCStylePortPosition(ap1, apport1);
					vddpos1 = getCStylePortPosition(vdd1, vddport1);
					appos2 = getCStylePortPosition(ap2, apport2);
					appos3 = getCStylePortPosition(ap3, apport3);
					vddpos2 = getCStylePortPosition(vdd2, vddport2);
					
					// ndiffarc size centers around 12 and goes up by multiples of 2
					makeCStyleArcInst(ndiffarc, 16*lambda, ap1, apport1,
										appos1[0], appos1[1], ap2,
										apport2, appos2[0], appos2[1]);
					makeCStyleArcInst(ndiffarc, 16*lambda, vdd1, vddport1,
										vddpos1[0], vddpos1[1], vdd2,
										vddport2, vddpos2[0], vddpos2[1]);
	
					makeCStyleArcInst(m1arc, 4*lambda, ap2, apport2,
										appos2[0], appos2[1], ap3,
										apport3, appos3[0], appos3[1]);
				}
			}
		}
	
		// connect vdd lines
		for (i=0; i<inputs/2; i++)
		{
			ap1 = vddpins[0][i];
			apport1 = vddports[0][i];
			appos1 = getCStylePortPosition(ap1, apport1); 
			for (m=1; m<wordlines; m++)
			{
				ap2 = vddpins[m][i];
				apport2 = vddports[m][i];
				appos2 = getCStylePortPosition(ap2, apport2); 
				makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1,
									appos1[0], appos1[1],
									ap2, apport2, appos2[0], appos2[1]);
				ap1 = ap2;
				apport1 = apport2;
				appos1 = appos2;
			}
		}
	}
	

	/**
	 */
	private static void decoderpmos(Library destLib, double lambda, int bits, String cellname, boolean top)
	{
		int[][] romplane = generateplane(bits);
		int i, m, o;
		double x, y;
		NodeInst ap1, ap2, ap3, apx, apy;
		PortProto apport1, apport2, apport3, apportx, apporty;
		double[] appos1, appos2, appos3, apposx, apposy;
		int inputs = romplane[0].length;
		int wordlines = romplane.length;
		
		NodeInst[][] andtrans = new NodeInst[wordlines+2][inputs+2];
		NodeInst[][] minpins = new NodeInst[wordlines+3][inputs+2];
		NodeInst[] m1m2pins = new NodeInst[wordlines+2];
		NodeInst[] m2pins = new NodeInst[wordlines+2];
		NodeInst vddpin = null;
		NodeInst vddipin = null;
		NodeInst vddbpin = null;
		NodeInst vddcpin = null;
	
		PortProto[][] minports = new PortProto[wordlines+2][inputs+2];
		PortProto[] m1m2ports = new PortProto[wordlines+2];
		PortProto[] m2ports = new PortProto[wordlines+2];
		PortProto vddport = null;
		PortProto vddiport = null;
		PortProto vddbport = null;
		PortProto vddcport = null;
	
		// get pointers to primitives
		NodeProto nmos = tech.findNodeProto("N-Transistor");
		PortProto nmosg1port = nmos.findPortProto("n-trans-poly-right");
		PortProto nmosg2port = nmos.findPortProto("n-trans-poly-left");
		PortProto nmosd1port = nmos.findPortProto("n-trans-diff-top");
		PortProto nmosd2port = nmos.findPortProto("n-trans-diff-bottom");
		double[] nmosbox = {-nmos.getDefWidth()/2-lambda/2,
						 nmos.getDefWidth()/2+lambda/2,
						 -nmos.getDefHeight()/2,
						 nmos.getDefHeight()/2};
					
		NodeProto pmos = tech.findNodeProto("P-Transistor");
		PortProto pmosg1port = pmos.findPortProto("p-trans-poly-right");
		PortProto pmosg2port = pmos.findPortProto("p-trans-poly-left");
		PortProto pmosd1port = pmos.findPortProto("p-trans-diff-top");
		PortProto pmosd2port = pmos.findPortProto("p-trans-diff-bottom");
		double[] pmosbox = {-pmos.getDefWidth()/2-lambda/2,
						 pmos.getDefWidth()/2+lambda/2,
						 -pmos.getDefHeight()/2,
						 pmos.getDefHeight()/2};
	
		NodeProto ppin = tech.findNodeProto("Polysilicon-1-Pin");
		PortProto ppinport = ppin.getPort(0);
		double[] ppinbox = {-ppin.getDefWidth()/2,
						 ppin.getDefWidth()/2,
						 -ppin.getDefHeight()/2,
						 ppin.getDefHeight()/2};
		
		NodeProto napin = tech.findNodeProto("N-Active-Pin");
		
		NodeProto m1pin = tech.findNodeProto("Metal-1-Pin");
		PortProto m1pinport = m1pin.getPort(0);
		double[] m1pinbox = {-m1pin.getDefWidth()/2-lambda/2,
						  m1pin.getDefWidth()/2+lambda/2,
						  -m1pin.getDefHeight()/2-lambda/2,
						  m1pin.getDefHeight()/2+lambda/2};
		
		NodeProto m2pin = tech.findNodeProto("Metal-2-Pin");
		PortProto m2pinport = m2pin.getPort(0);
		double[] m2pinbox = {-m1pin.getDefWidth()/2-lambda/2,
						  m1pin.getDefWidth()/2+lambda/2,
						  -m1pin.getDefHeight()/2-lambda/2,
						  m1pin.getDefHeight()/2+lambda/2};
	
		double mx = 5;
		NodeProto mpc = tech.findNodeProto("Metal-1-Polysilicon-1-Con");
		PortProto mpcport = mpc.getPort(0);
		double[] mpcbox = {-mx*lambda/2, mx*lambda/2, -mx*lambda/2, mx*lambda/2}; 
		
		NodeProto diffpin = tech.findNodeProto("Active-Pin");
		PortProto diffpinport = diffpin.getPort(0);
		double[] diffpinbox =
			{-diffpin.getDefWidth()/2-lambda/2,
			 diffpin.getDefWidth()/2+lambda/2,
			 -diffpin.getDefHeight()/2-lambda/2,
			 diffpin.getDefHeight()/2+lambda/2};
		
		NodeProto m1m2c = tech.findNodeProto("Metal-1-Metal-2-Con");
		PortProto m1m2cport = m1m2c.getPort(0);
		double[] m1m2cbox = {-5*lambda/2, 5*lambda/2, -5*lambda/2, 5*lambda/2}; 
	
		NodeProto mnac = tech.findNodeProto("Metal-1-N-Active-Con");
		PortProto mnacport = mnac.getPort(0);
		double[] mnacbox = {-17*lambda/2, 17*lambda/2, -17*lambda/2, 17*lambda/2};
		// centers around 6 goes up by multiples of 2
	
		NodeProto mpac = tech.findNodeProto("Metal-1-P-Active-Con");
		PortProto mpacport = mpac.getPort(0);
		double[] mpacbox = {-17*lambda/2, 17*lambda/2, -17*lambda/2, 17*lambda/2};
		// centers around 6 goes up my multiple of 2

		NodeProto mnwc = tech.findNodeProto("Metal-1-N-Well-Con");
		PortProto mnwcport = mnwc.getPort(0);
		double[] mnwcbox = {-17*lambda/2, 17*lambda/2, -17*lambda/2, 17*lambda/2};
	
		NodeProto mpwc = tech.findNodeProto("Metal-1-P-Well-Con");
		PortProto mpwcport = mpwc.getPort(0);
		double[] mpwcbox = {-2*lambda, 2*lambda, -2*lambda, 2*lambda};
	
		ArcProto parc = tech.findArcProto("Polysilicon-1");
		ArcProto m1arc = tech.findArcProto("Metal-1");
		ArcProto m2arc = tech.findArcProto("Metal-2");
		ArcProto ndiffarc = tech.findArcProto("N-Active");
		ArcProto pdiffarc = tech.findArcProto("P-Active");
	
		NodeProto nwnode = tech.findNodeProto("N-Well-Node");
		double[] nwnodebox =
			{-nwnode.getDefWidth()/2-lambda/2,
			 nwnode.getDefWidth()/2+lambda/2,
			 -nwnode.getDefHeight()/2-lambda/2,
			 nwnode.getDefHeight()/2+lambda/2};
		NodeProto pwnode = tech.findNodeProto("P-Well-Node");
		double[] pwnodebox =
			{-pwnode.getDefWidth()/2-lambda/2,
			 pwnode.getDefWidth()/2+lambda/2,
			 -pwnode.getDefHeight()/2-lambda/2,
			 pwnode.getDefHeight()/2+lambda/2};
	
		// create a cell called cellname+"{lay}" in the destination library
		Cell decp = Cell.newInstance(destLib, cellname+"{lay}");
		
		NodeProto psnode = tech.findNodeProto("P-Select-Node");
		int pselectx = 8; 
		int pselecty = 8;
		double[] psnodebox =
			{-pselectx*lambda/2, pselectx*lambda/2, -pselecty*lambda/2, pselecty*lambda/2};
	
		NodeInst nwellnode =
			makeCStyleNodeInst(nwnode,0,(8*lambda*(2*bits)),
										0,8*lambda*(wordlines+1),0,0,decp);
		NodeInst pselectnode =
			makeCStyleNodeInst(psnode,0,(8*lambda*(2*bits)),
										0,8*lambda*(wordlines+1),0,0,decp);
	
		// Create instances of objects on decoder pmos plane
		x = 0;
		for (i=0; i<inputs+1; i++)
		{
			x += 8*lambda;
			y = 0;
			
			if (i < inputs)
			{
				if (top == true)
				{
					andtrans[0][i] =
						makeCStyleNodeInst(ppin, ppinbox[0]+x, ppinbox[1]+x,
												   ppinbox[2], ppinbox[3], 0, 0, decp);
					
				} else
				{    
					andtrans[0][i] =
						makeCStyleNodeInst(mpc, mpcbox[0]+x, mpcbox[1]+x,
												  mpcbox[2], mpcbox[3], 0, 0, decp);
				}
			}
			for (m=0; m<wordlines; m++)
			{
				y += 8*lambda;
				if (i < inputs)
				{
					if (romplane[m][i] == 1)
					{
						// create a transistor
						andtrans[m+1][i] =
							makeCStyleNodeInst(pmos, pmosbox[0]+x, pmosbox[1]+x,
												 pmosbox[2]+y, pmosbox[3]+y, 1, 0, decp);
					} else
					{
						andtrans[m+1][i] =
							makeCStyleNodeInst(ppin, ppinbox[0]+x, ppinbox[1]+x,
												 ppinbox[2]+y, ppinbox[3]+y, 0, 0, decp);
					}
					if (m == wordlines-1)
					{
						if (top == true)
						{
							andtrans[m+2][i] =
								makeCStyleNodeInst(mpc, mpcbox[0]+x, mpcbox[1]+x,
													 mpcbox[2]+y+16*lambda,
													 mpcbox[3]+y+16*lambda,0,0,decp);
						} else
						{
							andtrans[m+2][i] =
								makeCStyleNodeInst(ppin, ppinbox[0]+x, ppinbox[1]+x,
													 ppinbox[2]+y+4*lambda, ppinbox[3]+y+4*lambda,
													 0, 0, decp);	
						}
					}
				}
	
				boolean transcont = false;
				if (i < inputs) transcont = (romplane[m][i] == 1);
				if (i == 0)
				{
					m1m2pins[m] =
						makeCStyleNodeInst(m1m2c, m1m2cbox[0]+x-4*lambda,
											 m1m2cbox[1]+x-4*lambda, m1m2cbox[2]+y,
											 m1m2cbox[3]+y, 0, 0, decp);			
					m1m2ports[m] = m1m2cport;
				}
				if (i == (inputs))
				{
					m2pins[m] =
						makeCStyleNodeInst(m2pin, m2pinbox[0]+x-4*lambda,
											 m2pinbox[1]+x-4*lambda, m2pinbox[2]+y,
											 m2pinbox[3]+y, 0, 0, decp);			
					m2ports[m] = m2pinport;
				}
				if (i >= 1) transcont |= (romplane[m][i-1] == 1);
				if (transcont)
				{
					minpins[m][i] =
						makeCStyleNodeInst(mpac, mpacbox[0]+x-4*lambda,
											 mpacbox[1]+x-4*lambda, mpacbox[2]+y,
											 mpacbox[3]+y, 0, 0, decp);			
					minports[m][i] = mpacport;
				} else
				{
					if (i == inputs)
					{
						minpins[m][i] =
							makeCStyleNodeInst(mnwc, mnwcbox[0]+x-4*lambda,
												 mnwcbox[1]+x-4*lambda, mnwcbox[2]+y,
												 mnwcbox[3]+y, 0, 0, decp);
						minports[m][i] = mnwcport;
					} else
					{
						minpins[m][i] =
							makeCStyleNodeInst(m1pin, m1pinbox[0]+x-4*lambda,
												 m1pinbox[1]+x-4*lambda, m1pinbox[2]+y,
												 m1pinbox[3]+y, 0, 0, decp);
						minports[m][i] = m1pinport;
					}
				}
				if (i == inputs)
				{
					vddpin =
						makeCStyleNodeInst(m1pin, m1pinbox[0]+x-4*lambda,
											 m1pinbox[1]+x-4*lambda,
											 m1pinbox[2]+y+8*lambda,
											 m1pinbox[3]+y+8*lambda, 0, 0, decp);
					vddport = m1pinport;
					if (m == 0)
					{
						vddbpin =
							makeCStyleNodeInst(m1pin, m1pinbox[0]+x+4*lambda,
												 m1pinbox[1]+x+4*lambda,
												 m1pinbox[2]+y+0*lambda,
												 m1pinbox[3]+y+0*lambda, 0, 0, decp);
							vddbport = m1pinport;
					}
					if (m == wordlines-1)
					{
						vddcpin =
							makeCStyleNodeInst(m1m2c, m1m2cbox[0]+x+4*lambda,
												 m1m2cbox[1]+x+4*lambda,
												 m1m2cbox[2]+y+8*lambda,
												 m1m2cbox[3]+y+8*lambda, 0, 0, decp);	
						vddcport = m1m2cport;
					}
				}
			}
		}
	
		// connect polysilicon gates
		for (i=0; i<inputs; i++)
		{
			ap1 = andtrans[wordlines+1][i];
			if (top == true)
			{
				apport1 = mpcport;
			} else
			{
				apport1 = ppinport;
			}
			appos1 = getCStylePortPosition(ap1, apport1); 
			if (i%2 == 0)
			{
				makeCStyleExport(decp, ap1, apport1, "top_in"+(i/2), PortCharacteristic.IN);
			} else
			{
				makeCStyleExport(decp, ap1, apport1, "top_in"+((i-1)/2)+"_b", PortCharacteristic.IN);
			}
			ap1 = andtrans[0][i];
			if (top == true)
			{
				apport1 = ppinport;
			} else
			{
				apport1 = mpcport;
			}
			appos1 = getCStylePortPosition(ap1, apport1); 
			if (i%2 == 0)
			{
				makeCStyleExport(decp, ap1, apport1, "bot_in"+(i/2), PortCharacteristic.IN);
			} else
			{
				makeCStyleExport(decp, ap1, apport1, "bot_in"+((i-1)/2)+"_b", PortCharacteristic.IN);
			}
			for (m=1; m<wordlines+1; m++)
			{
				ap2 = andtrans[m][i];
				if (romplane[m-1][i] == 1)
				{
					apport2 = pmosg1port;
					apport3 = pmosg2port;
				} else
				{
					apport2 = ppinport;
					apport3 = ppinport;
				}
				appos2 = getCStylePortPosition(ap2, apport2);
				appos3 = getCStylePortPosition(ap2, apport3);
				makeCStyleArcInst(parc,2*lambda,ap1,apport1,appos1[0],
									appos1[1],ap2,apport2,appos2[0], appos2[1]);
				ap1 = ap2;
				apport1 = apport3;
				appos1 = appos3;
			}
			
			ap2 = andtrans[wordlines+1][i];
			if (top == true)
			{
				apport2 = mpcport;
				apport3 = mpcport;
			} else
			{
				apport2 = ppinport;
				apport3 = ppinport;
			}
			appos2 = getCStylePortPosition(ap2, apport2);
			appos3 = getCStylePortPosition(ap2, apport3);
			makeCStyleArcInst(parc,2*lambda,ap1,apport1,appos1[0],
								appos1[1],ap2,apport2,appos2[0], appos2[1]);
		}
	
		// connect m1 wordline lines
		for (m=0; m<wordlines; m++)
		{
			ap1 = minpins[m][0];
			apport1 = minports[m][0];
			appos1 = getCStylePortPosition(ap1, apport1); 
			for (i=1; i<inputs+1; i++)
			{
				ap2 = minpins[m][i];
				apport2 = minports[m][i];
				appos2 = getCStylePortPosition(ap2, apport2); 
				if (romplane[m][i-1] != 1)
				{
					makeCStyleArcInst(m1arc,4*lambda,ap1,apport1,appos1[0],
										appos1[1],ap2,apport2, appos2[0], appos2[1]);
				}
				ap1 = ap2;
				apport1 = apport2;
				appos1 = appos2;
			}
		}
	
		// connect transistors to wordline lines
		for (m=0; m<wordlines; m++)
		{
			for (i=0; i<inputs; i++)
			{
				if (romplane[m][i] == 1)
				{
					ap1 = andtrans[m+1][i];
					apport1 = pmosd1port;
					apport2 = pmosd2port;
					appos1 = getCStylePortPosition(ap1, apport1);
					appos2 = getCStylePortPosition(ap1, apport2);
					apx = minpins[m][i];
					apy = minpins[m][i+1];
					apportx = mpacport;
					apporty = mpacport;
					apposx = getCStylePortPosition(apx, apportx);
					apposy = getCStylePortPosition(apy, apporty);
					// pdiffarc size centers around 12 and goes up by multiples of 2
					makeCStyleArcInst(pdiffarc,16*lambda,ap1,apport1,
										appos1[0], appos1[1],apx,
										apportx,apposx[0],apposx[1]);
					makeCStyleArcInst(pdiffarc,16*lambda,ap1,apport2,
										appos2[0], appos2[1],apy,
										apporty,apposy[0],apposy[1]);
				}
			}
		}
	
		// connect ground lines
		i = inputs;
		ap1 = minpins[0][i];
		apport1 = minports[0][i];
		appos1 = getCStylePortPosition(ap1, apport1); 
		for (m=1; m<wordlines; m++)
		{
			ap2 = minpins[m][i];
			apport2 = minports[m][i];
			appos2 = getCStylePortPosition(ap2, apport2); 
			makeCStyleArcInst(m1arc,4*lambda,ap1,apport1,appos1[0],
								appos1[1],ap2,apport2,appos2[0], appos2[1]);
			ap1 = ap2;
			apport1 = apport2;
			appos1 = appos2;
		}
	
		ap1 = vddpin;
		apport1 = vddport;
		appos1 = getCStylePortPosition(ap1, apport1); 
		ap2 = minpins[wordlines-1][inputs];
		apport2 = minports[wordlines-1][inputs];
		appos2 = getCStylePortPosition(ap2, apport2); 
		makeCStyleArcInst(m1arc,4*lambda,ap1,apport1,appos1[0],
							appos1[1],ap2,apport2,appos2[0], appos2[1]);
		
		ap1 = vddcpin;
		apport1 = vddcport;
		appos1 = getCStylePortPosition(ap1, apport1);
		ap2 = vddpin;
		apport2 = vddport;
		appos2 = getCStylePortPosition(ap2, apport2); 
		makeCStyleArcInst(m1arc,4*lambda,ap1,apport1,appos1[0],
							appos1[1],ap2,apport2,appos2[0], appos2[1]);
		makeCStyleExport(decp, ap1, apport1, "vdd", PortCharacteristic.PWR);
	
		ap1 = vddbpin;
		apport1 = vddbport;
		appos1 = getCStylePortPosition(ap1, apport1); 
		ap2 = minpins[0][inputs];
		apport2 = minports[0][inputs];
		appos2 = getCStylePortPosition(ap2, apport2); 
		makeCStyleArcInst(m1arc,4*lambda,ap1,apport1,appos1[0],
							appos1[1],ap2,apport2,appos2[0], appos2[1]);
		makeCStyleExport(decp, ap1, apport1, "vddb", PortCharacteristic.PWR);
		
		// connect metal 2 lines
		for (m=0; m<wordlines; m++)
		{
			ap1 = m1m2pins[m];
			apport1 = m1m2ports[m];
			appos1 = getCStylePortPosition(ap1,apport1);
			ap2 = m2pins[m];
			apport2 = m2ports[m];
			appos2 = getCStylePortPosition(ap2, apport2); 
			ap3 = minpins[m][0];
			apport3 = minports[m][0];
			appos3 = getCStylePortPosition(ap3, apport3);
			makeCStyleArcInst(m2arc,4*lambda,ap1,apport1,appos1[0],
								appos1[1],ap2,apport2,appos2[0], appos2[1]);
			makeCStyleArcInst(m1arc,4*lambda,ap1,apport1,appos1[0],
								appos1[1],ap3,apport3,appos3[0], appos3[1]);
			makeCStyleExport(decp, ap2, apport2, "word"+m, PortCharacteristic.OUT);
			makeCStyleExport(decp, ap1, apport1, "wordin"+m, PortCharacteristic.IN);
		}
	}

	/**
	 */
	private static int[][] generatemuxarray(int folds, int romoutputs)
	{
		int muxes = romoutputs/folds;
		int[][] muxarray = new int[romoutputs][folds];
		for (int i=0; i<muxes; i++)
		{
			for (int j=0; j<folds; j++)
			{
				for (int k=0; k<folds; k++)
				{
					if (j == k)
					{
						muxarray[i*folds+j][k] = 1;
					} else
					{
						muxarray[i*folds+j][k] = 0;
					}
				}
			}
		}
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<romoutputs; i++)
		{
			sb = new StringBuffer();
			for (int j=0; j<folds; j++)
			{
				sb.append(Integer.toString(muxarray[i][j]));
			}
		}
		return muxarray;
	}
	
	
	/**
	 */
	private static void muxplane(Library destLib, double lambda, int folds, int romoutputs, String mp)
	{
		int[][] muxarray = generatemuxarray(folds,romoutputs);
		int muxnumber = folds;
		int selects = folds;
		int outputbits = romoutputs;
	
		int i, m, o;
		double x, y;
		NodeInst ap1, ap2, ap3, apx, apy;
		PortProto apport1, apport2, apport3, apportx, apporty;
		double[] appos1, appos2, appos3, apposx, apposy;
	
		NodeInst[][] ntrans = new NodeInst[outputbits+2][selects+2];
		NodeInst[][] minpins = new NodeInst[outputbits+2][selects+2];
		NodeInst[] m1m2pins2 = new NodeInst[outputbits+2];
		NodeInst[] m1m2pins = new NodeInst[selects+2];
		NodeInst[] m2pins = new NodeInst[outputbits+2];
		NodeInst[] m1polypins = new NodeInst[outputbits+2];
	
		PortProto[][] minports = new PortProto[outputbits+2][selects+2];
		PortProto[] m1m2ports2 = new PortProto[outputbits+2];
		PortProto[] m1m2ports = new PortProto[selects+2];
		PortProto[] m2ports = new PortProto[outputbits+2];
		PortProto[] m1polyports = new PortProto[outputbits+2];
		
		// get pointers to primitives
		NodeProto nmos = tech.findNodeProto("N-Transistor");
		PortProto nmosg1port = nmos.findPortProto("n-trans-poly-right");
		PortProto nmosg2port = nmos.findPortProto("n-trans-poly-left");
		PortProto nmosd1port = nmos.findPortProto("n-trans-diff-top");
		PortProto nmosd2port = nmos.findPortProto("n-trans-diff-bottom");
		double[] nmosbox = {-nmos.getDefWidth()/2-lambda/2,
						 nmos.getDefWidth()/2+lambda/2,
						 -nmos.getDefHeight()/2,
						 nmos.getDefHeight()/2};
					
		NodeProto pmos = tech.findNodeProto("P-Transistor");
		PortProto pmosg1port = pmos.findPortProto("p-trans-poly-right");
		PortProto pmosg2port = pmos.findPortProto("p-trans-poly-left");
		PortProto pmosd1port = pmos.findPortProto("p-trans-diff-top");
		PortProto pmosd2port = pmos.findPortProto("p-trans-diff-bottom");
		double[] pmosbox = {-nmos.getDefWidth()/2-lambda/2,
						 nmos.getDefWidth()/2+lambda/2,
						 -nmos.getDefHeight()/2,
						 nmos.getDefHeight()/2};
	
		NodeProto ppin = tech.findNodeProto("Polysilicon-1-Pin");
		PortProto ppinport = ppin.getPort(0);
		double[] ppinbox = {-ppin.getDefWidth()/2,
						 ppin.getDefWidth()/2,
						 -ppin.getDefHeight()/2,
						 ppin.getDefHeight()/2};
		
		NodeProto napin = tech.findNodeProto("N-Active-Pin");
		
		NodeProto m1pin = tech.findNodeProto("Metal-1-Pin");
		PortProto m1pinport = m1pin.getPort(0);
		double[] m1pinbox = {-m1pin.getDefWidth()/2-lambda/2,
						  m1pin.getDefWidth()/2+lambda/2,
						  -m1pin.getDefHeight()/2-lambda/2,
						  m1pin.getDefHeight()/2+lambda/2};
		
		NodeProto m2pin = tech.findNodeProto("Metal-2-Pin");
		PortProto m2pinport = m2pin.getPort(0);
		double[] m2pinbox = {-m1pin.getDefWidth()/2-lambda/2,
						  m1pin.getDefWidth()/2+lambda/2,
						  -m1pin.getDefHeight()/2-lambda/2,
						  m1pin.getDefHeight()/2+lambda/2};
	
		NodeProto diffpin = tech.findNodeProto("Active-Pin");
		PortProto diffpinport = diffpin.getPort(0);
		double[] diffpinbox =
			{-diffpin.getDefWidth()/2-lambda/2,
			 diffpin.getDefWidth()/2+lambda/2,
			 -diffpin.getDefHeight()/2-lambda/2,
			 diffpin.getDefHeight()/2+lambda/2};
		
		NodeProto m1m2c = tech.findNodeProto("Metal-1-Metal-2-Con");
		PortProto m1m2cport = m1m2c.getPort(0);
		double[] m1m2cbox = {-5*lambda/2, 5*lambda/2, -5*lambda/2, 5*lambda/2}; 
		
		NodeProto mnac = tech.findNodeProto("Metal-1-N-Active-Con");
		PortProto mnacport = mnac.getPort(0);
		double mult = 17;
		double[] mnacbox = {-1*mult*lambda/2, mult*lambda/2, -1*mult*lambda/2, mult*lambda/2};
		
		NodeProto mpac = tech.findNodeProto("Metal-1-N-Active-Con");
		
		NodeProto mpwc = tech.findNodeProto("Metal-1-P-Well-Con");
		PortProto mpwcport = mpwc.getPort(0);
		double[] mpwcbox = {-2*lambda, 2*lambda, -2*lambda, 2*lambda};
	
		NodeProto mpc = tech.findNodeProto("Metal-1-Polysilicon-1-Con");
		PortProto mpcport = mpc.getPort(0);
		double mx = 5;
		double[] mpcbox = {-mx*lambda/2, mx*lambda/2, -mx*lambda/2, mx*lambda/2}; 
	
		ArcProto parc = tech.findArcProto("Polysilicon-1");
		ArcProto m1arc = tech.findArcProto("Metal-1");
		ArcProto m2arc = tech.findArcProto("Metal-2");
		ArcProto ndiffarc = tech.findArcProto("N-Active");
	
		NodeProto pwnode = tech.findNodeProto("P-Well-Node");
		double[] pwnodebox =
			{-pwnode.getDefWidth()/2-lambda/2,
			 pwnode.getDefWidth()/2+lambda/2,
			 -pwnode.getDefHeight()/2-lambda/2,
			 pwnode.getDefHeight()/2+lambda/2};

		// create a cell called "muxplane{lay}" in the destination library
		Cell muxplane = Cell.newInstance(destLib, mp+"{lay}");
	
		NodeInst pwellnode =
			makeCStyleNodeInst(pwnode,-8*lambda,lambda*8*(folds+1),
										-8*lambda,8*lambda*3*romoutputs/2,0,0,muxplane);
	
		// Create instances of objects in mux plane
		x = 0;
		for (i=0; i<selects+1; i++)
		{
			x += 8*lambda;
			y = 0;
			if (i < selects)
			{  
				ntrans[0][i] =
					makeCStyleNodeInst(ppin, ppinbox[0]+x, ppinbox[1]+x, ppinbox[2],
										 ppinbox[3], 0, 0, muxplane);
				m1polypins[i] =
					makeCStyleNodeInst(mpc, mpcbox[0]+x, mpcbox[1]+x, mpcbox[2],
										 mpcbox[3], 0, 0, muxplane);
				m1m2pins[i] =
					makeCStyleNodeInst(m1m2c, m1m2cbox[0]+x, m1m2cbox[1]+x, m1m2cbox[2],
										 m1m2cbox[3], 0, 0, muxplane);
				ap1 = m1m2pins[i];
				apport1 = m1m2cport;
				makeCStyleExport(muxplane, ap1, apport1, "sel"+(selects-i-1), PortCharacteristic.IN);
			}
	
			for (m=0; m<outputbits; m++)
			{
				y += 8*lambda;
				if (m%2 == 1)
				{
					y += 8*lambda;
				}
				if (i < selects)
				{
					if (muxarray[m][i] == 1)
					{
						// create a transistor
						ntrans[m+1][i] =
							makeCStyleNodeInst(nmos, nmosbox[0]+x, nmosbox[1]+x,
												 nmosbox[2]+y, nmosbox[3]+y,1,0,muxplane);
					} else
					{
						ntrans[m+1][i] =
							makeCStyleNodeInst(ppin, ppinbox[0]+x, ppinbox[1]+x,
												 ppinbox[2]+y,ppinbox[3]+y,0,0,muxplane);
					}
				}
				boolean transcont = false;
				if (i < selects) transcont = (muxarray[m][i] == 1);
				if (i == (selects))
				{
					m1m2pins2[m] =
						makeCStyleNodeInst(m1m2c, m1m2cbox[0]+x-4*lambda,
											 m1m2cbox[1]+x-4*lambda, m1m2cbox[2]+y,
											 m1m2cbox[3]+y, 0, 0, muxplane);			
					m1m2ports2[m] = m1m2cport;		
				}
				if (i >= 1) transcont |= (muxarray[m][i-1] == 1);
				if (transcont)
				{
					minpins[m][i] =
						makeCStyleNodeInst(mnac, mnacbox[0]+x-4*lambda,
											 mnacbox[1]+x-4*lambda, mnacbox[2]+y,
											 mnacbox[3]+y, 0, 0, muxplane);
					minports[m][i] = mnacport;
				} else
				{
					minpins[m][i] =
						makeCStyleNodeInst(m1pin, m1pinbox[0]+x-4*lambda,
											 m1pinbox[1]+x-4*lambda, m1pinbox[2]+y,
											 m1pinbox[3]+y, 0, 0, muxplane);
					minports[m][i] = m1pinport;
				}
			}
		}
	
		// finished placing objects, now wire arcs
	
		// connect polysilicon gates
		for (i=0; i<selects; i++)
		{
			ap1 = ntrans[0][i];
			apport1 = ppinport;
			appos1 = getCStylePortPosition(ap1, apport1); 
			ap2 = m1polypins[i];
			apport2 = mpcport;
			appos2 = getCStylePortPosition(ap2, apport2); 
			ap3 = m1m2pins[i];
			apport3 = m1m2cport;
			appos3 = getCStylePortPosition(ap3, apport3); 
			makeCStyleArcInst(parc, 2*lambda, ap1, apport1, appos1[0],
								appos1[1],ap2, apport2, appos2[0], appos2[1]);
			makeCStyleArcInst(m1arc, 4*lambda, ap2, apport2, appos2[0],
								appos2[1],ap3, apport3, appos3[0], appos3[1]);
		}
		
		// connect polysilicon gates
		for (i=0; i<selects; i++)
		{
			ap1 = ntrans[0][i];
			apport1 = ppinport;
			appos1 = getCStylePortPosition(ap1, apport1); 
			for (m=1; m<outputbits+1; m++)
			{
				ap2 = ntrans[m][i];
				if (muxarray[m-1][i] == 1)
				{
					apport2 = nmosg1port;
					apport3 = nmosg2port;
				} else
				{
					apport2 = ppinport;
					apport3 = ppinport;
				}
				appos2 = getCStylePortPosition(ap2, apport2);
				appos3 = getCStylePortPosition(ap2, apport3);
				makeCStyleArcInst(parc, 2*lambda, ap1, apport1, appos1[0],
									appos1[1], ap2, apport2, appos2[0], appos2[1]);
				ap1 = ap2;
				apport1 = apport3;
				appos1 = appos3;
			}
		}
	
		// connect m1 wordline lines
		for (m=0; m<outputbits; m++)
		{
			ap1 = minpins[m][0];
			apport1 = minports[m][0];
			appos1 = getCStylePortPosition(ap1, apport1); 
			for (i=1; i<selects+1; i++)
			{
				ap2 = minpins[m][i];
				apport2 = minports[m][i];
				appos2 = getCStylePortPosition(ap2, apport2); 
				if (muxarray[m][i-1] != 1)
				{
					makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1,
										appos1[0], appos1[1],
										ap2, apport2, appos2[0], appos2[1]);
				}
				if (i == 1)
				{
					makeCStyleExport(muxplane, ap1, apport1, "muxin"+m, PortCharacteristic.IN);
				}
				ap1 = ap2;
				apport1 = apport2;
				appos1 = appos2;
			}
		}

		// connect transistors to wordline lines
		for (m=0; m<outputbits; m++)
		{
			for (i=0; i<selects; i++)
			{
				if (muxarray[m][i] == 1)
				{
					// connect transistor
					ap1 = ntrans[m+1][i];
					apport1 = nmosd1port;
					apport2 = nmosd2port;
					appos1 = getCStylePortPosition(ap1, apport1);
					appos2 = getCStylePortPosition(ap1, apport2);
					apx = minpins[m][i];
					apy = minpins[m][i+1];
					apportx = mnacport;
					apporty = mnacport;
					apposx = getCStylePortPosition(apx, apportx);
					apposy = getCStylePortPosition(apy, apporty);
					makeCStyleArcInst(ndiffarc, 16*lambda, ap1, apport1,
										appos1[0], appos1[1],
										apx, apportx, apposx[0], apposx[1]);
					makeCStyleArcInst(ndiffarc, 16*lambda, ap1, apport2,
										appos2[0], appos2[1],
										apy, apporty, apposy[0], apposy[1]);
				}
			}
		}
	
		for(int j = 0 ; j < outputbits; j++)
		{
			i = selects;
			ap1 = minpins[j][i];
			apport1 = minports[j][i];
			appos1 = getCStylePortPosition(ap1, apport1); 
			ap2 = m1m2pins2[j];
			apport2 = m1m2ports2[j];
			appos2 = getCStylePortPosition(ap2, apport2); 
			makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
								appos1[1], ap2, apport2, appos2[0], appos2[1]);
		}
	
		// connect mux together
		for(int j = 0 ; j < outputbits/muxnumber; j++)
		{
			ap1 = m1m2pins2[j*muxnumber];
			apport1 = m1m2ports2[j*muxnumber];
			appos1 = getCStylePortPosition(ap1, apport1); 
			for (m=1+j*muxnumber; m<muxnumber+j*muxnumber; m++) {
				ap2 = m1m2pins2[m];
				apport2 = m1m2ports2[m];
				appos2 = getCStylePortPosition(ap2, apport2); 
				makeCStyleArcInst(m2arc, 4*lambda, ap1, apport1, appos1[0],
									appos1[1],ap2, apport2, appos2[0], appos2[1]);
			}
			makeCStyleExport(muxplane, ap1, apport1, "muxout"+j, PortCharacteristic.OUT);
		}
	}
		
	/**
	 */
	private static void inverterplane(Library destLib, double lambda, int outs, int folds, String ip)
	{		
		int i, m, o;
		double x, y;
		NodeInst ap1, ap2, ap3, gnd1, gnd2, vdd1, vdd2;
		PortProto apport1, apport2, apport3, trans1port, trans2port,
						   gndport1, gndport2, vddport1, vddport2;
		double[] appos1, appos2, appos3, trans1pos, trans2pos, gndpos1, gndpos2,
				  vddpos1, vddpos2;
	
		NodeInst[] ntrans = new NodeInst[outs/folds];
		NodeInst[] ptrans = new NodeInst[outs/folds];
		NodeInst[] inpins = new NodeInst[outs/folds];
		NodeInst[] polypins = new NodeInst[outs/folds];
		NodeInst[] intppins = new NodeInst[outs/folds];
		NodeInst[] nmospins = new NodeInst[outs/folds];
		NodeInst[] pmospins = new NodeInst[outs/folds];
		NodeInst[] gndpins = new NodeInst[outs/2];
		NodeInst[] pwellpins = new NodeInst[outs/2];
		NodeInst[] vddpins = new NodeInst[outs/2];
		NodeInst[] midvddpins = new NodeInst[outs/2];
		NodeInst[] gndpins2 = new NodeInst[outs/folds];
		NodeInst[] pwellpins2 = new NodeInst[outs/folds];
		NodeInst[] vddpins2 = new NodeInst[outs/folds];
		NodeInst[] midvddpins2 = new NodeInst[outs/folds];
		NodeInst gndc = null;
		NodeInst nwellc = null;
		NodeInst nwellm = null;
		
		PortProto[] ntransports = new PortProto[outs/folds];
		PortProto[] ptransports = new PortProto[outs/folds];
		PortProto[] inports = new PortProto[outs/folds];
		PortProto[] polyports = new PortProto[outs/folds];
		PortProto[] intpports = new PortProto[outs/folds];
		PortProto[] nmosports = new PortProto[outs/folds];
		PortProto[] pmosports = new PortProto[outs/folds];
		PortProto[] pwellports = new PortProto[outs/2];
		PortProto[] gndports = new PortProto[outs/2];
		PortProto[] vddports = new PortProto[outs/2];
		PortProto[] midvddports = new PortProto[outs/2];
		PortProto[] pwellports2 = new PortProto[outs/folds];
		PortProto[] gndports2 = new PortProto[outs/folds];
		PortProto[] vddports2 = new PortProto[outs/folds];
		PortProto[] midvddports2 = new PortProto[outs/folds];
		PortProto gndcport = null;
		PortProto nwellcport = null;
		PortProto nwellmport = null;

		// get pointers to primitives
		NodeProto nmos = tech.findNodeProto("N-Transistor");
		PortProto nmosg1port = nmos.findPortProto("n-trans-poly-right");
		PortProto nmosg2port = nmos.findPortProto("n-trans-poly-left");
		PortProto nmosd1port = nmos.findPortProto("n-trans-diff-top");
		PortProto nmosd2port = nmos.findPortProto("n-trans-diff-bottom");
		double[] nmosbox = {-nmos.getDefWidth()/2-lambda/2,
						 nmos.getDefWidth()/2+lambda/2,
						 -nmos.getDefHeight()/2,
						 nmos.getDefHeight()/2};
					
		NodeProto pmos = tech.findNodeProto("P-Transistor");
		PortProto pmosg1port = pmos.findPortProto("p-trans-poly-right");
		PortProto pmosg2port = pmos.findPortProto("p-trans-poly-left");
		PortProto pmosd1port = pmos.findPortProto("p-trans-diff-top");
		PortProto pmosd2port = pmos.findPortProto("p-trans-diff-bottom");
		double[] pmosbox = {-pmos.getDefWidth()/2-lambda/2,
						 pmos.getDefWidth()/2+lambda/2,
						 -pmos.getDefHeight()/2,
						 pmos.getDefHeight()/2};
	
		NodeProto ppin = tech.findNodeProto("Polysilicon-1-Pin");
		PortProto ppinport = ppin.getPort(0);
		double[] ppinbox = {-ppin.getDefWidth()/2,
						 ppin.getDefWidth()/2,
						 -ppin.getDefHeight()/2,
						 ppin.getDefHeight()/2};
		
		NodeProto napin = tech.findNodeProto("N-Active-Pin");
		
		NodeProto m1pin = tech.findNodeProto("Metal-1-Pin");
		PortProto m1pinport = m1pin.getPort(0);
		double[] m1pinbox = {-m1pin.getDefWidth()/2-lambda/2,
						  m1pin.getDefWidth()/2+lambda/2,
						  -m1pin.getDefHeight()/2-lambda/2,
						  m1pin.getDefHeight()/2+lambda/2};
		
		NodeProto m2pin = tech.findNodeProto("Metal-2-Pin");
		PortProto m2pinport = m2pin.getPort(0);
		double[] m2pinbox = {-m1pin.getDefWidth()/2-lambda/2,
						  m1pin.getDefWidth()/2+lambda/2,
						  -m1pin.getDefHeight()/2-lambda/2,
						  m1pin.getDefHeight()/2+lambda/2};
	
		NodeProto nwnode = tech.findNodeProto("N-Well-Node");
		double[] nwnodebox =
			{-nwnode.getDefWidth()/2-lambda/2,
			 nwnode.getDefWidth()/2+lambda/2,
			 -nwnode.getDefHeight()/2-lambda/2,
			 nwnode.getDefHeight()/2+lambda/2};
		
		NodeProto pwnode = tech.findNodeProto("P-Well-Node");
		double[] pwnodebox =
			{-pwnode.getDefWidth()/2-lambda/2,
			 pwnode.getDefWidth()/2+lambda/2,
			 -pwnode.getDefHeight()/2-lambda/2,
			 pwnode.getDefHeight()/2+lambda/2};
		
		NodeProto m1m2c = tech.findNodeProto("Metal-1-Metal-2-Con");
		PortProto m1m2cport = m1m2c.getPort(0);
		double[] m1m2cbox = {-5*lambda/2, 5*lambda/2, -5*lambda/2, 5*lambda/2}; 
	
		NodeProto diffpin = tech.findNodeProto("Active-Pin");
		PortProto diffpinport = diffpin.getPort(0);
		double[] diffpinbox = 
			{-diffpin.getDefWidth()/2-lambda/2,
			 diffpin.getDefWidth()/2+lambda/2,
			 -diffpin.getDefHeight()/2-lambda/2,
			 diffpin.getDefHeight()/2+lambda/2};
	
		NodeProto mnac = tech.findNodeProto("Metal-1-N-Active-Con");
		PortProto mnacport = mnac.getPort(0);
		double aaa = 17;
		double[] mnacbox = {-aaa*lambda/2, aaa*lambda/2, -aaa*lambda/2, aaa*lambda/2};
	
		NodeProto mpac = tech.findNodeProto("Metal-1-P-Active-Con");
		PortProto mpacport = mpac.getPort(0);
		double[] mpacbox = {-aaa*lambda/2, aaa*lambda/2, -aaa*lambda/2, aaa*lambda/2};   //
	
		NodeProto mpc = tech.findNodeProto("Metal-1-Polysilicon-1-Con");
		PortProto mpcport = mpc.getPort(0);
		double mx = -7;
		double my = 5;
		double[] mpcbox = {-mx*lambda/2, mx*lambda/2, -my*lambda/2, my*lambda/2}; 
	
		NodeProto mpwc = tech.findNodeProto("Metal-1-P-Well-Con");
		PortProto mpwcport = mpwc.getPort(0);
		double[] mpwcbox = {-17*lambda/2, 17*lambda/2, -17*lambda/2, 17*lambda/2};
		
		NodeProto mnwc = tech.findNodeProto("Metal-1-N-Well-Con");
		PortProto mnwcport = mnwc.getPort(0);
		double[] mnwcbox = {-17*lambda/2, 17*lambda/2, -17*lambda/2, 17*lambda/2};
	
		ArcProto parc = tech.findArcProto("Polysilicon-1");
		ArcProto m1arc = tech.findArcProto("Metal-1");
		ArcProto m2arc = tech.findArcProto("Metal-2");
		ArcProto ndiffarc = tech.findArcProto("N-Active");
		ArcProto pdiffarc = tech.findArcProto("P-Active");
	
		// create a cell called "inverterplane{lay}" in the destination library
		Cell invp = Cell.newInstance(destLib, ip+"{lay}");
	
		NodeInst pwellnode =
			makeCStyleNodeInst(pwnode,-32*lambda,(3*lambda*8*outs/2)+8*lambda,
										-18*lambda,16*lambda,0,0,invp);
	
		NodeInst nwellnode =
			makeCStyleNodeInst(nwnode,-32*lambda,(3*lambda*8*outs/2)+8*lambda,
										-34*lambda,-18*lambda,0,0,invp);
	
		// Create instances of objects on inverter plane
		x = 0*lambda;
		for (i=0; i<outs; i++)
		{
			x += 8*lambda;
			y = 0;
			if (folds > 1)
			{
				if (i % folds == 1)
				{
					pwellpins2[i/folds] =
						makeCStyleNodeInst(mpwc, mpwcbox[0]+x, mpwcbox[1]+x,
											 mpwcbox[2]+y, mpwcbox[3]+y, 0, 0, invp);
					pwellports2[i/folds] = mpwcport;
					gndpins2[i/folds] =
						makeCStyleNodeInst(mnac, mnacbox[0]+x, mnacbox[1]+x,
											 mnacbox[2]+y-10*lambda,
											 mnacbox[3]+y-10*lambda, 0, 0, invp);
					gndports2[i/folds] = mnacport;
					midvddpins2[i/folds] =
						makeCStyleNodeInst(mpac, mpacbox[0]+x, mpacbox[1]+x,
											 mpacbox[2]+y-26*lambda,
											 mpacbox[3]+y-26*lambda, 0, 0, invp);
					midvddports2[i/folds] = mpacport;
					vddpins2[i/folds] =
						makeCStyleNodeInst(m1m2c, m1m2cbox[0]+x, m1m2cbox[1]+x,
											 m1m2cbox[2]+y-26*lambda,
											 m1m2cbox[3]+y-26*lambda, 0, 0, invp);
					vddports2[i/folds] = m1m2cport;
					if (i == 1)
					{
						gndc =
							makeCStyleNodeInst(m1m2c, m1m2cbox[0]+x, m1m2cbox[1]+x,
												 m1m2cbox[2]+y-10*lambda,
												 m1m2cbox[3]+y-10*lambda, 0, 0, invp);
						gndcport = m1m2cport;
						makeCStyleExport(invp, gndc, gndcport, "gnd", PortCharacteristic.GND);
					}
				}
			} else
			{
				if (i%2 == 1)
				{
					// place gnd, intvdd, vddpins
	//				pwellpins[i/2] =
	//					makeCStyleNodeInst(mpwc, mpwcbox[0]+x, mpwcbox[1]+x,
	//										 mpwcbox[2]+y-10*lambda,
	//										 mpwcbox[3]+y-10*lambda, 0, 0, invp);
	//				pwellports[i/2] = mpwcport;
					gndpins[i/2] =
						makeCStyleNodeInst(mnac, mnacbox[0]+x, mnacbox[1]+x,
											 mnacbox[2]+y-10*lambda,
											 mnacbox[3]+y-10*lambda, 0, 0, invp);
					gndports[i/2] = mnacport;
					midvddpins[i/2] =
						makeCStyleNodeInst(mpac, mpacbox[0]+x, mpacbox[1]+x,
											 mpacbox[2]+y-26*lambda,
											 mpacbox[3]+y-26*lambda, 0, 0, invp);
					midvddports[i/2] = mpacport;
					vddpins[i/2] =
						makeCStyleNodeInst(m1m2c, m1m2cbox[0]+x, m1m2cbox[1]+x,
											 m1m2cbox[2]+y-26*lambda,
											 m1m2cbox[3]+y-26*lambda, 0, 0, invp);
					vddports[i/2] = m1m2cport;
				
					if (i == 1)
					{
						nwellc =
							makeCStyleNodeInst(mnwc, mnwcbox[0]+x-24*lambda,
												 mnwcbox[1]+x-24*lambda,
												 mnwcbox[2]+y-26*lambda,
											 	 mnwcbox[3]+y-26*lambda, 0, 0, invp); 
						nwellcport = mnwcport;
	
						nwellm =
							makeCStyleNodeInst(m1m2c, m1m2cbox[0]+x-24*lambda,
												 m1m2cbox[1]+x-24*lambda,
												 m1m2cbox[2]+y-26*lambda,
												 m1m2cbox[3]+y-26*lambda, 0, 0, invp);
						nwellmport = m1m2cport;
	
						gndc =
							makeCStyleNodeInst(m1m2c, m1m2cbox[0]+x, m1m2cbox[1]+x,
												 m1m2cbox[2]+y-10*lambda,
												 m1m2cbox[3]+y-10*lambda, 0, 0, invp);
						gndcport = m1m2cport;
						makeCStyleExport(invp, gndc, gndcport, "gnd", PortCharacteristic.GND);
					}
				}
			}
			if (i%2 == 1)
			{
				x += 8*lambda;
			}
			if (folds > 1)
			{
				if (i%folds == 0)
				{
					inpins[i/folds] =
						makeCStyleNodeInst(mpc, mpcbox[0]+x-6*lambda,
											 mpacbox[1]+x-6*lambda, mpcbox[2]+y,
											 mpcbox[3]+y, 0, 0, invp);
					inports[i/folds] = mpcport;
					polypins[i/folds] =
						makeCStyleNodeInst(ppin, ppinbox[0]+x, ppinbox[1]+x,
											 ppinbox[2]+y-6*lambda, ppinbox[3]+y-6*lambda,
											 0, 0, invp);
					polyports[i/folds] = ppinport;
					nmospins[i/folds] =
						makeCStyleNodeInst(mnac, mnacbox[0]+x, mnacbox[1]+x,
											 mnacbox[2]+y-10*lambda, mnacbox[3]+y-10*lambda,
											 0, 0, invp);
					nmosports[i/folds] = mnacport;
					pmospins[i/folds] =
						makeCStyleNodeInst(mpac, mpacbox[0]+x, mpacbox[1]+x,
											 mpacbox[2]+y-26*lambda, mpacbox[3]+y-26*lambda,
											 0, 0, invp);
					pmosports[i/folds] = mpacport; 
				}
			} else
			{
				// place inpins, polypins, nmospins, pmospins
				inpins[i] =
					makeCStyleNodeInst(mpc, mpcbox[0]+x-6*lambda, mpacbox[1]+x-6*lambda,
										 mpcbox[2]+y, mpcbox[3]+y, 0, 0, invp);
				inports[i] = mpcport;
				polypins[i] =
					makeCStyleNodeInst(ppin, ppinbox[0]+x, ppinbox[1]+x,
										 ppinbox[2]+y-6*lambda, ppinbox[3]+y-6*lambda,
										 0, 0, invp);
				polyports[i] = ppinport;
				nmospins[i] =
					makeCStyleNodeInst(mnac, mnacbox[0]+x, mnacbox[1]+x,
										 mnacbox[2]+y-10*lambda, mnacbox[3]+y-10*lambda,
										 0, 0, invp);
				nmosports[i] = mnacport;
				pmospins[i] =
					makeCStyleNodeInst(mpac, mpacbox[0]+x, mpacbox[1]+x,
										 mpacbox[2]+y-26*lambda, mpacbox[3]+y-26*lambda,
										 0, 0, invp);
				pmosports[i] = mpacport;
			}
			
			if (folds > 1)
			{
				if (i%folds == 0)
				{
					double off = 4*lambda;
					ptrans[i/folds] =
						makeCStyleNodeInst(pmos, pmosbox[0]+x+off, pmosbox[1]+x+off,
											 pmosbox[2]+y-26*lambda,
											 pmosbox[3]+y-26*lambda, 1, 0, invp);
					ntrans[i/folds] =
						makeCStyleNodeInst(nmos, nmosbox[0]+x+off, nmosbox[1]+x+off,
											 nmosbox[2]+y-10*lambda,
											 nmosbox[3]+y-10*lambda, 1, 0, invp);
					intppins[i/folds] =
						makeCStyleNodeInst(ppin, ppinbox[0]+x+off, ppinbox[1]+x+off,
											 ppinbox[2]+y-6*lambda, ppinbox[3]+y-6*lambda,
											 0, 0, invp);
					intpports[i/folds] = ppinport;
				}
			} else
			{
				double off = 0;
				if (i%2 == 0)
				{
					off = 4*lambda;
				} else
				{
					off = -4*lambda;
				}
				ptrans[i] =
					makeCStyleNodeInst(pmos, pmosbox[0]+x+off, pmosbox[1]+x+off,
										 pmosbox[2]+y-26*lambda, pmosbox[3]+y-26*lambda,
										 1, 0, invp);
				ntrans[i] =
					makeCStyleNodeInst(nmos, nmosbox[0]+x+off, nmosbox[1]+x+off,
										 nmosbox[2]+y-10*lambda, nmosbox[3]+y-10*lambda,
										 1, 0, invp);
				intppins[i] =
					makeCStyleNodeInst(ppin, ppinbox[0]+x+off, ppinbox[1]+x+off,
										 ppinbox[2]+y-6*lambda, ppinbox[3]+y-6*lambda,
										 0, 0, invp);
				intpports[i] = ppinport;
			}
		}
	
		// connect transistors to diffusion lines
		if (folds > 1)
		{
			for (i=0; i<outs/folds; i++)
			{
				ap2 = nmospins[i];
				ap3 = gndpins2[i];
				ap1 = ntrans[i];
				apport1 = nmosd1port;
				gndport1 = nmosd2port;
				appos1 = getCStylePortPosition(ap1, apport1);
				gndpos1 = getCStylePortPosition(ap1, gndport1);
				apport2 = mnacport;
				appos2 = getCStylePortPosition(ap2, apport2);
				apport3 = mnacport;
				appos3 = getCStylePortPosition(ap3, apport3);
				// ndiffarc size centers around 12 and goes up by multiples of 2
				makeCStyleArcInst(ndiffarc, 16*lambda, ap1, apport1,
									appos1[0], appos1[1],
									ap2, apport2, appos2[0], appos2[1]);
				makeCStyleArcInst(ndiffarc, 16*lambda, ap1, gndport1,
									gndpos1[0], gndpos1[1],
									ap3, apport3, appos3[0], appos3[1]);
			}
		} else
		{
			for (i=0; i<outs; i++)
			{
				if (i%2 == 0)
				{
					ap2 = nmospins[i];
					ap3 = gndpins[i/2];
				} else
				{
					ap2 = gndpins[i/2];
					ap3 = nmospins[i];
				}
				ap1 = ntrans[i];
				apport1 = nmosd1port;
				gndport1 = nmosd2port;
				appos1 = getCStylePortPosition(ap1, apport1);
				gndpos1 = getCStylePortPosition(ap1, gndport1);
				apport2 = mnacport;
				appos2 = getCStylePortPosition(ap2, apport2);
				apport3 = mnacport;
				appos3 = getCStylePortPosition(ap3, apport3);
				makeCStyleArcInst(ndiffarc, 16*lambda, ap1, apport1,
									appos1[0], appos1[1],
									ap2, apport2, appos2[0], appos2[1]);
				makeCStyleArcInst(ndiffarc, 16*lambda, ap1, gndport1,
									gndpos1[0], gndpos1[1],
									ap3, apport3, appos3[0], appos3[1]);
			}
		}
	
		if (folds >1)
		{
			for (i=0; i<outs/folds; i++)
			{
				ap2 = pmospins[i];
				ap3 = midvddpins2[i];
				ap1 = ptrans[i];
				ap1 = ptrans[i];
				apport1 = pmosd1port;
				vddport1 = pmosd2port;
				appos1 = getCStylePortPosition(ap1, apport1);
				vddpos1 = getCStylePortPosition(ap1, vddport1);
				apport2 = mpacport;
				appos2 = getCStylePortPosition(ap2, apport2);
				apport3 = mpacport;
				appos3 = getCStylePortPosition(ap3, apport3);
				makeCStyleArcInst(pdiffarc, 16*lambda, ap1, apport1,
									appos1[0], appos1[1],
									ap2, apport2, appos2[0], appos2[1]);
				makeCStyleArcInst(pdiffarc, 16*lambda, ap1, vddport1,
									vddpos1[0], vddpos1[1],
									ap3, apport3, appos3[0], appos3[1]);
			}
		} else
		{
			// connect transistors to diffusion lines
			for (i=0; i<outs; i++)
			{
				if (i%2 == 0)
				{
					ap2 = pmospins[i];
					ap3 = midvddpins[i/2];
				} else
				{
					ap2 = midvddpins[i/2];
					ap3 = pmospins[i];
				}
				ap1 = ptrans[i];
				apport1 = pmosd1port;
				vddport1 = pmosd2port;
				appos1 = getCStylePortPosition(ap1, apport1);
				vddpos1 = getCStylePortPosition(ap1, vddport1);
				apport2 = mpacport;
				appos2 = getCStylePortPosition(ap2, apport2);
				apport3 = mpacport;
				appos3 = getCStylePortPosition(ap3, apport3);
				makeCStyleArcInst(pdiffarc, 16*lambda, ap1, apport1,
									appos1[0], appos1[1],
									ap2, apport2, appos2[0], appos2[1]);
				makeCStyleArcInst(pdiffarc, 16*lambda, ap1, vddport1,
									vddpos1[0], vddpos1[1],
									ap3, apport3, appos3[0], appos3[1]);
			}
		}
	
		// metal-1 mpac to mnac
		for (i=0; i<outs/folds; i++)
		{
			ap1 = nmospins[i];
			apport1 = nmosports[i];
			appos1 = getCStylePortPosition(ap1, apport1);
			ap2 = pmospins[i];
			apport2 = pmosports[i];
			appos2 = getCStylePortPosition(ap2, apport2);
			makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
								appos1[1], ap2, apport2, appos2[0], appos2[1]);
		}
	
		// poly inpins to polypins
		for (i=0; i<outs/folds; i++)
		{
			ap1 = inpins[i];
			apport1 = inports[i];
			appos1 = getCStylePortPosition(ap1, apport1);
			ap2 = polypins[i];
			apport2 = polyports[i];
			appos2 = getCStylePortPosition(ap2, apport2);
			makeCStyleArcInst(parc, 2*lambda, ap1, apport1, appos1[0],
								appos1[1], ap2, apport2, appos2[0], appos2[1]);
		}
	
		// poly polypins to intppins
		for (i=0; i<outs/folds; i++)
		{
			ap1 = polypins[i];
			apport1 = polyports[i];
			appos1 = getCStylePortPosition(ap1, apport1);
			ap2 = intppins[i];
			apport2 = intpports[i];
			appos2 = getCStylePortPosition(ap2, apport2);
			makeCStyleArcInst(parc, 2*lambda, ap1, apport1, appos1[0],
								appos1[1], ap2, apport2, appos2[0], appos2[1]);
		}
	
		// poly intppins to ntrans
		for (i=0; i<outs/folds; i++)
		{
			ap1 = intppins[i];
			apport1 = intpports[i];
			appos1 = getCStylePortPosition(ap1, apport1);
			ap2 = ntrans[i];
			apport2 = nmosg2port;
			appos2 = getCStylePortPosition(ap2, apport2);
			makeCStyleArcInst(parc, 2*lambda, ap1, apport1, appos1[0],
								appos1[1], ap2, apport2, appos2[0], appos2[1]);
		}
	
		// poly ntrans to ptrans
		for (i=0; i<outs/folds; i++)
		{
			ap1 = ntrans[i];
			apport1 = nmosg1port;
			appos1 = getCStylePortPosition(ap1, apport1);
			ap2 = ptrans[i];
			apport2 = pmosg2port;
			appos2 = getCStylePortPosition(ap2, apport2);
			makeCStyleArcInst(parc, 2*lambda, ap1, apport1, appos1[0],
								appos1[1], ap2, apport2, appos2[0], appos2[1]);
		}
	
		if (folds > 1)
		{
			for (i=0; i < outs/folds; i++)
			{
				ap1 = pwellpins2[i];
				apport1 = pwellports2[i];
				appos1 = getCStylePortPosition(ap1, apport1);
				ap2 = gndpins2[i];
				apport2 = gndports2[i];
				appos2 = getCStylePortPosition(ap2, apport2);
				makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
									appos1[1], ap2, apport2, appos2[0], appos2[1]);
			}
			for (i=0; i < outs/folds; i++)
			{
				ap1 = midvddpins2[i];
				apport1 = midvddports2[i];
				appos1 = getCStylePortPosition(ap1, apport1);
				ap2 = vddpins2[i];
				apport2 = vddports2[i];
				appos2 = getCStylePortPosition(ap2, apport2);
				makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
									appos1[1], ap2, apport2, appos2[0], appos2[1]);
			}
	
			// metal 2 vddpins
			ap1 = vddpins2[0];
			apport1 = vddports2[0];
			appos1 = getCStylePortPosition(ap1, apport1);
			for (i=1; i<outs/folds; i++)
			{
				ap2 = vddpins2[i];
				apport2 = vddports2[i];
				appos2 = getCStylePortPosition(ap2, apport2);
				makeCStyleArcInst(m2arc, 4*lambda, ap1, apport1, appos1[0],
									appos1[1], ap2, apport2, appos2[0], appos2[1]);
			}
			makeCStyleExport(invp, ap1, apport1, "vdd", PortCharacteristic.PWR);
	
			for (i=0; i<outs/folds; i++)
			{
				ap1 = gndpins2[i];
				apport1 = gndports2[i];
				makeCStyleExport(invp, ap1, apport1, "invgnd" + i, PortCharacteristic.GND);
			}
			ap1 = gndpins2[0];
			apport1 = gndports2[0];
			appos1 = getCStylePortPosition(ap1, apport1);
	
		} else
		{
			// metal 1 midvddpins to vddpins
			for (i=0; i<outs/2; i++)
			{
				ap1 = midvddpins[i];
				apport1 = midvddports[i];
				appos1 = getCStylePortPosition(ap1, apport1);
				ap2 = vddpins[i];
				apport2 = vddports[i];
				appos2 = getCStylePortPosition(ap2, apport2);
				makeCStyleArcInst(m1arc, 4*lambda, ap1, apport1, appos1[0],
									appos1[1], ap2, apport2, appos2[0], appos2[1]);
			}
	
			ap1 = vddpins[0];
			apport1 = vddports[0];
			appos1 = getCStylePortPosition(ap1, apport1);
			ap2 = nwellm;
			apport2 = nwellmport;
			appos2 = getCStylePortPosition(ap2, apport2);
			ap3 = nwellc;
			apport3 = nwellcport;
			appos3 = getCStylePortPosition(ap3, apport3);
			makeCStyleArcInst(m2arc, 4*lambda, ap1, apport1, appos1[0],
								appos1[1], ap2, apport2, appos2[0], appos2[1]);
			makeCStyleArcInst(m1arc, 4*lambda, ap3, apport3, appos3[0],
								appos3[1], ap2, apport2, appos2[0], appos2[1]);
	
			// metal 2 vddpins
			ap1 = vddpins[0];
			apport1 = vddports[0];
			appos1 = getCStylePortPosition(ap1, apport1);
			for (i=1; i<outs/2; i++)
			{
				ap2 = vddpins[i];
				apport2 = vddports[i];
				appos2 = getCStylePortPosition(ap2, apport2);
				makeCStyleArcInst(m2arc, 4*lambda, ap1, apport1, appos1[0],
									appos1[1], ap2, apport2, appos2[0], appos2[1]);
			}
			makeCStyleExport(invp, ap1, apport1, "vdd", PortCharacteristic.PWR);
	
			for (i=0; i<outs/2; i++)
			{
				ap1 = gndpins[i];
				apport1 = gndports[i];
				makeCStyleExport(invp, ap1, apport1, "invgnd" + i, PortCharacteristic.GND);
			}
	
			ap1 = gndpins[0];
			apport1 = gndports[0];
			appos1 = getCStylePortPosition(ap1, apport1);
		}
	
		ap2 = gndc;
		apport2 = gndcport;
		appos2 = getCStylePortPosition(ap2, apport2);
		makeCStyleArcInst(m1arc,4*lambda,ap1,apport1,appos1[0],
							appos1[1], ap2, apport2, appos2[0], appos2[1]);
		for (i=0; i<outs/folds; i++)
		{
			ap1 = inpins[i];
			apport1 = inports[i];
			makeCStyleExport(invp, ap1, apport1, "invin" + i, PortCharacteristic.IN);
		}
	
		for (i=0; i<outs/folds; i++)
		{
			ap1 = pmospins[i];
			apport1 = pmosports[i];
			makeCStyleExport(invp, ap1, apport1, "invout"+((outs/folds - 1) - i), PortCharacteristic.OUT);
		}
	
	}

	/**
	 */
	private static void ininverterplane(Library destLib, double lambda, int outs, String layoutname, boolean top,
						   int lengthbits)
	{	
		int i, m, o;
		double x, y;
		NodeInst ap1, ap2, ap3, gnd1, gnd2, vdd1, vdd2;
		PortProto apport1, apport2, apport3, trans1port, trans2port,
						   gndport1, gndport2, vddport1, vddport2;
		double[] appos1, appos2, appos3, trans1pos, trans2pos, gndpos1, gndpos2,
				  vddpos1, vddpos2;
	
		NodeInst[] ntrans = new NodeInst[outs];
		NodeInst[] ptrans = new NodeInst[outs];
		NodeInst[] inpins = new NodeInst[outs];
		NodeInst[] inpins2 = new NodeInst[outs];
		NodeInst[] outpins = new NodeInst[outs];
		NodeInst[] outpins2 = new NodeInst[outs];
		NodeInst[] polypins = new NodeInst[outs];
		NodeInst[] intppins = new NodeInst[outs];
		NodeInst[] polypins2 = new NodeInst[outs];
		NodeInst[] intppins2 = new NodeInst[outs];
		NodeInst[] gndpins = new NodeInst[outs];
		NodeInst[] midvddpins = new NodeInst[outs];
		NodeInst[] nmospins = new NodeInst[outs];
		NodeInst[] gndpins2 = new NodeInst[outs];
		NodeInst[] vddpins = new NodeInst[outs];
		NodeInst[] pmospins = new NodeInst[outs];
		NodeInst vddc = null;
		NodeInst nwellc = null;
		NodeInst gndc = null;
		NodeInst pwellc = null;
	
		PortProto[] ntransports = new PortProto[outs];
		PortProto[] ptransports = new PortProto[outs];
		PortProto[] inports = new PortProto[outs];
		PortProto[] outports = new PortProto[outs];
		PortProto[] inports2 = new PortProto[outs];
		PortProto[] outports2 = new PortProto[outs];
		PortProto[] polyports = new PortProto[outs];
		PortProto[] intpports = new PortProto[outs];
		PortProto[] polyports2 = new PortProto[outs];
		PortProto[] intpports2 = new PortProto[outs];
		PortProto[] nmosports = new PortProto[outs];
		PortProto[] pmosports = new PortProto[outs];
		PortProto[] gndports = new PortProto[outs];
		PortProto[] gndports2 = new PortProto[outs];
		PortProto[] vddports = new PortProto[outs];
		PortProto[] midvddports = new PortProto[outs];
		PortProto vddcport = null;
		PortProto nwellport = null;
		PortProto gndcport = null;
		PortProto pwellport = null;

		// get pointers to primitives
		NodeProto nmos = tech.findNodeProto("N-Transistor");
		PortProto nmosg1port = nmos.findPortProto("n-trans-poly-right");
		PortProto nmosg2port = nmos.findPortProto("n-trans-poly-left");
		PortProto nmosd1port = nmos.findPortProto("n-trans-diff-top");
		PortProto nmosd2port = nmos.findPortProto("n-trans-diff-bottom");
		double[] nmosbox = {-nmos.getDefWidth()/2-lambda/2,
						 nmos.getDefWidth()/2+lambda/2,
						 -nmos.getDefHeight()/2,
						 nmos.getDefHeight()/2};
					
		NodeProto pmos = tech.findNodeProto("P-Transistor");
		PortProto pmosg1port = pmos.findPortProto("p-trans-poly-right");
		PortProto pmosg2port = pmos.findPortProto("p-trans-poly-left");
		PortProto pmosd1port = pmos.findPortProto("p-trans-diff-top");
		PortProto pmosd2port = pmos.findPortProto("p-trans-diff-bottom");
		double[] pmosbox = {-pmos.getDefWidth()/2-lambda/2,
						 pmos.getDefWidth()/2+lambda/2,
						 -pmos.getDefHeight()/2,
						 pmos.getDefHeight()/2};
	
		NodeProto ppin = tech.findNodeProto("Polysilicon-1-Pin");
		PortProto ppinport = ppin.getPort(0);
		double[] ppinbox = {-ppin.getDefWidth()/2,
						 ppin.getDefWidth()/2,
						 -ppin.getDefHeight()/2,
						 ppin.getDefHeight()/2};
		
		NodeProto napin = tech.findNodeProto("N-Active-Pin");
		
		NodeProto m1pin = tech.findNodeProto("Metal-1-Pin");
		PortProto m1pinport = m1pin.getPort(0);
		double[] m1pinbox = {-m1pin.getDefWidth()/2-lambda/2,
						  m1pin.getDefWidth()/2+lambda/2,
						  -m1pin.getDefHeight()/2-lambda/2,
						  m1pin.getDefHeight()/2+lambda/2};
		
		NodeProto m2pin = tech.findNodeProto("Metal-2-Pin");
		PortProto m2pinport = m2pin.getPort(0);
		double[] m2pinbox = {-m1pin.getDefWidth()/2-lambda/2,
						  m1pin.getDefWidth()/2+lambda/2,
						  -m1pin.getDefHeight()/2-lambda/2,
						  m1pin.getDefHeight()/2+lambda/2};
		
		NodeProto nwnode = tech.findNodeProto("N-Well-Node");
		double[] nwnodebox =
			{-nwnode.getDefWidth()/2-lambda/2,
			 nwnode.getDefWidth()/2+lambda/2,
			 -nwnode.getDefHeight()/2-lambda/2,
			 nwnode.getDefHeight()/2+lambda/2};
		
		NodeProto pwnode = tech.findNodeProto("P-Well-Node");
		double[] pwnodebox =
			{-pwnode.getDefWidth()/2-lambda/2,
			 pwnode.getDefWidth()/2+lambda/2,
			 -pwnode.getDefHeight()/2-lambda/2,
			 pwnode.getDefHeight()/2+lambda/2};
	
		NodeProto m1m2c = tech.findNodeProto("Metal-1-Metal-2-Con");
		PortProto m1m2cport = m1m2c.getPort(0);
		double[] m1m2cbox = {-5*lambda/2, 5*lambda/2, -5*lambda/2, 5*lambda/2}; 
	
		NodeProto diffpin = tech.findNodeProto("Active-Pin");
		PortProto diffpinport = diffpin.getPort(0);
		double[] diffpinbox =
			{-diffpin.getDefWidth()/2-lambda/2,
			 diffpin.getDefWidth()/2+lambda/2,
			 -diffpin.getDefHeight()/2-lambda/2,
			 diffpin.getDefHeight()/2+lambda/2};
		
		NodeProto mnac = tech.findNodeProto("Metal-1-N-Active-Con");
		PortProto mnacport = mnac.getPort(0);
		double aaa = 17;
		double[] mnacbox = {-aaa*lambda/2, aaa*lambda/2, -aaa*lambda/2, aaa*lambda/2};
		// centers around 6 goes up by multiples of 2
		
		NodeProto mpac = tech.findNodeProto("Metal-1-P-Active-Con");
		PortProto mpacport = mpac.getPort(0);
		double[] mpacbox = {-aaa*lambda/2, aaa*lambda/2, -aaa*lambda/2, aaa*lambda/2};
		// centers around 6 goes up by multiples of 2
	
		double mx = -7;
		double my = 5;
		NodeProto mpc = tech.findNodeProto("Metal-1-Polysilicon-1-Con");
		PortProto mpcport = mpc.getPort(0);
		double[] mpcbox = {-mx*lambda/2, mx*lambda/2, -my*lambda/2, my*lambda/2}; 
	
		NodeProto mpwc = tech.findNodeProto("Metal-1-P-Well-Con");
		PortProto mpwcport = mpwc.getPort(0);
		double[] mpwcbox = {-17*lambda/2, 17*lambda/2, -17*lambda/2, 17*lambda/2};
		
		NodeProto mnwc = tech.findNodeProto("Metal-1-N-Well-Con");
		PortProto mnwcport = mnwc.getPort(0);
		double[] mnwcbox = {-17*lambda/2, 17*lambda/2, -17*lambda/2, 17*lambda/2};
	
		ArcProto parc = tech.findArcProto("Polysilicon-1");
		ArcProto m1arc = tech.findArcProto("Metal-1");
		ArcProto m2arc = tech.findArcProto("Metal-2");
		ArcProto ndiffarc = tech.findArcProto("N-Active");
		ArcProto pdiffarc = tech.findArcProto("P-Active");
	
		// create a cell called layoutname+lay}" in the destination library
		Cell ininvp = Cell.newInstance(destLib, layoutname+"{lay}");

		double lowX = -8*lambda;
lowX += 7*lambda;
		double highX = (4*lambda*8*lengthbits)+24*lambda;
highX -= 64*lambda;
		NodeInst pwellnode =
			makeCStyleNodeInst(pwnode, lowX,highX, -18*lambda,10*lambda, 0,0,ininvp);
		NodeInst nwellnode =
			makeCStyleNodeInst(nwnode, lowX,highX, -36*lambda,-18*lambda, 0,0,ininvp);

		// Create instances of objects on input inverter plane
		x = 0;
		for (i=0; i<outs; i++)
		{
			x += 8*lambda;
			y = 0;

			// place inpins, polypins, gndpins, midvddpins
			gndpins[i] =
				makeCStyleNodeInst(mnac,mnacbox[0]+x,mnacbox[1]+x,mnacbox[2]+y-10*lambda,
									 mnacbox[3]+y-10*lambda,0,0,ininvp);
			gndports[i] = mnacport;
			midvddpins[i] =
				makeCStyleNodeInst(mpac,mpacbox[0]+x,mpacbox[1]+x,mpacbox[2]+y-26*lambda,
									 mpacbox[3]+y-26*lambda,0,0,ininvp);
			midvddports[i] = mpacport;
			
			double off = 4*lambda;
			ptrans[i] =
				makeCStyleNodeInst(pmos,pmosbox[0]+x+off,pmosbox[1]+x+off,
									 pmosbox[2]+y-26*lambda,pmosbox[3]+y-26*lambda,
									 1,0,ininvp);
			ntrans[i] =
				makeCStyleNodeInst(nmos,nmosbox[0]+x+off,nmosbox[1]+x+off,
									 nmosbox[2]+y-10*lambda,nmosbox[3]+y-10*lambda,
									 1,0,ininvp);
	
			// place gnd, intvdd, vddpins
			x += 8*lambda;
			polypins[i] =
				makeCStyleNodeInst(ppin,ppinbox[0]+x-8*lambda,ppinbox[1]+x-8*lambda,
									 ppinbox[2]+y-6*lambda,ppinbox[3]+y-6*lambda,
									 0,0,ininvp);
			polyports[i] = ppinport;
			inpins[i] =
				makeCStyleNodeInst(mpc,mpcbox[0]+x-14*lambda,mpacbox[1]+x-14*lambda,
									 mpcbox[2]+y,mpcbox[3]+y,0,0,ininvp);
			inports[i] = mpcport;
			if (top == true)
			{
				inpins2[i] =
					makeCStyleNodeInst(m1m2c,m1m2cbox[0]+x-8*lambda,
										 m1m2cbox[1]+x-8*lambda,
										 m1m2cbox[2]+y+(8*lambda*(i+1)),
										 m1m2cbox[3]+y+(8*lambda*(i+1)),0,0,ininvp);
				inports2[i] = m1m2cport;
			}	
			intppins[i] =
				makeCStyleNodeInst(ppin,ppinbox[0]+x-off,ppinbox[1]+x-off,
									 ppinbox[2]+y-6*lambda,ppinbox[3]+y-6*lambda,
									 0,0,ininvp);
			intpports[i] = ppinport;
			pmospins[i] =
				makeCStyleNodeInst(mpac,mpacbox[0]+x,mpacbox[1]+x,mpacbox[2]+y-26*lambda,
									 mpacbox[3]+y-26*lambda,0,0,ininvp);
			pmosports[i] = mpacport;
			vddpins[i] = makeCStyleNodeInst(m1m2c,m1m2cbox[0]+x-8*lambda,
											  m1m2cbox[1]+x-8*lambda,
											  m1m2cbox[2]+y-26*lambda,
											  m1m2cbox[3]+y-26*lambda,0,0,ininvp);
			vddports[i] = m1m2cport;
			nmospins[i] =
				makeCStyleNodeInst(mnac,mnacbox[0]+x,mnacbox[1]+x,mnacbox[2]+y-10*lambda,
									 mnacbox[3]+y-10*lambda,0,0,ininvp);
			nmosports[i] = mnacport;
			gndpins2[i] =
				makeCStyleNodeInst(m1m2c,m1m2cbox[0]+x-8*lambda,m1m2cbox[1]+x-8*lambda,
									 m1m2cbox[2]+y-10*lambda,m1m2cbox[3]+y-10*lambda,
									 0,0,ininvp);
			gndports2[i] = m1m2cport;
			outpins[i] =
				makeCStyleNodeInst(mpc,mpcbox[0]+x-14*lambda,mpacbox[1]+x-14*lambda,
									 mpcbox[2]+y-36*lambda,mpcbox[3]+y-36*lambda,
									 0,0,ininvp);
			outports[i] = mpcport;
			if (top == false)
			{
				outpins2[i] =
					makeCStyleNodeInst(m1m2c,m1m2cbox[0]+x-8*lambda,
										 m1m2cbox[1]+x-8*lambda,
										 m1m2cbox[2]+y-(8*lambda*(i+1))-36*lambda,
										 m1m2cbox[3]+y-(8*lambda*(i+1))-36*lambda,
										 0,0,ininvp);
				outports2[i] = m1m2cport;
			}
			polypins2[i] =
				makeCStyleNodeInst(ppin,ppinbox[0]+x-8*lambda,ppinbox[1]+x-8*lambda,
									 ppinbox[2]+y-30*lambda,ppinbox[3]+y-30*lambda,
									 0,0,ininvp);
			polyports2[i] = ppinport;
			intppins2[i] =
				makeCStyleNodeInst(ppin,ppinbox[0]+x-off,ppinbox[1]+x-off,
									 ppinbox[2]+y-30*lambda,ppinbox[3]+y-30*lambda,
									 0,0,ininvp);
			intpports2[i] = ppinport;
			
			if (i == (outs-1))
			{
				if (top == true)
				{
					vddc =
						makeCStyleNodeInst(m2pin,m2pinbox[0]+x+12*lambda,
											 m2pinbox[1]+x+12*lambda,
											 m2pinbox[2]+y-26*lambda,
											 m2pinbox[3]+y-26*lambda,0,0,ininvp);
					vddcport = m2pinport;
					gndc =
						makeCStyleNodeInst(m1m2c,m1m2cbox[0]+x+12*lambda,
											 m1m2cbox[1]+x+12*lambda,
											 m1m2cbox[2]+y-10*lambda,
											 m1m2cbox[3]+y-10*lambda,0,0,ininvp);
					gndcport = m1m2cport;
					pwellc =
						makeCStyleNodeInst(mpwc,mpwcbox[0]+x+12*lambda,
											 mpwcbox[1]+x+12*lambda,mpwcbox[2]+y-10*lambda,
											 mpwcbox[3]+y-10*lambda,0,0,ininvp);
					pwellport = mpwcport;
				} else
				{
					vddc =
						makeCStyleNodeInst(m1m2c,m1m2cbox[0]+x+12*lambda,
											 m1m2cbox[1]+x+12*lambda,
											 m1m2cbox[2]+y-26*lambda,
											 m1m2cbox[3]+y-26*lambda,0,0,ininvp);
					vddcport = m1m2cport;
					nwellc =
						makeCStyleNodeInst(mnwc,mnwcbox[0]+x+12*lambda,
											 mnwcbox[1]+x+12*lambda,mnwcbox[2]+y-26*lambda,
											 mnwcbox[3]+y-26*lambda,0,0,ininvp);
					nwellport = mnwcport;
					gndc =
						makeCStyleNodeInst(m2pin,m2pinbox[0]+x+12*lambda,
											 m2pinbox[1]+x+12*lambda,
											 m2pinbox[2]+y-10*lambda,
											 m2pinbox[3]+y-10*lambda,0,0,ininvp);
					gndcport = m2pinport;
				}
			}
		}
	
		// connect transistors to diffusion lines
		for (i=0; i<outs; i++)
		{
			ap2 = gndpins[i];
			ap3 = nmospins[i];
			ap1 = ntrans[i];
			apport1 = nmosd1port;
			gndport1 = nmosd2port;
			appos1 = getCStylePortPosition(ap1, apport1);
			gndpos1 = getCStylePortPosition(ap1, gndport1);
			apport2 = mnacport;
			appos2 = getCStylePortPosition(ap2, apport2);
			apport3 = mnacport;
			appos3 = getCStylePortPosition(ap3, apport3);
			// ndiffarc size centers around 12 and goes up by multiples of 2
			makeCStyleArcInst(ndiffarc,16*lambda,ap1,apport1,appos1[0],
								appos1[1],ap2,apport2,appos2[0], appos2[1]);
			makeCStyleArcInst(ndiffarc,16*lambda,ap1,gndport1,gndpos1[0],
								gndpos1[1],ap3,apport3,appos3[0], appos3[1]);
		}
	
		// connect transistors to diffusion lines
		for (i=0; i<outs; i++)
		{
			ap2 = midvddpins[i];
			ap3 = pmospins[i];
			ap1 = ptrans[i];
			apport1 = pmosd1port;
			vddport1 = pmosd2port;
			appos1 = getCStylePortPosition(ap1, apport1);
			vddpos1 = getCStylePortPosition(ap1, vddport1);
			apport2 = mpacport;
			appos2 = getCStylePortPosition(ap2, apport2);
			apport3 = mpacport;
			appos3 = getCStylePortPosition(ap3, apport3);
			// pdiffarc size centers around 12 and goes up by multiples of 2
			makeCStyleArcInst(pdiffarc,16*lambda,ap1,apport1,appos1[0],
								appos1[1],ap2,apport2,appos2[0], appos2[1]);
			makeCStyleArcInst(pdiffarc,16*lambda,ap1,vddport1,vddpos1[0],
								vddpos1[1],ap3,apport3,appos3[0], appos3[1]);
		}
	
		// metal-1 mpac to mnac
		for (i=0; i<outs; i++)
		{
			ap1 = nmospins[i];
			apport1 = nmosports[i];
			appos1 = getCStylePortPosition(ap1, apport1);
			ap2 = pmospins[i];
			apport2 = pmosports[i];
			appos2 = getCStylePortPosition(ap2, apport2);
			makeCStyleArcInst(m1arc,4*lambda,ap1,apport1,appos1[0],
								appos1[1],ap2,apport2,appos2[0], appos2[1]);
		}
	
		if (top == true)
		{
			ap1 = gndc;
			apport1 = gndcport;
			appos1 = getCStylePortPosition(ap1, apport1);
			ap2 = pwellc;
			apport2 = pwellport;
			appos2 = getCStylePortPosition(ap2, apport2);
			makeCStyleArcInst(m1arc,4*lambda,ap1,apport1,appos1[0],
								appos1[1],ap2,apport2,appos2[0], appos2[1]);
		} else
		{
			ap1 = vddc;
			apport1 = vddcport;
			appos1 = getCStylePortPosition(ap1, apport1);
			ap2 = nwellc;
			apport2 = nwellport;
			appos2 = getCStylePortPosition(ap2, apport2);
			makeCStyleArcInst(m1arc,4*lambda,ap1,apport1,appos1[0],
								appos1[1],ap2,apport2,appos2[0], appos2[1]);
		}
	
		if (top == true)
		{
			for (i=0; i<outs; i++)
			{
				ap1 = inpins[i];
				apport1 = inports[i];
				appos1 = getCStylePortPosition(ap1, apport1);
				ap2 = inpins2[i];
				apport2 = inports2[i];
				appos2 = getCStylePortPosition(ap2, apport2);
				makeCStyleArcInst(m1arc,4*lambda,ap1,apport1,appos1[0],
									appos1[1],ap2,apport2,appos2[0], appos2[1]);
			}
		} else
		{
			for (i=0; i<outs; i++)
			{
				ap1 = outpins[i];
				apport1 = outports[i];
				appos1 = getCStylePortPosition(ap1, apport1);
				ap2 = outpins2[i];
				apport2 = outports2[i];
				appos2 = getCStylePortPosition(ap2, apport2);
				makeCStyleArcInst(m1arc,4*lambda,ap1,apport1,appos1[0],
									appos1[1],ap2,apport2,appos2[0], appos2[1]);
			}
		}
	
		// poly inpins to polypins
		for (i=0; i<outs; i++)
		{
			ap1 = inpins[i];
			apport1 = inports[i];
			appos1 = getCStylePortPosition(ap1, apport1);
			ap2 = polypins[i];
			apport2 = polyports[i];
			appos2 = getCStylePortPosition(ap2, apport2);
			makeCStyleArcInst(parc,2*lambda,ap1,apport1,appos1[0],
								appos1[1],ap2,apport2,appos2[0], appos2[1]);
		}
	
		// poly polypins to intppins
		for (i=0; i<outs; i++)
		{
			ap1 = polypins[i];
			apport1 = polyports[i];
			appos1 = getCStylePortPosition(ap1, apport1);
			ap2 = intppins[i];
			apport2 = intpports[i];
			appos2 = getCStylePortPosition(ap2, apport2);
			makeCStyleArcInst(parc,2*lambda,ap1,apport1,appos1[0],
								appos1[1],ap2,apport2,appos2[0], appos2[1]);
		}
	
		// poly intppins to ntrans
		for (i=0; i<outs; i++)
		{
			ap1 = intppins[i];
			apport1 = intpports[i];
			appos1 = getCStylePortPosition(ap1, apport1);
			ap2 = ntrans[i];
			apport2 = nmosg2port;
			appos2 = getCStylePortPosition(ap2, apport2);
			makeCStyleArcInst(parc,2*lambda,ap1,apport1,appos1[0],
								appos1[1],ap2,apport2,appos2[0], appos2[1]);
		}
	
		// poly ntrans to ptrans
		for (i=0; i<outs; i++)
		{
			ap1 = ntrans[i];
			apport1 = nmosg1port;
			appos1 = getCStylePortPosition(ap1, apport1);
			ap2 = ptrans[i];
			apport2 = pmosg2port;
			appos2 = getCStylePortPosition(ap2, apport2);
			makeCStyleArcInst(parc,2*lambda,ap1,apport1,appos1[0],
								appos1[1],ap2,apport2,appos2[0], appos2[1]);
		}
		
		// poly outpins to polypins
		for (i=0; i<outs; i++)
		{
			ap1 = outpins[i];
			apport1 = outports[i];
			appos1 = getCStylePortPosition(ap1, apport1);
			ap2 = polypins2[i];
			apport2 = polyports2[i];
			appos2 = getCStylePortPosition(ap2, apport2);
			makeCStyleArcInst(parc,2*lambda,ap1,apport1,appos1[0],
								appos1[1],ap2,apport2,appos2[0], appos2[1]);
		}
	
		// poly polypins to intppins
		for (i=0; i<outs; i++)
		{
			ap1 = polypins2[i];
			apport1 = polyports2[i];
			appos1 = getCStylePortPosition(ap1, apport1);
			ap2 = intppins2[i];
			apport2 = intpports2[i];
			appos2 = getCStylePortPosition(ap2, apport2);
			makeCStyleArcInst(parc,2*lambda,ap1,apport1,appos1[0],
								appos1[1],ap2,apport2,appos2[0], appos2[1]);
		}
	
		// poly outtppins to ptrans
		for (i=0; i<outs; i++)
		{
			ap1 = intppins2[i];
			apport1 = intpports2[i];
			appos1 = getCStylePortPosition(ap1, apport1);
			ap2 = ptrans[i];
			apport2 = pmosg1port;
			appos2 = getCStylePortPosition(ap2, apport2);
			makeCStyleArcInst(parc,2*lambda,ap1,apport1,appos1[0],
								appos1[1],ap2,apport2,appos2[0], appos2[1]);
		}
	
		// metal 1 pmospins to vddpins
		for (i=0; i<outs; i++)
		{
			ap1 = midvddpins[i];
			apport1 = midvddports[i];
			appos1 = getCStylePortPosition(ap1, apport1);
			ap2 = vddpins[i];
			apport2 = vddports[i];
			appos2 = getCStylePortPosition(ap2, apport2);
			makeCStyleArcInst(m1arc,4*lambda,ap1,apport1,appos1[0],
								appos1[1],ap2,apport2,appos2[0], appos2[1]);
		}
	
		// metal 1 nmospins to nmospins2
		for (i=0; i<outs; i++)
		{
			ap1 = gndpins[i];
			apport1 = gndports[i];
			appos1 = getCStylePortPosition(ap1, apport1);
			ap2 = gndpins2[i];
			apport2 = gndports2[i];
			appos2 = getCStylePortPosition(ap2, apport2);
			makeCStyleArcInst(m1arc,4*lambda,ap1,apport1,appos1[0],
								appos1[1],ap2,apport2,appos2[0], appos2[1]);
		}
	
		// metal 2 nmospins
		ap1 = gndpins2[0];
		apport1 = gndports2[0];
		appos1 = getCStylePortPosition(ap1, apport1);
		for (i=1; i<outs; i++)
		{
			ap2 = gndpins2[i];
			apport2 = gndports2[i];
			appos2 = getCStylePortPosition(ap2, apport2);
			makeCStyleArcInst(m2arc,4*lambda,ap1,apport1,appos1[0],
								appos1[1],ap2,apport2,appos2[0], appos2[1]);
		}
	
		// metal 2 vddpins
		ap1 = vddpins[0];
		apport1 = vddports[0];
		appos1 = getCStylePortPosition(ap1, apport1);
		for (i=1; i<outs; i++)
		{
			ap2 = vddpins[i];
			apport2 = vddports[i];
			appos2 = getCStylePortPosition(ap2, apport2);
			makeCStyleArcInst(m2arc,4*lambda,ap1,apport1,appos1[0],
								appos1[1],ap2,apport2,appos2[0], appos2[1]);
		}
	
		ap1 = vddpins[0];
		apport1 = vddports[0];
		appos1 = getCStylePortPosition(ap1, apport1);
		ap2 = vddc;
		apport2 = vddcport;
		appos2 = getCStylePortPosition(ap2, apport2);
		makeCStyleArcInst(m2arc,4*lambda,ap1,apport1,appos1[0],
							appos1[1],ap2,apport2,appos2[0], appos2[1]);
		makeCStyleExport(ininvp, ap2, apport2, "vdd", PortCharacteristic.PWR);
	
		ap1 = gndpins2[0];
		apport1 = gndports2[0];
		appos1 = getCStylePortPosition(ap1, apport1);
		ap2 = gndc;
		apport2 = gndcport;
		appos2 = getCStylePortPosition(ap2, apport2);
		makeCStyleArcInst(m2arc,4*lambda,ap1,apport1,appos1[0],
							appos1[1],ap2,apport2,appos2[0], appos2[1]);
		makeCStyleExport(ininvp, ap2, apport2, "gnd", PortCharacteristic.GND);
	
		if (top == true)
		{
			for (i=0; i<outs; i++)
			{
				ap1 = inpins2[i];
				apport1 = inports2[i];
				makeCStyleExport(ininvp, ap1, apport1, "in_top" + i, PortCharacteristic.IN);
			}
		} else
		{
			for (i=0; i<outs; i++)
			{
				ap1 = inpins[i];
				apport1 = inports[i];
				makeCStyleExport(ininvp, ap1, apport1, "in_top" + i, PortCharacteristic.IN);
			}
		}
		if (top == true)
		{
			for (i=0; i<outs; i++)
			{
				ap1 = outpins[i];
				apport1 = outports[i];
				makeCStyleExport(ininvp, ap1, apport1, "in_bot" + i, PortCharacteristic.IN);
			}
		} else
		{
			for (i=0; i<outs; i++)
			{
				ap1 = outpins2[i];
				apport1 = outports2[i];
				makeCStyleExport(ininvp, ap1, apport1, "in_bot" + i, PortCharacteristic.IN);
			}
		}
	
		for (i=0; i<outs; i++)
		{
			ap1 = pmospins[i];
			apport1 = pmosports[i];
			makeCStyleExport(ininvp, ap1, apport1, "in_b" + i, PortCharacteristic.IN);
		}
	}

	/**
	 */		
	private static int[][] romarraygen(String romfile)
	{
		boolean end = false;
		int[][] returnarray = new int[1][1];
	
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(romfile));
			try
			{
				int w = -1;
				int bits = 0;
				StringBuffer sb;
				StringBuffer allfile = new StringBuffer();
				while (!end)
				{
					w++;
					String temp = in.readLine();
					if (temp == null)
					{
						end = true;
					} else
					{
						sb = new StringBuffer(temp);
						if (w==1)
						{
							bits = sb.length();
						}
						if (w==0)
						{
							folds = Integer.parseInt(temp,10);
						} else
						{
							allfile.append(sb);
						}
					}
				}
				w--;

				// set globalbits
				globalbits = w/folds;
				returnarray = new int[bits][w];
				for (int r=0; r<w; r++)
				{
					for (int s=0; s<bits; s++)
					{
						if (allfile.charAt(r*bits + s) == '1')
						{
							returnarray[s][w-r-1] = 1;
						} else
						{
							returnarray[s][w-r-1] = 0; 
						}
					}
				}
			} catch (IOException e)
			{
			}
		} catch(FileNotFoundException e)
		{
			System.out.println(e.toString());
		}
		return returnarray;
	}

	/**
	 */
	private static int[][] generateplane(int bits)
	{
		int lines = (int) (Math.pow(2.0, (double) bits));
		char[][] wordlines = new char[lines][bits];
		for (int i = 0; i<lines; i++)
		{
			int len = Integer.toBinaryString(i).length();
			int leadingz = bits - len;
			int h;
			for (h=0; h<leadingz; h++)
			{
				wordlines[i][h] = '0';
			}
			for (int j=h; j<bits; j++)
			{
				wordlines[i][j] = Integer.toBinaryString(i).charAt(j-h);
			}
		}
		int[][] x =  new int[lines][bits];
		for (int j = 0; j<lines; j++)
		{
			for (int k = 0; k<bits; k++)
			{
				x[j][k] = Character.getNumericValue(wordlines[j][k]);
			}
		}
		int[][] wcomp = new int[lines][2*bits];
		for (int j = 0; j<lines; j++)
		{
			for (int k = 0; k<bits; k++)
			{
				wcomp[j][(2*k)] = x[j][k];
				int complement;
				if (x[j][k] == 1)
				{
					complement = 0;
				} else
				{
					complement = 1;
				}
				wcomp[j][(2*k)+1] = complement;
			}
		}
		int[][] wcompb = new int[lines][2*bits];
		for (int j = 0; j<lines; j++)
		{
			for (int k = 0; k<(2*bits); k++)
			{
				wcompb[j][k] = wcomp[j][(2*bits)-1-k];
			}
		}
		return wcompb;
	}

	/**
	 * Method to fold the ROM data.
	 */
	private static int[][] romfold(int[][] romarray)
	{
		int roma = romarray.length*folds;
		int romb = romarray[1].length/folds;
		int[][] foldedrom = new int[roma][romb];
		
		for (int i=0; i<romarray.length; i++)
		{
			for (int j=0; j<folds; j++)
			{
				for (int k=0; k<romb; k++)
				{
					foldedrom[folds*i+j][k] = romarray[i][j*romb+k];
				}
			}
		}
		return foldedrom;
	}

	/**
	 * Method to convert C-style node creation parameters into the real thing.
	 */
	private static NodeInst makeCStyleNodeInst(NodeProto np, double lX, double hX, double lY, double hY, int trn, int rot, Cell parent)
	{
		double cX = (lX+hX)/2;
		double cY = (lY+hY)/2;
		if (np instanceof Cell)
		{
			// adjust center according to anchor of the cell
			Rectangle2D bounds = ((Cell)np).getBounds();
			cX -= bounds.getCenterX();
			cY -= bounds.getCenterY();
		}
		double width = hX - lX;
		double height = hY - lY;
		Orientation orient = Orientation.fromC(rot, trn != 0);
		NodeInst ni = NodeInst.makeInstance(np, new Point2D.Double(cX, cY), width, height, parent, orient, null, 0);
// 		if (trn != 0)
// 		{
// 			height = -height;
// 			rot = (rot + 900) % 3600;
// 		}
// 		NodeInst ni = NodeInst.makeInstance(np, new Point2D.Double(cX, cY), width, height, parent, rot, null, 0);
		return ni;
	}

	/**
	 * Method to convert C-style Arc creation parameters into the real thing.
	 */
	private static void makeCStyleArcInst(ArcProto ap, double wid, NodeInst hNI, PortProto hPP, double hX, double hY,
		NodeInst tNI, PortProto tPP, double tX, double tY)
	{
		PortInst head = hNI.findPortInstFromProto(hPP);
		PortInst tail = tNI.findPortInstFromProto(tPP);
		ArcInst ai = ArcInst.makeInstance(ap, wid, head, tail, new Point2D.Double(hX, hY), new Point2D.Double(tX, tY), null);
	}

	/**
	 * Method to create an export on a cell.
	 * @param parent the Cell in which to create the Export.
	 * @param ni the NodeInst in the Cell from which to export a port.
	 * @param pp the PortProto on that NodeInst to export.
	 * @param name the name of the new Export.
	 * @param exporttype the Characteristic (in, out, etc.) of the new Export.
	 */
	private static void makeCStyleExport(Cell parent, NodeInst ni, PortProto pp, String name, PortCharacteristic exporttype)
	{
		PortInst pi = ni.findPortInstFromProto(pp);
		Export e = Export.newInstance(parent, pi, name);
		e.setCharacteristic(exporttype);
	}

	/**
	 * Method to return an array of two doubles that describes the center of a port.
	 * @param ni the NodeInst with the port.
	 * @param pp the PortProto on that NodeInst.
	 * @return an array of two doubles that describes the X/Y of the center of a port.
	 */
	private static double [] getCStylePortPosition(NodeInst ni, PortProto pp)
	{
		PortInst pi = ni.findPortInstFromProto(pp);
		Poly poly = pi.getPoly();
		double [] ret = new double[2];
		ret[0] = poly.getCenterX();
		ret[1] = poly.getCenterY();
		return ret;
	}

}
