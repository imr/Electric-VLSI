/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TextTab.java
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
package com.sun.electric.tool.user.dialogs.options;

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.tool.user.User;

import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;

import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Class to handle the "Text" tab of the Preferences dialog.
 */
public class TextTab extends PreferencePanel
{
	/** Creates new form TextTab */
	public TextTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	public JPanel getPanel() { return text; }

	public String getName() { return "Text"; }

	private String initialTextFontName;
	private TextDescriptor initialTextNodeDescriptor, currentTextNodeDescriptor;
	private TextDescriptor initialTextArcDescriptor, currentTextArcDescriptor;
	private TextDescriptor initialTextExportDescriptor, currentTextExportDescriptor;
	private TextDescriptor initialTextAnnotationDescriptor, currentTextAnnotationDescriptor;
	private TextDescriptor initialTextInstanceDescriptor, currentTextInstanceDescriptor;
	private TextDescriptor initialTextCellDescriptor, currentTextCellDescriptor;
	private TextDescriptor currentTextDescriptor;
	private String initialTextNodeFont;
	private String initialTextArcFont;
	private String initialTextExportFont;
	private String initialTextAnnotationFont;
	private String initialTextInstanceFont;
	private String initialTextCellFont;
	private int initialTextSmartVertical;
	private int initialTextSmartHorizontal;
	private StringBuffer currentTextNodeFont;
	private StringBuffer currentTextArcFont;
	private StringBuffer currentTextExportFont;
	private StringBuffer currentTextAnnotationFont;
	private StringBuffer currentTextInstanceFont;
	private StringBuffer currentTextCellFont;
	private StringBuffer currentTextFont;
	private boolean textValuesChanging = false;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Text tab.
	 */
	public void init()
	{
		for (Iterator it = TextDescriptor.Position.getPositions(); it.hasNext(); )
		{
			TextDescriptor.Position pos = (TextDescriptor.Position)it.next();
			textAnchor.addItem(pos);
		}

		// get initial descriptors
		initialTextNodeDescriptor = TextDescriptor.getNodeTextDescriptor(null);
		initialTextArcDescriptor = TextDescriptor.getArcTextDescriptor(null);
		initialTextExportDescriptor = TextDescriptor.getExportTextDescriptor(null);
		initialTextAnnotationDescriptor = TextDescriptor.getAnnotationTextDescriptor(null);
		initialTextInstanceDescriptor = TextDescriptor.getInstanceTextDescriptor(null);
		initialTextCellDescriptor = TextDescriptor.getCellTextDescriptor(null);
		initialTextNodeFont = TextDescriptor.getNodeTextDescriptorFont();
		initialTextArcFont = TextDescriptor.getArcTextDescriptorFont();
		initialTextExportFont = TextDescriptor.getExportTextDescriptorFont();
		initialTextAnnotationFont = TextDescriptor.getAnnotationTextDescriptorFont();
		initialTextInstanceFont = TextDescriptor.getInstanceTextDescriptorFont();
		initialTextCellFont = TextDescriptor.getCellTextDescriptorFont();

		// get current descriptors (gets changed by dialog)
		currentTextNodeDescriptor = TextDescriptor.getNodeTextDescriptor(null);
		currentTextArcDescriptor = TextDescriptor.getArcTextDescriptor(null);
		currentTextExportDescriptor = TextDescriptor.getExportTextDescriptor(null);
		currentTextAnnotationDescriptor = TextDescriptor.getAnnotationTextDescriptor(null);
		currentTextInstanceDescriptor = TextDescriptor.getInstanceTextDescriptor(null);
		currentTextCellDescriptor = TextDescriptor.getCellTextDescriptor(null);
		currentTextNodeFont = new StringBuffer(TextDescriptor.getNodeTextDescriptorFont());
		currentTextArcFont = new StringBuffer(TextDescriptor.getArcTextDescriptorFont());
		currentTextExportFont = new StringBuffer(TextDescriptor.getExportTextDescriptorFont());
		currentTextAnnotationFont = new StringBuffer(TextDescriptor.getAnnotationTextDescriptorFont());
		currentTextInstanceFont = new StringBuffer(TextDescriptor.getInstanceTextDescriptorFont());
		currentTextCellFont = new StringBuffer(TextDescriptor.getCellTextDescriptorFont());

		initialTextSmartVertical = User.getSmartVerticalPlacement();
		switch (initialTextSmartVertical)
		{
			case 0: textSmartVerticalOff.setSelected(true);       break;
			case 1: textSmartVerticalInside.setSelected(true);    break;
			case 2: textSmartVerticalOutside.setSelected(true);   break;
		}
		initialTextSmartHorizontal = User.getSmartHorizontalPlacement();
		switch (initialTextSmartVertical)
		{
			case 0: textSmartHorizontalOff.setSelected(true);       break;
			case 1: textSmartHorizontalInside.setSelected(true);    break;
			case 2: textSmartHorizontalOutside.setSelected(true);   break;
		}

		initialTextFontName = User.getDefaultFont();
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		String [] fontNames = ge.getAvailableFontFamilyNames();
		textFace.addItem("DEFAULT FONT");
		for(int i=0; i<fontNames.length; i++)
		{
			textDefaultFont.addItem(fontNames[i]);
			textFace.addItem(fontNames[i]);
		}
		textDefaultFont.setSelectedItem(initialTextFontName);

		textNodes.setSelected(true);
		textButtonChanged();

		textNodes.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { textButtonChanged(); }
		});
		textArcs.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { textButtonChanged(); }
		});
		textPorts.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { textButtonChanged(); }
		});
		textAnnotation.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { textButtonChanged(); }
		});
		textInstances.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { textButtonChanged(); }
		});
		textCellText.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { textButtonChanged(); }
		});

		textPoints.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { textValuesChanged(); }
		});
		textUnits.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { textValuesChanged(); }
		});
		textItalic.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { textValuesChanged(); }
		});
		textBold.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { textValuesChanged(); }
		});
		textUnderline.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { textValuesChanged(); }
		});
		textFace.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { textValuesChanged(); }
		});
		textAnchor.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { textValuesChanged(); }
		});
		textNewVisibleInsideCell.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { textValuesChanged(); }
		});
		textPointSize.getDocument().addDocumentListener(new TextSizeDocumentListener(this));
		textUnitSize.getDocument().addDocumentListener(new TextSizeDocumentListener(this));
	}

	/**
	 * Class to handle special changes to text sizes.
	 */
	private static class TextSizeDocumentListener implements DocumentListener
	{
		TextTab dialog;

		TextSizeDocumentListener(TextTab dialog) { this.dialog = dialog; }

		public void changedUpdate(DocumentEvent e) { dialog.textValuesChanged(); }
		public void insertUpdate(DocumentEvent e) { dialog.textValuesChanged(); }
		public void removeUpdate(DocumentEvent e) { dialog.textValuesChanged(); }
	}

	private void textValuesChanged()
	{
		if (textValuesChanging) return;
		if (textPoints.isSelected())
		{
			int size = TextUtils.atoi(textPointSize.getText());
			currentTextDescriptor.setAbsSize(size);
		} else
		{
			double size = TextUtils.atof(textUnitSize.getText());
			currentTextDescriptor.setRelSize(size);
		}

		if (textItalic.isSelected()) currentTextDescriptor.setItalic(); else
			currentTextDescriptor.clearItalic();
		if (textBold.isSelected()) currentTextDescriptor.setBold(); else
			currentTextDescriptor.clearBold();
		if (textUnderline.isSelected()) currentTextDescriptor.setUnderline(); else
			currentTextDescriptor.clearUnderline();

		currentTextDescriptor.setPos((TextDescriptor.Position)textAnchor.getSelectedItem());

		if (textNewVisibleInsideCell.isSelected()) currentTextDescriptor.setInterior(); else
			currentTextDescriptor.clearInterior();

		int index = textFace.getSelectedIndex();
		int len = currentTextFont.length();
		currentTextFont.delete(0, len);
		if (index != 0)
			currentTextFont.append((String)textFace.getSelectedItem());
	}

	private void textButtonChanged()
	{
		currentTextDescriptor = null;
		if (textNodes.isSelected())
		{
			currentTextDescriptor = currentTextNodeDescriptor;
			currentTextFont = currentTextNodeFont;
		} else if (textArcs.isSelected())
		{
			currentTextDescriptor = currentTextArcDescriptor;
			currentTextFont = currentTextArcFont;
		} else if (textPorts.isSelected())
		{
			currentTextDescriptor = currentTextExportDescriptor;
			currentTextFont = currentTextExportFont;
		} else if (textAnnotation.isSelected())
		{
			currentTextDescriptor = currentTextAnnotationDescriptor;
			currentTextFont = currentTextAnnotationFont;
		} else if (textInstances.isSelected())
		{
			currentTextDescriptor = currentTextInstanceDescriptor;
			currentTextFont = currentTextInstanceFont;
		} else if (textCellText.isSelected())
		{
			currentTextDescriptor = currentTextCellDescriptor;
			currentTextFont = currentTextCellFont;
		}
		loadCurrentDescriptorInfo();
	}

	private void loadCurrentDescriptorInfo()
	{
		textValuesChanging = true;
		TextDescriptor.Size size = currentTextDescriptor.getSize();
		if (size.isAbsolute())
		{
			textPoints.setSelected(true);
			textPointSize.setText(Integer.toString((int)size.getSize()));
			textUnitSize.setText("");
		} else
		{
			textUnits.setSelected(true);
			textUnitSize.setText(Double.toString(size.getSize()));
			textPointSize.setText("");
		}
		textItalic.setSelected(currentTextDescriptor.isItalic());
		textBold.setSelected(currentTextDescriptor.isBold());
		textUnderline.setSelected(currentTextDescriptor.isUnderline());
		
		textAnchor.setSelectedItem(currentTextDescriptor.getPos());
		textValuesChanging = false;

		textNewVisibleInsideCell.setSelected(currentTextDescriptor.isInterior());

		if (currentTextFont.length() == 0) textFace.setSelectedIndex(0); else
			textFace.setSelectedItem(currentTextFont.toString());
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Text tab.
	 */
	public void term()
	{
		String currentFontName = (String)textDefaultFont.getSelectedItem();
		if (!currentFontName.equalsIgnoreCase(initialTextFontName))
			User.setDefaultFont(currentFontName);

		if (!currentTextNodeDescriptor.compare(initialTextNodeDescriptor))
			TextDescriptor.setNodeTextDescriptor(currentTextNodeDescriptor);
		if (!currentTextArcDescriptor.compare(initialTextArcDescriptor))
			TextDescriptor.setArcTextDescriptor(currentTextArcDescriptor);
		if (!currentTextExportDescriptor.compare(initialTextExportDescriptor))
			TextDescriptor.setExportTextDescriptor(currentTextExportDescriptor);
		if (!currentTextAnnotationDescriptor.compare(initialTextAnnotationDescriptor))
			TextDescriptor.setAnnotationTextDescriptor(currentTextAnnotationDescriptor);
		if (!currentTextInstanceDescriptor.compare(initialTextInstanceDescriptor))
			TextDescriptor.setInstanceTextDescriptor(currentTextInstanceDescriptor);
		if (!currentTextCellDescriptor.compare(initialTextCellDescriptor))
			TextDescriptor.setCellTextDescriptor(currentTextCellDescriptor);

		if (!currentTextNodeFont.toString().equals(initialTextNodeFont))
			TextDescriptor.setNodeTextDescriptorFont(currentTextNodeFont.toString());
		if (!currentTextArcFont.toString().equals(initialTextArcFont))
			TextDescriptor.setArcTextDescriptorFont(currentTextArcFont.toString());
		if (!currentTextExportFont.toString().equals(initialTextExportFont))
			TextDescriptor.setExportTextDescriptorFont(currentTextExportFont.toString());
		if (!currentTextAnnotationFont.toString().equals(initialTextAnnotationFont))
			TextDescriptor.setAnnotationTextDescriptorFont(currentTextAnnotationFont.toString());
		if (!currentTextInstanceFont.toString().equals(initialTextInstanceFont))
			TextDescriptor.setInstanceTextDescriptorFont(currentTextInstanceFont.toString());
		if (!currentTextCellFont.toString().equals(initialTextCellFont))
			TextDescriptor.setCellTextDescriptorFont(currentTextCellFont.toString());

		int currentSmartVertical = 0;
		if (textSmartVerticalInside.isSelected()) currentSmartVertical = 1; else
			if (textSmartVerticalOutside.isSelected()) currentSmartVertical = 2;
		if (currentSmartVertical != initialTextSmartVertical)
			User.setSmartVerticalPlacement(currentSmartVertical);

		int currentSmartHorizontal = 0;
		if (textSmartHorizontalInside.isSelected()) currentSmartHorizontal = 1; else
			if (textSmartHorizontalOutside.isSelected()) currentSmartHorizontal = 2;
		if (currentSmartHorizontal != initialTextSmartHorizontal)
			User.setSmartHorizontalPlacement(currentSmartHorizontal);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        textSizeGroup = new javax.swing.ButtonGroup();
        textTypeGroup = new javax.swing.ButtonGroup();
        textVerticalGroup = new javax.swing.ButtonGroup();
        textHorizontalGroup = new javax.swing.ButtonGroup();
        text = new javax.swing.JPanel();
        top = new javax.swing.JPanel();
        jLabel41 = new javax.swing.JLabel();
        textNodes = new javax.swing.JRadioButton();
        textArcs = new javax.swing.JRadioButton();
        textPorts = new javax.swing.JRadioButton();
        textAnnotation = new javax.swing.JRadioButton();
        textInstances = new javax.swing.JRadioButton();
        textCellText = new javax.swing.JRadioButton();
        jLabel42 = new javax.swing.JLabel();
        textPointSize = new javax.swing.JTextField();
        textUnitSize = new javax.swing.JTextField();
        jLabel43 = new javax.swing.JLabel();
        textFace = new javax.swing.JComboBox();
        textItalic = new javax.swing.JCheckBox();
        textBold = new javax.swing.JCheckBox();
        textUnderline = new javax.swing.JCheckBox();
        textPoints = new javax.swing.JRadioButton();
        textUnits = new javax.swing.JRadioButton();
        jSeparator3 = new javax.swing.JSeparator();
        jLabel4 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        textAnchor = new javax.swing.JComboBox();
        textNewVisibleInsideCell = new javax.swing.JCheckBox();
        middle = new javax.swing.JPanel();
        jLabel44 = new javax.swing.JLabel();
        textDefaultFont = new javax.swing.JComboBox();
        bottom = new javax.swing.JPanel();
        jLabel56 = new javax.swing.JLabel();
        textSmartVerticalOff = new javax.swing.JRadioButton();
        textSmartVerticalInside = new javax.swing.JRadioButton();
        textSmartVerticalOutside = new javax.swing.JRadioButton();
        jLabel57 = new javax.swing.JLabel();
        textSmartHorizontalOff = new javax.swing.JRadioButton();
        textSmartHorizontalInside = new javax.swing.JRadioButton();
        textSmartHorizontalOutside = new javax.swing.JRadioButton();
        jSeparator1 = new javax.swing.JSeparator();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Edit Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        text.setLayout(new java.awt.GridBagLayout());

        top.setLayout(new java.awt.GridBagLayout());

        top.setBorder(new javax.swing.border.TitledBorder("Default Text Style"));
        jLabel41.setText("Which type of text:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
        top.add(jLabel41, gridBagConstraints);

        textNodes.setText("Nodes");
        textTypeGroup.add(textNodes);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        top.add(textNodes, gridBagConstraints);

        textArcs.setText("Arcs");
        textTypeGroup.add(textArcs);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        top.add(textArcs, gridBagConstraints);

        textPorts.setText("Exports/Ports");
        textTypeGroup.add(textPorts);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        top.add(textPorts, gridBagConstraints);

        textAnnotation.setText("Annotation text");
        textTypeGroup.add(textAnnotation);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        top.add(textAnnotation, gridBagConstraints);

        textInstances.setText("Instance names");
        textTypeGroup.add(textInstances);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        top.add(textInstances, gridBagConstraints);

        textCellText.setText("Cell text");
        textTypeGroup.add(textCellText);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        top.add(textCellText, gridBagConstraints);

        jLabel42.setText("Size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(jLabel42, gridBagConstraints);

        textPointSize.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        top.add(textPointSize, gridBagConstraints);

        textUnitSize.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        top.add(textUnitSize, gridBagConstraints);

        jLabel43.setText("Font::");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(jLabel43, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(textFace, gridBagConstraints);

        textItalic.setText("Italic");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(textItalic, gridBagConstraints);

        textBold.setText("Bold");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(textBold, gridBagConstraints);

        textUnderline.setText("Underline");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(textUnderline, gridBagConstraints);

        textPoints.setText("Points (max 63)");
        textSizeGroup.add(textPoints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        top.add(textPoints, gridBagConstraints);

        textUnits.setText("Units (max 127.75)");
        textSizeGroup.add(textUnits);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        top.add(textUnits, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(jSeparator3, gridBagConstraints);

        jLabel4.setText("Default style:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(jLabel4, gridBagConstraints);

        jLabel6.setText("Anchor:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(jLabel6, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(textAnchor, gridBagConstraints);

        textNewVisibleInsideCell.setText("Invisible outside cell");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(textNewVisibleInsideCell, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        text.add(top, gridBagConstraints);

        middle.setLayout(new java.awt.GridBagLayout());

        jLabel44.setText("Default font:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        middle.add(jLabel44, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        middle.add(textDefaultFont, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        text.add(middle, gridBagConstraints);

        bottom.setLayout(new java.awt.GridBagLayout());

        bottom.setBorder(new javax.swing.border.TitledBorder("Smart Placement of Export Text"));
        jLabel56.setText("Vertical");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
        bottom.add(jLabel56, gridBagConstraints);

        textSmartVerticalOff.setText("Off");
        textVerticalGroup.add(textSmartVerticalOff);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        bottom.add(textSmartVerticalOff, gridBagConstraints);

        textSmartVerticalInside.setText("Inside");
        textVerticalGroup.add(textSmartVerticalInside);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        bottom.add(textSmartVerticalInside, gridBagConstraints);

        textSmartVerticalOutside.setText("Outside");
        textVerticalGroup.add(textSmartVerticalOutside);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
        bottom.add(textSmartVerticalOutside, gridBagConstraints);

        jLabel57.setText("Horizontal");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
        bottom.add(jLabel57, gridBagConstraints);

        textSmartHorizontalOff.setText("Off");
        textHorizontalGroup.add(textSmartHorizontalOff);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        bottom.add(textSmartHorizontalOff, gridBagConstraints);

        textSmartHorizontalInside.setText("Inside");
        textHorizontalGroup.add(textSmartHorizontalInside);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        bottom.add(textSmartHorizontalInside, gridBagConstraints);

        textSmartHorizontalOutside.setText("Outside");
        textHorizontalGroup.add(textSmartHorizontalOutside);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
        bottom.add(textSmartHorizontalOutside, gridBagConstraints);

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.weighty = 1.0;
        bottom.add(jSeparator1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        text.add(bottom, gridBagConstraints);

        getContentPane().add(text, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel bottom;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel56;
    private javax.swing.JLabel jLabel57;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JPanel middle;
    private javax.swing.JPanel text;
    private javax.swing.JComboBox textAnchor;
    private javax.swing.JRadioButton textAnnotation;
    private javax.swing.JRadioButton textArcs;
    private javax.swing.JCheckBox textBold;
    private javax.swing.JRadioButton textCellText;
    private javax.swing.JComboBox textDefaultFont;
    private javax.swing.JComboBox textFace;
    private javax.swing.ButtonGroup textHorizontalGroup;
    private javax.swing.JRadioButton textInstances;
    private javax.swing.JCheckBox textItalic;
    private javax.swing.JCheckBox textNewVisibleInsideCell;
    private javax.swing.JRadioButton textNodes;
    private javax.swing.JTextField textPointSize;
    private javax.swing.JRadioButton textPoints;
    private javax.swing.JRadioButton textPorts;
    private javax.swing.ButtonGroup textSizeGroup;
    private javax.swing.JRadioButton textSmartHorizontalInside;
    private javax.swing.JRadioButton textSmartHorizontalOff;
    private javax.swing.JRadioButton textSmartHorizontalOutside;
    private javax.swing.JRadioButton textSmartVerticalInside;
    private javax.swing.JRadioButton textSmartVerticalOff;
    private javax.swing.JRadioButton textSmartVerticalOutside;
    private javax.swing.ButtonGroup textTypeGroup;
    private javax.swing.JCheckBox textUnderline;
    private javax.swing.JTextField textUnitSize;
    private javax.swing.JRadioButton textUnits;
    private javax.swing.ButtonGroup textVerticalGroup;
    private javax.swing.JPanel top;
    // End of variables declaration//GEN-END:variables
	
}
