/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ManualViewer.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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

package com.sun.electric.tool.user.help;

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.user.dialogs.EDialog;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * A Dialog for displaying the Electric users manual.
 */
public class ManualViewer extends EDialog
{
    private JScrollPane rightHalf;
    private JEditorPane editorPane;
	private JSplitPane splitPane;
	private JTree optionTree;
	private DefaultMutableTreeNode rootNode;
	private HashMap pageTitle;
	private List pageSequence;
	private List pageURL;

    /**
     * Create a new user's manual dialog.
     * @param parent
     */
    public ManualViewer(java.awt.Frame parent)
    {
        super(parent, false);
        setTitle("User's Manual");
        init();

		// load the table of contents
		URL url = ManualViewer.class.getResource("helphtml/toc.txt");
		InputStream stream = TextUtils.getURLStream(url, null);
		InputStreamReader is = new InputStreamReader(stream);
		pageTitle = new HashMap();
		pageSequence = new ArrayList();
		pageURL = new ArrayList();
		DefaultMutableTreeNode [] stack = new DefaultMutableTreeNode[20];
		stack[0] = rootNode;
		for(;;)
		{
			String line = getLine(is);
			if (line == null) break;
			if (line.length() == 0) continue;
			int indent = 0;
			for(;;)
			{
				if (indent >= line.length() || line.charAt(indent) != ' ') break;
				indent++;
			}
			int titleStart = indent;
			int titleEnd = line.indexOf('=', titleStart);
			String fileName = null;
			if (titleEnd < 0) titleEnd = line.length(); else
				fileName = line.substring(titleEnd+1);
			String title = line.substring(titleStart, titleEnd).trim();

			if (fileName == null)
			{
				stack[indent+1] = new DefaultMutableTreeNode(title);
				stack[indent].add(stack[indent+1]);
			} else
			{
				DefaultMutableTreeNode node = new DefaultMutableTreeNode(new Integer(pageSequence.size()));
				stack[indent].add(node);
				pageTitle.put(fileName, title);
				pageSequence.add(fileName);
				URL theURL = ManualViewer.class.getResource("helphtml/" + fileName + ".html");
				if (theURL == null) System.out.println("NULL URL to "+fileName);
				pageURL.add(theURL);
			}
		}
		try
		{
			stream.close();
		} catch (IOException e)
		{
			System.out.println("Error closing file");
		}

		// pre-expand the tree
		TreePath topPath = optionTree.getPathForRow(0);
		optionTree.expandPath(topPath);
		topPath = optionTree.getPathForRow(1);
		optionTree.expandPath(topPath);

		// load the title page of the manual
        loadPage(0);
    }

	private String getLine(InputStreamReader is)
	{
		StringBuffer sb = new StringBuffer();
		for(;;)
		{
			int ch = -1;
			try
			{
				ch = is.read();
			} catch (IOException e) {}
			if (ch == -1) return null;
			if (ch == '\n' || ch == '\r') break;
			sb.append((char)ch);
		}
		return sb.toString();
	}

    private void loadPage(int index)
	{
		String fileName = (String)pageSequence.get(index);
		URL url = (URL)pageURL.get(index);
		InputStream stream = TextUtils.getURLStream(url, null);
		InputStreamReader is = new InputStreamReader(stream);
		StringBuffer sb = new StringBuffer();

		// emit header HTML
		sb.append("<BASE href=\"" + url.toString() + "\">");

		int lastIndex = index - 1;
		if (lastIndex < 0) lastIndex = pageSequence.size() - 1;
		String lastFileName = (String)pageSequence.get(lastIndex); 
		int nextIndex = index + 1;
		if (nextIndex >= pageSequence.size()) nextIndex = 0;
		String nextFileName = (String)pageSequence.get(nextIndex); 
		for(;;)
		{
			String line = getLine(is);
			if (line == null) break;
			if (line.startsWith("<!-- HEADER "))
			{
				int endPt = line.indexOf("-->");
				if (endPt < 0)
				{
					System.out.println("No end comment on line: "+line);
					continue;
				}
				String pageName = line.substring(12, endPt).trim();
				sb.append("<HTML><HEAD><TITLE>Using Electric " + pageName + "\"</TITLE></HEAD>\n");
				sb.append("<BODY BGCOLOR=\"#FFFFFF\">\n");
				sb.append("<!-- PAGE BREAK --><A NAME=\"" + fileName + "\"></A>\n");
				sb.append("<CENTER><TABLE WIDTH=\"90%\" BORDER=0><TR>\n");
				sb.append("<TD><CENTER><A HREF=\"" + lastFileName + ".html#" + lastFileName +
					".html\"><IMG SRC=\"iconplug.png\" ALT=\"plug\" BORDER=0></A></CENTER></TD>\n");
				sb.append("<TD><CENTER><H1>" + pageName + "</H1></CENTER></TD>\n");
				sb.append("<TD><CENTER><A HREF=\"" + nextFileName + ".html#" + nextFileName +
					".html\"><IMG SRC=\"iconplug.png\" ALT=\"plug\" BORDER=0></A></CENTER></TD></TR></TABLE></CENTER>\n");
				sb.append("<HR>\n");
				sb.append("<BR>\n");
				continue;
			}
			if (line.equals("<!-- TRAILER -->"))
			{
				sb.append("<P>\n");
				sb.append("<HR>\n");
				sb.append("<CENTER><TABLE BORDER=0><TR>\n");
				sb.append("<TD><A HREF=\"" + lastFileName + ".html#" + lastFileName +".html\"><IMG SRC=\"iconbackarrow.png\" ALT=\"Prev\" BORDER=0></A></TD>\n");
				sb.append("<TD><A HREF=\"" + lastFileName + ".html#" + lastFileName +".html\">Previous</A></TD>\n");
				sb.append("<TD>&nbsp;&nbsp;&nbsp;</TD>\n");
				sb.append("<TD><A HREF=\"index.html\"><IMG SRC=\"iconcontarrow.png\" ALT=\"Contents\" BORDER=0></A></TD>\n");
				sb.append("<TD><A HREF=\"index.html\">Table of Contents</A></TD>\n");
				sb.append("<TD>&nbsp;&nbsp;&nbsp;</TD>\n");
				sb.append("<TD><A HREF=\"" + nextFileName + ".html#" + nextFileName +".html\">Next</A></TD>\n");
				sb.append("<TD><A HREF=\"" + nextFileName + ".html#" + nextFileName +".html\"><IMG SRC=\"iconforearrow.png\" ALT=\"Next\" BORDER=0></A></TD>\n");
				sb.append("</TR></TABLE></CENTER>\n");
				sb.append("</BODY>\n");
				sb.append("</HTML>\n");
				continue;
			}
			sb.append(line);
			sb.append("\n");
		}
		try
		{
			stream.close();
		} catch (IOException e)
		{
			System.out.println("Error closing file");
		}
		editorPane.setText(sb.toString());
		editorPane.setCaretPosition(0);
    }

	private static class Hyperactive implements HyperlinkListener
	{
		private ManualViewer dialog;

		Hyperactive(ManualViewer dialog) { this.dialog = dialog; }

 		public void hyperlinkUpdate(HyperlinkEvent e)
		{
			 if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
			 {
				JEditorPane pane = (JEditorPane)e.getSource();
			 	if (e instanceof HTMLFrameHyperlinkEvent)
				{
					HTMLFrameHyperlinkEvent evt = (HTMLFrameHyperlinkEvent)e;
					HTMLDocument doc = (HTMLDocument)pane.getDocument();
					doc.processHTMLFrameHyperlinkEvent(evt);
			 	} else
			 	{
					URL desiredURL = e.getURL();

					// first see if it is one of the manual files, in which case it gets auto-generated
					String desiredFile = desiredURL.getFile();
			 		for(int i=0; i<dialog.pageURL.size(); i++)
			 		{
			 			URL url = (URL)dialog.pageURL.get(i);
			 			if (url.getFile().equals(desiredFile))
			 			{
			 				dialog.loadPage(i);
			 				return;
			 			}
			 		}

					// external URL: fetch it
					try
					{
						pane.setPage(desiredURL);
					} catch (Throwable t)
					{
						System.out.println("Cannot find URL "+e.getURL());
					}
			 	}
			}
		}
	}

    /**
     * Initialize list of all ToolTips and initilize components
     */
    private void init() {

        // set up dialog
        GridBagConstraints gbc;
        getContentPane().setLayout(new GridBagLayout());

		// setup tree pane for chapter selection (on the left)
		rootNode = new DefaultMutableTreeNode("Manual");
		DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
		optionTree = new ManualTree(treeModel, this);
		TreeHandler handler = new TreeHandler(this);
		optionTree.addMouseListener(handler);
		JScrollPane scrolledTree = new JScrollPane(optionTree);

		// the left side of the options dialog: a tree
		JPanel leftHalf = new JPanel();
		leftHalf.setLayout(new java.awt.GridBagLayout());
		gbc = new java.awt.GridBagConstraints();
		gbc.gridx = 0;      gbc.gridy = 0;
		gbc.gridwidth = 1;  gbc.gridheight = 1;
		gbc.fill = java.awt.GridBagConstraints.BOTH;
		gbc.weightx = 1.0;  gbc.weighty = 1.0;
		leftHalf.add(scrolledTree, gbc);
 
		// set up scroll pane for manual (on the right)
		editorPane = new JEditorPane();
		editorPane.setEditable(false);
		editorPane.setContentType("text/html");
		editorPane.addHyperlinkListener(new Hyperactive(this));
		rightHalf = new JScrollPane(editorPane);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		rightHalf.setPreferredSize(new Dimension(screenSize.width/2, screenSize.height*3/4));
		rightHalf.setMinimumSize(new Dimension(screenSize.width/4, screenSize.height/3));

		// build split pane with both halves
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setLeftComponent(leftHalf);
		splitPane.setRightComponent(rightHalf);
		splitPane.setDividerLocation(200);
		gbc = new java.awt.GridBagConstraints();
		gbc.gridx = 0;      gbc.gridy = 0;
		gbc.gridwidth = 1;  gbc.gridheight = 1;
		gbc.fill = java.awt.GridBagConstraints.BOTH;
		gbc.weightx = 1.0;  gbc.weighty = 1.0;
		getContentPane().add(splitPane, gbc);

        // close of dialog event
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt) { closeDialog(evt); }
        });

        pack();
    }

	private static class ManualTree extends JTree
	{
		private ManualViewer dialog;

		private ManualTree(DefaultTreeModel treeModel, ManualViewer dialog)
		{
			super(treeModel);
			this.dialog = dialog;

			// single selection as default
			getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

			// do not show top-level
			setRootVisible(true);
			setShowsRootHandles(true);

//			// enable tool tips - we'll use these to display useful info
//			ToolTipManager.sharedInstance().registerComponent(this);
		}

		public String convertValueToText(Object value, boolean selected, boolean expanded, boolean leaf,
			int row, boolean hasFocus)
		{
			Object nodeInfo = ((DefaultMutableTreeNode)value).getUserObject();
			if (nodeInfo instanceof Integer)
			{
				Integer index = (Integer)nodeInfo;
				String fileName = (String)dialog.pageSequence.get(index.intValue());
				String title = (String)dialog.pageTitle.get(fileName);
				return title;
			}
			return nodeInfo.toString();
		}
	}

	private static class TreeHandler implements MouseListener
	{
		private ManualViewer dialog;

		TreeHandler(ManualViewer dialog) { this.dialog = dialog; }

		public void mouseClicked(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {}

		public void mousePressed(MouseEvent e)
		{
			TreePath currentPath = dialog.optionTree.getPathForLocation(e.getX(), e.getY());
			if (currentPath == null) return;
			dialog.optionTree.setSelectionPath(currentPath);
			dialog.optionTree.expandPath(currentPath);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)currentPath.getLastPathComponent();
			Object obj = node.getUserObject();
			if (obj instanceof Integer)
			{
				int index = ((Integer)obj).intValue();
				dialog.loadPage(index);
			}
		}
	}

    private void closeDialog(java.awt.event.WindowEvent evt)
    {
        setVisible(false);
        dispose();
    }

}
