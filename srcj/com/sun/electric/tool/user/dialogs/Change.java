/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Change.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.dialogs;

import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.geometry.GenMath.MutableInteger;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode.Function;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Client;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.CircuitChangeJobs;
import com.sun.electric.tool.user.HighlightListener;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.ExplorerTree;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 * Class to handle the "Change" dialog.
 */
public class Change extends EModelessDialog implements HighlightListener
{
	/** Change selected only. */				private static final int CHANGE_SELECTED = 1;
	/** Change all connected to this. */		private static final int CHANGE_CONNECTED = 2;
	/** Change all in this cell. */				private static final int CHANGE_CELL = 3;
	/** Change all in this Library. */			private static final int CHANGE_LIBRARY = 4;
	/** Change all in all Libraries. */			private static final int CHANGE_EVERYWHERE = 5;

	private static Change theDialog = null;
	private static boolean lastChangeNodesWithArcs = false;
	private static boolean lastIgnorePortNames = false;
	private static boolean lastAllowMissingPorts = false;
	private static int whatToChange = CHANGE_SELECTED;
	private static String libSelected = null;
	private static Map<PrimitiveNode,Map<Function,String>> specialSchematics = null;
	private DefaultMutableTreeNode rootNode, rootCells, rootPrims, rootArcs, currentlySelected;
	private DefaultTreeModel treeModel;
	private JTree changeTree;
	private List<Geometric> geomsToChange;                  // List of Geometrics to change
	private Map<DefaultMutableTreeNode,NodeProto> changeNodeProtoList;
	private Map<DefaultMutableTreeNode,ArcProto> changeArcProtoList;
	private Map<DefaultMutableTreeNode,Function> changeNodeProtoFunctionList;
	private EditWindow wnd;

	public static void showChangeDialog()
	{
        if (Client.getOperatingSystem() == Client.OS.UNIX && theDialog != null)
		{
			// On Linux, if a dialog is built, closed using setVisible(false),
			// and then requested again using setVisible(true), it does
			// not appear on top. I've tried using toFront(), requestFocus(),
			// but none of that works.  Instead, I brute force it and
			// rebuild the dialog from scratch each time.
		   	theDialog.closeDialog(null);
		}
		if (theDialog == null)
		{
			JFrame jf = null;
			if (TopLevel.isMDIMode()) jf = TopLevel.getCurrentJFrame();
			theDialog = new Change(jf);
		}
		theDialog.loadInfo(true);
		theDialog.setVisible(true);
		theDialog.toFront();
	}

	private void setupSpecialPrimitives()
	{
		if (specialSchematics != null) return;
		specialSchematics = new HashMap<PrimitiveNode,Map<Function,String>>();
		Map<Function,String> specialTransistor = new HashMap<Function,String>();
		Map<Function,String> special4Transistor = new HashMap<Function,String>();
    	List<Function> functions = Function.getFunctions();
    	for(Function fun : functions)
    	{
    		if (!fun.isTransistor()) continue;
    		Function altFun = fun.make3PortTransistor();
    		if (altFun != null) special4Transistor.put(fun, fun.getShortName()); else
    			specialTransistor.put(fun, fun.getShortName());
    	}
    	specialSchematics.put(Schematics.tech().transistorNode, specialTransistor);
    	specialSchematics.put(Schematics.tech().transistor4Node, special4Transistor);

    	Map<Function,String> specialResistor = new HashMap<Function,String>();
    	specialResistor.put(Function.RESIST, "normal");
    	specialResistor.put(Function.RESNPOLY, "n-poly");
    	specialResistor.put(Function.RESPPOLY, "p-poly");
    	specialResistor.put(Function.RESNNSPOLY, "n-poly-no-silicide");
    	specialResistor.put(Function.RESPNSPOLY, "p-poly-no-silicide");
    	specialResistor.put(Function.RESNWELL, "n-well");
    	specialResistor.put(Function.RESPWELL, "p-well");
    	specialResistor.put(Function.RESNACTIVE, "n-active");
    	specialResistor.put(Function.RESPACTIVE, "p-active");
    	specialResistor.put(Function.RESHIRESPOLY2, "hi-res-poly-2");
    	specialSchematics.put(Schematics.tech().resistorNode, specialResistor);

    	Map<Function,String> specialDiode = new HashMap<Function,String>();
    	specialDiode.put(Function.DIODE, "normal");
    	specialDiode.put(Function.DIODEZ, "zener");
    	specialSchematics.put(Schematics.tech().diodeNode, specialDiode);

    	Map<Function,String> specialCapacitor = new HashMap<Function,String>();
    	specialCapacitor.put(Function.CAPAC, "normal");
    	specialCapacitor.put(Function.ECAPAC, "electrolytic");
    	specialCapacitor.put(Function.POLY2CAPAC, "poly-2");
    	specialSchematics.put(Schematics.tech().capacitorNode, specialCapacitor);

    	Map<Function,String> specialFlipFlop = new HashMap<Function,String>();
    	specialFlipFlop.put(Function.FLIPFLOPRSMS, "RS-ms");
    	specialFlipFlop.put(Function.FLIPFLOPRSP, "RS-p");
    	specialFlipFlop.put(Function.FLIPFLOPRSN, "RS-n");
    	specialFlipFlop.put(Function.FLIPFLOPJKMS, "JK-ms");
    	specialFlipFlop.put(Function.FLIPFLOPJKP, "JK-p");
    	specialFlipFlop.put(Function.FLIPFLOPJKN, "JK-n");
    	specialFlipFlop.put(Function.FLIPFLOPDMS, "D-ms");
    	specialFlipFlop.put(Function.FLIPFLOPDP, "D-p");
    	specialFlipFlop.put(Function.FLIPFLOPDN, "D-n");
    	specialFlipFlop.put(Function.FLIPFLOPTMS, "T-ms");
    	specialFlipFlop.put(Function.FLIPFLOPTP, "T-p");
    	specialFlipFlop.put(Function.FLIPFLOPTN, "T-n");
    	specialSchematics.put(Schematics.tech().flipflopNode, specialFlipFlop);

    	Map<Function,String> specialTwoport = new HashMap<Function,String>();
    	specialTwoport.put(Function.VCCS, Function.VCCS.getShortName());
    	specialTwoport.put(Function.CCVS, Function.CCVS.getShortName());
    	specialTwoport.put(Function.VCVS, Function.VCVS.getShortName());
    	specialTwoport.put(Function.CCCS, Function.CCCS.getShortName());
    	specialTwoport.put(Function.TLINE, "transmission");
    	specialSchematics.put(Schematics.tech().twoportNode, specialTwoport);
	}

	/** Creates new form Change */
	private Change(Frame parent)
	{
		super(parent, false);
		initComponents();
        getRootPane().setDefaultButton(done);
        apply.setMnemonic('A');
        done.setMnemonic('D');

        // make sure special primitive map is built
        setupSpecialPrimitives();

        // build the change list
        currentlySelected = null;
		rootNode = new DefaultMutableTreeNode("");
		rootCells = new DefaultMutableTreeNode("Cells");
		rootPrims = new DefaultMutableTreeNode("Primitives");
		rootArcs = new DefaultMutableTreeNode("Arcs");
		rootNode.add(rootCells);
		rootNode.add(rootPrims);
		rootNode.add(rootArcs);
		treeModel = new MyDefaultTreeModel(rootNode);
		changeTree = new JTree(treeModel);
		changeTree.setRootVisible(false);
		changeTree.setShowsRootHandles(true);
		changeTree.addMouseListener(new TreeHandler());
		listPane.setViewportView(changeTree);

		changeNodeProtoList = new HashMap<DefaultMutableTreeNode,NodeProto>();
		changeArcProtoList = new HashMap<DefaultMutableTreeNode,ArcProto>();
		changeNodeProtoFunctionList = new HashMap<DefaultMutableTreeNode,Function>();

		// make a popup of libraries
		List<Library> libList = Library.getVisibleLibraries();
		int curIndex = libList.indexOf(Library.getCurrent());
		for(Library lib: libList)
		{
			librariesPopup.addItem(lib.getName());
			if (lib.getName().equals(libSelected))
			{
				curIndex = -1;						// won't set to current library now
				librariesPopup.setSelectedItem(libSelected);
			}
		}
		if (curIndex >= 0) librariesPopup.setSelectedIndex(curIndex);
		librariesPopup.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { reload(false); }
		});

		// restore defaults
		ignorePortNames.setSelected(lastIgnorePortNames);
		allowMissingPorts.setSelected(lastAllowMissingPorts);
		ignorePortNames.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { rememberState(); }
		});
		allowMissingPorts.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { rememberState(); }
		});

		showPrimitives.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { reload(false); }
		});
		showCells.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { reload(false); }
		});
		changeNodesWithArcs.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { reload(false); }
		});

		// setup the radio buttons that select what to change
		changeSelected.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { whatToChangeChanged(evt); }
		});
		changeConnected.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { whatToChangeChanged(evt); }
		});
		changeInCell.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { whatToChangeChanged(evt); }
		});
		changeInLibrary.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { whatToChangeChanged(evt); }
		});
		changeEverywhere.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { whatToChangeChanged(evt); }
		});
		switch (whatToChange)
		{
			case CHANGE_SELECTED:   changeSelected.setSelected(true);     break;
			case CHANGE_CONNECTED:  changeConnected.setSelected(true);    break;
			case CHANGE_CELL:       changeInCell.setSelected(true);       break;
			case CHANGE_LIBRARY:    changeInLibrary.setSelected(true);    break;
			case CHANGE_EVERYWHERE: changeEverywhere.setSelected(true);   break;
		}
		finishInitialization();
		Highlighter.addHighlightListener(this);
	}

	/**
	 * Class to handle clicks in the tree.
	 * Tracks what is selected and handles double-clicks to make a change.
	 */
	private class TreeHandler implements MouseListener
	{
		public void mouseClicked(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {}

		public void mousePressed(MouseEvent e)
		{
			if (e.getClickCount() == 2)
			{
				apply(null);
				return;
			}
			currentlySelected = null;
			TreePath currentPath = changeTree.getPathForLocation(e.getX(), e.getY());
			if (currentPath == null) return;
			currentlySelected = (DefaultMutableTreeNode)currentPath.getLastPathComponent();
		}
	}

	/**
	 * Class to manage the tree model so that the arcs/cells/primitives are all folder icons.
	 */
	private class MyDefaultTreeModel extends DefaultTreeModel
	{
		MyDefaultTreeModel(DefaultMutableTreeNode dmtn) { super(dmtn); }

		public boolean isLeaf(Object node)
		{
			if (node == rootArcs || node == rootCells || node == rootPrims) return false;
			return super.isLeaf(node);
		}
	}

	/**
	 * Reloads the dialog when Highlights change
	 */
	public void highlightChanged(Highlighter which)
	{
		if (!isVisible()) return;
		loadInfo(true);
	}

	/**
	 * Called when by a Highlighter when it loses focus. The argument
	 * is the Highlighter that has gained focus (may be null).
	 * @param highlighterGainedFocus the highlighter for the current window (may be null).
	 */
	public void highlighterLostFocus(Highlighter highlighterGainedFocus)
	{
		if (!isVisible()) return;
		loadInfo(false);
	}

	protected void escapePressed() { done(null); }

	private void whatToChangeChanged(ActionEvent evt)
	{
		JRadioButton src = (JRadioButton)evt.getSource();
		if (src == changeSelected) whatToChange = CHANGE_SELECTED; else
		if (src == changeConnected) whatToChange = CHANGE_CONNECTED; else
		if (src == changeInCell) whatToChange = CHANGE_CELL; else
		if (src == changeInLibrary) whatToChange = CHANGE_LIBRARY; else
		if (src == changeEverywhere) whatToChange = CHANGE_EVERYWHERE;
		Geometric geomToChange = geomsToChange.get(0);
		if (whatToChange == CHANGE_EVERYWHERE)
		{
			if (geomToChange instanceof ArcInst)
			{
				if (changeNodesWithArcs.isSelected())
				{
					changeNodesWithArcs.setSelected(false);
					reload(false);
				}
				changeNodesWithArcs.setEnabled(false);
			}
		} else
		{
			if (geomToChange instanceof ArcInst)
				changeNodesWithArcs.setEnabled(true);
		}
		updateChangeCount();
	}

	/**
	 * Method to count the number of arcs that will be considered when changing arc "oldAi".
	 * @param connected true to, replace all such arcs connected to this.
	 * @param thiscell true to replace all such arcs in the cell.
	 */
	private void countAllArcs(Cell cell, List<Geometric> highs, ArcInst oldAi, boolean connected, boolean thiscell,
		Set<Geometric> changedAlready)
	{
		List<NodeInst> changePins = new ArrayList<NodeInst>();

		for(Geometric geom : highs)
		{
			if (!(geom instanceof ArcInst)) continue;
			ArcInst ai = (ArcInst)geom;
			if (ai.getProto() != oldAi.getProto()) continue;
			changedAlready.add(ai);
		}
		if (connected)
		{
			Netlist netlist = cell.getNetlist();
			for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = it.next();
				if (ai.getProto() != oldAi.getProto()) continue;
				if (!netlist.sameNetwork(ai, oldAi)) continue;
				changedAlready.add(ai);
			}
		}
		if (thiscell)
		{
			for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = it.next();
				if (ai.getProto() != oldAi.getProto()) continue;
				changedAlready.add(ai);
			}
		}
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (ni.isCellInstance()) continue;
			if (!ni.getFunction().isPin()) continue;
			boolean allArcs = true;
			for(Iterator<Connection> cIt = ni.getConnections(); cIt.hasNext(); )
			{
				Connection con = cIt.next();
				if (!changedAlready.contains(con.getArc())) { allArcs = false;   break; }
			}
			if (ni.hasConnections() && allArcs)
				changePins.add(ni);
		}
	}

	private void updateChangeCount()
	{
		Set<Geometric> changedAlready = new HashSet<Geometric>();
		Cell cell = WindowFrame.getCurrentCell();
		String toChange = "";
		for (Geometric geomToChange : geomsToChange)
		{
			// count node replacement
			if (geomToChange instanceof NodeInst)
			{
				// get node to be replaced
				toChange = "nodes";
				NodeInst ni = (NodeInst)geomToChange;
				NodeProto oldNType = ni.getProto();
				changedAlready.add(ni);

				// count additional replacements if requested
				if (changeEverywhere.isSelected())
				{
					// replace in all cells of library if requested
					for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
					{
						Library lib = it.next();
						for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
						{
							Cell c = cIt.next();
							for(Iterator<NodeInst> nIt = c.getNodes(); nIt.hasNext(); )
							{
								NodeInst lNi = nIt.next();
								if (lNi.getProto() == oldNType) changedAlready.add(lNi);
							}
						}
					}
				} else if (changeInLibrary.isSelected())
				{
					// replace throughout the library containing "this cell" if requested
					Library lib = cell.getLibrary();
					for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
					{
						Cell c = cIt.next();
						for(Iterator<NodeInst> nIt = c.getNodes(); nIt.hasNext(); )
						{
							NodeInst lNi = nIt.next();
							if (lNi.getProto() == oldNType) changedAlready.add(lNi);
						}
					}
				} else if (changeInCell.isSelected())
				{
					// replace throughout this cell if requested
					for(Iterator<NodeInst> nIt = cell.getNodes(); nIt.hasNext(); )
					{
						NodeInst lNi = nIt.next();
						if (lNi.getProto() == oldNType) changedAlready.add(lNi);
					}
				} else if (changeConnected.isSelected())
				{
					// replace all connected to this in the cell if requested
					Netlist netlist = cell.getNetlist();
					for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
					{
						NodeInst lNi = it.next();
						if (lNi.getProto() != oldNType) continue;

						boolean found = false;
						for(Iterator<PortInst> pIt = ni.getPortInsts(); pIt.hasNext(); )
						{
							PortInst pi = pIt.next();
							for(Iterator<PortInst> lPIt = lNi.getPortInsts(); lPIt.hasNext(); )
							{
								PortInst lPi = lPIt.next();
								if (netlist.sameNetwork(pi.getNodeInst(), pi.getPortProto(), lPi.getNodeInst(), lPi.getPortProto()))
								{
									found = true;
									break;
								}
							}
							if (found) break;
						}
						if (found) changedAlready.add(lNi);
					}
				}
			} else
			{
				// count arc replacement
				toChange = "arcs";
				ArcInst ai = (ArcInst)geomToChange;
				ArcProto oldAType = ai.getProto();

				// special case when replacing nodes, too
				if (changeNodesWithArcs.isSelected())
				{
					List<Geometric> highs = wnd.getHighlighter().getHighlightedEObjs(true, true);
					if (changeInLibrary.isSelected())
					{
						for(Iterator<Cell> it = Library.getCurrent().getCells(); it.hasNext(); )
						{
							Cell e = it.next();
							countAllArcs(e, highs, ai, false, true, changedAlready);
						}
					} else
					{
						countAllArcs(ai.getParent(), highs, ai, changeConnected.isSelected(), changeInCell.isSelected(), changedAlready);
					}
				} else
				{
					// replace the arcinst
					changedAlready.add(ai);

					// do additional replacements if requested
					if (changeEverywhere.isSelected())
					{
						// replace in all cells of library if requested
						for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
						{
							Library lib = it.next();
							for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
							{
								Cell c = cIt.next();
								for(Iterator<ArcInst> nIt = c.getArcs(); nIt.hasNext(); )
								{
									ArcInst lAi = nIt.next();
									if (lAi.getProto() == oldAType) changedAlready.add(lAi);
								}
							}
						}
					} else if (changeInLibrary.isSelected())
					{
						// replace throughout this library if requested
						Library lib = Library.getCurrent();
						for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
						{
							Cell c = cIt.next();
							for(Iterator<ArcInst> nIt = c.getArcs(); nIt.hasNext(); )
							{
								ArcInst lAi = nIt.next();
								if (lAi.getProto() == oldAType) changedAlready.add(lAi);
							}
						}
					} else if (changeInCell.isSelected())
					{
						// replace throughout this cell if requested
						for(Iterator<ArcInst> nIt = cell.getArcs(); nIt.hasNext(); )
						{
							ArcInst lAi = nIt.next();
							if (lAi.getProto() == oldAType) changedAlready.add(lAi);
						}
					} else if (changeConnected.isSelected())
					{
						// replace all connected to this if requested
						Netlist netlist = cell.getNetlist();
						for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
						{
							ArcInst lAi = it.next();
							if (netlist.sameNetwork(ai, lAi)) changedAlready.add(lAi);
						}
					}
				}
			}
		}
		int num = changedAlready.size();
		changeCount.setText("Selected " + num + " " + toChange);
	}

	private static final Pattern dummyName = Pattern.compile("(.*?)FROM(.*?)\\{(.*)");

	/**
	 * Method called when the current selection has changed.
	 * Makes sure displayed options are correct.
	 */
	private void loadInfo(boolean showHighlighted)
	{
		// update current window
		EditWindow curWnd = EditWindow.getCurrent();
		if (curWnd != null) wnd = curWnd;

		// find out what is going to be changed
		geomsToChange = new ArrayList<Geometric>();
		if (wnd != null)
		{
			List<Geometric> highs = wnd.getHighlighter().getHighlightedEObjs(true, true);
            boolean hasArcs = false, hasCells = false, hasPrimitives = false;

            for (Geometric geom : highs)
			{
				geomsToChange.add(geom);
                if (geom instanceof ArcInst)
                    hasArcs = true;
                else if (geom instanceof NodeInst)
                {
                    NodeInst ni = (NodeInst)geom;
                    if (ni.isCellInstance())
                        hasCells = true;
                    else
                        hasPrimitives = true;
                }
            }
            if (hasArcs && hasCells)
            {
                System.out.println("The 'Change' dialog cannot handle selection of both arcs and cells." +
                    "\n Close the dialog if the selection of elements is correct.");
                geomsToChange.clear();
            }
            else if (hasArcs && hasPrimitives)
            {
            	System.out.println("The 'Change' dialog cannot handle selection of both arcs and primitives." +
                    "\n Close the dialog if the selection of elements is correct.");
                geomsToChange.clear();
            }
            else if (hasCells && hasPrimitives)
            {
            	System.out.println("The 'Change' dialog cannot handle selection of both cells and primitives." +
                "\n Close the dialog if the selection of elements is correct.");
                geomsToChange.clear();
            }
        }
		if (geomsToChange.size() == 0)
		{
			librariesPopup.setEnabled(false);
			ignorePortNames.setEnabled(false);
			allowMissingPorts.setEnabled(false);
			showPrimitives.setEnabled(false);
			showCells.setEnabled(false);
			changeNodesWithArcs.setEnabled(false);
			apply.setEnabled(false);
			changeSelected.setEnabled(false);
			changeConnected.setEnabled(false);
			changeInCell.setEnabled(false);
			changeInLibrary.setEnabled(false);
			changeEverywhere.setEnabled(false);
			return;
		}

		apply.setEnabled(true);
		changeSelected.setEnabled(true);
		changeConnected.setEnabled(true);
		changeInCell.setEnabled(true);
		changeInLibrary.setEnabled(true);
		changeEverywhere.setEnabled(true);
		Geometric geomToChange = geomsToChange.get(0);
		if (geomToChange instanceof NodeInst)
		{
			librariesPopup.setEnabled(true);
			ignorePortNames.setEnabled(true);
			allowMissingPorts.setEnabled(true);
			showPrimitives.setEnabled(true);
			showCells.setEnabled(true);
			NodeInst ni = (NodeInst)geomToChange;
			if (ni.isCellInstance())
			{
				showCells.setSelected(true);
			} else
			{
				showPrimitives.setSelected(true);
			}
			changeNodesWithArcs.setSelected(false);
			changeNodesWithArcs.setEnabled(false);
		} else
		{
			librariesPopup.setEnabled(false);
			ignorePortNames.setEnabled(false);
			allowMissingPorts.setEnabled(false);
			showPrimitives.setEnabled(false);
			showCells.setEnabled(false);
			changeNodesWithArcs.setEnabled(true);
			changeNodesWithArcs.setSelected(lastChangeNodesWithArcs);
		}

		reload(true);
	}

	private void rememberState()
	{
		lastIgnorePortNames = ignorePortNames.isSelected();
		lastAllowMissingPorts = allowMissingPorts.isSelected();
	}

	private boolean dontReload = false;

	/**
	 * Method called when dialog controls have changed.
	 * Makes sure the displayed lists and options are correct.
	 */
	private void reload(boolean canSwitchLibraries)
	{
		lastChangeNodesWithArcs = changeNodesWithArcs.isSelected();
		if (dontReload) return;

		ExplorerTree.KeepTreeExpansion kte = new ExplorerTree.KeepTreeExpansion(changeTree, rootNode, treeModel, new TreePath(rootNode));

        rootCells.removeAllChildren();
		rootPrims.removeAllChildren();
		rootArcs.removeAllChildren();
		changeNodeProtoList.clear();
		changeArcProtoList.clear();
		changeNodeProtoFunctionList.clear();
		if (geomsToChange.size() == 0) return;
		Technology curTech = Technology.getCurrent();
		Geometric geomToChange = geomsToChange.get(0);
		if (geomToChange instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)geomToChange;
			if (showCells.isSelected())
			{
				// cell: only list other cells as replacements
				if (ni.isCellInstance() && canSwitchLibraries)
				{
					Cell parent = (Cell)ni.getProto();
					Library lib = parent.getLibrary();
					dontReload = true;
					librariesPopup.setSelectedItem(lib.getName());
					dontReload = false;
				}

				View origView = null;
				if (ni.isCellInstance())
					origView = ((Cell)ni.getProto()).getView();
				String libName = (String)librariesPopup.getSelectedItem();
				Library lib = Library.findLibrary(libName);
				for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
				{
					Cell cell = it.next();
					if (origView != null)
					{
						// filter according to original node's view
						if (origView == View.ICON)
						{
							if (cell.getView() != View.ICON) continue;
						} else if (origView == View.LAYOUT || origView == View.LAYOUTCOMP || origView == View.LAYOUTSKEL)
						{
							if (cell.getView() != View.LAYOUT && cell.getView() != View.LAYOUTCOMP && cell.getView() != View.LAYOUTSKEL)
								continue;
						}
					}
					DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode(cell.noLibDescribe());
					rootCells.add(dmtn);
					changeNodeProtoList.put(dmtn, cell);
				}
			}
			if (showPrimitives.isSelected())
			{
            	// primitive: list primitives in this and the generic technology
				for(PrimitiveNode np : curTech.getNodesSortedByName())
				{
                    if (np.isNotUsed()) continue; // skip primitives not in use
                    Map<Function,String> specialList = specialSchematics.get(np);
                    if (specialList != null)
                    {
                    	Map<String,Function> subNames = new TreeMap<String,Function>();
                    	for(Function fun : specialList.keySet()) subNames.put(specialList.get(fun), fun);
                    	DefaultMutableTreeNode subPrims = new DefaultMutableTreeNode(np.describe(false));
    					rootPrims.add(subPrims);
                    	for(String subName : subNames.keySet())
                    	{
                    		Function fun = subNames.get(subName);
                    		DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode(subName);
                    		subPrims.add(dmtn);
        					changeNodeProtoList.put(dmtn, np);
        					changeNodeProtoFunctionList.put(dmtn, fun);
                    	}
                    } else
                    {
                    	DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode(np.describe(false)); 
                    	rootPrims.add(dmtn);
    					changeNodeProtoList.put(dmtn, np);
                    }
				}
				if (curTech != Generic.tech())
				{
					DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode("Generic:Universal-Pin");
					rootPrims.add(dmtn);
					changeNodeProtoList.put(dmtn, Generic.tech().universalPinNode);

					dmtn = new DefaultMutableTreeNode("Generic:Invisible-Pin");
					rootPrims.add(dmtn);
					changeNodeProtoList.put(dmtn, Generic.tech().invisiblePinNode);

					dmtn = new DefaultMutableTreeNode("Generic:Unrouted-Pin");
					rootPrims.add(dmtn);
					changeNodeProtoList.put(dmtn, Generic.tech().unroutedPinNode);
				}
			}

			// try to select prototype of selected node
			for(DefaultMutableTreeNode dmtn : changeNodeProtoList.keySet())
			{
				NodeProto np = changeNodeProtoList.get(dmtn);
				Function fun = changeNodeProtoFunctionList.get(dmtn);
				if (np == ni.getProto())
				{
					TreePath path = new TreePath(rootNode);
					if (ni.isCellInstance())
					{
						path = path.pathByAddingChild(rootCells);
					} else
					{
						path = path.pathByAddingChild(rootPrims);
					}
					if (fun != null)
					{
						if (ni.getFunction() != fun) continue;
						DefaultMutableTreeNode parent = (DefaultMutableTreeNode)dmtn.getParent();
						path = path.pathByAddingChild(parent);
					}
					path = path.pathByAddingChild(dmtn);
					changeTree.expandPath(path);
					changeTree.setSelectionPath(path);
					SwingUtilities.invokeLater(new MyRunnable(path));
					break;
				}
			}
			if (showCells.isSelected())
			{
				String geomName = ((NodeInst)geomToChange).getProto().describe(false);

				// if replacing dummy facet, name will be [cellname]FROM[libname][{view}]
				Matcher mat = dummyName.matcher(geomName);
				if (mat.matches())
				{
					// try to select items.  Nothing will happen if they are not in list.
//					changeList.setSelectedValue(mat.group(1) + "{" + mat.group(3), true);
					librariesPopup.setSelectedItem(mat.group(2));
				} else
				{
					// otherwise, try to match name
//					changeList.setSelectedValue(geomName, true);
				}
			}
		} else
		{
			// load arcs in current technology, arc's technology, and generic technology
			ArcInst ai = (ArcInst)geomToChange;
			PortProto pp1 = ai.getHeadPortInst().getPortProto();
			PortProto pp2 = ai.getTailPortInst().getPortProto();
			for(Iterator<ArcProto> it = curTech.getArcs(); it.hasNext(); )
			{
				ArcProto ap = it.next();
				if (ap.isNotUsed()) continue;
				if (!changeNodesWithArcs.isSelected())
				{
					if (!pp1.connectsTo(ap)) continue;
					if (!pp2.connectsTo(ap)) continue;
				}
				DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode(ap.describe());
				rootArcs.add(dmtn);
				changeArcProtoList.put(dmtn, ap);
			}
			if (curTech != Generic.tech())
			{
				for(Iterator<ArcProto> it = Generic.tech().getArcs(); it.hasNext(); )
				{
					ArcProto ap = it.next();
					if (ap.isNotUsed()) continue;
					if (!changeNodesWithArcs.isSelected())
					{
						if (!pp1.connectsTo(ap)) continue;
						if (!pp2.connectsTo(ap)) continue;
					}
					DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode(ap.describe());
					rootArcs.add(dmtn);
					changeArcProtoList.put(dmtn, ap);
				}
			}
			Technology arcTech = ai.getProto().getTechnology();
			if (arcTech != curTech && arcTech != Generic.tech())
			{
				for(Iterator<ArcProto> it = arcTech.getArcs(); it.hasNext(); )
				{
					ArcProto ap = it.next();
					if (ap.isNotUsed()) continue;
					if (!changeNodesWithArcs.isSelected())
					{
						if (!pp1.connectsTo(ap)) continue;
						if (!pp2.connectsTo(ap)) continue;
					}
					DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode(ap.describe());
					rootArcs.add(dmtn);
					changeArcProtoList.put(dmtn, ap);
				}
			}
			for(DefaultMutableTreeNode dmtn : changeArcProtoList.keySet())
			{
				ArcProto ap = changeArcProtoList.get(dmtn);
				if (ap == ai.getProto())
				{
					TreePath path = new TreePath(rootNode);
					path = path.pathByAddingChild(rootArcs);
					path = path.pathByAddingChild(dmtn);
					changeTree.expandPath(path);
					changeTree.setSelectionPath(path);
					SwingUtilities.invokeLater(new MyRunnable(path));
					break;
				}
			}
 		}
		changeTree.updateUI();
        kte.restore();
		updateChangeCount();
	}

	private class MyRunnable implements Runnable
	{
		private TreePath path;

		MyRunnable(TreePath path) { this.path = path; }

		public void run() { changeTree.scrollPathToVisible(path); }
	}

	private void doTheChange()
	{
		NodeProto np = null;
		ArcProto ap = null;
		Function func = null;
		Geometric geomToChange = geomsToChange.get(0);
		if (geomToChange instanceof NodeInst)
		{
			np = changeNodeProtoList.get(currentlySelected);
			func = changeNodeProtoFunctionList.get(currentlySelected);
		} else
		{
			ap = changeArcProtoList.get(currentlySelected);
			if (ap == null)
			{
				System.out.println("Nothing is selected");
				return;
			}
		}

		List<Geometric> highs = wnd.getHighlighter().getHighlightedEObjs(true, true);
		new ChangeObject(geomsToChange, highs, getLibSelected(), np, func, ap, ignorePortNames.isSelected(),
			allowMissingPorts.isSelected(), changeNodesWithArcs.isSelected(), changeInCell.isSelected(),
			changeInLibrary.isSelected(), changeEverywhere.isSelected(), changeConnected.isSelected());
	}

	/**
	 * Class to change the node/arc type in a new thread.
	 */
	private static class ChangeObject extends Job
	{
		private List<Geometric> geomsToChange, highs;
		private String libName;
		private NodeProto np;
		private ArcProto ap;
		private Cell cell;
		private boolean ignorePortNames, allowMissingPorts, changeNodesWithArcs;
		private boolean changeInCell, changeInLibrary, changeEverywhere, changeConnected;
		private Function func;
		private List<Geometric> highlightThese;

		private ChangeObject(List<Geometric> geomsToChange, List<Geometric> highs, String libName,
			NodeProto np, Function func, ArcProto ap, boolean ignorePortNames, boolean allowMissingPorts,
			boolean changeNodesWithArcs, boolean changeInCell, boolean changeInLibrary,
			boolean changeEverywhere, boolean changeConnected)
		{
			super("Change type", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.geomsToChange = geomsToChange;
			this.highs = highs;
			this.libName = libName;
			this.np = np;
			this.func = func;
			this.ap = ap;
			this.cell = WindowFrame.getCurrentCell();
			this.ignorePortNames = ignorePortNames;
			this.allowMissingPorts = allowMissingPorts;
			this.changeNodesWithArcs = changeNodesWithArcs;
			this.changeInCell = changeInCell;
			this.changeInLibrary = changeInLibrary;
			this.changeEverywhere = changeEverywhere;
			this.changeConnected = changeConnected;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			highlightThese = new ArrayList<Geometric>();
			fieldVariableChanged("highlightThese");
			Set<Geometric> changedAlready = new HashSet<Geometric>();
			MutableInteger nodeFailures = new MutableInteger(0);

			for (Geometric geomToChange : geomsToChange)
			{
				// handle node replacement
				if (geomToChange instanceof NodeInst)
				{
					// get node to be replaced
					NodeInst ni = (NodeInst)geomToChange;
					NodeProto oldNType = ni.getProto();

					// disallow replacing if lock is on
					if (CircuitChangeJobs.cantEdit(ni.getParent(), ni, true, false, true) != 0) return false;

					// get nodeproto to replace it with
					Library library = Library.findLibrary(libName);
					if (library == null) return false;
					if (np == null) return false;

					// replace the selected node
					int total = 0;
					NodeInst onlyNewNi = updateNodeInst(ni, changedAlready, nodeFailures);
					if (onlyNewNi != null)
					{
						highlightThese.add(onlyNewNi);
						total++;
					}

					String replacedWith = "node " + np.describe(false);
					if (func != null) replacedWith += " (" + func.getShortName() + ")";

					// do additional replacements if requested
					if (changeEverywhere)
					{
						// replace in all cells of library if requested
						for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
						{
							Library lib = it.next();
							for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
							{
								Cell cell = cIt.next();

								boolean found = true;
								while (found)
								{
									found = false;
									for(Iterator<NodeInst> nIt = cell.getNodes(); nIt.hasNext(); )
									{
										NodeInst lNi = nIt.next();
										if (lNi.getProto() != oldNType) continue;

										// do not replace the example icon
										if (lNi.isIconOfParent())
										{
											System.out.println("Example icon in " + cell + " not replaced");
											continue;
										}

										// disallow replacing if lock is on
										int errorCode = CircuitChangeJobs.cantEdit(cell, lNi, true, false, true);
										if (errorCode < 0) return false;
										if (errorCode > 0) continue;

										NodeInst newNi = updateNodeInst(lNi, changedAlready, nodeFailures);
										if (newNi != null)
										{
											total++;
											found = true;
											break;
										}
									}
								}
							}
						}
						if (total > 0) System.out.println("All " + total + " " + oldNType.describe(true) +
							" nodes in all libraries replaced with " + replacedWith);
					} else if (changeInLibrary)
					{
						// replace throughout the library containing "this cell" if requested
						Library lib = cell.getLibrary();
						for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
						{
							Cell cell = cIt.next();
							boolean found = true;
							while (found)
							{
								found = false;
								for(Iterator<NodeInst> nIt = cell.getNodes(); nIt.hasNext(); )
								{
									NodeInst lNi = nIt.next();
									if (lNi.getProto() != oldNType) continue;

									// disallow replacing if lock is on
									int errorCode = CircuitChangeJobs.cantEdit(cell, lNi, true, false, true);
									if (errorCode < 0) return false;
									if (errorCode > 0) continue;

									NodeInst newNi = updateNodeInst(lNi, changedAlready, nodeFailures);
									if (newNi != null)
									{
										total++;
										found = true;
										break;
									}
								}
							}
						}
						if (total > 0) System.out.println("All " + total + " " + oldNType.describe(true) +
							" nodes in " + lib + " replaced with " + replacedWith);
					} else if (changeInCell)
					{
						// replace throughout this cell if requested
						boolean found = true;
						while (found)
						{
							found = false;
							for(Iterator<NodeInst> nIt = cell.getNodes(); nIt.hasNext(); )
							{
								NodeInst lNi = nIt.next();
								if (lNi.getProto() != oldNType) continue;

								// disallow replacing if lock is on
								int errorCode = CircuitChangeJobs.cantEdit(cell, lNi, true, false, true);
								if (errorCode < 0) return false;
								if (errorCode > 0) continue;

								NodeInst newNi = updateNodeInst(lNi, changedAlready, nodeFailures);
								if (newNi != null)
								{
									total++;
									found = true;
									break;
								}
							}
						}
						if (total > 0) System.out.println("All " + total + " " + oldNType.describe(true) + " nodes in " +
							cell + " replaced with " + replacedWith);
					} else if (changeConnected)
					{
						// replace all connected to this in the cell if requested
						Netlist netlist = cell.getNetlist();
						List<NodeInst> others = new ArrayList<NodeInst>();
						NodeInst newNi = null;
						if (highlightThese.size() == 1 && highlightThese.get(0) instanceof NodeInst)
							newNi = (NodeInst)highlightThese.get(0);
						for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
						{
							NodeInst lNi = it.next();
							if (lNi.getProto() != oldNType) continue;
							if (lNi == newNi) continue;

							boolean found = false;
							for(Iterator<PortInst> pIt = newNi.getPortInsts(); pIt.hasNext(); )
							{
								PortInst pi = pIt.next();
								for(Iterator<PortInst> lPIt = lNi.getPortInsts(); lPIt.hasNext(); )
								{
									PortInst lPi = lPIt.next();
									if (netlist.sameNetwork(pi.getNodeInst(), pi.getPortProto(), lPi.getNodeInst(), lPi.getPortProto()))
									{
										found = true;
										break;
									}
								}
								if (found) break;
							}
							if (found) others.add(lNi);
						}

						// make the changes
						for(NodeInst lNi : others)
						{
							// disallow replacing if lock is on
							int errorCode = CircuitChangeJobs.cantEdit(cell, lNi, true, false, true);
							if (errorCode < 0) return false;
							if (errorCode > 0) continue;

							NodeInst newNode = updateNodeInst(lNi, changedAlready, nodeFailures);
							if (newNode != null) total++;
						}
						if (total > 0) System.out.println("All " + total + " " + oldNType.describe(true) +
							" nodes connected to this replaced with " + replacedWith);
					} else
					{
						if (total > 0) System.out.println(oldNType + " replaced with " + replacedWith);
					}
				} else
				{
                    // get arc to be replaced
					ArcInst ai = (ArcInst)geomToChange;

                    if (ap == null)
                    {
                        System.out.println("Arc " + ai.getName() + " skipped");
                        continue;
                    }

					// disallow replacement if lock is on
					if (CircuitChangeJobs.cantEdit(ai.getParent(), null, true, false, true) != 0) return false;

					// sanity check
					ArcProto oldAType = ai.getProto();
					if (oldAType == ap)
					{
						System.out.println("Arc already of type " + ap.describe());
						return false;
					}

					// special case when replacing nodes, too
					if (changeNodesWithArcs)
					{
						if (changeInLibrary)
						{
							for(Iterator<Cell> it = Library.getCurrent().getCells(); it.hasNext(); )
							{
								Cell cell = it.next();
								replaceAllArcs(cell, highs, ai, ap, false, true);
							}
						} else
						{
							replaceAllArcs(ai.getParent(), highs, ai, ap, changeConnected, changeInCell);
						}
						return true;
					}

					// replace the arcinst
					ArcInst onlyNewAi = ai.replace(ap);
					if (onlyNewAi == null)
					{
						System.out.println(ap + " does not fit in the place of " + oldAType);
						return false;
					}
					highlightThese.add(onlyNewAi);

					// do additional replacements if requested
					int total = 1;
					if (changeEverywhere)
					{
						// replace in all cells of library if requested
						for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
						{
							Library lib = it.next();
							for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
							{
								Cell cell = cIt.next();
								boolean found = true;
								while (found)
								{
									found = false;
									for(Iterator<ArcInst> nIt = cell.getArcs(); nIt.hasNext(); )
									{
										ArcInst lAi = nIt.next();
										if (lAi.getProto() != oldAType) continue;

										// disallow replacing if lock is on
										int errorCode = CircuitChangeJobs.cantEdit(cell, null, true, false, true);
										if (errorCode < 0) return false;
										if (errorCode > 0) continue;

										ArcInst newAi = lAi.replace(ap);
										if (newAi != null)
										{
											total++;
											found = true;
											break;
										}
									}
								}
							}
						}
						System.out.println("All " + total + " " + oldAType.describe() +
							" arcs in the library replaced with " + ap);
					} else if (changeInLibrary)
					{
						// replace throughout this library if requested
						Library lib = Library.getCurrent();
						for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
						{
							Cell cell = cIt.next();
							boolean found = true;
							while (found)
							{
								found = false;
								for(Iterator<ArcInst> nIt = cell.getArcs(); nIt.hasNext(); )
								{
									ArcInst lAi = nIt.next();
									if (lAi.getProto() != oldAType) continue;

									// disallow replacing if lock is on
									int errorCode = CircuitChangeJobs.cantEdit(cell, null, true, false, true);
									if (errorCode < 0) return false;
									if (errorCode > 0) continue;

									ArcInst newAi = lAi.replace(ap);
									if (newAi != null)
									{
										total++;
										found = true;
										break;
									}
								}
							}
						}
						System.out.println("All " + total + " " + oldAType.describe() +
							" arcs in " + lib + " replaced with " + ap);
					} else if (changeInCell)
					{
						// replace throughout this cell if requested
						boolean found = true;
						while (found)
						{
							found = false;
							for(Iterator<ArcInst> nIt = cell.getArcs(); nIt.hasNext(); )
							{
								ArcInst lAi = nIt.next();
								if (lAi.getProto() != oldAType) continue;

								// disallow replacing if lock is on
								int errorCode = CircuitChangeJobs.cantEdit(cell, null, true, false, true);
								if (errorCode < 0) return false;
								if (errorCode > 0) continue;

								ArcInst newAi = lAi.replace(ap);
								if (newAi != null)
								{
									total++;
									found = true;
									break;
								}
							}
						}
						System.out.println("All " + total + " " + oldAType.describe() +
							" arcs in " + cell + " replaced with " + ap);
					} else if (changeConnected)
					{
						// replace all connected to this if requested
						List<ArcInst> others = new ArrayList<ArcInst>();
						Netlist netlist = cell.getNetlist();
						for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
						{
							ArcInst lAi = it.next();
							if (lAi == onlyNewAi) continue;
							if (netlist.sameNetwork(onlyNewAi, lAi)) others.add(lAi);
						}

						for(ArcInst lAi : others)
						{
							ArcInst newAi = lAi.replace(ap);
							if (newAi != null) total++;
						}
						System.out.println("All " + total + " " + oldAType.describe() +
							" arcs connected to this replaced with " + ap);
					} else System.out.println(oldAType + " replaced with " +ap);
				}
			}
			if (nodeFailures.intValue() > 0)
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"There were " + nodeFailures.intValue() + " nodes that could not be replaced with " + np,
					"Change failed", JOptionPane.ERROR_MESSAGE);
				
			}
			return true;
		}

		/**
		 * Method to update a node with the replacement type.
		 * @param ni the node to update
		 * @param changedAlready a Set of nodes already updates.
		 * @return a node to highlight (because it was changed).  May be null.
		 */
		private NodeInst updateNodeInst(NodeInst ni, Set<Geometric> changedAlready, MutableInteger failures)
		{
			if (changedAlready.contains(ni)) return null;
			NodeProto oldNType = ni.getProto();
			boolean noChange = oldNType == np;
			if (noChange)
			{
				if (func != null)
				{
					Function oldFunc = ni.getFunction();
					if (func != oldFunc) noChange = false;
				}
			}
			if (noChange)
			{
				System.out.println("Node already of type " + np.describe(true));

				// just skip this case. No need to redo it. This not an error.
				return null;
			}

			// replace the nodeinsts
			changedAlready.add(ni);
			NodeInst onlyNewNi = ni;
			if (oldNType != np)
			{
				onlyNewNi = CircuitChangeJobs.replaceNodeInst(ni, np, ignorePortNames, allowMissingPorts);
				if (onlyNewNi == null)
				{
					System.out.println(np + " does not fit in the place of " + oldNType);
					failures.increment();
					return null;
				}
			}
			if (func != null)
				onlyNewNi.setTechSpecific(Schematics.getPrimitiveFunctionBits(func));
			return onlyNewNi;
		}

		public void terminateOK()
		{
			EditWindow wnd = EditWindow.getCurrent();
			if (wnd != null)
			{
				Highlighter highlighter = wnd.getHighlighter();
				for(Geometric geom : highlightThese)
				{
					highlighter.addElectricObject(geom, geom.getParent());
				}
                highlighter.finished();
            }
		}

		/**
		 * Method to replace arc "oldAi" with another of type "ap", adding layer-change contacts
		 * as needed to keep the connections.  If "connected" is true, replace all such arcs
		 * connected to this.  If "thiscell" is true, replace all such arcs in the cell.
		 */
		private void replaceAllArcs(Cell cell, List<Geometric> highs, ArcInst oldAi, ArcProto ap, boolean connected, boolean thiscell)
		{
			Set<Geometric> geomMarked = new HashSet<Geometric>();
			List<NodeInst> changePins = new ArrayList<NodeInst>();

			for(Geometric geom : highs)
			{
				if (!(geom instanceof ArcInst)) continue;
				ArcInst ai = (ArcInst)geom;
				if (ai.getProto() != oldAi.getProto()) continue;
				geomMarked.add(ai);
			}
			if (connected)
			{
				Netlist netlist = cell.getNetlist();
				for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
				{
					ArcInst ai = it.next();
					if (ai.getProto() != oldAi.getProto()) continue;
					if (!netlist.sameNetwork(ai, oldAi)) continue;
					geomMarked.add(ai);
				}
			}
			if (thiscell)
			{
				for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
				{
					ArcInst ai = it.next();
					if (ai.getProto() != oldAi.getProto()) continue;
					geomMarked.add(ai);
				}
			}
			for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				if (ni.isCellInstance()) continue;
				if (!ni.getFunction().isPin()) continue;
				boolean allArcs = true;
				for(Iterator<Connection> cIt = ni.getConnections(); cIt.hasNext(); )
				{
					Connection con = cIt.next();
					if (!geomMarked.contains(con.getArc())) { allArcs = false;   break; }
				}
				if (ni.hasConnections() && allArcs)
					changePins.add(ni);
			}

			// now create new pins where they belong
            EditingPreferences ep = cell.getEditingPreferences();
			PrimitiveNode pin = ap.findOverridablePinProto(ep);
			double xS = pin.getDefWidth();
			double yS = pin.getDefHeight();
			Map<NodeInst,NodeInst> newNodes = new HashMap<NodeInst,NodeInst>();
			for(NodeInst ni : changePins)
			{
				NodeInst newNi = NodeInst.makeInstance(pin, ni.getAnchorCenter(), xS, yS, cell);
				if (newNi == null) return;
				newNodes.put(ni, newNi);

				// move exports
				for(Iterator<Export> eIt = ni.getExports(); eIt.hasNext(); )
				{
					Export oldExport = eIt.next();
					if (oldExport.move(newNi.getOnlyPortInst()))
					{
						System.out.println("Unable to move export " + oldExport.getName() + " from old pin " + ni.describe(true) +
							" to new pin " + newNi);
					}
				}
			}

			// now create new arcs to replace the old ones
			for(Geometric geom : geomMarked)
			{
				if (!(geom instanceof ArcInst)) continue;
				ArcInst ai = (ArcInst)geom;
				PortInst pi0 = null;
				NodeInst newNi0 = newNodes.get(ai.getHeadPortInst().getNodeInst());
				if (newNi0 != null)
				{
					pi0 = newNi0.getOnlyPortInst();
				} else
				{
					// need contacts to get to the right level
					pi0 = makeContactStack(ai, ArcInst.HEADEND, ap);
					if (pi0 == null) return;
				}
				PortInst pi1 = null;
				NodeInst newNi1 = newNodes.get(ai.getTailPortInst().getNodeInst());
				if (newNi1 != null)
				{
					pi1 = newNi1.getOnlyPortInst();
				} else
				{
					// need contacts to get to the right level
					pi1 = makeContactStack(ai, ArcInst.TAILEND, ap);
					if (pi1 == null) return;
				}

				double wid = ap.getDefaultLambdaBaseWidth();
				if (ai.getLambdaBaseWidth() > wid) wid = ai.getLambdaBaseWidth();
				ArcInst newAi = ArcInst.makeInstanceBase(ap, wid, pi0, pi1, ai.getHeadLocation(),
					ai.getTailLocation(), ai.getName());
				if (newAi == null) return;
				newAi.copyPropertiesFrom(ai);
				geomMarked.remove(newAi);
			}

			// now remove the previous arcs and nodes
			for(Geometric geom : geomMarked)
			{
				if (geom instanceof ArcInst)
				{
					ArcInst ai = (ArcInst)geom;
					ai.kill();
				}
			}

			// delete old pins and copy their names to the new ones
			for(NodeInst ni : changePins)
			{
				if (!ni.hasExports())
				{
					String niName = ni.getName();
					ni.kill();
					NodeInst newNi = newNodes.get(ni);
					newNi.setName(niName);
				}
			}
		}

		private NodeProto [] contactStack = new NodeProto[100];
		private ArcProto [] contactStackArc = new ArcProto[100];
        private Technology connectionTech = null;
        private Map<ArcProto,Map<ArcProto,PrimitivePort>> connectionMap;

		/**
		 * Method to examine end "end" of arc "ai" and return a node at that position which
		 * can connect to arcs of type "ap".  This may require creation of one or more contacts
		 * to change layers.
		 */
		private PortInst makeContactStack(ArcInst ai, int end, ArcProto ap)
		{
			NodeInst lastNi = ai.getPortInst(end).getNodeInst();
			PortProto lastPp = ai.getPortInst(end).getPortProto();
			PortInst lastPi = lastNi.findPortInstFromProto(lastPp);
			Point2D center = ai.getLocation(end);
			Cell cell = ai.getParent();

			// setup map of contacts in the technology
            setupConnections(ap.getTechnology());

            // find the path of contacts to make the connection
			Set<ArcProto> markedArcs = new HashSet<ArcProto>();
            int depth = findOtherPathToArc(lastPp, ai.getProto(), ap, 0, markedArcs);
            if (depth < 0) return null;

			// create the contacts
			for(int i=0; i<depth; i++)
			{
				ArcProto typ = contactStackArc[i];
				double wid = ai.getLambdaBaseWidth();
				double xS = contactStack[i].getDefWidth();
				double yS = contactStack[i].getDefHeight();
				SizeOffset so = contactStack[i].getProtoSizeOffset();
				xS = Math.max(xS - so.getLowXOffset() - so.getHighXOffset(), wid) + so.getLowXOffset() + so.getHighXOffset();
				yS = Math.max(yS - so.getLowYOffset() - so.getHighYOffset(), wid) + so.getLowYOffset() + so.getHighYOffset();
				NodeInst newNi = NodeInst.makeInstance(contactStack[i], center, xS, yS, cell);
				if (newNi == null) return null;
				PortInst thisPi = newNi.findPortInstFromProto(contactStack[i].getPort(0));
				ArcInst newAi = ArcInst.newInstanceBase(typ, wid, thisPi, lastPi, center, center, null, ai.getAngle());
				lastPi = thisPi;
				if (newAi == null) return null;
				newAi.setFixedAngle(true);
			}
			return lastPi;
		}

		/**
		 * Method to setup the contact network for a given Technology.
		 * This network shows which contacts can be used to move from one ArcProto to another.
		 * @param tech the Technology to setup.
		 */
        private void setupConnections(Technology tech)
        {
        	if (connectionTech == tech) return;
        	connectionTech = tech;
        	connectionMap = new HashMap<ArcProto,Map<ArcProto,PrimitivePort>>();
        	for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
        	{
        		PrimitiveNode np = it.next();
        		if (np.isNotUsed()) continue;
				PrimitiveNode.Function fun = np.getFunction();
				if (!fun.isContact()) continue;
        		PrimitivePort pp = np.getPort(0);
        		ArcProto[] arcs = pp.getConnections();
        		ArcProto ap1 = null, ap2 = null;
        		for(int i=0; i<arcs.length; i++)
        		{
        			ArcProto ap = arcs[i];
        			if (ap.getTechnology() != tech) continue;
        			if (ap1 == null) ap1 = ap; else
        				if (ap2 == null) ap2 = ap;
        		}
        		if (ap1 == null || ap2 == null) continue;
        		addConnection(ap1, ap2, null);
        		addConnection(ap2, ap1, null);
        	}
        }

        /**
         * Method to update the contact network.
         * @param ap1 an ArcProto.
         * @param ap2 another ArcProto.
         * @param np the contact that connects them.
         */
        private void addConnection(ArcProto ap1, ArcProto ap2, PrimitivePort np)
        {
        	Map<ArcProto,PrimitivePort> arcMap = connectionMap.get(ap1);
        	if (arcMap == null) connectionMap.put(ap1, arcMap = new HashMap<ArcProto,PrimitivePort>());
        	if (arcMap.get(ap2) == null)
        	{
        		PrimitivePort pp = User.getUserTool().getCurrentContactPortProto(ap1, ap2);
if (pp == null) System.out.println("NULL PORT CONNECTING "+ap1.describe()+" AND "+ap2.describe());
        		arcMap.put(ap2, pp);
        	}
        }

        /**
		 * Method to compute an array of contacts and arcs that connects a port to an arcproto.
		 * @param pp the original port.
		 * @param sourceAp the source ArcProto.
		 * @param destAp the destination ArcProto.
		 * @param depth the location in the contact array to fill.
		 * @param markedArcs a set of Arcprotos that have been used in the search.
		 * @return the new size of the contact array.
		 */
		private int findOtherPathToArc(PortProto pp, ArcProto sourceAp, ArcProto destAp, int depth, Set<ArcProto> markedArcs)
		{
			// see if the connection is made
			if (pp.connectsTo(destAp)) return depth;

            // look for a contact
			PrimitiveNode bestNp = null;
			ArcProto bestAp = null;
			int bestDepth = 0;

			Map<ArcProto,PrimitivePort> arcMap = connectionMap.get(sourceAp);
			for(ArcProto nextAp : arcMap.keySet())
			{
				if (markedArcs.contains(nextAp)) continue;
                PortProto nextPp = arcMap.get(nextAp);
                if (nextPp == null) continue;
                PrimitiveNode nextNp = (PrimitiveNode)nextPp.getParent();

                // this contact is part of the chain
				contactStack[depth] = nextNp;
				markedArcs.add(nextAp);
				int newDepth = findOtherPathToArc(nextPp, nextAp, destAp, depth+1, markedArcs);
				markedArcs.remove(nextAp);
				if (newDepth < 0) continue;
				if (bestNp == null || newDepth < bestDepth)
				{
					bestDepth = newDepth;
					bestNp = nextNp;
					bestAp = nextAp;
				}
			}
			if (bestNp != null)
			{
				contactStack[depth] = bestNp;
				contactStackArc[depth] = sourceAp;
				markedArcs.add(bestAp);
				int newDepth = findOtherPathToArc(bestNp.getPort(0), bestAp, destAp, depth+1, markedArcs);
				markedArcs.remove(bestAp);
				return newDepth;
			}
			return -1;
		}
	}

	private String getLibSelected()
	{
		return (String)librariesPopup.getSelectedItem();
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        changeOption = new javax.swing.ButtonGroup();
        done = new javax.swing.JButton();
        apply = new javax.swing.JButton();
        listPane = new javax.swing.JScrollPane();
        changeSelected = new javax.swing.JRadioButton();
        changeConnected = new javax.swing.JRadioButton();
        changeInCell = new javax.swing.JRadioButton();
        changeInLibrary = new javax.swing.JRadioButton();
        changeEverywhere = new javax.swing.JRadioButton();
        changeNodesWithArcs = new javax.swing.JCheckBox();
        showPrimitives = new javax.swing.JCheckBox();
        showCells = new javax.swing.JCheckBox();
        ignorePortNames = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        librariesPopup = new javax.swing.JComboBox();
        allowMissingPorts = new javax.swing.JCheckBox();
        changeCount = new javax.swing.JLabel();

        setTitle("Change");
        setName(""); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridBagLayout());

        done.setText("Done");
        done.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                done(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(done, gridBagConstraints);

        apply.setText("Change");
        apply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                apply(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(apply, gridBagConstraints);

        listPane.setMinimumSize(new java.awt.Dimension(150, 22));
        listPane.setPreferredSize(new java.awt.Dimension(250, 22));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 11;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(listPane, gridBagConstraints);

        changeOption.add(changeSelected);
        changeSelected.setText("Change selected ones only");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        getContentPane().add(changeSelected, gridBagConstraints);

        changeOption.add(changeConnected);
        changeConnected.setText("Change all connected to this");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        getContentPane().add(changeConnected, gridBagConstraints);

        changeOption.add(changeInCell);
        changeInCell.setText("Change all in this cell");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        getContentPane().add(changeInCell, gridBagConstraints);

        changeOption.add(changeInLibrary);
        changeInLibrary.setText("Change all in this library");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        getContentPane().add(changeInLibrary, gridBagConstraints);

        changeOption.add(changeEverywhere);
        changeEverywhere.setText("Change all in all libraries");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 10, 4);
        getContentPane().add(changeEverywhere, gridBagConstraints);

        changeNodesWithArcs.setText("Change nodes with arcs");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 4, 4, 4);
        getContentPane().add(changeNodesWithArcs, gridBagConstraints);

        showPrimitives.setText("Show primitives");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(showPrimitives, gridBagConstraints);

        showCells.setText("Show cells");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(showCells, gridBagConstraints);

        ignorePortNames.setText("Ignore port names");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ignorePortNames, gridBagConstraints);

        jLabel1.setText("Library:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        getContentPane().add(jLabel1, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(librariesPopup, gridBagConstraints);

        allowMissingPorts.setText("Allow missing ports");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(allowMissingPorts, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(changeCount, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void done(java.awt.event.ActionEvent evt)//GEN-FIRST:event_done
	{//GEN-HEADEREND:event_done
		closeDialog(null);
	}//GEN-LAST:event_done

	private void apply(java.awt.event.ActionEvent evt)//GEN-FIRST:event_apply
	{//GEN-HEADEREND:event_apply
		doTheChange();
		libSelected = (String)librariesPopup.getSelectedItem();
	}//GEN-LAST:event_apply

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		Highlighter.removeHighlightListener(this);
		setVisible(false);
		dispose();
		theDialog = null;
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox allowMissingPorts;
    private javax.swing.JButton apply;
    private javax.swing.JRadioButton changeConnected;
    private javax.swing.JLabel changeCount;
    private javax.swing.JRadioButton changeEverywhere;
    private javax.swing.JRadioButton changeInCell;
    private javax.swing.JRadioButton changeInLibrary;
    private javax.swing.JCheckBox changeNodesWithArcs;
    private javax.swing.ButtonGroup changeOption;
    private javax.swing.JRadioButton changeSelected;
    private javax.swing.JButton done;
    private javax.swing.JCheckBox ignorePortNames;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JComboBox librariesPopup;
    private javax.swing.JScrollPane listPane;
    private javax.swing.JCheckBox showCells;
    private javax.swing.JCheckBox showPrimitives;
    // End of variables declaration//GEN-END:variables
}
