/*
 * Created on Oct 1, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.sun.electric.tool.user.ui;

import javax.swing.*;

import com.sun.electric.database.hierarchy.Cell;


/**
 * @author wc147374
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class UIEditFrame extends JInternalFrame 
{
	static int windowOffset = 0;

	// constructor
	private UIEditFrame(Cell cell)
	{
		super(cell.describe(), true, true, true, true);
		setSize(500, 500);
		setLocation(windowOffset, windowOffset);
		windowOffset += 70;
		if (windowOffset > 300) windowOffset = 0;
		setAutoscrolls(true);
		UIEdit wnd = UIEdit.CreateElectricDoc(cell);
		this.getContentPane().add(wnd);
		show();
//		this.moveToFront();
//		this.toFront();
	}

	// factory
	public static UIEditFrame CreateEditWindow(Cell cell)
	{
		UIEditFrame frame = new UIEditFrame(cell);
		JDesktopPane desktop = UITopLevel.getDesktop();
		desktop.add(frame); 
		return frame;
	}

}
