/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Array.java
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

import com.sun.electric.database.change.DatabaseChangeEvent;
import com.sun.electric.database.change.DatabaseChangeListener;
import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.drc.Quick;
import com.sun.electric.tool.user.CircuitChangeJobs;
import com.sun.electric.tool.user.ExportChanges;
import com.sun.electric.tool.user.HighlightListener;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.UserInterfaceMain;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.MeasureListener;
import com.sun.electric.tool.user.ui.ToolBar;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.util.TextUtils;

import java.awt.Cursor;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

/**
 * Class to handle the "Array" dialog.
 */
public class Array extends EModelessDialog implements HighlightListener, DatabaseChangeListener
{
	/** Space by edge overlap. */				private static final int SPACING_EDGE = 1;
	/** Space by centerline distance. */		private static final int SPACING_CENTER = 2;
	/** Space by characteristic distance. */	private static final int SPACING_ESSENTIALBND = 3;
	/** Space by measured distance. */			private static final int SPACING_MEASURED = 4;

	private static Cursor drawArrayCursor = ToolBar.readCursor("CursorArray.gif", 8, 8);
	private static Pref.Group prefs = Pref.groupForPackage(Array.class);
	private static Pref
		prefLinearDiagonal = Pref.makeBooleanPref("Array_LinearDiagonal", prefs, false),
		prefAddNames = Pref.makeBooleanPref("Array_AddNames", prefs, false),
		prefDRCGood = Pref.makeBooleanPref("Array_DRCGood", prefs, false),
		prefTranspose = Pref.makeBooleanPref("Array_Transpose", prefs, false),
		prefXFlip = Pref.makeBooleanPref("Array_XFlip", prefs, false),
		prefYFlip = Pref.makeBooleanPref("Array_YFlip", prefs, false),
		prefXStagger = Pref.makeBooleanPref("Array_XStagger", prefs, false),
		prefYStagger = Pref.makeBooleanPref("Array_YStagger", prefs, false),
		prefXCenter = Pref.makeBooleanPref("Array_XCenter", prefs, false),
		prefYCenter = Pref.makeBooleanPref("Array_YCenter", prefs, false),
		prefSpacingType = Pref.makeIntPref("Array_SpacingType", prefs, SPACING_EDGE),
		prefXRepeat = Pref.makeIntPref("Array_XRepeat", prefs, 1),
		prefYRepeat = Pref.makeIntPref("Array_YRepeat", prefs, 1);
	private static Double lastEdgeOverlapX = null, lastEdgeOverlapY = null;
	private static Double lastCenterlineX = null, lastCenterlineY = null;

	private static class ArrayPrefs implements Serializable
	{
		boolean linearDiagonal;
		boolean addNames;
		boolean DRCGood;
		boolean transpose;
		boolean xFlip;
		boolean yFlip;
		boolean xStagger;
		boolean yStagger;
		boolean xCenter;
		boolean yCenter;
		int xRepeat;
		int yRepeat;

		public ArrayPrefs()
		{
			linearDiagonal = prefLinearDiagonal.getBoolean();
			addNames = prefAddNames.getBoolean();
			DRCGood = prefDRCGood.getBoolean();
			transpose = prefTranspose.getBoolean();
			xFlip = prefXFlip.getBoolean();
			yFlip = prefYFlip.getBoolean();
			xStagger = prefXStagger.getBoolean();
			yStagger = prefYStagger.getBoolean();
			xCenter = prefXCenter.getBoolean();
			yCenter = prefYCenter.getBoolean();
			xRepeat = prefXRepeat.getInt();
			yRepeat = prefYRepeat.getInt();
		}
	}

	/** amount when spacing by edge overlap */				private double spacingOverX, spacingOverY;
	/** amount when spacing by centerline distance */		private double spacingCenterlineX, spacingCenterlineY;
	/** amount when spacing by characteristic distance */	private double essentialBndX, essentialBndY;
	/** amount when spacing by measured distance */			private double spacingMeasuredX, spacingMeasuredY;
	/** the selected objects */								private Set<Geometric> selected;
	/** the selected objects including connecting nodes */	private Set<Geometric> selectedAll;
	/** the bounds of the selected objects */				private Rectangle2D bounds;
	/** the technology of the cell where arraying is done */private Technology tech;

	/**
	 * Method to display a dialog for arraying the selected circuitry.
	 */
	public static void showArrayDialog()
	{
		// first make sure something is selected
		EditWindow wnd = EditWindow.needCurrent();
		if (wnd == null) return;
		Highlighter highlighter = wnd.getHighlighter();
		if (highlighter == null)
		{
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
				"Cannot array: nothing is highlighted in this window.");
			return;
		}
		List<Geometric> highs = highlighter.getHighlightedEObjs(true, true);
		if (highs.size() == 0)
		{
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
				"Select some objects before arraying them.");
			return;
		}
		Array dialog = new Array(TopLevel.getCurrentJFrame());
		dialog.setVisible(true);
	}

	/** Creates new form Array */
	private Array(Frame parent)
	{
		super(parent);
		initComponents();
		getRootPane().setDefaultButton(ok);
		UserInterfaceMain.addDatabaseChangeListener(this);
		Highlighter.addHighlightListener(this);

		// make all text fields select-all when entered
		EDialog.makeTextFieldSelectAllOnTab(xRepeat);
		EDialog.makeTextFieldSelectAllOnTab(yRepeat);
		EDialog.makeTextFieldSelectAllOnTab(xSpacing);
		EDialog.makeTextFieldSelectAllOnTab(ySpacing);

		// load the repeat factors
		xRepeat.setText(Integer.toString(prefXRepeat.getInt()));
		flipAlternateColumns.setSelected(prefXFlip.getBoolean());
		staggerAlternateColumns.setSelected(prefXStagger.getBoolean());
		centerXAboutOriginal.setSelected(prefXCenter.getBoolean());
		yRepeat.setText(Integer.toString(prefYRepeat.getInt()));
		flipAlternateRows.setSelected(prefYFlip.getBoolean());
		staggerAlternateRows.setSelected(prefYStagger.getBoolean());
		centerYAboutOriginal.setSelected(prefYCenter.getBoolean());

		// load the other factors
		linearDiagonalArray.setSelected(prefLinearDiagonal.getBoolean());
		generateArrayIndices.setSelected(prefAddNames.getBoolean());
		onlyDRCCorrect.setSelected(prefDRCGood.getBoolean());
		transposePlacement.setSelected(prefTranspose.getBoolean());

		spaceByEdgeOverlap.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { newSpacingSelected(); }
		});
		spaceByCenterlineDistance.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { newSpacingSelected(); }
		});
		spaceByEssentialBnd.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { newSpacingSelected(); }
		});
		spaceByMeasuredDistance.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { newSpacingSelected(); }
		});

		updateDialogForSelection();
		finishInitialization();
		pack();
	}

	protected void escapePressed() { cancel(null); }

	/**
	 * Reloads the dialog when Highlights change
	 */
	public void highlightChanged(Highlighter which)
	{
		if (!isVisible()) return;
		updateDialogForSelection();
	}

	/**
	 * Called when by a Highlighter when it loses focus. The argument
	 * is the Highlighter that has gained focus (may be null).
	 * @param highlighterGainedFocus the highlighter for the current window (may be null).
	 */
	public void highlighterLostFocus(Highlighter highlighterGainedFocus)
	{
		if (!isVisible()) return;
		updateDialogForSelection();
	}

	/**
	 * Respond to database changes
	 * @param e database change event
	 */
	public void databaseChanged(DatabaseChangeEvent e)
	{
		if (!isVisible()) return;
		updateDialogForSelection();
	}

	private void updateDialogForSelection()
	{
		// do not update if drawing the array interactively
		EventListener oldListener = WindowFrame.getListener();
		if (oldListener != null && oldListener instanceof DrawArrayListener) return;

		// see what is highlighted
		EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null || wnd.getCell() == null) return; // invalid window or cell

        tech = wnd.getCell().getTechnology();
		List<Geometric> highs = wnd.getHighlighter().getHighlightedEObjs(true, true);

		// if a single cell instance is selected, enable DRC-guided placement
		onlyDRCCorrect.setEnabled(false);
		if (highs.size() == 1)
		{
			ElectricObject eObj = highs.get(0);
			if (eObj instanceof NodeInst)
				onlyDRCCorrect.setEnabled(true);
		}

		// see if essential bounds are defined
		essentialBndX = essentialBndY = 0;
		boolean haveEB = false;
		for(Geometric eObj : highs)
		{
			if (!(eObj instanceof NodeInst)) continue;
			NodeInst ni = (NodeInst)eObj;
			if (!ni.isCellInstance()) continue;
			Cell subCell = (Cell)ni.getProto();
			Rectangle2D spacing = subCell.findEssentialBounds();
			if (spacing == null) continue;
			double thisDistX = spacing.getWidth();
			double thisDistY = spacing.getHeight();
			if (ni.getAngle() == 900 || ni.getAngle() == 2700)
			{
				double swap = thisDistX;   thisDistX = thisDistY;   thisDistY = swap;
			}
			if (haveEB)
			{
				if (essentialBndX != thisDistX || essentialBndY != thisDistY)
				{
					haveEB = false;
					break;
				}
			}
			essentialBndX = thisDistX;
			essentialBndY = thisDistY;
			haveEB = true;
		}
		spaceByEssentialBnd.setEnabled(haveEB);
		if (prefSpacingType.getInt() == SPACING_ESSENTIALBND && !haveEB)
			prefSpacingType.setInt(SPACING_EDGE);

		// see if there was a measured distance
		Dimension2D dim = MeasureListener.getLastMeasuredDistance();
		if (dim.getWidth() > 0 || dim.getHeight() > 0)
		{
			spaceByMeasuredDistance.setEnabled(true);
			spacingMeasuredX = dim.getWidth();
			spacingMeasuredY = dim.getHeight();
		} else
		{
			spaceByMeasuredDistance.setEnabled(false);
			if (prefSpacingType.getInt() == SPACING_MEASURED)
				prefSpacingType.setInt(SPACING_EDGE);
		}

		// mark the list of nodes and arcs in the cell that will be arrayed
		selected = new HashSet<Geometric>();
		selectedAll = new HashSet<Geometric>();
		for(Geometric eObj : highs)
		{
			selected.add(eObj);
			selectedAll.add(eObj);
			if (eObj instanceof ArcInst)
			{
				ArcInst ai = (ArcInst)eObj;
				NodeInst niHead = ai.getHeadPortInst().getNodeInst();
				selectedAll.add(niHead);
				NodeInst niTail = ai.getTailPortInst().getNodeInst();
				selectedAll.add(niTail);
			}
		}

		// determine size of arrayed objects
		boolean first = true;
		bounds = new Rectangle2D.Double();
		for(Geometric geom : selected)
		{
			if (first)
			{
				bounds.setRect(geom.getBounds());
				first = false;
			} else
			{
				Rectangle2D.union(bounds, geom.getBounds(), bounds);
			}
		}
		spacingCenterlineX = bounds.getWidth();
		spacingCenterlineY = bounds.getHeight();
		if (lastCenterlineX != null && lastCenterlineY != null)
		{
			spacingCenterlineX = lastCenterlineX.doubleValue();
			spacingCenterlineY = lastCenterlineY.doubleValue();
		}

		// determine default edge overlap value
		spacingOverX = spacingOverY = 0;
		if (lastEdgeOverlapX != null && lastEdgeOverlapY != null)
		{
			spacingOverX = lastEdgeOverlapX.doubleValue();
			spacingOverY = lastEdgeOverlapY.doubleValue();
		}

		// load the spacing distances
		switch (prefSpacingType.getInt())
		{
			case SPACING_EDGE:
				spaceByEdgeOverlap.setSelected(true);
				xSpacing.setText(TextUtils.formatDistance(spacingOverX, tech));
				ySpacing.setText(TextUtils.formatDistance(spacingOverY, tech));
				break;
			case SPACING_CENTER:
				spaceByCenterlineDistance.setSelected(true);
				xSpacing.setText(TextUtils.formatDistance(spacingCenterlineX, tech));
				ySpacing.setText(TextUtils.formatDistance(spacingCenterlineY, tech));
				break;
			case SPACING_ESSENTIALBND:
				spaceByEssentialBnd.setSelected(true);
				xSpacing.setText(TextUtils.formatDistance(essentialBndX, tech));
				ySpacing.setText(TextUtils.formatDistance(essentialBndY, tech));
				break;
			case SPACING_MEASURED:
				spaceByMeasuredDistance.setSelected(true);
				xSpacing.setText(TextUtils.formatDistance(spacingMeasuredX, tech));
				ySpacing.setText(TextUtils.formatDistance(spacingMeasuredY, tech));
				break;
		}
		if (prefSpacingType.getInt() == SPACING_EDGE)
		{
			xOverlapLabel.setText("X edge overlap:");
			yOverlapLabel.setText("Y edge overlap:");
		} else
		{
			xOverlapLabel.setText("X centerline distance:");
			yOverlapLabel.setText("Y centerline distance:");
		}
	}

	private void newSpacingSelected()
	{
		double x = TextUtils.atofDistance(xSpacing.getText(), tech);
		double y = TextUtils.atofDistance(ySpacing.getText(), tech);
		switch (prefSpacingType.getInt())
		{
			case SPACING_EDGE:          spacingOverX = x;         spacingOverY = y;         break;
			case SPACING_CENTER:        spacingCenterlineX = x;   spacingCenterlineY = y;   break;
			case SPACING_ESSENTIALBND:  essentialBndX = x;        essentialBndY = y;        break;
			case SPACING_MEASURED:      spacingMeasuredX = x;     spacingMeasuredY = y;     break;
		}
		if (spaceByEdgeOverlap.isSelected()) prefSpacingType.setInt(SPACING_EDGE); else
		if (spaceByCenterlineDistance.isSelected()) prefSpacingType.setInt(SPACING_CENTER); else
		if (spaceByEssentialBnd.isSelected()) prefSpacingType.setInt(SPACING_ESSENTIALBND); else
		if (spaceByMeasuredDistance.isSelected()) prefSpacingType.setInt(SPACING_MEASURED);
		if (prefSpacingType.getInt() == SPACING_EDGE)
		{
			xOverlapLabel.setText("X edge overlap:");
			yOverlapLabel.setText("Y edge overlap:");
		} else
		{
			xOverlapLabel.setText("X centerline distance:");
			yOverlapLabel.setText("Y centerline distance:");
		}
		switch (prefSpacingType.getInt())
		{
			case SPACING_EDGE:          x = spacingOverX;         y = spacingOverY;         break;
			case SPACING_CENTER:        x = spacingCenterlineX;   y = spacingCenterlineY;   break;
			case SPACING_ESSENTIALBND:  x = essentialBndX;        y = essentialBndY;        break;
			case SPACING_MEASURED:      x = spacingMeasuredX;     y = spacingMeasuredY;     break;
		}
		xSpacing.setText(TextUtils.formatDistance(x, tech));
		ySpacing.setText(TextUtils.formatDistance(y, tech));
	}

	private void rememberFields()
	{
		// gather all "last" values
		prefXRepeat.setInt((int)TextUtils.getValueOfExpression(xRepeat.getText()));
		prefXFlip.setBoolean(flipAlternateColumns.isSelected());
		prefXStagger.setBoolean(staggerAlternateColumns.isSelected());
		prefXCenter.setBoolean(centerXAboutOriginal.isSelected());
		prefYRepeat.setInt((int)TextUtils.getValueOfExpression(yRepeat.getText()));
		prefYFlip.setBoolean(flipAlternateRows.isSelected());
		prefYStagger.setBoolean(staggerAlternateRows.isSelected());
		prefYCenter.setBoolean(centerYAboutOriginal.isSelected());
		prefLinearDiagonal.setBoolean(linearDiagonalArray.isSelected());
		prefAddNames.setBoolean(generateArrayIndices.isSelected());
		prefDRCGood.setBoolean(onlyDRCCorrect.isSelected());
		prefTranspose.setBoolean(transposePlacement.isSelected());

		if (prefSpacingType.getInt() == SPACING_EDGE)
		{
			lastEdgeOverlapX = new Double(TextUtils.atofDistance(xSpacing.getText()));
			lastEdgeOverlapY = new Double(TextUtils.atofDistance(ySpacing.getText()));
		} else
		{
			lastCenterlineX = new Double(TextUtils.atofDistance(xSpacing.getText()));
			lastCenterlineY = new Double(TextUtils.atofDistance(ySpacing.getText()));
		}
	}

	private void makeArray()
	{
		// check for nonsense
		int xRepeat = Math.abs(prefXRepeat.getInt());
		int yRepeat = Math.abs(prefYRepeat.getInt());
		if (xRepeat <= 1 && yRepeat <= 1)
		{
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
				"One dimension of the array must be greater than 1");
			return;
		}
		if (prefLinearDiagonal.getBoolean() && xRepeat != 1 && yRepeat != 1)
		{
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
				"Diagonal arrays need one dimension to be 1");
			return;
		}

		// make lists of nodes and arcs that will be arrayed
		Set<NodeInst> nodeSet = new HashSet<NodeInst>();
		List<ArcInst> arcList = new ArrayList<ArcInst>();
		List<Export> exportList = new ArrayList<Export>();
		Cell cell = null;
		for(Geometric geom : selected)
		{
			cell = geom.getParent();
			if (geom instanceof NodeInst)
			{
				nodeSet.add((NodeInst)geom);
				if (User.isDupCopiesExports())
				{
					NodeInst ni = (NodeInst)geom;
					for(Iterator<Export> eIt = ni.getExports(); eIt.hasNext(); )
						exportList.add(eIt.next());
				}
			} else
			{
                ArcInst ai = (ArcInst)geom;
                arcList.add(ai);
                nodeSet.add(ai.getHead().getPortInst().getNodeInst());
                nodeSet.add(ai.getTail().getPortInst().getNodeInst());
            }
		}
		List<NodeInst> nodeList = new ArrayList<NodeInst>();
		for(NodeInst ni : nodeSet) nodeList.add(ni);
		Collections.sort(nodeList, new Comparator<NodeInst> () {
            public int compare(NodeInst n1, NodeInst n2) {
                return TextUtils.STRING_NUMBER_ORDER.compare(n1.getName(), n2.getName());
            }
        });
		Collections.sort(arcList, new Comparator<ArcInst> () {
            public int compare(ArcInst a1, ArcInst a2) {
                return TextUtils.STRING_NUMBER_ORDER.compare(a1.getName(), a2.getName());
            }
        });
		Collections.sort(exportList, new Comparator<Export> () {
            public int compare(Export e1, Export e2) {
                return TextUtils.STRING_NUMBER_ORDER.compare(e1.getName(), e2.getName());
            }
        });

		// determine the distance between arrayed entries
		double xOverlap = TextUtils.atofDistance(xSpacing.getText());
		double yOverlap = TextUtils.atofDistance(ySpacing.getText());
		if (prefSpacingType.getInt() == SPACING_EDGE)
		{
			xOverlap = bounds.getWidth() - xOverlap;
			yOverlap = bounds.getHeight() - yOverlap;
		}
		double cX = bounds.getCenterX();
		double cY = bounds.getCenterY();

		// disallow arraying if lock is on
		for(NodeInst ni : nodeList)
		{
			if (CircuitChangeJobs.cantEdit(cell, ni, true, false, false) != 0) return;
		}

		// create the array
		new ArrayStuff(cell, nodeList, arcList, exportList, xRepeat, yRepeat, xOverlap, yOverlap, cX, cY,
			User.isArcsAutoIncremented());
	}

	/**
	 * Class to create an array in a new thread.
	 */
	private static class ArrayStuff extends Job
	{
		private List<NodeInst> nodeList;
		private List<ArcInst> arcList;
		private List<Export> exportList;
		private int xRepeat, yRepeat;
		private double xOverlap, yOverlap, cX, cY;
		private boolean arcsAutoIncrement, fromRight;
        private Cell cell;
        private DRC.DRCPreferences dp;
        private ArrayPrefs ap;

        protected ArrayStuff(Cell c, List<NodeInst> nodeList, List<ArcInst> arcList, List<Export> exportList,
			int xRepeat, int yRepeat, double xOverlap, double yOverlap, double cX, double cY, boolean arcsAutoIncrement)
		{
			super("Make Array", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.cell = c;
            this.nodeList = nodeList;
			this.arcList = arcList;
			this.exportList = exportList;
			this.xRepeat = xRepeat;
			this.yRepeat = yRepeat;
			this.xOverlap = xOverlap;
			this.yOverlap = yOverlap;
			this.cX = cX;
			this.cY = cY;
			this.arcsAutoIncrement = arcsAutoIncrement;
			this.fromRight = User.isIncrementRightmostIndex();
            this.dp = new DRC.DRCPreferences(false);
            this.ap = new ArrayPrefs();
            startJob();
		}

		public boolean doIt() throws JobException
		{
            assert(cell != null);

			// if only arraying where DRC clean, make an array of newly created nodes
			Geometric [] geomsToCheck = null;
			boolean [] validity = null;
			int checkNodeCount = 0;
			if (ap.DRCGood)
			{
				geomsToCheck = new NodeInst[xRepeat * yRepeat];
				validity = new boolean[xRepeat * yRepeat];
				if (nodeList.size() == 1)
					geomsToCheck[checkNodeCount++] = nodeList.get(0);
			}

			// create the array
			int originalX = 0, originalY = 0;
			int total = yRepeat * xRepeat;
			for(int index = 0; index < total; index++)
			{
				int x = index % xRepeat;
				int y = index / xRepeat;
				if (ap.transpose)
				{
					y = index % yRepeat;
					x = index / yRepeat;
				}
				int xIndex = x;
				int yIndex = y;
				if (ap.xCenter) xIndex = x - (xRepeat-1)/2;
				if (ap.yCenter) yIndex = y - (yRepeat-1)/2;
				if (ap.xRepeat < 0) xIndex = -xIndex;
				if (ap.yRepeat < 0) yIndex = -yIndex;
				if (xIndex == 0 && yIndex == 0)
				{
					originalX = x;
					originalY = y;
					continue;
				}

				// first replicate the nodes
				boolean firstNode = true;
				Map<NodeInst,NodeInst> nodeMap = new HashMap<NodeInst,NodeInst>();
				for(NodeInst ni : nodeList)
				{
					double xPos = cX + xOverlap * xIndex;
					if (ap.linearDiagonal && xRepeat == 1) xPos = cX + xOverlap * yIndex;
					double yPos = cY + yOverlap * yIndex;
					if (ap.linearDiagonal && yRepeat == 1) yPos = cY + yOverlap * xIndex;
					double xOff = ni.getAnchorCenterX() - cX;
					double yOff = ni.getAnchorCenterY() - cY;
					if ((xIndex&1) != 0 && ap.xStagger) yPos += yOverlap/2;
					if ((yIndex&1) != 0 && ap.yStagger) xPos += xOverlap/2;
					boolean flipX = false, flipY = false;
					if ((xIndex&1) != 0 && ap.xFlip)
					{
						flipX = true;
						xOff = -xOff;
					}
					if ((yIndex&1) != 0 && ap.yFlip)
					{
						flipY = true;
						yOff = -yOff;
					}
					Orientation orient = Orientation.fromJava(0, flipX, flipY).concatenate(ni.getOrient());
					xPos += xOff;   yPos += yOff;
					NodeInst newNi = NodeInst.makeInstance(ni.getProto(),
						new Point2D.Double(xPos, yPos), ni.getXSize(), ni.getYSize(), cell, orient, null);
					if (newNi == null) continue;
					newNi.copyTextDescriptorFrom(ni, NodeInst.NODE_PROTO);
					newNi.copyTextDescriptorFrom(ni, NodeInst.NODE_NAME);
					newNi.setExpanded(ni.isExpanded());
					if (ni.isHardSelect()) newNi.setHardSelect(); else newNi.clearHardSelect();
					newNi.setTechSpecific(ni.getTechSpecific());
					newNi.copyVarsFrom(ni);
					if (ap.addNames)
					{
						setNewName(newNi, x, y);
					} else
					{
						Name nodeNameKey = ni.getNameKey();
						if (!nodeNameKey.isTempname())
						{
							newNi.setName(ElectricObject.uniqueObjectName(ni.getName(), cell, NodeInst.class, false, fromRight));
							newNi.copyTextDescriptorFrom(ni, NodeInst.NODE_NAME);
						}
					}

					nodeMap.put(ni, newNi);
					if (ap.DRCGood && firstNode)
					{
						geomsToCheck[checkNodeCount++] = newNi;
						firstNode = false;
					}
				}

				// next replicate the arcs
				for(ArcInst ai : arcList)
				{
					double cX0 = ai.getHeadPortInst().getNodeInst().getAnchorCenterX();
					double cY0 = ai.getHeadPortInst().getNodeInst().getAnchorCenterY();
					double xOff0 = ai.getHeadLocation().getX() - cX0;
					double yOff0 = ai.getHeadLocation().getY() - cY0;

					double cX1 = ai.getTailPortInst().getNodeInst().getAnchorCenterX();
					double cY1 = ai.getTailPortInst().getNodeInst().getAnchorCenterY();
					double xOff1 = ai.getTailLocation().getX() - cX1;
					double yOff1 = ai.getTailLocation().getY() - cY1;

					if ((xIndex&1) != 0 && ap.xFlip)
					{
						xOff0 = -xOff0;
						xOff1 = -xOff1;
					}
					if ((yIndex&1) != 0 && ap.yFlip)
					{
						yOff0 = -yOff0;
						yOff1 = -yOff1;
					}

					NodeInst ni0 = nodeMap.get(ai.getHeadPortInst().getNodeInst());
					if (ni0 == null) continue;
					NodeInst ni1 = nodeMap.get(ai.getTailPortInst().getNodeInst());
					if (ni1 == null) continue;
					cX0 = ni0.getAnchorCenterX();
					cY0 = ni0.getAnchorCenterY();
					cX1 = ni1.getAnchorCenterX();
					cY1 = ni1.getAnchorCenterY();
					PortInst pi0 = ni0.findPortInstFromProto(ai.getHeadPortInst().getPortProto());
					PortInst pi1 = ni1.findPortInstFromProto(ai.getTailPortInst().getPortProto());
					ArcInst newAi = ArcInst.makeInstanceBase(ai.getProto(), ai.getLambdaBaseWidth(), pi0,
						pi1, new Point2D.Double(cX0+xOff0, cY0+yOff0), new Point2D.Double(cX1+xOff1, cY1+yOff1), null);
					if (newAi == null) continue;
					newAi.copyPropertiesFrom(ai);

					if (ap.addNames)
					{
						setNewName(newAi, x, y);
					} else
					{
						Name arcNameKey = ai.getNameKey();
						if (!arcNameKey.isTempname())
						{
							String newName = ai.getName();
							if (arcsAutoIncrement)
								newName = ElectricObject.uniqueObjectName(newName, cell, ArcInst.class, false, fromRight);
							newAi.setName(newName);
							newAi.copyTextDescriptorFrom(ai, ArcInst.ARC_NAME);
						}
					}
				}

				// copy the exports, too
				List<PortInst> portInstsToExport = new ArrayList<PortInst>();
				Map<PortInst,Export> originalExports = new HashMap<PortInst,Export>();
				for(Export pp : exportList)
				{
					PortInst oldPI = pp.getOriginalPort();
					NodeInst newNI = nodeMap.get(oldPI.getNodeInst());
					if (newNI == null) continue;
					PortInst pi = newNI.findPortInstFromProto(oldPI.getPortProto());
					portInstsToExport.add(pi);
					originalExports.put(pi, pp);
				}
				ExportChanges.reExportPorts(cell, portInstsToExport, false, true, true, false, fromRight, originalExports);
			}

			// rename the replicated objects
			if (ap.addNames)
			{
				for(NodeInst ni : nodeList)
				{
					setNewName(ni, originalX, originalY);
				}
				for(ArcInst ai : arcList)
				{
					setNewName(ai, originalX, originalY);
				}
			}

			// if only arraying where DRC valid, check them now and delete what is not valid
			if (ap.DRCGood)
			{
				Quick.checkDesignRules(dp, cell, geomsToCheck, validity).termLogging(true);
				for(int i=1; i<checkNodeCount; i++)
				{
					if (!validity[i])
					{
						// delete the node
						((NodeInst)geomsToCheck[i]).kill();
					}
				}
			}
			return true;
		}

		private void setNewName(Geometric geom, int x, int y)
		{
			String objName = "";
			Name geomNameKey = geom instanceof NodeInst ? ((NodeInst)geom).getNameKey() : ((ArcInst)geom).getNameKey();
			String geomName = geomNameKey.toString();
			if (geomNameKey.isTempname()) geomName = null;
			if (geomName != null)
			{
				if (!geomName.equals("0") && !geomName.equals("0-0"))
					objName = geomName.toString();
			}
			String totalName = objName + x + "-" + y;
			if (Math.abs(ap.xRepeat) <= 1 || Math.abs(ap.yRepeat) <= 1)
				totalName = objName + (x+y);
			if (geom instanceof NodeInst) {
				NodeInst ni = (NodeInst)geom;
				ni.setName(totalName);
				if (ni.isCellInstance())
					ni.setOff(NodeInst.NODE_NAME, 0, ni.getYSize() / 4);
			} else {
				((ArcInst)geom).setName(totalName);
			}
		}
	}

	private class DrawArrayListener implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener
	{
		EventListener oldListener;
		Cursor oldCursor;

		public void mousePressed(MouseEvent evt)
		{
			determineArrayAmount(evt);
			showHighlight(evt, (EditWindow)evt.getSource());
		}

		public void mouseDragged(MouseEvent evt)
		{
			determineArrayAmount(evt);
			showHighlight(evt, (EditWindow)evt.getSource());
		}

		public void mouseReleased(MouseEvent evt)
		{
			// restore the highlighting
			EditWindow wnd = (EditWindow)evt.getSource();
			Highlighter highlighter = wnd.getHighlighter();
			Cell cell = wnd.getCell();
			highlighter.clear();
			for(Geometric geom : selected)
				highlighter.addElectricObject(geom, cell);
			highlighter.finished();

			// restore the listener to the former state
			restoreOriginalSetup();

			makeArray();
		}

		public void keyPressed(KeyEvent evt)
		{
			// ESCAPE for abort
			int chr = evt.getKeyCode();
			if (chr == KeyEvent.VK_ESCAPE)
			{
				restoreOriginalSetup();
				System.out.println("Array drawing aborted");
			}
		}

		private void restoreOriginalSetup()
		{
			// restore the listener to the former state
			WindowFrame.setListener(oldListener);
			TopLevel.setCurrentCursor(oldCursor);
		}

		private void determineArrayAmount(MouseEvent evt)
		{
			EditWindow wnd = (EditWindow)evt.getSource();
			int x = evt.getX();
			int y = evt.getY();
			Point2D pt = wnd.screenToDatabase(x, y);

			double xOverlap = TextUtils.atofDistance(xSpacing.getText());
			double yOverlap = TextUtils.atofDistance(ySpacing.getText());
			if (prefSpacingType.getInt() == SPACING_EDGE)
			{
				xOverlap = bounds.getWidth() - xOverlap;
				yOverlap = bounds.getHeight() - yOverlap;
			}

			double dX = pt.getX() - bounds.getCenterX();
			if (dX > 0) dX -= bounds.getWidth()/2; else
				dX += bounds.getWidth()/2;
			int xRep = (int)(dX / xOverlap);
			if (xRep == 0) xRep = 1; else
			{
				if (xRep < 0) xRep--; else xRep++;
			}
			if (prefXCenter.getBoolean()) xRep = (Math.abs(xRep) - 1) * 2 + 1;

			double dY = pt.getY() - bounds.getCenterY();
			if (dY > 0) dY -= bounds.getHeight()/2; else
				dY += bounds.getHeight()/2;
			int yRep = (int)(dY / yOverlap);
			if (yRep == 0) yRep = 1; else
			{
				if (yRep < 0) yRep--; else yRep++;
			}
			if (prefYCenter.getBoolean()) yRep = (Math.abs(yRep) - 1) * 2 + 1;

			if (prefLinearDiagonal.getBoolean())
			{
				if (Math.abs(xRep) > Math.abs(yRep)) xRep = 1; else yRep = 1;
			}

			// show current repeat factors
			prefXRepeat.setInt(xRep);
			xRepeat.setText(Integer.toString(xRep));
			prefYRepeat.setInt(yRep);
			yRepeat.setText(Integer.toString(yRep));
		}

		private void showHighlight(MouseEvent evt, EditWindow wnd)
		{
			double xOverlap = TextUtils.atofDistance(xSpacing.getText());
			double yOverlap = TextUtils.atofDistance(ySpacing.getText());
			if (prefSpacingType.getInt() == SPACING_EDGE)
			{
				xOverlap = bounds.getWidth() - xOverlap;
				yOverlap = bounds.getHeight() - yOverlap;
			}

			Highlighter highlighter = wnd.getHighlighter();
			Cell cell = wnd.getCell();
			highlighter.clear();
			int xRepeat = Math.abs(prefXRepeat.getInt());
			int yRepeat = Math.abs(prefYRepeat.getInt());
			double cX = bounds.getCenterX();
			double cY = bounds.getCenterY();
			int total = yRepeat * xRepeat;
			for(int index = 0; index < total; index++)
			{
				int x = index % xRepeat;
				int y = index / xRepeat;
				if (prefTranspose.getBoolean())
				{
					y = index % yRepeat;
					x = index / yRepeat;
				}
				int xIndex = x;
				int yIndex = y;
				if (prefXCenter.getBoolean()) xIndex = x - (xRepeat-1)/2;
				if (prefYCenter.getBoolean()) yIndex = y - (yRepeat-1)/2;
				if (prefXRepeat.getInt() < 0) xIndex = -xIndex;
				if (prefYRepeat.getInt() < 0) yIndex = -yIndex;

				double xPos = cX + xOverlap * xIndex;
				if (prefLinearDiagonal.getBoolean() && xRepeat == 1) xPos = cX + xOverlap * yIndex;
				double yPos = cY + yOverlap * yIndex;
				if (prefLinearDiagonal.getBoolean() && yRepeat == 1) yPos = cY + yOverlap * xIndex;
				if ((xIndex&1) != 0 && prefXStagger.getBoolean()) yPos += yOverlap/2;
				if ((yIndex&1) != 0 && prefYStagger.getBoolean()) xPos += xOverlap/2;
				double lX = xPos - bounds.getWidth()/2;
				double hX = lX + bounds.getWidth();
				double lY = yPos - bounds.getHeight()/2;
				double hY = lY + bounds.getHeight();
				Rectangle2D area = new Rectangle2D.Double(lX, lY, hX-lX, hY-lY);
				highlighter.addArea(area, cell);
				double headSize = Math.min(bounds.getWidth(), bounds.getHeight()) / 5;
				if ((xIndex&1) != 0 && prefXFlip.getBoolean())
				{
					Point2D head = new Point2D.Double(lX, yPos);
					Point2D head1 = new Point2D.Double(lX+headSize, yPos+headSize);
					Point2D head2 = new Point2D.Double(lX+headSize, yPos-headSize);
					Point2D tail = new Point2D.Double(hX, yPos);
					Point2D tail1 = new Point2D.Double(hX-headSize, yPos+headSize);
					Point2D tail2 = new Point2D.Double(hX-headSize, yPos-headSize);
					highlighter.addLine(head, tail, cell);
					highlighter.addLine(head, head1, cell);
					highlighter.addLine(head, head2, cell);
					highlighter.addLine(tail, tail1, cell);
					highlighter.addLine(tail, tail2, cell);
				}
				if ((yIndex&1) != 0 && prefYFlip.getBoolean())
				{
					Point2D head = new Point2D.Double(xPos, lY);
					Point2D head1 = new Point2D.Double(xPos+headSize, lY+headSize);
					Point2D head2 = new Point2D.Double(xPos-headSize, lY+headSize);
					Point2D tail = new Point2D.Double(xPos, hY);
					Point2D tail1 = new Point2D.Double(xPos+headSize, hY-headSize);
					Point2D tail2 = new Point2D.Double(xPos-headSize, hY-headSize);
					highlighter.addLine(head, tail, cell);
					highlighter.addLine(head, head1, cell);
					highlighter.addLine(head, head2, cell);
					highlighter.addLine(tail, tail1, cell);
					highlighter.addLine(tail, tail2, cell);
				}
			}
			highlighter.finished();
		}

		public void mouseClicked(MouseEvent evt) {}
		public void mouseMoved(MouseEvent evt) {}
		public void mouseEntered(MouseEvent evt) {}
		public void mouseExited(MouseEvent evt) {}
		public void mouseWheelMoved(MouseWheelEvent evt) {}
		public void keyReleased(KeyEvent evt) {}
		public void keyTyped(KeyEvent evt) {}
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        spacing = new javax.swing.ButtonGroup();
        cancel = new javax.swing.JButton();
        ok = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        xRepeat = new javax.swing.JTextField();
        flipAlternateColumns = new javax.swing.JCheckBox();
        staggerAlternateColumns = new javax.swing.JCheckBox();
        centerXAboutOriginal = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        yRepeat = new javax.swing.JTextField();
        flipAlternateRows = new javax.swing.JCheckBox();
        staggerAlternateRows = new javax.swing.JCheckBox();
        centerYAboutOriginal = new javax.swing.JCheckBox();
        jSeparator1 = new javax.swing.JSeparator();
        xOverlapLabel = new javax.swing.JLabel();
        xSpacing = new javax.swing.JTextField();
        spaceByEdgeOverlap = new javax.swing.JRadioButton();
        spaceByCenterlineDistance = new javax.swing.JRadioButton();
        yOverlapLabel = new javax.swing.JLabel();
        ySpacing = new javax.swing.JTextField();
        spaceByEssentialBnd = new javax.swing.JRadioButton();
        spaceByMeasuredDistance = new javax.swing.JRadioButton();
        linearDiagonalArray = new javax.swing.JCheckBox();
        generateArrayIndices = new javax.swing.JCheckBox();
        onlyDRCCorrect = new javax.swing.JCheckBox();
        transposePlacement = new javax.swing.JCheckBox();
        apply = new javax.swing.JButton();
        draw = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Array");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancel(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(cancel, gridBagConstraints);

        ok.setText("OK");
        ok.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ok(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);

        jLabel1.setText("X repeat factor:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel1, gridBagConstraints);

        xRepeat.setColumns(6);
        xRepeat.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(xRepeat, gridBagConstraints);

        flipAlternateColumns.setText("Flip alternate columns");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        getContentPane().add(flipAlternateColumns, gridBagConstraints);

        staggerAlternateColumns.setText("Stagger alternate columns");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        getContentPane().add(staggerAlternateColumns, gridBagConstraints);

        centerXAboutOriginal.setText("Center about original");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
        getContentPane().add(centerXAboutOriginal, gridBagConstraints);

        jLabel2.setText("Y repeat factor:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel2, gridBagConstraints);

        yRepeat.setColumns(6);
        yRepeat.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(yRepeat, gridBagConstraints);

        flipAlternateRows.setText("Flip alternate rows");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        getContentPane().add(flipAlternateRows, gridBagConstraints);

        staggerAlternateRows.setText("Stagger alternate rows");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        getContentPane().add(staggerAlternateRows, gridBagConstraints);

        centerYAboutOriginal.setText("Center about original");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
        getContentPane().add(centerYAboutOriginal, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jSeparator1, gridBagConstraints);

        xOverlapLabel.setText("X edge overlap:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(xOverlapLabel, gridBagConstraints);

        xSpacing.setColumns(6);
        xSpacing.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(xSpacing, gridBagConstraints);

        spacing.add(spaceByEdgeOverlap);
        spaceByEdgeOverlap.setText("Space by edge overlap");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(spaceByEdgeOverlap, gridBagConstraints);

        spacing.add(spaceByCenterlineDistance);
        spaceByCenterlineDistance.setText("Space by centerline distance");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(spaceByCenterlineDistance, gridBagConstraints);

        yOverlapLabel.setText("Y edge overlap:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(yOverlapLabel, gridBagConstraints);

        ySpacing.setColumns(6);
        ySpacing.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ySpacing, gridBagConstraints);

        spacing.add(spaceByEssentialBnd);
        spaceByEssentialBnd.setText("Space by cell essential bound");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(spaceByEssentialBnd, gridBagConstraints);

        spacing.add(spaceByMeasuredDistance);
        spaceByMeasuredDistance.setText("Space by last measured distance");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(spaceByMeasuredDistance, gridBagConstraints);

        linearDiagonalArray.setText("Linear diagonal array");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        getContentPane().add(linearDiagonalArray, gridBagConstraints);

        generateArrayIndices.setText("Generate array indices");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        getContentPane().add(generateArrayIndices, gridBagConstraints);

        onlyDRCCorrect.setText("Only place entries that are DRC correct");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        getContentPane().add(onlyDRCCorrect, gridBagConstraints);

        transposePlacement.setText("Transpose placement ordering");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        getContentPane().add(transposePlacement, gridBagConstraints);

        apply.setText("Apply");
        apply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                applyActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(apply, gridBagConstraints);

        draw.setText("Draw");
        draw.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                drawActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(draw, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void drawActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_drawActionPerformed
		rememberFields();
		EventListener oldListener = WindowFrame.getListener();
		Cursor oldCursor = TopLevel.getCurrentCursor();
		if (oldListener == null || !(oldListener instanceof DrawArrayListener))
		{
			DrawArrayListener newListener = new DrawArrayListener();
			WindowFrame.setListener(newListener);
			newListener.oldListener = oldListener;
			newListener.oldCursor = oldCursor;
		}
		TopLevel.setCurrentCursor(drawArrayCursor);
		System.out.println("Click to draw the array");
    }//GEN-LAST:event_drawActionPerformed

    private void applyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_applyActionPerformed
		rememberFields();
		makeArray();
    }//GEN-LAST:event_applyActionPerformed

	private void cancel(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancel
	{//GEN-HEADEREND:event_cancel
		closeDialog(null);
	}//GEN-LAST:event_cancel

	private void ok(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ok
	{//GEN-HEADEREND:event_ok
		rememberFields();
		makeArray();
		closeDialog(null);
	}//GEN-LAST:event_ok

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		rememberFields();
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton apply;
    private javax.swing.JButton cancel;
    private javax.swing.JCheckBox centerXAboutOriginal;
    private javax.swing.JCheckBox centerYAboutOriginal;
    private javax.swing.JButton draw;
    private javax.swing.JCheckBox flipAlternateColumns;
    private javax.swing.JCheckBox flipAlternateRows;
    private javax.swing.JCheckBox generateArrayIndices;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JCheckBox linearDiagonalArray;
    private javax.swing.JButton ok;
    private javax.swing.JCheckBox onlyDRCCorrect;
    private javax.swing.JRadioButton spaceByCenterlineDistance;
    private javax.swing.JRadioButton spaceByEdgeOverlap;
    private javax.swing.JRadioButton spaceByEssentialBnd;
    private javax.swing.JRadioButton spaceByMeasuredDistance;
    private javax.swing.ButtonGroup spacing;
    private javax.swing.JCheckBox staggerAlternateColumns;
    private javax.swing.JCheckBox staggerAlternateRows;
    private javax.swing.JCheckBox transposePlacement;
    private javax.swing.JLabel xOverlapLabel;
    private javax.swing.JTextField xRepeat;
    private javax.swing.JTextField xSpacing;
    private javax.swing.JLabel yOverlapLabel;
    private javax.swing.JTextField yRepeat;
    private javax.swing.JTextField ySpacing;
    // End of variables declaration//GEN-END:variables
}
