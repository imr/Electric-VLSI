/*
 * Created on Oct 1, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.sun.electric.tool.user;

import javax.swing.JInternalFrame;

import com.sun.electric.database.hierarchy.Cell;


/**
 * @author wc147374
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ElectricDocWndFrame extends JInternalFrame 
{
		static int openFrameCount =0;
		static int windowOffset = 30;
		
		//constructor
		private ElectricDocWndFrame(Cell cell)
		{
			super(cell.describe(), true, true, true, true);
			setSize(500,500); //change size 
			setLocation(windowOffset,windowOffset);
			windowOffset += 100;
			if (windowOffset > 500) windowOffset = 0;
			setAutoscrolls(true);
			ElectricDocWnd wnd = ElectricDocWnd.CreateElectricDoc(cell);
			this.getContentPane().add(wnd);
			show();
		}
	
		//factory
		public static ElectricDocWndFrame CreateElectricDocFrame(Cell cell)
		{
			return new ElectricDocWndFrame(cell);
		}

}
