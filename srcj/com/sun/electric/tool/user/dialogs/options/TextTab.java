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
import com.sun.electric.database.variable.MutableTextDescriptor;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TextWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

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

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return text; }

	/** return the name of this preferences tab. */
	public String getName() { return "Text"; }

	private MutableTextDescriptor initialTextNodeDescriptor, currentTextNodeDescriptor;
	private MutableTextDescriptor initialTextArcDescriptor, currentTextArcDescriptor;
	private MutableTextDescriptor initialTextExportDescriptor, currentTextExportDescriptor;
	private MutableTextDescriptor initialTextAnnotationDescriptor, currentTextAnnotationDescriptor;
	private MutableTextDescriptor initialTextInstanceDescriptor, currentTextInstanceDescriptor;
	private MutableTextDescriptor initialTextCellDescriptor, currentTextCellDescriptor;
	private MutableTextDescriptor currentTextDescriptor;
	private boolean textValuesChanging = false;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Text tab.
	 */
	public void init()
	{
		for (Iterator<TextDescriptor.Position> it = TextDescriptor.Position.getPositions(); it.hasNext(); )
		{
			TextDescriptor.Position pos = it.next();
			textAnchor.addItem(pos);
		}

		// get initial descriptors
		initialTextNodeDescriptor = MutableTextDescriptor.getNodeTextDescriptor();
		initialTextArcDescriptor = MutableTextDescriptor.getArcTextDescriptor();
		initialTextExportDescriptor = MutableTextDescriptor.getExportTextDescriptor();
		initialTextAnnotationDescriptor = MutableTextDescriptor.getAnnotationTextDescriptor();
		initialTextInstanceDescriptor = MutableTextDescriptor.getInstanceTextDescriptor();
		initialTextCellDescriptor = MutableTextDescriptor.getCellTextDescriptor();

		// get current descriptors (gets changed by dialog)
		currentTextNodeDescriptor = MutableTextDescriptor.getNodeTextDescriptor();
		currentTextArcDescriptor = MutableTextDescriptor.getArcTextDescriptor();
		currentTextExportDescriptor = MutableTextDescriptor.getExportTextDescriptor();
		currentTextAnnotationDescriptor = MutableTextDescriptor.getAnnotationTextDescriptor();
		currentTextInstanceDescriptor = MutableTextDescriptor.getInstanceTextDescriptor();
		currentTextCellDescriptor = MutableTextDescriptor.getCellTextDescriptor();

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		String [] fontNames = ge.getAvailableFontFamilyNames();
		textFace.addItem("DEFAULT FONT");
		for(int i=0; i<fontNames.length; i++)
		{
			textDefaultFont.addItem(fontNames[i]);
			textFace.addItem(fontNames[i]);
			textCellFont.addItem(fontNames[i]);
		}
		textDefaultFont.setSelectedItem(User.getDefaultFont());
		textCellFont.setSelectedItem(User.getDefaultTextCellFont());
		textCellSize.setText(Integer.toString(User.getDefaultTextCellSize()));

		textGlobalScale.setText(TextUtils.formatDouble(User.getGlobalTextScale() * 100));
		
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

	    currentTextDescriptor.setItalic(textItalic.isSelected());
        currentTextDescriptor.setBold(textBold.isSelected());
        currentTextDescriptor.setUnderline(textUnderline.isSelected());

		currentTextDescriptor.setPos((TextDescriptor.Position)textAnchor.getSelectedItem());

        currentTextDescriptor.setInterior(textNewVisibleInsideCell.isSelected());

		int face = 0;
		if (textFace.getSelectedIndex() != 0)
		{
			TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont((String)textFace.getSelectedItem());
            if (af != null)
                face = af.getIndex();
		}
		currentTextDescriptor.setFace(face);
	}

	private void textButtonChanged()
	{
		currentTextDescriptor = null;
		if (textNodes.isSelected())
		{
			currentTextDescriptor = currentTextNodeDescriptor;
		} else if (textArcs.isSelected())
		{
			currentTextDescriptor = currentTextArcDescriptor;
		} else if (textPorts.isSelected())
		{
			currentTextDescriptor = currentTextExportDescriptor;
		} else if (textAnnotation.isSelected())
		{
			currentTextDescriptor = currentTextAnnotationDescriptor;
		} else if (textInstances.isSelected())
		{
			currentTextDescriptor = currentTextInstanceDescriptor;
		} else if (textCellText.isSelected())
		{
			currentTextDescriptor = currentTextCellDescriptor;
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
			textUnitSize.setText(TextUtils.formatDouble(size.getSize()));
			textPointSize.setText("");
		}
		textItalic.setSelected(currentTextDescriptor.isItalic());
		textBold.setSelected(currentTextDescriptor.isBold());
		textUnderline.setSelected(currentTextDescriptor.isUnderline());
	
		textAnchor.setSelectedItem(currentTextDescriptor.getPos());
		textValuesChanging = false;

		textNewVisibleInsideCell.setSelected(currentTextDescriptor.isInterior());

		int face = currentTextDescriptor.getFace();
		if (face == 0)
		{
			textFace.setSelectedIndex(0);
		} else
		{
			String fontName = TextDescriptor.ActiveFont.findActiveFont(face).getName();
			textFace.setSelectedItem(fontName);
		}
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Text tab.
	 */
	public void term()
	{
		boolean editCellsChanged = false;
		boolean textCellsChanged = false;

		String currentFontName = (String)textDefaultFont.getSelectedItem();
		if (!currentFontName.equalsIgnoreCase(User.getDefaultFont()))
		{
			User.setDefaultFont(currentFontName);
			editCellsChanged = true;
		}

		currentFontName = (String)textCellFont.getSelectedItem();
		if (!currentFontName.equalsIgnoreCase(User.getDefaultTextCellFont()))
		{
			User.setDefaultTextCellFont(currentFontName);
			textCellsChanged = true;
		}

		int currInt = TextUtils.atoi(textCellSize.getText());
		if (currInt != User.getDefaultTextCellSize())
		{
			User.setDefaultTextCellSize(currInt);
			textCellsChanged = true;
		}

		if (!currentTextNodeDescriptor.equals(initialTextNodeDescriptor))
			TextDescriptor.setNodeTextDescriptor(currentTextNodeDescriptor);
		if (!currentTextArcDescriptor.equals(initialTextArcDescriptor))
			TextDescriptor.setArcTextDescriptor(currentTextArcDescriptor);
		if (!currentTextExportDescriptor.equals(initialTextExportDescriptor))
			TextDescriptor.setExportTextDescriptor(currentTextExportDescriptor);
		if (!currentTextAnnotationDescriptor.equals(initialTextAnnotationDescriptor))
			TextDescriptor.setAnnotationTextDescriptor(currentTextAnnotationDescriptor);
		if (!currentTextInstanceDescriptor.equals(initialTextInstanceDescriptor))
			TextDescriptor.setInstanceTextDescriptor(currentTextInstanceDescriptor);
		if (!currentTextCellDescriptor.equals(initialTextCellDescriptor))
			TextDescriptor.setCellTextDescriptor(currentTextCellDescriptor);

		double currentGlobalScale = TextUtils.atof(textGlobalScale.getText()) / 100;
		if (currentGlobalScale != User.getGlobalTextScale())
		{
			User.setGlobalTextScale(currentGlobalScale);
			editCellsChanged = true;
		}

		if (textCellsChanged || editCellsChanged)
		{
			// redraw appropriate cells
			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = it.next();
				if (textCellsChanged && wf.getContent() instanceof TextWindow)
				{
					TextWindow tw = (TextWindow)wf.getContent();
					tw.updateFontInformation();
				}
				if (editCellsChanged && wf.getContent() instanceof EditWindow)
				{
					EditWindow wnd = (EditWindow)wf.getContent();
					wnd.repaintContents(null, false);
				}
			}
		}
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents()
    {
        java.awt.GridBagConstraints gridBagConstraints;

        textSizeGroup = new javax.swing.ButtonGroup();
        textTypeGroup = new javax.swing.ButtonGroup();
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
        jLabel6 = new javax.swing.JLabel();
        textAnchor = new javax.swing.JComboBox();
        textNewVisibleInsideCell = new javax.swing.JCheckBox();
        globals = new javax.swing.JPanel();
        jLabel44 = new javax.swing.JLabel();
        textDefaultFont = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        textGlobalScale = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        textCells = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        textCellFont = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        textCellSize = new javax.swing.JTextField();

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

        top.setBorder(javax.swing.BorderFactory.createTitledBorder("Default Style for New Text"));
        jLabel41.setText("Which type of new text:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
        top.add(jLabel41, gridBagConstraints);

        textTypeGroup.add(textNodes);
        textNodes.setText("Node text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        top.add(textNodes, gridBagConstraints);

        textTypeGroup.add(textArcs);
        textArcs.setText("Arc text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        top.add(textArcs, gridBagConstraints);

        textTypeGroup.add(textPorts);
        textPorts.setText("Exports/Ports");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        top.add(textPorts, gridBagConstraints);

        textTypeGroup.add(textAnnotation);
        textAnnotation.setText("Annotation text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        top.add(textAnnotation, gridBagConstraints);

        textTypeGroup.add(textInstances);
        textInstances.setText("Instance names");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        top.add(textInstances, gridBagConstraints);

        textTypeGroup.add(textCellText);
        textCellText.setText("Cell text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        top.add(textCellText, gridBagConstraints);

        jLabel42.setText("Size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(jLabel42, gridBagConstraints);

        textPointSize.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        top.add(textPointSize, gridBagConstraints);

        textUnitSize.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 4);
        top.add(textUnitSize, gridBagConstraints);

        jLabel43.setText("Font:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(jLabel43, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(textFace, gridBagConstraints);

        textItalic.setText("Italic");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        top.add(textItalic, gridBagConstraints);

        textBold.setText("Bold");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        top.add(textBold, gridBagConstraints);

        textUnderline.setText("Underline");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        top.add(textUnderline, gridBagConstraints);

        textSizeGroup.add(textPoints);
        textPoints.setText("Points (max 63)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        top.add(textPoints, gridBagConstraints);

        textSizeGroup.add(textUnits);
        textUnits.setText("Units (max 127.75)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 4);
        top.add(textUnits, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 30, 4, 30);
        top.add(jSeparator3, gridBagConstraints);

        jLabel6.setText("Anchor:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(jLabel6, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        top.add(textAnchor, gridBagConstraints);

        textNewVisibleInsideCell.setText("Invisible outside cell");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        top.add(textNewVisibleInsideCell, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        text.add(top, gridBagConstraints);

        globals.setLayout(new java.awt.GridBagLayout());

        globals.setBorder(javax.swing.BorderFactory.createTitledBorder("Everywhere:"));
        jLabel44.setText("Default font:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        globals.add(jLabel44, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        globals.add(textDefaultFont, gridBagConstraints);

        jLabel1.setText("Global text scale:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        globals.add(jLabel1, gridBagConstraints);

        textGlobalScale.setColumns(10);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        globals.add(textGlobalScale, gridBagConstraints);

        jLabel2.setText("percent");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        globals.add(jLabel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        text.add(globals, gridBagConstraints);

        textCells.setLayout(new java.awt.GridBagLayout());

        textCells.setBorder(javax.swing.BorderFactory.createTitledBorder("For Textual Cells:"));
        jLabel7.setText("Font:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        textCells.add(jLabel7, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        textCells.add(textCellFont, gridBagConstraints);

        jLabel3.setText("Size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        textCells.add(jLabel3, gridBagConstraints);

        textCellSize.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        textCells.add(textCellSize, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        text.add(textCells, gridBagConstraints);

        getContentPane().add(text, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel globals;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JPanel text;
    private javax.swing.JComboBox textAnchor;
    private javax.swing.JRadioButton textAnnotation;
    private javax.swing.JRadioButton textArcs;
    private javax.swing.JCheckBox textBold;
    private javax.swing.JComboBox textCellFont;
    private javax.swing.JTextField textCellSize;
    private javax.swing.JRadioButton textCellText;
    private javax.swing.JPanel textCells;
    private javax.swing.JComboBox textDefaultFont;
    private javax.swing.JComboBox textFace;
    private javax.swing.JTextField textGlobalScale;
    private javax.swing.JRadioButton textInstances;
    private javax.swing.JCheckBox textItalic;
    private javax.swing.JCheckBox textNewVisibleInsideCell;
    private javax.swing.JRadioButton textNodes;
    private javax.swing.JTextField textPointSize;
    private javax.swing.JRadioButton textPoints;
    private javax.swing.JRadioButton textPorts;
    private javax.swing.ButtonGroup textSizeGroup;
    private javax.swing.ButtonGroup textTypeGroup;
    private javax.swing.JCheckBox textUnderline;
    private javax.swing.JTextField textUnitSize;
    private javax.swing.JRadioButton textUnits;
    private javax.swing.JPanel top;
    // End of variables declaration//GEN-END:variables

}
