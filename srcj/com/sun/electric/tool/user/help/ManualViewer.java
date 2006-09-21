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

import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.menus.EMenu;
import com.sun.electric.tool.user.menus.EMenuBar;
import com.sun.electric.tool.user.menus.EMenuItem;
import com.sun.electric.tool.user.menus.FileMenu;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * A Dialog for displaying the Electric users manual.
 * 
 * The html files in the user's manual have special lines that control them:
 * <!-- HEADER 1-2: Chapter Title -->
 * <!-- COMMAND Menu/Command -->
 * <!-- PREFERENCE Section/Panel -->
 * <!-- PROJECTSETTING Panel -->
 * <!-- NEED 2in -->
 * <!-- TRAILER -->
 */
public class ManualViewer extends EDialog
{
	private static final String RUSSIANMANUALPATH = "plugins/manualRussian";

	/** the menus that are not checked */
	private static Set<String> excludeMenu = new HashSet<String>();
	static
	{
		excludeMenu.add("Sun");
		excludeMenu.add("Test");
		excludeMenu.add("Steve");
		excludeMenu.add("Russell");
		excludeMenu.add("JonG");
		excludeMenu.add("Gilda");
		excludeMenu.add("Dima");
		excludeMenu.add("Kon");
	}

	private static class PageInfo
	{
		String title;
		String fileName;
		String chapterName;
		String fullChapterNumber;
		int chapterNumber;
		int sectionNumber;
		URL url;
		int level;
		boolean newAtLevel;
	};

	private Class htmlBaseClass;
	private String htmlDirectory;
    private JScrollPane rightHalf;
    private JEditorPane editorPane;
	private JSplitPane splitPane;
	private JTextField searchField;
	private JTree optionTree;
	private DefaultMutableTreeNode rootNode;
	private List<PageInfo> pageSequence;
	private int currentIndex;
	private boolean menubarShown = false;
	private static int lastPageVisited = 0;
	private static HashMap<String,String> menuMap = null;
	private static HashMap<String,String> preferenceMap = null;
	private List<Object> history = new ArrayList<Object>();
	private static ManualViewer theManual = null;

	/**
	 * Method to display the user's manual.
	 */
	public static void userManualCommand()
	{
		if (theManual == null)
		{
			theManual = new ManualViewer(TopLevel.getCurrentJFrame(), null, ManualViewer.class, "helphtml");
		}
		theManual.setVisible(true);
	}

	/**
	 * Method to tell whether there is a Russian user's manual installed.
	 * @return true if the Russian user's manual is available.
	 */
	public static boolean hasRussianManual()
	{
		URL url = Main.class.getResource(RUSSIANMANUALPATH + "/toc.txt");
		return url != null;
	}

	/**
	 * Method to display the Russian user's manual.
	 */
	public static void userManualRussianCommand()
	{
		if (theManual == null)
		{
			theManual = new ManualViewer(TopLevel.getCurrentJFrame(), null, Main.class, RUSSIANMANUALPATH);
		}
		theManual.setVisible(true);
	}

	/**
	 * Method to show the help page for a particular panel in the "Preferences" dialog.
	 * @param preference the panel name, of the form "section/panel".
	 * For example, the "CIF" panel in the "I/O" section will be named "I/O/CIF".
	 */
	public static void showPreferenceHelp(String preference)
	{
		showSettingHelp("PREF", preference);
	}

	/**
	 * Method to show the help page for a particular panel in the "Project Settings" dialog.
	 * @param preference the panel name, of the form "section/panel".
	 * For example, the "CIF" panel in the "I/O" section will be named "I/O/CIF".
	 */
	public static void showProjectSettingHelp(String preference)
	{
		showSettingHelp("PROJ", preference);
	}

	/**
	 * Internal method to show Preferences or Project Settings help.
	 * @param str the help page requested.
	 */
	private static void showSettingHelp(String dialog, String str)
	{
		if (theManual == null)
		{
			theManual = new ManualViewer(TopLevel.getCurrentJFrame(), dialog+str, ManualViewer.class, "helphtml");
		} else
		{
			if (str != null)
			{
				String prefFileName = preferenceMap.get(dialog+str);
			    if (prefFileName == null)
			    {
			    	Job.getUserInterface().showErrorMessage("No help for " + str + " settings", "Missing documentation");
			    } else
				{
					for(int i=0; i<theManual.pageSequence.size(); i++)
					{
						PageInfo pi = theManual.pageSequence.get(i);
						if (pi.fileName.equals(prefFileName))
						{
							theManual.loadPage(i);
							break;
						}
					}
				}
			}
		}
		theManual.setVisible(true);
	}

    /**
     * Method to open the 2D view of a given layout cell
     * @param fileName name of the library where the cell is stored
     * @param cellName cell name
     * @param menuName name of the menu executing this command
     */
    public static Cell open2DSample(String fileName, String cellName, String menuName)
    {
        Library library = Library.findLibrary(fileName);
        if (library == null)
        {
            System.out.println("Load first the library '" + fileName +
                    "' (Help -> " + menuName + " -> Load Library)");
            return null;
        }
        Cell cell = library.findNodeProto(cellName);
        if (cell == null)
        {
            System.out.println("Cell '" + cellName + "' not found");
            return null;
        }
        // Open the window frame if not available
        if (cell != WindowFrame.getCurrentCell())
            WindowFrame.createEditWindow(cell);
        return cell;
    }

    /**
     * Method to open the 3D view of a given layout cell
     * @param fileName name of the library where the cell is stored
     * @param cellName cell name
     */
    public static void open3DSample(String fileName, String cellName, String menuName)
    {
        Cell cell = open2DSample(fileName, cellName, menuName);

        if (cell == null) return; // error opening the 2D view

        // Making sure all cell instances are expanded
        for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext();)
        {
            NodeInst ni = it.next();
            ni.setExpanded();
        }
        // to guarantee the redisplay with extended 

        Class plugin3D = Resources.get3DClass("ui.J3DMenu");
        if (plugin3D != null)
        {
            // Adding 3D/Demo menu
            try {
                Method createMethod = plugin3D.getDeclaredMethod("create3DViewCommand", new Class[] {Boolean.class});
                createMethod.invoke(plugin3D, new Object[] {new Boolean(false)});
            } catch (Exception e)
            {
                System.out.println("Can't open 3D view: " + e.getMessage());
                ActivityLogger.logException(e);
            }
        }
    }

    /**
     * Method to animate the 3D view of current layout cell
     * @param demoName name of j3d file containing the demo
     */
    public static void animate3DSample(String demoName)
    {
        String fileName = "helphtml/" + demoName;
        URL url = ManualViewer.class.getResource(fileName);
        if (url == null)
        {
            System.out.println("Can't open 3D demo file '" + fileName + "'");
            return;
        }
        Class plugin3D = Resources.get3DClass("ui.J3DDemoDialog");
        if (plugin3D != null)
        {
            // Adding 3D/Demo menu
            try {
                Method createMethod = plugin3D.getDeclaredMethod("create3DDemoDialog",
                        new Class[] {java.awt.Frame.class, URL.class});
                createMethod.invoke(plugin3D, new Object[] {TopLevel.getCurrentJFrame(), url});
            } catch (Exception e)
            {
                System.out.println("Can't open 3D demo dialog: " + e.getMessage());
                ActivityLogger.logException(e);
            }
        }
    }

    /**
     * Method to load a sample library from the lib area.
     * @param fileName library name
     */
	public static void loadSamplesLibrary(String fileName, String cellName)
	{
        Library lib = Library.findLibrary(fileName);
		if (lib != null)
        {
            System.out.println(lib + " already loaded");
            Cell cell = lib.findNodeProto(cellName);
            if (cell == null)
                System.out.println("Cell '" + cellName + "' does not exist in " + lib);
            return;
        }
		URL url = ManualViewer.class.getResource("helphtml/"+fileName+".jelib");
		new FileMenu.ReadLibrary(url, FileType.JELIB, null, cellName);
	}

	/**
     * Create a new user's manual dialog.
     * @param parent
     */
    private ManualViewer(Frame parent, String preference, Class baseClass, String htmlDir)
    {
        super(parent, false);
        htmlBaseClass = baseClass;
        htmlDirectory = htmlDir;
        setTitle("User's Manual");
        init();

        // load indices
        loadPointers();
        String prefFileName = null;
        if (preference != null)
        {
            prefFileName = preferenceMap.get(preference);
		    if (prefFileName == null)
		    	Job.getUserInterface().showErrorMessage("No help for " + preference + " settings", "Missing documentation");
        }

		// load the table of contents
        String indexName = htmlDirectory + "/toc.txt";
		URL url = htmlBaseClass.getResource(indexName);
		InputStream stream = TextUtils.getURLStream(url, null);
        if (stream == null)
        {
            System.out.println("Can't open " + indexName + " in " + htmlBaseClass.getPackage());
            return;
        }
		InputStreamReader is = new InputStreamReader(stream);
		pageSequence = new ArrayList<PageInfo>();
		DefaultMutableTreeNode [] stack = new DefaultMutableTreeNode[20];
		stack[0] = rootNode;
		boolean newAtLevel = false;
		String chapterName = null;
		int chapterNumber = 0;
		int [] sectionNumbers = new int[5];
		sectionNumbers[0] = -1;
		currentIndex = lastPageVisited;
        DefaultMutableTreeNode thisNode = null;
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
			if (titleEnd < 0) titleEnd = line.length();
            else
				fileName = line.substring(titleEnd+1).trim();
			String title = line.substring(titleStart, titleEnd).trim();

			if (fileName == null)
			{
				if (indent == 0)
				{
					chapterNumber++;
					chapterName = chapterNumber + ": " + title;
				}
				sectionNumbers[indent]++;
				stack[indent+1] = new DefaultMutableTreeNode(sectionNumbers[indent] + ": " + title);
				stack[indent].add(stack[indent+1]);
				sectionNumbers[indent+1] = 0;
				newAtLevel = true;
			} else
			{
				PageInfo pi = new PageInfo();
				pi.fileName = fileName;
				pi.title = title;
				pi.chapterName = chapterName;
				pi.chapterNumber = chapterNumber;
				pi.sectionNumber = ++sectionNumbers[indent];
				pi.fullChapterNumber = "";
				for(int i=0; i<indent; i++)
				{
					pi.fullChapterNumber += sectionNumbers[i] + "-";
				}
				pi.fullChapterNumber += sectionNumbers[indent];
				pi.level = indent;
				pi.newAtLevel = newAtLevel;
				pi.url = htmlBaseClass.getResource(htmlDirectory + "/" + fileName + ".html");
				if (pi.url == null)
                    System.out.println("NULL URL to "+fileName);
				DefaultMutableTreeNode node = new DefaultMutableTreeNode(new Integer(pageSequence.size()));
                if (preference != null && pi.fileName.equals(prefFileName))
                {
                    currentIndex = pageSequence.size();
                    thisNode = node;
                }
				stack[indent].add(node);
				pageSequence.add(pi);
				newAtLevel = false;
			}
		}
		try
		{
			stream.close();
		} catch (IOException e)
		{
			System.out.println("Error closing file");
		}

        // No preference page given
        if (preference == null)
        {
            // pre-expand the tree
            TreePath topPath = optionTree.getPathForRow(0);
            optionTree.expandPath(topPath);
            topPath = optionTree.getPathForRow(1);
            optionTree.expandPath(topPath);
        }
        else
        {
            optionTree.scrollPathToVisible(new TreePath(thisNode.getPath()));
        }
		// load the title page of the manual
        loadPage(currentIndex);
    }

	/**
	 * Method to show the menu bar in the manual dialog.
	 */
	private void loadMenuBar()
	{
		if (menubarShown) return;
		menubarShown = true;

		JMenuBar helpMenuBar = new JMenuBar();

		// convert menuBar to tree
		TopLevel top = (TopLevel)TopLevel.getCurrentJFrame();
		EMenuBar menuBar = top.getEMenuBar();
		for (EMenuItem menu: menuBar.getItems())
		{
			JMenu helpMenu = new JMenu(menu.getText());
			helpMenuBar.add(helpMenu);
			addMenu((EMenu)menu, helpMenu, menu.getText() + "/");
		}
		setJMenuBar(helpMenuBar);
		pack();

		StringBuffer sb = new StringBuffer();
		sb.append("<CENTER><H1>HELP MENU ENABLED</H1></CENTER>\n");
		sb.append("The menu bar at the top of <I>this</I> window looks the same as the main menu bar in Electric.<BR><BR>\n");
		sb.append("Use any entry in this menu bar to see the manual page that explains that menu entry.\n");
		editorPane.setText(sb.toString());
		editorPane.setCaretPosition(0);
	}

	/**
	 * Method to examine the online manual and extract pointers to commands and preferences.
	 */
	private void loadPointers()
	{
		// stop if already done
		if (preferenceMap != null) return;

		menuMap = new HashMap<String,String>();
		HashMap<String,String> menuMapCheck = null;
		preferenceMap = new HashMap<String,String>();
		if (Job.getDebug())
		{
			menuMapCheck = new HashMap<String,String>();
		}

		// scan all manual entries for menu associations
        String indexName = htmlDirectory + "/toc.txt";
		URL url = htmlBaseClass.getResource(indexName);
		InputStream stream = TextUtils.getURLStream(url, null);
        if (stream == null)
        {
            System.out.println("Can't open " + indexName + " in " + htmlBaseClass.getPackage());
            return;
        }
		InputStreamReader is = new InputStreamReader(stream);
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
			if (titleEnd < 0) continue;
			String fileName = line.substring(titleEnd+1).trim();

			URL pageURL = htmlBaseClass.getResource(htmlDirectory + "/" + fileName + ".html");
			if (pageURL == null)
			{
				System.out.println("NULL URL to "+fileName);
				continue;
			}
			InputStream pageStream = TextUtils.getURLStream(pageURL, null);
			InputStreamReader pageIS = new InputStreamReader(pageStream);
			for(;;)
			{
				String pageLine = getLine(pageIS);
				if (pageLine == null) break;
				if (pageLine.startsWith("<!-- COMMAND "))
				{
					int endPt = pageLine.indexOf("-->");
					if (endPt < 0)
					{
						System.out.println("No end comment on line: "+pageLine);
						continue;
					}
					String commandName = pageLine.substring(13, endPt).trim();
					for(;;)
					{
						int backslashPos = commandName.indexOf('\\');
						if (backslashPos < 0) break;
						commandName = commandName.substring(0, backslashPos) + commandName.substring(backslashPos+1);
					}
					String already = menuMap.get(commandName);
					if (already != null && Job.getDebug())
					{
						System.out.println("ERROR: command " + commandName + " is keyed to both " + already + " and " + fileName);
					}
					menuMap.put(commandName, fileName);
					if (menuMapCheck != null) menuMapCheck.put(commandName, fileName);
					continue;
				}
				if (pageLine.startsWith("<!-- PREFERENCE "))
				{
					int endPt = pageLine.indexOf("-->");
					if (endPt < 0)
					{
						System.out.println("No end comment on line: "+pageLine);
						continue;
					}
					String preferenceName = "PREF" + pageLine.substring(16, endPt).trim();
					String already = preferenceMap.get(preferenceName);
					if (already != null && Job.getDebug())
					{
						System.out.println("ERROR: command " + preferenceName + " is keyed to both " + already + " and " + fileName);
					}
					preferenceMap.put(preferenceName, fileName);
					continue;
				}
				if (pageLine.startsWith("<!-- PROJECTSETTING "))
				{
					int endPt = pageLine.indexOf("-->");
					if (endPt < 0)
					{
						System.out.println("No end comment on line: "+pageLine);
						continue;
					}
					String preferenceName = "PROJ" + pageLine.substring(20, endPt).trim();
					String already = preferenceMap.get(preferenceName);
					if (already != null && Job.getDebug())
					{
						System.out.println("ERROR: command " + preferenceName + " is keyed to both " + already + " and " + fileName);
					}
					preferenceMap.put(preferenceName, fileName);
					continue;
				}
			}
			try
			{
				pageStream.close();
			} catch (IOException e)
			{
				System.out.println("Error closing file");
			}
		}
		if (menuMapCheck != null)
		{
			TopLevel top = (TopLevel)TopLevel.getCurrentJFrame();
			EMenuBar menuBar = top.getEMenuBar();
			for (EMenuItem menu: menuBar.getItems())
			{
				if (Job.getDebug() && excludeMenu.contains(menu.getText())) continue;
				checkMenu((EMenu)menu, menu.getText() + "/", menuMapCheck);
			}

			for(String commandName : menuMapCheck.keySet())
			{
				String fileName = menuMapCheck.get(commandName);
				System.out.println("Command " + commandName + " was mentioned in file " + fileName + " but does not exist");
			}
			menuMapCheck = null;
		}
	}

	private void checkMenu(EMenu menu, String cumulative, HashMap<String,String> menuMapCheck)
	{
		for (EMenuItem menuItem: menu.getItems())
		{
			if (menuItem == EMenuItem.SEPARATOR) continue;
			if (menuItem instanceof EMenu)
			{
				EMenu subMenu = (EMenu)menuItem;
				checkMenu(subMenu, cumulative + subMenu.getText() + "/", menuMapCheck);
			} else
			{
				String commandName = cumulative + menuItem.getText();
				String fileName = menuMap.get(commandName);
				if (fileName == null && Job.getDebug())
				{
					System.out.println("No help for " + commandName);
				} else
				{
					if (menuMapCheck != null) menuMapCheck.remove(commandName);
				}
			}
		}
	}

	private void addMenu(EMenu menu, JMenu helpMenu, String cumulative)
	{
		for (EMenuItem menuItem: menu.getItems())
		{
			if (menuItem == EMenuItem.SEPARATOR)
			{
				helpMenu.addSeparator();
				continue;
			}
			if (menuItem instanceof EMenu)
			{
				EMenu subMenu = (EMenu)menuItem;
				JMenu helpSubMenu = new JMenu(subMenu.getText());
				helpMenu.add(helpSubMenu);
				addMenu(subMenu, helpSubMenu, cumulative + subMenu.getText() + "/");
			} else
			{
				JMenuItem helpMenuItem = new JMenuItem(menuItem.getText());
				helpMenu.add(helpMenuItem);
				String commandName = cumulative + menuItem.getText();
				String fileName = menuMap.get(commandName);
				helpMenuItem.addActionListener(new HelpMenuActionListener(this, fileName));
			}
		}
	}

	private static class HelpMenuActionListener implements ActionListener
	{
		private ManualViewer dialog;
		private String title;

		HelpMenuActionListener(ManualViewer dialog, String title)
		{
			this.dialog = dialog;
			this.title = title;
		}

		public void actionPerformed(ActionEvent e) { doHelpMenu(title); }

		private void doHelpMenu(String fileName)
		{
			if (fileName == null)
			{
				System.out.println("No help for this command");
				return;
			}
			for(int i=0; i<dialog.pageSequence.size(); i++)
			{
				PageInfo pi = dialog.pageSequence.get(i);
				if (pi.fileName.equals(fileName))
				{
					dialog.loadPage(i);
					return;
				}
			}
		}
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
		// add to browsing history
		history.add(new Integer(index));

		currentIndex = index;
		lastPageVisited = index;
		PageInfo pi = pageSequence.get(index);
        if (pi.url == null) return; // error reading the html file
        
		InputStream stream = TextUtils.getURLStream(pi.url, null);
        InputStreamReader is;
        try {
            is = new InputStreamReader(stream, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.out.println("UTF-8 is UnsupportedEncodingException");
            return;
        }
		StringBuffer sb = new StringBuffer();

		// emit header HTML
		sb.append("<BASE href=\"" + pi.url.toString() + "\">");

		int lastIndex = index - 1;
		if (lastIndex < 0) lastIndex = pageSequence.size() - 1;
		PageInfo lastPi = pageSequence.get(lastIndex);
		String lastFileName = lastPi.fileName;
		int nextIndex = index + 1;
		if (nextIndex >= pageSequence.size()) nextIndex = 0;
		PageInfo nextPi = pageSequence.get(nextIndex);
		String nextFileName = nextPi.fileName;
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
				sb.append("<BODY>\n");
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

	/**
	 * Method to go to the previous page that was viewed.
	 */
	private void back()
	{
		int len = history.size();
		if (len <= 1) return;
		Object lastPage = history.get(len-2);
		history.remove(len-1);
		if (lastPage instanceof Integer)
		{
			history.remove(len-2);
			Integer lpi = (Integer)lastPage;
			loadPage(lpi.intValue());
			return;
		}
		if (lastPage instanceof String)
		{
			editorPane.setText((String)lastPage);
			editorPane.setCaretPosition(0);
		}
	}

	/**
	 * Method to go to the previous page in the manual.
	 */
	private void prev()
	{
		int index = currentIndex - 1;
		if (index < 0) index = pageSequence.size() - 1;
		loadPage(index);
	}

	/**
	 * Method to go to the next page in the manual.
	 */
	private void next()
	{
		int index = currentIndex + 1;
		if (index >= pageSequence.size()) index = 0;
		loadPage(index);
	}

	private void search()
	{
		String ret = searchField.getText().trim();
		if (ret.length() == 0) return;
		Pattern pattern = Pattern.compile(ret, Pattern.CASE_INSENSITIVE);

		StringBuffer sbResult = new StringBuffer();

		sbResult.append("<CENTER><H1>Search Results for " + ret + "</H1></CENTER>\n");
		int numFound = 0;
		for(int index=0; index < pageSequence.size(); index++)
		{
			PageInfo pi = pageSequence.get(index);
			InputStream stream = TextUtils.getURLStream(pi.url, null);
			InputStreamReader is = new InputStreamReader(stream);
			StringBuffer sb = new StringBuffer();
			for(;;)
			{
				String line = getLine(is);
				if (line == null) break;
				if (line.length() == 0) continue;
				if (line.equals("<!-- TRAILER -->")) continue;
				if (line.startsWith("<!-- COMMAND ")) continue;
				if (line.startsWith("<!-- NEED ")) continue;
				if (line.startsWith("<!-- HEADER ")) line = line.substring(12);
				sb.append(line);
			}
			Matcher matcher = pattern.matcher(sb.toString());
			if (!matcher.find()) continue;
			sbResult.append("<B><A HREF=\"" + pi.fileName + ".html\">" + pi.fullChapterNumber + ": " + pi.title + "</A></B<BR>\n");
			numFound++;
		}
		sbResult.append("<P><B>Found " + numFound + " entries</B>\n");
		String wholePage = sbResult.toString();
		history.add(wholePage);
		editorPane.setText(wholePage);
		editorPane.setCaretPosition(0);
	}

	/**
	 * Method to generate a 1-page HTML file with the entire manual.
	 * This is an advanced function that is not available to users.
	 * The purpose of the single-file HTML is to generate an acrobat file (pdf).
	 */
	private void manual1Page()
	{
		String manualFileName = OpenFile.chooseOutputFile(FileType.HTML, "Manual file", "electric.html");
		if (manualFileName == null) return;
		PrintWriter printWriter = null;
		try
		{
			printWriter = new PrintWriter(new BufferedWriter(new FileWriter(manualFileName)));
		} catch (IOException e)
		{
			System.out.println("Error creating " + manualFileName);
			return;
		}

		printWriter.println("<HTML><HEAD><TITLE>Using The Electric VLSI Design System</TITLE></HEAD>");
		printWriter.println("<BODY BGCOLOR=\"#FFFFFF\">");
		for(int index=0; index < pageSequence.size(); index++)
		{
			PageInfo pi = pageSequence.get(index);
			InputStream stream = TextUtils.getURLStream(pi.url, null);
			InputStreamReader is = new InputStreamReader(stream);

			int lastIndex = index - 1;
			if (lastIndex < 0) lastIndex = pageSequence.size() - 1;
			PageInfo lastPi = pageSequence.get(lastIndex);
			String lastFileName = lastPi.fileName;
			int nextIndex = index + 1;
			if (nextIndex >= pageSequence.size()) nextIndex = 0;
			PageInfo nextPi = pageSequence.get(nextIndex);
			String nextFileName = nextPi.fileName;

			for(;;)
			{
				String line = getLine(is);
				if (line == null) break;
				if (line.length() == 0) continue;
				if (line.startsWith("<!-- HEADER "))
				{
					int endPt = line.indexOf("-->");
					if (endPt < 0)
					{
						System.out.println("No end comment on line: "+line);
						continue;
					}
					String pageName = line.substring(12, endPt).trim();
					if (pi.level < 2 || pi.newAtLevel)
					{
						if (pi.chapterNumber > 0 && lastPi.chapterNumber < pi.chapterNumber)
						{
							printWriter.println("<HR>");
							printWriter.println("<CENTER><H1><A NAME=\"" + pi.fileName + "\">Chapter " + pi.chapterName + "</A></H1></CENTER>");
						} else
						{
							printWriter.println("<!-- PAGE BREAK --><A NAME=\"" + pi.fileName + "\"></A>");
							printWriter.println("<CENTER><FONT SIZE=6><B>Chapter " + pi.chapterName + "</B></FONT></CENTER>");
						}
						printWriter.println("<CENTER><TABLE WIDTH=\"90%\" BORDER=0><TR>");
						printWriter.println("<TD><CENTER><A HREF=\"" + lastFileName + ".html#" + lastFileName +
							".html\"><IMG SRC=\"iconplug.png\" ALT=\"plug\" BORDER=0></A></CENTER></TD>");
						printWriter.println("<TD><CENTER><H2>" + pageName + "</H2></CENTER></TD>");
						printWriter.println("<TD><CENTER><A HREF=\"" + nextFileName + ".html#" + nextFileName +
							".html\"><IMG SRC=\"iconplug.png\" ALT=\"plug\" BORDER=0></A></CENTER></TD></TR></TABLE></CENTER>");
						printWriter.println("<HR>");
						printWriter.println("<BR>");
					} else
					{
						printWriter.println("<H3>" + pageName + "</H3>");
					}
					continue;
				}
				if (line.equals("<!-- TRAILER -->")) continue;
				printWriter.println(line);
			}
		}
		printWriter.println("</BODY>");
		printWriter.println("</HTML>");
		printWriter.close();
	}

	/**
	 * Method to generate a multi-page HTML file with the entire manual.
	 * This is an advanced function that is not available to users.
	 * The output is a single "index" file, and many chapter files that start with the letter "m"
	 * (i.e. "mchap01-01.html").
	 * If you copy the "index.html", all of the "mchap" files, and all of the image files,
	 * it will be a complete manual.
	 */
	private void manualManyPages()
	{
		String manualFileName = OpenFile.chooseOutputFile(FileType.HTML, "Manual file", "index.html");
		if (manualFileName == null) return;
		PrintWriter printWriter = null;
		try
		{
			printWriter = new PrintWriter(new BufferedWriter(new FileWriter(manualFileName)));
		} catch (IOException e)
		{
			System.out.println("Error creating " + manualFileName);
			return;
		}
		System.out.println("Writing 'index.html' and many files starting with 'mchap'");

		// gather the table of contents by chapter
		int lastChapterNumber = 0;
		StringBuffer chapterText = new StringBuffer();
		List<String> chapters = new ArrayList<String>();
		StringBuffer afterTOC = new StringBuffer();
		for(int index=0; index < pageSequence.size(); index++)
		{
			PageInfo pi = pageSequence.get(index);
			if (pi.chapterNumber <= 0)
			{
				InputStream stream = TextUtils.getURLStream(pi.url, null);
				InputStreamReader is = new InputStreamReader(stream);
				for(;;)
				{
					String line = getLine(is);
					if (line == null) break;
					if (line.length() == 0) continue;
					if (line.startsWith("<!-- HEADER ")) continue;
					if (line.startsWith("<!-- TRAILER ")) continue;
					if (line.equals("<HR>")) break;
					printWriter.println(line);
				}
				for(;;)
				{
					String line = getLine(is);
					if (line == null) break;
					if (line.length() == 0) continue;
					if (line.startsWith("<!-- HEADER ")) continue;
					if (line.startsWith("<!-- TRAILER ")) continue;
					afterTOC.append(line);
				}
				try
				{
					is.close();
				} catch (IOException e) {}
				continue;
			}
			if (pi.chapterNumber != lastChapterNumber)
			{
				if (lastChapterNumber > 0) chapters.add(chapterText.toString());
				lastChapterNumber = pi.chapterNumber;
				chapterText = new StringBuffer();
				chapterText.append("<B>Chapter " + pi.chapterName.toUpperCase() + "</B><BR>");
			}
			chapterText.append("<A HREF=\"m" + pi.fileName + ".html\">" + pi.fullChapterNumber + ": " + pi.title + "</A><BR>");
		}
		chapters.add(chapterText.toString());

		// write the table of contents
		printWriter.println("<CENTER><H1>Table of Contents</H1></CENTER>");
		printWriter.println("<CENTER><TABLE BORDER=\"1\">");
		for(int i=0; i<chapters.size(); i += 2)
		{
			String leftSide = chapters.get(i);
			String rightSide = "";
			if (i+1 < chapters.size()) rightSide = chapters.get(i+1);
			printWriter.println("<TR><TD VALIGN=TOP>" + leftSide + "</TD>");
			printWriter.println("<TD VALIGN=TOP>" + rightSide + "</TD></TR>");
		}
		printWriter.println("</TABLE></CENTER><HR>");

		printWriter.print(afterTOC.toString());
		printWriter.println("<CENTER><TABLE BORDER=\"0\"><TR>");
		printWriter.println("<TD><A HREF=\"mchap01-01.html\">Next</A></TD>");
		printWriter.println("<TD><A HREF=\"mchap01-01.html\"><IMG SRC=\"iconforearrow.png\" ALT=\"Next\" BORDER=\"0\"></A></TD>");
		printWriter.println("</TR></TABLE></CENTER>");
		printWriter.println("</BODY>");
		printWriter.println("</HTML>");
		printWriter.close();

		for(int index=0; index < pageSequence.size(); index++)
		{
			PageInfo pi = pageSequence.get(index);
			if (pi.chapterNumber <= 0) continue;
			InputStream stream = TextUtils.getURLStream(pi.url, null);
			InputStreamReader is = new InputStreamReader(stream);
			String pageFileName = manualFileName;
			int lastSep = pageFileName.lastIndexOf('\\');
			if (lastSep >= 0) pageFileName = pageFileName.substring(0, lastSep+1);
			pageFileName += "m" + pi.fileName + ".html";
			try
			{
				printWriter = new PrintWriter(new BufferedWriter(new FileWriter(pageFileName)));
			} catch (IOException e)
			{
				System.out.println("Error creating " + pageFileName);
				break;
			}

			int lastIndex = index - 1;
			if (lastIndex < 0) lastIndex = pageSequence.size() - 1;
			PageInfo lastPi = pageSequence.get(lastIndex);
			String lastFileName = lastPi.fileName;
			if (lastFileName.equals("title")) lastFileName = "index"; else lastFileName = "m" + lastFileName;
			int nextIndex = index + 1;
			if (nextIndex >= pageSequence.size()) nextIndex = 0;
			PageInfo nextPi = pageSequence.get(nextIndex);
			String nextFileName = nextPi.fileName;
			if (nextFileName.equals("title")) nextFileName = "index"; else nextFileName = "m" + nextFileName;

			for(;;)
			{
				String line = getLine(is);
				if (line == null) break;
				if (line.length() == 0) continue;
				if (line.startsWith("<!-- NEED ")) continue;
				if (line.startsWith("<!-- HEADER "))
				{
					int endPt = line.indexOf("-->");
					if (endPt < 0)
					{
						System.out.println("No end comment on line: "+line);
						continue;
					}
					String pageName = line.substring(12, endPt).trim();
					printWriter.println("<A NAME=\"" + pi.fileName + "\"></A>");
					printWriter.println("<CENTER><FONT SIZE=6><B>Chapter " + pi.chapterName + "</B></FONT></CENTER>");
					printWriter.println("<CENTER><TABLE WIDTH=\"90%\" BORDER=0><TR>");
					printWriter.println("<TD><CENTER><A HREF=\"" + lastFileName + ".html#" + lastFileName +
						".html\"><IMG SRC=\"iconplug.png\" ALT=\"plug\" BORDER=0></A></CENTER></TD>");
					printWriter.println("<TD><CENTER><H2>" + pageName + "</H2></CENTER></TD>");
					printWriter.println("<TD><CENTER><A HREF=\"" + nextFileName + ".html#" + nextFileName +
						".html\"><IMG SRC=\"iconplug.png\" ALT=\"plug\" BORDER=0></A></CENTER></TD></TR></TABLE></CENTER>");
					printWriter.println("<HR>");
					printWriter.println("<BR>");
					continue;
				}
				if (line.equals("<!-- TRAILER -->"))
				{
					printWriter.println("<P>");
					printWriter.println("<HR>");
					printWriter.println("<CENTER><TABLE BORDER=0><TR>");
					printWriter.println("<TD><A HREF=\"" + lastFileName + ".html#" + lastFileName +".html\"><IMG SRC=\"iconbackarrow.png\" ALT=\"Prev\" BORDER=0></A></TD>");
					printWriter.println("<TD><A HREF=\"" + lastFileName + ".html#" + lastFileName +".html\">Previous</A></TD>");
					printWriter.println("<TD>&nbsp;&nbsp;&nbsp;</TD>");
					printWriter.println("<TD><A HREF=\"index.html\"><IMG SRC=\"iconcontarrow.png\" ALT=\"Contents\" BORDER=0></A></TD>");
					printWriter.println("<TD><A HREF=\"index.html\">Table of Contents</A></TD>");
					printWriter.println("<TD>&nbsp;&nbsp;&nbsp;</TD>");
					printWriter.println("<TD><A HREF=\"" + nextFileName + ".html#" + nextFileName +".html\">Next</A></TD>");
					printWriter.println("<TD><A HREF=\"" + nextFileName + ".html#" + nextFileName +".html\"><IMG SRC=\"iconforearrow.png\" ALT=\"Next\" BORDER=0></A></TD>");
					printWriter.println("</TR></TABLE></CENTER>");
					continue;
				}

				// convert references to other chapters
				for(;;)
				{
					int x = line.indexOf("\"chap");
					if (x >= 0)
					{
						line = line.substring(0, x+1) + "m" + line.substring(x+1);
						continue;
					}
					x = line.indexOf("#chap");
					if (x >= 0)
					{
						line = line.substring(0, x+1) + "m" + line.substring(x+1);
						continue;
					}
					break;
				}

				// write the line
				printWriter.println(line);
			}
			printWriter.close();
		}
	}

	/**
	 * Method to edit the current page in the manual.
	 * This is an advanced function that is not available to users.
	 */
	private void edit()
	{
		PageInfo pi = pageSequence.get(currentIndex);
		EditHTML dialog = new EditHTML(TopLevel.getCurrentJFrame(), pi.url, this);
		dialog.setVisible(true);
	}

	private static class EditHTML extends EDialog
	{
		private JTextArea textArea;
		private URL file;
		private ManualViewer world;

		private EditHTML(Frame parent, URL file, ManualViewer world)
		{
			super(parent, false);
			this.file = file;
			this.world = world;
			getContentPane().setLayout(new GridBagLayout());

			setTitle("Edit HTML");
			setName("");
			Dimension maxScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
			setPreferredSize(new Dimension(maxScreenSize.width/2, maxScreenSize.height/2));
			addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent evt) { closeDialog(evt); }
			});

			textArea = new JTextArea();
			textArea.getDocument().putProperty( DefaultEditorKit.EndOfLineStringProperty, System.getProperty("line.separator") );
			JScrollPane scrollPane = new JScrollPane(textArea);
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 0;
			gridBagConstraints.weightx = 1;
			gridBagConstraints.weighty = 1;
			gridBagConstraints.anchor = GridBagConstraints.CENTER;
			gridBagConstraints.fill = GridBagConstraints.BOTH;
			gridBagConstraints.insets = new Insets(4, 4, 4, 4);
			getContentPane().add(scrollPane, gridBagConstraints);

			try
			{
				URLConnection con = file.openConnection();
				InputStream stream = con.getInputStream();
				InputStreamReader is = new InputStreamReader(stream);
				LineNumberReader ln = new LineNumberReader(is);
				StringBuffer sb = new StringBuffer();
				for(;;)
				{
					String aLine = ln.readLine();
					if (aLine == null) break;
					sb.append(aLine);
					sb.append('\n');
				}
				ln.close();
				textArea.setText(sb.toString());
				textArea.setSelectionStart(0);
				textArea.setSelectionEnd(0);
			} catch (IOException e)
			{
				System.out.println("Could not find file: " + file.getFile());
				return;
			}

			pack();
		}

		private void closeDialog(WindowEvent evt)
		{
			String fileName = file.getFile();
			try
			{
				PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
				printWriter.print(textArea.getText());
				printWriter.close();
			} catch (IOException e)
			{
				System.out.println("Could not save file: " + fileName);
				return;
			}

			/*
			 * Because some IDEs keep class files in a separate directory,
			 * the file that is being displayed is only a cache of the real one.
			 * The real one is missing the "/bin/" part.
			 */
			int binPos = fileName.indexOf("/bin/");
			if (binPos >= 0)
			{
				String otherFileName = fileName.substring(0, binPos) + "/srcj" + fileName.substring(binPos+4);
				try
				{
					PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(otherFileName)));
					printWriter.print(textArea.getText());
					printWriter.close();
					System.out.println("Also saved to "+otherFileName);
				} catch (IOException e)
				{
					System.out.println("Could not also save file: " + otherFileName);
					System.out.println("  but did save: " + fileName);
					return;
				}
			}

			setVisible(false);
			dispose();
			world.loadPage(world.currentIndex);
		}
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
			 		for(int i=0; i<dialog.pageSequence.size(); i++)
			 		{
			 			PageInfo pi = dialog.pageSequence.get(i);
			 			if (pi.url.getFile().equals(desiredFile))
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
    private void init()
    {
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
		leftHalf.setLayout(new GridBagLayout());
		gbc = new GridBagConstraints();
		gbc.gridx = 0;      gbc.gridy = 4;
		gbc.gridwidth = 2;  gbc.gridheight = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;  gbc.weighty = 1.0;
		gbc.insets = new Insets(0, 4, 4, 4);
		leftHalf.add(scrolledTree, gbc);
		 
		// forward and backward buttons
		JButton backButton = new JButton("Back");
		gbc = new GridBagConstraints();
		gbc.gridx = 0;      gbc.gridy = 0;
		gbc.gridwidth = 1;  gbc.gridheight = 1;
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.anchor = GridBagConstraints.CENTER;
		leftHalf.add(backButton, gbc);
		backButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { back(); }
		});
		JButton menuButton = new JButton("Menu Help");
		gbc = new GridBagConstraints();
		gbc.gridx = 0;      gbc.gridy = 1;
		gbc.gridwidth = 1;  gbc.gridheight = 1;
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.anchor = GridBagConstraints.CENTER;
		leftHalf.add(menuButton, gbc);
		menuButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { loadMenuBar(); }
		});

		// Previous and Next buttons
		JButton nextButton = new JButton("Next");
		gbc = new GridBagConstraints();
		gbc.gridx = 1;      gbc.gridy = 0;
		gbc.gridwidth = 1;  gbc.gridheight = 1;
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.anchor = GridBagConstraints.CENTER;
		leftHalf.add(nextButton, gbc);
		nextButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { next(); }
		});
		JButton prevButton = new JButton("Prev");
		gbc = new GridBagConstraints();
		gbc.gridx = 1;      gbc.gridy = 1;
		gbc.gridwidth = 1;  gbc.gridheight = 1;
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.anchor = GridBagConstraints.CENTER;
		leftHalf.add(prevButton, gbc);
		prevButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { prev(); }
		});

		JSeparator sep = new JSeparator();
		gbc = new GridBagConstraints();
		gbc.gridx = 0;      gbc.gridy = 2;
		gbc.gridwidth = 2;  gbc.gridheight = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		leftHalf.add(sep, gbc);

		// forward and backward buttons at the bottom of the left side
		searchField = new JTextField();
		gbc = new GridBagConstraints();
		gbc.gridx = 0;      gbc.gridy = 3;
		gbc.gridwidth = 1;  gbc.gridheight = 1;
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.weightx = 0.5;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.CENTER;
		leftHalf.add(searchField, gbc);
		JButton searchButton = new JButton("Find");
		gbc = new GridBagConstraints();
		gbc.gridx = 1;      gbc.gridy = 3;
		gbc.gridwidth = 1;  gbc.gridheight = 1;
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.anchor = GridBagConstraints.CENTER;
		leftHalf.add(searchButton, gbc);
		searchButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt) { search(); }
		});
        getRootPane().setDefaultButton(searchButton);

		if (Job.getDebug())
		{
			// manual and edit buttons at the bottom of the left side
			JButton manualButton = new JButton("1-Page Man");
			gbc = new GridBagConstraints();
			gbc.gridx = 1;      gbc.gridy = 5;
			gbc.gridwidth = 1;  gbc.gridheight = 1;
			gbc.insets = new Insets(0, 4, 4, 4);
			gbc.anchor = GridBagConstraints.CENTER;
			leftHalf.add(manualButton, gbc);
			manualButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { manual1Page(); }
			});

			// manual and edit buttons at the bottom of the left side
			JButton manualMultiButton = new JButton("Many-Page Man");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;      gbc.gridy = 5;
			gbc.gridwidth = 1;  gbc.gridheight = 1;
			gbc.insets = new Insets(0, 4, 4, 4);
			gbc.anchor = GridBagConstraints.CENTER;
			leftHalf.add(manualMultiButton, gbc);
			manualMultiButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { manualManyPages(); }
			});

			JButton editButton = new JButton("Edit Page");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;      gbc.gridy = 6;
			gbc.gridwidth = 2;  gbc.gridheight = 1;
			gbc.insets = new Insets(0, 4, 4, 4);
			leftHalf.add(editButton, gbc);
			editButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { edit(); }
			});
		}

		// set up scroll pane for manual (on the right)
		editorPane = new JEditorPane();
		editorPane.setEditorKit(new HTMLEditorKit());
		editorPane.addHyperlinkListener(new Hyperactive(this));
		editorPane.setEditable(false);
		rightHalf = new JScrollPane(editorPane);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		rightHalf.setPreferredSize(new Dimension(screenSize.width/2, screenSize.height*3/4));
		rightHalf.setMinimumSize(new Dimension(screenSize.width/4, screenSize.height/3));

		// build split pane with both halves
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setLeftComponent(leftHalf);
		splitPane.setRightComponent(rightHalf);
		splitPane.setDividerLocation(200);
		gbc = new GridBagConstraints();
		gbc.gridx = 0;      gbc.gridy = 0;
		gbc.gridwidth = 1;  gbc.gridheight = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;  gbc.weighty = 1.0;
		getContentPane().add(splitPane, gbc);

		// close of dialog event
		addWindowListener(new WindowAdapter()
		{
		    public void windowClosing(WindowEvent evt) { closeDialog(evt); }
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
		}

		public String convertValueToText(Object value, boolean selected, boolean expanded, boolean leaf,
			int row, boolean hasFocus)
		{
			Object nodeInfo = ((DefaultMutableTreeNode)value).getUserObject();
			if (nodeInfo instanceof Integer)
			{
				Integer index = (Integer)nodeInfo;
				PageInfo pi = dialog.pageSequence.get(index.intValue());
				String ret = pi.title;
				if (pi.sectionNumber > 0) ret = pi.sectionNumber + ": " + ret;
				return ret;
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

    private void closeDialog(WindowEvent evt)
    {
        setVisible(false);
        dispose();
		theManual = null;
    }

}
