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
	}
	
	public static void setMenuBar(JMenuBar menuBar)
	{
		topLevel.setJMenuBar(menuBar);
	}

	public static JDesktopPane getDesktop() { return desktop; }

	public void AddWindowExit()
	{
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e){
				System.exit(0);
			}
		});
	}
	
}
