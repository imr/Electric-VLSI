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

import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.Quick;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.ExportChanges;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.MeasureListener;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;


/**
 * Class to handle the "Array" dialog.
 */
public class Array extends EDialog
{
	/** Space by edge overlap. */				private static final int SPACING_EDGE = 1;
	/** Space by centerline distance. */		private static final int SPACING_CENTER = 2;
	/** Space by characteristic distance. */	private static final int SPACING_CHARACTERISTIC = 3;
	/** Space by measured distance. */			private static final int SPACING_MEASURED = 4;

	private static int lastXRepeat = 1, lastYRepeat = 1;
	private static double lastXDistance = 0, lastYDistance = 0;
	private static boolean lastXFlip = false, lastYFlip = false;
	private static boolean lastXStagger = false, lastYStagger = false;
	private static boolean lastXCenter = false, lastYCenter = false;
	private static boolean lastLinearDiagonal = false, lastAddNames = false, lastDRCGood = false, lastTranspose = false;
	private static int lastSpacingType = SPACING_EDGE;
	/** amount when spacing by edge overlap */				private double spacingOverX, spacingOverY;
	/** amount when spacing by centerline distance */		private double spacingCenterlineX, spacingCenterlineY;
	/** amount when spacing by characteristic distance */	private double spacingCharacteristicX, spacingCharacteristicY;
	/** amount when spacing by measured distance */			private double spacingMeasuredX, spacingMeasuredY;
	/** the selected objects to be arrayed */				private HashMap selected;
	/** the bounds of the selected objects */				private Rectangle2D bounds;

	public static void showArrayDialog()
	{
		// first make sure something is selected
        EditWindow wnd = EditWindow.getCurrent();
		List highs = wnd.getHighlighter().getHighlightedEObjs(true, true);
		if (highs.size() == 0)
		{
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
				"Select some objects before arraying them.");
			return;
		}
		Array dialog = new Array(TopLevel.getCurrentJFrame(), true);
		dialog.setVisible(true);
	}

	/** Creates new form Array */
	private Array(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
        getRootPane().setDefaultButton(ok);

		// load the repeat factors
		xRepeat.setText(Integer.toString(lastXRepeat));
		flipAlternateColumns.setSelected(lastXFlip);
		staggerAlternateColumns.setSelected(lastXStagger);
		centerXAboutOriginal.setSelected(lastXCenter);
		yRepeat.setText(Integer.toString(lastYRepeat));
		flipAlternateRows.setSelected(lastYFlip);
		staggerAlternateRows.setSelected(lastYStagger);
		centerYAboutOriginal.setSelected(lastYCenter);

		// see if a single cell instance is selected (in which case DRC validity can be done)
		onlyDRCCorrect.setEnabled(false);
        EditWindow wnd = EditWindow.getCurrent();
		List highs = wnd.getHighlighter().getHighlightedEObjs(true, true);
		if (highs.size() == 1)
		{
			ElectricObject eObj = (ElectricObject)highs.get(0);
			if (eObj instanceof NodeInst)
			{
				onlyDRCCorrect.setEnabled(true);
			}
		}
		linearDiagonalArray.setSelected(lastLinearDiagonal);
		generateArrayIndices.setSelected(lastAddNames);
		onlyDRCCorrect.setSelected(lastDRCGood);
		transposePlacement.setSelected(lastTranspose);

		// see if a cell was selected which has a characteristic distance
		spacingCharacteristicX = spacingCharacteristicY = 0;
		boolean haveChar = false;
		for(Iterator it = highs.iterator(); it.hasNext(); )
		{
			ElectricObject eObj = (ElectricObject)it.next();
			if (!(eObj instanceof NodeInst)) continue;
			NodeInst ni = (NodeInst)eObj;
			if (!(ni.getProto() instanceof Cell)) continue;
			Cell subCell = (Cell)ni.getProto();
			Dimension2D spacing = subCell.getCharacteristicSpacing();
			if (spacing == null) continue;
			double thisDistX = spacing.getWidth();
			double thisDistY = spacing.getHeight();
			if (ni.isMirroredAboutXAxis() ^ ni.isMirroredAboutYAxis())
			{
				double swap = thisDistX;   thisDistX = thisDistY;   thisDistY = swap;
			}
			if (haveChar)
			{
				if (spacingCharacteristicX != thisDistX || spacingCharacteristicY != thisDistY)
				{
					haveChar = false;
					break;
				}
			}
			spacingCharacteristicX = thisDistX;
			spacingCharacteristicY = thisDistY;
			haveChar = true;
		}
		spaceByCharacteristicSpacing.setEnabled(haveChar);
		if (haveChar)
		{
			if (lastSpacingType == SPACING_CHARACTERISTIC)
			{
				lastXDistance = spacingCharacteristicX;
				lastYDistance = spacingCharacteristicY;
			}
		} else
		{
			if (lastSpacingType == SPACING_CHARACTERISTIC)
			{
				lastSpacingType = SPACING_EDGE;
				lastXDistance = lastYDistance = 0;
			}
		}

		// see if there was a measured distance
		Dimension2D dim = MeasureListener.getLastMeasuredDistance();
		if (dim.getWidth() > 0 || dim.getHeight() > 0)
		{
			spaceByMeasuredDistance.setEnabled(true);
			spacingMeasuredX = dim.getWidth();
			spacingMeasuredY = dim.getHeight();
			if (lastSpacingType == SPACING_MEASURED)
			{
				lastXDistance = spacingMeasuredX;
				lastYDistance = spacingMeasuredY;
			}
		} else
		{
			spaceByMeasuredDistance.setEnabled(false);
			if (lastSpacingType == SPACING_MEASURED)
			{
				lastSpacingType = SPACING_EDGE;
				lastXDistance = lastYDistance = 0;
			}
		}

		// load the spacing distances
		xSpacing.setText(TextUtils.formatDouble(lastXDistance));
		ySpacing.setText(TextUtils.formatDouble(lastYDistance));
		switch (lastSpacingType)
		{
			case SPACING_EDGE:           spaceByEdgeOverlap.setSelected(true);             break;
			case SPACING_CENTER:         spaceByCenterlineDistance.setSelected(true);      break;
			case SPACING_CHARACTERISTIC: spaceByCharacteristicSpacing.setSelected(true);   break;
			case SPACING_MEASURED:       spaceByMeasuredDistance.setSelected(true);        break;
		}
		if (lastSpacingType == SPACING_EDGE)
		{
			xOverlapLabel.setText("X edge overlap:");
			yOverlapLabel.setText("Y edge overlap:");
		} else
		{
			xOverlapLabel.setText("X centerline distance:");
			yOverlapLabel.setText("Y centerline distance:");
		}

		// mark the list of nodes and arcs in the cell that will be arrayed
		selected = new HashMap();
		for(Iterator it = highs.iterator(); it.hasNext(); )
		{
			ElectricObject eObj = (ElectricObject)it.next();
			if (eObj instanceof NodeInst)
			{
				selected.put(eObj, eObj);
			} else if (eObj instanceof ArcInst)
			{
				ArcInst ai = (ArcInst)eObj;
				NodeInst niHead = ai.getHead().getPortInst().getNodeInst();
				selected.put(niHead,  niHead);
				NodeInst niTail = ai.getTail().getPortInst().getNodeInst();
				selected.put(niTail,  niTail);
				selected.put(ai, ai);
			}
		}

		// determine spacing between arrayed objects
		boolean first = true;
		bounds = new Rectangle2D.Double();
		for(Iterator it = selected.keySet().iterator(); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
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
		spacingOverX = spacingOverY = 0;
		spaceByEdgeOverlap.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { newSpacingSelected(); }
		});
		spaceByCenterlineDistance.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { newSpacingSelected(); }
		});
		spaceByCharacteristicSpacing.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { newSpacingSelected(); }
		});
		spaceByMeasuredDistance.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { newSpacingSelected(); }
		});
		finishInitialization();
	}

	protected void escapePressed() { cancel(null); }

	private void newSpacingSelected()
	{
		double x = TextUtils.atof(xSpacing.getText());
		double y = TextUtils.atof(ySpacing.getText());
		switch (lastSpacingType)
		{
			case SPACING_EDGE:   spacingOverX = x;         spacingOverY = y;         break;
			case SPACING_CENTER: spacingCenterlineX = x;   spacingCenterlineY = y;   break;
		}
		if (spaceByEdgeOverlap.isSelected()) lastSpacingType = SPACING_EDGE; else
		if (spaceByCenterlineDistance.isSelected()) lastSpacingType = SPACING_CENTER; else
		if (spaceByCharacteristicSpacing.isSelected()) lastSpacingType = SPACING_CHARACTERISTIC; else
		if (spaceByMeasuredDistance.isSelected()) lastSpacingType = SPACING_MEASURED;
		if (lastSpacingType == SPACING_EDGE)
		{
			xOverlapLabel.setText("X edge overlap:");
			yOverlapLabel.setText("Y edge overlap:");
		} else
		{
			xOverlapLabel.setText("X centerline distance:");
			yOverlapLabel.setText("Y centerline distance:");
		}
		switch (lastSpacingType)
		{
			case SPACING_EDGE:            x = spacingOverX;             y = spacingOverY;             break;
			case SPACING_CENTER:          x = spacingCenterlineX;       y = spacingCenterlineY;       break;
			case SPACING_CHARACTERISTIC:  x = spacingCharacteristicX;   y = spacingCharacteristicY;   break;
			case SPACING_MEASURED:        x = spacingMeasuredX;         y = spacingMeasuredY;         break;
		}
		xSpacing.setText(TextUtils.formatDouble(x));
		ySpacing.setText(TextUtils.formatDouble(y));
	}

	private void rememberFields()
	{
		// gather all "last" values
		lastXRepeat = TextUtils.atoi(xRepeat.getText());
		lastXFlip = flipAlternateColumns.isSelected();
		lastXStagger = staggerAlternateColumns.isSelected();
		lastXCenter = centerXAboutOriginal.isSelected();
		lastYRepeat = TextUtils.atoi(yRepeat.getText());
		lastYFlip = flipAlternateRows.isSelected();
		lastYStagger = staggerAlternateRows.isSelected();
		lastYCenter = centerYAboutOriginal.isSelected();
		lastXDistance = TextUtils.atof(xSpacing.getText());
		lastYDistance = TextUtils.atof(ySpacing.getText());
		lastLinearDiagonal = linearDiagonalArray.isSelected();
		lastAddNames = generateArrayIndices.isSelected();
		lastDRCGood = onlyDRCCorrect.isSelected();
		lastTranspose = transposePlacement.isSelected();
	}

	private void makeArray()
	{
		// create the array
		ArrayStuff job = new ArrayStuff(this);
	}

	/**
	 * Class to create an array in a new thread.
	 */
	private static class ArrayStuff extends Job
	{
		Array dialog;

		protected ArrayStuff(Array dialog)
		{
			super("Make Array", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.dialog = dialog;
			startJob();
		}

		public boolean doIt()
		{
			Cell cell = null;

			// disallow arraying if lock is on
			for(Iterator it = dialog.selected.keySet().iterator(); it.hasNext(); )
			{
				Geometric geom = (Geometric)it.next();
				cell = geom.getParent();
				if (geom instanceof NodeInst)
				{
					if (CircuitChanges.cantEdit(cell, (NodeInst)geom, true) != 0) return false;
				} else
				{
					if (CircuitChanges.cantEdit(cell, null, true) != 0) return false;
				}
			}

			// check for nonsense
			int xRepeat = Math.abs(lastXRepeat);
			int yRepeat = Math.abs(lastYRepeat);
			if (xRepeat <= 1 && yRepeat <= 1)
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"One dimension of the array must be greater than 1");
				return false;
			}
			if (lastLinearDiagonal && xRepeat != 1 && yRepeat != 1)
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"Diagonal arrays need one dimension to be 1");
				return false;
			}

			// make lists of nodes and arcs that will be arrayed
			List nodeList = new ArrayList();
			List arcList = new ArrayList();
			List exportList = new ArrayList();
			for(Iterator it = dialog.selected.keySet().iterator(); it.hasNext(); )
			{
				Geometric geom = (Geometric)it.next();
				cell = geom.getParent();
				if (geom instanceof NodeInst)
				{
					nodeList.add(geom);
					if (User.isDupCopiesExports())
					{
						NodeInst ni = (NodeInst)geom;
						for(Iterator eIt = ni.getExports(); eIt.hasNext(); )
							exportList.add(eIt.next());
					}
				} else
				{
					arcList.add(geom);
				}
			}
			Collections.sort(nodeList, new TextUtils.GeometricsByName());
			Collections.sort(arcList, new TextUtils.GeometricsByName());
			Collections.sort(exportList, new TextUtils.ExportsByName());

			// determine the distance between arrayed entries
			double xOverlap = lastXDistance;
			double yOverlap = lastYDistance;
			if (lastSpacingType == SPACING_EDGE)
			{
				xOverlap = dialog.bounds.getWidth() - lastXDistance;
				yOverlap = dialog.bounds.getHeight() - lastYDistance;
			}
			double cX = dialog.bounds.getCenterX();
			double cY = dialog.bounds.getCenterY();

			// if only arraying where DRC clean, make an array of newly created nodes
			Geometric [] geomsToCheck = null;
			boolean [] validity = null;
			int checkNodeCount = 0;
			if (lastDRCGood)
			{
				geomsToCheck = new NodeInst[xRepeat * yRepeat];
				validity = new boolean[xRepeat * yRepeat];
				if (nodeList.size() == 1)
					geomsToCheck[checkNodeCount++] = (NodeInst)nodeList.get(0);
			}

			// create the array
			int originalX = 0, originalY = 0;
			int total = yRepeat * xRepeat;
			for(int index = 0; index < total; index++)
			{
				int x = index % xRepeat;
				int y = index / xRepeat;
				if (lastTranspose)
				{
					y = index % yRepeat;
					x = index / yRepeat;
				}
				int xIndex = x;
				int yIndex = y;
				if (lastXCenter) xIndex = x - (xRepeat-1)/2;
				if (lastYCenter) yIndex = y - (yRepeat-1)/2;
				if (lastXRepeat < 0) xIndex = -xIndex;
				if (lastYRepeat < 0) yIndex = -yIndex;
				if (xIndex == 0 && yIndex == 0)
				{
					originalX = x;
					originalY = y;
					continue;
				}

				// first replicate the nodes
				boolean firstNode = true;
				HashMap nodeMap = new HashMap();
				for(Iterator it = nodeList.iterator(); it.hasNext(); )
				{
					NodeInst ni = (NodeInst)it.next();
					double xPos = cX + xOverlap * xIndex;
					if (lastLinearDiagonal && xRepeat == 1) xPos = cX + xOverlap * yIndex;
					double yPos = cY + yOverlap * yIndex;
					if (lastLinearDiagonal && yRepeat == 1) yPos = cY + yOverlap * xIndex;
					double xOff = ni.getAnchorCenterX() - cX;
					double yOff = ni.getAnchorCenterY() - cY;
					if ((xIndex&1) != 0 && lastXStagger) yPos += yOverlap/2;
					if ((yIndex&1) != 0 && lastYStagger) xPos += xOverlap/2;
					int ro = ni.getAngle();
					double sx = ni.getXSizeWithMirror();
					double sy = ni.getYSizeWithMirror();
					if ((xIndex&1) != 0 && lastXFlip)
					{
						sx = -sx;
						xOff = -xOff;
					}
					if ((yIndex&1) != 0 && lastYFlip)
					{
						sy = -sy;
						yOff = -yOff;
					}
					xPos += xOff;   yPos += yOff;
					NodeInst newNi = NodeInst.makeInstance(ni.getProto(),
						new Point2D.Double(xPos, yPos), sx, sy, ro, cell, null, 0);
					if (newNi == null) continue;
					newNi.setProtoTextDescriptor(ni.getProtoTextDescriptor());
					newNi.setNameTextDescriptor(ni.getNameTextDescriptor());
					if (ni.isExpanded()) newNi.setExpanded(); else newNi.clearExpanded();
					if (ni.isHardSelect()) newNi.setHardSelect(); else newNi.clearHardSelect();
					newNi.setTechSpecific(ni.getTechSpecific());
					newNi.copyVarsFrom(ni);
					if (lastAddNames)
					{
						setNewName(newNi, x, y);
					} else
					{
						String nodeName = ni.getName();
						if (nodeName != null)
						{
							newNi.setName(ElectricObject.uniqueObjectName(nodeName, cell, NodeInst.class));
							newNi.setNameTextDescriptor(ni.getNameTextDescriptor());
						}
					}

					nodeMap.put(ni, newNi);
					if (lastDRCGood && firstNode)
					{
						geomsToCheck[checkNodeCount++] = newNi;
						firstNode = false;
					}
				}

				// next replicate the arcs
				for(Iterator it = arcList.iterator(); it.hasNext(); )
				{
					ArcInst ai = (ArcInst)it.next();
					double cX0 = ai.getHead().getPortInst().getNodeInst().getAnchorCenterX();
					double cY0 = ai.getHead().getPortInst().getNodeInst().getAnchorCenterY();
					double xOff0 = ai.getHead().getLocation().getX() - cX0;
					double yOff0 = ai.getHead().getLocation().getY() - cY0;

					double cX1 = ai.getTail().getPortInst().getNodeInst().getAnchorCenterX();
					double cY1 = ai.getTail().getPortInst().getNodeInst().getAnchorCenterY();
					double xOff1 = ai.getTail().getLocation().getX() - cX1;
					double yOff1 = ai.getTail().getLocation().getY() - cY1;

					if ((xIndex&1) != 0 && lastXFlip)
					{
						xOff0 = -xOff0;
						xOff1 = -xOff1;
					}
					if ((yIndex&1) != 0 && lastYFlip)
					{
						yOff0 = -yOff0;
						yOff1 = -yOff1;
					}

					NodeInst ni0 = (NodeInst)nodeMap.get(ai.getHead().getPortInst().getNodeInst());
					if (ni0 == null) continue;
					NodeInst ni1 = (NodeInst)nodeMap.get(ai.getTail().getPortInst().getNodeInst());
					if (ni1 == null) continue;
					cX0 = ni0.getAnchorCenterX();
					cY0 = ni0.getAnchorCenterY();
					cX1 = ni1.getAnchorCenterX();
					cY1 = ni1.getAnchorCenterY();
					PortInst pi0 = ni0.findPortInstFromProto(ai.getHead().getPortInst().getPortProto());
					PortInst pi1 = ni1.findPortInstFromProto(ai.getTail().getPortInst().getPortProto());
					ArcInst newAi = ArcInst.makeInstance(ai.getProto(), ai.getWidth(), pi0,
						new Point2D.Double(cX0+xOff0, cY0+yOff0), pi1, new Point2D.Double(cX1+xOff1, cY1+yOff1), null);
					if (newAi == null) continue;
					newAi.copyPropertiesFrom(ai);

					if (lastAddNames)
					{
						setNewName(newAi, x, y);
					} else
					{
						String arcName = ai.getName();
						if (arcName != null) {
							newAi.setName(ElectricObject.uniqueObjectName(arcName, cell, ArcInst.class));
							newAi.getNameTextDescriptor().copy(ai.getNameTextDescriptor());
						}
					}
				}

				// copy the exports, too
				List portInstsToExport = new ArrayList();
				HashMap originalExports = new HashMap();
				for(Iterator eit = exportList.iterator(); eit.hasNext(); )
				{
					Export pp = (Export)eit.next();
					PortInst oldPI = pp.getOriginalPort();
					NodeInst newNI = (NodeInst)nodeMap.get(oldPI.getNodeInst());
					if (newNI == null) continue;
					PortInst pi = newNI.findPortInstFromProto(oldPI.getPortProto());
					portInstsToExport.add(pi);
					originalExports.put(pi, pp);
				}
				ExportChanges.reExportPorts(portInstsToExport, false, true, false, originalExports);
			}

			// rename the replicated objects
			if (lastAddNames)
			{
				for(Iterator it = dialog.selected.keySet().iterator(); it.hasNext(); )
				{
					Geometric geom = (Geometric)it.next();
					setNewName(geom, originalX, originalY);
				}
			}

			// if only arraying where DRC valid, check them now and delete what is not valid
			if (lastDRCGood)
			{
				Quick.checkDesignRules(cell, checkNodeCount, geomsToCheck, validity, null, null);
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
			String geomName = geom.getName();
			if (geomName != null)
			{
				if (!geomName.equals("0") && !geomName.equals("0-0"))
					objName = geomName.toString();
			}
			String totalName = objName + x + "-" + y;
			if (Math.abs(lastXRepeat) <= 1 || Math.abs(lastYRepeat) <= 1)
				totalName = objName + (x+y);
			geom.setName(totalName);

			if (geom instanceof NodeInst && ((NodeInst)geom).getProto() instanceof Cell)
			{
				NodeInst ni = (NodeInst)geom;
				TextDescriptor td = ni.getNameTextDescriptor();
				td.setOff(0, ni.getYSize() / 4);
			}
		}
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
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
        spaceByCharacteristicSpacing = new javax.swing.JRadioButton();
        spaceByMeasuredDistance = new javax.swing.JRadioButton();
        linearDiagonalArray = new javax.swing.JCheckBox();
        generateArrayIndices = new javax.swing.JCheckBox();
        onlyDRCCorrect = new javax.swing.JCheckBox();
        transposePlacement = new javax.swing.JCheckBox();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Array");
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
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridheight = 3;
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
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.weightx = 0.5;
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

        spaceByEdgeOverlap.setText("Space by edge overlap");
        spacing.add(spaceByEdgeOverlap);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(spaceByEdgeOverlap, gridBagConstraints);

        spaceByCenterlineDistance.setText("Space by centerline distance");
        spacing.add(spaceByCenterlineDistance);
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

        spaceByCharacteristicSpacing.setText("Space by characteristic spacing");
        spacing.add(spaceByCharacteristicSpacing);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(spaceByCharacteristicSpacing, gridBagConstraints);

        spaceByMeasuredDistance.setText("Space by last measured distance");
        spacing.add(spaceByMeasuredDistance);
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
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(onlyDRCCorrect, gridBagConstraints);

        transposePlacement.setText("Transpose placement ordering");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(transposePlacement, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

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
    private javax.swing.JButton cancel;
    private javax.swing.JCheckBox centerXAboutOriginal;
    private javax.swing.JCheckBox centerYAboutOriginal;
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
    private javax.swing.JRadioButton spaceByCharacteristicSpacing;
    private javax.swing.JRadioButton spaceByEdgeOverlap;
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
