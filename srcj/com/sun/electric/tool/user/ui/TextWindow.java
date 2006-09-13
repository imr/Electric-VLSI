/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TextWindow.java
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.WeakReferences;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.OpenFile;

import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.tree.MutableTreeNode;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

/**
 * This class defines a text window for displaying text cells.
 */
public class TextWindow implements WindowContent
{
	/** the cell that is in the window */					private Cell cell;
	/** the window frame containing this editwindow */      private WindowFrame wf;
	/** the overall panel with disp area and sliders */		private JPanel overall;
	/** true if the text in the window changed. */			private boolean dirty;
	/** true if the text in the window is closing. */		private boolean finishing;
	private JTextArea textArea;
	private JScrollPane scrollPane;
	private UndoManager undo = new UndoManager();

	/**
	 * Factory method to create a new TextWindow with a given cell, in a given WindowFrame.
	 * @param cell the cell in this TextWindow.
	 * @param wf the WindowFrame that this TextWindow lives in.
	 * //@return the new TextWindow.
	 */
	public TextWindow(Cell cell, WindowFrame wf)
	{
		this.wf = wf;
		this.finishing = false;

		textArea = new JTextArea();
		scrollPane = new JScrollPane(textArea);
		overall = new JPanel();
		overall.setLayout(new BorderLayout());
		overall.add(scrollPane, BorderLayout.CENTER);

		setCell(cell, VarContext.globalContext, null);

		TextWindowDocumentListener twDocumentListener = new TextWindowDocumentListener(this);
		textArea.getDocument().addDocumentListener(twDocumentListener);
		textArea.getDocument().addUndoableEditListener(new MyUndoableEditListener());
		textArea.addFocusListener(twDocumentListener);
	}

    public void setCursor(Cursor cursor)
    {
    	// nothing implemented in TextWindow
    }

	private void setCellFont(Cell cell)
	{
        String fontName = User.getDefaultTextCellFont();
        int fontSize = User.getDefaultTextCellSize();
        if (cell != null)
        {
	        Variable var = cell.getVar(Cell.TEXT_CELL_FONT_NAME, String.class);
	        if (var != null) fontName = (String)var.getObject();
	        var = cell.getVar(Cell.TEXT_CELL_FONT_SIZE, Integer.class);
	        if (var != null) fontSize = ((Integer)var.getObject()).intValue();
        }

        textArea.setFont(new Font(fontName, 0, fontSize));
	}

	private class MyUndoableEditListener implements UndoableEditListener
	{
		public void undoableEditHappened(UndoableEditEvent e)
		{
			// Remember the edit and update the menus
			undo.addEdit(e.getEdit());
			updateUndoRedo();
		}
	}

	private static WeakReferences<PropertyChangeListener> undoListeners = new WeakReferences<PropertyChangeListener>();
    private static WeakReferences<PropertyChangeListener> redoListeners = new WeakReferences<PropertyChangeListener>();

	public static void addTextUndoListener(PropertyChangeListener l) { undoListeners.add(l); }

	public static void addTextRedoListener(PropertyChangeListener l) { redoListeners.add(l); }

	public static void removeTextUndoListener(PropertyChangeListener l) { undoListeners.remove(l); }

	public static void removeTextRedoListener(PropertyChangeListener l) { redoListeners.remove(l); }

	private void updateUndoRedo()
	{
        // Commented temporarily DN 10-Apr-2006
        
//		TopLevel tl = TopLevel.getCurrentJFrame();
//		PropertyChangeEvent un = new PropertyChangeEvent(tl, UserInterfaceMain.propUndoEnabled, null, new Boolean(undo.canUndo()));
//		PropertyChangeEvent re = new PropertyChangeEvent(tl, UserInterfaceMain.propRedoEnabled, null, new Boolean(undo.canRedo()));
//		if (tl != null)
//		{
//			tl.getToolBar().propertyChange(un);
//			tl.getToolBar().propertyChange(re);
//		}
//        for (Iterator<PropertyChangeListener> it = undoListeners.reverseIterator(); it.hasNext(); )
//            it.next().propertyChange(un);
//        for (Iterator<PropertyChangeListener> it = redoListeners.reverseIterator(); it.hasNext(); )
//            it.next().propertyChange(un);
	}

	/**
	 * Method to undo changes to text in this TextWindow.
	 */
	public void undo()
	{
		try
		{
			undo.undo();
			updateUndoRedo();
		} catch (CannotUndoException e)
		{
			System.out.println("Cannot undo");
		}
	} 

	/**
	 * Method to redo changes to text in this TextWindow.
	 */
	public void redo()
	{
		try
		{
			undo.redo();
			updateUndoRedo();
		} catch (CannotRedoException e)
		{
			System.out.println("Cannot redo");
		}
	}

	/**
	 * Method to repaint this TextWindow.
	 */
	public void paint(Graphics g)
	{
		// to enable keys to be received
		if (cell != null && cell == WindowFrame.getCurrentCell())
			textArea.requestFocus();

		textArea.paint(g);
	}

	/**
	 * Method to update the font information in this window.
	 */
	public void updateFontInformation()
	{
		textArea.setFont(new Font(User.getDefaultTextCellFont(), 0, User.getDefaultTextCellSize()));
	}

	/**
	 * Class to handle special changes to changes to design rules.
	 */
	private static class TextWindowDocumentListener implements DocumentListener, FocusListener
	{
		private TextWindow tw;

		TextWindowDocumentListener(TextWindow tw) { this.tw = tw; }

		public void changedUpdate(DocumentEvent e) { tw.textWindowContentChanged(); }
		public void insertUpdate(DocumentEvent e) { tw.textWindowContentChanged(); }
		public void removeUpdate(DocumentEvent e) { tw.textWindowContentChanged(); }
		public void focusGained(FocusEvent e)
		{
			TopLevel top = (TopLevel)TopLevel.getCurrentJFrame();
			top.getTheMenuBar().setIgnoreTextEditKeys(true);
		}
		public void focusLost(FocusEvent e)
		{
			TopLevel top = (TopLevel)TopLevel.getCurrentJFrame();
			top.getTheMenuBar().setIgnoreTextEditKeys(false);
			if (tw.dirty)
				new SaveCellText(tw);
		}
	}

	private void textWindowContentChanged()
	{
        new ChangedCellText(this);
	}

	public List<MutableTreeNode> loadExplorerTrees()
	{
        return wf.loadDefaultExplorerTree();
	}

	/**
	 * Method to return the top-level JPanel for this TextWindow.
	 * @return the top-level JPanel for this TextWindow.
	 */
	public JPanel getPanel() { return overall; }

	/**
	 * Method to get rid of this EditWindow.  Called by WindowFrame when
	 * that windowFrame gets closed.
	 */
	public void finished()
	{
		if (dirty)
		{
			dirty = false;
			finishing = true;
			if (cell == null) return;
			new SaveCellText(this);
		}
	}

	/**
	 * Class to save a cell's text in a new Job.
	 */
	private static class SaveCellText extends Job
	{
		private Cell cell;
		private String [] strings;
		private transient TextWindow tw;

		private SaveCellText(TextWindow tw)
		{
			super("Save Cell Text", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.tw = tw;
			this.cell = tw.cell;
			this.strings = tw.convertToStrings();
			startJob();
		}

		public boolean doIt() throws JobException
		{
			if (cell != null) cell.setTextViewContents(strings);
			return true;
		}

		public void terminateOK()
		{
			tw.dirty = false;
		}
	}

    private static class ChangedCellText extends Job
	{
		private Cell cell;
		private transient TextWindow tw;

		private ChangedCellText(TextWindow tw)
		{
			super("Change Cell Text", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.tw = tw;
			this.cell = tw.cell;
			startJob();
		}

		public boolean doIt() throws JobException
		{
            if (cell != null) cell.getLibrary().setChanged();
			return true;
		}

		public void terminateOK()
		{
			tw.dirty = true;
		}
	}

	/**
	 * Method to set the window title.
	 */
	public void setWindowTitle()
	{
		if (wf == null) return;
		wf.setTitle(wf.composeTitle(cell, "", 0));
	}

	/**
	 * Method to return the cell that is shown in this window.
	 * @return the cell that is shown in this window.
	 */
	public Cell getCell() { return cell; }

    public Highlighter getHighlighter() { return null; }

	/**
	 * Method to set the cell that is shown in the window to "cell".
	 */
	public void setCell(Cell cell, VarContext context, WindowFrame.DisplayAttributes displayAttributes)
	{
		this.cell = cell;
		String [] lines = (cell != null) ? cell.getTextViewContents() : null;
		String oneLine = (lines != null) ? oneLine = makeOneString(lines) : "";
		setCellFont(cell);
		textArea.setText(oneLine);
		textArea.setSelectionStart(0);
		textArea.setSelectionEnd(0);
		dirty = false;
		setWindowTitle();
	}

	/**
	 * Method to read a text disk file into this TextWindow.
	 */
	public static void readTextCell()
	{
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf == null) return;
		WindowContent content = wf.getContent();
		if (content instanceof TextWindow)
		{
			TextWindow tw = (TextWindow)content;
			String fileName = OpenFile.chooseInputFile(FileType.TEXT, null);
            tw.readTextCell(fileName);
        }
    }

    public void readTextCell(String fileName)
    {
        if (fileName == null) {
            System.out.println("Bad file name: "+fileName);
        }
        // start a job to do the input
        URL fileURL = TextUtils.makeURLToFile(fileName);
        InputStream stream = TextUtils.getURLStream(fileURL);
        try
        {
            fileURL.openConnection();
        } catch (IOException e)
        {
            System.out.println("Could not find file: " + fileURL.getFile());
            return;
        }

        // clear the buffer
        textArea.setText("");

        final int READ_BUFFER_SIZE = 65536;
        char [] buf = new char[READ_BUFFER_SIZE];
//        BufferedInputStream bufStrm = new BufferedInputStream(stream, READ_BUFFER_SIZE);
        InputStreamReader is = new InputStreamReader(stream);
        try
        {
            for(;;)
            {
                int amtRead = is.read(buf, 0, READ_BUFFER_SIZE);
                if (amtRead <= 0) break;
                String addString = new String(buf, 0, amtRead);
                textArea.append(addString);
            }
            stream.close();
        } catch (IOException e)
        {
            System.out.println("Error reading the file");
        }
	}

	/**
	 * Method to save this TextWindow to a disk file.
	 */
	public static void writeTextCell()
	{
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf == null) return;
		WindowContent content = wf.getContent();
		if (content instanceof TextWindow)
		{
			TextWindow tw = (TextWindow)content;
			String fileName = OpenFile.chooseOutputFile(FileType.TEXT, "Text file", content.getCell().getName() + ".txt");
            tw.writeTextCell(fileName);
        }
    }

    public void writeTextCell(String fileName)
    {
        if (fileName == null) {
            System.out.println("Bad filename: "+fileName);
        }
        try
        {
            PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
            Document doc = textArea.getDocument();
            Element paragraph = doc.getDefaultRootElement();
            int lines = paragraph.getElementCount();
            for(int i=0; i<lines; i++)
            {
                Element e = paragraph.getElement(i);
                int startPos = e.getStartOffset();
                int endPos = e.getEndOffset();
                try
                {
                    String line = textArea.getText(startPos, endPos - startPos);
                    printWriter.print(line);
                } catch (BadLocationException ex) {}
            }
            printWriter.close();
        } catch (IOException e)
        {
            System.out.println("Error saving " + fileName);
            return;
        }
        System.out.println("Wrote " + fileName);
	}

	/**
	 * Method to select a line number in this TextWindow.
	 * @param lineNumber the line to select (1-based).
	 */
	public void goToLineNumber(int lineNumber)
	{
		Document doc = textArea.getDocument();
		Element paragraph = doc.getDefaultRootElement();
		int lines = paragraph.getElementCount();
		if (lineNumber <= 0 || lineNumber > lines)
		{
			System.out.println("Line numbers must be between 1 and "+lines);
			return;
		}

		Element e = paragraph.getElement(lineNumber-1);
		int startPos = e.getStartOffset();
		int endPos = e.getEndOffset();
		textArea.setSelectionStart(startPos);
		textArea.setSelectionEnd(endPos);
	}

//	/**
//	 * Method to get the text (for a textual cell) that is being edited.
//	 * @param cell a text-view Cell.
//	 * @return the text for the cell.
//	 * If that text is not being edited (or has not changed), returns null. 
//	 */
//	public static String [] getEditedText(Cell cell)
//	{
//		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
//		{
//			WindowFrame wf = it.next();
//			WindowContent content = wf.getContent();
//			if (content instanceof TextWindow)
//			{
//				if (content.getCell() == cell)
//				{
//					TextWindow tw = (TextWindow)content;
//					if (tw.dirty)
//					{
//						String [] strings = tw.convertToStrings();
//						return strings;
//					}
//				}
//			}
//		}
//		return null;
//	}

    private void updateText(String [] strings) {
        textArea.setText(makeOneString(strings));
        dirty = false;
    }

	/**
	 * Method to update text for a cell (if it is being displayed).
	 * This is called when the text for a cell has been changed by some other part of the system,
	 * and should be redisplayed where appropriate.
	 * @param cell the Cell whose text changed.
	 */
	public static void updateText(Cell cell)
	{
		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = it.next();
			WindowContent content = wf.getContent();
			if (content instanceof TextWindow)
			{
				if (content.getCell() == cell)
				{
					TextWindow tw = (TextWindow)content;
					if (!tw.finishing)
					{
						String [] strings = cell.getTextViewContents();
						tw.updateText(strings);
					}
				}
			}
		}
	}

	/**
	 * Method to convert an array of strings to a single string.
	 * @param strings the array of strings.
	 * @return the single string.
	 */
	private static String makeOneString(String [] strings)
	{
		StringBuffer sb = new StringBuffer();
		int len = strings.length;
		for(int i=0; i<len; i++)
		{
			sb.append(strings[i]);
			sb.append("\n");
		}
		return sb.toString();
	}

	/**
	 * Method to return the number of lines of text in this TextWindow.
	 * @return the number of lines of text in this TextWindow.
	 */
	public int getLineCount()
	{
		Document doc = textArea.getDocument();
		Element paragraph = doc.getDefaultRootElement();
		int lines = paragraph.getElementCount();
		return lines;
	}

	/**
	 * Method to convert the document in this window to an array of strings.
	 * @return an array of strings with the current text.
	 */
	private String [] convertToStrings()
	{
		Document doc = textArea.getDocument();
		Element paragraph = doc.getDefaultRootElement();
		int lines = paragraph.getElementCount();
		String [] strings = new String[lines];
		for(int i=0; i<lines; i++)
		{
			Element e = paragraph.getElement(i);
			int startPos = e.getStartOffset();
			int endPos = e.getEndOffset();
			try
			{
				strings[i] = textArea.getText(startPos, endPos - startPos - 1);
			} catch (BadLocationException ex) {}
		}
		return strings;
	}

	public void rightScrollChanged(int value)
	{
	}

	public void bottomScrollChanged(int value)
	{
	}

	public void repaint() {}

	public void fullRepaint() {}

	/** Returns true if we can go back in history list, false otherwise */
	public boolean cellHistoryCanGoBack() { return false; }

	/** Returns true if we can go forward in history list, false otherwise */
	public boolean cellHistoryCanGoForward() { return false; }

	public void cellHistoryGoBack() {}

	public void cellHistoryGoForward() {}

	/**
	 * Method to pan and zoom the screen so that the entire cell is displayed.
	 */
	public void fillScreen()
	{
	}

	public void zoomOutContents()
	{
	}

	public void zoomInContents()
	{
	}

	public void focusOnHighlighted()
	{
	}

    /**
     * Used when new tool bar is created with existing edit window
     * (when moving windows across displays).  Updates back/forward
     * button states.
     */
    public void fireCellHistoryStatus()
	{
    }

	private String searchString = null;
	private boolean searchCaseSensitive = false;

	/**
	 * Method to initialize for a new text search.
	 * @param search the string to locate.
	 * @param caseSensitive true to match only where the case is the same.
	 */
	public void initTextSearch(String search, boolean caseSensitive, 
	                           boolean regExp, Set<TextUtils.WhatToSearch> whatToSearch)
	{
		if (regExp) {
			System.out.println("Text windows don't yet implement Regular Expression matching");
		}
		searchString = search;
		searchCaseSensitive = caseSensitive;
	}

	/**
	 * Method to find the next occurrence of a string.
	 * @param reverse true to find in the reverse direction.
	 * @return true if something was found.
	 */
	public boolean findNextText(boolean reverse)
	{
		Document doc = textArea.getDocument();
		Element paragraph = doc.getDefaultRootElement();
		int lines = paragraph.getElementCount();
		int lineNo = 0;
		int searchPoint = textArea.getSelectionEnd();
		if (reverse) searchPoint = textArea.getSelectionStart();
		try
		{
			lineNo = textArea.getLineOfOffset(searchPoint);
		} catch (BadLocationException e)
		{
			return false;
		}

		for(int i=0; i<=lines; i++)
		{
			int index = lineNo + i;
			if (reverse) index = lineNo - i + lines;
			Element e = paragraph.getElement(index % lines);
			int startPos = e.getStartOffset();
			int endPos = e.getEndOffset();
			if (i == 0)
			{
				if (reverse) endPos = searchPoint+1; else
					startPos = searchPoint;
			}

			String theLine = null;
			try
			{
				theLine = textArea.getText(startPos, endPos - startPos - 1);
			} catch (BadLocationException ex)
			{
				return false;
			}
			int foundPos = TextUtils.findStringInString(theLine, searchString, 0, searchCaseSensitive, reverse);
			if (foundPos >= 0)
			{
				textArea.setSelectionStart(startPos + foundPos);
				textArea.setSelectionEnd(startPos + foundPos + searchString.length());
				return true;
			}
		}
		return false;
	}

	/**
	 * Method to replace the text that was just selected with findNextText().
	 * @param replace the new text to replace.
	 */
	public void replaceText(String replace)
	{
		int startSelection = textArea.getSelectionStart();
		int endSelection = textArea.getSelectionEnd();
		textArea.replaceRange(replace, startSelection, endSelection);
	}

	/**
	 * Method to replace all selected text.
	 * @param replace the new text to replace everywhere.
	 */
	public void replaceAllText(String replace)
	{
		Document doc = textArea.getDocument();
		Element paragraph = doc.getDefaultRootElement();
		int lines = paragraph.getElementCount();
		for(int i=0; i<lines; i++)
		{
			Element e = paragraph.getElement(i);
			int startPos = e.getStartOffset();
			int endPos = e.getEndOffset()-1;
			String theLine = null;
			try
			{
				theLine = textArea.getText(startPos, endPos - startPos);
			} catch (BadLocationException ex)
			{
				return;
			}
			boolean found = false;
			int scanPos = 0;
			for(;;)
			{
				int foundPos = TextUtils.findStringInString(theLine, searchString, scanPos, searchCaseSensitive, false);
				if (foundPos < 0) break;
				theLine = theLine.substring(0, foundPos) + replace + theLine.substring(foundPos+searchString.length());
				scanPos = foundPos + replace.length();
				found = true;
			}
			if (found) textArea.replaceRange(theLine, startPos, endPos);
		}
	}

    /**
     * Method to export directly PNG file
	 * @param ep printable object.
     * @param filePath
     */
    public void writeImage(ElectricPrinter ep, String filePath)
    {
        System.out.println("TextWindow:writeImage not implemented");
    }

	/**
	 * Method to intialize for printing.
	 * @param ep the ElectricPrinter object.
	 * @param pageFormat information about the print job.
     * @return Always true.
	 */
	public boolean initializePrinting(ElectricPrinter ep, PageFormat pageFormat)
	{
		int pageWid = (int)pageFormat.getImageableWidth() * ep.getDesiredDPI() / 72;
		int pageHei = (int)pageFormat.getImageableHeight() * ep.getDesiredDPI() / 72;
		overall.setSize(pageWid, pageHei);
		overall.validate();
		overall.repaint();
        return true;
    }

	/**
	 * Method to print window using offscreen canvas.
	 * @param ep printable object.
	 * @return the image to print (null on error).
	 */
	public BufferedImage getPrintImage(ElectricPrinter ep)
	{
		Graphics2D g2d = (Graphics2D)ep.getGraphics();
		if (g2d != null)
		{
			textArea.paint(g2d);
		}
		return (BufferedImage)overall.createImage(1, 1);
	}

	/**
	 * Method to pan along X or Y according to fixed amount of ticks
	 * @param direction
	 * @param panningAmounts
	 * @param ticks
	 */
	public void panXOrY(int direction, double[] panningAmounts, int ticks)
	{
		// Nothing in this case
	}
}
