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
	private static final int INPUT     = 1;
	private static final int YESNO     = 2;
	private static final int SELECTION = 3;
	private static final int CUSTOM    = 4;

	private int type;
	private String value;
	private boolean goodClicked;
	private JTextField dX;
	private JComboBox combo;
	private boolean closed;
	private Field [] fields;

	public static class Field
	{
		private String label;
		private Object initial;
		private Object finalValue;
		private int type;
		private JTextField dX;
		private JComboBox combo;

		private static final int FIELD_BOOL   = 1;
		private static final int FIELD_INT    = 2;
		private static final int FIELD_DOUBLE = 3;
		private static final int FIELD_SELECT = 4;

		public Field(String label, boolean initial)
		{
			this.label = label;
			this.initial = new Boolean(initial);
			this.finalValue = this.initial;
			this.type = FIELD_BOOL;
		}

		public Field(String label, int initial)
		{
			this.label = label;
			this.initial = new Integer(initial);
			this.finalValue = this.initial;
			this.type = FIELD_INT;
		}

		public Field(String label, double initial)
		{
			this.label = label;
			this.initial = new Double(initial);
			this.finalValue = this.initial;
			this.type = FIELD_DOUBLE;
		}

		public Field(String label, String [] choices, String initial)
		{
			this.label = label;
			this.initial = choices;
			this.finalValue = initial;
			this.type = FIELD_SELECT;
		}

		public Object getFinal() { return finalValue; }
	}

	/**
	 * Method to invoke a popup dialog centered at a point in the circuit.
	 * @param wnd the window displaying the circuit.
	 * @param ni the NodeInst about which to display the dialog.
	 * @param title the dialog title.
	 * @param label the message inside of the dialog, before the choices.
	 * @param initial the default choice.
	 * @param choices an array of strings to present as choices.
	 * @return the returned choice (null if cancelled).
	 */
	public static String showPromptAt(EditWindow wnd, NodeInst ni, String title, String label, String initial, String [] choices)
	{
		PromptAt dialog = new PromptAt(SELECTION);
		dialog.initComponents(wnd, ni, title, label, initial, false, choices, null);
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
		dialog.initComponents(wnd, ni, title, label, null, initial, null, null);
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
		dialog.initComponents(wnd, ni, title, label, initial, false, null, null);
		dialog.setVisible(true);
		return dialog.value;
	}

	/**
	 * Method to invoke a custom dialog centered at a point in the circuit.
	 * @param wnd the window displaying the circuit.
	 * @param ni the NodeInst about which to display the dialog.
	 * @param title the dialog title.
	 * @param fields an array of Field objects that describe each field in the dialog.
	 * @return null if cancelled, non-null if OK (the results are stored in the Field objects).
	 */
	public static String showPromptAt(EditWindow wnd, NodeInst ni, String title, Field [] fields)
	{
		PromptAt dialog = new PromptAt(CUSTOM);
		dialog.initComponents(wnd, ni, title, null, null, false, null, fields);
		dialog.goodClicked = false;
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
			if (type == CUSTOM)
			{
				for(int i=0; i<fields.length; i++)
				{
					switch (fields[i].type)
					{
						case Field.FIELD_INT:
							fields[i].finalValue = new Integer(TextUtils.atoi(fields[i].dX.getText()));
							break;
						case Field.FIELD_DOUBLE:
							fields[i].finalValue = new Double(TextUtils.atof(fields[i].dX.getText()));
							break;
						case Field.FIELD_SELECT:
							fields[i].finalValue = fields[i].combo.getSelectedItem();
							break;
					}
				}
				value = "";
			}
		}
		dispose();
	}

	private void initComponents(EditWindow wnd, NodeInst ni, String title, String label, String initialInput,
		boolean initialYesNo, String [] choices, Field [] fields)
	{
		getContentPane().setLayout(new GridBagLayout());

		JComponent centerIt = null;
		setTitle(title);
		setName("");
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent evt) { closed = true;   exit(false); }
		});

		int buttonRow = 1;
		if (type == CUSTOM)
		{
			this.fields = fields;
			for(int i=0; i<fields.length; i++)
			{
				JLabel jLabel1 = new JLabel(fields[i].label);
				GridBagConstraints gbc = new java.awt.GridBagConstraints();
				gbc.gridx = 0;
				gbc.gridy = i;
				gbc.insets = new java.awt.Insets(4, 4, 4, 4);
				gbc.anchor = java.awt.GridBagConstraints.WEST;
				getContentPane().add(jLabel1, gbc);
				if (fields[i].type == Field.FIELD_INT || fields[i].type == Field.FIELD_DOUBLE)
				{
					fields[i].dX = new JTextField();
					fields[i].dX.setColumns(8);
					fields[i].dX.setText(fields[i].initial.toString());
					gbc = new java.awt.GridBagConstraints();
					gbc.gridx = 1;
					gbc.gridy = i;
					gbc.weightx = 1.0;
					gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
					gbc.insets = new java.awt.Insets(4, 4, 4, 4);
					getContentPane().add(fields[i].dX, gbc);
					if (centerIt == null)
					{
						fields[i].dX.selectAll();
						centerIt = fields[i].dX;
					}
				} else if (fields[i].type == Field.FIELD_SELECT)
				{
					fields[i].combo = new JComboBox();
					String [] poss = (String [])fields[i].initial;
					for(int j=0; j<poss.length; j++)
						fields[i].combo.addItem(poss[j]);
					fields[i].combo.setSelectedItem(fields[i].finalValue);
					gbc = new java.awt.GridBagConstraints();
					gbc.gridx = 1;
					gbc.gridy = i;
					gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
					gbc.insets = new java.awt.Insets(4, 4, 4, 4);
					getContentPane().add(fields[i].combo, gbc);
					if (centerIt == null) centerIt = fields[i].combo;
				}
			}
			buttonRow = fields.length + 1;
		} else
		{
			if (label != null)
			{
				JLabel jLabel1 = new JLabel(label);
				GridBagConstraints gbc = new java.awt.GridBagConstraints();
				gbc.gridx = 0;
				gbc.gridy = 0;
				gbc.insets = new java.awt.Insets(4, 4, 4, 4);
				gbc.anchor = java.awt.GridBagConstraints.WEST;
				getContentPane().add(jLabel1, gbc);
				if (type == YESNO) centerIt = jLabel1;
			}
			if (type == INPUT)
			{
				dX = new JTextField();
//				dX.setColumns(8);
				dX.setText(initialInput);
				GridBagConstraints gbc = new java.awt.GridBagConstraints();
				gbc.gridx = 1;
				gbc.gridy = 0;
				gbc.weightx = 1.0;
				gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
				gbc.insets = new java.awt.Insets(4, 4, 4, 4);
				getContentPane().add(dX, gbc);
				dX.selectAll();
				centerIt = dX;
			}
			if (type == SELECTION)
			{
				combo = new JComboBox();
				for(int i=0; i<choices.length; i++)
					combo.addItem(choices[i]);
				combo.setSelectedItem(initialInput);
				GridBagConstraints gbc = new java.awt.GridBagConstraints();
				gbc.gridx = 1;
				gbc.gridy = 0;
				gbc.weightx = 1.0;
				gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
				gbc.insets = new java.awt.Insets(4, 4, 4, 4);
				getContentPane().add(combo, gbc);
				centerIt = combo;
			}
		}

		String badButton = (type == YESNO ? "No" : "Cancel");
		JButton cancel = new JButton(badButton);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = buttonRow;
		gbc.insets = new java.awt.Insets(4, 4, 4, 4);
		gbc.weightx = 0.5;
		getContentPane().add(cancel, gbc);
		cancel.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { exit(false); }
		});

		String goodButton = (type == YESNO ? "Yes" : "OK");
		JButton ok = new JButton(goodButton);
		getRootPane().setDefaultButton(ok);
		gbc = new java.awt.GridBagConstraints();
		gbc.gridx = 2;
		gbc.gridy = buttonRow;
		gbc.insets = new java.awt.Insets(4, 4, 4, 4);
		gbc.weightx = 0.5;
		getContentPane().add(ok, gbc);
		ok.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt) { exit(true); }
		});

		pack();

		Point ew = wnd.getLocationOnScreen();
		Point locInWnd = wnd.databaseToScreen(ni.getAnchorCenterX(), ni.getAnchorCenterY());
		Point textfield = centerIt.getLocation();
		Dimension textSize = centerIt.getSize();
		this.setLocation(locInWnd.x+ew.x-(textfield.x+textSize.width/2), locInWnd.y+ew.y-(textfield.y+textSize.height/2+20));
	}
}
