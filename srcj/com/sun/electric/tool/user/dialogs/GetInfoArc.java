/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GetInfoArc.java
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

import com.sun.electric.database.change.DatabaseChangeListener;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.HighlightListener;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.geom.Point2D;
import java.util.Iterator;

import javax.swing.JFrame;


/**
 * Class to handle the "Arc Get-Info" dialog.
 */
public class GetInfoArc extends EDialog implements HighlightListener, DatabaseChangeListener
{
	private static GetInfoArc theDialog = null;
	private static ArcInst shownArc = null;
	private String initialName;
	private double initialWidth;
	private boolean initialEasyToSelect;
	private boolean initialRigid, initialFixedAngle, initialSlidable;
	private boolean initialDirectional, initialEndsExtend;
	private boolean initialSkipHead, initialSkipTail, initialReverseEnds;
    private String initialColor;
    private EditWindow wnd;
    /** true if need to reload info due to failure to get Examine lock on DB */ private boolean needReload = false;

	/**
	 * Method to show the Arc Get-Info dialog.
	 */
	public static void showDialog()
	{
		if (theDialog == null)
		{
            JFrame jf;
            if (TopLevel.isMDIMode())
			    jf = TopLevel.getCurrentJFrame();
            else
                jf = null;
			theDialog = new GetInfoArc(jf, false);
		}
        theDialog.loadInfo();
        if (!theDialog.isVisible()) theDialog.pack();
		theDialog.setVisible(true);
	}

    /**
     * Reloads the dialog when Highlights change
     */
    public void highlightChanged(Highlighter which)
	{
        if (!isVisible()) return;
		loadInfo();
	}

    /**
     * Called when by a Highlighter when it loses focus. The argument
     * is the Highlighter that has gained focus (may be null).
     * @param highlighterGainedFocus the highlighter for the current window (may be null).
     */
    public void highlighterLostFocus(Highlighter highlighterGainedFocus) {
        if (!isVisible()) return;
        loadInfo();
    }

    /**
     * Respond to database changes
     * @param batch a batch of changes completed
     */
    public void databaseEndChangeBatch(Undo.ChangeBatch batch) {
        if (!isVisible()) return;

        // check if we need to reload because we couldn't
        // load before because a Change Job was running
        if (needReload) {
            needReload = false;
            loadInfo();
            return;
        }

        // check if we care about the changes
        boolean reload = false;
        for (Iterator it = batch.getChanges(); it.hasNext(); ) {
            Undo.Change change = (Undo.Change)it.next();
            ElectricObject obj = change.getObject();
            if (obj == shownArc) {
                reload = true;
                break;
            }
        }
        if (reload) {
            // update dialog
            loadInfo();
        }
    }

    /** Don't do anything on little database changes, only after all database changes */
    public void databaseChanged(Undo.Change change) {}

    /** This is a GUI listener */
    public boolean isGUIListener() { return true; }

	/** Creates new form Arc Get-Info */
	private GetInfoArc(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
        getRootPane().setDefaultButton(ok);
        Undo.addDatabaseChangeListener(this);

        // populate arc color combo box (only for Artwork technology)
        int [] colorIndices = EGraphics.getColorIndices();
        arcColorComboBox.addItem("");
        for (int i=0; i<colorIndices.length; i++) {
            String str = EGraphics.getColorIndexName(colorIndices[i]);
            arcColorComboBox.addItem(str);
        }
		finishInitialization();
	}

	protected void escapePressed() { cancelActionPerformed(null); }

	protected void loadInfo()
	{
        Job.checkSwingThread();

        // update current window
        EditWindow curWnd = EditWindow.getCurrent();
        if ((wnd != curWnd) && (curWnd != null)) {
            if (wnd != null) wnd.getHighlighter().removeHighlightListener(this);
            curWnd.getHighlighter().addHighlightListener(this);
            wnd = curWnd;
        }
        if (wnd == null) {
            disableDialog();
            return;
        }

		// must have a single node selected
		ArcInst ai = null;
		int arcCount = 0;
		for(Iterator it = wnd.getHighlighter().getHighlights().iterator(); it.hasNext(); )
		{
			Highlight h = (Highlight)it.next();
			if (h.getType() == Highlight.Type.EOBJ)
			{
				ElectricObject eobj = h.getElectricObject();
				if (eobj instanceof ArcInst)
				{
					ai = (ArcInst)eobj;
					arcCount++;
				}
			}
		}
		if (arcCount > 1) ai = null;
		if (ai == null)
		{
			if (shownArc != null)
			{
                disableDialog();
			}
			return;
		}

        // try to get Examine lock. If fails, set needReload to true to
        // call loadInfo again when database is done changing
        if (!Job.acquireExamineLock(false)) {
            needReload = true;
            disableDialog();
            return;
        }
        // else: lock acquired
        try {
            focusClearOnTextField(name);

            // enable it
            name.setEditable(true);
            width.setEditable(true);
            easyToSelect.setEnabled(true);
            rigid.setEnabled(true);
            fixedAngle.setEnabled(true);
            slidable.setEnabled(true);
            directional.setEnabled(true);
            endsExtend.setEnabled(true);
            skipHead.setEnabled(true);
            skipTail.setEnabled(true);
            reverseEnds.setEnabled(true);
            headSee.setEnabled(true);
            tailSee.setEnabled(true);
            apply.setEnabled(true);
            arcColorComboBox.setEnabled(false);

            // get initial values
            initialName = ai.getName();
            initialWidth = ai.getWidth();
            initialEasyToSelect = !ai.isHardSelect();
            initialRigid = ai.isRigid();
            initialFixedAngle = ai.isFixedAngle();
            initialSlidable = ai.isSlidable();
            initialDirectional = ai.isDirectional();
            initialEndsExtend = ai.isExtended();
            initialSkipHead = ai.isSkipHead();
            initialSkipTail = ai.isSkipTail();
            initialReverseEnds = ai.isReverseEnds();

            // load the dialog
            type.setText(ai.getProto().describe());
            Netlist netlist = ai.getParent().getUserNetlist();
            int busWidth = netlist.getBusWidth(ai);
            String netName = netlist.getNetworkName(ai);
            if (netName != null && netName.length() > 80)
            	netName = netName.substring(0, 80) + "...";
            network.setText(netName);
            name.setText(initialName);
            width.setText(TextUtils.formatDouble(initialWidth - ai.getProto().getWidthOffset()));
            busSize.setText(Integer.toString(busWidth));
            angle.setText(TextUtils.formatDouble(ai.getAngle() / 10.0));
            easyToSelect.setSelected(initialEasyToSelect);
            headNode.setText(ai.getHead().getPortInst().getNodeInst().describe());
            Point2D headPt = ai.getHead().getLocation();
            headLoc.setText("(" + headPt.getX() + "," + headPt.getY() + ")");
            tailNode.setText(ai.getTail().getPortInst().getNodeInst().describe());
            Point2D tailPt = ai.getTail().getLocation();
            tailLoc.setText("(" + tailPt.getX() + "," + tailPt.getY() + ")");
            rigid.setSelected(initialRigid);
            fixedAngle.setSelected(initialFixedAngle);
            slidable.setSelected(initialSlidable);
            directional.setSelected(initialDirectional);
            endsExtend.setSelected(initialEndsExtend);
            skipHead.setSelected(initialSkipHead);
            skipTail.setSelected(initialSkipTail);
            reverseEnds.setSelected(initialReverseEnds);

            // arc color
            initialColor = "";
            Variable var = ai.getVar(Artwork.ART_COLOR);
            if (var != null) {
                Integer integer = (Integer)var.getObject();
                String color = EGraphics.getColorIndexName(integer.intValue());
                initialColor = color;
            }
            arcColorComboBox.setSelectedItem(initialColor);
            if (ai.getProto().getTechnology() == Artwork.tech) {
                arcColorComboBox.setEnabled(true);
            }
            Job.releaseExamineLock();
        } catch (Error e) {
            Job.releaseExamineLock();
            throw e;
        }

		shownArc = ai;
        focusOnTextField(name);
	}

    private void disableDialog() {
        // no arc selected, disable the dialog
        type.setText("");
        network.setText("");
        name.setEditable(false);
        name.setText("");
        width.setEditable(false);
        width.setText("");
        busSize.setText("");
        angle.setText("");
        easyToSelect.setEnabled(false);
        headNode.setText("");
        headLoc.setText("");
        headSee.setEnabled(false);
        tailNode.setText("");
        tailLoc.setText("");
        tailSee.setEnabled(false);
        rigid.setEnabled(false);
        rigid.setSelected(false);
        fixedAngle.setEnabled(false);
        fixedAngle.setSelected(false);
        slidable.setEnabled(false);
        slidable.setSelected(false);
        directional.setEnabled(false);
        directional.setSelected(false);
        endsExtend.setEnabled(false);
        endsExtend.setSelected(false);
        skipHead.setEnabled(false);
        skipHead.setSelected(false);
        skipTail.setEnabled(false);
        skipTail.setSelected(false);
        reverseEnds.setEnabled(false);
        reverseEnds.setSelected(false);
        apply.setEnabled(false);
        arcColorComboBox.setEnabled(false);

        shownArc = null;
    }

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        type = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        network = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        name = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        width = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        busSize = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        angle = new javax.swing.JLabel();
        easyToSelect = new javax.swing.JCheckBox();
        jLabel11 = new javax.swing.JLabel();
        headNode = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        headLoc = new javax.swing.JLabel();
        headSee = new javax.swing.JButton();
        jLabel15 = new javax.swing.JLabel();
        tailNode = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        tailLoc = new javax.swing.JLabel();
        tailSee = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        rigid = new javax.swing.JCheckBox();
        reverseEnds = new javax.swing.JCheckBox();
        endsExtend = new javax.swing.JCheckBox();
        slidable = new javax.swing.JCheckBox();
        skipTail = new javax.swing.JCheckBox();
        directional = new javax.swing.JCheckBox();
        fixedAngle = new javax.swing.JCheckBox();
        skipHead = new javax.swing.JCheckBox();
        jPanel3 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        arcColorComboBox = new javax.swing.JComboBox();
        jPanel4 = new javax.swing.JPanel();
        ok = new javax.swing.JButton();
        cancel = new javax.swing.JButton();
        apply = new javax.swing.JButton();
        attributes = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Arc Properties");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("Type:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(jLabel1, gridBagConstraints);

        type.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(type, gridBagConstraints);

        jLabel3.setText("Network:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(jLabel3, gridBagConstraints);

        network.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(network, gridBagConstraints);

        jLabel5.setText("Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(jLabel5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(name, gridBagConstraints);

        jLabel6.setText("Width:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(jLabel6, gridBagConstraints);

        width.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(width, gridBagConstraints);

        jLabel7.setText("Bus size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(jLabel7, gridBagConstraints);

        busSize.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(busSize, gridBagConstraints);

        jLabel9.setText("Angle:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(jLabel9, gridBagConstraints);

        angle.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(angle, gridBagConstraints);

        easyToSelect.setText("Easy to Select");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(easyToSelect, gridBagConstraints);

        jLabel11.setText("Head:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(jLabel11, gridBagConstraints);

        headNode.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(headNode, gridBagConstraints);

        jLabel13.setText("At:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        jPanel1.add(jLabel13, gridBagConstraints);

        headLoc.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(headLoc, gridBagConstraints);

        headSee.setText("See");
        headSee.setMinimumSize(new java.awt.Dimension(56, 20));
        headSee.setPreferredSize(new java.awt.Dimension(56, 20));
        headSee.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                headSeeActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(headSee, gridBagConstraints);

        jLabel15.setText("Tail:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(jLabel15, gridBagConstraints);

        tailNode.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(tailNode, gridBagConstraints);

        jLabel17.setText("At:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        jPanel1.add(jLabel17, gridBagConstraints);

        tailLoc.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(tailLoc, gridBagConstraints);

        tailSee.setText("See");
        tailSee.setMinimumSize(new java.awt.Dimension(56, 20));
        tailSee.setPreferredSize(new java.awt.Dimension(56, 20));
        tailSee.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tailSeeActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(tailSee, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(jPanel1, gridBagConstraints);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        rigid.setText("Rigid");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add(rigid, gridBagConstraints);

        reverseEnds.setText("Reverse head and tail");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add(reverseEnds, gridBagConstraints);

        endsExtend.setText("Ends extend");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add(endsExtend, gridBagConstraints);

        slidable.setText("Slidable");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add(slidable, gridBagConstraints);

        skipTail.setText("Ignore tail");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add(skipTail, gridBagConstraints);

        directional.setText("Directional");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add(directional, gridBagConstraints);

        fixedAngle.setText("Fixed-angle");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add(fixedAngle, gridBagConstraints);

        skipHead.setText("Ignore head");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add(skipHead, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(jPanel2, gridBagConstraints);

        jPanel3.setLayout(new java.awt.GridBagLayout());

        jLabel2.setText("Color: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel3.add(jLabel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.weightx = 1.0;
        jPanel3.add(arcColorComboBox, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(jPanel3, gridBagConstraints);

        jPanel4.setLayout(new java.awt.GridBagLayout());

        ok.setText("OK");
        ok.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.weightx = 1.0;
        jPanel4.add(ok, gridBagConstraints);

        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.weightx = 1.0;
        jPanel4.add(cancel, gridBagConstraints);

        apply.setText("Apply");
        apply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                applyActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.weightx = 1.0;
        jPanel4.add(apply, gridBagConstraints);

        attributes.setText("Attributes");
        attributes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                attributesActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        jPanel4.add(attributes, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(jPanel4, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void attributesActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_attributesActionPerformed
	{//GEN-HEADEREND:event_attributesActionPerformed
		Attributes.showDialog();
	}//GEN-LAST:event_attributesActionPerformed

	private void tailSeeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_tailSeeActionPerformed
	{//GEN-HEADEREND:event_tailSeeActionPerformed
		if (shownArc == null) return;
		ArcInst ai = shownArc;
		NodeInst ni = shownArc.getTail().getPortInst().getNodeInst();
        if (wnd != null) {
            Highlighter highlighter = wnd.getHighlighter();
            highlighter.clear();
            highlighter.addElectricObject(ni, ni.getParent());
            highlighter.addElectricObject(ai, ai.getParent());
            highlighter.finished();
        }
	}//GEN-LAST:event_tailSeeActionPerformed

	private void headSeeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_headSeeActionPerformed
	{//GEN-HEADEREND:event_headSeeActionPerformed
		if (shownArc == null) return;
		ArcInst ai = shownArc;
		NodeInst ni = shownArc.getHead().getPortInst().getNodeInst();
        if (wnd != null) {
            Highlighter highlighter = wnd.getHighlighter();
            highlighter.clear();
            highlighter.addElectricObject(ni, ni.getParent());
            highlighter.addElectricObject(ai, ai.getParent());
            highlighter.finished();
        }
	}//GEN-LAST:event_headSeeActionPerformed

	private void applyActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_applyActionPerformed
	{//GEN-HEADEREND:event_applyActionPerformed
		if (shownArc == null) return;
		ChangeArc job = new ChangeArc(shownArc, this);
	}//GEN-LAST:event_applyActionPerformed

	private void okActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_okActionPerformed
	{//GEN-HEADEREND:event_okActionPerformed
		applyActionPerformed(evt);
		closeDialog(null);
	}//GEN-LAST:event_okActionPerformed

	private void cancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancelActionPerformed
	{//GEN-HEADEREND:event_cancelActionPerformed
		closeDialog(null);
	}//GEN-LAST:event_cancelActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		//theDialog = null;
        //Highlight.removeHighlightListener(this);
		//dispose();
	}//GEN-LAST:event_closeDialog

	private static class ChangeArc extends Job
	{
		ArcInst ai;
		GetInfoArc dialog;

		protected ChangeArc(ArcInst ai, GetInfoArc dialog)
		{
			super("Modify Arc", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.ai = ai;
			this.dialog = dialog;
			startJob();
		}

		public boolean doIt()
		{
			boolean changed = false;

			String currentName = dialog.name.getText().trim();
			if (!currentName.equals(dialog.initialName))
			{
				dialog.initialName = new String(currentName);
				if (currentName.length() == 0) currentName = null;
				ai.setName(currentName);
				changed = true;
			}
			boolean currentEasyToSelect = dialog.easyToSelect.isSelected();
			if (currentEasyToSelect != dialog.initialEasyToSelect)
			{
                ai.setHardSelect(!currentEasyToSelect);
				dialog.initialEasyToSelect = currentEasyToSelect;
			}

			boolean currentRigid = dialog.rigid.isSelected();
			if (currentRigid != dialog.initialRigid)
			{
				ai.setRigid(currentRigid);
				dialog.initialRigid = currentRigid;
				changed = true;
			}
			boolean currentFixedAngle = dialog.fixedAngle.isSelected();
			if (currentFixedAngle != dialog.initialFixedAngle)
			{
                ai.setFixedAngle(currentFixedAngle);
				dialog.initialFixedAngle = currentFixedAngle;
				changed = true;
			}
			boolean currentSlidable = dialog.slidable.isSelected();
			if (currentSlidable != dialog.initialSlidable)
			{
                ai.setSlidable(currentSlidable);
				dialog.initialSlidable = currentSlidable;
				changed = true;
			}

			boolean currentDirectional = dialog.directional.isSelected();
			if (currentDirectional != dialog.initialDirectional)
			{
                ai.setDirectional(currentDirectional);
				dialog.initialDirectional = currentDirectional;
				changed = true;
			}
			boolean currentEndsExtend = dialog.endsExtend.isSelected();
			if (currentEndsExtend != dialog.initialEndsExtend)
			{
                ai.setExtended(currentEndsExtend);
				dialog.initialEndsExtend = currentEndsExtend;
				changed = true;
			}

			boolean currentSkipHead = dialog.skipHead.isSelected();
			if (currentSkipHead != dialog.initialSkipHead)
			{
                ai.setSkipHead(currentSkipHead);
				dialog.initialSkipHead = currentSkipHead;
				changed = true;
			}
			boolean currentSkipTail = dialog.skipTail.isSelected();
			if (currentSkipTail != dialog.initialSkipTail)
			{
                ai.setSkipTail(currentSkipTail);
				dialog.initialSkipTail = currentSkipTail;
				changed = true;
			}
			boolean currentReverseEnds = dialog.reverseEnds.isSelected();
			if (currentReverseEnds != dialog.initialReverseEnds)
			{
                ai.setReverseEnds(currentReverseEnds);
				dialog.initialReverseEnds = currentReverseEnds;
				changed = true;
			}

            String currentColor = (String)dialog.arcColorComboBox.getSelectedItem();
            if (!currentColor.equals(dialog.initialColor))
            {
                int colorIndex = EGraphics.findColorIndex(currentColor);
                ai.updateVar(Artwork.ART_COLOR, new Integer(colorIndex));
                changed = true;
            }

			double currentWidth = TextUtils.atof(dialog.width.getText()) + ai.getProto().getWidthOffset();
			if (!DBMath.doublesEqual(currentWidth, dialog.initialWidth) || changed)
			{
				ai.modify(currentWidth - dialog.initialWidth, 0, 0, 0, 0);
				dialog.initialWidth = currentWidth;
			}
			return true;
		}
	}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel angle;
    private javax.swing.JButton apply;
    private javax.swing.JComboBox arcColorComboBox;
    private javax.swing.JButton attributes;
    private javax.swing.JLabel busSize;
    private javax.swing.JButton cancel;
    private javax.swing.JCheckBox directional;
    private javax.swing.JCheckBox easyToSelect;
    private javax.swing.JCheckBox endsExtend;
    private javax.swing.JCheckBox fixedAngle;
    private javax.swing.JLabel headLoc;
    private javax.swing.JLabel headNode;
    private javax.swing.JButton headSee;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JTextField name;
    private javax.swing.JLabel network;
    private javax.swing.JButton ok;
    private javax.swing.JCheckBox reverseEnds;
    private javax.swing.JCheckBox rigid;
    private javax.swing.JCheckBox skipHead;
    private javax.swing.JCheckBox skipTail;
    private javax.swing.JCheckBox slidable;
    private javax.swing.JLabel tailLoc;
    private javax.swing.JLabel tailNode;
    private javax.swing.JButton tailSee;
    private javax.swing.JLabel type;
    private javax.swing.JTextField width;
    // End of variables declaration//GEN-END:variables

}
