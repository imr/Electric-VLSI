package com.sun.electric.tool.user;

/*
 * Created on Sep 30, 2003
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
public class ElectricMenu extends JMenu 
{
	private JMenuItem menuItem = null;
	
	// constructor
	private ElectricMenu(String s)
	{
		super(s);
	}
	
	private ElectricMenu(String s, char mnemonic)
	{
		super(s);
		setMnemonic(mnemonic);
	}

	// factory function
	public static ElectricMenu CreateElectricMenu(String s)
	{
		return new ElectricMenu(s);
	}
	
	public static ElectricMenu CreateElectricMenu(String s, char mnemonic)
	{
		return new ElectricMenu(s, mnemonic);
	}
	
	// add menu item
	public void addMenuItem(String s, ActionListener action)
	{
		menuItem = new JMenuItem(s);
		menuItem.addActionListener(action);
		this.add(menuItem);
	}
	
	public void addMenuItem(String s, KeyStroke accelerator, ActionListener action)
	{
		menuItem = new JMenuItem(s);
		menuItem.setAccelerator(accelerator);
		menuItem.addActionListener(action);
		this.add(menuItem);
	}
}
