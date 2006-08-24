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

import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.extract.TransistorSearch;

import java.awt.geom.Rectangle2D;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;


/**
 * Class to handle the "Cell Lists" dialog.
 */
public class CellLists extends EDialog
{
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
		dialog.setVisible(true);
	}

	/** Creates new form Cell Lists */
	private CellLists(JFrame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
        getRootPane().setDefaultButton(ok);

		// make a popup of views
		for (View v : View.getOrderedViews())
		{
			views.addItem(v.getFullName());
		}

		curCell = WindowFrame.getCurrentCell();
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
		finishInitialization();
	}

	protected void escapePressed() { cancel(null); }

	private static String makeCellLine(Cell cell, int maxlen)
	{
		String line = cell.noLibDescribe();
		if (maxlen < 0) line += "\t"; else
		{
			for(int i=line.length(); i<maxlen; i++) line += " ";
		}

		// add the version number
		String versionString = TextUtils.toBlankPaddedString(cell.getVersion(), 5);
		line += versionString;
		if (maxlen < 0) line += "\t"; else line += "   ";

		// add the creation date
		Date creationDate = cell.getCreationDate();
		if (creationDate == null)
		{
			if (maxlen < 0) line += "UNRECORDED"; else
				line += "     UNRECORDED     ";
		} else
		{
			line += TextUtils.formatDate(creationDate);
		}
		if (maxlen < 0) line += "\t"; else line += "   ";

		// add the revision date
		Date revisionDate = cell.getRevisionDate();
		if (revisionDate == null)
		{
			if (maxlen < 0) line += "UNRECORDED"; else
				line += "     UNRECORDED     ";
		} else
		{
			line += TextUtils.formatDate(revisionDate);
		}
		if (maxlen < 0) line += "\t"; else line += "   ";

		// add the size
		if (cell.getView().isTextView())
		{
			int len = 0;
			String [] textLines = cell.getTextViewContents();
			if (textLines != null) len = textLines.length;
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

		// count the number of instances
		int total = 0;
		for(Iterator<NodeInst> it = cell.getInstancesOf(); it.hasNext(); )
		{
			total++;
			it.next();
		}
		if (maxlen < 0) line += total; else
		{
			line += TextUtils.toBlankPaddedString(total, 4);
		}
		if (maxlen < 0) line += "\t"; else line += "   ";

		// show other factors about the cell
		if (cell.isAllLocked()) line += "L"; else line += " ";
		if (maxlen < 0) line += "\t"; else line += " ";
		if (cell.isInstancesLocked()) line += "I"; else line += " ";
		if (maxlen < 0) line += "\t"; else line += " ";
		if (cell.isInCellLibrary()) line += "C"; else line += " ";
		if (maxlen < 0) line += "\t"; else line += " ";

		boolean goodDRC = false;
        int activeBits = DRC.getActiveBits(cell.getTechnology());
		Date lastGoodDate = DRC.getLastDRCDateBasedOnBits(cell, activeBits, true);
		if (!Job.getDebug() && lastGoodDate != null && cell.getRevisionDate().before(lastGoodDate)) goodDRC = true;
		if (goodDRC) line += "D"; else line += " ";
		if (maxlen < 0) line += "\t"; else line += " ";

//		if (net_ncchasmatch(cell) != 0) addstringtoinfstr(infstr, x_("N")); else
//			addstringtoinfstr(infstr, x_(" "));
		return line;
	}

	/**
	 * Method to recursively walk the hierarchy from "np", marking all cells below it.
	 */
	private void recursiveMark(Cell cell, HashSet<Cell> cellsSeen)
	{
		if (cellsSeen.contains(cell)) return;
		cellsSeen.add(cell);
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (!ni.isCellInstance()) continue;
			Cell subCell = (Cell)ni.getProto();
			recursiveMark(subCell, cellsSeen);
			Cell contentsCell = subCell.contentsView();
			if (contentsCell != null) recursiveMark(contentsCell, cellsSeen);
		}
	}

	private static class SortByCellStructure implements Comparator<Cell>
	{
		public int compare(Cell c1, Cell c2)
		{
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
//				PortProto pp1 = it.next();
//				pp1.clearBit(portFlagBit);
//			}
//			for(Iterator it = c2.getPorts(); it.hasNext(); )
//			{
//				PortProto pp2 = it.next();
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

//	/**
//	 * Class for counting instances.
//	 * It is basically a modifiable Integer.
//	 */
//	private static class InstanceCount
//	{
//		private int count;
//
//		InstanceCount() { count = 0; }
//		public void increment() { count++; }
//		public int getCount() { return count; }
//	}

	/**
	 * This method implements the command to list (recursively) the nodes in this Cell.
	 */
	public static void listNodesInCellCommand()
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;

		HashMap<NodeProto,GenMath.MutableInteger> nodeCount = new HashMap<NodeProto,GenMath.MutableInteger>();

		// first zero the count of each nodeproto
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			for(Iterator<PrimitiveNode> nIt = tech.getNodes(); nIt.hasNext(); )
			{
				PrimitiveNode np = nIt.next();
				nodeCount.put(np, new GenMath.MutableInteger(0));
			}
		}
		for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = it.next();
			for(Iterator<Cell> nIt = lib.getCells(); nIt.hasNext(); )
			{
				Cell np = nIt.next();
				nodeCount.put(np, new GenMath.MutableInteger(0));
			}
		}

		// now look at every object recursively in this cell
		addObjects(curCell, nodeCount);

		// print the totals
		System.out.println("Contents of " + curCell + ":");
		Technology printtech = null;
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology curtech = it.next();
			for(Iterator<PrimitiveNode> nIt = curtech.getNodes(); nIt.hasNext(); )
			{
				PrimitiveNode np = nIt.next();
				GenMath.MutableInteger count = nodeCount.get(np);
				if (count.intValue() == 0) continue;
				if (curtech != printtech)
				{
					System.out.println(curtech.getTechName() + " technology:");
					printtech = curtech;
				}
				System.out.println(TextUtils.toBlankPaddedString(count.intValue(), 6) + " " + np.describe(true) + " nodes");
			}
		}
		for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = it.next();
			Library printlib = null;
			for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = cIt.next();
				GenMath.MutableInteger count = nodeCount.get(cell);
				if (count.intValue() == 0) continue;
				if (lib != printlib)
				{
					System.out.println(lib + ":");
					printlib = lib;
				}
				System.out.println(TextUtils.toBlankPaddedString(count.intValue(), 6) + " " + cell.describe(true) + " nodes");
			}
		}
	}

	/**
	 * Method to recursively examine cell "np" and update the number of
	 * instantiated primitive nodeprotos in the "temp1" field of the nodeprotos.
	 */
	private static void addObjects(Cell cell, HashMap<NodeProto,GenMath.MutableInteger> nodeCount)
	{
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			NodeProto np = ni.getProto();
			GenMath.MutableInteger count = nodeCount.get(np);
			if (count != null) count.increment();

			if (!ni.isCellInstance()) continue;
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
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;

		HashMap<Cell,GenMath.MutableInteger> nodeCount = new HashMap<Cell,GenMath.MutableInteger>();

		// set counters on every cell in every library
		for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = it.next();
			for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = cIt.next();
				nodeCount.put(cell, new GenMath.MutableInteger(0));
			}
		}

		// count the number of instances in this cell
		for(Iterator<NodeInst> it = curCell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (!ni.isCellInstance()) continue;
			GenMath.MutableInteger count = nodeCount.get(ni.getProto());
			if (count != null) count.increment();
		}

		// show the results
		boolean first = true;
		for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = it.next();
			for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = cIt.next();
				GenMath.MutableInteger count = nodeCount.get(cell);
				if (count == null || count.intValue() == 0) continue;
				if (first)
					System.out.println("Instances appearing in " + curCell);
				first = false;
				String line = "   " + count.intValue() + " instances of " + cell + " at";
				for(Iterator<NodeInst> nIt = curCell.getNodes(); nIt.hasNext(); )
				{
					NodeInst ni = nIt.next();
					if (ni.getProto() != cell) continue;
					line += " (" + ni.getAnchorCenterX() + "," + ni.getAnchorCenterY() + ")";
				}
				System.out.println(line);
			}
		}
		if (first)
			System.out.println("There are no instances in " + curCell);
	}

    /**
     * This method implements the command to count the number of transistors
     * from this current Cell.
     */
    public static void numberOfTransistorsCommand()
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;
        TransistorSearch.countNumberOfTransistors(curCell);
    }

    /**
	 * This method implements the command to list the usage of the current Cell.
	 */
	public static void listCellUsageCommand()
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;

		HashMap<Cell,GenMath.MutableInteger> nodeCount = new HashMap<Cell,GenMath.MutableInteger>();

		// set counters on every cell in every library
		for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = it.next();
			for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = cIt.next();
				nodeCount.put(cell, new GenMath.MutableInteger(0));
			}
		}

		// count the number of instances in this cell
		boolean found = false;
		for(Iterator<NodeInst> nIt = curCell.getInstancesOf(); nIt.hasNext(); )
		{
			NodeInst ni = nIt.next();
			Cell cell = ni.getParent();
			GenMath.MutableInteger count = nodeCount.get(cell);
			if (count != null)
			{
				count.increment();
				found = true;
			}
		}

		// count the number of instances in this cell's icon
		Cell iconCell = curCell.iconView();
		if (iconCell != null)
		{
			for(Iterator<NodeInst> nIt = iconCell.getInstancesOf(); nIt.hasNext(); )
			{
				NodeInst ni = nIt.next();
				if (ni.isIconOfParent()) continue;
				Cell cell = ni.getParent();
				GenMath.MutableInteger count = nodeCount.get(cell);
				if (count != null)
				{
					count.increment();
					found = true;
				}
			}
		}

		// show the results
		if (!found)
		{
			System.out.println("Cell " + curCell.describe(true) + " is not used anywhere");
			return;
		}
		System.out.println("Cell " + curCell.describe(true) + " is used in these locations:");
		for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = it.next();
			for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = cIt.next();
				GenMath.MutableInteger count = nodeCount.get(cell);
				if (count == null || count.intValue() == 0) continue;
				System.out.println("  " + count.intValue() + " instances in " + cell);
			}
		}
	}

	/**
	 * This method implements the command to describe the current Cell.
	 */
	public static void describeThisCellCommand()
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;
		int maxLen = curCell.describe(false).length();
		printHeaderLine(maxLen);
		String line = makeCellLine(curCell, maxLen);
		System.out.println(line);
	}

	private static void printHeaderLine(int maxLen)
	{
		String header = "Cell";
		for(int i=4; i<maxLen; i++) header += "-";
		header += "Version--------Creation date";
		header += "---------------Revision Date--------------Size-------Usage--L-I-C-D";
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

        setTitle("Cell Lists");
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
		HashSet<Cell> cellsSeen = new HashSet<Cell>();

		// mark cells to be shown
		if (allCells.isSelected())
		{
			// mark all cells for display
			for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = it.next();
				for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = cIt.next();
					cellsSeen.add(cell);
				}
			}
		} else
		{
			// mark no cells for display, filter according to request
			if (onlyCellsUnderCurrent.isSelected())
			{
				// mark those that are under this
				recursiveMark(curCell, cellsSeen);
			} else if (onlyCellsUsedElsewhere.isSelected())
			{
				// mark those that are in use
				for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
				{
					Library lib = it.next();
					for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
					{
						Cell cell = cIt.next();
						Cell iconCell = cell.iconView();
						if (iconCell == null) iconCell = cell;
						if (cell.getInstancesOf().hasNext() || iconCell.getInstancesOf().hasNext())
							cellsSeen.add(cell);
					}
				}
			} else if (onlyCellsNotUsedElsewhere.isSelected())
			{
				// mark those that are not in use
				for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
				{
					Library lib = it.next();
					for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
					{
						Cell cell = cIt.next();
						Cell iconCell = cell.iconView();
						if (iconCell != null)
						{
							// has icon: acceptable if the only instances are examples
							if (cell.getInstancesOf().hasNext()) continue;
							boolean found = false;
							for(Iterator<NodeInst> nIt = iconCell.getInstancesOf(); nIt.hasNext(); )
							{
								NodeInst ni = nIt.next();
								if (ni.isIconOfParent()) { found = true;   break; }
							}
							if (found) continue;
						} else
						{
							// no icon: reject if this has instances
							if (cell.isIcon())
							{
								// this is an icon: reject if instances are not examples
								boolean found = false;
								for(Iterator<NodeInst> nIt = cell.getInstancesOf(); nIt.hasNext(); )
								{
									NodeInst ni = nIt.next();
									if (ni.isIconOfParent()) { found = true;   break; }
								}
								if (found) continue;
							} else
							{
								if (cell.getInstancesOf().hasNext()) continue;
							}
						}
						cellsSeen.add(cell);
					}
				}
			} else
			{
				// mark placeholder cells
				for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
				{
					Library lib = it.next();
					for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
					{
						Cell cell = cIt.next();
						Variable var = cell.getVar("IO_true_library");
						if (var != null) cellsSeen.add(cell);
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
				for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
				{
					Library lib = it.next();
					for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
					{
						Cell cell = cIt.next();
						if (cell.getView() != v)
						{
							if (cell.isIcon())
							{
								if (alsoIconViews.isSelected()) continue;
							}
							cellsSeen.remove(cell);
						}
					}
				}
			}
		}

		// filter versions
		if (excludeOlderVersions.isSelected())
		{
			for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = it.next();
				for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = cIt.next();
					if (cell.getNewestVersion() != cell) cellsSeen.remove(cell);
				}
			}
		}
		if (excludeNewestVersions.isSelected())
		{
			for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
			{
				Library lib = it.next();
				for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
				{
					Cell cell = cIt.next();
					if (cell.getNewestVersion() == cell) cellsSeen.remove(cell);
				}
			}
		}

		// now make a list and sort it
		List<Cell> cellList = new ArrayList<Cell>();
		for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = it.next();
			if (lib.isHidden()) continue;
			for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = cIt.next();
				if (cellsSeen.contains(cell)) cellList.add(cell);
			}
		}
		if (cellList.size() == 0) System.out.println("No cells match this request"); else
		{
			if (orderByName.isSelected())
			{
				Collections.sort(cellList/*, new TextUtils.CellsByName()*/);
			} else if (orderByDate.isSelected())
			{
				Collections.sort(cellList, new TextUtils.CellsByDate());
			} else if (orderByStructure.isSelected())
			{
				Collections.sort(cellList, new SortByCellStructure());
			}

			// finally show the results
			if (saveToDisk.isSelected())
			{
				String trueName = OpenFile.chooseOutputFile(FileType.READABLEDUMP, null, "celllist.txt");
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
						for(Cell cell : cellList)
						{
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
				for(Cell cell : cellList)
				{
					maxLen = Math.max(maxLen, cell.noLibDescribe().length());
				}
				maxLen = Math.max(maxLen+2, 7);
				printHeaderLine(maxLen);
				Library lib = null;
				for(Cell cell : cellList)
				{
					if (cell.getLibrary() != lib)
					{
						lib = cell.getLibrary();
						System.out.println("======== LIBRARY " + lib.getName() + ": ========");
					}
					System.out.println(makeCellLine(cell, maxLen));
				}
			}
		}

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
