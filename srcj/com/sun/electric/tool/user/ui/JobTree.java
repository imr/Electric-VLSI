package com.sun.electric.tool.user.ui;

import com.sun.electric.tool.Job;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * Class defines Job information in the explorer tree.
 */
public class JobTree extends DefaultMutableTreeNode {

    /**
     * A static object is used so that its open/closed tree state can be maintained.
     */
    private static final String jobNodeName = "JOBS";
    private static final JobTree jobTree = new JobTree();
    private static final TreePath jobPath = (new TreePath(ExplorerTreeModel.rootNode)).pathByAddingChild(jobTree);
    
    private final Vector<JobTreeNode> jobNodes = new Vector<JobTreeNode>();
    
    // array of ints
    private int[] indices = new int[1];
    private int indicesCount = 0;
    
    private JobTree() {
        super(jobNodeName);
        children = jobNodes;
    }

    /** Build Job explorer tree */
    public static DefaultMutableTreeNode getExplorerTree() { return jobTree; }
    
    /**
     * Update Job Tree to given list of Jobs.
     * @param jobs given list of jobs. 
     */
    public static void update(List<Job> jobs) {
        jobTree.updateJobs(jobs);
    }

    /** popup menu when user right-clicks on job in explorer tree */
    public static JPopupMenu getPopupStatus(JobTreeNode jobNode) {
        JPopupMenu popup = new JPopupMenu();
        ActionListener a = new JobMenuActionListener(jobNode.job);
        JMenuItem m;
        m = new JMenuItem("Get Info"); m.addActionListener(a); popup.add(m);
        m = new JMenuItem("Abort"); m.addActionListener(a); popup.add(m);
        m = new JMenuItem("Delete"); m.addActionListener(a); popup.add(m);
        return popup;
    }

    // TreeNode overrides 
    
    public boolean isLeaf() { return false; }
    public boolean getAllowsChildren() { return true; }
    public void insert(MutableTreeNode newChild, int childIndex) { throw new UnsupportedOperationException(); }
    public void remove(int childIndex) { throw new UnsupportedOperationException(); }
    
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
            return jobNodes.indexOf(tn);
        } catch (Exception e) {
            if (aChild == null)
                throw new IllegalArgumentException("argument is null");
        }
        return -1;
    }
    
    private void updateJobs(List<Job> newJobs) {
        assert SwingUtilities.isEventDispatchThread();
        indicesClear();
        int newJ = 0;
        for (int oldJ = 0; oldJ < jobNodes.size(); oldJ++) {
            JobTreeNode node = jobNodes.get(oldJ);
            int k;
            for (k = newJ; k < newJobs.size() && newJobs.get(k) != node.job; k++);
            if (k == newJobs.size())
                indicesAdd(oldJ);
            else
                newJ = k + 1;
        }
        if (indicesCount != 0) {
            int[] childIndices = new int[indicesCount];
            Object[] children = new Object[indicesCount];
            for (int i = indicesCount - 1; i >= 0; i--) {
                childIndices[i] = indices[i];
                children[i] = jobNodes.remove(indices[i]);
            }
            ExplorerTreeModel.fireTreeNodesRemoved(jobTree, jobPath, childIndices, children);
        }
        
        indicesClear();
        for (int i = 0; i < newJobs.size(); i++) {
            Job job = newJobs.get(i);
            if (i < jobNodes.size() && jobNodes.get(i).job == job)
                continue;
            jobNodes.add(i, new JobTreeNode(job));
            indicesAdd(i);
        }
        if (indicesCount != 0) {
            int[] childIndices = new int[indicesCount];
            Object[] children = new Object[indicesCount];
            for (int i = 0; i < indicesCount; i++) {
                childIndices[i] = indices[i];
                children[i] = jobNodes.get(indices[i]);
            }
            ExplorerTreeModel.fireTreeNodesInserted(jobTree, jobPath, childIndices, children);
        }
        if (newJobs.size() != jobNodes.size())
        assert newJobs.size() == jobNodes.size();
        
        indicesClear();
        for (int i = 0; i < jobNodes.size(); i++) {
            JobTreeNode node = jobNodes.get(i);
            assert node.job == newJobs.get(i);
            String s = node.toString();
            if (!node.jobString.equals(s)) {
                node.jobString = s;
                indicesAdd(i);
            }
        }
        if (indicesCount != 0) {
            int[] childIndices = new int[indicesCount];
            Object[] children = new Object[indicesCount];
            for (int i = 0; i < indicesCount; i++) {
                childIndices[i] = indices[i];
                children[i] = jobNodes.get(indices[i]);
            }
            ExplorerTreeModel.fireTreeNodesChanged(jobTree, jobPath, childIndices, children);
        }
    }
    
    void indicesClear() { indicesCount = 0; }
    
    void indicesAdd(int index) {
        if (indicesCount >= indices.length) {
            int[] newIndices = new int[indices.length*2];
            System.arraycopy(indices, 0, newIndices, 0, indices.length);
            indices = newIndices; 
        }
        indices[indicesCount++] = index;
    }
    
    private static class JobMenuActionListener implements ActionListener {
        private final Job job;
        
        JobMenuActionListener(Job job) { this.job = job; }
        
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
        
    public static class JobTreeNode implements TreeNode
    {
        private Job job;
        private String jobString;

        JobTreeNode(Job job)
        {
            this.job = job;
            jobString = job.toString();
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

        public String getInfo() { return job.getInfo(); }
    }
}
