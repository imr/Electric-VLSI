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
package com.sun.electric.tool.user.dialogs;

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.swing.JComboBox;
import javax.swing.JRadioButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.DefaultListModel;
import javax.swing.ListSelectionModel;

/**
 * Class to handle the "Change" dialog.
 */
public class Change extends javax.swing.JDialog
{
	/** Change selected only. */				private static final int CHANGE_SELECTED = 1;
	/** Change all connected to this. */		private static final int CHANGE_CONNECTED = 2;
	/** Change all in this cell. */				private static final int CHANGE_CELL = 3;
	/** Change all in this Library. */			private static final int CHANGE_LIBRARY = 4;
	/** Change all in all Libraries. */			private static final int CHANGE_EVERYWHERE = 5;

	private static boolean nodesAndArcs = false;
	private static int whatToChange = CHANGE_SELECTED;
	private static Geometric geomToChange;
    private static String libSelected = null;
	private JList changeList;
	private DefaultListModel changeListModel;
	private List changeNodeProtoList;

	public static void showChangeDialog()
	{
		// first make sure something is selected
		List highs = Highlight.getHighlighted(true, true);
		Geometric geomToChange = null;
		if (highs.size() > 0) geomToChange = (Geometric)highs.get(0);
		if (geomToChange == null)
		{
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
				"Select an object before changing it.");
			return;
		}
		Change dialog = new Change(TopLevel.getCurrentJFrame(), true, geomToChange);
		dialog.show();
	}

	/** Creates new form Change */
	private Change(java.awt.Frame parent, boolean modal, Geometric geomToChange)
	{
		super(parent, modal);
		setLocation(100, 50);
		initComponents();
        getRootPane().setDefaultButton(ok);

		// build the change list
		changeListModel = new DefaultListModel();
		changeList = new JList(changeListModel);
		changeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listPane.setViewportView(changeList);
		changeNodeProtoList = new ArrayList();

		// make a popup of libraries
		List libList = Library.getVisibleLibrariesSortedByName();
        int curIndex = libList.indexOf(Library.getCurrent());
		for(Iterator it = libList.iterator(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			librariesPopup.addItem(lib.getLibName());
            if (lib.getLibName().equals(libSelected)) {
                curIndex = -1;                          // won't set to current library now
                librariesPopup.setSelectedItem(libSelected);
            }
		}
		if (curIndex >= 0) librariesPopup.setSelectedIndex(curIndex);
		librariesPopup.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { reload(); }
		});

		// find out what is going to be changed
		Change.geomToChange = geomToChange;
		if (geomToChange instanceof NodeInst)
		{
			librariesPopup.setEnabled(true);
			ignorePortNames.setEnabled(true);
			allowMissingPorts.setEnabled(true);
			showPrimitives.setEnabled(true);
			showCells.setEnabled(true);
			NodeInst ni = (NodeInst)geomToChange;
			if (ni.getProto() instanceof Cell)
			{
				showCells.setSelected(true);
//				us_curlib = ni->proto->lib;
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
			changeNodesWithArcs.setSelected(nodesAndArcs);
		}
		showPrimitives.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { reload(); }
		});
		showCells.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { reload(); }
		});
		changeNodesWithArcs.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { reload(); }
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
		reload();
	}

	private void whatToChangeChanged(ActionEvent evt)
	{
		JRadioButton src = (JRadioButton)evt.getSource();
		if (src == changeSelected) whatToChange = CHANGE_SELECTED; else
		if (src == changeConnected) whatToChange = CHANGE_CONNECTED; else
		if (src == changeInCell) whatToChange = CHANGE_CELL; else
		if (src == changeInLibrary) whatToChange = CHANGE_LIBRARY; else
		if (src == changeEverywhere) whatToChange = CHANGE_EVERYWHERE;
		if (whatToChange == CHANGE_EVERYWHERE)
		{
			if (geomToChange instanceof ArcInst)
			{
				if (changeNodesWithArcs.isSelected())
				{
					changeNodesWithArcs.setSelected(false);
					reload();
				}
				changeNodesWithArcs.setEnabled(false);
			}
		} else
		{
			if (geomToChange instanceof ArcInst)
				changeNodesWithArcs.setEnabled(true);
		}
	}

    private static final Pattern dummyName = Pattern.compile("(.*?)FROM(.*?)\\{(.*)");

	private void reload()
	{
		changeListModel.clear();
		changeNodeProtoList.clear();
		Technology curTech = Technology.getCurrent();
		if (geomToChange instanceof NodeInst)
		{
			if (showCells.isSelected())
			{
				// cell: only list other cells as replacements
				String libName = (String)librariesPopup.getSelectedItem();
				Library lib = Library.findLibrary(libName);
				List cells = lib.getCellsSortedByName();
				for(Iterator it = cells.iterator(); it.hasNext(); )
				{
					Cell cell = (Cell)it.next();
					changeListModel.addElement(cell.noLibDescribe());
					changeNodeProtoList.add(cell);
				}
//				(void)us_setscrolltocurrentcell(DCHG_ALTLIST, TRUE, FALSE, FALSE, FALSE, dia);
                changeList.setSelectedIndex(0);
                String geomName = ((NodeInst)geomToChange).getProto().describe();
                // if replacing dummy facet, name will be [cellname]FROM[libname][{view}]
                Matcher mat = dummyName.matcher(geomName);
                if (mat.matches()) {
                    // try to select items.  Nothing will happen if they are not in list.
                    changeList.setSelectedValue(mat.group(1)+"{"+mat.group(3), true);
                    librariesPopup.setSelectedItem(mat.group(2));
                } else {
                    // otherwise, try to match name
                    changeList.setSelectedValue(geomName, true);
                }
			}
			if (showPrimitives.isSelected())
			{
				// primitive: list primitives in this and the generic technology
				for(Iterator it = curTech.getNodes(); it.hasNext(); )
				{
					PrimitiveNode np = (PrimitiveNode)it.next();
					changeListModel.addElement(np.describe());
					changeNodeProtoList.add(np);
				}
				if (curTech != Generic.tech)
				{
					changeListModel.addElement("Generic:Universal-Pin");
					changeNodeProtoList.add(Generic.tech.universalPinNode);
					changeListModel.addElement("Generic:Invisible-Pin");
					changeNodeProtoList.add(Generic.tech.invisiblePinNode);
					changeListModel.addElement("Generic:Unrouted-Pin");
					changeNodeProtoList.add(Generic.tech.unroutedPinNode);
				}
                changeList.setSelectedIndex(0);
			}
		} else
		{
			// load arcs in current technology, arc's technology, and generic technology
			ArcInst ai = (ArcInst)geomToChange;
			PortProto pp1 = ai.getHead().getPortInst().getPortProto();
			PortProto pp2 = ai.getTail().getPortInst().getPortProto();
			for(Iterator it = curTech.getArcs(); it.hasNext(); )
			{
				PrimitiveArc ap = (PrimitiveArc)it.next();
				if (!changeNodesWithArcs.isSelected())
				{
					if (!pp1.connectsTo(ap)) continue;
					if (!pp2.connectsTo(ap)) continue;
				}
				changeListModel.addElement(ap.describe());
			}
			if (curTech != Generic.tech)
			{
				for(Iterator it = Generic.tech.getArcs(); it.hasNext(); )
				{
					PrimitiveArc ap = (PrimitiveArc)it.next();
					if (!changeNodesWithArcs.isSelected())
					{
						if (!pp1.connectsTo(ap)) continue;
						if (!pp2.connectsTo(ap)) continue;
					}
					changeListModel.addElement(ap.describe());
				}
			}
			Technology arcTech = ai.getProto().getTechnology();
			if (arcTech != curTech && arcTech != Generic.tech)
			{
				for(Iterator it = arcTech.getArcs(); it.hasNext(); )
				{
					PrimitiveArc ap = (PrimitiveArc)it.next();
					if (!changeNodesWithArcs.isSelected())
					{
						if (!pp1.connectsTo(ap)) continue;
						if (!pp2.connectsTo(ap)) continue;
					}
					changeListModel.addElement(ap.describe());
				}
			}
            changeList.setSelectedIndex(0);
		}
	}

	private void doTheChange()
	{
		// change the node/arc type
		ChangeObject job = new ChangeObject(this);
	}

	/**
	 * Class to change the node/arc type in a new thread.
	 */
	protected static class ChangeObject extends Job
	{
		Change dialog;

		protected ChangeObject(Change dialog)
		{
			super("Change type", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.dialog = dialog;
			startJob();
		}

		public void doIt()
		{
			// handle node replacement
			if (Change.geomToChange instanceof NodeInst)
			{
				// get node to be replaced
				NodeInst ni = (NodeInst)Change.geomToChange;

				// disallow replacing if lock is on
				if (CircuitChanges.cantEdit(ni.getParent(), ni, true)) return;

				// get nodeproto to replace it with
                String line = dialog.getLibSelected();
                Library library = Library.findLibrary(line);
                if (library == null) return;
                int index = dialog.changeList.getSelectedIndex();
                NodeProto np = (NodeProto)dialog.changeNodeProtoList.get(index);
//				line = (String)dialog.changeList.getSelectedValue();
//                NodeProto np = NodeProto.findNodeProto(line);
				if (np == null) return;

				// sanity check
				NodeProto oldNType = ni.getProto();
				if (oldNType == np)
				{
					System.out.println("Node already of type " + np.describe());
					return;
				}

				// get any arguments to the replace
				boolean ignorePortNames = dialog.ignorePortNames.isSelected();
				boolean allowMissingPorts = dialog.allowMissingPorts.isSelected();

				// clear highlighting
				Highlight.clear();
				Highlight.finished();

				// replace the nodeinsts
				NodeInst onlyNewNi = CircuitChanges.replaceNodeInst(ni, np, ignorePortNames, allowMissingPorts);
				if (onlyNewNi == null)
				{
					System.out.println(np.describe() + " does not fit in the place of " + oldNType.describe());
					return;
				}

				// do additional replacements if requested
				int total = 1;
				if (dialog.changeEverywhere.isSelected())
				{
					// replace in all cells of library if requested
					for(Iterator it = Library.getLibraries(); it.hasNext(); )
					{
						Library lib = (Library)it.next();
						for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
						{
							Cell cell = (Cell)cIt.next();
							boolean found = true;
							while (found)
							{
								found = false;
								for(Iterator nIt = cell.getNodes(); nIt.hasNext(); )
								{
									NodeInst lNi = (NodeInst)nIt.next();
									if (lNi.getProto() != oldNType) continue;

									// do not replace the example icon
									if (lNi.isIconOfParent())
									{
										System.out.println("Example icon in cell " + cell.describe() + " not replaced");
										continue;
									}

									// disallow replacing if lock is on
									if (CircuitChanges.cantEdit(cell, lNi, true)) continue;

									NodeInst newNi = CircuitChanges.replaceNodeInst(lNi, np, ignorePortNames, allowMissingPorts);
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
					System.out.println("All " + total + " " + oldNType.describe() +
						" nodes in all libraries replaced with " + np.describe());
				} else if (dialog.changeInLibrary.isSelected())
				{
					// replace throughout this library if requested
					Library lib = Library.getCurrent();
					for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
					{
						Cell cell = (Cell)cIt.next();
						boolean found = true;
						while (found)
						{
							found = false;
							for(Iterator nIt = cell.getNodes(); nIt.hasNext(); )
							{
								NodeInst lNi = (NodeInst)nIt.next();
								if (lNi.getProto() != oldNType) continue;

								// disallow replacing if lock is on
								if (CircuitChanges.cantEdit(cell, lNi, true)) continue;

								NodeInst newNi = CircuitChanges.replaceNodeInst(lNi, np, ignorePortNames, allowMissingPorts);
								if (newNi != null)
								{
									total++;
									found = true;
									break;
								}
							}
						}
					}
					System.out.println("All " + total + " " + oldNType.describe() +
						" nodes in library " + lib.getLibName() + " replaced with " + np.describe());
				} else if (dialog.changeInCell.isSelected())
				{
					// replace throughout this cell if "requested
					Cell cell = WindowFrame.getCurrentCell();
					boolean found = true;
					while (found)
					{
						found = false;
						for(Iterator nIt = cell.getNodes(); nIt.hasNext(); )
						{
							NodeInst lNi = (NodeInst)nIt.next();
							if (lNi.getProto() != oldNType) continue;

							// disallow replacing if lock is on
							if (CircuitChanges.cantEdit(cell, lNi, true)) continue;

							NodeInst newNi = CircuitChanges.replaceNodeInst(lNi, np, ignorePortNames, allowMissingPorts);
							if (newNi != null)
							{
								total++;
								found = true;
								break;
							}
						}
					}
					System.out.println("All " + total + " " + oldNType.describe() + " nodes in cell " +
						cell.describe() + " replaced with " + np.describe());
				} else if (dialog.changeConnected.isSelected())
				{
					// replace all connected to this in the cell if requested
					Cell curCell = WindowFrame.getCurrentCell();
					Netlist netlist = curCell.getUserNetlist();
					List others = new ArrayList();
					for(Iterator it = curCell.getNodes(); it.hasNext(); )
					{
						NodeInst lNi = (NodeInst)it.next();
						if (lNi.getProto() != oldNType) continue;
						if (lNi == onlyNewNi) continue;

						boolean found = false;
						for(Iterator pIt = onlyNewNi.getPortInsts(); pIt.hasNext(); )
						{
							PortInst pi = (PortInst)pIt.next();
							for(Iterator lPIt = lNi.getPortInsts(); lPIt.hasNext(); )
							{
								PortInst lPi = (PortInst)lPIt.next();
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
					for(Iterator it = others.iterator(); it.hasNext(); )
					{
						NodeInst lNi = (NodeInst)it.next();

						// disallow replacing if lock is on
						if (CircuitChanges.cantEdit(curCell, lNi, true)) continue;

						NodeInst newNi = CircuitChanges.replaceNodeInst(lNi, np, ignorePortNames, allowMissingPorts);
						if (newNi != null)
						{
							total++;
						}
					}
					System.out.println("All " + total + " " + oldNType.describe() +
						" nodes connected to this replaced with " + np.describe());
				} else System.out.println("Node " + oldNType.describe() + " replaced with " + np.describe());
			} else
			{
				// get arc to be replaced
				ArcInst ai = (ArcInst)Change.geomToChange;

				// disallow replacement if lock is on
				if (CircuitChanges.cantEdit(ai.getParent(), null, true)) return;

				String line = (String)dialog.changeList.getSelectedValue();
				ArcProto ap = ArcProto.findArcProto(line);
				if (ap == null)
				{
					System.out.println("Nothing called '" + line + "'");
					return;
				}

				// sanity check
				ArcProto oldAType = ai.getProto();
				if (oldAType == ap)
				{
					System.out.println("Arc already of type " + ap.describe());
					return;
				}

				// special case when replacing nodes, too
				if (dialog.changeNodesWithArcs.isSelected())
				{
					if (dialog.changeInLibrary.isSelected())
					{
						for(Iterator it = Library.getCurrent().getCells(); it.hasNext(); )
						{
							Cell cell = (Cell)it.next();
							replaceAllArcs(cell, ai, ap, false, true);
						}
					} else
					{
						replaceAllArcs(ai.getParent(), ai, ap, dialog.changeConnected.isSelected(),
							dialog.changeInCell.isSelected());
					}
					return;
				}

				// remove highlighting
				Highlight.clear();
				Highlight.finished();

				// replace the arcinst
				ArcInst onlyNewAi = ai.replace(ap);
				if (onlyNewAi == null)
				{
					System.out.println(ap.describe() + " does not fit in the place of " + oldAType.describe());
					return;
				}

				// do additional replacements if requested
				int total = 1;
				if (dialog.changeEverywhere.isSelected())
				{
					// replace in all cells of library if requested
					for(Iterator it = Library.getLibraries(); it.hasNext(); )
					{
						Library lib = (Library)it.next();
						for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
						{
							Cell cell = (Cell)cIt.next();
							boolean found = true;
							while (found)
							{
								found = false;
								for(Iterator nIt = cell.getArcs(); nIt.hasNext(); )
								{
									ArcInst lAi = (ArcInst)nIt.next();
									if (lAi.getProto() != oldAType) continue;

									// disallow replacing if lock is on
									if (CircuitChanges.cantEdit(cell, null, true)) continue;

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
						" arcs in the library replaced with " + ap.describe());
				} else if (dialog.changeInLibrary.isSelected())
				{
					// replace throughout this library if requested
					Library lib = Library.getCurrent();
					for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
					{
						Cell cell = (Cell)cIt.next();
						boolean found = true;
						while (found)
						{
							found = false;
							for(Iterator nIt = cell.getArcs(); nIt.hasNext(); )
							{
								ArcInst lAi = (ArcInst)nIt.next();
								if (lAi.getProto() != oldAType) continue;

								// disallow replacing if lock is on
								if (CircuitChanges.cantEdit(cell, null, true)) continue;

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
						" arcs in library " + lib.getLibName() + " replaced with " + ap.describe());
				} else if (dialog.changeInCell.isSelected())
				{
					// replace throughout this cell if requested
					Cell cell = WindowFrame.getCurrentCell();
					boolean found = true;
					while (found)
					{
						found = false;
						for(Iterator nIt = cell.getArcs(); nIt.hasNext(); )
						{
							ArcInst lAi = (ArcInst)nIt.next();
							if (lAi.getProto() != oldAType) continue;

							// disallow replacing if lock is on
							if (CircuitChanges.cantEdit(cell, null, true)) continue;

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
						" arcs in cell " + cell.describe() + " replaced with " + ap.describe());
				} else if (dialog.changeConnected.isSelected())
				{
					// replace all connected to this if requested
					List others = new ArrayList();
					Cell cell = WindowFrame.getCurrentCell();
					Netlist netlist = cell.getUserNetlist();
					for(Iterator it = cell.getArcs(); it.hasNext(); )
					{
						ArcInst lAi = (ArcInst)it.next();
						if (lAi == onlyNewAi) continue;
						if (netlist.sameNetwork(onlyNewAi, lAi)) others.add(lAi);
					}

					for(Iterator it = others.iterator(); it.hasNext(); )
					{
						ArcInst lAi = (ArcInst)it.next();
						ArcInst newAi = lAi.replace(ap);
						if (newAi != null)
						{
							total++;
						}
					}
					System.out.println("All " + total + " " + oldAType.describe() +
						" arcs connected to this replaced with " + ap.describe());
				} else System.out.println("Arc " + oldAType.describe() + " replaced with " +ap.describe());
			}
		}

		/**
		 * Method to replace arcs in "list" (that match the first arc there) with another of type
		 * "ap", adding layer-change contacts
		 * as needed to keep the connections.  If "connected" is true, replace all such arcs
		 * connected to this.  If "thiscell" is true, replace all such arcs in the cell.
		 */
		private void replaceAllArcs(Cell cell, ArcInst oldAi, ArcProto ap, boolean connected, boolean thiscell)
		{
			List highs = Highlight.getHighlighted(true, true);
			FlagSet marked = Geometric.getFlagSet(1);

			// mark the pin nodes that must be changed
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				ni.clearBit(marked);
				ni.setTempObj(null);
			}
			for(Iterator it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				ai.clearBit(marked);
			}

			for(Iterator it = highs.iterator(); it.hasNext(); )
			{
				Geometric geom = (Geometric)it.next();
				if (!(geom instanceof ArcInst)) continue;
				ArcInst ai = (ArcInst)geom;
				if (ai.getProto() != oldAi.getProto()) continue;
				ai.setBit(marked);
			}
			if (connected)
			{
				Netlist netlist = cell.getUserNetlist();
				for(Iterator it = cell.getArcs(); it.hasNext(); )
				{
					ArcInst ai = (ArcInst)it.next();
					if (ai.getProto() != oldAi.getProto()) continue;
					if (!netlist.sameNetwork(ai, oldAi)) continue;
					ai.setBit(marked);
				}
			}
			if (thiscell)
			{
				for(Iterator it = cell.getArcs(); it.hasNext(); )
				{
					ArcInst ai = (ArcInst)it.next();
					if (ai.getProto() != oldAi.getProto()) continue;
					ai.setBit(marked);
				}
			}
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (ni.getProto() instanceof Cell) continue;
				if (ni.getNumExports() != 0) continue;
				if (ni.getFunction() != NodeProto.Function.PIN) continue;
				boolean allArcs = true;
				for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
				{
					Connection con = (Connection)cIt.next();
					if (!con.getArc().isBit(marked)) { allArcs = false;   break; }
				}
				if (allArcs) ni.setBit(marked);
			}

			// now create new pins where they belong
			PrimitiveNode pin = ((PrimitiveArc)ap).findPinProto();
			double xS = pin.getDefWidth();
			double yS = pin.getDefHeight();
			List dupPins = new ArrayList();
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (!ni.isBit(marked)) continue;
				dupPins.add(ni);
			}
			for(Iterator it = dupPins.iterator(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();

				NodeInst newNi = NodeInst.makeInstance(pin, ni.getGrabCenter(), xS, yS, 0, cell, null);
				if (newNi == null) return;
				newNi.clearBit(marked);
				ni.setTempObj(newNi);
			}

			// now create new arcs to replace the old ones
			List dupArcs = new ArrayList();
			for(Iterator it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				if (ai.isBit(marked)) dupArcs.add(ai);
			}
			for(Iterator it = dupArcs.iterator(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();

				NodeInst ni0 = ai.getHead().getPortInst().getNodeInst();
				PortInst pi0 = null;
				if (ni0.getTempObj() != null)
				{
					ni0 = (NodeInst)ni0.getTempObj();
					pi0 = ni0.getOnlyPortInst();
				} else
				{
					// need contacts to get to the right level
					pi0 = makeContactStack(ai, 0, ap);
					if (pi0 == null) return;
				}
				NodeInst ni1 = ai.getTail().getPortInst().getNodeInst();
				PortInst pi1 = null;
				if (ni1.getTempObj() != null)
				{
					ni1 = (NodeInst)ni1.getTempObj();
					pi1 = ni1.getOnlyPortInst();
				} else
				{
					// need contacts to get to the right level
					pi1 = makeContactStack(ai, 1, ap);
					if (pi1 == null) return;
				}

				double wid = ap.getDefaultWidth();
				if (ai.getWidth() > wid) wid = ai.getWidth();
				ArcInst newAi = ArcInst.makeInstance(ap, wid, pi0, ai.getHead().getLocation(),
					pi1, ai.getTail().getLocation(), null);
				if (newAi == null) return;
				ai.copyVars(newAi);
				newAi.clearBit(marked);
			}

			// now remove the previous arcs and nodes
			List killArcs = new ArrayList();
			for(Iterator it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				if (ai.isBit(marked)) killArcs.add(ai);
			}
			for(Iterator it = killArcs.iterator(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				ai.kill();
			}

			List killNodes = new ArrayList();
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (ni.isBit(marked)) killNodes.add(ni);
			}
			for(Iterator it = killNodes.iterator(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				ni.kill();
			}
			marked.freeFlagSet();
		}

		NodeProto [] contactStack = new NodeProto[100];
		ArcProto [] contactStackArc = new ArcProto[100];

		/**
		 * Method to examine end "end" of arc "ai" and return a node at that position which
		 * can connect to arcs of type "ap".  This may require creation of one or more contacts
		 * to change layers.
		 */
		private PortInst makeContactStack(ArcInst ai, int end, ArcProto ap)
		{
			NodeInst lastNi = ai.getConnection(end).getPortInst().getNodeInst();
			PortProto lastPp = ai.getConnection(end).getPortInst().getPortProto();
			FlagSet marked = NodeProto.getFlagSet(1);
			for(Iterator it = ap.getTechnology().getNodes(); it.hasNext(); )
			{
				PrimitiveNode np = (PrimitiveNode)it.next();
				np.clearBit(marked);
			}
			int depth = findPathToArc(lastPp, ap, 0, marked);
			if (depth < 0) return null;

			// create the contacts
			Cell cell = ai.getParent();
			PortInst retPi = lastNi.findPortInstFromProto(lastPp);
			Point2D center = ai.getConnection(end).getLocation();
			for(int i=0; i<depth; i++)
			{
				double xS = contactStack[i].getDefWidth();
				double yS = contactStack[i].getDefHeight();
				NodeInst newNi = NodeInst.makeInstance(contactStack[i], center, xS, yS, 0, cell, null);
				if (newNi == null) return null;
				PortInst thisPi = newNi.findPortInstFromProto(contactStack[i].getPort(0));

				ArcProto typ = contactStackArc[i];
				double wid = typ.getDefaultWidth();
				ArcInst newAi = ArcInst.makeInstance(typ, wid, thisPi, retPi, null);
				retPi = thisPi;
				if (newAi == null) return null;
			}
			marked.freeFlagSet();
			return retPi;
		}

		int findPathToArc(PortProto pp, ArcProto ap, int depth, FlagSet marked)
		{
			// see if the connection is made
			if (pp.connectsTo(ap)) return depth;

			// look for a contact
			NodeProto bestNp = null;
			ArcProto bestAp = null;
			int bestDepth = 0;
			Technology tech = ap.getTechnology();
			for(Iterator it = tech.getNodes(); it.hasNext(); )
			{
				PrimitiveNode nextNp = (PrimitiveNode)it.next();
				if (nextNp.isBit(marked)) continue;
				NodeProto.Function fun = nextNp.getFunction();
				if (fun != NodeProto.Function.CONTACT) continue;

				// see if this contact connects to the destination
				PortProto nextPp = nextNp.getPort(0);
				ArcProto [] connections = nextPp.getBasePort().getConnections();
				ArcProto found = null;
				for(int i=0; i<connections.length; i++)
				{
					ArcProto thisAp = connections[i];
					if (thisAp.getTechnology() != tech) continue;
					if (pp.connectsTo(thisAp)) { found = thisAp;   break; }
				}
				if (found == null) continue;

				// this contact is part of the chain
				contactStack[depth] = nextNp;
				nextNp.setBit(marked);
				int newDepth = findPathToArc(nextPp, ap, depth+1, marked);
				nextNp.clearBit(marked);
				if (newDepth < 0) continue;
				if (bestNp == null || newDepth < bestDepth)
				{
					bestDepth = newDepth;
					bestNp = nextNp;
					bestAp = found;
				}
			}
			if (bestNp != null)
			{
				contactStack[depth] = bestNp;
				contactStackArc[depth] = bestAp;
				bestNp.setBit(marked);
				int newDepth = findPathToArc(bestNp.getPort(0), ap, depth+1, marked);
				bestNp.clearBit(marked);
				return newDepth;
			}
			return -1;
		}

	}

    protected String getLibSelected()
    {
        return (String)librariesPopup.getSelectedItem();
    }

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        changeOption = new javax.swing.ButtonGroup();
        cancel = new javax.swing.JButton();
        ok = new javax.swing.JButton();
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

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Change");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cancel(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(cancel, gridBagConstraints);

        ok.setText("OK");
        ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                ok(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);

        listPane.setMinimumSize(new java.awt.Dimension(150, 22));
        listPane.setPreferredSize(new java.awt.Dimension(150, 22));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(listPane, gridBagConstraints);

        changeSelected.setText("Change selected ones only");
        changeOption.add(changeSelected);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        getContentPane().add(changeSelected, gridBagConstraints);

        changeConnected.setText("Change all connected to this");
        changeOption.add(changeConnected);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        getContentPane().add(changeConnected, gridBagConstraints);

        changeInCell.setText("Change all in this cell");
        changeOption.add(changeInCell);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        getContentPane().add(changeInCell, gridBagConstraints);

        changeInLibrary.setText("Change all in this library");
        changeOption.add(changeInLibrary);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        getContentPane().add(changeInLibrary, gridBagConstraints);

        changeEverywhere.setText("Change all in all libraries");
        changeOption.add(changeEverywhere);
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
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 4, 4, 4);
        getContentPane().add(changeNodesWithArcs, gridBagConstraints);

        showPrimitives.setText("Show primitives");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(showPrimitives, gridBagConstraints);

        showCells.setText("Show cells");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(showCells, gridBagConstraints);

        ignorePortNames.setText("Ignore port names");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ignorePortNames, gridBagConstraints);

        jLabel1.setText("Library:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        getContentPane().add(jLabel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(librariesPopup, gridBagConstraints);

        allowMissingPorts.setText("Allow missing ports");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(allowMissingPorts, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void cancel(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancel
	{//GEN-HEADEREND:event_cancel
		closeDialog(null);
	}//GEN-LAST:event_cancel

	private void ok(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ok
	{//GEN-HEADEREND:event_ok
		doTheChange();
        libSelected = (String)librariesPopup.getSelectedItem();
		closeDialog(null);
	}//GEN-LAST:event_ok

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox allowMissingPorts;
    private javax.swing.JButton cancel;
    private javax.swing.JRadioButton changeConnected;
    private javax.swing.JRadioButton changeEverywhere;
    private javax.swing.JRadioButton changeInCell;
    private javax.swing.JRadioButton changeInLibrary;
    private javax.swing.JCheckBox changeNodesWithArcs;
    private javax.swing.ButtonGroup changeOption;
    private javax.swing.JRadioButton changeSelected;
    private javax.swing.JCheckBox ignorePortNames;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JComboBox librariesPopup;
    private javax.swing.JScrollPane listPane;
    private javax.swing.JButton ok;
    private javax.swing.JCheckBox showCells;
    private javax.swing.JCheckBox showPrimitives;
    // End of variables declaration//GEN-END:variables
	
}
