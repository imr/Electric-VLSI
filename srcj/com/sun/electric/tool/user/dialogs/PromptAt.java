/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PromptAt.java
 * User tool: Technology Editor, creation
 * Written by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.dialogs.EDialog;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JFrame;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.prefs.Preferences;

/**
 * This class places an inquiry dialog at specific places on the display.
 */
public class PromptAt extends EDialog
{
	private static final int INPUT = 1;
	private static final int YESNO = 2;
	private static final int SELECTION = 3;

	private int type;
	private String value;
    private boolean goodClicked;
    private JTextField dX;
    private JComboBox combo;
    private boolean closed;

    /**
     * Method to invoke a "yes/no" dialog centered at a point in the circuit.
     * @param wnd the window displaying the circuit.
     * @param ni the NodeInst about which to display the dialog.
     * @param title the dialog title.
     * @param label the message inside of the dialog, before the text area.
     * @param choices an array of strings to present as choices.
     * @return the returned choice (null if cancelled).
     */
    public static String showPromptAt(EditWindow wnd, NodeInst ni, String title, String label, String initial, String [] choices)
	{
    	PromptAt dialog = new PromptAt(SELECTION);
    	dialog.initComponents(wnd, ni, title, label, initial, false, choices);
    	dialog.goodClicked = false;
		dialog.setVisible(true);
		return dialog.value;
	}

    /**
     * Method to invoke a "yes/no" dialog centered at a point in the circuit.
     * @param wnd the window displaying the circuit.
     * @param ni the NodeInst about which to display the dialog.
     * @param title the dialog title.
     * @param label the message inside of the dialog, before the text area.
     * @param initial the default button (true for yes, false for no).
     * @return the returned value.
     */
    public static boolean showPromptAt(EditWindow wnd, NodeInst ni, String title, String label, boolean initial)
	{
    	PromptAt dialog = new PromptAt(YESNO);
    	dialog.initComponents(wnd, ni, title, label, null, initial, null);
    	dialog.goodClicked = false;
		dialog.setVisible(true);
    	if (dialog.closed) return initial;
		return dialog.goodClicked;
	}

    /**
     * Method to invoke an input dialog centered at a point in the circuit.
     * @param wnd the window displaying the circuit.
     * @param ni the NodeInst about which to display the dialog.
     * @param title the dialog title.
     * @param label the message inside of the dialog, before the text area.
     * @param initial the initial value of the text area.
     * @return the returned value (null if cancelled).
     */
    public static String showPromptAt(EditWindow wnd, NodeInst ni, String title, String label, String initial)
	{
    	PromptAt dialog = new PromptAt(INPUT);
    	dialog.initComponents(wnd, ni, title, label, initial, false, null);
		dialog.setVisible(true);
		return dialog.value;
	}

	/** Creates new form Move By */
	public PromptAt(int type)
	{
		super(null, true);
		this.type = type;
		this.closed = false;
	}
	
	private void ok() { exit(true); }
	
	protected void escapePressed() { closed = true;   exit(false); }
	 
    // Call this method when the user clicks the OK button
    private void exit(boolean goodButton)
    {
    	goodClicked = goodButton;
    	if (goodClicked)
    	{
    		if (type == INPUT) value = dX.getText();
    		if (type == SELECTION) value = (String)combo.getSelectedItem();
    	}
        dispose();
    }

    private void initComponents(EditWindow wnd, NodeInst ni, String title, String label, String initialInput,
    	boolean initialYesNo, String [] choices)
    {
        getContentPane().setLayout(new GridBagLayout());

        JComponent centerIt = null;
        setTitle(title);
        setName("");
        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent evt) { closed = true;   exit(false); }
        });

        String badButton = (type == YESNO ? "No" : "Cancel");
        JButton cancel = new JButton(badButton);
        cancel.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt) { exit(false); }
        });
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.weightx = 0.5;
        getContentPane().add(cancel, gridBagConstraints);

        String goodButton = (type == YESNO ? "Yes" : "OK");
        JButton ok = new JButton(goodButton);
        ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt) { exit(true); }
        });
        getRootPane().setDefaultButton(ok);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.weightx = 0.5;
        getContentPane().add(ok, gridBagConstraints);

    	JLabel jLabel1 = new JLabel(label);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(jLabel1, gridBagConstraints);
        if (type == YESNO) centerIt = jLabel1;
        if (type == INPUT)
        {
	    	dX = new JTextField();
	        dX.setColumns(8);
	        dX.setText(initialInput);
	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 1;
	        gridBagConstraints.gridy = 0;
	        gridBagConstraints.gridwidth = 2;
	        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
	        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
	        getContentPane().add(dX, gridBagConstraints);
	        dX.selectAll();
	        centerIt = dX;
        }
        if (type == SELECTION)
        {
        	combo = new JComboBox();
        	for(int i=0; i<choices.length; i++)
        		combo.addItem(choices[i]);
        	combo.setSelectedItem(initialInput);
	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 1;
	        gridBagConstraints.gridy = 0;
	        gridBagConstraints.gridwidth = 2;
	        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
	        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
	        getContentPane().add(combo, gridBagConstraints);
	        centerIt = combo;
        }

        pack();

        Point ew = wnd.getLocationOnScreen();
    	Point locInWnd = wnd.databaseToScreen(ni.getAnchorCenterX(), ni.getAnchorCenterY());
    	Point textfield = centerIt.getLocation();
    	Dimension textSize = centerIt.getSize();
    	this.setLocation(locInWnd.x+ew.x-(textfield.x+textSize.width/2), locInWnd.y+ew.y-(textfield.y+textSize.height/2+20));
    }
}
