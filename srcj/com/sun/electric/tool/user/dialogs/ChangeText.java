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
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.DisplayedText;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.MutableTextDescriptor;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.CircuitChangeJobs;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.Iterator;

/**
 * Class to handle the "Change Text" dialog.
 */
public class ChangeText extends EDialog
{
	private static boolean lastNodesSelected = false;
	private static boolean lastArcsSelected = false;
	private static boolean lastExportsSelected = false;
	private static boolean lastAnnotationsSelected = false;
	private static boolean lastInstancesSelected = false;
	private static boolean lastCellsSelected = false;

	private ChangeParameters cp;
	private Cell cell;
    private EditWindow wnd;

    private static class ChangeParameters implements Serializable
    {
    	// which types of text are to be changed
    	private boolean nodesSelected;
    	private boolean arcsSelected;
    	private boolean exportsSelected;
    	private boolean annotationsSelected;
    	private boolean instancesSelected;
    	private boolean cellsSelected;

    	// how much text is to be changed
    	private boolean changeSelectedObjects;
    	private boolean changeAllInCell;
    	private boolean changeCellsWithView;
    	private boolean changeAllInLibrary;

    	// the type of changes to be made
    	private boolean usePoints;
    	private String pointSize;
    	private String unitSize;
    	private int selectedFontIndex;
    	private String selectedFontName;
    	private boolean isBold, isItalic, isUnderline;
    	private String viewListSelection;

    	// statistics on the existing text
    	private int numToChange;
    	private int lowPointSize, highPointSize;
    	private double lowUnitSize, highUnitSize;
    	private int numNodesChanged, numArcsChanged, numExportsChanged;
    	private int numAnnotationsChanged, numInstancesChanged, numCellsChanged;
    }

	private void gatherTextChoices()
	{
		// update which types of text are to be changed
		lastNodesSelected = cp.nodesSelected = changeNodeText.isSelected();
		lastArcsSelected = cp.arcsSelected = changeArcText.isSelected();
		lastExportsSelected  = cp.exportsSelected = changeExportText.isSelected();
		lastAnnotationsSelected = cp.annotationsSelected = changeAnnotationText.isSelected();
		lastInstancesSelected = cp.instancesSelected = changeInstanceText.isSelected();
		lastCellsSelected = cp.cellsSelected = changeCellText.isSelected();

		// update how much text is to be changed
    	cp.changeSelectedObjects = changeSelectedObjects.isSelected();
    	cp.changeAllInCell = changeAllInCell.isSelected();
    	cp.changeCellsWithView = changeCellsWithView.isSelected();
    	cp.changeAllInLibrary = changeAllInLibrary.isSelected();

    	// update the type of changes to be made
    	cp.usePoints = usePoints.isSelected();
    	cp.pointSize = pointSize.getText();
    	cp.unitSize = unitSize.getText();
    	cp.selectedFontIndex = font.getSelectedIndex();
    	cp.selectedFontName = (String)font.getSelectedItem();
    	cp.isBold = bold.isSelected();
    	cp.isItalic = italic.isSelected();
    	cp.isUnderline = underline.isSelected();
    	cp.viewListSelection = (String)viewList.getSelectedItem();
	}

    public static void changeTextDialog()
	{
		ChangeText dialog = new ChangeText(TopLevel.getCurrentJFrame(), true);
		dialog.setVisible(true);
	}

	/** Creates new form Change Text */
	private ChangeText(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();

		getRootPane().setDefaultButton(ok);
		useUnits.setSelected(true);
		changeNodeText.setSelected(lastNodesSelected);
		changeArcText.setSelected(lastArcsSelected);
		changeExportText.setSelected(lastExportsSelected);
		changeAnnotationText.setSelected(lastAnnotationsSelected);
		changeInstanceText.setSelected(lastInstancesSelected);
		changeCellText.setSelected(lastCellsSelected);

		cell = WindowFrame.getCurrentCell();
        wnd = EditWindow.getCurrent();
		for(View view : View.getOrderedViews())
		{
			viewList.addItem(view.getFullName());
		}

		font.addItem("DEFAULT FONT");
		Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
		for(int i=0; i<fonts.length; i++)
			font.addItem(fonts[i].getFontName());

		if ((wnd == null) || (wnd.getHighlighter().getNumHighlights() == 0))
		{
			changeSelectedObjects.setEnabled(false);
			changeAllInCell.setSelected(true);
		} else
		{
			changeSelectedObjects.setSelected(true);
		}
		cp = new ChangeParameters();
		gatherTextChoices();   findSelectedText();

		changeSelectedObjects.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { gatherTextChoices();   findSelectedText(); }
		});
		changeAllInCell.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { gatherTextChoices();   findSelectedText(); }
		});
		changeCellsWithView.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { gatherTextChoices();   findSelectedText(); }
		});
		viewList.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { gatherTextChoices();   findSelectedText(); }
		});
		changeAllInLibrary.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { gatherTextChoices();   findSelectedText(); }
		});

		changeNodeText.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { findSelectedText(); }
		});
		changeArcText.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { findSelectedText(); }
		});
		changeExportText.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { findSelectedText(); }
		});
		changeAnnotationText.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { findSelectedText(); }
		});
		changeInstanceText.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { findSelectedText(); }
		});
		changeCellText.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { findSelectedText(); }
		});
		finishInitialization();
	}

	protected void escapePressed() { cancelActionPerformed(null); }

	/**
	 * Method to scan for all relevant text.
	 * This looks at the top part of the dialog to figure out which text is relevant.
	 * @param change true to change the relevant text according to the bottom of the dialog;
	 * false to gather the relevant text sizes for display.
	 */
	private void findSelectedText()
	{
		gatherTextChoices();
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;
		cp.numToChange = 0;
		cp.lowPointSize = -1;
		cp.lowUnitSize = -1;
		if (cp.changeSelectedObjects)
		{
            EditWindow wnd = EditWindow.getCurrent();
            if (wnd != null)
            {
                for(DisplayedText dt : wnd.getHighlighter().getHighlightedText(false))
                {
                    accumulateTextFound(cp, dt.getElectricObject(), dt.getVariableKey(), false);
                }
            }
		} else if (cp.changeAllInCell)
		{
			findAllInCell(cp, cell, false);
		} else if (cp.changeCellsWithView)
		{
			View v = View.findView(cp.viewListSelection);
			if (v != null)
			{
				for(Iterator<Cell> it = Library.getCurrent().getCells(); it.hasNext(); )
				{
					Cell c = it.next();
					if (c.getView() == v) findAllInCell(cp, c, false);
				}
			}
		} else if (cp.changeAllInLibrary)
		{
			for(Iterator<Cell> it = Library.getCurrent().getCells(); it.hasNext(); )
			{
				Cell c = it.next();
				findAllInCell(cp, c, false);
			}
		}
		if (cp.numToChange == 0)
		{
			selectedText.setText("No text to change");
		} else
		{
			String what = "Text runs from ";
			if (cp.lowPointSize >= 0) what += cp.lowPointSize + " to " + cp.highPointSize + " points";
			if (cp.lowUnitSize >= 0)
			{
				if (cp.lowPointSize >= 0) what += "; ";
				what += cp.lowUnitSize + " to " + cp.highUnitSize + " units";
			}
			selectedText.setText(what);
			if (cp.lowUnitSize >= 0)
			{
				useUnits.setSelected(true);
				unitSize.setText(TextUtils.formatDouble(cp.highUnitSize));
			} else
			{
				usePoints.setSelected(true);
				pointSize.setText(Integer.toString(cp.highPointSize));
			}
		}
	}

	/**
	 * Method to scan for all relevant text.
	 * This looks at the top part of the dialog to figure out which text is relevant.
	 * @param change true to change the relevant text according to the bottom of the dialog;
	 * false to gather the relevant text sizes for display.
	 */
	private static void changeSelectedText(Cell cell, ChangeParameters cp)
	{
		if (cp.changeSelectedObjects)
		{
			// make sure text adjustment is allowed
			if (CircuitChangeJobs.cantEdit(cell, null, true) != 0) return;

            EditWindow wnd = EditWindow.getCurrent();
            if (wnd != null)
            {
                for(DisplayedText dt : wnd.getHighlighter().getHighlightedText(false))
                {
                    accumulateTextFound(cp, dt.getElectricObject(), dt.getVariableKey(), true);
                }
            }
		} else if (cp.changeAllInCell)
		{
			findAllInCell(cp, cell, true);
		} else if (cp.changeCellsWithView)
		{
			View v = View.findView(cp.viewListSelection);
			if (v != null)
			{
				for(Iterator<Cell> it = Library.getCurrent().getCells(); it.hasNext(); )
				{
					Cell c = it.next();
					if (c.getView() == v) findAllInCell(cp, c, true);
				}
			}
		} else if (cp.changeAllInLibrary)
		{
			for(Iterator<Cell> it = Library.getCurrent().getCells(); it.hasNext(); )
			{
				Cell c = it.next();
				findAllInCell(cp, c, true);
			}
		}
	}

	/**
	 * Method to grab all text in a Cell and process it.
	 * @param cell the cell to examine.
	 * @param change true to change the text in the cell according to the bottom of the dialog;
	 * false to gather the text sizes in the cell for display.
	 */
	private static void findAllInCell(ChangeParameters cp, Cell cell, boolean change)
	{
		// make sure text adjustment is allowed
		if (change)
		{
			if (CircuitChangeJobs.cantEdit(cell, null, true) != 0) return;
		}

		// text on nodes
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (ni.isCellInstance() && !ni.isExpanded())
			{
				// cell instance text
				accumulateTextFound(cp, ni, NodeInst.NODE_PROTO, change);
			}
			if (!ni.getNameKey().isTempname())
			{
				// node name
				accumulateTextFound(cp, ni, NodeInst.NODE_NAME, change);
			}
			for(Iterator<Variable> vIt = ni.getVariables(); vIt.hasNext(); )
			{
				Variable var = vIt.next();
				if (!var.isDisplay()) continue;
				accumulateTextFound(cp, ni, var.getKey(), change);
			}
		}

		// text on arcs
		for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = it.next();
			if (!ai.getNameKey().isTempname())
			{
				// arc name
				accumulateTextFound(cp, ai, ArcInst.ARC_NAME, change);
			}
			for(Iterator<Variable> vIt = ai.getVariables(); vIt.hasNext(); )
			{
				Variable var = vIt.next();
				if (!var.isDisplay()) continue;
				accumulateTextFound(cp, ai, var.getKey(), change);
			}
		}

		// text on exports
		for(Iterator<PortProto> it = cell.getPorts(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			accumulateTextFound(cp, pp, Export.EXPORT_NAME, change);
			for(Iterator<Variable> vIt = pp.getVariables(); vIt.hasNext(); )
			{
				Variable var = vIt.next();
				if (!var.isDisplay()) continue;
				accumulateTextFound(cp, pp, var.getKey(), change);
			}
		}

		// text on the cell
		for(Iterator<Variable> vIt = cell.getVariables(); vIt.hasNext(); )
		{
			Variable var = vIt.next();
			if (!var.isDisplay()) continue;
			accumulateTextFound(cp, cell, var.getKey(), change);
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
	private static void accumulateTextFound(ChangeParameters cp, ElectricObject eObj, Variable.Key varKey, boolean change)
	{
		if (eObj instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)eObj;
			if (ni.getProto() == Generic.tech.invisiblePinNode)
			{
				if (cp.annotationsSelected)
				{
					if (processText(cp, eObj, varKey, change))
						cp.numAnnotationsChanged++;
				}
			} else if (varKey == NodeInst.NODE_PROTO)
			{
				if (cp.instancesSelected)
				{
					if (processText(cp, eObj, varKey, change))
						cp.numInstancesChanged++;
				}
			} else
			{
				if (cp.nodesSelected)
				{
					if (processText(cp, eObj, varKey, change))
						cp.numNodesChanged++;
				}
			}
		} else if (eObj instanceof ArcInst)
		{
			if (cp.arcsSelected)
			{
				if (processText(cp, eObj, varKey, change))
					cp.numArcsChanged++;
			}
		} else if (eObj instanceof Cell)
		{
			if (cp.cellsSelected)
			{
				if (processText(cp, eObj, varKey, change))
					cp.numCellsChanged++;
			}
		} else if (eObj instanceof Export)
		{
			if (cp.exportsSelected)
			{
				if (processText(cp, eObj, varKey, change))
					cp.numExportsChanged++;
			}
		}
	}

	/**
	 * Method to process a single TextDescriptor that is on a relevant piece of text.
	 * @param cp the ChangeParameters with information about how to handle the text.
	 * @param owner ElectricObject which is owner of the TextDescriptor
	 * @param varKey key of variable or speical key selecting TextDescriptor
	 * @param change true to change the TextDescriptor according to the bottom of the dialog;
	 * false to gather the TextDescriptor sizes for display.
	 * @return true if a change was made.
	 */
	private static boolean processText(ChangeParameters cp, ElectricObject owner, Variable.Key varKey, boolean change)
	{
		TextDescriptor.Size s = owner.getTextDescriptor(varKey).getSize();
		if (change)
		{
			// change this text
			boolean changed = false;
			MutableTextDescriptor td = owner.getMutableTextDescriptor(varKey);
			if (cp.usePoints)
			{
				int size = TextUtils.atoi(cp.pointSize);
				if (!s.isAbsolute() || s.getSize() != size)
				{
					td.setAbsSize(size);
					changed = true;
				}
			} else
			{
				double size = TextUtils.atof(cp.unitSize);
				if (s.isAbsolute() || s.getSize() != size)
				{
					td.setRelSize(size);
					changed = true;
				}
			}
			int fontIndex = 0;
			if (cp.selectedFontIndex != 0)
			{
				TextDescriptor.ActiveFont newFont = TextDescriptor.ActiveFont.findActiveFont(cp.selectedFontName);
                if (newFont != null)
                    fontIndex = newFont.getIndex();
			}
			if (fontIndex != td.getFace())
			{
				td.setFace(fontIndex);
				changed = true;
			}
			if (cp.isBold != td.isBold())
			{
                td.setBold(!td.isBold());
				changed = true;
			}
			if (cp.isItalic != td.isItalic())
			{
                td.setItalic(!td.isItalic());
				changed = true;
			}
			if (cp.isUnderline != td.isUnderline())
			{
                td.setUnderline(!td.isUnderline());
				changed = true;
			}
			if (changed)
				owner.setTextDescriptor(varKey, TextDescriptor.newTextDescriptor(td));
			return changed;
		}

		// accumulate information to list the range of sizes
		double size = s.getSize();
		if (cp.numToChange == 0)
		{
			if (s.isAbsolute())
			{
				cp.lowPointSize = cp.highPointSize = (int)size;
			} else
			{
				cp.lowUnitSize = cp.highUnitSize = size;
			}
		} else
		{
			if (s.isAbsolute())
			{
				if ((int)size < cp.lowPointSize) cp.lowPointSize = (int)size;
				if ((int)size > cp.highPointSize) cp.highPointSize = (int)size;
			} else
			{
				if (size < cp.lowUnitSize) cp.lowUnitSize = size;
				if (size > cp.highUnitSize) cp.highUnitSize = size;
			}
		}
		cp.numToChange++;
		return false;
	}

	private static class ChangeTextSizes extends Job
	{
		private Cell cell;
		private ChangeParameters cp;

		private ChangeTextSizes(Cell cell, ChangeParameters cp)
		{
			super("Change Text Size", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.cp = cp;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			cp.numNodesChanged = cp.numArcsChanged = cp.numExportsChanged = 0;
			cp.numAnnotationsChanged = cp.numInstancesChanged = cp.numCellsChanged = 0;
			changeSelectedText(cell, cp);
			if (cp.numNodesChanged != 0 || cp.numArcsChanged != 0 ||
				cp.numExportsChanged != 0 || cp.numAnnotationsChanged != 0 ||
				cp.numInstancesChanged != 0 || cp.numCellsChanged != 0)
			{
				String what = "Changed text on";
				boolean others = false;
				if (cp.numNodesChanged != 0)
				{
					what += " " + cp.numNodesChanged + " nodes";
					others = true;
				}
				if (cp.numArcsChanged != 0)
				{
					if (others) what += ", ";
					what += " " + cp.numArcsChanged + " arcs";
					others = true;
				}
				if (cp.numExportsChanged != 0)
				{
					if (others) what += ", ";
					what += " " + cp.numExportsChanged + " exports";
					others = true;
				}
				if (cp.numAnnotationsChanged != 0)
				{
					if (others) what += ", ";
					what += " " + cp.numAnnotationsChanged + " annotations";
					others = true;
				}
				if (cp.numInstancesChanged != 0)
				{
					if (others) what += ", ";
					what += " " + cp.numInstancesChanged + " instances";
					others = true;
				}
				if (cp.numCellsChanged != 0)
				{
					if (others) what += ", ";
					what += " " + cp.numCellsChanged + " cells";
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
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
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
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
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

        whereGroup.add(changeSelectedObjects);
        changeSelectedObjects.setText("Change only selected objects");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanel1.add(changeSelectedObjects, gridBagConstraints);

        whereGroup.add(changeAllInCell);
        changeAllInCell.setText("Change all in this cell");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanel1.add(changeAllInCell, gridBagConstraints);

        whereGroup.add(changeCellsWithView);
        changeCellsWithView.setText("Change all cells with view:");
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

        whereGroup.add(changeAllInLibrary);
        changeAllInLibrary.setText("Change all in this library");
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

        sizeGroup.add(usePoints);
        usePoints.setText("Points (max 63)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(usePoints, gridBagConstraints);

        sizeGroup.add(useUnits);
        useUnits.setText("Units (max 127.75)");
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
        cancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(cancel, gridBagConstraints);

        ok.setText("OK");
        ok.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);

        pack();
    }
    // </editor-fold>//GEN-END:initComponents

	private void okActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_okActionPerformed
	{//GEN-HEADEREND:event_okActionPerformed
		Cell cell = WindowFrame.needCurCell();
		if (cell == null) return;
		gatherTextChoices();
		ChangeTextSizes job = new ChangeTextSizes(cell, cp);
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
