package com.sun.electric.tool.user;

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
public class ElectricFrame extends JFrame
{

	private ElectricFrame()
	{
		super();
	}
	
	private ElectricFrame(String s)
	{
		super(s);
	}
	
	public static ElectricFrame CreateFrame(String s)
	{
		return new ElectricFrame(s);	
	}
	
	public void AddWindowExit()
	{
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e){
				System.exit(0);
			}
		});
	}
	
}
