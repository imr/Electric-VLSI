/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EditOptions.java
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
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.CircuitChanges;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.HashMap;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListModel;
import javax.swing.JScrollPane;
import javax.swing.text.Document;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * Class to handle the "Edit Options" dialog.
 */
public class EditOptions extends javax.swing.JDialog
{
	/** The name of the current tab in this dialog. */	private static String currentTabName = null;

	/** Creates new form Edit Options */
	public EditOptions(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		setLocation(100, 50);
		initComponents();

		// if the last know tab name is available, find that tab again
		if (currentTabName != null)
		{
			int numTabs = tabPane.getTabCount();
			for(int i=0; i<numTabs; i++)
			{
				String tabName = tabPane.getTitleAt(i);
				if (tabName.equals(currentTabName))
				{
					tabPane.setSelectedIndex(i);
					break;
				}
			}
		}

		// listen for changes in the current tab
        tabPane.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent evt)
            {
				currentTabName = tabPane.getTitleAt(tabPane.getSelectedIndex());
            }
        });

		// initialize all of the tab panes
		initNewNodes();			// initialize the New Nodes Options panel
		initNewArcs();			// initialize the New Arcs Options panel
		initSelection();		// initialize the Selection Options panel
		initPorts();			// initialize the Ports Options panel
		initFrame();			// initialize the Frame Options panel
		initIcon();				// initialize the Icon Options panel
		initGrid();				// initialize the Grid Options panel
		initLayers();			// initialize the Layers Options panel
		initColors();			// initialize the Colors Options panel
		initText();				// initialize the Text Options panel
		init3D();				// initialize the 3D Options panel
		initTechnology();		// initialize the Technology Options panel
	}

	//******************************** NEW NODES ********************************

	static class PrimNodeInfo
	{
		double initialWid, wid;
		double initialHei, hei;
		boolean initialOverride, override;
		int initialRotation, rotation;
		boolean initialMirrorX, mirrorX;
		Variable var;
	}
	private HashMap initialNewNodesPrimInfo;
	private boolean initialNewNodesCheckDatesDuringCreation;
	private boolean initialNewNodesAutoTechnologySwitch;
	private boolean initialNewNodesPlaceCellCenter;
	private boolean initialNewNodesDisallowModificationLockedPrims;
	private boolean initialNewNodesMoveAfterDuplicate;
	private boolean initialNewNodesDupCopiesExports;
	private int initialNewNodesRotation;
	private boolean initialNewNodesMirrorX;
	private boolean newNodesDataChanging = false;

	/**
	 * Routine called at the start of the dialog.
	 * Caches current values and displays them in the New Nodes tab.
	 */
	private void initNewNodes()
	{
		// gather information about the PrimitiveNodes in the current Technology
		initialNewNodesPrimInfo = new HashMap();
		for(Iterator it = Technology.getCurrent().getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = (PrimitiveNode)it.next();
			PrimNodeInfo pni = new PrimNodeInfo();
			pni.initialWid = pni.wid = np.getDefWidth();
			pni.initialHei = pni.hei = np.getDefHeight();
			pni.var = np.getVar(User.PLACEMENT_ANGLE, Integer.class);
			if (pni.var != null)
			{
				Integer rot = (Integer)pni.var.getObject();
				pni.override = true;
				pni.rotation = rot.intValue();
				pni.mirrorX = false;
				if (pni.rotation >= 3600)
				{
					pni.rotation -= 3600;
					pni.mirrorX = true;
				}
			} else
			{
				pni.override = false;
				pni.rotation = 0;
				pni.mirrorX = false;
			}
			pni.initialOverride = pni.override;
			pni.initialRotation = pni.rotation;
			pni.initialMirrorX = pni.mirrorX;
			initialNewNodesPrimInfo.put(np, pni);
			nodePrimitive.addItem(np.getProtoName());
		}
		newNodesPrimPopupChanged();

		// some things are not available yet
		nodeTinyCellsHashedOut.setEnabled(false);
		nodeHashLimit.setEditable(false);
		nodeMoveAfterDuplicate.setEnabled(false);

		// set checkboxes for "Cells" area
		nodeCheckCellDates.setSelected(initialNewNodesCheckDatesDuringCreation = User.isCheckCellDates());
		nodeSwitchTechnology.setSelected(initialNewNodesAutoTechnologySwitch = User.isAutoTechnologySwitch());
		nodePlaceCellCenter.setSelected(initialNewNodesPlaceCellCenter = User.isPlaceCellCenter());

		// set checkboxes for "all nodes" area
		nodeDisallowModificationLockedPrims.setSelected(initialNewNodesDisallowModificationLockedPrims = User.isDisallowModificationLockedPrims());
		nodeMoveAfterDuplicate.setSelected(initialNewNodesMoveAfterDuplicate = User.isMoveAfterDuplicate());
		nodeCopyExports.setSelected(initialNewNodesDupCopiesExports = User.isDupCopiesExports());
		nodeAllRotation.setText(Double.toString((initialNewNodesRotation = User.getNewNodeRotation()) / 10));
		nodeAllMirror.setSelected(initialNewNodesMirrorX = User.isDupCopiesExports());

		// setup listeners to react to any changes in the top part of the dialog
        nodePrimitive.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt) { nodePrimitiveActionPerformed(evt); }
        });
		nodeOverrideDefaultOrientation.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { newNodesOverrideChanged(); }
		});
		nodePrimitiveMirror.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { newNodesPrimDataChanged(); }
		});
		nodePrimitiveXSize.getDocument().addDocumentListener(new NewNodeDocumentListener(this));
		nodePrimitiveYSize.getDocument().addDocumentListener(new NewNodeDocumentListener(this));
		nodePrimitiveRotation.getDocument().addDocumentListener(new NewNodeDocumentListener(this));
	}

	/**
	 * Routine called when the primitive node popup is changed.
	 */
	private void nodePrimitiveActionPerformed(java.awt.event.ActionEvent evt)
	{
		newNodesPrimPopupChanged();
	}

	/**
	 * Routine called when the primitive node popup is changed.
	 */
	private void newNodesPrimPopupChanged()
	{
		String primName = (String)nodePrimitive.getSelectedItem();
		PrimitiveNode np = Technology.getCurrent().findNodeProto(primName);
		PrimNodeInfo pni = (PrimNodeInfo)initialNewNodesPrimInfo.get(np);
		if (pni == null) return;
		newNodesDataChanging = true;
		nodePrimitiveXSize.setText(Double.toString(pni.wid));
		nodePrimitiveYSize.setText(Double.toString(pni.hei));
		nodeOverrideDefaultOrientation.setSelected(pni.override);
		nodePrimitiveRotation.setText(Double.toString(pni.rotation / 10));
		nodePrimitiveMirror.setSelected(pni.mirrorX);
		newNodesDataChanging = false;
	}

	/**
	 * Class to handle special changes to per-primitive node options.
	 */
	private static class NewNodeDocumentListener implements DocumentListener
	{
		EditOptions dialog;

		NewNodeDocumentListener(EditOptions dialog) { this.dialog = dialog; }

		public void changedUpdate(DocumentEvent e) { dialog.newNodesPrimDataChanged(); }
		public void insertUpdate(DocumentEvent e) { dialog.newNodesPrimDataChanged(); }
		public void removeUpdate(DocumentEvent e) { dialog.newNodesPrimDataChanged(); }
	}

	/**
	 * Routine called when any of the primitive data (in the top part) changes.
	 * Caches all values for the selected primitive node.
	 */
	private void newNodesPrimDataChanged()
	{
		if (newNodesDataChanging) return;
		String primName = (String)nodePrimitive.getSelectedItem();
		PrimitiveNode np = Technology.getCurrent().findNodeProto(primName);
		PrimNodeInfo pni = (PrimNodeInfo)initialNewNodesPrimInfo.get(np);
		if (pni == null) return;
		pni.wid = EMath.atof(nodePrimitiveXSize.getText());
		pni.hei = EMath.atof(nodePrimitiveYSize.getText());
		pni.rotation = (int)(EMath.atof(nodePrimitiveRotation.getText()) * 10);
		pni.mirrorX = nodePrimitiveMirror.isSelected();
		if (pni.rotation != pni.initialRotation || pni.mirrorX != pni.initialMirrorX)
		{
			if (!pni.override)
				nodeOverrideDefaultOrientation.setSelected(pni.override = true);
		}
	}

	/**
	 * Routine called when the "Override default orientation" checkbox is changed.
	 * This affects whether primitive nodes have an individual orientation override.
	 */
	private void newNodesOverrideChanged()
	{
		if (newNodesDataChanging) return;
		String primName = (String)nodePrimitive.getSelectedItem();
		PrimitiveNode np = Technology.getCurrent().findNodeProto(primName);
		PrimNodeInfo pni = (PrimNodeInfo)initialNewNodesPrimInfo.get(np);
		if (pni == null) return;
		pni.override = nodeOverrideDefaultOrientation.isSelected();
		if (!pni.override)
		{
			pni.rotation = pni.initialRotation;
			pni.mirrorX = pni.initialMirrorX;
			newNodesDataChanging = true;
			nodePrimitiveRotation.setText(Double.toString(pni.rotation / 10));
			nodePrimitiveMirror.setSelected(pni.mirrorX);
			newNodesDataChanging = false;
		}
	}

	/**
	 * Routine called when the "OK" panel is hit.
	 * Updates any changed fields in the New Nodes tab.
	 */
	private void termNewNodes()
	{
		NewNodeUpdate job = new NewNodeUpdate(this);

		boolean currentCheckCellDates = nodeCheckCellDates.isSelected();
		if (currentCheckCellDates != initialNewNodesCheckDatesDuringCreation)
			User.setCheckCellDates(currentCheckCellDates);

		boolean currentSwitchTechnology = nodeSwitchTechnology.isSelected();
		if (currentSwitchTechnology != initialNewNodesAutoTechnologySwitch)
			User.setAutoTechnologySwitch(currentSwitchTechnology);

		boolean currentPlaceCellCenters = nodePlaceCellCenter.isSelected();
		if (currentPlaceCellCenters != initialNewNodesPlaceCellCenter)
			User.setPlaceCellCenter(currentPlaceCellCenters);

		boolean currentDisallowModificationLockedPrims = nodePlaceCellCenter.isSelected();
		if (currentDisallowModificationLockedPrims != initialNewNodesDisallowModificationLockedPrims)
			User.setDisallowModificationLockedPrims(currentDisallowModificationLockedPrims);

		boolean currentMoveAfterDuplicate = nodePlaceCellCenter.isSelected();
		if (currentMoveAfterDuplicate != initialNewNodesMoveAfterDuplicate)
			User.setMoveAfterDuplicate(currentMoveAfterDuplicate);

		boolean currentCopyExports = nodePlaceCellCenter.isSelected();
		if (currentCopyExports != initialNewNodesDupCopiesExports)
			User.setDupCopiesExports(currentCopyExports);

		int currentAllRotation = (int)(EMath.atof(nodeAllRotation.getText()) * 10);
		if (currentAllRotation != initialNewNodesRotation)
			User.setNewNodeRotation(currentAllRotation);

		boolean currentMirrorX = nodeAllMirror.isSelected();
		if (currentMirrorX != initialNewNodesMirrorX)
			User.setDupCopiesExports(currentMirrorX);
	}

	/**
	 * Class to update primitive node information.
	 */
	protected static class NewNodeUpdate extends Job
	{
		EditOptions dialog;

		protected NewNodeUpdate(EditOptions dialog)
		{
			super("Update Primitive Node Info", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.dialog = dialog;
			this.startJob();
		}

		public void doIt()
		{
			for(Iterator it = Technology.getCurrent().getNodes(); it.hasNext(); )
			{
				PrimitiveNode np = (PrimitiveNode)it.next();
				PrimNodeInfo pni = (PrimNodeInfo)dialog.initialNewNodesPrimInfo.get(np);
				if (pni.wid != pni.initialWid || pni.hei != pni.initialHei)
					np.setDefSize(pni.wid, pni.hei);
				if (pni.override != pni.initialOverride)
				{
					if (pni.override)
					{
						int rot = pni.rotation;
						if (pni.mirrorX) rot += 3600;
						np.newVar(User.PLACEMENT_ANGLE, new Integer(rot));
					} else
					{
						if (pni.var != null)
							np.delVar(User.PLACEMENT_ANGLE);
					}
				}
			}
		}
	}

	//******************************** NEW ARCS ********************************

	/**
	 * Routine called at the start of the dialog.
	 * Caches current values and displays them in the New Arcs tab.
	 */
	private void initNewArcs()
	{
		arcDefaultForArc.setEnabled(false);
		arcProtoList.setEnabled(false);
		arcDefaultForAllArcs.setEnabled(false);
		arcReset.setEnabled(false);
		arcRigid.setEnabled(false);
		arcFixedAngle.setEnabled(false);
		arcSlidable.setEnabled(false);
		arcNegated.setEnabled(false);
		arcDirectional.setEnabled(false);
		arcEndsExtend.setEnabled(false);
		arcWidth.setEditable(false);
		arcAngle.setEditable(false);
		arcPinName.setEnabled(false);
		arcSetPin.setEnabled(false);
	}

	/**
	 * Routine called when the "OK" panel is hit.
	 * Updates any changed fields in the New Arcs tab.
	 */
	private void termNewArcs()
	{
	}

	//******************************** SELECTION ********************************

	private boolean initialSelectionEasyCellInstances;
	private boolean initialSelectionEasyAnnotationText;
	private boolean initialSelectionDraggingMustEnclose;

	/**
	 * Routine called at the start of the dialog.
	 * Caches current values and displays them in the Selection tab.
	 */
	private void initSelection()
	{
		selEasyCellInstances.setSelected(initialSelectionEasyCellInstances = User.isEasySelectionOfCellInstances());
		selEasyAnnotationText.setSelected(initialSelectionEasyAnnotationText = User.isEasySelectionOfAnnotationText());
		selDraggingEnclosesEntireObject.setSelected(initialSelectionDraggingMustEnclose = User.isDraggingMustEncloseObjects());
	}

	/**
	 * Routine called when the "OK" panel is hit.
	 * Updates any changed fields in the Selection tab.
	 */
	private void termSelection()
	{
		boolean currentEasyCellInstances = selEasyCellInstances.isSelected();
		if (currentEasyCellInstances != initialSelectionEasyCellInstances)
			User.setEasySelectionOfCellInstances(currentEasyCellInstances);

		boolean currentEasyAnnotationText = selEasyAnnotationText.isSelected();
		if (currentEasyAnnotationText != initialSelectionEasyAnnotationText)
			User.setEasySelectionOfAnnotationText(currentEasyAnnotationText);

		boolean currentDraggingMustEnclose = selDraggingEnclosesEntireObject.isSelected();
		if (currentDraggingMustEnclose != initialSelectionDraggingMustEnclose)
			User.setDraggingMustEncloseObjects(currentDraggingMustEnclose);
	}

	//******************************** PORTS ********************************

	private int initialPortDisplayPortLevel;
	private int initialPortDisplayExportLevel;
	private boolean initialPortMoveNodeWithExport;

	/**
	 * Routine called at the start of the dialog.
	 * Caches current values and displays them in the Ports tab.
	 */
	private void initPorts()
	{
		switch (initialPortDisplayPortLevel = User.getPortDisplayLevel())
		{
			case 0: portFullPort.setSelected(true);    break;
			case 1: portShortPort.setSelected(true);   break;
			case 2: portCrossPort.setSelected(true);   break;
		}

		switch (initialPortDisplayExportLevel = User.getExportDisplayLevel())
		{
			case 0: portFullExport.setSelected(true);    break;
			case 1: portShortExport.setSelected(true);   break;
			case 2: portCrossExport.setSelected(true);   break;
		}

		// doesn't work yet
		portMoveNode.setEnabled(false);
		portMoveNode.setSelected(initialPortMoveNodeWithExport = User.isMoveNodeWithExport());
	}

	/**
	 * Routine called when the "OK" panel is hit.
	 * Updates any changed fields in the Ports tab.
	 */
	private void termPorts()
	{
		int currentDisplayPortLevel = 0;
		if (portShortPort.isSelected()) currentDisplayPortLevel = 1; else
			if (portCrossPort.isSelected()) currentDisplayPortLevel = 2;
		if (currentDisplayPortLevel != initialPortDisplayPortLevel)
			User.setPortDisplayLevels(currentDisplayPortLevel);

		int currentDisplayExportLevel = 0;
		if (portShortExport.isSelected()) currentDisplayExportLevel = 1; else
			if (portCrossExport.isSelected()) currentDisplayExportLevel = 2;
		if (currentDisplayExportLevel != initialPortDisplayExportLevel)
			User.setExportDisplayLevels(currentDisplayExportLevel);

		boolean currentMoveNodeWithExport = portMoveNode.isSelected();
		if (currentMoveNodeWithExport != initialPortMoveNodeWithExport)
			User.setMoveNodeWithExport(currentMoveNodeWithExport);

		// redisplay everything if port options changed
		if (currentDisplayPortLevel != initialPortDisplayPortLevel ||
			currentDisplayExportLevel != initialPortDisplayExportLevel)
				EditWindow.redrawAll();
	}

	//******************************** FRAME ********************************

	/**
	 * Routine called at the start of the dialog.
	 * Caches current values and displays them in the Frame tab.
	 */
	private void initFrame()
	{
		frameCellName.setEnabled(false);
		frameSize.setEnabled(false);
		frameLandscape.setEnabled(false);
		framePortrait.setEnabled(false);
		frameTitleBox.setEnabled(false);
		frameLibrary.setEnabled(false);
		frameDefaultCompany.setEditable(false);
		frameLibraryCompany.setEditable(false);
		frameDefaultDesigner.setEditable(false);
		frameLibraryDesigner.setEditable(false);
		frameDefaultProject.setEditable(false);
		frameLibraryProject.setEditable(false);
	}

	/**
	 * Routine called when the "OK" panel is hit.
	 * Updates any changed fields in the Frame tab.
	 */
	private void termFrame()
	{
	}

	//******************************** ICON ********************************

	private int initialIconInputPos, initialIconOutputPos, initialIconBidirPos;
	private int initialIconPowerPos, initialIconGroundPos, initialIconClockPos;
	private int initialIconExportLocation, initialIconExportStyle, initialIconExportTech;
	private int initialIconInstanceLocation;
	private boolean initialIconDrawLeads, initialIconDrawBody, initialIconReverseExportOrder;
	private float initialIconLeadLength, initialIconLeadSpacing;

	/**
	 * Routine called at the start of the dialog.
	 * Caches current values and displays them in the Icon tab.
	 */
	private void initIcon()
	{
		// listen for the "Make Icon" button
		iconMakeIcon.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { CircuitChanges.makeIconViewCommand(); }
		});

		// show the current cell
		Cell curCell = Library.getCurrent().getCurCell();
		if (curCell == null)
		{
			iconCurrentCell.setText("");
			iconMakeIcon.setEnabled(false);
		} else
		{
			iconCurrentCell.setText(curCell.describe());
			iconMakeIcon.setEnabled(true);
		}

		iconInputPos.addItem("Left Side");
		iconInputPos.addItem("Right Side");
		iconInputPos.addItem("Top Side");
		iconInputPos.addItem("Bottom Side");
		iconInputPos.setSelectedIndex(initialIconInputPos = User.getIconGenInputSide());

		iconOutputPos.addItem("Left Side");
		iconOutputPos.addItem("Right Side");
		iconOutputPos.addItem("Top Side");
		iconOutputPos.addItem("Bottom Side");
		iconOutputPos.setSelectedIndex(initialIconOutputPos = User.getIconGenOutputSide());

		iconBidirPos.addItem("Left Side");
		iconBidirPos.addItem("Right Side");
		iconBidirPos.addItem("Top Side");
		iconBidirPos.addItem("Bottom Side");
		iconBidirPos.setSelectedIndex(initialIconBidirPos = User.getIconGenBidirSide());

		iconPowerPos.addItem("Left Side");
		iconPowerPos.addItem("Right Side");
		iconPowerPos.addItem("Top Side");
		iconPowerPos.addItem("Bottom Side");
		iconPowerPos.setSelectedIndex(initialIconPowerPos = User.getIconGenPowerSide());

		iconGroundPos.addItem("Left Side");
		iconGroundPos.addItem("Right Side");
		iconGroundPos.addItem("Top Side");
		iconGroundPos.addItem("Bottom Side");
		iconGroundPos.setSelectedIndex(initialIconGroundPos = User.getIconGenGroundSide());

		iconClockPos.addItem("Left Side");
		iconClockPos.addItem("Right Side");
		iconClockPos.addItem("Top Side");
		iconClockPos.addItem("Bottom Side");
		iconClockPos.setSelectedIndex(initialIconClockPos = User.getIconGenClockSide());

		iconExportPos.addItem("Body");
		iconExportPos.addItem("Lead End");
		iconExportPos.addItem("Lead Middle");
		iconExportPos.setSelectedIndex(initialIconExportLocation = User.getIconGenExportLocation());

		iconExportStyle.addItem("Centered");
		iconExportStyle.addItem("Inward");
		iconExportStyle.addItem("Outward");
		iconExportStyle.setSelectedIndex(initialIconExportStyle = User.getIconGenExportStyle());

		iconExportTechnology.addItem("Universal");
		iconExportTechnology.addItem("Schematic");
		iconExportTechnology.setSelectedIndex(initialIconExportTech = User.getIconGenExportTech());

		iconInstancePos.addItem("Upper-right");
		iconInstancePos.addItem("Upper-left");
		iconInstancePos.addItem("Lower-right");
		iconInstancePos.addItem("Lower-left");
		iconInstancePos.setSelectedIndex(initialIconInstanceLocation = User.getIconGenInstanceLocation());

		iconDrawLeads.setSelected(initialIconDrawLeads = User.isIconGenDrawLeads());
		iconDrawBody.setSelected(initialIconDrawBody = User.isIconGenDrawBody());
		iconReverseOrder.setSelected(initialIconReverseExportOrder = User.isIconGenReverseExportOrder());

		iconLeadLength.setText(Float.toString(initialIconLeadLength = User.getIconGenLeadLength()));
		iconLeadSpacing.setText(Float.toString(initialIconLeadSpacing = User.getIconGenLeadSpacing()));
	}

	/**
	 * Routine called when the "OK" panel is hit.
	 * Updates any changed fields in the Icon tab.
	 */
	private void termIcon()
	{
		int currentInputPos = iconInputPos.getSelectedIndex();
		if (currentInputPos != initialIconInputPos)
			User.setIconGenInputSide(currentInputPos);

		int currentOutputPos = iconOutputPos.getSelectedIndex();
		if (currentOutputPos != initialIconOutputPos)
			User.setIconGenOutputSide(currentOutputPos);

		int currentBidirPos = iconBidirPos.getSelectedIndex();
		if (currentBidirPos != initialIconBidirPos)
			User.setIconGenBidirSide(currentBidirPos);

		int currentPowerPos = iconPowerPos.getSelectedIndex();
		if (currentPowerPos != initialIconPowerPos)
			User.setIconGenPowerSide(currentPowerPos);

		int currentGroundPos = iconGroundPos.getSelectedIndex();
		if (currentGroundPos != initialIconGroundPos)
			User.setIconGenGroundSide(currentGroundPos);

		int currentClockPos = iconClockPos.getSelectedIndex();
		if (currentClockPos != initialIconClockPos)
			User.setIconGenClockSide(currentClockPos);

		int currentExportPos = iconExportPos.getSelectedIndex();
		if (currentExportPos != initialIconExportLocation)
			User.setIconGenExportLocation(currentExportPos);

		int currentExportStyle = iconExportStyle.getSelectedIndex();
		if (currentExportStyle != initialIconExportStyle)
			User.setIconGenExportStyle(currentExportStyle);

		int currentExportTechnology = iconExportTechnology.getSelectedIndex();
		if (currentExportTechnology != initialIconExportTech)
			User.setIconGenExportTech(currentExportTechnology);

		int currentInstancePos = iconInstancePos.getSelectedIndex();
		if (currentInstancePos != initialIconInstanceLocation)
			User.setIconGenInstanceLocation(currentInstancePos);

		boolean currentDrawLeads = iconDrawLeads.isSelected();
		if (currentDrawLeads != initialIconDrawLeads)
			User.setIconGenDrawLeads(currentDrawLeads);

		boolean currentDrawBody = iconDrawBody.isSelected();
		if (currentDrawBody != initialIconDrawBody)
			User.setIconGenDrawBody(currentDrawBody);

		boolean currentReverseOrder = iconReverseOrder.isSelected();
		if (currentReverseOrder != initialIconReverseExportOrder)
			User.setIconGenReverseExportOrder(currentReverseOrder);

		float currentLeadLength = (float)EMath.atof(iconLeadLength.getText());
		if (currentLeadLength != initialIconLeadLength)
			User.setIconGenLeadLength(currentLeadLength);

		float currentLeadSpacing = (float)EMath.atof(iconLeadSpacing.getText());
		if (currentLeadSpacing != initialIconLeadSpacing)
			User.setIconGenLeadSpacing(currentLeadSpacing);
	}

	//******************************** GRID ********************************

	private float initialGridXSpacing, initialGridYSpacing;
	private float initialGridDefXSpacing, initialGridDefYSpacing;
	private int initialGridDefXBoldFrequency, initialGridDefYBoldFrequency;
	private boolean initialGridAlignWithCircuitry;
	private float initialGridAlignment, initialGridEdgeAlignment;

	/**
	 * Routine called at the start of the dialog.
	 * Caches current values and displays them in the Grid tab.
	 */
	private void initGrid()
	{
		EditWindow wnd = EditWindow.getCurrent();
		if (wnd == null)
		{
			initialGridXSpacing = initialGridYSpacing = 0;
			gridCurrentHoriz.setEditable(false);
			gridCurrentHoriz.setText("");
			gridCurrentVert.setEditable(false);
			gridCurrentVert.setText("");
		} else
		{
			gridCurrentHoriz.setEditable(true);
			gridCurrentHoriz.setText(Double.toString(initialGridXSpacing = wnd.getGridXSpacing()));
			gridCurrentVert.setEditable(true);
			gridCurrentVert.setText(Double.toString(initialGridYSpacing = wnd.getGridYSpacing()));
		}

		gridNewHoriz.setText(Double.toString(initialGridDefXSpacing = User.getDefGridXSpacing()));
		gridNewVert.setText(Double.toString(initialGridDefYSpacing = User.getDefGridYSpacing()));
		gridBoldHoriz.setText(Double.toString(initialGridDefXBoldFrequency = User.getDefGridXBoldFrequency()));
		gridBoldVert.setText(Double.toString(initialGridDefYBoldFrequency = User.getDefGridYBoldFrequency()));

		gridAlign.setSelected(initialGridAlignWithCircuitry = User.isAlignGridWithCircuitry());
		gridAlign.setEnabled(false);

		gridAlignCursor.setText(Double.toString(initialGridAlignment = User.getAlignmentToGrid()));
		gridAlignEdges.setText(Double.toString(initialGridEdgeAlignment = User.getEdgeAlignmentToGrid()));
		gridAlignEdges.setEnabled(false);
	}

	/**
	 * Routine called when the "OK" panel is hit.
	 * Updates any changed fields in the Grid tab.
	 */
	private void termGrid()
	{
		EditWindow wnd = EditWindow.getCurrent();
		if (wnd != null)
		{
			float currentXSpacing = (float)EMath.atof(gridCurrentHoriz.getText());
			if (currentXSpacing != initialGridXSpacing)
				wnd.setGridXSpacing(currentXSpacing);

			float currentYSpacing = (float)EMath.atof(gridCurrentVert.getText());
			if (currentYSpacing != initialGridYSpacing)
				wnd.setGridYSpacing(currentYSpacing);
		}

		float currentDefXSpacing = (float)EMath.atof(gridNewHoriz.getText());
		if (currentDefXSpacing != initialGridDefXSpacing)
			User.setDefGridXSpacing(currentDefXSpacing);

		float currentDefYSpacing = (float)EMath.atof(gridNewVert.getText());
		if (currentDefYSpacing != initialGridDefYSpacing)
			User.setDefGridYSpacing(currentDefYSpacing);

		int currentDefXBoldFrequency = EMath.atoi(gridBoldHoriz.getText());
		if (currentDefXBoldFrequency != initialGridDefXBoldFrequency)
			User.setDefGridXBoldFrequency(currentDefXBoldFrequency);

		int currentDefYBoldFrequency = EMath.atoi(gridBoldVert.getText());
		if (currentDefYBoldFrequency != initialGridDefYBoldFrequency)
			User.setDefGridYBoldFrequency(currentDefYBoldFrequency);

		boolean currentAlignWithCircuitry = gridAlign.isSelected();
		if (currentAlignWithCircuitry != initialGridAlignWithCircuitry)
			User.setAlignGridWithCircuitry(currentAlignWithCircuitry);

		float currentAlignment = (float)EMath.atof(gridAlignCursor.getText());
		if (currentAlignment != initialGridAlignment)
			User.setAlignmentToGrid(currentAlignment);

		float currentEdgeAlignment = (float)EMath.atof(gridAlignEdges.getText());
		if (currentEdgeAlignment != initialGridEdgeAlignment)
			User.setEdgeAlignmentToGrid(currentEdgeAlignment);
	}

	//******************************** LAYERS ********************************

	/**
	 * Routine called at the start of the dialog.
	 * Caches current values and displays them in the Layers tab.
	 */
	private void initLayers()
	{
		layerName.setEnabled(false);
	}

	/**
	 * Routine called when the "OK" panel is hit.
	 * Updates any changed fields in the Layers tab.
	 */
	private void termLayers()
	{
	}

	//******************************** COLORS ********************************

	/**
	 * Routine called at the start of the dialog.
	 * Caches current values and displays them in the Colors tab.
	 */
	private void initColors()
	{
	}

	/**
	 * Routine called when the "OK" panel is hit.
	 * Updates any changed fields in the Colors tab.
	 */
	private void termColors()
	{
	}

	//******************************** TEXT ********************************

	/**
	 * Routine called at the start of the dialog.
	 * Caches current values and displays them in the Text tab.
	 */
	private void initText()
	{
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

		textNodes.setEnabled(false);
		textArcs.setEnabled(false);
		textPorts.setEnabled(false);
		textNonlayout.setEnabled(false);
		textInstances.setEnabled(false);
		textCellText.setEnabled(false);
		textPointSize.setEditable(false);
		textUnitSize.setEditable(false);
		textFace.setEnabled(false);
		textItalic.setEnabled(false);
		textBold.setEnabled(false);
		textUnderline.setEnabled(false);
		textPoints.setEnabled(false);
		textUnits.setEnabled(false);
		textEditor.setEnabled(false);
		textNewVisibleInsideCell.setEnabled(false);
		textGrabCenter.setEnabled(false);
		textGrabBottom.setEnabled(false);
		textGrabTop.setEnabled(false);
		textGrabRight.setEnabled(false);
		textGrabLeft.setEnabled(false);
		textGrabLowerRight.setEnabled(false);
		textGrabLowerLeft.setEnabled(false);
		textGrabUpperRight.setEnabled(false);
		textGrabUpperLeft.setEnabled(false);
		textGrabBoxed.setEnabled(false);
		textSmartVerticalOff.setEnabled(false);
		textSmartVerticalInside.setEnabled(false);
		textSmartVerticalOutside.setEnabled(false);
		textSmartHorizontalOff.setEnabled(false);
		textSmartHorizontalInside.setEnabled(false);
		textSmartHorizontalOutside.setEnabled(false);
	}

	/**
	 * Routine called when the "OK" panel is hit.
	 * Updates any changed fields in the Text tab.
	 */
	private void termText()
	{
	}

	//******************************** 3D ********************************

	/**
	 * Routine called at the start of the dialog.
	 * Caches current values and displays them in the 3D tab.
	 */
	private void init3D()
	{
	}

	/**
	 * Routine called when the "OK" panel is hit.
	 * Updates any changed fields in the 3D tab.
	 */
	private void term3D()
	{
	}

	//******************************** TECHNOLOGY ********************************

	/**
	 * Routine called at the start of the dialog.
	 * Caches current values and displays them in the Technology tab.
	 */
	private void initTechnology()
	{
		techMOCMOSMetalLayers.setEnabled(false);
		techMOCMOSSCMOSRules.setEnabled(false);
		techMOCMOSSubmicronRules.setEnabled(false);
		techMOCMOSDeepRules.setEnabled(false);
		techMOCMOSSecondPoly.setEnabled(false);
		techMOCMOSDisallowStackedVias.setEnabled(false);
		techMOCMOSAlternateContactRules.setEnabled(false);
		techMOCMOSShowSpecialTrans.setEnabled(false);
		techMOCMOSFullGeom.setEnabled(false);
		techMOCMOSStickFigures.setEnabled(false);
		techArtworkArrowsFilled.setEnabled(false);
		techSchematicsNegatingSize.setEditable(false);
	}

	/**
	 * Routine called when the "OK" panel is hit.
	 * Updates any changed fields in the Technology tab.
	 */
	private void termTechnology()
	{
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        newArcGroup = new javax.swing.ButtonGroup();
        portGroup = new javax.swing.ButtonGroup();
        exportGroup = new javax.swing.ButtonGroup();
        frameGroup = new javax.swing.ButtonGroup();
        textSizeGroup = new javax.swing.ButtonGroup();
        textCornerGroup = new javax.swing.ButtonGroup();
        textTypeGroup = new javax.swing.ButtonGroup();
        textVerticalGroup = new javax.swing.ButtonGroup();
        textHorizontalGroup = new javax.swing.ButtonGroup();
        techMOCMOSRules = new javax.swing.ButtonGroup();
        techMOCMOSSticks = new javax.swing.ButtonGroup();
        tabPane = new javax.swing.JTabbedPane();
        newNode = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        nodePrimitive = new javax.swing.JComboBox();
        nodePrimitiveXSize = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        nodePrimitiveYSize = new javax.swing.JTextField();
        nodeOverrideDefaultOrientation = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        nodePrimitiveRotation = new javax.swing.JTextField();
        nodePrimitiveMirror = new javax.swing.JCheckBox();
        jPanel3 = new javax.swing.JPanel();
        nodeCheckCellDates = new javax.swing.JCheckBox();
        nodeSwitchTechnology = new javax.swing.JCheckBox();
        nodePlaceCellCenter = new javax.swing.JCheckBox();
        nodeTinyCellsHashedOut = new javax.swing.JCheckBox();
        jLabel47 = new javax.swing.JLabel();
        nodeHashLimit = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        nodeAllRotation = new javax.swing.JTextField();
        nodeAllMirror = new javax.swing.JCheckBox();
        nodeDisallowModificationLockedPrims = new javax.swing.JCheckBox();
        nodeMoveAfterDuplicate = new javax.swing.JCheckBox();
        nodeCopyExports = new javax.swing.JCheckBox();
        newArc = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        arcDefaultForArc = new javax.swing.JRadioButton();
        arcDefaultForAllArcs = new javax.swing.JRadioButton();
        arcProtoList = new javax.swing.JComboBox();
        arcReset = new javax.swing.JButton();
        jPanel7 = new javax.swing.JPanel();
        arcRigid = new javax.swing.JCheckBox();
        arcNegated = new javax.swing.JCheckBox();
        arcFixedAngle = new javax.swing.JCheckBox();
        arcDirectional = new javax.swing.JCheckBox();
        arcSlidable = new javax.swing.JCheckBox();
        arcEndsExtend = new javax.swing.JCheckBox();
        jPanel8 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        arcWidth = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        arcAngle = new javax.swing.JTextField();
        arcSetPin = new javax.swing.JButton();
        arcPinName = new javax.swing.JLabel();
        selection = new javax.swing.JPanel();
        selEasyCellInstances = new javax.swing.JCheckBox();
        selEasyAnnotationText = new javax.swing.JCheckBox();
        selDraggingEnclosesEntireObject = new javax.swing.JCheckBox();
        port = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        portFullPort = new javax.swing.JRadioButton();
        portFullExport = new javax.swing.JRadioButton();
        portShortPort = new javax.swing.JRadioButton();
        portShortExport = new javax.swing.JRadioButton();
        portCrossPort = new javax.swing.JRadioButton();
        portCrossExport = new javax.swing.JRadioButton();
        jSeparator2 = new javax.swing.JSeparator();
        portMoveNode = new javax.swing.JCheckBox();
        jSeparator9 = new javax.swing.JSeparator();
        frame = new javax.swing.JPanel();
        frameCellName = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        frameSize = new javax.swing.JComboBox();
        frameLandscape = new javax.swing.JRadioButton();
        framePortrait = new javax.swing.JRadioButton();
        frameTitleBox = new javax.swing.JCheckBox();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        frameLibrary = new javax.swing.JComboBox();
        jLabel17 = new javax.swing.JLabel();
        frameDefaultCompany = new javax.swing.JTextField();
        frameLibraryCompany = new javax.swing.JTextField();
        jLabel18 = new javax.swing.JLabel();
        frameDefaultDesigner = new javax.swing.JTextField();
        frameLibraryDesigner = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        frameDefaultProject = new javax.swing.JTextField();
        frameLibraryProject = new javax.swing.JTextField();
        icon = new javax.swing.JPanel();
        jLabel48 = new javax.swing.JLabel();
        jLabel54 = new javax.swing.JLabel();
        iconCurrentCell = new javax.swing.JLabel();
        iconMakeIcon = new javax.swing.JButton();
        jSeparator11 = new javax.swing.JSeparator();
        iconInstancePos = new javax.swing.JComboBox();
        jLabel30 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jLabel20 = new javax.swing.JLabel();
        iconInputPos = new javax.swing.JComboBox();
        jLabel21 = new javax.swing.JLabel();
        iconPowerPos = new javax.swing.JComboBox();
        iconGroundPos = new javax.swing.JComboBox();
        jLabel23 = new javax.swing.JLabel();
        iconOutputPos = new javax.swing.JComboBox();
        jLabel22 = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        iconBidirPos = new javax.swing.JComboBox();
        jLabel25 = new javax.swing.JLabel();
        iconClockPos = new javax.swing.JComboBox();
        jLabel31 = new javax.swing.JLabel();
        iconExportTechnology = new javax.swing.JComboBox();
        jPanel6 = new javax.swing.JPanel();
        iconDrawLeads = new javax.swing.JCheckBox();
        iconDrawBody = new javax.swing.JCheckBox();
        jLabel26 = new javax.swing.JLabel();
        iconLeadLength = new javax.swing.JTextField();
        iconLeadSpacing = new javax.swing.JTextField();
        jLabel27 = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        iconExportPos = new javax.swing.JComboBox();
        jLabel29 = new javax.swing.JLabel();
        iconExportStyle = new javax.swing.JComboBox();
        iconReverseOrder = new javax.swing.JCheckBox();
        grid = new javax.swing.JPanel();
        gridPart = new javax.swing.JPanel();
        jLabel32 = new javax.swing.JLabel();
        jLabel33 = new javax.swing.JLabel();
        jLabel34 = new javax.swing.JLabel();
        gridCurrentHoriz = new javax.swing.JTextField();
        gridCurrentVert = new javax.swing.JTextField();
        jLabel35 = new javax.swing.JLabel();
        gridNewHoriz = new javax.swing.JTextField();
        gridNewVert = new javax.swing.JTextField();
        jLabel36 = new javax.swing.JLabel();
        gridBoldHoriz = new javax.swing.JTextField();
        gridBoldVert = new javax.swing.JTextField();
        gridAlign = new javax.swing.JCheckBox();
        jLabel10 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        alignPart = new javax.swing.JPanel();
        jLabel37 = new javax.swing.JLabel();
        gridAlignCursor = new javax.swing.JTextField();
        jLabel38 = new javax.swing.JLabel();
        jLabel39 = new javax.swing.JLabel();
        gridAlignEdges = new javax.swing.JTextField();
        layers = new javax.swing.JPanel();
        jLabel40 = new javax.swing.JLabel();
        layerName = new javax.swing.JComboBox();
        colors = new javax.swing.JPanel();
        text = new javax.swing.JPanel();
        top = new javax.swing.JPanel();
        jLabel41 = new javax.swing.JLabel();
        textNodes = new javax.swing.JRadioButton();
        textArcs = new javax.swing.JRadioButton();
        textPorts = new javax.swing.JRadioButton();
        textNonlayout = new javax.swing.JRadioButton();
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
        middle = new javax.swing.JPanel();
        jLabel44 = new javax.swing.JLabel();
        textEditor = new javax.swing.JComboBox();
        textNewVisibleInsideCell = new javax.swing.JCheckBox();
        bottom = new javax.swing.JPanel();
        jLabel45 = new javax.swing.JLabel();
        textGrabCenter = new javax.swing.JRadioButton();
        textGrabBottom = new javax.swing.JRadioButton();
        textGrabTop = new javax.swing.JRadioButton();
        textGrabRight = new javax.swing.JRadioButton();
        textGrabLeft = new javax.swing.JRadioButton();
        textGrabLowerRight = new javax.swing.JRadioButton();
        textGrabLowerLeft = new javax.swing.JRadioButton();
        textGrabUpperRight = new javax.swing.JRadioButton();
        textGrabUpperLeft = new javax.swing.JRadioButton();
        textGrabBoxed = new javax.swing.JRadioButton();
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
        jSeparator5 = new javax.swing.JSeparator();
        jLabel56 = new javax.swing.JLabel();
        textSmartVerticalOff = new javax.swing.JRadioButton();
        textSmartVerticalInside = new javax.swing.JRadioButton();
        textSmartVerticalOutside = new javax.swing.JRadioButton();
        jLabel57 = new javax.swing.JLabel();
        textSmartHorizontalOff = new javax.swing.JRadioButton();
        textSmartHorizontalInside = new javax.swing.JRadioButton();
        textSmartHorizontalOutside = new javax.swing.JRadioButton();
        jSeparator3 = new javax.swing.JSeparator();
        jSeparator4 = new javax.swing.JSeparator();
        threeD = new javax.swing.JPanel();
        technology = new javax.swing.JPanel();
        jLabel46 = new javax.swing.JLabel();
        jLabel49 = new javax.swing.JLabel();
        techMOCMOSMetalLayers = new javax.swing.JComboBox();
        techMOCMOSSCMOSRules = new javax.swing.JRadioButton();
        techMOCMOSSubmicronRules = new javax.swing.JRadioButton();
        techMOCMOSDeepRules = new javax.swing.JRadioButton();
        techMOCMOSSecondPoly = new javax.swing.JCheckBox();
        techMOCMOSDisallowStackedVias = new javax.swing.JCheckBox();
        techMOCMOSAlternateContactRules = new javax.swing.JCheckBox();
        techMOCMOSShowSpecialTrans = new javax.swing.JCheckBox();
        techMOCMOSFullGeom = new javax.swing.JRadioButton();
        techMOCMOSStickFigures = new javax.swing.JRadioButton();
        jSeparator6 = new javax.swing.JSeparator();
        jLabel50 = new javax.swing.JLabel();
        techArtworkArrowsFilled = new javax.swing.JCheckBox();
        jSeparator7 = new javax.swing.JSeparator();
        jLabel51 = new javax.swing.JLabel();
        jLabel52 = new javax.swing.JLabel();
        techSchematicsNegatingSize = new javax.swing.JTextField();
        cancel = new javax.swing.JButton();
        ok = new javax.swing.JButton();

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

        newNode.setLayout(new java.awt.GridBagLayout());

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jPanel2.setBorder(new javax.swing.border.TitledBorder("For Primitive Nodes"));
        jLabel1.setText("Primitive:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(jLabel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.weightx = 1.0;
        jPanel2.add(nodePrimitive, gridBagConstraints);

        nodePrimitiveXSize.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel2.add(nodePrimitiveXSize, gridBagConstraints);

        jLabel2.setText("X size of new primitives:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(jLabel2, gridBagConstraints);

        jLabel3.setText("Y size of new primitives:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(jLabel3, gridBagConstraints);

        nodePrimitiveYSize.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel2.add(nodePrimitiveYSize, gridBagConstraints);

        nodeOverrideDefaultOrientation.setText("Override default orientation");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(nodeOverrideDefaultOrientation, gridBagConstraints);

        jLabel4.setText("Rotation of new nodes:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(jLabel4, gridBagConstraints);

        nodePrimitiveRotation.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel2.add(nodePrimitiveRotation, gridBagConstraints);

        nodePrimitiveMirror.setText("Mirror X");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(nodePrimitiveMirror, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        newNode.add(jPanel2, gridBagConstraints);

        jPanel3.setLayout(new java.awt.GridBagLayout());

        jPanel3.setBorder(new javax.swing.border.TitledBorder("For Cells"));
        nodeCheckCellDates.setText("Check cell dates during editing");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel3.add(nodeCheckCellDates, gridBagConstraints);

        nodeSwitchTechnology.setText("Switch technology to match current cell");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel3.add(nodeSwitchTechnology, gridBagConstraints);

        nodePlaceCellCenter.setText("Place Cell-Center in new cells");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel3.add(nodePlaceCellCenter, gridBagConstraints);

        nodeTinyCellsHashedOut.setText("Tiny cell instances hashed out");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel3.add(nodeTinyCellsHashedOut, gridBagConstraints);

        jLabel47.setText("Units per pixel when cells are hashed:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(2, 20, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel3.add(jLabel47, gridBagConstraints);

        nodeHashLimit.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel3.add(nodeHashLimit, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        newNode.add(jPanel3, gridBagConstraints);

        jPanel4.setLayout(new java.awt.GridBagLayout());

        jPanel4.setBorder(new javax.swing.border.TitledBorder("For All Nodes"));
        jLabel6.setText("Rotation of new nodes:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel4.add(jLabel6, gridBagConstraints);

        nodeAllRotation.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel4.add(nodeAllRotation, gridBagConstraints);

        nodeAllMirror.setText("Mirror X");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel4.add(nodeAllMirror, gridBagConstraints);

        nodeDisallowModificationLockedPrims.setText("Disallow modification of locked primitives");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel4.add(nodeDisallowModificationLockedPrims, gridBagConstraints);

        nodeMoveAfterDuplicate.setText("Move after Duplicate");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel4.add(nodeMoveAfterDuplicate, gridBagConstraints);

        nodeCopyExports.setText("Duplicate/Array/Extract copies exports");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel4.add(nodeCopyExports, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        newNode.add(jPanel4, gridBagConstraints);

        tabPane.addTab("New Nodes", newNode);

        newArc.setLayout(new java.awt.GridBagLayout());

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel1.setBorder(new javax.swing.border.TitledBorder("Which Arcs"));
        arcDefaultForArc.setText("For arc:");
        newArcGroup.add(arcDefaultForArc);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(arcDefaultForArc, gridBagConstraints);

        arcDefaultForAllArcs.setText("For all arcs");
        newArcGroup.add(arcDefaultForAllArcs);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(arcDefaultForAllArcs, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(arcProtoList, gridBagConstraints);

        arcReset.setText("Reset to initial defaults");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(arcReset, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        newArc.add(jPanel1, gridBagConstraints);

        jPanel7.setLayout(new java.awt.GridBagLayout());

        jPanel7.setBorder(new javax.swing.border.TitledBorder("Default Constraints"));
        arcRigid.setText("Rigid");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel7.add(arcRigid, gridBagConstraints);

        arcNegated.setText("Negated");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel7.add(arcNegated, gridBagConstraints);

        arcFixedAngle.setText("Fixed-angle");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel7.add(arcFixedAngle, gridBagConstraints);

        arcDirectional.setText("Directional");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel7.add(arcDirectional, gridBagConstraints);

        arcSlidable.setText("Slidable");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel7.add(arcSlidable, gridBagConstraints);

        arcEndsExtend.setText("Ends extended");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel7.add(arcEndsExtend, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        newArc.add(jPanel7, gridBagConstraints);

        jPanel8.setLayout(new java.awt.GridBagLayout());

        jPanel8.setBorder(new javax.swing.border.TitledBorder("Other Information"));
        jLabel7.setText("Default Width:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel8.add(jLabel7, gridBagConstraints);

        jLabel9.setText("Pin:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel8.add(jLabel9, gridBagConstraints);

        arcWidth.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel8.add(arcWidth, gridBagConstraints);

        jLabel8.setText("Placement angle:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel8.add(jLabel8, gridBagConstraints);

        arcAngle.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel8.add(arcAngle, gridBagConstraints);

        arcSetPin.setText("Set Pin");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel8.add(arcSetPin, gridBagConstraints);

        arcPinName.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel8.add(arcPinName, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        newArc.add(jPanel8, gridBagConstraints);

        tabPane.addTab("New Arcs", newArc);

        selection.setLayout(new java.awt.GridBagLayout());

        selEasyCellInstances.setText("Easy selection of cell instances");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        selection.add(selEasyCellInstances, gridBagConstraints);

        selEasyAnnotationText.setText("Easy selection of annotation text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        selection.add(selEasyAnnotationText, gridBagConstraints);

        selDraggingEnclosesEntireObject.setText("Dragging must enclose entire object");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        selection.add(selDraggingEnclosesEntireObject, gridBagConstraints);

        tabPane.addTab("Selection", selection);

        port.setLayout(new java.awt.GridBagLayout());

        jLabel11.setText("Ports (in instances):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        port.add(jLabel11, gridBagConstraints);

        jLabel12.setText("Exports (in cells):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        port.add(jLabel12, gridBagConstraints);

        portFullPort.setText("Full Names");
        portGroup.add(portFullPort);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        port.add(portFullPort, gridBagConstraints);

        portFullExport.setText("Full Names");
        exportGroup.add(portFullExport);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        port.add(portFullExport, gridBagConstraints);

        portShortPort.setText("Short Names");
        portGroup.add(portShortPort);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        port.add(portShortPort, gridBagConstraints);

        portShortExport.setText("Short Names");
        exportGroup.add(portShortExport);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        port.add(portShortExport, gridBagConstraints);

        portCrossPort.setText("Crosses");
        portGroup.add(portCrossPort);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        port.add(portCrossPort, gridBagConstraints);

        portCrossExport.setText("Crosses");
        exportGroup.add(portCrossExport);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        port.add(portCrossExport, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        port.add(jSeparator2, gridBagConstraints);

        portMoveNode.setText("Move node with export name");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        port.add(portMoveNode, gridBagConstraints);

        jSeparator9.setOrientation(javax.swing.SwingConstants.VERTICAL);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        port.add(jSeparator9, gridBagConstraints);

        tabPane.addTab("Ports/Exports", port);

        frame.setLayout(new java.awt.GridBagLayout());

        frameCellName.setText("No cell in window");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        frame.add(frameCellName, gridBagConstraints);

        jLabel14.setText("Frame size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        frame.add(jLabel14, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        frame.add(frameSize, gridBagConstraints);

        frameLandscape.setText("Landscape");
        frameGroup.add(frameLandscape);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        frame.add(frameLandscape, gridBagConstraints);

        framePortrait.setText("Portrait");
        frameGroup.add(framePortrait);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        frame.add(framePortrait, gridBagConstraints);

        frameTitleBox.setText("Title Box");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        frame.add(frameTitleBox, gridBagConstraints);

        jLabel15.setText("Default:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        frame.add(jLabel15, gridBagConstraints);

        jLabel16.setText("Library:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        frame.add(jLabel16, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        frame.add(frameLibrary, gridBagConstraints);

        jLabel17.setText("Company Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        frame.add(jLabel17, gridBagConstraints);

        frameDefaultCompany.setColumns(15);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        frame.add(frameDefaultCompany, gridBagConstraints);

        frameLibraryCompany.setColumns(15);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        frame.add(frameLibraryCompany, gridBagConstraints);

        jLabel18.setText("Designer Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        frame.add(jLabel18, gridBagConstraints);

        frameDefaultDesigner.setColumns(15);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        frame.add(frameDefaultDesigner, gridBagConstraints);

        frameLibraryDesigner.setColumns(15);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        frame.add(frameLibraryDesigner, gridBagConstraints);

        jLabel19.setText("Project Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        frame.add(jLabel19, gridBagConstraints);

        frameDefaultProject.setColumns(15);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        frame.add(frameDefaultProject, gridBagConstraints);

        frameLibraryProject.setColumns(15);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        frame.add(frameLibraryProject, gridBagConstraints);

        tabPane.addTab("Frame", frame);

        icon.setLayout(new java.awt.GridBagLayout());

        jLabel48.setText("These are the settings used to automatically generate an icon from a schematic.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        icon.add(jLabel48, gridBagConstraints);

        jLabel54.setText("Current cell:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        icon.add(jLabel54, gridBagConstraints);

        iconCurrentCell.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.weightx = 1.0;
        icon.add(iconCurrentCell, gridBagConstraints);

        iconMakeIcon.setText("Make Icon");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        icon.add(iconMakeIcon, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        icon.add(jSeparator11, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        icon.add(iconInstancePos, gridBagConstraints);

        jLabel30.setText("Export technology:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        icon.add(jLabel30, gridBagConstraints);

        jPanel5.setLayout(new java.awt.GridBagLayout());

        jPanel5.setBorder(new javax.swing.border.TitledBorder("Export location by Characteristic"));
        jLabel20.setText("Inputs on:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel5.add(jLabel20, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        jPanel5.add(iconInputPos, gridBagConstraints);

        jLabel21.setText("Power on:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel5.add(jLabel21, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        jPanel5.add(iconPowerPos, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        jPanel5.add(iconGroundPos, gridBagConstraints);

        jLabel23.setText("Ground on:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel5.add(jLabel23, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        jPanel5.add(iconOutputPos, gridBagConstraints);

        jLabel22.setText("Outputs on:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel5.add(jLabel22, gridBagConstraints);

        jLabel24.setText("Bidir. on:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel5.add(jLabel24, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        jPanel5.add(iconBidirPos, gridBagConstraints);

        jLabel25.setText("Clock on:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel5.add(jLabel25, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        jPanel5.add(iconClockPos, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        icon.add(jPanel5, gridBagConstraints);

        jLabel31.setText("Instance location:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        icon.add(jLabel31, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        icon.add(iconExportTechnology, gridBagConstraints);

        jPanel6.setLayout(new java.awt.GridBagLayout());

        jPanel6.setBorder(new javax.swing.border.TitledBorder("Body and Leads"));
        iconDrawLeads.setText("Draw leads");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel6.add(iconDrawLeads, gridBagConstraints);

        iconDrawBody.setText("Draw body");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel6.add(iconDrawBody, gridBagConstraints);

        jLabel26.setText("Lead length:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel6.add(jLabel26, gridBagConstraints);

        iconLeadLength.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        jPanel6.add(iconLeadLength, gridBagConstraints);

        iconLeadSpacing.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        jPanel6.add(iconLeadSpacing, gridBagConstraints);

        jLabel27.setText("Lead spacing:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel6.add(jLabel27, gridBagConstraints);

        jLabel28.setText("Export location:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel6.add(jLabel28, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel6.add(iconExportPos, gridBagConstraints);

        jLabel29.setText("Export style:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel6.add(jLabel29, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel6.add(iconExportStyle, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        icon.add(jPanel6, gridBagConstraints);

        iconReverseOrder.setText("Place Exports in Reverse Alphabetical Order");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        icon.add(iconReverseOrder, gridBagConstraints);

        tabPane.addTab("Icon", icon);

        grid.setLayout(new java.awt.GridBagLayout());

        gridPart.setLayout(new java.awt.GridBagLayout());

        gridPart.setBorder(new javax.swing.border.TitledBorder("Grid Display"));
        jLabel32.setText("Horizontal:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridPart.add(jLabel32, gridBagConstraints);

        jLabel33.setText("Vertical:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridPart.add(jLabel33, gridBagConstraints);

        jLabel34.setText("Grid dot spacing:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(8, 4, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridPart.add(jLabel34, gridBagConstraints);

        gridCurrentHoriz.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridPart.add(gridCurrentHoriz, gridBagConstraints);

        gridCurrentVert.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridPart.add(gridCurrentVert, gridBagConstraints);

        jLabel35.setText("Default grid spacing:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(8, 4, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridPart.add(jLabel35, gridBagConstraints);

        gridNewHoriz.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridPart.add(gridNewHoriz, gridBagConstraints);

        gridNewVert.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridPart.add(gridNewVert, gridBagConstraints);

        jLabel36.setText("Frequency of bold dots:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(8, 4, 8, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridPart.add(jLabel36, gridBagConstraints);

        gridBoldHoriz.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridPart.add(gridBoldHoriz, gridBagConstraints);

        gridBoldVert.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridPart.add(gridBoldVert, gridBagConstraints);

        gridAlign.setText("Align grid with circuitry");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridPart.add(gridAlign, gridBagConstraints);

        jLabel10.setText("(for current window)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 14, 8, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridPart.add(jLabel10, gridBagConstraints);

        jLabel13.setText("(for new windows)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(0, 14, 8, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridPart.add(jLabel13, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        grid.add(gridPart, gridBagConstraints);

        alignPart.setLayout(new java.awt.GridBagLayout());

        alignPart.setBorder(new javax.swing.border.TitledBorder("Alignment to Grid"));
        jLabel37.setText("Alignment of cursor to grid:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        alignPart.add(jLabel37, gridBagConstraints);

        gridAlignCursor.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        alignPart.add(gridAlignCursor, gridBagConstraints);

        jLabel38.setText("Values of zero will cause no alignment");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        alignPart.add(jLabel38, gridBagConstraints);

        jLabel39.setText("Alignment of edges to grid:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        alignPart.add(jLabel39, gridBagConstraints);

        gridAlignEdges.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        alignPart.add(gridAlignEdges, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        grid.add(alignPart, gridBagConstraints);

        tabPane.addTab("Grid/Alignment", grid);

        layers.setLayout(new java.awt.GridBagLayout());

        jLabel40.setText("Appearance of layer:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        layers.add(jLabel40, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        layers.add(layerName, gridBagConstraints);

        tabPane.addTab("Layers", layers);

        colors.setLayout(new java.awt.GridBagLayout());

        tabPane.addTab("Colors", colors);

        text.setLayout(new java.awt.GridBagLayout());

        top.setLayout(new java.awt.GridBagLayout());

        jLabel41.setText("Default text information for different types of text:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 0);
        top.add(jLabel41, gridBagConstraints);

        textNodes.setText("Nodes");
        textTypeGroup.add(textNodes);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        top.add(textNodes, gridBagConstraints);

        textArcs.setText("Arcs");
        textTypeGroup.add(textArcs);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        top.add(textArcs, gridBagConstraints);

        textPorts.setText("Exports/Ports");
        textTypeGroup.add(textPorts);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        top.add(textPorts, gridBagConstraints);

        textNonlayout.setText("Nonlayout text");
        textTypeGroup.add(textNonlayout);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        top.add(textNonlayout, gridBagConstraints);

        textInstances.setText("Instance names");
        textTypeGroup.add(textInstances);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        top.add(textInstances, gridBagConstraints);

        textCellText.setText("Cell text");
        textTypeGroup.add(textCellText);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        top.add(textCellText, gridBagConstraints);

        jLabel42.setText("Size");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        top.add(jLabel42, gridBagConstraints);

        textPointSize.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        top.add(textPointSize, gridBagConstraints);

        textUnitSize.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        top.add(textUnitSize, gridBagConstraints);

        jLabel43.setText("Type face:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        top.add(jLabel43, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        top.add(textFace, gridBagConstraints);

        textItalic.setText("Italic");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        top.add(textItalic, gridBagConstraints);

        textBold.setText("Bold");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        top.add(textBold, gridBagConstraints);

        textUnderline.setText("Underline");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        top.add(textUnderline, gridBagConstraints);

        textPoints.setText("Points (max 63)");
        textSizeGroup.add(textPoints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        top.add(textPoints, gridBagConstraints);

        textUnits.setText("Units (max 127.75)");
        textSizeGroup.add(textUnits);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        top.add(textUnits, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        text.add(top, gridBagConstraints);

        middle.setLayout(new java.awt.GridBagLayout());

        jLabel44.setText("Text editor:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        middle.add(jLabel44, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        middle.add(textEditor, gridBagConstraints);

        textNewVisibleInsideCell.setText("New text visible only inside cell");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        middle.add(textNewVisibleInsideCell, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        text.add(middle, gridBagConstraints);

        bottom.setLayout(new java.awt.GridBagLayout());

        jLabel45.setText("Text corner:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        bottom.add(jLabel45, gridBagConstraints);

        textGrabCenter.setText("Center");
        textCornerGroup.add(textGrabCenter);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        bottom.add(textGrabCenter, gridBagConstraints);

        textGrabBottom.setText("Bottom");
        textCornerGroup.add(textGrabBottom);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        bottom.add(textGrabBottom, gridBagConstraints);

        textGrabTop.setText("Top");
        textCornerGroup.add(textGrabTop);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        bottom.add(textGrabTop, gridBagConstraints);

        textGrabRight.setText("Right");
        textCornerGroup.add(textGrabRight);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        bottom.add(textGrabRight, gridBagConstraints);

        textGrabLeft.setText("Left");
        textCornerGroup.add(textGrabLeft);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        bottom.add(textGrabLeft, gridBagConstraints);

        textGrabLowerRight.setText("Lower right");
        textCornerGroup.add(textGrabLowerRight);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        bottom.add(textGrabLowerRight, gridBagConstraints);

        textGrabLowerLeft.setText("Lower left");
        textCornerGroup.add(textGrabLowerLeft);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        bottom.add(textGrabLowerLeft, gridBagConstraints);

        textGrabUpperRight.setText("Upper right");
        textCornerGroup.add(textGrabUpperRight);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        bottom.add(textGrabUpperRight, gridBagConstraints);

        textGrabUpperLeft.setText("Upper left");
        textCornerGroup.add(textGrabUpperLeft);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        bottom.add(textGrabUpperLeft, gridBagConstraints);

        textGrabBoxed.setText("Boxed");
        textCornerGroup.add(textGrabBoxed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        bottom.add(textGrabBoxed, gridBagConstraints);

        textIconCenter.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconCenter.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        bottom.add(textIconCenter, gridBagConstraints);

        textIconBottom.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconBottom.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        bottom.add(textIconBottom, gridBagConstraints);

        textIconTop.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconTop.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        bottom.add(textIconTop, gridBagConstraints);

        textIconRight.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconRight.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        bottom.add(textIconRight, gridBagConstraints);

        textIconLeft.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconLeft.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        bottom.add(textIconLeft, gridBagConstraints);

        textIconLowerRight.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconLowerRight.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        bottom.add(textIconLowerRight, gridBagConstraints);

        textIconLowerLeft.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconLowerLeft.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        bottom.add(textIconLowerLeft, gridBagConstraints);

        textIconUpperRight.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconUpperRight.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        bottom.add(textIconUpperRight, gridBagConstraints);

        textIconUpperLeft.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconUpperLeft.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        bottom.add(textIconUpperLeft, gridBagConstraints);

        textIconBoxed.setMinimumSize(new java.awt.Dimension(25, 15));
        textIconBoxed.setPreferredSize(new java.awt.Dimension(25, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        bottom.add(textIconBoxed, gridBagConstraints);

        jSeparator5.setOrientation(javax.swing.SwingConstants.VERTICAL);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 11;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        bottom.add(jSeparator5, gridBagConstraints);

        jLabel56.setText("Smart Vertical Placement");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        bottom.add(jLabel56, gridBagConstraints);

        textSmartVerticalOff.setText("Off");
        textVerticalGroup.add(textSmartVerticalOff);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        bottom.add(textSmartVerticalOff, gridBagConstraints);

        textSmartVerticalInside.setText("Inside");
        textVerticalGroup.add(textSmartVerticalInside);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        bottom.add(textSmartVerticalInside, gridBagConstraints);

        textSmartVerticalOutside.setText("Outside");
        textVerticalGroup.add(textSmartVerticalOutside);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        bottom.add(textSmartVerticalOutside, gridBagConstraints);

        jLabel57.setText("Smart Horizontal Placement");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        bottom.add(jLabel57, gridBagConstraints);

        textSmartHorizontalOff.setText("Off");
        textHorizontalGroup.add(textSmartHorizontalOff);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        bottom.add(textSmartHorizontalOff, gridBagConstraints);

        textSmartHorizontalInside.setText("Inside");
        textHorizontalGroup.add(textSmartHorizontalInside);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        bottom.add(textSmartHorizontalInside, gridBagConstraints);

        textSmartHorizontalOutside.setText("Outside");
        textHorizontalGroup.add(textSmartHorizontalOutside);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        bottom.add(textSmartHorizontalOutside, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        text.add(bottom, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        text.add(jSeparator3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        text.add(jSeparator4, gridBagConstraints);

        tabPane.addTab("Text", text);

        threeD.setLayout(new java.awt.GridBagLayout());

        tabPane.addTab("3D", threeD);

        technology.setLayout(new java.awt.GridBagLayout());

        jLabel46.setText("MOSIS CMOS:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        technology.add(jLabel46, gridBagConstraints);

        jLabel49.setText("Metal layers:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        technology.add(jLabel49, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        technology.add(techMOCMOSMetalLayers, gridBagConstraints);

        techMOCMOSSCMOSRules.setText("SCMOS rules (4 metal or less)");
        techMOCMOSRules.add(techMOCMOSSCMOSRules);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        technology.add(techMOCMOSSCMOSRules, gridBagConstraints);

        techMOCMOSSubmicronRules.setText("Submicron rules");
        techMOCMOSRules.add(techMOCMOSSubmicronRules);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        technology.add(techMOCMOSSubmicronRules, gridBagConstraints);

        techMOCMOSDeepRules.setText("Deep rules (5 metal or more)");
        techMOCMOSRules.add(techMOCMOSDeepRules);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        technology.add(techMOCMOSDeepRules, gridBagConstraints);

        techMOCMOSSecondPoly.setText("Second Polysilicon Layer");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        technology.add(techMOCMOSSecondPoly, gridBagConstraints);

        techMOCMOSDisallowStackedVias.setText("Disallow stacked vias");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        technology.add(techMOCMOSDisallowStackedVias, gridBagConstraints);

        techMOCMOSAlternateContactRules.setText("Alternate Active and Poly contact rules");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        technology.add(techMOCMOSAlternateContactRules, gridBagConstraints);

        techMOCMOSShowSpecialTrans.setText("Show Special transistors");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        technology.add(techMOCMOSShowSpecialTrans, gridBagConstraints);

        techMOCMOSFullGeom.setText("Full Geometry");
        techMOCMOSSticks.add(techMOCMOSFullGeom);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        technology.add(techMOCMOSFullGeom, gridBagConstraints);

        techMOCMOSStickFigures.setText("Stick Figures");
        techMOCMOSSticks.add(techMOCMOSStickFigures);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        technology.add(techMOCMOSStickFigures, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        technology.add(jSeparator6, gridBagConstraints);

        jLabel50.setText("Artwork:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 0);
        technology.add(jLabel50, gridBagConstraints);

        techArtworkArrowsFilled.setText("Arrows filled");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        technology.add(techArtworkArrowsFilled, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        technology.add(jSeparator7, gridBagConstraints);

        jLabel51.setText("Schematics:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 0);
        technology.add(jLabel51, gridBagConstraints);

        jLabel52.setText("Negating Bubble Size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        technology.add(jLabel52, gridBagConstraints);

        techSchematicsNegatingSize.setColumns(8);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        technology.add(techSchematicsNegatingSize, gridBagConstraints);

        tabPane.addTab("Technology", technology);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(tabPane, gridBagConstraints);

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
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 40, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
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
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 40);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        getContentPane().add(ok, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void cancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancelActionPerformed
	{//GEN-HEADEREND:event_cancelActionPerformed
		closeDialog(null);
	}//GEN-LAST:event_cancelActionPerformed

	private void okActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_okActionPerformed
	{//GEN-HEADEREND:event_okActionPerformed
		termNewNodes();			// terminate the New Nodes Options panel
		termNewArcs();			// terminate the New Arcs Options panel
		termSelection();		// terminate the Selection Options panel
		termPorts();			// terminate the Ports Options panel
		termFrame();			// terminate the Frame Options panel
		termIcon();				// terminate the Icon Options panel
		termGrid();				// terminate the Grid Options panel
		termLayers();			// terminate the Layers Options panel
		termColors();			// terminate the Colors Options panel
		termText();				// terminate the Text Options panel
		term3D();				// terminate the 3D Options panel
		termTechnology();		// terminate the Technology Options panel
		closeDialog(null);
	}//GEN-LAST:event_okActionPerformed
	
	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel alignPart;
    private javax.swing.JTextField arcAngle;
    private javax.swing.JRadioButton arcDefaultForAllArcs;
    private javax.swing.JRadioButton arcDefaultForArc;
    private javax.swing.JCheckBox arcDirectional;
    private javax.swing.JCheckBox arcEndsExtend;
    private javax.swing.JCheckBox arcFixedAngle;
    private javax.swing.JCheckBox arcNegated;
    private javax.swing.JLabel arcPinName;
    private javax.swing.JComboBox arcProtoList;
    private javax.swing.JButton arcReset;
    private javax.swing.JCheckBox arcRigid;
    private javax.swing.JButton arcSetPin;
    private javax.swing.JCheckBox arcSlidable;
    private javax.swing.JTextField arcWidth;
    private javax.swing.JPanel bottom;
    private javax.swing.JButton cancel;
    private javax.swing.JPanel colors;
    private javax.swing.ButtonGroup exportGroup;
    private javax.swing.JPanel frame;
    private javax.swing.JLabel frameCellName;
    private javax.swing.JTextField frameDefaultCompany;
    private javax.swing.JTextField frameDefaultDesigner;
    private javax.swing.JTextField frameDefaultProject;
    private javax.swing.ButtonGroup frameGroup;
    private javax.swing.JRadioButton frameLandscape;
    private javax.swing.JComboBox frameLibrary;
    private javax.swing.JTextField frameLibraryCompany;
    private javax.swing.JTextField frameLibraryDesigner;
    private javax.swing.JTextField frameLibraryProject;
    private javax.swing.JRadioButton framePortrait;
    private javax.swing.JComboBox frameSize;
    private javax.swing.JCheckBox frameTitleBox;
    private javax.swing.JPanel grid;
    private javax.swing.JCheckBox gridAlign;
    private javax.swing.JTextField gridAlignCursor;
    private javax.swing.JTextField gridAlignEdges;
    private javax.swing.JTextField gridBoldHoriz;
    private javax.swing.JTextField gridBoldVert;
    private javax.swing.JTextField gridCurrentHoriz;
    private javax.swing.JTextField gridCurrentVert;
    private javax.swing.JTextField gridNewHoriz;
    private javax.swing.JTextField gridNewVert;
    private javax.swing.JPanel gridPart;
    private javax.swing.JPanel icon;
    private javax.swing.JComboBox iconBidirPos;
    private javax.swing.JComboBox iconClockPos;
    private javax.swing.JLabel iconCurrentCell;
    private javax.swing.JCheckBox iconDrawBody;
    private javax.swing.JCheckBox iconDrawLeads;
    private javax.swing.JComboBox iconExportPos;
    private javax.swing.JComboBox iconExportStyle;
    private javax.swing.JComboBox iconExportTechnology;
    private javax.swing.JComboBox iconGroundPos;
    private javax.swing.JComboBox iconInputPos;
    private javax.swing.JComboBox iconInstancePos;
    private javax.swing.JTextField iconLeadLength;
    private javax.swing.JTextField iconLeadSpacing;
    private javax.swing.JButton iconMakeIcon;
    private javax.swing.JComboBox iconOutputPos;
    private javax.swing.JComboBox iconPowerPos;
    private javax.swing.JCheckBox iconReverseOrder;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel48;
    private javax.swing.JLabel jLabel49;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel54;
    private javax.swing.JLabel jLabel56;
    private javax.swing.JLabel jLabel57;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JSeparator jSeparator11;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JSeparator jSeparator9;
    private javax.swing.JComboBox layerName;
    private javax.swing.JPanel layers;
    private javax.swing.JPanel middle;
    private javax.swing.JPanel newArc;
    private javax.swing.ButtonGroup newArcGroup;
    private javax.swing.JPanel newNode;
    private javax.swing.JCheckBox nodeAllMirror;
    private javax.swing.JTextField nodeAllRotation;
    private javax.swing.JCheckBox nodeCheckCellDates;
    private javax.swing.JCheckBox nodeCopyExports;
    private javax.swing.JCheckBox nodeDisallowModificationLockedPrims;
    private javax.swing.JTextField nodeHashLimit;
    private javax.swing.JCheckBox nodeMoveAfterDuplicate;
    private javax.swing.JCheckBox nodeOverrideDefaultOrientation;
    private javax.swing.JCheckBox nodePlaceCellCenter;
    private javax.swing.JComboBox nodePrimitive;
    private javax.swing.JCheckBox nodePrimitiveMirror;
    private javax.swing.JTextField nodePrimitiveRotation;
    private javax.swing.JTextField nodePrimitiveXSize;
    private javax.swing.JTextField nodePrimitiveYSize;
    private javax.swing.JCheckBox nodeSwitchTechnology;
    private javax.swing.JCheckBox nodeTinyCellsHashedOut;
    private javax.swing.JButton ok;
    private javax.swing.JPanel port;
    private javax.swing.JRadioButton portCrossExport;
    private javax.swing.JRadioButton portCrossPort;
    private javax.swing.JRadioButton portFullExport;
    private javax.swing.JRadioButton portFullPort;
    private javax.swing.ButtonGroup portGroup;
    private javax.swing.JCheckBox portMoveNode;
    private javax.swing.JRadioButton portShortExport;
    private javax.swing.JRadioButton portShortPort;
    private javax.swing.JCheckBox selDraggingEnclosesEntireObject;
    private javax.swing.JCheckBox selEasyAnnotationText;
    private javax.swing.JCheckBox selEasyCellInstances;
    private javax.swing.JPanel selection;
    private javax.swing.JTabbedPane tabPane;
    private javax.swing.JCheckBox techArtworkArrowsFilled;
    private javax.swing.JCheckBox techMOCMOSAlternateContactRules;
    private javax.swing.JRadioButton techMOCMOSDeepRules;
    private javax.swing.JCheckBox techMOCMOSDisallowStackedVias;
    private javax.swing.JRadioButton techMOCMOSFullGeom;
    private javax.swing.JComboBox techMOCMOSMetalLayers;
    private javax.swing.ButtonGroup techMOCMOSRules;
    private javax.swing.JRadioButton techMOCMOSSCMOSRules;
    private javax.swing.JCheckBox techMOCMOSSecondPoly;
    private javax.swing.JCheckBox techMOCMOSShowSpecialTrans;
    private javax.swing.JRadioButton techMOCMOSStickFigures;
    private javax.swing.ButtonGroup techMOCMOSSticks;
    private javax.swing.JRadioButton techMOCMOSSubmicronRules;
    private javax.swing.JTextField techSchematicsNegatingSize;
    private javax.swing.JPanel technology;
    private javax.swing.JPanel text;
    private javax.swing.JRadioButton textArcs;
    private javax.swing.JCheckBox textBold;
    private javax.swing.JRadioButton textCellText;
    private javax.swing.ButtonGroup textCornerGroup;
    private javax.swing.JComboBox textEditor;
    private javax.swing.JComboBox textFace;
    private javax.swing.JRadioButton textGrabBottom;
    private javax.swing.JRadioButton textGrabBoxed;
    private javax.swing.JRadioButton textGrabCenter;
    private javax.swing.JRadioButton textGrabLeft;
    private javax.swing.JRadioButton textGrabLowerLeft;
    private javax.swing.JRadioButton textGrabLowerRight;
    private javax.swing.JRadioButton textGrabRight;
    private javax.swing.JRadioButton textGrabTop;
    private javax.swing.JRadioButton textGrabUpperLeft;
    private javax.swing.JRadioButton textGrabUpperRight;
    private javax.swing.ButtonGroup textHorizontalGroup;
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
    private javax.swing.JRadioButton textInstances;
    private javax.swing.JCheckBox textItalic;
    private javax.swing.JCheckBox textNewVisibleInsideCell;
    private javax.swing.JRadioButton textNodes;
    private javax.swing.JRadioButton textNonlayout;
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
    private javax.swing.JPanel threeD;
    private javax.swing.JPanel top;
    // End of variables declaration//GEN-END:variables
	
}
