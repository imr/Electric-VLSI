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

import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.dialogs.Attributes;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListModel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;


/**
 * Class to handle the "Node Get-Info" dialog.
 */
public class GetInfoNode extends javax.swing.JDialog
{
	private static GetInfoNode theDialog = null;
	private static NodeInst shownNode = null;
	private static PortProto shownPort = null;
	private double initialXPos, initialYPos;
	private double initialXSize, initialYSize;
	private int initialRotation, initialListPopupEntry;
	private boolean initialEasyToSelect, initialInvisibleOutsideCell, initialLocked, initialExpansion;
	private String initialName, initialTextField;
	private String initialPopupEntry, initialListTextField;
	private DefaultListModel listModel;
	private JList list;
	private List allAttributes;
	private List allParameters;
	private List portObjects;
	private boolean bigger;

	/**
	 * Method to show the Node Get-Info dialog.
	 */
	public static void showDialog()
	{
		if (theDialog == null)
		{
			JFrame jf = TopLevel.getCurrentJFrame();
			theDialog = new GetInfoNode(jf, false);
		}
		theDialog.show();
	}

	/**
	 * Method to reload the Node Get-Info dialog from the current highlighting.
	 */
	public static void load()
	{
		if (theDialog == null) return;
		theDialog.loadNodeInfo();
	}

	/** Creates new form Node Get-Info */
	private GetInfoNode(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		setLocation(100, 50);
		initComponents();

		// start small
		bigger = false;
		getContentPane().remove(moreStuffTop);
		getContentPane().remove(listPane);
		getContentPane().remove(moreStuffBottom);
        pack();

		// make the list
		listModel = new DefaultListModel();
		list = new JList(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listPane.setViewportView(list);
		list.addMouseListener(new java.awt.event.MouseAdapter()
		{
			public void mouseClicked(java.awt.event.MouseEvent evt) { listClick(); }
		});
		allAttributes = new ArrayList();
		allParameters = new ArrayList();
		portObjects = new ArrayList();

		listPopup.addItem("Not Code");
		listPopup.addItem("TCL (not available)");
		listPopup.addItem("LISP (not available)");
		listPopup.addItem("Java");

		ports.setSelected(true);

		loadNodeInfo();
	}

	static class AttValPair
	{
		Variable.Key key;
		String trueName;
		String value;
		String eval;
		boolean code;
	};

	private void loadNodeInfo()
	{
		// must have a single node selected
		NodeInst ni = null;
		PortProto pp = null;
		int nodeCount = 0;
		for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
		{
			Highlight h = (Highlight)it.next();
			if (h.getType() == Highlight.Type.EOBJ)
			{
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
				textFieldLabel.setText("");
				textField.setText("");
				textField.setEditable(false);
				popupLabel.setText("");
				popup.removeAllItems();
				popup.setEnabled(false);
				ports.setEnabled(false);
				parameters.setEnabled(false);
				attributes.setEnabled(false);
				listPane.setEnabled(false);
				listModel.clear();
				locked.setEnabled(false);
				locked.setSelected(false);
				see.setEnabled(false);
				attributesButton.setEnabled(false);
				listEditLabel.setText("");
				listEdit.setEditable(false);
				listEdit.setText("");
				listPopupLabel.setText("");
				listPopup.setEnabled(false);
				listPopup.removeAllItems();

				shownNode = null;
				shownPort = null;
			}
			return;
		}

		shownNode = ni;
		shownPort = pp;

		// in small version
		NodeProto np = ni.getProto();
		name.setEditable(true);
		xSize.setEditable(true);
		ySize.setEditable(true);
		xPos.setEditable(true);
		yPos.setEditable(true);
		rotation.setEditable(true);
		mirrorX.setEnabled(true);
		mirrorY.setEnabled(true);
		apply.setEnabled(true);

		initialName = ni.getName();
		initialXPos = ni.getGrabCenterX();
		initialYPos = ni.getGrabCenterY();
		initialXSize = ni.getXSize();
		if (ni.isXMirrored()) initialXSize = -initialXSize;
		initialYSize = ni.getYSize();
		if (ni.isYMirrored()) initialYSize = -initialYSize;
		initialRotation = ni.getAngle();

		type.setText(np.describe());
		name.setText(initialName);
		xPos.setText(Double.toString(initialXPos));
		yPos.setText(Double.toString(initialYPos));
		xSize.setText(Double.toString(Math.abs(initialXSize)));
		ySize.setText(Double.toString(Math.abs(initialYSize)));
		rotation.setText(Double.toString(initialRotation / 10.0));
		mirrorX.setSelected(initialXSize < 0);
		mirrorY.setSelected(initialYSize < 0);

		// in "more" version
		easyToSelect.setEnabled(true);
		invisibleOutsideCell.setEnabled(true);
		ports.setEnabled(true);
		parameters.setEnabled(true);
		attributes.setEnabled(true);
		listPane.setEnabled(true);
		locked.setEnabled(true);
		attributesButton.setEnabled(true);

		// grab all attributes and parameters
		allAttributes.clear();
		allParameters.clear();

		for(Iterator it = ni.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (var.getTextDescriptor().isParam())
			{
				// found a parameter
				AttValPair avp = new AttValPair();
				avp.key = var.getKey();
				avp.trueName = var.getTrueName();
				avp.value = var.getObject().toString();
				avp.code = var.isCode();
				allParameters.add(avp);
				continue;
			}
			String name = var.getKey().getName();
			if (!name.startsWith("ATTR_")) continue;

			// found an attribute
			AttValPair avp = new AttValPair();
			avp.key = var.getKey();
			avp.trueName = var.getTrueName();
			avp.value = var.getObject().toString();
			avp.code = var.isCode();
			allAttributes.add(avp);
		}
		attributes.setEnabled(allAttributes.size() != 0);
		parameters.setEnabled(allParameters.size() != 0);
		if ((attributes.isSelected() && allAttributes.size() == 0) ||
			(parameters.isSelected() && allParameters.size() == 0)) ports.setSelected(true);
		showProperList();

		// special lines default to empty
		textFieldLabel.setText("");
		textField.setText("");
		textField.setEditable(false);
		popupLabel.setText("");
		popup.removeAllItems();
		popup.setEnabled(false);

		// see if this node has outline information
		boolean holdsOutline = false;
		Point2D [] outline = ni.getTrace();
		if (outline != null)
		{
			holdsOutline = true;
			xSize.setEditable(false);
			ySize.setEditable(false);
		}

		// if there is outline information on a transistor, remember that
		initialTextField = null;
		double serpWidth = -1;
		NodeProto.Function fun = ni.getFunction();
		if ((fun == NodeProto.Function.TRANMOS || fun == NodeProto.Function.TRADMOS ||
			fun == NodeProto.Function.TRAPMOS) && holdsOutline)
		{
			/* serpentine transistor: show width, edit length */
			serpWidth = 0;
			Dimension size = ni.getTransistorSize(null);
			if (size.getWidth() > 0 && size.getHeight() > 0)
			{
				textFieldLabel.setText("Width=" + size.getWidth() + "; Length:");
				initialTextField = new String(Double.toString(size.getHeight()));
				serpWidth = size.getHeight();
				textField.setEditable(true);
			}
		}

		// set the expansion button
		if (np instanceof Cell)
		{
			expanded.setEnabled(true);
			unexpanded.setEnabled(true);
			initialExpansion = ni.isExpanded();
			if (initialExpansion) expanded.setSelected(true); else
				unexpanded.setSelected(true);
			xSize.setEditable(false);
			ySize.setEditable(false);
		} else
		{
			expanded.setEnabled(false);
			unexpanded.setEnabled(false);
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
		if (fun == NodeProto.Function.DIODE || fun == NodeProto.Function.DIODEZ)
		{
			if (fun == NodeProto.Function.DIODEZ)
				textFieldLabel.setText("Zener diode size:"); else
					textFieldLabel.setText("Diode size:");
			Variable var = ni.getVar("SCHEM_diode");
			if (var == null) initialTextField = "0"; else
				initialTextField = new String(var.getObject().toString());
			textField.setEditable(true);
			textField.setText(initialTextField);
		}
		if (fun == NodeProto.Function.RESIST)
		{
			textFieldLabel.setText("Resistance:");
//			formatinfstr(infstr, x_(" (%s):"),
//				TRANSLATE(us_resistancenames[(us_electricalunits&INTERNALRESUNITS) >> INTERNALRESUNITSSH]));
			Variable var = ni.getVar("SCHEM_resistance");
			if (var == null) initialTextField = "0"; else
				initialTextField = new String(var.getObject().toString());
			textField.setEditable(true);
			textField.setText(initialTextField);
		}
		if (fun == NodeProto.Function.CAPAC || fun == NodeProto.Function.ECAPAC)
		{
			if (fun == NodeProto.Function.ECAPAC)
				textFieldLabel.setText("Electrolytic cap:"); else
					textFieldLabel.setText("Capacitance:");
//			formatinfstr(infstr, x_(" (%s):"),
//				TRANSLATE(us_capacitancenames[(us_electricalunits&INTERNALCAPUNITS) >> INTERNALCAPUNITSSH]));
			Variable var = ni.getVar("SCHEM_capacitance");
			if (var == null) initialTextField = "0"; else
				initialTextField = new String(var.getObject().toString());
			textField.setEditable(true);
			textField.setText(initialTextField);
		}
		if (fun == NodeProto.Function.INDUCT)
		{
			textFieldLabel.setText("Inductance:");
//			formatinfstr(infstr, x_(" (%s):"),
//				TRANSLATE(us_inductancenames[(us_electricalunits&INTERNALINDUNITS) >> INTERNALINDUNITSSH]));
			Variable var = ni.getVar("SCHEM_inductance");
			if (var == null) initialTextField = "0"; else
				initialTextField = new String(var.getObject().toString());
			textField.setEditable(true);
			textField.setText(initialTextField);
		}
		if (np == Schematics.tech.bboxNode)
		{
			textFieldLabel.setText("Function:");
			Variable var = ni.getVar("SCHEM_function");
			if (var == null) initialTextField = ""; else
				initialTextField = new String(var.getObject().toString());
			textField.setEditable(true);
			textField.setText(initialTextField);
		}
		if (np == Schematics.tech.transistorNode || np == Schematics.tech.transistor4Node)
		{
			textFieldLabel.setText("Transistor type:");
			textField.setText(fun.getName());
		}
		if (fun == NodeProto.Function.FLIPFLOP)
		{
			popupLabel.setText("Flip-flop type:");
//			for(i=0; i<12; i++) newlang[i] = TRANSLATE(flipfloptype[i]);
//			setPopup(DGIN_SPECIAL2, 12, newlang);
//			switch (ni->userbits&FFTYPE)
//			{
//				case FFTYPERS: i = 0;   break;
//				case FFTYPEJK: i = 1;   break;
//				case FFTYPED:  i = 2;   break;
//				case FFTYPET:  i = 3;   break;
//			}
//			switch (ni->userbits&FFCLOCK)
//			{
//				case FFCLOCKMS: i += 0;   break;
//				case FFCLOCKP:  i += 4;   break;
//				case FFCLOCKN:  i += 8;   break;
//			}
//			setPopupEntry(DGIN_SPECIAL2, i);
//			popup.setEnabled(true);
		}
		if (np == Schematics.tech.globalNode)
		{
			textFieldLabel.setText("Global name:");
			Variable var = ni.getVar("SCHEM_global_name");
			if (var == null) initialTextField = ""; else
				initialTextField = new String(var.getObject().toString());
			textField.setEditable(true);
			textField.setText(initialTextField);

			popupLabel.setText("Characteristics:");
			List characteristics = PortProto.Characteristic.getOrderedCharacteristics();
			for(Iterator it = characteristics.iterator(); it.hasNext(); )
			{
				PortProto.Characteristic ch = (PortProto.Characteristic)it.next();
				popup.addItem(ch.getName());
			}
			PortProto.Characteristic ch = PortProto.Characteristic.findCharacteristic(ni.getTechSpecific());
			initialPopupEntry = ch.getName();
			popup.setSelectedItem(initialPopupEntry);
			popup.setEnabled(true);
		}

		// load color of artwork primitives
		if (np instanceof PrimitiveNode && np.getTechnology() == Artwork.tech)
		{
			popupLabel.setText("Color:");
			int [] colors = EGraphics.getColors();
			for(int i=0; i<colors.length; i++)
				popup.addItem(EGraphics.getColorName(colors[i]));
			int index = EGraphics.BLACK;
			Variable var = ni.getVar(Artwork.ART_COLOR);
			if (var != null) index = ((Integer)var.getObject()).intValue();
			initialPopupEntry = EGraphics.getColorName(index);
			popup.setSelectedItem(initialPopupEntry);
			popup.setEnabled(true);
		}

		// load the degrees of a circle if appropriate
		if (np == Artwork.tech.circleNode || np == Artwork.tech.thickCircleNode)
		{
			double [] arcData = ni.getArcDegrees();
			double start = EMath.smooth(arcData[0] * 180.0 / Math.PI);
			double curvature = EMath.smooth(arcData[1] * 180.0 / Math.PI);
			if (start != 0.0)
			{
				textFieldLabel.setText("Offset angle / Degrees of circle:");
				initialTextField = new String(start + " / " + curvature);
			} else
			{
				textFieldLabel.setText("Degrees of circle:");
				if (curvature == 0) initialTextField = "360"; else
					initialTextField = new String(Double.toString(curvature));
			}
			textField.setEditable(true);
			textField.setText(initialTextField);
		}
	}
	
	private void showProperList()
	{
		listModel.clear();
		portObjects.clear();

		if (ports.isSelected())
		{
			// show ports
			NodeProto np = shownNode.getProto();
			for(Iterator it = shownNode.getPortInsts(); it.hasNext(); )
			{
				PortInst pi = (PortInst)it.next();
				PortProto pp = pi.getPortProto();
				PortProto.Characteristic ch = pp.getCharacteristic();
				String description;
				if (ch == PortProto.Characteristic.UNKNOWN) description = "Port "; else
					description = ch.getName() + " port ";
				description += pp.getProtoName() + " connects to";
				ArcProto [] connList = pp.getBasePort().getConnections();
				int count = 0;
				for(int i=0; i<connList.length; i++)
				{
					ArcProto ap = connList[i];
					if ((np instanceof Cell || np.getTechnology() != Generic.tech) &&
						ap.getTechnology() == Generic.tech) continue;
					if (count > 0) description += ",";
					description += " " + ap.getProtoName();
					count++;
				}
				boolean moreInfo = false;
				if (pp == shownPort) moreInfo = true;
				for(Iterator aIt = shownNode.getConnections(); aIt.hasNext(); )
				{
					Connection con = (Connection)aIt.next();
					if (con.getPortInst() == pi) { moreInfo = true;   break; }
				}
				for(Iterator eIt = shownNode.getExports(); eIt.hasNext(); )
				{
					Export e = (Export)eIt.next();
					if (e.getOriginalPort() == pi) { moreInfo = true;   break; }
				}
				if (moreInfo) description += ":";
				listModel.addElement(description);
				portObjects.add(null);

				// mention if it is highlighted
				if (pp == shownPort)
				{
					listModel.addElement("  Highlighted port");
					portObjects.add(null);
				}

				// talk about any arcs on this prototype
				for(Iterator aIt = shownNode.getConnections(); aIt.hasNext(); )
				{
					Connection con = (Connection)aIt.next();
					if (con.getPortInst() != pi) continue;
					ArcInst ai = con.getArc();
					description = "  Connected at (" + con.getLocation().getX() + "," + con.getLocation().getY() +
						") to " + ai.describe() + " arc";
					listModel.addElement(description);
					portObjects.add(ai);
				}

				// talk about any exports of this prototype
				for(Iterator eIt = shownNode.getExports(); eIt.hasNext(); )
				{
					Export e = (Export)eIt.next();
					if (e.getOriginalPort() != pi) continue;
					description = "  Available as " + e.getCharacteristic().getName() + " export '" + e.getProtoName() + "'";
					listModel.addElement(description);
					portObjects.add(null);
				}
			}
			see.setEnabled(true);
			listEdit.setEditable(false);
			listPopup.setEnabled(false);
			listEdit.setText("");
			listEditLabel.setText("");
			listPopupLabel.setText("");
			listEvalLabel.setText("");
			listEval.setText("");
		}
		if (parameters.isSelected())
		{
			// show parameters
			for(Iterator it = allParameters.iterator(); it.hasNext(); )
			{
				AttValPair avp = (AttValPair)it.next();
				listModel.addElement(avp.trueName + " = " + avp.value);
			}
			see.setEnabled(false);
			listEdit.setEditable(true);
			listPopup.setEnabled(true);
			listPopupLabel.setText("Type:");
		}
		if (attributes.isSelected())
		{
			// show attributes
			for(Iterator it = allAttributes.iterator(); it.hasNext(); )
			{
				AttValPair avp = (AttValPair)it.next();
				listModel.addElement(avp.trueName + " = " + avp.value);
			}
			see.setEnabled(false);
			listEdit.setEditable(true);
			listPopup.setEnabled(true);
			listPopupLabel.setText("Type:");
		}
		list.setSelectedIndex(0);
		listClick();
	}

	private void listClick()
	{
		int index = list.getSelectedIndex();
		if (parameters.isSelected())
		{
			AttValPair avp = (AttValPair)allParameters.get(index);
			listEditLabel.setText("Parameter '" + avp.trueName + "'");
			initialListTextField = new String(avp.value);
			listEdit.setText(initialListTextField);
			if (avp.code)
			{
				initialListPopupEntry = 3;
				listEvalLabel.setText("Evaluation:");
				Variable var = shownNode.getVar(avp.key);
				listEval.setText(var.describe(-1, -1, VarContext.globalContext, shownNode));
			} else
			{
				initialListPopupEntry = 0;
				listEvalLabel.setText("");
				listEval.setText("");
			}
			listPopup.setSelectedIndex(initialListPopupEntry);
		}
		if (attributes.isSelected())
		{
			AttValPair avp = (AttValPair)allAttributes.get(index);
			listEditLabel.setText("Attribute '" + avp.trueName + "'");
			initialListTextField = new String(avp.value);
			listEdit.setText(initialListTextField);
			if (avp.code)
			{
				initialListPopupEntry = 3;
				listEvalLabel.setText("Evaluation:");
				Variable var = shownNode.getVar(avp.key);
				listEval.setText(var.describe(-1, -1, VarContext.globalContext, shownNode));
			} else
			{
				initialListPopupEntry = 0;
				listEvalLabel.setText("");
				listEval.setText("");
			}
			listPopup.setSelectedIndex(initialListPopupEntry);
		}
	}

	protected static class ChangeNode extends Job
	{
		NodeInst ni;
		GetInfoNode dialog;

		protected ChangeNode(NodeInst ni, GetInfoNode dialog)
		{
			super("Modify Node", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.ni = ni;
			this.dialog = dialog;
			startJob();
		}

		public void doIt()
		{
			boolean changed = false;
			NodeProto np = ni.getProto();

			String currentName = dialog.name.getText().trim();
			if (!currentName.equals(dialog.initialName))
			{
				dialog.initialName = new String(currentName);
				if (currentName.length() == 0) currentName = null;
				ni.setName(currentName);
				changed = true;
			}

			if (np instanceof Cell)
			{
				boolean currentExpansion = dialog.expanded.isSelected();
				if (currentExpansion != dialog.initialExpansion)
				{
					if (currentExpansion) ni.setExpanded(); else
						ni.clearExpanded();
					dialog.initialExpansion = currentExpansion;
					changed = true;
				}
			}

			boolean currentEasyToSelect = dialog.easyToSelect.isSelected();
			if (currentEasyToSelect != dialog.initialEasyToSelect)
			{
				if (currentEasyToSelect) ni.clearHardSelect(); else
					ni.setHardSelect();
				dialog.initialEasyToSelect = currentEasyToSelect;
			}

			boolean currentInvisibleOutsideCell = dialog.invisibleOutsideCell.isSelected();
			if (currentInvisibleOutsideCell != dialog.initialInvisibleOutsideCell)
			{
				if (currentInvisibleOutsideCell) ni.setVisInside(); else
					ni.clearVisInside();
				dialog.initialInvisibleOutsideCell = currentInvisibleOutsideCell;
				changed = true;
			}

			boolean currentLocked = dialog.locked.isSelected();
			if (currentLocked != dialog.initialLocked)
			{
				if (currentLocked) ni.setLocked(); else
					ni.clearLocked();
				dialog.initialLocked = currentLocked;
			}

			// handle special node information
			NodeProto.Function fun = ni.getFunction();
			if (fun == NodeProto.Function.DIODE || fun == NodeProto.Function.DIODEZ)
			{
				String currentTextField = dialog.textField.getText();
				if (!currentTextField.equals(dialog.initialTextField))
				{
					ni.newVar("SCHEM_diode", currentTextField);
					dialog.initialTextField = currentTextField;
					changed = true;
				}
			}
			if (fun == NodeProto.Function.RESIST)
			{
				String currentTextField = dialog.textField.getText();
				if (!currentTextField.equals(dialog.initialTextField))
				{
					ni.newVar("SCHEM_resistance", currentTextField);
					dialog.initialTextField = currentTextField;
					changed = true;
				}
			}
			if (fun == NodeProto.Function.CAPAC || fun == NodeProto.Function.ECAPAC)
			{
				String currentTextField = dialog.textField.getText();
				if (!currentTextField.equals(dialog.initialTextField))
				{
					ni.newVar("SCHEM_capacitance", currentTextField);
					dialog.initialTextField = currentTextField;
					changed = true;
				}
			}
			if (fun == NodeProto.Function.INDUCT)
			{
				String currentTextField = dialog.textField.getText();
				if (!currentTextField.equals(dialog.initialTextField))
				{
					ni.newVar("SCHEM_inductance", currentTextField);
					dialog.initialTextField = currentTextField;
					changed = true;
				}
			}
			if (np == Schematics.tech.bboxNode)
			{
				String currentTextField = dialog.textField.getText();
				if (!currentTextField.equals(dialog.initialTextField))
				{
					ni.newVar("SCHEM_function", currentTextField);
					dialog.initialTextField = currentTextField;
					changed = true;
				}
			}
			if (fun == NodeProto.Function.FLIPFLOP)
			{
			}
			if (np == Schematics.tech.globalNode)
			{
				String currentTextField = dialog.textField.getText();
				if (!currentTextField.equals(dialog.initialTextField))
				{
					Variable oldVar = ni.getVar("SCHEM_global_name");
					Variable var = ni.newVar("SCHEM_global_name", currentTextField);
					if (var != null)
					{
						var.setDisplay();
						if (oldVar != null) var.setTextDescriptor(oldVar.getTextDescriptor());
					}
					dialog.initialTextField = currentTextField;
					changed = true;
				}

				String currentCharacteristic = (String)dialog.popup.getSelectedItem();
				if (!currentCharacteristic.equals(dialog.initialPopupEntry))
				{
					PortProto.Characteristic ch = PortProto.Characteristic.findCharacteristic(currentCharacteristic);
					ni.setTechSpecific(ch.getBits());
					dialog.initialPopupEntry = currentCharacteristic;
					changed = true;
				}
			}

			// load color of artwork primitives
			if (np instanceof PrimitiveNode && np.getTechnology() == Artwork.tech)
			{
				String currentColorName = (String)dialog.popup.getSelectedItem();
				if (!currentColorName.equals(dialog.initialPopupEntry))
				{
					int value = EGraphics.findColorIndex(currentColorName);
					ni.newVar(Artwork.ART_COLOR, new Integer(value));
					dialog.initialPopupEntry = currentColorName;
					changed = true;
				}
			}

			// load the degrees of a circle if appropriate
			if (np == Artwork.tech.circleNode || np == Artwork.tech.thickCircleNode)
			{
				String currentTextField = dialog.textField.getText();
				if (!currentTextField.equals(dialog.initialTextField))
				{
					double start = 0;
					double curvature = TextUtils.atof(currentTextField) * Math.PI / 180.0;
					int slashPos = currentTextField.indexOf('/');
					if (slashPos >= 0)
					{
						start = curvature;
						curvature = TextUtils.atof(currentTextField.substring(slashPos+1)) * Math.PI / 180.0;
					}
					ni.setArcDegrees(start, curvature);
					dialog.initialTextField = currentTextField;
					changed = true;
				}
			}

			// update parameter/attribute if it changed
			if (dialog.parameters.isSelected())
			{
				String currentTextField = dialog.listEdit.getText();
				int currentIndex = dialog.listPopup.getSelectedIndex();
				if (!currentTextField.equals(dialog.initialListTextField) ||
					currentIndex != dialog.initialListPopupEntry)
				{
					int index = dialog.list.getSelectedIndex();
					AttValPair avp = (AttValPair)dialog.allParameters.get(index);
					Variable oldVar = ni.getVar(avp.key);
					TextDescriptor oldTD = oldVar.getTextDescriptor();
					Variable var = ni.newVar(avp.key, currentTextField);
					var.lowLevelSetFlags(oldVar.lowLevelGetFlags());
					if (currentIndex == 3) var.setJava(); else var.clearCode();
					TextDescriptor td = var.getTextDescriptor();
					td.lowLevelSet(oldTD.lowLevelGet0(), oldTD.lowLevelGet1());
					dialog.initialListTextField = currentTextField;
					dialog.initialListPopupEntry = currentIndex;
					changed = true;
				}
			}
			if (dialog.attributes.isSelected())
			{
				String currentTextField = dialog.listEdit.getText();
				int currentIndex = dialog.listPopup.getSelectedIndex();
				if (!currentTextField.equals(dialog.initialListTextField) ||
					currentIndex != dialog.initialListPopupEntry)
				{
					int index = dialog.list.getSelectedIndex();
					AttValPair avp = (AttValPair)dialog.allAttributes.get(index);
					Variable oldVar = ni.getVar(avp.key);
					TextDescriptor oldTD = oldVar.getTextDescriptor();
					Variable var = ni.newVar(avp.key, currentTextField);
					var.lowLevelSetFlags(oldVar.lowLevelGetFlags());
					if (currentIndex == 3) var.setJava(); else var.clearCode();
					TextDescriptor td = var.getTextDescriptor();
					td.lowLevelSet(oldTD.lowLevelGet0(), oldTD.lowLevelGet1());
					dialog.initialListTextField = currentTextField;
					dialog.initialListPopupEntry = currentIndex;
					changed = true;
				}
			}

			double currentXPos = Double.parseDouble(dialog.xPos.getText());
			double currentYPos = Double.parseDouble(dialog.yPos.getText());
			double currentXSize = Double.parseDouble(dialog.xSize.getText());
			if (dialog.mirrorX.isSelected()) currentXSize = -currentXSize;
			double currentYSize = Double.parseDouble(dialog.ySize.getText());
			if (dialog.mirrorY.isSelected()) currentYSize = -currentYSize;
			int currentRotation = (int)(Double.parseDouble(dialog.rotation.getText()) * 10);
			if (!EMath.doublesEqual(currentXPos, dialog.initialXPos) ||
				!EMath.doublesEqual(currentYPos, dialog.initialYPos) ||
				!EMath.doublesEqual(currentXSize, dialog.initialXSize) ||
				!EMath.doublesEqual(currentYSize, dialog.initialYSize) ||
				currentRotation != dialog.initialRotation || changed)
			{
				ni.modifyInstance(currentXPos - dialog.initialXPos, currentYPos - dialog.initialYPos,
					currentXSize - dialog.initialXSize, currentYSize - dialog.initialYSize,
					currentRotation - dialog.initialRotation);
				dialog.initialXPos = currentXPos;
				dialog.initialYPos = currentYPos;
				dialog.initialXSize = currentXSize;
				dialog.initialYSize = currentYSize;
				dialog.initialRotation = currentRotation;
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

        expansion = new javax.swing.ButtonGroup();
        selection = new javax.swing.ButtonGroup();
        jLabel1 = new javax.swing.JLabel();
        name = new javax.swing.JTextField();
        cancel = new javax.swing.JButton();
        ok = new javax.swing.JButton();
        type = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        xSize = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
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
        textFieldLabel = new javax.swing.JLabel();
        textField = new javax.swing.JTextField();
        popupLabel = new javax.swing.JLabel();
        popup = new javax.swing.JComboBox();
        ports = new javax.swing.JRadioButton();
        parameters = new javax.swing.JRadioButton();
        attributes = new javax.swing.JRadioButton();
        moreStuffBottom = new javax.swing.JPanel();
        locked = new javax.swing.JCheckBox();
        see = new javax.swing.JButton();
        attributesButton = new javax.swing.JButton();
        listEditLabel = new javax.swing.JLabel();
        listEdit = new javax.swing.JTextField();
        listPopupLabel = new javax.swing.JLabel();
        listPopup = new javax.swing.JComboBox();
        listEvalLabel = new javax.swing.JLabel();
        listEval = new javax.swing.JLabel();
        listPane = new javax.swing.JScrollPane();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Node Information");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

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
        cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cancelActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
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
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);

        type.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(type, gridBagConstraints);

        jLabel3.setText("Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        getContentPane().add(jLabel3, gridBagConstraints);

        jLabel4.setText("X size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        getContentPane().add(jLabel4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(xSize, gridBagConstraints);

        jLabel5.setText("Y size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        getContentPane().add(jLabel5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ySize, gridBagConstraints);

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
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(rotation, gridBagConstraints);

        mirrorX.setText("Mirror X");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(mirrorX, gridBagConstraints);

        more.setText("More");
        more.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                moreActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(more, gridBagConstraints);

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
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(apply, gridBagConstraints);

        mirrorY.setText("Mirror Y");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(mirrorY, gridBagConstraints);

        moreStuffTop.setLayout(new java.awt.GridBagLayout());

        expanded.setText("Expanded");
        expansion.add(expanded);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        moreStuffTop.add(expanded, gridBagConstraints);

        unexpanded.setText("Unexpanded");
        expansion.add(unexpanded);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        moreStuffTop.add(unexpanded, gridBagConstraints);

        easyToSelect.setText("Easy to Select");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        moreStuffTop.add(easyToSelect, gridBagConstraints);

        invisibleOutsideCell.setText("Invisible Outside Cell");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        moreStuffTop.add(invisibleOutsideCell, gridBagConstraints);

        textFieldLabel.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        moreStuffTop.add(textFieldLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        moreStuffTop.add(textField, gridBagConstraints);

        popupLabel.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        moreStuffTop.add(popupLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        moreStuffTop.add(popup, gridBagConstraints);

        ports.setText("Ports:");
        selection.add(ports);
        ports.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                portsActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        moreStuffTop.add(ports, gridBagConstraints);

        parameters.setText("Parameters:");
        selection.add(parameters);
        parameters.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                parametersActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        moreStuffTop.add(parameters, gridBagConstraints);

        attributes.setText("Attributes:");
        selection.add(attributes);
        attributes.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                attributesActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        moreStuffTop.add(attributes, gridBagConstraints);

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
        see.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                seeActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        moreStuffBottom.add(see, gridBagConstraints);

        attributesButton.setText("Attributes");
        attributesButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                attributesButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        moreStuffBottom.add(attributesButton, gridBagConstraints);

        listEditLabel.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        moreStuffBottom.add(listEditLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        moreStuffBottom.add(listEdit, gridBagConstraints);

        listPopupLabel.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        moreStuffBottom.add(listPopupLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        moreStuffBottom.add(listPopup, gridBagConstraints);

        listEvalLabel.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        moreStuffBottom.add(listEvalLabel, gridBagConstraints);

        listEval.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        moreStuffBottom.add(listEval, gridBagConstraints);

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

        pack();
    }//GEN-END:initComponents

	private void attributesButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_attributesButtonActionPerformed
	{//GEN-HEADEREND:event_attributesButtonActionPerformed
		Attributes.showDialog();
	}//GEN-LAST:event_attributesButtonActionPerformed

	private void moreActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_moreActionPerformed
	{//GEN-HEADEREND:event_moreActionPerformed
		bigger = !bigger;
		if (bigger)
		{
			java.awt.GridBagConstraints gridBagConstraints;
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 6;
			gridBagConstraints.gridwidth = 4;
			gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints.weightx = 1.0;
			getContentPane().add(moreStuffTop, gridBagConstraints);

			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 7;
			gridBagConstraints.gridwidth = 4;
			gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
			gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.weighty = 1.0;
			getContentPane().add(listPane, gridBagConstraints);

			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 8;
			gridBagConstraints.gridwidth = 4;
			gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
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
		if (!theDialog.ports.isSelected()) return;
		int currentIndex = theDialog.list.getSelectedIndex();
		ArcInst ai = (ArcInst)theDialog.portObjects.get(currentIndex);
		if (ai == null) return;
		NodeInst ni = theDialog.shownNode;
		Highlight.clear();
		Highlight.addElectricObject(ni, ni.getParent());
		Highlight.addElectricObject(ai, ai.getParent());
		Highlight.finished();
	}//GEN-LAST:event_seeActionPerformed

	private void attributesActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_attributesActionPerformed
	{//GEN-HEADEREND:event_attributesActionPerformed
		theDialog.showProperList();
	}//GEN-LAST:event_attributesActionPerformed

	private void parametersActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_parametersActionPerformed
	{//GEN-HEADEREND:event_parametersActionPerformed
		theDialog.showProperList();
	}//GEN-LAST:event_parametersActionPerformed

	private void portsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_portsActionPerformed
	{//GEN-HEADEREND:event_portsActionPerformed
		theDialog.showProperList();
	}//GEN-LAST:event_portsActionPerformed

	private void applyActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_applyActionPerformed
	{//GEN-HEADEREND:event_applyActionPerformed
		if (shownNode == null) return;
		ChangeNode job = new ChangeNode(shownNode, this);
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
		setVisible(false);
		theDialog = null;
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton apply;
    private javax.swing.JRadioButton attributes;
    private javax.swing.JButton attributesButton;
    private javax.swing.JButton cancel;
    private javax.swing.JCheckBox easyToSelect;
    private javax.swing.JRadioButton expanded;
    private javax.swing.ButtonGroup expansion;
    private javax.swing.JCheckBox invisibleOutsideCell;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JTextField listEdit;
    private javax.swing.JLabel listEditLabel;
    private javax.swing.JLabel listEval;
    private javax.swing.JLabel listEvalLabel;
    private javax.swing.JScrollPane listPane;
    private javax.swing.JComboBox listPopup;
    private javax.swing.JLabel listPopupLabel;
    private javax.swing.JCheckBox locked;
    private javax.swing.JCheckBox mirrorX;
    private javax.swing.JCheckBox mirrorY;
    private javax.swing.JButton more;
    private javax.swing.JPanel moreStuffBottom;
    private javax.swing.JPanel moreStuffTop;
    private javax.swing.JTextField name;
    private javax.swing.JButton ok;
    private javax.swing.JRadioButton parameters;
    private javax.swing.JComboBox popup;
    private javax.swing.JLabel popupLabel;
    private javax.swing.JRadioButton ports;
    private javax.swing.JTextField rotation;
    private javax.swing.JButton see;
    private javax.swing.ButtonGroup selection;
    private javax.swing.JTextField textField;
    private javax.swing.JLabel textFieldLabel;
    private javax.swing.JLabel type;
    private javax.swing.JRadioButton unexpanded;
    private javax.swing.JTextField xPos;
    private javax.swing.JTextField xSize;
    private javax.swing.JTextField yPos;
    private javax.swing.JTextField ySize;
    // End of variables declaration//GEN-END:variables
	
}
