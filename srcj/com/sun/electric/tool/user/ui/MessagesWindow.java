/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MessagesWindow.java
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

import com.sun.electric.tool.user.dialogs.OpenFile;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

/**
 * a console for the Java side of Electric.  Used because the standard
 * Electric console can't handle multiple threads of printing.
 * An instance of this class should be set as the PrintStream for System.out,
 * e.g. System.setOut(new PrintStream(new MessagesWindow()));
 * In such a situation, there should never be a reason to call any of
 * the methods of this class directly.
 */
public class MessagesWindow
	extends OutputStream
	implements ActionListener, KeyListener, Runnable
{
	private ArrayList history;
	private JTextField entry;
	private JTextArea info;
	private Container contentFrame;
	private int histidx = 0;
	private Thread ticker = null;
	private StringBuffer buffer = new StringBuffer();
	private Container jf;
	private PrintWriter printWriter = null;

	// -------------------- private and protected methods ------------------------
	public MessagesWindow()
	{
		Dimension scrnSize = TopLevel.getScreenSize();
		Dimension msgSize = new Dimension(scrnSize.width/3*2, scrnSize.height/100*15);
		if (TopLevel.isMDIMode())
		{
			JInternalFrame jInternalFrame = new JInternalFrame("Electric Messages", true, false, true, true);
			jf = jInternalFrame;
			contentFrame = jInternalFrame.getContentPane();
			jInternalFrame.setFrameIcon(new ImageIcon(getClass().getResource("IconElectric.gif")));
		} else
		{
			JFrame jFrame = new JFrame("Electric Messages");
			jf = jFrame;
			jFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			contentFrame = jFrame.getContentPane();
		}
		history = new ArrayList();
		entry = new JTextField();
		entry.addActionListener(this);
		entry.addKeyListener(this);
		info = new JTextArea(20, 110);
		info.setLineWrap(false);
		JScrollPane scrollPane = new JScrollPane(info,
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setPreferredSize(msgSize);
		JScrollBar vertscroll = scrollPane.getVerticalScrollBar();
		contentFrame.setLayout(new BorderLayout());
		contentFrame.add(entry, BorderLayout.SOUTH);
		contentFrame.add(scrollPane, BorderLayout.CENTER);
		jf.setLocation(150, scrnSize.height/100*80);
		if (TopLevel.isMDIMode())
		{
			((JInternalFrame)jf).pack();
			((JInternalFrame)jf).show();
			TopLevel.addToDesktop((JInternalFrame)jf);
		} else
		{
			((JFrame)jf).pack();
			((JFrame)jf).show();
		}

		System.setOut(new java.io.PrintStream(this));
	}

	public Rectangle getMessagesLocation()
	{
		return jf.getBounds();
	}

	public void flush()
	{
		// no need to do anything.
	}

	public void close()
	{
		// don't close!
	}

	/**
	 * Method to erase everything in the messages window.
	 */
	public void clear()
	{
		info.setText("");
	}

	/**
	 * Method to start saving the messages window.
	 */
	public void save()
	{
		String filePath = OpenFile.chooseOutputFile(OpenFile.Type.TEXT, null, "emessages.txt");
		if (filePath == null) return;
		try
		{
			printWriter = new PrintWriter(new BufferedWriter(new FileWriter(filePath)));
		} catch (IOException e)
		{
			System.out.println("Error creating " + filePath);
			return;
		}
		System.out.println("Messages will be saved to " + filePath);
	}

	public void write(byte[] b)
	{
		appendString(new String(b));
	}

	public void write(int b)
	{
		appendString(String.valueOf((char) b));
	}

	public void write(byte[] b, int off, int len)
	{
		appendString(new String(b, off, len));
	}

	protected void appendString(String str)
	{
		synchronized (buffer)
		{
			if (printWriter != null)
			{
				printWriter.print(str);
				printWriter.flush();
			}
			buffer.append(str);
			if (ticker == null)
			{
				ticker = new Thread(this);
				ticker.start();
			}
		}
	}

	public void run()
	{
		try
		{
			Thread.sleep(200);
		} catch (InterruptedException ie)
		{
		}
		ticker = null;
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				synchronized (buffer)
				{
					dump(buffer.toString());
					buffer.setLength(0);
				}
			}
		});
	}

	protected void dump(String str)
	{
		info.append(str);
		try
		{
			Rectangle r = info.modelToView(info.getDocument().getLength());
			info.scrollRectToVisible(r);
		} catch (javax.swing.text.BadLocationException ble)
		{
		}
	}

	public void keyPressed(KeyEvent evt)
	{
		int code = evt.getKeyCode();
		if (code == KeyEvent.VK_UP)
		{
			if (histidx > 0)
			{
				histidx--;
			}
			if (histidx < history.size())
			{
				entry.setText((String) history.get(histidx));
			}
		} else if (code == KeyEvent.VK_DOWN)
		{
			if (histidx < history.size())
			{
				histidx++;
				if (histidx < history.size())
				{
					entry.setText((String) history.get(histidx));
				} else
				{
					entry.setText("");
				}
			}
		}
	}

	public void keyReleased(KeyEvent evt)
	{
	}

	public void keyTyped(KeyEvent evt)
	{
	}

	public void actionPerformed(ActionEvent evt)
	{
		String msg = entry.getText();
		history.add(msg);
		histidx = history.size();
		entry.setText("");
		info.append("=================== " + msg + " =================\n");

		// split msg into strings
		StringTokenizer st = new StringTokenizer(msg);
		String cmds[] = new String[st.countTokens()];
        if (cmds.length == 0) return;
		for (int i = 0; i < cmds.length; i++)
		{
			cmds[i] = st.nextToken();
		}

		if (cmds[0].equals("mem"))
		{
			Runtime rt = Runtime.getRuntime();
			System.out.println("Total memory: " + rt.totalMemory());
			System.out.println("Free memory: " + rt.freeMemory());
		}
	}

}
