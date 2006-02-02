package com.sun.electric.tool.user.ui;

import com.sun.electric.tool.Job;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

/**
 * Class defines Job information in the explorer tree.
 */
public class JobTree extends DefaultMutableTreeNode {

    private static Vector<JobTreeNode> jobs = new Vector<JobTreeNode>();
    
    private JobTree(String name) {
        super(name);
        children = jobs;
    }

    /**
     * Returns the index of the specified child in this node's child array.
     * If the specified node is not a child of this node, returns
     * <code>-1</code>.  This method performs a linear search and is O(n)
     * where n is the number of children.
     *
     * @param	aChild	the TreeNode to search for among this node's children
     * @exception	IllegalArgumentException	if <code>aChild</code>
     *							is null
     * @return	an int giving the index of the node in this node's child
     *          array, or <code>-1</code> if the specified node is a not
     *          a child of this node
     */
    public int getIndex(TreeNode aChild) {
        try {
            JobTreeNode tn = (JobTreeNode)aChild;
            return jobs.indexOf(tn);
        } catch (Exception e) {
            if (aChild == null)
                throw new IllegalArgumentException("argument is null");
        }
        return -1;
    }
    
    public void insert(MutableTreeNode newChild, int childIndex) {
        throw new UnsupportedOperationException();
    }
    
    public void remove(int childIndex) {
        throw new UnsupportedOperationException();
    }
    
    /** popup menu when user right-clicks on job in explorer tree */
    public static JPopupMenu getPopupStatus(JobTreeNode job) {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem m;
        m = new JMenuItem("Get Info"); m.addActionListener(job); popup.add(m);
        m = new JMenuItem("Abort"); m.addActionListener(job); popup.add(m);
        m = new JMenuItem("Delete"); m.addActionListener(job); popup.add(m);
        return popup;
    }

    /**
     * A static object is used so that its open/closed tree state can be maintained.
     */
    private static String jobNode = "JOBS";

    /** Build Job explorer tree */
    public static DefaultMutableTreeNode getExplorerTree() {
        return new JobTree(jobNode);
//        DefaultMutableTreeNode explorerTree = new DefaultMutableTreeNode(jobNode);
//        for (Iterator<Job> it = Job.getAllJobs(); it.hasNext();) {
//            Job j = (Job)it.next();
//            if (j.getDisplay()) {
//                DefaultMutableTreeNode node = new DefaultMutableTreeNode(new JobTreeNode(j));
////                    j.myNode.setUserObject(null);       // remove reference to job on old node
////                    j.myNode = node;                    // get rid of old node, point to new node
//                explorerTree.add(node);
//            }
//        }
//        return explorerTree;
    }
    
    public static void update() {
        assert SwingUtilities.isEventDispatchThread();
        jobs.clear();
        for (Iterator<Job> it = Job.getAllJobs(); it.hasNext();) {
            Job j = it.next();
            if (j.getDisplay()) {
                jobs.add(new JobTreeNode(j));
            }
        }
		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
            wf.getTreeModel().reload(wf.jobExplorerNode);
		}
    }

    public static class JobTreeNode implements TreeNode, ActionListener
    {
        private Job job;

        JobTreeNode(Job log)
        {
            this.job = log;
        }

        /**
         * Returns the child <code>TreeNode</code> at index
         * <code>childIndex</code>.
         */
        public TreeNode getChildAt(int childIndex) {
            throw new ArrayIndexOutOfBoundsException("node has no children");
        }
        
        /**
         * Returns the number of children <code>TreeNode</code>s the receiver
         * contains.
         */
        public int getChildCount() {
            return 0;
        }
        
        /**
         * Returns the parent <code>TreeNode</code> of the receiver.
         */
        public TreeNode getParent() {
            throw new UnsupportedOperationException();
        }
        
        /**
         * Returns the index of <code>node</code> in the receivers children.
         * If the receiver does not contain <code>node</code>, -1 will be
         * returned.
         */
        public int getIndex(TreeNode node) {
            if (node == null)
                throw new IllegalArgumentException("argument is null");
            return -1;
        }
        
        /**
         * Returns true if the receiver allows children.
         */
        public boolean getAllowsChildren() { return false; }
        
        /**
         * Returns true if the receiver is a leaf.
         */
        public boolean isLeaf() { return true; }
        
        /**
         * Returns the children of the receiver as an <code>Enumeration</code>.
         */
        public Enumeration children() {
            return DefaultMutableTreeNode.EMPTY_ENUMERATION;
        }
        
        public String toString() { return job.toString(); }

        /** Get info on Job */
        public String getInfo() { return job.getInfo(); }
        
        /** respond to menu item command */
        public void actionPerformed(ActionEvent e) {
            JMenuItem source = (JMenuItem)e.getSource();
            // extract library and cell from string
            if (source.getText().equals("Get Info"))
                System.out.println(job.getInfo());
            if (source.getText().equals("Abort"))
                job.abort();
            if (source.getText().equals("Delete")) {
                if (!job.remove()) {  // the job is out of databaseChangesThread inside Job.remove()
                    System.out.println("Cannot delete running jobs.  Wait till it is finished, or abort it");
                    return;
                }
            }
        }
    }
}
