/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GetInfoText.java
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
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.ui.TopLevel;

import java.util.Iterator;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.geom.Rectangle2D;
import javax.swing.JFrame;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListModel;
import javax.swing.JScrollPane;
import javax.swing.text.Document;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


/**
 * Class to handle the "Text Get-Info" dialog.
 */
public class GetInfoText extends javax.swing.JDialog
{
	private static GetInfoText theDialog = null;
	private Highlight shownText;
	private TextDescriptor.Position initialPos;
	private TextDescriptor.Size initialSize;
	private TextDescriptor.Rotation initialRotation;
	private TextDescriptor.Unit initialUnit;
	private TextDescriptor.DispPos initialDispPart;
	private boolean initialItalic, initialBold, initialUnderline;
	private boolean initialInvisibleOutsideCell;
	private int initialLanguage;
	private int initialFont;
	private String initialText;
	private double initialXOffset, initialYOffset;
	private boolean posNotOffset;

	/**
	 * Method to show the Text Get-Info dialog.
	 */
	public static void showDialog()
	{
		if (theDialog == null)
		{
			JFrame jf = TopLevel.getCurrentJFrame();
			theDialog = new GetInfoText(jf, false);
		}
		theDialog.show();
	}

	/**
	 * Method to reload the Text Get-Info dialog from the current highlighting.
	 */
	public static void load()
	{
		if (theDialog == null) return;
		theDialog.loadTextInfo();
	}

	private void loadTextInfo()
	{
		// must have a single text selected
		Highlight textHighlight = null;
		int textCount = 0;
		for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
		{
			Highlight h = (Highlight)it.next();
			if (h.getType() != Highlight.Type.TEXT) continue;

			// ignore export text
			if (h.getVar() == null && h.getElectricObject() instanceof Export) continue;
			textHighlight = h;
			textCount++;
		}
		if (textCount > 1) textHighlight = null;
		if (textHighlight == null)
		{
			if (shownText != null)
			{
				// no text selected, disable the dialog
				header.setText("No Text Selected");
				theText.setEditable(false);
				evaluation.setText("");
				xOffset.setEditable(false);
				yOffset.setEditable(false);
				pointsSize.setEditable(false);
				unitsSize.setEditable(false);
				theText.setText("");
				xOffset.setText("");
				yOffset.setText("");
				pointsSize.setText("");
				unitsSize.setText("");
				center.setEnabled(false);
				bottom.setEnabled(false);
				top.setEnabled(false);
				left.setEnabled(false);
				right.setEnabled(false);
				lowerRight.setEnabled(false);
				lowerLeft.setEnabled(false);
				upperRight.setEnabled(false);
				upperLeft.setEnabled(false);
				boxed.setEnabled(false);
				editText.setEnabled(false);
				language.setEnabled(false);
				invisibleOutsideCell.setEnabled(false);
				seeNode.setEnabled(false);
				show.setEnabled(false);
				pointsButton.setEnabled(false);
				unitsButton.setEnabled(false);
				font.setEnabled(false);
				italic.setEnabled(false);
				bold.setEnabled(false);
				underline.setEnabled(false);
				rotation.setEnabled(false);
				units.setEnabled(false);

				shownText = null;
			}
			return;
		}

		// enable it
//		editText.setEnabled(true);
		seeNode.setEnabled(true);
		String description = "Unknown text";
		initialText = "";
		TextDescriptor td = null;
		posNotOffset = false;
		boolean editable = true;
		ElectricObject eobj = textHighlight.getElectricObject();
		NodeInst ni = null;
		if (eobj instanceof NodeInst) ni = (NodeInst)eobj;
		Variable var = textHighlight.getVar();
		if (var != null)
		{
			if (ni != null)
			{
				if (ni.getProto() == Generic.tech.invisiblePinNode) posNotOffset = true;
			}
			td = var.getTextDescriptor();
			initialText = var.getPureValue(-1, -1);
			description = var.getFullDescription(eobj);
			if (var.getLength() > 1) editable = false;
		} else
		{
			if (textHighlight.getName() != null)
			{
				if (eobj instanceof Geometric)
				{
					Geometric geom = (Geometric)eobj;
					td = geom.getNameTextDescriptor();
					if (geom instanceof NodeInst)
					{
						description = "Name of node " + ((NodeInst)geom).getProto().describe();
					} else
					{
						description = "Name of arc " + ((ArcInst)geom).getProto().describe();
					}
					initialText = geom.getName();
				}
			} else if (eobj instanceof NodeInst)
			{
				description = "Name of cell instance " + ni.describe();
				td = ni.getProtoTextDescriptor();
				initialText = ni.getProto().describe();
				editable = false;
			}
		}
		header.setText(description);
		theText.setEditable(editable);
		theText.setText(initialText);

		// set the text corner
		center.setEnabled(true);
		bottom.setEnabled(true);
		top.setEnabled(true);
		left.setEnabled(true);
		right.setEnabled(true);
		lowerRight.setEnabled(true);
		lowerLeft.setEnabled(true);
		upperRight.setEnabled(true);
		upperLeft.setEnabled(true);
		boxed.setEnabled(true);
		initialPos = td.getPos();
		if (initialPos == TextDescriptor.Position.CENT)      center.setSelected(true); else
		if (initialPos == TextDescriptor.Position.UP)        bottom.setSelected(true); else
		if (initialPos == TextDescriptor.Position.DOWN)      top.setSelected(true); else
		if (initialPos == TextDescriptor.Position.RIGHT)     left.setSelected(true); else
		if (initialPos == TextDescriptor.Position.LEFT)      right.setSelected(true); else
		if (initialPos == TextDescriptor.Position.UPLEFT)    lowerRight.setSelected(true); else
		if (initialPos == TextDescriptor.Position.UPRIGHT)   lowerLeft.setSelected(true); else
		if (initialPos == TextDescriptor.Position.DOWNLEFT)  upperRight.setSelected(true); else
		if (initialPos == TextDescriptor.Position.DOWNRIGHT) upperLeft.setSelected(true); else
		if (initialPos == TextDescriptor.Position.BOXED)     boxed.setSelected(true);

		// set the offset
		xOffset.setEditable(true);
		yOffset.setEditable(true);
		if (posNotOffset)
		{
			initialXOffset = ni.getGrabCenterX();
			initialYOffset = ni.getGrabCenterY();
		} else
		{
			initialXOffset = td.getXOff();
			initialYOffset = td.getYOff();
		}
		xOffset.setText(Double.toString(initialXOffset));
		yOffset.setText(Double.toString(initialYOffset));

		// set the language
		initialLanguage = 0;
		evaluation.setText("");
		if (var == null) language.setEnabled(true); else
		{
			language.setEnabled(true);
			if (var.isCode())
			{
				initialLanguage = 3;
				evaluation.setText("Evaluation: " + var.describe(-1, -1));
			}
			language.setSelectedIndex(initialLanguage);
		}

		// set the "invisible outside cell"
		invisibleOutsideCell.setEnabled(true);
		initialInvisibleOutsideCell = td.isInterior();
		invisibleOutsideCell.setSelected(initialInvisibleOutsideCell);

		// set what to show
		show.setEnabled(true);
		initialDispPart = td.getDispPart();
		if (initialDispPart == TextDescriptor.DispPos.VALUE) show.setSelectedIndex(0); else
		if (initialDispPart == TextDescriptor.DispPos.NAMEVALUE) show.setSelectedIndex(1); else
		if (initialDispPart == TextDescriptor.DispPos.NAMEVALINH) show.setSelectedIndex(2); else
		if (initialDispPart == TextDescriptor.DispPos.NAMEVALINHALL) show.setSelectedIndex(3);

		// set the size
		pointsSize.setEditable(true);
		unitsSize.setEditable(true);
		pointsButton.setEnabled(true);
		unitsButton.setEnabled(true);
		initialSize = td.getSize();
		if (initialSize.isAbsolute())
		{
			pointsButton.setSelected(true);
			unitsSize.setText("");
			pointsSize.setText(Double.toString(initialSize.getSize()));
		} else
		{
			unitsButton.setSelected(true);
			pointsSize.setText("");
			unitsSize.setText(Double.toString(initialSize.getSize()));
		}

		// set the font
		font.setEnabled(true);
		initialFont = td.getFace();
		if (initialFont == 0) font.setSelectedIndex(0); else
		{
			TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(initialFont);
			if (af != null)
				font.setSelectedItem(af.getName());
		}

		// set italic / bold / underline
		italic.setEnabled(true);
		initialItalic = td.isItalic();
		italic.setSelected(initialItalic);
		bold.setEnabled(true);
		initialBold = td.isBold();
		bold.setSelected(initialBold);
		underline.setEnabled(true);
		initialUnderline = td.isUnderline();
		underline.setSelected(initialUnderline);

		// set the rotation
		rotation.setEnabled(true);
		initialRotation = td.getRotation();
		if (initialRotation == TextDescriptor.Rotation.ROT0) rotation.setSelectedIndex(0); else
		if (initialRotation == TextDescriptor.Rotation.ROT90) rotation.setSelectedIndex(1); else
		if (initialRotation == TextDescriptor.Rotation.ROT180) rotation.setSelectedIndex(2); else
		if (initialRotation == TextDescriptor.Rotation.ROT270) rotation.setSelectedIndex(3);

		// set the units
		units.setEnabled(true);
		initialUnit = td.getUnit();
		if (initialUnit == TextDescriptor.Unit.NONE) units.setSelectedIndex(0); else
		if (initialUnit == TextDescriptor.Unit.RESISTANCE) units.setSelectedIndex(1); else
		if (initialUnit == TextDescriptor.Unit.CAPACITANCE) units.setSelectedIndex(2); else
		if (initialUnit == TextDescriptor.Unit.INDUCTANCE) units.setSelectedIndex(3); else
		if (initialUnit == TextDescriptor.Unit.CURRENT) units.setSelectedIndex(4); else
		if (initialUnit == TextDescriptor.Unit.VOLTAGE) units.setSelectedIndex(5); else
		if (initialUnit == TextDescriptor.Unit.DISTANCE) units.setSelectedIndex(6); else
		if (initialUnit == TextDescriptor.Unit.TIME) units.setSelectedIndex(7);

		shownText = textHighlight;
	}

	/** Creates new form Text Get-Info */
	private GetInfoText(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		setLocation(100, 50);
		initComponents();

		textIconCenter.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabCenter.gif")));
		textIconLeft.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabLeft.gif")));
		textIconRight.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabRight.gif")));
		textIconTop.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabTop.gif")));
		textIconBottom.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabBottom.gif")));
		textIconLowerRight.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabLowerRight.gif")));
		textIconLowerLeft.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabLowerLeft.gif")));
		textIconUpperRight.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabUpperRight.gif")));
		textIconUpperLeft.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabUpperLeft.gif")));
		textIconBoxed.setIcon(new javax.swing.ImageIcon(getClass().getResource("IconGrabBoxed.gif")));

		// This can be done better!!!!! (see Attributes.java)
		language.addItem("Not Code");
		language.addItem("TCL (not available)");
		language.addItem("LISP (not available)");
		language.addItem("Java");

		show.addItem("Value");
		show.addItem("Name&Value");
		show.addItem("Name,Inherit,Value");
		show.addItem("Name,Inherit-All,Value");

		font.addItem("DEFAULT FONT");
		Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
		for(int i=0; i<fonts.length; i++)
			font.addItem(fonts[i].getFontName());

		rotation.addItem("None");
		rotation.addItem("90 degrees counterclockwise");
		rotation.addItem("180 degrees");
		rotation.addItem("90 degrees clockwise");

		units.addItem("None");
		units.addItem("Resistance");
		units.addItem("Capacitance");
		units.addItem("Inductance");
		units.addItem("Current");
		units.addItem("Voltage");
		units.addItem("Distance");
		units.addItem("Time");

		editText.setEnabled(false);

		loadTextInfo();
	}

	protected static class ChangeText extends Job
	{
		Highlight text;
		GetInfoText dialog;

		protected ChangeText(Highlight text, GetInfoText dialog)
		{
			super("Modify Text", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.text = text;
			this.dialog = dialog;
			this.startJob();
		}

		public void doIt()
		{
			boolean changed = false;

			TextDescriptor td = null;
			Variable var = text.getVar();
			ElectricObject eobj = text.getElectricObject();
			if (var != null)
			{
				td = var.getTextDescriptor();
			} else
			{
				if (text.getName() != null && eobj instanceof Geometric)
				{
					Geometric geom = (Geometric)eobj;
					td = geom.getNameTextDescriptor();
				} else if (eobj != null && eobj instanceof NodeInst)
				{
					td = ((NodeInst)eobj).getProtoTextDescriptor();
				}
			}

			String currentText = dialog.theText.getText();
			if (!currentText.equals(dialog.initialText))
			{
				// change the text
				changed = true;
				if (var != null)
				{
					boolean oldCantSet = var.isCantSet();
					boolean oldJava = var.isJava();
					boolean oldDisplay = var.isDisplay();
					boolean oldTemporary = var.isDontSave();

					// change variable
					Variable newVar = eobj.newVar(var.getKey(), currentText);
					if (newVar != null)
					{
						newVar.setDescriptor(td);
						if (oldCantSet) newVar.setCantSet();
						if (oldJava) newVar.setJava();
						if (oldDisplay) newVar.setDisplay();
						if (oldTemporary) newVar.setDontSave();
					}
				} else
				{
					if (text.getName() != null)
					{
						if (eobj != null)
						{
							// change name of NodeInst or ArcInst
							((Geometric)eobj).setName(currentText);
						}
					}
				}
				dialog.initialText = currentText;
			}

			// handle changes to the text corner
			TextDescriptor.Position currentPos = TextDescriptor.Position.CENT;
			if (dialog.bottom.isSelected()) currentPos = TextDescriptor.Position.UP; else
			if (dialog.top.isSelected()) currentPos = TextDescriptor.Position.DOWN; else
			if (dialog.left.isSelected()) currentPos = TextDescriptor.Position.RIGHT; else
			if (dialog.right.isSelected()) currentPos = TextDescriptor.Position.LEFT; else
			if (dialog.lowerRight.isSelected()) currentPos = TextDescriptor.Position.UPLEFT; else
			if (dialog.lowerLeft.isSelected()) currentPos = TextDescriptor.Position.UPRIGHT; else
			if (dialog.upperRight.isSelected()) currentPos = TextDescriptor.Position.DOWNLEFT; else
			if (dialog.upperLeft.isSelected()) currentPos = TextDescriptor.Position.DOWNRIGHT;
			if (currentPos != dialog.initialPos)
			{
				changed = true;
				td.setPos(currentPos);
				dialog.initialPos = currentPos;
			}

			// handle changes to the offset
			double currentXOffset = EMath.atof(dialog.xOffset.getText());
			double currentYOffset = EMath.atof(dialog.yOffset.getText());
			if (!EMath.doublesEqual(currentXOffset, dialog.initialXOffset) ||
				!EMath.doublesEqual(currentYOffset, dialog.initialYOffset))
			{
				changed = true;
				if (dialog.posNotOffset)
				{
					NodeInst ni = (NodeInst)eobj;
					double dX = currentXOffset - ni.getGrabCenterX();
					double dY = currentYOffset - ni.getGrabCenterY();
					ni.modifyInstance(dX, dY, 0, 0, 0);
				} else
				{
					td.setOff(currentXOffset, currentYOffset);
				}
				dialog.initialXOffset = currentXOffset;
				dialog.initialYOffset = currentYOffset;
			}

			// handle changes to the language
			int currentLanguage = dialog.language.getSelectedIndex();
			if (currentLanguage != dialog.initialLanguage && var != null)
			{
				changed = true;
				if (currentLanguage == 3) var.setJava(); else var.clearCode();
			}

			// handle changes to "invisible outside cell"
			boolean currentInvisibleOutsideCell = dialog.invisibleOutsideCell.isSelected();
			if (currentInvisibleOutsideCell != dialog.initialInvisibleOutsideCell)
			{
				changed = true;
				if (currentInvisibleOutsideCell) td.setInterior(); else
					td.clearInterior();
				dialog.initialInvisibleOutsideCell = currentInvisibleOutsideCell;
			}

			// handle changes to what to show
			TextDescriptor.DispPos currentDispPos = null;
			int unitDispPos = dialog.show.getSelectedIndex();
			switch (unitDispPos)
			{
				case 1:  currentDispPos = TextDescriptor.DispPos.NAMEVALUE;       break;
				case 2:  currentDispPos = TextDescriptor.DispPos.NAMEVALINH;      break;
				case 3:  currentDispPos = TextDescriptor.DispPos.NAMEVALINHALL;   break;
				default: currentDispPos = TextDescriptor.DispPos.VALUE;           break;
			}
			if (currentDispPos != dialog.initialDispPart)
			{
				changed = true;
				td.setDispPart(currentDispPos);
				dialog.initialDispPart = currentDispPos;
			}

			// handle changes to the size
			TextDescriptor.Size currentSize = null;
			if (dialog.pointsButton.isSelected())
			{
				int newSize = EMath.atoi(dialog.pointsSize.getText());
				currentSize = TextDescriptor.Size.newAbsSize(newSize);
			} else
			{
				double newSize = EMath.atof(dialog.unitsSize.getText());
				currentSize = TextDescriptor.Size.newRelSize(newSize);
			}
			if (!currentSize.equals(dialog.initialSize))
			{
				changed = true;
				if (currentSize.isAbsolute())
					td.setAbsSize((int)currentSize.getSize()); else
						td.setRelSize(currentSize.getSize());
				dialog.initialSize = currentSize;
			}

			// handle changes to the font
			int currentFont = dialog.font.getSelectedIndex();
			if (currentFont != dialog.initialFont)
			{
				changed = true;
				if (currentFont == 0) td.setFace(0); else
				{
					String fontName = (String)dialog.font.getSelectedItem();
					TextDescriptor.ActiveFont newFont = TextDescriptor.ActiveFont.findActiveFont(fontName);
					int newFontIndex = newFont.getIndex();
					td.setFace(newFontIndex);
				}
				dialog.initialFont = currentFont;
			}

			// handle changes to italic / bold / underline
			boolean currentItalic = dialog.italic.isSelected();
			if (currentItalic != dialog.initialItalic)
			{
				changed = true;
				if (currentItalic) td.setItalic(); else
					td.clearItalic();
				dialog.initialItalic = currentItalic;
			}

			boolean currentBold = dialog.bold.isSelected();
			if (currentBold != dialog.initialBold)
			{
				changed = true;
				if (currentBold) td.setBold(); else
					td.clearBold();
				dialog.initialBold = currentBold;
			}

			boolean currentUnderline = dialog.underline.isSelected();
			if (currentUnderline != dialog.initialUnderline)
			{
				changed = true;
				if (currentUnderline) td.setUnderline(); else
					td.clearUnderline();
				dialog.initialUnderline = currentUnderline;
			}

			// handle changes to the rotation
			TextDescriptor.Rotation currentRotation = null;
			int rotIndex = dialog.rotation.getSelectedIndex();
			switch (rotIndex)
			{
				case 1:  currentRotation = TextDescriptor.Rotation.ROT90;    break;
				case 2:  currentRotation = TextDescriptor.Rotation.ROT180;   break;
				case 3:  currentRotation = TextDescriptor.Rotation.ROT270;   break;
				default: currentRotation = TextDescriptor.Rotation.ROT0;     break;
			}
			if (currentRotation != dialog.initialRotation)
			{
				changed = true;
				td.setRotation(currentRotation);
				dialog.initialRotation = currentRotation;
			}

			// handle changes to the units
			TextDescriptor.Unit currentUnit = null;
			int unitIndex = dialog.units.getSelectedIndex();
			switch (unitIndex)
			{
				case 1:  currentUnit = TextDescriptor.Unit.RESISTANCE;   break;
				case 2:  currentUnit = TextDescriptor.Unit.CAPACITANCE;  break;
				case 3:  currentUnit = TextDescriptor.Unit.INDUCTANCE;   break;
				case 4:  currentUnit = TextDescriptor.Unit.CURRENT;      break;
				case 5:  currentUnit = TextDescriptor.Unit.VOLTAGE;      break;
				case 6:  currentUnit = TextDescriptor.Unit.DISTANCE;     break;
				case 7:  currentUnit = TextDescriptor.Unit.TIME;         break;
				default: currentUnit = TextDescriptor.Unit.NONE;         break;
			}
			if (currentUnit != dialog.initialUnit)
			{
				changed = true;
				td.setUnit(currentUnit);
				dialog.initialUnit = currentUnit;
			}

			if (changed)
			{
				if (eobj != null)
				{
					Undo.redrawObject(eobj);
				} else
				{
					if (text.getCell().getNumNodes() > 0)
					{
						NodeInst ni = (NodeInst)text.getCell().getNodes().next();
						Undo.redrawObject(ni);
					}
				}
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

        grab = new javax.swing.ButtonGroup();
        sizes = new javax.swing.ButtonGroup();
        cancel = new javax.swing.JButton();
        ok = new javax.swing.JButton();
        header = new javax.swing.JLabel();
        theText = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        show = new javax.swing.JComboBox();
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
        jLabel7 = new javax.swing.JLabel();
        units = new javax.swing.JComboBox();
        editText = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        xOffset = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        yOffset = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        language = new javax.swing.JComboBox();
        invisibleOutsideCell = new javax.swing.JCheckBox();
        seeNode = new javax.swing.JButton();
        apply = new javax.swing.JButton();
        evaluation = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        center = new javax.swing.JRadioButton();
        bottom = new javax.swing.JRadioButton();
        top = new javax.swing.JRadioButton();
        right = new javax.swing.JRadioButton();
        left = new javax.swing.JRadioButton();
        lowerRight = new javax.swing.JRadioButton();
        lowerLeft = new javax.swing.JRadioButton();
        upperRight = new javax.swing.JRadioButton();
        upperLeft = new javax.swing.JRadioButton();
        boxed = new javax.swing.JRadioButton();
        textIconCenter = new javax.swing.JLabel();
        textIconBottom = new javax.swing.JLabel();
        textIconTop = new javax.swing.JLabel();
        textIconRight = new javax.swing.JLabel();
        textIconLeft = new javax.swing.JLabel();
        textIconLowerRight = new javax.swing.JLabel();
        textIconLowerLeft = new javax.swing.JLabel();
        textIconUpperRight = new javax.swing.JLabel();
        textIconUpperLeft = new javax.swing.JLabel();
        textIconBoxed = new javax.swing.JLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Text Information");
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
                cancelActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 21;
        gridBagConstraints.gridwidth = 3;
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
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 21;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);

        header.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 0);
        getContentPane().add(header, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(theText, gridBagConstraints);

        jLabel3.setText("Show:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(show, gridBagConstraints);

        jLabel4.setText("Size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel4, gridBagConstraints);

        pointsSize.setColumns(8);
        pointsSize.setMinimumSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        getContentPane().add(pointsSize, gridBagConstraints);

        unitsSize.setColumns(8);
        unitsSize.setMinimumSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        getContentPane().add(unitsSize, gridBagConstraints);

        pointsButton.setText("Points (max 63)");
        sizes.add(pointsButton);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        getContentPane().add(pointsButton, gridBagConstraints);

        unitsButton.setText("Units (max 127.75)");
        sizes.add(unitsButton);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        getContentPane().add(unitsButton, gridBagConstraints);

        jLabel5.setText("Font:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(font, gridBagConstraints);

        italic.setText("Italic");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(italic, gridBagConstraints);

        bold.setText("Bold");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(bold, gridBagConstraints);

        underline.setText("Underline");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(underline, gridBagConstraints);

        jLabel6.setText("Rotation:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 19;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel6, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 19;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(rotation, gridBagConstraints);

        jLabel7.setText("Units:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel7, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(units, gridBagConstraints);

        editText.setText("Edit Text");
        editText.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                editTextActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(editText, gridBagConstraints);

        jLabel8.setText("X offset:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        getContentPane().add(jLabel8, gridBagConstraints);

        xOffset.setColumns(8);
        xOffset.setMinimumSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        getContentPane().add(xOffset, gridBagConstraints);

        jLabel9.setText("Y offset:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        getContentPane().add(jLabel9, gridBagConstraints);

        yOffset.setColumns(8);
        yOffset.setMinimumSize(new java.awt.Dimension(92, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        getContentPane().add(yOffset, gridBagConstraints);

        jLabel10.setText("Language:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel10, gridBagConstraints);

        language.setMinimumSize(new java.awt.Dimension(125, 25));
        language.setPreferredSize(new java.awt.Dimension(125, 25));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(language, gridBagConstraints);

        invisibleOutsideCell.setText("Invisible outside cell");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(invisibleOutsideCell, gridBagConstraints);

        seeNode.setText("See");
        seeNode.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                seeNodeActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(seeNode, gridBagConstraints);

        apply.setText("Apply");
        apply.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                applyActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 21;
        getContentPane().add(apply, gridBagConstraints);

        evaluation.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        getContentPane().add(evaluation, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel1.setBorder(new javax.swing.border.TitledBorder("Text Corner"));
        center.setText("Center");
        grab.add(center);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(center, gridBagConstraints);

        bottom.setText("Bottom");
        grab.add(bottom);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(bottom, gridBagConstraints);

        top.setText("Top");
        grab.add(top);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(top, gridBagConstraints);

        right.setText("Right");
        grab.add(right);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(right, gridBagConstraints);

        left.setText("Left");
        grab.add(left);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(left, gridBagConstraints);

        lowerRight.setText("Lower right");
        grab.add(lowerRight);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(lowerRight, gridBagConstraints);

        lowerLeft.setText("Lower left");
        grab.add(lowerLeft);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(lowerLeft, gridBagConstraints);

        upperRight.setText("Upper right");
        grab.add(upperRight);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(upperRight, gridBagConstraints);

        upperLeft.setText("Upper left");
        grab.add(upperLeft);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(upperLeft, gridBagConstraints);

        boxed.setText("Boxed");
        grab.add(boxed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = -4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(boxed, gridBagConstraints);

        textIconCenter.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconCenter.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(textIconCenter, gridBagConstraints);

        textIconBottom.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconBottom.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(textIconBottom, gridBagConstraints);

        textIconTop.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconTop.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(textIconTop, gridBagConstraints);

        textIconRight.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconRight.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(textIconRight, gridBagConstraints);

        textIconLeft.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconLeft.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(textIconLeft, gridBagConstraints);

        textIconLowerRight.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconLowerRight.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(textIconLowerRight, gridBagConstraints);

        textIconLowerLeft.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconLowerLeft.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(textIconLowerLeft, gridBagConstraints);

        textIconUpperRight.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconUpperRight.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(textIconUpperRight, gridBagConstraints);

        textIconUpperLeft.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconUpperLeft.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(textIconUpperLeft, gridBagConstraints);

        textIconBoxed.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconBoxed.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(textIconBoxed, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.gridheight = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        getContentPane().add(jPanel1, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void editTextActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_editTextActionPerformed
	{//GEN-HEADEREND:event_editTextActionPerformed
		// Add your handling code here:
	}//GEN-LAST:event_editTextActionPerformed

	private void seeNodeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_seeNodeActionPerformed
	{//GEN-HEADEREND:event_seeNodeActionPerformed
		ElectricObject eobj = shownText.getElectricObject();
		if (eobj instanceof NodeInst || eobj instanceof PortInst)
		{
			Cell cell = shownText.getCell();
			Variable var = shownText.getVar();
			Name name = shownText.getName();

			Highlight.clear();
			Highlight.addElectricObject(eobj, cell);
			Highlight newHigh = Highlight.addText(eobj, cell, var, name);
			Highlight.finished();
		}
	}//GEN-LAST:event_seeNodeActionPerformed

	private void applyActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_applyActionPerformed
	{//GEN-HEADEREND:event_applyActionPerformed
		if (shownText == null) return;
		ChangeText job = new ChangeText(shownText, this);
	}//GEN-LAST:event_applyActionPerformed

	private void okActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_okActionPerformed
	{//GEN-HEADEREND:event_okActionPerformed
		applyActionPerformed(evt);
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
//		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton apply;
    private javax.swing.JCheckBox bold;
    private javax.swing.JRadioButton bottom;
    private javax.swing.JRadioButton boxed;
    private javax.swing.JButton cancel;
    private javax.swing.JRadioButton center;
    private javax.swing.JButton editText;
    private javax.swing.JLabel evaluation;
    private javax.swing.JComboBox font;
    private javax.swing.ButtonGroup grab;
    private javax.swing.JLabel header;
    private javax.swing.JCheckBox invisibleOutsideCell;
    private javax.swing.JCheckBox italic;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JComboBox language;
    private javax.swing.JRadioButton left;
    private javax.swing.JRadioButton lowerLeft;
    private javax.swing.JRadioButton lowerRight;
    private javax.swing.JButton ok;
    private javax.swing.JRadioButton pointsButton;
    private javax.swing.JTextField pointsSize;
    private javax.swing.JRadioButton right;
    private javax.swing.JComboBox rotation;
    private javax.swing.JButton seeNode;
    private javax.swing.JComboBox show;
    private javax.swing.ButtonGroup sizes;
    private javax.swing.JLabel textIconBottom;
    private javax.swing.JLabel textIconBoxed;
    private javax.swing.JLabel textIconCenter;
    private javax.swing.JLabel textIconLeft;
    private javax.swing.JLabel textIconLowerLeft;
    private javax.swing.JLabel textIconLowerRight;
    private javax.swing.JLabel textIconRight;
    private javax.swing.JLabel textIconTop;
    private javax.swing.JLabel textIconUpperLeft;
    private javax.swing.JLabel textIconUpperRight;
    private javax.swing.JTextField theText;
    private javax.swing.JRadioButton top;
    private javax.swing.JCheckBox underline;
    private javax.swing.JComboBox units;
    private javax.swing.JRadioButton unitsButton;
    private javax.swing.JTextField unitsSize;
    private javax.swing.JRadioButton upperLeft;
    private javax.swing.JRadioButton upperRight;
    private javax.swing.JTextField xOffset;
    private javax.swing.JTextField yOffset;
    // End of variables declaration//GEN-END:variables
	
}
