/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ExecDialog.java
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

import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.Exec;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * A Dialog for running and interacting with an external process. Usage:
 * <p>Create a new ExecDialog
 * <p>Call the startProcess() method.
 */
public class ExecDialog extends EDialog implements Exec.FinishedListener {

    /** Reads from process, writes to text area */
    private static class ProcessOutput extends OutputStream {

        private JTextArea area;
        private boolean updateScheduled;

        private ProcessOutput(JTextArea area) {
            this.area = area;
            updateScheduled = false;
        }

        public synchronized void write(int b) throws IOException {
            byte [] bytes = new byte[1];
            bytes[0] = (byte)b;
            String str = new String(bytes);
            area.append(str);

            if (!updateScheduled) {
                // no update scheduled, schedule one now
                updateScheduled = true;

                Runnable guiUpdater = new Runnable() {
                    public void run() { updateScroll(); }
                };
                SwingUtilities.invokeLater(guiUpdater);
            }
        }

        // scroll to end
        private synchronized void updateScroll() {
            try {
                Rectangle r = area.modelToView(area.getDocument().getLength());
                area.scrollRectToVisible(r);
                updateScheduled = false;
            } catch (javax.swing.text.BadLocationException e) {
                e.printStackTrace(System.out);
                ActivityLogger.logException(e);                
            }
        }

    }

    private ProcessOutput outStream;
    private ProcessOutput errStream;
    private Exec exec;
    private List<Exec.FinishedListener> finishedListenersToAdd;

    /** Creates new form ExecDialog */
    public ExecDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        exec = null;
        finishedListenersToAdd = new ArrayList<Exec.FinishedListener>();
		finishInitialization();
    }

    /** Do this before calling startProcess(). Listeners are only added before
     * the process starts. There is no need to remove yourself unless the
     * process has not finished yet, and you do not want to be notified.
     * @param l
     */
    public synchronized void addFinishedListener(Exec.FinishedListener l) {
        finishedListenersToAdd.add(l);
    }

    public synchronized void removeFinishedListener(Exec.FinishedListener l) {
        finishedListenersToAdd.remove(l);
    }

    /**
     * Start a process within an interactive dialog.
     * @param command the command to run (NOT a shell command!!)
     * @param envVars environment variables to use in the form name=value. If null, parent process vars are inherited
     * @param dir the working dir. If null, the parent process' working dir is used
     */
    public synchronized void startProcess(String command, String [] envVars, File dir) {
        if (exec != null) {
            System.out.println("ERROR: ExecDialog can only execute one process at a time.");
            return;
        }
        outStream = new ProcessOutput(outputTextArea);
        errStream = new ProcessOutput(outputTextArea);
        exec = new Exec(command, envVars, dir, outStream, errStream);
        exec.addFinishedListener(this);
        for (Exec.FinishedListener fl : finishedListenersToAdd) {
            exec.addFinishedListener(fl);
        }
        finishedListenersToAdd.clear();
        setTitle("External Process");
        statusLabel.setText("Running '"+command+"'...");
        setVisible(true);
        exec.start();
    }

    /**
     * Start a process within an interactive dialog.
     * @param command the command to run (NOT a shell command!!)
     * @param envVars environment variables to use in the form name=value. If null, parent process vars are inherited
     * @param dir the working dir. If null, the parent process' working dir is used
     */
    public synchronized void startProcess(String [] command, String [] envVars, File dir) {
        if (exec != null) {
            System.out.println("ERROR: ExecDialog can only execute one process at a time.");
            return;
        }
        outStream = new ProcessOutput(outputTextArea);
        errStream = new ProcessOutput(outputTextArea);
        exec = new Exec(command, envVars, dir, outStream, errStream);
        exec.addFinishedListener(this);
        for (Exec.FinishedListener l : finishedListenersToAdd) {
            exec.addFinishedListener(l);
        }
        finishedListenersToAdd.clear();
        setTitle("External Process");
        statusLabel.setText("Running "+command[0]+"...");
        setVisible(true);
        exec.start();
    }

    /**
     * Called by Exec when it is done. Satifies the Exec.FinishedListener Interface.
     * @param e a finished event.
     */
    public void processFinished(Exec.FinishedEvent e) {
        endProcess(e);
    }

    /**
     * Write one line to the process. Also writes that line to the output text area.
     * @param line the line to write
     */
    private synchronized void writeln(String line) {
        if (exec != null) {
            outputTextArea.append(">>> " +line + "\n");
            exec.writeln(line);
        }
    }

    /**
     * Clean up after getting a Exec.FinishedEvent.
     * @param e the event
     */
    private synchronized void endProcess(Exec.FinishedEvent e) {
        exec.removeFinishedListener(this);
        exec = null;
        String str;
        if (e.getExitValue() != 0) {
            JOptionPane.showMessageDialog(this, exec, "Exec '"+e.getExec()+"' failed: return value: "+e.getExitValue(), JOptionPane.ERROR_MESSAGE);
            str = "Process FAILED [exit="+e.getExitValue()+"]: '"+e.getExec()+"'\n";
        } else
            str = "Process Done [exit="+e.getExitValue()+"]: '"+e.getExec()+"'\n";
        statusLabel.setText(str);
        outputTextArea.append("*****" + str);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        mainPanel = new javax.swing.JPanel();
        inputTextField = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        outputTextArea = new javax.swing.JTextArea();
        statusLabel = new javax.swing.JLabel();

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        mainPanel.setLayout(new java.awt.GridBagLayout());

        inputTextField.setColumns(8);
        inputTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                inputTextFieldActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
        gridBagConstraints.weightx = 1.0;
        mainPanel.add(inputTextField, gridBagConstraints);

        outputTextArea.setColumns(40);
        outputTextArea.setRows(20);
        jScrollPane1.setViewportView(outputTextArea);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        mainPanel.add(jScrollPane1, gridBagConstraints);

        statusLabel.setText("Status:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        mainPanel.add(statusLabel, gridBagConstraints);

        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);

        pack();
    }//GEN-END:initComponents

    private void inputTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inputTextFieldActionPerformed
        writeln(inputTextField.getText());
        inputTextField.setText("");
    }//GEN-LAST:event_inputTextFieldActionPerformed
    
    /** Closes the dialog */
    private void closeDialog(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_closeDialog
        setVisible(false);
        if (exec != null) {
            // need to remove self as listener, otherwise will get call back via processFinished()
            exec.removeFinishedListener(this);
            exec.destroyProcess();
        }
        dispose();
    }//GEN-LAST:event_closeDialog

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField inputTextField;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JTextArea outputTextArea;
    private javax.swing.JLabel statusLabel;
    // End of variables declaration//GEN-END:variables
    
}
