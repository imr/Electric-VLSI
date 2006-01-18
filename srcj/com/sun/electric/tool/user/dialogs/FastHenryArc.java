/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: FastHenryArc.java
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

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.io.output.FastHenry;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.Highlight2;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * Class to handle the "FastHenryArc" dialog.
 */
public class FastHenryArc extends EDialog
{
	private ArcInst ai;
	private List<String> groupsList;
	private String initialGroupName;
	private double initialThickness;
	private int initialWidthSubdivs, initialHeightSubdivs;
	private double initialZHead, initialZTail;

	public static void showFastHenryArcDialog()
	{
		JFrame jf = null;
		if (TopLevel.isMDIMode())
			jf = TopLevel.getCurrentJFrame();
		FastHenryArc theDialog = new FastHenryArc(jf, true);
		theDialog.setVisible(true);
	}

	/** Creates new form FastHenryArc */
	private FastHenryArc(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();

		// must have a single arc selected
		ai = null;
		int arcCount = 0;
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd != null)
        {
			for(Highlight2 h : wnd.getHighlighter().getHighlights())
			{
				if (h.isHighlightEOBJ())
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
        }
		if (ai == null)
		{
			disableDialog();
			return;
		}

		// enable dialog
		fhaIncludeArc.setEnabled(true);

		// gather list of all groups in the cell
		Set<String> groupNames = new HashSet<String>();
		for(Iterator<ArcInst> it = ai.getParent().getArcs(); it.hasNext(); )
		{
			ArcInst oAi = it.next();
			Variable var = oAi.getVar(FastHenry.GROUP_NAME_KEY);
			if (var == null) continue;
			groupNames.add(var.getPureValue(-1));
		}
		groupsList = new ArrayList<String>();
		for(String str : groupNames)
		{
			groupsList.add(str);
		}
		Collections.sort(groupsList, String.CASE_INSENSITIVE_ORDER);
		for(String str : groupsList)
		{
			fhaGroups.addItem(str);
		}

		// see if an arc is selected
		FastHenry.FastHenryArcInfo fhai = new FastHenry.FastHenryArcInfo(ai);
		initialGroupName = "";
		String groupName = fhai.getGroupName();
		if (groupName == null) fhaIncludeArc.setSelected(false); else
		{
			fhaIncludeArc.setSelected(true);
			fhaGroups.setSelectedItem(initialGroupName = groupName);
		}

		fhaWidth.setText(TextUtils.formatDouble(ai.getWidth() - ai.getProto().getWidthOffset()));

		String thickness = "";
		if (fhai.getThickness() >= 0) thickness = TextUtils.formatDouble(initialThickness = fhai.getThickness());
		fhaThickness.setText(thickness);
	    fhaDefaultThickness.setText("default=" + TextUtils.formatDouble(Simulation.getFastHenryDefThickness()));

		String widthSubdivisions = "";
		if (fhai.getWidthSubdivisions() >= 0) widthSubdivisions = Integer.toString(initialWidthSubdivs = fhai.getWidthSubdivisions());
		fhaWidthSubdivs.setText(widthSubdivisions);
	    fhaDefaultWidthSubdivs.setText("default=" + Integer.toString(Simulation.getFastHenryWidthSubdivisions()));

		String heightSubdivisions = "";
		if (fhai.getHeightSubdivisions() >= 0) heightSubdivisions = Integer.toString(initialHeightSubdivs = fhai.getHeightSubdivisions());
		fhaHeightSubdivs.setText(heightSubdivisions);
	    fhaDefaultHeightSubdivs.setText("default=" + Integer.toString(Simulation.getFastHenryHeightSubdivisions()));

		fhaHeadXY.setText("Head at:   X=" + TextUtils.formatDouble(ai.getHeadLocation().getX()) +
			"   Y=" + TextUtils.formatDouble(ai.getHeadLocation().getY()) + "   Z=");
		String headZ = "";
		if (fhai.getZHead() >= 0) headZ = TextUtils.formatDouble(initialZHead = fhai.getZHead());
		fhaHeadZ.setText(headZ);

		fhaTailXY.setText("Tail at:   X=" + TextUtils.formatDouble(ai.getTailLocation().getX()) +
			"   Y=" + TextUtils.formatDouble(ai.getTailLocation().getY()) + "   Z=");
		String tailZ = "";
		if (fhai.getZTail() >= 0) tailZ = TextUtils.formatDouble(initialZTail = fhai.getZTail());
		fhaTailZ.setText(tailZ);
	    
		String defaultZ = "";
		if (fhai.getZDefault() >= 0) defaultZ = TextUtils.formatDouble(fhai.getZDefault());
		fhaDefaultZ.setText("default=" + defaultZ);

		includeArcClicked();

		fhaNewGroup.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { makeNewGroup(); }
		});
		fhaIncludeArc.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { includeArcClicked(); }
		});
		pack();
	}

	private void makeNewGroup()
	{
		String groupName = (String)JOptionPane.showInputDialog(null, "Name of new FastHenry group:",
			"New Group Name", JOptionPane.QUESTION_MESSAGE, null, null, "NewGroup");
		if (groupName == null) return;
		if (groupsList.contains(groupName))
		{
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
				"That group name is already in the list");
			return;
		}
		groupsList.add(groupName);
		Collections.sort(groupsList, String.CASE_INSENSITIVE_ORDER);
		fhaGroups.removeAllItems();
		for(String str : groupsList)
			fhaGroups.addItem(str);
		fhaGroups.setSelectedItem(groupName);
	}

	private void includeArcClicked()
	{
		boolean enable = fhaIncludeArc.isSelected();
	    fhaGroupLabel.setEnabled(enable);
		fhaGroups.setEnabled(enable);
		fhaNewGroup.setEnabled(enable);

		fhaWidthLabel.setEnabled(enable);
		fhaWidth.setEnabled(enable);

		fhaThicknessLabel.setEnabled(enable);
		fhaThickness.setEnabled(enable);
		fhaThickness.setEditable(enable);
		fhaDefaultThickness.setEnabled(enable);

	    fhaWidthSubdivsLabel.setEnabled(enable);
		fhaWidthSubdivs.setEnabled(enable);
		fhaWidthSubdivs.setEditable(enable);
		fhaDefaultWidthSubdivs.setEnabled(enable);

	    fhaHeightSubdivsLabel.setEnabled(enable);
		fhaHeightSubdivs.setEnabled(enable);
		fhaHeightSubdivs.setEditable(enable);
		fhaDefaultHeightSubdivs.setEnabled(enable);

		fhaHeadXY.setEnabled(enable);
		fhaHeadZ.setEnabled(enable);
		fhaHeadZ.setEditable(enable);
		fhaTailXY.setEnabled(enable);
		fhaTailZ.setEnabled(enable);
		fhaTailZ.setEditable(enable);
		fhaDefaultZ.setEnabled(enable);
	}

	private void disableDialog()
	{
		fhaIncludeArc.setSelected(false);
		fhaIncludeArc.setEnabled(false);
	    fhaGroupLabel.setEnabled(false);
		fhaGroups.setEnabled(false);
		fhaNewGroup.setEnabled(false);
		fhaWidthLabel.setEnabled(false);
		fhaWidth.setText("");
		fhaThicknessLabel.setEnabled(false);
		fhaThickness.setText("");
		fhaDefaultThickness.setText("");
	    fhaWidthSubdivsLabel.setEnabled(false);
		fhaWidthSubdivs.setText("");
		fhaDefaultWidthSubdivs.setText("");
	    fhaHeightSubdivsLabel.setEnabled(false);
		fhaHeightSubdivs.setText("");
		fhaDefaultHeightSubdivs.setText("");
		fhaHeadXY.setText("Head at:   X=   Y=   Z=");
		fhaHeadXY.setEnabled(false);
		fhaHeadZ.setText("");
		fhaTailXY.setText("Tail at:   X=   Y=   Z=");
		fhaTailXY.setEnabled(false);
		fhaTailZ.setText("");
		fhaDefaultZ.setText("default=");
		fhaDefaultZ.setEnabled(false);
	}

    /**
     * Class to delete an attribute in a new thread.
     */
    private static class UpdateFastHenryArc extends Job
	{
    	private ArcInst ai;
    	private boolean includeArc;
    	private String groupName, initialGroupName;
    	private double thickness, initialThickness;
    	private int widthSubdivs, initialWidthSubdivs;
    	private int heightSubdivs, initialHeightSubdivs;
    	private double headZ, initialHeadZ;
    	private double tailZ, initialTailZ;

        private UpdateFastHenryArc(
        	ArcInst ai,
        	boolean includeArc,
        	String groupName, String initialGroupName,
        	double thickness, double initialThickness,
        	int widthSubdivs, int initialWidthSubdivs,
        	int heightSubdivs, int initialHeightSubdivs,
        	double headZ, double initialHeadZ,
        	double tailZ, double initialTailZ)
        {
            super("Update FastHenry Arc", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.ai = ai;
            this.includeArc = includeArc;
            this.groupName = groupName;
            this.initialGroupName = initialGroupName;
            this.thickness = thickness;
            this.initialThickness = initialThickness;
            this.widthSubdivs = widthSubdivs;
            this.initialWidthSubdivs = initialWidthSubdivs;
            this.heightSubdivs = heightSubdivs;
            this.initialHeightSubdivs = initialHeightSubdivs;
            this.headZ = headZ;
            this.initialHeadZ = initialHeadZ;
            this.tailZ = tailZ;
            this.initialTailZ = initialTailZ;
            startJob();
        }

        public boolean doIt() throws JobException
        {
    		if (includeArc)
    		{
    			// add to fasthenry analysis
    			if (!groupName.equals(initialGroupName))
    			{
    				ai.newDisplayVar(FastHenry.GROUP_NAME_KEY, groupName);
       			}

    			// update variables
    			if (thickness != initialThickness)
    				ai.newVar(FastHenry.THICKNESS_KEY, new Double(thickness));

    			if (widthSubdivs != initialWidthSubdivs)
    				ai.newVar(FastHenry.WIDTH_SUBDIVS_KEY, new Integer(widthSubdivs));

    			if (heightSubdivs != initialHeightSubdivs)
    				ai.newVar(FastHenry.HEIGHT_SUBDIVS_KEY, new Integer(heightSubdivs));

    			if (headZ != initialHeadZ)
    				ai.newVar(FastHenry.ZHEAD_KEY, new Double(headZ));

    			if (tailZ != initialTailZ)
    				ai.newVar(FastHenry.ZTAIL_KEY, new Double(tailZ));
    		} else
    		{
    			if (ai.getVar(FastHenry.GROUP_NAME_KEY) != null)
    				ai.delVar(FastHenry.GROUP_NAME_KEY);
    		}
            return true;
        }
    }

	private void okPressed()
	{
		if (ai != null)
		{
			double thickness = -1;
			if (fhaThickness.getText().length() > 0)
				thickness = TextUtils.atof(fhaThickness.getText());
			int widthSubdivs = -1;
			if (fhaWidthSubdivs.getText().length() > 0)
				widthSubdivs = TextUtils.atoi(fhaWidthSubdivs.getText());
			int heightSubdivs = -1;
			if (fhaHeightSubdivs.getText().length() > 0)
				heightSubdivs = TextUtils.atoi(fhaHeightSubdivs.getText());
			double headZ = -1;
			if (fhaHeadZ.getText().length() > 0)
				headZ = TextUtils.atof(fhaHeadZ.getText());
			double tailZ = -1;
			if (fhaTailZ.getText().length() > 0)
				tailZ = TextUtils.atof(fhaTailZ.getText());
			new UpdateFastHenryArc(ai,
				fhaIncludeArc.isSelected(),
				(String)fhaGroups.getSelectedItem(), initialGroupName,
		    	thickness, initialThickness,
		    	widthSubdivs, initialWidthSubdivs,
		    	heightSubdivs, initialHeightSubdivs,
		    	headZ, initialZHead,
		    	tailZ, initialZTail);
		}
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

        ok = new javax.swing.JButton();
        cancel = new javax.swing.JButton();
        fhaIncludeArc = new javax.swing.JCheckBox();
        fhaGroupLabel = new javax.swing.JLabel();
        fhaGroups = new javax.swing.JComboBox();
        fhaThicknessLabel = new javax.swing.JLabel();
        fhaThickness = new javax.swing.JTextField();
        fhaWidthLabel = new javax.swing.JLabel();
        fhaWidth = new javax.swing.JLabel();
        fhaWidthSubdivsLabel = new javax.swing.JLabel();
        fhaWidthSubdivs = new javax.swing.JTextField();
        fhaHeightSubdivsLabel = new javax.swing.JLabel();
        fhaHeightSubdivs = new javax.swing.JTextField();
        jSeparator1 = new javax.swing.JSeparator();
        fhaNewGroup = new javax.swing.JButton();
        fhaDefaultZ = new javax.swing.JLabel();
        head = new javax.swing.JPanel();
        fhaHeadXY = new javax.swing.JLabel();
        fhaHeadZ = new javax.swing.JTextField();
        tail = new javax.swing.JPanel();
        fhaTailXY = new javax.swing.JLabel();
        fhaTailZ = new javax.swing.JTextField();
        fhaDefaultThickness = new javax.swing.JLabel();
        fhaDefaultWidthSubdivs = new javax.swing.JLabel();
        fhaDefaultHeightSubdivs = new javax.swing.JLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("FastHenry Arc Properties");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

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
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);

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
        gridBagConstraints.gridy = 10;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(cancel, gridBagConstraints);

        fhaIncludeArc.setText("Include this arc in FastHenry analysis");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(fhaIncludeArc, gridBagConstraints);

        fhaGroupLabel.setText("Group name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(fhaGroupLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(fhaGroups, gridBagConstraints);

        fhaThicknessLabel.setText("Thickness:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(fhaThicknessLabel, gridBagConstraints);

        fhaThickness.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(fhaThickness, gridBagConstraints);

        fhaWidthLabel.setText("Width:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(fhaWidthLabel, gridBagConstraints);

        fhaWidth.setText("3");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(fhaWidth, gridBagConstraints);

        fhaWidthSubdivsLabel.setText("Width subdivisions:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(fhaWidthSubdivsLabel, gridBagConstraints);

        fhaWidthSubdivs.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(fhaWidthSubdivs, gridBagConstraints);

        fhaHeightSubdivsLabel.setText("Height subdivisions:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(fhaHeightSubdivsLabel, gridBagConstraints);

        fhaHeightSubdivs.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(fhaHeightSubdivs, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        getContentPane().add(jSeparator1, gridBagConstraints);

        fhaNewGroup.setText("New Group");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(fhaNewGroup, gridBagConstraints);

        fhaDefaultZ.setText("default=17");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(fhaDefaultZ, gridBagConstraints);

        head.setLayout(new java.awt.GridBagLayout());

        fhaHeadXY.setText("Head at:   X=-17   Y=7   Z=");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        head.add(fhaHeadXY, gridBagConstraints);

        fhaHeadZ.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.weightx = 1.0;
        head.add(fhaHeadZ, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.2;
        getContentPane().add(head, gridBagConstraints);

        tail.setLayout(new java.awt.GridBagLayout());

        fhaTailXY.setText("Tail at:   X=20   Y=7   Z=");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        tail.add(fhaTailXY, gridBagConstraints);

        fhaTailZ.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.weightx = 1.0;
        tail.add(fhaTailZ, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.2;
        getContentPane().add(tail, gridBagConstraints);

        fhaDefaultThickness.setText("default=2");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(fhaDefaultThickness, gridBagConstraints);

        fhaDefaultWidthSubdivs.setText("default=1");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(fhaDefaultWidthSubdivs, gridBagConstraints);

        fhaDefaultHeightSubdivs.setText("default=1");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(fhaDefaultHeightSubdivs, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void ok(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ok
	{//GEN-HEADEREND:event_ok
		okPressed();
		closeDialog(null);
	}//GEN-LAST:event_ok

	private void cancel(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancel
	{//GEN-HEADEREND:event_cancel
		closeDialog(null);
	}//GEN-LAST:event_cancel

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();

	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancel;
    private javax.swing.JLabel fhaDefaultHeightSubdivs;
    private javax.swing.JLabel fhaDefaultThickness;
    private javax.swing.JLabel fhaDefaultWidthSubdivs;
    private javax.swing.JLabel fhaDefaultZ;
    private javax.swing.JLabel fhaGroupLabel;
    private javax.swing.JComboBox fhaGroups;
    private javax.swing.JLabel fhaHeadXY;
    private javax.swing.JTextField fhaHeadZ;
    private javax.swing.JTextField fhaHeightSubdivs;
    private javax.swing.JLabel fhaHeightSubdivsLabel;
    private javax.swing.JCheckBox fhaIncludeArc;
    private javax.swing.JButton fhaNewGroup;
    private javax.swing.JLabel fhaTailXY;
    private javax.swing.JTextField fhaTailZ;
    private javax.swing.JTextField fhaThickness;
    private javax.swing.JLabel fhaThicknessLabel;
    private javax.swing.JLabel fhaWidth;
    private javax.swing.JLabel fhaWidthLabel;
    private javax.swing.JTextField fhaWidthSubdivs;
    private javax.swing.JLabel fhaWidthSubdivsLabel;
    private javax.swing.JPanel head;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JButton ok;
    private javax.swing.JPanel tail;
    // End of variables declaration//GEN-END:variables
    
}
