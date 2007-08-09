/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Spread.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.CircuitChangeJobs;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;


/**
 * Class to handle the "Spread" dialog.
 */
public class Spread extends EDialog
{
    NodeInst ni;

	public static void showSpreadDialog()
	{
        EditWindow wnd = EditWindow.needCurrent();
		NodeInst ni = (NodeInst)wnd.getHighlighter().getOneElectricObject(NodeInst.class);
		if (ni == null) return;

		Spread dialog = new Spread(TopLevel.getCurrentJFrame(), true, ni);
		dialog.setVisible(true);
	}

	/** Creates new form Spread */
	public Spread(java.awt.Frame parent, boolean modal, NodeInst ni)
	{
		super(parent, modal);
        this.ni = ni;
		initComponents();
        getRootPane().setDefaultButton(ok);
		spreadUp.setSelected(true);
		finishInitialization();
	}

	protected void escapePressed() { cancel(null); }


	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        direction = new javax.swing.ButtonGroup();
        cancel = new javax.swing.JButton();
        ok = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        spreadAmount = new javax.swing.JTextField();
        spreadUp = new javax.swing.JRadioButton();
        spreadDown = new javax.swing.JRadioButton();
        spreadLeft = new javax.swing.JRadioButton();
        spreadRight = new javax.swing.JRadioButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Spread About Highlighted");
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
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridheight = 2;
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
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);

        jLabel1.setText("Distance to spread:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel1, gridBagConstraints);

        spreadAmount.setColumns(8);
        spreadAmount.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(spreadAmount, gridBagConstraints);

        spreadUp.setText("Spread up");
        direction.add(spreadUp);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(spreadUp, gridBagConstraints);

        spreadDown.setText("Spread down");
        direction.add(spreadDown);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(spreadDown, gridBagConstraints);

        spreadLeft.setText("Spread left");
        direction.add(spreadLeft);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(spreadLeft, gridBagConstraints);

        spreadRight.setText("Spread right");
        direction.add(spreadRight);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(spreadRight, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void cancel(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancel
	{//GEN-HEADEREND:event_cancel
		closeDialog(null);
	}//GEN-LAST:event_cancel

	private void ok(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ok
	{//GEN-HEADEREND:event_ok
		// spread it
		char direction = 0;
		if (spreadUp.isSelected()) direction = 'u'; else
		if (spreadDown.isSelected()) direction = 'd'; else
		if (spreadLeft.isSelected()) direction = 'l'; else
		if (spreadRight.isSelected()) direction = 'r';
		double amount = TextUtils.atof(spreadAmount.getText());
		if (ni == null) return;
		new SpreadJob(ni, direction, amount);
		closeDialog(null);
	}//GEN-LAST:event_ok

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

	/**
	 * Class to spread a cell in a new thread.
	 */
	private static class SpreadJob extends Job
	{
		private NodeInst ni;
		private char direction;
		private double amount;

		private SpreadJob(NodeInst ni, char direction, double amount)
		{
			super("Spread Circuitry", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.ni = ni;
			this.direction = direction;
			this.amount = amount;
			startJob();
		}

		/**
		 * Method to implement the "spread" command.
		 */
		public boolean doIt() throws JobException
		{
			SizeOffset so = ni.getSizeOffset();
			double sLx = ni.getTrueCenterX() - ni.getXSize()/2 + so.getLowXOffset();
			double sHx = ni.getTrueCenterX() + ni.getXSize()/2 - so.getHighXOffset();
			double sLy = ni.getTrueCenterY() - ni.getYSize()/2 + so.getLowYOffset();
			double sHy = ni.getTrueCenterY() + ni.getYSize()/2 - so.getHighYOffset();

			// spread it
			CircuitChangeJobs.spreadCircuitry(ni.getParent(), ni, direction, amount, sLx, sHx, sLy, sHy);
			return true;
		}
	}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancel;
    private javax.swing.ButtonGroup direction;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JButton ok;
    private javax.swing.JTextField spreadAmount;
    private javax.swing.JRadioButton spreadDown;
    private javax.swing.JRadioButton spreadLeft;
    private javax.swing.JRadioButton spreadRight;
    private javax.swing.JRadioButton spreadUp;
    // End of variables declaration//GEN-END:variables

}
