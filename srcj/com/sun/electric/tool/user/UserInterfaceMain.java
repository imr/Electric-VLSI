/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UserInterfaceMain.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user;

import com.sun.electric.database.Snapshot;
import com.sun.electric.database.change.DatabaseChangeEvent;
import com.sun.electric.database.change.DatabaseChangeListener;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.Version;
import com.sun.electric.tool.AbstractUserInterface;
import com.sun.electric.tool.Client;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.dialogs.OptionReconcile;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.ErrorLoggerTree;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

/**
 * Class to build the UserInterface for the main GUI version of the user interface.
 */
public class UserInterfaceMain extends AbstractUserInterface
{
    static final Logger logger = Logger.getLogger("com.sun.electric.tool.user");
    /**
     * Describe the windowing mode.  The current modes are MDI and SDI.
     */
    public static enum Mode { MDI, SDI };

	/** Property fired if ability to Undo changes */	public static final String propUndoEnabled = "UndoEnabled";
	/** Property fired if ability to Redo changes */	public static final String propRedoEnabled = "RedoEnabled";
    
    static volatile boolean initializationFinished = false;
    
    private static volatile boolean undoEnabled = false;
    private static volatile boolean redoEnabled = false;
    private static final EventListenerList undoRedoListenerList = new EventListenerList();
    private static EventListenerList listenerList = new EventListenerList();
    private static Snapshot currentSnapshot = Snapshot.EMPTY;
    private static EDatabase database = EDatabase.clientDatabase();
    
    private Class osXClass = null;
    private Method osXRegisterMethod = null/*, osXSetJobMethod = null*/;
    private SplashWindow sw = null;
 
    public UserInterfaceMain(List<String> argsList, Mode mode, boolean showSplash) {
        new EventProcessor();
        SwingUtilities.invokeLater(new InitializationRun(argsList, mode, showSplash));
    }
    
    protected void dispatchServerEvent(ServerEvent serverEvent) throws Exception { }
    
    public void addEvent(Client.ServerEvent serverEvent) { SwingUtilities.invokeLater(serverEvent); }
    
    private class InitializationRun implements Runnable {
        List<String> argsList;
        Mode mode;
        boolean showSplash;
        InitializationRun(List<String> argsList, Mode mode, boolean showSplash) {
            this.argsList = argsList;
            this.mode = mode;
            this.showSplash = showSplash;
        }
        public void run() {
            assert SwingUtilities.isEventDispatchThread();
            // see if there is a Mac OS/X interface
            if (System.getProperty("os.name").toLowerCase().startsWith("mac")) {
                try {
                    osXClass = Class.forName("com.sun.electric.MacOSXInterface");
                    
                    // find the necessary methods on the Mac OS/X class
                    try {
                        osXRegisterMethod = osXClass.getMethod("registerMacOSXApplication", new Class[] {List.class});
//                    osXSetJobMethod = osXClass.getMethod("setInitJob", new Class[] {Job.class});
                    } catch (NoSuchMethodException e) {
                        osXRegisterMethod = /*osXSetJobMethod =*/ null;
                    }
                    if (osXRegisterMethod != null) {
                        try {
                            osXRegisterMethod.invoke(osXClass, new Object[] {argsList});
                        } catch (Exception e) {
                            System.out.println("Error initializing Mac OS/X interface");
                        }
                    }
                } catch (ClassNotFoundException e) {}
            }
            //		MacOSXInterface.registerMacOSXApplication(argsList);
            
            //runThreadStatusTimer();
            
            if (showSplash)
                sw = new SplashWindow();
            
            TopLevel.OSInitialize(mode);
        }
    }
        
   /**
     * Method is called when initialization was finished.
     */
    public void finishInitialization() {
        initializationFinished = true;
//        if (osXRegisterMethod != null) {
//            // tell the Mac OS/X system of the initialization job
//            try {
//                osXSetJobMethod.invoke(osXClass, new Object[] {job});
//            } catch (Exception e) {
//                System.out.println("Error initializing Mac OS/X interface");
//            }
//        }
        
        if (sw != null) {
            sw.removeNotify();
            sw = null;
        }
        TopLevel.InitializeWindows();
        WindowFrame.wantToOpenCurrentLibrary(true);
    }
    
    public EditWindow_ getCurrentEditWindow_() { return EditWindow.getCurrent(); }

	public EditWindow_ needCurrentEditWindow_() { return EditWindow.needCurrent(); }

	public Cell getCurrentCell() { return WindowFrame.getCurrentCell(); }

	/**
	 * Method to get the current Cell in a given Library.
	 * @param lib the library to query.
	 * @return the current Cell in the Library.
	 * @return the current cell in the library; null if there is no current Cell.
	 */
	public Cell getCurrentCell(Library lib)
	{
		return lib.getCurCell();
	}

	/**
	 * Method to set the current Cell in a Library.
	 * @param lib the library in which to set a current cell.
	 * @param curCell the new current Cell in the Library (can be null).
	 */
	public void setCurrentCell(Library lib, Cell curCell)
	{
		lib.setCurCell(curCell);
	}

	public Cell needCurrentCell() { return WindowFrame.needCurCell(); }

    /**
     * Method to adjust reference point in WindowFrame containing the cell
     */
    public void adjustReferencePoint(Cell theCell, double cX, double cY)
    {
        // adjust all windows showing this cell
		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			WindowContent content = wf.getContent();
			if (!(content instanceof EditWindow_)) continue;
			Cell cell = content.getCell();
			if (cell != theCell) continue;
			EditWindow_ wnd = (EditWindow_)content;
			Point2D off = wnd.getOffset();
			off.setLocation(off.getX()-cX, off.getY()-cY);
			wnd.setOffset(off);
		}
    }

	public void repaintAllEditWindows() { EditWindow.repaintAllContents(); }

	public void alignToGrid(Point2D pt) { EditWindow.gridAlign(pt); }

	public int getDefaultTextSize() { return EditWindow.getDefaultFontSize(); }

	public EditWindow_ displayCell(Cell cell)
	{
		WindowFrame wf = WindowFrame.createEditWindow(cell);
		if (wf.getContent() instanceof EditWindow_) return (EditWindow_)wf.getContent();
		return null;
	}

    // ErrorLogger functions
    public void termLogging(final ErrorLogger logger, boolean explain)
    {
        if (logger.getNumLogs() == 0) return;

        ErrorLoggerTree.addLogger(logger, explain);
    }

    public void updateNetworkErrors(Cell cell, List<ErrorLogger.MessageLog> errors) {
        ErrorLoggerTree.updateNetworkErrors(cell, errors);
    }
    
    public void updateIncrementalDRCErrors(Cell cell, List<ErrorLogger.MessageLog> errors) {
        ErrorLoggerTree.updateNetworkErrors(cell, errors);
    }

    /**
     * Method to return the error message associated with the current error.
     * Highlights associated graphics if "showhigh" is nonzero.  Fills "g1" and "g2"
     * with associated geometry modules (if nonzero).
     */
    public String reportLog(ErrorLogger.MessageLog log, boolean showhigh, Geometric [] gPair)
    {
        // if two highlights are requested, find them
        if (gPair != null)
        {
            Geometric geom1 = null, geom2 = null;
            for(Iterator<ErrorHighlight> it = log.getHighlights(); it.hasNext(); )
            {
                ErrorHighlight eh = it.next();
                    if (geom1 == null) geom1 = (Geometric)eh.getObject();
                    else if (geom2 == null) geom2 = (Geometric)eh.getObject();
            }

            // return geometry if requested
            if (geom1 != null) gPair[0] = geom1;
            if (geom2 != null) gPair[1] = geom2;
        }

        // show the error
        if (showhigh)
        {
            Highlighter highlighter = null;
            EditWindow wnd = null;

            // first show the geometry associated with this error
            for(Iterator<ErrorHighlight> it = log.getHighlights(); it.hasNext(); )
            {
                ErrorHighlight eh = it.next();

                Cell cell = eh.getCell();
                // validate the cell (it may have been deleted)
                if (cell != null)
                {
                    if (!cell.isLinked())
                    {
                        return "(cell deleted): " + log.getMessageString();
                    }

                    // make sure it is shown
                    boolean found = false;
                    for(Iterator<WindowFrame> it2 = WindowFrame.getWindows(); it2.hasNext(); )
                    {
                        WindowFrame wf = (WindowFrame)it2.next();
                        WindowContent content = wf.getContent();
                        if (!(content instanceof EditWindow)) continue;
                        wnd = (EditWindow)content;
                        if (wnd.getCell() == cell)
                        {
                            if (((eh.getVarContext() != null) && eh.getVarContext().equals(wnd.getVarContext())) ||
                                    (eh.getVarContext() == null)) {
                                // already displayed.  should force window "wf" to front? yes
                                wf.getFrame().toFront();
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found)
                    {
                        // make a new window for the cell
                        WindowFrame wf = WindowFrame.createEditWindow(cell);
                        wnd = (EditWindow)wf.getContent();
                        wnd.setCell(eh.getCell(), eh.getVarContext());
                    }
                    if (highlighter == null) {
                        highlighter = wnd.getHighlighter();
                        highlighter.clear();
                    }
                }

                if (highlighter == null) continue;

                eh.addToHighlighter(highlighter);
            }

            if (highlighter != null)
            {
                highlighter.ensureHighlightingSeen();
                highlighter.finished();

                // make sure the selection is visible
                Rectangle2D hBounds = highlighter.getHighlightedArea(wnd);
                Rectangle2D shown = wnd.getDisplayedBounds();
                if (!shown.intersects(hBounds))
                {
                    wnd.focusOnHighlighted();
                }
            }
        }

        // return the error message
        return log.getMessageString();
    }

    /**
     * Method to show an error message.
     * @param message the error message to show.
     * @param title the title of a dialog with the error message.
     */
    public void showErrorMessage(Object message, String title)
    {
		JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Method to show an informational message.
     * @param message the message to show.
     * @param title the title of a dialog with the message.
     */
    public void showInformationMessage(Object message, String title)
    {
		JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Method to show a message and ask for confirmation.
     * @param message the message to show.
     * @return true if "yes" was selected, false if "no" was selected.
     */
    public boolean confirmMessage(Object message)
    {
		int response = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(), message);
		return response == JOptionPane.YES_OPTION;
    }

    /**
     * Method to ask for a choice among possibilities.
     * @param message the message to show.
     * @param title the title of the dialog with the query.
     * @param choices an array of choices to present, each in a button.
     * @param defaultChoice the default choice.
     * @return the index into the choices array that was selected.
     */
    public int askForChoice(Object message, String title, String [] choices, String defaultChoice)
    {
	    int val = JOptionPane.showOptionDialog(TopLevel.getCurrentJFrame(), message, title,
	    	JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, choices, defaultChoice);
	    return val;
    }

    /**
     * Method to ask for a line of text.
     * @param message the prompt message.
     * @param title the title of a dialog with the message.
     * @param def the default response.
     * @return the string (null if cancelled).
     */
    public String askForInput(Object message, String title, String def)
    {
    	return JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(), message, title, JOptionPane.QUESTION_MESSAGE, null, null, def).toString();
    }

    /** For Pref */
	/**
	 * Method to import the preferences from an XML file.
	 * Prompts the user and reads the file.
	 */
    public void importPrefs()
    {
		// prompt for the XML file
        String fileName = OpenFile.chooseInputFile(FileType.PREFS, "Saved Preferences");
        if (fileName == null) return;

        Pref.importPrefs(fileName);
    }

    /**
	 * Method to export the preferences to an XML file.
	 * Prompts the user and writes the file.
	 */
	public void exportPrefs()
	{
		// prompt for the XML file
        String fileName = OpenFile.chooseOutputFile(FileType.PREFS, "Saved Preferences", "electricPrefs.xml");
        if (fileName == null) return;

        Pref.exportPrefs(fileName);
    }
    
    // ExtendedUserInterface
    
    public void restoreSavedBindings(boolean initialCall)
    {
        TopLevel top = (TopLevel)TopLevel.getCurrentJFrame();
        top.getTheMenuBar().restoreSavedBindings(false); //trying to cache again
    }

    public void finishPrefReconcilation(String libName, List<Pref.Meaning> meaningsToReconcile)
    {
        OptionReconcile dialog = new OptionReconcile(TopLevel.getCurrentJFrame(), true, meaningsToReconcile, libName);
		dialog.setVisible(true);
    }

    /**
     * Save current state of highlights and return its ID.
     */
    public int saveHighlights() {
        EditWindow_ wnd = getCurrentEditWindow_();
        if (wnd == null) return -1;

        SavedHighlights sh = new SavedHighlights(lastId++, wnd);
        while (savedHighlights.size() >= User.getMaxUndoHistory() && !savedHighlights.isEmpty())
            savedHighlights.remove(0);
        savedHighlights.add(sh);
        return sh.id;
    }
    
    /**
     * Restore state of highlights by its ID.
     */
    public void restoreHighlights(int highlightsId) {
        for (SavedHighlights sh: savedHighlights) {
            if (sh.id == highlightsId) {
                sh.restore();
                break;
            }
        }
    }
    
    /**
     * Show status of undo/redo buttons
     * @param newUndoEnabled new status of undo button.
     * @param newRedoEnabled new status of redo button.
     */
    public void showUndoRedoStatus(boolean newUndoEnabled, boolean newRedoEnabled) {
        if (undoEnabled != newUndoEnabled) {
            PropertyChangeEvent e = new PropertyChangeEvent(this, propUndoEnabled, undoEnabled, newUndoEnabled);
            undoEnabled = newUndoEnabled;
            SwingUtilities.invokeLater(new PropertyChangeRun(e));
        }
        if (redoEnabled != newRedoEnabled) {
            PropertyChangeEvent e = new PropertyChangeEvent(this, propRedoEnabled, redoEnabled, newRedoEnabled);
            redoEnabled = newRedoEnabled;
            SwingUtilities.invokeLater(new PropertyChangeRun(e));
        }
    }
    
    /**
     * Show new database snapshot.
     * @param newSnapshot new snapshot.
     */
    public void showSnapshot(Snapshot newSnapshot, boolean undoRedo) {
            SwingUtilities.invokeLater(new DatabaseChangeRun(newSnapshot, undoRedo));
    }

    public void beep() {
        Toolkit.getDefaultToolkit().beep();
    }
    
    /**
	 * Method to tell whether undo can be done.
	 * This is used by the tool bar to determine whether the undo button should be available.
	 * @return true if undo can be done.
	 */
	public static boolean getUndoEnabled() { return undoEnabled; }

	/**
	 * Method to tell whether redo can be done.
	 * This is used by the tool bar to determine whether the undo button should be available.
	 * @return true if redo can be done.
	 */
	public static boolean getRedoEnabled() { return redoEnabled; }

	/** Add a property change listener. This generates Undo and Redo enabled property changes */
	public static synchronized void addUndoRedoListener(PropertyChangeListener l)
	{
        assert SwingUtilities.isEventDispatchThread();
		undoRedoListenerList.add(PropertyChangeListener.class, l);
	}

	/** Remove a property change listener. */
	public static synchronized void removeUndoRedoListener(PropertyChangeListener l)
	{
        assert SwingUtilities.isEventDispatchThread();
		undoRedoListenerList.remove(PropertyChangeListener.class, l);
	}

    private static void firePropertyChange(PropertyChangeEvent e) {
        assert SwingUtilities.isEventDispatchThread();
        Object[] listeners;
        synchronized (UserInterfaceMain.class) {
            listeners = undoRedoListenerList.getListenerList();
        }
        // Process the listeners last to first, notifying those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i] == PropertyChangeListener.class)
                ((PropertyChangeListener)listeners[i+1]).propertyChange(e);
        }
    }

	private static class PropertyChangeRun implements Runnable {
		private PropertyChangeEvent e;
		private PropertyChangeRun(PropertyChangeEvent e) { this.e = e; }
		public void run() { firePropertyChange(e); }
    }

    /** Add a DatabaseChangeListener. It will be notified when
     * state of the database changes.
     * @param l the listener
     */
    public static synchronized void addDatabaseChangeListener(DatabaseChangeListener l) {
        listenerList.add(DatabaseChangeListener.class, l);
    }
    
    /** Remove a DatabaseChangeListener. */
    public static synchronized void removeDatabaseChangeListener(DatabaseChangeListener l) {
        listenerList.remove(DatabaseChangeListener.class, l);
    }
    
    /**
     * Fire DatabaseChangeEvent to DatabaseChangeListeners.
     * @param e DatabaseChangeEvent.
     */
    public static void fireDatabaseChangeEvent(DatabaseChangeEvent e) {
        Object[] listeners;
        synchronized (User.class) {
            listeners = listenerList.getListenerList();
        }
        // Process the listeners last to first, notifying those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i] == DatabaseChangeListener.class)
                ((DatabaseChangeListener)listeners[i+1]).databaseChanged(e);
        }
    }
   
	private static class DatabaseChangeRun implements Runnable
	{
		private Snapshot newSnapshot;
        private boolean undoRedo;
		private DatabaseChangeRun(Snapshot newSnapshot, boolean undRedo) {
            this.newSnapshot = newSnapshot;
            this.undoRedo = undoRedo;
        }
        public void run() {
            DatabaseChangeEvent event = new DatabaseChangeEvent(currentSnapshot, newSnapshot);
            for(Iterator<Listener> it = Tool.getListeners(); it.hasNext(); ) {
                Listener listener = it.next();
                listener.endBatch(currentSnapshot, newSnapshot, false);
            }
            currentSnapshot = newSnapshot;
            fireDatabaseChangeEvent(event);
        }
	}
    
    private int lastId = 0;
    private ArrayList<SavedHighlights> savedHighlights = new ArrayList<SavedHighlights>();
    
    private static class SavedHighlights {
        /** id of this saved state */               private final int id;
        /** EditWindow_ of highlights */            private final EditWindow_ wnd;
        /** list of saved Highlights */             private final List<Highlight2> savedHighlights;
        /** saved Highlight offset */               private final Point2D savedHighlightsOffset;
        
        private SavedHighlights(int id, EditWindow_ wnd) {
            this.id = id;
            this.wnd = wnd;
            savedHighlights = wnd.saveHighlightList();
            savedHighlightsOffset = wnd.getHighlightOffset();
        }
        
        private void restore() {
			wnd.restoreHighlightList(savedHighlights);
			wnd.setHighlightOffset((int)savedHighlightsOffset.getX(), (int)savedHighlightsOffset.getY());
			wnd.finishedHighlighting();
        }
    }
    
	/**
	 * Class to display a Splash Screen at the start of the program.
	 */
	private static class SplashWindow extends JFrame
	{
		public SplashWindow()
		{
			super();
			setUndecorated(true);
			setTitle("Electric Splash");
			setIconImage(TopLevel.getFrameIcon().getImage());

			JPanel whole = new JPanel();
			whole.setBorder(BorderFactory.createLineBorder(new Color(0, 170, 0), 5));
			whole.setLayout(new BorderLayout());

			JLabel l = new JLabel(Resources.getResource(TopLevel.class, "SplashImage.gif"));
			whole.add(l, BorderLayout.CENTER);
			JLabel v = new JLabel("Version " + Version.getVersion(), JLabel.CENTER);
			whole.add(v, BorderLayout.SOUTH);
			Font font = new Font(User.getDefaultFont(), Font.BOLD, 24);
			v.setFont(font);
			v.setForeground(Color.BLACK);
			v.setBackground(Color.WHITE);

			getContentPane().add(whole, BorderLayout.SOUTH);
			pack();
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			Dimension labelSize = getPreferredSize();
			setLocation(screenSize.width/2 - (labelSize.width/2),
				screenSize.height/2 - (labelSize.height/2));
			WindowsEvents windowsEvents = new WindowsEvents(this);
			addWindowListener(windowsEvents);
			setVisible(true);
		}
	}

	/**
	 * This class handles deactivation of the splash screen and forces it back to the top.
	 */
	private static class WindowsEvents implements WindowListener
	{
		SplashWindow sw;

		WindowsEvents(SplashWindow sw)
		{
			super();
			this.sw = sw;
		}

		public void windowActivated(WindowEvent e) {}
		public void windowClosed(WindowEvent e) {}
		public void windowClosing(WindowEvent e) {}
		public void windowDeiconified(WindowEvent e) {}
		public void windowIconified(WindowEvent e) {}
		public void windowOpened(WindowEvent e) {}

		public void windowDeactivated(WindowEvent e)
		{
			sw.toFront();
		}
	}

    /**
     * Places a custom event processor on the event queue in order to
     * catch all exceptions generated by event processing.
     */
    private static class EventProcessor extends EventQueue
    {
        private final String CLASS_NAME = getClass().getName();
        private int dispatchDepth = 0;
		private EventProcessor() {
            Toolkit kit = Toolkit.getDefaultToolkit();
            kit.getSystemEventQueue().push(this);
        }

//        public void postEvent(AWTEvent theEvent) {
//            logger.entering(CLASS_NAME, "postEvent", theEvent);
//            super.postEvent(theEvent);
//            logger.exiting(CLASS_NAME, "postEvent");
//        }
//        
//        public AWTEvent getNextEvent() throws InterruptedException {
//            logger.entering(CLASS_NAME, "getNextEvent");
//            AWTEvent event = super.getNextEvent();
//            logger.exiting(CLASS_NAME, "getNextEvent", event);
//            return event;
//        }
//        
//        public synchronized AWTEvent peekEvent() {
//            logger.entering(CLASS_NAME, "peekEvent");
//            AWTEvent event = super.peekEvent();
//            logger.exiting(CLASS_NAME, "peekEvent", event);
//            return event;
//        }
//
//        public synchronized AWTEvent peekEvent(int id) {
//            logger.entering(CLASS_NAME, "peekEvent", id);
//            AWTEvent event = super.peekEvent(id);
//            logger.exiting(CLASS_NAME, "peekEvent", event);
//            return event;
//        }
        
        protected void dispatchEvent(AWTEvent e) {
//            logger.entering(CLASS_NAME, "dispatchEvent", e);
//            if (dispatchDepth == 0)
//                database.lock(false);
            dispatchDepth++;
            try {
                super.dispatchEvent(e);
            }
            catch(Throwable ex) {
                ex.printStackTrace(System.err);
                ActivityLogger.logException(ex);
                if (ex instanceof Error) {
                    logger.throwing(CLASS_NAME, "dispatchEvent", ex);
                    throw (Error)ex;
                }
//            } finally {
//                dispatchDepth--;
//                if (dispatchDepth == 0)
//                    database.unlock();
            }
//            logger.exiting(CLASS_NAME, "dispatchEvent");
        }
    }

//    private static void runThreadStatusTimer() {
//        int delay = 1000*60*10; // milliseconds
//        Timer timer = new Timer(delay, new ThreadStatusTask());
//        timer.start();
//    }
//
//    private static class ThreadStatusTask implements ActionListener {
//        public void actionPerformed(ActionEvent e) {
//            Thread t = Thread.currentThread();
//            ThreadGroup group = t.getThreadGroup();
//            // get the top level group
//            while (group.getParent() != null)
//                group = group.getParent();
//            Thread [] threads = new Thread[200];
//            int numThreads = group.enumerate(threads, true);
//            StringBuffer buf = new StringBuffer();
//            for (int i=0; i<numThreads; i++) {
//                buf.append("Thread["+i+"] "+threads[i]+": alive: "+threads[i].isAlive()+", interrupted: "+threads[i].isInterrupted()+"\n");
//            }
//            ActivityLogger.logThreadMessage(buf.toString());
//        }
//    }


}
