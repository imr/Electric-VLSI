/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: OpenFile.java
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
package com.sun.electric.tool.user.dialogs;

import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.menus.FileMenu;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibDirs;
import com.sun.electric.tool.Client;
import com.sun.electric.database.text.TextUtils;

import java.awt.FileDialog;
import java.io.File;
import java.util.Iterator;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileSystemView;

/**
 * Class to handle file selection dialogs.
 */
public class OpenFile
{
	private static class OpenFileSwing extends JFileChooser
	{
		/** True if this is a file save dialog */						private boolean saveDialog;
        /** True to set new dir as working dir (default is true) */     private boolean setSelectedDirAsWorkingDir;
        private FileType fileType;

		/** Private constructor, use factory methods chooseInputFile or
		 * chooseOutputFile instead.
		 */
		private OpenFileSwing() {}

		/**
		 * Method called when the user clicks "ok" during file choosing.
		 * Prevents overwriting of existing files.
		 */
		public void approveSelection()
		{
			File f = getSelectedFile();
			if (saveDialog)
			{
				String filename = f.getName();
				if (f.exists())
				{
					int result = JOptionPane.showConfirmDialog(this, "The file "+filename+" already exists, would you like to overwrite it?",
						"Overwrite?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
					if (result != JOptionPane.OK_OPTION) return;
				}
			} else {
            // IF: Opening file found in LIBDIR redirect, then
            // if user clicks on file and hits ok, it acts as if file
            // is in current directory, which it is not.  We need to check
            // libdir directories in this case.
                FileSystemView view = getFileSystemView();
                if ((view instanceof LibDirs.LibDirFileSystemView) && !f.exists()) {
                    //LibDirs.LibDirFileSystemView lview = (LibDirs.LibDirFileSystemView)view;
                    for (Iterator<String> it = LibDirs.getLibDirs(); it.hasNext(); ) {
                        String dirName = (String)it.next();
                        File dir = new File(dirName);
                        if (!dir.exists()) continue;
                        if (!dir.isDirectory()) continue;
                        File newFile = new File(dir, f.getName());
                        if (newFile.exists()) {
                            // assume it's this one
                            f = newFile;
                            break;
                        }
                    }
                }
            }

			setSelectedFile(f);
			if (setSelectedDirAsWorkingDir)
                User.setWorkingDirectory(getCurrentDirectory().getPath());

            if (fileType != null)
            fileType.setGroupPath(getCurrentDirectory().getPath());
			super.approveSelection();
		}

        /**
         * Directories ending in .delib are treated as files.  Specifically, library files.
         * @param f the file
         * @return true if the file is traversable, false otherwise
         */
        public boolean isTraversable(File f) {
            if (f.getName().toLowerCase().endsWith("."+FileType.DELIB.getExtensions()[0])) {
                return false;
            }
            return super.isTraversable(f);
        }

        /**
         * Overridden to return true when the currently selected file is a .delib file.
         * This allows .DELIB to be treated as files for the approve (open/save) button.
         * @return
         */
        public boolean isDirectorySelectionEnabled() {

            File file = getSelectedFile();
            // return true if a .delib file is selected, otherwise call the parent method
            if (file != null &&
                (super.getFileSelectionMode() != JFileChooser.DIRECTORIES_ONLY) &&
                file.getName().toLowerCase().endsWith("."+FileType.DELIB.getExtensions()[0])) {
                return true;
            }
            return super.isDirectorySelectionEnabled();
        }
	}

//	/** the location of the open file dialog */		private static Point location = null;

	/**
	 * Factory method to create a new open dialog box using the default Type.
	 * @param type the type of file to read. Defaults to ANY if null.
	 * @param title dialog title to use; if null uses "Open 'filetype'".
	 */
	public static String chooseInputFile(FileType type, String title)
	{
		return chooseInputFile(type, title, false, User.getWorkingDirectory(), true);
	}

	/**
	 * Factory method to create a new open dialog box to search for a directory.
	 * @param title dialog title to use; if null uses "Select Directory".
	 */
	public static String chooseDirectory(String title)
	{
		return chooseInputFile(null, title, true, User.getWorkingDirectory(), true);
	}

    /**
     * Factory method to create a new open dialog box using the default Type.
     * @param type the type of file to read. Defaults to ANY if null.
     * @param title dialog title to use; if null uses "Open 'filetype'".
     * @param wantDirectory true to request a directory be selected, instead of a file.
     */
    public static String chooseInputFile(FileType type, String title, boolean wantDirectory) {
        return chooseInputFile(type, title, wantDirectory, User.getWorkingDirectory(), true);
    }

	/**
	 * Factory method to create a new open dialog box using the default Type.
	 * @param type the type of file to read. Defaults to ANY if null.
	 * @param title dialog title to use; if null uses "Open 'filetype'".
	 * @param wantDirectory true to request a directory be selected, instead of a file.
     * @param initialDir the initial directory
     * @param setSelectedDirAsWorkingDir if the user approves the selection,
     * set the directory as the current working dir if this is true.
	 */
	public static String chooseInputFile(FileType type, String title, boolean wantDirectory,
                                         String initialDir, boolean setSelectedDirAsWorkingDir)
	{
		if (title == null)
		{
			if (wantDirectory) title = "Choose Directory"; else
				title = "Open file";
			if (type != null) title = "Open " + type.getDescription();
		}

		boolean useSwing = true;
        // MacOS Open Dialog doesn't work when directories must be available for selection
//		if (!wantDirectory && Client.isOSMac())
//			useSwing = false;

//		if (location == null) location = new Point(100, 50);
//		System.out.println("Put it at "+location);


        String path = (type != null) ? type.getGroupPath() : null;
        if (path != null)
            initialDir = path;

		if (useSwing)
		{
			OpenFileSwing dialog = new OpenFileSwing();
			dialog.saveDialog = false;
            dialog.setSelectedDirAsWorkingDir = setSelectedDirAsWorkingDir;
            dialog.fileType = type;
			dialog.setDialogTitle(title);
            File dir = new File(initialDir);
            if (!dir.exists() || !dir.isDirectory()) dir = new File(User.getWorkingDirectory());
            dialog.setCurrentDirectory(dir);
			if (type != null) {
                if (type == FileType.ELIB || type == FileType.JELIB || type == FileType.DELIB ||
                    type == FileType.LIBFILE || type == FileType.LIBRARYFORMATS) {
                    LibDirs.LibDirFileSystemView view = LibDirs.newLibDirFileSystemView(dialog.getFileSystemView());
                    dialog.setFileSystemView(view);
                    dialog.setFileView(new LibDirs.LibDirFileView(view));
                }
                dialog.setFileFilter(type.getFileFilterSwing());
            }
			if (wantDirectory) dialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//			dialog.setLocation(location.x, location.y);
//			dialog.addComponentListener(new MoveComponentListener());
			int returnVal = dialog.showOpenDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				File file = dialog.getSelectedFile();
				return file.getPath();
			}
			return null;
		} else
		{
			// the AWT way
			FileDialog dialog = new FileDialog(TopLevel.getCurrentJFrame(), title, FileDialog.LOAD);
			dialog.setDirectory(User.getWorkingDirectory());
			if (type != null) dialog.setFilenameFilter(type.getFileFilterAWT());
			dialog.setVisible(true);
			String fileName = dialog.getFile();
			if (fileName == null) return null;
			User.setWorkingDirectory(dialog.getDirectory());
			return dialog.getDirectory() + fileName;
		}
	}

//	private static class MoveComponentListener implements ComponentListener
//	{
//		public void componentHidden(ComponentEvent e) {}
//		public void componentShown(ComponentEvent e) {}
//		public void componentResized(ComponentEvent e) {}
//		public void componentMoved(ComponentEvent e)
//		{
//			Rectangle bound = ((JFileChooser)e.getSource()).getBounds();
//			location.x = (int)bound.getMinX();
//			location.y = (int)bound.getMinY();
//System.out.println("Moved to "+location);
//		}
//	}

    /**
     * Factory method to create a new save dialog box using the
     * default EFileFilter.
     * @param type the type of file. Defaults to ANY if null.
     * @param title dialog title to use; if null uses "Write 'filetype'".
     * @param defaultFile default file name to write.
     */
    public static String chooseOutputFile(FileType type, String title, String defaultFile)
    {
        FileType [] types;
        if (type == null) types = null;
        else types = new FileType [] {type};
        return chooseOutputFile(types, title, defaultFile);
    }

	/**
	 * Factory method to create a new save dialog box using the
	 * default EFileFilter.
	 * @param types the types of file. Defaults to ANY if null.
	 * @param title dialog title to use; if null uses "Write 'filetype'".
	 * @param defaultFile default file name to write.
	 */
	public static String chooseOutputFile(FileType [] types, String title, String defaultFile)
	{
		if (title == null)
		{
			if (types != null) title = "Write " + types[0].getDescription(); else
				title = "Write file";
		}
        if (types == null) types = new FileType [] {com.sun.electric.tool.io.FileType.ANY};

		boolean useSwing = true;
		if (Client.isOSMac())
			useSwing = false;

        String initialDir = User.getWorkingDirectory();
        String path = types[0].getGroupPath();
        if (path != null)
            initialDir = path;

		if (useSwing)
		{
			OpenFileSwing dialog = new OpenFileSwing();
			dialog.saveDialog = true;
			dialog.setDialogTitle(title);
            for (int i=0; i<types.length; i++) {
			    dialog.addChoosableFileFilter(types[i].getFileFilterSwing());
            }
			dialog.setCurrentDirectory(new File(initialDir));
            if (defaultFile != null)
            {
                dialog.setFileFilter(FileMenu.getLibraryFormat(defaultFile, types[0]).getFileFilterSwing());
                dialog.setSelectedFile(new File(defaultFile));
            }
			int returnVal = dialog.showSaveDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				File file = dialog.getSelectedFile();
                String fileName = file.getPath();
                FileType selectedType = com.sun.electric.tool.io.FileType.getType(dialog.getFileFilter());
                if (selectedType != null)
                {
                    String dir = TextUtils.getFilePath(TextUtils.makeURLToFile(fileName));
                    selectedType.setGroupPath(dir);
	                String extension = selectedType.getExtensions()[0];
	                int dotPos = fileName.lastIndexOf('.');
	                if (dotPos < 0)
                        fileName += "." + extension;
                    else
	                {
	                    if (!fileName.substring(dotPos+1).equals(extension))
	                    {
	                        //fileName = fileName.substring(0, dotPos) + "." + extension;
	                        fileName = fileName + "." + extension;
	                    }
	                }
                }
				return fileName;
			}
			return null;
		} else
		{
			// the AWT way
			FileDialog awtDialog = new FileDialog(TopLevel.getCurrentJFrame(), title, FileDialog.SAVE);
			awtDialog.setDirectory(initialDir);
			awtDialog.setFile(defaultFile);
			awtDialog.setFilenameFilter(types[0].getFileFilterAWT());
			awtDialog.setVisible(true);
			String fileName = awtDialog.getFile();
			if (fileName == null) return null;
            types[0].setGroupPath(awtDialog.getDirectory());
			return awtDialog.getDirectory() + fileName;
		}
	}

	/**
	 * Method to determine OpenFile type based on extension
	 * @param libName
	 * @return OpenFile.Type extension
	 */
	public static FileType getOpenFileType(String libName, FileType def)
	{
        File libFile = new File(libName);
        libName = libFile.getName();
        // remove trailing file separator if file is a directory (as it is with .delib)
        if (libName.endsWith(File.separator)) {
            libName = libName.substring(0, libName.length()-1);
        }

		if (libName.endsWith(".elib"))
			return com.sun.electric.tool.io.FileType.ELIB;
		else if (libName.endsWith(".jelib"))
			return com.sun.electric.tool.io.FileType.JELIB;
        else if (libName.endsWith(".delib"))
            return com.sun.electric.tool.io.FileType.DELIB;
		else if (libName.endsWith(".txt"))
			return com.sun.electric.tool.io.FileType.READABLEDUMP;
		return (def);
	}
}
