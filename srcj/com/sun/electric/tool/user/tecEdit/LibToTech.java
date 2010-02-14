/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LibToTech.java
 * Technology Editor, conversion of technology libraries to technologies
 * Written by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
package com.sun.electric.tool.user.tecEdit;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.DRCTemplate;
import com.sun.electric.technology.EdgeH;
import com.sun.electric.technology.EdgeV;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.TechFactory;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Xml;
import com.sun.electric.technology.ArcProto.Function;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
* This class creates technologies from technology libraries.
*/
public class LibToTech
{
	/* the meaning of "us_tecflags" */
//	private static final int HASDRCMINWID  =          01;				/* has DRC minimum width information */
//	private static final int HASDRCMINWIDR =          02;				/* has DRC minimum width information */
//	private static final int HASCOLORMAP   =          04;				/* has color map */
//	private static final int HASARCWID     =         010;				/* has arc width offset factors */
//	private static final int HASCIF        =         020;				/* has CIF layers */
//	private static final int HASDXF        =         040;				/* has DXF layers */
//	private static final int HASGDS        =        0100;				/* has Calma GDS-II layers */
//	private static final int HASGRAB       =        0200;				/* has grab point information */
//	private static final int HASSPIRES     =        0400;				/* has SPICE resistance information */
//	private static final int HASSPICAP     =       01000;				/* has SPICE capacitance information */
//	private static final int HASSPIECAP    =       02000;				/* has SPICE edge capacitance information */
//	private static final int HAS3DINFO     =       04000;				/* has 3D height/thickness information */
//	private static final int HASCONDRC     =      010000;				/* has connected design rules */
//	private static final int HASCONDRCR    =      020000;				/* has connected design rules reasons */
//	private static final int HASUNCONDRC   =      040000;				/* has unconnected design rules */
//	private static final int HASUNCONDRCR  =     0100000;				/* has unconnected design rules reasons */
//	private static final int HASCONDRCW    =     0200000;				/* has connected wide design rules */
//	private static final int HASCONDRCWR   =     0400000;				/* has connected wide design rules reasons */
//	private static final int HASUNCONDRCW  =    01000000;				/* has unconnected wide design rules */
//	private static final int HASUNCONDRCWR =    02000000;				/* has unconnected wide design rules reasons */
//	private static final int HASCONDRCM    =    04000000;				/* has connected multicut design rules */
//	private static final int HASCONDRCMR   =   010000000;				/* has connected multicut design rules reasons */
//	private static final int HASUNCONDRCM  =   020000000;				/* has unconnected multicut design rules */
//	private static final int HASUNCONDRCMR =   040000000;				/* has unconnected multicut design rules reasons */
//	private static final int HASEDGEDRC    =  0100000000;				/* has edge design rules */
//	private static final int HASEDGEDRCR   =  0200000000;				/* has edge design rules reasons */
//	private static final int HASMINNODE    =  0400000000;				/* has minimum node size */
//	private static final int HASMINNODER   = 01000000000;				/* has minimum node size reasons */
//	private static final int HASPRINTCOL   = 02000000000;				/* has print colors */

	/* the globals that define a technology */
//	static int              us_tecflags;
//	static DRCRULES        *us_tecdrc_rules = 0;

	private TechConversionResult error;

	/************************************* API AND USER INTERFACE *************************************/

	/**
	 * Method to convert the current library to a technology in a new job.
	 * Starts with a dialog to control the process.
	 */
	public static void makeTechFromLib()
	{
		new GenerateTechnology();
	}

	/**
	 * This class displays a dialog for converting a library to a technology.
	 */
	private static class GenerateTechnology extends EDialog
	{
		private JLabel lab2, lab3;
		private JTextField renameName, newName;
		private JCheckBox alsoXML;

		/** Creates new form convert library to technology */
		private GenerateTechnology()
		{
			super(null, true);
			initComponents();
			nameChanged();
			setVisible(true);
		}

		protected void escapePressed() { exit(false); }

		// Call this method when the user clicks the OK button
		private void exit(boolean goodButton)
		{
			if (goodButton)
				new TechFromLibJob(Library.getCurrent(), newName.getText(), alsoXML.isSelected());
			dispose();
		}

		private void nameChanged()
		{
			String techName = newName.getText();
			if (Technology.findTechnology(techName) != null)
			{
				// name exists, offer to rename it
				lab2.setEnabled(true);
				lab3.setEnabled(true);
				renameName.setEnabled(true);
				renameName.setEditable(true);
			} else
			{
				// name is unique, don't offer to rename it
				lab2.setEnabled(false);
				lab3.setEnabled(false);
				renameName.setEnabled(false);
				renameName.setEditable(false);
			}
		}

		private void initComponents()
		{
			getContentPane().setLayout(new GridBagLayout());

			setTitle("Convert Library to Technology");
			setName("");
			addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent evt) { exit(false); }
			});

			JLabel lab1 = new JLabel("Creating new technology:");
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 0;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(lab1, gbc);

			newName = new JTextField(Library.getCurrent().getName());
			gbc = new GridBagConstraints();
			gbc.gridx = 1;   gbc.gridy = 0;
			gbc.gridwidth = 2;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(newName, gbc);
			newName.getDocument().addDocumentListener(new TechNameDocumentListener());

			lab2 = new JLabel("Already a technology with this name");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 1;
			gbc.gridwidth = 3;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(lab2, gbc);

			lab3 = new JLabel("Rename existing technology to:");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 2;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(lab3, gbc);

			renameName = new JTextField();
			gbc = new GridBagConstraints();
			gbc.gridx = 1;   gbc.gridy = 2;
			gbc.gridwidth = 2;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(renameName, gbc);

			alsoXML = new JCheckBox("Also write XML code");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 3;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(alsoXML, gbc);

			// OK and Cancel
			JButton cancel = new JButton("Cancel");
			gbc = new GridBagConstraints();
			gbc.gridx = 1;   gbc.gridy = 3;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(cancel, gbc);
			cancel.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { exit(false); }
			});

			JButton ok = new JButton("OK");
			getRootPane().setDefaultButton(ok);
			gbc = new GridBagConstraints();
			gbc.gridx = 2;   gbc.gridy = 3;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(ok, gbc);
			ok.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { exit(true); }
			});

			pack();
		}

		/**
		 * Class to handle special changes to the new technology name.
		 */
		private class TechNameDocumentListener implements DocumentListener
		{
			public void changedUpdate(DocumentEvent e) { nameChanged(); }
			public void insertUpdate(DocumentEvent e) { nameChanged(); }
			public void removeUpdate(DocumentEvent e) { nameChanged(); }
		}
	}

	/************************************* BUILDING TECHNOLOGY FROM LIBRARY *************************************/

	/**
	 * Class to create a technology-library from a technology (in a Job).
	 */
	private static class TechFromLibJob extends Job
	{
        private Library techLib;
		private String newName;
		private String fileName;
		private TechConversionResult tcr;

		private TechFromLibJob(Library techLib, String newName, boolean alsoXML)
		{
			super("Make Technology from Technolog Library", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.techLib = techLib;
			this.newName = newName;
			if (alsoXML)
			{
				// print the technology as XML
				fileName = OpenFile.chooseOutputFile(FileType.XML, "File for Technology's XML Code",
					newName + ".xml");
			}
			startJob();
		}

		@Override
		public boolean doIt()
		{
			LibToTech ltt = new LibToTech();
			tcr = new TechConversionResult();
	   		ltt.makeTech(techLib, newName, fileName, tcr);
   			fieldVariableChanged("tcr");
			return true;
		}

		public void terminateOK()
		{
	   		if (tcr.failed())
	   		{
	   			tcr.showError();
				System.out.println("Failed to convert the library to a technology");
	   		}
			WindowFrame.updateTechnologyLists();
		}
	}

	/**
	 * Method to convert the current Library to a Technology.
     * @param lib technology Library
	 * @param newName the name of the Technology to create.
	 * @param fileName the name of the XML file to write (null to skip XML output).
	 * @param error the structure for storing error status.
	 * @return the new Technology.  Returns null on error (and fills in "error").
	 */
	public Technology makeTech(Library lib, String newName, String fileName, TechConversionResult error)
	{
		this.error = error;

		// get a new name for the technology
		String newTechName = newName;
		boolean modified = false;
		for(;;)
		{
			// search by hand because "gettechnology" handles partial matches
			if (Technology.findTechnology(newTechName) == null) break;
			newTechName += "X";
			modified = true;
		}
		if (modified)
			System.out.println("Warning: already a technology called " + newName + ".  Naming this " + newTechName);

		// get list of dependent libraries
		Library [] dependentLibs = Info.getDependentLibraries(lib);

		// get general information from the "factors" cell
		Cell np = null;
		for(int i=dependentLibs.length-1; i>=0; i--)
		{
			np = dependentLibs[i].findNodeProto("factors");
			if (np != null) break;
		}
		if (np == null)
		{
			error.markError(null, null, "Cell with general information, called 'factors', is missing");
			return null;
		}
		GeneralInfo gi = parseCell(np);

		// get layer information
		LayerInfo [] lList = extractLayers(dependentLibs);
		if (lList == null) return null;

		// get arc information
		ArcInfo [] aList = extractArcs(dependentLibs, lList);
		if (aList == null) return null;

		// get node information
		NodeInfo [] nList = extractNodes(dependentLibs, lList, aList);
		if (nList == null) return null;

		for(NodeInfo ni: nList)
			ni.arcsShrink = ni.func.isPin() && !ni.wipes;

		// create the pure-layer associations
		for(int i=0; i<lList.length; i++)
		{
			if (lList[i].pseudo) continue;

			// find the pure layer node
			for(int j=0; j<nList.length; j++)
			{
				if (nList[j].func != PrimitiveNode.Function.NODE) continue;
				NodeInfo.LayerDetails nld = nList[j].nodeLayers[0];
				if (nld.layer == lList[i])
				{
					lList[i].pureLayerNode = nList[j];
					break;
				}
			}
		}

		// add the component menu information if available
		Variable var = Library.getCurrent().getVar(Info.COMPMENU_KEY);
		if (var != null)
		{
			String compMenuXML = (String)var.getObject();
			List<Xml.PrimitiveNodeGroup> nodeGroups = new ArrayList<Xml.PrimitiveNodeGroup>();
			for(int i=0; i<nList.length; i++)
			{
                Xml.PrimitiveNodeGroup xng = new Xml.PrimitiveNodeGroup();
				Xml.PrimitiveNode xnp = new Xml.PrimitiveNode();
				xnp.name = nList[i].name;
				xnp.function = nList[i].func;
                xng.nodes.add(xnp);
				nodeGroups.add(xng);
			}
			List<Xml.ArcProto> arcs = new ArrayList<Xml.ArcProto>();
			for(int i=0; i<aList.length; i++)
			{
				Xml.ArcProto xap = new Xml.ArcProto();
				xap.name = aList[i].name;
				arcs.add(xap);
			}
			gi.menuPalette = Xml.parseComponentMenuXMLTechEdit(compMenuXML, nodeGroups, arcs);
		}

		Xml.Technology t = makeXml(newTechName, gi, lList, nList, aList);
        URL techUrl = null;
		if (fileName != null) {
            boolean includeDateAndVersion = User.isIncludeDateAndVersionInOutput();
            String copyrightMessage = IOTool.isUseCopyrightMessage() ? IOTool.getCopyrightMessage() : null;
			t.writeXml(fileName, includeDateAndVersion, copyrightMessage);
            techUrl = TextUtils.makeURLToFile(fileName);
        }
        TechFactory techFactory = TechFactory.fromXml(techUrl, t);
        Technology tech = techFactory.newInstance(Generic.tech());
        if (tech == null) System.out.println("ERROR creating new technology"); else
        {
	        lib.getDatabase().addTech(tech);

			// switch to this technology
			System.out.println("Technology " + tech.getTechName() + " built.");
        }
		return tech;
	}

//	private void checkAndWarn(LayerInfo [] lList, ArcInfo [] aList, NodeInfo [] nList)
//	{
//		// make sure there is a pure-layer node for every nonpseudo layer
//		for(int i=0; i<lList.length; i++)
//		{
//			if (lList[i].pseudo) continue;
//			boolean found = false;
//			for(int j=0; j<nList.length; j++)
//			{
//				NodeInfo nIn = nList[j];
//				if (nIn.func != PrimitiveNode.Function.NODE) continue;
//				if (nIn.nodeLayers[0].layer == lList[i])
//				{
//					found = true;
//					break;
//				}
//			}
//			if (found) continue;
//			System.out.println("Warning: Layer " + lList[i].name + " has no associated pure-layer node");
//		}
//
//		// make sure there is a pin for every arc and that it uses pseudo-layers
//		for(int i=0; i<aList.length; i++)
//		{
//			// find that arc's pin
//			boolean found = false;
//			for(int j=0; j<nList.length; j++)
//			{
//				NodeInfo nIn = nList[j];
//				if (nIn.func != PrimitiveNode.Function.PIN) continue;
//
//				for(int k=0; k<nIn.nodePortDetails.length; k++)
//				{
//					ArcInfo [] connections = nIn.nodePortDetails[k].connections;
//					for(int l=0; l<connections.length; l++)
//					{
//						if (connections[l] == aList[i])
//						{
//							// pin found: make sure it uses pseudo-layers
//							boolean allPseudo = true;
//							for(int m=0; m<nIn.nodeLayers.length; m++)
//							{
//								LayerInfo lin = nIn.nodeLayers[m].layer;
//								if (!lin.pseudo) { allPseudo = false;   break; }
//							}
//							if (!allPseudo)
//								System.out.println("Warning: Pin " + nIn.name + " is not composed of pseudo-layers");
//
//							found = true;
//							break;
//						}
//					}
//					if (found) break;
//				}
//				if (found) break;
//			}
//			if (!found)
//				System.out.println("Warning: Arc " + aList[i].name + " has no associated pin node");
//		}
//	}

	/**
	 * Method to parse the miscellaneous-info cell in "np" and return a GeneralInfo object that describes it.
	 */
	private GeneralInfo parseCell(Cell np)
	{
		// create and initialize the GRAPHICS structure
		GeneralInfo gi = new GeneralInfo();

		for(Iterator<NodeInst> it = np.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			int opt = Manipulate.getOptionOnNode(ni);
			String str = Info.getValueOnNode(ni);
			switch (opt)
			{
				case Info.TECHSHORTNAME:
					gi.shortName = str;
					break;
				case Info.TECHSCALE:
					gi.scale = TextUtils.atof(str);
					gi.scaleRelevant = true;
					break;
                case Info.TECHRESOLUTION:
					gi.resolution = TextUtils.atof(str);
					break;
                case Info.TECHFOUNDRY:
					gi.defaultFoundry = str;
					break;
				case Info.TECHDEFMETALS:
					gi.defaultNumMetals = TextUtils.atoi(str);
					break;
				case Info.TECHDESCRIPT:
					gi.description = str;
					break;
				case Info.TECHSPICEMINRES:
					gi.minRes = TextUtils.atof(str);
					break;
				case Info.TECHSPICEMINCAP:
					gi.minCap = TextUtils.atof(str);
					break;
				case Info.TECHMAXSERIESRES:
					gi.maxSeriesResistance = TextUtils.atof(str);
					break;
				case Info.TECHGATESHRINK:
					gi.gateShrinkage = TextUtils.atof(str);
					break;
				case Info.TECHGATEINCLUDED:
					gi.includeGateInResistance = str.equalsIgnoreCase("yes");
					break;
				case Info.TECHGROUNDINCLUDED:
					gi.includeGround = str.equalsIgnoreCase("yes");
					break;
				case Info.TECHTRANSPCOLORS:
					Color [] colors = GeneralInfo.getTransparentColors(ni);
					if (colors != null) gi.transparentColors = colors;
					break;
				case Info.CENTEROBJ:
					break;
				default:
					error.markError(ni, np, "Unknown node in miscellaneous-information cell");
					break;
			}
		}
		return gi;
	}

	/************************************* LAYER ANALYSIS *************************************/

	/**
	 * Method to scan the "dependentlibcount" libraries in "dependentLibs",
	 * and build the layer structures for it in technology "tech".  Returns true on error.
	 */
	private LayerInfo [] extractLayers(Library [] dependentLibs)
	{
		// first find the number of layers
		Cell [] layerCells = Info.findCellSequence(dependentLibs, "layer-", Info.LAYERSEQUENCE_KEY);
		if (layerCells.length <= 0)
		{
			System.out.println("No layers found");
			return null;
		}

		// create the layers
		LayerInfo [] lis = new LayerInfo[layerCells.length];
		for(int i=0; i<layerCells.length; i++)
		{
			lis[i] = LayerInfo.parseCell(layerCells[i]);
			if (lis[i] == null)
			{
				error.markError(null, layerCells[i], "Error parsing layer information");
				continue;
			}
		}

//		// get the design rules
//		drcsize = us_teclayer_count*us_teclayer_count/2 + (us_teclayer_count+1)/2;
//		us_tecedgetlayernamelist();
//		if (us_tecdrc_rules != 0)
//		{
//			dr_freerules(us_tecdrc_rules);
//			us_tecdrc_rules = 0;
//		}
//		nodecount = Info.findCellSequence(dependentLibs, "node-", Generate.NODERSEQUENCE_KEY);
//		us_tecdrc_rules = dr_allocaterules(us_teceddrclayers, nodecount, x_("EDITED TECHNOLOGY"));
//		if (us_tecdrc_rules == NODRCRULES) return(TRUE);
//		for(i=0; i<us_teceddrclayers; i++)
//			(void)allocstring(&us_tecdrc_rules.layernames[i], us_teceddrclayernames[i], el_tempcluster);
//		for(i=0; i<nodecount; i++)
//			(void)allocstring(&us_tecdrc_rules.nodenames[i], &nodesequence[i].protoname[5], el_tempcluster);
//		if (nodecount > 0) efree((CHAR *)nodesequence);
//		var = NOVARIABLE;
//		for(i=dependentlibcount-1; i>=0; i--)
//		{
//			var = getval((INTBIG)dependentLibs[i], VLIBRARY, VSTRING|VISARRAY, x_("EDTEC_DRC"));
//			if (var != NOVARIABLE) break;
//		}
//		us_teceditgetdrcarrays(var, us_tecdrc_rules);

//		// see which design rules exist
//		for(i=0; i<us_teceddrclayers; i++)
//		{
//			if (us_tecdrc_rules.minwidth[i] >= 0) us_tecflags |= HASDRCMINWID;
//			if (*us_tecdrc_rules.minwidthR[i] != 0) us_tecflags |= HASDRCMINWIDR;
//		}
//		for(i=0; i<drcsize; i++)
//		{
//			if (us_tecdrc_rules.conlist[i] >= 0) us_tecflags |= HASCONDRC;
//			if (*us_tecdrc_rules.conlistR[i] != 0) us_tecflags |= HASCONDRCR;
//			if (us_tecdrc_rules.unconlist[i] >= 0) us_tecflags |= HASUNCONDRC;
//			if (*us_tecdrc_rules.unconlistR[i] != 0) us_tecflags |= HASUNCONDRCR;
//			if (us_tecdrc_rules.conlistW[i] >= 0) us_tecflags |= HASCONDRCW;
//			if (*us_tecdrc_rules.conlistWR[i] != 0) us_tecflags |= HASCONDRCWR;
//			if (us_tecdrc_rules.unconlistW[i] >= 0) us_tecflags |= HASUNCONDRCW;
//			if (*us_tecdrc_rules.unconlistWR[i] != 0) us_tecflags |= HASUNCONDRCWR;
//			if (us_tecdrc_rules.conlistM[i] >= 0) us_tecflags |= HASCONDRCM;
//			if (*us_tecdrc_rules.conlistMR[i] != 0) us_tecflags |= HASCONDRCMR;
//			if (us_tecdrc_rules.unconlistM[i] >= 0) us_tecflags |= HASUNCONDRCM;
//			if (*us_tecdrc_rules.unconlistMR[i] != 0) us_tecflags |= HASUNCONDRCMR;
//			if (us_tecdrc_rules.edgelist[i] >= 0) us_tecflags |= HASEDGEDRC;
//			if (*us_tecdrc_rules.edgelistR[i] != 0) us_tecflags |= HASEDGEDRCR;
//		}
//		for(i=0; i<us_tecdrc_rules.numnodes; i++)
//		{
//			if (us_tecdrc_rules.minnodesize[i*2] > 0 ||
//				us_tecdrc_rules.minnodesize[i*2+1] > 0) us_tecflags |= HASMINNODE;
//			if (*us_tecdrc_rules.minnodesizeR[i] != 0) us_tecflags |= HASMINNODER;
//		}

//		// store this information on the technology object
//		if ((us_tecflags&(HASCONDRCW|HASUNCONDRCW)) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_wide_limitkey, us_tecdrc_rules.widelimit,
//				VFRACT|VDONTSAVE);
//		if ((us_tecflags&HASDRCMINWID) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_min_widthkey, (INTBIG)us_tecdrc_rules.minwidth,
//				VFRACT|VDONTSAVE|VISARRAY|(tech.layercount<<VLENGTHSH));
//		if ((us_tecflags&HASDRCMINWIDR) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_min_width_rulekey, (INTBIG)us_tecdrc_rules.minwidthR,
//				VSTRING|VDONTSAVE|VISARRAY|(tech.layercount<<VLENGTHSH));
//		if ((us_tecflags&HASCONDRC) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_connected_distanceskey,
//				(INTBIG)us_tecdrc_rules.conlist, VFRACT|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASCONDRCR) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_connected_distances_rulekey,
//				(INTBIG)us_tecdrc_rules.conlistR, VSTRING|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASUNCONDRC) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_unconnected_distanceskey,
//				(INTBIG)us_tecdrc_rules.unconlist, VFRACT|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASUNCONDRCR) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_unconnected_distances_rulekey,
//				(INTBIG)us_tecdrc_rules.unconlistR, VSTRING|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASCONDRCW) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_connected_distancesWkey,
//				(INTBIG)us_tecdrc_rules.conlistW, VFRACT|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASCONDRCWR) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_connected_distancesW_rulekey,
//				(INTBIG)us_tecdrc_rules.conlistWR, VSTRING|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASUNCONDRCW) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_unconnected_distancesWkey,
//				(INTBIG)us_tecdrc_rules.unconlistW, VFRACT|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASUNCONDRCWR) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_unconnected_distancesW_rulekey,
//				(INTBIG)us_tecdrc_rules.unconlistWR, VSTRING|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASCONDRCM) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_connected_distancesMkey,
//				(INTBIG)us_tecdrc_rules.conlistM, VFRACT|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASCONDRCMR) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_connected_distancesM_rulekey,
//				(INTBIG)us_tecdrc_rules.conlistMR, VSTRING|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASUNCONDRCM) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_unconnected_distancesMkey,
//				(INTBIG)us_tecdrc_rules.unconlistM, VFRACT|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASUNCONDRCMR) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_unconnected_distancesM_rulekey,
//				(INTBIG)us_tecdrc_rules.unconlistMR, VSTRING|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASEDGEDRC) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_edge_distanceskey,
//				(INTBIG)us_tecdrc_rules.edgelist, VFRACT|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASEDGEDRCR) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_edge_distances_rulekey,
//				(INTBIG)us_tecdrc_rules.edgelistR, VSTRING|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASMINNODE) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_min_node_sizekey,
//				(INTBIG)us_tecdrc_rules.minnodesize, VFRACT|VDONTSAVE|VISARRAY|((us_tecdrc_rules.numnodes*2)<<VLENGTHSH));
//		if ((us_tecflags&HASMINNODER) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_min_node_size_rulekey,
//				(INTBIG)us_tecdrc_rules.minnodesizeR, VSTRING|VDONTSAVE|VISARRAY|(us_tecdrc_rules.numnodes<<VLENGTHSH));

		return lis;
	}

	/************************************* ARC ANALYSIS *************************************/

	/**
	 * Method to scan the "dependentlibcount" libraries in "dependentLibs",
	 * and build the arc structures for it in technology "tech".  Returns true on error.
	 */
	private ArcInfo [] extractArcs(Library [] dependentLibs, LayerInfo [] lList)
	{
		// count the number of arcs in the technology
		Cell [] arcCells = Info.findCellSequence(dependentLibs, "arc-", Info.ARCSEQUENCE_KEY);
		if (arcCells.length <= 0)
		{
			System.out.println("No arcs found");
			return null;
		}

		ArcInfo [] allArcs = new ArcInfo[arcCells.length];
		for(int i=0; i<arcCells.length; i++)
		{
			Cell np = arcCells[i];
			allArcs[i] = ArcInfo.parseCell(np);

			// build a list of examples found in this arc
			List<Example> neList = Example.getExamples(np, false, error, null);
			if (neList == null) return null;
			if (neList.size() > 1)
			{
				error.markError(null, np, "Can only be one drawing of an arc, but more were found");
				return null;
			}
			Example arcEx = neList.get(0);

			// sort the layers in the example
			Collections.sort(arcEx.samples, new SamplesByLayerOrder(lList));

			// get width and polygon count information
			double hWid = -1;
			int count = 0;
			for(Sample ns : arcEx.samples)
			{
				double wid = Math.min(ns.node.getXSize(), ns.node.getYSize());
				if (ns.layer == null) hWid = wid; else count++;
			}

			// error if there is no highlight box
			if (hWid < 0)
			{
				error.markError(null, np, "No highlight layer found");
				return null;
			}
			allArcs[i].arcDetails = new ArcInfo.LayerDetails[count];

			// fill the individual arc layer structures
			int layerIndex = 0;
			for(int k=0; k<2; k++)
			{
				for(Sample ns : arcEx.samples)
				{
					if (ns.layer == null) continue;

					// get the layer index
					String sampleLayer = ns.layer.getName().substring(6);
					LayerInfo li = null;
					for(int j=0; j<lList.length; j++)
					{
						if (sampleLayer.equals(lList[j].name)) { li = lList[j];   break; }
					}
					if (li == null)
					{
						error.markError(ns.node, np, "Unknown layer: " + sampleLayer);
						return null;
					}

					// only add transparent layers when k=0
					if (k == 0)
					{
						if (li.desc.getTransparentLayer() == 0) continue;
					} else
					{
						if (li.desc.getTransparentLayer() != 0) continue;
					}
					allArcs[i].arcDetails[layerIndex] = new ArcInfo.LayerDetails();
					allArcs[i].arcDetails[layerIndex].layer = li;

					// determine the style of this arc layer
					Poly.Type style = Poly.Type.CLOSED;
					if (ns.node.getProto() == Artwork.tech().filledBoxNode)
						style = Poly.Type.FILLED;
					allArcs[i].arcDetails[layerIndex].style = style;

					// determine the width offset of this arc layer
					double wid = Math.min(ns.node.getXSize(), ns.node.getYSize());
					allArcs[i].arcDetails[layerIndex].width = wid;

					layerIndex++;
				}
			}
		}
		return allArcs;
	}

	/************************************* NODE ANALYSIS *************************************/

	/**
	 * Method to scan the "dependentlibcount" libraries in "dependentLibs",
	 * and build the node structures for it in technology "tech".
	 */
	private NodeInfo [] extractNodes(Library [] dependentLibs, LayerInfo [] lList, ArcInfo [] aList)
	{
		Cell [] nodeCells = Info.findCellSequence(dependentLibs, "node-", Info.NODESEQUENCE_KEY);
		if (nodeCells.length <= 0)
		{
			System.out.println("No nodes found");
			return null;
		}

		List<NodeInfo> nList = new ArrayList<NodeInfo>();

		// get the nodes
		for(int pass=0; pass<3; pass++)
			for(Cell np : nodeCells)
		{
			// make sure this is the right type of node for this pass of the nodes
			NodeInfo nIn = NodeInfo.parseCell(np);

			// only want pins on pass 0, pure-layer nodes on pass 2
			if (pass == 0 && !nIn.func.isPin()) continue;
			if (pass == 1 && (nIn.func.isPin() || nIn.func == PrimitiveNode.Function.NODE)) continue;
			if (pass == 2 && nIn.func != PrimitiveNode.Function.NODE) continue;
			if (nIn.func == PrimitiveNode.Function.NODE)
			{
				if (nIn.serp)
				{
					error.markError(null, np, "Pure layer " + nIn.name + " can not be serpentine");
					return null;
				}
				nIn.specialType = PrimitiveNode.POLYGONAL;
			}

			// build a list of examples found in this node
			List<Example> variations = new ArrayList<Example>();
			List<Example> neList = Example.getExamples(np, true, error, variations);
			if (neList == null || neList.size() == 0)
			{
				System.out.println("Cannot analyze " + np);
				return null;
			}

			// sort the layers in the main example
			Example firstEx = neList.get(0);
			Collections.sort(firstEx.samples, new SamplesByLayerOrder(lList));

			nIn.xSize = firstEx.hx - firstEx.lx;
			nIn.ySize = firstEx.hy - firstEx.ly;
			nIn.name = np.getName().substring(5);
			nList.add(nIn);

			// add in variation names
			if (variations.size() > 0)
			{
				nIn.primitiveNodeGroupNames = new ArrayList<String>();
				nIn.primitiveNodeGroupNames.add(nIn.name);
				for(Example variation : variations)
				{
					for(Sample s : variation.samples)
					{
						if (!s.node.getNameKey().isTempname())
						{
							nIn.primitiveNodeGroupNames.add(s.node.getName());
							break;
						}
					}
				}
			}

			// associate the samples in each example
			if (associateExamples(neList, np)) return null;

			// derive primitives from the examples
			nIn.nodeLayers = makePrimitiveNodeLayers(neList, np, lList, variations);
			if (nIn.nodeLayers == null) return null;

			if (fillPrimitivePorts(np, nIn, firstEx, aList)) return null;
			if (fillSizeOffset(np, firstEx, nIn)) return null;
		}
		NodeInfo [] nListArray = new NodeInfo[nList.size()];
		for(int i=0; i<nList.size(); i++)
			nListArray[i] = nList.get(i);
		return nListArray;
	}

	/**
	 * Method to load the size offset information for a new PrimitiveNode.
	 * @param np the Cell that describes the PrimitiveNode.
	 * @param firstEx the Example that describes the PrimitiveNode.
	 * @param nIn the new PrimitiveNode information being created.
	 * @return true on error.
	 */
	private boolean fillSizeOffset(Cell np, Example firstEx, NodeInfo nIn)
	{
		// extract width offset information from highlight box
		double lX = 0, hX = 0, lY = 0, hY = 0;
		boolean found = false;
		for(Sample ns : firstEx.samples)
		{
			if (ns.layer != null) continue;
			found = true;
			if (ns.values != null)
			{
				boolean err = false;
				if (ns.values[0].getX().getMultiplier() == -0.5)		// left edge offset
				{
					lX = ns.values[0].getX().getAdder();
				} else if (ns.values[0].getX().getMultiplier() == 0.5)
				{
					lX = nIn.xSize + ns.values[0].getX().getAdder();
				} else err = true;
				if (ns.values[0].getY().getMultiplier() == -0.5)		// bottom edge offset
				{
					lY = ns.values[0].getY().getAdder();
				} else if (ns.values[0].getY().getMultiplier() == 0.5)
				{
					lY = nIn.ySize + ns.values[0].getY().getAdder();;
				} else err = true;
				if (ns.values[1].getX().getMultiplier() == 0.5)		// right edge offset
				{
					hX = -ns.values[1].getX().getAdder();
				} else if (ns.values[1].getX().getMultiplier() == -0.5)
				{
					hX = nIn.xSize - ns.values[1].getX().getAdder();
				} else err = true;
				if (ns.values[1].getY().getMultiplier() == 0.5)		// top edge offset
				{
					hY = -ns.values[1].getY().getAdder();
				} else if (ns.values[1].getY().getMultiplier() == -0.5)
				{
					hY = nIn.ySize - ns.values[1].getY().getAdder();
				} else err = true;
				if (err)
				{
					error.markError(ns.node, np, "Highlighting cannot scale from center");
					return true;
				}
			} else
			{
				error.markError(ns.node, np, "No rule found for highlight");
				return true;
			}
		}
		if (!found)
		{
			error.markError(null, np, "No highlight found");
			return true;
		}
		if (lX != 0 || hX != 0 || lY != 0 || hY != 0)
		{
			nIn.so = new SizeOffset(lX, hX, lY, hY);
		}

//		// get grab point information
//		for(ns = neList.firstSample; ns != NOSAMPLE; ns = ns.nextSample)
//			if (ns.layer == Generic.tech.cellCenterNode) break;
//		if (ns != NOSAMPLE)
//		{
//			us_tecnode_grab[us_tecnode_grabcount++] = nodeindex+1;
//			us_tecnode_grab[us_tecnode_grabcount++] = (ns.node.geom.lowx +
//				ns.node.geom.highx - neList.lx - neList.hx)/2 *
//				el_curlib.lambda[tech.techindex] / lambda;
//			us_tecnode_grab[us_tecnode_grabcount++] = (ns.node.geom.lowy +
//				ns.node.geom.highy - neList.ly - neList.hy)/2 *
//				el_curlib.lambda[tech.techindex] / lambda;
//			us_tecflags |= HASGRAB;
//		}
		return false;
	}

	/**
	 * Method to fill the ports in a newly-built primitiveNode.
	 * @param np the Cell that describes the PrimitiveNode.
	 * @param nIn the new PrimitiveNode information being created.
	 * @param firstEx the Example that describes the PrimitiveNode.
	 * @param aList the list of Arcs already in the new Technology.
	 * @return true on error.
	 */
	private boolean fillPrimitivePorts(Cell np, NodeInfo nIn, Example firstEx, ArcInfo [] aList)
	{
		Netlist netList = np.getNetlist();
		if (netList == null)
		{
			System.out.println("Sorry, a deadlock technology generation (network information unavailable).  Please try again");
			return true;
		}

		// fill the port structures
		List<NodeInfo.PortDetails> ports = new ArrayList<NodeInfo.PortDetails>();
		Map<NodeInfo.PortDetails,Sample> portSamples = new HashMap<NodeInfo.PortDetails,Sample>();
		for(Sample ns : firstEx.samples)
		{
			if (ns.layer != Generic.tech().portNode) continue;

			// port connections
			NodeInfo.PortDetails nipd = new NodeInfo.PortDetails();
			portSamples.put(nipd, ns);

			// port name
			nipd.name = Info.getPortName(ns.node);
			if (nipd.name == null)
			{
				error.markError(ns.node, np, "Port does not have a name");
				return true;
			}
			for(int c=0; c<nipd.name.length(); c++)
			{
				char str = nipd.name.charAt(c);
				if (str <= ' ' || str >= 0177)
				{
					error.markError(ns.node, np, "Invalid port name");
					return true;
				}
			}

			// port angle and range
			nipd.angle = 0;
			Variable varAngle = ns.node.getVar(Info.PORTANGLE_KEY);
			if (varAngle != null)
				nipd.angle = ((Integer)varAngle.getObject()).intValue();
			nipd.range = 180;
			Variable varRange = ns.node.getVar(Info.PORTRANGE_KEY);
			if (varRange != null)
				nipd.range = ((Integer)varRange.getObject()).intValue();

			// port area rule
			nipd.values = ns.values;
			ports.add(nipd);
		}

		// sort the ports by name within angle
		Collections.sort(ports, new PortsByAngleAndName());

		// now find the poly/active ports for transistor rearranging
		int pol1Port = -1, pol2Port = -1, dif1Port = -1, dif2Port = -1;
		int m1Port1 = -1, m1Port2 = -1, m2Port1 = -1, m2Port2 = -1;
		for(int i=0; i<ports.size(); i++)
		{
			NodeInfo.PortDetails nipd = ports.get(i);
			Sample ns = portSamples.get(nipd);

			nipd.connections = new ArcInfo[0];
			Variable var = ns.node.getVar(Info.CONNECTION_KEY);
			if (var != null)
			{
				// convert "arc-CELL" pointers to indices
				CellId [] arcCells = (CellId [])var.getObject();
				List<ArcInfo> validArcCells = new ArrayList<ArcInfo>();
				for(int j=0; j<arcCells.length; j++)
				{
					// find arc that connects
					if (arcCells[j] == null) continue;
					Cell arcCell = EDatabase.serverDatabase().getCell(arcCells[j]);
					if (arcCell == null) continue;
					String cellName = arcCell.getName().substring(4);
					for(int k=0; k<aList.length; k++)
					{
						if (aList[k].name.equalsIgnoreCase(cellName))
						{
							validArcCells.add(aList[k]);
							break;
						}
					}
				}
				ArcInfo [] connections = new ArcInfo[validArcCells.size()];
				nipd.connections = connections;
				for(int j=0; j<validArcCells.size(); j++)
					connections[j] = validArcCells.get(j);
				for(int j=0; j<connections.length; j++)
				{
					// find port characteristics for possible transistors
					Variable meaningVar = ns.node.getVar(Info.PORTMEANING_KEY);
					int meaning = 0;
					if (meaningVar != null) meaning = ((Integer)meaningVar.getObject()).intValue();
					Function fun = connections[j].func;
					if (fun.isPoly() || meaning == 1)
					{
						if (pol1Port < 0) pol1Port = i; else
							if (pol2Port < 0) pol2Port = i;
					} else if (fun.isDiffusion() || meaning == 2)
					{
						if (dif1Port < 0) dif1Port = i; else
							if (dif2Port < 0) dif2Port = i;
					}
					if ((fun.isMetal() && fun.getLevel() == 1) || meaning == 1)
					{
						if (m1Port1 < 0) m1Port1 = i; else
							if (m1Port2 < 0) m1Port2 = i;
					} else if ((fun.isMetal() && fun.getLevel() == 2) || meaning == 2)
					{
						if (m2Port1 < 0) m2Port1 = i; else
							if (m2Port2 < 0) m2Port2 = i;
					}
				}
			}
		}

		// save the ports in an array
		nIn.nodePortDetails = new NodeInfo.PortDetails[ports.size()];
		for(int j=0; j<ports.size(); j++) nIn.nodePortDetails[j] = ports.get(j);

		// establish port connectivity
		for(int i=0; i<nIn.nodePortDetails.length; i++)
		{
			NodeInfo.PortDetails nipd = nIn.nodePortDetails[i];
			Sample ns = portSamples.get(nipd);
			nipd.netIndex = i;
			if (ns.node.hasConnections())
			{
				ArcInst ai1 = ns.node.getConnections().next().getArc();
				Network net1 = netList.getNetwork(ai1, 0);
				for(int j=0; j<i; j++)
				{
					NodeInfo.PortDetails onipd = nIn.nodePortDetails[j];
					Sample oNs = portSamples.get(onipd);
					if (oNs.node.hasConnections())
					{
						ArcInst ai2 = oNs.node.getConnections().next().getArc();
						Network net2 = netList.getNetwork(ai2, 0);
						if (net1 == net2)
						{
							nipd.netIndex = j;
							break;
						}
					}
				}
			}
		}

		// on MOS transistors, make sure the first 4 ports are poly/active/poly/active
		if (nIn.func.isFET())
		{
			if (pol1Port < 0 || pol2Port < 0 || dif1Port < 0 || dif2Port < 0)
			{
				// in TFT transistors, its metal1 crossing metal2
				if (m1Port1 >= 0 && m1Port2 >= 0 && m2Port1 >= 0 && m2Port2 >= 0)
				{
					pol1Port = m1Port1;
					pol2Port = m1Port2;
					dif1Port = m2Port1;
					dif2Port = m2Port2;					
				} else
				{
					error.markError(null, np, "Need 2 gate (poly/m1) and 2 gated (active/m2) ports on field-effect transistor");
					return true;
				}
			}

			// also make sure that dif1Port is positive and dif2Port is negative
			double x1Pos = (nIn.nodePortDetails[dif1Port].values[0].getX().getMultiplier() * nIn.xSize +
				nIn.nodePortDetails[dif1Port].values[0].getX().getAdder() +
				nIn.nodePortDetails[dif1Port].values[1].getX().getMultiplier() * nIn.xSize +
				nIn.nodePortDetails[dif1Port].values[1].getX().getAdder()) / 2;
			double x2Pos = (nIn.nodePortDetails[dif2Port].values[0].getX().getMultiplier() * nIn.xSize +
				nIn.nodePortDetails[dif2Port].values[0].getX().getAdder() +
				nIn.nodePortDetails[dif2Port].values[1].getX().getMultiplier() * nIn.xSize +
				nIn.nodePortDetails[dif2Port].values[1].getX().getAdder()) / 2;
			double y1Pos = (nIn.nodePortDetails[dif1Port].values[0].getY().getMultiplier() * nIn.ySize +
				nIn.nodePortDetails[dif1Port].values[0].getY().getAdder() +
				nIn.nodePortDetails[dif1Port].values[1].getY().getMultiplier() * nIn.ySize +
				nIn.nodePortDetails[dif1Port].values[1].getY().getAdder()) / 2;
			double y2Pos = (nIn.nodePortDetails[dif2Port].values[0].getY().getMultiplier() * nIn.ySize +
				nIn.nodePortDetails[dif2Port].values[0].getY().getAdder() +
				nIn.nodePortDetails[dif2Port].values[1].getY().getMultiplier() * nIn.ySize +
				nIn.nodePortDetails[dif2Port].values[1].getY().getAdder()) / 2;
			if (Math.abs(x1Pos-x2Pos) > Math.abs(y1Pos-y2Pos))
			{
				if (x1Pos < x2Pos)
				{
					int k = dif1Port;   dif1Port = dif2Port;   dif2Port = k;
				}
			} else
			{
				if (y1Pos < y2Pos)
				{
					int k = dif1Port;   dif1Port = dif2Port;   dif2Port = k;
				}
			}

			// also make sure that pol1Port is negative and pol2Port is positive
			x1Pos = (nIn.nodePortDetails[pol1Port].values[0].getX().getMultiplier() * nIn.xSize +
				nIn.nodePortDetails[pol1Port].values[0].getX().getAdder() +
				nIn.nodePortDetails[pol1Port].values[1].getX().getMultiplier() * nIn.xSize +
				nIn.nodePortDetails[pol1Port].values[1].getX().getAdder()) / 2;
			x2Pos = (nIn.nodePortDetails[pol2Port].values[0].getX().getMultiplier() * nIn.xSize +
				nIn.nodePortDetails[pol2Port].values[0].getX().getAdder() +
				nIn.nodePortDetails[pol2Port].values[1].getX().getMultiplier() * nIn.xSize +
				nIn.nodePortDetails[pol2Port].values[1].getX().getAdder()) / 2;
			y1Pos = (nIn.nodePortDetails[pol1Port].values[0].getY().getMultiplier() * nIn.ySize +
				nIn.nodePortDetails[pol1Port].values[0].getY().getAdder() +
				nIn.nodePortDetails[pol1Port].values[1].getY().getMultiplier() * nIn.ySize +
				nIn.nodePortDetails[pol1Port].values[1].getY().getAdder()) / 2;
			y2Pos = (nIn.nodePortDetails[pol2Port].values[0].getY().getMultiplier() * nIn.ySize +
				nIn.nodePortDetails[pol2Port].values[0].getY().getAdder() +
				nIn.nodePortDetails[pol2Port].values[1].getY().getMultiplier() * nIn.ySize +
				nIn.nodePortDetails[pol2Port].values[1].getY().getAdder()) / 2;
			if (Math.abs(x1Pos-x2Pos) > Math.abs(y1Pos-y2Pos))
			{
				if (x1Pos > x2Pos)
				{
					int k = pol1Port;   pol1Port = pol2Port;   pol2Port = k;
				}
			} else
			{
				if (y1Pos > y2Pos)
				{
					int k = pol1Port;   pol1Port = pol2Port;   pol2Port = k;
				}
			}

			// gather extra ports that go at the end
			List<NodeInfo.PortDetails> extras = new ArrayList<NodeInfo.PortDetails>();
			for(int j=0; j<ports.size(); j++)
			{
				if (j != pol1Port && j != dif1Port && j != pol2Port && j != dif2Port)
					extras.add(ports.get(j));
			}

			// rearrange the ports
			NodeInfo.PortDetails port0 = nIn.nodePortDetails[pol1Port];
			NodeInfo.PortDetails port1 = nIn.nodePortDetails[dif1Port];
			NodeInfo.PortDetails port2 = nIn.nodePortDetails[pol2Port];
			NodeInfo.PortDetails port3 = nIn.nodePortDetails[dif2Port];
			nIn.nodePortDetails[pol1Port=0] = port0;
			nIn.nodePortDetails[dif1Port=1] = port1;
			nIn.nodePortDetails[pol2Port=2] = port2;
			nIn.nodePortDetails[dif2Port=3] = port3;
			for(int j=0; j<extras.size(); j++)
				nIn.nodePortDetails[j+4] = extras.get(j);

			// make sure implant layers are not connected to ports
			for(int k=0; k<nIn.nodeLayers.length; k++)
			{
				NodeInfo.LayerDetails nld = nIn.nodeLayers[k];
				if (nld.layer.fun.isSubstrate()) nld.portIndex = -1;
			}
		}

		if (nIn.serp)
		{
			// finish up serpentine transistors
			nIn.specialType = PrimitiveNode.SERPTRANS;

			// determine port numbers for serpentine transistors
			int polIndex = -1, difIndex = -1;
			for(int k=0; k<nIn.nodeLayers.length; k++)
			{
				NodeInfo.LayerDetails nld = nIn.nodeLayers[k];
				if (nld.layer.fun.isPoly())
				{
					polIndex = k;
				} else if (nld.layer.fun.isDiff())
				{
					if (difIndex >= 0)
					{
						// figure out which layer is the basic active layer
						int funExtraOld = nIn.nodeLayers[difIndex].layer.funExtra;
						int funExtraNew = nld.layer.funExtra;
						if (funExtraOld == funExtraNew) continue;
						if (funExtraOld == 0) continue;
					}
					difIndex = k;
				}
			}
			if (difIndex < 0 || polIndex < 0)
			{
				error.markError(null, np, "No diffusion and polysilicon layers in serpentine transistor");
				return true;
			}

			// find width and extension from comparison to poly layer
			Sample polNs = nIn.nodeLayers[polIndex].ns;
			Rectangle2D polNodeBounds = polNs.node.getBounds();
			Sample difNs = nIn.nodeLayers[difIndex].ns;
			Rectangle2D difNodeBounds = difNs.node.getBounds();
			for(int k=0; k<nIn.nodeLayers.length; k++)
			{
				NodeInfo.LayerDetails nld = nIn.nodeLayers[k];
				Sample ns = nld.ns;
				Rectangle2D nodeBounds = ns.node.getBounds();
				if (polNodeBounds.getWidth() > polNodeBounds.getHeight())
				{
					// horizontal layer
					nld.lWidth = nodeBounds.getMaxY() - (ns.parent.ly + ns.parent.hy)/2;
					nld.rWidth = (ns.parent.ly + ns.parent.hy)/2 - nodeBounds.getMinY();
					nld.extendT = difNodeBounds.getMinX() - nodeBounds.getMinX();
				} else
				{
					// vertical layer
					nld.lWidth = nodeBounds.getMaxX() - (ns.parent.lx + ns.parent.hx)/2;
					nld.rWidth = (ns.parent.lx + ns.parent.hx)/2 - nodeBounds.getMinX();
					nld.extendT = difNodeBounds.getMinY() - nodeBounds.getMinY();
				}
				nld.extendB = nld.extendT;
			}

			// add in electrical layers for diffusion
			NodeInfo.LayerDetails [] addedLayers = new NodeInfo.LayerDetails[nIn.nodeLayers.length+2];
			for(int k=0; k<nIn.nodeLayers.length; k++)
				addedLayers[k] = nIn.nodeLayers[k];
			NodeInfo.LayerDetails diff1 = nIn.nodeLayers[difIndex].duplicate();
			NodeInfo.LayerDetails diff2 = nIn.nodeLayers[difIndex].duplicate();
			addedLayers[nIn.nodeLayers.length] = diff1;
			addedLayers[nIn.nodeLayers.length+1] = diff2;
			nIn.nodeLayers = addedLayers;
			diff1.inLayers = diff2.inLayers = false;
			nIn.nodeLayers[difIndex].inElectricalLayers = false;
			diff1.portIndex = dif1Port;
			diff2.portIndex = dif2Port;

			// compute port extension factors
			nIn.specialValues = new double[6];
			int layerCount = 0;
			for(Sample ns : firstEx.samples)
			{
				if (ns.values != null && ns.layer != Generic.tech().portNode &&
					ns.layer != Generic.tech().cellCenterNode && ns.layer != null)
						layerCount++;
			}
			nIn.specialValues[0] = layerCount+1;
			if (nIn.nodePortDetails[dif1Port].values[0].getX().getAdder() >
				nIn.nodePortDetails[dif1Port].values[0].getY().getAdder())
			{
				// vertical diffusion layer: determine polysilicon width
				nIn.specialValues[3] = (nIn.ySize * nIn.nodeLayers[polIndex].values[1].getY().getMultiplier() +
					nIn.nodeLayers[polIndex].values[1].getY().getAdder()) -
					(nIn.ySize * nIn.nodeLayers[polIndex].values[0].getY().getMultiplier() +
					nIn.nodeLayers[polIndex].values[0].getY().getAdder());

				// determine diffusion port rule
				nIn.specialValues[1] = (nIn.xSize * nIn.nodePortDetails[dif1Port].values[0].getX().getMultiplier() +
					nIn.nodePortDetails[dif1Port].values[0].getX().getAdder()) -
					(nIn.xSize * nIn.nodeLayers[difIndex].values[0].getX().getMultiplier() +
					nIn.nodeLayers[difIndex].values[0].getX().getAdder());
				nIn.specialValues[2] = (nIn.ySize * nIn.nodePortDetails[dif1Port].values[0].getY().getMultiplier() +
					nIn.nodePortDetails[dif1Port].values[0].getY().getAdder()) -
					(nIn.ySize * nIn.nodeLayers[polIndex].values[1].getY().getMultiplier() +
					nIn.nodeLayers[polIndex].values[1].getY().getAdder());

				// determine polysilicon port rule
				nIn.specialValues[4] = (nIn.ySize * nIn.nodePortDetails[pol1Port].values[0].getY().getMultiplier() +
					nIn.nodePortDetails[pol1Port].values[0].getY().getAdder()) -
					(nIn.ySize * nIn.nodeLayers[polIndex].values[0].getY().getMultiplier() +
					nIn.nodeLayers[polIndex].values[0].getY().getAdder());
				nIn.specialValues[5] = (nIn.xSize * nIn.nodeLayers[difIndex].values[0].getX().getMultiplier() +
					nIn.nodeLayers[difIndex].values[0].getX().getAdder()) -
					(nIn.xSize * nIn.nodePortDetails[pol1Port].values[1].getX().getMultiplier() +
					nIn.nodePortDetails[pol1Port].values[1].getX().getAdder());

				// setup electrical layers for diffusion
				diff1.values[0] = diff1.values[0].withX(new EdgeH(0,0));
//                diff1.values[0].getY().setMultiplier(0);
//				diff1.values[0].getY().setAdder(0);
				diff1.rWidth = 0;
				diff2.values[1] = diff2.values[0].withY(new EdgeV(0,0));
//				diff2.values[1].getY().setMultiplier(0);
//				diff2.values[1].getY().setAdder(0);
				diff2.lWidth = 0;
			} else
			{
				// horizontal diffusion layer: determine polysilicon width
				nIn.specialValues[3] = (nIn.xSize * nIn.nodeLayers[polIndex].values[1].getX().getMultiplier() +
					nIn.nodeLayers[polIndex].values[1].getX().getAdder()) -
					(nIn.xSize * nIn.nodeLayers[polIndex].values[0].getX().getMultiplier() +
					nIn.nodeLayers[polIndex].values[0].getX().getAdder());

				// determine diffusion port rule
				nIn.specialValues[1] = (nIn.ySize * nIn.nodePortDetails[dif1Port].values[0].getY().getMultiplier() +
					nIn.nodePortDetails[dif1Port].values[0].getY().getAdder()) -
					(nIn.ySize * nIn.nodeLayers[difIndex].values[0].getY().getMultiplier() +
					nIn.nodeLayers[difIndex].values[0].getY().getAdder());
				nIn.specialValues[2] = (nIn.xSize * nIn.nodeLayers[polIndex].values[0].getX().getMultiplier() +
					nIn.nodeLayers[polIndex].values[0].getX().getAdder()) -
					(nIn.xSize * nIn.nodePortDetails[dif1Port].values[1].getX().getMultiplier() +
					nIn.nodePortDetails[dif1Port].values[1].getX().getAdder());

				// determine polysilicon port rule
				nIn.specialValues[4] = (nIn.xSize * nIn.nodePortDetails[pol1Port].values[0].getX().getMultiplier() +
					nIn.nodePortDetails[pol1Port].values[0].getX().getAdder()) -
					(nIn.xSize * nIn.nodeLayers[polIndex].values[0].getX().getMultiplier() +
					nIn.nodeLayers[polIndex].values[0].getX().getAdder());
				nIn.specialValues[5] = (nIn.ySize * nIn.nodeLayers[difIndex].values[0].getY().getMultiplier() +
					nIn.nodeLayers[difIndex].values[0].getY().getAdder()) -
					(nIn.ySize * nIn.nodePortDetails[pol1Port].values[1].getY().getMultiplier() +
					nIn.nodePortDetails[pol1Port].values[1].getY().getAdder());

				// setup electrical layers for diffusion
                diff1.values[0] = diff1.values[0].withX(new EdgeH(0, 0));
//				diff1.values[0].getX().setMultiplier(0);
//				diff1.values[0].getX().setAdder(0);
				diff1.rWidth = 0;
                diff2.values[1] = diff2.values[1].withX(new EdgeH(0, 0));
//				diff2.values[1].getX().setMultiplier(0);
//				diff2.values[1].getX().setAdder(0);
				diff2.lWidth = 0;
			}
		}
		return false;
	}

	private NodeInfo.LayerDetails [] makePrimitiveNodeLayers(List<Example> neList, Cell np, LayerInfo [] lis,
		List<Example> variations)
	{
		// if there is only one example: make sample scale with edge
		Example firstEx = neList.get(0);
		if (neList.size() <= 1)
		{
			return makeNodeScaledUniformly(neList, np, lis, null, null);
		}

		List<NodeInfo.LayerDetails> nodeLayersList = new ArrayList<NodeInfo.LayerDetails>();

		// look at every sample "ns" in the main example "neList"
		for(Sample ns : firstEx.samples)
		{
			// ignore grab point specification
			if (ns.layer == Generic.tech().cellCenterNode) continue;
			AffineTransform trans = ns.node.rotateOut();
			Rectangle2D nodeBounds = getBoundingBox(ns.node);

			// determine the layer
			LayerInfo giLayer = null;
			if (ns.layer != null && ns.layer != Generic.tech().portNode)
			{
				String desiredLayer = ns.layer.getName().substring(6);
				for(int i=0; i<lis.length; i++)
				{
					if (desiredLayer.equals(lis[i].name)) { giLayer = lis[i];   break; }
				}
				if (giLayer == null)
				{
					System.out.println("Cannot find layer " + desiredLayer);
					return null;
				}
			}

			// look at other examples and find samples associated with this
			firstEx.studySample = ns;
			NodeInfo.LayerDetails multiRule = null;
			for(int n=1; n<neList.size(); n++)
			{
				Example ne = neList.get(n);

				// count number of samples associated with the main sample
				int total = 0;
				for(Sample nso : ne.samples)
				{
					if (nso.assoc == ns)
					{
						ne.studySample = nso;
						total++;
					}
				}
				if (total == 0)
				{
					error.markError(ns.node, np, "Still unassociated sample (shouldn't happen)");
					return null;
				}

				// if there are multiple associations, it must be a contact cut
				if (total > 1)
				{
					// make sure the layer is real geometry, not highlight or a port
					if (ns.layer == null || ns.layer == Generic.tech().portNode)
					{
						error.markError(ns.node, np, "Only contact layers may be iterated in examples");
						return null;
					}

					// add the rule
					multiRule = getMultiCutRule(ns, neList, np);
					if (multiRule != null) break;
				}
			}
			if (multiRule != null)
			{
				multiRule.layer = giLayer;
				nodeLayersList.add(multiRule);
				continue;
			}

			// associations done for this sample, now analyze them
			Point2D [] pointList = null;
			int [] pointFactor = null;
			Point2D [] points = null;
			if (ns.node.getProto() == Artwork.tech().filledPolygonNode ||
				ns.node.getProto() == Artwork.tech().closedPolygonNode ||
				ns.node.getProto() == Artwork.tech().openedPolygonNode ||
				ns.node.getProto() == Artwork.tech().openedDottedPolygonNode ||
				ns.node.getProto() == Artwork.tech().openedDashedPolygonNode ||
				ns.node.getProto() == Artwork.tech().openedThickerPolygonNode)
			{
				points = ns.node.getTrace();
			}
			int trueCount = 0;
			int minFactor = 0;
			if (points != null)
			{
				// make sure the arrays hold "count" points
				pointList = new Point2D[points.length];
				pointFactor = new int[points.length];
				for(int i=0; i<points.length; i++)
				{
					pointList[i] = new Point2D.Double(nodeBounds.getCenterX() + points[i].getX(),
						nodeBounds.getCenterY() + points[i].getY());
					trans.transform(pointList[i], pointList[i]);
				}
				trueCount = points.length;
			} else
			{
				double [] angles = null;
				if (ns.node.getProto() == Artwork.tech().circleNode || ns.node.getProto() == Artwork.tech().thickCircleNode)
				{
					angles = ns.node.getArcDegrees();
					if (angles[0] == 0 && angles[1] == 0) angles = null;
				}
//				if (angles == null)
//				{
//					Variable var2 = ns.node.getVar(Info.MINSIZEBOX_KEY);
//					if (var2 != null) minFactor = 2;
//				}

				// set sample description
				if (angles != null)
				{
					// handle circular arc sample
					pointList = new Point2D[3];
					pointFactor = new int[3];
					pointList[0] = new Point2D.Double(nodeBounds.getCenterX(), nodeBounds.getCenterY());
					double dist = nodeBounds.getMaxX() - nodeBounds.getCenterX();
					pointList[1] = new Point2D.Double(nodeBounds.getCenterX() + dist * Math.cos(angles[0]),
						nodeBounds.getCenterY() + dist * Math.sin(angles[0]));
					trans.transform(pointList[1], pointList[1]);
					trueCount = 3;
				} else if (ns.node.getProto() == Artwork.tech().circleNode || ns.node.getProto() == Artwork.tech().thickCircleNode ||
					ns.node.getProto() == Artwork.tech().filledCircleNode)
				{
					// handle circular sample
					pointList = new Point2D[2+minFactor];
					pointFactor = new int[2+minFactor];
					pointList[0] = new Point2D.Double(nodeBounds.getCenterX(), nodeBounds.getCenterY());
					pointList[1] = new Point2D.Double(nodeBounds.getMaxX(), nodeBounds.getCenterY());
					trueCount = 2;
				} else
				{
					// rectangular sample: get the bounding box in (pointListx, pointListy)
					pointList = new Point2D[2+minFactor];
					pointFactor = new int[2+minFactor];
					pointList[0] = new Point2D.Double(nodeBounds.getMinX(), nodeBounds.getMinY());
					pointList[1] = new Point2D.Double(nodeBounds.getMaxX(), nodeBounds.getMaxY());
					trueCount = 2;
				}
//				if (minFactor > 1)
//				{
//					pointList[2] = new Point2D.Double(pointList[0].getX(),pointList[0].getY());
//					pointList[3] = new Point2D.Double(pointList[1].getX(),pointList[1].getY());
//				}
			}

			double [] pointLeftDist = new double[pointFactor.length];
			double [] pointRightDist = new double[pointFactor.length];
			double [] pointBottomDist = new double[pointFactor.length];
			double [] pointTopDist = new double[pointFactor.length];
			double [] centerXDist = new double[pointFactor.length];
			double [] centerYDist = new double[pointFactor.length];
			double [] pointXRatio = new double[pointFactor.length];
			double [] pointYRatio = new double[pointFactor.length];
			for(int i=0; i<pointFactor.length; i++)
			{
				pointLeftDist[i] = pointList[i].getX() - firstEx.lx;
				pointRightDist[i] = firstEx.hx - pointList[i].getX();
				pointBottomDist[i] = pointList[i].getY() - firstEx.ly;
				pointTopDist[i] = firstEx.hy - pointList[i].getY();
				centerXDist[i] = pointList[i].getX() - (firstEx.lx+firstEx.hx)/2;
				centerYDist[i] = pointList[i].getY() - (firstEx.ly+firstEx.hy)/2;
				if (firstEx.hx == firstEx.lx) pointXRatio[i] = 0; else
					pointXRatio[i] = (pointList[i].getX() - (firstEx.lx+firstEx.hx)/2) / (firstEx.hx-firstEx.lx);
				if (firstEx.hy == firstEx.ly) pointYRatio[i] = 0; else
					pointYRatio[i] = (pointList[i].getY() - (firstEx.ly+firstEx.hy)/2) / (firstEx.hy-firstEx.ly);
				if (i < trueCount)
					pointFactor[i] = TOEDGELEFT | TOEDGERIGHT | TOEDGETOP | TOEDGEBOT | FROMCENTX |
						FROMCENTY | RATIOCENTX | RATIOCENTY; else
							pointFactor[i] = FROMCENTX | FROMCENTY;
			}

			Point2D [] pointCoords = new Point2D[pointFactor.length];
			for(int n = 1; n<neList.size(); n++)
			{
				Example ne = neList.get(n);
				NodeInst ni = ne.studySample.node;
				AffineTransform oTrans = ni.rotateOut();
				Rectangle2D oNodeBounds = getBoundingBox(ni);
				Point2D [] oPoints = null;
				if (ni.getProto() == Artwork.tech().filledPolygonNode ||
					ni.getProto() == Artwork.tech().closedPolygonNode ||
					ni.getProto() == Artwork.tech().openedPolygonNode ||
					ni.getProto() == Artwork.tech().openedDottedPolygonNode ||
					ni.getProto() == Artwork.tech().openedDashedPolygonNode ||
					ni.getProto() == Artwork.tech().openedThickerPolygonNode)
				{
					oPoints = ni.getTrace();
				}
				int newCount = 2;
				if (oPoints != null)
				{
					newCount = oPoints.length;
					int numPoints = Math.min(trueCount, newCount);
					int bestOffset = 0;
					double bestDist = Double.MAX_VALUE;
					for(int offset = 0; offset < numPoints; offset++)
					{
						// determine total distance between points
						double dist = 0;
						for(int i=0; i<numPoints; i++)
						{
							double dX = points[i].getX() - oPoints[(i+offset)%numPoints].getX();
							double dY = points[i].getY() - oPoints[(i+offset)%numPoints].getY();
							dist += Math.hypot(dX, dY);
						}
						if (dist < bestDist)
						{
							bestDist = dist;
							bestOffset = offset;
						}
					}
					for(int i=0; i<numPoints; i++)
					{
						pointCoords[i] = new Point2D.Double(oNodeBounds.getCenterX() + oPoints[(i+bestOffset)%numPoints].getX(),
							oNodeBounds.getCenterY() + oPoints[(i+bestOffset)%numPoints].getY());
						oTrans.transform(pointCoords[i], pointCoords[i]);
					}
				} else
				{
					double [] angles = null;
					if (ni.getProto() == Artwork.tech().circleNode || ni.getProto() == Artwork.tech().thickCircleNode)
					{
						angles = ni.getArcDegrees();
						if (angles[0] == 0 && angles[1] == 0) angles = null;
					}
					if (angles != null)
					{
						pointCoords[0] = new Point2D.Double(oNodeBounds.getCenterX(), oNodeBounds.getCenterY());
						double dist = oNodeBounds.getMaxX() - oNodeBounds.getCenterX();
						pointCoords[1] = new Point2D.Double(oNodeBounds.getCenterX() + dist * Math.cos(angles[0]),
							oNodeBounds.getCenterY() + dist * Math.sin(angles[0]));
						oTrans.transform(pointCoords[1], pointCoords[1]);
					} else if (ni.getProto() == Artwork.tech().circleNode || ni.getProto() == Artwork.tech().thickCircleNode ||
						ni.getProto() == Artwork.tech().filledCircleNode)
					{
						pointCoords[0] = new Point2D.Double(oNodeBounds.getCenterX(), oNodeBounds.getCenterY());
						pointCoords[1] = new Point2D.Double(oNodeBounds.getMaxX(), oNodeBounds.getCenterY());
					} else
					{
						pointCoords[0] = new Point2D.Double(oNodeBounds.getMinX(), oNodeBounds.getMinY());
						pointCoords[1] = new Point2D.Double(oNodeBounds.getMaxX(), oNodeBounds.getMaxY());
					}
				}
				if (newCount != trueCount)
				{
					error.markError(ni, np, "Main example of layer " + Info.getSampleName(ne.studySample.layer) +
						" has " + trueCount + " points but this has " + newCount);
					return null;
				}

				for(int i=0; i<trueCount; i++)
				{
					// see if edges are fixed distance from example edge
					if (!DBMath.areEquals(pointLeftDist[i], pointCoords[i].getX() - ne.lx)) pointFactor[i] &= ~TOEDGELEFT;
					if (!DBMath.areEquals(pointRightDist[i], ne.hx - pointCoords[i].getX())) pointFactor[i] &= ~TOEDGERIGHT;
					if (!DBMath.areEquals(pointBottomDist[i], pointCoords[i].getY() - ne.ly)) pointFactor[i] &= ~TOEDGEBOT;
					if (!DBMath.areEquals(pointTopDist[i], ne.hy - pointCoords[i].getY())) pointFactor[i] &= ~TOEDGETOP;

					// see if edges are fixed distance from example center
					if (!DBMath.areEquals(centerXDist[i], pointCoords[i].getX() - (ne.lx+ne.hx)/2)) pointFactor[i] &= ~FROMCENTX;
					if (!DBMath.areEquals(centerYDist[i], pointCoords[i].getY() - (ne.ly+ne.hy)/2)) pointFactor[i] &= ~FROMCENTY;

					// see if edges are fixed ratio from example center
					double r = 0;
					if (ne.hx != ne.lx)
						r = (pointCoords[i].getX() - (ne.lx+ne.hx)/2) / (ne.hx-ne.lx);
					if (!DBMath.areEquals(r, pointXRatio[i])) pointFactor[i] &= ~RATIOCENTX;
					if (ne.hy == ne.ly) r = 0; else
						r = (pointCoords[i].getY() - (ne.ly+ne.hy)/2) / (ne.hy-ne.ly);
					if (!DBMath.areEquals(r, pointYRatio[i])) pointFactor[i] &= ~RATIOCENTY;
				}

				// make sure port information is on the primary example
				if (ns.layer != Generic.tech().portNode) continue;

				// check port angle
				Variable var = ns.node.getVar(Info.PORTANGLE_KEY);
				Variable var2 = ni.getVar(Info.PORTANGLE_KEY);
				if (var == null && var2 != null)
				{
					System.out.println("Warning: moving port angle to main example of " + np);
					ns.node.newVar(Info.PORTANGLE_KEY, var2.getObject());
				}

				// check port range
				var = ns.node.getVar(Info.PORTRANGE_KEY);
				var2 = ni.getVar(Info.PORTRANGE_KEY);
				if (var == null && var2 != null)
				{
					System.out.println("Warning: moving port range to main example of " + np);
					ns.node.newVar(Info.PORTRANGE_KEY, var2.getObject());
				}

				// check connectivity
				var = ns.node.getVar(Info.CONNECTION_KEY);
				var2 = ni.getVar(Info.CONNECTION_KEY);
				if (var == null && var2 != null)
				{
					System.out.println("Warning: moving port connections to main example of " + np);
					ns.node.newVar(Info.CONNECTION_KEY, var2.getObject());
				}
			}

			// error check for the highlight layer
			if (ns.layer == null)
			{
				for(int i=0; i<trueCount; i++)
					if ((pointFactor[i]&(TOEDGELEFT|TOEDGERIGHT)) == 0 ||
						(pointFactor[i]&(TOEDGETOP|TOEDGEBOT)) == 0)
				{
					error.markError(ns.node, np, "Highlight must be constant distance from edge");
					return null;
				}
			}

			// finally, make a rule for this sample
			Technology.TechPoint [] newRule = stretchPoints(pointList, pointFactor, ns, np, neList, neList.get(0));
			if (newRule == null) return null;

			// add the rule to the global list
			ns.msg = Info.getValueOnNode(ns.node);
			if (ns.msg != null && ns.msg.length() == 0) ns.msg = null;
			ns.values = newRule;

			// stop now if a highlight or port object
			if (ns.layer == null || ns.layer == Generic.tech().portNode) continue;

			NodeInfo.LayerDetails nild = new NodeInfo.LayerDetails();
			nodeLayersList.add(nild);
			nild.layer = giLayer;
			nild.ns = ns;
			nild.style = getStyle(ns.node);
			nild.representation = Technology.NodeLayer.POINTS;
			nild.values = fixValues(np, ns.values);
			nild.inNodes = null;
			if (nild.values.length == 2)
			{
				if (nild.style == Poly.Type.CROSSED ||
					nild.style == Poly.Type.FILLED ||
					nild.style == Poly.Type.CLOSED)
				{
					nild.representation = Technology.NodeLayer.BOX;
//					if (minFactor != 0)
//						nodeLayers[count].representation = Technology.NodeLayer.MINBOX;
				}
			}

			// add in variations
			if (variations.size() > 0)
			{
				nild.inNodes = new BitSet();
				nild.inNodes.set(0);
				int variationIndex = 1;
				for(Example variation : variations)
				{
					// align variation with main example and determine overall size
					Sample namedSample = null;
					for(Sample s : variation.samples)
						if (!s.node.getNameKey().isTempname()) { namedSample = s;   break; }
					Sample mainSample = null;
					for(Sample s : neList.get(0).samples)
						if (s.layer == namedSample.layer) { mainSample = s;   break; }
					double lX = neList.get(0).lx - mainSample.xPos + namedSample.xPos;
					double hX = neList.get(0).hx - mainSample.xPos + namedSample.xPos;
					double lY = neList.get(0).ly - mainSample.yPos + namedSample.yPos;
					double hY = neList.get(0).hy - mainSample.yPos + namedSample.yPos;

					Sample samp = null;
					for(Sample s : variation.samples)
						if (s.layer == ns.layer) { samp = s;   break; }
					if (samp != null)
					{
						NodeInfo.LayerDetails variationLayer = new NodeInfo.LayerDetails();
						nodeLayersList.add(variationLayer);
						variationLayer.layer = giLayer;
						variationLayer.ns = samp;
						variationLayer.style = getStyle(samp.node);

						// rectangular sample: get the bounding box in (px, py)		TODO: get this right
						Rectangle2D variationBounds = getBoundingBox(samp.node);
						Technology.TechPoint [] variationRule = new Technology.TechPoint[2];
						EdgeH horiz = EdgeH.fromLeft(variationBounds.getMinX()-lX);
						EdgeV vert = EdgeV.fromBottom(variationBounds.getMinY()-lY);
						variationRule[0] = new Technology.TechPoint(horiz, vert);
						horiz = EdgeH.fromRight(hX-variationBounds.getMaxX());
						vert = EdgeV.fromTop(hY-variationBounds.getMaxY());
						variationRule[1] = new Technology.TechPoint(horiz, vert);
						variationLayer.values = fixValues(np, variationRule);
						variationLayer.representation = Technology.NodeLayer.BOX;
						variationLayer.inNodes = new BitSet();
						variationLayer.inNodes.set(variationIndex);
					}
					variationIndex++;
				}
			}
		}
		NodeInfo.LayerDetails [] nodeLayers = new NodeInfo.LayerDetails[nodeLayersList.size()];
		for(int i=0; i<nodeLayersList.size(); i++)
			nodeLayers[i] = nodeLayersList.get(i);
		return nodeLayers;
	}

	private NodeInfo.LayerDetails [] makeNodeScaledUniformly(List<Example> neList, Cell np, LayerInfo [] lis,
		Example variation, NodeInfo.LayerDetails [] variationDetails)
	{
		List<NodeInfo.LayerDetails> nodeLayers = new ArrayList<NodeInfo.LayerDetails>();
		Example firstEx = neList.get(0);
		for(Sample ns : firstEx.samples)
		{
			if (ns.layer != null && ns.layer != Generic.tech().portNode)
			{
				LayerInfo cutLayer = null;
				String cutLayerName = ns.layer.getName().substring(6);
				for(int i=0; i<lis.length; i++)
				{
					if (cutLayerName.equals(lis[i].name) && lis[i].fun.isContact()) { cutLayer = lis[i];   break; }
				}
				if (cutLayer != null)
				{
					// this is the contact layer
					boolean copied = false;
					for(int i=0; i<variationDetails.length; i++)
					{
						if (variationDetails[i].layer == cutLayer)
						{
							if (variationDetails[i].multiCut)
							{
								// just copy this layer
								nodeLayers.add(variationDetails[i]);
								copied = true;
								break;
							}
						}
					}
					if (copied) continue;
				}
			}

			Sample samp = ns;
			if (variation != null)
			{
				for(Sample s : variation.samples)
				{
					if (s.layer == ns.layer) { samp = s;   break; }
				}
			}
			Rectangle2D nodeBounds = getBoundingBox(samp.node);
			AffineTransform trans = samp.node.rotateOut();

			// see if there is polygonal information
			Point2D [] pointList = null;
			int [] pointFactor = null;
			Point2D [] points = null;
			if (samp.node.getProto() == Artwork.tech().filledPolygonNode ||
				samp.node.getProto() == Artwork.tech().closedPolygonNode ||
				samp.node.getProto() == Artwork.tech().openedPolygonNode ||
				samp.node.getProto() == Artwork.tech().openedDottedPolygonNode ||
				samp.node.getProto() == Artwork.tech().openedDashedPolygonNode ||
				samp.node.getProto() == Artwork.tech().openedThickerPolygonNode)
			{
				points = samp.node.getTrace();
			}
			if (points != null)
			{
				// fill the array
				pointList = new Point2D[points.length];
				pointFactor = new int[points.length];
				for(int i=0; i<points.length; i++)
				{
					pointList[i] = new Point2D.Double(nodeBounds.getCenterX() + points[i].getX(),
						nodeBounds.getCenterY() + points[i].getY());
					trans.transform(pointList[i], pointList[i]);
					pointFactor[i] = RATIOCENTX|RATIOCENTY;
				}
			} else
			{
				// see if it is an arc of a circle
				double [] angles = null;
				if (samp.node.getProto() == Artwork.tech().circleNode || samp.node.getProto() == Artwork.tech().thickCircleNode)
				{
					angles = samp.node.getArcDegrees();
					if (angles[0] == 0 && angles[1] == 0) angles = null;
				}

				// set sample description
				if (angles != null)
				{
					// handle circular arc sample
					pointList = new Point2D[3];
					pointFactor = new int[3];
					pointList[0] = new Point2D.Double(nodeBounds.getCenterX(), nodeBounds.getCenterY());
					double dist = nodeBounds.getMaxX() - nodeBounds.getCenterX();
					pointList[1] = new Point2D.Double(nodeBounds.getCenterX() + dist * Math.cos(angles[0]),
						nodeBounds.getCenterY() + dist * Math.sin(angles[0]));
					trans.transform(pointList[1], pointList[1]);
					pointFactor[0] = FROMCENTX|FROMCENTY;
					pointFactor[1] = RATIOCENTX|RATIOCENTY;
					pointFactor[2] = RATIOCENTX|RATIOCENTY;
				} else if (samp.node.getProto() == Artwork.tech().circleNode || samp.node.getProto() == Artwork.tech().thickCircleNode ||
					samp.node.getProto() == Artwork.tech().filledCircleNode)
				{
					// handle circular sample
					pointList = new Point2D[2];
					pointFactor = new int[2];
					pointList[0] = new Point2D.Double(nodeBounds.getCenterX(), nodeBounds.getCenterY());
					pointList[1] = new Point2D.Double(nodeBounds.getMaxX(), nodeBounds.getCenterY());
					pointFactor[0] = FROMCENTX|FROMCENTY;
					pointFactor[1] = TOEDGERIGHT|FROMCENTY;
				} else
				{
					// rectangular sample: get the bounding box in (px, py)
					pointList = new Point2D[2];
					pointFactor = new int[2];
					pointList[0] = new Point2D.Double(nodeBounds.getMinX(), nodeBounds.getMinY());
					pointList[1] = new Point2D.Double(nodeBounds.getMaxX(), nodeBounds.getMaxY());

					// preset stretch factors to go to the edges of the box
					pointFactor[0] = TOEDGELEFT|TOEDGEBOT;
					pointFactor[1] = TOEDGERIGHT|TOEDGETOP;
				}
			}

			// add the rule to the collection
			Technology.TechPoint [] newRule = stretchPoints(pointList, pointFactor, samp, np, neList, samp.parent);
			if (newRule == null) return null;
			samp.msg = Info.getValueOnNode(samp.node);
			if (samp.msg != null && samp.msg.length() == 0) samp.msg = null;
			samp.values = newRule;

			// stop now if a highlight or port object
			if (samp.layer == null || samp.layer == Generic.tech().portNode) continue;

			// determine the layer
			LayerInfo layer = null;
			String desiredLayer = samp.layer.getName().substring(6);
			for(int i=0; i<lis.length; i++)
			{
				if (desiredLayer.equals(lis[i].name)) { layer = lis[i];   break; }
			}
			if (layer == null)
			{
				error.markError(samp.node, np, "Unknown layer: " + desiredLayer);
				return null;
			}
			NodeInfo.LayerDetails nld = new NodeInfo.LayerDetails();
			nodeLayers.add(nld);
			nld.layer = layer;
			nld.ns = samp;
			nld.style = getStyle(samp.node);
			nld.representation = Technology.NodeLayer.POINTS;
			nld.values = fixValues(np, samp.values);
			if (nld.values.length == 2)
			{
				if (nld.style == Poly.Type.CROSSED ||
					nld.style == Poly.Type.FILLED ||
					nld.style == Poly.Type.CLOSED)
				{
					nld.representation = Technology.NodeLayer.BOX;
//					Variable var2 = ns.node.getVar(Info.MINSIZEBOX_KEY);
//					if (var2 != null) nld.representation = Technology.NodeLayer.MINBOX;
				}
			}
		}
		NodeInfo.LayerDetails[] nodeLayerArray = new NodeInfo.LayerDetails[nodeLayers.size()];
		for(int i=0; i<nodeLayers.size(); i++)
			nodeLayerArray[i] = nodeLayers.get(i);
		return nodeLayerArray;
	}

	/**
	 * Method to build a rule for multiple contact-cut sample "ns" from the
	 * overall example list in "neList".  Returns true on error.
	 */
	private NodeInfo.LayerDetails getMultiCutRule(Sample ns, List<Example> neList, Cell np)
	{
		// find the highlight layer
		Example firstEx = neList.get(0);
		Sample hs = needHighlightLayer(firstEx, np);
		if (hs == null) return null;
		Rectangle2D highlightBounds = hs.node.getBounds();

		// determine size of each cut
		Rectangle2D nodeBounds = ns.node.getBounds();
		double multiXS = DBMath.round(nodeBounds.getWidth());
		double multiYS = DBMath.round(nodeBounds.getHeight());

		// determine indentation of cuts
		double leftIndent = DBMath.round(nodeBounds.getMinX() - highlightBounds.getMinX());
		double rightIndent = DBMath.round(highlightBounds.getMaxX() - nodeBounds.getMaxX());
		double topIndent = DBMath.round(highlightBounds.getMaxY() - nodeBounds.getMaxY());
		double bottomIndent = DBMath.round(nodeBounds.getMinY() - highlightBounds.getMinY());
		double realIndentX = nodeBounds.getMinX() - firstEx.lx + multiXS/2;
		double realIndentY = nodeBounds.getMinY() - firstEx.ly + multiYS/2;
		if (rightIndent != leftIndent || bottomIndent != leftIndent || topIndent != leftIndent)
		{
			error.markError(ns.node, np, "Multiple contact cuts must be indented uniformly (left=" +
				leftIndent + ", right=" + rightIndent +
				", top=" + topIndent + ", bottom=" + bottomIndent + ")");
			return null;
		}
		double multiIndent = leftIndent;

		// look at every example after the first
		double xSep = -1, ySep = -1;
		for(int n=1; n<neList.size(); n++)
		{
			Example ne = neList.get(n);

			// count number of samples equivalent to the main sample
			int total = 0;
			for(Sample nso : ne.samples)
			{
				if (nso.assoc == ns)
				{
					// make sure size is proper
					Rectangle2D oNodeBounds = nso.node.getBounds();
					if (multiXS != oNodeBounds.getWidth() || multiYS != oNodeBounds.getHeight())
					{
						error.markError(nso.node, np, "Multiple contact cuts must not differ in size");
						return null;
					}
					total++;
				}
			}

			// allocate space for these samples
			Sample [] nsList = new Sample[total];

			// fill the list of samples
			int fill = 0;
			for(Sample nso : ne.samples)
			{
				if (nso.assoc == ns) nsList[fill++] = nso;
			}

			// analyze the samples for separation
			for(int i=1; i<total; i++)
			{
				// find separation
				Rectangle2D thisNodeBounds = nsList[i].node.getBounds();
				Rectangle2D lastNodeBounds = nsList[i-1].node.getBounds();
				double sepX = DBMath.round(Math.abs(lastNodeBounds.getCenterX() - thisNodeBounds.getCenterX()));
				double sepY = DBMath.round(Math.abs(lastNodeBounds.getCenterY() - thisNodeBounds.getCenterY()));

				// check for validity
				if (sepX < multiXS && sepY < multiYS)
				{
					error.markError(nsList[i].node, np, "Multiple contact cuts must not overlap");
					return null;
				}

				// accumulate minimum separation
				if (sepX >= multiXS)
				{
					if (xSep < 0) xSep = sepX; else
					{
						if (xSep > sepX) xSep = sepX;
					}
				}
				if (sepY >= multiYS)
				{
					if (ySep < 0) ySep = sepY; else
					{
						if (ySep > sepY) ySep = sepY;
					}
				}
			}

			// finally ensure that all separations are multiples of "multiSep"
			for(int i=1; i<total; i++)
			{
				// find X separation
				Rectangle2D thisNodeBounds = nsList[i].node.getBounds();
				Rectangle2D lastNodeBounds = nsList[i-1].node.getBounds();
				double sepX = Math.abs(lastNodeBounds.getCenterX() - thisNodeBounds.getCenterX());
				double sepY = Math.abs(lastNodeBounds.getCenterY() - thisNodeBounds.getCenterY());
				if (sepX / xSep * xSep != sepX)
				{
					error.markError(nsList[i].node, np, "Multiple contact cut X spacing must be uniform");
					return null;
				}

				// find Y separation
				if (sepY / ySep * ySep != sepY)
				{
					error.markError(nsList[i].node, np, "Multiple contact cut Y spacing must be uniform");
					return null;
				}
			}
		}
		double multiSepX = xSep - multiXS;
		double multiSepY = ySep - multiYS;
		if (multiSepX != multiSepY)
		{
			error.markError(null, np, "Multiple contact cut X and Y spacing must be the same (X=" + multiSepX + ", Y=" + multiSepY + ")");
			return null;
		}
		ns.values = new Technology.TechPoint[2];
		ns.values[0] = new Technology.TechPoint(EdgeH.fromLeft(realIndentX), EdgeV.fromBottom(realIndentY));
		ns.values[1] = new Technology.TechPoint(EdgeH.fromRight(realIndentX), EdgeV.fromTop(realIndentY));

		NodeInfo.LayerDetails multiDetails = new NodeInfo.LayerDetails();
		multiDetails.style = getStyle(ns.node);
		multiDetails.representation = Technology.NodeLayer.POINTS;
		if (multiDetails.style == Poly.Type.CROSSED ||
			multiDetails.style == Poly.Type.FILLED ||
			multiDetails.style == Poly.Type.CLOSED)
		{
			multiDetails.representation = Technology.NodeLayer.BOX;
//			Variable var2 = ns.node.getVar(Info.MINSIZEBOX_KEY);
//			if (var2 != null) multiDetails.representation = Technology.NodeLayer.MINBOX;
		}
		multiDetails.values = ns.values;
		multiDetails.ns = ns;
		multiDetails.multiCut = true;
		multiDetails.representation = Technology.NodeLayer.MULTICUTBOX;
		multiDetails.multiXS = multiXS;
		multiDetails.multiYS = multiYS;
		multiDetails.multiIndent = multiIndent;
		multiDetails.multiSep = multiSepX;
		multiDetails.multiSep2D = multiSepX;
		return multiDetails;
	}

	/**
	 * Method to adjust the "count"-long array of points in "px" and "py" according
	 * to the stretch factor bits in "factor" and return an array that describes
	 * these points.  Returns zero on error.
	 */
	private Technology.TechPoint [] stretchPoints(Point2D [] pts, int [] factor,
		Sample ns, Cell np, List<Example> neList, Example firstEx)
	{
		Technology.TechPoint [] newRule = new Technology.TechPoint[pts.length];

		for(int i=0; i<pts.length; i++)
		{
			// determine the X algorithm
			EdgeH horiz = null;
			if ((factor[i]&TOEDGELEFT) != 0)
			{
				// left edge rule
				horiz = EdgeH.fromLeft(pts[i].getX()-firstEx.lx);
			} else if ((factor[i]&TOEDGERIGHT) != 0)
			{
				// right edge rule
				horiz = EdgeH.fromRight(firstEx.hx-pts[i].getX());
			} else if ((factor[i]&FROMCENTX) != 0)
			{
				// center rule
				horiz = EdgeH.fromCenter(pts[i].getX()-(firstEx.lx+firstEx.hx)/2);
			} else if ((factor[i]&RATIOCENTX) != 0)
			{
				// constant stretch rule
				if (firstEx.hx == firstEx.lx)
				{
					horiz = EdgeH.makeCenter();
				} else
				{
					horiz = new EdgeH((pts[i].getX()-(firstEx.lx+firstEx.hx)/2) / (firstEx.hx-firstEx.lx), 0);
				}
			} else
			{
				error.markStretchProblem(neList, ns, np, pts[i].getX(), true);
				return null;
			}

			// determine the Y algorithm
			EdgeV vert = null;
			if ((factor[i]&TOEDGEBOT) != 0)
			{
				// bottom edge rule
				vert = EdgeV.fromBottom(pts[i].getY()-firstEx.ly);
			} else if ((factor[i]&TOEDGETOP) != 0)
			{
				// top edge rule
				vert = EdgeV.fromTop(firstEx.hy-pts[i].getY());
			} else if ((factor[i]&FROMCENTY) != 0)
			{
				// center rule
				vert = EdgeV.fromCenter(pts[i].getY()-(firstEx.ly+firstEx.hy)/2);
			} else if ((factor[i]&RATIOCENTY) != 0)
			{
				// constant stretch rule
				if (firstEx.hy == firstEx.ly)
				{
					vert = EdgeV.makeCenter();
				} else
				{
					vert = new EdgeV((pts[i].getY()-(firstEx.ly+firstEx.hy)/2) / (firstEx.hy-firstEx.ly), 0);
				}
			} else
			{
				error.markStretchProblem(neList, ns, np, pts[i].getY(), false);
				return null;
			}
			newRule[i] = new Technology.TechPoint(horiz, vert);
		}
		return newRule;
	}

	private Technology.TechPoint [] fixValues(NodeProto np, Technology.TechPoint [] currentList)
	{
		EdgeH h1 = null, h2 = null, h3 = null;
		EdgeV v1 = null, v2 = null, v3 = null;
		for(int p=0; p<currentList.length; p++)
		{
			EdgeH h = currentList[p].getX();
			if (h.equals(h1) || h.equals(h2) || h.equals(h3)) continue;
			if (h1 == null) h1 = h; else
				if (h2 == null) h2 = h; else
					h3 = h;
			EdgeV v = currentList[p].getY();
			if (v.equals(v1) || v.equals(v2) || v.equals(v3)) continue;
			if (v1 == null) v1 = v; else
				if (v2 == null) v2 = v; else
					v3 = v;
		}
		if (h1 != null && h2 != null && h3 == null &&
			v1 != null && v2 != null && v3 == null)
		{
			// reduce to a box with two points
			currentList = new Technology.TechPoint[2];
			currentList[0] = new Technology.TechPoint(h1, v1);
			currentList[1] = new Technology.TechPoint(h2, v2);
		}
		return currentList;
	}

	/************************************* SUPPORT *************************************/

	/**
	 * Method to associate the samples of example "neList" in cell "np"
	 * Returns true if there is an error
	 */
	private boolean associateExamples(List<Example> neList, Cell np)
	{
		// if there is only one example, no association
		if (neList.size() <= 1) return false;

		// associate each example "ne" with the original in "neList"
		Example firstEx = neList.get(0);
		for(int n=1; n<neList.size(); n++)
		{
			Example ne = neList.get(n);

			// clear associations for every sample "ns" in the example "ne"
			for(Sample ns : ne.samples)
				ns.assoc = null;

			// associate every sample "ns" in the example "ne"
			for(Sample ns : ne.samples)
			{
				if (ns.assoc != null) continue;

				// cannot have center in other examples
				if (ns.layer == Generic.tech().cellCenterNode)
				{
					error.markError(ns.node, np, "Grab point should only be in main example");
					return true;
				}

				// count number of similar layers in original example "neList"
				int total = 0;
				Sample nsFound = null;
				for(Sample nsList : firstEx.samples)
				{
					if (nsList.layer != ns.layer) continue;
					total++;
					nsFound = nsList;
				}

				// no similar layer found in the original: error
				if (total == 0)
				{
					error.markError(ns.node, np, "Layer " + Info.getSampleName(ns.layer) + " not found in main example");
					return true;
				}

				// just one in the original: simple association
				if (total == 1)
				{
					ns.assoc = nsFound;
					continue;
				}

				// if it is a port, associate by port name
				if (ns.layer == Generic.tech().portNode)
				{
					String name = Info.getPortName(ns.node);
					if (name == null)
					{
						error.markError(ns.node, np, "Port does not have a name");
						return true;
					}

					// search the original for that port
					boolean found = false;
					for(Sample nsList : firstEx.samples)
					{
						if (nsList.layer == Generic.tech().portNode)
						{
							String otherName = Info.getPortName(nsList.node);
							if (otherName == null)
							{
								error.markError(nsList.node, np, "Port does not have a name");
								return true;
							}
							if (!name.equalsIgnoreCase(otherName)) continue;
							ns.assoc = nsList;
							found = true;
							break;
						}
					}
					if (!found)
					{
						error.markError(null, np, "Could not find port " + name + " in all examples");
						return true;
					}
					continue;
				}

				// count the number of this layer in example "ne"
				int i = 0;
				for(Sample nsList : ne.samples)
				{
					if (nsList.layer == ns.layer) i++;
				}

				// if number of similar layers differs: error
				if (total != i)
				{
					error.markError(ns.node, np, "Layer " + Info.getSampleName(ns.layer) +
						" found " + total + " times in main example, " + i + " in others");
					return true;
				}

				// make a list of samples on this layer in original
				List<Sample> mainList = new ArrayList<Sample>();
				i = 0;
				for(Sample nsList : firstEx.samples)
				{
					if (nsList.layer == ns.layer) mainList.add(nsList);
				}

				// make a list of samples on this layer in example "ne"
				List<Sample> thisList = new ArrayList<Sample>();
				i = 0;
				for(Sample nsList : ne.samples)
				{
					if (nsList.layer == ns.layer) thisList.add(nsList);
				}

				// sort each list in X/Y/shape
				Collections.sort(mainList, new SampleCoordAscending());
				Collections.sort(thisList, new SampleCoordAscending());

				// see if the lists have duplication
				for(i=1; i<total; i++)
				{
					Sample thisSample = thisList.get(i);
					Sample lastSample = thisList.get(i-1);
					Sample thisMainSample = mainList.get(i);
					Sample lastMainSample = mainList.get(i-1);
					if ((thisSample.xPos == lastSample.xPos &&
							thisSample.yPos == lastSample.yPos &&
							thisSample.node.getProto() == lastSample.node.getProto()) ||
						(thisMainSample.xPos == lastMainSample.xPos &&
							thisMainSample.yPos == lastMainSample.yPos &&
							thisMainSample.node.getProto() == lastMainSample.node.getProto())) break;
				}
				if (i >= total)
				{
					// association can be made in X
					for(i=0; i<total; i++)
					{
						Sample thisSample = thisList.get(i);
						thisSample.assoc = mainList.get(i);
					}
					continue;
				}

				// don't know how to associate this sample
				Sample thisSample = thisList.get(i);
				error.markError(thisSample.node, np, "Sample " + Info.getSampleName(thisSample.layer) + " is unassociated");
				return true;
			}

			// final check: make sure every sample in original example associates
			for(Sample nsList : firstEx.samples)
			{
				nsList.assoc = null;
			}
			for(Sample ns : ne.samples)
			{
				ns.assoc.assoc = ns;
			}
			for(Sample nsList : firstEx.samples)
			{
				if (nsList.assoc == null)
				{
					if (nsList.layer == Generic.tech().cellCenterNode) continue;
					error.markError(nsList.node, np, "Layer " + Info.getSampleName(nsList.layer) + " found in main example, but not others");
					return true;
				}
			}
		}
		return false;
	}

	private static class SamplesByLayerOrder implements Comparator<Sample>
	{
		private LayerInfo [] lList;

		SamplesByLayerOrder(LayerInfo [] lList) { this.lList = lList; }

		public int compare(Sample s1, Sample s2)
		{
			int i1 = -1;
			if (s1.layer != null && s1.layer != Generic.tech().portNode)
			{
				String s1Name = s1.layer.getName().substring(6);
				for(int i = 0; i < lList.length; i++) if (lList[i].name.equals(s1Name)) { i1 = i;   break; }
			}
			int i2 = -1;
			if (s2.layer != null && s2.layer != Generic.tech().portNode)
			{
				String s2Name = s2.layer.getName().substring(6);
				for(int i = 0; i < lList.length; i++) if (lList[i].name.equals(s2Name)) { i2 = i;   break; }
			}
			return i1-i2;
		}
	}

	private static class PortsByAngleAndName implements Comparator<NodeInfo.PortDetails>
	{
		public int compare(NodeInfo.PortDetails s1, NodeInfo.PortDetails s2)
		{
			if (s1.angle != s2.angle) return s1.angle - s2.angle;
			return s1.name.compareTo(s2.name);
		}
	}

	private static class SampleCoordAscending implements Comparator<Sample>
	{
		public int compare(Sample s1, Sample s2)
		{
			if (s1.xPos != s2.xPos) return (int)(s1.xPos - s2.xPos);
			if (s1.yPos != s2.yPos) return (int)(s1.yPos - s2.yPos);
			return s1.node.getName().compareTo(s2.node.getName());
		}
	}

	/* flags about the edge positions in the examples */
	private static final int TOEDGELEFT     =   01;		/* constant to left edge */
	private static final int TOEDGERIGHT    =   02;		/* constant to right edge */
	private static final int TOEDGETOP      =   04;		/* constant to top edge */
	private static final int TOEDGEBOT      =  010;		/* constant to bottom edge */
	private static final int FROMCENTX      =  020;		/* constant in X to center */
	private static final int FROMCENTY      =  040;		/* constant in Y to center */
	private static final int RATIOCENTX     = 0100;		/* fixed ratio from X center to edge */
	private static final int RATIOCENTY     = 0200;		/* fixed ratio from Y center to edge */

	private Poly.Type getStyle(NodeInst ni)
	{
		// layer style
		Poly.Type sty = null;
		if (ni.getProto() == Artwork.tech().filledBoxNode)             sty = Poly.Type.FILLED; else
		if (ni.getProto() == Artwork.tech().boxNode)                   sty = Poly.Type.CLOSED; else
		if (ni.getProto() == Artwork.tech().crossedBoxNode)            sty = Poly.Type.CROSSED; else
		if (ni.getProto() == Artwork.tech().filledPolygonNode)         sty = Poly.Type.FILLED; else
		if (ni.getProto() == Artwork.tech().closedPolygonNode)         sty = Poly.Type.CLOSED; else
		if (ni.getProto() == Artwork.tech().openedPolygonNode)         sty = Poly.Type.OPENED; else
		if (ni.getProto() == Artwork.tech().openedDottedPolygonNode)   sty = Poly.Type.OPENEDT1; else
		if (ni.getProto() == Artwork.tech().openedDashedPolygonNode)   sty = Poly.Type.OPENEDT2; else
		if (ni.getProto() == Artwork.tech().openedThickerPolygonNode)  sty = Poly.Type.OPENEDT3; else
		if (ni.getProto() == Artwork.tech().filledCircleNode)          sty = Poly.Type.DISC; else
		if (ni.getProto() == Artwork.tech().circleNode)
		{
			sty = Poly.Type.CIRCLE;
			double [] angles = ni.getArcDegrees();
			if (angles[0] != 0 || angles[1] != 0) sty = Poly.Type.CIRCLEARC;
		} else if (ni.getProto() == Artwork.tech().thickCircleNode)
		{
			sty = Poly.Type.THICKCIRCLE;
			double [] angles = ni.getArcDegrees();
			if (angles[0] != 0 || angles[1] != 0) sty = Poly.Type.THICKCIRCLEARC;
		} else if (ni.getProto() == Generic.tech().invisiblePinNode)
		{
			Variable var = ni.getVar(Artwork.ART_MESSAGE);
			if (var != null)
			{
				TextDescriptor.Position pos = var.getTextDescriptor().getPos();
				if (pos == TextDescriptor.Position.BOXED)     sty = Poly.Type.TEXTBOX; else
				if (pos == TextDescriptor.Position.CENT)      sty = Poly.Type.TEXTCENT; else
				if (pos == TextDescriptor.Position.UP)        sty = Poly.Type.TEXTBOT; else
				if (pos == TextDescriptor.Position.DOWN)      sty = Poly.Type.TEXTTOP; else
				if (pos == TextDescriptor.Position.LEFT)      sty = Poly.Type.TEXTRIGHT; else
				if (pos == TextDescriptor.Position.RIGHT)     sty = Poly.Type.TEXTLEFT; else
				if (pos == TextDescriptor.Position.UPLEFT)    sty = Poly.Type.TEXTBOTRIGHT; else
				if (pos == TextDescriptor.Position.UPRIGHT)   sty = Poly.Type.TEXTBOTLEFT; else
				if (pos == TextDescriptor.Position.DOWNLEFT)  sty = Poly.Type.TEXTTOPRIGHT; else
				if (pos == TextDescriptor.Position.DOWNRIGHT) sty = Poly.Type.TEXTTOPLEFT;
			}
		}
		if (sty == null)
		{
			System.out.println("Warning: Cannot determine style to use for " + ni.describe(false) +
				" node in " + ni.getParent() + ", assuming FILLED");
			sty = Poly.Type.FILLED;
		}
		return sty;
	}

	/**
	 * Method to return the actual bounding box of layer node "ni" in the
	 * reference variables "lx", "hx", "ly", and "hy"
	 */
	private Rectangle2D getBoundingBox(NodeInst ni)
	{
		Rectangle2D bounds = ni.getBaseShape().getBounds2D();
//		Rectangle2D bounds = ni.getBounds();
//		if (ni.getProto() == Generic.tech().portNode)
//		{
//			double portShrink = 2;
//			bounds.setRect(bounds.getMinX() + portShrink, bounds.getMinY() + portShrink,
//				bounds.getWidth()-portShrink*2, bounds.getHeight()-portShrink*2);
//		}
		bounds.setRect(DBMath.round(bounds.getMinX()), DBMath.round(bounds.getMinY()),
			DBMath.round(bounds.getWidth()), DBMath.round(bounds.getHeight()));
		return bounds;
	}

	private Sample needHighlightLayer(Example neList, Cell np)
	{
		// find the highlight layer
		for(Sample ns : neList.samples)
		{
			if (ns.layer == null) return ns;
		}

		error.markError(null, np, "No highlight layer on contact");
		return null;
	}

	/************************************* WRITE TECHNOLOGY AS "XML" *************************************/

	/**
	 * Method to convert tech-edit information to an Xml.Technology.
	 * @param newTechName new technology name.
	 * @param gi general technology information.
	 * @param lList information about layers.
	 * @param nList information about primitive nodes.
	 * @param aList information about primitive arcs.
	 */
	private Xml.Technology makeXml(String newTechName, GeneralInfo gi, LayerInfo[] lList, NodeInfo[] nList, ArcInfo[] aList)
	{
		Xml.Technology t = new Xml.Technology();
		t.techName = newTechName;
		t.shortTechName = gi.shortName;
		t.description = gi.description;
		t.minNumMetals = t.maxNumMetals = t.defaultNumMetals = gi.defaultNumMetals;
		t.scaleValue = gi.scale;
		t.scaleRelevant = gi.scaleRelevant;
		t.resolutionValue = gi.resolution;
		t.defaultFoundry = gi.defaultFoundry;
		t.minResistance = gi.minRes;
		t.minCapacitance = gi.minCap;

		if (gi.transparentColors != null) {
			for (int i = 0; i < gi.transparentColors.length; i++)
				t.transparentLayers.add(gi.transparentColors[i]);
		}

		for (LayerInfo li: lList) {
			if (li.pseudo) continue;
			Xml.Layer layer = new Xml.Layer();
			layer.name = li.name;
			layer.function = li.fun;
			layer.extraFunction = li.funExtra;
			layer.desc = li.desc;
			layer.thick3D = li.thick3d;
			layer.height3D = li.height3d;
			layer.cif = li.cif;
			layer.resistance = li.spiRes;
			layer.capacitance = li.spiCap;
			layer.edgeCapacitance = li.spiECap;
			if (li.pureLayerNode != null) {
				layer.pureLayerNode = new Xml.PureLayerNode();
				layer.pureLayerNode.name = li.pureLayerNode.name;
				layer.pureLayerNode.style = li.pureLayerNode.nodeLayers[0].style;
				layer.pureLayerNode.port = li.pureLayerNode.nodePortDetails[0].name;
				layer.pureLayerNode.size.addLambda(DBMath.round(li.pureLayerNode.xSize));
				for (ArcInfo a: li.pureLayerNode.nodePortDetails[0].connections)
					layer.pureLayerNode.portArcs.add(a.name);
			}
			t.layers.add(layer);
		}

		for (ArcInfo ai: aList) {
			Xml.ArcProto ap = new Xml.ArcProto();
			ap.name = ai.name;
			ap.function = ai.func;
			ap.wipable = ai.wipes;
			ap.curvable = ai.curvable;
			ap.special = ai.special;
			ap.notUsed = ai.notUsed;
			ap.skipSizeInPalette = ai.skipSizeInPalette;
			ap.extended = !ai.noExtend;
			ap.fixedAngle = ai.fixAng;
			ap.angleIncrement = ai.angInc;
			ap.antennaRatio = ai.antennaRatio;
			for (ArcInfo.LayerDetails al: ai.arcDetails) {
				Xml.ArcLayer l = new Xml.ArcLayer();
				l.layer = al.layer.name;
				l.style = al.style == Poly.Type.FILLED ? Poly.Type.FILLED : Poly.Type.CLOSED;
				l.extend.addLambda(DBMath.round(al.width/2));
				ap.arcLayers.add(l);
			}
			t.arcs.add(ap);
		}

		for (NodeInfo ni: nList) {
			if (ni.func == PrimitiveNode.Function.NODE && ni.nodeLayers[0].layer.pureLayerNode == ni)
				continue;
			Xml.PrimitiveNodeGroup png = new Xml.PrimitiveNodeGroup();
			if (ni.primitiveNodeGroupNames == null)
            {
                Xml.PrimitiveNode n = new Xml.PrimitiveNode();
                n.name = ni.name;
                n.function = ni.func;
                n.lowVt = ni.lowVt;
                n.highVt = ni.highVt;
                n.nativeBit = ni.nativeBit;
                n.od18 = ni.od18;
                n.od25 = ni.od25;
                n.od33 = ni.od33;
                png.nodes.add(n);
            } else
			{
				for(String s : ni.primitiveNodeGroupNames) {
                    Xml.PrimitiveNode n = new Xml.PrimitiveNode();
                    n.name = s;
                    n.function = ni.func;
                    n.lowVt = ni.lowVt;
                    n.highVt = ni.highVt;
                    n.nativeBit = ni.nativeBit;
                    n.od18 = ni.od18;
                    n.od25 = ni.od25;
                    n.od33 = ni.od33;
					png.nodes.add(n);
                }
			}
			png.shrinkArcs = ni.arcsShrink;
			png.square = ni.square;
			png.canBeZeroSize = ni.canBeZeroSize;
			png.wipes = ni.wipes;
			png.lockable = ni.lockable;
			png.edgeSelect = ni.edgeSelect;
			png.skipSizeInPalette = ni.skipSizeInPalette;
			png.notUsed = ni.notUsed;
			EPoint minFullSize = EPoint.fromLambda(0.5*ni.xSize, 0.5*ni.ySize);

//			pn.sizeOffset = ni.so;
//			double lx = -minFullSize.getLambdaX();
//			double hx = minFullSize.getLambdaX();
//			double ly = -minFullSize.getLambdaY();
//			double hy = minFullSize.getLambdaY();
//			if (ni.so != null) {
//				lx += ni.so.getLowXOffset();
//				hx -= ni.so.getHighXOffset();
//				ly += ni.so.getLowYOffset();
//				hy -= ni.so.getHighYOffset();
//			}
/*			pn.nodeBase = ERectangle.fromLambda(lx, ly, hx - lx, hy - ly);
			if (!minFullSize.equals(EPoint.ORIGIN))
				pn.diskOffset = minFullSize;*/
//			EPoint p2 = EPoint.fromGrid(pn.nodeBase.getGridWidth() >> 1, pn.nodeBase.getGridHeight() >> 1);
//			if (!p2.equals(minFullSize))
//				pn.diskOffset.put(Integer.valueOf(1), minFullSize);
//			if (!p2.equals(EPoint.ORIGIN))
//				pn.diskOffset.put(Integer.valueOf(2), p2);

//			pn.defaultWidth.value = DBMath.round(ni.xSize);
//			pn.defaultHeight.value = DBMath.round(ni.ySize);
			if (ni.spiceTemplate != null && !ni.spiceTemplate.equals(""))
				png.spiceTemplate = ni.spiceTemplate;
			for(int j=0; j<ni.nodeLayers.length; j++) {
				NodeInfo.LayerDetails nl = ni.nodeLayers[j];
				png.nodeLayers.add(makeNodeLayerDetails(nl, ni.serp, minFullSize));
			}
			for (int j = 0; j < ni.nodePortDetails.length; j++) {
				NodeInfo.PortDetails pd = ni.nodePortDetails[j];
				Xml.PrimitivePort pp = new Xml.PrimitivePort();
				pp.name = pd.name;
				pp.portAngle = pd.angle;
				pp.portRange = pd.range;
				pp.portTopology = pd.netIndex;

				EdgeH left = pd.values[0].getX();
				EdgeH right = pd.values[1].getX();
				EdgeV bottom = pd.values[0].getY();
				EdgeV top = pd.values[1].getY();

				pp.lx.k = left.getMultiplier()*2;
				pp.lx.addLambda(left.getAdder() + minFullSize.getLambdaX()*left.getMultiplier()*2);
				pp.hx.k = right.getMultiplier()*2;
				pp.hx.addLambda(right.getAdder() + minFullSize.getLambdaX()*right.getMultiplier()*2);
				pp.ly.k = bottom.getMultiplier()*2;
				pp.ly.addLambda(bottom.getAdder() + minFullSize.getLambdaY()*bottom.getMultiplier()*2);
				pp.hy.k = top.getMultiplier()*2;
				pp.hy.addLambda(top.getAdder() + minFullSize.getLambdaY()*top.getMultiplier()*2);
				for (ArcInfo a: pd.connections)
					pp.portArcs.add(a.name);
				png.ports.add(pp);
			}
			png.specialType = ni.specialType;
			if (png.specialType == PrimitiveNode.SERPTRANS) {
				png.specialValues = new double[6];
				for (int i = 0; i < 6; i++)
					png.specialValues[i] = ni.specialValues[i];
			}
			if (ni.nodeSizeRule != null) {
				png.nodeSizeRule = new Xml.NodeSizeRule();
				png.nodeSizeRule.width = ni.nodeSizeRule.getWidth();
				png.nodeSizeRule.height = ni.nodeSizeRule.getHeight();
				png.nodeSizeRule.rule = ni.nodeSizeRule.getRuleName();
			}

            ERectangle base = calcBaseRectangle(ni.so, png.nodeLayers, ni.nodeSizeRule);
            png.baseLX.value = base.getLambdaMinX();
            png.baseHX.value = base.getLambdaMaxX();
            png.baseLY.value = base.getLambdaMinY();
            png.baseHY.value = base.getLambdaMaxY();

			t.nodeGroups.add(png);
		}

		addSpiceHeader(t, 1, gi.spiceLevel1Header);
		addSpiceHeader(t, 2, gi.spiceLevel2Header);
		addSpiceHeader(t, 3, gi.spiceLevel3Header);

        t.menuPalette = gi.menuPalette;

		Xml.Foundry foundry = new Xml.Foundry();
		foundry.name = gi.defaultFoundry;
		for (LayerInfo li: lList) {
			if (li.gds != null && li.gds.length() > 0)
				foundry.layerGds.put(li.name, li.gds);
		}
		if (gi.conDist != null && gi.unConDist != null) {
			int layerTotal = lList.length;
			int ruleIndex = 0;
			for (int i1 = 0; i1 < layerTotal; i1++) {
				LayerInfo l1 = lList[i1];
				for (int i2 = i1; i2 < layerTotal; i2++) {
					LayerInfo l2 = lList[i2];
					double conSpa = gi.conDist[ruleIndex];
					double uConSpa = gi.unConDist[ruleIndex];
					if (conSpa > -1)
						foundry.rules.add(makeDesignRule("C" + ruleIndex, l1, l2, DRCTemplate.DRCRuleType.CONSPA, conSpa));
					if (uConSpa > -1)
						foundry.rules.add(makeDesignRule("U" + ruleIndex, l1, l2, DRCTemplate.DRCRuleType.UCONSPA, uConSpa));
					ruleIndex++;
				}
			}
		}
		t.foundries.add(foundry);

		return t;
	}

    private ERectangle calcBaseRectangle(SizeOffset so, List<Xml.NodeLayer> nodeLayers, PrimitiveNode.NodeSizeRule nodeSizeRule) {
        long lx, hx, ly, hy;
        if (nodeSizeRule != null) {
            hx = DBMath.lambdaToGrid(0.5*nodeSizeRule.getWidth());
            lx = -hx;
            hy = DBMath.lambdaToGrid(0.5*nodeSizeRule.getHeight());
            ly = -hy;
        } else {
            lx = Long.MAX_VALUE;
            hx = Long.MIN_VALUE;
            ly = Long.MAX_VALUE;
            hy = Long.MIN_VALUE;
            for (int i = 0; i < nodeLayers.size(); i++) {
                Xml.NodeLayer nl = nodeLayers.get(i);
                long x, y;
                if (nl.representation == Technology.NodeLayer.BOX || nl.representation == Technology.NodeLayer.MULTICUTBOX) {
                    x = DBMath.lambdaToGrid(nl.lx.value);
                    lx = Math.min(lx, x);
                    hx = Math.max(hx, x);
                    x = DBMath.lambdaToGrid(nl.hx.value);
                    lx = Math.min(lx, x);
                    hx = Math.max(hx, x);
                    y = DBMath.lambdaToGrid(nl.ly.value);
                    ly = Math.min(ly, y);
                    hy = Math.max(hy, y);
                    y = DBMath.lambdaToGrid(nl.hy.value);
                    ly = Math.min(ly, y);
                    hy = Math.max(hy, y);
                } else {
                    for (Technology.TechPoint p: nl.techPoints) {
                        x = p.getX().getGridAdder();
                        lx = Math.min(lx, x);
                        hx = Math.max(hx, x);
                        y = p.getY().getGridAdder();
                        ly = Math.min(ly, y);
                        hy = Math.max(hy, y);
                    }
                }
            }
        }
        if (so != null) {
            lx += so.getLowXGridOffset();
            hx -= so.getHighXGridOffset();
            ly += so.getLowYGridOffset();
            hy -= so.getHighYGridOffset();
        }
        return ERectangle.fromGrid(lx, ly, hx - lx, hy - ly);
    }

	private Xml.NodeLayer makeNodeLayerDetails(NodeInfo.LayerDetails nl, boolean isSerp, EPoint correction) {
		Xml.NodeLayer nld = new Xml.NodeLayer();
		nld.layer = nl.layer.name;
		nld.style = nl.style;
		nld.portNum = nl.portIndex;
		nld.inLayers = nl.inLayers;
		nld.inElectricalLayers = nl.inElectricalLayers;
		nld.representation = nl.representation;
		Technology.TechPoint[] points = nl.values;
		if (nld.representation == Technology.NodeLayer.BOX || nld.representation == Technology.NodeLayer.MULTICUTBOX) {
			nld.lx.k = points[0].getX().getMultiplier()*2;
			nld.lx.addLambda(DBMath.round(points[0].getX().getAdder() + correction.getLambdaX()*points[0].getX().getMultiplier()*2));
			nld.hx.k = points[1].getX().getMultiplier()*2;
			nld.hx.addLambda(DBMath.round(points[1].getX().getAdder() + correction.getLambdaX()*points[1].getX().getMultiplier()*2));
			nld.ly.k = points[0].getY().getMultiplier()*2;
			nld.ly.addLambda(DBMath.round(points[0].getY().getAdder() + correction.getLambdaY()*points[0].getY().getMultiplier()*2));
			nld.hy.k = points[1].getY().getMultiplier()*2;
			nld.hy.addLambda(DBMath.round(points[1].getY().getAdder() + correction.getLambdaY()*points[1].getY().getMultiplier()*2));
		} else {
			for (Technology.TechPoint p: points)
				nld.techPoints.add(correction(p, correction));
		}
		nld.sizex = DBMath.round(nl.multiXS);
		nld.sizey = DBMath.round(nl.multiYS);
		nld.sep1d = DBMath.round(nl.multiSep);
		nld.sep2d = DBMath.round(nl.multiSep2D);
		if (isSerp) {
			nld.lWidth = DBMath.round(nl.lWidth);
			nld.rWidth = DBMath.round(nl.rWidth);
			nld.tExtent = DBMath.round(nl.extendT);
			nld.bExtent = DBMath.round(nl.extendB);
		}
		nld.inNodes = nl.inNodes;
		return nld;
	}

	private Technology.TechPoint correction(Technology.TechPoint p, EPoint correction)
	{
		EdgeH h = p.getX();
		EdgeV v = p.getY();
		h = new EdgeH(h.getMultiplier(), h.getAdder() + correction.getLambdaX()*h.getMultiplier()*2);
		v = new EdgeV(v.getMultiplier(), v.getAdder() + correction.getLambdaY()*v.getMultiplier()*2);
		return new Technology.TechPoint(h, v);
	}

	private void addSpiceHeader(Xml.Technology t, int level, String[] spiceLines) {
		if (spiceLines == null) return;
		Xml.SpiceHeader spiceHeader = new Xml.SpiceHeader();
		spiceHeader.level = level;
		for (String spiceLine: spiceLines)
			spiceHeader.spiceLines.add(spiceLine);
		t.spiceHeaders.add(spiceHeader);
	}

	private List<Object> makeMenuBoxXml(Xml.Technology t, Object o)
	{
		List<Object> menuBox = new ArrayList<Object>();
		if (o != null)
		{
			if (o instanceof List)
			{
				for(Object subO : (List)o)
					menuBox.add(subO);
			} else menuBox.add(o);
		}
		return menuBox;
	}

	private DRCTemplate makeDesignRule(String ruleName, LayerInfo l1, LayerInfo l2, DRCTemplate.DRCRuleType type, double value)
	{
		return new DRCTemplate(ruleName, DRCTemplate.DRCMode.ALL.mode(), type, l1.name, l2.name, new double[] {value}, null, null);
	}
}
