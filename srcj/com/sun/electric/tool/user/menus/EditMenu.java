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
package com.sun.electric.tool.user.menus;

import static com.sun.electric.tool.user.menus.EMenuItem.SEPARATOR;

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.Variable.Key;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Xml;
import com.sun.electric.technology.technologies.FPGA;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Client;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.sandbox.TechExplorerDriver;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.CircuitChangeJobs;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.Clipboard;
import com.sun.electric.tool.user.Highlight;
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
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.dialogs.SelectObject;
import com.sun.electric.tool.user.dialogs.SpecialProperties;
import com.sun.electric.tool.user.dialogs.Spread;
import com.sun.electric.tool.user.tecEdit.LibToTech;
import com.sun.electric.tool.user.tecEdit.Manipulate;
import com.sun.electric.tool.user.tecEdit.TechToLib;
import com.sun.electric.tool.user.tecEditWizard.TechEditWizard;
import com.sun.electric.tool.user.ui.ClickZoomWireListener;
import com.sun.electric.tool.user.ui.CurveListener;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.ErrorLoggerTree;
import com.sun.electric.tool.user.ui.LayerVisibility;
import com.sun.electric.tool.user.ui.MessagesWindow;
import com.sun.electric.tool.user.ui.OutlineListener;
import com.sun.electric.tool.user.ui.PaletteFrame;
import com.sun.electric.tool.user.ui.SizeListener;
import com.sun.electric.tool.user.ui.TextWindow;
import com.sun.electric.tool.user.ui.ToolBar;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.waveform.WaveformWindow;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

/**
 * Class to handle the commands in the "Edit" pulldown menu.
 */
public class EditMenu {

    static EMenu makeMenu() {
    	int ctrl = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    	int ctrlShift = ctrl | InputEvent.SHIFT_DOWN_MASK;

    	/****************************** THE EDIT MENU ******************************/

		// mnemonic keys available:  B       JK     Q
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
				new EMenuItem("Move Objects Left", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0)) { public void run() {
					ClickZoomWireListener.moveSelected(-1, 0, false, false); }},
				new EMenuItem("Move Objects Right", KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0)) { public void run() {
					ClickZoomWireListener.moveSelected(1, 0, false, false); }},
				new EMenuItem("Move Objects Up", KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)) { public void run() {
					ClickZoomWireListener.moveSelected(0, 1, false, false); }},
				new EMenuItem("Move Objects Down", KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)) { public void run() {
					ClickZoomWireListener.moveSelected(0, -1, false, false); }},
				SEPARATOR,
				new EMenuItem("Move Objects More Left", new KeyStroke [] { KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, ctrl),
		    		KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_DOWN_MASK)}) { public void run() {
		    			ClickZoomWireListener.moveSelected(-1, 0, true, false); }},
				new EMenuItem("Move Objects More Right", new KeyStroke [] { KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, ctrl),
		    		KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK)}) { public void run() {
		    			ClickZoomWireListener.moveSelected(1, 0, true, false); }},
				new EMenuItem("Move Objects More Up", new KeyStroke [] { KeyStroke.getKeyStroke(KeyEvent.VK_UP, ctrl),
		    		KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.SHIFT_DOWN_MASK)}) { public void run() {
		    			ClickZoomWireListener.moveSelected(0, 1, true, false); }},
				new EMenuItem("Move Objects More Down", new KeyStroke [] { KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, ctrl),
		    		KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.SHIFT_DOWN_MASK)}) { public void run() {
		    			ClickZoomWireListener.moveSelected(0, -1, true, false); }},
				SEPARATOR,
				new EMenuItem("Move Objects Most Left", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, ctrlShift)) { public void run() {
					ClickZoomWireListener.moveSelected(-1, 0, true, true); }},
				new EMenuItem("Move Objects Most Right", KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, ctrlShift)) { public void run() {
					ClickZoomWireListener.moveSelected(1, 0, true, true); }},
				new EMenuItem("Move Objects Most Up", KeyStroke.getKeyStroke(KeyEvent.VK_UP, ctrlShift)) { public void run() {
					ClickZoomWireListener.moveSelected(0, 1, true, true); }},
				new EMenuItem("Move Objects Most Down", KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, ctrlShift)) { public void run() {
					ClickZoomWireListener.moveSelected(0, -1, true, true); }},
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
					CircuitChanges.alignNodes(false, 2); }},
                SEPARATOR,
				new EMenuItem("Cell Center to Center of Selection") { public void run() {
					CircuitChanges.cellCenterToCenterOfSelection(); }}
                      ),

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

		// mnemonic keys available: A CD FGHIJK M   QRSTUVWXYZ
			new EMenu("Propert_ies",
				new EMenuItem("_Object Properties...", 'I') { public void run() {
					getInfoCommand(false); }},
				SEPARATOR,
				new EMenuItem("Cell _Parameters...") { public void run() {
					Attributes.showDialog(); }},
				new EMenuItem("Update Parameters on _Node") { public void run() {
					updateParameters(false); }},
				new EMenuItem("Update Parameters all _Libraries") { public void run() {
					updateParameters(true); }},
				SEPARATOR,
				new EMenuItem("Parameterize _Bus Name") { public void run() {
					BusParameters.makeBusParameter(); }},
				new EMenuItem("_Edit Bus Parameters...") { public void run() {
					BusParameters.showBusParametersDialog(); }}),

		// mnemonic keys available:     E G I KL   PQ S  VWXYZ
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
				new EMenuItem("Toggle End Extension _of Both Head/Tail") { public void run() {
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

		// mnemonic keys available: AB    GH JKLMNOPQRS UVWXYZ
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
					PaletteFrame.placeInstance("ART_message", null, false); }},
				new EMenuItem("Edit Text Cell _Externally...") { public void run() {
					editExternally(); }}),

		// mnemonic keys available: ABCD FGHIJK M O Q  TUVWXYZ
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
					CircuitChanges.shortenArcsCommand(); }},
				new EMenuItem("Show _Redundant Pure-Layer Nodes") { public void run() {
					CircuitChanges.showRedundantPureLayerNodes(); }}),

		// mnemonic keys available:    DE GHIJKLM OPQRSTUVWXYZ
			new EMenu("Technology Speci_fic",
				new EMenuItem("Toggle Port _Negation", 'T') { public void run() {
					CircuitChanges.toggleNegatedCommand(); }},
				new EMenuItem("_Artwork Color and Pattern...") { public void run() {
					ArtworkLook.showArtworkLookDialog(); }},
				SEPARATOR,
				new EMenuItem("Descri_be this Technology") { public void run() {
					describeTechnologyCommand(); }},
				new EMenuItem("Do_cument Current Technology") { public void run() {
					Manipulate.describeTechnology(Technology.getCurrent()); }},
				new EMenuItem("Write XML of Current Technology...") { public void run()
				{
					Technology tech = Technology.getCurrent();
//                    if (!tech.isLayout())
//                    {
//                        System.out.println(tech + " is not a layout technology.");
//                        System.out.println("Only layout technologies can be exported as XML");
//                        return;
//                    }
                    // If there is a representation in XML. Technologies defined as Java classes don't seem
                    // to have the correct XML info.
                    if (tech.isXmlTechAvailable())
                    {
                        Xml.Technology xmlTech = tech.getXmlTech();
                        if (xmlTech == null)
                            xmlTech = tech.makeXml();
                        String fileName = tech.getTechName() + ".xml";
                        fileName = OpenFile.chooseOutputFile(FileType.XML, "Technology XML File", fileName);
                        if (fileName != null) // didn't press cancel button
                        {
                            boolean includeDateAndVersion = User.isIncludeDateAndVersionInOutput();
                            String copyrightMessage = IOTool.isUseCopyrightMessage() ? IOTool.getCopyrightMessage() : null;
                            xmlTech.writeXml(fileName, includeDateAndVersion, copyrightMessage);
                        }
                    }
                    else
                    {
                        System.out.println("Technology '" + tech.getTechName() + "' is not available for XML export");
                    }
                }},
				new EMenuItem("Write XML of Technology from Old Electric Build...") { public void run() {
					writeXmlTechnologyFromElectricBuildCommand(); }},
				SEPARATOR,
//				new EMenuItem("Rename Current Technology...") { public void run() {
//					CircuitChanges.renameCurrentTechnology(); }},
//				SEPARATOR,

				// mnemonic keys available:  B DEFG IJKLM O Q  TUV XYZ
				new EMenu("_FPGA",
					new EMenuItem("Read _Architecture And Primitives...") { public void run() {
						FPGA.tech().readArchitectureFile(true); }},
					new EMenuItem("Read P_rimitives...") { public void run() {
						FPGA.tech().readArchitectureFile(false); }},
					SEPARATOR,
					new EMenuItem("Edit _Pips...") { public void run() {
						FPGA.tech().programPips(); }},
					SEPARATOR,
					new EMenuItem("Show _No Wires") { public void run() {
						FPGA.tech().setWireDisplay(0); }},
					new EMenuItem("Show A_ctive Wires") { public void run() {
						FPGA.tech().setWireDisplay(1); }},
					new EMenuItem("Show All _Wires") { public void run() {
						FPGA.tech().setWireDisplay(2); }},
					SEPARATOR,
					new EMenuItem("_Show Text") { public void run() {
						FPGA.tech().setTextDisplay(true); }},
					new EMenuItem("_Hide Text") { public void run() {
						FPGA.tech().setTextDisplay(false); }})),

		// mnemonic keys available: AB  EFGH JK MNO QRS UV XYZ
			new EMenu("Technolo_gy Editing",
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
				new EMenuItem("Edit _Component Menu...") { public void run() {
					Manipulate.editComponentMenu(); }},
				SEPARATOR,
				new EMenuItem("Technology Creation _Wizard...") { public void run() {
					TechEditWizard.techEditWizardCommand(); }}),

		// mnemonic keys available:      F    K     Q        Z
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
				new EMenuItem("Select O_bject...") { public void run() {
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
					showNextErrorCommand(true); }},
				new EMenuItem("Show Pre_vious Error", KeyStroke.getKeyStroke('<')) { public void run() {
					showPrevErrorCommand(true); }},
				new EMenuItem("Show Next Error, _same Window", KeyStroke.getKeyStroke(']')) { public void run() {
					showNextErrorCommand(false); }},
				new EMenuItem("Show Previous Error, sa_me Window", KeyStroke.getKeyStroke('[')) { public void run() {
					showPrevErrorCommand(false); }},
                new EMenuItem("Show Single Geometry", KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, 0)) { public void run() {
					showSingleGeometryCommand(); }},
                new EMenuItem("Show Current Co_llection of Errors") { public void run() {
					ErrorLoggerTree.showCurrentErrors(); }},
				SEPARATOR,
				new EMenuItem("Add to Waveform _in New Panel", KeyStroke.getKeyStroke('A', 0)) { public void run() {
					addToWaveformNewCommand(); }},
				new EMenuItem("Add to _Waveform in Current Panel", KeyStroke.getKeyStroke('O', 0)) { public void run() {
					addToWaveformCurrentCommand(); }},
				new EMenuItem("_Remove from Waveform", KeyStroke.getKeyStroke('R', 0)) { public void run() {
					removeFromWaveformCommand(); }}));
	}

	/**
	 * This method implements the command to show the Key Bindings Options dialog.
	 */
	public static void keyBindingsCommand()
	{
		// edit key bindings for current menu
		TopLevel top = TopLevel.getCurrentJFrame();
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
			System.out.println("Must select an object first");
		} else
		{
			int [] counts = new int[5];
			NodeInst theNode = Highlight.getInfoCommand(wnd.getHighlighter().getHighlights(), counts);
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

			if (arcCount+nodeCount+exportCount+textCount == 1 && graphicsCount == 0)
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

//	/**
//	 * Method to move selected object(s).  If either scaleMove or scaleMove2
//     * is true, the move is multiplied by the grid Bold frequency.  If both are
//     * true the move gets multiplied twice.
//     * @param dX amount to move in X in lambda
//     * @param dY amount to move in Y in lambda
//     * @param scaleMove scales move up if true
//     * @param scaleMove2 scales move up if true (stacks with scaleMove)
//     */
//    private static void moveSelected(double dX, double dY, boolean scaleMove, boolean scaleMove2)
//    {
//        // scale distance according to arrow motion
//        EditWindow wnd = EditWindow.getCurrent();
//        if (wnd == null) return;
//        Highlighter highlighter = wnd.getHighlighter();
//		double arrowDistance = User.getAlignmentToGrid();
//		dX *= arrowDistance;
//		dY *= arrowDistance;
//		int scaleX = User.getDefGridXBoldFrequency();
//        int scaleY = User.getDefGridYBoldFrequency();
//		if (scaleMove) { dX *= scaleX;   dY *= scaleY; }
//		if (scaleMove2) { dX *= scaleX;   dY *= scaleY; }
//		highlighter.setHighlightOffset(0, 0);
//		if (wnd.isInPlaceEdit())
//		{
//			Point2D delta = new Point2D.Double(dX, dY);
//			AffineTransform trans = wnd.getInPlaceTransformIn();
//	        double m00 = trans.getScaleX();
//	        double m01 = trans.getShearX();
//	        double m10 = trans.getShearY();
//	        double m11 = trans.getScaleY();
//			AffineTransform justRot = new AffineTransform(m00, m10, m01, m11, 0, 0);
//			justRot.transform(delta, delta);
//			dX = delta.getX();
//			dY = delta.getY();
//		}
//		CircuitChanges.manyMove(dX, dY);
//		wnd.fullRepaint();
//	}

	/**
	 * Method to handle the "See All Parameters on Node" command.
	 */
	public static void seeAllParametersCommand()
	{
		new ParameterVisibility(0, MenuCommands.getSelectedObjects(true, false));
	}

	/**
	 * Method to handle the "Hide All Parameters on Node" command.
	 */
	public static void hideAllParametersCommand()
	{
		new ParameterVisibility(1, MenuCommands.getSelectedObjects(true, false));
	}

	/**
	 * Method to handle the "Default Parameter Visibility" command.
	 */
	public static void defaultParamVisibilityCommand()
	{
		new ParameterVisibility(2, MenuCommands.getSelectedObjects(true, false));
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
				for(Iterator<Variable> vIt = ni.getParameters(); vIt.hasNext(); )
				{
					Variable var = vIt.next();
					Variable nVar = ((Cell)ni.getProto()).getParameter(var.getKey());
					switch (how)
					{
						case 0:			// make all parameters visible
							if (var.isDisplay()) continue;
							ni.addParameter(var.withDisplay(true));
							changed = true;
							break;
						case 1:			// make all parameters invisible
							if (!var.isDisplay()) continue;
							ni.addParameter(var.withTextDescriptor(var.getTextDescriptor().withDisplay(TextDescriptor.Display.HIDDEN)));
							changed = true;
							break;
						case 2:			// make all parameters have default visiblity
							if (nVar.getTextDescriptor().isInterior())
							{
								// prototype wants parameter to be invisible
								if (!var.isDisplay()) continue;
								ni.addParameter(var.withDisplay(false));
								changed = true;
							} else
							{
								// prototype wants parameter to be visible
								if (var.isDisplay()) continue;
								ni.addParameter(var.withDisplay(true));
								changed = true;
							}
							break;
					}
				}
				if (changed)
				{
					changeCount++;
				}
			}
			if (changeCount == 0) System.out.println("No Parameter visibility changed"); else
				System.out.println("Changed visibility on " + changeCount + " nodes");
			return true;
		}
	}

	public static void updateParameters(boolean allLibraries)
	{
		// get currently selected node(s)
		List<Geometric> highlighted = MenuCommands.getSelectedObjects(true, false);
		new UpdateParameters(highlighted, allLibraries);
	}

	private static class UpdateParameters extends Job {
		private List<Geometric> highlighted;
		private boolean allLibraries;

		/**
		 * Update Parameters.
		 * @param highlighted currently highlighted objects
		 * @param allLibraries if true, update all nodeinsts in all libraries, otherwise update highlighted
		 */
		UpdateParameters(List<Geometric> highlighted, boolean allLibraries) {
			super("Update Parameters", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.highlighted = highlighted;
			this.allLibraries = allLibraries;
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
								CircuitChangeJobs.inheritAttributes(ni);
								count++;
							}
						}
					}
				}
			} else {
				for (Geometric eobj : highlighted) {
					if (eobj instanceof NodeInst) {
						NodeInst ni = (NodeInst)eobj;
						if (ni.isCellInstance()) {
							CircuitChangeJobs.inheritAttributes(ni);
							count++;
						}
					}
				}
			}
			System.out.println("Updated Parameters on " + count + " nodes");
			return true;
		}
	}

	/**
	 * Method to change the global text scale by a given amount.
	 * @param scale the amount to scale the global text size.
	 */
	public static void changeGlobalTextSize(double scale)
	{
		EditWindow wnd = EditWindow.needCurrent();
		if (wnd == null) return;
		double curScale = wnd.getGlobalTextScale();
		curScale *= scale;
		if (curScale != 0)
		{
			wnd.setGlobalTextScale(curScale);
			EditWindow.repaintAllContents();
		}
	}

	/**
	 * Method to edit the current text-cell in an external editor.
	 */
	private static void editExternally()
	{
		TextWindow tw = null;
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf != null && wf.getContent() instanceof TextWindow)
		{
			tw = (TextWindow)wf.getContent();
		}
		if (tw == null)
		{
			Job.getUserInterface().showErrorMessage("You must be editing a text cell before editing it externally", "No Text To Edit");
			return;
		}
		String externalEditor = User.getDefaultTextExternalEditor();
		if (externalEditor.length() == 0)
		{
			Job.getUserInterface().showErrorMessage("No external text editor is defined.  Use the Display/Text Preferences to set one", "No Text Editor Set");
			return;
		}
		Cell cell = tw.getCell();
		File f = null;
        String fileName = cell.getName() + "tmp"; // prefix in File.createTempFile must be longer than 2

        try
        {
            f = File.createTempFile(fileName, null);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

		if (f == null) return;
        fileName = f.getPath();
		if (!tw.writeTextCell(fileName))
		{
			// error with written file
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
				"Could not save temporary file " + fileName, "Error saving temporary file", JOptionPane.ERROR_MESSAGE);
			return;
		}
		(new EditExternally(tw, externalEditor, fileName, f)).start();
	}

	/**
	 * Class to do external text editing in a separate thread, allowing Electric to continue to run.
	 */
	private static class EditExternally extends Thread
	{
		private TextWindow tw;
		private String externalEditor, fileName;
		private File f;

		EditExternally(TextWindow tw, String externalEditor, String fileName, File f)
		{
			this.tw = tw;
			this.externalEditor = externalEditor;
			this.fileName = fileName;
			this.f = f;
		}

		public void run()
		{
			try
			{
				Client.OS os = Client.getOperatingSystem();
				String commandString;
				if (os == Client.OS.WINDOWS) commandString = "cmd /c \"" + externalEditor + "\" " + fileName;
				else if (os == Client.OS.MACINTOSH)
				{
					// MacOS box only allows the selection of *.app programs.
					int index = externalEditor.indexOf(".app"); // like TextEdit.app
					if (index != -1)
					{
						String rootName = externalEditor.substring(0, index);
						int ind2 = rootName.lastIndexOf("/");
						if (ind2 != -1) // remove all /
							rootName = rootName.substring(ind2, rootName.length());
						commandString = externalEditor + "/Contents/MacOS/" + rootName + " " + fileName;
					}
					else
						commandString = externalEditor + " " + fileName;
				}
				else
					commandString = externalEditor + " " + fileName;
				Process p = Runtime.getRuntime().exec(commandString);
				try
				{
					p.waitFor();
				} catch (InterruptedException e)
				{
					System.out.println("External text editor interrupted: " + e);
				}
			} catch (IOException e)
			{
				System.out.println("IO Exception: " + e);
			}
			tw.readTextCell(fileName);
			tw.goToLineNumber(1);

	        if (f.delete())
	            System.out.println("** Deleted " + fileName + " **");
	        else
	            System.out.println("Failed to delete " + fileName);
		}
	}

	/**
	 * This method implements the command to highlight all objects in the current Cell.
	 */
	public static void selectAllCommand()
	{
        // is this the messages window?
        MessagesWindow mw = TopLevel.getMessagesWindow();
        if (mw.isFocusOwner())
        {
        	mw.selectAll();
        	return;
        }

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
        LayerVisibility lv = wnd.getLayerVisibility();
		for(Iterator<NodeInst> it = curCell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();

			// for multipage schematics, restrict to current page
			if (thisPageBounds != null)
			{
				if (!thisPageBounds.contains(ni.getAnchorCenter())) continue;
			}

			// "select all" should not include the cell-center
			if (ni.getProto() == Generic.tech().cellCenterNode && !mustBeEasy && !mustBeHard) continue;
			boolean hard = ni.isHardSelect();
			if (ni.isCellInstance() && cellsAreHard) hard = true;
			if (mustBeEasy && hard) continue;
			if (mustBeHard && !hard && !ni.isInvisiblePinWithText()) continue;

			// do not show primitives with all layers invisible
			if (!User.isHighlightInvisibleObjects() && !ni.isCellInstance())
			{
				PrimitiveNode np = (PrimitiveNode)ni.getProto();
				if (!lv.isVisible(np)) continue;
			}
			if (!ni.isInvisiblePinWithText() || mustBeHard)
				highlighter.addElectricObject(ni, curCell);
			if (ni.isInvisiblePinWithText() && mustBeHard && !hard) continue;
			if (User.isTextVisibilityOnNode())
			{
				if (ni.isUsernamed())
					highlighter.addText(ni, curCell, NodeInst.NODE_NAME);
				for(Iterator<Variable> vIt = ni.getParametersAndVariables(); vIt.hasNext(); )
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

			// do not include arcs that have all layers invisible
			if (!User.isHighlightInvisibleObjects() && !lv.isVisible(ai.getProto())) continue;

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
			for(Iterator<Variable> it = curCell.getParametersAndVariables(); it.hasNext(); )
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

		// make a set of prototypes and characteristics to match
		Set<Object> likeThis = new HashSet<Object>();
		for(Highlight h : highlighter.getHighlights())
		{
			// handle attribute text
			if (h.isHighlightText())
			{
				Key key = h.getVarKey();
				if (key != null && key != Export.EXPORT_NAME)
				{
					likeThis.add(key.getName());
					continue;
				}
			}

			ElectricObject eObj = h.getElectricObject();
			if (eObj instanceof PortInst) eObj = ((PortInst)eObj).getNodeInst();
			if (eObj instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)eObj;
				likeThis.add(ni.getProto());
			} else if (eObj instanceof ArcInst)
			{
				ArcInst ai = (ArcInst)eObj;
				likeThis.add(ai.getProto());
			} else if (eObj instanceof Export)
			{
				Export e = (Export)eObj;
				PortCharacteristic pc = e.getCharacteristic();
				likeThis.add(pc.getName());
			}
		}

		highlighter.clear();
		for(Iterator<NodeInst> it = curCell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (likeThis.contains(ni.getProto()))
			{
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
            if (likeThis.contains(NodeInst.NODE_NAME.getName()))
                highlighter.addText(ni, curCell, NodeInst.NODE_NAME);
            for(Iterator<Variable> vIt = ni.getParametersAndVariables(); vIt.hasNext(); )
			{
				Variable var = vIt.next();
				if (likeThis.contains(var.getKey().getName()))
					highlighter.addText(ni, curCell, var.getKey());
			}
		}
		for(Iterator<ArcInst> it = curCell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = it.next();
            if (likeThis.contains(ai.getProto()))
				highlighter.addElectricObject(ai, curCell);
            if (likeThis.contains(ArcInst.ARC_NAME.getName()))
                highlighter.addText(ai, curCell, ArcInst.ARC_NAME);
            for(Iterator<Variable> vIt = ai.getVariables(); vIt.hasNext(); )
			{
				Variable var = vIt.next();
				if (likeThis.contains(var.getKey().getName()))
					highlighter.addText(ai, curCell, var.getKey());
			}
		}
		for(Iterator<Export> it = curCell.getExports(); it.hasNext(); )
		{
			Export e = it.next();
			PortCharacteristic pc = e.getCharacteristic();
			if (!likeThis.contains(pc.getName())) continue;
			highlighter.addText(e, curCell, Export.EXPORT_NAME);
		}
		for(Iterator<Variable> vIt = curCell.getParametersAndVariables(); vIt.hasNext(); )
		{
			Variable var = vIt.next();
			if (likeThis.contains(var.getKey().getName()))
				highlighter.addText(curCell, curCell, var.getKey());
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
		Highlight high = highlighter.getOneHighlight();
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
				PortCharacteristic pc = ((Export)eObj).getCharacteristic();
				List<Export> allExports = new ArrayList<Export>();
				int which = 0;
				for(Iterator<Export> it = curCell.getExports(); it.hasNext(); )
				{
					Export e = it.next();
					if (e.getCharacteristic() != pc) continue;
					if (e == eObj) which = allExports.size();
					allExports.add(e);
				}
				if (next)
				{
					which++;
					if (which >= allExports.size()) which = 0;
				} else
				{
					which--;
					if (which < 0) which = allExports.size() - 1;
				}
				highlighter.clear();
				highlighter.addText(allExports.get(which), curCell, Export.EXPORT_NAME);
				highlighter.finished();
			} else
			{
				// advance to next with this name
				List<Key> allVarKeys = new ArrayList<Key>();
				List<ElectricObject> allVarObjs = new ArrayList<ElectricObject>();
				int which = 0;
				for(Iterator<ArcInst> it = curCell.getArcs(); it.hasNext(); )
				{
					ArcInst ai = it.next();
					for(Iterator<Variable> vIt = ai.getVariables(); vIt.hasNext(); )
					{
						Variable var = vIt.next();
						if (var.getKey() != high.getVarKey()) continue;
						if (ai == high.getElectricObject() && var.getKey() == high.getVarKey()) which = allVarKeys.size();
						allVarKeys.add(var.getKey());
						allVarObjs.add(ai);
					}
				}
				for(Iterator<NodeInst> it = curCell.getNodes(); it.hasNext(); )
				{
					NodeInst ni = it.next();
					for(Iterator<Variable> vIt = ni.getParametersAndVariables(); vIt.hasNext(); )
					{
						Variable var = vIt.next();
						if (var.getKey() != high.getVarKey()) continue;
						if (ni == high.getElectricObject() && var.getKey() == high.getVarKey()) which = allVarKeys.size();
						allVarKeys.add(var.getKey());
						allVarObjs.add(ni);
					}
				}
				for(Iterator<Variable> vIt = curCell.getVariables(); vIt.hasNext(); )
				{
					Variable var = vIt.next();
					if (var.getKey() != high.getVarKey()) continue;
					if (curCell == high.getElectricObject() && var.getKey() == high.getVarKey()) which = allVarKeys.size();
					allVarKeys.add(var.getKey());
					allVarObjs.add(curCell);
				}
				if (next)
				{
					which++;
					if (which >= allVarKeys.size()) which = 0;
				} else
				{
					which--;
					if (which < 0) which = allVarKeys.size() - 1;
				}
				highlighter.clear();
				highlighter.addText(allVarObjs.get(which), curCell, allVarKeys.get(which));
				highlighter.finished();
			}
			return;
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

        List<Highlight> newHighList = new ArrayList<Highlight>();
        for(Highlight h : highlighter.getHighlights())
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
     * @param separateWindow true to show each cell in its own window; false to show in the current window.
     */
    private static void showNextErrorCommand(boolean separateWindow)
    {
    	// if the display preference "show cell results in new window" is unchecked, always disable separate window display
    	if (!User.isShowCellsInNewWindow()) separateWindow = false;

    	String msg = ErrorLoggerTree.reportNextMessage(separateWindow);
        System.out.println(msg);
    }

    /**
     * This method implements the command to iterate along geometries found in the
     * current MessageLog
     * The error log lists the results of the latest command (DRC, NCC, etc.)
     */
    private static void showSingleGeometryCommand()
    {
        ErrorLoggerTree.reportSingleGeometry(true);
    }

    /**
     * This method implements the command to show the last logged error.
     * The error log lists the results of the latest command (DRC, NCC, etc.)
     * @param separateWindow true to show each cell in its own window; false to show in the current window.
     */
    private static void showPrevErrorCommand(boolean separateWindow)
    {
    	// if the display preference "show cell results in new window" is unchecked, always disable separate window display
    	if (!User.isShowCellsInNewWindow()) separateWindow = false;

    	String msg = ErrorLoggerTree.reportPrevMessage(separateWindow);
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
            new InsertJogPoint(ai, insert, wnd.getHighlighter());
            WindowFrame.setListener(currentListener);
        }

        public void mouseMoved(MouseEvent evt)
        {
            Point2D insert = getInsertPoint(evt);
            double x = insert.getX();
            double y = insert.getY();

            double width = ai.getLambdaBaseWidth() / 2;
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
            Point2D mouseDB = wnd.screenToDatabase(evt.getX(), evt.getY());
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
                if (CircuitChangeJobs.cantEdit(ai.getParent(), null, true, false, true) != 0) return false;

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

                // now save the arc information and delete it
                PortInst headPort = ai.getHeadPortInst();
                PortInst tailPort = ai.getTailPortInst();
                Point2D headPt = ai.getHeadLocation();
                Point2D tailPt = ai.getTailLocation();
                double width = ai.getLambdaBaseWidth();
                String arcName = ai.getName();
                int angle = ai.getDefinedAngle();
                angle = (angle + 900) % 3600;

                // create the new arcs
                ArcInst newAi1 = ArcInst.makeInstanceBase(ap, width, headPort, pi, headPt, insert, null);
                ArcInst newAi2 = ArcInst.makeInstanceBase(ap, width, pi, pi2, insert, insert, null);
                ArcInst newAi3 = ArcInst.makeInstanceBase(ap, width, pi2, tailPort, insert, tailPt, null);
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
                highlighter.addElectricObject(jogPoint.getOnlyPortInst(), jogPoint.getParent());
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
        int pageWidth = TopLevel.getMessagesWindow().getMessagesCharWidth();
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
        int newLineIndent = sb.length();
        for(Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); )
        {
            ArcProto ap = it.next();
            if (ap.isNotUsed()) continue;
            String addThis = " " + ap.getName();
            if (sb.length() + addThis.length() > pageWidth)
            {
                System.out.println(sb.toString());
                sb = new StringBuffer();
                for(int i=0; i<newLineIndent; i++) sb.append(' ');
            }
            sb.append(addThis);
        }
        System.out.println(sb.toString());

        int pinCount = 0, totalCount = 0, pureCount = 0, contactCount = 0;
        for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
        {
            PrimitiveNode np = it.next();
            if (np.isNotUsed()) continue;
            PrimitiveNode.Function fun = np.getFunction();
            totalCount++;
            if (fun.isPin()) pinCount++; else
            if (fun.isContact() || fun == PrimitiveNode.Function.CONNECT) contactCount++; else
            if (fun == PrimitiveNode.Function.NODE) pureCount++;
        }
        if (pinCount > 0)
        {
            sb = new StringBuffer();
            sb.append("    Has " + pinCount + " pin nodes:");
            newLineIndent = sb.length();
            for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
            {
                PrimitiveNode np = it.next();
                if (np.isNotUsed()) continue;
                PrimitiveNode.Function fun = np.getFunction();
                if (!fun.isPin()) continue;
                String addThis = " " + np.getName();
                if (sb.length() + addThis.length() > pageWidth)
                {
                    System.out.println(sb.toString());
                    sb = new StringBuffer();
                    for(int i=0; i<newLineIndent; i++) sb.append(' ');
                }
                sb.append(addThis);
            }
            System.out.println(sb.toString());
        }
        if (contactCount > 0)
        {
            sb = new StringBuffer();
            sb.append("    Has " + contactCount + " contact nodes:");
            newLineIndent = sb.length();
            for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
            {
                PrimitiveNode np = it.next();
                if (np.isNotUsed()) continue;
                PrimitiveNode.Function fun = np.getFunction();
                if (!fun.isContact() && fun != PrimitiveNode.Function.CONNECT)
                    continue;
                String addThis = " " + np.getName();
                if (sb.length() + addThis.length() > pageWidth)
                {
                    System.out.println(sb.toString());
                    sb = new StringBuffer();
                    for(int i=0; i<newLineIndent; i++) sb.append(' ');
                }
                sb.append(addThis);
            }
            System.out.println(sb.toString());
        }
        if (pinCount+contactCount+pureCount < totalCount)
        {
            sb = new StringBuffer();
            sb.append("    Has " + (totalCount-pinCount-contactCount-pureCount) + " regular nodes:");
            newLineIndent = sb.length();
            for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
            {
                PrimitiveNode np = it.next();
                if (np.isNotUsed()) continue;
                PrimitiveNode.Function fun = np.getFunction();
                if (fun.isPin() || fun.isContact() ||
                    fun == PrimitiveNode.Function.CONNECT || fun == PrimitiveNode.Function.NODE)
                    continue;
                String addThis = " " + np.getName();
                if (sb.length() + addThis.length() > pageWidth)
                {
                    System.out.println(sb.toString());
                    sb = new StringBuffer();
                    for(int i=0; i<newLineIndent; i++) sb.append(' ');
                }
                sb.append(addThis);
            }
            System.out.println(sb.toString());
        }
        if (pureCount > 0)
        {
            sb = new StringBuffer();
            sb.append("    Has " + pureCount + " pure-layer nodes:");
            newLineIndent = sb.length();
            for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
            {
                PrimitiveNode np = it.next();
                if (np.isNotUsed()) continue;
                PrimitiveNode.Function fun = np.getFunction();
                if (fun != PrimitiveNode.Function.NODE) continue;
                String addThis = " " + np.getName();
                if (sb.length() + addThis.length() > pageWidth)
                {
                    System.out.println(sb.toString());
                    sb = new StringBuffer();
                    for(int i=0; i<newLineIndent; i++) sb.append(' ');
                }
                sb.append(addThis);
            }
            System.out.println(sb.toString());
        }
    }

    private static void writeXmlTechnologyFromElectricBuildCommand() {
        String jarPath = OpenFile.chooseInputFile(FileType.JAR, "Electric build", false, null, false);
        if (jarPath == null) return;

        try {
            new TechExplorerDriver(jarPath, System.out) {
                private int state = 0;

                @Override
                protected void terminateOk(Object result) {
                    switch (state) {
                        case 0:
                            putCommand("initTechnologies");
                            state = 1;
                            break;
                        case 1:
                            String[] techNames = (String[])result;
                    		String chosen = (String)JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(),
                                    "Technology to Write",
                                    "Choose a technology to write",
                                    JOptionPane.QUESTION_MESSAGE, null,
                                    techNames, null);
                            if (chosen != null)
                                putCommand("makeXml", chosen);
                            else
                                closeCommands();
                            state = 2;
                            break;
                        case 2:
                            Xml.Technology xmlTech = (Xml.Technology)result;
                            if (xmlTech != null) {
                                String fileName = xmlTech.techName + ".xml";
                                fileName = OpenFile.chooseOutputFile(FileType.XML, "Technology XML File", fileName);
                                if (fileName != null) {
                                    boolean includeDateAndVersion = User.isIncludeDateAndVersionInOutput();
                                    String copyrightMessage = IOTool.isUseCopyrightMessage() ? IOTool.getCopyrightMessage() : null;
                                    xmlTech.writeXml(fileName, includeDateAndVersion, copyrightMessage);
                                }
                            }
                            closeCommands();
                            break;
                        default:
                            super.terminateOk(result);
                            closeCommands();
                   }
                }

                @Override
                protected void terminateFail(Exception e) {
                    super.terminateFail(e);
                    closeCommands();
                }
            };
        } catch (IOException e) {
            ActivityLogger.logException(e);
            e.printStackTrace();
        }
    }

}
