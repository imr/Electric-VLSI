/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PromptAt.java
 * Display a prompt dialog over a specific piece of circuitry.
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

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.EditWindow_;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * This class places an inquiry dialog at specific places on the display.
 */
public class PromptAt extends EDialog
{
	private boolean yesNo;
	private String value;
	private boolean goodClicked;
	private String customButtonClicked;
	private boolean closed;
	private Field [] fieldList;
	private Field [][] fieldArray;

	/**
	 * Class to define a single entry in the custom prompt dialog.
	 */
	public static class Field
	{
		private String label;
		private Object initial;
		private Object finalValue;
		private int type;
		private JTextField textField;
		private JComboBox combo;
		private JButton but;
		private JPanel patch;
		private JLabel labelObj;

		private static final int FIELD_MESSAGE   = 1;
		private static final int FIELD_BOOL      = 2;
		private static final int FIELD_STRING    = 3;
		private static final int FIELD_SELECT    = 4;
		private static final int FIELD_COLOR     = 5;
		private static final int FIELD_BUTTON    = 6;

		/**
		 * Constructor for a field in a prompt dialog that displays a message.
		 * @param label the question to ask.
		 */
		public Field(String label)
		{
			this.label = label;
			this.initial = null;
			this.finalValue = null;
			this.type = FIELD_MESSAGE;
		}

		/**
		 * Constructor for a field in a prompt dialog that chooses between Yes and No.
		 * @param label the question to ask.
		 * @param initial the default response.
		 */
		public Field(String label, boolean initial)
		{
			this.label = label;
			this.initial = new Boolean(initial);
			this.finalValue = this.initial;
			this.type = FIELD_BOOL;
		}

		/**
		 * Constructor for a field in a prompt dialog that edits a string.
		 * @param label the label of the string.
		 * @param initial the initial string value.
		 */
		public Field(String label, String initial)
		{
			this.label = label;
			this.initial = initial;
			this.finalValue = this.initial;
			this.type = FIELD_STRING;
		}

		/**
		 * Constructor for a field in a prompt dialog that selects among different choices.
		 * @param label the label of the choice.
		 * @param choices the array of choices.
		 * @param initial the default choice.
		 */
		public Field(String label, String [] choices, String initial)
		{
			this.label = label;
			this.initial = choices;
			this.finalValue = initial;
			this.type = FIELD_SELECT;
		}

		/**
		 * Constructor for a field in a prompt dialog that edits a color value.
		 * @param label the label of the color.
		 * @param initial the initial Color value.
		 */
		public Field(String label, Color initial)
		{
			this.label = label;
			this.initial = initial;
			this.finalValue = initial;
			this.type = FIELD_COLOR;
		}

		/**
		 * Constructor for a field in a prompt dialog that places a button.
		 * @param id the returned value of the dialog if the button is pressed.
		 * @param but the button.
		 */
		public Field(String id, JButton but)
		{
			this.label = null;
			this.initial = but;
			this.finalValue = id;
			this.type = FIELD_BUTTON;
		}

		/**
		 * Method to return the final value for a field, after the dialog has completed.
		 * @return the final value (dependent on the type of field).
		 */
		public Object getFinal() { return finalValue; }
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
	public static boolean showPromptAt(EditWindow_ wnd, NodeInst ni, String title, String label, boolean initial)
	{
		Field [] fields = new Field[1];
		fields[0] = new PromptAt.Field(label);

		PromptAt dialog = new PromptAt(true);
		dialog.initComponents(wnd, ni, title, fields, null);
		dialog.setVisible(true);
		if (dialog.closed) return initial;
		return dialog.goodClicked;
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
	public static String showPromptAt(EditWindow_ wnd, NodeInst ni, String title, String label, String initial, String [] choices)
	{
		Field [] fields = new Field[1];
		fields[0] = new PromptAt.Field(label, choices, initial);

		PromptAt dialog = new PromptAt(false);
		dialog.initComponents(wnd, ni, title, fields, null);
		dialog.setVisible(true);
		return (String)fields[0].finalValue;
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
	public static String showPromptAt(EditWindow_ wnd, NodeInst ni, String title, String label, String initial)
	{
		Field [] fields = new Field[1];
		fields[0] = new PromptAt.Field(label, initial);
		
		PromptAt dialog = new PromptAt(false);
		dialog.initComponents(wnd, ni, title, fields, null);
		dialog.setVisible(true);
		return (String)fields[0].finalValue;
	}

	/**
	 * Method to invoke a custom dialog centered at a point in the circuit.
	 * @param wnd the window displaying the circuit.
	 * @param ni the NodeInst about which to display the dialog.
	 * @param title the dialog title.
	 * @param fields an array of Field objects that describe each field in the dialog.
	 * @return null if cancelled, non-null if OK (the results are stored in the Field objects).
	 */
	public static String showPromptAt(EditWindow_ wnd, NodeInst ni, String title, Field [] fields)
	{
		PromptAt dialog = new PromptAt(false);
		dialog.initComponents(wnd, ni, title, fields, null);
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
	public static String showPromptAt(EditWindow_ wnd, NodeInst ni, String title, Field [][] fields)
	{
		PromptAt dialog = new PromptAt(false);
		dialog.initComponents(wnd, ni, title, null, fields);
		dialog.setVisible(true);
		return dialog.value;
	}

	/** Creates new form PromptAt */
	public PromptAt(boolean yesNo)
	{
		super(null, true);
		this.yesNo = yesNo;
		this.closed = false;
	}

	private void ok() { exit(true); }

	protected void escapePressed() { closed = true;   exit(false); }

	/**
	 * Call this method when the user closes the dialog.
	 * @param goodButton true if it is an "OK" completion, false if a "Cancel" completion.
	 */
	private void exit(boolean goodButton)
	{
		goodClicked = goodButton;
		if (goodClicked)
		{
			if (!yesNo)
			{
				if (fieldList != null)
				{
					for(int i=0; i<fieldList.length; i++)
					{
						finishField(fieldList[i]);
					}
				} else if (fieldArray != null)
				{
					for(int i=0; i<fieldArray.length; i++)
					{
						Field [] row = fieldArray[i];
						for(int j=0; j<row.length; j++)
							finishField(row[j]);
					}
				}
				value = "";
				if (customButtonClicked != null) value = customButtonClicked;
			}
		}
		dispose();
	}

	/**
	 * Method called to complete a field when the dialog closes.
	 * @param field the field to process.
	 */
	private void finishField(Field field)
	{
		if (field == null) return;
		switch (field.type)
		{
			case Field.FIELD_STRING:
				field.finalValue = field.textField.getText();
				break;
			case Field.FIELD_SELECT:
				field.finalValue = field.combo.getSelectedItem();
				break;
			case Field.FIELD_COLOR:
				Color newColor = parseColor(field);
				if (newColor == null) break;
				field.finalValue = newColor;
				break;
		}
	}

	/**
	 * Method called to initialize the prompt dialog.
	 * @param wnd the EditWindow_ in which to show the dialog.
	 * @param ni the NodeInst (in the EditWindow_) over which to show the dialog.
	 * @param title the title of the dialog.
	 * @param fieldList if not null, a 1-dimensional array of fields to place in the dialog.
	 * @param fieldArray if not null, a 2-dimensional grid of fields to place in the dialog.
	 */
	private void initComponents(EditWindow_ wnd, NodeInst ni, String title, Field [] fieldList, Field [][] fieldArray)
	{
		getContentPane().setLayout(new GridBagLayout());

		setTitle(title);
		setName("");
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent evt) { closed = true;   exit(false); }
		});

		int buttonRow = 1;
		goodClicked = false;
		JComponent centerIt = null;
		this.fieldList = fieldList;
		this.fieldArray = fieldArray;
		if (fieldList != null)
		{
			for(int i=0; i<fieldList.length; i++)
			{
				centerIt = initializeField(fieldList[i], i, 0, centerIt);
			}
			buttonRow = fieldList.length + 1;
		} else if (fieldArray != null)
		{
			for(int i=0; i<fieldArray.length; i++)
			{
				Field [] row = fieldArray[i];
				for(int j=0; j<row.length; j++)
					centerIt = initializeField(row[j], i, j, centerIt);
			}
			buttonRow = fieldArray.length + 1;
		}
		if (centerIt == null)
			centerIt = fieldList[0].labelObj;

		String badButton = (yesNo ? "No" : "Cancel");
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

		String goodButton = (yesNo ? "Yes" : "OK");
		JButton ok = new JButton(goodButton);
		getRootPane().setDefaultButton(ok);
		gbc = new java.awt.GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = buttonRow;
		gbc.gridwidth = 3;
		gbc.insets = new java.awt.Insets(4, 4, 4, 4);
		gbc.weightx = 0.5;
		getContentPane().add(ok, gbc);
		ok.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt) { exit(true); }
		});

		pack();

		// now make the dialog appear over a node
		Point ew = wnd.getScreenLocationOfCorner();
		Point locInWnd = wnd.databaseToScreen(ni.getAnchorCenterX(), ni.getAnchorCenterY());
		Point textfield = centerIt.getLocation();
		Dimension textSize = centerIt.getSize();
		setLocation(locInWnd.x+ew.x-(textfield.x+textSize.width/2), locInWnd.y+ew.y-(textfield.y+textSize.height/2+20));
	}

	/**
	 * Method to initialize a field in the dialog.
	 * @param field the Field to initialize.
	 * @param i the Y index (0-based) of the field in the dialog.
	 * @param j the X index (0-based) of the field in the dialog.
	 * @param centerIt the component in the dialog that will be centered over the desired piece of circuitry.
	 * @return the new component in the dialog that will be centered over the desired piece of circuitry.
	 */
	private JComponent initializeField(Field field, int i, int j, JComponent centerIt)
	{
		if (field == null) return centerIt;
		if (field.label != null)
		{
			field.labelObj = new JLabel(field.label);
			GridBagConstraints gbc = new java.awt.GridBagConstraints();
			gbc.gridx = j*4;
			gbc.gridy = i;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			gbc.anchor = java.awt.GridBagConstraints.WEST;
			getContentPane().add(field.labelObj, gbc);
		}
		switch (field.type)
		{
			case Field.FIELD_STRING:
				field.textField = new JTextField((String)field.initial);
				GridBagConstraints gbc = new java.awt.GridBagConstraints();
				gbc.gridx = j*4+1;
				gbc.gridy = i;
				gbc.gridwidth = 3;
				gbc.weightx = 1.0;
				gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
				gbc.insets = new java.awt.Insets(4, 4, 4, 4);
				getContentPane().add(field.textField, gbc);
				if (centerIt == null)
				{
					field.textField.selectAll();
					centerIt = field.textField;
				}
				break;
			case Field.FIELD_SELECT:
				field.combo = new JComboBox();
				String [] poss = (String [])field.initial;
				for(int k=0; k<poss.length; k++)
					field.combo.addItem(poss[k]);
				field.combo.setSelectedItem(field.finalValue);
				gbc = new java.awt.GridBagConstraints();
				gbc.gridx = j*4+1;
				gbc.gridy = i;
				gbc.gridwidth = 3;
				gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
				gbc.insets = new java.awt.Insets(4, 4, 4, 4);
				getContentPane().add(field.combo, gbc);
				if (centerIt == null) centerIt = field.combo;
				break;
			case Field.FIELD_BUTTON:
				JButton but = (JButton)field.initial;
				but.addActionListener(new CustomButtonActionListener(this, field));
				gbc = new java.awt.GridBagConstraints();
				gbc.gridx = j*4+1;
				gbc.gridy = i;
				gbc.gridwidth = 3;
				gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
				gbc.insets = new java.awt.Insets(4, 4, 4, 4);
				getContentPane().add(but, gbc);
				if (centerIt == null) centerIt = but;
				break;			
			case Field.FIELD_COLOR:
				Color col = (Color)field.initial;
				field.patch = new JPanel();
				Dimension size = new Dimension(25, 25);
				field.patch.setSize(size);
				field.patch.setPreferredSize(size);
				field.patch.setBackground(col);
				gbc = new java.awt.GridBagConstraints();
				gbc.gridx = j*4+1;
				gbc.gridy = i;
				gbc.insets = new java.awt.Insets(4, 4, 4, 4);
				getContentPane().add(field.patch, gbc);
		
				field.textField = new JTextField();
				field.textField.setColumns(8);
				field.textField.setText(col.getRed() + "," + col.getGreen() + "," + col.getBlue());
				gbc = new java.awt.GridBagConstraints();
				gbc.gridx = j*4+2;
				gbc.gridy = i;
				gbc.insets = new java.awt.Insets(4, 4, 4, 4);
				getContentPane().add(field.textField, gbc);
				field.textField.getDocument().addDocumentListener(new ColorDocumentListener(field));
		
				field.but = new JButton("Set");
				gbc = new java.awt.GridBagConstraints();
				gbc.gridx = j*4+3;
				gbc.gridy = i;
				gbc.insets = new java.awt.Insets(4, 4, 4, 4);
				getContentPane().add(field.but, gbc);
				field.but.addActionListener(new MixColorActionListener(this, field));
				if (centerIt == null)
				{
					field.textField.selectAll();
					centerIt = field.but;
				}
				break;
		}
		return centerIt;
	}

	/**
	 * Method to extract a Color value from a Field.
	 * @param field the Field to examine (must hold a Color selection).
	 * @return the Color in that Field.
	 */
	private static Color parseColor(Field field)
	{
		String newColor = field.textField.getText();
		String [] rgb = newColor.split(",");
		if (rgb.length < 3) return null;
		int r = TextUtils.atoi(rgb[0]);
		if (r < 0) r = 0;   if (r > 255) r = 255;
		int g = TextUtils.atoi(rgb[1]);
		if (g < 0) g = 0;   if (g > 255) g = 255;
		int b = TextUtils.atoi(rgb[2]);
		if (b < 0) b = 0;   if (b > 255) b = 255;
		return new Color(r, g, b);
	}

	/**
	 * Class to handle clicks on the "set" color button.
	 */
	private static class MixColorActionListener implements ActionListener
	{
		private PromptAt top;
		private Field field;

		private MixColorActionListener(PromptAt top, Field field)
		{
			this.top = top;
			this.field = field;
		}

		public void actionPerformed(ActionEvent evt)
		{
			Color origColor = parseColor(field);
			Color newColor = JColorChooser.showDialog(top, "Edit color", origColor);
			if (newColor == null) return;
			field.textField.setText(newColor.getRed() + "," + newColor.getGreen() + "," + newColor.getBlue());
			field.patch.setBackground(newColor);
		}
	}

	/**
	 * Class to handle clicks on user's custom buttons.
	 */
	private static class CustomButtonActionListener implements ActionListener
	{
		private PromptAt top;
		private Field field;

		private CustomButtonActionListener(PromptAt top, Field field)
		{
			this.top = top;
			this.field = field;
		}

		public void actionPerformed(ActionEvent evt)
		{
			top.customButtonClicked = (String)field.finalValue;
			top.exit(true);
		}
	}

	/**
	 * Class to handle changes to the text description of a color value.
	 */
	private static class ColorDocumentListener implements DocumentListener
	{
		private Field which;

		private ColorDocumentListener(Field which)
		{
			this.which = which;
		}

		public void changedUpdate(DocumentEvent e) { updatePatchColor(); }
		public void insertUpdate(DocumentEvent e) { updatePatchColor(); }
		public void removeUpdate(DocumentEvent e) { updatePatchColor(); }

		private void updatePatchColor()
		{
			Color newColor = parseColor(which);
			which.patch.setBackground(newColor);
		}
	}
}
