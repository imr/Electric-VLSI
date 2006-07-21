package com.sun.electric.tool.user.ui;

import com.sun.electric.database.CellId;
import com.sun.electric.database.change.DatabaseChangeEvent;
import com.sun.electric.database.change.DatabaseChangeListener;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ErrorLogger.MessageLog;
import com.sun.electric.tool.user.UserInterfaceMain;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * Class to define a collection of highlighted errors in the Explorer tree.
 */
// ----------------------------- Explorer Tree Stuff ---------------------------

public class ErrorLoggerTree {

    /** Top of error tree */            private static final DefaultMutableTreeNode errorTree = new DefaultMutableTreeNode("ERRORS");
    /** Path to error tree */           private static final TreePath errorPath = (new TreePath(ExplorerTreeModel.rootNode)).pathByAddingChild(errorTree);
    /** Current Logger */               static DefaultMutableTreeNode currentLogger;
    
    private static final ErrorLogger networkErrorLogger = ErrorLogger.newInstance("Network Errors");
    private static final ErrorLogger drcErrorLogger = ErrorLogger.newInstance("DRC (incremental)");
    private static DefaultMutableTreeNode networkTree;
    private static DefaultMutableTreeNode drcTree;

    // public methods called from any thread
    
    public static boolean hasLogger(ErrorLogger logger) {
        return indexOf(logger) >= 0;
    };
    
    public static void addLogger(ErrorLogger logger, boolean explain, boolean terminate) {
        logger.termLogging_(terminate);
        if (logger.getNumLogs() == 0) return;
        SwingUtilities.invokeLater(new AddLogger(logger, explain));
    };
    
    public static void updateNetworkErrors(Cell cell, List<ErrorLogger.MessageLog> errors) {
        SwingUtilities.invokeLater(new UpdateNetwork((CellId)cell.getId(), errors));
    }

    public static void updateDrcErrors(Cell cell, List<ErrorLogger.MessageLog> errors) {
        SwingUtilities.invokeLater(new UpdateDrc((CellId)cell.getId(), errors));
    }

    // public methods called from GUI thread
    
    public static DefaultMutableTreeNode getExplorerTree() { return errorTree; }
    
    /**
     * Method to advance to the next error and report it.
     */
    public static String reportNextMessage() {
        if (currentLogger == null) return "No errors to report";
        return ((ErrorLoggerTreeNode)currentLogger.getUserObject()).reportNextMessage_(true);
    }

    /**
     * Method to back up to the previous error and report it.
     */
    public static String reportPrevMessage() {
        if (currentLogger == null) return "No errors to report";
        return ((ErrorLoggerTreeNode)currentLogger.getUserObject()).reportPrevMessage_();
    }

    private static class AddLogger implements Runnable {
        private ErrorLogger logger;
        private boolean explain;
        AddLogger(ErrorLogger logger, boolean explain) {
            this.logger = logger;
            this.explain = explain;
        }
        public void run() {
            int i = indexOf(logger);
            if (i >= 0) {
                updateTree((DefaultMutableTreeNode)errorTree.getChildAt(i));
            } else {
                addLogger(errorTree.getChildCount(), logger);
                if (explain)
                    explain(logger);
            }
        }
    }
    
    private static void explain(ErrorLogger logger) {
        // To print consistent message in message window
        String extraMsg = "errors/warnings";
        if (logger.getNumErrors() == 0) extraMsg = "warnings";
        else  if (logger.getNumWarnings() == 0) extraMsg = "errors";
        String msg = logger.getInfo();
        System.out.println(msg);
        if (logger.getNumLogs() > 0) {
            System.out.println("Type > and < to step through " + extraMsg + ", or open the ERRORS view in the explorer");
        }
        if (logger.getNumErrors() > 0) {
            JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), msg,
                    logger.getSystem() + " finished with Errors", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private static class UpdateNetwork implements Runnable {
        private CellId cellId;
        private ArrayList<ErrorLogger.MessageLog> errors;
        UpdateNetwork(CellId cellId, List<ErrorLogger.MessageLog> errors) {
            this.cellId = cellId;
            this.errors = new ArrayList<ErrorLogger.MessageLog>(errors);
        }
        public void run() {
            Cell cell = EDatabase.clientDatabase().getCell(cellId);
            if (cell == null) return;
            boolean changed = networkErrorLogger.clearLogs(cell) || !errors.isEmpty();
            networkErrorLogger.addMessages(errors);
            if (!changed) return;
            networkErrorLogger.termLogging_(true);
            if (networkErrorLogger.getNumLogs() == 0) {
                removeLogger(0);
                return;
            }
            if (networkTree == null)
                networkTree = addLogger(0, networkErrorLogger);
            updateTree(networkTree);
            setCurrent(0);
        }
    }
            
    private static class UpdateDrc implements Runnable {
        private CellId cellId;
        private ArrayList<ErrorLogger.MessageLog> errors;
        UpdateDrc(CellId cellId, List<ErrorLogger.MessageLog> errors) {
            this.cellId = cellId;
            this.errors = new ArrayList<ErrorLogger.MessageLog>(errors);
        }
        public void run() {
            Cell cell = EDatabase.clientDatabase().getCell(cellId);
            if (cell == null) return;
            boolean changed = drcErrorLogger.clearLogs(cell) || !errors.isEmpty();
            drcErrorLogger.addMessages(errors);
            if (!changed) return;
            drcErrorLogger.termLogging_(true);
            int index = networkTree != null ? 1 : 0;
            if (drcErrorLogger.getNumLogs() == 0) {
                removeLogger(index);
                return;
            }
            if (drcTree == null)
                drcTree = addLogger(index, networkErrorLogger);
            updateTree(networkTree);
            setCurrent(index);
        }
    }
            
    private static DefaultMutableTreeNode addLogger(int index, ErrorLogger logger) {
        ErrorLoggerTreeNode tn = new ErrorLoggerTreeNode(logger);
        UserInterfaceMain.addDatabaseChangeListener(tn);
        DefaultMutableTreeNode newNode = new ErrorLoggerDefaultMutableTreeNode(tn);
        int[] childIndices = new int[] { index };
        DefaultMutableTreeNode[] children = new DefaultMutableTreeNode[] { newNode };
        setCurrent(-1);
        errorTree.insert(newNode, index);
        currentLogger = newNode;
        ExplorerTreeModel.fireTreeNodesInserted(errorTree, errorPath, childIndices, children);
        updateTree(newNode);
        return newNode;
    }
    
    private static void removeLogger(int index) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)errorTree.getChildAt(index);
        UserInterfaceMain.removeDatabaseChangeListener((ErrorLoggerTreeNode)node.getUserObject());
        if (node == networkTree) networkTree = null;
        if (node == drcTree) drcTree = null;
        if (node == currentLogger) currentLogger = null;
        int[] childIndices = new int[] { index };
        DefaultMutableTreeNode[] children = new DefaultMutableTreeNode[] { node };
        errorTree.remove(index);
        ExplorerTreeModel.fireTreeNodesRemoved(errorTree, errorPath, childIndices, children);
    }
    
    private static void updateTree(DefaultMutableTreeNode loggerNode) {
        TreePath loggerPath = errorPath.pathByAddingChild(loggerNode);
        int oldChildCount = loggerNode.getChildCount();
        if (oldChildCount != 0) {
            int[] childIndex = new int[oldChildCount];
            DefaultMutableTreeNode[] children = new DefaultMutableTreeNode[oldChildCount];
            for (int i = 0; i < oldChildCount; i++) {
                childIndex[i] = i;
                children[i] = (DefaultMutableTreeNode)loggerNode.getChildAt(i);
            }
            loggerNode.removeAllChildren();
            ExplorerTreeModel.fireTreeNodesRemoved(errorTree, loggerPath, childIndex, children);
        }
        ErrorLogger logger = ((ErrorLoggerTreeNode)loggerNode.getUserObject()).logger;
        if (logger.getNumLogs() == 0) return;
        DefaultMutableTreeNode groupNode = loggerNode;
        int currentSortKey = -1;
        for (Iterator<ErrorLogger.MessageLog> it = logger.getLogs(); it.hasNext();) {
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
        int newChildCount = loggerNode.getChildCount();
        int[] childIndex = new int[newChildCount];
        DefaultMutableTreeNode[] children = new DefaultMutableTreeNode[newChildCount];
        for (int i = 0; i < newChildCount; i++) {
            childIndex[i] = i;
            children[i] = (DefaultMutableTreeNode)loggerNode.getChildAt(i);
        }
        ExplorerTreeModel.fireTreeNodesInserted(errorTree, loggerPath, childIndex, children);
    }
    
    private static void setCurrent(int index) {
        int oldIndex = currentLogger != null ? indexOf((ErrorLoggerTreeNode)currentLogger.getUserObject()) : -1;
        if (index == oldIndex) return;
        currentLogger = index >= 0 ? (DefaultMutableTreeNode)errorTree.getChildAt(index) : null;
        int l = 0;
        if (oldIndex >= 0) l++;
        if (index >= 0) l++;
        int[] childIndex = new int[l];
        TreeNode[] children = new TreeNode[l];
        l = 0;
        if (oldIndex >= 0 && oldIndex < index) {
            childIndex[l] = oldIndex;
            children[l] = errorTree.getChildAt(oldIndex);
            l++;
        }
        if (index >= 0) {
            childIndex[l] = index;
            children[l] = errorTree.getChildAt(index);
            l++;
        }
        if (oldIndex >= 0 && oldIndex > index) {
            childIndex[l] = oldIndex;
            children[l] = errorTree.getChildAt(oldIndex);
            l++;
        }
        ExplorerTreeModel.fireTreeNodesChanged(errorTree, errorPath, childIndex, children);
    }
    
    /** Delete this logger */
    private static void delete(ErrorLoggerTreeNode node) {
        int index = indexOf(node);
        if (index < 0) return;
//        if (node.logger == networkErrorLogger || node.logger == drcErrorLogger) {
//            // just clear errors
//            for (int i = node.logger.getNumLogs() - 1; i >= 0; i--)
//                node.logger.deleteLog(i);
//            node.currentLogNumber = -1;
//            updateTree((DefaultMutableTreeNode)errorTree.getChildAt(index));
//            return;
//        }
        removeLogger(index);
        if (currentLogger != null && ((ErrorLoggerTreeNode)currentLogger.getUserObject()) == node) {
            if (errorTree.getChildCount() != 0)
                currentLogger = (DefaultMutableTreeNode)errorTree.getChildAt(0);
            else
                currentLogger = null;
        }
    }
    
    private static int indexOf(ErrorLoggerTreeNode tn) {
        for (int i = 0, numLoggers = errorTree.getChildCount(); i < numLoggers; i++)
            if (((DefaultMutableTreeNode)errorTree.getChildAt(i)).getUserObject() == tn) return i;
        return -1;
    }
        
    private static int indexOf(ErrorLogger logger) {
        for (int i = 0, numLoggers = errorTree.getChildCount(); i < numLoggers; i++) {
            DefaultMutableTreeNode defaultMutableTreeNode = (DefaultMutableTreeNode)errorTree.getChildAt(i);
            ErrorLoggerTreeNode errorLoggerTreeNode = (ErrorLoggerTreeNode)defaultMutableTreeNode.getUserObject();
            if (errorLoggerTreeNode.logger == logger) return i;
        }
        return -1;
    }
        
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
            ErrorLogger logger = parser.process(TextUtils.makeURLToFile(fileName), true);
            if (logger != null)
                addLogger(logger, false, true);
        } catch (Exception e)
		{
			System.out.println("Error loading " + fileName);
			return;
		}
    }

    public static class ErrorLoggerDefaultMutableTreeNode extends DefaultMutableTreeNode {
        ErrorLoggerDefaultMutableTreeNode(ErrorLoggerTreeNode tn) { super(tn); }
        public boolean isLeaf() { return false; }
    }
    
    public static class ErrorLoggerTreeNode implements DatabaseChangeListener, ActionListener
    {
        private ErrorLogger logger;
        private int currentLogNumber;

        ErrorLoggerTreeNode(ErrorLogger log)
        {
            this.logger = log;
        }

        public ErrorLogger getLogger() { return logger; }

        public String reportNextMessage_(boolean showHigh) {
            if (currentLogNumber < logger.getNumLogs()-1) {
                currentLogNumber++;
            } else {
                if (logger.getNumLogs() <= 0) return "No "+logger.getSystem()+" errors";
                currentLogNumber = 0;
            }
            return reportLog(currentLogNumber, showHigh);
        }
        
        public String reportPrevMessage_() {
            if (currentLogNumber > 0) {
                currentLogNumber--;
            } else {
                if (logger.getNumLogs() <= 0) return "No "+logger.getSystem()+" errors";
                currentLogNumber = logger.getNumLogs() - 1;
            }
            return reportLog(currentLogNumber, true);
        }
        
        /**
         * Report an error
         */
        private String reportLog(int logNumber, boolean showHigh) {
            
            if (logNumber < 0 || (logNumber >= logger.getNumLogs())) {
                return logger.getSystem() + ": no such error or warning "+(logNumber+1)+", only "+logger.getNumLogs()+" errors.";
            }
            
            ErrorLogger.MessageLog el = logger.getLog(logNumber);
            String extraMsg = null;
            if (logNumber < logger.getNumErrors()) {
                extraMsg = " error " + (logNumber+1) + " of " + logger.getNumErrors();
            } else {
                extraMsg = " warning " + (logNumber+1-logger.getNumErrors()) + " of " + logger.getNumWarnings();
            }
            String message = Job.getUserInterface().reportLog(el, showHigh, null);
            return (logger.getSystem() + extraMsg + ": " + message);
        }
        
        public void databaseChanged(DatabaseChangeEvent e) {
            // check if any errors need to be deleted
            boolean changed = false;
            for (int i = logger.getNumLogs() - 1; i >= 0; i--) {
                MessageLog err = logger.getLog(i);
                if (!err.isValid(EDatabase.clientDatabase())) {
                    logger.deleteLog(i);
                    if (i < currentLogNumber)
                        currentLogNumber--;
                    else if (i == currentLogNumber)
                        currentLogNumber = 0;
                    changed = true;
                }
            }
            if (!changed) return;
            int index = indexOf(this);
            if (index < 0) return;
            if (logger.getNumLogs() == 0)
                removeLogger(index);
            else
                updateTree((DefaultMutableTreeNode)errorTree.getChildAt(index));
        }
        
        public void actionPerformed(ActionEvent e) {
            int index = indexOf(this);
            if (e.getSource() instanceof JMenuItem) {
                JMenuItem m = (JMenuItem)e.getSource();
                if (m.getText().equals("Delete")) removeLogger(index);
                else if (m.getText().equals("Save")) {
                    String filePath = null;
                    
                    try {
                        filePath = OpenFile.chooseOutputFile(FileType.XML, null, "ErrorLoggerSave.xml");
                        if (filePath == null) return; // cancel operation
                        logger.save(filePath);
                    } catch (Exception se) {
                        System.out.println("Error creating " + filePath);
                    }
                } else if (m.getText().equals("Get Info")) {
                    System.out.println("ErrorLogger Information: " +  logger.getInfo());
                } else if (m.getText().equals("Set Current")) {
                    setCurrent(index);
                }
            }
        }
    }
    
    public static void deleteAllLoggers() {
        for (int i = errorTree.getChildCount() - 1; i >= 0; i--)
            removeLogger(i);
    }
}
