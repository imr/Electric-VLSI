package com.sun.electric.tool.user.ui;

/*
 * Created on Sep 22, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;


/**
 * @author wc147374
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class UITopLevel extends JFrame
{
	static JDesktopPane desktop;
	static UITopLevel topLevel;
	static String libdir;

	private UITopLevel()
	{
		super();
	}
	
	private UITopLevel(String s)
	{
		super(s);
	}
	
	public static void Initialize()
	{
		try{
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch(Exception e) {}

		topLevel = new UITopLevel("Electric");	
		topLevel.AddWindowExit();
		Dimension scrnSize = (Toolkit.getDefaultToolkit()).getScreenSize();
		scrnSize.height -= 30;
		topLevel.setSize(scrnSize);

		// make the desktop
		desktop = new JDesktopPane();
		topLevel.getContentPane().add(desktop);
		topLevel.setVisible(true);

		// create the messages window
		UIMessages cl = new UIMessages(desktop.getSize());
		desktop.add(cl.getFrame());

		// figure out the library directory
		libdir = null;
		File dir1 = new File ("./lib");
		try
		{
			libdir = dir1.getCanonicalPath();
		} catch(Exception e) { libdir = null; }
		libdir = "C:\\DevelE\\Electric\\lib";

		topLevel.setIconImage(new ImageIcon(UITopLevel.getLibDir() + "\\ElectricIcon.gif").getImage());
	}
	
	public static void setMenuBar(JMenuBar menuBar)
	{
		topLevel.setJMenuBar(menuBar);
	}

	public static JDesktopPane getDesktop() { return desktop; }
	public static String getLibDir() { return libdir; }

	public void AddWindowExit()
	{
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e){
				System.exit(0);
			}
		});
	}
	
}
