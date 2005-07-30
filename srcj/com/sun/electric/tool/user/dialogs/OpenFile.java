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
                    for (Iterator it = LibDirs.getLibDirs(); it.hasNext(); ) {
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
			User.setWorkingDirectory(getCurrentDirectory().getPath());
			super.approveSelection();
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
		return chooseInputFile(type, title, false);
	}

	/**
	 * Factory method to create a new open dialog box to search for a directory.
	 * @param title dialog title to use; if null uses "Select Directory".
	 */
	public static String chooseDirectory(String title)
	{
		return chooseInputFile(null, title, true);
	}

	/**
	 * Factory method to create a new open dialog box using the default Type.
	 * @param type the type of file to read. Defaults to ANY if null.
	 * @param title dialog title to use; if null uses "Open 'filetype'".
	 * @param wantDirectory true to request a directory be selected, instead of a file.
	 */
	public static String chooseInputFile(FileType type, String title, boolean wantDirectory)
	{
		if (title == null)
		{
			if (wantDirectory) title = "Choose Directory"; else
				title = "Open file";
			if (type != null) title = "Open " + type.getDescription();
		}

		boolean useSwing = true;
		if (TopLevel.getOperatingSystem() == TopLevel.OS.MACINTOSH)
			useSwing = false;

//		if (location == null) location = new Point(100, 50);
//		System.out.println("Put it at "+location);
		if (useSwing)
		{
			OpenFileSwing dialog = new OpenFileSwing();
			dialog.saveDialog = false;
			dialog.setDialogTitle(title);
            dialog.setCurrentDirectory(new File(User.getWorkingDirectory()));
			if (type != null) {
                if (type == FileType.ELIB || type == FileType.JELIB ||
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
		if (title != null)
		{
			if (types != null) title = "Write " + types[0].getDescription(); else
				title = "Write file";
		}
        if (types == null) types = new FileType [] {com.sun.electric.tool.io.FileType.ANY};

		boolean useSwing = true;
		if (TopLevel.getOperatingSystem() == TopLevel.OS.MACINTOSH)
			useSwing = false;

		if (useSwing)
		{
			OpenFileSwing dialog = new OpenFileSwing();
			dialog.saveDialog = true;
			dialog.setDialogTitle(title);
            for (int i=0; i<types.length; i++) {
			    dialog.addChoosableFileFilter(types[i].getFileFilterSwing());
            }
			dialog.setFileFilter(FileMenu.getLibraryFormat(defaultFile, types[0]).getFileFilterSwing());
			dialog.setCurrentDirectory(new File(User.getWorkingDirectory()));
			dialog.setSelectedFile(new File(defaultFile));
			int returnVal = dialog.showSaveDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				File file = dialog.getSelectedFile();
                String fileName = file.getPath();
                FileType selectedType = com.sun.electric.tool.io.FileType.getType(dialog.getFileFilter());
                if (selectedType != null)
                {
	                String extension = selectedType.getExtensions()[0];
	                int dotPos = fileName.lastIndexOf('.');
	                if (dotPos < 0) fileName += "." + extension; else
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
			awtDialog.setDirectory(User.getWorkingDirectory());
			awtDialog.setFile(defaultFile);
			awtDialog.setFilenameFilter(types[0].getFileFilterAWT());
			awtDialog.setVisible(true);
			String fileName = awtDialog.getFile();
			if (fileName == null) return null;
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
		if (libName.endsWith(".elib"))
			return com.sun.electric.tool.io.FileType.ELIB;
		else if (libName.endsWith(".jelib"))
			return com.sun.electric.tool.io.FileType.JELIB;
		else if (libName.endsWith(".txt"))
			return com.sun.electric.tool.io.FileType.READABLEDUMP;
		return (def);
	}
}
