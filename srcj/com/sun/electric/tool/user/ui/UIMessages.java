package com.sun.electric.tool.user.ui;

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.io.Input;

import bsh.*;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Dimension;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.JFrame;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.JInternalFrame;

/**
 * a console for the Java side of Electric.  Used because the standard
 * Electric console can't handle multiple threads of printing.
 * An instance of this class should be set as the PrintStream for System.out,
 * e.g. System.setOut(new PrintStream(new UIMessages()));
 * In such a situation, there should never be a reason to call any of
 * the methods of this class directly.
 */
public class UIMessages
	extends OutputStream
	implements ActionListener, KeyListener, CaretListener, Runnable
{
	ArrayList history;
	JTextField entry;
	JTextArea info;
	JScrollBar vertscroll;
	int histidx = 0;
	Thread ticker = null;
	StringBuffer buffer = new StringBuffer();
	Interpreter bi;
	JInternalFrame jf;

	// -------------------- private and protected methods ------------------------
	public UIMessages(Dimension scrnSize)
	{
		jf = new JInternalFrame("Messages", true, true, true, true);
//		jf.setDefaultCloseOperation(jf.DO_NOTHING_ON_CLOSE);
		bi = new Interpreter();
		history = new ArrayList();
		entry = new JTextField();
		entry.addActionListener(this);
		entry.addKeyListener(this);
		info = new JTextArea(10, 80);
		info.addCaretListener(this);
		info.setLineWrap(false);
		JScrollPane scroll = new JScrollPane(info,
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		vertscroll = scroll.getVerticalScrollBar();
		jf.getContentPane().setLayout(new BorderLayout());
		jf.getContentPane().add(entry, BorderLayout.SOUTH);
		jf.getContentPane().add(scroll, BorderLayout.CENTER);
		jf.pack();
		jf.setLocation(100, scrnSize.height/4*3);
		jf.show();

		System.setOut(new java.io.PrintStream(this));
	}

	public JInternalFrame getFrame() { return jf; }

	public void interpret(String args[])
	{
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < args.length - 1; i++)
		{
			sb.append(args[i] + " ");
		}
		sb.append(args[args.length - 1]);
		try
		{
			Object obj = bi.eval(sb.toString());
			System.out.println("=>" + obj);
		} catch (EvalError ee)
		{
			System.out.println(ee);
		}
	}

	public void flush()
	{
		// no need to do anything.
	}

	public void close()
	{
		// don't close!
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
		for (int i = 0; i < cmds.length; i++)
		{
			cmds[i] = st.nextToken();
		}

		if (cmds[0].equals("mem"))
		{
			Runtime rt = Runtime.getRuntime();
			System.out.println("Total memory: " + rt.totalMemory());
			System.out.println("Free memory: " + rt.freeMemory());
		} else
		{
			// try to execute it
			interpret(cmds);
		}
	}

	public void caretUpdate(CaretEvent evt)
	{
		int d = evt.getDot();
		int m = evt.getMark();
		if (d != m)
		{
			String sel = info.getSelectedText();
			try
			{
				int value = Integer.parseInt(sel, 16);
				if (value > 0)
				{
					String cmds[] = new String[2];
					cmds[0] = "info";
					cmds[1] = sel;
					info.append("=================== info " + sel + " =================\n");
					interpret(cmds);
				}
			} catch (NumberFormatException nfe)
			{
			}
		}
	}
}
