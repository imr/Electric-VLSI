/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TopLevel.java
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
package com.sun.electric.tool.user.ui;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.tool.user.UserMenuCommands;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.ImageIcon;


/**
 * @author wc147374
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TopLevel extends JFrame
{
	private static JDesktopPane desktop;
	private static TopLevel topLevel;
	
	private static int mode;
	private static List windowList = new ArrayList();
	private static TopLevel current;
	private boolean editWindow;
	private EditWindow edwin;
	
	public final static int SDIMode = 1;
	public final static int MDIMode = 2;

	private TopLevel(String s)
	{
		super(s);
	}

	public static void InitLookAndFeel()
	{
//		setup specific look-and-feel
		 try{
			 String os = System.getProperty("os.name").toLowerCase();
			 if (os.startsWith("windows"))
			 {
				 UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			 } else if (os.startsWith("linux") || os.startsWith("solaris") || os.startsWith("sunos"))
			 {
				 UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.MotifLookAndFeel");
			 } else if (os.startsWith("mac"))
			 {
				 UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.MacLookAndFeel");
			 }
		 } catch(Exception e) {}
	}

	public static void Initialize()
	{
		// make the top-level window
		topLevel = new TopLevel("Electric");	
		topLevel.AddWindowExit();
		Dimension scrnSize = (Toolkit.getDefaultToolkit()).getScreenSize();
		scrnSize.height -= 30;
		topLevel.setSize(scrnSize);

		// make the desktop
		desktop = new JDesktopPane();
		topLevel.getContentPane().setLayout(new BorderLayout());
		topLevel.getContentPane().add(desktop, BorderLayout.CENTER);
		topLevel.setVisible(true);

		// set an icon with the application
		topLevel.setIconImage(new ImageIcon(topLevel.getClass().getResource("IconElectric.gif")).getImage());

		// create the menu bar
		JMenuBar menuBar = UserMenuCommands.createMenuBar();
		topLevel.setJMenuBar(menuBar);

		// create the tool bar
		ToolBar toolBar = ToolBar.createToolBar();
		topLevel.getContentPane().add(toolBar, BorderLayout.NORTH);

		// create the messages window
		MessagesWindow cl = new MessagesWindow(desktop.getSize());
		desktop.add(cl.getFrame());
	}

	public static JDesktopPane getDesktop() { return desktop; }

	public static EditWindow getCurrentEditWindow()
	{
		if(mode==MDIMode)
        {
        	JInternalFrame frame = desktop.getSelectedFrame();
        	if (frame == null || !(frame instanceof WindowFrame))
            	return null;
			WindowFrame wf = (WindowFrame)frame;
			return wf.getEditWindow();
        }
        else
        {
        	return current.getEditWindow();
        }
	}

	public void AddWindowExit()
	{
		this.addWindowListener(new WindowsEvents(this));
		this.addWindowFocusListener(new WindowsEvents(this));
	}
	
	public static void setMode(int newmode)
	{
		mode = newmode;
	}

	public static int getMode()
	{
		return mode;
	}
	public static void sdiInit()
	{
		TopLevel sdi = new TopLevel("Electric");
		sdi.AddWindowExit(); 
//			create the menu bar
		sdi.getContentPane().setLayout(new BorderLayout());
		JMenuBar menuBar = UserMenuCommands.createMenuBar();
		sdi.setJMenuBar(menuBar);

			 // create the tool bar
		ToolBar toolBar = ToolBar.createToolBar();
		sdi.getContentPane().add(toolBar, BorderLayout.NORTH);
		sdi.setVisible(true);

		sdi.setSize(new Dimension(640,480));
		windowList.add(sdi);
		sdi.setEditWindowOn(false);
		current = sdi;
	}
	
	public boolean isThereEditWindow()
	{
		return editWindow;
	}
	
	public void setEditWindowOn(boolean bool)
	{
		editWindow=bool;
	}
	
	public static void createEditWindow(Cell cell)
	{
		if(TopLevel.getMode()==TopLevel.SDIMode)
		{
			if(current.isThereEditWindow()==false)
			{
				EditWindow wnd = EditWindow.CreateElectricDoc(cell);
			
				// the left half: a cell explorer tree in a scroll pane
				TreeView tree = TreeView.CreateTreeView(Library.getExplorerTree(), wnd);
				JScrollPane scrolledTree = new JScrollPane(tree);
				JSplitPane js = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
				js.setRightComponent(wnd);
				js.setLeftComponent(scrolledTree);
				js.setDividerLocation(180);
				current.getContentPane().add(js);
				current.setEditWindowOn(true);
				String str = current.getTitle();
				current.setTitle(str+" - "+cell.describe());
				current.show();
				current.setEditWindow(wnd);
			}
			else
			{
				TopLevel.sdiInit();
				TopLevel.createEditWindow(cell);
			}
		}
	}
	
	public void setEditWindow(EditWindow wnd)
	{
		edwin=wnd;
	}
	
	public EditWindow getEditWindow()
	{
		return edwin;
	}
	public static Iterator getWindows()
	{
		return windowList.iterator();
	}
	
	public static void removeWindow(TopLevel tl)
	{
		windowList.remove(tl);
	}
	
	public static TopLevel getCurrentWindow()
	{
		return current;
	}
	
	public static void setCurrentWindow(TopLevel tl)
	{
		current = tl;
	}
}


class WindowsEvents extends WindowAdapter
{
	TopLevel window;
	
	WindowsEvents(TopLevel tl)
	{
		super();
		this.window = tl;	
	}

	public void windowGainedFocus(WindowEvent e)
	{
		if(TopLevel.getMode()==TopLevel.SDIMode)
			TopLevel.setCurrentWindow(window);
	}
	
	public void windowClosing(WindowEvent e)
	{
		if(TopLevel.getMode()==TopLevel.MDIMode)
		{
			System.exit(0);
		}
		else if(TopLevel.getMode()==TopLevel.SDIMode)
		{
			Iterator it = TopLevel.getWindows();
			it.next();
			if(it.hasNext())
			{
				TopLevel.removeWindow(window);
				window.dispose();
			}
			else
			{
				System.exit(0);
			}
		}
		else
		{
			System.exit(0);
		}
	}		
}