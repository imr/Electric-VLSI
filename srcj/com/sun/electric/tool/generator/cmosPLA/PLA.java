/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PLA.java
 * MOSIS CMOS PLA Generator.
 * Originally written by Wallace Kroeker at the University of Calgary
 * Translated to Java by Steven Rubin, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.generator.cmosPLA;

import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.lib.LibFile;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 * Class to generate MOSIS CMOS PLAs from personality files.
 */
public class PLA
{
	/** maximum number of columns */						static final int MAX_COL_SIZE = 500;
	/** Lambda seperation between grid lines in array */	static final int X_SEP        =  10;
	/** Lambda seperation between grid line */				static final int Y_MIR_SEP    =  10;
	/** Lambda seperation between grid lines in array */	static final int Y_SEP        =  10;

	static class UCItem
	{
		int       value;
		NodeInst  nodeInst;
		UCItem    rightItem;
		UCItem    bottomItem;
	};

	/**
	 * Row and column pointers
	 */
	static class UCRow
	{
		UCItem firstItem;
		UCItem lastitem;

		UCRow()
		{
			firstItem = new UCItem();
			lastitem = new UCItem();
		}
	};

	/** the cells in the pla_mocmos library */	Cell nmosOne, pmosOne, decoderInv, ioInv4, pullUps;
	/** the generated cells */					private Cell pmosCell, nmosCell, orCell, decodeCell, orPlaneCell;
	/** primitives in the mocmos technology */	PrimitiveNode m1Pin, m2Pin, m12Con, mpCon, maCon, mwBut, msBut, pwNode;
	/** arcs in the mocmos technology */		ArcProto m1Arc, m2Arc, pArc, aArc;
	UCRow [] columnList;
	UCRow [][] rowList;
	private NGrid ng;
	private PGrid pg;
	private Decode dec;
	private String cellName;
	private String andFileName;
	private String orFileName;
	private boolean inputsOnTop;
	private boolean outputsOnBottom;

	/**
	 * Method called to generate a CMOS PLA.
	 * Invokes a dialog to ask for options.
	 */
	public static void generate()
	{
		// prompt the user for some information
		SetupPLAGen dialog = new SetupPLAGen();
		if (dialog.failed()) return;

		generate(Library.getCurrent(), dialog.getCellName(), dialog.getAndFileName(), dialog.getOrFileName(),
			dialog.isInputsOnTop(), dialog.isOutputsOnBottom());
	}

	/**
	 * Method called to generate a CMOS PLA, given all options.
     * @param destLib library where create PLA cells.
	 * @param cellName the name of the PLA cell to generate.
	 * @param andFileName the disk file with the AND plane.
	 * @param orFileName the disk file with the OR plane.
	 * @param inputsOnTop true to place inputs on the top of the plane.
	 * @param outputsOnBottom true to place outputs on the bottom of the plane.
	 */
	public static void generate(Library destLib, String cellName, String andFileName, String orFileName,
                                boolean inputsOnTop, boolean outputsOnBottom)
	{
		// make sure the standard cell library is read in
		String libName = "pla_mocmos";
		if (Library.findLibrary(libName) == null)
		{
            // start a job to read the PLA support library
			new ReadPLALibraryJob(libName, true);
		}
		new GeneratePLAJob(destLib, cellName, andFileName, orFileName, inputsOnTop, outputsOnBottom, true);
	}

	/**
	 * Class to read the PLA support library in a new job.
	 */
	private static class ReadPLALibraryJob extends Job
	{
		private String libName;

		private ReadPLALibraryJob(String libName, boolean doItNow)
		{
			super("Read PLA Library", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.libName = libName;
            if (doItNow)
            {
                try { doIt(); } catch (Exception e) { e.printStackTrace(); }
            }
            else
			    startJob();
		}

		public boolean doIt() throws JobException
		{
			URL fileURL = LibFile.getLibFile(libName + ".jelib");
    		Library lib = LibraryFiles.readLibrary(fileURL, libName, FileType.JELIB, true);
//            Undo.noUndoAllowed();
			return true;
		}
	}

	/**
	 * Class to build a CMOS PLA in a new Job.
	 */
	private static class GeneratePLAJob extends Job
	{
        private Library destLib;
		private String cellName;
		private String andFileName;
		private String orFileName;
		private boolean inputsOnTop;
		private boolean outputsOnBottom;
		private Cell newCell;
        private boolean doItNow; // for regression

		protected GeneratePLAJob(Library destLib, String cellName, String andFileName, String orFileName,
                                 boolean inputsOnTop, boolean outputsOnBottom, boolean doItNow)
		{
			super("Generate MOSIS CMOS PLA", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.destLib = destLib;
			this.cellName = cellName;
			this.andFileName = andFileName;
			this.orFileName = orFileName;
			this.inputsOnTop = inputsOnTop;
			this.outputsOnBottom = outputsOnBottom;
            this.doItNow = doItNow;
            if (!doItNow)
			    startJob();
            else
            {
                try {doIt();} catch (Exception e) { e.printStackTrace();}
            }
		}

		public boolean doIt() throws JobException
		{
			PLA pla = new PLA(cellName, andFileName, orFileName, inputsOnTop, outputsOnBottom);
			newCell = pla.doStep(destLib);
            if (!doItNow)
			    fieldVariableChanged("newCell");
			return true;
		}

        public void terminateOK()
        {
            if (newCell != null)
                WindowFrame.createEditWindow(newCell);
        }
	}

	private PLA(String cellName, String andFileName, String orFileName, boolean inputsOnTop, boolean outputsOnBottom)
	{
		this.cellName = cellName;
		this.andFileName = andFileName;
		this.orFileName = orFileName;
		this.inputsOnTop = inputsOnTop;
		this.outputsOnBottom = outputsOnBottom;
		pg = new PGrid(this);
		ng = new NGrid(this);
		dec = new Decode(this);
	}

	private Cell doStep(Library lib)
		throws JobException
	{
		initialize();

		// generate the AND plane (Decode unit of a ROM)
		String thisCellName = cellName + "_p_cell{lay}";
		pmosCell = pg.pmosGrid(lib, andFileName, thisCellName);
		if (pmosCell == null) return null;

		thisCellName = cellName + "_n_cell{lay}";
		nmosCell = ng.nmosGrid(lib, andFileName, thisCellName);
		if (nmosCell == null) return null;

		thisCellName = cellName + "_decode{lay}";
		decodeCell = dec.decodeGen(lib, pmosCell, nmosCell, thisCellName, inputsOnTop);
		if (decodeCell == null) return null;

		// Generate the OR plane
		thisCellName = cellName + "_or_cell{lay}";
		orCell = ng.nmosGrid(lib, orFileName, thisCellName);
		if (orCell == null) return null;

		thisCellName = cellName + "_or_plane{lay}";
		orPlaneCell = makeOrPlane(lib, thisCellName, outputsOnBottom);
		if (orPlaneCell == null) return null;

		Cell plaCell = makePLA(lib, cellName + "{lay}");
		return plaCell;
	}

	private void initialize()
		throws JobException
	{
		// get the right technology and all its nodes and arcs
		Technology theTech = Technology.findTechnology("mocmos");
		if (theTech == null) throw new JobException("Cannot find technology 'mocmos'");

		m1Pin = theTech.findNodeProto("Metal-1-Pin");
		if (m1Pin == null) throw new JobException("Cannot find Metal-1-Pin primitive");
		m2Pin = theTech.findNodeProto("Metal-2-Pin");
		if (m2Pin == null) throw new JobException("Cannot find Metal-2-Pin primitive");
		m12Con = theTech.findNodeProto("Metal-1-Metal-2-Con");
		if (m12Con == null) throw new JobException("Cannot find Metal-1-Metal-2-Con primitive");
		mpCon = theTech.findNodeProto("Metal-1-Polysilicon-1-Con");
		if (mpCon == null) throw new JobException("Cannot find Metal-1-Polysilicon-1-Con primitive");
		maCon = theTech.findNodeProto("Metal-1-N-Active-Con");
		if (maCon == null) throw new JobException("Cannot find Metal-1-N-Active-Con primitive");
		mwBut = theTech.findNodeProto("Metal-1-P-Well-Con");
		if (mwBut == null) throw new JobException("Cannot find Metal-1-P-Well-Con primitive");
		msBut = theTech.findNodeProto("Metal-1-N-Well-Con");
		if (msBut == null) throw new JobException("Cannot find Metal-1-N-Well-Con primitive");
		pwNode = theTech.findNodeProto("P-Well-Node");
		if (pwNode == null) throw new JobException("Cannot find P-Well-Node primitive");

		m1Arc = theTech.findArcProto("Metal-1");
		if (msBut == null) throw new JobException("Cannot find Metal-1 arc");
		m2Arc = theTech.findArcProto("Metal-2");
		if (msBut == null) throw new JobException("Cannot find Metal-2 arc");
		pArc = theTech.findArcProto("Polysilicon-1");
		if (msBut == null) throw new JobException("Cannot find Polysilicon-1 arc");
		aArc = theTech.findArcProto("N-Active");
		if (msBut == null) throw new JobException("Cannot find N-Active arc");

		// make sure the standard cell library is read in
		String libName = "pla_mocmos";
		Library cellLib = Library.findLibrary(libName);
		if (cellLib == null) throw new JobException("Cannot find the 'pla_mocmos' support library");

		// find required cells in the library
		nmosOne = cellLib.findNodeProto("nmos_one");
		if (nmosOne == null) throw new JobException("Unable to find cell 'nmos_one'");
		pmosOne = cellLib.findNodeProto("pmos_one");
		if (pmosOne == null) throw new JobException("Unable to find cell 'pmos_one'");
		decoderInv = cellLib.findNodeProto("decoder_inv1");
		if (decoderInv == null) throw new JobException("Unable to find cell 'decoder_inv1'");
		ioInv4 = cellLib.findNodeProto("io-inv-4");
		if (ioInv4 == null) throw new JobException("Unable to find cell 'io-inv-4'");
		pullUps = cellLib.findNodeProto("pullups");
		if (pullUps == null) throw new JobException("Unable to find cell 'pullups'");

		// initialize the global tables
		columnList = new UCRow[MAX_COL_SIZE];
		rowList = new UCRow[MAX_COL_SIZE][3];
		for(int i=0; i<MAX_COL_SIZE; i++)
		{
			columnList[i] = new UCRow();
			rowList[i][0] = new UCRow();
			rowList[i][1] = new UCRow();
			rowList[i][2] = new UCRow();
		}
	}

	/**
	 * This class displays a dialog to setup PLA generation.
	 */
	private static class SetupPLAGen extends EDialog
	{
		private JTextField andPlaneFile, orPlaneFile, cellName;
		private JCheckBox inputs, outputs;
		private boolean good;

		/** Creates new form to setup PLA generation */
		private SetupPLAGen()
		{
			super(null, true);
			good = false;
			initComponents();
			setVisible(true);
		}

		private void ok() { exit(true); }

		protected void escapePressed() { exit(false); }

		public boolean failed() { return !good; }

		public String getAndFileName() { return andPlaneFile.getText(); }

		public String getOrFileName() { return orPlaneFile.getText(); }

		public boolean isInputsOnTop() { return inputs.isSelected(); }

		public boolean isOutputsOnBottom() { return outputs.isSelected(); }

		public String getCellName() { return cellName.getText(); }

		// Call this method when the user clicks the OK button
		private void exit(boolean goodButton)
		{
			good = goodButton;
			setVisible(false);
		}

		private void browse(boolean andPlane)
		{
			String fileName = OpenFile.chooseInputFile(FileType.ANY,
				(andPlane ? "AND Plane File:" : "OR Plane File:"));
			if (fileName == null) return;
			if (andPlane) andPlaneFile.setText(fileName); else
				orPlaneFile.setText(fileName);
		}

		private void initComponents()
		{
			getContentPane().setLayout(new GridBagLayout());

			setTitle("MOSIS CMOS PLA Generation Control");
			setName("");
			addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent evt) { exit(false); }
			});

			// the AND plane
			JLabel lab1 = new JLabel("AND Plane File:");
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 0;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(lab1, gbc);

			andPlaneFile = new JTextField();
			andPlaneFile.setColumns(35);
			gbc = new GridBagConstraints();
			gbc.gridx = 1;   gbc.gridy = 0;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.weightx = 1;   gbc.weighty = 0.5;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(andPlaneFile, gbc);

			JButton andPlaneBrowse = new JButton("Browse");
			gbc = new GridBagConstraints();
			gbc.gridx = 2;    gbc.gridy = 0;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(andPlaneBrowse, gbc);
			andPlaneBrowse.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { browse(true); }
			});

			// the OR plane
			JLabel lab2 = new JLabel("OR Plane File:");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 1;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(lab2, gbc);

			orPlaneFile = new JTextField();
			orPlaneFile.setColumns(35);
			gbc = new GridBagConstraints();
			gbc.gridx = 1;   gbc.gridy = 1;
			gbc.weightx = 1;   gbc.weighty = 0.5;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(orPlaneFile, gbc);

			JButton orPlaneBrowse = new JButton("Browse");
			gbc = new GridBagConstraints();
			gbc.gridx = 2;    gbc.gridy = 1;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(orPlaneBrowse, gbc);
			orPlaneBrowse.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { browse(false); }
			});

			// other questions
			inputs = new JCheckBox("Input to the Top of the AND plane");
			inputs.setSelected(true);
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 2;
			gbc.gridwidth = 3;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(inputs, gbc);

			outputs = new JCheckBox("Outputs from the Bottom of the OR plane");
			outputs.setSelected(true);
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 3;
			gbc.gridwidth = 3;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(outputs, gbc);

			JLabel lab3 = new JLabel("Cell name:");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 4;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(lab3, gbc);

			cellName = new JTextField();
			cellName.setText("pla");
			gbc = new GridBagConstraints();
			gbc.gridx = 1;   gbc.gridy = 4;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(cellName, gbc);

			// OK and Cancel
			JButton cancel = new JButton("Cancel");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 5;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(cancel, gbc);
			cancel.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { exit(false); }
			});

			JButton ok = new JButton("OK");
			getRootPane().setDefaultButton(ok);
			gbc = new java.awt.GridBagConstraints();
			gbc.gridx = 1;   gbc.gridy = 5;
			gbc.gridwidth = 2;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(ok, gbc);
			ok.addActionListener(new java.awt.event.ActionListener()
			{
				public void actionPerformed(java.awt.event.ActionEvent evt) { exit(true); }
			});

			pack();
		}
	}

	/**
	 * Plus Output options eg. column decode etc.
	 */
	private Cell makeOrPlane(Library library, String cellName, boolean outputsOnBottom)
	{
		Cell cell = Cell.makeInstance(library, cellName);
		NodeInst orNode = makeInstance(cell, orCell, 0, 0, false);
		if (orNode == null) return null;

		NodeInst newNode = null;
		NodeInst lastNode = null;
		int columns = 2;
		Variable var = orCell.getVar("PLA_data_cols");
		if (var != null) columns = ((Integer)var.getObject()).intValue(); else
			System.out.println("DATA_cols defaulting to 2");

		NodeInst gndNode2 = null, pwrNode1 = null;
		char side = 'n';
		if (outputsOnBottom) side = 's';
		int limit = columns / 4;
		Rectangle2D nodeBounds = orNode.getBounds();
		double lowX = nodeBounds.getMinX();   double highX = nodeBounds.getMaxX();
		double lowY = nodeBounds.getMinY();   double highY = nodeBounds.getMaxY();

		Rectangle2D cellBounds = ioInv4.getBounds();
		double diff = cellBounds.getHeight();
		if (diff < 0) diff = -diff;
		if (limit == 0 || (columns % 4) != 0) limit++;
		int invCnt = 0;
		double invY = highY;
		if (outputsOnBottom) invY = lowY - diff;
		invY -= 5;
		double invXOffset = lowX + 17;
		PortProto invPwrW = ioInv4.findExport("PWR.m-2.w");
		PortProto invPwrE = ioInv4.findExport("PWR.m-2.e");
		PortProto invGnd1W = ioInv4.findExport("GND.m-2.sw");
		PortProto invGnd1E = ioInv4.findExport("GND.m-2.se");
		PortProto invGnd2W = ioInv4.findExport("GND.m-2.nw");
		PortProto invGnd2E = ioInv4.findExport("GND.m-2.ne");
		for (int i = 0; i < limit; i++)
		{
			newNode = makeInstance(cell, ioInv4, i*50 + invXOffset, invY, !outputsOnBottom);
			if (newNode == null) return null;
			if (lastNode == null)
			{
				Export.newInstance(cell, newNode.findPortInstFromProto(invPwrW), "PWR.m-2.w");
				Export.newInstance(cell, newNode.findPortInstFromProto(invGnd1W), "GND.m-2.w");

				// Put in GND bar pin (metal-2)
				Poly poly = newNode.findPortInstFromProto(invGnd1W).getPoly();
				double pwrY = poly.getCenterY();
				NodeInst gndNode1 = makePin(cell, lowX-13, pwrY, 6, m12Con);
				if (gndNode1 == null) return null;
				makeWire(m2Arc, 14, newNode, invGnd1W, gndNode1, gndNode1.getProto().getPort(0), cell);

				poly = newNode.findPortInstFromProto(invGnd2W).getPoly();
				pwrY = poly.getCenterY();
				gndNode2 = makePin(cell, lowX-13, pwrY, 6, m12Con);
				if (gndNode2 == null) return null;
				makeWire(m2Arc, 14, newNode, invGnd2W, gndNode2, gndNode2.getProto().getPort(0), cell);
				makeWire(m2Arc, 14, gndNode1, gndNode1.getProto().getPort(0), gndNode2,
					gndNode2.getProto().getPort(0), cell);
			} else
			{
				// get wired
				if (invPwrW != null && invPwrE != null)
					makeWire(m2Arc, 14, lastNode, invPwrE, newNode, invPwrW, cell);
				if (invGnd1W != null && invGnd1E != null)
					makeWire(m2Arc, 14, lastNode, invGnd1E, newNode, invGnd1W, cell);
				if (invGnd2W != null && invGnd2E != null)
					makeWire(m2Arc, 14, lastNode, invGnd2E, newNode, invGnd2W, cell);
			}
			lastNode = newNode;
			for (int x = invCnt; x < invCnt+4; x++)
			{
				Export pPort = orCell.findExport("DATA" + x + ".m-1." + side);
				Export nPort = ioInv4.findExport("in" + (x%4) + ".m-1.n");
				Export outPort = ioInv4.findExport("out" + (x % 4) + "-bar.m-1.s");
				if (outPort != null)
					Export.newInstance(cell, newNode.findPortInstFromProto(outPort), "out" + (invCnt + (x % 4)) + ".m-1." + side);
				if (pPort != null && nPort != null)
					makeWire(m1Arc, 4, orNode, pPort, newNode, nPort, cell);
			}
			invCnt += 4;
		}
		if (invPwrE != null)
		{
			Poly poly = lastNode.findPortInstFromProto(invPwrE).getPoly();
			double pwrY = poly.getCenterY();

			pwrNode1 = makePin(cell, highX+13, pwrY, 14, m2Pin);
			if (pwrNode1 == null) return null;
			makeWire(m2Arc, 14, lastNode, invPwrE, pwrNode1, pwrNode1.getProto().getPort(0), cell);
			Export.newInstance(cell, lastNode.findPortInstFromProto(invPwrE), "PWR.m-2.e");
		}
		if (invGnd1W != null)
			Export.newInstance(cell, lastNode.findPortInstFromProto(invGnd1E), "GND.m-2.e");

		// OK PUT in the PULLUPS
		newNode = null;
		lastNode = null;
		side = 's';
		if (outputsOnBottom) side = 'n';

		Rectangle2D pullUpsBounds = pullUps.getBounds();
		double pullUpsDiff = pullUpsBounds.getHeight();
		if (pullUpsDiff < 0) pullUpsDiff = -pullUpsDiff;
		int pullUpsCnt = 0;
		double pullUpsY = highY;
		if (!outputsOnBottom) pullUpsY = lowY - pullUpsDiff;
		pullUpsY += 16;
		double pullUpsXOffset = lowX + 27;
		Export pullUpsPwrW = pullUps.findExport("PWR.m-2w");
		if (pullUpsPwrW == null)
		{
			System.out.println("Cannot find port PWR.m-2w in "+pullUps);
			return null;
		}
		Export pullUpsPwrE = pullUps.findExport("PWR.m-2e");
		if (pullUpsPwrE == null)
		{
			System.out.println("Cannot find port PWR.m-2e in "+pullUps);
			return null;
		}
		Export pullUpsGnd1W = pullUps.findExport("GND.m-1.sw");
		if (pullUpsGnd1W == null)
		{
			System.out.println("Cannot find port GND.m-1.sw in "+pullUps);
			return null;
		}
		Export pullUpsGnd1E = pullUps.findExport("GND.m-1.se");
		if (pullUpsGnd1E == null)
		{
			System.out.println("Cannot find port GND.m-1.se in "+pullUps);
			return null;
		}
		for (int i = 0; i < limit; i++)
		{
			newNode = makeInstance(cell, pullUps, i*50 + pullUpsXOffset, pullUpsY, !outputsOnBottom);
			if (newNode == null) return null;
			if (lastNode == null)
			{
				Export.newInstance(cell, newNode.findPortInstFromProto(pullUpsPwrW), "PWR0.m-2.w");
				Export.newInstance(cell, newNode.findPortInstFromProto(pullUpsGnd1W), "GND0.m-1.w");
				PortInst pi = newNode.findPortInstFromProto(pullUpsGnd1W);
				Poly poly = pi.getPoly();
				double pwrY = poly.getCenterY();
				NodeInst gndNode1 = makePin(cell, lowX-13, pwrY, 6, m12Con);
				if (gndNode1 == null) return null;
				makeWire(m1Arc, 4, newNode, pullUpsGnd1W, gndNode1, gndNode1.getProto().getPort(0), cell);
				makeWire(m2Arc, 14, gndNode1, gndNode1.getProto().getPort(0), gndNode2,
					gndNode2.getProto().getPort(0), cell);

				Export pullUpsGnd2W = orCell.findExport("GND" + i + ".m-1." + side);
				Export pullUpsGnd2E = orCell.findExport("GND" + (i+1) + ".m-1." + side);

				if (pullUpsGnd1W != null && pullUpsGnd2W != null)
					makeWire(m1Arc, 4, newNode, pullUpsGnd1W, orNode, pullUpsGnd2W, cell);
				if (pullUpsGnd2W != null && pullUpsGnd2E != null)
					makeWire(m1Arc, 4, newNode, pullUpsGnd1E, orNode, pullUpsGnd2E, cell);
			} else
			{
				// get wired
				if (pullUpsPwrW != null && pullUpsPwrE != null)
					makeWire(m2Arc, 14, lastNode, pullUpsPwrE, newNode, pullUpsPwrW, cell);
				Export pullUpsGnd2W = orCell.findExport("GND" + i + ".m-1." + side);
				Export pullUpsGnd2E = orCell.findExport("GND" + (i+1) + ".m-1." + side);
				if (pullUpsGnd1W != null && pullUpsGnd2W != null)
					makeWire(m1Arc, 4, newNode, pullUpsGnd1W, orNode, pullUpsGnd2W, cell);
				if (pullUpsGnd1E != null && pullUpsGnd2E != null)
					makeWire(m1Arc, 4, newNode, pullUpsGnd1E, orNode, pullUpsGnd2E, cell);
			}
			lastNode = newNode;
			for (int x = pullUpsCnt; x < pullUpsCnt+4; x++)
			{
				Export pPort = orCell.findExport("DATA" + x + ".m-1." + side);
				Export nPort = pullUps.findExport("PULLUP" + (x%4) + ".m-1.s");
				if (pPort != null && nPort != null)
					makeWire(m1Arc, 4, orNode, pPort, newNode, nPort, cell);
			}
			pullUpsCnt += 4;
		}
		if (pullUpsPwrE != null)
		{
			PortInst pi = lastNode.findPortInstFromProto(pullUpsPwrE);
			Poly poly = pi.getPoly();
			double pwrY = poly.getCenterY();
			NodeInst pwrNode2 = makePin(cell, highX+13, pwrY, 14, m2Pin);
			if (pwrNode2 == null) return null;
			makeWire(m2Arc, 14, lastNode, pullUpsPwrE, pwrNode2, pwrNode2.getProto().getPort(0), cell);
			makeWire(m2Arc, 14, pwrNode1, pwrNode1.getProto().getPort(0), pwrNode2,
				pwrNode2.getProto().getPort(0), cell);
			Export.newInstance(cell, lastNode.findPortInstFromProto(pullUpsPwrE), "PWR0.m-2.e");
		}
		if (pullUpsGnd1E != null)
			Export.newInstance(cell, lastNode.findPortInstFromProto(pullUpsGnd1E), "GND0.m-1.e");
		int x = 0;
		String aName = "ACCESS" + x + ".p.w";
		PortProto pPort = orCell.findPortProto(aName);
		while (pPort != null)
		{
			Export.newInstance(cell, orNode.findPortInstFromProto(pPort), aName);
			x++;
			aName = "ACCESS" + x + ".p.w";
			pPort = orCell.findPortProto(aName);
		}

		return cell;
	}

	private Cell makePLA(Library library, String name)
	{
		Cell cell = Cell.makeInstance(library, name);
		NodeInst decodeNode = makeInstance(cell, decodeCell, 0, 0, false);
		if (decodeNode == null) return null;
		Rectangle2D decodeBounds = decodeNode.getBounds();
		NodeInst orNode = makeInstance(cell, orPlaneCell, decodeBounds.getMaxX()+27, 0, false);
		if (orNode == null) return null;

		Export orPort = orPlaneCell.findExport("ACCESS0.p.w");
		Poly orPoly = orNode.findPortInstFromProto(orPort).getPoly();
		double orY = orPoly.getCenterY();

		Export decodePort = decodeCell.findExport("DATA0.m-1.n");
		Poly decodePoly = decodeNode.findPortInstFromProto(decodePort).getPoly();
		double decodeY = decodePoly.getCenterY();
		double dY = decodeY - orY;
		if (orY != decodeY) orNode.move(0, dY);

		int x = 0;
		while (orPort != null && decodePort != null)
		{
			makeWire(m1Arc, 4, orNode, orPort, decodeNode, decodePort, cell);
			x++;
			orPort = orPlaneCell.findExport("ACCESS" + x + ".p.w");
			decodePort = decodeCell.findExport("DATA" + x + ".m-1.n");
		}

		return cell;
	}

	NodeInst makePin(Cell cell, double x, double y, double size, PrimitiveNode node)
	{
		double xS = node.getDefWidth();
		double yS = node.getDefHeight();
		if (size < xS) size = xS;
		if (size < yS) size = yS;
		NodeInst ni = NodeInst.makeInstance(node, new Point2D.Double(x, y), size, size, cell);
		if (ni == null)
			System.out.println("Unable to create " + node + " in " + cell);
		return ni;
	}

	void makeWire(ArcProto typ, double width, NodeInst fromNodeInst,
		PortProto fromPortProto, NodeInst toNodeInst, PortProto toPortProto, Cell cell)
	{
		PortInst fromPi = fromNodeInst.findPortInstFromProto(fromPortProto);
		Poly fromPoly = fromPi.getPoly();
		PortInst toPi = toNodeInst.findPortInstFromProto(toPortProto);
		Poly toPoly = toPi.getPoly();
		if (typ == null)
		{
			System.out.println("Attempting to wire with unknown arc layer");
			return;
		}
		double wid = typ.getDefaultWidth();
//		w = us_widestarcinst(typ, fromnodeinst, fromportproto);
//		if (w > wid) wid = w;
//		w = us_widestarcinst(typ, tonodeinst, toportproto);
//		if (w > wid) wid = w;
		if (width > wid) wid = width;
		ArcInst ai = ArcInst.makeInstance(typ, wid, fromPi, toPi);
		if (ai == null)
		{
			System.out.println("Unable to run " + typ + " from " + fromNodeInst +
				", " + fromPortProto + " to " + toNodeInst + ", " +
				toPortProto + " in " + cell);
		}
	}

	NodeInst makeInstance(Cell cell, Cell instProto, double x, double y, boolean mirror)
	{
		Rectangle2D protoBounds = instProto.getBounds();
		double sX = protoBounds.getWidth();
		double sY = protoBounds.getHeight();
		Orientation orient = Orientation.IDENT;
		if (mirror)
		{
			orient = Orientation.Y;
			double y1 = protoBounds.getMinY() + y;
			double y2 = protoBounds.getMaxY() + y;
			y = y2 + y1 - y;
		}
		NodeInst ni = NodeInst.makeInstance(instProto, new Point2D.Double(x, y), sX, sY, cell, orient, null, 0);
		return ni;
	}
}
