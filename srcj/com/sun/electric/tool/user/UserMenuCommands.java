/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UserMenuCommands.java
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
package com.sun.electric.tool.user;

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.hierarchy.NodeUsage;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.DialogOpenFile;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.Input;
import com.sun.electric.tool.io.Output;
import com.sun.electric.tool.user.Clipboard;
import com.sun.electric.tool.user.ui.Menu;
import com.sun.electric.tool.user.dialogs.About;
import com.sun.electric.tool.user.dialogs.EditCell;
import com.sun.electric.tool.user.dialogs.ToolOptions;
import com.sun.electric.tool.user.dialogs.EditOptions;
import com.sun.electric.tool.user.dialogs.IOOptions;
import com.sun.electric.tool.user.dialogs.CrossLibCopy;
import com.sun.electric.tool.user.dialogs.NewExport;
import com.sun.electric.tool.logicaleffort.LENetlister;
import com.sun.electric.tool.logicaleffort.LETool;
import com.sun.electric.tool.misc.PadGenerator;
import com.sun.electric.tool.simulation.Spice;
import com.sun.electric.tool.simulation.IRSIMTool;
//import com.sun.electric.tool.ncc.factory.NetFactory;

import java.util.Iterator;
import java.util.ArrayList;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.KeyStroke;

/**
 * This class has all of the pulldown menu commands in Electric.
 */
public final class UserMenuCommands
{
	// It is never useful for anyone to create an instance of this class
	private UserMenuCommands() {}

	/**
	 * Routine to create the pulldown menus.
	 */
	public static JMenuBar createMenuBar()
	{
		// create the menu bar
		JMenuBar menuBar = new JMenuBar();

		// setup the File menu
		Menu fileMenu = Menu.createMenu("File", 'F');
		menuBar.add(fileMenu);
		fileMenu.addMenuItem("Open", KeyStroke.getKeyStroke('O', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { openLibraryCommand(); } });
		Menu importSubMenu = Menu.createMenu("Import");
		fileMenu.add(importSubMenu);
		importSubMenu.addMenuItem("Readable Dump", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { importLibraryCommand(); } });
		fileMenu.addMenuItem("Save", KeyStroke.getKeyStroke('S', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { saveLibraryCommand(); } });
		fileMenu.addMenuItem("Save as...",null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { saveAsLibraryCommand(); } });
		if (TopLevel.getOperatingSystem() == TopLevel.OS.MACINTOSH)
		{
//			MRJApplicationUtils.registerQuitHandler(new MRJQuitHandler()
//			{
//				public void handleQuit() { quitCommand(); }
//			});
		} else
		{
			fileMenu.addSeparator();
			fileMenu.addMenuItem("Quit", KeyStroke.getKeyStroke('Q', InputEvent.CTRL_MASK),
				new ActionListener() { public void actionPerformed(ActionEvent e) { quitCommand(); } });
		}

		// setup the Edit menu
		Menu editMenu = Menu.createMenu("Edit", 'E');
		menuBar.add(editMenu);
		editMenu.addMenuItem("Undo", KeyStroke.getKeyStroke('Z', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { new UndoCommand(); } });
		editMenu.addMenuItem("Redo", KeyStroke.getKeyStroke('Y', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { new RedoCommand(); } });
		editMenu.addMenuItem("Show Undo List", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { showUndoListCommand(); } });
		editMenu.addSeparator();
		editMenu.addMenuItem("Cut", KeyStroke.getKeyStroke('X', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { cutCommand(); } });
		editMenu.addMenuItem("Copy", KeyStroke.getKeyStroke('C', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { copyCommand(); } });
		editMenu.addMenuItem("Paste", KeyStroke.getKeyStroke('V', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { pasteCommand(); } });
		editMenu.addSeparator();
		Menu arcSubMenu = Menu.createMenu("Arc", 'A');
		editMenu.add(arcSubMenu);
        arcSubMenu.addMenuItem("Rigid", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { arcRigidCommand(); }});
        arcSubMenu.addMenuItem("Not Rigid", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { arcNotRigidCommand(); }});
        arcSubMenu.addMenuItem("Fixed Angle", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { arcFixedAngleCommand(); }});
        arcSubMenu.addMenuItem("Not Fixed Angle", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { arcNotFixedAngleCommand(); }});
		editMenu.addSeparator();
		editMenu.addMenuItem("I/O Options...",null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { ioOptionsCommand(); } });
		editMenu.addMenuItem("Edit Options...",null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { editOptionsCommand(); } });
		editMenu.addMenuItem("Tool Options...",null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { toolOptionsCommand(); } });
		editMenu.addSeparator();
		editMenu.addMenuItem("Get Info", KeyStroke.getKeyStroke('I', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { getInfoCommand(); } });

		// setup the Cell menu
		Menu cellMenu = Menu.createMenu("Cell", 'C');
		menuBar.add(cellMenu);
		cellMenu.addMenuItem("Edit Cell", KeyStroke.getKeyStroke('N', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { editCellCommand(); } });
		cellMenu.addSeparator();
        cellMenu.addMenuItem("Down Hierarchy", KeyStroke.getKeyStroke('D', InputEvent.CTRL_MASK),
            new ActionListener() { public void actionPerformed(ActionEvent e) { downHierCommand(); }});
        cellMenu.addMenuItem("Up Hierarchy", KeyStroke.getKeyStroke('U', InputEvent.CTRL_MASK),
            new ActionListener() { public void actionPerformed(ActionEvent e) { upHierCommand(); }});
		cellMenu.addMenuItem("Show Cell Groups", KeyStroke.getKeyStroke('T', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { showCellGroupsCommand(); } });
		cellMenu.addSeparator();
		cellMenu.addMenuItem("Cross-Library Copy...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { crossLibraryCopyCommand(); } });

		// setup the Export menu
		Menu exportMenu = Menu.createMenu("Export", 'X');
		menuBar.add(exportMenu);
		exportMenu.addMenuItem("Create Export...", KeyStroke.getKeyStroke('E', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { newExportCommand(); } });

		// setup the View menu
		Menu viewMenu = Menu.createMenu("View", 'V');
		menuBar.add(viewMenu);

		// setup the Window menu
		Menu windowMenu = Menu.createMenu("Window", 'W');
		menuBar.add(windowMenu);
		windowMenu.addMenuItem("Full Display", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { fullDisplayCommand(); } });
		windowMenu.addMenuItem("Toggle Grid", KeyStroke.getKeyStroke('G', InputEvent.CTRL_MASK),
			new ActionListener() { public void actionPerformed(ActionEvent e) { toggleGridCommand(); } });

		// setup the Tool menu
		Menu toolMenu = Menu.createMenu("Tool", 'T');
		menuBar.add(toolMenu);
		Menu drcSubMenu = Menu.createMenu("DRC", 'D');
		toolMenu.add(drcSubMenu);
		Menu simulationSubMenu = Menu.createMenu("Simulation", 'S');
		toolMenu.add(simulationSubMenu);
		simulationSubMenu.addMenuItem("Write SPICE Deck...", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { writeSpiceDeckCommand(); }});

		Menu ercSubMenu = Menu.createMenu("ERC", 'E');
		toolMenu.add(ercSubMenu);
		Menu networkSubMenu = Menu.createMenu("Network", 'N');
		toolMenu.add(networkSubMenu);
		networkSubMenu.addMenuItem("NCC test 1", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { nccTest1Command(); }});
		networkSubMenu.addMenuItem("NCC test 2", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { nccTest2Command(); }});
		networkSubMenu.addMenuItem("NCC test 3", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { nccTest3Command(); }});
		networkSubMenu.addMenuItem("NCC test 4", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { nccTest4Command(); }});
		Menu logEffortSubMenu = Menu.createMenu("Logical Effort", 'L');
        logEffortSubMenu.addMenuItem("Analyze Cell", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { analyzeCellCommand(); }});
		toolMenu.add(logEffortSubMenu);
        Menu netlisters = Menu.createMenu("Other Netlisters");
        netlisters.addMenuItem("Write IRSIM Netlist", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { irsimNetlistCommand(); }});
        toolMenu.add(netlisters);
		Menu routingSubMenu = Menu.createMenu("Routing", 'R');
		toolMenu.add(routingSubMenu);
		Menu generationSubMenu = Menu.createMenu("Generation", 'G');
		toolMenu.add(generationSubMenu);
        generationSubMenu.addMenuItem("Pad Frame Generator", null, 
            new ActionListener() { public void actionPerformed(ActionEvent e) { padFrameGeneratorCommand(); }});
		Menu compactionSubMenu = Menu.createMenu("Compaction", 'C');
		toolMenu.add(compactionSubMenu);

		// setup the Help menu
		Menu helpMenu = Menu.createMenu("Help", 'H');
		menuBar.add(helpMenu);
		helpMenu.addMenuItem("About Electric...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { aboutCommand(); } });
		helpMenu.addMenuItem("Check and Repair Libraries...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { checkAndRepairCommand(); } });

		// setup Russell's test menu
		Menu russMenu = Menu.createMenu("Russell", 'R');
		menuBar.add(russMenu);
		russMenu.addMenuItem("ivanFlat", new com.sun.electric.tool.generator.layout.IvanFlat());
		russMenu.addMenuItem("layout flat", new com.sun.electric.tool.generator.layout.LayFlat());
		russMenu.addMenuItem("gate regression", new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			    new com.sun.electric.tool.generator.layout.GateRegression();
		    }
		});
		russMenu.addMenuItem("create corrupt library", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new com.sun.electric.tool.generator.layout.BadLibrary();
			}
		});
		

		// setup Dima's test menu
		Menu dimaMenu = Menu.createMenu("Dima", 'D');
		menuBar.add(dimaMenu);
		dimaMenu.addMenuItem("redo Network Numbering", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { redoNetworkNumberingCommand(); } });

        // setup JonGainsley's test menu
        Menu jongMenu = Menu.createMenu("JonG", 'J');
		menuBar.add(jongMenu);
        jongMenu.addMenuItem("Describe Vars", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { listVarsOnObject(false); }});
        jongMenu.addMenuItem("Describe Proto Vars", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { listVarsOnObject(true); }});
        jongMenu.addMenuItem("Eval Vars", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { evalVarsOnObject(); }});
        jongMenu.addMenuItem("LE test1", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { LENetlister.test1(); }});
        jongMenu.addMenuItem("Open Purple Lib", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { openP4libCommand(); }});
            
		// return the menu bar
		return menuBar;
	}

	// ---------------------- THE FILE MENU -----------------

	/**
	 * This routine implements the command to read a library.
	 */
	public static void openLibraryCommand()
	{
		String fileName = DialogOpenFile.ELIB.chooseInputFile(null);
		if (fileName != null)
		{
			// start a job to do the input
			ReadBinaryLibrary job = new ReadBinaryLibrary(fileName);
		}
	}

	/**
	 * Class to read a library in a new thread.
	 */
	protected static class ReadBinaryLibrary extends Job
	{
		String fileName;

		protected ReadBinaryLibrary(String fileName)
		{
			super("Read Library", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.fileName = fileName;
			this.startJob();
		}

		public void doIt()
		{
			Library lib = Input.readLibrary(fileName, Input.ImportType.BINARY);
			Undo.noUndoAllowed();
			if (lib == null) return;
			Library.setCurrent(lib);
			Cell cell = lib.getCurCell();
			if (cell == null) System.out.println("No current cell in this library"); else
			{
				WindowFrame.createEditWindow(cell);
			}
		}
	}

	/**
	 * This routine implements the command to import a library (Readable Dump format).
	 */
	public static void importLibraryCommand()
	{
		String fileName = DialogOpenFile.TEXT.chooseInputFile(null);
		if (fileName != null)
		{
			// start a job to do the input
			ReadTextLibrary job = new ReadTextLibrary(fileName);
		}
	}

	/**
	 * Class to read a library in a new thread.
	 */
	protected static class ReadTextLibrary extends Job
	{
		String fileName;
		protected ReadTextLibrary(String fileName)
		{
			super("Read Text Library", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.fileName = fileName;
			this.startJob();
		}

		public void doIt()
		{
			Library lib = Input.readLibrary(fileName, Input.ImportType.TEXT);
			Undo.noUndoAllowed();
			if (lib == null) return;
			Library.setCurrent(lib);
			Cell cell = lib.getCurCell();
			if (cell == null) System.out.println("No current cell in this library"); else
			{
				WindowFrame.createEditWindow(cell);
			}
		}
	}

	/**
	 * This routine implements the command to save a library.
	 */
	public static void saveLibraryCommand()
	{
		Library lib = Library.getCurrent();
		String fileName;
		if (lib.isFromDisk())
		{
			fileName = lib.getLibFile();
		} else
		{
			fileName = DialogOpenFile.ELIB.chooseOutputFile(lib.getLibName()+".elib");
			if (fileName != null)
			{
				Library.Name n = Library.Name.newInstance(fileName);
				n.setExtension("elib");
				lib.setLibFile(n.makeName());
				lib.setLibName(n.getName());
			}
		}
		SaveLibrary job = new SaveLibrary(lib);
	}

	/**
	 * Class to save a library in a new thread.
	 */
	protected static class SaveLibrary extends Job
	{
		Library lib;

		protected SaveLibrary(Library lib)
		{
			super("Write Library", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.lib = lib;
			this.startJob();
		}

		public void doIt()
		{
			boolean error = Output.writeLibrary(lib, Output.ExportType.BINARY);
			if (error)
			{
				System.out.println("Error writing the library file");
			}
		}
	}

	/**
	 * This routine implements the command to save a library to a different file.
	 */
	public static void saveAsLibraryCommand()
	{
		Library lib = Library.getCurrent();
		lib.clearFromDisk();
		saveLibraryCommand();
	}

	/**
	 * This routine implements the command to quit Electric.
	 */
	public static void quitCommand()
	{
		System.exit(0);
	}

	// ---------------------- THE EDIT MENU -----------------

	/**
	 * This class implement the command to undo the last change.
	 */
	protected static class UndoCommand extends Job
	{
		protected UndoCommand()
		{
			super("Undo", User.tool, Job.Type.UNDO, Undo.upCell(false), null, Job.Priority.USER);
			this.startJob();
		}

		public void doIt()
		{
			if (!Undo.undoABatch())
				System.out.println("Undo failed!");
		}
	}

	/**
	 * This class implement the command to undo the last change.
	 */
	protected static class RedoCommand extends Job
	{
		protected RedoCommand()
		{
			super("Redo", User.tool, Job.Type.UNDO, Undo.upCell(true), null, Job.Priority.USER);
			this.startJob();
		}

		public void doIt()
		{
			if (!Undo.redoABatch())
				System.out.println("Redo failed!");
		}
	}

	/**
	 * This routine implements the command to show the undo history.
	 */
	public static void showUndoListCommand()
	{
		Undo.showHistoryList();
	}

	/**
	 * This routine implements the command to cut circuitry or text.
	 */
	public static void cutCommand()
	{
		Clipboard.cut();
	}

	/**
	 * This routine implements the command to copy circuitry or text.
	 */
	public static void copyCommand()
	{
		Clipboard.copy();
	}

	/**
	 * This routine implements the command to paste circuitry or text.
	 */
	public static void pasteCommand()
	{
		Clipboard.paste();
	}

	public static void arcRigidCommand()
	{
		int numSet = 0;
		for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
		{
			Highlight h = (Highlight)it.next();
			if (h.getType() != Highlight.Type.GEOM) continue;
			Geometric geom = h.getGeom();
			if (geom instanceof ArcInst)
			{
				ArcInst ai = (ArcInst)geom;
				ai.setRigid();
				numSet++;
			}
		}
		if (numSet == 0) System.out.println("No arcs made Rigid"); else
			System.out.println("Made " + numSet + " arcs Rigid");
		EditWindow.redrawAll();
	}

	public static void arcNotRigidCommand()
	{
		int numSet = 0;
		for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
		{
			Highlight h = (Highlight)it.next();
			if (h.getType() != Highlight.Type.GEOM) continue;
			Geometric geom = h.getGeom();
			if (geom instanceof ArcInst)
			{
				ArcInst ai = (ArcInst)geom;
				ai.clearRigid();
				numSet++;
			}
		}
		if (numSet == 0) System.out.println("No arcs made Non-Rigid"); else
			System.out.println("Made " + numSet + " arcs Non-Rigid");
		EditWindow.redrawAll();
	}

	public static void arcFixedAngleCommand()
	{
		int numSet = 0;
		for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
		{
			Highlight h = (Highlight)it.next();
			if (h.getType() != Highlight.Type.GEOM) continue;
			Geometric geom = h.getGeom();
			if (geom instanceof ArcInst)
			{
				ArcInst ai = (ArcInst)geom;
				ai.setFixedAngle();
				numSet++;
			}
		}
		if (numSet == 0) System.out.println("No arcs made Fixed-Angle"); else
			System.out.println("Made " + numSet + " arcs Fixed-Angle");
		EditWindow.redrawAll();
	}

	public static void arcNotFixedAngleCommand()
	{
		int numSet = 0;
		for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
		{
			Highlight h = (Highlight)it.next();
			if (h.getType() != Highlight.Type.GEOM) continue;
			Geometric geom = h.getGeom();
			if (geom instanceof ArcInst)
			{
				ArcInst ai = (ArcInst)geom;
				ai.clearFixedAngle();
				numSet++;
			}
		}
		if (numSet == 0) System.out.println("No arcs made Not-Fixed-Angle"); else
			System.out.println("Made " + numSet + " arcs Not-Fixed-Angle");
		EditWindow.redrawAll();
	}

	/**
	 * This routine implements the command to show I/O Options.
	 */
	public static void ioOptionsCommand()
	{
		JFrame jf = TopLevel.getCurrentJFrame();
 		IOOptions dialog = new IOOptions(jf, true);
		dialog.show();
	}

	/**
	 * This routine implements the command to show Edit Options.
	 */
	public static void editOptionsCommand()
	{
		JFrame jf = TopLevel.getCurrentJFrame();
 		EditOptions dialog = new EditOptions(jf, true);
		dialog.show();
	}

	/**
	 * This routine implements the command to show Tool Options.
	 */
	public static void toolOptionsCommand()
	{
		JFrame jf = TopLevel.getCurrentJFrame();
 		ToolOptions dialog = new ToolOptions(jf, true);
		dialog.show();
	}

	public static void getInfoCommand()
	{
		if (Highlight.getNumHighlights() == 0)
		{
			// information about the cell
			Cell c = Library.getCurrent().getCurCell();
			c.getInfo();
		} else
		{
			// information about the selected items
			for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
			{
				Highlight h = (Highlight)it.next();
				if (h.getType() == Highlight.Type.GEOM)
				{
					Geometric geom = h.getGeom();
					geom.getInfo();
				} else if (h.getType() == Highlight.Type.TEXT)
				{
					if (h.getVar() != null)
					{
						String trueName = h.getVar().getReadableName();
						if (h.getGeom() != null)
						{
							if (h.getPort() != null)
							{
								System.out.println("TEXT: " + trueName + " on export '" + h.getPort().getProtoName() + "'");
							} else
							{
								if (h.getGeom() instanceof NodeInst)
								{
									NodeInst ni = (NodeInst)h.getGeom();
									if (ni.getProto() == Generic.tech.invisiblePinNode)
									{
										String varName = h.getVar().getKey().getName();
										if (varName.equals("ART_message"))
										{
											System.out.println("TEXT: Nonlayout text");
											continue;
										}
										if (varName.equals("VERILOG_code"))
										{
											System.out.println("TEXT: Verilog Code");
											continue;
										}
										if (varName.equals("VERILOG_declaration"))
										{
											System.out.println("TEXT: Verilog declaration");
											continue;
										}
										if (varName.equals("SIM_spice_card"))
										{
											System.out.println("TEXT: SPICE card");
											continue;
										}
									}
								}
								System.out.println("TEXT: " + trueName + " on geometry " + h.getGeom().describe());
							}
						} else
						{
							System.out.println("TEXT: " + trueName + " on cell " + h.getCell().describe());
						}
					} else
					{
						if (h.getPort() != null)
						{
							System.out.println("TEXT: Export '" + h.getPort().getProtoName() + "'");
						} else
						{
							if (h.getGeom() != null)
							{
								System.out.println("TEXT: Cell instance name " + h.getGeom().describe());
							} else
							{
								System.out.println("TEXT: UNKNOWN");
							}
						}
					}
				} else if (h.getType() == Highlight.Type.BBOX)
				{
					System.out.println("*** Area selected");
				} else if (h.getType() == Highlight.Type.LINE)
				{
					System.out.println("*** Line selected");
				}
			}
		}
	}

	// ---------------------- THE CELL MENU -----------------

    public static void editCellCommand()
	{
		JFrame jf = TopLevel.getCurrentJFrame();
 		EditCell dialog = new EditCell(jf, true);
		dialog.show();
    }

	public static void downHierCommand() {
        EditWindow curEdit = TopLevel.getCurrentEditWindow();
        curEdit.downHierarchy();
    }
    
    public static void upHierCommand() {
        EditWindow curEdit = TopLevel.getCurrentEditWindow();
        curEdit.upHierarchy();
    }

	public static void showCellGroupsCommand()
	{
		// list things by cell group
		FlagSet cellFlag = NodeProto.getFlagSet(1);
		Library curLib = Library.getCurrent();
		for(Iterator it = curLib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			cell.clearBit(cellFlag);
		}
		for(Iterator it = curLib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			if (cell.isBit(cellFlag)) continue;

			// untouched: show whole cell group
			System.out.println("**** Cell group:");
			for(Iterator git = cell.getCellGroup().getCells(); git.hasNext(); )
			{
				Cell cellInGroup = (Cell)git.next();
				System.out.println("    " + cellInGroup.describe());
				cellInGroup.setBit(cellFlag);
			}
		}
		cellFlag.freeFlagSet();
	}

	/**
	 * This routine implements the command to do cross-library copies.
	 */
	public static void crossLibraryCopyCommand()
	{
		JFrame jf = TopLevel.getCurrentJFrame();
 		CrossLibCopy dialog = new CrossLibCopy(jf, true);
		dialog.show();
	}

	// ---------------------- THE EXPORT MENU -----------------

	/**
	 * This routine implements the command to create a new Export.
	 */
	public static void newExportCommand()
	{
		JFrame jf = TopLevel.getCurrentJFrame();
 		NewExport dialog = new NewExport(jf, true);
		dialog.show();
	}

	// ---------------------- THE WINDOW MENU -----------------

	public static void fullDisplayCommand()
	{
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			for(Iterator cit = lib.getCells(); cit.hasNext(); )
			{
				Cell cell = (Cell)cit.next();
				for(Iterator nit = cell.getNodes(); nit.hasNext(); )
				{
					NodeInst ni = (NodeInst)nit.next();
					ni.setExpanded();
				}
			}
		}

		// get the current window
        EditWindow curEdit = TopLevel.getCurrentEditWindow();
		if (curEdit != null)
		{
			curEdit.setTimeTracking(true);
			curEdit.redraw();
		}
	}

	/**
	 * This routine implements the command to toggle the display of the grid.
	 */
	public static void toggleGridCommand()
	{
		EditWindow wnd = TopLevel.getCurrentEditWindow();
		if (wnd != null)
		{
			wnd.setGrid(!wnd.getGrid());
		}
	}

	// ---------------------- THE TOOLS MENU -----------------

	public static void writeSpiceDeckCommand()
	{
		Spice.writeSpiceDeck();
	}

    // Logical Effort Tool
    public static void analyzeCellCommand()
    {
        EditWindow curEdit = TopLevel.getCurrentEditWindow();
        if (curEdit == null) {
            System.out.println("Please select valid window first");
            return;
        }
        LETool letool = LETool.getLETool();
        if (letool == null) {
            System.out.println("Logical Effort tool not found");
            return;
        }
        letool.analyzeCell(curEdit.getCell(), curEdit.getVarContext(), curEdit);
    }

	// NCC Tool
	public static void nccTest1Command()
	{
//		NetFactory nf = new NetFactory();
//		nf.testOne();
	}

	public static void nccTest2Command()
	{
//		NetFactory nf = new NetFactory();
//		nf.testTwo();
	}

	public static void nccTest3Command()
	{
//		NetFactory nf = new NetFactory();
//		nf.testThree();
	}

	public static void nccTest4Command()
	{
//		NetFactory nf = new NetFactory();
//		nf.testFour();
	}
    
    public static void irsimNetlistCommand()
    {
        EditWindow curEdit = TopLevel.getCurrentEditWindow();
        if (curEdit == null) {
            System.out.println("Please select valid window first");
            return;
        }
        IRSIMTool.tool.netlistCell(curEdit.getCell(), curEdit.getVarContext(), curEdit);
    }        

	public static void padFrameGeneratorCommand()
	{
		PadGenerator gen = new PadGenerator();
		gen.ArrayFromFile();
	}
    
    // ---------------------- THE HELP MENU -----------------

	public static void aboutCommand()
	{
		JFrame jf = TopLevel.getCurrentJFrame();
		About dialog = new About(jf, true);
		dialog.show();
	}

	public static void checkAndRepairCommand()
	{
		int errorCount = 0;
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			errorCount += lib.checkAndRepair();
		}
		if (errorCount > 0) System.out.println("Found " + errorCount + " errors"); else
			System.out.println("No errors found");
	}

//	public static void showRTreeCommand()
//	{
//		Library curLib = Library.getCurrent();
//		Cell curCell = curLib.getCurCell();
//		System.out.println("Current cell is " + curCell.describe());
//		if (curCell == null) return;
//		curCell.getRTree().printRTree(0);
//	}

	// ---------------------- THE DIMA MENU -----------------

	public static void redoNetworkNumberingCommand()
	{
		long startTime = System.currentTimeMillis();
		System.out.println("**** Renumber networks of cells");
		if (false)
		{
			int ncell = 0;
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = (Library)it.next();
				for(Iterator cit = lib.getCells(); cit.hasNext(); )
				{
					Cell cell = (Cell)cit.next();
					ncell++;
					cell.rebuildNetworks(null, false);
				}
			}
		} else
		{
			ArrayList connectedPorts = new ArrayList();
			/*
			ArrayList pair = new ArrayList();
			NodeProto diode = NodeProto.findNodeProto("schematic:Diode");
			pair.add(diode.findPortProto("a"));
			pair.add(diode.findPortProto("b"));
			connectedPorts.add(pair);
			*/
			Cell.rebuildAllNetworks(connectedPorts);
		}
		long endTime = System.currentTimeMillis();
		float finalTime = (endTime - startTime) / 1000F;
		System.out.println("**** Renumber networks took " + finalTime + " seconds");
	}
    
    // ---------------------- THE JON GAINSLEY MENU -----------------
    
    public static void listVarsOnObject(boolean useproto) {
        if (Highlight.getNumHighlights() == 0) {
            System.out.println("Nothing highlighted");
            return;
        }
        for (Iterator it = Highlight.getHighlights(); it.hasNext();) {
            Highlight h = (Highlight)it.next();
			if (h.getType() != Highlight.Type.GEOM) continue;
            Geometric geom = (Geometric)h.getGeom();
            if (geom instanceof NodeInst) {
                NodeInst ni = (NodeInst)geom;
                if (useproto) {
                    System.out.println("using prototype");
                    ((ElectricObject)ni.getProto()).getInfo();
                } else {
                    ni.getInfo();
                }
            }
        }
    }

    public static void evalVarsOnObject() {
        EditWindow curEdit = TopLevel.getCurrentEditWindow();
        if (Highlight.getNumHighlights() == 0) {
            System.out.println("Nothing highlighted");
            return;
        }
        for (Iterator it = Highlight.getHighlights(); it.hasNext();) {
            Highlight h = (Highlight)it.next();
			if (h.getType() != Highlight.Type.GEOM) continue;
            ElectricObject eobj = (ElectricObject)h.getGeom();
            Iterator itVar = eobj.getVariables();
            while(itVar.hasNext()) {
                Variable var = (Variable)itVar.next();
                Object obj = curEdit.getVarContext().evalVar(var);
                System.out.print(var.getKey().getName() + ": ");
                System.out.println(obj);
            }
        }
    }

    public static void openP4libCommand() {
		ReadBinaryLibrary job = new ReadBinaryLibrary("/export/gainsley/soesrc_java/test/purpleFour.elib");
//        OpenBinLibraryThread oThread = new OpenBinLibraryThread("/export/gainsley/soesrc_java/test/purpleFour.elib");
//        oThread.start();
    }

}
