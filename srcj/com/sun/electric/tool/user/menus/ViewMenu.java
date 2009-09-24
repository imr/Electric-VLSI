/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ViewMenu.java
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ViewChanges;
import com.sun.electric.tool.user.dialogs.ViewControl;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.util.List;

import javax.swing.JOptionPane;

/**
 * Class to handle the commands in the "View" pulldown menu.
 */
public class ViewMenu {

	static EMenu makeMenu() {
		/****************************** THE VIEW MENU ******************************/

		// mnemonic keys available:  B DEF   J  MN PQR     X Z
		return new EMenu("_View",

			new EMenuItem("View _Control...") { public void run() {
				viewControlCommand(); }},
			new EMenuItem("Chan_ge Cell's View...") { public void run() {
				changeViewCommand(); }},

			SEPARATOR,

			new EMenuItem("Edit La_yout View") { public void run() {
				editLayoutViewCommand(); }},
			new EMenuItem("Edit Schema_tic View") { public void run() {
				editSchematicViewCommand(); }},
			new EMenuItem("Edit Ic_on View") { public void run() {
				editIconViewCommand(); }},
			new EMenuItem("Edit V_HDL View") { public void run() {
				editVHDLViewCommand(); }},
			new EMenuItem("Edit Document_ation View") { public void run() {
				editDocViewCommand(); }},
			new EMenuItem("Edit S_keleton View") { public void run() {
				editSkeletonViewCommand(); }},
			new EMenuItem("Edit Other Vie_w...") { public void run() {
				editOtherViewCommand(); }},

			SEPARATOR,

			new EMenuItem("Make _Icon View") { public void run() {
				ViewChanges.makeIconViewCommand(); }},
			new EMenuItem("Make _Schematic View") { public void run() {
				ViewChanges.makeSchematicView(); }},
			new EMenuItem("Make Alternate Layo_ut View...") { public void run() {
				ViewChanges.makeLayoutView(); }},
			new EMenuItem("Make Ske_leton View") { public void run() {
				ViewChanges.makeSkeletonViewCommand(); }},
			new EMenuItem("Make _VHDL View") { public void run() {
				ToolMenu.makeVHDL(); }});
	}

	/**
	 * This method implements the command to control Views.
	 */
	public static void viewControlCommand()
	{
		 ViewControl dialog = new ViewControl(TopLevel.getCurrentJFrame());
		dialog.setVisible(true);
	}

	public static void changeViewCommand()
	{
		Cell cell = WindowFrame.getCurrentCell();
		if (cell == null) return;
		if (cell.getView() == View.ICON)
		{
			Job.getUserInterface().showErrorMessage("Icon cells are special and cannot have their views changed",
				"Cannot change view");
			return;
		}

		List<View> views = View.getOrderedViews();
		String [] viewNames = new String[views.size()];
		int j = 0;
		for(int i=0; i<views.size(); i++)
		{
			if (views.get(i) == View.ICON) continue;
			viewNames[j++] = views.get(i).getFullName();
		}
		viewNames[j] = "Icon (cannot change into Icon view)";
		Object newName = JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(), "New view for this cell",
			"Choose alternate view", JOptionPane.QUESTION_MESSAGE, null, viewNames, cell.getView().getFullName());
		if (newName == null) return;
		String newViewName = (String)newName;
		View newView = View.findView(newViewName);
		if (newView != null && newView != cell.getView())
		{
			ViewChanges.changeCellView(cell, newView);
		}
	}

	public static void editLayoutViewCommand()
	{
		editView(View.LAYOUT);
	}

	public static void editSchematicViewCommand()
	{
		editView(View.SCHEMATIC);
	}

	public static void editIconViewCommand()
	{
		editView(View.ICON);
	}

	public static void editVHDLViewCommand()
	{
		editView(View.VHDL);
	}

	public static void editDocViewCommand()
	{
		editView(View.DOC);
	}

	public static void editSkeletonViewCommand()
	{
		editView(View.LAYOUTSKEL);
	}

	public static void editOtherViewCommand()
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;

		List<View> views = View.getOrderedViews();
		String [] viewNames = new String[views.size()];
		for(int i=0; i<views.size(); i++)
			viewNames[i] = views.get(i).getFullName();
		Object newName = JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(), "Which associated view do you want to see?",
			"Choose alternate view", JOptionPane.QUESTION_MESSAGE, null, viewNames, curCell.getView().getFullName());
		if (newName == null) return;
		String newViewName = (String)newName;
		View newView = View.findView(newViewName);
		editView(newView);
	}

	private static void editView(View v)
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;
		if (curCell.getView() == v)
		{
			System.out.println("Cell " + curCell.describe(false) + " is already the " + v.getFullName() + " view");
			return;
		}
		Cell otherCell = curCell.otherView(v);
		if (otherCell != null)
		{
            WindowFrame wf = WindowFrame.getCurrentWindowFrame();
			if (User.isShowCellsInNewWindow()) wf = null;
			if (wf == null) wf = WindowFrame.createEditWindow(otherCell);
            wf.setCellWindow(otherCell, null);
			return;
		}
		String [] options = {"Yes", "No"};
		int ret = Job.getUserInterface().askForChoice("There is no " + v.getFullName() + " view of " + curCell +
			"\nDo you want to create an empty cell?", "Create " + v.getFullName() + " View", options, "No");
		if (ret == 1) return;
		new ViewChanges.CreateAndViewCell(curCell.getName() + "{" + v.getAbbreviation() + "}", curCell.getLibrary());
	}
}
