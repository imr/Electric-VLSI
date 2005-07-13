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

import com.sun.electric.database.change.DatabaseChangeEvent;
import com.sun.electric.database.change.DatabaseChangeListener;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.TransistorSize;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.MoCMOS;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.HighlightListener;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.tecEdit.Manipulate;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.ListSelectionModel;


/**
 * Class to handle the "Node Get-Info" dialog.
 */
public class GetInfoNode extends EDialog implements HighlightListener, DatabaseChangeListener
{
	private static GetInfoNode theDialog = null;
	private NodeInst shownNode = null;
	private PortProto shownPort = null;
	private double initialXPos, initialYPos;
	private String initialXSize, initialYSize;
	private boolean initialMirrorX, initialMirrorY;
	private int initialRotation, initialPopupIndex;
//    private TextDescriptor.Code initialListPopupEntry;
	private boolean initialEasyToSelect, initialInvisibleOutsideCell, initialLocked, initialExpansion;
	private String initialName, initialTextField;
	private String initialPopupEntry; //, initialListTextField;
	private DefaultListModel listModel;
	private JList list;
	private List allAttributes;
	private List portObjects;
	private boolean bigger;
	private boolean scalableTrans;
	private boolean swapXY;
    private AttributesTable attributesTable;
    private EditWindow wnd;
    private boolean changingValues = false;;

    private static Preferences prefs = Preferences.userNodeForPackage(GetInfoNode.class);

	/**
	 * Method to show the Node Get-Info dialog.
	 */
	public static void showDialog()
	{
		if (theDialog == null)
		{
            JFrame jf;
            if (TopLevel.isMDIMode())
			    jf = TopLevel.getCurrentJFrame();
            else
                jf = null;
			theDialog = new GetInfoNode(jf, false);
		}
        theDialog.loadInfo();
        if (!theDialog.isVisible()) theDialog.pack();
		theDialog.setVisible(true);
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

//     /**
//      * Respond to database changes
//      * @param batch a batch of changes completed
//      */
//     public void databaseEndChangeBatch(Undo.ChangeBatch batch) {
//         if (!isVisible()) return;

//         // check if we care about the changes
//         boolean reload = false;
//         for (Iterator it = batch.getChanges(); it.hasNext(); ) {
//             Undo.Change change = (Undo.Change)it.next();
//             ElectricObject obj = change.getObject();
//             if (obj == shownNode || obj == shownPort) {
//                 reload = true;
//                 break;
//             }
//         }
//         if (reload) {
//             // update dialog
//             loadInfo();
//         }
//     }

//     /** Don't do anything on little database changes, only after all database changes */
//     public void databaseChanged(Undo.Change change) {}

//     /** This is a GUI listener */
//     public boolean isGUIListener() { return true; }

	/** Creates new form Node Get-Info */
	private GetInfoNode(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
        getRootPane().setDefaultButton(ok);
        setLocation(100, 50);

        Undo.addDatabaseChangeListener(this);

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
		list.addMouseListener(new java.awt.event.MouseAdapter()
		{
			public void mouseClicked(java.awt.event.MouseEvent evt) { listClick(); }
		});
		allAttributes = new ArrayList();
		portObjects = new ArrayList();

        attributesTable = new AttributesTable(null, true, false, false);

        if (buttonSelected == 0)
		    ports.setSelected(true);
        if (buttonSelected == 1)
		    attributes.setSelected(true);

		loadInfo();
		finishInitialization();
	}

	protected void escapePressed() { cancelActionPerformed(null); }

	protected void loadInfo()
	{
        // update current window
        EditWindow curWnd = EditWindow.getCurrent();
        if ((wnd != curWnd) && (curWnd != null)) {
            if (wnd != null) wnd.getHighlighter().removeHighlightListener(this);
            curWnd.getHighlighter().addHighlightListener(this);
            wnd = curWnd;
        }
		// must have a single node selected
		NodeInst ni = null;
		PortProto pp = null;
		int nodeCount = 0;
        if (wnd != null) {
            for(Iterator it = wnd.getHighlighter().getHighlights().iterator(); it.hasNext(); )
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
				attributes.setEnabled(false);
                attributesTable.setElectricObject(null);
                attributesTable.setEnabled(false);
				listPane.setEnabled(false);
				listModel.clear();
				locked.setEnabled(false);
				locked.setSelected(false);
				see.setEnabled(false);
				attributesButton.setEnabled(false);

				shownNode = null;
				shownPort = null;
			}
			return;
		}

		shownNode = ni;
		shownPort = pp;
        //ActivityLogger.logMessage("GetInfoNode loadInfo on "+ni+" or "+pp);

        focusClearOnTextField(name);

		// in small version
		NodeProto np = ni.getProto();
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
        double initXSize = ni.getXSizeWithMirror();
        double initYSize = ni.getYSizeWithMirror();
        initialRotation = ni.getAngle();
        swapXY = false;
        if (initialRotation == 900 || initialRotation == 2700) swapXY = true;

        type.setText(np.describe(true));
        name.setText(initialName);
        xPos.setText(TextUtils.formatDouble(initialXPos));
        yPos.setText(TextUtils.formatDouble(initialYPos));
        boolean realMirrorX = (initXSize < 0);
        boolean realMirrorY = (initYSize < 0);
        SizeOffset so = ni.getSizeOffset();
        if (swapXY)
        {
            xSize.setText(TextUtils.formatDouble(Math.abs(initYSize) - so.getLowYOffset() - so.getHighYOffset()));
            ySize.setText(TextUtils.formatDouble(Math.abs(initXSize) - so.getLowXOffset() - so.getHighXOffset()));
            initialMirrorX = realMirrorY;
            initialMirrorY = realMirrorX;
        } else
        {
            xSize.setText(TextUtils.formatDouble(Math.abs(initXSize) - so.getLowXOffset() - so.getHighXOffset()));
            ySize.setText(TextUtils.formatDouble(Math.abs(initYSize) - so.getLowYOffset() - so.getHighYOffset()));
            initialMirrorX = realMirrorX;
            initialMirrorY = realMirrorY;
        }
        initialXSize = xSize.getText();
        initialYSize = ySize.getText();
        mirrorX.setSelected(initialMirrorX);
        mirrorY.setSelected(initialMirrorY);
		rotation.setText(TextUtils.formatDouble(initialRotation / 10.0));

        // special case for transistors
        TransistorSize transSize = ni.getTransistorSize(null);
        if (transSize != null) {
            xsizeLabel.setText("Width:");
            ysizeLabel.setText("Length:");
            double width = transSize.getDoubleWidth();
            if (width == 0 && transSize.getWidth() != null)
                xSize.setText(transSize.getWidth().toString());
            else
                xSize.setText(TextUtils.formatDouble(width));
            double length = transSize.getDoubleLength();
            if (length == 0 && transSize.getLength() != null)
                ySize.setText(transSize.getLength().toString());
            else
                ySize.setText(TextUtils.formatDouble(length));
            initialXSize = xSize.getText();
            initialYSize = ySize.getText();
        } else {
            xsizeLabel.setText("X size:");
            ysizeLabel.setText("Y size:");
        }

		// in "more" version
		easyToSelect.setEnabled(true);
		invisibleOutsideCell.setEnabled(true);
		ports.setEnabled(true);
		attributes.setEnabled(true);
        attributesTable.setEnabled(true);
		listPane.setEnabled(true);
		locked.setEnabled(true);
		attributesButton.setEnabled(true);

		// grab all attributes and parameters
		allAttributes.clear();

		for(Iterator it = ni.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
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
		attributes.setEnabled(allAttributes.size() != 0);
        attributesTable.setEnabled(allAttributes.size() != 0);
        attributesTable.setElectricObject(ni);
		//if (attributes.isSelected() && allAttributes.size() == 0) ports.setSelected(true);
		showProperList();

		// special lines default to empty
		textFieldLabel.setText("");
		textField.setText("");
		textField.setEditable(false);
		popupLabel.setText("");
		popup.removeAllItems();
		popup.setEnabled(false);

		// see if this node has outline information
//		boolean holdsOutline = false;
		Point2D [] outline = ni.getTrace();
		if (outline != null)
		{
//			holdsOutline = true;
            sizeEditable = false;
		}

		// if there is outline information on a transistor, remember that
		initialTextField = null;
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
		if (np == Schematics.tech.transistorNode || np == Schematics.tech.transistor4Node)
		{
            if (!ni.isFET()) {
                textField.setEditable(true);
                textFieldLabel.setText("Area:");

                Variable var = ni.getVar(Schematics.ATTR_AREA);
//                TransistorSize d = ni.getTransistorSize(null);
//                initialTextField = Double.toString(d.getDoubleWidth());

                textField.setText(var.getPureValue(-1));

                popupLabel.setText("Transistor type:");
                popup.addItem(fun.getName());
            }
		}

		scalableTrans = false;
		if (np instanceof PrimitiveNode)
		{
			if (np.getTechnology() == MoCMOS.tech)
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
			Variable var = ni.getVar(MoCMOS.TRANS_CONTACT, String.class);
			int numContacts = 2;
			boolean insetContacts = false;
			if (var != null)
			{
				String pt = (String)var.getObject();
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


			textFieldLabel.setText("Width:");
			var = ni.getVar(Schematics.ATTR_WIDTH);
			double width = ni.getXSize() - so.getLowXOffset() - so.getHighXOffset();
			if (var != null) width = TextUtils.atof(var.getPureValue(-1));
			initialTextField = Double.toString(width);
			textField.setEditable(true);
			textField.setText(initialTextField);
		}
		if (fun.isResistor()) // == PrimitiveNode.Function.RESIST)
		{
            if (fun == PrimitiveNode.Function.PRESIST)
				textFieldLabel.setText("Poly resistance:"); else
					textFieldLabel.setText("Resistance:");
//			formatinfstr(infstr, x_(" (%s):"),
//				TRANSLATE(us_resistancenames[(us_electricalunits&INTERNALRESUNITS) >> INTERNALRESUNITSSH]));
			Variable var = ni.getVar(Schematics.SCHEM_RESISTANCE);
			if (var == null) initialTextField = "0"; else
				initialTextField = new String(var.getObject().toString());
			textField.setEditable(true);
			textField.setText(initialTextField);
		}
		if (fun.isCapacitor()) // == PrimitiveNode.Function.CAPAC || fun == PrimitiveNode.Function.ECAPAC)
		{
			if (fun == PrimitiveNode.Function.ECAPAC)
				textFieldLabel.setText("Electrolytic cap:"); else
					textFieldLabel.setText("Capacitance:");
//			formatinfstr(infstr, x_(" (%s):"),
//				TRANSLATE(us_capacitancenames[(us_electricalunits&INTERNALCAPUNITS) >> INTERNALCAPUNITSSH]));
			Variable var = ni.getVar(Schematics.SCHEM_CAPACITANCE);
			if (var == null) initialTextField = "0"; else
				initialTextField = new String(var.getObject().toString());
			textField.setEditable(true);
			textField.setText(initialTextField);
		}
		if (fun == PrimitiveNode.Function.INDUCT)
		{
			textFieldLabel.setText("Inductance:");
//			formatinfstr(infstr, x_(" (%s):"),
//				TRANSLATE(us_inductancenames[(us_electricalunits&INTERNALINDUNITS) >> INTERNALINDUNITSSH]));
			Variable var = ni.getVar(Schematics.SCHEM_INDUCTANCE);
			if (var == null) initialTextField = "0"; else
				initialTextField = new String(var.getObject().toString());
			textField.setEditable(true);
			textField.setText(initialTextField);
		}
		if (np == Schematics.tech.bboxNode)
		{
			textFieldLabel.setText("Function:");
			Variable var = ni.getVar(Schematics.SCHEM_FUNCTION);
			if (var == null) initialTextField = ""; else
				initialTextField = new String(var.getObject().toString());
			textField.setEditable(true);
			textField.setText(initialTextField);
		}
		if (fun.isFlipFlop())
		{
			popupLabel.setText("Flip-flop type:");
			popup.addItem(fun.getName());
		}
		if (np == Schematics.tech.globalNode)
		{
			textFieldLabel.setText("Global name:");
			Variable var = ni.getVar(Schematics.SCHEM_GLOBAL_NAME);
			if (var == null) initialTextField = ""; else
				initialTextField = new String(var.getObject().toString());
			textField.setEditable(true);
			textField.setText(initialTextField);

			popupLabel.setText("Characteristics:");
			List characteristics = PortCharacteristic.getOrderedCharacteristics();
			for(Iterator it = characteristics.iterator(); it.hasNext(); )
			{
				PortCharacteristic ch = (PortCharacteristic)it.next();
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
		} else
		{
			// load color of artwork primitives
			if (np instanceof PrimitiveNode && np.getTechnology() == Artwork.tech)
			{
				popupLabel.setText("Color:");
				int [] colors = EGraphics.getColorIndices();
				for(int i=0; i<colors.length; i++)
					popup.addItem(EGraphics.getColorIndexName(colors[i]));
				int index = EGraphics.BLACK;
				Variable var = ni.getVar(Artwork.ART_COLOR);
				if (var != null) index = ((Integer)var.getObject()).intValue();
				initialPopupEntry = EGraphics.getColorIndexName(index);
				popup.setSelectedItem(initialPopupEntry);
				popup.setEnabled(true);
			}
		}

		// load the degrees of a circle if appropriate
		if (np == Artwork.tech.circleNode || np == Artwork.tech.thickCircleNode)
		{
			double [] arcData = ni.getArcDegrees();
			double start = DBMath.round(arcData[0] * 180.0 / Math.PI);
			double curvature = DBMath.round(arcData[1] * 180.0 / Math.PI);
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
        focusOnTextField(name);
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
				PortCharacteristic ch = pp.getCharacteristic();
				String description;
				if (ch == PortCharacteristic.UNKNOWN) description = "Port "; else
					description = ch.getName() + " port ";
				description += pp.getName() + " connects to";
				ArcProto [] connList = pp.getBasePort().getConnections();
				int count = 0;
				for(int i=0; i<connList.length; i++)
				{
					ArcProto ap = connList[i];
					if ((np instanceof Cell || np.getTechnology() != Generic.tech) &&
						ap.getTechnology() == Generic.tech) continue;
					if (count > 0) description += ",";
					description += " " + ap.getName();
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
						") to " + ai;
					listModel.addElement(description);
					portObjects.add(ai);
				}

				// talk about any exports of this prototype
				for(Iterator eIt = shownNode.getExports(); eIt.hasNext(); )
				{
					Export e = (Export)eIt.next();
					if (e.getOriginalPort() != pi) continue;
					description = "  Available as " + e.getCharacteristic().getName() + " export '" + e.getName() + "'";
					listModel.addElement(description);
					portObjects.add(null);
				}
			}
			see.setEnabled(true);
            listPane.setViewportView(list);
            list.setSelectedIndex(0);
            listClick();
		}
		if (attributes.isSelected())
		{
            listPane.setViewportView(attributesTable);
		}
	}

	private void listClick()
	{
//		int index = list.getSelectedIndex();
//		if (attributes.isSelected())
//		{
//			AttributesTable.AttValPair avp = (AttributesTable.AttValPair)allAttributes.get(index);
//			listEditLabel.setText("Attribute '" + avp.trueName + "'");
//			initialListTextField = new String(avp.value);
//			listEdit.setText(initialListTextField);
//			if (avp.code)
//			{
//				initialListPopupEntry = TextDescriptor.Code.JAVA;
//				listEvalLabel.setText("Evaluation:");
//				Variable var = shownNode.getVar(avp.key);
//				listEval.setText(var.describe(-1, VarContext.globalContext, shownNode));
//			} else
//			{
//				initialListPopupEntry = TextDescriptor.Code.NONE;
//				listEvalLabel.setText("");
//				listEval.setText("");
//			}
//			listPopup.setSelectedItem(initialListPopupEntry);
//		}
	}

	private static class ChangeNode extends Job
	{
		NodeInst ni;
		GetInfoNode dialog;

		protected ChangeNode(NodeInst ni, GetInfoNode dialog)
		{
			super("Modify Node", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.ni = ni;
			this.dialog = dialog;
			startJob();
		}

		public boolean doIt()
		{
			boolean changed = false;
			NodeProto np = ni.getProto();

			dialog.changingValues = true;
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
			if (dialog.scalableTrans)
			{
				int index = dialog.popup.getSelectedIndex();
				if (index != dialog.initialPopupIndex)
				{
					int numContacts = 2 - (index / 2);
					boolean inset = (index&1) != 0;
					String contactInfo = String.valueOf(numContacts);
					if (inset) contactInfo += "i";
					ni.newVar(MoCMOS.TRANS_CONTACT, contactInfo);
				}

				String currentTextField = dialog.textField.getText();
				if (!currentTextField.equals(dialog.initialTextField))
				{
					double width = TextUtils.atof(currentTextField);
					Variable oldVar = ni.getVar(Schematics.ATTR_WIDTH);
					Variable var = ni.updateVar(Schematics.ATTR_WIDTH, new Double(width));
					if (var != null && oldVar == null)
					{
						var.setDisplay(true);
						var.setDispPart(TextDescriptor.DispPos.NAMEVALUE);
					}
					dialog.initialTextField = currentTextField;
				}
			}
			PrimitiveNode.Function fun = ni.getFunction();
			if (fun == PrimitiveNode.Function.DIODE || fun == PrimitiveNode.Function.DIODEZ)
			{
				String currentTextField = dialog.textField.getText();
				if (!currentTextField.equals(dialog.initialTextField))
				{
					Variable var = ni.updateVar(Schematics.SCHEM_DIODE, currentTextField);
					if (var != null) var.setDisplay(true);
					dialog.initialTextField = currentTextField;
					changed = true;
				}
			}
			if (fun.isResistor()) // == PrimitiveNode.Function.RESIST)
			{
				String currentTextField = dialog.textField.getText();
				if (!currentTextField.equals(dialog.initialTextField))
				{
					Variable var = ni.updateVar(Schematics.SCHEM_RESISTANCE, currentTextField);
					if (var != null) var.setDisplay(true);
					dialog.initialTextField = currentTextField;
					changed = true;
				}
			}
			if (fun.isCapacitor()) // == PrimitiveNode.Function.CAPAC || fun == PrimitiveNode.Function.ECAPAC)
			{
				String currentTextField = dialog.textField.getText();
				if (!currentTextField.equals(dialog.initialTextField))
				{
					Variable var = ni.updateVar(Schematics.SCHEM_CAPACITANCE, currentTextField);
					if (var != null) var.setDisplay(true);
					dialog.initialTextField = currentTextField;
					changed = true;
				}
			}
			if (fun == PrimitiveNode.Function.INDUCT)
			{
				String currentTextField = dialog.textField.getText();
				if (!currentTextField.equals(dialog.initialTextField))
				{
					Variable var = ni.updateVar(Schematics.SCHEM_INDUCTANCE, currentTextField);
					if (var != null) var.setDisplay(true);
					dialog.initialTextField = currentTextField;
					changed = true;
				}
			}
			if (np == Schematics.tech.bboxNode)
			{
				String currentTextField = dialog.textField.getText();
				if (!currentTextField.equals(dialog.initialTextField))
				{
					Variable var = ni.updateVar(Schematics.SCHEM_FUNCTION, currentTextField);
					if (var != null) var.setDisplay(true);
					dialog.initialTextField = currentTextField;
					changed = true;
				}
			}
			if (np == Schematics.tech.globalNode)
			{
				String currentTextField = dialog.textField.getText();
				if (!currentTextField.equals(dialog.initialTextField))
				{
					Variable var = ni.updateVar(Schematics.SCHEM_GLOBAL_NAME, currentTextField);
					if (var != null) var.setDisplay(true);
					dialog.initialTextField = currentTextField;
					changed = true;
				}

				String currentCharacteristic = (String)dialog.popup.getSelectedItem();
				if (!currentCharacteristic.equals(dialog.initialPopupEntry))
				{
					PortCharacteristic ch = PortCharacteristic.findCharacteristic(currentCharacteristic);
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

			SizeOffset so = ni.getSizeOffset();
			double currentXPos = TextUtils.atof(dialog.xPos.getText(), new Double(dialog.initialXPos));
			double currentYPos = TextUtils.atof(dialog.yPos.getText(), new Double(dialog.initialYPos));
			double currentXSize = 0, currentYSize = 0;
            double initXSize = 0, initYSize = 0;

            // Figure out change in X and Y size
            // if swapXY, X size was put in Y text box, and vice versa.
            if (dialog.swapXY)
            {
                // get true size minus offset (this is the size the user sees)
                currentXSize = TextUtils.atof(dialog.ySize.getText(), new Double(ni.getXSize() - (so.getLowXOffset() + so.getHighXOffset())));
                currentYSize = TextUtils.atof(dialog.xSize.getText(), new Double(ni.getYSize() - (so.getLowYOffset() + so.getHighYOffset())));
                initXSize = TextUtils.atof(dialog.initialYSize, new Double(currentXSize));
                initYSize = TextUtils.atof(dialog.initialXSize, new Double(currentYSize));
                // bloat by offset
                currentXSize += (so.getLowXOffset() + so.getHighXOffset());
                currentYSize += (so.getLowYOffset() + so.getHighYOffset());
                initXSize += (so.getLowXOffset() + so.getHighXOffset());
                initYSize += (so.getLowYOffset() + so.getHighYOffset());
                // mirror
				if (dialog.mirrorX.isSelected()) currentYSize = -currentYSize;
				if (dialog.mirrorY.isSelected()) currentXSize = -currentXSize;
				if (dialog.initialMirrorX) initYSize = -initYSize;
				if (dialog.initialMirrorY) initXSize = -initXSize;
            } else
            {
                currentXSize = TextUtils.atof(dialog.xSize.getText(), new Double(ni.getXSize() - (so.getLowXOffset() + so.getHighXOffset())));
                currentYSize = TextUtils.atof(dialog.ySize.getText(), new Double(ni.getYSize() - (so.getLowYOffset() + so.getHighYOffset())));
                initXSize = TextUtils.atof(dialog.initialXSize, new Double(currentXSize));
                initYSize = TextUtils.atof(dialog.initialYSize, new Double(currentYSize));
                // bloat by offset
                currentXSize += (so.getLowXOffset() + so.getHighXOffset());
                currentYSize += (so.getLowYOffset() + so.getHighYOffset());
                initXSize += (so.getLowXOffset() + so.getHighXOffset());
                initYSize += (so.getLowYOffset() + so.getHighYOffset());
                // mirror
                if (dialog.mirrorX.isSelected()) currentXSize = -currentXSize;
                if (dialog.mirrorY.isSelected()) currentYSize = -currentYSize;
				if (dialog.initialMirrorX) initXSize = -initXSize;
				if (dialog.initialMirrorY) initYSize = -initYSize;
            }

            // The following code is specific for transistors, and uses the X/Y size fields for
            // Width and Length, and therefore may override the values such that the node size does not
            // get set by them.
            if (ni.getTransistorSize(null) != null) {

                // see if this is a schematic transistor
                if (np == Schematics.tech.transistorNode || np == Schematics.tech.transistor4Node)
                {
                    if (ni.isFET())
					{
                        Object width, length;
                        // see if we can convert width and length to a Number
                        double w = TextUtils.atof(dialog.xSize.getText(), null);
                        if (w == 0) {
                            // set width to whatever text is there
                            width = dialog.xSize.getText();
                        } else {
                            width = new Double(w);
                        }

                        double l = TextUtils.atof(dialog.ySize.getText(), null);
                        if (l == 0) {
                            // set length to whatever text is there
                            length = dialog.ySize.getText();
                        } else {
                            length = new Double(l);
                        }
                        ni.setTransistorSize(width, length);
                    }
                } else
                {
                    // this is a layout transistor
                    if (ni.isSerpentineTransistor()) {
                        // serpentine transistors can only set length
                        double initialLength = ni.getSerpentineTransistorLength();
                        double length = TextUtils.atof(dialog.ySize.getText(), new Double(initialLength));
                        if (length != initialLength)
                            ni.setSerpentineTransistorLength(length);
                    } else {
                        // set length and width by node size for layout transistors
                        double initialWidth = ni.getTransistorSize(null).getDoubleWidth();
                        double initialLength = ni.getTransistorSize(null).getDoubleLength();
                        double width = TextUtils.atof(dialog.xSize.getText(), new Double(initialWidth));
                        double length = TextUtils.atof(dialog.ySize.getText(), new Double(initialLength));
                        if (!DBMath.doublesEqual(width, initialWidth) ||
                            !DBMath.doublesEqual(length, initialLength))
                        {
                            // set transistor size
                            ni.setTransistorSize(width, length);
                        }
                    }
                }
                // ignore size change, but retain mirroring change (sign)
                currentXSize = initXSize = ni.getXSize();
                currentYSize = initYSize = ni.getYSize();
                if (dialog.swapXY) {
                    if (dialog.mirrorX.isSelected()) currentYSize = -currentYSize;
                    if (dialog.mirrorY.isSelected()) currentXSize = -currentXSize;
                    if (dialog.initialMirrorX) initYSize = -initYSize;
                    if (dialog.initialMirrorY) initXSize = -initXSize;
                } else {
                    if (dialog.mirrorX.isSelected()) currentXSize = -currentXSize;
                    if (dialog.mirrorY.isSelected()) currentYSize = -currentYSize;
                    if (dialog.initialMirrorX) initXSize = -initXSize;
                    if (dialog.initialMirrorY) initYSize = -initYSize;
                }
            }

			int currentRotation = (int)(TextUtils.atof(dialog.rotation.getText(), new Double(dialog.initialRotation)) * 10);
			if (!DBMath.doublesEqual(currentXPos, dialog.initialXPos) ||
				!DBMath.doublesEqual(currentYPos, dialog.initialYPos) ||
				!DBMath.doublesEqual(currentXSize, initXSize) ||
				!DBMath.doublesEqual(currentYSize, initYSize) ||
				currentRotation != dialog.initialRotation || changed)
			{
				ni.modifyInstance(DBMath.round(currentXPos - dialog.initialXPos), DBMath.round(currentYPos - dialog.initialYPos),
					DBMath.round(currentXSize - initXSize), DBMath.round(currentYSize - initYSize),
					currentRotation - dialog.initialRotation);
				dialog.initialXPos = currentXPos;
				dialog.initialYPos = currentYPos;
				dialog.initialXSize = dialog.xSize.getText();
				dialog.initialYSize = dialog.ySize.getText();
				dialog.initialMirrorX = dialog.mirrorX.isSelected();
				dialog.initialMirrorY = dialog.mirrorY.isSelected();
				dialog.initialRotation = currentRotation;
			}

			dialog.changingValues = false;

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

        expansion = new javax.swing.ButtonGroup();
        selection = new javax.swing.ButtonGroup();
        jLabel1 = new javax.swing.JLabel();
        name = new javax.swing.JTextField();
        cancel = new javax.swing.JButton();
        ok = new javax.swing.JButton();
        type = new javax.swing.JLabel();
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
        textFieldLabel = new javax.swing.JLabel();
        textField = new javax.swing.JTextField();
        popupLabel = new javax.swing.JLabel();
        popup = new javax.swing.JComboBox();
        ports = new javax.swing.JRadioButton();
        attributes = new javax.swing.JRadioButton();
        moreStuffBottom = new javax.swing.JPanel();
        locked = new javax.swing.JCheckBox();
        see = new javax.swing.JButton();
        attributesButton = new javax.swing.JButton();
        listPane = new javax.swing.JScrollPane();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Node Properties");
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
        gridBagConstraints.gridx = 2;
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

        selection.add(ports);
        ports.setText("Ports:");
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

        selection.add(attributes);
        attributes.setText("Attributes:");
        attributes.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                attributesActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
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
		theDialog.showProperList();
	}//GEN-LAST:event_attributesActionPerformed

	private void portsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_portsActionPerformed
	{//GEN-HEADEREND:event_portsActionPerformed
		theDialog.showProperList();
	}//GEN-LAST:event_portsActionPerformed

	private void applyActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_applyActionPerformed
	{//GEN-HEADEREND:event_applyActionPerformed
		if (shownNode == null) return;
		ChangeNode job = new ChangeNode(shownNode, this);
        attributesTable.applyChanges();
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
		setVisible(false);
		//theDialog = null;
        //Highlight.removeHighlightListener(this);
        //Undo.removeDatabaseChangeListener(this);
		//dispose();
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
    private javax.swing.JTextField textField;
    private javax.swing.JLabel textFieldLabel;
    private javax.swing.JLabel type;
    private javax.swing.JRadioButton unexpanded;
    private javax.swing.JTextField xPos;
    private javax.swing.JTextField xSize;
    private javax.swing.JLabel xsizeLabel;
    private javax.swing.JTextField yPos;
    private javax.swing.JTextField ySize;
    private javax.swing.JLabel ysizeLabel;
    // End of variables declaration//GEN-END:variables

}
