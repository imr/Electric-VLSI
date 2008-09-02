/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GridAndAlignmentTab.java
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
package com.sun.electric.tool.user.dialogs.options;

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.ToolBar;

import javax.swing.AbstractButton;
import javax.swing.JPanel;

/**
 * Class to handle the "Grid And Alignment" tab of the Preferences dialog.
 */
public class GridAndAlignmentTab extends PreferencePanel
{
	/** Creates new form Edit Options */
	public GridAndAlignmentTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return grid; }

	/** return the name of this preferences tab. */
	public String getName() { return "Grid"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Grid tab.
	 */
	public void init()
	{
		EditWindow wnd = EditWindow.getCurrent();
		if (wnd == null)
		{
			gridCurrentHoriz.setEditable(false);
			gridCurrentHoriz.setText("");
			gridCurrentVert.setEditable(false);
			gridCurrentVert.setText("");
		} else
		{
			gridCurrentHoriz.setEditable(true);
			gridCurrentHoriz.setText(TextUtils.formatDouble(wnd.getGridXSpacing()));
			gridCurrentVert.setEditable(true);
			gridCurrentVert.setText(TextUtils.formatDouble(wnd.getGridYSpacing()));
		}

		gridNewHoriz.setText(TextUtils.formatDouble(User.getDefGridXSpacing()));
		gridNewVert.setText(TextUtils.formatDouble(User.getDefGridYSpacing()));
		gridBoldHoriz.setText(TextUtils.formatDouble(User.getDefGridXBoldFrequency()));
		gridBoldVert.setText(TextUtils.formatDouble(User.getDefGridYBoldFrequency()));
		gridShowAxes.setSelected(User.isGridAxesShown());

        double[] values = User.getAlignmentToGridVector();
        gridSize1.setText(TextUtils.formatDouble(Math.abs(values[0])));
        gridSize2.setText(TextUtils.formatDouble(Math.abs(values[1])));
        gridSize3.setText(TextUtils.formatDouble(Math.abs(values[2])));
        gridSize4.setText(TextUtils.formatDouble(Math.abs(values[3])));
        gridSize5.setText(TextUtils.formatDouble(Math.abs(values[4])));
        AbstractButton[] list = {size1Button, size2Button, size3Button, size4Button, size5Button};
        for (int i = 0; i < values.length; i++)
        {
            if (values[i] < 0) // found the marked one
            {
                list[i].setSelected(true);
                break;
            }
        }
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Grid tab.
	 */
	public void term()
	{
		EditWindow wnd = EditWindow.getCurrent();
        double currDouble;

		boolean redraw = false;
		if (wnd != null)
		{
			currDouble = TextUtils.atof(gridCurrentHoriz.getText());
			if (currDouble != wnd.getGridXSpacing())
			{
				wnd.setGridXSpacing(currDouble);
				redraw = true;
			}

			currDouble = TextUtils.atof(gridCurrentVert.getText());
			if (currDouble != wnd.getGridYSpacing())
			{
				wnd.setGridYSpacing(currDouble);
				redraw = true;
			}
		}

		currDouble = TextUtils.atof(gridNewHoriz.getText());
		if (currDouble != User.getDefGridXSpacing())
			User.setDefGridXSpacing(currDouble);

		currDouble = TextUtils.atof(gridNewVert.getText());
		if (currDouble != User.getDefGridYSpacing())
			User.setDefGridYSpacing(currDouble);

		int currInt = TextUtils.atoi(gridBoldHoriz.getText());
		if (currInt != User.getDefGridXBoldFrequency())
			User.setDefGridXBoldFrequency(currInt);

		currInt = TextUtils.atoi(gridBoldVert.getText());
		if (currInt != User.getDefGridYBoldFrequency())
			User.setDefGridYBoldFrequency(currInt);

		boolean curBoolean = gridShowAxes.isSelected();
		if (curBoolean != User.isGridAxesShown())
		{
			User.setGridAxesShown(curBoolean);
			redraw = true;
		}

        double[] oldValues = User.getAlignmentToGridVector();
        double[] newValues = {TextUtils.atof(gridSize1.getText()),
                              TextUtils.atof(gridSize2.getText()),
                              TextUtils.atof(gridSize3.getText()),
                              TextUtils.atof(gridSize4.getText()),
                              TextUtils.atof(gridSize5.getText())};
        int pos = -1;
        if (size1Button.isSelected()) pos = 0; else
        if (size2Button.isSelected()) pos = 1; else
        if (size3Button.isSelected()) pos = 2; else
        if (size4Button.isSelected()) pos = 3; else
            pos = 4;
        if (newValues[pos] > 0) newValues[pos] *= -1;

        if (oldValues[0] != newValues[0] || oldValues[1] != newValues[1] ||
        	oldValues[2] != newValues[2] || oldValues[3] != newValues[3] ||
        	oldValues[4] != newValues[4])
        {
            User.setAlignmentToGridVector(newValues);
            ToolBar.setGridAligment();
        }

		if (redraw && wnd != null)
			wnd.repaint();
	}

	/**
	 * Method called when the factory reset is requested.
	 */
	public void reset()
	{
		User.setDefGridXSpacing(User.getFactoryDefGridXSpacing());
		User.setDefGridYSpacing(User.getFactoryDefGridYSpacing());
		User.setDefGridXBoldFrequency(User.getFactoryDefGridXBoldFrequency());
		User.setDefGridYBoldFrequency(User.getFactoryDefGridYBoldFrequency());
		User.setGridAxesShown(User.isFactoryGridAxesShown());
		User.setAlignmentToGridVector(User.getFactoryAlignmentToGridVector());
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        alignGroup = new javax.swing.ButtonGroup();
        grid = new javax.swing.JPanel();
        gridPart = new javax.swing.JPanel();
        jLabel32 = new javax.swing.JLabel();
        jLabel33 = new javax.swing.JLabel();
        jLabel34 = new javax.swing.JLabel();
        gridCurrentHoriz = new javax.swing.JTextField();
        gridCurrentVert = new javax.swing.JTextField();
        jLabel35 = new javax.swing.JLabel();
        gridNewHoriz = new javax.swing.JTextField();
        gridNewVert = new javax.swing.JTextField();
        jLabel36 = new javax.swing.JLabel();
        gridBoldHoriz = new javax.swing.JTextField();
        gridBoldVert = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        gridShowAxes = new javax.swing.JCheckBox();
        alignPart = new javax.swing.JPanel();
        gridSize5 = new javax.swing.JTextField();
        jLabel38 = new javax.swing.JLabel();
        gridSize3 = new javax.swing.JTextField();
        gridSize1 = new javax.swing.JTextField();
        size1Button = new javax.swing.JRadioButton();
        size2Button = new javax.swing.JRadioButton();
        size3Button = new javax.swing.JRadioButton();
        size4Button = new javax.swing.JRadioButton();
        size5Button = new javax.swing.JRadioButton();
        gridSize2 = new javax.swing.JTextField();
        gridSize4 = new javax.swing.JTextField();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Edit Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        grid.setLayout(new java.awt.GridBagLayout());

        gridPart.setLayout(new java.awt.GridBagLayout());

        gridPart.setBorder(javax.swing.BorderFactory.createTitledBorder("Grid Display"));
        jLabel32.setText("Horizontal:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridPart.add(jLabel32, gridBagConstraints);

        jLabel33.setText("Vertical:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridPart.add(jLabel33, gridBagConstraints);

        jLabel34.setText("Grid dot spacing:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 4, 0, 0);
        gridPart.add(jLabel34, gridBagConstraints);

        gridCurrentHoriz.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridPart.add(gridCurrentHoriz, gridBagConstraints);

        gridCurrentVert.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridPart.add(gridCurrentVert, gridBagConstraints);

        jLabel35.setText("Default grid spacing:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 4, 0, 0);
        gridPart.add(jLabel35, gridBagConstraints);

        gridNewHoriz.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridPart.add(gridNewHoriz, gridBagConstraints);

        gridNewVert.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridPart.add(gridNewVert, gridBagConstraints);

        jLabel36.setText("Frequency of bold dots:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 4, 8, 0);
        gridPart.add(jLabel36, gridBagConstraints);

        gridBoldHoriz.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridPart.add(gridBoldHoriz, gridBagConstraints);

        gridBoldVert.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridPart.add(gridBoldVert, gridBagConstraints);

        jLabel10.setText("(for current window)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 14, 8, 0);
        gridPart.add(jLabel10, gridBagConstraints);

        jLabel13.setText("(for new windows)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 14, 8, 0);
        gridPart.add(jLabel13, gridBagConstraints);

        gridShowAxes.setText("Show X and Y axes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridPart.add(gridShowAxes, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        grid.add(gridPart, gridBagConstraints);

        alignPart.setLayout(new java.awt.GridBagLayout());

        alignPart.setBorder(javax.swing.BorderFactory.createTitledBorder("Alignment of Cursor to Grid"));
        gridSize5.setColumns(8);
        gridSize5.setPreferredSize(new java.awt.Dimension(40, 22));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        alignPart.add(gridSize5, gridBagConstraints);

        jLabel38.setText("Values of zero will cause no alignment");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        alignPart.add(jLabel38, gridBagConstraints);

        gridSize3.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        alignPart.add(gridSize3, gridBagConstraints);

        gridSize1.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        alignPart.add(gridSize1, gridBagConstraints);

        alignGroup.add(size1Button);
        size1Button.setText("Size 1 (largest)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        alignPart.add(size1Button, gridBagConstraints);

        alignGroup.add(size2Button);
        size2Button.setText("Size 2");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        alignPart.add(size2Button, gridBagConstraints);

        alignGroup.add(size3Button);
        size3Button.setText("Size 3");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        alignPart.add(size3Button, gridBagConstraints);

        alignGroup.add(size4Button);
        size4Button.setText("Size 4");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        alignPart.add(size4Button, gridBagConstraints);

        alignGroup.add(size5Button);
        size5Button.setText("Size 5 (smallest)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        alignPart.add(size5Button, gridBagConstraints);

        gridSize2.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        alignPart.add(gridSize2, gridBagConstraints);

        gridSize4.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        alignPart.add(gridSize4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        grid.add(alignPart, gridBagConstraints);

        getContentPane().add(grid, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup alignGroup;
    private javax.swing.JPanel alignPart;
    private javax.swing.JPanel grid;
    private javax.swing.JTextField gridBoldHoriz;
    private javax.swing.JTextField gridBoldVert;
    private javax.swing.JTextField gridCurrentHoriz;
    private javax.swing.JTextField gridCurrentVert;
    private javax.swing.JTextField gridNewHoriz;
    private javax.swing.JTextField gridNewVert;
    private javax.swing.JPanel gridPart;
    private javax.swing.JCheckBox gridShowAxes;
    private javax.swing.JTextField gridSize1;
    private javax.swing.JTextField gridSize2;
    private javax.swing.JTextField gridSize3;
    private javax.swing.JTextField gridSize4;
    private javax.swing.JTextField gridSize5;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JRadioButton size1Button;
    private javax.swing.JRadioButton size2Button;
    private javax.swing.JRadioButton size3Button;
    private javax.swing.JRadioButton size4Button;
    private javax.swing.JRadioButton size5Button;
    // End of variables declaration//GEN-END:variables

}
