/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Manipulate.java
 * Technology Editor, editing technology libraries
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

import com.sun.electric.database.CellId;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.*;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.erc.ERC;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.PromptAt;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/**
 * This class manipulates technology libraries.
 */
public class Manipulate
{
	/**
	 * Method to update tables to reflect that cell "oldName" is now called "newName".
	 * If "newName" is not valid, any rule that refers to "oldName" is removed.
	 */
	public static void renamedCell(String oldName, String newName)
	{
		// if this is a layer, rename the layer sequence array
		if (oldName.startsWith("layer-") && newName.startsWith("layer-"))
		{
			renameSequence(Info.LAYERSEQUENCE_KEY, oldName.substring(6), newName.substring(6));
		}

		// if this is an arc, rename the arc sequence array
		if (oldName.startsWith("arc-") && newName.startsWith("arc-"))
		{
			renameSequence(Info.ARCSEQUENCE_KEY, oldName.substring(4), newName.substring(4));
		}

		// if this is a node, rename the node sequence array
		if (oldName.startsWith("node-") && newName.startsWith("node-"))
		{
			renameSequence(Info.NODESEQUENCE_KEY, oldName.substring(5), newName.substring(5));
		}

//		// see if there are design rules in the current library
//		var = getval((INTBIG)el_curlib, VLIBRARY, VSTRING|VISARRAY, x_("EDTEC_DRC"));
//		if (var == NOVARIABLE) return;
//
//		// examine the rules and convert the name
//		len = getlength(var);
//		sa = newstringarray(us_tool.cluster);
//		for(i=0; i<len; i++)
//		{
//			// parse the DRC rule
//			str = ((CHAR **)var.addr)[i];
//			origstr = str;
//			firstkeyword = getkeyword(&str, x_(" "));
//			if (firstkeyword == NOSTRING) return;
//
//			// pass wide wire limitation through
//			if (*firstkeyword == 'l')
//			{
//				addtostringarray(sa, origstr);
//				continue;
//			}
//
//			// rename nodes in the minimum node size rule
//			if (*firstkeyword == 'n')
//			{
//				if (namesamen(oldName, x_("node-"), 5) == 0 &&
//					namesame(&oldName[5], &firstkeyword[1]) == 0)
//				{
//					// substitute the new name
//					if (namesamen(newName, x_("node-"), 5) == 0)
//					{
//						infstr = initinfstr();
//						addstringtoinfstr(infstr, x_("n"));
//						addstringtoinfstr(infstr, &newName[5]);
//						addstringtoinfstr(infstr, str);
//						addtostringarray(sa, returninfstr(infstr));
//					}
//					continue;
//				}
//				addtostringarray(sa, origstr);
//				continue;
//			}
//
//			// rename layers in the minimum layer size rule
//			if (*firstkeyword == 's')
//			{
//				valid = TRUE;
//				infstr = initinfstr();
//				formatinfstr(infstr, x_("%s "), firstkeyword);
//				keyword = getkeyword(&str, x_(" "));
//				if (keyword == NOSTRING) return;
//				if (namesamen(oldName, x_("layer-"), 6) == 0 &&
//					namesame(&oldName[6], keyword) == 0)
//				{
//					if (namesamen(newName, x_("layer-"), 6) != 0) valid = FALSE; else
//						addstringtoinfstr(infstr, &newName[6]);
//				} else
//					addstringtoinfstr(infstr, keyword);
//				addstringtoinfstr(infstr, str);
//				str = returninfstr(infstr);
//				if (valid) addtostringarray(sa, str);
//				continue;
//			}
//
//			// layer width rule: substitute layer names
//			infstr = initinfstr();
//			formatinfstr(infstr, x_("%s "), firstkeyword);
//			valid = TRUE;
//
//			// get the first layer name and convert it
//			keyword = getkeyword(&str, x_(" "));
//			if (keyword == NOSTRING) return;
//			if (namesamen(oldName, x_("layer-"), 6) == 0 &&
//				namesame(&oldName[6], keyword) == 0)
//			{
//				// substitute the new name
//				if (namesamen(newName, x_("layer-"), 6) != 0) valid = FALSE; else
//					addstringtoinfstr(infstr, &newName[6]);
//			} else
//				addstringtoinfstr(infstr, keyword);
//			addtoinfstr(infstr, ' ');
//
//			// get the second layer name and convert it
//			keyword = getkeyword(&str, x_(" "));
//			if (keyword == NOSTRING) return;
//			if (namesamen(oldName, x_("layer-"), 6) == 0 &&
//				namesame(&oldName[6], keyword) == 0)
//			{
//				// substitute the new name
//				if (namesamen(newName, x_("layer-"), 6) != 0) valid = FALSE; else
//					addstringtoinfstr(infstr, &newName[6]);
//			} else
//				addstringtoinfstr(infstr, keyword);
//
//			addstringtoinfstr(infstr, str);
//			str = returninfstr(infstr);
//			if (valid) addtostringarray(sa, str);
//		}
//		strings = getstringarray(sa, &count);
//		setval((INTBIG)el_curlib, VLIBRARY, x_("EDTEC_DRC"), (INTBIG)strings,
//			VSTRING|VISARRAY|(count<<VLENGTHSH));
//		killstringarray(sa);
	}

	/**
	 * Method called when a cell has been deleted.
	 */
	public static void deletedCell(Cell np)
	{
		if (np.getName().startsWith("layer-"))
		{
			// may have deleted layer cell in technology library
			String layerName = np.getName().substring(6);
			StringBuffer warning = null;
			for(Iterator<Cell> it = np.getLibrary().getCells(); it.hasNext(); )
			{
				Cell oNp = it.next();
				boolean isNode = false;
				if (oNp.getName().startsWith("node-")) isNode = true; else
					if (!oNp.getName().startsWith("arc-")) continue;
				for(Iterator<NodeInst> nIt = oNp.getNodes(); nIt.hasNext(); )
				{
					NodeInst ni = nIt.next();
					Variable var = ni.getVar(Info.LAYER_KEY);
					if (var == null) continue;
					CellId cID = (CellId)var.getObject();
					Cell varCell = EDatabase.serverDatabase().getCell(cID);
					if (varCell == np)
					{
						if (warning != null) warning.append(","); else
						{
							warning = new StringBuffer();
							warning.append("Warning: layer " + layerName + " is used in");
						}
						if (isNode) warning.append(" node " + oNp.getName().substring(5)); else
							warning.append(" arc " + oNp.getName().substring(4));
						break;
					}
				}
			}
			if (warning != null) System.out.println(warning.toString());

			// see if this layer is mentioned in the design rules
			renamedCell(np.getName(), "");
		} else if (np.getName().startsWith("node-"))
		{
			// see if this node is mentioned in the design rules
			renamedCell(np.getName(), "");
		}
	}

	/**
	 * Method to rename the layer/arc/node sequence arrays to account for a name change.
	 * The sequence array is in variable "varName", and the item has changed from "oldName" to
	 * "newName".
	 */
	private static void renameSequence(Variable.Key varName, String oldName, String newName)
	{
		Library lib = Library.getCurrent();
		Variable var = lib.getVar(varName);
		if (var == null) return;

		String [] strings = (String [])var.getObject();
		for(int i=0; i<strings.length; i++)
			if (strings[i].equals(oldName)) strings[i] = newName;
		lib.newVar(varName, strings);
	}

	/**
	 * Method to determine whether it is legal to place an instance in a technology-edit cell.
	 * @param np the type of node to create.
	 * @param cell the cell in which to place it.
	 * @return true if the creation is invalid (and prints an error message).
	 */
	public static boolean invalidCreation(NodeProto np, Cell cell)
	{
		// make sure the cell is right
		if (!cell.getName().startsWith("node-") && !cell.getName().startsWith("arc-"))
		{
			System.out.println("Must be editing a node or arc to place geometry");
			return true;
		}
		if (np == Generic.tech.portNode && !cell.getName().startsWith("node-"))
		{
			System.out.println("Can only place ports in node descriptions");
			return true;
		}
		return false;
	}

	/**
	 * Make a new technology-edit cell of a given type.
	 * @param type 1=layer, 2=arc, 3=node, 4=factors
	 */
	public static void makeCell(int type)
	{
		Library lib = Library.getCurrent();
		String cellName = null;
		switch (type)
		{
			case 1:		// layer
				String layerName = JOptionPane.showInputDialog("Name of new layer:", "");
				if (layerName == null) return;
				cellName = "layer-" + layerName;
				break;
			case 2:		// arc
				String arcName = JOptionPane.showInputDialog("Name of new arc:", "");
				if (arcName == null) return;
				cellName = "arc-" + arcName;
				break;
			case 3:		// node
				String nodeName = JOptionPane.showInputDialog("Name of new node:", "");
				if (nodeName == null) return;
				cellName = "node-" + nodeName;
				break;
			case 4:		// factors
				cellName = "factors";
				break;
		}

		// see if the cell exists
		Cell cell = lib.findNodeProto(cellName);
		if (cell != null)
		{
			// cell exists: put it in the current window
			WindowFrame wf = WindowFrame.getCurrentWindowFrame();
			if (wf != null) wf.setCellWindow(cell, null);
			return;
		}

		// create the cell
		MakeOneCellJob job = new MakeOneCellJob(lib, cellName, type);
	}

	/**
	 * Class to create a single cell in a technology-library.
	 */
	private static class MakeOneCellJob extends Job
	{
		private Library lib;
		private String name;
		private int type;
		private Cell newCell;

		private MakeOneCellJob(Library lib, String name, int type)
		{
			super("Make Cell in Technology-Library", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.lib = lib;
			this.name = name;
			this.type = type;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			newCell = Cell.makeInstance(lib, name);
			if (newCell == null) return false;
			newCell.setInTechnologyLibrary();
			newCell.setTechnology(Artwork.tech);

			// specialty initialization
			switch (type)
			{
				case 1:
					LayerInfo li = new LayerInfo();
					li.generate(newCell);
					break;
				case 2:
					ArcInfo aIn = new ArcInfo();
					aIn.generate(newCell);
					break;
				case 3:
					NodeInfo nIn = new NodeInfo();
					nIn.generate(newCell);
					break;
			}

			// show it
			fieldVariableChanged("newCell");
			return true;
		}

        public void terminateOK()
        {
			WindowFrame wf = WindowFrame.getCurrentWindowFrame();
			if (wf != null && newCell != null) wf.setCellWindow(newCell, null);
        }
	}

	/**
	 * Method to complete the creation of a new node in a technology edit cell.
	 * @param newNi the node that was just created.
	 */
	public static void completeNodeCreation(NodeInst newNi, NodeInst niTemplate)
	{
		// postprocessing on the nodes
		String portName = null;
		if (newNi.getProto() == Generic.tech.portNode)
		{
			// a port layer
			portName = JOptionPane.showInputDialog("Port name:", "");
			if (portName == null) return;
		}
		boolean isHighlight = false;
		if (niTemplate != null)
		{
			Variable v = niTemplate.getVar(Info.OPTION_KEY);
			if (v != null)
			{
				if (v.getObject() instanceof Integer &&
					((Integer)v.getObject()).intValue() == Info.HIGHLIGHTOBJ)
				{
					isHighlight = true;
				}
			}
		}
		new AddTechEditMarks(newNi, isHighlight, portName);
	}

	/**
	 * Class to prepare a NodeInst for technology editing.
	 * Adds variables to the NodeInst which identify it to the technology editor.
	 */
	private static class AddTechEditMarks extends Job
	{
		private NodeInst newNi;
		private boolean isHighlight;
		private String portName;

		private AddTechEditMarks(NodeInst newNi, boolean isHighlight, String portName)
		{
			super("Prepare node for technology editing", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.newNi = newNi;
			this.isHighlight = isHighlight;
			this.portName = portName;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			if (isHighlight)
			{
				newNi.newVar(Info.OPTION_KEY, new Integer(Info.HIGHLIGHTOBJ));
				return true;
			}

			// set layer information
			newNi.newVar(Info.OPTION_KEY, new Integer(Info.LAYERPATCH));

			// postprocessing on the nodes
			if (newNi.getProto() == Generic.tech.portNode)
			{
				// a port layer
				newNi.newDisplayVar(Info.PORTNAME_KEY, portName);
				return true;
			}

			// a real layer: default to the first one
			String [] layerNames = getLayerNameList();
			if (layerNames != null && layerNames.length > 0)
			{
				Cell cell = Library.getCurrent().findNodeProto(layerNames[0]);
				if (cell != null)
				{
					newNi.newVar(Info.LAYER_KEY, cell.getId());
					LayerInfo li = LayerInfo.parseCell(cell);
					if (li != null)
						setPatch(newNi, li.desc);
				}
			}
			return true;
		}
    }

	/**
	 * Method to reorganize the dependent libraries
	 */
	public static void editLibraryDependencies()
	{
		EditDependentLibraries dialog = new EditDependentLibraries();
	}

	/**
	 * This class displays a dialog for editing library dependencies.
	 */
	private static class EditDependentLibraries extends EDialog
	{
		private JList allLibsList, depLibsList;
		private DefaultListModel allLibsModel, depLibsModel;
		private JTextField libToAdd;

		/** Creates new form edit library dependencies */
		private EditDependentLibraries()
		{
			super(null, true);
			initComponents();
			setVisible(true);
		}

		private void ok() { exit(true); }

		protected void escapePressed() { exit(false); }

		// Call this method when the user clicks the OK button
		private void exit(boolean goodButton)
		{
			if (goodButton)
			{
				int numDeps = depLibsModel.size();
				String [] depLibs = new String[numDeps];
				for(int i=0; i<numDeps; i++)
					depLibs[i] = (String)depLibsModel.get(i);
				new ModifyDependenciesJob(depLibs);
			}
			setVisible(false);
			dispose();
		}

		/**
		 * Class for saving library dependencies.
		 */
		private static class ModifyDependenciesJob extends Job
		{
			private String [] depLibs;

			private ModifyDependenciesJob(String [] depLibs)
			{
				super("Modify Library Dependencies", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
				this.depLibs = depLibs;
				startJob();
			}

			public boolean doIt() throws JobException
			{
				Library lib = Library.getCurrent();
				if (depLibs.length == 0)
				{
					if (lib.getVar(Info.DEPENDENTLIB_KEY) != null)
						lib.delVar(Info.DEPENDENTLIB_KEY);
				} else
				{
					lib.newVar(Info.DEPENDENTLIB_KEY, depLibs);
				}
				return true;
			}
		}

		private void removeLib()
		{
			int index = depLibsList.getSelectedIndex();
			if (index < 0) return;
			depLibsModel.remove(index);
		}

		private void addLib()
		{
			String value = (String)allLibsList.getSelectedValue();
			String specialLib = libToAdd.getText();
			if (specialLib.length() > 0)
			{
				value = specialLib;
				libToAdd.setText("");
			}

			if (value == null) return;
			for(int i=0; i<depLibsModel.size(); i++)
			{
				String depLib = (String)depLibsModel.get(i);
				if (depLib.equals(value)) return;
			}
			depLibsModel.addElement(value);
		}

		private void initComponents()
		{
			getContentPane().setLayout(new GridBagLayout());

			setTitle("Dependent Library Selection");
			setName("");
			addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent evt) { exit(false); }
			});

			// left column
			JLabel lab1 = new JLabel("Dependent Libraries:");
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 0;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(lab1, gbc);

			JScrollPane depLibsPane = new JScrollPane();
			depLibsModel = new DefaultListModel();
			depLibsList = new JList(depLibsModel);
			depLibsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			depLibsPane.setViewportView(depLibsList);
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 1;
			gbc.gridheight = 4;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(depLibsPane, gbc);
			depLibsModel.clear();
			Library [] libs = Info.getDependentLibraries(Library.getCurrent());
			for(int i=0; i<libs.length; i++)
			{
				if (libs[i] == Library.getCurrent()) continue;
				depLibsModel.addElement(libs[i].getName());
			}

			JLabel lab2 = new JLabel("Current: " + Library.getCurrent().getName());
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 5;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(lab2, gbc);

			JLabel lab3 = new JLabel("Libraries are examined from bottom up");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 6;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(lab3, gbc);


			// center column
			JButton remove = new JButton("Remove");
			gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = 1;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(remove, gbc);
			remove.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { removeLib(); }
			});

			JButton add = new JButton("<< Add");
			gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = 2;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(add, gbc);
			add.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { addLib(); }
			});


			// right column
			JLabel lab4 = new JLabel("All Libraries:");
			gbc = new GridBagConstraints();
			gbc.gridx = 2;   gbc.gridy = 0;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(lab4, gbc);

			JScrollPane allLibsPane = new JScrollPane();
			allLibsModel = new DefaultListModel();
			allLibsList = new JList(allLibsModel);
			allLibsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			allLibsPane.setViewportView(allLibsList);
			gbc = new GridBagConstraints();
			gbc.gridx = 2;   gbc.gridy = 1;
			gbc.gridheight = 2;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(allLibsPane, gbc);
			allLibsModel.clear();
			for(Library lib : Library.getVisibleLibraries())
			{
				allLibsModel.addElement(lib.getName());
			}

			JLabel lab5 = new JLabel("Library (if not in list):");
			gbc = new GridBagConstraints();
			gbc.gridx = 2;   gbc.gridy = 3;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(lab5, gbc);

			libToAdd = new JTextField("");
			gbc = new GridBagConstraints();
			gbc.gridx = 2;   gbc.gridy = 4;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(libToAdd, gbc);

			// OK and Cancel
			JButton cancel = new JButton("Cancel");
			gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = 6;
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
			gbc.gridy = 6;
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
	 * Method to highlight information about all layers (or ports if "doPorts" is true)
	 */
	public static void identifyLayers(boolean doPorts)
	{
		EditWindow wnd = EditWindow.getCurrent();
		Cell np = WindowFrame.needCurCell();
		if (wnd == null || np == null) return;

		if (doPorts)
		{
			if (!np.getName().startsWith("node-"))
			{
				System.out.println("Must be editing a node to identify ports");
				return;
			}
		} else
		{
			if (!np.getName().startsWith("node-") && !np.getName().startsWith("arc-"))
			{
				System.out.println("Must be editing a node or arc to identify layers");
				return;
			}
		}

		// get examples
		Example neList = null;
		if (np.getName().startsWith("node-"))
			neList = Example.getExamples(np, true); else
				neList = Example.getExamples(np, false);
		if (neList == null) return;

		// count the number of appropriate samples in the main example
		int total = 0;
		for(Sample ns : neList.samples)
		{
			if (!doPorts)
			{
				if (ns.layer != Generic.tech.portNode) total++;
			} else
			{
				if (ns.layer == Generic.tech.portNode) total++;
			}
		}
		if (total == 0)
		{
			System.out.println("There are no " + (doPorts ? "ports" : "layers") + " to identify");
			return;
		}

		// make arrays for position and association
		double [] xPos = new double[total];
		double [] yPos = new double[total];
		Poly.Type [] style = new Poly.Type[total];
		Sample [] whichSam = new Sample[total];

		// fill in label positions
		int qTotal = (total+3) / 4;
		Rectangle2D screen = wnd.getBoundsInWindow();
		double ySep = screen.getHeight() / qTotal;
		double xSep = screen.getWidth() / qTotal;
		double indent = screen.getHeight() / 15;
		for(int i=0; i<qTotal; i++)
		{
			// label on the left side
			xPos[i] = screen.getMinX() + indent;
			yPos[i] = screen.getMinY() + ySep * i + ySep/2;
			style[i] = Poly.Type.TEXTLEFT;
			if (i+qTotal < total)
			{
				// label on the top side
				xPos[i+qTotal] = screen.getMinX() + xSep * i + xSep/2;
				yPos[i+qTotal] = screen.getMaxY() - indent;
				style[i+qTotal] = Poly.Type.TEXTTOP;
			}
			if (i+qTotal*2 < total)
			{
				// label on the right side
				xPos[i+qTotal*2] = screen.getMaxX() - indent;
				yPos[i+qTotal*2] = screen.getMinY() + ySep * i + ySep/2;
				style[i+qTotal*2] = Poly.Type.TEXTRIGHT;
			}
			if (i+qTotal*3 < total)
			{
				// label on the bottom side
				xPos[i+qTotal*3] = screen.getMinX() + xSep * i + xSep/2;
				yPos[i+qTotal*3] = screen.getMinY() + indent;
				style[i+qTotal*3] = Poly.Type.TEXTBOT;
			}
		}

		// fill in sample associations
		int k = 0;
		for(Sample ns : neList.samples)
		{
			if (!doPorts)
			{
				if (ns.layer != Generic.tech.portNode) whichSam[k++] = ns;
			} else
			{
				if (ns.layer == Generic.tech.portNode) whichSam[k++] = ns;
			}
		}

		// rotate through all configurations, finding least distance
		double bestDist = Double.MAX_VALUE;
		int bestRot = 0;
		for(int i=0; i<total; i++)
		{
			// find distance from each label to its sample center
			double dist = 0;
			for(int j=0; j<total; j++)
				dist += new Point2D.Double(xPos[j], yPos[j]).distance(new Point2D.Double(whichSam[j].xPos, whichSam[j].yPos));
			if (dist < bestDist)
			{
				bestDist = dist;
				bestRot = i;
			}

			// rotate the samples
			Sample ns = whichSam[0];
			for(int j=1; j<total; j++) whichSam[j-1] = whichSam[j];
			whichSam[total-1] = ns;
		}

		// rotate back to the best orientation
		for(int i=0; i<bestRot; i++)
		{
			Sample ns = whichSam[0];
			for(int j=1; j<total; j++) whichSam[j-1] = whichSam[j];
			whichSam[total-1] = ns;
		}

		// draw the highlighting
		Highlighter highlighter = wnd.getHighlighter();
		highlighter.clear();
		for(int i=0; i<total; i++)
		{
			Sample ns = whichSam[i];
			String msg = null;
			if (ns.layer == null)
			{
				msg = "HIGHLIGHT";
			} else if (ns.layer == Generic.tech.cellCenterNode)
			{
				msg = "GRAB";
			} else if (ns.layer == Generic.tech.portNode)
			{
				msg = Info.getPortName(ns.node);
				if (msg == null) msg = "?";
			} else msg = ns.layer.getName().substring(6);
			Point2D curPt = new Point2D.Double(xPos[i], yPos[i]);
			highlighter.addMessage(np, msg, curPt);

			SizeOffset so = ns.node.getSizeOffset();
			Rectangle2D nodeBounds = ns.node.getBounds();
			Point2D other = null;
			if (style[i] == Poly.Type.TEXTLEFT)
			{
				other = new Point2D.Double(nodeBounds.getMinX()+so.getLowXOffset(), nodeBounds.getCenterY());
			} else if (style[i] == Poly.Type.TEXTRIGHT)
			{
				other = new Point2D.Double(nodeBounds.getMaxX()-so.getHighXOffset(), nodeBounds.getCenterY());
			} else if (style[i] == Poly.Type.TEXTTOP)
			{
				other = new Point2D.Double(nodeBounds.getCenterX(), nodeBounds.getMaxY()-so.getHighYOffset());
			} else if (style[i] == Poly.Type.TEXTBOT)
			{
				other = new Point2D.Double(nodeBounds.getCenterX(), nodeBounds.getMinY()+so.getLowYOffset());
			}
			highlighter.addLine(curPt, other, np);
		}
		highlighter.finished();
	}

	/**
	 * Method to return information about a given object.
	 */
	public static String describeNodeMeaning(Geometric geom)
	{
		if (geom instanceof ArcInst)
		{
			// describe currently highlighted arc
			ArcInst ai = (ArcInst)geom;
			if (ai.getProto() != Generic.tech.universal_arc)
				return "This is an unimportant " + ai.getProto();
			if (ai.getHeadPortInst().getNodeInst().getProto() != Generic.tech.portNode ||
				ai.getTailPortInst().getNodeInst().getProto() != Generic.tech.portNode)
					return "This arc makes an unimportant connection";
			String pt1 = Info.getPortName(ai.getHeadPortInst().getNodeInst());
			String pt2 = Info.getPortName(ai.getTailPortInst().getNodeInst());
			if (pt1 == null || pt2 == null)
				return "This arc connects two port objects";
			return "This arc connects ports '" + pt1 + "' and '" + pt2 + "'";
		}
		NodeInst ni = (NodeInst)geom;
		Cell cell = ni.getParent();
		int opt = getOptionOnNode(ni);
		if (opt < 0) return "No relevance";

		switch (opt)
		{
			case Info.TECHDESCRIPT:
				return "The technology description";
			case Info.TECHSCALE:
				return "The technology scale";
			case Info.TECHSPICEMINRES:
				return "Minimum resistance of SPICE elements";
			case Info.TECHSPICEMINCAP:
				return "Minimum capacitance of SPICE elements";
			case Info.TECHGATESHRINK:
				return "The shrinkage of gates, in um";
			case Info.TECHGATEINCLUDED:
				return "Whether gates are included in resistance";
			case Info.TECHGROUNDINCLUDED:
				return "Whether to include the ground network in parasitics";
			case Info.TECHTRANSPCOLORS:
				return "The transparent colors";

			case Info.LAYER3DHEIGHT:
				return "The 3D height of " + cell;
			case Info.LAYER3DTHICK:
				return "The 3D thickness of " + cell;
			case Info.LAYERTRANSPARENCY:
				return "The transparency layer of " + cell;
			case Info.LAYERCIF:
				return "The CIF name of " + cell;
			case Info.LAYERCOLOR:
				return "The color of " + cell;
			case Info.LAYERLETTERS:
				return "The unique letter for " + cell + " (obsolete)";
			case Info.LAYERDXF:
				return "The DXF name(s) of " + cell + " (obsolete)";
			case Info.LAYERDRCMINWID:
				return "DRC minimum width " + cell + " (obsolete)";
			case Info.LAYERFUNCTION:
				return "The function of " + cell;
			case Info.LAYERGDS:
				return "The Calma GDS-II number of " + cell;
			case Info.LAYERPATCONT:
				return "A stipple-pattern controller";
			case Info.LAYERPATTERN:
				return "One of the bitmap squares in " + cell;
			case Info.LAYERSPICAP:
				return "The SPICE capacitance of " + cell;
			case Info.LAYERSPIECAP:
				return "The SPICE edge capacitance of " + cell;
			case Info.LAYERSPIRES:
				return "The SPICE resistance of " + cell;
			case Info.LAYERSTYLE:
				return "The style of " + cell;
			case Info.LAYERCOVERAGE:
				return "The desired coverage percentage for " + cell;

			case Info.ARCFIXANG:
				return "Whether " + cell + " is fixed-angle";
			case Info.ARCFUNCTION:
				return "The function of " + cell;
			case Info.ARCINC:
				return "The prefered angle increment of " + cell;
			case Info.ARCNOEXTEND:
				return "The arc extension of " + cell;
			case Info.ARCWIPESPINS:
				return "Thie arc coverage of " + cell;
			case Info.ARCANTENNARATIO:
				return "The maximum antenna ratio for " + cell;

			case Info.NODEFUNCTION:
				return "The function of " + cell;
			case Info.NODELOCKABLE:
				return "Whether " + cell + " can be locked (used in array technologies)";
			case Info.NODEMULTICUT:
				return "The separation between multiple contact cuts in " + cell + " (obsolete)";
			case Info.NODESERPENTINE:
				return "Whether " + cell + " is a serpentine transistor";
			case Info.NODESQUARE:
				return "Whether " + cell + " is square";
			case Info.NODEWIPES:
				return "Whether " + cell + " disappears when conencted to one or two arcs";

			case Info.CENTEROBJ:
				return "The grab point of " + cell;
			case Info.LAYERPATCH:
			case Info.HIGHLIGHTOBJ:
				Cell np = getLayerCell(ni);
				if (np == null) return "Highlight box";
				String msg = "Layer '" + np.getName().substring(6) + "'";
				Variable var = ni.getVar(Info.MINSIZEBOX_KEY);
				if (var != null) msg += " (at minimum size)";
				return msg;
			case Info.PORTOBJ:
				String pt = Info.getPortName(ni);
				if (pt == null) return "Unnamed port";
				return "Port '" + pt + "'";
		}
		return "Unknown information";
	}

	/**
	 * Method to obtain the layer associated with node "ni".  Returns 0 if the layer is not
	 * there or invalid.  Returns null if this is the highlight layer.
	 */
	static Cell getLayerCell(NodeInst ni)
	{
		Variable var = ni.getVar(Info.LAYER_KEY);
		if (var == null) return null;
		CellId cID = (CellId)var.getObject();
		Cell cell = EDatabase.serverDatabase().getCell(cID);
		if (cell != null)
		{
			// validate the reference
			for(Iterator<Cell> it = ni.getParent().getLibrary().getCells(); it.hasNext(); )
			{
				Cell oCell = it.next();
				if (oCell == cell) return cell;
			}
		}
		return null;
	}

	/**
	 * Method for modifying the selected object.  If two are selected, connect them.
	 */
	public static void modifyObject(EditWindow wnd, NodeInst ni, int opt)
	{
		// handle other cases
		switch (opt)
		{
			case Info.TECHDESCRIPT:      modTechDescription(wnd, ni);        break;
			case Info.TECHSCALE:         modTechScale(wnd, ni);              break;
			case Info.TECHSPICEMINRES:   modTechMinResistance(wnd, ni);      break;
			case Info.TECHSPICEMINCAP:   modTechMinCapacitance(wnd, ni);     break;
			case Info.TECHGATESHRINK:    modTechGateShrinkage(wnd, ni);      break;
			case Info.TECHGATEINCLUDED:  modTechGateIncluded(wnd, ni);       break;
			case Info.TECHGROUNDINCLUDED:modTechGroundIncluded(wnd, ni);     break;
			case Info.TECHTRANSPCOLORS:  modTechTransparentColors(wnd, ni);  break;

			case Info.LAYERFUNCTION:     modLayerFunction(wnd, ni);          break;
			case Info.LAYERCOLOR:        modLayerColor(wnd, ni);             break;
			case Info.LAYERTRANSPARENCY: modLayerTransparency(wnd, ni);      break;
			case Info.LAYERSTYLE:        modLayerStyle(wnd, ni);             break;
			case Info.LAYERCIF:          modLayerCIF(wnd, ni);               break;
			case Info.LAYERGDS:          modLayerGDS(wnd, ni);               break;
			case Info.LAYERSPIRES:       modLayerResistance(wnd, ni);        break;
			case Info.LAYERSPICAP:       modLayerCapacitance(wnd, ni);       break;
			case Info.LAYERSPIECAP:      modLayerEdgeCapacitance(wnd, ni);   break;
			case Info.LAYER3DHEIGHT:     modLayerHeight(wnd, ni);            break;
			case Info.LAYER3DTHICK:      modLayerThickness(wnd, ni);         break;
			case Info.LAYERPATTERN:      modLayerPattern(wnd, ni);           break;
			case Info.LAYERPATCONT:      doPatternControl(wnd, ni, 0);       break;
			case Info.LAYERPATCLEAR:     doPatternControl(wnd, ni, 1);       break;
			case Info.LAYERPATINVERT:    doPatternControl(wnd, ni, 2);       break;
			case Info.LAYERPATCOPY:      doPatternControl(wnd, ni, 3);       break;
			case Info.LAYERPATPASTE:     doPatternControl(wnd, ni, 4);       break;
			case Info.LAYERPATCH:        modLayerPatch(wnd, ni);             break;
			case Info.LAYERCOVERAGE:     modLayerCoverage(wnd, ni);          break;

			case Info.ARCFIXANG:         modArcFixAng(wnd, ni);              break;
			case Info.ARCFUNCTION:       modArcFunction(wnd, ni);            break;
			case Info.ARCINC:            modArcAngInc(wnd, ni);              break;
			case Info.ARCNOEXTEND:       modArcExtension(wnd, ni);           break;
			case Info.ARCWIPESPINS:      modArcWipes(wnd, ni);               break;
			case Info.ARCANTENNARATIO:   modArcAntennaRatio(wnd, ni);        break;

			case Info.NODEFUNCTION:      modNodeFunction(wnd, ni);           break;
			case Info.NODELOCKABLE:      modNodeLockability(wnd, ni);        break;
			case Info.NODESERPENTINE:    modNodeSerpentine(wnd, ni);         break;
			case Info.NODESQUARE:        modNodeSquare(wnd, ni);             break;
			case Info.NODEWIPES:         modNodeWipes(wnd, ni);              break;

			case Info.PORTOBJ:           modPort(wnd, ni);                   break;
			case Info.HIGHLIGHTOBJ:
				System.out.println("Cannot modify highlight boxes");
				break;
			default:
				System.out.println("Cannot modify this object");
				break;
		}
	}

	/***************************** OBJECT MODIFICATION *****************************/

	private static void modTechMinResistance(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		String newUnit = PromptAt.showPromptAt(wnd, ni, "Change Minimum Resistance",
			"Minimum resistance (for parasitics):", initialMsg);
		if (newUnit != null) new SetTextJob(ni, "Minimum Resistance: " + newUnit);
	}

	private static void modTechMinCapacitance(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		String newUnit = PromptAt.showPromptAt(wnd, ni, "Change Minimum Capacitance",
			"Minimum capacitance (for parasitics):", initialMsg);
		if (newUnit != null) new SetTextJob(ni, "Minimum Capacitance: " + newUnit);
	}

	private static void modArcAntennaRatio(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		String newUnit = PromptAt.showPromptAt(wnd, ni, "Change Antenna Ratio",
			"Maximum antenna ratio for this layer:", initialMsg);
		if (newUnit != null) new SetTextJob(ni, "Antenna Ratio: " + newUnit);
	}

	private static void modLayerCoverage(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		String newUnit = PromptAt.showPromptAt(wnd, ni, "Change Coverage Percent",
			"Desired coverage percentage:", initialMsg);
		if (newUnit != null) new SetTextJob(ni, "Coverage percent: " + newUnit);
	}

	private static void modTechGateShrinkage(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		String newUnit = PromptAt.showPromptAt(wnd, ni, "Change Gate Shrinkage",
			"Gate shrinkage (in um):", initialMsg);
		if (newUnit != null) new SetTextJob(ni, "Gate Shrinkage: " + newUnit);
	}

	private static void modTechGateIncluded(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		boolean initialChoice = initialMsg.equalsIgnoreCase("yes");
		boolean finalChoice = PromptAt.showPromptAt(wnd, ni, "Change Whether Gate is Included in Resistance",
			"Should the gate be included in resistance?", initialChoice);
		if (finalChoice != initialChoice)
		{
			new SetTextJob(ni, "Gates Included in Resistance: " + (finalChoice ? "Yes" : "No"));
		}
	}

	private static void modTechGroundIncluded(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		boolean initialChoice = initialMsg.equalsIgnoreCase("yes");
		boolean finalChoice = PromptAt.showPromptAt(wnd, ni, "Change Whether parasitics include the ground network",
			"Should parasitics include the ground network?", initialChoice);
		if (finalChoice != initialChoice)
		{
			new SetTextJob(ni, "Parasitics Includes Ground: " + (finalChoice ? "Yes" : "No"));
		}
	}

	private static void modTechTransparentColors(EditWindow wnd, NodeInst ni)
	{
		Variable var = ni.getVar(Info.TRANSLAYER_KEY);
		if (var == null) return;
		Color [] colors = GeneralInfo.getTransparentColors((String)var.getObject());
		for(;;)
		{
			PromptAt.Field [][] fields = new PromptAt.Field[colors.length+1][2];
			for(int i=0; i<colors.length; i++)
			{
				fields[i][0] = new PromptAt.Field("Transparent layer " + (i+1) + ":", colors[i]);
				JButton but = new JButton("Remove");
				fields[i][1] = new PromptAt.Field(""+(i+1), but);
			}
			JButton addBut = new JButton("Add");
			fields[colors.length][0] = new PromptAt.Field("add", addBut);

			String choice = PromptAt.showPromptAt(wnd, ni, "Change Transparent Colors", fields);
			if (choice == null) return;
			if (choice.length() == 0)
			{
				// done
				for(int i=0; i<colors.length; i++)
					colors[i] = (Color)fields[i][0].getFinal();
				SetTransparentColorJob job = new SetTransparentColorJob(ni, GeneralInfo.makeTransparentColorsLine(colors));

				// redraw the demo layer in this cell
				new RedoLayerGraphicsJob(ni.getParent());
				break;
			}

			if (choice.equals("add"))
			{
				// add a layer
				Color [] newColors = new Color[colors.length+1];
				for(int i=0; i<colors.length; i++)
					newColors[i] = (Color)fields[i][0].getFinal();
				newColors[colors.length] = new Color(128, 128, 128);
				colors = newColors;
				continue;
			}

			// a layer was removed
			int remove = TextUtils.atoi(choice);
			Color [] newColors = new Color[colors.length-1];
			int j = 0;
			for(int i=0; i<colors.length; i++)
			{
				if (i+1 == remove) continue;
				newColors[j++] = (Color)fields[i][0].getFinal();
			}
			colors = newColors;
		}
	}

	private static void modLayerHeight(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		String newHei = PromptAt.showPromptAt(wnd, ni, "Change 3D Height",
			"New 3D height (depth) for this layer:", initialMsg);
		if (newHei != null) new SetTextJob(ni, "3D Height: " + newHei);
	}

	private static void modLayerThickness(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		String newThk = PromptAt.showPromptAt(wnd, ni, "Change 3D Thickness",
			"New 3D thickness for this layer:", initialMsg);
		if (newThk != null) new SetTextJob(ni, "3D Thickness: " + newThk);
	}

	private static void modLayerColor(EditWindow wnd, NodeInst ni)
	{
		String initialString = Info.getValueOnNode(ni);
		StringTokenizer st = new StringTokenizer(initialString, ",");
		if (st.countTokens() != 5)
		{
			System.out.println("Color information must have 5 fields, separated by commas");
			return;
		}
		PromptAt.Field [] fields = new PromptAt.Field[3];
		int r = TextUtils.atoi(st.nextToken());
		int g = TextUtils.atoi(st.nextToken());
		int b = TextUtils.atoi(st.nextToken());

		fields[0] = new PromptAt.Field("Color:", new Color(r, g, b));
		fields[1] = new PromptAt.Field("Opacity (0-1):", st.nextToken());
		fields[2] = new PromptAt.Field("Foreground:", new String [] {"on", "off"}, st.nextToken());
		String choice = PromptAt.showPromptAt(wnd, ni, "Change Color", fields);
		if (choice == null) return;
		Color col = (Color)fields[0].getFinal();
		r = col.getRed();
		g = col.getGreen();
		b = col.getBlue();
		double o = TextUtils.atof((String)fields[1].getFinal());
		String oo = (String)fields[2].getFinal();
		new SetTextJob(ni, "Color: " + r + "," + g + "," + b + ", " + o + "," + oo);

		// redraw the demo layer in this cell
		new RedoLayerGraphicsJob(ni.getParent());
	}

	private static void modLayerTransparency(EditWindow wnd, NodeInst ni)
	{
		String initialTransLayer = Info.getValueOnNode(ni);
		String [] transNames = new String[11];
		transNames[0] = "none";
		transNames[1] = "layer 1";
		transNames[2] = "layer 2";
		transNames[3] = "layer 3";
		transNames[4] = "layer 4";
		transNames[5] = "layer 5";
		transNames[6] = "layer 6";
		transNames[7] = "layer 7";
		transNames[8] = "layer 8";
		transNames[9] = "layer 9";
		transNames[10] = "layer 10";
		String choice = PromptAt.showPromptAt(wnd, ni, "Change Transparent Layer",
			"New transparent layer number for this layer:", initialTransLayer, transNames);
		if (choice == null) return;
		new SetTextJob(ni, "Transparency: " + choice);

		// redraw the demo layer in this cell
		new RedoLayerGraphicsJob(ni.getParent());
	}

	private static void modLayerStyle(EditWindow wnd, NodeInst ni)
	{
		String initialStyleName = Info.getValueOnNode(ni);
		List<EGraphics.Outline> outlines = EGraphics.Outline.getOutlines();
		String [] styleNames = new String[outlines.size()+1];
		styleNames[0] = "Solid";
		int i = 1;
		for(EGraphics.Outline o : outlines)
		{
			styleNames[i++] = "Patterned/Outline=" + o.getName();
		}
		String choice = PromptAt.showPromptAt(wnd, ni, "Change Layer Drawing Style",
			"New drawing style for this layer:", initialStyleName, styleNames);
		if (choice == null) return;
		new SetTextJob(ni, "Style: " + choice);

		// redraw the demo layer in this cell
		new RedoLayerGraphicsJob(ni.getParent());
	}

	private static void modLayerCIF(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		String newCIF = PromptAt.showPromptAt(wnd, ni, "Change CIF layer name", "New CIF symbol for this layer:", initialMsg);
		if (newCIF != null) new SetTextJob(ni, "CIF Layer: " + newCIF);
	}

	private static void modLayerGDS(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		String newGDS = PromptAt.showPromptAt(wnd, ni, "Change GDS layer name", "New GDS symbol for this layer:", initialMsg);
		if (newGDS != null) new SetTextJob(ni, "GDS-II Layer: " + newGDS);
	}

	private static void modLayerResistance(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		String newRes = PromptAt.showPromptAt(wnd, ni, "Change SPICE Layer Resistance",
			"New SPICE resistance for this layer:", initialMsg);
		if (newRes != null) new SetTextJob(ni, "SPICE Resistance: " + newRes);
	}

	private static void modLayerCapacitance(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		String newCap = PromptAt.showPromptAt(wnd, ni, "Change SPICE Layer Capacitance",
			"New SPICE capacitance for this layer:", initialMsg);
		if (newCap != null) new SetTextJob(ni, "SPICE Capacitance: " + newCap);
	}

	private static void modLayerEdgeCapacitance(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		String newCap = PromptAt.showPromptAt(wnd, ni, "Change SPICE Layer Edge Capacitance",
			"New SPICE edge capacitance for this layer:", initialMsg);
		if (newCap != null) new SetTextJob(ni, "SPICE Edge Capacitance: " + newCap);
	}

	private static void modLayerFunction(EditWindow wnd, NodeInst ni)
	{
		String initialFuncName = Info.getValueOnNode(ni);
		int commaPos = initialFuncName.indexOf(',');
		if (commaPos >= 0) initialFuncName = initialFuncName.substring(0, commaPos);

		// make a list of all layer functions and extras
		List<Layer.Function> funs = Layer.Function.getFunctions();
		int [] extraBits = Layer.Function.getFunctionExtras();
		String [] functionNames = new String[funs.size() + extraBits.length];
		int j = 0;
		for(Layer.Function fun : funs)
		{
			functionNames[j++] = fun.toString();
		}
		for(int i=0; i<extraBits.length; i++)
			functionNames[j++] = Layer.Function.getExtraName(extraBits[i]);

		// prompt for a new layer function
		String choice = PromptAt.showPromptAt(wnd, ni, "Change Layer Function", "New function for this layer:", initialFuncName, functionNames);
		if (choice == null) return;

		// see if the choice is an extra
		int thisExtraBit = -1;
		for(int i=0; i<extraBits.length; i++)
		{
			if (choice.equals(Layer.Function.getExtraName(extraBits[i]))) { thisExtraBit = extraBits[i];   break; }
		}

		LayerInfo li = LayerInfo.parseCell(ni.getParent());
		if (li == null) return;
		if (thisExtraBit > 0)
		{
			// adding (or removing) an extra bit
			if ((li.funExtra & thisExtraBit) != 0) li.funExtra &= ~thisExtraBit; else
				li.funExtra |= thisExtraBit;
		} else
		{
			li.funExtra = 0;
			for(Layer.Function fun : funs)
			{
				if (fun.toString().equalsIgnoreCase(choice))
				{
					li.fun = fun;
					break;
				}
			}
		}
		new SetTextJob(ni, "Function: " + LayerInfo.makeLayerFunctionName(li.fun, li.funExtra));
	}

	private static int [] copiedPattern = null;

	private static void doPatternControl(EditWindow wnd, NodeInst ni, int forced)
	{
		if (forced == 0)
		{
			String [] operationNames = new String[4];
			operationNames[0] = "Clear Pattern";
			operationNames[1] = "Invert Pattern";
			operationNames[2] = "Copy Pattern";
			operationNames[3] = "Paste Pattern";
			String choice = PromptAt.showPromptAt(wnd, ni, "Pattern Operations", null, "", operationNames);
			if (choice == null) return;
			if (choice.equals("Clear Pattern")) forced = 1; else
			if (choice.equals("Invert Pattern")) forced = 2; else
			if (choice.equals("Copy Pattern")) forced = 3; else
			if (choice.equals("Paste Pattern")) forced = 4;
		}
		switch (forced)
		{
			case 1:		// clear pattern
				for(Iterator<NodeInst> it = ni.getParent().getNodes(); it.hasNext(); )
				{
					NodeInst pni = it.next();
					int opt = getOptionOnNode(pni);
					if (opt != Info.LAYERPATTERN) continue;
					int color = getLayerColor(pni);
					if (color != 0)
						new SetLayerPatternJob(pni, 0);
				}

				// redraw the demo layer in this cell
				new RedoLayerGraphicsJob(ni.getParent());
				break;
			case 2:		// invert pattern
				for(Iterator<NodeInst> it = ni.getParent().getNodes(); it.hasNext(); )
				{
					NodeInst pni = it.next();
					int opt = getOptionOnNode(pni);
					if (opt != Info.LAYERPATTERN) continue;
					int color = getLayerColor(pni);
					new SetLayerPatternJob(pni, ~color);
				}

				// redraw the demo layer in this cell
				new RedoLayerGraphicsJob(ni.getParent());
				break;
			case 3:		// copy pattern
				LayerInfo li = LayerInfo.parseCell(ni.getParent());
				if (li == null) return;
				copiedPattern = li.desc.getPattern();
				break;
			case 4:		// paste pattern
				if (copiedPattern == null) return;
				setLayerPattern(ni.getParent(), copiedPattern);

				// redraw the demo layer in this cell
				new RedoLayerGraphicsJob(ni.getParent());
				break;
		}
	}

	/**
	 * Method to return the color in layer-pattern node "ni" (off is 0, on is 0xFFFF).
	 */
	private static int getLayerColor(NodeInst ni)
	{
		if (ni.getProto() == Artwork.tech.boxNode) return 0;
		if (ni.getProto() != Artwork.tech.filledBoxNode) return 0;
		Variable var = ni.getVar(Artwork.ART_PATTERN);
		if (var == null) return 0xFFFF;
		return ((Short[])var.getObject())[0].intValue();
	}

	/**
	 * Class to create a technology-library from a technology.
	 */
	private static class SetLayerPatternJob extends Job
	{
		private NodeInst ni;
		private int color;

		private SetLayerPatternJob(NodeInst ni, int color)
		{
			super("Change Pattern In Layer", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.ni = ni;
			this.color = color;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			if (ni.getProto() == Artwork.tech.boxNode)
			{
				if (color == 0) return true;
				ni.replace(Artwork.tech.filledBoxNode, false, false);
			} else if (ni.getProto() == Artwork.tech.filledBoxNode)
			{
				Short [] col = new Short[16];
				for(int i=0; i<16; i++) col[i] = new Short((short)color);
				ni.newVar(Artwork.ART_PATTERN, col);
			}
			return true;
		}
	}

	/**
	 * Method to toggle the color of layer-pattern node "ni" (called when the user does a
	 * "technology edit" click on the node).
	 */
	private static void modLayerPattern(EditWindow wnd, NodeInst ni)
	{
		int color = getLayerColor(ni);
		new SetLayerPatternJob(ni, ~color);

		Highlighter h = wnd.getHighlighter();
		h.clear();
		h.addElectricObject(ni, ni.getParent());

		// redraw the demo layer in this cell
		new RedoLayerGraphicsJob(ni.getParent());
	}

	/**
	 * Method to get a list of layers in the current library (in the proper order).
	 * @return an array of strings with the names of the layers.
	 */
	private static String [] getLayerNameList()
	{
		Library [] dependentlibs = Info.getDependentLibraries(Library.getCurrent());
		Cell [] layerCells = Info.findCellSequence(dependentlibs, "layer-", Info.LAYERSEQUENCE_KEY);

		// build and fill array of layers for DRC parsing
		String [] layerNames = new String[layerCells.length];
		for(int i=0; i<layerCells.length; i++)
			layerNames[i] = layerCells[i].getName().substring(6);
		return layerNames;
	}

	/**
	 * Method to get a list of arcs in the current library (in the proper order).
	 * @return an array of strings with the names of the arcs.
	 */
	private static String [] getArcNameList()
	{
		Library [] dependentlibs = Info.getDependentLibraries(Library.getCurrent());
		Cell [] arcCells = Info.findCellSequence(dependentlibs, "arc-", Info.ARCSEQUENCE_KEY);

		// build and fill array of layers for DRC parsing
		String [] arcNames = new String[arcCells.length];
		for(int i=0; i<arcCells.length; i++)
			arcNames[i] = arcCells[i].getName().substring(4);
		return arcNames;
	}

	/**
	 * Method to get a list of arcs in the current library (in the proper order).
	 * @return an array of strings with the names of the arcs.
	 */
	private static String [] getNodeNameList()
	{
		Library [] dependentlibs = Info.getDependentLibraries(Library.getCurrent());
		Cell [] nodeCells = Info.findCellSequence(dependentlibs, "node-", Info.NODESEQUENCE_KEY);

		// build and fill array of nodes
		String [] nodeNames = new String[nodeCells.length];
		for(int i=0; i<nodeCells.length; i++)
			nodeNames[i] = nodeCells[i].getName().substring(5);
		return nodeNames;
	}

	/**
	 * Method to modify the layer information in node "ni".
	 */
	private static void modLayerPatch(EditWindow wnd, NodeInst ni)
	{
		Library [] dependentlibs = Info.getDependentLibraries(Library.getCurrent());
		Cell [] layerCells = Info.findCellSequence(dependentlibs, "layer-", Info.LAYERSEQUENCE_KEY);
		if (layerCells == null) return;

		String [] options = new String[layerCells.length + 2];
		for(int i=0; i<layerCells.length; i++)
			options[i] = layerCells[i].getName().substring(6);
		options[layerCells.length] = "SET-MINIMUM-SIZE";
		options[layerCells.length+1] = "CLEAR-MINIMUM-SIZE";
		String initial = options[0];
		Variable curLay = ni.getVar(Info.LAYER_KEY);
		if (curLay != null)
		{
			CellId cID = (CellId)curLay.getObject();
			Cell cell = EDatabase.serverDatabase().getCell(cID);
			initial = cell.getName().substring(6);
		} else
		{
			
		}
		String choice = PromptAt.showPromptAt(wnd, ni, "Change Layer", "New layer for this geometry:", initial, options);
		if (choice == null) return;

		// save the results
		ModifyLayerJob job = new ModifyLayerJob(ni, choice, layerCells);
	}

	/**
	 * Class to modify a port object in a node of the technology editor.
	 */
	private static class ModifyLayerJob extends Job
	{
		private NodeInst ni;
		private String choice;
		private Cell [] layerCells;

		private ModifyLayerJob(NodeInst ni, String choice, Cell [] layerCells)
		{
			super("Change Layer Information", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.ni = ni;
			this.choice = choice;
			this.layerCells = layerCells;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			if (choice.equals("SET-MINIMUM-SIZE"))
			{
				if (!ni.getParent().getName().startsWith("node-"))
				{
					System.out.println("Can only set minimum size in node descriptions");
					return true;
				}
				Variable var = ni.newDisplayVar(Info.MINSIZEBOX_KEY, "MIN");
				return true;
			}

			if (choice.equals("CLEAR-MINIMUM-SIZE"))
			{
				if (ni.getVar(Info.MINSIZEBOX_KEY) == null)
				{
					System.out.println("Minimum size is not set on this layer");
					return true;
				}
				ni.delVar(Info.MINSIZEBOX_KEY);
				return true;
			}

			// find the actual cell with that layer specification
			for(int i=0; i<layerCells.length; i++)
			{
				if (choice.equals(layerCells[i].getName().substring(6)))
				{
					// found the name, set the patch
					LayerInfo li = LayerInfo.parseCell(layerCells[i]);
					if (li == null) return true;
					setPatch(ni, li.desc);
					ni.newVar(Info.LAYER_KEY, layerCells[i].getId());
				}
			}
			System.out.println("Cannot find layer primitive " + choice);
			return true;
		}
	}

	/**
	 * Method to modify port characteristics
	 */
	private static void modPort(EditWindow wnd, NodeInst ni)
	{
		// count the number of arcs in this technology
		List<Cell> allArcs = new ArrayList<Cell>();
		for(Iterator<Cell> it = ni.getParent().getLibrary().getCells(); it.hasNext(); )
		{
			Cell cell = it.next();
			if (cell.getName().startsWith("arc-")) allArcs.add(cell);
		}

		// make a set of those arcs which can connect to this port
		HashSet<NodeProto> connectSet = new HashSet<NodeProto>();
		Variable var = ni.getVar(Info.CONNECTION_KEY);
		if (var != null)
		{
			CellId [] connects = (CellId [])var.getObject();
			for(int i=0; i<connects.length; i++)
				connectSet.add(EDatabase.serverDatabase().getCell(connects[i]));
		}

		// build an array of arc connections
		PromptAt.Field [] fields = new PromptAt.Field[allArcs.size()+2];
		for(int i=0; i<allArcs.size(); i++)
		{
			Cell cell = (Cell)allArcs.get(i);
			boolean doesConnect = connectSet.contains(cell);
			fields[i] = new PromptAt.Field(cell.getName().substring(4),
				new String [] {"Allowed", "Disallowed"}, (doesConnect ? "Allowed" : "Disallowed"));
		}
		Variable angVar = ni.getVar(Info.PORTANGLE_KEY);
		int ang = 0;
		if (angVar != null) ang = ((Integer)angVar.getObject()).intValue();
		Variable rangeVar = ni.getVar(Info.PORTRANGE_KEY);
		int range = 180;
		if (rangeVar != null) range = ((Integer)rangeVar.getObject()).intValue();
		fields[allArcs.size()] = new PromptAt.Field("Angle:", TextUtils.formatDouble(ang));
		fields[allArcs.size()+1] = new PromptAt.Field("Angle Range:", TextUtils.formatDouble(range));
		String choice = PromptAt.showPromptAt(wnd, ni, "Change Port", fields);
		if (choice == null) return;

		// save the results
		String [] fieldValues = new String[allArcs.size()];
		for(int i=0; i<allArcs.size(); i++)
		{
			fieldValues[i] = (String)fields[i].getFinal();
		}
		ModifyPortJob job = new ModifyPortJob(ni, allArcs, fieldValues);
	}

	/**
	 * Class to modify a port object in a node of the technology editor.
	 */
	private static class ModifyPortJob extends Job
	{
		private NodeInst ni;
		private List<Cell> allArcs;
		private String [] fieldValues;

		private ModifyPortJob(NodeInst ni, List<Cell> allArcs, String [] fieldValues)
		{
			super("Change Port Information", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.ni = ni;
			this.allArcs = allArcs;
			this.fieldValues = fieldValues;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			int numConnects = 0;
			for(int i=0; i<allArcs.size(); i++)
			{
				String answer = fieldValues[i];
				if (answer.equals("Allowed")) numConnects++;
			}
			Cell [] newConnects = new Cell[numConnects];
			int k = 0;
			for(int i=0; i<allArcs.size(); i++)
			{
				String answer = fieldValues[i];
				if (answer.equals("Allowed")) newConnects[k++] = (Cell)allArcs.get(i);
			}
			ni.newVar(Info.CONNECTION_KEY, newConnects);

			int newAngle = TextUtils.atoi(fieldValues[allArcs.size()]);
			ni.newVar(Info.PORTANGLE_KEY, new Integer(newAngle));
			int newRange = TextUtils.atoi(fieldValues[allArcs.size()+1]);
			ni.newVar(Info.PORTRANGE_KEY, new Integer(newRange));
			return true;
		}
	}

	private static void modArcFunction(EditWindow wnd, NodeInst ni)
	{
		String initialFuncName = Info.getValueOnNode(ni);
		List<ArcProto.Function> funs = ArcProto.Function.getFunctions();
		String [] functionNames = new String[funs.size()];
		for(int i=0; i<funs.size(); i++)
		{
			ArcProto.Function fun = (ArcProto.Function)funs.get(i);
			functionNames[i] = fun.toString();
		}
		String choice = PromptAt.showPromptAt(wnd, ni, "Change Arc Function", "New function for this arc:", initialFuncName, functionNames);
		if (choice == null) return;
		new SetTextJob(ni, "Function: " + choice);
	}

	private static void modArcFixAng(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		boolean initialChoice = initialMsg.equalsIgnoreCase("yes");
		boolean finalChoice = PromptAt.showPromptAt(wnd, ni, "Set whether this Arc Remains at a Fixed Angle",
			"Should instances of this arc be created with the 'fixed angle' constraint?", initialChoice);
		if (finalChoice != initialChoice)
		{
			new SetTextJob(ni, "Fixed-angle: " + (finalChoice ? "Yes" : "No"));
		}
	}

	private static void modArcWipes(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		boolean initialChoice = initialMsg.equalsIgnoreCase("yes");
		boolean finalChoice = PromptAt.showPromptAt(wnd, ni, "Set Whether this Arc Can Obscure a Pin Node",
			"Can this arc obscure a pin node (that is obscurable)?", initialChoice);
		if (finalChoice != initialChoice)
		{
			new SetTextJob(ni, "Wipes pins: " + (finalChoice ? "Yes" : "No"));
		}
	}

	private static void modArcExtension(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		boolean initialChoice = initialMsg.equalsIgnoreCase("yes");
		boolean finalChoice = PromptAt.showPromptAt(wnd, ni, "Set Extension Default",
			"Are new instances of this arc drawn with ends extended?", initialChoice);
		if (finalChoice != initialChoice)
		{
			new SetTextJob(ni, "Extend arcs: " + (finalChoice ? "Yes" : "No"));
		}
	}

	private static void modArcAngInc(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		String newInc = PromptAt.showPromptAt(wnd, ni, "Change Angle Increment",
			"New angular granularity for placing this type of arc:", initialMsg);
		if (newInc != null) new SetTextJob(ni, "Angle increment: " + newInc);
	}

	private static void modNodeFunction(EditWindow wnd, NodeInst ni)
	{
		String initialFuncName = Info.getValueOnNode(ni);
		List<PrimitiveNode.Function> funs = PrimitiveNode.Function.getFunctions();
		String [] functionNames = new String[funs.size()];
		for(int i=0; i<funs.size(); i++)
		{
			PrimitiveNode.Function fun = (PrimitiveNode.Function)funs.get(i);
			functionNames[i] = fun.toString();
		}
		String choice = PromptAt.showPromptAt(wnd, ni, "Change Node Function", "New function for this node:", initialFuncName, functionNames);
		if (choice == null) return;
		new SetTextJob(ni, "Function: " + choice);
	}

	private static void modNodeSerpentine(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		boolean initialChoice = initialMsg.equalsIgnoreCase("yes");
		boolean finalChoice = PromptAt.showPromptAt(wnd, ni, "Set Serpentine Transistor Capability",
			"Is this node a serpentine transistor?", initialChoice);
		if (finalChoice != initialChoice)
		{
			new SetTextJob(ni, "Serpentine transistor: " + (finalChoice ? "Yes" : "No"));
		}
	}

	private static void modNodeSquare(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		boolean initialChoice = initialMsg.equalsIgnoreCase("yes");
		boolean finalChoice = PromptAt.showPromptAt(wnd, ni, "Does Node Remain Square",
			"Must this node remain square?", initialChoice);
		if (finalChoice != initialChoice)
		{
			new SetTextJob(ni, "Square node: " + (finalChoice ? "Yes" : "No"));
		}
	}

	private static void modNodeWipes(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		boolean initialChoice = initialMsg.equalsIgnoreCase("yes");
		boolean finalChoice = PromptAt.showPromptAt(wnd, ni, "Set How Arcs Obscure This Node",
			"Is this node invisible when 1 or 2 arcs connect to it?", initialChoice);
		if (finalChoice != initialChoice)
		{
			new SetTextJob(ni, "Invisible with 1 or 2 arcs: " + (finalChoice ? "Yes" : "No"));
		}
	}

	private static void modNodeLockability(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		boolean initialChoice = initialMsg.equalsIgnoreCase("yes");
		boolean finalChoice = PromptAt.showPromptAt(wnd, ni, "Set Node Lockability",
			"Is this node able to be locked down (used for FPGA primitives):", initialChoice);
		if (finalChoice != initialChoice)
		{
			new SetTextJob(ni, "Lockable: " + (finalChoice ? "Yes" : "No"));
		}
	}

	private static void modTechScale(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		String newUnit = PromptAt.showPromptAt(wnd, ni, "Set Unit Size",
			"The scale of this technology (nanometers per grid unit):", initialMsg);
		if (newUnit != null) new SetTextJob(ni, "Lambda: " + newUnit);
	}

	private static void modTechDescription(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		String newDesc = PromptAt.showPromptAt(wnd, ni, "Set Technology Description",
			"Full description of this technology:", initialMsg);
		if (newDesc != null) new SetTextJob(ni, "Description: " + newDesc);
	}

	/****************************** UTILITIES ******************************/

	/**
	 * Class to create a technology-library from a technology.
	 */
	private static class SetTextJob extends Job
	{
		private NodeInst ni;
		private String chr;

		private SetTextJob(NodeInst ni, String chr)
		{
			super("Make Technology Library from Technology", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.ni = ni;
			this.chr = chr;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			ni.newDisplayVar(Artwork.ART_MESSAGE, chr);
			return true;
		}
	}

	/**
	 * Class to set transparent colors on a technology.
	 */
	private static class SetTransparentColorJob extends Job
	{
		private NodeInst ni;
		private String chr;

		private SetTransparentColorJob(NodeInst ni, String chr)
		{
			super("Set Transparent Colors", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.ni = ni;
			this.chr = chr;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			ni.newVar(Info.TRANSLAYER_KEY, chr);
			return true;
		}
	}

	/**
	 * Class to create a technology-library from a technology.
	 */
	private static class RedoLayerGraphicsJob extends Job
	{
		private Cell cell;

		private RedoLayerGraphicsJob(Cell cell)
		{
			super("Redo Layer Graphics", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			NodeInst patchNi = null;
			for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				if (ni.getProto() != Artwork.tech.filledBoxNode) continue;
				int opt = getOptionOnNode(ni);
				if (opt == Info.LAYERPATTERN) continue;
				patchNi = ni;
				break;
			}
			if (patchNi == null) return false;

			// get the current description of this layer
			LayerInfo li = LayerInfo.parseCell(cell);
			if (li == null) return false;

			// modify the demo patch to reflect the color and pattern
			setPatch(patchNi, li.desc);

			// now do this to all layers in all cells!
			for(Iterator<Cell> cIt = cell.getLibrary().getCells(); cIt.hasNext(); )
			{
				Cell onp = cIt.next();
				if (!onp.getName().startsWith("arc-") && !onp.getName().startsWith("node-")) continue;
				for(Iterator<NodeInst> nIt = onp.getNodes(); nIt.hasNext(); )
				{
					NodeInst cNi = nIt.next();
					if (getOptionOnNode(cNi) != Info.LAYERPATCH) continue;
					Variable varLay = cNi.getVar(Info.LAYER_KEY);
					if (varLay == null) continue;
					CellId cID = (CellId)varLay.getObject();
					Cell varCell = EDatabase.serverDatabase().getCell(cID);
					if (varCell != cell) continue;
					setPatch(cNi, li.desc);
				}
			}
			return true;
		}
	}

	static void setPatch(NodeInst ni, EGraphics desc)
	{
		if (desc.getTransparentLayer() > 0)
		{
			ni.newVar(Artwork.ART_COLOR, new Integer(EGraphics.makeIndex(desc.getTransparentLayer())));
		} else
		{
			ni.newVar(Artwork.ART_COLOR, new Integer(EGraphics.makeIndex(desc.getColor())));
		}
		if (desc.isPatternedOnDisplay())
		{
			int [] raster = desc.getPattern();
			Integer [] pattern = new Integer[17];
			for(int i=0; i<16; i++) pattern[i] = new Integer(raster[i]);
			pattern[16] = new Integer(desc.getOutlined().getIndex());
			ni.newVar(Artwork.ART_PATTERN, pattern);
		} else
		{
			if (ni.getVar(Artwork.ART_PATTERN) != null)
				ni.delVar(Artwork.ART_PATTERN);
		}
	}

	/**
	 * Method to set the layer-pattern squares of cell "np" to the bits in "desc".
	 */
	private static void setLayerPattern(Cell np, int [] pattern)
	{
		// look at all nodes in the layer description cell
		int patternCount = 0;
		Rectangle2D patternBounds = null;
		for(Iterator<NodeInst> it = np.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (ni.getProto() == Artwork.tech.boxNode || ni.getProto() == Artwork.tech.filledBoxNode)
			{
				Variable var = ni.getVar(Info.OPTION_KEY);
				if (var == null) continue;
				if (((Integer)var.getObject()).intValue() != Info.LAYERPATTERN) continue;
				Rectangle2D bounds = ni.getBounds();
				if (patternCount == 0)
				{
					patternBounds = bounds;
				} else
				{
					Rectangle2D.union(patternBounds, bounds, patternBounds);
				}
				patternCount++;
			}
		}

		if (patternCount != 16*16 && patternCount != 16*8)
		{
			System.out.println("Incorrect number of pattern boxes in " + np +
				" (has " + patternCount + ", not " + (16*16) + ")");
			return;
		}

		// set the pattern
		for(Iterator<NodeInst> it = np.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (ni.getProto() != Artwork.tech.boxNode && ni.getProto() != Artwork.tech.filledBoxNode) continue;
			Variable var = ni.getVar(Info.OPTION_KEY);
			if (var == null) continue;
			if (((Integer)var.getObject()).intValue() != Info.LAYERPATTERN) continue;

			Rectangle2D niBounds = ni.getBounds();
			int x = (int)((niBounds.getMinX() - patternBounds.getMinX()) / (patternBounds.getWidth() / 16));
			int y = (int)((patternBounds.getMaxY() - niBounds.getMaxY()) / (patternBounds.getHeight() / 16));
			int wantColor = 0;
			if ((pattern[y] & (1 << (15-x))) != 0) wantColor = 0xFFFF;

			int color = getLayerColor(ni);
			if (color != wantColor)
				new SetLayerPatternJob(ni, wantColor);
		}
	}

	/**
	 * Method to return the option index of node "ni"
	 */
	public static int getOptionOnNode(NodeInst ni)
	{
		// port objects are readily identifiable
		if (ni.getProto() == Generic.tech.portNode) return Info.PORTOBJ;

		// center objects are also readily identifiable
		if (ni.getProto() == Generic.tech.cellCenterNode) return Info.CENTEROBJ;

		Variable var = ni.getVar(Info.OPTION_KEY);
		if (var == null) return -1;
		int option = ((Integer)var.getObject()).intValue();
		if (option == Info.LAYERPATCH)
		{
			// may be a highlight object
			Variable var2 = ni.getVar(Info.LAYER_KEY);
			if (var2 != null)
			{
				if (var2.getObject() == null) return Info.HIGHLIGHTOBJ;
			}
		}
		return option;
	}

	/******************** SUPPORT ROUTINES ********************/

	public static void reorderPrimitives(int type)
	{
		RearrangeOrder dialog = new RearrangeOrder();
		dialog.lib = Library.getCurrent();
		dialog.type = type;
		dialog.initComponents();
		dialog.setVisible(true);
	}

	/**
	 * This class displays a dialog for rearranging layers, arcs, or nodes in a technology library.
	 */
	private static class RearrangeOrder extends EDialog
	{
		private JList list;
		private DefaultListModel model;
		private Library lib;
		private int type;

		/** Creates new form Rearrange technology components */
		private RearrangeOrder()
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
				String [] newList = new String[model.size()];
				for(int i=0; i<model.size(); i++)
					newList[i] = (String)model.getElementAt(i);
				new UpdateOrderingJob(lib, newList, type);
			}
			dispose();
		}

		private static class UpdateOrderingJob extends Job
		{
			private Library lib;
			private String [] newList;
			private int type;

			private UpdateOrderingJob(Library lib, String [] newList, int type)
			{
				super("Update Ordering", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
				this.lib = lib;
				this.newList = newList;
				this.type = type;
				startJob();
			}

			public boolean doIt() throws JobException
			{
				switch (type)
				{
					case 1: lib.newVar(Info.LAYERSEQUENCE_KEY, newList);   break;
					case 2: lib.newVar(Info.ARCSEQUENCE_KEY, newList);     break;
					case 3: lib.newVar(Info.NODESEQUENCE_KEY, newList);    break;
				}
				return true;
			}

	        public void terminateOK()
	        {
				// force redraw of explorer tree
	        	WindowFrame.wantToRedoLibraryTree();
	        }
		}

		/**
		 * Call when an up/down button is pressed.
		 * @param direction -2: far down   -1: down   1: up   2: far up
		 */
		private void moveSelected(int direction)
		{
			int index = list.getSelectedIndex();
			if (index < 0) return;
			int newIndex = index;
			switch (direction)
			{
				case -2: newIndex -= 10;   break;
				case -1: newIndex -= 1;    break;
				case  1: newIndex += 1;    break;
				case  2: newIndex += 10;   break;
			}
			if (newIndex < 0) newIndex = 0;
			if (newIndex >= model.size()) newIndex = model.size()-1;
			Object was = model.getElementAt(index);
			model.remove(index);
			model.add(newIndex, was);
			list.setSelectedIndex(newIndex);
			list.ensureIndexIsVisible(newIndex);
		}

		private void initComponents()
		{
			getContentPane().setLayout(new GridBagLayout());

			switch (type)
			{
				case 1: setTitle("Rearrange Layer Order");   break;
				case 2: setTitle("Rearrange Arc Order");     break;
				case 3: setTitle("Rearrange Node Order");    break;
			}
			setName("");
			addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent evt) { exit(false); }
			});

			JScrollPane center = new JScrollPane();
			center.setMinimumSize(new java.awt.Dimension(100, 50));
			center.setPreferredSize(new java.awt.Dimension(300, 200));
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;      gbc.gridy = 1;
			gbc.weightx = 1;    gbc.weighty = 1;
			gbc.gridwidth = 2;  gbc.gridheight = 4;
			gbc.anchor = java.awt.GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(center, gbc);

			model = new DefaultListModel();
			list = new JList(model);
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			center.setViewportView(list);

			model.clear();
			String [] listNames = null;
			switch (type)
			{
				case 1: listNames = getLayerNameList();   break;
				case 2: listNames = getArcNameList();     break;
				case 3: listNames = getNodeNameList();    break;
			}
			for(int i=0; i<listNames.length; i++)
				model.addElement(listNames[i]);

			JButton farUp = new JButton("Far Up");
			gbc = new GridBagConstraints();
			gbc.gridx = 2;   gbc.gridy = 1;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(farUp, gbc);
			farUp.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { moveSelected(-2); }
			});

			JButton up = new JButton("Up");
			gbc = new GridBagConstraints();
			gbc.gridx = 2;   gbc.gridy = 2;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(up, gbc);
			up.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { moveSelected(-1); }
			});

			JButton down = new JButton("Down");
			gbc = new GridBagConstraints();
			gbc.gridx = 2;   gbc.gridy = 3;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(down, gbc);
			down.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { moveSelected(1); }
			});

			JButton farDown = new JButton("Far Down");
			gbc = new GridBagConstraints();
			gbc.gridx = 2;   gbc.gridy = 4;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(farDown, gbc);
			farDown.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { moveSelected(2); }
			});

			// OK and Cancel
			JButton cancel = new JButton("Cancel");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 5;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(cancel, gbc);
			cancel.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { exit(false); }
			});

			JButton ok = new JButton("OK");
			getRootPane().setDefaultButton(ok);
			gbc = new java.awt.GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = 5;
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
	 * Method to print detailled information about a given technology.
	 * @param tech the technology to describe.
	 */
	public static void describeTechnology(Technology tech)
	{
		// ***************************** dump layers ******************************

		// allocate space for all layer fields
		int layerCount = tech.getNumLayers();
		String [] layerNames = new String[layerCount+1];
		String [] layerColors = new String[layerCount+1];
		String [] layerStyles = new String[layerCount+1];
		String [] layerCifs = new String[layerCount+1];
		String [] layerGdss = new String[layerCount+1];
		String [] layerFuncs = new String[layerCount+1];
		String [] layerCoverage = new String[layerCount+1];

		// load the header
		layerNames[0] = "Layer";
		layerColors[0] = "Color";
		layerStyles[0] = "Style";
		layerCifs[0] = "CIF";
		layerGdss[0] = "GDS";
		layerFuncs[0] = "Function";
		layerCoverage[0] = "Coverage";

        Foundry foundry = tech.getSelectedFoundry();

		// compute each layer
		for(int i=0; i<layerCount; i++)
		{
			Layer layer = tech.getLayer(i);
			layerNames[i+1] = layer.getName();

			EGraphics gra = layer.getGraphics();
			if (gra.getTransparentLayer() > 0) layerColors[i+1] = "Transparent " + gra.getTransparentLayer(); else
			{
				Color col = gra.getColor();
				layerColors[i+1] = "(" + col.getRed() + "," + col.getGreen() + "," + col.getBlue() + ")";
			}

			layerStyles[i+1] = "?";
			if (gra.isPatternedOnDisplay())
			{
				if (gra.getOutlined() != EGraphics.Outline.NOPAT) layerStyles[i+1] = "pat/outl"; else
					layerStyles[i+1] = "pat";
			} else
			{
				layerStyles[i+1] = "solid";
			}

			layerCifs[i+1] = layer.getCIFLayer();
//			layerGdss[i+1] = layer.getGDSLayer();
            if (foundry != null)
                layerGdss[i+1] = foundry.getGDSLayer(layer);
			layerFuncs[i+1] = layer.getFunction().toString();
			layerCoverage[i+1] = TextUtils.formatDouble(layer.getAreaCoverage());
		}

		// write the layer information
		String [][] fields = new String[7][];
		fields[0] = layerNames;     fields[1] = layerColors;   fields[2] = layerStyles;
		fields[3] = layerCifs;      fields[4] = layerGdss;     fields[5] = layerFuncs;
		fields[6] = layerCoverage;
		dumpFields(fields, layerCount+1, "LAYERS IN " + tech.getTechName().toUpperCase());

		// ****************************** dump arcs ******************************

		// allocate space for all arc fields
		int tot = 1;
		for(Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); )
		{
			ArcProto ap = it.next();
			ArcInst ai = ArcInst.makeDummyInstance(ap, 4000);
			Poly [] polys = tech.getShapeOfArc(ai);
			tot += polys.length;
		}
		String [] arcNames = new String[tot];
		String [] arcLayers = new String[tot];
		String [] arcLayerSizes = new String[tot];
		String [] arcExtensions = new String[tot];
		String [] arcAngles = new String[tot];
		String [] arcWipes = new String[tot];
		String [] arcFuncs = new String[tot];
		String [] arcAntennas = new String[tot];

		// load the header
		arcNames[0] = "Arc";
		arcLayers[0] = "Layer";
		arcLayerSizes[0] = "Size";
		arcExtensions[0] = "Extend";
		arcAngles[0] = "Angle";
		arcWipes[0] = "Wipes";
		arcFuncs[0] = "Function";
		arcAntennas[0] = "Antenna";

		tot = 1;
		for(Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); )
		{
			ArcProto ap = it.next();
			arcNames[tot] = ap.getName();
			arcExtensions[tot] = (ap.isExtended() ? "yes" : "no");
			arcAngles[tot] = "" + ap.getAngleIncrement();
			arcWipes[tot] = (ap.isWipable() ? "yes" : "no");
			arcFuncs[tot] = ap.getFunction().toString();
			arcAntennas[tot] = TextUtils.formatDouble(ERC.getERCTool().getAntennaRatio(ap));

			ArcInst ai = ArcInst.makeDummyInstance(ap, 4000);
			ai.setExtended(ArcInst.HEADEND, false);
			ai.setExtended(ArcInst.TAILEND, false);
			Poly [] polys = tech.getShapeOfArc(ai);
			for(int k=0; k<polys.length; k++)
			{
				Poly poly = polys[k];
				arcLayers[tot] = poly.getLayer().getName();
				double area = poly.getArea() / ai.getLength();
				arcLayerSizes[tot] = TextUtils.formatDouble(area);
				if (k > 0)
				{
					arcNames[tot] = "";
					arcExtensions[tot] = "";
					arcAngles[tot] = "";
					arcWipes[tot] = "";
					arcFuncs[tot] = "";
				}
				tot++;
			}
		}

		// write the arc information
		fields = new String[8][];
		fields[0] = arcNames;        fields[1] = arcLayers;    fields[2] = arcLayerSizes;
		fields[3] = arcExtensions;   fields[4] = arcAngles;    fields[5] = arcWipes;
		fields[6] = arcFuncs;        fields[7] = arcAntennas;
		dumpFields(fields, tot, "ARCS IN " + tech.getTechName().toUpperCase());

		// ****************************** dump nodes ******************************

		// allocate space for all node fields
		int total = 1;
		for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = it.next();
			NodeInst ni = NodeInst.makeDummyInstance(np);
			Poly [] polys = tech.getShapeOfNode(ni);
			int l = 0;
			for(Iterator<PortProto> pIt = np.getPorts(); pIt.hasNext(); )
			{
				PrimitivePort pp = (PrimitivePort)pIt.next();
				int m = 0;
				ArcProto [] apArray = pp.getConnections();
				for(int k=0; k<apArray.length; k++)
					if (apArray[k].getTechnology() == tech) m++;
				if (m == 0) m = 1;
				l += m;
			}
			total += Math.max(polys.length, l);
		}
		String [] nodeNames = new String[total];
		String [] nodeFuncs = new String[total];
		String[] nodeLayers = new String[total];
		String [] nodeLayerSizes = new String[total];
		String [] nodePorts = new String[total];
		String [] nodePortSizes = new String[total];
		String [] nodePortAngles = new String[total];
		String [] nodeConnections = new String[total];

		// load the header
		nodeNames[0] = "Node";
		nodeFuncs[0] = "Function";
		nodeLayers[0] = "Layers";
		nodeLayerSizes[0] = "Size";
		nodePorts[0] = "Ports";
		nodePortSizes[0] = "Size";
		nodePortAngles[0] = "Angle";
		nodeConnections[0] = "Connections";

		tot = 1;
		for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = it.next();
			int base = tot;
			nodeNames[tot] = np.getName();
			nodeFuncs[tot] = np.getFunction().getName();

			NodeInst ni = NodeInst.makeDummyInstance(np);
			Poly [] polys = tech.getShapeOfNode(ni);
			for(int k=0; k<polys.length; k++)
			{
				Poly poly = polys[k];
				if (tot >= total)
				{
					System.out.println("ARRAY OVERFLOW: LIMIT IS " + total);
					break;
				}
				nodeLayers[tot] = poly.getLayer().getName();
				Rectangle2D polyBounds = poly.getBounds2D();
				nodeLayerSizes[tot] = polyBounds.getWidth() + " x " + polyBounds.getHeight();
				if (k > 0)
				{
					nodeNames[tot] = "";
					nodeFuncs[tot] = "";
				}
				tot++;
			}
			for(Iterator<PortProto> pIt = np.getPorts(); pIt.hasNext(); )
			{
				PrimitivePort pp = (PrimitivePort)pIt.next();
				nodePorts[base] = pp.getName();
				Poly portPoly = ni.getShapeOfPort(pp);
				Rectangle2D portRect = portPoly.getBounds2D();
				nodePortSizes[base] = portRect.getWidth() + " x " + portRect.getHeight();
				if (pp.getAngleRange() == 180) nodePortAngles[base] = ""; else
					nodePortAngles[base] = "" + pp.getAngle();
				int m = 0;
				ArcProto [] conList = pp.getConnections();
				for(int k=0; k<conList.length; k++)
				{
					if (conList[k].getTechnology() != tech) continue;
					nodeConnections[base] = conList[k].getName();
					if (m != 0)
					{
						nodePorts[base] = "";
						nodePortSizes[base] = "";
						nodePortAngles[base] = "";
					}
					m++;
					base++;
				}
				if (m == 0) nodeConnections[base++] = "<NONE>";
			}
			for( ; base < tot; base++)
			{
				nodePorts[base] = "";
				nodePortSizes[base] = "";
				nodePortAngles[base] = "";
				nodeConnections[base] = "";
			}
			for( ; tot < base; tot++)
			{
				nodeNames[tot] = "";
				nodeFuncs[tot] = "";
				nodeLayers[tot] = "";
				nodeLayerSizes[tot] = "";
			}
		}

		// write the node information */
		fields = new String[8][];
		fields[0] = nodeNames;        fields[1] = nodeFuncs;       fields[2] = nodeLayers;
		fields[3] = nodeLayerSizes;   fields[4] = nodePorts;       fields[5] = nodePortSizes;
		fields[6] = nodePortAngles;   fields[7] = nodeConnections;
		dumpFields(fields, tot, "NODES IN " + tech.getTechName().toUpperCase());
	}

	private static void dumpFields(String [][] fields, int length, String title)
	{
		int totWid = 0;
		int [] widths = new int[fields.length];
		for(int i=0; i<fields.length; i++)
		{
			widths[i] = 0;
			for(int j=0; j<length; j++)
			{
				if (fields[i][j] == null) continue;
				int len = fields[i][j].length();
				if (len > widths[i]) widths[i] = len;
			}
			widths[i] += 2;
			totWid += widths[i];
		}

		int stars = (totWid - title.length() - 4) / 2;
		for(int i=0; i<stars; i++) System.out.print("*");
		System.out.print(" " + title + " ");
		for(int i=0; i<stars; i++) System.out.print("*");
		System.out.println();

		for(int j=0; j<length; j++)
		{
			for(int i=0; i<fields.length; i++)
			{
				int len = 0;
				if (fields[i][j] != null)
				{
					System.out.print(fields[i][j]);
					len = fields[i][j].length();
				}
				if (i == fields.length-1) continue;
				for(int k=len; k<widths[i]; k++) System.out.print(" ");
			}
			System.out.println();

			if (j == 0)
			{
				// underline the header
				for(int i=0; i<fields.length; i++)
				{
					for(int k=2; k<widths[i]; k++) System.out.print("-");
					System.out.print("  ");
				}
				System.out.println();
			}
		}
		System.out.println();
	}

//	/**
//	 * the entry Method for all technology editing
//	 */
//	void us_tecedentry(INTBIG count, CHAR *par[])
//	{
//		if (count == 0)
//		{
//			ttyputusage(x_("technology edit OPTION"));
//			return;
//		}
//
//		l = estrlen(pp = par[0]);
//		if (namesamen(pp, x_("edit-design-rules"), l) == 0 && l >= 6)
//		{
//			us_teceditdrc();
//			return;
//		}
//		if (namesamen(pp, x_("dependent-libraries"), l) == 0 && l >= 2)
//		{
//			if (count < 2)
//			{
//				// display dependent library names
//				var = el_curlib.getVar(DEPENDENTLIB_KEY);
//				if (var == NOVARIABLE) ttyputmsg(_("There are no dependent libraries")); else
//				{
//					i = getlength(var);
//					ttyputmsg(_("%ld dependent %s:"), i, makeplural(x_("library"), i));
//					for(l=0; l<i; l++)
//					{
//						pp = ((CHAR **)var.addr)[l];
//						lib = getlibrary(pp);
//						ttyputmsg(x_("    %s%s"), pp, (lib == NOLIBRARY ? _(" (not read in)") : x_("")));
//					}
//				}
//				return;
//			}
//
//			// clear list if just "-" is given
//			if (count == 2 && estrcmp(par[1], x_("-")) == 0)
//			{
//				var = el_curlib.getVar(DEPENDENTLIB_KEY);
//				if (var != NOVARIABLE)
//					delval((INTBIG)el_curlib, VLIBRARY, DEPENDENTLIB_KEY);
//				return;
//			}
//
//			// create a list
//			dependentlist = (CHAR **)emalloc((count-1) * (sizeof (CHAR *)), el_tempcluster);
//			if (dependentlist == 0) return;
//			for(i=1; i<count; i++) dependentlist[i-1] = par[i];
//			el_curlib.newVar(DEPENDENTLIB_KEY, dependentlist);
//			efree((CHAR *)dependentlist);
//			return;
//		}
//		ttyputbadusage(x_("technology edit"));
//	}
//
//	/*
//	 * Routine for editing the DRC tables.
//	 */
//	void us_teceditdrc(void)
//	{
//		REGISTER INTBIG i, changed, nodecount;
//		NODEPROTO **nodesequence;
//		LIBRARY *liblist[1];
//		REGISTER VARIABLE *var;
//		REGISTER DRCRULES *rules;
//
//		// get the current list of layer and node names
//		getLayerNameList();
//		liblist[0] = el_curlib;
//		nodecount = findCellSequence(liblist, "node-", NODESEQUENCE_KEY);
//
//		// create a RULES structure
//		rules = dr_allocaterules(us_teceddrclayers, nodecount, x_("EDITED TECHNOLOGY"));
//		if (rules == NODRCRULES) return;
//		for(i=0; i<us_teceddrclayers; i++)
//			(void)allocstring(&rules.layernames[i], us_teceddrclayernames[i], el_tempcluster);
//		for(i=0; i<nodecount; i++)
//			(void)allocstring(&rules.nodenames[i], &nodesequence[i].protoname[5], el_tempcluster);
//		if (nodecount > 0) efree((CHAR *)nodesequence);
//
//		// get the text-list of design rules and convert them into arrays
//		var = getval((INTBIG)el_curlib, VLIBRARY, VSTRING|VISARRAY, x_("EDTEC_DRC"));
//		us_teceditgetdrcarrays(var, rules);
//
//		// edit the design-rule arrays
//		changed = dr_rulesdlog(NOTECHNOLOGY, rules);
//
//		// if changes were made, convert the arrays back into a text-list
//		if (changed != 0)
//		{
//			us_tecedloaddrcmessage(rules, el_curlib);
//		}
//
//		// free the arrays
//		dr_freerules(rules);
//	}
//
//	/*
//	 * Routine to create arrays describing the design rules in the variable "var" (which is
//	 * from "EDTEC_DRC" on a library).  The arrays are stored in "rules".
//	 */
//	void us_teceditgetdrcarrays(VARIABLE *var, DRCRULES *rules)
//	{
//		REGISTER INTBIG i, l;
//		INTBIG amt;
//		BOOLEAN connected, wide, multi, edge;
//		INTBIG widrule, layer1, layer2, j;
//		REGISTER CHAR *str, *pt;
//		CHAR *rule;
//
//		// get the design rules
//		if (var == NOVARIABLE) return;
//
//		l = getlength(var);
//		for(i=0; i<l; i++)
//		{
//			// parse the DRC rule
//			str = ((CHAR **)var.addr)[i];
//			while (*str == ' ') str++;
//			if (*str == 0) continue;
//
//			// special case for node minimum size rule
//			if (*str == 'n')
//			{
//				str++;
//				for(pt = str; *pt != 0; pt++) if (*pt == ' ') break;
//				if (*pt == 0)
//				{
//					ttyputmsg(_("Bad node size rule (line %ld): %s"), i+1, str);
//					continue;
//				}
//				*pt = 0;
//				for(j=0; j<rules.numnodes; j++)
//					if (namesame(str, rules.nodenames[j]) == 0) break;
//				*pt = ' ';
//				if (j >= rules.numnodes)
//				{
//					ttyputmsg(_("Unknown node (line %ld): %s"), i+1, str);
//					continue;
//				}
//				while (*pt == ' ') pt++;
//				rules.minnodesize[j*2] = atofr(pt);
//				while (*pt != 0 && *pt != ' ') pt++;
//				while (*pt == ' ') pt++;
//				rules.minnodesize[j*2+1] = atofr(pt);
//				while (*pt != 0 && *pt != ' ') pt++;
//				while (*pt == ' ') pt++;
//				if (*pt != 0) reallocstring(&rules.minnodesizeR[j], pt, el_tempcluster);
//				continue;
//			}
//
//			// parse the layer rule
//			if (us_tecedgetdrc(str, &connected, &wide, &multi, &widrule, &edge,
//				&amt, &layer1, &layer2, &rule, rules.numlayers, rules.layernames))
//			{
//				ttyputmsg(_("DRC line %ld is: %s"), i+1, str);
//				continue;
//			}
//
//			// set the layer spacing
//			if (widrule == 1)
//			{
//				rules.minwidth[layer1] = amt;
//				if (*rule != 0)
//					(void)reallocstring(&rules.minwidthR[layer1], rule, el_tempcluster);
//			} else if (widrule == 2)
//			{
//				rules.widelimit = amt;
//			} else
//			{
//				if (layer1 > layer2) { j = layer1;  layer1 = layer2;  layer2 = j; }
//				j = (layer1+1) * (layer1/2) + (layer1&1) * ((layer1+1)/2);
//				j = layer2 + rules.numlayers * layer1 - j;
//				if (edge)
//				{
//					rules.edgelist[j] = amt;
//					if (*rule != 0)
//						(void)reallocstring(&rules.edgelistR[j], rule, el_tempcluster);
//				} else if (wide)
//				{
//					if (connected)
//					{
//						rules.conlistW[j] = amt;
//						if (*rule != 0)
//							(void)reallocstring(&rules.conlistWR[j], rule, el_tempcluster);
//					} else
//					{
//						rules.unconlistW[j] = amt;
//						if (*rule != 0)
//							(void)reallocstring(&rules.unconlistWR[j], rule, el_tempcluster);
//					}
//				} else if (multi)
//				{
//					if (connected)
//					{
//						rules.conlistM[j] = amt;
//						if (*rule != 0)
//							(void)reallocstring(&rules.conlistMR[j], rule, el_tempcluster);
//					} else
//					{
//						rules.unconlistM[j] = amt;
//						if (*rule != 0)
//							(void)reallocstring(&rules.unconlistMR[j], rule, el_tempcluster);
//					}
//				} else
//				{
//					if (connected)
//					{
//						rules.conlist[j] = amt;
//						if (*rule != 0)
//							(void)reallocstring(&rules.conlistR[j], rule, el_tempcluster);
//					} else
//					{
//						rules.unconlist[j] = amt;
//						if (*rule != 0)
//							(void)reallocstring(&rules.unconlistR[j], rule, el_tempcluster);
//					}
//				}
//			}
//		}
//	}
//
//	/*
//	 * routine to parse DRC line "str" and fill the factors "connected" (set nonzero
//	 * if rule is for connected layers), "amt" (rule distance), "layer1" and "layer2"
//	 * (the layers).  Presumes that there are "maxlayers" layer names in the
//	 * array "layernames".  Returns true on error.
//	 */
//	BOOLEAN us_tecedgetdrc(CHAR *str, BOOLEAN *connected, BOOLEAN *wide, BOOLEAN *multi, INTBIG *widrule,
//		BOOLEAN *edge, INTBIG *amt, INTBIG *layer1, INTBIG *layer2, CHAR **rule, INTBIG maxlayers,
//		CHAR **layernames)
//	{
//		REGISTER CHAR *pt;
//		REGISTER INTBIG save;
//
//		*connected = *wide = *multi = *edge = FALSE;
//		for( ; *str != 0; str++)
//		{
//			if (tolower(*str) == 'c')
//			{
//				*connected = TRUE;
//				continue;
//			}
//			if (tolower(*str) == 'w')
//			{
//				*wide = TRUE;
//				continue;
//			}
//			if (tolower(*str) == 'm')
//			{
//				*multi = TRUE;
//				continue;
//			}
//			if (tolower(*str) == 'e')
//			{
//				*edge = TRUE;
//				continue;
//			}
//			break;
//		}
//		*widrule = 0;
//		if (tolower(*str) == 's')
//		{
//			*widrule = 1;
//			str++;
//		} else if (tolower(*str) == 'l')
//		{
//			*widrule = 2;
//			str++;
//		}
//
//		// get the distance
//		pt = str;
//		while (*pt != 0 && *pt != ' ' && *pt != '\t') pt++;
//		while (*pt == ' ' || *pt == '\t') pt++;
//		*amt = atofr(str);
//
//		// get the first layer
//		if (*widrule != 2)
//		{
//			str = pt;
//			if (*str == 0)
//			{
//				ttyputerr(_("Cannot find layer names on DRC line"));
//				return(TRUE);
//			}
//			while (*pt != 0 && *pt != ' ' && *pt != '\t') pt++;
//			if (*pt == 0)
//			{
//				ttyputerr(_("Cannot find layer name on DRC line"));
//				return(TRUE);
//			}
//			save = *pt;
//			*pt = 0;
//			for(*layer1 = 0; *layer1 < maxlayers; (*layer1)++)
//				if (namesame(str, layernames[*layer1]) == 0) break;
//			*pt++ = (CHAR)save;
//			if (*layer1 >= maxlayers)
//			{
//				ttyputerr(_("First DRC layer name unknown"));
//				return(TRUE);
//			}
//			while (*pt == ' ' || *pt == '\t') pt++;
//		}
//
//		// get the second layer
//		if (*widrule == 0)
//		{
//			str = pt;
//			while (*pt != 0 && *pt != ' ' && *pt != '\t') pt++;
//			save = *pt;
//			*pt = 0;
//			for(*layer2 = 0; *layer2 < maxlayers; (*layer2)++)
//				if (namesame(str, layernames[*layer2]) == 0) break;
//			*pt = (CHAR)save;
//			if (*layer2 >= maxlayers)
//			{
//				ttyputerr(_("Second DRC layer name unknown"));
//				return(TRUE);
//			}
//		}
//
//		while (*pt == ' ' || *pt == '\t') pt++;
//		*rule = pt;
//		return(FALSE);
//	}

//	/**
//	 * Method to examine the arrays describing the design rules and create
//	 * the variable "EDTEC_DRC" on library "lib".
//	 */
//	void us_tecedloaddrcmessage(DRCRules rules, Library lib)
//	{
//		// load the arrays
//		List drclist = new ArrayList();
//
//		// write the minimum width for each layer
//		for(i=0; i<rules.numlayers; i++)
//		{
//	        DRCTemplate lr = drRules.getMinValue(layer, DRCTemplate.MINWID, foundry.techMode);
//	        if (lr == null) continue;
//			String ruleMsg = "s" + lr.value1 + " " + layer.getName() + " " + lr.ruleName;
//			drclist.add(ruleMsg);
//		}
//
//		// write the minimum size for each node
//		for(i=0; i<rules.numnodes; i++)
//		{
//			if (rules.minnodesize[i*2] <= 0 && rules.minnodesize[i*2+1] <= 0) continue;
//			{
//				String ruleMsg = "n" + rules.nodenames[i] + " " + rules.minnodesize[i*2] + " " +
//					rules.minnodesize[i*2+1] + " " + rules.minnodesizeR[i];
//				drclist.add(ruleMsg);
//			}
//		}
//
//		// now do the distance rules
//		k = 0;
//		for(i=0; i<rules.numlayers; i++) for(j=i; j<rules.numlayers; j++)
//		{
//			if (rules.conlist[k] >= 0)
//			{
//				infstr = initinfstr();
//				formatinfstr(infstr, x_("c%s %s %s %s"), frtoa(rules.conlist[k]),
//					rules.layernames[i], rules.layernames[j],
//						rules.conlistR[k]);
//				drclist.add(ruleMsg);
//			}
//			if (rules.unconlist[k] >= 0)
//			{
//				infstr = initinfstr();
//				formatinfstr(infstr, x_("%s %s %s %s"), frtoa(rules.unconlist[k]),
//					rules.layernames[i], rules.layernames[j],
//						rules.unconlistR[k]);
//				drclist.add(ruleMsg);
//			}
//			if (rules.conlistW[k] >= 0)
//			{
//				formatinfstr(infstr, x_("cw%s %s %s %s"), frtoa(rules.conlistW[k]),
//					rules.layernames[i], rules.layernames[j],
//						rules.conlistWR[k]);
//				drclist.add(ruleMsg);
//			}
//			if (rules.unconlistW[k] >= 0)
//			{
//				formatinfstr(infstr, x_("w%s %s %s %s"), frtoa(rules.unconlistW[k]),
//					rules.layernames[i], rules.layernames[j],
//						rules.unconlistWR[k]);
//				drclist.add(ruleMsg);
//			}
//			if (rules.conlistM[k] >= 0)
//			{
//				formatinfstr(infstr, x_("cm%s %s %s %s"), frtoa(rules.conlistM[k]),
//					rules.layernames[i], rules.layernames[j],
//						rules.conlistMR[k]);
//				drclist.add(ruleMsg);
//			}
//			if (rules.unconlistM[k] >= 0)
//			{
//				formatinfstr(infstr, x_("m%s %s %s %s"), frtoa(rules.unconlistM[k]),
//					rules.layernames[i], rules.layernames[j],
//						rules.unconlistMR[k]);
//				drclist.add(ruleMsg);
//			}
//			if (rules.edgelist[k] >= 0)
//			{
//				formatinfstr(infstr, x_("e%s %s %s %s"), frtoa(rules.edgelist[k]),
//					rules.layernames[i], rules.layernames[j],
//						rules.edgelistR[k]);
//				drclist.add(ruleMsg);
//			}
//			k++;
//		}
//
//		if (drclist.size() == 0)
//		{
//			// no rules: remove the variable
//			if (lib.getVal("EDTEC_DRC") != null)
//				lib.delVal("EDTEC_DRC");
//		} else
//		{
//			lib.newVal("EDTEC_DRC", drclist);
//		}
//	}
}
