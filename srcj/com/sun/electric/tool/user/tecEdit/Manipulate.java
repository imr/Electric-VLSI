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

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Xml;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.extract.LayerCoverageTool;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.ComponentMenu;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.PromptAt;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
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
					Cell varCell = getLayerCell(ni);
//					Variable var = ni.getVar(Info.LAYER_KEY);
//					if (var == null) continue;
//					CellId cID = (CellId)var.getObject();
//					Cell varCell = EDatabase.serverDatabase().getCell(cID);
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
		if (np == Generic.tech().portNode && !cell.getName().startsWith("node-"))
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
				cellName = "layer-" + layerName + "{lay}";
				break;
			case 2:		// arc
				String arcName = JOptionPane.showInputDialog("Name of new arc:", "");
				if (arcName == null) return;
				cellName = "arc-" + arcName + "{lay}";
				break;
			case 3:		// node
				String nodeName = JOptionPane.showInputDialog("Name of new node:", "");
				if (nodeName == null) return;
				cellName = "node-" + nodeName + "{lay}";
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
		new MakeOneCellJob(lib, cellName, type);
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
			newCell.setTechnology(Artwork.tech());

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
	public static void completeNodeCreation(NodeInst newNi, Variable v)
	{
		// postprocessing on the nodes
		String portName = null;
		if (newNi.getProto() == Generic.tech().portNode)
		{
			// a port layer
			portName = JOptionPane.showInputDialog("Port name:", "");
			if (portName == null) return;
		}
		boolean isHighlight = false;
		if (v != null)
		{
			if (v.getObject() instanceof Integer &&
				((Integer)v.getObject()).intValue() == Info.HIGHLIGHTOBJ)
			{
				isHighlight = true;
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
			if (newNi.getProto() == Generic.tech().portNode)
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
		new EditDependentLibraries();
	}

	/**
	 * Method to edit the component menu for the technology.
	 */
	public static void editComponentMenu()
	{
		// get information about arcs and nodes in the technology being edited
		Library [] dependentlibs = Info.getDependentLibraries(Library.getCurrent());
		Cell [] arcCells = Info.findCellSequence(dependentlibs, "arc-", Info.ARCSEQUENCE_KEY);
		Cell [] nodeCells = Info.findCellSequence(dependentlibs, "node-", Info.NODESEQUENCE_KEY);

		// get the XML string describing the component menu
		String compMenuXML;
		Variable var = Library.getCurrent().getVar(Info.COMPMENU_KEY);
		if (var == null)
		{
			// construct a default component menu
			List<Object> things = new ArrayList<Object>();

			// add in arcs
			for(int i=0; i<arcCells.length; i++)
			{
				String arcName = arcCells[i].getName().substring(4);
				Xml.ArcProto curArc = new Xml.ArcProto();
                curArc.name = arcName;
				things.add(curArc);
			}

			// add in nodes
			for(int i=0; i<nodeCells.length; i++)
			{
				Cell np = nodeCells[i];
				NodeInfo nIn = NodeInfo.parseCell(np);
				if (nIn.func == PrimitiveNode.Function.NODE) continue;
				String nodeName = nodeCells[i].getName().substring(5);

//				// see if there are custom overrides
//				if (nIn.surroundOverrides != null)
//				{
//            		List<Object> tmp = new ArrayList<Object>();
//            		for(int j=0; j<nIn.surroundOverrides.length; j++)
//	            	{
//            			int commaPos = nIn.surroundOverrides[j].indexOf(',');
//            			if (commaPos < 0) continue;
//            			Xml.MenuNodeInst xni = new MenuNodeInst();
//            			xni.protoName = nodeName;
//            			xni.function = nIn.func;
//            			xni.text = nodeName + "-" + nIn.surroundOverrides[j].substring(0, commaPos);
//            			xni.fontSize = 5;
//            			tmp.add(xni);
//            		}
//    				things.add(tmp);
//				} else
            	{
    				Xml.PrimitiveNode curNode = new Xml.PrimitiveNode();
                    curNode.name = nodeName;
                    curNode.function = nIn.func;
    				things.add(curNode);
            	}
			}

			// add in special menu entries
			things.add(Technology.SPECIALMENUPURE);
			things.add(Technology.SPECIALMENUMISC);
			things.add(Technology.SPECIALMENUCELL);

			// construct the menu information
			int columns = (things.size()+13) / 14;
			Xml.MenuPalette xmp = new Xml.MenuPalette();
			xmp.numColumns = columns;
			xmp.menuBoxes = new ArrayList<List<?>>();
			for(Object item : things)
			{
				if (item instanceof List) xmp.menuBoxes.add((List)item); else
				{
					List<Object> subList = new ArrayList<Object>();
					subList.add(item);
					xmp.menuBoxes.add(subList);
				}
			}

			compMenuXML = xmp.writeXml();
		} else
		{
			compMenuXML = (String)var.getObject();
		}
	    List<Xml.PrimitiveNodeGroup> nodeGroups = new ArrayList<Xml.PrimitiveNodeGroup>();
		for(int i=0; i<nodeCells.length; i++)
		{
			Xml.PrimitiveNodeGroup ng = new Xml.PrimitiveNodeGroup();
            Xml.PrimitiveNode n = new Xml.PrimitiveNode();
            ng.nodes.add(n);
			n.name = nodeCells[i].getName().substring(5);
			NodeInfo nIn = NodeInfo.parseCell(nodeCells[i]);
			n.function = nIn.func;
			nodeGroups.add(ng);
		}
	    List<Xml.ArcProto> arcs = new ArrayList<Xml.ArcProto>();
		for(int i=0; i<arcCells.length; i++)
		{
			Xml.ArcProto xap = new Xml.ArcProto();
			xap.name = arcCells[i].getName().substring(4);
			arcs.add(xap);
		}
	    Xml.MenuPalette xmp = Xml.parseComponentMenuXMLTechEdit(compMenuXML, nodeGroups, arcs);
	    ComponentMenu.showComponentMenuDialog(Library.getCurrent().getName(), xmp, nodeGroups, arcs);
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
			gbc.insets = new Insets(4, 4, 4, 4);
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
			gbc.insets = new Insets(4, 4, 4, 4);
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
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(lab2, gbc);

			JLabel lab3 = new JLabel("Libraries are examined from bottom up");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 6;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(lab3, gbc);

			// center column
			JButton remove = new JButton("Remove");
			gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = 1;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(remove, gbc);
			remove.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { removeLib(); }
			});

			JButton add = new JButton("<< Add");
			gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = 2;
			gbc.insets = new Insets(4, 4, 4, 4);
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
			gbc.insets = new Insets(4, 4, 4, 4);
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
			gbc.insets = new Insets(4, 4, 4, 4);
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
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(lab5, gbc);

			libToAdd = new JTextField("");
			gbc = new GridBagConstraints();
			gbc.gridx = 2;   gbc.gridy = 4;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(libToAdd, gbc);

			// OK and Cancel
			JButton cancel = new JButton("Cancel");
			gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = 6;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(cancel, gbc);
			cancel.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { exit(false); }
			});

			JButton ok = new JButton("OK");
			getRootPane().setDefaultButton(ok);
			gbc = new GridBagConstraints();
			gbc.gridx = 2;
			gbc.gridy = 6;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(ok, gbc);
			ok.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { exit(true); }
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
		List<Example> neList = null;
		TechConversionResult tcr = new TechConversionResult();
		if (np.getName().startsWith("node-"))
			neList = Example.getExamples(np, true, tcr, null); else
				neList = Example.getExamples(np, false, tcr, null);
   		if (tcr.failed()) tcr.showError();
		if (neList == null || neList.size() == 0) return;
		Example firstEx = neList.get(0);

		// count the number of appropriate samples in the main example
		int total = 0;
		for(Sample ns : firstEx.samples)
		{
			if (!doPorts)
			{
				if (ns.layer != Generic.tech().portNode) total++;
			} else
			{
				if (ns.layer == Generic.tech().portNode) total++;
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
		for(Sample ns : firstEx.samples)
		{
			if (!doPorts)
			{
				if (ns.layer != Generic.tech().portNode) whichSam[k++] = ns;
			} else
			{
				if (ns.layer == Generic.tech().portNode) whichSam[k++] = ns;
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
			} else if (ns.layer == Generic.tech().cellCenterNode)
			{
				msg = "GRAB";
			} else if (ns.layer == Generic.tech().portNode)
			{
				msg = Info.getPortName(ns.node);
				if (msg == null) msg = "?";
			} else msg = ns.layer.getName().substring(6);
			Point2D curPt = new Point2D.Double(xPos[i], yPos[i]);
			highlighter.addMessage(np, msg, curPt);

            Rectangle2D nodeBounds = ns.node.getBaseShape().getBounds2D();
			Point2D other = null;
			if (style[i] == Poly.Type.TEXTLEFT)
			{
				other = new Point2D.Double(nodeBounds.getMinX(), nodeBounds.getCenterY());
			} else if (style[i] == Poly.Type.TEXTRIGHT)
			{
				other = new Point2D.Double(nodeBounds.getMaxX(), nodeBounds.getCenterY());
			} else if (style[i] == Poly.Type.TEXTTOP)
			{
				other = new Point2D.Double(nodeBounds.getCenterX(), nodeBounds.getMaxY());
			} else if (style[i] == Poly.Type.TEXTBOT)
			{
				other = new Point2D.Double(nodeBounds.getCenterX(), nodeBounds.getMinY());
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
			if (ai.getProto() != Generic.tech().universal_arc)
				return "This is an unimportant " + ai.getProto();
			if (ai.getHeadPortInst().getNodeInst().getProto() != Generic.tech().portNode ||
				ai.getTailPortInst().getNodeInst().getProto() != Generic.tech().portNode)
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
			case Info.TECHSHORTNAME:
				return "The technology name";
			case Info.TECHSCALE:
				return "The technology scale";
			case Info.TECHFOUNDRY:
				return "The technology's foundry";
			case Info.TECHDEFMETALS:
				return "The number of metal layers in the technology";
			case Info.TECHDESCRIPT:
				return "The technology description";
			case Info.TECHSPICEMINRES:
				return "Minimum resistance of SPICE elements";
			case Info.TECHSPICEMINCAP:
				return "Minimum capacitance of SPICE elements";
			case Info.TECHMAXSERIESRES:
				return "Maximum series resistance of SPICE elements";
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
			case Info.ARCWIDTHOFFSET:
				return "The ELIB width offset for " + cell;

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
			case Info.NODESPICETEMPLATE:
				return "Spice template for " + cell;

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
		Cell clientCell = EDatabase.clientDatabase().getCell(cID);
		Cell cell = EDatabase.serverDatabase().getCell(cID);
		if (clientCell != null || cell != null)
		{
			// validate the reference
			for(Iterator<Cell> it = ni.getParent().getLibrary().getCells(); it.hasNext(); )
			{
				Cell oCell = it.next();
				if (oCell == cell || oCell == clientCell) return oCell;
			}
		}
        System.out.println("Layer " + cID.cellName + " not found");
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
			case Info.TECHSHORTNAME:     modTechShortName(wnd, ni);          break;
			case Info.TECHSCALE:         modTechScale(wnd, ni);              break;
			case Info.TECHFOUNDRY:       modTechFoundry(wnd, ni);            break;
			case Info.TECHDEFMETALS:     modTechNumMetals(wnd, ni);          break;
			case Info.TECHDESCRIPT:      modTechDescription(wnd, ni);        break;
			case Info.TECHSPICEMINRES:   modTechMinResistance(wnd, ni);      break;
			case Info.TECHSPICEMINCAP:   modTechMinCapacitance(wnd, ni);     break;
			case Info.TECHMAXSERIESRES:  modTechMaxSeriesResistance(wnd, ni);break;
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
			case Info.ARCWIDTHOFFSET:    modArcWidthOffset(wnd, ni);         break;

			case Info.NODEFUNCTION:      modNodeFunction(wnd, ni);           break;
			case Info.NODELOCKABLE:      modNodeLockability(wnd, ni);        break;
			case Info.NODESERPENTINE:    modNodeSerpentine(wnd, ni);         break;
			case Info.NODESQUARE:        modNodeSquare(wnd, ni);             break;
			case Info.NODEWIPES:         modNodeWipes(wnd, ni);              break;
			case Info.NODESPICETEMPLATE: modNodeSpiceTemplate(wnd, ni);      break;

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

	private static void modTechMaxSeriesResistance(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		String newUnit = PromptAt.showPromptAt(wnd, ni, "Change Maximum Series Resistance",
			"Maximum Series Resistance (for parasitics):", initialMsg);
		if (newUnit != null) new SetTextJob(ni, "Maximum Series Resistance: " + newUnit);
	}

	private static void modArcAntennaRatio(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		String newUnit = PromptAt.showPromptAt(wnd, ni, "Change Antenna Ratio",
			"Maximum antenna ratio for this layer:", initialMsg);
		if (newUnit != null) new SetTextJob(ni, "Antenna Ratio: " + newUnit);
	}

	private static void modArcWidthOffset(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		String newUnit = PromptAt.showPromptAt(wnd, ni, "Change Width Offset",
			"ELIB width offset for this arc:", initialMsg);
		if (newUnit != null) new SetTextJob(ni, "ELIB width offset: " + newUnit);
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
		Color [] colors = GeneralInfo.getTransparentColors(ni);
		if (colors == null) return;
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
				new SetTransparentColorJob(ni, GeneralInfo.makeTransparentColorsLine(colors));

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
		transNames[1] = "layer-1";
		transNames[2] = "layer-2";
		transNames[3] = "layer-3";
		transNames[4] = "layer-4";
		transNames[5] = "layer-5";
		transNames[6] = "layer-6";
		transNames[7] = "layer-7";
		transNames[8] = "layer-8";
		transNames[9] = "layer-9";
		transNames[10] = "layer-10";
		String choice = PromptAt.showPromptAt(wnd, ni, "Change Transparent Layer",
			"New transparent layer number for this layer:", initialTransLayer, transNames);
		if (choice == null) return;
		new SetTextJob(ni, "Transparency: " + choice);

		// redraw the demo layer in this cell
		new RedoLayerGraphicsJob(ni.getParent());
	}

	private static final String PRINTSOLID = "PRINTER: Solid";
	private static final String PRINTPATTERNED = "PRINTER: Patterned";

	private static void modLayerStyle(EditWindow wnd, NodeInst ni)
	{
		String initialStyleName = Info.getValueOnNode(ni);
		String printerPart;
		int commaPos = initialStyleName.indexOf(',');
		if (commaPos < 0) printerPart = ""; else
		{
			printerPart = initialStyleName.substring(commaPos);
			initialStyleName = initialStyleName.substring(0, commaPos);
		}
		List<EGraphics.Outline> outlines = EGraphics.Outline.getOutlines();
		String [] styleNames = new String[outlines.size()+3];
		styleNames[0] = "Solid";
		int i = 1;
		for(EGraphics.Outline o : outlines)
		{
			styleNames[i++] = "Patterned/Outline=" + o.getName();
		}
		styleNames[i++] = PRINTSOLID;
		styleNames[i++] = PRINTPATTERNED;
		String choice = PromptAt.showPromptAt(wnd, ni, "Change Layer Drawing Style",
			"New drawing style for this layer:", initialStyleName, styleNames);
		if (choice == null) return;
		if (choice.equals(PRINTSOLID))
		{
			choice = initialStyleName + ",PrintSolid";
		} else if (choice.equals(PRINTPATTERNED))
		{
			choice = initialStyleName;
		} else choice += printerPart;
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
		if (ni.getProto() == Artwork.tech().boxNode) return 0;
		if (ni.getProto() != Artwork.tech().filledBoxNode) return 0;
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
			if (ni.getProto() == Artwork.tech().boxNode)
			{
				if (color == 0) return true;
				ni.replace(Artwork.tech().filledBoxNode, false, false);
			} else if (ni.getProto() == Artwork.tech().filledBoxNode)
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
		Cell cell = getLayerCell(ni);
		if (cell != null) initial = cell.getName().substring(6);
		String choice = PromptAt.showPromptAt(wnd, ni, "Change Layer", "New layer for this geometry:", initial, options);
		if (choice == null) return;

		// save the results
		new ModifyLayerJob(ni, choice, layerCells);
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
				ni.newDisplayVar(Info.MINSIZEBOX_KEY, "MIN");
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
					if (li != null)
					{
						setPatch(ni, li.desc);
						ni.newVar(Info.LAYER_KEY, layerCells[i].getId());
					}
					return true;
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
		Set<NodeProto> connectSet = new HashSet<NodeProto>();
		Variable var = ni.getVar(Info.CONNECTION_KEY);
		if (var != null)
		{
			CellId [] connects = (CellId [])var.getObject();
			for(int i=0; i<connects.length; i++)
			{
				if (connects[i] != null)
					connectSet.add(EDatabase.clientDatabase().getCell(connects[i]));
			}
		}

		// build an array of arc connections
		PromptAt.Field [] fields = new PromptAt.Field[allArcs.size()+3];
		for(int i=0; i<allArcs.size(); i++)
		{
			Cell cell = allArcs.get(i);
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
		Variable meaningVar = ni.getVar(Info.PORTMEANING_KEY);
		int meaning = 0;
		if (meaningVar != null) meaning = ((Integer)meaningVar.getObject()).intValue();
		fields[allArcs.size()] = new PromptAt.Field("Angle:", TextUtils.formatDouble(ang));
		fields[allArcs.size()+1] = new PromptAt.Field("Angle range:", TextUtils.formatDouble(range));
		String[] meanings = new String[]{"No meaning", "Gate", "Gated"};
		fields[allArcs.size()+2] = new PromptAt.Field("Transistor meaning:", meanings, meanings[meaning]);
		String choice = PromptAt.showPromptAt(wnd, ni, "Change Port", fields);
		if (choice == null) return;

		// save the results
		String [] fieldValues = new String[fields.length];
		for(int i=0; i<fields.length; i++)
			fieldValues[i] = (String)fields[i].getFinal();
		new ModifyPortJob(ni, allArcs, fieldValues);
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
			CellId [] newConnects = new CellId[numConnects];
			int k = 0;
			for(int i=0; i<allArcs.size(); i++)
			{
				String answer = fieldValues[i];
				if (answer.equals("Allowed")) newConnects[k++] = allArcs.get(i).getId();
			}
			ni.newVar(Info.CONNECTION_KEY, newConnects);

			int newAngle = TextUtils.atoi(fieldValues[allArcs.size()]);
			ni.newVar(Info.PORTANGLE_KEY, new Integer(newAngle));
			int newRange = TextUtils.atoi(fieldValues[allArcs.size()+1]);
			ni.newVar(Info.PORTRANGE_KEY, new Integer(newRange));
			String newMeaning = fieldValues[allArcs.size()+2];
			int meaning = 0;
			if (newMeaning.equals("Gate")) meaning = 1; else
				if (newMeaning.equals("Gated")) meaning = 2;
			ni.newVar(Info.PORTMEANING_KEY, new Integer(meaning));
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
			ArcProto.Function fun = funs.get(i);
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
			PrimitiveNode.Function fun = funs.get(i);
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

	private static void modNodeSpiceTemplate(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		String newST = PromptAt.showPromptAt(wnd, ni, "Change Spice Template",
			"New Spice Template for this node:", initialMsg);
		if (newST != null) new SetTextJob(ni, "Spice Template: " + newST);
	}

//	private static void modNodeCustomOverrides(EditWindow wnd, NodeInst ni)
//	{
//		Variable var = ni.getVar(Artwork.ART_MESSAGE);
//		if (var == null) return;
//		String [] lines = (String[])var.getObject();
//
//		PromptAt.Field [] fields = new PromptAt.Field[lines.length+1];
//		int j = 0;
//		for(int i=1; i<lines.length; i++)
//		{
//			fields[j] = new PromptAt.Field("Override " + (j+1) + ":", lines[i]);
//			j++;
//		}
//		for(int i=0; i<2; i++)
//		{
//			fields[j] = new PromptAt.Field("New Override " + (j+1) + ":", "");
//			j++;
//		}
//		String choice = PromptAt.showPromptAt(wnd, ni, "Adjust Custom Overrides", fields);
//		if (choice == null) return;
//
//		j = 0;
//		for(int i=0; i<=lines.length; i++)
//			if (((String)fields[i].getFinal()).trim().length() > 0) j++;
//		String [] newLines = new String[j+1];
//		newLines[0] = lines[0];
//		j = 1;
//		for(int i=0; i<=lines.length; i++)
//		{
//			String str = (String)fields[i].getFinal();
//			if (str.trim().length() > 0) newLines[j++] = str.trim();
//		}
//		new SetTextJob(ni, newLines);
//	}

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

	private static void modTechShortName(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		String newUnit = PromptAt.showPromptAt(wnd, ni, "Set Short Name",
			"The Short Name of this technology:", initialMsg);
		if (newUnit != null) new SetTextJob(ni, "Short Name: " + newUnit);
	}

	private static void modTechScale(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		String newUnit = PromptAt.showPromptAt(wnd, ni, "Set Unit Size",
			"The scale of this technology (nanometers per grid unit):", initialMsg);
		if (newUnit != null) new SetTextJob(ni, "Lambda: " + newUnit);
	}

	private static void modTechFoundry(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		String newUnit = PromptAt.showPromptAt(wnd, ni, "Set Default Foundry",
			"The default foundry for this technology:", initialMsg);
		if (newUnit != null) new SetTextJob(ni, "Foundry: " + newUnit);
	}

	private static void modTechNumMetals(EditWindow wnd, NodeInst ni)
	{
		String initialMsg = Info.getValueOnNode(ni);
		String newDesc = PromptAt.showPromptAt(wnd, ni, "Set Number of Metal Layers",
			"Number of Metal Layers in this technology:", initialMsg);
		if (newDesc != null) new SetTextJob(ni, "Number of Metal Layers: " + newDesc);
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
		private Object chr;

		private SetTextJob(NodeInst ni, Object chr)
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
				if (ni.getProto() != Artwork.tech().filledBoxNode) continue;
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
					Cell varCell = getLayerCell(cNi);
//					Variable varLay = cNi.getVar(Info.LAYER_KEY);
//					if (varLay == null) continue;
//					CellId cID = (CellId)varLay.getObject();
//					Cell varCell = EDatabase.serverDatabase().getCell(cID);
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
			if (ni.getProto() == Artwork.tech().boxNode || ni.getProto() == Artwork.tech().filledBoxNode)
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
			if (ni.getProto() != Artwork.tech().boxNode && ni.getProto() != Artwork.tech().filledBoxNode) continue;
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
		if (ni.getProto() == Generic.tech().portNode) return Info.PORTOBJ;

		// center objects are also readily identifiable
		if (ni.getProto() == Generic.tech().cellCenterNode) return Info.CENTEROBJ;

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
		new RearrangeOrder(type);
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
		private int startItem;
		private int endItem;
		private boolean endBefore;
		private int endLineHighlightBefore;

		/** Creates new form Rearrange technology components */
		private RearrangeOrder(int type)
		{
			super(null, true);
			this.type = type;
			lib = Library.getCurrent();

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
			getContentPane().setLayout(new GridBagLayout());

			JLabel title = new JLabel("Drag to reorganize the list");
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;      gbc.gridy = 0;
			gbc.gridwidth = 2;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(title, gbc);

			JScrollPane center = new JScrollPane();
			center.setPreferredSize(new Dimension(300, 150));
			gbc = new GridBagConstraints();
			gbc.gridx = 0;      gbc.gridy = 1;
			gbc.weightx = 1;    gbc.weighty = 1;
			gbc.gridwidth = 2;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(center, gbc);

			model = new DefaultListModel();
			list = new JList(model);
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			center.setViewportView(list);
			DragListener dl = new DragListener();
			list.addMouseListener(dl);
			list.addMouseMotionListener(dl);
			list.setCellRenderer(new MyCellRenderer());

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
			list.setSelectedIndex(-1);

			// OK and Cancel
			JButton cancel = new JButton("Cancel");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 4;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(cancel, gbc);
			cancel.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { exit(false); }
			});

			JButton ok = new JButton("OK");
			getRootPane().setDefaultButton(ok);
			gbc = new GridBagConstraints();
			gbc.gridx = 1;   gbc.gridy = 4;
			gbc.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(ok, gbc);
			ok.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { exit(true); }
			});

			pack();
			setVisible(true);
		}

		protected void escapePressed() { exit(false); }

		/**
		 * Method to handle the OK button
		 */
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

		/**
		 * Class to handle saving the new orderings in a Job.
		 */
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
		 * Class to handle selection display in the list.
		 * Disables normal selection and instead shows only what is being dragged.
		 */
		private class MyCellRenderer extends JLabel implements ListCellRenderer
		{
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
			{
				String s = value.toString();
				setText(s);
				if (index == startItem)
				{
					setBackground(list.getForeground());
					setForeground(list.getBackground());
				} else
				{
					setBackground(list.getBackground());
					setForeground(list.getForeground());
				}
				setEnabled(list.isEnabled());
				setFont(list.getFont());
				setOpaque(true);
				return this;
			}
		}

		/**
		 * Class to handle clicks and drags to rearrange the list.
		 */
		private class DragListener implements MouseListener, MouseMotionListener
		{
			public void mousePressed(MouseEvent e)
			{
				startItem = list.locationToIndex(e.getPoint());
				endItem = startItem;
				endBefore = true;
				endLineHighlightBefore = -1;
			}

			public void mouseDragged(MouseEvent e)
			{
				highlightEndItem(e);
			}

			public void mouseReleased(MouseEvent e)
			{
				removeHighlight();

				// rearrange the list
				int newIndex = endItem;
				if (!endBefore) newIndex++;
				if (newIndex > startItem) newIndex--;
				Object was = model.getElementAt(startItem);
				model.remove(startItem);
				model.add(newIndex, was);
				startItem = -1;
			}

			public void mouseMoved(MouseEvent e) {}
			public void mouseClicked(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}

			private void highlightEndItem(MouseEvent e)
			{
				endItem = list.locationToIndex(e.getPoint());
				int height = list.getFixedCellHeight();
				Point pt = list.indexToLocation(endItem);
				endBefore = e.getPoint().y < pt.y - height/2;
				removeHighlight();
				if (startItem == endItem) return;
				if (startItem == endItem-1 && endBefore) return;
				if (startItem == endItem+1 && !endBefore) return;

				endLineHighlightBefore = endItem;
				if (!endBefore) endLineHighlightBefore++;
				Graphics g = list.getGraphics();
				pt = list.indexToLocation(endLineHighlightBefore);
				Rectangle rect = list.getBounds();
				g.setColor(Color.RED);
				g.drawLine(rect.x, pt.y, rect.x+rect.width, pt.y);
			}

			private void removeHighlight()
			{
				if (endLineHighlightBefore >= 0)
				{
					Graphics g = list.getGraphics();
					Point pt = list.indexToLocation(endLineHighlightBefore);
					Rectangle rect = list.getBounds();
					g.setColor(list.getBackground());
					g.drawLine(rect.x, pt.y, rect.x+rect.width, pt.y);
					endLineHighlightBefore = -1;
				}
			}
		}
	}

    static class DocColumn
    {
        List<String> elements;
        String header;
        int maxWid = 0;

        DocColumn(String header, int numE)
        {
            this.header = header;
            this.maxWid = header.length();
            elements = new ArrayList<String>(numE);
        }

        void add(String el)
        {
            elements.add(el);
            int len = el.length();
            if (maxWid < len) maxWid = len;
        }

        private String getColumn(String s)
        {
            StringBuffer val = new StringBuffer(s);
            val.ensureCapacity(maxWid);
            int fillS = val.length();
            for (int i = fillS; i < maxWid+2; i++) // add 2 extra spaces for better formatting
                val.append(" "); // add the remaind spaces
            return val.toString();
        }

        String getUnderlying()
        {
            StringBuffer s = new StringBuffer();
            s.ensureCapacity(maxWid);
            for (int i = 0; i < maxWid; i++)
                s.append("-");
            s.append("  "); // two extra spaces
            return s.toString();
        }

        String getHeader()
        {
            return getColumn(header);
        }

        String get(int pos)
        {
            if (pos >= elements.size())
                return "";
            return getColumn(elements.get(pos));
        }

        static void printColumns(DocColumn[] cols, String title)
        {
            // print headers
            StringBuffer header = new StringBuffer();
            StringBuffer under = new StringBuffer();
            int totalNumEls = 0;

            for (DocColumn col : cols)
            {
                header.append(col.getHeader());
                under.append(col.getUnderlying());
                int numEls = col.elements.size();
                if (numEls > totalNumEls) totalNumEls = numEls;
            }
            int numLine = header.length();
            int stars = (numLine - title.length() - 4) / 2;
            for(int i=0; i<stars; i++) System.out.print("*");
            System.out.print(" " + title + " ");
            for(int i=0; i<stars; i++) System.out.print("*");
            System.out.println();

            System.out.println(header.toString());
            System.out.println(under.toString());

            for (int i = 0; i < totalNumEls; i++)
            {
                for (DocColumn col : cols)
                    System.out.print(col.get(i));
                System.out.println();
            }
            System.out.println();
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
        Map<Layer,String> gdsLayers = tech.getGDSLayers();

        DocColumn[] cols = new DocColumn[7];
        cols[0] = new DocColumn("Layer", layerCount);
        cols[1] = new DocColumn("Color", layerCount);
        cols[2] = new DocColumn("Style", layerCount);
        cols[3] = new DocColumn("CIF", layerCount);
        cols[4] = new DocColumn("GDS", layerCount);
        cols[5] = new DocColumn("Function", layerCount);
        cols[6] = new DocColumn("Coverage", layerCount);

        // Adding the layers
        for (Iterator<Layer> it = tech.getLayers(); it.hasNext();)
        {
            Layer layer = it.next();
            // Name
            cols[0].add(layer.getName());
            // Transparency
            EGraphics gra = layer.getGraphics();
			if (gra.getTransparentLayer() > 0)
                cols[1].add("Transparent " + gra.getTransparentLayer());
            else
			{
				Color col = gra.getColor();
				cols[1].add("(" + col.getRed() + "," + col.getGreen() + "," + col.getBlue() + ")");
			}
            // Style
            String tmp = "?";
            if (gra.isPatternedOnDisplay())
			{
				if (gra.getOutlined() != EGraphics.Outline.NOPAT)
                    tmp = "pat/outl";
                else
					tmp = "pat";
			} else
			{
				tmp = "solid";
			}
            cols[2].add(tmp);
            // CIF
            cols[3].add(layer.getCIFLayer());
            // GDS
            String gdsLayer = gdsLayers.get(layer);
            cols[4].add(gdsLayer != null ? gdsLayer : "");
            // Function
            cols[5].add(layer.getFunction().toString());
            // Coverage
            cols[6].add(TextUtils.formatDouble(LayerCoverageTool.LayerCoveragePreferences.DEFAULT_AREA_COVERAGE/*layer.getAreaCoverage()*/));
        }

        // write the layer information */
        DocColumn.printColumns(cols, "LAYERS IN " + tech.getTechName().toUpperCase());

		// ****************************** dump arcs ******************************
        cols = new DocColumn[8];
        int numArcs = tech.getNumArcs();
        cols[0] = new DocColumn("Arc", numArcs);
        cols[1] = new DocColumn("Layer", numArcs);
        cols[2] = new DocColumn("Size", numArcs);
        cols[3] = new DocColumn("Extend", numArcs);
        cols[4] = new DocColumn("Angle", numArcs);
        cols[5] = new DocColumn("Wipes", numArcs);
        cols[6] = new DocColumn("Function", numArcs);
        cols[7] = new DocColumn("Antenna", numArcs);

        for(Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); )
		{
			ArcProto ap = it.next();
			Poly [] polys = ap.getShapeOfDummyArc(4000);

            for(int k=0; k<polys.length; k++)
			{
                String name = "", extend = "", increment = "", wipe = "", func = "", antenna = "";
                if (k == 0) // first time only
                {
                    // Arc name
                    name = ap.getName();
                    // Extended
                    extend = ap.getFactoryDefaultInst().isTailExtended() ? "yes" : "no";
                    // Increment
                    increment = String.valueOf(ap.getFactoryAngleIncrement());
                    // Wipable
                    wipe = ap.isWipable() ? "yes" : "no";
                    // Function
                    func = ap.getFunction().toString();
                    // Antenna
                    antenna = TextUtils.formatDouble(ap.getFactoryAntennaRatio());
                }

                // Arc name
                cols[0].add(name);
                // Extended
                cols[3].add(extend);
                // Increment
                cols[4].add(increment);
                // Wipable
                cols[5].add(wipe);
                // Function
                cols[6].add(func);
                // Antenna
                cols[7].add(antenna);

                Poly poly = polys[k];
                // Layer name
                cols[1].add(poly.getLayer().getName());
				Rectangle2D bounds = poly.getBounds2D();
				double width = Math.min(bounds.getWidth(), bounds.getHeight());
                cols[2].add(TextUtils.formatDouble(width));
			}
		}

        // write the arc information */
        DocColumn.printColumns(cols, "ARCS IN " + tech.getTechName().toUpperCase());

		// ****************************** dump nodes ******************************
        cols = new DocColumn[8];
        int numNodes = tech.getNumNodes();
        cols[0] = new DocColumn("Node", numNodes);
        cols[1] = new DocColumn("Function", numNodes);
        cols[2] = new DocColumn("Layers", numNodes);
        cols[3] = new DocColumn("Size (#instances)", numNodes);
        cols[4] = new DocColumn("Ports", numNodes);
        cols[5] = new DocColumn("Size", numNodes);
        cols[6] = new DocColumn("Angle", numNodes);
        cols[7] = new DocColumn("Connections", numNodes);

        for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
        {
            PrimitiveNode np = it.next();
            if (np.isNotUsed()) continue; // not need of reporting it. Valid when foundry is not Mosis in 180nm,

            NodeInst ni = NodeInst.makeDummyInstance(np);
            Poly [] polys = tech.getShapeOfNode(ni);

            Map<Layer, List<Poly>> map = new HashMap<Layer,List<Poly>>();

            for (Poly p : polys)
            {
                List<Poly> list = map.get(p.getLayer());
                if (list == null)
                {
                    list = new ArrayList<Poly>(1);
                    map.put(p.getLayer(), list);
                }
                list.add(p);
            }

            boolean firstTime = true;
            int numLayers = 0;
            for (Layer layer : map.keySet())
            {
                Set<Rectangle2D> set = new HashSet<Rectangle2D>();
                List<Poly> list = map.get(layer);
                for (Poly p : list)
                {
                    Rectangle2D bound = p.getBounds2D();
                    ERectangle size = ERectangle.fromLambda(0, 0, bound.getWidth(), bound.getHeight());
                    set.add(size);
                }
                // only if all are identical
                int size = list.size();
                boolean allIdentical = set.size() == 1;
                if (allIdentical)
                {
                    Poly p = list.get(0);
                    list.clear();
                    list.add(p); // leaves only 1
                }
                for (Poly poly : list)
                {
                    String name = "", function = "";

                    if (firstTime)
                    {
                        firstTime = false;
                        name = np.getName();
                        function = np.getFunction().getName();
                    }
                    // Name
                    cols[0].add(name);
                    // Function
                    cols[1].add(function);
                    // Layer name
                    cols[2].add(poly.getLayer().getName());
                    Rectangle2D polyBounds = poly.getBounds2D();
                    // Layer Size
                    String sizeLabel = TextUtils.formatDouble(polyBounds.getWidth()) + " x " +
                        TextUtils.formatDouble(polyBounds.getHeight());
                    if (allIdentical && size > 1)
                        sizeLabel += " ("+size+")";
                    cols[3].add(sizeLabel);
                    numLayers++;
                }
            }
            int countPorts = 0, extra = 0;
            for(Iterator<PortProto> pIt = np.getPorts(); pIt.hasNext(); )
            {
                PrimitivePort pp = (PrimitivePort)pIt.next();

                // Port Name
                cols[4].add(pp.getName());
                Poly portPoly = ni.getShapeOfPort(pp);
                Rectangle2D portRect = portPoly.getBounds2D();
                // Port Size
                cols[5].add(TextUtils.formatDouble(portRect.getWidth()) + " x " +
                    TextUtils.formatDouble(portRect.getHeight()));
                // Port Angle
                cols[6].add((pp.getAngleRange() == 180) ? "" : String.valueOf(pp.getAngle()));

                int m = 0;
                ArcProto [] conList = pp.getConnections();
                for(ArcProto proto : conList)
                {
                    if (proto.getTechnology() != tech) continue;

                    if (m > 0)
                    {
                        // adding empty strings to previous columns
                        if ((countPorts+extra+m) >= numLayers)
                        {
                            cols[0].add("");
                            cols[1].add("");
                            cols[2].add("");
                            cols[3].add("");
                        }
                        cols[4].add("");
                        cols[5].add("");
                        cols[6].add("");
                        extra++;
                    }

                    // Connection
                    cols[7].add(proto.getName());
                    m++;
                }
                if (m == 0)
                    cols[7].add("<NONE>");
                countPorts++;
            }
            for (int i = (countPorts+extra); i < numLayers; i++)
            {
                cols[4].add("");
                cols[5].add("");
                cols[6].add("");
                cols[7].add("");
            }
        }

		// write the node information */
        DocColumn.printColumns(cols, "NODES IN " + tech.getTechName().toUpperCase());
    }

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
