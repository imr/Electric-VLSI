/*
 * Created on Sep 22, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
/**
 * @author wc147374
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Test {

	public static void main(String[] args)
	{
		try{UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		}catch(Exception e){}
		ElectricFrame test = ElectricFrame.CreateFrame("test");
		test.AddWindowExit();
		Dimension scrnSize = (Toolkit.getDefaultToolkit()).getScreenSize();
		scrnSize.height-=30;

		 
		ElectricMenu eMenu = ElectricMenu.CreateElectricMenu("FILE");
		OpenElectricFile fileOpen = new OpenElectricFile();
		eMenu.addMenuItem("Open", fileOpen);
		eMenu.addMenuItem("Close", new ActionListener(){public void actionPerformed(ActionEvent e){System.exit(0);}});
		
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(eMenu);
		test.setJMenuBar(menuBar);
		//fileOpen.setParentWnd((JFrame)test);
		/*	JMenu menu = new JMenu("File");
		
		JMenuItem item = new JMenuItem("Close");
		menu.add(item);
		
		
		JToolBar toolbar = new JToolBar();
		JToolBar statusbar = new JToolBar();
		statusbar.setFloatable(false);
		JProgressBar progBar = new JProgressBar();
		
		//statusbar.setLayout(new BorderLayout());
		//, BorderLayout.EAST);
		
		ImageIcon icon = new ImageIcon("./button1.gif");
		JButton button = new JButton(icon);
		button.setMargin(new Insets(0,0,0,0));
		button.setBorderPainted(false);
		
	
		toolbar.add(button);
		test.getContentPane().setLayout(new BorderLayout());
		test.getContentPane().add(toolbar, BorderLayout.NORTH);
		test.getContentPane().add(statusbar, BorderLayout.SOUTH);		
	
	
		JTextField xstatus = new JTextField("x: ",10);
		xstatus.setEditable(false);
		JTextField ystatus = new JTextField("y: ",10);
		ystatus.setEditable(false);
		JTextField text = new JTextField();
		text.setEditable(false);
		statusbar.add(xstatus);//, BorderLayout.WEST);
		statusbar.add(ystatus);
		statusbar.add(progBar);
		//statusbar.add(text);
		
		JToolBar tabBar = new JToolBar();
		
		test.getContentPane().add(tabBar,BorderLayout.WEST);	
		
		JTabbedPane tabbedTool = new JTabbedPane();
		
		Component panel1 = null;
		tabbedTool.addTab("Tool 1", panel1);
		Component panel2 = null;
		tabbedTool.addTab("Tool 2", panel2);

		
		tabBar.add(tabbedTool);
		
		tabBar.setOrientation(JToolBar.VERTICAL);*/
		
		JDesktopPane desktop = new JDesktopPane();
		//JInternalFrame frame = ElectricDocWnd.CreateElectricDoc();
		//frame.setVisible(true);
		//desktop.add(frame);
		//try{frame.setSelected(true);
		//}catch(java.beans.PropertyVetoException e){}
		test.getContentPane().add(desktop);
		fileOpen.setDesktop(desktop);	
		
		test.setSize(scrnSize);
		test.setVisible(true);
	}
}
