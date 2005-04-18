/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: Parse.java
* User tool: Technology Editor, creation
* Written by Steven M. Rubin, Sun Microsystems.
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
package com.sun.electric.tool.user.tecEdit;

import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.EdgeH;
import com.sun.electric.technology.EdgeV;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Technology.TechPoint;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.erc.ERC;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
* This class creates technology libraries from technologies.
*/
public class Parse
{	
	/* the meaning of "us_tecflags" */
	private static final int HASDRCMINWID  =          01;				/* has DRC minimum width information */
	private static final int HASDRCMINWIDR =          02;				/* has DRC minimum width information */
	private static final int HASCOLORMAP   =          04;				/* has color map */
	private static final int HASARCWID     =         010;				/* has arc width offset factors */
	private static final int HASCIF        =         020;				/* has CIF layers */
	private static final int HASDXF        =         040;				/* has DXF layers */
	private static final int HASGDS        =        0100;				/* has Calma GDS-II layers */
	private static final int HASGRAB       =        0200;				/* has grab point information */
	private static final int HASSPIRES     =        0400;				/* has SPICE resistance information */
	private static final int HASSPICAP     =       01000;				/* has SPICE capacitance information */
	private static final int HASSPIECAP    =       02000;				/* has SPICE edge capacitance information */
	private static final int HAS3DINFO     =       04000;				/* has 3D height/thickness information */
	private static final int HASCONDRC     =      010000;				/* has connected design rules */
	private static final int HASCONDRCR    =      020000;				/* has connected design rules reasons */
	private static final int HASUNCONDRC   =      040000;				/* has unconnected design rules */
	private static final int HASUNCONDRCR  =     0100000;				/* has unconnected design rules reasons */
	private static final int HASCONDRCW    =     0200000;				/* has connected wide design rules */
	private static final int HASCONDRCWR   =     0400000;				/* has connected wide design rules reasons */
	private static final int HASUNCONDRCW  =    01000000;				/* has unconnected wide design rules */
	private static final int HASUNCONDRCWR =    02000000;				/* has unconnected wide design rules reasons */
	private static final int HASCONDRCM    =    04000000;				/* has connected multicut design rules */
	private static final int HASCONDRCMR   =   010000000;				/* has connected multicut design rules reasons */
	private static final int HASUNCONDRCM  =   020000000;				/* has unconnected multicut design rules */
	private static final int HASUNCONDRCMR =   040000000;				/* has unconnected multicut design rules reasons */
	private static final int HASEDGEDRC    =  0100000000;				/* has edge design rules */
	private static final int HASEDGEDRCR   =  0200000000;				/* has edge design rules reasons */
	private static final int HASMINNODE    =  0400000000;				/* has minimum node size */
	private static final int HASMINNODER   = 01000000000;				/* has minimum node size reasons */
	private static final int HASPRINTCOL   = 02000000000;				/* has print colors */

	static class Sample
	{
		NodeInst  node;					/* true node used for sample */
		NodeProto layer;				/* type of node used for sample */
		double    xpos, ypos;			/* center of sample */
		Sample    assoc;				/* associated sample in first example */

		Technology.TechPoint [] values;	/* points that describe the sample */
		String    msg;					/* string (null if none) */

		Example   parent;				/* example containing this sample */
		Sample    nextsample;			/* next sample in list */
	};

	static class Example
	{
		Sample    firstsample;			/* head of list of samples in example */
		Sample    studysample;			/* sample under analysis */
		double    lx, hx, ly, hy;		/* bounding box of example */
		Example   nextexample;			/* next example in list */
	};

	/* the globals that define a technology */
	static int           us_tecflags;
//	static INTBIG           us_teclayer_count;
//	static CHAR           **us_teclayer_iname = 0;
//	static CHAR           **us_teclayer_names = 0;
//	static INTBIG          *us_teclayer_function = 0;
//	static DRCRULES        *us_tecdrc_rules = 0;
//	static INTBIG          *us_tecnode_grab = 0;
//	static INTBIG           us_tecnode_grabcount;
	
	/**
	 * Method invoked for the "technology edit library-to-tech" command.  Dumps
	 * code if "dumpformat" is nonzero
	 */
	public static void makeTechFromLib()
	{
		GenerateTechnology dialog = new GenerateTechnology();
		dialog.initComponents();
		dialog.setVisible(true);
	}
	
	private static class SoftTech extends Technology
	{
		SoftTech(String name)
		{
			super(name);
			setNoNegatedArcs();
		}

		void setTheScale(double scale)
		{
			setFactoryScale(scale, true);
		}

		void setTransparentColors(Color [] colors)
		{
			this.setFactoryTransparentLayers(colors);
		}
	}

	private static void makeTech(String newName, String renameName, boolean alsoJava)
	{
		Library lib = Library.getCurrent();

		// get a new name for the technology
		String newtechname = newName;
		boolean modified = false;
		for(;;)
		{
			// search by hand because "gettechnology" handles partial matches
			if (Technology.findTechnology(newtechname) == null) break;
			newtechname += "X";
			modified = true;
		}
		if (modified)
			System.out.println("Warning: already a technology called " + newName + ".  Naming this " + newtechname);
	
		// get list of dependent libraries
		Library [] dependentlibs = Manipulate.us_teceditgetdependents(lib);
	
		// initialize the state of this technology
//		us_tecflags = 0;

		// get general information from the "factors" cell
		Cell np = null;
		for(int i=dependentlibs.length-1; i>=0; i--)
		{
			np = dependentlibs[i].findNodeProto("factors");
			if (np != null) break;
		}
		if (np == null)
		{
			System.out.println("Cell with general information, called 'factors', is missing");
			return;
		}
		Generate.GeneralInfo gi = Generate.GeneralInfo.us_teceditgettechinfo(np);

		// get layer information
		Generate.LayerInfo [] lList = us_tecedmakelayers(dependentlibs);
		if (lList == null) return;

		// get arc information
		Generate.ArcInfo [] aList = us_tecedmakearcs(dependentlibs, lList);
		if (aList == null) return;

		// get node information
		Generate.NodeInfo [] nList = us_tecedmakenodes(dependentlibs, lList, aList);
		if (nList == null) return;

		// create the technology
		SoftTech tech = new SoftTech(newtechname);
		tech.setTheScale(gi.scale);
		tech.setTechDesc(gi.description);
		tech.setMinResistance(gi.minres);
		tech.setMinCapacitance(gi.mincap);
		tech.setGateLengthSubtraction(gi.gateShrinkage);
		tech.setGateIncluded(gi.includeGateInResistance);
		tech.setGroundNetIncluded(gi.includeGround);
		if (gi.transparentColors != null) tech.setTransparentColors(gi.transparentColors);

		// create the layers
		for(int i=0; i<lList.length; i++)
		{
			Layer lay = Layer.newInstance(tech, lList[i].name, lList[i].desc);
			lay.setFunction(lList[i].fun, lList[i].funExtra);
			lay.setCIFLayer(lList[i].cif);
			lay.setGDSLayer(lList[i].gds);
			lay.setResistance(lList[i].spires);
			lay.setCapacitance(lList[i].spicap);
			lay.setEdgeCapacitance(lList[i].spiecap);
			lay.setDistance(lList[i].height3d);
			lay.setThickness(lList[i].thick3d);
			lList[i].generated = lay;
		}

		// create the arcs
		for(int i=0; i<aList.length; i++)
		{
			Generate.ArcDetails [] ad = aList[i].arcDetails;
			Technology.ArcLayer [] arcLayers = new Technology.ArcLayer[ad.length];
			for(int j=0; j<ad.length; j++)
				arcLayers[j] = new Technology.ArcLayer(ad[j].layer.generated, ad[j].width, ad[j].style);
			PrimitiveArc newArc = PrimitiveArc.newInstance(tech, aList[i].name, aList[i].maxWidth, arcLayers);
			newArc.setFunction(aList[i].func);
			newArc.setFactoryFixedAngle(aList[i].fixang);
			if (aList[i].wipes) newArc.setWipable(); else newArc.clearWipable();
			newArc.setFactoryAngleIncrement(aList[i].anginc);
			newArc.setExtended(!aList[i].noextend);
			newArc.setWidthOffset(aList[i].widthOffset);
			ERC.getERCTool().setAntennaRatio(newArc, aList[i].antennaRatio);
			aList[i].generated = newArc;
		}

		// create the nodes
		for(int i=0; i<nList.length; i++)
		{
			Generate.NodeLayerDetails [] nd = nList[i].nodeLayers;
			Technology.NodeLayer [] nodeLayers = new Technology.NodeLayer[nd.length];
			for(int j=0; j<nd.length; j++)
			{
				Generate.LayerInfo li = nd[j].layer;
				Layer lay = li.generated;
				TechPoint [] points = nd[j].values;
				nodeLayers[j] = new Technology.NodeLayer(lay, nd[j].portIndex, nd[j].style, Technology.NodeLayer.BOX, points);
			}
			PrimitiveNode prim = PrimitiveNode.newInstance(nList[i].name, tech, nList[i].xSize, nList[i].ySize, nList[i].so, nodeLayers);
			nList[i].generated = prim;
			prim.setFunction(nList[i].func);
			if (nList[i].wipes) prim.setArcsWipe();
			if (nList[i].lockable) prim.setLockedPrim();
			if (nList[i].square) prim.setSquare();

			// add special information if present
			if (nList[i].specialType == PrimitiveNode.MULTICUT ||
				nList[i].specialType == PrimitiveNode.SERPTRANS ||
				nList[i].specialType == PrimitiveNode.POLYGONAL)
			{
				prim.setSpecialType(nList[i].specialType);
				if (nList[i].specialType == PrimitiveNode.MULTICUT ||
					nList[i].specialType == PrimitiveNode.SERPTRANS)
						prim.setSpecialValues(nList[i].specialValues);
			}

			// analyze special node function circumstances
			if (nList[i].func == PrimitiveNode.Function.NODE)
			{
				prim.setHoldsOutline();
			} else if (nList[i].func == PrimitiveNode.Function.PIN)
			{
				if (!prim.isWipeOn1or2())
				{
					prim.setArcsWipe();
					prim.setArcsShrink();
				}
			}

			int numPorts = nList[i].nodePortDetails.length;
			PrimitivePort [] portList = new PrimitivePort[numPorts];
			for(int j=0; j<numPorts; j++)
			{
				Generate.NodePortDetails portDetail = nList[i].nodePortDetails[j];
				ArcProto [] cons = new ArcProto[portDetail.connections.length];
				for(int k=0; k<portDetail.connections.length; k++)
					cons[k] = portDetail.connections[k].generated;
				portList[j] = PrimitivePort.newInstance(tech, prim, cons, portDetail.name,
					portDetail.angle, portDetail.range, portDetail.netIndex, PortCharacteristic.UNKNOWN,
					portDetail.values[0].getX(), portDetail.values[0].getY(),
					portDetail.values[1].getX(), portDetail.values[1].getY());
			}
			prim.addPrimitivePorts(portList);
		}
		
		// create the pure-layer associations
		for(int i=0; i<lList.length; i++)
		{
			if ((lList[i].funExtra&Layer.Function.PSEUDO) != 0) continue;
	
			// find the pure layer node
			for(int j=0; j<nList.length; j++)
			{
				if (nList[j].func != PrimitiveNode.Function.NODE) continue;
				Generate.NodeLayerDetails nld = nList[j].nodeLayers[0];
				if (nld.layer == lList[i])
				{
					lList[i].generated.setPureLayerNode(nList[j].generated);
					break;
				}
			}
		}

		// check technology for consistency
		us_tecedcheck(lList, aList, nList);
	
		if (alsoJava)
		{
			// print the technology as Java code
			String fileName = OpenFile.chooseOutputFile(FileType.JAVA, "File for Technology's Java Code",
				newtechname + ".java");
			if (fileName != null)
			{
				FileOutputStream fileOutputStream = null;
				try {
				    PrintStream buffWriter = new PrintStream(new FileOutputStream(fileName));
			
					// write the layers, arcs, and nodes
					us_teceditdumpjavalayers(buffWriter, newtechname, lList, gi);
					us_teceditdumpjavaarcs(buffWriter, newtechname, aList, gi);
					us_teceditdumpjavanodes(buffWriter, newtechname, nList, lList, gi);
					buffWriter.println("}");
			
					// clean up
					buffWriter.close();
					System.out.println("Wrote " + fileName);
				} catch (IOException e)
				{
					System.out.println("Error creating " + fileName);
				}
			}
		}
	
//		// finish initializing the technology
//		if ((us_tecflags&HASGRAB) == 0) us_tecvariables[0].name = 0; else
//		{
//			us_tecvariables[0].name = x_("prototype_center");
//			us_tecvariables[0].value = (CHAR *)us_tecnode_grab;
//			us_tecvariables[0].type = us_tecnode_grabcount/3;
//		}
	
		// switch to this technology
		System.out.println("Technology " + tech.getTechName() + " built.  Switching to it.");
		WindowFrame.updateTechnologyLists();
		tech.setCurrent();
	}

	/**
	 * This class displays a dialog for converting a library to a technology.
	 */
	public static class GenerateTechnology extends EDialog
	{
		private JLabel lab2, lab3;
		private JTextField renameName, newName;
		private JCheckBox alsoJava;

		/** Creates new form convert library to technology */
		public GenerateTechnology()
		{
			super(null, true);
		}

		private void ok() { exit(true); }

		protected void escapePressed() { exit(false); }

		// Call this method when the user clicks the OK button
		private void exit(boolean goodButton)
		{
			if (goodButton)
			{
				makeTech(newName.getText(), renameName.getText(), alsoJava.isSelected());
			}
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
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(lab1, gbc);

			newName = new JTextField(Library.getCurrent().getName());
			gbc = new GridBagConstraints();
			gbc.gridx = 1;   gbc.gridy = 0;
			gbc.gridwidth = 2;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(newName, gbc);
			TechNameDocumentListener myDocumentListener = new TechNameDocumentListener(this);
			newName.getDocument().addDocumentListener(myDocumentListener);

			lab2 = new JLabel("Already a technology with this name");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 1;
			gbc.gridwidth = 3;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(lab2, gbc);

			lab3 = new JLabel("Rename existing technology to:");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 2;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(lab3, gbc);

			renameName = new JTextField();
			gbc = new GridBagConstraints();
			gbc.gridx = 1;   gbc.gridy = 2;
			gbc.gridwidth = 2;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(renameName, gbc);

			alsoJava = new JCheckBox("Also write Java code");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 3;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(alsoJava, gbc);

			// OK and Cancel
			JButton cancel = new JButton("Cancel");
			gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = 3;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(cancel, gbc);
			cancel.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { exit(false); }
			});

			JButton ok = new JButton("OK");
			getRootPane().setDefaultButton(ok);
			gbc = new java.awt.GridBagConstraints();
			gbc.gridx = 2;
			gbc.gridy = 3;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(ok, gbc);
			ok.addActionListener(new java.awt.event.ActionListener()
			{
				public void actionPerformed(java.awt.event.ActionEvent evt) { exit(true); }
			});

			pack();
		}

		/**
		 * Class to handle special changes to changes to a GDS layer.
		 */
		private static class TechNameDocumentListener implements DocumentListener
		{
			GenerateTechnology dialog;

			TechNameDocumentListener(GenerateTechnology dialog) { this.dialog = dialog; }

			public void changedUpdate(DocumentEvent e) { dialog.nameChanged(); }
			public void insertUpdate(DocumentEvent e) { dialog.nameChanged(); }
			public void removeUpdate(DocumentEvent e) { dialog.nameChanged(); }
		}
	}

	private static void us_tecedcheck(Generate.LayerInfo [] lList, Generate.ArcInfo [] aList, Generate.NodeInfo [] nList)
	{
		// make sure there is a pure-layer node for every nonpseudo layer
		for(int i=0; i<lList.length; i++)
		{
			if ((lList[i].funExtra & Layer.Function.PSEUDO) != 0) continue;
			boolean found = false;
			for(int j=0; j<nList.length; j++)
			{
				Generate.NodeInfo nin = nList[j];
				if (nin.func != PrimitiveNode.Function.NODE) continue;
				if (nin.nodeLayers[0].layer == lList[i])
				{
					found = true;
					break;
				}
			}
			if (found) continue;
			System.out.println("Warning: Layer " + lList[i].name + " has no associated pure-layer node");
		}
	
		// make sure there is a pin for every arc and that it uses pseudo-layers
		for(int i=0; i<aList.length; i++)
		{
			// find that arc's pin
			boolean found = false;
			for(int j=0; j<nList.length; j++)
			{
				Generate.NodeInfo nin = nList[j];
				if (nin.func != PrimitiveNode.Function.PIN) continue;

				for(int k=0; k<nin.nodePortDetails.length; k++)
				{
					Generate.ArcInfo [] connections = nin.nodePortDetails[k].connections;
					for(int l=0; l<connections.length; l++)
					{
						if (connections[l] == aList[i])
						{
							// pin found: make sure it uses pseudo-layers
							boolean allPseudo = true;
							for(int m=0; m<nin.nodeLayers.length; m++)
							{
								Generate.LayerInfo lin = nin.nodeLayers[m].layer;
								if ((lin.funExtra & Layer.Function.PSEUDO) == 0) { allPseudo = false;   break; }
							}
							if (!allPseudo)
								System.out.println("Warning: Pin " + nin.name + " is not composed of pseudo-layers");

							found = true;
							break;
						}
					}
					if (found) break;
				}
				if (found) break;
			}
			if (!found)
				System.out.println("Warning: Arc " + aList[i].name + " has no associated pin node");
		}
	}

	/**
	 * Method to scan the "dependentlibcount" libraries in "dependentlibs",
	 * and build the layer structures for it in technology "tech".  Returns true on error.
	 */
	static Generate.LayerInfo [] us_tecedmakelayers(Library [] dependentlibs)
	{
		// first find the number of layers
		Cell [] layerCells = Manipulate.us_teceditfindsequence(dependentlibs, "layer-", Generate.LAYERSEQUENCE_KEY);
		if (layerCells.length <= 0)
		{
			System.out.println("No layers found");
			return null;
		}

		// create the layers
		Generate.LayerInfo [] lis = new Generate.LayerInfo[layerCells.length];
		for(int i=0; i<layerCells.length; i++)
		{
			lis[i] = Generate.LayerInfo.us_teceditgetlayerinfo(layerCells[i]);
			if (lis[i] == null) continue;
		}

//		// get the design rules
//		drcsize = us_teclayer_count*us_teclayer_count/2 + (us_teclayer_count+1)/2;
//		us_tecedgetlayernamelist();
//		if (us_tecdrc_rules != 0)
//		{
//			dr_freerules(us_tecdrc_rules);
//			us_tecdrc_rules = 0;
//		}
//		nodecount = us_teceditfindsequence(dependentlibs, "node-", Generate.NODERSEQUENCE_KEY);
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
//			var = getval((INTBIG)dependentlibs[i], VLIBRARY, VSTRING|VISARRAY, x_("EDTEC_DRC"));
//			if (var != NOVARIABLE) break;
//		}
//		us_teceditgetdrcarrays(var, us_tecdrc_rules);
	
//		for(i=0; i<total; i++)
//		{
//			(void)allocstring(&us_teclayer_iname[i], makeabbrev(us_teclayer_names[i], TRUE),
//				us_tool.cluster);
//	
//			// loop until the name is unique
//			for(;;)
//			{
//				// see if a previously assigned abbreviation is the same
//				for(j=0; j<i; j++)
//					if (namesame(us_teclayer_iname[i], us_teclayer_iname[j]) == 0)
//						break;
//				if (j >= i) break;
//	
//				// name conflicts: change it
//				l = estrlen(ab = us_teclayer_iname[i]);
//				if (ab[l-1] >= '0' && ab[l-1] <= '8') ab[l-1]++; else
//				{
//					infstr = initinfstr();
//					addstringtoinfstr(infstr, ab);
//					addtoinfstr(infstr, '0');
//					(void)reallocstring(&us_teclayer_iname[i], returninfstr(infstr), us_tool.cluster);
//				}
//			}
//		}
	
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
	
	/**
	 * Method to scan the "dependentlibcount" libraries in "dependentlibs",
	 * and build the arc structures for it in technology "tech".  Returns true on error.
	 */
	static Generate.ArcInfo [] us_tecedmakearcs(Library [] dependentlibs, Generate.LayerInfo [] lList)
	{
		// count the number of arcs in the technology
		Cell [] arcCells = Manipulate.us_teceditfindsequence(dependentlibs, "arc-", Generate.ARCSEQUENCE_KEY);
		if (arcCells.length <= 0)
		{
			System.out.println("No arcs found");
			return null;
		}

		Generate.ArcInfo [] allArcs = new Generate.ArcInfo[arcCells.length];
		for(int i=0; i<arcCells.length; i++)
		{
			Cell np = arcCells[i];
			allArcs[i] = Generate.ArcInfo.us_teceditgetarcinfo(np);

			// build a list of examples found in this arc
			Example nelist = us_tecedgetexamples(np, false);
			if (nelist == null) return null;
			if (nelist.nextexample != null)
			{
				us_tecedpointout(null, np);
				System.out.println("Can only be one example of " + np.describe() + " but more were found");
				return null;
			}
	
			// get width and polygon count information
			double maxwid = -1, hwid = -1;
			int count = 0;
			for(Sample ns = nelist.firstsample; ns != null; ns = ns.nextsample)
			{
				double wid = Math.min(ns.node.getXSize(), ns.node.getYSize());
				if (wid > maxwid) maxwid = wid;
				if (ns.layer == null) hwid = wid; else count++;
			}
			allArcs[i].widthOffset = maxwid - hwid;
			allArcs[i].maxWidth = maxwid;
	
			// error if there is no highlight box
			if (hwid < 0)
			{
				us_tecedpointout(null, np);
				System.out.println("No highlight layer found in " + np.describe());
				return null;
			}
			allArcs[i].arcDetails = new Generate.ArcDetails[count];
			
			// fill the individual arc layer structures
			int layerindex = 0;
			for(int k=0; k<2; k++)
				for(Sample ns = nelist.firstsample; ns != null; ns = ns.nextsample)
			{
				if (ns.layer == null) continue;
	
				// get the layer index
				String sampleLayer = ns.layer.getName().substring(6);
				Generate.LayerInfo li = null;
				for(int j=0; j<lList.length; j++)
				{
					if (sampleLayer.equals(lList[j].name)) { li = lList[j];   break; }
				}
				if (li == null)
				{
					System.out.println("Cannot find layer " + sampleLayer + ", used in " + np.describe());
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
				allArcs[i].arcDetails[layerindex] = new Generate.ArcDetails();
				allArcs[i].arcDetails[layerindex].layer = li;
	
				// determine the style of this arc layer
				Poly.Type style = Poly.Type.CLOSED;
				if (ns.node.getProto() == Artwork.tech.filledBoxNode)
					style = Poly.Type.FILLED;
				allArcs[i].arcDetails[layerindex].style = style;
	
				// determine the width offset of this arc layer
				double wid = Math.min(ns.node.getXSize(), ns.node.getYSize());
				allArcs[i].arcDetails[layerindex].width = maxwid-wid;

				layerindex++;
			}
		}
		return allArcs;
	}

	/**
	 * Method to scan the "dependentlibcount" libraries in "dependentlibs",
	 * and build the node structures for it in technology "tech".  Returns true on error.
	 */
	static Generate.NodeInfo [] us_tecedmakenodes(Library [] dependentlibs, Generate.LayerInfo [] lList, Generate.ArcInfo [] aList)
	{
		Cell [] nodeCells = Manipulate.us_teceditfindsequence(dependentlibs, "node-", Generate.NODESEQUENCE_KEY);
		if (nodeCells.length <= 0)
		{
			System.out.println("No nodes found");
			return null;
		}

		Generate.NodeInfo [] nList = new Generate.NodeInfo[nodeCells.length];

		// get the nodes
		int nodeIndex = 0;
		for(int pass=0; pass<3; pass++)
			for(int m=0; m<nodeCells.length; m++)
		{
			// make sure this is the right type of node for this pass of the nodes
			Cell np = nodeCells[m];
			Generate.NodeInfo nin = Generate.NodeInfo.us_teceditgetnodeinfo(np);
			Netlist netList = np.acquireUserNetlist();
			if (netList == null)
			{
				System.out.println("Sorry, a deadlock technology generation (network information unavailable).  Please try again");
				return null;
			}

			// only want pins on pass 0, pure-layer nodes on pass 2
			if (pass == 0 && nin.func != PrimitiveNode.Function.PIN) continue;
			if (pass == 1 && (nin.func == PrimitiveNode.Function.PIN || nin.func == PrimitiveNode.Function.NODE)) continue;
			if (pass == 2 && nin.func != PrimitiveNode.Function.NODE) continue;
			if (nin.func == PrimitiveNode.Function.NODE)
			{
				if (nin.serp)
				{
					us_tecedpointout(null, np);
					System.out.println("Pure layer " + nin.name + " can not be serpentine");
					return null;
				}
				nin.specialType = PrimitiveNode.POLYGONAL;
			}

			nList[nodeIndex] = nin;
			nin.name = np.getName().substring(5);

			// build a list of examples found in this node
			Example nelist = us_tecedgetexamples(np, true);
			if (nelist == null)
			{
				System.out.println("Cannot analyze cell " + np.describe());
				return null;
			}
			nin.xSize = nelist.hx - nelist.lx;
			nin.ySize = nelist.hy - nelist.ly;
	
			// associate the samples in each example
			if (us_tecedassociateexamples(nelist, np))
			{
				System.out.println("Cannot match different examples in " + np.describe());
				return null;
			}

			// derive primitives from the examples
			nin.nodeLayers = us_tecedmakeprim(nelist, np, lList);
			if (nin.nodeLayers == null)
			{
				System.out.println("Cannot derive stretching rules for " + np.describe());
				return null;
			}

			// handle multicut layers
			for(int i=0; i<nin.nodeLayers.length; i++)
			{
				Generate.NodeLayerDetails nld = nin.nodeLayers[i];
				if (nld.multiCut)
				{
					nin.specialType = PrimitiveNode.MULTICUT;
					nin.specialValues = new double[6];
					nin.specialValues[0] = nin.nodeLayers[i].multixs;
					nin.specialValues[1] = nin.nodeLayers[i].multiys;
					nin.specialValues[2] = nin.nodeLayers[i].multiindent;
					nin.specialValues[3] = nin.nodeLayers[i].multiindent;
					nin.specialValues[4] = nin.nodeLayers[i].multisep;
					nin.specialValues[5] = nin.nodeLayers[i].multisep;

					// make the multicut layer the last one
					Generate.NodeLayerDetails nldLast = nin.nodeLayers[nin.nodeLayers.length-1];
					Generate.NodeLayerDetails nldMC = nin.nodeLayers[i];
					nin.nodeLayers[i] = nldLast;
					nin.nodeLayers[nin.nodeLayers.length-1] = nldMC;
					break;
				}
			}
	
			// count the number of ports on this node
			int portcount = 0;
			for(Sample ns = nelist.firstsample; ns != null; ns = ns.nextsample)
				if (ns.layer == Generic.tech.portNode) portcount++;
			if (portcount == 0)
			{
				us_tecedpointout(null, np);
				System.out.println("No ports found in " + np.describe());
				return null;
			}
	
			// allocate space for the ports
			nin.nodePortDetails = new Generate.NodePortDetails[portcount];

			// fill the port structures
			int pol1port = -1, pol2port = -1, dif1port = -1, dif2port = -1;
			int i = 0;
			for(Sample ns = nelist.firstsample; ns != null; ns = ns.nextsample)
			{
				if (ns.layer != Generic.tech.portNode) continue;
	
				// port connections
				nin.nodePortDetails[i] = new Generate.NodePortDetails();
				nin.nodePortDetails[i].connections = new Generate.ArcInfo[0];
				Variable var = ns.node.getVar(Generate.CONNECTION_KEY);
				if (var != null)
				{
					// convert "arc-CELL" pointers to indices
					Cell [] arcCells = (Cell [])var.getObject();					
					Generate.ArcInfo [] connections = new Generate.ArcInfo[arcCells.length];
					nin.nodePortDetails[i].connections = connections;
					boolean portchecked = false;
					for(int j=0; j<arcCells.length; j++)
					{
						// find arc that connects
						Cell arcCell = arcCells[j];
						connections[j] = null;
						if (arcCell != null)
						{
							String cellName = arcCell.getName().substring(4);
							for(int k=0; k<aList.length; k++)
							{
								if (aList[k].name.equalsIgnoreCase(cellName))
								{
									connections[j] = aList[k];
									break;
								}
							}
						}
						if (connections[j] == null)
						{
							us_tecedpointout(ns.node, ns.node.getParent());
							System.out.println("Invalid connection list on port in " + np.describe());
							return null;
						}
	
						// find port characteristics for possible transistors
						if (portchecked) continue;
						if (connections[j].func.isPoly())
						{
							if (pol1port < 0)
							{
								pol1port = i;
								portchecked = true;
							} else if (pol2port < 0)
							{
								pol2port = i;
								portchecked = true;
							}
						} else if (connections[j].func.isDiffusion())
						{
							if (dif1port < 0)
							{
								dif1port = i;
								portchecked = true;
							} else if (dif2port < 0)
							{
								dif2port = i;
								portchecked = true;
							}
						}
					}
				}
	
				// link connection list to the port
				if (nin.nodePortDetails[i].connections == null) return null;
	
				// port name
				String portname = Manipulate.us_tecedgetportname(ns.node);
				if (portname == null)
				{
					us_tecedpointout(ns.node, np);
					System.out.println("Cell " + np.describe() + ": port does not have a name");
					return null;
				}
				for(int c=0; c<portname.length(); c++)
				{
					char str = portname.charAt(c);
					if (str <= ' ' || str >= 0177)
					{
						us_tecedpointout(ns.node, np);
						System.out.println("Invalid port name '" + portname + "' in " + np.describe());
						return null;
					}
				}
				nin.nodePortDetails[i].name = portname;
	
				// port angle and range
				nin.nodePortDetails[i].angle = 0;
				Variable varAngle = ns.node.getVar(Generate.PORTANGLE_KEY);
				if (varAngle != null)
					nin.nodePortDetails[i].angle |= ((Integer)varAngle.getObject()).intValue();
				nin.nodePortDetails[i].range = 180;
				Variable varRange = ns.node.getVar(Generate.PORTRANGE_KEY);
				if (varRange != null)
					nin.nodePortDetails[i].range |= ((Integer)varRange.getObject()).intValue();
	
				// port connectivity
				nin.nodePortDetails[i].netIndex = i;
				if (ns.node.getNumConnections() != 0)
				{
					ArcInst ai1 = ((Connection)ns.node.getConnections().next()).getArc();
					Network net1 = netList.getNetwork(ai1, 0);
					int j = 0;
					for(Sample ons = nelist.firstsample; ons != ns; ons = ons.nextsample)
					{
						if (ons.layer != Generic.tech.portNode) continue;
						if (ons.node.getNumConnections() != 0)
						{
							ArcInst ai2 = ((Connection)ons.node.getConnections().next()).getArc();
							Network net2 = netList.getNetwork(ai2, 0);
							if (net1 == net2)
							{
								nin.nodePortDetails[i].netIndex = j;
								break;
							}
						}
						j++;
					}
				}
	
				// port area rule
				nin.nodePortDetails[i].values = ns.values;
				i++;
			}
	
			// on field-effect transistors, make sure ports 0 and 2 are poly
			if (nin.func == PrimitiveNode.Function.TRANMOS || nin.func == PrimitiveNode.Function.TRADMOS ||
				nin.func == PrimitiveNode.Function.TRAPMOS || nin.func == PrimitiveNode.Function.TRADMES ||
				nin.func == PrimitiveNode.Function.TRAEMES)
			{
				if (pol1port < 0 || pol2port < 0 || dif1port < 0 || dif2port < 0)
				{
					us_tecedpointout(null, np);
					System.out.println("Need 2 gate and 2 active ports on field-effect transistor " + np.describe());
					return null;
				}
				if (pol1port != 0)
				{
					if (pol2port == 0)
					{
						Generate.NodePortDetails formerPortA = nin.nodePortDetails[pol1port];
						Generate.NodePortDetails formerPortB = nin.nodePortDetails[pol2port];
						int swap = pol1port;   pol1port = pol2port;   pol2port = swap;
						nin.nodePortDetails[pol1port] = formerPortA;
						nin.nodePortDetails[pol2port] = formerPortB;
					} else if (dif1port == 0)
					{
						Generate.NodePortDetails formerPortA = nin.nodePortDetails[pol1port];
						Generate.NodePortDetails formerPortB = nin.nodePortDetails[dif1port];
						int swap = pol1port;   pol1port = dif1port;   dif1port = swap;
						nin.nodePortDetails[pol1port] = formerPortA;
						nin.nodePortDetails[dif1port] = formerPortB;
					} else if (dif2port == 0)
					{
						Generate.NodePortDetails formerPortA = nin.nodePortDetails[pol1port];
						Generate.NodePortDetails formerPortB = nin.nodePortDetails[dif2port];
						int swap = pol1port;   pol1port = dif2port;   dif2port = swap;
						nin.nodePortDetails[pol1port] = formerPortA;
						nin.nodePortDetails[dif2port] = formerPortB;
					}
				}
				if (pol2port != 2)
				{
					if (dif1port == 2)
					{
						Generate.NodePortDetails formerPortA = nin.nodePortDetails[pol2port];
						Generate.NodePortDetails formerPortB = nin.nodePortDetails[dif1port];
						int swap = pol2port;   pol2port = dif1port;   dif1port = swap;
						nin.nodePortDetails[pol2port] = formerPortA;
						nin.nodePortDetails[dif1port] = formerPortB;
					} else if (dif2port == 2)
					{
						Generate.NodePortDetails formerPortA = nin.nodePortDetails[pol2port];
						Generate.NodePortDetails formerPortB = nin.nodePortDetails[dif2port];
						int swap = pol2port;   pol2port = dif2port;   dif2port = swap;
						nin.nodePortDetails[pol2port] = formerPortA;
						nin.nodePortDetails[dif2port] = formerPortB;
					}
				}
				if (dif1port != 1)
				{
					Generate.NodePortDetails formerPortA = nin.nodePortDetails[dif1port];
					Generate.NodePortDetails formerPortB = nin.nodePortDetails[dif2port];
					int swap = dif1port;   dif1port = dif2port;   dif2port = swap;
					nin.nodePortDetails[dif1port] = formerPortA;
					nin.nodePortDetails[dif2port] = formerPortB;
				}
	
				// also make sure that dif1port is positive and dif2port is negative
				double x1pos = (nin.nodePortDetails[dif1port].values[0].getX().getMultiplier() * nin.xSize +
					nin.nodePortDetails[dif1port].values[0].getX().getAdder() +
					nin.nodePortDetails[dif1port].values[1].getX().getMultiplier() * nin.xSize +
					nin.nodePortDetails[dif1port].values[1].getX().getAdder()) / 2;
				double x2pos = (nin.nodePortDetails[dif2port].values[0].getX().getMultiplier() * nin.xSize +
					nin.nodePortDetails[dif2port].values[0].getX().getAdder() +
					nin.nodePortDetails[dif2port].values[1].getX().getMultiplier() * nin.xSize +
					nin.nodePortDetails[dif2port].values[1].getX().getAdder()) / 2;
				double y1pos = (nin.nodePortDetails[dif1port].values[0].getY().getMultiplier() * nin.ySize +
					nin.nodePortDetails[dif1port].values[0].getY().getAdder() +
					nin.nodePortDetails[dif1port].values[1].getY().getMultiplier() * nin.ySize +
					nin.nodePortDetails[dif1port].values[1].getY().getAdder()) / 2;
				double y2pos = (nin.nodePortDetails[dif2port].values[0].getY().getMultiplier() * nin.ySize +
					nin.nodePortDetails[dif2port].values[0].getY().getAdder() +
					nin.nodePortDetails[dif2port].values[1].getY().getMultiplier() * nin.ySize +
					nin.nodePortDetails[dif2port].values[1].getY().getAdder()) / 2;
				if (Math.abs(x1pos-x2pos) > Math.abs(y1pos-y2pos))
				{
					if (x1pos < x2pos)
					{
						Generate.NodePortDetails formerPortA = nin.nodePortDetails[dif1port];
						Generate.NodePortDetails formerPortB = nin.nodePortDetails[dif2port];
						nin.nodePortDetails[dif1port] = formerPortA;
						nin.nodePortDetails[dif2port] = formerPortB;
					}
				} else
				{
					if (y1pos < y2pos)
					{
						Generate.NodePortDetails formerPortA = nin.nodePortDetails[dif1port];
						Generate.NodePortDetails formerPortB = nin.nodePortDetails[dif2port];
						nin.nodePortDetails[dif1port] = formerPortA;
						nin.nodePortDetails[dif2port] = formerPortB;
					}
				}
	
				// also make sure that pol1port is negative and pol2port is positive
				x1pos = (nin.nodePortDetails[pol1port].values[0].getX().getMultiplier() * nin.xSize +
					nin.nodePortDetails[pol1port].values[0].getX().getAdder() +
					nin.nodePortDetails[pol1port].values[1].getX().getMultiplier() * nin.xSize +
					nin.nodePortDetails[pol1port].values[1].getX().getAdder()) / 2;
				x2pos = (nin.nodePortDetails[pol2port].values[0].getX().getMultiplier() * nin.xSize +
					nin.nodePortDetails[pol2port].values[0].getX().getAdder() +
					nin.nodePortDetails[pol2port].values[1].getX().getMultiplier() * nin.xSize +
					nin.nodePortDetails[pol2port].values[1].getX().getAdder()) / 2;
				y1pos = (nin.nodePortDetails[pol1port].values[0].getY().getMultiplier() * nin.ySize +
					nin.nodePortDetails[pol1port].values[0].getY().getAdder() +
					nin.nodePortDetails[pol1port].values[1].getY().getMultiplier() * nin.ySize +
					nin.nodePortDetails[pol1port].values[1].getY().getAdder()) / 2;
				y1pos = (nin.nodePortDetails[pol2port].values[0].getY().getMultiplier() * nin.ySize +
					nin.nodePortDetails[pol2port].values[0].getY().getAdder() +
					nin.nodePortDetails[pol2port].values[1].getY().getMultiplier() * nin.ySize +
					nin.nodePortDetails[pol2port].values[1].getY().getAdder()) / 2;
				if (Math.abs(x1pos-x2pos) > Math.abs(y1pos-y2pos))
				{
					if (x1pos > x2pos)
					{
						Generate.NodePortDetails formerPortA = nin.nodePortDetails[pol1port];
						Generate.NodePortDetails formerPortB = nin.nodePortDetails[pol2port];
						nin.nodePortDetails[pol1port] = formerPortA;
						nin.nodePortDetails[pol2port] = formerPortB;
					}
				} else
				{
					if (y1pos > y2pos)
					{
						Generate.NodePortDetails formerPortA = nin.nodePortDetails[pol1port];
						Generate.NodePortDetails formerPortB = nin.nodePortDetails[pol2port];
						nin.nodePortDetails[pol1port] = formerPortA;
						nin.nodePortDetails[pol2port] = formerPortB;
					}
				}
			}
	
			// count the number of layers on the node
			int layercount = 0;
			for(Sample ns = nelist.firstsample; ns != null; ns = ns.nextsample)
			{
				if (ns.values != null && ns.layer != Generic.tech.portNode &&
					ns.layer != Generic.tech.cellCenterNode && ns.layer != null)
						layercount++;
			}

			// finish up serpentine transistors
			if (nList[i].specialType == PrimitiveNode.SERPTRANS)
			{
				// determine port numbers for serpentine transistors
				int polindex = -1, difindex = -1;
				for(int k=0; k<nin.nodeLayers.length; k++)
				{
					Generate.NodeLayerDetails nld = nin.nodeLayers[k];
					if (nld.layer.fun.isPoly())
					{
						polindex = k;
					} else if (nld.layer.fun.isDiff())
					{
						difindex = i;
					}
				}
				if (difindex < 0 || polindex < 0)
				{
					us_tecedpointout(null, np);
					System.out.println("No diffusion and polysilicon layers in transistor " + np.describe());
					return null;
				}

				// compute port extension factors
				nin.specialValues = new double[6];
				nin.specialValues[0] = layercount+1;
				if (nin.nodePortDetails[dif1port].values[0].getX().getAdder() >
					nin.nodePortDetails[dif1port].values[0].getY().getAdder())
				{
					// vertical diffusion layer: determine polysilicon width
					nin.specialValues[3] = (nin.ySize * nin.nodeLayers[polindex].values[1].getY().getMultiplier() +
						nin.nodeLayers[polindex].values[1].getY().getAdder()) -
						(nin.ySize * nin.nodeLayers[polindex].values[0].getY().getMultiplier() +
						nin.nodeLayers[polindex].values[0].getY().getAdder());
	
					// determine diffusion port rule
					nin.specialValues[1] = (nin.xSize * nin.nodePortDetails[dif1port].values[0].getX().getMultiplier() +
						nin.nodePortDetails[dif1port].values[0].getX().getAdder()) -
						(nin.xSize * nin.nodeLayers[difindex].values[0].getX().getMultiplier() +
						nin.nodeLayers[difindex].values[0].getX().getAdder());
					nin.specialValues[2] = (nin.ySize * nin.nodePortDetails[dif1port].values[0].getY().getMultiplier() +
						nin.nodePortDetails[dif1port].values[0].getY().getAdder()) -
						(nin.ySize * nin.nodeLayers[polindex].values[1].getY().getMultiplier() +
						nin.nodeLayers[polindex].values[1].getY().getAdder());
	
					// determine polysilicon port rule
					nin.specialValues[4] = (nin.ySize * nin.nodePortDetails[pol1port].values[0].getY().getMultiplier() +
						nin.nodePortDetails[pol1port].values[0].getY().getAdder()) -
						(nin.ySize * nin.nodeLayers[polindex].values[0].getY().getMultiplier() +
						nin.nodeLayers[polindex].values[0].getY().getAdder());
					nin.specialValues[5] = (nin.xSize * nin.nodeLayers[difindex].values[0].getX().getMultiplier() +
						nin.nodeLayers[difindex].values[0].getX().getAdder()) -
						(nin.xSize * nin.nodePortDetails[pol1port].values[1].getX().getMultiplier() +
						nin.nodePortDetails[pol1port].values[1].getX().getAdder());
				} else
				{
					// horizontal diffusion layer: determine polysilicon width
					nin.specialValues[3] = (nin.xSize * nin.nodeLayers[polindex].values[1].getX().getMultiplier() +
						nin.nodeLayers[polindex].values[1].getX().getAdder()) -
						(nin.xSize * nin.nodeLayers[polindex].values[0].getX().getMultiplier() +
						nin.nodeLayers[polindex].values[0].getX().getAdder());
	
					// determine diffusion port rule
					nin.specialValues[1] = (nin.ySize * nin.nodePortDetails[dif1port].values[0].getY().getMultiplier() +
						nin.nodePortDetails[dif1port].values[0].getY().getAdder()) -
						(nin.ySize * nin.nodeLayers[difindex].values[0].getY().getMultiplier() +
						nin.nodeLayers[difindex].values[0].getY().getAdder());
					nin.specialValues[2] = (nin.xSize * nin.nodeLayers[polindex].values[0].getX().getMultiplier() +
						nin.nodeLayers[polindex].values[0].getX().getAdder()) -
						(nin.xSize * nin.nodePortDetails[dif1port].values[1].getX().getMultiplier() +
						nin.nodePortDetails[dif1port].values[1].getX().getAdder());
	
					// determine polysilicon port rule
					nin.specialValues[4] = (nin.xSize * nin.nodePortDetails[pol1port].values[0].getX().getMultiplier() +
						nin.nodePortDetails[pol1port].values[0].getX().getAdder()) -
						(nin.xSize * nin.nodeLayers[polindex].values[0].getX().getMultiplier() +
						nin.nodeLayers[polindex].values[0].getX().getAdder());
					nin.specialValues[5] = (nin.ySize * nin.nodeLayers[difindex].values[0].getY().getMultiplier() +
						nin.nodeLayers[difindex].values[0].getY().getAdder()) -
						(nin.ySize * nin.nodePortDetails[pol1port].values[1].getY().getMultiplier() +
						nin.nodePortDetails[pol1port].values[1].getY().getAdder());
				}
	
				// find width and extension from comparison to poly layer
//				for(int k=0; k<nin.nodeLayers.length; k++)
//				{
//					Generate.NodeLayerDetails nld = nin.nodeLayers[k];
//
////					for(nsindex=0, ns = nelist.firstsample; ns != null;
////						nsindex++, ns = ns.nextsample)
////							if (tlist.gra[i].lwidth == nsindex) break;
////					if (ns == null)
////					{
////						us_tecedpointout(null, np);
////						System.out.println("Internal error in serpentine " + np.describe());
////						continue;
////					}
//
//					Sample ns = nld.ns;
//					Sample polNs = nin.nodeLayers[polindex].ns;
//					Rectangle2D polNodeBounds = polNs.node.getBounds();
//					if (polNodeBounds.getWidth() > polNodeBounds.getHeight())
//					{
//						// horizontal layer
//						nld.lWidth = polNodeBounds.getMaxY() - (ns.parent.ly + ns.parent.hy)/2;
//						nld.rWidth = (ns.parent.ly + ns.parent.hy)/2 - polNodeBounds.getMinY();
//						nld.extendT = diflayer.node.lowx - polNodeBounds.getMinX();
//					} else
//					{
//						// vertical layer
//						nld.lWidth = polNodeBounds.getMaxX() - (ns.parent.lx + ns.parent.hx)/2;
//						nld.rWidth = (ns.parent.lx + ns.parent.hx)/2 - polNodeBounds.getMinX();
//						nld.extendT = diflayer.node.lowy - polNodeBounds.getMinY();
//					}
//					nld.extendB = nld.extendT;
//				}
	
//				// copy basic graphics to electrical version, doubling diffusion
//				i = 0;
//				for(j=0; j<tlist.layercount; j++)
//				{
//					if (j != difindex) k = 1; else
//					{
//						k = 2;
//	
//						// copy rectangle rule and prepare for electrical layers
//						r = diflayer.rule;
//						if (r.count != 8)
//						{
//							us_tecedpointout(null, np);
//							System.out.println("Nonrectangular diffusion in Serpentine " + np.describe());
//							return true;
//						}
//						for(l=0; l<r.count; l++) serprule[l] = r.value[l];
//						if (serprule[0] != -H0 || serprule[2] != -H0 ||
//							serprule[4] != H0 || serprule[6] != H0)
//						{
//							us_tecedpointout(null, np);
//							System.out.println("Unusual diffusion in Serpentine " + np.describe());
//							return true;
//						}
//						if (nin.xSize - serprule[1] + serprule[5] <
//							nin.ySize - serprule[3] + serprule[7]) serpdifind = 2; else
//								serpdifind = 0;
//					}
//					for(l=0; l<k; l++)
//					{
//						tlist.ele[i].basics.layernum = tlist.gra[j].basics.layernum;
//						tlist.ele[i].basics.count = tlist.gra[j].basics.count;
//						tlist.ele[i].basics.style = tlist.gra[j].basics.style;
//						tlist.ele[i].basics.representation = tlist.gra[j].basics.representation;
//						tlist.ele[i].basics.points = tlist.gra[j].basics.points;
//						tlist.ele[i].lwidth = tlist.gra[j].lwidth;
//						tlist.ele[i].rwidth = tlist.gra[j].rwidth;
//						tlist.ele[i].extendt = tlist.gra[j].extendt;
//						tlist.ele[i].extendb = tlist.gra[j].extendb;
//						if (k == 1) tlist.ele[i].basics.portnum = tlist.gra[j].basics.portnum; else
//							switch (l)
//						{
//							case 0:
//								tlist.ele[i].basics.portnum = (INTSML)dif1port;
//								tlist.ele[i].rwidth = -tlist.gra[polindex].lwidth;
//								save1 = serprule[serpdifind+1];
//	
//								// in transistor, diffusion stops in center
//								serprule[serpdifind] = 0;
//								serprule[serpdifind+1] = 0;
//								r = us_tecedaddrule(serprule, 8, FALSE, 0);
//								if (r == NORULE) return(TRUE);
//								r.used = TRUE;
//								tlist.ele[i].basics.points = r.value;
//								serprule[serpdifind] = -H0;
//								serprule[serpdifind+1] = save1;
//								break;
//							case 1:
//								tlist.ele[i].basics.portnum = (INTSML)dif2port;
//								tlist.ele[i].lwidth = -tlist.gra[polindex].rwidth;
//								save1 = serprule[serpdifind+5];
//	
//								// in transistor, diffusion stops in center
//								serprule[serpdifind+4] = 0;
//								serprule[serpdifind+5] = 0;
//								r = us_tecedaddrule(serprule, 8, FALSE, 0);
//								if (r == null) return(TRUE);
//								r.used = TRUE;
//								tlist.ele[i].basics.points = r.value;
//								serprule[serpdifind+4] = H0;
//								serprule[serpdifind+5] = save1;
//								break;
//						}
//						i++;
//					}
//				}
			}
	
			// extract width offset information from highlight box
			double lx = 0, hx = 0, ly = 0, hy = 0;
			boolean found = false;
			for(Sample ns = nelist.firstsample; ns != null; ns = ns.nextsample)
			{
				if (ns.layer != null) continue;
				found = true;
				if (ns.values != null)
				{
					boolean err = false;
					if (ns.values[0].getX().getMultiplier() == -0.5)		// left edge offset
					{
						lx = ns.values[0].getX().getAdder();
					} else if (ns.values[0].getX().getMultiplier() == 0.5)
					{
						lx = nin.xSize + ns.values[0].getX().getAdder();
					} else err = true;
					if (ns.values[0].getY().getMultiplier() == -0.5)		// bottom edge offset
					{
						ly = ns.values[0].getY().getAdder();
					} else if (ns.values[0].getY().getMultiplier() == 0.5)
					{
						ly = nin.ySize + ns.values[0].getY().getAdder();;
					} else err = true;
					if (ns.values[1].getX().getMultiplier() == 0.5)		// right edge offset
					{
						hx = -ns.values[1].getX().getAdder();
					} else if (ns.values[1].getX().getMultiplier() == -0.5)
					{
						hx = nin.xSize - ns.values[1].getX().getAdder();
					} else err = true;
					if (ns.values[1].getY().getMultiplier() == 0.5)		// top edge offset
					{
						hy = -ns.values[1].getY().getAdder();
					} else if (ns.values[1].getY().getMultiplier() == -0.5)
					{
						hy = nin.ySize - ns.values[1].getY().getAdder();
					} else err = true;
					if (err)
					{
						us_tecedpointout(ns.node, ns.node.getParent());
						System.out.println("Highlighting cannot scale from center in " + np.describe());
						return null;
					}
				} else
				{
					us_tecedpointout(ns.node, ns.node.getParent());
					System.out.println("No rule found for highlight in " + np.describe());
					return null;
				}
			}
			if (!found)
			{
				us_tecedpointout(null, np);
				System.out.println("No highlight found in " + np.describe());
				return null;
			}
			if (lx != 0 || hx != 0 || ly != 0 || hy != 0)
			{
				nList[nodeIndex].so = new SizeOffset(lx, hx, ly, hy);
			}

//			// get grab point information
//			for(ns = nelist.firstsample; ns != NOSAMPLE; ns = ns.nextsample)
//				if (ns.layer == Generic.tech.cellCenterNode) break;
//			if (ns != NOSAMPLE)
//			{
//				us_tecnode_grab[us_tecnode_grabcount++] = nodeindex+1;
//				us_tecnode_grab[us_tecnode_grabcount++] = (ns.node.geom.lowx +
//					ns.node.geom.highx - nelist.lx - nelist.hx)/2 *
//					el_curlib.lambda[tech.techindex] / lambda;
//				us_tecnode_grab[us_tecnode_grabcount++] = (ns.node.geom.lowy +
//					ns.node.geom.highy - nelist.ly - nelist.hy)/2 *
//					el_curlib.lambda[tech.techindex] / lambda;
//				us_tecflags |= HASGRAB;
//			}
	
			// advance the fill pointer
			nodeIndex++;
		}
		return nList;
	}

	/**
	 * Method to parse the node examples in cell "np" and return a list of
	 * EXAMPLEs (one per example).  "isnode" is true if this is a node
	 * being examined.  Returns NOEXAMPLE on error.
	 */
	static Example us_tecedgetexamples(Cell np, boolean isnode)
	{
		HashMap nodeExamples = new HashMap();
		for(Iterator it = np.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
	
			// ignore special nodes with function information
			int funct = Manipulate.us_tecedgetoption(ni);
			if (funct != Generate.LAYERPATCH && funct != Generate.PORTOBJ && funct != Generate.HIGHLIGHTOBJ)
			{
				nodeExamples.put(ni, new Integer(0));
			}
		}
	
		Example nelist = null;
		for(Iterator it = np.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (nodeExamples.get(ni) != null) continue;
	
			// get a new cluster of nodes
			Example ne = new Example();
			ne.firstsample = null;
			ne.nextexample = nelist;
			nelist = ne;

			SizeOffset so = ni.getSizeOffset();
			Poly poly = new Poly(ni.getAnchorCenterX(), ni.getAnchorCenterY(),
				ni.getXSize() - so.getLowXOffset() - so.getHighXOffset(),
				ni.getYSize() - so.getLowYOffset() - so.getHighYOffset());
			poly.transform(ni.rotateOut());
			Rectangle2D sofar = poly.getBounds2D();
	
			// now find all others that touch this area
			boolean gotbbox = false;
			boolean foundone = true;
			int hcount = 0;
			while (foundone)
			{
				foundone = false;
	
				// begin to search the area so far
	            for(Iterator oIt = np.searchIterator(sofar); oIt.hasNext(); )
	            {
	                Geometric geom = (Geometric)oIt.next();
					if (geom == null) break;
					if (!(geom instanceof NodeInst)) continue;
					NodeInst otherni = (NodeInst)geom;
					SizeOffset oSo = otherni.getSizeOffset();
					Poly oPoly = new Poly(otherni.getAnchorCenterX(), otherni.getAnchorCenterY(),
						otherni.getXSize() - oSo.getLowXOffset() - oSo.getHighXOffset(),
						otherni.getYSize() - oSo.getLowYOffset() - oSo.getHighYOffset());
					oPoly.transform(otherni.rotateOut());
					Rectangle2D otherRect = oPoly.getBounds2D();
					if (!GenMath.rectsIntersect(otherRect, sofar)) continue;
					// make sure the node is valid
					Object otherAssn = nodeExamples.get(otherni);
					if (otherAssn != null)
					{
						if (otherAssn instanceof Integer) continue;
						if ((Example)otherAssn == ne) continue;
						us_tecedpointout(otherni, np);
						System.out.println("Examples are too close in " + np.describe());
						return null;
					}
					nodeExamples.put(otherni, ne);
	
					// add it to the cluster
					Sample ns = new Sample();
					ns.node = otherni;
					ns.values = null;
					ns.msg = null;
					ns.parent = ne;
					ns.nextsample = ne.firstsample;
					ne.firstsample = ns;
					ns.assoc = null;
					ns.xpos = otherRect.getCenterX();
					ns.ypos = otherRect.getCenterY();
					int funct = Manipulate.us_tecedgetoption(otherni);
					switch (funct)
					{
						case Generate.PORTOBJ:
							if (!isnode)
							{
								us_tecedpointout(otherni, np);
								System.out.println(np.describe() + " cannot have ports.  Delete this");
								return null;
							}
							ns.layer = Generic.tech.portNode;
							break;
						case Generate.CENTEROBJ:
							if (!isnode)
							{
								us_tecedpointout(otherni, np);
								System.out.println(np.describe() + " cannot have a grab point.  Delete this");
								return null;
							}
							ns.layer = Generic.tech.cellCenterNode;
							break;
						case Generate.HIGHLIGHTOBJ:
							hcount++;
							break;
						default:
							ns.layer = Manipulate.us_tecedgetlayer(otherni);
							if (ns.layer == null)
							{
								us_tecedpointout(otherni, np);
								System.out.println("No layer information on node " + otherni.describe() + " in " + np.describe());
								return null;
							}
							break;
					}
	
					// accumulate state if this is not a "grab point" mark
					if (otherni.getProto() != Generic.tech.cellCenterNode)
					{
						if (!gotbbox)
						{
							ne.lx = otherRect.getMinX();   ne.hx = otherRect.getMaxX();
							ne.ly = otherRect.getMinY();   ne.hy = otherRect.getMaxY();
							gotbbox = true;
						} else
						{
							if (otherRect.getMinX() < ne.lx) ne.lx = otherRect.getMinX();
							if (otherRect.getMaxX() > ne.hx) ne.hx = otherRect.getMaxX();
							if (otherRect.getMinY() < ne.ly) ne.ly = otherRect.getMinY();
							if (otherRect.getMaxY() > ne.hy) ne.hy = otherRect.getMaxY();
						}
						sofar.setRect(ne.lx, ne.ly, ne.hx-ne.lx, ne.hy-ne.ly);
					}
					foundone = true;
				}
			}
			if (hcount == 0)
			{
				us_tecedpointout(null, np);
				System.out.println("No highlight layer in " + np.describe() + " example");
				return null;
			}
			if (hcount != 1)
			{
				us_tecedpointout(null, np);
				System.out.println("Too many highlight layers in " + np.describe() + " example.  Delete some");
				return null;
			}
		}
		if (nelist == null)
		{
			us_tecedpointout(null, np);
			System.out.println("No examples found in " + np.describe());
			return nelist;
		}
	
		/*
		 * now search the list for the smallest, most upper-right example
		 * (the "main" example)
		 */
		double sizex = nelist.hx - nelist.lx;
		double sizey = nelist.hy - nelist.ly;
		double locx = (nelist.lx + nelist.hx) / 2;
		double locy = (nelist.ly + nelist.hy) / 2;
		Example bestne = nelist;
		for(Example ne = nelist; ne != null; ne = ne.nextexample)
		{
			double newsize = ne.hx-ne.lx;
			newsize *= ne.hy-ne.ly;
			if (newsize > sizex*sizey) continue;
			if (newsize == sizex*sizey && (ne.lx+ne.hx)/2 >= locx && (ne.ly+ne.hy)/2 <= locy)
				continue;
			sizex = ne.hx - ne.lx;
			sizey = ne.hy - ne.ly;
			locx = (ne.lx + ne.hx) / 2;
			locy = (ne.ly + ne.hy) / 2;
			bestne = ne;
		}
	
		// place the main example at the top of the list
		if (bestne != nelist)
		{
			for(Example ne = nelist; ne != null; ne = ne.nextexample)
				if (ne.nextexample == bestne)
			{
				ne.nextexample = bestne.nextexample;
				break;
			}
			bestne.nextexample = nelist;
			nelist = bestne;
		}
	
		// done
		return nelist;
	}
	
	/**
	 * Method to associate the samples of example "nelist" in cell "np"
	 * Returns true if there is an error
	 */
	static boolean us_tecedassociateexamples(Example nelist, Cell np)
	{	
		// if there is only one example, no association
		if (nelist.nextexample == null) return false;
	
		// associate each example "ne" with the original in "nelist"
		for(Example ne = nelist.nextexample; ne != null; ne = ne.nextexample)
		{
			// clear associations for every sample "ns" in the example "ne"
			for(Sample ns = ne.firstsample; ns != null; ns = ns.nextsample)
				ns.assoc = null;
	
			// associate every sample "ns" in the example "ne"
			for(Sample ns = ne.firstsample; ns != null; ns = ns.nextsample)
			{
				if (ns.assoc != null) continue;
	
				// cannot have center in other examples
				if (ns.layer == Generic.tech.cellCenterNode)
				{
					us_tecedpointout(ns.node, ns.node.getParent());
					System.out.println("Grab point should only be in main example of " + np.describe());
					return true;
				}
	
				// count number of similar layers in original example "nelist"
				int total = 0;
				Sample nsfound = null;
				for(Sample nslist = nelist.firstsample; nslist != null; nslist = nslist.nextsample)
				{
					if (nslist.layer != ns.layer) continue;
					total++;
					nsfound = nslist;
				}
	
				// no similar layer found in the original: error
				if (total == 0)
				{
					us_tecedpointout(ns.node, ns.node.getParent());
					System.out.println("Layer " + us_tecedsamplename(ns.layer) + " not found in main example of " + np.describe());
					return true;
				}
	
				// just one in the original: simple association
				if (total == 1)
				{
					ns.assoc = nsfound;
					continue;
				}
	
				// if it is a port, associate by port name
				if (ns.layer == Generic.tech.portNode)
				{
					String name = Manipulate.us_tecedgetportname(ns.node);
					if (name == null)
					{
						us_tecedpointout(ns.node, ns.node.getParent());
						System.out.println("Cell " + np.describe() + ": port does not have a name");
						return true;
					}
	
					// search the original for that port
					boolean found = false;
					for(Sample nslist = nelist.firstsample; nslist != null; nslist = nslist.nextsample)
						if (nslist.layer == Generic.tech.portNode)
					{
						String othername = Manipulate.us_tecedgetportname(nslist.node);
						if (othername == null)
						{
							us_tecedpointout(nslist.node, nslist.node.getParent());
							System.out.println("Cell " + np.describe() + ": port does not have a name");
							return true;
						}
						if (!name.equalsIgnoreCase(othername)) continue;
						ns.assoc = nslist;
						found = true;
						break;
					}
					if (!found)
					{
						us_tecedpointout(null, np);
						System.out.println("Could not find port " + name + " in all examples of " + np.describe());
						return true;
					}
					continue;
				}
	
				// count the number of this layer in example "ne"
				int i = 0;
				for(Sample nslist = ne.firstsample; nslist != null; nslist = nslist.nextsample)
					if (nslist.layer == ns.layer) i++;
	
				// if number of similar layers differs: error
				if (total != i)
				{
					us_tecedpointout(ns.node, ns.node.getParent());
					System.out.println("Layer " + us_tecedsamplename(ns.layer) + " found " + total + " times in main example, " + i + " in other");
					System.out.println("Make the counts consistent");
					return true;
				}
	
				// make a list of samples on this layer in original
				List mainList = new ArrayList();
				i = 0;
				for(Sample nslist = nelist.firstsample; nslist != null; nslist = nslist.nextsample)
					if (nslist.layer == ns.layer) mainList.add(nslist);
	
				// make a list of samples on this layer in example "ne"
				List thisList = new ArrayList();
				i = 0;
				for(Sample nslist = ne.firstsample; nslist != null; nslist = nslist.nextsample)
					if (nslist.layer == ns.layer) thisList.add(nslist);
	
				// sort each list in X/Y/shape
				Collections.sort(mainList, new SampleCoordAscending());
				Collections.sort(thisList, new SampleCoordAscending());
	
				// see if the lists have duplication
				for(i=1; i<total; i++)
				{
					Sample thisSample = (Sample)thisList.get(i);
					Sample lastSample = (Sample)thisList.get(i-1);
					Sample thisMainSample = (Sample)mainList.get(i);
					Sample lastMainSample = (Sample)mainList.get(i-1);
					if ((thisSample.xpos == lastSample.xpos &&
							thisSample.ypos == lastSample.ypos &&
							thisSample.node.getProto() == lastSample.node.getProto()) ||
						(thisMainSample.xpos == lastMainSample.xpos &&
							thisMainSample.ypos == lastMainSample.ypos &&
							thisMainSample.node.getProto() == lastMainSample.node.getProto())) break;
				}
				if (i >= total)
				{
					// association can be made in X
					for(i=0; i<total; i++)
					{
						Sample thisSample = (Sample)thisList.get(i);
						thisSample.assoc = (Sample)mainList.get(i);
					}
					continue;
				}
	
				// don't know how to associate this sample
				Sample thisSample = (Sample)thisList.get(i);
				us_tecedpointout(thisSample.node, thisSample.node.getParent());
				System.out.println("Sample " + us_tecedsamplename(thisSample.layer) + " is unassociated in " + np.describe());
				return true;
			}
	
			// final check: make sure every sample in original example associates
			for(Sample nslist = nelist.firstsample; nslist != null;
				nslist = nslist.nextsample) nslist.assoc = null;
			for(Sample ns = ne.firstsample; ns != null; ns = ns.nextsample)
				ns.assoc.assoc = ns;
			for(Sample nslist = nelist.firstsample; nslist != null; nslist = nslist.nextsample)
				if (nslist.assoc == null)
			{
				if (nslist.layer == Generic.tech.cellCenterNode) continue;
				us_tecedpointout(nslist.node, nslist.node.getParent());
				System.out.println("Layer " + us_tecedsamplename(nslist.layer) + " found in main example, but not others in " + np.describe());
				return true;
			}
		}
		return false;
	}

	private static class SampleCoordAscending implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			Sample s1 = (Sample)o1;
			Sample s2 = (Sample)o2;
			if (s1.xpos != s2.xpos) return (int)(s1.xpos - s2.xpos);
			if (s1.ypos != s2.ypos) return (int)(s1.ypos - s2.ypos);
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

	static Poly.Type getStyle(NodeInst ni)
	{
		// layer style
		Poly.Type sty = null;
		if (ni.getProto() == Artwork.tech.filledBoxNode)             sty = Poly.Type.FILLED; else
		if (ni.getProto() == Artwork.tech.boxNode)                   sty = Poly.Type.CLOSED; else
		if (ni.getProto() == Artwork.tech.crossedBoxNode)            sty = Poly.Type.CROSSED; else
		if (ni.getProto() == Artwork.tech.filledPolygonNode)         sty = Poly.Type.FILLED; else
		if (ni.getProto() == Artwork.tech.closedPolygonNode)         sty = Poly.Type.CLOSED; else
		if (ni.getProto() == Artwork.tech.openedPolygonNode)         sty = Poly.Type.OPENED; else
		if (ni.getProto() == Artwork.tech.openedDottedPolygonNode)   sty = Poly.Type.OPENEDT1; else
		if (ni.getProto() == Artwork.tech.openedDashedPolygonNode)   sty = Poly.Type.OPENEDT2; else
		if (ni.getProto() == Artwork.tech.openedThickerPolygonNode)  sty = Poly.Type.OPENEDT3; else
		if (ni.getProto() == Artwork.tech.filledCircleNode)          sty = Poly.Type.DISC; else
		if (ni.getProto() == Artwork.tech.circleNode)
		{
			sty = Poly.Type.CIRCLE;
			double [] angles = ni.getArcDegrees();
			if (angles[0] != 0 || angles[1] != 0) sty = Poly.Type.CIRCLEARC;
		} else if (ni.getProto() == Artwork.tech.thickCircleNode)
		{
			sty = Poly.Type.THICKCIRCLE;
			double [] angles = ni.getArcDegrees();
			if (angles[0] != 0 || angles[1] != 0) sty = Poly.Type.THICKCIRCLEARC;
		} else if (ni.getProto() == Generic.tech.invisiblePinNode)
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
			System.out.println("Cannot determine style to use for " + ni.getProto().describe() +
				" node in " + ni.getParent().describe());
		return sty;
	}

	static Generate.NodeLayerDetails [] makeNodeScaledUniformly(Example nelist, NodeProto np, Generate.LayerInfo [] lis)
	{
		// count the number of real layers in the node
		int count = 0;
		for(Sample ns = nelist.firstsample; ns != null; ns = ns.nextsample)
		{
			if (ns.layer != null && ns.layer != Generic.tech.portNode) count++;
		}

		Generate.NodeLayerDetails [] nodeLayers = new Generate.NodeLayerDetails[count];
		count = 0;
		for(Sample ns = nelist.firstsample; ns != null; ns = ns.nextsample)
		{
			Rectangle2D nodeBounds = us_tecedgetbbox(ns.node);
			AffineTransform trans = ns.node.rotateOut();

			// see if there is polygonal information
			Point2D [] us_tecedmakep = null;
			int [] us_tecedmakefactor = null;
			Point2D [] points = null;
			Variable var = null;
			if (ns.node.getProto() == Artwork.tech.filledPolygonNode ||
				ns.node.getProto() == Artwork.tech.closedPolygonNode ||
				ns.node.getProto() == Artwork.tech.openedPolygonNode ||
				ns.node.getProto() == Artwork.tech.openedDottedPolygonNode ||
				ns.node.getProto() == Artwork.tech.openedDashedPolygonNode ||
				ns.node.getProto() == Artwork.tech.openedThickerPolygonNode)
			{
				points = ns.node.getTrace();
			}
			if (points != null)
			{
				// fill the array
				us_tecedmakep = new Point2D[points.length];
				us_tecedmakefactor = new int[points.length];
				for(int i=0; i<points.length; i++)
				{
					us_tecedmakep[i] = new Point2D.Double(nodeBounds.getCenterX() + points[i].getX(),
						nodeBounds.getCenterY() + points[i].getY());
					trans.transform(us_tecedmakep[i], us_tecedmakep[i]);
					us_tecedmakefactor[i] = FROMCENTX|FROMCENTY;
				}
			} else
			{
				// see if it is an arc of a circle
				double [] angles = null;
				if (ns.node.getProto() == Artwork.tech.circleNode || ns.node.getProto() == Artwork.tech.thickCircleNode)
				{
					angles = ns.node.getArcDegrees();
					if (angles[0] == 0 && angles[1] == 0) angles = null;
				}
	
				// set sample description
				if (angles != null)
				{
					// handle circular arc sample
					us_tecedmakep = new Point2D[3];
					us_tecedmakefactor = new int[3];
					us_tecedmakep[0] = new Point2D.Double(nodeBounds.getCenterX(), nodeBounds.getCenterY());
					double dist = nodeBounds.getMaxX() - nodeBounds.getCenterX();
					us_tecedmakep[1] = new Point2D.Double(nodeBounds.getCenterX() + dist * Math.cos(angles[0]),
						nodeBounds.getCenterY() + dist * Math.sin(angles[0]));
					trans.transform(us_tecedmakep[1], us_tecedmakep[1]);
					us_tecedmakefactor[0] = FROMCENTX|FROMCENTY;
					us_tecedmakefactor[1] = RATIOCENTX|RATIOCENTY;
					us_tecedmakefactor[2] = RATIOCENTX|RATIOCENTY;
				} else if (ns.node.getProto() == Artwork.tech.circleNode || ns.node.getProto() == Artwork.tech.thickCircleNode ||
					ns.node.getProto() == Artwork.tech.filledCircleNode)
				{
					// handle circular sample
					us_tecedmakep = new Point2D[2];
					us_tecedmakefactor = new int[2];
					us_tecedmakep[0] = new Point2D.Double(nodeBounds.getCenterX(), nodeBounds.getCenterY());
					us_tecedmakep[1] = new Point2D.Double(nodeBounds.getMaxX(), nodeBounds.getCenterY());
					us_tecedmakefactor[0] = FROMCENTX|FROMCENTY;
					us_tecedmakefactor[1] = TOEDGERIGHT|FROMCENTY;
				} else
				{
					// rectangular sample: get the bounding box in (px, py)
					us_tecedmakep = new Point2D[2];
					us_tecedmakefactor = new int[2];
					us_tecedmakep[0] = new Point2D.Double(nodeBounds.getMinX(), nodeBounds.getMinY());
					us_tecedmakep[1] = new Point2D.Double(nodeBounds.getMaxX(), nodeBounds.getMaxY());
	
					// preset stretch factors to go to the edges of the box
					us_tecedmakefactor[0] = TOEDGELEFT|TOEDGEBOT;
					us_tecedmakefactor[1] = TOEDGERIGHT|TOEDGETOP;
				}
			}

			// add the rule to the collection
			Technology.TechPoint [] newrule = us_tecedstretchpoints(us_tecedmakep, us_tecedmakefactor, ns, np, nelist);
			if (newrule == null)
			{
				System.out.println("Error creating stretch point in " + np.describe());
				return null;
			}
			ns.msg = Manipulate.getValueOnNode(ns.node);
			if (ns.msg != null && ns.msg.length() == 0) ns.msg = null;
			ns.values = newrule;

			// stop now if a highlight or port object
			if (ns.layer == null || ns.layer == Generic.tech.portNode) continue;

			// determine the layer
			Generate.LayerInfo layer = null;
			String desiredLayer = ns.layer.getName().substring(6);
			for(int i=0; i<lis.length; i++)
			{
				if (desiredLayer.equals(lis[i].name)) { layer = lis[i];   break; }
			}
			if (layer == null)
			{
				System.out.println("Cannot find layer " + desiredLayer);
				return null;
			}
			nodeLayers[count] = new Generate.NodeLayerDetails();
			nodeLayers[count].layer = layer;
			nodeLayers[count].ns = ns;
			nodeLayers[count].style = getStyle(ns.node);
			nodeLayers[count].representation = Technology.NodeLayer.POINTS;
			if (nodeLayers[count].style == Poly.Type.CROSSED ||
				nodeLayers[count].style == Poly.Type.FILLED ||
				nodeLayers[count].style == Poly.Type.CLOSED)
			{
				nodeLayers[count].representation = Technology.NodeLayer.BOX;
//				if (r.count == 16)
//				{
//					nodeLayers[count].representation = Technology.NodeLayer.MINBOX;
//					tlist.layerlist[i].count = 4;
//				}
			}
			nodeLayers[count].values = ns.values;
			count++;
		}
		return nodeLayers;
	}

	static Generate.NodeLayerDetails [] us_tecedmakeprim(Example nelist, Cell np, Generate.LayerInfo [] lis)
	{	
		// if there is only one example: make sample scale with edge
		if (nelist.nextexample == null)
		{
			return makeNodeScaledUniformly(nelist, np, lis);
		}

		// count the number of real layers in the node
		int count = 0;
		for(Sample ns = nelist.firstsample; ns != null; ns = ns.nextsample)
		{
			if (ns.layer != null && ns.layer != Generic.tech.portNode) count++;
		}

		Generate.NodeLayerDetails [] nodeLayers = new Generate.NodeLayerDetails[count];
		count = 0;

		// look at every sample "ns" in the main example "nelist"
		for(Sample ns = nelist.firstsample; ns != null; ns = ns.nextsample)
		{
			// ignore grab point specification
			if (ns.layer == Generic.tech.cellCenterNode) continue;
			AffineTransform trans = ns.node.rotateOut();
			Rectangle2D nodeBounds = us_tecedgetbbox(ns.node);

			// determine the layer
			Generate.LayerInfo giLayer = null;
			if (ns.layer != null && ns.layer != Generic.tech.portNode)
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
			nelist.studysample = ns;
			Generate.NodeLayerDetails multiRule = null;
			for(Example ne = nelist.nextexample; ne != null; ne = ne.nextexample)
			{
				// count number of samples associated with the main sample
				int total = 0;
				for(Sample nso = ne.firstsample; nso != null; nso = nso.nextsample)
					if (nso.assoc == ns)
				{
					ne.studysample = nso;
					total++;
				}
				if (total == 0)
				{
					us_tecedpointout(ns.node, ns.node.getParent());
					System.out.println("Still unassociated sample in " + np.describe() + " (shouldn't happen)");
					return null;
				}

				// if there are multiple associations, it must be a contact cut
				if (total > 1)
				{
					// make sure the layer is real geometry, not highlight or a port
					if (ns.layer == null || ns.layer == Generic.tech.portNode)
					{
						us_tecedpointout(ns.node, ns.node.getParent());
						System.out.println("Only contact layers may be iterated in examples of " + np.describe());
						return null;
					}

					// add the rule
					multiRule = us_tecedmulticut(ns, nelist, np);
					if (multiRule != null) break;
				}
			}
			if (multiRule != null)
			{
				multiRule.layer = giLayer;
				nodeLayers[count] = multiRule;
				count++;
				continue;
			}

			// associations done for this sample, now analyze them
			Point2D [] us_tecedmakep = null;
			int [] us_tecedmakefactor = null;
			Point2D [] points = null;
			if (ns.node.getProto() == Artwork.tech.filledPolygonNode ||
				ns.node.getProto() == Artwork.tech.closedPolygonNode ||
				ns.node.getProto() == Artwork.tech.openedPolygonNode ||
				ns.node.getProto() == Artwork.tech.openedDottedPolygonNode ||
				ns.node.getProto() == Artwork.tech.openedDashedPolygonNode ||
				ns.node.getProto() == Artwork.tech.openedThickerPolygonNode)
			{
				points = ns.node.getTrace();
			}
			int trueCount = 0;
			if (points != null)
			{	
				// make sure the arrays hold "count" points
				us_tecedmakep = new Point2D[points.length];
				us_tecedmakefactor = new int[points.length];

				for(int i=0; i<points.length; i++)
				{
					us_tecedmakep[i] = new Point2D.Double(nodeBounds.getCenterX() + points[i].getX(),
						nodeBounds.getCenterY() + points[i].getY());
					trans.transform(us_tecedmakep[i], us_tecedmakep[i]);
				}
				trueCount = points.length;
			} else
			{
				// make sure the arrays hold enough points
				double [] angles = null;
				if (ns.node.getProto() == Artwork.tech.circleNode || ns.node.getProto() == Artwork.tech.thickCircleNode)
				{
					angles = ns.node.getArcDegrees();
					if (angles[0] == 0 && angles[1] == 0) angles = null;
				}
				int minFactor = 0;
				if (angles == null)
				{
//					Variable var2 = ns.node.getVar(Generate.MINSIZEBOX_KEY);
//					if (var2 != null) minFactor = 2;
				}

				// set sample description
				if (angles != null)
				{
					// handle circular arc sample
					us_tecedmakep = new Point2D[3];
					us_tecedmakefactor = new int[3];
					us_tecedmakep[0] = new Point2D.Double(nodeBounds.getCenterX(), nodeBounds.getCenterY());
					double dist = nodeBounds.getMaxX() - nodeBounds.getCenterX();
					us_tecedmakep[1] = new Point2D.Double(nodeBounds.getCenterX() + dist * Math.cos(angles[0]),
						nodeBounds.getCenterY() + dist * Math.sin(angles[0]));
					trans.transform(us_tecedmakep[1], us_tecedmakep[1]);
					trueCount = 3;
				} else if (ns.node.getProto() == Artwork.tech.circleNode || ns.node.getProto() == Artwork.tech.thickCircleNode ||
					ns.node.getProto() == Artwork.tech.filledCircleNode)
				{
					// handle circular sample
					us_tecedmakep = new Point2D[2+minFactor];
					us_tecedmakefactor = new int[2+minFactor];
					us_tecedmakep[0] = new Point2D.Double(nodeBounds.getCenterX(), nodeBounds.getCenterY());
					us_tecedmakep[0] = new Point2D.Double(nodeBounds.getMaxX(), nodeBounds.getCenterY());
					trueCount = 2;
				} else
				{
					// rectangular sample: get the bounding box in (us_tecedmakepx, us_tecedmakepy)
					us_tecedmakep = new Point2D[2+minFactor];
					us_tecedmakefactor = new int[2+minFactor];
					us_tecedmakep[0] = new Point2D.Double(nodeBounds.getMinX(), nodeBounds.getMinY());
					us_tecedmakep[1] = new Point2D.Double(nodeBounds.getMaxX(), nodeBounds.getMaxY());
					trueCount = 2;
				}
				if (minFactor > 1)
				{
					us_tecedmakep[2] = new Point2D.Double(us_tecedmakep[0].getX(),us_tecedmakep[0].getY());
					us_tecedmakep[3] = new Point2D.Double(us_tecedmakep[1].getX(),us_tecedmakep[1].getY());
				}
			}

			double [] us_tecedmakeleftdist = new double[us_tecedmakefactor.length];
			double [] us_tecedmakerightdist = new double[us_tecedmakefactor.length];
			double [] us_tecedmakebotdist = new double[us_tecedmakefactor.length];
			double [] us_tecedmaketopdist = new double[us_tecedmakefactor.length];
			double [] us_tecedmakecentxdist = new double[us_tecedmakefactor.length];
			double [] us_tecedmakecentydist = new double[us_tecedmakefactor.length];
			double [] us_tecedmakeratiox = new double[us_tecedmakefactor.length];
			double [] us_tecedmakeratioy = new double[us_tecedmakefactor.length];
			for(int i=0; i<us_tecedmakefactor.length; i++)
			{
				us_tecedmakeleftdist[i] = us_tecedmakep[i].getX() - nelist.lx;
				us_tecedmakerightdist[i] = nelist.hx - us_tecedmakep[i].getX();
				us_tecedmakebotdist[i] = us_tecedmakep[i].getY() - nelist.ly;
				us_tecedmaketopdist[i] = nelist.hy - us_tecedmakep[i].getY();
				us_tecedmakecentxdist[i] = us_tecedmakep[i].getX() - (nelist.lx+nelist.hx)/2;
				us_tecedmakecentydist[i] = us_tecedmakep[i].getY() - (nelist.ly+nelist.hy)/2;
				if (nelist.hx == nelist.lx) us_tecedmakeratiox[i] = 0; else
					us_tecedmakeratiox[i] = (us_tecedmakep[i].getX() - (nelist.lx+nelist.hx)/2) / (nelist.hx-nelist.lx);
				if (nelist.hy == nelist.ly) us_tecedmakeratioy[i] = 0; else
					us_tecedmakeratioy[i] = (us_tecedmakep[i].getY() - (nelist.ly+nelist.hy)/2) / (nelist.hy-nelist.ly);
				if (i < trueCount)
					us_tecedmakefactor[i] = TOEDGELEFT | TOEDGERIGHT | TOEDGETOP | TOEDGEBOT | FROMCENTX |
						FROMCENTY | RATIOCENTX | RATIOCENTY; else
							us_tecedmakefactor[i] = FROMCENTX | FROMCENTY;
			}

			Point2D [] us_tecedmakec = new Point2D[us_tecedmakefactor.length];
			for(Example ne = nelist.nextexample; ne != null; ne = ne.nextexample)
			{
				NodeInst ni = ne.studysample.node;
				AffineTransform oTrans = ni.rotateOut();
				Rectangle2D oNodeBounds = us_tecedgetbbox(ni);
				Point2D [] oPoints = null;
				if (ni.getProto() == Artwork.tech.filledPolygonNode ||
					ni.getProto() == Artwork.tech.closedPolygonNode ||
					ni.getProto() == Artwork.tech.openedPolygonNode ||
					ni.getProto() == Artwork.tech.openedDottedPolygonNode ||
					ni.getProto() == Artwork.tech.openedDashedPolygonNode ||
					ni.getProto() == Artwork.tech.openedThickerPolygonNode)
				{
					oPoints = ni.getTrace();
				}
				int newCount = 2;
				if (oPoints != null)
				{
					newCount = oPoints.length;
					for(int i=0; i<Math.min(trueCount, newCount); i++)
					{
						us_tecedmakec[i] = new Point2D.Double(oNodeBounds.getCenterX() + oPoints[i].getX(),
							oNodeBounds.getCenterY() + oPoints[i].getY());
						oTrans.transform(us_tecedmakec[i], us_tecedmakec[i]);
					}
				} else
				{
					double [] angles = null;
					if (ni.getProto() == Artwork.tech.circleNode || ni.getProto() == Artwork.tech.thickCircleNode)
					{
						angles = ni.getArcDegrees();
						if (angles[0] == 0 && angles[1] == 0) angles = null;
					}
					if (angles != null)
					{
						us_tecedmakec[0] = new Point2D.Double(oNodeBounds.getCenterX(), oNodeBounds.getCenterY());
						double dist = oNodeBounds.getMaxX() - oNodeBounds.getCenterX();
						us_tecedmakec[1] = new Point2D.Double(oNodeBounds.getCenterX() + dist * Math.cos(angles[0]),
								oNodeBounds.getCenterY() + dist * Math.sin(angles[0]));
						oTrans.transform(us_tecedmakec[1], us_tecedmakec[1]);
					} else if (ni.getProto() == Artwork.tech.circleNode || ni.getProto() == Artwork.tech.thickCircleNode ||
						ni.getProto() == Artwork.tech.filledCircleNode)
					{
						us_tecedmakec[0] = new Point2D.Double(oNodeBounds.getCenterX(), oNodeBounds.getCenterY());
						us_tecedmakec[1] = new Point2D.Double(oNodeBounds.getMaxX(), oNodeBounds.getCenterY());
					} else
					{
						us_tecedmakec[0] = new Point2D.Double(oNodeBounds.getMinX(), oNodeBounds.getMinY());
						us_tecedmakec[1] = new Point2D.Double(oNodeBounds.getMaxX(), oNodeBounds.getMaxY());
					}
				}
				if (newCount != trueCount)
				{
					us_tecedpointout(ni, ni.getParent());
					System.out.println("Main example of " + us_tecedsamplename(ne.studysample.layer) +
						" has " + trueCount + " points but this has " + newCount + " in " + np.describe());
					return null;
				}

				for(int i=0; i<trueCount; i++)
				{
					// see if edges are fixed distance from example edge
					if (us_tecedmakeleftdist[i] != us_tecedmakec[i].getX() - ne.lx) us_tecedmakefactor[i] &= ~TOEDGELEFT;
					if (us_tecedmakerightdist[i] != ne.hx - us_tecedmakec[i].getX()) us_tecedmakefactor[i] &= ~TOEDGERIGHT;
					if (us_tecedmakebotdist[i] != us_tecedmakec[i].getY() - ne.ly) us_tecedmakefactor[i] &= ~TOEDGEBOT;
					if (us_tecedmaketopdist[i] != ne.hy - us_tecedmakec[i].getY()) us_tecedmakefactor[i] &= ~TOEDGETOP;

					// see if edges are fixed distance from example center
					if (us_tecedmakecentxdist[i] != us_tecedmakec[i].getX() - (ne.lx+ne.hx)/2) us_tecedmakefactor[i] &= ~FROMCENTX;
					if (us_tecedmakecentydist[i] != us_tecedmakec[i].getY() - (ne.ly+ne.hy)/2) us_tecedmakefactor[i] &= ~FROMCENTY;

					// see if edges are fixed ratio from example center
					double r = 0;
					if (ne.hx != ne.lx)
						r = (us_tecedmakec[i].getX() - (ne.lx+ne.hx)/2) / (ne.hx-ne.lx);
					if (r != us_tecedmakeratiox[i]) us_tecedmakefactor[i] &= ~RATIOCENTX;
					if (ne.hy == ne.ly) r = 0; else
						r = (us_tecedmakec[i].getY() - (ne.ly+ne.hy)/2) / (ne.hy-ne.ly);
					if (r != us_tecedmakeratioy[i]) us_tecedmakefactor[i] &= ~RATIOCENTY;
				}

				// make sure port information is on the primary example
				if (ns.layer != Generic.tech.portNode) continue;

				// check port angle
				Variable var = ns.node.getVar(Generate.PORTANGLE_KEY);
				Variable var2 = ni.getVar(Generate.PORTANGLE_KEY);
				if (var == null && var2 != null)
				{
					us_tecedpointout(null, np);
					System.out.println("Warning: moving port angle to main example of " + np.describe());
					ns.node.newVar(Generate.PORTANGLE_KEY, var2.getObject());
				}

				// check port range
				var = ns.node.getVar(Generate.PORTRANGE_KEY);
				var2 = ni.getVar(Generate.PORTRANGE_KEY);
				if (var == null && var2 != null)
				{
					us_tecedpointout(null, np);
					System.out.println("Warning: moving port range to main example of " + np.describe());
					ns.node.newVar(Generate.PORTRANGE_KEY, var2.getObject());
				}

				// check connectivity
				var = ns.node.getVar(Generate.CONNECTION_KEY);
				var2 = ni.getVar(Generate.CONNECTION_KEY);
				if (var == null && var2 != null)
				{
					us_tecedpointout(null, np);
					System.out.println("Warning: moving port connections to main example of " + np.describe());
					ns.node.newVar(Generate.CONNECTION_KEY, var2.getObject());
				}
			}

			// error check for the highlight layer
			if (ns.layer == null)
			{
				for(int i=0; i<trueCount; i++)
					if ((us_tecedmakefactor[i]&(TOEDGELEFT|TOEDGERIGHT)) == 0 ||
						(us_tecedmakefactor[i]&(TOEDGETOP|TOEDGEBOT)) == 0)
				{
					us_tecedpointout(ns.node, ns.node.getParent());
					System.out.println("Highlight must be constant distance from edge in " + np.describe());
					return null;
				}
			}

			// finally, make a rule for this sample
			Technology.TechPoint [] newrule = us_tecedstretchpoints(us_tecedmakep, us_tecedmakefactor, ns, np, nelist);
			if (newrule == null) return null;

			// add the rule to the global list
			ns.msg = Manipulate.getValueOnNode(ns.node);
			if (ns.msg != null && ns.msg.length() == 0) ns.msg = null;
			ns.values = newrule;

			// stop now if a highlight or port object
			if (ns.layer == null || ns.layer == Generic.tech.portNode) continue;

			nodeLayers[count] = new Generate.NodeLayerDetails();
			nodeLayers[count].layer = giLayer;
			nodeLayers[count].ns = ns;
			nodeLayers[count].style = getStyle(ns.node);
			nodeLayers[count].representation = Technology.NodeLayer.POINTS;
			if (nodeLayers[count].style == Poly.Type.CROSSED ||
				nodeLayers[count].style == Poly.Type.FILLED ||
				nodeLayers[count].style == Poly.Type.CLOSED)
			{
				nodeLayers[count].representation = Technology.NodeLayer.BOX;
//				if (r.count == 16)
//				{
//					nodeLayers[count].representation = Technology.NodeLayer.MINBOX;
//					tlist.layerlist[i].count = 4;
//				}
			}
			nodeLayers[count].values = ns.values;
			count++;
		}
		if (count != nodeLayers.length)
			System.out.println("Generated only " + count + " of " + nodeLayers.length + " layers for " + np.describe());
		return nodeLayers;
	}

	/**
	 * Method to return the actual bounding box of layer node "ni" in the
	 * reference variables "lx", "hx", "ly", and "hy"
	 */
	static Rectangle2D us_tecedgetbbox(NodeInst ni)
	{
		Rectangle2D bounds = ni.getBounds();
		if (ni.getProto() == Generic.tech.portNode)
		{
			double portShrink = 2;
			bounds.setRect(bounds.getMinX() + portShrink, bounds.getMinY() + portShrink,
				bounds.getWidth()-portShrink*2, bounds.getHeight()-portShrink*2);
		}
		return bounds;
	}

	/**
	 * Method to adjust the "count"-long array of points in "px" and "py" according
	 * to the stretch factor bits in "factor" and return an array that describes
	 * these points.  Returns zero on error.
	 */
	static Technology.TechPoint [] us_tecedstretchpoints(Point2D [] pts, int [] factor,
		Sample ns, NodeProto np, Example nelist)
	{
		Technology.TechPoint [] newrule = new Technology.TechPoint[pts.length];

		for(int i=0; i<pts.length; i++)
		{
			// determine the X algorithm
			EdgeH horiz = null;
			if ((factor[i]&TOEDGELEFT) != 0)
			{
				// left edge rule
				horiz = EdgeH.fromLeft(pts[i].getX()-nelist.lx);
			} else if ((factor[i]&TOEDGERIGHT) != 0)
			{
				// right edge rule
				horiz = EdgeH.fromRight(nelist.hx-pts[i].getX());
			} else if ((factor[i]&FROMCENTX) != 0)
			{
				// center rule
				horiz = EdgeH.fromCenter(pts[i].getX()-(nelist.lx+nelist.hx)/2);
			} else if ((factor[i]&RATIOCENTX) != 0)
			{
				// constant stretch rule
				if (nelist.hx == nelist.lx)
				{
					horiz = EdgeH.makeCenter();
				} else
				{
					horiz = new EdgeH((pts[i].getX()-(nelist.lx+nelist.hx)/2) / (nelist.hx-nelist.lx), 0);
				}
			} else
			{
				us_tecedpointout(ns.node, ns.node.getParent());
				System.out.println("Cannot determine X stretching rule for layer " + us_tecedsamplename(ns.layer) +
					" in " + np.describe());
				return null;
			}
	
			// determine the Y algorithm
			EdgeV vert = null;
			if ((factor[i]&TOEDGEBOT) != 0)
			{
				// bottom edge rule
				vert = EdgeV.fromBottom(pts[i].getY()-nelist.ly);
			} else if ((factor[i]&TOEDGETOP) != 0)
			{
				// top edge rule
				vert = EdgeV.fromTop(nelist.hy-pts[i].getY());
			} else if ((factor[i]&FROMCENTY) != 0)
			{
				// center rule
				vert = EdgeV.fromCenter(pts[i].getY()-(nelist.ly+nelist.hy)/2);
			} else if ((factor[i]&RATIOCENTY) != 0)
			{
				// constant stretch rule
				if (nelist.hy == nelist.ly)
				{
					vert = EdgeV.makeCenter();
				} else
				{
					vert = new EdgeV((pts[i].getY()-(nelist.ly+nelist.hy)/2) / (nelist.hy-nelist.ly), 0);
				}
			} else
			{
				us_tecedpointout(ns.node, ns.node.getParent());
				System.out.println("Cannot determine Y stretching rule for layer " + us_tecedsamplename(ns.layer) +
					" in " + np.describe());
				return null;
			}
			newrule[i] = new Technology.TechPoint(horiz, vert);
		}
		return newrule;
	}
	
	private static Sample us_tecedneedhighlightlayer(Example nelist, Cell np)
	{
		// find the highlight layer
		for(Sample hs = nelist.firstsample; hs != null; hs = hs.nextsample)
			if (hs.layer == null) return hs;
	
		us_tecedpointout(null, np);
		System.out.println("No highlight layer on contact " + np.describe());
		return null;
	}

	/**
	 * Method to build a rule for multiple contact-cut sample "ns" from the
	 * overall example list in "nelist".  Returns true on error.
	 */
	static Generate.NodeLayerDetails us_tecedmulticut(Sample ns, Example nelist, Cell np)
	{
		// find the highlight layer
		Sample hs = us_tecedneedhighlightlayer(nelist, np);
		if (hs == null) return null;
		Rectangle2D highlightBounds = hs.node.getBounds();

		// determine size of each cut
		Rectangle2D nodeBounds = ns.node.getBounds();
		double multixs = nodeBounds.getWidth();
		double multiys = nodeBounds.getHeight();

		// determine indentation of cuts
		double multiindent = nodeBounds.getMinX() - highlightBounds.getMinX();
		if (highlightBounds.getMaxX() - nodeBounds.getMaxX() != multiindent ||
			nodeBounds.getMinY() - highlightBounds.getMinY() != multiindent ||
			highlightBounds.getMaxY() - nodeBounds.getMaxY() != multiindent)
		{
			us_tecedpointout(ns.node, ns.node.getParent());
			System.out.println("Multiple contact cuts must be indented uniformly in " + np.describe());
			return null;
		}

		// look at every example after the first
		double xsep = -1, ysep = -1;
		for(Example ne = nelist.nextexample; ne != null; ne = ne.nextexample)
		{
			// count number of samples equivalent to the main sample
			int total = 0;
			for(Sample nso = ne.firstsample; nso != null; nso = nso.nextsample)
				if (nso.assoc == ns)
			{
				// make sure size is proper
				Rectangle2D oNodeBounds = nso.node.getBounds();
				if (multixs != oNodeBounds.getWidth() || multiys != oNodeBounds.getHeight())
				{
					us_tecedpointout(nso.node, nso.node.getParent());
					System.out.println("Multiple contact cuts must not differ in size in " + np.describe());
					return null;
				}
				total++;
			}

			// allocate space for these samples
			Sample [] nslist = new Sample[total];

			// fill the list of samples
			int fill = 0;
			for(Sample nso = ne.firstsample; nso != null; nso = nso.nextsample)
				if (nso.assoc == ns) nslist[fill++] = nso;

			// analyze the samples for separation
			for(int i=1; i<total; i++)
			{
				// find separation
				Rectangle2D thisNodeBounds = nslist[i].node.getBounds();
				Rectangle2D lastNodeBounds = nslist[i-1].node.getBounds();
				double sepx = Math.abs(lastNodeBounds.getCenterX() - thisNodeBounds.getCenterX());
				double sepy = Math.abs(lastNodeBounds.getCenterY() - thisNodeBounds.getCenterY());

				// check for validity
				if (sepx < multixs && sepy < multiys)
				{
					us_tecedpointout(nslist[i].node, nslist[i].node.getParent());
					System.out.println("Multiple contact cuts must not overlap in " + np.describe());
					return null;
				}

				// accumulate minimum separation
				if (sepx >= multixs)
				{
					if (xsep < 0) xsep = sepx; else
					{
						if (xsep > sepx) xsep = sepx;
					}
				}
				if (sepy >= multiys)
				{
					if (ysep < 0) ysep = sepy; else
					{
						if (ysep > sepy) ysep = sepy;
					}
				}
			}

			// finally ensure that all separations are multiples of "multisep"
			for(int i=1; i<total; i++)
			{
				// find X separation
				Rectangle2D thisNodeBounds = nslist[i].node.getBounds();
				Rectangle2D lastNodeBounds = nslist[i-1].node.getBounds();
				double sepx = Math.abs(lastNodeBounds.getCenterX() - thisNodeBounds.getCenterX());
				double sepy = Math.abs(lastNodeBounds.getCenterY() - thisNodeBounds.getCenterY());
				if (sepx / xsep * xsep != sepx)
				{
					us_tecedpointout(nslist[i].node, nslist[i].node.getParent());
					System.out.println("Multiple contact cut X spacing must be uniform in " + np.describe());
					return null;
				}

				// find Y separation
				if (sepy / ysep * ysep != sepy)
				{
					us_tecedpointout(nslist[i].node, nslist[i].node.getParent());
					System.out.println("Multiple contact cut Y spacing must be uniform in " + np.describe());
					return null;
				}
			}
		}
		double multisep = xsep - multixs;
		if (multisep != ysep - multiys)
		{
			us_tecedpointout(null, np);
			System.out.println("Multiple contact cut X and Y spacing must be the same in " + np.describe());
			return null;
		}
		ns.values = new Technology.TechPoint[2];
		ns.values[0] = new Technology.TechPoint(EdgeH.fromLeft(1), EdgeV.fromBottom(1));
		ns.values[1] = new Technology.TechPoint(EdgeH.fromLeft(3), EdgeV.fromBottom(3));

		Generate.NodeLayerDetails multiDetails = new Generate.NodeLayerDetails();
		multiDetails.style = getStyle(ns.node);
		multiDetails.representation = Technology.NodeLayer.POINTS;
		if (multiDetails.style == Poly.Type.CROSSED ||
			multiDetails.style == Poly.Type.FILLED ||
			multiDetails.style == Poly.Type.CLOSED)
		{
			multiDetails.representation = Technology.NodeLayer.BOX;
//			if (r.count == 16)
//			{
//				multiDetails.representation = Technology.NodeLayer.MINBOX;
//				tlist.layerlist[i].count = 4;
//			}
		}
		multiDetails.values = ns.values;
		multiDetails.ns = ns;
		multiDetails.multiCut = true;
		multiDetails.multixs = multixs;
		multiDetails.multiys = multiys;
		multiDetails.multiindent = multiindent;
		multiDetails.multisep = multisep;
		return multiDetails;
	}

	/****************************** WRITE TECHNOLOGY AS "JAVA" CODE ******************************/

	/**
	 * Method to dump the layer information in technology "tech" to the stream in
	 * "f".
	 */
	static void us_teceditdumpjavalayers(PrintStream buffWriter, String techName, Generate.LayerInfo [] lList, Generate.GeneralInfo gi)
	{
		// write header
		buffWriter.println("// BE SURE TO INCLUDE THIS TECHNOLOGY IN Technology.initAllTechnologies()");
		buffWriter.println();
		buffWriter.println("/* -*- tab-width: 4 -*-");
		buffWriter.println(" *");
		buffWriter.println(" * Electric(tm) VLSI Design System");
		buffWriter.println(" *");
		buffWriter.println(" * File: " + techName + ".java");
		buffWriter.println(" * " + techName + " technology description");
		buffWriter.println(" * Generated automatically from a library");
		buffWriter.println(" *");
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		buffWriter.println(" * Copyright (c) " + cal.get(Calendar.YEAR) + " Sun Microsystems and Static Free Software");
		buffWriter.println(" *");
		buffWriter.println(" * Electric(tm) is free software; you can redistribute it and/or modify");
		buffWriter.println(" * it under the terms of the GNU General Public License as published by");
		buffWriter.println(" * the Free Software Foundation; either version 2 of the License, or");
		buffWriter.println(" * (at your option) any later version.");
		buffWriter.println(" *");
		buffWriter.println(" * Electric(tm) is distributed in the hope that it will be useful,");
		buffWriter.println(" * but WITHOUT ANY WARRANTY; without even the implied warranty of");
		buffWriter.println(" * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the");
		buffWriter.println(" * GNU General Public License for more details.");
		buffWriter.println(" *");
		buffWriter.println(" * You should have received a copy of the GNU General Public License");
		buffWriter.println(" * along with Electric(tm); see the file COPYING.  If not, write to");
		buffWriter.println(" * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,");
		buffWriter.println(" * Boston, Mass 02111-1307, USA.");
		buffWriter.println(" */");
		buffWriter.println("package com.sun.electric.technology.technologies;");
		buffWriter.println();
	
		// write imports
		buffWriter.println("import com.sun.electric.database.geometry.EGraphics;");
		buffWriter.println("import com.sun.electric.database.geometry.Poly;");
		buffWriter.println("import com.sun.electric.database.prototype.ArcProto;");
		buffWriter.println("import com.sun.electric.database.prototype.PortCharacteristic;");
		buffWriter.println("import com.sun.electric.database.prototype.PortProto;");
		buffWriter.println("import com.sun.electric.database.prototype.NodeProto;");
		buffWriter.println("import com.sun.electric.technology.DRCRules;");
		buffWriter.println("import com.sun.electric.technology.EdgeH;");
		buffWriter.println("import com.sun.electric.technology.EdgeV;");
		buffWriter.println("import com.sun.electric.technology.Layer;");
		buffWriter.println("import com.sun.electric.technology.PrimitiveArc;");
		buffWriter.println("import com.sun.electric.technology.PrimitiveNode;");
		buffWriter.println("import com.sun.electric.technology.PrimitivePort;");
		buffWriter.println("import com.sun.electric.technology.SizeOffset;");
		buffWriter.println("import com.sun.electric.technology.Technology;");
		buffWriter.println("import com.sun.electric.technology.technologies.utils.MOSRules;");
		buffWriter.println();
		buffWriter.println("import java.awt.Color;");
		buffWriter.println();
	
		buffWriter.println("/**");
		buffWriter.println(" * This is the " + gi.description + " Technology.");
		buffWriter.println(" */");
		buffWriter.println("public class " + techName + " extends Technology");
		buffWriter.println("{");
		buffWriter.println("\t/** the " + gi.description + " Technology object. */	public static final " +
				techName + " tech = new " + techName + "();");
		if ((us_tecflags&(HASCONDRC|HASUNCONDRC)) != 0)
		{
			buffWriter.println("\tprivate static final double XX = -1;");
			buffWriter.println("\tprivate double [] conDist, unConDist;");
		}
		buffWriter.println();
	
		buffWriter.println("\tprivate " + techName + "()");
		buffWriter.println("\t{");
		buffWriter.println("\t\tsuper(\"" + techName + "\");");
		buffWriter.println("\t\tsetTechDesc(\"" + gi.description + "\");");
		buffWriter.println("\t\tsetFactoryScale(" + TextUtils.formatDouble(gi.scale) + ", true);   // in nanometers: really " +
			(gi.scale / 1000) + " microns");
		buffWriter.println("\t\tsetMinResistance(" + gi.minres + ");");
		buffWriter.println("\t\tsetMinCapacitance(" + gi.mincap + ");");
		buffWriter.println("\t\tsetGateLengthSubtraction(" + gi.gateShrinkage + ");");
		buffWriter.println("\t\tsetGateIncluded(" + gi.includeGateInResistance + ");");
		buffWriter.println("\t\tsetGroundNetIncluded(" + gi.includeGround + ");");
		buffWriter.println("\t\tsetNoNegatedArcs();");
		buffWriter.println("\t\tsetStaticTechnology();");

		if (gi.transparentColors != null && gi.transparentColors.length > 0)
		{
			buffWriter.println("\t\tsetFactoryTransparentLayers(new Color []");
			buffWriter.println("\t\t{");
			for(int i=0; i<gi.transparentColors.length; i++)
			{
				Color col = gi.transparentColors[i];
				buffWriter.print("\t\t\tnew Color(" + col.getRed() + "," + col.getGreen() + "," + col.getBlue() + ")");
				if (i+1 < gi.transparentColors.length) buffWriter.print(",");
				buffWriter.println();
			}
			buffWriter.println("\t\t});");
		}
		buffWriter.println();
	
		// write the layer declarations
		buffWriter.println("\t\t//**************************************** LAYERS ****************************************");
		for(int i=0; i<lList.length; i++)
		{
			lList[i].javaName = us_teceditconverttojava(lList[i].name);
			buffWriter.print("\t\t/** " + lList[i].name + " layer */");
			buffWriter.println("\t\tLayer " + lList[i].javaName + "_lay = Layer.newInstance(this, \"" + lList[i].name + "\",");
			buffWriter.print("\t\t\tnew EGraphics(");
			if (lList[i].desc.isPatternedOnDisplay())
			{
				if (lList[i].desc.isOutlinedOnDisplay()) buffWriter.print("EGraphics.OUTLINEPAT"); else
					buffWriter.print("EGraphics.PATTERNED");
			} else
			{
				buffWriter.print("EGraphics.SOLID");
			}
			buffWriter.print(", ");
			if (lList[i].desc.isPatternedOnPrinter())
			{
				if (lList[i].desc.isOutlinedOnPrinter()) buffWriter.print("EGraphics.OUTLINEPAT"); else
					buffWriter.print("EGraphics.PATTERNED");
			} else
			{
				buffWriter.print("EGraphics.SOLID");
			}
			String transparent = "0";
			if (lList[i].desc.getTransparentLayer() > 0)
				transparent = "EGraphics.TRANSPARENT_" + lList[i].desc.getTransparentLayer();
			int red = lList[i].desc.getColor().getRed();
			int green = lList[i].desc.getColor().getGreen();
			int blue = lList[i].desc.getColor().getBlue();
			if (red < 0 || red > 255) red = 0;
			if (green < 0 || green > 255) green = 0;
			if (blue < 0 || blue > 255) blue = 0;
			buffWriter.println(", " + transparent + ", " + red + "," + green + "," + blue + ", 0.8,true,");

			boolean hasPattern = false;
			int [] pattern = lList[i].desc.getPattern();
			for(int j=0; j<16; j++) if (pattern[j] != 0) hasPattern = true;
			if (hasPattern)
			{
				for(int j=0; j<16; j++)
				{
					buffWriter.print("\t\t\t");
					if (j == 0) buffWriter.print("new int[] { "); else
						buffWriter.print("\t\t\t");
					String hexValue = Integer.toHexString(pattern[j] & 0xFFFF);
					while (hexValue.length() < 4) hexValue = "0" + hexValue;
					buffWriter.print("0x" + hexValue);
					if (j == 15) buffWriter.print("}));"); else
						buffWriter.print(",   ");
	
					buffWriter.print("// ");
					for(int k=0; k<16; k++)
						if ((pattern[j] & (1 << (15-k))) != 0)
							buffWriter.print("X"); else buffWriter.print(" ");
					buffWriter.println();
				}
				buffWriter.println();
			} else
			{
				buffWriter.println("\t\t\tnew int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));");
			}
		}
	
		// write the layer functions
		buffWriter.println();
		buffWriter.println("\t\t// The layer functions");
		for(int i=0; i<lList.length; i++)
		{
			Layer.Function fun = lList[i].fun;
			int funExtra = lList[i].funExtra;
			String infstr = lList[i].javaName + "_lay.setFunction(Layer.Function.";
			if (fun.isDiff() && (funExtra&Layer.Function.PTYPE) != 0)
			{
				infstr += "DIFFP";
				funExtra &= ~Layer.Function.PTYPE;
			} else if (fun.isDiff() && (funExtra&Layer.Function.NTYPE) != 0)
			{
				infstr += "DIFFN";
				funExtra &= ~Layer.Function.NTYPE;
			} else if (fun == Layer.Function.WELL && (funExtra&Layer.Function.PTYPE) != 0)
			{
				infstr += "WELLP";
				funExtra &= ~Layer.Function.PTYPE;
			} else if (fun == Layer.Function.WELL && (funExtra&Layer.Function.NTYPE) != 0)
			{
				infstr += "WELLN";
				funExtra &= ~Layer.Function.NTYPE;
			} else if (fun == Layer.Function.IMPLANT && (funExtra&Layer.Function.PTYPE) != 0)
			{
				infstr += "IMPLANTP";
				funExtra &= ~Layer.Function.PTYPE;
			} else if (fun == Layer.Function.IMPLANT && (funExtra&Layer.Function.NTYPE) != 0)
			{
				infstr += "IMPLANTN";
				funExtra &= ~Layer.Function.NTYPE;
			} else if (fun.isPoly() && (funExtra&Layer.Function.INTRANS) != 0)
			{
				infstr += "GATE";
				funExtra &= ~Layer.Function.INTRANS;
			} else
			{
				infstr += fun.getConstantName();
			}
			boolean extrafunction = false;
			int [] extras = Layer.Function.getFunctionExtras();
			for(int j=0; j<extras.length; j++)
			{
				if ((funExtra&extras[j]) != 0)
				{
					if (extrafunction) infstr += "|"; else
						infstr += ", ";
					infstr += "Layer.Function.";
					infstr += Layer.Function.getExtraConstantName(extras[j]);
					extrafunction = true;
				}
			}
			infstr += ");";
			buffWriter.println("\t\t" + infstr + "\t\t// " + lList[i].name);
		}
	
		// write the CIF layer names
		for(int j=0; j<lList.length; j++)
		{
			if (lList[j].cif.length() > 0)
			{
				buffWriter.println("\n\t\t// The CIF names");
				for(int i=0; i<lList.length; i++)
					buffWriter.println("\t\t" + lList[i].javaName + "_lay.setFactoryCIFLayer(\"" + lList[i].cif +
						"\");\t\t// " + lList[i].name);
				break;
			}
		}
	
		// write the Calma GDS-II layer number
		for(int j=0; j<lList.length; j++)
		{
			if (lList[j].gds.length() > 0)
			{
				buffWriter.println("\n\t\t// The GDS names");
				for(int i=0; i<lList.length; i++)
					buffWriter.println("\t\t" + lList[i].javaName + "_lay.setFactoryGDSLayer(\"" + lList[i].gds +
						"\");\t\t// " + lList[i].name);
				break;
			}
		}
	
		// write the 3D information
		for(int j=0; j<lList.length; j++)
		{
			if (lList[j].thick3d != 0 || lList[j].height3d != 0)
			{
				buffWriter.println("\n\t\t// The layer height");
				for(int i=0; i<lList.length; i++)
					buffWriter.println("\t\t" + lList[i].javaName + "_lay.setFactory3DInfo(" +
						TextUtils.formatDouble(lList[i].thick3d) + ", " + TextUtils.formatDouble(lList[i].height3d) +
						");\t\t// " + lList[i].name);
				break;
			}
		}
	
		// write the SPICE information
		for(int j=0; j<lList.length; j++)
		{
			if (lList[j].spires != 0 || lList[j].spicap != 0 || lList[j].spiecap != 0)
			{
				buffWriter.println("\n\t\t// The SPICE information");
				for(int i=0; i<lList.length; i++)
					buffWriter.println("\t\t" + lList[i].javaName + "_lay.setFactoryParasitics(" +
						TextUtils.formatDouble(lList[i].spires) + ", " +
						TextUtils.formatDouble(lList[i].spicap) + ", " +
						TextUtils.formatDouble(lList[i].spiecap) + ");\t\t// " + lList[i].name);
				break;
			}
		}
	
		// write design rules
//		if ((us_tecflags&(HASCONDRC|HASUNCONDRC)) != 0)
//		{
//			buffWriter.println("\n\t\t//******************** DESIGN RULES ********************");
//	
//			if ((us_tecflags&HASCONDRC) != 0)
//			{
//				buffWriter.println("\n\t\tconDist = new double[] {");
//				us_teceditdumpjavadrctab(f, us_tecdrc_rules.conlist, tech, FALSE);
//			}
//			if ((us_tecflags&HASUNCONDRC) != 0)
//			{
//				buffWriter.println("\n\t\tunConDist = new double[] {");
//				us_teceditdumpjavadrctab(f, us_tecdrc_rules.unconlist, tech, FALSE);
//			}
//		}
	}
	
//	void us_teceditdumpjavadrctab(FILE *f, void *distances, TECHNOLOGY *tech, BOOLEAN isstring)
//	{
//		REGISTER INTBIG i, j;
//		REGISTER INTBIG amt, *amtlist;
//		CHAR shortname[7], *msg, **distlist;
//	
//		for(i=0; i<6; i++)
//		{
//			buffWriter.println("\t\t\t//            "));
//			for(j=0; j<tech.layercount; j++)
//			{
//				if ((INTBIG)estrlen(us_teclayer_iname[j]) <= i) buffWriter.println(" ")); else
//					buffWriter.println("%c"), us_teclayer_iname[j][i]);
//				buffWriter.println("  "));
//			}
//			buffWriter.println();
//		}
//		if (isstring) distlist = (CHAR **)distances; else
//			amtlist = (INTBIG *)distances;
//		for(j=0; j<tech.layercount; j++)
//		{
//			(void)estrncpy(shortname, us_teclayer_iname[j], 6);
//			shortname[6] = 0;
//			buffWriter.println("\t\t\t/* %-6s */ "), shortname);
//			for(i=0; i<j; i++) buffWriter.println("   "));
//			for(i=j; i<tech.layercount; i++)
//			{
//				if (isstring)
//				{
//					msg = *distlist++;
//					buffWriter.println("x_(\"%s\")"), msg);
//				} else
//				{
//					amt = *amtlist++;
//					if (amt < 0) buffWriter.println("XX")); else
//					{
//						buffWriter.println("%g"), (float)amt/WHOLE);
//					}
//				}
//				if (j != tech.layercount-1 || i != tech.layercount-1)
//					buffWriter.println(","));
//			}
//			buffWriter.println("\n"));
//		}
//		buffWriter.println("\t\t};\n"));
//	}
	
	/**
	 * Method to dump the arc information in technology "tech" to the stream in
	 * "f".
	 */
	static void us_teceditdumpjavaarcs(PrintStream buffWriter, String techName, Generate.ArcInfo [] aList, Generate.GeneralInfo gi)
	{
		// print the header
		buffWriter.println();
		buffWriter.println("\t\t//******************** ARCS ********************");
	
		// now write the arcs
		for(int i=0; i<aList.length; i++)
		{
			aList[i].javaName = us_teceditconverttojava(aList[i].name);
			buffWriter.println("\n\t\t/** " + aList[i].name + " arc */");
			buffWriter.println("\t\tPrimitiveArc " + aList[i].javaName + "_arc = PrimitiveArc.newInstance(this, \"" +
				aList[i].name + "\", " + TextUtils.formatDouble(aList[i].maxWidth) + ", new Technology.ArcLayer []");
			buffWriter.println("\t\t{");
			for(int k=0; k<aList[i].arcDetails.length; k++)
			{
				buffWriter.print("\t\t\tnew Technology.ArcLayer(" + aList[i].arcDetails[k].layer.javaName + "_lay, ");
				buffWriter.print(TextUtils.formatDouble(aList[i].arcDetails[k].width) + ",");
				if (aList[i].arcDetails[k].style == Poly.Type.FILLED) buffWriter.print(" Poly.Type.FILLED)"); else
					buffWriter.print(" Poly.Type.CLOSED)");
				if (k+1 < aList[i].arcDetails.length) buffWriter.print(",");
				buffWriter.println();
			}
			buffWriter.println("\t\t});");
			buffWriter.println("\t\t" + aList[i].javaName + "_arc.setFunction(ArcProto.Function." + aList[i].func.getConstantName() + ");");
			if (aList[i].wipes)
				buffWriter.println("\t\t" + aList[i].javaName + "_arc.setWipable();");
			buffWriter.println("\t\t" + aList[i].javaName + "_arc.setWidthOffset(" +
				TextUtils.formatDouble(aList[i].widthOffset) + ");");
			if (aList[i].fixang)
				buffWriter.println("\t\t" + aList[i].javaName + "_arc.setFactoryFixedAngle(true);");
			if (aList[i].noextend)
				buffWriter.println("\t\t" + aList[i].javaName + "_arc.setFactoryExtended(false);");
			buffWriter.println("\t\t" + aList[i].javaName + "_arc.setFactoryAngleIncrement(" + aList[i].anginc + ");");
			buffWriter.println("\t\tERC.getERCTool().setAntennaRatio(" + aList[i].javaName + ", " +
				TextUtils.formatDouble(aList[i].antennaRatio) + ");");
		}
	}

	/**
	 * Method to make an abbreviation for a string.
	 * @param pt the string to abbreviate.
	 * @param upper true to make it an upper-case abbreviation.
	 * @return the abbreviation for the string.
	 */
	static String makeabbrev(String pt, boolean upper)
	{
		// generate an abbreviated name for this prototype
		StringBuffer infstr = new StringBuffer();
		for(int i=0; i<pt.length(); )
		{
			char chr = pt.charAt(i);
			if (Character.isLetterOrDigit(chr))
			{
				if (upper) infstr.append(Character.toUpperCase(chr)); else
					infstr.append(Character.toLowerCase(chr));
				while (Character.isLetterOrDigit(chr))
				{
					i++;
					if (i >= pt.length()) break;
					chr = pt.charAt(i);
				}
			}
			while (!Character.isLetterOrDigit(chr))
			{
				i++;
				if (i >= pt.length()) break;
				chr = pt.charAt(i);
			}
		}
		return infstr.toString();
	}

	/**
	 * Method to dump the node information in technology "tech" to the stream in
	 * "f".
	 */
	static void us_teceditdumpjavanodes(PrintStream buffWriter, String techName, Generate.NodeInfo [] nList,
		Generate.LayerInfo [] lList, Generate.GeneralInfo gi)
	{
		// make abbreviations for each node
		HashSet abbrevs = new HashSet();
		for(int i=0; i<nList.length; i++)
		{
			String ab = makeabbrev(nList[i].name, false);
	
			// loop until the name is unique
			for(;;)
			{
				// see if a previously assigned abbreviation is the same
				if (!abbrevs.contains(ab)) break;
	
				// name conflicts: change it
				int l = ab.length() - 1;
				char last = ab.charAt(l);
				if (last >= '0' && last <= '8')
				{
					ab = ab.substring(0, l) + (last+1);
				} else
				{
					ab += "0";
				}
			}
			abbrevs.add(ab);
			nList[i].abbrev = ab;
		}
		buffWriter.println();

		// print node information
		buffWriter.println("\t\t//******************** NODES ********************");
		for(int i=0; i<nList.length; i++)
		{
			// header comment
			String ab = nList[i].abbrev;
			buffWriter.println();
			buffWriter.println("\t\t/** " + nList[i].name + " */");
	
			buffWriter.print("\t\tPrimitiveNode " + ab + "_node = PrimitiveNode.newInstance(\"" +
				nList[i].name + "\", this, " + TextUtils.formatDouble(nList[i].xSize) + ", " +
				TextUtils.formatDouble(nList[i].ySize) + ", ");
			if (nList[i].so == null) buffWriter.println("null,"); else
			{
				buffWriter.println("new SizeOffset(" + TextUtils.formatDouble(nList[i].so.getLowXOffset()) + ", " +
					TextUtils.formatDouble(nList[i].so.getHighXOffset()) + ", " +
					TextUtils.formatDouble(nList[i].so.getLowYOffset()) + ", " +
					TextUtils.formatDouble(nList[i].so.getHighYOffset()) + "),");
			}
	
			// print layers
			buffWriter.println("\t\t\tnew Technology.NodeLayer []");
			buffWriter.println("\t\t\t{");
			int tot = nList[i].nodeLayers.length;
			for(int j=0; j<tot; j++)
			{
//				if (nlist.special == PrimitiveNode.SERPTRANS) plist = &nlist.gra[j].basics; else
//					plist = &nlist.layerlist[j];
				int portNum = nList[i].nodeLayers[j].portIndex;
				buffWriter.print("\t\t\t\tnew Technology.NodeLayer(" +
					nList[i].nodeLayers[j].layer.javaName + "_lay, " + portNum + ", Poly.Type." +
					nList[i].nodeLayers[j].style.getConstantName() + ",");
				switch (nList[i].nodeLayers[j].representation)
				{
					case Technology.NodeLayer.BOX:
						buffWriter.print(" Technology.NodeLayer.BOX,");     break;
					case Technology.NodeLayer.MINBOX:
						buffWriter.print(" Technology.NodeLayer.MINBOX,");  break;
					case Technology.NodeLayer.POINTS:
						buffWriter.print(" Technology.NodeLayer.POINTS,");  break;
					default:
						buffWriter.print(" Technology.NodeLayer.????,");    break;
				}
				buffWriter.println(" new Technology.TechPoint [] {");
				int totLayers = nList[i].nodeLayers[j].values.length;
				for(int k=0; k<totLayers; k++)
				{
					Technology.TechPoint tp = nList[i].nodeLayers[j].values[k];
					buffWriter.print("\t\t\t\t\tnew Technology.TechPoint(" +
						us_tecededgelabeljava(tp, false) + ", " + us_tecededgelabeljava(tp, true) + ")");
					if (k < totLayers-1) buffWriter.println(","); else
						buffWriter.print("}");
				}
//				if (nlist.special == PrimitiveNode.SERPTRANS)
//				{
//					buffWriter.println(", %g, %g, %g, %g"),
//						nlist.gra[j].lwidth / (float)WHOLE, nlist.gra[j].rwidth / (float)WHOLE,
//						nlist.gra[j].extendb / (float)WHOLE, nlist.gra[j].extendt / (float)WHOLE);
//				}
				buffWriter.print(")");
				if (j+1 < tot) buffWriter.print(",");
				buffWriter.println();
			}
			buffWriter.println("\t\t\t});");
	
			// print ports
			buffWriter.println("\t\t" + ab + "_node.addPrimitivePorts(new PrimitivePort[]");
			buffWriter.println("\t\t\t{");
			int numPorts = nList[i].nodePortDetails.length;
			for(int j=0; j<numPorts; j++)
			{
				Generate.NodePortDetails portDetail = nList[i].nodePortDetails[j];
				buffWriter.print("\t\t\t\tPrimitivePort.newInstance(this, " + ab + "_node, new ArcProto [] {");
				Generate.ArcInfo [] conns = portDetail.connections;
				for(int l=0; l<conns.length; l++)
				{
					buffWriter.print(conns[l].javaName + "_arc");
					if (l+1 < conns.length) buffWriter.print(", ");
				}
				buffWriter.println("}, \"" + portDetail.name + "\", " + portDetail.angle + "," +
					portDetail.range + ", " + portDetail.netIndex + ", PortCharacteristic.UNKNOWN,");
				buffWriter.print("\t\t\t\t\t" + us_tecededgelabeljava(portDetail.values[0], false) + ", " +
					us_tecededgelabeljava(portDetail.values[0], true) + ", " +
					us_tecededgelabeljava(portDetail.values[1], false) + ", " +
					us_tecededgelabeljava(portDetail.values[1], true) + ")");
	
				if (j+1 < numPorts) buffWriter.print(",");
				buffWriter.println();
			}
			buffWriter.println("\t\t\t});");
	
			// print the node information
			PrimitiveNode.Function fun = nList[i].func;
			buffWriter.println("\t\t" + ab + "_node.setFunction(PrimitiveNode.Function." + fun.getConstantName() + ");");
	
			if (nList[i].wipes) buffWriter.println("\t\t" + ab + "_node.setWipeOn1or2();");
//			if ((nlist.initialbits&HOLDSTRACE) != 0) buffWriter.println("\t\t" + ab + "_node.setHoldsOutline();");
			if (nList[i].square) buffWriter.println("\t\t" + ab + "_node.setSquare();");
//			if ((nlist.initialbits&ARCSWIPE) != 0) buffWriter.println("\t\t" + ab + "_node.setArcsWipe();");
//			if ((nlist.initialbits&ARCSHRINK) != 0) buffWriter.println("\t\t" + ab + "_node.setArcsShrink();");
//			if ((nlist.initialbits&NODESHRINK) != 0) buffWriter.println("\t\t" + ab + "_node.setCanShrink();");
			if (nList[i].lockable) buffWriter.println("\t\t" + ab + "_node.setLockedPrim();");
			if (nList[i].specialType != 0)
			{
				switch (nList[i].specialType)
				{
					case PrimitiveNode.SERPTRANS:
						buffWriter.println("\t\t" + ab + "_node.setSpecialType(PrimitiveNode.SERPTRANS);");
						buffWriter.println("\t\t" + ab + "_node.setSpecialValues(new double [] {" +
							nList[i].specialValues[0] + ", " + nList[i].specialValues[1] + ", " +
							nList[i].specialValues[2] + ", " + nList[i].specialValues[3] + ", " +
							nList[i].specialValues[4] + ", " + nList[i].specialValues[5] + "});");
						break;
					case PrimitiveNode.POLYGONAL:
						buffWriter.println("\t\t" + ab + "_node.setSpecialType(PrimitiveNode.POLYGONAL);");
						break;
					case PrimitiveNode.MULTICUT:
						buffWriter.println("\t\t" + ab + "_node.setSpecialType(PrimitiveNode.MULTICUT);");
						buffWriter.println("\t\t" + ab + "_node.setSpecialValues(new double [] {" +
							nList[i].specialValues[0] + ", " + nList[i].specialValues[1] + ", " +
							nList[i].specialValues[2] + ", " + nList[i].specialValues[3] + ", " +
							nList[i].specialValues[4] + ", " + nList[i].specialValues[5] + "});");
						break;
				}
			}
		}
	
		// write the pure-layer associations
		buffWriter.println();
		buffWriter.println("\t\t// The pure layer nodes");
		for(int i=0; i<lList.length; i++)
		{
			if ((lList[i].funExtra&Layer.Function.PSEUDO) != 0) continue;
	
			// find the pure layer node
			for(int j=0; j<nList.length; j++)
			{
				if (nList[j].func != PrimitiveNode.Function.NODE) continue;
				Generate.NodeLayerDetails nld = nList[j].nodeLayers[0];
				if (nld.layer == lList[i])
				{
					buffWriter.println("\t\t" + lList[i].name + "_lay.setPureLayerNode(" +
						nList[j].abbrev + "_node);\t\t// " + lList[i].name);
					break;
				}
			}
		}
	
		buffWriter.println("\t};");

//		// write method to reset rules
//		if ((us_tecflags&(HASCONDRC|HASUNCONDRC)) != 0)
//		{
//			CHAR *conword, *unconword;
//			if ((us_tecflags&HASCONDRC) != 0) conword = "conDist"; else conword = "null";
//			if ((us_tecflags&HASUNCONDRC) != 0) unconword = "unConDist"; else unconword = "null";
//			buffWriter.println("\tpublic DRCRules getFactoryDesignRules()\n"));
//			buffWriter.println("\t{\n"));
//			buffWriter.println("\t\treturn MOSRules.makeSimpleRules(this, %s, %s);\n"), conword, unconword);
//			buffWriter.println("\t}\n"));
//		}
	}
	
	/**
	 * Method to remove illegal Java charcters from "string".
	 */
	static String us_teceditconverttojava(String string)
	{
		StringBuffer infstr = new StringBuffer();
		for(int i=0; i<string.length(); i++)
		{
			char chr = string.charAt(i);
			if (i == 0) chr = Character.toLowerCase(chr);
			if (chr == '-') infstr.append('_'); else
				infstr.append(chr);
		}
		return infstr.toString();
	}
	
	/**
	 * Method to convert the multiplication and addition factors in "mul" and
	 * "add" into proper constant names.  The "yaxis" is false for X and 1 for Y
	 */
	static String us_tecededgelabeljava(Technology.TechPoint pt, boolean yaxis)
	{
		double mul, add;
		if (yaxis)
		{
			add = pt.getY().getAdder();
			mul = pt.getY().getMultiplier();
		} else
		{
			add = pt.getX().getAdder();
			mul = pt.getX().getMultiplier();
		}
		StringBuffer infstr = new StringBuffer();
	
		// handle constant distance from center
		if (mul == 0)
		{
			if (yaxis) infstr.append("EdgeV."); else
				infstr.append("EdgeH.");
			if (add == 0)
			{
				infstr.append("makeCenter()");
			} else
			{
				infstr.append("fromCenter(" + TextUtils.formatDouble(add) + ")");
			}
			return infstr.toString();
		}
	
		// handle constant distance from edge
		if (mul == 0.5 || mul == -0.5)
		{
			if (yaxis) infstr.append("EdgeV."); else
				infstr.append("EdgeH.");
			double amt = Math.abs(add);
			if (!yaxis)
			{
				if (mul < 0)
				{
					if (add == 0) infstr.append("makeLeftEdge()"); else
						infstr.append("fromLeft(" + TextUtils.formatDouble(amt) + ")");
				} else
				{
					if (add == 0) infstr.append("makeRightEdge()"); else
						infstr.append("fromRight(" + TextUtils.formatDouble(amt) + ")");
				}
			} else
			{
				if (mul < 0)
				{
					if (add == 0) infstr.append("makeBottomEdge()"); else
						infstr.append("fromBottom(" + TextUtils.formatDouble(amt) + ")");
				} else
				{
					if (add == 0) infstr.append("makeTopEdge()"); else
						infstr.append("fromTop(" + TextUtils.formatDouble(amt) + ")");
				}
			}
			return infstr.toString();
		}
	
		// generate two-value description
		if (!yaxis)
			infstr.append("new EdgeH(" + TextUtils.formatDouble(mul) + ", " + TextUtils.formatDouble(add) + ")"); else
			infstr.append("new EdgeV(" + TextUtils.formatDouble(mul) + ", " + TextUtils.formatDouble(add) + ")");
		return infstr.toString();
	}
	
	/****************************** SUPPORT FOR SOURCE-CODE GENERATION ******************************/

	static void us_tecedpointout(NodeInst ni, Cell cell)
	{
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf == null) return;
		if (!(wf.getContent() instanceof EditWindow)) return;
		EditWindow wnd = (EditWindow)wf.getContent();
		wf.setCellWindow(cell);
		if (ni != null)
		{
			Highlighter highligher = wnd.getHighlighter();
			highligher.clear();
			highligher.addElectricObject(ni, cell);
			highligher.finished();
		}
	}
	
	static String us_tecedsamplename(NodeProto layernp)
	{
		if (layernp == Generic.tech.portNode) return "PORT";
		if (layernp == Generic.tech.cellCenterNode) return "GRAB";
		if (layernp == null) return "HIGHLIGHT";
		return layernp.getName().substring(6);
	}

}
