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
 * the Free Software Foundation; either version 3 of the License, or
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

import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.util.TextUtils;

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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;
import java.io.File;
import java.util.*;
import java.io.PrintWriter;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
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
	implements MouseListener, ClipboardOwner
{
	private JTextArea info;
	private Container contentFrame;
	private Container jf;

    private static boolean initialized = false;
    private static Font currentFont = null;
    private static final HashSet<MessagesWindow> messagesWindows = new HashSet<MessagesWindow>();

    private static final StringBuffer text = new StringBuffer();

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;
        if (!User.isDockMessagesWindow()) new MessagesWindow();
        appendString(ActivityLogger.getLoggingInformation());
    }

    public static Iterable<MessagesWindow> getMessagesWindows() {
        return messagesWindows;
    }

    public Container getContent() { return contentFrame; }

	public MessagesWindow()
	{
		Dimension scrnSize = TopLevel.getScreenSize();
		Dimension msgSize = new Dimension(scrnSize.width/3*2, scrnSize.height/100*15);
		Point msgPos = new Point(150, scrnSize.height/100*85);
		if (TopLevel.isMDIMode())
		{
			JInternalFrame jInternalFrame = new JInternalFrame("Electric Messages", true, false, true, true);
			jf = jInternalFrame;
			contentFrame = jInternalFrame.getContentPane();
			jInternalFrame.setFrameIcon(TopLevel.getFrameIcon());
			jf.setLocation(msgPos);
		} else if (!User.isDockMessagesWindow())
		{
			JFrame jFrame = new JFrame("Electric Messages");
			jf = jFrame;
			jFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			contentFrame = jFrame.getContentPane();
			jFrame.setIconImage(TopLevel.getFrameIcon().getImage());
			Point pt = User.getDefaultMessagesPos();
			if (pt == null) pt = msgPos;
			jf.setLocation(pt);
			Dimension override = User.getDefaultMessagesSize();
			if (override != null) jf.setPreferredSize(override);
        } else {
            contentFrame = new JPanel();
        }
		contentFrame.setLayout(new BorderLayout());

		info = new JTextArea(20, 110);
		info.setLineWrap(false);
		info.setFont(new Font("Monospaced", 0, 12));
		info.addMouseListener(this);
		JScrollPane scrollPane = new JScrollPane(info,
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setPreferredSize(msgSize);
		contentFrame.add(scrollPane, BorderLayout.CENTER);

		if (TopLevel.isMDIMode())
		{
			((JInternalFrame)jf).pack();
			TopLevel.addToDesktop((JInternalFrame)jf);
		} else if (!User.isDockMessagesWindow())
		{
			((JFrame)jf).pack();
			((JFrame)jf).setVisible(true);
        }

        if (currentFont==null)
            currentFont = info.getFont();
        if (User.isDockMessagesWindow()) appendStr(text.toString());
        messagesWindows.add(this);
	}

//	public Component getComponent()
//	{
//		return jf;
//	}

	/**
	 * Method to tell whether the Messages Window is the current window.
	 * @return true if the Messages Window is the current window.
	 */
	public boolean isFocusOwner()
	{
        if (jf==null) return false;
		if (TopLevel.isMDIMode())
			return ((JInternalFrame)jf).isSelected();
		return jf.isFocusOwner();
	}

    /**
     *  If a MessagesWindow has the focus, return it; else return null.
     */
	public static MessagesWindow getFocusOwner() {
        for (MessagesWindow mw : messagesWindows)
            if (mw.isFocusOwner())
                return mw;
        return null;
    }

	/**
	 * Method to request focus on the Messages Window.
	 */
	public void requestFocus()
	{
		if (!Job.isClientThread())
		{
			SwingUtilities.invokeLater(new Runnable() {
				public void run() { requestFocusUnsafe(); }
			});
			return;
		}
		requestFocusUnsafe();
	}

	private void requestFocusUnsafe()
	{
        if (jf==null) return;
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

	/**
	 * Method to return the bounds of the Messages Window.
	 * @return the bounds of the Messages Window.
	 */
	public Rectangle getMessagesLocation()
	{
        if (jf==null) return contentFrame.getBounds();
		return jf.getBounds();
	}

	/**
	 * Method to return the number of columns in the narrowest Messages Window.
	 * @return the number of columns in the narrowest Messages Window.
	 */
	public static int getMinMessagesCharWidth()
	{
        int min = Integer.MAX_VALUE;
        for (MessagesWindow mw : messagesWindows)
            min = Math.min(mw.info.getColumns(), min);
        return min;
	}

	/**
	 * Method to adjust the Messages Window so that it attaches to the current Edit Window.
	 */
	public static void tileWithEdit()
	{
        if (User.isDockMessagesWindow()) return;
        for (MessagesWindow mw : messagesWindows) {
            // get the location of the edit window
            WindowFrame wf = WindowFrame.getCurrentWindowFrame();
            if (wf == null) return;
            Rectangle eb;
            if (TopLevel.isMDIMode()) eb = wf.getInternalFrame().getBounds(); else
                eb = wf.getFrame().getBounds();
            
            // get the location of the messages window
            Rectangle mb = mw.getMessagesLocation();
            
            // adjust the messages window location and size
            mb.x = eb.x;
            mb.width = eb.width;
            mb.y = eb.y + eb.height;
            if (mw.jf!=null)
                mw.jf.setBounds(mb);
        }
	}

	/**
	 * Method to add text to the Messages Window.
	 * @param str the text to add.
	 */
	public static void appendString(String str)
	{
        if (messagesWindows.size()==0 && !User.isDockMessagesWindow()) {
            // Error before the message window is available. Sending error to std error output
            System.err.println(str);
            return;
        }
        if (User.isDockMessagesWindow()) text.append(str);
        for(MessagesWindow mw : messagesWindows)
            mw.appendStr(str);
    }
    private void appendStr(String str) {
		info.append(str);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() { info.setCaretPosition(info.getDocument().getLength()); }
		});
//		info.setCaretPosition(info.getDocument().getLength());

//		try
//		{
//			Rectangle r = info.modelToView(info.getDocument().getLength());
//			info.scrollRectToVisible(r);
//		} catch (BadLocationException ble)
//		{
//		}
	}

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
        if (Job.getDebug())
        {
            menuItem = new JMenuItem("Save All");
            menu.add(menuItem);
            menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { saveAll(); } });
        }
        menuItem = new JMenuItem("Clear");
		menu.add(menuItem);
		menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { clear(true); } });

		menu.show((Component)e.getSource(), e.getX(), e.getY());
	}

	/**
	 * Method to paste text from the clipboard to the Messages Window.
	 */
	public void pasteText()
	{
		info.paste();
	}

	/**
	 * Method to copy text from the Messages Window.
	 * @param all true to copy ALL text in the Messages Window; false to copy only the selected text.
	 * @param cut true to cut instead of copy (delete after copying).
	 */
	public void copyText(boolean all, boolean cut)
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

    /**
	 * Method to copy text from the Messages Window.
	 */
	public void saveAll()
	{
        String fileName = OpenFile.chooseOutputFile(FileType.TEXT, null, "Message");
        if (fileName == null) return; // cancel

        URL libURL = TextUtils.makeURLToFile(fileName);
        File f = new File(libURL.getPath());
        try
        {
            System.out.println("Saving console messages in '" + fileName + " '");
            PrintWriter wr = new PrintWriter(f);
            wr.print(info.getText());
            wr.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
	}

    /**
	 * Method to erase everything in the Messages Window.
	 * @param all true to delete all text; false to delete only selected text.
	 */
	public void clear(boolean all)
	{
		if (all)
		{
			info.setText("");
		} else
		{
			info.replaceSelection("");
		}
	}

    public static void clearAll() {
        text.setLength(0);
        for (MessagesWindow mw : messagesWindows)
            mw.clear(true); 
    }

	/**
	 * Method to select all text in the Messages Window.
	 */
	public void selectAll()
	{
		info.selectAll();
	}

	/************************************ MESSAGES WINDOW FONT SETTING ************************************/

	/**
	 * Method to interactively select the messages window font.
	 */
	public static void selectFont()
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

	private static class FontSelectDialog extends EDialog
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
			initialFont = currentFont;
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
                for (MessagesWindow mw : messagesWindows)
                    mw.info.setFont(initialFont);
				System.out.println("Messages window font is now " + currentFontName + ", size " + currentFontSize);
			}
			cancel();
		}
	}

}
