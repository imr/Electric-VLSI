/*
 * Created on Nov 18, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.sun.electric.tool.user.ui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;


import com.borland.jbcl.layout.XYConstraints;
import com.borland.jbcl.layout.XYLayout;

/**
 * @author Willy Chung
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class SelectFrame extends JDialog 
{
	JPanel jpanel1 = new JPanel();
	XYLayout xYLayout1 = new XYLayout();
	JRadioButton mdiButton = new JRadioButton();
	JRadioButton sdiButton = new JRadioButton();
	Button button = Button.newInstance("OK");
	ButtonGroup choice = new ButtonGroup();
	
	private SelectFrame(Frame frame, String title, boolean modal)
	{
		super(frame, title, modal);
		Initialize();
		pack();
		show();
	}
	
	public static SelectFrame CreateSelectFrameDialog(Frame frame, String title, boolean modal)
	{
		return new SelectFrame(frame, title, modal);
	}
	
	private void Initialize()
	{
		mdiButton.setSelected(true);
		mdiButton.setText("Multi Document Interface");
				
		sdiButton.setSelected(false);
		sdiButton.setText("Single Document Interface");
		
		button.setBorderPainted(true);
		button.addActionListener(new FrameStyleSelected(this));
		
		jpanel1.setLayout(xYLayout1);
		jpanel1.setPreferredSize(new Dimension(270,80));
		jpanel1.setVerifyInputWhenFocusTarget(true);
		jpanel1.add(mdiButton, new XYConstraints(15,14,-1,-1));
		jpanel1.add(sdiButton, new XYConstraints(15,41,-1,-1));
		jpanel1.add(button, new XYConstraints(197,13,66,42));
		
		this.getContentPane().add(jpanel1);
		
		choice.add(sdiButton);
		choice.add(mdiButton);	
	}
	
	void Button_Pressed(ActionEvent e)
	{
		if(mdiButton.isSelected())
		{
			TopLevel.setMode(TopLevel.MDIMode);
			TopLevel.Initialize();
		}
		else if(sdiButton.isSelected())
		{	
			TopLevel.setMode(TopLevel.SDIMode);
			TopLevel.sdiInit();
			MessagesWindow cl = new MessagesWindow(new Dimension(300,150),true);
		}
		this.dispose();
	}
}

class FrameStyleSelected implements ActionListener
{
	SelectFrame frame;
	
	FrameStyleSelected(SelectFrame selectframe)
	{
		this.frame=selectframe;
	}
	
	public void actionPerformed(ActionEvent e) 
	{
		frame.Button_Pressed(e);	
	}
}