/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellLists.java
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
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.geom.Rectangle2D;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;
import java.util.Comparator;
import java.util.Collections;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JFrame;


/**
 * Class to handle the "New Cell" dialog.
 */
public class CellLists extends javax.swing.JDialog
{
	FlagSet nodeFlagBit;
	FlagSet portFlagBit;
	Cell curCell;
	private static int whichSwitch = 0;
	private static boolean onlyViewSwitch = false;
	private static View viewSwitch = View.SCHEMATIC;
	private static boolean alsoIconSwitch = false;
	private static boolean excOldVersSwitch = false;
	private static boolean excNewVersSwitch = false;
	private static int orderingSwitch = 0;
	private static int destinationSwitch = 0;

	/**
	 * This method implements the command to create general Cell lists.
	 */
	public static void generalCellListsCommand()
	{
		JFrame jf = TopLevel.getCurrentJFrame();
 		CellLists dialog = new CellLists(jf, true);
		dialog.show();
	}

	/** Creates new form New Cell */
	private CellLists(JFrame parent, boolean modal)
	{
		super(parent, modal);
		setLocation(100, 50);
		initComponents();

		// make a popup of views
		for(Iterator it = View.getViews(); it.hasNext(); )
		{
			View v = (View)it.next();
			views.addItem(v.getFullName());
		}

		curCell = Library.getCurrent().getCurCell();
		onlyCellsUnderCurrent.setEnabled(curCell != null);

		switch (whichSwitch)
		{
			case 0: allCells.setSelected(true);                   break;
			case 1: onlyCellsUsedElsewhere.setSelected(true);     break;
			case 2: onlyCellsNotUsedElsewhere.setSelected(true);  break;
			case 3: onlyCellsUnderCurrent.setSelected(true);      break;
			case 4: onlyPlaceholderCells.setSelected(true);       break;
		}
		onlyThisView.setSelected(onlyViewSwitch);
		views.setSelectedItem(viewSwitch.getFullName());
		views.setEnabled(onlyViewSwitch);
		alsoIconViews.setSelected(alsoIconSwitch);
		excludeOlderVersions.setSelected(excOldVersSwitch);
		excludeNewestVersions.setSelected(excNewVersSwitch);
		switch (orderingSwitch)
		{
			case 0: orderByName.setSelected(true);                break;
			case 1: orderByDate.setSelected(true);                break;
			case 2: orderByStructure.setSelected(true);           break;
		}
		switch (destinationSwitch)
		{
			case 0: displayInMessages.setSelected(true);          break;
			case 1: saveToDisk.setSelected(true);                 break;
		}
	}

	private static String makeCellLine(Cell cell, int maxlen)
	{
		String line = cell.noLibDescribe();
		if (maxlen < 0) line += "\t"; else
		{
			for(int i=line.length(); i<maxlen; i++) line += " ";
		}

		/* add the version number */
		String versionString = TextUtils.toBlankPaddedString(cell.getVersion(), 5);
		line += versionString;
		if (maxlen < 0) line += "\t"; else line += "   ";

		/* add the creation date */
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date creationDate = cell.getCreationDate();
		if (creationDate == null)
		{
			if (maxlen < 0) line += "UNRECORDED"; else
				line += "     UNRECORDED     ";
		} else
		{
			line += sdf.format(creationDate);
		}
		if (maxlen < 0) line += "\t"; else line += "   ";

		/* add the revision date */
		Date revisionDate = cell.getRevisionDate();
		if (revisionDate == null)
		{
			if (maxlen < 0) line += "UNRECORDED"; else
				line += "     UNRECORDED     ";
		} else
		{
			line += sdf.format(creationDate);
		}
		if (maxlen < 0) line += "\t"; else line += "   ";

		/* add the size */
		if (cell.getView().isTextView())
		{
			int len = 0;
			Variable var = cell.getVar("FACET_message");
			if (var != null) len = var.getLength();
			if (maxlen < 0) line += len + " lines"; else
			{
				line += TextUtils.toBlankPaddedString(len, 8) + " lines   ";
			}
		} else
		{
			String width = Double.toString(cell.getBounds().getWidth());
			if (maxlen >= 0)
			{
				while (width.length() < 8) width = " " + width;
			}
			String height = Double.toString(cell.getBounds().getHeight());
			if (maxlen >= 0)
			{
				while (height.length() < 8) height = height + " ";
			}
			line += width + "x" + height;
		}
		if (maxlen < 0) line += "\t";

		/* count the number of instances */
		int total = 0;
		for(Iterator it = cell.getInstancesOf(); it.hasNext(); )
		{
			total++;
			it.next();
		}
		if (maxlen < 0) line += total; else
		{
			line += TextUtils.toBlankPaddedString(total, 4);
		}
		if (maxlen < 0) line += "\t"; else line += "   ";

		/* show other factors about the cell */
		if (cell.isLockedPrim()) line += "L"; else line += " ";
		if (maxlen < 0) line += "\t"; else line += " ";
		if (cell.isInstancesLocked()) line += "I"; else line += " ";
		if (maxlen < 0) line += "\t"; else line += " ";
		if (cell.isInCellLibrary()) line += "C"; else line += " ";
		if (maxlen < 0) line += "\t"; else line += " ";

		boolean goodDRC = false;
		Variable var = cell.getVar("DRC_last_good_drc", Integer.class);
		if (var != null)
		{
			long lastGoodDateLong = ((Integer)var.getObject()).intValue();
			Date lastGoodDate = new Date(lastGoodDateLong);
			if (cell.getRevisionDate().before(lastGoodDate)) goodDRC = true;
		}
		if (goodDRC) line += "D"; else line += " ";
		if (maxlen < 0) line += "\t"; else line += " ";

//		if (net_ncchasmatch(cell) != 0) addstringtoinfstr(infstr, x_("N")); else
//			addstringtoinfstr(infstr, x_(" "));
		return line;
	}

	/*
	 * Method to recursively walk the hierarchy from "np", marking all cells below it.
	 */
	private void recursiveMark(Cell cell)
	{
		if (cell.isBit(nodeFlagBit)) return;
		cell.setBit(nodeFlagBit);
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			NodeProto np = ni.getProto();
			if (!(np instanceof Cell)) continue;
			Cell subCell = (Cell)np;
			recursiveMark(subCell);
			Cell contentsCell = subCell.contentsView();
			if (contentsCell != null) recursiveMark(contentsCell);
		}
	}

	static class SortByCellName implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			Cell c1 = (Cell)o1;
			Cell c2 = (Cell)o2;
			String s1 = c1.noLibDescribe();
			String s2 = c2.noLibDescribe();
			return s1.compareToIgnoreCase(s2);
		}
	}

	static class SortByCellDate implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			Cell c1 = (Cell)o1;
			Cell c2 = (Cell)o2;
			Date r1 = c1.getRevisionDate();
			Date r2 = c2.getRevisionDate();
			return r1.compareTo(r2);
		}
	}

	static class SortByCellStructure implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			Cell c1 = (Cell)o1;
			Cell c2 = (Cell)o2;

			// first sort by cell size
			Rectangle2D b1 = c1.getBounds();
			Rectangle2D b2 = c2.getBounds();
			int xs1 = (int)b1.getWidth();
			int xs2 = (int)b2.getWidth();
			if (xs1 != xs2) return(xs1-xs2);
			int ys1 = (int)b1.getHeight();
			int ys2 = (int)b2.getHeight();
			if (ys1 != ys2) return(ys1-ys2);

			// now sort by number of exports
			int pc1 = c1.getNumPorts();
			int pc2 = c2.getNumPorts();
			pc1 = 0;
			if (pc1 != pc2) return(pc1-pc2);

			// now match the exports
//			for(Iterator it = c1.getPorts(); it.hasNext(); )
//			{
//				PortProto pp1 = (PortProto)it.next();
//				pp1.clearBit(portFlagBit);
//			}
//			for(Iterator it = c2.getPorts(); it.hasNext(); )
//			{
//				PortProto pp2 = (PortProto)it.next();
//
//				// locate center of this export
//				ni = &dummyni;
//				initdummynode(ni);
//				ni->proto = f2;
//				ni->lowx = -xs1/2;   ni->highx = ni->lowx + xs1;
//				ni->lowy = -ys1/2;   ni->highy = ni->lowy + ys1;
//				portposition(ni, pp2, &x2, &y2);
//
//				ni->proto = f1;
//				for(pp1 = f1->firstportproto; pp1 != NOPORTPROTO; pp1 = pp1->nextportproto)
//				{
//					portposition(ni, pp1, &x1, &y1);
//					if (x1 == x2 && y1 == y2) break;
//				}
//				if (pp1 == NOPORTPROTO) return(f1-f2);
//				pp1->temp1 = 1;
//			}
//			for(pp1 = f1->firstportproto; pp1 != NOPORTPROTO; pp1 = pp1->nextportproto)
//				if (pp1->temp1 == 0) return(f1-f2);
			return(0);
		}
	}

	/**
	 * Class for counting instances.
	 * It is basically a modifiable Integer.
	 */
	static class InstanceCount
	{
		private int count;

		InstanceCount() { count = 0; }
		public void increment() { count++; }
		public int getCount() { return count; }
	}

	/**
	 * This method implements the command to list (recursively) the nodes in this Cell.
	 */
	public static void listNodesInCellCommand()
	{
		Cell curCell = Library.needCurCell();
		if (curCell == null) return;

		HashMap nodeCount = new HashMap();

		// first zero the count of each nodeproto
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			for(Iterator nIt = tech.getNodes(); nIt.hasNext(); )
			{
				NodeProto np = (NodeProto)nIt.next();
				nodeCount.put(np, new InstanceCount());
			}
		}
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			for(Iterator nIt = lib.getCells(); nIt.hasNext(); )
			{
				NodeProto np = (NodeProto)nIt.next();
				nodeCount.put(np, new InstanceCount());
			}
		}

		// now look at every object recursively in this cell
		addObjects(curCell, nodeCount);

		// print the totals
		System.out.println("Contents of cell " + curCell.describe() + ":");
		Technology printtech = null;
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology curtech = (Technology)it.next();
			for(Iterator nIt = curtech.getNodes(); nIt.hasNext(); )
			{
				NodeProto np = (NodeProto)nIt.next();
				InstanceCount count = (InstanceCount)nodeCount.get(np);
				if (count.getCount() == 0) continue;
				if (curtech != printtech)
				{
					System.out.println(curtech.getTechName() + " technology:");
					printtech = curtech;
				}
				System.out.println(TextUtils.toBlankPaddedString(count.getCount(), 6) + " " + np.describe() + " nodes");
			}
		}
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			List cellList = lib.getCellsSortedByName();
			Library printlib = null;
			for(Iterator cIt = cellList.iterator(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				InstanceCount count = (InstanceCount)nodeCount.get(cell);
				if (count.getCount() == 0) continue;
				if (lib != printlib)
				{
					System.out.println(lib.getLibName() + " library:");
					printlib = lib;
				}
				System.out.println(TextUtils.toBlankPaddedString(count.getCount(), 6) + " " + cell.describe() + " nodes");
			}
		}
	}

	/**
	 * Method to recursively examine cell "np" and update the number of
	 * instantiated primitive nodeprotos in the "temp1" field of the nodeprotos.
	 */
	private static void addObjects(Cell cell, HashMap nodeCount)
	{
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			NodeProto np = ni.getProto();
			InstanceCount count = (InstanceCount)nodeCount.get(np);
			if (count != null) count.increment();

			if (!(np instanceof Cell)) continue;
			Cell subCell = (Cell)np;

			/* ignore recursive references (showing icon in contents) */
			if (ni.isIconOfParent()) continue;
			Cell cnp = subCell.contentsView();
			if (cnp == null) cnp = subCell;
			addObjects(cnp, nodeCount);
		}
	}

	/**
	 * This method implements the command to list instances in this Cell.
	 */
	public static void listCellInstancesCommand()
	{
		Cell curCell = Library.needCurCell();
		if (curCell == null) return;

		HashMap nodeCount = new HashMap();

		// set counters on every cell in every library
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				nodeCount.put(cell, new InstanceCount());
			}
		}

		// count the number of instances in this cell
		for(Iterator it = curCell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			NodeProto np = ni.getProto();
			if (!(np instanceof Cell)) continue;
			InstanceCount count = (InstanceCount)nodeCount.get(np);
			if (count != null) count.increment();
		}

		// show the results
		boolean first = true;
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				InstanceCount count = (InstanceCount)nodeCount.get(cell);
				if (count == null || count.getCount() == 0) continue;
				if (first)
					System.out.println("Cell instances appearing in " + curCell.describe());
				first = false;
				String line = "   " + count.getCount() + " instances of " + cell.describe() + " at";
				for(Iterator nIt = curCell.getNodes(); nIt.hasNext(); )
				{
					NodeInst ni = (NodeInst)nIt.next();
					if (ni.getProto() != cell) continue;
					line += " (" + ni.getGrabCenterX() + "," + ni.getGrabCenterY() + ")";
				}
				System.out.println(line);
			}
		}
		if (first)
			System.out.println("There are no cell instances in " + curCell.describe());
	}

	/**
	 * This method implements the command to list the usage of the current Cell.
	 */
	public static void listCellUsageCommand()
	{
		Cell curCell = Library.needCurCell();
		if (curCell == null) return;

		HashMap nodeCount = new HashMap();

		// stop now if this cell has no instances
		if (!curCell.getInstancesOf().hasNext())
		{
			System.out.println("Cell " + curCell.describe() + " is not used anywhere");
			return;
		}

		// set counters on every cell in every library
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				nodeCount.put(cell, new InstanceCount());
			}
		}

		// count the number of instances in this cell
		for(Iterator nIt = curCell.getInstancesOf(); nIt.hasNext(); )
		{
			NodeInst ni = (NodeInst)nIt.next();
			Cell cell = ni.getParent();
			InstanceCount count = (InstanceCount)nodeCount.get(cell);
			if (count != null) count.increment();
		}

		// show the results
		System.out.println("Cell " + curCell.describe() + " is used in these locations:");
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				InstanceCount count = (InstanceCount)nodeCount.get(cell);
				if (count == null || count.getCount() == 0) continue;
				System.out.println("  " + count.getCount() + " instances in cell " + cell.describe());
			}
		}
	}

	/**
	 * This method implements the command to describe the current Cell.
	 */
	public static void describeThisCellCommand()
	{
		Cell curCell = Library.needCurCell();
		if (curCell == null) return;
		int maxLen = curCell.describe().length();
		printHeaderLine(maxLen);
		String line = makeCellLine(curCell, maxLen);
		System.out.println(line);
	}

	private static void printHeaderLine(int maxLen)
	{
		String header = "Cell";
		for(int i=4; i<maxLen; i++) header += "-";
		header += "Version-----Creation date";
		header += "---------Revision Date------------Size-------Usage-L-I-C-D-N";
		System.out.println(header);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        whichCells = new javax.swing.ButtonGroup();
        ordering = new javax.swing.ButtonGroup();
        destination = new javax.swing.ButtonGroup();
        cancel = new javax.swing.JButton();
        ok = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        allCells = new javax.swing.JRadioButton();
        onlyCellsUsedElsewhere = new javax.swing.JRadioButton();
        onlyCellsNotUsedElsewhere = new javax.swing.JRadioButton();
        onlyCellsUnderCurrent = new javax.swing.JRadioButton();
        onlyPlaceholderCells = new javax.swing.JRadioButton();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel2 = new javax.swing.JLabel();
        onlyThisView = new javax.swing.JCheckBox();
        views = new javax.swing.JComboBox();
        alsoIconViews = new javax.swing.JCheckBox();
        jSeparator2 = new javax.swing.JSeparator();
        jLabel3 = new javax.swing.JLabel();
        excludeOlderVersions = new javax.swing.JCheckBox();
        excludeNewestVersions = new javax.swing.JCheckBox();
        jSeparator3 = new javax.swing.JSeparator();
        jLabel4 = new javax.swing.JLabel();
        orderByName = new javax.swing.JRadioButton();
        orderByDate = new javax.swing.JRadioButton();
        orderByStructure = new javax.swing.JRadioButton();
        jSeparator4 = new javax.swing.JSeparator();
        jLabel5 = new javax.swing.JLabel();
        displayInMessages = new javax.swing.JRadioButton();
        saveToDisk = new javax.swing.JRadioButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("New Cell");
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
        gridBagConstraints.gridy = 24;
        gridBagConstraints.weightx = 0.5;
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
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 24;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);

        jLabel1.setText("Which cells:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel1, gridBagConstraints);

        allCells.setText("All cells");
        whichCells.add(allCells);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 0, 4);
        getContentPane().add(allCells, gridBagConstraints);

        onlyCellsUsedElsewhere.setText("Only those used elsewhere");
        whichCells.add(onlyCellsUsedElsewhere);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 4);
        getContentPane().add(onlyCellsUsedElsewhere, gridBagConstraints);

        onlyCellsNotUsedElsewhere.setText("Only those not used elsewhere");
        whichCells.add(onlyCellsNotUsedElsewhere);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 4);
        getContentPane().add(onlyCellsNotUsedElsewhere, gridBagConstraints);

        onlyCellsUnderCurrent.setText("Only those under current cell");
        whichCells.add(onlyCellsUnderCurrent);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 4);
        getContentPane().add(onlyCellsUnderCurrent, gridBagConstraints);

        onlyPlaceholderCells.setText("Only placeholder cells");
        whichCells.add(onlyPlaceholderCells);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 4, 4);
        getContentPane().add(onlyPlaceholderCells, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(jSeparator1, gridBagConstraints);

        jLabel2.setText("View filter:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel2, gridBagConstraints);

        onlyThisView.setText("Show only this view:");
        onlyThisView.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                onlyThisViewActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 0, 4);
        getContentPane().add(onlyThisView, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 40, 4, 4);
        getContentPane().add(views, gridBagConstraints);

        alsoIconViews.setText("Also include icon views");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        getContentPane().add(alsoIconViews, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(jSeparator2, gridBagConstraints);

        jLabel3.setText("Version filter:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel3, gridBagConstraints);

        excludeOlderVersions.setText("Exclude older versions");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 0, 4);
        getContentPane().add(excludeOlderVersions, gridBagConstraints);

        excludeNewestVersions.setText("Exclude newest versions");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 4, 4);
        getContentPane().add(excludeNewestVersions, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(jSeparator3, gridBagConstraints);

        jLabel4.setText("Display ordering:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel4, gridBagConstraints);

        orderByName.setText("Order by name");
        ordering.add(orderByName);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 0, 4);
        getContentPane().add(orderByName, gridBagConstraints);

        orderByDate.setText("Order by modification date");
        ordering.add(orderByDate);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 4);
        getContentPane().add(orderByDate, gridBagConstraints);

        orderByStructure.setText("Order by skeletal structure");
        ordering.add(orderByStructure);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 19;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 4, 4);
        getContentPane().add(orderByStructure, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(jSeparator4, gridBagConstraints);

        jLabel5.setText("Destination:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 21;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jLabel5, gridBagConstraints);

        displayInMessages.setText("Display in messages window");
        destination.add(displayInMessages);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 0, 4);
        getContentPane().add(displayInMessages, gridBagConstraints);

        saveToDisk.setText("Save to disk");
        destination.add(saveToDisk);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 23;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 4, 4);
        getContentPane().add(saveToDisk, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void onlyThisViewActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_onlyThisViewActionPerformed
	{//GEN-HEADEREND:event_onlyThisViewActionPerformed
		boolean selected = onlyThisView.isSelected();
		views.setEnabled(selected);
	}//GEN-LAST:event_onlyThisViewActionPerformed

	private void cancel(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancel
	{//GEN-HEADEREND:event_cancel
		closeDialog(null);
	}//GEN-LAST:event_cancel

	private void ok(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ok
	{//GEN-HEADEREND:event_ok
		// get cell and port markers
		nodeFlagBit = NodeProto.getFlagSet(1);
		portFlagBit = PortProto.getFlagSet(1);

		// mark cells to be shown
		if (allCells.isSelected())
		{
			// mark all cells for display
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = (Library)it.next();
				for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = (Cell)cIt.next();
					cell.setBit(nodeFlagBit);
				}
			}
		} else
		{
			// mark no cells for display, filter according to request
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = (Library)it.next();
				for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = (Cell)cIt.next();
					cell.clearBit(nodeFlagBit);
				}
			}
			if (onlyCellsUnderCurrent.isSelected())
			{
				// mark those that are under this
				recursiveMark(curCell);
			} else if (onlyCellsUsedElsewhere.isSelected())
			{
				// mark those that are in use
				for(Iterator it = Library.getLibraries(); it.hasNext(); )
				{
					Library lib = (Library)it.next();
					for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
					{
						Cell cell = (Cell)cIt.next();
						Cell iconCell = cell.iconView();
						if (iconCell == null) iconCell = cell;
						if (cell.getInstancesOf().hasNext() || iconCell.getInstancesOf().hasNext())
							cell.setBit(nodeFlagBit);
					}
				}
			} else if (onlyCellsNotUsedElsewhere.isSelected())
			{
				// mark those that are not in use
				for(Iterator it = Library.getLibraries(); it.hasNext(); )
				{
					Library lib = (Library)it.next();
					for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
					{
						Cell cell = (Cell)cIt.next();
						Cell iconCell = cell.iconView();
						if (iconCell != null)
						{
							// has icon: acceptable if the only instances are examples
							if (cell.getInstancesOf().hasNext()) continue;
							boolean found = false;
							for(Iterator nIt = iconCell.getInstancesOf(); nIt.hasNext(); )
							{
								NodeInst ni = (NodeInst)nIt.next();
								if (ni.isIconOfParent()) { found = true;   break; }
							}
							if (found) continue;
						} else
						{
							// no icon: reject if this has instances
							if (cell.getView() == View.ICON)
							{
								// this is an icon: reject if instances are not examples
								boolean found = false;
								for(Iterator nIt = cell.getInstancesOf(); nIt.hasNext(); )
								{
									NodeInst ni = (NodeInst)nIt.next();
									if (ni.isIconOfParent()) { found = true;   break; }
								}
								if (found) continue;
							} else
							{
								if (cell.getInstancesOf().hasNext()) continue;
							}
						}
						cell.setBit(nodeFlagBit);
					}
				}
			} else
			{
				// mark placeholder cells
				for(Iterator it = Library.getLibraries(); it.hasNext(); )
				{
					Library lib = (Library)it.next();
					for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
					{
						Cell cell = (Cell)cIt.next();
						Variable var = cell.getVar("IO_true_library");
						if (var != null) cell.setBit(nodeFlagBit);
					}
				}
			}
		}

		// filter views
		if (onlyThisView.isSelected())
		{
			String viewName = (String)views.getSelectedItem();
			View v = View.findView(viewName);
			if (v != null)
			{
				for(Iterator it = Library.getLibraries(); it.hasNext(); )
				{
					Library lib = (Library)it.next();
					for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
					{
						Cell cell = (Cell)cIt.next();
						if (cell.getView() != v)
						{
							if (cell.getView() == View.ICON)
							{
								if (alsoIconViews.isSelected()) continue;
							}
							cell.clearBit(nodeFlagBit);
						}
					}
				}
			}
		}

		// filter versions
		if (excludeOlderVersions.isSelected())
		{
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = (Library)it.next();
				for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = (Cell)cIt.next();
					if (cell.getNewestVersion() != cell) cell.clearBit(nodeFlagBit);
				}
			}
		}
		if (excludeNewestVersions.isSelected())
		{
			for(Iterator it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = (Library)it.next();
				for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = (Cell)cIt.next();
					if (cell.getNewestVersion() == cell) cell.clearBit(nodeFlagBit);
				}
			}
		}

		// now make a list and sort it
		List cellList = new ArrayList();
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			if (lib.isHidden()) continue;
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				if (cell.isBit(nodeFlagBit)) cellList.add(cell);
			}
		}
		if (cellList.size() == 0) System.out.println("No cells match this request"); else
		{
			if (orderByName.isSelected())
			{
				Collections.sort(cellList, new SortByCellName());
			} else if (orderByDate.isSelected())
			{
				Collections.sort(cellList, new SortByCellDate());
			} else if (orderByStructure.isSelected())
			{
				Collections.sort(cellList, new SortByCellStructure());
			}

			// finally show the results
			if (saveToDisk.isSelected())
			{
				String trueName = OpenFile.chooseOutputFile(OpenFile.TEXT, null, "celllist.txt");
				if (trueName == null) System.out.println("Cannot write cell listing"); else
				{
					FileOutputStream fileOutputStream = null;
					try {
						fileOutputStream = new FileOutputStream(trueName);
					} catch (FileNotFoundException e) {}
					BufferedOutputStream bufStrm = new BufferedOutputStream(fileOutputStream);
					DataOutputStream dataOutputStream = new DataOutputStream(bufStrm);
					try
					{
						DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
						String header = "List of cells created on " + df.format(new Date()) + "\n";
						dataOutputStream.write(header.getBytes(), 0, header.length());
						header = "Cell\tVersion\tCreation date\tRevision Date\tSize\tUsage\tLock\tInst-lock\tCell-lib\tDRC\tNCC\n";
						dataOutputStream.write(header.getBytes(), 0, header.length());
						for(Iterator it = cellList.iterator(); it.hasNext(); )
						{
							Cell cell = (Cell)it.next();
							String line =  makeCellLine(cell, -1) + "\n";
							dataOutputStream.write(line.getBytes(), 0, line.length());
						}
						dataOutputStream.close();
						System.out.println("Wrote " + trueName);
					} catch (IOException e)
					{
						System.out.println("Error closing " + trueName);
					}
				}
			} else
			{
				int maxLen = 0;
				for(Iterator it = cellList.iterator(); it.hasNext(); )
				{
					Cell cell = (Cell)it.next();
					maxLen = Math.max(maxLen, cell.noLibDescribe().length());
				}
				maxLen = Math.max(maxLen+2, 7);
				printHeaderLine(maxLen);
				Library lib = null;
				for(Iterator it = cellList.iterator(); it.hasNext(); )
				{
					Cell cell = (Cell)it.next();
					if (cell.getLibrary() != lib)
					{
						lib = cell.getLibrary();
						System.out.println("======== LIBRARY " + lib.getLibName() + ": ========");
					}
					System.out.println(makeCellLine(cell, maxLen));
				}
			}
		}

		// free cell and port markers
		nodeFlagBit.freeFlagSet();
		portFlagBit.freeFlagSet();
		closeDialog(null);
	}//GEN-LAST:event_ok

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		// remember settings
		if (allCells.isSelected()) whichSwitch = 0; else
		if (onlyCellsUsedElsewhere.isSelected()) whichSwitch = 1; else
		if (onlyCellsNotUsedElsewhere.isSelected()) whichSwitch = 2; else
		if (onlyCellsUnderCurrent.isSelected()) whichSwitch = 3; else
		if (onlyPlaceholderCells.isSelected()) whichSwitch = 4;

		onlyViewSwitch = onlyThisView.isSelected();
		viewSwitch = View.findView((String)views.getSelectedItem());
		alsoIconSwitch = alsoIconViews.isSelected();
		excOldVersSwitch = excludeOlderVersions.isSelected();
		excNewVersSwitch = excludeNewestVersions.isSelected();

		if (orderByName.isSelected()) orderingSwitch = 0; else
		if (orderByDate.isSelected()) orderingSwitch = 1; else
		if (orderByStructure.isSelected()) orderingSwitch = 2;

		if (displayInMessages.isSelected()) destinationSwitch = 0; else
		if (saveToDisk.isSelected()) destinationSwitch = 1;

		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JRadioButton allCells;
    private javax.swing.JCheckBox alsoIconViews;
    private javax.swing.JButton cancel;
    private javax.swing.ButtonGroup destination;
    private javax.swing.JRadioButton displayInMessages;
    private javax.swing.JCheckBox excludeNewestVersions;
    private javax.swing.JCheckBox excludeOlderVersions;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JButton ok;
    private javax.swing.JRadioButton onlyCellsNotUsedElsewhere;
    private javax.swing.JRadioButton onlyCellsUnderCurrent;
    private javax.swing.JRadioButton onlyCellsUsedElsewhere;
    private javax.swing.JRadioButton onlyPlaceholderCells;
    private javax.swing.JCheckBox onlyThisView;
    private javax.swing.JRadioButton orderByDate;
    private javax.swing.JRadioButton orderByName;
    private javax.swing.JRadioButton orderByStructure;
    private javax.swing.ButtonGroup ordering;
    private javax.swing.JRadioButton saveToDisk;
    private javax.swing.JComboBox views;
    private javax.swing.ButtonGroup whichCells;
    // End of variables declaration//GEN-END:variables
	
}
