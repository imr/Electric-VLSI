/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ChangeText.java
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TextWindow;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.Frame;
import java.awt.Insets;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import javax.swing.JDialog;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSeparator;


/**
 * Class to handle the "Change Text" dialog.
 */
public class ChangeText extends EDialog
{
	private static boolean nodesSelected = false;
	private static boolean arcsSelected = false;
	private static boolean exportsSelected = false;
	private static boolean annotationsSelected = false;
	private static boolean instancesSelected = false;
	private static boolean cellsSelected = false;

	private Cell cell;
	private int numToChange;
	private int lowPointSize, highPointSize;
	private double lowUnitSize, highUnitSize;
	private int numNodesChanged, numArcsChanged, numExportsChanged;
	private int numAnnotationsChanged, numInstancesChanged, numCellsChanged;

	public static void changeTextDialog()
	{
		ChangeText dialog = new ChangeText(TopLevel.getCurrentJFrame(), true);
		dialog.show();
	}

	/** Creates new form Change Text */
	private ChangeText(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();

		getRootPane().setDefaultButton(ok);
		useUnits.setSelected(true);
		changeNodeText.setSelected(nodesSelected);
		changeArcText.setSelected(arcsSelected);
		changeExportText.setSelected(exportsSelected);
		changeAnnotationText.setSelected(annotationsSelected);
		changeInstanceText.setSelected(instancesSelected);
		changeCellText.setSelected(cellsSelected);

		cell = WindowFrame.getCurrentCell();
		for(Iterator it = View.getOrderedViews().iterator(); it.hasNext(); )
		{
			View view = (View)it.next();
			viewList.addItem(view.getFullName());
		}

		font.addItem("DEFAULT FONT");
		Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
		for(int i=0; i<fonts.length; i++)
			font.addItem(fonts[i].getFontName());

		if (Highlight.getNumHighlights() == 0)
		{
			changeSelectedObjects.setEnabled(false);
			changeAllInCell.setSelected(true);
		} else
		{
			changeSelectedObjects.setSelected(true);
		}
		findSelectedText(false);

		changeSelectedObjects.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { findSelectedText(false); }
		});
		changeAllInCell.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { findSelectedText(false); }
		});
		changeCellsWithView.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { findSelectedText(false); }
		});
		viewList.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { findSelectedText(false); }
		});
		changeAllInLibrary.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { findSelectedText(false); }
		});

		changeNodeText.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { findSelectedText(false); }
		});
		changeArcText.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { findSelectedText(false); }
		});
		changeExportText.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { findSelectedText(false); }
		});
		changeAnnotationText.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { findSelectedText(false); }
		});
		changeInstanceText.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { findSelectedText(false); }
		});
		changeCellText.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { findSelectedText(false); }
		});
	}

	protected void escapePressed() { cancelActionPerformed(null); }

	/**
	 * Method to scan for all relevant text.
	 * This looks at the top part of the dialog to figure out which text is relevant.
	 * @param change true to change the relevant text according to the bottom of the dialog;
	 * false to gather the relevant text sizes for display.
	 */
	private void findSelectedText(boolean change)
	{
		nodesSelected = changeNodeText.isSelected();
		arcsSelected = changeArcText.isSelected();
		exportsSelected = changeExportText.isSelected();
		annotationsSelected = changeAnnotationText.isSelected();
		instancesSelected = changeInstanceText.isSelected();
		cellsSelected = changeCellText.isSelected();
		numToChange = 0;
		lowPointSize = -1;
		lowUnitSize = -1;
		if (changeSelectedObjects.isSelected())
		{
			for(Iterator it = Highlight.getHighlightedText(false).iterator(); it.hasNext(); )
			{
				Highlight h = (Highlight)it.next();
				if (h.getType() != Highlight.Type.TEXT) continue;
				accumulateTextFound(h.getElectricObject(), h.getVar(), h.getName(), change);
			}
		} else if (changeAllInCell.isSelected())
		{
			findAllInCell(cell, change);
		} else if (changeCellsWithView.isSelected())
		{
			String viewName = (String)viewList.getSelectedItem();
			View v = View.findView(viewName);
			if (v != null)
			{
				for(Iterator it = Library.getCurrent().getCells(); it.hasNext(); )
				{
					Cell c = (Cell)it.next();
					if (c.getView() == v) findAllInCell(c, change);
				}
			}
		} else if (changeAllInLibrary.isSelected())
		{
			for(Iterator it = Library.getCurrent().getCells(); it.hasNext(); )
			{
				Cell c = (Cell)it.next();
				findAllInCell(c, change);
			}
		}
		if (change) return;
		if (numToChange == 0)
		{
			selectedText.setText("No text to change");
		} else
		{
			String what = "Text runs from ";
			if (lowPointSize >= 0) what += lowPointSize + " to " + highPointSize + " points";
			if (lowUnitSize >= 0)
			{
				if (lowPointSize >= 0) what += "; ";
				what += lowUnitSize + " to " + highUnitSize + " units";
			}
			selectedText.setText(what);
			if (lowUnitSize >= 0)
			{
				useUnits.setSelected(true);
				unitSize.setText(Double.toString(highUnitSize));
			} else
			{
				usePoints.setSelected(true);
				pointSize.setText(Integer.toString(highPointSize));
			}
		}
	}

	/**
	 * Method to grab all text in a Cell and process it.
	 * @param cell the cell to examine.
	 * @param change true to change the text in the cell according to the bottom of the dialog;
	 * false to gather the text sizes in the cell for display.
	 */
	private void findAllInCell(Cell cell, boolean change)
	{
		// text on nodes
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getProto() instanceof Cell && !ni.isExpanded())
			{
				// cell instance text
				accumulateTextFound(ni, null, null, change);
			}
			if (!ni.getNameKey().isTempname())
			{
				// node name
				accumulateTextFound(ni, null, ni.getNameKey(), change);
			}
			for(Iterator vIt = ni.getVariables(); vIt.hasNext(); )
			{
				Variable var = (Variable)vIt.next();
				if (!var.isDisplay()) continue;
				accumulateTextFound(ni, var, null, change);
			}
		}

		// text on arcs
		for(Iterator it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			if (!ai.getNameKey().isTempname())
			{
				// arc name
				accumulateTextFound(ai, null, ai.getNameKey(), change);
			}
			for(Iterator vIt = ai.getVariables(); vIt.hasNext(); )
			{
				Variable var = (Variable)vIt.next();
				if (!var.isDisplay()) continue;
				accumulateTextFound(ai, var, null, change);
			}
		}

		// text on exports
		for(Iterator it = cell.getPorts(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			accumulateTextFound(pp, null, null, change);
			for(Iterator vIt = pp.getVariables(); vIt.hasNext(); )
			{
				Variable var = (Variable)vIt.next();
				if (!var.isDisplay()) continue;
				accumulateTextFound(pp, var, null, change);
			}
		}

		// text on the cell
		for(Iterator vIt = cell.getVariables(); vIt.hasNext(); )
		{
			Variable var = (Variable)vIt.next();
			if (!var.isDisplay()) continue;
			accumulateTextFound(cell, var, null, change);
		}
	}

	/**
	 * Method to process a relevant piece of text.
	 * @param eObj the ElectricObject on which the text resides.
	 * @param var the Variable on which the text resides (may be null).
	 * @param name the Name object of the text (for Node and Arc names).
	 * @param change true to change the text the cell according to the bottom of the dialog;
	 * false to gather the text sizes for display.
	 */
	private void accumulateTextFound(ElectricObject eObj, Variable var, Name name, boolean change)
	{
		if (var != null)
		{
			// a variable on an object
			if (eObj instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)eObj;
				if (ni.getProto() == Generic.tech.invisiblePinNode)
				{
					if (changeAnnotationText.isSelected())
					{
						if (processText(var.getTextDescriptor(), change))
							numAnnotationsChanged++;
					}
				} else
				{
					if (changeNodeText.isSelected())
					{
						if (processText(var.getTextDescriptor(), change))
							numNodesChanged++;
					}
				}
			} else if (eObj instanceof ArcInst)
			{
				if (changeArcText.isSelected())
				{
					if (processText(var.getTextDescriptor(), change))
						numArcsChanged++;
				}
			} else if (eObj instanceof Cell)
			{
				if (changeCellText.isSelected())
				{
					if (processText(var.getTextDescriptor(), change))
						numCellsChanged++;
				}
			}
		} else if (name != null)
		{
			if (eObj instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)eObj;
				if (changeNodeText.isSelected())
				{
					if (processText(ni.getNameTextDescriptor(), change))
						numNodesChanged++;
				}
			} else
			{
				ArcInst ai = (ArcInst)eObj;
				if (changeArcText.isSelected())
				{
					if (processText(ai.getNameTextDescriptor(), change))
						numNodesChanged++;
				}
			}
		} else
		{
			if (eObj instanceof Export)
			{
				Export pp = (Export)eObj;
				if (changeExportText.isSelected())
				{
					if (processText(pp.getTextDescriptor(), change))
						numExportsChanged++;
				}
			} else
			{
				NodeInst ni = (NodeInst)eObj;
				if (changeInstanceText.isSelected())
				{
					if (processText(ni.getProtoTextDescriptor(), change))
						numInstancesChanged++;
				}
			}
		}
	}

	/**
	 * Method to process a single TextDescriptor that is on a relevant piece of text.
	 * @param td the TextDescriptor to process.
	 * @param change true to change the TextDescriptor according to the bottom of the dialog;
	 * false to gather the TextDescriptor sizes for display.
	 * @return true if a change was made.
	 */
	private boolean processText(TextDescriptor td, boolean change)
	{
		TextDescriptor.Size s = td.getSize();
		boolean changed = false;
		if (change)
		{
			// change this text
			if (usePoints.isSelected())
			{
				int size = TextUtils.atoi(pointSize.getText());
				if (!s.isAbsolute() || s.getSize() != size)
				{
					td.setAbsSize(size);
					changed = true;
				}
			} else
			{
				double size = TextUtils.atof(unitSize.getText());
				if (s.isAbsolute() || s.getSize() != size)
				{
					td.setRelSize(size);
					changed = true;
				}
			}
			int fontIndex = 0;
			if (font.getSelectedIndex() != 0)
			{
				String nameOfFont = (String)font.getSelectedItem();
				TextDescriptor.ActiveFont newFont = TextDescriptor.ActiveFont.findActiveFont(nameOfFont);
				fontIndex = newFont.getIndex();
			}
			if (fontIndex != td.getFace())
			{
				td.setFace(fontIndex);
				changed = true;
			}
			if (bold.isSelected() != td.isBold())
			{
				if (td.isBold()) td.clearBold(); else
					td.setBold();
				changed = true;
			}
			if (italic.isSelected() != td.isItalic())
			{
				if (td.isItalic()) td.clearItalic(); else
					td.setItalic();
				changed = true;
			}
			if (underline.isSelected() != td.isUnderline())
			{
				if (td.isUnderline()) td.clearUnderline(); else
					td.setUnderline();
				changed = true;
			}
			return changed;
		}

		// accumulate text to list the range of sizes
		double size = s.getSize();
		if (numToChange == 0)
		{
			if (s.isAbsolute())
			{
				lowPointSize = highPointSize = (int)size;
			} else
			{
				lowUnitSize = highUnitSize = size;
			}
		} else
		{
			if (s.isAbsolute())
			{
				if ((int)size < lowPointSize) lowPointSize = (int)size;
				if ((int)size > highPointSize) highPointSize = (int)size;
			} else
			{
				if (size < lowUnitSize) lowUnitSize = size;
				if (size > highUnitSize) highUnitSize = size;
			}
		}
		numToChange++;
		return false;
	}

	private static class ChangeTextSizes extends Job
	{
		ChangeText dialog;

		protected ChangeTextSizes(ChangeText dialog)
		{
			super("Change Text Size", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.dialog = dialog;
			startJob();
		}

		public boolean doIt()
		{
			dialog.numNodesChanged = dialog.numArcsChanged = dialog.numExportsChanged = 0;
			dialog.numAnnotationsChanged = dialog.numInstancesChanged = dialog.numCellsChanged = 0;
			dialog.findSelectedText(true);
			if (dialog.numNodesChanged != 0 || dialog.numArcsChanged != 0 ||
				dialog.numExportsChanged != 0 || dialog.numAnnotationsChanged != 0 ||
				dialog.numInstancesChanged != 0 || dialog.numCellsChanged != 0)
			{
				String what = "Changed text on";
				boolean others = false;
				if (dialog.numNodesChanged != 0)
				{
					what += " " + dialog.numNodesChanged + " nodes";
					others = true;
				}
				if (dialog.numArcsChanged != 0)
				{
					if (others) what += ", ";
					what += " " + dialog.numArcsChanged + " arcs";
					others = true;
				}
				if (dialog.numExportsChanged != 0)
				{
					if (others) what += ", ";
					what += " " + dialog.numExportsChanged + " exports";
					others = true;
				}
				if (dialog.numAnnotationsChanged != 0)
				{
					if (others) what += ", ";
					what += " " + dialog.numAnnotationsChanged + " annotations";
					others = true;
				}
				if (dialog.numInstancesChanged != 0)
				{
					if (others) what += ", ";
					what += " " + dialog.numInstancesChanged + " instances";
					others = true;
				}
				if (dialog.numCellsChanged != 0)
				{
					if (others) what += ", ";
					what += " " + dialog.numCellsChanged + " cells";
					others = true;
				}
				System.out.println(what);					
			}
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

        sizeGroup = new javax.swing.ButtonGroup();
        whereGroup = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        changeNodeText = new javax.swing.JCheckBox();
        changeArcText = new javax.swing.JCheckBox();
        changeExportText = new javax.swing.JCheckBox();
        changeAnnotationText = new javax.swing.JCheckBox();
        changeInstanceText = new javax.swing.JCheckBox();
        changeCellText = new javax.swing.JCheckBox();
        selectedText = new javax.swing.JLabel();
        changeSelectedObjects = new javax.swing.JRadioButton();
        changeAllInCell = new javax.swing.JRadioButton();
        changeCellsWithView = new javax.swing.JRadioButton();
        viewList = new javax.swing.JComboBox();
        changeAllInLibrary = new javax.swing.JRadioButton();
        jPanel2 = new javax.swing.JPanel();
        pointSize = new javax.swing.JTextField();
        unitSize = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        usePoints = new javax.swing.JRadioButton();
        useUnits = new javax.swing.JRadioButton();
        jLabel3 = new javax.swing.JLabel();
        font = new javax.swing.JComboBox();
        bold = new javax.swing.JCheckBox();
        italic = new javax.swing.JCheckBox();
        underline = new javax.swing.JCheckBox();
        cancel = new javax.swing.JButton();
        ok = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Change Text Size");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel1.setBorder(new javax.swing.border.TitledBorder("What to Change"));
        changeNodeText.setText("Change size of node text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanel1.add(changeNodeText, gridBagConstraints);

        changeArcText.setText("Change size of arc text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanel1.add(changeArcText, gridBagConstraints);

        changeExportText.setText("Change size of export text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanel1.add(changeExportText, gridBagConstraints);

        changeAnnotationText.setText("Change size of annotation text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanel1.add(changeAnnotationText, gridBagConstraints);

        changeInstanceText.setText("Change size of instance name text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanel1.add(changeInstanceText, gridBagConstraints);

        changeCellText.setText("Change size of cell text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        jPanel1.add(changeCellText, gridBagConstraints);

        selectedText.setText("No text to change");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(selectedText, gridBagConstraints);

        changeSelectedObjects.setText("Change only selected objects");
        whereGroup.add(changeSelectedObjects);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanel1.add(changeSelectedObjects, gridBagConstraints);

        changeAllInCell.setText("Change all in this cell");
        whereGroup.add(changeAllInCell);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanel1.add(changeAllInCell, gridBagConstraints);

        changeCellsWithView.setText("Change all cells with view:");
        whereGroup.add(changeCellsWithView);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanel1.add(changeCellsWithView, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 20, 2, 4);
        jPanel1.add(viewList, gridBagConstraints);

        changeAllInLibrary.setText("Change all in this library");
        whereGroup.add(changeAllInLibrary);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanel1.add(changeAllInLibrary, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(jPanel1, gridBagConstraints);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jPanel2.setBorder(new javax.swing.border.TitledBorder("How to Change it"));
        pointSize.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel2.add(pointSize, gridBagConstraints);

        unitSize.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        jPanel2.add(unitSize, gridBagConstraints);

        jLabel2.setText("Size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        jPanel2.add(jLabel2, gridBagConstraints);

        usePoints.setText("Points (max 63)");
        sizeGroup.add(usePoints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(usePoints, gridBagConstraints);

        useUnits.setText("Units (max 127.75)");
        sizeGroup.add(useUnits);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(useUnits, gridBagConstraints);

        jLabel3.setText("Font:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        jPanel2.add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add(font, gridBagConstraints);

        bold.setText("Bold");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        jPanel2.add(bold, gridBagConstraints);

        italic.setText("Italic");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        jPanel2.add(italic, gridBagConstraints);

        underline.setText("Underline");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        jPanel2.add(underline, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(jPanel2, gridBagConstraints);

        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cancelActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
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
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void okActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_okActionPerformed
	{//GEN-HEADEREND:event_okActionPerformed
		ChangeTextSizes job = new ChangeTextSizes(this);
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
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox bold;
    private javax.swing.JButton cancel;
    private javax.swing.JRadioButton changeAllInCell;
    private javax.swing.JRadioButton changeAllInLibrary;
    private javax.swing.JCheckBox changeAnnotationText;
    private javax.swing.JCheckBox changeArcText;
    private javax.swing.JCheckBox changeCellText;
    private javax.swing.JRadioButton changeCellsWithView;
    private javax.swing.JCheckBox changeExportText;
    private javax.swing.JCheckBox changeInstanceText;
    private javax.swing.JCheckBox changeNodeText;
    private javax.swing.JRadioButton changeSelectedObjects;
    private javax.swing.JComboBox font;
    private javax.swing.JCheckBox italic;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JButton ok;
    private javax.swing.JTextField pointSize;
    private javax.swing.JLabel selectedText;
    private javax.swing.ButtonGroup sizeGroup;
    private javax.swing.JCheckBox underline;
    private javax.swing.JTextField unitSize;
    private javax.swing.JRadioButton usePoints;
    private javax.swing.JRadioButton useUnits;
    private javax.swing.JComboBox viewList;
    private javax.swing.ButtonGroup whereGroup;
    // End of variables declaration//GEN-END:variables
}
