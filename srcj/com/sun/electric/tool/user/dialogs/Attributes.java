/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Attributes.java
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

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListModel;
import javax.swing.JRadioButton;


/**
 * Class to handle the "Attributes" dialog.
 */
public class Attributes extends javax.swing.JDialog
{
	private static Attributes theDialog = null;
	private DefaultListModel listModel;
	private JList list;
	private ElectricObject selectedObject;
	private Cell selectedCell;
	private NodeInst selectedNode;
	private ArcInst selectedArc;
	private Export selectedExport;
	private PortInst selectedPort;
	private JRadioButton currentButton;

	private String initialName;
	private String initialValue;
	private int initialCode;
	private TextDescriptor.Unit initialUnit;
	private int initialDisplayIndex;
	private boolean initialIsParameter;
	private boolean initialInherit;
	private TextDescriptor.Size initialSize;
	private double initialOffX, initialOffY;
	private boolean initialItalic, initialBold, initialUnderline;
	private TextDescriptor.Rotation initialRotation;
	private TextDescriptor.Position initialPosition;
	private int initialFont;

	/**
	 * Method to show the Attributes dialog.
	 */
	public static void showDialog()
	{
		if (theDialog == null)
		{
			JFrame jf = TopLevel.getCurrentJFrame();
			theDialog = new Attributes(jf, false);
		}
		theDialog.show();
	}

	/**
	 * Method to reload the Attributes dialog from the current highlighting.
	 */
	public static void load()
	{
		if (theDialog == null) return;
		theDialog.loadAttributesInfo();
	}

	/**
	 * Creates new form Attributes.
	 */
	private Attributes(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		setLocation(100, 50);
		initComponents();

		// load "show" popup
		language.addItem("Not Code");
		language.addItem("Java");

		// load the "Units" popup
		int numUnits = TextDescriptor.Unit.getNumUnits();
		for(int i=0; i<numUnits; i++)
		{
			TextDescriptor.Unit tdu = TextDescriptor.Unit.getUnitAt(i);
			units.addItem(tdu.getDescription());
		}

		// load the "Show" popup
		show.addItem("Nothing");
		int numStyles = TextDescriptor.DispPos.getNumShowStyles();
		for(int i=0; i<numStyles; i++)
		{
			TextDescriptor.DispPos tdp = TextDescriptor.DispPos.getShowStylesAt(i);
			show.addItem(tdp.getName());
		}

		// load the "Fonts" popup
		font.addItem("DEFAULT FONT");
		Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
		for(int i=0; i<fonts.length; i++)
			font.addItem(fonts[i].getFontName());

		// load the "Rotation" popup
		int numRotations = TextDescriptor.Rotation.getNumRotations();
		for(int i=0; i<numRotations; i++)
		{
			TextDescriptor.Rotation tdr = TextDescriptor.Rotation.getRotationAt(i);
			rotation.addItem(tdr.getDescription());
		}

		textIconCenter.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabCenter.gif")));
		textIconLeft.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabLeft.gif")));
		textIconRight.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabRight.gif")));
		textIconTop.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabTop.gif")));
		textIconBottom.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabBottom.gif")));
		textIconLowerRight.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabLowerRight.gif")));
		textIconLowerLeft.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabLowerLeft.gif")));
		textIconUpperRight.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabUpperRight.gif")));
		textIconUpperLeft.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabUpperLeft.gif")));

		// make the list
		listModel = new DefaultListModel();
		list = new JList(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listPane.setViewportView(list);
		list.addMouseListener(new java.awt.event.MouseAdapter()
		{
			public void mouseClicked(java.awt.event.MouseEvent evt) { listClick(); }
		});

		// have the radio buttons at the top reevaluate
		currentCell.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { objectSelectorActionPerformed(evt); }
		});
		currentNode.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { objectSelectorActionPerformed(evt); }
		});
		currentArc.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { objectSelectorActionPerformed(evt); }
		});
		currentExport.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { objectSelectorActionPerformed(evt); }
		});
		currentPort.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { objectSelectorActionPerformed(evt); }
		});

		currentPort.setEnabled(false);

		loadAttributesInfo();
	}

	/**
	 * Method called when the user clicks on one of the top radio buttons.
	 * Changes the object being examined for attributes.
	 */
	private void objectSelectorActionPerformed(ActionEvent evt)
	{
		currentButton = (JRadioButton)evt.getSource();
		if (currentButton == currentCell) selectedObject = selectedCell;
		else if (currentButton == currentNode) selectedObject = selectedNode;
		else if (currentButton == currentArc) selectedObject = selectedArc;
		else if (currentButton == currentExport) selectedObject = selectedExport;
		else if (currentButton == currentPort) selectedObject = selectedPort;
		showAttributesOnSelectedObject(null);
	}

	/**
	 * Method called when the user clicks in the list of attribute names.
	 */
	private void listClick()
	{
		showSelectedAttribute();
	}

	/**
	 * Method to reload the entire dialog from the current highlighting.
	 */
	private void loadAttributesInfo()
	{
		// determine what attributes can be set
		selectedObject = null;
		selectedCell = null;
		selectedNode = null;
		selectedArc = null;
		selectedExport = null;
		selectedPort = null;
		Variable selectedVar = null;

		currentButton = currentCell;
		selectedCell = Library.needCurCell();   selectedObject = selectedCell;
		if (selectedCell != null)
		{
			if (Highlight.getNumHighlights() == 1)
			{
				Highlight high = (Highlight)Highlight.getHighlights().next();
				ElectricObject eobj = high.getElectricObject();
				selectedVar = high.getVar();
				if (high.getType() == Highlight.Type.EOBJ)
				{
					if (eobj instanceof ArcInst)
					{
						selectedArc = (ArcInst)eobj;   selectedObject = selectedArc;   currentButton = currentArc;
					} else if (eobj instanceof NodeInst)
					{
						selectedNode = (NodeInst)eobj;   selectedObject = selectedNode;   currentButton = currentNode;
					} else if (eobj instanceof PortInst)
					{
						PortInst pi = (PortInst)eobj;
						selectedNode = (NodeInst)pi.getNodeInst();   selectedObject = selectedNode;   currentButton = currentNode;
						selectedPort = pi;
					}
				} else if (high.getType() == Highlight.Type.TEXT)
				{
					if (selectedVar != null)
					{
						if (eobj instanceof NodeInst)
						{
							selectedNode = (NodeInst)eobj;   selectedObject = selectedNode;   currentButton = currentNode;
						} else if (eobj instanceof ArcInst)
						{
							selectedArc = (ArcInst)eobj;   selectedObject = selectedArc;   currentButton = currentArc;
						} else if (eobj instanceof PortInst)
						{
							selectedPort = (PortInst)eobj;   selectedObject = selectedPort;   currentButton = currentPort;
							selectedNode = selectedPort.getNodeInst();
						} else if (eobj instanceof Export)
						{
							selectedExport = (Export)eobj;   selectedObject = selectedExport;   currentButton = currentExport;
						}
					} else if (high.getName() != null)
					{
						// node or arc name
						if (eobj instanceof NodeInst)
						{
							// node name
							selectedNode = (NodeInst)eobj;   selectedObject = selectedNode;   currentButton = currentNode;
						} else if (eobj instanceof ArcInst)
						{
							// arc variable
							selectedArc = (ArcInst)eobj;   selectedObject = selectedArc;   currentButton = currentArc;
						}
					} else if (eobj instanceof Export)
					{
						selectedExport = (Export)eobj;   selectedObject = selectedExport;   currentButton = currentExport;
					}
				}
			}
		}

		// show initial values in the dialog
		if (selectedCell == null)
		{
			// nothing can be done: dim the entire dialog
			currentCell.setEnabled(false);
			cellName.setText("NO CURRENT CELL");
			currentNode.setEnabled(false);
			currentArc.setEnabled(false);
			currentExport.setEnabled(false);
			currentPort.setEnabled(false);
			list.clearSelection();
			listModel.clear();
			name.setText("");
			name.setEditable(false);
			value.setText("");
			value.setEditable(false);
			language.setEnabled(false);
			evaluation.setText("");
			units.setEnabled(false);
			show.setEnabled(false);
			isParameter.setEnabled(false);
			instancesInherit.setEnabled(false);
			pointsSize.setEditable(false);
			pointsButton.setEnabled(false);
			unitsSize.setEditable(false);
			unitsButton.setEnabled(false);
			xOffset.setText("");
			xOffset.setEditable(false);
			yOffset.setText("");
			yOffset.setEditable(false);
			italic.setEnabled(false);
			bold.setEnabled(false);
			underline.setEnabled(false);
			font.setEnabled(false);
			rotation.setEnabled(false);
			center.setEnabled(false);
			bottom.setEnabled(false);
			top.setEnabled(false);
			left.setEnabled(false);
			right.setEnabled(false);
			lowerRight.setEnabled(false);
			lowerLeft.setEnabled(false);
			upperRight.setEnabled(false);
			upperLeft.setEnabled(false);
			rename.setEnabled(false);
			deleteButton.setEnabled(false);
			apply.setEnabled(false);
			return;
		}

		// enable the dialog
		currentCell.setEnabled(true);
		currentNode.setEnabled(selectedNode != null);
		currentArc.setEnabled(selectedArc != null);
		currentExport.setEnabled(selectedExport != null);
		currentPort.setEnabled(selectedPort != null);
		currentButton.setSelected(true);
		cellName.setText(selectedCell.describe());

		if (currentCell.isSelected() || currentExport.isSelected())
			instancesInherit.setEnabled(true); else
		{
			instancesInherit.setSelected(false);
			instancesInherit.setEnabled(false);
		}
		if (currentCell.isSelected() || currentNode.isSelected())
			isParameter.setEnabled(false); else
		{
			isParameter.setSelected(false);
			isParameter.setEnabled(true);
		}
		name.setEditable(true);
		value.setEditable(true);
		language.setEnabled(true);
		units.setEnabled(true);
		show.setEnabled(true);
		pointsSize.setEditable(true);
		pointsButton.setEnabled(true);
		unitsSize.setEditable(true);
		unitsButton.setEnabled(true);
		xOffset.setEditable(true);
		yOffset.setEditable(true);
		italic.setEnabled(true);
		bold.setEnabled(true);
		underline.setEnabled(true);
		font.setEnabled(true);
		rotation.setEnabled(true);
		center.setEnabled(true);
		bottom.setEnabled(true);
		top.setEnabled(true);
		left.setEnabled(true);
		right.setEnabled(true);
		lowerRight.setEnabled(true);
		lowerLeft.setEnabled(true);
		upperRight.setEnabled(true);
		upperLeft.setEnabled(true);
		rename.setEnabled(true);
		deleteButton.setEnabled(true);
		apply.setEnabled(true);

		// show all attributes on the selected object
		showAttributesOnSelectedObject(selectedVar);
	}

	/**
	 * Class to sort Variables by name.
	 */
	static class VariableNameSort implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			Variable v1 = (Variable)o1;
			Variable v2 = (Variable)o2;
			String s1 = v1.getKey().getName();
			String s2 = v2.getKey().getName();
			return s1.compareToIgnoreCase(s2);
		}
	}

	/**
	 * Method to show all attributes on the selected object in the list.
	 * @param selectThis highlight this variable in the list (if not null).
	 */
	private void showAttributesOnSelectedObject(Variable selectThis)
	{
		list.clearSelection();
		listModel.clear();

		// make a sorted list of variable names
		List variables = new ArrayList();
		for(Iterator it = selectedObject.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (!var.isDisplay()) continue;
			variables.add(var);
		}
		Collections.sort(variables, new VariableNameSort());

		// show the variables
		int selectIndex = 0;
		int count = 0;
		for(Iterator it = variables.iterator(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (var == selectThis) selectIndex = count;
			String varName = var.getKey().getName();
			if (varName.startsWith("ATTR_"))
			{
				listModel.addElement(varName.substring(5));
				count++;
				continue;
			}

			// see if any cell, node, or arc variables are available to the user
			String betterName = Variable.betterVariableName(varName);
			if (betterName != null)
			{
				listModel.addElement(betterName);
				count++;
				continue;
			}
		}
		if (count != 0)
		{
			list.setSelectedIndex(selectIndex);
			list.ensureIndexIsVisible(selectIndex);
			showSelectedAttribute();
		}
	}

	/**
	 * Method to return the Variable that is selected in the dialog.
	 * @return the Variable that is selected.  Returns null if none are.
	 */
	Variable getSelectedVariable()
	{
		int i = list.getSelectedIndex();
		if (i < 0) return null;
		String pt = (String)list.getSelectedValue();
		for(Iterator it = selectedObject.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			String varName = var.getKey().getName();
			if (!varName.startsWith("ATTR_")) continue;
			if (varName.substring(5).equalsIgnoreCase(pt)) return var;
		}

		// didn't find name with "ATTR" prefix, look for name directly
		for(Iterator it = selectedObject.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			String varName = var.getKey().getName();
			if (varName.equalsIgnoreCase(pt)) return var;
		}
		return null;
	}

	/**
	 * Method to convert a string to a proper Object for storage in a Variable.
	 * Currently knows only Integer, Double, and String.
	 * @param text the text describing the Variable's value.
	 * @return an Object that contains the value.
	 */
	private Object getVariableObject(String text)
	{
		if (TextUtils.isANumber(text))
		{
			int i = TextUtils.atoi(text);
			double d = TextUtils.atof(text);
			if (i == d) return new Integer(i);
			return new Double(d);
		}
		return text;
	}

	/**
	 * Method to display the aspects of the currently selected Variable in the dialog.
	 */
	void showSelectedAttribute()
	{
		Variable var = getSelectedVariable();
		if (var == null) return;

		// set the Name field
		initialName = (String)list.getSelectedValue();
		String pt = initialName;
		name.setText(pt);

		// get settings associated with "var" into the "initial*" field variables
		grabAttributeInitialValues(var);

		// set the Value field
		value.setText(initialValue);
		if (var.getObject() instanceof Object [])
		{
			value.setEditable(false);
		} else
		{
			value.setEditable(true);
		}

		// set the Code field
		if (initialCode != 0)
		{
			Object eval = VarContext.globalContext.evalVar(var);
			evaluation.setText("Evaluation: "+eval);
		} else
		{
			evaluation.setText("");
		}
		language.setSelectedIndex(initialCode);

		// set the Units field
		units.setSelectedIndex(initialUnit.getIndex());

		// set the Show field
		show.setSelectedIndex(initialDisplayIndex);

		// set the Is Parameter check
		if (currentButton == currentCell || currentButton == currentNode)
		{
			isParameter.setEnabled(true);
		} else
		{
			isParameter.setEnabled(false);
		}
		isParameter.setSelected(initialIsParameter);

		// set the Instances Inherit check
		instancesInherit.setSelected(initialInherit);

		// set the Size field
		pointsSize.setEditable(true);
		unitsSize.setEditable(true);
		if (initialSize.isAbsolute())
		{
			// show point size
			int height = (int)initialSize.getSize();
			pointsSize.setText(Integer.toString(height));
			pointsButton.setSelected(true);

			// figure out how many units the point value is
			EditWindow wnd = EditWindow.getCurrent();
			if (wnd == null) unitsSize.setEditable(false); else
			{
				double u = wnd.getTextUnitSize(height);
				unitsSize.setText(Double.toString(u));
			}
		} else
		{
			// show units value
			double height = initialSize.getSize();
			unitsSize.setText(Double.toString(height));
			unitsButton.setSelected(true);

			// figure out how many points the unit value is
			EditWindow wnd = EditWindow.getCurrent();
			if (wnd == null) pointsSize.setEditable(false); else
			{
				int p = wnd.getTextPointSize(height);
				pointsSize.setText(Integer.toString(p));
			}
		}

		// set the Offset fields
		xOffset.setText(Double.toString(initialOffX));
		yOffset.setText(Double.toString(initialOffY));

		// set text styles
		italic.setSelected(initialItalic);
		bold.setSelected(initialBold);
		underline.setSelected(initialUnderline);

		// set text font popup
		if (initialFont == 0) font.setSelectedIndex(0); else
		{
			TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(initialFont);
			if (af != null)
				font.setSelectedItem(af.getName());
		}

		// set text rotation popup
		rotation.setSelectedIndex(initialRotation.getIndex());

		// set the Grab Point buttons
		if (initialPosition == TextDescriptor.Position.CENT)      center.setSelected(true); else
		if (initialPosition == TextDescriptor.Position.UP)        bottom.setSelected(true); else
		if (initialPosition == TextDescriptor.Position.DOWN)      top.setSelected(true); else
		if (initialPosition == TextDescriptor.Position.RIGHT)     left.setSelected(true); else
		if (initialPosition == TextDescriptor.Position.LEFT)      right.setSelected(true); else
		if (initialPosition == TextDescriptor.Position.UPLEFT)    lowerRight.setSelected(true); else
		if (initialPosition == TextDescriptor.Position.UPRIGHT)   lowerLeft.setSelected(true); else
		if (initialPosition == TextDescriptor.Position.DOWNLEFT)  upperRight.setSelected(true); else
		if (initialPosition == TextDescriptor.Position.DOWNRIGHT) upperLeft.setSelected(true);
	}

	/**
	 * Method to save settings of a Variable in the field variables "initial*".
	 * @param var the Variable to save information about.
	 */
	private void grabAttributeInitialValues(Variable var)
	{
		TextDescriptor td = var.getTextDescriptor();

		// get initial the Value field
		initialValue = var.getPureValue(-1, 0);

		// get initial the Code field
		if (var.isJava()) initialCode = 1; else initialCode = 0;

		// get initial the Units field
		initialUnit = td.getUnit();

		// get initial the Show field
		initialDisplayIndex = 0;
		if (var.isDisplay())
		{
			TextDescriptor.DispPos tdp = td.getDispPart();
			initialDisplayIndex = tdp.getIndex() + 1;
		}

		// get initial the Is Parameter check
		if (currentButton == currentCell || currentButton == currentNode)
		{
			initialIsParameter = td.isParam();
		} else
		{
			initialIsParameter = false;
		}

		// get initial the Instances Inherit check
		initialInherit = td.isInherit();

		// get initial the Size field
		initialSize = td.getSize();

		// get initial the Offset fields
		initialOffX = td.getXOff();
		initialOffY = td.getYOff();

		// get initial text styles
		initialItalic = td.isItalic();
		initialBold = td.isBold();
		initialUnderline = td.isUnderline();

		// get initial text font popup
		initialFont = td.getFace();

		// get initial text rotation popup
		initialRotation = td.getRotation();

		// get initial the Grab Point buttons
		initialPosition = td.getPos();
	}

	/**
	 * Class to delete an attribute in a new thread.
	 */
	protected static class DeleteAttribute extends Job
	{
		Attributes dialog;

		protected DeleteAttribute(Attributes dialog)
		{
			super("Delete Attribute", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.dialog = dialog;
			startJob();
		}

		public void doIt()
		{
			Variable var = dialog.getSelectedVariable();
			if (var == null) return;
			dialog.selectedObject.delVar(var.getKey());
			dialog.showAttributesOnSelectedObject(var);
		}
	}

	/**
	 * Class to create or modify an attribute in a new thread.
	 */
	protected static class ChangeAttribute extends Job
	{
		Attributes dialog;

		protected ChangeAttribute(Attributes dialog)
		{
			super("Change Attribute", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.dialog = dialog;
			startJob();
		}

		public void doIt()
		{
			// convert the Name field to a proper Variable name
			String varName = dialog.name.getText();
			if (varName.trim().length() == 0)
			{
				System.out.println("Attribute name must not be empty");
				return;
			}
			varName = "ATTR_" + varName;

			// get the Variable that is currently selected
			Variable var = dialog.getSelectedVariable();

			// decide whether a new Variable is being created
			boolean newAttribute = false;
			if (var == null) newAttribute = true; else
			{
				// see if this is a new or updated attribute
				newAttribute = !var.getKey().getName().equalsIgnoreCase(varName);
			}

			// change the Value field if a new Variable is being created or if the value changed
			String currentText = dialog.value.getText();
			if (newAttribute || !currentText.equals(dialog.initialValue))
			{
				Object obj = dialog.getVariableObject(currentText);
				var = dialog.selectedObject.newVar(varName, obj);
				if (var == null)
				{
					System.out.println("Error setting the attribute");
					return;
				}
			}

			TextDescriptor td = var.getTextDescriptor();

			// change the Code field
			int currentCode = dialog.language.getSelectedIndex();
			if (newAttribute || currentCode != dialog.initialCode)
			{
				if (currentCode != 0)
				{
					var.setJava();
					Object eval = VarContext.globalContext.evalVar(var);
					dialog.evaluation.setText("Evaluation: "+eval);
				} else
				{
					var.clearCode();
					dialog.evaluation.setText("");
				}
				dialog.initialCode = currentCode;
			}

			// change the Units field
			int currentUnitIndex = dialog.units.getSelectedIndex();
			if (newAttribute || dialog.initialUnit.getIndex() != currentUnitIndex)
			{
				dialog.initialUnit = TextDescriptor.Unit.getUnitAt(currentUnitIndex);
				td.setUnit(dialog.initialUnit);
			}

			// change the Show field
			int currentShowIndex = dialog.show.getSelectedIndex();
			if (newAttribute || currentShowIndex != dialog.initialDisplayIndex)
			{
				if (currentShowIndex == 0)
				{
					var.clearDisplay();
				} else
				{
					var.setDisplay();
					td.setDispPart(TextDescriptor.DispPos.getShowStylesAt(currentShowIndex-1));
				}
				dialog.initialDisplayIndex = currentShowIndex;
			}

			// change the Is Parameter check
			boolean currentParameter = dialog.isParameter.isSelected();
			if (newAttribute || dialog.initialIsParameter != currentParameter)
			{
				if (currentParameter) td.setParam(); else td.clearParam();
				dialog.initialIsParameter = currentParameter;
			}

			// change the Instances Inherit check
			boolean currentInherit = dialog.instancesInherit.isSelected();
			if (newAttribute || dialog.initialInherit != currentInherit)
			{
				if (currentInherit) td.setInherit(); else td.clearInherit();
				dialog.initialInherit = currentInherit;
			}

			// change the Size field
			TextDescriptor.Size currentSize = null;
			if (dialog.pointsButton.isSelected())
			{
				int numPoints = TextUtils.atoi(dialog.pointsSize.getText());
				if (newAttribute || !dialog.initialSize.isAbsolute() || numPoints != dialog.initialSize.getSize())
				{
					td.setAbsSize(numPoints);
					dialog.initialSize = td.getSize();
				}
			} else
			{
				double newSize = TextUtils.atof(dialog.unitsSize.getText());
				if (newAttribute || dialog.initialSize.isAbsolute() || newSize != dialog.initialSize.getSize())
				{
					td.setRelSize(newSize);
					dialog.initialSize = td.getSize();
				}
			}

			// set the Offset fields
			double currentOffX = TextUtils.atof(dialog.xOffset.getText());
			double currentOffY = TextUtils.atof(dialog.yOffset.getText());
			if (newAttribute || dialog.initialOffX != currentOffX || dialog.initialOffY != currentOffY)
			{
				td.setOff(currentOffX, currentOffY);
				dialog.initialOffX = currentOffX;
				dialog.initialOffY = currentOffY;
			}

			// set text styles
			boolean currentItalic = dialog.italic.isSelected();
			if (newAttribute || currentItalic != dialog.initialItalic)
			{
				if (currentItalic) td.setItalic(); else td.clearItalic();
				dialog.initialItalic = currentItalic;
			}
			boolean currentBold = dialog.bold.isSelected();
			if (newAttribute || currentBold != dialog.initialBold)
			{
				if (currentBold) td.setBold(); else td.clearBold();
				dialog.initialBold = currentBold;
			}
			boolean currentUnderline = dialog.underline.isSelected();
			if (newAttribute || currentUnderline != dialog.initialUnderline)
			{
				if (currentUnderline) td.setUnderline(); else td.clearUnderline();
				dialog.initialUnderline = currentUnderline;
			}

			// set text font popup
			int currentFont = dialog.font.getSelectedIndex();
			if (currentFont != dialog.initialFont)
			{
				if (currentFont == 0) td.setFace(0); else
				{
					String fontName = (String)dialog.font.getSelectedItem();
					TextDescriptor.ActiveFont newFont = TextDescriptor.ActiveFont.findActiveFont(fontName);
					int newFontIndex = newFont.getIndex();
					td.setFace(newFontIndex);
				}
				dialog.initialFont = currentFont;
			}

			// set text rotation popup
			int currentRotationIndex = dialog.rotation.getSelectedIndex();
			if (newAttribute || currentRotationIndex != dialog.initialRotation.getIndex())
			{
				dialog.initialRotation = TextDescriptor.Rotation.getRotationAt(currentRotationIndex);
				td.setRotation(dialog.initialRotation);
			}

			// set the Grab Point buttons
			TextDescriptor.Position currentPosition = TextDescriptor.Position.CENT;
			if (dialog.bottom.isSelected()) currentPosition = TextDescriptor.Position.UP; else
			if (dialog.top.isSelected()) currentPosition = TextDescriptor.Position.DOWN; else
			if (dialog.left.isSelected()) currentPosition = TextDescriptor.Position.RIGHT; else
			if (dialog.right.isSelected()) currentPosition = TextDescriptor.Position.LEFT; else
			if (dialog.lowerRight.isSelected()) currentPosition = TextDescriptor.Position.UPLEFT; else
			if (dialog.lowerLeft.isSelected()) currentPosition = TextDescriptor.Position.UPRIGHT; else
			if (dialog.upperRight.isSelected()) currentPosition = TextDescriptor.Position.DOWNLEFT; else
			if (dialog.upperLeft.isSelected()) currentPosition = TextDescriptor.Position.DOWNRIGHT;
			if (newAttribute || !currentPosition.equals(dialog.initialPosition))
			{
				td.setPos(currentPosition);
				dialog.initialPosition = currentPosition;
			}

			// queue it for redraw
			Undo.redrawObject(dialog.selectedObject);

			// redisplay lists if a new attribute was added
			if (newAttribute)
			{
				dialog.showAttributesOnSelectedObject(var);
			}
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

        which = new javax.swing.ButtonGroup();
        corner = new javax.swing.ButtonGroup();
        size = new javax.swing.ButtonGroup();
        cancel = new javax.swing.JButton();
        ok = new javax.swing.JButton();
        currentCell = new javax.swing.JRadioButton();
        currentNode = new javax.swing.JRadioButton();
        currentExport = new javax.swing.JRadioButton();
        currentPort = new javax.swing.JRadioButton();
        currentArc = new javax.swing.JRadioButton();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel1 = new javax.swing.JLabel();
        body = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        pointsSize = new javax.swing.JTextField();
        unitsSize = new javax.swing.JTextField();
        pointsButton = new javax.swing.JRadioButton();
        unitsButton = new javax.swing.JRadioButton();
        jLabel5 = new javax.swing.JLabel();
        font = new javax.swing.JComboBox();
        italic = new javax.swing.JCheckBox();
        bold = new javax.swing.JCheckBox();
        underline = new javax.swing.JCheckBox();
        jLabel6 = new javax.swing.JLabel();
        rotation = new javax.swing.JComboBox();
        jLabel8 = new javax.swing.JLabel();
        xOffset = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        yOffset = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        listPane = new javax.swing.JScrollPane();
        rename = new javax.swing.JButton();
        deleteButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        name = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        value = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        language = new javax.swing.JComboBox();
        jLabel13 = new javax.swing.JLabel();
        units = new javax.swing.JComboBox();
        jLabel14 = new javax.swing.JLabel();
        show = new javax.swing.JComboBox();
        isParameter = new javax.swing.JCheckBox();
        instancesInherit = new javax.swing.JCheckBox();
        textCornerPanel = new javax.swing.JPanel();
        center = new javax.swing.JRadioButton();
        bottom = new javax.swing.JRadioButton();
        top = new javax.swing.JRadioButton();
        right = new javax.swing.JRadioButton();
        left = new javax.swing.JRadioButton();
        lowerRight = new javax.swing.JRadioButton();
        lowerLeft = new javax.swing.JRadioButton();
        upperRight = new javax.swing.JRadioButton();
        upperLeft = new javax.swing.JRadioButton();
        textIconCenter = new javax.swing.JLabel();
        textIconBottom = new javax.swing.JLabel();
        textIconTop = new javax.swing.JLabel();
        textIconRight = new javax.swing.JLabel();
        textIconLeft = new javax.swing.JLabel();
        textIconLowerRight = new javax.swing.JLabel();
        textIconLowerLeft = new javax.swing.JLabel();
        textIconUpperRight = new javax.swing.JLabel();
        textIconUpperLeft = new javax.swing.JLabel();
        evaluation = new javax.swing.JLabel();
        cellName = new javax.swing.JLabel();
        apply = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Edit Attributes");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

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
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 0.3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(cancel, gridBagConstraints);

        ok.setText("OK");
        ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                ok(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 0.3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);

        currentCell.setText("On Current Cell:");
        which.add(currentCell);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        getContentPane().add(currentCell, gridBagConstraints);

        currentNode.setText(" Node");
        which.add(currentNode);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        getContentPane().add(currentNode, gridBagConstraints);

        currentExport.setText(" Export");
        which.add(currentExport);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        getContentPane().add(currentExport, gridBagConstraints);

        currentPort.setText("Port on Node");
        which.add(currentPort);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        getContentPane().add(currentPort, gridBagConstraints);

        currentArc.setText(" Arc");
        which.add(currentArc);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        getContentPane().add(currentArc, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(jSeparator1, gridBagConstraints);

        jLabel1.setText("or Highlighted Object:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(jLabel1, gridBagConstraints);

        body.setLayout(new java.awt.GridBagLayout());

        jLabel4.setText("Size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(jLabel4, gridBagConstraints);

        pointsSize.setColumns(8);
        pointsSize.setMinimumSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        body.add(pointsSize, gridBagConstraints);

        unitsSize.setColumns(8);
        unitsSize.setMinimumSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        body.add(unitsSize, gridBagConstraints);

        pointsButton.setText("Points (max 63)");
        size.add(pointsButton);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        body.add(pointsButton, gridBagConstraints);

        unitsButton.setText("Units (max 127.75)");
        size.add(unitsButton);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        body.add(unitsButton, gridBagConstraints);

        jLabel5.setText("Font:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(jLabel5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(font, gridBagConstraints);

        italic.setText("Italic");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(italic, gridBagConstraints);

        bold.setText("Bold");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(bold, gridBagConstraints);

        underline.setText("Underline");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(underline, gridBagConstraints);

        jLabel6.setText("Rotation:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(jLabel6, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(rotation, gridBagConstraints);

        jLabel8.setText("X offset:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(jLabel8, gridBagConstraints);

        xOffset.setColumns(8);
        xOffset.setMinimumSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(xOffset, gridBagConstraints);

        jLabel9.setText("Y offset:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(jLabel9, gridBagConstraints);

        yOffset.setColumns(8);
        yOffset.setMinimumSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(yOffset, gridBagConstraints);

        jLabel10.setText("Attributes:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        body.add(jLabel10, gridBagConstraints);

        jLabel3.setText("(increments of 0.25)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
        body.add(listPane, gridBagConstraints);

        rename.setText("Rename");
        rename.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                renameActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(rename, gridBagConstraints);

        deleteButton.setText("Delete");
        deleteButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                deleteButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(deleteButton, gridBagConstraints);

        jLabel2.setText("Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(jLabel2, gridBagConstraints);

        name.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(name, gridBagConstraints);

        jLabel11.setText("Value:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(jLabel11, gridBagConstraints);

        value.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(value, gridBagConstraints);

        jLabel12.setText("Code:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(jLabel12, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(language, gridBagConstraints);

        jLabel13.setText("Units:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(jLabel13, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(units, gridBagConstraints);

        jLabel14.setText("Show:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(jLabel14, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(show, gridBagConstraints);

        isParameter.setText("Is Parameter");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(isParameter, gridBagConstraints);

        instancesInherit.setText("Instances inherit");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        body.add(instancesInherit, gridBagConstraints);

        textCornerPanel.setLayout(new java.awt.GridBagLayout());

        textCornerPanel.setBorder(new javax.swing.border.TitledBorder("Text corner"));
        center.setText("Center");
        corner.add(center);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        textCornerPanel.add(center, gridBagConstraints);

        bottom.setText("Bottom");
        corner.add(bottom);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        textCornerPanel.add(bottom, gridBagConstraints);

        top.setText("Top");
        corner.add(top);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        textCornerPanel.add(top, gridBagConstraints);

        right.setText("Right");
        corner.add(right);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        textCornerPanel.add(right, gridBagConstraints);

        left.setText("Left");
        corner.add(left);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        textCornerPanel.add(left, gridBagConstraints);

        lowerRight.setText("Lower right");
        corner.add(lowerRight);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        textCornerPanel.add(lowerRight, gridBagConstraints);

        lowerLeft.setText("Lower left");
        corner.add(lowerLeft);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        textCornerPanel.add(lowerLeft, gridBagConstraints);

        upperRight.setText("Upper right");
        corner.add(upperRight);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        textCornerPanel.add(upperRight, gridBagConstraints);

        upperLeft.setText("Upper left");
        corner.add(upperLeft);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 0);
        textCornerPanel.add(upperLeft, gridBagConstraints);

        textIconCenter.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconCenter.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        textCornerPanel.add(textIconCenter, gridBagConstraints);

        textIconBottom.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconBottom.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        textCornerPanel.add(textIconBottom, gridBagConstraints);

        textIconTop.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconTop.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        textCornerPanel.add(textIconTop, gridBagConstraints);

        textIconRight.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconRight.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        textCornerPanel.add(textIconRight, gridBagConstraints);

        textIconLeft.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconLeft.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        textCornerPanel.add(textIconLeft, gridBagConstraints);

        textIconLowerRight.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconLowerRight.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        textCornerPanel.add(textIconLowerRight, gridBagConstraints);

        textIconLowerLeft.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconLowerLeft.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        textCornerPanel.add(textIconLowerLeft, gridBagConstraints);

        textIconUpperRight.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconUpperRight.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        textCornerPanel.add(textIconUpperRight, gridBagConstraints);

        textIconUpperLeft.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconUpperLeft.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        textCornerPanel.add(textIconUpperLeft, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridheight = 9;
        body.add(textCornerPanel, gridBagConstraints);

        evaluation.setText("Evaluation:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        body.add(evaluation, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(body, gridBagConstraints);

        cellName.setText("clock{sch}");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(cellName, gridBagConstraints);

        apply.setText("Apply");
        apply.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                applyActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.weightx = 0.3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(apply, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void deleteButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_deleteButtonActionPerformed
	{//GEN-HEADEREND:event_deleteButtonActionPerformed
		// delete the attribute
		DeleteAttribute job = new DeleteAttribute(this);
	}//GEN-LAST:event_deleteButtonActionPerformed

	private void renameActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_renameActionPerformed
	{//GEN-HEADEREND:event_renameActionPerformed
		// Add your handling code here:
	}//GEN-LAST:event_renameActionPerformed

	private void applyActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_applyActionPerformed
	{//GEN-HEADEREND:event_applyActionPerformed
		// change the attribute
		ChangeAttribute job = new ChangeAttribute(this);
	}//GEN-LAST:event_applyActionPerformed

	private void cancel(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancel
	{//GEN-HEADEREND:event_cancel
		closeDialog(null);
	}//GEN-LAST:event_cancel

	private void ok(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ok
	{//GEN-HEADEREND:event_ok
		applyActionPerformed(evt);
		closeDialog(null);
	}//GEN-LAST:event_ok

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
//		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton apply;
    private javax.swing.JPanel body;
    private javax.swing.JCheckBox bold;
    private javax.swing.JRadioButton bottom;
    private javax.swing.JButton cancel;
    private javax.swing.JLabel cellName;
    private javax.swing.JRadioButton center;
    private javax.swing.ButtonGroup corner;
    private javax.swing.JRadioButton currentArc;
    private javax.swing.JRadioButton currentCell;
    private javax.swing.JRadioButton currentExport;
    private javax.swing.JRadioButton currentNode;
    private javax.swing.JRadioButton currentPort;
    private javax.swing.JButton deleteButton;
    private javax.swing.JLabel evaluation;
    private javax.swing.JComboBox font;
    private javax.swing.JCheckBox instancesInherit;
    private javax.swing.JCheckBox isParameter;
    private javax.swing.JCheckBox italic;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JComboBox language;
    private javax.swing.JRadioButton left;
    private javax.swing.JScrollPane listPane;
    private javax.swing.JRadioButton lowerLeft;
    private javax.swing.JRadioButton lowerRight;
    private javax.swing.JTextField name;
    private javax.swing.JButton ok;
    private javax.swing.JRadioButton pointsButton;
    private javax.swing.JTextField pointsSize;
    private javax.swing.JButton rename;
    private javax.swing.JRadioButton right;
    private javax.swing.JComboBox rotation;
    private javax.swing.JComboBox show;
    private javax.swing.ButtonGroup size;
    private javax.swing.JPanel textCornerPanel;
    private javax.swing.JLabel textIconBottom;
    private javax.swing.JLabel textIconCenter;
    private javax.swing.JLabel textIconLeft;
    private javax.swing.JLabel textIconLowerLeft;
    private javax.swing.JLabel textIconLowerRight;
    private javax.swing.JLabel textIconRight;
    private javax.swing.JLabel textIconTop;
    private javax.swing.JLabel textIconUpperLeft;
    private javax.swing.JLabel textIconUpperRight;
    private javax.swing.JRadioButton top;
    private javax.swing.JCheckBox underline;
    private javax.swing.JComboBox units;
    private javax.swing.JRadioButton unitsButton;
    private javax.swing.JTextField unitsSize;
    private javax.swing.JRadioButton upperLeft;
    private javax.swing.JRadioButton upperRight;
    private javax.swing.JTextField value;
    private javax.swing.ButtonGroup which;
    private javax.swing.JTextField xOffset;
    private javax.swing.JTextField yOffset;
    // End of variables declaration//GEN-END:variables
	
}
