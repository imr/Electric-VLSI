import javax.swing.JInternalFrame;
import java.awt.event.*;
import java.awt.*;
/*
 * Created on Sep 25, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

/**
 * @author wc147374
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ElectricDocWnd extends JInternalFrame
{
	static int openFrameCount =0;
	static final int xOffset = 30, yOffset = 30;
	
	//constructor
	private ElectricDocWnd()
	{
		super("Document #"+(++openFrameCount), true, true, true, true);
		setSize(300,300); //change size 
		setLocation(xOffset*openFrameCount, yOffset*openFrameCount);
	}
	
	//factory
	public static ElectricDocWnd CreateElectricDoc()
	{
		return new ElectricDocWnd();
	}
}
