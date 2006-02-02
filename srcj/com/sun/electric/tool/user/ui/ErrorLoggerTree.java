package com.sun.electric.tool.user.ui;

import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.database.text.TextUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Class to define a collection of highlighted errors in the Explorer tree.
 */
// ----------------------------- Explorer Tree Stuff ---------------------------

public class ErrorLoggerTree {

    /**
     * A static object is used so that its open/closed tree state can be maintained.
     */
    public static JPopupMenu getPopupMenu(ErrorLoggerTreeNode log) {
        JPopupMenu p = new JPopupMenu();
        JMenuItem m;
        m = new JMenuItem("Delete"); m.addActionListener(log); p.add(m);
        m = new JMenuItem("Get Info"); m.addActionListener(log); p.add(m);
	    m = new JMenuItem("Save"); m.addActionListener(log); p.add(m);
        m = new JMenuItem("Set Current"); m.addActionListener(log); p.add(m);
        return p;
    }

    public static void load()
    {
        String fileName = OpenFile.chooseInputFile(FileType.XML, "Read ErrorLogger");
        try {
            ErrorLogger.XMLParser parser = new ErrorLogger.XMLParser();
            parser.process(TextUtils.makeURLToFile(fileName));
        } catch (Exception e)
		{
			System.out.println("Error loading " + fileName);
			return;
		}
    }

    public static class ErrorLoggerTreeNode implements ActionListener
    {
        private ErrorLogger logger;

        ErrorLoggerTreeNode(ErrorLogger log)
        {
            this.logger = log;
        }

        public ErrorLogger getLogger() { return logger; }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() instanceof JMenuItem) {
                JMenuItem m = (JMenuItem)e.getSource();
                if (m.getText().equals("Delete")) logger.delete();
                else if (m.getText().equals("Save"))
                {
                    String filePath = null;

                    try
                    {
                        filePath = OpenFile.chooseOutputFile(FileType.XML, null, "ErrorLoggerSave.xml");
                        if (filePath == null) return; // cancel operation
                        PrintStream buffWriter = new PrintStream(new FileOutputStream(filePath));
                        logger.save(buffWriter);
                    } catch (IOException se)
                    {
                        System.out.println("Error creating " + filePath);
                    }
                }
                else if (m.getText().equals("Get Info")) {
                    System.out.println("ErrorLogger Information: " +  logger.getInfo());
                }
                else if (m.getText().equals("Set Current")) {
                    synchronized(ErrorLogger.getAllErrors()) { ErrorLogger.setCurrentLogger(logger); }
                    WindowFrame.wantToRedoErrorTree();
                }
            }
        }
    }

    public static void updateExplorerTree(DefaultMutableTreeNode explorerTree)
    {
        explorerTree.removeAllChildren();
        ArrayList<ErrorLogger> loggersCopy = new ArrayList<ErrorLogger>();
        synchronized(ErrorLogger.getAllErrors()) {
            loggersCopy.addAll(ErrorLogger.getAllErrors());
        }
        for (ErrorLogger logger : loggersCopy) {
            if (logger.getNumErrors() == 0 && logger.getNumWarnings() == 0) continue;
            DefaultMutableTreeNode loggerNode = new DefaultMutableTreeNode(new ErrorLoggerTreeNode(logger));
            DefaultMutableTreeNode groupNode = loggerNode;
            int currentSortKey = -1;
            for (Iterator<ErrorLogger.MessageLog> it = logger.getLogs(); it.hasNext();)
            {
                ErrorLogger.MessageLog el = it.next();
                // by default, groupNode is entire loggerNode
                // but, groupNode could be sub-node:
                if (logger.getSortKeyToGroupNames() != null) {
                    if (currentSortKey != el.getSortKey()) {
                        // create new sub-tree node
                        currentSortKey = el.getSortKey();
                        String groupName = logger.getSortKeyToGroupNames().get(new Integer(el.getSortKey()));
                        if (groupName != null) {
                            groupNode = new DefaultMutableTreeNode(groupName);
                            loggerNode.add(groupNode);
                        } else {
                            // not found, put in loggerNode
                            groupNode = loggerNode;
                        }
                    }
                }
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(el);
                groupNode.add(node);
            }
            explorerTree.add(loggerNode);
        }
    }

    public static void deleteAllLoggers()
    {
        ArrayList<ErrorLogger> loggersCopy = new ArrayList<ErrorLogger>();
        synchronized(ErrorLogger.getAllErrors()) {
            loggersCopy.addAll(ErrorLogger.getAllErrors());
        }
        for (ErrorLogger log : loggersCopy)
        {
            log.delete();
        }
    }
}
