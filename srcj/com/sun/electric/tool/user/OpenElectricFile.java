package com.sun.electric.tool.user;

/*
 * Created on Sep 30, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

import com.sun.electric.tool.user.ElectricDocWnd;
import com.sun.electric.tool.io.Input;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import javax.swing.*;
import java.awt.event.*;
import java.io.File;

/**
 * @author wc147374
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class OpenElectricFile implements ActionListener 
{
	private JComponent parent=null;
	/** File Chooser Dialog handle*/private final JFileChooser fc = new JFileChooser();
	/** file handle*/				public File file;
	/** desktop pane handle*/ 		private JDesktopPane desktop = null;

	//messange handler when clicked
	public void actionPerformed(ActionEvent e)
	{
		int returnVal = fc.showOpenDialog(parent);
		if(returnVal==JFileChooser.APPROVE_OPTION)
		{
			file=fc.getSelectedFile();
			
			//open internal frame window

			//frame.setVisible(true);
			Library lib = Input.ReadLibrary(file.getPath(), null, Input.ImportType.BINARY);
			if (lib == null)
			{
				System.out.println("Error reading the library file");
			} else
			{
				System.out.println("Library read");
				Library.setCurrent(lib);
				Cell cell = lib.getCurCell();
				if (cell == null)
				{
					System.out.println("No current cell in this library");
				} else
				{
					ElectricDocWndFrame frame = ElectricDocWndFrame.CreateElectricDocFrame(cell);
					desktop.add(frame); 
				}
			}

			
			//	try{frame.setSelected(true);
			//	}catch(java.beans.PropertyVetoException f){}
			
			//TODO: open elib file
			
		}		
	}

	//set desktop for the internal frame
	public void setDesktop(JDesktopPane desktopPane)
	{
		desktop = desktopPane;
	}
	
	//get desktop for the internal frame
	public JDesktopPane getDesktop()
	{
		return desktop;
	}
}

