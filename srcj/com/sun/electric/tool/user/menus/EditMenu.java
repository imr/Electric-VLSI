/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EditMenu.java
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

package com.sun.electric.tool.user.menus;

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.FPGA;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.CircuitChangeJobs;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.Clipboard;
import com.sun.electric.tool.user.Highlight2;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.Array;
import com.sun.electric.tool.user.dialogs.ArtworkLook;
import com.sun.electric.tool.user.dialogs.Attributes;
import com.sun.electric.tool.user.dialogs.BusParameters;
import com.sun.electric.tool.user.dialogs.Change;
import com.sun.electric.tool.user.dialogs.ChangeText;
import com.sun.electric.tool.user.dialogs.EditKeyBindings;
import com.sun.electric.tool.user.dialogs.FindText;
import com.sun.electric.tool.user.dialogs.GetInfoArc;
import com.sun.electric.tool.user.dialogs.GetInfoExport;
import com.sun.electric.tool.user.dialogs.GetInfoMulti;
import com.sun.electric.tool.user.dialogs.GetInfoNode;
import com.sun.electric.tool.user.dialogs.GetInfoOutline;
import com.sun.electric.tool.user.dialogs.GetInfoText;
import com.sun.electric.tool.user.dialogs.MoveBy;
import com.sun.electric.tool.user.dialogs.SelectObject;
import com.sun.electric.tool.user.dialogs.SpecialProperties;
import com.sun.electric.tool.user.dialogs.Spread;
import static com.sun.electric.tool.user.menus.EMenuItem.SEPARATOR;
import com.sun.electric.tool.user.tecEdit.LibToTech;
import com.sun.electric.tool.user.tecEdit.Manipulate;
import com.sun.electric.tool.user.tecEdit.TechToLib;
import com.sun.electric.tool.user.ui.*;
import com.sun.electric.tool.user.waveform.WaveformWindow;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.KeyStroke;

/**
 * Class to handle the commands in the "Edit" pulldown menu.
 */
public class EditMenu {

    static EMenu makeMenu() {
		/****************************** THE EDIT MENU ******************************/

		// mnemonic keys available:  B   F   JK     Q        
		// still don't have mnemonic for "Repeat Last Action"
        return new EMenu("_Edit",

            new EMenuItem("Cu_t", 'X') { public void run() {
                Clipboard.cut(); }},
            new EMenuItem("Cop_y", 'C') { public void run() {
                Clipboard.copy(); }},
            new EMenuItem("_Paste", 'V') { public void run() {
                Clipboard.paste(); }},
            new EMenuItem("Dup_licate", 'M') { public void run() {
                Clipboard.duplicate(); }},

            SEPARATOR,

       // TODO: figure out how to remove this property change listener for correct garbage collection
            ToolBar.undoCommand, // U
            ToolBar.redoCommand, // D
            new EMenuItem("Sho_w Undo List") { public void run() {
                showUndoListCommand(); }},
            new EMenuItem("Repeat Last Action", KeyStroke.getKeyStroke('&')) { public void run() {
                EMenuItem.repeatLastCommand(); }},

            SEPARATOR,

		// mnemonic keys available: AB  EFGHIJKLMN PQRSTUV XYZ
            new EMenu("_Rotate",
                new EMenuItem("90 Degrees Clock_wise") { public void run() {
                    CircuitChanges.rotateObjects(2700); }},
                new EMenuItem("90 Degrees _Counterclockwise", 'J') { public void run() {
                    CircuitChanges.rotateObjects(900); }},
                new EMenuItem("180 _Degrees") { public void run() {
                    CircuitChanges.rotateObjects(1800); }},
                new EMenuItem("_Other...") { public void run() {
                    CircuitChanges.rotateObjects(0); }}),

		// mnemonic keys available: ABCDEFGHIJK MNOPQRST VWXYZ
            new EMenu("_Mirror",
                new EMenuItem("_Up <-> Down") { public void run() {
                    CircuitChanges.mirrorObjects(true); }},
                new EMenuItem("_Left <-> Right") { public void run() {
                    CircuitChanges.mirrorObjects(false); }}),

		// mnemonic keys available:  BCDEFGH JKLM OPQRSTUVWXYZ
            new EMenu("Si_ze",
                new EMenuItem("_Interactively", 'B') { public void run() {
                    SizeListener.sizeObjects(); }},
                new EMenuItem("All Selected _Nodes...") { public void run() {
                    SizeListener.sizeAllNodes(); }},
                new EMenuItem("All Selected _Arcs...") { public void run() {
                    SizeListener.sizeAllArcs(); }}),

		// mnemonic keys available:    DEFGHIJK  NOPQ   U WXYZ
            new EMenu("Mo_ve",
                new EMenuItem("_Spread...") { public void run() {
                    Spread.showSpreadDialog(); }},
                new EMenuItem("_Move Objects By...") { public void run() {
                    MoveBy.showMoveByDialog(); }},
                new EMenuItem("_Align to Grid") { public void run() {
                    CircuitChanges.alignToGrid(); }},
                SEPARATOR,
                new EMenuItem("Align Horizontally to _Left") { public void run() {
                    CircuitChanges.alignNodes(true, 0); }},
                new EMenuItem("Align Horizontally to _Right") { public void run() {
                    CircuitChanges.alignNodes(true, 1); }},
                new EMenuItem("Align Horizontally to _Center") { public void run() {
                    CircuitChanges.alignNodes(true, 2); }},
                SEPARATOR,
                new EMenuItem("Align Vertically to _Top") { public void run() {
                    CircuitChanges.alignNodes(false, 0); }},
                new EMenuItem("Align Vertically to _Bottom") { public void run() {
                    CircuitChanges.alignNodes(false, 1); }},
                new EMenuItem("Align _Vertically to Center") { public void run() {
                    CircuitChanges.alignNodes(false, 2); }}),

            SEPARATOR,

		// mnemonic keys available:   CDEFGHIJKLMNOPQR TUVWXYZ
            new EMenu("_Erase",
                new EMenuItem("_Selected", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0)) {
                    public void run() {
                        CircuitChanges.deleteSelected(); }},    
                new EMenuItem("_Arcs Connected to Selected Nodes") { public void run() {
                    CircuitChanges.deleteArcsOnSelected(false); }},
                new EMenuItem("Arcs Connected _Between Selected Nodes") { public void run() {
                    CircuitChanges.deleteArcsOnSelected(true); }}),
                            
            new EMenuItem("_Array...", KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0)) { public void run() {
                Array.showArrayDialog(); }},
            new EMenuItem("C_hange...", KeyStroke.getKeyStroke('C', 0)) { public void run() {
                Change.showChangeDialog(); }},

            SEPARATOR,

		// mnemonic keys available:   C  FG IJK M  PQR TUVWXYZ
            new EMenu("Propert_ies",
                new EMenuItem("_Object Properties...", 'I') { public void run() {
                    getInfoCommand(false); }},
                new EMenuItem("_Attribute Properties...") { public void run() {
                    Attributes.showDialog(); }},
                SEPARATOR,
                new EMenuItem("_See All Attributes on Node") { public void run() {
                    seeAllParametersCommand(); }},
                new EMenuItem("_Hide All Attributes on Node") { public void run() {
                    hideAllParametersCommand(); }},
                new EMenuItem("_Default Attribute Visibility") { public void run() {
                    defaultParamVisibilityCommand(); }},
                new EMenuItem("Update Attributes Inheritance on _Node") { public void run() {
                    updateInheritance(false); }},
                new EMenuItem("Update Attributes Inheritance all _Libraries") { public void run() {
                    updateInheritance(true); }},
                SEPARATOR,
                new EMenuItem("Parameterize _Bus Name") { public void run() {
                    BusParameters.makeBusParameter(); }},
                new EMenuItem("_Edit Bus Parameters...") { public void run() {
                    BusParameters.showBusParametersDialog(); }}),

		// mnemonic keys available:     E G I KL  OPQ S  VWXYZ
            new EMenu("Ar_c",
                new EMenuItem("_Rigid") { public void run() {
                    CircuitChanges.arcRigidCommand(); }},
                new EMenuItem("_Not Rigid") { public void run() {
                    CircuitChanges.arcNotRigidCommand(); }},
                new EMenuItem("_Fixed Angle") { public void run() {
                    CircuitChanges.arcFixedAngleCommand(); }},
                new EMenuItem("Not Fixed _Angle") { public void run() {
                    CircuitChanges.arcNotFixedAngleCommand(); }},
                SEPARATOR,
                new EMenuItem("Toggle _Directionality") { public void run() {
                    CircuitChanges.arcDirectionalCommand(); }},
                new EMenuItem("Toggle End Extension of Both Head/Tail") { public void run() {
                    CircuitChanges.arcHeadExtendCommand(); CircuitChanges.arcTailExtendCommand();}},
                new EMenuItem("Toggle End Extension of _Head") { public void run() {
                    CircuitChanges.arcHeadExtendCommand(); }},
                new EMenuItem("Toggle End Extension of _Tail") { public void run() {
                    CircuitChanges.arcTailExtendCommand(); }},
                SEPARATOR,
                new EMenuItem("Insert _Jog In Arc") { public void run() {
                    insertJogInArcCommand(); }},
                new EMenuItem("Rip _Bus") { public void run() {
                    CircuitChanges.ripBus(); }},
                SEPARATOR,
                new EMenuItem("_Curve through Cursor") { public void run() {
                    CurveListener.setCurvature(true); }},
                new EMenuItem("Curve abo_ut Cursor") { public void run() {
                    CurveListener.setCurvature(false); }},
                new EMenuItem("Re_move Curvature") { public void run() {
                    CurveListener.removeCurvature(); }}),
                            
            ToolBar.modesSubMenu, // O                

		// mnemonic keys available: AB  E GH JKLMNOPQRS UVWXYZ
            new EMenu("Te_xt",
                new EMenuItem("_Find Text...", 'L') { public void run() {
                    FindText.findTextDialog(); }},
                new EMenuItem("_Change Text Size...") { public void run() {
                    ChangeText.changeTextDialog(); }},
                new EMenuItem("_Increase All Text Size", '=') { public void run() {
                    changeGlobalTextSize(1.25); }},
                new EMenuItem("_Decrease All Text Size", '-') { public void run() {
                    changeGlobalTextSize(0.8); }},
                new EMenuItem("Add _Text Annotation", KeyStroke.getKeyStroke('T', 0)) { public void run() {
                    PaletteFrame.placeInstance("ART_message", null, false); }}),

		// mnemonic keys available: ABCD FGHIJK M O QR TUVWXYZ
            new EMenu("Clea_nup Cell",
                new EMenuItem("Cleanup _Pins") { public void run() {
                    CircuitChanges.cleanupPinsCommand(false); }},
                new EMenuItem("Cleanup Pins _Everywhere") { public void run() {
                    CircuitChanges.cleanupPinsCommand(true); }},
                new EMenuItem("Show _Nonmanhattan") { public void run() {
                    CircuitChanges.showNonmanhattanCommand(); }},
                new EMenuItem("Show Pure _Layer Nodes") { public void run() {
                    CircuitChanges.showPureLayerCommand(); }},
                new EMenuItem("_Shorten Selected Arcs") { public void run() {
                    CircuitChanges.shortenArcsCommand(); }}),

		// mnemonic keys available:       GH JK   O QRS UVWXYZ
            new EMenu("Technolo_gy Specific",
                new EMenuItem("Toggle Port _Negation", 'T') { public void run() {
                    CircuitChanges.toggleNegatedCommand(); }},
                new EMenuItem("_Artwork Color and Pattern...") { public void run() {
                    ArtworkLook.showArtworkLookDialog(); }},
                SEPARATOR,

		// mnemonic keys available:  B DEFG IJKLM O Q  TUV XYZ
                new EMenu("_FPGA",
                    new EMenuItem("Read _Architecture And Primitives...") { public void run() {
                        FPGA.readArchitectureFile(true); }},
                    new EMenuItem("Read P_rimitives...") { public void run() {
                        FPGA.readArchitectureFile(false); }},
                    SEPARATOR,
                    new EMenuItem("Edit _Pips...") { public void run() {
                        FPGA.programPips(); }},
                    SEPARATOR,
                    new EMenuItem("Show _No Wires") { public void run() {
                        FPGA.setWireDisplay(0); }},
                    new EMenuItem("Show A_ctive Wires") { public void run() {
                        FPGA.setWireDisplay(1); }},
                    new EMenuItem("Show All _Wires") { public void run() {
                        FPGA.setWireDisplay(2); }},
                    SEPARATOR,
                    new EMenuItem("_Show Text") { public void run() {
                        FPGA.setTextDisplay(true); }},
                    new EMenuItem("_Hide Text") { public void run() {
                        FPGA.setTextDisplay(false); }}),

                SEPARATOR,
                new EMenuItem("Convert Technology to _Library for Editing...") { public void run() {
                    TechToLib.makeLibFromTech(); }},
                new EMenuItem("Convert Library to _Technology...") { public void run() {
                    LibToTech.makeTechFromLib(); }},
                SEPARATOR,
                new EMenuItem("_Identify Primitive Layers") { public void run() {
                    Manipulate.identifyLayers(false); }},
                new EMenuItem("Identify _Ports") { public void run() {
                    Manipulate.identifyLayers(true); }},
                SEPARATOR,
                new EMenuItem("Edit Library _Dependencies...") { public void run() {
                    Manipulate.editLibraryDependencies(); }},
                SEPARATOR,
                new EMenuItem("Descri_be this Technology") { public void run() {
                    describeTechnologyCommand(); }},
                new EMenuItem("Do_cument Current Technology") { public void run() {
                    Manipulate.describeTechnology(Technology.getCurrent()); }},
                SEPARATOR,
                new EMenuItem("Rena_me Current Technology...") { public void run() {
                    CircuitChanges.renameCurrentTechnology(); }}),
//              new EMenuItem("D_elete Current Technology", null, { public void run() {
//                  CircuitChanges.deleteCurrentTechnology(); }});

		// mnemonic keys available:  B   F    KLM   Q        Z
            new EMenu("_Selection",
                new EMenuItem("Sele_ct All", 'A') { public void run() {
                    selectAllCommand(); }},
                new EMenuItem("Select All _Easy") { public void run() {
                    selectEasyCommand(); }},
                new EMenuItem("Select All _Hard") { public void run() {
                    selectHardCommand(); }},
                new EMenuItem("Select Nothin_g") { public void run() {
                    selectNothingCommand(); }},
                SEPARATOR,
                new EMenuItem("Select All Like _This") { public void run() {
                    selectAllLikeThisCommand(); }},
                new EMenuItem("Select _Next Like This" /*, '\t', KeyEvent.VK_TAB */) { public void run() {
                    selectNextLikeThisCommand(true); }},
                new EMenuItem("Select _Previous Like This") { public void run() {
                    selectNextLikeThisCommand(false); }},
                SEPARATOR,
                new EMenuItem("_Select Object...") { public void run() {
                    SelectObject.selectObjectDialog(null, false); }},
                new EMenuItem("Deselect All _Arcs") { public void run() {
                    deselectAllArcsCommand(); }},
                SEPARATOR,
                new EMenuItem("Make Selected Eas_y") { public void run() {
                    selectMakeEasyCommand(); }},
                new EMenuItem("Make Selected Har_d") { public void run() {
                    selectMakeHardCommand(); }},
                SEPARATOR,
                new EMenuItem("P_ush Selection") { public void run() {
                    EditWindow wnd = EditWindow.getCurrent(); if (wnd == null) return;
                    wnd.getHighlighter().pushHighlight(); }},
                new EMenuItem("P_op Selection") { public void run() {
                    EditWindow wnd = EditWindow.getCurrent(); if (wnd ==null) return;
                    wnd.getHighlighter().popHighlight(); }},
                SEPARATOR,
                new EMenuItem("Enclosed Ob_jects") { public void run() {
                    selectEnclosedObjectsCommand(); }},
                SEPARATOR,
                new EMenuItem("Show Ne_xt Error", KeyStroke.getKeyStroke('>')) { public void run() {
                    showNextErrorCommand(); }},
                new EMenuItem("Show Pre_vious Error", KeyStroke.getKeyStroke('<')) { public void run() {
                    showPrevErrorCommand(); }},
                SEPARATOR,
                new EMenuItem("Add to Waveform _in New Panel", KeyStroke.getKeyStroke('A', 0)) { public void run() { 
                    addToWaveformNewCommand(); }},
                new EMenuItem("Add to _Waveform in Current Panel", KeyStroke.getKeyStroke('O', 0)) { public void run() {
                    addToWaveformCurrentCommand(); }},
                new EMenuItem("_Remove from Waveform", KeyStroke.getKeyStroke('R', 0)) { public void run() {
                    removeFromWaveformCommand(); }}));
    }

//	/**
//     * Repeat the last Command
//     */
//    public static void repeatLastCommand() {
//        AbstractButton lastActivated = MenuBar.repeatLastCommandListener.getLastActivated();
//        if (lastActivated != null)
//        {
//        	lastActivated.doClick();
//        }
//    }

	/**
	 * This method implements the command to show the Key Bindings Options dialog.
	 */
	public static void keyBindingsCommand()
	{
        // edit key bindings for current menu
        TopLevel top = (TopLevel)TopLevel.getCurrentJFrame();
		EditKeyBindings dialog = new EditKeyBindings(top.getEMenuBar(), top, true);
		dialog.setVisible(true);
	}

	/**
	 * This method shows the GetInfo dialog for the highlighted nodes, arcs, and/or text.
	 */
	public static void getInfoCommand(boolean doubleClick)
	{
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;
		if (wnd.getHighlighter().getNumHighlights() == 0)
		{
			// information about the cell
			Cell c = WindowFrame.getCurrentCell();
            //if (c != null) c.getInfo();
            if (c != null) Attributes.showDialog();
		} else
		{
            int [] counts = new int[5];
            NodeInst theNode = Highlight2.getInfoCommand(wnd.getHighlighter().getHighlights(), counts);
			// information about the selected items
			int arcCount = counts[0];
			int nodeCount = counts[1];
			int exportCount = counts[2];
			int textCount = counts[3];
			int graphicsCount = counts[4];

			// special dialogs for double-clicking on known nodes
			if (doubleClick)
			{
				// if double-clicked on a technology editing object, modify it
				if (arcCount == 0 && exportCount == 0 && graphicsCount == 0 &&
					(nodeCount == 1 ^ textCount == 1) && theNode != null)
				{
					int opt = Manipulate.getOptionOnNode(theNode);
					if (opt >= 0)
					{
						Manipulate.modifyObject(wnd, theNode, opt);
						return;
					}
				}

				if (arcCount == 0 && exportCount == 0 && graphicsCount == 0 &&
					nodeCount == 1 &&  textCount == 0 && theNode != null)
				{
					int ret = SpecialProperties.doubleClickOnNode(wnd, theNode);
					if (ret > 0) return;
					if (ret < 0) doubleClick = false;
				}
			}

			if (arcCount <= 1 && nodeCount <= 1 && exportCount <= 1 && textCount <= 1 && graphicsCount == 0)
			{
				if (arcCount == 1) GetInfoArc.showDialog();
				if (nodeCount == 1)
				{
					// if in outline-edit mode, show that dialog
			        if (WindowFrame.getListener() == OutlineListener.theOne)
			        {
			        	GetInfoOutline.showOutlinePropertiesDialog();
			        } else
			        {
			        	GetInfoNode.showDialog();
			        }
				}
				if (exportCount == 1)
				{
					if (doubleClick)
					{
						GetInfoText.editTextInPlace();
					} else
					{
						GetInfoExport.showDialog();
					}
				}
				if (textCount == 1)
				{
					if (doubleClick)
					{
						GetInfoText.editTextInPlace();
					} else
					{
						GetInfoText.showDialog();
					}
				}
			} else
			{
				GetInfoMulti.showDialog();
			}
		}
	}

	/**
	 * Method to handle the "See All Parameters on Node" command.
	 */
	public static void seeAllParametersCommand()
	{
		ParameterVisibility job = new ParameterVisibility(0, MenuCommands.getSelectedObjects(true, false));
	}

	/**
	 * Method to handle the "Hide All Parameters on Node" command.
	 */
	public static void hideAllParametersCommand()
	{
		ParameterVisibility job = new ParameterVisibility(1, MenuCommands.getSelectedObjects(true, false));
	}

	/**
	 * Method to handle the "Default Parameter Visibility" command.
	 */
	public static void defaultParamVisibilityCommand()
	{
		ParameterVisibility job = new ParameterVisibility(2, MenuCommands.getSelectedObjects(true, false));
	}

	/**
	 * Class to do change parameter visibility in a new thread.
	 */
	private static class ParameterVisibility extends Job
	{
		private int how;
        private List<Geometric> selected;

        protected ParameterVisibility(int how, List<Geometric> selected)
		{
			super("Change Parameter Visibility", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.how = how;
            this.selected = selected;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// change visibility of parameters on the current node(s)
			int changeCount = 0;
			List<Geometric> list = selected;
			for(Geometric geom : list)
			{
				NodeInst ni = (NodeInst)geom;
				if (!ni.isCellInstance()) continue;
				boolean changed = false;
				for(Iterator<Variable> vIt = ni.getVariables(); vIt.hasNext(); )
				{
					Variable var = vIt.next();
					Variable nVar = findParameterSource(var, ni);
					if (nVar == null) continue;
					switch (how)
					{
						case 0:			// make all parameters visible
							if (var.isDisplay()) continue;
                            ni.addVar(var.withDisplay(true));
							changed = true;
							break;
						case 1:			// make all parameters invisible
							if (!var.isDisplay()) continue;
                            ni.addVar(var.withDisplay(false));
							changed = true;
							break;
						case 2:			// make all parameters have default visiblity
							if (nVar.getTextDescriptor().isInterior())
							{
								// prototype wants parameter to be invisible
								if (!var.isDisplay()) continue;
                                ni.addVar(var.withDisplay(false));
								changed = true;
							} else
							{
								// prototype wants parameter to be visible
								if (var.isDisplay()) continue;
                                ni.addVar(var.withDisplay(true));
								changed = true;
							}
							break;
					}
				}
				if (changed)
				{
//					Undo.redrawObject(ni);
					changeCount++;
				}
			}
			if (changeCount == 0) System.out.println("No Parameter visibility changed"); else
				System.out.println("Changed visibility on " + changeCount + " nodes");
			return true;
		}

		/**
		 * Method to find the formal parameter that corresponds to the actual parameter
		 * "var" on node "ni".  Returns null if not a parameter or cannot be found.
		 */
		private Variable findParameterSource(Variable var, NodeInst ni)
		{
			// find this parameter in the cell
			Cell np = (Cell)ni.getProto();
			Cell cnp = np.contentsView();
			if (cnp != null) np = cnp;
			for(Iterator<Variable> it = np.getVariables(); it.hasNext(); )
			{
				Variable nVar = it.next();
				if (var.getKey() == nVar.getKey()) return nVar;
			}
			return null;
		}
	}

    public static void updateInheritance(boolean allLibraries)
    {
        // get currently selected node(s)
        List<Geometric> highlighted = MenuCommands.getSelectedObjects(true, false);
        UpdateAttributes job = new UpdateAttributes(highlighted, allLibraries, 0);
    }

    private static class UpdateAttributes extends Job {
        private List<Geometric> highlighted;
        private boolean allLibraries;
        private int whatToUpdate;

        /**
         * Update Attributes.
         * @param highlighted currently highlighted objects
         * @param allLibraries if true, update all nodeinsts in all libraries, otherwise update
         * highlighted
         * @param whatToUpdate if 0, update inheritance. If 1, update attributes locations.
         */
        UpdateAttributes(List<Geometric> highlighted, boolean allLibraries, int whatToUpdate) {
            super("Update Inheritance", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.highlighted = highlighted;
            this.allLibraries = allLibraries;
            this.whatToUpdate = whatToUpdate;
            startJob();
        }

        public boolean doIt() throws JobException {
            int count = 0;
            if (allLibraries) {
                for (Iterator<Library> it = Library.getLibraries(); it.hasNext(); ) {
                    Library lib = it.next();
                    for (Iterator<Cell> it2 = lib.getCells(); it2.hasNext(); ) {
                        Cell c = it2.next();
                        for (Iterator<NodeInst> it3 = c.getNodes(); it3.hasNext(); ) {
                            NodeInst ni = it3.next();
                            if (ni.isCellInstance()) {
                                if (whatToUpdate == 0) {
                                    updateInheritance(ni, (Cell)ni.getProto());
                                    count++;
                                }
                                if (whatToUpdate == 1) {
                                    updateLocations(ni, (Cell)ni.getProto());
                                    count++;
                                }
                            }
                        }
                    }
                }
            } else {
                for (Geometric eobj : highlighted) {
                    if (eobj instanceof NodeInst) {
                        NodeInst ni = (NodeInst)eobj;
                        if (ni.isCellInstance()) {
                            if (whatToUpdate == 0) {
                                updateInheritance(ni, (Cell)ni.getProto());
                                count++;
                            }
                            if (whatToUpdate == 1) {
                                updateLocations(ni, (Cell)ni.getProto());
                                count++;
                            }
                        }
                    }
                }
            }
            if (whatToUpdate == 0)
                System.out.println("Updated Attribute Inheritance on "+count+" nodes");
            if (whatToUpdate == 1)
                System.out.println("Updated Attribute Locations on "+count+" nodes");
            return true;
        }

        private void updateInheritance(NodeInst ni, Cell proto) {
        	CircuitChangeJobs.inheritAttributes(ni, true);
        }

        private void updateLocations(NodeInst ni, Cell proto) {

        }
    }

    /**
     * Method to change the global text scale by a given amount.
     * @param scale the amount to scale the global text size.
     */
    public static void changeGlobalTextSize(double scale)
    {
    	double curScale = User.getGlobalTextScale();
    	curScale *= scale;
    	if (curScale != 0)
    	{
    		User.setGlobalTextScale(curScale);
    		EditWindow.repaintAllContents();
    	}
    }

    /**
	 * This method implements the command to highlight all objects in the current Cell.
	 */
	public static void selectAllCommand()
	{
		doSelection(false, false);
	}

	/**
	 * This method implements the command to highlight all objects in the current Cell
	 * that are easy to select.
	 */
	public static void selectEasyCommand()
	{
		doSelection(true, false);
	}

	/**
	 * This method implements the command to highlight all objects in the current Cell
	 * that are hard to select.
	 */
	public static void selectHardCommand()
	{
		doSelection(false, true);
	}

	private static void doSelection(boolean mustBeEasy, boolean mustBeHard)
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();

		// compute bounds for multi-page schematics
		Rectangle2D thisPageBounds = null;
    	if (curCell.isMultiPage())
    	{
	    	int curPage = wnd.getMultiPageNumber();
	        Dimension d = new Dimension();
	        int frameFactor = Cell.FrameDescription.getCellFrameInfo(curCell, d);
	        if (frameFactor == 0 && curCell.isMultiPage())
            {
            	double offY = curPage * Cell.FrameDescription.MULTIPAGESEPARATION;
				thisPageBounds = new Rectangle2D.Double(-d.getWidth()/2, -d.getHeight()/2+offY, d.getWidth(), d.getHeight());
            }
    	}

		boolean cellsAreHard = !User.isEasySelectionOfCellInstances();
		highlighter.clear();
		for(Iterator<NodeInst> it = curCell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();

			// for multipage schematics, restrict to current page
			if (thisPageBounds != null)
			{
				if (!thisPageBounds.contains(ni.getAnchorCenter())) continue;
			}

			// "select all" should not include the cell-center
			if (ni.getProto() == Generic.tech.cellCenterNode && !mustBeEasy && !mustBeHard) continue;
			boolean hard = ni.isHardSelect();
			if ((ni.isCellInstance()) && cellsAreHard) hard = true;
			if (mustBeEasy && hard) continue;
			if (mustBeHard && !hard) continue;
			if (!ni.isInvisiblePinWithText())
				highlighter.addElectricObject(ni, curCell);
            if (User.isTextVisibilityOnNode())
			{
				if (ni.isUsernamed())
					highlighter.addText(ni, curCell, NodeInst.NODE_NAME);
				for(Iterator<Variable> vIt = ni.getVariables(); vIt.hasNext(); )
				{
					Variable var = vIt.next();
					if (var.isDisplay())
						highlighter.addText(ni, curCell, var.getKey());
				}
			}
		}
		for(Iterator<ArcInst> it = curCell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = it.next();

			// for multipage schematics, restrict to current page
			if (thisPageBounds != null)
			{
				if (!thisPageBounds.contains(ai.getHeadLocation())) continue;
			}
			boolean hard = ai.isHardSelect();
			if (mustBeEasy && hard) continue;
			if (mustBeHard && !hard) continue;
			highlighter.addElectricObject(ai, curCell);
            if (User.isTextVisibilityOnArc())
			{
				if (ai.isUsernamed())
					highlighter.addText(ai, curCell, ArcInst.ARC_NAME);
				for(Iterator<Variable> vIt = ai.getVariables(); vIt.hasNext(); )
				{
					Variable var = vIt.next();
					if (var.isDisplay())
						highlighter.addText(ai, curCell, var.getKey());
				}
			}
		}
		for(Iterator<Export> it = curCell.getExports(); it.hasNext(); )
		{
			Export pp = it.next();
			highlighter.addText(pp, curCell, null);
		}

		// Selecting annotations
        if (User.isTextVisibilityOnCell())
		{
			for(Iterator<Variable> it = curCell.getVariables(); it.hasNext(); )
			{
				Variable var = it.next();
				if (var.isAttribute())
				{
					// for multipage schematics, restrict to current page
					if (thisPageBounds != null)
					{
						if (!thisPageBounds.contains(new Point2D.Double(var.getXOff(), var.getYOff()))) continue;
					}
					highlighter.addText(curCell, curCell, var.getKey());
				}
			}
		}
		highlighter.finished();
	}

	/**
	 * This method implements the command to highlight all objects in the current Cell
	 * that are like the currently selected object.
	 */
	public static void selectAllLikeThisCommand()
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();

		HashMap<Object,Object> likeThis = new HashMap<Object,Object>();
		List<Geometric> highlighted = highlighter.getHighlightedEObjs(true, true);
		for(Geometric geom : highlighted)
		{
			if (geom instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)geom;
				likeThis.put(ni.getProto(), ni);
			} else
			{
				ArcInst ai = (ArcInst)geom;
				likeThis.put(ai.getProto(), ai);
			}
		}

		highlighter.clear();
		for(Iterator<NodeInst> it = curCell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			Object isLikeThis = likeThis.get(ni.getProto());
			if (isLikeThis == null) continue;
			if (ni.isInvisiblePinWithText())
			{
				for(Iterator<Variable> vIt = ni.getVariables(); vIt.hasNext(); )
				{
					Variable var = vIt.next();
					if (var.isDisplay())
					{
						highlighter.addText(ni, curCell, var.getKey());
						break;
					}
				}
			} else
			{
				highlighter.addElectricObject(ni, curCell);
			}
		}
		for(Iterator<ArcInst> it = curCell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = it.next();
			Object isLikeThis = likeThis.get(ai.getProto());
			if (isLikeThis == null) continue;
			highlighter.addElectricObject(ai, curCell);
		}
		highlighter.finished();
        System.out.println("Selected "+highlighter.getNumHighlights()+ " objects");
	}

	/**
	 * Method to select the next object that is of the same type as the current object.
	 */
	public static void selectNextLikeThisCommand(boolean next)
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();
		Highlight2 high = highlighter.getOneHighlight();
		if (high == null) return;
        ElectricObject eObj = high.getElectricObject();
        if (high.isHighlightEOBJ())
		{
            if (eObj instanceof PortInst)
            {
            	eObj = ((PortInst)eObj).getNodeInst();
            }
			if (eObj instanceof NodeInst)
			{
				NodeInst thisNi = (NodeInst)eObj;
				NodeInst [] allNodes = new NodeInst[curCell.getNumNodes()];
				int tot = 0;
				int which = 0;
				for(Iterator<NodeInst> it = curCell.getNodes(); it.hasNext(); )
				{
					NodeInst ni = it.next();
					if (ni.getProto() != thisNi.getProto()) continue;
					if (ni == thisNi) which = tot;
					allNodes[tot++] = ni;
				}
				if (next)
				{
					which++;
					if (which >= tot) which = 0;
				} else
				{
					which--;
					if (which < 0) which = tot - 1;
				}
				highlighter.clear();
				highlighter.addElectricObject(allNodes[which], curCell);
				highlighter.finished();
				return;
			}
			if (eObj instanceof ArcInst)
			{
				ArcInst thisAi = (ArcInst)eObj;
				ArcInst [] allArcs = new ArcInst[curCell.getNumArcs()];
				int tot = 0;
				int which = 0;
				for(Iterator<ArcInst> it = curCell.getArcs(); it.hasNext(); )
				{
					ArcInst ai = it.next();
					if (ai.getProto() != thisAi.getProto()) continue;
					if (ai == thisAi) which = tot;
					allArcs[tot++] = ai;
				}
				if (next)
				{
					which++;
					if (which >= tot) which = 0;
				} else
				{
					which--;
					if (which < 0) which = tot - 1;
				}
				highlighter.clear();
				highlighter.addElectricObject(allArcs[which], curCell);
				highlighter.finished();
				return;
			}
		}
        if (high.isHighlightText())
        {
			if (eObj instanceof Export)
			{
				Export [] allExports = new Export[curCell.getNumPorts()];
				int i = 0;
				int which = 0;
				for(Iterator<Export> it = curCell.getExports(); it.hasNext(); )
				{
					Export e = it.next();
					if (e == eObj) which = i;
					allExports[i++] = e;
				}
				if (next)
				{
					which++;
					if (which >= allExports.length) which = 0;
				} else
				{
					which--;
					if (which < 0) which = allExports.length - 1;
				}
				highlighter.clear();
				highlighter.addText(allExports[which], curCell, Export.EXPORT_NAME);
				highlighter.finished();
				return;
			}
        }
        System.out.println("Cannot advance the current selection");
	}

	/**
	 * This method implements the command to highlight nothing in the current Cell.
	 */
	public static void selectNothingCommand()
	{
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;
		wnd.getHighlighter().clear();
		wnd.getHighlighter().finished();
	}

	/**
	 * This method implements the command to deselect all selected arcs.
	 */
	public static void deselectAllArcsCommand()
	{
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();

		List<Highlight2> newHighList = new ArrayList<Highlight2>();
		for(Highlight2 h : highlighter.getHighlights())
		{
			if (h.isHighlightEOBJ() || h.isHighlightText())
			{
				if (h.getElectricObject() instanceof ArcInst) continue;
			}
			newHighList.add(h);
		}
		highlighter.clear();
		highlighter.setHighlightList(newHighList);
		highlighter.finished();
	}

	/**
	 * This method implements the command to make all selected objects be easy-to-select.
	 */
	public static void selectMakeEasyCommand()
	{
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;
		List<Geometric> highlighted = wnd.getHighlighter().getHighlightedEObjs(true, true);
		new SetEasyHardSelect(true, highlighted);
	}

	/**
	 * This method implements the command to make all selected objects be hard-to-select.
	 */
	public static void selectMakeHardCommand()
	{
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;
		List<Geometric> highlighted = wnd.getHighlighter().getHighlightedEObjs(true, true);
		new SetEasyHardSelect(false, highlighted);
	}

	/**
	 * Class to set selected objects "easy to select" or "hard to select".
	 */
	private static class SetEasyHardSelect extends Job
	{
	    private boolean easy;
		private List<Geometric> highlighted;
	
		private SetEasyHardSelect(boolean easy, List<Geometric> highlighted)
		{
	        super("Make Selected Objects Easy/Hard To Select", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
	        this.easy = easy;
	        this.highlighted = highlighted;
	        startJob();
	    }
	
	    public boolean doIt() throws JobException
		{
			for(Geometric geom : highlighted)
			{
				if (geom instanceof NodeInst)
				{
					NodeInst ni = (NodeInst)geom;
					if (easy)
					{
						ni.clearHardSelect();
					} else
					{
						ni.setHardSelect();
					}
				} else
				{
					ArcInst ai = (ArcInst)geom;
					if (easy)
					{
						ai.setHardSelect(false);
					} else
					{
						ai.setHardSelect(true);
					}
				}
			}
			return true;
		}
	}

	/**
	 * This method implements the command to replace the rectangular highlight
	 * with the selection of objects in that rectangle.
	 */
	public static void selectEnclosedObjectsCommand()
	{
		EditWindow wnd = EditWindow.needCurrent();
		if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();
		Rectangle2D selection = highlighter.getHighlightedArea(wnd);
		highlighter.clear();
		if (selection != null )
			highlighter.selectArea(wnd, selection.getMinX(), selection.getMaxX(), selection.getMinY(), selection.getMaxY(), false,
			ToolBar.isSelectSpecial());
		highlighter.finished();
	}

	/**
	 * This method implements the command to show the next logged error.
	 * The error log lists the results of the latest command (DRC, NCC, etc.)
	 */
	public static void showNextErrorCommand()
	{
		String msg = ErrorLoggerTree.reportNextMessage();
		System.out.println(msg);
	}

	/**
	 * This method implements the command to show the last logged error.
	 * The error log lists the results of the latest command (DRC, NCC, etc.)
	 */
	public static void showPrevErrorCommand()
	{
		String msg = ErrorLoggerTree.reportPrevMessage();
		System.out.println(msg);
	}

	/**
	 * This method implements the command to add the currently selected network
	 * to the waveform window, in a new panel.
	 */
	public static void addToWaveformNewCommand()
	{
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (!(wf.getContent() instanceof EditWindow)) return;
        EditWindow wnd = (EditWindow)wf.getContent();

		WaveformWindow.Locator wwLoc = new WaveformWindow.Locator(wnd);
		WaveformWindow ww = wwLoc.getWaveformWindow();
		if (ww == null)
		{
			System.out.println("Cannot add selected signals to the waveform window: no waveform window is associated with this cell");
			return;
		}
		ww.showSignals(wnd.getHighlighter(), wwLoc.getContext(), true);
	}

	/**
	 * This method implements the command to add the currently selected network
	 * to the waveform window, overlaid on top of the current panel.
	 */
	public static void addToWaveformCurrentCommand()
	{
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (!(wf.getContent() instanceof EditWindow)) return;
        EditWindow wnd = (EditWindow)wf.getContent();
		WaveformWindow.Locator wwLoc = new WaveformWindow.Locator(wnd);
		WaveformWindow ww = wwLoc.getWaveformWindow();
		if (ww == null)
		{
			System.out.println("Cannot overlay selected signals to the waveform window: no waveform window is associated with this cell");
			return;
		}
		ww.showSignals(wnd.getHighlighter(), wwLoc.getContext(), false);
	}

	/**
	 * This method implements the command to remove the currently selected network
	 * from the waveform window.
	 */
	public static void removeFromWaveformCommand()
	{
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (!(wf.getContent() instanceof EditWindow)) return;
        EditWindow wnd = (EditWindow)wf.getContent();
		WaveformWindow.Locator wwLoc = new WaveformWindow.Locator(wnd);
		WaveformWindow ww = wwLoc.getWaveformWindow();
		if (ww == null)
		{
			System.out.println("Cannot remove selected signals from the waveform window: no waveform window is associated with this cell");
			return;
		}
		Set<Network> nets = wnd.getHighlighter().getHighlightedNetworks();
		ww.removeSignals(nets, wwLoc.getContext());
	}

    /**
     * This method implements the command to insert a jog in an arc
     */
    public static void insertJogInArcCommand()
    {
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        ArcInst ai = (ArcInst)wnd.getHighlighter().getOneElectricObject(ArcInst.class);
        if (ai == null) return;

        System.out.println("Select the position in the arc to place the jog");
        EventListener currentListener = WindowFrame.getListener();
        WindowFrame.setListener(new InsertJogInArcListener(wnd, ai, currentListener));
    }

    /**
     * Class to handle the interactive selection of a jog point in an arc.
     */
    private static class InsertJogInArcListener
        implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener
    {
        private EditWindow wnd;
        private ArcInst ai;
        private EventListener currentListener;

        /**
         * Create a new insert-jog-point listener
         * @param wnd Controlling window
         * @param ai the arc that is having a jog inserted.
         * @param currentListener listener to restore when done
         */
        public InsertJogInArcListener(EditWindow wnd, ArcInst ai, EventListener currentListener)
        {
            this.wnd = wnd;
            this.ai = ai;
            this.currentListener = currentListener;
        }

        public void mousePressed(MouseEvent evt) {}
        public void mouseClicked(MouseEvent evt) {}
        public void mouseEntered(MouseEvent evt) {}
        public void mouseExited(MouseEvent evt) {}

        public void mouseDragged(MouseEvent evt)
        {
            mouseMoved(evt);
        }

        public void mouseReleased(MouseEvent evt)
        {
            Point2D insert2D = getInsertPoint(evt);
            EPoint insert = new EPoint(insert2D.getX(), insert2D.getY());
            InsertJogPoint job = new InsertJogPoint(ai, insert, wnd.getHighlighter());
            WindowFrame.setListener(currentListener);
        }

        public void mouseMoved(MouseEvent evt)
        {
            Point2D insert = getInsertPoint(evt);
            double x = insert.getX();
            double y = insert.getY();

            double width = (ai.getWidth() - ai.getProto().getWidthOffset()) / 2;
            Highlighter highlighter = wnd.getHighlighter();
            highlighter.clear();
            highlighter.addLine(new Point2D.Double(x-width, y-width), new Point2D.Double(x-width, y+width), ai.getParent());
            highlighter.addLine(new Point2D.Double(x-width, y+width), new Point2D.Double(x+width, y+width), ai.getParent());
            highlighter.addLine(new Point2D.Double(x+width, y+width), new Point2D.Double(x+width, y-width), ai.getParent());
            highlighter.addLine(new Point2D.Double(x+width, y-width), new Point2D.Double(x-width, y-width), ai.getParent());
            highlighter.finished();
            wnd.repaint();
        }

        private Point2D getInsertPoint(MouseEvent evt)
        {
            Point2D mouseDB = wnd.screenToDatabase((int)evt.getX(), (int)evt.getY());
			EditWindow.gridAlign(mouseDB);
            Point2D insert = DBMath.closestPointToSegment(ai.getHeadLocation(), ai.getTailLocation(), mouseDB);
            return insert;
        }

        public void mouseWheelMoved(MouseWheelEvent e) {}

        public void keyPressed(KeyEvent e) {}

        public void keyReleased(KeyEvent e) {}

        public void keyTyped(KeyEvent e) {}

        private static class InsertJogPoint extends Job
        {
            private ArcInst ai;
            private EPoint insert;
            private transient Highlighter highlighter;
            private NodeInst jogPoint;

            protected InsertJogPoint(ArcInst ai, EPoint insert, Highlighter highlighter)
            {
                super("Insert Jog in Arc", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
                this.ai = ai;
                this.insert = insert;
                this.highlighter = highlighter;
                startJob();
            }

            public boolean doIt() throws JobException
            {
                if (CircuitChangeJobs.cantEdit(ai.getParent(), null, true) != 0) return false;

                // create the break pins
                ArcProto ap = ai.getProto();
                NodeProto np = ap.findPinProto();
                if (np == null) return false;
                NodeInst ni = NodeInst.makeInstance(np, insert, np.getDefWidth(), np.getDefHeight(), ai.getParent());
                if (ni == null)
                {
                    System.out.println("Cannot create pin " + np.describe(true));
                    return false;
                }
                NodeInst ni2 = NodeInst.makeInstance(np, insert, np.getDefWidth(), np.getDefHeight(), ai.getParent());
                if (ni2 == null)
                {
                    System.out.println("Cannot create pin " + np.describe(true));
                    return false;
                }

                // get location of connection to these pins
                PortInst pi = ni.getOnlyPortInst();
                PortInst pi2 = ni2.getOnlyPortInst();

//				// see if edge alignment is appropriate
//				if (us_edgealignment_ratio != 0 && (ai->end[0].xpos == ai->end[1].xpos ||
//					ai->end[0].ypos == ai->end[1].ypos))
//				{
//					edgealignment = muldiv(us_edgealignment_ratio, WHOLE, el_curlib->lambda[el_curtech->techindex]);
//					px = us_alignvalue(x, edgealignment, &otheralign);
//					py = us_alignvalue(y, edgealignment, &otheralign);
//					if (px != x || py != y)
//					{
//						// shift the nodes and make sure the ports are still valid
//						startobjectchange((INTBIG)ni, VNODEINST);
//						modifynodeinst(ni, px-x, py-y, px-x, py-y, 0, 0);
//						endobjectchange((INTBIG)ni, VNODEINST);
//						startobjectchange((INTBIG)ni2, VNODEINST);
//						modifynodeinst(ni2, px-x, py-y, px-x, py-y, 0, 0);
//						endobjectchange((INTBIG)ni2, VNODEINST);
//						(void)shapeportpoly(ni, ppt, poly, FALSE);
//						if (!isinside(nx, ny, poly)) getcenter(poly, &nx, &ny);
//					}
//				}

                // now save the arc information and delete it
                PortInst headPort = ai.getHeadPortInst();
                PortInst tailPort = ai.getTailPortInst();
                Point2D headPt = ai.getHeadLocation();
                Point2D tailPt = ai.getTailLocation();
                double width = ai.getWidth();
                String arcName = ai.getName();
                int angle = (ai.getAngle() + 900) % 3600;

                // create the new arcs
                ArcInst newAi1 = ArcInst.makeInstance(ap, width, headPort, pi, headPt, insert, null);
                ArcInst newAi2 = ArcInst.makeInstance(ap, width, pi, pi2, insert, insert, null);
                ArcInst newAi3 = ArcInst.makeInstance(ap, width, pi2, tailPort, insert, tailPt, null);
				newAi1.setHeadNegated(ai.isHeadNegated());
                newAi1.setHeadExtended(ai.isHeadExtended());
                newAi1.setHeadArrowed(ai.isHeadArrowed());
				newAi3.setTailNegated(ai.isTailNegated());
                newAi3.setTailExtended(ai.isTailExtended());
                newAi3.setTailArrowed(ai.isTailArrowed());
				ai.kill();
                if (arcName != null)
                {
                    if (headPt.distance(insert) > tailPt.distance(insert))
                    {
                        newAi1.setName(arcName);
                        newAi1.copyTextDescriptorFrom(ai, ArcInst.ARC_NAME);
                    } else
                    {
                        newAi3.setName(arcName);
                        newAi3.copyTextDescriptorFrom(ai, ArcInst.ARC_NAME);
                    }
                }
                newAi2.setAngle(angle);

                // remember the node to be highlighted
                jogPoint = ni;
    			fieldVariableChanged("jogPoint");
                return true;
            }

            public void terminateOK()
            {
                // highlight one of the jog nodes
                highlighter.clear();
                highlighter.addElectricObject(jogPoint, jogPoint.getParent());
                highlighter.finished();
            }
        }
    }

    /**
     * This method implements the command to show the undo history.
     */
    public static void showUndoListCommand()
    {
        Undo.showHistoryList();
    }

    public static void describeTechnologyCommand()
    {
        Technology tech = Technology.getCurrent();
        System.out.println("Technology " + tech.getTechName());
        System.out.println("    Full name: " + tech.getTechDesc());
        if (tech.isScaleRelevant())
        {
            System.out.println("    Scale: 1 grid unit is " + tech.getScale() + " nanometers (" +
                (tech.getScale()/1000) + " microns)");
        }
        int arcCount = 0;
        for(Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); )
        {
            ArcProto ap = it.next();
            if (!ap.isNotUsed()) arcCount++;
        }
        StringBuffer sb = new StringBuffer();
        sb.append("    Has " + arcCount + " arcs (wires):");
        for(Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); )
        {
            ArcProto ap = it.next();
            if (ap.isNotUsed()) continue;
            sb.append(" " + ap.getName());
        }
        System.out.println(sb.toString());

        int pinCount = 0, totalCount = 0, pureCount = 0, contactCount = 0;
        for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
        {
            PrimitiveNode np = it.next();
            if (np.isNotUsed()) continue;
            PrimitiveNode.Function fun = np.getFunction();
            totalCount++;
            if (fun == PrimitiveNode.Function.PIN) pinCount++; else
            if (fun == PrimitiveNode.Function.CONTACT || fun == PrimitiveNode.Function.CONNECT) contactCount++; else
            if (fun == PrimitiveNode.Function.NODE) pureCount++;
        }
        if (pinCount > 0)
        {
            sb = new StringBuffer();
            sb.append("    Has " + pinCount + " pin nodes for making bends in arcs:");
            for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
            {
                PrimitiveNode np = it.next();
                if (np.isNotUsed()) continue;
                PrimitiveNode.Function fun = np.getFunction();
                if (fun == PrimitiveNode.Function.PIN) sb.append(" " + np.getName());
            }
            System.out.println(sb.toString());
        }
        if (contactCount > 0)
        {
            sb = new StringBuffer();
            sb.append("    Has " + contactCount + " contact nodes for joining different arcs:");
            for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
            {
                PrimitiveNode np = it.next();
                if (np.isNotUsed()) continue;
                PrimitiveNode.Function fun = np.getFunction();
                if (fun == PrimitiveNode.Function.CONTACT || fun == PrimitiveNode.Function.CONNECT)
                    sb.append(" " + np.getName());
            }
            System.out.println(sb.toString());
        }
        if (pinCount+contactCount+pureCount < totalCount)
        {
            sb = new StringBuffer();
            sb.append("    Has " + (totalCount-pinCount-contactCount-pureCount) + " regular nodes:");
            for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
            {
                PrimitiveNode np = it.next();
                if (np.isNotUsed()) continue;
                PrimitiveNode.Function fun = np.getFunction();
                if (fun != PrimitiveNode.Function.PIN && fun != PrimitiveNode.Function.CONTACT &&
                    fun != PrimitiveNode.Function.CONNECT && fun != PrimitiveNode.Function.NODE)
                        sb.append(" " + np.getName());
            }
            System.out.println(sb.toString());
        }
        if (pureCount > 0)
        {
            sb = new StringBuffer();
            sb.append("    Has " + pureCount + " pure-layer nodes for creating custom geometry:");
            for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
            {
                PrimitiveNode np = it.next();
                if (np.isNotUsed()) continue;
                PrimitiveNode.Function fun = np.getFunction();
                if (fun == PrimitiveNode.Function.NODE) sb.append(" " + np.getName());
            }
            System.out.println(sb.toString());
        }
    }

}
