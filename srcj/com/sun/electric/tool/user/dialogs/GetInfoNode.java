/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GetInfoNode.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.dialogs;

import com.sun.electric.database.change.DatabaseChangeEvent;
import com.sun.electric.database.change.DatabaseChangeListener;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveNodeSize;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Technology.NodeLayer;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Client;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.HighlightListener;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.UserInterfaceMain;
import com.sun.electric.tool.user.tecEdit.Manipulate;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.util.TextUtils;
import com.sun.electric.util.math.DBMath;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Class to handle the "Node Properties" dialog.
 */
public class GetInfoNode extends EModelessDialog implements HighlightListener, DatabaseChangeListener
{
	private static GetInfoNode theDialog = null;
	private NodeInst shownNode = null;
	private PortProto shownPort = null;
	private double initialXPos, initialYPos;
	private String initialXSize, initialYSize;
	private boolean initialMirrorX, initialMirrorY;
	private int initialRotation, initialPopupIndex;
	private boolean initialEasyToSelect, initialInvisibleOutsideCell, initialLocked, initialExpansion;
	private String initialName, initialTextField1, initialTextField2;
	private String initialPopupEntry;
	private boolean textField1Visible, textField2Visible, popupVisible;
	private DefaultListModel listModel;
	private JList list;
	private List<AttributesTable.AttValPair> allAttributes;
	private List<ArcInst> portObjects;
	private boolean bigger;
	private boolean scalableTrans;
	private boolean multiCutNode;
	private boolean carbonNanotubeNode;
	private boolean swapXY;
	private AttributesTable attributesTable;
	private EditWindow wnd;

	private static Preferences prefs = Preferences.userNodeForPackage(GetInfoNode.class);

	/**
	 * Method to show the Node Properties dialog.
	 */
	public static void showDialog()
	{
		if (Client.getOperatingSystem() == Client.OS.UNIX) {
			// JKG 07Apr2006:
			// On Linux, if a dialog is built, closed using setVisible(false),
			// and then requested again using setVisible(true), it does
			// not appear on top. I've tried using toFront(), requestFocus(),
			// but none of that works.  Instead, I brute force it and
			// rebuild the dialog from scratch each time.
			if (theDialog != null) theDialog.dispose();
			theDialog = null;
		}
		if (theDialog == null)
		{
			JFrame jf = null;
			if (TopLevel.isMDIMode()) jf = TopLevel.getCurrentJFrame();
			theDialog = new GetInfoNode(jf);
		}

        theDialog.loadInfo();

		if (!theDialog.isVisible())
		{
			theDialog.pack();
			theDialog.ensureProperSize();
			theDialog.setVisible(true);
			theDialog.name.requestFocus();
		}
		theDialog.toFront();
	}

	/**
	 * Reloads the dialog when Highlights change
	 */
	public void highlightChanged(Highlighter which)
	{
		if (!isVisible()) return;
		loadInfo();
	}

	/**
	 * Called when by a Highlighter when it loses focus. The argument
	 * is the Highlighter that has gained focus (may be null).
	 * @param highlighterGainedFocus the highlighter for the current window (may be null).
	 */
	public void highlighterLostFocus(Highlighter highlighterGainedFocus) {
		if (!isVisible()) return;
		loadInfo();
	}

	/**
	 * Respond to database changes
	 * @param e database change event
	 */
	public void databaseChanged(DatabaseChangeEvent e) {
		if (!isVisible()) return;

		// update dialog if we care about the changes
		if (e.objectChanged(shownNode) || shownPort instanceof Export && e.objectChanged((Export)shownPort))
		{
			loadInfo();
		}
	}

	/** Creates new form Node Properties */
	private GetInfoNode(Frame parent)
	{
		super(parent);
		initComponents();
		getRootPane().setDefaultButton(ok);

		UserInterfaceMain.addDatabaseChangeListener(this);
		Highlighter.addHighlightListener(this);

		// make all text fields select-all when entered
	    EDialog.makeTextFieldSelectAllOnTab(name);
	    EDialog.makeTextFieldSelectAllOnTab(rotation);
	    EDialog.makeTextFieldSelectAllOnTab(textField1);
	    EDialog.makeTextFieldSelectAllOnTab(textField2);
	    EDialog.makeTextFieldSelectAllOnTab(xPos);
	    EDialog.makeTextFieldSelectAllOnTab(xSize);
	    EDialog.makeTextFieldSelectAllOnTab(yPos);
	    EDialog.makeTextFieldSelectAllOnTab(ySize);

	    // make type a selectable but not editable field
	    textField1Visible = textField2Visible = popupVisible = true;
		type.setEditable(false);
		type.setBorder(null);
		type.setForeground(UIManager.getColor("Label.foreground"));
		type.setFont(UIManager.getFont("Label.font"));

		bigger = prefs.getBoolean("GetInfoNode-bigger", false);
		int buttonSelected = prefs.getInt("GetInfoNode-buttonSelected", 0);

		// start small
		if (bigger == false) {
			getContentPane().remove(moreStuffTop);
			getContentPane().remove(listPane);
			getContentPane().remove(moreStuffBottom);
			more.setText("More");
			pack();
		} else {
			more.setText("Less");
		}

		// make the list
		listModel = new DefaultListModel();
		list = new JList(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listPane.setViewportView(list);
		allAttributes = new ArrayList<AttributesTable.AttValPair>();
		portObjects = new ArrayList<ArcInst>();

		attributesTable = new AttributesTable(null, true, false, false);

		switch (buttonSelected)
		{
			case 0: ports.setSelected(true);	   break;
			case 1: attributes.setSelected(true);  break;
			case 2: busMembers.setSelected(true);  break;
		}

		finishInitialization();
	}

	protected void escapePressed() { cancelActionPerformed(null); }

	protected void loadInfo()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run() { doLoadInfo(); }
			});
			return;
		}
		doLoadInfo();
	}

	private void doLoadInfo()
	{
		// update current window
		EditWindow curWnd = EditWindow.getCurrent();
		if (curWnd != null) wnd = curWnd;

		// must have a single node selected
		NodeInst ni = null;
		PortProto pp = null;
		int nodeCount = 0;
		if (wnd != null) {
			for(Highlight h : wnd.getHighlighter().getHighlights())
			{
				if (!h.isHighlightEOBJ()) continue;
				ElectricObject eobj = h.getElectricObject();
				if (eobj instanceof PortInst)
				{
					pp = ((PortInst)eobj).getPortProto();
					eobj = ((PortInst)eobj).getNodeInst();
				}
				if (eobj instanceof NodeInst)
				{
					ni = (NodeInst)eobj;
					nodeCount++;
				}
			}
		}
		showAllButton.setEnabled(false);
		if (nodeCount > 1) ni = null;
		if (ni == null)
		{
			if (shownNode != null)
			{
				// no node selected, disable the dialog
				type.setText("");
				name.setEditable(false);
				name.setText("");
				xSize.setEditable(false);
				xSize.setText("");
				ySize.setEditable(false);
				ySize.setText("");
				xPos.setEditable(false);
				xPos.setText("");
				yPos.setEditable(false);
				yPos.setText("");
				rotation.setEditable(false);
				rotation.setText("");
				mirrorX.setEnabled(false);
				mirrorX.setSelected(false);
				mirrorY.setEnabled(false);
				mirrorY.setSelected(false);
				apply.setEnabled(false);

				// in "more" version
				expanded.setEnabled(false);
				unexpanded.setEnabled(false);
				easyToSelect.setEnabled(false);
				easyToSelect.setSelected(false);
				invisibleOutsideCell.setEnabled(false);
				invisibleOutsideCell.setSelected(false);
				textField1Label.setText("");
				textField1.setText("");
				textField1.setEditable(false);
				textField2Label.setText("");
				textField2.setText("");
				textField2.setEditable(false);
				popupLabel.setText("");
				popup.removeAllItems();
				popup.setEnabled(false);
				ports.setEnabled(false);
				attributes.setEnabled(false);
				attributesTable.setElectricObject(null);
				attributesTable.setEnabled(false);
				busMembers.setEnabled(false);
				listPane.setEnabled(false);
				listModel.clear();
				locked.setEnabled(false);
				locked.setSelected(false);
				see.setEnabled(false);
				editParameters.setEnabled(false);
				colorAndPattern.setEnabled(false);

				shownNode = null;
				shownPort = null;
			}
			return;
		}

		shownNode = ni;
		shownPort = pp;

		EDialog.focusClearOnTextField(name);

		// in small version
		NodeProto np = ni.getProto();
		Technology tech = ni.getParent().getTechnology();
		name.setEditable(true);
		boolean sizeEditable = true;
		xPos.setEditable(true);
		yPos.setEditable(true);
		rotation.setEditable(true);
		mirrorX.setEnabled(true);
		mirrorY.setEnabled(true);
		apply.setEnabled(true);

		initialName = ni.getName();
		initialXPos = ni.getAnchorCenterX();
		initialYPos = ni.getAnchorCenterY();
		double initXSize = ni.getLambdaBaseXSize();
		double initYSize = ni.getLambdaBaseYSize();
		initialRotation = ni.getAngle();
		swapXY = false;
		if (initialRotation == 900 || initialRotation == 2700) swapXY = true;

		String protoName = np.describe(false);
		String extra = ni.getTechSpecificAddition();
		if (extra.length() > 0) protoName += " (" + extra + ")";
		type.setText(protoName);
		name.setText(initialName);
		xPos.setText(TextUtils.formatDistance(initialXPos, tech));
		yPos.setText(TextUtils.formatDistance(initialYPos, tech));
		boolean realMirrorX = ni.isXMirrored();
		boolean realMirrorY = ni.isYMirrored();
		if (swapXY)
		{
			xSize.setText(TextUtils.formatDistance(initYSize, tech));
			ySize.setText(TextUtils.formatDistance(initXSize, tech));
			initialMirrorX = realMirrorY;
			initialMirrorY = realMirrorX;
		} else
		{
			xSize.setText(TextUtils.formatDistance(initXSize, tech));
			ySize.setText(TextUtils.formatDistance(initYSize, tech));
			initialMirrorX = realMirrorX;
			initialMirrorY = realMirrorY;
		}
		initialXSize = xSize.getText();
		initialYSize = ySize.getText();
		mirrorX.setSelected(initialMirrorX);
		mirrorY.setSelected(initialMirrorY);
		rotation.setText(TextUtils.formatDouble(initialRotation / 10.0));

		// special case for transistors or resistors
		PrimitiveNodeSize npSize = ni.getPrimitiveDependentNodeSize(null);
		if (npSize != null) {
			xsizeLabel.setText("Width:");
	        xsizeLabel.setDisplayedMnemonic('w');
			ysizeLabel.setText("Length:");
	        ysizeLabel.setDisplayedMnemonic('l');
            String finalW = npSize.getWidthInString(), finalH = npSize.getLengthInString();
            xSize.setText(finalW);
            ySize.setText(finalH);
            initialXSize = xSize.getText();
			initialYSize = ySize.getText();
		} else {
			xsizeLabel.setText("X size:");
	        xsizeLabel.setDisplayedMnemonic('s');
			ysizeLabel.setText("Y size:");
	        ysizeLabel.setDisplayedMnemonic('z');
		}

		// in "more" version
		easyToSelect.setEnabled(true);
		invisibleOutsideCell.setEnabled(true);
		ports.setEnabled(true);
		attributes.setEnabled(true);
		attributesTable.setEnabled(true);
		busMembers.setEnabled(true);
		listPane.setEnabled(true);
		locked.setEnabled(true);
		editParameters.setEnabled(false);
		if (ni.isCellInstance()) editParameters.setEnabled(true);
		colorAndPattern.setEnabled(ni.getProto().getTechnology() == Artwork.tech());

		// grab all attributes and parameters
		allAttributes.clear();

		for(Iterator<Variable> it = ni.getParametersAndVariables(); it.hasNext(); )
		{
			Variable var = it.next();
			String name = var.getKey().getName();
			if (!name.startsWith("ATTR_")) continue;

			// found an attribute
			AttributesTable.AttValPair avp = new AttributesTable.AttValPair();
			avp.key = var.getKey();
			avp.trueName = var.getTrueName();
			avp.value = var.getObject().toString();
			avp.code = var.isCode();
			allAttributes.add(avp);
		}
		boolean hasAttributes = allAttributes.size() != 0 || ni.getParameters().hasNext();
		attributes.setEnabled(hasAttributes);
		attributesTable.setEnabled(hasAttributes);
		attributesTable.setElectricObject(ni);
		if (attributes.isSelected() && !hasAttributes) ports.setSelected(true);

		int busWidth = 1;
		Netlist nl = shownNode.getParent().getNetlist();
		if (nl != null)
		{
			if (shownPort != null && shownPort instanceof Export)
				busWidth = nl.getBusWidth((Export)shownPort);
		}
		if (busWidth <= 1)
		{
			if (busMembers.isSelected()) ports.setSelected(true);
			busMembers.setEnabled(false);
		}
		showProperList(false);

		// special lines default to empty
		textField1Label.setText("");
		textField1.setText("");
		textField1.setEditable(false);
		textField2Label.setText("");
		textField2.setText("");
		textField2.setEditable(false);
		popupLabel.setText("");
		popup.removeAllItems();
		popup.setEnabled(false);

		// see if this node has outline information
//		Point2D [] outline = ni.getTrace();
//		if (outline != null)
//		{
//			sizeEditable = false;
//		}

		// if there is outline information on a transistor, remember that
		initialTextField1 = initialTextField2 = null;
		boolean lengthEditable = false;
		if (ni.isSerpentineTransistor())
			lengthEditable = true;

		// set the expansion button
		if (np instanceof Cell)
		{
			expanded.setEnabled(true);
			unexpanded.setEnabled(true);
			initialExpansion = ni.isExpanded();
			if (initialExpansion) expanded.setSelected(true); else
				unexpanded.setSelected(true);
			sizeEditable = false;
		} else
		{
			expanded.setEnabled(false);
			unexpanded.setEnabled(false);
		}

		if (sizeEditable) {
			xSize.setEditable(true);
			ySize.setEditable(true);
		} else {
			xSize.setEditable(false);
			ySize.setEditable(lengthEditable);
		}

		// load visible-outside-cell state
		initialInvisibleOutsideCell = ni.isVisInside();
		invisibleOutsideCell.setSelected(initialInvisibleOutsideCell);

		// load easy of selection
		initialEasyToSelect = !ni.isHardSelect();
		easyToSelect.setSelected(initialEasyToSelect);
		if (np instanceof Cell && !User.isEasySelectionOfCellInstances())
			easyToSelect.setEnabled(false);

		// load locked state
		initialLocked = ni.isLocked();
		locked.setSelected(initialLocked);

		// load special node information
		PrimitiveNode.Function fun = ni.getFunction();
		if (np == Schematics.tech().transistorNode || np == Schematics.tech().transistor4Node)
		{
			if (!ni.getFunction().isFET())
			{
				textField1.setEditable(true);
				textField1Label.setText("Area:");

				Variable var = ni.getVar(Schematics.ATTR_AREA);
				textField1.setText(var.getPureValue(-1));
			}
		}

		multiCutNode = false;
		if (!ni.isCellInstance())
		{
			PrimitiveNode pnp = (PrimitiveNode)np;
			multiCutNode = pnp.isMulticut();
//			if (pnp.findMulticut() != null) multiCutNode = true;
		}
		if (multiCutNode)
		{
			popupLabel.setText("Cut placement:");
			popup.setEnabled(true);
			popup.addItem("In node center");
			popup.addItem("At node edges");
			popup.addItem("In node corner");
			initialPopupIndex = ni.getVarValue(NodeLayer.CUT_ALIGNMENT, Integer.class, new Integer(NodeLayer.MULTICUT_CENTERED)).intValue();
			popup.setSelectedIndex(initialPopupIndex);

			textField1Label.setText("Cut spacing:");
			textField1.setEditable(true);
            Variable var = ni.getVar(NodeLayer.CUT_SPACING);
			if (var == null)
				textField1.setText("DEFAULT");
            else
            	textField1.setText(var.getPureValue(-1));
		}

		carbonNanotubeNode = false;
		if (!ni.isCellInstance())
		{
			PrimitiveNode pnp = (PrimitiveNode)np;
			if (pnp.getFunction() == PrimitiveNode.Function.TRANMOSCN ||
				pnp.getFunction() == PrimitiveNode.Function.TRAPMOSCN)
			{
				NodeLayer[] primLayers = pnp.getNodeLayers();
	            for (NodeLayer nodeLayer: primLayers)
	                if (nodeLayer.getLayer().isCarbonNanotubeLayer()) carbonNanotubeNode = true;
			}
		}
		if (carbonNanotubeNode)
		{
			textField1Label.setText("Number of Carbon Nanotubes:");
			textField1.setEditable(true);
            Variable var = ni.getVar(NodeLayer.CARBON_NANOTUBE_COUNT);
			if (var == null)
				textField1.setText("DEFAULT");
            else
            	textField1.setText(var.getPureValue(-1));

			textField2Label.setText("Spacing of Carbon Nanotubes:");
			textField2.setEditable(true);
            var = ni.getVar(NodeLayer.CARBON_NANOTUBE_PITCH);
			if (var == null)
				textField2.setText("DEFAULT");
            else
            	textField2.setText(var.getPureValue(-1));
		}

		scalableTrans = false;
		if (!ni.isCellInstance())
		{
			if (np.getTechnology() == Technology.getMocmosTechnology())
			{
				if (np.getName().equals("P-Transistor-Scalable") ||
					np.getName().equals("N-Transistor-Scalable"))
						scalableTrans = true;
			}
		}
		if (scalableTrans)
		{
			popupLabel.setText("Contacts:");
			popup.addItem("Top & Bottom / normal spacing");
			popup.addItem("Top & Bottom / half-unit closer");
			popup.addItem("Only Bottom / normal spacing");
			popup.addItem("Only Bottom / half-unit closer");
			popup.addItem("None");
			String pt = ni.getVarValue(Technology.TRANS_CONTACT, String.class);
			int numContacts = 2;
			boolean insetContacts = false;
			if (pt != null)
			{
				for(int i=0; i<pt.length(); i++)
				{
					char chr = pt.charAt(i);
					if (chr == '0' || chr == '1' || chr == '2')
					{
						numContacts = chr - '0';
					} else if (chr == 'i' || chr == 'I') insetContacts = true;
				}
			}
			initialPopupIndex = (2 - numContacts) * 2;
			if (insetContacts && numContacts > 0) initialPopupIndex++;
			popup.setSelectedIndex(initialPopupIndex);
			popup.setEnabled(true);

			textField1Label.setText("Width:");
			Variable var = ni.getVar(Schematics.ATTR_WIDTH);
			double width = ni.getLambdaBaseXSize();
			if (var != null) width = TextUtils.atof(var.getPureValue(-1));
			initialTextField1 = TextUtils.formatDistance(width, tech);
			textField1.setEditable(true);
			textField1.setText(initialTextField1);
		}
		if (fun.isResistor())
		{
			if (fun == PrimitiveNode.Function.RESPPOLY || fun == PrimitiveNode.Function.RESNPOLY ||
                fun == PrimitiveNode.Function.RESPNSPOLY || fun == PrimitiveNode.Function.RESNNSPOLY)
				textField1Label.setText("Poly resistance:");
            else if (fun == PrimitiveNode.Function.RESHIRESPOLY2)
            	textField1Label.setText("Hi-Res Poly2 resistance:");
            else if (fun == PrimitiveNode.Function.RESPWELL || fun == PrimitiveNode.Function.RESNWELL)
            	textField1Label.setText("Well resistance:");
            else if (fun == PrimitiveNode.Function.RESPACTIVE || fun == PrimitiveNode.Function.RESNACTIVE)
            	textField1Label.setText("Active resistance:");
            else
            	textField1Label.setText("Resistance:");
//			formatinfstr(infstr, x_(" (%s):"),
//				TRANSLATE(us_resistancenames[(us_electricalunits&INTERNALRESUNITS) >> INTERNALRESUNITSSH]));
			Variable var = ni.getVar(Schematics.SCHEM_RESISTANCE);
			if (var == null) initialTextField1 = "0"; else
				initialTextField1 = new String(var.getObject().toString());
			textField1.setEditable(true);
			textField1.setText(initialTextField1);
		}
		if (fun.isCapacitor())
		{
			if (fun == PrimitiveNode.Function.ECAPAC)
				textField1Label.setText("Electrolytic cap:");
			else if (fun == PrimitiveNode.Function.POLY2CAPAC)
				textField1Label.setText("Poly2 cap:");
            else
			    textField1Label.setText("Capacitance:");
//			formatinfstr(infstr, x_(" (%s):"),
//				TRANSLATE(us_capacitancenames[(us_electricalunits&INTERNALCAPUNITS) >> INTERNALCAPUNITSSH]));
			Variable var = ni.getVar(Schematics.SCHEM_CAPACITANCE);
			if (var == null) initialTextField1 = "0"; else
				initialTextField1 = new String(var.getObject().toString());
			textField1.setEditable(true);
			textField1.setText(initialTextField1);
		}
		if (fun == PrimitiveNode.Function.INDUCT)
		{
			textField1Label.setText("Inductance:");
//			formatinfstr(infstr, x_(" (%s):"),
//				TRANSLATE(us_inductancenames[(us_electricalunits&INTERNALINDUNITS) >> INTERNALINDUNITSSH]));
			Variable var = ni.getVar(Schematics.SCHEM_INDUCTANCE);
			if (var == null) initialTextField1 = "0"; else
				initialTextField1 = new String(var.getObject().toString());
			textField1.setEditable(true);
			textField1.setText(initialTextField1);
		}
		if (np == Schematics.tech().bboxNode)
		{
			textField1Label.setText("Function:");
			Variable var = ni.getVar(Schematics.SCHEM_FUNCTION);
			if (var == null) initialTextField1 = ""; else
				initialTextField1 = new String(var.getObject().toString());
			textField1.setEditable(true);
			textField1.setText(initialTextField1);
		}
		if (np == Schematics.tech().globalNode)
		{
			textField1Label.setText("Global name:");
			Variable var = ni.getVar(Schematics.SCHEM_GLOBAL_NAME);
			if (var == null) initialTextField1 = ""; else
				initialTextField1 = new String(var.getObject().toString());
			textField1.setEditable(true);
			textField1.setText(initialTextField1);

			popupLabel.setText("Characteristics:");
			List<PortCharacteristic> characteristics = PortCharacteristic.getOrderedCharacteristics();
			for(PortCharacteristic ch : characteristics)
			{
				popup.addItem(ch.getName());
			}
			PortCharacteristic ch = PortCharacteristic.findCharacteristic(ni.getTechSpecific());
			initialPopupEntry = ch.getName();
			popup.setSelectedItem(initialPopupEntry);
			popup.setEnabled(true);
		}

		// handle technology editor primitives
		if (ni.getParent().isInTechnologyLibrary())
		{
			popupLabel.setText("Tech. editor:");
			popup.addItem(Manipulate.describeNodeMeaning(ni));
		}

		// load the degrees of a circle if appropriate
		if (np == Artwork.tech().circleNode || np == Artwork.tech().thickCircleNode)
		{
			double [] arcData = ni.getArcDegrees();
			double start = DBMath.round(arcData[0] * 180.0 / Math.PI);
			double curvature = DBMath.round(arcData[1] * 180.0 / Math.PI);
			if (start != 0.0)
			{
				textField1Label.setText("Offset angle / Degrees of circle:");
				initialTextField1 = new String(start + " / " + curvature);
			} else
			{
				textField1Label.setText("Degrees of circle:");
				if (curvature == 0) initialTextField1 = "360"; else
					initialTextField1 = new String(Double.toString(curvature));
			}
			textField1.setEditable(true);
			textField1.setText(initialTextField1);
		}

		// make special information lines in "more" mode be conditionally shown
		boolean repack = (textField1.isEditable() == textField1Visible) ||
			(textField2.isEditable() == textField2Visible) ||
			(popup.isEnabled() == popupVisible);
		textField1Visible = textField1.isEditable();
		if (textField1Visible)
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.WEST;
			moreStuffTop.add(textField1Label, gbc);

			gbc = new GridBagConstraints();
			gbc.gridx = 1;   gbc.gridy = 1;
			gbc.gridwidth = 3;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new Insets(4, 4, 4, 4);
			moreStuffTop.add(textField1, gbc);
		} else
		{
			moreStuffTop.remove(textField1Label);
			moreStuffTop.remove(textField1);
		}
		textField2Visible = textField2.isEditable();
		if (textField2Visible)
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 2;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.WEST;
			moreStuffTop.add(textField2Label, gbc);

			gbc = new GridBagConstraints();
			gbc.gridx = 1;   gbc.gridy = 2;
			gbc.gridwidth = 3;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new Insets(4, 4, 4, 4);
			moreStuffTop.add(textField2, gbc);
		} else
		{
			moreStuffTop.remove(textField2Label);
			moreStuffTop.remove(textField2);
		}
		popupVisible = popup.isEnabled();
		if (popupVisible)
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 3;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.WEST;
			moreStuffTop.add(popupLabel, gbc);

			gbc = new GridBagConstraints();
			gbc.gridx = 1;   gbc.gridy = 3;
			gbc.gridwidth = 3;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new Insets(4, 4, 4, 4);
			moreStuffTop.add(popup, gbc);
		} else
		{
			moreStuffTop.remove(popupLabel);
			moreStuffTop.remove(popup);
		}

		// Setting the initial focus
        EDialog.focusOnTextField(name);
        if (repack) pack();
	}

	private void showProperList(boolean showAll)
	{
		listModel.clear();
		portObjects.clear();

		if (ports.isSelected())
		{
			// show ports
			listPane.setViewportView(list);
			NodeProto np = shownNode.getProto();
			List<String> portMessages = new ArrayList<String>();
			int selectedLine = 0;
			int total = 0;
			for(Iterator<PortInst> it = shownNode.getPortInsts(); it.hasNext(); )
			{
				PortInst pi = it.next();
				if (!showAll && total++ > 100)
				{
					int numLeft = 1;
					while(it.hasNext()) { pi = it.next();   numLeft++; }
					String description = "*** Plus " + numLeft + " more ports.  Use 'Show All' to see them.";
					portMessages.add(description);
					portObjects.add(null);
					showAllButton.setEnabled(true);
					break;
				}
				PortProto pp = pi.getPortProto();
				PortCharacteristic ch = pp.getCharacteristic();
				String description;
				if (ch == PortCharacteristic.UNKNOWN) description = "Port "; else
					description = ch.getName() + " port ";
				description += pp.getName();

				// mention if it is highlighted
				if (pp == shownPort)
				{
					selectedLine = portMessages.size();
					description += " (Highlighted)";
				}

				description += " connects to";
				ArcProto [] connList = pp.getBasePort().getConnections();
				int count = 0;
				for(int i=0; i<connList.length; i++)
				{
					ArcProto ap = connList[i];
					if ((np instanceof Cell || np.getTechnology() != Generic.tech()) &&
						ap.getTechnology() == Generic.tech()) continue;
					if (count > 0) description += ",";
					description += " " + ap.getName();
					count++;
				}
				boolean moreInfo = false;
				if (pp == shownPort) moreInfo = true;
				for(Iterator<Connection> aIt = shownNode.getConnections(); aIt.hasNext(); )
				{
					Connection con = aIt.next();
					if (con.getPortInst() == pi) { moreInfo = true;   break; }
				}
				for(Iterator<Export> eIt = shownNode.getExports(); eIt.hasNext(); )
				{
					Export e = eIt.next();
					if (e.getOriginalPort() == pi) { moreInfo = true;   break; }
				}
				if (moreInfo) description += ":";
				portMessages.add(description);
				portObjects.add(null);

				// talk about any arcs on this prototype
				for(Iterator<Connection> aIt = shownNode.getConnections(); aIt.hasNext(); )
				{
					Connection con = aIt.next();
					if (con.getPortInst() != pi) continue;
					ArcInst ai = con.getArc();
					description = "  Connected at (" + con.getLocation().getX() + "," + con.getLocation().getY() +
						") to " + ai;
					portMessages.add(description);
					portObjects.add(ai);
				}

				// talk about any exports of this prototype
				for(Iterator<Export> eIt = shownNode.getExports(); eIt.hasNext(); )
				{
					Export e = eIt.next();
					if (e.getOriginalPort() != pi) continue;
					description = "  Available as " + e.getCharacteristic().getName() + " export '" + e.getName() + "'";
					portMessages.add(description);
					portObjects.add(null);
				}
			}
			see.setEnabled(true);
			list.setListData(portMessages.toArray());
			list.setSelectedIndex(selectedLine);
			list.ensureIndexIsVisible(selectedLine);
		}
		if (busMembers.isSelected())
		{
			Netlist nl = shownNode.getParent().getNetlist();
			int busWidth = nl.getBusWidth((Export)shownPort);
			List<String> busMessages = new ArrayList<String>();
			for(int i=0; i<busWidth; i++)
			{
				if (!showAll && i++ > 100)
				{
					int numLeft = busWidth-100;
					String description = "*** Plus " + numLeft + " more bus members.  Use 'Show All' to see them.";
					busMessages.add(description);
					showAllButton.setEnabled(true);
					break;
				}
				Network net = nl.getNetwork(shownNode, shownPort, i);
				String netDescr = "?";
				if (net != null) netDescr = net.describe(false);
				busMessages.add(i + ": " + netDescr);
			}
			listPane.setViewportView(list);
			list.setListData(busMessages.toArray());
		}
		if (attributes.isSelected())
		{
			listPane.setViewportView(attributesTable);
		}
	}

	private static class ChangeNode extends Job
	{
		private NodeInst ni;
		private double initialXPos, initialYPos, currentXPos, currentYPos;
		private String initialXSize, initialYSize, currentXSize, currentYSize;
		private boolean currentMirrorX, currentMirrorY;
		private int currentRotation;
		private int initialPopupIndex, currentPopupIndex;
		private boolean initialEasyToSelect, currentEasyToSelect;
		private boolean initialInvisibleOutsideCell, currentInvisibleOutsideCell;
		private boolean initialLocked, currentLocked;
		private boolean initialExpansion, currentExpansion;
		private String initialName, currentName;
		private String initialTextField1, currentTextField1;
		private String initialTextField2, currentTextField2;
		private String initialPopupEntry, currentPopupEntry;
		private boolean scalableTrans;
		private boolean multiCutNode;
		private boolean carbonNanotubeNode;
		private boolean swapXY;
		private boolean expansionChanged;

		public ChangeNode(NodeInst ni,
			double initialXPos, double currentXPos, double initialYPos, double currentYPos,
			String initialXSize, String currentXSize, String initialYSize, String currentYSize,
			boolean currentMirrorX, boolean currentMirrorY,
			int currentRotation,
			int initialPopupIndex, int currentPopupIndex,
			boolean initialEasyToSelect, boolean currentEasyToSelect,
			boolean initialInvisibleOutsideCell, boolean currentInvisibleOutsideCell,
			boolean initialLocked, boolean currentLocked,
			boolean initialExpansion, boolean currentExpansion,
			String initialName, String currentName,
			String initialTextField1, String currentTextField1,
			String initialTextField2, String currentTextField2,
			String initialPopupEntry, String currentPopupEntry,
			boolean bigger,
			boolean scalableTrans,
			boolean multiCutNode,
			boolean carbonNanotubeNode,
			boolean swapXY)
		{
			super("Modify Node", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.ni = ni;
			this.initialXPos = initialXPos;                                   this.currentXPos = currentXPos;
			this.initialYPos = initialYPos;                                   this.currentYPos = currentYPos;
			this.initialXSize = initialXSize;                                 this.currentXSize = currentXSize;
			this.initialYSize = initialYSize;                                 this.currentYSize = currentYSize;
			                                                                  this.currentMirrorX = currentMirrorX;
			                                                                  this.currentMirrorY = currentMirrorY;
			                                                                  this.currentRotation = currentRotation;
			this.initialPopupIndex = initialPopupIndex;                       this.currentPopupIndex = currentPopupIndex;
			this.initialEasyToSelect = initialEasyToSelect;                   this.currentEasyToSelect = currentEasyToSelect;
			this.initialInvisibleOutsideCell = initialInvisibleOutsideCell;   this.currentInvisibleOutsideCell = currentInvisibleOutsideCell;
			this.initialLocked = initialLocked;                               this.currentLocked = currentLocked;
			this.initialExpansion = initialExpansion;                         this.currentExpansion = currentExpansion;
			this.initialName = initialName;                                   this.currentName = currentName;
			this.initialTextField1 = initialTextField1;                       this.currentTextField1 = currentTextField1;
			this.initialTextField2 = initialTextField2;                       this.currentTextField2 = currentTextField2;
			this.initialPopupEntry = initialPopupEntry;                       this.currentPopupEntry = currentPopupEntry;
			this.scalableTrans = scalableTrans;
			this.multiCutNode = multiCutNode;
			this.carbonNanotubeNode = carbonNanotubeNode;
			this.swapXY = swapXY;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			boolean changed = false;
			expansionChanged = false;
			NodeProto np = ni.getProto();
			Technology tech = ni.getParent().getTechnology();

			if (!currentName.equals(initialName))
			{
				if (currentName.length() == 0) currentName = null;
				ni.setName(currentName);
				TextDescriptor td = ni.getTextDescriptor(NodeInst.NODE_NAME);
				if (td.getDisplay() != TextDescriptor.Display.SHOWN)
					ni.setTextDescriptor(NodeInst.NODE_NAME, td.withDisplay(TextDescriptor.Display.SHOWN));
				changed = true;
			}

			if (ni.isCellInstance())
			{
				if (currentExpansion != initialExpansion)
				{
					ni.setExpanded(currentExpansion);
					changed = true;
					expansionChanged = true;
				}
			}

			if (currentEasyToSelect != initialEasyToSelect)
			{
				if (currentEasyToSelect) ni.clearHardSelect(); else
					ni.setHardSelect();
			}

			if (currentInvisibleOutsideCell != initialInvisibleOutsideCell)
			{
				if (currentInvisibleOutsideCell) ni.setVisInside(); else
					ni.clearVisInside();
				changed = true;
			}

			if (currentLocked != initialLocked)
			{
				if (currentLocked) ni.setLocked(); else
					ni.clearLocked();
			}

			// handle special node information
			if (scalableTrans)
			{
				if (currentPopupIndex != initialPopupIndex)
				{
					int numContacts = 2 - (currentPopupIndex / 2);
					boolean inset = (currentPopupIndex&1) != 0;
					String contactInfo = String.valueOf(numContacts);
					if (inset) contactInfo += "i";
					ni.newVar(Technology.TRANS_CONTACT, contactInfo);
				}

				if (!currentTextField1.equals(initialTextField1))
				{
					double width = TextUtils.atofDistance(currentTextField1, tech);
					Variable oldVar = ni.getVar(Schematics.ATTR_WIDTH);
					Variable var = ni.updateVar(Schematics.ATTR_WIDTH, new Double(width));
					if (var != null && oldVar == null)
					{
						ni.addVar(var.withDisplay(true).withDispPart(TextDescriptor.DispPos.NAMEVALUE));
					}
				}
			}
			if (multiCutNode)
			{
				if (!currentTextField1.equals(initialTextField1))
				{
					if (currentTextField1.equals("DEFAULT") || currentTextField1.length() == 0)
						ni.delVar(NodeLayer.CUT_SPACING); else
							ni.newVar(NodeLayer.CUT_SPACING, new Double(TextUtils.atof(currentTextField1)));
				}
				if (currentPopupIndex != initialPopupIndex)
				{
                    if (currentPopupIndex != NodeLayer.MULTICUT_CENTERED)
                        ni.newVar(NodeLayer.CUT_ALIGNMENT, Integer.valueOf(currentPopupIndex));
                    else
                        ni.delVar(NodeLayer.CUT_ALIGNMENT);
					changed = true;
				}
			}
			if (carbonNanotubeNode)
			{
				if (!currentTextField1.equals(initialTextField1))
				{
					if (currentTextField1.equals("DEFAULT") || currentTextField1.length() == 0)
						ni.delVar(NodeLayer.CARBON_NANOTUBE_COUNT); else
							ni.newVar(NodeLayer.CARBON_NANOTUBE_COUNT, new Integer(TextUtils.atoi(currentTextField1)));
				}
				if (!currentTextField2.equals(initialTextField2))
				{
					if (currentTextField2.equals("DEFAULT") || currentTextField2.length() == 0)
						ni.delVar(NodeLayer.CARBON_NANOTUBE_PITCH); else
							ni.newVar(NodeLayer.CARBON_NANOTUBE_PITCH, new Double(TextUtils.atof(currentTextField2)));
				}
			}
			PrimitiveNode.Function fun = ni.getFunction();
			if (fun == PrimitiveNode.Function.DIODE || fun == PrimitiveNode.Function.DIODEZ)
			{
				if (!currentTextField1.equals(initialTextField1))
				{
					Variable var = ni.updateVarText(Schematics.SCHEM_DIODE, currentTextField1);
					if (var != null && !var.isDisplay()) ni.addVar(var.withDisplay(true));
					changed = true;
				}
			}
			if (fun.isResistor())
			{
				if (!currentTextField1.equals(initialTextField1))
				{
					Variable var = ni.updateVarText(Schematics.SCHEM_RESISTANCE, currentTextField1);
					if (var != null && !var.isDisplay()) ni.addVar(var.withDisplay(true));
					changed = true;
				}
			}
			if (fun.isCapacitor())
			{
				if (!currentTextField1.equals(initialTextField1))
				{
					Variable var = ni.updateVarText(Schematics.SCHEM_CAPACITANCE, currentTextField1);
					if (var != null && !var.isDisplay()) ni.addVar(var.withDisplay(true));
					changed = true;
				}
			}
			if (fun == PrimitiveNode.Function.INDUCT)
			{
				if (!currentTextField1.equals(initialTextField1))
				{
					Variable var = ni.updateVarText(Schematics.SCHEM_INDUCTANCE, currentTextField1);
					if (var != null && !var.isDisplay()) ni.addVar(var.withDisplay(true));
					changed = true;
				}
			}
			if (np == Schematics.tech().bboxNode)
			{
				if (!currentTextField1.equals(initialTextField1))
				{
					Variable var = ni.updateVarText(Schematics.SCHEM_FUNCTION, currentTextField1);
					if (var != null && !var.isDisplay()) ni.addVar(var.withDisplay(true));
					changed = true;
				}
			}
			if (np == Schematics.tech().globalNode)
			{
				if (!currentTextField1.equals(initialTextField1))
				{
					Variable var = ni.updateVarText(Schematics.SCHEM_GLOBAL_NAME, currentTextField1);
					if (var != null && !var.isDisplay()) ni.addVar(var.withDisplay(true));
					changed = true;
				}

				if (!currentPopupEntry.equals(initialPopupEntry))
				{
					PortCharacteristic ch = PortCharacteristic.findCharacteristic(currentPopupEntry);
					ni.setTechSpecific(ch.getBits());
					changed = true;
				}
			}

			// load the degrees of a circle if appropriate
			if (np == Artwork.tech().circleNode || np == Artwork.tech().thickCircleNode)
			{
				if (!currentTextField1.equals(initialTextField1))
				{
					double start = 0;
					double curvature = TextUtils.atof(currentTextField1) * Math.PI / 180.0;
					int slashPos = currentTextField1.indexOf('/');
					if (slashPos >= 0)
					{
						start = curvature;
						curvature = TextUtils.atof(currentTextField1.substring(slashPos+1)) * Math.PI / 180.0;
					}
					ni.setArcDegrees(start, curvature);
					changed = true;
				}
			}

			double initXSize, initYSize;
			double currXSize, currYSize;
			Orientation orient;

			// Figure out change in X and Y size
			// if swapXY, X size was put in Y text box, and vice versa.
			if (swapXY)
			{
				// get true size minus offset (this is the size the user sees)
				currXSize = TextUtils.atof(currentYSize, new Double(ni.getLambdaBaseXSize()), TextDescriptor.Unit.DISTANCE, tech);
				currYSize = TextUtils.atof(currentXSize, new Double(ni.getLambdaBaseYSize()), TextDescriptor.Unit.DISTANCE, tech);
				initXSize = TextUtils.atof(initialYSize, new Double(currXSize), TextDescriptor.Unit.DISTANCE, tech);
				initYSize = TextUtils.atof(initialXSize, new Double(currYSize), TextDescriptor.Unit.DISTANCE, tech);

				// mirror
				orient = Orientation.fromJava(currentRotation, currentMirrorY, currentMirrorX);
			} else
			{
				currXSize = TextUtils.atof(currentXSize, new Double(ni.getLambdaBaseXSize()), TextDescriptor.Unit.DISTANCE, tech);
				currYSize = TextUtils.atof(currentYSize, new Double(ni.getLambdaBaseYSize()), TextDescriptor.Unit.DISTANCE, tech);
				initXSize = TextUtils.atof(initialXSize, new Double(currXSize), TextDescriptor.Unit.DISTANCE, tech);
				initYSize = TextUtils.atof(initialYSize, new Double(currYSize), TextDescriptor.Unit.DISTANCE, tech);

				// mirror
				orient = Orientation.fromJava(currentRotation, currentMirrorX, currentMirrorY);
			}

			// The following code is specific for transistors, and uses the X/Y size fields for
			// Width and Length, and therefore may override the values such that the node size does not
			// get set by them.
			PrimitiveNodeSize size = ni.getPrimitiveDependentNodeSize(null);
			if (size != null)
			{
				// see if this is a schematic transistor
				if (np == Schematics.tech().transistorNode || np == Schematics.tech().transistor4Node ||
					np == Schematics.tech().resistorNode)
				{
					Object width, length;
					if (ni.getFunction().isFET() || ni.getFunction().isComplexResistor())
					{
						// see if we can convert width and length to a Number
						double w = TextUtils.atof(currentXSize, null);
						if (w == 0)
						{
							// set width to whatever text is there
							width = Variable.withCode(currentXSize, ni.getCode(Schematics.ATTR_WIDTH));
						} else
						{
							width = new Double(w);
						}

						double l = TextUtils.atof(currentYSize, null);
						if (l == 0)
						{
							// set length to whatever text is there
							length = Variable.withCode(currentYSize, ni.getCode(Schematics.ATTR_LENGTH));
						} else
						{
							length = new Double(l);
						}
						ni.setPrimitiveNodeSize(width, length);
					}
				} else // layout transistors or resistors
				{
					// this is a layout transistor
					if (ni.isSerpentineTransistor())
					{
						// serpentine transistors can only set length
						double initialLength = ni.getSerpentineTransistorLength();
						double length = TextUtils.atof(currentYSize, new Double(initialLength));
						if (length != initialLength)
							ni.setSerpentineTransistorLength(length);
					} else
					{
						// set length and width by node size for layout transistors
						double initialWidth = size.getDoubleWidth();
						double initialLength = size.getDoubleLength();
						double width = TextUtils.atof(currentXSize, new Double(initialWidth));
						double length = TextUtils.atof(currentYSize, new Double(initialLength));
						if (!DBMath.doublesEqual(width, initialWidth) ||
							!DBMath.doublesEqual(length, initialLength))
						{
							// set transistor or resistor size
							ni.setPrimitiveNodeSize(width, length);
						}
					}
				}
				// ignore size change, but retain mirroring change (sign)
				currXSize = initXSize = ni.getLambdaBaseXSize();
				currYSize = initYSize = ni.getLambdaBaseYSize();
				if (swapXY)
					orient = Orientation.fromJava(currentRotation, currentMirrorY, currentMirrorX);
				else
					orient = Orientation.fromJava(currentRotation, currentMirrorX, currentMirrorY);
			}

			Orientation dOrient = orient.concatenate(ni.getOrient().inverse());
			if (!DBMath.doublesEqual(currentXPos, initialXPos) ||
				!DBMath.doublesEqual(currentYPos, initialYPos) ||
				!DBMath.doublesEqual(currXSize, initXSize) ||
				!DBMath.doublesEqual(currYSize, initYSize) ||
				dOrient != Orientation.IDENT || changed)
			{
				Point2D [] points = ni.getTrace();
				if (points != null)
				{
					double percX = currXSize / initXSize;
					double percY = currYSize / initYSize;
					AffineTransform trans = ni.pureRotateOut();
					Point2D [] newPoints = new Point2D[points.length];
					for(int i=0; i<points.length; i++)
					{
						if (points[i] == null) continue;
						Point2D newPoint = new Point2D.Double(points[i].getX() * percX, points[i].getY() * percY);
						trans.transform(newPoint, newPoint);
						newPoint.setLocation(newPoint.getX() + currentXPos, newPoint.getY() + currentYPos);
						newPoints[i] = newPoint;
					}
					ni.setTrace(newPoints);
					dOrient = Orientation.IDENT;
				}
				ni.modifyInstance(DBMath.round(currentXPos - initialXPos), DBMath.round(currentYPos - initialYPos),
					DBMath.round(currXSize - initXSize),
					DBMath.round(currYSize - initYSize), dOrient);
			}
			fieldVariableChanged("expansionChanged");

			return true;
		}

		public void terminateOK()
		{
			if (expansionChanged)
			{
				EditWindow.expansionChanged(ni.getParent());
				EditWindow.clearSubCellCache();
				EditWindow.repaintAllContents();
			}
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

        expansion = new javax.swing.ButtonGroup();
        selection = new javax.swing.ButtonGroup();
        jLabel1 = new javax.swing.JLabel();
        name = new javax.swing.JTextField();
        cancel = new javax.swing.JButton();
        ok = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        xsizeLabel = new javax.swing.JLabel();
        xSize = new javax.swing.JTextField();
        ysizeLabel = new javax.swing.JLabel();
        ySize = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        xPos = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        yPos = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        rotation = new javax.swing.JTextField();
        mirrorX = new javax.swing.JCheckBox();
        more = new javax.swing.JButton();
        apply = new javax.swing.JButton();
        mirrorY = new javax.swing.JCheckBox();
        moreStuffTop = new javax.swing.JPanel();
        expanded = new javax.swing.JRadioButton();
        unexpanded = new javax.swing.JRadioButton();
        easyToSelect = new javax.swing.JCheckBox();
        invisibleOutsideCell = new javax.swing.JCheckBox();
        textField1Label = new javax.swing.JLabel();
        textField1 = new javax.swing.JTextField();
        popupLabel = new javax.swing.JLabel();
        popup = new javax.swing.JComboBox();
        ports = new javax.swing.JRadioButton();
        attributes = new javax.swing.JRadioButton();
        busMembers = new javax.swing.JRadioButton();
        showAllButton = new javax.swing.JButton();
        textField2Label = new javax.swing.JLabel();
        textField2 = new javax.swing.JTextField();
        moreStuffBottom = new javax.swing.JPanel();
        locked = new javax.swing.JCheckBox();
        see = new javax.swing.JButton();
        colorAndPattern = new javax.swing.JButton();
        editParameters = new javax.swing.JButton();
        listPane = new javax.swing.JScrollPane();
        type = new javax.swing.JTextField();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Node Properties");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        jLabel1.setLabelFor(type);
        jLabel1.setText("Type:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel1, gridBagConstraints);

        name.setPreferredSize(new java.awt.Dimension(250, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(name, gridBagConstraints);

        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(cancel, gridBagConstraints);

        ok.setText("OK");
        ok.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);

        jLabel3.setDisplayedMnemonic('n');
        jLabel3.setLabelFor(name);
        jLabel3.setText("Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        getContentPane().add(jLabel3, gridBagConstraints);

        xsizeLabel.setDisplayedMnemonic('s');
        xsizeLabel.setLabelFor(xSize);
        xsizeLabel.setText("X size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        getContentPane().add(xsizeLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(xSize, gridBagConstraints);

        ysizeLabel.setDisplayedMnemonic('z');
        ysizeLabel.setLabelFor(ySize);
        ysizeLabel.setText("Y size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        getContentPane().add(ysizeLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ySize, gridBagConstraints);

        jLabel6.setDisplayedMnemonic('x');
        jLabel6.setLabelFor(xPos);
        jLabel6.setText("X position:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        getContentPane().add(jLabel6, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(xPos, gridBagConstraints);

        jLabel7.setDisplayedMnemonic('y');
        jLabel7.setLabelFor(yPos);
        jLabel7.setText("Y position:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        getContentPane().add(jLabel7, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(yPos, gridBagConstraints);

        jLabel8.setDisplayedMnemonic('r');
        jLabel8.setLabelFor(rotation);
        jLabel8.setText("Rotation:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        getContentPane().add(jLabel8, gridBagConstraints);

        rotation.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(rotation, gridBagConstraints);

        mirrorX.setText("Mirror L-R");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(mirrorX, gridBagConstraints);

        more.setText("More");
        more.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moreActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(more, gridBagConstraints);

        apply.setText("Apply");
        apply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                applyActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(apply, gridBagConstraints);

        mirrorY.setText("Mirror U-D");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(mirrorY, gridBagConstraints);

        moreStuffTop.setLayout(new java.awt.GridBagLayout());

        expansion.add(expanded);
        expanded.setText("Expanded");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        moreStuffTop.add(expanded, gridBagConstraints);

        expansion.add(unexpanded);
        unexpanded.setText("Unexpanded");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        moreStuffTop.add(unexpanded, gridBagConstraints);

        easyToSelect.setText("Easy to Select");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        moreStuffTop.add(easyToSelect, gridBagConstraints);

        invisibleOutsideCell.setText("Invisible Outside Cell");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        moreStuffTop.add(invisibleOutsideCell, gridBagConstraints);

        textField1Label.setLabelFor(textField1);
        textField1Label.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        moreStuffTop.add(textField1Label, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        moreStuffTop.add(textField1, gridBagConstraints);

        popupLabel.setLabelFor(popup);
        popupLabel.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        moreStuffTop.add(popupLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        moreStuffTop.add(popup, gridBagConstraints);

        selection.add(ports);
        ports.setText("Ports:");
        ports.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                portsActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        moreStuffTop.add(ports, gridBagConstraints);

        selection.add(attributes);
        attributes.setText("Parameters:");
        attributes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                attributesActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        moreStuffTop.add(attributes, gridBagConstraints);

        selection.add(busMembers);
        busMembers.setText("Bus Members on Port:");
        busMembers.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        busMembers.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                busMembersActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        moreStuffTop.add(busMembers, gridBagConstraints);

        showAllButton.setText("Show All");
        showAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showAllButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        moreStuffTop.add(showAllButton, gridBagConstraints);

        textField2Label.setLabelFor(textField1);
        textField2Label.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        moreStuffTop.add(textField2Label, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        moreStuffTop.add(textField2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(moreStuffTop, gridBagConstraints);

        moreStuffBottom.setLayout(new java.awt.GridBagLayout());

        locked.setText("Locked");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        moreStuffBottom.add(locked, gridBagConstraints);

        see.setText("See");
        see.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                seeActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        moreStuffBottom.add(see, gridBagConstraints);

        colorAndPattern.setText("Color and Pattern...");
        colorAndPattern.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                colorAndPatternActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        moreStuffBottom.add(colorAndPattern, gridBagConstraints);

        editParameters.setText("Edit Parameters");
        editParameters.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editParametersActionPerformed(evt);
            }
        });

        moreStuffBottom.add(editParameters, new java.awt.GridBagConstraints());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(moreStuffBottom, gridBagConstraints);

        listPane.setMinimumSize(new java.awt.Dimension(200, 100));
        listPane.setPreferredSize(new java.awt.Dimension(200, 100));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
        getContentPane().add(listPane, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(type, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void showAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showAllButtonActionPerformed
		showProperList(true);
		showAllButton.setEnabled(false);
    }//GEN-LAST:event_showAllButtonActionPerformed

    private void busMembersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_busMembersActionPerformed
		showProperList(false);
    }//GEN-LAST:event_busMembersActionPerformed

	private void colorAndPatternActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_colorAndPatternActionPerformed
	{//GEN-HEADEREND:event_colorAndPatternActionPerformed
		ArtworkLook.showArtworkLookDialog();
	}//GEN-LAST:event_colorAndPatternActionPerformed

	private void moreActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_moreActionPerformed
	{//GEN-HEADEREND:event_moreActionPerformed
		bigger = !bigger;
		if (bigger)
		{
			GridBagConstraints gridBagConstraints;
			gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 6;
			gridBagConstraints.gridwidth = 4;
			gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints.weightx = 1.0;
			getContentPane().add(moreStuffTop, gridBagConstraints);

			gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 7;
			gridBagConstraints.gridwidth = 4;
			gridBagConstraints.fill = GridBagConstraints.BOTH;
			gridBagConstraints.insets = new Insets(0, 4, 4, 4);
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.weighty = 1.0;
			getContentPane().add(listPane, gridBagConstraints);

			gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 8;
			gridBagConstraints.gridwidth = 4;
			gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints.weightx = 1.0;
			getContentPane().add(moreStuffBottom, gridBagConstraints);

			more.setText("Less");
		} else
		{
			getContentPane().remove(moreStuffTop);
			getContentPane().remove(listPane);
			getContentPane().remove(moreStuffBottom);
			more.setText("More");
		}
		pack();
	}//GEN-LAST:event_moreActionPerformed

	private void seeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_seeActionPerformed
	{//GEN-HEADEREND:event_seeActionPerformed
		if (!ports.isSelected()) return;
		int currentIndex = list.getSelectedIndex();
        if (currentIndex == -1) return; // nothing to select

        ArcInst ai = portObjects.get(currentIndex);
		if (ai == null) return;
		NodeInst ni = shownNode;
		if (wnd != null) {
			Highlighter highlighter = wnd.getHighlighter();
			highlighter.clear();
			highlighter.addElectricObject(ni, ni.getParent());
			highlighter.addElectricObject(ai, ai.getParent());
			highlighter.finished();
		}
	}//GEN-LAST:event_seeActionPerformed

	private void attributesActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_attributesActionPerformed
	{//GEN-HEADEREND:event_attributesActionPerformed
		showProperList(false);
	}//GEN-LAST:event_attributesActionPerformed

	private void portsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_portsActionPerformed
	{//GEN-HEADEREND:event_portsActionPerformed
		showProperList(false);
	}//GEN-LAST:event_portsActionPerformed

	private void applyActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_applyActionPerformed
	{//GEN-HEADEREND:event_applyActionPerformed
		if (shownNode == null) return;

		Technology tech = shownNode.getParent().getTechnology();
		double currentXPos = TextUtils.atof(xPos.getText(), new Double(initialXPos), TextDescriptor.Unit.DISTANCE, tech);
		double currentYPos = TextUtils.atof(yPos.getText(), new Double(initialYPos), TextDescriptor.Unit.DISTANCE, tech);
		int currentRotation = (int)(TextUtils.atof(rotation.getText(), new Double(initialRotation)) * 10);

		new ChangeNode(shownNode,
			initialXPos, currentXPos, initialYPos, currentYPos,
			initialXSize, xSize.getText(), initialYSize, ySize.getText(),
			mirrorX.isSelected(), mirrorY.isSelected(),
			currentRotation,
			initialPopupIndex, popup.getSelectedIndex(),
			initialEasyToSelect, easyToSelect.isSelected(),
			initialInvisibleOutsideCell, invisibleOutsideCell.isSelected(),
			initialLocked, locked.isSelected(),
			initialExpansion, expanded.isSelected(),
			initialName, name.getText().trim(),
			initialTextField1, textField1.getText(),
			initialTextField2, textField2.getText(),
			initialPopupEntry, (String)popup.getSelectedItem(),
			bigger,
			scalableTrans,
			multiCutNode,
			carbonNanotubeNode,
			swapXY);
		attributesTable.applyChanges();

		initialName = name.getText().trim();
		initialExpansion = expanded.isSelected();
		initialEasyToSelect = easyToSelect.isSelected();
		initialInvisibleOutsideCell = invisibleOutsideCell.isSelected();
		initialLocked = locked.isSelected();
		initialTextField1 = textField1.getText();
		initialTextField2 = textField2.getText();
		initialPopupEntry = (String)popup.getSelectedItem();
		initialXPos = currentXPos;
		initialYPos = currentYPos;
		initialXSize = xSize.getText();
		initialYSize = ySize.getText();
		initialMirrorX = mirrorX.isSelected();
		initialMirrorY = mirrorY.isSelected();
		initialRotation = currentRotation;
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

	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		prefs.putBoolean("GetInfoNode-bigger", bigger);
		if (ports.isSelected()) prefs.putInt("GetInfoNode-buttonSelected", 0);
		if (attributes.isSelected()) prefs.putInt("GetInfoNode-buttonSelected", 1);
		if (busMembers.isSelected()) prefs.putInt("GetInfoNode-buttonSelected", 2);
		super.closeDialog();
	}//GEN-LAST:event_closeDialog

    private void editParametersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editParametersActionPerformed
		if (shownNode == null) return;
		if (!shownNode.isCellInstance()) return;
		Cell.CellGroup group = ((Cell)shownNode.getProto()).getCellGroup();
		Cell paramOwner = group.getParameterOwner();
		if (paramOwner != null) {
			WindowFrame.createEditWindow(paramOwner);
			Attributes.showDialog();
		}
    }//GEN-LAST:event_editParametersActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton apply;
    private javax.swing.JRadioButton attributes;
    private javax.swing.JRadioButton busMembers;
    private javax.swing.JButton cancel;
    private javax.swing.JButton colorAndPattern;
    private javax.swing.JCheckBox easyToSelect;
    private javax.swing.JButton editParameters;
    private javax.swing.JRadioButton expanded;
    private javax.swing.ButtonGroup expansion;
    private javax.swing.JCheckBox invisibleOutsideCell;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JScrollPane listPane;
    private javax.swing.JCheckBox locked;
    private javax.swing.JCheckBox mirrorX;
    private javax.swing.JCheckBox mirrorY;
    private javax.swing.JButton more;
    private javax.swing.JPanel moreStuffBottom;
    private javax.swing.JPanel moreStuffTop;
    private javax.swing.JTextField name;
    private javax.swing.JButton ok;
    private javax.swing.JComboBox popup;
    private javax.swing.JLabel popupLabel;
    private javax.swing.JRadioButton ports;
    private javax.swing.JTextField rotation;
    private javax.swing.JButton see;
    private javax.swing.ButtonGroup selection;
    private javax.swing.JButton showAllButton;
    private javax.swing.JTextField textField1;
    private javax.swing.JLabel textField1Label;
    private javax.swing.JTextField textField2;
    private javax.swing.JLabel textField2Label;
    private javax.swing.JTextField type;
    private javax.swing.JRadioButton unexpanded;
    private javax.swing.JTextField xPos;
    private javax.swing.JTextField xSize;
    private javax.swing.JLabel xsizeLabel;
    private javax.swing.JTextField yPos;
    private javax.swing.JTextField ySize;
    private javax.swing.JLabel ysizeLabel;
    // End of variables declaration//GEN-END:variables
}
