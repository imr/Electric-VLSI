/*
 * Created on Sep 30, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
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
			JInternalFrame frame = ElectricDocWnd.CreateElectricDoc();
			frame.setVisible(true);
			desktop.add(frame);
			try{frame.setSelected(true);
			}catch(java.beans.PropertyVetoException f){}
			
			//TODO: open elib file
			
		}		
	}
/*	
	//set parent window for file chooser dialog
	public void setParentWnd(JComponent comp)
	{
		parent = comp;	
	}
	
	//get parent window
	public JComponent getParentWnd()
	{
		return parent;
	}
*/	
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
