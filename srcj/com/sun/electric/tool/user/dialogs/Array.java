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

import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.Quick;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.Clipboard;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Dimension2D;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Comparator;
import java.util.Collections;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JOptionPane;


/**
 * Class to handle the "Array" dialog.
 */
public class Array extends javax.swing.JDialog
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
	private static boolean lastLinearDiagonal = false, lastAddNames = false, lastDRCGood = false;
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
		List highs = Highlight.getHighlighted(true, true);
		if (highs.size() == 0)
		{
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
				"Select some objects before arraying them.");
			return;
		}
		Array dialog = new Array(TopLevel.getCurrentJFrame(), true);
		dialog.show();
	}

	/** Creates new form Array */
	private Array(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		setLocation(100, 50);
		initComponents();
	
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
		List highs = Highlight.getHighlighted(true, true);
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
//		if (us_validmesaure)
//		{
//			spaceByMeasuredDistance.setEnabled(true);
//			spacingMeasuredX = abs(us_lastmeasurex);
//			spacingMeasuredY = abs(us_lastmeasurey);
//			if (lastSpacingType == SPACING_MEASURED)
//			{
//				lastXDistance = spacingMeasuredX;
//				lastYDistance = spacingMeasuredY;
//			}
//		} else
		{
			spaceByMeasuredDistance.setEnabled(false);
			if (lastSpacingType == SPACING_MEASURED)
			{
				lastSpacingType = SPACING_EDGE;
				lastXDistance = lastYDistance = 0;
			}
		}

		// load the spacing distances
		xSpacing.setText(Double.toString(lastXDistance));
		ySpacing.setText(Double.toString(lastYDistance));
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
	}

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
		xSpacing.setText(Double.toString(x));
		ySpacing.setText(Double.toString(y));
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
	}

	private void makeArray()
	{
		// create the array
		ArrayStuff job = new ArrayStuff(this);
	}

	/**
	 * Class to create an array in a new thread.
	 */
	protected static class ArrayStuff extends Job
	{
		Array dialog;

		protected ArrayStuff(Array dialog)
		{
			super("Make Array", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.dialog = dialog;
			this.startJob();
		}

		public void doIt()
		{
			Cell cell = null;

			// disallow arraying if lock is on
			for(Iterator it = dialog.selected.keySet().iterator(); it.hasNext(); )
			{
				Geometric geom = (Geometric)it.next();
				cell = geom.getParent();
				if (geom instanceof NodeInst)
				{
					if (CircuitChanges.cantEdit(cell, (NodeInst)geom, true)) return;
				} else
				{
					if (CircuitChanges.cantEdit(cell, null, true)) return;
				}
			}

			// check for nonsense
			if (lastXRepeat <= 1 && lastYRepeat <= 1)
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"One dimension of the array must be greater than 1");
				return;
			}
			if (lastLinearDiagonal && lastXRepeat != 1 && lastYRepeat != 1)
			{
				JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
					"Diagonal arrays need one dimension to be 1");
				return;
			}

			// make lists of nodes and arcs that will be arrayed
			List nodeList = new ArrayList();
			List arcList = new ArrayList();
			for(Iterator it = dialog.selected.keySet().iterator(); it.hasNext(); )
			{
				Geometric geom = (Geometric)it.next();
				cell = geom.getParent();
				if (geom instanceof NodeInst) nodeList.add(geom); else
					arcList.add(geom);
			}
			Collections.sort(nodeList, new GeometricsByName());
			Collections.sort(arcList, new GeometricsByName());

			// determine the distance between arrayed entries
			double xOverlap = dialog.lastXDistance;
			double yOverlap = dialog.lastYDistance;
			if (dialog.lastSpacingType == SPACING_EDGE)
			{
				xOverlap = dialog.bounds.getWidth() - dialog.lastXDistance;
				yOverlap = dialog.bounds.getHeight() - dialog.lastYDistance;
			}
			double cX = dialog.bounds.getCenterX();
			double cY = dialog.bounds.getCenterY();

			// if only arraying where DRC clean, make an array of newly created nodes
			NodeInst [] nodesToCheck = null;
			boolean [] validity = null;
			int checkNodeCount = 0;
			if (lastDRCGood)
			{
				nodesToCheck = new NodeInst[lastXRepeat * lastYRepeat];
				validity = new boolean[lastXRepeat * lastYRepeat];
				if (nodeList.size() == 1)
					nodesToCheck[checkNodeCount++] = (NodeInst)nodeList.get(0);
			}

			// create the array
			int originalX = 0, originalY = 0;
			for(int y=0; y<lastYRepeat; y++) for(int x=0; x<lastXRepeat; x++)
			{
				int xIndex = x;
				int yIndex = y;
				if (lastXCenter) xIndex = x - (lastXRepeat-1)/2;
				if (lastYCenter) yIndex = y - (lastYRepeat-1)/2;
				if (xIndex == 0 && yIndex == 0)
				{
					originalX = x;
					originalY = y;
					continue;
				}

				// initialize for queueing creation of new exports
				List queuedExports = new ArrayList();

				// first replicate the nodes
				boolean firstNode = true;
				for(Iterator it = dialog.selected.keySet().iterator(); it.hasNext(); )
				{
					Geometric geom = (Geometric)it.next();
					if (!(geom instanceof NodeInst)) continue;
					NodeInst ni = (NodeInst)geom;
					double xPos = cX + xOverlap * xIndex;
					if (dialog.lastLinearDiagonal && dialog.lastXRepeat == 1) xPos = cX + xOverlap * yIndex;
					double yPos = cY + yOverlap * yIndex;
					if (dialog.lastLinearDiagonal && dialog.lastYRepeat == 1) yPos = cY + yOverlap * xIndex;
					double xOff = ni.getGrabCenterX() - cX;
					double yOff = ni.getGrabCenterY() - cY;
					if ((xIndex&1) != 0 && dialog.lastXStagger) yPos += yOverlap/2;
					if ((yIndex&1) != 0 && dialog.lastYStagger) xPos += xOverlap/2;
					int ro = ni.getAngle();
					double sx = ni.getXSizeWithMirror();
					double sy = ni.getYSizeWithMirror();
					if ((xIndex&1) != 0 && dialog.lastXFlip)
					{
						sx = -sx;
						xOff = -xOff;
					}
					if ((yIndex&1) != 0 && dialog.lastYFlip)
					{
						sy = -sy;
						yOff = -yOff;
					}
					xPos += xOff;   yPos += yOff;
					NodeInst newNi = NodeInst.makeInstance(ni.getProto(),
						new Point2D.Double(xPos, yPos), sx, sy, ro, cell, null);
					if (newNi == null) return;
					newNi.setProtoTextDescriptor(ni.getProtoTextDescriptor());
					newNi.setNameTextDescriptor(ni.getNameTextDescriptor());
					if (ni.isExpanded()) newNi.setExpanded(); else newNi.clearExpanded();
					if (ni.isHardSelect()) newNi.setHardSelect(); else newNi.clearHardSelect();
					newNi.setTechSpecific(ni.getTechSpecific());
					newNi.copyVars(ni);
					if (lastAddNames)
						setNewName(newNi, x, y);

					// copy the ports, too
					if (User.isDupCopiesExports())
					{
						for(Iterator eit = ni.getExports(); eit.hasNext(); )
						{
							Export pp = (Export)eit.next();
							queuedExports.add(pp);
						}
					}

					ni.setTempObj(newNi);
					if (lastDRCGood && firstNode)
					{
						nodesToCheck[checkNodeCount++] = newNi;
						firstNode = false;
					}
				}

				// create any queued exports
				Clipboard.createQueuedExports(queuedExports);

				// next replicate the arcs
				for(Iterator it = dialog.selected.keySet().iterator(); it.hasNext(); )
				{
					Geometric geom = (Geometric)it.next();
					if (!(geom instanceof ArcInst)) continue;
					ArcInst ai = (ArcInst)geom;
					double cX0 = ai.getHead().getPortInst().getNodeInst().getGrabCenterX();
					double cY0 = ai.getHead().getPortInst().getNodeInst().getGrabCenterY();
					double xOff0 = ai.getHead().getLocation().getX() - cX0;
					double yOff0 = ai.getHead().getLocation().getY() - cY0;

					double cX1 = ai.getTail().getPortInst().getNodeInst().getGrabCenterX();
					double cY1 = ai.getTail().getPortInst().getNodeInst().getGrabCenterY();
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

					NodeInst ni0 = (NodeInst)ai.getHead().getPortInst().getNodeInst().getTempObj();
					NodeInst ni1 = (NodeInst)ai.getTail().getPortInst().getNodeInst().getTempObj();
					cX0 = ni0.getGrabCenterX();
					cY0 = ni0.getGrabCenterY();
					cX1 = ni1.getGrabCenterX();
					cY1 = ni1.getGrabCenterY();
					PortInst pi0 = ni0.findPortInstFromProto(ai.getHead().getPortInst().getPortProto());
					PortInst pi1 = ni1.findPortInstFromProto(ai.getTail().getPortInst().getPortProto());
					ArcInst newAi = ArcInst.makeInstance(ai.getProto(), ai.getWidth(), pi0,
						new Point2D.Double(cX0+xOff0, cY0+yOff0), pi1, new Point2D.Double(cX1+xOff1, cY1+yOff1), null);
					if (newAi == null) return;
					newAi.copyVars(ai);
					if (lastAddNames)
						setNewName(newAi, x, y);
				}
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
				Quick.doCheck(cell, checkNodeCount, nodesToCheck, validity, false);
				for(int i=1; i<checkNodeCount; i++)
				{
					if (!validity[i])
					{
						// delete the node
						nodesToCheck[i].kill();
					}
				}
			}
		}

		static class GeometricsByName implements Comparator
		{
			public int compare(Object o1, Object o2)
			{
				Geometric g1 = (Geometric)o1;
				Geometric g2 = (Geometric)o2;
				String name1 = g1.getName();
				String name2 = g2.getName();
				if (name1 == null || name2 == null) return 0;
				return name1.compareToIgnoreCase(name2);
			}
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
			if (dialog.lastXRepeat <= 1 || dialog.lastYRepeat <= 1)
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
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        getContentPane().add(onlyDRCCorrect, gridBagConstraints);

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
    private javax.swing.JLabel xOverlapLabel;
    private javax.swing.JTextField xRepeat;
    private javax.swing.JTextField xSpacing;
    private javax.swing.JLabel yOverlapLabel;
    private javax.swing.JTextField yRepeat;
    private javax.swing.JTextField ySpacing;
    // End of variables declaration//GEN-END:variables
	
}
