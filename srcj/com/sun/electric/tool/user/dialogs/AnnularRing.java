/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AnnularRing.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Frame;
import java.awt.geom.Point2D;
import java.util.Iterator;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

/**
 * Class to handle the "Annular Ring" dialog.
 */
public class AnnularRing extends EDialog
{
	private static double lastInner = 5;
	private static double lastOuter = 10;
	private static int lastSegments = 32;
	private static int lastDegrees = 360;
	private JList layerJList;
	private DefaultListModel layerModel;
	private Cell cell;

    // To have ability to store directly the PrimitiveNode and not
    // to depende on names to search the PrimitiveNode instance
    // and have ability to handle DRC Exclusion node
    private static class AnnularRingNode
    {
        public PrimitiveNode node;

        AnnularRingNode(PrimitiveNode t) { node = t; }

        // This avoids to call PrimitiveNode.toString() and get
        // extra text.
        public String toString() { return node.getName(); }
    }

	/**
	 * Method to display the dialog for building annular rings.
	 */
	public static void showAnnularRingDialog()
	{
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;
		int total = 0;
		for(Iterator<PrimitiveNode> it = Technology.getCurrent().getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = it.next();
			if (np.getFunction() == PrimitiveNode.Function.NODE) total++;
		}
		if (total == 0)
		{
			System.out.println("The " + Technology.getCurrent().getTechName() + " technology has no pure-layer nodes");
			return;
		}
		AnnularRing dialog = new AnnularRing(TopLevel.getCurrentJFrame(), true, cell);
		dialog.setVisible(true);
	}

    /** Creates new form AnnularRing */
	private AnnularRing(Frame parent, boolean modal, Cell cell)
    {
		super(parent, modal);
		this.cell = cell;
		initComponents();
		getRootPane().setDefaultButton(ok);

		layerModel = new DefaultListModel();
		layerJList = new JList(layerModel);
		layerJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		layerPane.setViewportView(layerJList);
		for(Iterator<PrimitiveNode> it = Technology.getCurrent().getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = it.next();
			if (np.getFunction() != PrimitiveNode.Function.NODE) continue;
			layerModel.addElement(new AnnularRingNode(np));
		}
        layerModel.addElement(new AnnularRingNode(Generic.tech.drcNode));
        layerModel.addElement(new AnnularRingNode(Generic.tech.afgNode));
		layerJList.setSelectedIndex(0);

		innerRadius.setText(TextUtils.formatDouble(lastInner));
		outerRadius.setText(TextUtils.formatDouble(lastOuter));
		numSegments.setText(Integer.toString(lastSegments));
		numDegrees.setText(Integer.toString(lastDegrees));
		finishInitialization();
   }

	protected void escapePressed() { cancelActionPerformed(null); }

	private void cacheValues()
	{
		lastInner = TextUtils.atof(innerRadius.getText());
		lastOuter = TextUtils.atof(outerRadius.getText());
		lastSegments = TextUtils.atoi(numSegments.getText());
		lastDegrees = TextUtils.atoi(numDegrees.getText());
	}

	private void makeRing()
	{
		cacheValues();
		if (lastSegments < 4) lastSegments = 4;
		if (lastDegrees <= 0) lastDegrees = 360;
		if (lastDegrees > 360) lastDegrees = 360;
		int degrees = lastDegrees * 10;

		// figure out what node to use
        PrimitiveNode np = ((AnnularRingNode)layerJList.getSelectedValue()).node;
		if (np == null) return;
		new MakeAnnulus(cell, np, lastSegments, degrees, lastInner, lastOuter);
	}

	/**
	 * This class finishes the Annular Ring command by creating the ring.
	 */
	private static class MakeAnnulus extends Job
	{
		private Cell cell;
		private PrimitiveNode np;
		private int segments, degrees;
		private double inner, outer;

		protected MakeAnnulus(Cell cell, PrimitiveNode np, int segments, int degrees, double inner, double outer)
		{
			super("Make Annular Ring", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.np = np;
			this.segments = segments;
			this.degrees = degrees;
			this.inner = inner;
			this.outer = outer;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// allocate space for the trace
			int numSegments = segments + 1;
			if (inner == 0 && degrees < 3600) numSegments += 2;
			if (inner > 0) numSegments *= 2;
			EPoint [] points = new EPoint[numSegments];
	
			int l = 0;
			if (inner > 0)
			{
				for(int i=0; i<=segments; i++)
				{
					int p = degrees * i / segments;
					double x = inner * DBMath.cos(p);
					double y = inner * DBMath.sin(p);
					points[l++] = new EPoint(x, y);
				}
			}
			if (inner == 0 && degrees < 3600)
				points[l++] = new EPoint(0, 0);
			for(int i=segments; i>=0; i--)
			{
				int p = degrees*i/segments;
				double x = outer * DBMath.cos(p);
				double y = outer * DBMath.sin(p);
				points[l++] = new EPoint(x, y);
			}
			if (inner == 0 && degrees < 3600)
				points[l++] = new EPoint(0, 0);
			double lX = points[0].getX();
			double hX = lX;
			double lY = points[0].getY();
			double hY = lY;
			for(int i=1; i<points.length; i++)
			{
				if (points[i].getX() < lX) lX = points[i].getX();
				if (points[i].getX() > hX) hX = points[i].getX();
				if (points[i].getY() < lY) lY = points[i].getY();
				if (points[i].getY() > hY) hY = points[i].getY();
			}
			double cX = (lX + hX) / 2;
			double cY = (lY + hY) / 2;
			for(int i=0; i<points.length; i++)
				points[i] = new EPoint(points[i].getX() - cX, points[i].getY() - cY);
			double sX = hX - lX;
			double sY = hY - lY;

			Point2D center = new Point2D.Double(0, 0);
			NodeInst ni = NodeInst.makeInstance(np, center, sX, sY, cell);
			ni.newVar(NodeInst.TRACE, points);
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

        layerPane = new javax.swing.JScrollPane();
        cancel = new javax.swing.JButton();
        ok = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        innerRadius = new javax.swing.JTextField();
        outerRadius = new javax.swing.JTextField();
        numSegments = new javax.swing.JTextField();
        numDegrees = new javax.swing.JTextField();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Annulus Construction");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        layerPane.setMinimumSize(new java.awt.Dimension(200, 200));
        layerPane.setPreferredSize(new java.awt.Dimension(200, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(layerPane, gridBagConstraints);

        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cancelActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(cancel, gridBagConstraints);

        ok.setText("OK");
        ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                okActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);

        jLabel1.setText("Layer to use for ring:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel1, gridBagConstraints);

        jLabel2.setText("Inner Radius:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel2, gridBagConstraints);

        jLabel3.setText("Outer Radius:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel3, gridBagConstraints);

        jLabel4.setText("Number of segments:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel4, gridBagConstraints);

        jLabel5.setText("Number of degrees:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel5, gridBagConstraints);

        innerRadius.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        getContentPane().add(innerRadius, gridBagConstraints);

        outerRadius.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        getContentPane().add(outerRadius, gridBagConstraints);

        numSegments.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        getContentPane().add(numSegments, gridBagConstraints);

        numDegrees.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        getContentPane().add(numDegrees, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void okActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_okActionPerformed
	{//GEN-HEADEREND:event_okActionPerformed
		makeRing();
		closeDialog(null);
	}//GEN-LAST:event_okActionPerformed

	private void cancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancelActionPerformed
	{//GEN-HEADEREND:event_cancelActionPerformed
		cacheValues();
		closeDialog(null);
	}//GEN-LAST:event_cancelActionPerformed
    
    /** Closes the dialog */
    private void closeDialog(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_closeDialog
		setVisible(false);
		dispose();
    }//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancel;
    private javax.swing.JTextField innerRadius;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane layerPane;
    private javax.swing.JTextField numDegrees;
    private javax.swing.JTextField numSegments;
    private javax.swing.JButton ok;
    private javax.swing.JTextField outerRadius;
    // End of variables declaration//GEN-END:variables
    
}
