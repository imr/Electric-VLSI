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
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.CircuitChangeJobs;
import com.sun.electric.tool.user.HighlightListener;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

/**
 * Class to handle the "Change" dialog.
 */
public class Change extends EDialog implements HighlightListener
{
	/** Change selected only. */				private static final int CHANGE_SELECTED = 1;
	/** Change all connected to this. */		private static final int CHANGE_CONNECTED = 2;
	/** Change all in this cell. */				private static final int CHANGE_CELL = 3;
	/** Change all in this Library. */			private static final int CHANGE_LIBRARY = 4;
	/** Change all in all Libraries. */			private static final int CHANGE_EVERYWHERE = 5;

	private static Change theDialog = null;
	private static boolean nodesAndArcs = false;
	private static int whatToChange = CHANGE_SELECTED;
	private List<Geometric> geomsToChange;                  // List of Geometrics to change
    private static String libSelected = null;
	private JList changeList;
	private DefaultListModel changeListModel;
	private List<NodeProto> changeNodeProtoList;
    private EditWindow wnd;

	public static void showChangeDialog()
	{
		if (theDialog == null)
		{
			JFrame jf = null;
			if (TopLevel.isMDIMode())
				jf = TopLevel.getCurrentJFrame();
			theDialog = new Change(jf, false);
		}
		theDialog.loadInfo(true);
		theDialog.setVisible(true);
	}

	/** Creates new form Change */
	private Change(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();

		// build the change list
		changeListModel = new DefaultListModel();
		changeList = new JList(changeListModel);
		changeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listPane.setViewportView(changeList);
		changeNodeProtoList = new ArrayList<NodeProto>();
		changeList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2) apply(null);
			}
		});

		// make a popup of libraries
		List<Library> libList = Library.getVisibleLibraries();
        int curIndex = libList.indexOf(Library.getCurrent());
		for(Library lib: libList)
		{
			librariesPopup.addItem(lib.getName());
            if (lib.getName().equals(libSelected))
            {
                curIndex = -1;                          // won't set to current library now
                librariesPopup.setSelectedItem(libSelected);
            }
		}
		if (curIndex >= 0) librariesPopup.setSelectedIndex(curIndex);
		librariesPopup.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { reload(false); }
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
			for (Geometric geom : highs)
			{
				geomsToChange.add(geom);
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
			changeNodesWithArcs.setSelected(nodesAndArcs);
		}

		reload(true);
	}

	private boolean dontReload = false;

	/**
	 * Method called when dialog controls have changed.
	 * Makes sure the displayed lists and options are correct.
	 */
	private void reload(boolean canSwitchLibraries)
	{
		if (dontReload) return;

		changeListModel.clear();
		changeNodeProtoList.clear();
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

				String libName = (String)librariesPopup.getSelectedItem();
				Library lib = Library.findLibrary(libName);
				for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
				{
					Cell cell = it.next();
					changeListModel.addElement(cell.noLibDescribe());
					changeNodeProtoList.add(cell);
				}
			}
			if (showPrimitives.isSelected())
			{
				// primitive: list primitives in this and the generic technology
				for(PrimitiveNode np : curTech.getNodesSortedByName())
				{
					changeListModel.addElement(np.describe(false));
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
			}
            changeList.setSelectedIndex(0);
            // try to select prototype of selected node
            if (ni.isCellInstance()) {
                Cell c = (Cell)ni.getProto();
                for (int i=0; i<changeListModel.getSize(); i++) {
                    String str = (String)changeListModel.get(i);
                    if (str.equals(c.noLibDescribe())) {
                        changeList.setSelectedIndex(i);
                        break;
                    }
                }
            } else {
                for (int i=0; i<changeListModel.getSize(); i++) {
                    String str = (String)changeListModel.get(i);
                    if (str.equals(ni.getProto().describe(false))) {
                        changeList.setSelectedIndex(i);
                        break;
                    }
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
					changeList.setSelectedValue(mat.group(1) + "{" + mat.group(3), true);
					librariesPopup.setSelectedItem(mat.group(2));
				} else
				{
					// otherwise, try to match name
					changeList.setSelectedValue(geomName, true);
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
				if (!changeNodesWithArcs.isSelected())
				{
					if (!pp1.connectsTo(ap)) continue;
					if (!pp2.connectsTo(ap)) continue;
				}
				changeListModel.addElement(ap.describe());
			}
			if (curTech != Generic.tech)
			{
				for(Iterator<ArcProto> it = Generic.tech.getArcs(); it.hasNext(); )
				{
					ArcProto ap = it.next();
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
				for(Iterator<ArcProto> it = arcTech.getArcs(); it.hasNext(); )
				{
					ArcProto ap = it.next();
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
		SwingUtilities.invokeLater(new Runnable() {
            public void run() { centerSelection(changeList); }});
	}

	private void doTheChange()
	{
		NodeProto np = null;
		ArcProto ap = null;
		Geometric geomToChange = geomsToChange.get(0);
		if (geomToChange instanceof NodeInst)
		{
			int index = changeList.getSelectedIndex();
	        np = changeNodeProtoList.get(index);
		} else
		{
	        String line = (String)changeList.getSelectedValue();
			ap = ArcProto.findArcProto(line);
			if (ap == null)
			{
				System.out.println("Nothing called '" + line + "'");
				return;
			}
		}

		List<Geometric> highs = wnd.getHighlighter().getHighlightedEObjs(true, true);

		ChangeObject job = new ChangeObject(
			geomsToChange, highs,
			getLibSelected(),
			np, ap,
			ignorePortNames.isSelected(),
			allowMissingPorts.isSelected(),
			changeNodesWithArcs.isSelected(),
			changeInCell.isSelected(),
			changeInLibrary.isSelected(),
			changeEverywhere.isSelected(),
			changeConnected.isSelected()
			);
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
		private boolean ignorePortNames, allowMissingPorts, changeNodesWithArcs;
		private boolean changeInCell, changeInLibrary, changeEverywhere, changeConnected;
		private List<Geometric> highlightThese;

		protected ChangeObject(
			List<Geometric> geomsToChange,
			List<Geometric> highs,
			String libName,
			NodeProto np, ArcProto ap,
			boolean ignorePortNames,
			boolean allowMissingPorts,
			boolean changeNodesWithArcs,
			boolean changeInCell,
			boolean changeInLibrary,
			boolean changeEverywhere,
			boolean changeConnected)
		{
			super("Change type", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.geomsToChange = geomsToChange;
			this.highs = highs;
			this.libName = libName;
			this.np = np;
			this.ap = ap;
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

            for (Geometric geomToChange : geomsToChange)
			{
				// handle node replacement
				if (geomToChange instanceof NodeInst)
				{
					// get node to be replaced
					NodeInst ni = (NodeInst)geomToChange;

					// disallow replacing if lock is on
					if (CircuitChangeJobs.cantEdit(ni.getParent(), ni, true) != 0) return false;

					// get nodeproto to replace it with
	                Library library = Library.findLibrary(libName);
	                if (library == null) return false;
					if (np == null) return false;

					// sanity check
					NodeProto oldNType = ni.getProto();
					if (oldNType == np)
					{
						System.out.println("Node already of type " + np.describe(true));
                        // just skip this case. No need to redo it. This not an error.
                        continue;
//						return false;
					}

					// replace the nodeinsts
					NodeInst onlyNewNi = CircuitChangeJobs.replaceNodeInst(ni, np, ignorePortNames, allowMissingPorts);
					if (onlyNewNi == null)
					{
						JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
							np + " does not fit in the place of " + oldNType,
							"Change failed", JOptionPane.ERROR_MESSAGE);
						return false;
					}
					highlightThese.add(onlyNewNi);

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
										int errorCode = CircuitChangeJobs.cantEdit(cell, lNi, true);
										if (errorCode < 0) return false;
										if (errorCode > 0) continue;

										NodeInst newNi = CircuitChangeJobs.replaceNodeInst(lNi, np, ignorePortNames, allowMissingPorts);
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
						System.out.println("All " + total + " " + oldNType.describe(true) +
							" nodes in all libraries replaced with " + np);
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
								for(Iterator<NodeInst> nIt = cell.getNodes(); nIt.hasNext(); )
								{
									NodeInst lNi = nIt.next();
									if (lNi.getProto() != oldNType) continue;

									// disallow replacing if lock is on
									int errorCode = CircuitChangeJobs.cantEdit(cell, lNi, true);
									if (errorCode < 0) return false;
									if (errorCode > 0) continue;

									NodeInst newNi = CircuitChangeJobs.replaceNodeInst(lNi, np, ignorePortNames, allowMissingPorts);
									if (newNi != null)
									{
										total++;
										found = true;
										break;
									}
								}
							}
						}
						System.out.println("All " + total + " " + oldNType.describe(true) +
							" nodes in " + lib + " replaced with " + np);
					} else if (changeInCell)
					{
						// replace throughout this cell if "requested
						Cell cell = WindowFrame.getCurrentCell();
						boolean found = true;
						while (found)
						{
							found = false;
							for(Iterator<NodeInst> nIt = cell.getNodes(); nIt.hasNext(); )
							{
								NodeInst lNi = nIt.next();
								if (lNi.getProto() != oldNType) continue;

								// disallow replacing if lock is on
								int errorCode = CircuitChangeJobs.cantEdit(cell, lNi, true);
								if (errorCode < 0) return false;
								if (errorCode > 0) continue;

								NodeInst newNi = CircuitChangeJobs.replaceNodeInst(lNi, np, ignorePortNames, allowMissingPorts);
								if (newNi != null)
								{
									total++;
									found = true;
									break;
								}
							}
						}
						System.out.println("All " + total + " " + oldNType.describe(true) + " nodes in " +
							cell + " replaced with " + np);
					} else if (changeConnected)
					{
						// replace all connected to this in the cell if requested
						Cell curCell = WindowFrame.getCurrentCell();
						Netlist netlist = curCell.getUserNetlist();
						List<NodeInst> others = new ArrayList<NodeInst>();
						for(Iterator<NodeInst> it = curCell.getNodes(); it.hasNext(); )
						{
							NodeInst lNi = it.next();
							if (lNi.getProto() != oldNType) continue;
							if (lNi == onlyNewNi) continue;

							boolean found = false;
							for(Iterator<PortInst> pIt = onlyNewNi.getPortInsts(); pIt.hasNext(); )
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
							int errorCode = CircuitChangeJobs.cantEdit(curCell, lNi, true);
							if (errorCode < 0) return false;
							if (errorCode > 0) continue;

							NodeInst newNi = CircuitChangeJobs.replaceNodeInst(lNi, np, ignorePortNames, allowMissingPorts);
							if (newNi != null)
							{
								total++;
							}
						}
						System.out.println("All " + total + " " + oldNType.describe(true) +
							" nodes connected to this replaced with " + np);
					} else System.out.println(oldNType + " replaced with " + np);
				} else
				{
					// get arc to be replaced
					ArcInst ai = (ArcInst)geomToChange;

					// disallow replacement if lock is on
					if (CircuitChangeJobs.cantEdit(ai.getParent(), null, true) != 0) return false;

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
										int errorCode = CircuitChangeJobs.cantEdit(cell, null, true);
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
									int errorCode = CircuitChangeJobs.cantEdit(cell, null, true);
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
						Cell cell = WindowFrame.getCurrentCell();
						boolean found = true;
						while (found)
						{
							found = false;
							for(Iterator<ArcInst> nIt = cell.getArcs(); nIt.hasNext(); )
							{
								ArcInst lAi = nIt.next();
								if (lAi.getProto() != oldAType) continue;

								// disallow replacing if lock is on
								int errorCode = CircuitChangeJobs.cantEdit(cell, null, true);
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
						Cell cell = WindowFrame.getCurrentCell();
						Netlist netlist = cell.getUserNetlist();
						for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
						{
							ArcInst lAi = it.next();
							if (lAi == onlyNewAi) continue;
							if (netlist.sameNetwork(onlyNewAi, lAi)) others.add(lAi);
						}

						for(ArcInst lAi : others)
						{
							ArcInst newAi = lAi.replace(ap);
							if (newAi != null)
							{
								total++;
							}
						}
						System.out.println("All " + total + " " + oldAType.describe() +
							" arcs connected to this replaced with " + ap);
					} else System.out.println(oldAType + " replaced with " +ap);
				}
            }
			return true;
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
            }
        }

		/**
		 * Method to replace arc "oldAi" with another of type "ap", adding layer-change contacts
		 * as needed to keep the connections.  If "connected" is true, replace all such arcs
		 * connected to this.  If "thiscell" is true, replace all such arcs in the cell.
		 */
		private void replaceAllArcs(Cell cell, List<Geometric> highs, ArcInst oldAi, ArcProto ap, boolean connected, boolean thiscell)
		{
			HashSet<Geometric> geomMarked = new HashSet<Geometric>();

			for(Geometric geom : highs)
			{
				if (!(geom instanceof ArcInst)) continue;
				ArcInst ai = (ArcInst)geom;
				if (ai.getProto() != oldAi.getProto()) continue;
				geomMarked.add(ai);
			}
			if (connected)
			{
				Netlist netlist = cell.getUserNetlist();
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
				if (ni.getFunction() != PrimitiveNode.Function.PIN) continue;
				boolean allArcs = true;
				for(Iterator<Connection> cIt = ni.getConnections(); cIt.hasNext(); )
				{
					Connection con = cIt.next();
					if (!geomMarked.contains(con.getArc())) { allArcs = false;   break; }
				}
				if (ni.hasConnections() && allArcs) geomMarked.add(ni);
//				if (ni.getNumConnections() != 0 && allArcs) geomMarked.add(ni);
			}

			// now create new pins where they belong
			PrimitiveNode pin = ap.findOverridablePinProto();
			double xS = pin.getDefWidth();
			double yS = pin.getDefHeight();
			List<NodeInst> dupPins = new ArrayList<NodeInst>();
			for(Geometric geom : geomMarked)
			{
				if (geom instanceof NodeInst)
					dupPins.add((NodeInst)geom);
			}
			HashMap<NodeInst,NodeInst> newNodes = new HashMap<NodeInst,NodeInst>();
			for(NodeInst ni : dupPins)
			{
				// TODO this used to provide the node name (ni.getName()) in the 2nd to last argument
				NodeInst newNi = NodeInst.makeInstance(pin, ni.getAnchorCenter(), xS, yS, cell, Orientation.IDENT, null, 0);
				if (newNi == null) return;
				geomMarked.remove(newNi);
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
				NodeInst ni0 = ai.getHeadPortInst().getNodeInst();
				PortInst pi0 = null;
				NodeInst newNi0 = newNodes.get(ni0);
				if (newNi0 != null)
				{
					ni0 = newNi0;
					pi0 = ni0.getOnlyPortInst();
				} else
				{
					// need contacts to get to the right level
					pi0 = makeContactStack(ai, ArcInst.HEADEND, ap);
					if (pi0 == null) return;
				}
				NodeInst ni1 = ai.getTailPortInst().getNodeInst();
				PortInst pi1 = null;
				NodeInst newNi1 = newNodes.get(ni1);
				if (newNi1 != null)
				{
					ni1 = newNi1;
					pi1 = ni1.getOnlyPortInst();
				} else
				{
					// need contacts to get to the right level
					pi1 = makeContactStack(ai, ArcInst.TAILEND, ap);
					if (pi1 == null) return;
				}

				double wid = ap.getDefaultWidth();
				if (ai.getWidth() > wid) wid = ai.getWidth();
				ArcInst newAi = ArcInst.makeInstance(ap, wid, pi0, pi1, ai.getHeadLocation(),
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
			for(NodeInst ni : dupPins)
			{
				NodeInst newNi = newNodes.get(ni);
				if (!ni.hasExports())
//				if (ni.getNumExports() == 0)
				{
					String niName = ni.getName();
					ni.kill();
					newNi.setName(niName);
				}
			}

//			List<NodeInst> killNodes = new ArrayList<NodeInst>();
//			for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
//			{
//				NodeInst ni = it.next();
//				if (geomMarked.contains(ni)) killNodes.add(ni);
//			}
//			for(NodeInst ni : killNodes)
//			{
//				if (ni.getNumExports() == 0)
//					ni.kill();
//			}
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
			NodeInst lastNi = ai.getPortInst(end).getNodeInst();
			PortProto lastPp = ai.getPortInst(end).getPortProto();
			Set<PrimitiveNode> marked = new HashSet<PrimitiveNode>();
			int depth = findPathToArc(lastPp, ap, 0, marked);
			if (depth < 0) return null;

			// create the contacts
			Cell cell = ai.getParent();
			PortInst retPi = lastNi.findPortInstFromProto(lastPp);
			Point2D center = ai.getLocation(end);
			for(int i=0; i<depth; i++)
			{
				double xS = contactStack[i].getDefWidth();
				double yS = contactStack[i].getDefHeight();
				NodeInst newNi = NodeInst.makeInstance(contactStack[i], center, xS, yS, cell);
				if (newNi == null) return null;
				PortInst thisPi = newNi.findPortInstFromProto(contactStack[i].getPort(0));

				ArcProto typ = contactStackArc[i];
				double wid = typ.getDefaultWidth();
				ArcInst newAi = ArcInst.makeInstance(typ, wid, thisPi, retPi);
				retPi = thisPi;
				if (newAi == null) return null;
			}
			return retPi;
		}

		int findPathToArc(PortProto pp, ArcProto ap, int depth, Set<PrimitiveNode> marked)
		{
			// see if the connection is made
			if (pp.connectsTo(ap)) return depth;

			// look for a contact
			PrimitiveNode bestNp = null;
			ArcProto bestAp = null;
			int bestDepth = 0;
			Technology tech = ap.getTechnology();
			for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
			{
				PrimitiveNode nextNp = it.next();
				if (marked.contains(nextNp)) continue;
				PrimitiveNode.Function fun = nextNp.getFunction();
				if (fun != PrimitiveNode.Function.CONTACT) continue;

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
				marked.add(nextNp);
				int newDepth = findPathToArc(nextPp, ap, depth+1, marked);
				marked.remove(nextNp);
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
				marked.add(bestNp);
				int newDepth = findPathToArc(bestNp.getPort(0), ap, depth+1, marked);
				marked.remove(bestNp);
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

        done.setText("Done");
        done.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                done(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(done, gridBagConstraints);

        apply.setText("Apply");
        apply.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                apply(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(apply, gridBagConstraints);

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
