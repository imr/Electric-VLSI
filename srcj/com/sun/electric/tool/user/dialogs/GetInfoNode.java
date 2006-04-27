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
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
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
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveNodeSize;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.MoCMOS;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Client;
import com.sun.electric.tool.user.Highlight2;
import com.sun.electric.tool.user.HighlightListener;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.UserInterfaceMain;
import com.sun.electric.tool.user.tecEdit.Manipulate;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.Frame;
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
	private boolean initialEasyToSelect, initialInvisibleOutsideCell, initialLocked, initialExpansion;
	private String initialName, initialTextField;
	private String initialPopupEntry;
	private DefaultListModel listModel;
	private JList list;
	private List<AttributesTable.AttValPair> allAttributes;
	private List<ArcInst> portObjects;
	private boolean bigger;
	private boolean scalableTrans;
	private boolean swapXY;
    private AttributesTable attributesTable;
    private EditWindow wnd;

    private static Preferences prefs = Preferences.userNodeForPackage(GetInfoNode.class);

	/**
	 * Method to show the Node Get-Info dialog.
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
            JFrame jf;
            if (TopLevel.isMDIMode())
			    jf = TopLevel.getCurrentJFrame();
            else
                jf = null;
			theDialog = new GetInfoNode(jf, false);
		}
        theDialog.loadInfo();

        if (!theDialog.isVisible()) {
            theDialog.pack();
        }
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
//             Undo.Change change = it.next();
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
	private GetInfoNode(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
        getRootPane().setDefaultButton(ok);

        UserInterfaceMain.addDatabaseChangeListener(this);
        Highlighter.addHighlightListener(this);

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
		allAttributes = new ArrayList<AttributesTable.AttValPair>();
		portObjects = new ArrayList<ArcInst>();

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
        if (curWnd != null) wnd = curWnd;

		// must have a single node selected
		NodeInst ni = null;
		PortProto pp = null;
		int nodeCount = 0;
        if (wnd != null) {
            for(Highlight2 h : wnd.getHighlighter().getHighlights())
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
				colorAndPattern.setEnabled(false);

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
        double initXSize = ni.getXSize();
        double initYSize = ni.getYSize();
//        double initXSize = ni.getXSizeWithMirror();
//        double initYSize = ni.getYSizeWithMirror();
        initialRotation = ni.getAngle();
        swapXY = false;
        if (initialRotation == 900 || initialRotation == 2700) swapXY = true;

        type.setText(np.describe(true));
        name.setText(initialName);
        xPos.setText(TextUtils.formatDouble(initialXPos));
        yPos.setText(TextUtils.formatDouble(initialYPos));
        boolean realMirrorX = ni.isXMirrored();
        boolean realMirrorY = ni.isYMirrored();
//        boolean realMirrorX = (initXSize < 0);
//        boolean realMirrorY = (initYSize < 0);
        SizeOffset so = ni.getSizeOffset();
        if (swapXY)
        {
            xSize.setText(TextUtils.formatDouble(initYSize - so.getLowYOffset() - so.getHighYOffset()));
            ySize.setText(TextUtils.formatDouble(initXSize - so.getLowXOffset() - so.getHighXOffset()));
//            xSize.setText(TextUtils.formatDouble(Math.abs(initYSize) - so.getLowYOffset() - so.getHighYOffset()));
//            ySize.setText(TextUtils.formatDouble(Math.abs(initXSize) - so.getLowXOffset() - so.getHighXOffset()));
            initialMirrorX = realMirrorY;
            initialMirrorY = realMirrorX;
        } else
        {
            xSize.setText(TextUtils.formatDouble(initXSize - so.getLowXOffset() - so.getHighXOffset()));
            ySize.setText(TextUtils.formatDouble(initYSize - so.getLowYOffset() - so.getHighYOffset()));
//            xSize.setText(TextUtils.formatDouble(Math.abs(initXSize) - so.getLowXOffset() - so.getHighXOffset()));
//            ySize.setText(TextUtils.formatDouble(Math.abs(initYSize) - so.getLowYOffset() - so.getHighYOffset()));
            initialMirrorX = realMirrorX;
            initialMirrorY = realMirrorY;
        }
        initialXSize = xSize.getText();
        initialYSize = ySize.getText();
        mirrorX.setSelected(initialMirrorX);
        mirrorY.setSelected(initialMirrorY);
		rotation.setText(TextUtils.formatDouble(initialRotation / 10.0));

        // special case for transistors and resistors
        PrimitiveNodeSize npSize = ni.getPrimitiveNodeSize(null);
        if (npSize != null) {
            xsizeLabel.setText("Width:");
            ysizeLabel.setText("Length:");
            double width = npSize.getDoubleWidth();
            if (width == 0 && npSize.getWidth() != null)
                xSize.setText(npSize.getWidth().toString());
            else
                xSize.setText(TextUtils.formatDouble(width));
            double length = npSize.getDoubleLength();
            if (length == 0 && npSize.getLength() != null)
                ySize.setText(npSize.getLength().toString());
            else
                ySize.setText(TextUtils.formatDouble(length));
            initialXSize = xSize.getText();
            initialYSize = ySize.getText();
//        } else if (ni.getFunction()==PrimitiveNode.Function.PRESIST) {
//        	// special case for Poly resistors
//        	xsizeLabel.setText("Length:");
//			ysizeLabel.setText("Width:");
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
		colorAndPattern.setEnabled(ni.getProto().getTechnology() == Artwork.tech);

		// grab all attributes and parameters
		allAttributes.clear();

		for(Iterator<Variable> it = ni.getVariables(); it.hasNext(); )
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
		attributes.setEnabled(allAttributes.size() != 0);
        attributesTable.setEnabled(allAttributes.size() != 0);
        attributesTable.setElectricObject(ni);
		if (attributes.isSelected() && allAttributes.size() == 0) ports.setSelected(true);
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
		if (!ni.isCellInstance())
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
            listPane.setViewportView(list);
			NodeProto np = shownNode.getProto();
			List<String> portMessages = new ArrayList<String>();
			for(Iterator<PortInst> it = shownNode.getPortInsts(); it.hasNext(); )
			{
				PortInst pi = it.next();
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

				// mention if it is highlighted
				if (pp == shownPort)
				{
					portMessages.add("  Highlighted port");
					portObjects.add(null);
				}

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
//			AttributesTable.AttValPair avp = allAttributes.get(index);
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
		private NodeInst ni;
		private double initialXPos, initialYPos, currentXPos, currentYPos;
		private String initialXSize, initialYSize, currentXSize, currentYSize;
		private boolean initialMirrorX, initialMirrorY, currentMirrorX, currentMirrorY;
		private int initialRotation, currentRotation;
		private int initialPopupIndex, currentPopupIndex;
		private boolean initialEasyToSelect, currentEasyToSelect;
		private boolean initialInvisibleOutsideCell, currentInvisibleOutsideCell;
		private boolean initialLocked, currentLocked;
		private boolean initialExpansion, currentExpansion;
		private String initialName, currentName;
		private String initialTextField, currentTextField;
		private String initialPopupEntry, currentPopupEntry;
		private boolean bigger;
		private boolean scalableTrans;
		private boolean swapXY;

		public ChangeNode(NodeInst ni,
			double initialXPos, double currentXPos, double initialYPos, double currentYPos,
			String initialXSize, String currentXSize, String initialYSize, String currentYSize,
			boolean initialMirrorX, boolean currentMirrorX, boolean initialMirrorY, boolean currentMirrorY,
			int initialRotation, int currentRotation,
			int initialPopupIndex, int currentPopupIndex,
			boolean initialEasyToSelect, boolean currentEasyToSelect,
			boolean initialInvisibleOutsideCell, boolean currentInvisibleOutsideCell,
			boolean initialLocked, boolean currentLocked,
			boolean initialExpansion, boolean currentExpansion,
			String initialName, String currentName,
			String initialTextField, String currentTextField,
			String initialPopupEntry, String currentPopupEntry,
			boolean bigger,
			boolean scalableTrans,
			boolean swapXY)
		{
			super("Modify Node", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.ni = ni;
			this.initialXPos = initialXPos;                                   this.currentXPos = currentXPos;
			this.initialYPos = initialYPos;                                   this.currentYPos = currentYPos;
			this.initialXSize = initialXSize;                                 this.currentXSize = currentXSize;
			this.initialYSize = initialYSize;                                 this.currentYSize = currentYSize;
			this.initialMirrorX = initialMirrorX;                             this.currentMirrorX = currentMirrorX;
			this.initialMirrorY = initialMirrorY;                             this.currentMirrorY = currentMirrorY;
			this.initialRotation = initialRotation;                           this.currentRotation = currentRotation;
			this.initialPopupIndex = initialPopupIndex;                       this.currentPopupIndex = currentPopupIndex;
			this.initialEasyToSelect = initialEasyToSelect;                   this.currentEasyToSelect = currentEasyToSelect;
			this.initialInvisibleOutsideCell = initialInvisibleOutsideCell;   this.currentInvisibleOutsideCell = currentInvisibleOutsideCell;
			this.initialLocked = initialLocked;                               this.currentLocked = currentLocked;
			this.initialExpansion = initialExpansion;                         this.currentExpansion = currentExpansion;
			this.initialName = initialName;                                   this.currentName = currentName;
			this.initialTextField = initialTextField;                         this.currentTextField = currentTextField;
			this.initialPopupEntry = initialPopupEntry;                       this.currentPopupEntry = currentPopupEntry;
			this.bigger = bigger;
			this.scalableTrans = scalableTrans;
			this.swapXY = swapXY;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			boolean changed = false;
			NodeProto np = ni.getProto();

			if (!currentName.equals(initialName))
			{
				if (currentName.length() == 0) currentName = null;
				ni.setName(currentName);
				changed = true;
			}

			if (ni.isCellInstance())
			{
				if (currentExpansion != initialExpansion)
				{
					if (currentExpansion) ni.setExpanded(); else
						ni.clearExpanded();
					changed = true;
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
					ni.newVar(MoCMOS.TRANS_CONTACT, contactInfo);
				}

				if (!currentTextField.equals(initialTextField))
				{
					double width = TextUtils.atof(currentTextField);
					Variable oldVar = ni.getVar(Schematics.ATTR_WIDTH);
					Variable var = ni.updateVar(Schematics.ATTR_WIDTH, new Double(width));
					if (var != null && oldVar == null)
					{
                        ni.addVar(var.withDisplay(true).withDispPart(TextDescriptor.DispPos.NAMEVALUE));
					}
				}
			}
			PrimitiveNode.Function fun = ni.getFunction();
			if (fun == PrimitiveNode.Function.DIODE || fun == PrimitiveNode.Function.DIODEZ)
			{
				if (!currentTextField.equals(initialTextField))
				{
					Variable var = ni.updateVar(Schematics.SCHEM_DIODE, currentTextField);
                    if (var != null && !var.isDisplay()) ni.addVar(var.withDisplay(true));
					changed = true;
				}
			}
			if (fun.isResistor())
			{
				if (!currentTextField.equals(initialTextField))
				{
					Variable var = ni.updateVar(Schematics.SCHEM_RESISTANCE, currentTextField);
                    if (var != null && !var.isDisplay()) ni.addVar(var.withDisplay(true));
					changed = true;
				}
			}
			if (fun.isCapacitor())
			{
				if (!currentTextField.equals(initialTextField))
				{
					Variable var = ni.updateVar(Schematics.SCHEM_CAPACITANCE, currentTextField);
                    if (var != null && !var.isDisplay()) ni.addVar(var.withDisplay(true));
					changed = true;
				}
			}
			if (fun == PrimitiveNode.Function.INDUCT)
			{
				if (!currentTextField.equals(initialTextField))
				{
					Variable var = ni.updateVar(Schematics.SCHEM_INDUCTANCE, currentTextField);
                    if (var != null && !var.isDisplay()) ni.addVar(var.withDisplay(true));
					changed = true;
				}
			}
			if (np == Schematics.tech.bboxNode)
			{
				if (!currentTextField.equals(initialTextField))
				{
					Variable var = ni.updateVar(Schematics.SCHEM_FUNCTION, currentTextField);
                    if (var != null && !var.isDisplay()) ni.addVar(var.withDisplay(true));
					changed = true;
				}
			}
			if (np == Schematics.tech.globalNode)
			{
				if (!currentTextField.equals(initialTextField))
				{
					Variable var = ni.updateVar(Schematics.SCHEM_GLOBAL_NAME, currentTextField);
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
			if (np == Artwork.tech.circleNode || np == Artwork.tech.thickCircleNode)
			{
				if (!currentTextField.equals(initialTextField))
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
					changed = true;
				}
			}

			SizeOffset so = ni.getSizeOffset();
            double initXSize = 0, initYSize = 0;
    		double currXSize, currYSize;

            // Figure out change in X and Y size
            // if swapXY, X size was put in Y text box, and vice versa.
            if (swapXY)
            {
                // get true size minus offset (this is the size the user sees)
                currXSize = TextUtils.atof(currentYSize, new Double(ni.getXSize() - (so.getLowXOffset() + so.getHighXOffset())));
                currYSize = TextUtils.atof(currentXSize, new Double(ni.getYSize() - (so.getLowYOffset() + so.getHighYOffset())));
                initXSize = TextUtils.atof(initialYSize, new Double(currXSize));
                initYSize = TextUtils.atof(initialXSize, new Double(currYSize));
                // bloat by offset
                currXSize += (so.getLowXOffset() + so.getHighXOffset());
                currYSize += (so.getLowYOffset() + so.getHighYOffset());
                initXSize += (so.getLowXOffset() + so.getHighXOffset());
                initYSize += (so.getLowYOffset() + so.getHighYOffset());
                // mirror
				if (currentMirrorX) currYSize = -currYSize;
				if (currentMirrorY) currXSize = -currXSize;
				if (initialMirrorX) initYSize = -initYSize;
				if (initialMirrorY) initXSize = -initXSize;
            } else
            {
                currXSize = TextUtils.atof(currentXSize, new Double(ni.getXSize() - (so.getLowXOffset() + so.getHighXOffset())));
                currYSize = TextUtils.atof(currentYSize, new Double(ni.getYSize() - (so.getLowYOffset() + so.getHighYOffset())));
                initXSize = TextUtils.atof(initialXSize, new Double(currXSize));
                initYSize = TextUtils.atof(initialYSize, new Double(currYSize));
                // bloat by offset
                currXSize += (so.getLowXOffset() + so.getHighXOffset());
                currYSize += (so.getLowYOffset() + so.getHighYOffset());
                initXSize += (so.getLowXOffset() + so.getHighXOffset());
                initYSize += (so.getLowYOffset() + so.getHighYOffset());
                // mirror
                if (currentMirrorX) currXSize = -currXSize;
                if (currentMirrorY) currYSize = -currYSize;
				if (initialMirrorX) initXSize = -initXSize;
				if (initialMirrorY) initYSize = -initYSize;
            }

            // The following code is specific for transistors, and uses the X/Y size fields for
            // Width and Length, and therefore may override the values such that the node size does not
            // get set by them.
            PrimitiveNodeSize size = ni.getPrimitiveNodeSize(null);
            if (size != null)
            {
                // see if this is a schematic transistor
                if (np == Schematics.tech.transistorNode || np == Schematics.tech.transistor4Node ||
                    np == Schematics.tech.resistorNode)
                {
                    Object width, length;
                    if (ni.isFET() || ni.getFunction() == PrimitiveNode.Function.PRESIST)
					{
                        // see if we can convert width and length to a Number
                        double w = TextUtils.atof(currentXSize, null);
                        if (w == 0) {
                            // set width to whatever text is there
                            width = currentXSize;
                        } else {
                            width = new Double(w);
                        }

                        double l = TextUtils.atof(currentYSize, null);
                        if (l == 0) {
                            // set length to whatever text is there
                            length = currentYSize;
                        } else {
                            length = new Double(l);
                        }
                        ni.setPrimitiveNodeSize(width, length);
                    }
                } else // transistors or resistors
                {
                    // this is a layout transistor
                    if (ni.isSerpentineTransistor()) {
                        // serpentine transistors can only set length
                        double initialLength = ni.getSerpentineTransistorLength();
                        double length = TextUtils.atof(currentYSize, new Double(initialLength));
                        if (length != initialLength)
                            ni.setSerpentineTransistorLength(length);
                    } else {
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
                currXSize = initXSize = ni.getXSize();
                currYSize = initYSize = ni.getYSize();
                if (swapXY) {
                    if (currentMirrorX) currYSize = -currYSize;
                    if (currentMirrorY) currXSize = -currXSize;
                    if (initialMirrorX) initYSize = -initYSize;
                    if (initialMirrorY) initXSize = -initXSize;
                } else {
                    if (currentMirrorX) currXSize = -currXSize;
                    if (currentMirrorY) currYSize = -currYSize;
                    if (initialMirrorX) initXSize = -initXSize;
                    if (initialMirrorY) initYSize = -initYSize;
                }
            }

			if (!DBMath.doublesEqual(currentXPos, initialXPos) ||
				!DBMath.doublesEqual(currentYPos, initialYPos) ||
				!DBMath.doublesEqual(currXSize, initXSize) ||
				!DBMath.doublesEqual(currYSize, initYSize) ||
				currentRotation != initialRotation || changed)
			{
                Orientation orient = Orientation.fromJava(currentRotation,
                        currXSize < 0 || currXSize == 0 && 1/currXSize < 0,
                        currYSize < 0 || currYSize == 0 && 1/currYSize < 0);
                orient = orient.concatenate(ni.getOrient().inverse());
				ni.modifyInstance(DBMath.round(currentXPos - initialXPos), DBMath.round(currentYPos - initialYPos),
					DBMath.round(Math.abs(currXSize) - Math.abs(initXSize)),
                    DBMath.round(Math.abs(currYSize) - Math.abs(initYSize)), orient);
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
    private void initComponents()
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
        colorAndPattern = new javax.swing.JButton();
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

            public void windowActivated(java.awt.event.WindowEvent evt)
            {
//                System.out.println("DD");
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
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        moreStuffBottom.add(attributesButton, gridBagConstraints);

        colorAndPattern.setText("Color and Pattern...");
        colorAndPattern.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                colorAndPatternActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        moreStuffBottom.add(colorAndPattern, gridBagConstraints);

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
    }
    // </editor-fold>//GEN-END:initComponents

	private void colorAndPatternActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_colorAndPatternActionPerformed
	{//GEN-HEADEREND:event_colorAndPatternActionPerformed
		ArtworkLook.showArtworkLookDialog();
	}//GEN-LAST:event_colorAndPatternActionPerformed

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
		if (!ports.isSelected()) return;
		int currentIndex = list.getSelectedIndex();
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
		showProperList();
	}//GEN-LAST:event_attributesActionPerformed

	private void portsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_portsActionPerformed
	{//GEN-HEADEREND:event_portsActionPerformed
		showProperList();
	}//GEN-LAST:event_portsActionPerformed

	private void applyActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_applyActionPerformed
	{//GEN-HEADEREND:event_applyActionPerformed
		if (shownNode == null) return;

		double currentXPos = TextUtils.atof(xPos.getText(), new Double(initialXPos));
		double currentYPos = TextUtils.atof(yPos.getText(), new Double(initialYPos));
		int currentRotation = (int)(TextUtils.atof(rotation.getText(), new Double(initialRotation)) * 10);

		new ChangeNode(shownNode,
			initialXPos, currentXPos, initialYPos, currentYPos,
			initialXSize, xSize.getText(), initialYSize, ySize.getText(),
			initialMirrorX, mirrorX.isSelected(), initialMirrorY, mirrorY.isSelected(),
			initialRotation, currentRotation,
			initialPopupIndex, popup.getSelectedIndex(),
			initialEasyToSelect, easyToSelect.isSelected(),
			initialInvisibleOutsideCell, invisibleOutsideCell.isSelected(),
			initialLocked, locked.isSelected(),
			initialExpansion, expanded.isSelected(),
			initialName, name.getText().trim(),
			initialTextField, textField.getText(),
			initialPopupEntry, (String)popup.getSelectedItem(),
			bigger,
			scalableTrans,
			swapXY);
        attributesTable.applyChanges();

		initialName = name.getText().trim();
		initialExpansion = expanded.isSelected();
		initialEasyToSelect = easyToSelect.isSelected();
		initialInvisibleOutsideCell = invisibleOutsideCell.isSelected();
		initialLocked = locked.isSelected();
		initialTextField = textField.getText();
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
        super.closeDialog();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton apply;
    private javax.swing.JRadioButton attributes;
    private javax.swing.JButton attributesButton;
    private javax.swing.JButton cancel;
    private javax.swing.JButton colorAndPattern;
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
