package com.sun.electric.tool.user.ui;

import com.sun.electric.tool.Job;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * Class defines Job information in the explorer tree.
 */
public class JobTree {

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
    public static synchronized DefaultMutableTreeNode getExplorerTree() {
        DefaultMutableTreeNode explorerTree = new DefaultMutableTreeNode(jobNode);
        for (Iterator<Job> it = Job.getAllJobs(); it.hasNext();) {
            Job j = (Job)it.next();
            if (j.getDisplay()) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(new JobTreeNode(j));
//                    j.myNode.setUserObject(null);       // remove reference to job on old node
//                    j.myNode = node;                    // get rid of old node, point to new node
                explorerTree.add(node);
            }
        }
        return explorerTree;
    }

    public static class JobTreeNode implements ActionListener
    {
        private Job job;

        JobTreeNode(Job log)
        {
            this.job = log;
        }

        public String toString() { return job.toString(); }

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
