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

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Client;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Observable;
import java.util.Observer;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
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
	implements Observer, ActionListener, MouseListener, KeyListener, Runnable, ClipboardOwner
{
    private static final String[] NULL_STRING_ARRAY = {};
    private static final int STACK_SIZE = Client.isOSMac()?0:8*1024;

    private ArrayList<String> history;
	private JTextField entry;
	private JTextArea info;
	private Container contentFrame;
	private int histidx = 0;
	private final Thread ticker = new Thread(null, this, "MessagesTicker", STACK_SIZE);
    private boolean dumpInvoked = false;
	private StringBuilder buffer = new StringBuilder();
	private Container jf;

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
			jInternalFrame.setFrameIcon(TopLevel.getFrameIcon());
			jf.setLocation(150, scrnSize.height/100*80);
		} else
		{
			JFrame jFrame = new JFrame("Electric Messages");
			jf = jFrame;
			jFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			contentFrame = jFrame.getContentPane();
			jFrame.setIconImage(TopLevel.getFrameIcon().getImage());
			Point pt = User.getDefaultMessagesPos();
			if (pt == null) pt = new Point(150, scrnSize.height/100*80);
			jf.setLocation(pt);
			Dimension override = User.getDefaultMessagesSize();
			if (override != null) jf.setPreferredSize(override);
		}
		contentFrame.setLayout(new BorderLayout());
		history = new ArrayList<String>();

		entry = new JTextField();
		entry.addActionListener(this);
		entry.addKeyListener(this);
		contentFrame.add(entry, BorderLayout.SOUTH);

		info = new JTextArea(20, 110);
		info.setLineWrap(false);
        info.setFont(new Font("Monospaced", 0, 12));
		info.addMouseListener(this);
		JScrollPane scrollPane = new JScrollPane(info,
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setPreferredSize(msgSize);
		contentFrame.add(scrollPane, BorderLayout.CENTER);

        ticker.start();
//		jf.setLocation(150, scrnSize.height/100*80);
		if (TopLevel.isMDIMode())
		{
			((JInternalFrame)jf).pack();
			//((JInternalFrame)jf).show();
			TopLevel.addToDesktop((JInternalFrame)jf);
		} else
		{
			((JFrame)jf).pack();
			((JFrame)jf).setVisible(true);
		}
	}

	public Component getComponent()
	{
		return jf;
	}

    public boolean isFocusOwner()
    {
		if (TopLevel.isMDIMode())
		{
	        return ((JInternalFrame)jf).isSelected();
		} else
		{
	        return jf.isFocusOwner();
		}
    }

    /**
     * Method to request focus on this window
     */
    public void requestFocus() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { requestFocusUnsafe(); }
            });
            return;
        }
        requestFocusUnsafe();
    }

    private void requestFocusUnsafe()
    {
		if (TopLevel.isMDIMode())
		{
			((JInternalFrame)jf).toFront();
            try {
            	((JInternalFrame)jf).setSelected(true);
            } catch (java.beans.PropertyVetoException e) {}
        } else
        {
        	((JFrame)jf).toFront();
            jf.requestFocus();
        }
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

    public void update(Observable obs, Object str)
    {
        String mess = (String)str;
        appendString(mess);
    }

	private void appendString(String str)
	{
        if (str.length() == 0) return;

		synchronized (buffer)
		{
            if (buffer.length() == 0)
                buffer.notify();
			buffer.append(str);
		}
	}

    public void run() {
        for (;;) {
            synchronized (buffer) {
                try {
                    while (dumpInvoked || buffer.length() == 0)
                        buffer.wait();
                } catch (InterruptedException ie) {
                }
                dumpInvoked = true;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException ie) {}
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    String s;
                    synchronized (buffer) {
                        s = buffer.toString();
                        buffer.setLength(0);
                        dumpInvoked = false;
                    }
                    dump(s);
                }
            });
        }
    }

	protected void dump(String str)
	{
		info.append(str);
		if (Job.BATCHMODE) return;
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

	public void keyReleased(KeyEvent evt) {}

	public void keyTyped(KeyEvent evt) {}

	public void mouseClicked(MouseEvent e) {}

	public void mouseEntered(MouseEvent e) {}

	public void mouseExited(MouseEvent e) {}

	public void mousePressed(MouseEvent e)
	{
		// popup menu event (right click)
		if (e.isPopupTrigger()) doContext(e);
	}

	public void mouseReleased(MouseEvent e)
	{
		// popup menu event (right click)
		if (e.isPopupTrigger()) doContext(e);
	}

	public void lostOwnership (Clipboard parClipboard, Transferable parTransferable) {}

	private void doContext(MouseEvent e)
	{
		JPopupMenu menu = new JPopupMenu("Messages Window");

		JMenuItem menuItem = new JMenuItem("Cut");
		menu.add(menuItem);
		menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { copyText(false, true); } });
		menuItem = new JMenuItem("Copy");
		menu.add(menuItem);
		menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { copyText(false, false); } });
		menuItem = new JMenuItem("Paste");
		menu.add(menuItem);
		menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { pasteText(); } });

		menu.addSeparator();

		menuItem = new JMenuItem("Cut All");
		menu.add(menuItem);
		menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { copyText(true, true); } });
		menuItem = new JMenuItem("Copy All");
		menu.add(menuItem);
		menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { copyText(true, false); } });
		menuItem = new JMenuItem("Clear");
		menu.add(menuItem);
		menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { clear(); } });

		menu.show((Component)e.getSource(), e.getX(), e.getY());
	}

	private void pasteText()
	{
		info.paste();
	}

	private void copyText(boolean all, boolean cut)
	{
		if (all)
		{
			if (cut)
			{
				info.selectAll();
				info.cut();
			} else
			{
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(new StringSelection(info.getText()), this);
			}
		} else
		{
			if (cut) info.cut(); else
				info.copy();
		}
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

    /************************************ MESSAGES WINDOW FONT SETTING ************************************/

	/**
	 * Method to interactively select the messages window font.
	 */
    public void selectFont()
	{
         if (TopLevel.isMDIMode())
        {
            JFrame jf = TopLevel.getCurrentJFrame();
            new FontSelectDialog(jf);
        } else
        {
            new FontSelectDialog(null);
        }
	}

	private class FontSelectDialog extends EDialog
	{
		private Font initialFont;
		private String initialFontName;
		private int initialFontSize;
		private JLabel sampleText;
		private JList fontNameList;
		private JList fontSizeList;

		public FontSelectDialog(Frame parent)
		{
			super(parent, true);
			setTitle("Set Messages Window Font");
	        getContentPane().setLayout(new java.awt.GridBagLayout());

			// get the current messages window font
			initialFont = info.getFont();
			initialFontName = initialFont.getName();
			initialFontSize = initialFont.getSize();

			// the title of the font column
			JLabel fontLabel = new JLabel("Font:");
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.anchor = java.awt.GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
	        getContentPane().add(fontLabel, gbc);

	        // the font column
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			String [] fontNames = ge.getAvailableFontFamilyNames();
			JScrollPane fontNamePane = new JScrollPane();
			DefaultListModel fontNameListModel = new DefaultListModel();
			fontNameList = new JList(fontNameListModel);
			fontNameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			fontNamePane.setViewportView(fontNameList);
			int initialIndex = 0;
			for(int i=0; i<fontNames.length; i++)
			{
				if (fontNames[i].equals(initialFontName)) initialIndex = i;
				fontNameListModel.addElement(fontNames[i]);
			}
			fontNameList.setSelectedIndex(initialIndex);
			fontNameList.ensureIndexIsVisible(initialIndex);
			fontNameList.addMouseListener(new MouseAdapter()
			{
				public void mouseClicked(MouseEvent evt) { updateSampleText(); }
			});
			gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 1;
			gbc.weightx = 1;
			gbc.weighty = 1;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.anchor = java.awt.GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
	        getContentPane().add(fontNamePane, gbc);

	        // the title of the font size column
	        JLabel sizeLabel = new JLabel("Size:");
			gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = 0;
			gbc.anchor = java.awt.GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
	        getContentPane().add(sizeLabel, gbc);

	        // the font size column
			JScrollPane fontSizePane = new JScrollPane();
			DefaultListModel fontSizeListModel = new DefaultListModel();
			fontSizeList = new JList(fontSizeListModel);
			fontSizeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			fontSizePane.setViewportView(fontSizeList);
			fontSizeListModel.addElement("8");
			fontSizeListModel.addElement("9");
			fontSizeListModel.addElement("10");
			fontSizeListModel.addElement("11");
			fontSizeListModel.addElement("12");
			fontSizeListModel.addElement("14");
			fontSizeListModel.addElement("16");
			fontSizeListModel.addElement("18");
			fontSizeListModel.addElement("20");
			fontSizeListModel.addElement("22");
			fontSizeListModel.addElement("24");
			fontSizeListModel.addElement("28");
			fontSizeListModel.addElement("32");
			fontSizeListModel.addElement("36");
			fontSizeListModel.addElement("40");
			fontSizeListModel.addElement("48");
			fontSizeListModel.addElement("72");
			fontSizeList.setSelectedValue(Integer.toString(initialFontSize), true);
			fontSizeList.addMouseListener(new MouseAdapter()
			{
				public void mouseClicked(MouseEvent evt) { updateSampleText(); }
			});
			gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = 1;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.anchor = java.awt.GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
	        getContentPane().add(fontSizePane, gbc);

	        // the sample text
			sampleText = new JLabel("The Electric VLSI Design System");
	        sampleText.setBorder(javax.swing.BorderFactory.createTitledBorder("Sample text"));
			gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 2;
			gbc.gridwidth = 2;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = java.awt.GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
	        getContentPane().add(sampleText, gbc);
	        sampleText.setFont(initialFont);

	        // the "OK" button
			JButton okButton = new JButton("OK");
			okButton.addActionListener(new java.awt.event.ActionListener()
			{
	            public void actionPerformed(java.awt.event.ActionEvent evt) { OK(); }
	        });
			gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 3;
			gbc.anchor = java.awt.GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
	        getContentPane().add(okButton, gbc);

	        // the "Cancel" button
	        JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(new java.awt.event.ActionListener()
			{
	            public void actionPerformed(java.awt.event.ActionEvent evt) { cancel(); }
	        });
			gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = 3;
			gbc.anchor = java.awt.GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
	        getContentPane().add(cancelButton, gbc);

	        pack();
	        addWindowListener(new java.awt.event.WindowAdapter()
	        {
	            public void windowClosing(java.awt.event.WindowEvent evt) { cancel(); }
	        });
			setVisible(true);
		}

		private void cancel()
		{
			dispose();
		}

		private void updateSampleText()
		{
			String currentFontName = initialFontName;
			if (fontNameList.getSelectedIndex() != -1) currentFontName = (String)fontNameList.getSelectedValue();
			int currentFontSize = initialFontSize;
			if (fontSizeList.getSelectedIndex() != -1) currentFontSize = TextUtils.atoi((String)fontSizeList.getSelectedValue());
			Font font = new Font(currentFontName, 0, currentFontSize);
			sampleText.setFont(font);
		}

		private void OK()
		{
			String currentFontName = initialFontName;
			if (fontNameList.getSelectedIndex() != -1) currentFontName = (String)fontNameList.getSelectedValue();
			int currentFontSize = initialFontSize;
			if (fontSizeList.getSelectedIndex() != -1) currentFontSize = TextUtils.atoi((String)fontSizeList.getSelectedValue());
			if (!currentFontName.equals(initialFontName) || currentFontSize != initialFontSize)
			{
				initialFont = new Font(currentFontName, 0, currentFontSize);
				info.setFont(initialFont);
				System.out.println("Messages window font is now " + currentFontName + ", size " + currentFontSize);
			}
			cancel();
		}
	}

}
