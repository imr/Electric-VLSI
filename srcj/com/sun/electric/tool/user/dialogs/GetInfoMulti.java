/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GetInfoMulti.java
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
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.change.DatabaseChangeListener;
import com.sun.electric.database.change.Undo;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.HighlightListener;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import javax.swing.*;


/**
 * Class to handle the "Multi-object Get Info" dialog.
 */
public class GetInfoMulti extends EDialog implements HighlightListener, DatabaseChangeListener
{
	private static GetInfoMulti theDialog = null;
	private DefaultListModel listModel;
	private JList list;
	private List highlightList;
	private String initialXPosition, initialYPosition, initialXSize, initialYSize, initialWidth;
	private int numNodes, numArcs, numExports;

	/**
	 * Method to show the Multi-object Get-Info dialog.
	 */
	public static void showDialog()
	{
		if (theDialog == null)
		{
			JFrame jf = TopLevel.getCurrentJFrame();
			theDialog = new GetInfoMulti(jf, false);
		}
        theDialog.loadMultiInfo();
        if (!theDialog.isVisible()) theDialog.pack();
		theDialog.show();
	}

	/**
	 * Reloads the dialog when Highlights change
	 */
	public void highlightChanged()
	{
        if (!isVisible()) return;
		loadMultiInfo();
	}

    /**
     * Respond to database changes we care about
     * @param batch a batch of changes
     */
    public void databaseEndChangeBatch(Undo.ChangeBatch batch) {
        if (!isVisible()) return;

        boolean reload = false;
        // reload if any objects that changed are part of our list of highlighted objects
        for (Iterator it = batch.getChanges(); it.hasNext(); ) {
            Undo.Change change = (Undo.Change)it.next();
            ElectricObject obj = change.getObject();
            for (Iterator it2 = highlightList.iterator(); it2.hasNext(); ) {
                Highlight h = (Highlight)it2.next();
                if (obj == h.getElectricObject()) {
                    reload = true; break;
                }
            }
        }
        if (reload) {
            // update dialog
            loadMultiInfo();
        }
    }
    public void databaseChanged(Undo.Change change) {}

	/** Creates new form Multi-Object Get Info */
	private GetInfoMulti(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		highlightList = new ArrayList();
		initComponents();
        getRootPane().setDefaultButton(ok);
        setLocation(100, 50);

        // add myself as a listener to Highlights
        Highlight.addHighlightListener(this);
        Undo.addDatabaseChangeListener(this);

		// make the list
		listModel = new DefaultListModel();
		list = new JList(listModel);
		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		listPane.setViewportView(list);

		characteristics.addItem("Leave selection alone");
		for(Iterator it = PortProto.Characteristic.getOrderedCharacteristics().iterator(); it.hasNext(); )
		{
			PortProto.Characteristic ch = (PortProto.Characteristic)it.next();
			characteristics.addItem(ch.getName());
		}

		selection.addItem("Leave selection alone");
		selection.addItem("Make all Hard-to-select");
		selection.addItem("Make all Easy-to-select");
		loadMultiInfo();
	}

	protected void escapePressed() { cancelActionPerformed(null); }

	private void loadMultiInfo()
	{
		// copy the selected objects to a private list and sort it
		highlightList.clear();
		for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
		{
			highlightList.add(it.next());
		}
		Collections.sort(highlightList, new SortMultipleHighlights());

		// show the list
		numNodes = numArcs = numExports = 0;
		Geometric firstGeom = null;
		Geometric secondGeom = null;
		double xPositionLow=0, xPositionHigh=0, yPositionLow=0, yPositionHigh=0;
		double xSizeLow=0, xSizeHigh=0, ySizeLow=0, ySizeHigh=0;
		double widthLow=0, widthHigh=0;
		selectionCount.setText(Integer.toString(highlightList.size()) + " selections:");
		listModel.clear();
		for(Iterator it = highlightList.iterator(); it.hasNext(); )
		{
			Highlight h = (Highlight)it.next();
			ElectricObject eobj = h.getElectricObject();
			if (h.getType() == Highlight.Type.EOBJ)
			{
				String description = "";
				if (eobj instanceof PortInst)
					eobj = ((PortInst)eobj).getNodeInst();
				if (eobj instanceof Geometric)
				{
					if (firstGeom == null) firstGeom = (Geometric)eobj; else
						if (secondGeom == null) secondGeom = (Geometric)eobj;
				}
				if (eobj instanceof NodeInst)
				{
					NodeInst ni = (NodeInst)eobj;
					if (numNodes != 0)
					{
						xPositionLow = Math.min(xPositionLow, ni.getAnchorCenterX());
						xPositionHigh = Math.max(xPositionHigh, ni.getAnchorCenterX());
						yPositionLow = Math.min(yPositionLow, ni.getAnchorCenterY());
						yPositionHigh = Math.max(yPositionHigh, ni.getAnchorCenterY());
						xSizeLow = Math.min(xSizeLow, ni.getXSize());
						xSizeHigh = Math.max(xSizeHigh, ni.getXSize());
						ySizeLow = Math.min(ySizeLow, ni.getYSize());
						ySizeHigh = Math.max(ySizeHigh, ni.getYSize());
					} else
					{
						xPositionLow = xPositionHigh = ni.getAnchorCenterX();
						yPositionLow = yPositionHigh = ni.getAnchorCenterY();
						xSizeLow = xSizeHigh = ni.getXSize();
						ySizeLow =  ySizeHigh = ni.getYSize();
					}
					numNodes++;
					description = "Node " + ni.describe();
				} else if (eobj instanceof ArcInst)
				{
					ArcInst ai = (ArcInst)eobj;
					if (numArcs != 0)
					{
						widthLow = Math.min(widthLow, ai.getWidth());
						widthHigh = Math.max(widthHigh, ai.getWidth());
					} else
					{
						widthLow = widthHigh = ai.getWidth();
					}
					numArcs++;
					description = "Arc " + ai.describe();
				}
				listModel.addElement(description);
			} else if (h.getType() == Highlight.Type.TEXT)
			{
				String description = "Text: unknown";
				if (h.getVar() != null)
				{
					description = "Text: " + h.getVar().getFullDescription(eobj);
				} else
				{
					if (h.getName() != null)
					{
						if (eobj instanceof NodeInst) description = "Node name for " + ((NodeInst)eobj).describe(); else
							if (eobj instanceof ArcInst) description = "Arc name for " + ((ArcInst)eobj).describe();
					} else if (eobj instanceof Export)
					{
						description = "Text: Export '" + ((Export)eobj).getProtoName() + "'";
						numExports++;
					} else if (eobj instanceof NodeInst)
					{
						description = "Text: Cell instance name " + ((NodeInst)eobj).describe();
					}
				}
				listModel.addElement(description);
			} else if (h.getType() == Highlight.Type.LINE)
			{
				Point2D pt1 = h.getFromPoint();
				Point2D pt2 = h.getToPoint();
				String description = "Line from (" + pt1.getX() + "," + pt1.getY() + ") to (" +
					pt2.getX() + "," + pt2.getY() + ")";
				listModel.addElement(description);
			} else if (h.getType() == Highlight.Type.BBOX)
			{
				Rectangle2D bounds = h.getBounds();
				String description = "Area from " + bounds.getMinX() + "<=X<=" + bounds.getMaxX() +
					" and " + bounds.getMinY() + "<=Y<=" + bounds.getMaxY();
				listModel.addElement(description);
			}
		}

		// with exactly 2 objects, show the distance between them
		if (numNodes + numArcs == 2)
		{
			listModel.addElement("---------------------------");
			Point2D firstPt = firstGeom.getTrueCenter();
			if (firstGeom instanceof NodeInst) firstPt = ((NodeInst)firstGeom).getAnchorCenter();
			Point2D secondPt = secondGeom.getTrueCenter();
			if (secondGeom instanceof NodeInst) secondPt = ((NodeInst)secondGeom).getAnchorCenter();
			listModel.addElement("Distance between centers is " + firstPt.distance(secondPt));
		}
		if (numNodes != 0)
		{
            initialXPosition = "";
			if (xPositionLow == xPositionHigh)
			{
				initialXPosition = Double.toString(xPositionLow);
				xPositionRange.setText("All the same");
			} else
			{
				xPositionRange.setText(xPositionLow + " to " + xPositionHigh);
			}
			xPosition.setEditable(true);
			xPosition.setText(initialXPosition);

			initialYPosition = "";
			if (yPositionLow == yPositionHigh)
			{
				initialYPosition = Double.toString(yPositionLow);
				yPositionRange.setText("All the same");
			} else
			{
				yPositionRange.setText(yPositionLow + " to " + yPositionHigh);
			}
			yPosition.setEditable(true);
			yPosition.setText(initialYPosition);

			initialXSize = "";
			if (xSizeLow == xSizeHigh)
			{
				initialXSize = Double.toString(xSizeLow);
				xSizeRange.setText("All the same");
			} else
			{
				xSizeRange.setText(xSizeLow + " to " + xSizeHigh);
			}
			xSize.setEditable(true);
			xSize.setText(initialXSize);

			initialYSize = "";
			if (ySizeLow == ySizeHigh)
			{
				initialYSize = Double.toString(ySizeLow);
				ySizeRange.setText("All the same");
			} else
			{
				ySizeRange.setText(ySizeLow + " to " + ySizeHigh);
			}
			ySize.setEditable(true);
			ySize.setText(initialYSize);
		} else
		{
			xPosition.setEditable(false);
			xPosition.setText("");
			xPositionRange.setText("");
			yPosition.setEditable(false);
			yPosition.setText("");
			yPositionRange.setText("");
			xSize.setEditable(false);
			xSize.setText("");
			xSizeRange.setText("");
			ySize.setEditable(false);
			ySize.setText("");
			ySizeRange.setText("");
		}
		if (numArcs != 0)
		{
			initialWidth = "";
			if (widthLow == widthHigh)
			{
				initialWidth = Double.toString(widthLow);
				widthRange.setText("All the same");
			} else
			{
				widthRange.setText(widthLow + " to " + widthHigh);
			}
			width.setEditable(true);
			width.setText(initialWidth);
		} else
		{
			width.setEditable(false);
			width.setText("");
			widthRange.setText("");
		}
		characteristics.setEnabled(numExports != 0);
		if (numNodes == 0 && numArcs == 0)
		{
			listPane.setEnabled(false);
			remove.setEnabled(false);
			removeOthers.setEnabled(false);
			apply.setEnabled(false);
			selection.setEnabled(false);
		} else
		{
			listPane.setEnabled(true);
			remove.setEnabled(true);
			removeOthers.setEnabled(true);
			apply.setEnabled(true);
			selection.setEnabled(true);
		}
	}

	static class SortMultipleHighlights implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			Highlight h1 = (Highlight)o1;
			Highlight h2 = (Highlight)o2;

			// if the types are different, order by types
			if (h1.getType() != h2.getType())
			{
				return h1.getType().getOrder() - h2.getType().getOrder();
			}

			// if not a geometric, no order is available
			if (h1.getType() != Highlight.Type.EOBJ) return 0;

			// sort on mix of NodeInst / ArcInst / PortInst
			ElectricObject e1 = h1.getElectricObject();
			int type1 = 0;
			if (e1 instanceof NodeInst) type1 = 1; else
				if (e1 instanceof ArcInst) type1 = 2;
			ElectricObject e2 = h2.getElectricObject();
			int type2 = 0;
			if (e2 instanceof NodeInst) type2 = 1; else
				if (e2 instanceof ArcInst) type2 = 2;
			if (type1 != type2) return type1 - type2;

			// sort on the object name
			String s1 = e1.toString();
			if (e1 instanceof Geometric) s1 = ((Geometric)e1).describe();
			String s2 = e2.toString();
			if (e2 instanceof Geometric) s2 = ((Geometric)e2).describe();
			return s1.compareToIgnoreCase(s2);
		}
	}

	/**
	 * This class implements database changes requested by the dialog.
	 */
	private static class MultiChange extends Job
	{
		GetInfoMulti dialog;

		protected MultiChange(GetInfoMulti dialog)
		{
			super("Modify Objects", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.dialog = dialog;
			startJob();
		}

		public boolean doIt()
		{
			String currentXPosition = dialog.xPosition.getText();
			String currentYPosition = dialog.yPosition.getText();
			String currentXSize = dialog.xSize.getText();
			String currentYSize = dialog.ySize.getText();
			if (!currentXPosition.equals(dialog.initialXPosition) ||
				!currentYPosition.equals(dialog.initialYPosition) ||
				!currentXSize.equals(dialog.initialXSize) ||
				!currentYSize.equals(dialog.initialYSize))
			{
				double newXPosition = TextUtils.atof(currentXPosition);
				double newYPosition = TextUtils.atof(currentYPosition);
				double newXSize = TextUtils.atof(currentXSize);
				double newYSize = TextUtils.atof(currentYSize);
				NodeInst [] nis = new NodeInst[dialog.numNodes];
				double [] dXP = new double[dialog.numNodes];
				double [] dYP = new double[dialog.numNodes];
				double [] dXS = new double[dialog.numNodes];
				double [] dYS = new double[dialog.numNodes];
				int [] dRot = new int[dialog.numNodes];
				int index = 0;
				for(Iterator it = dialog.highlightList.iterator(); it.hasNext(); )
				{
					Highlight h = (Highlight)it.next();
					if (h.getType() != Highlight.Type.EOBJ) continue;
					ElectricObject eobj = h.getElectricObject();
					if (!(eobj instanceof NodeInst)) continue;
					NodeInst ni = (NodeInst)eobj;
					nis[index] = ni;
					if (currentXPosition.equals("")) dXP[index] = 0; else
						dXP[index] = newXPosition - ni.getAnchorCenterX();
					if (currentYPosition.equals("")) dYP[index] = 0; else
						dYP[index] = newYPosition - ni.getAnchorCenterY();
					if (currentXSize.equals("")) dXS[index] = 0; else
						dXS[index] = newXSize - ni.getXSize();
					if (currentYSize.equals("")) dYS[index] = 0; else
						dYS[index] = newYSize - ni.getYSize();
					dRot[index] = 0;
					index++;
				}
				NodeInst.modifyInstances(nis, dXP, dYP, dXS, dYS, dRot);
				dialog.initialXPosition = currentXPosition;
				dialog.initialYPosition = currentYPosition;
				dialog.initialXSize = currentXSize;
				dialog.initialYSize = currentYSize;
			}

			String currentWidth = dialog.width.getText();
			if (!currentWidth.equals(dialog.initialWidth))
			{
				double newWidth = TextUtils.atof(currentWidth);
				for(Iterator it = dialog.highlightList.iterator(); it.hasNext(); )
				{
					Highlight h = (Highlight)it.next();
					if (h.getType() != Highlight.Type.EOBJ) continue;
					ElectricObject eobj = h.getElectricObject();
					if (!(eobj instanceof ArcInst)) continue;
					ArcInst ai = (ArcInst)eobj;
					ai.modify(newWidth - ai.getWidth(), 0, 0, 0, 0);
				}
				dialog.initialWidth = currentWidth;
			}

			int selectEase = dialog.selection.getSelectedIndex();
			if (selectEase != 0)
			{
				for(Iterator it = dialog.highlightList.iterator(); it.hasNext(); )
				{
					Highlight h = (Highlight)it.next();
					if (h.getType() != Highlight.Type.EOBJ) continue;
					ElectricObject eobj = h.getElectricObject();
					if (eobj instanceof NodeInst)
					{
						NodeInst ni = (NodeInst)eobj;
						if (selectEase == 1) ni.setHardSelect(); else
							ni.clearHardSelect();
					} else
					{
						ArcInst ai = (ArcInst)eobj;
						if (selectEase == 1) ai.setHardSelect(); else
							ai.clearHardSelect();
					}
				}
			}

			int exportCharacteristics = dialog.characteristics.getSelectedIndex();
			if (exportCharacteristics != 0)
			{
				String charName = (String)dialog.characteristics.getSelectedItem();
				PortProto.Characteristic ch = PortProto.Characteristic.findCharacteristic(charName);
				for(Iterator it = dialog.highlightList.iterator(); it.hasNext(); )
				{
					Highlight h = (Highlight)it.next();
					if (h.getType() != Highlight.Type.TEXT) continue;
					if (h.getVar() != null) continue;
					if (!(h.getElectricObject() instanceof Export)) continue;
					PortProto pp = (Export)h.getElectricObject();
					pp.setCharacteristic(ch);
				}
			}
			return true;
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

        removeOthers = new javax.swing.JButton();
        apply = new javax.swing.JButton();
        selectionCount = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        listPane = new javax.swing.JScrollPane();
        jLabel3 = new javax.swing.JLabel();
        xPosition = new javax.swing.JTextField();
        xPositionRange = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        yPosition = new javax.swing.JTextField();
        yPositionRange = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        xSize = new javax.swing.JTextField();
        xSizeRange = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        ySize = new javax.swing.JTextField();
        ySizeRange = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        width = new javax.swing.JTextField();
        widthRange = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        jLabel14 = new javax.swing.JLabel();
        characteristics = new javax.swing.JComboBox();
        jSeparator3 = new javax.swing.JSeparator();
        jLabel15 = new javax.swing.JLabel();
        selection = new javax.swing.JComboBox();
        ok = new javax.swing.JButton();
        remove = new javax.swing.JButton();
        cancel = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Multi-Object Properties");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        removeOthers.setText("Remove Others");
        removeOthers.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                removeOthersActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 19;
        gridBagConstraints.weightx = 0.33;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(removeOthers, gridBagConstraints);

        apply.setText("Apply");
        apply.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                applyActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 19;
        gridBagConstraints.weightx = 0.33;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(apply, gridBagConstraints);

        selectionCount.setText("0 selections:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 0);
        getContentPane().add(selectionCount, gridBagConstraints);

        jLabel2.setText("For all selected nodes:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel2, gridBagConstraints);

        listPane.setMinimumSize(new java.awt.Dimension(300, 22));
        listPane.setPreferredSize(new java.awt.Dimension(300, 22));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.gridheight = 18;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(listPane, gridBagConstraints);

        jLabel3.setText("X Position:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        getContentPane().add(jLabel3, gridBagConstraints);

        xPosition.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(xPosition, gridBagConstraints);

        xPositionRange.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(xPositionRange, gridBagConstraints);

        jLabel5.setText("Y Position:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        getContentPane().add(jLabel5, gridBagConstraints);

        yPosition.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(yPosition, gridBagConstraints);

        yPositionRange.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(yPositionRange, gridBagConstraints);

        jLabel7.setText("X Size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        getContentPane().add(jLabel7, gridBagConstraints);

        xSize.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(xSize, gridBagConstraints);

        xSizeRange.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(xSizeRange, gridBagConstraints);

        jLabel9.setText("Y Size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        getContentPane().add(jLabel9, gridBagConstraints);

        ySize.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ySize, gridBagConstraints);

        ySizeRange.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ySizeRange, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(jSeparator1, gridBagConstraints);

        jLabel11.setText("For all selected arcs:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel11, gridBagConstraints);

        jLabel12.setText("Width:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        getContentPane().add(jLabel12, gridBagConstraints);

        width.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(width, gridBagConstraints);

        widthRange.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(widthRange, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(jSeparator2, gridBagConstraints);

        jLabel14.setText("For all selected exports:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel14, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(characteristics, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(jSeparator3, gridBagConstraints);

        jLabel15.setText("For everything:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel15, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(selection, gridBagConstraints);

        ok.setText("OK");
        ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                okActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 19;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);

        remove.setText("Remove");
        remove.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                removeActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 19;
        gridBagConstraints.weightx = 0.33;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(remove, gridBagConstraints);

        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cancelActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 19;
        getContentPane().add(cancel, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void cancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancelActionPerformed
	{//GEN-HEADEREND:event_cancelActionPerformed
		closeDialog(null);
	}//GEN-LAST:event_cancelActionPerformed

	private void okActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_okActionPerformed
	{//GEN-HEADEREND:event_okActionPerformed
		applyActionPerformed(evt);
		closeDialog(null);
	}//GEN-LAST:event_okActionPerformed

	private void removeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_removeActionPerformed
	{//GEN-HEADEREND:event_removeActionPerformed
		int [] items = list.getSelectedIndices();
		List newList = new ArrayList();
		for(int i=0; i<highlightList.size(); i++)
		{
			int j = 0;
			for( ; j<items.length; j++)
				if (i == items[j]) break;
			if (j < items.length) continue;
			newList.add(highlightList.get(i));
		}
		Highlight.clear();
		Highlight.setHighlightList(newList);
		Highlight.finished();
	}//GEN-LAST:event_removeActionPerformed

	private void applyActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_applyActionPerformed
	{//GEN-HEADEREND:event_applyActionPerformed
		MultiChange job = new MultiChange(this);
	}//GEN-LAST:event_applyActionPerformed

	private void removeOthersActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_removeOthersActionPerformed
	{//GEN-HEADEREND:event_removeOthersActionPerformed
		int [] items = list.getSelectedIndices();
		List newList = new ArrayList();
		for(int i=0; i<highlightList.size(); i++)
		{
			int j = 0;
			for( ; j<items.length; j++)
				if (i == items[j]) break;
			if (j >= items.length) continue;
			newList.add(highlightList.get(i));
		}
		highlightList = newList;
		Highlight.clear();
		Highlight.setHighlightList(newList);
		Highlight.finished();
	}//GEN-LAST:event_removeOthersActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		//theDialog = null;
        //Highlight.removeHighlightListener(this);
		//dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton apply;
    private javax.swing.JButton cancel;
    private javax.swing.JComboBox characteristics;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JScrollPane listPane;
    private javax.swing.JButton ok;
    private javax.swing.JButton remove;
    private javax.swing.JButton removeOthers;
    private javax.swing.JComboBox selection;
    private javax.swing.JLabel selectionCount;
    private javax.swing.JTextField width;
    private javax.swing.JLabel widthRange;
    private javax.swing.JTextField xPosition;
    private javax.swing.JLabel xPositionRange;
    private javax.swing.JTextField xSize;
    private javax.swing.JLabel xSizeRange;
    private javax.swing.JTextField yPosition;
    private javax.swing.JLabel yPositionRange;
    private javax.swing.JTextField ySize;
    private javax.swing.JLabel ySizeRange;
    // End of variables declaration//GEN-END:variables
	
}
