package com.sun.electric.tool.user.ui;

import com.sun.electric.database.text.Pref;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.WindowContent;

import javax.swing.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 *
 * In progress
 */
public class WindowManager {


    /**
     * OS is a typesafe enum class that describes the current operating system.
     */
    public static class OS
    {
        private final String name;

        private OS(String name) { this.name = name; }

        /**
         * Returns a printable version of this OS.
         * @return a printable version of this OS.
         */
        public String toString() { return name; }

        /** Describes Windows. */							public static final OS WINDOWS   = new OS("Windows");
        /** Describes UNIX/Linux. */						public static final OS UNIX      = new OS("UNIX");
        /** Describes Macintosh. */							public static final OS MACINTOSH = new OS("Macintosh");
    }

    /**
     * Describe the windowing mode.  The current modes are MDI and SDI.
     */
    public static class Mode
    {
        private final String name;
        private Mode(String name) { this.name = name; }

        public String toString() { return name; }
        public static final Mode MDI = new Mode("MDI");
        public static final Mode SDI = new Mode("SDI");
    }


    /** The type of OS */                                   private OS os;
    /** The mode */                                         private Mode mode;
    /** The size of the screen. */							private static Dimension scrnSize;
    /** Map of WindowContents to their Containers */        private Map windowContents;

    private static WindowManager WM = null;
    private static final Pref cacheWindowLoc = Pref.makeStringPref("WindowLocation", User.tool.prefs, "");

    private WindowManager() {
        windowContents = new HashMap();
        // setup the size of the screen
        scrnSize = (Toolkit.getDefaultToolkit()).getScreenSize();
        os = null;
        mode = null;
    }

    // ---------------------------- Static Interface Methods ----------------------------------------

    public static void initialize(OS os, Mode mode) {
        WM = new WindowManager();
        WM.init(os, mode);
    }


    public static void addWindowContentStatic(WindowContent content) {
        if (WM == null) return;
        WM.addWindowContent(content);
    }

    public static void removeWindowContentStatic(WindowContent content) {
        if (WM == null) return;
        WM.removeWindowContent(content);
    }

    public static void replaceWindowContentStatic(WindowContent oldContent, WindowContent newContent) {
        if (WM == null) return;
        WM.replaceWindowContent(oldContent, newContent);
    }


    // ----------------------------- Private Methods -----------------------------------------

    /**
     * Initialize the WindowManager based on OS and Inteface Mode
     * @param os the OS
     * @param mode the interface mode (MDI or SDI)
     */
    private void init(OS os, Mode mode) {

        OS defaultOS = null;
        Mode defaultMode = null;
		try{
			String osName = System.getProperty("os.name").toLowerCase();
			if (osName.startsWith("windows"))
			{
				defaultOS = OS.WINDOWS;
				defaultMode = Mode.MDI;
				scrnSize.height -= 30;
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			} else if (osName.startsWith("linux") || osName.startsWith("solaris") || osName.startsWith("sunos"))
			{
				defaultOS = OS.UNIX;
                defaultMode = Mode.SDI;
                //UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			} else if (osName.startsWith("mac"))
			{
				defaultOS = OS.MACINTOSH;
                defaultMode = Mode.SDI;
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.MacLookAndFeel");
			}
		} catch(Exception e) {}

        this.os = (os != null) ? os : defaultOS;
        this.mode = (mode != null) ? mode : defaultMode;

        // set current working directory
        String setting = User.getInitialWorkingDirectorySetting();
        if (setting.equals(User.INITIALWORKINGDIRSETTING_BASEDONOS)) {
            // default is last used dir
            if (os == OS.UNIX) {
                // switch to current dir
                User.setWorkingDirectory(System.getProperty("user.dir"));
            }
        } else if (setting.equals(User.INITIALWORKINGDIRSETTING_USECURRENTDIR))
            User.setWorkingDirectory(System.getProperty("user.dir"));
        // else
            // default is to use last used dir

        // If MDI mode, initialize the top level frame
        if (mode == Mode.MDI) initMDI();
    }

    /**
     * Initialize the Multiple Document Inteface top level frame
     */
    private void initMDI() {

    }


    /**
     * This is the thread-safe way to add a WindowContent to the windowing system.
     * It will be setVisible after it is added.
     * @param content
     */
    public void addWindowContent(WindowContent content) {
        // this is the safe way to add content
        final WindowContent theContent = content;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { addWindowContentUnsafe(theContent); }
        });
    }

    /**
     * This is the thread-safe way to remove a WindowContent from the windowing System.
     * @param content the content to remove
     */
    public void removeWindowContent(WindowContent content) {
        // this is the safe way to remove content
        final WindowContent theContent = content;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { removeWindowContentUnsafe(theContent); }
        });
    }

    /**
     * This is the thread-safe way to replace a WindowContent in the windowing System.
     * @param oldContent the content to replace
     * @param newContent the new content
     */
    public void replaceWindowContent(WindowContent oldContent, WindowContent newContent) {
        // this is the safe way to replace content
        final WindowContent theOldContent = oldContent;
        final WindowContent theNewContent = newContent;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { replaceWindowContentUnsafe(theOldContent, theNewContent); }
        });
    }



    /**
     * This is the thread-unsafe way to add a WindowContent.
     * For code running the AWT event thread (most likely other GUI
     * components), yoo can use this method.
     * @param content the content to add
     */
    protected void addWindowContentUnsafe(WindowContent content) {

    }

    /**
     * This is the thread-unsafe way to remove a WindowContent.
     * For code running in the AWT event thread (most likely other GUI
     * components), you can use this method.
     * @param content the content to remove
     */
    protected void removeWindowContentUnsafe(WindowContent content) {

    }

    /**
     * This is the thread-unsafe way to replace a WindowContent.
     * For code running in the AWT event thread (most likely other GUI
     * components), you can use this method.
     * @param oldContent the content to replace
     * @param newContent the new content
     */
    protected void replaceWindowContentUnsafe(WindowContent oldContent, WindowContent newContent) {

    }
}
