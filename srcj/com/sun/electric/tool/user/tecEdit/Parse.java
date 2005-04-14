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

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.EvalJavaBsh;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.EdgeH;
import com.sun.electric.technology.EdgeV;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.tecEdit.Generate.ArcInfo;
import com.sun.electric.tool.user.tecEdit.Generate.GeneralInfo;
import com.sun.electric.tool.user.tecEdit.Generate.LayerInfo;
import com.sun.electric.tool.user.tecEdit.Generate.NodeInfo;
import com.sun.electric.tool.user.tecEdit.Generate.NodePortDetails;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.technology.Technology.TechPoint;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
* This class creates technology libraries from technologies.
*/
public class Parse
{

//	typedef struct Ilist
//	{
//		CHAR  *name;
//		CHAR  *constant;
//		INTBIG value;
//	} LIST;

	static class Sample
	{
		NodeInst  node;					/* true node used for sample */
		NodeProto layer;				/* type of node used for sample */
		double    xpos, ypos;			/* center of sample */
		Sample    assoc;				/* associated sample in first example */
		Rule      rule;					/* rule associated with this sample */
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
	
	/* rectangle rules */
	static class Rule
	{
		Technology.TechPoint []     value;					/* data points for rule */
		String        msg;
		int        istext;					/* nonzero if text at end of rule */
		int        rindex;					/* identifier for this rule */
		boolean       used;						/* nonzero if actually used */
		boolean       multicut;					/* nonzero if this is multiple cut */
		double        multixs, multiys;			/* size of multicut */
		double        multiindent, multisep;	/* indent and separation of multicuts */
		Rule nextrule;
	};


	/* port connections */
	static class PCon
	{
		Generate.ArcInfo [] connects;
		boolean []          assoc;
		int                 total;
		int                 pcindex;
		PCon                nextpcon;
	};


	/* the globals that define a technology */
	static int           us_tecflags;
//	static INTBIG           us_teclayer_count;
//	static CHAR           **us_teclayer_iname = 0;
//	static CHAR           **us_teclayer_names = 0;
//	static CHAR           **us_teccif_layers = 0;
//	static CHAR           **us_tecdxf_layers = 0;
//	static CHAR           **us_tecgds_layers = 0;
//	static INTBIG          *us_teclayer_function = 0;
//	static CHAR           **us_teclayer_letters = 0;
//	static DRCRULES        *us_tecdrc_rules = 0;
//	static float           *us_tecspice_res = 0;
//	static float           *us_tecspice_cap = 0;
//	static float           *us_tecspice_ecap = 0;
//	static INTBIG          *us_tec3d_height = 0;
//	static INTBIG          *us_tec3d_thickness = 0;
//	static INTBIG          *us_tecprint_colors = 0;
//	static INTBIG           us_tecarc_count;
//	static INTBIG          *us_tecarc_widoff = 0;
//	static INTBIG           us_tecnode_count;
//	static INTBIG          *us_tecnode_widoff = 0;
//	static INTBIG          *us_tecnode_grab = 0;
//	static INTBIG           us_tecnode_grabcount;
//	static TECH_COLORMAP    us_teccolmap[32];
//	
//	/* these must correspond to the layer functions in "efunction.h" */
//	LIST us_tecarc_functions[] =
//	{
//		{x_("unknown"),             x_("APUNKNOWN"),  APUNKNOWN},
//		{x_("metal-1"),             x_("APMETAL1"),   APMETAL1},
//		{x_("metal-2"),             x_("APMETAL2"),   APMETAL2},
//		{x_("metal-3"),             x_("APMETAL3"),   APMETAL3},
//		{x_("metal-4"),             x_("APMETAL4"),   APMETAL4},
//		{x_("metal-5"),             x_("APMETAL5"),   APMETAL5},
//		{x_("metal-6"),             x_("APMETAL6"),   APMETAL6},
//		{x_("metal-7"),             x_("APMETAL7"),   APMETAL7},
//		{x_("metal-8"),             x_("APMETAL8"),   APMETAL8},
//		{x_("metal-9"),             x_("APMETAL9"),   APMETAL9},
//		{x_("metal-10"),            x_("APMETAL10"),  APMETAL10},
//		{x_("metal-11"),            x_("APMETAL11"),  APMETAL11},
//		{x_("metal-12"),            x_("APMETAL12"),  APMETAL12},
//		{x_("polysilicon-1"),       x_("APPOLY1"),    APPOLY1},
//		{x_("polysilicon-2"),       x_("APPOLY2"),    APPOLY2},
//		{x_("polysilicon-3"),       x_("APPOLY3"),    APPOLY3},
//		{x_("diffusion"),           x_("APDIFF"),     APDIFF},
//		{x_("p-Diffusion"),         x_("APDIFFP"),    APDIFFP},
//		{x_("n-Diffusion"),         x_("APDIFFN"),    APDIFFN},
//		{x_("substrate-Diffusion"), x_("APDIFFS"),    APDIFFS},
//		{x_("well-Diffusion"),      x_("APDIFFW"),    APDIFFW},
//		{x_("bus"),                 x_("APBUS"),      APBUS},
//		{x_("unrouted"),            x_("APUNROUTED"), APUNROUTED},
//		{x_("nonelectrical"),       x_("APNONELEC"),  APNONELEC},
//		{NULL, NULL, 0}
//	};
	
	static PCon  us_tecedfirstpcon = null;	/* list of port connections */
	static Rule  us_tecedfirstrule = null;	/* list of rules */
	
//	/* working memory for "us_tecedmakeprim()" */
//	static INTBIG *us_tecedmakepx, *us_tecedmakepy, *us_tecedmakefactor,
//		*us_tecedmakeleftdist, *us_tecedmakerightdist, *us_tecedmakebotdist,
//		*us_tecedmaketopdist, *us_tecedmakecentxdist, *us_tecedmakecentydist, *us_tecedmakeratiox,
//		*us_tecedmakeratioy, *us_tecedmakecx, *us_tecedmakecy;
//	static INTBIG us_tecedmakearrlen = 0;
//	
//	/* working memory for "us_teceditgetdependents()" */
//	static LIBRARY **us_teceddepliblist;
//	static INTBIG    us_teceddepliblistsize = 0;
	
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
		Generate.GeneralInfo gi = Generate.GeneralInfo.us_teceditgetlayerinfo(np);

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
//		setFactoryTransparentLayers(new Color []
//		{
//			new Color(  0,  0,255), // Metal
//			new Color(223,  0,  0), // Polysilicon
//			new Color(  0,255,  0), // Diffusion
//			new Color(255,190,  6), // P+
//			new Color(170,140, 30)  // P-Well
//		});
		tech.setTheScale(gi.scale);
		tech.setTechDesc(gi.description);

		// create the layers
		for(int i=0; i<lList.length; i++)
		{
			Layer lay = Layer.newInstance(tech, lList[i].name, lList[i].desc);
			lay.setFunction(lList[i].fun, lList[i].funExtra);
			lay.setCIFLayer(lList[i].cif);
			lay.setGDSLayer(lList[i].gds);
			lay.setDXFLayer(lList[i].dxf);
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
			aList[i].generated = newArc;
		}

		// create the nodes
		for(int i=0; i<nList.length; i++)
		{
			Generate.NodeLayerDetails [] nd = nList[i].nodeLayers;
			Technology.NodeLayer [] nodeLayers = new Technology.NodeLayer[nd.length];
			for(int j=0; j<nd.length; j++)
			{
				LayerInfo li = nd[j].layer;
				Layer lay = li.generated;
				TechPoint [] points = nd[j].rule.value;
				nodeLayers[j] = new Technology.NodeLayer(lay, nd[j].portIndex, nd[j].style, Technology.NodeLayer.BOX, points);
			}
			PrimitiveNode prim = PrimitiveNode.newInstance(nList[i].name, tech, nList[i].xSize, nList[i].ySize, nList[i].so, nodeLayers);
			prim.setFunction(nList[i].func);
			if (nList[i].wipes) prim.setArcsWipe();
			if (nList[i].lockable) prim.setLockedPrim();
			if (nList[i].square) prim.setSquare();
	
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
				portList[j] = PrimitivePort.newInstance(tech, prim, cons, portDetail.name, portDetail.angle,portDetail.range,
					portDetail.netIndex, PortCharacteristic.UNKNOWN,
					portDetail.rule.value[0].getX(), portDetail.rule.value[0].getY(),
					portDetail.rule.value[1].getX(), portDetail.rule.value[1].getY());
			}
			prim.addPrimitivePorts(portList);
		}

//System.out.println("Technology "+tech.getTechName()+" has "+tech.getNumLayers()+" layers:");
//for(Iterator it = tech.getLayers(); it.hasNext(); )
//{
//	Layer layer = (Layer)it.next();
//	System.out.println("   Layer "+layer.getName());
//}
//System.out.println("Technology "+tech.getTechName()+" has "+tech.getNumArcs()+" arcs:");
//for(Iterator it = tech.getArcs(); it.hasNext(); )
//{
//	ArcProto ap = (ArcProto)it.next();
//	System.out.println("   Arc "+ap.getName());
//}
//System.out.println("Technology "+tech.getTechName()+" has "+tech.getNumNodes()+" nodes:");
//for(Iterator it = tech.getNodes(); it.hasNext(); )
//{
//	PrimitiveNode n = (PrimitiveNode)it.next();
//	System.out.println("   Node "+n.describe());
//}

//		// copy any miscellaneous variables (should use dependent libraries facility)
//		Variable var = lib.getVar(Generate.VARIABLELIST_KEY);
//		if (var != NOVARIABLE)
//		{
//			j = getlength(var);
//			varnames = (CHAR **)var.addr;
//			for(i=0; i<j; i++)
//			{
//				ovar = getval((INTBIG)lib, VLIBRARY, -1, varnames[i]);
//				if (ovar == NOVARIABLE) continue;
//				(void)setval((INTBIG)tech, VTECHNOLOGY, varnames[i], ovar.addr, ovar.type);
//			}
//		}
	
		// check technology for consistency
		us_tecedcheck(lList, aList, nList);
	
		if (alsoJava)
		{
			// print the technology as Java code
			String fileName = OpenFile.chooseOutputFile(FileType.JAVA, "File for Technology's Java Code", "X.java");
			if (fileName != null)
			{
				FileOutputStream fileOutputStream = null;
				try {
				    PrintStream buffWriter = new PrintStream(new FileOutputStream(fileName));
			
					// write the layers, arcs, and nodes
					us_teceditdumpjavalayers(buffWriter, lList);
					us_teceditdumpjavaarcs(buffWriter, aList);
					us_teceditdumpjavanodes(buffWriter, nList);
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
	
//		// get the color map
//		var = NOVARIABLE;
//		for(i=dependentlibcount-1; i>=0; i--)
//		{
//			var = dependentlibs[i].getVar(Generate.COLORMAP_KEY);
//			if (var != null) break;
//		}
//		if (var != NOVARIABLE)
//		{
//			us_tecflags |= HASCOLORMAP;
//			drcptr = (INTBIG *)var.addr;
//			for(i=0; i<32; i++)
//			{
//				us_teccolmap[i].red = (INTSML)drcptr[(i<<2)*3];
//				us_teccolmap[i].green = (INTSML)drcptr[(i<<2)*3+1];
//				us_teccolmap[i].blue = (INTSML)drcptr[(i<<2)*3+2];
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
//		if ((us_tecflags&HASCOLORMAP) != 0)
//			(void)setval((INTBIG)tech, VTECHNOLOGY, x_("USER_color_map"), (INTBIG)us_teccolmap,
//				VCHAR|VDONTSAVE|VISARRAY|((sizeof us_teccolmap)<<VLENGTHSH));
//	
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
//		REGISTER NODEPROTO *np;
//		NODEPROTO **sequence;
//		REGISTER NODEINST *ni;
//		REGISTER VARIABLE *var;
//		REGISTER CHAR *str, *portname;
//		REGISTER INTBIG *list, save1, nfunction, x1pos, x2pos, y1pos, y2pos, net, lambda;
//		REGISTER INTBIG i, j, k, l, m, pass, nodeindex, sty, difindex, polindex,
//			serpdifind, opt, nsindex, err, portchecked;
//		INTBIG pol1port, pol2port, dif1port, dif2port;
//		INTBIG serprule[8];
//		REGISTER EXAMPLE *nelist;
//		REGISTER SAMPLE *ns, *ons, *diflayer, *pollayer;
//		REGISTER PCON *pc;
//		REGISTER RULE *r;
//		REGISTER TECH_NODES *tlist;
	
		// no rectangle rules
		us_tecedfirstrule = null;

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
						connections[j] = null;
						for(int k=0; k<aList.length; k++)
						{
							if (aList[k].name.equalsIgnoreCase(arcCells[j].getName().substring(4)))
							{
								connections[j] = aList[k];
								break;
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
//				if (ns.node.firstportarcinst != null)
//				{
//					int j = 0;
//					for(Sample ons = nelist.firstsample; ons != ns; ons = ons.nextsample)
//					{
//						if (ons.layer != Generic.tech.portNode) continue;
//						if (ons.node.firstportarcinst != null)
//						{
//							if (ns.node.firstportarcinst.conarcinst.network ==
//								ons.node.firstportarcinst.conarcinst.network)
//							{
//								nin.nodePortDetails[i].netIndex = j;
//								break;
//							}
//						}
//						j++;
//					}
//				}
	
				// port area rule
				nin.nodePortDetails[i].rule = ns.rule;
				i++;
			}
	
			// on FET transistors, make sure ports 0 and 2 are poly
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
//					if (pol2port == 0) us_tecedswapports(&pol1port, &pol2port, tlist); else
//					if (dif1port == 0) us_tecedswapports(&pol1port, &dif1port, tlist); else
//					if (dif2port == 0) us_tecedswapports(&pol1port, &dif2port, tlist);
				}
				if (pol2port != 2)
				{
//					if (dif1port == 2) us_tecedswapports(&pol2port, &dif1port, tlist); else
//					if (dif2port == 2) us_tecedswapports(&pol2port, &dif2port, tlist);
				}
//				if (dif1port != 1) us_tecedswapports(&dif1port, &dif2port, tlist);
//	
//				// also make sure that dif1port is positive and dif2port is negative
//				x1pos = (tlist.portlist[dif1port].lowxmul*tlist.xsize +
//					tlist.portlist[dif1port].lowxsum +
//						tlist.portlist[dif1port].highxmul*tlist.xsize +
//							tlist.portlist[dif1port].highxsum) / 2;
//				x2pos = (tlist.portlist[dif2port].lowxmul*tlist.xsize +
//					tlist.portlist[dif2port].lowxsum +
//						tlist.portlist[dif2port].highxmul*tlist.xsize +
//							tlist.portlist[dif2port].highxsum) / 2;
//				y1pos = (tlist.portlist[dif1port].lowymul*tlist.ysize +
//					tlist.portlist[dif1port].lowysum +
//						tlist.portlist[dif1port].highymul*tlist.ysize +
//							tlist.portlist[dif1port].highysum) / 2;
//				y2pos = (tlist.portlist[dif2port].lowymul*tlist.ysize +
//					tlist.portlist[dif2port].lowysum +
//						tlist.portlist[dif2port].highymul*tlist.ysize +
//							tlist.portlist[dif2port].highysum) / 2;
//				if (abs(x1pos-x2pos) > abs(y1pos-y2pos))
//				{
//					if (x1pos < x2pos)
//					{
//						us_tecedswapports(&dif1port, &dif2port, tlist);
//						j = dif1port;   dif1port = dif2port;   dif2port = j;
//					}
//				} else
//				{
//					if (y1pos < y2pos)
//					{
//						us_tecedswapports(&dif1port, &dif2port, tlist);
//						j = dif1port;   dif1port = dif2port;   dif2port = j;
//					}
//				}
//	
//				// also make sure that pol1port is negative and pol2port is positive
//				x1pos = (tlist.portlist[pol1port].lowxmul*tlist.xsize +
//					tlist.portlist[pol1port].lowxsum +
//						tlist.portlist[pol1port].highxmul*tlist.xsize +
//							tlist.portlist[pol1port].highxsum) / 2;
//				x2pos = (tlist.portlist[pol2port].lowxmul*tlist.xsize +
//					tlist.portlist[pol2port].lowxsum +
//						tlist.portlist[pol2port].highxmul*tlist.xsize +
//							tlist.portlist[pol2port].highxsum) / 2;
//				y1pos = (tlist.portlist[pol1port].lowymul*tlist.ysize +
//					tlist.portlist[pol1port].lowysum +
//						tlist.portlist[pol1port].highymul*tlist.ysize +
//							tlist.portlist[pol1port].highysum) / 2;
//				y2pos = (tlist.portlist[pol2port].lowymul*tlist.ysize +
//					tlist.portlist[pol2port].lowysum +
//						tlist.portlist[pol2port].highymul*tlist.ysize +
//							tlist.portlist[pol2port].highysum) / 2;
//				if (abs(x1pos-x2pos) > abs(y1pos-y2pos))
//				{
//					if (x1pos > x2pos)
//					{
//						us_tecedswapports(&pol1port, &pol2port, tlist);
//						j = pol1port;   pol1port = pol2port;   pol2port = j;
//					}
//				} else
//				{
//					if (y1pos > y2pos)
//					{
//						us_tecedswapports(&pol1port, &pol2port, tlist);
//						j = pol1port;   pol1port = pol2port;   pol2port = j;
//					}
//				}
			}
	
			// count the number of layers on the node
			int layercount = 0;
			for(Sample ns = nelist.firstsample; ns != null; ns = ns.nextsample)
			{
				if (ns.rule != null && ns.layer != Generic.tech.portNode &&
					ns.layer != Generic.tech.cellCenterNode && ns.layer != null)
						layercount++;
			}
	
//			// allocate space for the layers
//			if (tlist.special != SERPTRANS)
//			{
//				tlist.layerlist = (TECH_POLYGON *)emalloc((tlist.layercount *
//					(sizeof (TECH_POLYGON))), tech.cluster);
//				if (tlist.layerlist == 0) return(TRUE);
//			} else
//			{
//				tlist.gra = (TECH_SERPENT *)emalloc(((sizeof (TECH_SERPENT)) * tlist.layercount),
//					tech.cluster);
//				if (tlist.gra == 0) return(TRUE);
//				tlist.ele = (TECH_SERPENT *)emalloc(((sizeof (TECH_SERPENT)) * (tlist.layercount+1)),
//					tech.cluster);
//				if (tlist.ele == 0) return(TRUE);
//			}
//	
//			// fill the layer structures (3 times: transparent, opaque, multicut)
//			i = 0;
//			pollayer = diflayer = NOSAMPLE;
//			for(k=0; k<3; k++)
//				for(nsindex=0, ns = nelist.firstsample; ns != NOSAMPLE; nsindex++, ns = ns.nextsample)
//			{
//				r = ns.rule;
//				if (r == NORULE || ns.layer == Generic.tech.portNode ||
//					ns.layer == Generic.tech.cellCenterNode || ns.layer == NONODEPROTO) continue;
//	
//				// add cut layers last (only when k=2)
//				if (k == 2)
//				{
//					if (!r.multicut) continue;
//					if (tlist.special != 0)
//					{
//						us_tecedpointout(ns.node, ns.node.parent);
//						System.out.println(_("%s is too complex (multiple cuts AND serpentine)"),
//							describenodeproto(np));
//						us_tecedfreeexamples(nelist);
//						return(TRUE);
//					}
//					tlist.special = MULTICUT;
//					tlist.f1 = (INTSML)(r.multixs*WHOLE/lambda);
//					tlist.f2 = (INTSML)(r.multiys*WHOLE/lambda);
//					tlist.f3 = (INTSML)(r.multiindent*WHOLE/lambda);
//					tlist.f4 = (INTSML)(r.multisep*WHOLE/lambda);
//				} else
//				{
//					if (r.multicut) continue;
//				}
//	
//				// layer number
//				for(j=0; j<tech.layercount; j++)
//					if (namesame(&ns.layer.protoname[6], us_teclayer_names[j]) == 0) break;
//				if (j >= tech.layercount)
//				{
//					System.out.println(_("Cannot find layer %s in %s"), describenodeproto(ns.layer),
//						describenodeproto(np));
//					return(TRUE);
//				}
//	
//				// only add transparent layers when k=0
//				if (k == 0)
//				{
//					if (tech.layers[j].bits == LAYERO) continue;
//				} else if (k == 1)
//				{
//					if (tech.layers[j].bits != LAYERO) continue;
//				}
//	
//				// layer style
//				sty = -1;
//				if (ns.node.proto == art_filledboxprim)             sty = FILLEDRECT; else
//				if (ns.node.proto == art_boxprim)                   sty = CLOSEDRECT; else
//				if (ns.node.proto == art_crossedboxprim)            sty = CROSSED; else
//				if (ns.node.proto == Artwork.tech.filledPolygonNode)         sty = FILLED; else
//				if (ns.node.proto == Artwork.tech.closedPolygonNode)         sty = CLOSED; else
//				if (ns.node.proto == Artwork.tech.openedPolygonNode)         sty = OPENED; else
//				if (ns.node.proto == Artwork.tech.openedDottedPolygonNode)   sty = OPENEDT1; else
//				if (ns.node.proto == Artwork.tech.openedDashedPolygonNode)   sty = OPENEDT2; else
//				if (ns.node.proto == Artwork.tech.openedThickerPolygonNode)  sty = OPENEDT3; else
//				if (ns.node.proto == Artwork.tech.filledCircleNode)          sty = DISC; else
//				if (ns.node.proto == Artwork.tech.circleNode)
//				{
//					var = getvalkey((INTBIG)ns.node, VNODEINST, VINTEGER, art_degreeskey);
//					if (var != NOVARIABLE) sty = CIRCLEARC; else sty = CIRCLE;
//				} else if (ns.node.proto == Artwork.tech.thickCircleNode)
//				{
//					var = getvalkey((INTBIG)ns.node, VNODEINST, VINTEGER, art_degreeskey);
//					if (var != NOVARIABLE) sty = THICKCIRCLEARC; else sty = THICKCIRCLE;
//				} else if (ns.node.proto == gen_invispinprim)
//				{
//					var = getvalkey((INTBIG)ns.node, VNODEINST, VSTRING|VISARRAY, art_messagekey);
//					if (var != NOVARIABLE)
//					{
//						switch (TDGETPOS(var.textdescript))
//						{
//							case VTPOSBOXED:     sty = TEXTBOX;       break;
//							case VTPOSCENT:      sty = TEXTCENT;      break;
//							case VTPOSUP:        sty = TEXTBOT;       break;
//							case VTPOSDOWN:      sty = TEXTTOP;       break;
//							case VTPOSLEFT:      sty = TEXTRIGHT;     break;
//							case VTPOSRIGHT:     sty = TEXTLEFT;      break;
//							case VTPOSUPLEFT:    sty = TEXTBOTRIGHT;  break;
//							case VTPOSUPRIGHT:   sty = TEXTBOTLEFT;   break;
//							case VTPOSDOWNLEFT:  sty = TEXTTOPRIGHT;  break;
//							case VTPOSDOWNRIGHT: sty = TEXTTOPLEFT;   break;
//						}
//					}
//				}
//				if (sty == -1)
//					System.out.println(_("Cannot determine style to use for %s node in %s"),
//						describenodeproto(ns.node.proto), describenodeproto(np));
//	
//				// load the layer structure(s)
//				if (tlist.special == SERPTRANS)
//				{
//					// determine port numbers for serpentine transistors
//					if (layerismetal(us_teclayer_function[j]))
//					{
//						tlist.gra[i].basics.portnum = 0;
//					} else if (layerispoly(us_teclayer_function[j]))
//					{
//						pollayer = ns;
//						if (pol1port >= 0)
//							tlist.gra[i].basics.portnum = (INTSML)pol1port; else
//								tlist.gra[i].basics.portnum = 0;
//						polindex = i;
//					} else if ((us_teclayer_function[j]&LFTYPE) == LFDIFF)
//					{
//						diflayer = ns;
//						difindex = i;
//						tlist.gra[i].basics.portnum = 0;
//					} else
//					{
//						tlist.gra[i].basics.portnum = -1;
//					}
//	
//					tlist.gra[i].basics.layernum = (INTSML)j;
//					tlist.gra[i].basics.count = (INTSML)(r.count/4);
//					if (sty == CROSSED || sty == FILLEDRECT || sty == FILLED || sty == CLOSEDRECT ||
//						sty == CLOSED)
//					{
//						if (tlist.gra[i].basics.count == 4)
//						{
//							System.out.println(_("Ignoring Minimum-Size setting on layer %s in serpentine transistor %s"),
//								&ns.layer.protoname[6], &np.protoname[5]);
//							tlist.gra[i].basics.count = 2;
//						}
//					}
//					tlist.gra[i].basics.style = (INTSML)sty;
//					if (tlist.gra[i].basics.count == 2 && (sty == CROSSED ||
//						sty == FILLEDRECT || sty == FILLED || sty == CLOSEDRECT || sty == CLOSED))
//					{
//						tlist.gra[i].basics.representation = BOX;
//						tlist.gra[i].basics.count = 4;
//					} else tlist.gra[i].basics.representation = POINTS;
//					tlist.gra[i].basics.points = r.value;
//					tlist.gra[i].lwidth = (INTSML)nsindex;
//					tlist.gra[i].rwidth = 0;
//					tlist.gra[i].extendt = 0;
//					tlist.gra[i].extendb = 0;
//				} else
//				{
//					tlist.layerlist[i].portnum = (INTSML)us_tecedfindport(tlist, nelist,
//						ns.node.lowx, ns.node.highx, ns.node.lowy, ns.node.highy,
//							lambdaofnode(ns.node));
//					tlist.layerlist[i].layernum = (INTSML)j;
//					tlist.layerlist[i].count = (INTSML)(r.count/4);
//					tlist.layerlist[i].style = (INTSML)sty;
//					tlist.layerlist[i].representation = POINTS;
//					if (sty == CROSSED || sty == FILLEDRECT || sty == FILLED || sty == CLOSEDRECT ||
//						sty == CLOSED)
//					{
//						if (r.count == 8)
//						{
//							tlist.layerlist[i].representation = BOX;
//							tlist.layerlist[i].count = 4;
//						} else if (r.count == 16)
//						{
//							tlist.layerlist[i].representation = MINBOX;
//							tlist.layerlist[i].count = 4;
//						}
//					}
//					tlist.layerlist[i].points = r.value;
//				}
//	
//				// mark this rectangle rule "used"
//				r.used = TRUE;
//				i++;
//			}
//	
//			// finish up serpentine transistors
//			if (tlist.special == SERPTRANS)
//			{
//				if (diflayer == NOSAMPLE || pollayer == NOSAMPLE || dif1port < 0)
//				{
//					us_tecedpointout(NONODEINST, np);
//					System.out.println(_("No diffusion and polysilicon layers in transistor %s"),
//						describenodeproto(np));
//					us_tecedfreeexamples(nelist);
//					return(TRUE);
//				}
//	
//				// compute port extension factors
//				tlist.f1 = tlist.layercount+1;
//				if (tlist.portlist[dif1port].lowxsum >
//					tlist.portlist[dif1port].lowysum)
//				{
//					// vertical diffusion layer: determine polysilicon width
//					tlist.f4 = (INTSML)((muldiv(tlist.ysize, tlist.gra[polindex].basics.points[6], WHOLE) +
//						tlist.gra[polindex].basics.points[7]) -
//							(muldiv(tlist.ysize, tlist.gra[polindex].basics.points[2], WHOLE) +
//								tlist.gra[polindex].basics.points[3]));
//	
//					// determine diffusion port rule
//					tlist.f2 = (INTSML)((muldiv(tlist.xsize, tlist.portlist[dif1port].lowxmul, WHOLE) +
//						tlist.portlist[dif1port].lowxsum) -
//							(muldiv(tlist.xsize, tlist.gra[difindex].basics.points[0], WHOLE) +
//								tlist.gra[difindex].basics.points[1]));
//					tlist.f3 = (INTSML)((muldiv(tlist.ysize, tlist.portlist[dif1port].lowymul, WHOLE) +
//						tlist.portlist[dif1port].lowysum) -
//							(muldiv(tlist.ysize, tlist.gra[polindex].basics.points[6], WHOLE) +
//								tlist.gra[polindex].basics.points[7]));
//	
//					// determine polysilicon port rule
//					tlist.f5 = (INTSML)((muldiv(tlist.ysize, tlist.portlist[pol1port].lowymul, WHOLE) +
//						tlist.portlist[pol1port].lowysum) -
//							(muldiv(tlist.ysize, tlist.gra[polindex].basics.points[2], WHOLE) +
//								tlist.gra[polindex].basics.points[3]));
//					tlist.f6 = (INTSML)((muldiv(tlist.xsize, tlist.gra[difindex].basics.points[0], WHOLE) +
//						tlist.gra[difindex].basics.points[1]) -
//							(muldiv(tlist.xsize, tlist.portlist[pol1port].highxmul, WHOLE) +
//								tlist.portlist[pol1port].highxsum));
//				} else
//				{
//					// horizontal diffusion layer: determine polysilicon width
//					tlist.f4 = (INTSML)((muldiv(tlist.xsize, tlist.gra[polindex].basics.points[4], WHOLE) +
//						tlist.gra[polindex].basics.points[5]) -
//							(muldiv(tlist.xsize, tlist.gra[polindex].basics.points[0], WHOLE) +
//								tlist.gra[polindex].basics.points[1]));
//	
//					// determine diffusion port rule
//					tlist.f2 = (INTSML)((muldiv(tlist.ysize, tlist.portlist[dif1port].lowymul, WHOLE) +
//						tlist.portlist[dif1port].lowysum) -
//							(muldiv(tlist.ysize, tlist.gra[difindex].basics.points[2], WHOLE) +
//								tlist.gra[difindex].basics.points[3]));
//					tlist.f3 = (INTSML)((muldiv(tlist.xsize, tlist.gra[polindex].basics.points[0], WHOLE) +
//						tlist.gra[polindex].basics.points[1]) -
//							(muldiv(tlist.xsize, tlist.portlist[dif1port].highxmul, WHOLE) +
//								tlist.portlist[dif1port].highxsum));
//	
//					// determine polysilicon port rule
//					tlist.f5 = (INTSML)((muldiv(tlist.xsize, tlist.portlist[pol1port].lowxmul, WHOLE) +
//						tlist.portlist[pol1port].lowxsum) -
//							(muldiv(tlist.xsize, tlist.gra[polindex].basics.points[0], WHOLE) +
//								tlist.gra[polindex].basics.points[1]));
//					tlist.f6 = (INTSML)((muldiv(tlist.ysize, tlist.gra[difindex].basics.points[2], WHOLE) +
//						tlist.gra[difindex].basics.points[3]) -
//							(muldiv(tlist.ysize, tlist.portlist[pol1port].highymul, WHOLE) +
//								tlist.portlist[pol1port].highysum));
//				}
//	
//				// find width and extension from comparison to poly layer
//				for(i=0; i<tlist.layercount; i++)
//				{
//					for(nsindex=0, ns = nelist.firstsample; ns != NOSAMPLE;
//						nsindex++, ns = ns.nextsample)
//							if (tlist.gra[i].lwidth == nsindex) break;
//					if (ns == NOSAMPLE)
//					{
//						us_tecedpointout(NONODEINST, np);
//						System.out.println(_("Internal error in serpentine %s"), describenodeproto(np));
//						us_tecedfreeexamples(nelist);
//						continue;
//					}
//	
//					if (pollayer.node.highx-pollayer.node.lowx >
//						pollayer.node.highy-pollayer.node.lowy)
//					{
//						// horizontal layer
//						tlist.gra[i].lwidth = (INTSML)((ns.node.highy - (ns.parent.ly + ns.parent.hy)/2) *
//							WHOLE/lambda);
//						tlist.gra[i].rwidth = (INTSML)(((ns.parent.ly + ns.parent.hy)/2 - ns.node.lowy) *
//							WHOLE/lambda);
//						tlist.gra[i].extendt = (INTSML)((diflayer.node.lowx - ns.node.lowx) * WHOLE /
//							lambda);
//					} else
//					{
//						// vertical layer
//						tlist.gra[i].lwidth = (INTSML)((ns.node.highx - (ns.parent.lx + ns.parent.hx)/2) *
//							WHOLE/lambda);
//						tlist.gra[i].rwidth = (INTSML)(((ns.parent.lx + ns.parent.hx)/2 - ns.node.lowx) *
//							WHOLE/lambda);
//						tlist.gra[i].extendt = (INTSML)((diflayer.node.lowy - ns.node.lowy) * WHOLE /
//							lambda);
//					}
//					tlist.gra[i].extendb = tlist.gra[i].extendt;
//				}
//	
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
//							us_tecedpointout(NONODEINST, np);
//							System.out.println(_("Nonrectangular diffusion in Serpentine %s"),
//								describenodeproto(np));
//							us_tecedfreeexamples(nelist);
//							return(TRUE);
//						}
//						for(l=0; l<r.count; l++) serprule[l] = r.value[l];
//						if (serprule[0] != -H0 || serprule[2] != -H0 ||
//							serprule[4] != H0 || serprule[6] != H0)
//						{
//							us_tecedpointout(NONODEINST, np);
//							System.out.println(_("Unusual diffusion in Serpentine %s"), describenodeproto(np));
//							us_tecedfreeexamples(nelist);
//							return(TRUE);
//						}
//						if (tlist.xsize - serprule[1] + serprule[5] <
//							tlist.ysize - serprule[3] + serprule[7]) serpdifind = 2; else
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
//								r = us_tecedaddrule(serprule, 8, FALSE, (CHAR *)0);
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
//								r = us_tecedaddrule(serprule, 8, FALSE, (CHAR *)0);
//								if (r == NORULE) return(TRUE);
//								r.used = TRUE;
//								tlist.ele[i].basics.points = r.value;
//								serprule[serpdifind+4] = H0;
//								serprule[serpdifind+5] = save1;
//								break;
//						}
//						i++;
//					}
//				}
//			}
	
			// extract width offset information from highlight box
			double lx = 0, hx = 0, ly = 0, hy = 0;
			boolean found = false;
			for(Sample ns = nelist.firstsample; ns != null; ns = ns.nextsample)
			{
				if (ns.layer != null) continue;
				found = true;
				Rule r = ns.rule;
				if (r != null)
				{
					boolean err = false;
					if (r.value[0].getX().getMultiplier() == -0.5)		// left edge offset
					{
						lx = r.value[0].getX().getAdder();
					} else if (r.value[0].getX().getMultiplier() == 0.5)
					{
						lx = nin.xSize + r.value[0].getX().getAdder();
					} else err = true;
					if (r.value[0].getY().getMultiplier() == -0.5)		// bottom edge offset
					{
						ly = r.value[0].getY().getAdder();
					} else if (r.value[0].getY().getMultiplier() == 0.5)
					{
						ly = nin.ySize + r.value[0].getY().getAdder();;
					} else err = true;
					if (r.value[1].getX().getMultiplier() == 0.5)		// right edge offset
					{
						hx = -r.value[1].getX().getAdder();
					} else if (r.value[1].getX().getMultiplier() == -0.5)
					{
						hx = nin.xSize - r.value[1].getX().getAdder();
					} else err = true;
					if (r.value[1].getY().getMultiplier() == 0.5)		// top edge offset
					{
						hy = -r.value[1].getY().getAdder();
					} else if (r.value[1].getY().getMultiplier() == -0.5)
					{
						hy = nin.ySize - r.value[1].getY().getAdder();
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

//	/**
//	 * Method to swap entries "p1" and "p2" of the port list in "tlist"
//	 */
//	void us_tecedswapports(INTBIG *p1, INTBIG *p2, TECH_NODES *tlist)
//	{
//		REGISTER INTBIG temp, *templ;
//		REGISTER CHAR *tempc;
//	
//		templ = tlist.portlist[*p1].portarcs;
//		tlist.portlist[*p1].portarcs = tlist.portlist[*p2].portarcs;
//		tlist.portlist[*p2].portarcs = templ;
//	
//		tempc = tlist.portlist[*p1].protoname;
//		tlist.portlist[*p1].protoname = tlist.portlist[*p2].protoname;
//		tlist.portlist[*p2].protoname = tempc;
//	
//		temp = tlist.portlist[*p1].initialbits;
//		tlist.portlist[*p1].initialbits = tlist.portlist[*p2].initialbits;
//		tlist.portlist[*p2].initialbits = temp;
//	
//		temp = tlist.portlist[*p1].lowxmul;
//		tlist.portlist[*p1].lowxmul = tlist.portlist[*p2].lowxmul;
//		tlist.portlist[*p2].lowxmul = (INTSML)temp;
//		temp = tlist.portlist[*p1].lowxsum;
//		tlist.portlist[*p1].lowxsum = tlist.portlist[*p2].lowxsum;
//		tlist.portlist[*p2].lowxsum = (INTSML)temp;
//	
//		temp = tlist.portlist[*p1].lowymul;
//		tlist.portlist[*p1].lowymul = tlist.portlist[*p2].lowymul;
//		tlist.portlist[*p2].lowymul = (INTSML)temp;
//		temp = tlist.portlist[*p1].lowysum;
//		tlist.portlist[*p1].lowysum = tlist.portlist[*p2].lowysum;
//		tlist.portlist[*p2].lowysum = (INTSML)temp;
//	
//		temp = tlist.portlist[*p1].highxmul;
//		tlist.portlist[*p1].highxmul = tlist.portlist[*p2].highxmul;
//		tlist.portlist[*p2].highxmul = (INTSML)temp;
//		temp = tlist.portlist[*p1].highxsum;
//		tlist.portlist[*p1].highxsum = tlist.portlist[*p2].highxsum;
//		tlist.portlist[*p2].highxsum = (INTSML)temp;
//	
//		temp = tlist.portlist[*p1].highymul;
//		tlist.portlist[*p1].highymul = tlist.portlist[*p2].highymul;
//		tlist.portlist[*p2].highymul = (INTSML)temp;
//		temp = tlist.portlist[*p1].highysum;
//		tlist.portlist[*p1].highysum = tlist.portlist[*p2].highysum;
//		tlist.portlist[*p2].highysum = (INTSML)temp;
//	
//		// finally, swap the actual identifiers
//		temp = *p1;   *p1 = *p2;   *p2 = temp;
//	}

//	/*
//	 * routine to find the closest port to the layer describe by "lx<=X<=hx" and
//	 * "ly<+Y<=hy" in the list "nelist".  The ports are listed in "tlist".  The algorithm
//	 * is to find a port that overlaps this layer.  If there is only one, or if all of
//	 * them electrically connect, use that.  If there are no such ports, or multiple
//	 * unconnected ports, presume that the layer is not related to any port.
//	 */
//	INTBIG us_tecedfindport(TECH_NODES *tlist, EXAMPLE *nelist, INTBIG lx, INTBIG hx, INTBIG ly,
//		INTBIG hy, INTBIG lambda)
//	{
//		REGISTER INTBIG bestport, l, oldnet, newnet;
//		INTBIG portlx, porthx, portly, porthy;
//		REGISTER INTBIG swap;
//	
//		bestport = -1;
//		for(l=0; l<tlist.portcount; l++)
//		{
//			subrange(nelist.lx, nelist.hx, tlist.portlist[l].lowxmul,
//				tlist.portlist[l].lowxsum, tlist.portlist[l].highxmul,
//					tlist.portlist[l].highxsum, &portlx, &porthx, lambda);
//			if (portlx > porthx)
//			{
//				swap = portlx;   portlx = porthx;   porthx = swap;
//			}
//			subrange(nelist.ly, nelist.hy, tlist.portlist[l].lowymul,
//				tlist.portlist[l].lowysum, tlist.portlist[l].highymul,
//					tlist.portlist[l].highysum, &portly, &porthy, lambda);
//			if (portlx > porthx)
//			{
//				swap = portly;   portly = porthy;   porthy = swap;
//			}
//	
//			// ignore the port if there is no intersection
//			if (lx > porthx || hx < portlx || ly > porthy || hy < portly) continue;
//	
//			// if there is no previous overlapping port, use this
//			if (bestport == -1)
//			{
//				bestport = l;
//				continue;
//			}
//	
//			// if these two ports connect, all is well
//			newnet = (tlist.portlist[l].initialbits & PORTNET) >> PORTNETSH;
//			oldnet = (tlist.portlist[bestport].initialbits & PORTNET) >> PORTNETSH;
//			if (newnet == oldnet) continue;
//	
//			// two unconnected ports intersect layer: make it free
//			return(-1);
//		}
//		return(bestport);
//	}
//	
//	/*
//	 * Routine to free the examples created by "us_tecedgetexamples()".
//	 */
//	void us_tecedfreeexamples(EXAMPLE *nelist)
//	{
//		REGISTER EXAMPLE *ne;
//		REGISTER SAMPLE *ns;
//	
//		while (nelist != NOEXAMPLE)
//		{
//			ne = nelist;
//			nelist = nelist.nextexample;
//			while (ne.firstsample != NOSAMPLE)
//			{
//				ns = ne.firstsample;
//				ne.firstsample = ne.firstsample.nextsample;
//				efree((CHAR *)ns);
//			}
//			efree((CHAR *)ne);
//		}
//	}
	
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
					ns.rule = null;
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
			Rectangle2D nodeBounds = ns.node.getBounds();
			AffineTransform trans = ns.node.rotateOut();

//			// if a multicut separation was given and this is a cut, add the rule
//			if (tlist.f4 > 0 && ns.layer != null && ns.layer != Generic.tech.portNode)
//			{
//				for(int i=0; i<lis.length; i++)
//				{
//					if (ns.layer.getName().substring(6).equalsIgnoreCase(lis[i].name))
//					{
//						if (layeriscontact(us_teclayer_function[i]))
//						{
//							hs = us_tecedneedhighlightlayer(nelist, np);
//							if (hs == 0) return true;
//							multixs = ns.node.highx - ns.node.lowx;
//							multiys = ns.node.highy - ns.node.lowy;
//							multiindent = ns.node.lowx - hs.node.lowx;
//							multisep = muldiv(tlist.f4, lambda, WHOLE);
//							ns.rule = us_tecedaddmulticutrule(multixs, multiys, multiindent, multisep);
//							if (ns.rule == 0) return true;
//							continue;
//						}
//					}
//				}
//			}

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
			String str = Manipulate.getValueOnNode(ns.node);
			if (str != null && str.length() == 0) str = null;
			ns.rule = us_tecedaddrule(newrule, false, str);
			if (ns.rule == null)
			{
				System.out.println("Cannot save stretching rule in " + np.describe());
				return null;
			}

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
			nodeLayers[count].style = Poly.Type.FILLED;
			nodeLayers[count].rule = ns.rule;
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
			Rectangle2D nodeBounds = ns.node.getBounds();
	
			// look at other examples and find samples associated with this
			nelist.studysample = ns;
			boolean error = false;
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
	
					// make sure the contact cut layer is opaque
					for(int i=0; i<lis.length; i++)
					{
						if (ns.layer.getName().substring(6).equalsIgnoreCase(lis[i].name))
						{
//							if (tech.layers[i].bits != LAYERO)
//							{
//								us_tecedpointout(ns.node, ns.node.getParent());
//								System.out.println(_("Multiple contact layers must not be transparent in %s"),
//									describenodeproto(np));
//								return true;
//							}
							break;
						}
					}
	
					// add the rule
//					if (us_tecedmulticut(ns, nelist, np)) return true;
					error = true;
					break;
				}
			}
			if (error) continue;
	
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
				Rectangle2D oNodeBounds = ni.getBounds();
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
			String str = Manipulate.getValueOnNode(ns.node);
			if (str != null && str.length() == 0) str = null;
			ns.rule = us_tecedaddrule(newrule, false, str);
			if (ns.rule == null) return null;

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
			nodeLayers[count].style = Poly.Type.FILLED;
			nodeLayers[count].rule = ns.rule;
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
			double portGrowth = 2;
			bounds.setRect(bounds.getMinX() - portGrowth, bounds.getMinY() - portGrowth,
				bounds.getWidth()+portGrowth*2, bounds.getHeight()+portGrowth*2);
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
				horiz = EdgeH.fromRight(pts[i].getX()-nelist.hx);
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
				vert = EdgeV.fromTop(pts[i].getY()-nelist.hy);
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
	
//	SAMPLE *us_tecedneedhighlightlayer(EXAMPLE *nelist, NODEPROTO *np)
//	{
//		REGISTER SAMPLE *hs;
//	
//		// find the highlight layer
//		for(hs = nelist.firstsample; hs != NOSAMPLE; hs = hs.nextsample)
//			if (hs.layer == NONODEPROTO) return(hs);
//	
//		us_tecedpointout(NONODEINST, np);
//		System.out.println(_("No highlight layer on contact %s"), describenodeproto(np));
//		return(0);
//	}
//	
//	/*
//	 * routine to build a rule for multiple contact-cut sample "ns" from the
//	 * overall example list in "nelist".  Returns true on error.
//	 */
//	BOOLEAN us_tecedmulticut(SAMPLE *ns, EXAMPLE *nelist, NODEPROTO *np)
//	{
//		REGISTER INTBIG total, i, multixs, multiys, multiindent, multisep;
//		REGISTER INTBIG xsep, ysep, sepx, sepy;
//		REGISTER SAMPLE **nslist, *nso, *hs;
//		REGISTER EXAMPLE *ne;
//	
//		// find the highlight layer
//		hs = us_tecedneedhighlightlayer(nelist, np);
//		if (hs == 0) return(TRUE);
//	
//		// determine size of each cut
//		multixs = ns.node.highx - ns.node.lowx;
//		multiys = ns.node.highy - ns.node.lowy;
//	
//		// determine indentation of cuts
//		multiindent = ns.node.lowx - hs.node.lowx;
//		if (hs.node.highx - ns.node.highx != multiindent ||
//			ns.node.lowy - hs.node.lowy != multiindent ||
//			hs.node.highy - ns.node.highy != multiindent)
//		{
//			us_tecedpointout(ns.node, ns.node.parent);
//			System.out.println(_("Multiple contact cuts must be indented uniformly in %s"),
//				describenodeproto(np));
//			return(TRUE);
//		}
//	
//		// look at every example after the first
//		xsep = ysep = -1;
//		for(ne = nelist.nextexample; ne != NOEXAMPLE; ne = ne.nextexample)
//		{
//			// count number of samples equivalent to the main sample
//			total = 0;
//			for(nso = ne.firstsample; nso != NOSAMPLE; nso = nso.nextsample)
//				if (nso.assoc == ns)
//			{
//				// make sure size is proper
//				if (multixs != nso.node.highx - nso.node.lowx ||
//					multiys != nso.node.highy - nso.node.lowy)
//				{
//					us_tecedpointout(nso.node, nso.node.parent);
//					System.out.println(_("Multiple contact cuts must not differ in size in %s"),
//						describenodeproto(np));
//					return(TRUE);
//				}
//				total++;
//			}
//	
//			// allocate space for these samples
//			nslist = (SAMPLE **)emalloc((total * (sizeof (SAMPLE *))), el_tempcluster);
//			if (nslist == 0) return(TRUE);
//	
//			// fill the list of samples
//			i = 0;
//			for(nso = ne.firstsample; nso != NOSAMPLE; nso = nso.nextsample)
//				if (nso.assoc == ns) nslist[i++] = nso;
//	
//			// analyze the samples for separation
//			for(i=1; i<total; i++)
//			{
//				// find separation
//				sepx = abs((nslist[i-1].node.highx + nslist[i-1].node.lowx) / 2 -
//					(nslist[i].node.highx + nslist[i].node.lowx) / 2);
//				sepy = abs((nslist[i-1].node.highy + nslist[i-1].node.lowy) / 2 -
//					(nslist[i].node.highy + nslist[i].node.lowy) / 2);
//	
//				// check for validity
//				if (sepx < multixs && sepy < multiys)
//				{
//					us_tecedpointout(nslist[i].node, nslist[i].node.parent);
//					System.out.println(_("Multiple contact cuts must not overlap in %s"),
//						describenodeproto(np));
//					efree((CHAR *)nslist);
//					return(TRUE);
//				}
//	
//				// accumulate minimum separation
//				if (sepx >= multixs)
//				{
//					if (xsep < 0) xsep = sepx; else
//					{
//						if (xsep > sepx) xsep = sepx;
//					}
//				}
//				if (sepy >= multiys)
//				{
//					if (ysep < 0) ysep = sepy; else
//					{
//						if (ysep > sepy) ysep = sepy;
//					}
//				}
//			}
//	
//			// finally ensure that all separations are multiples of "multisep"
//			for(i=1; i<total; i++)
//			{
//				// find X separation
//				sepx = abs((nslist[i-1].node.highx + nslist[i-1].node.lowx) / 2 -
//					(nslist[i].node.highx + nslist[i].node.lowx) / 2);
//				sepy = abs((nslist[i-1].node.highy + nslist[i-1].node.lowy) / 2 -
//					(nslist[i].node.highy + nslist[i].node.lowy) / 2);
//				if (sepx / xsep * xsep != sepx)
//				{
//					us_tecedpointout(nslist[i].node, nslist[i].node.parent);
//					System.out.println(_("Multiple contact cut X spacing must be uniform in %s"),
//						describenodeproto(np));
//					efree((CHAR *)nslist);
//					return(TRUE);
//				}
//	
//				// find Y separation
//				if (sepy / ysep * ysep != sepy)
//				{
//					us_tecedpointout(nslist[i].node, nslist[i].node.parent);
//					System.out.println(_("Multiple contact cut Y spacing must be uniform in %s"),
//						describenodeproto(np));
//					efree((CHAR *)nslist);
//					return(TRUE);
//				}
//			}
//			efree((CHAR *)nslist);
//		}
//		multisep = xsep - multixs;
//		if (multisep != ysep - multiys)
//		{
//			us_tecedpointout(NONODEINST, np);
//			System.out.println(_("Multiple contact cut X and Y spacing must be the same in %s"),
//				describenodeproto(np));
//			return(TRUE);
//		}
//		ns.rule = us_tecedaddmulticutrule(multixs, multiys, multiindent, multisep);
//		if (ns.rule == 0) return(TRUE);
//		return(FALSE);
//	}
//	
//	RULE *us_tecedaddmulticutrule(INTBIG multixs, INTBIG multiys, INTBIG multiindent, INTBIG multisep)
//	{
//		REGISTER RULE *rule;
//		INTBIG rulearr[8];
//	
//		rulearr[0] = -H0;   rulearr[1] = K1;
//		rulearr[2] = -H0;   rulearr[3] = K1;
//		rulearr[4] = -H0;   rulearr[5] = K3;
//		rulearr[6] = -H0;   rulearr[7] = K3;
//		rule = us_tecedaddrule(rulearr, 8, TRUE, (CHAR *)0);
//		if (rule == NORULE) return(0);
//		rule.multixs = multixs;
//		rule.multiys = multiys;
//		rule.multiindent = multiindent;
//		rule.multisep = multisep;
//		return(rule);
//	}

	static Rule us_tecedaddrule(Technology.TechPoint [] list, boolean multcut, String istext)
	{
		for(Rule r = us_tecedfirstrule; r != null; r = r.nextrule)
		{
			if (multcut != r.multicut) continue;
			if (istext != null && r.msg != null)
			{
				if (!istext.equalsIgnoreCase(r.msg)) continue;
			} else if (istext != null || r.msg != null) continue;
			if (list.length != r.value.length) continue;
			boolean same = true;
			for(int i=0; i<list.length; i++) if (r.value[i] != list[i]) { same = false;   break; }
			if (same) return r;
		}
	
		Rule r = new Rule();
		r.value = new Technology.TechPoint[list.length];
		r.nextrule = us_tecedfirstrule;
		r.used = false;
		r.multicut = multcut;
		us_tecedfirstrule = r;
		for(int i=0; i<list.length; i++) r.value[i] = list[i];
		r.istext = 0;
		if (istext != null)
		{
			r.msg = istext;
			r.istext = 1;
		}
		return r;
	}

	/****************************** WRITE TECHNOLOGY AS "JAVA" CODE ******************************/
	
	/**
	 * Method to dump the layer information in technology "tech" to the stream in
	 * "f".
	 */
	static void us_teceditdumpjavalayers(PrintStream buffWriter, Generate.LayerInfo [] lList)
	{
//		CHAR date[30], *transparent, *l1, *l2, *l3, *l4, *l5;
//		REGISTER INTBIG i, j, k, red, green, blue;
//		REGISTER void *infstr;
//		float r, c;
//		REGISTER BOOLEAN extrafunction;
//		REGISTER VARIABLE *varr, *varc, *rvar, *gvar, *bvar;
//	
//		// write legal banner
//		xprintf(f, x_("// BE SURE TO INCLUDE THIS TECHNOLOGY IN Technology.initAllTechnologies()\n\n"));
//		xprintf(f, x_("/* -*- tab-width: 4 -*-\n"));
//		xprintf(f, x_(" *\n"));
//		xprintf(f, x_(" * Electric(tm) VLSI Design System\n"));
//		xprintf(f, x_(" *\n"));
//		xprintf(f, x_(" * File: %s.java\n"), techname);
//		xprintf(f, x_(" * %s technology description\n"), techname);
//		xprintf(f, x_(" * Generated automatically from a library\n"));
//		xprintf(f, x_(" *\n"));
//		estrcpy(date, timetostring(getcurrenttime()));
//		date[24] = 0;
//		xprintf(f, x_(" * Copyright (c) %s Sun Microsystems and Static Free Software\n"), &date[20]);
//		xprintf(f, x_(" *\n"));
//		xprintf(f, x_(" * Electric(tm) is free software; you can redistribute it and/or modify\n"));
//		xprintf(f, x_(" * it under the terms of the GNU General Public License as published by\n"));
//		xprintf(f, x_(" * the Free Software Foundation; either version 2 of the License, or\n"));
//		xprintf(f, x_(" * (at your option) any later version.\n"));
//		xprintf(f, x_(" *\n"));
//		xprintf(f, x_(" * Electric(tm) is distributed in the hope that it will be useful,\n"));
//		xprintf(f, x_(" * but WITHOUT ANY WARRANTY; without even the implied warranty of\n"));
//		xprintf(f, x_(" * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n"));
//		xprintf(f, x_(" * GNU General Public License for more details.\n"));
//		xprintf(f, x_(" *\n"));
//		xprintf(f, x_(" * You should have received a copy of the GNU General Public License\n"));
//		xprintf(f, x_(" * along with Electric(tm); see the file COPYING.  If not, write to\n"));
//		xprintf(f, x_(" * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,\n"));
//		xprintf(f, x_(" * Boston, Mass 02111-1307, USA.\n"));
//		xprintf(f, x_(" */\n"));
//		xprintf(f, x_("package com.sun.electric.technology.technologies;\n"));
//		xprintf(f, x_("\n"));
//	
//		// write header
//		xprintf(f, x_("import com.sun.electric.database.geometry.EGraphics;\n"));
//		xprintf(f, x_("import com.sun.electric.database.geometry.Poly;\n"));
//		xprintf(f, x_("import com.sun.electric.database.prototype.ArcProto;\n"));
//		xprintf(f, x_("import com.sun.electric.database.prototype.PortProto;\n"));
//		xprintf(f, x_("import com.sun.electric.database.prototype.NodeProto;\n"));
//		xprintf(f, x_("import com.sun.electric.technology.DRCRules;\n"));
//		xprintf(f, x_("import com.sun.electric.technology.EdgeH;\n"));
//		xprintf(f, x_("import com.sun.electric.technology.EdgeV;\n"));
//		xprintf(f, x_("import com.sun.electric.technology.Layer;\n"));
//		xprintf(f, x_("import com.sun.electric.technology.PrimitiveArc;\n"));
//		xprintf(f, x_("import com.sun.electric.technology.PrimitiveNode;\n"));
//		xprintf(f, x_("import com.sun.electric.technology.PrimitivePort;\n"));
//		xprintf(f, x_("import com.sun.electric.technology.SizeOffset;\n"));
//		xprintf(f, x_("import com.sun.electric.technology.Technology;\n"));
//		xprintf(f, x_("import com.sun.electric.technology.technologies.utils.MOSRules;\n"));
//		xprintf(f, x_("\n"));
//		xprintf(f, x_("import java.awt.Color;\n"));
//		xprintf(f, x_("\n"));
//	
//		xprintf(f, x_("/**\n"));
//		xprintf(f, x_(" * This is the %s Technology.\n"), tech.techdescript);
//		xprintf(f, x_(" */\n"));
//		xprintf(f, x_("public class %s extends Technology\n"), techname);
//		xprintf(f, x_("{\n"), techname);
//		xprintf(f, x_("\t/** the %s Technology object. */	public static final %s tech = new %s();\n"),
//			tech.techdescript, techname, techname);
//		if ((us_tecflags&(HASCONDRC|HASUNCONDRC)) != 0)
//		{
//			xprintf(f, x_("\tprivate static final double XX = -1;\n"));
//			xprintf(f, x_("\tprivate double [] conDist, unConDist;\n"));
//		}
//		xprintf(f, x_("\n"));
//	
//		xprintf(f, x_("\t// -------------------- private and protected methods ------------------------\n"));
//		xprintf(f, x_("\tprivate %s()\n"), techname);
//		xprintf(f, x_("\t{\n"));
//		xprintf(f, x_("\t\tsuper(\"%s\");\n"), techname);
//		xprintf(f, x_("\t\tsetTechDesc(\"%s\");\n"), tech.techdescript);
//		xprintf(f, x_("\t\tsetFactoryScale(%ld, true);   // in nanometers: really %g microns\n"),
//			tech.deflambda / 2, (float)tech.deflambda / 2000.0);
//		xprintf(f, x_("\t\tsetNoNegatedArcs();\n"));
//		xprintf(f, x_("\t\tsetStaticTechnology();\n"));
//	
//		// write the color map
//		if ((us_tecflags&HASCOLORMAP) != 0)
//		{
//			// determine the five transparent layers
//			l1 = x_("layer 1");
//			l2 = x_("layer 2");
//			l3 = x_("layer 3");
//			l4 = x_("layer 4");
//			l5 = x_("layer 5");
//			for(i=0; i<tech.layercount; i++)
//			{
//				if (tech.layers[i].bits == LAYERT1 && l1 == 0)
//					l1 = us_teclayer_names[i]; else
//				if (tech.layers[i].bits == LAYERT2 && l2 == 0)
//					l2 = us_teclayer_names[i]; else
//				if (tech.layers[i].bits == LAYERT3 && l3 == 0)
//					l3 = us_teclayer_names[i]; else
//				if (tech.layers[i].bits == LAYERT4 && l4 == 0)
//					l4 = us_teclayer_names[i]; else
//				if (tech.layers[i].bits == LAYERT5 && l5 == 0)
//					l5 = us_teclayer_names[i];
//			}
//			xprintf(f, x_("\t\tsetFactoryTransparentLayers(new Color []\n"));
//			xprintf(f, x_("\t\t{\n"));
//			xprintf(f, x_("\t\t\tnew Color(%3d,%3d,%3d), // %s\n"),
//				us_teccolmap[1].red, us_teccolmap[1].green, us_teccolmap[1].blue, l1);
//			xprintf(f, x_("\t\t\tnew Color(%3d,%3d,%3d), // %s\n"),
//				us_teccolmap[2].red, us_teccolmap[2].green, us_teccolmap[2].blue, l2);
//			xprintf(f, x_("\t\t\tnew Color(%3d,%3d,%3d), // %s\n"),
//				us_teccolmap[4].red, us_teccolmap[4].green, us_teccolmap[4].blue, l3);
//			xprintf(f, x_("\t\t\tnew Color(%3d,%3d,%3d), // %s\n"),
//				us_teccolmap[8].red, us_teccolmap[8].green, us_teccolmap[8].blue, l4);
//			xprintf(f, x_("\t\t\tnew Color(%3d,%3d,%3d), // %s\n"),
//				us_teccolmap[16].red, us_teccolmap[16].green, us_teccolmap[16].blue, l5);
//			xprintf(f, x_("\t\t});\n"));
//		}
//		xprintf(f, x_("\n"));
//	
//		// write the layer declarations
//		xprintf(f, x_("\t\t//**************************************** LAYERS ****************************************\n\n"));
//		for(i=0; i<tech.layercount; i++)
//		{
//			xprintf(f, x_("\t\t/** %s layer */\n"), us_teclayer_iname[i]);
//			xprintf(f, x_("\t\tLayer %s_lay = Layer.newInstance(this, \"%s\",\n"), us_teclayer_iname[i],
//				us_teclayer_names[i]);
//			xprintf(f, x_("\t\t\tnew EGraphics("));
//			if ((tech.layers[i].colstyle&NATURE) == SOLIDC) xprintf(f, x_("EGraphics.SOLID")); else
//			{
//				if ((tech.layers[i].colstyle&OUTLINEPAT) == 0)
//					xprintf(f, x_("EGraphics.PATTERNED")); else
//						xprintf(f, x_("EGraphics.OUTLINEPAT"));
//			}
//			xprintf(f, x_(", "));
//			if ((tech.layers[i].bwstyle&NATURE) == SOLIDC) xprintf(f, x_("EGraphics.SOLID")); else
//			{
//				if ((tech.layers[i].bwstyle&OUTLINEPAT) == 0)
//					xprintf(f, x_("EGraphics.PATTERNED")); else
//						xprintf(f, x_("EGraphics.OUTLINEPAT"));
//			}
//			transparent = "0";
//			switch (tech.layers[i].bits)
//			{
//				case LAYERT1: transparent = "EGraphics.TRANSPARENT_1";   break;
//				case LAYERT2: transparent = "EGraphics.TRANSPARENT_2";   break;
//				case LAYERT3: transparent = "EGraphics.TRANSPARENT_3";   break;
//				case LAYERT4: transparent = "EGraphics.TRANSPARENT_4";   break;
//				case LAYERT5: transparent = "EGraphics.TRANSPARENT_5";   break;
//			}
//			if (tech.layers[i].bits == LAYERO)
//			{
//				rvar = getvalkey((INTBIG)us_tool, VTOOL, VINTEGER|VISARRAY, us_colormap_red_key);
//				gvar = getvalkey((INTBIG)us_tool, VTOOL, VINTEGER|VISARRAY, us_colormap_green_key);
//				bvar = getvalkey((INTBIG)us_tool, VTOOL, VINTEGER|VISARRAY, us_colormap_blue_key);
//				if (rvar != NOVARIABLE && gvar != NOVARIABLE && bvar != NOVARIABLE)
//				{
//					red = ((INTBIG *)rvar.addr)[tech.layers[i].col];
//					green = ((INTBIG *)gvar.addr)[tech.layers[i].col];
//					blue = ((INTBIG *)bvar.addr)[tech.layers[i].col];
//				}
//			} else
//			{
//				red = us_teccolmap[tech.layers[i].col].red;
//				green = us_teccolmap[tech.layers[i].col].green;
//				blue = us_teccolmap[tech.layers[i].col].blue;
//			}
//			if (red < 0 || red > 255) red = 0;
//			if (green < 0 || green > 255) green = 0;
//			if (blue < 0 || blue > 255) blue = 0;
//			xprintf(f, x_(", %s, %ld,%ld,%ld, 0.8,true,\n"), transparent, red, green, blue);
//	
//			for(j=0; j<16; j++) if (tech.layers[i].raster[j] != 0) break;
//			if (j >= 16)
//				xprintf(f, x_("\t\t\tnew int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));\n\n")); else
//			{
//				for(j=0; j<16; j++)
//				{
//					xprintf(f, x_("\t\t\t"));
//					if (j == 0) xprintf(f, x_("new int[] { ")); else
//						xprintf(f, x_("\t\t\t"));
//					xprintf(f, x_("0x%04x"), tech.layers[i].raster[j]&0xFFFF);
//					if (j == 15) xprintf(f, x_("}));")); else
//						xprintf(f, x_(",   "));
//	
//					xprintf(f, x_("// "));
//					for(k=0; k<16; k++)
//						if ((tech.layers[i].raster[j] & (1 << (15-k))) != 0)
//							xprintf(f, x_("X")); else xprintf(f, x_(" "));
//					xprintf(f, x_("\n"));
//				}
//				xprintf(f, x_("\n"));
//			}
//		}
//	
//		// write the layer functions
//		xprintf(f, x_("\t\t// The layer functions\n"));
//		for(i=0; i<tech.layercount; i++)
//		{
//			k = us_teclayer_function[i];
//			infstr = initinfstr();
//			formatinfstr(infstr, x_("%s_lay.setFunction(Layer.Function."), us_teclayer_iname[i]);
//			if ((k&(LFTYPE|LFPTYPE)) == (LFDIFF|LFPTYPE))
//			{
//				addstringtoinfstr(infstr, "DIFFP");
//				k &= ~LFPTYPE;
//			} else if ((k&(LFTYPE|LFNTYPE)) == (LFDIFF|LFNTYPE))
//			{
//				addstringtoinfstr(infstr, "DIFFN");
//				k &= ~LFNTYPE;
//			} else if ((k&(LFTYPE|LFPTYPE)) == (LFWELL|LFPTYPE))
//			{
//				addstringtoinfstr(infstr, "WELLP");
//				k &= ~LFPTYPE;
//			} else if ((k&(LFTYPE|LFNTYPE)) == (LFWELL|LFNTYPE))
//			{
//				addstringtoinfstr(infstr, "WELLN");
//				k &= ~LFNTYPE;
//			} else if ((k&(LFTYPE|LFPTYPE)) == (LFIMPLANT|LFPTYPE))
//			{
//				addstringtoinfstr(infstr, "IMPLANTP");
//				k &= ~LFPTYPE;
//			} else if ((k&(LFTYPE|LFNTYPE)) == (LFIMPLANT|LFNTYPE))
//			{
//				addstringtoinfstr(infstr, "IMPLANTN");
//				k &= ~LFNTYPE;
//			} else if ((k&(LFTYPE|LFINTRANS)) == (LFPOLY1|LFINTRANS))
//			{
//				addstringtoinfstr(infstr, "GATE");
//				k &= ~LFINTRANS;
//			} else
//			{
//				addstringtoinfstr(infstr, &us_teclayer_functions[k&LFTYPE].constant[2]);
//			}
//			extrafunction = FALSE;
//			for(j=0; us_teclayer_functions[j].name != 0; j++)
//			{
//				if (us_teclayer_functions[j].value <= LFTYPE) continue;
//				if ((k&us_teclayer_functions[j].value) != 0)
//				{
//					if (extrafunction) addstringtoinfstr(infstr, "|"); else
//						addstringtoinfstr(infstr, ", ");
//					addstringtoinfstr(infstr, "Layer.Function.");
//					addstringtoinfstr(infstr, &us_teclayer_functions[j].constant[2]);
//					extrafunction = TRUE;
//				}
//			}
//			addstringtoinfstr(infstr, ");");
//			xprintf(f, x_("\t\t%s"), returninfstr(infstr));
//			xprintf(f, x_("\t\t// %s\n"), us_teclayer_names[i]);
//		}
//	
//		// write the CIF layer names
//		if ((us_tecflags&HASCIF) != 0)
//		{
//			xprintf(f, x_("\n\t\t// The CIF names\n"));
//			for(i=0; i<tech.layercount; i++)
//			{
//				xprintf(f, x_("\t\t%s_lay.setFactoryCIFLayer(\""), us_teclayer_iname[i]);
//				if (us_teccif_layers[i] != 0) xprintf(f, x_("%s"), us_teccif_layers[i]);
//				xprintf(f, x_("\");\t\t// %s\n"), us_teclayer_names[i]);
//			}
//		}
//	
//		// write the DXF layer numbers
//		if ((us_tecflags&HASDXF) != 0)
//		{
//			xprintf(f, x_("\n\t\t// The DXF names\n"));
//			for(i=0; i<tech.layercount; i++)
//			{
//				xprintf(f, x_("\t\t%s_lay.setFactoryDXFLayer(\""), us_teclayer_iname[i]);
//				xprintf(f, x_("%s"), us_tecdxf_layers[i]);
//				xprintf(f, x_("\");\t\t// %s\n"), us_teclayer_names[i]);
//			}
//		}
//	
//		// write the Calma GDS-II layer number
//		if ((us_tecflags&HASGDS) != 0)
//		{
//			xprintf(f, x_("\n\t\t// The GDS names\n"));
//			for(i=0; i<tech.layercount; i++)
//			{
//				xprintf(f, x_("\t\t%s_lay.setFactoryGDSLayer(\""), us_teclayer_iname[i]);
//				xprintf(f, x_("%s"), us_tecgds_layers[i]);
//				xprintf(f, x_("\");\t\t// %s\n"), us_teclayer_names[i]);
//			}
//		}
//	
//		// write the 3D information
//		if ((us_tecflags&HAS3DINFO) != 0)
//		{
//			xprintf(f, x_("\n\t\t// The layer height\n"));
//			for(i=0; i<tech.layercount; i++)
//			{
//				xprintf(f, x_("\t\t%s_lay.setFactory3DInfo("), us_teclayer_iname[i]);
//				xprintf(f, x_("%ld, %ld"), us_tec3d_thickness[i], us_tec3d_height[i]);
//				xprintf(f, x_(");\t\t// %s\n"), us_teclayer_names[i]);
//			}
//		}
//	
//		// write the SPICE information
//		if ((us_tecflags&(HASSPIRES|HASSPICAP|HASSPIECAP)) != 0)
//		{
//			xprintf(f, x_("\n\t\t// The SPICE information\n"));
//			for(i=0; i<tech.layercount; i++)
//			{
//				xprintf(f, x_("\t\t%s_lay.setFactoryParasitics("), us_teclayer_iname[i]);
//				if ((us_tecflags&HASSPIRES) == 0) xprintf(f, x_("0, ")); else
//					xprintf(f, x_("%s, "), us_tecedmakefloatstring(us_tecspice_res[i]));
//				if ((us_tecflags&HASSPICAP) == 0) xprintf(f, x_("0, ")); else
//					xprintf(f, x_("%s, "), us_tecedmakefloatstring(us_tecspice_cap[i]));
//				if ((us_tecflags&HASSPIECAP) == 0) xprintf(f, x_("0")); else
//					xprintf(f, x_("%s"), us_tecedmakefloatstring(us_tecspice_ecap[i]));
//				xprintf(f, x_(");\t\t// %s\n"), us_teclayer_names[i]);
//			}
//		}
//		varr = getval((INTBIG)tech, VTECHNOLOGY, -1, x_("SIM_spice_min_resistance"));
//		varc = getval((INTBIG)tech, VTECHNOLOGY, -1, x_("SIM_spice_min_capacitance"));
//		if (varr != NOVARIABLE || varc != NOVARIABLE)
//		{
//			if (varr != NOVARIABLE) r = castfloat(varr.addr); else r = 0.0;
//			if (varc != NOVARIABLE) c = castfloat(varr.addr); else c = 0.0;
//	        xprintf(f, x_("\t\tsetFactoryParasitics(%g, %g);\n"), r, c);
//		}
//	
//		// write design rules
//		if ((us_tecflags&(HASCONDRC|HASUNCONDRC)) != 0)
//		{
//			xprintf(f, x_("\n\t\t//******************** DESIGN RULES ********************\n"));
//	
//			if ((us_tecflags&HASCONDRC) != 0)
//			{
//				xprintf(f, x_("\n\t\tconDist = new double[] {\n"));
//				us_teceditdumpjavadrctab(f, us_tecdrc_rules.conlist, tech, FALSE);
//			}
//			if ((us_tecflags&HASUNCONDRC) != 0)
//			{
//				xprintf(f, x_("\n\t\tunConDist = new double[] {\n"));
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
//			xprintf(f, x_("\t\t\t//            "));
//			for(j=0; j<tech.layercount; j++)
//			{
//				if ((INTBIG)estrlen(us_teclayer_iname[j]) <= i) xprintf(f, x_(" ")); else
//					xprintf(f, x_("%c"), us_teclayer_iname[j][i]);
//				xprintf(f, x_("  "));
//			}
//			xprintf(f, x_("\n"));
//		}
//		if (isstring) distlist = (CHAR **)distances; else
//			amtlist = (INTBIG *)distances;
//		for(j=0; j<tech.layercount; j++)
//		{
//			(void)estrncpy(shortname, us_teclayer_iname[j], 6);
//			shortname[6] = 0;
//			xprintf(f, x_("\t\t\t/* %-6s */ "), shortname);
//			for(i=0; i<j; i++) xprintf(f, x_("   "));
//			for(i=j; i<tech.layercount; i++)
//			{
//				if (isstring)
//				{
//					msg = *distlist++;
//					xprintf(f, x_("x_(\"%s\")"), msg);
//				} else
//				{
//					amt = *amtlist++;
//					if (amt < 0) xprintf(f, x_("XX")); else
//					{
//						xprintf(f, x_("%g"), (float)amt/WHOLE);
//					}
//				}
//				if (j != tech.layercount-1 || i != tech.layercount-1)
//					xprintf(f, x_(","));
//			}
//			xprintf(f, x_("\n"));
//		}
//		xprintf(f, x_("\t\t};\n"));
//	}
	
	/**
	 * Method to dump the arc information in technology "tech" to the stream in
	 * "f".
	 */
	static void us_teceditdumpjavaarcs(PrintStream buffWriter, Generate.ArcInfo [] aList)
	{
//		REGISTER INTBIG i, j, k;
//	
//		// print the header
//		xprintf(f, x_("\n\t\t//******************** ARCS ********************\n"));
//	
//		// now write the arcs
//		for(i=0; i<tech.arcprotocount; i++)
//		{
//			xprintf(f, x_("\n\t\t/** %s arc */\n"), tech.arcprotos[i].arcname);
//			xprintf(f, x_("\t\tPrimitiveArc %s_arc = PrimitiveArc.newInstance(this, \"%s\", %g, new Technology.ArcLayer []\n"),
//				us_teceditconverttojava(tech.arcprotos[i].arcname), tech.arcprotos[i].arcname, (float)tech.arcprotos[i].arcwidth/WHOLE);
//			xprintf(f, x_("\t\t{\n"));
//			for(k=0; k<tech.arcprotos[i].laycount; k++)
//			{
//				xprintf(f, x_("\t\t\tnew Technology.ArcLayer(%s_lay, "),
//					us_teclayer_iname[tech.arcprotos[i].list[k].lay]);
//				if (tech.arcprotos[i].list[k].off == 0) xprintf(f, x_("0,")); else
//					xprintf(f, x_("%g,"), (float)tech.arcprotos[i].list[k].off/WHOLE);
//				if (tech.arcprotos[i].list[k].style == FILLED) xprintf(f, x_(" Poly.Type.FILLED)")); else
//					xprintf(f, x_(" Poly.Type.CLOSED)"));
//				if (k+1 < tech.arcprotos[i].laycount) xprintf(f, x_(","));
//				xprintf(f, x_("\n"));
//			}
//			xprintf(f, x_("\t\t});\n"));
//			for(j=0; us_tecarc_functions[j].name != 0; j++)
//				if (us_tecarc_functions[j].value ==
//					(INTBIG)((tech.arcprotos[i].initialbits&AFUNCTION)>>AFUNCTIONSH))
//			{
//				xprintf(f, x_("\t\t%s_arc.setFunction(PrimitiveArc.Function.%s);\n"),
//					us_teceditconverttojava(tech.arcprotos[i].arcname), &us_tecarc_functions[j].constant[2]);
//				break;
//			}
//			if (us_tecarc_functions[j].name == 0)
//				xprintf(f, x_("\t\t%s_arc.setFunction(PrimitiveArc.Function.UNKNOWN);\n"), us_teceditconverttojava(tech.arcprotos[i].arcname));
//			if ((tech.arcprotos[i].initialbits&CANWIPE) != 0)
//				xprintf(f, x_("\t\t%s_arc.setWipable();\n"), us_teceditconverttojava(tech.arcprotos[i].arcname));
//			if ((us_tecflags&HASARCWID) != 0 && us_tecarc_widoff[i] != 0)
//			{
//				xprintf(f, x_("\t\t%s_arc.setWidthOffset(%ld);\n"), us_teceditconverttojava(tech.arcprotos[i].arcname),
//					(float)us_tecarc_widoff[i]/WHOLE);
//			}
//	
//			if ((tech.arcprotos[i].initialbits&WANTFIXANG) != 0)
//				xprintf(f, x_("\t\t%s_arc.setFactoryFixedAngle(true);\n"), us_teceditconverttojava(tech.arcprotos[i].arcname));
//			if ((tech.arcprotos[i].initialbits&WANTNOEXTEND) != 0)
//				xprintf(f, x_("\t\t%s_arc.setFactoryExtended(false);\n"), us_teceditconverttojava(tech.arcprotos[i].arcname));
//			xprintf(f, x_("\t\t%s_arc.setFactoryAngleIncrement(%ld);\n"), us_teceditconverttojava(tech.arcprotos[i].arcname),
//				(tech.arcprotos[i].initialbits&AANGLEINC)>>AANGLEINCSH);
//	
//		}
	}
	
	/**
	 * Method to dump the node information in technology "tech" to the stream in
	 * "f".
	 */
	static void us_teceditdumpjavanodes(PrintStream buffWriter, Generate.NodeInfo [] nList)
	{
//		REGISTER RULE *r;
//		REGISTER INTBIG i, j, k, l, tot;
//		CHAR *ab;
//		REGISTER PCON *pc;
//		REGISTER BOOLEAN yaxis;
//		REGISTER TECH_POLYGON *plist;
//		REGISTER TECH_NODES *nlist;
//		REGISTER void *infstr;
//	
//		// make abbreviations for each node
//		for(i=0; i<tech.nodeprotocount; i++)
//		{
//			(void)allocstring(&ab, makeabbrev(tech.nodeprotos[i].nodename, FALSE), el_tempcluster);
//			tech.nodeprotos[i].creation = (NODEPROTO *)ab;
//	
//			// loop until the name is unique
//			for(;;)
//			{
//				// see if a previously assigned abbreviation is the same
//				for(j=0; j<i; j++)
//					if (namesame(ab, (CHAR *)tech.nodeprotos[j].creation) == 0) break;
//				if (j == i) break;
//	
//				// name conflicts: change it
//				l = estrlen(ab);
//				if (ab[l-1] >= '0' && ab[l-1] <= '8') ab[l-1]++; else
//				{
//					infstr = initinfstr();
//					addstringtoinfstr(infstr, ab);
//					addtoinfstr(infstr, '0');
//					(void)reallocstring(&ab, returninfstr(infstr), el_tempcluster);
//					tech.nodeprotos[i].creation = (NODEPROTO *)ab;
//				}
//			}
//		}
//	
//		xprintf(f, x_("\n\t\t//******************** RECTANGLE DESCRIPTIONS ********************"));
//		xprintf(f, x_("\n\n"));
//	
//		// print box information
//		i = 1;
//		for(r = us_tecedfirstrule; r != NORULE; r = r.nextrule)
//		{
//			if (!r.used) continue;
//			r.rindex = i++;
//			xprintf(f, x_("\t\tTechnology.TechPoint [] box_%ld = new Technology.TechPoint[] {\n"),
//				r.rindex);
//			for(j=0; j<r.count; j += 2)
//			{
//				if ((j%4) == 0)
//				{
//					yaxis = FALSE;
//					xprintf(f, x_("\t\t\tnew Technology.TechPoint("));
//				} else
//				{
//					yaxis = TRUE;
//				}
//				xprintf(f, x_("%s"), us_tecededgelabeljava(r.value[j], r.value[j+1], yaxis));
//				if ((j%4) == 0) xprintf(f, x_(", ")); else
//				{
//					xprintf(f, x_(")"));
//					if (j+1 < r.count) xprintf(f, x_(","));
//					xprintf(f, x_("\n"));
//				}
//			}
//			xprintf(f, x_("\t\t};\n"));
//		}
//	
//		xprintf(f, x_("\n\t\t//******************** NODES ********************\n"));
//	
//		// print node information
//		for(i=0; i<tech.nodeprotocount; i++)
//		{
//			// header comment
//			nlist = tech.nodeprotos[i];
//			ab = (CHAR *)nlist.creation;
//			xprintf(f, x_("\n\t\t/** %s */\n"), nlist.nodename);
//	
//			xprintf(f, x_("\t\tPrimitiveNode %s_node = PrimitiveNode.newInstance(\"%s\", this, %g, %g, "),
//				ab, nlist.nodename, (float)nlist.xsize/WHOLE, (float)nlist.ysize/WHOLE);
//			if (us_tecnode_widoff[i*4] != 0 || us_tecnode_widoff[i*4+1] != 0 ||
//				us_tecnode_widoff[i*4+2] != 0 || us_tecnode_widoff[i*4+3] != 0)
//			{
//				xprintf(f, x_("new SizeOffset(%g, %g, %g, %g),\n"),
//					(float)us_tecnode_widoff[i*4] / WHOLE, (float)us_tecnode_widoff[i*4+1] / WHOLE,
//					(float)us_tecnode_widoff[i*4+2] / WHOLE, (float)us_tecnode_widoff[i*4+3] / WHOLE);
//			} else
//			{
//				xprintf(f, x_("null,\n"));
//			}
//	
//			// print layers
//			xprintf(f, x_("\t\t\tnew Technology.NodeLayer []\n"));
//			xprintf(f, x_("\t\t\t{\n"));
//			tot = nlist.layercount;
//			for(j=0; j<tot; j++)
//			{
//				if (nlist.special == SERPTRANS) plist = &nlist.gra[j].basics; else
//					plist = &nlist.layerlist[j];
//				xprintf(f, x_("\t\t\t\tnew Technology.NodeLayer(%s_lay, %ld, Poly.Type."),
//					us_teclayer_iname[plist.layernum], plist.portnum);
//				switch (plist.style)
//				{
//					case FILLEDRECT:     xprintf(f, x_("FILLED,"));         break;
//					case CLOSEDRECT:     xprintf(f, x_("CLOSED,"));         break;
//					case CROSSED:        xprintf(f, x_("CROSSED,"));        break;
//					case FILLED:         xprintf(f, x_("FILLED,"));         break;
//					case CLOSED:         xprintf(f, x_("CLOSED,"));         break;
//					case OPENED:         xprintf(f, x_("OPENED,"));         break;
//					case OPENEDT1:       xprintf(f, x_("OPENEDT1,"));       break;
//					case OPENEDT2:       xprintf(f, x_("OPENEDT2,"));       break;
//					case OPENEDT3:       xprintf(f, x_("OPENEDT3,"));       break;
//					case VECTORS:        xprintf(f, x_("VECTORS,"));        break;
//					case CIRCLE:         xprintf(f, x_("CIRCLE,"));         break;
//					case THICKCIRCLE:    xprintf(f, x_("THICKCIRCLE,"));    break;
//					case DISC:           xprintf(f, x_("DISC,"));           break;
//					case CIRCLEARC:      xprintf(f, x_("CIRCLEARC,"));      break;
//					case THICKCIRCLEARC: xprintf(f, x_("THICKCIRCLEARC,")); break;
//					case TEXTCENT:       xprintf(f, x_("TEXTCENT,"));       break;
//					case TEXTTOP:        xprintf(f, x_("TEXTTOP,"));        break;
//					case TEXTBOT:        xprintf(f, x_("TEXTBOT,"));        break;
//					case TEXTLEFT:       xprintf(f, x_("TEXTLEFT,"));       break;
//					case TEXTRIGHT:      xprintf(f, x_("TEXTRIGHT,"));      break;
//					case TEXTTOPLEFT:    xprintf(f, x_("TEXTTOPLEFT,"));    break;
//					case TEXTBOTLEFT:    xprintf(f, x_("TEXTBOTLEFT,"));    break;
//					case TEXTTOPRIGHT:   xprintf(f, x_("TEXTTOPRIGHT,"));   break;
//					case TEXTBOTRIGHT:   xprintf(f, x_("TEXTBOTRIGHT,"));   break;
//					case TEXTBOX:        xprintf(f, x_("TEXTBOX,"));        break;
//					default:             xprintf(f, x_("????,"));           break;
//				}
//				switch (plist.representation)
//				{
//					case BOX:    xprintf(f, x_(" Technology.NodeLayer.BOX,"));     break;
//					case MINBOX: xprintf(f, x_(" Technology.NodeLayer.MINBOX,"));  break;
//					case POINTS: xprintf(f, x_(" Technology.NodeLayer.POINTS,"));  break;
//					default:     xprintf(f, x_(" Technology.NodeLayer.????,"));    break;
//				}
//				for(r = us_tecedfirstrule; r != NORULE; r = r.nextrule)
//					if (r.value == plist.points) break;
//				if (r != NORULE)
//				{
//					xprintf(f, x_(" box_%ld"), r.rindex);
//				} else
//					xprintf(f, x_(" box??"));
//				if (nlist.special == SERPTRANS)
//				{
//					xprintf(f, x_(", %g, %g, %g, %g"),
//						nlist.gra[j].lwidth / (float)WHOLE, nlist.gra[j].rwidth / (float)WHOLE,
//						nlist.gra[j].extendb / (float)WHOLE, nlist.gra[j].extendt / (float)WHOLE);
//				}
//				xprintf(f, x_(")"));
//				if (j+1 < tot) xprintf(f, x_(","));
//				xprintf(f, x_("\n"));
//			}
//			xprintf(f, x_("\t\t\t});\n"));
//	
//			// print ports
//			xprintf(f, x_("\t\t%s_node.addPrimitivePorts(new PrimitivePort[]\n"), ab);
//			xprintf(f, x_("\t\t\t{\n"));
//			for(j=0; j<nlist.portcount; j++)
//			{
//				xprintf(f, x_("	\t\t\tPrimitivePort.newInstance(this, %s_node, new ArcProto [] {"), ab);
//				for(pc = us_tecedfirstpcon; pc != NOPCON; pc = pc.nextpcon)
//					if (pc.connects == nlist.portlist[j].portarcs) break;
//				if (pc != NOPCON)
//				{
//					for(l=0; l<pc.total; l++)
//					{
//						k = pc.connects[l+1];
//						xprintf(f, x_("%s_arc"), us_teceditconverttojava(tech.arcprotos[k].arcname));
//						if (l+1 < pc.total) xprintf(f, x_(", "));
//					}
//				}
//				xprintf(f, x_("}, \"%s\", %ld,%ld, %ld, PortCharacteristic.UNKNOWN,\n"),
//					nlist.portlist[j].protoname,
//					(nlist.portlist[j].initialbits&PORTANGLE)>>PORTANGLESH,
//					(nlist.portlist[j].initialbits&PORTARANGE)>>PORTARANGESH,
//					(nlist.portlist[j].initialbits&PORTNET)>>PORTNETSH);
//				xprintf(f, x_("\t\t\t\t\t%s, %s, %s, %s)"),
//					us_tecededgelabeljava(nlist.portlist[j].lowxmul, nlist.portlist[j].lowxsum, FALSE),
//					us_tecededgelabeljava(nlist.portlist[j].lowymul, nlist.portlist[j].lowysum, TRUE),
//					us_tecededgelabeljava(nlist.portlist[j].highxmul, nlist.portlist[j].highxsum, FALSE),
//					us_tecededgelabeljava(nlist.portlist[j].highymul, nlist.portlist[j].highysum, TRUE));
//	
//				if (j+1 < nlist.portcount) xprintf(f, x_(","));
//				xprintf(f, x_("\n"));
//			}
//			xprintf(f, x_("\t\t\t});\n"));
//	
//			// print the node information
//			j = (nlist.initialbits&NFUNCTION)>>NFUNCTIONSH;
//			if (j < 0 || j >= MAXNODEFUNCTION) j = 0;
//			xprintf(f, x_("\t\t%s_node.setFunction(PrimitiveNode.Function.%s);\n"), ab, &nodefunctionconstantname(j)[2]);
//	
//			if ((nlist.initialbits&WIPEON1OR2) != 0)
//				xprintf(f, x_("\t\t%s_node.setWipeOn1or2();\n"), ab);
//			if ((nlist.initialbits&HOLDSTRACE) != 0)
//				xprintf(f, x_("\t\t%s_node.setHoldsOutline();\n"), ab);
//			if ((nlist.initialbits&NSQUARE) != 0)
//				xprintf(f, x_("\t\t%s_node.setSquare();\n"), ab);
//			if ((nlist.initialbits&ARCSWIPE) != 0)
//				xprintf(f, x_("\t\t%s_node.setArcsWipe();\n"), ab);
//			if ((nlist.initialbits&ARCSHRINK) != 0)
//				xprintf(f, x_("\t\t%s_node.setArcsShrink();\n"), ab);
//			if ((nlist.initialbits&NODESHRINK) != 0)
//				xprintf(f, x_("\t\t%s_node.setCanShrink();\n"), ab);
//			if ((nlist.initialbits&LOCKEDPRIM) != 0)
//				xprintf(f, x_("\t\t%s_node.setLockedPrim();\n"), ab);
//			if (nlist.special != 0)
//			{
//				switch (nlist.special)
//				{
//					case SERPTRANS:
//						xprintf(f, x_("\t\t%s_node.setSpecialType(PrimitiveNode.SERPTRANS);\n"), ab);
//						xprintf(f, x_("\t\t%s_node.setSpecialValues(new double [] {%g, %g, %g, %g, %g, %g});\n"),
//							ab, (float)nlist.f1/WHOLE, (float)nlist.f2/WHOLE, (float)nlist.f3/WHOLE,
//								(float)nlist.f4/WHOLE, (float)nlist.f5/WHOLE, (float)nlist.f6/WHOLE);
//						break;
//					case POLYGONAL:
//						xprintf(f, x_("\t\t%s_node.setSpecialType(PrimitiveNode.POLYGONAL);\n"), ab);
//						break;
//					case MULTICUT:
//						xprintf(f, x_("\t\t%s_node.setSpecialType(PrimitiveNode.MULTICUT);\n"), ab);
//						xprintf(f, x_("\t\t%s_node.setSpecialValues(new double [] {%g, %g, %g, %g, %g, %g});\n"),
//							ab, (float)nlist.f1/WHOLE, (float)nlist.f2/WHOLE,
//								(float)nlist.f3/WHOLE, (float)nlist.f3/WHOLE,
//								(float)nlist.f4/WHOLE, (float)nlist.f4/WHOLE);
//						break;
//				}
//			}
//		}
//	
//		// write the pure-layer associations
//		xprintf(f, x_("\n\t\t// The pure layer nodes\n"));
//		for(i=0; i<tech.layercount; i++)
//		{
//			if ((us_teclayer_function[i]&LFPSEUDO) != 0) continue;
//	
//			// find the pure layer node
//			for(j=0; j<tech.nodeprotocount; j++)
//			{
//				nlist = tech.nodeprotos[j];
//				if (((nlist.initialbits&NFUNCTION)>>NFUNCTIONSH) != NPNODE) continue;
//				plist = &nlist.layerlist[0];
//				if (plist.layernum == i) break;
//			}
//			if (j >= tech.nodeprotocount) continue;
//			ab = (CHAR *)tech.nodeprotos[j].creation;
//			xprintf(f, x_("\t\t%s_lay.setPureLayerNode("), us_teclayer_iname[i]);
//			xprintf(f, x_("%s_node"), ab);
//			xprintf(f, x_(");\t\t// %s\n"), us_teclayer_names[i]);
//		}
//	
//		xprintf(f, x_("\t};\n"));
//	
//	#if 0
//		// print grab point informaton if it exists
//		if ((us_tecflags&HASGRAB) != 0 && us_tecnode_grabcount > 0)
//		{
//			xprintf(f, x_("\nstatic INTBIG %s_centergrab[] = {\n"), us_tecedmakesymbol(techname));
//			for(i=0; i<us_tecnode_grabcount; i += 3)
//			{
//				ab = (CHAR *)tech.nodeprotos[us_tecnode_grab[i]-1].creation;
//				xprintf(f, x_("\tN%s, %ld, %ld"), us_tecedmakeupper(ab), us_tecnode_grab[i+1],
//					us_tecnode_grab[i+2]);
//				if (i != us_tecnode_grabcount-3) xprintf(f, x_(",\n"));
//			}
//			xprintf(f, x_("\n};\n"));
//		}
//	
//		// print minimum node size informaton if it exists
//		if ((us_tecflags&HASMINNODE) != 0)
//		{
//			xprintf(f, x_("\nstatic INTBIG %s_node_minsize[NODEPROTOCOUNT*2] = {\n"), us_tecedmakesymbol(techname));
//			for(i=0; i<tech.nodeprotocount; i++)
//			{
//				if (us_tecdrc_rules.minnodesize[i*2] < 0) ab = x_("XX"); else
//					ab = us_tecedmakefract(us_tecdrc_rules.minnodesize[i*2]);
//				xprintf(f, x_("\t%s, "), ab);
//				if (us_tecdrc_rules.minnodesize[i*2+1] < 0) ab = x_("XX"); else
//					ab = us_tecedmakefract(us_tecdrc_rules.minnodesize[i*2+1]);
//				xprintf(f, x_("%s"), ab);
//				if (i == tech.nodeprotocount-1) ab = x_(""); else ab = x_(",");
//				xprintf(f, x_("%s\t\t/* %s */\n"), ab, tech.nodeprotos[i].nodename);
//			}
//			xprintf(f, x_("};\n"));
//		}
//		if ((us_tecflags&HASMINNODER) != 0)
//		{
//			xprintf(f, x_("\nstatic char *%s_node_minsize_rule[NODEPROTOCOUNT] = {\n"), us_tecedmakesymbol(techname));
//			for(i=0; i<tech.nodeprotocount; i++)
//			{
//				if (i == tech.nodeprotocount-1) ab = x_(""); else ab = x_(",");
//				xprintf(f, x_("\tx_(\"%s\")%s\t\t/* %s */\n"), us_tecdrc_rules.minnodesizeR[i], ab,
//					tech.nodeprotos[i].nodename);
//			}
//			xprintf(f, x_("};\n"));
//		}
//	#endif
//	
//		// write method to reset rules
//		if ((us_tecflags&(HASCONDRC|HASUNCONDRC)) != 0)
//		{
//			CHAR *conword, *unconword;
//			if ((us_tecflags&HASCONDRC) != 0) conword = "conDist"; else conword = "null";
//			if ((us_tecflags&HASUNCONDRC) != 0) unconword = "unConDist"; else unconword = "null";
//			xprintf(f, x_("\tpublic DRCRules getFactoryDesignRules()\n"));
//			xprintf(f, x_("\t{\n"));
//			xprintf(f, x_("\t\treturn MOSRules.makeSimpleRules(this, %s, %s);\n"), conword, unconword);
//			xprintf(f, x_("\t}\n"));
//		}
//	
//		// clean up
//		for(i=0; i<tech.nodeprotocount; i++)
//		{
//			efree((CHAR *)tech.nodeprotos[i].creation);
//			tech.nodeprotos[i].creation = NONODEPROTO;
//		}
	}
	
//	/*
//	 * Routine to remove illegal Java charcters from "string".
//	 */
//	CHAR *us_teceditconverttojava(CHAR *string)
//	{
//		REGISTER void *infstr;
//		REGISTER CHAR *pt;
//	
//		infstr = initinfstr();
//		for(pt = string; *pt != 0; pt++)
//		{
//			if (*pt == '-') addtoinfstr(infstr, '_'); else
//				addtoinfstr(infstr, *pt);
//		}
//		return(returninfstr(infstr));
//	}
//	
//	/*
//	 * routine to convert the multiplication and addition factors in "mul" and
//	 * "add" into proper constant names.  The "yaxis" is false for X and 1 for Y
//	 */
//	CHAR *us_tecededgelabeljava(INTBIG mul, INTBIG add, BOOLEAN yaxis)
//	{
//		REGISTER INTBIG amt;
//		REGISTER void *infstr;
//	
//		infstr = initinfstr();
//	
//		// handle constant distance from center
//		if (mul == 0)
//		{
//			if (yaxis) addstringtoinfstr(infstr, "EdgeV."); else
//				addstringtoinfstr(infstr, "EdgeH.");
//			if (add == 0)
//			{
//				addstringtoinfstr(infstr, x_("makeCenter()"));
//			} else
//			{
//				formatinfstr(infstr, x_("fromCenter(%g)"), (float)add/WHOLE);
//			}
//			return(returninfstr(infstr));
//		}
//	
//		// handle constant distance from edge
//		if ((mul == H0 || mul == -H0))
//		{
//			if (yaxis) addstringtoinfstr(infstr, "EdgeV."); else
//				addstringtoinfstr(infstr, "EdgeH.");
//			amt = abs(add);
//			if (!yaxis)
//			{
//				if (mul < 0)
//				{
//					if (add == 0) addstringtoinfstr(infstr, x_("makeLeftEdge()")); else
//						formatinfstr(infstr, x_("fromLeft(%g)"), (float)amt/WHOLE);
//				} else
//				{
//					if (add == 0) addstringtoinfstr(infstr, x_("makeRightEdge()")); else
//						formatinfstr(infstr, x_("fromRight(%g)"), (float)amt/WHOLE);
//				}
//			} else
//			{
//				if (mul < 0)
//				{
//					if (add == 0) addstringtoinfstr(infstr, x_("makeBottomEdge()")); else
//						formatinfstr(infstr, x_("fromBottom(%g)"), (float)amt/WHOLE);
//				} else
//				{
//					if (add == 0) addstringtoinfstr(infstr, x_("makeTopEdge()")); else
//						formatinfstr(infstr, x_("fromTop(%g)"), (float)amt/WHOLE);
//				}
//			}
//			return(returninfstr(infstr));
//		}
//	
//		// generate two-value description
//		if (!yaxis)
//			formatinfstr(infstr, x_("new EdgeH(%g, %g)"), (float)mul/WHOLE, (float)add/WHOLE); else
//			formatinfstr(infstr, x_("new EdgeV(%g, %g)"), (float)mul/WHOLE, (float)add/WHOLE);
//		return(returninfstr(infstr));
//	}
//	
//	/****************************** SUPPORT FOR SOURCE-CODE GENERATION ******************************/
//	
//	/*
//	 * Routine to return a string representation of the floating point value "v".
//	 * The letter "f" is added to the end if appropriate.
//	 */
//	CHAR *us_tecedmakefloatstring(float v)
//	{
//		static CHAR retstr[50];
//		REGISTER CHAR *pt;
//	
//		esnprintf(retstr, 50, x_("%g"), v);
//		if (estrcmp(retstr, x_("0")) == 0) return(retstr);
//		for(pt = retstr; *pt != 0; pt++)
//			if (*pt == '.') break;
//		if (*pt == 0) estrcat(retstr, x_(".0"));
//		estrcat(retstr, x_("f"));
//		return(retstr);
//	}
//	
//	/*
//	 * routine to convert the fractional value "amt" to a technology constant.
//	 * The presumption is that quarter values exist from K0 to K10, that
//	 * half values exist up to K20, that whole values exist up to K30, and
//	 * that other values are not necessarily defined in "tech.h".
//	 */
//	CHAR *us_tecedmakefract(INTBIG amt)
//	{
//		static CHAR line[21];
//		REGISTER INTBIG whole;
//		REGISTER CHAR *pt;
//	
//		pt = line;
//		if (amt < 0)
//		{
//			*pt++ = '-';
//			amt = -amt;
//		}
//		whole = amt/WHOLE;
//		switch (amt%WHOLE)
//		{
//			case 0:
//				if (whole <= 30) (void)esnprintf(pt, 20, x_("K%ld"), whole); else
//					(void)esnprintf(pt, 20, x_("%ld"), amt);
//				break;
//			case Q0:
//				if (whole <= 10) (void)esnprintf(pt, 20, x_("Q%ld"), whole); else
//					(void)esnprintf(pt, 20, x_("%ld"), amt);
//				break;
//			case H0:
//				if (whole <= 20) (void)esnprintf(pt, 20, x_("H%ld"), whole); else
//					(void)esnprintf(pt, 20, x_("%ld"), amt);
//				break;
//			case T0:
//				if (whole <= 10) (void)esnprintf(pt, 20, x_("T%ld"), whole); else
//					(void)esnprintf(pt, 20, x_("%ld"), amt);
//				break;
//			default:
//				(void)esnprintf(pt, 20, x_("%ld"), amt);
//				break;
//		}
//		return(line);
//	}
//	
//	/*
//	 * routine to convert all characters in string "str" to upper case and to
//	 * change any nonalphanumeric characters to a "_"
//	 */
//	CHAR *us_tecedmakeupper(CHAR *str)
//	{
//		REGISTER CHAR ch;
//		REGISTER void *infstr;
//	
//		infstr = initinfstr();
//		while (*str != 0)
//		{
//			ch = *str++;
//			if (islower(ch)) ch = toupper(ch);
//			if (!isalnum(ch)) ch = '_';
//			addtoinfstr(infstr, ch);
//		}
//		return(returninfstr(infstr));
//	}
//	
//	/*
//	 * routine to change any nonalphanumeric characters in string "str" to a "_"
//	 */
//	CHAR *us_tecedmakesymbol(CHAR *str)
//	{
//		REGISTER CHAR ch;
//		REGISTER void *infstr;
//	
//		infstr = initinfstr();
//		while (*str != 0)
//		{
//			ch = *str++;
//			if (!isalnum(ch)) ch = '_';
//			addtoinfstr(infstr, ch);
//		}
//		return(returninfstr(infstr));
//	}
//	
//	/*
//	 * Routine to find the parameter value in a string that has been stored as a message
//	 * on a node.  These parameters always have the form "name: value".  This returns a pointer
//	 * to the "value" part.
//	 */
//	CHAR *us_teceditgetparameter(VARIABLE *var)
//	{
//		REGISTER CHAR *str, *orig;
//	
//		orig = str = (CHAR *)var.addr;
//		while (*str != 0 && *str != ':') str++;
//		if (*str == 0) return(orig);
//		*str++;
//		while (*str == ' ') str++;
//		return(str);
//	}
	
	static void us_tecedpointout(NodeInst ni, Cell np)
	{
//		REGISTER WINDOWPART *w;
//		CHAR *newpar[2];
//	
//		for(w = el_topwindowpart; w != NOWINDOWPART; w = w.nextwindowpart)
//			if (w.curnodeproto == np) break;
//		if (w == NOWINDOWPART)
//		{
//			newpar[0] = describenodeproto(np);
//			us_editcell(1, newpar);
//		}
//		if (ni != NONODEINST)
//		{
//			us_clearhighlightcount();
//			(void)asktool(us_tool, x_("show-object"), (INTBIG)ni.geom);
//		}
	}
	
	static String us_tecedsamplename(NodeProto layernp)
	{
		if (layernp == Generic.tech.portNode) return "PORT";
		if (layernp == Generic.tech.cellCenterNode) return "GRAB";
		if (layernp == null) return "HIGHLIGHT";
		return layernp.getName().substring(6);
	}

}
